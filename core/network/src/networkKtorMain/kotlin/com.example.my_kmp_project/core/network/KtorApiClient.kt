package com.example.my_kmp_project.core.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeoutConfig
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.preparePost
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.Parameters
import io.ktor.http.contentType
import io.ktor.http.formUrlEncode
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.JsonElement

/** HTTP status when the server answered with an error response, else null (Flutter d7876a1 parity). */
private fun Throwable.transportStatus(): Int? =
    (this as? ResponseException)?.response?.status?.value

/** 一次 `readAvailable` 的窗口；只影响分片粒度，帧边界由 `SseParser` 自己切。 */
private const val SSE_READ_WINDOW_BYTES: Int = 8 * 1024

public class KtorApiClient(
    private val http: HttpClient,
    private val onTokenExpired: TokenExpiredHandler? = null,
    private val businessHandlers: NetworkBusinessHandlers? = null,
) : ApiClient {

    override suspend fun getRaw(path: String, query: Map<String, String>): String {
        return try {
            http.get(path.trimStart('/')) {
                query.forEach { (k, v) -> parameter(k, v) }
            }.bodyAsText()
        } catch (t: Throwable) {
            throw NetworkError.Transport(t.message ?: "GET failed", t.transportStatus())
        }
    }

    override suspend fun getAbsoluteRaw(url: String): String {
        return try {
            http.get(url).bodyAsText()
        } catch (t: Throwable) {
            throw NetworkError.Transport(t.message ?: "GET absolute failed", t.transportStatus())
        }
    }

    override suspend fun postRaw(path: String, body: String): String {
        return try {
            http.post(path.trimStart('/')) {
                if (body.isNotEmpty()) setBody(body)
            }.bodyAsText()
        } catch (t: Throwable) {
            throw NetworkError.Transport(t.message ?: "POST failed", t.transportStatus())
        }
    }

    override suspend fun postForm(path: String, fields: Map<String, String>): HttpTextResponse {
        return try {
            val response = http.post(path.trimStart('/')) {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(
                    Parameters.build {
                        fields.forEach { (k, v) -> append(k, v) }
                    }.formUrlEncode(),
                )
            }
            HttpTextResponse(
                statusCode = response.status.value,
                body = response.bodyAsText(),
            )
        } catch (t: Throwable) {
            throw NetworkError.Transport(t.message ?: "FORM POST failed", t.transportStatus())
        }
    }

    /**
     * 流式 POST（Dart `SseClient.post` sse_client.dart:25-84 + `parseSseByteStream` :88-104）。
     *
     * 与既有 `*Raw`/`*Api` 的关键差别：**不读整包**。
     * - `expectSuccess = false` ⇒ 4xx/5xx 不抛 `ResponseException`，状态码只是判定输入
     *   （Dart `validateStatus: status < 600`，:46）；
     * - `Accept: text/event-stream` 覆盖全局默认头（:43），body 仍是 JSON；
     * - `requestTimeout` / `socketTimeout` 置为无限 ⇒ 长连接不被 `HttpTimeout` 掐死
     *   （Dart `receiveTimeout: Duration.zero`，:40；`connectTimeout` 仍守 30s，等价
     *   Dart `sendTimeout`）；
     * - `preparePost(...).execute { ... }` 才是 Ktor 的流式出口（`execute()` 不带 block
     *   会把整包读进内存），因此分片读取必须在 block 内完成；
     * - 字节→文本由 [Utf8ChunkDecoder] 增量解码，切在多字节字符中间也不会乱码。
     *
     * 失败一律是 [SseException]（Dart 词汇表），不混进 [NetworkError.Transport]。
     */
    override fun openSseTextStream(
        path: String,
        body: String,
        headers: Map<String, String>,
    ): Flow<String> = flow {
        val statement = http.preparePost(path.trimStart('/')) {
            expectSuccess = false
            header(HttpHeaders.Accept, SSE_ACCEPT_HEADER_VALUE)
            headers.forEach { (name, value) -> header(name, value) }
            contentType(ContentType.Application.Json)
            timeout {
                requestTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
                socketTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
            }
            setBody(body)
        }
        try {
            statement.execute { response ->
                val channel = response.bodyAsChannel()
                val chunks = flow {
                    val decoder = Utf8ChunkDecoder()
                    val buffer = ByteArray(SSE_READ_WINDOW_BYTES)
                    while (true) {
                        val read = channel.readAvailable(buffer)
                        if (read < 0) break
                        if (read == 0) continue
                        val text = decoder.decode(buffer, 0, read)
                        if (text.isNotEmpty()) emit(text)
                    }
                    val tail = decoder.finish()
                    if (tail.isNotEmpty()) emit(tail)
                }
                val gate = SseOpenGate(
                    statusCode = response.status.value,
                    contentType = response.contentType()?.toString()
                        ?: response.headers[HttpHeaders.ContentType],
                    chunks = chunks,
                )
                // 前流失败在这里抛出（读净 body 后），block 结束时 Ktor 自动回收连接。
                emitAll(gate.eventStreamText())
            }
        } catch (t: Throwable) {
            // 取消是正常出口（用户「停止生成」），原样传播；Dart CancelToken 同义。
            if (t is CancellationException || t is SseException) throw t
            throw SseException(
                message = t.message ?: "SSE connection failed",
                statusCode = t.transportStatus(),
                error = SSE_ERROR_CONNECTION,
            )
        }
    }

    override suspend fun <T> getApi(
        path: String,
        query: Map<String, String>,
        parseData: (JsonElement?) -> T?,
    ): ApiResponse<T> {
        val response = try {
            http.get(path.trimStart('/')) {
                query.forEach { (k, v) -> parameter(k, v) }
            }
        } catch (t: Throwable) {
            throw NetworkError.Transport(t.message ?: "GET failed", t.transportStatus())
        }
        return ApiEnvelopeParser.parse(
            raw = response.bodyAsText(),
            parseData = parseData,
            onTokenExpired = onTokenExpired,
            businessHandlers = businessHandlers,
            httpStatus = response.status.value,
        )
    }

    override suspend fun <T> postApi(
        path: String,
        body: String,
        parseData: (JsonElement?) -> T?,
    ): ApiResponse<T> {
        val response = try {
            http.post(path.trimStart('/')) {
                if (body.isNotEmpty()) setBody(body)
            }
        } catch (t: Throwable) {
            throw NetworkError.Transport(t.message ?: "POST failed", t.transportStatus())
        }
        return ApiEnvelopeParser.parse(
            raw = response.bodyAsText(),
            parseData = parseData,
            onTokenExpired = onTokenExpired,
            businessHandlers = businessHandlers,
            httpStatus = response.status.value,
        )
    }

    override suspend fun putRaw(path: String, body: String): String =
        sendRaw(HttpMethod.Put, path, body, emptyMap())

    override suspend fun patchRaw(path: String, body: String): String =
        sendRaw(HttpMethod.Patch, path, body, emptyMap())

    override suspend fun deleteRaw(path: String): String =
        sendRaw(HttpMethod.Delete, path, body = "", query = emptyMap())

    override suspend fun <T> putApi(
        path: String,
        body: String,
        parseData: (JsonElement?) -> T?,
    ): ApiResponse<T> = sendApi(HttpMethod.Put, path, body, emptyMap(), parseData)

    override suspend fun <T> patchApi(
        path: String,
        body: String,
        parseData: (JsonElement?) -> T?,
    ): ApiResponse<T> = sendApi(HttpMethod.Patch, path, body, emptyMap(), parseData)

    override suspend fun <T> deleteApi(
        path: String,
        query: Map<String, String>,
        parseData: (JsonElement?) -> T?,
    ): ApiResponse<T> = sendApi(HttpMethod.Delete, path, body = "", query, parseData)

    private suspend fun sendRaw(
        httpMethod: HttpMethod,
        path: String,
        body: String,
        query: Map<String, String>,
    ): String = try {
        http.request(path.trimStart('/')) {
            method = httpMethod
            if (body.isNotEmpty()) setBody(body)
            query.forEach { (k, v) -> parameter(k, v) }
        }.bodyAsText()
    } catch (t: Throwable) {
        throw NetworkError.Transport(t.message ?: "${httpMethod.value} failed", t.transportStatus())
    }

    private suspend fun <T> sendApi(
        httpMethod: HttpMethod,
        path: String,
        body: String,
        query: Map<String, String>,
        parseData: (JsonElement?) -> T?,
    ): ApiResponse<T> {
        val response = try {
            http.request(path.trimStart('/')) {
                method = httpMethod
                if (body.isNotEmpty()) setBody(body)
                query.forEach { (k, v) -> parameter(k, v) }
            }
        } catch (t: Throwable) {
            throw NetworkError.Transport(t.message ?: "${httpMethod.value} failed", t.transportStatus())
        }
        return ApiEnvelopeParser.parse(
            raw = response.bodyAsText(),
            parseData = parseData,
            onTokenExpired = onTokenExpired,
            businessHandlers = businessHandlers,
            httpStatus = response.status.value,
        )
    }
}

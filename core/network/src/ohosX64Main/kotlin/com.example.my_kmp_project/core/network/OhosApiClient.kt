package com.example.my_kmp_project.core.network

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonElement

/** ohosX64-only cinterop client (no CPF Ktor ohosX64 klib) — streaming POST is a GAP(R5). */
public class OhosApiClient(
    private val onTokenExpired: TokenExpiredHandler? = null,
    private val businessHandlers: NetworkBusinessHandlers? = null,
) : ApiClient {

    override suspend fun getRaw(path: String, query: Map<String, String>): String {
        val url = OhosHttpTransport.buildBusinessUrl(path, query)
        val result = OhosHttpTransport.get(url, DemoRequestHeaders.forUrl(url))
        if (result.statusCode >= 400) {
            throw NetworkError.Transport("GET $path failed: HTTP ${result.statusCode}")
        }
        return result.body
    }

    override suspend fun getAbsoluteRaw(url: String): String {
        val result = OhosHttpTransport.get(url, DemoRequestHeaders.forUrl(url))
        if (result.statusCode >= 400) {
            throw NetworkError.Transport("GET absolute failed: HTTP ${result.statusCode}")
        }
        return result.body
    }

    override suspend fun postRaw(path: String, body: String): String {
        val url = OhosHttpTransport.buildBusinessUrl(path, emptyMap())
        val headers = DemoRequestHeaders.forUrl(url)
        val result = OhosHttpTransport.post(
            url = url,
            headers = headers,
            body = body,
            contentType = "application/json",
        )
        if (result.statusCode >= 400) {
            throw NetworkError.Transport("POST $path failed: HTTP ${result.statusCode}")
        }
        return result.body
    }

    override suspend fun postForm(path: String, fields: Map<String, String>): HttpTextResponse {
        val url = OhosHttpTransport.buildBusinessUrl(path, emptyMap())
        val encoded = fields.entries.joinToString("&") { (k, v) ->
            "${encodeComponent(k)}=${encodeComponent(v)}"
        }
        val headers = DemoRequestHeaders.forUrl(url)
        val result = OhosHttpTransport.post(
            url = url,
            headers = headers,
            body = encoded,
            contentType = "application/x-www-form-urlencoded",
        )
        return HttpTextResponse(statusCode = result.statusCode, body = result.body)
    }

    // GAP(R5): `libnet_http` cinterop 只在回调里给**整包** body
    // （OhosHttpTransport.post 返回完整字符串，无增量分片通道），因此 ohosX64 无法做 SSE。
    // 这里明确抛类型化错误，绝不伪造「OHOS x64 流式已就绪」。
    // 注意 ohos**Arm64** 不落到本类：core/network/build.gradle.kts 把 ohosArm64Main 接的是
    // networkKtorMain（CPF Ktor 3.3.3-0.3.0 + CIO），那条路走 KtorApiClient.openSseTextStream，支持流式。
    override fun openSseTextStream(
        path: String,
        body: String,
        headers: Map<String, String>,
    ): Flow<String> = throw SseStreamingUnsupportedException(
        transport = "ohosX64/net_http",
        path = path,
    )

    override suspend fun <T> getApi(
        path: String,
        query: Map<String, String>,
        parseData: (JsonElement?) -> T?,
    ): ApiResponse<T> {
        val url = OhosHttpTransport.buildBusinessUrl(path, query)
        val result = OhosHttpTransport.get(url, DemoRequestHeaders.forUrl(url))
        return ApiEnvelopeParser.parse(
            raw = result.body,
            parseData = parseData,
            onTokenExpired = onTokenExpired,
            businessHandlers = businessHandlers,
            httpStatus = result.statusCode,
        )
    }

    override suspend fun <T> postApi(
        path: String,
        body: String,
        parseData: (JsonElement?) -> T?,
    ): ApiResponse<T> {
        val url = OhosHttpTransport.buildBusinessUrl(path, emptyMap())
        val result = OhosHttpTransport.post(
            url = url,
            headers = DemoRequestHeaders.forUrl(url),
            body = body,
            contentType = "application/json",
        )
        return ApiEnvelopeParser.parse(
            raw = result.body,
            parseData = parseData,
            onTokenExpired = onTokenExpired,
            businessHandlers = businessHandlers,
            httpStatus = result.statusCode,
        )
    }

    override suspend fun putRaw(path: String, body: String): String {
        val url = OhosHttpTransport.buildBusinessUrl(path, emptyMap())
        val result = OhosHttpTransport.put(
            url = url,
            headers = DemoRequestHeaders.forUrl(url),
            body = body,
            contentType = "application/json",
        )
        if (result.statusCode >= 400) {
            throw NetworkError.Transport("PUT $path failed: HTTP ${result.statusCode}")
        }
        return result.body
    }

    override suspend fun patchRaw(path: String, body: String): String {
        val url = OhosHttpTransport.buildBusinessUrl(path, emptyMap())
        val result = OhosHttpTransport.patch(
            url = url,
            headers = DemoRequestHeaders.forUrl(url),
            body = body,
            contentType = "application/json",
        )
        if (result.statusCode >= 400) {
            throw NetworkError.Transport("PATCH $path failed: HTTP ${result.statusCode}")
        }
        return result.body
    }

    override suspend fun deleteRaw(path: String): String {
        val url = OhosHttpTransport.buildBusinessUrl(path, emptyMap())
        val result = OhosHttpTransport.delete(url, DemoRequestHeaders.forUrl(url))
        if (result.statusCode >= 400) {
            throw NetworkError.Transport("DELETE $path failed: HTTP ${result.statusCode}")
        }
        return result.body
    }

    override suspend fun <T> putApi(
        path: String,
        body: String,
        parseData: (JsonElement?) -> T?,
    ): ApiResponse<T> {
        val url = OhosHttpTransport.buildBusinessUrl(path, emptyMap())
        val result = OhosHttpTransport.put(
            url = url,
            headers = DemoRequestHeaders.forUrl(url),
            body = body,
            contentType = "application/json",
        )
        return ApiEnvelopeParser.parse(
            raw = result.body,
            parseData = parseData,
            onTokenExpired = onTokenExpired,
            businessHandlers = businessHandlers,
            httpStatus = result.statusCode,
        )
    }

    override suspend fun <T> patchApi(
        path: String,
        body: String,
        parseData: (JsonElement?) -> T?,
    ): ApiResponse<T> {
        val url = OhosHttpTransport.buildBusinessUrl(path, emptyMap())
        val result = OhosHttpTransport.patch(
            url = url,
            headers = DemoRequestHeaders.forUrl(url),
            body = body,
            contentType = "application/json",
        )
        return ApiEnvelopeParser.parse(
            raw = result.body,
            parseData = parseData,
            onTokenExpired = onTokenExpired,
            businessHandlers = businessHandlers,
            httpStatus = result.statusCode,
        )
    }

    override suspend fun <T> deleteApi(
        path: String,
        query: Map<String, String>,
        parseData: (JsonElement?) -> T?,
    ): ApiResponse<T> {
        val url = OhosHttpTransport.buildBusinessUrl(path, query)
        val result = OhosHttpTransport.delete(url, DemoRequestHeaders.forUrl(url))
        return ApiEnvelopeParser.parse(
            raw = result.body,
            parseData = parseData,
            onTokenExpired = onTokenExpired,
            businessHandlers = businessHandlers,
            httpStatus = result.statusCode,
        )
    }

    private fun encodeComponent(value: String): String = buildString(value.length) {
        value.forEach { ch ->
            when {
                ch.isLetterOrDigit() || ch == '-' || ch == '_' || ch == '.' || ch == '~' -> append(ch)
                else -> ch.code.toString(16).uppercase().let { hex ->
                    append('%')
                    if (hex.length == 1) append('0')
                    append(hex)
                }
            }
        }
    }
}

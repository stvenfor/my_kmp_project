package com.example.my_kmp_project.core.network

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * 信封调用 + 流式调用的统一出口。继承 [SseTransport]（契约表 #96），
 * 因此 feature 只需拿到 `NetworkFacade.api()` 就能同时发 JSON 请求与 SSE 请求。
 *
 * 流式方法是**带默认实现的 override**：不支持流式的实现（测试替身、ohosX64 cinterop）
 * 不强制编写空壳，调用即抛 [SseStreamingUnsupportedException]；
 * 支持流式的平台（android / ios / ohosArm64 = Ktor）在各自 client 里 override。
 */
public interface ApiClient : SseTransport {
    suspend fun getRaw(path: String, query: Map<String, String> = emptyMap()): String
    /**
     * GET an absolute URL (no business host). Used for COS assets such as countrycode.json.
     */
    suspend fun getAbsoluteRaw(url: String): String
    suspend fun postRaw(path: String, body: String = ""): String
    /** POST `application/x-www-form-urlencoded`; returns status + raw body (no envelope parse). */
    suspend fun postForm(path: String, fields: Map<String, String>): HttpTextResponse
    suspend fun putRaw(path: String, body: String = ""): String
    suspend fun patchRaw(path: String, body: String = ""): String
    suspend fun deleteRaw(path: String): String
    suspend fun <T> getApi(
        path: String,
        query: Map<String, String> = emptyMap(),
        parseData: (JsonElement?) -> T?,
    ): ApiResponse<T>
    suspend fun <T> postApi(
        path: String,
        body: String = "",
        parseData: (JsonElement?) -> T?,
    ): ApiResponse<T>
    suspend fun <T> putApi(
        path: String,
        body: String = "",
        parseData: (JsonElement?) -> T?,
    ): ApiResponse<T>
    suspend fun <T> patchApi(
        path: String,
        body: String = "",
        parseData: (JsonElement?) -> T?,
    ): ApiResponse<T>
    suspend fun <T> deleteApi(
        path: String,
        query: Map<String, String> = emptyMap(),
        parseData: (JsonElement?) -> T?,
    ): ApiResponse<T>

    /**
     * 流式 POST（Dart `SseClient.post`，`commons/network/lib/sse/sse_client.dart:25-84`）。
     * 冷流：collect 才发请求；元素是 UTF-8 增量解码后的文本分片。
     *
     * 默认实现 = 「这个 client 不会流式」：直接抛 [SseStreamingUnsupportedException]，
     * 与 ohosX64 的 `// GAP(R5)` 同一套类型化错误，绝不静默返回空流。
     */
    override fun openSseTextStream(
        path: String,
        body: String,
        headers: Map<String, String>,
    ): Flow<String> = throw SseStreamingUnsupportedException(transport = "ApiClient", path = path)
}

public expect fun createPlatformApiClient(
    onTokenExpired: TokenExpiredHandler? = null,
    businessHandlers: NetworkBusinessHandlers? = null,
): ApiClient

/** Typed GET — decode `data` as [T] via [NetworkJson]. */
public suspend inline fun <reified T> ApiClient.getApi(
    path: String,
    query: Map<String, String> = emptyMap(),
): ApiResponse<T> = getApi(path, query, parseData = ::decodeEnvelopeData)

/**
 * Typed GET of a BFF list page — `data` 归一为 [ListPage]
 * （Dart `ResultModel.listPage` → `ListData<T>`：`list` / `items` / 裸数组）。
 */
public suspend inline fun <reified T> ApiClient.getListPage(
    path: String,
    query: Map<String, String> = emptyMap(),
): ApiResponse<ListPage<T>> = getApi(path, query, parseData = { decodeListPage<T>(it) })

/**
 * Typed POST with a JSON string body — decode `data` as [Res].
 */
public suspend inline fun <reified Res> ApiClient.postApiDecoded(
    path: String,
    body: String = "",
): ApiResponse<Res> = postApi(path, body, parseData = ::decodeEnvelopeData)

/**
 * Typed POST — encode [Req], decode `data` as [Res]. Preferred call-site API
 * (Kotlin-Owned BFF JSON); callers never touch [JsonElement].
 */
public suspend inline fun <reified Req, reified Res> ApiClient.postApi(
    path: String,
    body: Req,
): ApiResponse<Res> = postApiDecoded(path, NetworkJson.encodeToString(body))

/** Typed POST when the caller only cares about business `code` (no `data`). */
public suspend inline fun <reified Req> ApiClient.postApiOk(
    path: String,
    body: Req,
): ApiResponse<Unit> = postApi(path, NetworkJson.encodeToString(body)) { Unit }

/** Typed PUT — encode [Req], decode `data` as [Res]. */
public suspend inline fun <reified Req, reified Res> ApiClient.putApi(
    path: String,
    body: Req,
): ApiResponse<Res> = putApi(path, NetworkJson.encodeToString(body), parseData = ::decodeEnvelopeData)

/** Typed PUT when the caller only cares about business `code` (no `data`). */
public suspend inline fun <reified Req> ApiClient.putApiOk(
    path: String,
    body: Req,
): ApiResponse<Unit> = putApi(path, NetworkJson.encodeToString(body)) { Unit }

/** Typed PATCH — encode [Req], decode `data` as [Res]. */
public suspend inline fun <reified Req, reified Res> ApiClient.patchApi(
    path: String,
    body: Req,
): ApiResponse<Res> = patchApi(path, NetworkJson.encodeToString(body), parseData = ::decodeEnvelopeData)

/** Typed PATCH when the caller only cares about business `code` (no `data`). */
public suspend inline fun <reified Req> ApiClient.patchApiOk(
    path: String,
    body: Req,
): ApiResponse<Unit> = patchApi(path, NetworkJson.encodeToString(body)) { Unit }

/** Typed DELETE — decode `data` as [T]. */
public suspend inline fun <reified T> ApiClient.deleteApi(
    path: String,
    query: Map<String, String> = emptyMap(),
): ApiResponse<T> = deleteApi(path, query, parseData = ::decodeEnvelopeData)

/** Typed DELETE when the caller only cares about business `code` (no `data`). */
public suspend inline fun ApiClient.deleteApiOk(
    path: String,
    query: Map<String, String> = emptyMap(),
): ApiResponse<Unit> = deleteApi(path, query) { Unit }

public suspend fun ApiClient.getApiString(
    path: String,
    query: Map<String, String> = emptyMap(),
): ApiResponse<String> = getApi(path, query) { el ->
    when (el) {
        null -> null
        is JsonPrimitive -> el.contentOrNull
        else -> el.toString()
    }
}

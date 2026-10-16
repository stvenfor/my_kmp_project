package com.example.my_kmp_project.feature.home.api

import com.example.my_kmp_project.core.network.ApiClient
import com.example.my_kmp_project.core.network.ApiEnvelopeParser
import com.example.my_kmp_project.core.network.ApiResponse
import com.example.my_kmp_project.core.network.HttpTextResponse
import kotlinx.serialization.json.JsonElement

/**
 * home 契约层测试用的哑 client：记录 verb/path/query/body，把预置的 BFF 原始响应体交给
 * 真实解析器 [ApiEnvelopeParser]（与 `KtorApiClient` 同一条解析路径），
 * 因此测试覆盖「请求形状 + 信封解码」，不发网络请求。
 *
 * 与 `feature/mine` 的 `MineRecordingApiClient` 同形（各 lane 各自持有一份，避免跨 feature 依赖）。
 */
internal class HomeRecordingApiClient(
    private var raw: String = EMPTY_OK,
) : ApiClient {

    /** 一次调用记录：`body` 对 GET / DELETE 是空串；无 body 的动作 POST 也是空串。 */
    internal data class Call(
        val method: String,
        val path: String,
        val query: Map<String, String>,
        val body: String,
    )

    val calls: MutableList<Call> = mutableListOf()

    /** 最近一次调用（断言请求形状用）。 */
    val last: Call
        get() = calls.last()

    fun stub(raw: String) {
        this.raw = raw
    }

    private fun record(method: String, path: String, query: Map<String, String>, body: String): String {
        calls += Call(method, path, query, body)
        return raw
    }

    override suspend fun getRaw(path: String, query: Map<String, String>): String =
        record("GET", path, query, "")

    override suspend fun getAbsoluteRaw(url: String): String =
        record("GET", url, emptyMap(), "")

    override suspend fun postRaw(path: String, body: String): String =
        record("POST", path, emptyMap(), body)

    override suspend fun postForm(path: String, fields: Map<String, String>): HttpTextResponse {
        record("POST_FORM", path, fields, "")
        return HttpTextResponse(statusCode = 200, body = raw)
    }

    override suspend fun putRaw(path: String, body: String): String =
        record("PUT", path, emptyMap(), body)

    override suspend fun patchRaw(path: String, body: String): String =
        record("PATCH", path, emptyMap(), body)

    override suspend fun deleteRaw(path: String): String =
        record("DELETE", path, emptyMap(), "")

    override suspend fun <T> getApi(
        path: String,
        query: Map<String, String>,
        parseData: (JsonElement?) -> T?,
    ): ApiResponse<T> = ApiEnvelopeParser.parse(
        raw = record("GET", path, query, ""),
        parseData = parseData,
    )

    override suspend fun <T> postApi(
        path: String,
        body: String,
        parseData: (JsonElement?) -> T?,
    ): ApiResponse<T> = ApiEnvelopeParser.parse(
        raw = record("POST", path, emptyMap(), body),
        parseData = parseData,
    )

    override suspend fun <T> putApi(
        path: String,
        body: String,
        parseData: (JsonElement?) -> T?,
    ): ApiResponse<T> = ApiEnvelopeParser.parse(
        raw = record("PUT", path, emptyMap(), body),
        parseData = parseData,
    )

    override suspend fun <T> patchApi(
        path: String,
        body: String,
        parseData: (JsonElement?) -> T?,
    ): ApiResponse<T> = ApiEnvelopeParser.parse(
        raw = record("PATCH", path, emptyMap(), body),
        parseData = parseData,
    )

    override suspend fun <T> deleteApi(
        path: String,
        query: Map<String, String>,
        parseData: (JsonElement?) -> T?,
    ): ApiResponse<T> = ApiEnvelopeParser.parse(
        raw = record("DELETE", path, query, ""),
        parseData = parseData,
    )

    private companion object {
        /** 默认应答：空成功信封，避免未 stub 时静默当失败。 */
        const val EMPTY_OK: String = """{"code":0,"message":"ok","data":null}"""
    }
}

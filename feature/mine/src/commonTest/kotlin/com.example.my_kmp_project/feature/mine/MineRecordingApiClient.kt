package com.example.my_kmp_project.feature.mine

import com.example.my_kmp_project.core.network.ApiClient
import com.example.my_kmp_project.core.network.ApiEnvelopeParser
import com.example.my_kmp_project.core.network.ApiResponse
import com.example.my_kmp_project.core.network.HttpTextResponse
import kotlinx.serialization.json.JsonElement

/**
 * 契约层测试用的哑 client：记录 verb/path/query/body，把预置的 BFF 原始响应体
 * 交给真实解析器 [ApiEnvelopeParser]（与 `KtorApiClient` 同一条解析路径），
 * 这样测试覆盖的是「请求形状 + 响应解码」，不碰网络。
 */
internal class MineRecordingApiClient(
    private var raw: String = DEFAULT_EMPTY_OK,
) : ApiClient {

    /** 一次调用的记录（method、path、query、body）。 */
    internal data class Call(
        val method: String,
        val path: String,
        val query: Map<String, String>,
        val body: String,
    )

    val calls: MutableList<Call> = mutableListOf()

    /** 最近一次调用；断言请求形状用。 */
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
    ): ApiResponse<T> =
        ApiEnvelopeParser.parse(getRaw(path, query), parseData)

    override suspend fun <T> postApi(
        path: String,
        body: String,
        parseData: (JsonElement?) -> T?,
    ): ApiResponse<T> =
        ApiEnvelopeParser.parse(postRaw(path, body), parseData)

    override suspend fun <T> putApi(
        path: String,
        body: String,
        parseData: (JsonElement?) -> T?,
    ): ApiResponse<T> =
        ApiEnvelopeParser.parse(putRaw(path, body), parseData)

    override suspend fun <T> patchApi(
        path: String,
        body: String,
        parseData: (JsonElement?) -> T?,
    ): ApiResponse<T> =
        ApiEnvelopeParser.parse(patchRaw(path, body), parseData)

    override suspend fun <T> deleteApi(
        path: String,
        query: Map<String, String>,
        parseData: (JsonElement?) -> T?,
    ): ApiResponse<T> =
        ApiEnvelopeParser.parse(deleteRaw(path), parseData)

    private companion object {
        /** 默认体：`code == 0` 且 `data` 为 null（多数用例都会先 stub 真实体）。 */
        const val DEFAULT_EMPTY_OK: String = """{"code":0,"message":"ok","data":null}"""
    }
}

package com.example.my_kmp_project.feature.auth

import com.example.my_kmp_project.core.network.ApiClient
import com.example.my_kmp_project.core.network.ApiEnvelopeParser
import com.example.my_kmp_project.core.network.ApiResponse
import com.example.my_kmp_project.core.network.HttpTextResponse
import kotlinx.serialization.json.JsonElement

/**
 * B 槽契约测试用假 client：记录 verb/path/query/body，并走真实 [ApiEnvelopeParser]
 * 解析信封。与 feature/pay 的 FakePayClient 同款，独立一份避免跨 Gradle 模块的
 * test 依赖（feature→feature import 会被 check-layer-deps 拦截）。
 */
class FakeAuthApiClient(
    private val responder: (verb: String, path: String, query: Map<String, String>, body: String) -> String,
) : ApiClient {
    data class Call(
        val verb: String,
        val path: String,
        val query: Map<String, String> = emptyMap(),
        val body: String = "",
    )

    val calls = mutableListOf<Call>()

    private fun <T> parse(
        verb: String,
        path: String,
        query: Map<String, String>,
        body: String,
        parseData: (JsonElement?) -> T?,
    ): ApiResponse<T> {
        calls += Call(verb, path, query, body)
        return ApiEnvelopeParser.parse(
            raw = responder(verb, path, query, body),
            parseData = parseData,
            httpStatus = 200,
        )
    }

    override suspend fun getRaw(path: String, query: Map<String, String>): String {
        calls += Call("GET", path, query)
        return responder("GET", path, query, "")
    }

    override suspend fun getAbsoluteRaw(url: String): String {
        calls += Call("GET", url)
        return responder("GET", url, emptyMap(), "")
    }

    override suspend fun postRaw(path: String, body: String): String {
        calls += Call("POST", path, body = body)
        return responder("POST", path, emptyMap(), body)
    }

    override suspend fun postForm(path: String, fields: Map<String, String>): HttpTextResponse {
        calls += Call("POST-FORM", path)
        return HttpTextResponse(200, "")
    }

    override suspend fun putRaw(path: String, body: String): String {
        calls += Call("PUT", path, body = body)
        return responder("PUT", path, emptyMap(), body)
    }

    override suspend fun patchRaw(path: String, body: String): String {
        calls += Call("PATCH", path, body = body)
        return responder("PATCH", path, emptyMap(), body)
    }

    override suspend fun deleteRaw(path: String): String {
        calls += Call("DELETE", path)
        return responder("DELETE", path, emptyMap(), "")
    }

    override suspend fun <T> getApi(
        path: String,
        query: Map<String, String>,
        parseData: (JsonElement?) -> T?,
    ): ApiResponse<T> = parse("GET", path, query, "", parseData)

    override suspend fun <T> postApi(
        path: String,
        body: String,
        parseData: (JsonElement?) -> T?,
    ): ApiResponse<T> = parse("POST", path, emptyMap(), body, parseData)

    override suspend fun <T> putApi(
        path: String,
        body: String,
        parseData: (JsonElement?) -> T?,
    ): ApiResponse<T> = parse("PUT", path, emptyMap(), body, parseData)

    override suspend fun <T> patchApi(
        path: String,
        body: String,
        parseData: (JsonElement?) -> T?,
    ): ApiResponse<T> = parse("PATCH", path, emptyMap(), body, parseData)

    override suspend fun <T> deleteApi(
        path: String,
        query: Map<String, String>,
        parseData: (JsonElement?) -> T?,
    ): ApiResponse<T> = parse("DELETE", path, query, "", parseData)
}

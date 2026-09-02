package com.example.my_kmp_project.core.network

import kotlinx.serialization.json.JsonElement

internal class OhosApiClient(
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

internal actual fun createPlatformApiClient(
    onTokenExpired: TokenExpiredHandler?,
    businessHandlers: NetworkBusinessHandlers?,
): ApiClient = OhosApiClient(onTokenExpired, businessHandlers)

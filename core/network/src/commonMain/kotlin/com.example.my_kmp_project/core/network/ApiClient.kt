package com.example.my_kmp_project.core.network

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

public interface ApiClient {
    suspend fun getRaw(path: String, query: Map<String, String> = emptyMap()): String
    /**
     * GET an absolute URL (no business host). Used for COS assets such as countrycode.json.
     */
    suspend fun getAbsoluteRaw(url: String): String
    suspend fun postRaw(path: String, body: String = ""): String
    /** POST `application/x-www-form-urlencoded`; returns status + raw body (no envelope parse). */
    suspend fun postForm(path: String, fields: Map<String, String>): HttpTextResponse
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

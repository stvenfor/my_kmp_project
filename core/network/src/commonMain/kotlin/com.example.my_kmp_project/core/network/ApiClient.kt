package com.example.my_kmp_project.core.network

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

package com.example.my_kmp_project.core.network

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Parameters
import io.ktor.http.contentType
import io.ktor.http.formUrlEncode
import kotlinx.serialization.json.JsonElement

internal class KtorApiClient(
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
            throw NetworkError.Transport(t.message ?: "GET failed")
        }
    }

    override suspend fun getAbsoluteRaw(url: String): String {
        return try {
            http.get(url).bodyAsText()
        } catch (t: Throwable) {
            throw NetworkError.Transport(t.message ?: "GET absolute failed")
        }
    }

    override suspend fun postRaw(path: String, body: String): String {
        return try {
            http.post(path.trimStart('/')) {
                if (body.isNotEmpty()) setBody(body)
            }.bodyAsText()
        } catch (t: Throwable) {
            throw NetworkError.Transport(t.message ?: "POST failed")
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
            throw NetworkError.Transport(t.message ?: "FORM POST failed")
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
            throw NetworkError.Transport(t.message ?: "GET failed")
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
            throw NetworkError.Transport(t.message ?: "POST failed")
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

package com.example.my_kmp_project.feature.media

import com.example.my_kmp_project.core.network.ApiClient
import com.example.my_kmp_project.core.network.ApiEnvelopeParser
import com.example.my_kmp_project.core.network.ApiResponse
import com.example.my_kmp_project.core.network.HttpTextResponse
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonElement
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * `POST /api/v1/short-videos/:id/review` 契约回归（`short_video/router.go:49`，B 区 #14）。
 * 只断言请求形状（路径 / HTTP verb / 请求体字段名 `action`），不接 UI。
 */
class ShortVideoReviewApiContractTest {

    @Test
    fun reviewPathMatchesBackendRoute() {
        assertEquals("/api/v1/short-videos/7/review", ShortVideoReviewPaths.review("7"))
    }

    @Test
    fun reviewPostsActionApprove() = runBlocking {
        val fake = RecordingApiClient()
        ShortVideoReviewApi(fake).review("7", ShortVideoReviewRequest())
        val call = fake.calls.single()
        assertEquals("POST", call.verb)
        assertEquals("/api/v1/short-videos/7/review", call.path)
        // short_video/handler.go:171-173：唯一合法值 approve。
        assertEquals("""{"action":"approve"}""", call.body)
    }

    /** 只实装 review 用到的低层 postApi 的假 client（其余方法不被契约层调用）。 */
    private class RecordingApiClient : ApiClient {
        data class Call(val verb: String, val path: String, val body: String)

        val calls = mutableListOf<Call>()
        private val raw = """{"code":0,"message":"ok","data":{}}"""

        override suspend fun <T> postApi(
            path: String,
            body: String,
            parseData: (JsonElement?) -> T?,
        ): ApiResponse<T> {
            calls += Call("POST", path, body)
            return ApiEnvelopeParser.parse(raw, parseData)
        }

        override suspend fun getRaw(path: String, query: Map<String, String>): String = unused("getRaw")
        override suspend fun getAbsoluteRaw(url: String): String = unused("getAbsoluteRaw")
        override suspend fun postRaw(path: String, body: String): String = unused("postRaw")
        override suspend fun postForm(path: String, fields: Map<String, String>): HttpTextResponse = unused("postForm")
        override suspend fun putRaw(path: String, body: String): String = unused("putRaw")
        override suspend fun patchRaw(path: String, body: String): String = unused("patchRaw")
        override suspend fun deleteRaw(path: String): String = unused("deleteRaw")

        override suspend fun <T> getApi(
            path: String,
            query: Map<String, String>,
            parseData: (JsonElement?) -> T?,
        ): ApiResponse<T> = unused("getApi")

        override suspend fun <T> putApi(
            path: String,
            body: String,
            parseData: (JsonElement?) -> T?,
        ): ApiResponse<T> = unused("putApi")

        override suspend fun <T> patchApi(
            path: String,
            body: String,
            parseData: (JsonElement?) -> T?,
        ): ApiResponse<T> = unused("patchApi")

        override suspend fun <T> deleteApi(
            path: String,
            query: Map<String, String>,
            parseData: (JsonElement?) -> T?,
        ): ApiResponse<T> = unused("deleteApi")

        private fun unused(name: String): Nothing = error("review 契约不应调用 $name")
    }
}

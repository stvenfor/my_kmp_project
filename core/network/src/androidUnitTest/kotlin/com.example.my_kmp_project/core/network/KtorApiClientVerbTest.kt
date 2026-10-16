package com.example.my_kmp_project.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * `KtorApiClient` 动词与查询串是否真的落到 HTTP 层
 * （Flutter `/api/v1` 里 PATCH 4 个、DELETE 7 个、GET 分页带 `page/size`；
 * 见 my_ai_project features/auth/lib/api/user_profile_api.dart:59 等）。
 */
class KtorApiClientVerbTest {

    private val recorded = mutableListOf<String>()

    private fun mockClient(): HttpClient = HttpClient(MockEngine) {
        engine {
            addHandler { request ->
                val query = request.url.encodedQuery
                recorded += "${request.method.value} ${request.url.encodedPath}" +
                    if (query.isNullOrEmpty()) "" else "?$query"
                respondOk("""{"code":0,"message":"ok","data":"d"}""")
            }
        }
    }

    @Test
    fun put_patch_delete_use_matching_http_verbs() = runBlocking {
        val api = KtorApiClient(mockClient())
        val put = api.putApi("/api/v1/profiles/me", """{"user_name":"n"}""") { it?.jsonPrimitive?.contentOrNull }
        val patch = api.patchApi("/api/v1/user/addresses/3", """{"city":"SH"}""") { it?.jsonPrimitive?.contentOrNull }
        val delete = api.deleteApi<String>("/api/v1/community/posts/7")

        assertEquals("d", put.data)
        assertEquals("d", patch.data)
        assertEquals("d", delete.data)
        // BFF 成功码 0 必须判成功（遗留 200 表兼容，见 NetworkCodes）。
        assertTrue(put.isSuccess && patch.isSuccess && delete.isSuccess)
        assertEquals(
            listOf(
                "PUT /api/v1/profiles/me",
                "PATCH /api/v1/user/addresses/3",
                "DELETE /api/v1/community/posts/7",
            ),
            recorded,
        )
    }

    @Test
    fun delete_carries_query_and_get_page_query_is_encoded() = runBlocking {
        val api = KtorApiClient(mockClient())
        api.deleteApi<String>("/api/v1/wallet/cards/9", mapOf("reason" to "user"))
        api.getListPage<String>("/api/v1/mall/stores/1/products", pageQuery(page = 2, size = 10, "status" to null))

        assertEquals(
            listOf(
                "DELETE /api/v1/wallet/cards/9?reason=user",
                "GET /api/v1/mall/stores/1/products?page=2&size=10",
            ),
            recorded,
        )
    }
}

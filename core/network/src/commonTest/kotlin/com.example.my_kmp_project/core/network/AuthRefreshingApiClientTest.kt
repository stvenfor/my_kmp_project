package com.example.my_kmp_project.core.network

import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Flutter `AuthTokenRefreshInterceptor` 语义的 KMP 对等测试
 * （my_ai_project features/auth/lib/session/auth_token_refresh_interceptor.dart，d7876a1）。
 */
class AuthRefreshingApiClientTest {

    /** 可编程假 client：FIFO 返回预制应答；记录调用路径。 */
    private class FakeApiClient(
        responses: List<ApiResponse<String>>,
    ) : ApiClient {
        private val queue = ArrayDeque(responses)
        val postPaths = mutableListOf<String>()
        val getPaths = mutableListOf<String>()
        val writePaths = mutableListOf<String>()

        override suspend fun getRaw(path: String, query: Map<String, String>): String = ""
        override suspend fun getAbsoluteRaw(url: String): String = ""
        override suspend fun postRaw(path: String, body: String): String = ""
        override suspend fun postForm(path: String, fields: Map<String, String>): HttpTextResponse =
            HttpTextResponse(200, "")
        override suspend fun putRaw(path: String, body: String): String = ""
        override suspend fun patchRaw(path: String, body: String): String = ""
        override suspend fun deleteRaw(path: String): String = ""

        override suspend fun <T> getApi(
            path: String,
            query: Map<String, String>,
            parseData: (JsonElement?) -> T?,
        ): ApiResponse<T> {
            getPaths += path
            return drain()
        }

        override suspend fun <T> postApi(
            path: String,
            body: String,
            parseData: (JsonElement?) -> T?,
        ): ApiResponse<T> {
            postPaths += path
            writePaths += path
            return drain()
        }

        override suspend fun <T> putApi(
            path: String,
            body: String,
            parseData: (JsonElement?) -> T?,
        ): ApiResponse<T> {
            writePaths += path
            return drain()
        }

        override suspend fun <T> patchApi(
            path: String,
            body: String,
            parseData: (JsonElement?) -> T?,
        ): ApiResponse<T> {
            writePaths += path
            return drain()
        }

        override suspend fun <T> deleteApi(
            path: String,
            query: Map<String, String>,
            parseData: (JsonElement?) -> T?,
        ): ApiResponse<T> {
            writePaths += path
            return drain()
        }

        @Suppress("UNCHECKED_CAST")
        private fun <T> drain(): ApiResponse<T> =
            queue.removeFirstOrNull() as ApiResponse<T>? ?: ApiResponse(NetworkCodes.OK, "drained")
    }

    private fun ok(tag: String): ApiResponse<String> =
        ApiResponse(NetworkCodes.OK, "ok", "data-$tag")

    private fun expired(status: Int? = null, code: Int = NetworkCodes.EXPIRE_TOKEN): ApiResponse<String> =
        ApiResponse(code, "登录已失效", null, httpStatus = status)

    private fun forbidden(): ApiResponse<String> =
        ApiResponse(NetworkCodes.FORBIDDEN, "无权限", null)

    /** feature/auth `classifyAuthHttp` 的最小对等（供 core 层测试注入）。 */
    private val flutterPolicy = AuthRefreshPolicy { statusCode, code, _ ->
        when {
            code == 10021 || code == 10022 -> AuthRefreshDecision.ForceLogout
            code == 10023 || statusCode == NetworkCodes.EXPIRE_TOKEN ||
                code == NetworkCodes.EXPIRE_TOKEN -> AuthRefreshDecision.Refresh
            else -> AuthRefreshDecision.PassThrough
        }
    }

    @Test
    fun http_401_refreshes_and_retries_once() = runBlocking {
        val delegate = FakeApiClient(listOf(expired(status = NetworkCodes.EXPIRE_TOKEN), ok("retried")))
        var refreshCalls = 0
        val client = AuthRefreshingApiClient(
            delegate = delegate,
            policy = flutterPolicy,
            refresher = TokenRefreshHandler { refreshCalls++; "new-token" },
        )
        val resp = client.postApi<String>("/api/v1/chat/list", "{}") { null }
        assertEquals(NetworkCodes.OK, resp.code)
        assertEquals("data-retried", resp.data)
        assertEquals(1, refreshCalls)
        assertEquals(2, delegate.postPaths.size) // 原请求 + 重试
    }

    @Test
    fun business_code_10023_refreshes() = runBlocking {
        val delegate = FakeApiClient(listOf(expired(code = 10023), ok("after")))
        val client = AuthRefreshingApiClient(delegate, flutterPolicy, TokenRefreshHandler { "t" })
        val resp = client.postApi<String>("/api/v1/mine/profile", "{}") { null }
        assertEquals(NetworkCodes.OK, resp.code)
        assertEquals(2, delegate.postPaths.size)
    }

    @Test
    fun concurrent_401s_both_succeed_with_refresh() = runBlocking {
        // 确定性并发：前两次 postApi（两条原始请求）→ 401；其后（两条重试）→ ok。
        // 单飞去重由 feature 层协调器负责，这里只验证装饰器不丢请求。
        var callCount = 0
        val delegate = object : ApiClient {
            val postPaths = mutableListOf<String>()
            override suspend fun getRaw(path: String, query: Map<String, String>): String = ""
            override suspend fun getAbsoluteRaw(url: String): String = ""
            override suspend fun postRaw(path: String, body: String): String = ""
            override suspend fun postForm(path: String, fields: Map<String, String>): HttpTextResponse =
                HttpTextResponse(200, "")
            override suspend fun putRaw(path: String, body: String): String = ""
            override suspend fun patchRaw(path: String, body: String): String = ""
            override suspend fun deleteRaw(path: String): String = ""

            override suspend fun <T> getApi(
                path: String,
                query: Map<String, String>,
                parseData: (JsonElement?) -> T?,
            ): ApiResponse<T> = respond()

            override suspend fun <T> postApi(
                path: String,
                body: String,
                parseData: (JsonElement?) -> T?,
            ): ApiResponse<T> {
                postPaths += path
                return respond()
            }

            override suspend fun <T> putApi(
                path: String,
                body: String,
                parseData: (JsonElement?) -> T?,
            ): ApiResponse<T> = respond()

            override suspend fun <T> patchApi(
                path: String,
                body: String,
                parseData: (JsonElement?) -> T?,
            ): ApiResponse<T> = respond()

            override suspend fun <T> deleteApi(
                path: String,
                query: Map<String, String>,
                parseData: (JsonElement?) -> T?,
            ): ApiResponse<T> = respond()

            @Suppress("UNCHECKED_CAST")
            private fun <T> respond(): ApiResponse<T> {
                callCount++
                return if (callCount <= 2) {
                    expired(status = NetworkCodes.EXPIRE_TOKEN)
                } else {
                    ok("r$callCount")
                } as ApiResponse<T>
            }
        }
        var refreshCalls = 0
        val client = AuthRefreshingApiClient(
            delegate = delegate,
            policy = flutterPolicy,
            refresher = TokenRefreshHandler {
                refreshCalls++
                delay(50) // 模拟网络往返
                "new-token"
            },
        )
        val a = async { client.postApi<String>("/api/v1/chat/list", "{}") { null } }
        val b = async { client.postApi<String>("/api/v1/chat/unread", "{}") { null } }
        assertEquals(NetworkCodes.OK, a.await().code)
        assertEquals(NetworkCodes.OK, b.await().code)
        assertTrue(refreshCalls >= 1)
        assertEquals(4, delegate.postPaths.size) // 2 原始 + 2 重试
    }

    @Test
    fun login_path_skips_refresh() = runBlocking {
        val delegate = FakeApiClient(listOf(expired(status = NetworkCodes.EXPIRE_TOKEN)))
        var refreshCalls = 0
        val client = AuthRefreshingApiClient(
            delegate = delegate,
            policy = flutterPolicy,
            refresher = TokenRefreshHandler { refreshCalls++; "t" },
        )
        val resp = client.postApi<String>("/api/v1/user/login", "{}") { null }
        assertEquals(NetworkCodes.EXPIRE_TOKEN, resp.code)
        assertEquals(0, refreshCalls)
        assertEquals(1, delegate.postPaths.size)
    }

    @Test
    fun force_logout_code_clears_session_without_refresh() = runBlocking {
        val delegate = FakeApiClient(listOf(ApiResponse(10021, "账号已在其他设备登录", null)))
        var refreshCalls = 0
        var cleared = false
        val client = AuthRefreshingApiClient(
            delegate = delegate,
            policy = flutterPolicy,
            refresher = TokenRefreshHandler { refreshCalls++; "t" },
            onSessionCleared = { cleared = true },
        )
        val resp = client.postApi<String>("/api/v1/chat/list", "{}") { null }
        assertEquals(10021, resp.code)
        assertEquals(0, refreshCalls)
        assertTrue(cleared)
    }

    @Test
    fun forbidden_403_passes_through_without_refresh() = runBlocking {
        val delegate = FakeApiClient(listOf(forbidden()))
        var refreshCalls = 0
        var cleared = false
        val client = AuthRefreshingApiClient(
            delegate = delegate,
            policy = flutterPolicy,
            refresher = TokenRefreshHandler { refreshCalls++; "t" },
            onSessionCleared = { cleared = true },
        )
        val resp = client.postApi<String>("/api/v1/mine/invoice", "{}") { null }
        assertEquals(NetworkCodes.FORBIDDEN, resp.code)
        assertEquals(0, refreshCalls)
        assertTrue(!cleared)
    }

    @Test
    fun refresh_failure_returns_original_401() = runBlocking {
        val delegate = FakeApiClient(listOf(expired(status = NetworkCodes.EXPIRE_TOKEN)))
        val client = AuthRefreshingApiClient(
            delegate = delegate,
            policy = flutterPolicy,
            refresher = TokenRefreshHandler { error("refresh exhausted") },
        )
        val resp = client.postApi<String>("/api/v1/chat/list", "{}") { null }
        assertEquals(NetworkCodes.EXPIRE_TOKEN, resp.code)
        assertEquals(1, delegate.postPaths.size) // 不重试
    }

    @Test
    fun no_policy_configured_passes_everything_through() = runBlocking {
        val delegate = FakeApiClient(listOf(expired(status = NetworkCodes.EXPIRE_TOKEN)))
        val client = AuthRefreshingApiClient(delegate)
        val resp = client.postApi<String>("/api/v1/chat/list", "{}") { null }
        assertEquals(NetworkCodes.EXPIRE_TOKEN, resp.code)
        assertEquals(1, delegate.postPaths.size)
    }

    @Test
    fun get_api_also_refreshes() = runBlocking {
        val delegate = FakeApiClient(listOf(expired(status = NetworkCodes.EXPIRE_TOKEN), ok("g")))
        val client = AuthRefreshingApiClient(delegate, flutterPolicy, TokenRefreshHandler { "t" })
        val resp = client.getApi<String>("/api/v1/home/feed", emptyMap()) { null }
        assertEquals(NetworkCodes.OK, resp.code)
        assertEquals(2, delegate.getPaths.size)
    }

    @Test
    fun put_patch_delete_also_refresh_and_retry_once() = runBlocking {
        val delegate = FakeApiClient(
            listOf(
                expired(status = NetworkCodes.EXPIRE_TOKEN), ok("p"),
                expired(status = NetworkCodes.EXPIRE_TOKEN), ok("q"),
                expired(status = NetworkCodes.EXPIRE_TOKEN), ok("d"),
            ),
        )
        var refreshCalls = 0
        val client = AuthRefreshingApiClient(
            delegate,
            flutterPolicy,
            TokenRefreshHandler { refreshCalls++; "t" },
        )
        assertEquals("data-p", client.putApi<String>("/api/v1/mine/profile", "{}") { null }.data)
        assertEquals("data-q", client.patchApi<String>("/api/v1/mine/nickname", "{}") { null }.data)
        assertEquals("data-d", client.deleteApi<String>("/api/v1/mine/address", emptyMap()) { null }.data)
        assertEquals(3, refreshCalls)
        // 3 条原始请求 + 3 条重试
        assertEquals(6, delegate.writePaths.size)
    }

    @Test
    fun raw_verbs_never_trigger_refresh() = runBlocking {
        val delegate = FakeApiClient(emptyList())
        var refreshCalls = 0
        val client = AuthRefreshingApiClient(
            delegate,
            flutterPolicy,
            TokenRefreshHandler { refreshCalls++; "t" },
        )
        assertEquals("", client.putRaw("/api/v1/mine/profile", "{}"))
        assertEquals("", client.patchRaw("/api/v1/mine/profile", "{}"))
        assertEquals("", client.deleteRaw("/api/v1/mine/address"))
        assertEquals(0, refreshCalls)
        assertTrue(delegate.writePaths.isEmpty())
    }
}

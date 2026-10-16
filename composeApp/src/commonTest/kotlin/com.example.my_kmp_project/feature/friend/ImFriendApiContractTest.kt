package com.example.my_kmp_project.feature.friend

import com.example.my_kmp_project.core.network.ApiClient
import com.example.my_kmp_project.core.network.ApiEnvelopeParser
import com.example.my_kmp_project.core.network.ApiResponse
import com.example.my_kmp_project.core.network.HttpTextResponse
import com.example.my_kmp_project.core.network.NetworkError
import com.example.my_kmp_project.core.network.NetworkJson
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 契约表 §2.18 行 #103 / #104 / #105 / #106 / #107 / #108 的契约测试。
 * 对应 Dart：`components/rongcloud_im/lib/api/im_friend_api.dart`。
 */
class ImFriendApiContractTest {

    @Test
    fun list_friends_reads_items_array_and_tolerates_aliases() = runNow {
        val client = FakeFriendApiClient {
            ok(
                """{"items":[
                    {"user_id":"f1","display_name":"甲","avatar_url":"https://x/1.png","phone_masked":"134****0001"},
                    {"im_user_id":"f2","displayName":"乙","avatarUrl":"https://x/2.png"},
                    "跳过非对象"
                ]}""",
            )
        }
        val friends = ImFriendApi(client).listFriends()

        assertEquals(ImFriendApiPaths.FRIENDS_PATH, client.calls.single().path)
        assertEquals("GET", client.calls.single().method)
        assertEquals(
            listOf(
                ImFriendUser("f1", "甲", "https://x/1.png", "134****0001"),
                // 契约表要求 im_user_id / camelCase 别名在解码时被容忍
                ImFriendUser("f2", "乙", "https://x/2.png", ""),
            ),
            friends,
        )
    }

    @Test
    fun incoming_requests_default_status_to_pending() = runNow {
        val client = FakeFriendApiClient {
            ok("""{"items":[{"id":"r1","from_user_id":"f1","display_name":"甲","created_at":"2026-10-16T10:00:00Z"}]}""")
        }
        val requests = ImFriendApi(client).listIncomingRequests()

        assertEquals(ImFriendApiPaths.FRIEND_REQUESTS_PATH, client.calls.single().path)
        assertEquals(
            listOf(ImFriendRequest("r1", "f1", "甲", "", "", "pending", "2026-10-16T10:00:00Z")),
            requests,
        )
        // status 显式回传时原样保留
        val explicit = FakeFriendApiClient { ok("""{"items":[{"id":"r2","status":"accepted"}]}""") }
        assertEquals("accepted", ImFriendApi(explicit).listIncomingRequests().single().status)
    }

    @Test
    fun search_always_sends_q_even_when_blank() = runNow {
        val client = FakeFriendApiClient { ok("""{"items":[]}""") }
        val hits = ImFriendApi(client).search("")

        assertEquals(ImFriendApiPaths.USERS_SEARCH_PATH, client.calls.single().path)
        // Dart 原样 `{'q': q}`（im_friend_api.dart:78）——空串也要发
        assertEquals(mapOf("q" to ""), client.calls.single().query)
        assertEquals(0, hits.size)
    }

    @Test
    fun list_failures_throw_business_error_with_envelope_message() = runNow {
        val client = FakeFriendApiClient { """{"code":500,"message":"boom","data":null}""" }
        val error = assertFailsWith<NetworkError.Business> { ImFriendApi(client).listFriends() }
        assertEquals(500, error.code)
        assertEquals("boom", error.message)

        val malformed = FakeFriendApiClient { """{"message":"no code"}""" }
        assertFailsWith<NetworkError.Business> { ImFriendApi(malformed).listIncomingRequests() }
    }

    @Test
    fun request_friend_sends_to_user_id_as_string() = runNow {
        val client = FakeFriendApiClient { ok("{}") }
        ImFriendApi(client).requestFriend("u7")

        assertEquals("POST", client.calls.single().method)
        assertEquals(ImFriendApiPaths.FRIEND_REQUESTS_PATH, client.calls.single().path)
        assertEquals("""{"to_user_id":"u7"}""", client.calls.single().body)
        assertEquals(setOf("to_user_id"), client.calls.single().body.asJsonObject().keys)
    }

    /**
     * 契约表 #105 —— `POST /im/friends/requests`，body `{to_user_id: String}`，**响应整个忽略**：
     * Dart `requestFriend` 只判 `result.data?.isSuccess != true`（im_friend_api.dart:149），
     * 既不读 `data` 也不要求它是对象，故三种 data 形状都必须算成功且只发一次请求。
     */
    @Test
    fun row_105_request_friend_ignores_the_response_whatever_data_looks_like() = runNow {
        listOf(
            ok("""{"request_id":"r9","status":"pending"}"""),
            """{"code":0,"message":"ok","data":null}""",
            """{"code":0,"message":"ok","data":[1,2]}""",
        ).forEach { raw ->
            val client = FakeFriendApiClient { raw }
            ImFriendApi(client).requestFriend("u7")

            val call = client.calls.single()
            assertEquals("POST", call.method)
            assertEquals(ImFriendApiPaths.FRIEND_REQUESTS_PATH, call.path)
            assertEquals("""{"to_user_id":"u7"}""", call.body)
            assertEquals(setOf("to_user_id"), call.body.asJsonObject().keys)
        }
    }

    /**
     * 契约表 #105 的英文转中文那一列：Dart 把映射放在**调用点**
     * （`features/friend/lib/friend/view/friend_page.dart:104-114`：
     * `await _api.requestFriend(u.userId)` 外面包 try/catch，
     * catch 里 `_toast(ImFriendApi.friendlyError(e, fallback: '申请失败'))`）。
     * 所以本端点抛出的必须是**原始英文** message（§5.13 服务端文案优先），
     * 中文由 [ImFriendApi.friendlyError] 在调用点翻译。
     */
    @Test
    fun row_105_english_errors_stay_raw_until_the_call_site_maps_them() = runNow {
        val cases = listOf(
            "already pending" to "已发送过申请，等待对方同意",
            "You are already friends with this user" to "你们已经是好友了",
            "cannot friend yourself" to "不能添加自己",
            "User Not Found" to "用户不存在",
            "you are not the request recipient" to "无权处理该申请",
        )
        cases.forEach { (english, chinese) ->
            val client = FakeFriendApiClient { """{"code":400,"message":"$english","data":null}""" }
            val error = assertFailsWith<NetworkError.Business> { ImFriendApi(client).requestFriend("u7") }

            assertEquals(english, error.message)
            assertEquals(chinese, ImFriendApi.friendlyError(error, fallback = "申请失败"))
        }

        // data 是对象但 code 非 0 时依然失败（Dart 只看 code，:149）
        val rejected = FakeFriendApiClient { """{"code":409,"message":"already pending","data":{"ignored":true}}""" }
        val conflict = assertFailsWith<NetworkError.Business> { ImFriendApi(rejected).requestFriend("u7") }
        assertEquals(409, conflict.code)
        assertEquals("已发送过申请，等待对方同意", ImFriendApi.friendlyError(conflict, fallback = "申请失败"))

        // 服务端没给 message ⇒ 兜底英文位换成 Dart 的 '申请失败'（im_friend_api.dart:151），
        // 此时 friendlyError 看到的 raw 非空、原样透出，与 Dart 完全一致
        val silent = FakeFriendApiClient { """{"code":500,"data":null}""" }
        val silentError = assertFailsWith<NetworkError.Business> { ImFriendApi(silent).requestFriend("u7") }
        assertEquals("申请失败", silentError.message)
        assertEquals("申请失败", ImFriendApi.friendlyError(silentError, fallback = "申请失败"))
    }

    @Test
    fun respond_friend_uses_one_endpoint_for_accept_and_reject() = runNow {
        val client = FakeFriendApiClient { ok("{}") }
        val api = ImFriendApi(client)

        api.respondFriend("r1", accept = true)
        api.respondFriend("r1", accept = false)

        assertEquals(2, client.calls.size)
        assertEquals(ImFriendApiPaths.respondPath("r1"), client.calls[0].path)
        assertEquals("/api/v1/im/friends/requests/r1/respond", client.calls[0].path)
        assertEquals("""{"accept":true}""", client.calls[0].body)
        assertEquals("""{"accept":false}""", client.calls[1].body)
    }

    @Test
    fun request_and_respond_failures_throw_raw_english_message_for_friendlyError() = runNow {
        val client = FakeFriendApiClient { """{"code":409,"message":"already pending","data":null}""" }
        val api = ImFriendApi(client)

        val error = assertFailsWith<NetworkError.Business> { api.requestFriend("u7") }
        assertEquals("already pending", error.message)
        assertEquals("已发送过申请，等待对方同意", ImFriendApi.friendlyError(error))

        // 同意/拒绝共用端点，抛出的仍是原始英文 message，交给 friendlyError 映射
        val error2 = assertFailsWith<NetworkError.Business> { api.respondFriend("r1", accept = true) }
        assertEquals("already pending", error2.message)
        assertEquals("已发送过申请，等待对方同意", ImFriendApi.friendlyError(error2, fallback = "处理失败"))
        assertEquals(2, client.calls.size)
    }

    @Test
    fun private_admission_is_fail_closed() = runNow {
        // 只有布尔 true 才放行
        assertEquals(
            true,
            ImFriendApi(FakeFriendApiClient { ok("""{"allowed":true}""") }).canPrivateChat("p1"),
        )
        mapOf(
            "布尔 false" to ok("""{"allowed":false}"""),
            "字符串 true" to ok("""{"allowed":"true"}"""),
            "缺 allowed" to ok("""{"other":1}"""),
            "data 为 null" to """{"code":0,"message":"ok","data":null}""",
            "业务失败码" to """{"code":403,"message":"no","data":{"allowed":true}}""",
        ).forEach { (label, raw) ->
            val api = ImFriendApi(FakeFriendApiClient { raw })
            assertFalse(api.canPrivateChat("p1"), label)
        }
        // 传输异常也返回 false
        val throwing = FakeFriendApiClient { throw NetworkError.Transport("socket died") }
        assertFalse(ImFriendApi(throwing).canPrivateChat("p1"))

        // query key 固定 peer_user_id（im_friend_api.dart:177）
        val admitted = FakeFriendApiClient { ok("""{"allowed":true}""") }
        assertTrue(ImFriendApi(admitted).canPrivateChat("p1"))
        assertEquals(mapOf("peer_user_id" to "p1"), admitted.calls.single().query)
        assertEquals(ImFriendApiPaths.PRIVATE_ADMISSION_PATH, admitted.calls.single().path)
    }

    @Test
    fun friendly_error_mapping_table_matches_dart() {
        val cases = listOf(
            "already pending" to "已发送过申请，等待对方同意",
            "Already Pending, try later" to "已发送过申请，等待对方同意",
            "already friends" to "你们已经是好友了",
            "cannot friend yourself" to "不能添加自己",
            "user not found" to "用户不存在",
            "not the request recipient" to "无权处理该申请",
        )
        cases.forEach { (raw, expected) -> assertEquals(expected, ImFriendApi.friendlyMessage(raw)) }

        // 兜底：空串 / HttpRequestException 前缀 → fallback（im_friend_api.dart:69-71）
        assertEquals("操作失败", ImFriendApi.friendlyMessage(""))
        assertEquals("操作失败", ImFriendApi.friendlyMessage("   "))
        assertEquals("操作失败", ImFriendApi.friendlyMessage("HttpRequestException: boom"))
        // 其它消息原样透出
        assertEquals("服务器繁忙", ImFriendApi.friendlyMessage("服务器繁忙"))
        assertEquals("操作失败", ImFriendApi.friendlyError(null))
        assertEquals(
            "用户不存在",
            ImFriendApi.friendlyError(NetworkError.Business(404, "User Not Found")),
        )
        // blank message 的 NetworkError → fallback（Dart raw.trim().isEmpty()）
        assertEquals(
            "自定义兜底",
            ImFriendApi.friendlyError(NetworkError.Transport("   "), fallback = "自定义兜底"),
        )
        // 非 NetworkError 走 toString（Dart `e.toString()`，im_friend_api.dart:62）
        assertEquals(
            "用户不存在",
            ImFriendApi.friendlyError(RuntimeException("user not found")),
        )
        // 传输错误带可读 message 时原样透出（Dart 的 HttpRequestException.message 分支）
        assertEquals("连接超时", ImFriendApi.friendlyError(NetworkError.Transport("连接超时")))
    }

    private fun ok(dataJson: String): String = """{"code":0,"message":"ok","data":$dataJson}"""
}

/** 记录型假 client（friend lane 自带一份，避免跨 feature import）。 */
private class FakeFriendApiClient(
    private val responder: (FriendCall) -> String,
) : ApiClient {
    val calls: MutableList<FriendCall> = mutableListOf()

    private fun reply(
        method: String,
        path: String,
        body: String,
        query: Map<String, String> = emptyMap(),
    ): String {
        val call = FriendCall(method, path, body, query)
        calls += call
        return responder(call)
    }

    override suspend fun getRaw(path: String, query: Map<String, String>): String =
        reply("GET", path, "", query)

    override suspend fun getAbsoluteRaw(url: String): String = reply("GET", url, "")

    override suspend fun postRaw(path: String, body: String): String = reply("POST", path, body)

    override suspend fun postForm(path: String, fields: Map<String, String>): HttpTextResponse =
        error("postForm unused in IM contracts")

    override suspend fun putRaw(path: String, body: String): String = reply("PUT", path, body)

    override suspend fun patchRaw(path: String, body: String): String = reply("PATCH", path, body)

    override suspend fun deleteRaw(path: String): String = reply("DELETE", path, "")

    override suspend fun <T> getApi(
        path: String,
        query: Map<String, String>,
        parseData: (JsonElement?) -> T?,
    ): ApiResponse<T> = ApiEnvelopeParser.parse(reply("GET", path, "", query), parseData)

    override suspend fun <T> postApi(
        path: String,
        body: String,
        parseData: (JsonElement?) -> T?,
    ): ApiResponse<T> = ApiEnvelopeParser.parse(reply("POST", path, body), parseData)

    override suspend fun <T> putApi(
        path: String,
        body: String,
        parseData: (JsonElement?) -> T?,
    ): ApiResponse<T> = ApiEnvelopeParser.parse(reply("PUT", path, body), parseData)

    override suspend fun <T> patchApi(
        path: String,
        body: String,
        parseData: (JsonElement?) -> T?,
    ): ApiResponse<T> = ApiEnvelopeParser.parse(reply("PATCH", path, body), parseData)

    override suspend fun <T> deleteApi(
        path: String,
        query: Map<String, String>,
        parseData: (JsonElement?) -> T?,
    ): ApiResponse<T> = ApiEnvelopeParser.parse(reply("DELETE", path, "", query), parseData)
}

private data class FriendCall(
    val method: String,
    val path: String,
    val body: String,
    val query: Map<String, String>,
)

/** commonTest 无 kotlinx-coroutines-test：同步跑完不挂起的测试体。 */
private fun runNow(block: suspend () -> Unit) {
    var failure: Throwable? = null
    var completed = false
    block.startCoroutine(
        object : Continuation<Unit> {
            override val context = EmptyCoroutineContext

            override fun resumeWith(result: Result<Unit>) {
                completed = true
                failure = result.exceptionOrNull()
            }
        },
    )
    if (!completed) throw IllegalStateException("Test coroutine suspended — stubs must not delay")
    failure?.let { throw it }
}

/** 断言 body 是合法 JSON 对象（防止把 `{}` 写成省略 body）。 */
private fun String.asJsonObject(): JsonObject =
    NetworkJson.parseToJsonElement(this) as? JsonObject ?: error("Not a JSON object: $this")

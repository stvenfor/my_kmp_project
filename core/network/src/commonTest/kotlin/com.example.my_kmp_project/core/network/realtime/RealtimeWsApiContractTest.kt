package com.example.my_kmp_project.core.network.realtime

import com.example.my_kmp_project.core.network.ApiClient
import com.example.my_kmp_project.core.network.ApiResponse
import com.example.my_kmp_project.core.network.HttpTextResponse
import com.example.my_kmp_project.core.network.NetworkError
import com.example.my_kmp_project.core.network.NetworkJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * Realtime 两个 HTTP 接口的**裸 body** 契约（契约表 §2.17 行 #97/#98）：
 * `/api/v1/realtime/ws-ticket` 与 `/api/v1/realtime/sync`。
 *
 * 真值（my_ai_project d7876a1）：
 * - `components/realtime/lib/api/ws_ticket_api.dart:14-75`
 *   —— 请求恒为 `{"platform":"mobile"}`（:25-27）、空 token **先抛** `StateError`（:20-22）、
 *   响应 camelCase 四键、单键值 null 时 `?? ''` / `?? 0`（:61-74）
 * - `components/realtime/lib/api/ws_sync_api.dart:7-69`
 *   —— 请求 `{sinceSeq:int, topics:[String]}`（:14-17）、响应 `{events, latestSeq}`（:47-63）
 *
 * 用手写假 [ApiClient]（与 `ApiEnvelopeParserTest` / `SseContractTest` 同款），
 * 因此本文件不含任何 Ktor 类型，可在包括 ohosX64 在内的每个 target 编译。
 */
class RealtimeWsApiContractTest {

    /** FIFO 返回预制裸 body 的假 client；只实装 realtime 用到的 postRaw。 */
    private class RecordingApiClient(
        responses: List<String>,
        private val failure: Throwable? = null,
    ) : ApiClient {
        private val queue = ArrayDeque(responses)
        val postBodies = mutableListOf<Pair<String, String>>()

        override suspend fun postRaw(path: String, body: String): String {
            postBodies += path to body
            if (failure != null) throw failure
            return queue.removeFirst()
        }

        override suspend fun getRaw(path: String, query: Map<String, String>): String =
            unused("getRaw")

        override suspend fun getAbsoluteRaw(url: String): String = unused("getAbsoluteRaw")

        override suspend fun postForm(path: String, fields: Map<String, String>): HttpTextResponse =
            unused("postForm")

        override suspend fun putRaw(path: String, body: String): String = unused("putRaw")
        override suspend fun patchRaw(path: String, body: String): String = unused("patchRaw")
        override suspend fun deleteRaw(path: String): String = unused("deleteRaw")

        override suspend fun <T> getApi(
            path: String,
            query: Map<String, String>,
            parseData: (JsonElement?) -> T?,
        ): ApiResponse<T> = unused("getApi")

        override suspend fun <T> postApi(
            path: String,
            body: String,
            parseData: (JsonElement?) -> T?,
        ): ApiResponse<T> = unused("postApi")

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

        private fun unused(name: String): Nothing =
            throw AssertionError("realtime 契约不应调用 $name")
    }

    // ── #97 POST /realtime/ws-ticket ───────────────────────────────────────────

    @Test
    fun fetchTicket_postsFixedPlatformBodyToTicketPath(): Unit = runBlocking {
        val client = RecordingApiClient(
            listOf("""{"ticket":"t1","wsUrl":"wss://rt.example.com/ws","expiresInSeconds":60,"connId":"c1"}"""),
        )
        RealtimeWsTicketApi(client).fetchTicket("access-token")

        assertEquals(1, client.postBodies.size)
        val (path, body) = client.postBodies.single()
        assertEquals("/api/v1/realtime/ws-ticket", path)
        assertEquals(RealtimeConfig.TICKET_PATH, path)
        // Dart ws_ticket_api.dart:25-27 固定只发一个键。
        assertEquals("""{"platform":"mobile"}""", body)
    }

    @Test
    fun fetchTicket_decodesTheBareCamelCaseGoBody(): Unit = runBlocking {
        val client = RecordingApiClient(
            listOf("""{"ticket":"t1","wsUrl":"wss://rt.example.com/ws","expiresInSeconds":60,"connId":"c1"}"""),
        )
        val ticket = RealtimeWsTicketApi(client).fetchTicket("access-token")
        assertEquals("t1", ticket.ticket)
        assertEquals("wss://rt.example.com/ws", ticket.wsUrl)
        assertEquals(60L, ticket.expiresInSeconds)
        assertEquals("c1", ticket.connId)
    }

    @Test
    fun fetchTicket_fallsBackToEmptyStringAndZeroLikeDartConverters(): Unit = runBlocking {
        // Dart `json['ticket']?.toString() ?? ''` / `_asInt(...) ?? 0`（ws_ticket_api.dart:61-74）。
        val client = RecordingApiClient(
            listOf("""{"ticket":null,"wsUrl":"wss://rt.example.com/ws","expiresInSeconds":"120","connId":4711}"""),
        )
        val ticket = RealtimeWsTicketApi(client).fetchTicket("access-token")
        assertEquals("", ticket.ticket)
        assertEquals(120L, ticket.expiresInSeconds)
        assertEquals("4711", ticket.connId)
    }

    @Test
    fun fetchTicket_ignoresUnknownKeysAddedByGo(): Unit = runBlocking {
        val client = RecordingApiClient(
            listOf("""{"ticket":"t","wsUrl":"wss://h/ws","expiresInSeconds":1,"connId":"c","futureField":true}"""),
        )
        assertEquals("t", RealtimeWsTicketApi(client).fetchTicket("t").ticket)
    }

    @Test
    fun fetchTicket_rejectsTheEnvelopeWrappedBody(): Unit = runBlocking {
        // 契约表 #97「套 `{code,data}` 会崩」：顶层四键全缺 ⇒ MissingField ⇒ BodyUnparsable。
        val client = RecordingApiClient(
            listOf("""{"code":0,"message":"success","data":{"ticket":"t","wsUrl":"wss://h/ws","expiresInSeconds":1,"connId":"c"}}"""),
        )
        val error = assertFailsWith<RealtimeWsError.BodyUnparsable> {
            RealtimeWsTicketApi(client).fetchTicket("access-token")
        }
        assertTrue(error.message!!.contains("Realtime 裸 body 解析失败"), error.message ?: "")
        assertEquals(
            """{"code":0,"message":"success","data":{"ticket":"t","wsUrl":"wss://h/ws","expiresInSeconds":1,"connId":"c"}}""",
            error.raw,
        )
    }

    @Test
    fun fetchTicket_throwsBeforeAnyHttpCallWhenTokenIsBlank(): Unit = runBlocking {
        // Dart `throw StateError('缺少 access token，请先登录')`（ws_ticket_api.dart:21）。
        val client = RecordingApiClient(listOf("should-not-be-read"))
        val error = assertFailsWith<IllegalStateException> {
            RealtimeWsTicketApi(client).fetchTicket("")
        }
        assertEquals("缺少 access token，请先登录", error.message)
        assertTrue(client.postBodies.isEmpty())
    }

    @Test
    fun fetchTicket_failsFastWhenServerOmitsTheDialUrl(): Unit = runBlocking {
        // Dart 会在 `WebSocketChannel.connect(Uri.parse(''))` 处炸；KMP 提前判票无效。
        val client = RecordingApiClient(
            listOf("""{"ticket":"t","wsUrl":"","expiresInSeconds":60,"connId":"c"}"""),
        )
        val error = assertFailsWith<RealtimeWsError.TicketInvalid> {
            RealtimeWsTicketApi(client).fetchTicket("access-token")
        }
        assertTrue(error.message!!.contains("wsUrl"), error.message ?: "")
    }

    @Test
    fun fetchTicket_propagatesHttpErrorsUnchangedForTheAuthPolicy(): Unit = runBlocking {
        // 401 必须是原样的 NetworkError.Transport，交给 DefaultRealtimeAuthHardFailurePolicy 判硬失败。
        val client = RecordingApiClient(
            emptyList(),
            failure = NetworkError.Transport("401 Unauthorized", httpStatus = 401),
        )
        val error = assertFailsWith<NetworkError.Transport> {
            RealtimeWsTicketApi(client).fetchTicket("access-token")
        }
        assertEquals(401, error.httpStatus)
    }

    // ── #98 POST /realtime/sync ────────────────────────────────────────────────

    @Test
    fun sync_sendsCamelCaseSinceSeqAndTopics(): Unit = runBlocking {
        val client = RecordingApiClient(listOf("""{"events":[],"latestSeq":41}"""))
        RealtimeWsSyncApi(client).sync(sinceSeq = 41, topics = listOf("sys.notify", "presence.bulk"))

        val (path, body) = client.postBodies.single()
        assertEquals("/api/v1/realtime/sync", path)
        assertEquals(RealtimeConfig.SYNC_PATH, path)
        // 全仓习惯是 snake_case，这两条是**例外**（ws_sync_api.dart:14-17）。
        assertEquals("""{"sinceSeq":41,"topics":["sys.notify","presence.bulk"]}""", body)
    }

    @Test
    fun sync_decodesEventsAsEnvelopesAndRaisesTheWatermarkHint(): Unit = runBlocking {
        val client = RecordingApiClient(
            listOf(
                """{"events":[{"id":"e1","type":"event","topic":"sys.notify","seq":42,"payload":{"name":"sys.notify.show","notifyId":"n1"}},""" +
                    """{"type":"event","topic":"presence.bulk","seq":"43","payload":{"name":"presence.update"}}],""" +
                    """"latestSeq":43}""",
            ),
        )
        val data = RealtimeWsSyncApi(client).sync(sinceSeq = 41, topics = listOf("sys.notify"))
        assertEquals(43L, data.latestSeq)
        assertEquals(2, data.events.size)
        assertEquals("sys.notify", data.events[0].topic)
        assertEquals(42L, data.events[0].seq)
        assertEquals("sys.notify.show", data.events[0].eventName)
        // seq 数字字符串同样接受（Dart `_asInt`，realtime_envelope.dart:52-56）。
        assertEquals(43L, data.events[1].seq)
        assertNull(data.events[1].id)
    }

    @Test
    fun sync_toleratesStringLatestSeqAndEmptyEventList(): Unit = runBlocking {
        val client = RecordingApiClient(listOf("""{"events":[],"latestSeq":"900"}"""))
        val data = RealtimeWsSyncApi(client).sync(sinceSeq = 0, topics = emptyList())
        assertEquals(900L, data.latestSeq)
        assertTrue(data.events.isEmpty())
    }

    @Test
    fun sync_rejectsEnvelopeWrappedOrKeylessBody(): Unit = runBlocking {
        val client = RecordingApiClient(listOf("""{"code":0,"message":"success","data":{"events":[],"latestSeq":1}}"""))
        val error = assertFailsWith<RealtimeWsError.BodyUnparsable> {
            RealtimeWsSyncApi(client).sync(sinceSeq = 1, topics = listOf("sys.notify"))
        }
        assertTrue(error.message!!.contains("裸 body"), error.message ?: "")
    }

    @Test
    fun syncHandDecodedPath_skipsNonObjectItemsAndDefaultsLatestSeqToZero() {
        // Dart WsSyncResult.fromJson（ws_sync_api.dart:47-63）：`if (item is Map)` 跳过非对象项、
        // `_asInt(json['latestSeq']) ?? 0` 缺键归 0。手搓入口逐条复刻。
        val json = NetworkJson.parseToJsonElement(
            """{"events":[1,null,{"type":"event","topic":"sys.notify","seq":5},"x"]}""",
        )
        val data = RealtimeSyncData.fromJson(json as JsonObject)
        assertEquals(1, data.events.size)
        assertEquals("sys.notify", data.events.single().topic)
        assertEquals(5L, data.events.single().seq)
        assertEquals(0L, data.latestSeq)
    }

    @Test
    fun syncHandDecodedPath_survivesMissingEventsKey() {
        val json = NetworkJson.parseToJsonElement("""{"latestSeq":7}""")
        val data = RealtimeSyncData.fromJson(json as JsonObject)
        assertTrue(data.events.isEmpty())
        assertEquals(7L, data.latestSeq)
    }

    @Test
    fun bodyUnparsable_previewsOnlyTheFirstTwoHundredChars() {
        val long = "x".repeat(400)
        val error = RealtimeWsError.BodyUnparsable(long, IllegalStateException("boom"))
        assertTrue(error.message!!.length < 300, error.message ?: "")
        assertTrue(error.message!!.contains("x".repeat(200)), error.message ?: "")
        assertTrue(!error.message!!.contains("x".repeat(201)))
    }

    @Test
    fun resolveRealtimeWsUrl_keepsSchemePortAndPathOfTheIssuedUrl() {
        // 契约表 §5「必须原样拨号」：只做 loopback host 改写，不动 scheme / 端口 / 路径。
        val issued = "wss://rt.example.com:8443/ws/realtime?connId=c1"
        val resolved = resolveRealtimeWsUrl(issued)
        assertTrue(resolved.startsWith("wss://"), resolved)
        assertTrue(resolved.endsWith(":8443/ws/realtime?connId=c1"), resolved)
    }
}

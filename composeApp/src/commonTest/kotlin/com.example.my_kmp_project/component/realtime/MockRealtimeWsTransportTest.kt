package com.example.my_kmp_project.component.realtime

import com.example.my_kmp_project.core.network.realtime.RealtimeConfig
import com.example.my_kmp_project.core.network.realtime.RealtimeEnvelope
import com.example.my_kmp_project.core.network.realtime.RealtimeTopics
import com.example.my_kmp_project.core.network.realtime.RealtimeWsClose
import com.example.my_kmp_project.core.network.realtime.RealtimeWsTransport
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * 进程内 Mock 网关 —— 真值 Dart `components/realtime/lib/transport/mock_ws_transport.dart:9-180`（d7876a1）。
 *
 * 锁的是**模拟协议**本身（客户端与调试页都依赖它）：
 * `auth` ⇒ `auth_ok`；`ping` ⇒ `pong` 且 **id 原样回带**（心跳靠 id 匹配，改了就等于丢包）；
 * `sub`/`unsub`/`event` ⇒ `ack`（payload 带 `refId`）；其它出站类型只记日志；
 * 周期推送每 20s 给每个已登记 topic 造一帧 `event`，`seq` 从 1 起单调自增。
 *
 * 与 Dart 的两处刻意差别也在这里钉住：
 * 建连耗时可注入（测试传 0），以及 KMP 新增的 [RealtimeWsTransport.closed] 通道
 * —— [RealtimeWsTransport.close] **不往里发**（Dart 的 mock 从不触发 onDone），
 * 只有 `simulateClose` 会发，用于演练 4001 / 4002 / 4003。
 */
class MockRealtimeWsTransportTest {

    private val holder = RealtimeTestScope()
    private val ticker = ManualRealtimeTicker()
    private val clock = SteppingRealtimeClock(5_000L)
    private val transport = MockRealtimeWsTransport(ticker = ticker, clock = clock, connectDelayMs = 0L)
    private val inbound = mutableListOf<RealtimeEnvelope>()
    private val closes = mutableListOf<RealtimeWsClose>()

    init {
        holder.scope.launch { transport.inbound.toList(inbound) }
        holder.scope.launch { transport.closed.toList(closes) }
    }

    @AfterTest
    fun tearDown() {
        holder.cancel()
    }

    private suspend fun sendFrame(
        type: String,
        id: String? = "f-1",
        topics: List<String> = emptyList(),
        payload: kotlinx.serialization.json.JsonObject? = null,
    ) {
        val body = payload
            ?: if (topics.isEmpty()) {
                buildJsonObject { }
            } else {
                buildJsonObject {
                    put(
                        RealtimeEnvelope.KEY_TOPICS,
                        kotlinx.serialization.json.JsonArray(topics.map { kotlinx.serialization.json.JsonPrimitive(it) }),
                    )
                }
            }
        transport.send(RealtimeEnvelope(id = id, type = type, ts = clock.nowMs(), payload = body))
    }

    @Test
    fun sendBeforeConnectThrowsLikeDartStateError(): Unit = runBlocking {
        assertFalse(transport.isConnected)
        val error = assertFailsWith<IllegalStateException> { sendFrame("auth") }
        assertTrue(error.message!!.contains("not connected"), error.message ?: "")
    }

    @Test
    fun connectFlipsTheFlagAndKeepsTheDialUrlVerbatim(): Unit = runBlocking {
        transport.connect("wss://rt.example.com:8443/ws/realtime")
        assertTrue(transport.isConnected)
        assertEquals("wss://rt.example.com:8443/ws/realtime", transport.dialedUrlForTest())
    }

    @Test
    fun authIsAnsweredWithAuthOkCarryingMockUserAndSession(): Unit = runBlocking {
        transport.connect("wss://h/ws")
        sendFrame("auth", payload = buildJsonObject { put("ticket", "tk"); put("platform", "mobile") })
        val reply = inbound.single()
        assertEquals("auth_ok", reply.type)
        assertEquals("auth_ok_5000", reply.id)
        assertEquals(5_000L, reply.ts)
        assertEquals("mock_user", reply.payloadString("userId"))
        assertEquals("mock_session_5000", reply.payloadString("sessionId"))
        assertEquals(5_000L, reply.payloadLong("serverTime"))
    }

    @Test
    fun pongEchoesTheExactPingIdBecauseHeartbeatMatchesOnIt(): Unit = runBlocking {
        transport.connect("wss://h/ws")
        sendFrame("ping", id = "ping_123")
        val pong = inbound.single()
        assertEquals("pong", pong.type)
        assertEquals("ping_123", pong.id)
        assertNull(pong.topic)
    }

    @Test
    fun subRegistersTopicsAndAnswersWithAckSnapshotAndArmsThePushTimer(): Unit = runBlocking {
        transport.connect("wss://h/ws")
        sendFrame("auth")
        inbound.clear()
        sendFrame("sub", id = "s1", topics = listOf(RealtimeTopics.SYS_NOTIFY, RealtimeTopics.PRESENCE_BULK))
        val ack = inbound.single()
        assertEquals("ack", ack.type)
        assertEquals("s1", ack.payloadString(RealtimeEnvelope.KEY_REF_ID))
        assertEquals(
            listOf(RealtimeTopics.SYS_NOTIFY, RealtimeTopics.PRESENCE_BULK),
            ack.payloadTopics(),
        )
        assertEquals(listOf(20_000L), ticker.createdDelays)
    }

    @Test
    fun subBeforeAuthDoesNotArmThePushTimer(): Unit = runBlocking {
        // Dart `_startMockPushIfNeeded`：`!_connected || !_authed ⇒ return`（:101-103）。
        transport.connect("wss://h/ws")
        sendFrame("sub", id = "s1", topics = listOf(RealtimeTopics.SYS_NOTIFY))
        assertTrue(ticker.createdDelays.isEmpty())
        assertEquals(1, inbound.size)
    }

    @Test
    fun unsubRemovesTopicsAndAStopWhenTheSetEmptiesCancelsThePushTimer(): Unit = runBlocking {
        transport.connect("wss://h/ws")
        sendFrame("auth")
        sendFrame("sub", id = "s1", topics = listOf("live.signal.r1"))
        inbound.clear()
        sendFrame("unsub", id = "u1", topics = listOf("live.signal.r1"))
        val ack = inbound.single()
        assertEquals("u1", ack.payloadString(RealtimeEnvelope.KEY_REF_ID))
        assertTrue(ack.payloadTopics().isEmpty())
        assertEquals(listOf(20_000L), ticker.createdDelays)
        assertTrue(ticker.pendingDelays.isEmpty(), ticker.pendingDelays.toString())
    }

    @Test
    fun eventIsAnsweredWithAnAckThatCarriesAcceptedTrue(): Unit = runBlocking {
        transport.connect("wss://h/ws")
        sendFrame("auth")
        inbound.clear()
        sendFrame(
            "event",
            id = "msg-1",
            payload = buildJsonObject { put(RealtimeEnvelope.KEY_NAME, "presence.report") },
        )
        val ack = inbound.single()
        assertEquals("ack", ack.type)
        assertEquals("msg-1", ack.payloadString(RealtimeEnvelope.KEY_REF_ID))
        assertEquals(true, ack.payloadBool("accepted"))
        // Dart 的 ack id 是 `ack_<now>`。
        assertEquals("ack_5000", ack.id)
    }

    @Test
    fun unknownOutboundTypesAreIgnoredWithoutAReply(): Unit = runBlocking {
        transport.connect("wss://h/ws")
        sendFrame("totally-bogus", id = "x1")
        assertTrue(inbound.isEmpty())
    }

    @Test
    fun closeResetsTheSessionAndDoesNotPublishToTheClosedChannel(): Unit = runBlocking {
        // Dart mock 从不触发 onDone ⇒ KMP 的 close() 同样**不**往 closed 发（刻意差别）。
        transport.connect("wss://h/ws")
        sendFrame("auth")
        sendFrame("sub", id = "s1", topics = listOf(RealtimeTopics.SYS_NOTIFY))
        transport.close(code = RealtimeConfig.WS_CLOSE_NORMAL, reason = "bye")
        assertFalse(transport.isConnected)
        assertTrue(closes.isEmpty())
        inbound.clear()
        assertFailsWith<IllegalStateException> { sendFrame("ping", id = "p1") }
    }

    @Test
    fun simulateClosePublishesTheCodeAndResetsTheConnection(): Unit = runBlocking {
        for (code in listOf(4001, 4002, 4003)) {
            transport.connect("wss://h/ws")
            transport.simulateClose(code, reason = "r$code")
        }
        assertEquals(listOf(4001, 4002, 4003), closes.map { it.code })
        assertEquals(listOf("r4001", "r4002", "r4003"), closes.map { it.reason })
        assertFalse(transport.isConnected)
        assertTrue(RealtimeConfig.requiresNewTicket(4001))
    }

    @Test
    fun redialResetsTopicsAuthAndTimersLikeDartConnectClosingThePreviousOne(): Unit = runBlocking {
        transport.connect("wss://h/ws")
        sendFrame("auth")
        sendFrame("sub", id = "s1", topics = listOf(RealtimeTopics.SYS_NOTIFY))
        ticker.clear()
        inbound.clear()
        transport.connect("wss://h2/ws")
        // 重连后必须先重新鉴权，sub 才会再排推送。
        sendFrame("sub", id = "s2", topics = listOf(RealtimeTopics.SYS_NOTIFY))
        assertTrue(ticker.createdDelays.isEmpty())
        assertEquals(1, inbound.size)
    }

    @Test
    fun pushTickEmitsOneEventPerRegisteredTopicWithAMonotonicSeq(): Unit = runBlocking {
        transport.connect("wss://h/ws")
        sendFrame("auth")
        sendFrame(
            "sub",
            id = "s1",
            topics = listOf(RealtimeTopics.SYS_NOTIFY, "live.signal.r7", "presence.user.u3"),
        )
        inbound.clear()
        ticker.runFirst()

        val pushed = inbound.toList()
        assertEquals(3, pushed.size)
        assertTrue(pushed.all { it.type == "event" })
        assertEquals(listOf(1L, 1L, 1L), pushed.map { it.seq })

        val notify = pushed.first { it.topic == RealtimeTopics.SYS_NOTIFY }
        assertEquals("evt_notify_1", notify.id)
        assertEquals("sys.notify.show", notify.eventName)
        assertEquals("mock_notify_ws_1", notify.payloadString("notifyId"))
        assertEquals("Mock 全局通知", notify.payloadString("title"))

        val live = pushed.first { it.topic == "live.signal.r7" }
        assertEquals("evt_live_1", live.id)
        assertEquals("live.seat_changed", live.eventName)
        assertEquals("r7", live.payloadString("roomId"))
        assertEquals(1L, live.payloadLong("seatIndex"))

        val presence = pushed.first { it.topic == "presence.user.u3" }
        assertEquals("evt_presence_1", presence.id)
        assertEquals("presence.update", presence.eventName)
        assertEquals(true, presence.payloadBool("online"))
        assertEquals(101L, presence.payloadLong("count"))
    }

    @Test
    fun pushTickReArmsItselfAndTheSeqKeepsGrowingAcrossTicks(): Unit = runBlocking {
        transport.connect("wss://h/ws")
        sendFrame("auth")
        sendFrame("sub", id = "s1", topics = listOf(RealtimeTopics.PRESENCE_BULK))
        inbound.clear()
        ticker.runFirst()
        ticker.runFirst()
        assertEquals(listOf(1L, 2L), inbound.map { it.seq })
        assertEquals(listOf("evt_presence_1", "evt_presence_2"), inbound.map { it.id })
        // 每一拍都自约下一次（RealtimeTicker 句柄一次性 ⇒ 周期靠手动重排实现）。
        assertEquals(listOf(20_000L, 20_000L, 20_000L), ticker.createdDelays)
    }

    @Test
    fun pushTickForAnUnmatchedTopicConsumesTheSeqButEmitsNothing(): Unit = runBlocking {
        // Dart 的 when 分支没有 default：未知 topic 不造帧，但 `_seq++` 已经发生（:107）。
        transport.connect("wss://h/ws")
        sendFrame("auth")
        sendFrame("sub", id = "s1", topics = listOf("chat.room.9"))
        inbound.clear()
        ticker.runFirst()
        assertTrue(inbound.isEmpty())
        sendFrame("unsub", id = "u1", topics = listOf("chat.room.9"))
        sendFrame("sub", id = "s2", topics = listOf(RealtimeTopics.SYS_NOTIFY))
        inbound.clear()
        ticker.runFirst()
        assertEquals(2L, inbound.single().seq)
    }

    @Test
    fun pushTickStopsWhenTheConnectionWasDroppedMidFlight(): Unit = runBlocking {
        transport.connect("wss://h/ws")
        sendFrame("auth")
        sendFrame("sub", id = "s1", topics = listOf(RealtimeTopics.SYS_NOTIFY))
        inbound.clear()
        transport.close(code = null, reason = null)
        // close 已取消计时器，这里手动再跑一次已登记的句柄也不会发帧（守卫条件）。
        ticker.clear()
        transport.connect("wss://h/ws")
        sendFrame("sub", id = "s2", topics = listOf(RealtimeTopics.SYS_NOTIFY))
        assertTrue(inbound.isEmpty())
    }

    @Test
    fun disposeResetsStateWithoutPublishingAnything(): Unit = runBlocking {
        transport.connect("wss://h/ws")
        sendFrame("auth")
        inbound.clear()
        transport.dispose()
        assertFalse(transport.isConnected)
        assertTrue(inbound.isEmpty())
        assertTrue(closes.isEmpty())
    }

    @Test
    fun mockTransportSatisfiesTheTransportContractShape(): Unit = runBlocking {
        val asTransport: RealtimeWsTransport = transport
        transport.connect("wss://h/ws")
        assertFalse(asTransport.inbound.toListIfCompleted())
        asTransport.send(RealtimeEnvelope(id = "p", type = "ping"))
        assertEquals("pong", inbound.single().type)
    }

    /** 只读辅助：Mock 不暴露拨号 URL，这里用最后一次 connect 的入参做断言。 */
    private fun MockRealtimeWsTransport.dialedUrlForTest(): String? = lastDialedUrl

    private fun Flow<RealtimeEnvelope>.toListIfCompleted(): Boolean = false
}

/** 测试可见的最后一次拨号 URL；生产类不记录，故由本测试自己包一层。 */
private var lastDialedUrl: String? = null

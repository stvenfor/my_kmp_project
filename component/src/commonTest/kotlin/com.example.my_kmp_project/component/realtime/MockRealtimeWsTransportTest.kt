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
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * 进程内 Mock 网关 —— 真值 Dart `components/realtime/lib/transport/mock_ws_transport.dart:9-180`（d7876a1）。
 *
 * 锁的是**模拟协议**本身（客户端与调试页都依赖它）：
 * `auth` ⇒ `auth_ok`；`ping` ⇒ `pong` 且 **id 原样回带**（心跳靠 id 匹配，改了就等于丢包）；
 * `sub`/`unsub`/`event` ⇒ `ack`（payload 带 `refId`）；其它出站类型只记日志。
 *
 * 周期推送的 `seq` 语义要特别小心：Dart 把 `_seq++` 放在 **topic 循环体内**
 * （mock_ws_transport.dart:107），所以**同一拍里每个 topic 各占一个 seq**，
 * 不是一拍一个 seq。下面 `pushTick...` 三条断言就是在钉这一点。
 *
 * 与 Dart 的两处刻意差别也在这里钉住：
 * 建连耗时可注入（测试传 0），以及 KMP 新增的 [RealtimeWsTransport.closed] 通道
 * —— [RealtimeWsTransport.close] **不往里发**（Dart 的 mock 从不触发 onDone），
 * 只有 [MockRealtimeWsTransport.simulateClose] 会发，用于演练 4001 / 4002 / 4003。
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

    /** 只走传输接口的「服务器视角」断言入口：拿 [RealtimeWsTransport] 抽象类型来用。 */
    private val asTransport: RealtimeWsTransport get() = transport

    private suspend fun sendFrame(
        type: String,
        id: String? = "f-1",
        topics: List<String> = emptyList(),
        payload: JsonObject? = null,
    ) {
        val body = payload ?: if (topics.isEmpty()) {
            buildJsonObject { }
        } else {
            buildJsonObject {
                put(RealtimeEnvelope.KEY_TOPICS, JsonArray(topics.map { JsonPrimitive(it) }))
            }
        }
        asTransport.send(RealtimeEnvelope(id = id, type = type, ts = clock.nowMs(), payload = body))
    }

    /** 建连 + 鉴权，让后续 `sub` 具备起推送的条件。 */
    private suspend fun connectedAndAuthed() {
        asTransport.connect("wss://mock.test/ws")
        sendFrame("auth")
        inbound.clear()
    }

    @Test
    fun sendBeforeConnectThrowsLikeDartStateError(): Unit = runBlocking {
        assertFalse(asTransport.isConnected)
        val error = assertFailsWith<IllegalStateException> { sendFrame("auth") }
        // Dart mock 是 `throw StateError(...)`；Kotlin 侧 IllegalStateException 即 its 等价物。
        assertTrue(error.message!!.contains("not connected"), error.message ?: "")
        assertTrue(inbound.isEmpty())
    }

    @Test
    fun connectFlipsTheFlagAndClearsThePreviousSession(): Unit = runBlocking {
        asTransport.connect("wss://mock.test/ws")
        assertTrue(asTransport.isConnected)
        // Dart `connect` 首行就是 `await close()`（:25）⇒ 二次拨号也一定复位。
        sendFrame("auth")
        sendFrame("sub", id = "s1", topics = listOf(RealtimeTopics.SYS_NOTIFY))
        asTransport.connect("wss://mock.test/ws")
        assertTrue(asTransport.isConnected)
        assertTrue(ticker.pendingDelays.isEmpty(), ticker.pendingDelays.toString())
    }

    @Test
    fun authIsAnsweredWithAuthOkCarryingMockUserAndSession(): Unit = runBlocking {
        asTransport.connect("wss://mock.test/ws")
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
        asTransport.connect("wss://mock.test/ws")
        sendFrame("ping", id = "ping_123")
        val pong = inbound.single()
        assertEquals("pong", pong.type)
        // HeartbeatScheduler 用 `currentPendingPingId` 比对 pong.id；改了 id 就等于丢包。
        assertEquals("ping_123", pong.id)
        assertNull(pong.topic)
        assertEquals(5_000L, pong.ts)
    }

    @Test
    fun subRegistersTopicsAndAnswersWithAckSnapshotAndArmsThePushTimer(): Unit = runBlocking {
        connectedAndAuthed()
        sendFrame("sub", id = "s1", topics = listOf(RealtimeTopics.SYS_NOTIFY, RealtimeTopics.PRESENCE_BULK))
        val ack = inbound.single()
        assertEquals("ack", ack.type)
        assertEquals("ack_5000", ack.id)
        assertEquals("s1", ack.payloadString(RealtimeEnvelope.KEY_REF_ID))
        // 快照按登记顺序（Dart `_topics` 是 LinkedHashSet，:86-98）。
        assertEquals(listOf(RealtimeTopics.SYS_NOTIFY, RealtimeTopics.PRESENCE_BULK), ack.payloadTopics())
        assertEquals(listOf(20_000L), ticker.createdDelays)
        assertEquals(listOf(20_000L), ticker.pendingDelays)
    }

    @Test
    fun subBeforeAuthDoesNotArmThePushTimer(): Unit = runBlocking {
        // Dart `_startMockPushIfNeeded`：`!_connected || !_authed ⇒ return`（:101-103）。
        asTransport.connect("wss://mock.test/ws")
        sendFrame("sub", id = "s1", topics = listOf(RealtimeTopics.SYS_NOTIFY))
        assertTrue(ticker.createdDelays.isEmpty())
        assertEquals(1, inbound.size)
        assertEquals("ack", inbound.single().type)
    }

    @Test
    fun unsubRemovesTopicsAndStopsThePushTimerWhenTheSetEmpties(): Unit = runBlocking {
        connectedAndAuthed()
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
        connectedAndAuthed()
        sendFrame(
            "event",
            id = "msg-1",
            payload = buildJsonObject { put(RealtimeEnvelope.KEY_NAME, "presence.report") },
        )
        val ack = inbound.single()
        assertEquals("ack", ack.type)
        assertEquals("msg-1", ack.payloadString(RealtimeEnvelope.KEY_REF_ID))
        assertEquals(true, ack.payloadBool("accepted"))
        // Dart 的 ack id 是 `ack_<now>`（:87）⇒ 时钟不动时可逐字断言。
        assertEquals("ack_5000", ack.id)
        assertNull(ack.payloadString(RealtimeEnvelope.KEY_TOPICS))
    }

    @Test
    fun unknownOutboundTypesAreIgnoredWithoutAReply(): Unit = runBlocking {
        // Dart `default: _log('ignore outbound type')`（:79-81）——不回帧，客户端也就等不到 ack。
        connectedAndAuthed()
        sendFrame("totally-bogus", id = "x1")
        assertTrue(inbound.isEmpty())
    }

    @Test
    fun closeResetsTheSessionAndDoesNotPublishToTheClosedChannel(): Unit = runBlocking {
        // Dart mock 从不触发 onDone ⇒ KMP 的 close() 同样**不**往 closed 发（刻意差别）。
        connectedAndAuthed()
        sendFrame("sub", id = "s1", topics = listOf(RealtimeTopics.SYS_NOTIFY))
        asTransport.close(code = RealtimeConfig.WS_CLOSE_NORMAL, reason = "bye")
        assertFalse(asTransport.isConnected)
        assertTrue(closes.isEmpty())
        inbound.clear()
        assertFailsWith<IllegalStateException> { sendFrame("ping", id = "p1") }
    }

    @Test
    fun closeCancelsTheArmedPushTimerSoNoFrameArrivesAfterwards(): Unit = runBlocking {
        connectedAndAuthed()
        sendFrame("sub", id = "s1", topics = listOf(RealtimeTopics.SYS_NOTIFY))
        inbound.clear()
        assertEquals(listOf(20_000L), ticker.pendingDelays)
        asTransport.close(code = null, reason = null)
        assertTrue(ticker.pendingDelays.isEmpty(), ticker.pendingDelays.toString())
        assertEquals(0, ticker.runAtMost(3))
        assertTrue(inbound.isEmpty())
    }

    @Test
    fun simulateClosePublishesTheCodeAndResetsTheConnection(): Unit = runBlocking {
        for (code in listOf(4001, 4002, 4003)) {
            asTransport.connect("wss://mock.test/ws")
            transport.simulateClose(code, reason = "r$code")
        }
        // 契约行 #97-#99：三个码分别对应认证失败 / 互踢 / 票过期。
        assertEquals(listOf(4001, 4002, 4003), closes.map { it.code })
        assertEquals(listOf("r4001", "r4002", "r4003"), closes.map { it.reason })
        assertFalse(asTransport.isConnected)
        assertTrue(RealtimeConfig.requiresNewTicket(4001))
        assertTrue(RealtimeConfig.requiresNewTicket(4002))
        assertTrue(RealtimeConfig.requiresNewTicket(4003))
    }

    @Test
    fun redialResetsTopicsAndAuthSoSubAloneNoLongerArmsPush(): Unit = runBlocking {
        connectedAndAuthed()
        sendFrame("sub", id = "s1", topics = listOf(RealtimeTopics.SYS_NOTIFY))
        assertEquals(listOf(20_000L), ticker.pendingDelays)
        inbound.clear()

        // Dart `connect` 首行 `await close()` ⇒ 重拨会把上一条的推送计时一起取消。
        asTransport.connect("wss://mock.test/ws")
        assertTrue(ticker.pendingDelays.isEmpty(), ticker.pendingDelays.toString())

        // 重连后必须先重新鉴权，sub 才会再排推送（:101-103）。
        sendFrame("sub", id = "s2", topics = listOf(RealtimeTopics.SYS_NOTIFY))
        assertTrue(ticker.pendingDelays.isEmpty(), ticker.pendingDelays.toString())
        assertEquals(1, inbound.size)

        sendFrame("auth")
        inbound.clear()
        sendFrame("sub", id = "s3", topics = listOf(RealtimeTopics.SYS_NOTIFY))
        assertEquals(listOf(20_000L), ticker.pendingDelays)
    }

    @Test
    fun pushTickEmitsOneEventPerTopicWithSeqConsumedInsideTheLoop(): Unit = runBlocking {
        connectedAndAuthed()
        sendFrame(
            "sub",
            id = "s1",
            topics = listOf(RealtimeTopics.SYS_NOTIFY, "live.signal.r7", "presence.user.u3"),
        )
        inbound.clear()
        ticker.runFirst()

        val pushed = inbound.toList()
        assertEquals(3, pushed.size)
        assertTrue(pushed.all { it.type == "event" }, pushed.toString())
        // Dart `_seq++` 在 topic 循环体内（:107）⇒ 一拍三个不同 seq。
        assertEquals(listOf(1L, 2L, 3L), pushed.map { it.seq })
        assertEquals(listOf("evt_notify_1", "evt_live_2", "evt_presence_3"), pushed.map { it.id })

        val notify = pushed[0]
        assertEquals(RealtimeTopics.SYS_NOTIFY, notify.topic)
        assertEquals("sys.notify.show", notify.eventName)
        assertEquals("mock_notify_ws_1", notify.payloadString("notifyId"))
        assertEquals("Mock 全局通知", notify.payloadString("title"))
        assertEquals("seq=1 来自 WS", notify.payloadString("body"))

        val live = pushed[1]
        assertEquals("live.signal.r7", live.topic)
        assertEquals("live.seat_changed", live.eventName)
        // `topic.removePrefix('live.signal.')`（:131）
        assertEquals("r7", live.payloadString("roomId"))
        assertEquals(2L, live.payloadLong("seatIndex"))

        val presence = pushed[2]
        assertEquals("presence.user.u3", presence.topic)
        assertEquals("presence.update", presence.eventName)
        assertEquals(true, presence.payloadBool("online"))
        assertEquals(103L, presence.payloadLong("count"))
    }

    @Test
    fun pushTickReArmsItselfAndTheSeqKeepsGrowingAcrossTicks(): Unit = runBlocking {
        connectedAndAuthed()
        sendFrame("sub", id = "s1", topics = listOf(RealtimeTopics.PRESENCE_BULK))
        inbound.clear()
        ticker.runFirst()
        ticker.runFirst()
        // presence.bulk 也命中 `presence.` 前缀（:139）—— 与 presence.user.x 同一分支。
        assertEquals(listOf(1L, 2L), inbound.map { it.seq })
        assertEquals(listOf("evt_presence_1", "evt_presence_2"), inbound.map { it.id })
        assertEquals(listOf(101L, 102L), inbound.map { it.payloadLong("count") })
        // 首排 + 两拍各自自约下一次（RealtimeTicker 句柄一次性 ⇒ 周期靠手动重排实现）。
        assertEquals(listOf(20_000L, 20_000L, 20_000L), ticker.createdDelays)
        assertEquals(listOf(20_000L), ticker.pendingDelays)
    }

    @Test
    fun pushTickForAnUnmatchedTopicConsumesTheSeqButEmitsNothing(): Unit = runBlocking {
        // Dart 的 when 分支没有 default：未知 topic 不造帧，但 `_seq++` 已经发生（:107）。
        connectedAndAuthed()
        sendFrame("sub", id = "s1", topics = listOf("chat.room.9"))
        inbound.clear()
        assertEquals(1, ticker.runAtMost(1))
        assertTrue(inbound.isEmpty())
        sendFrame("unsub", id = "u1", topics = listOf("chat.room.9"))
        sendFrame("sub", id = "s2", topics = listOf(RealtimeTopics.SYS_NOTIFY))
        inbound.clear()
        ticker.runFirst()
        // 被空转掉的那一拍仍然推进了水位 ⇒ mock 的 seq 不等同于「已发帧数」。
        assertEquals(2L, inbound.single().seq)
        assertEquals("evt_notify_2", inbound.single().id)
    }

    @Test
    fun unsubOfASubsetKeepsPushingOnlyTheRemainingTopics(): Unit = runBlocking {
        connectedAndAuthed()
        sendFrame("sub", id = "s1", topics = listOf(RealtimeTopics.SYS_NOTIFY, "presence.user.u1"))
        inbound.clear()
        sendFrame("unsub", id = "u1", topics = listOf(RealtimeTopics.SYS_NOTIFY))
        inbound.clear()
        // 集合非空 ⇒ 计时器仍在（Dart 只在 `_topics.isEmpty()` 时才 `_cancelPush`，:73）。
        assertEquals(listOf(20_000L), ticker.pendingDelays)
        ticker.runFirst()
        assertEquals(1, inbound.size)
        assertEquals("presence.user.u1", inbound.single().topic)
        // 摘掉的那一拍不占 seq：只按**剩余** topic 计数（Dart 循环遍历 `_topics.toList()`，:106）。
        assertEquals(1L, inbound.single().seq)
        assertEquals("evt_presence_1", inbound.single().id)
    }

    @Test
    fun disposeResetsStateWithoutPublishingAnything(): Unit = runBlocking {
        connectedAndAuthed()
        sendFrame("sub", id = "s1", topics = listOf(RealtimeTopics.SYS_NOTIFY))
        inbound.clear()
        transport.dispose()
        assertFalse(asTransport.isConnected)
        assertTrue(ticker.pendingDelays.isEmpty(), ticker.pendingDelays.toString())
        assertTrue(inbound.isEmpty())
        assertTrue(closes.isEmpty())
    }

    @Test
    fun mockTransportNeverReplaysFramesToLateCollectors(): Unit = runBlocking {
        // replay = 0：Dart 的 StreamController 同样无重放 ⇒ 迟到的订阅者只收新帧。
        asTransport.connect("wss://mock.test/ws")
        val late = mutableListOf<RealtimeEnvelope>()
        val job = holder.scope.launch { asTransport.inbound.toList(late) }
        sendFrame("ping", id = "p-late")
        assertEquals(listOf("p-late"), late.map { it.id })
        job.cancel()
    }
}

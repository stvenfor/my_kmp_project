package com.example.my_kmp_project.component.realtime

import com.example.my_kmp_project.core.network.NetworkError
import com.example.my_kmp_project.core.network.realtime.RealtimeConfig
import com.example.my_kmp_project.core.network.realtime.RealtimeEnvelope
import com.example.my_kmp_project.core.network.realtime.RealtimeTopics
import com.example.my_kmp_project.core.network.realtime.RealtimeWsError
import com.example.my_kmp_project.core.network.realtime.RealtimeWsSyncApi
import com.example.my_kmp_project.core.network.realtime.RealtimeWsTicketApi
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Realtime 状态机 —— 真值 Dart
 * `components/realtime/lib/client/app_realtime_client_impl.dart:39-538`（d7876a1），
 * 外加本仓 KMP 侧唯一的新增分支：WS 关闭码（Dart 读不到，契约行 #97-#99）。
 *
 * 覆盖的是**决策**，不是行覆盖率：
 * - 连接阶梯 `Connecting → Authenticating → Connected`，换票 body 是裸 `{"platform":"mobile"}`；
 * - `auth` 帧内容（ticket / platform / connId）—— 这三个键是 Go 侧鉴权的输入；
 * - `auth_ok` 之后：心跳起摆、`/realtime/sync` 用**持久化水位**当 `sinceSeq`、
 *   离线期登记的 topic 会被补 `sub`、出站队列被重放；
 * - seq 水位与 `sys.notify` 投递是**两条独立**路径（Dart client:259 明确写了原因）；
 * - `ack.refId` → 出站点确认；15s 无 ack → 标记失败；
 * - 关闭码：4002 停重连并交给会话守护，4001 复位退避立刻重连，其它走指数退避；
 * - `transportLostHandled` 闩（Ktor 会同时给 closed 与入站收尾，不夹就双算）；
 * - [RealtimeWsError.Unsupported] 与 401 都是**永久停止**，不进退避死循环。
 *
 * 时间全部走 [ManualRealtimeTicker] + [SteppingRealtimeClock]，协程挂 `Dispatchers.Unconfined`：
 * 推一帧入站 ⇒ 状态机在同一调用栈里跑完，断言不需要任何真实延时，
 * 也不需要 `kotlinx-coroutines-test`（本仓 commonTest 无该依赖，且 ohosX64 要一起编译）。
 */
class AppRealtimeClientImplTest {

    private val holder = RealtimeTestScope()
    private val ticker = ManualRealtimeTicker()
    private val clock = SteppingRealtimeClock(5_000L)
    private val kv = InMemoryRealtimeKeyValueStore()
    private val store = RecordingOutboundStore()
    private var msgSeq = 0
    private var frameSeq = 0

    private val outbound = RealtimeOutboundQueue(
        store = store,
        clock = clock,
        newMessageId = { "msg-${++msgSeq}" },
    )
    private val seqStore = RealtimeSeqStore(kv)
    private val router = RealtimeInboundRouter()
    private val notifier = RecordingNotifyHandler()
    private val telemetrySink = RecordingTelemetrySink()
    private val telemetry = RealtimeTelemetry(telemetrySink)
    private val session = FakeSessionPort()
    private val guard = FakeSessionGuard()
    private val network = RecordingNetworkMonitor()
    private val transport = FakeRealtimeWsTransport()

    /** 默认只备一张票：重连类用例用 [stockTickets] 按需补，多取会直接报「没准备响应体」。 */
    private val ticketClient = RecordingRawApiClient(listOf(TICKET_OK_BODY))
    private val syncClient = RecordingRawApiClient()

    private val notifyFrames = mutableListOf<RealtimeEnvelope>()
    private val presenceFrames = mutableListOf<RealtimeEnvelope>()

    /** 抖动关掉 ⇒ 退避序列可逐档断言（1000 / 2000 / 4000 …）。 */
    private val client = AppRealtimeClientImpl(
        ticketApi = RealtimeWsTicketApi(ticketClient),
        syncApi = RealtimeWsSyncApi(syncClient),
        outboundQueue = outbound,
        seqStore = seqStore,
        router = router,
        notifyHandler = notifier,
        telemetry = telemetry,
        session = session,
        scope = holder.scope,
        ticker = ticker,
        clock = clock,
        transportFactory = { transport },
        sessionGuard = guard,
        networkMonitor = network,
        reconnectPolicy = ReconnectPolicy(jitterRatio = 0.0),
        newFrameId = { "frame-${++frameSeq}" },
    )

    init {
        // topic 频道是懒注册的（inbound_router.dart:23），必须先 watchTopic 才收得到。
        holder.scope.launch { router.watchTopic(RealtimeTopics.SYS_NOTIFY).toList(notifyFrames) }
        holder.scope.launch { router.watchTopic(RealtimeTopics.PRESENCE_BULK).toList(presenceFrames) }
    }

    @AfterTest
    fun tearDown() {
        holder.cancel()
    }

    private fun stockTickets(count: Int) = repeat(count) { ticketClient.respond(TICKET_OK_BODY) }

    /** 每次 auth_ok 都会打一次 `/realtime/sync`，所以补一帧空响应再推。 */
    private fun pushAuthOk() {
        syncClient.respond(EMPTY_SYNC_BODY)
        transport.pushInbound(controlEnvelope(TYPE_AUTH_OK))
    }

    /** 走完 prepare → connect → auth_ok，返回时状态机处于 Connected。 */
    private suspend fun connected() {
        client.prepare()
        client.connect()
        pushAuthOk()
    }

    private fun stateTrail(): List<String> = payloadsOf(RealtimeTelemetryTypes.METRIC, "ws_state")
        .map { it["state"] as String }

    private fun payloadsOf(type: String, name: String): List<Map<String, Any?>> = telemetrySink.events
        .filter { it.payload[RealtimeEnvelope.KEY_TYPE] == type }
        .filter { it.payload[RealtimeEnvelope.KEY_NAME] == name }
        .map { it.payload }

    private fun metricsNamed(name: String): List<Map<String, Any?>> =
        payloadsOf(RealtimeTelemetryTypes.METRIC, name)

    // ---------------------------------------------------------------- 连接阶梯

    @Test
    fun connectFetchesTicketWithBareBodyThenDialsTheTicketUrl(): Unit = runBlocking {
        client.connect()

        assertEquals(listOf(RealtimeConfig.TICKET_PATH to """{"platform":"mobile"}"""), ticketClient.calls)
        // wsUrl 原样拨号（§5 反直觉语义 4）：非 loopback ⇒ 不改写。
        assertEquals(listOf("wss://rt.example.com:8443/ws/realtime"), transport.dialedUrls)
        assertEquals(RealtimeConnectionState.Authenticating, client.currentState)
        assertEquals(listOf("Connecting", "Authenticating"), stateTrail())
    }

    @Test
    fun authFrameCarriesTicketPlatformAndConnId(): Unit = runBlocking {
        client.connect()

        val auth = transport.sent.single()
        assertEquals(TYPE_AUTH, auth.type)
        assertEquals("frame-1", auth.id)
        assertEquals(5_000L, auth.ts)
        // Dart client:163-171 的三个键，Go 侧逐键校验。
        assertEquals("tk-1", auth.payloadString("ticket"))
        assertEquals(RealtimeConfig.PLATFORM_MOBILE, auth.payloadString("platform"))
        assertEquals("cn-1", auth.payloadString("connId"))
        // 建连耗时走 trace 而不是 metric（Dart telemetry :20-29）。
        assertEquals(0L, payloadsOf(RealtimeTelemetryTypes.TRACE, "ws_connect").single()["durationMs"])
        assertEquals(false, payloadsOf(RealtimeTelemetryTypes.TRACE, "ws_connect").single()["mock"])
    }

    @Test
    fun authOkArmsHeartbeatRunsSyncAndReSubscribesPendingTopics(): Unit = runBlocking {
        client.prepare()
        client.connect()
        // 未活跃时只登记不发送（Dart `subscribeTopics` :378-384）。
        client.subscribeTopics(listOf(RealtimeTopics.SYS_NOTIFY))
        assertEquals(listOf(TYPE_AUTH), transport.sentTypes())

        pushAuthOk()

        assertEquals(RealtimeConnectionState.Connected, client.currentState)
        assertEquals(listOf(TYPE_AUTH, TYPE_SUB), transport.sentTypes())
        assertEquals(
            listOf(RealtimeTopics.SYS_NOTIFY),
            transport.firstSentOfType(TYPE_SUB)!!.payloadTopics(),
        )
        // 心跳第一拍 25s（Dart :317-331）；sync 用「离线期登记的 topic」原样回传。
        assertEquals(listOf(25_000L), ticker.createdDelays)
        assertEquals(
            listOf(RealtimeConfig.SYNC_PATH to """{"sinceSeq":0,"topics":["sys.notify"]}"""),
            syncClient.calls,
        )
        // 网络监听只挂一次（Dart :356-363）。
        assertEquals(1, network.startCount)
        assertTrue(network.hasListener)
    }

    @Test
    fun connectIsSkippedWhenNotLoggedIn(): Unit = runBlocking {
        session.loggedIn = false
        client.connect()

        assertTrue(ticketClient.calls.isEmpty())
        assertTrue(transport.dialedUrls.isEmpty())
        assertEquals(RealtimeConnectionState.Disconnected, client.currentState)
    }

    @Test
    fun connectIsDeduplicatedWhileInFlightAndAfterConnected(): Unit = runBlocking {
        client.connect()
        client.connect()
        assertEquals(1, ticketClient.calls.size)

        pushAuthOk()
        client.connect()
        assertEquals(1, ticketClient.calls.size)
        assertEquals(RealtimeConnectionState.Connected, client.currentState)
    }

    @Test
    fun prepareIsIdempotentAndReadsThePersistedWatermark(): Unit = runBlocking {
        kv.setString(RealtimePrefKeys.LAST_SEQ, "41")
        store.upsert(RealtimeOutboundRow("m1", "presence", "presence.report", "{}", "pending", 1L, 1L))
        store.upsert(RealtimeOutboundRow("m2", "presence", "presence.report", "{}", "failed", 2L, 2L))

        client.prepare()
        assertEquals(41L, client.lastSeq)
        // refreshDepth 只看 pending 视图：failed 行不计（outbound_queue_manager.dart:48-62）。
        assertEquals(1, client.outboundQueueDepth)

        kv.setString(RealtimePrefKeys.LAST_SEQ, "99")
        client.prepare()
        // 第二次 prepare 被 `_initialized` 吃掉 ⇒ 不会重新读盘。
        assertEquals(41L, client.lastSeq)
    }

    // ---------------------------------------------------------------- sync 与水位

    @Test
    fun syncBackfillDispatchesEventsThenRaisesWatermarkToLatestSeq(): Unit = runBlocking {
        kv.setString(RealtimePrefKeys.LAST_SEQ, "2")
        syncClient.respond(
            syncBody(
                """[{"id":"e1","type":"event","topic":"sys.notify","seq":3,"payload":{"name":"sys.notify.show","notifyId":"n3"}},""" +
                    """{"id":"e2","type":"event","topic":"presence.bulk","seq":5,"payload":{"name":"presence.update"}}]""",
                latestSeq = 9L,
            ),
        )

        client.prepare()
        client.connect()
        pushAuthOk()

        assertEquals("""{"sinceSeq":2,"topics":[]}""", syncClient.calls.last().second)
        assertEquals("n3", notifyFrames.single().payloadString("notifyId"))
        assertEquals("e2", presenceFrames.single().id)
        // 两条都进路由，但只有 sys.notify 那条交给 Banner 处理器。
        assertEquals(1, notifier.handled.size)
        assertEquals("e1", notifier.handled.single().id)
        // latestSeq 比事件里最大的 seq 还高 ⇒ 以 latestSeq 为准（ws_sync_api.dart:61）。
        assertEquals(9L, client.lastSeq)
        assertEquals("9", kv.getString(RealtimePrefKeys.LAST_SEQ))
    }

    @Test
    fun syncFailureIsReportedButNeverBreaksTheConnection(): Unit = runBlocking {
        syncClient.failNext(error = NetworkError.Transport("sync 503", 503))

        connected()

        assertEquals(RealtimeConnectionState.Connected, client.currentState)
        assertEquals(0, client.reconnectCount)
        assertEquals(listOf(25_000L), ticker.pendingDelays)
        assertEquals(0, metricsNamed("ws_reconnect_scheduled").size)
        assertTrue(payloadsOf(RealtimeTelemetryTypes.ERROR, "ws_sync_fail").isNotEmpty())
    }

    @Test
    fun staleSeqEventSkipsTheRouterButStillReachesTheNotifyHandler(): Unit = runBlocking {
        kv.setString(RealtimePrefKeys.LAST_SEQ, "10")
        connected()
        assertEquals(10L, client.lastSeq)

        // 旧 seq：不进路由，但 Banner 仍要展示（Dart client:259 注释的理由）。
        transport.pushInbound(
            eventEnvelope(seq = 5L, topic = RealtimeTopics.SYS_NOTIFY, name = "sys.notify.show"),
        )
        assertTrue(notifyFrames.isEmpty())
        assertEquals(1, notifier.handled.size)

        transport.pushInbound(eventEnvelope(seq = 11L, topic = RealtimeTopics.PRESENCE_BULK))
        assertEquals(1, presenceFrames.size)
        assertEquals(1, notifier.handled.size)
        assertEquals(11L, client.lastSeq)
    }

    @Test
    fun controlFramesAreNeverRoutedToBusinessChannels(): Unit = runBlocking {
        connected()
        for (type in listOf(TYPE_PONG, TYPE_ERROR, TYPE_SUB, "unsub", TYPE_PING, TYPE_AUTH)) {
            transport.pushInbound(controlEnvelope(type, id = "c-$type", payloadJson = """{"code":"x"}"""))
        }
        // ack 单独走：它会消费 pendingAcks，但不该进任何业务频道。
        transport.pushInbound(controlEnvelope(TYPE_ACK, payloadJson = """{"refId":"no-such-msg"}"""))

        assertTrue(notifyFrames.isEmpty())
        assertTrue(presenceFrames.isEmpty())
        assertEquals(RealtimeConnectionState.Connected, client.currentState)
    }

    @Test
    fun serverErrorFrameOnlyGoesToTelemetry(): Unit = runBlocking {
        connected()
        transport.pushInbound(controlEnvelope(TYPE_ERROR, payloadJson = """{"code":4003}"""))

        val payload = payloadsOf(RealtimeTelemetryTypes.ERROR, "ws_server_error").single()
        assertEquals("error", payload[RealtimeEnvelope.KEY_TYPE])
        assertTrue((payload["error"] ?: "").toString().contains("4003"), payload.toString())
        assertEquals(RealtimeConnectionState.Connected, client.currentState)
        assertEquals(0, client.reconnectCount)
    }

    // ---------------------------------------------------------------- 出站方向

    @Test
    fun sendEventMergesNameIntoPayloadAndWaitsForAck(): Unit = runBlocking {
        connected()
        client.sendEvent(
            topic = "presence.user.u1",
            eventName = "presence.report",
            payload = buildJsonObject { put("online", true) },
        )

        val frame = transport.sent.last()
        assertEquals(TYPE_EVENT, frame.type)
        // 帧 id = 队列表的 message_id ⇒ ack 才能对上（Dart :463-477）。
        assertEquals("msg-1", frame.id)
        assertEquals("presence.user.u1", frame.topic)
        assertEquals("presence.report", frame.eventName)
        assertEquals(true, frame.payloadBool("online"))
        assertEquals(RealtimeOutboundStatus.SENT, store.pending().single().status)
        assertEquals(1, client.outboundQueueDepth)
        // 心跳 25s 与 ack 超时 15s 并行在跑。
        assertEquals(listOf(25_000L, RealtimeConfig.OUTBOUND_ACK_TIMEOUT_MS), ticker.createdDelays)

        transport.pushInbound(controlEnvelope(TYPE_ACK, payloadJson = """{"refId":"msg-1"}"""))

        assertEquals(0, client.outboundQueueDepth)
        assertTrue(store.pending().isEmpty())
        // markAcked 走删行而不是改状态 ⇒ 状态流转日志里只有 sent（Dart :39-42）。
        assertEquals(listOf("msg-1" to RealtimeOutboundStatus.SENT), store.statusLog)
        assertEquals(listOf(25_000L), ticker.pendingDelays)
    }

    @Test
    fun ackTimeoutMarksTheRowFailedAndKeepsItOutOfResend(): Unit = runBlocking {
        connected()
        client.sendEvent("presence.user.u1", "presence.report", null)
        assertTrue(ticker.runWithDelay(RealtimeConfig.OUTBOUND_ACK_TIMEOUT_MS))

        assertEquals(listOf(25_000L), ticker.pendingDelays)
        assertEquals(
            listOf("msg-1" to RealtimeOutboundStatus.SENT, "msg-1" to RealtimeOutboundStatus.FAILED),
            store.statusLog,
        )
        // failed 不在 pending 视图里 ⇒ 不会被重放，但行仍在表里。
        assertTrue(store.pending().isEmpty())
        assertEquals(1, client.outboundQueueDepth)
        assertEquals("msg-1", metricsNamed("ws_outbound_ack_timeout").single()["messageId"])
    }

    @Test
    fun sendEventWhileInactiveQueuesAndDegrades(): Unit = runBlocking {
        client.sendEvent("presence.user.u1", "presence.report", buildJsonObject { put("k", "v") })

        assertEquals(RealtimeConnectionState.Degraded, client.currentState)
        assertTrue(transport.sent.isEmpty())
        assertEquals(1, client.outboundQueueDepth)
        assertEquals(RealtimeOutboundStatus.PENDING, store.pending().single().status)
    }

    @Test
    fun queuedMessageIsReplayedAfterReconnect(): Unit = runBlocking {
        client.sendEvent("presence.user.u1", "presence.report", buildJsonObject { put("k", "v") })
        client.prepare()
        client.connect()
        assertEquals(listOf(TYPE_AUTH), transport.sentTypes())

        pushAuthOk()

        // 重放在 sync 之后、补 sub 之前；这里没登记过 topic ⇒ 只有 auth + event。
        assertEquals(listOf(TYPE_AUTH, TYPE_EVENT), transport.sentTypes())
        val replayed = transport.sent.last()
        assertEquals("msg-1", replayed.id)
        assertEquals("presence.report", replayed.eventName)
        assertEquals("v", replayed.payloadString("k"))
        assertEquals(1, client.outboundQueueDepth)
    }

    @Test
    fun sendEventWithoutRequireAckIsConfirmedImmediately(): Unit = runBlocking {
        connected()
        client.sendEvent("presence.user.u1", "presence.report", null, requireAck = false)

        // 不起 15s 计时（Dart :477：requireAck=false 直接标已确认）。
        assertEquals(listOf(25_000L), ticker.createdDelays)
        assertEquals(0, client.outboundQueueDepth)
        assertTrue(store.pending().isEmpty())
        assertEquals(listOf("msg-1" to RealtimeOutboundStatus.SENT), store.statusLog)
    }

    // ---------------------------------------------------------------- 关闭码 / 重连

    @Test
    fun kickedClose4002StopsReconnectAndHandsTheSessionToTheGuard(): Unit = runBlocking {
        connected()
        transport.pushClosed(RealtimeConfig.WS_CLOSE_KICKED, "replaced")

        assertEquals(RealtimeConnectionState.Disconnected, client.currentState)
        assertEquals(0, client.reconnectCount)
        assertTrue(ticker.pendingDelays.isEmpty(), ticker.pendingDelays.toString())
        // 互踢也在「需换票」三码里（契约 #97-#99）⇒ 先记 ticket_refresh，再永久停止。
        assertEquals(4002, metricsNamed("ws_ticket_refresh").single()["code"])
        assertEquals("ws_close_4002", metricsNamed("ws_transport_lost").single()["reason"])
        assertEquals(4002, metricsNamed("ws_transport_lost").single()["closeCode"])
        assertEquals(0, metricsNamed("ws_reconnect_scheduled").size)
        assertEquals(1, guard.handledErrors.size)
        assertTrue(guard.handledErrors.single().message!!.contains("ws_kicked"))
        val closed: List<Pair<Int?, String?>> = listOf(null to null)
        assertEquals(closed, transport.closeCalls)
    }

    @Test
    fun authClose4001ResetsTheBackoffSoTheNextDropStaysOnFirstGear(): Unit = runBlocking {
        stockTickets(2)
        connected()

        // 先烧掉一档退避，才能证明 4001 的 reset() 把它归零了。
        transport.pushClosed(1006)
        assertEquals(listOf(1_000L), ticker.pendingDelays)
        assertTrue(ticker.runFirst())
        assertEquals(1, client.reconnectCount)

        transport.pushClosed(RealtimeConfig.WS_CLOSE_AUTH_FAILED)

        assertEquals(2, client.reconnectCount)
        assertEquals(RealtimeConnectionState.Reconnecting, client.currentState)
        assertEquals(4001, metricsNamed("ws_ticket_refresh").single()["code"])
        // 关键断言：若 4001 没有 reconnectPolicy.reset()，这一档会是 2000ms。
        assertEquals(listOf(1_000L), ticker.pendingDelays)
        assertTrue(ticker.runFirst())
        assertEquals(3, ticketClient.calls.size)
        assertEquals(3, transport.dialedUrls.size)
    }

    @Test
    fun plainTransportLossBacksOffExponentiallyAcrossDrops(): Unit = runBlocking {
        stockTickets(1)
        connected()
        transport.pushClosed(RealtimeConfig.WS_CLOSE_NO_STATUS)

        assertEquals(1, client.reconnectCount)
        assertEquals(RealtimeConnectionState.Reconnecting, client.currentState)
        val scheduled = metricsNamed("ws_reconnect_scheduled").single()
        assertEquals("ws_close_1005", scheduled["reason"])
        assertEquals(1_000L, scheduled["delayMs"])
        assertEquals(1, scheduled["attempt"])

        assertTrue(ticker.runWithDelay(1_000L))
        transport.pushClosed(1006)
        assertEquals(2, client.reconnectCount)
        assertEquals(2_000L, metricsNamed("ws_reconnect_scheduled").last()["delayMs"])
    }

    @Test
    fun theTransportLostLatchMakesSeveralClosesCountAsOneReconnect(): Unit = runBlocking {
        connected()
        // Ktor 会同时给 closed 与入站收尾：没有闩就双算（Dart 只有 onDone/onError 之一）。
        transport.pushClosed(1006)
        transport.pushClosed(1006)
        transport.pushClosed(RealtimeConfig.WS_CLOSE_AUTH_FAILED)

        assertEquals(1, client.reconnectCount)
        assertEquals(1, metricsNamed("ws_transport_lost").size)
        assertEquals(0, metricsNamed("ws_ticket_refresh").size)
        assertEquals(listOf(1_000L), ticker.pendingDelays)
    }

    @Test
    fun unsupportedTransportStopsPermanentlyInsteadOfBackingOffForever(): Unit = runBlocking {
        // GAP(R5)：ohosX64 没有 CPF Ktor/WebSockets 产物；桩异常重连一万次也不会成。
        val stub = FakeRealtimeWsTransport()
        stub.connectFailure = RealtimeWsError.Unsupported()
        val offline = AppRealtimeClientImpl(
            ticketApi = RealtimeWsTicketApi(ticketClient),
            syncApi = RealtimeWsSyncApi(syncClient),
            outboundQueue = outbound,
            seqStore = seqStore,
            router = router,
            notifyHandler = notifier,
            telemetry = telemetry,
            session = session,
            scope = holder.scope,
            ticker = ticker,
            clock = clock,
            transportFactory = { stub },
            networkMonitor = network,
            reconnectPolicy = ReconnectPolicy(jitterRatio = 0.0),
            newFrameId = { "frame-${++frameSeq}" },
        )

        offline.connect()

        assertEquals(RealtimeConnectionState.Disconnected, offline.currentState)
        assertEquals(0, offline.reconnectCount)
        assertTrue(ticker.createdDelays.isEmpty(), ticker.createdDelays.toString())
        assertTrue(payloadsOf(RealtimeTelemetryTypes.ERROR, "ws_connect_fail").isNotEmpty())
    }

    @Test
    fun http401OnTicketingStopsTheReconnectLoop(): Unit = runBlocking {
        ticketClient.failNext(error = NetworkError.Transport("unauthorized", 401))
        guard.recoverSucceeds = false

        client.connect()

        assertEquals(RealtimeConnectionState.Disconnected, client.currentState)
        assertEquals(0, client.reconnectCount)
        assertTrue(ticker.createdDelays.isEmpty(), ticker.createdDelays.toString())
    }

    @Test
    fun recoverableAuthFailureRetriesConnectOnceThenProceeds(): Unit = runBlocking {
        ticketClient.failNext(error = NetworkError.Transport("unauthorized", 401))
        guard.recoverSucceeds = true

        client.connect()

        // tryRecover() 成功 ⇒ 递归再走一次换票，而不是排退避定时器。
        assertEquals(2, ticketClient.calls.size)
        // 第一次失败发生在**换票阶段**，压根没拨号 ⇒ 只拨了一次（Dart client:185-214 的顺序）。
        assertEquals(1, transport.dialedUrls.size)
        assertEquals(RealtimeConnectionState.Authenticating, client.currentState)
        assertTrue(ticker.createdDelays.isEmpty(), ticker.createdDelays.toString())
        assertEquals(0, client.reconnectCount)
    }

    @Test
    fun http403OnTicketingKeepsBackingOff(): Unit = runBlocking {
        // 403 不是「会话已废」（AuthHttpDecision 同款结论）⇒ 走普通退避，不清会话。
        ticketClient.failNext(error = NetworkError.Transport("forbidden", 403))

        client.connect()

        assertEquals(1, client.reconnectCount)
        assertEquals(RealtimeConnectionState.Reconnecting, client.currentState)
        assertEquals(listOf(1_000L), ticker.pendingDelays)
        assertTrue(guard.handledErrors.isEmpty())
    }

    @Test
    fun heartbeatMissesPastThresholdPromoteAReconnect(): Unit = runBlocking {
        connected()
        assertEquals(listOf(25_000L), ticker.pendingDelays)

        assertTrue(ticker.runWithDelay(RealtimeConfig.HEARTBEAT_INTERVAL_MS))
        // ping 帧用的就是心跳那个 id，Go 侧按同 id 回 pong。
        val ping = transport.sent.last()
        assertEquals(TYPE_PING, ping.type)
        assertEquals("ping_5000", ping.id)
        assertEquals(listOf(10_000L, 25_000L), ticker.pendingDelays)

        // 第一次丢：不重连，只重新排程（HEARTBEAT_MAX_MISS=2）。
        assertTrue(ticker.runWithDelay(RealtimeConfig.HEARTBEAT_TIMEOUT_MS))
        assertEquals(0, client.reconnectCount)
        assertEquals(RealtimeConnectionState.Connected, client.currentState)

        assertTrue(ticker.runWithDelay(RealtimeConfig.HEARTBEAT_INTERVAL_MS))
        assertTrue(ticker.runWithDelay(RealtimeConfig.HEARTBEAT_TIMEOUT_MS))

        // 连续第 2 次 ⇒ 归一化成 transport lost 走重连。
        assertEquals(1, client.reconnectCount)
        assertEquals("heartbeat_timeout", metricsNamed("ws_reconnect_scheduled").single()["reason"])
        assertEquals(RealtimeConnectionState.Reconnecting, client.currentState)
        assertEquals(listOf(1_000L), ticker.pendingDelays)
    }

    @Test
    fun pongWithTheExactPendingIdClearsTheTimeout(): Unit = runBlocking {
        connected()
        assertTrue(ticker.runWithDelay(RealtimeConfig.HEARTBEAT_INTERVAL_MS))
        assertEquals(listOf(10_000L, 25_000L), ticker.pendingDelays)

        transport.pushInbound(controlEnvelope(TYPE_PONG, id = "ping_5000"))
        assertEquals(listOf(25_000L), ticker.pendingDelays)

        // id 不匹配的 pong 完全不起作用（heartbeat_scheduler.dart:47）。
        assertTrue(ticker.runWithDelay(RealtimeConfig.HEARTBEAT_INTERVAL_MS))
        transport.pushInbound(controlEnvelope(TYPE_PONG, id = "ping_other"))
        assertTrue(10_000L in ticker.pendingDelays, ticker.pendingDelays.toString())
    }

    @Test
    fun backOnlineReconnectsOnlyWhenCurrentStateIsInactive(): Unit = runBlocking {
        stockTickets(1)
        connected()

        // 已连接时「恢复联网」不该再起一条 WS。
        network.fireOnline()
        assertEquals(1, ticketClient.calls.size)

        transport.pushClosed(1006)
        network.fireOnline()

        assertEquals(2, ticketClient.calls.size)
        assertEquals(2, transport.dialedUrls.size)
        assertEquals(1, network.startCount)
    }

    @Test
    fun manualDisconnectClosesWithCodeOneThousandAndSilencesTheWatcher(): Unit = runBlocking {
        connected()
        client.disconnect(reason = "manual", clearQueue = true)

        val closed: List<Pair<Int?, String?>> = listOf(RealtimeConfig.WS_CLOSE_NORMAL to "manual")
        assertEquals(closed, transport.closeCalls)
        assertEquals(RealtimeConnectionState.Disconnected, client.currentState)
        assertTrue(ticker.pendingDelays.isEmpty(), ticker.pendingDelays.toString())
        assertTrue(store.pending().isEmpty())
        assertEquals(0, client.outboundQueueDepth)

        // 手动断开后：恢复联网、任何定时器都不再把 WS 拉起来。
        network.fireOnline()
        assertEquals(1, ticketClient.calls.size)
        assertEquals(0, ticker.runAtMost(5))
        assertEquals(RealtimeConnectionState.Disconnected, client.currentState)
    }

    @Test
    fun pausedWithoutKeepAliveDisconnectsButKeepAliveClientStaysUp(): Unit = runBlocking {
        stockTickets(1)
        connected()

        client.onAppLifecycle(RealtimeAppLifecycle.Paused)
        val closed: List<Pair<Int?, String?>> = listOf(RealtimeConfig.WS_CLOSE_NORMAL to "app_paused")
        assertEquals(closed, transport.closeCalls)
        assertEquals(RealtimeConnectionState.Disconnected, client.currentState)

        // paused 的断开走的是 disconnect ⇒ `_manualDisconnect` 置位，resumed 不会自动补连。
        client.onAppLifecycle(RealtimeAppLifecycle.Resumed)
        assertEquals(1, ticketClient.calls.size)

        client.setKeepAliveInBackground(true)
        assertTrue(client.keepAliveInBackground)
        client.connect()
        pushAuthOk()
        client.onAppLifecycle(RealtimeAppLifecycle.Paused)
        assertEquals(1, transport.closeCalls.size)
        assertEquals(RealtimeConnectionState.Connected, client.currentState)
    }

    @Test
    fun resumedReconnectsWhenTheDropWasNotManual(): Unit = runBlocking {
        stockTickets(1)
        connected()

        transport.pushClosed(1006)
        assertEquals(RealtimeConnectionState.Reconnecting, client.currentState)

        client.onAppLifecycle(RealtimeAppLifecycle.Resumed)

        assertEquals(2, ticketClient.calls.size)
        assertEquals(RealtimeConnectionState.Authenticating, client.currentState)
    }

    @Test
    fun disposeReleasesTransportWatcherAndRouter(): Unit = runBlocking {
        connected()

        client.dispose()

        assertEquals(RealtimeConnectionState.Disconnected, client.currentState)
        val closed: List<Pair<Int?, String?>> = listOf(RealtimeConfig.WS_CLOSE_NORMAL to "dispose")
        assertEquals(closed, transport.closeCalls)
        assertEquals(1, transport.disposedCount)
        assertEquals(1, network.stopCount)
        assertTrue(ticker.pendingDelays.isEmpty(), ticker.pendingDelays.toString())
        // dispose 不清队列（Dart :531-537）⇒ 这里没入队所以仍是空。
        assertEquals(0, client.outboundQueueDepth)
        // 路由登记表被清空 ⇒ 之后即便有帧也不再投递。
        transport.pushInbound(eventEnvelope(seq = 3L, topic = RealtimeTopics.SYS_NOTIFY))
        assertTrue(notifyFrames.isEmpty())
    }

    @Test
    fun sendFailureIsPropagatedButTheQueueKeepsTheMessage(): Unit = runBlocking {
        connected()
        transport.sendFailure = IllegalStateException("WebSocket not connected")

        assertFailsWith<IllegalStateException> {
            client.sendEvent("presence.user.u1", "presence.report", null)
        }

        // 状态机不吞发送异常：交给调用方（Dart 也是让 Future 直接抛）。
        assertEquals(RealtimeConnectionState.Connected, client.currentState)
        assertEquals(1, client.outboundQueueDepth)
    }

    private companion object {
        const val TYPE_AUTH: String = "auth"
        const val TYPE_AUTH_OK: String = "auth_ok"
        const val TYPE_PING: String = "ping"
        const val TYPE_PONG: String = "pong"
        const val TYPE_ACK: String = "ack"
        const val TYPE_ERROR: String = "error"
        const val TYPE_SUB: String = "sub"
        const val TYPE_EVENT: String = "event"
    }
}

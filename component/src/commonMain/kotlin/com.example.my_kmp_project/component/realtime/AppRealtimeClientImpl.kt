package com.example.my_kmp_project.component.realtime

import com.example.my_kmp_project.core.log.AppLog
import com.example.my_kmp_project.core.network.NetworkJson
import com.example.my_kmp_project.core.network.realtime.RealtimeConfig
import com.example.my_kmp_project.core.network.realtime.RealtimeEnvelope
import com.example.my_kmp_project.core.network.realtime.RealtimeTopics
import com.example.my_kmp_project.core.network.realtime.RealtimeWsError
import com.example.my_kmp_project.core.network.realtime.RealtimeWsSyncApi
import com.example.my_kmp_project.core.network.realtime.RealtimeWsTicketApi
import com.example.my_kmp_project.core.network.realtime.RealtimeWsClose
import com.example.my_kmp_project.core.network.realtime.RealtimeWsTransport
import com.example.my_kmp_project.core.network.realtime.createPlatformRealtimeWsTransport
import com.example.my_kmp_project.core.network.realtime.emptyRealtimeObject
import com.example.my_kmp_project.core.network.realtime.realtimeStringOf
import com.example.my_kmp_project.core.network.realtime.resolveRealtimeWsUrl
import com.example.my_kmp_project.core.uuid.AppUuid
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * =============================================================================
 * AppRealtimeClientImpl — Realtime 核心状态机
 *
 * 【连接全流程】（与 Dart 一致，client:31-37）
 *   connect → HTTP 换票 → WS connect → auth → auth_ok → sync → sub → 心跳
 *
 * 【收消息】[onEnvelope] 按 `type` 分发；【发消息】[sendEvent] → WS `type:event`。
 *
 * 逐段对齐 Dart `components/realtime/lib/client/app_realtime_client_impl.dart:39-538`
 * （d7876a1），行号在每个成员的 KDoc 里。刻意差异只有四处，全部写在下面：
 * 1. 定时/墙钟/协程作用域走 [RealtimeTicker] + [RealtimeClock] + [scope]，为了可测；
 * 2. 新增 [RealtimeWsTransport.closed] 的关闭码分支（Dart 读不到关闭码），
 *    4001 / 4002 / 4003 ⇒ 丢弃旧票并立刻重换，4002 额外交给会话守护；
 * 3. 全局单例（`AuthLifecycle` / `Get.find<UserService>` / `SessionGuardService` /
 *    `Connectivity`）全部改为注入缝隙（[RealtimeSessionPort] 等）；
 * 4. Dart 的构造期三元选 mock（:56-59）挪到**首次使用时**惰性解析，
 *    这样 ohosX64 的 [RealtimeWsError.Unsupported] 桩不会在依赖装配阶段就炸。
 * =============================================================================
 */
public class AppRealtimeClientImpl(
    private val ticketApi: RealtimeWsTicketApi,
    private val syncApi: RealtimeWsSyncApi,
    private val outboundQueue: RealtimeOutboundQueue,
    private val seqStore: RealtimeSeqStore,
    private val router: RealtimeInboundRouter,
    private val notifyHandler: RealtimeNotifyHandler,
    private val telemetry: RealtimeTelemetry,
    private val session: RealtimeSessionPort,
    private val scope: CoroutineScope,
    private val ticker: RealtimeTicker = CoroutineRealtimeTicker(scope),
    private val clock: RealtimeClock = SystemRealtimeClock,
    private val transportFactory: () -> RealtimeWsTransport = {
        createRealtimeTransport(ticker = ticker, clock = clock)
    },
    private val sessionGuard: RealtimeSessionGuard? = null,
    private val networkMonitor: RealtimeNetworkMonitor = NoopRealtimeNetworkMonitor,
    private val authFailurePolicy: RealtimeAuthHardFailurePolicy = DefaultRealtimeAuthHardFailurePolicy,
    private val reconnectPolicy: ReconnectPolicy = ReconnectPolicy(),
    private val newFrameId: () -> String = { AppUuid.random() },
) : AppRealtimeClient {

    private val _stateFlow = MutableSharedFlow<RealtimeConnectionState>(replay = STATE_REPLAY)
    override val connectionState: Flow<RealtimeConnectionState> = _stateFlow.asSharedFlow()

    private var _state: RealtimeConnectionState = RealtimeConnectionState.Disconnected

    private val subscribedTopics: MutableSet<String> = linkedSetOf()
    private val pendingAcks: MutableMap<String, RealtimeCancellable> = mutableMapOf()

    private var transport: RealtimeWsTransport? = null
    private var heartbeat: HeartbeatScheduler? = null
    private var inboundJob: Job? = null
    private var closedJob: Job? = null
    private var reconnectTask: RealtimeCancellable? = null
    private var networkWatcherStarted: Boolean = false

    private var _reconnectCount: Int = 0
    private var _keepAliveInBackground: Boolean = false
    private var _manualDisconnect: Boolean = false
    private var _initialized: Boolean = false

    /**
     * KMP 侧的「本次断线已处理」闩：Ktor 会同时给 [RealtimeWsTransport.closed] 与
     * 入站流的收尾事件，Dart 只有 `onError` / `onDone` 之一，不夹一道会多算一次重连。
     * 进入 [connectInternal] 时复位。
     */
    private var transportLostHandled: Boolean = false

    override val currentState: RealtimeConnectionState get() = _state
    override val lastSeq: Long get() = seqStore.lastSeq
    override val reconnectCount: Int get() = _reconnectCount
    override val outboundQueueDepth: Int get() = outboundQueue.depth
    override val keepAliveInBackground: Boolean get() = _keepAliveInBackground

    /** Dart `prepare()`（:105-110）：读 seq 水位 + 用真实行数覆盖出站深度。 */
    public override suspend fun prepare() {
        if (_initialized) return
        seqStore.load()
        outboundQueue.refreshDepth()
        _initialized = true
    }

    /**
     * Dart `connect()`（:112-128）：未登录直接 return；
     * 已连接 / 连接中 / 鉴权中时**去重**（防多条 WS）。
     */
    override suspend fun connect() {
        if (!session.isLoggedIn()) {
            AppLog.d("Realtime", "skip connect: not logged in")
            return
        }
        if (_state == RealtimeConnectionState.Connected ||
            _state == RealtimeConnectionState.Connecting ||
            _state == RealtimeConnectionState.Authenticating
        ) {
            AppLog.d("Realtime", "skip connect: already ${_state.label}")
            return
        }
        _manualDisconnect = false
        connectInternal(isReconnect = false)
    }

    /**
     * 内部连接：换票 → 建连 → 发 auth 帧（Dart `_connectInternal`，:131-215）。
     * 失败分支的顺序**不能调整**：先看是否已登出，再看互踢，再看鉴权硬失败，最后才退避重连。
     */
    private suspend fun connectInternal(isReconnect: Boolean) {
        reconnectTask?.cancel()
        // 退出登录后禁止继续换票/重连（避免 401 token 无效刷屏）。
        if (_manualDisconnect || !session.isLoggedIn()) {
            setState(RealtimeConnectionState.Disconnected)
            AppLog.d(
                "Realtime",
                "skip connectInternal: manual=$_manualDisconnect " +
                    "loggedIn=${session.isLoggedIn()}",
            )
            return
        }
        transportLostHandled = false
        setState(
            if (isReconnect) RealtimeConnectionState.Reconnecting else RealtimeConnectionState.Connecting,
        )

        val startedAt = clock.nowMs()
        try {
            val token = session.accessToken()
            val ticket = ticketApi.fetchTicket(token)
            val url = resolveRealtimeWsUrl(ticket.wsUrl)

            activeTransport().connect(url)
            listenInbound()

            setState(RealtimeConnectionState.Authenticating)
            activeTransport().send(
                RealtimeEnvelope(
                    id = newFrameId(),
                    type = TYPE_AUTH,
                    ts = clock.nowMs(),
                    payload = buildJsonObject {
                        put("ticket", ticket.ticket)
                        put("platform", RealtimeConfig.PLATFORM_MOBILE)
                        put("connId", ticket.connId)
                    },
                ),
            )

            telemetry.trace(
                name = "ws_connect",
                durationMs = clock.nowMs() - startedAt,
                params = mapOf("mock" to RealtimeConfig.useMockGateway),
            )
        } catch (t: CancellationException) {
            throw t
        } catch (t: Throwable) {
            telemetry.error("ws_connect_fail", t)
            if (_manualDisconnect || !session.isLoggedIn()) {
                reconnectTask?.cancel()
                setState(RealtimeConnectionState.Disconnected)
                AppLog.d("Realtime", "abort reconnect: logged out during connect")
                return
            }
            val guard = sessionGuard
            if (guard != null && guard.isForceLogoutError(t)) {
                if (guard.tryRecover()) {
                    AppLog.d("Realtime", "session recovered on same device, retry connect")
                    connectInternal(isReconnect)
                    return
                }
                stopReconnectPermanently("session invalid")
                guard.handleIfForceLogout(t)
                return
            }
            // ohosX64 等无 WS 产物的目标：桩异常重连一万次也不会成，直接停（Dart 无此分支）。
            if (t is RealtimeWsError.Unsupported) {
                stopReconnectPermanently(reason = t.message ?: "websocket unsupported")
                return
            }
            // 401 / token 无效：勿指数退避死循环（退出登录或 JWT 已废）。
            if (authFailurePolicy.isHardFailure(t)) {
                if (guard != null) {
                    if (guard.tryRecover() && session.isLoggedIn()) {
                        AppLog.d("Realtime", "token recovered, retry connect")
                        connectInternal(isReconnect)
                        return
                    }
                }
                stopReconnectPermanently(t.toString())
                return
            }
            AppLog.w("Realtime", "connect failed: $t")
            scheduleReconnect(reason = t.toString())
        }
    }

    /** Dart `_stopReconnectPermanently`（:217-222）。 */
    private fun stopReconnectPermanently(reason: String) {
        _manualDisconnect = true
        reconnectTask?.cancel()
        reconnectTask = null
        setState(RealtimeConnectionState.Disconnected)
        AppLog.w("Realtime", "stop reconnect: $reason")
    }

    /**
     * Dart `_listenInbound`（:224-234）：重挂前先取消旧订阅。
     * Dart 的 `onError` / `onDone` 两个回调在 Ktor 侧分别对应「流抛异常」与
     * 「流正常收尾」，都归一到 [handleTransportLost]；关闭码走 [listenClosed]。
     */
    private fun listenInbound() {
        inboundJob?.cancel()
        closedJob?.cancel()
        val ws = activeTransport()
        inboundJob = scope.launch {
            try {
                ws.inbound.collect { onEnvelope(it) }
                handleTransportLost(reason = REASON_STREAM_DONE)
            } catch (ce: CancellationException) {
                throw ce
            } catch (t: Throwable) {
                AppLog.w("Realtime", "inbound error: $t")
                handleTransportLost(reason = t.toString())
            }
        }
        listenClosed(ws)
    }

    /** KMP 扩展（Dart 无关闭码可读）：按码决定「重换票」还是「停重连」。 */
    private fun listenClosed(ws: RealtimeWsTransport) {
        closedJob = scope.launch {
            ws.closed.collect { close ->
                handleTransportLost(reason = "ws_close_${close.code}", close = close)
            }
        }
    }

    /**
     * Dart `_onEnvelope`（:236-270）：`type` 分派表，逐分支照搬。
     * 注意 `event` 分支里 seq 去重与 notify 处理是**两条独立**路径
     * （:259 注释：避免「seq 已更新但 Banner 未展示」）。
     */
    private suspend fun onEnvelope(envelope: RealtimeEnvelope) {
        when (envelope.type) {
            TYPE_AUTH_OK -> {
                // 鉴权成功：重置重连计数，启动心跳，sync 补消息，重新 sub
                reconnectPolicy.reset()
                _reconnectCount = 0
                setState(RealtimeConnectionState.Connected)
                AppLog.d(
                    "Realtime",
                    "auth_ok topics=${subscribedTopics.joinToString(",")} " +
                        "lastSeq=${seqStore.lastSeq}",
                )
                startHeartbeat()
                afterConnected()
            }

            TYPE_PONG -> heartbeat?.onPong(envelope.id ?: "")

            TYPE_ACK -> {
                // 服务端确认收到客户端 event（如 presence.report）
                val refId = realtimeStringOf(envelope.payload[RealtimeEnvelope.KEY_REF_ID])
                if (refId != null) completeAck(refId)
            }

            TYPE_ERROR -> {
                val code = realtimeStringOf(envelope.payload[KEY_ERROR_CODE]) ?: "null"
                AppLog.w("Realtime", "server error $code ${envelope.payload}")
                telemetry.error("ws_server_error", IllegalStateException(code))
            }

            TYPE_EVENT -> {
                val seqAccepted = seqStore.acceptSeq(envelope.seq)
                if (seqAccepted) router.dispatch(envelope)
                if (envelope.topic == RealtimeTopics.SYS_NOTIFY) notifyHandler.handle(envelope)
            }

            else -> Unit
        }
    }

    /**
     * auth_ok 之后（Dart `_afterConnected`，:272-303）：
     * HTTP sync 补离线消息 → 重发出站队列 → 重新订阅 topic → 挂网络监听。
     * sync 失败**只记日志/遥测**，不影响已建立的连接（:291-294）。
     */
    private suspend fun afterConnected() {
        val syncStartedAt = clock.nowMs()
        try {
            val sync = syncApi.sync(
                sinceSeq = seqStore.lastSeq,
                topics = subscribedTopics.toList(),
            )
            for (event in sync.events) {
                val seqAccepted = seqStore.acceptSeq(event.seq)
                if (seqAccepted) router.dispatch(event)
                if (event.topic == RealtimeTopics.SYS_NOTIFY) notifyHandler.handle(event)
            }
            seqStore.setLastSeq(sync.latestSeq)
            telemetry.trace("ws_sync", clock.nowMs() - syncStartedAt)
        } catch (t: Throwable) {
            telemetry.error("ws_sync_fail", t)
            AppLog.w("Realtime", "sync failed: $t")
        }

        replayOutboundQueue()

        if (subscribedTopics.isNotEmpty()) {
            sendSubscribe(subscribedTopics.toList())
        }

        ensureNetworkWatcher()
    }

    /** Dart `_replayOutboundQueue`（:305-315）：注意 Dart 不传 `requireAck` ⇒ 沿用默认 true。 */
    private suspend fun replayOutboundQueue() {
        val items = outboundQueue.pendingItems()
        for (item in items) {
            sendOutbound(
                messageId = item.messageId,
                topic = item.topic,
                eventName = item.eventName,
                payload = item.decodePayload(),
            )
        }
    }

    /** Dart `_startHeartbeat`（:317-331）：先停旧的再起新的，ping 帧只带 id/type/ts。 */
    private fun startHeartbeat() {
        heartbeat?.stop()
        heartbeat = HeartbeatScheduler(
            ticker = ticker,
            clock = clock,
            onSendPing = { pingId ->
                activeTransport().send(
                    RealtimeEnvelope(id = pingId, type = TYPE_PING, ts = clock.nowMs()),
                )
            },
            onTimeout = { handleTransportLost(reason = REASON_HEARTBEAT_TIMEOUT) },
        ).also { it.start() }
    }

    /**
     * Dart `_handleTransportLost`（:333-339）：停心跳 + 遥测 + 关连接 + 排重连。
     *
     * 新增关闭码分支（[close] 非空时）：
     * - 4001 / 4003（票过期类）⇒ 本实现每次连接都换票，这里额外把退避计数归零，
     *   让下一次重连**立刻**重新换票（对齐契约 §5「close 4001/4002/4003 ⇒ 重新换票」）；
     * - 4002（互踢）⇒ 会话已废，退避重连只会反复 401，停重连并交给 [RealtimeSessionGuard]。
     */
    private fun handleTransportLost(reason: String, close: RealtimeWsClose? = null) {
        if (_manualDisconnect) return
        if (transportLostHandled) return
        transportLostHandled = true

        heartbeat?.stop()
        val baseParams = buildMap {
            put("reason", reason)
            if (close != null) put("closeCode", close.code)
        }
        telemetry.metric("ws_transport_lost", baseParams)

        val ws = transport
        if (ws != null) {
            scope.launch { runCatching { ws.close() } }
        }

        if (close != null && close.requiresNewTicket) {
            telemetry.metric("ws_ticket_refresh", mapOf("code" to close.code))
            reconnectPolicy.reset()
        }
        if (close != null && close.code == RealtimeConfig.WS_CLOSE_KICKED) {
            stopReconnectPermanently(reason = "ws_close_${close.code}")
            val guard = sessionGuard
            if (guard != null) scope.launch { guard.handleIfForceLogout(IllegalStateException("ws_kicked")) }
            return
        }
        scheduleReconnect(reason)
    }

    /** Dart `_scheduleReconnect`（:341-354）：计数 +1、状态置重连中、按退避排一次定时器。 */
    private fun scheduleReconnect(reason: String) {
        if (_manualDisconnect || !session.isLoggedIn()) return
        _reconnectCount++
        setState(RealtimeConnectionState.Reconnecting)
        val delayMs = reconnectPolicy.nextDelayMs()
        telemetry.metric(
            "ws_reconnect_scheduled",
            mapOf("reason" to reason, "delayMs" to delayMs, "attempt" to _reconnectCount),
        )
        reconnectTask?.cancel()
        reconnectTask = ticker.after(delayMs) {
            reconnectTask = null
            connectInternal(isReconnect = true)
        }
    }

    /** Dart `_ensureNetworkWatcher`（:356-363）：只挂一次；恢复联网且当前不活跃时补连。 */
    private fun ensureNetworkWatcher() {
        if (networkWatcherStarted) return
        networkWatcherStarted = true
        networkMonitor.start {
            if (!_manualDisconnect && !currentState.isActive) {
                scope.launch { connect() }
            }
        }
    }

    /** Dart `disconnect({reason, clearQueue})`（:365-376）：显式 `code = 1000`。 */
    override suspend fun disconnect(reason: String?, clearQueue: Boolean) {
        _manualDisconnect = true
        reconnectTask?.cancel()
        reconnectTask = null
        heartbeat?.stop()
        inboundJob?.cancel()
        inboundJob = null
        closedJob?.cancel()
        closedJob = null
        transport?.close(code = RealtimeConfig.WS_CLOSE_NORMAL, reason = reason)
        if (clearQueue) outboundQueue.clear()
        setState(RealtimeConnectionState.Disconnected)
        AppLog.d("Realtime", "disconnected reason=$reason")
    }

    /** Dart `subscribeTopics`（:378-384）：先记集合，活跃时才发 `sub` 帧。 */
    override suspend fun subscribeTopics(topics: List<String>) {
        subscribedTopics.addAll(topics)
        if (currentState.isActive) sendSubscribe(topics)
    }

    /** Dart `unsubscribeTopics`（:386-401）：摘除后活跃时才发 `unsub`。 */
    override suspend fun unsubscribeTopics(topics: List<String>) {
        for (t in topics) subscribedTopics.remove(t)
        if (currentState.isActive) {
            activeTransport().send(
                RealtimeEnvelope(
                    id = newFrameId(),
                    type = TYPE_UNSUB,
                    ts = clock.nowMs(),
                    payload = topicsPayload(topics),
                ),
            )
        }
    }

    /** Dart `_sendSubscribe`（:403-412）。 */
    private suspend fun sendSubscribe(topics: List<String>) {
        activeTransport().send(
            RealtimeEnvelope(
                id = newFrameId(),
                type = TYPE_SUB,
                ts = clock.nowMs(),
                payload = topicsPayload(topics),
            ),
        )
    }

    override fun watchTopic(topic: String): Flow<RealtimeEnvelope> = router.watchTopic(topic)

    override fun watchEvents(eventName: String?): Flow<RealtimeEnvelope> =
        router.watchEvents(eventName)

    /**
     * Dart `sendEvent`（:414-445）：**先入队再发送** ——
     * 断线期间消息不丢，连上后由 [replayOutboundQueue] 重发；
     * 不活跃时状态置 [RealtimeConnectionState.Degraded] 并直接返回。
     */
    override suspend fun sendEvent(topic: String, eventName: String, payload: JsonObject?, requireAck: Boolean) {
        val messageId = outboundQueue.enqueue(
            topic = topic,
            eventName = eventName,
            payloadJson = NetworkJson.encodeToString(
                JsonObject.serializer(),
                payload ?: emptyRealtimeObject,
            ),
        )
        if (!currentState.isActive) {
            setState(RealtimeConnectionState.Degraded)
            return
        }
        sendOutbound(
            messageId = messageId,
            topic = topic,
            eventName = eventName,
            payload = payload ?: emptyRealtimeObject,
            requireAck = requireAck,
        )
    }

    /**
     * 构造 `type=event` 帧并发送（Dart `_sendOutbound`，:447-484）。
     * 出站 payload = `{'name': eventName, ...payload}`（:463-466）；
     * `requireAck=false` 时直接标记已确认（:477）。
     */
    private suspend fun sendOutbound(
        messageId: String,
        topic: String,
        eventName: String,
        payload: JsonObject,
        requireAck: Boolean = true,
    ) {
        outboundQueue.markSent(messageId)
        val startedAt = clock.nowMs()
        activeTransport().send(
            RealtimeEnvelope(
                id = messageId,
                type = TYPE_EVENT,
                topic = topic,
                ts = clock.nowMs(),
                payload = buildJsonObject {
                    put(RealtimeEnvelope.KEY_NAME, eventName)
                    payload.forEach { (k, v) -> put(k, v) }
                },
            ),
        )

        if (requireAck) {
            pendingAcks[messageId]?.cancel()
            pendingAcks[messageId] = ticker.after(RealtimeConfig.OUTBOUND_ACK_TIMEOUT_MS) {
                // 句柄失效：先摘掉自己（Dart :472 的 Timer 回调也是先 remove 再上报）
                pendingAcks.remove(messageId)
                telemetry.metric("ws_outbound_ack_timeout", mapOf("messageId" to messageId))
                outboundQueue.markFailed(messageId)
            }
        } else {
            outboundQueue.markAcked(messageId)
        }

        telemetry.trace(
            name = "ws_send",
            durationMs = clock.nowMs() - startedAt,
            params = mapOf("topic" to topic, "eventName" to eventName),
        )
    }

    /** Dart `_completeAck`（:486-489）。 */
    private fun completeAck(refId: String) {
        pendingAcks.remove(refId)?.cancel()
        outboundQueue.markAcked(refId)
    }

    /** Dart `setKeepAliveInBackground`（:491-495）。 */
    override fun setKeepAliveInBackground(enabled: Boolean) {
        _keepAliveInBackground = enabled
        AppLog.d("Realtime", "keepAliveInBackground=$enabled")
    }

    /**
     * Dart `onAppLifecycle(AppLifecycleState)`（:497-509）：
     * resumed ⇒ 非手动断开且不活跃则补连；paused 且不后台保活 ⇒ 活跃时断开（`app_paused`）。
     */
    public fun onAppLifecycle(state: RealtimeAppLifecycle) {
        if (state == RealtimeAppLifecycle.Resumed) {
            if (!_manualDisconnect && !currentState.isActive) {
                scope.launch { connect() }
            }
            return
        }
        if (state == RealtimeAppLifecycle.Paused && !_keepAliveInBackground) {
            if (currentState.isActive) {
                scope.launch { disconnect(reason = REASON_APP_PAUSED, clearQueue = false) }
            }
        }
    }

    /** Dart `_setState`（:511-518）：同值不发；发流 + 日志 + `ws_state` 遥测。 */
    private fun setState(state: RealtimeConnectionState) {
        if (_state == state) return
        val previous = _state
        _state = state
        _stateFlow.tryEmit(state)
        AppLog.d("Realtime", "state ${previous.name} -> ${state.name} (${state.label})")
        telemetry.metric("ws_state", mapOf("state" to state.name))
    }

    /**
     * Dart `dispose()`（:531-537）：断开（不清队列）→ 停网络监听 → 清路由 → 释放传输。
     * `connectionState` 用 SharedFlow，没有 Dart 的「关闭 controller」动作，
     * 状态机置 Disconnected 后不会再发值，等价。
     */
    public suspend fun dispose() {
        disconnect(reason = REASON_DISPOSE, clearQueue = false)
        networkMonitor.stop()
        networkWatcherStarted = false
        router.dispose()
        transport?.dispose()
        transport = null
    }

    /** `sub` / `unsub` 帧的 payload：`{'topics': [...]}`（Dart client:397、:409）。 */
    private fun topicsPayload(topics: List<String>): JsonObject = buildJsonObject {
        put(
            RealtimeEnvelope.KEY_TOPICS,
            JsonArray(topics.map { JsonPrimitive(it) }),
        )
    }

    /** 惰性解析传输（见类注释第 4 点）：mock 开关在这里生效。 */
    private fun activeTransport(): RealtimeWsTransport =
        transport ?: transportFactory().also { transport = it }

    private companion object {
        /** Dart 的 broadcast 流不重放；这里留 1 条，让后到的订阅者立刻拿到当前态。 */
        const val STATE_REPLAY: Int = 1

        const val TYPE_AUTH: String = "auth"
        const val TYPE_AUTH_OK: String = "auth_ok"
        const val TYPE_PING: String = "ping"
        const val TYPE_PONG: String = "pong"
        const val TYPE_ACK: String = "ack"
        const val TYPE_ERROR: String = "error"
        const val TYPE_SUB: String = "sub"
        const val TYPE_UNSUB: String = "unsub"
        const val TYPE_EVENT: String = "event"

        /** Dart `envelope.payload['code']`（client:255）。 */
        const val KEY_ERROR_CODE: String = "code"

        const val REASON_STREAM_DONE: String = "stream_done"
        const val REASON_HEARTBEAT_TIMEOUT: String = "heartbeat_timeout"
        const val REASON_APP_PAUSED: String = "app_paused"
        const val REASON_DISPOSE: String = "dispose"
    }
}

/**
 * 传输选择 —— Dart `AppRealtimeClientImpl` 构造期三元的显式版
 * （client:56-59）：[RealtimeConfig.useMockGateway] ⇒ 进程内 mock 网关，
 * 否则平台 Ktor 实现（ohosX64 为 [RealtimeWsError.Unsupported] 桩）。
 */
public fun createRealtimeTransport(
    ticker: RealtimeTicker,
    clock: RealtimeClock = SystemRealtimeClock,
): RealtimeWsTransport =
    if (RealtimeConfig.useMockGateway) {
        MockRealtimeWsTransport(ticker = ticker, clock = clock)
    } else {
        createPlatformRealtimeWsTransport()
    }

package com.example.my_kmp_project.component.realtime

import com.example.my_kmp_project.core.network.realtime.RealtimeConfig
import com.example.my_kmp_project.core.network.realtime.RealtimeEnvelope
import com.example.my_kmp_project.core.network.realtime.RealtimeTopics
import com.example.my_kmp_project.core.network.realtime.RealtimeWsClose
import com.example.my_kmp_project.core.network.realtime.RealtimeWsTransport
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * 进程内 Mock WS 网关 —— 逐行对齐 Dart
 * `components/realtime/lib/transport/mock_ws_transport.dart:9-180`（d7876a1），
 * 由 [RealtimeConfig.useMockGateway] 显式选路（Dart client:56-59 的构造期三元）。
 *
 * 复刻的模拟协议（与 Dart 完全一致）：
 * - `auth` ⇒ 置 `_authed`，回 `auth_ok`（payload：userId=`mock_user` / sessionId / serverTime）
 * - `ping` ⇒ 回 `pong`，**id 原样回带**（心跳靠 id 匹配，改了就等价于丢包）
 * - `sub`  ⇒ 登记 topic，回 `ack`（payload：refId + 当前 topics 快照），并按需起周期推送
 * - `unsub`⇒ 摘除 topic，回 `ack`；topics 空 ⇒ 停周期推送
 * - `event`⇒ 回 `ack`（payload：refId + `accepted:true`）
 * - 其它出站类型 ⇒ 只记日志，忽略
 *
 * 周期推送：每 [MOCK_PUSH_INTERVAL_MS]（=20s，:104）给每个已登记 topic 造一帧 `event`，
 * `seq` 从 1 起单调自增（:107）——注意它和 [RealtimeSeqStore] 的水位是两回事，
 * mock 前缀 id 也各自不同（`evt_notify_N` / `evt_live_N` / `evt_presence_N`）。
 *
 * 与 Dart 的两处刻意差别，都写在这里以免被当成 bug：
 * 1. [connect] 的 80ms「建连耗时」（:30）改为可注入的 [connectDelayMs]，测试传 0 即可去掉竞态；
 * 2. [closed] 是 KMP 侧新增的关闭码通道，**[close] 不往里发**（Dart 的 mock 从不触发 `onDone`），
 *    只有测试/调试用的 [simulateClose] 会发，用来演练 4001 / 4002 / 4003 分支。
 */
public class MockRealtimeWsTransport(
    private val ticker: RealtimeTicker,
    private val clock: RealtimeClock = SystemRealtimeClock,
    private val connectDelayMs: Long = DEFAULT_CONNECT_DELAY_MS,
) : RealtimeWsTransport {

    private val inboundFlow = MutableSharedFlow<RealtimeEnvelope>(
        replay = 0,
        extraBufferCapacity = CHANNEL_BUFFER,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val closedFlow = MutableSharedFlow<RealtimeWsClose>(
        replay = 0,
        extraBufferCapacity = CLOSED_BUFFER,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val topics: MutableSet<String> = linkedSetOf()
    private var pushTask: RealtimeCancellable? = null
    private var seq: Long = 0L
    private var authed: Boolean = false
    private var connected: Boolean = false
    private var sessionId: String? = null

    override val inbound: Flow<RealtimeEnvelope> get() = inboundFlow
    override val closed: Flow<RealtimeWsClose> get() = closedFlow

    /** Dart `isConnected => _connected`（:20-22）。 */
    override val isConnected: Boolean get() = connected

    /** Dart `connect(uri)`：先 `close()` 复位，再置连接位，最后模拟 80ms 耗时（:24-31）。 */
    override suspend fun connect(url: String) {
        resetState()
        connected = true
        authed = false
        println("[MockWsTransport] connected url=$url")
        if (connectDelayMs > 0L) delay(connectDelayMs)
    }

    override suspend fun send(envelope: RealtimeEnvelope) {
        if (!connected) throw IllegalStateException("Mock WS not connected")

        when (envelope.type) {
            TYPE_AUTH -> {
                authed = true
                val now = clock.nowMs()
                sessionId = "mock_session_$now"
                emit(
                    RealtimeEnvelope(
                        id = "auth_ok_$now",
                        type = TYPE_AUTH_OK,
                        ts = now,
                        payload = buildJsonObject {
                            put("userId", "mock_user")
                            put("sessionId", sessionId ?: "")
                            put("serverTime", clock.nowMs())
                        },
                    ),
                )
            }

            TYPE_PING -> emit(
                RealtimeEnvelope(
                    id = envelope.id,
                    type = TYPE_PONG,
                    ts = clock.nowMs(),
                ),
            )

            TYPE_SUB -> {
                topics.addAll(envelope.payloadTopics())
                emitAck(envelope.id, topicsSnapshot())
                startMockPushIfNeeded()
            }

            TYPE_UNSUB -> {
                for (t in envelope.payloadTopics()) topics.remove(t)
                emitAck(envelope.id, topicsSnapshot())
                if (topics.isEmpty()) cancelPushTimer()
            }

            TYPE_EVENT -> emitAck(envelope.id, buildJsonObject { put("accepted", true) })

            else -> println("[MockWsTransport] ignore outbound type=${envelope.type}")
        }
    }

    /**
     * Dart `close({code, reason})`（:165-173）：停计时 + 复位连接/鉴权/topic，
     * 并像 Dart 一样**不**往 [closed] 发东西（mock 从不触发 `onDone`/`onError`）。
     */
    override suspend fun close(code: Int?, reason: String?) {
        if (code != null || reason != null) {
            println("[MockWsTransport] closed code=$code reason=$reason")
        }
        resetState()
    }

    override fun dispose() {
        resetState()
    }

    /**
     * KMP 扩展（Dart 无对应）：模拟服务端以 [code] 断开，用于演练
     * 4001 认证失败 / 4002 互踢 / 4003 票过期 的换票与停重连分支。
     * 同时复位连接状态（等价于真实链路断开）。
     */
    public suspend fun simulateClose(code: Int, reason: String = "") {
        resetState()
        closedFlow.tryEmit(RealtimeWsClose(code, reason))
    }

    private fun resetState() {
        cancelPushTimer()
        connected = false
        authed = false
        topics.clear()
    }

    private fun cancelPushTimer() {
        pushTask?.cancel()
        pushTask = null
    }

    /** Dart `_emitAck(refId, payload)`（:86-98）：`refId` 非空才写键。 */
    private fun emitAck(refId: String?, payload: JsonObject) {
        emit(
            RealtimeEnvelope(
                id = "ack_${clock.nowMs()}",
                type = TYPE_ACK,
                ts = clock.nowMs(),
                payload = buildJsonObject {
                    if (refId != null) put(RealtimeEnvelope.KEY_REF_ID, refId)
                    payload.forEach { (k, v) -> put(k, v) }
                },
            ),
        )
    }

    private fun topicsSnapshot(): JsonObject = buildJsonObject {
        put(RealtimeEnvelope.KEY_TOPICS, JsonArray(topics.map { JsonPrimitive(it) }))
    }

    /** Dart `_startMockPushIfNeeded()`（:100-157）：先停旧计时，未鉴权或无 topic 则不起。 */
    private fun startMockPushIfNeeded() {
        cancelPushTimer()
        if (!authed || topics.isEmpty()) return
        pushTask = ticker.after(MOCK_PUSH_INTERVAL_MS) { pushTick() }
    }

    /**
     * `Timer.periodic` 的等价实现：每次触发后**自己再排下一次**（[RealtimeTicker] 的句柄一次性语义），
     * 守卫条件与 Dart 的周期回调体首行一致（`!_connected || !_authed ⇒ return`）。
     */
    private suspend fun pushTick() {
        pushTask = null
        if (!connected || !authed) return
        for (topic in topics.toList()) {
            seq++
            val now = clock.nowMs()
            when {
                topic == RealtimeTopics.SYS_NOTIFY -> emit(
                    RealtimeEnvelope(
                        id = "evt_notify_$seq",
                        type = TYPE_EVENT,
                        topic = topic,
                        seq = seq,
                        ts = now,
                        payload = buildJsonObject {
                            put("name", "sys.notify.show")
                            put("notifyId", "mock_notify_ws_$seq")
                            put("title", "Mock 全局通知")
                            put("body", "seq=$seq 来自 WS")
                        },
                    ),
                )

                topic.startsWith(LIVE_SIGNAL_PREFIX) -> emit(
                    RealtimeEnvelope(
                        id = "evt_live_$seq",
                        type = TYPE_EVENT,
                        topic = topic,
                        seq = seq,
                        ts = now,
                        payload = buildJsonObject {
                            put("name", "live.seat_changed")
                            put("roomId", topic.removePrefix(LIVE_SIGNAL_PREFIX))
                            put("seatIndex", seq % 4L)
                        },
                    ),
                )

                topic.startsWith(PRESENCE_PREFIX) -> emit(
                    RealtimeEnvelope(
                        id = "evt_presence_$seq",
                        type = TYPE_EVENT,
                        topic = topic,
                        seq = seq,
                        ts = now,
                        payload = buildJsonObject {
                            put("name", "presence.update")
                            put("online", true)
                            put("count", 100L + seq)
                        },
                    ),
                )
            }
        }
        if (connected && authed && topics.isNotEmpty()) {
            pushTask = ticker.after(MOCK_PUSH_INTERVAL_MS) { pushTick() }
        }
    }

    private fun emit(envelope: RealtimeEnvelope) {
        inboundFlow.tryEmit(envelope)
    }

    private companion object {
        const val TYPE_AUTH: String = "auth"
        const val TYPE_AUTH_OK: String = "auth_ok"
        const val TYPE_PING: String = "ping"
        const val TYPE_PONG: String = "pong"
        const val TYPE_ACK: String = "ack"
        const val TYPE_SUB: String = "sub"
        const val TYPE_UNSUB: String = "unsub"
        const val TYPE_EVENT: String = "event"

        /** mock 网关里的 `live.signal.` 前缀（:124、:133）。 */
        const val LIVE_SIGNAL_PREFIX: String = "live.signal."

        /** `presence.` 前缀（:139）：`presence.bulk` 与 `presence.user.x` 都命中。 */
        const val PRESENCE_PREFIX: String = "presence."

        /** Dart `Duration(seconds: 20)`（:104）。 */
        const val MOCK_PUSH_INTERVAL_MS: Long = 20_000L

        /** Dart `Future.delayed(80ms)`（:30）。 */
        const val DEFAULT_CONNECT_DELAY_MS: Long = 80L

        const val CHANNEL_BUFFER: Int = 64
        const val CLOSED_BUFFER: Int = 8
    }
}

package com.example.my_kmp_project.component.realtime

import com.example.my_kmp_project.core.network.ApiClient
import com.example.my_kmp_project.core.network.ApiResponse
import com.example.my_kmp_project.core.network.HttpTextResponse
import com.example.my_kmp_project.core.network.realtime.RealtimeEnvelope
import com.example.my_kmp_project.core.network.realtime.RealtimeWsClose
import com.example.my_kmp_project.core.network.realtime.RealtimeWsTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * Realtime lane 单测的双件。
 *
 * 生产代码本身把**时间**抽成了 [RealtimeClock] / [RealtimeTicker] 缝隙
 * （设计说明见 `RealtimeScheduler.kt`），所以「记录型」假件即可完全确定化：
 * 本仓库的 commonTest 依赖里没有 `kotlinx-coroutines-test`（见 composeApp 构建脚本的
 * commonTest 声明），且 commonTest 要连 ohosX64 一起编译 ⇒ **禁止** Ktor 类型。
 *
 * 协程统一挂 `Dispatchers.Unconfined`：`SharedFlow.tryEmit` 会就地恢复订阅者，
 * 于是「推一帧入站 → 状态机跑完」在同一调用栈里同步完成，不依赖任何真实延时。
 */

/** 可步进墙钟（Dart `DateTime.now().millisecondsSinceEpoch`）。 */
internal class SteppingRealtimeClock(private var now: Long = 1_700_000_000_000L) : RealtimeClock {
    override fun nowMs(): Long = now
    fun advance(ms: Long) {
        now += ms
    }
}

/**
 * 记录型调度器：`after` 只登记、绝不启动协程。
 * 测试自己决定「哪个定时器到点」，因此心跳 25s/10s、退避 1s→2s、ack 超时 15s 都能逐拍断言。
 */
internal class ManualRealtimeTicker : RealtimeTicker {
    private class Task(val delayMs: Long, val action: suspend () -> Unit)

    private val tasks = mutableListOf<Task>()

    /** 创建过的全部延时（含随后被取消的），用于断言协议常量。 */
    val createdDelays: MutableList<Long> = mutableListOf()

    /** 当前仍在等待触发的延时。 */
    val pendingDelays: List<Long> get() = tasks.map { it.delayMs }

    override fun after(delayMs: Long, action: suspend () -> Unit): RealtimeCancellable {
        val task = Task(delayMs, action)
        createdDelays += delayMs
        tasks += task
        return RealtimeCancellable { tasks.remove(task) }
    }

    /** 触发最早登记的待命任务；没有则返回 false。 */
    suspend fun runFirst(): Boolean {
        val task = tasks.firstOrNull() ?: return false
        tasks.remove(task)
        task.action()
        return true
    }

    /** 有界推进：最多跑 [max] 拍，避免周期任务把测试拖成死循环。 */
    suspend fun runAtMost(max: Int): Int {
        var ran = 0
        repeat(max) {
            if (!runFirst()) return ran
            ran++
        }
        return ran
    }

    /**
     * 触发**第一个**该延时的待命任务。
     * 心跳的 25000 与 ack 的 15000 会同时在表里，按延时取才能选中目标那一个。
     */
    suspend fun runWithDelay(delayMs: Long): Boolean {
        val task = tasks.firstOrNull { it.delayMs == delayMs } ?: return false
        tasks.remove(task)
        task.action()
        return true
    }

    fun clear() {
        tasks.clear()
    }
}

/**
 * 立即触发的调度器：登记延时的**同时**就地跑任务体。
 *
 * 用途很窄但必要：[RealtimeStartupGate] 的 12s / 8s 等待走 `CompletableDeferred.await()`，
 * 若用 [ManualRealtimeTicker] 则没人触发 → 测试协程永久挂起；用生产
 * [CoroutineRealtimeTicker] 又要真的等 8 秒。这个假件把「期限已到」变成同步事件，
 * 既记录了真实延时常量（[createdDelays]）又不消耗墙钟时间。
 */
internal class ImmediateRealtimeTicker(private val scope: CoroutineScope) : RealtimeTicker {
    val createdDelays: MutableList<Long> = mutableListOf()

    override fun after(delayMs: Long, action: suspend () -> Unit): RealtimeCancellable {
        createdDelays += delayMs
        val job = scope.launch { action() }
        return RealtimeCancellable { job.cancel() }
    }
}

/** Unconfined 作用域 + 其根 Job（测试结束必须 cancel，绝不留悬挂协程）。 */
internal class RealtimeTestScope {
    private val root: Job = Job()
    val scope: CoroutineScope = CoroutineScope(Dispatchers.Unconfined + root)

    /** 此刻仍在运行的协程数（心跳 / 入站订阅 / 重连任务都不该在断言后还活着）。 */
    fun activeChildren(): Int = root.children.count()

    fun cancel() {
        root.cancel()
    }
}

/**
 * 假 WS 传输：记录出站帧与拨号 URL，入站/关闭由测试手动推。
 * 刻意**不**自动应答（区别于 [MockRealtimeWsTransport]），好让每一条入站帧都可控。
 */
internal class FakeRealtimeWsTransport : RealtimeWsTransport {
    private val inboundFlow = MutableSharedFlow<RealtimeEnvelope>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val closedFlow = MutableSharedFlow<RealtimeWsClose>(
        replay = 0,
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    val sent: MutableList<RealtimeEnvelope> = mutableListOf()
    val dialedUrls: MutableList<String> = mutableListOf()
    val closeCalls: MutableList<Pair<Int?, String?>> = mutableListOf()
    var disposedCount: Int = 0
    var connected: Boolean = false

    /** 置非 null 后，connect / send 会抛该异常（模拟建连失败、发送失败）。 */
    var connectFailure: Throwable? = null
    var sendFailure: Throwable? = null

    override val inbound: Flow<RealtimeEnvelope> get() = inboundFlow
    override val closed: Flow<RealtimeWsClose> get() = closedFlow
    override val isConnected: Boolean get() = connected

    override suspend fun connect(url: String) {
        dialedUrls += url
        connectFailure?.let { throw it }
        connected = true
    }

    override suspend fun send(envelope: RealtimeEnvelope) {
        if (!connected) throw IllegalStateException("WebSocket not connected")
        sent += envelope
        sendFailure?.let { throw it }
    }

    override suspend fun close(code: Int?, reason: String?) {
        closeCalls += code to reason
        connected = false
    }

    override fun dispose() {
        disposedCount++
        connected = false
    }

    /** 推一帧入站；订阅者挂在 Unconfined 上，返回时状态机已跑完这一帧。 */
    fun pushInbound(envelope: RealtimeEnvelope) {
        check(inboundFlow.tryEmit(envelope)) { "入站帧未被消费：${envelope.type}" }
    }

    fun pushClosed(code: Int, reason: String = "") {
        check(closedFlow.tryEmit(RealtimeWsClose(code, reason))) { "关闭事件未被消费：$code" }
    }

    /** 出站帧的类型序列，用于「auth → sub」这类顺序断言。 */
    fun sentTypes(): List<String> = sent.map { it.type }

    fun firstSentOfType(type: String): RealtimeEnvelope? = sent.firstOrNull { it.type == type }
}

/** 出站队列表的假实现：复用生产内存表，同时记录状态流转顺序。 */
internal class RecordingOutboundStore : RealtimeOutboundStore {
    private val delegate = InMemoryRealtimeOutboundStore()
    val statusLog: MutableList<Pair<String, String>> = mutableListOf()

    override fun upsert(row: RealtimeOutboundRow) = delegate.upsert(row)

    override fun updateStatus(messageId: String, status: String, nowMs: Long) {
        statusLog += messageId to status
        delegate.updateStatus(messageId, status, nowMs)
    }

    override fun delete(messageId: String) = delegate.delete(messageId)

    override fun pending(): List<RealtimeOutboundRow> = delegate.pending()

    override fun clear() = delegate.clear()
}

/** 遥测出口：把 Dart 的 `EventBusUtils.post(CustomEvent)` 面转成可断言的列表。 */
internal class RecordingTelemetrySink : RealtimeTelemetrySink {
    class Event(val eventType: String, val payload: Map<String, Any?>)

    val events: MutableList<Event> = mutableListOf()

    override fun report(eventType: String, payload: Map<String, Any?>) {
        events += Event(eventType, payload)
    }

    fun ofType(type: String): List<Event> = events.filter { it.eventType == type }

    fun lastPayload(type: String): Map<String, Any?> = ofType(type).last().payload
}

internal class FakeSessionPort(
    var loggedIn: Boolean = true,
    var token: String = "access-token",
) : RealtimeSessionPort {
    var accessTokenCalls: Int = 0

    override fun isLoggedIn(): Boolean = loggedIn

    override fun accessToken(): String {
        accessTokenCalls++
        // Dart `StateError('未登录或 token 为空…')`：文案含 token ⇒ 被鉴权硬失败策略认下。
        if (!loggedIn || token.isEmpty()) throw IllegalStateException("未登录或 token 为空，无法建 WS")
        return token
    }
}

internal class RecordingNotifyHandler : RealtimeNotifyHandler {
    val handled: MutableList<RealtimeEnvelope> = mutableListOf()

    override suspend fun handle(envelope: RealtimeEnvelope) {
        handled += envelope
    }
}

internal class FakeSessionGuard(
    var forceLogoutError: Boolean = false,
    var recoverSucceeds: Boolean = false,
) : RealtimeSessionGuard {
    val handledErrors: MutableList<Throwable> = mutableListOf()

    override fun isForceLogoutError(error: Throwable): Boolean = forceLogoutError

    override suspend fun tryRecover(): Boolean = recoverSucceeds

    override suspend fun handleIfForceLogout(error: Throwable) {
        handledErrors += error
    }
}

internal class RecordingNetworkMonitor : RealtimeNetworkMonitor {
    var startCount: Int = 0
    var stopCount: Int = 0
    private var onOnline: (() -> Unit)? = null

    override fun start(onOnline: () -> Unit) {
        startCount++
        this.onOnline = onOnline
    }

    override fun stop() {
        stopCount++
        onOnline = null
    }

    fun fireOnline() {
        onOnline?.invoke()
    }

    val hasListener: Boolean get() = onOnline != null
}

/**
 * [AppRealtimeClient] 的手写假件：只记录闸门下发的指令，不跑状态机。
 *
 * [RealtimeStartupGate] 只依赖接口（`client.connect()` / `subscribeTopics` / `disconnect`），
 * 所以闸门逻辑可以完全不碰 HTTP / WS，只断言「下发了什么、顺序对不对」。
 */
internal class FakeRealtimeClient(
    var state: RealtimeConnectionState = RealtimeConnectionState.Disconnected,
    /** false ⇒ `connect()` 只走到 Connecting（用于演练「连不上」的超时分支）。 */
    var connectSucceeds: Boolean = true,
) : AppRealtimeClient {
    private val _stateFlow = MutableSharedFlow<RealtimeConnectionState>(replay = 1)
    override val connectionState: Flow<RealtimeConnectionState> = _stateFlow.asSharedFlow()

    var connectCalls: Int = 0
    var prepareCalls: Int = 0
    val disconnectCalls: MutableList<Pair<String?, Boolean>> = mutableListOf()
    val subscribeCalls: MutableList<List<String>> = mutableListOf()
    val unsubscribeCalls: MutableList<List<String>> = mutableListOf()
    val sentEvents: MutableList<String> = mutableListOf()
    var keepAliveCalls: MutableList<Boolean> = mutableListOf()

    override val currentState: RealtimeConnectionState get() = state
    override val lastSeq: Long get() = 7L
    override val reconnectCount: Int get() = 0
    override val outboundQueueDepth: Int get() = 0
    override val keepAliveInBackground: Boolean get() = keepAliveCalls.lastOrNull() == true

    private fun moveTo(next: RealtimeConnectionState) {
        state = next
        _stateFlow.tryEmit(next)
    }

    override suspend fun prepare() {
        prepareCalls++
    }

    override suspend fun connect() {
        connectCalls++
        moveTo(if (connectSucceeds) RealtimeConnectionState.Connected else RealtimeConnectionState.Connecting)
    }

    override suspend fun disconnect(reason: String?, clearQueue: Boolean) {
        disconnectCalls += reason to clearQueue
        moveTo(RealtimeConnectionState.Disconnected)
    }

    override suspend fun subscribeTopics(topics: List<String>) {
        subscribeCalls += topics
    }

    override suspend fun unsubscribeTopics(topics: List<String>) {
        unsubscribeCalls += topics
    }

    override fun watchTopic(topic: String): Flow<RealtimeEnvelope> = flowOf()

    override fun watchEvents(eventName: String?): Flow<RealtimeEnvelope> = flowOf()

    override suspend fun sendEvent(
        topic: String,
        eventName: String,
        payload: JsonObject?,
        requireAck: Boolean,
    ) {
        sentEvents += "$topic/$eventName"
    }

    override fun setKeepAliveInBackground(enabled: Boolean) {
        keepAliveCalls += enabled
    }
}

/**
 * 只实装 realtime 用到的 postRaw 的假 client（与 core:network 同名假件同款）。
 *
 * 响应队列与失败都是**可变**的：状态机会在一条用例里换票好几次（重连、恢复重试），
 * 用构造期固定队列很难对齐次数。
 */
internal class RecordingRawApiClient(
    initialResponses: List<String> = emptyList(),
) : ApiClient {
    val queue: ArrayDeque<String> = ArrayDeque(initialResponses)
    val calls: MutableList<Pair<String, String>> = mutableListOf()
    private var failure: Throwable? = null
    private var failuresLeft: Int = 0

    /**
     * 让接下来 [times] 次调用抛 [error]，之后回到 [queue] 正常应答。
     * 「只失败一次」是必须的：鉴权硬失败分支里 `guard.tryRecover()` 成功后会**递归**重跑
     * `connectInternal`（client:207-214），永久失败的假件会直接把测试拖成栈溢出。
     */
    fun failNext(times: Int = 1, error: Throwable) {
        failure = error
        failuresLeft = times
    }

    fun respond(vararg bodies: String) {
        bodies.forEach { queue.add(it) }
    }

    override suspend fun postRaw(path: String, body: String): String {
        calls += path to body
        val f = failure
        if (f != null && failuresLeft > 0) {
            failuresLeft--
            throw f
        }
        check(queue.isNotEmpty()) { "第 ${calls.size} 次调用（$path）没有准备响应体，请 respond(...)" }
        return queue.removeFirst()
    }

    override suspend fun getRaw(path: String, query: Map<String, String>): String = unused("getRaw")
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

/** 裸 body 换票响应（契约表 #97）：字段与 Go `RealtimeTicketData` 一致。 */
internal const val TICKET_OK_BODY: String =
    """{"ticket":"tk-1","wsUrl":"wss://rt.example.com:8443/ws/realtime","expiresInSeconds":60,"connId":"cn-1"}"""

/** 裸 body 补投响应（契约表 #98）。 */
internal fun syncBody(eventsJson: String, latestSeq: Long): String =
    """{"events":$eventsJson,"latestSeq":$latestSeq}"""

internal val EMPTY_SYNC_BODY: String = syncBody("[]", 0L)

/** 事件入站帧（Dart `type == 'event'` 才会路由）。 */
internal fun eventEnvelope(
    seq: Long?,
    topic: String? = "presence.bulk",
    name: String = "presence.update",
    id: String = "evt-$seq",
    extraFields: String = "",
): RealtimeEnvelope {
    val seqJson = if (seq == null) "" else """"seq":$seq,"""
    val topicJson = if (topic == null) "" else """"topic":"$topic","""
    val tail = if (extraFields.isEmpty()) "" else ",$extraFields"
    return requireNotNull(
        RealtimeEnvelope.fromRawJson(
            """{"id":"$id","type":"event",$topicJson$seqJson"payload":{"name":"$name"$tail}}""",
        ),
    )
}

/** 控制帧（pong / ack / error 等）。 */
internal fun controlEnvelope(
    type: String,
    id: String? = null,
    payloadJson: String = "{}",
): RealtimeEnvelope {
    val idJson = if (id == null) "" else """"id":"$id","""
    return requireNotNull(
        RealtimeEnvelope.fromRawJson("""{$idJson"type":"$type","payload":$payloadJson}"""),
    )
}

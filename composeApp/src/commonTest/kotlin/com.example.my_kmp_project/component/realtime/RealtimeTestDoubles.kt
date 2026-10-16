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
import kotlinx.serialization.json.JsonElement

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

    fun clear() {
        tasks.clear()
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

/** 只实装 realtime 用到的 postRaw 的假 client（与 core:network 同名假件同款）。 */
internal class RecordingRawApiClient(
    responses: List<String>,
    private val failure: Throwable? = null,
) : ApiClient {
    private val queue = ArrayDeque(responses)
    val calls: MutableList<Pair<String, String>> = mutableListOf()

    override suspend fun postRaw(path: String, body: String): String {
        calls += path to body
        if (failure != null) throw failure
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

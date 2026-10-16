package com.example.my_kmp_project.core.network.realtime

import com.example.my_kmp_project.core.network.NetworkError
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.url
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readReason
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlin.concurrent.Volatile

/**
 * `ktor-client-websockets` 传输（networkKtorMain：android / ios* / ohosArm64）。
 *
 * 与 Dart `components/realtime/lib/transport/websocket_transport.dart:11-68`（d7876a1）一一对应：
 * - `connect` 先关旧连接（:24）
 * - 文本帧 → `RealtimeEnvelope`；解析失败**只记日志、不中断流**（:29-34）
 * - 流结束 / 出错 → 走 [RealtimeWsTransport.closed]（Dart 只能 `addError(WebSocketTransportClosed)`，:36-40）
 * - 未连接时 `send` 抛 [IllegalStateException]（Dart `StateError('WebSocket not connected')`，:47）
 * - `close(code ?? 1000, reason)`（:55）
 *
 * [http] 必须是装好 [WebSockets] 插件的 client（见 [installRealtimeWebSockets]）；
 * 引擎由平台 actual 决定：OkHttp / Darwin / CIO。
 */
public class KtorRealtimeWsTransport(
    private val http: HttpClient,
) : RealtimeWsTransport {

    private val _inbound = MutableSharedFlow<RealtimeEnvelope>(
        replay = 0,
        extraBufferCapacity = INBOUND_BUFFER,
        onBufferOverflow = BufferOverflow.SUSPEND,
    )
    private val _closed = MutableSharedFlow<RealtimeWsClose>(
        replay = 0,
        extraBufferCapacity = CLOSED_BUFFER,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    @Volatile
    private var session: DefaultClientWebSocketSession? = null

    private var readJob: Job? = null

    override val inbound: Flow<RealtimeEnvelope> get() = _inbound
    override val closed: Flow<RealtimeWsClose> get() = _closed

    /** Dart `isConnected => _channel != null`（websocket_transport.dart:20）。 */
    override val isConnected: Boolean get() = session != null

    override suspend fun connect(url: String) {
        close()
        val started = try {
            http.webSocketSession { url(url) }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (t: Throwable) {
            throw NetworkError.Transport(
                "WS 拨号失败：${t.message ?: url}",
                (t as? ResponseException)?.response?.status?.value,
            )
        }
        session = started
        readJob = started.launch { readLoop(started) }
    }

    override suspend fun send(envelope: RealtimeEnvelope) {
        val current = session ?: throw IllegalStateException("WebSocket not connected")
        current.send(Frame.Text(envelope.asWsText()))
    }

    override suspend fun close(code: Int?, reason: String?) {
        val current = session
        session = null
        readJob?.cancel()
        readJob = null
        if (current == null) return
        val closeReason = CloseReason(
            (code ?: RealtimeConfig.WS_CLOSE_NORMAL).toShort(),
            reason.orEmpty(),
        )
        runCatching { current.close(closeReason) }
    }

    override fun dispose() {
        readJob?.cancel()
        readJob = null
        session = null
        runCatching { http.close() }
    }

    /**
     * 读循环：`incoming` 结束（对端关闭 / 引擎异常）后必须发一次 [RealtimeWsClose]，
     * 否则状态机感知不到掉线（Dart 靠 `onDone`/`onError` 回调，websocket_transport.dart:36-40）。
     */
    private suspend fun readLoop(current: DefaultClientWebSocketSession) {
        var reported = false
        try {
            for (frame in current.incoming) {
                when (frame) {
                    is Frame.Text -> {
                        val envelope = RealtimeEnvelope.fromRawJson(frame.readText())
                        if (envelope != null) {
                            _inbound.emit(envelope)
                        } else {
                            println("[KtorRealtimeWsTransport] parse error (frame dropped)")
                        }
                    }
                    is Frame.Close -> {
                        reported = true
                        _closed.emit(closeInfoOf(frame))
                    }
                    else -> Unit
                }
                if (reported) break
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (t: Throwable) {
            // Dart：onError → addError；这里统一归成一次关闭事件交给状态机重连。
            println("[KtorRealtimeWsTransport] stream error: ${t.message}")
        } finally {
            if (session === current) session = null
            if (!reported) {
                val info = awaitedClose(current)
                runCatching { _closed.emit(info) }
            }
        }
    }

    /** `Frame.Close` → 关闭码 + 文本；空载荷按 1005（No Status Received）。 */
    private fun closeInfoOf(frame: Frame.Close): RealtimeWsClose {
        val reason = runCatching { frame.readReason() }.getOrNull()
        return RealtimeWsClose(
            code = reason?.code?.toInt() ?: RealtimeConfig.WS_CLOSE_NO_STATUS,
            reason = reason?.message.orEmpty(),
        )
    }

    /** 没拿到 Close 帧时读 `closeReason` Deferred；拿不到就按异常断开处理。 */
    private suspend fun awaitedClose(current: DefaultClientWebSocketSession): RealtimeWsClose {
        val reason = runCatching { current.closeReason.await() }.getOrNull()
        return RealtimeWsClose(
            code = reason?.code?.toInt() ?: RealtimeConfig.WS_CLOSE_NO_STATUS,
            reason = reason?.message.orEmpty(),
        )
    }

    private companion object {
        const val INBOUND_BUFFER: Int = 64
        const val CLOSED_BUFFER: Int = 8
    }
}

/**
 * WS client 的最小安装：只装 `WebSockets`。
 * 刻意**不**复用 [com.example.my_kmp_project.core.network.installDemoDefaults] ——
 * 那套带了 HTTP baseUrl / JSON 内容协商 / 30s 超时，会把长连接的 socket 超时算进来，
 * 与 Dart 侧独立 `WebSocketChannel` 的行为不一致。鉴权也不走 HTTP 头，
 * 由 `auth` 帧携带 ticket（app_realtime_client_impl.dart:158-169）。
 */
public fun HttpClientConfig<*>.installRealtimeWebSockets() {
    install(WebSockets) {
        // 应用层心跳由 HeartbeatScheduler 负责（25s ping / 10s pong），
        // 这里不启用协议层 pingInterval，避免与业务心跳重复计数。
        pingIntervalMillis = 0L
    }
}

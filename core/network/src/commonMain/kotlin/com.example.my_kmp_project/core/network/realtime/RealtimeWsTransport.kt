package com.example.my_kmp_project.core.network.realtime

import kotlinx.coroutines.flow.Flow

/**
 * 平台可替换的 WebSocket 传输缝隙 —— Dart
 * `components/realtime/lib/transport/ws_transport.dart:5-17`（d7876a1）的 KMP 对等抽象。
 *
 * | Dart | KMP |
 * |---|---|
 * | `Stream<RealtimeEnvelope> get inbound` | [inbound]（冷流由实现内部做多播） |
 * | `bool get isConnected` | [isConnected] |
 * | `Future<void> connect(Uri uri)` | [connect]（**字符串 URL**：Dart 也是 `Uri.parse(ticket.wsUrl)` 后原样拨号） |
 * | `Future<void> send(RealtimeEnvelope)` | [send] |
 * | `Future<void> close({int? code, String? reason})` | [close] |
 * | `void dispose()` | [dispose] |
 *
 * 相对 Dart 的**刻意扩展**：[closed] 携带关闭码。
 * Dart 的 `web_socket_channel` 只给 `onDone`，`RealtimeConfig` 里的
 * 4001/4002/4003（realtime_config.dart:47-49）**声明了但从未被读取**，
 * 重连时靠「每次都重新换票」间接达成同等行为（client:151）。
 * KMP 侧 Ktor 能拿到 close code，因此把它显式暴露出来，由状态机决定
 * 「丢弃旧票 + 立即重新换票 / 互踢时停止重连」。
 *
 * 解析失败的帧按 Dart 语义**吞掉并记日志**（websocket_transport.dart:29-34），不中断流。
 */
public interface RealtimeWsTransport {
    /** 入站帧流（已解码为 [RealtimeEnvelope]）。 */
    public val inbound: Flow<RealtimeEnvelope>

    /** 连接关闭事件流：`code` 为 WS 关闭码，未收到时 [RealtimeConfig.WS_CLOSE_NO_STATUS]。 */
    public val closed: Flow<RealtimeWsClose>

    /** 是否已建立连接（Dart `isConnected => _channel != null`，websocket_transport.dart:20）。 */
    public val isConnected: Boolean

    /** 拨号；实现必须先关闭上一条连接（websocket_transport.dart:24）。 */
    public suspend fun connect(url: String)

    /** 发一帧；未连接时抛 [IllegalStateException]（Dart `StateError('WebSocket not connected')`，:47）。 */
    public suspend fun send(envelope: RealtimeEnvelope)

    /** 主动关闭；`code` 缺省 1000（Dart `sink.close(code ?? 1000, reason)`，:55）。 */
    public suspend fun close(code: Int? = null, reason: String? = null)

    /** 释放：关闭连接并丢弃流（Dart `dispose()`，:60-63）。 */
    public fun dispose()
}

/** WS 关闭信息（`code` 用 [Int] 承载，Ktor 的 `CloseReason.code` 是 Short）。 */
public data class RealtimeWsClose(
    val code: Int,
    val reason: String = "",
) {
    /** 是否要求重新换票（4001 / 4002 / 4003；RealtimeConfig.requiresNewTicket）。 */
    public val requiresNewTicket: Boolean get() = RealtimeConfig.requiresNewTicket(code)
}

/**
 * 平台 WS 传输工厂 —— 与 [com.example.my_kmp_project.core.network.createPlatformApiClient]
 * 同一套 expect/actual 机制：
 * android / ios / ohosArm64 = Ktor `ktor-client-websockets`（networkKtorMain），
 * ohosX64 = 明确抛 [RealtimeWsError.Unsupported] 的桩（无 CPF Ktor ohosX64 产物）。
 */
public expect fun createPlatformRealtimeWsTransport(): RealtimeWsTransport

/**
 * 按 [RealtimeConfig.useMockGateway] 选路（Dart client:56-59 的构造期三元表达式）。
 * mock 实现位于 `composeApp` 的 `component/realtime`（core 层不反向依赖组件层），
 * 因此这里只负责真链路；调用方自己处理 mock 分支。
 */
public fun createRealtimeWsTransport(): RealtimeWsTransport =
    createPlatformRealtimeWsTransport()

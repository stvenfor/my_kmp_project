package com.example.my_kmp_project.core.network.realtime

/**
 * ohosX64: **无 WebSocket 能力**。
 *
 * GAP(R5): CPF 未发布 `ktor-client-websockets` / `ktor-client-core` 的 ohosX64 klib
 * （与 Coil `3.3.0-0.3.0`、`com.benasher44:uuid` 的 ohosX64 缺口同一原因，
 * 参照 `composeApp/src/ohosX64Main/.../core/ui/PlatformNetworkImage.ohosX64.kt`、
 * `core/uuid/AppUuid.ohosX64.kt` 的占位策略）。
 * OHOS 侧 `net_http` cinterop 只有 HTTP，没有 WS 帧协议，**不做伪实现**：
 * 这里直接抛类型化 [RealtimeWsError.Unsupported]，让状态机记 `ws_connect_fail` 后停重连，
 * 而不是假装连上。真机（ohosArm64）走 `RealtimeWsTransportFactory.ohosArm64`。
 *
 * Dart 参考：`components/realtime/lib/transport/websocket_transport.dart:11-68`
 * （真实现依赖 `web_socket_channel`，KMP ohosX64 无对应产物）。
 */
public actual fun createPlatformRealtimeWsTransport(): RealtimeWsTransport {
    throw RealtimeWsError.Unsupported()
}

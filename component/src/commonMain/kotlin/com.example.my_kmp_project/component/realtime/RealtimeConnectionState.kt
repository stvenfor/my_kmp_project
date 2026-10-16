package com.example.my_kmp_project.component.realtime

/**
 * 全局 WS 连接状态 —— 逐一对齐 Dart
 * `commons/core/lib/model/realtime/realtime_connection_state.dart:2-25`（d7876a1）。
 *
 * `failed` 在 Dart 里只被 `RealtimeInitializer._waitUntilConnected` 当作
 * 「别再等了」的终止条件（realtime_initializer.dart:156-158），状态机本身不写入它。
 */
public enum class RealtimeConnectionState {
    Disconnected,
    Connecting,
    Authenticating,
    Connected,
    Reconnecting,
    Degraded,
    Failed,
}

/** Dart `RealtimeConnectionStateX`（同文件 :12-25）。 */
public val RealtimeConnectionState.isActive: Boolean
    get() = this == RealtimeConnectionState.Connected || this == RealtimeConnectionState.Degraded

/** 中文文案与 Dart `label` 完全一致（调试页 / 日志用）。 */
public val RealtimeConnectionState.label: String
    get() = when (this) {
        RealtimeConnectionState.Disconnected -> "未连接"
        RealtimeConnectionState.Connecting -> "连接中"
        RealtimeConnectionState.Authenticating -> "鉴权中"
        RealtimeConnectionState.Connected -> "已连接"
        RealtimeConnectionState.Reconnecting -> "重连中"
        RealtimeConnectionState.Degraded -> "降级"
        RealtimeConnectionState.Failed -> "失败"
    }

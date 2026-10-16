package com.example.my_kmp_project.core.network.realtime

import kotlin.concurrent.Volatile

/**
 * Realtime 模块的「开关与常量」集中配置 —— 逐行对齐 Dart
 * `components/realtime/lib/config/realtime_config.dart:11-61`（d7876a1）。
 *
 * 时长单位统一改成 **毫秒 Long**（Kotlin -common 没有 `Duration` 的跨端定时语义），
 * 数值与 Dart 完全一致：心跳 25s/10s、连续丢 2 次才重连、退避 1s→2s→4s… 上限 60s、抖动 20%。
 *
 * 未移植项（属 UI wave，本 wave 明确不搬）：`notifyBannerAutoDismiss`、`notifyBannerAnimation`
 * （realtime_config.dart:41-44，只服务 `ui/realtime_notify_banner_*.dart`）。
 */
public object RealtimeConfig {
    /** Dart `useMockGateway`（realtime_config.dart:15）：true = 进程内 mock 网关，无需后端。 */
    @Volatile
    public var useMockGateway: Boolean = false

    /** 换票 / 增量补投路径（realtime_config.dart:18-19；契约表 §2.17 #97/#98）。 */
    public const val TICKET_PATH: String = "/api/v1/realtime/ws-ticket"
    public const val SYNC_PATH: String = "/api/v1/realtime/sync"

    /** 换票请求固定 `platform`（`ws_ticket_api.dart:26`）与 auth 帧里的 `platform`（client:165）。 */
    public const val PLATFORM_MOBILE: String = "mobile"

    /** 应用层心跳：每 25s 发 ping，10s 内须收到同 id 的 pong，连续丢 2 次才重连。 */
    public const val HEARTBEAT_INTERVAL_MS: Long = 25_000L
    public const val HEARTBEAT_TIMEOUT_MS: Long = 10_000L
    public const val HEARTBEAT_MAX_MISS: Int = 2

    /** 重连指数退避：1s 起、×2、上限 60s、±20% 抖动，最小 500ms。 */
    public const val RECONNECT_INITIAL_MS: Long = 1_000L
    public const val RECONNECT_MAX_MS: Long = 60_000L
    public const val RECONNECT_MULTIPLIER: Double = 2.0
    public const val RECONNECT_JITTER_RATIO: Double = 0.2
    public const val RECONNECT_FLOOR_MS: Long = 500L

    /** 客户端 `sendEvent` 等 ack 的上限，超时标记失败（realtime_config.dart:35）。 */
    public const val OUTBOUND_ACK_TIMEOUT_MS: Long = 15_000L

    /** notifyId 去重队列最大长度（realtime_config.dart:38）。 */
    public const val NOTIFY_DEDUP_MAX: Int = 200

    /**
     * WS 关闭码：与 Go `entity.WSClose*` 一致（realtime_config.dart:47-49）。
     * 4001 认证失败 / 4002 互踢 / 4003 ticket 过期 ⇒ 必须重新换票。
     */
    public const val WS_CLOSE_AUTH_FAILED: Int = 4001
    public const val WS_CLOSE_KICKED: Int = 4002
    public const val WS_CLOSE_TICKET_EXPIRED: Int = 4003

    /**
     * 该关闭码是否要求**重新换票**。
     *
     * Dart 事实（务必知悉）：`RealtimeConfig` 声明了三个码，但**全仓没有任何读取点**
     * （`grep wsCloseAuthFailed` 只命中定义处），因为 Dart 的重连路径
     * `_connectInternal` 每次都重新换票（client:151）—— 行为上等价。
     * KMP 侧传输层能拿到 close code（`websocket_transport.dart` 的 `onDone` 拿不到），
     * 所以这里把语义显式化：命中三码 ⇒ 丢弃旧票 + 立即重新换票。
     */
    public fun requiresNewTicket(code: Int): Boolean =
        code == WS_CLOSE_AUTH_FAILED || code == WS_CLOSE_KICKED || code == WS_CLOSE_TICKET_EXPIRED

    /** 关闭码是否属于「鉴权类」（4001 / 4003）：与 4002 互踢区分开，供上层决定是否停重连。 */
    public fun isAuthCloseCode(code: Int): Boolean =
        code == WS_CLOSE_AUTH_FAILED || code == WS_CLOSE_TICKET_EXPIRED

    /** 正常关闭（Dart `close(code ?? 1000)`，websocket_transport.dart:55）。 */
    public const val WS_CLOSE_NORMAL: Int = 1000

    /** 未收到关闭码时的占位（RFC6455 1005 No Status Received）。 */
    public const val WS_CLOSE_NO_STATUS: Int = 1005
}

/**
 * Topic 命名约定 —— 与 Go `entity.Topic*` 字符串必须完全一致
 * （Dart `realtime_config.dart:53-61`）。
 */
public object RealtimeTopics {
    public const val SYS_NOTIFY: String = "sys.notify"
    public const val PRESENCE_BULK: String = "presence.bulk"

    public fun presenceUser(userId: String): String = "presence.user.$userId"

    public fun liveSignal(roomId: String): String = "live.signal.$roomId"

    public fun liveRoomState(roomId: String): String = "live.room.$roomId.state"
}

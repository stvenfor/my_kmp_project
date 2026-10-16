package com.example.my_kmp_project.component.realtime

import com.example.my_kmp_project.core.network.realtime.RealtimeEnvelope
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonObject

/**
 * 全局单连接 Realtime 客户端契约 ——
 * 逐条对齐 Dart `commons/core/lib/service/app_realtime_client.dart:5-39`（d7876a1）。
 *
 * Dart 用 `Stream`，这里统一换成 `kotlinx.coroutines.flow.Flow`（本仓库既有做法）；
 * 语义仍是「多播、不重放」——契约实现见 [RealtimeInboundRouter] /
 * [com.example.my_kmp_project.core.network.realtime.RealtimeWsTransport]。
 *
 * 相对 Dart 的刻意扩展只有 [prepare]（Dart 是 impl 上的公开方法、由
 * `RealtimeInitializer.initDeferred` 调，:33；这里提进接口以便装配层不依赖实现类）。
 */
public interface AppRealtimeClient {

    /** Dart `Stream<RealtimeConnectionState> get connectionState`（:6）。 */
    public val connectionState: Flow<RealtimeConnectionState>

    /** Dart `RealtimeConnectionState get currentState`（:8）。 */
    public val currentState: RealtimeConnectionState

    /** Dart `int get lastSeq`（:10）：来自 [RealtimeSeqStore]。 */
    public val lastSeq: Long

    /** Dart `int get reconnectCount`（:12）。 */
    public val reconnectCount: Int

    /** Dart `int get outboundQueueDepth`（:14）。 */
    public val outboundQueueDepth: Int

    /** Dart `bool get keepAliveInBackground`（:16）。 */
    public val keepAliveInBackground: Boolean

    /**
     * Dart 里这是 impl 的公开方法（client:105-110），由 `RealtimeInitializer.initDeferred` 调（:33）；
     * 这里提进接口，装配层就不必依赖实现类。语义：读 seq 水位 + 用真实行数同步出站深度。
     */
    public suspend fun prepare()

    /** Dart `connect()`（:18）：未登录 / 已连接时静默跳过。 */
    public suspend fun connect()

    /** Dart `disconnect({reason, clearQueue})`（:20）。 */
    public suspend fun disconnect(reason: String? = null, clearQueue: Boolean = false)

    /** Dart `subscribeTopics(List<String>)`（:22）。 */
    public suspend fun subscribeTopics(topics: List<String>)

    /** Dart `unsubscribeTopics(List<String>)`（:24）。 */
    public suspend fun unsubscribeTopics(topics: List<String>)

    /** Dart `watchTopic(topic)`（:26）。 */
    public fun watchTopic(topic: String): Flow<RealtimeEnvelope>

    /** Dart `watchEvents({String? eventName})`（:28）。 */
    public fun watchEvents(eventName: String? = null): Flow<RealtimeEnvelope>

    /**
     * Dart `sendEvent({topic, eventName, payload, requireAck = true})`（:30-35）。
     * [payloadJson] 为出站帧 `payload` 里除 `name` 之外的字段（Dart 传 `Map<String, dynamic>`）。
     */
    public suspend fun sendEvent(
        topic: String,
        eventName: String,
        payload: JsonObject? = null,
        requireAck: Boolean = true,
    )

    /** Dart `setKeepAliveInBackground(bool)`（:37-38）：直播页等场景 paused 仍保持 WS。 */
    public fun setKeepAliveInBackground(enabled: Boolean)
}

/**
 * 会话缝隙：Dart 在这里读的是全局单例（`AuthLifecycle.isLoggedIn` +
 * `Get.find<UserService>().currentUser.value?.token`，client:114、:520-529）。
 * 组件层不能反向依赖 feature，所以由装配层注入实现。
 *
 * 契约（与 Dart 一致）：
 * - [isLoggedIn] 只回答「有没有登录态」；
 * - [accessToken] 无 token 时**抛** [IllegalStateException]（Dart `StateError('未登录或 token 为空…')`），
 *   该异常会被 [DefaultRealtimeAuthHardFailurePolicy] 认成鉴权硬失败 ⇒ 停止重连。
 */
public interface RealtimeSessionPort {
    public fun isLoggedIn(): Boolean

    public fun accessToken(): String
}

/**
 * 会话守护缝隙（Dart `SessionGuardService`，client:185-198）：
 * 互踢 / 强制下线时先尝试同设备恢复，恢复不了就停重连并交给它处理弹窗。
 */
public interface RealtimeSessionGuard {
    /** Dart `isForceLogoutError(e)`（:188）。 */
    public fun isForceLogoutError(error: Throwable): Boolean

    /** Dart `tryRecover()`（:189、:202）：同设备静默恢复会话，成功返回 true。 */
    public suspend fun tryRecover(): Boolean

    /** Dart `handleIfForceLogout(e)`（:196）：仅在确认互踢时弹提示 / 清会话。 */
    public suspend fun handleIfForceLogout(error: Throwable)
}

/**
 * `sys.notify` 落地口（Dart `GlobalNotifyHandler.handle`，client:265、:286）。
 *
 * 通知横幅属 **UI wave**（`ui/realtime_notify_banner_*.dart` 本 wave 不搬），
 * 所以这里只留接缝：实现方负责 notifyId 去重（用 [NotifyDedupStore]）与展示。
 */
public fun interface RealtimeNotifyHandler {
    public suspend fun handle(envelope: RealtimeEnvelope)
}

/**
 * 网络可达性缝隙（Dart `Connectivity().onConnectivityChanged`，client:356-363）：
 * 恢复联网且当前不活跃时自动补一次 `connect()`。
 *
 * // GAP(R5): 本仓库没有跨端 connectivity 插件（Dart 用 `connectivity_plus`），
 * // 因此只提供接口与 [NoopRealtimeNetworkMonitor]；平台侧接好后由装配层注入实现。
 */
public interface RealtimeNetworkMonitor {
    /** 注册监听；实现应在「有网 ⇔ offline→online」时回调 [onOnline]。 */
    public fun start(onOnline: () -> Unit)

    public fun stop()
}

/** 默认实现：什么都不做（不假装已监听）。 */
public object NoopRealtimeNetworkMonitor : RealtimeNetworkMonitor {
    override fun start(onOnline: () -> Unit) = Unit
    override fun stop() = Unit
}

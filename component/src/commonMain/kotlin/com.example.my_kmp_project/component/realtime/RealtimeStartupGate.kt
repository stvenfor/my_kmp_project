package com.example.my_kmp_project.component.realtime

import com.example.my_kmp_project.core.log.AppLog
import com.example.my_kmp_project.core.network.realtime.RealtimeConfig
import com.example.my_kmp_project.core.network.realtime.RealtimeTopics
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Realtime 启动闸门 —— 逻辑对齐 Dart
 * `components/realtime/lib/realtime_initializer.dart:19-180`（d7876a1）。
 *
 * 搬的是**决策逻辑**，不搬 GetX 装配：
 * Dart 的 `initDeferred()`（:28-59）做的是「绑定依赖 + 挂四个回调链 + 非阻塞首连」，
 * 回调链（`PrivacyConsentService.onGranted` / `AuthLifecycle.onAfterLogin` /
 * `onAfterLogout` / `AppLifecycleListener` / `EnvironmentService.onEnvChanged`）
 * 属 `app/` 装配 wave，这里只把它们各自对应的入口暴露成方法：
 * [onPrivacyGranted] / [onAfterLogin] / [onAfterLogout] / [onEnvChanged] / [onAppLifecycle]。
 *
 * 复刻的关键语义（[tryConnectIfReady]，:91-146）：
 * 1. **隐私未同意 ⇒ 直接不连**（:103-106），先于登录检查；
 * 2. 未登录 ⇒ 不连（:107-110）；
 * 3. 已经活跃 ⇒ 不重复连（:117-122）；
 * 4. 首连**带 12s 超时**且超时只记日志、不打断后续（:126-129）——
 *    注释写明「不得阻塞 runApp，真机会一直停在原生启动图」（:57）；
 * 5. 连完**固定订阅** `sys.notify` + `presence.bulk`（:130）；
 * 6. 再等 8s 看是否真的连上，只用于打成功/失败日志（:132-145），不影响状态机。
 *
 * 时间：Dart 用 `Duration` + `Future.timeout`，这里统一走注入的 [RealtimeTicker]，
 * 因此测试用手写虚拟时钟即可确定化（本仓库 test 依赖里没有 kotlinx-coroutines-test）。
 */
public class RealtimeStartupGate(
    private val client: AppRealtimeClient,
    private val session: RealtimeSessionPort,
    private val scope: CoroutineScope,
    private val privacyGranted: () -> Boolean,
    private val ticker: RealtimeTicker = CoroutineRealtimeTicker(scope),
    private val defaultTopics: List<String> = listOf(RealtimeTopics.SYS_NOTIFY, RealtimeTopics.PRESENCE_BULK),
    private val connectTimeoutMs: Long = CONNECT_TIMEOUT_MS,
    private val connectedWaitMs: Long = CONNECTED_WAIT_MS,
    private val log: (String) -> Unit = { AppLog.d("RealtimeStartupGate", it) },
) {
    private var clientImpl: AppRealtimeClientImpl? = null

    /**
     * Dart `initDeferred()` 里的 `impl.prepare()` + `NotifyDedupStore.load()`（:33-34）。
     * dedup 表由装配层自己 load（它不在 client 里），这里只负责 `prepare()`。
     */
    public suspend fun prepare() {
        (client as? AppRealtimeClientImpl)?.let { clientImpl = it }
        client.prepare()
    }

    /**
     * Dart `tryConnectIfReady({trigger})`（:91-146）。
     * [trigger] 只做日志用（`app_startup` / `privacy_granted` / `after_login` / `env_changed` / `manual`）。
     */
    public suspend fun tryConnectIfReady(trigger: String = "manual") {
        val privacy = privacyGranted()
        val loggedIn = session.isLoggedIn()
        val currentState = client.currentState
        log(
            "[Realtime] tryConnect trigger=$trigger privacy=$privacy " +
                "loggedIn=$loggedIn state=${currentState.label}",
        )

        if (!privacy) {
            log("[Realtime] skip connect: privacy not granted (trigger=$trigger)")
            return
        }
        if (!loggedIn) {
            log("[Realtime] skip connect: not logged in (trigger=$trigger)")
            return
        }

        if (currentState.isActive) {
            log("[Realtime] skip connect: already ${currentState.label} (trigger=$trigger)")
            return
        }

        log("[Realtime] connecting... trigger=$trigger mock=${RealtimeConfig.useMockGateway}")
        val finishedInTime = awaitWithTimeout(connectTimeoutMs) { client.connect() }
        if (!finishedInTime) log("[Realtime] connect timed out trigger=$trigger")

        client.subscribeTopics(defaultTopics)

        val connected = waitUntilConnected(connectedWaitMs)
        log(
            "[Realtime] connect result trigger=$trigger success=$connected " +
                "state=${client.currentState.label} lastSeq=${client.lastSeq} " +
                "topics=${defaultTopics.joinToString(",")}",
        )
        if (!connected) {
            log(
                "[Realtime] connect not ready within timeout trigger=$trigger " +
                    "state=${client.currentState.label} — check backend / token / network",
            )
        }
    }

    /** Dart `_chainPrivacyGranted`（:61-68）：同意隐私后补一次首连。 */
    public suspend fun onPrivacyGranted() {
        tryConnectIfReady(trigger = "privacy_granted")
    }

    /** Dart `_chainAfterLogin`（:70-77）。 */
    public suspend fun onAfterLogin() {
        // 登录会让「手动断开」位失效：Dart 靠 connect() 重置 _manualDisconnect（client:126）。
        tryConnectIfReady(trigger = "after_login")
    }

    /** Dart `_chainAfterLogout` → `onLogout()`（:79-86、:166-168）：登出清连接，但**不清队列**。 */
    public suspend fun onAfterLogout() {
        client.disconnect(reason = REASON_LOGOUT, clearQueue = false)
    }

    /**
     * Dart `_onEnvChanged`（:170-173）：切环境先断开再重连。
     * 注意这里 `disconnect` 用的是**默认** `clearQueue = false`。
     */
    public suspend fun onEnvChanged() {
        client.disconnect(reason = REASON_ENV_CHANGED)
        tryConnectIfReady(trigger = REASON_ENV_CHANGED)
    }

    /**
     * Dart `AppLifecycleListener(onStateChange: impl.onAppLifecycle)`（:44-46）：
     * 装配层把平台生命周期事件转成 [RealtimeAppLifecycle] 交进来即可。
     */
    public fun onAppLifecycle(state: RealtimeAppLifecycle) {
        clientImpl?.onAppLifecycle(state) ?: run {
            // 非 impl（理论上不会发生）：只有 resumed 才尝试补连，语义与 Dart 一致。
            if (state == RealtimeAppLifecycle.Resumed && !client.currentState.isActive) {
                scope.launch { client.connect() }
            }
        }
    }

    /** Dart `dispose()`（:175-179）：摘监听 + 释放 client。 */
    public suspend fun dispose() {
        (client as? AppRealtimeClientImpl)?.dispose()
        clientImpl = null
    }

    /** 观测口：`prepare()` 之后是否拿到了具体实现句柄（调试用）。 */
    public val hasLifecycleCapableClient: Boolean get() = clientImpl != null

    /**
     * Dart `connect().timeout(12s)`：超时**不取消**底层任务（Dart 的 `Future.timeout` 会抛出但
     * 原 future 继续跑），所以这里用 [RealtimeTicker] 只做「等待上限」，工作继续在 scope 里跑。
     */
    private suspend fun awaitWithTimeout(timeoutMs: Long, work: suspend () -> Unit): Boolean {
        val done = CompletableDeferred<Boolean>()
        val timer = ticker.after(timeoutMs) { done.complete(false) }
        scope.launch {
            runCatching { work() }
            done.complete(true)
        }
        val result = done.await()
        timer.cancel()
        return result
    }

    /** Dart `_waitUntilConnected`（:148-164）：命中活跃或 failed 即结束等待。 */
    private suspend fun waitUntilConnected(timeoutMs: Long): Boolean {
        if (client.currentState.isActive) return true
        val winner = CompletableDeferred<Boolean>()
        val timer = ticker.after(timeoutMs) { winner.complete(false) }
        val watcher: Job = scope.launch {
            client.connectionState.collect { state ->
                if (state.isActive || state == RealtimeConnectionState.Failed) {
                    winner.complete(state.isActive)
                }
            }
        }
        val connected = winner.await()
        timer.cancel()
        watcher.cancel()
        return connected
    }

    private companion object {
        /** Dart `Duration(seconds: 12)`（:126）。 */
        const val CONNECT_TIMEOUT_MS: Long = 12_000L

        /** Dart `Duration(seconds: 8)`（:133）。 */
        const val CONNECTED_WAIT_MS: Long = 8_000L

        const val REASON_LOGOUT: String = "logout"
        const val REASON_ENV_CHANGED: String = "env_changed"
    }
}

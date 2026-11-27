package com.example.my_kmp_project.app

import com.example.my_kmp_project.component.di.RealtimeGraph
import com.example.my_kmp_project.component.realtime.RealtimeAppLifecycle
import com.example.my_kmp_project.component.realtime.RealtimeStartupGate
import kotlin.concurrent.Volatile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Realtime 启动编排 —— 对齐 Dart `RealtimeInitializer.initDeferred()`
 * （components/realtime/lib/realtime_initializer.dart:28-59，d7876a1）。
 *
 * DI 第三批把「客户端 + 闸门 + 存储/会话绑定」搬进 AppGraph（见
 * [com.example.my_kmp_project.app.di.CoreModule]）；本对象只剩「何时首连 / 何时补连」：
 * - 首连 app_startup：[AppBootstrap.start] 调 [install]，prepare 与首连都丢进 [scope]
 *   后台协程，install 本身只记录闸门、不 await ⇒ 不阻塞冷启动。
 *   隐私未同意 / 未登录由闸门自行跳过（见 RealtimeStartupGate）。
 * - onPrivacyGranted / onAfterLogin / onAfterLogout：AppShell 三个交互点调下面的方法。
 * - onEnvChanged / onAppLifecycle：触发源在平台 source set 与 feature/mine 环境切换 UI，
 *   属书面 GAP，不塞伪实现。
 */
internal object RealtimeRuntime {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    private var installed = false

    @Volatile
    private var gate: RealtimeStartupGate? = null

    /** 由 AppGraph 提供完整绑定；本批签名从无参改为显式注入。 */
    fun install(realtime: RealtimeGraph) {
        if (installed) return
        installed = true
        gate = realtime.startupGate

        // 非阻塞首连：install() 到此为止只记录对象；prepare 与「12s 超时 + 8s 等待」的首连
        // 全部在后台 scope 里跑，不 await、不阻塞 runApp，也就不拖慢冷启动或停在原生启动图。
        // 隐私未同意 / 未登录时 tryConnectIfReady 直接跳过，不会拨号。
        scope.launch {
            realtime.startupGate.prepare()
            realtime.startupGate.tryConnectIfReady(trigger = "app_startup")
        }
    }

    /** 隐私同意后补一次首连（AppShell 隐私页 onAccept）。 */
    fun onPrivacyGranted() {
        gate?.let { g -> scope.launch { g.onPrivacyGranted() } }
    }

    /** 登录成功后补连（AppShell afterAuthSuccess）。 */
    fun onAfterLogin() {
        gate?.let { g -> scope.launch { g.onAfterLogin() } }
    }

    /** 登出清连接、不清出站队列（AppShell onLogoutClick）。 */
    fun onAfterLogout() {
        gate?.let { g -> scope.launch { g.onAfterLogout() } }
    }

    /**
     * 切环境：断开再重连。GAP：环境切换在 feature/mine MineSettingsScreen 写
     * NetworkConfig，未暴露 commonMain 可见的变更事件；后续 wave 在切换处转发本方法。
     */
    fun onEnvChanged() {
        gate?.let { g -> scope.launch { g.onEnvChanged() } }
    }

    /**
     * 平台生命周期：GAP：resumed / paused 事件在平台 source set
     * （MainActivity.onResume/onPause、iOS scene phase、OHOS ability 生命周期）里，
     * 不在本 wave 白名单；后续 wave 转发 RealtimeAppLifecycle 进来。
     */
    fun onAppLifecycle(state: RealtimeAppLifecycle) {
        gate?.onAppLifecycle(state)
    }
}

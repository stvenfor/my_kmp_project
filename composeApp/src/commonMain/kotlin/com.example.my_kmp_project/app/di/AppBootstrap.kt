package com.example.my_kmp_project.app.di

import com.example.my_kmp_project.app.AppCrossFeatureScreens
import com.example.my_kmp_project.app.RealtimeRuntime
import com.example.my_kmp_project.component.auth.AuthSessionState
import com.example.my_kmp_project.component.di.AppGraph
import com.example.my_kmp_project.component.di.SessionGraph
import com.example.my_kmp_project.component.routing.CrossFeatureScreens
import com.example.my_kmp_project.feature.auth.AuthSessionBootstrap
import kotlin.concurrent.Volatile

/**
 * 应用启动器（DI 第三批）：吸收原 `ShellBootstrap.install()` 的全部职责。
 *
 * 生命周期：
 * - [start]：建图 + 安装 LegacyGraph + 按**原顺序**一次性接线
 *   （会话水合/401 刷新 → 跨功能屏幕端口 → realtime 非阻塞首连 → 已登录则建会话作用域）；
 * - [sessionReady]：登录 / 冷启动恢复成功后挂载 SessionGraph，幂等（同一登录期复用同一个实例）；
 * - [logout]：摘除 SessionGraph 并清本地响应式状态，幂等。
 *
 * 入口的 `ShellBootstrap.install()` 只是 [start] 的薄委托，三端壳不需要改动。
 */
internal object AppBootstrap {

    @Volatile
    private var graph: AppGraph? = null

    @Volatile
    private var wired: Boolean = false

    fun start(): AppGraph = start(wire = true)

    fun sessionReady(): SessionGraph = attachSessionIfNeeded(ensureGraph())

    fun logout() {
        graph?.detachSession()
        AuthSessionState.clearLocal()
    }

    /**
     * 测试隔离：重建图、重置接线标记（生产入口不需要调用）。
     * 放在这里是因为单测要在同一 JVM 里重复验证 start / sessionReady / logout。
     */
    internal fun resetForTests() {
        graph = null
        wired = false
        LegacyGraph.reset()
    }

    /** 测试用启动：只建图、安装 LegacyGraph，不触发 store/网络/realtime 接线。 */
    internal fun startForTests(create: () -> AppGraph = { AppGraphImpl() }): AppGraph =
        start(wire = false, create = create)

    private fun start(wire: Boolean, create: () -> AppGraph = { AppGraphImpl() }): AppGraph {
        val current = graph ?: create().also { graph = it }
        LegacyGraph.install(current)
        if (wire) wireOnce(current)
        return current
    }

    /** 幂等接线：顺序与原 ShellBootstrap 完全一致。 */
    private fun wireOnce(current: AppGraph) {
        if (wired) return
        wired = true

        // 1) 会话水合 + 401 刷新独占（feature/auth）；依赖从 CoreModule 注入。
        AuthSessionBootstrap.install(
            session = CoreModule.accountRepository(),
            client = current.network.api(),
        )
        // 2) 跨功能屏幕端口（app 装配实现）。
        CrossFeatureScreens.install(AppCrossFeatureScreens)
        // 3) realtime 首连闸门：只把首连丢进后台协程，不阻塞冷启动。
        RealtimeRuntime.install(current.realtime)
        // 4) 冷启动已恢复登录态：直接建立会话作用域。
        if (AuthSessionState.isLoggedIn) attachSessionIfNeeded(current)
    }

    private fun ensureGraph(): AppGraph = graph ?: start()

    private fun attachSessionIfNeeded(current: AppGraph): SessionGraph =
        current.sessionOrNull ?: SessionModule.create().also { current.attachSession(it) }
}

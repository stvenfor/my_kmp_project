package com.example.my_kmp_project.app

import com.example.my_kmp_project.component.routing.CrossFeatureScreens
import com.example.my_kmp_project.feature.auth.AuthSessionBootstrap
import kotlin.concurrent.Volatile

/**
 * Assembly bootstrap for every shell entrypoint (precedent: [AuthSessionBootstrap.install],
 * which this wraps so ordering stays idempotent and single-sourced):
 * session hydration + 401 refresh wiring + cross-feature screen port installation
 * + realtime 启动闸门装配（[RealtimeRuntime.install]，非阻塞首连）。
 *
 * Called from `AppShell`, `MainActivity`, iOS `MainViewController` factories and
 * `MainArkUIViewController` before any shared Compose UI renders.
 */
internal object ShellBootstrap {

    @Volatile
    private var installed = false

    fun install() {
        if (installed) return
        installed = true
        AuthSessionBootstrap.install()
        CrossFeatureScreens.install(AppCrossFeatureScreens)
        // 放在会话水合 / 401 刷新之后：换票与补投 API 捕获的是最终装饰后的 NetworkFacade.api()。
        // install() 只构造对象并把首连丢进后台协程，不阻塞任何入口的冷启动。
        RealtimeRuntime.install()
    }
}

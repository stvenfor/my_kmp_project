package com.example.my_kmp_project.feature.auth

import com.example.my_kmp_project.component.auth.AuthSessionState
import com.example.my_kmp_project.core.network.NetworkFacade

/**
 * 会话 + 刷新装机入口（幂等，三端原生壳与 legacy shell 启动时调用）。
 *
 * 注册后 [com.example.my_kmp_project.core.network.NetworkFacade] 的 401 流由
 * [AuthTokenRefresh] 独占：先静默刷新再重试；刷新耗尽/互踢才清会话回登录
 * （Flutter d7876a1 `AuthTokenRefreshInterceptor` 语义）。
 */
internal object AuthSessionBootstrap {

    @kotlin.concurrent.Volatile
    private var installed = false

    fun install() {
        if (installed) return
        installed = true
        AuthSessionState.sync()
        NetworkFacade.setAuthRefreshSupport(
            policy = AuthTokenRefresh.policy,
            refresher = AuthTokenRefresh.handler,
            onSessionCleared = { AuthTokenRefresh.handleRefreshExhausted() },
        )
    }
}

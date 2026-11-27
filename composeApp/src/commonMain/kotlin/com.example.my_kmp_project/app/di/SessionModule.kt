package com.example.my_kmp_project.app.di

import com.example.my_kmp_project.component.di.SessionGraph
import com.example.my_kmp_project.core.account.SessionRepository
import com.example.my_kmp_project.core.account.SessionState

/**
 * 会话作用域绑定（DI 第三批）。
 *
 * 登录/恢复成功时由 [AppBootstrap.sessionReady] 建一次并挂到 AppGraph；
 * 登出时 [AppBootstrap.logout] 摘除。仓库与响应式状态都复用过渡期单例的接口视图，
 * 因此 UI 看到的仍是同一份 `AuthSessionState.snapshot`。
 */
internal object SessionModule {

    fun create(
        repository: SessionRepository = CoreModule.accountRepository(),
        state: SessionState = CoreModule.sessionState(),
    ): SessionGraph = DefaultSessionGraph(repository = repository, state = state)
}

internal data class DefaultSessionGraph(
    override val repository: SessionRepository,
    override val state: SessionState,
) : SessionGraph

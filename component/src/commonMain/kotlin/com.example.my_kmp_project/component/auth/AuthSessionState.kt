package com.example.my_kmp_project.component.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.my_kmp_project.core.account.AccountFacade
import com.example.my_kmp_project.core.account.DefaultSessionState
import com.example.my_kmp_project.core.account.SessionState
import kotlinx.coroutines.flow.StateFlow

/**
 * Sole reactive session source of truth for UI soft-gates (ADR 0001).
 *
 * DI 过渡（第二批）：[snapshot] / [sync] / [clearLocal] 委托给
 * [DefaultSessionState]（core 层纯 StateFlow 实现）；Compose 镜像字段
 * [isLoggedIn] / [displayName] 在每次写入后同步，旧调用方行为不变。
 *
 * 新代码请注入 `SessionGraph.state` 或收集 [snapshot]；
 * Compose 字段仅为 Spike I 期间的存量调用保留。
 */
public object AuthSessionState {
    private val delegate: SessionState = DefaultSessionState(AccountFacade)

    val snapshot: StateFlow<SessionState.Snapshot> get() = delegate.snapshot

    var isLoggedIn by mutableStateOf(false)
        private set

    var displayName by mutableStateOf<String?>(null)
        private set

    /** Pull latest values from [AccountFacade] (hydrates store on first access). */
    fun sync() {
        delegate.sync()
        mirror(delegate.snapshot.value)
    }

    fun clearLocal() {
        delegate.clearLocal()
        mirror(delegate.snapshot.value)
    }

    /** DI：注入用的会话状态视图，与单例共享同一份 StateFlow。 */
    fun state(): SessionState = delegate

    private fun mirror(value: SessionState.Snapshot) {
        isLoggedIn = value.isLoggedIn
        displayName = value.displayName
    }
}

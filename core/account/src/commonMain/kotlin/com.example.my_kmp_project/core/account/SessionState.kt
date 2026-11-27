package com.example.my_kmp_project.core.account

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 会话的响应式只读视图（ADR 0001，DI 第一批）。
 *
 * UI 软门槛只订阅 [snapshot]；写入一律走 [SessionRepository]，然后调用 [sync] 发布。
 * 这里只有 StateFlow，没有 Compose 依赖 —— Compose 镜像字段留在
 * `component.auth.AuthSessionState` 过渡层，避免 core 反向依赖 compose。
 */
public interface SessionState {
    public val snapshot: StateFlow<Snapshot>

    /** 从仓库拉取最新会话并发布（首次调用触发本地水合）。 */
    public fun sync()

    /** 本地清空（登出 / 401），不触碰仓库。 */
    public fun clearLocal()

    public data class Snapshot(
        public val isLoggedIn: Boolean = false,
        public val displayName: String? = null,
    )
}

/** 纯 StateFlow 的默认实现：可在 core 层与测试中直接使用。 */
public class DefaultSessionState(
    private val repository: SessionRepository,
) : SessionState {
    private val _snapshot = MutableStateFlow(SessionState.Snapshot())

    override val snapshot: StateFlow<SessionState.Snapshot> = _snapshot.asStateFlow()

    override fun sync() {
        val session = repository.current()
        publish(isLoggedIn = session.isLoggedIn, displayName = session.displayName)
    }

    override fun clearLocal() {
        publish(isLoggedIn = false, displayName = null)
    }

    private fun publish(isLoggedIn: Boolean, displayName: String?) {
        _snapshot.value = SessionState.Snapshot(
            isLoggedIn = isLoggedIn,
            displayName = displayName,
        )
    }
}

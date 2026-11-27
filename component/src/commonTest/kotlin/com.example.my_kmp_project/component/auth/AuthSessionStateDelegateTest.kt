package com.example.my_kmp_project.component.auth

import com.example.my_kmp_project.core.account.SessionState
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * DI 第二批：AuthSessionState 是 DefaultSessionState 的 Compose 过渡壳。
 * 注入视图与单例必须共享同一份 StateFlow；镜像字段在写入后保持一致。
 */
class AuthSessionStateDelegateTest {

    @Test
    fun stateViewIsStableAndSharesTheSnapshotFlow() {
        assertSame(AuthSessionState.state(), AuthSessionState.state())
        assertSame(AuthSessionState.snapshot, AuthSessionState.state().snapshot)
    }

    @Test
    fun clearLocalUpdatesBothTheFlowAndTheComposeMirror() {
        AuthSessionState.clearLocal()

        val snapshot: SessionState.Snapshot = AuthSessionState.snapshot.value
        assertFalse(snapshot.isLoggedIn)
        assertFalse(AuthSessionState.isLoggedIn)
        assertTrue(snapshot.displayName == null)
        assertTrue(AuthSessionState.displayName == null)
    }
}

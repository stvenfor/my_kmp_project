package com.example.my_kmp_project.app.di

import com.example.my_kmp_project.component.auth.AuthSessionState
import com.example.my_kmp_project.core.account.AccountFacade
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * DI 第三批：Session 作用域生命周期。
 *
 * - [AppBootstrap.startForTests] 只建图（不碰 store / 网络 / realtime），
 *   保证这些用例在纯 JVM 单测里可重复；
 * - 登录/恢复 → `sessionReady()` 挂载且幂等；
 * - 登出 → `logout()` 摘除且幂等，下一次登录拿到新的 SessionGraph，不串会话。
 */
class SessionScopeLifecycleTest {

    @BeforeTest
    fun resetBootstrap() {
        AppBootstrap.resetForTests()
    }

    @Test
    fun startIsIdempotentAndInstallsLegacyGraph() {
        val first = AppBootstrap.startForTests()
        val second = AppBootstrap.startForTests()

        assertSame(first, second)
        assertSame(first, LegacyGraph.require())
    }

    @Test
    fun sessionReadyAttachesExactlyOneScopeAndReusesIt() {
        val graph = AppBootstrap.startForTests()
        assertNull(graph.sessionOrNull)

        val session = AppBootstrap.sessionReady()

        assertSame(session, graph.sessionOrNull)
        assertSame(session, AppBootstrap.sessionReady(), "重复调用必须复用同一会话作用域")
        // 仓库与响应式状态复用过渡期单例的同一实例：UI 软门槛看到的是同一份 snapshot。
        assertSame(AccountFacade.repository(), session.repository)
        assertSame(AuthSessionState.state(), session.state)
    }

    @Test
    fun logoutDetachesAndTheNextLoginGetsAFreshScope() {
        AppBootstrap.startForTests()
        val first = AppBootstrap.sessionReady()

        AppBootstrap.logout()

        assertNull(LegacyGraph.require().sessionOrNull)
        assertFalse(AuthSessionState.isLoggedIn)

        val second = AppBootstrap.sessionReady()
        assertNotSame(first, second, "登出后重登必须建立新的会话作用域")
        assertSame(second, LegacyGraph.require().sessionOrNull)
    }

    @Test
    fun logoutWithoutSessionIsIdempotent() {
        val graph = AppBootstrap.startForTests()

        AppBootstrap.logout()
        AppBootstrap.logout()

        assertNull(graph.sessionOrNull)
    }
}

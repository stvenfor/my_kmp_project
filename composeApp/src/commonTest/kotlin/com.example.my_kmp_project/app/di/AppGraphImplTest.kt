package com.example.my_kmp_project.app.di

import com.example.my_kmp_project.component.di.AppGraph
import com.example.my_kmp_project.component.di.SessionGraph
import com.example.my_kmp_project.core.account.AccountSession
import com.example.my_kmp_project.core.account.DefaultSessionState
import com.example.my_kmp_project.core.account.LoggedOut
import com.example.my_kmp_project.core.account.SessionRepository
import com.example.my_kmp_project.core.account.SessionState
import com.example.my_kmp_project.core.network.ApiClient
import com.example.my_kmp_project.core.network.ApiClientProvider
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** DI 第二批：应用图骨架的装配语义（不接线，仅新代码入口）。 */
class AppGraphImplTest {

    @Test
    fun sessionIsAbsentUntilAttached() {
        val graph = AppGraphImpl(network = UnusedNetwork)

        assertNull(graph.sessionOrNull)
        assertFailsWith<IllegalStateException> { graph.requireSession() }
    }

    @Test
    fun attachAndDetachManageTheSessionScope() {
        val graph = AppGraphImpl(network = UnusedNetwork)
        val session = FakeSessionGraph()

        graph.attachSession(session)
        assertSame(session, graph.sessionOrNull)
        assertSame(session, graph.requireSession())

        graph.detachSession()
        assertNull(graph.sessionOrNull)
        assertFailsWith<IllegalStateException> { graph.requireSession() }
    }
}

class LegacyGraphTest {

    @BeforeTest
    fun resetHolder() {
        LegacyGraph.reset()
    }

    @Test
    fun requireBeforeInstallFailsWithAReadableMessage() {
        val error = assertFailsWith<IllegalStateException> { LegacyGraph.require() }
        assertTrue(error.message.orEmpty().contains("AppBootstrap.start()"))
    }

    @Test
    fun sessionOrThrowBeforeInstallFails() {
        assertFailsWith<IllegalStateException> { LegacyGraph.sessionOrThrow() }
    }

    @Test
    fun installThenRequireReturnsTheSameGraph() {
        val graph: AppGraph = AppGraphImpl(network = UnusedNetwork)

        LegacyGraph.install(graph)

        assertSame(graph, LegacyGraph.require())
    }
}

class AppBootstrapTest {

    @Test
    fun startIsIdempotentAndKeepsLegacyGraphInSync() {
        // startForTests：不触发 store / 网络 / realtime 接线，纯 JVM 单测可跑。
        val first = AppBootstrap.startForTests()
        val second = AppBootstrap.startForTests()

        assertSame(first, second)
        assertSame(first, LegacyGraph.require())
    }
}

/** 只用于构造 AppGraphImpl；测试不触发网络请求。 */
private object UnusedNetwork : ApiClientProvider {
    override fun api(): ApiClient = throw IllegalStateException("network not used in this test")

    override fun rawApi(): ApiClient = throw IllegalStateException("network not used in this test")
}

private object EmptyRepository : SessionRepository {
    override fun current(): AccountSession = LoggedOut

    override fun setSession(value: AccountSession) = Unit

    override fun logout() = Unit

    override fun updateAuthTokens(token: String, refreshToken: String, sessionId: String) = Unit

    override fun updateProfile(displayName: String?, avatarUrl: String?) = Unit

    override fun updateProfilePhone(phone: String) = Unit

    override fun updateProfileEmail(email: String) = Unit

    override fun updateCompanyOrg(companyId: Long?, orgId: Long?) = Unit
}

private class FakeSessionGraph : SessionGraph {
    override val repository: SessionRepository = EmptyRepository
    override val state: SessionState = DefaultSessionState(repository)
}

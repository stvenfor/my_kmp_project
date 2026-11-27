package com.example.my_kmp_project.feature.auth

import com.example.my_kmp_project.component.auth.AuthSessionPayload
import com.example.my_kmp_project.component.auth.DefaultLoginStrategyRegistry
import com.example.my_kmp_project.component.auth.LoginAvailability
import com.example.my_kmp_project.component.auth.LoginCredentials
import com.example.my_kmp_project.component.auth.LoginError
import com.example.my_kmp_project.component.auth.LoginOutcome
import com.example.my_kmp_project.component.auth.LoginStrategy
import com.example.my_kmp_project.core.account.AccountSession
import com.example.my_kmp_project.core.account.DefaultSessionState
import com.example.my_kmp_project.core.account.LoggedOut
import com.example.my_kmp_project.core.account.SessionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 策略模式第 1 批：编排器只做「找策略 → 执行 → 唯一落库 / 清会话」。
 */
class LoginOrchestratorTest {

    @Test
    fun success_establishes_session_once_with_fallbacks() = runBlocking {
        val repository = FakeSessionRepository()
        val state = DefaultSessionState(repository)
        var cleared = 0
        val orchestrator = orchestrator(
            repository = repository,
            state = state,
            strategy = FakeStrategy(
                id = "fake",
                outcome = LoginOutcome.Success(
                    AuthSessionPayload(
                        token = "token",
                        userId = "",
                        displayName = "",
                        email = "alice@example.com",
                        deviceId = "device-1",
                    ),
                ),
            ),
            onSessionCleared = { cleared++ },
        )

        val outcome = orchestrator.login("fake", LoginCredentials.Password("a", "b"))

        assertTrue(outcome is LoginOutcome.Success)
        assertEquals(1, repository.setCount)
        assertEquals("token", repository.session.token)
        assertEquals("alice", repository.session.displayName)
        assertEquals("alice", repository.session.userId)
        assertEquals("device-1", repository.session.deviceId)
        assertTrue(state.snapshot.value.isLoggedIn)
        assertEquals(0, cleared)
    }

    @Test
    fun unknown_method_returns_unavailable_without_touching_session() = runBlocking {
        val repository = FakeSessionRepository()
        val orchestrator = orchestrator(repository = repository, state = DefaultSessionState(repository))

        val outcome = orchestrator.login("qq", LoginCredentials.Interactive)

        val error = (outcome as LoginOutcome.Failure).error
        assertTrue(error is LoginError.Unavailable)
        assertEquals("qq", error.methodId)
        assertEquals(0, repository.setCount)
    }

    @Test
    fun unavailable_strategy_is_not_executed() = runBlocking {
        val repository = FakeSessionRepository()
        val strategy = FakeStrategy(
            id = "wechat",
            availability = LoginAvailability.Unavailable("未接入微信 SDK"),
        )
        val orchestrator = orchestrator(repository = repository, state = DefaultSessionState(repository), strategy = strategy)

        val outcome = orchestrator.login("wechat", LoginCredentials.Interactive)

        val error = (outcome as LoginOutcome.Failure).error
        assertTrue(error is LoginError.Unavailable)
        assertEquals("未接入微信 SDK", error.message)
        assertEquals(0, strategy.loginCount)
    }

    @Test
    fun session_gone_failure_clears_local_session() = runBlocking {
        val repository = FakeSessionRepository()
        var cleared = 0
        val orchestrator = orchestrator(
            repository = repository,
            state = DefaultSessionState(repository),
            strategy = FakeStrategy(
                id = "fake",
                outcome = LoginOutcome.Failure(LoginError.SessionGone("会话已失效")),
            ),
            onSessionCleared = { cleared++ },
        )

        val outcome = orchestrator.login("fake", LoginCredentials.Password("a", "b"))

        assertTrue(outcome is LoginOutcome.Failure)
        assertEquals(1, cleared)
        assertEquals(0, repository.setCount)
    }

    @Test
    fun cancelled_is_not_treated_as_failure() = runBlocking {
        val repository = FakeSessionRepository()
        var cleared = 0
        val orchestrator = orchestrator(
            repository = repository,
            state = DefaultSessionState(repository),
            strategy = FakeStrategy(id = "fake", outcome = LoginOutcome.Cancelled),
            onSessionCleared = { cleared++ },
        )

        val outcome = orchestrator.login("fake", LoginCredentials.Interactive)

        assertEquals(LoginOutcome.Cancelled, outcome)
        assertEquals(0, cleared)
    }

    @Test
    fun unexpected_exception_is_mapped_to_failure() = runBlocking {
        val repository = FakeSessionRepository()
        val orchestrator = orchestrator(
            repository = repository,
            state = DefaultSessionState(repository),
            strategy = FakeStrategy(
                id = "fake",
                failure = IllegalStateException("boom"),
            ),
        )

        val outcome = orchestrator.login("fake", LoginCredentials.Password("a", "b"))

        val error = (outcome as LoginOutcome.Failure).error
        assertTrue(error is LoginError.Unknown)
        assertEquals("boom", error.message)
    }

    @Test
    fun cancellation_is_rethrown() {
        val repository = FakeSessionRepository()
        val orchestrator = orchestrator(
            repository = repository,
            state = DefaultSessionState(repository),
            strategy = FakeStrategy(
                id = "fake",
                failure = CancellationException("cancelled"),
            ),
        )

        assertFailsWith<CancellationException> {
            runBlocking { orchestrator.login("fake", LoginCredentials.Password("a", "b")) }
        }
        assertEquals(0, repository.setCount)
    }

    @Test
    fun descriptors_come_from_registry_order() {
        val repository = FakeSessionRepository()
        val orchestrator = LoginOrchestrator(
            registry = DefaultLoginStrategyRegistry(
                listOf(
                    FakeStrategy("phone_otp", order = 20),
                    FakeStrategy("password", order = 10),
                ),
            ),
            repository = repository,
            state = DefaultSessionState(repository),
            publishState = {},
        )

        assertEquals(listOf("password", "phone_otp"), orchestrator.descriptors().map { it.id })
    }

    private fun orchestrator(
        repository: FakeSessionRepository,
        state: DefaultSessionState,
        strategy: FakeStrategy = FakeStrategy("fake"),
        onSessionCleared: () -> Unit = {},
    ): LoginOrchestrator = LoginOrchestrator(
        registry = DefaultLoginStrategyRegistry(listOf(strategy)),
        repository = repository,
        state = state,
        publishState = {},
        onSessionCleared = onSessionCleared,
    )

    private class FakeStrategy(
        override val id: String,
        override val order: Int = 100,
        private val outcome: LoginOutcome? = null,
        private val availability: LoginAvailability = LoginAvailability.Available,
        private val failure: Throwable? = null,
    ) : LoginStrategy {
        override val displayName: String = id
        var loginCount: Int = 0
            private set

        override fun availability(): LoginAvailability = availability

        override suspend fun login(credentials: LoginCredentials): LoginOutcome {
            loginCount++
            failure?.let { throw it }
            return outcome ?: LoginOutcome.Success(AuthSessionPayload(token = "token"))
        }
    }

    private class FakeSessionRepository : SessionRepository {
        var session: AccountSession = LoggedOut
            private set
        var setCount: Int = 0
            private set

        override fun current(): AccountSession = session

        override fun setSession(value: AccountSession) {
            session = value
            setCount++
        }

        override fun logout() {
            session = LoggedOut
        }

        override fun updateAuthTokens(token: String, refreshToken: String, sessionId: String) = Unit

        override fun updateProfile(displayName: String?, avatarUrl: String?) = Unit

        override fun updateProfilePhone(phone: String) = Unit

        override fun updateProfileEmail(email: String) = Unit

        override fun updateCompanyOrg(companyId: Long?, orgId: Long?) = Unit
    }
}

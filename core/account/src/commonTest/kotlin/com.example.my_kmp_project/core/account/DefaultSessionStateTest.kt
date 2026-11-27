package com.example.my_kmp_project.core.account

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * DI 第一批：core 层会话状态只有 StateFlow，不依赖 Compose。
 * 这些用例锁住「sync 读仓库、clearLocal 只清本地」的既有语义。
 */
class DefaultSessionStateTest {

    private class FakeRepository(var stored: AccountSession = LoggedOut) : SessionRepository {
        override fun current(): AccountSession = stored

        override fun setSession(value: AccountSession) {
            stored = value
        }

        override fun logout() {
            stored = LoggedOut
        }

        override fun updateAuthTokens(token: String, refreshToken: String, sessionId: String) = Unit

        override fun updateProfile(displayName: String?, avatarUrl: String?) = Unit

        override fun updateProfilePhone(phone: String) = Unit

        override fun updateProfileEmail(email: String) = Unit

        override fun updateCompanyOrg(companyId: Long?, orgId: Long?) = Unit
    }

    @Test
    fun startsLoggedOut() {
        val state = DefaultSessionState(FakeRepository())
        assertEquals(SessionState.Snapshot(isLoggedIn = false, displayName = null), state.snapshot.value)
    }

    @Test
    fun syncPublishesTheRepositorySession() {
        val repository = FakeRepository()
        val state = DefaultSessionState(repository)

        repository.stored = LoggedInUser(displayName = "Ada", token = "t")
        state.sync()

        val snapshot = state.snapshot.value
        assertTrue(snapshot.isLoggedIn)
        assertEquals("Ada", snapshot.displayName)
    }

    @Test
    fun clearLocalPublishesLoggedOutButLeavesTheRepositoryAlone() {
        val repository = FakeRepository(LoggedInUser(displayName = "Ada", token = "t"))
        val state = DefaultSessionState(repository)
        state.sync()

        state.clearLocal()

        assertEquals(SessionState.Snapshot(isLoggedIn = false, displayName = null), state.snapshot.value)
        // 仓库本身未被清空：清本地只负责 UI 视图（登出由 AuthSessionActions 显式 logout）。
        assertTrue(repository.stored.isLoggedIn)
    }

    @Test
    fun syncAfterRepositoryLogoutReturnsToLoggedOut() {
        val repository = FakeRepository(LoggedInUser(displayName = "Ada", token = "t"))
        val state = DefaultSessionState(repository)
        state.sync()

        repository.logout()
        state.sync()

        assertFalse(state.snapshot.value.isLoggedIn)
        assertEquals(null, state.snapshot.value.displayName)
    }
}

/** 过渡期单例必须和注入视图指向同一仓库，否则迁移期会出现两份会话状态。 */
class AccountFacadeRepositoryViewTest {

    @Test
    fun repositoryViewIsStable() {
        assertSame(AccountFacade.repository(), AccountFacade.repository())
    }
}

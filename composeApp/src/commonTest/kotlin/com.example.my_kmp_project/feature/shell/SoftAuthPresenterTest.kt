package com.example.my_kmp_project.feature.shell

import com.example.my_kmp_project.core.router.MainTab
import com.example.my_kmp_project.feature.auth.AuthGate
import com.example.my_kmp_project.feature.auth.AuthSessionState
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SoftAuthPresenterTest {

    private lateinit var presenter: SoftAuthPresenter

    @BeforeTest
    fun setUp() {
        AuthGate.clearPending()
        AuthSessionState.clearLocal()
        presenter = SoftAuthPresenter(
            sessionSync = { },
            isLoggedIn = { AuthSessionState.isLoggedIn },
        )
    }

    @Test
    fun selecting_chat_while_logged_out_opens_gate() {
        assertFalse(presenter.trySelectTab(MainTab.Chat))
        assertTrue(presenter.uiState.value.showAuthGate)
        assertEquals(MainTab.Chat, presenter.uiState.value.pendingTab)
    }

    @Test
    fun selecting_home_while_logged_out_does_not_open_gate() {
        assertTrue(presenter.trySelectTab(MainTab.Home))
        assertFalse(presenter.uiState.value.showAuthGate)
        assertNull(presenter.uiState.value.pendingTab)
    }

    @Test
    fun login_success_resumes_pending_tab() {
        presenter.trySelectTab(MainTab.Community)
        // Simulate login without AccountFacade
        AuthSessionState.clearLocal()
        // Force logged-in snapshot via sync path: publish by reflecting clear then manual
        // SoftAuthPresenter reads AuthSessionState.isLoggedIn — set through clearLocal false.
        // Use a custom presenter with logged-in flag for this slice:
        val loggedInPresenter = SoftAuthPresenter(
            sessionSync = { },
            isLoggedIn = { true },
        )
        AuthGate.rememberPending(MainTab.Community)
        assertEquals(MainTab.Community, loggedInPresenter.onLoginSucceeded())
        assertFalse(loggedInPresenter.uiState.value.showAuthGate)
        assertNull(AuthGate.pendingTab)
    }

    @Test
    fun clear_local_session_closes_gate() {
        presenter.trySelectTab(MainTab.Chat)
        presenter.clearLocalSession()
        assertFalse(presenter.uiState.value.isLoggedIn)
        assertFalse(presenter.uiState.value.showAuthGate)
        assertNull(presenter.uiState.value.pendingTab)
    }
}

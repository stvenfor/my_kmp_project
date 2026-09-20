package com.example.my_kmp_project.feature.shell

import com.example.my_kmp_project.core.router.MainTab
import com.example.my_kmp_project.feature.auth.AuthGate
import com.example.my_kmp_project.feature.auth.AuthSessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Shared Presentation Logic for shell session + soft-auth (ADR 0002).
 * Native shells and Mine island host observe [uiState]; they do not own login rules.
 */
internal class SoftAuthPresenter(
    private val sessionSync: () -> Unit = { AuthSessionState.sync() },
    private val isLoggedIn: () -> Boolean = { AuthSessionState.isLoggedIn },
) {
    data class UiState(
        val isLoggedIn: Boolean = false,
        val displayName: String? = null,
        val pendingTab: MainTab? = null,
        val showAuthGate: Boolean = false,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun sync() {
        sessionSync()
        publish()
    }

    fun clearLocalSession() {
        AuthSessionState.clearLocal()
        AuthGate.clearPending()
        publish(showGate = false)
    }

    /**
     * @return true if the tab may be shown; false if soft-auth gate should open.
     */
    fun trySelectTab(tab: MainTab): Boolean {
        if (AuthGate.requiresAuth(tab) && !isLoggedIn()) {
            AuthGate.rememberPending(tab)
            publish(showGate = true)
            return false
        }
        publish(showGate = false)
        return true
    }

    fun onLoginSucceeded(): MainTab {
        sync()
        val resume = AuthGate.consumePending() ?: MainTab.Home
        publish(showGate = false)
        return resume
    }

    fun dismissGate() {
        AuthGate.clearPending()
        publish(showGate = false)
    }

    private fun publish(showGate: Boolean? = null) {
        _uiState.update {
            UiState(
                isLoggedIn = isLoggedIn(),
                displayName = AuthSessionState.displayName,
                pendingTab = AuthGate.pendingTab,
                showAuthGate = showGate ?: it.showAuthGate,
            )
        }
    }
}

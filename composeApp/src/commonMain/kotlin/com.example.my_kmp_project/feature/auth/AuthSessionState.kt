package com.example.my_kmp_project.feature.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.my_kmp_project.core.account.AccountFacade
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Sole reactive session source of truth for UI soft-gates (ADR 0001).
 *
 * [snapshot] / Compose fields are updated together from [sync] and auth writes.
 * Prefer collecting [snapshot] in new code; legacy `isLoggedIn`/`displayName`
 * remain for existing callers during Spike I migration.
 */
internal object AuthSessionState {
    data class Snapshot(
        val isLoggedIn: Boolean = false,
        val displayName: String? = null,
    )

    private val _snapshot = MutableStateFlow(Snapshot())
    val snapshot: StateFlow<Snapshot> = _snapshot.asStateFlow()

    var isLoggedIn by mutableStateOf(false)
        private set

    var displayName by mutableStateOf<String?>(null)
        private set

    /** Pull latest values from [AccountFacade] (hydrates store on first access). */
    fun sync() {
        val session = AccountFacade.current()
        publish(session.isLoggedIn, session.displayName)
    }

    fun clearLocal() {
        publish(false, null)
    }

    private fun publish(loggedIn: Boolean, name: String?) {
        isLoggedIn = loggedIn
        displayName = name
        _snapshot.value = Snapshot(isLoggedIn = loggedIn, displayName = name)
    }
}

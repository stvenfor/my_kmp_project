package com.example.my_kmp_project.feature.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.my_kmp_project.core.account.AccountFacade

/**
 * Observable session mirror for Compose soft-gates and Mine header.
 *
 * Backed by [AccountFacade] persistence; call [sync] after process start
 * and whenever the façade session changes outside [AuthRepository].
 */
internal object AuthSessionState {
    var isLoggedIn by mutableStateOf(false)
        private set

    var displayName by mutableStateOf<String?>(null)
        private set

    /** Pull latest values from [AccountFacade] (hydrates store on first access). */
    fun sync() {
        val session = AccountFacade.current()
        isLoggedIn = session.isLoggedIn
        displayName = session.displayName
    }

    fun clearLocal() {
        isLoggedIn = false
        displayName = null
    }
}

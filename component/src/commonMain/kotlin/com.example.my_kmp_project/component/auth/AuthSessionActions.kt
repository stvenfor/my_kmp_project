package com.example.my_kmp_project.component.auth

import com.example.my_kmp_project.core.account.AccountFacade

/**
 * Local session-teardown contract shared by features (rule R7: no feature→feature imports).
 *
 * Same transaction as `AuthRepository.logout()` (façade clear + reactive state +
 * soft-auth pending tab); the network-side login flows stay in `feature/auth`.
 */
public object AuthSessionActions {
    fun logout() {
        AccountFacade.logout()
        AuthSessionState.clearLocal()
        AuthGate.clearPending()
    }
}

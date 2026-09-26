package com.example.my_kmp_project.feature.auth

import com.example.my_kmp_project.core.account.AccountFacade
import com.example.my_kmp_project.core.account.LoggedInUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Public callback bridge for native shells (SwiftUI / ArkTS via platform export)
 * that cannot call suspend [AuthRepository] directly.
 *
 * Always commits into [AccountFacade] so Compose islands see the same session.
 */
object AuthBridge {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun isLoggedIn(): Boolean = AccountFacade.current().isLoggedIn

    fun displayName(): String =
        AccountFacade.current().displayName?.takeIf { it.isNotBlank() } ?: "用户"

    fun logout() {
        AuthRepository.logout()
    }

    /** Apply a session obtained outside AuthRepository (e.g. OHOS Huawei ID). */
    fun applySession(
        token: String,
        userId: String,
        displayName: String,
        phone: String? = null,
        email: String? = null,
    ) {
        if (token.isBlank()) return
        AccountFacade.setSession(
            LoggedInUser(
                displayName = displayName.ifBlank { "用户" },
                token = token,
                userId = userId.ifBlank { displayName },
                avatarUrl = null,
                phone = phone,
                email = email,
                companyId = null,
                orgId = null,
            ),
        )
        AuthSessionState.sync()
    }

    fun loginWithPassword(
        account: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        scope.launch {
            val result = AuthRepository.loginWithPassword(account, password)
            deliver(result, onSuccess, onError)
        }
    }

    fun loginWithOtp(
        phone: String,
        code: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        scope.launch {
            val result = AuthRepository.loginWithOtp(phone, code)
            deliver(result, onSuccess, onError)
        }
    }

    fun sendPhoneOtp(
        phone: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        scope.launch {
            val result = AuthRepository.sendPhoneOtp(phone)
            deliver(result, onSuccess, onError)
        }
    }

    fun register(
        email: String,
        password: String,
        displayName: String = "",
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        scope.launch {
            val result = AuthRepository.register(email, password, displayName)
            deliver(result, onSuccess, onError)
        }
    }

    fun registerWithPhone(
        phone: String,
        code: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        scope.launch {
            val result = AuthRepository.registerWithPhone(phone, code)
            deliver(result, onSuccess, onError)
        }
    }

    private suspend fun deliver(
        result: Result<Unit>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        withContext(Dispatchers.Main) {
            result.fold(
                onSuccess = { onSuccess() },
                onFailure = { onError(it.message ?: "登录失败，请稍后重试") },
            )
        }
    }
}

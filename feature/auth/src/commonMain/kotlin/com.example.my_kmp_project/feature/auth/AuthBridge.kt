package com.example.my_kmp_project.feature.auth

import com.example.my_kmp_project.component.auth.AuthSessionPayload
import com.example.my_kmp_project.component.auth.AuthSessionState
import com.example.my_kmp_project.component.auth.LoginCredentials
import com.example.my_kmp_project.component.auth.LoginOutcome
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Public callback bridge for native shells (SwiftUI / ArkTS via platform export)
 * that cannot call suspend [AuthRepository] directly.
 *
 * Always commits into [AccountFacade] so Compose islands see the same session.
 */
object AuthBridge {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun isLoggedIn(): Boolean = AuthDependencies.session.current().isLoggedIn

    fun displayName(): String =
        AuthDependencies.session.current().displayName?.takeIf { it.isNotBlank() } ?: "用户"

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
        refreshToken: String = "",
        sessionId: String = "",
        deviceId: String = "",
    ) {
        if (token.isBlank()) return
        // 与登录 / 注册 / 华为回码共用同一落库出口（SessionRepository + SessionState）。
        LoginSessionEstablisher.establish(
            payload = AuthSessionPayload(
                token = token,
                refreshToken = refreshToken,
                sessionId = sessionId,
                userId = userId,
                displayName = displayName,
                email = email.orEmpty(),
                phone = phone.orEmpty(),
                deviceId = deviceId,
            ),
            repository = AuthDependencies.session,
            state = AuthSessionState.state(),
        )
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

    /**
     * 通用登录入口：原生壳（ArkTS / Swift）拿到的授权码按稳定 method id 交给策略。
     *
     * 华为 / 微信 / QQ 都不再各自新增 Kotlin 函数；新增方式只改注册表。
     */
    fun loginWithMethod(
        methodId: String,
        code: String,
        deviceId: String = "",
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        scope.launch {
            val outcome = AuthStrategies.controller.login(
                methodId = methodId,
                credentials = LoginCredentials.ExternalCode(code = code, deviceId = deviceId),
            )
            deliver(outcome, onSuccess, onError)
        }
    }

    /**
     * 当前可用登录方式（JSON，供 ArkTS / Swift 动态渲染）。
     *
     * 只返回 `available()`：不可用方式不渲染死按钮；字段与
     * [com.example.my_kmp_project.component.auth.LoginMethodDescriptor] 对齐。
     */
    fun availableLoginMethodsJson(): String =
        Json.encodeToString(
            AuthStrategies.registry.available().map { descriptor ->
                LoginMethodJson(
                    id = descriptor.id,
                    displayName = descriptor.displayName,
                    interactive = descriptor.interactive,
                    available = descriptor.available,
                )
            },
        )

    fun loginWithHuaweiCode(
        code: String,
        deviceId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        loginWithMethod(LoginMethodIds.HUAWEI, code, deviceId, onSuccess, onError)
    }

    fun token(): String = AuthDependencies.session.current().token

    fun userId(): String = AuthDependencies.session.current().userId.orEmpty()

    fun phone(): String = AuthDependencies.session.current().phone.orEmpty()

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

    private suspend fun deliver(
        outcome: LoginOutcome,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        withContext(Dispatchers.Main) {
            when (outcome) {
                is LoginOutcome.Success -> onSuccess()
                is LoginOutcome.Failure -> onError(outcome.error.message)
                LoginOutcome.Cancelled -> onError("登录已取消")
            }
        }
    }
}

/** 原生桥 JSON 形状；字段名供 ArkTS / Swift 直接读取。 */
@Serializable
private data class LoginMethodJson(
    val id: String,
    val displayName: String,
    val interactive: Boolean,
    val available: Boolean,
)

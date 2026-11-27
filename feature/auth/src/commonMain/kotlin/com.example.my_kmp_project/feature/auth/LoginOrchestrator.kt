package com.example.my_kmp_project.feature.auth

import com.example.my_kmp_project.component.auth.AuthGate
import com.example.my_kmp_project.component.auth.AuthSessionPayload
import com.example.my_kmp_project.component.auth.AuthSessionState
import com.example.my_kmp_project.component.auth.LoginAvailability
import com.example.my_kmp_project.component.auth.LoginController
import com.example.my_kmp_project.component.auth.LoginCredentials
import com.example.my_kmp_project.component.auth.LoginError
import com.example.my_kmp_project.component.auth.LoginMethodDescriptor
import com.example.my_kmp_project.component.auth.LoginOutcome
import com.example.my_kmp_project.component.auth.LoginStrategyRegistry
import com.example.my_kmp_project.core.account.LoggedInUser
import com.example.my_kmp_project.core.account.SessionRepository
import com.example.my_kmp_project.core.account.SessionState
import kotlinx.coroutines.CancellationException

/**
 * 登录编排（策略模式第 1 批）。
 *
 * 三个职责，且只有这三个：
 * 1. 按 method id 找策略并执行（UI 不再 `when (method)`）；
 * 2. 成功时**唯一**一次会话落库（[establishSession]）；
 * 3. 失败且策略标记 `clearsSession` 时清本地会话。
 *
 * 不参与：页面跳转、pending tab 恢复、realtime 补连 —— 那些仍由 app 壳的
 * `afterAuthSuccess` 负责。
 */
public class LoginOrchestrator(
    private val registry: LoginStrategyRegistry,
    private val repository: SessionRepository,
    private val state: SessionState,
    private val publishState: () -> Unit = { AuthSessionState.sync() },
    private val onSessionCleared: () -> Unit = {
        repository.logout()
        state.clearLocal()
        AuthGate.clearPending()
        publishState()
    },
) : LoginController {

    override fun descriptors(): List<LoginMethodDescriptor> = registry.descriptors()

    override suspend fun login(
        methodId: String,
        credentials: LoginCredentials,
    ): LoginOutcome {
        val strategy = registry.find(methodId)
            ?: return LoginOutcome.Failure(LoginError.Unavailable(methodId, "未知登录方式"))

        val availability = strategy.availability()
        if (availability is LoginAvailability.Unavailable) {
            return LoginOutcome.Failure(LoginError.Unavailable(methodId, availability.reason))
        }

        val outcome = try {
            strategy.login(credentials)
        } catch (e: CancellationException) {
            // 协程取消不是登录失败，原样上抛。
            throw e
        } catch (t: Throwable) {
            LoginOutcome.Failure(t.toLoginError())
        }

        when (outcome) {
            is LoginOutcome.Success -> establishSession(outcome.session)
            is LoginOutcome.Failure -> if (outcome.error.clearsSession) onSessionCleared()
            LoginOutcome.Cancelled -> Unit
        }
        return outcome
    }

    /**
     * 会话落库唯一出口：登录、注册即登录、华为 / 微信回码、冷启动壳缓存都走这里。
     *
     * 兜底规则与旧 `AuthRepository.commitSession` 一致：
     * displayName 空 → 邮箱前缀 → 邮箱 → 「用户」；userId 空 → displayName。
     */
    public fun establishSession(session: AuthSessionPayload): Result<Unit> =
        LoginSessionEstablisher.establish(
            payload = session,
            repository = repository,
            state = state,
            publish = publishState,
        )
}

/** 会话落库实现：`SessionRepository.setSession` + `SessionState.sync`。 */
internal object LoginSessionEstablisher {

    fun establish(
        payload: AuthSessionPayload,
        repository: SessionRepository,
        state: SessionState,
        publish: () -> Unit = { AuthSessionState.sync() },
    ): Result<Unit> {
        if (payload.token.isBlank()) {
            return Result.failure(IllegalStateException("登录响应缺少 token"))
        }

        val displayName = payload.displayName.trim()
            .ifBlank { payload.email.substringBefore('@').ifBlank { payload.email }.trim() }
            .ifBlank { "用户" }
        val userId = payload.userId.trim().ifBlank { displayName }

        repository.setSession(
            LoggedInUser(
                displayName = displayName,
                token = payload.token,
                refreshToken = payload.refreshToken,
                sessionId = payload.sessionId,
                deviceId = payload.deviceId,
                userId = userId,
                avatarUrl = payload.avatarUrl.takeIf { it.isNotBlank() },
                phone = payload.phone.takeIf { it.isNotBlank() },
                email = payload.email.takeIf { it.isNotBlank() },
                companyId = null,
                orgId = null,
            ),
        )
        state.sync()
        publish()
        return Result.success(Unit)
    }
}

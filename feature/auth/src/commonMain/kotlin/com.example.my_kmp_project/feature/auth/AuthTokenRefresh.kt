package com.example.my_kmp_project.feature.auth

import com.example.my_kmp_project.component.auth.AuthGate
import com.example.my_kmp_project.component.auth.AuthSessionState
import com.example.my_kmp_project.core.account.AccountFacade
import com.example.my_kmp_project.core.network.ApiClient
import com.example.my_kmp_project.core.network.AuthRefreshDecision
import com.example.my_kmp_project.core.network.AuthRefreshPolicy
import com.example.my_kmp_project.core.network.TokenRefreshHandler
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Flutter `AuthTokenRefreshInterceptor` + `UserService`/`SessionGuardHook` 的 KMP 对等
 * （my_ai_project d7876a1：401/403 会话决策统一）。
 *
 * - [policy]：HTTP/业务码 → 刷新决策（复用 [classifyAuthHttp]，403 不透传刷新）
 * - [handler]：单飞刷新执行体；失败/无 refreshToken 时走 [handleRefreshExhausted]
 * - 刷新请求经 [com.example.my_kmp_project.core.network.NetworkFacade.rawApi]
 *   （未装饰 client）发出，不会递归触发刷新（Flutter `skipAuthRefresh` 对等）。
 */
public object AuthTokenRefresh {

    private val mutex = Mutex()

    /** 单飞：进行中的刷新；后续 401 搭同一班车（Flutter `_refreshCompleter`）。 */
    private var inFlight: CompletableDeferred<String>? = null

    /** 供 [com.example.my_kmp_project.core.network.NetworkFacade] 装饰器注入。 */
    val policy = AuthRefreshPolicy { statusCode, code, message ->
        when (classifyAuthHttp(statusCode = statusCode, code = code, message = message.orEmpty())) {
            AuthHttpDecision.TryRefresh -> AuthRefreshDecision.Refresh
            // 互踢/会话无效 与 403「账号不可用」都意味着本地会话已废 → 清会话回登录。
            AuthHttpDecision.ForceLogout, AuthHttpDecision.AccountUnavailable -> AuthRefreshDecision.ForceLogout
            AuthHttpDecision.Forbidden, AuthHttpDecision.None -> AuthRefreshDecision.PassThrough
        }
    }

    /** 供装饰器注入；成功后会话三件套已原子替换，重试自动带头 token。 */
    val handler = TokenRefreshHandler { raw -> refresh(raw) }

    /**
     * 单飞刷新。已在刷新中 → 等待其结果并返回当前 token；
     * 否则发起刷新，成功返回新 token，失败抛错（调用方按首次 401 应答提示）。
     */
    suspend fun refresh(raw: ApiClient): String {
        val existing = mutex.withLock { inFlight }
        if (existing != null) return existing.await()
        val deferred = CompletableDeferred<String>()
        mutex.withLock { inFlight = deferred }
        try {
            val token = performRefresh(raw)
            deferred.complete(token)
            return token
        } catch (t: Throwable) {
            deferred.completeExceptionally(t)
            throw t
        } finally {
            mutex.withLock { inFlight = null }
        }
    }

    private suspend fun performRefresh(raw: ApiClient): String {
        val session = AccountFacade.current()
        // Flutter：refreshToken 为空 → 刷新耗尽（不发起请求）。
        if (!session.isLoggedIn || session.refreshToken.isBlank()) {
            handleRefreshExhausted()
            throw IllegalStateException(AuthFailure.SessionCleared().message)
        }
        // 契约表 #3：HTTP 与 omit 规则在 UserAuthHttpApi.refresh，本函数只负责会话落库与耗尽处理。
        // device_id / session_id / platform 为空白时由 AuthRefreshRequest.of 省略 key。
        val result = UserAuthHttpApi.refresh(
            raw = raw,
            refreshToken = session.refreshToken,
            deviceId = session.deviceId.ifBlank { AuthDeviceContext.deviceId() },
            sessionId = session.sessionId,
            platform = AuthDeviceContext.platform(),
        )
        val data = result.getOrNull()
        if (data == null) {
            handleRefreshExhausted()
            throw IllegalStateException(
                result.exceptionOrNull()?.message ?: AuthFailure.SessionCleared().message,
            )
        }
        AccountFacade.updateAuthTokens(
            token = data.token,
            // 服务端可能只轮换 access token：缺省时沿用旧 refresh/session。
            refreshToken = data.refreshToken.ifBlank { session.refreshToken },
            sessionId = data.sessionId.ifBlank { session.sessionId },
        )
        return data.token
    }

    /**
     * Flutter `SessionGuardHook.handleRefreshExhausted`：刷新耗尽/互踢后的清会话。
     * 仅已登录才处理（幂等）；token 已废，不再请求远端 logout（Flutter 同款）。
     */
    fun handleRefreshExhausted() {
        if (!AccountFacade.current().isLoggedIn) return
        AccountFacade.logout()
        AuthSessionState.clearLocal()
        AuthGate.clearPending()
    }
}

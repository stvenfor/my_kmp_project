package com.example.my_kmp_project.feature.auth

import com.example.my_kmp_project.component.auth.AuthGate
import com.example.my_kmp_project.component.auth.AuthSessionActions
import com.example.my_kmp_project.component.auth.AuthSessionPayload
import com.example.my_kmp_project.component.auth.AuthSessionState
import com.example.my_kmp_project.component.auth.LoginController
import com.example.my_kmp_project.component.auth.LoginCredentials
import com.example.my_kmp_project.component.auth.LoginOutcome

/**
 * Remote auth repository aligned with Flutter `UserAuthApi` / `BackendAuthService`.
 *
 * HTTP 契约（契约表 #1–#7）由 [UserAuthHttpApi] 承担。策略模式第 1 批后，
 * 密码 / 短信登录已委托 [PasswordLoginStrategy] / [PhoneOtpLoginStrategy]；
 * 本对象保留为旧调用点（三端原生桥）的兼容门面，注册 / 华为 / 微信仍在此编排，
 * 所有成功会话统一走 [LoginSessionEstablisher] 落库。
 *
 * 无本地 Mock：debug/release 一律请求 Go BFF 真实链路。
 */
object AuthRepository {

    /** 策略模式第 1 批：本对象只做委托，校验 / 请求 / 会话载荷由 [PasswordLoginStrategy] 承担。 */
    suspend fun loginWithPassword(account: String, password: String): Result<Unit> =
        AuthStrategies.controller
            .login(LoginMethodIds.PASSWORD, LoginCredentials.Password(account, password))
            .toUnitResult()

    /** 契约表 #5：`data` 恒 null，只看 `code == 0`；委托 [PhoneOtpLoginStrategy.sendOtp]。 */
    suspend fun sendPhoneOtp(phone: String): Result<Unit> =
        AuthStrategies.phoneOtp.sendOtp(phone).guardSessionGone()

    /** 契约表 #6：`data` = `LoginResult`（与 #1 同 DTO）；委托 [PhoneOtpLoginStrategy]。 */
    suspend fun loginWithOtp(phone: String, code: String): Result<Unit> =
        AuthStrategies.controller
            .login(LoginMethodIds.PHONE_OTP, LoginCredentials.PhoneOtp(phone, code))
            .toUnitResult()

    /**
     * 登录页默认控制器（过渡期）：与 app 层 `AuthModule` 构建的是同一组策略。
     * 新 UI 应优先由装配层传入 controller，而不是继续依赖本 object。
     */
    fun loginController(): LoginController = AuthStrategies.controller

    /**
     * Email register matching Flutter `BackendAuthService.signUpWithEmail`
     * （契约表 #2；`data` 双形态见 [AuthRegisterData.resolvedUser]）.
     * If the response has no session token, falls back to password login.
     */
    suspend fun register(
        email: String,
        password: String,
        displayName: String = "",
    ): Result<Unit> {
        val normalizedEmail = email.trim()
        if (!AuthValidators.isValidEmail(normalizedEmail)) {
            return Result.failure(IllegalArgumentException("请输入有效的邮箱"))
        }
        if (password.length < AuthValidators.MIN_PASSWORD_LENGTH) {
            return Result.failure(IllegalArgumentException("密码至少 6 位"))
        }
        val username = displayName.trim().ifBlank {
            normalizedEmail.substringBefore('@').ifBlank { normalizedEmail }
        }
        val result = AuthDependencies.api.register(
            username = username,
            password = password,
            email = normalizedEmail,
            deviceId = AuthDeviceContext.deviceId(),
            platform = AuthDeviceContext.platform(),
        ).guardSessionGone()
        return result.fold(
            onSuccess = { data ->
                val payload = data.toPayload()
                if (payload.hasSession) {
                    commitSession(
                        displayName = payload.username.ifBlank { username },
                        userId = payload.userId.ifBlank { username },
                        token = payload.token!!,
                        phone = payload.phone.ifBlank { null },
                        email = payload.email.ifBlank { normalizedEmail },
                        refreshToken = payload.refreshToken.orEmpty(),
                        sessionId = payload.sessionId.orEmpty(),
                        avatarUrl = payload.avatarUrl.ifBlank { null },
                    )
                } else {
                    // Flutter：注册只回资料不回 token 时，再走一次密码登录拿会话。
                    loginWithPassword(normalizedEmail, password)
                }
            },
            onFailure = { Result.failure(it) },
        )
    }

    /** Phone register: same OTP verify contract as login (Flutter registerWithPhone). */
    suspend fun registerWithPhone(phone: String, code: String): Result<Unit> =
        loginWithOtp(phone, code)

    /**
     * Exchange Huawei Account Kit authorization [code] for a BFF session
     * (`POST /api/v1/user/huawei/login`).
     *
     * 批次 2 起委托 [HuaweiLoginStrategy]；旧签名保留给 ArkTS 旧 op 与 iOS 调用点。
     */
    suspend fun loginWithHuaweiCode(code: String, deviceId: String): Result<Unit> =
        AuthStrategies.controller
            .login(
                LoginMethodIds.HUAWEI,
                LoginCredentials.ExternalCode(code = code, deviceId = deviceId),
            )
            .toUnitResult()

    /**
     * 契约表 #7：微信授权码换会话（Flutter `BackendAuthService.signInWithWechatCode`，:74）。
     *
     * 取码已由 [WechatLoginStrategy] 通过 `AuthorizationCodeProvider` 统一处理；
     * 未注册 provider 时该登录方式不可用。本方法仅保留给旧原生桥直传 `code`。
     */
    suspend fun loginWithWechatCode(code: String, deviceId: String = ""): Result<Unit> {
        val authCode = code.trim()
        if (authCode.isEmpty()) {
            return Result.failure(IllegalArgumentException("缺少微信授权码"))
        }
        return AuthDependencies.api.loginWithWechat(
            code = authCode,
            deviceId = deviceId.trim().ifBlank { AuthDeviceContext.deviceId() },
            platform = AuthDeviceContext.platform(),
        ).commitLogin()
    }

    /** 本地登出（互踢 / 会话已废时走这条，对应 Flutter `logout(remote: false)`）。 */
    fun logout() {
        AuthSessionActions.logout()
    }

    /**
     * 契约表 #4：Flutter `BackendAuthService.signOut`（:97-121）**Server-Confirmed Logout**：
     * token/sessionId/deviceId 三者齐备才打远端 `POST /user/logout`（无 body，只看 `code == 0`）；
     * 失败时**只有**「会话已废」类错误才继续清本地，其余错误原样上抛并**保留**本地会话；
     * 远端成功或 Gone 之后一律 `clearUser()`。
     */
    suspend fun logoutWithRemote(): Result<Unit> {
        val session = AuthDependencies.session.current()
        if (!session.isLoggedIn ||
            session.token.isBlank() ||
            session.sessionId.isBlank() ||
            session.deviceId.isBlank()
        ) {
            // 会话不完整（token 已废 / 缺 header）→ 不做远端登出，直接清本地。
            AuthSessionActions.logout()
            return Result.success(Unit)
        }
        return AuthDependencies.api.logout().fold(
            onSuccess = {
                AuthSessionActions.logout()
                Result.success(Unit)
            },
            onFailure = { error ->
                if (isLogoutSessionGone(error)) {
                    AuthSessionActions.logout()
                    Result.success(Unit)
                } else {
                    // 网络等失败：保留本地会话，用户可重试（Flutter 同款）。
                    Result.failure(error)
                }
            },
        )
    }

    /**
     * `POST /api/v1/user/deactivate`（Go `user_routes.go:26`）的会话编排：
     * **Server-Confirmed Logout**——远端确认账号注销成功后一律清本地会话
     * （三件套 [clearLocalSession]，对齐 `AuthTokenRefresh.handleRefreshExhausted`），
     * 因为账号已废、本地 token 不再可用；失败时按 [shouldClearSessionFor]
     * （即 `classifyAuthHttp` 决策）处理：会话已废 / 账号不可用才清，
     * 普通 403 / 网络错保留会话供重试。
     *
     * [remote] / [onClearLocal] 为测试注入口：生产调用 `deactivate()` 走
     * [UserAuthHttpApi.deactivate] 与真实清会话三件套；不依赖 Android Context 的
     * 单元测试可注入假结果观察编排。
     */
    suspend fun deactivate(
        remote: suspend () -> Result<Unit> = { AuthDependencies.api.deactivate() },
        onClearLocal: () -> Unit = ::clearLocalSession,
    ): Result<Unit> = remote().fold(
        onSuccess = {
            onClearLocal()
            Result.success(Unit)
        },
        onFailure = { error ->
            if (shouldClearSessionFor(error)) onClearLocal()
            Result.failure(error)
        },
    )

    /**
     * Flutter `isLogoutSessionGone`（`backend_auth_service.dart:255-275`）：
     * 服务端会话确实已废 / 无权限的失败——清本地是安全的。
     */
    internal fun isLogoutSessionGone(error: Throwable): Boolean {
        val code = (error as? AuthHttpFailure)?.code
        if (code != null &&
            (code == AuthBizCode.UNAUTHORIZED ||
                AuthBizCode.isSessionGone(code))
        ) {
            return true
        }
        val text = error.message.orEmpty()
        if (text.contains("Unauthorized") || text.contains("未登录") || text.contains("未授权")) {
            return true
        }
        return text.contains("token 无效") ||
            text.contains("token 已过期") ||
            text.contains("登录已失效") ||
            text.contains("会话不存在") ||
            text.contains("会话已失效") ||
            text.contains("会话无效") ||
            text.contains("session not found", ignoreCase = true)
    }

    /** 会话失效判定：文案已由 [UserAuthHttpApi] 映射，这里只补本地清理。 */
    private fun clearSessionIfGone(error: Throwable): Throwable {
        if (shouldClearSessionFor(error)) clearLocalSession()
        return error
    }

    /** 三件套清本地会话（对齐 `AuthTokenRefresh.handleRefreshExhausted` 的写法）。 */
    internal fun clearLocalSession() {
        AuthDependencies.session.logout()
        AuthSessionState.clearLocal()
        AuthGate.clearPending()
    }

    /**
     * 纯判定：该错误是否意味着本地会话已废、需要清会话回登录。
     * 覆盖互踢 / token 废（业务码 10021/10022/10023）与 403「账号不可用」
     * （[AuthHttpDecision.AccountUnavailable]，按 message 而非 code 判定）。
     * 普通 403（Forbidden）/ 网络错 / 无 code 的普通异常 → 不清，保留会话供重试。
     */
    internal fun shouldClearSessionFor(error: Throwable): Boolean {
        val failure = error as? AuthHttpFailure
        val code = failure?.code
        if (code != null &&
            (AuthBizCode.isSessionGone(code) || AuthFailure.fromBizCode(code).isSessionGoneFailure())
        ) {
            return true
        }
        val decision = classifyAuthHttp(
            statusCode = failure?.statusCode,
            code = code,
            message = failure?.message.orEmpty(),
        )
        return decision == AuthHttpDecision.ForceLogout ||
            decision == AuthHttpDecision.AccountUnavailable
    }

    /**
     * 会话落库。[UserAuthHttpApi] 已保证成功分支的 `token` 非空（Dart `_requireData`
     * 对 `data == null` / 空 token 一律抛），这里只把 DTO 归一为统一载荷，
     * 落库统一走 [LoginSessionEstablisher]。
     */
    private fun Result<AuthLoginData>.commitLogin(): Result<Unit> = fold(
        onSuccess = {
            LoginSessionEstablisher.establish(
                payload = it.toSessionPayload(),
                repository = AuthDependencies.session,
                state = AuthSessionState.state(),
            )
        },
        onFailure = { Result.failure(clearSessionIfGone(it)) },
    )

    // ── 会话落库 ──────────────────────────────────────────────────────────────────

    private fun <T> Result<T>.guardSessionGone(): Result<T> =
        fold(onSuccess = { Result.success(it) }, onFailure = { Result.failure(clearSessionIfGone(it)) })

    private fun commitSession(
        displayName: String,
        userId: String,
        token: String,
        phone: String?,
        email: String?,
        refreshToken: String = "",
        sessionId: String = "",
        avatarUrl: String? = null,
    ): Result<Unit> = LoginSessionEstablisher.establish(
        payload = AuthSessionPayload(
            token = token,
            refreshToken = refreshToken,
            sessionId = sessionId,
            userId = userId,
            displayName = displayName,
            email = email.orEmpty(),
            phone = phone.orEmpty(),
            avatarUrl = avatarUrl.orEmpty(),
            deviceId = AuthDeviceContext.deviceId(),
        ),
        repository = AuthDependencies.session,
        state = AuthSessionState.state(),
    )

    /** 旧 `Result<Unit>` 调用方（三端原生桥）兼容：失败仍是带中文文案的异常。 */
    private fun LoginOutcome.toUnitResult(): Result<Unit> = when (this) {
        is LoginOutcome.Success -> Result.success(Unit)
        is LoginOutcome.Failure -> Result.failure(error)
        LoginOutcome.Cancelled -> Result.failure(IllegalStateException("登录已取消"))
    }
}

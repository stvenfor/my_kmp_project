package com.example.my_kmp_project.feature.auth

import com.example.my_kmp_project.component.auth.AuthGate
import com.example.my_kmp_project.component.auth.AuthSessionActions
import com.example.my_kmp_project.component.auth.AuthSessionState
import com.example.my_kmp_project.core.account.AccountFacade
import com.example.my_kmp_project.core.account.LoggedInUser
import com.example.my_kmp_project.core.network.NetworkError
import com.example.my_kmp_project.core.network.NetworkFacade
import com.example.my_kmp_project.core.network.postApi

/**
 * Remote auth repository aligned with Flutter `UserAuthApi` / `BackendAuthService`.
 *
 * HTTP 契约（契约表 #1–#7）由 [UserAuthHttpApi] 承担；本对象只做**会话编排**
 * （校验 → 调用 → [AccountFacade] 落库 / 失效清理），不重复拼请求体。
 *
 * Mock 分流：[MockAuthConfig.useMockAuth] 为 true 时（debug 默认），手机号/邮箱
 * 登录走 Flutter `MockAuthService` 同款本地短路——不请求 Go BFF，测试号
 * `13400000000` + 验证码 `123456` 直接建本地会话。Release 下恒 false，走真实链路。
 */
object AuthRepository {

    suspend fun loginWithPassword(account: String, password: String): Result<Unit> {
        val user = account.trim()
        if (user.isEmpty()) {
            return Result.failure(IllegalArgumentException("请输入账号"))
        }
        if (user.contains('@') && !AuthValidators.isValidEmail(user)) {
            return Result.failure(IllegalArgumentException("请输入有效的邮箱"))
        }
        if (password.length < AuthValidators.MIN_PASSWORD_LENGTH) {
            return Result.failure(IllegalArgumentException("密码至少 6 位"))
        }
        if (MockAuthConfig.useMockAuth) {
            return mockEmailLogin(user)
        }
        return UserAuthHttpApi.login(
            username = user,
            password = password,
            deviceId = AuthDeviceContext.deviceId(),
            platform = AuthDeviceContext.platform(),
        ).commitLogin()
    }

    /** 契约表 #5：`data` 恒 null，只看 `code == 0`。 */
    suspend fun sendPhoneOtp(phone: String): Result<Unit> {
        if (!AuthPhoneUtils.isValidChinaMobile(phone)) {
            return Result.failure(IllegalArgumentException("请输入有效的手机号"))
        }
        if (MockAuthConfig.useMockAuth) {
            // Flutter `MockAuthService.sendPhoneOtp`：只认白名单，不真发短信。
            return if (MockAuthConfig.isMockPhone(phone)) {
                Result.success(Unit)
            } else {
                Result.failure(IllegalArgumentException(MockAuthConfig.rejectedPhoneMessage()))
            }
        }
        return UserAuthHttpApi
            .sendPhoneOtp(AuthPhoneUtils.toE164China(phone))
            .guardSessionGone()
    }

    /** 契约表 #6：`data` = `LoginResult`（与 #1 同 DTO）。 */
    suspend fun loginWithOtp(phone: String, code: String): Result<Unit> {
        if (!AuthPhoneUtils.isValidChinaMobile(phone)) {
            return Result.failure(IllegalArgumentException("请输入有效的手机号"))
        }
        if (!AuthValidators.isValidOtp(code)) {
            return Result.failure(IllegalArgumentException("请输入 6 位验证码"))
        }
        if (MockAuthConfig.useMockAuth) {
            return mockPhoneLogin(phone, code)
        }
        return UserAuthHttpApi.verifyPhoneOtp(
            phone = AuthPhoneUtils.toE164China(phone),
            otp = code.trim(),
            deviceId = AuthDeviceContext.deviceId(),
            platform = AuthDeviceContext.platform(),
        ).commitLogin()
    }

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
        if (MockAuthConfig.useMockAuth) {
            // Flutter `MockAuthService.signUpWithEmail` = 注册即登录。
            return mockEmailLogin(normalizedEmail)
        }
        val result = UserAuthHttpApi.register(
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
     * 该端点**不在**契约表 §2.1 内（本仓既有扩展，非 Flutter d7876a1 调用点），
     * 因此保留直连写法，不经 [UserAuthHttpApi]。
     */
    suspend fun loginWithHuaweiCode(code: String, deviceId: String): Result<Unit> {
        val authCode = code.trim()
        if (authCode.isEmpty()) {
            return Result.failure(IllegalArgumentException("缺少授权码"))
        }
        val id = deviceId.trim().ifBlank { AuthDeviceContext.deviceId() }
        return try {
            val response = NetworkFacade.api().postApi<AuthHuaweiLoginRequest, AuthLoginData>(
                path = AuthApiPaths.HUAWEI_LOGIN,
                body = AuthHuaweiLoginRequest(code = authCode, deviceId = id),
            )
            val data = response.data?.toPayload()
            if (!isAuthBusinessSuccess(response.code) || data == null || data.token.isBlank()) {
                return Result.failure(handleAuthFailure(response.code, null, response.message))
            }
            commitSession(
                displayName = data.username.ifBlank { "华为用户" },
                userId = data.userId.ifBlank { data.username }.ifBlank { "华为用户" },
                token = data.token,
                phone = data.phone.ifBlank { null },
                email = data.email.ifBlank { null },
                refreshToken = data.refreshToken,
                sessionId = data.sessionId,
                avatarUrl = data.avatarUrl.ifBlank { null },
            )
        } catch (e: NetworkError.Transport) {
            Result.failure(handleAuthFailure(null, e.httpStatus, e.message))
        } catch (e: Throwable) {
            Result.failure(handleAuthFailure(null, null, e.message))
        }
    }

    /**
     * 契约表 #7：微信授权码换会话（Flutter `BackendAuthService.signInWithWechatCode`，:74）。
     *
     * `// GAP(R5)`: 只有 **HTTP 契约**可用——授权码需原生微信 SDK
     * （iOS `WXApi` / Android `wechat-sdk-android` / OHOS 无对应能力）产出，
     * 取码桥尚未接入。接入前请勿把本方法当成「微信登录已就绪」。
     */
    suspend fun loginWithWechatCode(code: String, deviceId: String = ""): Result<Unit> {
        val authCode = code.trim()
        if (authCode.isEmpty()) {
            return Result.failure(IllegalArgumentException("缺少微信授权码"))
        }
        return UserAuthHttpApi.loginWithWechat(
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
        val session = AccountFacade.current()
        if (!session.isLoggedIn ||
            session.token.isBlank() ||
            session.sessionId.isBlank() ||
            session.deviceId.isBlank()
        ) {
            // 会话不完整（token 已废 / 缺 header）→ 不做远端登出，直接清本地。
            AuthSessionActions.logout()
            return Result.success(Unit)
        }
        return UserAuthHttpApi.logout().fold(
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
     * Flutter `isLogoutSessionGone`（`backend_auth_service.dart:255-275`）：
     * 服务端会话确实已废 / 无权限的失败——清本地是安全的。
     */
    private fun isLogoutSessionGone(error: Throwable): Boolean {
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

    /**
     * Flutter `SessionGuardHook._handleForcedLogout`（d7876a1）：互踢/会话无效/登录失效
     * 先清本地会话（token 已废，不再请求远端 logout），再交由调用方提示与跳登录。
     * 403（Forbidden）不走登出。
     */
    private fun handleAuthFailure(code: Int?, statusCode: Int?, message: String?): IllegalStateException {
        val failure = AuthFailure.fromBizCode(code)
        if (AuthBizCode.isSessionGone(code) || failure.isSessionGoneFailure()) {
            AccountFacade.logout()
            AuthSessionState.clearLocal()
            AuthGate.clearPending()
        }
        return IllegalStateException(mapAuthFailure(code, message, statusCode))
    }

    /** [handleAuthFailure] 的 `AuthHttpFailure` 版本：文案已由 [UserAuthHttpApi] 映射，只补会话清理。 */
    private fun clearSessionIfGone(error: Throwable): Throwable {
        val code = (error as? AuthHttpFailure)?.code
        if (code != null &&
            (AuthBizCode.isSessionGone(code) || AuthFailure.fromBizCode(code).isSessionGoneFailure())
        ) {
            AccountFacade.logout()
            AuthSessionState.clearLocal()
            AuthGate.clearPending()
        }
        return error
    }

    /**
     * 会话落库。[UserAuthHttpApi] 已保证成功分支的 `token` 非空（Dart `_requireData`
     * 对 `data == null` / 空 token 一律抛），这里只做展示名与 id 的兜底推导。
     */
    private fun Result<AuthLoginData>.commitLogin(): Result<Unit> = fold(
        onSuccess = { data ->
            val payload = data.toPayload()
            val display = payload.username.ifBlank {
                payload.email.substringBefore('@').ifBlank { payload.email }
            }.ifBlank { "用户" }
            commitSession(
                displayName = display,
                userId = payload.userId.ifBlank { display },
                token = payload.token,
                phone = payload.phone.ifBlank { null },
                email = payload.email.ifBlank { null },
                refreshToken = payload.refreshToken,
                sessionId = payload.sessionId,
                avatarUrl = payload.avatarUrl.ifBlank { null },
            )
        },
        onFailure = { Result.failure(clearSessionIfGone(it)) },
    )

    // ── Mock 分支（Flutter `commons/core/lib/dev/mock_auth_service.dart` 移植）─────────
    //
    // 只在 [MockAuthConfig.useMockAuth] 为 true 时进入；不触网、不落远端会话，
    // 目的仅是让 debug 包在后端未起时也能走通 UI 链路（首页/我的/登录态）。

    /** Flutter `MockAuthService.verifyPhoneOtp`：白名单 + 固定 OTP。 */
    private fun mockPhoneLogin(phone: String, code: String): Result<Unit> {
        val digits = AuthPhoneUtils.normalizeDigits(phone)
        if (digits !in MockAuthConfig.mockTestPhones) {
            return Result.failure(IllegalArgumentException(MockAuthConfig.rejectedPhoneMessage()))
        }
        if (code.trim() != MockAuthConfig.mockOtpCode) {
            return Result.failure(IllegalArgumentException("验证码错误或已失效"))
        }
        return commitSession(
            displayName = MockAuthConfig.displayNameFor(digits),
            userId = "mock_phone_$digits",
            token = MockAuthConfig.issueToken("phone"),
            phone = MockAuthConfig.maskedPhone(digits),
            email = null,
            avatarUrl = "https://picsum.photos/seed/im_$digits/200/200",
        )
    }

    /** Flutter `MockAuthService.signInWithEmail`：任意合法邮箱 + ≥6 位密码即建会话。 */
    private fun mockEmailLogin(account: String): Result<Unit> {
        val localPart = account.substringBefore('@').ifBlank { account }
        return commitSession(
            displayName = "用户$localPart",
            userId = "mock_$localPart",
            token = MockAuthConfig.issueToken("email"),
            phone = null,
            email = if (account.contains('@')) account else null,
            avatarUrl = "https://api.dicebear.com/7.x/avataaars/png?seed=$localPart",
        )
    }

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
    ): Result<Unit> {
        AccountFacade.setSession(
            LoggedInUser(
                displayName = displayName,
                token = token,
                refreshToken = refreshToken,
                sessionId = sessionId,
                deviceId = AuthDeviceContext.deviceId(),
                userId = userId,
                avatarUrl = avatarUrl,
                phone = phone,
                email = email,
                companyId = null,
                orgId = null,
            ),
        )
        AuthSessionState.sync()
        return Result.success(Unit)
    }
}

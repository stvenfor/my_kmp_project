package com.example.my_kmp_project.feature.auth

import com.example.my_kmp_project.component.auth.AuthSessionPayload
import com.example.my_kmp_project.component.auth.AuthSessionState
import com.example.my_kmp_project.component.auth.AuthorizationCodeProvider
import com.example.my_kmp_project.component.auth.AuthorizationCodeResult
import com.example.my_kmp_project.component.auth.DefaultLoginStrategyRegistry
import com.example.my_kmp_project.component.auth.LoginController
import com.example.my_kmp_project.component.auth.LoginCredentials
import com.example.my_kmp_project.component.auth.LoginError
import com.example.my_kmp_project.component.auth.LoginAvailability
import com.example.my_kmp_project.component.auth.LoginOutcome
import com.example.my_kmp_project.component.auth.LoginStrategy
import com.example.my_kmp_project.component.auth.LoginStrategyRegistry
import com.example.my_kmp_project.component.auth.OtpChallengeStrategy
import com.example.my_kmp_project.component.auth.PlatformLoginCapabilities
import com.example.my_kmp_project.core.network.platformClientType
import kotlinx.coroutines.CancellationException

/** 登录方式的稳定 id：UI、原生桥、开关与埋点共用同一份字面量。 */
public object LoginMethodIds {
    public const val PASSWORD: String = "password"
    public const val PHONE_OTP: String = "phone_otp"
    public const val HUAWEI: String = "huawei"
    public const val WECHAT: String = "wechat"
}

/**
 * 账号密码登录（契约表 #1）。
 *
 * 只做「校验 → 请求 → 会话载荷」，不写 [com.example.my_kmp_project.core.account.SessionRepository]；
 * 落库由 `LoginOrchestrator.establishSession` 统一处理。
 */
public class PasswordLoginStrategy(
    private val api: UserAuthHttpApi,
) : LoginStrategy {

    override val id: String = LoginMethodIds.PASSWORD

    override val displayName: String = "密码登录"

    override val order: Int = 10

    override suspend fun login(credentials: LoginCredentials): LoginOutcome {
        val input = credentials as? LoginCredentials.Password
            ?: return LoginOutcome.Failure(LoginError.InvalidInput("请使用账号密码登录"))

        val user = input.account.trim()
        if (user.isEmpty()) {
            return LoginOutcome.Failure(LoginError.InvalidInput("请输入账号"))
        }
        if (user.contains('@') && !AuthValidators.isValidEmail(user)) {
            return LoginOutcome.Failure(LoginError.InvalidInput("请输入有效的邮箱"))
        }
        if (input.password.length < AuthValidators.MIN_PASSWORD_LENGTH) {
            return LoginOutcome.Failure(LoginError.InvalidInput("密码至少 6 位"))
        }

        return api.login(
            username = user,
            password = input.password,
            deviceId = AuthDeviceContext.deviceId(),
            platform = AuthDeviceContext.platform(),
        ).toLoginOutcome()
    }
}

/**
 * 手机号验证码登录（契约表 #5 发送 + #6 校验）。
 *
 * [sendOtp] 单独暴露；登录页可把它作为「获取验证码」入口，而不必依赖 AuthRepository。
 */
public class PhoneOtpLoginStrategy(
    private val api: UserAuthHttpApi,
) : OtpChallengeStrategy {

    override val id: String = LoginMethodIds.PHONE_OTP

    override val displayName: String = "短信登录"

    override val order: Int = 20

    override suspend fun login(credentials: LoginCredentials): LoginOutcome {
        val input = credentials as? LoginCredentials.PhoneOtp
            ?: return LoginOutcome.Failure(LoginError.InvalidInput("请使用手机号验证码登录"))

        if (!AuthPhoneUtils.isValidChinaMobile(input.phone)) {
            return LoginOutcome.Failure(LoginError.InvalidInput("请输入有效的手机号"))
        }
        if (!AuthValidators.isValidOtp(input.code)) {
            return LoginOutcome.Failure(LoginError.InvalidInput("请输入 6 位验证码"))
        }

        return api.verifyPhoneOtp(
            phone = AuthPhoneUtils.toE164China(input.phone),
            otp = input.code.trim(),
            deviceId = AuthDeviceContext.deviceId(),
            platform = AuthDeviceContext.platform(),
        ).toLoginOutcome()
    }

    override suspend fun sendOtp(target: String): Result<Unit> {
        if (!AuthPhoneUtils.isValidChinaMobile(target)) {
            return Result.failure(IllegalArgumentException("请输入有效的手机号"))
        }
        return api.sendPhoneOtp(AuthPhoneUtils.toE164China(target))
    }
}

/**
 * 微信登录（契约表 #7）。
 *
 * 本批只做策略契约，不引入微信开放平台 SDK、不读取 appid：
 * - 取码走既有 [AuthorizationCodeProvider] 通道抽象；
 * - 未注册 provider（即凭据 / SDK 未配置）时 [availability] 如实返回不可用；
 * - 原生壳已自行取码时可直接传 [LoginCredentials.ExternalCode]。
 */
public class WechatLoginStrategy(
    private val api: UserAuthHttpApi,
    private val provider: AuthorizationCodeProvider? =
        PlatformLoginCapabilities.providerOrNull(LoginMethodIds.WECHAT),
) : LoginStrategy {

    override val id: String = LoginMethodIds.WECHAT

    override val displayName: String = "微信登录"

    override val interactive: Boolean = true

    override val order: Int = 40

    override fun availability(): LoginAvailability =
        if (provider != null) {
            LoginAvailability.Available
        } else {
            LoginAvailability.Unavailable("微信登录未配置授权通道（缺少 appId / SDK 适配器）")
        }

    override suspend fun login(credentials: LoginCredentials): LoginOutcome {
        val externalDeviceId: String
        val code: String = when (credentials) {
            is LoginCredentials.ExternalCode -> {
                externalDeviceId = credentials.deviceId
                credentials.code.trim()
            }
            LoginCredentials.Interactive -> {
                externalDeviceId = ""
                val channel = provider
                    ?: return LoginOutcome.Failure(
                        LoginError.Unavailable(id, "微信登录未配置授权通道"),
                    )
                when (val result = requestAuthorizationCode(channel)) {
                    is AuthorizationCodeResult.Success -> result.code.trim()
                    AuthorizationCodeResult.Cancelled -> return LoginOutcome.Cancelled
                    is AuthorizationCodeResult.Unavailable ->
                        return LoginOutcome.Failure(LoginError.Unavailable(id, result.reason))
                    is AuthorizationCodeResult.Failure ->
                        return LoginOutcome.Failure(LoginError.ProviderRejected(result.message))
                }
            }
            else -> return LoginOutcome.Failure(LoginError.InvalidInput("请使用微信授权登录"))
        }

        if (code.isEmpty()) {
            return LoginOutcome.Failure(LoginError.ProviderRejected("微信授权未返回授权码"))
        }

        val deviceId = externalDeviceId.trim().ifBlank { AuthDeviceContext.deviceId() }
        return api.loginWithWechat(
            code = code,
            deviceId = deviceId,
            platform = AuthDeviceContext.platform(),
        ).toLoginOutcome()
    }

    private suspend fun requestAuthorizationCode(
        channel: AuthorizationCodeProvider,
    ): AuthorizationCodeResult = try {
        channel.requestCode()
    } catch (e: CancellationException) {
        throw e
    } catch (t: Throwable) {
        AuthorizationCodeResult.Failure(t.message ?: "微信授权失败")
    }
}

/** 华为账号登录（鸿蒙 Account Kit）。
 *
 * 取码由 ArkTS `HuaweiQuickLoginPane` 完成，Kotlin 只负责用 code 换会话，
 * 因此仅接受 [LoginCredentials.ExternalCode]；非鸿蒙平台如实标记不可用。
 */
public class HuaweiLoginStrategy(
    private val api: UserAuthHttpApi,
    private val clientType: String = platformClientType(),
) : LoginStrategy {

    override val id: String = LoginMethodIds.HUAWEI

    override val displayName: String = "华为账号登录"

    override val interactive: Boolean = true

    override val order: Int = 30

    override fun availability(): LoginAvailability =
        if (isHarmonyClient(clientType)) {
            LoginAvailability.Available
        } else {
            LoginAvailability.Unavailable("仅鸿蒙设备支持华为账号登录")
        }

    override suspend fun login(credentials: LoginCredentials): LoginOutcome {
        val input = credentials as? LoginCredentials.ExternalCode
            ?: return LoginOutcome.Failure(
                LoginError.Unavailable(id, "请在鸿蒙登录页使用华为一键登录"),
            )
        val code = input.code.trim()
        if (code.isEmpty()) {
            return LoginOutcome.Failure(LoginError.InvalidInput("缺少华为授权码"))
        }
        val deviceId = input.deviceId.trim().ifBlank { AuthDeviceContext.deviceId() }
        return api.loginWithHuawei(code, deviceId)
            .toLoginOutcome(defaultDisplay = "华为用户")
    }
}

private fun isHarmonyClient(clientType: String): Boolean {
    val name = clientType.lowercase()
    return name.contains("ohos") || name.contains("harmony")
}

/**
 * feature/auth 内部的策略装配（过渡期）。
 *
 * 依赖仍来自 [AuthDependencies]；[com.example.my_kmp_project.app.di.AuthModule]
 * 是 app 层的显式装配入口，两者构建的是同一组策略、行为一致。
 */
internal object AuthStrategies {

    private val api: UserAuthHttpApi get() = AuthDependencies.api

    val password: PasswordLoginStrategy get() = PasswordLoginStrategy(api)

    val phoneOtp: PhoneOtpLoginStrategy get() = PhoneOtpLoginStrategy(api)

    val huawei: HuaweiLoginStrategy get() = HuaweiLoginStrategy(api)

    val wechat: WechatLoginStrategy get() = WechatLoginStrategy(api)

    val registry: LoginStrategyRegistry
        get() = DefaultLoginStrategyRegistry(listOf(password, phoneOtp, wechat, huawei))

    val controller: LoginController
        get() = LoginOrchestrator(
            registry = registry,
            repository = AuthDependencies.session,
            state = AuthSessionState.state(),
        )
}

/** 登录响应 → 统一会话载荷（展示名 / userId 兜底与旧 `commitLogin` 完全一致）。 */
internal fun AuthLoginData.toSessionPayload(defaultDisplay: String = "用户"): AuthSessionPayload {
    val payload = toPayload()
    val display = payload.username
        .ifBlank { payload.email.substringBefore('@').ifBlank { payload.email } }
        .ifBlank { defaultDisplay }
    return AuthSessionPayload(
        token = payload.token,
        refreshToken = payload.refreshToken,
        sessionId = payload.sessionId,
        userId = payload.userId.ifBlank { display },
        displayName = display,
        email = payload.email,
        phone = payload.phone,
        avatarUrl = payload.avatarUrl,
        deviceId = AuthDeviceContext.deviceId(),
    )
}

/** HTTP 结果 → 策略结果；失败文案沿用 [mapAuthFailure] 的输出。 */
internal fun Result<AuthLoginData>.toLoginOutcome(
    defaultDisplay: String = "用户",
): LoginOutcome = fold(
    onSuccess = { LoginOutcome.Success(it.toSessionPayload(defaultDisplay)) },
    onFailure = { LoginOutcome.Failure(it.toLoginError()) },
)

/**
 * 异常 → [LoginError]。
 *
 * `clearsSession` 只在互踢 / 会话失效 / 账号不可用时为 true，
 * 复用 [AuthRepository.shouldClearSessionFor] 的唯一判定，UI 不解析业务码。
 */
internal fun Throwable.toLoginError(): LoginError {
    if (this is CancellationException) throw this
    return when (this) {
        is LoginError -> this
        is AuthHttpFailure -> when {
            AuthRepository.shouldClearSessionFor(this) -> LoginError.SessionGone(message)
            isConnectionFailure(message) -> LoginError.Network(message)
            else -> LoginError.Server(message, code, statusCode)
        }
        is IllegalArgumentException -> LoginError.InvalidInput(message.orEmpty())
        else -> {
            val text = message.orEmpty()
            if (isConnectionFailure(text)) {
                LoginError.Network(text)
            } else {
                LoginError.Unknown(text.ifBlank { "登录失败，请稍后重试" })
            }
        }
    }
}

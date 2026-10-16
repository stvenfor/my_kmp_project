package com.example.my_kmp_project.feature.auth

import com.example.my_kmp_project.core.network.DemoApiHosts
import com.example.my_kmp_project.core.network.FlexibleIntSerializer
import com.example.my_kmp_project.core.network.NetworkCodes
import com.example.my_kmp_project.core.network.NetworkConfig
import com.example.my_kmp_project.getPlatform
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Flutter `UserAuthApi` path / body contracts (my_go_study).
 */
internal object AuthApiPaths {
    /** 契约表 #1（`user_auth_api.dart:7,22` loginPath）。 */
    const val LOGIN = "/api/v1/user/login"

    /** 契约表 #2（`user_auth_api.dart:8,141` registerPath）。 */
    const val REGISTER = "/api/v1/user/register"

    /** 契约表 #5（`user_auth_api.dart:11,38` sendPhoneOtpPath）。 */
    const val SEND_PHONE_OTP = "/api/v1/user/phone/otp/send"

    /** 契约表 #6（`user_auth_api.dart:12,54` verifyPhoneOtpPath）。 */
    const val VERIFY_PHONE_OTP = "/api/v1/user/phone/otp/verify"

    /** 契约表 #7（`user_auth_api.dart:13,74` wechatLoginPath）。 */
    const val WECHAT_LOGIN = "/api/v1/user/wechat/login"

    /** 本仓既有扩展端点（契约表外，非 Flutter d7876a1 调用点，保留不动）。 */
    const val HUAWEI_LOGIN = "/api/v1/user/huawei/login"

    /** 契约表 #4（`user_auth_api.dart:10,116` logoutPath）。 */
    const val LOGOUT = "/api/v1/user/logout"

    /** access token 静默续期（契约表 #3，`user_auth_api.dart:9,95` refreshPath）。 */
    const val REFRESH = "/api/v1/user/refresh"
}

/** 契约表 #1 请求体：`{username,password,device_id,platform}` 四键**总发**（`user_auth_api.dart:24-29`）。 */
@Serializable
internal data class AuthPasswordLoginRequest(
    val username: String,
    val password: String,
    @SerialName("device_id") val deviceId: String,
    val platform: String,
)

/** 契约表 #5 请求体：`{phone}`（E.164 由调用方归一，`user_auth_api.dart:40`）。 */
@Serializable
internal data class AuthPhoneOtpSendRequest(
    val phone: String,
)

/** 契约表 #6 请求体：`{phone,otp,device_id,platform}`（`user_auth_api.dart:56-61`）。 */
@Serializable
internal data class AuthPhoneOtpVerifyRequest(
    val phone: String,
    val otp: String,
    @SerialName("device_id") val deviceId: String,
    val platform: String,
)

/** 契约表 #2 请求体：`{username,password,email,device_id,platform}`（`user_auth_api.dart:143-149`）。 */
@Serializable
internal data class AuthRegisterRequest(
    val username: String,
    val password: String,
    val email: String,
    @SerialName("device_id") val deviceId: String,
    val platform: String,
)

/**
 * 契约表 #7 请求体：`{code,device_id,platform}`（`user_auth_api.dart:76-80`）。
 * 与 [AuthHuaweiLoginRequest] 同形状但 path/语义不同，勿复用。
 */
@Serializable
internal data class AuthWechatLoginRequest(
    val code: String,
    @SerialName("device_id") val deviceId: String,
    val platform: String,
)

@Serializable
internal data class AuthHuaweiLoginRequest(
    val code: String,
    @SerialName("device_id") val deviceId: String,
    val platform: String = "android",
)

/**
 * Flutter `BackendUser`（`user_auth_api.dart:434-482`；契约表 #1/#2/#6/#7 的 `data.user`）。
 *
 * 双键大小写混用全部接受，由 `resolved*` 取值函数按 Dart `??` 顺序择先：
 * `user_id|id`、`user_name|username|display_name`、`avatar_url|avatarUrl`。
 * 与 Dart 的差异（刻意为之）：Dart 只把 **null** 当缺失，这里把**空白串**也当缺失，
 * 以免 BFF 用 `""` 占位时丢掉另一个别名的值。
 * [status] 用 `core/network` 的 [FlexibleIntSerializer] 容忍数字与数字串（Dart `_statusOf`）。
 */
@Serializable
internal data class AuthUserDto(
    val id: String? = null,
    @SerialName("user_id") val userIdAlt: String? = null,
    val username: String? = null,
    @SerialName("user_name") val userNameAlt: String? = null,
    @SerialName("display_name") val displayNameAlt: String? = null,
    val email: String? = null,
    @Serializable(with = FlexibleIntSerializer::class)
    val status: Int? = null,
    val phone: String? = null,
    @SerialName("avatar_url") val avatarUrlSnake: String? = null,
    @SerialName("avatarUrl") val avatarUrlCamel: String? = null,
) {
    /** Dart `canonicalId`：`user_id` 优先，其次 `id`（:447-449）。 */
    val resolvedUserId: String
        get() = firstPresent(userIdAlt, id)

    /** Dart `BackendUser.id`：`id` 优先，否则回落 [resolvedUserId]（:457）。 */
    val resolvedId: String
        get() = firstPresent(id, userIdAlt)

    /** Dart `BackendUser.username`：`user_name` → `username` → `display_name`（:450-455,459）。 */
    val resolvedUsername: String
        get() = firstPresent(userNameAlt, username, displayNameAlt)

    /** Dart `BackendUser.userName`：`user_name` 优先，否则回落 [resolvedUsername]（:460）。 */
    val resolvedUserName: String
        get() = firstPresent(userNameAlt, resolvedUsername)

    val resolvedEmail: String get() = email?.trim().orEmpty()

    val resolvedPhone: String get() = phone?.trim().orEmpty()

    /** Dart `_statusOf`：缺省 / 非数字 → 0（:477-481）。 */
    val resolvedStatus: Int get() = status ?: 0

    /** Dart `avatar_url ?? avatarUrl`（:464）。 */
    val resolvedAvatarUrl: String
        get() = firstPresent(avatarUrlSnake, avatarUrlCamel)
}

/**
 * Flutter `LoginResult`（`user_auth_api.dart:348-373`；契约表 #1/#6/#7）。
 * `data.user` 缺失时 Dart 用 `const {}` 兜底 → 全空 [AuthUserDto]，此处以 `null` 表达同等语义。
 */
@Serializable
internal data class AuthLoginData(
    val token: String = "",
    @SerialName("refresh_token") val refreshToken: String = "",
    @SerialName("session_id") val sessionId: String = "",
    val user: AuthUserDto? = null,
)

/**
 * Flutter `RegisterResult`（`user_auth_api.dart:395-432`；契约表 #2）。
 *
 * **双形态**：`data` 带嵌套 `user` → 解析 `user`；不带 → 把整个 `data` 自身当
 * `BackendUser` 解析（:410-423）。因此本 DTO 同时声明根级用户字段，
 * 由 [resolvedUser] 归一为 [AuthUserDto]。空 map → 全 `''` 用户。
 */
@Serializable
internal data class AuthRegisterData(
    val token: String? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("session_id") val sessionId: String? = null,
    val user: AuthUserDto? = null,
    val id: String? = null,
    @SerialName("user_id") val userIdAlt: String? = null,
    val username: String? = null,
    @SerialName("user_name") val userNameAlt: String? = null,
    @SerialName("display_name") val displayNameAlt: String? = null,
    val email: String? = null,
    @Serializable(with = FlexibleIntSerializer::class)
    val status: Int? = null,
    val phone: String? = null,
    @SerialName("avatar_url") val avatarUrlSnake: String? = null,
    @SerialName("avatarUrl") val avatarUrlCamel: String? = null,
) {
    /** Dart `hasSession`：token 非空串（:431）。 */
    val hasSession: Boolean get() = !token.isNullOrEmpty()

    /** 双形态归一（:410-423）：优先嵌套 `user`，否则把根 `data` 当用户。 */
    fun resolvedUser(): AuthUserDto =
        user ?: AuthUserDto(
            id = id,
            userIdAlt = userIdAlt,
            username = username,
            userNameAlt = userNameAlt,
            displayNameAlt = displayNameAlt,
            email = email,
            status = status,
            phone = phone,
            avatarUrlSnake = avatarUrlSnake,
            avatarUrlCamel = avatarUrlCamel,
        )

    fun resolvedUserId(): String = resolvedUser().resolvedId

    fun resolvedCanonicalUserId(): String = resolvedUser().resolvedUserId

    fun resolvedUsername(): String = resolvedUser().resolvedUsername

    fun resolvedEmail(): String = resolvedUser().resolvedEmail

    fun resolvedPhone(): String = resolvedUser().resolvedPhone

    fun resolvedAvatarUrl(): String = resolvedUser().resolvedAvatarUrl

    fun resolvedStatus(): Int = resolvedUser().resolvedStatus
}

internal data class AuthLoginPayload(
    val token: String,
    val refreshToken: String = "",
    val sessionId: String = "",
    val userId: String = "",
    val username: String = "",
    val email: String = "",
    val phone: String = "",
    val avatarUrl: String = "",
    val status: Int = 0,
)

/**
 * 契约表 #3 请求体（`user_auth_api.dart:97-102`）：`refresh_token` 必填；
 * `device_id` / `session_id` / `platform` **null 或空串时整个 key 省略**。
 *
 * 实现：三键声明为 `String? = null` + `NetworkJson`（`explicitNulls = false`）
 * → null 不出现在 body 里；[of] 负责把空白串折成 null（调用方勿直接构造）。
 */
@Serializable
internal data class AuthRefreshRequest(
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("device_id") val deviceId: String? = null,
    @SerialName("session_id") val sessionId: String? = null,
    val platform: String? = null,
) {
    companion object {
        /** Dart `if (x != null && x.isNotEmpty) 'k': x` 的对等构造（空白串省略 key）。 */
        fun of(
            refreshToken: String,
            deviceId: String?,
            sessionId: String?,
            platform: String?,
        ): AuthRefreshRequest = AuthRefreshRequest(
            refreshToken = refreshToken,
            deviceId = deviceId?.takeIf { it.isNotBlank() },
            sessionId = sessionId?.takeIf { it.isNotBlank() },
            platform = platform?.takeIf { it.isNotBlank() },
        )
    }
}

/** Flutter `RefreshTokenResult`（`user_auth_api.dart:375-393`；契约表 #3）。 */
@Serializable
internal data class AuthRefreshData(
    val token: String = "",
    @SerialName("refresh_token") val refreshToken: String = "",
    @SerialName("session_id") val sessionId: String = "",
)

internal data class AuthRegisterPayload(
    val token: String?,
    val refreshToken: String?,
    val sessionId: String?,
    val userId: String,
    val username: String,
    val email: String,
    val phone: String = "",
    val avatarUrl: String = "",
    val status: Int = 0,
) {
    val hasSession: Boolean get() = !token.isNullOrEmpty()
}

/** 别名择先取值：返回第一个非空（trim 后）候选，全部缺失 → `""`（镜像 Dart `a ?? b ?? ''` + `_stringOf`）。 */
internal fun firstPresent(vararg candidates: String?): String {
    for (c in candidates) {
        val text = c?.trim().orEmpty()
        if (text.isNotEmpty()) return text
    }
    return ""
}

internal fun AuthLoginData.toPayload(): AuthLoginPayload {
    val u = resolvedUser()
    return AuthLoginPayload(
        token = token,
        refreshToken = refreshToken,
        sessionId = sessionId,
        userId = u.resolvedId,
        username = u.resolvedUsername,
        email = u.resolvedEmail,
        phone = u.resolvedPhone,
        avatarUrl = u.resolvedAvatarUrl,
        status = u.resolvedStatus,
    )
}

/** `data.user` 缺省时按 Dart `const {}` 兜底成空用户（:361-365）。 */
internal fun AuthLoginData.resolvedUser(): AuthUserDto =
    user ?: AuthUserDto()

internal fun AuthRegisterData.toPayload(): AuthRegisterPayload =
    AuthRegisterPayload(
        token = token?.takeIf { it.isNotEmpty() },
        refreshToken = refreshToken?.takeIf { it.isNotEmpty() },
        sessionId = sessionId?.takeIf { it.isNotEmpty() },
        userId = resolvedUserId(),
        username = resolvedUsername(),
        email = resolvedEmail(),
        phone = resolvedPhone(),
        avatarUrl = resolvedAvatarUrl(),
        status = resolvedStatus(),
    )

internal object AuthPhoneUtils {
    private val chinaMobile = Regex("^1[3-9]\\d{9}$")

    fun normalizeDigits(raw: String): String =
        raw.trim().filter { it.isDigit() }

    fun isValidChinaMobile(raw: String): Boolean =
        chinaMobile.matches(normalizeDigits(raw))

    /** Flutter [PhoneAuthUtils.toE164China]. */
    fun toE164China(raw: String): String {
        val digits = normalizeDigits(raw)
        return when {
            raw.trim().startsWith("+") -> raw.trim()
            digits.startsWith("86") && digits.length == 13 -> "+$digits"
            else -> "+86$digits"
        }
    }
}

internal object AuthValidators {
    private val emailRegex =
        Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")

    const val MIN_PASSWORD_LENGTH = 6

    fun isValidEmail(raw: String): Boolean = emailRegex.matches(raw.trim())

    fun isValidOtp(raw: String): Boolean = Regex("^\\d{6}$").matches(raw.trim())
}

internal object AuthDeviceContext {
    fun deviceId(): String = "kmp-${platform()}-device"

    fun platform(): String {
        val name = getPlatform().name.lowercase()
        return when {
            name.contains("android") -> "android"
            name.contains("ios") || name.contains("iphone") || name.contains("ipad") -> "ios"
            // Go LoginRequest currently oneof=android|ios — map OHOS to android for BFF.
            name.contains("harmony") || name.contains("ohos") -> "android"
            else -> "android"
        }
    }
}

/**
 * Maps Go / transport failures to Flutter-equivalent Chinese copy
 * (`UserAuthApi._mapFailure` / `AuthFailure`).
 *
 * Session decisions route through [classifyAuthHttp] first (Flutter d7876a1):
 * 10021/10022 → 互踢/会话无效；10023 或 401 → 登录已失效；HTTP 403 → 无权限（不登出）。
 */
internal fun mapAuthFailure(
    code: Int?,
    message: String?,
    statusCode: Int? = null,
): String {
    val text = message?.trim().orEmpty()
    val decision = classifyAuthHttp(
        statusCode = statusCode,
        code = code,
        message = text,
    )
    when (decision) {
        AuthHttpDecision.ForceLogout -> {
            return if (AuthBizCode.isSessionReplaced(code) || text.contains("其他设备登录")) {
                AuthFailure.SessionReplaced().message
            } else {
                AuthFailure.SessionInvalid().message
            }
        }
        AuthHttpDecision.TryRefresh -> return AuthFailure.SessionCleared().message
        AuthHttpDecision.Forbidden -> {
            // 403：权限问题，不走登出；但 10003 与「账号未注册」共用，先判注册语义。
            if (code == AuthBizCode.ACCOUNT_NOT_REGISTERED ||
                text.contains("账号未注册") ||
                text.contains("请先注册")
            ) {
                return "账号未注册，请先注册"
            }
            return "暂无权限"
        }
        AuthHttpDecision.None -> Unit
    }
    if (code == 10003 || text.contains("账号未注册") || text.contains("请先注册")) {
        return "账号未注册，请先注册"
    }
    if (code == 10002 ||
        text.contains("密码错误") ||
        text.contains("用户名或密码错误") ||
        text.contains("Unauthorized")
    ) {
        return "密码错误"
    }
    if (text.contains("验证邮件")) {
        return "注册成功，请查收验证邮件后再登录"
    }
    if (text.contains("验证码错误") || text.contains("验证码已失效")) {
        return "验证码错误或已失效"
    }
    if (text.contains("短信登录暂未开放")) {
        return text
    }
    if (text.contains("用户已存在")) {
        return "该邮箱已注册"
    }
    if (text.contains("参数错误") || text.contains("password", ignoreCase = true)) {
        return "密码至少 6 位"
    }
    if (text.contains("无法连接 Supabase") ||
        text.contains("Supabase 未配置") ||
        text.contains("认证服务暂时不可用")
    ) {
        return text.ifBlank {
            "认证服务暂时不可用，请检查后端配置（${NetworkConfig.effectiveBaseUrl()}）"
        }
    }
    if (code == 50000 || text.contains("服务器内部错误")) {
        return text.ifBlank { "服务端异常，请稍后重试" }
    }
    if (isConnectionFailure(text)) {
        val base = NetworkConfig.effectiveBaseUrl().ifBlank {
            DemoApiHosts.forEnvironment(NetworkConfig.netEnvironment)
        }
        return "无法连接服务端（$base），请确认后端已启动"
    }
    return text.ifBlank { "登录失败，请稍后重试" }
}

internal fun isAuthBusinessSuccess(code: Int): Boolean =
    code == 0 || code == NetworkCodes.OK

internal fun isConnectionFailure(text: String): Boolean {
    if (text.isBlank()) return false
    val keywords = listOf(
        "Connection refused",
        "Connection failed",
        "connection error",
        "connection timeout",
        "Network is unreachable",
        "Failed host lookup",
        "SocketException",
        "网络连接异常",
        "连接超时",
        "未知网络异常",
        "Software caused connection abort",
        "No route to host",
        "POST failed",
        "GET failed",
        "Unable to resolve host",
        "timed out",
    )
    return keywords.any { text.contains(it, ignoreCase = true) }
}

package com.example.my_kmp_project.feature.auth

import com.example.my_kmp_project.core.network.DemoApiHosts
import com.example.my_kmp_project.core.network.NetworkCodes
import com.example.my_kmp_project.core.network.NetworkConfig
import com.example.my_kmp_project.getPlatform
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * Flutter `UserAuthApi` path / body contracts (my_go_study).
 */
internal object AuthApiPaths {
    const val LOGIN = "/api/v1/user/login"
    const val REGISTER = "/api/v1/user/register"
    const val SEND_PHONE_OTP = "/api/v1/user/phone/otp/send"
    const val VERIFY_PHONE_OTP = "/api/v1/user/phone/otp/verify"
    const val LOGOUT = "/api/v1/user/logout"
}

internal data class AuthLoginPayload(
    val token: String,
    val refreshToken: String = "",
    val sessionId: String = "",
    val userId: String = "",
    val username: String = "",
    val email: String = "",
)

internal data class AuthRegisterPayload(
    val token: String?,
    val refreshToken: String?,
    val sessionId: String?,
    val userId: String,
    val username: String,
    val email: String,
) {
    val hasSession: Boolean get() = !token.isNullOrBlank()
}

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
 */
internal fun mapAuthFailure(code: Int?, message: String?): String {
    val text = message?.trim().orEmpty()
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

internal fun parseAuthLoginPayload(el: JsonElement?): AuthLoginPayload? {
    val obj = el as? JsonObject ?: return null
    val user = obj["user"] as? JsonObject
    return AuthLoginPayload(
        token = obj.string("token"),
        refreshToken = obj.string("refresh_token"),
        sessionId = obj.string("session_id"),
        userId = user?.string("id").orEmpty(),
        username = user?.string("username").orEmpty(),
        email = user?.string("email").orEmpty(),
    )
}

internal fun parseAuthRegisterPayload(el: JsonElement?): AuthRegisterPayload? {
    if (el == null) return null
    val obj = el as? JsonObject ?: return null
    val userObj = (obj["user"] as? JsonObject) ?: obj
    val token = obj.string("token").ifBlank { null }
    val refresh = obj.string("refresh_token").ifBlank { null }
    val session = obj.string("session_id").ifBlank { null }
    return AuthRegisterPayload(
        token = token,
        refreshToken = refresh,
        sessionId = session,
        userId = userObj.string("id"),
        username = userObj.string("username"),
        email = userObj.string("email"),
    )
}

private fun JsonObject.string(key: String): String =
    (this[key] as? JsonPrimitive)?.contentOrNull.orEmpty()

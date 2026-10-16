package com.example.my_kmp_project.feature.auth

/**
 * Flutter `AuthFailure` — UI 只处理本类型；底层（transport / BFF）映射进来。
 * 固定中文文案逐条对齐源仓 `auth_failure.dart`。
 */
internal sealed class AuthFailure(
    override val message: String,
) : Exception(message) {

    override fun toString(): String = message

    class InvalidCredentials : AuthFailure("密码错误")

    class AccountNotRegistered : AuthFailure("账号未注册，请先注册")

    class EmailAlreadyRegistered : AuthFailure("该邮箱已注册")

    class WeakPassword : AuthFailure("密码至少 6 位")

    class SignUpDisabled : AuthFailure("当前未开放注册")

    class DatabaseAuth : AuthFailure("用户资料初始化失败，请确认已执行 supabase/migrations SQL")

    class InvalidEmail : AuthFailure("请输入有效的邮箱")

    class InvalidPhone : AuthFailure("请输入有效的手机号")

    class InvalidOtp : AuthFailure("验证码错误或已失效")

    class EmailConfirmationRequired : AuthFailure("注册成功，请查收验证邮件后再登录")

    class OtpRateLimit : AuthFailure("发送过于频繁，请稍后再试")

    class NetworkAuth : AuthFailure("网络异常，请稍后重试")

    class BackendService : AuthFailure("认证服务暂时不可用，请稍后重试")

    class Unknown : AuthFailure("登录失败，请稍后重试")

    /** 10021 — 单设备互踢。 */
    class SessionReplaced : AuthFailure("账号已在其他设备登录，请重新登录")

    /** 10022 — 会话无效。 */
    class SessionInvalid : AuthFailure("会话无效，请重新登录")

    /** 守卫已清会话并将跳登录页；业务层不得再提示「token 无效」。 */
    class SessionCleared : AuthFailure("登录已失效，请重新登录")

    companion object {
        /** 从业务码映射（Flutter SessionGuardHook.handleIfForceLogout 的 code 推导）。 */
        fun fromBizCode(code: Int?): AuthFailure? = when (code) {
            AuthBizCode.SESSION_REPLACED -> SessionReplaced()
            AuthBizCode.SESSION_INVALID -> SessionInvalid()
            AuthBizCode.TOKEN_INVALID -> SessionCleared()
            else -> null
        }
    }
}

/** [AuthBizCode.isSessionGone] 等价语义：本地会话应视为已失效。 */
internal fun AuthFailure?.isSessionGoneFailure(): Boolean =
    this is AuthFailure.SessionReplaced ||
        this is AuthFailure.SessionInvalid ||
        this is AuthFailure.SessionCleared

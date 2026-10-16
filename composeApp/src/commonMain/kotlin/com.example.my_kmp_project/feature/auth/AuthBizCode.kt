package com.example.my_kmp_project.feature.auth

/**
 * Go BFF 业务码（与 my_go_study `response.Code*` 对齐；Flutter `AuthBizCode`）。
 *
 * 客户端分支只认 code；message 仅用于展示。文案匹配仅作旧包兼容兜底。
 */
internal object AuthBizCode {
    /**
     * 资料参数无效（契约表 #9 `PATCH /profiles/me`；Flutter `user_profile_api.dart:97`）。
     * 头像 base64 / mime / 体积不合法时服务端回此码，客户端只 toast、保留会话。
     */
    const val PROFILE_PARAM_INVALID = 10001

    /** 登录凭证错误（密码错误等）。 */
    const val UNAUTHORIZED = 10002

    /** 账号未注册（Go handler 现用 CodeForbidden=10003 表达此语义）。 */
    const val ACCOUNT_NOT_REGISTERED = 10003
    const val FORBIDDEN = ACCOUNT_NOT_REGISTERED

    /** 单设备：账号已在其他设备登录。 */
    const val SESSION_REPLACED = 10021

    /** 单设备：会话无效（缺 header / Redis 无会话 / 同设备 session 过期等）。 */
    const val SESSION_INVALID = 10022

    /** access token 无效 / 未带 Authorization；先 refresh，失败再清会话。 */
    const val TOKEN_INVALID = 10023

    /** 验证码错误或已失效。 */
    const val INVALID_OTP = 10024

    const val INTERNAL_ERROR = 50000

    fun isForceLogout(code: Int?): Boolean =
        code == SESSION_REPLACED || code == SESSION_INVALID

    fun isSessionReplaced(code: Int?): Boolean = code == SESSION_REPLACED

    /** token 废或单设备会话废：本地会话应视为已失效。 */
    fun isSessionGone(code: Int?): Boolean =
        isForceLogout(code) || code == TOKEN_INVALID

    /** 可尝试静默 refresh（非互踢、非纯密码错）。 */
    fun isTryRefresh(code: Int?): Boolean = code == TOKEN_INVALID
}

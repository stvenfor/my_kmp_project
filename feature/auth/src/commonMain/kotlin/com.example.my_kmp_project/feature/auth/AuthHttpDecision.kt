package com.example.my_kmp_project.feature.auth

/**
 * HTTP 鉴权决策（统一入口，避免各业务自行猜 401/403；Flutter `classifyAuthHttp`）。
 *
 * | 类别 | 典型条件 | 行为 |
 * |------|----------|------|
 * | [ForceLogout] | 业务码 10021/10022 | 弹窗 → 清会话 → 登录页 |
 * | [TryRefresh] | 10023 / HTTP 401（非互踢） | 静默续期，失败再 forceLogout |
 * | [AccountUnavailable] | HTTP 403 且 message 含「账号不可用」 | 清会话 → 登录页 |
 * | [Forbidden] | 其它 HTTP 403 | **不**登出，业务自行提示无权限 |
 * | [None] | 其它 | 不管会话 |
 *
 * 分支优先 [code]；[message] 仅旧包无 code 时兜底。
 */
public enum class AuthHttpDecision {
    ForceLogout,
    TryRefresh,
    AccountUnavailable,
    Forbidden,
    None,
}

/**
 * 统一分类。业务码优先于 HTTP status / 文案。
 *
 * 普通 HTTP 403 → [AuthHttpDecision.Forbidden]（不登出）；**仅**当 403 且 message
 * 含「账号不可用」→ [AuthHttpDecision.AccountUnavailable]（清会话回登录）。
 *
 * 后端 `middleware/require_active_account.go:29-31` 对已注销/停用账号回
 * `HTTP 403 + BackendError(c, 403, "账号不可用")`。该 403 的业务码是
 * `CodeForbidden = 10003`，而 10003 同时也是「账号未注册」（`AuthBizCode.ACCOUNT_NOT_REGISTERED`），
 * **有歧义**，因此这里只认 message，不按 `code==10003` 判定，避免把「账号未注册」误清会话。
 */
public fun classifyAuthHttp(
    statusCode: Int? = null,
    code: Int? = null,
    message: String = "",
): AuthHttpDecision {
    if (AuthBizCode.isForceLogout(code) ||
        (code == null &&
            (message.contains("其他设备登录") || message.contains("会话无效")))
    ) {
        return AuthHttpDecision.ForceLogout
    }
    if (statusCode == 403) {
        return if (message.contains("账号不可用")) {
            AuthHttpDecision.AccountUnavailable
        } else {
            AuthHttpDecision.Forbidden
        }
    }
    if (AuthBizCode.isTryRefresh(code) ||
        statusCode == 401 ||
        (code == null && looksLikeExpiredToken(message))
    ) {
        return AuthHttpDecision.TryRefresh
    }
    return AuthHttpDecision.None
}

/** 旧包无 code 时的文案兜底；新路径勿再依赖。 */
public fun looksLikeExpiredToken(message: String): Boolean {
    return message.contains("token 无效") ||
        message.contains("token 已过期") ||
        message.contains("JWT") ||
        message.contains("jwt") ||
        message.contains("token expired") ||
        message.contains("Token expired") ||
        message.contains("未提供 Authorization") ||
        message.contains("未授权")
}

/**
 * Realtime / hydrate 等：鉴权硬失败应停重试；普通 403 不在此列。
 * 账号不可用（403 + 指定文案）同样停重试——账号已废，重试无意义。
 */
public fun isAuthHttpHardFailure(
    statusCode: Int? = null,
    code: Int? = null,
    message: String = "",
): Boolean {
    val decision = classifyAuthHttp(
        statusCode = statusCode,
        code = code,
        message = message,
    )
    return decision == AuthHttpDecision.ForceLogout ||
        decision == AuthHttpDecision.TryRefresh ||
        decision == AuthHttpDecision.AccountUnavailable
}

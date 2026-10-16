package com.example.my_kmp_project.feature.auth

/**
 * HTTP 鉴权决策（统一入口，避免各业务自行猜 401/403；Flutter `classifyAuthHttp`）。
 *
 * | 类别 | 典型条件 | 行为 |
 * |------|----------|------|
 * | [ForceLogout] | 业务码 10021/10022 | 弹窗 → 清会话 → 登录页 |
 * | [TryRefresh] | 10023 / HTTP 401（非互踢） | 静默续期，失败再 forceLogout |
 * | [Forbidden] | HTTP 403 | **不**登出，业务自行提示无权限 |
 * | [None] | 其它 | 不管会话 |
 *
 * 分支优先 [code]；[message] 仅旧包无 code 时兜底。
 */
public enum class AuthHttpDecision { ForceLogout, TryRefresh, Forbidden, None }

/**
 * 统一分类。业务码优先于 HTTP status / 文案。
 * 仅 HTTP 403 → [AuthHttpDecision.Forbidden]：勿用 code==10003（与「账号未注册」共用）。
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
        return AuthHttpDecision.Forbidden
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
 * Realtime / hydrate 等：鉴权硬失败应停重试；403 不在此列。
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
        decision == AuthHttpDecision.TryRefresh
}

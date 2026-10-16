package com.example.my_kmp_project.component.realtime

import com.example.my_kmp_project.core.network.NetworkError

/**
 * Realtime 换票 / 建连的「鉴权硬失败」判定 ——
 * 对齐 Dart `components/realtime/lib/client/realtime_auth_failure.dart:7-20`（d7876a1）。
 *
 * 结论与 Dart 一致：**401 ⇒ 硬失败（停止退避循环）；403 ⇒ 非硬失败**（继续重连没有意义，
 * 但 403 属「无权限」而非「会话已废」，Dart 明确不据此清会话）。
 *
 * 分层说明：组件层不得依赖 `feature/` 层，因此这里**自带**一份与
 * `feature/auth/AuthHttpDecision.kt:21-54`（`classifyAuthHttp` / `looksLikeExpiredToken`）
 * 等价的判定逻辑，而不是 import 过去。两处规则若有变更必须同步——
 * 业务码字面量与 `feature/auth/AuthBizCode.kt` 逐一对得上：
 * 10021 / 10022 = 互踢、会话无效，10023 = token 无效。
 */

/** 可注入的判定缝隙：装配层可用自己那套（例如把 feature 的 classify 传进来）覆盖默认规则。 */
public fun interface RealtimeAuthHardFailurePolicy {
    public fun isHardFailure(error: Throwable): Boolean
}

/** 单点分类（纯函数，可脱离异常类型单测）。 */
public object RealtimeAuthHttpRules {

    /** 互踢 / 会话无效（AuthBizCode.SESSION_REPLACED=10021、SESSION_INVALID=10022）。 */
    public const val SESSION_REPLACED: Int = 10021
    public const val SESSION_INVALID: Int = 10022

    /** token 无效、可尝试静默续期（AuthBizCode.TOKEN_INVALID=10023）。 */
    public const val TOKEN_INVALID: Int = 10023

    /** 文案兜底关键词（AuthHttpDecision.kt:45-54）；新路径不应依赖，仅为等价复刻。 */
    private val EXPIRED_TOKEN_HINTS: List<String> = listOf(
        "token 无效",
        "token 已过期",
        "JWT",
        "jwt",
        "token expired",
        "Token expired",
        "未提供 Authorization",
        "未授权",
    )

    /** 互踢类文案（AuthHttpDecision.kt:28）。 */
    private val FORCE_LOGOUT_HINTS: List<String> = listOf("其他设备登录", "会话无效")

    public fun isForceLogout(code: Int?): Boolean =
        code == SESSION_REPLACED || code == SESSION_INVALID

    public fun isTryRefresh(code: Int?): Boolean = code == TOKEN_INVALID

    public fun looksLikeExpiredToken(message: String): Boolean =
        EXPIRED_TOKEN_HINTS.any { message.contains(it) }

    /**
     * Dart `isAuthHttpHardFailure`（AuthHttpDecision.kt:59-71）：
     * ForceLogout 或 TryRefresh ⇒ 硬失败；Forbidden / None ⇒ 否。
     */
    public fun isHardFailure(
        statusCode: Int? = null,
        code: Int? = null,
        message: String = "",
    ): Boolean {
        if (isForceLogout(code) ||
            (code == null && FORCE_LOGOUT_HINTS.any { message.contains(it) })
        ) {
            return true
        }
        if (statusCode == 403) return false
        return isTryRefresh(code) ||
            statusCode == 401 ||
            (code == null && looksLikeExpiredToken(message))
    }
}

/**
 * 默认实现：先看异常类型（对应 Dart 的 `is HttpRequestException` / `is StateError` 分支），
 * 落不到类型时退回「整条 message 文案判定」（Dart :19）。
 */
public object DefaultRealtimeAuthHardFailurePolicy : RealtimeAuthHardFailurePolicy {

    /**
     * `HttpRequestException` 在 KMP 侧的对应物是 [NetworkError]：
     * - [NetworkError.Business] 有业务码、无 HTTP status
     * - [NetworkError.Transport] 有 HTTP status（换票 401 会走这里，见 `KtorApiClient.postRaw`）
     * - [NetworkError.TokenExpired] 的 `code` 是遗留表 401，Dart 同一路径按 status=401 处理
     */
    override fun isHardFailure(error: Throwable): Boolean {
        if (error is NetworkError.Business) {
            return RealtimeAuthHttpRules.isHardFailure(
                code = error.code,
                message = error.message ?: "",
            )
        }
        if (error is NetworkError.Transport) {
            return RealtimeAuthHttpRules.isHardFailure(
                statusCode = error.httpStatus,
                message = error.message ?: "",
            )
        }
        if (error is NetworkError.TokenExpired) {
            // Dart：token 过期异常同样命中「硬失败 ⇒ 停止重连」，这里显式对齐。
            return true
        }
        if (error is IllegalStateException) {
            // Dart :15-18：StateError（「UserService 未注册」「未登录或 token 为空」）看文案。
            val m = error.toString()
            return m.contains("token") || m.contains("未登录")
        }
        return RealtimeAuthHttpRules.isHardFailure(message = error.toString())
    }
}

/** Dart 顶层函数 `isRealtimeAuthHardFailure` 的同名入口。 */
public fun isRealtimeAuthHardFailure(error: Throwable): Boolean =
    DefaultRealtimeAuthHardFailurePolicy.isHardFailure(error)

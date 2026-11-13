@file:Suppress("DEPRECATION")

package com.example.my_kmp_project.core.network

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement

/**
 * Shared `{ code, message, data }` envelope parser for Ktor and OHOS transport.
 *
 * Standard shape is decoded via [@Serializable] [ApiEnvelope]. Non-envelope bodies
 * (e.g. OAuth HTTP 401) use [Envelope Fallback] only — not a second business parser.
 *
 * [onError] 是 Go 后端五位数码（[BackendCodes]）的分类结果回调：这些码没有专属
 * `businessHandlers` 槽位，用它可以观察 [NetworkError.NotImplemented] / [NetworkError.Business]
 * 分类而不影响既有 `onTokenExpired` / `businessHandlers` 行为（缺省 null，无副作用）。
 */
public object ApiEnvelopeParser {
    fun <T> parse(
        raw: String,
        parseData: (JsonElement?) -> T?,
        onTokenExpired: TokenExpiredHandler? = null,
        businessHandlers: NetworkBusinessHandlers? = null,
        httpStatus: Int? = null,
        onError: ((NetworkError) -> Unit)? = null,
    ): ApiResponse<T> {
        val envelope = runCatching {
            NetworkJson.decodeFromString(ApiEnvelope.serializer(), raw)
        }.getOrNull()

        // Gateway HTTP 401 with OAuth-style body (no business code).
        if (httpStatus == NetworkCodes.EXPIRE_TOKEN) {
            val businessCode = envelope?.code
            if (businessCode == null) {
                val message = envelope?.oauthOrMessage()
                    ?: "已退出登录，请重新登录"
                val err = NetworkError.TokenExpired(message)
                onTokenExpired?.onTokenExpired(err)
                return ApiResponse(
                    code = NetworkCodes.EXPIRE_TOKEN,
                    message = message,
                    raw = raw,
                    httpStatus = httpStatus,
                    timestamp = envelope?.timestamp,
                )
            }
        }

        if (envelope == null) {
            return ApiResponse(
                // 非信封体（畸形 JSON / 静态页 / 网关 HTML）：Dart 缺 code 记 -1，不可当成功。
                code = httpStatus?.takeIf { it >= 400 } ?: NetworkCodes.PASSTHROUGH,
                message = "响应格式错误",
                raw = raw,
                httpStatus = httpStatus,
            )
        }

        val code = envelope.code
            ?: httpStatus?.takeIf { it >= 400 }
            ?: NetworkCodes.PASSTHROUGH
        val message = envelope.message?.takeIf { it.isNotBlank() }
            ?: envelope.oauthOrMessage()
        val dataElement = envelope.dataOrNull()

        when (code) {
            NetworkCodes.EXPIRE_TOKEN -> {
                val err = NetworkError.TokenExpired(message ?: "已退出登录，请重新登录")
                onTokenExpired?.onTokenExpired(err)
            }
            NetworkCodes.INVALID_AUTH -> {
                val err = NetworkError.Business(code, message ?: "认证无效")
                businessHandlers?.onInvalidAuth?.invoke(err)
            }
            NetworkCodes.INVALID_MEMBER -> {
                val err = NetworkError.Business(code, message ?: "会员无效")
                businessHandlers?.onInvalidMember?.invoke(err)
            }
            NetworkCodes.BUSY -> {
                val err = NetworkError.Business(code, message ?: "系统繁忙")
                businessHandlers?.onBusy?.invoke(err)
            }
            NetworkCodes.INVALID_ACCOUNT -> {
                val err = NetworkError.Business(code, message ?: "账号无效")
                businessHandlers?.onInvalidAccount?.invoke(err)
            }
            NetworkCodes.FORBIDDEN -> {
                // 403（业务码或 HTTP 回退）：无权限，不登出、不刷新（Flutter d7876a1）。
                // 仅 HTTP 403 / 遗留码 403 走这里；后端 code 10003 见下方显式分支。
                val err = NetworkError.Business(code, message ?: "无权限")
                businessHandlers?.onForbidden?.invoke(err)
            }

            // ---- Go 后端五位数码（response/common.go:12-29），由 [BackendCodes] 定义 ----
            BackendCodes.NOT_IMPLEMENTED -> {
                // 10005 能力未接入（HTTP 501）：不可重试 / 不刷新 / 不登出，
                // 客户端不得按成功或重试处理（Go common.go:18-20）。
                // 不触碰 onTokenExpired / onForbidden，也不进入任何刷新分支。
                onError?.invoke(NetworkError.NotImplemented(message ?: "能力未接入"))
            }
            BackendCodes.INVALID_PARAMS,
            BackendCodes.UNAUTHORIZED,
            BackendCodes.NOT_FOUND,
            BackendCodes.INVALID_OTP,
            BackendCodes.INTERNAL_ERROR -> {
                // 普通业务错误：只分类为 Business，既不触发 onTokenExpired（不登出 / 刷新）
                // 也不触发 onForbidden（无权限）。
                onError?.invoke(NetworkError.Business(code, message ?: "业务错误"))
            }
            BackendCodes.SESSION_REPLACED,
            BackendCodes.SESSION_INVALID,
            BackendCodes.TOKEN_INVALID -> {
                // 10021 / 10022 / 10023 是会话 / token 码：本层不重复决策，
                // 由 feature/auth 的 AuthRefreshingApiClient + classifyAuthHttp 装饰器统一
                // 判定 ForceLogout / Refresh。显式列出仅为分类完整，不调用任何 handler。
            }
            BackendCodes.FORBIDDEN -> {
                // 10003 后端 CodeForbidden 有歧义（KMP 侧也曾用于「账号未注册」）：
                // 只认 HTTP 403 判无权限，绝不在此按 code == 10003 触发 onForbidden。
            }
        }

        return ApiResponse(
            code = code,
            message = message,
            data = parseData(dataElement),
            raw = raw,
            httpStatus = httpStatus,
            timestamp = envelope.timestamp,
        )
    }

    private fun ApiEnvelope.oauthOrMessage(): String? =
        errorDescription?.takeIf { it.isNotBlank() }
            ?: error?.takeIf { it.isNotBlank() }
}

/** Decode `data` with the shared [NetworkJson]; null/blank → null. */
public inline fun <reified T> decodeEnvelopeData(el: JsonElement?): T? {
    if (el == null) return null
    return runCatching { NetworkJson.decodeFromJsonElement<T>(el) }.getOrNull()
}

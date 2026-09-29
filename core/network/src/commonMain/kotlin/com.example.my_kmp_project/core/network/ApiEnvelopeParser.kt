package com.example.my_kmp_project.core.network

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement

/**
 * Shared `{ code, message, data }` envelope parser for Ktor and OHOS transport.
 *
 * Standard shape is decoded via [@Serializable] [ApiEnvelope]. Non-envelope bodies
 * (e.g. OAuth HTTP 401) use [Envelope Fallback] only — not a second business parser.
 */
public object ApiEnvelopeParser {
    fun <T> parse(
        raw: String,
        parseData: (JsonElement?) -> T?,
        onTokenExpired: TokenExpiredHandler? = null,
        businessHandlers: NetworkBusinessHandlers? = null,
        httpStatus: Int? = null,
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
                )
            }
        }

        if (envelope == null) {
            return ApiResponse(
                code = httpStatus?.takeIf { it >= 400 } ?: NetworkCodes.NOT_NETWORK,
                message = "响应格式错误",
                raw = raw,
            )
        }

        val code = envelope.code
            ?: httpStatus?.takeIf { it >= 400 }
            ?: NetworkCodes.NOT_NETWORK
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
        }

        return ApiResponse(
            code = code,
            message = message,
            data = parseData(dataElement),
            raw = raw,
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

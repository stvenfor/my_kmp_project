package com.example.my_kmp_project.core.network

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * Optional business-code callbacks.
 * Defaults are no-op so existing UI is not interrupted.
 */
internal data class NetworkBusinessHandlers(
    val onInvalidAuth: ((NetworkError.Business) -> Unit)? = null,
    val onInvalidMember: ((NetworkError.Business) -> Unit)? = null,
    val onBusy: ((NetworkError.Business) -> Unit)? = null,
    val onInvalidAccount: ((NetworkError.Business) -> Unit)? = null,
)

/**
 * Shared `{ code, message, data }` envelope parser for Ktor and OHOS transport.
 * - business `code` may be int or string
 * - HTTP 401 without business code → [NetworkCodes.EXPIRE_TOKEN]
 */
internal object ApiEnvelopeParser {
    fun <T> parse(
        raw: String,
        parseData: (JsonElement?) -> T?,
        onTokenExpired: TokenExpiredHandler? = null,
        businessHandlers: NetworkBusinessHandlers? = null,
        httpStatus: Int? = null,
    ): ApiResponse<T> {
        val element = runCatching { NetworkJson.parseToJsonElement(raw) }.getOrNull()
        val obj = element as? JsonObject

        // Gateway HTTP 401 with OAuth-style body: {"error":"...","error_description":"..."}
        if (httpStatus == NetworkCodes.EXPIRE_TOKEN) {
            val businessCode = obj?.let { parseCode(it["code"]) }
            if (businessCode == null) {
                val message = obj?.let { oauthOrMessage(it) }
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

        if (obj == null) {
            return ApiResponse(
                code = httpStatus?.takeIf { it >= 400 } ?: NetworkCodes.NOT_NETWORK,
                message = "响应格式错误",
                raw = raw,
            )
        }

        val code = parseCode(obj["code"])
            ?: httpStatus?.takeIf { it >= 400 }
            ?: NetworkCodes.NOT_NETWORK
        val message = obj["message"]?.jsonPrimitive?.contentOrNull
            ?: oauthOrMessage(obj)
        val dataElement = obj["data"]?.takeUnless { it is JsonNull }

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

    private fun parseCode(el: JsonElement?): Int? {
        val p = el as? JsonPrimitive ?: return null
        p.intOrNull?.let { return it }
        return p.contentOrNull?.toIntOrNull()
    }

    private fun oauthOrMessage(obj: JsonObject): String? {
        obj["error_description"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
            ?.let { return it }
        return obj["error"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
    }
}

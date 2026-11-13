@file:Suppress("DEPRECATION")

package com.example.my_kmp_project.core.network

/**
 * Business envelope `{ code, message, data }`.
 *
 * [httpStatus] carries the HTTP status when the server answered with an error
 * (e.g. 401/403); null when the body had a business code or the transport failed.
 * 401/403 决策同时看 [code] 与 [httpStatus]（Flutter d7876a1 parity）。
 *
 * [timestamp] 是 Go `response.Response.Timestamp`（`common.go:36`，Unix 秒）；
 * 缺省 / 畸形体时为 null。
 */
public data class ApiResponse<T>(
    val code: Int,
    val message: String? = null,
    val data: T? = null,
    val raw: String? = null,
    val httpStatus: Int? = null,
    val timestamp: Long? = null,
) {
    /**
     * 只认 Go BFF 成功码 0（[NetworkCodes.BFF_OK]）。
     *
     * 遗留 `wys_network` 表用 200（[NetworkCodes.OK]）判成功，但 Go 后端从不返回业务码 200，
     * 故 200 不再参与判定（已标注 `@Deprecated` 保留兼容）。缺 `code` 的畸形响应由解析器
     * 回退 [NetworkCodes.PASSTHROUGH]（-1），同样不会误判成功。
     */
    val isSuccess: Boolean get() = code == NetworkCodes.BFF_OK
}

public sealed class NetworkError(
    message: String,
    val code: Int? = null,
) : Exception(message) {
    /** Transport failure; [httpStatus] carries the HTTP status when the server answered (e.g. 401/403). */
    class Transport(message: String, val httpStatus: Int? = null) : NetworkError(message)
    class Business(code: Int, message: String) : NetworkError(message, code)
    class TokenExpired(message: String = "已退出登录，请重新登录") :
        NetworkError(message, NetworkCodes.EXPIRE_TOKEN)

    /**
     * 后端「能力未接入」：Go `CodeNotImplemented`（[BackendCodes.NOT_IMPLEMENTED] = 10005，
     * HTTP 501）。`common.go:18-20` 明确「客户端不得按成功或重试处理」。
     *
     * [retryable] 恒为 false 锁死不可重试语义：不重试、不刷新 token、不登出，
     * 与 [TokenExpired]（刷新 / 登出）路径互斥。
     */
    class NotImplemented(
        message: String = "能力未接入",
        code: Int = BackendCodes.NOT_IMPLEMENTED,
    ) : NetworkError(message, code) {
        val retryable: Boolean = false
    }
}

public fun interface TokenExpiredHandler {
    fun onTokenExpired(error: NetworkError.TokenExpired)
}

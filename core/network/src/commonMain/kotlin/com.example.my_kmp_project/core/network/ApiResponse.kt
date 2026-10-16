package com.example.my_kmp_project.core.network

/**
 * Business envelope `{ code, message, data }`.
 *
 * [httpStatus] carries the HTTP status when the server answered with an error
 * (e.g. 401/403); null when the body had a business code or the transport failed.
 * 401/403 决策同时看 [code] 与 [httpStatus]（Flutter d7876a1 parity）。
 */
public data class ApiResponse<T>(
    val code: Int,
    val message: String? = null,
    val data: T? = null,
    val raw: String? = null,
    val httpStatus: Int? = null,
) {
    /**
     * Go BFF 成功码 0（Dart `ResultModel.isSuccess`）；遗留 `wys_network` 表用 200。
     * 缺 `code` 的畸形响应由解析器回退 -1，因此不会误判成功。
     */
    val isSuccess: Boolean get() = code == NetworkCodes.BFF_OK || code == NetworkCodes.OK
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
}

public fun interface TokenExpiredHandler {
    fun onTokenExpired(error: NetworkError.TokenExpired)
}

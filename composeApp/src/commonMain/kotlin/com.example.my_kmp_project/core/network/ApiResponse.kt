package com.example.my_kmp_project.core.network

/**
 * Business envelope `{ code, message, data }`.
 */
internal data class ApiResponse<T>(
    val code: Int,
    val message: String? = null,
    val data: T? = null,
    val raw: String? = null,
) {
    val isSuccess: Boolean get() = code == NetworkCodes.OK
}

internal sealed class NetworkError(
    message: String,
    val code: Int? = null,
) : Exception(message) {
    class Transport(message: String) : NetworkError(message)
    class Business(code: Int, message: String) : NetworkError(message, code)
    class TokenExpired(message: String = "已退出登录，请重新登录") :
        NetworkError(message, NetworkCodes.EXPIRE_TOKEN)
}

internal fun interface TokenExpiredHandler {
    fun onTokenExpired(error: NetworkError.TokenExpired)
}

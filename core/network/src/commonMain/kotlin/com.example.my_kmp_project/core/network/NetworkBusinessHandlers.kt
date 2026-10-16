package com.example.my_kmp_project.core.network

/**
 * Optional callbacks for non-token business error codes from the shared envelope parser.
 *
 * [onForbidden] mirrors Flutter `onForbidden` (d7876a1): 403 → 无权限提示，
 * 不登出、不刷新 token（与 onTokenExpired 互斥）。
 */
public data class NetworkBusinessHandlers(
    val onInvalidAuth: ((NetworkError.Business) -> Unit)? = null,
    val onInvalidMember: ((NetworkError.Business) -> Unit)? = null,
    val onBusy: ((NetworkError.Business) -> Unit)? = null,
    val onInvalidAccount: ((NetworkError.Business) -> Unit)? = null,
    val onForbidden: ((NetworkError.Business) -> Unit)? = null,
)

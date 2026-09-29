package com.example.my_kmp_project.core.network

/**
 * Optional callbacks for non-token business error codes from the shared envelope parser.
 */
public data class NetworkBusinessHandlers(
    val onInvalidAuth: ((NetworkError.Business) -> Unit)? = null,
    val onInvalidMember: ((NetworkError.Business) -> Unit)? = null,
    val onBusy: ((NetworkError.Business) -> Unit)? = null,
    val onInvalidAccount: ((NetworkError.Business) -> Unit)? = null,
)

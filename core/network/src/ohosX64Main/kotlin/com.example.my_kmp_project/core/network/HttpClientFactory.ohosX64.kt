package com.example.my_kmp_project.core.network

/**
 * ohosX64: CPF Ktor has no published ohosX64 variant — keep net_http cinterop.
 */
public actual fun createPlatformApiClient(
    onTokenExpired: TokenExpiredHandler?,
    businessHandlers: NetworkBusinessHandlers?,
): ApiClient = OhosApiClient(onTokenExpired, businessHandlers)

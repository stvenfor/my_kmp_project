package com.example.my_kmp_project.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

internal actual fun createPlatformApiClient(
    onTokenExpired: TokenExpiredHandler?,
    businessHandlers: NetworkBusinessHandlers?,
): ApiClient = KtorApiClient(
    http = HttpClient(Darwin) { configureDemoHttpClient() },
    onTokenExpired = onTokenExpired,
    businessHandlers = businessHandlers,
)

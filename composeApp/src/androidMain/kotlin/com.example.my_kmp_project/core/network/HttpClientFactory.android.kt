package com.example.my_kmp_project.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

internal actual fun createPlatformApiClient(
    onTokenExpired: TokenExpiredHandler?,
    businessHandlers: NetworkBusinessHandlers?,
): ApiClient = KtorApiClient(
    http = HttpClient(OkHttp) { configureDemoHttpClient() },
    onTokenExpired = onTokenExpired,
    businessHandlers = businessHandlers,
)

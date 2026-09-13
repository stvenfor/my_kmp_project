package com.example.my_kmp_project.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO

/** ohosArm64: CPF Ktor + CIO (HTTPS gate passed — cinterop retired on this target). */
public actual fun createPlatformApiClient(
    onTokenExpired: TokenExpiredHandler?,
    businessHandlers: NetworkBusinessHandlers?,
): ApiClient = KtorApiClient(
    http = HttpClient(CIO) { configureDemoHttpClient() },
    onTokenExpired = onTokenExpired,
    businessHandlers = businessHandlers,
)

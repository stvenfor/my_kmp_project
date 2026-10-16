package com.example.my_kmp_project.core.network.realtime

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO

/**
 * ohosArm64: CPF Ktor + CIO（与 `HttpClientFactory.ohosArm64` 同引擎）。
 * CPF `ktor-client-websockets:3.3.3-0.3.0` 有 ohosArm64 klib，故真链路可用。
 */
public actual fun createPlatformRealtimeWsTransport(): RealtimeWsTransport =
    KtorRealtimeWsTransport(HttpClient(CIO) { installRealtimeWebSockets() })

package com.example.my_kmp_project.core.network.realtime

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

/** android: Ktor + OkHttp 引擎的 WS 传输（与 `HttpClientFactory.android` 同引擎）。 */
public actual fun createPlatformRealtimeWsTransport(): RealtimeWsTransport =
    KtorRealtimeWsTransport(HttpClient(OkHttp) { installRealtimeWebSockets() })

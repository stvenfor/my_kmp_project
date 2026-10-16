package com.example.my_kmp_project.core.network.realtime

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

/** ios*: Ktor + Darwin 引擎的 WS 传输（与 `HttpClientFactory.ios` 同引擎）。 */
public actual fun createPlatformRealtimeWsTransport(): RealtimeWsTransport =
    KtorRealtimeWsTransport(HttpClient(Darwin) { installRealtimeWebSockets() })

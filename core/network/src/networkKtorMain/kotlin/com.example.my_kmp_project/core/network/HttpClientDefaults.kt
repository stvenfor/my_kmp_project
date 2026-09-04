package com.example.my_kmp_project.core.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json

private val DemoDynamicHeaders = createClientPlugin("DemoDynamicHeaders") {
    onRequest { request, _ ->
        val urlStr = request.url.buildString()
        DemoRequestHeaders.forUrl(urlStr).forEach { (name, value) ->
            request.headers.append(name, value)
        }
    }
}

public fun HttpClientConfig<*>.installDemoDefaults() {
    install(ContentNegotiation) {
        json(NetworkJson)
    }
    install(HttpTimeout) {
        connectTimeoutMillis = NetworkConfig.TIMEOUT_MS
        requestTimeoutMillis = NetworkConfig.TIMEOUT_MS
        socketTimeoutMillis = NetworkConfig.TIMEOUT_MS
    }
    install(Logging) {
        level = LogLevel.INFO
        logger = object : Logger {
            override fun log(message: String) {
                println("[Ktor] $message")
            }
        }
    }
    install(DemoDynamicHeaders)
    defaultRequest {
        url(NetworkConfig.effectiveBaseUrl().trimEnd('/') + "/")
        contentType(ContentType.Application.Json)
    }
}

public fun HttpClientConfig<*>.configureDemoHttpClient() {
    installDemoDefaults()
}

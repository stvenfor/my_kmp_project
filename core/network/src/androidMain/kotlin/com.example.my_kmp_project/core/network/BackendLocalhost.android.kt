package com.example.my_kmp_project.core.network

public actual fun remapBackendLocalhost(baseUrl: String): String {
    val uri = runCatching { java.net.URI(baseUrl) }.getOrNull() ?: return baseUrl
    val host = uri.host ?: return baseUrl
    if (host != "127.0.0.1" && host != "localhost") return baseUrl
    return java.net.URI(
        uri.scheme,
        uri.userInfo,
        "10.0.2.2",
        uri.port,
        uri.path,
        uri.query,
        uri.fragment,
    ).toString()
}

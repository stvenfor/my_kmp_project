package com.example.my_kmp_project.core.network

/** Shared request headers for demo HTTP clients. */
internal object DemoRequestHeaders {
    fun forUrl(url: String): Map<String, String> = buildMap {
        put("Client-Type", NetworkConfig.clientType)
        NetworkConfig.clientVersion?.let { put("Client-Version", it) }
        NetworkConfig.clientBuildNumber?.let { put("Client-BuildNumber", it) }
        val token = NetworkConfig.accessToken
        if (token.isNotBlank() && !NetworkConfig.skipAuthForUrl(url)) {
            put("Authorization", "Bearer $token")
        }
    }
}

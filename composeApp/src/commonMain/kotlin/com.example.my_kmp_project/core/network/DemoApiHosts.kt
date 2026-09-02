package com.example.my_kmp_project.core.network

internal enum class BuildMode {
    Debug,
    Release,
}

internal enum class NetEnvironment {
    Dev,
    Test,
    Staging,
    Product,
    Custom,
}

/** @deprecated Use [BuildMode]. */
internal typealias AppEnv = BuildMode

/** Placeholder API hosts for the demo skeleton (no product backends). */
internal object DemoApiHosts {
    const val DEV = "https://httpbin.org"
    const val TEST = "https://httpbin.org"
    const val PRODUCT = "https://httpbin.org"
    const val STAGING = "https://httpbin.org"

    /** Absolute CDN-style hosts that skip Authorization (must NOT match API host). */
    const val AUTH_SKIP_HOST = "cdn.demo.local"

    val debugSwitchHosts: List<Pair<String, String>> = listOf(
        "DEV" to DEV,
        "TEST" to TEST,
        "STAGING" to STAGING,
        "PRODUCTION" to PRODUCT,
    )

    fun forEnvironment(env: NetEnvironment, customUrl: String = ""): String = when (env) {
        NetEnvironment.Dev -> DEV
        NetEnvironment.Test -> TEST
        NetEnvironment.Staging -> STAGING
        NetEnvironment.Product -> PRODUCT
        NetEnvironment.Custom -> customUrl.ifBlank { TEST }
    }

    fun environmentForHost(url: String): NetEnvironment {
        val normalized = url.trim().trimEnd('/')
        return when (normalized) {
            DEV.trimEnd('/') -> NetEnvironment.Dev
            TEST.trimEnd('/') -> NetEnvironment.Test
            STAGING.trimEnd('/') -> NetEnvironment.Staging
            PRODUCT.trimEnd('/') -> NetEnvironment.Product
            else -> if (normalized.isBlank()) NetEnvironment.Test else NetEnvironment.Custom
        }
    }

    fun labelForBaseUrl(url: String): String {
        val normalized = url.trim().trimEnd('/')
        return debugSwitchHosts.firstOrNull { it.second.trimEnd('/') == normalized }?.first
            ?: normalized.ifBlank { "DEFAULT" }
    }
}

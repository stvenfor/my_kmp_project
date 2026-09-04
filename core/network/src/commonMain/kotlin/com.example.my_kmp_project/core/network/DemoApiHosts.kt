package com.example.my_kmp_project.core.network

public enum class BuildMode {
    Debug,
    Release,
}

public enum class NetEnvironment {
    Dev,
    Test,
    Staging,
    Product,
    Custom,
}

/** @deprecated Use [BuildMode]. */
public typealias AppEnv = BuildMode

/**
 * API hosts aligned with Flutter [EnvConfig] / my_go_study BFF.
 * Local debug: `http://127.0.0.1:8080` (Android emulator remapped via [remapBackendLocalhost]).
 */
public object DemoApiHosts {
    const val DEV = "http://127.0.0.1:8080"
    const val TEST = "http://127.0.0.1:8080"
    const val STAGING = "http://127.0.0.1:8080"
    const val PRODUCT = "https://api.xiaomaomain.com"

    /** Absolute CDN-style hosts that skip Authorization (must NOT match API host). */
    const val AUTH_SKIP_HOST = "cdn.demo.local"

    val debugSwitchHosts: List<Pair<String, String>> = listOf(
        "GO_LOCAL" to TEST,
        "GO_EMU" to "http://10.0.2.2:8080",
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
            DEV.trimEnd('/'),
            "http://10.0.2.2:8080",
            -> NetEnvironment.Test
            STAGING.trimEnd('/') -> NetEnvironment.Staging
            PRODUCT.trimEnd('/') -> NetEnvironment.Product
            else -> if (normalized.isBlank()) NetEnvironment.Test else NetEnvironment.Custom
        }
    }

    fun labelForBaseUrl(url: String): String {
        val normalized = url.trim().trimEnd('/')
        return debugSwitchHosts.firstOrNull { it.second.trimEnd('/') == normalized }?.first
            ?: when (normalized) {
                "http://127.0.0.1:8080", "http://10.0.2.2:8080" -> "GO_LOCAL"
                else -> normalized.ifBlank { "DEFAULT" }
            }
    }
}

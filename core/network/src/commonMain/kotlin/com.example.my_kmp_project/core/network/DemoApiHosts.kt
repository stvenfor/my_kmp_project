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
 *
 * Values come from the generated [BuildEnvironment] (source of truth:
 * `gradle/app.env.properties` + `-Papp.hosts.*`), so a build can target
 * dev / test / staging / prod without editing Kotlin.
 * Local debug: `http://127.0.0.1:8080` (Android emulator remapped via [remapBackendLocalhost]).
 */
public object DemoApiHosts {
    public const val DEV: String = BuildEnvironment.API_DEV
    public const val TEST: String = BuildEnvironment.API_TEST
    public const val STAGING: String = BuildEnvironment.API_STAGING
    public const val PRODUCT: String = BuildEnvironment.API_PROD

    /** Absolute CDN-style hosts that skip Authorization (must NOT match API host). */
    public const val AUTH_SKIP_HOST: String = "cdn.demo.local"

    public val debugSwitchHosts: List<Pair<String, String>> = listOf(
        "GO_LOCAL" to DEV,
        "GO_EMU" to "http://10.0.2.2:8080",
        "PRODUCTION" to PRODUCT,
    )

    public fun forEnvironment(env: NetEnvironment, customUrl: String = ""): String = when (env) {
        NetEnvironment.Dev -> DEV
        NetEnvironment.Test -> TEST
        NetEnvironment.Staging -> STAGING
        NetEnvironment.Product -> PRODUCT
        NetEnvironment.Custom -> customUrl.ifBlank { TEST }
    }

    public fun environmentForHost(url: String): NetEnvironment {
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

    public fun labelForBaseUrl(url: String): String {
        val normalized = url.trim().trimEnd('/')
        return debugSwitchHosts.firstOrNull { it.second.trimEnd('/') == normalized }?.first
            ?: when (normalized) {
                DEV.trimEnd('/'), TEST.trimEnd('/'), "http://10.0.2.2:8080" -> "GO_LOCAL"
                STAGING.trimEnd('/') -> "STAGING"
                else -> normalized.ifBlank { "DEFAULT" }
            }
    }
}

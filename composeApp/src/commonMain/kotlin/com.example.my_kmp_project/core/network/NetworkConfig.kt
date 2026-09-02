package com.example.my_kmp_project.core.network

import kotlin.concurrent.Volatile

/** Mutable network session config for the demo skeleton. */
internal object NetworkConfig {
    const val TIMEOUT_MS: Long = 30_000L

    @Volatile
    var buildMode: BuildMode = BuildMode.Debug

    @Volatile
    var netEnvironment: NetEnvironment = NetEnvironment.Test

    @Volatile
    var baseUrlOverride: String = ""

    @Volatile
    var accessToken: String = ""

    @Volatile
    var clientType: String = "android"

    @Volatile
    var clientVersion: String? = null

    @Volatile
    var clientBuildNumber: String? = null

    fun effectiveBaseUrl(): String {
        if (buildMode == BuildMode.Release) {
            return DemoApiHosts.PRODUCT.trimEnd('/')
        }
        val override = baseUrlOverride.trim().trimEnd('/')
        if (override.isNotEmpty()) return override
        return DemoApiHosts.forEnvironment(netEnvironment, baseUrlOverride).trimEnd('/')
    }

    fun skipAuthForUrl(url: String): Boolean =
        url.contains(DemoApiHosts.AUTH_SKIP_HOST, ignoreCase = true)

    fun resetClient() {
        NetworkFacade.rebuildClient()
    }

    fun applyEnvironment(env: NetEnvironment, customUrl: String? = null): String {
        netEnvironment = env
        baseUrlOverride = when (env) {
            NetEnvironment.Custom -> (customUrl ?: "").trim().trimEnd('/')
            else -> ""
        }
        resetClient()
        return DemoApiHosts.labelForBaseUrl(effectiveBaseUrl())
    }

    fun switchDebugHost(label: String, hostUrl: String): String {
        val url = hostUrl.trim().trimEnd('/')
        val env = DemoApiHosts.environmentForHost(url)
        netEnvironment = env
        baseUrlOverride = if (env == NetEnvironment.Custom) url else ""
        resetClient()
        return label.ifBlank { DemoApiHosts.labelForBaseUrl(effectiveBaseUrl()) }
    }
}

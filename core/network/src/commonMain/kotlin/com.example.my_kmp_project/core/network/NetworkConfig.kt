package com.example.my_kmp_project.core.network

import kotlin.concurrent.Volatile

/** Mutable network session config for the demo skeleton. */
public object NetworkConfig {
    const val TIMEOUT_MS: Long = 30_000L

    @Volatile
    var buildMode: BuildMode = BuildMode.Debug

    @Volatile
    var netEnvironment: NetEnvironment = NetEnvironment.Test

    @Volatile
    var baseUrlOverride: String = ""

    /**
     * 真机局域网后端 host 注入（Flutter `--dart-define=BACKEND_HOST` 等价通道）。
     * 机器 IP 不入库（[LanHost]）；运行时注入，仅重写 loopback host，不动非 loopback 地址。
     */
    @Volatile
    var backendHostOverride: String = ""

    /** 注入优先；[LanHost.fallback] 恒为空，仅为历史兼容保留。 */
    val effectiveBackendHost: String
        get() = backendHostOverride.trim().ifEmpty { LanHost.fallback }

    @Volatile
    var accessToken: String = ""

    /** 单设备会话 id；[DemoRequestHeaders.HEADER_SESSION_ID] 逐请求透传。 */
    @Volatile
    var sessionId: String = ""

    /** 登录设备 id；[DemoRequestHeaders.HEADER_DEVICE_ID] 逐请求透传。 */
    @Volatile
    var deviceId: String = ""

    @Volatile
    var clientType: String = "android"

    @Volatile
    var clientVersion: String? = null

    @Volatile
    var clientBuildNumber: String? = null

    fun effectiveBaseUrl(): String {
        if (buildMode == BuildMode.Release) {
            return remapBackendLocalhost(DemoApiHosts.PRODUCT.trimEnd('/'))
        }
        val override = baseUrlOverride.trim().trimEnd('/')
        val raw = if (override.isNotEmpty()) {
            override
        } else {
            DemoApiHosts.forEnvironment(netEnvironment, baseUrlOverride).trimEnd('/')
        }
        return remapBackendLocalhost(raw)
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

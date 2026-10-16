package com.example.my_kmp_project.core.network

/**
 * 鸿蒙：模拟器专用 10.0.2.2 不适用；loopback 直达或显式注入
 * [NetworkConfig.backendHostOverride]（Flutter 同语义：注入优先）。
 */
public actual fun remapBackendLocalhost(baseUrl: String): String {
    val mapped = resolveLoopbackHostMapping(NetworkConfig.effectiveBackendHost, isAndroidEmulator = false)
    return if (mapped != null) replaceLocalhostHost(baseUrl, mapped) else baseUrl
}

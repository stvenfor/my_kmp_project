package com.example.my_kmp_project.core.network

/**
 * iOS 模拟器 / macOS 桌面端与后端同机，loopback 直达（Flutter 同语义）；
 * 真机联调必须显式注入 [NetworkConfig.backendHostOverride]。
 */
public actual fun remapBackendLocalhost(baseUrl: String): String {
    val mapped = resolveLoopbackHostMapping(NetworkConfig.effectiveBackendHost, isAndroidEmulator = false)
    return if (mapped != null) replaceLocalhostHost(baseUrl, mapped) else baseUrl
}

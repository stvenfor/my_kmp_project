package com.example.my_kmp_project.core.network

import com.example.my_kmp_project.core.log.AppLog

public actual fun remapBackendLocalhost(baseUrl: String): String {
    val emulator = runCatching { isEmulator() }.getOrDefault(false)
    val mapped = resolveLoopbackHostMapping(NetworkConfig.effectiveBackendHost, emulator)
    if (mapped != null) return replaceLocalhostHost(baseUrl, mapped)
    warnRealDeviceWithoutOverride(baseUrl)
    return baseUrl
}

private fun isEmulator(): Boolean {
    val fingerprint = android.os.Build.FINGERPRINT.lowercase()
    val model = android.os.Build.MODEL.lowercase()
    return fingerprint.contains("generic") ||
        fingerprint.contains("emulator") ||
        model.contains("sdk") ||
        model.contains("emulator")
}

private var warnedRealDevice = false

/** 真机未注入 BACKEND host：127.0.0.1 指向设备自身，连不上本机后端（Flutter 同款告警，不阻断）。 */
private fun warnRealDeviceWithoutOverride(baseUrl: String) {
    if (warnedRealDevice) return
    if (!isLoopbackUrl(baseUrl)) return
    warnedRealDevice = true
    AppLog.w(
        "BackendHttpConfig",
        "真机上未注入后端 host：127.0.0.1 指向设备自身，连不上本机后端。" +
            "请在 设置 → 运行环境 自定义地址注入，或用局域网后端地址。",
    )
}

private fun isLoopbackUrl(baseUrl: String): Boolean {
    return baseUrl.startsWith("http://127.0.0.1") ||
        baseUrl.startsWith("http://localhost") ||
        baseUrl.startsWith("ws://127.0.0.1") ||
        baseUrl.startsWith("ws://localhost")
}

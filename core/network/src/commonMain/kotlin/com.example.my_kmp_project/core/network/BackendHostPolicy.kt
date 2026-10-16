package com.example.my_kmp_project.core.network

/**
 * Loopback host 映射策略 —— Flutter `BackendHttpConfig.remapLocalhostForPlatform`（10654a9）。
 *
 * 把配置里的 `127.0.0.1` / `localhost` 改写成「当前设备真正能访问到宿主机的地址」：
 *
 * | 场景 | 结果 |
 * |---|---|
 * | 显式注入 [overrideHost] | 用它 —— 真机联调的唯一通道 |
 * | Android 模拟器（未注入） | `10.0.2.2`（宿主 loopback 的别名） |
 * | iOS 模拟器 / macOS 桌面端 | 不改 —— 与后端同机，loopback 直达 |
 *
 * 返回 null = 保持原样。真机且未注入的场景由平台 actual 负责告警一次并原样返回。
 */
public fun resolveLoopbackHostMapping(
    overrideHost: String,
    isAndroidEmulator: Boolean,
): String? {
    val host = overrideHost.trim()
    if (host.isNotEmpty()) return host
    if (isAndroidEmulator) return "10.0.2.2"
    return null
}

/** 仅这两个 host 被视为 loopback（大小写不敏感）。 */
private val loopbackHosts = setOf("127.0.0.1", "localhost")

/**
 * 把 URL 里的 loopback host（127.0.0.1 / localhost）替换为 [host]，
 * 保留 scheme、端口、路径；非 loopback host 原样返回（Flutter 10654a9 parity）。
 */
public fun replaceLocalhostHost(baseUrl: String, host: String): String {
    val replacement = host.trim()
    if (replacement.isEmpty()) return baseUrl
    val schemeEnd = baseUrl.indexOf("://")
    if (schemeEnd < 0) return baseUrl
    val scheme = baseUrl.substring(0, schemeEnd)
    val rest = baseUrl.substring(schemeEnd + 3)
    val authorityEnd = rest.indexOfFirst { it == '/' || it == '?' || it == '#' }
    val authority = if (authorityEnd < 0) rest else rest.substring(0, authorityEnd)
    val tail = if (authorityEnd < 0) "" else rest.substring(authorityEnd)
    val colon = authority.indexOf(':')
    val hostPart = if (colon < 0) authority else authority.substring(0, colon)
    val portSuffix = if (colon < 0) "" else authority.substring(colon)
    if (hostPart.lowercase() !in loopbackHosts) return baseUrl
    return "$scheme://$replacement$portSuffix$tail"
}

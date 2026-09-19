package com.example.my_kmp_project.core.network

/** 真机走 [LanHost.fallback]（sync-lan-ip 写入）；模拟器专用 10.0.2.2 不用于鸿蒙。 */
public actual fun remapBackendLocalhost(baseUrl: String): String =
    replaceLocalhostHost(baseUrl, LanHost.fallback)

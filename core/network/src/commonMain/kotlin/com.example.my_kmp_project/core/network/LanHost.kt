package com.example.my_kmp_project.core.network

/**
 * 真机局域网回退地址。由 my_go_study `scripts/sync-lan-ip.sh` 按当前网卡写入。
 */
public object LanHost {
    public const val fallback: String = "192.168.0.102"
}

public fun replaceLocalhostHost(baseUrl: String, mappedHost: String): String {
    val prefixes = listOf(
        "http://127.0.0.1",
        "https://127.0.0.1",
        "ws://127.0.0.1",
        "wss://127.0.0.1",
        "http://localhost",
        "https://localhost",
        "ws://localhost",
        "wss://localhost",
    )
    for (prefix in prefixes) {
        if (baseUrl.startsWith(prefix)) {
            val scheme = prefix.substringBefore("://")
            return "$scheme://$mappedHost" + baseUrl.removePrefix(prefix)
        }
    }
    return baseUrl
}

package com.example.my_kmp_project.core.network

/** OHOS device uses LAN IP via debug host switch; do not map to 10.0.2.2. */
public actual fun remapBackendLocalhost(baseUrl: String): String = baseUrl

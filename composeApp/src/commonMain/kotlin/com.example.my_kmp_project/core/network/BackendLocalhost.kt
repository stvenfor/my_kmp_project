package com.example.my_kmp_project.core.network

/**
 * Remap loopback hosts for platform reachability (Flutter BackendHttpConfig parity).
 * Android emulator: 127.0.0.1 / localhost → 10.0.2.2
 */
internal expect fun remapBackendLocalhost(baseUrl: String): String

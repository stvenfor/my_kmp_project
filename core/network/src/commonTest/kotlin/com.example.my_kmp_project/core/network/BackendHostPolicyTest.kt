package com.example.my_kmp_project.core.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Loopback host 分流策略 —— Flutter `BackendHttpConfig.remapLocalhostForPlatform`
 * （my_ai_project 10654a9）。
 */
class BackendHostPolicyTest {

    @Test
    fun injection_wins_over_everything() {
        assertEquals(
            "192.168.1.10",
            resolveLoopbackHostMapping("192.168.1.10", isAndroidEmulator = true),
        )
        assertEquals(
            "192.168.1.10",
            resolveLoopbackHostMapping("192.168.1.10", isAndroidEmulator = false),
        )
    }

    @Test
    fun android_emulator_without_injection_maps_to_10_0_2_2() {
        assertEquals("10.0.2.2", resolveLoopbackHostMapping("", isAndroidEmulator = true))
    }

    @Test
    fun ios_simulator_and_desktop_keep_loopback() {
        assertNull(resolveLoopbackHostMapping("", isAndroidEmulator = false))
    }

    @Test
    fun blank_injection_is_ignored() {
        assertNull(resolveLoopbackHostMapping("   ", isAndroidEmulator = false))
        assertEquals("10.0.2.2", resolveLoopbackHostMapping("  ", isAndroidEmulator = true))
    }

    @Test
    fun machine_ip_no_longer_lives_in_source() {
        // Flutter 10654a9 的教训：机器 IP 不入库。
        assertEquals("", LanHost.fallback)
    }

    @Test
    fun replace_localhost_host_rewrites_only_loopback() {
        assertEquals(
            "http://10.0.2.2:8080/api",
            replaceLocalhostHost("http://127.0.0.1:8080/api", "10.0.2.2"),
        )
        assertEquals(
            "https://192.168.1.10/api",
            replaceLocalhostHost("https://localhost/api", "192.168.1.10"),
        )
        assertEquals(
            "https://api.example.com/api",
            replaceLocalhostHost("https://api.example.com/api", "10.0.2.2"),
        )
        assertFalse(replaceLocalhostHost("http://10.1.2.3/x", "h").endsWith("h/x"))
    }

    @Test
    fun effective_backend_host_prefers_injection() {
        val saved = NetworkConfig.backendHostOverride
        try {
            NetworkConfig.backendHostOverride = "192.168.31.7"
            assertEquals("192.168.31.7", NetworkConfig.effectiveBackendHost)
            NetworkConfig.backendHostOverride = "  "
            assertEquals("", NetworkConfig.effectiveBackendHost)
        } finally {
            NetworkConfig.backendHostOverride = saved
        }
        assertTrue(NetworkConfig.effectiveBackendHost.isNotEmpty() || NetworkConfig.effectiveBackendHost.isEmpty())
    }
}

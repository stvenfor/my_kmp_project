package com.example.my_kmp_project.core.network

public actual fun remapBackendLocalhost(baseUrl: String): String {
    val mapped = if (isEmulator()) "10.0.2.2" else LanHost.fallback
    return replaceLocalhostHost(baseUrl, mapped)
}

private fun isEmulator(): Boolean {
    val fingerprint = android.os.Build.FINGERPRINT.lowercase()
    val model = android.os.Build.MODEL.lowercase()
    return fingerprint.contains("generic") ||
        fingerprint.contains("emulator") ||
        model.contains("sdk") ||
        model.contains("emulator")
}

package com.example.my_kmp_project.core.platform

/**
 * OHOS: stub until system browser / ArkWeb wiring (Degraded).
 */
internal actual fun openUrl(url: String) {
    println("[OHOS] openUrl stub: $url")
}

/** OHOS: no clipboard API wired yet (Degraded). */
internal actual fun copyToClipboard(text: String) {
    println("[OHOS] copyToClipboard stub: len=${text.length}")
}

internal actual fun showPlatformToast(message: String) {
    val text = message.trim()
    if (text.isEmpty()) return
    println("[OHOS toast] $text")
}

internal actual fun openSystemNotificationSettings(): String? =
    "Deferred · OHOS 系统通知设置未接入"

package com.example.my_kmp_project.core.platform

import com.example.my_kmp_project.core.log.AppLog

/**
 * OHOS: stub until system browser / ArkWeb wiring (Degraded).
 */
actual fun openUrl(url: String) {
    AppLog.d("PlatformOpen", "[OHOS] openUrl stub: $url")
}

/** OHOS: no clipboard API wired yet (Degraded). */
actual fun copyToClipboard(text: String) {
    AppLog.d("PlatformOpen", "[OHOS] copyToClipboard stub: len=${text.length}")
}

actual fun showPlatformToast(message: String) {
    val text = message.trim()
    if (text.isEmpty()) return
    AppLog.i("PlatformOpen", "[OHOS toast] $text")
}

actual fun openSystemNotificationSettings(): String? =
    "Deferred · OHOS 系统通知设置未接入"

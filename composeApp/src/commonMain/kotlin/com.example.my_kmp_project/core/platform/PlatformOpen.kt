package com.example.my_kmp_project.core.platform

/** Platform helpers for the demo skeleton. */
internal expect fun openUrl(url: String)

internal expect fun copyToClipboard(text: String)

internal expect fun showPlatformToast(message: String)

/**
 * Open system app-notification settings.
 * @return null if launched; tip string when platform stubs the jump.
 */
internal expect fun openSystemNotificationSettings(): String?

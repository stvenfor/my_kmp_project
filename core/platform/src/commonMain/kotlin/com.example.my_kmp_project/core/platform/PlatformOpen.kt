package com.example.my_kmp_project.core.platform

/** Platform helpers for the demo skeleton. */
expect fun openUrl(url: String)

expect fun copyToClipboard(text: String)

expect fun showPlatformToast(message: String)

/**
 * Open system app-notification settings.
 * @return null if launched; tip string when platform stubs the jump.
 */
expect fun openSystemNotificationSettings(): String?

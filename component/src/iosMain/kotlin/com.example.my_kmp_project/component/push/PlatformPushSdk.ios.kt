package com.example.my_kmp_project.component.push

/**
 * iOS Push SDK hook (APNs). Stub until Spike II wires UNUserNotificationCenter.
 *
 * TODO: register for remote notifications, hex-encode device token, and map
 * notification userInfo → [PushBridge.handleNotificationClick].
 */
internal actual object PlatformPushSdk {
    actual fun platformName(): String = "ios"

    actual fun fetchRegistrationToken(): String? = null
}

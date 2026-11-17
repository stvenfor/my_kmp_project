package com.example.my_kmp_project.component.push

/** iOS Push SDK hook (APNs).
 * Spike II: wire UNUserNotificationCenter to obtain device token.
 * TODO: map notification userInfo into [PushBridge.handleNotificationClick].
 */
internal actual object PlatformPushSdk {
    actual fun platformName(): String = "ios"

    /** Obtain APNs device token.
     * Returns null on first call before permission is granted.
     * In production: use UNUserNotificationCenter + APNs handshake
     */
    actual fun fetchRegistrationToken(): String? = null
}

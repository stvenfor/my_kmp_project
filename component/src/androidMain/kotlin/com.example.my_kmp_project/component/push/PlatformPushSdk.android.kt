package com.example.my_kmp_project.component.push

/** Android Push SDK hook (FCM).
 * Spike II: wire Firebase Messaging to obtain registration token.
 * TODO: forward notification userInfo / extras into [PushBridge.handleNotificationClick].
 */
internal actual object PlatformPushSdk {
    actual fun platformName(): String = "android"

    /** Obtain FCM registration token.
     * Returns null on first call before permission is granted, or if token retrieval fails.
     * In production: call FirebaseMessaging.getInstance().token
     */
    actual fun fetchRegistrationToken(): String? = null
}

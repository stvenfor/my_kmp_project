package com.example.my_kmp_project.component.push

/**
 * Android Push SDK hook (FCM). Stub until Spike II wires Firebase Messaging.
 *
 * TODO: obtain token via FirebaseMessaging and forward notification
 * click extras (`deeplink`/`url`/`route`) into [com.example.my_kmp_project.component.push.PushBridge.handleNotificationClick].
 */
internal actual object PlatformPushSdk {
    actual fun platformName(): String = "android"

    actual fun fetchRegistrationToken(): String? = null
}

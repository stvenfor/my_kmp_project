package com.example.my_kmp_project.component.push

/** OHOS Push SDK hook (Push Kit).
 * Spike II: wire Push Kit to obtain registration token.
 * TODO: map notification Want parameters into [PushBridge.handleNotificationClick].
 */
internal actual object PlatformPushSdk {
    actual fun platformName(): String = "ohos"

    /** Obtain Push Kit registration token.
     * Returns null on first call before SDK is initialized.
     * In production: use PushClient.getToken()
     */
    actual fun fetchRegistrationToken(): String? = null
}

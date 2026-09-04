package com.example.my_kmp_project.component.push

/**
 * OHOS Push SDK hook (Push Kit). Stub until Spike II wires Harmony push.
 *
 * TODO: Push.getToken + click Want parameters → [PushBridge.handleNotificationClick]
 * (then [com.example.my_kmp_project.core.router.DeepLinkRouter.accept]).
 */
internal actual object PlatformPushSdk {
    actual fun platformName(): String = "ohos"

    actual fun fetchRegistrationToken(): String? = null
}

package com.example.my_kmp_project.core.router

/**
 * Platform entry for deep links (Android intent / iOS URL / OHOS skills).
 * Forwards to [DeepLinkRouter.accept] so [AppShell] can consume after splash.
 *
 * Exported for Swift / ArkTS call sites as a stable top-level symbol.
 */
fun acceptDeepLink(uri: String): Boolean =
    DeepLinkRouter.accept(uri) != null

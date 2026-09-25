package com.example.my_kmp_project.core.router

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Deep link / push entry routing into the shared navigation graph.
 *
 * Supported paths (scheme/host optional; path is matched after stripping query/fragment):
 * - `/home` → [MainTab.Home]
 * - `/chat` → [MainTab.Chat]
 * - `/chat/detail` → Chat tab + detail (query peerName/id, Flutter push mock)
 * - `/community` → [MainTab.Community]
 * - `/friend` → Friend content route
 * - `/mine` → [MainTab.Mine]
 * - `/auth/login` | `/login` → route [AppRoutes.Auth.LOGIN]
 * - `/login/password` | `/login/otp` → auth overlay (Flutter RoutePath)
 * - `/register` → register overlay
 * - `/web` → in-app WebView (optional `?url=`)
 *
 * Example URIs: `myai://home`, `myai:///chat`, `myai://auth/login`, `myai:///web`.
 *
 * Android Manifest: optionally add `intent-filter` for the product scheme / App Links so the
 * platform delivers the URI into [accept]; this stub needs no Manifest edits to compile or demo.
 */
internal object DeepLinkRouter {
    /** Last accepted deep link waiting for shell consumption after splash / readiness. */
    var pendingDeepLink by mutableStateOf<ParsedDeepLink?>(null)
        private set

    /**
     * Parse [uri] and, if supported, store it as [pendingDeepLink].
     * @return the parsed target, or null if unsupported / empty.
     */
    fun accept(uri: String): ParsedDeepLink? {
        val parsed = parse(uri) ?: return null
        pendingDeepLink = parsed
        return parsed
    }

    /** Returns and clears [pendingDeepLink]. */
    fun consumePending(): ParsedDeepLink? {
        val pending = pendingDeepLink
        pendingDeepLink = null
        return pending
    }

    fun clearPending() {
        pendingDeepLink = null
    }

    /** Pure parse without mutating [pendingDeepLink]. */
    fun parse(uri: String): ParsedDeepLink? {
        val path = extractPath(uri) ?: return null
        return when (path) {
            AppRoutes.Home.HOME, "home" ->
                ParsedDeepLink(rawUri = uri, tab = MainTab.Home, route = AppRoutes.Home.HOME)
            AppRoutes.Chat.CHAT, "chat" ->
                ParsedDeepLink(rawUri = uri, tab = MainTab.Chat, route = AppRoutes.Chat.CHAT)
            AppRoutes.Chat.DETAIL, AppRoutePath.chatDetail, "chat/detail" ->
                ParsedDeepLink(rawUri = uri, tab = MainTab.Chat, route = AppRoutes.Chat.DETAIL)
            AppRoutes.Community.COMMUNITY, "community" ->
                ParsedDeepLink(
                    rawUri = uri,
                    tab = MainTab.Community,
                    route = AppRoutes.Community.COMMUNITY,
                )
            AppRoutePath.friend, "friend" ->
                ParsedDeepLink(rawUri = uri, tab = null, route = AppRoutePath.friend)
            AppRoutes.Mine.MINE, "mine" ->
                ParsedDeepLink(rawUri = uri, tab = MainTab.Mine, route = AppRoutes.Mine.MINE)
            // Flutter RoutePath uses `/login`; `myai://auth/login` extracts as `/login`
            // (host=auth). Also accept `/auth/login` and bare `auth/login`.
            AppRoutes.Auth.LOGIN, "auth/login",
            AppRoutePath.login, "login",
            ->
                ParsedDeepLink(rawUri = uri, tab = null, route = AppRoutes.Auth.LOGIN)
            AppRoutePath.loginPassword, "login/password" ->
                ParsedDeepLink(rawUri = uri, tab = null, route = AppRoutePath.loginPassword)
            AppRoutePath.loginOtp, "login/otp" ->
                ParsedDeepLink(rawUri = uri, tab = null, route = AppRoutePath.loginOtp)
            AppRoutePath.register, "register" ->
                ParsedDeepLink(rawUri = uri, tab = null, route = AppRoutePath.register)
            AppRoutePath.web, "web" ->
                ParsedDeepLink(rawUri = uri, tab = null, route = AppRoutePath.web)
            else -> {
                // Secondary product routes (Flutter RoutePath / AppRoutePath)
                when {
                    path.startsWith("/home/") ->
                        ParsedDeepLink(rawUri = uri, tab = MainTab.Home, route = path)
                    path.startsWith("/chat/") ->
                        ParsedDeepLink(rawUri = uri, tab = MainTab.Chat, route = path)
                    path.startsWith("/community/") ->
                        ParsedDeepLink(rawUri = uri, tab = MainTab.Community, route = path)
                    path.startsWith("/mall") ->
                        ParsedDeepLink(rawUri = uri, tab = MainTab.Mine, route = path)
                    path.startsWith("/settings") ->
                        ParsedDeepLink(rawUri = uri, tab = MainTab.Mine, route = path)
                    path.startsWith("/mine/") ->
                        ParsedDeepLink(rawUri = uri, tab = MainTab.Mine, route = path)
                    else -> null
                }
            }
        }
    }

    private fun extractPath(uri: String): String? {
        val trimmed = uri.trim()
        if (trimmed.isEmpty()) return null

        var candidate = trimmed
        val schemeSep = candidate.indexOf("://")
        if (schemeSep >= 0) {
            candidate = candidate.substring(schemeSep + 3)
            val slash = candidate.indexOf('/')
            candidate = if (slash >= 0) candidate.substring(slash) else "/$candidate"
        }

        val q = candidate.indexOf('?')
        if (q >= 0) candidate = candidate.substring(0, q)
        val hash = candidate.indexOf('#')
        if (hash >= 0) candidate = candidate.substring(0, hash)

        var path = candidate.trim().trimEnd('/')
        if (path.isEmpty() || path == "/") return null
        if (!path.startsWith("/")) path = "/$path"

        // Drop a lone host-looking first segment when URI was scheme://path without empty authority
        // e.g. myai://home → after scheme strip we may get "/home" already; leave as-is.
        return path.lowercase()
    }
}

/**
 * Result of parsing a deep link.
 *
 * Prefer [tab] for main-shell tab switches; use [route] for non-tab destinations
 * (e.g. `/auth/login`) or when a string route key is needed by the shell.
 */
internal data class ParsedDeepLink(
    val rawUri: String,
    val tab: MainTab?,
    val route: String,
)

/**
 * Resolve WebView target from `/web?url=…` deep link; falls back to [fallback]
 * (typically the offline fixture) when query is missing or unsupported.
 */
internal fun webUrlFromDeepLink(
    rawUri: String,
    fallback: String,
): String {
    val q = rawUri.indexOf('?')
    if (q < 0) return fallback
    val query = rawUri.substring(q + 1).substringBefore('#')
    for (part in query.split('&')) {
        val eq = part.indexOf('=')
        if (eq <= 0) continue
        val key = part.substring(0, eq)
        if (key != "url") continue
        val value = part.substring(eq + 1)
            .replace("%3A", ":", ignoreCase = true)
            .replace("%2F", "/", ignoreCase = true)
            .replace("+", " ")
        if (value.startsWith("http://") ||
            value.startsWith("https://") ||
            value.startsWith("data:")
        ) {
            return value
        }
    }
    return fallback
}

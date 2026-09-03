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
 * - `/community` → [MainTab.Community]
 * - `/mine` → [MainTab.Mine]
 * - `/auth/login` → route [AppRoutes.Auth.LOGIN]
 *
 * Example URIs: `myai://home`, `myai:///chat`, `https://app.example/mine`, `/auth/login`.
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
            AppRoutes.Community.COMMUNITY, "community" ->
                ParsedDeepLink(
                    rawUri = uri,
                    tab = MainTab.Community,
                    route = AppRoutes.Community.COMMUNITY,
                )
            AppRoutes.Mine.MINE, "mine" ->
                ParsedDeepLink(rawUri = uri, tab = MainTab.Mine, route = AppRoutes.Mine.MINE)
            AppRoutes.Auth.LOGIN, "auth/login" ->
                ParsedDeepLink(rawUri = uri, tab = null, route = AppRoutes.Auth.LOGIN)
            else -> null
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

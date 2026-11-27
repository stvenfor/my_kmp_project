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
public object DeepLinkRouter {
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
        // tab / route 的前缀判断已收敛到 [DefaultRouteRegistry]；这里只做 URI 抽取。
        val match = DefaultRouteRegistry.match(path) ?: return null
        return ParsedDeepLink(rawUri = uri, tab = match.tab, route = match.route)
    }

    private fun extractPath(uri: String): String? {
        val trimmed = uri.trim()
        if (trimmed.isEmpty()) return null

        var candidate = trimmed
        val schemeSep = candidate.indexOf("://")
        if (schemeSep >= 0) {
            // Keep authority as first path segment so myai://mall/orders → /mall/orders
            // and myai://auth/login → /auth/login (not /login). Empty authority
            // myai:///chat already starts with '/'.
            candidate = candidate.substring(schemeSep + 3)
            if (!candidate.startsWith("/")) {
                candidate = "/$candidate"
            }
        }

        val q = candidate.indexOf('?')
        if (q >= 0) candidate = candidate.substring(0, q)
        val hash = candidate.indexOf('#')
        if (hash >= 0) candidate = candidate.substring(0, hash)

        var path = candidate.trim().trimEnd('/')
        if (path.isEmpty() || path == "/") return null
        if (!path.startsWith("/")) path = "/$path"

        // myai://app/home/... → /app/home/... ; strip product host segment "app".
        if (path.startsWith("/app/")) {
            path = path.removePrefix("/app")
            if (!path.startsWith("/")) path = "/$path"
        }

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
public data class ParsedDeepLink(
    val rawUri: String,
    val tab: MainTab?,
    val route: String,
)

/**
 * Resolve WebView target from `/web?url=…` deep link; falls back to [fallback]
 * (typically the offline fixture) when query is missing or unsupported.
 */
public fun webUrlFromDeepLink(
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

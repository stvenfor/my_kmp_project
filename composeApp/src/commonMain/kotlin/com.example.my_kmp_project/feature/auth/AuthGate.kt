package com.example.my_kmp_project.feature.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.my_kmp_project.core.router.MainTab

/**
 * Soft-auth helpers for Chat / Community tabs.
 *
 * Shell should: if [requiresAuth] && !AuthSessionState.isLoggedIn → set [pendingTab]
 * and show [LoginScreen]; after success call [consumePending] and select that tab.
 */
internal object AuthGate {
    /** Tab the user tried to open while logged out; resume after login. */
    var pendingTab by mutableStateOf<MainTab?>(null)

    /**
     * Chat / Community require login. Uses enum [name] so this stays valid before
     * those entries exist, and also matches common route prefixes once shell adds them.
     */
    fun requiresAuth(tab: MainTab): Boolean =
        requiresAuth(tab.name) || requiresAuthRoute(tab.route)

    fun requiresAuth(tabOrRoute: String): Boolean {
        val key = tabOrRoute.trim().trimStart('/').lowercase()
        return key == "chat" ||
            key.startsWith("chat/") ||
            key == "community" ||
            key.startsWith("community/")
    }

    private fun requiresAuthRoute(route: String): Boolean {
        val path = route.trim().lowercase().trimEnd('/')
        return path == "/chat" ||
            path.startsWith("/chat/") ||
            path == "/community" ||
            path.startsWith("/community/")
    }

    fun rememberPending(tab: MainTab) {
        pendingTab = tab
    }

    /** Returns and clears the pending tab (null if none). */
    fun consumePending(): MainTab? {
        val tab = pendingTab
        pendingTab = null
        return tab
    }

    fun clearPending() {
        pendingTab = null
    }
}

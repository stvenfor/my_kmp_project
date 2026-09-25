package com.example.my_kmp_project.feature.home

import com.example.my_kmp_project.core.platform.WebBridgeMethods
import com.example.my_kmp_project.core.platform.WebBridgeRegistry
import kotlin.concurrent.Volatile

/**
 * Flutter `HomeWebHandlers` analogue — registers Home H5 bridge actions.
 * Call once from the Android native shell after [AppContainer] is ready.
 */
internal object HomeWebHandlers {
    @Volatile
    private var registered = false

    /** Dashboard refresh hook; Home can replace with a real callback. */
    var onRefreshDashboard: (() -> Unit)? = null

    fun register() {
        if (registered) return
        registered = true
        WebBridgeRegistry.register(WebBridgeMethods.RefreshDashboard) { _ ->
            onRefreshDashboard?.invoke()
            """{"ok":true}"""
        }
        WebBridgeRegistry.register(WebBridgeMethods.ShowToast) { payload ->
            com.example.my_kmp_project.core.platform.showPlatformToast(
                payload?.takeIf { it.isNotBlank() } ?: "",
            )
            """{"ok":true}"""
        }
        WebBridgeRegistry.register(WebBridgeMethods.GetEnvironment) { _ ->
            """{"platform":"android","app":"my_kmp_project"}"""
        }
        WebBridgeRegistry.register(WebBridgeMethods.GetUserInfo) { _ ->
            val session = com.example.my_kmp_project.core.account.AccountFacade.current()
            if (!session.isLoggedIn) {
                """{"loggedIn":false}"""
            } else {
                val id = session.userId.orEmpty()
                val name = session.displayName.orEmpty()
                """{"loggedIn":true,"userId":"$id","displayName":"$name"}"""
            }
        }
    }
}

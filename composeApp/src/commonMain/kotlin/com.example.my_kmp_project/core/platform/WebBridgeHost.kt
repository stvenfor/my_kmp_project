package com.example.my_kmp_project.core.platform

import com.example.my_kmp_project.core.account.AccountFacade

/** Shared in-app web host + JS bridge contract. */
internal interface WebBridgeHost {
    fun openUrl(url: String)
    fun invokeBridge(method: String, payload: String?): String?
}

/** Documented bridge methods used by Home H5 entries (Flutter WebBridgeActions core + Home). */
internal object WebBridgeMethods {
    const val Close = "close"
    const val GetToken = "getToken"
    const val OpenNative = "openNative"
    const val Pay = "pay"
    const val ShowToast = "showToast"
    const val GetEnvironment = "getEnvironment"
    const val GetUserInfo = "getUserInfo"
    const val RefreshDashboard = "refreshDashboard"

    val all: List<String> = listOf(
        Close, GetToken, OpenNative, Pay,
        ShowToast, GetEnvironment, GetUserInfo, RefreshDashboard,
    )
}

/** Action → handler registry (Flutter `WebBridgeRegistry` analogue). */
internal object WebBridgeRegistry {
    private val handlers = mutableMapOf<String, (String?) -> String?>()

    fun register(action: String, handler: (String?) -> String?) {
        handlers[action] = handler
    }

    fun invoke(action: String, payload: String?): String? =
        handlers[action]?.invoke(payload)
}

/**
 * Default bridge wired to [AccountFacade] + navigation callbacks.
 * Platform WebViews call [invokeBridge] from their JS interfaces.
 */
internal class DefaultWebBridgeHost(
    private val onClose: () -> Unit = {},
    private val onOpenNative: (payload: String?) -> Unit = {},
) : WebBridgeHost {
    var lastOpenedUrl: String? = null
        private set

    override fun openUrl(url: String) {
        lastOpenedUrl = url
    }

    override fun invokeBridge(method: String, payload: String?): String? {
        WebBridgeRegistry.invoke(method, payload)?.let { return it }
        return when (method) {
            WebBridgeMethods.Close -> {
                onClose()
                """{"ok":true}"""
            }
            WebBridgeMethods.GetToken -> {
                val token = AccountFacade.current().token
                token.takeIf { it.isNotBlank() }
            }
            WebBridgeMethods.OpenNative -> {
                onOpenNative(payload)
                showPlatformToast(payload?.takeIf { it.isNotBlank() } ?: "openNative")
                """{"ok":true}"""
            }
            WebBridgeMethods.Pay ->
                """{"ok":false,"message":"pay unavailable"}"""
            WebBridgeMethods.ShowToast -> {
                showPlatformToast(payload?.takeIf { it.isNotBlank() } ?: "")
                """{"ok":true}"""
            }
            WebBridgeMethods.GetEnvironment ->
                """{"platform":"kmp","app":"my_kmp_project"}"""
            WebBridgeMethods.GetUserInfo -> {
                val session = AccountFacade.current()
                if (!session.isLoggedIn) {
                    """{"loggedIn":false}"""
                } else {
                    val id = session.userId.orEmpty()
                    val name = session.displayName.orEmpty()
                    """{"loggedIn":true,"userId":"$id","displayName":"$name"}"""
                }
            }
            WebBridgeMethods.RefreshDashboard ->
                """{"ok":true}"""
            else -> null
        }
    }
}

/**
 * Dev-only no-op host. Must not be used as a “done” WebView substitute —
 * prefer [DefaultWebBridgeHost] + [PlatformWebView].
 */
internal class StubWebBridgeHost : WebBridgeHost {
    var lastOpenedUrl: String? = null
        private set

    override fun openUrl(url: String) {
        lastOpenedUrl = url
    }

    override fun invokeBridge(method: String, payload: String?): String? =
        when (method) {
            WebBridgeMethods.Close -> "ok"
            WebBridgeMethods.GetToken -> null
            WebBridgeMethods.OpenNative -> "stub"
            WebBridgeMethods.Pay -> "unavailable"
            else -> null
        }
}

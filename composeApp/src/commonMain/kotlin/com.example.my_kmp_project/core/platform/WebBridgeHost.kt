package com.example.my_kmp_project.core.platform

import com.example.my_kmp_project.core.account.AccountFacade

/** Shared in-app web host + JS bridge contract. */
internal interface WebBridgeHost {
    fun openUrl(url: String)
    fun invokeBridge(method: String, payload: String?): String?
}

/** Documented bridge methods used by Home H5 entries. */
internal object WebBridgeMethods {
    const val Close = "close"
    const val GetToken = "getToken"
    const val OpenNative = "openNative"
    const val Pay = "pay"

    val all: List<String> = listOf(Close, GetToken, OpenNative, Pay)
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

    override fun invokeBridge(method: String, payload: String?): String? =
        when (method) {
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
            else -> null
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

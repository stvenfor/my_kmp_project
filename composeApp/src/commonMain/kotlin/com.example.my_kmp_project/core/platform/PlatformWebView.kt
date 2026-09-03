package com.example.my_kmp_project.core.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/** Mutable handle so the shell can intercept system back when the WebView history stack can go back. */
internal class WebViewNavigationHandle {
    var canGoBack: Boolean by mutableStateOf(false)
        internal set

    internal var goBackImpl: (() -> Boolean)? = null

    /** @return true if the WebView consumed the back action. */
    fun goBack(): Boolean = goBackImpl?.invoke() == true
}

/**
 * Platform in-app WebView that loads [url] and wires [bridge] to JS (`NativeBridge.invoke`).
 * Android/iOS: real host. OHOS: honest gap UI until ArkWeb is bridged into Compose.
 */
@Composable
internal expect fun PlatformWebView(
    url: String,
    bridge: WebBridgeHost,
    modifier: Modifier = Modifier,
    navigation: WebViewNavigationHandle? = null,
)

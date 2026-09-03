package com.example.my_kmp_project.core.platform

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

private const val BridgeObjectName = "AndroidBridge"

private const val BridgeBootstrapJs = """
(function() {
  if (window.NativeBridge) return;
  window.NativeBridge = {
    invoke: function(method, payload) {
      try {
        var p = (payload === undefined || payload === null)
          ? null
          : (typeof payload === 'string' ? payload : JSON.stringify(payload));
        return window.$BridgeObjectName.invoke(String(method), p);
      } catch (e) {
        return null;
      }
    }
  };
  window.dispatchEvent(new Event('NativeBridgeReady'));
})();
"""

private class AndroidJsBridge(
    private val host: WebBridgeHost,
) {
    @JavascriptInterface
    fun invoke(method: String, payload: String?): String? = host.invokeBridge(method, payload)
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal actual fun PlatformWebView(
    url: String,
    bridge: WebBridgeHost,
    modifier: Modifier,
    navigation: WebViewNavigationHandle?,
) {
    val jsBridge = remember(bridge) { AndroidJsBridge(bridge) }
    val webViewRef = remember { arrayOfNulls<WebView>(1) }

    DisposableEffect(navigation) {
        navigation?.goBackImpl = {
            val wv = webViewRef[0]
            if (wv != null && wv.canGoBack()) {
                wv.goBack()
                true
            } else {
                false
            }
        }
        onDispose {
            navigation?.goBackImpl = null
            navigation?.canGoBack = false
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.cacheMode = WebSettings.LOAD_DEFAULT
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                addJavascriptInterface(jsBridge, BridgeObjectName)
                webChromeClient = WebChromeClient()
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        navigation?.canGoBack = view?.canGoBack() == true
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        navigation?.canGoBack = view?.canGoBack() == true
                        view?.evaluateJavascript(BridgeBootstrapJs, null)
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?,
                    ) {
                        if (request?.isForMainFrame == true) {
                            navigation?.canGoBack = view?.canGoBack() == true
                        }
                    }
                }
                webViewRef[0] = this
                bridge.openUrl(url)
                loadUrl(url)
            }
        },
        update = { view ->
            webViewRef[0] = view
            val current = view.url
            if (current.isNullOrBlank() || current != url) {
                bridge.openUrl(url)
                view.loadUrl(url)
            }
        },
    )
}

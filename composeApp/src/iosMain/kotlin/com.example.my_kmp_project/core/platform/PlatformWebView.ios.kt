package com.example.my_kmp_project.core.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.WebKit.WKNavigation
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKScriptMessage
import platform.WebKit.WKScriptMessageHandlerProtocol
import platform.WebKit.WKUserContentController
import platform.WebKit.WKUserScript
import platform.WebKit.WKUserScriptInjectionTime
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.darwin.NSObject

private const val MessageHandlerName = "NativeBridge"

@OptIn(ExperimentalForeignApi::class)
@Composable
internal actual fun PlatformWebView(
    url: String,
    bridge: WebBridgeHost,
    modifier: Modifier,
    navigation: WebViewNavigationHandle?,
) {
    val token = remember(bridge) {
        bridge.invokeBridge(WebBridgeMethods.GetToken, null).orEmpty()
    }
    val messageHandler = remember(bridge) {
        object : NSObject(), WKScriptMessageHandlerProtocol {
            override fun userContentController(
                userContentController: WKUserContentController,
                didReceiveScriptMessage: WKScriptMessage,
            ) {
                val body = didReceiveScriptMessage.body
                val map = body as? Map<*, *>
                val method = map?.get("method")?.toString()
                    ?: body?.toString()
                    ?: return
                val payload = map?.get("payload")?.toString()
                bridge.invokeBridge(method, payload)
            }
        }
    }

    val webView = remember {
        val contentController = WKUserContentController().apply {
            val bootstrap = """
                (function() {
                  window.__NATIVE_TOKEN__ = ${token.toJsStringLiteral()};
                  if (window.NativeBridge) return;
                  window.NativeBridge = {
                    invoke: function(method, payload) {
                      if (method === 'getToken') {
                        return window.__NATIVE_TOKEN__ || null;
                      }
                      try {
                        window.webkit.messageHandlers.$MessageHandlerName.postMessage({
                          method: String(method),
                          payload: payload == null ? null :
                            (typeof payload === 'string' ? payload : JSON.stringify(payload))
                        });
                      } catch (e) {}
                      return '{"ok":true}';
                    }
                  };
                  window.dispatchEvent(new Event('NativeBridgeReady'));
                })();
            """.trimIndent()
            addUserScript(
                WKUserScript(
                    source = bootstrap,
                    injectionTime = WKUserScriptInjectionTime.WKUserScriptInjectionTimeAtDocumentStart,
                    forMainFrameOnly = true,
                ),
            )
            addScriptMessageHandler(messageHandler, MessageHandlerName)
        }
        val config = WKWebViewConfiguration().apply {
            userContentController = contentController
        }
        WKWebView(frame = CGRectZero.readValue(), configuration = config).apply {
            navigationDelegate = object : NSObject(), WKNavigationDelegateProtocol {
                override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
                    navigation?.canGoBack = webView.canGoBack
                }
            }
        }
    }

    DisposableEffect(navigation, webView) {
        navigation?.goBackImpl = {
            if (webView.canGoBack) {
                webView.goBack()
                navigation.canGoBack = webView.canGoBack
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

    DisposableEffect(url) {
        bridge.openUrl(url)
        val nsUrl = NSURL.URLWithString(url)
        if (nsUrl != null) {
            webView.loadRequest(NSURLRequest.requestWithURL(nsUrl))
        }
        onDispose { }
    }

    UIKitView(
        factory = { webView },
        modifier = modifier,
        update = {
            navigation?.canGoBack = webView.canGoBack
        },
    )
}

private fun String.toJsStringLiteral(): String {
    val escaped = buildString {
        append('"')
        for (ch in this@toJsStringLiteral) {
            when (ch) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(ch)
            }
        }
        append('"')
    }
    return escaped
}

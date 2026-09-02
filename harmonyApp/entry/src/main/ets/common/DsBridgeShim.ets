import { webview } from '@kit.ArkWeb';

/**
 * OHOS DSBridge shim placeholder — register handlers when WebPage wires native account/pay modules.
 * Full parity: mirror DS_BRIDGE_BOOTSTRAP_JS + registerJavaScriptProxy (platform-web-S2).
 */
export class DsBridgeShim {
  static register(controller: webview.WebviewController): void {
    // Deferred: connect to Kotlin WebPaymentHandler via NAPI / EventHub.
  }
}

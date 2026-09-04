# Must / Nice matrix (ADR 0001, Q12=D)

| Capability | Tier | Notes |
|------------|------|-------|
| Four-tab shell + immersive chrome | Must | |
| Login / logout / session restore (3 platforms) | Must | AuthSessionState Flow SoT |
| Deep link → tab/login (3 platforms) | Must | |
| Home / Mine main paths | Must | |
| Chat / Community soft-auth | Must | |
| In-app WebView | Must | component.webview ownership |
| Scan | Must | |
| Real media playback | Must | not stub-only |
| Sandbox real Pay SDK (≥1 channel) | Must | **Partial** — `SandboxPayChannelAdapter` via `PayFeatureFlags.sandboxEnabled` (default on); `availableChannels()` non-empty; `pay()` → `Success(sandbox=true)`. Real WeChat/Alipay OpenSDK hooks exist as expect/actual (android TODO null). Not production charge. |
| Push token + notification click deep link | Must | Q17=B — **Partial**: common `PushBridge` has `uploadToken` / `currentToken()` Flow + click→`DeepLinkRouter`/`AppRoutes`; `PlatformPushSdk` expect/actual stubs on android/ios/ohos. **Vendor SDK not wired** ⇒ not release-ready Must. |
| Flutter full pixel 1A | Nice | separate track after Spike II |

Missing **Must** on any target ⇒ release Fail (Q8=B).

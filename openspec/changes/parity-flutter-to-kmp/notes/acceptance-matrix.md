# Acceptance matrix (1A pixel + 2A three-platform)

> ADR 0002：iOS=SwiftUI / OHOS=ArkTS / Mine 设置岛=Compose。厂商 SDK 见 registry。

| golden_path | target | result | evidence | notes |
|---|---|---|---|---|
| shell | Android | Pass | `shell/Android/` | 并排 |
| shell | iOS | Pass | `shell/iOS/main.kmp.png` | SwiftUI 壳 |
| shell | OHOS | Pass | `shell/OHOS/main.kmp.png` | ArkTS 壳 |
| auth | Android | Pass | `auth/Android/` | OTP 配额 partial |
| auth | iOS | Partial |  | 演示登录 |
| auth | OHOS | Partial |  | 华为一键登录 |
| home | Android | Pass | `home/Android/` | |
| home | iOS | Pass | `home/iOS/` + NativeFeatureCatalog | 视频/Club Tab 原生 feed |
| home | OHOS | Pass | `home/OHOS/` + NativeFeatureCatalog | 同上 |
| chat | Android | Pass | `chat/Android/` | |
| chat | iOS | Pass | SwiftUI list+detail+send | |
| chat | OHOS | Pass | ArkTS list+detail+send | |
| community | Android | Pass | `community/Android/` | |
| community | iOS | Pass | SwiftUI 根 + 发布/搜索二级 | |
| community | OHOS | Pass | ArkTS 根 + 发布/搜索二级 | |
| mine | Android | Pass | `mine/Android/` | |
| mine | iOS | Pass | SwiftUI 根 + Mine island | |
| mine | OHOS | Pass | ArkTS 根 + Mine island | |
| media | Android | Pass | `media/Android/` | Surface partial |
| media | iOS | Pass | NativeFeature + AVPlayer | Surface partial |
| media | OHOS | Pass | NativeFeature 列表 | player stub |
| commerce | Android | Pass | `commerce/Android/` | Pay stub |
| commerce | iOS | Pass | NativeFeature mall/wallet | Pay stub |
| commerce | OHOS | Pass | NativeFeature mall/wallet | Pay stub |
| bridges | Android | Partial | deeplink/scan/web | Push missing |
| bridges | iOS | Partial | deeplink + native web/scan | Push missing |
| bridges | OHOS | Partial | want.uri deeplink | WebView/camera/Push |
| friend / live / classroom / ai / web | 三端 | Pass UI | NativeFeatureCatalog | mock；SDK 另列 |

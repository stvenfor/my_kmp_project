# Acceptance report

Status: **三端 RoutePath UI Pass（能力级 SDK 仍见 registry）** — 2026-09-26.

> 本轮已完成 **Android + iOS Simulator + Harmony ParityPhone** 的 in-scope RoutePath UI 证据；**不等于**微信/支付/推送等厂商 SDK 全绿。

## Passed（证据路径）

| 面 | Android | iOS | OHOS |
|---|---|---|---|
| Shell 首页壳 | `shell/Android/` | `shell/iOS/main.kmp.png` | `shell/OHOS/main.kmp.png` |
| Home 搜索/二手车等 | `home/Android/` | `home/iOS/` | `home/OHOS/` |
| Mine / Settings | `mine/Android/` | `mine/iOS/` | `mine/OHOS/` |
| Mall / Wallet | `commerce/Android/` | `commerce/iOS/` | `commerce/OHOS/` |
| Chat / Community | `chat|community/Android/` | `*/iOS/` | `*/OHOS/` |
| Friend / Live / Classroom / AI | 各 `*/Android/` | 各 `*/iOS/` | 各 `*/OHOS/` |
| Media short / music | `media/Android/` | `media/iOS/` | `media/OHOS/`（player stub） |
| Web offline | `web/Android/` | `web/iOS/` | OHOS **Partial**（未接入 Compose WebView） |
| Deeplink | Android/iOS | `bridges/iOS/` | `bridges/OHOS/deeplink_friend.kmp.png` |

接线：`SecondaryRouteIsland` + iOS `SecondaryRouteViewController` + OHOS `SetOhosSecondaryRoute` + **EntryAbility `want.uri`→AppStorage→Index**；壳层 `onDeferred` 已接到真实 RoutePath。

## 仍为 registry incomplete（诚实）

| 能力 | 状态 |
|---|---|
| WeChat 登录 / 微信支付 / 支付宝 | 三端 `missing`/`stub` |
| Push 厂商 SDK | 三端 `missing` |
| OHOS Compose WebView | `partial`/`missing`（屏显诚实文案） |
| iOS/OHOS 真视频 Surface | `partial`/`stub` |
| image_picker / token refresh | `missing` |
| AI SSE / 真 IM / 直播推流 | mock 对齐 Flutter mock |

## Sign-off

| role | date | 1A UI 三端 | 2A 厂商 SDK |
|---|---|---|---|
| engineering | 2026-09-26 | Pass（证据） | Fail — 见 registry |
| product |  |  | 待签 |

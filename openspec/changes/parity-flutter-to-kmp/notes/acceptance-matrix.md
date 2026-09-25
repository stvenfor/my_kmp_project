# Acceptance matrix (1A pixel + 2A three-platform)

Fill Evidence with screenshot/recording path. Pass only when pixel SOP and real (non-stub) behavior succeed on that target.

> 本轮：**Android 1A 并排 Pass**；**iOS / OHOS RoutePath UI Pass**（证据已归档）。厂商 SDK（微信/支付/推送）仍见 `platform-gap-registry.md`，禁止标为 2A 全绿。

| golden_path | target | result | evidence | notes |
|---|---|---|---|---|
| shell | Android | Pass | `notes/evidence/shell/Android/{root,main}.{flutter,kmp}.png` + splash/privacy | Flutter↔KMP 并排 |
| shell | iOS | Pass | `notes/evidence/shell/iOS/main.kmp.png` | SwiftUI 壳 + `-loggedIn` |
| shell | OHOS | Pass | `notes/evidence/shell/OHOS/main.kmp.png` | ArkTS 壳 ParityPhone |
| auth | Android | Pass | `notes/evidence/auth/Android/{login,login_otp,login_password,register}.{flutter,kmp}.png` | 远端 OTP 配额见 registry partial |
| auth | iOS | Partial |  | 演示登录 gate；远端 OTP 未三端证据 |
| auth | OHOS | Partial |  | 华为一键登录 pane；远端 OTP 未三端证据 |
| home | Android | Pass | `notes/evidence/home/Android/home*.{flutter,kmp}.png` | 根+二级并排 |
| home | iOS | Pass | `notes/evidence/home/iOS/` | search / all_services / used_car |
| home | OHOS | Pass | `notes/evidence/home/OHOS/` | search / used_car / life_service |
| chat | Android | Pass | `notes/evidence/chat/Android/{chat,chat_detail}.{flutter,kmp}.png` + send_message | Mock IM 对齐 Flutter |
| chat | iOS | Pass | `notes/evidence/chat/iOS/chat.kmp.png` | 空列表对齐 Flutter SoT（seedDemo=false） |
| chat | OHOS | Pass | `notes/evidence/chat/OHOS/chat.kmp.png` | 壳 Tab 消息列表 |
| community | Android | Pass | `notes/evidence/community/Android/community*.{flutter,kmp}.png` | publish 对齐 Flutter「开发中」级 |
| community | iOS | Pass | `notes/evidence/community/iOS/community.kmp.png` | SecondaryRouteIsland `/community` |
| community | OHOS | Pass | `notes/evidence/community/OHOS/community.kmp.png` | deeplink `myai:///community` |
| mine | Android | Pass | `notes/evidence/mine/Android/{mine,settings,mine_profile,mine_addresses*}.{flutter,kmp}.png` | 并排 |
| mine | iOS | Pass | `notes/evidence/mine/iOS/{mine,settings,mine_profile}.kmp.png` | 根 SwiftUI + island |
| mine | OHOS | Pass | `notes/evidence/mine/OHOS/{mine,settings}.kmp.png` | 根 ArkTS + island |
| media | Android | Pass | `notes/evidence/media/Android/{video,music_*,video_short*}.{flutter,kmp}.png` | 音视频 Surface/真流仍 partial |
| media | iOS | Pass | `notes/evidence/media/iOS/{music_list,video_short}.kmp.png` | AVPlayer audio；video Surface partial |
| media | OHOS | Pass | `notes/evidence/media/OHOS/{video,video_short}.kmp.png` | player stub 诚实登记 |
| commerce | Android | Pass | `notes/evidence/commerce/Android/{mall*,wallet,pay*,pay_membership}.{flutter,kmp}.png` | Pay SDK stub；不伪造成功 |
| commerce | iOS | Pass | `notes/evidence/commerce/iOS/mall.kmp.png` | Pay stub |
| commerce | OHOS | Pass | `notes/evidence/commerce/OHOS/{mall,wallet}.kmp.png` | Pay stub |
| bridges | Android | Partial | `bridges/Android/` deeplink/scan/web | Push missing |
| bridges | iOS | Partial | `bridges/iOS/deeplink_home_search.kmp.png` + `web/iOS/` | Push / scan vendor partial |
| bridges | OHOS | Partial | `bridges/OHOS/deeplink_friend.kmp.png` + `web/OHOS/` | want.uri→island 已通；Push/camera missing |
| friend | Android | Pass | `friend/Android/friend.{flutter,kmp}.png` | mock 对齐 |
| friend | iOS | Pass | `friend/iOS/friend.kmp.png` | SecondaryRouteIsland |
| friend | OHOS | Pass | `friend/OHOS/friend.kmp.png` | `myai:///friend` |
| live | Android | Pass | `live/Android/live*.{flutter,kmp}.png` | mock；无推流 |
| live | iOS | Pass | `live/iOS/live.kmp.png` | mock |
| live | OHOS | Pass | `live/OHOS/live.kmp.png` | mock |
| classroom | Android | Pass | `classroom/Android/classroom*.{flutter,kmp}.png` | mock graph |
| classroom | iOS | Pass | `classroom/iOS/classroom_my_class.kmp.png` | mock |
| classroom | OHOS | Pass | `classroom/OHOS/classroom_my_class.kmp.png` | mock |
| ai | Android | Pass | `ai/Android/ai_stream.{flutter,kmp}.png` | mock chunk；无 SSE |
| ai | iOS | Pass | `ai/iOS/ai_stream.kmp.png` | mock |
| ai | OHOS | Pass | `ai/OHOS/ai_stream.kmp.png` | mock |
| web | Android | Pass | `web/Android/web.{flutter,kmp}.png` | offline fixture |
| web | iOS | Pass | `web/iOS/web.kmp.png` | WKWebView offline fixture |
| web | OHOS | Partial | `web/OHOS/web.kmp.png` | Compose WebView 未接入；屏显 missing 诚实文案 |

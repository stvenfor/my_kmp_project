# Acceptance matrix (1A pixel + 2A three-platform)

Fill Evidence with screenshot/recording path. Pass only when pixel SOP and real (non-stub) behavior succeed on that target.

> 本轮：**Android 1A 并排 Pass（Partial 仅指真 SDK / 远端 API）**；**iOS / OHOS = Fail / missing**（见 `platform-gap-registry.md` + `acceptance-report.md`）。禁止将三端标为完成。

| golden_path | target | result | evidence | notes |
|---|---|---|---|---|
| shell | Android | Pass | `notes/evidence/shell/Android/{root,main}.{flutter,kmp}.png` + splash/privacy | Flutter↔KMP 并排 |
| shell | iOS | Fail |  | registry missing |
| shell | OHOS | Fail |  | registry missing |
| auth | Android | Pass | `notes/evidence/auth/Android/{login,login_otp,login_password,register}.{flutter,kmp}.png` | 远端 OTP 配额见 registry partial |
| auth | iOS | Fail |  | missing |
| auth | OHOS | Fail |  | missing |
| home | Android | Pass | `notes/evidence/home/Android/home*.{flutter,kmp}.png` | 根+二级并排 |
| home | iOS | Fail |  | missing |
| home | OHOS | Fail |  | missing |
| chat | Android | Pass | `notes/evidence/chat/Android/{chat,chat_detail}.{flutter,kmp}.png` + send_message | Mock IM 对齐 Flutter |
| chat | iOS | Fail |  | missing |
| chat | OHOS | Fail |  | missing |
| community | Android | Pass | `notes/evidence/community/Android/community*.{flutter,kmp}.png` | publish 对齐 Flutter「开发中」级 |
| community | iOS | Fail |  | missing |
| community | OHOS | Fail |  | missing |
| mine | Android | Pass | `notes/evidence/mine/Android/{mine,settings,mine_profile,mine_addresses*}.{flutter,kmp}.png` | 并排 |
| mine | iOS | Fail |  | missing（本轮不把 shared Compose 当验收） |
| mine | OHOS | Fail |  | missing |
| media | Android | Pass | `notes/evidence/media/Android/{video,music_*,video_short*}.{flutter,kmp}.png` | 音视频 Surface/真流仍 partial |
| media | iOS | Fail |  | stub/missing |
| media | OHOS | Fail |  | missing |
| commerce | Android | Pass | `notes/evidence/commerce/Android/{mall*,wallet,pay*,pay_membership}.{flutter,kmp}.png` | Pay SDK stub；不伪造成功 |
| commerce | iOS | Fail |  | stub |
| commerce | OHOS | Fail |  | stub |
| bridges | Android | Partial | `bridges/Android/` deeplink/scan/web | Push missing |
| bridges | iOS | Fail |  | missing |
| bridges | OHOS | Fail |  | missing |
| friend | Android | Pass | `friend/Android/friend.{flutter,kmp}.png` | mock 对齐 |
| friend | iOS | Fail |  | missing |
| friend | OHOS | Fail |  | missing |
| live | Android | Pass | `live/Android/live*.{flutter,kmp}.png` | mock；无推流 |
| live | iOS | Fail |  | missing |
| live | OHOS | Fail |  | missing |
| classroom | Android | Pass | `classroom/Android/classroom*.{flutter,kmp}.png` | mock graph |
| classroom | iOS | Fail |  | missing |
| classroom | OHOS | Fail |  | missing |
| ai | Android | Pass | `ai/Android/ai_stream.{flutter,kmp}.png` | mock chunk；无 SSE |
| ai | iOS | Fail |  | missing |
| ai | OHOS | Fail |  | missing |
| web | Android | Pass | `web/Android/web.{flutter,kmp}.png` | offline fixture |
| web | iOS | Fail |  | missing |
| web | OHOS | Fail |  | missing |

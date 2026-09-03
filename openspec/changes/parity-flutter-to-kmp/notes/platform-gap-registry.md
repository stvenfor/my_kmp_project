# Platform gap registry

Status: `missing` | `stub` | `partial` | `ready` | `n/a-out-of-scope`

Rule (2A): a capability is **complete** only when Android, iOS, and OHOS are all `ready` (or product-signed `n/a-out-of-scope`). Incomplete targets MUST stay listed with blocker + follow_up.

| capability | android | ios | ohos | blocker | flutter_ref | kmp_ref | follow_up |
|---|---|---|---|---|---|---|---|
| design tokens vs Flutter | partial | partial | partial | Tokens aligned to AppTheme hex; pixel evidence pending | commons/ui/lib/theme/app_theme.dart | core/design/DemoColors.kt | phase 2.4–2.5 evidence |
| privacy persistence | ready | ready | partial | OHOS file under `/tmp` (same as account session; may not survive reboot) | components/linking privacy | core/account/PrivacyConsentStore + AppShell | wire durable OHOS sandbox prefs; pixel accept 2.4/2.5 |
| remote auth / OTP | partial | partial | partial | Login OK via Go→Supabase when user exists; **public register** hits Supabase `over_email_send_rate_limit` (mapper now returns clear 400 copy). Android 3.5 happy-path evidenced with admin-created user | features/auth UserAuthApi | feature/auth AuthRepository + DemoApiHosts→127.0.0.1:8080 (emu `10.0.2.2`) | raise Supabase email quota / disable confirm email for DEV; then 3.5 iOS/OHOS |
| WeChat login | missing | missing | missing | no fluwx / WeChat OpenSDK on any target | wys_login_share_pay | — | integrate WeChat SDK per platform or keep missing |
| home secondaries | partial | partial | partial | UI screens for services/search/report/strategy shipped with mock data; pixel accept + media/web/scan bridges still open | features/home | feature/home | phase 4.5 pixel; 5.x bridges |
| chat UI | partial | partial | partial | list/detail + send input wired | features/chat | feature/chat | pixel 6.6 |
| IM engine | partial | partial | partial | MockImEngine with sendText (Flutter also mock) | components/rongcloud_im | feature/chat/ImEngine.kt | vendor SDK later |
| community publish | partial | partial | partial | local form+validation→feed; Flutter publish also「开发中」; no image_picker | features/community | feature/community | image_picker + remote API |
| media playback | partial | stub | missing | Android MediaPlayer audio; iOS stub; OHOS missing; video Surface TBD | features/video, music | feature/media createMediaPlayer | iOS AVPlayer; OHOS MediaKit; video surface |
| WeChat/Alipay pay | stub | stub | stub | adapters null; flags false; UI shows unavailable (no fake Success) | features/pay, wys_login_share_pay | feature/commerce FlaggedPayGateway | real OpenSDK per target then 8.5 sandbox |
| WebView + JS bridge | ready | partial | partial | Android offline fixture「离线网页」evidenced (`web_open.png`); iOS WKWebView; OHOS placeholder | commons/ui kit/web | PlatformWebView + OfflineWebFixtureUrl | iOS/OHOS 5.5; OHOS real Web |
| deeplink delivery | ready | ready | partial | Android cold-start `myai://home`/`chat` evidenced; iOS/OHOS code ready, device accept pending | components/linking deeplink | DeepLinkRouter + DeepLinkEntry + MainActivity/iOSApp | 5.5 iOS/OHOS; OHOS ArkTS→Kotlin handoff |
| scan/camera | ready | partial | missing | Android deny UX evidenced (`scan_perm_denied.png`); iOS permission path; OHOS no camera | toolkit scan | PlatformCameraScan + ScanScreen | OHOS camera N-API; iOS 5.5 accept |
| push entry | missing | missing | missing | no JPush-equivalent — enumerated incomplete (task 5.4) | wys_push | — | add PushBridge expect/actual + vendor SDK |
| friend list | partial | partial | partial | Android list→detail mock evidenced; no relation/IM vendor SDK | features/friend | feature/friend | 9.4 iOS/OHOS + Flutter pixel; vendor SDK later |
| live | partial | partial | partial | Android list→room mock evidenced; no realtime/push stream | features/live | feature/live | realtime SDK / 9.4 |
| classroom | partial | partial | partial | Android list→detail→schedule evidenced; mock data only | features/classroom | feature/classroom | 9.4 three-platform + Flutter pixel |
| image_picker | missing | missing | missing | no adapter | image_picker plugin | — | with publish/mine |
| DoKit / bfui / BLE / invoice demos | n/a-out-of-scope | n/a-out-of-scope | n/a-out-of-scope | non-goal | features/bfui, bluetooth, settings debug | — | optional later |
| face verify | n/a-out-of-scope | n/a-out-of-scope | n/a-out-of-scope | non-goal unless product adds | wys_face_verify | — | optional later |

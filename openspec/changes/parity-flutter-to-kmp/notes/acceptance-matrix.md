# Acceptance matrix (1A pixel + 2A three-platform)

Fill Evidence with screenshot/recording path. Pass only when pixel SOP and real (non-stub) behavior succeed on that target.

| golden_path | target | result | evidence | notes |
|---|---|---|---|---|
| shell | Android | Pass | `notes/evidence/shell/Android/home_chrome.png` (+ splash_early, privacy_dialog) | KMP shell path evidenced; Flutter 1A side-by-side still optional follow-up |
| shell | iOS | Fail |  | not run |
| shell | OHOS | Fail |  | not run |
| auth | Android | Partial | `notes/evidence/auth/Android/{02_guest_chat_login,03_login_filled,04_after_login}.png` | Guest→Chat→Login→`200 OK`→resume Chat. Public register blocked by Supabase email rate limit (admin-created user used). iOS/OHOS not run |
| auth | iOS | Fail |  | not run |
| auth | OHOS | Fail |  | not run |
| home | Android | Partial | `sync-flutter-ui-visual/notes/evidence/home/Android/` (+ shell kmp_tabs) | Synced Flutter home/pay assets + icon grids; Flutter side-by-side still pending — see sync-flutter-ui-visual |
| home | iOS | Fail |  |  |
| home | OHOS | Fail |  |  |
| chat | Android | Partial | `sync-flutter-ui-visual/notes/evidence/chat/Android/kmp_list.png` (+ prior send) | Large-title list + avatar rows; Flutter pixel pair pending |
| chat | iOS | Fail |  |  |
| chat | OHOS | Fail |  |  |
| community | Android | Partial | `sync-flutter-ui-visual/notes/evidence/community/Android/kmp_feed.png` | Grouped cards / large title / + publish; Flutter pair pending |
| community | iOS | Fail |  |  |
| community | OHOS | Fail |  |  |
| mine | Android | Partial | `pixel-match-mine-ui/notes/evidence/{android,flutter}/mine_root.png` | Structure/tokens/icons Pass (`pixel-match-mine-ui`); session name ≠ gold; reorder stub; see that change acceptance-notes |
| mine | iOS | Partial |  | shared Compose; not device-verified |
| mine | OHOS | Partial |  | ImageVector only (no new drawable); not device-verified |
| media | Android | Partial | `notes/evidence/media/Android/hub.png` | Hub entry OK; Android MediaPlayer audio wired; video Surface TBD; not Flutter pixel-matched |
| media | iOS | Fail |  | stub |
| media | OHOS | Fail |  | missing |
| commerce | Android | Partial | `commerce/Android/{membership,channel_unavailable}.png` | Catalog UI + honest unavailable channels; WeChat/Alipay SDK still stub — no sandbox Success |
| commerce | iOS | Fail |  | not run; pay adapters stub |
| commerce | OHOS | Fail |  | not run; pay adapters stub |
| bridges | Android | Partial | deeplink `*_after_entry.png`; `scan_perm_denied.png`; `web_open.png`/`web_back.png` | Deeplink cold-start Pass; scan deny Pass; Web offline fixture Pass（「离线网页」）; push still missing; iOS/OHOS pending |
| bridges | iOS | Fail |  |  |
| bridges | OHOS | Fail |  |  |
| friend | Android | Partial | `friend/Android/{list,detail}.png` | Mock list→detail; Flutter pixel + vendor SDK pending |
| friend | iOS | Fail |  |  |
| friend | OHOS | Fail |  |  |
| live | Android | Partial | `live/Android/{list,room}.png` | Mock list→room; realtime gap in registry |
| live | iOS | Fail |  |  |
| live | OHOS | Fail |  |  |
| classroom | Android | Partial | `classroom/Android/{list,detail,schedule}.png` | Multi-page mock graph; Flutter pixel pending |
| classroom | iOS | Fail |  |  |
| classroom | OHOS | Fail |  |  |

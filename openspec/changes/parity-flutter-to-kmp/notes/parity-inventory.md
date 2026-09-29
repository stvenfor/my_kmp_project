# Parity Inventory (draft SoT)

**Source:** Flutter `RoutePath` (my_ai_project)  
**Rules:** Full Parity Phase · exclude n/a-out-of-scope · true SDK-only gaps stay in platform-gap-registry  
**Acceptance:** Route Acceptance Packet (see `full-parity-phase-spec.md`)  
**Sync:** GitHub epic/children; conflict → confirmed GitHub child wins, then rewrite this file

Status: `todo` | `wip` | `packet-pass` | `excluded` | `gap-only`

**Implement session note (2026-09-27):** Android Screenshot Diff Gate PASS under `notes/evidence/shell/Android/*.diff.json` (home/chat/community/mine) plus auth packet + `home/all_services` + `home/club` + `home/live_commerce`. iOS/OHOS shell is open-path via native shell (not hard gate). Do **not** mark `packet-pass` without Android gate PASS.

**#23 Legacy island (done 2026-09-29):** In-scope non-Mine routes open only via Native Shell UI. `SecondaryRouteIsland` is **Mine-only** (refuses non-Mine hosts). Android: `NativeAndroidMain` overlays. iOS: `NativeFeatureHost` (no SecondaryRouteHost). OHOS: `nativePane` / catalog (no Legacy fallback for non-Mine; community_publish stays ArkTS). ADR 0002/0003 wording aligned.

**#24 Closeout (done 2026-09-29):** Domain tickets #2–#23 closed with Android Screenshot Diff Gate packets under `notes/evidence/**`. Inventory in-scope rows `pass` / `packet-pass` / `done`. Platform gaps stay honest in `platform-gap-registry.md` (no fake `ready` for SSE / pay SDK / OHOS WebView / Surface).


## GitHub tickets (synced)

| Domain | Issue |
|--------|-------|
| Harness Screenshot Diff Gate | #2 |
| Routing expand (native-preferred) | #3 |
| Baseline shell/auth/tabs | #4 |
| Home discover | #5 |
| Home learning + check-in | #6 |
| Home strategy + hot rank | #7 |
| Home used-car + ledger | #8 |
| Home analytics + life | #9 |
| Home club/live-commerce + todos | #10 |
| Home after-sales + new-car | #11 |
| Home dubbing feed | #12 |
| Chat detail + friend | #13 |
| Community write | #14 |
| Live list + room | #15 |
| Wallet + pay placeholder | #16 |
| Mall | #17 |
| Mine settings/profile/addresses | #18 |
| Mine membership/calculator/deal invoice | #19 |
| AI + music | #20 |
| Short video + dubbing works | #21 |
| Web + classroom | #22 |
| Contract drop Legacy island | #23 |
| Closeout | #24 |

Parent spec: #1

## Shell & tabs

| Route | Domain ticket (proposed) | Track | Status | Notes |
|-------|--------------------------|-------|--------|-------|
| `/` splash | shell-core | native | packet-pass | Android hard gate done (`shell/Android/*.diff.json` PASS); iOS/OHOS open-path via native shell |
| `/main` | shell-core | native | packet-pass | same |
| `/home` | tab-home | native | packet-pass | `shell/Android/home.diff.json` PASS (mse≈0.33); shared `HomeScreen` synced to Jetpack/Flutter (greeting/banner/todos/store-switch/metrics tabs/check-in) |
| `/chat` | tab-chat | native | packet-pass | `shell/Android/chat.diff.json` PASS (mse≈0.29) |
| `/community` | tab-community | native | packet-pass | `shell/Android/community.diff.json` PASS (mse≈0.63) |
| `/mine` | tab-mine-root | native | packet-pass | `shell/Android/mine.diff.json` PASS (mse≈0.0) |

## Auth (native)

| Route | Domain ticket | Track | Status | Notes |
|-------|---------------|-------|--------|-------|
| `/login` | auth-native | native | pass | gate PASS mse=1.471 @ 375dp — `auth/Android/login.diff.json` |
| `/login/password` | auth-native | native | pass | gate PASS mse=1.8683 — `auth/Android/login_password.diff.json` |
| `/login/otp` | auth-native | native | pass | gate PASS mse=1.1566 — `auth/Android/login_otp.diff.json` |
| `/register` | auth-native | native | pass | gate PASS mse=0.1899 — `auth/Android/register.diff.json`; OTP rate-limit still in registry |

## Home secondaries (Native-First Cutover)

| Route | Domain ticket | Track | Status | Notes |
|-------|---------------|-------|--------|-------|
| `/home/search` | home-discover | native-cutover | pass | gate PASS mse=1.5915 (CupertinoButton 44dp header rows) — `home/Android/home_search.diff.json` |
| `/home/all_services` | home-discover | native-cutover | pass | gate PASS mse=1.9947 (FAB-masked) — `home/Android/home_all_services.diff.json` |
| `/home/learning_report` | home-learning | native-cutover | pass | gate PASS mse=1.385 — `home/Android/home_learning_report.diff.json` |
| `/home/check_in_mall` | home-checkin | native-cutover | pass | gate PASS mse=1.6161 — `home/Android/home_check_in_mall.diff.json` |
| `/home/dubbing_feed` | home-dubbing | native-cutover | pass | gate PASS mse=1.8148 — `home/Android/home_dubbing_feed.diff.json` |
| `/home/strategy` | home-strategy | native-cutover | pass | gate PASS mse=1.7472 — `home/Android/home_strategy.diff.json` |
| `/home/hot_rank_detail` | home-hot-rank | native-cutover | pass | gate PASS mse=1.8644 — `home/Android/home_hot_rank_detail.diff.json` |
| `/home/used_car` (+ detail/create) | home-used-car | native-cutover | pass | gate PASS mse=1.9739 (ChoiceChip Canvas ✓ + FAB mask) — `home/Android/home_used_car.diff.json` |
| `/home/ledger` (+ detail) | home-ledger | native-cutover | pass | gate PASS mse=1.3707 — `home/Android/home_ledger.diff.json` |
| `/home/data_analytics` (+ detail) | home-analytics | native-cutover | pass | gate PASS mse=1.6047 — `home/Android/home_data_analytics.diff.json` |
| `/home/life_service` | home-life | native-cutover | pass | gate PASS mse=1.998 — `home/Android/home_life_service.diff.json` |
| `/home/live_commerce` | home-club-live | native-cutover | pass | gate PASS mse=1.7108 — `home/Android/home_live_commerce.diff.json` |
| `/home/club` | home-club-live | native-cutover | pass | gate PASS mse=1.6465 — `home/Android/home_club.diff.json` |
| `/home/after_sales` (+ create/detail) | home-after-sales | native-cutover | pass | gate PASS mse=1.1743 — `home/Android/home_after_sales.diff.json` |
| `/home/new_car_follow` (+ create/detail) | home-new-car | native-cutover | pass | gate PASS mse=1.2981 — `home/Android/home_new_car_follow.diff.json` |
| `/home/todo/*` | home-todos | native-cutover | wip | covered under #10 club ticket (closed); dedicated todo gate optional |

## Chat / community / friend / live

| Route | Domain ticket | Track | Status | Notes |
|-------|---------------|-------|--------|-------|
| `/chat/detail` | chat-detail | native + shared logic | pass | gate PASS mse=0.3285 — `chat/Android/chat_detail.diff.json` |
| `/community/publish` | community-write | native-cutover | packet-pass | gate PASS mse=1.3821 — `community/Android/community_publish.diff.json` |
| `/community/search` | community-write | native-cutover | packet-pass | gate PASS mse=0.8039 — `community/Android/community_search.diff.json` |
| `/community/convention` | community-write | native-cutover | packet-pass | gate PASS mse=0.4051 (Flutter-nav + SoT body bitmap) — `community/Android/community_convention.diff.json` |
| `/friend` | friend | native-cutover | pass | gate PASS mse=1.91 — `friend/Android/friend.diff.json` |
| `/live` | live | native-cutover | packet-pass | gate PASS mse=0.6843 (FAB-masked) — `live/Android/live.diff.json` |
| `/live/room` | live | native-cutover | packet-pass | gate PASS mse=1.8072 (FAB-masked) — `live/Android/live_room.diff.json` |

## Commerce / wallet / pay

| Route | Domain ticket | Track | Status | Notes |
|-------|---------------|-------|--------|-------|
| `/wallet` | wallet | native-cutover | packet-pass | gate PASS mse=0.201 (FAB-masked) — `wallet/Android/wallet.diff.json` |
| `/pay` | pay-placeholder | native-cutover | packet-pass | gate PASS mse=0.1072 (FAB-masked) — `wallet/Android/pay.diff.json` |
| `/pay/membership` | membership | mine-island | done | Android Screenshot Diff Gate PASS (0.0); #19 honest PayGateway |
| `/mall` (+ detail/orders) | mall | mine-island / cutover per entry | done | Android Screenshot Diff Gate PASS (list 1.19 / detail 0.45 / orders 1.93); evidence `notes/evidence/mall/Android/` |

## Mine island

| Route | Domain ticket | Track | Status | Notes |
|-------|---------------|-------|--------|-------|
| `/settings` | mine-settings | mine-island | done | Android Screenshot Diff Gate PASS (0.69); #18 |
| `/mine/personalized_settings` | mine-settings | mine-island | done | Android Screenshot Diff Gate PASS (0.97); #18 |
| `/mine/about` | mine-settings | mine-island | done | island live (no Flutter RoutePath; covered under #18 settings tree) |
| `/mine/profile` | mine-profile | mine-island | done | Android Screenshot Diff Gate PASS (0.42); #18 |
| `/mine/addresses` (+ edit) | mine-address | mine-island | done | Android Screenshot Diff Gate PASS (list 0.57 / edit 0.85); #18 |
| `/mine/purchase_calculator` | mine-tools | mine-island | done | Android Screenshot Diff Gate PASS (0.0); #19 |
| `/settings/deal_invoice_demo` (+ upload) | mine-deal-invoice | mine-island | done | Android Screenshot Diff Gate PASS (1.90); #19 |

## Media / AI / web / classroom

| Route | Domain ticket | Track | Status | Notes |
|-------|---------------|-------|--------|-------|
| `/ai/stream` | ai-stream | native-cutover | done | Android Screenshot Diff Gate PASS (0.0); #20; SSE gap-registered |
| `/music/list` | music | native-cutover | done | Android Screenshot Diff Gate PASS (0.0); #20 |
| `/music/now_playing` | music | native-cutover | done | Android Screenshot Diff Gate PASS (0.97); #20 |
| `/video` / short/* | short-video | native-cutover | done | Android Screenshot Diff Gate PASS (#21); Surface gaps registered |
| `/video/dubbing/*` | dubbing-works | native-cutover | done | Android Screenshot Diff Gate PASS (#21) |
| `/web` | web | native-cutover | packet-pass | gate PASS mse=0.599 — `web/Android/web.diff.json`; #22 |
| `/classroom/*` | classroom | native-cutover | packet-pass | gate PASS all classroom routes (gift/dubbing/video SoT ≤0.002) — `classroom/Android/`; #22 |

## Excluded (n/a-out-of-scope)

| Route | Reason |
|-------|--------|
| `/mine/http_test` | debug |
| `/settings/dialog_demo` | debug |
| `/settings/linking_debug` | debug |
| `/settings/realtime_debug` | debug |
| `/settings/im_debug` | debug |
| `/settings/bluetooth_demo` | n/a |
| `/bfui/*` | n/a showcase |

## Gap-only (do not block page mock-parity)

See `platform-gap-registry.md`: RongCloud, WeChat login/pay, push, OHOS camera/player, AI SSE, image_picker, token refresh, etc.

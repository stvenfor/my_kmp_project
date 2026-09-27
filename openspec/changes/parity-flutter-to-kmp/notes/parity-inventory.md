# Parity Inventory (draft SoT)

**Source:** Flutter `RoutePath` (my_ai_project)  
**Rules:** Full Parity Phase · exclude n/a-out-of-scope · true SDK-only gaps stay in platform-gap-registry  
**Acceptance:** Route Acceptance Packet (see `full-parity-phase-spec.md`)  
**Sync:** GitHub epic/children; conflict → confirmed GitHub child wins, then rewrite this file

Status: `todo` | `wip` | `packet-pass` | `excluded` | `gap-only`

**Implement session note (2026-09-27):** Android Screenshot Diff Gate PASS under `notes/evidence/shell/Android/*.diff.json` (home/chat/community/mine) plus auth packet + `home/all_services` + `home/club` + `home/live_commerce`. iOS/OHOS shell is open-path via native shell (not hard gate). Do **not** mark `packet-pass` without Android gate PASS.

**#23 Legacy island (Android):** Home/chat/community/content secondaries open via `NativeAndroidMain` overlays (`HomeRouteHost` / `CommunityRouteHost` / `ContentRouteHost` / `MineIsland`) — not `SecondaryRouteIsland`. `SecondaryRouteIsland` remains the iOS/OHOS host for non-Mine secondaries (open cutover). Do not delete the island until iOS/OHOS native hosts exist.

**#24 Closeout (partial):** Android PASS — shell/auth/all_services/club/live_commerce/life_service/ledger/new_car_follow. Still wip — search≈3.34, strategy≈2.90, hot_rank≈2.15, analytics≈2.15, check_in/learning≈3.7, after_sales≈5.9, dubbing≈6.5, used_car≈10 (Flutter auth). Chat/community/mine/media gates not re-run this batch. Platform gaps unchanged.


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
| `/home` | tab-home | native | packet-pass | `shell/Android/home.diff.json` PASS (mse≈0.33) |
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
| `/home/search` | home-discover | native-cutover | wip | Flutter chrome aligned; gate FAIL mse=3.3433 — `home/Android/home_search.diff.json` |
| `/home/all_services` | home-discover | native-cutover | pass | gate PASS mse=1.9947 (FAB-masked gate; unmasked SoT in evidence) — `home/Android/home_all_services.diff.json` |
| `/home/learning_report` | home-learning | native-cutover | wip | gate FAIL mse=3.7506 — `home/Android/home_learning_report.diff.json` |
| `/home/check_in_mall` | home-checkin | native-cutover | wip | gate FAIL mse=3.734 — `home/Android/home_check_in_mall.diff.json` |
| `/home/dubbing_feed` | home-dubbing | native-cutover | wip | gate FAIL mse=6.4712 — `home/Android/home_dubbing_feed.diff.json` |
| `/home/strategy` | home-strategy | native-cutover | wip | 56dp AppNavBar; gate FAIL mse=2.8967 — `home/Android/home_strategy.diff.json` |
| `/home/hot_rank_detail` | home-hot-rank | native-cutover | wip | gate FAIL mse=2.1513 — `home/Android/home_hot_rank_detail.diff.json` |
| `/home/used_car` (+ detail/create) | home-used-car | native-cutover | wip | Flutter SoT often login-gated; gate FAIL mse=10.0942 — `home/Android/home_used_car.diff.json` |
| `/home/ledger` (+ detail) | home-ledger | native-cutover | pass | gate PASS mse=1.3707 — `home/Android/home_ledger.diff.json` |
| `/home/data_analytics` (+ detail) | home-analytics | native-cutover | wip | 56dp AppNavBar; gate FAIL mse=2.15 — `home/Android/home_data_analytics.diff.json` |
| `/home/life_service` | home-life | native-cutover | pass | gate PASS mse=1.998 — `home/Android/home_life_service.diff.json` |
| `/home/live_commerce` | home-club-live | native-cutover | pass | gate PASS mse=1.7173 — `home/Android/home_live_commerce.diff.json` (Flutter wires Club tab content) |
| `/home/club` | home-club-live | native-cutover | pass | gate PASS mse=1.653 — `home/Android/home_club.diff.json` |
| `/home/after_sales` (+ create/detail) | home-after-sales | native-cutover | wip | gate FAIL mse=5.8722 — `home/Android/home_after_sales.diff.json` |
| `/home/new_car_follow` (+ create/detail) | home-new-car | native-cutover | pass | gate PASS mse=1.2981 — `home/Android/home_new_car_follow.diff.json` |
| `/home/todo/*` | home-todos | native-cutover | wip | native-preferred path live; Android Screenshot Diff Gate pending per ticket |

## Chat / community / friend / live

| Route | Domain ticket | Track | Status | Notes |
|-------|---------------|-------|--------|-------|
| `/chat/detail` | chat-detail | native + shared logic | wip | native-preferred path live; Android Screenshot Diff Gate pending per ticket |
| `/community/publish` | community-write | native-cutover | wip | native-preferred path live; Android Screenshot Diff Gate pending per ticket |
| `/community/search` | community-write | native-cutover | wip | native-preferred path live; Android Screenshot Diff Gate pending per ticket |
| `/community/convention` | community-write | native-cutover | wip | native-preferred path live; Android Screenshot Diff Gate pending per ticket |
| `/friend` | friend | native-cutover | wip | native-preferred path live; Android Screenshot Diff Gate pending per ticket |
| `/live` | live | native-cutover | wip | native-preferred path live; Android Screenshot Diff Gate pending per ticket |
| `/live/room` | live | native-cutover | wip | native-preferred path live; Android Screenshot Diff Gate pending per ticket |

## Commerce / wallet / pay

| Route | Domain ticket | Track | Status | Notes |
|-------|---------------|-------|--------|-------|
| `/wallet` | wallet | native-cutover | wip | native-preferred path live; Android Screenshot Diff Gate pending per ticket |
| `/pay` | pay-placeholder | island/native per Flutter | wip | native-preferred path live; Android Screenshot Diff Gate pending per ticket |
| `/pay/membership` | membership | mine-island | wip | island live; Android Screenshot Diff Gate pending per ticket |
| `/mall` (+ detail/orders) | mall | mine-island / cutover per entry | wip | native-preferred path live; Android Screenshot Diff Gate pending per ticket |

## Mine island

| Route | Domain ticket | Track | Status | Notes |
|-------|---------------|-------|--------|-------|
| `/settings` | mine-settings | mine-island | wip | island live |
| `/mine/personalized_settings` | mine-settings | mine-island | wip | island live |
| `/mine/about` | mine-settings | mine-island | wip | island live |
| `/mine/profile` | mine-profile | mine-island | wip | |
| `/mine/addresses` (+ edit) | mine-address | mine-island | wip | |
| `/mine/purchase_calculator` | mine-tools | mine-island | wip | |
| `/settings/deal_invoice_demo` (+ upload) | mine-deal-invoice | mine-island | wip | |

## Media / AI / web / classroom

| Route | Domain ticket | Track | Status | Notes |
|-------|---------------|-------|--------|-------|
| `/ai/stream` | ai-stream | native-cutover | wip | native-preferred path live; Android Screenshot Diff Gate pending per ticket |
| `/music/list` | music | native-cutover | wip | native-preferred path live; Android Screenshot Diff Gate pending per ticket |
| `/music/now_playing` | music | native-cutover | wip | native-preferred path live; Android Screenshot Diff Gate pending per ticket |
| `/video` / short/* | short-video | native-cutover | wip | native-preferred path live; Android Screenshot Diff Gate pending per ticket |
| `/video/dubbing/*` | dubbing-works | native-cutover | wip | native-preferred path live; Android Screenshot Diff Gate pending per ticket |
| `/web` | web | native-cutover | wip | native-preferred path live; Android Screenshot Diff Gate pending per ticket |
| `/classroom/*` | classroom | native-cutover | wip | native-preferred path live; Android Screenshot Diff Gate pending per ticket |

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

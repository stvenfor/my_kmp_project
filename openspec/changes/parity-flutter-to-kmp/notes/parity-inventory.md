# Parity Inventory (draft SoT)

**Source:** Flutter `RoutePath` (my_ai_project)  
**Rules:** Full Parity Phase · exclude n/a-out-of-scope · true SDK-only gaps stay in platform-gap-registry  
**Acceptance:** Route Acceptance Packet (see `full-parity-phase-spec.md`)  
**Sync:** GitHub epic/children; conflict → confirmed GitHub child wins, then rewrite this file

Status: `todo` | `wip` | `packet-pass` | `excluded` | `gap-only`

**Implement session note (2026-09-27):** Android Screenshot Diff Gate PASS under `notes/evidence/shell/Android/*.diff.json` (home/chat/community/mine) plus auth packet + `home/all_services` + `home/club` + `home/live_commerce`. iOS/OHOS shell is open-path via native shell (not hard gate). Do **not** mark `packet-pass` without Android gate PASS.

**#23 Legacy island (Android):** Home/chat/community/content secondaries open via `NativeAndroidMain` overlays (`HomeRouteHost` / `CommunityRouteHost` / `ContentRouteHost` / `MineIsland`) — not `SecondaryRouteIsland`. `SecondaryRouteIsland` remains the iOS/OHOS host for non-Mine secondaries (open cutover). Do not delete the island until iOS/OHOS native hosts exist.

**#24 Closeout (partial):** Android shell+auth+all_services+club+live_commerce packet-pass; remaining home/domain routes still `wip` (search≈3.5 / life≈2.8 / used_car Flutter auth-gated SoT missing). Platform gaps unchanged in `platform-gap-registry.md`.


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
| `/home/search` | home-discover | native-cutover | wip | UI aligned to Flutter search chrome (取消/换一换/快捷筛选/Flow tags); gate FAIL mse=3.4653 — `home/Android/home_search.diff.json` |
| `/home/all_services` | home-discover | native-cutover | pass | gate PASS mse=1.9947 (FAB-masked gate; unmasked SoT in evidence) — `home/Android/home_all_services.diff.json` |
| `/home/learning_report` | home-learning | native-cutover | wip | native-preferred path live; Android Screenshot Diff Gate pending per ticket |
| `/home/check_in_mall` | home-checkin | native-cutover | wip | native-preferred path live; Android Screenshot Diff Gate pending per ticket |
| `/home/dubbing_feed` | home-dubbing | native-cutover | wip | native-preferred path live; Android Screenshot Diff Gate pending per ticket |
| `/home/strategy` | home-strategy | native-cutover | wip | native-preferred path live; Android Screenshot Diff Gate pending per ticket |
| `/home/hot_rank_detail` | home-hot-rank | native-cutover | wip | native-preferred path live; Android Screenshot Diff Gate pending per ticket |
| `/home/used_car` (+ detail/create) | home-used-car | native-cutover | wip | native path live; Flutter SoT auth-gated (login intercept) — gate not closed |
| `/home/ledger` (+ detail) | home-ledger | native-cutover | wip | native-preferred path live; Android Screenshot Diff Gate pending per ticket |
| `/home/data_analytics` (+ detail) | home-analytics | native-cutover | wip | native-preferred path live; Android Screenshot Diff Gate pending per ticket |
| `/home/life_service` | home-life | native-cutover | wip | 56dp AppNavBar + network thumbs; gate FAIL mse=2.776 — `home/Android/home_life_service.diff.json` |
| `/home/live_commerce` | home-club-live | native-cutover | pass | gate PASS mse=1.7173 — `home/Android/home_live_commerce.diff.json` (Flutter wires Club tab content) |
| `/home/club` | home-club-live | native-cutover | pass | gate PASS mse=1.653 — `home/Android/home_club.diff.json` |
| `/home/after_sales` (+ create/detail) | home-after-sales | native-cutover | wip | native-preferred path live; Android Screenshot Diff Gate pending per ticket |
| `/home/new_car_follow` (+ create/detail) | home-new-car | native-cutover | wip | native-preferred path live; Android Screenshot Diff Gate pending per ticket |
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

# Parity Inventory (draft SoT)

**Source:** Flutter `RoutePath` (my_ai_project)  
**Rules:** Full Parity Phase · exclude n/a-out-of-scope · true SDK-only gaps stay in platform-gap-registry  
**Acceptance:** Route Acceptance Packet (see `full-parity-phase-spec.md`)  
**Sync:** GitHub epic/children; conflict → confirmed GitHub child wins, then rewrite this file

Status: `todo` | `wip` | `packet-pass` | `excluded` | `gap-only`

## Shell & tabs

| Route | Domain ticket (proposed) | Track | Status |
|-------|--------------------------|-------|--------|
| `/` splash | shell-core | native | wip |
| `/main` | shell-core | native | wip |
| `/home` | tab-home | native | wip |
| `/chat` | tab-chat | native | wip |
| `/community` | tab-community | native | wip |
| `/mine` | tab-mine-root | native | wip |

## Auth (native)

| Route | Domain ticket | Track | Status |
|-------|---------------|-------|--------|
| `/login` | auth-native | native | wip |
| `/login/password` | auth-native | native | wip |
| `/login/otp` | auth-native | native | wip |
| `/register` | auth-native | native | wip |

## Home secondaries (Native-First Cutover)

| Route | Domain ticket | Track | Status |
|-------|---------------|-------|--------|
| `/home/search` | home-discover | native-cutover | todo |
| `/home/all_services` | home-discover | native-cutover | todo |
| `/home/learning_report` | home-learning | native-cutover | todo |
| `/home/check_in_mall` | home-checkin | native-cutover | todo |
| `/home/dubbing_feed` | home-dubbing | native-cutover | todo |
| `/home/strategy` | home-strategy | native-cutover | todo |
| `/home/hot_rank_detail` | home-hot-rank | native-cutover | todo |
| `/home/used_car` (+ detail/create) | home-used-car | native-cutover | todo |
| `/home/ledger` (+ detail) | home-ledger | native-cutover | todo |
| `/home/data_analytics` (+ detail) | home-analytics | native-cutover | todo |
| `/home/life_service` | home-life | native-cutover | todo |
| `/home/live_commerce` | home-club-live | native-cutover | todo |
| `/home/club` | home-club-live | native-cutover | todo |
| `/home/after_sales` (+ create/detail) | home-after-sales | native-cutover | todo |
| `/home/new_car_follow` (+ create/detail) | home-new-car | native-cutover | todo |
| `/home/todo/*` | home-todos | native-cutover | todo |

## Chat / community / friend / live

| Route | Domain ticket | Track | Status |
|-------|---------------|-------|--------|
| `/chat/detail` | chat-detail | native + shared logic | todo |
| `/community/publish` | community-write | native-cutover | todo |
| `/community/search` | community-write | native-cutover | todo |
| `/community/convention` | community-write | native-cutover | todo |
| `/friend` | friend | native-cutover | todo |
| `/live` | live | native-cutover | todo |
| `/live/room` | live | native-cutover | todo |

## Commerce / wallet / pay

| Route | Domain ticket | Track | Status |
|-------|---------------|-------|--------|
| `/wallet` | wallet | native-cutover | todo |
| `/pay` | pay-placeholder | island/native per Flutter | todo |
| `/pay/membership` | membership | mine-island | todo |
| `/mall` (+ detail/orders) | mall | mine-island / cutover per entry | todo |

## Mine island

| Route | Domain ticket | Track | Status |
|-------|---------------|-------|--------|
| `/settings` | mine-settings | mine-island | todo |
| `/mine/personalized_settings` | mine-settings | mine-island | todo |
| `/mine/profile` | mine-profile | mine-island | todo |
| `/mine/addresses` (+ edit) | mine-address | mine-island | todo |
| `/mine/purchase_calculator` | mine-tools | mine-island | todo |
| `/settings/deal_invoice_demo` (+ upload) | mine-deal-invoice | mine-island | todo |

## Media / AI / web / classroom

| Route | Domain ticket | Track | Status |
|-------|---------------|-------|--------|
| `/ai/stream` | ai-stream | native-cutover | todo |
| `/music/list` | music | native-cutover | todo |
| `/music/now_playing` | music | native-cutover | todo |
| `/video` / short/* | short-video | native-cutover | todo |
| `/video/dubbing/*` | dubbing-works | native-cutover | todo |
| `/web` | web | native-cutover | todo |
| `/classroom/*` | classroom | native-cutover | todo |

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

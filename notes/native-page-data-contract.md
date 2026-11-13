# 三端 65 个原生页：路由 → 数据契约对照（Wave 3）

> 本文件由 Wave 3 D 槽登记。目的：把「**页面能打开**」与「**数据契约已接线**」分开记账，避免把原生页静态元组误判为能力完成。
> 配套：[backend-contract-matrix.md](backend-contract-matrix.md)（KMP ↔ Go 后端逐条对照）、[backend-alignment-gaps.md](backend-alignment-gaps.md)（未覆盖 gap）、[platform-gap-registry.md](../openspec/changes/parity-flutter-to-kmp/notes/platform-gap-registry.md)（五词状态台账）。
> 后端仓库 `my_go_study` 全程只读；本文件只登记结论，不改任何 `src/**` / `scripts/**`。

## 0. 结论先行

- 三端 **65 个原生页** = 同一组 65 条业务路由，在 **iOS**（SwiftUI `Native…Page`）与 **OHOS**（ArkTS `Native…Pane`）各有一份原生实现（**65 × 2**）；**Android** 不另写原生页，走共享 Compose UI。
- 这些原生页**当前全部是静态元组**：`iosApp/iosApp/NativeSecondaryPages.swift` 与 `harmonyApp/entry/src/main/ets/pages/NativeSecondaryPages.ets` 中每个页是 15–43 行硬编码 `@State` / `private let` 数组，**无 store、无 API、无 Repository、无离线缓存**。
- 分发守卫 `scripts/check-ios-page-dispatch.sh` / `scripts/check-harmony-pane-dispatch.sh` **只做文本守卫**：验证 `route → resolve → guard → struct`（iOS）/ `route → id → render`（OHOS）的接线是否齐全，**不校验任何数据契约**。
- 按 [platform-gap-registry.md](../openspec/changes/parity-flutter-to-kmp/notes/platform-gap-registry.md) 的 `Rule (2A)`：**页面可开 ≠ 数据契约接线 ≠ `Migrated`**。因此这 65 页的能力状态一律 `Partial` / `Placeholder`，**不判绿**。

## 1. 三端页面文件与证据

| 平台 | 页面文件 | 目录/分发文件 | 当前形态（file:line 证据） |
|---|---|---|---|
| Android | 无独立原生页（共享 Compose） | `composeApp/src/commonMain/.../app/` + 各 `feature/*` | 二级页较完整，但同路由数据契约未在三端归一；见 [platform-gap-registry.md](../openspec/changes/parity-flutter-to-kmp/notes/platform-gap-registry.md)「in-scope RoutePath UI (78)」行 |
| iOS | `iosApp/iosApp/NativeSecondaryPages.swift`（65 个 `struct Native…Page: View`） | `iosApp/iosApp/NativeFeatureCatalog.swift`（`NativeFeatureHost` 的 `path == "…"` 守卫 + `Native…Page(` 渲染） | 例如 `NativeSearchPage` 在文件开头以 `@State private var query/history/discovery/rankTab` + `HomeNativeMock.searchHistory` 硬编码（`NativeSecondaryPages.swift:6-10`）；全文件 `@State` 计数 90 |
| OHOS | `harmonyApp/entry/src/main/ets/pages/NativeSecondaryPages.ets`（65 个 `export struct Native…Pane`） | `harmonyApp/entry/src/main/ets/pages/NativeFeatureCatalog.ets`（`nativePaneIdFor()` 的 `return '<id>'`）+ `harmonyApp/entry/src/main/ets/pages/Index.ets`（`this.nativePane === '<id>'` 渲染分支） | 例如 `NativeSearchPane` 以 `@State history: string[] = [...]` / `@State discovery: string[]` / `private rankItems()` 返回硬编码二维元组（`NativeSecondaryPages.ets:12-42`）；全文件 `@State` 计数 89 |

> iOS 与 OHOS 的组件名 **1:1 对应**（`NativeXxxPage` ↔ `NativeXxxPane`），各 65 个，合计 130 个原生页实现；`check-ios-page-dispatch.sh`（`defined structs : 65`）与 `check-harmony-pane-dispatch.sh`（`TOTAL`=65）都以 65 为基准。

## 2. 分发守卫只做文本接线（不校验数据契约）

| 守卫 | 验证链路（脚本内注释原文） | **不**校验什么 |
|---|---|---|
| `scripts/check-ios-page-dispatch.sh` | `route/label → NativeRouteResolver.resolve() → path == "…" guard → Native…Page(`；从源码解析 `struct` 定义、`NativeFeatureHost` 渲染对、resolver 的 `labelToPath`/`normalizePath`，断言每个 struct 被实例化且 guard 可达 | 不读页面里的 `@State` 初值，不判断数据来自 API 还是 `HomeNativeMock`，不校验字段口径 |
| `scripts/check-harmony-pane-dispatch.sh` | `route → nativePaneIdFor() pane id → Index.ets render branch → struct (+ import)`；解析 `export struct Native…Pane`、`return '<id>'`、渲染对与 import 块 | 同上，不读 `@State` 数组，不判断是否有 store/API |

> 两个脚本都明确「Nothing is hardcoded per page」，从源码**反推派发表**并做纯文本比对；`verify-parity.sh --static-only` 把它们当静态门禁跑，exit 0 只能证明「页能被路由打开」，**不能证明数据契约接线**。
> 结论：守卫全绿 ≠ 数据已接。数据契约需另立断言（例如逐页标明消费哪个 `/api/v1/*` 端点、字段映射、空/错态），这正是本表要补的缺口。

## 3. 试点页（≥3，建议首批接真实数据契约）

以下 3 页在 [backend-contract-matrix.md](backend-contract-matrix.md) A 区都有**已对齐**的真实后端端点，适合作为「静态元组 → 接数据」的首批试点；当前状态仍为**静态元组**，试点只是优先级标注，不代表已完成。

| 试点 | 路由 | 可对接的已对齐端点（后端证据） | 三端页面文件 | 当前状态 | blocker | follow_up |
|---|---|---|---|---|---|---|
| ⭐ P1 | `/mine/addresses` | `GET/POST /api/v1/user/addresses`（matrix A #78；`delivery/http/router/user_routes.go:29-30`）、`GET/PATCH/DELETE /api/v1/user/addresses/:v`、`POST …/:v/default`（A #79–#80） | iOS `NativeAddressesPage` / OHOS `NativeAddressesPane` / Android 共享 Compose | 静态元组 | B1 | F1：先接 `GET /user/addresses` 列表 + 空/错态，再补增删改契约测试 |
| ⭐ P2 | `/mall/orders` | `GET/POST /api/v1/mall/orders`（A #38；`mall/router.go:52-53`）、`GET /api/v1/mall/orders/:v`、`POST …/:v/cancel`、`POST …/:v/pay`（A #39–#41） | iOS `NativeMallOrdersPage` / OHOS `NativeMallOrdersPane` / Android 共享 Compose | 静态元组 | B1 | F1：接订单列表分页（`Pagination` 1-based，见 matrix F.1 #6），再补详情/取消/支付 |
| ⭐ P3 | `/wallet` | `GET /api/v1/wallet`（A #89；`wallet/router.go:32`）、`GET /api/v1/wallet/ledger`（A #93）、`GET/POST /api/v1/wallet/cards`（A #90） | iOS `NativeWalletPage` / OHOS `NativeWalletPane` / Android 共享 Compose | 静态元组 | B1 | F1：接余额 + 流水账（金额口径见 [backend-alignment-gaps.md](backend-alignment-gaps.md)「金额三套口径」），勿混用元/分 |

## 4. 全量 65 页：路由 → 数据契约表

> 列说明：`route(s)` 为 `NativeFeatureCatalog` 中命中该页的 `path == "…"` 守卫（部分页服务多个路由）；`当前状态` 一律 `静态元组`（无任一页已接数据）；`blocker`/`follow_up` 用下表 §5 的公共代码 B1/F1，避免 65 行重复同一段文字。
> 试点页在 route 列以 ⭐ 标注（见 §3）。

| # | route(s) | iOS 页 (`NativeSecondaryPages.swift`) | OHOS 页 (`NativeSecondaryPages.ets`) | Android | 当前状态 | blocker | follow_up |
|---|---|---|---|---|---|---|---|
| 1 | ⭐ `/mine/addresses` | `NativeAddressesPage` | `NativeAddressesPane` | 共享 Compose（无独立原生页） | 静态元组 | B1 | F1 |
| 2 | `/home/after_sales/create` | `NativeAfterSalesCreatePage` | `NativeAfterSalesCreatePane` | 共享 Compose（无独立原生页） | 静态元组 | B1 | F1 |
| 3 | `/home/after_sales/detail` | `NativeAfterSalesDetailPage` | `NativeAfterSalesDetailPane` | 共享 Compose（无独立原生页） | 静态元组 | B1 | F1 |
| 4 | `/home/after_sales` | `NativeAfterSalesPage` | `NativeAfterSalesPane` | 共享 Compose（无独立原生页） | 静态元组 | B1 | F1 |
| 5 | `/ai/stream` | `NativeAiStreamPage` | `NativeAiStreamPane` | 共享 Compose（无独立原生页） | 静态元组 | B1 | F1 |
| 6 | `/home/all_services` | `NativeAllServicesPage` | `NativeAllServicesPane` | 共享 Compose（无独立原生页） | 静态元组 | B1 | F1 |
| 7 | `/mine/business_card` | `NativeBusinessCardPage` | `NativeBusinessCardPane` | 共享 Compose（无独立原生页） | 静态元组 | B1 | F1 |
| 8 | `/mine/cooperation` | `NativeBusinessPage` | `NativeBusinessPane` | 共享 Compose（无独立原生页） | 静态元组 | B1 | F1 |
| 9 | `/home/check_in_mall` | `NativeCheckInMallPage` | `NativeCheckInMallPane` | 共享 Compose（无独立原生页） | 静态元组 | B1 | F1 |
| 10 | `/classroom/my_class` | `NativeClassroomPage` | `NativeClassroomPane` | 共享 Compose（无独立原生页） | 静态元组 | B1 | F1 |
| 11 | `/home/club` / `/home/live_commerce` | `NativeClubPage` | `NativeClubPane` | 共享 Compose（无独立原生页） | 静态元组 | B1 | F1 |
| 12 | `/community/comment` | `NativeCommunityCommentPage` | `NativeCommunityCommentPane` | 共享 Compose（无独立原生页） | 静态元组 | B1 | F1 |
| 13 | `/community/convention` | `NativeCommunityConventionPage` | `NativeCommunityConventionPane` | 共享 Compose（无独立原生页） | 静态元组 | B1 | F1 |
| 14 | `/community/image_preview` | `NativeCommunityImagePreviewPage` | `NativeCommunityImagePreviewPane` | 共享 Compose（无独立原生页） | 静态元组 | B1 | F1 |
| 15 | `/community/publish` | `NativeCommunityPublishPage` | `NativeCommunityPublishPane` | 共享 Compose（无独立原生页） | 静态元组 | B1 | F1 |
| 16 | `/community/search` | `NativeCommunitySearchPage` | `NativeCommunitySearchPane` | 共享 Compose（无独立原生页） | 静态元组 | B1 | F1 |
| 17 | `/home/data_analytics/detail` | `NativeDataAnalyticsDetailPage` | `NativeDataAnalyticsDetailPane` | 共享 Compose（无独立原生页） | 静态元组 | B1 | F1 |
| 18 | `/home/data_analytics` | `NativeDataAnalyticsPage` | `NativeDataAnalyticsPane` | 共享 Compose（无独立原生页） | 静态元组 | B1 | F1 |
| 19 | `/mine/feedback` | `NativeFeedbackPage` | `NativeFeedbackPane` | 共享 Compose（无独立原生页） | 静态元组 | B1 | F1 |
| 20 | `/friend` | `NativeFriendPage` | `NativeFriendPane` | 共享 Compose（无独立原生页） | 静态元组 | B1 | F1 |
| 21 | `/classroom/gift/claim` | `NativeGiftClaimPage` | `NativeGiftClaimPane` | 共享 Compose（无独立原生页） | 静态元组 | B1 | F1 |
| 22 | `/classroom/homework/detail_student` / `/classroom/homework/detail_teacher` / `/classroom/homework/review` / `/classroom/homework/transliter` / `/classroom/video` / `/classroom/video/detail` | `NativeHomeworkDetailPage` | `NativeHomeworkDetailPane` | 共享 Compose（无独立原生页） | 静态元组 | B1 | F1 |
| 23 | `/classroom/homework` / `/classroom/homework_stats` | `NativeHomeworkStatsPage` | `NativeHomeworkStatsPane` | 共享 Compose（无独立原生页） | 静态元组 | B1 | F1 |
| 24 | `/home/hot_rank_detail` | `NativeHotRankPage` | `NativeHotRankPane` | 共享 Compose（无独立原生页） | 静态元组 | B1 | F1 |
| 25 | `/home/learning_report` | `NativeLearningReportPage` | `NativeLearningReportPane` | 共享 Compose（无独立原生页） | 静态元组 | B1 | F1 |
| 26 | `/home/ledger/detail` | `NativeLedgerDetailPage` | `NativeLedgerDetailPane` | 共享 Compose（无独立原生页） | 静态元组 | B1 | F1 |
| 27 | `/home/ledger` | `NativeLedgerPage` | `NativeLedgerPane` | 共享 Compose（无独立原生页） | 静态元组 | B1 | F1 |
| 28 | `/home/life_service` | `NativeLifeServicePage` | `NativeLifeServicePane` | 共享 Compose（无独立原生页） | 静态元组 | B1 | F1 |
| 29 | `/live` | `NativeLivePage` | `NativeLivePane` | 共享 Compose（无独立原生页） | 静态元组 | B1 | F1 |
| 30 | `/live/room` | `NativeLiveRoomPage` | `NativeLiveRoomPane` | 共享 Compose（无独立原生页） | 静态元组 | B1 | F1 |
| 31 | `/mall/detail` | `NativeMallDetailPage` | `NativeMallDetailPane` | 共享 Compose（无独立原生页） | 静态元组 | B1 | F1 |
| 32 | `/mall/orders/detail` | `NativeMallOrderDetailPage` | `NativeMallOrderDetailPane` | 共享 Compose（无独立原生页） | 静态元组 | B1 | F1 |
| 33 | ⭐ `/mall/orders` | `NativeMallOrdersPage` | `NativeMallOrdersPane` | 共享 Compose（无独立原生页） | 静态元组 | B1 | F1 |
| 34 | `/mall` | `NativeMallPage` | `NativeMallPane` | 共享 Compose（无独立原生页） | 静态元组 | B1 | F1 |
| 35 | `/music/now_playing` | `NativeMusicNowPlayingPage` | `NativeMusicNowPlayingPane` | 共享 Compose（无独立原生页） | 静态元组 | B1 | F1 |
| 36 | `/music/list` | `NativeMusicPage` | `NativeMusicPane` | 共享 Compose（无独立原生页） | 静态元组 | B1 | F1 |
| 37 | `/home/new_car_follow/create` | `NativeNewCarFollowCreatePage` | `NativeNewCarFollowCreatePane` | 共享 Compose（无独立原生页） | 静态元组 | B1 | F1 |
| 38 | `/home/new_car_follow/detail` | `NativeNewCarFollowDetailPage` | `NativeNewCarFollowDetailPane` | 共享 Compose（无独立原生页） | 静态元组 | B1 | F1 |
| 39 | `/home/new_car_follow` | `NativeNewCarFollowPage` | `NativeNewCarFollowPane` | 共享 Compose（无独立原生页） | 静态元组 | B1 | F1 |
| 40 | `/mine/poster` | `NativePosterPage` | `NativePosterPane` | 共享 Compose（无独立原生页） | 静态元组 | B1 | F1 |
| 41 | `/mine/profile` | `NativeProfilePage` | `NativeProfilePane` | 共享 Compose（无独立原生页） | 静态元组 | B1 | F1 |
| 42 | `/mine/purchase_calculator` | `NativePurchaseCalculatorPage` | `NativePurchaseCalculatorPane` | 共享 Compose（无独立原生页） | 静态元组 | B1 | F1 |
| 43 | `/mine/http_test` / `/mine/qa` | `NativeQaPage` | `NativeQaPane` | 共享 Compose（无独立原生页） | 静态元组 | B1 | F1 |
| 44 | `/mine/reminder` | `NativeRemindersPage` | `NativeRemindersPane` | 共享 Compose（无独立原生页） | 静态元组 | B1 | F1 |
| 45 | `/scan` | `NativeScanPage` | `NativeScanPane` | 共享 Compose（无独立原生页） | 静态元组 | B1 | F1 |
| 46 | `/home/search` | `NativeSearchPage` | `NativeSearchPane` | 共享 Compose（无独立原生页） | 静态元组 | B1 | F1 |
| 47 | `/video/short/help` | `NativeShortVideoHelpPage` | `NativeShortVideoHelpPane` | 共享 Compose（无独立原生页） | 静态元组 | B1 | F1 |
| 48 | `/video/short` | `NativeShortVideoPage` | `NativeShortVideoPane` | 共享 Compose（无独立原生页） | 静态元组 | B1 | F1 |
| 49 | `/video/short/play` | `NativeShortVideoPlayPage` | `NativeShortVideoPlayPane` | 共享 Compose（无独立原生页） | 静态元组 | B1 | F1 |
| 50 | `/video/short/publish` | `NativeShortVideoPublishPage` | `NativeShortVideoPublishPane` | 共享 Compose（无独立原生页） | 静态元组 | B1 | F1 |
| 51 | `/mine/sms_templates` | `NativeSmsTemplatePage` | `NativeSmsTemplatePane` | 共享 Compose（无独立原生页） | 静态元组 | B1 | F1 |
| 52 | `/mine/shop_qr` | `NativeStoreQrPage` | `NativeStoreQrPane` | 共享 Compose（无独立原生页） | 静态元组 | B1 | F1 |
| 53 | `/home/strategy` | `NativeStrategyPage` | `NativeStrategyPane` | 共享 Compose（无独立原生页） | 静态元组 | B1 | F1 |
| 54 | `/home/todo/after-sales-appointments` / `/home/todo/follow-up-customers` / `/home/todo/order-pending-review` / `/home/todo/partner-pending` | `NativeTodoListPage` | `NativeTodoListPane` | 共享 Compose（无独立原生页） | 静态元组 | B1 | F1 |
| 55 | `/home/transliter_feed` | `NativeTransliterFeedPage` | `NativeTransliterFeedPane` | 共享 Compose（无独立原生页） | 静态元组 | B1 | F1 |
| 56 | `/video/transliter/videos/detail` | `NativeTransliterVideoDetailPage` | `NativeTransliterVideoDetailPane` | 共享 Compose（无独立原生页） | 静态元组 | B1 | F1 |
| 57 | `/video/transliter/videos` | `NativeTransliterVideosPage` | `NativeTransliterVideosPane` | 共享 Compose（无独立原生页） | 静态元组 | B1 | F1 |
| 58 | `/video/transliter/works/detail` | `NativeTransliterWorkDetailPage` | `NativeTransliterWorkDetailPane` | 共享 Compose（无独立原生页） | 静态元组 | B1 | F1 |
| 59 | `/video/transliter/works` | `NativeTransliterWorksPage` | `NativeTransliterWorksPane` | 共享 Compose（无独立原生页） | 静态元组 | B1 | F1 |
| 60 | `/home/used_car/create` | `NativeUsedCarCreatePage` | `NativeUsedCarCreatePane` | 共享 Compose（无独立原生页） | 静态元组 | B1 | F1 |
| 61 | `/home/used_car/detail` | `NativeUsedCarDetailPage` | `NativeUsedCarDetailPane` | 共享 Compose（无独立原生页） | 静态元组 | B1 | F1 |
| 62 | `/home/used_car` | `NativeUsedCarPage` | `NativeUsedCarPane` | 共享 Compose（无独立原生页） | 静态元组 | B1 | F1 |
| 63 | `/video` | `NativeVideoHubPage` | `NativeVideoHubPane` | 共享 Compose（无独立原生页） | 静态元组 | B1 | F1 |
| 64 | ⭐ `/wallet` | `NativeWalletPage` | `NativeWalletPane` | 共享 Compose（无独立原生页） | 静态元组 | B1 | F1 |
| 65 | `/web` | `NativeWebPage` | `NativeWebPane` | 共享 Compose（无独立原生页） | 静态元组 | B1 | F1 |

计数：65 页（iOS）/ 65 页（OHOS）= 130 个原生页实现；命中 76 条 `path ==` 守卫（部分页服务多路由）；3 个试点页（#1 / #33 / #64）。

## 5. 公共 blocker / follow_up 代码

| 代码 | 含义 |
|---|---|
| **B1**（blocker） | iOS `NativeSecondaryPages.swift` / OHOS `NativeSecondaryPages.ets` 对应页为 15–43 行硬编码 `@State` 静态元组，无 store / API / Repository / 离线缓存；Android 走共享 Compose 但同路由数据契约未在三端归一；`check-*-dispatch.sh` 仅验证文本接线，不校验数据来源 |
| **F1**（follow_up） | 先补跨端 Repository / SSOT 与字段映射设计，再逐页接 `/api/v1/*` 已对齐端点（见 [backend-contract-matrix.md](backend-contract-matrix.md) A 区）；为每个试点页补契约测试（空态 / 错态 / 分页 / 金额口径）；三端真机 HTTP 往返 + 像素证据；给分发守卫补「数据契约断言」层（现仅文本守卫） |

> 状态词只用 `Migrated | Degraded | Placeholder | Deferred | Partial`。本表 65 页当前全部为 `Placeholder`（仅静态页、背后无数据通路）；按 [platform-gap-registry.md](../openspec/changes/parity-flutter-to-kmp/notes/platform-gap-registry.md) `Rule (2A)` 不判 `Migrated`。
> 剩余结构性 gap（Repository/离线/SSOT、InMemory DB、明文凭据等）统一登记在 [backend-alignment-gaps.md](backend-alignment-gaps.md)，不在本文件重复。

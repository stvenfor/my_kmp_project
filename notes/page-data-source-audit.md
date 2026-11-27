# 页面数据来源审计（KMP 共享页 + 三端原生壳）

> 日期：2026-11-26 · 范围：17 个 feature 模块 + `composeApp` 装配/Android 原生壳 + iOS/OHOS 原生页
> 目的：逐个页面确认数据来自「真实 `/api/v1/*` 接口」还是「本地 mock / 硬编码 / 静态占位」，为「禁止 mock」与后端补种子提供依据。
> 后端仓库 `my_go_study` 只读；本文件仅登记结论与处置。

## 0. 结论摘要

| 分类 | 数量 | 说明 |
|---|---|---|
| 已接真实 HTTP 接口 | 约 40 个页面/路由 | 首页看板、待办、二手车、售后、新车跟进（含流水）、账本、全部服务、搜索、热榜、学习报告、策略、签到、积分任务、社区、短视频、钱包、地址、积分、会员、交易、权限、商城四页、购车计算器、成交发票、个人资料、门店切换等 |
| 有真实接口但**前端没接线** | 0 处 | A1–A9、B15 已全部接线（2026-11-26） |
| **硬编码 / 本地假数据** | 约 10 处 | 音乐曲目、短信模板、提醒、选买问答、海报、收款码、邀请/合作/反馈表单、生活服务、车友会、跟读 Feed、地址省市区等 |
| 无后端接口，只能占位（第三方 SDK / 无接口） | 约 12 个 | 直播（RTC）、IM 聊天/通讯录（融云）、课堂、部分 Flutter SoT 演示页 |
| iOS / OHOS 原生页 | 65 + 65 | **全部硬编码静态数组，零 HTTP**（见 `notes/native-page-data-contract.md`） |

## 1. 判定口径

- **真实接口**：屏幕 → store/repository → `*Api` → `ApiClient` / `NetworkFacade.api()` 发 `/api/v1/*`。
- **硬编码**：屏幕/常量对象直接 `listOf(...)`、`@State` 数组、PNG 底图提供业务内容。
- **静态占位**：只有固定文案/图片，没有数据需求（如「直播能力未接入」）。
- **有接口未接线**：`*Api.kt` 已实现，屏幕没调用。

> 注意：`AddressMockStore`、`WalletMockStore`、`HomeMockData.kt` 只是**历史命名**，内部已改为真实 HTTP，不是假数据。

## 2. 已接真实接口的页面（可用）

| 模块 | 页面/路由 | 证据 |
|---|---|---|
| home | 首页根（看板/宫格/联系人/资讯/指标） | `HomeDashboardStore.kt:96-110` → `GET /home/dashboard` |
| home | 待办 / 入店申请 / 跟进客户 / 售后预约 / 审核单 | `HomeTodoStore.kt:77-105` |
| home | 二手车列表/详情/建单、客户、摘要 | `HomeSecondaryStore.kt:107-162` |
| home | 售后工单列表/建单/详情 | `HomeSecondaryStore.kt:165-210` |
| home | 新车跟进列表/建档/详情 | `HomeSecondaryStore.kt:214-252` |
| home | 全部服务 / 收藏 | `HomeContentStore.kt:79-98` |
| home | 搜索（历史/发现/榜单） | `HomeSearchScreen.kt:101-113` |
| home | 热榜详情 | `HomeContentStore.kt:133-150` |
| home | 学习报告 | `HomeContentStore.kt:154-178` |
| home | 投资策略 | `HomeContentStore.kt:182-198` |
| home | 数据台账列表/详情 | `HomeContentStore.kt:202-217` |
| home | 签到商城 / 签到 | `HomePointsStore.kt:103-140` |
| home | 数据分析列表/详情 | `AnalyticsGrpcClient.kt:16-22`（gRPC :9090，**仅 Android**；iOS/OHOS 抛 Unsupported） |
| mine | 收货地址列表/编辑/默认/删除 | `AddressMockStore.kt:89-189`（真实 HTTP） |
| mine | 钱包余额/银行卡/流水/充值/绑卡 | `WalletMockStore.kt:114-217`（真实 HTTP） |
| community | 动态流 / 帖子详情 / 点赞 / 评论 / 搜索 / 话题 | `HttpCommunityPostRepository.kt:74-90`、`CommunityRouteHost.kt:392-420` |
| community | 统一评论（列表/回复/置顶/审核） | `UnifiedCommentController.kt` + `HttpUnifiedCommentRepository.kt` |
| media | 短视频列表/详情/点赞/浏览/发布/审核 | `ShortVideoRepository.kt:145-205` |
| media | 音视频入口（只是选择器） | `VideoHubScreen.kt:59-104` |
| auth | 登录（密码/OTP）、注册、资料接口封装 | `AuthRepository.kt:19-37`、`UserProfileHttpApi.kt:29-78` |
| ai | AI SSE / 客服 Agent | `AiSseApi.kt:31`、`CsAgentApi.kt:58` |
| commerce/pay | 会员目录/支付渠道 | `PayApi.kt`、`/api/v1/membership/me` 已有真实数据 |
| wallet | 钱包契约模块 | `WalletApi.kt`（被 mine 复用实现） |
| Android 原生壳 | Home / Community Tab | 复用共享首页；社区走 `HttpCommunityPostRepository`（`NativeJetpackCommunityTab.kt:79-128`） |

## 3. 硬编码 / 假数据清单（按可修复性排序）

### A. 后端已有接口、前端未接线（P0，优先修）

> 2026-11-26 进展：A1–A5 已完成接线（见 §3A-已完成）。实现方式：`ApiClient` 由 App 层注入
> `MineRouteHost`（不再新增 `NetworkFacade.api()` 直调，layer gate 保持 clean）。

| # | 页面 | 现状 | 可用接口 |
|---|---|---|---|
| ~~A1~~ | 我的-商城列表 `/mine/mall` | ✅ 已接真实货架接口 | `GET /api/v1/mall/stores/{id}/products`（`MallApi.kt:30-42`） |
| ~~A2~~ | 我的-商品详情 `/mine/mall/detail` | ✅ 已接真实详情 + 立即购买 | `GET /api/v1/mall/stores/{id}/products/{pid}` |
| ~~A3~~ | 我的-订单列表 `/mine/mall/orders` | ✅ 已接真实订单 + 状态筛选 | `GET /api/v1/mall/orders` |
| ~~A4~~ | 我的-订单详情 `/mine/mall/orders/detail` | ✅ 已接真实详情 + 取消/支付 | `GET /api/v1/mall/orders/{id}` |
| ~~A5~~ | 我的-购车计算器 | ✅ 已接产品列表 + 真实报价 | `GET /api/v1/purchase-calculator/products`、`POST /quote`（`PurchaseCalculatorApi.kt`） |
| ~~A6~~ | 我的-成交发票 `/mine/deal_invoice` | ✅ 摘要/分类列表/详情/上传全部走真实接口 | `GET /api/v1/deal-invoices`、`/summary`、`/{id}`、`/customers`、`POST /deal-invoices`（`DealInvoiceController.kt`） |
| ~~A7~~ | 我的-个人资料 | ✅ 读取/保存改走 `/me/profile` | `GET/PATCH /api/v1/me/profile`（`profile/MineProfileApi.kt`） |
| ~~A8~~ | 我的-门店切换 | ✅ 门店列表 + 切换全部走真实接口 | `GET /api/v1/me/stores` + `POST /api/v1/profiles/me/store`（`profile/MineAccountController.kt`） |
| ~~A9~~ | 我的-签到快捷页 | ✅ 余额/签到/成长任务全部走真实接口 | `GET /points/status`、`POST /points/check-in`、`GET /points/tasks`、`POST /points/tasks/{code}/claim`（`points/MinePointsApi.kt`） |

#### 3A-已完成（2026-11-26）

| 文件 | 说明 |
|---|---|
| `feature/mine/.../mall/MineMallApi.kt` | 商城四页契约：货架/详情/订单/订单详情/下单/支付/取消；`api` 由外部注入 |
| `feature/mine/.../MineMallController.kt` | 页面级状态（`remember` 持有），失败只写 `errorMessage`，无本地兜底 |
| `feature/mine/.../MineMallScreens.kt` | 四个页面改为真实列表/详情/订单 + 加载/错误/重试/空态；PNG 底图已删除 |
| `feature/mine/.../MineCalculatorScreens.kt` | 全款/贷款、金融产品、期限、首付 → `POST /quote`，展示服务端明细 |
| `feature/mine/.../MineRouteHost.kt` | 新增 `api: ApiClient`，维护选中商品/订单并分发四页 |
| `composeApp` 四个入口 | `MineScreen` / `MineIsland` / `MineRouteHost` 统一从 `AppContainer` / `LegacyGraph` 注入 `ApiClient` |
| `feature/mine/.../dealinvoice/DealInvoiceController.kt` | 成交发票摘要/列表/详情/客户/提交状态机 |
| `feature/mine/.../DealInvoiceScreens.kt` | 去 PNG：摘要卡 + 分类 Tab + 列表 + 详情弹窗 + 客户上传（图片上传如实标注平台 gap） |
| `feature/mine/.../profile/MineProfileApi.kt` | `/me/profile` GET/PATCH、`/me/stores`、`/profiles/me/store` |
| `feature/mine/.../profile/MineAccountController.kt` | 资料 / 门店列表 / 切换 / 统计的页面级状态 |
| `feature/mine/.../MineHomeContent.kt` | 根页统计、门店名、角色、手机号改读 `/me/profile`；切店走真实 POST 并持久化选中 id |
| `feature/mine/.../SwitchStoreDialog.kt` | 删除 `MineStoreCatalog` 样例门店，列表由调用方注入 |
| `feature/mine/.../points/MinePointsApi.kt` | `/points/status`、`/check-in`、`/tasks`、`/tasks/{code}/claim` |
| `feature/mine/.../MineMiscScreens.kt` | 签到快捷页改真实余额/签到/成长任务，删除写死兑换项 |
| `feature/home/.../HomeDetailScreens.kt` | 新车跟进详情改读 `/new-car-follow-files/{id}/logs`，提交走真实 `POST …/logs` |
| `composeApp/.../AppCrossFeatureScreens.kt`、`NativeJetpackHome.kt` | Home 侧换店对话框同样从 `/me/stores` 拉取并 POST 切店 |

验证：`:feature:mine:compileDebugKotlinAndroid`、`:composeApp:compileDebugKotlinAndroid`、
`:composeApp:compileKotlinIosSimulatorArm64`、`:composeApp:compileKotlinOhosArm64` 全部通过；
`:feature:mine:testDebugUnitTest` 通过；`./scripts/check-layer-deps.sh` clean。
后端实测：货架 5 商品、订单 2 笔、报价 200000/36 期月供 4164.57 均来自真实接口。

### B. 硬编码假数据，且后端**没有**对应接口（P1，需要新增后端接口或改 honest 空态）

| # | 页面 | 硬编码内容 | 证据 |
|---|---|---|---|
| B1 | 音乐列表 `/music/list` | 6 首曲目 `FlutterTracks`（Ya Ali / Shape of You…） | `MusicScreens.kt:41-55,74` |
| B2 | 正在播放 `/music/now_playing` | 无曲目时回落到 `FlutterTracks.first()` | `MusicScreens.kt:112-120` |
| ~~B3~~ | 扫码 `/scan` | ✅ 删除「模拟扫码成功」假结果；无相机平台如实提示不可用 | `ScanScreen.kt` |
| ~~B4~~ | 我的-根页统计 | ✅ 改读 `/me/profile` 的 `stats`（门店名/角色/手机号同源） | `MineHomeContent.kt` + `profile/MineAccountController.kt` |
| B5 | 我的-短信模板 | 4 条写死模板 + 「已复制并打开短信（mock）」 | `MineServiceScreens.kt:47-52,78` |
| B6 | 我的-提醒事项 | 写死今日 3 条 / 明日 1 条 | `MineServiceScreens.kt:262-267` |
| B7 | 我的-选买问答 | 写死问答行 | `MineServiceScreens.kt:314-316` |
| B8 | 我的-商家海报 | 写死 4 个海报名 | `MineServiceScreens.kt:344` |
| B9 | 我的-店铺收款码 | 「演示门店」+ 纯色假二维码 | `MineServiceScreens.kt:107-120` |
| B10 | 我的-邀请/合作/反馈 | 表单只 toast，不提交 | `MineServiceScreens.kt:142-230` |
| B11 | 生活服务 `/home/life_service` | 快捷入口/每日直播/课程全写死，图片为 picsum | `LifeServiceScreen.kt:54-68` |
| B12 | 车友会/直播电商 `/home/club` | 写死帖子列表 | `ClubScreens.kt:60-71` |
| B13 | 跟读 Feed `/home/transliter_feed` | 分类/功能区/最近/达人/编辑/专辑/猜你喜欢 7 组全写死 | `TransliterFeedScreen.kt:93-135` |
| ~~B14~~ | 新车跟进-详情日志 | ✅ 改读/写 `/new-car-follow-files/{id}/logs` | `HomeDetailScreens.kt` + `NewCarFollowApi` |
| ~~B15~~ | 会员页 `/pay/membership` | ✅ 去 PNG：套餐/权益读 `/membership/me`，支付 CTA 仍走 PayGateway | `MembershipScreen.kt` + `MembershipApi.kt` |
| B16 | 地址编辑-省市区 | `DemoRegions` 3 条写死 | `MineProfileScreens.kt:352-354` |
| B17 | 我的-关于/合作等 | `DeferredDestinationStub`「后续开放」 | `DeferredDestinationStub.kt:30` |

### C. 第三方 SDK 未接入（保留占位，不算 mock）

- 直播 `/live`、`/live/room`：RTC 未接入（`LiveScreen.kt:21-24`）
- 聊天 `/chat`、通讯录 `/friend`：融云 IM 未接入（`ChatScreen.kt:27-30`、`FriendScreen.kt:20-24`）
- 课堂 `/classroom/*`：后端与课堂 SDK 未接入（`ClassroomScreen.kt:22-25`）
- 扫码：真机相机可用时走 `PlatformBarcodeScanner`（`ScanScreen.kt:140-155`）

### D. 静态 PNG 页（Flutter SoT 截图复刻）

`mall_list_body` / `mall_detail_body` / `mall_orders_body` / `pay_membership_body` / `deal_invoice_demo_body` / `mine_purchase_calculator_body` / `music_list_body`。这些页面「看起来有内容」但全部是位图，属于 A/B 类的实现方式，不单独计数。

## 4. 三端原生壳

| 平台 | 页面数 | 数据来源 | 证据 |
|---|---|---|---|
| Android | 复用共享 Compose（无独立业务页） | 与共享层一致（真实接口） | `NativeJetpackHome.kt:47-89` |
| iOS | 65 个 `NativeXxxPage` | **全部 `@State`/常量元组，无 URLSession** | `NativeSecondaryPages.swift`、`NativeFeatureCatalog.swift:163-177` |
| OHOS | 65 个 `NativeXxxPane` | **全部本地数组/方法，无 http 请求** | `NativeSecondaryPages.ets:229-307` |

判定：iOS/OHOS 原生页目前是「能打开、但数据全假」的静态重写页；`check-ios-page-dispatch.sh` / `check-harmony-pane-dispatch.sh` 只验证路由文本接线，不校验数据来源。

## 5. 后端接口实测（登录 13400000000 / OTP 123456，修复前后）

| 接口 | 修复前 | 补种子后 |
|---|---|---|
| `GET /api/v1/home/dashboard` | features 9 / services 8 / contacts 5 / news 3 | 不变 |
| `GET /api/v1/home/catalog` | sections 4 / favorites 3 | 不变 |
| `GET /api/v1/home/search/meta` | history 3 / discovery 5 / rank_tabs 4 | 不变 |
| `GET /api/v1/home/hot-rank` | items 3 | 不变 |
| `GET /api/v1/home/learning-report` | highlights 3 / records 5 | 不变 |
| `GET /api/v1/home/strategy` | assets 9 | 不变 |
| `GET /api/v1/points/status` | balance 0 | **1280** |
| `GET /api/v1/points/tasks` | 3 | 不变 |
| `GET /api/v1/wallet` | cards **0** | **2**，balance 843.00 |
| `GET /api/v1/wallet/ledger` | items **0** | **3** |
| `GET /api/v1/user/addresses` | items **0** | **3** |
| `GET /api/v1/after-sales/records` | list **0** | **3** |
| `GET /api/v1/mall/orders` | list **0** | **2** |
| `GET /api/v1/mall/stores/1/products` | list 5 | 不变 |
| `GET /api/v1/community/posts` | list 5 | 不变 |
| `GET /api/v1/used-car-orders` | list 5 | 不变 |
| `GET /api/v1/new-car-follow-files` | list 3 | 不变 |
| `GET /api/v1/deal-invoices` | list 5 | 不变 |
| `GET /api/v1/purchase-calculator/products` | items 3 | 不变 |
| `GET /api/v1/membership/me` | catalog 6 | 不变 |
| `GET /api/v1/me/profile` | stats 全 0 | **365/28/2059/9366** |
| `GET /api/v1/im/friends` | items 0 | 仍为 0（好友关系需真实用户配对，IM 未接入） |

种子脚本：`notes/demo-seed-extra.sql`（幂等，可重复执行），写入表：
`wys_user_store_stats`、`wys_cash_wallet`、`wys_bank_card`、`wys_cash_ledger`、`wys_user_address`、
`wys_after_sales_record`、`wys_mall_order`、`wys_mall_order_item`、`wys_points_wallet`。

## 6. 「禁止 mock」规则

1. 新页面/新接口一律走 `/api/v1/*`；禁止在 `commonMain` 出现业务假数据常量（列表、金额、订单、地址、统计）。
2. 后端没有的接口 → 先补后端接口 + 种子，再接线；不得用本地假数据「撑满」界面。
3. 第三方 SDK 未接入的页面，只允许「honest 空态/未接入」文案，不允许 mock 交互（假成功 toast、假扫码结果、假支付）。
4. 仅允许的本地数据：UI 文案、Tab 标签、枚举选项、表单校验提示、纯本地偏好设置（主题/语言）。
5. 测试夹具（`commonTest`）不受此限。

## 7. 建议执行批次

| 批次 | 内容 | 依赖 |
|---|---|---|
| 1 | ✅ A1–A4 商城四页接 `MallApi`（Mine 内实现同源最小 API，遵守 feature 不互相依赖） | 后端种子已就绪 |
| 2 | ✅ A5 购车计算器、A6 成交发票接已有 Api | 无 |
| 3 | ✅ A7 个人资料、A8 门店切换接 `/me/profile`、`/me/stores` | 无 |
| 4 | ✅ B4 我的统计、A9 签到接真实数据、B14 新车跟进流水 | 种子已就绪 |
| 5 | B1/B2 音乐：新增后端曲目接口后接线；否则 honest 空态 | 需后端新增 |
| 6 | B5–B10 短信/提醒/问答/海报/收款码/邀请合作反馈：新增后端接口或下线入口 | 需后端新增 |
| 7 | B11–B13 生活服务/车友会/跟读 Feed：新增内容接口 | 需后端新增 |
| 7b | B15 会员 PNG 页接 `/membership/me`；B16 地址省市区接后端行政区接口 | 需后端新增（B16） |
| 8 | iOS/OHOS 65+65 原生页：改为承载共享 Compose 页或逐页接 Repository | 架构决策 |

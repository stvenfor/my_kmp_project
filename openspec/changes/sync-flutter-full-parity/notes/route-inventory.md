# 全量路由清单（Flutter → KMP）

> SoT：`my_ai_project` @ `d7876a1` · 权威表：`commons/wys_router/lib/src/route/route_path.dart`
> 状态：`ready`（三端 UI 可验收）· `partial` · `stub` · `todo` · `n/a`（out-of-scope）
> 平台列：A=Android · i=iOS · h=Harmony。KMP 落点以 ADR 0002 为准：非「我的」页走原生壳 + NativeFeatureCatalog，「我的」二级走共享 Compose 岛。

## 汇总

| 分组 | 路由数 | A | i | h | 说明 |
|---|---|---|---|---|---|
| 壳/鉴权/桥 | 7 | 6 ready / 1 partial | 4 partial | 4 partial | auth 远程链路/OTP 配额、OHOS 一键登录、WebView/扫码 iOS·OHOS |
| Home 根+二级 | 28 | ready | ready | ready | 像素精修可迭代；new-car-follow 有 drift |
| AI | 1 | stub | stub | stub | 无 SSE（真链另列） |
| Chat | 2 | partial | partial | partial | mock IM 对等；真融云 drift 见 L.2 |
| Community | 4 | partial | partial | partial | mock 引擎对等；发布缺 image_picker |
| Friend | 1 | partial | partial | partial | 目录↔ImEngine mock |
| Live | 2 | partial | partial | partial | mock 列表/房间 |
| Pay / Wallet | 3 | stub | stub | stub | UI 显示不可用，无假 Success |
| Mine | 7 | ready | ready | ready | 共享 Compose 岛（含 http_test） |
| Settings | 8 | 1 ready / 5 n/a / 2 待定 | 同左 | 同左 | 发票两页待产品定 scope |
| Mall | 4 | ready | ready | ready | NativeFeature mall/wallet |
| Video | 9 | partial | partial | partial | 短视频/配音；播放器真适配另列 |
| bfui | 17 | n/a | n/a | n/a | 模板演示，out-of-scope |
| Music | 2 | partial | partial | stub | OHOS 播放器 stub |
| Classroom | 8 | partial | partial | partial | mock 数据对等 |
| **合计** | **103** | — | — | — | in-scope 78 条三端 UI ready（gap registry） |

## 明细

### 壳 / 鉴权 / 桥（A）

| RoutePath | Flutter 页面 | 模块 | KMP 落点 | A | i | h |
|---|---|---|---|---|---|---|
| `/` | SplashPage | shell | AppShell/Splash | ready | ready | ready |
| `/main` | MainPage | shell | AppShell 四 Tab | ready | ready | ready |
| `/web` | AppWebViewPage | commons/ui | PlatformWebView + OfflineWebFixtureUrl | ready | partial | partial |
| `/login` | LoginPage | auth | feature/auth | ready | partial | partial |
| `/login/password` | 账密页 | auth | feature/auth | ready | partial | partial |
| `/login/otp` | 验证码页 | auth | feature/auth | ready | partial | partial |
| `/register` | 注册页 | auth | feature/auth | ready | partial | partial |

### Home（B，28 条）

| RoutePath | Flutter 页面 | KMP 落点 | A | i | h |
|---|---|---|---|---|---|
| `/home` | HomePage | feature/home | ready | ready | ready |
| `/home/learning_report` | HomeLearningReportPage | feature/home | ready | ready | ready |
| `/home/check_in_mall` | CheckInMallPage | feature/home | ready | ready | ready |
| `/home/all_services` | AllServicesPage | feature/home | ready | ready | ready |
| `/home/search` | SearchPage | feature/home | ready | ready | ready |
| `/home/dubbing_feed` | DubbingHomePage | feature/home | ready | ready | ready |
| `/home/strategy` | StrategyPage | feature/home | ready | ready | ready |
| `/home/hot_rank_detail` | HotRankDetailPage | feature/home | ready | ready | ready |
| `/home/used_car` | 二手车列表 | feature/home | ready | ready | ready |
| `/home/used_car/detail` | 二手车详情 | feature/home | ready | ready | ready |
| `/home/used_car/create` | 发布二手车 | feature/home | ready | ready | ready |
| `/home/ledger` | 台账列表 | feature/home | ready | ready | ready |
| `/home/ledger/detail` | 台账详情 | feature/home | ready | ready | ready |
| `/home/data_analytics` | 数据分析列表 | feature/home | ready | ready | ready |
| `/home/data_analytics/detail` | 分析详情（图表） | feature/home | ready | ready | ready |
| `/home/life_service` | 生活服务容器 | feature/home | ready | ready | ready |
| `/home/live_commerce` | 直播电商容器 | feature/home | ready | ready | ready |
| `/home/club` | Club 容器 | feature/home | ready | ready | ready |
| `/home/todo/partner-pending` | 待跟进合伙人 | feature/home | ready | ready | ready |
| `/home/todo/follow-up-customers` | 待跟进客户 | feature/home | ready | ready | ready |
| `/home/todo/after-sales-appointments` | 售后预约 | feature/home | ready | ready | ready |
| `/home/todo/order-pending-review` | 待审订单 | feature/home | ready | ready | ready |
| `/home/after_sales` | 售后列表 | feature/home | ready | ready | ready |
| `/home/after_sales/create` | 建售后 | feature/home | ready | ready | ready |
| `/home/after_sales/detail` | 售后详情 | feature/home | ready | ready | ready |
| `/home/new_car_follow` | 新车跟进列表 | feature/home | ready | ready | ready |
| `/home/new_car_follow/create` | 建跟进 | feature/home | ready | ready | ready |
| `/home/new_car_follow/detail` | 跟进详情/流水 | feature/home | ready | ready | ready |

### AI / Chat / Community / Friend / Live

| RoutePath | Flutter 页面 | 模块 | KMP 落点 | A | i | h |
|---|---|---|---|---|---|---|
| `/ai/stream` | AiStreamPage | ai | feature/ai（mock chunk） | stub | stub | stub |
| `/chat` | ChatPage | chat | feature/chat | partial | partial | partial |
| `/chat/detail` | ChatDetailPage | chat | feature/chat | partial | partial | partial |
| `/community` | CommunityPage | community | feature/community | partial | partial | partial |
| `/community/publish` | PublishPage | community | feature/community | partial | partial | partial |
| `/community/convention` | 社区公约 | community | feature/community | partial | partial | partial |
| `/community/search` | CommunitySearchPage | community | feature/community | partial | partial | partial |
| `/friend` | FriendPage（853 行通讯录） | friend | feature/friend | partial | partial | partial |
| `/live` | LivePage | live | feature/live | partial | partial | partial |
| `/live/room` | LiveRoomPage | live | feature/live | partial | partial | partial |

### Pay / Wallet / Mine / Settings

| RoutePath | Flutter 页面 | 模块 | KMP 落点 | A | i | h |
|---|---|---|---|---|---|---|
| `/pay` | 支付页 | pay | feature/commerce | stub | stub | stub |
| `/pay/membership` | 会员页 | pay | feature/commerce | stub | stub | stub |
| `/wallet` | 钱包 | wallet | feature/commerce | partial | partial | partial |
| `/mine` | MinePage | settings | **MineIsland（共享 Compose）** | ready | ready | ready |
| `/mine/http_test` | 网络测试 | settings | **MineIsland** | ready | ready | ready |
| `/mine/personalized_settings` | 个性化 | settings | **MineIsland** | ready | ready | ready |
| `/mine/profile` | 个人资料 | settings | **MineIsland** | ready | ready | ready |
| `/mine/addresses` | 地址列表 | settings | **MineIsland** | ready | ready | ready |
| `/mine/addresses/edit` | 地址编辑 | settings | **MineIsland** | ready | ready | ready |
| `/mine/purchase_calculator` | 购车计算器 | settings | **MineIsland** | ready | ready | ready |
| `/settings` | 设置页 | settings | NativeFeature + MineIsland | ready | ready | ready |
| `/settings/dialog_demo` | 调试 | settings | — | n/a | n/a | n/a |
| `/settings/linking_debug` | 调试 | settings | — | n/a | n/a | n/a |
| `/settings/realtime_debug` | 调试 | settings | — | n/a | n/a | n/a |
| `/settings/im_debug` | 调试 | settings | — | n/a | n/a | n/a |
| `/settings/bluetooth_demo` | 调试 | settings | — | n/a | n/a | n/a |
| `/settings/deal_invoice_demo` | 发票演示 | settings | 待产品定 scope | todo | todo | todo |
| `/settings/deal_invoice/upload` | 发票上传 | settings | 待产品定 scope | todo | todo | todo |

### Mall / Video / Music / Classroom / bfui

| RoutePath | Flutter 页面 | 模块 | KMP 落点 | A | i | h |
|---|---|---|---|---|---|---|
| `/mall` | 商城瀑布流 | mall | feature/commerce | ready | ready | ready |
| `/mall/detail` | 商品详情 | mall | feature/commerce | ready | ready | ready |
| `/mall/orders` | 订单列表 | mall | feature/commerce | ready | ready | ready |
| `/mall/orders/detail` | 订单详情 | mall | feature/commerce | ready | ready | ready |
| `/video` | 视频 Hub | video | feature/media | partial | partial | partial |
| `/video/short` | 短视频 feed | video | feature/media | partial | partial | partial |
| `/video/short/play` | 播放页 | video | feature/media | partial | partial | partial |
| `/video/short/publish` | 发布 | video | feature/media | partial | partial | partial |
| `/video/short/help` | 帮助 | video | feature/media | partial | partial | partial |
| `/video/dubbing/videos` | 配音视频列表 | video | feature/media | partial | partial | partial |
| `/video/dubbing/videos/detail` | 配音视频详情 | video | feature/media | partial | partial | partial |
| `/video/dubbing/works` | 作品列表 | video | feature/media | partial | partial | partial |
| `/video/dubbing/works/detail` | 作品详情 | video | feature/media | partial | partial | partial |
| `/music/list` | 音乐列表 | music | feature/media | partial | partial | stub |
| `/music/now_playing` | 播放页 | music | feature/media | partial | partial | stub |
| `/classroom/my_class` | 我的班级 | classroom | feature/classroom | partial | partial | partial |
| `/classroom/homework_stats` | 作业统计 | classroom | feature/classroom | partial | partial | partial |
| `/classroom/homework/detail_teacher` | 作业详情（师） | classroom | feature/classroom | partial | partial | partial |
| `/classroom/homework/detail_student` | 作业详情（生） | classroom | feature/classroom | partial | partial | partial |
| `/classroom/homework/dubbing` | 配音作业 | classroom | feature/classroom | partial | partial | partial |
| `/classroom/homework/review` | 作业批改 | classroom | feature/classroom | partial | partial | partial |
| `/classroom/gift/claim` | 领礼物卡 | classroom | feature/classroom | partial | partial | partial |
| `/classroom/video/detail` | 课堂视频 | classroom | feature/classroom | partial | partial | partial |
| `/bfui/*`（17 条） | BFUI 模板 | bfui | — | n/a | n/a | n/a |

## 维护规则

- 新增/删除路由必须同步本表 + `tasks-ultra-fine.md` + `AppRoutePath`（KMP）三处，字符串与 Flutter 一致（前序 K.3 已迁共享常量表）。
- 每周与 `tasks-ultra-fine.md` 对一次状态（前序 K.8 待办延续为本清单 L.K.1）。
- Flutter 侧路由变更以 `git log my_ai_project` + `route_path.dart` diff 为触发。

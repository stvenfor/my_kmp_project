# 后端契约对照表：KMP `/api/v1/*` ↔ Go `my_go_study` 真实路由

> 本表由 wave1 B 槽以 **Go 后端仓库为唯一真相源**逐条核对产出；后端仓库只读，未改动一行。
> `audit-flutter-drift.sh` 比较的是 **KMP ↔ Flutter(Dart)**，与本表（KMP ↔ 后端）是两条不同的轴；
> 二者的差异处置见 `notes/route-inventory.md`。

## 约定

- **真相源**：`/Users/qpy/Desktop/github/my_go_study/app/internal/`
  - `delivery/http/router/router.go`（`v1 := r.Group("/api/v1")` + 各 `register*Routes` + `DomainRouters`）
  - `delivery/http/router/{user,profile,realtime,jpush,im,sse,cs_agent}_routes.go`
  - 16 个域 `app/internal/<域>/router.go`（access / after_sales / community / deal_invoice / home / mall / membership / new_car_follow / payment / points / purchase_calculator / short_video / transaction / used_car / wallet）
- **路径归一化**：gin `:param` 与 Kotlin/Dart `$var` 统一记作 `:v`；`Group("/x")` 下 `GET("")` 记作 `/x` 基路径。表中 KMP 路径取 `audit-flutter-drift.sh` 的 `resolve.py` 归一化结果。
- **结论枚举**：`aligned`（后端有此真实路由）/ `missing(待接线)`（后端有、KMP 无）/ `mock(自造)`（后端无，KMP 自造）/ `extended(产品确认)`（KMP 超出后端、经确认）。**不许编造后端不存在的接口**：B 区每一行都带后端 `file:line`。
- **KMP file:line 简写**：省略公共前缀 `feature/<模块>/src/commonMain/kotlin/com.example.my_kmp_project/feature/<模块>/`，写作 `<模块>/<文件>.kt:行`；`core/network/…` 与 `component/…` 用真实短路径。
- **后端 file:line 简写**：`<域>/router.go:行` = `app/internal/<域>/router.go:行`；`<名>_routes.go:行` = `app/internal/delivery/http/router/<名>_routes.go:行`。

## 计数

- KMP 现有 `/api/v1/*` 归一化路径：**94** 条（含本轮新增 `/api/v1/payments/prepay`）。
- 后端唯一路径：**117** 条（method 去重后）。
- A 区 `aligned`：91 条；`mock(自造)`（组合前缀常量）：3 条。
- B 区后端有、KMP 无：**22** 条 genuine `missing(待接线)`；另有 4 条 im 群动作实为 KMP 已接线、仅解析器盲区（B.1 单列）。合计后端独有路径 26 条。

## A. KMP 现有 `/api/v1/*` 路径逐条对照

| # | KMP 路径 | KMP file:line | 后端 method+path | 后端 file:line | 结论 |
|---|---|---|---|---|---|
| 1 | `/api/v1/after-sales` | `home/aftersales/AfterSalesApi.kt:26` | —（后端无裸路由） | — | mock(自造)（组合前缀常量，非调用端点，见 C 区） |
| 2 | `/api/v1/after-sales/pending-appointments` | `home/aftersales/AfterSalesApi.kt:104` | `GET /api/v1/after-sales/pending-appointments` | `after_sales/router.go:39` | aligned |
| 3 | `/api/v1/after-sales/records` | `home/aftersales/AfterSalesApi.kt:44,95` | `GET/POST /api/v1/after-sales/records` | `after_sales/router.go:40; after_sales/router.go:41` | aligned |
| 4 | `/api/v1/after-sales/records/:v` | `home/aftersales/AfterSalesApi.kt:58` | `GET /api/v1/after-sales/records/:v` | `after_sales/router.go:42` | aligned |
| 5 | `/api/v1/community` | `community/http/HttpCommunityPostRepository.kt:32` | —（后端无裸路由） | — | mock(自造)（组合前缀常量，非调用端点，见 C 区） |
| 6 | `/api/v1/community/posts` | `community/http/HttpCommunityPostRepository.kt:35` | `GET/POST /api/v1/community/posts` | `community/router.go:44; community/router.go:45` | aligned |
| 7 | `/api/v1/community/posts/:v` | `community/http/HttpCommunityPostRepository.kt:53` | `DELETE /api/v1/community/posts/:v` | `community/router.go:46` | aligned |
| 8 | `/api/v1/community/posts/:v/comments` | `community/http/HttpCommunityPostRepository.kt:47` | `GET/POST /api/v1/community/posts/:v/comments` | `community/router.go:49; community/router.go:50` | aligned |
| 9 | `/api/v1/community/posts/:v/like` | `community/http/HttpCommunityPostRepository.kt:50` | `DELETE/POST /api/v1/community/posts/:v/like` | `community/router.go:47; community/router.go:48` | aligned |
| 10 | `/api/v1/community/search` | `community/http/HttpCommunityPostRepository.kt:44` | `GET /api/v1/community/search` | `community/router.go:42` | aligned |
| 11 | `/api/v1/community/topics` | `community/http/HttpCommunityPostRepository.kt:38` | `GET /api/v1/community/topics` | `community/router.go:40` | aligned |
| 12 | `/api/v1/community/topics/search` | `community/http/HttpCommunityPostRepository.kt:41` | `GET /api/v1/community/topics/search` | `community/router.go:41` | aligned |
| 13 | `/api/v1/community/users/:v/follow` | `community/http/HttpCommunityPostRepository.kt:56` | `DELETE/POST /api/v1/community/users/:v/follow` | `community/router.go:52; community/router.go:53` | aligned |
| 14 | `/api/v1/deal-invoices` | `mine/dealinvoice/DealInvoiceApi.kt:21` | `GET/POST /api/v1/deal-invoices` | `deal_invoice/router.go:41; deal_invoice/router.go:42` | aligned |
| 15 | `/api/v1/deal-invoices/:v` | `mine/dealinvoice/DealInvoiceApi.kt:30` | `GET /api/v1/deal-invoices/:v` | `deal_invoice/router.go:43` | aligned |
| 16 | `/api/v1/deal-invoices/:v/resubmit` | `mine/dealinvoice/DealInvoiceApi.kt:33` | `PATCH /api/v1/deal-invoices/:v/resubmit` | `deal_invoice/router.go:44` | aligned |
| 17 | `/api/v1/deal-invoices/customers` | `mine/dealinvoice/DealInvoiceApi.kt:27` | `GET /api/v1/deal-invoices/customers` | `deal_invoice/router.go:40` | aligned |
| 18 | `/api/v1/deal-invoices/summary` | `mine/dealinvoice/DealInvoiceApi.kt:24` | `GET /api/v1/deal-invoices/summary` | `deal_invoice/router.go:39` | aligned |
| 19 | `/api/v1/home/after-sales-appointments` | `home/api/HomeTodoApi.kt:31` | `GET /api/v1/home/after-sales-appointments` | `home/router.go:41` | aligned |
| 20 | `/api/v1/home/follow-up-customers` | `home/api/HomeTodoApi.kt:28` | `GET /api/v1/home/follow-up-customers` | `home/router.go:40` | aligned |
| 21 | `/api/v1/home/join-applications` | `home/api/HomeTodoApi.kt:25` | `GET /api/v1/home/join-applications` | `home/router.go:37` | aligned |
| 22 | `/api/v1/home/join-applications/:v/approve` | `home/api/HomeTodoApi.kt:54` | `POST /api/v1/home/join-applications/:v/approve` | `home/router.go:38` | aligned |
| 23 | `/api/v1/home/join-applications/:v/reject` | `home/api/HomeTodoApi.kt:58` | `POST /api/v1/home/join-applications/:v/reject` | `home/router.go:39` | aligned |
| 24 | `/api/v1/home/store-review-orders` | `home/api/HomeTodoApi.kt:34` | `GET /api/v1/home/store-review-orders` | `home/router.go:42` | aligned |
| 25 | `/api/v1/home/todo-cards` | `home/api/HomeTodoApi.kt:22` | `GET /api/v1/home/todo-cards` | `home/router.go:36` | aligned |
| 26 | `/api/v1/im/friends` | `friend/ImFriendApi.kt:25` | `GET /api/v1/im/friends` | `delivery/http/router/im_routes.go:19` | aligned |
| 27 | `/api/v1/im/friends/requests` | `friend/ImFriendApi.kt:28` | `GET/POST /api/v1/im/friends/requests` | `delivery/http/router/im_routes.go:20; delivery/http/router/im_routes.go:21` | aligned |
| 28 | `/api/v1/im/friends/requests/:v/respond` | `friend/ImFriendApi.kt:37` | `POST /api/v1/im/friends/requests/:v/respond` | `delivery/http/router/im_routes.go:22` | aligned |
| 29 | `/api/v1/im/groups/free` | `chat/RongImConfig.kt:33` | `POST /api/v1/im/groups/free` | `delivery/http/router/im_routes.go:24` | aligned |
| 30 | `/api/v1/im/groups/store/sync` | `chat/RongImConfig.kt:36` | `POST /api/v1/im/groups/store/sync` | `delivery/http/router/im_routes.go:29` | aligned |
| 31 | `/api/v1/im/messages/backup` | `chat/RongImConfig.kt:30` | `POST /api/v1/im/messages/backup` | `delivery/http/router/im_routes.go:30` | aligned |
| 32 | `/api/v1/im/private/admission` | `friend/ImFriendApi.kt:34` | `GET /api/v1/im/private/admission` | `delivery/http/router/im_routes.go:23` | aligned |
| 33 | `/api/v1/im/session` | `chat/RongImConfig.kt:24` | `POST /api/v1/im/session` | `delivery/http/router/im_routes.go:16` | aligned |
| 34 | `/api/v1/im/users/profile` | `chat/RongImConfig.kt:27` | `GET /api/v1/im/users/profile` | `delivery/http/router/im_routes.go:18` | aligned |
| 35 | `/api/v1/im/users/search` | `friend/ImFriendApi.kt:31` | `GET /api/v1/im/users/search` | `delivery/http/router/im_routes.go:17` | aligned |
| 36 | `/api/v1/mall` | `commerce/MallApi.kt:232` | —（后端无裸路由） | — | mock(自造)（组合前缀常量，非调用端点，见 C 区） |
| 37 | `/api/v1/mall/cart` | `commerce/MallApi.kt:239` | `GET/POST /api/v1/mall/cart` | `mall/router.go:49; mall/router.go:50` | aligned |
| 38 | `/api/v1/mall/orders` | `commerce/MallApi.kt:241` | `GET/POST /api/v1/mall/orders` | `mall/router.go:52; mall/router.go:53` | aligned |
| 39 | `/api/v1/mall/orders/:v` | `commerce/MallApi.kt:243` | `GET /api/v1/mall/orders/:v` | `mall/router.go:54` | aligned |
| 40 | `/api/v1/mall/orders/:v/cancel` | `commerce/MallApi.kt:247` | `POST /api/v1/mall/orders/:v/cancel` | `mall/router.go:56` | aligned |
| 41 | `/api/v1/mall/orders/:v/pay` | `commerce/MallApi.kt:245` | `POST /api/v1/mall/orders/:v/pay` | `mall/router.go:55` | aligned |
| 42 | `/api/v1/mall/stores/:v/products` | `commerce/MallApi.kt:234` | `GET /api/v1/mall/stores/:v/products` | `mall/router.go:46` | aligned |
| 43 | `/api/v1/mall/stores/:v/products/:v` | `commerce/MallApi.kt:237` | `GET /api/v1/mall/stores/:v/products/:v` | `mall/router.go:47` | aligned |
| 44 | `/api/v1/me/stores` | `auth/UserProfileContracts.kt:22` | `GET /api/v1/me/stores` | `delivery/http/router/profile_routes.go:26` | aligned |
| 45 | `/api/v1/membership/apple/verify` | `pay/PayApi.kt:249` | `POST /api/v1/membership/apple/verify` | `membership/router.go:45` | aligned |
| 46 | `/api/v1/membership/buyout` | `pay/PayApi.kt:246` | `POST /api/v1/membership/buyout` | `membership/router.go:42` | aligned |
| 47 | `/api/v1/membership/buyout/confirm` | `pay/PayApi.kt:247` | `POST /api/v1/membership/buyout/confirm` | `membership/router.go:43` | aligned |
| 48 | `/api/v1/membership/huawei/verify` | `pay/PayApi.kt:248` | `POST /api/v1/membership/huawei/verify` | `membership/router.go:44` | aligned |
| 49 | `/api/v1/membership/me` | `pay/PayApi.kt:245` | `GET /api/v1/membership/me` | `membership/router.go:41` | aligned |
| 50 | `/api/v1/new-car-follow-files` | `home/newcar/NewCarFollowApi.kt:32` | `GET/POST /api/v1/new-car-follow-files` | `new_car_follow/router.go:41; new_car_follow/router.go:42` | aligned |
| 51 | `/api/v1/new-car-follow-files/:v` | `home/newcar/NewCarFollowApi.kt:70` | `GET/PATCH /api/v1/new-car-follow-files/:v` | `new_car_follow/router.go:43; new_car_follow/router.go:44` | aligned |
| 52 | `/api/v1/new-car-follow-files/:v/logs` | `home/newcar/NewCarFollowApi.kt:134,159` | `GET/POST /api/v1/new-car-follow-files/:v/logs` | `new_car_follow/router.go:45; new_car_follow/router.go:46` | aligned |
| 53 | `/api/v1/new-car-follow-files/summary` | `home/newcar/NewCarFollowApi.kt:43` | `GET /api/v1/new-car-follow-files/summary` | `new_car_follow/router.go:39` | aligned |
| 54 | `/api/v1/payments/prepay` | `pay/PaymentPrepayApi.kt:52` | `POST /api/v1/payments/prepay` | `payment/router.go:49` | aligned（本轮补契约） |
| 55 | `/api/v1/points/check-in` | `home/api/PointsApi.kt:27` | `POST /api/v1/points/check-in` | `points/router.go:33` | aligned |
| 56 | `/api/v1/points/status` | `home/api/PointsApi.kt:24` | `GET /api/v1/points/status` | `points/router.go:32` | aligned |
| 57 | `/api/v1/points/tasks` | `home/api/PointsApi.kt:30` | `GET /api/v1/points/tasks` | `points/router.go:34` | aligned |
| 58 | `/api/v1/points/tasks/:v/claim` | `home/api/PointsApi.kt:60` | `POST /api/v1/points/tasks/:v/claim` | `points/router.go:35` | aligned |
| 59 | `/api/v1/profiles/me` | `auth/UserProfileContracts.kt:16` | `GET/PATCH /api/v1/profiles/me` | `delivery/http/router/profile_routes.go:16; delivery/http/router/profile_routes.go:17` | aligned |
| 60 | `/api/v1/profiles/me/store` | `auth/UserProfileContracts.kt:19` | `POST /api/v1/profiles/me/store` | `delivery/http/router/profile_routes.go:18` | aligned |
| 61 | `/api/v1/purchase-calculator/products` | `mine/calculator/PurchaseCalculatorApi.kt:20` | `GET /api/v1/purchase-calculator/products` | `purchase_calculator/router.go:25` | aligned |
| 62 | `/api/v1/purchase-calculator/quote` | `mine/calculator/PurchaseCalculatorApi.kt:23` | `POST /api/v1/purchase-calculator/quote` | `purchase_calculator/router.go:26` | aligned |
| 63 | `/api/v1/push/devices` | `component/push/PushRegistrationApi.kt:76` | `POST /api/v1/push/devices` | `delivery/http/router/jpush_routes.go:15` | aligned |
| 64 | `/api/v1/realtime/sync` | `core/network/realtime/RealtimeConfig.kt:22` | `POST /api/v1/realtime/sync` | `delivery/http/router/realtime_routes.go:15` | aligned |
| 65 | `/api/v1/realtime/ws-ticket` | `core/network/realtime/RealtimeConfig.kt:21` | `POST /api/v1/realtime/ws-ticket` | `delivery/http/router/realtime_routes.go:14` | aligned |
| 66 | `/api/v1/short-videos` | `media/ShortVideoRepository.kt:91` | `GET/POST /api/v1/short-videos` | `short_video/router.go:41; short_video/router.go:42` | aligned |
| 67 | `/api/v1/short-videos/:v` | `media/ShortVideoRepository.kt:96` | `DELETE/GET /api/v1/short-videos/:v` | `short_video/router.go:44; short_video/router.go:45` | aligned |
| 68 | `/api/v1/short-videos/:v/like` | `media/ShortVideoRepository.kt:94` | `DELETE/POST /api/v1/short-videos/:v/like` | `short_video/router.go:46; short_video/router.go:47` | aligned |
| 69 | `/api/v1/short-videos/:v/view` | `media/ShortVideoRepository.kt:95` | `POST /api/v1/short-videos/:v/view` | `short_video/router.go:48` | aligned |
| 70 | `/api/v1/short-videos/profile` | `media/ShortVideoRepository.kt:92` | `GET /api/v1/short-videos/profile` | `short_video/router.go:43` | aligned |
| 71 | `/api/v1/sse/completions` | `ai/AiSseApi.kt:32` | `POST /api/v1/sse/completions` | `delivery/http/router/sse_routes.go:14` | aligned |
| 72 | `/api/v1/transactions` | `home/api/HomeTransactionApi.kt:35` | `GET/POST /api/v1/transactions` | `transaction/router.go:46; transaction/router.go:47` | aligned |
| 73 | `/api/v1/transactions/:v` | `home/api/HomeTransactionApi.kt:71` | `DELETE/GET/PUT /api/v1/transactions/:v` | `transaction/router.go:49; transaction/router.go:50; transaction/router.go:51` | aligned |
| 74 | `/api/v1/used-car-orders` | `mine/usedcar/UsedCarOrderApi.kt:20` | `GET/POST /api/v1/used-car-orders` | `used_car/router.go:42; used_car/router.go:43` | aligned |
| 75 | `/api/v1/used-car-orders/:v` | `mine/usedcar/UsedCarOrderApi.kt:29` | `GET /api/v1/used-car-orders/:v` | `used_car/router.go:44` | aligned |
| 76 | `/api/v1/used-car-orders/customers` | `mine/usedcar/UsedCarOrderApi.kt:26` | `GET /api/v1/used-car-orders/customers` | `used_car/router.go:41` | aligned |
| 77 | `/api/v1/used-car-orders/summary` | `mine/usedcar/UsedCarOrderApi.kt:23` | `GET /api/v1/used-car-orders/summary` | `used_car/router.go:40` | aligned |
| 78 | `/api/v1/user/addresses` | `mine/address/AddressApi.kt:23` | `GET/POST /api/v1/user/addresses` | `delivery/http/router/user_routes.go:29; delivery/http/router/user_routes.go:30` | aligned |
| 79 | `/api/v1/user/addresses/:v` | `mine/address/AddressApi.kt:26` | `DELETE/GET/PATCH /api/v1/user/addresses/:v` | `delivery/http/router/user_routes.go:31; delivery/http/router/user_routes.go:32; delivery/http/router/user_routes.go:34` | aligned |
| 80 | `/api/v1/user/addresses/:v/default` | `mine/address/AddressApi.kt:29` | `POST /api/v1/user/addresses/:v/default` | `delivery/http/router/user_routes.go:33` | aligned |
| 81 | `/api/v1/user/huawei/login` | `auth/AuthApiContracts.kt:31` | `POST /api/v1/user/huawei/login` | `delivery/http/router/user_routes.go:21` | aligned |
| 82 | `/api/v1/user/login` | `auth/AuthApiContracts.kt:16` | `POST /api/v1/user/login` | `delivery/http/router/user_routes.go:16` | aligned |
| 83 | `/api/v1/user/logout` | `auth/AuthApiContracts.kt:34` | `POST /api/v1/user/logout` | `delivery/http/router/user_routes.go:25` | aligned |
| 84 | `/api/v1/user/phone/otp/send` | `auth/AuthApiContracts.kt:22` | `POST /api/v1/user/phone/otp/send` | `delivery/http/router/user_routes.go:18` | aligned |
| 85 | `/api/v1/user/phone/otp/verify` | `auth/AuthApiContracts.kt:25` | `POST /api/v1/user/phone/otp/verify` | `delivery/http/router/user_routes.go:19` | aligned |
| 86 | `/api/v1/user/refresh` | `auth/AuthApiContracts.kt:37` | `POST /api/v1/user/refresh` | `delivery/http/router/user_routes.go:17` | aligned |
| 87 | `/api/v1/user/register` | `auth/AuthApiContracts.kt:19` | `POST /api/v1/user/register` | `delivery/http/router/user_routes.go:15` | aligned |
| 88 | `/api/v1/user/wechat/login` | `auth/AuthApiContracts.kt:28` | `POST /api/v1/user/wechat/login` | `delivery/http/router/user_routes.go:20` | aligned |
| 89 | `/api/v1/wallet` | `wallet/WalletApi.kt:133` | `GET /api/v1/wallet` | `wallet/router.go:32` | aligned |
| 90 | `/api/v1/wallet/cards` | `wallet/WalletApi.kt:135` | `GET/POST /api/v1/wallet/cards` | `wallet/router.go:34; wallet/router.go:35` | aligned |
| 91 | `/api/v1/wallet/cards/:v` | `wallet/WalletApi.kt:138` | `DELETE /api/v1/wallet/cards/:v` | `wallet/router.go:37` | aligned |
| 92 | `/api/v1/wallet/cards/:v/default` | `wallet/WalletApi.kt:140` | `POST /api/v1/wallet/cards/:v/default` | `wallet/router.go:36` | aligned |
| 93 | `/api/v1/wallet/ledger` | `wallet/WalletApi.kt:134` | `GET /api/v1/wallet/ledger` | `wallet/router.go:33` | aligned |
| 94 | `/api/v1/wallet/recharge` | `wallet/WalletApi.kt:136` | `POST /api/v1/wallet/recharge` | `wallet/router.go:38` | aligned |

## B. 后端有而 KMP 无（missing(待接线) / 已登记 gap）

> 每行都对应后端真实存在的路由（带 `file:line`），不是编造。`待接线` = 客户端 KMP 后续应补；
> 对公网回调 / 服务端管理接口，注明「非客户端目标，仅登记 gap」。

| # | 后端 method+path | 后端 file:line | 结论 | 说明 |
|---|---|---|---|---|
| 1 | POST `/api/v1/cs-agent/chat` | `delivery/http/router/cs_agent_routes.go:13` | missing(待接线) | 客户端待接线（登记 gap） |
| 2 | POST `/api/v1/mall/products` | `mall/router.go:42` | missing(待接线) | 客户端待接线（登记 gap） |
| 3 | POST `/api/v1/mall/products/:v/skus` | `mall/router.go:43` | missing(待接线) | 客户端待接线（登记 gap） |
| 4 | POST `/api/v1/mall/skus/:v/codes` | `mall/router.go:44` | missing(待接线) | 客户端待接线（登记 gap） |
| 5 | GET `/api/v1/me/permissions` | `access/router.go:55` | missing(待接线) | access 域门店/角色管理接口，客户端 KMP 暂未接线（登记 gap） |
| 6 | GET/PATCH `/api/v1/me/profile` | `delivery/http/router/profile_routes.go:24; delivery/http/router/profile_routes.go:25` | missing(待接线) | 客户端待接线（登记 gap） |
| 7 | POST `/api/v1/membership/huawei/notifications` | `membership/router.go:48` | missing(待接线) | 公网渠道回调 / 服务端占位（恒 501），非客户端调用目标，仅登记 gap |
| 8 | GET `/api/v1/new-car-follow-files/customers` | `new_car_follow/router.go:40` | missing(待接线) | 客户端待接线（登记 gap） |
| 9 | POST `/api/v1/payments/notify/alipay` | `payment/router.go:41` | missing(待接线) | 公网渠道回调 / 服务端占位（恒 501），非客户端调用目标，仅登记 gap |
| 10 | POST `/api/v1/payments/notify/wechat` | `payment/router.go:40` | missing(待接线) | 公网渠道回调 / 服务端占位（恒 501），非客户端调用目标，仅登记 gap |
| 11 | POST `/api/v1/push/send` | `delivery/http/router/jpush_routes.go:16` | missing(待接线) | 服务端推送/管理触发口，非客户端调用目标，仅登记 gap |
| 12 | POST `/api/v1/realtime/push` | `delivery/http/router/realtime_routes.go:16` | missing(待接线) | 服务端推送/管理触发口，非客户端调用目标，仅登记 gap |
| 13 | DELETE/POST `/api/v1/roles/assignments` | `access/router.go:48; access/router.go:49` | missing(待接线) | access 域门店/角色管理接口，客户端 KMP 暂未接线（登记 gap） |
| 14 | POST `/api/v1/short-videos/:v/review` | `short_video/router.go:49` | missing(待接线) | 客户端待接线（登记 gap） |
| 15 | POST `/api/v1/stores` | `access/router.go:40` | missing(待接线) | access 域门店/角色管理接口，客户端 KMP 暂未接线（登记 gap） |
| 16 | POST `/api/v1/stores/:v/join-applications` | `home/router.go:48` | missing(待接线) | access 域门店/角色管理接口，客户端 KMP 暂未接线（登记 gap） |
| 17 | POST `/api/v1/stores/:v/members` | `access/router.go:41` | missing(待接线) | access 域门店/角色管理接口，客户端 KMP 暂未接线（登记 gap） |
| 18 | DELETE `/api/v1/stores/:v/members/:v` | `access/router.go:42` | missing(待接线) | access 域门店/角色管理接口，客户端 KMP 暂未接线（登记 gap） |
| 19 | GET/POST `/api/v1/transactions/manage` | `transaction/router.go:58; transaction/router.go:59` | missing(待接线) | 客户端待接线（登记 gap） |
| 20 | DELETE/GET/PUT `/api/v1/transactions/manage/:v` | `transaction/router.go:60; transaction/router.go:61; transaction/router.go:62` | missing(待接线) | 客户端待接线（登记 gap） |
| 21 | GET `/api/v1/transactions/search` | `transaction/router.go:48` | missing(待接线) | 客户端待接线（登记 gap） |
| 22 | POST `/api/v1/user/deactivate` | `delivery/http/router/user_routes.go:26` | missing(待接线) | 客户端待接线（登记 gap） |

### B.1 已接线但审计解析器盲区（不计入待接线）

KMP 生产代码确实会发出以下 4 条请求，但 `resolve.py` 无法把 `${RongImConfig.GROUPS_FREE_PATH}`（`ClassName.CONST`）
与运行时 `$action` 变量归一化成具体路径，故端点集合里看不到它们 —— **不是后端/KMP 差异**。

| 后端 method+path | 后端 file:line | KMP 接线处 | 结论 |
|---|---|---|---|
| POST `/api/v1/im/groups/free/:v/dismiss` | `delivery/http/router/im_routes.go:28` | `chat/ImGroupApi.kt:145-146 + chat/RongImConfig.kt:77` | aligned（已接线，解析器盲区） |
| POST `/api/v1/im/groups/free/:v/invite` | `delivery/http/router/im_routes.go:25` | `chat/ImGroupApi.kt:118-120 + chat/RongImConfig.kt:77` | aligned（已接线，解析器盲区） |
| POST `/api/v1/im/groups/free/:v/kick` | `delivery/http/router/im_routes.go:26` | `chat/ImGroupApi.kt:128-130 + chat/RongImConfig.kt:77` | aligned（已接线，解析器盲区） |
| POST `/api/v1/im/groups/free/:v/quit` | `delivery/http/router/im_routes.go:27` | `chat/ImGroupApi.kt:138-139 + chat/RongImConfig.kt:77` | aligned（已接线，解析器盲区） |

## C. KMP 侧非端点路径常量 / 测试夹具（mock(自造)）

### C.1 组合前缀常量（后端无对应裸路由）

这些是 `const val BASE/PREFIX/ROOT`，仅用于拼装子路径；后端对应 `Group` 没有 `GET("")`/`POST("")` 的裸路由。
它们不是被调用的接口，保留作为组合前缀（`after-sales`、`community` 在 Dart 侧同样存在 → 审计对齐；
`/api/v1/mall` 仅 KMP 有 → 已入 `audit-flutter-drift.sh` 书面 allowlist）。

| KMP 路径 | KMP file:line | 后端 | 结论 | 处置 |
|---|---|---|---|---|
| `/api/v1/after-sales` | `home/aftersales/AfterSalesApi.kt:26` | 后端 `<group>` 下无裸路由 | mock(自造)（组合前缀常量） | 保留（前缀）；Dart 侧同款 → 审计对齐，无需 allowlist |
| `/api/v1/community` | `community/http/HttpCommunityPostRepository.kt:32` | 后端 `<group>` 下无裸路由 | mock(自造)（组合前缀常量） | 保留（前缀）；Dart 侧同款 → 审计对齐，无需 allowlist |
| `/api/v1/mall` | `commerce/MallApi.kt:232` | 后端 `<group>` 下无裸路由 | mock(自造)（组合前缀常量） | 保留（前缀）；登记 `audit-flutter-drift.sh` 书面 allowlist，理由见 `notes/route-inventory.md` |

### C.2 契约测试里的自造路径（仅出现在 commonTest，不进生产端点集合）

`core/network/src/commonTest/.../AuthRefreshingApiClientTest.kt` 用任意 URL 验证 401→刷新→重试逻辑；
这些路径后端不存在，也**不是**契约端点，审计脚本的 `dart_kt_ep_files` 按 `Test.kt` 过滤、不计入 drift。
按「登记为产品确认的本地 mock（测试夹具）并写明」处置：保留、书面说明，不删除、不自造后端接口。

| KMP 路径 | KMP file:line | 后端 | 结论 |
|---|---|---|---|
| `/api/v1/chat/list` | `core/network/.../AuthRefreshingApiClientTest.kt:114,199,233,264,273` | 后端无此路径（测试夹具；后端相近路径为 `/api/v1/me/profile` 与 `/api/v1/im/*`，均不同） | mock(自造)（测试夹具，产品确认） |
| `/api/v1/chat/unread` | `core/network/.../AuthRefreshingApiClientTest.kt:200` | 后端无此路径（测试夹具；后端相近路径为 `/api/v1/me/profile` 与 `/api/v1/im/*`，均不同） | mock(自造)（测试夹具，产品确认） |
| `/api/v1/mine/profile` | `core/network/.../AuthRefreshingApiClientTest.kt:125,302,319,320` | 后端无此路径（测试夹具；后端相近路径为 `/api/v1/me/profile` 与 `/api/v1/im/*`，均不同） | mock(自造)（测试夹具，产品确认） |
| `/api/v1/mine/invoice` | `core/network/.../AuthRefreshingApiClientTest.kt:250` | 后端无此路径（测试夹具；后端相近路径为 `/api/v1/me/profile` 与 `/api/v1/im/*`，均不同） | mock(自造)（测试夹具，产品确认） |
| `/api/v1/mine/nickname` | `core/network/.../AuthRefreshingApiClientTest.kt:303` | 后端无此路径（测试夹具；后端相近路径为 `/api/v1/me/profile` 与 `/api/v1/im/*`，均不同） | mock(自造)（测试夹具，产品确认） |
| `/api/v1/mine/address` | `core/network/.../AuthRefreshingApiClientTest.kt:304,321` | 后端无此路径（测试夹具；后端相近路径为 `/api/v1/me/profile` 与 `/api/v1/im/*`，均不同） | mock(自造)（测试夹具，产品确认） |
| `/api/v1/home/feed` | `core/network/.../AuthRefreshingApiClientTest.kt:282` | 后端无此路径（测试夹具；后端 `/api/v1/home/*` 下另有一组待办接口，不是 feed） | mock(自造)（测试夹具，产品确认） |

## D. 方法级差异备注（路径级 aligned，但个别 verb 未接线）

- `POST /api/v1/after-sales/records`：coordinator 初判「KMP 缺」，**逐条核实后 KMP 已接线** —— `AfterSalesApi.kt:70` `create()` → `:95 postApiDecoded("$BASE_PATH/records")`；后端 `after_sales/router.go:41` 有 POST，路径与方法都对齐。
- `GET/PUT/DELETE /api/v1/transactions/:v`：KMP `HomeTransactionApi.kt:71` 只接了 GET；后端 `transaction/router.go:49-51` 还有 PUT / DELETE，属**方法级待接线**（路径级已 aligned）。
- `GET/DELETE /api/v1/short-videos/:v`、`POST/DELETE /api/v1/short-videos/:v/like`：KMP `ShortVideoRepository.kt` 复用 `item()/like()` 拼路径，verb 由调用方给出；本表按路径对齐，未逐 verb 展开。

## E. 与审计脚本（KMP↔Flutter）的关系

- 审计脚本的 1 条 `missing endpoint` = `/api/v1/payments/prepay`：本轮已补 KMP 契约（A 区 #54），KMP↔Flutter 归零。
- 审计脚本的 3 条 `extra endpoint` 与 40 条 `extra route`（经 `LC_ALL=C` 与别名归一后余 33 条）的逐条分类、别名映射、allowlist 理由见 `notes/route-inventory.md`。
- 本表不修改后端、不发明后端接口；B 区每行均可回溯到 `app/internal/**/router.go` 的真实注册行。

## F. 对齐状态（Wave 3，纯双方逐字段核对结论）

> 本小节由 Wave 3 D 槽登记。范围：**仅 KMP ↔ Go 后端 `my_go_study` 双方**、不涉及任何第三方；后端仓库全程只读。
> A/B/C 区 22 条 `missing(待接线)` 的原有措辞**未改**，仅在 F.3 追加状态列备注。
>
> **范围排除（外部前置，非本轮范围）**：极光/JPush、微信/支付宝 OpenSDK、Apple/Huawei/Google IAP、融云 IM、Supabase、短信/OTP 服务商、LLM provider、对象存储、第三方渠道回调与恒 501 占位、Prometheus。这些不列为「需对齐」；B 区 #7/#9/#10/#11/#12 行原已注明「公网渠道回调 / 服务端占位（恒 501），非客户端调用目标」。
>
> **状态词**（只用五个）：`Migrated | Degraded | Placeholder | Deferred | Partial`。按 [platform-gap-registry.md](../openspec/changes/parity-flutter-to-kmp/notes/platform-gap-registry.md) 的 `Rule (2A)`：`Migrated` 仅当三端落地 + 数据契约对齐 + 链接证据；**页面可开 ≠ 数据契约接线 ≠ Migrated**。以下「已对齐」指**字段/协议层逐字段核对一致**，不等于能力 `Migrated`（真机 e2e / 三端像素证据仍缺）。
>
> **行号口径**：下列 `file:line` 是 Wave 3 D 槽核对时（2026-11-11）的**代码快照**。本轮 A/B/C 槽并发改动同一批 `core/network` / `feature/auth` / `feature/pay` 文件，行号可能随改动微调；定位以「文件路径 + 符号名」为准，行号仅作跳转辅助，不作唯一依据。

### F.1 已逐字段核对为「对齐」（勿误判为差距）

| # | 契约点 | KMP 证据（file:line） | 后端证据（file:line） | 结论 |
|---|---|---|---|---|
| 1 | realtime 换票/补投**裸 body** camelCase：`{ticket,wsUrl,expiresInSeconds,connId}` / `{events,latestSeq}`，**非** `{code,message,data}` 信封 | `core/network/realtime/RealtimeWsTicketApi.kt:57-58,69-73`、`RealtimeWsSyncApi.kt:17,52-55,65-67` | `app/internal/delivery/http/dto/response/realtime.go:5-17` | aligned |
| 2 | WS `RealtimeEnvelope{id,type,topic,ts,seq,payload}` 与 `sys.notify` / `presence.bulk` 主题 | `core/network/realtime/RealtimeEnvelope.kt:27,74-76,98`、`RealtimeConfig.kt:81-82` | `model/entity/realtime_envelope.go:16-38`、`app/internal/delivery/ws/handler.go:78` | aligned |
| 3 | SSE `event: meta/delta/done/error` 与 `CompletionRequest` camelCase（`prompt`/`conversationId`/`clientRequestId`） | `core/network/.../core/sse/AiSseDecoder.kt:44-58`（`meta/delta/done/error`）、`feature/ai/AiSseApi.kt:74-84`（`completionsRequestBody`） | `model/entity/sse_event.go:5-10`、`app/internal/delivery/http/dto/request/sse_completion.go:4-9`、`app/internal/delivery/http/controller/sse_controller.go:111-114` | aligned（与 `/api/v1` 其余 snake_case 不同，勿顺手统一） |
| 4 | prepay 五键 `channel/amount_fen/subject/ref_type/ref_id` → `PrepayResult{channel,out_trade_no,params}` | `feature/pay/PaymentPrepayApi.kt:21-23,55-75,78-83`（`PrepayRequest` / `PrepayResponse`） | `app/internal/payment/handler.go:30-33`、`app/internal/payment/service.go:66-70` | aligned（A 区 #54） |
| 5 | push devices 五键 `registration_id/alias/device_id/platform/mock` | `component/src/commonMain/.../push/PushRegistrationApi.kt:60-64,78-82,85-97`（`buildJsonObject` / `KEY_*` / `PLATFORM_*`） | `app/internal/delivery/http/controller/jpush_controller.go:36-40` | aligned |
| 6 | 分页 `Pagination{page,size,total,totalPages}`、`page` 从 1 起（`Offset=(page-1)*size`） | `core/network/ListPage.kt:20-24,70-77` | `app/internal/delivery/http/dto/response/common.go:44-45,59-62` | aligned |
| 7 | 登录/刷新 DTO snake_case（`device_id`/`refresh_token`/`session_id`）、刷新请求空键省略 | `feature/auth/AuthApiContracts.kt:43-48,63-73,230-239`（登录/注册/刷新 DTO 的 `@SerialName`） | `app/internal/delivery/http/dto/request/user_request.go:8-9,31` | aligned |
| 8 | 会话三件套 `X-Session-ID` / `X-Device-ID` 与 `Authorization` 同一 gating | `core/network/DemoRequestHeaders.kt:11-12`、`NetworkFacade.kt:77` | `app/internal/delivery/http/middleware/supabase_session_auth.go:15-16,36-37` | aligned（Wave 1 已落） |
| 9 | `AuthBizCode` 10021–10024 与单飞 401 静默刷新 | `feature/auth/AuthBizCode.kt:8,23-32,37-46`、`AuthHttpDecision.kt:8-9,40,53`、`core/network/TokenRefresh.kt:10-11` | `app/internal/delivery/http/dto/response/common.go:22-27` | aligned |

> 逐条结论同步进 [platform-gap-registry.md](../openspec/changes/parity-flutter-to-kmp/notes/platform-gap-registry.md) 对应行备注（D3）；未由 A/B/C 覆盖的结构性 gap 见 [backend-alignment-gaps.md](backend-alignment-gaps.md)（D5）。

### F.2 纯双方仍未对齐（本轮 A/B/C 槽已改代码，D 槽只登记结论与证据锚点）

> 以下均为**双方契约层**差异，本轮由 A/B/C 槽在代码侧处置；D 槽不改代码、只登记结论与 file:line 锚点，具体改动的最终验收由 coordinator 统一跑。
> 未完成项一律保留 blocker + follow_up，不判绿。

| # | 差异 | 现状证据（file:line） | 状态 | blocker | follow_up |
|---|---|---|---|---|---|
| 1 | 错误码双轨：`core/network/NetworkCodes` 遗留小码表（`OK=200`/`EXPIRE_TOKEN=401`/`FORBIDDEN=403`）vs 后端五位数码表（`10002–10005`/`10021–10024`/`50000`）；`10005`（HTTP 501 能力未接入）无分支；非 401 的 `{error,code}` 分类；信封 `timestamp` | `core/network/NetworkCodes.kt:21-57`（`object NetworkCodes`）、`app/internal/delivery/http/dto/response/common.go:17-30` | `Partial` | 小码表与五位数码表并存，`0` 同值不同义 | A 槽统一码表分支 + 契约测试；信封 `timestamp` 补解析 |
| 2 | OHOS `body.platform` 被映射成 `android`（后端 `binding:"required,oneof=android ios"` 只认二值） | `feature/auth/AuthApiContracts.kt:357-380`（`AuthDeviceContext.platformForBody` / `platform()`）、`app/internal/delivery/http/dto/request/user_request.go:9,18,31,38,45`、`app/internal/usecase/device_session_usecase.go:60-61` | `Partial` | 后端不接受 `ohos`/`harmony`，客户端只能伪报 | 后端扩 `oneof` 或客户端登记伪值语义，勿静默伪装 |
| 3 | `Client-Type`/`Client-Version`/`Client-BuildNumber` 三头后端不读（仅 `X-Session-ID`/`X-Device-ID`/`Authorization` 被读） | `core/network/DemoRequestHeaders.kt:16-19`、`app/internal/delivery/http/middleware/local_session_auth.go:36-37`（后端全仓无 `Client-Type` 读取点） | `Partial` | 后端未消费三头，客户端发了也无人读 | 三头用途书面登记；若确需后端消费再提后端变更（后端只读） |
| 4 | realtime 少发 `connId`（换票响应带 `connId`，客户端后续链路未回带） | `core/network/realtime/RealtimeWsTicketApi.kt:73`、`app/internal/delivery/http/dto/response/realtime.go:10` | `Partial` | 客户端未把 `connId` 串进后续链路 | B 槽补 `connId` 回带 + 契约测试 |
| 5 | alias 被后端强制覆盖（越权防护：忽略客户端 `alias`，强制绑定会话用户） | `component/src/commonMain/.../push/PushRegistrationApi.kt:41`、`app/internal/delivery/http/controller/jpush_controller.go:53` | `Partial` | 客户端语义与后端实际落库不一致 | 文档/契约测试注明「alias 由服务端覆盖」，勿依赖客户端传值 |
| 6 | realtime 错误体非统一信封（`{"error":…}` / WS `type=error`，与 `{code,message,data}` 不同） | `core/network/Sse.kt:83-97`、`core/network/realtime/RealtimeEnvelope.kt:89` | `Partial` | 错误体形态分叉 | B 槽补错误体解析分支 + 契约测试 |
| 7 | transactions 双分页（`ListPage` 新 `pagination` 包装 vs `listLegacy` 的 `{"items":[...]}` 旧形） | `core/network/ListPage.kt:16`、`app/internal/transaction/handler.go:75-105` | `Partial` | 新旧两种分页响应并存 | 逐端点标注走哪套，勿混用 |
| 8 | 两层鉴权链（SessionAuth 中间件 + AccountGate/DeviceSessionUsecase），客户端只建模一层 | `app/internal/delivery/http/middleware/supabase_session_auth.go:36-37`、`app/internal/usecase/device_session_usecase.go:60-61` | `Partial` | 客户端 403/401 分支未区分两层 | C 槽补 KDoc + 403「账号不可用」用例 |
| 9 | 403「账号不可用」业务语义（区别于 10003 无权限 / 10021 互踢） | `app/internal/delivery/http/dto/response/common.go:17-27`、`core/network/NetworkCodes.kt`（`FORBIDDEN` / `EXPIRE_TOKEN`） | `Partial` | `FORBIDDEN=403` 现不登出、不刷新，仅业务提示 | 明确 403 与业务码映射，补契约测试 |
| 10 | 金额三套口径（元 `amount` / 分 `amount_fen` / 字符串展示），亚分精度边界见 [backend-alignment-gaps.md](backend-alignment-gaps.md) | `app/internal/transaction/repo_postgres.go:15-17`、`app/internal/transaction/money_fen_integration_test.go:161-176` | `Partial` | 客户端未统一金额口径 | 统一以分为存储口径、元为展示口径并写边界 |
| 11 | 14 条后端自有端点待接（B 区属客户端目标者）+ `PUT/DELETE /transactions/:v`、`/user/deactivate` | B 区表 + `notes/backend-contract-matrix.md:196-200`（D 区方法级差异） | `Deferred` | 客户端契约未登记 / 方法级未接线 | 逐条补契约（不编造后端路由，均带 `router.go` 行号） |

### F.3 B 区 22 条 `missing(待接线)` 的 Wave 3 状态列备注（措辞未改）

| 分组 | B 区行号 | Wave 3 状态 | 说明 |
|---|---|---|---|
| 客户端目标（待补契约） | #1–#4、#6、#8、#14、#19–#22 | `Deferred` | 后端真实路由、KMP 暂未接线；A/B/C 槽本轮按 F.2 逐条补契约，未完成的保留 blocker + follow_up |
| 公网渠道回调 / 服务端占位（恒 501） | #7、#9、#10 | `Deferred` | **外部前置，非本轮范围**：非客户端调用目标，仅登记 gap |
| 服务端推送 / 管理触发口 | #11、#12 | `Deferred` | 非客户端调用目标，仅登记 gap |
| access 域门店/角色管理 | #5、#13、#15–#18 | `Deferred` | 后端管理接口，客户端 KMP 暂未接线；见 [backend-alignment-gaps.md](backend-alignment-gaps.md) |

> 22 条措辞与 `aligned/missing/mock` 结论均保持 wave1 B 槽原文；本表仅为状态列备注（不改 A/B/C 区正文、不新增后端路由）。

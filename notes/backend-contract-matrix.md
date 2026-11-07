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

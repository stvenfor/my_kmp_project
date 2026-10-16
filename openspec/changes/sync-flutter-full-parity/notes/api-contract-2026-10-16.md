# `/api/v1` 端点契约表（Dart 调用点实测，my_ai_project @ d7876a1）

> 由 2026-10-16 全量扫描 Dart 调用点得到：**111 个 verb+path 调用对 / 94 个不同 path 模板**。
> 这是 M1（网络契约同步）的唯一 SoT；任何 KMP 侧新增调用必须能在这里找到对应行。
> 之前估的「50 个缺失端点」低估了：真实缺口按模块分组见 §7。

## 0. 全局语义（先读，会推翻若干 KMP 现有假设）

| 主题 | Flutter 事实 | 出处 |
|---|---|---|
| 信封 | `{code:Int, message:String, data:..., timestamp:Int?}` | `commons/network/lib/api/result_model.dart` |
| **成功码是 `0`** | `bool get isSuccess => code == 0`（result_model.dart:28）；**遗留 `wys_network` 表才是 `ok=200`** | `commons/wys_network/lib/src/network_codes.dart` |
| 缺 `code` | `code ?? -1` → 一律判失败 | result_model.dart:16 |
| 非信封体 | 缺 `code` 且 body 是 `{error:"..."}` → 抛错，message 取 `error` | `commons/network/lib/http/backend_response_parser.dart:17-27` |
| HTTP status | `validateStatus` 接受 **200–599**：状态码本身从不构成失败，只有 `code` 构成 | `commons/network/lib/http/app_http_bootstrap.dart:80` |
| Base URL | `BackendHttpConfig.resolveBackendBaseUrl()` | `app_http_bootstrap.dart:14` |
| Auth 头 | `HeaderInterceptor` 给**每个**请求注入 `Authorization: Bearer <access>` + `X-Session-ID` + `X-Device-ID`；未登录时 `AuthHeaderProvider` 省略 | `commons/network/lib/http/auth_header_provider.dart:20-32` |
| skipAuthRefresh | `Options.extra['skipAuthRefresh']` 让刷新拦截器跳过（`/user/login|register|refresh|logout` 同豁免） | `features/auth/lib/session/auth_token_refresh_interceptor.dart:26-42` |
| 重试 | `RetryInterceptor` 仅对 HTTP≥500 重试，`500ms·(n+1)`；**只有 Mine/PurchaseCalculator 配 `maxRetries:3`**（那里的 POST 可能重复） | `commons/network/lib/http/retry_interceptor.dart:62-66` |

**2026-10-16 已在 KMP `:core:network` 落地的对应修正**：
`NetworkCodes.BFF_OK = 0` + `ApiResponse.isSuccess` 双码（0 / 200）兼容、`ApiEnvelopeParser`
缺 `code` / 非信封体回退 `PASSTHROUGH(-1)`（原回退 `0` 会把畸形响应判成成功）、
`putRaw/patchRaw/deleteRaw + putApi/patchApi/deleteApi`、`ListPage`/`ListPagination`/
`queryOf`/`pageQuery`/`limitOffsetQuery`（`ListData`+`PaginationModel`+`PageResult` 对等）、
`AuthRefreshingApiClient` 五个 `*Api` 出口统一走 401 刷新。

## 1. 动词统计

| Verb | 调用对 | 说明 |
|---|---|---|
| GET | 47 | 列表/详情/汇总 |
| POST | 53 | 含动作式 POST：`/approve` `/reject` `/pay` `/cancel` `/claim` `/default` `/view` `/respond` `/invite` `/kick` `/quit` `/dismiss` |
| PATCH | 4 | `/profiles/me`、`/new-car-follow-files/:fileId`、`/deal-invoices/:invoiceId/resubmit`、`/user/addresses/:addressId` |
| DELETE | 7 | `/community/posts/:postId`、`/community/posts/:postId/like`、`/community/users/:userId/follow`、`/short-videos/:id`、`/short-videos/:id/like`、`/user/addresses/:addressId`、`/wallet/cards/:cardId` |
| PUT | **0** | `HttpManager.put` 存在（`commons/network/lib/http/http.dart:380`）但**全仓无调用点**。KMP 保留 `putApi` 只是与 Dart client 表面对齐，M1 不得为它编造端点 |

`HttpManager.instance.get/post/put/patch/delete` → `HttpMethod` 1:1（`http.dart:338-480,541-544`）。
`upload` = POST + FormData（`/api/v1` 未用）。SSE 走 `dio.post` 直连。没有端点把动词藏在 helper 里。

## 2. 契约表（按 Dart 模块）

### 2.1 auth/user — `features/auth/lib/api/user_auth_api.dart`

| # | Verb | Path | Auth | 请求 | `data` | 备注 / 行号 |
|---|---|---|---|---|---|---|
| 1 | POST | `/user/login` | 公开 | `{username,password,device_id,platform:String}` | `LoginResult{token,refresh_token,session_id,user:BackendUser}` | `ResultModel.object`；`BackendUser{user_id/id, user_name/username/display_name, email, status:int, phone, avatar_url\|avatarUrl}` — user_auth_api.dart:22 |
| 2 | POST | `/user/register` | 公开 | `{username,password,email,device_id,platform}` | `RegisterResult{token?,refresh_token?,session_id?,user}` | `data` 可带可不带嵌套 `user`（双形态）；空 map → 全 `''` user — :141 |
| 3 | POST | `/user/refresh` | 公开 + skipAuthRefresh | `{refresh_token, device_id?, session_id?, platform?}`（null/空则省略 key） | `RefreshTokenResult{token,refresh_token,session_id}` | 静默续期 — :95 |
| 4 | POST | `/user/logout` | **手工头** + skipAuthRefresh | 无 body | `null` | 只看 `code==0`；会话已死时**不调**（`session_guard.dart:216` `remote:false`）— :116 |
| 5 | POST | `/user/phone/otp/send` | 公开 | `{phone}` | `null` | :38 |
| 6 | POST | `/user/phone/otp/verify` | 公开 | `{phone,otp,device_id,platform}` | `LoginResult` | :54 |
| 7 | POST | `/user/wechat/login` | 公开 | `{code,device_id,platform}` | `LoginResult` | :74 |

### 2.2 profile / 门店 — `features/auth/lib/api/user_profile_api.dart`

| # | Verb | Path | 请求 | `data` | 备注 / 行号 |
|---|---|---|---|---|---|
| 8 | GET | `/profiles/me` | query `store_id?`（trim，空则省略） | `UserProfile{id,user_id,user_name,email,status,display_name\|displayName,avatar_url\|avatarUrl,phone,stats:UserStoreStats}` | 大小写混用都接受；`stats` 是 snake_case — :14 |
| 9 | PATCH | `/profiles/me` | `{user_name?,display_name?,avatar_base64?,avatar_mime?}`；改名同时写两字段；`avatar_mime` 默认 `image/jpeg` 且仅随 `avatar_base64` 发 | `UserProfile` | auth 模块唯一 PATCH；`10001` → `UnknownAuthFailure('资料参数无效')` — :59 |
| 10 | POST | `/profiles/me/store` | `{store_id:int}` | `UserStoreStats{store_id,store_name,days_joined,employee_count,store_days,total_customers,role,role_label}` | `role` 0 销售顾问 /1 销售经理 /2 总经理 — :45 |
| 11 | GET | `/me/stores` | — | `{list:[{store_id,store_name,role,role_label,is_current}],current_store_id:int}` | **前缀是 `/me`，不是 `/profiles`**；服务端 `current_store_id` 覆盖本地 `mine_selected_store_id` — :30 |

### 2.3 transactions（`/transactions` 有两套查询方言）

| # | Verb | Path | 请求 | `data` | 备注 |
|---|---|---|---|---|---|
| 12 | GET | `/transactions` | `limit=size&offset=page*size`（page **0-based**）+ `type?` | `ListData<TransactionModel{id:int!,type,category,amount:double,date,user_id?,note?,created_at?,updated_at?}>` | 双信封：有 `code` 用 `listPage`，否则把裸 `{items:[]}` 当成功；`list`/`items`/裸数组都吃；**`id` 缺失会抛** — transaction_api.dart:23 |
| 13 | GET | `/transactions/:id` | — | `TransactionModel` | `data==null` → `HttpRequestException('交易记录不存在')` — :43 |
| 14 | GET | `/transactions` | `page=1&size=20` | `ListData<Map>` | Mine 调试页走 page/size 变体（Go `listLegacy` 同时支持）— mine_api.dart:11 |

### 2.4 home 待办条 — `features/home/lib/home/api/home_todo_api.dart`（全 BEARER）

| # | Verb | Path | `data` | 备注 |
|---|---|---|---|---|
| 15 | GET | `/home/todo-cards` | `[HomeTodoCard{type,title,subtitle,action_label,action_route,count:int,image_url?}]` | `data.items[]` 或裸数组；卡片尺寸由 `type` 客户端推导 — :16 |
| 16 | GET | `/home/join-applications` | `[HomeTodoJoinApplication{application_id,store_id,applicant_user_id,applicant_name,status,created_at→DateTime?}]` | :29 |
| 17 | POST | `/home/join-applications/:applicationId/approve` | 忽略 | 无 body；只看 `code==0` — :43 |
| 18 | POST | `/home/join-applications/:applicationId/reject` | 忽略 | :56 |
| 19 | GET | `/home/follow-up-customers` | `[HomeTodoFollowUpCustomer{customer_id,display_name,next_follow_up_at:DateTime?}]` | :69 |
| 20 | GET | `/home/after-sales-appointments` | `[HomeTodoAppointment{appointment_id,customer_name,appointment_date:DateTime?,status}]` | :83 |
| 21 | GET | `/home/store-review-orders` | `[HomeTodoReviewOrder{order_id,title,status,created_at:DateTime?}]` | :98 |

### 2.5 积分签到 — `features/home/lib/home/api/points_api.dart`

| # | Verb | Path | 请求 | `data` | 备注 |
|---|---|---|---|---|---|
| 22 | GET | `/points/status` | — | `PointsStatus{balance,checked_in_today,streak,today_reward(默认5),calendar:[{day,signed,reward,is_today}]}` | :13 |
| 23 | POST | `/points/check-in` | **无 body** | `CheckInResult{points,balance,streak,day}` | 幂等靠客户端防双击 — :26 |
| 24 | GET | `/points/tasks` | — | `[GrowthTask{code,title,reward,progress:'incomplete'\|'claimable'\|'claimed'}]` | 手搓转换器：`data.items` 否则根 `items`；无 `code` 时合成 `code:0` — :40 |
| 25 | POST | `/points/tasks/:code/claim` | 无 body | `ClaimTaskResult{points,balance,day}` | `:code` 是字符串任务码 — :68 |
| 26 | GET | `/mall/stores/:storeId/products` | `page=1,size=20`，`storeId` **默认 1** | `ListData<PointsGiftItem{product_id\|id,sku_id,title,cover_url,price:String,price_points}>` | 客户端过滤 `price_points>0`；**整个调用 try/catch → `const []`**（失败静默）— :87 |

### 2.6 mall — `features/mall/lib/mall/api/mall_api.dart`

| # | Verb | Path | 请求 | `data` | 备注 |
|---|---|---|---|---|---|
| 27 | GET | `/mall/stores/:storeId/products` | `page`(1-based),`size=10` | `ListData<MallProductCardModel{product_id\|id,sku_id,title,price:String,cover_url,cover_aspect:double,kind,subtitle?,price_points}>` | `soldLabel`/`badges` 由 `id.hashCode` 客户端生成，**不是服务端字段** — :18 |
| 28 | GET | `/mall/stores/:storeId/products/:productId` | — | `{product:{product_id,title,cover_url,cover_aspect,kind}, skus:[{sku_id,title,price,price_points,stock_qty,specs:Map,deliver_type?}]}` | — | :46 |
| 29 | POST | `/mall/cart` | `{sku_id:int, qty:int}` | 原始 Map（丢弃） | :60 |
| 30 | POST | `/mall/orders` | `{idempotency_key:'$skuId-<micros>', store_id:int, lines:[{sku_id,qty}], receiver_name, receiver_phone, receiver_address}`（receiver 三件套默认 `''` 且总发） | `{order:{order_id,...}}` | `order_id<=0` → 抛「下单失败：缺少订单号」；**key 每次重新生成 ⇒ 超时重试不能去重** — :83 |
| 31 | GET | `/mall/orders` | `page,size=10,status?`，status 为**逗号串**：`null` 全部 / `"0"` 待付 / `"1,2"` 已付+履约 / `"3,4"` 取消+关闭 | `ListData<MallOrderListRow{order_id,order_no,store_id,status,amount,created_at,pay_deadline_at,items:[MallOrderItem]}>` | 状态 0 待支付 1 已支付 2 已履约 3 已取消 4 已关闭 — :149 |
| 32 | GET | `/mall/orders/:orderId` | — | `{order:{order_id,order_no,store_id,status,amount,created_at,receiver_*,payment_channel?,paid_at,total_points}, items:[MallOrderItem], pay_deadline_at}` | **`pay_deadline_at` 读根 `data`，不在 `order` 里** — :171 |
| 33 | POST | `/mall/orders/:orderId/pay` | `{payment_channel:int}` | 同 #32 | `buy()` = #30→#33 两步 — :188 |
| 34 | POST | `/mall/orders/:orderId/cancel` | 无 body | 原始 Map | 取消是 POST，不是 PATCH/DELETE — :203 |

`MallOrderItem{item_id,order_id,product_title,cover_url,qty,line_amount,kind,content_url}`；`kind==1` ⇒ 虚拟商品。
`mall_order_detail_controller.dart:75` 另 GET `/wallet` 只读 `data.balance`（失败吞掉、非阻塞）。

### 2.7 二手车订单 — `features/home/lib/home/api/used_car_order_api.dart`

| # | Verb | Path | 请求 | `data` | 备注 |
|---|---|---|---|---|---|
| 35 | GET | `/used-car-orders/summary` | — | `{display_name,avatar_url,position_label,store_name,stats:{submitted,pending_review,approved,rejected}}` | `stats` 缺省 `{}` — :11 |
| 36 | GET | `/used-car-orders` | `page`(1-based),`size=10`,`status:'all'\|'pending_review'\|'approved'\|'rejected'`,`kind?`(`trade_in\|consign\|purchase`，all 时省略) | `ListData<UsedCarOrderItem>` | `hasMore = page*size < total` — :37 |
| 37 | GET | `/used-car-orders/:orderId` | — | `UsedCarOrderItem{order_id:**String**,kind,kind_label,amount_label,customer_name,phone,vehicle_model,plate_no,vin,mileage_km,model_year,amount:double,status,submitted_at→DateTime,reject_reason?,rating_stars?,image_url?}` | `order_id` 被字符串化；status 含 `approved_pending_rating`、`rated` — :55 |
| 38 | GET | `/used-car-orders/customers` | `page=1,size=50,q?`(trim 省略) | `ListData<UsedCarCustomer{customer_id,display_name,phone}>` | 只取 `.list`，忽略 pagination — :75 |
| 39 | POST | `/used-car-orders` | `{kind,customer_id,vehicle_model,plate_no,vin,mileage_km,model_year,amount,image_url?}`（`image_url` **总发，可为 null**） | `UsedCarOrderItem` | 建单即 `pending_review` — :100 |

### 2.8 新车跟进 — `features/home/lib/new_car_follow/api/new_car_follow_api.dart`

| # | Verb | Path | 请求 | `data` | 备注 |
|---|---|---|---|---|---|
| 40 | GET | `/new-car-follow-files/summary` | — | `{display_name,avatar_url,position_label,store_name,stats:{active,overdue,high_intent,lost}}` | :12 |
| 41 | GET | `/new-car-follow-files` | `page,size=10` + tab：`intent_band:'高'\|'中'\|'低'`（**中文字面量**）或 `overdue:'1'`，all 时不带 | `ListData<NewCarFollowFile{file_id,customer_id,customer_name,customer_phone,follow_level,intent_band,stage,vehicle_interest,next_follow_up_at?,owner_user_id,owner_display_name}>` | :32 |
| 42 | GET | `/new-car-follow-files/:fileId` | — | `NewCarFollowFile` | :50 |
| 43 | POST | `/new-car-follow-files` | `{follow_level}` + (`customer_id:int>0` **或** `display_name,phone`) + 可选 `vehicle_interest,next_follow_up_at` | `NewCarFollowFile` | 互斥分支：有 `customer_id>0` 时**省略** name/phone 键，否则两键总发（即使 null）— :84 |
| 44 | PATCH | `/new-car-follow-files/:fileId` | 稀疏 `{follow_level?,stage?,next_follow_up_at?}` | `NewCarFollowFile` | `stage`: `new/following/test_drive/quoted/won/lost`；level `H/A/B/E` — :108 |
| 45 | GET | `/new-car-follow-files/:fileId/logs` | `page=1,size=20` | `ListData<NewCarFollowLog{log_id,file_id,author_user_id,author_display_name,body,follow_level,intent_band,next_follow_up_at?,created_at}>` | :127 |
| 46 | POST | `/new-car-follow-files/:fileId/logs` | `{body, follow_level?, next_follow_up_at?}` | `NewCarFollowLog` | 写日志可服务端移动档案 level/下次跟进 — :157 |

### 2.9 售后 — `features/home/lib/after_sales/api/after_sales_api.dart`

| # | Verb | Path | 请求 | `data` | 备注 |
|---|---|---|---|---|---|
| 47 | GET | `/after-sales/records` | `page,size=10` | `{list:[AfterSalesRecord{record_id,store_id,appointment_id?,customer_name,customer_phone,service_kind,service_kind_label,title,content,service_date,plate_no,mileage?}], pagination:{total}, can_create:bool}` | `data` 按**裸 map** 读，不走 `ListData`；`can_create` 控制「新建」入口 — :15 |
| 48 | GET | `/after-sales/records/:recordId` | — | `AfterSalesRecord` | :43 |
| 49 | POST | `/after-sales/records` | 总发 `{service_kind,title,service_date,customer_name?,customer_phone?}` + 条件 `appointment_id,plate_no(trim),mileage,content(trim)` | `AfterSalesRecord` | 失败抛裸 `Exception`，不是 `HttpRequestException` — :82 |
| 50 | GET | `/after-sales/pending-appointments` | — | `{list:[{appointment_id,customer_name,appointment_date}]}` | 日期按 `'T'` 截断 — :97 |

### 2.10 成交发票 — `features/settings/lib/deal_invoice/api/deal_invoice_api.dart`

| # | Verb | Path | 请求 | `data` | 备注 |
|---|---|---|---|---|---|
| 51 | GET | `/deal-invoices/summary` | — | `{display_name,avatar_url,position_label,store_name,stats:{uploaded,pending_review,approved,rejected}}` | :12 |
| 52 | GET | `/deal-invoices` | `page,size=10,status:'all'\|'pending_review'\|'approved'\|'rejected'` | `ListData<DealInvoiceItem{invoice_id\|id:String,phone,customer_name?,status,submitted_at→DateTime,reject_reason?,rating_stars?,image_url?}>` | status 域 `pending_review/approved_pending_rating/rated/rejected` — :30 |
| 53 | GET | `/deal-invoices/:invoiceId` | — | `DealInvoiceItem` | :52 |
| 54 | GET | `/deal-invoices/customers` | `page=1,size=50,q?` | `ListData<DealInvoiceCustomer{customer_id\|id:int,phone,display_name\|name}>` | :74 |
| 55 | POST | `/deal-invoices` | `{customer_id:int, image_url?}`（两键总发） | `DealInvoiceItem` | :92 |
| 56 | PATCH | `/deal-invoices/:invoiceId/resubmit` | `{image_url?}` | `DealInvoiceItem` | 驳回后重提 — :113 |

### 2.11 购车计算器 — `features/settings/lib/purchase_calculator/api/purchase_calculator_api.dart`（`maxRetries:3`）

| # | Verb | Path | 请求 | `data` | 备注 |
|---|---|---|---|---|---|
| 57 | GET | `/purchase-calculator/products` | — | `{items:[FinanceProduct{id,name,annual_rate_percent,allowed_terms_months:[int],min_down_payment_percent,one_time_fee,subsidy_type,subsidy_rate_cut_percent,subsidy_amount_cut,compulsory_insurance,commercial_rate_percent}]}` | `data.items` 非 List → 抛「产品列表为空」；**retry=3 ⇒ 这个 GET 可能重复** — :13 |
| 58 | POST | `/purchase-calculator/quote` | 总发 `{mode,bare_price,disable_commercial}` + 条件 `taxable_price,product_id,down_payment,term_months` | `PurchaseQuote{mode,product_id?,product_name,bare_price,taxable_price,down_payment,loan_amount,term_months,effective_annual_rate_percent,monthly_payment,total_interest,total_repayment,initial_payment,total_due,lines:[{code,label,amount}]}` | 报价以服务端为准 — :52 |

### 2.12 社区 — `features/community/lib/community/repository/http_post_repository.dart`

| # | Verb | Path | 请求 | `data` | 备注 |
|---|---|---|---|---|---|
| 59 | GET | `/community/posts` | `page=local+1,size=10,tab:'latest'…` | `ListData<PostModel{id,user_id,nickname,avatar,content,publish_time→DateTime,source,images[]\|image_urls[],video_url?,video_cover_url?,like_count,comment_count,is_liked,is_mine,preview_comments:[CommentModel],media_type?,is_ask_everyone,topic:TopicModel?}>` | 只返 list（无 PageResult）；`is_ask_everyone` 也可由 `topic.is_ask_everyone` 推 — :21 |
| 60 | POST | `/community/posts` | `{content, media_type:'none', image_urls:[](总发), video_url:null, video_cover_url:null, topic_id?, is_ask_everyone, source:'来自 iPhone'}` | `PostModel` | **null 显式发送** — :120 |
| 61 | GET | `/community/posts/:postId/comments` | `page=1,size=50`（固定） | `ListData<CommentModel{id,post_id,nickname,avatar,content,create_time→DateTime,reply_to_nickname?}>` | :40 |
| 62 | POST | `/community/posts/:postId/comments` | `{content, reply_to_nickname?}` | `CommentModel` | :58 |
| 63 | POST | `/community/posts/:postId/like` | 无 body | 片段 `{id,like_count,is_liked}` | like/unlike 靠**切动词** — :78 |
| 64 | DELETE | `/community/posts/:postId/like` | 无 body | 同片段 | `liked==false` 时走这条 — :86 |
| 65 | DELETE | `/community/posts/:postId` | 无 body | 原始 Map（可空） | `_dataAllowEmpty` 只看 `code` — :101 |
| 66 | GET | `/community/topics` | `page+1,size=20` | `ListData<TopicModel{id,name,heat,is_ask_everyone}>` | :147 |
| 67 | GET | `/community/topics/search` | `q,page+1,size=20` | `ListData<TopicModel>` | 与 `/search` 不同端点 — :169 |
| 68 | GET | `/community/search` | `q,type='all',page+1,size=5` | `{q, posts:ListData<PostModel>, topics:ListData<TopicModel>, users:ListData<CommunityUserHit{user_id,nickname,avatar,is_followed}>}` | 三个子对象各自带 `list/items`+`pagination` — :252 |
| 69 | GET | `/community/search` | `q,type='post'\|'topic'\|'user',page+1,size=10` | 单个 `ListData<T>` | **同端点靠 `type` 切响应形状** — :326 |
| 70 | POST | `/community/users/:userId/follow` | 无 body | 原始 Map | :220 |
| 71 | DELETE | `/community/users/:userId/follow` | 无 body | 原始 Map | :234 |

### 2.13 短视频 — `features/video/lib/short_video/repository/http_short_video_repository.dart`

| # | Verb | Path | 请求 | `data` | 备注 |
|---|---|---|---|---|---|
| 72 | GET | `/short-videos/profile` | `user_id?` | `ShortVideoProfileModel{user_id?,display_name,avatar_url?,role_badge,store_name,is_me,stats:{video_count,view_count,like_count}→String}` | stats 数字转字符串给 UI — :14 |
| 73 | GET | `/short-videos` | `scope,page+1,size=5,user_id?` | `ListData<ShortVideoItemModel{id,title,cover_url,video_url,view_count?,like_count,duration:String?,aspect_ratio=1.25,status:'normal'\|'reviewing'\|'uploading',is_liked,is_mine,user_id,nickname,avatar,topic:{id,name},publish_time}>` | `hasMore` 取 `pagination.hasMore`，否则 `len>=pageSize` — :36 |
| 74 | POST | `/short-videos` | `{title, video_url:null, cover_url:null, duration:null, aspect_ratio:null, topic_id?}` | `ShortVideoItemModel` | 上传字段全是占位 null — :63 |
| 75 | DELETE | `/short-videos/:id` | 无 body | 原始 Map | :85 |
| 76 | POST | `/short-videos/:id/like` | 无 body | `{id,like_count,is_liked}` | :101 |
| 77 | DELETE | `/short-videos/:id/like` | 无 body | 同 | :110 |
| 78 | POST | `/short-videos/:id/view` | 无 body | `{view_count:int}` | 每次播放 fire-and-forget — :125 |

### 2.14 收货地址 — `features/settings/lib/mine/address/api/address_api.dart`

| # | Verb | Path | 请求 | `data` | 备注 |
|---|---|---|---|---|---|
| 79 | GET | `/user/addresses` | — | `{items:[AddressModel{address_id,receiver_name,receiver_phone,province,city,district,detail_address,postal_code,is_default,label,full_address}]}` | 是 `items` 不是 `list` — :10 |
| 80 | POST | `/user/addresses` | UI 透传 map（`toBridgeJson` 键：`address_id,receiver_name,receiver_phone,province,city,district,detail_address,full_address,is_default,label`） | `AddressModel` | API 层不带类型 — :29 |
| 81 | PATCH | `/user/addresses/:addressId` | 同 map | `AddressModel` | 更新是 PATCH 而非 PUT — :44 |
| 82 | POST | `/user/addresses/:addressId/default` | 无 body | `AddressModel` | 设默认是 POST — :59 |
| 83 | DELETE | `/user/addresses/:addressId` | 无 body | 原始 Map | `_data` 要求非空 map — :73 |

### 2.15 钱包 — `features/wallet/lib/wallet/api/wallet_api.dart`

| # | Verb | Path | 请求 | `data` | 备注 |
|---|---|---|---|---|---|
| 84 | GET | `/wallet` | — | `WalletSummary{balance:String,balance_fen:int,cards:[BankCard{card_id,bank_name,card_last4,holder_name,is_default}]}` | **3 个调用点、2 种形状**（mall / 会员续费只读 `balance`）— :10 |
| 85 | GET | `/wallet/ledger` | `limit=20,offset=0` | `{items:[WalletLedgerEntry{ledger_id,delta_fen,balance_fen,reason:'recharge'\|'mall_pay'\|'mall_refund'\|…,ref_id}]}` | **无 list/pagination**；手搓转换器，无 `code` 时合成 0 — :27 |
| 86 | POST | `/wallet/cards` | `{bank_name,card_last4,holder_name:'',is_default:false}` | `BankCard` | :60 |
| 87 | POST | `/wallet/cards/:cardId/default` | 无 body | 原始 Map | :80 |
| 88 | DELETE | `/wallet/cards/:cardId` | 无 body | 原始 Map | :94 |
| 89 | POST | `/wallet/recharge` | `{amount:String(十进制定价串，不是分), channel:int, card_id?}` | `WalletRechargeResult{balance,balance_fen,delta_fen}` | :112 |

### 2.16 会员 / 支付 — `features/pay/lib/membership/…`（响应只按裸 Map 取键）

| # | Verb | Path | 请求 | `data` 消费键 | 备注 |
|---|---|---|---|---|---|
| 90 | GET | `/membership/me` | — | `{catalog:[{plan_id,tier:'svip'\|'ai_svip',title,price,price_fen,months,huawei_product_id,apple_product_id,auto_renew_eligible}], entitlements:[{tier,cta}]}` | `data` 非 map → `{}` — membership_api.dart:16 |
| 91 | POST | `/membership/buyout` | `{plan_id:String, channel:'wechat'\|'alipay'\|'balance'}` | `{status:'paid'\|…, order_id:int, prepay_params:Map}` | **两步之一**；`status=='paid'`（余额渠道）直接结束；`prepay_params` 原样交 SDK（微信兼容 `prepayid`/`prepayId`）— :27 |
| 92 | POST | `/membership/buyout/confirm` | `{order_id:int}` | 忽略 | **两步之二**，仅 SDK 成功后调；`order_id<=0` 静默跳过 confirm — :36 |
| 93 | POST | `/membership/huawei/verify` | `{product_id,purchase_token,purchase_order_id,subscription_id,jws_purchase_order}`（后三个默认 `''` 总发） | `{finish_purchase:bool}` | 服务端驱动：`true` ⇒ 客户端 `HuaweiIapBridge.finishPurchase`；`restoreHuaweiPurchases()` 会**循环**调用 — :51 |
| 94 | POST | `/membership/apple/verify` | `{product_id,transaction_id,original_transaction_id,receipt_data}` | `{complete_purchase:bool}` | `true` ⇒ `AppleIapBridge.completePurchase` — :71 |
| 95 | POST | `/payments/prepay` | — | — | **常量为死码**（`membership_payment_gateway.dart:17` 无任何调用点）。文档形状 `{channel,amount_fen,subject}`；真实 prepay 走 #91 的 `prepay_params` |

### 2.17 AI / Realtime / Push

| # | Verb | Path | 请求 | 响应 | 备注 |
|---|---|---|---|---|---|
| 96 | POST | `/sse/completions` | `{prompt:String, conversationId?:String, clientRequestId?:String}` — **camelCase**，与他处 snake_case 不同 | **SSE 帧，非信封**：`event:meta{requestId,conversationId,model,clientRequestId}`、`delta{text}`、`done{finishReason}`、`error{code,message}` | 绕开 `HttpManager.request`：`SseClient.post` → `dio.post` `ResponseType.stream`、`Accept: text/event-stream`、`receiveTimeout:0` — `commons/network/lib/sse/sse_client.dart:34`；消费方 features/ai 聊天 |
| 97 | POST | `/realtime/ws-ticket` | `{platform:'mobile'}` | **裸 body 无信封**：`{ticket,wsUrl,expiresInSeconds,connId}`（camelCase） | 转换器直接吃整份 JSON，套 `{code,data}` 会崩；`accessToken` 为空 → `StateError` — ws_ticket_api.dart:23；WS 关闭码 `4001` 认证 / `4002` 互踢 / `4003` ticket 过期 ⇒ 重新取票（realtime_config.dart:47-49）|
| 98 | POST | `/realtime/sync` | `{sinceSeq:int, topics:[String]}` | **裸 body**：`{events:[RealtimeEnvelope{id?,type,topic?,ts?,seq?,payload}], latestSeq:int}` | 重连后补投 — ws_sync_api.dart:12 |
| — | POST | `/realtime/push` | `{title,body,topic?,name?}` | `{envelope,delivered}` | **无 Dart 调用点**（仅文档 AGENTS.md:434 / docs/BACKEND_INTEGRATION.md:405），dev 推流测试用 |
| 99 | POST | `/push/devices` | `{registration_id, alias?, device_id?, platform:'harmony'\|'ios'\|'android'\|'unknown', mock:false}` | 原始 map 或 `{}`，结果丢弃 | **完全不解析 `ResultModel`**，`code` 不校验；全 `try/catch` + `LogUtils.w` — push_registration_api.dart:37 |

### 2.18 RongCloud IM — `components/rongcloud_im/lib/`（路径常量 `config/rong_im_config.dart:18-26`）

| # | Verb | Path | 请求 | `data` | 备注 |
|---|---|---|---|---|---|
| 100 | POST | `/im/session` | `{display_name?}`（空则省略），身份来自会话头 | `ImSessionResult{user_id\|im_user_id,token,expires_in_seconds}` | `RONG_APP_KEY` 空/`PLACEHOLDER` 或 `USE_MOCK_IM=true` 走 mock 分支；`token` 空 → 抛 — im_session_api.dart:72 |
| 101 | GET | `/im/users/profile` | query `user_ids`=**逗号拼接** | `{items:[{user_id\|im_user_id,display_name,avatar_url}]}` → `Map<imUserId, ImUserProfile>` | 空 id 列表直接不发 — im_user_profile_api.dart:33 |
| 102 | POST | `/im/messages/backup` | `{events:[{message_uid,direction:'in'\|'out'\|'recall',conversation_type:'private',target_id,message_type,payload,sent_at:UTC ISO-8601}]}` | 忽略 | **攒够 ≥5 才 flush**；失败/异常整批 `insertAll(0,…)` 回头重排 ⇒ at-least-once，服务端按 `message_uid` 去重 — http_im_backup_service.dart:77 |
| 103 | GET | `/im/friends` | — | `{items:[ImFriendUser{user_id,display_name,avatar_url,phone_masked}]}` | im_friend_api.dart:99 |
| 104 | GET | `/im/friends/requests` | — | `{items:[ImFriendRequest{id:String,from_user_id,display_name,avatar_url,phone_masked,status:'pending',created_at}]}` | 仅入向 — :120 |
| 105 | POST | `/im/friends/requests` | `{to_user_id:String}` | 忽略 | 英文错误转中文（`already pending`/`already friends`/…`ImFriendApi.friendlyError`）— :141 |
| 106 | POST | `/im/friends/requests/:requestId/respond` | `{accept:bool}` | 原始 Map | 同意/拒绝共用一端点 — :158 |
| 107 | GET | `/im/users/search` | `q` | `{items:[ImFriendUser]}` | :76 |
| 108 | GET | `/im/private/admission` | `peer_user_id` | `{allowed:bool}` | **fail-closed**：非 `true` 一律 false — :175 |
| 109 | POST | `/im/groups/free` | `{name,member_ids:[String]}` | `ImFreeGroupResult{group_id,name,owner_user_id}` | im_group_api.dart:65 |
| 110 | POST | `/im/groups/free/:groupId/invite` | `{member_ids:[String]}` | 原始 Map | `_postOk` — :113/:90 |
| 111 | POST | `/im/groups/free/:groupId/kick` | `{member_ids:[String]}` | 原始 Map | :113/:99 |
| 112 | POST | `/im/groups/free/:groupId/quit` | **`{}` 显式空对象** | 原始 Map | 不是 null body — :113/:105 |
| 113 | POST | `/im/groups/free/:groupId/dismiss` | `{}` | 原始 Map | :113/:109 |
| 114 | POST | `/im/groups/store/sync` | `{store_id:String}`（字符串） | `ImStoreGroupResult{store_id:String,group_id:String}` | 服务端幂等 provisioning — :43 |

## 3. 分页方言（KMP 侧必须归一，别在每个 feature 重写）

**A 型 `ListData`**：`data.list` **或** `data.items`（`result_model.dart:106`）+ 可选 `data.pagination{page,size,total,totalPages?}`。
`hasMore`：`totalPages>0 → page<totalPages`；否则 `size>0 → page*size<total`；否则 `list.length>=pageSize`（`:74-82,129-135`）。
请求侧 `page` 服务端 **1-based**（0-based 本地要 `+1`）。
命中端点：#26,27,31,36,38,41,45,52,54,59,61,66,67,68,69,73,14。

**B 型 `items[]` 无分页**（整表返回）：#15,16,19,20,21（且容忍裸数组）、24、57、79、103,104,107,101。
**C 型 `limit`/`offset`（page 0-based）**：#12 `/transactions`、#85 `/wallet/ledger`（后者只有 `items`，无 total）。
**裸 map 自解读**：#47 `/after-sales/records`（`{list,pagination{total},can_create}`）。

KMP 落点：`core/network/ListPage.kt`（`ListPage`/`ListPagination`/`decodeListPage`/`queryOf`/`pageQuery`/`limitOffsetQuery`）+ `ApiClient.getListPage<T>()`。

## 4. 业务错误码

`code`：0 成功；非 0 在 `BackendResponseParser._parseEnvelope` **抛** `HttpRequestException{message, code, statusCode, data}`。

| Code | 常量 / 出处 | 含义 | 客户端行为 |
|---|---|---|---|
| 0 | `ResultModel.isSuccess` | 成功 | 解析 `data` |
| 10001 | user_profile_api.dart:97 | 资料参数无效（头像 base64/mime/过大） | `UnknownAuthFailure` toast，保留会话 |
| 10002 | `AuthBizCode.unauthorized` | 账密错 | `InvalidCredentialsFailure`，留在登录页，不登出 |
| 10003 | `AuthBizCode.accountNotRegistered`（Go 复用 `CodeForbidden`） | 账号未注册 | → 去注册。`classifyAuthHttp` **故意不**把 `code==10003` 当 403（只有 HTTP 403 才是）— auth_http_decision.dart:26-28 |
| 10021 | sessionReplaced | 单设备互踢 | `forceLogout`：**先清本地会话**再弹「下线通知/账号已在其他设备登录」，`resetToLogin()`，`logout(remote:false)`（不打 `/user/logout`）— session_guard.dart:174-224 |
| 10022 | sessionInvalid | 会话无效 | 同上 `forceLogout`，文案「登录失效」 |
| 10023 | tokenInvalid | access token 过期/无效/无 `Authorization` | `tryRefresh`：单飞刷新 `/user/refresh` → 落库 → **重放原请求**；失败 → `handleRefreshExhausted` 清会话 + `SessionClearedFailure` |
| 10024 | invalidOtp | OTP 错/过期 | `InvalidOtpFailure`，原位重试 |
| 50000 | internalError | 服务端内部错 | 按 message 嗅探：Supabase 不可用/未配置 → `BackendServiceFailure`（提示 base URL），否则 `UnknownAuthFailure` |
| HTTP 401 | `NetworkCodes.expireToken` | 无业务码时也走 tryRefresh | 静默刷新+重放；耗尽则登出 |
| HTTP 403 | forbidden | 无权限 | **不登出、不刷新**，业务自行提示 |
| HTTP 429 | busy | 限流 | 无专门分支 |
| HTTP ≥500 | `RetryInterceptor._shouldRetry` | 服务端错 | 按 `maxRetries` 重试（仅 Mine/购车计算器 =3 ⇒ POST 可能重复） |
| -1 | `code` 缺失默认 | 畸形/非 BFF 体 | 判失败 |
| 缺 `code` + `error:String` | backend_response_parser.dart:17-27 | Supabase/PostgREST 裸错 | 抛，message 取 `error` |

## 5. 反直觉语义（移植时必须保留，否则行为不等价）

1. **会员两步支付**：`/membership/buyout` → `{status,order_id,prepay_params}`；`status=='paid'` 即完成，否则交 SDK，**SDK 成功后**才 `/buyout/confirm{order_id}`；`order_id<=0` 静默跳过 confirm。合并成一步会丢「已支付未确认」订单。
2. **商城两步下单**：`/mall/orders` 建未付单 → 详情 → `/mall/orders/:id/pay{payment_channel}`；`buy()` 是兼容包装。未付单带 `pay_deadline_at`，到期客户端自动 reload。
3. **幂等**：`/mall/orders` 的 `idempotency_key` 每次重新生成（`$skuId-<micros>`）⇒ 只能去重「同一请求重发」，**不能**去重客户端超时重试。同样 at-least-once 敏感的：`/points/check-in`、`/points/tasks/:code/claim`、`/short-videos/:id/view`、`/im/messages/backup`（后者整批回头重排，服务端必须按 `message_uid` 去重）。
4. **服务端下发地址**：`/realtime/ws-ticket` 返回的 `wsUrl` 必须原样拨号（禁止本地拼），还带 `connId`；WS 关闭码 4001/4002/4003 ⇒ 重新取票；重连后 `/realtime/sync{sinceSeq,topics}` 补投（camelCase 请求、裸 body 响应）。
5. **门店作用域**：`/me/stores` 前缀不同于 `/profiles/*`；服务端 `current_store_id` **赢过**本地持久化；`/profiles/me?store_id=` 会按经销商重算 `stats`；切店是 `POST /profiles/me/store` 且返回新 stats（不是布尔）。
6. `/payments/prepay` 是死常量；真实 prepay 藏在 #91 `prepay_params`。`/payments/{wechat,alipay}/notify` 只在服务端。
7. `POST /user/logout` 手工带 `Authorization`/`X-Session-ID`/`X-Device-ID` + `skipAuthRefresh`；token 已死时全局守卫**故意不调**它。
8. **同 path 不同查询方言**：`/transactions` 同时被 `limit/offset`（home，0-based）和 `page/size`（Mine 调试）调用；`/mall/stores/:id/products` 被两个 feature 用两种 item 模型消费（积分侧硬编码 `storeId=1` 且吞掉所有错误 → `[]`）。
9. **同端点靠 `type` 切形状**：`/community/search`。
10. **动词不对称**：like/unlike、follow/unfollow 是**同 path 切 POST/DELETE**；cancel/pay/default/claim/approve/reject/respond/view 是动作 POST 或 PATCH，从不 PUT。
11. **mock 开关**要一并移植（或留 flag）：`RongImConfig.useMockImFor`、`RealtimeConfig.useMockGateway`、`--dart-define=USE_MOCK_IM`。
12. **无 body 的动作 POST 必须真的发空 body**（不是省略请求），而 `/im/groups/free/:id/quit|dismiss` 显式发 `{}`。
13. **失败文案优先级**：Dart `_data()` / `_ensureOk()` 取 `message: (model?.message.isNotEmpty ?? false) ? model!.message : fallback`
    （`features/home/lib/home/api/home_todo_api.dart:134-151`）⇒ **服务端 message 非空时永远盖过调用点中文兜底**，
    包括 `code:0` 但 `data` 缺失的情况。所以 fixture 写 `"message":"success"` 却断言中文兜底是**测试错**，不是生产错；
    要测兜底就得把 `message` 留空/省略，同时保留一条断言锁住「服务端文案优先」这个行为。
14. **显式 null 有两套不同机制，别混为一谈**：`@Serializable` DTO 走 `NetworkJson`（`explicitNulls = false`）会**丢掉 null 键**，
    契约里要求发显式 null 的行（#39/#55/#60/#74）必须改用 `NetworkJsonExplicitNulls`；
    但手工 `buildJsonObject { put(key, value: String?) }` 写进去的是 `JsonNull` **元素**，不受 `explicitNulls` 影响，照常输出 `"key":null`
    （`new_car_follow` create 就是这一类，`display_name`/`phone` 为 null 时照样发键 ⇒ 别误判成 bug）。
    同一条 Dart 里 `vehicle_interest` 是 **trim 后非空**才入 body（`new_car_follow_api.dart:77-79`），空白串不发键。

## 6. 需要在 KMP `commonMain` 共享的模型（Dart 里跨模块复用的）

| 类 | Dart 出处 | 被谁复用 |
|---|---|---|
| `ResultModel<T>` / `ListData<T>` / `PaginationModel` / `PageResult<T>` | `commons/network/lib/api/result_model.dart` | 全模块（KMP 已由 `ListPage` 覆盖 A/B/C 型归一） |
| `HttpResult<T>` / `HttpRequestException` / `HttpManager` / `HttpClientConfig` / `HttpMethod` | `commons/network/lib/http/http.dart` | 所有 repository（携带 `code`/`statusCode` 供错误映射） |
| `AuthBizCode` + `AuthHttpDecision.classifyAuthHttp/isAuthHttpHardFailure` + `AuthFailure` 家族（SessionReplaced/SessionInvalid/SessionCleared/InvalidCredentials/InvalidOtp/AccountNotRegistered/WeakPassword/EmailConfirmationRequired/EmailAlreadyRegistered/BackendService/NetworkAuth/Unknown） | `commons/core/lib/model/auth/*.dart` | auth、home repo、realtime、session guard、所有失败路径 |
| `BackendUser` / `LoginResult` / `RefreshTokenResult` / `RegisterResult` | `features/auth/lib/api/user_auth_api.dart:348-482` | auth、UserService、wallet/pay 引导 |
| `UserProfile` / `UserStoreStats` / `UserStoreListResult` / `UserStoreListItem` / `UpdateUserProfileRequest` | `features/auth/lib/api/user_profile_models.dart` | auth hydrate、Mine、home 公司数据、视频主页头 |
| `ImUserProfile` | `commons/core/lib/model/im/im_user_profile.dart` | IM、聊天 UI、社区头像 |
| `RealtimeEnvelope` | `commons/core/lib/model/realtime/realtime_envelope.dart` | `/realtime/sync`、WS 入站、通知横幅、seq store |
| `TransactionModel`(+ `Transaction*Result` typedef) | `features/home/lib/home/model/transaction_model.dart` | home 收支列表+详情、Mine 调试页 |
| `TopicModel` / `CommentModel` | `features/community/lib/community/models/…` | 社区 feed、搜索分区、评论预览；短视频侧 `ShortVideoTopicBrief{id,name}` 是**故意重复**，KMP 只保留一份 |
| `PostModel`（含 `fromLikeFragment`） | `features/community/lib/community/models/post_model.dart` | feed、详情、搜索、点赞就地更新 |
| `MallOrderItem` | `features/mall/lib/mall/model/mall_order.dart:2` | 订单列表行 **和** 订单详情（两端点一模型） |
| `MallOrderDetail` / `MallOrderListRow` + `mallOrderStatusLabel` / `parseMallTime` / `formatPayCountdown` | 同上 `:39,98,179-209` | 列表、详情、倒计时、钱包 gated 支付 UI |
| `BankCard` | `features/wallet/lib/wallet/model/wallet_models.dart:25` | `GET /wallet` 的 `cards` **和** `POST /wallet/cards` 响应 |
| `AddressModel`(+`toBridgeJson`) | `features/settings/lib/mine/address/model/address_model.dart` | 地址 CRUD、商城结算收件人预填 |
| `MembershipPlan.fromCatalogJson` / `MembershipTier` / `PaymentMethodType` / `MembershipEntitlementView` | `features/pay/lib/membership/model/membership_models.dart` | 目录/权益、渠道字符串映射（`wechat/alipay/balance/''`） |
| `WsTicketResult` / `WsSyncResult` / `ImSessionResult` / `ImFriendUser` / `ImFriendRequest` / `ImFreeGroupResult` / `ImStoreGroupResult` | `components/realtime/lib/api/…`、`components/rongcloud_im/lib/api/…` | realtime + 聊天；**注意这些绕过 `ResultModel`（裸 body）** |
| `PointsGiftItem` vs `MallProductCardModel` | `features/home/lib/home/model/points_models.dart:137`、`features/mall/…/mall_product_model.dart` | 解的是**同一个** `/mall/stores/:id/products` 行 ⇒ KMP 合并成一个商品 DTO |

## 7. KMP 缺口分组（M1 派发依据）

| 组 | 覆盖行 | 数量 | KMP 落点目录 | 现有基础 |
|---|---|---|---|---|
| G1 auth+profile+门店 | #1–11 | 11 | `feature/auth/` | 已有 `AuthApiContracts.kt`/`AuthRepository.kt`，需比对增量（PATCH `/profiles/me`、`/me/stores`、`/profiles/me/store`、otp/wechat 可能缺） |
| G2 home 待办+积分 | #15–26 | 12 | `feature/home/` | 无 HTTP 契约（mock 引擎） |
| G3 transactions+wallet | #12–14, #84–89 | 9 | `feature/home/`+`feature/mine/` | 无 |
| G4 mall | #27–34 | 8 | `feature/commerce/` | 无 |
| G5 二手车+成交发票+购车计算器 | #35–39, #51–58 | 14 | `feature/mine/`（或新 `feature/deal/`） | 无 |
| G6 新车跟进+售后 | #40–50 | 11 | `feature/home/` 新子包 | 无 |
| G7 社区 | #59–71 | 13 | `feature/community/` | 有 mock 引擎 `MockCommunityEngine` |
| G8 短视频 | #72–78 | 7 | `feature/content/`(或 `video/`) | 有 mock |
| G9 地址 | #79–83 | 5 | `feature/mine/` | 无 |
| G10 会员/支付 | #90–95 | 6 | `feature/commerce/` 或新 `feature/pay/` | 无（IAP SDK 需 R5 gap-register） |
| G11 SSE+realtime+push | #96–99 | 4 | `feature/ai/`+`core/network` | SSE parser 由 M3 并行产出 |
| G12 RongCloud IM | #100–114 | 15 | `feature/chat/`+`feature/friend/` | 无（真 SDK 需 R5 gap-register） |

派发纪律：**一个 agent 只碰一个目录**，禁止跨组改 `feature/mine/`+`feature/home/` 之外共享文件；
`core/network/**` 只允许网络层 lane 改；`RouteHost`/分发文件在 V0-b 收口前禁止并发改。

构建纪律（本轮实测教训，写给 M2 之后的每个 agent）：

1. **KDoc 里不要出现 `/*` 序列**（例：`feature/**`、`core/*`、`/home/*`）。Kotlin 块注释可嵌套，
   这类写法会打开一个永不闭合的注释，整个文件解析失败，并沿着调用方级联成上百条
   `Unresolved reference`。本轮它在 4 个文件上复发，每次让 agent 白跑一轮构建。
   `scripts/verify-parity.sh` 已内置词法器静态拦截，先跑静态门再跑 gradle。
2. **不要并发抢 gradle**：一次只跑一个构建，统一入口 `bash scripts/verify-parity.sh --offline`
   （自带 mkdir 锁 + 单条 gradle 命令）；锁等待默认 3600s，可用 `GRADLE_LOCK_WAIT` 调。
3. **单 agent 行数上限约 12 行**（150 turn 会被撞死；本轮 3 个 agent 各拿 18–25 行都在
   收尾前耗尽 turn，代码落盘但从未验证）。超出就拆组，并让 agent 把自己那组编译+测试跑绿再收工。

### 7.1 补漏落地记录

- **#14 Mine 调试页 `GET /transactions?page=1&size=20`** → `feature/mine/httpdebug/`
  （`MineHttpTestApi.kt` + `HarmonyIndexModels.kt`，测试 `MineHttpTestApiTest` 8 例）。
  G3 派发时把它漏给了 home（只做 #12/#13）与 wallet（只做 #84–89）。
  该类**不看业务码**、只用 `pageQuery`，与 #12 的 `limitOffsetQuery` 双信封读法分开实现，
  以免跨 feature import `feature/home` 的 helper（分层门禁）。
  遗留：`MineRouteHost` 的 `BuyQaScreen`（路由 `/mine/http_test`）仍是硬编码 3 行 mock，
  接这个 API + 加载/失败态属 M10「原生分发 + 逐模块数据契约」，不在本行范围。

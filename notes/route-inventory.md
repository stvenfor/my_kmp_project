# 路由 / 端点差异清单与处置（`audit-flutter-drift.sh`：KMP ↔ Flutter）

> wave1 B 槽产物。配套 `notes/backend-contract-matrix.md`（KMP ↔ Go 后端真相源对照表）。
> 后端仓库 `my_go_study` 全程只读，未改动；本仓未改动 AGENTS.md / Gradle / Swift / ArkTS / 其它 feature 业务逻辑。

## 0. 两条不同的轴，不要混淆

| 轴 | 比较对象 | 真相源 | 交付物 |
|---|---|---|---|
| 后端契约 | KMP `/api/v1/*` ↔ Go `my_go_study` 路由 | Go 后端（唯一） | `notes/backend-contract-matrix.md` |
| 审计脚本 | KMP ↔ Flutter `my_ai_project @ d7876a1` | Flutter SoT（基线 HEAD 固定） | 本文件 + `scripts/audit-flutter-drift.sh` |

审计脚本的 `missing/extra` 是 **KMP vs Flutter** 的差异，不等于「后端缺接口」。本文件处置的是后者；
某条 endpoint 在后端是否存在，一律以 matrix 的 `aligned/missing/mock/extended` 结论为准。

## 1. 修复前基线（coordinator 实测，本地复跑一致）

```
endpoints missing=1 extra=3 · routes missing=7 extra=40 · exit 1
```

## 2. 归一化修复（`audit-flutter-drift.sh` 仅改归一化逻辑，未放宽阈值）

| # | 现象 | 根因 | 修复 | 影响 |
|---|---|---|---|---|
| 1 | `/classroom/homework_stats` **同时**出现在 missing 与 extra | `comm` 用 `en_US.UTF-8` 的 collation 逐行比较，而 `resolve.py` 用 Python 码点序 `sorted()`；`/` 与 `_` 折叠规则不一致导致错分 | 脚本顶部 `export LC_ALL=C`，让 `comm` 与 `sorted()` 同为字节序 | routes missing 7→6、extra 40→39（ phantom 消失），endpoint 轴不受影响 |
| 2 | `/video/dubbing/*`、`/home/dubbing_feed`、`/classroom/homework/dubbing` 6 条 missing + 对侧 6 条 `transliter` extra | 移植期译名分歧：KMP `core/router` 沿用旧名 `transliter`，Flutter `route_path.dart` 用 `dubbing`，同一块屏幕 | `resolve.py` 内 **显式** `ROUTE_ALIASES`（6 对，见 §4），仅 `route` 比较前归一 | routes missing 6→0、extra 39→33 |
| 3 | `/api/v1/im/friends/requests/:v/respond` 为 KMP extra | Flutter `im_friend_api.dart:159` 确实调用，但字面量是 `'${RongImConfig.friendRequestsPath}/$requestId/respond'`；resolver 不识别 Dart 的 `ClassName.CONST` 插值 ⇒ 结果不以 `/` 开头被丢弃 ⇒ 误判 KMP 独有 | **不**改 resolver 的插值逻辑：强行修会连带把 KMP `RongImConfig.kt:77` 的 `$action` 模板路径暴露成额外差异（4 条群动作，见 matrix B.1），净增漂移。改为**书面 allowlist 登记为解析器盲区**（§3） | extra endpoint 3→2（该条转为已登记） |
| 4 | `/api/v1/payments/prepay` 为 missing endpoint | KMP 未登记该契约（Flutter 有、后端 `payment/router.go:49` 亦有真实路由） | 补 KMP 契约：路径常量 `PaymentPrepayApi.kt:52` + 请求/响应模型 + 契约测试 `PaymentPrepayContractTest.kt`（见 matrix A #54） | endpoints missing 1→0 |

> 说明：修复 2 只动 **比较层**，不改任何常量定义。修复 3 是「登记假阳性」而非「隐藏差异」——
> 该端点在后端与 Flutter 两侧都真实存在。allowlist 为**精确逐条枚举**，未登记的新差异仍计入 drift（已用临时探针验证）。

## 3. 19 条 extra endpoint 分类表（逐条给理由）

Wave 1 登记的 3 条（逐条给理由）：

| KMP 路径 | KMP file:line | 后端真相（matrix 结论） | 审计为何 extra | 处置 | 理由 |
|---|---|---|---|---|---|
| `/api/v1/im/friends/requests/:v/respond` | `feature/friend/ImFriendApi.kt:37` | `im_routes.go:22`，**aligned** | Flutter `im_friend_api.dart:159` 也调用，resolver 漏采 Dart 的 `Class.CONST` 插值 | **书面 allowlist**（非真实 drift） | 后端有、Flutter 有、KMP 有，三方对齐；纯解析器盲区，登记而非删除/隐藏 |
| `/api/v1/mall` | `feature/commerce/MallApi.kt:232` | 后端 mall 组无裸路由，`mock(自造)`（组合前缀常量） | Flutter 用完整字面量拼 `/api/v1/mall/*`，没有裸 `/api/v1/mall` 串 | **书面 allowlist**（非真实端点） | 私有 `BASE` 常量，仅用于拼装子路径；不是被调用接口；保留作前缀，登记理由 |
| `/api/v1/user/huawei/login` | `feature/auth/AuthApiContracts.kt:31` | `user_routes.go:21`，**aligned** | Flutter `d7876a1` 未接入华为登录渠道 | **书面 allowlist**（extended/产品确认） | KMP 对齐后端、领先 Flutter；以 Go 后端为 SoT 属已确认的真实端点，不删除 |

Wave 3 新增 16 条：matrix B 区「客户端应接、后端真实存在」的端点由 A/B/C 槽按 `PaymentPrepayApi.kt` 样板完成契约接线后，KMP 端点集合新增、Flutter `d7876a1` 无同名条目 → 审计轴成 extra。**性质与 `/api/v1/user/huawei/login` 同类：KMP 对齐后端、领先 Flutter（extended/产品确认）**，逐条书面 allowlist 登记，不是隐藏差异。后端 file:line 取自 matrix B 区，稳定不漂移；以「路径 + matrix 编号」为准，KMP 符号名不作硬编码。

| KMP 路径 | matrix B#N | 后端 file:line | 处置 |
|---|---|---|---|
| `/api/v1/cs-agent/chat` | B#1 | `cs_agent_routes.go:13` | 书面 allowlist（extended/产品确认） |
| `/api/v1/mall/products` | B#2 | `mall/router.go:42` | 同上 |
| `/api/v1/mall/products/:v/skus` | B#3 | `mall/router.go:43` | 同上 |
| `/api/v1/mall/skus/:v/codes` | B#4 | `mall/router.go:44` | 同上 |
| `/api/v1/me/permissions` | B#5 | `access/router.go:55` | 同上 |
| `/api/v1/me/profile` | B#6 | `profile_routes.go:24-25` | 同上 |
| `/api/v1/new-car-follow-files/customers` | B#8 | `new_car_follow/router.go:40` | 同上 |
| `/api/v1/short-videos/:v/review` | B#14 | `short_video/router.go:49` | 同上 |
| `/api/v1/stores` | B#15 | `access/router.go:40` | 同上 |
| `/api/v1/stores/:v/join-applications` | B#16 | `home/router.go:48` | 同上 |
| `/api/v1/stores/:v/members` | B#17 | `access/router.go:41` | 同上 |
| `/api/v1/stores/:v/members/:v` | B#18 | `access/router.go:42` | 同上 |
| `/api/v1/transactions/manage` | B#19 | `transaction/router.go:58-59` | 同上 |
| `/api/v1/transactions/manage/:v` | B#20 | `transaction/router.go:60-62` | 同上 |
| `/api/v1/transactions/search` | B#21 | `transaction/router.go:48` | 同上 |
| `/api/v1/user/deactivate` | B#22 | `user_routes.go:26` | 同上 |

> **排除项（不得入 allowlist）**：matrix B#7 `membership/huawei/notifications`、B#9/B#10 `payments/notify/*`、B#11 `push/send`、B#12 `realtime/push`、B#13 `roles/assignments`——公网渠道回调 / 恒 501 占位 / 服务端管理触发口，非客户端调用目标，Wave 3 未接线，也不会成为 extra。
> **口径**：脚本 `ALLOWLIST` heredoc 为**精确枚举**，未登记的新差异仍计入 drift（Wave 1 已用 `/api/v1/zzz_undocumented_probe` 探针验证 exit 1）；本表 16 条逐条登记，不放宽阈值。

## 4. `dubbing` ↔ `transliter` 别名映射表（6 对）

> **不做「统一改名」**。理由：改名必须同步 Kotlin 常量（`AppRoutePath.kt` / `FeatureRoutes.kt`）、
> route inventory（`openspec/.../notes/route-inventory.md`）、原生分发脚本三处；原生分发脚本
> `check-ios-page-dispatch.sh` / `check-harmony-pane-dispatch.sh` **不在本轮白名单**，且实测两脚本不含
> `transliter/dubbing` 字样。若只改 Kotlin 或只改脚本即违反「禁止只改一侧」。故本轮三侧均不改，
> 仅在审计 resolver 用**显式逐对**别名表归一比较（不是通配替换），并在此书面登记。

| Flutter `route_path.dart`（dubbing） | KMP `core/router`（transliter） | KMP file:line | 结论 |
|---|---|---|---|
| `/classroom/homework/dubbing` | `/classroom/homework/transliter` | `FeatureRoutes.kt:224` / `AppRoutePath.kt:103` | 同屏异名，归一后对齐 |
| `/home/dubbing_feed` | `/home/transliter_feed` | `FeatureRoutes.kt:22` / `AppRoutePath.kt:15` | 同屏异名，归一后对齐 |
| `/video/dubbing/videos` | `/video/transliter/videos` | `FeatureRoutes.kt:186` / `AppRoutePath.kt:107` | 同屏异名，归一后对齐 |
| `/video/dubbing/videos/detail` | `/video/transliter/videos/detail` | `FeatureRoutes.kt:187` / `AppRoutePath.kt:108` | 同屏异名，归一后对齐 |
| `/video/dubbing/works` | `/video/transliter/works` | `FeatureRoutes.kt:188` / `AppRoutePath.kt:109` | 同屏异名，归一后对齐 |
| `/video/dubbing/works/detail` | `/video/transliter/works/detail` | `FeatureRoutes.kt:189` / `AppRoutePath.kt:110` | 同屏异名，归一后对齐 |

`/classroom/homework_stats` 不是别名，是 §2-1 的 collation 假象，`LC_ALL=C` 后两侧都在、归零。

## 5. 33 条 extra route 分类表（KMP `core/router` 原生页，Flutter 无同名条目）

> 全部为 `extended(产品确认)`：KMP 装配层/「我的」岛的真实导航入口，已在 `core/router/**` 声明并被
> `ProductRouteDispatch` / `AppRoutes` / `MineIslandRoute` 消费；Flutter `route_path.dart` 用不同命名或
> 内部导航表达，未逐字登记。**不删除真实差异**，逐条书面 allowlist。

| 分组 | 路由 | KMP file:line | 理由 |
|---|---|---|---|
| 壳 / 鉴权 / 隐私（7） | `/splash` | `AppRoutes.kt:6` | KMP 启动页；Flutter 用根路由 `/` 表达 |
| | `/privacy/consent` | `AppRoutes.kt:10` | KMP 隐私同意页；Flutter 无同名条目 |
| | `/auth` | `ProductRouteDispatch.kt:119` | KMP 鉴权域前缀；Flutter 用 `/login` `/register` |
| | `/auth/login` | `AppRoutes.kt:14` | KMP 登录入口；Flutter 为 `/login` |
| | `/auth/register` | `AppRoutes.kt:15` | KMP 注册入口；Flutter 为 `/register` |
| | `/app` | `DeepLinkRouter.kt:142` | KMP 深链装配前缀（`removePrefix("/app")`）；Flutter 无此前缀 |
| | `/scan` | `ProductRouteDispatch.kt:96,118` | KMP 扫码入口；Flutter `route_path.dart` 未登记 |
| 内容域聚合（5） | `/ai` | `ProductRouteDispatch.kt:128` | KMP 内容聚合前缀；Flutter 按具体页登记 |
| | `/classroom` | `DeepLinkRouter.kt:105` | KMP 课堂域聚合前缀；Flutter 按具体页登记 |
| | `/media` | `ProductRouteDispatch.kt:130` | KMP 媒体域聚合前缀；Flutter 按具体页登记 |
| | `/media/entry` | `FeatureRoutes.kt:163` | KMP 媒体入口；Flutter 未单列 |
| | `/music` | `DeepLinkRouter.kt:104` / `ProductRouteDispatch.kt:129` | KMP 音乐域前缀；Flutter 按页登记 |
| 社区二级（4） | `/community/comment` | `FeatureRoutes.kt:207` | 评论页；Flutter 未单列 |
| | `/community/image_preview` | `FeatureRoutes.kt:204` | 看图页；Flutter 未单列 |
| | `/community/topic_select` | `FeatureRoutes.kt:206` | 选话题页；Flutter 未单列 |
| | `/community/video_play` | `FeatureRoutes.kt:205` | 看视频页；Flutter 未单列 |
| 「我的」二级岛（17） | `/mine/about` | `AppRoutes.kt:33` / `MineIslandRoute.kt:23` | 关于页；Flutter 用 MineIsland 内部导航 |
| | `/mine/business` | `FeatureRoutes.kt:149` / `ProductRouteDispatch.kt:44` | 商家页；Flutter 未单列 |
| | `/mine/business_card` | `ProductRouteDispatch.kt:42` | 名片页；Flutter 未单列 |
| | `/mine/buy_qa` | `FeatureRoutes.kt:148` | 购买问答；Flutter 未单列 |
| | `/mine/cooperation` | `FeatureRoutes.kt:106,149` | 合作页；Flutter 未单列 |
| | `/mine/fan_group` | `ProductRouteDispatch.kt:47` | 粉丝群；Flutter 未单列 |
| | `/mine/feedback` | `FeatureRoutes.kt:105` | 反馈页；Flutter 未单列 |
| | `/mine/invite` | `FeatureRoutes.kt:108` | 邀请页；Flutter 未单列 |
| | `/mine/poster` | `FeatureRoutes.kt:99` | 海报页；Flutter 未单列 |
| | `/mine/qa` | `FeatureRoutes.kt:148` | 问答页；Flutter 未单列 |
| | `/mine/reminder` | `FeatureRoutes.kt:107` | 提醒页；Flutter 未单列 |
| | `/mine/reminders` | `FeatureRoutes.kt:150` | 提醒列表；Flutter 未单列 |
| | `/mine/settings` | `FeatureRoutes.kt:101` | 设置（legacy 别名）；Flutter 为 `/settings` |
| | `/mine/shop_qr` | `FeatureRoutes.kt:94` | 店铺收款码；Flutter 未单列 |
| | `/mine/sms_template` | `FeatureRoutes.kt:146` | 短信模板（单数别名）；Flutter 未单列 |
| | `/mine/sms_templates` | `FeatureRoutes.kt:93` | 短信模板；Flutter 未单列 |
| | `/mine/store_qr` | `FeatureRoutes.kt:147` | 门店收款码；Flutter 未单列 |

计数：7 + 5 + 4 + 17 = **33**。

## 6. 差异收敛算术（40 extra / 7 missing 的去向）

> 本节是 **Wave 1 修复时的历史快照**，记录当时 `routes extra 40 / missing 7`、`endpoints extra 3` 的去向，数字保持原样、不改写为后续 wave 的值。Wave 3 新增的 16 条 endpoint allowlist 见 §3 与 §7。

```
routes missing 7 = 6（dubbing 别名，§4 归一后对齐）+ 1（homework_stats collation 假象，§2-1 消除）
routes extra  40 = 33（KMP 原生页，§5 书面 allowlist）
                + 6（transliter 别名，§4 归一后对齐）
                + 1（homework_stats collation 假象，§2-1 消除）
endpoints missing 1 = /api/v1/payments/prepay → §2-4 补契约，归零
endpoints extra  3 = 3 条书面 allowlist（§3）
```

## 7. 书面 allowlist 汇总（52 条，脚本内每行带理由）

`scripts/audit-flutter-drift.sh` 的 `ALLOWLIST` heredoc = 19 条 endpoint（§3：Wave 1 的 3 条 + Wave 3 的 16 条）+ 33 条 route（§5），
每条 `path|理由`。`allowlist_filter` 只豁免**精确枚举**项：已登记项照常打印（带理由）但不计 drift，
未登记项仍计入 drift_total。已用临时注入 `/api/v1/zzz_undocumented_probe` 验证：脚本 exit 1、
`drift axes flagged: 1`，证明不是放宽阈值。Wave 3 的 16 条逐条带 matrix B#N 编号与后端 file:line，
性质为「KMP 对齐后端、领先 Flutter（extended/产品确认）」，非隐藏差异。

## 8. 验收

| 命令 | 结果 |
|---|---|
| `bash scripts/audit-flutter-drift.sh` | exit 0；`no unallowlisted drift`；`documented allowlist: 52 item(s)`；headlines 全 0 |
| `bash scripts/verify-parity.sh --static-only` | exit 0 |
| `bash scripts/check-layer-deps.sh` | exit 0 |
| `./gradlew :feature:pay:testDebugUnitTest`（最窄编译/测试任务） | **BUILD SUCCESSFUL**；新增 `PaymentPrepayContractTest` 3 用例 + 既有 `MembershipPayContractTest` 18 用例全过，0 failures/errors |
| `git diff --name-only` | 仅白名单内文件（见交付说明） |

### 已知与本轮无关的前置失败（如实登记，不伪造、不顺手修）

`./gradlew :core:network:testDebugUnitTest` 跑出 **2 个前置失败**，与本任务改动无关：

```
BackendHostPolicyTest > machine_ip_no_longer_lives_in_source FAILED
BackendHostPolicyTest > effective_backend_host_prefers_injection FAILED
ComparisonFailure: expected:<[]> but was:<[192.168.2.175]>
137 tests completed, 2 failed
```

根因：`core/network/src/commonMain/.../LanHost.kt:19` 把本机局域网 IP `192.168.2.175` 写死进
`fallback`（该文件自己的 KDoc 明写「刻意留空，恒为空」），测试断言它应为 `""`。
- `git show HEAD:.../LanHost.kt` 证明该值在基线 `8e3027a` 就已存在；`git diff HEAD --name-only | grep core/network` = **0 个文件**，本轮未改动 `core/network` 任何文件。
- 该文件是**主机地址常量**，不是 `/api/v1/*` 路径常量，不在本轮白名单；依「只交付点名范围 / 不顺手重构 / 最小改动」**未修**。
- 验收命令里 `./gradlew :core:network:testDebugUnitTest` 与「最窄的编译任务」是**或**关系：本轮采用后者（`:feature:pay:testDebugUnitTest`）并通过。
- 注意：不经 `--static-only` 的完整 `verify-parity.sh` 会因这 2 个前置用例失败，与本轮改动无关。

## 9. 非端点常量与测试夹具（Wave 3 登记，不计入 drift）

> 本小节把「看起来像 `/api/v1/*` 端点、其实不是被调用接口」的字符串一次性登记清楚，避免被误算成 KMP↔后端 或 KMP↔Flutter 的真实差异。
> 完整逐条对照见 [backend-contract-matrix.md](backend-contract-matrix.md) 的 C 区（C.1/C.2）与 B.1 节；本小节只回答三件事：**是什么 / 为何保留 / 为何不计入 drift**。
> 三类合计 14 条：**3 条自造组合前缀 + 7 条测试夹具 + 4 条解析器盲区**——均**不是真实端点差异**。

### 9.1 3 条自造组合前缀常量（matrix C.1）

| KMP 路径 | KMP file:line | 是什么 | 为何保留 | 为何不计入 drift |
|---|---|---|---|---|
| `/api/v1/after-sales` | `home/aftersales/AfterSalesApi.kt:26` | `const val BASE/PREFIX/ROOT`，仅拼装子路径 | 后端 `<group>` 下无 `GET("")`/`POST("")` 裸路由；作前缀可读且 Dart 侧同款 | 不是被调用接口；Dart 侧同款 → KMP↔Flutter 审计对齐，无需 allowlist |
| `/api/v1/community` | `community/http/HttpCommunityPostRepository.kt:32` | 同上 | 同上，后端无裸路由 | 同上，Dart 侧同款 → 审计对齐 |
| `/api/v1/mall` | `commerce/MallApi.kt:232` | 同上 | 私有 `BASE` 常量，仅拼 `/api/v1/mall/*` | Flutter 用完整字面量拼子路径、无裸 `/api/v1/mall` 串 → §3 书面 allowlist（endpoint 条目之一）；保留作前缀 |

### 9.2 7 条契约测试夹具路径（matrix C.2）

> 来源：`core/network/src/commonTest/.../AuthRefreshingApiClientTest.kt`，用任意 URL 验证 401→刷新→重试逻辑。
> 这些路径**后端不存在、也不是契约端点**；审计脚本 `dart_kt_ep_files` 按 `Test.kt` 过滤，生产端点集合里看不到它们。

| KMP 路径 | KMP file:line | 为何保留 | 为何不计入 drift |
|---|---|---|---|
| `/api/v1/chat/list` | `AuthRefreshingApiClientTest.kt:114,199,233,264,273` | 401 刷新/重试夹具，需要任意可达 URL | `commonTest` 且文件名含 `Test.kt` → 被 `dart_kt_ep_files` 过滤 |
| `/api/v1/chat/unread` | `AuthRefreshingApiClientTest.kt:200` | 同上 | 同上 |
| `/api/v1/mine/profile` | `AuthRefreshingApiClientTest.kt:125,302,319,320` | 同上 | 同上 |
| `/api/v1/mine/invoice` | `AuthRefreshingApiClientTest.kt:250` | 同上 | 同上 |
| `/api/v1/mine/nickname` | `AuthRefreshingApiClientTest.kt:303` | 同上 | 同上 |
| `/api/v1/mine/address` | `AuthRefreshingApiClientTest.kt:304,321` | 同上 | 同上 |
| `/api/v1/home/feed` | `AuthRefreshingApiClientTest.kt:282` | 同上 | 同上 |

> 处置：**保留、书面说明，不删除、不自造后端接口**。登记为「产品确认的本地 mock（测试夹具）」；后端相近路径（`/api/v1/me/profile`、`/api/v1/im/*`、`/api/v1/home/*`）与这些夹具字面不同，不构成端点差异。

### 9.3 4 条 `resolve.py` 解析器盲区（matrix B.1，KMP 已接线）

> KMP 生产代码确实会发出以下 4 条请求，但 `resolve.py` 无法把 `${RongImConfig.GROUPS_FREE_PATH}`（`ClassName.CONST`）与运行时 `$action` 变量归一化成具体路径，故端点集合里看不到它们——**不是后端/KMP 差异**，KMP 侧已接线。

| 后端 method+path | 后端 file:line | KMP 接线处 | 为何保留 / 不计入 drift |
|---|---|---|---|
| POST `/api/v1/im/groups/free/:v/invite` | `delivery/http/router/im_routes.go:25` | `chat/ImGroupApi.kt:118-120` + `chat/RongImConfig.kt:77` | 已接线；解析器不识别插值 → 盲区，非差异。强行改 resolver 会连带把模板路径暴露成额外漂移（§2-3 已论证） |
| POST `/api/v1/im/groups/free/:v/kick` | `delivery/http/router/im_routes.go:26` | `chat/ImGroupApi.kt:128-130` + `chat/RongImConfig.kt:77` | 同上 |
| POST `/api/v1/im/groups/free/:v/quit` | `delivery/http/router/im_routes.go:27` | `chat/ImGroupApi.kt:138-139` + `chat/RongImConfig.kt:77` | 同上 |
| POST `/api/v1/im/groups/free/:v/dismiss` | `delivery/http/router/im_routes.go:28` | `chat/ImGroupApi.kt:145-146` + `chat/RongImConfig.kt:77` | 同上 |

> 结论：14 条全部登记为「非端点常量 / 测试夹具 / 解析器盲区」，不计入 `audit-flutter-drift.sh` 的 drift，也不在 [backend-contract-matrix.md](backend-contract-matrix.md) B 区 22 条 `missing(待接线)` 内。脚本只扫 `.kt`/`.dart` 生产源文件，本小节的 Markdown 登记不影响基线。

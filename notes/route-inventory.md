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

## 3. 三条 extra endpoint 分类表（逐条给理由）

| KMP 路径 | KMP file:line | 后端真相（matrix 结论） | 审计为何 extra | 处置 | 理由 |
|---|---|---|---|---|---|
| `/api/v1/im/friends/requests/:v/respond` | `feature/friend/ImFriendApi.kt:37` | `im_routes.go:22`，**aligned** | Flutter `im_friend_api.dart:159` 也调用，resolver 漏采 Dart 的 `Class.CONST` 插值 | **书面 allowlist**（非真实 drift） | 后端有、Flutter 有、KMP 有，三方对齐；纯解析器盲区，登记而非删除/隐藏 |
| `/api/v1/mall` | `feature/commerce/MallApi.kt:232` | 后端 mall 组无裸路由，`mock(自造)`（组合前缀常量） | Flutter 用完整字面量拼 `/api/v1/mall/*`，没有裸 `/api/v1/mall` 串 | **书面 allowlist**（非真实端点） | 私有 `BASE` 常量，仅用于拼装子路径；不是被调用接口；保留作前缀，登记理由 |
| `/api/v1/user/huawei/login` | `feature/auth/AuthApiContracts.kt:31` | `user_routes.go:21`，**aligned** | Flutter `d7876a1` 未接入华为登录渠道 | **书面 allowlist**（extended/产品确认） | KMP 对齐后端、领先 Flutter；以 Go 后端为 SoT 属已确认的真实端点，不删除 |

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

```
routes missing 7 = 6（dubbing 别名，§4 归一后对齐）+ 1（homework_stats collation 假象，§2-1 消除）
routes extra  40 = 33（KMP 原生页，§5 书面 allowlist）
                + 6（transliter 别名，§4 归一后对齐）
                + 1（homework_stats collation 假象，§2-1 消除）
endpoints missing 1 = /api/v1/payments/prepay → §2-4 补契约，归零
endpoints extra  3 = 3 条书面 allowlist（§3）
```

## 7. 书面 allowlist 汇总（36 条，脚本内每行带理由）

`scripts/audit-flutter-drift.sh` 的 `ALLOWLIST` heredoc = 3 条 endpoint（§3）+ 33 条 route（§5），
每条 `path|理由`。`allowlist_filter` 只豁免**精确枚举**项：已登记项照常打印（带理由）但不计 drift，
未登记项仍计入 drift_total。已用临时注入 `/api/v1/zzz_undocumented_probe` 验证：脚本 exit 1、
`drift axes flagged: 1`，证明不是放宽阈值。

## 8. 验收

| 命令 | 结果 |
|---|---|
| `bash scripts/audit-flutter-drift.sh` | exit 0；`no unallowlisted drift`；`documented allowlist: 36 item(s)`；headlines 全 0 |
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

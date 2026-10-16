# my_ai_project → my_kmp_project 同步 v2 · 超细粒度任务清单

> SoT：`my_ai_project` @ `d7876a1`（工作树干净，**无新增 commit**，103 条路由与 KMP 常量表 1:1 对齐）
> 本轮依据：2026-10-16 双仓实测审计（Flutter 侧 905 Dart 文件 / 56 条 `/api/v1`；KMP 侧编译状态实测）
> 粒度：每项 ≤ 0.5–1 人日 · ID 规则 `{M}.{组}.{序号}` · 平台后缀 `.a/.i/.h`
> 用户本轮决策：**① 模块拆分回退 composeApp ② 新发现能力全部纳入 ③ 三端补数据契约 + 修分发**

## 与 v1（`tasks-ultra-fine.md`）的关系

v1 的 A–L 段仍然有效，但三类失真必须修正，v2 为**执行口径**：

| v1 结论 | 实测 | v2 处置 |
|---|---|---|
| W5.i / W6.h「17 子路由已闭环」 | 页面确已接线，但 iOS/ArkTS 各 65 页均为 15–43 行**静态元组**，无 store、无 API → 未满足 v1 自定的「可点路径 + 数据契约 + 三端」 | 全部降级 `[~]`，重开为 M10 |
| K.3「每路由证据目录规范」`[x]` | `notes/evidence/` 目录**不存在** | 重开为 M13.5 |
| R7「`check-layer-deps.sh` 必须通过」 | 实跑 **24 条 feature→feature 违规**，且 CI **从未调用**该脚本 | 重开为 M13.1/M13.2 |
| L.1/L.4 drift 即全部增量 | Flutter 另有 realtime WS / gRPC / SSE / IAP / 语音 / 防截屏 / 关系存储 / 3-flavor 环境 **完全未登记** | 新增 M2–M9 |

**架构硬规则沿用 v1 R1–R8**；R4（单 `:composeApp` + `libkn.so`）本轮由用户确认**继续生效**，故 M0 回退拆分。

---

# M0. 修红 · 恢复编译（阻断项，先于一切）

**背景（实测证据）**：`core/{navigation,platform,ui}` 三目录共 36 个 `.kt`（35 个为 composeApp → 新目录的 staged 移动 + 1 个未跟踪新文件）。三目录**无 `build.gradle.kts`**、**未进 `settings.gradle.kts`**、`composeApp/build.gradle.kts` 无对应依赖。
`./gradlew :composeApp:compileDebugKotlinAndroid` → **BUILD FAILED**，报 `Unresolved reference 'MineRoutes' / 'ProductRouteDispatch' / 'ProductRouteHost' / 'ReportMainTabRoot' / 'WebBridgeHost' / 'PlatformWebView' / 'PlatformBackHandler'` 等（`SecondaryRouteIsland.kt:49/57`、`SoftAuthPresenter.kt:3`、`InAppWebScreen.kt:13-56`）。
`composeApp/src/commonMain/.../core/{router,platform,ui}` 现为**空目录**。

- [ ] M0.1 `core/navigation` 8 文件 → `composeApp/src/commonMain/kotlin/com.example.my_kmp_project/core/router/`（`AppRoutePath` `AppRoutes` `AppNavigator` `RoutePath` `ProductRouteDispatch` `DeepLinkRouter` `DeepLinkEntry` `FeatureRoutes` 之外的 8 个）
- [ ] M0.2 `core/navigation/commonTest` 1 文件（`ProductRouteDispatchTest.kt`）→ `composeApp/src/commonTest/.../core/router/`
- [ ] M0.3 `core/platform`：commonMain 6 → `composeApp/.../commonMain/.../core/platform/`；androidMain 6 → `androidMain/.../core/platform/`；iosMain 5 → `iosMain/.../core/platform/`；ohosMain 5 → `ohosMain/.../core/platform/`
- [ ] M0.4 `core/ui`：commonMain 3 → `commonMain/.../core/ui/`；`PlatformNetworkImage.coil.kt` → `imageCoilMain/`；`PlatformNetworkImage.ohosX64.kt` → `ohosX64Main/`（**必须落在这两个自定义源集**，否则 Coil/OHOS 目标链接断裂）
- [ ] M0.5 `core/navigation/.../FeatureRoutes.kt`（未跟踪、与 feature host 内既有 `internal object HomeRoutes/MineRoutes/...` **重复声明**）→ 决策：删除或改为复用现有 internal 表；不得双份
- [ ] M0.6 删除空壳目录 `core/navigation` `core/platform` `core/ui`（含 staged 索引），清空目录残留
- [ ] M0.7 可见性回归：`ProductRouteDispatch.kt:13` 的 `internal enum ProductRouteHost` 在单模块下重新合法；确认 54 处 import 全部指向 `composeApp` 内定义
- [ ] M0.8 `./gradlew :composeApp:compileDebugKotlinAndroid` **绿**
- [ ] M0.9 五目标编译矩阵绿：`compileDebugKotlinAndroid` / `compileKotlinIosX64` / `compileKotlinIosArm64` / `compileKotlinOhosArm64` / `compileKotlinOhosX64`
- [ ] M0.10 `./gradlew :composeApp:testDebugUnitTest` 全绿（v1 记录基线 77 例）
- [ ] M0.11 `./gradlew :composeApp:linkDebugSharedOhosArm64`（或 `publishDebugBinariesToHarmonyApp`）确认 `libkn.so` 未被拆分污染
- [ ] M0.12 提交本轮 staged 积压（HEAD `b4d6501` 之上有 104 个未提交文件：W0 鉴权 + W5/W6 原生页 + M0 回退）——**按 M0 / W0 / W5-W6 三笔分开提交**，便于回滚

**DoD**：五目标 + 测试 + OHOS 链接全绿；`git status` 仅剩文档改动。

---

# M1. API 契约层（最大净增量 · 50 条端点）

**实测基线**：Flutter **56** 条 `/api/v1/*`；KMP **12** 条，其中仅 **6** 条与 Flutter 同名（全属 auth：`user/login|logout|register|refresh|phone/otp/send|verify`）。
→ **50 条 Flutter 端点 KMP 无对应**；**6 条 KMP 端点 Flutter 无对应**（违反 proposal Non-goal「不发明 Flutter 未用的后端 API」）。
KMP 真实发 HTTP 的仅 2 个文件（`AuthRepository.kt`、`AuthTokenRefresh.kt`）；其余 home/community/mall/wallet/classroom 全走 `HomeMockData` / `MockCommunityEngine` / `WalletMockStore`。

## M1.0 契约纠偏（6 条自造路径）

- [ ] M1.0.1 `/api/v1/mine/profile` → 对齐 Flutter `/api/v1/profiles/me`
- [ ] M1.0.2 `/api/v1/mine/invoice` → 对齐 Flutter `/api/v1/deal-invoices`
- [ ] M1.0.3 `/api/v1/chat/list` + `/api/v1/chat/unread` → 归入 Flutter IM 契约族（`/im/session`、`/im/messages/backup`），或明确为本地 mock 端口并注明不发 HTTP
- [ ] M1.0.4 `/api/v1/home/feed` → 拆为 Flutter 真实族（`/home/todo-cards` 等，见 M1.2）
- [ ] M1.0.5 `/api/v1/user/huawei/login` → Flutter 无此端点（仅 `/user/wechat/login`）：删除或登记为 KMP-OHOS 专有扩展并经产品确认
- [ ] M1.0.6 契约纠偏后跑 `AuthHttpDecisionTest` + 新增 envelope 回归

## M1.1 网络层能力补齐（其余 M1.x 的前置）

- [ ] M1.1.1 `NetworkFacade` 补 GET / PUT / PATCH / DELETE（当前仅 post 族被业务使用）
- [ ] M1.1.2 分页契约（`page/pageSize/total`）统一封装，对齐 Flutter `backend_response_parser.dart`
- [ ] M1.1.3 路径参数与查询串安全拼接（Flutter 有 `/mall/orders/$orderId` 形态 6 条）
- [ ] M1.1.4 业务码 → 领域错误映射表全量对齐 Flutter `NetworkCodes` + `AuthBizCode`（`10003 forbidden`、`10021/10022`）
- [ ] M1.1.5 离线/不可达 → 回退 mock 的统一开关（R6：保持同级 mock 可演示，不得白屏）
- [ ] M1.1.6 `commonTest`：M1.1.1–1.5 各 1 例 + 契约快照测试

## M1.2 Home / Todo（5 条）

- [ ] M1.2.1 `/api/v1/home/todo-cards` ↔ `HomeMockData` todo strip
- [ ] M1.2.2 `/api/v1/home/join-applications` ↔ `/home/todo/partner-pending`
- [ ] M1.2.3 `/api/v1/home/follow-up-customers` ↔ `/home/todo/follow-up-customers`
- [ ] M1.2.4 `/api/v1/home/after-sales-appointments` ↔ `/home/todo/after-sales-appointments`
- [ ] M1.2.5 `/api/v1/home/store-review-orders` ↔ `/home/todo/order-pending-review`
- [ ] M1.2.6 四子页列表 + 筛选 + 空态接真实契约（替换 B.13 mock）

## M1.3 车辆业务（4 条）

- [ ] M1.3.1 `/api/v1/used-car-orders` ↔ `/home/used_car` 列表/详情/创建三路由
- [ ] M1.3.2 `/api/v1/after-sales` ↔ `/home/after_sales` 列表/创建/详情
- [ ] M1.3.3 `/api/v1/new-car-follow-files` ↔ `/home/new_car_follow`（与 M12 成交 drift 交叉）
- [ ] M1.3.4 `/api/v1/me/stores` ↔ 店管归属数据源（M12.2 依赖）

## M1.4 Community / Mall（7 条）

- [ ] M1.4.1 `/api/v1/community` ↔ Feed latest/hot/following + 发布 + 搜索 + 评论
- [ ] M1.4.2 `/api/v1/mall/stores/{storeId}/products` ↔ 瀑布流列表
- [ ] M1.4.3 `/api/v1/mall/stores/{storeId}/products/{productId}` ↔ 商品详情
- [ ] M1.4.4 `/api/v1/mall/cart` ↔ 购物车
- [ ] M1.4.5 `/api/v1/mall/orders` ↔ 订单列表
- [ ] M1.4.6 `/api/v1/mall/orders/{orderId}` + `/cancel` + `/pay` ↔ 订单详情三动作（与 M5 支付交叉）
- [ ] M1.4.7 `/api/v1/short-videos` ↔ `/video/short` feed（与 M10 三端同源）

## M1.5 会员 / 支付 / 钱包 / 积分（11 条）

- [ ] M1.5.1 `/api/v1/membership/me` ↔ 会员态
- [ ] M1.5.2 `/api/v1/membership/buyout` + `/confirm` ↔ 购买主流程（两步确认）
- [ ] M1.5.3 `/api/v1/payments/prepay` ↔ 预支付单（微信/支付宝下单，真通道见 M5.1）
- [ ] M1.5.4 `/api/v1/membership/apple/verify` ↔ Apple IAP 票据校验（M5.2）
- [ ] M1.5.5 `/api/v1/membership/huawei/verify` ↔ Huawei IAP 校验（M5.3）
- [ ] M1.5.6 `/api/v1/wallet` ↔ 钱包首页
- [ ] M1.5.7 `/api/v1/transactions` ↔ 流水
- [ ] M1.5.8 `/api/v1/points/status` + `/check-in` + `/tasks` ↔ 积分三端点（打卡商城 B.6）
- [ ] M1.5.9 订单状态机（超时/取消/轮询）对齐 Flutter `pay` 模块（v1 G.4）

## M1.6 IM 契约族（9 条 · 与 M11 真融云并行）

- [ ] M1.6.1 `/api/v1/im/session` ↔ IM Token 签发（承接 v1 C.4.4 / L.2.5）
- [ ] M1.6.2 `/im/users/profile` + `/im/users/search`
- [ ] M1.6.3 `/im/friends` + `/im/friends/requests` ↔ `/friend` 通讯录（v1 J.2.14）
- [ ] M1.6.4 `/im/private/admission` ↔ 单聊准入
- [ ] M1.6.5 `/im/groups/free` + `/im/groups/store/sync` ↔ 自由群 + 门店群补拉（L.2.3）
- [ ] M1.6.6 `/im/messages/backup` ↔ 消息备份（`http_im_backup_service.dart` 对等）
- [ ] M1.6.7 `ChatPort.kt`（16 行占位、`StubChatPort.isReady=false`）→ 升级为真端口，替换 `IllegalStateException("Chat SDK not wired")`

## M1.7 用户资料 / 地址 / 其它（13 条）

- [ ] M1.7.1 `/api/v1/profiles/me` ↔ `/mine/profile` 读写
- [ ] M1.7.2 `/user/addresses` + `/{addressId}` + `/{addressId}/default` ↔ E.4 地址列表/编辑/默认（含省市区选择器）
- [ ] M1.7.3 `/purchase-calculator/products` + `/quote` ↔ E.5 购车计算器
- [ ] M1.7.4 `/api/v1/deal-invoices` ↔ E.8 发票（**E.8.1 产品 scope 仍待定**：`/settings/deal_invoice_demo` + `/upload`）
- [ ] M1.7.5 `/api/v1/push/devices` ↔ 设备 token 上报（v1 A.4.7/4.8）
- [ ] M1.7.6 `/api/v1/user/wechat/login` ↔ 微信登录（真 SDK 见 M5.6）

**DoD（M1 全段）**：56/56 端点在 KMP 有契约定义与调用点；`commonTest` 覆盖 envelope + 错误码；每条路径留 `notes/evidence/` 证据。

---

# M2. Realtime WebSocket 模块（Flutter 24 文件 / 2363 行 · v1 完全未登记）

SoT：`my_ai_project/components/realtime/lib/`（`websocket_transport.dart` + `mock_ws_transport.dart` + heartbeat + reconnect + seq-store + outbound-queue）；HTTP 侧 `/api/v1/realtime/ws-ticket`、`/realtime/sync`。
KMP 现状：`grep -i websocket` = **0 命中**；v1 仅 J.1.10「realtime 联调壳」。

- [ ] M2.1 `RealtimeSession` 领域模型 + 状态机（idle/connecting/ready/degraded/closed）
- [ ] M2.2 `RealtimeTransport` expect/actual；common 用 **Ktor WebSockets**（`networkKtorMain` 已有 Ktor 3.3.3-0.3.0 基座）
- [ ] M2.3 `ws-ticket` 换取 + 鉴权头复用 `auth_header_provider` 对等实现（依赖 M1.6.1/M1.7 会话）
- [ ] M2.4 心跳（interval/timeout/miss 计数）移植
- [ ] M2.5 重连退避（指数 + 上限 + 手动重试）移植
- [ ] M2.6 **seq store**（乱序丢弃 / 连续确认）移植 — 关键正确性件
- [ ] M2.7 **outbound queue**（断线暂存 + 重连回放 + 去重）移植
- [ ] M2.8 `/realtime/sync` 增量拉取补齐离线缺口
- [ ] M2.9 `MockRealtimeTransport` 对等 Flutter `mock_ws_transport`（R6 同级 mock，可注入剧本）
- [ ] M2.10 ohosArm64 CIO WebSocket 可用性实测；不可用则 `platform-gap-registry` 登记 `realtime transport: ready|ready|partial|missing`
- [ ] M2.11 消费方接线：`/live/room` 弹幕与在线人数、`/chat/detail` 收推（替换 `Index.ets:467-660` 本地 mock）
- [ ] M2.12 `commonTest`：seq/重连/队列各 ≥3 例（时钟注入）

---

# M3. SSE 流式（v1 J.4.10 标「真 SDK」，实为可移植纯逻辑）

SoT：`commons/network/lib/sse/{sse_client,sse_parser,sse_frame}.dart` + `/api/v1/sse/completions`；ADR-0005 明确 **SSE ≠ realtime IM**（两条通道不可合并）。
KMP 现状：`AiStreamScreen.kt:36-80` 单张静态 drawable + 双 tap 弹「mock」toast。

- [ ] M3.1 `SseFrame` / `SseParser` 纯逻辑移植（事件边界、多行 `data:`、`\r\n` 归一、心跳注释行）+ ≥8 例 `commonTest`
- [ ] M3.2 Ktor 流式响应读取（`HttpSend` + 分块字节流 → parser）接入 `/api/v1/sse/completions`
- [ ] M3.3 `AiStreamRepository` 落地，替换 `AiStreamScreen` mock chunk；多轮上下文 + 停止生成 + 错误气泡
- [ ] M3.4 ohosArm64（CIO）流式可用性实测；不达标则登记 gap 并保留 mock 回退
- [ ] M3.5 ADR-0005 边界写入 KMP 文档：AI 走 SSE、聊天走 WS/IM，不得互相复用传输

---

# M4. gRPC 数据分析（Flutter 真用，KMP 平台受限 → 契约对齐 + gap 登记）

SoT：`commons/network/lib/grpc/analytics_grpc_api.dart` + `grpc/generated/analytics/v1/*.pb.dart`，BFF `:9090`；消费方 `features/home/lib/home/repository/analytics_repository.dart`。
KMP 现状：仅 1 处提及 grpc；`/home/data_analytics` 走 Vico 图表 + mock。

- [ ] M4.1 清点 Flutter analytics 请求/响应字段与指标口径（转化环、漏斗、对比柱、雷达 0–100）
- [ ] M4.2 KMP `data_analytics` 列表/详情**数据契约**对齐 M4.1（HTTP 回退或 mock 同级）
- [ ] M4.3 三端 gRPC 可用性评估（Kotlin multiplatform gRPC / connect-kotlin；OHOS 无产物）
- [ ] M4.4 `platform-gap-registry.md` 新增 `analytics grpc` 行（android/ios/ohos + blocker + follow_up），**不伪称 ready**
- [ ] M4.5 图表组件收敛（承接 v1 B.12.4）：散落图表统一到 `feature/content` 图表出口，对齐 Flutter `wys_chart`（ADR-0001 fl_chart）独立包模式

---

# M5. 支付 / IAP / 微信（v1 只登记了微信/支付宝，IAP 完全遗漏）

SoT：`features/pay/lib/membership/payment/{apple_iap_bridge,huawei_iap_bridge}.dart` ↔ MethodChannel `wys.membership/apple_iap`、`wys.membership/huawei_iap` ↔ `android/.../MainActivity.kt` + `ohos/entry/src/main/ets/plugins/HuaweiIapPlugin.ets`。
KMP 现状：`PayGateway.kt` 三端 adapter 全 null + `SandboxPayChannelAdapter` 假 `Success(sandbox=true)`（**与 v1 G.1「无假 Success」自相矛盾，需复核**）。

- [ ] M5.1 微信 / 支付宝真通道（OpenSDK/tobias）；未接入前 UI 严格保持不可用态
- [ ] M5.2 Apple IAP：`iosMain` StoreKit actual + `membership/apple/verify` 票据上行
- [ ] M5.3 Huawei IAP：`ohosMain` + `harmonyApp` 侧 IAP 插件（对齐 Flutter `HuaweiIapPlugin.ets`）+ `membership/huawei/verify`
- [ ] M5.4 Android 侧 IAP 边界（Flutter 走原生 MethodChannel）→ KMP `androidMain` actual 或登记 gap
- [ ] M5.5 **复核 sandbox**：`SandboxPayChannelAdapter` 返回 `Success` 是否违反 v1 G.1/R5；若违反，改为「沙箱明示 + 不写订单成功态」
- [ ] M5.6 微信登录三端（v1 A.3.15 仍 `[ ]`）：`fluwx` 对等能力 + `/user/wechat/login`；Flutter `appId=''` 未配置 → 同级 mock + gap 保持 `missing`
- [ ] M5.7 JPush/FCM/OHOS Push 真 SDK（v1 A.4.8、K.6）；Flutter 侧亦为 **placeholder key**（`DEV/PROD_JPUSH_APP_KEY_PLACEHOLDER`）→ 同级 mock + gap `stub`

---

# M6. Chat 语音（v1 C.2.8 误标「n/a」，Flutter 为真实现）

SoT：`features/chat/lib/chat/services/voice_recorder.dart`（`record: ^6.0.0`，AAC/M4A）+ `voice_player.dart`；commit `0fbfe18` 加固。

- [ ] M6.1 `VoiceRecorder` expect/actual（Android `MediaRecorder` AAC；iOS `AVAudioRecorder`）
- [ ] M6.2 `VoicePlayer` actual（复用 `MediaPlayer` 基座，Android 已有 123 行实现）
- [ ] M6.3 录音权限 UX（复用 `PermissionGate.kt`）
- [ ] M6.4 Chat `InputPanel` 语音入口 + 按住说话 + 时长/波形 + 失败重试（替换 v1 C.2.8）
- [ ] M6.5 语音消息气泡：时长、未播红点、播放进度
- [ ] M6.6 OHOS 录音可用性实测（`avplayer.def` cinterop 已有，录音需新 def）；不达标登记 gap
- [ ] M6.7 v1 `C.2.8` 状态由 `[~] n/a` 改为 `[ ]`（口径纠偏）

---

# M7. 屏幕防截取（Flutter 有，v1 完全未登记）

SoT：`commons/wys_common/lib/src/wys_screen_protector.dart` ↔ MethodChannel `flutter/sensitivecontent`。

- [ ] M7.1 清点 Flutter 生效范围（哪些页面/全局、开关键盘遮挡策略）
- [ ] M7.2 `PlatformScreenGuard` expect + Android `FLAG_SECURE` actual
- [ ] M7.3 iOS actual（截屏/录屏监听 + 遮罩）
- [ ] M7.4 OHOS actual（window private flag）或登记 gap
- [ ] M7.5 与 Compose 岛/原生壳的挂载点约定（避免每页重复声明）

---

# M8. 存储层（v1 KMP `core:storage` 仅 1 个占位文件）

SoT：`commons/storage/lib/{prefs/sp_manager,db/app_database(sqflite)}`。

- [ ] M8.1 `sp_manager` 语义清点（键空间、类型、迁移）
- [ ] M8.2 KMP KV 对齐：现有 `multiplatform-settings`（`accountSettingsMain`）扩至全量键；补类型化 API
- [ ] M8.3 `app_database` 表结构清点（Flutter sqflite 建表 + 用途）
- [ ] M8.4 关系存储方案决策：SQLDelight（三端产物，OHOS 需验证）/ KV 降级 / 登记 gap — **先出评估文档再动手**
- [ ] M8.5 **OHOS 持久化出 `/tmp`**（v1 A.2.4 长期遗留：隐私同意 + 会话重启即丢）→ 落沙箱持久目录
- [ ] M8.6 隐私/会话持久化回归测试（三端重启后仍为已同意/已登录）

---

# M9. 环境系统 3-flavor（承接 v1 L.4.2 / L.4.3）

SoT：`commons/core/lib/env/{app_env.dart(env_config/app_auth_config)}`，`enum AppEnv { test, staging, production }`；宏 `APP_ENV` / `TF_NET_TEST` / `TF_NET_PRODUCT` / `USE_MOCK_AUTH` / `USE_MOCK_IM` / `BACKEND_HOST` / `BACKEND_GRPC_PORT`；文件 `.env` `.env.example` `.env.lan(.example)`（LAN IP 不入库，commit `10654a9`）；入口 `features/settings/lib/env/`（2 文件）。

- [ ] M9.1 `AppEnv` 三态 + `EnvConfig` 对等（含每态 host/port/mock 开关）
- [ ] M9.2 Gradle 侧参数注入对等（`-PappEnv=` / `local.properties`，**不新增 `BuildConfig` 依赖**）
- [ ] M9.3 `DemoApiHosts` 与 Flutter 规则统一（承接 L.4.3）
- [ ] M9.4 `.env*` 约定：`.gitignore` 校验 + 机器 IP 不入库（承接 L.1.4 已绿的 `resolveLoopbackHostMapping`/`replaceLocalhostHost`/`LanHost.fallback=""`）
- [ ] M9.5 设置内环境切换入口（对齐 Flutter `settings/env`）+ 切换后重启语义
- [ ] M9.6 `USE_MOCK_IM` gating 对等（Flutter `rong_im_config.dart`：无 key / `PLACEHOLDER` / 显式开关 → 回落 mock）
- [ ] M9.7 `BackendHostPolicyTest` 扩测三 flavor × loopback/LAN 矩阵

---

# M10. 三端原生壳 · 补数据契约 + 修分发（用户决策 ③）

**实测**：`iosApp` 8447 行（`NativeSecondaryPages.swift` 4797 行 / **65 个 `struct Native…Page`**，全部被 `NativeFeatureCatalog.swift` 实例化）；`harmonyApp` pages 8595 行（`NativeSecondaryPages.ets` 4949 行 / **65 个 `struct Native…Pane`**，与 iOS 名字 1:1）。
**缺陷**：① 每页 15–43 行硬编码元组，无 store/API；② Harmony `NativeGiftClaimPane` / `NativeHomeworkDetailPane` / `NativeHomeworkStatsPane` **定义了但从未分发**；③ iOS `genericList` 兜底仍在 `NativeFeatureCatalog.swift:628-632` 存活；④ ArkTS 在 `Index.ets:480-660` **重复实现** chat mock，未调 KMP 引擎。

## M10.A 分发缺陷（先修，成本低）

- [ ] M10.A.1.h Harmony 三个未分发 classroom pane 接入 `nativePaneIdFor()` + `Index.ets` 渲染分支
- [ ] M10.A.1.h 全量扫描 `Native…Pane` 定义数 == 分发数，写一条脚本断言（防回归）
- [ ] M10.A.2.i iOS 枚举仍落入 `genericList` 的路由（`/bfui/*`、`/settings/im_debug`、`/video/short/*` 变体），逐个补专属页或显式登记 out-of-scope
- [ ] M10.A.3 Kotlin `ClassroomRouteHost.kt:100-107` 硬编码文案（`"批改进度 12/30 · 平均分 86"`）→ 接 `ClassroomLogicModels`
- [ ] M10.A.4 复核 `DeferredDestinationStub.kt:36-52` / `MineRouteHost.kt:354`(`/pay`) / `MusicScreens.kt:139`(「播放中（Stub）」) 是否与 Flutter toast-only 行为一致；不一致的降级登记

## M10.B 数据契约（按模块分批，每批 = 同一路由族三端同源）

- [ ] M10.B.1 定义跨语言出口约定：Kotlin `featurePageFor` 数据 → Swift / ArkTS 渲染（字段表 + 序列化格式），文档化到 `notes/`
- [ ] M10.B.2 Home 族（28 路由）：列表/详情/创建页接 M1.2–M1.3 契约
- [ ] M10.B.3 Mall 族（4）：接 M1.4.2–1.4.6，含 `$orderId` 参数化
- [ ] M10.B.4 Pay/Wallet/Membership 族（3）：接 M1.5
- [ ] M10.B.5 Video 族（9）：接 M1.4.7 + M2/M3
- [ ] M10.B.6 Classroom 族（8）：接 M10.A.3 + 真数据源（v1 I.0.3）
- [ ] M10.B.7 Community 族（4）：接 M1.4.1
- [ ] M10.B.8 Chat/Friend/Live/Music 族（7）：接 M1.6 + M2 + M6
- [ ] M10.B.9 Mine 二级（Compose 岛，R2）：确认岛内已共享、原生侧无重复实现
- [ ] M10.B.10.h ArkTS 移除本地 chat mock，改调 KMP `ImEngine`（消除双份真相）
- [ ] M10.B.11 v1 `W5.i.*` / `W6.h.*` 状态由 `[x]` 改为 `[~]`，M10.B 对应批次完成后统一回 `[x]`

---

# M11. 真融云 IM（承接 v1 L.2 / C.4.3；最大单块）

SoT：`components/rongcloud_im/`（16 文件 / 1545 行）+ `rongcloud_im_wrapper_plugin: ^5.44.0`；`rong_engine_holder.dart`、`rong_sdk_mapper.dart`；ADR-0012（**IMLib 而非 IMKit；超级群/聊天室不在范围**）；`backup/{mock,http}_im_backup_service.dart`。

- [ ] M11.1 三端 SDK 可用性评估（Android/iOS 官方；**OHOS 融云产物需确认**）→ `platform-gap-registry` 先登记再动手
- [ ] M11.2 依赖引入策略：KMP 侧不做 GetX/Plugin 字面 API，经 `ChatPort`（M1.6.7）适配
- [ ] M11.3 `RongImEngine` 实现 `ImEngine` 接口（保留 `MockImEngine` 作 M9.6 gating 回退）
- [ ] M11.4 消息模型映射（对齐 `rong_sdk_mapper.dart`）+ 已读回执 + 离线补拉
- [ ] M11.5 门店群补拉 + 自由群 UI（对齐 `90c7b46`，接 M1.6.5）
- [ ] M11.6 消息备份（接 M1.6.6）
- [ ] M11.7 IM Token 链路端到端（Auth session → `im/session` → SDK connect）
- [ ] M11.8 ADR-0012 范围声明写入 `openspec/specs/feature-chat`
- [ ] M11.9 三端验收（`.a/.i/.h`）+ 证据

---

# M12. 新车跟进成交 UI（承接 v1 L.3，P1 业务工作流）

SoT：Flutter commit `fc99691`（HEAD 前）；`features/home/lib/new_car_follow/`（12 文件）。

- [ ] M12.1 成交态 UI（状态徽标/字段）对齐
- [ ] M12.2 跟进流水时间线（列表 + 时间轴）
- [ ] M12.3 店管归属展示字段（数据源 M1.3.4 `/me/stores`）
- [ ] M12.4 创建页字段/校验与 Flutter 对齐（接 M1.3.3）
- [ ] M12.5 `.a` Android Compose 岛/原生壳 + `.i` SwiftUI + `.h` ArkTS 三端同批
- [ ] M12.6 契约快照测试

---

# M13. 工程门禁 / 证据 / 测试债（含 R7 实测失败）

**实测**：`bash scripts/check-layer-deps.sh` → **Found 24 feature→feature import(s)**（`ContentRouteHost` 引 ai/classroom/friend/live/media ×8、`MineRouteHost` 引 classroom/commerce/media、`HomeScreen` 引 content/mine ×2、`SoftAuthPresenter` 引 auth ×2、`FriendDirectory` 引 chat ×2 等）。脚本仅扫 `composeApp/src/commonMain/.../feature`，**CI（`.github/workflows/ci.yml:70`）从未调用**，故违规静默累积。
测试：**18 个 `*Test.kt`**；pay gateway / push bridge / media player / deep link / AI stream / web / scan / 任何 Compose UI / `core:account` **零测试**。

- [ ] M13.1 `check-layer-deps.sh` 扩扫全部源集与 `core/*`；输出机器可读摘要
- [ ] M13.2 24 条违规清零：改为 Navigator + `component/*` / `core/*` 间接层（**与 M0 回退同批做，避免二次搬家**）
- [ ] M13.3 CI 增列 `bash scripts/check-layer-deps.sh` + `parity_task_audit.py` 两步
- [ ] M13.4 `platform-gap-registry.md` 补登 6 行：`analytics grpc` / `realtime ws` / `sse` / `apple/huawei iap` / `voice record` / `screen guard` / `relational storage`
- [ ] M13.5 建立 `notes/evidence/{module}/{Platform}/{route}.md` 实际目录（v1 K.3 勾 `[x]` 但目录不存在）
- [ ] M13.6 `parity_task_audit.py` 支持 v2 清单路径（当前硬编码 `parity-flutter-to-kmp/tasks-execution.md`）
- [ ] M13.7 无测试域补测：pay / push / media / deeplink / AI / web / scan / `core:account`
- [ ] M13.8 v1→v2 状态迁移：69 条 `[ ]` 逐条映射到 M 段，避免两份清单并存失真
- [ ] M13.9 K.7 设计令牌终检（`core/design/DemoColors.kt` vs Flutter `AppTheme`，含 ADR-0004 移动字号阶）
- [ ] M13.10 K.9 恢复 Android 截图 diff 硬门禁（`scripts/screenshot_diff_gate.py`，mse ≤ 2.0）
- [ ] M13.11 更新 `AGENTS.md` 地图：M0 回退后 `core/` 实际模块清单（`core:base|analytics|storage` 为 0 引用占位，需决定保留或摘除）

---

# M14. 双仓流程（承接 v1 L.5）

- [ ] M14.1 KMP 侧「双仓编码流程入口」对齐 Flutter `docs/coding-playbook-dual-repo.md`
- [ ] M14.2 Flutter **12 条 ADR** 摘要入库（至少 0003 SSE/token API、0005 SSE≠IM、0006 auth session owner、0007 shell-only login gate、0008 cold-start+server logout、0009 `wys_account` 废弃、0012 IMLib 非 IMKit）——避免移植已废弃件
- [ ] M14.3 巡检规则：Flutter `HEAD` 前进时自动 diff 路由表 + `/api/v1` 集合（脚本化，接 M13.6）

---

# P. 架构前置（用户决策 2026-10-16：feature 与 core 同级、feature 互不依赖、core 补持久化）

M 段是「内容对齐」，P 段是「骨架对齐」：不先做 P，feature 拆出去的模块拿不到 `core.*`，只能反向依赖 `:composeApp`，R7 门禁立刻红。

| ID | 范围 | 状态 | 出口证据 |
|---|---|---|---|
| P1 | 共享 KMP 接线：根 `build.gradle.kts` 的 `subprojects {}` 统一 target 集 / android 基础配置 / commonMain 公共依赖（**buildSrc 在本环境不可用**：`org.gradle.kotlin.kotlin-dsl` 在镜像里解析失败） | ✅ 完成 | 10 模块 × 5 目标编译通过 |
| P2 | `core.*` 下沉为真模块：`base`/`analytics`/`storage`/`router`/`network`/`account`/`design`(+171 资源) / `platform` / `ui` | 🚧 进行中 | 资源归属 `:core:design`（`publicResClass=true`，包名 `my_kmp_project.core.design.generated.resources`，OHOS 走 `harmonyResourceRoots` 投递） |
| P3 | `core:persistence`：把 KV 收成一个模块（multiplatform-settings，android/ios/ohosArm64；ohosX64 走文件回落），修掉 OHOS `/tmp` 重启丢数据（M8.5）；`:core:account` + `core.platform.StringListStore` 都改为消费它 | ⬜ 待做 | session/隐私同意/收藏列表读写三端同源测试 |
| P4 | 17 个 `feature/` 目录（123 文件）按叶子优先拆 `:feature:*`：scan/web/content/live/classroom → media/community/friend/chat → pay/wallet/commerce/auth → mine/home | ⬜ 待做 | 每模块独立 `testDebugUnitTest`；R7 门禁含模块级检查 |
| P5 | `:composeApp` 收窄为 app + shell + RouteHost 分发；`libkn.so`/framework 产出者仍只有 `:composeApp`；R4 口径已改写 | ⬜ 待做 | 门禁 + 三端可跑 |

**已踩平的两个「拆了就红」坑（后续批次直接照抄结论）**：

1. **`internal` 跨不过模块边界**。`core.design`（`DemoColors`/`MineTopBar`/`ImmersiveInsets`…8 个）与整个 `core.platform`、`core.ui` 原先全是 `internal`——在同模块内合法，拆出去后 29+29 个消费文件立刻 `Cannot access … it is internal`。下沉一个包之前先把它对外暴露的符号改 public（expect/actual 必须两侧一起改）。feature 拆分会是同一形态。
2. **门禁必须按「全模块」写死不存在**。`scripts/verify-parity.sh` 的任务名不带冒号（`testDebugUnitTest`、`compileKotlinOhosArm64`…）＝ 在每个有该任务的子工程执行；JUnit XML 统计走全仓 `**/build/test-results/**`。写死 `:composeApp:` 会让新模块的编译与单测静默漏检——这就是之前「绿灯但其实没跑」的根因。

---

# 执行顺序与波次

| Wave | 内容 | 出口门禁 |
|---|---|---|
| **V0（阻断）** | M0 全段 + M13.2 违规清零 | 五目标编译 + 测试 + OHOS 链接绿；layer gate 0 违规 |
| **V1（最大净增量）** | M1.0–M1.1 契约纠偏 + 网络层能力 | 6 条自造路径归零；GET/PUT/DELETE + 分页 + 错误码可用 |
| **V2** | M1.2–M1.7 按模块落 50 端点 + M9 环境 | 每模块契约测试 + 证据 |
| **V3** | M2 realtime WS + M3 SSE | seq/重连/队列/解析器测试绿；OHOS 实测或登记 gap |
| **V4** | M10.A 分发缺陷 + M10.B 三端数据契约 | 定义数==分发数；无 `genericList` 误吞 |
| **V5** | M5 支付/IAP + M6 语音 + M7 防截屏 + M8 存储 | 假 Success 复核结论 + gap 登记 |
| **V6** | M11 真融云 + M12 成交 drift + M4 gRPC | 依赖外部 SDK/凭据，允许 gap 收尾 |
| **V7** | M13 门禁收口 + M14 流程 + K.9 像素门禁恢复 | 全量对齐报告 |

**外部依赖告警（无法在本环境落地，需你提供或确认）**：融云 AppKey（`String.fromEnvironment('RONG_APP_KEY')`，Flutter 默认空 → mock）、微信 `appId`、JPush key、Go BFF 可达地址（`:8080` HTTP / `:9090` gRPC）、DevEco/Xcode 整包构建。这些一律走 M13.4 gap 登记，不伪称 ready（R5）。

**建议起手**：V0 单独一轮（回退 + 清违规 + 编译矩阵绿），拿到绿色基线后再进 V1。

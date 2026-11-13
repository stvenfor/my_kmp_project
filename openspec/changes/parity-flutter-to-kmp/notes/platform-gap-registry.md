# Platform gap registry

状态词（仅允许以下五个）：`Migrated` | `Degraded` | `Placeholder` | `Deferred` | `Partial`

Rule (2A): `Migrated` 仅当 Android / iOS / OHOS 三端都落地能力**且数据契约对齐**并有链接证据；否则一律 `Partial` / `Placeholder` / `Degraded` / `Deferred`。**页面可开 ≠ 数据契约已接线 ≠ `Migrated`**：路由能打开静态页不等于能力完成。未完成目标必须保留 blocker + follow_up，禁止判绿。

> **本次纠偏（2026-11-08，按代码证据）**：OHOS 持久化已走 `OH_Preferences`（不再是 `/tmp`）；token refresh 已落（三端共享 KMP 机制）；页面可开 ≠ 数据契约接线（iOS/OHOS Native 页为静态元组、无 store/API）。重复的两条 `push entry` 行已合并为一条。唯一执行账本见 `sync-flutter-full-parity/tasks-ultra-fine-v2.md`，证据登记见 `notes/evidence/index.md`。
>
> **Wave 1–3 收尾（2026-11-08）**：以 `my_go_study` 后端为唯一真相源，已完成 Wave 1（`0841c94`：账本门禁假绿修复、契约漂移清零、`X-Session-ID`/`X-Device-ID` 逐请求头注入、LanHost 回归修复）与 Wave 2（`8ce00f1`：AI 真 SSE 接线、社区切 HTTP、realtime 生产装配）。下表对应行已按代码证据更新；**接线 ≠ `Migrated`**：真机/后端联调、三端像素与链接证据仍缺，按 Rule 2A 保持 `Partial` / `Degraded` / `Deferred`。剩余需外部前置（厂商 SDK、商户凭据、真机证据、平台 klib）的能力见文末「收尾」节，逐项登记 blocker 与 follow_up，不伪称完成。

**Harness note (not a capability):** Android Screenshot Diff Gate runner lives at `scripts/screenshot_diff_gate.py` (ADR 0003 / issue #2). This documents that the pixel-gate tooling exists; it does **not** mark pay / WeChat / RongCloud / OHOS camera (or any other SDK gap) as `Migrated`.

| capability | android | ios | ohos | blocker | flutter_ref | kmp_ref | follow_up |
|---|---|---|---|---|---|---|---|
| design tokens vs Flutter | Partial | Partial | Partial | Tokens aligned to AppTheme hex; pixel evidence pending | commons/ui/lib/theme/app_theme.dart | core/design/DemoColors.kt | phase 2.4–2.5 evidence |
| privacy persistence | Migrated | Migrated | Migrated | 三端 KV 持久化已落：Android/iOS 走 multiplatform-settings（`settingsMain`），OHOS 走 `OH_Preferences` NDK 沙箱持久目录（M8.5，旧 `/tmp` 文件 actual 已移除）；会话 + 隐私同意重启可存活 | components/linking privacy | core/account/PrivacyConsentStore + core/storage/KeyValueStore.ohos.kt + AppShell | 真机重启回归待补（本轮未执行） |
| remote auth / OTP | Partial | Partial | Partial | Login OK via Go→Supabase when user exists; **public register** hits Supabase `over_email_send_rate_limit` (mapper now returns clear 400 copy). Android 3.5 happy-path evidenced with admin-created user。【Wave 3 已逐字段对齐】登录/刷新 DTO snake_case、`Pagination{page,size,total,totalPages}` 1-based（matrix F.1 #6–#7） | features/auth UserAuthApi | feature/auth AuthRepository + DemoApiHosts→127.0.0.1:8080 (emu `10.0.2.2`) | raise Supabase email quota / disable confirm email for DEV; then 3.5 iOS/OHOS |
| push entry | Placeholder | Deferred | Deferred | 厂商推送未接线：仅 Android 在隐私授权后挂 StubPushBridge（占位）；iOS/OHOS 无 JPush/FCM 等价实现（原重复行 task 5.4 已并入本行）。【Wave 3 已逐字段对齐】push devices 五键 `registration_id/alias/device_id/platform/mock`（matrix F.1 #5）；厂商 token 仍 `Deferred` | wys_push | component/push + NativeAndroidApp | 按平台接入厂商 SDK；补 PushBridge expect/actual（v2 追踪） |
| token refresh interceptor | Migrated | Migrated | Migrated | 已落（W0，2026-10-15）：`core/network` 装饰器 `AuthRefreshingApiClient` 独占 401 流；`feature/auth` 经 `AuthSessionBootstrap.install()` 注入 `classifyAuthHttp` 策略 + 单飞刷新器 `AuthTokenRefresh`；刷新请求走 `NetworkFacade.rawApi()`（未装饰 client，对等 Flutter `skipAuthRefresh`）防递归。三端共享同一 KMP 机制 | auth_token_refresh_interceptor | core/network/TokenRefresh.kt + NetworkFacade + feature/auth/AuthTokenRefresh.kt + AuthSessionBootstrap.kt | 已闭环；仅需随 W0 合入更新本登记 |
| WeChat login | Deferred | Deferred | Deferred | no fluwx / WeChat OpenSDK on any target | wys_login_share_pay | — | integrate WeChat SDK per platform or keep Deferred |
| home secondaries | Partial | Partial | Partial | 页面可开 ≠ 数据契约接线：iOS/OHOS `NativeFeatureCatalog` 二级页为静态元组（无 store/API）；Android Compose 二级较完整但像素精修仍可迭代 | features/home | feature/home + NativeFeatureCatalog | pixel polish vs Flutter；补数据契约 |
| chat UI | Partial | Partial | Partial | list/detail + InputPanel; Flutter MockIm parity seed/send/read | features/chat | feature/chat | pixel 6.6; Rong SDK later |
| IM engine | Partial | Partial | Partial | MockImEngine Flutter-aligned (seed 3 peers, 280ms send, 2s peer-read, markRead) on CMP+Android+iOS+OHOS | components/rongcloud_im | feature/chat/ImEngine.kt + native stores | vendor SDK later |
| community feed | Partial | Partial | Partial | 已切真实 HTTP：`CommunityScreen` / `CommunityRouteHost` 经 `HttpCommunityPostRepository` 取 feed/点赞/评论/搜索/关注；`MockCommunityEngine` 保留供既有单测与 native shell。仅 Android 单测证据（35 tests / 0 failures），iOS/OHOS 编译与真机 HTTP 往返未验 | features/community | feature/community/http/HttpCommunityPostRepository.kt + CommunityScreen.kt + CommunityRouteHost.kt | 真机联调 + 三端编译证据；各分支回归 |
| community publish | Partial | Partial | Partial | createPost 已走 `HttpCommunityPostRepository`；`image_picker` 适配器仍缺，Flutter publish 亦「开发中」 | features/community | feature/community/http/HttpCommunityPostRepository.kt | image_picker adapter + 真机往返 |
| media playback | Partial | Partial | Placeholder | Android MediaPlayer；iOS AVPlayer audio；OHOS 仅占位；video Surface TBD | features/video, music | feature/media createMediaPlayer | OHOS MediaKit；video surface |
| WeChat/Alipay pay | Placeholder | Placeholder | Placeholder | adapters null; flags false; UI shows unavailable (no fake Success)。【Wave 3 已逐字段对齐】prepay 五键 `channel/amount_fen/subject/ref_type/ref_id` → `PrepayResult{channel,out_trade_no,params}`（matrix F.1 #4 / A #54）；真 SDK 仍 `Deferred` | features/pay, wys_login_share_pay | feature/commerce FlaggedPayGateway | real OpenSDK per target then 8.5 sandbox |
| WebView + JS bridge | Partial | Partial | Placeholder | Android offline fixture「离线网页」evidenced (`web_open.png`); iOS WKWebView; OHOS 仅占位实现 | commons/ui kit/web | PlatformWebView + OfflineWebFixtureUrl | iOS/OHOS 5.5; OHOS real Web |
| deeplink delivery | Partial | Partial | Partial | Android/iOS cold-start + OHOS `want.uri`→`AppStorage`→`SetOhosSecondaryRoute` evidenced (`bridges/OHOS/deeplink_friend.kmp.png`)；仅路由可达，业务数据契约另评 | components/linking deeplink | DeepLinkRouter + shells | keep regression on EntryAbility onNewWant |
| scan/camera | Partial | Partial | Deferred | Android deny UX evidenced (`scan_perm_denied.png`); iOS permission path; OHOS no camera | toolkit scan | PlatformCameraScan + ScanScreen | OHOS camera N-API; iOS 5.5 accept |
| friend list | Partial | Partial | Partial | FriendDirectory ↔ ImEngineStore (mock_peer_01…03 + ensureChat); no relation/IM vendor SDK | features/friend | feature/friend FriendDirectory | vendor SDK later |
| live | Partial | Partial | Partial | Android list→room mock evidenced; no realtime/push stream | features/live | feature/live | realtime SDK / 9.4 |
| classroom | Partial | Partial | Partial | Logic models from Flutter ClassroomMockData + one-shot gift claim; UI still SoT chrome for some routes; no realtime classroom | features/classroom | ClassroomLogicModels + ClassroomRouteHost | deepen student/timeline; realtime SDK later |
| short video | Partial | Partial | Partial | 三端 short list UI evidenced；Surface/decode TBD | features/video | feature/media VideoRouteHost | real player surfaces |
| AI stream | Partial | Partial | Deferred | `AiStreamScreen` 已接真实 `AiSseApi`：Meta/Delta/Done/Error 折叠进 UI 状态，含加载中/错误/停止/完成四态，两处 toast mock 已删，新增 reducer + 4 条单测；ohosX64 流式不支持（既有 GAP R5），三端真机 SSE e2e 未验 | features/ai | feature/ai/AiStreamScreen.kt + AiStreamUiStateTest.kt + AiSseApi.kt | 真机/后端凭据下 e2e；ohosX64 保持 GAP(R5) |
| music | Partial | Partial | Placeholder | Android+iOS list/now_playing；OHOS 仅占位播放器 | features/music | feature/media MusicListScreen | OHOS MediaKit |
| image_picker | Deferred | Deferred | Deferred | no adapter | image_picker plugin | — | with publish/mine |
| DoKit / bfui / BLE / invoice demos | Deferred | Deferred | Deferred | non-goal | features/bfui, bluetooth, settings debug | — | optional later |
| face verify | Deferred | Deferred | Deferred | non-goal unless product adds | wys_face_verify | — | optional later |
| in-scope RoutePath UI (78) | Partial | Placeholder | Placeholder | **页面可开 ≠ 数据契约已接线 ≠ `Migrated`**：主路由在三端可打开，但 iOS SwiftUI `NativeSecondaryPages.swift` / OHOS ArkTS `NativeFeaturePane` 65 页均为 15–43 行静态元组（硬编码 `@State`、无 store/API）；二级详情/创建子路由仍落 `genericList`/`default` 占位（v2 W5.i/W6.h、M10） | my_ai_project RoutePath | NativeFeatureCatalog + MineIsland | 厂商 SDK / 真播放器 / OHOS WebView 另列；子路由补数据契约见 v2 M10 |
| realtime WS 长连（换票/退避重连/序号去重） | Partial | Partial | Deferred | 核心已落（`core/network/realtime` + `component/realtime` 状态机/启动闸门/序号存储）；**生产构造点已落**：`composeApp/.../app/RealtimeRuntime.kt` 经 `ShellBootstrap.install()` 接入三端入口，惰性构造 Ktor transport + `RealtimeStartupGate`，隐私/登录/登出回调绑定 `AppShell`，首连非阻塞（后台 scope，12s/8s 超时只记日志）。`onEnvChanged` / `onAppLifecycle` 触发源在 feature/mine 与平台 source set（白名单外），登记书面 GAP 未接；三端 e2e 与 iOS/OHOS 链接未验；ohosX64 无 CPF Ktor/WebSockets klib → 类型化桩异常。【Wave 3 已逐字段对齐】换票/补投裸 body camelCase `{ticket,wsUrl,expiresInSeconds,connId}` / `{events,latestSeq}`、WS `RealtimeEnvelope{id,type,topic,ts,seq,payload}` 与 `sys.notify` / `presence.bulk`（matrix F.1 #1–#2）；**字段对齐 ≠ `Migrated`**（真机 e2e 仍缺） | components/realtime（`realtime_initializer.dart`） | core/network/realtime + component/realtime + composeApp/app/RealtimeRuntime.kt | 服务端联调 + env/lifecycle 运行期转发 + ohosArm64 真机 e2e；ohosX64 保持 GAP(R5) |
| connectivity 监听 | Deferred | Deferred | Deferred | 本仓库无跨端 connectivity 等价插件，realtime 闸门只能「连不上就退避」，拿不到网络恢复事件 | Dart `connectivity_plus`（components/realtime 依赖） | component/realtime/AppRealtimeClient.kt（`// GAP(R5)`） | expect/actual connectivity 或各端原生注册 |
| SSE 流式传输 | Partial | Partial | Deferred | 传输层已落（`ApiClient : SseTransport` + `core/network/Sse.kt` + `feature/ai/AiSseApi.kt`，Ktor/android/ios/ohosArm64 增量解码）；**UI 已接**：`AiStreamScreen` 直接消费 `AiSseApi` 增量流（见 AI stream 行）；三端真机证据缺；ohosX64 `libnet_http` cinterop 只在完成回调给整包 body → 抛 `SseStreamingUnsupportedException`。【Wave 3 已逐字段对齐】`event: meta/delta/done/error` 与 `CompletionRequest` camelCase（matrix F.1 #3） | commons/network `sse_client.dart`、features/ai | core/network/Sse.kt + ApiClient.kt + feature/ai/AiSseApi.kt / AiStreamScreen.kt | 三端真机证据；ohosX64 保持 GAP(R5) |
| RongCloud IM 厂商 SDK | Deferred | Deferred | Deferred | 契约表 #100–114 的 **HTTP 侧已落**（会话/群/好友/资料/备份）；融云 SDK 动作（connect、join/dismiss group、在线态、语音事件）无对等实现 → `// GAP(R5)`；`USE_MOCK_IM` 开关也未对等 | components/rongcloud_im | feature/chat/{ImSessionApi,ImGroupApi,ImMessageBackupApi,ImUserProfileApi}.kt、feature/friend/ImFriendApi.kt | 厂商 SDK；mock 回落门见 M9.6 |
| analytics gRPC | Deferred | Deferred | Deferred | Flutter 真用 gRPC（BFF `:9090`）；KMP 无 gRPC/connect-kotlin 产物，**OHOS 平台无产物**；`/home/data_analytics` 现为 Vico 图表 + mock | commons/network/lib/grpc/analytics_grpc_api.dart + grpc/generated/analytics/v1 | — | M4.1–M4.4：字段口径对齐 + 三端可用性评估，不伪称完成 |
| Apple / Huawei / Android IAP | Deferred | Deferred | Deferred | 验单 HTTP 契约已落（`membership/apple|huawei/verify`）；`finishPurchase` 桥接本体（StoreKit / HuaweiIapPlugin.ets / Play Billing）缺 → 只回传标志位 | features/pay/lib/membership/payment/{apple,huawei}_iap_bridge.dart、ohos/entry/src/main/ets/plugins/HuaweiIapPlugin.ets | feature/pay/PayApi.kt（5 处 `// GAP(R5)`） | M5.2–M5.4 |
| WeChat / Alipay 真 SDK 支付 | Deferred | Deferred | Deferred | `PayApi.sdkPay` 只是交接点（吃布尔结果），OpenSDK/tobias 通道本体缺；UI 保持不可用态 | features/pay、wys_login_share_pay | feature/commerce/FlaggedPayGateway + feature/pay/PayApi.kt | M5.1；同步复核 `SandboxPayChannelAdapter` 假 Success（M5.5） |
| WeChat 登录授权码 | Deferred | Deferred | Deferred | `/user/wechat/login` HTTP 契约已落；授权码需原生 SDK（iOS WXApi / Android WXEntryActivity）；Flutter 侧 `appId=''` 亦未配置 | features/auth + fluwx | feature/auth/UserAuthHttpApi.kt（`// GAP(R5)`） | M5.6，保持 Deferred |
| `X-Session-ID` / `X-Device-ID` 逐请求头 | Migrated | Migrated | Migrated | 已落（Wave 1，`0841c94`）：`DemoRequestHeaders.forUrl` 注入两头，`NetworkConfig.sessionId/deviceId` 经 `NetworkFacade.bindAccessToken` 与 `AccountFacade` 会话三件套绑定，`clearAccessToken` 同步清空；`UserAuthHttpApi` 的 `GAP(R5)` 已移除，4 条回归测试 + network 套件全绿。三端共享同一 commonMain 机制（同 token refresh 行判定）。【Wave 3 已逐字段对齐】会话三件套 + `AuthBizCode` 10021–10024 单飞 401 刷新（matrix F.1 #8–#9） | commons/network/lib/http/auth_header_provider.dart:20-32 | core/network/DemoRequestHeaders.kt + NetworkConfig.kt + NetworkFacade.kt + DemoRequestHeadersTest.kt | 真机单设备登录校验（`DeviceSessionUsecase`）e2e 待补 |
| Chat 语音录制 / 播放 | Deferred | Deferred | Deferred | Flutter 真实现（`record: ^6.0.0`，AAC/M4A + voice_player）；v1 C.2.8 曾误标 out-of-scope | features/chat/lib/chat/services/{voice_recorder,voice_player}.dart | — | M6（OHOS 录音需新 cinterop def，M6.6） |
| 屏幕防截取 | Deferred | Deferred | Deferred | Flutter 走 MethodChannel `flutter/sensitivecontent`；KMP 无任何 actual | commons/wys_common/lib/src/wys_screen_protector.dart | — | M7.1–M7.5 |
| 关系型存储 | Degraded | Degraded | Degraded | Flutter 用 sqflite `app_database`；KMP 已落 `core:database`（`Database` / `Table` / `Entity` 接口 + `InMemoryDatabase` + `DatabaseProvider` 注入缝）。默认内存 driver 是**有意设计**而非遗留缺陷：OHOS fork 无 Room/SQLDelight klib，强引 SQL 依赖会挂 ohosArm64/ohosX64（见 `core/database/build.gradle.kts` 注释）；进程重启不持久 | commons/storage/lib/db/app_database.dart | core/database/{Database,InMemoryDatabase,DatabaseProvider}.kt | 待平台 SQL klib 就位后经 `DatabaseProvider.setFactory` 注入，上层零改动；补真机重启回归 |
| 3-flavor 环境系统 | Deferred | Deferred | Deferred | Flutter `AppEnv{test,staging,production}` + `APP_ENV`/`TF_NET_*`/`USE_MOCK_*`/`BACKEND_HOST`/`BACKEND_GRPC_PORT` 宏与 `.env*`；KMP 只有 `DemoApiHosts` + `LanHost` 单环境，无切换入口 | commons/core/lib/env/app_env.dart、features/settings/lib/env/ | core/network/DemoApiHosts.kt + LanHost.kt | M9.1–M9.7 |
| OHOS 持久化目录 | Migrated | Migrated | Migrated | 已落 `core.storage.KeyValueStore` 唯一 KV 契约，OHOS 双 arch 直接实现在 `OH_Preferences` NDK API（`libohpreferences`，`core/storage/.../KeyValueStore.ohos.kt`），写 app 沙箱数据目录、重启不丢；旧 `/tmp` 文件 actual 已移除（M8.5）；会话 / 隐私同意 / `RealtimeSeqStore` 同源 | commons/storage prefs/sp_manager | core/storage/KeyValueStore.ohos.kt + core/account/PrivacyConsentStore + component/realtime/RealtimeSeqStore | 真机重启回归（本轮未执行） |

## 收尾（2026-11-08）：Wave 1–3 交付边界

本节是**交付边界说明**，不是执行账本；唯一执行账本仍是 `sync-flutter-full-parity/tasks-ultra-fine-v2.md`（162 条 `[ ]` 为全量 parity 迁移 backlog，本轮不刻意清空）。

### 已完成（均已验收合并）

| wave | commit | 内容 |
|---|---|---|
| 1 | `0841c94` | 账本门禁假绿修复（审计默认读 v2、真实剩余 >0 即 FAIL）；drift 清零（36 条书面 allowlist + 探针验证）；`X-Session-ID` / `X-Device-ID` 逐请求头注入与会话三件套绑定；`LanHost.fallback` 回归修复；历史账本作废标注；证据索引 |
| 2 | `8ce00f1` | AI 真 SSE 接线；社区切 `HttpCommunityPostRepository`；realtime 生产装配（`RealtimeRuntime` + `ShellBootstrap.install()` + 隐私/登录/登出回调绑定） |

两次提交均在 main，**未 push**。合并后 `verify-parity.sh --static-only` / `check-layer-deps.sh` / `audit-flutter-drift.sh` 全绿；`:core:network` / `:feature:pay` / `:feature:ai` / `:feature:community` / `:component` / `:composeApp` 六个 `testDebugUnitTest` 均 BUILD SUCCESSFUL；`parity_task_audit.py` 按预期 exit 1（162 条真实未完成）。

### 剩余：需外部前置，本轮登记 gap、不伪实现

| 项 | 现状（代码证据） | blocker | follow_up |
|---|---|---|---|
| 厂商 Push（JPush/FCM/HPush） | `PlatformPushSdk.fetchRegistrationToken()` 返回 null，`PushRegistrationApi.kt:24` 留 `GAP(R5)`；`push entry` 行 Placeholder/Deferred | 厂商 SDK 与 appKey/证书 | 按平台接 SDK 后经 `PushBridge` 注入真实 token |
| 微信/支付宝真 SDK 支付、Apple/Huawei/Android IAP | `FlaggedPayGateway` / `PayApi` 交接点只吃布尔，UI 保持不可用态；`PayApi` 5 处 `GAP(R5)` | 商户号、OpenSDK、IAP 凭据与沙箱 | 凭据到位后接通道并跑 8.5 沙箱；勿伪造 Success |
| 融云 IM 厂商 SDK | HTTP 契约 #100–114 已落，`ImSessionApi` 等留 `GAP(R5)`，IM 仍 MockImEngine | AppKey 与 SDK 动作 | 接 SDK；mock 回落门 M9.6 |
| WeChat 登录授权码 | `/user/wechat/login` HTTP 契约已落，授权码需原生 SDK | WXApi / WXEntryActivity、`appId` | M5.6 |
| OHOS ArkWeb / 相机 / MediaKit | `PlatformWebView.ohos.kt`、`PlatformCameraScan.ohos.kt`、`PlatformOpen.ohos.kt` 明确为 `Degraded` stub（KDoc「Keep contract; fail acceptance honestly」） | ArkWeb / NDK cinterop + 真机 | 平台能力就位后替换 actual，不做伪实现 |
| 三端 65×2 原生页数据契约 | `NativeSecondaryPages.swift` / `NativeFeatureCatalog.swift` / ArkTS `NativeFeaturePane` 为 15–43 行静态元组（硬编码 `@State`、无 store/API）；`in-scope RoutePath UI (78)` 行 Partial/Placeholder | 跨端数据契约设计 + 三端真机 | v2 W5.i / W6.h / M10 |
| 截图 diff 硬门禁 | runner `scripts/screenshot_diff_gate.py` + seam 测试 `test_screenshot_diff_gate.py` 齐备，但**未接进** `verify-parity.sh`；`notes/evidence/` 缺成对 Flutter/KMP 真机截图 | 真机像素证据（P2） | 证据到位后接入静态门禁链；当前不接空壳门禁，避免假绿或误阻塞 CI |
| `core:database` 落盘 | `DatabaseProvider` 默认 `InMemoryDatabase`，`core/database/build.gradle.kts` 注释明确 OHOS 无 Room/SQLDelight klib | 平台 SQL klib | 经 `DatabaseProvider.setFactory` 注入，上层零改动 |
| analytics gRPC / 3-flavor 环境 / 语音录制 / 屏幕防截取 / DoKit·BLE·invoice | 表中 `Deferred`，非本轮范围 | 依赖产物 / 宏 / 插件 | 见各行 follow_up（M4/M6/M7/M9） |

### 未验证（不伪称完成）

真机/后端凭据下的端到端 SSE、社区 HTTP 往返、单设备登录校验、realtime 三端链接与 `onEnvChanged` / `onAppLifecycle` 运行期转发、iOS/OHOS 三端编译与像素验收、真实 T+1。这些需设备、厂商凭据或外部账号，本轮只能登记 gap。

## Wave 3 结论同步（2026-11-11，纯双方对齐）

> 本节同步 Wave 3 D 槽结论：以 `my_go_study` 后端为唯一真相源（**全程只读**），对 KMP ↔ 后端**纯双方**契约做逐字段复核。全文状态词仍只用 `Migrated | Degraded | Placeholder | Deferred | Partial`；按 Rule (2A)，本节登记项一律 `Deferred` / `Partial`、保留 blocker + follow_up，**不判绿**（「已对齐」仅指字段/协议层一致，不等于能力 `Migrated`）。
> **范围排除（外部前置，非本轮范围）**：极光/JPush、微信/支付宝 OpenSDK、Apple/Huawei/Google IAP、融云 IM、Supabase、短信/OTP 服务商、LLM provider、对象存储、第三方渠道回调与恒 501 占位、Prometheus；不列为「需对齐」。
> 逐字段结论与 file:line 证据见 `notes/backend-contract-matrix.md` § F；结构性 gap 与后端占位语义边界见 `notes/backend-alignment-gaps.md`；65×2 原生页路由→数据契约见 `notes/native-page-data-contract.md`。
> **行号口径**：本节 `file:line` 为 2026-11-11 代码快照；A/B/C 槽本轮并发改动同一批文件会微调行号，定位以「路径 + 符号名」为准。

### Wave 3.1 已逐字段对齐事实 → 已写进主表对应行备注

下列事实已以 `【Wave 3 已逐字段对齐】` 写进主表对应行的 blocker/备注列（逐字段证据在 matrix § F.1）：

| 主表对应行 | 已写进的对齐事实 |
|---|---|
| realtime WS 长连 | 换票/补投裸 body camelCase `{ticket,wsUrl,expiresInSeconds,connId}` / `{events,latestSeq}`；WS `RealtimeEnvelope{id,type,topic,ts,seq,payload}` 与 `sys.notify` / `presence.bulk` |
| SSE 流式传输 | `event: meta/delta/done/error` 与 `CompletionRequest` camelCase |
| WeChat/Alipay pay | prepay 五键 `channel/amount_fen/subject/ref_type/ref_id` → `PrepayResult{channel,out_trade_no,params}` |
| push entry | push devices 五键 `registration_id/alias/device_id/platform/mock` |
| remote auth / OTP | 登录/刷新 DTO snake_case、`Pagination{page,size,total,totalPages}` 1-based |
| `X-Session-ID` / `X-Device-ID` 逐请求头 | 会话三件套 + `AuthBizCode` 10021–10024 单飞 401 刷新 |

> 这 6 行的能力状态仍按原值（`Partial` / `Placeholder` / `Migrated` 等）**不变**：字段对齐不构成三端真机 / 像素 / e2e 证据，不升级状态。

### Wave 3.2 新增 Deferred / Partial 登记（逐项，带 blocker + follow_up，不判绿）

| 项 | 状态 | blocker | follow_up | 证据锚点（file:line） |
|---|---|---|---|---|
| 错误码双轨：`core/network/NetworkCodes` 遗留小码表 vs 后端五位数码表；`10005`/501 无分支；非 401 `{error,code}` 分类；信封 `timestamp` | `Partial` | 小码表（`OK=200`/`EXPIRE_TOKEN=401`）与五位数码表（`10002–10005`/`10021–10024`/`50000`）并存，`0` 同值不同义 | A 槽统一码表分支 + `10005` 非重试分支 + `timestamp` 解析 + 契约测试；见 matrix § F.2 #1 | `core/network/NetworkCodes.kt:21-57`（`object NetworkCodes`）、`app/internal/delivery/http/dto/response/common.go:17-30` |
| 三头无人读：`Client-Type` / `Client-Version` / `Client-BuildNumber` 后端不读 | `Partial` | 后端仅消费 `X-Session-ID` / `X-Device-ID` / `Authorization`，三头发了无人读 | 书面登记三头用途；确需后端消费再提后端变更（后端只读）；见 matrix § F.2 #3 | `core/network/DemoRequestHeaders.kt:16-19`、`app/internal/delivery/http/middleware/local_session_auth.go:36-37` |
| OHOS `body.platform` 伪值（映射成 `android`，后端 `oneof=android ios`） | `Partial` | 后端不接受 `ohos`/`harmony`，客户端只能伪报，单设备会话口径失真 | 后端扩 `oneof` 或客户端登记伪值语义，勿静默伪装；见 matrix § F.2 #2 | `feature/auth/AuthApiContracts.kt:357-380`（`AuthDeviceContext.platformForBody` / `platform()`）、`app/internal/delivery/http/dto/request/user_request.go:9` |
| InMemory DB / 明文凭据 | `Deferred` | OHOS fork 无 Room/SQLDelight klib（默认 `InMemoryDatabase` 是有意设计）；会话 JSON 含 token 以明文 `String` 落 KV，无 Keystore/Keychain 抽象 | 平台 SQL klib 就位后经 `DatabaseProvider.setFactory` 注入；Android Keystore / iOS Keychain / OHOS 资产保护接入，token 不落明文 KV；见 `notes/backend-alignment-gaps.md` §1 #2–#3 | `core/database/InMemoryDatabase.kt:15`、`core/database/DatabaseProvider.kt:9-17`、`core/account/AccountSessionStore.kt:40`、`core/storage/KeyValueStore.kt:33,49` |
| 无 Repository / 缓存 / 离线 / SSOT | `Deferred` | 仅社区一域有真实 HTTP Repository，其余 feature 走进程内 mock；无跨端数据层契约、无缓存 / 离线队列 / 单一真相源 | 先定 SSOT 契约再补 Repository + 缓存 + 离线队列；按 `notes/native-page-data-contract.md` §3 试点页逐页落地；见 `notes/backend-alignment-gaps.md` §1 #1 | `feature/home/HomeTodoStore.kt:15`、`feature/home/HomeMockData.kt:3`、`core/database/DatabaseProvider.kt:9-17` |
| 双方无 trace-id / request-id | `Deferred` | 后端全仓无 `X-Request-ID`/traceparent；KMP `DemoRequestHeaders` 只发 6 个头，双方日志无法按请求串联 | 客户端生成 `X-Request-ID` 并透传 + 结构化日志；后端日志/Metrics 关联需后端配合（本轮只读）；见 `notes/backend-alignment-gaps.md` §1 #7 | `core/network/DemoRequestHeaders.kt:11-15`；后端全仓 grep 0 命中 |
| analytics gRPC 缺 | `Deferred` | KMP 无 gRPC/connect-kotlin 产物、OHOS 无产物；`/home/data_analytics` 为 Vico 图表 + mock；Flutter 走 BFF `:9090` | 字段口径对齐 + 三端可用性评估（M4.1–M4.4），产物就位后替换 mock，不伪称完成（主表「analytics gRPC」行已是 `Deferred`） | `feature/home/AnalyticsCharts.kt:11-13`、`feature/home/HomeMockData.kt:81` |
| `/health` 无探活 | `Deferred` | 后端 `GET /health` 仅 `200 {"status":"ok"}` 浅存活探针（不查依赖）；KMP 全仓无探活 / readiness / 不可达降级 | 客户端补探活 + 降级文案；后端补 readiness（查 DB/Redis）需后端侧变更（本轮只读）；见 `notes/backend-alignment-gaps.md` §1 #6 | `app/internal/delivery/http/router/router.go:67-69`；KMP 全仓 grep 0 命中 |

> 本表 8 项与主表既有行（「关系型存储」`Degraded`、「analytics gRPC」`Deferred`、「3-flavor 环境系统」`Deferred`、「SSE 流式传输」`Partial`）互为交叉引用，不重复改主表已验收措辞；结构性 gap 的完整 blocker/follow_up/是否需外部前置见 `notes/backend-alignment-gaps.md`。

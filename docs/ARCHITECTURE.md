# My_kmp_project 架构文档

> 本文按仓库源码事实整理，对应提交 `55116c5`（`main`）。版本、插件与依赖坐标以 `gradle/libs.versions.toml` 为唯一真相源；模块索引见 `AGENTS.md` 的 Map，完整 28 模块图见 `composeApp/src/commonMain/kotlin/com.example.my_kmp_project/MODULES.md`。架构图见 `.archify/architecture-kmp-client-20261108-100242/kmp-client-architecture.html`。

## 1. 定位与技术栈

Kotlin Multiplatform + Compose Multiplatform 演示工程，同时面向 **Android / iOS / HarmonyOS(OHOS)** 三端。三端的原生壳很薄，共享的 Kotlin/Compose 代码承担绝大部分 UI、领域逻辑与网络契约。

| 项 | 值 | 出处 |
|---|---|---|
| Kotlin（OHOS fork） | `2.2.21-0.4.0` | `gradle/libs.versions.toml:27` |
| Compose Multiplatform | `1.9.2-0.4.0` | `gradle/libs.versions.toml:23` |
| AGP / compileSdk / minSdk | `8.6.0` / `36` / `26` | `gradle/libs.versions.toml:3-5` |
| Ktor（CPF OH 线） | `3.3.3-0.3.0` | `gradle/libs.versions.toml:33` |
| Gradle wrapper / JDK | `9.4.1` / 17 | `build.gradle.kts:14`、`composeApp/build.gradle.kts:34` |
| 包根 | `com.example.my_kmp_project` | 各模块 `package` 声明 |

依赖从 `https://maven.eazytec-cloud.com/nexus/repository/maven-public/` 解析（含 OHOS Kotlin/Compose fork 产物），见 `settings.gradle.kts:4-21`。**三处单一真相源**：版本目录 `libs.versions.toml`、模块清单 `settings.gradle.kts`、库模块共享接线 `build.gradle.kts`。

## 2. 分层与依赖方向

```
① composeApp      装配 + 三端入口 —— 唯一二进制产物
② 17 个 feature   业务模块，之间禁止互相依赖
③ component       7 类能力端口（端口/适配器）
④ 10 个 core      横向公共层
   :ohosAggregate  OHOS 聚合工程
```

依赖方向单向向下：`composeApp → feature → component / core`。跨 feature 的跳转**只能**在装配层接线，因为 `app/` 是唯一允许看见全部 feature 的地方（`composeApp/build.gradle.kts:131-149`）。这条规则由 `scripts/check-layer-deps.sh` 静态强制，禁止三类违规：feature→feature import、向上 import、feature 间模块依赖（`AppCrossFeatureScreens.kt:21-28`）。

**二进制边界**：库模块一律不产 framework / sharedLib，iOS `ComposeApp.framework` 与 OHOS `libkn.so` 只由 `:composeApp` 产出（`build.gradle.kts:95-102`）。新模块必须 `settings.gradle.kts` include + 自己的 `build.gradle.kts`（plugins + `api(projects…)` + `android { namespace }`）；**buildSrc 在本机不可用**——`~/.gradle/init.gradle` 把 plugin-marker 钉在无法解析 `org.gradle.kotlin.kotlin-dsl` 的镜像上，因此共享接线放在根工程 `subprojects {}`，而非约定插件。

## 3. 模块清单

### 3.1 `core/*`（10 个横向公共模块，`settings.gradle.kts:24-33`）

| 模块 | 职责 | 代表文件 |
|---|---|---|
| `:core:base` | 基础能力、`AppUuid` | `core/base/` |
| `:core:network` | HTTP 引擎、信封解析、SSE、realtime 传输、token 刷新、逐请求头 | `NetworkFacade.kt`、`DemoRequestHeaders.kt`、`Sse.kt`、`TokenRefresh.kt`、`realtime/` |
| `:core:router` | 路由表、深链、导航器 | `AppRoutes.kt`、`RoutePath.kt`、`ProductRouteDispatch.kt`、`DeepLinkRouter.kt` |
| `:core:account` | 会话模型、会话水合/持久化、隐私同意 | `AccountFacade.kt` |
| `:core:design` | 设计令牌、沉浸式顶栏组件、**全部** composeResources drawable/font | `DemoColors.kt`、`ImmersiveInsets` |
| `:core:platform` | `Platform` expect/actual：打开 URL、剪贴板、Toast、WebView、扫码、返回键 | `PlatformOpen.*.kt`、`PlatformWebView.*.kt`、`PlatformCameraScan.*.kt` |
| `:core:ui` | 通用 UI 能力、网络图片、Tab chrome | `PlatformNetworkImage.*.kt`、`LocalMainTabChrome` |
| `:core:analytics` | 埋点门面 | `core/analytics/` |
| `:core:storage` | **唯一 KV 持久化契约** | `KeyValueStore.kt` + 三端 actual |
| `:core:database` | 结构化实体表（带主键、响应式查询） | `Database.kt`、`InMemoryDatabase.kt`、`DatabaseProvider.kt` |

### 3.2 `component`（端口层，`settings.gradle.kts:34`）

`auth`、`pay`、`push`、`chat`、`webview`、`routing`、`realtime` 七类端口。装配层通过 `AppContainer.BridgeComponent` 持有 `PushBridge` / `PayGateway` / `ChatPort` 的实例（`AppContainer.kt:38-43`），`ShellBootstrap` 通过 `CrossFeatureScreens.install(...)` 注入跨 feature 屏幕端口（`ShellBootstrap.kt:25`）。

### 3.3 `feature/*`（17 个业务模块，`settings.gradle.kts:38-43`）

`ai`、`auth`、`chat`、`classroom`、`commerce`、`community`、`content`、`friend`、`home`、`live`、`media`、`mine`、`pay`、`scan`、`shell`、`wallet`、`web`。每个都能单独跑 `./gradlew :feature:x:testDebugUnitTest`。已读到的代表实现：`feature/ai/AiStreamScreen.kt` + `AiSseApi.kt`、`feature/community/http/HttpCommunityPostRepository.kt`、`feature/pay/PayApi.kt` + `PaymentPrepayApi.kt`、`feature/shell/MainBottomBar`。

### 3.4 `composeApp/src/commonMain/.../app/`（装配层 6 文件）

| 文件 | 职责 |
|---|---|
| `DemoApp.kt` | `@Composable` 入口别名，直接调 `AppShell()` |
| `AppShell.kt` | 壳状态机（`Splash` / `PrivacyConsent` / `Main` + `AuthOverlay`）、Scaffold、Tab 底栏、隐私同意交互 |
| `AppContainer.kt` | Composition root：`NetworkComponent` / `SessionComponent` / `BridgeComponent` / `NavigationComponent` |
| `ShellBootstrap.kt` | 幂等 `install()`，三端入口在渲染共享 UI 前调用 |
| `AppCrossFeatureScreens.kt` | `CrossFeatureScreenPort` 实现，跨 feature 屏幕表与回退行为 |
| `RealtimeRuntime.kt` | realtime 生产构造点，见第 8 节 |

## 4. 三端入口与壳

| 平台 | 入口 | 关键动作 |
|---|---|---|
| Android | `composeApp/src/androidMain/.../MainActivity.kt:21-40` | `installSplashScreen()` → `enableEdgeToEdge()` → `setContent { NativeAndroidApp() }`；同时 `AndroidAppContext.install` / `AndroidActivityContext.install` / `platformNetworkBootstrap()` / `ShellBootstrap.install()` / `handleDeepLinkIntent` |
| iOS | `composeApp/src/iosMain/.../MainViewController.kt:17-21`、`iosApp/iosApp/ContentView.swift` | SwiftUI 壳承载 Compose Island；`MainViewController` 已标 `@Deprecated`（ADR 0002），实际用 `MineIslandViewController` / `SecondaryRouteViewController` |
| OHOS | `composeApp/src/ohosMain/.../MainArkUIViewController.kt:33-43`、`harmonyApp/entry/src/main/ets/pages/Index.ets:1-16` | ArkTS 经 `libentry.so` 加载 `@CName("MainArkUIViewController")`，`initMainHandler(env)` + `ComposeArkUIViewController`；ArkTS 拥有启动图 / Tab / Mine 根 / 认证 |

**沉浸式是硬性规范**（`AGENTS.md` Compose 节）：状态栏与导航栏透明、内容延伸其下；`DemoApp` Scaffold 用 `containerColor = Color.Transparent` + `contentWindowInsets = WindowInsets(0)`；禁止裸 `TopAppBar`，改用 `core.design.ImmersiveCenterTopAppBar` / `ImmersiveTopAppBar` / `MineTopBar`；Shell 内容 inset 由 `ImmersiveInsets.shellContentInsets` 处理（Tab 根含 49.dp 底栏，二级页 `navigationBarsPadding()`）；二级页必须 `ReportMainTabRoot(isRoot = false)`。

启动图与图标对齐 Flutter SoT `my_ai_project`：Android 五密度 `mipmap-*/ic_launcher.png` + `launch_image.png`、Android 12+ `Theme.DemoApp.Splash`、iOS `AppIcon.appiconset` / `LaunchImage.imageset`、Harmony `startIcon.png` + `start_window_background=#F7FAFC`，共享 splash drawable 在 `core/design/src/commonMain/composeResources/drawable/`。

## 5. Source set 与平台差异

| Source set | 用途 |
|---|---|
| `commonMain` | 共享 UI / 领域 / `expect` 声明，默认落点 |
| `androidMain` / `iosMain` / `ohosMain` | 平台 API、Activity/ViewController/ArkUI 入口、cinterop |
| `commonTest` / `androidUnitTest` | 共享单测与 app 层测试 |

**中间源集**（避免在 `commonMain` 引入 OHOS 不支持的产物）：

| Source set | 覆盖 | 内容 |
|---|---|---|
| `networkKtorMain` | android + ios + ohosArm64 | Ktor client；ohosX64 走 `net_http` cinterop 回退 |
| `settingsMain` | android + ios | `multiplatform-settings` KV；OHOS 直接实现 `KeyValueStore`（无一条 settings 线能同时覆盖 Android 与 OHOS） |
| `imageCoilMain` | android + ios + ohosArm64 | CPF Coil 网络图；ohosX64 占位 |
| `uuidCpfMain` | android + ios + ohosArm64 | CPF `com.benasher44:uuid`；ohosX64 stub |
| `chartsVicoMain` | Android / iOS | Vico 图表（CPF 无 OHOS 适配） |

平台差异集中在 `Platform` expect/actual 与上述中间源集。`ohosArm64` 用 CPF Ktor **CIO**（HTTPS 已通）与 Coil/uuid；`ohosX64` 因缺 klib 分别用 cinterop / 占位 / stub——这是 OHOS 双架构最需要留意的约束。

## 6. 网络与后端契约

**`NetworkFacade`**（`core/network/.../NetworkFacade.kt:5-34`）是进程级网络唯一入口，feature 只依赖它、不自行构造引擎。它持有 `rawClient` 与装饰后的 `client`，提供 `api()` / `rawApi()`（Flutter `skipAuthRefresh` 对等，refresh 请求专用，防递归）/ `setAuthRefreshSupport(...)` / `rebuildClient()` / `bindAccessToken(token, sessionId, deviceId)` / `clearAccessToken()`。

**逐请求头**（`DemoRequestHeaders.kt:11-26`）：`Client-Type` / `Client-Version` / `Client-BuildNumber`，登录态且非免鉴权 host 时带 `Authorization: Bearer <token>`，并**与 Authorization 同一 gating** 注入 `X-Session-ID` / `X-Device-ID`（空白值省略）。头名来自后端 `middleware/supabase_session_auth.go` 的 `HeaderSessionID` / `HeaderDeviceID`，供 `DeviceSessionUsecase.Validate` 做单设备登录校验。

**401 静默刷新**：`AuthRefreshingApiClient` 独占 401 流（刷新→重试 / 清会话），`feature/auth` 经 `AuthSessionBootstrap.install()` 注入策略与单飞刷新器；平台 client 不再直接触发 `tokenExpiredHandler`，避免「先跳登录再刷新成功」。

**SSE / realtime 传输**：`Sse.kt:10-35` 定义 `text/event-stream`、状态 <600 不判失败、增量 UTF-8 解码契约；ohosX64 的 `libnet_http` 只能在完成回调给整包 body，抛 `SseStreamingUnsupportedException`。`core/network/realtime/` 提供换票（`RealtimeWsTicketApi`）、补投（`RealtimeWsSyncApi`）与 `networkKtorMain/.../KtorRealtimeWsTransport.kt`。

**后端契约（`notes/backend-contract-matrix.md:1-23`）**：以 Go 仓库 `my_go_study` 的 16 个域路由为唯一真相源，逐条核对 KMP 的 94 条归一化 `/api/v1/*` 路径（后端唯一路径 117 条）：91 条 `aligned`、3 条组合前缀常量、22 条 `missing(待接线)`、4 条解析器盲区。`feature/pay/PaymentPrepayApi.kt` 是补齐 `POST /api/v1/payments/prepay` 的独立契约（对应 `payment/router.go:49`）。**注意两条不同的漂移轴**：`audit-flutter-drift.sh` 比的是 KMP ↔ Flutter(Dart)，本表比的是 KMP ↔ 后端；差异处置见 `notes/route-inventory.md`（36 条书面 allowlist，已用探针验证未登记差异仍 exit 1）。

## 7. 会话与持久化

`AccountSession`（`AccountFacade.kt:13-33`）携带 `token` / `refreshToken` / `sessionId` / `deviceId` / `userId` 等；`AccountFacade` 是单例：`setSession()` 持久化后若已登录则 `NetworkFacade.bindAccessToken(token, sessionId, deviceId)`，否则 `clearAccessToken()`；`logout()` 清空存储与网络头；冷启动 `ensureHydrated()` 从持久化恢复并重新绑定（`AccountFacade.kt:49-58`）。

**KV**：`core/storage/.../KeyValueStore.kt:3-28` 是项目**唯一** KV 契约，值槽刻意收窄为 `String` / `Boolean` / `List<String>`，无 `clear()` 与 `contains()`（OHOS 等价 API 需 API 23+），删除用 `remove`。Android `SharedPreferencesSettings`、iOS `NSUserDefaultsSettings(suiteName=…)`、OHOS 双架构 `PreferencesKeyValueStore` 直接基于 `OH_Preferences` NDK 写 app 沙箱数据目录，重启不丢（M8.5，旧 `/tmp` 文件 actual 已移除）。会话、隐私同意、`RealtimeSeqStore` 同源。

**结构化表**：`core:database` 提供 `Database` / `Table<T>` / `Entity`，`all: StateFlow<List<T>>` 响应式快照、`insert/update/delete/findById/query`。默认 `InMemoryDatabase` 是**有意设计**而非遗留缺陷——OHOS fork 无 Room/SQLDelight klib，强引 SQL 依赖会挂 `ohosArm64` / `ohosX64`（`core/database/build.gradle.kts` 注释）。换 driver 只改 `DatabaseProvider.setFactory`，业务层零改动。

## 8. 装配与实时链路

`ShellBootstrap.install()`（`ShellBootstrap.kt:16-29`）以 `@Volatile installed` 保证幂等，三端入口都在渲染共享 UI 前调用，顺序固定：

1. `AuthSessionBootstrap.install()` —— 会话水合 + 401 静默刷新装机；
2. `CrossFeatureScreens.install(AppCrossFeatureScreens)` —— 跨 feature 屏幕端口；
3. `RealtimeRuntime.install()` —— 放在最后，确保换票/补投 API 捕获最终装饰后的 `NetworkFacade.api()`。

`RealtimeRuntime.kt:24-90` 是 realtime 的**生产构造点**：构造 `AppRealtimeClientImpl`（ticketApi / syncApi / outboundQueue / seqStore / inboundRouter / notifyHandler / telemetry / session / scope），默认 `transportFactory` 走 `createRealtimeTransport → createPlatformRealtimeWsTransport → Ktor`；再构造 `RealtimeStartupGate`，`privacyGranted` 取自 `createPrivacyConsentStore()`。`install()` 只构造对象，`prepare` 与首连全部丢进 `CoroutineScope(SupervisorJob() + Dispatchers.Default)` 的后台 `launch`，**不 await、不阻塞冷启动**；12s 首连超时与 8s 观察窗只记日志。闸门顺序：隐私未同意 → 未登录 → 已活跃不重连 → 带 12s 超时首连 → 固定订阅 `sys.notify` + `presence.bulk`。

五个回调装配位置：`app_startup` 在 `ShellBootstrap.install()`；`onPrivacyGranted` / `onAfterLogin` / `onAfterLogout` 绑定 `AppShell` 三个交互点；`onEnvChanged` / `onAppLifecycle` 装配层已暴露入口，但触发源在 `feature/mine` 环境切换 UI 与平台 source set，属书面 GAP，未塞伪实现。

## 9. 构建与发布

- **模块注册**：`settings.gradle.kts:23-44`，`listOf(...).forEach { include(":feature:$it") }` 生成 17 个 feature。
- **共享接线**：根 `build.gradle.kts:35-129` 对 `:core:*` / `:feature:*` 统一配置仓库顺序（eazytec 优先，排除 AGP 让 aapt2 落阿里云镜像）、`androidTarget` JVM 17、`iosX64/iosArm64/iosSimulatorArm64/ohosArm64/ohosX64` 五个 target、`commonMain` 的 coroutines / atomicfu / serialization-json、`commonTest` 的 kotlin-test、以及 `iosMain` / `ohosMain` 层级。
- **composeApp targets**：`composeApp/build.gradle.kts:29-110`，iOS framework `baseName = "ComposeApp"`、`isStatic = true`；OHOS `sharedLib` `baseName = "kn"`，Release 链接 `optimized = false` 防 DevirtualizationAnalysis OOM；`rendererBackend` 默认 `fusion-renderer`（`gradle.properties`）；`ohosX64` 额外 `-lnet_http`。`-PandroidOnly=true` 跳过所有 native target（CI 用）。
- **OHOS 发布契约**：`publish*BinariesToHarmonyApp` 把 `libkn.so` + `libkn_api.h` 投到 `harmonyApp/entry/libs/{arm64-v8a,x86_64}/` 与 `cpp/include/`，Compose 资源投到 `entry/src/main/resources/rawfile/composeResources/…`；`-PharmonyAppPath` 可覆盖输出根。ArkTS 入口 `Index.ets` 依赖 `MainArkUIViewController` 符号稳定。
- **运行**：`scripts/run.sh {android|ios|harmony}`（分别包 `run-android.sh` / `run-ios.sh` / `run-harmony.sh`）；始终用 `./gradlew` 而非系统 Gradle。
- **CI**：`.github/workflows/ci.yml` 单 pipeline，Android `assembleDebug` + `testDebugUnitTest`（`-PandroidOnly=true`），main push 上传 debug APK，`v*` tag 发 Release；iOS / OHOS 不在托管 runner。
- **并发锁**：`scripts/with-gradle-lock.sh` 与 `verify-parity.sh` 共用同一把 advisory lock，避免并行 agent 的窄构建耗尽 Gradle daemon 锁超时（曾观测 666s 等待 + 误归因）。

## 10. 门禁与验收

| 脚本 | 作用 | 当前状态 |
|---|---|---|
| `verify-parity.sh --static-only` | Kotlin 注释/括号词法扫描 + layer gate + 两个 native dispatch 守卫 | exit 0 |
| `check-layer-deps.sh` | 禁止 feature→feature / 向上 import / 模块级违规 | exit 0 |
| `check-harmony-pane-dispatch.sh` / `check-ios-page-dispatch.sh` | 65 个 Native pane / page 的 route→render 接线 | 全部 OK |
| `parity_task_audit.py` | 默认读**唯一执行账本** `tasks-ultra-fine-v2.md`，扫 `[ ]`/`[~]`，真实剩余 >0 即失败 | exit 1（162 条真实未完成；假绿已修） |
| `audit-flutter-drift.sh` | KMP ↔ Flutter 端点/路由漂移，36 条书面 allowlist | exit 0 |
| `screenshot_diff_gate.py` + `test_screenshot_diff_gate.py` | 像素门禁 runner 与 seam 测试 | runner 齐备、**未接硬门禁**；需真机截图证据，且依赖 numpy/PIL（本机 `/usr/bin/python3` 可用，默认 `python3` 缺 numpy） |
| `parity-evidence.sh` | 从 Gradle XML 汇总单测证据 | 辅助 |

`openspec/changes/sync-flutter-full-parity/tasks-ultra-fine-v2.md` 是唯一执行账本；`tasks-execution.md` 与 `tasks-fine.md` 已标注为历史账本，仅用于查漏。证据索引见 `openspec/changes/parity-flutter-to-kmp/notes/evidence/index.md`。

## 11. 平台缺口与设计约束

按 `openspec/changes/parity-flutter-to-kmp/notes/platform-gap-registry.md` 的五词状态表（`Migrated` / `Degraded` / `Placeholder` / `Deferred` / `Partial`），**页面可开 ≠ 数据契约已接线 ≠ Migrated**。关键约束：

- **厂商 SDK / 凭据**：JPush·FCM·HPush、微信/支付宝 OpenSDK、Apple/Huawei/Android IAP、融云 AppKey、WeChat 授权码均 `Deferred`；`PlatformPushSdk.fetchRegistrationToken()` 仍返回 null（`PushRegistrationApi.kt:24` 的 `GAP(R5)`），支付交接点保持不可用态，不伪造 Success。
- **OHOS actual 降级**：`PlatformWebView.ohos.kt`、`PlatformCameraScan.ohos.kt`、`PlatformOpen.ohos.kt` 明确为 `Degraded` stub（ArkWeb / NDK cinterop + 真机前置），KDoc 要求「Keep contract; fail acceptance honestly」。
- **65×2 原生页静态元组**：`iosApp/.../NativeSecondaryPages.swift`、`NativeFeatureCatalog.swift`、ArkTS `NativeFeaturePane` 为 15–43 行硬编码 `@State`、无 store/API，属 `Partial` / `Placeholder`。
- **截图硬门禁**：runner 齐备但缺成对 Flutter/KMP 真机截图，接空壳门禁会造成假绿或误阻塞 CI，故保持未接并登记。
- **其他 Deferred**：analytics gRPC（Flutter 走 BFF `:9090`，KMP 无 gRPC 产物）、3-flavor 环境系统、聊天语音录制/播放、屏幕防截取、DoKit/BLE/invoice demos。

## 12. 延伸阅读

- `AGENTS.md` —— 模块 Map、Compose/沉浸式规范、构建与 OHOS 发布契约的权威索引。
- `composeApp/src/commonMain/kotlin/com.example.my_kmp_project/MODULES.md` —— 28 模块依赖图与中间源集。
- `openspec/changes/` —— 各决策的 proposal / design / tasks（如 `adopt-cpf-ktor-ohos/`、`unify-core-kv-storage/`）。
- `notes/backend-contract-matrix.md`、`notes/route-inventory.md` —— 后端契约逐条对照与 drift allowlist。
- `docs/deep-dive/` —— 逐模块深入笔记（本机未跟踪目录）。

## 13. 当前对齐状态（Wave 3）

> 本节只给**指针与口径**，不复制整份差距清单。逐字段结论与 file:line 证据见 [backend-contract-matrix.md](../notes/backend-contract-matrix.md) § F（D1）；结构性 gap 台账见 [backend-alignment-gaps.md](../notes/backend-alignment-gaps.md)（D5）；65×2 原生页逐路由数据契约表见 [native-page-data-contract.md](../notes/native-page-data-contract.md)（D2）；非端点常量 / 测试夹具 / 解析器盲区登记见 [route-inventory.md](../notes/route-inventory.md) §9（D4）；五词状态台账见 [platform-gap-registry.md](../openspec/changes/parity-flutter-to-kmp/notes/platform-gap-registry.md)（D3）；证据成熟度见 [evidence/index.md](../openspec/changes/parity-flutter-to-kmp/notes/evidence/index.md)（D7）。

Wave 3 以 Go 后端 `my_go_study` 为唯一真相源（**全程只读**），完成一次**纯双方**（KMP ↔ 后端、不涉及任何第三方）契约复核，结论分三层：

1. **已逐字段对齐**：realtime / SSE / prepay / push / 分页 / 登录 DTO / 会话三件套 / `AuthBizCode` 的字段与协议层一致（D1 § F.1）——这是「不是差距」的澄清，勿误判为待办。
2. **契约层仍待收敛（本轮 A/B/C 槽改代码处置）**：错误码双轨（`NetworkCodes` 小码表 vs 后端五位数码表、`10005`/501 无分支、`timestamp`）、OHOS `body.platform` 伪值、三头无人读、realtime 少发 `connId`、alias 被后端强制覆盖、transactions 双分页、两层鉴权链、403「账号不可用」、金额三套口径、14 条自有端点待接（D1 § F.2）。
3. **结构性 gap（本轮只登记、不改代码）**：无 Repository / 缓存 / 离线 / SSOT、InMemory DB、明文凭据、65×2 静态页、analytics gRPC 缺失、`/health` 无探活、双方无 trace-id，以及后端自有占位语义边界（亚分金额 >2dp 返回 502、种子 lab-only、对账 local_missing 未实现）；逐项保留 blocker + follow_up（D5）。

另：`route-inventory.md` §9 把 3 条自造组合前缀、7 条测试夹具、4 条 `resolve.py` 盲区登记为「非端点常量 / 夹具 / 盲区」，**不计入** `audit-flutter-drift.sh` 的 drift；契约矩阵 B 区 22 条 `missing(待接线)` 措辞未改，仅追加状态列备注。

> **口径**：状态词只用 `Migrated | Degraded | Placeholder | Deferred | Partial`。按 registry `Rule (2A)`，**页面可开 ≠ 数据契约已接线 ≠ `Migrated`**；字段层「已对齐」不等于能力完成（三端真机 / e2e / 像素证据仍缺）。所有第三方内容（厂商 SDK / IAP / IM / Supabase / 短信 / LLM / 对象存储 / 渠道回调 / Prometheus）均为「外部前置，非本轮范围」，不列为需对齐。

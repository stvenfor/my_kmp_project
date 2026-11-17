# 08 · 整体架构图

> 这张页配一张全量架构图（本页底部）。图上每个方框都能点回仓库里的真实文件行号，图里说的每句话都有源码依据。
> 图的原始文件：`/Users/qpy/Desktop/github/my_kmp_project/.archify/architecture-kmp-overview-20261022-162513/kmp-architecture.html`（可交互，深浅色主题、可聚焦分步看）。

## 一句话看懂这个项目

它不是「一份 Compose UI 跑三端」的教科书项目，而是 **一份 Kotlin 共享代码 + 三套各自长出来的原生壳**：Android 用 Jetpack 原生导航做外壳，iOS 用 SwiftUI 铺了 65 个原生页，HarmonyOS 用 ArkTS 铺了 65 个原生面板；Compose 只在几个「岛」里出现。而真正决定架构形状的是三条硬约束：**只有 `:composeApp` 能产出二进制**、**feature 之间不得互相依赖**、**REST / SSE / WebSocket 三条通道不得互相翻译**。

## 怎么读这张图

图按「阅读顺序」排成三列车道，纵向是层次：

| 从上到下 | 这一层是什么 |
|---|---|
| 用户 | 三个平台的真实入口 |
| 三端原生壳 | Android / iOS / Harmony 各自的入口与原生 UI（图上的虚线框 = 一个「区域」，表示它们同属原生壳这一层） |
| `:composeApp` | 装配层：把 17 个 feature 和 10 个 core 装进一个 App，并产出 iOS framework / OHOS `.so` |
| `:core:router` | 路由分发：RouteHost 把路径映射到 feature 的屏 |
| feature 层 · `:core:ui` | ② 业务模块 + ④ 横向共享 UI 件 |
| `:component` | ③ 垂直端口（pay/push/chat/webview/auth/routing/realtime） |
| `:core:network` · `:core:account` | ④ 网络与账号，REST/SSE/WS 三条通道从这里出去 |
| `:core:storage` | 唯一 KV 契约，落到各端存储 |
| 后端 / IM SDK | 外部系统（图上 `external` 类型 = 项目外的东西） |

看的时候抓四条线，别的都可以先忽略：

1. **启动线**：用户 → 某个壳 → `:composeApp` → `:core:router` → feature 层。这是主路径。
2. **产物线**：`:composeApp` → sharedLib（`libkn.so`）→ publish → Harmony 工程。iOS 侧同理产出 `ComposeApp` framework。
3. **通信线**：feature → `:component` 端口 → `:core:network` → 后端；其中 WS 和 SSE 各自通向 `realtime` / AI 补全两个不同的 messagebus 节点。
4. **门禁线**：CI/脚本 → feature 层（那条虚线标的是「R7 门禁」）。

图上右侧的说明卡（cards）放的是「不看代码就猜不到」的事实，比如三端壳到底长什么样、三条通道各自的用途。

## 第 1 件事：五层，和依赖的唯一致意

| 层 | 内容 | 允许依赖 |
|---|---|---|
| ① 装配 | `:composeApp` | 什么都可以依赖，**但它是唯一产出二进制的模块** |
| ② 业务 | `:feature:*`（17 个） | 只允许向下依赖 core/component，**feature 之间 0 依赖** |
| ③ 垂直端口 | `:component` | `api` 转发 `:core:network` / `:core:base` / `:core:account` / `:core:router` |
| ④ 横向 commons | `:core:*`（10 个） | 彼此按层次，不回头 |
| ⑤ 原生壳 | `iosApp` / `harmonyApp` + 三端入口 | 只做 host，不写业务 |

依据：`settings.gradle.kts:23-44` 里共 29 个 Gradle project；`composeApp/build.gradle.kts:121-149` 是 `:composeApp` 对 `:component` + 10 个 `:core:*` + 17 个 `:feature:*` 的 `implementation`；`component/build.gradle.kts:38-41` 是端口层的 `api` 转发。

**为什么 feature 必须同级且互不依赖**：这样任何一个 feature 能单独跑测试、单独被拆走。这条不是口头约定，`scripts/check-layer-deps.sh` 会真的算账并报错。

## 第 2 件事：三端壳的真实形态（这里最容易被骗）

教科书会说「KMP 就是一份 UI，三端只是套个壳」。这个项目不是：

| 端 | 实际形态 | 依据 |
|---|---|---|
| **Android** | `MainActivity` → `installSplashScreen()` → `enableEdgeToEdge()` → `ShellBootstrap.install` → 深链；然后进 `NativeAndroidApp`/`NativeAndroidMain`，**用 Jetpack 原生 tab 直接 host 共享的 `HomeRouteHost` / `CommunityRouteHost` / `MineRouteHost` / `ContentRouteHost`**；唯一的 Compose 浮层是 `MineIsland` | `MainActivity.kt:21-41`、`NativeAndroidMain.kt:563-595`、`:82`、`:546-547` |
| **iOS** | SwiftUI 外壳 + **65 个 `Native…Page` 原生页**；Compose 只通过 `MineIslandViewController` / `SecondaryRouteViewController` 全屏岛出现；Kotlin 侧的「整包 App」入口已标 deprecated | `ContentView.swift:232-246`、`:435-450`、`NativeFeatureCatalog.swift:470`、`MainViewController.kt:16-20`、`:28` |
| **HarmonyOS** | ArkTS `Index.ets` 用 `nativePaneIdFor` / `featurePageFor` 渲染 **65 个原生面板**，再从 `libentry.so` 拿 `MainArkUIViewController` 建岛；ADR 0002 明确：ArkTS 拥有启动页 / tab / Mine 根 / 鉴权 | `Index.ets:996-1000`、`:836-842`、`:1963-1970`、`NativeFeatureCatalog.ets:484`、`MainArkUIViewController.kt:28-71`、`docs/adr/0002-*` |

读法：**「共享」的是路由与屏（RouteHost 那层），不是整个界面**。所以改一个 feature 时，你要同时想清楚三件事：这个屏在 Android 是不是被原生 tab 直接 host、在 iOS 有没有对应 `Native…Page`、在 Harmony 有没有对应 pane。少一个就是「页面分裂」，而这正是两个 dispatch 门禁脚本要拦的。

## 第 3 件事：三条长连接通道，不得互相翻译（ADR 0005）

| 通道 | 契约与实现 | 唯一用途 | 依据 |
|---|---|---|---|
| **REST** | `ApiClient` / `KtorApiClient`（`networkKtorMain`；ohosX64 回落 `net_http` cinterop） | 全部 `/api/v1/**` 请求 | `core/network/src/networkKtorMain/.../KtorApiClient.kt:37`、`core/network/build.gradle.kts:27-40` |
| **SSE** | `core/network/Sse.kt` + `core/sse/SseParser` | 只服务 `POST /api/v1/sse/completions`（AI 流式补全） | `Sse.kt:11`、`feature/ai/.../AiSseApi.kt:16-32` |
| **WebSocket** | `component/realtime/AppRealtimeClientImpl` | `/api/v1/realtime/ws-ticket` 换票 → `/api/v1/realtime/sync` 同步 | `AppRealtimeClientImpl.kt:117-137`、`RealtimeConfig.kt:21-22` |

还有一条**已知空缺**：IM 不是通道，是端口。`StubChatPort.isReady = false`（`ChatPort.kt:12-13`）配 `MockImEngine`（`ImEngine.kt:149`），即聊天现在是假的，真实融云 SDK 属于 parity 清单里的 R5 缺口。图上把它画成 `external` 的 IM SDK 节点 + 一条 ChatPort 边，就是在提醒：**这条线还没有真的接上**。

`docs/adr/0005-*` 的口径是这三条通道的语义不能互相「翻译」——不要拿 SSE 去做 realtime sync，也不要拿 WS 去做 AI 补全。

## 第 4 件事：唯一的二进制产出者，和 OHOS 投递契约

- 库模块永远不产二进制：根 `build.gradle.kts:94-102` 只给它们配 targets。
- iOS：`:composeApp` 出静态 framework `ComposeApp`（`composeApp/build.gradle.kts:44-46`）。
- OHOS：`:composeApp` 出 `sharedLib`，`baseName = "kn"` → `libkn.so`（`composeApp/build.gradle.kts:51-56`）。
- 改完共享代码必须重新 publish：`publish{Debug,Release}BinariesToHarmonyApp[X64]` 把 `libkn.so` + `libkn_api.h` 拷进 `harmonyApp/entry/libs/{arm64-v8a,x86_64}` 和对应 `cpp/include/`，再把 compose 资源拷进 `rawfile/composeResources/`（`composeApp/build.gradle.kts:259-299`）。

大白话：**Harmony 工程看到的 `.so` 是上一次 publish 的快照**。不 publish 就跑 DevEco，跑的是旧代码——这是这个项目最常见的「我明明改了却没生效」。

## 第 5 件事：持久化，一个契约 + 一个坑

- `core.storage.KeyValueStore` 是**唯一** KV 契约（`KeyValueStore.kt:3-28`）。Android / iOS 走 `multiplatform-settings`（`settingsMain`）；**两种 OHOS 架构都直接实现到 `OH_Preferences` NDK**（`-lohpreferences`，`KeyValueStore.ohos.kt:30-32`）。
- 为什么不用一套 settings 通吃：`1.3.0` 没有 ohos klib，CPF `-0.3.0` 那条线又缺 `multiplatform-settings-android` AAR（404）。这条历史原因写在 `openspec/changes/unify-core-kv-storage/`。
- `:core:database` 目前**只有契约 + in-memory 驱动**（`Database.kt:13`）——真正的结构化持久化还没落地，别当它已存在。

## 第 6 件事：架构是被脚本守着的，不是被文档守着

| 门禁 | 管什么 | 依据 |
|---|---|---|
| `scripts/check-layer-deps.sh` | 三条依赖轴：feature 不互相依赖、不越层、core 不回头 | `:2-10` |
| `scripts/check-ios-page-dispatch.sh` | iOS 65 页与 Kotlin 路由对得上 | `:2-16` |
| `scripts/check-harmony-pane-dispatch.sh` | Harmony pane 与路由对得上 | `:2` |
| `scripts/verify-parity.sh` | 对比 Flutter SoT 的 parity 校验（advisory-lock 串行；`--offline/--android-only/--static-only`） | `:1-5` |
| GitHub Actions | **只跑** `verify-parity.sh --static-only` + `testDebugUnitTest` + `:composeApp:assembleDebug`（`-PandroidOnly=true`）；iOS/OHOS 不在托管 runner 上 | `.github/workflows/ci.yml:75-80` |

工程侧两个本地约束，图上那张卡也记了：**(a) Gradle 任务名里不能带冒号以外的奇怪字符，且 `Project.exec` 在 Gradle 9 已移除**——29 处调用点改成注入 `ExecOperations`（root `build.gradle.kts:12-20`，wrapper 9.4.1）；**(b) buildSrc 在这台机器上不可用**（`~/.gradle/init.gradle` 把 plugin-marker 解析钉在取不到 `org.gradle.kotlin.kotlin-dsl` 的镜像上），所以新模块必须手写 `settings.gradle.kts` include + 自己的 `build.gradle.kts`。

## 两个「别信旧资料」的提醒

1. 仓库里已有的 `docs/architecture/my-kmp-project.architecture.json` 是**拆模块之前**的旧图：还把 core 画在 app 内部、写着「Ktor 仅 android+ios」。以本页和新的 HTML 为准。
2. `composeApp/src/commonMain/.../MODULES.md:32` 写「core:network ← base」，但 `core/network/build.gradle.kts` 里没有任何 `projects.*` 依赖——`:core:network` 目前并不依赖 `:core:base`（`grep -rln "projects.core.base" feature/*/build.gradle.kts` 也是 0 命中）。这条勘误建议顺手改掉。

## 原样保留的三个数字（便于对账）

- Gradle project 总数：**29**（`settings.gradle.kts:23-44`）
- 版本真值：Gradle wrapper **9.4.1**（`gradle/wrapper/gradle-wrapper.properties:3`）；其余坐标一律看 `gradle/libs.versions.toml`
- API 契约表：**111 个 verb+path / 94 个 path template**（`openspec/changes/sync-flutter-full-parity/notes/api-contract-2026-10-16.md:1-5`）

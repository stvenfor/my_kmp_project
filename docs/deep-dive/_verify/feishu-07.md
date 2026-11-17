# 07 · 速查地图

> 日常开发时最常翻的一页：模块依赖、关键文件与行数、命令、ADR、文档入口。

## 分层与依赖方向（硬规则）

```
:composeApp        ① 装配层：app/ 壳 + 三端入口 + RouteHost 分发；唯一二进制产出方
   ↓ 可以看见全部 feature
:feature:*         ② 业务面（17 个）—— 不依赖其它 feature，不依赖 :composeApp
:component         ③ 纵向端口层：pay / push / chat / webview / auth / routing / realtime
:core:*            ④ 横向 commons（10 个）
iosApp  harmonyApp ⑤ 平台薄壳（Swift / ArkTS），只 host 共享 UI
```

跨功能跳转只在 `:composeApp` 的 `app/AppCrossFeatureScreens.kt` / `app/ShellBootstrap.kt` 接线。

## 模块与 api 依赖（实测自各 `build.gradle.kts`）

| 模块 | api 依赖 | 内容 |
|-|-|-|
| `:core:base` | — | 文本 / 日期 / HtmlUtils、`core.uuid`（CPF uuid，ohosX64 stub） |
| `:core:analytics` | — | 埋点事件模型与门面 |
| `:core:storage` | — | `KeyValueStore` 唯一 KV 契约 + settings(Android/iOS) + `OH_Preferences`(两 arch) |
| `:core:database` | — | `Database` / `Table` 契约 + 内存 driver + `DatabaseProvider` 缝（平台 SQL driver 待 OHOS klib，已登记 gap） |
| `:core:network` | base | HTTP 门面 / 信封 / 分页 / 错误码、`core.sse`、realtime WS 帧与配置 |
| `:core:router` | — | `AppRoutePath`、路由常量与别名表、Navigator 契约 |
| `:core:account` | network, storage | `AccountFacade` + session / 隐私同意 store |
| `:core:design` | — | `DemoColors` / `MineTopBar` / `Immersive*` + **全部 171 个 composeResources**（`publicResClass`，包名 `my_kmp_project.core.design.generated.resources`） |
| `:core:platform` | account, design, storage | `Platform` expect/actual、CameraX / MLKit 扫码、WebView 契约 |
| `:core:ui` | platform | Coil 网络图（`imageCoilMain`）、通用 Compose 件 |
| `:component` | network, base, account, router | 端口 + 平台实现（自己声明全套 target 与公共依赖） |
| `:feature:*` | 见各自 `build.gradle.kts`，按需最小化 | 业务 UI + 契约 + 单测 |
| `:feature:pay:feature:wallet` | network | 纯 HTTP / 模型，**不挂 compose 插件** |
| `:composeApp` | 上述全部 + core.analytics | `App.ktPlatform.ktapp/androidMain/nativeshell`、iOS/OHOS 入口、OHOS cinterop |

feature 17 个：`homemineauthshellchatfriendcommunitymediaclassroomcommercecontentlivescanwebaipaywallet`。

## 关键文件速查（含实测行数）

| 路径 | 行 | 用途 |
|-|-|-|
| `composeApp/src/commonMain/…/App.kt` | 15 | Compose 入口 |
| `composeApp/src/commonMain/…/MODULES.md` | 76 | 29 模块真实依赖表 |
| `composeApp/src/commonMain/…/app/AppShell.kt` | 488 | RouteHost 分发主现场 |
| `composeApp/src/commonMain/…/app/AppContainer.kt` | 57 | 依赖装配 |
| `composeApp/src/commonMain/…/app/AppCrossFeatureScreens.kt` | 108 | 跨功能接线唯一位置 |
| `composeApp/src/androidMain/…/MainActivity.kt` | 68 | splash → edge-to-edge → setContent |
| `composeApp/src/iosMain/…/MainViewController.kt` | 142 | `ComposeUIViewController` |
| `composeApp/src/ohosMain/…/MainArkUIViewController.kt` | 326 | ArkUI 入口，符号须稳定 |
| `composeApp/src/androidMain/…/nativeshell/NativeJetpackTabs.kt` | 1292 | Jetpack 原生 Tab 岛 |
| `core/router/…/FeatureRoutes.kt` | 244 | 路由常量 + 别名表（`:93-107`、`:146-150`） |
| `core/network/…/Sse.kt` | 281 | SSE 传输契约 |
| `core/network/…/KtorApiClient.kt` | 266 | Ktor 实现（`networkKtorMain`） |
| `core/network/…/OhosHttpTransport.kt` | 291 | ohosX64 `net_http` 回落 |
| `core/network/…/core/sse/SseParser.ktSseFrame.ktAiSseDecoder.kt` | — | SSE 纯逻辑 |
| `component/…/realtime/`（13 个文件） | 2141 | 状态机 / 心跳 / 重连 / seq / 去重 / 队列 / mock |
| `component/…/pay/PayGateway.kt` | — | 支付端口（sandbox 复核项 M5.5） |
| `component/…/chat/ChatPort.ktImEngine.kt` | — | IM 端口（真融云在 M11） |
| `feature/shell/…/SecondaryRouteIsland.kt` | — | 二级页 Compose 岛 |
| `feature/ai/…/AiSseApi.kt` (85) `AiStreamScreen.kt` (81) | — | AI 侧（M3.3 待接线） |

## 命令速查

```bash
# 快速
./gradlew :composeApp:assembleDebug -PandroidOnly=true
bash scripts/verify-parity.sh --static-only
bash scripts/with-gradle-lock.sh ./gradlew :feature:home:testDebugUnitTest

# 权威（约 7 分钟）
bash scripts/verify-parity.sh

# 门禁单独跑
bash scripts/check-layer-deps.sh
bash scripts/check-ios-page-dispatch.sh
bash scripts/check-harmony-pane-dispatch.sh
bash scripts/audit-flutter-drift.sh

# 三端运行
./scripts/run-android.sh   ./scripts/run-ios.sh   ./scripts/run-harmony.sh

# OHOS 产物投递（改共享代码后必做）
./gradlew :composeApp:publishDebugBinariesToHarmonyApp
```

有用开关：`-PandroidOnly=true`（跳 native，迭代快）、`-PharmonyAppPath=/absolute/path`（覆盖 Harmony 输出根，默认仓库 `harmonyApp/`）。`gradle.properties` 里 `rendererBackend=fusion-renderer` 决定 OHOS 链接参数，除非有意切换否则不要动。

## ADR 索引

| ADR | 主题 |
|-|-|
| 0001 | 生产化架构（分层与模块化的起点） |
| 0002 | 原生壳 + Mine Compose 岛的混合形态 |
| 0003 | 全量 parity 阶段划分 |
| 0004 | Kotlin 自有 BFF 的 JSON 约定 |
| 0005 | 三条长连接各自独立传输（AI=SSE，realtime=WS，chat=ChatPort），失败语义不得互译 |

## 文档入口

| 内容 | 路径 |
|-|-|
| Agent 指南（必读） | `AGENTS.md` |
| 模块图 | `composeApp/src/commonMain/…/MODULES.md` |
| 架构图（可交互） | `docs/architecture/my-kmp-project.architecture.html`、`docs/architecture/ohos-aggregate.md` |
| 本轮任务清单 | `openspec/changes/sync-flutter-full-parity/tasks-ultra-fine-v2.md` |
| 端点契约表 | `openspec/changes/sync-flutter-full-parity/notes/api-contract-2026-10-16.md` |
| SoT 基线 HEAD | `openspec/changes/sync-flutter-full-parity/notes/flutter-baseline.md` |
| 路由清单 | `openspec/changes/sync-flutter-full-parity/notes/route-inventory.md` |
| 测试证据 | `openspec/changes/sync-flutter-full-parity/notes/evidence/unit-tests-2026-10-17.md` |
| gap 登记 | `openspec/changes/parity-flutter-to-kmp/notes/platform-gap-registry.md` |
| 稳定契约 | `openspec/specs/`（12 个能力域） |

## 规模基线（便于发现异常膨胀）

| 指标 | 数值 |
|-|-|
| Gradle 工程 | 29 |
| `.kt` 文件 | 359 |
| `*Test.kt` | 62 |
| `build.gradle.kts` | 31 |
| Swift / ArkTS 文件（薄壳） | 7 / 21 |
| 原生分发页面 | iOS 65 页、ArkTS 65 页（名字 1:1） |
| 单测 | 660 tests / 68 类 / 0 失败（21 模块，2026-10-17） |

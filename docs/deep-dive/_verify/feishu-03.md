# 03 · 前置知识与分层骨架

## 第一部分：够用即可的前置知识

不要通读 KMP 官方文档。按这张清单自查，缺哪块补哪块。

| 知识点 | 项目里的对应物 | 掌握标准 |
|-|-|-|
| Kotlin：`expect`/`actual`、`internal` 可见性、`object` / `sealed` | `Platform.kt` 三端 actual；`core/platform/` 全部 | 能解释为什么 `internal` 跨模块必红 |
| 源集层级：`commonMain` → 中间源集 → 平台源集 | `networkKtorMain` / `imageCoilMain` / `uuidCpfMain` / `settingsMain` / `chartsVicoMain` | 能手画 dependsOn 图 |
| Compose Multiplatform：`Modifier` 与 `modifier`、`WindowInsets`、`Res.drawable` | `feature/shell/SecondaryRouteIsland.kt`；`AGENTS.md` 沉浸式章节 | 能解释为什么禁裸 `TopAppBar` |
| kotlinx.serialization：`JsonElement`、`JsonNull`、`explicitNulls=false` | `core/network/ApiEnvelopeParser.kt` | 能解释 §5.14「null 不受 explicitNulls 影响」的结论 |
| 协程：`Flow`、`StateFlow`、时钟注入做可测性 | `component/realtime/RealtimeScheduler.kt`、`HeartbeatScheduler.kt` | 能解释心跳为什么不能直接 `delay` |
| Gradle：catalog 别名、`implementation` 与 `api` 的差别、`-P` 属性 | `settings.gradle.kts`、根 `subprojects {}` | 能解释 `api(projects.core.network)` 让 feature 少写哪一行 |

## 第二部分：五层结构

```
:composeApp      ①装配层：AppContainer / AppShell / RouteHost 分发 + 三端入口 + android nativeshell
:feature:*       ②业务层：17 个模块，互不依赖（门禁强制），可单独测试
:component       ③端口层：pay / push / chat / auth / realtime / routing / webview 抽象
:core:*          ④水平公共层：base network router account design platform ui analytics storage database
:ohosAggregate   ⑤占位（理解即可，不产东西）
```

依赖只能向下。`scripts/check-layer-deps.sh` 拦三件事：

1. feature → feature（同级横向依赖）
2. 任何向上 import（feature → composeApp、core → feature 等）
3. `component/build.gradle.kts` 里出现 `projects.feature.*`

`app/` 是唯一允许看见全部 feature 的装配层，`composeApp/build.gradle.kts` 里那 17 行 `implementation(projects.feature.*)` 就是跨功能接线的唯一位置。

## 模块清单

| 层 | 模块 |
|-|-|
| 装配 | `:composeApp`（唯一二进制产出方） |
| core 10 | `basenetworkrouteraccountdesignplatformuianalyticsstoragedatabase` |
| 端口 | `:component` |
| feature 17 | `homemineauthshellchatfriendcommunitymediaclassroomcommercecontentlivescanwebaipaywallet` |
| 占位 | `:ohosAggregate` |

新增一个 `core:*` 或 `feature:*` 模块只需要两处：`settings.gradle.kts` 的 include + 该模块自己的 `build.gradle.kts`（plugins 块 + `api(projects…)` + `android { namespace }`）。target 集、Android 基础配置、iosMain/ohosMain 层级、coroutines/atomicFu/serialization/kotlin-test 全部由根 `subprojects {}` 供给。

注意 `:component` 的路径不在 `:core:` / `:feature:` 命中范围内，所以它自己要把 target 集和公共依赖写全。

## 第三部分：跟读顺序（从入口往下追）

| 序号 | 文件 | 行数 | 要看什么 |
|-|-|-|-|
| 1 | `composeApp/src/commonMain/…/App.kt` | 15 | 入口，找到 `DemoApp` |
| 2 | `composeApp/src/commonMain/…/app/AppShell.kt` | 488 | 本层最厚文件：RouteHost 如何把路由分给各 feature |
| 3 | `composeApp/src/commonMain/…/app/AppContainer.kt` | 57 | 依赖装配，「谁 new 谁」的唯一真相 |
| 4 | `core/router/…/FeatureRoutes.kt:93-107` 与 `:146-150` | — | 路由常量 + 别名表，三端分发的上游 |
| 5 | `core/router/…/ProductRouteDispatch.kt:42-54` | — | host 归属与 Toast 兜底 |
| 6 | `composeApp/src/androidMain/…/MainActivity.kt` | 68 | `installSplashScreen()` → `enableEdgeToEdge()` → `setContent` |
| 7 | `composeApp/src/iosMain/…/MainViewController.kt` | 142 | `ComposeUIViewController` 入口 |
| 8 | `composeApp/src/ohosMain/…/MainArkUIViewController.kt` | 326 | ArkUI/N-API 入口，符号必须保持稳定 |
| 9 | `composeApp/src/commonMain/…/MODULES.md` | 76 | 29 模块真实依赖表 + 每模块测试命令 |

三端壳都很薄：`iosApp/` 是托管 framework 的 SwiftUI 壳，`harmonyApp/` 是托管 `libkn.so` 的 ArkTS 壳。业务不放壳里，这一点是硬规矩。

## 第四部分：中间源集（本项目最重要的一类妥协）

判据只有一条：**某个坐标缺某一端的产物，就必须从 `commonMain` 搬出去**，否则那一端链接断裂。

| 源集 | 住在哪个模块 | 覆盖端 | 为什么单独存在 |
|-|-|-|-|
| `networkKtorMain` | `:core:network` | android + ios + ohosArm64 | OHOS arm64 走 Ktor CIO；ohosX64 无 Ktor klib，回落 `net_http` cinterop |
| `imageCoilMain` | `:core:ui` | android + ios + ohosArm64 | Coil 无 ohosX64 产物 → 占位实现 |
| `uuidCpfMain` | `:core:base` | android + ios + ohosArm64 | `com.benasher44:uuid` 无 ohosX64 产物 → stub actual |
| `chartsVicoMain` | `:feature:home` | android + ios | Vico 图表在 OHOS 两端都无产物，`ohosMain` 不进这个源集 |
| `settingsMain` | `:core:storage` | android + ios | `multiplatform-settings` 无法同时服务 Android 与 OHOS：stock `1.3.0` 没有 ohos klib，CPF `-0.3.0` 线没有 `multiplatform-settings-android` AAR（404） |

OHOS 侧 KV 不依赖 settings：两个 arch 都直接实现 `OH_Preferences` NDK（`platform.ArkData.Preferences`，链接 `-lohpreferences`），`/tmp` 回落实现已删除。

## 第五部分：Compose 硬约束（改 UI 前必读）

全应用沉浸式，内容可延伸到状态栏与系统导航栏下方，但可点击控件必须避开系统 insets。

| 用法 | 组件 |
|-|-|
| 居中顶栏 | `core.design.ImmersiveCenterTopAppBar` |
| 左对齐顶栏 | `core.design.ImmersiveTopAppBar` |
| 二级页通用顶栏 | `core.design.MineTopBar` |

禁止事项：

- 不要用裸 `TopAppBar` / `CenterAlignedTopAppBar`（状态栏会白条）
- 不要在 `DemoApp` tab 内容外包 `statusBarsPadding()`
- 不要在二级页根布局重复 `navigationBarsPadding()`（Shell 已处理）
- 不要用第二个 Scaffold 再塞底栏

`MainBottomBar` 是 tab-root 专属；二级目标必须调 `ReportMainTabRoot(isRoot = false)`。  
`ImmersiveInsets.MainBottomBarHeight` = 49.dp（对齐 Flutter `NavigationBarTheme` height）。

`Modifier` 与 `modifier` 是两回事：前者是类型与工厂对象（`Modifier.fillMaxSize()`），后者只是参数名（`fun Foo(modifier: Modifier = Modifier)` 内部才用 `modifier.fillMaxWidth()`）。写反就是编译红。

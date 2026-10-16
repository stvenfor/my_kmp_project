# Module map (28 Gradle projects)

历史：`composeApp` 曾经是唯一的 KMP 模块，`core/*` `feature/*` 只是包名。2026-10-16 起按
「feature 与 core 同级、feature 之间不得互相依赖、单个 feature 可独立测试」拆成真模块。
OHOS 仍只有一条 `libkn.so`：二进制的产出方**只有** `:composeApp`（`:ohosAggregate` 是占位）。
另见 `docs/adr/0001-production-architecture.md`、`docs/architecture/ohos-aggregate.md`、
`openspec/changes/sync-flutter-full-parity/tasks-ultra-fine-v2.md` 的 P 段。

## 分层与依赖方向（硬规则，`scripts/check-layer-deps.sh` 机器把关）

```
:composeApp        ① 装配层：app/ 壳 + 三端入口 + RouteHost 分发；唯一二进制产出方
   ↓ 可以看见全部 feature
:feature:*         ② 业务面（17 个）——↛ 其它 feature，↛ :composeApp
:component         ③ 纵向桥（push / webview / pay / chat / realtime / routing / auth 端口）
:core:*            ④ 横向 commons（9 个）
iosApp harmonyApp  ⑤ 平台薄壳（Swift / ArkTS），只 host 共享 UI
```

- `component` / `core` **永不** import `feature.*` 或 `app.*`，`component/build.gradle.kts` 里
  也**不许**出现 `projects.feature.*`（门禁第 3c 条）。
- 跨功能跳转只在 `:composeApp` 的 `app/AppCrossFeatureScreens.kt` / `ShellBootstrap.kt` 接线。

## Gradle 模块与依赖（实测自各模块 `build.gradle.kts`）

| 模块 | 依赖（`api`） | 内容 |
|---|---|---|
| `:core:base` | — | 文本/日期/HtmlUtils、`core.uuid`（CPF uuid，ohosX64 stub） |
| `:core:analytics` | — | 埋点事件模型与门面 |
| `:core:storage` | — | `KeyValueStore` 唯一 KV 契约 + settings(Android/iOS) + `OH_Preferences`(两 arch) |
| `:core:network` | base | HTTP 门面 / 信封 / 分页 / 错误码、`core.sse`、realtime WS 帧与配置 |
| `:core:router` | — | `AppRoutePath`、路由常量表、Navigator 契约 |
| `:core:account` | network, storage | `AccountFacade` + session / 隐私同意 store |
| `:core:design` | — | `DemoColors`/`MineTopBar`/`Immersive*` 设计件 + **全部 171 个 composeResources**（`publicResClass`，包名 `my_kmp_project.core.design.generated.resources`） |
| `:core:platform` | account, design, storage | `Platform` expect/actual、相机 / CameraX / MLKit 扫码、WebView 契约 |
| `:core:ui` | platform | Coil 网络图（`imageCoilMain`）、通用 Compose 件 |
| `:component` | network, base, account, router | pay / push / chat / webview / auth / routing / realtime 端口 + 平台实现 |
| `:feature:{home,mine,auth,shell,chat,friend,community,media,classroom,commerce,content,live,scan,web,ai}` | 见各自 `build.gradle.kts`，按需最小化 | 业务 UI + 契约 + 单测 |
| `:feature:{pay,wallet}` | network | **纯 HTTP/模型，不挂 compose 插件**（挂了会因 classpath 上没有 compose runtime 直接编译失败） |
| `:composeApp` | 上述全部 + core.analytics | `App.kt` `Platform.kt` `app/` `androidMain/nativeshell` iOS/OHOS 入口、OHOS cinterop |

共享接线（target 集、android 基础配置、iosMain/ohosMain 层级、coroutines/atomicfu/serialization/kotlin-test）
写在根 `build.gradle.kts` 的 `subprojects {}` 里，命中 `:core:*` 与 `:feature:*`；
`:component` 自己声明全套（它是 app 层与 feature 层之间的特例）。
**buildSrc 在本环境不可用**：`~/.gradle/init.gradle` 把 plugin marker 解析钉在阿里云/eazytec，
`org.gradle.kotlin.kotlin-dsl` 取不到。

## 中间 source set（OHOS 产物缺口专用，别在 commonMain 放平台 API）

| source set | 挂在 | 原因 |
|---|---|---|
| `networkKtorMain` | android + ios + ohosArm64 | Ktor；ohosX64 走 `net_http` cinterop |
| `imageCoilMain` | android + ios + ohosArm64 | CPF Coil；ohosX64 占位 |
| `uuidCpfMain` | android + ios + ohosArm64 | CPF uuid；ohosX64 stub |
| `chartsVicoMain` | android + ios | Vico 图表（现属 `:feature:home`）；OHOS 无产物 |
| `settingsMain` | android + ios | `multiplatform-settings`（OHOS 直接实现 `KeyValueStore`） |

## 单模块独立测试

```bash
./gradlew :feature:home:testDebugUnitTest        # 只编 home + 它的下游
bash scripts/verify-parity.sh --android-only      # 全模块 android + 单测
bash scripts/verify-parity.sh                     # 五目标 + 单测 + JUnit 汇总
```

门禁任务名一律**不带冒号**（＝每个有该任务的子工程都跑）。写死 `:composeApp:` 会让新模块的
编译与单测静默漏检——这就是历史上「绿灯但其实没跑」的根因。

## 拆模块时会踩的两个坑

1. **`internal` 跨不过模块边界**：下沉一个包之前，先把它对外暴露的符号改 public（expect 与 actual
   两侧必须一起改）。反向的坑同样存在：测试里的假 client 不该被顺手改 public，否则会暴露
   internal 类型（`'public' property exposes its 'internal' type`）。
2. **`composeCompiler` 需要 classpath 上有 compose runtime**：没有 `@Composable` 的模块不要挂
   compose 插件。

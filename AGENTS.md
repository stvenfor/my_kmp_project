# AGENTS.md

Agent guide for **My_kmp_project** — Kotlin Multiplatform + Compose Multiplatform demo targeting Android, iOS, and HarmonyOS (OHOS).

Versions, plugins, and dependency coordinates live in `gradle/libs.versions.toml`. Read that file; do not restate versions here.

## Map

| Path | Role |
|------|------|
| `composeApp/` | Shared KMP module: UI + logic + platform actuals |
| `composeApp/src/commonMain/.../app/` | `DemoApp` shell (Home + Mine tabs) |
| `composeApp/src/commonMain/.../core/` | router, account, network, design, platform, ui |
| `composeApp/src/commonMain/.../feature/` | `home`, `mine`, `shell` |
| `composeApp/src/androidMain/` | Android actuals + `MainActivity` |
| `composeApp/src/iosMain/` | iOS actuals + `MainViewController` |
| `composeApp/src/ohosMain/` | OHOS actuals + `MainArkUIViewController` + cinterop |
| `composeApp/src/commonTest/` | Shared tests |
| `iosApp/` | Thin SwiftUI shell hosting the Compose framework |
| `harmonyApp/` | Thin ArkTS/Harmony shell hosting `libkn.so` |
| `gradle/libs.versions.toml` | Version catalog (single source of truth) |
| `local.properties` | Machine-local SDK paths — never commit |

**Intermediate source sets (avoid unsupported OHOS artifacts in `commonMain`):**

| Source set | Used by | Purpose |
|------------|---------|---------|
| `networkKtorMain` | android + ios | Ktor client |
| `accountSettingsMain` | android + ios | `multiplatform-settings` session KV |

Package root: `com.example.my_kmp_project`.

Maven/plugins resolve from `https://maven.eazytec-cloud.com/nexus/repository/maven-public/` (see `settings.gradle.kts`). This fork includes OHOS Kotlin/Compose artifacts; treat the catalog as authoritative.

## Placement

**common-first.** Put code in `commonMain` unless it needs a platform API.

| Need | Put it in |
|------|-----------|
| Shared UI / domain / expect API | `commonMain` |
| Android-only API or Activity entry | `androidMain` |
| UIKit / ComposeUIViewController entry | `iosMain` |
| ArkUI / N-API / HiLog / cinterop | `ohosMain` |
| Shared unit test | `commonTest` |

Use `expect` / `actual` for platform splits. Keep shells thin: `iosApp` and `harmonyApp` host the shared UI; feature work belongs in `composeApp`.

Compose resources live under `composeApp/src/commonMain/composeResources/`.

### Launch icon & splash assets

| Asset | Location |
|-------|----------|
| Launcher (mdpi…xxxhdpi + adaptive) | `composeApp/.../mipmap-*`, `drawable-nodpi`/`drawable-xxhdpi/ic_launcher_foreground.png`, `values/colors.xml` |
| Android 12+ splash | `Theme.DemoApp.Splash` + `ic_splash_icon`; `MainActivity` calls `installSplashScreen()`; `postSplashScreenTheme` → `Theme.DemoApp` |
| Pre-12 cold start | `drawable/splash_screen.xml` as `windowBackground` |
| Shared splash drawables | `composeResources/drawable/{ic_splash_logo,bg_splash}.png` |
| Harmony app icon | **`AppScope`** + `entry` `media/{foreground,background}.png` |
| Harmony start window | `entry/.../media/startIcon.png` |
| iOS | `iosApp/.../AppIcon.appiconset/` |

After icon changes, **uninstall/reinstall** if the launcher shows a cached icon.

## Build & run

Always use the project wrappers (`./gradlew`), not a system Gradle.

| Goal | Command / action |
|------|------------------|
| Android debug | `./gradlew :composeApp:assembleDebug` or Run in IDE |
| Shared tests | `./gradlew :composeApp:testDebugUnitTest` / commonTest tasks |
| iOS | Open `iosApp/iosApp.xcodeproj` in Xcode after a Gradle sync that produces the `ComposeApp` framework |
| OHOS libs → Harmony | `./gradlew :composeApp:publishDebugBinariesToHarmonyApp` |
| Harmony app | Build/run `harmonyApp/` in DevEco Studio after publishing binaries |

Optional: `-PharmonyAppPath=/absolute/path` overrides the Harmony output root (default: repo `harmonyApp/`).

`gradle.properties` sets `rendererBackend=fusion-renderer` for OHOS linking. Change it only when intentionally switching renderers; linker opts in `composeApp/build.gradle.kts` follow this flag.

## OHOS publish contract

`publish*BinariesToHarmonyApp` copies:

- `libkn.so` + `libkn_api.h` → `harmonyApp/entry/libs/{arm64-v8a,x86_64}/` and matching `cpp/include/`
- Compose resources → `harmonyApp/entry/src/main/resources/rawfile/composeResources/...`

After changing Kotlin/Compose shared code for Harmony, publish again before DevEco run. ArkTS entry (`harmonyApp/entry/.../Index.ets`) loads `MainArkUIViewController` from the native module — keep that symbol stable unless you update both sides.

Release OHOS links set `optimized = false` to avoid OOM in DevirtualizationAnalysis; leave that unless you have measured headroom.

OHOS HTTP uses cinterop (`demo_net_http_*` + `libdemo_net_http.a`), not Ktor.

## Conventions

- Prefer catalog aliases (`libs.*`, `libs.plugins.*`) over hard-coded coordinates.
- Match existing naming: `Platform` expect/actual, `App()` composable, platform entrypoints as above.
- Keep changes scoped: one concern per edit; reuse existing patterns in neighboring files.
- New dependencies: add to `gradle/libs.versions.toml`, then reference from the right source set in `composeApp/build.gradle.kts`.

## Compose

Any new or edited `@Composable` / Compose UI must use **valid Jetpack Compose / Compose Multiplatform Kotlin APIs** and compile. Copy patterns from compiling neighbors in the same feature; do not invent Flutter/SwiftUI-shaped APIs or free-form pseudo-Compose.

### Immersive / edge-to-edge（required）

全应用 **沉浸式**：内容可延伸至**状态栏**与**系统导航栏**下方；**可点击的控件**必须避开系统 insets。

#### 平台入口（必须先配）

| 平台 | 配置 |
|------|------|
| **Android** | `MainActivity`: `installSplashScreen()` → `enableEdgeToEdge()` → `setContent`；`Theme.DemoApp` 中 `statusBarColor` / `navigationBarColor` 透明 |
| **iOS** | `iosApp/.../ContentView.swift`: `ComposeView().ignoresSafeArea(.all)` |
| **Shell** | `DemoApp` Scaffold: `containerColor = Color.Transparent`，`contentWindowInsets = WindowInsets(0)` |

#### 顶栏

**禁止**直接使用裸 `TopAppBar` / `CenterAlignedTopAppBar`（状态栏会白条）。

| 用法 | 组件 |
|------|------|
| 居中标题顶栏 | `core.design.ImmersiveCenterTopAppBar` |
| 左对齐顶栏 | `core.design.ImmersiveTopAppBar` |
| 二级页通用顶栏 | `core.design.MineTopBar` |

#### Shell 底栏 inset（`ImmersiveInsets.shellContentInsets`）

| `bottomBarVisible` | 内容区底部 |
|--------------------|------------|
| `true`（Tab 根） | `padding(bottom = MainBottomBar + nav inset)` |
| `false`（二级栈页） | `navigationBarsPadding()` |

常量：`ImmersiveInsets.MainBottomBarHeight` = **56.dp**。

#### 禁止事项

- 不要用裸 `TopAppBar` / `CenterAlignedTopAppBar`。
- 不要在 `DemoApp` tab 内容外包 `statusBarsPadding()`。
- 不要在二级页根布局重复 `navigationBarsPadding()`（Shell 已处理）。
- 不要用第二个 Scaffold 再塞底栏。

### Main bottom bar vs secondary pages

`MainBottomBar` is **tab-root only**. Secondary destinations must call `ReportMainTabRoot(isRoot = false)`.

**`Modifier` vs `Modifier` (hard rule):**

| Form | Meaning | Use |
|------|---------|-----|
| `Modifier` | type + factory object | `Modifier.fillMaxSize()`, `Spacer(Modifier.height(8.dp))` |
| `modifier` | parameter name only | `fun Foo(modifier: Modifier = Modifier)` then `modifier.fillMaxWidth()` |

Wrong: `Spacer(modifier.height(4.dp))` when no such parameter exists.

Resources: `painterResource(Res.drawable.…)` from `composeResources`, not Android `R.drawable` in `commonMain`.

Prefer `DemoColors` design tokens over one-off magic colors when a token already exists.

## Guardrails

- Commit only source and project config. Build caches (`.gradle`, `.kotlin`, `**/build`), `local.properties`, IDE/user state, `oh_modules`, and `.hvigor` stay out of git (see `.gitignore`).
- Treat `local.properties` as private machine config.
- Prefer editing shared Compose in `commonMain` over duplicating UI in Swift/ArkTS shells.
- When touching OHOS native packaging, update publish paths and ArkTS consumers together.

## Done when

A change is complete when:

1. Code sits in the correct source set.
2. Catalog/Gradle wiring matches the placement.
3. Relevant target builds (or the narrowest compile task for that target) succeed.
4. Compose edits follow the **Compose** section (valid APIs; `Modifier`/`modifier`; immersive insets).
5. If Harmony-facing Kotlin changed: binaries published and shell still resolves `MainArkUIViewController`.

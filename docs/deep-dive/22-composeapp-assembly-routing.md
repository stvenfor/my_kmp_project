# 22 · composeApp 装配层与路由跳转全景

> 分册覆盖 **① 装配层**：`composeApp/src/commonMain/` 全部 kt（App.kt / Platform.kt / app/ 五件套）、`composeApp/build.gradle.kts` 依赖声明段、`composeApp/src/commonTest/`，以及三端入口（androidMain/nativeshell、iosMain、ohosMain）中与路由相关的接线。
> 每条事实均附仓库相对路径行号；全部来自整篇源码阅读，未跑 Gradle/build。
> 路由链路（第 5 节）写到可照着复现：点击 → 解析 → 分发 → 目标屏 → 三端各自承载。

---

## 1. 装配层职责边界

### 1.1 「只装配、不写业务」的落点

| 职责 | 实证 |
|------|------|
| 组合根（composition root） | `composeApp/src/commonMain/kotlin/com.example.my_kmp_project/app/AppContainer.kt:18-28`，注释自称 Spike I 组合根，计划迁移 Koin（ADR 0001） |
| Legacy 共享 Compose 壳（splash→隐私→四 Tab） | `app/AppShell.kt:99-130`；文件头注释 L93-98 明确「LEGACY — 运行时主路径是 Android NativeAndroidApp / iOS SwiftUI / Harmony ArkTS」 |
| 跨功能屏渲染表（app→feature 唯一合法接线处） | `app/AppCrossFeatureScreens.kt:21-28` 注释 + `MODULES.md:22` |
| 装机入口（会话水合 + 端口安装） | `app/ShellBootstrap.kt:7-26` |
| 唯一二进制产出方 | `composeApp/build.gradle.kts:44-48`（iOS framework `ComposeApp`，isStatic）、`:51-92`（OHOS sharedLib `libkn.so`）、`:259-300`（publish 任务）；`:ohosAggregate` 是占位（`ohosAggregate/build.gradle.kts:1-13`） |
| 三端入口 + OHOS cinterop | `androidMain/.../MainActivity.kt:21-63`、`iosMain/.../MainViewController.kt`、`ohosMain/.../MainArkUIViewController.kt`；cinterop `composeApp/build.gradle.kts:93-108`（resource.def / avplayer.def） |

装配层**不含任何业务屏实现**：`commonMain` 只有 7 个 kt（见第 2 节），业务 UI 全在 `:feature:*`；能力端口全在 `:component` / `:core:*`。

### 1.2 项目级依赖逐条（`composeApp/build.gradle.kts` commonMain.dependencies）

`:component`（1 个）：

| 依赖 | 行号 |
|------|------|
| `implementation(projects.component)` | `composeApp/build.gradle.kts:121` |

`:core:*`——**注意：实际是 9 个，不是 10 个**（`:core:database` 没有被 composeApp 依赖，见 §8.2 勘误 E2）：

| 依赖 | 行号 |
|------|------|
| `implementation(projects.core.network)` | `:122` |
| `implementation(projects.core.account)` | `:123` |
| `implementation(projects.core.router)` | `:124` |
| `implementation(projects.core.design)` | `:125` |
| `implementation(projects.core.base)` | `:126` |
| `implementation(projects.core.analytics)` | `:127` |
| `implementation(projects.core.storage)` | `:128` |
| `implementation(projects.core.platform)` | `:129` |
| `implementation(projects.core.ui)` | `:130` |

`:feature:*` 全部 17 个（L131-132 注释：「app/ 是唯一允许看见全部 feature 的装配层；跨功能跳转只能在这里接线」）：

| 依赖 | 行号 | 依赖 | 行号 |
|------|------|------|------|
| `projects.feature.ai` | `:133` | `projects.feature.media` | `:143` |
| `projects.feature.auth` | `:134` | `projects.feature.mine` | `:144` |
| `projects.feature.chat` | `:135` | `projects.feature.pay` | `:145` |
| `projects.feature.classroom` | `:136` | `projects.feature.scan` | `:146` |
| `projects.feature.commerce` | `:137` | `projects.feature.shell` | `:147` |
| `projects.feature.community` | `:138` | `projects.feature.wallet` | `:148` |
| `projects.feature.content` | `:139` | `projects.feature.web` | `:149` |
| `projects.feature.friend` | `:140` | `projects.feature.home` | `:141` |
| `projects.feature.live` | `:142` | | |

其他声明：compose 运行时族 `:150-156`、coroutines `:157`、atomicfu `:158`、serialization-json `:159`；androidMain `:116-119`（activity-compose、core-splashscreen）；ohosMain `:172-174`（`api(libs.compose.multiplatform.export)`）；commonTest/androidUnitTest `:183-188`；根级 `debugImplementation(libs.compose.ui.tooling)` `:222-224`。

`build.gradle.kts:112-115` 注释明确：**能力型依赖已全部下沉**（uuid→core:base、Coil→core:ui、CameraX/MLKit→core:platform、Vico→feature:home），`:composeApp` 不再创建任何中间 source set——但源码树里仍残留一个空的 `composeApp/src/chartsVicoMain/`（0 个 kt，见勘误 E5）。

分层门禁由 `scripts/check-layer-deps.sh` 机器把关：feature↛feature（第 1 轴，L18-28）、core/component ↛ feature/app（第 2 轴，L31-40）、Gradle 层 feature/core/component 不得 `projects.feature.*`（第 3/3b/3c 轴，L42-64）。composeApp 自身不在被禁名单里，它是唯一能 `import feature.*` 的模块。

---

## 2. 文件清单表（commonMain 全部 7 个 kt + 三端入口）

| 路径（仓库相对） | 行数 | 作用 | 关键行号 |
|---|---|---|---|
| `composeApp/src/commonMain/kotlin/com.example.my_kmp_project/App.kt` | 15 | Legacy 入口别名：取容器 + MaterialTheme + AppShell | 8-15 |
| `composeApp/src/commonMain/kotlin/com.example.my_kmp_project/Platform.kt` | 8 | `Platform` expect/actual 契约（脚手架残留，无人调用） | 3-7 |
| `.../app/AppContainer.kt` | 57 | 组合根：4 个分组子组件 | 18-57 |
| `.../app/AppCrossFeatureScreens.kt` | 108 | `CrossFeatureScreenPort` 装配层实现（3 张路由表 + 门店弹窗） | 29-108 |
| `.../app/AppShell.kt` | 488 | Legacy 共享壳：splash/隐私/四 Tab/overlay/deep-link | 79-488 |
| `.../app/DemoApp.kt` | 12 | `DemoApp()` → `AppShell()` 兼容别名（当前无调用方） | 9-12 |
| `.../app/ShellBootstrap.kt` | 26 | 幂等装机：AuthSessionBootstrap + CrossFeatureScreens | 15-26 |
| `composeApp/src/commonTest/kotlin/com.example.my_kmp_project/ComposeAppCommonTest.kt` | 11 | 唯一测试：脚手架 `1+2=3` | 6-11 |
| `composeApp/src/androidMain/.../MainActivity.kt` | 69 | Android 唯一 Activity 入口 | 21-63 |
| `composeApp/src/androidMain/.../nativeshell/NativeAndroidApp.kt` | 151 | Android 运行时主路径：Splash/Privacy/Main 三相 | 51-74 |
| `composeApp/src/androidMain/.../nativeshell/NativeAndroidMain.kt` | 794 | **Android 路由中枢**：Tab+8 类 overlay+deep-link 消费 | 85-758 |
| `composeApp/src/androidMain/.../nativeshell/NativeJetpackHome.kt` | 227 | Jetpack 首页宿主（按钮→openDeferred） | 57-211 |
| `composeApp/src/androidMain/.../nativeshell/NativeJetpackTabs.kt` | 1292 | Jetpack Chat/Community/Mine Tab 根 | 86 / 840 / 1267 |
| `composeApp/src/iosMain/.../MainViewController.kt` | 143 | iOS 导出函数集：3 个 VC 工厂 + deep-link + AuthBridge 门面 | 17-142 |
| `composeApp/src/ohosMain/.../MainArkUIViewController.kt` | 327 | OHOS `@CName` N-API 导出：岛容器 + 宿主参数 + Auth | 34-315 |
| `composeApp/src/{android,ios,ohos}Main/.../Platform.*.kt` | 9/9/12 | `getPlatform()` actual（无调用方） | android 4-9 / ios 4-8 / ohos 6-11 |

---

## 3. 逐文件逐函数（commonMain + 三端入口）

### 3.1 `App.kt`

| 签名 | 可见性 | 行为 | 副作用 | 被谁调用 |
|------|--------|------|--------|----------|
| `@Composable internal fun App()`（`App.kt:8-15`） | internal | L11 `AppContainer.get()` 提前触碰组合根（注释：Spike I）；L12-14 `MaterialTheme { AppShell() }` | 无（容器是 lazy） | 仅 `composeApp/src/iosMain/.../MainViewController.kt:20`（已 `@Deprecated` 的 `MainViewController()`） |

### 3.2 `Platform.kt`

- `internal interface Platform { val name: String }`（`Platform.kt:3-5`）；`internal expect fun getPlatform(): Platform`（`:7`）。
- actual：Android `"Android ${Build.VERSION.SDK_INT}"`（`Platform.android.kt:4-9`）、iOS UIDevice 拼接（`Platform.ios.kt:4-8`）、OHOS 常量 `"HarmonyOS"`（`Platform.ohos.kt:6-11`）。
- **状态**：全仓 grep 无业务调用方，是模板脚手架残留。

### 3.3 `app/AppContainer.kt`

```
internal class AppContainer(
    val network: NetworkComponent = NetworkComponent(),      // :19
    val session: SessionComponent = SessionComponent(),      // :20
    val bridges: BridgeComponent = BridgeComponent(),        // :21
    val navigation: NavigationComponent = NavigationComponent(), // :22
)
```

- 伴生单例：`private val instance by lazy { AppContainer() }` + `fun get()`（`:24-27`）。类头注释（`:14-17`）：分组子组件避免 god object，Spike I 后迁 Koin（ADR 0001 / Q15=D）。
- `internal class NetworkComponent`（`:30-32`）：`fun api() = NetworkFacade.api()`。**当前无调用方**（全仓仅此处定义）。
- `internal class SessionComponent`（`:34-36`）：空壳，注释说 AuthSessionState 仍是响应式 SoT，未来放 SessionRepository。
- `internal class BridgeComponent`（`:38-43`）：
  - `push: PushBridge = StubPushBridge`（`:39`；契约 `component/push/PushBridge.kt:19`、桩 `:46`）。唯一调用方：`NativeAndroidApp.kt:66-68` 隐私同意后 `runCatching { AppContainer.get().bridges.push.registerForPush() }`。
  - `pay: PayGateway = FlaggedPayGateway()`（`:41`，注释：默认 sandboxEnabled 时走沙箱适配器）。当前装配层无消费方。
  - `chat: ChatPort = StubChatPort`（`:42`）。当前装配层无消费方。
- `internal class NavigationComponent`（`:45-57`）：
  - `fun bind(onNavigate: (AppRoute) -> Unit): AppNavigator`（`:48-52`）：新建 `AppNavigator(onNavigate)` 并缓存。**唯一调用方**：`AppShell.kt:240`。
  - `fun navigatorOrNull(): AppNavigator?`（`:54`）、`fun consumeDeepLink() = DeepLinkRouter.consumePending()`（`:56`）——两者当前无调用方（AppShell 直接调 `DeepLinkRouter.consumePending()`，`AppShell.kt:261`）。

### 3.4 `app/AppCrossFeatureScreens.kt`（装配层跨功能渲染表）

`internal object AppCrossFeatureScreens : CrossFeatureScreenPort`（`:29`）。文件头注释（`:21-28`）：Content 表从旧 `ContentRouteHost` 逐字搬来；Mine/Home 表仍留在各自 feature 里，此处只是转发——纯依赖方向重构。

| 成员 | 行号 | 行为 |
|------|------|------|
| `override fun handlesSiblingScreen(route: String): Boolean` | `:31-34` | `route.startsWith("/classroom")` ∥ `startsWith("/video")` ∥ `== MineRoutes.Membership` |
| `@Composable override fun RenderSiblingScreen(route, onBack, onNavigate)` | `:36-47` | `/classroom*`→`ClassroomRouteHost`（`:43`）；`/video*`→`VideoRouteHost`（`:44`）；否则→`MembershipScreen(onBack)`（`:45`，:feature:commerce） |
| `@Composable override fun ContentRouteScreen(route, onBack, onNavigate)` | `:53-76` | 9 路 when：`ContentRoutes.Live`/`LiveRoom`→`LiveScreen(openRoom)`（`:60-61`）、`Friend`→`FriendScreen`（`:62`）、`Music`→`MusicListScreen(onOpenNowPlaying=onNavigate(MusicNowPlaying))`（`:63-67`）、`MusicNowPlaying`→`MusicNowPlayingRoute`（`:68-69`）、`MediaEntry`→`MediaEntryScreen`（`:70`）、`AiRoutes.Stream`→`AiStreamScreen`（`:71`）、`/classroom*`、`/video*` 转发对应 RouteHost（`:72-73`）、else→`MediaEntryScreen` 兜底（`:74`） |
| `@Composable override fun MineRouteScreen(...)` | `:78-85` | 转发 `MineRouteHost(route, onBack, onNavigate)` |
| `@Composable override fun HomeRouteScreen(...)` | `:87-94` | 转发 `HomeRouteHost(...)` |
| `@Composable override fun SwitchStoreDialog(selectedId, onDismiss, onPicked)` | `:96-107` | 调 `feature.mine.SwitchStoreDialog`（import 别名 `:19`），把 `store` 对象展平为 `(store.id, store.name)` 回调 |

被谁调用：由 `ShellBootstrap.install()` 装入 `CrossFeatureScreens`（`ShellBootstrap.kt:24`）；feature 侧消费点见 §6.2。

### 3.5 `app/AppShell.kt`（Legacy 共享壳，488 行）

私有枚举：`AppPhase { Splash, PrivacyConsent, Main }`（`:79-83`）、`AuthOverlay { None, Login, LoginPassword, LoginOtp, Register }`（`:85-91`）。

**`@Composable internal fun AppShell()`（`:99-130`）**
- `:101` `remember { createPrivacyConsentStore() }`（工厂在 `core/account/PrivacyConsentStore.kt:19-20`）。
- `:102-103` `phase` 初始 `Splash`；`privacyAccepted` 初始读 store。
- `:105-109` `DisposableEffect(Unit)` 调 `ShellBootstrap.install()`（注释：会话水合+401 装机，幂等）。
- `:111-129` 三相分发：`SplashPhase(onFinished = { phase = if (privacyAccepted) Main else PrivacyConsent })`（`:112-118`）；`PrivacyConsentPhase(onAccept = { store.setAccepted(true); phase = Main })`（`:119-127`）；`Main -> MainShell()`（`:128`）。

**`@Composable private fun SplashPhase(onFinished)`（`:132-170`）**
- `:134-137` `LaunchedEffect` 延时 **1_000ms** 后 `onFinished()`（对比 Android 原生壳是 1200ms，`NativeAndroidApp.kt:78-81`）。
- 全屏 `bg_splash`（Crop）+ 居中 `ic_splash_logo` 96dp + 「正在启动…」+ 「进入」TextButton 可跳过（`:138-168`）。资源来自 `:core:design` composeResources（import `:74-77`）。

**`@Composable private fun PrivacyConsentPhase(onAccept)`（`:172-221`）**
- 「隐私政策与用户协议」文案 + 两按钮；「不同意」onClick 为空实现（`:203-208`，注释 `stay — Flutter also blocks without grant`），只有「同意并继续」触发 `onAccept`（`:209-218`）。

**`@Composable private fun MainShell()`（`:223-488`）**——Legacy 壳的路由心脏：

| 局部 | 行号 | 语义 |
|------|------|------|
| `tab` / `authOverlay` / `shellRoute: AppRoute?` / `tabChrome = MainTabChromeController()` / `sessionEpoch` / `loggedIn` | `:225-230` | Tab 态、登录浮层态、单值二级路由态、底栏 chrome、会话快照代际 |
| `fun clearShellRoute()` | `:232-237` | 回根；**Membership 例外**——`restoreBottomBar = shellRoute != AppRoute.Membership`（`:234`），因为 Membership 覆盖在 Mine Settings 之上（二级叠二级） |
| `navigator = remember { AppContainer.get().navigation.bind { ... } }` | `:239-258` | 绑定回调：只接受 `InAppWeb / AllServices / Membership / Scan / Media / Live / Friend / Classroom` 8 类 `AppRoute`（`:241-246`）→ 设 `shellRoute`、清 authOverlay、隐藏底栏（`:250-254`）；其余（Tab/Login/Register/MineAbout/Splash/PrivacyConsent）`else -> Unit` 被丢弃（`:255`） |
| `LaunchedEffect` deep-link 消费 | `:260-302` | `DeepLinkRouter.consumePending()`（`:261`）；LOGIN/loginPassword/loginOtp/register 四类设 authOverlay（`:265-280`）；`/web` → `shellRoute = AppRoute.InAppWeb(webUrlFromDeepLink(pending.rawUri, OfflineWebFixtureUrl))`（`:281-288`）；`pending.tab != null` → 经 `AuthGate.requiresAuth` 软门禁后 `tab = target`（`:289-300`）。注释 `:262`/`:290-292`：`ParsedDeepLink.tab` 是跨模块 public 属性，需局部变量才能智能转换 |
| `fun refreshSession()` | `:304-307` | `AuthSessionState.sync()` + `sessionEpoch += 1` 触发 `loggedIn` 重组 |
| `fun selectTab(next)` | `:309-322` | Chat/Community 未登录 → `AuthGate.rememberPending(next)` + 弹登录（`:310-316`）；否则切 Tab、清 overlay、`AuthGate.clearPending()`、恢复底栏（`:317-321`） |
| `fun afterAuthSuccess()` | `:324-331` | `refreshSession()` → `AuthGate.consumePending() ?: MainTab.Home` 续跳 |
| `showBottomBar` | `:333-336` | `authOverlay==None && shellRoute==null && tabChrome.bottomBarVisible` |
| Provider | `:338-341` | `LocalMainTabChrome provides tabChrome`、`LocalAppNavigator provides navigator` |
| Scaffold | `:342-355` | `containerColor = Transparent`、`contentWindowInsets = WindowInsets(0)`、bottomBar 条件渲染 `MainBottomBar(selected, onSelect=selectTab)`（`feature/shell/MainBottomBar.kt:73-78`，49dp 规格见其注释 `:51-72`） |
| 内容区 | `:356-364` | `shellContentInsets(bottomBarVisible, bottomBarPadding)`（import `core.design.ImmersiveInsets.shellContentInsets`，`:42`） |
| authOverlay 渲染 | `:366-388` | `LoginScreen`/`LoginPasswordScreen`/`LoginOtpScreen`/`RegisterScreen`（均 :feature:auth），onLoginSuccess→`afterAuthSuccess` |
| Tab 内容 | `:389-418` | Home→`HomeScreen(loggedIn, displayName)`（:feature:home 共享版）；Chat→`ChatScreen(onOpenFriends = { shellRoute=AppRoute.Friend; 藏底栏 })`（`:394-399`）；Community→`CommunityScreen()`；Mine→`MineScreen(onLoginClick=…AuthGate.rememberPending(Mine)+弹登录, onLogoutClick=…AuthRepository.logout()+回 Home)`（`:401-417`） |
| shellRoute 渲染（叠在 Tab 内容之上） | `:421-483` | `InAppWeb(url,onBack)`（`:422-425`）、`AllServices`（`:426-428`）、`Membership`（`:429-431`）、`Scan`（`:432-469`，内含扫码→`DeepLinkRouter.accept(payload)` 的再分发：http(s)→InAppWeb `:437-441`、`/web`→`webUrlFromDeepLink` `:442-447`、`tab`→`selectTab` `:448-453`、`/friend` `:454-457`、`/mall\|/pay`→Membership `:458-466`）、`Media`→`MediaEntryScreen`（`:470-472`）、`Live`→`LiveScreen`（`:473-475`）、`Friend`→`FriendScreen`（`:476-478`）、`Classroom`→`ClassroomScreen`（`:479-481`） |

调用方：`App.kt:13` 与 `DemoApp.kt:11`。实际运行时入口只剩 `MainViewController`（deprecated）。

### 3.6 `app/DemoApp.kt` / `app/ShellBootstrap.kt`

- `@Composable internal fun DemoApp() { AppShell() }`（`DemoApp.kt:9-12`）；注释称「保留旧调用点」，但**当前 composeApp 内没有任何调用方**（AGENTS.md 仍把它描述为运行时入口，见勘误 E7）。
- `internal object ShellBootstrap`（`ShellBootstrap.kt:15-26`）：
  - `@Volatile private var installed = false`（`:17-18`）。
  - `fun install()`（`:20-25`）：幂等守卫后依序 `AuthSessionBootstrap.install()` + `CrossFeatureScreens.install(AppCrossFeatureScreens)`。
  - 文件头注释（`:7-14`）声称「从 AppShell、MainActivity、iOS MainViewController 工厂、MainArkUIViewController 调用」——与代码相符，调用点：`AppShell.kt:107`、`MainActivity.kt:35`、`MainViewController.kt:19/30/53/76`、`MainArkUIViewController.kt:41/153`。

### 3.7 `iosMain/kotlin/com.example.my_kmp_project/MainViewController.kt`（143 行，逐函数）

该文件是 CMP 导出给 SwiftUI 的全部符号面（`ComposeApp` framework，`composeApp/build.gradle.kts:44-48`）。共 13 个顶层函数：12 public + 1 private（`ensureIosAuthReady`）。

| 函数 | 行号 | 签名与行为 | 副作用 | Swift 调用方 |
|---|---|---|---|---|
| `MainViewController` | `:16-21` | `@Deprecated("Native SwiftUI shell owns the app; …")`；`:18` `platformNetworkBootstrap()` → `:19` `ShellBootstrap.install()` → `:20` `ComposeUIViewController { App() }`（即 legacy 全 App 壳） | bootstrap+装机 | **无**（iosApp 全目录 grep 仅命中其余符号）；文件头 `:13-15` 自注「Legacy full-app Compose entry — reference only (ADR 0002)」 |
| `MineIslandViewController` | `:28-43` | `(route: String = "settings"): UIViewController`；`:29-30` bootstrap+install；`:31` trim；`:32` `MineIslandRoute.fromPath(key) ?: fromHostKey(key)`（路径优先、壳键兜底且未知键回退 Settings，`core/router/MineIslandRoute.kt:34-45`）；`:33-41` `lateinit var controller` 自引用，`MineIsland(initialRoute, onRequestClose = controller.dismissViewControllerAnimated)` 形成「屏内返回=关 VC」闭环 | 同上 | `iosApp/iosApp/ContentView.swift:500`（`MineIslandHost` representable，struct `:497-503`；fullScreenCover 挂载 `:435-437`） |
| `SecondaryRouteViewController` | `:51-64` | `(routeOrLabel: String): UIViewController`；`:52-53` bootstrap+install；`:54-62` 同款自关闭环包 `SecondaryRouteIsland(initialRoute = routeOrLabel)`。注释 `:45-50` 说明入参可为 Flutter 路径或中文标签，但**岛内部只受理 Mine host**（`feature/shell/SecondaryRouteIsland.kt:34-57`，非 Mine 显示拦截页 `:95-109`） | 同上 | `ContentView.swift:508`（`MineSecondaryHost` `:505-510`） |
| `AcceptDeepLinkFromIos` | `:67-70` | `(uri: String): String?`；`DeepLinkRouter.accept(uri)` 为 null 直接返回 null，否则返回 `parsed.route`（Swift 侧只需知道落点字符串） | 写 `DeepLinkRouter.pendingDeepLink`（`core/router/DeepLinkRouter.kt:36-40`） | `ContentView.swift:122`（`.onOpenURL` `:102-104` 与冷启动 `pending_deeplink`/`-route` `:105-113` 同经此入口） |
| `ensureIosAuthReady`（private） | `:73-77` | `platformNetworkBootstrap()` + `ShellBootstrap.install()`（注释「幂等：401 静默刷新装机 + 会话水合」`:75`） | 同装机 | 下面 8 个 Auth 函数各自首行调用 |
| `AuthIsLoggedIn(): Boolean` | `:79-82` | 转 `AuthBridge.isLoggedIn()`（`feature/auth` 门面） | 装机 | `ContentView.swift:66`（privacy 门条件）、`:416` |
| `AuthDisplayName(): String` | `:84-87` | 转 `AuthBridge.displayName()` | 装机 | `ContentView.swift:67` |
| `AuthLogout()` | `:89-92` | 转 `AuthBridge.logout()` | 装机+清会话 | `ContentView.swift:372` |
| `AuthLoginWithPassword(account, password, onSuccess, onError)` | `:94-102` | 转 `AuthBridge.loginWithPassword(…)`，回调式 | 装机+写会话 | `NativeAuthPages.swift:335` |
| `AuthLoginWithOtp(phone, code, onSuccess, onError)` | `:104-112` | 转 `AuthBridge.loginWithOtp(…)` | 同上 | `NativeAuthPages.swift:348` |
| `AuthSendPhoneOtp(phone, onSuccess, onError)` | `:114-121` | 转 `AuthBridge.sendPhoneOtp(…)` | 同上 | `NativeAuthPages.swift:311, 428` |
| `AuthRegister(email, password, displayName, onSuccess, onError)` | `:123-132` | 转 `AuthBridge.register(…)` | 同上 | `NativeAuthPages.swift:461` |
| `AuthRegisterWithPhone(phone, code, onSuccess, onError)` | `:134-142` | 转 `AuthBridge.registerWithPhone(…)` | 同上 | `NativeAuthPages.swift:475` |

> 结构结论：iOS 侧没有任何「共享 Tab 根」导出——SwiftUI 自己拥有四 Tab（§5.3），CMP 只导出 3 个 VC 工厂（1 个已 deprecated）+ 1 个深链入口 + 8 个 Auth 门面。这就是 ADR 0002「iOS 主路径原生、Compose 只承载岛」在符号层面的全部足迹。

### 3.8 `ohosMain/kotlin/com.example.my_kmp_project/MainArkUIViewController.kt`（327 行，逐导出符号）

符号链共三层：ArkTS `libentry.*`（类型声明 `harmonyApp/entry/src/main/cpp/types/libentry/Index.d.ts:4-5`）→ N-API 桩 `harmonyApp/entry/src/main/cpp/napi_init.cpp`（注册表 `:313-316`，`extern "C"` 声明 `:44-58`）→ Kotlin `@CName` 符号（`libkn.so` 静态符号）。Kotlin 侧共 1 个 VC 工厂 + 2 个宿主意图写入 + 1 个会话同步 + 5 个阻塞 Auth + 4 个字符串拷贝 + 2 个状态 + 1 个空钩子 + 2 个 private + 1 个冒烟常量。

| `@CName` 符号 | 行号 | 行为 | C++/ArkTS 消费点 |
|---|---|---|---|
| （常量）`OhosSmokeUiOnly = false` | `:26` | true 时 `:44-45` 渲染 `OhosSmokeRoot`（private @Composable `:321-326`，一行 Text「KMP Demo OHOS Compose OK」）并跳过 `:38-42` 的 bootstrap+install——纯链接冒烟开关，当前关 | 无 |
| `MainArkUIViewController(env: napi_env): napi_value` | `:32-71` | 唯一 UI 工厂。`:36` println 当前 `OhosComposeHostRequest.mode`；`:37` `initMainHandler(env)`（协程 main handler，阻塞调用前置）；`:39-41` `platformNetworkBootstrap()`+`ShellBootstrap.install()`（注释 `:40`「须早于 ArkTS 侧业务请求」）；`:47-48` `closed` 状态；`:49-53` `mode==1` → `SecondaryRouteIsland(initialRoute = OhosComposeHostRequest.secondaryRoute, onRequestClose={closed=true})`；`:54-57` else → `MineIsland(initialRoute = OhosComposeHostRequest.mineRoute)`；`:59-63` closed 后仅渲染「已关闭」占位（不销毁 controller，由 ArkTS 收起）；`:66-70` catch Throwable → 打栈后 `null as napi_value` | `Index.ets:836,842` → `napi_init.cpp:21-36`（null 时 `:27-33` 记错误日志） |
| `KnSetOhosHost(kind: Int, routeCode: Int)` | `:80-98` | `:83` `mode = kind`；`kind==0` 时 `:85-96` routeCode→`MineIslandRoute`：1 Personalized / 2 Membership / 3 About / 4 Profile / 5 Addresses / 6 Calculator / 7 DealInvoiceDemo / 8 DealInvoiceUpload / 9 AddressEdit / else Settings。注释 `:73-79` 声明 kind=1 忽略 routeCode | `Index.ets:832-834`（openComposeHost 内）→ `napi_init.cpp:60-72` |
| `KnSetOhosSecondaryRoute(routePtr: CPointer<ByteVar>?)` | `:100-106` | `:103` 空指针回退 `"/home/search"`；`:104-105` `mode=1` + 写 `secondaryRoute`。注意：C++ 侧也有一层同名默认串回退（`napi_init.cpp:80-82` `kDefault`），双保险 | `Index.ets:871-872`（openSecondaryCompose 内）→ `napi_init.cpp:76-90` |
| `KnApplyAuthSession(token, userId, displayName, phone ptrs)` | `:112-129` | 头注释 `:108-111`：把 ArkTS **展示态**会话同步进共享 AccountFacade（冷启动缓存）；新登录应走 `KnAuthLogin*`。`:121` blank token 直接 return；`:122` bootstrap；`:123-128` `AuthBridge.applySession`，displayName 空回退「用户」`:126`、phone `takeIf 非空` `:127` | `napi_init.cpp:46, 115` |
| `KnAuthLoginWithPasswordBlocking(account, password, errBuf, errCap): Int` | `:156-179` | 返回码契约 `:156`「0=成功，1=失败（errBuf 内有消息）；Blocking——只能在 NAPI async work 里调」。`:165` `ensureOhosAuthReady()`；`:166-171` `runBlocking { AuthRepository.loginWithPassword }`；`:172-178` fold：失败时 `copyUtf8(message ?: "登录失败，请稍后重试")` 并返 1 | `napi_init.cpp:147`（async worker 分发 `:143-160`） |
| `KnAuthLoginWithOtpBlocking(phone, code, errBuf, errCap)` | `:181-203` | 同构，`loginWithOtp` `:191` | `napi_init.cpp:150` |
| `KnAuthSendPhoneOtpBlocking(phone, errBuf, errCap)` | `:205-225` | 同构，`sendPhoneOtp` `:214`，默认错词「发送失败」`:221` | `napi_init.cpp:153` |
| `KnAuthRegisterBlocking(email, password, displayName, errBuf, errCap)` | `:227-251` | 同构，`register` `:238`，默认错词「注册失败」`:247` | `napi_init.cpp:156` |
| `KnAuthLoginWithHuaweiBlocking(code, deviceId, errBuf, errCap)` | `:253-275` | 同构，`loginWithHuaweiCode` `:263`，默认错词「登录失败」`:271` | `napi_init.cpp:159` |
| `KnAuthCopyToken(buf, cap)`: `277-282`、`KnAuthCopyDisplayName`: `284-289`、`KnAuthCopyUserId`: `291-296`、`KnAuthCopyPhone`: `298-303` | `:277-303` | 单行表达式 `copyUtf8(AuthBridge.token()/displayName()/userId()/phone(), buf, cap)`——出参缓冲区式字符串取回 | `napi_init.cpp:277,282,287,292`（`NapiCopyStringGetter` 包装） |
| `KnAuthIsLoggedIn(): Int` | `:305-308` | `AuthBridge.isLoggedIn()` → 1/0（**不**装机，纯读） | `napi_init.cpp:296-301` |
| `KnAuthLogout()` | `:310-315` | `ensureOhosAuthReady()` + `AuthBridge.logout()` | `napi_init.cpp:304` |
| `KnAudioOnPageHide()` | `:317-319` | **空实现占位**（`= Unit`）；页面隐藏钩子已接通但行为未填 | `napi_init.cpp:40` |
| （private）`copyUtf8(value, buf, cap): Int` | `:131-147` | UTF-8 字节写入 `min(size, cap-1)` + `\0` 结尾；buf null 或 cap<=0 返 -1 | 上表 Copy 系列 |
| （private）`ensureOhosAuthReady()` | `:149-154` | bootstrap + `ShellBootstrap.install()`（注释 `:152`「ArkTS 任意入口都可能首个触发」） | 各 KnAuth* 首行 |

契约对象 `OhosComposeHostRequest` 本体在 feature 层：`feature/shell/src/commonMain/.../OhosComposeHostRequest.kt:10-14`（`mode`：0=Mine 岛 / 1=二级岛；`mineRoute: MineIslandRoute`、`secondaryRoute: String` 三个可变槽）。装配语义：ArkTS 每次开岛前**先写槽、后造 VC**（`Index.ets:832-842`），Kotlin 工厂读槽决定渲染哪个岛——一条无参数的进程级 IPC。

### 3.9 `androidMain` 入口逐函数（MainActivity / NativeAndroidApp / NativeAndroidMain 骨架）

| 声明 | 行号 | 行为要点 |
|---|---|---|
| `class MainActivity : ComponentActivity` | `MainActivity.kt:21-63` | `onCreate :22-41` 严格顺序：`installSplashScreen()` `:23` → `enableEdgeToEdge()` `:24` → `super` `:25` → `AndroidAppContext.install(this)` `:26` → `AndroidActivityContext.install(this)` `:27` → `platformNetworkBootstrap()` `:28` → `MockAuthConfig.useMockAuth = FLAG_DEBUGGABLE` `:32-33`（注释 `:29-31`：core:network android actual 恒 false，debug 包走本地 Mock 13400000000/123456，release 自动回落真实 BFF）→ `ShellBootstrap.install()` `:35`（注释 `:34`「须早于任何业务请求」）→ `handleDeepLinkIntent(intent)` `:36` → `setContent { NativeAndroidApp() }` `:38-40` |
| `onNewIntent` | `:43-47` | `setIntent` + 重新 `handleDeepLinkIntent`（manifest 声明 `launchMode="singleTop"`，`composeApp/src/androidMain/AndroidManifest.xml:20`，顶复用走此路径） |
| `onResume` / `onPause` | `:49-52` / `:54-57` | `AndroidActivityContext.install/clear`——Activity 引用随生命周期维护（`:core:platform` 消费） |
| `private handleDeepLinkIntent(intent?)` | `:59-62` | `intent?.data ?: return` → `acceptDeepLink(data.toString())`（`core/router/DeepLinkEntry.kt:9-10`） |
| `@Composable internal fun AppAndroidPreview()` | `:65-68` | 渲染 `NativeAndroidApp()`；**无 `@Preview` 注解**、仓内无调用方——名字残留（并入 §8.2 E9 死符号清单） |
| `private enum NativePhase` | `NativeAndroidApp.kt:44` | Splash / Privacy / Main |
| `@Composable fun NativeAndroidApp()` | `:50-74` | `:52` `AppContainer.get()` 早绑定；`:53` `remember { createPrivacyConsentStore() }`；`:58-73` 三相：Splash 完 → `accepted ? Main : Privacy` `:59-61`；Privacy onAccept `:63-70` = `setAccepted(true)` + `scope.launch { runCatching { AppContainer.get().bridges.push.registerForPush() } }`（`:66-68`，**push 注册唯一触发点**）+ Main；Main → `NativeAndroidMain()` `:72` |
| `private fun NativeSplash(onFinished)` | `:76-97` | `LaunchedEffect` `delay(1200)` `:78-81`（legacy 是 1000ms，`AppShell.kt:134-137`）；bg_splash Crop + ic_splash_logo 96dp `:82-96`；**无「进入」跳过按钮**（legacy 有，`AppShell.kt:165-167`） |
| `private fun NativePrivacy(onAccept)` | `:99-150` | `denied` 本地态 `:101`：「不同意」按钮第一次点击切拒绝文案 `:119-124`、同按钮变「重新选择」`:140`（拒绝回路）；仅「同意并继续」`:142-147` 触发 onAccept。对比 legacy 的空 onClick `AppShell.kt:203-208` |
| `@Composable fun NativeAndroidMain()` | `NativeAndroidMain.kt:85-758` | 路由中枢；内部局域函数清单（行号）：`requiresLoginForRoute :124-145`、`selectTab :147-157`、`openHomeRoute :159-164`、`pushHomeRoute :166-171`、`popHomeRoute :173-184`、`openCommunityRoute :186-198`、`openMineRoute :200-213`、`pushMineRoute :215-220`、`popMineRoute :222-233`、`openContentRoute :235-240`、`pushContentRoute :242-247`、`popContentRoute :249-260`、`openPendingRoute :262-302`、`openWithSoftAuth :304-312`、`afterAuthSuccess :314-325`、`openDeferred :327-369`、`navigateSecondary :372-415`、`closeOverlay :417-427`；deep-link `LaunchedEffect(snapshotFlow)` `:429-543`；overlay `when` 渲染 `:545-757`；Jetpack Tab 根 keep-alive `:684-752`（逐级行为见 §5.2-A） |
| `private enum AuthOverlay` / `ShellOverlay` | `:760` / `:761-773` | AuthOverlay 5 值；ShellOverlay 11 值：None/MineIsland/UnmappedEntry/AllServices/Membership/InAppWeb/Scan/HomeRoute/CommunityRoute/MineRoute/ContentRoute。**Membership 无任何赋值点**（死分支，§8.2 E9） |
| `private fun JetpackUnmappedEntry(title, onBack)` | `:775-794` | 未映射兜底页：文案「未映射入口（out-of-scope / 未登记）」`:788` + 返回按钮 `:790-792` |
| `NativeJetpackHome.kt` | `:57`（`JetpackHomeRoot`） | 首页点击源：搜索栏 `:141-142`、扫一扫 `:143`、宫格「更多」→全部服务 `:148-153`、待办卡片 `:157`；直接 import `feature.mine.SwitchStoreDialog` `:37`（装配层特权，R7 允许） |

`AndroidManifest.xml`（`composeApp/src/androidMain/AndroidManifest.xml`）为 `MainActivity` 注册了三组 intent-filter：LAUNCHER `:22-25`、自定义 scheme `myai` `:27-32`（注释 `:26` 举例 `myai://home|chat|mine|auth/login`）、可选 https App Link `app.myai.example` `:34-42`；权限 `INTERNET/ACCESS_NETWORK_STATE/CAMERA` `:4-6`、`usesCleartextTraffic=true` `:14`。深链冷启动/热启动分别落 `onCreate :36` 与 `onNewIntent :46`（与 §5.2-D 链路 D 的 Android 段衔接）。

---

## 4. 依赖注入专题

### 4.1 AppContainer 实际建了哪些对象、构造顺序

`AppContainer.get()` 首次触碰时（`AppContainer.kt:25` lazy），按构造参数默认值顺序实例化：`NetworkComponent`（无状态）→ `SessionComponent`（空）→ `BridgeComponent`（`StubPushBridge` 单例引用 + `FlaggedPayGateway()` + `StubChatPort` 单例引用）→ `NavigationComponent`（`navigator=null`）。**四个组件互相不引用**，是纯命名空间分组。

触发点：`App.kt:11`（legacy）、`NativeAndroidApp.kt:52`、`NativeAndroidMain.kt:86`——Android 主路径确实「早绑定」，但绑完基本没用：四个子组件里只有 `navigation.bind`（legacy AppShell）和 `bridges.push`（隐私后注册推送）有消费方，`network.api()`/`pay`/`chat`/`session` 当前为空转（诚实状态，勿照抄成 DI 容器）。

### 4.2 KeyValueStore / ApiClient / RealtimeClient 的真正持有者

| 对象 | 持有者 | 证据 |
|------|--------|------|
| `KeyValueStore` | **不在 AppContainer**。KV 工厂 `createKeyValueStore(name)` 是 `core.storage` 的 expect fun（`core/storage/src/commonMain/.../KeyValueStore.kt:58`，接口 `:28`）。消费者：① `AccountFacade` 的 lazy `store: AccountSessionStore`（`core/account/AccountFacade.kt:42`，工厂 `AccountSessionStore.kt:24-25`，store 名 `"demo_account"` `:20`）；② `createPrivacyConsentStore()`（`PrivacyConsentStore.kt:19-20`，同名 store，键 `"demo_privacy_accepted"` `:17`）——由 AppShell（`AppShell.kt:101`）和 NativeAndroidApp（`NativeAndroidApp.kt:53`）各自 `remember` 创建 |
| `ApiClient` | `NetworkFacade` 单例：`rawClient`（平台 client，`core/network/NetworkFacade.kt:26-29` 初始即 `createPlatformApiClient(...)`）+ `client`（装饰后，`:32`）；`api()/rawApi()` `:34/:40`；`rebuildClient()` `:68-74`；401 装饰器 `AuthRefreshingApiClient` 由 `decorate()` 条件挂载（`:93-106`）。AppContainer 的 `NetworkComponent.api()` 只是薄转发且无人调用 |
| `RealtimeClient` | **装配层当前没有持有任何 realtime client**。`AppRealtimeClient` 接口在 `component/src/commonMain/.../realtime/AppRealtimeClient.kt:18`，`AppRealtimeClientImpl`（`AppRealtimeClientImpl.kt:50`）全仓只在 `component/src/commonTest/` 里被构造（如 `AppRealtimeClientImplTest.kt:74`）——引擎已装机未接线 |

### 4.3 feature 拿到的端口从哪里注入

| 端口/回调 | 注入动作 | 注入方（file:line） | feature 侧消费点 |
|-----------|----------|----------------------|------------------|
| 401 静默刷新（policy+refresher+onSessionCleared） | `NetworkFacade.setAuthRefreshSupport(...)` | `feature/auth/AuthSessionBootstrap.kt:22-26`（经 `ShellBootstrap.install()` → `AppShell.kt:107` / `MainActivity.kt:35` / `MainViewController.kt:19,30,53,76` / `MainArkUIViewController.kt:41,153`） | `:core:network` 全部请求 |
| token 过期登出回调 | `NetworkFacade.setTokenExpiredHandler(TokenExpiredHandler { AuthRepository.logout(); softAuth.clearLocalSession() })`，onDispose 置 null | `NativeAndroidMain.kt:88-98` | 仅 Android 主路径安装 |
| `CrossFeatureScreenPort` | `CrossFeatureScreens.install(AppCrossFeatureScreens)` | `ShellBootstrap.kt:24` | §6 |
| WebView handlers | `HomeWebHandlers.register()` | `NativeAndroidMain.kt:90` | `:feature:home`（`feature/home/.../HomeWebHandlers.kt:11,18`） |
| Android Context | `AndroidAppContext.install(this)` / `AndroidActivityContext.install(this)`（onResume/onPause 同步维护） | `MainActivity.kt:26-27,49-57` | `:core:storage` / `:core:platform` |
| Mock 登录开关 | `MockAuthConfig.useMockAuth = FLAG_DEBUGGABLE 判定` | `MainActivity.kt:29-33` | `:feature:auth` |
| `AppNavigator` | `AppContainer.get().navigation.bind{...}` 后 `LocalAppNavigator provides navigator` | `AppShell.kt:239-258, 338-341` | 仅 `feature/home/HomeScreen.kt:43`（legacy 共享壳路径） |
| OHOS 宿主意图 | ArkTS 经 N-API `SetOhosHost(kind,routeCode)` / `SetOhosSecondaryRoute(ptr)` 写 `OhosComposeHostRequest` | `MainArkUIViewController.kt:82-106`；契约对象 `feature/shell/.../OhosComposeHostRequest.kt:10-14`（mode 0=Mine 岛 / 1=二级岛） | `MainArkUIViewController.kt:49-58` |

> 结论：装配层的注入是「**进程级单例 + 幂等 install**」风格（NetworkFacade/AccountFacade/AuthSessionState/CrossFeatureScreens 全是 object 上的可替换槽位），不是构造子图注入；`AppContainer` 只是把「将来的 Koin 缝」预留出来。

---

## 5. 路由跳转全景（本册核心）

### 5.1 三层契约总览

| 层 | 文件 | 作用 |
|----|------|------|
| 字符串路由表 | `core/router/.../AppRoutePath.kt:7-111`（103 条 Flutter 全量路径）、`FeatureRoutes.kt`（`HomeRoutes:17-76`、`MineRoutes:78-154`、`ContentRoutes:157-177`、`VideoRoutes:180-198`、`CommunityRoutes:200-217`、`ClassroomRoutes:219-235`、`AiRoutes:237-244`，每个对象带 `fromLabel(中文标签)→路径`）、`AppRoutes.kt:4-35`（壳键）+ `MainTab` 枚举 `:37-46`（Chat/Community `requiresAuth=true`）、`RoutePath.kt:8-64`（别名汇总） | 路径常量 SoT，位于 feature 层之下，feature 之间靠它互指而不互相依赖 |
| 解析/分发 | `ProductRouteDispatch.kt`：`ProductRouteHost` 枚举 `:9-23`（Tab/Home/Mine/Community/Content/AllServices/Web/Scan/Auth/Toast/Unknown）、`resolve(raw): ProductRouteTarget?` `:62-110`（标签/短别名/绝对路径三入口；toastOnly 名单 `:33-56`）、`hostForPath` `:112-133`（按前缀归 host）、`canonicalizePath:140-143`、`stripToPath:145-153`；`DeepLinkRouter`（`pendingDeepLink:29`、`accept:36-40`、`consumePending:43-47`、`parse:54-113`、`extractPath:115-149` 含 `myai://mall/orders→/mall/orders`、`/app/` host 剥离、小写化）、`ParsedDeepLink:158-162`、`webUrlFromDeepLink:168-192`（`/web?url=` 半角解码）；`acceptDeepLink(uri):9-10`（Swift/ArkTS 稳定符号，见 `DeepLinkEntry.kt:4-7`）；类型安全 `AppRoute` sealed `AppNavigator.kt:9-24` + `AppNavigator.navigate/openTab/popToRootTab` `:32-36`（openTab/popToRootTab 当前无人调用）+ `LocalAppNavigator :39`；Mine 岛枚举 `MineIslandRoute.kt:7-64` | 把任意输入归一为「路径 + 应落到的 host」 |
| 渲染表 | 装配层 `AppCrossFeatureScreens`（§3.4）；feature 层各 RouteHost（§5.4）；平台层 overlay 状态机（Android `NativeAndroidMain.kt:545-757`、iOS `ContentView.swift:117-329`、Harmony `Index.ets:877-1015`） | route 字符串 → composable / SwiftUI 页 / ArkTS pane |

类型安全层速览（`core/router/AppNavigator.kt` + `AppRoutes.kt`，均已整篇核对）：

- `AppRoute` sealed interface（`AppNavigator.kt:9-24`，头注释 `:5-8`：字符串 `AppRoutes` 仅为深链兼容保留）共 14 个变体：`Splash :10`、`PrivacyConsent :11`、`Login :12`、`Register :13`、`Tab(MainTab) :14`、`MineAbout :15`、`AllServices :16`、`Membership :17`、`InAppWeb(url) :18`、`Scan :19`、`Media :20`、`Live :21`、`Friend :22`、`Classroom :23`。**legacy AppShell 只接受后 8 类中的 7 个业务变体 + InAppWeb**（`AppShell.kt:241-246`），Splash/PrivacyConsent/Login/Register/Tab/MineAbout 6 类被 `else -> Unit` 丢弃（`:255`）——E9 死符号的判定点在此。
- `AppNavigator`（`AppNavigator.kt:29-37`）：`navigate :32` 转发构造参数 `onNavigate`；`openTab :34` 仅是 `navigate(Tab(…))` 语法糖；`popToRootTab :36` 实现为 `= Unit // reserved; shell clears overlay`——三个方法中只有 `navigate` 有真实消费链。`LocalAppNavigator = staticCompositionLocalOf<AppNavigator?> { null }`（`:39`），默认 null 意味着 feature 侧不判空就会 no-op；全仓唯一读取点 `feature/home/HomeScreen.kt:43`。
- `AppRoutes` 字符串键（`AppRoutes.kt:4-35`）：`/splash :6`、`/privacy/consent :10`、`/auth/login :14`、`/auth/register :15`、`/home :19`、`/chat :23`、`/chat/detail :24`、`/community :28`、`/mine :32`、`/mine/about :33`。
- `MainTab` 枚举（`AppRoutes.kt:37-46`）：`Home("/home","首页") :42`、`Chat("/chat","聊天",requiresAuth=true) :43`、`Community("/community","社区",requiresAuth=true) :44`、`Mine("/mine","我的") :45`。软门禁真正判定在 `component/auth/AuthGate.kt:22-33`——按**枚举名/路由字符串表**判断，`MainTab.requiresAuth` 字段本身（`:40,43-44`）在 Kotlin 侧无读取点（全仓 grep 仅声明/赋值/AuthGate 同名函数，属 E9 类预留符号）。各壳调用点：legacy `AppShell.kt:293,310`；Android 深链 `NativeAndroidMain.kt:481,504,532`，Tab 切换走 `softAuth.trySelectTab`（`:147-157`，内部 `SoftAuthPresenter.kt:44` 再调 AuthGate）；Harmony/iOS 不复用 AuthGate，使用各自的 `requiresLoginForRoute` 名单（`Index.ets:1017-1061`；iOS 见 `ContentView.swift:304-320`）。

### 5.2 端到端链路示例（照此可复现）

**链路 A：Android 原生 Tab 上点「搜索」**（运行时主路径）
1. 首页搜索栏点击 → `JetpackHomeRoot` 的 `HomeSearchBar(onSearch = { onDeferred("搜索") })`（`composeApp/src/androidMain/.../NativeJetpackHome.kt:141-142`；「扫一扫」`:143`、功能宫格「更多」→「全部服务」`:148-153`、待办卡片 `:157` 同理）。
2. `onDeferred` 即 `NativeAndroidMain.openDeferred(label)`（挂接点 `NativeAndroidMain.kt:697`；函数体 `:327-369`）。
3. `ProductRouteDispatch.resolve("搜索")`（`:328`）→ `HomeRoutes.fromLabel("搜索") == "/home/search"`（`core/router/FeatureRoutes.kt:49`）→ `ProductRouteTarget(path="/home/search", host=Home)`（`hostForPath` `ProductRouteDispatch.kt:123`）。
4. `case ProductRouteHost.Home -> openWithSoftAuth(path) { openHomeRoute(it) }`（`NativeAndroidMain.kt:359`）；`requiresLoginForRoute`（`:124-145`）不含 `/home/search` → 直接放行。
5. `openHomeRoute`（`:159-164`）：`homeRoute=route`、`homeRouteStack=[route]`、`overlay=ShellOverlay.HomeRoute`、`bottomBarVisible=false`。
6. 渲染：`when(overlay)` 的 `ShellOverlay.HomeRoute` 分支（`:561-568`）→ `HomeRouteHost(route, onBack={popHomeRoute()}, onNavigate={navigateSecondary(it)})`。
7. `feature/home/HomeRouteHost.kt:116-122` `when(route){ HomeRoutes.Search -> HomeSearchScreen(onBack) }`——**目标屏渲染完成**。站内再跳（如学习报告→会员）走 `onNavigate("/pay/membership")`（`HomeRouteHost.kt:126`）→ `navigateSecondary`（`NativeAndroidMain.kt:372-415`）重新按 host 选 overlay（`:395-400` Mine host → `openMineRoute` → `MineIslandRoute.fromPath` 命中 Membership → `ShellOverlay.MineIsland`，`:200-213`）。

**链路 B：iOS 上从「设置」入口到 Compose Mine 岛**
1. SwiftUI `MineRootView(onOpenSettings:)`（`iosApp/iosApp/ContentView.swift:376-379`）设 `mineIslandRoute="settings"`、`showMineIsland=true`；其他入口统一走 `openOwnedRoute`（`:132-230`）：先 toast-only 拦截（`:135-166`），`NativeRouteResolver.resolve`（`:172`；`iosApp/iosApp/NativeFeatureCatalog.swift:5-9`），再按 path switch：`/settings → showMineIsland`（`ContentView.swift:192-194`）。
2. `fullScreenCover(showMineIsland)` → `MineIslandHost(route:)`（`ContentView.swift:435-437`，representable `:497-503`）。
3. `MainViewControllerKt.MineIslandViewController(route:)`（`composeApp/src/iosMain/.../MainViewController.kt:28-43`）：`platformNetworkBootstrap()`+`ShellBootstrap.install()`（`:29-30`）→ `MineIslandRoute.fromPath(key) ?: fromHostKey(key)`（`:32`）→ `ComposeUIViewController { MineIsland(initialRoute, onRequestClose = controller.dismiss…) }`（`:34-41`）。
4. `feature/mine/MineIsland.kt:18-44`：内部栈 `[initialRoute.toPath()]`（`:22`）；`current == "/mine/about"` 走 `MineAboutScreen`（`:26-31`），否则 `MineRouteHost(route, onBack=栈弹, onNavigate=栈推)`（`:33-41`）→ 设置屏。
5. iOS 若目标是 mall/wallet 等「Mine 属主但非岛内」路径：`openSecondaryOrNative`（`:234-249`）→ `isMineComposeSecondary`（`:253-265`）→ `MineSecondaryHost`（`:505-510`）→ `SecondaryRouteViewController`（`MainViewController.kt:51-64`）→ `SecondaryRouteIsland`（`feature/shell/.../SecondaryRouteIsland.kt:61-93`，只收 Mine host，`:87-90` 非 Mine 显示拦截页）→ `CrossFeatureScreens.MineRouteScreen`（`:92`）→ **装配层** `AppCrossFeatureScreens.MineRouteScreen`（`AppCrossFeatureScreens.kt:78-85`）→ `MineRouteHost`。
6. 非 Mine 的 Home/Content/Community 二级：`ContentView.swift:246-248` → `NativeFeatureHost`（struct 定义 `NativeFeatureCatalog.swift:470`，SwiftUI 原生页），**不回 Compose**。

**链路 C：Harmony 上从 ArkTS 按钮到 Compose 岛或原生 pane**
1. ArkTS 页按钮统一调 `this.openDeferred(...)`（`harmonyApp/entry/src/main/ets/pages/Index.ets:1757/1789/1808/1874` 等数十处；Tab 内容里的 `onDeferred` 桥 `:1874`）。
2. `openDeferred(title)`（`Index.ets:877-995`）：toastOnly 名单先弹 toast（`:879-908`，对齐 Flutter）；`新车成交→openMineIslandRouteWithSoftAuth(7)`（`:911-913`）；`resolveNativePath(title)`（`:915`；`NativeFeatureCatalog.ets:132-218`，含 `LABEL_TO_PATH` 字典 `:21-109` 与 `MineRoutes.canonicalize` 同款别名 `:199-214`）；`/settings→openComposeHost(0,0)`、`/mine/personalized_settings→(0,1)`、`/pay/membership→(0,2)`、`/mine/about→(0,3)`、profile/addresses/address_edit 带软门禁 `:932-942`、calculator→`(0,6)`（`:944-946`）、`/settings/deal_invoice*→(0,7|8)`（`:948-951`）。
3. 其余路径：`Index.ets:987-994` → `requiresLoginForRoute`（`:1017-1061`）? `openSecondaryWithSoftAuth`（`:1063-1070`）: `openSecondaryOrNative`（`:997-1015`）。
4. `openSecondaryOrNative`：`nativePaneIdFor(route)`（`:998`；`NativeFeatureCatalog.ets:484-707`）非空 → `this.nativePane = pane`（ArkTS 原生二级页，如 `'search'`:538、`'mall'`:640-642、`'feature'` 兜底=目录页 `:707` + `featurePageFor(path)` 数据表 `:228-470`，页面消费 `Index.ets:1125-1250`）；空串且属 Mine/settings/membership → `openSecondaryCompose(route)`（`:1007-1010`，走 `SetOhosSecondaryRoute`，`:870-875`）；未知非 Mine → 强制 `nativePane='feature'`（`:1012-1014`，绝不回 Legacy 岛）。
5. `openComposeHost(kind, routeCode)`（`:826-848`）：`libentry.SetOhosHost(kind, routeCode)`（`:832-834`）→ Kotlin `KnSetOhosHost`（`MainArkUIViewController.kt:82-98`，routeCode 0-9 映射 `MineIslandRoute`，注释 `:73-79`）写 `OhosComposeHostRequest`；`libentry.MainArkUIViewController()`（`:836,842`）→ `@CName("MainArkUIViewController")`（`MainArkUIViewController.kt:33-71`）：`initMainHandler`（`:37`）→ `platformNetworkBootstrap()`+`ShellBootstrap.install()`（`:39-41`）→ 按 `mode`：`1 → SecondaryRouteIsland(initialRoute = OhosComposeHostRequest.secondaryRoute …)`（`:50-53`）、否则 `MineIsland(initialRoute = mineRoute)`（`:54-57`）——与 iOS 同一条 Compose 表。
6. 两个 dispatch 回归脚本用静态解析守护上述「route→id/页→渲染分支→import」闭环：`scripts/check-harmony-pane-dispatch.sh:1-14`（Harmony：`nativePaneIdFor` 返回的每个 id 必须有 Index.ets 渲染分支且 struct 已导入，双向检查 `:90-96`）、`scripts/check-ios-page-dispatch.sh:1-21`（iOS：每个 `Native…Page` struct 必须被 `NativeFeatureHost` 的 `path == "…"` 守卫实例化且守卫可达 resolve() 输出；已知路由不得在有专属页时落 genericList，`:242-266`；out-of-scope 豁免名单 `:171-202`）。

**链路 D：深链接（三端）**
- Android：`MainActivity.handleDeepLinkIntent`（`MainActivity.kt:59-62`，onCreate `:36` + onNewIntent `:43-47`）→ `acceptDeepLink`（`core/router/DeepLinkEntry.kt:9-10`）→ `DeepLinkRouter.accept`（`:36-40`）存 `pendingDeepLink` → `NativeAndroidMain` 的 `LaunchedEffect snapshotFlow{DeepLinkRouter.pendingDeepLink}.filterNotNull().distinctUntilChanged()`（`NativeAndroidMain.kt:429-543`）：auth 四路径 `:436-451`、`/web`→InAppWeb `:452-456`、`/friend` `:457-459`、`/video|/music|/classroom|/live|/ai/`→openContentRoute `:460-466`、chat detail 解析参数+软门禁 `:467-491`（`chatDetailArgsFromDeepLink`，`component/chat`）、`/home/…` `:492-501`、`/community/…` `:502-513`、`/mall|/wallet|/pay`→Mine `:514-520`、`/settings*` `:521-525`、`/mine/…` `:526-530`、else `pending.tab` `:531-540`。legacy AppShell 用同一 pending 槽（`AppShell.kt:260-302`）。
- iOS：SwiftUI `.onOpenURL`（`ContentView.swift:102-104`）+ 冷启动 `pending_deeplink`/`-route` 参数（`:105-113`）→ `AcceptDeepLinkFromIos(uri)`（`MainViewController.kt:67-70`，内部 `DeepLinkRouter.accept` 返回 `parsed.route`）→ `openOwnedRoute`。
- Harmony：ArkTS skills 侧调 `KnSetOhosSecondaryRoute`/`openDeferred` 组合（`Index.ets:870-875`；扫码同款复用 `MainArkUIViewController.kt:102-106`）。

### 5.3 路由路径 → 目标屏 → 三端承载方式对照表

以 `FeatureRoutes`/各 RouteHost/两个 dispatch 脚本为准。Legacy AppShell 列指 `AppShell.kt`；Android 列指 `NativeAndroidMain.kt`（overlay 名）；iOS 列指 `ContentView.swift` + `MainViewController.kt`；Harmony 列指 `Index.ets` + `NativeFeatureCatalog.ets` 的 `nativePaneIdFor` 返回值。

| 路由组（示例路径） | 目标屏 | Legacy AppShell | Android 原生 | iOS | Harmony |
|---|---|---|---|---|---|
| Tab 根 `/home /chat /community /mine` | 各 Tab 根 | `:389-418` 共享 HomeScreen/ChatScreen/CommunityScreen/MineScreen | `:684-752` JetpackHomeRoot/JetpackChatRoot/JetpackCommunityRoot/JetpackMineRoot（`NativeJetpackTabs.kt:86/840/1267`） | SwiftUI HomeTabView/ChatTabView/CommunityTabView/MineRootView（`ContentView.swift:341-398`） | ArkTS Index tab（`Index.ets:952-986`） |
| Mine 岛 10 路径（`/settings /mine/personalized_settings /mine/about /pay/membership /mine/profile /mine/addresses /mine/addresses/edit /mine/purchase_calculator /settings/deal_invoice_demo /settings/deal_invoice/upload`，`MineIslandRoute.kt:7-31`） | MineIsland→MineRouteHost | `AppRoute.Membership` 单例覆盖 `:429-431` | `ShellOverlay.MineIsland` `:546-551`（`openMineRoute` 命中 `fromPath` `:202-208`） | `MineIslandViewController`（`MainViewController.kt:28-43`）+ island key（`ContentView.swift:288-302`） | `openComposeHost(0, code)` `Index.ets:917-951`，routeCode 表 `MainArkUIViewController.kt:85-96` |
| Mine 属主其余（`/mall* /wallet /pay /mine/sms_templates /mine/shop_qr /mine/http_test /mine/poster /mine/feedback /mine/cooperation /mine/reminder /mine/invite`，表 `feature/mine/MineRouteHost.kt:71-136`） | MineRouteHost 各屏 | 无（仅 Membership） | `ShellOverlay.MineRoute` `:579-593`（栈 push/pop `:215-233`） | `SecondaryRouteViewController`→SecondaryRouteIsland（`ContentView.swift:241-244,505-510`） | **ArkTS 原生 pane**：`mall/mall_detail/mall_orders/…/wallet/qa/poster/store_qr/sms_template/feedback/business/reminders`（`NativeFeatureCatalog.ets:498-530,631-645`）——不进 Compose |
| Home 二级 `/home/*`（28 条，表 `core/router/.../FeatureRoutes.kt:17-45`；渲染表 `feature/home/HomeRouteHost.kt:116-208`） | HomeRouteHost→HomeSearchScreen 等 | Tab 内容内 `destination` 自栈（`HomeScreen.kt:67-104`） | `ShellOverlay.HomeRoute` `:561-568` | `NativeFeatureHost` SwiftUI 页（`ContentView.swift:246-248`） | `nativePaneIdFor`→`search/learning_report/strategy/check_in_mall/transliter/hot_rank/used_car*/ledger*/data_analytics*/life_service/club/after_sales*/new_car_follow*/todo`（`NativeFeatureCatalog.ets:538-609`） |
| Content `/video* /classroom* /live* /friend /music* /media/entry /ai/stream` | `AppCrossFeatureScreens.ContentRouteScreen` `:53-76`（Classroom 表 `:71-116`、Video 表 `:126-172`） | shellRoute 单值（`:470-481`，仅 Media/Live/Friend/Classroom 四类；音乐/AI 走 Legacy 岛） | `ShellOverlay.ContentRoute` `:594-600`→`ContentRouteHost`（`feature/content/ContentRouteHost.kt:13-19`）→ 装配层表 | 岛外路径 `NativeFeatureHost`；Mine 岛白名单外的 `/video/short` 等也进 `NativeFeatureHost` | pane：`video_hub/short_video/short_play/…/classroom/homework_stats/homework_detail/gift_claim/live/live_room/friend/ai_stream/music*/`（`NativeFeatureCatalog.ets:622-696`） |
| Community 二级 `/community/{publish,search,convention,comment,image_preview,…}`（`core/router/.../FeatureRoutes.kt:200-217`；渲染表 `feature/community/CommunityRouteHost.kt:84-…`） | CommunityRouteHost | 无（Community Tab 根内自管） | `ShellOverlay.CommunityRoute` `:569-578`（含 previewUrls/videoUrl 透传 `:186-198`） | `NativeFeatureHost` | pane：`community_publish/search/comment/image_preview/convention`（`NativeFeatureCatalog.ets:610-621,703-705`） |
| `/web`（含 `?url=`） | InAppWebScreen（:feature:web；url 解析 `DeepLinkRouter.webUrlFromDeepLink` `core/router/DeepLinkRouter.kt:168-192`；离线兜底 `OfflineWebFixtureUrl` `component/webview/WebViewComponent.kt:20`） | `AppRoute.InAppWeb` `:422-425` | `ShellOverlay.InAppWeb` `:604-617`（带 `DefaultWebBridgeHost` JS 桥） | `NativeFeatureHost`（web 页） | pane `web`（`NativeFeatureCatalog.ets:697-699`） |
| `/scan` | ScanScreen（:feature:scan）→ 扫码结果再进 `DeepLinkRouter.accept` | `AppRoute.Scan` `:432-469` | `ShellOverlay.Scan` `:618-636` | `NativeFeatureHost` | pane `scan`（`:700-702`） |
| `/home/all_services` | AllServicesScreen（:feature:home） | `AppRoute.AllServices` `:426-428` | `ShellOverlay.AllServices` `:552-560` | `NativeFeatureHost` | pane `all_services`（`:541-543`） |
| 认证 `AppRoutes.Auth.LOGIN`、`/login /login/password /login/otp /register` | Login/LoginPassword/LoginOtp/Register（:feature:auth） | authOverlay `:366-388`；deep-link `:265-280` | authOverlay `:643-667`；deep-link `:436-451`；host=Auth `:346-349` | `showLogin` fullScreenCover（`ContentView.swift:412-434`） | ArkTS 登录页 `showLogin`（`Index.ets:855-867` 软门禁复用） |
| toast-only（`/mine/business_card /mine/invite 短信模板 店铺收款码 商家海报 切换门店 …`） | 不导航 | 未做（表外） | `ProductRouteHost.Toast` `:330-332` | toast 文案表 `ContentView.swift:135-166` | toast 名单 `Index.ets:879-908` |
| 未映射 | 提示页 | 丢弃 | `ShellOverlay.UnmappedEntry` `:637-642`（`JetpackUnmappedEntry` `:775-794`）；Harmony 兜底 `nativePane='feature'` 目录页（`NativeFeatureCatalog.ets:707` + `featurePageFor` `:228`） | genericList 占位（脚本豁免名单外必须挂专属页） | 同左 |

### 5.4 各 RouteHost 的归属与签名（分发点核对表）

| RouteHost | 所在模块（file:line） | 签名 | 表规模/兜底 |
|---|---|---|---|
| `HomeRouteHost` | `feature/home/.../HomeRouteHost.kt:116-208` | `(route: String, onBack: () -> Unit, onNavigate: (String) -> Unit = {})` | 26 路 `HomeRoutes.*`；else `CrudDetailScreen("未识别路由")`（`:206`） |
| `MineRouteHost` | `feature/mine/.../MineRouteHost.kt:71-136` | 同上 | **进表先 `MineRoutes.canonicalize(route)`**（`:77`）；第一分支即 `CrossFeatureScreens.handlesSiblingScreen→RenderSiblingScreen`（`:79-80`，拦 `/classroom /video /pay/membership`）；else `UnmappedMineRoute`（`:134`，屏体 `:138-145`） |
| `ContentRouteHost` | `feature/content/.../ContentRouteHost.kt:13-19` | 同上 | 纯转发 `CrossFeatureScreens.ContentRouteScreen`（R7 规则：全表都是兄弟 feature 屏） |
| `ClassroomRouteHost` | `feature/classroom/.../ClassroomRouteHost.kt:70-116` | 同上 | `ClassroomRoutes` 8 路；else 回 `ClassroomScreen`（`:114`） |
| `VideoRouteHost` | `feature/media/.../VideoRouteHost.kt:125-172` | 同上 | `VideoRoutes` 9 路；else `VideoHubLanding`（`:163`） |
| `CommunityRouteHost` | `feature/community/.../CommunityRouteHost.kt:83-…` | `(route, onBack, onNavigate, previewUrls: List<String> = [], previewIndex: Int = 0, videoUrl: String? = null)` | `CommunityRoutes` 7 路 + `/community/comment?postId=` 查询串解析；else「未知路由」页 |
| `MineIsland` | `feature/mine/.../MineIsland.kt:17-44` | `(initialRoute: MineIslandRoute = Settings, onRequestClose: () -> Unit)` | 内部 List 栈 push/pop；about 特判 |
| `SecondaryRouteIsland` | `feature/shell/.../SecondaryRouteIsland.kt:60-93` | `(initialRoute: String, onRequestClose: () -> Unit)` | 仅 Mine host（`SecondaryRouteResolver.resolve:34-53` + `isMineIslandOwned:56-57`）；非 Mine 渲染「请从原生壳打开」（`:95-109`）。文件头 `:26-32`：**Android 主路径不使用此宿主** |
| 装配层表 | `composeApp/.../AppCrossFeatureScreens.kt` | §3.4 | — |

### 5.5 `ProductRouteDispatch.resolve` 输入优先级 + `hostForPath` 全表

`resolve(raw)`（`core/router/ProductRouteDispatch.kt:62-110`）六级瀑布，**先命中先出**：

| 级 | 条件 | 行号 | 结果 |
|---|---|---|---|
| 0 | trim 后空串 | `:63-64` | `null`（唯一返 null 的路径） |
| 1 | 标签在 `toastOnlyLabels`（16 项 `:33-39`）或路径在 `toastOnlyPaths`（14 项 `:41-56`） | `:66-76` | `host=Toast` + 专属文案：切换门店系「请在「我的」页头点击门店名称切换」`:68`、请先登录 `:69`、短信模板/收款码/海报「XX 开发中」`:70-72`、其余用去掉 `/mine/` 前缀的输入 `:73` |
| 2 | 以 `/` 开头 / 含 `://` / `http(s)://` | `:79-84` | `stripToPath`（`:145-153`：截 `://` 之后、补头 `/`、截 `?#`）→ `canonicalizePath`（`:140-143`：去 query/fragment 后过 `MineRoutes.canonicalize`）→ `hostForPath` |
| 3 | 显式壳别名 | `:87-102` | `search/strategy/report/media/friend/live/classroom` 直达路径；`services\\|全部服务\\|更多`→AllServices、`web\\|H5 调试\\|内嵌网页`→Web、`scan\\|扫一扫`→Scan、`消息`→Tab(chat) |
| 4 | 四张 feature 标签表 `fromLabel` 依序 | `:104-107` | **Mine → Home → Community → Content**（顺序即优先级：同名标签 Mine 优先） |
| 5 | 兜底 | `:109` | `ProductRouteTarget(input, Unknown)` |

`hostForPath(path)`（`:112-133`，对 path 先 `canonicalizePath` `:113`）when 分支顺序即优先级：

| # | 匹配 | host | 行号 |
|---|---|---|---|
| 1 | `== AppRoutePath.homeAllServices` | AllServices | `:115` |
| 2 | `== AppRoutePath.web` 或 `http://`/`https://` 前缀 | Web | `:116-117` |
| 3 | `== "/scan"` | Scan | `:118` |
| 4 | `== login` / `"/login"` 前缀 / `"/auth"` 前缀 / `== register` | Auth | `:119-120` |
| 5 | 恰为 home、main、chat、community、mine 之一（精确等值） | Tab | `:121-122` |
| 6 | `/home/` 前缀 | Home | `:123` |
| 7 | `/community/` 前缀 | Community | `:124` |
| 8 | `/mine`、`/settings`、`/mall`、`/wallet`、`/pay` 前缀 | **Mine** | `:125-127` |
| 9 | `/ai`、`/video`、`/classroom`、`/live`、`/friend`、`/music`、`/media` 前缀 | Content | `:128-130` |
| 10 | 其余 | Unknown | `:131` |

两个易错点：① mall/wallet/pay 归 **Mine host** 而非 Commerce/Pay——宿主归属按「入口挂在哪个 Tab」划分，与实现模块无关（渲染仍由 `MineRouteHost` 转 `:feature:commerce/:feature:pay` 屏，§3.4/§6）；② 第 5 级 Tab 判定是精确等值，`/home/search` 不会误落 Tab；`MineRoutes.fromLabel` 返回 null 的标签（短信模板/店铺收款码/商家海报/意见反馈/帮助中心/商务合作/提醒事项/邀请好友/粉丝群/电子名片/切换门店——`FeatureRoutes.kt:117,132-140` 注释对齐 Flutter toast-only）与第 1 级名单是**双保险**关系。

### 5.6 MineIslandRoute 十值总对照表（枚举 × 路径 × 壳键 × OHOS code）

枚举定义 `core/router/MineIslandRoute.kt:7-18`（头注释 `:3-5`：只承载二级+子页，Mine 根属原生壳）；`toPath()` `:20-31`；`fromHostKey`（未知键回退 Settings）`:34-45`；`fromPath`（先 `MineRoutes.canonicalize` `:48`，精确匹配、含 `SettingsLegacy="/mine/settings"` 兼容 `:50`，表外返 null）`:47-62`。OHOS 数字码在 `MainArkUIViewController.kt:85-96` 与 `Index.ets:854-868`（软门禁码 4/5/7/8/9 见 `Index.ets:856-857`，登录后续跳串 `:859-863`）。

| 枚举（行） | 规范路径（`FeatureRoutes.kt` 常量） | iOS 壳键（fromHostKey） | OHOS routeCode | Harmony 软门禁 |
|---|---|---|---|---|
| Settings `:8` | `/settings`（`MineRoutes.Settings:100`；兼容 `/mine/settings` `:101`） | `settings` 及一切未知键 `:44` | 0（else 兜底） | 否 |
| Personalized `:9` | `/mine/personalized_settings`（`:104`） | `personalized` `:35` | 1 | 否 |
| About `:10` | `/mine/about`（枚举内字面量 `:23,:52`） | `about` `:36` | 3 | 否 |
| Membership `:11` | `/pay/membership`（`MineRoutes.Membership:85`） | `membership` `:37` | 2 | 否 |
| Profile `:12` | `/mine/profile`（`:86`） | `profile` `:38` | 4 | **是**（`Index.ets:856-859`） |
| Addresses `:13` | `/mine/addresses`（`:87`） | `addresses` `:39` | 5 | **是** |
| AddressEdit `:14` | `/mine/addresses/edit`（`:88`） | `address_edit` \| `addresses/edit` `:40` | 9 | **是** |
| Calculator `:15` | `/mine/purchase_calculator`（`:89`） | `calculator` \| `purchase_calculator` `:41` | 6 | 否 |
| DealInvoiceDemo `:16` | `/settings/deal_invoice_demo`（`:102`） | `deal_invoice` \| `deal_invoice_demo` `:42` | 7 | **是** |
| DealInvoiceUpload `:17` | `/settings/deal_invoice/upload`（`:103`） | `deal_invoice_upload` `:43` | 8 | **是** |

消费端闭环：iOS `MineIslandViewController` 键解析（`MainViewController.kt:32`）与 Android `openMineRoute` 命中岛判定（`NativeAndroidMain.kt:202-208`）共用同一 `fromPath/fromHostKey`；OHOS 数字码经 `KnSetOhosHost` 直接构造枚举写入 `OhosComposeHostRequest.mineRoute`——三端**不可能漂移**，因为枚举只有这一个定义点。

---

## 6. 跨功能屏：CrossFeatureScreenPort

### 6.1 端口定义（`:component`）

`component/src/commonMain/.../routing/CrossFeatureScreenPort.kt`：
- `public interface CrossFeatureScreenPort`（`:18-49`）6 个成员：`handlesSiblingScreen(route): Boolean`（`:24`）、`@Composable RenderSiblingScreen/ContentRouteScreen/MineRouteScreen/HomeRouteScreen(route, onBack, onNavigate)`（`:27-40`）、`@Composable SwitchStoreDialog(selectedId, onDismiss, onPicked)`（`:42-48`）。头注释（`:5-16`）：渲染表住在允许 import 一切 feature 的装配层，feature 只能经此端口回来渲染，镜像 `NetworkFacade` 运行时注入与 `AuthSessionBootstrap.install()` 的装配时机；路径字符串仍归 `core.router`，端口不引入新路径。
- `public object CrossFeatureScreens`（`:55-116`）：`@Volatile installed`（`:57-58`）、`install(port)`（`:60-62`）、`require()` 未装机直接 `error(...)`（`:64-68`）、5 个同名 facade（`:70-115`，其中 `RenderSiblingScreen/ContentRouteScreen/MineRouteScreen/HomeRouteScreen` 的 `onNavigate` 默认 `{}`）。

### 6.2 实现方与 feature 侧消费点（feature→feature 如何被端口拆开）

| 消费方 | file:line | 用途 |
|---|---|---|
| `:feature:mine` | `MineRouteHost.kt:79-80` | mall 表里遇 `/classroom /video /pay/membership` → 交装配层渲染兄弟屏 |
| `:feature:content` | `ContentRouteHost.kt:18` | 整表外包（content 无自己的屏） |
| `:feature:shell` | `SecondaryRouteIsland.kt:92` | Mine 二级岛把渲染权交 `MineRouteScreen` |
| `:feature:home` | `HomeScreen.kt:87,92`（legacy 共享首页内跳 Content/Mine）、`:147`（门店切换弹窗 `SwitchStoreDialog`） | Home 根内跨域 |
| `:feature:mine` | `MineScreen.kt:81,86` | Mine 根内跳 Home/Content host 屏 |

规则闭环：feature 源码 import 只允许出现 `component.routing.CrossFeatureScreens` 与 `core.router.*Routes`；`scripts/check-layer-deps.sh` 第 1/3 轴（`:18-28`、`:42-49`）保证没有 feature 直接 import/依赖兄弟 feature——跨功能跳转的知识被拆成两半：**路径知识在 `core:router`（所有人可见），渲染知识在 `composeApp` 的 AppCrossFeatureScreens（只有装配层可见）**。

---

## 7. ShellBootstrap 做了什么（按代码，不按传闻）

执行顺序（`ShellBootstrap.kt:20-25`）：

1. `installed` volatile 幂等闸（`:17-18,21-22`）——可被任意线程先后触碰（AppShell DisposableEffect、MainActivity.onCreate、iOS 每个 VC 工厂、OHOS 每个 @CName 入口），重复调用为 no-op。
2. `AuthSessionBootstrap.install()`（`feature/auth/AuthSessionBootstrap.kt:18-27`，同样幂等）：
   - `AuthSessionState.sync()`（`:21`）——从 `AccountFacade.current()` 拉会话（`core/account/AccountFacade.kt:44-47` → `ensureHydrated:178-188` 首访反序列化 `demo_account` store、若登录则 `NetworkFacade.bindAccessToken`）并双写到 `isLoggedIn/displayName/_snapshot`（`component/auth/AuthSessionState.kt:34-37,43-47`）。这就是「会话水合」。
   - `NetworkFacade.setAuthRefreshSupport(AuthTokenRefresh.policy, AuthTokenRefresh.handler, onSessionCleared)`（`:22-26`）——启用 401 静默刷新独占流（Flutter `AuthTokenRefreshInterceptor` 语义；`NetworkFacade.kt:57-66,93-106`：启用后平台 client 不再直接触发 `tokenExpiredHandler`，避免「先跳登录再刷新成功」）。
3. `CrossFeatureScreens.install(AppCrossFeatureScreens)`（`:24`）——装配跨功能渲染表。

**它不做什么**（任务假设纠偏）：不做深链接（deep-link 由各平台入口 `acceptDeepLink`/`AcceptDeepLinkFromIos`/`snapshotFlow` 处理，§5.2-D）、不做隐私同意（隐私门在各壳自己：`AppShell.kt:101-127`、`NativeAndroidApp.kt:53-73`、iOS `ContentView.swift:62-96` UserDefaults `privacy_accepted`、Harmony ArkTS 自理）、不注册推送（`NativeAndroidApp.kt:66-68` 在隐私同意后单独做）。

---

## 8. 测试与真实状态；MODULES.md 勘误清单

### 8.1 commonTest 真实状态

| 文件 | 内容 | 判定 |
|---|---|---|
| `composeApp/src/commonTest/kotlin/com.example.my_kmp_project/ComposeAppCommonTest.kt:6-11` | 唯一用例 `example()`：`assertEquals(3, 1 + 2)` | KMP 模板脚手架残留，**不是** app 层测试 |
| `composeApp/src/commonTest/kotlin/com.example.my_kmp_project/feature/`（空目录） | 无任何 .kt | 历史拆模块迁移残留 |

即装配层（AppContainer/ShellBootstrap/AppCrossFeatureScreens/AppShell）**当前零测试**；路由分发逻辑的测试下沉在 `:core:router`（`core/router/src/commonTest/.../ProductRouteDispatchTest.kt`，本次未展开）。`composeApp/build.gradle.kts:183-188` 只声明了 kotlin-test。

### 8.2 MODULES.md 及关联文档勘误清单（与代码不符之处）

| # | 文档原话（file:line） | 代码实证 | 结论 |
|---|---|---|---|
| E1 | `MODULES.md:1` 标题「Module map (29 Gradle projects)」 | `settings.gradle.kts:23-44` include 30 个子工程（composeApp 1 + core 10 + component 1 + feature 17 + ohosAggregate 1），含 root 共 31；`AGENTS.md` 地图又写「Full 28-module graph」 | 29/28 均为旧计数；口径需统一（建议写「30 子工程，:ohosAggregate 为占位」） |
| E2 | `MODULES.md:41` `:composeApp` 依赖「上述全部 + core.analytics」 | `composeApp/build.gradle.kts:122-130` 只 implementation 9 个 core——**没有 `projects.core.database`**（analytics 本就在「上述」内，不构成「+」） | 表述与代码不符：composeApp 不依赖 :core:database；analytics 列举多余 |
| E3 | `MODULES.md:12` 称 composeApp 含「RouteHost 分发」 | composeApp commonMain 没有任何名为 RouteHost 的类型；分发实体是 `app/AppCrossFeatureScreens.kt:29-108`、`androidMain/NativeAndroidMain.kt:545-757`、ohosMain 岛选择器 | 表述易误导：RouteHost 本体在 :feature:*，装配层持有的是「渲染表 + overlay 状态机」 |
| E4 | `core/router/.../FeatureRoutes.kt:11-13` 文件头「Stays inside `:composeApp` — the `core/…` Gradle split was reverted」 | 该文件物理位于 `core/router/` 模块内（`settings.gradle.kts:26` include :core:router） | 陈旧注释（拆模块回退只发生在早期）；代码注释与 MODULES.md 的模块化叙事互相矛盾，应以目录+settings 为准 |
| E5 | `MODULES.md:56` `chartsVicoMain`「现属 :feature:home」；`composeApp/build.gradle.kts:112-115` 称 composeApp 不再建中间 source set | `composeApp/src/chartsVicoMain/` 目录树仍在但 0 个 .kt | 文档正确、磁盘残留空目录，建议清理，否则目录列表误导 |
| E6 | `AGENTS.md`「Done when」/中间 source set 表把 `networkKtorMain/settingsMain/imageCoilMain/uuidCpfMain` 列在概览里 | 这些 source set 挂在各 core 模块（如 `composeApp/build.gradle.kts:113-114` 下沉说明），composeApp 侧不存在 | 属 AGENTS.md 表述含糊（不算硬错误），读 AGENTS.md 时须结合 112-115 注释 |
| E7 | `AGENTS.md` Compose 节「`DemoApp` Scaffold: containerColor=Transparent, contentWindowInsets=WindowInsets(0)」，且把 `App/ShellBootstrap/AppCrossFeatureScreens` 列为运行时装配 | 该 Scaffold 配置在 `AppShell.kt:342-347`（legacy 壳）；`DemoApp.kt:9-12` 只是无调用方的别名；Android/iOS/Harmony 主路径（`MainActivity.kt:39`、`ContentView.swift`、`Index.ets`）都不经 DemoApp | 沉浸式规范只对 legacy 共享壳成立；三端原生壳各自有等效实现（如 `NativeAndroidMain.kt:669-676`） |
| E8 | `MODULES.md:4`「OHOS 仍只有一条 libkn.so，二进制产出方只有 :composeApp」 | 与 `composeApp/build.gradle.kts:51-92,259-300`、`ohosAggregate/build.gradle.kts:1-13`（placeholder）一致 | 正确，无需改 |
| E9 | 装配层自身「未验证/半装机」符号（不算文档错误，但读文档时易高估） | `AppRoute.Splash/PrivacyConsent/Login/Register/MineAbout/Tab`、`AppNavigator.openTab/popToRootTab`（`AppNavigator.kt:34-36`）、`AppContainer.network/pay/chat/session`、`NavigationComponent.navigatorOrNull/consumeDeepLink`、`Platform.getPlatform`、`ShellOverlay.Membership` 渲染分支（`NativeAndroidMain.kt:601-603` 无任何赋值点）、deprecated `MainViewController`（`iosApp` 无 Swift 调用方）、`AppAndroidPreview`（`MainActivity.kt:65-68`，无 `@Preview` 注解且无调用方）、`KnAudioOnPageHide`（`MainArkUIViewController.kt:317-319` 空实现，但 cpp 钩子已接通 `napi_init.cpp:40`） | 均为「存在但无消费方/无行为」的预留或死代码；文档若声称它们在用，需修正 |

### 8.3 真实主路径 vs Legacy（一图流结论）

| 入口 | 状态 | 证据 |
|---|---|---|
| Android：MainActivity → NativeAndroidApp → NativeAndroidMain | **运行时主路径** | `MainActivity.kt:38-40`；`AppShell.kt:96-97` 注释自认 legacy |
| iOS：SwiftUI ContentView（tab 根原生；Mine 岛与非 Mine Mine 属主二级进 Compose；其余原生页） | **运行时主路径** | `ContentView.swift:81-115,341-451`；`MainViewController.kt:16-21` legacy 全 App 入口已 `@Deprecated` |
| Harmony：ArkTS Index.ets（tab 根/多数二级原生；Mine 岛 + Mine 属主兜底进 Compose 岛） | **运行时主路径** | `Index.ets:826-1015`；`MainArkUIViewController.kt:28-31` 注释「ArkTS owns splash/tabs/Mine root/auth」 |
| 共享 Compose AppShell（DemoApp→AppShell 全功能四 Tab 壳） | 参考保留（ADR 0002 Legacy Shared Compose） | `AppShell.kt:93-98`；可达入口仅剩 deprecated `MainViewController()` |

---

### 附：本文档未逐行展开的部分（边界声明）

- `NativeJetpackTabs.kt`（1292 行）只核对了三个 Tab 根函数的位置与回调签名（`:86/:840/:1267`），内部业务屏细节属 Tab 分册；`feature/home/HomeRouteHost.kt` 的 26 个私有屏实现、`MineRouteHost.kt:138-1643` 私有屏实现只读全但此处仅列表级结论。
- `AuthTokenRefresh`（:feature:auth 内部）与 `RealtimeSeqStore` 等 realtime 子系统不属本册。
- iOS `NativeSecondaryPages.swift` / Harmony `NativeSecondaryPages.ets` 的逐页内容以两个 dispatch 脚本为守护边界，本册只引用其结构。
- `napi_init.cpp`（349 行）与 `types/libentry/Index.d.ts` 只核对了符号注册/extern 声明/调用行（`:21-36,44-58,60-90,115,143-160,277-304,313-316`），未整篇展开；`NativeAuthPages.swift` 只经 grep 确认 8 个 Auth 调用行（`:311,335,348,428,461,475`），其 UI 逻辑属 auth 分册。
- 本文所有行号基于当前工作区快照逐篇复读核定（§3.7 曾修正 `SecondaryRouteViewController` 装机行号 52→53）；任何入口文件再编辑后需同步复核 §2 行数与 §3.7-§3.9 行号。

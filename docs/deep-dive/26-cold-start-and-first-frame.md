# 26 · 三端冷启动与首帧流程（Android、iOS、HarmonyOS）

## 0. 本文要回答的唯一问题

**唯一问题**：用户点下图标之后，三端各自「从上电到第一帧可见」这条时间轴上，代码到底按什么顺序被执行；哪些装配发生在首帧之前、哪些被推到首帧之后；三端在阻塞点、可中断点、白屏/黑屏出现段落上的差异具体落在哪一行。

本文只管时序。壳的文件清单、符号与 ABI 见 `docs/deep-dive/23-native-shells.md`；装配层的依赖注入与路由表见 `docs/deep-dive/22-composeapp-assembly-routing.md`；本文不重复它们的清单。

### 0.1 实测方法（下列依据全部由本次实际执行得到）

- 逐文件 Read（全文，带行号核对）：`composeApp/src/commonMain/kotlin/com.example.my_kmp_project/App.kt`、`composeApp/src/commonMain/kotlin/com.example.my_kmp_project/app/AppContainer.kt`、`app/AppShell.kt`、`app/DemoApp.kt`、`app/ShellBootstrap.kt`、`composeApp/src/androidMain/kotlin/com.example.my_kmp_project/MainActivity.kt`、`nativeshell/NativeAndroidApp.kt`、`nativeshell/NativeAndroidMain.kt`、`composeApp/src/androidMain/AndroidManifest.xml`、`composeApp/src/androidMain/res/values/themes.xml`、`res/values/colors.xml`、`res/drawable/splash_screen.xml`、`res/xml/network_security_config.xml`、`iosApp/iosApp/iOSApp.swift`、`iosApp/iosApp/ContentView.swift`、`iosApp/iosApp/Info.plist`、`iosApp/iosApp/LaunchScreen.storyboard`、`composeApp/src/iosMain/kotlin/com.example.my_kmp_project/MainViewController.kt`、`harmonyApp/AppScope/app.json5`、`harmonyApp/entry/src/main/module.json5`、`harmonyApp/entry/src/main/ets/entryability/EntryAbility.ets`、`harmonyApp/entry/src/main/ets/pages/Index.ets`、`harmonyApp/entry/src/main/resources/base/profile/main_pages.json`、`composeApp/src/ohosMain/kotlin/com.example.my_kmp_project/MainArkUIViewController.kt`。
- 补充 Read（启动链下游）：`core/storage/src/commonMain/kotlin/com.example.my_kmp_project/core/storage/KeyValueStore.kt` 与三端 actual、`core/account/.../AccountFacade.kt`、`AccountSessionStore.kt`、`PrivacyConsentStore.kt`、`feature/auth/.../AuthSessionBootstrap.kt`、`component/auth/AuthSessionState.kt`、`core/network/.../NetworkBootstrap.kt`、`NetworkFacade.kt`、`feature/shell/.../SoftAuthPresenter.kt`、`core/router/.../DeepLinkRouter.kt`、`DeepLinkEntry.kt`、`core/ui/.../MainTabChrome.kt`、`component/realtime/RealtimeStartupGate.kt`、`AppRealtimeClientImpl.kt`、`harmonyApp/entry/src/main/ets/auth/SessionStore.ets`、`harmonyApp/entry/src/main/cpp/napi_init.cpp`、`harmonyApp/entry/src/main/cpp/CMakeLists.txt`。
- Grep 取证（结论见正文）：`ReportMainTabRoot`、`LocalMainTabChrome`（仅 1 个 provider）、`navigation.bind`（仅 1 个调用点）、`AppRealtimeClientImpl(`、`RealtimeStartupGate(`、`tryConnectIfReady`、`registerForPush()`、`privacy_accepted`、`configureDemoHttpClient`、`PREF_NAME`、`ComposeView`（在 `iosApp/` 下 0 命中）、`ignoresSafeArea`、`UILaunchStoryboardName`。
- Bash 核对：`wc -l` 各文件总行数（决定可引用的最大行号）、`ls` 三端资源目录、`grep` 取 `iosApp/iosApp.xcodeproj/project.pbxproj` 的 plist 生成键。
- 门禁自跑：`python3 _verify/verify_docs.py 26-cold-start-and-first-frame.md`、`python3 _verify/check_markdown.py`。

**耗时口径**：本文**没有任何实测毫秒数**（未启动真机、未跑 profiler）。所有出现的时长都是源码里写死的常量（闪屏驻留时长、心跳周期、超时上限），文中一律标注为「源码常量」；凡是真实耗时的一律写「未实测」。

### 0.2 三端时序速览（先看形状，再看行号）

| 阶段 | Android | iOS | HarmonyOS |
| --- | --- | --- | --- |
| 谁拉起进程 | 系统按 Manifest 启动 `.MainActivity`（`composeApp/src/androidMain/AndroidManifest.xml:17-25`） | SwiftUI `@main`（`iosApp/iosApp/iOSApp.swift:23-24`） | `EntryAbility`（`harmonyApp/entry/src/main/module.json5:40-41`） |
| 系统级画面 | activity 主题窗口背景（`composeApp/src/androidMain/res/values/themes.xml:8`） | `LaunchScreen.storyboard`（`iosApp/iosApp/LaunchScreen.storyboard:2`） | StartWindow（`harmonyApp/entry/src/main/module.json5:45-46`） |
| Kotlin 首次醒来 | `MainActivity.kt:28` 与 `:35` | `ContentView.swift:65-66` → `composeApp/src/iosMain/kotlin/com.example.my_kmp_project/MainViewController.kt:73-77` | 未醒来（见 §4.1 步 11） |
| 壳代码首帧 | `MainActivity.kt:38-40` | `ContentView.swift:81-99` | `harmonyApp/entry/src/main/ets/pages/Index.ets:1934-1961` |
| 壳内闪屏 | `NativeAndroidApp.kt:77-97` | `ContentView.swift:84-90` | `Index.ets:1143-1154` |
| 隐私门 | `NativeAndroidApp.kt:62-71` | `ContentView.swift:91-96` | `Index.ets:1156-1178` |
| Compose 首帧 | 与壳同树（`NativeAndroidMain.kt:669-754`） | 岛（`ContentView.swift:435-442`） | 岛（`Index.ets:1963-1986`） |
| 冷启动耗时 | 未实测 | 未实测 | 未实测 |

---

## 1. 三端共同骨架：装配层在首帧前做了什么

三端入口不同，但它们共享同一段装配代码：`AppContainer`、`ShellBootstrap`、会话水合、隐私旗标读取。先把这段骨架钉死，后面三章只讲「谁在什么时刻触发它」。

### 1.1 `AppContainer` 里「谁 new 谁」——按源码顺序

构造声明：`composeApp/src/commonMain/kotlin/com.example.my_kmp_project/app/AppContainer.kt:18-23`，四个子组件按**主构造函数参数从左到右**逐个求值（Kotlin 默认参数按声明顺序初始化），顺序即：

1. `NetworkComponent()`（`AppContainer.kt:19` 声明、类体 `:30-32`）——里面只有一个 `fun api() = NetworkFacade.api()`，**构造期不 new 任何 HTTP client**。
2. `SessionComponent()`（`:20` 声明、`:34-36`）——空壳，注释写明会话 SoT 仍是 `AuthSessionState`。
3. `BridgeComponent()`（`:21` 声明、`:38-43`）——按字段顺序 new：`push = StubPushBridge`（`:39`，桩定义 `component/src/commonMain/kotlin/com.example.my_kmp_project/component/push/PushBridge.kt:46`）、`pay = FlaggedPayGateway()`（`:41`，类 `component/src/commonMain/kotlin/com.example.my_kmp_project/component/pay/PayGateway.kt:101`，默认走沙箱开关 `:48-51`）、`chat = StubChatPort`（`:42`）。**pay 网关是容器构造期唯一真正被 new 出来的业务对象。**
4. `NavigationComponent()`（`:22` 声明、`:45-57`）——`navigator` 初值为 null（`:46`），要等 `bind()` 被调用才有实例（`:48-52`）。

单例与首次触达：`private val instance: AppContainer by lazy { AppContainer() }`（`AppContainer.kt:25`）+ `get()`（`:26`）。全仓首帧前触达 `AppContainer.get()` 的位置有三处：共享 Compose 壳 `App.kt:11`、Android Jetpack 壳 `composeApp/src/androidMain/kotlin/com.example.my_kmp_project/nativeshell/NativeAndroidApp.kt:52`、Android tab 根 `NativeAndroidMain.kt:86`。

容器与网络 client 的真实关系：`NetworkFacade` 是 object，字段初始化就建 client（`core/network/src/commonMain/kotlin/com.example.my_kmp_project/core/network/NetworkFacade.kt:26-32`，`rawClient` 直接调 `createPlatformApiClient(...)`）。所以 client 的构造时刻 = **第一次碰到 `NetworkFacade` 的时刻**，而不是容器构造时刻。三端都由 `platformNetworkBootstrap()` 触发：`core/network/src/commonMain/kotlin/com.example.my_kmp_project/core/network/NetworkBootstrap.kt:11-23`（末尾 `:22` 调 `NetworkFacade.rebuildClient()`）。三端 actual 只是转调：`core/network/src/androidMain/kotlin/com.example.my_kmp_project/core/network/NetworkBootstrap.android.kt:11-13`、`core/network/src/iosMain/.../NetworkBootstrap.ios.kt:18-20`、`core/network/src/ohosMain/.../NetworkBootstrap.ohos.kt:11-13`。引擎差异在工厂里：Android `HttpClient(OkHttp)`（`core/network/src/androidMain/.../HttpClientFactory.android.kt:10`）、iOS `HttpClient(Darwin)`（`core/network/src/iosMain/.../HttpClientFactory.ios.kt:10`）、ohosArm64 `HttpClient(CIO)`（`core/network/src/ohosArm64Main/.../HttpClientFactory.ohosArm64.kt:11`）、ohosX64 回落 cinterop client（`core/network/src/ohosX64Main/.../HttpClientFactory.ohosX64.kt:9`，无 Ktor）。配置装配函数：`core/network/src/networkKtorMain/kotlin/com.example.my_kmp_project/core/network/HttpClientDefaults.kt:49-51`。

### 1.2 `ShellBootstrap.install()`：一次幂等装机，六个调用点

`composeApp/src/commonMain/kotlin/com.example.my_kmp_project/app/ShellBootstrap.kt:15-25`：`@Volatile installed`（`:17-18`）保证只跑一次，内容为两行——`AuthSessionBootstrap.install()`（`:23`）与 `CrossFeatureScreens.install(AppCrossFeatureScreens)`（`:24`，实现对象 `composeApp/src/commonMain/kotlin/com.example.my_kmp_project/app/AppCrossFeatureScreens.kt:29`）。

`AuthSessionBootstrap.install()`（`feature/auth/src/commonMain/kotlin/com.example.my_kmp_project/feature/auth/AuthSessionBootstrap.kt:18-27`）做两件事：`AuthSessionState.sync()`（`:21`，实现 `component/src/commonMain/kotlin/com.example.my_kmp_project/component/auth/AuthSessionState.kt:34-37`，内部就是 `AccountFacade.current()`）+ 把 401 静默刷新三件套注册进网络层（`:22-26`，`NetworkFacade.setAuthRefreshSupport` 定义 `NetworkFacade.kt:57-66`，注意它**又会 rebuild 一次 client**）。

调用点（首帧前/后的边界都在这里）：

| 调用点 | 位置 | 相对首帧 |
| --- | --- | --- |
| Android `MainActivity.onCreate` | `composeApp/src/androidMain/kotlin/com.example.my_kmp_project/MainActivity.kt:35` | setContent（`:38`）之前 |
| 共享 Compose 壳 `AppShell` | `composeApp/src/commonMain/kotlin/com.example.my_kmp_project/app/AppShell.kt:105-109` | 组合期 `DisposableEffect` |
| iOS 全 App 入口（已废弃） | `composeApp/src/iosMain/kotlin/com.example.my_kmp_project/MainViewController.kt:19` | `ComposeUIViewController`（`:20`）之前 |
| iOS 岛入口 | `MainViewController.kt:30`、`:53` | 建 controller 之前 |
| iOS Swift 调用Auth 桥 | `MainViewController.kt:73-77`（`ensureIosAuthReady`），被 `:79-82` 等 8 个导出函数调用 | Swift 属性初始化期 |
| OHOS 岛入口 | `composeApp/src/ohosMain/kotlin/com.example.my_kmp_project/MainArkUIViewController.kt:41`、`:150-154` | `ComposeArkUIViewController`（`:43`）之前 |

### 1.3 会话与隐私同意从 KV 恢复的确切时刻

契约只有一个：`core/storage/src/commonMain/kotlin/com.example.my_kmp_project/core/storage/KeyValueStore.kt:28-58`（`expect fun createKeyValueStore(name)` 在 `:58`）。三端 actual：Android 需要 context（`core/storage/src/androidMain/kotlin/com.example.my_kmp_project/core/storage/KeyValueStore.android.kt:12-16`，缺 context 直接 `error`，见 `core/storage/src/androidMain/.../AndroidAppContext.kt:26-27`）、iOS 用 `NSUserDefaults(suiteName = name)`（`core/storage/src/iosMain/.../KeyValueStore.ios.kt:14-15`）、OHOS 两端都用 `OH_Preferences` NDK 且句柄按名字缓存（`core/storage/src/ohosMain/.../KeyValueStore.ohos.kt:41-42`、缓存表 `:52-78`）。

会话水合链（首次读即水合，之后不再读盘）：`AccountFacade.current()`（`core/account/src/commonMain/kotlin/com.example.my_kmp_project/core/account/AccountFacade.kt:44-47`）→ `ensureHydrated()`（`:178-188`）→ 懒建 store（`:42`）→ `createAccountSessionStore()`（`core/account/src/commonMain/.../AccountSessionStore.kt:24-25`，store 名 `:20` = `demo_account`、键名 `:22` = `demo_account_user_json`）→ 解码后若已登录**立刻把 token 绑到网络层**（`AccountFacade.kt:184-186`）。这意味着：谁第一个调用 `AccountFacade.current()`，谁就承担第一次磁盘读；三端分别是 `ShellBootstrap.install()`（Android）与 Swift 属性初始化（iOS）与 ArkTS 灌回（OHOS，见 §4）。

隐私同意链（Kotlin 侧）：`core/account/src/commonMain/kotlin/com.example.my_kmp_project/core/account/PrivacyConsentStore.kt:19-20`（复用同一个 `demo_account` store）、键名 `:17` = `demo_privacy_accepted`、读写 `:25-29`。消费点：共享壳 `AppShell.kt:101-103`（先读、splash 结束再判分支 `:112-127`）、Android 壳 `NativeAndroidApp.kt:53-55`、`:58-61`。

**三端在此处分叉**（关键差异，别当统一实现看）：iOS 壳读的是 `UserDefaults.standard` 的 `privacy_accepted`（`iosApp/iosApp/ContentView.swift:63`，写入 `:93`），Harmony 壳读的是 ArkTS `preferences` 里 `privacy_accepted`（`harmonyApp/entry/src/main/ets/pages/Index.ets:229-232`，写入 `:1169`）。也就是说同意旗标存在三套互不相通的介质里，同一台设备上 Kotlin 与壳可能读到不同结果。存储维度的对照表已由 `docs/deep-dive/18-feature-auth-account-friend.md:581-583` 记录，本文不重复。

### 1.4 心跳与实时连接的启动时机（诚实结论：冷启动路径上没有它们）

设计意图在闸门里：`component/src/commonMain/kotlin/com.example.my_kmp_project/component/realtime/RealtimeStartupGate.kt:59-100` 的 `tryConnectIfReady()` 检查顺序是**隐私 → 登录 → 是否已活跃**（`:68-80`），首连超时是源码常量 12s（`:83`、`:185`），连上后固定订阅 `sys.notify` 与 `presence.bulk`（`:86`、`:39`），再等 8s 只为打日志（`:88`、`:188`）；注释明确写「不得阻塞 runApp」（`:21-28`）。心跳由 `startHeartbeat()` 在收到 `auth_ok` 帧后启动（`component/src/commonMain/kotlin/com.example.my_kmp_project/component/realtime/AppRealtimeClientImpl.kt:272-282`，真正调用在 `:281`；调度器类 `component/src/commonMain/kotlin/com.example.my_kmp_project/component/realtime/HeartbeatScheduler.kt:18`；周期常量 25s 与 10s 在 `core/network/src/commonMain/kotlin/com.example.my_kmp_project/core/network/realtime/RealtimeConfig.kt:28-29`）。

但本次 Grep 全仓（排除 `build/`）得到的事实是：`AppRealtimeClientImpl(` 与 `RealtimeStartupGate(` **只在测试里被构造**（`component/src/commonTest/kotlin/com.example.my_kmp_project/component/realtime/AppRealtimeClientImplTest.kt:74`、`component/src/commonTest/kotlin/com.example.my_kmp_project/component/realtime/RealtimeStartupGateTest.kt:46`、`:57`、`:245`），`tryConnectIfReady` 的调用点全部落在闸门自身与测试（`RealtimeStartupGate.kt:104`、`:110`、`:124`；`RealtimeStartupGateTest.kt:79`）。同一条结论在 `docs/deep-dive/15-component-ports.md:668` 已经记录过。

因此对「首帧」的正确表述是：**冷启动序列里不存在 WebSocket 连接、也不存在应用层心跳**；两者目前只有实现与测试，没有装配点。首帧后唯一真实的异步注册动作是推送：`AppContainer.get().bridges.push.registerForPush()`，且只在 Android 隐私同意后触发一次（`composeApp/src/androidMain/kotlin/com.example.my_kmp_project/nativeshell/NativeAndroidApp.kt:63-71`，调用行 `:67`，被 `runCatching` 包住）。桩实现会调 `PlatformPushSdk.fetchRegistrationToken()`（`component/src/commonMain/kotlin/com.example.my_kmp_project/component/push/PushBridge.kt:49-58`），三端 actual 目前恒返回 null（同 `docs/deep-dive/15-component-ports.md:224`）。iOS 与 Harmony 的壳没有等价调用（Grep `registerForPush()` 仅 1 个生产命中）。

### 1.5 首帧前的对象图：eager 与 lazy 的界线

时序上「谁先被 new」决定冷启动开销分布。本次读到的实际界线如下（左列 eager、右列 lazy 或按需）：

| eager（构造即存在） | 依据 | lazy（首次触达才存在） | 依据 |
| --- | --- | --- | --- |
| `NetworkFacade.rawClient` 与装饰后的 `client` | `core/network/src/commonMain/kotlin/com.example.my_kmp_project/core/network/NetworkFacade.kt:26-32` | `AppContainer` 单例 | `composeApp/src/commonMain/kotlin/com.example.my_kmp_project/app/AppContainer.kt:25` |
| `FlaggedPayGateway()`（容器字段） | `AppContainer.kt:41` | `AccountFacade.store`（首个 KV 视图） | `core/account/src/commonMain/kotlin/com.example.my_kmp_project/core/account/AccountFacade.kt:42` |
| `StubPushBridge._token`（StateFlow） | `component/src/commonMain/kotlin/com.example.my_kmp_project/component/push/PushBridge.kt:47` | `AccountFacade.hydrated` 之后的那次磁盘读 | `AccountFacade.kt:40`、`:178-188` |
| `AuthSessionState._snapshot` | `component/src/commonMain/kotlin/com.example.my_kmp_project/component/auth/AuthSessionState.kt:24` | `NavigationComponent.navigator`（要 `bind()`） | `AppContainer.kt:46`、`:48-52` |
| `DeepLinkRouter.pendingDeepLink`（Compose state） | `core/router/src/commonMain/kotlin/com.example.my_kmp_project/core/router/DeepLinkRouter.kt:29-30` | OHOS `Preferences` 句柄（按 store 名缓存） | `core/storage/src/ohosMain/kotlin/com.example.my_kmp_project/core/storage/KeyValueStore.ohos.kt:52-55` |
| `NetworkComponent`、`SessionComponent`、`BridgeComponent`、`NavigationComponent` 四个壳 | `AppContainer.kt:19-22` | iOS 岛的 `UIViewController` | `composeApp/src/iosMain/kotlin/com.example.my_kmp_project/MainViewController.kt:33-42` |

两点结论：其一，`NetworkFacade` 是 object、字段直接初始化，所以 `platformNetworkBootstrap()` 与 `setAuthRefreshSupport()` 各触发一次 client 构造（`core/network/src/commonMain/kotlin/com.example.my_kmp_project/core/network/NetworkBootstrap.kt:22`、`NetworkFacade.kt:65`、`:68-74`），首帧前**必然**有两轮 client 装配（成本未实测）。其二，OHOS 的 `ComposeArkUIViewController` 只在岛里出现（`composeApp/src/ohosMain/kotlin/com.example.my_kmp_project/MainArkUIViewController.kt:43`），ArkTS 侧把 controller 存成私有字段、`showIsland` 为真才挂载（`harmonyApp/entry/src/main/ets/pages/Index.ets:174`、`:1963-1986`），所以「Compose 运行时首次真正渲染」在 OHOS 上晚于首帧，且完全由用户操作触发。

---

## 2. Android：从 Activity 被拉起，到 Jetpack 壳第一帧

### 2.1 启动序列（逐步，带 file:line）

| 步 | 事件 | 依据 |
| --- | --- | --- |
| 1 | 系统按 Manifest 的 LAUNCHER intent 拉起 `.MainActivity`，`launchMode=singleTop`、activity 主题 `Theme.DemoApp.Splash` | `composeApp/src/androidMain/AndroidManifest.xml:17-21`（filter `:22-25`） |
| 2 | 窗口在 `onCreate` 之前就用 activity 主题的 `android:windowBackground` 画第一张位图（pre-12 冷启动图） | `composeApp/src/androidMain/res/values/themes.xml:8` 指向 `composeApp/src/androidMain/res/drawable/splash_screen.xml:4-11` |
| 3 | `installSplashScreen()` 安装 Android 12+ SplashScreen API（背景、图标、时长、后续主题） | `composeApp/src/androidMain/kotlin/com.example.my_kmp_project/MainActivity.kt:23` + `themes.xml:3-7`（`postSplashScreenTheme` 在 `:7`） |
| 4 | `enableEdgeToEdge()`，再 `super.onCreate()` | `MainActivity.kt:24`、`:25`（注意：本仓实际顺序是两次调用**都早于** super） |
| 5 | 安装进程级 Context（KV 前置条件）与前场 Activity（支付用） | `MainActivity.kt:26`（`core/storage/src/androidMain/.../AndroidAppContext.kt:21-23`）、`:27`（`core/platform/src/androidMain/kotlin/com.example.my_kmp_project/core/platform/AndroidActivityContext.kt:11-13`） |
| 6 | 网络身份与 client 装配（第一次碰 `NetworkFacade`） | `MainActivity.kt:28` → `core/network/src/androidMain/.../NetworkBootstrap.android.kt:11-13` → `NetworkBootstrap.kt:22` |
| 7 | 用 APK 真实 debuggable 位覆盖 Mock 认证开关 | `MainActivity.kt:32-33`（开关定义 `feature/auth/src/commonMain/kotlin/com.example.my_kmp_project/feature/auth/MockAuthConfig.kt:56`） |
| 8 | 会话水合 + 401 静默刷新装机（幂等） | `MainActivity.kt:35` → `app/ShellBootstrap.kt:20-25` |
| 9 | 消费冷启动 intent 的 deep link（写 pending，不跳转） | `MainActivity.kt:36`、`:59-62` → `core/router/src/commonMain/kotlin/com.example.my_kmp_project/core/router/DeepLinkEntry.kt:9-10` → `DeepLinkRouter.kt:36-40` |
| 10 | `setContent { NativeAndroidApp() }`：Composition 起点，也就是 Compose 首帧起点 | `MainActivity.kt:38-40` |
| 11 | 首帧后：splash 驻留 1200ms（源码常量）→ 隐私页或直接主壳 | `nativeshell/NativeAndroidApp.kt:77-97`（`delay(1200)` 在 `:79`）、相位机 `:58-73` |
| 12 | 主壳首帧：tab 根为 Home、keep-alive 集合只含 Home、`DisposableEffect` 同步会话并注册 H5 bridge | `nativeshell/NativeAndroidMain.kt:100-101`、`:88-98`（`softAuth.sync()` `:89`、`HomeWebHandlers.register()` `:90`、401 处理 `:91-96`） |

`onNewIntent` 与 `onResume` 是启动序列的延伸：singleTop 复用实例时重新 `setIntent` 并再投一次 deep link（`MainActivity.kt:43-47`）；`onResume` 每次重新安装 Activity 引用（`:49-52`）、`onPause` 清空（`:54-57`）。

### 2.2 Manifest 中与启动有关的项

LAUNCHER filter `AndroidManifest.xml:22-25`；自定义 scheme `myai` 的 BROWSABLE filter `:27-32`；https App Link filter（host `app.myai.example`）`:34-42`；`android:theme="@style/Theme.DemoApp"` 在 application 级（`:16`）、activity 级覆盖为 Splash 主题（`:21`）；`android:icon`（`:11`）与 `android:label`（`:12`）决定桌面图标与启动时的应用名；`android:usesCleartextTraffic="true"`（`:14`）与 `android:networkSecurityConfig="@xml/network_security_config"`（`:15`）是一对（后者生效时以配置为准）。INTERNET 等权限 `:4-7`。

### 2.3 themes、colors、splash_screen 各自负责哪一段视觉

- `res/values/themes.xml:3` 继承 `Theme.SplashScreen`，`:4` 背景色、`:5` 动画图标 `@mipmap/launch_image`、`:6` 动画时长写 0（源码常量，交给系统默认）、`:7` `postSplashScreenTheme` 切回 `Theme.DemoApp`、`:9-11` 状态栏与导航栏透明加 `windowDrawsSystemBarBackgrounds`。
- `res/values/themes.xml:14-18` 的 `Theme.DemoApp` 是 post-splash 主题，同样透明系统栏（沉浸式前置条件）。
- `res/values/colors.xml:4` 定义 `splash_background` = `#F7FAFC`，同时被 `themes.xml:4` 与 `res/drawable/splash_screen.xml:5` 使用。
- `res/drawable/splash_screen.xml:4-11` 是 layer-list（纯色底 + 居中 bitmap），文件头注释写明分工：Android 12+ 走 SplashScreen API、pre-12 走这一层（`:2-3`）。
- `res/xml/network_security_config.xml:3` 只有一行 `base-config cleartextTrafficPermitted="true"`：它影响**首帧之后的明文 HTTP**（本地 BFF、局域网调试），不参与首帧。

### 2.4 Compose 岛与 `nativeshell` 的关系（Android 第一帧是谁画的）

`NativeAndroidApp`（`nativeshell/NativeAndroidApp.kt:50-74`）是 androidMain 里的 Jetpack 壳根：相位枚举 `:44`、splash 与隐私 UI `:76-150`、主相位交给 `NativeAndroidMain()`（`:72`）。共享 Compose 只以「Mine 岛」的形式出现在这个壳里：`NativeAndroidMain.kt:546-551` 在 `ShellOverlay.MineIsland` 下渲染 `MineIsland(...)`；其余二级屏走 `HomeRouteHost`（`:561-566`）等 feature composable。也就是说 Android 首帧不是 `App()` 画的：`composeApp/src/commonMain/kotlin/com.example.my_kmp_project/App.kt:9-15` 只被已废弃的 iOS 全 App 入口引用（`composeApp/src/iosMain/kotlin/com.example.my_kmp_project/MainViewController.kt:16-21`），`app/DemoApp.kt:10-12` 本次 Grep 全仓无调用点（仅定义）。`App.kt:11` 那行 `AppContainer.get()` 因此只在 legacy 路径上生效；但 `AppContainer.kt:25` 的 lazy 单例保证任何一条路径都只会构造一次。

首帧之后的两次延时（源码常量，非耗时测量）：todo 条 50ms（`nativeshell/NativeJetpackHome.kt:74-77`）、登录态签到弹窗 400ms（`:78-86`）。deeplink 消费是 `snapshotFlow` 订阅而非启动期一次读取（`NativeAndroidMain.kt:429-443`）。

### 2.5 Android 冷启动其实有「三张画面」，各自负责一段

| 段 | 画的是谁 | 何时消失 | 依据 |
| --- | --- | --- | --- |
| 窗口背景层 | `@drawable/splash_screen`（`#F7FAFC` 底 + 居中 `@mipmap/launch_image`） | 主题生效即存在，pre-12 靠它顶住空窗 | `composeApp/src/androidMain/res/values/themes.xml:8`、`composeApp/src/androidMain/res/drawable/splash_screen.xml:4-11` |
| 系统 SplashScreen 层 | `Theme.DemoApp.Splash` 的 background 与 animatedIcon，动画时长写 0 | `postSplashScreenTheme` 切到 `Theme.DemoApp`（`themes.xml:7`），由 `installSplashScreen()` 驱动 | `composeApp/src/androidMain/res/values/themes.xml:3-7`、`composeApp/src/androidMain/kotlin/com.example.my_kmp_project/MainActivity.kt:23` |
| Compose 闪屏层 | `NativeSplash`：`bg_splash` 铺满 + 96dp `ic_splash_logo` 居中，无文案无按钮 | `delay(1200)` 到点或被 dispose | `composeApp/src/androidMain/kotlin/com.example.my_kmp_project/nativeshell/NativeAndroidApp.kt:77-97`（资源 `:84-95`） |

两点容易被忽略的差异：其一，legacy 壳的 `SplashPhase` 多一个「进入」按钮可提前跳过（`composeApp/src/commonMain/kotlin/com.example.my_kmp_project/app/AppShell.kt:165-167`），驻留常量也不同（1000ms，`AppShell.kt:135`，对照 native 的 1200ms）；native 路径**没有手动跳过点**，因此 Android 唯一「用户可打断首帧」的入口只剩深链。其二，两套闪屏共用同一组资源：`core/design/src/commonMain/composeResources/drawable/bg_splash.png` 与 `ic_splash_logo.png`（Compose 层），Android 层用 `composeApp/src/androidMain/res/mipmap-mdpi/launch_image.png` 等五密度图（同目录 `ic_launcher.png`）。素材分工由 `AGENTS.md:90-92` 记录，本文不重复。

---

## 3. iOS：SwiftUI 壳先出帧，Kotlin 在属性初始化期就已经被调用

### 3.1 启动序列

| 步 | 事件 | 依据 |
| --- | --- | --- |
| 1 | `@main struct iOSApp: App` 是进程入口；`WindowGroup { ContentView() }` 是窗口根 | `iosApp/iosApp/iOSApp.swift:23-24`、`:27-29` |
| 2 | `@UIApplicationDelegateAdaptor` 挂上 AppDelegate（方向锁、URL 打开） | `iOSApp.swift:25`、`:5-12`（orientation）、`:14-20`（`DeepLinkEntryKt.acceptDeepLink`） |
| 3 | 系统先展示 LaunchScreen storyboard，再交棒 SwiftUI | `iosApp/iosApp/LaunchScreen.storyboard:2`（initialViewController）、`:22`（背景色即 `#F7FAFC`）、`:19-26`（居中 LaunchImage 约束）、`:35`（图片资源） |
| 4 | `ContentView` 构造时求值属性默认值 → **第一次进 Kotlin**：`MainViewControllerKt.AuthIsLoggedIn()` | `iosApp/iosApp/ContentView.swift:65-66`、展示名 `:67` → `composeApp/src/iosMain/kotlin/com.example.my_kmp_project/MainViewController.kt:79-82`、`:84-87` |
| 5 | 上述桥函数内部先 `platformNetworkBootstrap()` 再 `ShellBootstrap.install()` | `MainViewController.kt:73-77`（`ensureIosAuthReady`）→ `core/network/src/iosMain/.../NetworkBootstrap.ios.kt:18-20` |
| 6 | 隐私旗标读 `UserDefaults.standard`（不是 Kotlin 的 `demo_account` suite） | `ContentView.swift:63`（写 `:93`） |
| 7 | body 相位机：splash（1.2s，源码常量）→ privacy → main | `ContentView.swift:81-99`（`DispatchQueue.main.asyncAfter` `:86-89`） |
| 8 | main 相位首帧：SwiftUI tab 根 + `NativeBottomBar` | `ContentView.swift:341-398`（tab 分派 `:343-387`、底栏 `:389-397`） |
| 9 | 冷启动补投 pending deep link 与启动参数路由 | `ContentView.swift:105-114`、`:120-130` |
| 10 | Compose 只在用户打开岛时才建 controller | `ContentView.swift:435-442`（`MineIslandHost`）、`:439-441`（`MineSecondaryHost`）、`:443-449`（`NativeFeatureHost`）；host 实现 `:497-503`、`:505-510` → `MainViewController.kt:28-43`、`:51-64` |

### 3.2 Info.plist 与 pbxproj 里跟启动有关的键

手写的 `iosApp/iosApp/Info.plist` 只有三组键：`CADisableMinimumFrameDurationOnPhone`（`:5-6`，放开高刷、影响首帧后的合成节奏，不影响启动顺序）、`CFBundleURLTypes` 注册 scheme `myai`（`:7-17`）、`NSAppTransportSecurity` 且 `NSAllowsArbitraryLoads=false`（`:18-22`）——即 iOS 禁明文，与 Android 的 `res/xml/network_security_config.xml:3` 相反。启动画面与方向相关键**不在** Info.plist 里，而是由工程生成：`iosApp/iosApp.xcodeproj/project.pbxproj:305`（`INFOPLIST_FILE`）、`:309` 与 `:338`（`INFOPLIST_KEY_UILaunchStoryboardName = LaunchScreen`）、`:310-311` 与 `:339-340`（支持方向）。

### 3.3 `MainViewController.kt` 里 `ComposeUIViewController` 之前的初始化

两个仍在用的岛入口都是同一个套路：`MainViewController.kt:29-30`（先 `platformNetworkBootstrap()`、再 `ShellBootstrap.install()`，然后 `:34` 才 `ComposeUIViewController { MineIsland(...) }`）与 `:52-53`、`:55`。全 App 入口 `MainViewController()`（`:17-21`）保留了同样的两步（`:18-19`）但整段被 `@Deprecated` 标注（`:16`），Swift 侧无引用（本次 Grep `iosApp/` 下 `MainViewController()` 0 命中）。

沉浸式：本次 Grep `iosApp/` 下 `ComposeView` **0 命中**，实际写法是三个岛 host 上的 `.ignoresSafeArea(.all)`（`ContentView.swift:437`、`:441`、`:449`），键盘单独处理（`:101`）。`AGENTS.md:159` 那句「`ComposeView().ignoresSafeArea(.all)`」是文档与代码的偏差，照代码为准。

---

## 4. HarmonyOS：StartWindow、ArkTS 首帧，Kotlin 只在岛里醒过来

### 4.1 启动序列

| 步 | 事件 | 依据 |
| --- | --- | --- |
| 1 | 桌面按 bundle 与 ability 图标拉起 `EntryAbility` | `harmonyApp/AppScope/app.json5:3`（bundleName）、`:7-8`（icon、label）；`harmonyApp/entry/src/main/module.json5:6`（mainElement）、`:38-44`（ability 声明、`srcEntry` `:41`、icon `:43`） |
| 2 | 系统 StartWindow（ArkTS 一行代码都还没跑） | `harmonyApp/entry/src/main/module.json5:45`（`startWindowIcon` = `$media:startIcon`）、`:46`（`startWindowBackground` = `$color:start_window_background`）→ 值 `harmonyApp/entry/src/main/resources/base/element/color.json:4-5` = `#F7FAFC` |
| 3 | `onCreate`：设色模式、打 HiLog、暂存 want 的 uri | `harmonyApp/entry/src/main/ets/entryability/EntryAbility.ets:10-18`（`:12` colorMode、`:16` hilog、`:17` `stashDeepLink`），实现 `:24-31`（写 `AppStorage`，键 `:7`） |
| 4 | `onWindowStageCreate` → `loadContent('pages/Index')` | `EntryAbility.ets:37-47`（`:40` 是真正加载页面；成功日志 `:45`） |
| 5 | 首页路由表把 `pages/Index` 排在第一位 | `harmonyApp/entry/src/main/resources/base/profile/main_pages.json:3`（另 `:4` = `pages/WebPage`）；配合 `harmonyApp/entry/src/main/module.json5:12`（`pages` 指向 `$profile:main_pages`） |
| 6 | `Index` 的 `aboutToAppear`：注册 interop、灌 mock 种子、异步读隐私旗标与会话缓存 | `harmonyApp/entry/src/main/ets/pages/Index.ets:140-142`（`@Entry` `@Component` `struct Index`）、`:223-232`（interop `:224-225`、`preferences` 名 `native_shell` `:229`、旗标 `:231`） |
| 7 | 会话恢复：ArkTS 侧 `sharedSessionStore.init/load`，再经 NAPI 灌回 Kotlin | `Index.ets:233-251`（`ApplyAuthSession` `:239-246`）；store 实现 `harmonyApp/entry/src/main/ets/auth/SessionStore.ets:21-23`、`:42-59`、单例 `:86` → Kotlin 侧 `composeApp/src/ohosMain/kotlin/com.example.my_kmp_project/MainArkUIViewController.kt:113-129` |
| 8 | 闪屏驻留 1200ms（源码常量）后切相位并尝试消费 pending deeplink | `Index.ets:143`（初值 `AppPhase.Splash`）、`:252-255`、`:259-271` |
| 9 | ArkTS 首帧：SplashPane 或主 tab 栈 + 49vp 底栏 | `Index.ets:1934-1961`（`SplashPane` `:1143-1154`、`PrivacyPane` `:1156-1176`、底栏 `:1948-1955`） |
| 10 | Compose 岛首次挂载时才 `libentry.MainArkUIViewController()` | `Index.ets:1963-1986`（`Compose({controller, libraryName: 'entry'})` `:1965-1975`）；取符号 `:826-848`（`:836-842`）；libentry 缺失走错误文案 `:828-830` |
| 11 | NAPI 层把调用转发给 Kotlin 导出的 C 符号 | `harmonyApp/entry/src/main/cpp/napi_init.cpp:21-37`（失败时 throw `:28-32`）、导出表 `:309-319`（`MainArkUIViewController` `:313`）；`libkn.so` 按 ABI 选取与链接见 `harmonyApp/entry/src/main/cpp/CMakeLists.txt:14-22`、`:52-53` |
| 12 | Kotlin 侧入口签名与两步初始化 | `MainArkUIViewController.kt:33-34`（`@CName` + `fun MainArkUIViewController(env: napi_env): napi_value`）、`:36` 日志、`:37` `initMainHandler(env)`、`:39` `platformNetworkBootstrap()`、`:41` `ShellBootstrap.install()`、`:43` `ComposeArkUIViewController(env)`、模式分派 `:49-58`、异常吞掉后返回 `null as napi_value`（`:66-70`） |

### 4.2 与 Android、iOS 不同的一处：岛的模式要先用 NAPI 设好

ArkTS 在创建 controller 之前先调 `libentry.SetOhosHost(kind, routeCode)`（`Index.ets:832-834`），二级路由则先 `SetOhosSecondaryRoute`（`:870-875`）；Kotlin 侧对应 `MainArkUIViewController.kt:81-98`（routeCode 到 `MineIslandRoute` 的映射 `:85-96`）与 `:100-106`。因此 OHOS 的「首个 Compose 路由」不是靠 Kotlin 的导航器，而是靠两个全局写入 `OhosComposeHostRequest`（消费点 `MainArkUIViewController.kt:49-58`）。ABI 与符号细节见 `docs/deep-dive/23-native-shells.md`。

一处诚实提示：`harmonyApp/entry/src/main/ets/auth/SessionStore.ets` 里被使用的常量 `PREF_NAME`（`:22`）在本文件 Grep 无声明命中（`sharedSessionStore` 在 `:86`），本次不下结论，只在排障表里把它列为「若 ArkTS 编译失败先看这里」。

---

## 5. 首帧的路由归属：tab 根、二级栈与 `ReportMainTabRoot` 的真实效力

### 5.1 首帧落在哪个 tab 根

| 端 | 初始 tab | 依据 |
| --- | --- | --- |
| 共享 Compose 壳（legacy） | `MainTab.Home` | `composeApp/src/commonMain/kotlin/com.example.my_kmp_project/app/AppShell.kt:225` |
| Android Jetpack 壳 | `MainTab.Home`，且 keep-alive 集合只含 Home | `composeApp/src/androidMain/kotlin/com.example.my_kmp_project/nativeshell/NativeAndroidMain.kt:100-101`（渲染 `:684-752`） |
| iOS SwiftUI 壳 | `MainTab.fromLaunchArguments()`（可用 `-tab chat` 覆盖） | `iosApp/iosApp/ContentView.swift:64`、实现 `:47-56` |
| Harmony ArkTS 壳 | `MainTab.Home` | `harmonyApp/entry/src/main/ets/pages/Index.ets:145`（渲染 `:1941-1955`） |

### 5.2 `ReportMainTabRoot` 只在 legacy 壳里有效（本次最重要的时序结论）

组件契约：`core/ui/src/commonMain/kotlin/com.example.my_kmp_project/core/ui/MainTabChrome.kt:11-19`（controller）、`:21`（`staticCompositionLocalOf` 默认 null）、`:23-30`（`ReportMainTabRoot` 用 `LaunchedEffect` 调 `chrome?.updateBottomBarVisible(isRoot)`）。规则出自 `AGENTS.md:190`：底栏是 tab-root 专属，二级页必须 `ReportMainTabRoot(isRoot = false)`。

但 provider 全仓只有一个：`composeApp/src/commonMain/kotlin/com.example.my_kmp_project/app/AppShell.kt:338-341`（controller 在 `:228` 创建，可见性合算在 `:334-336`，底栏 `:349-355`，内容区 inset `:356-364` 走 `shellContentInsets`）。二级页确实大量调用它（例：`feature/media/src/commonMain/kotlin/com.example.my_kmp_project/feature/media/MusicScreens.kt:58`、`feature/media/src/commonMain/kotlin/com.example.my_kmp_project/feature/media/VideoRouteHost.kt:177`），而这些页面在 Android 首帧路径上是由 `NativeAndroidMain` 渲染的，**composition 里没有 `LocalMainTabChrome`** → `chrome` 为 null → 该调用是 no-op。Android 壳自己用本地状态位控制底栏：声明 `NativeAndroidMain.kt:117`，切换点 `:147-157`、`:159-164`、`:673-675`。iOS 靠 `NativeBottomBar` 的分支（`ContentView.swift:389-397`），Harmony 靠 ArkTS `TabBtn` 行（`Index.ets:1948-1955`）。结论：**首帧的 tab 根与二级栈归属由三端各自的状态位决定，`ReportMainTabRoot` 目前只服务 legacy `AppShell`**。

### 5.3 首个路由解析（deep link）在首帧前后的位置

解析器同一份：`core/router/src/commonMain/kotlin/com.example.my_kmp_project/core/router/DeepLinkRouter.kt:36-40`（`accept` 写 pending）、`:43-47`（`consumePending`）、路径抽取 `:115-149`；平台入口 `core/router/src/commonMain/kotlin/com.example.my_kmp_project/core/router/DeepLinkEntry.kt:9-10`。

- Android：冷启动 intent 在 `setContent` **之前**就写入 pending（`MainActivity.kt:36`、`:59-62`），真正的分派发生在主壳首帧组合后的 `snapshotFlow` 订阅（`NativeAndroidMain.kt:429-443`）。
- iOS：Swift 侧先解析再转成路由串（`ContentView.swift:120-130` → `MainViewController.kt:67-70`），入口两处：`iOSApp.swift:30-32` 与 `ContentView.swift:102-104`；冷启动补投走 `pending_deeplink`（`ContentView.swift:105-114`）。
- Harmony：`onCreate` 只暂存 uri（`EntryAbility.ets:17`、`:24-31`），ArkTS 在闪屏计时器末尾才消费（`Index.ets:254`、`:259-271`），并且消费时会**直接跳过隐私门**（`:267-269`）。
- 装配层的导航器只有 legacy 壳在用：`AppContainer.kt:48-52` 的 `bind()` 唯一调用点是 `AppShell.kt:240`；`AppContainer.kt:56` 的 `consumeDeepLink()` 全仓无调用点（仅定义）。

### 5.4 隐私门是首帧链条上唯一的「用户可停顿点」，四套实现并不一致

| 实现 | 未同意时的界面 | 有无「不同意」出口 | 同意后的副作用 | 落盘 |
| --- | --- | --- | --- | --- |
| legacy `AppShell` | `PrivacyConsentPhase`（`composeApp/src/commonMain/kotlin/com.example.my_kmp_project/app/AppShell.kt:172-221`） | 有按钮但 onClick 为空（`:203-208`，注释 `:204`） | 仅置 `phase = Main`（`:121-125`） | `privacyConsentStore.setAccepted(true)`（`:122`） |
| Android native | `NativePrivacy`（`composeApp/src/androidMain/kotlin/com.example.my_kmp_project/nativeshell/NativeAndroidApp.kt:99-150`） | 有，且变成「重新选择」二次确认（`:101`、`:131-141`） | 写旗标 + `runCatching` 注册推送（`:64-68`） | `demo_privacy_accepted`（`core/account/src/commonMain/kotlin/com.example.my_kmp_project/core/account/PrivacyConsentStore.kt:27-29`） |
| iOS SwiftUI | `PrivacyView`（`iosApp/iosApp/ContentView.swift:463-478`） | **无**，只有「同意并继续」（`:471-473`） | 仅置 `phase = .main`（`:94-95`） | `UserDefaults.standard`（`:93`） |
| Harmony ArkTS | `PrivacyPane`（`harmonyApp/entry/src/main/ets/pages/Index.ets:1156-1178`） | **无**，只有「同意并继续」（`:1165-1172`） | 仅置 `phase = AppPhase.Main`（`:1171`） | `putSync` 加 `flush`（`:1169-1170`） |

时序含义：只有 Android 的同意动作会连带触发一次外部注册（`NativeAndroidApp.kt:66-68`），其余三端同意后不产生任何网络或系统调用；而 Harmony 的深链消费甚至能在隐私页之前把相位直接推到 Main（`Index.ets:267-269`），这是三端里唯一「可绕过隐私门」的启动分支。

---

## 6. 三端差异对照表

| 维度 | Android | iOS | HarmonyOS |
| --- | --- | --- | --- |
| 进程入口 | 系统拉起 `MainActivity`（`AndroidManifest.xml:17-25`） | SwiftUI `@main`（`iOSApp.swift:23-29`） | `EntryAbility`（`harmonyApp/entry/src/main/module.json5:6`、`:41`） |
| 第一张画面 | activity 主题 windowBackground（`themes.xml:8`）+ SplashScreen API（`MainActivity.kt:23`） | `LaunchScreen.storyboard`（`project.pbxproj:309`） | 系统 StartWindow（`harmonyApp/entry/src/main/module.json5:45-46`） |
| 壳内闪屏 | Compose `NativeSplash`，1200ms 常量（`NativeAndroidApp.kt:77-97`） | SwiftUI `SplashView`，1.2s 常量（`ContentView.swift:84-90`） | ArkTS `SplashPane`，1200ms 常量（`Index.ets:1143-1154`、`:252-255`） |
| Kotlin 运行时醒来 | `MainActivity.kt:28` 与 `:35`（`setContent` 之前） | `ContentView.swift:65-66`（Swift 属性初始化，`body` 之前） | 冷启动阶段**未醒来**；`MainArkUIViewController.kt:37-41` 或 `:150-153`（岛打开、ArkTS 灌回 token） |
| 首帧绘制者 | androidMain Jetpack 壳（`NativeAndroidMain.kt:669-754`） | SwiftUI（`ContentView.swift:341-398`） | ArkTS（`Index.ets:1934-1961`） |
| Compose 首帧 | 与壳同树（岛 `NativeAndroidMain.kt:546-551`） | 仅岛 `ContentView.swift:435-449` → `MainViewController.kt:34`、`:55` | 仅岛 `Index.ets:1963-1986` → `MainArkUIViewController.kt:43` |
| 会话恢复 | Kotlin 读 `demo_account`（`AccountFacade.kt:178-188`） | 同 Kotlin（经 `MainViewController.kt:73-77`） | ArkTS `native_shell` 缓存 + 灌回 Kotlin（`Index.ets:233-251`） |
| 隐私旗标 | Kotlin `demo_privacy_accepted`（`NativeAndroidApp.kt:53-55`） | `UserDefaults.standard` `privacy_accepted`（`ContentView.swift:63`） | preferences `privacy_accepted`（`Index.ets:231`） |
| 主要阻塞点 | `NetworkFacade` 两次 `rebuildClient()`（`NetworkBootstrap.kt:22` + `NetworkFacade.kt:65`），全同步、未实测 | 同一次链（`MainViewController.kt:74` 与 `:76`），未实测 | 岛打开时同步 `initMainHandler` 与网络装配（`MainArkUIViewController.kt:37-41`），未实测 |
| 可中断点 | `onDestroy` 前任何一步；deeplink 消费可延迟到主壳订阅（`NativeAndroidMain.kt:429`） | 闪屏 1.2s 内可被 deeplink 抢跑（`ContentView.swift:105-114`） | 闪屏 1200ms 内 `consumePendingDeepLink` 直接置 Main（`Index.ets:267-269`） |
| 白屏/黑屏风险段 | splash 主题与 `postSplashScreenTheme` 衔接处（`themes.xml:7`）；`AndroidAppContext` 未装会抛（`AndroidAppContext.kt:26-27`） | LaunchScreen 交棒 SwiftUI 那一段；岛 `fullScreenCover` 建 controller 失败无回退 UI | StartWindow 交棒 ArkTS 那一段；`libentry` undefined 只有错误文案（`Index.ets:828-830`）；Kotlin 抛错时返回 null（`MainArkUIViewController.kt:66-70`） |
| 冷启动可见耗时 | **未实测** | **未实测** | **未实测** |

### 6.1 首帧前后有没有请求（这条决定「首屏白」是不是网络问题）

逐条核对启动路径上的函数体，结论是**首帧前没有一次 HTTP 请求**：

- `MainActivity.kt:22-41` 的 `onCreate` 里只有安装与装配，无请求调用；`AppShell.kt:105-109` 的 `DisposableEffect` 只调 `ShellBootstrap.install()`。
- `ShellBootstrap.kt:20-25` 两步：`AuthSessionBootstrap.install()`（`feature/auth/src/commonMain/kotlin/com.example.my_kmp_project/feature/auth/AuthSessionBootstrap.kt:21-26`，只做 `sync()` 与注册回调）与 `CrossFeatureScreens.install(...)`（纯 UI 表安装，`composeApp/src/commonMain/kotlin/com.example.my_kmp_project/app/AppCrossFeatureScreens.kt:29`）。
- `HomeWebHandlers.register()` 只往 bridge 注册表里塞两个 lambda（`feature/home/src/commonMain/kotlin/com.example.my_kmp_project/feature/home/HomeWebHandlers.kt:18-30`）。
- `MockAuthConfig` 在 debug 包默认开本地 Mock（`feature/auth/src/commonMain/kotlin/com.example.my_kmp_project/feature/auth/MockAuthConfig.kt:56`，Android 覆盖 `MainActivity.kt:32-33`），所以 debug 首屏连不上 BFF 也不是启动 bug。
- Android 主壳首帧后仍不发请求：`NativeJetpackHome.kt:74-86` 只有 `delay` 与一次本地 `getSharedPreferences("home_check_in", 0)`（`:81`）。
- Realtime 换票与订阅（`component/src/commonMain/kotlin/com.example.my_kmp_project/component/realtime/AppRealtimeClientImpl.kt:117-131`）没有装配点，见 §1.4，所以「首屏慢于网络」在本仓当前不成立。

---

## 7. 排障手册：卡住了看哪个通道、翻哪个文件

### 7.1 日志通道

| 端 | 通道 | 本仓实际存在的日志点 |
| --- | --- | --- |
| Android | Logcat（`adb logcat`） | 启动路径本身没有显式日志调用；可观测信号是崩溃栈（`AndroidAppContext.kt:26-27` 的 `error` 文案）与系统 ActivityTaskManager 记录。业务日志走 Kotlin `println`（如 `RealtimeStartupGate.kt:63-66`），当前 realtime 无装配点 |
| iOS | Xcode Console（stdout） | 同上，Kotlin `println` 落 stdout（`RealtimeStartupGate.kt:69-73`）；启动期无专属 tag，判断依赖相位常量（`ContentView.swift:86-89`） |
| HarmonyOS | `hdc shell hilog` | ArkTS：`EntryAbility.ets:16`、`:38`、`:45`（tag `testTag`）、`Index.ets:846`（tag `Compose`）。C 层：`napi_init.cpp:12`（`LOG_TAG` 定义为 `DemoNapi`）、`:18`、`:22`、`:35`（`OH_LOG_INFO`）。Kotlin：`MainArkUIViewController.kt:36`、`:67-68`（前缀 `DemoKN`）；napi 注释要求按 `DemoKN` 过滤（`napi_init.cpp:27`、`:32`） |

### 7.2 「卡在哪一步 → 看哪个文件」检查表

| 症状 | 第一现场 | 依据 |
| --- | --- | --- |
| Android 一直停在纯色启动图 | 主题链：`res/values/themes.xml:3-8` 与 `res/drawable/splash_screen.xml:4-11`；`installSplashScreen()` 调用位置 `MainActivity.kt:23` | 12 以上由 SplashScreen API 接管、`postSplashScreenTheme` 在 `themes.xml:7` |
| Android 进 App 就崩、日志含 `Call AndroidAppContext.install(context) before using KeyValueStore` | `MainActivity.kt:26` 是否早于 `:35` | `AndroidAppContext.kt:21-27`、`KeyValueStore.android.kt:12-16` |
| Android 每次都重弹隐私页 | KV 名与键：`AccountSessionStore.kt:20`、`PrivacyConsentStore.kt:17`、写入 `:27-29` | 相位判定 `NativeAndroidApp.kt:58-61` |
| Android 冷启动 deep link 不生效 | intent filter `AndroidManifest.xml:27-32`、`onNewIntent` `MainActivity.kt:43-47`、消费 `NativeAndroidMain.kt:429-443` | pending 写入 `DeepLinkRouter.kt:36-40` |
| iOS 停在 LaunchScreen | `LaunchScreen.storyboard:2` 的 initialViewController、pbxproj `:309` 的 `UILaunchStoryboardName` | Info.plist 不含该键（`iosApp/iosApp/Info.plist:4-23`） |
| iOS 首次请求失败但首帧正常 | `Info.plist:18-22` 的 ATS 关闭明文 | 对比 Android `res/xml/network_security_config.xml:3` |
| iOS 隐私页每次都弹 | `ContentView.swift:63`、`:93`（`UserDefaults.standard`） | 与 Kotlin 旗标不同介质，见 `docs/deep-dive/18-feature-auth-account-friend.md:581-583` |
| iOS 打开「我的」岛白屏 | `ContentView.swift:435-442` → `MainViewController.kt:28-43`（`ComposeUIViewController` 之前两步 `:29-30`） | 全 App 入口已废弃 `:16-21` |
| Harmony 只有 StartWindow、进不去页面 | `harmonyApp/entry/src/main/module.json5:45-46` 与 `resources/base/element/color.json:4-5`；`EntryAbility.ets:40-45` 的 `loadContent` 回调日志；`main_pages.json:3` | 权限缺失不影响加载页（`harmonyApp/entry/src/main/module.json5:13-37`） |
| Harmony 报 `libentry.so failed (abi=...)` | `Index.ets:826-831`，ABI 选择 `CMakeLists.txt:14-22` | 需先跑 publish（`CMakeLists.txt:20-23` 的报错文案），产物契约见 `docs/deep-dive/23-native-shells.md` |
| Harmony 报 `MainArkUIViewController missing` | `Index.ets:836-840`、导出表 `napi_init.cpp:313`、Kotlin 符号 `MainArkUIViewController.kt:33-34` | 两端必须同一符号，见 `AGENTS.md` 的 OHOS publish 契约 |
| Harmony ArkTS 里 Kotlin 抛错、界面空白 | `MainArkUIViewController.kt:66-70`（返回 null）→ `napi_init.cpp:26-34` throw；按 `DemoKN` 过滤 hilog | 岛模式未设则落 `else` 分支 `:49-58` |
| 想看「装配到底跑了几次」 | `ShellBootstrap.kt:17-25` 的一次性旗标、`MainActivity.kt:35`、`AppShell.kt:107`、`MainViewController.kt:19`、`MainArkUIViewController.kt:41` | 幂等靠 `@Volatile`，跨端不会重复装 |

### 7.3 建议的复现与观测顺序（不含耗时结论）

1. Android：装 debug 包（`MockAuthConfig` 由 FLAG_DEBUGGABLE 决定，`MainActivity.kt:32-33`），先确认 Logo 底 `#F7FAFC`（`colors.xml:4`）与 Compose 闪屏底图 `bg_splash` 是否同色（`NativeAndroidApp.kt:82-95`），避免误判「白屏」。
2. iOS：用启动参数验证壳路由（`-tab`、`-route`、`-loggedIn`、`-acceptPrivacy`，见 `ContentView.swift:47-56`、`:87`、`:110-113`、`:65`），可直接跳过闪屏与隐私段观察首帧。
3. Harmony：改 Kotlin 后必须重新 publish 再跑（`AGENTS.md` 的 OHOS publish 契约段），否则看到的仍是旧 `libkn.so` 的时序。
4. 三端都不要在首帧找网络证据：启动路径上没有任何请求调用（`MainActivity.kt` 无请求、`ShellBootstrap.kt:20-25` 只装机、tab 根首个动效是本地 delay，`NativeJetpackHome.kt:74-86`）。

### 7.4 不改代码的二分定位：把启动切成四段看

段界点（都在源码里，肉眼可验，不需要计时器）：

1. **系统画面 → 壳闪屏**：Android 看 `themes.xml:8` 的图与 `NativeAndroidApp.kt:82-95` 的图是否同色同图；iOS 看 `LaunchScreen.storyboard:22` 的底色与 `ContentView.swift:454-461` 的 `SplashView` 是否衔接（后者只有文案「正在进入应用...」，`:458`）；Harmony 看 `harmonyApp/entry/src/main/module.json5:45-46` 的 StartWindow 与 `Index.ets:1144-1154` 的 `SplashPane`（文案同样「正在进入应用...」`:1146`）。
2. **壳闪屏 → 隐私门或主壳**：三端都是固定常量（Android 1200ms `NativeAndroidApp.kt:79`、iOS 1.2s `ContentView.swift:86-89`、Harmony 1200ms `Index.ets:252-255`）。停在这一段就说明旗标读取有问题（三处读取点见 §5.4 的落盘列）。
3. **主壳首帧 → tab 内容**：Android 看 `NativeAndroidMain.kt:669-754` 是否进 `AuthOverlay.None` 分支（`:668`）；iOS 看 `ContentView.swift:343-387` 的 `tab` 与 `isLoggedIn`（软门 `:351-365`）；Harmony 看 `Index.ets:1941-1955` 与 `TabContent`。
4. **岛（Compose）**：Android `NativeAndroidMain.kt:546-551`、iOS `ContentView.swift:435-442`、Harmony `Index.ets:1963-1986` 加 `:826-848`。只有第 4 段出问题才需要去看 `libkn.so` 与产物投递（`docs/deep-dive/23-native-shells.md`）。

---

## 8. 未实测清单与证据最弱处

**未实测**：三端冷启动总耗时、闪屏到 tab 根的实际停留、`rebuildClient()` 两次同步构造的成本、LaunchScreen 与 StartWindow 的系统侧驻留、岛打开到 Compose 可见的间隔。文中出现的 `1200`、`1.2`、`12_000`、`8_000`、`25_000`、`10_000` 全部是源码常量（分别是 `NativeAndroidApp.kt:79`、`ContentView.swift:86`、`Index.ets:252-255`、`RealtimeStartupGate.kt:185`、`:188`、`RealtimeConfig.kt:28-29`）。

**证据最弱三处**（按可疑程度排序）：

1. `ReportMainTabRoot` 在三端原生壳里是 no-op —— 依据是「`LocalMainTabChrome` 仅 1 个 provider」这条 Grep（`MainTabChrome.kt:21` 默认 null、`AppShell.kt:339` 唯一 provides）。若某处用了嵌套 `CompositionLocalProvider` 而我没匹配到，这条结论要降级为「部分有效」。
2. 「Android/iOS 的 Kotlin `println` 一定能在 Logcat、Xcode Console 看到」—— 本仓未验证 K/N 各端 stdout 落点，`docs/deep-dive` 也无记录；`AGENTS.md` 只描述 OHOS 用 HiLog。表中已按「未实测」处理。
3. `harmonyApp/entry/src/main/cpp/napi_init.cpp:16-19` 的 `androidx_compose_ui_arkui_init` 在 `Init` 期间被调用的确切时机 —— 我只读到导出表注册与注释（`:309-312`），未追到该函数的实际调用行；因此「libkn.so 在 ArkTS `import libentry from 'libentry.so'`（`Index.ets:7`）时就完成 Compose 运行时初始化」这句是推断，不是行级证据。

---

## 9. 与 `AGENTS.md` 的三处偏差（以代码为准）

| 文档说法 | 代码实况 | 依据 |
| --- | --- | --- |
| `AGENTS.md:159`：iOS 用 `ComposeView().ignoresSafeArea(.all)` | `iosApp/` 全目录 Grep `ComposeView` 0 命中；实际是三个岛 host 上的 `.ignoresSafeArea(.all)` | `iosApp/iosApp/ContentView.swift:437`、`:441`、`:449` |
| `AGENTS.md:190`：二级页必须 `ReportMainTabRoot(isRoot = false)` | 组件存在且二级页确实大量调用，但 provider 只在 legacy `AppShell`，三端原生壳里调用为 no-op | `core/ui/src/commonMain/kotlin/com.example.my_kmp_project/core/ui/MainTabChrome.kt:21`、`:23-30`、`composeApp/src/commonMain/kotlin/com.example.my_kmp_project/app/AppShell.kt:338-341` |
| `AGENTS.md:158`：`installSplashScreen()` → `enableEdgeToEdge()` → `setContent` | 顺序一致，但 `enableEdgeToEdge()` 落在 `super.onCreate()` **之前**，文档未提这一层 | `composeApp/src/androidMain/kotlin/com.example.my_kmp_project/MainActivity.kt:23-25`、`:38` |

另外两条属于「文档没写但会影响读启动代码」的：`composeApp/src/commonMain/kotlin/com.example.my_kmp_project/app/DemoApp.kt:10-12` 已无任何调用点（仅定义），`composeApp/src/commonMain/kotlin/com.example.my_kmp_project/App.kt:9-15` 只被 `@Deprecated` 的 iOS 全 App 入口引用（`composeApp/src/iosMain/kotlin/com.example.my_kmp_project/MainViewController.kt:16-21`）；因此任何按「DemoApp 是入口」写的启动分析都会跑偏，运行时入口分别是 `MainActivity.kt:39`、`ContentView.swift:81` 与 `Index.ets:1934`。

---

## 10. 一页速查（本文全部结论的压缩版）

- Android 首帧前必跑的十步在 §2.1；`setContent` 是 Compose 起点（`composeApp/src/androidMain/kotlin/com.example.my_kmp_project/MainActivity.kt:38-40`），壳闪屏再驻留 1200ms 源码常量（`composeApp/src/androidMain/kotlin/com.example.my_kmp_project/nativeshell/NativeAndroidApp.kt:79`）。
- iOS 的 Kotlin 是在 SwiftUI 属性初始化期醒来的，不是首帧后（`iosApp/iosApp/ContentView.swift:65-66` → `composeApp/src/iosMain/kotlin/com.example.my_kmp_project/MainViewController.kt:73-77`）；LaunchScreen 之后到 SplashView 之间没有任何 Kotlin 日志点。
- Harmony 冷启动全程 ArkTS，Kotlin 与 Compose 只在打开岛或灌 token 时执行（`harmonyApp/entry/src/main/ets/pages/Index.ets:826-848`、`composeApp/src/ohosMain/kotlin/com.example.my_kmp_project/MainArkUIViewController.kt:33-43`）。
- 三端共享的只有 `ShellBootstrap`（`composeApp/src/commonMain/kotlin/com.example.my_kmp_project/app/ShellBootstrap.kt:20-25`）与其下游会话水合（`core/account/src/commonMain/kotlin/com.example.my_kmp_project/core/account/AccountFacade.kt:178-188`）；隐私旗标不共享（§5.4）。
- 心跳与实时连接不在冷启动时序里：闸门与状态机齐全（`component/src/commonMain/kotlin/com.example.my_kmp_project/component/realtime/RealtimeStartupGate.kt:59-100`、`component/src/commonMain/kotlin/com.example.my_kmp_project/component/realtime/AppRealtimeClientImpl.kt:272-282`），但没有生产装配点。
- 首帧归属：tab 根与二级栈由三端各自状态位决定，`ReportMainTabRoot` 目前只在 legacy `AppShell` 生效（`core/ui/src/commonMain/kotlin/com.example.my_kmp_project/core/ui/MainTabChrome.kt:23-30` 对照 `composeApp/src/commonMain/kotlin/com.example.my_kmp_project/app/AppShell.kt:338-341`）。
- 全部耗时字段未实测；需要数字时请先补测，再替换本文的「未实测」。



# 23 · 三端原生壳与产物投递（Android / iOS / HarmonyOS）

> 本文要回答的唯一问题：**这三端壳到底各自长了一大坨原生 UI，还是只是 Compose 的薄壳？**
> 结论：三端都是「厚原生壳 + 极小的 Compose 岛」。iOS 侧有 65 个专用 SwiftUI 页面、Harmony 侧有 65 个专用 ArkTS Pane（外加 1 个通用兜底 Pane），只有 Android 侧的 Jetpack 屏数量少得多（20 个 `@Composable`）——因为 Android 的"原生"与共享 CMP 都是 Compose，边界最容易被掩盖。
> 所有事实都带 `仓库相对路径:行号`；所有计数都带本文实测时执行的命令。

---

## 0. 复核方法（先给命令，再给数）

以下命令在仓库根目录 `/Users/qpy/Desktop/github/my_kmp_project` 执行，输出即为本文引用的数字。

| # | 命令 | 输出 | 含义 |
|---|------|------|------|
| M1 | `grep -c '^struct Native.*Page: View' iosApp/iosApp/NativeSecondaryPages.swift` | **65** | iOS 专用 SwiftUI 页面数 |
| M2 | `for f in iosApp/iosApp/*.swift; do printf '%s %s\n' "$(grep -c '^struct .*: View' "$f")" "$f"; done` | ContentView 1 / NativeAuthPages 2 / NativeFeatureCatalog 1 / NativeMineRoot 1 / NativeSecondaryPages 65 / NativeTabRoots 4 / iOSApp 0 | 每个 Swift 文件的顶层 `View` 结构体数 |
| M3 | `grep -o "this.nativePane === '[a-z_0-9]*'" harmonyApp/entry/src/main/ets/pages/Index.ets \\| sort -u \\| wc -l` | **66** | Harmony 侧 `nativePane` 分发 id 数（65 个具名 + `feature` 兜底） |
| M4 | `grep -c 'export struct Native[A-Za-z0-9]*Pane' harmonyApp/entry/src/main/ets/pages/NativeSecondaryPages.ets` | **65** | Harmony 专用 ArkTS Pane 数（第 66 个分派目标 `NativeFeaturePane` 是 `Index.ets:1204-1205` 的 `@Builder` 方法，不是 struct） |
| M16 | `python3` 扫描 `Index.ets` 中 `if (this.nativePane === '<id>')` 紧跟的 `Native…Pane` | **65 条分派**（§3.4.2 全表逐条列出） | Pane id → struct 的实际对应关系 |
| M5 | `grep -rc '@Composable' composeApp/src/androidMain/kotlin` | MainActivity 1 / NativeAndroidApp 3 / NativeAndroidMain 2 / NativeJetpackHome 1 / NativeJetpackTabs 13 / Platform 0 / TabIcons 0 = **20** | Android 壳自带 Composable 数 |
| M6 | `wc -l iosApp/iosApp/*.swift` | 合计 **8463** 行 Swift | iOS 壳体量 |
| M7 | `find harmonyApp/entry/src/main/ets -name '*.ets' \\| xargs wc -l \\| tail -1` | 合计 **9790** 行 ArkTS | Harmony 壳体量 |
| M8 | `wc -l composeApp/src/androidMain/kotlin/com.example.my_kmp_project/*.kt composeApp/src/androidMain/kotlin/com.example.my_kmp_project/nativeshell/*.kt` | 合计 **2680** 行 Kotlin | Android 壳体量 |
| M9 | `grep -c '": "' <(sed -n '90,144p' iosApp/iosApp/NativeFeatureCatalog.swift)` | **83** | iOS 中文标签 → 路径别名条数 |
| M10 | `sed -n '22,108p' harmonyApp/entry/src/main/ets/pages/NativeFeatureCatalog.ets \\| grep -c "\['"` | **84** | Harmony 标签别名条数（含 1 个重复键，见 §6.5） |
| M11 | `sed -n '179,466p' iosApp/iosApp/NativeFeatureCatalog.swift \\| grep -cE '^\s*"[^"]+"\s*:'` | **50** | iOS `builders` 字典条目（mock 目录页） |
| M12 | `sed -n '228,476p' harmonyApp/entry/src/main/ets/pages/NativeFeatureCatalog.ets \\| grep -cE "case '"` | **49** | Harmony `featurePageFor` 分支数（mock 目录页） |
| M13 | `grep -c 'path == "/' iosApp/iosApp/NativeFeatureCatalog.swift` | **78** | `NativeFeatureHost` 里精确路径比较次数（含 `\\|\\|` 续写） |
| M14 | `ls harmonyApp/entry/libs harmonyApp/entry/src/main/cpp/include harmonyApp/entry/src/main/resources/rawfile` | `No such file or directory`（三个都不存在） | 交付产物当前**未发布**到工作树，见 §5.4 |
| M15 | `grep -rn '^expect ' --include='*.kt' . \\| grep -v '/build/' \\| wc -l` | **27** | 全仓 `expect` 声明数（§4 全景表） |

> 复核要点：M1 与 M4 都是 65，M3 与 M1 相比多出的那 1 个是通用兜底 Pane。iOS/Harmony 两侧"页面清单"几乎一一对应，这是本项目最直观的**双份原生 UI 复制**证据（两份代码语言不同、不可能共享，只能手写两遍）。

---

## 1. 三端壳总体形态对照

| 维度 | Android | iOS | HarmonyOS |
|------|---------|-----|-----------|
| 壳语言/框架 | Jetpack Compose（Material3） | SwiftUI | ArkTS（ArkUI 声明式） |
| 壳入口 | `composeApp/src/androidMain/kotlin/com.example.my_kmp_project/MainActivity.kt:21` `class MainActivity : ComponentActivity` | `iosApp/iosApp/iOSApp.swift:23` `@main struct iOSApp: App` | `harmonyApp/entry/src/main/ets/entryability/EntryAbility.ets:37` `onWindowStageCreate` → `windowStage.loadContent('pages/Index')` |
| 壳代码位置 | `composeApp/src/androidMain/`（**与 KMP 同模块**） | `iosApp/`（**独立 Xcode 工程**） | `harmonyApp/`（**独立 DevEco 工程**） |
| 壳体量 | 2680 行 Kotlin（M8） | 8463 行 Swift（M6） | 9790 行 ArkTS（M7） |
| 自带 UI 屏数 | 20 个 `@Composable`（M5），但 12 个 overlay case 里 9 个是共享 CMP（§3.2）——真正只属于 Android 的屏远少于 20 | 65 个专用页面（M1）+ 4 个 Tab 根 + 登录/注册 + FeatureHost | 65 个专用 Pane（M4）+ 4 个 Tab 根 Pane + 登录/快登 Pane + FeaturePane |
| 注册到系统的"页面"数 | 1 个 Activity（`AndroidManifest.xml:17`，`launchMode="singleTop"`） | 1 个 `WindowGroup`（`iOSApp.swift:26`）；弹层用 4 个 `fullScreenCover`（`ContentView.swift:412/435/439/443`） | **2 个 @Entry 页面**（`main_pages.json:3-4` = `pages/Index` + `pages/WebPage`）；其余 66 个 Pane 全在 `Index` 内用 `if (this.nativePane === …)` 就地切换 |
| Compose 出现在哪里 | ① Mine 岛 `MineIsland`（`NativeAndroidMain.kt:546`）② Home/Community/Mine/Content 四个共享 `RouteHost`（同文件 `:563/569/579/594`）③ Mine 根 Tab 竟也是共享 CMP（`NativeJetpackTabs.kt:1267-1292` → `MineHomeContent`） | 仅两处 `UIViewControllerRepresentable`：`MineIslandHost`（`ContentView.swift:497-503`）、`MineSecondaryHost`（`:505-511`），均由 `MainViewController.kt` 工厂创建 | 仅一处 `Compose({...})`（`Index.ets:1963-1986`），controller 来自 `libentry.MainArkUIViewController()`（`Index.ets:836-843`） |
| Compose 侧唯一"入口工厂" | `setContent { NativeAndroidApp() }`（`MainActivity.kt:38-40`）——注意整个壳就是 Compose，没有"岛"的物理边界 | `MainViewController.kt:28` `MineIslandViewController(route:)` / `:51` `SecondaryRouteViewController(routeOrLabel:)` | `MainArkUIViewController.kt:33-34` `@CName("MainArkUIViewController")` |
| 页面栈所有权 | 全在 Kotlin 进程内的 `when (overlay)` 状态机（`NativeAndroidMain.kt:545`，枚举 `ShellOverlay` 12 个值 `:761-773`） | SwiftUI `@State` 布尔位 + `fullScreenCover` | ArkTS `@State nativePane: string` 字符串状态机 + `showIsland` 布尔 |
| 系统能力接入方式 | Android SDK 直接调（CameraX/MLKit/Toast/Clipboard/Intent） | UIKit/AVFoundation/UIApplication（在 `core/platform` 的 ios actual 里） | **NAPI**：ArkTS → `libentry.so`（C++）→ `libkn.so`（Kotlin `@CName`） |
| 产物形态 | 一个 APK（`:composeApp` 是唯一的 binary 生产模块，`AGENTS.md:36`） | 静态 XCFramework `ComposeApp`（`composeApp/build.gradle.kts:39-48`）由 Xcode 链接 | `libkn.so` + `libkn_api.h`（`composeApp/build.gradle.kts:51-56`）由 CMake 链接进 `libentry.so` |
| 谁需要"重新发布" | 不需要（同模块编译） | 不需要（Xcode 每次重编 framework） | **需要**：改共享 Kotlin 后必须跑 publish 任务，DevEco 才能看到新符号（§5.4） |

**为什么这具表是本文的全部说服力**：iOS 与 Harmony 的 65 个页面/Pane 与 Kotlin 共享代码**没有任何编译期关系**。把 iOS 的一个页面搬到 Harmony 只能重写。ADR 0002 §1（`docs/adr/0002-native-shell-mine-compose-island.md:13`）选择了这条路，代价就是这两坨各约 9k 行的原生代码。

---

## 2. 文件清单

### 2.1 `composeApp/src/androidMain/`（Android 壳，7 个 kt + Manifest + res）

| 路径 | 角色 | 关键行 |
|------|------|--------|
| `kotlin/com.example.my_kmp_project/MainActivity.kt` | Activity 入口；启动顺序的唯一真源 | 21-41 `onCreate`；43-47 `onNewIntent`；49-57 `onResume/onPause`；59-62 `handleDeepLinkIntent`；65-68 preview |
| `kotlin/com.example.my_kmp_project/Platform.android.kt` | `getPlatform()` actual | 9 |
| `kotlin/.../nativeshell/NativeAndroidApp.kt` | 三段门：Splash → 隐私 → 主壳 | 44 `enum NativePhase`；50-74 门流程（隐私 KV 53、同意时注册 push 66-68）；76-97 `NativeSplash`（79 `delay(1200)`）；99-150 `NativePrivacy` |
| `kotlin/.../nativeshell/NativeAndroidMain.kt` | 794 行的壳状态机，Android 的"路由表 + 栈" | 87-98 soft-auth/token 过期接线；124-145 `requiresLoginForRoute`；147-157 `selectTab`；159-260 各 host 的 push/pop helper；262-302 `openPendingRoute`；327-369 `openDeferred`；372-415 `navigateSecondary`；429-543 深链 `LaunchedEffect`；**545-757 `when (overlay)` 渲染体**；669-676 `Scaffold`；683-749 keep-alive Tab 根；760 `AuthOverlay`；761-773 `ShellOverlay`；775-794 `JetpackUnmappedEntry` |
| `kotlin/.../nativeshell/NativeJetpackHome.kt` | Home 根（Jetpack 宿主，但**复用共享 `feature/home` 的 section Composable**） | 45-54 去重说明注释；57 `JetpackHomeRoot`；81-86/212-216 签到 SharedPreferences；115 `PullToRefreshBox`；130 `statusBarsPadding`；138-204 共享 section；210 `MetricTabs`；218-227 `flutterStyleGreeting` |
| `kotlin/.../nativeshell/NativeJetpackTabs.kt` | 1292 行，Chat / Community / Mine 三个 Tab 根 | 86-310 `JetpackChatRoot`（129-152 缺参提示页）；385-705 `JetpackChatDetail`（474 `reverseLayout = true`，536-703 输入面板）；840-997 `JetpackCommunityRoot`（853-855 发布总线）；1005-1133 `CommunityPostCard`；1170-1178 `nineGridUrls`；1234-1264 `communityRichBody`（`#576B95`）；**1267-1292 `JetpackMineRoot` → 共享 `MineHomeContent`** |
| `kotlin/.../nativeshell/TabIcons.kt` | 手搓 ImageVector 图标，刻意不引 material-icons-extended | 12-15 注释「OHOS-safe」 |
| `AndroidManifest.xml` | 权限 + 启动 Activity + 深链 | 4-7 权限；9-16 application（`icon @mipmap/ic_launcher`、`usesCleartextTraffic`、`networkSecurityConfig`、`Theme.DemoApp`）；17-21 MainActivity `singleTop` + `Theme.DemoApp.Splash`；22-25 LAUNCHER；27-32 `myai` scheme；33-42 https App Link `app.myai.example` |
| `res/values/themes.xml` | 两套主题 | `Theme.DemoApp.Splash`（parent `Theme.SplashScreen`，`postSplashScreenTheme` → `Theme.DemoApp`）；`Theme.DemoApp` |
| `res/values/colors.xml` | `splash_background = #F7FAFC` | 与 Flutter SoT 对齐的注释在文件内 |
| `res/values/strings.xml` | `app_name = My AI` | 全文 3 行 |
| `res/drawable/splash_screen.xml` | pre-12 冷启动层 | `layer-list`：`@color/splash_background` + 居中 `@mipmap/launch_image` |
| `res/xml/network_security_config.xml` | 明文放行 | `<base-config cleartextTrafficPermitted="true"/>` |
| `res/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/{ic_launcher,launch_image}.png` | 5 密度 × 2 张 = 10 个位图 | 无 adaptive/round，对齐 Flutter SoT（`AGENTS.md:89`） |

### 2.2 `composeApp/src/iosMain/`（Kotlin 侧 iOS 出口，只有 2 个文件）

| 路径 | 角色 | 关键行 |
|------|------|--------|
| `kotlin/com.example.my_kmp_project/MainViewController.kt` | **Swift 可见的全部 Kotlin API 面** | 16-21 废弃的 `MainViewController()`；28-43 `MineIslandViewController(route)`（`MineIslandRoute.fromPath` → `fromHostKey`，自带 dismiss）；51-64 `SecondaryRouteViewController(routeOrLabel)` → `SecondaryRouteIsland`；67-70 `AcceptDeepLinkFromIos`；73-77 `ensureIosAuthReady`；79-143 认证面 `AuthIsLoggedIn/AuthDisplayName/AuthLogout/AuthLoginWithPassword/AuthLoginWithOtp/AuthSendPhoneOtp/AuthRegister/AuthRegisterWithPhone` |
| `kotlin/com.example.my_kmp_project/Platform.ios.kt` | `getPlatform()` actual | 8 |

目录里只有这两个文件（`find composeApp/src/iosMain -type f` 输出 2 行）。也就是说 iOS 的**平台能力 actual 全在 `core/*` 与 `component/*` 的 `iosMain` 里**，`composeApp/iosMain` 只负责「把 Compose 包成 UIViewController」+「把 Auth 包成 Swift 函数」。

### 2.3 `composeApp/src/ohosMain/`（Kotlin 侧 OHOS 出口，7 个文件）

| 路径 | 角色 | 关键行 |
|------|------|--------|
| `kotlin/com.example.my_kmp_project/MainArkUIViewController.kt` | **326 行；`libkn.so` 的全部导出** | 26 `OhosSmokeUiOnly = false`；33-71 `MainArkUIViewController(env)`；81-98 `KnSetOhosHost`（含 routeCode 注释表）；101-106 `KnSetOhosSecondaryRoute`；113-129 `KnApplyAuthSession`；132-147 `copyUtf8`；149-154 `ensureOhosAuthReady`；158-275 五个 `*Blocking` 认证导出；278-303 四个 `KnAuthCopy*`；306-308 `KnAuthIsLoggedIn`；311-315 `KnAuthLogout`；**318-319 `KnAudioOnPageHide() = Unit`**；321-326 `OhosSmokeRoot` |
| `kotlin/com.example.my_kmp_project/Platform.ohos.kt` | `getPlatform()` actual，name = "HarmonyOS" | 全文 12 行 |
| `kotlin/.../core/ohos/OhosJsObject.kt` | NAPI JS 对象构造 DSL（给 Compose interop 传参用） | 10-14 `ohosJsObject`；16-32 `OhosJsBuilder.put(String/Boolean/Int/Long)` |
| `cinterop/resource.def` | rawfile 读取绑定 | `package = platform.resource`；`headers = raw_file_manager.h raw_file.h` |
| `cinterop/avplayer.def` | 系统播放器绑定 | `package = platform.avplayer`；`headers = multimedia/player_framework/avplayer.h …_base.h` |
| `cinterop/include/raw_file.h` · `raw_file_manager.h` | 自备的 C 头（rawfile 无现成 klib） | — |

注意目录名是**字面量** `kotlin/com.example.my_kmp_project/`（一个含点的目录名），而不是 `com/example/my_kmp_project/`；三端一致如此（§2.1、§2.2 同样），本文所有路径均照此书写。

### 2.4 `iosApp/`（SwiftUI 壳）

| 路径 | 角色 | 行数 / 关键行 |
|------|------|---------------|
| `iosApp/iOSApp.swift` | `@main`；方向与深链 | 35 行；5-12 AppDelegate `.allButUpsideDown`；14-20 `open url` → `DeepLinkEntryKt.acceptDeepLink`；23-35 `WindowGroup { ContentView().onOpenURL }` |
| `iosApp/ContentView.swift` | 壳主编排 | 511 行；6-19 `DesignTokens`；21-31 `Text(_:color:)` 重载陷阱；35-57 `MainTab` + `fromLaunchArguments()`（`-tab`）；62-79 状态位；81-115 `body`（86 splash 1.2s、101 `.ignoresSafeArea(.keyboard)`、102-104 `onOpenURL`、106-113 `pending_deeplink` + `-route`）；120-130 `handleDeepLinkOrLabel`；132-230 `openOwnedRoute`（135-145 `toastOnlyKeys`，192-221 Mine 岛开关表）；234-249 `openSecondaryOrNative`；253-265 Mine 二级白名单；267-329 soft-auth helper；341-451 `mainShell`；454/463/480 Splash/Privacy/AuthGate；**497-503 `MineIslandHost`**；**505-511 `MineSecondaryHost`** |
| `iosApp/NativeFeatureCatalog.swift` | 路由解析 + mock 目录 + **原生页总调度** | 701 行；4-87 `NativeRouteResolver`（47-52 注释记录「前缀吞掉」bug 类，声明与 Harmony `nativePaneIdFor` 镜像，由 `scripts/check-ios-page-dispatch.sh` 守护）；89-145 `labelToPath`（M9=83 条）；148-161 `NativeFeaturePage`/`Row`；179-466 `builders`（M11=50）；**470-646 `NativeFeatureHost`**（M13=78 次路径比较）；648-700 `genericList` 兜底 |
| `iosApp/NativeSecondaryPages.swift` | 专用页面本体 | 4797 行；65 个 `struct Native…Page: View`（M1）；`HomeNativeMock` 4130；W5.i 子路由专用页 4228-4797 |
| `iosApp/NativeTabRoots.swift` | 4 个 Tab 根 + 底栏 | 1462 行；5 `NativeBottomBar`（注释「49pt, same contract as JetpackBottomBar」）；59 `HomeTabView`；540 `HomeSwitchStoreSheet`；592/602 `ChatConv`/`ChatMsg`；789 `DailyCheckInOverlay`；880 `ChatTabView`；1169 `CommunityPostModel`；1289 `SeededGenerator`；1298 `CommunityTabView` |
| `iosApp/NativeMineRoot.swift` | Mine 根（SwiftUI 版） | 414 行；6 `MineRootView`；351 `SwitchStoreSheet`；409 `MineScrollOffsetKey` |
| `iosApp/NativeAuthPages.swift` | 登录/注册（SwiftUI 版） | 543 行；5 `AuthTokens`；23 `NativeLoginView`；364 `NativeRegisterView`；调 Kotlin 认证的位置 311/335/348/428/461/475 |
| `iosApp/Info.plist` | URL scheme + ATS | `CFBundleURLTypes` 只注册 `myai`（与 Android 一致）；`NSAppTransportSecurity/NSAllowsArbitraryLoads = false`；`CADisableMinimumFrameDurationOnPhone = true` |
| `iosApp/LaunchScreen.storyboard` | 启动屏 | `#F7FAFC` + 居中 LaunchImage（`AGENTS.md:96`） |
| `Configuration/Config.xcconfig` | bundle 名/产品名 | — |
| `iosApp.xcodeproj/project.pbxproj` | 启动键 | 299-311 / 328-340（Debug/Release 两份）：`INFOPLIST_FILE = iosApp/Info.plist`（305/334）、`INFOPLIST_KEY_UILaunchStoryboardName = LaunchScreen`（309/338）、`INFOPLIST_KEY_UILaunchScreen_Generation = NO`（308/337）、`GENERATE_INFOPLIST_FILE = YES`（304/333）、`CODE_SIGN_STYLE = Automatic`（300/329） |
| `iosApp/Assets.xcassets/AppIcon.appiconset/` | 15 尺寸全套 | `AGENTS.md:96` |

**Swift 侧唯一触及 Compose 的地方**（`grep -n 'MainViewControllerKt\|MineIslandViewController\|SecondaryRouteViewController\|ComposeUIViewController' iosApp/**/*.swift` 的结论）：`ContentView.swift:497-511` 两个 representable、`NativeAuthPages.swift` 的 `MainViewControllerKt.Auth*` 调用、`ContentView.swift:67` 的 `MainViewControllerKt.AuthDisplayName()`。其余 6 个 Swift 文件一行都不碰 Compose。

### 2.5 `harmonyApp/`（ArkTS 壳）

| 路径 | 角色 | 行数 / 关键行 |
|------|------|---------------|
| `entry/src/main/ets/pages/Index.ets` | **2411 行的单页壳**：状态机 + 66 个 Pane 分发 + Compose 岛 | 1 import `ArkUIViewController, Compose`；7 `import libentry from 'libentry.so'`；8-9 interop 注册器 import；140 `@Entry @Component struct Index`；223-256 `aboutToAppear`（224-225 interop 注册、229 `getPreferences('native_shell')`、233-251 `sharedSessionStore.init` + `libentry.ApplyAuthSession`、252-255 `setTimeout(1200)` + `consumePendingDeepLink`）；259-294 深链取路径；338-386 聊天种子；544-610 乐观发送（280ms 成功 / 2s 已读）；717-761 社区 35 帖 LCG(seed=42)；**826-848 `openComposeHost(kind, routeCode)`**；850-852 `openMineIsland`；855-868 `openMineIslandRouteWithSoftAuth`（gated 4/5/7/8/9）；870-875 `openSecondaryCompose`；877-995 `openDeferred`（879-888 toastOnly、916-951 岛码、952-986 Tab 根）；997-1015 `openSecondaryOrNative`；1017-1061 `requiresLoginForRoute`；1144/1157/1181 Splash/Privacy/AuthGate Pane；1197 `MetricCell`；**1204-1205 `@Builder NativeFeaturePane()`（通用兜底，分派在 `:2334-2336`）**；1261/1272/1370 Chat 三层；1518 CommunityPane；1651 MinePane（172/1824 导航渐隐 72px、1831-1863 折叠导航）；1871 `TabContent`；1895 `tabIcon`（`$r('app.media.main_tab_*_selected/unselected')`）；1910 `TabBtn`；1934-2380 `build()`（1948-1957 底栏 `height(49)`+`#F2FFFFFF`、**1963-1986 Compose 覆盖层 + 「关闭」按钮**、1989-2332 约 65 个 `if (this.nativePane === '…')` 块、2334 通用 feature、2338 `NativeLoginPane`、2352 切店浮层、2371 `errorMessage`）；2382-2389 `onPageHide`（`libentry.AudioOnPageHide()` + `islandController.onPageHide()`）；2391 `onPageShow`；2397-2410 `onBackPressed`（切店 → 岛 → 登录 优先级） |
| `entry/src/main/ets/pages/NativeSecondaryPages.ets` | 专用 Pane 本体 | 4949 行；65 个 `export struct Native*Pane`（M4），从 `NativeSearchPane:13` 到 `NativeVideoHubPane:4917` |
| `entry/src/main/ets/pages/NativeFeatureCatalog.ets` | 标签表 + 路径解析 + 目录页 + **Pane 分派** | 708 行；1-4 头注释「Mirrors iosApp NativeFeatureCatalog.swift」；6-19 `FeatureRow/FeaturePage`；21-109 `LABEL_TO_PATH`（M10=84）；112-130 `stripSchemeToPath`；132-218 `resolveNativePath`（174-179 大括号/缩进异常，见 §6.5）；228-476 `featurePageFor`（M12=49 case）；**484-708 `nativePaneIdFor`**（498-529 的 11 条 Mine 专用规则在前；全函数 72 个 `return` 其中 5 个返回空串；707 `return 'feature'`，详见 §3.4.1） |
| `entry/src/main/ets/pages/NativeHomeContent.ets` | Home 内容组件 | 596 行 |
| `entry/src/main/ets/pages/NativeShellShared.ets` | 共享 token/枚举 | 23 行；2-10 `DesignTokens` 十六进制；12-16 `AppPhase`；18-23 `MainTab`（数值枚举） |
| `entry/src/main/ets/pages/WebPage.ets` | 第 2 个 @Entry 页（独立路由） | 50 行；9 `@Entry`；16-20 取 router 参数；29 `router.back()`；38 `Web({...})` |
| `entry/src/main/ets/auth/NativeLoginPane.ets` | 登录面板 | 360 行 |
| `entry/src/main/ets/auth/HuaweiQuickLoginPane.ets` | 华为快登面板 | 298 行 |
| `entry/src/main/ets/auth/SessionStore.ets` | ArkTS 侧会话缓存 | 86 行；`sharedSessionStore` 被 `Index.ets:233` 使用 |
| `entry/src/main/ets/auth/AuthApiClient.ets` | ArkTS 直连 BFF 的 HTTP | 67 行 |
| `entry/src/main/ets/auth/AuthConfig.ets` | mock/真实后端开关 | 16 行 |
| `entry/src/main/ets/auth/README.md` | 说明 | — |
| `entry/src/main/ets/ComposeVideoInterops.ets` | 把 ArkTS `Video` 注进 Compose 岛 | 108 行；2 深 import `registerComposeInteropBuilder`；4-13 `VodVideoArgs`；35-50 seek/play 同步；105-107 注册 `'vodVideoInline'` 与 `'vodVideoPreview'` |
| `entry/src/main/ets/ComposeWebInterops.ets` | 把 ArkTS `Web` 注进 Compose 岛 | 31 行；29-30 注册 `'agreementWebInline'` |
| `entry/src/main/ets/common/DsBridgeShim.ets` | DSBridge 占位 | 11 行；8-10 `register()` 体为空，注释「Deferred」 |
| `entry/src/main/ets/entryability/EntryAbility.ets` | Ability 入口 + 深链暂存 | 60 行；7 `PENDING_DEEP_LINK_KEY`；10-18 `onCreate`；20-22 `onNewWant`；24-31 `stashDeepLink` → `AppStorage.setOrCreate`；37-47 `onWindowStageCreate` |
| `entry/src/main/ets/entrybackupability/EntryBackupAbility.ets` | 备份能力 | 16 行 |
| `entry/src/main/module.json5` | 权限 / ability / skills | 13-37 INTERNET·GET_NETWORK_INFO·VIBRATE；38-48 EntryAbility（`$media:app_icon`、`startWindowIcon $media:startIcon`、`startWindowBackground $color:start_window_background`、`exported: true`、`orientation: auto_rotation`）；49-57 home entity；58-67 `ohos.want.action.viewData` + `scheme: "myai"`；71-84 EntryBackupAbility |
| `AppScope/app.json5` | bundle | 10 行；`bundleName: com.example.harmonyapp`、icon `$media:app_icon`、label `$string:app_name` |
| `entry/src/main/resources/base/profile/main_pages.json` | **页面注册表** | 3-4 只有 `pages/Index` 与 `pages/WebPage` |
| `entry/src/main/resources/base/element/` 下的 `color.json`、`float.json`、`string.json` | 值资源 | `harmonyApp/entry/src/main/resources/base/element/color.json:4-5` `start_window_background = #F7FAFC` |
| `entry/src/main/resources/base/media/` | 172 个位图（`app_icon/startIcon/bg_splash/main_tab_*/home_all_services_*` …） | `ls` 计数 172 |
| `entry/src/main/cpp/napi_init.cpp` | NAPI 桥（见 §2.6） | 349 行 |
| `entry/src/main/cpp/CMakeLists.txt` | 链接 `libkn.so` | 55 行 |
| `entry/src/main/cpp/types/libentry/Index.d.ts` | ArkTS 侧类型契约 | 22 行 |

**ArkTS 壳的 KMP 产物目录当前为空**（M14）：`harmonyApp/entry/libs/`、`entry/src/main/cpp/include/`、`entry/src/main/resources/rawfile/` 三者在工作树中都不存在——它们由 publish 任务生成，且被 `.gitignore` 忽略（`harmonyApp/.gitignore:14-17`、根 `.gitignore:85-90`）。

### 2.6 native↔Kotlin 边界文件（本文反复引用）

| 路径 | 角色 | 关键行 |
|------|------|--------|
| `harmonyApp/entry/src/main/cpp/napi_init.cpp` | `libentry.so` 的全部注册符号 | 1 `#include "libkn_api.h"`；16-19 `CallComposeArkUiInit`；21-37 `NapiMainArkUIViewController`；44-58 `extern "C"` 声明块；60-74 `NapiSetOhosHost`；76-93 `NapiSetOhosSecondaryRoute`；107-121 `NapiApplyAuthSession`；123-202 `AuthOp/AuthAsyncCtx/AuthExecute/AuthComplete/StartAuthPromise`；204-262 五个认证 wrapper；264-273 `NapiCopyStringGetter`（2048B 缓冲）；275-306 token/name/id/phone/isLoggedIn/logout；**313-328 16 个导出名**；330-332 `napi_define_properties` + Compose 初始化；336-349 `nm_modname = "entry"` 与构造函数注册 |
| `harmonyApp/entry/src/main/cpp/CMakeLists.txt` | 选 ABI、找 compose | 13-17 按 ABI 选 `libkn`；20-24 缺失即 `FATAL_ERROR` 并提示 publish 命令；26-28 `include/${LIBKN_ABI_DIR}`；30-32 `find_package(compose)` 自 `oh_modules/@cpf-kmp-cmp/compose`；34-35 imported target `kn`；40-45 RPATH `$ORIGIN`；47-54 link 清单（`libace_napi.z`、`libhilog_ndk.z`、`libdeviceinfo_ndk.z`、`librawfile.z`、`kn`、`compose::skikobridge`） |
| `composeApp/build.gradle.kts` | 唯一产物生产者 + 投递契约 | 17 `androidOnly`；19-27 `ohosNativeSysroot`；39-48 iOS 静态 framework `ComposeApp`；51-56 OHOS `sharedLib { baseName = "kn" }`；58-60 release `optimized = false`；61 `export(libs.compose.multiplatform.export)`；62-65 反 `moduleIncludes` 注释；66 `linkerOpts("-lz","-lavplayer")`；69-71 ohosX64 专属 `-lnet_http`；76-91 `rendererBackend` + 11 个 fusion-renderer `-l`；93-108 cinterop `resource`/`avplayer`；161-181 手写 `iosMain`/`ohosMain` source set（`api(libs.compose.multiplatform.export)`）；229-238 `harmonyAppDir`（`-PharmonyAppPath`）；246-253 `harmonyResourceRoots`；**259-300 四个 publish Copy 任务** |
| `runscript/runOhosApp-Mac.sh` | 一条命令跑通 Harmony | 25-28 默认值；31-52 参数；99-131 DevEco 检测（≥6.0.0）；137 `HDC_BIN`；140-141 `aa force-stop`；150-155 选 publish 任务；162-182 gradle 步；197 `ohpm install --all`；198 `hvigorw --sync`；199-203 `assembleHap`；208-231 签名 HAP 路径 + exit 6 提示；232-236 设备检查；238-256 `bm install` + 失败重装；273-281 `aa start`；287-297 `10106102` 锁屏处理；305-325 计时汇总 |
| `scripts/run-harmony.sh` | macOS 上的包装 | 19-21 exec `runscript/runOhosApp-Mac.sh` |

---

## 3. 逐文件 · 逐功能走读

### 3.1 Android 启动顺序（`MainActivity.kt:22-41`，顺序即契约）

```
installSplashScreen()          :23   ← 必须早于 super.onCreate，否则系统闪屏 API 无效
enableEdgeToEdge()             :24
super.onCreate(...)            :25
AndroidAppContext.install(this):26   ← 进程级 Context
AndroidActivityContext.install :27   ← Activity 级 Context
platformNetworkBootstrap()     :28   ← expect/actual，见 §4
MockAuthConfig.useMockAuth =   :32-33 ← 用 APK 真实 debuggable 标志覆盖 core:network 恒 false 的 actual
ShellBootstrap.install()       :35   ← 会话水合 + 401 静默刷新（幂等）
handleDeepLinkIntent(intent)   :36
setContent { NativeAndroidApp() }:38-40
```

`onNewIntent`（43-47）在 `singleTop`（`AndroidManifest.xml:20`）下承接第二次深链，转给 `acceptDeepLink(data.toString())`（59-62）；`onResume/onPause`（49-57）安装/清除 Activity Context——这是"把 Activity 塞给共享代码"的常规 KMP 手法，也是 Android 壳唯一必须存在的胶水。

**三段门**（`NativeAndroidApp.kt`）：`NativePhase{Splash,Privacy,Main}`（44）；隐私同意状态从 KV 读（53），同意时调 `PlatformPushSdk` 注册（66-68，但 actual 是 stub，见 §4）；`NativeSplash` 固定 `delay(1200)`（79）并渲染 `:core:design` 的 `bg_splash` + `ic_splash_logo` 96dp（99-150 是 `NativePrivacy`）。

### 3.2 Android 主壳：一个 `when` 就是一套导航栈（`NativeAndroidMain.kt`）

| 阶段 | 行 | 做什么 |
|------|----|--------|
| 软锁接线 | 87-98 | `SoftAuthPresenter` + token 过期回调；未登录时把目标路由存起来 |
| 状态位 | 100-121 | `tab / overlay / authOverlay / islandRoute / homeRoute / communityRoute / mineRoute / contentRoute / webUrl / deferredStack` 等 |
| 登录门槛表 | 124-145 | `requiresLoginForRoute(route)`：与 iOS `ContentView.swift:304` / Harmony `Index.ets:1017` 三份并列的同一张表 |
| 切 Tab | 147-157 | `selectTab` |
| 各 host 的 push/pop | 159-260 | Mine / Home / Content 支持内部多级栈（`popMineRoute` 等） |
| 登录后重放 | 262-302 | `openPendingRoute` |
| 中文标签/路径统一入口 | 327-369 | `openDeferred(title)`：先查 `toastOnly`，再决定岛/原生/toast |
| 二级路由派发 | 372-415 | `navigateSecondary`：`ProductRouteDispatch.resolve(...)` → `host` 为 Mine/Home/Community/Content/AllServices/Web/Tab/Toast/Scan/Auth/Unknown |
| 深链消费 | 429-543 | `LaunchedEffect` 订阅 `DeepLinkRouter` 流，解析到具体 Tab/Overlay |
| **渲染** | 545-757 | `when (overlay)`，12 个 case |
| 底栏 + keep-alive | 669-749 | `Scaffold(containerColor = Transparent, contentWindowInsets = WindowInsets(0))`（669-676），四个 Tab 根常驻并用 `zIndex`/`graphicsLayer` alpha 切换（683-749）——IndexedStack 式保活 |

`when (overlay)` 的 12 个 case 与它们的宿主归属（这是「Android 壳混用两套 Compose」最直接的表）：

| `ShellOverlay` 值（`:761-773`） | 渲染位置 | UI 归属 |
|-------------------------------|----------|---------|
| `MineIsland` | `:546-551` `MineIsland(initialRoute, onRequestClose)` | **共享 CMP** |
| `AllServices` | `:552-560` `AllServicesScreen`（`feature/home/src/commonMain/…/AllServicesScreen.kt:55`） | **共享 CMP** |
| `HomeRoute` | `:561-568` `HomeRouteHost`（`feature/home/src/commonMain/…/HomeRouteHost.kt:116`） | **共享 CMP** |
| `CommunityRoute` | `:569-578` `CommunityRouteHost`（`feature/community/src/commonMain/…:84`） | **共享 CMP** |
| `MineRoute` | `:579-593` `MineRouteHost`（`feature/mine/src/commonMain/…:72`），`next == "logout"` 特判 | **共享 CMP** |
| `ContentRoute` | `:594-600` `ContentRouteHost`（`feature/content/src/commonMain/…:13`） | **共享 CMP** |
| `Membership` | `:601-603` `MembershipScreen`（`feature/commerce/src/commonMain/…:40`） | **共享 CMP** |
| `InAppWeb` | `:604-617` `InAppWebScreen`（`feature/web/src/commonMain/…:21`）+ `DefaultWebBridgeHost` | **共享 CMP** |
| `Scan` | `:618-636` `ScanScreen`（`feature/scan/src/commonMain/…:50`）；`:622-634` 扫码结果回落 `DeepLinkRouter.accept` 或转 `InAppWeb` | **共享 CMP**（内部走 `PlatformBarcodeScanner`） |
| `UnmappedEntry` | `:637-642` + `:775-794` `JetpackUnmappedEntry` | **Android 自带** |
| `None` | `:643-667` 再 `when (authOverlay)` → `LoginScreen`(`feature/auth/…/LoginScreen.kt:43`)、`LoginPasswordScreen`(`:37`)、`LoginOtpScreen`(`:42`)、`RegisterScreen`(`:42`) | **共享 CMP** |

Tab 根侧：**Chat / Community 是 Android 自带的 Jetpack 实现**（`NativeJetpackTabs.kt:86` `JetpackChatRoot`、`:840` `JetpackCommunityRoot`），Home 自带但复用共享 section（`NativeJetpackHome.kt:138-204`），**Mine 根却是共享 CMP**（`NativeJetpackTabs.kt:1267-1292`，注释 1276 明写「CMP MineHomeContent already mirrors Flutter MinePage」）。

> **上表的加粗项是本文最重要的一条量化结论**：`when (overlay)` 的 12 个 case 里，只有 `UnmappedEntry` 与 `None→auth`（其中认证屏本身也是共享的）之外的 2 个真正由 Android 自己画；`MineIsland / AllServices / HomeRoute / CommunityRoute / MineRoute / ContentRoute / Membership / InAppWeb / Scan` 九个 case **全部是 `feature/*/commonMain` 的共享 CMP**，认证四屏同样如此（见下一段）。keep-alive 的四个 Tab 根（`:683-749`，`:684` `MainTab.entries.forEach` + `:690 zIndex` + `:691-693 graphicsLayer alpha`）里，Home/Chat/Community 自带、Mine 共享。

> 这条与 ADR 0002 §2「Mine **root** stays native」（`docs/adr/0002-native-shell-mine-compose-island.md:14`）相反。iOS（`NativeMineRoot.swift:6` `MineRootView`）和 Harmony（`Index.ets:1651` `MinePane`）都老老实实写了自己的 Mine 根，只有 Android 抄近路直接渲染共享 CMP。
> 同理，ADR 0002 §3「Auth UI 原生」（`:15`）在 Android 侧也只做到「入口是自己的 `when (authOverlay)`」：**四个认证屏全在 `feature/auth/src/commonMain`**（`LoginScreen.kt:43`、`LoginPasswordScreen.kt:37`、`LoginOtpScreen.kt:42`、`RegisterScreen.kt:42`），而 iOS 有 `NativeAuthPages.swift`（543 行，2 个 View，M2）、Harmony 有 `auth/NativeLoginPane.ets`（360 行）+ `auth/HuaweiQuickLoginPane.ets`（298 行）。详见 §8。

### 3.3 iOS：Swift 壳每个文件做了什么，Compose 岛在哪创建

| 文件 | 页面/组件注册方式 | Compose 岛创建点 |
|------|-------------------|------------------|
| `iOSApp.swift` | `WindowGroup { ContentView() }`（`:26`）；`AppDelegate.application(_:open:)`（`:14-20`）把 URL 交给 `DeepLinkEntryKt.acceptDeepLink` | 无 |
| `ContentView.swift` | `switch phase`（`:83`）→ `SplashView`（`:454`）/ `PrivacyView`（`:463`）/ `mainShell`（`:341`）；`mainShell` 内 `switch tab` 选 `HomeTabView`/`ChatTabView`/`CommunityTabView`/`MineRootView`，底部 `NativeBottomBar`；4 个 `fullScreenCover`（`:412` 登录、`:435` 岛、`:439` Mine 二级、`:443` 原生页） | `:497-503` `MineIslandHost.makeUIViewController` → `MainViewControllerKt.MineIslandViewController(route:)`；`:505-511` `MineSecondaryHost` → `MainViewControllerKt.SecondaryRouteViewController(routeOrLabel:)` |
| `NativeFeatureCatalog.swift` | `NativeRouteResolver`（`:4-87`）+ `labelToPath`（`:89-145`，83 条 M9）+ `builders`（`:179-466`，50 条 M11）+ **`NativeFeatureHost`（`:470-646`）= 一条 78 次路径比较的 if/else 链**（M13），全部不匹配时 `genericList` 兜底（`:648-700`） | 无（此文件是纯 SwiftUI；`:118-119` 注释声明「Legacy SecondaryRouteIsland is not used for …」） |
| `NativeSecondaryPages.swift` | 65 个 `struct Native…Page: View`（M1），供 `NativeFeatureHost` 精确路径分派 | 无 |
| `NativeTabRoots.swift` | `NativeBottomBar`（`:5`，注释声明与 `JetpackBottomBar` 同为 49pt 契约）、`HomeTabView`（`:59`）、`ChatTabView`（`:880`）、`CommunityTabView`（`:1298`）等 | 无 |
| `NativeMineRoot.swift` | `MineRootView`（`:6`）、`SwitchStoreSheet`（`:351`） | 无 |
| `NativeAuthPages.swift` | `NativeLoginView`（`:23`）、`NativeRegisterView`（`:364`） | 无，但**跨框架函数调用**在此：`:311/335/348/428/461/475` 调 `MainViewControllerKt.Auth*` |

iOS 路由决策链（`ContentView.swift`）：
1. `handleDeepLinkOrLabel(raw)`（`:120-130`）→ `openOwnedRoute`。
2. `openOwnedRoute`（`:132-230`）：先查 `toastOnlyKeys`（`:135-145`，与 Flutter 的「开发中」toast 对齐）；然后 `:192-221` 逐个把 `/settings`、`/mine/personalized_settings`、`/pay/membership`、`/mine/about`、`/mine/profile`、`/mine/addresses`、`/mine/addresses/edit`、`/mine/purchase_calculator`、`/settings/deal_invoice*` 映射到 **Mine 岛 route key**。
3. 其余落到 `openSecondaryOrNative`（`:234-249`）：Mine 岛 key → `MineIslandHost`；Mine 归属的二级白名单（`:253-265` `isMineComposeSecondary`）→ `MineSecondaryHost`；**其他一律 `NativeFeatureHost`**（原生）。
4. soft-auth 包裹在 `:267-329`（`requiresLogin(for:)` 在 `:304`）。

> `ComposeUIViewController` 在 Swift 侧永远不出现——它被封装在 `MainViewController.kt:28-64` 里。Swift 只见 `UIViewController`，这正是 CMP iOS 的标准宿主姿势。

#### 3.3.1 iOS 专用页面清册（65 行；`NativeSecondaryPages.swift` 定义行 + `NativeFeatureHost` 触发路径）

触发路径取自 `NativeFeatureCatalog.swift:470-646` 的分支条件；`A/B` 表示同一页被多条路径复用。

| 页面 struct | 定义行 | 触发路径（省略前导 `/`） |
|---|---|---|
| `NativeSearchPage` | 7 | home/search |
| `NativeAllServicesPage` | 163 | home/all_services |
| `NativeLearningReportPage` | 287 | home/learning_report |
| `NativePurchaseCalculatorPage` | 378 | mine/purchase_calculator |
| `NativeMallDetailPage` | 499 | mall/detail |
| `NativeMallOrdersPage` | 613 | mall/orders |
| `NativeFriendPage` | 688 | friend |
| `NativeCommunitySearchPage` | 874 | community/search |
| `NativeCommunityPublishPage` | 930 | community/publish |
| `NativeCommunityCommentPage` | 1060 | community/comment |
| `NativeCommunityImagePreviewPage` | 1128 | community/image_preview |
| `NativeMallPage` | 1169 | mall |
| `NativeWalletPage` | 1291 | wallet |
| `NativeShortVideoPlayPage` | 1418 | video/short/play |
| `NativeShortVideoPublishPage` | 1483 | video/short/publish |
| `NativeShortVideoHelpPage` | 1550 | video/short/help |
| `NativeShortVideoPage` | 1576 | video/short |
| `NativeLivePage` | 1662 | live |
| `NativeAiStreamPage` | 1748 | ai/stream |
| `NativeScanPage` | 1918 | scan |
| `NativeStrategyPage` | 1995 | home/strategy |
| `NativeCheckInMallPage` | 2131 | home/check_in_mall |
| `NativeMusicPage` | 2380 | music/list |
| `NativeHomeworkStatsPage` | 2462 | classroom/homework 与 classroom/homework_stats |
| `NativeGiftClaimPage` | 2513 | classroom/gift/claim |
| `NativeHomeworkDetailPage` | 2610 | classroom/homework/detail_teacher、…/detail_student、…/transliter、…/review、classroom/video、classroom/video/detail（6 条路径共用，靠 `title:` 参数区分） |
| `NativeClassroomPage` | 2651 | classroom/my_class |
| `NativeUsedCarPage` | 2690 | home/used_car |
| `NativeNewCarFollowPage` | 2822 | home/new_car_follow |
| `NativeClubPage` | 2951 | home/live_commerce、home/club（同页两标题） |
| `NativeLifeServicePage` | 3031 | home/life_service |
| `NativeTodoListPage` | 3124 | home/todo/partner-pending、…/follow-up-customers、…/after-sales-appointments、…/order-pending-review |
| `NativeWebPage` | 3165 | web |
| `NativeAfterSalesPage` | 3186 | home/after_sales |
| `NativeLedgerPage` | 3280 | home/ledger |
| `NativeSmsTemplatePage` | 3342 | mine/sms_templates |
| `NativeStoreQrPage` | 3391 | mine/shop_qr |
| `NativeQaPage` | 3431 | mine/qa 与 mine/http_test |
| `NativePosterPage` | 3462 | mine/poster |
| `NativeBusinessPage` | 3495 | mine/cooperation |
| `NativeRemindersPage` | 3521 | mine/reminder |
| `NativeFeedbackPage` | 3556 | mine/feedback |
| `NativeProfilePage` | 3590 | mine/profile |
| `NativeAddressesPage` | 3704 | mine/addresses |
| `NativeTransliterFeedPage` | 3804 | home/transliter_feed |
| `NativeHotRankPage` | 3914 | home/hot_rank_detail |
| `NativeDataAnalyticsPage` | 4023 | home/data_analytics |
| `NativeBusinessCardPage` | 4080 | mine/business_card |
| `NativeUsedCarDetailPage` | 4231 | home/used_car/detail |
| `NativeUsedCarCreatePage` | 4302 | home/used_car/create |
| `NativeLedgerDetailPage` | 4383 | home/ledger/detail |
| `NativeDataAnalyticsDetailPage` | 4435 | home/data_analytics/detail |
| `NativeNewCarFollowCreatePage` | 4542 | home/new_car_follow/create |
| `NativeNewCarFollowDetailPage` | 4571 | home/new_car_follow/detail |
| `NativeAfterSalesCreatePage` | 4603 | home/after_sales/create |
| `NativeAfterSalesDetailPage` | 4627 | home/after_sales/detail |
| `NativeMallOrderDetailPage` | 4643 | mall/orders/detail |
| `NativeTransliterVideosPage` | 4659 | video/transliter/videos |
| `NativeTransliterVideoDetailPage` | 4674 | video/transliter/videos/detail |
| `NativeTransliterWorksPage` | 4690 | video/transliter/works |
| `NativeTransliterWorkDetailPage` | 4705 | video/transliter/works/detail |
| `NativeCommunityConventionPage` | 4721 | community/convention |
| `NativeLiveRoomPage` | 4736 | live/room |
| `NativeMusicNowPlayingPage` | 4751 | music/now_playing |
| `NativeVideoHubPage` | 4766 | video |

**这张表说明三件事**：① 65 个页面全部是 SwiftUI，没有一个来自 Kotlin；② 4 个页面被复用为多路径（`NativeHomeworkDetailPage` 一条顶六条），因此「65 页」覆盖的路径条目其实更多；③ 全部不匹配时落到 `genericList`（`NativeFeatureCatalog.swift:648-700`），所以任何未登记路径也**不会**掉进 Compose 岛。

### 3.4 Harmony：ArkTS 壳的分派与岛创建

**`aboutToAppear`（`Index.ets:223-256`）**：先注册两组 interop（`:224-225`）；灌聊天/社区种子数据；`preferences.getPreferences('native_shell')`（`:229`）读隐私同意；`sharedSessionStore.init` 并把已有会话推给 Kotlin —— `libentry.ApplyAuthSession(...)`（`:233-251`）；`setTimeout(..., 1200)`（`:252`）走完闪屏，再 `consumePendingDeepLink()`（`:255`）从 `AppStorage` 取 `EntryAbility` 暂存的深链（`EntryAbility.ets:7` 键名、`:24-31` 暂存）。

**岛创建（`Index.ets:826-848`）**，逐行读：

```
const abi: string = deviceInfo.abiList;                 // 827  诊断用
if (libentry === undefined) → errorMessage = `libentry.so failed (abi=${abi})`   // 828-831
const setHost = libentry.SetOhosHost; if (setHost !== undefined) setHost(kind, routeCode);  // 832-835
const createController = libentry.MainArkUIViewController;
if (createController === undefined) → 'MainArkUIViewController missing'          // 836-840
this.islandController = createController(); this.showIsland = true               // 842-843
catch → errorMessage + hilog.error(DOMAIN,'Compose', …)                          // 844-847
```

`openMineIsland(personalized)`（`:850-852`）＝ `openComposeHost(0, personalized ? 1 : 0)`；`openSecondaryCompose(route)`（`:870-875`）＝ 先 `libentry.SetOhosSecondaryRoute(route)` 再 `openComposeHost(1, 0)`。**`kind` 就是 Kotlin 侧 `MainArkUIViewController.kt:38-44` 判断渲染 `MineIsland` 还是 `SecondaryRouteIsland` 的那个整数。**

**岛挂载（`Index.ets:1963-1986`）**：`Compose({ controller: this.islandController, libraryName: 'entry', onBackPressed: () => this.islandController.onBackPress() })`，下面一个 `Button('关闭')`（`:1976-1981`）把 `showIsland` 置 false 并清空 controller。`libraryName: 'entry'` 必须与 `napi_init.cpp:341` 的 `nm_modname = "entry"` 一致，否则运行时找不到 native 模块。

**`nativePaneIdFor`（`NativeFeatureCatalog.ets:484-708`）**：返回空串表示「不占 Pane」——4 个 Tab 根、`/chat/*`、未映射的 `/mine|/settings`、会员；`:498-529` 是 11 条 Mine 专用规则（实测 `sed -n '498,529p' … | grep -c "return '"` → 11）；随后是按域分组的精确/前缀规则。整个函数实测共有 **72 个 `return '…'` 语句，其中 5 个返回 `''`**（命令：`sed -n '484,708p' harmonyApp/entry/src/main/ets/pages/NativeFeatureCatalog.ets | grep -cE "return '"` → 72；`… | grep -c "return ''"` → 5），即 67 条「路径 → pane」产出，落到 66 个不同 id（有的 id 由多条规则命中，如 `club` 被 `/home/club` 与 `/home/live_commerce` 共用、`homework_detail` 被 6 种 classroom 路径共用）。`:707` 兜底 `return 'feature'`。**注意它返回的是字符串 id，`Index.ets:1989-2332` 再逐条 `if (this.nativePane === 'id') Native…Pane({...})`**——Pane 名与 id 并非机械对应（例：`'short_play'` → `NativeShortVideoPlayPane`、`'todo'` → `NativeTodoListPane`、`'transliter'` → `NativeTransliterFeedPane`、`'mall_orders_detail'` → `NativeMallOrderDetailPane`），因此不能用命名规则反推，只能读 §3.4.2 那张实抄表。

**`featurePageFor`（`:228-476`，49 个 case，M12）**：给"全部服务/未分类"目录提供 mock 页数据，与 iOS `builders`（50 条，M11）互为镜像，供 `@Builder NativeFeaturePane()`（`Index.ets:1204-1205`，分派块 `:2334-2336`）渲染。

**生命周期**：`onPageHide`（`:2382-2389`）调 `libentry.AudioOnPageHide()` + `islandController.onPageHide()`；`onPageShow`（`:2391-2395`）转发给 controller；`onBackPressed`（`:2397-2410`）优先级：切店浮层 → Compose 岛 `onBackPress()` → 登录面板。这就是 ADR 0002 §4「返回先弹岛再退原生页」（`docs/adr/…0002….md:16`）在 ArkTS 里的落地。

#### 3.4.1 `nativePaneIdFor` 的规则次序（「前缀吞掉」bug 类的实证）

`NativeFeatureCatalog.ets:484-708` 是一条**先特殊、后前缀**的顺序敏感链。以下每一对都是「把特殊路径排在前缀规则之前」的实例，行号为实抄：

| 特殊规则（行） | 前缀规则（行） | 若顺序颠倒会怎样 |
|---|---|---|
| `/home/used_car/detail` → `used_car_detail`（`:559`） | `/home/used_car` 或 `startsWith('/home/used_car/')` → `used_car`（`:565`） | detail 页被列表页吞掉 |
| `/mall/detail` → `mall_detail`（`:631`） | — | 与 `/mall/orders` 互不干扰，但必须早于 `:640` |
| `/mall/orders/detail` → `mall_orders_detail`（`:634`） | `/mall/orders`（`:637`）、`/mall` + `startsWith('/mall/')`（`:640`） | 三级路径全部塌到 `mall` |
| `/video/short/play\\|publish\\|help`（`:646/:649/:652`） | `/video/`（`:670`） | 小视频三个子页全变 `short_video` |
| `/video` → `video_hub`（`:655`） | — | 精确匹配，不抢 `/video/short` |
| `/video/transliter/videos/detail`（`:658`） | `/video/transliter/videos`（`:661`）；works 同理 `:664/:667` | detail 塌到列表 |
| `/classroom/gift` 前缀 → `gift_claim`（`:685`） | `/classroom` 前缀 → `classroom`（`:694`） | 礼包页塌到课堂页 |

兜底在 `:707` `return 'feature'`；返回空串（＝不占 Pane，交给 Tab 根 / 未映射 / Compose 岛）的四类在 `:486-492`（空路径 + `/`·`/main`·`/home`·`/chat`·`/community`·`/mine`）、`:493-495`（`indexOf('/chat/') === 0`）、`:532-534`（`startsWith('/mine')`·`startsWith('/settings')`，`:531` 注释「Unmapped mine/settings still stay on Compose Mine island host」）、`:535-537`（`/pay/membership`）。而 `:496-497` 的注释本身就写着这条纪律：**「specific ArkTS mine panes must be dispatched before the /mine catch-all (mirrors iOS NativeFeatureHost exact-path checks)」**。另一处顺序依赖在 classroom 家族内部：`:688`（`startsWith('/classroom/homework/')` 带尾斜杠 → `homework_detail`）必须早于 `:691`（精确 `/classroom/homework` → `homework_stats`）。iOS 侧 `NativeFeatureCatalog.swift:47-52` 的注释明确声明这份逻辑与 Harmony `nativePaneIdFor` 互为镜像。

#### 3.4.2 Harmony 专用 Pane 清册（65 行；struct 定义行 + `Index.ets` 分派块行）

分派写法统一为 `if (this.nativePane === '<id>') { <Struct>({ onClose: () => { this.nativePane = ''; }, … }) }`。

| `nativePane` id | Pane struct | struct 定义行（`NativeSecondaryPages.ets`） | 分派块行（`Index.ets`） |
|---|---|---|---|
| `search` | `NativeSearchPane` | 13 | 1990 |
| `all_services` | `NativeAllServicesPane` | 162 | 1994 |
| `learning_report` | `NativeLearningReportPane` | 311 | 2004 |
| `purchase_calculator` | `NativePurchaseCalculatorPane` | 426 | 2008 |
| `friend` | `NativeFriendPane` | 752 | 2012 |
| `community_search` | `NativeCommunitySearchPane` | 919 | 2022 |
| `community_publish` | `NativeCommunityPublishPane` | 969 | 2026 |
| `community_comment` | `NativeCommunityCommentPane` | 1078 | 2032 |
| `community_image_preview` | `NativeCommunityImagePreviewPane` | 1146 | 2036 |
| `mall_detail` | `NativeMallDetailPane` | 561 | 2040 |
| `mall_orders` | `NativeMallOrdersPane` | 666 | 2044 |
| `mall` | `NativeMallPane` | 1196 | 2054 |
| `wallet` | `NativeWalletPane` | 1322 | 2064 |
| `short_play` | `NativeShortVideoPlayPane` | 1480 | 2068 |
| `short_publish` | `NativeShortVideoPublishPane` | 1524 | 2071 |
| `short_help` | `NativeShortVideoHelpPane` | 1580 | 2074 |
| `short_video` | `NativeShortVideoPane` | 1614 | 2078 |
| `live` | `NativeLivePane` | 1707 | 2088 |
| `ai_stream` | `NativeAiStreamPane` | 1812 | 2092 |
| `scan` | `NativeScanPane` | 1984 | 2096 |
| `strategy` | `NativeStrategyPane` | 2064 | 2106 |
| `check_in_mall` | `NativeCheckInMallPane` | 2222 | 2110 |
| `music` | `NativeMusicPane` | 2467 | 2114 |
| `classroom` | `NativeClassroomPane` | 2727 | 2118 |
| `homework_stats` | `NativeHomeworkStatsPane` | 2545 | 2128 |
| `homework_detail` | `NativeHomeworkDetailPane` | 2692 | 2138 |
| `gift_claim` | `NativeGiftClaimPane` | 2610 | 2147 |
| `used_car` | `NativeUsedCarPane` | 2773 | 2151 |
| `new_car_follow` | `NativeNewCarFollowPane` | 2899 | 2161 |
| `club` | `NativeClubPane` | 3022 | 2171 |
| `life_service` | `NativeLifeServicePane` | 3091 | 2175 |
| `todo` | `NativeTodoListPane` | 3182 | 2179 |
| `web` | `NativeWebPane` | 3220 | 2188 |
| `after_sales` | `NativeAfterSalesPane` | 3242 | 2192 |
| `ledger` | `NativeLedgerPane` | 3326 | 2202 |
| `sms_template` | `NativeSmsTemplatePane` | 3386 | 2212 |
| `store_qr` | `NativeStoreQrPane` | 3447 | 2216 |
| `qa` | `NativeQaPane` | 3491 | 2220 |
| `poster` | `NativePosterPane` | 3531 | 2224 |
| `business` | `NativeBusinessPane` | 3574 | 2228 |
| `reminders` | `NativeRemindersPane` | 3598 | 2232 |
| `feedback` | `NativeFeedbackPane` | 3628 | 2236 |
| `profile` | `NativeProfilePane` | 3663 | 2240 |
| `addresses` | `NativeAddressesPane` | 3771 | 2244 |
| `transliter` | `NativeTransliterFeedPane` | 3854 | 2248 |
| `hot_rank` | `NativeHotRankPane` | 3965 | 2258 |
| `data_analytics` | `NativeDataAnalyticsPane` | 4051 | 2262 |
| `business_card` | `NativeBusinessCardPane` | 4122 | 2272 |
| `used_car_detail` | `NativeUsedCarDetailPane` | 4153 | 2277 |
| `used_car_create` | `NativeUsedCarCreatePane` | 4210 | 2280 |
| `ledger_detail` | `NativeLedgerDetailPane` | 4282 | 2283 |
| `data_analytics_detail` | `NativeDataAnalyticsDetailPane` | 4328 | 2286 |
| `new_car_follow_create` | `NativeNewCarFollowCreatePane` | 4371 | 2289 |
| `new_car_follow_detail` | `NativeNewCarFollowDetailPane` | 4452 | 2292 |
| `after_sales_create` | `NativeAfterSalesCreatePane` | 4507 | 2295 |
| `after_sales_detail` | `NativeAfterSalesDetailPane` | 4583 | 2298 |
| `mall_orders_detail` | `NativeMallOrderDetailPane` | 4621 | 2301 |
| `transliter_videos` | `NativeTransliterVideosPane` | 4659 | 2304 |
| `transliter_video_detail` | `NativeTransliterVideoDetailPane` | 4698 | 2307 |
| `transliter_works` | `NativeTransliterWorksPane` | 4735 | 2310 |
| `transliter_work_detail` | `NativeTransliterWorkDetailPane` | 4773 | 2313 |
| `community_convention` | `NativeCommunityConventionPane` | 4809 | 2316 |
| `live_room` | `NativeLiveRoomPane` | 4843 | 2319 |
| `music_now_playing` | `NativeMusicNowPlayingPane` | 4883 | 2322 |
| `video_hub` | `NativeVideoHubPane` | 4917 | 2325 |

第 66 条分派是 `if (this.nativePane === 'feature' && this.featurePath.length > 0) { this.NativeFeaturePane() }`（`Index.ets:2334-2336`），目标为同类内的 `@Builder NativeFeaturePane()`（`:1204-1205`，内部按 `featurePageFor(this.featurePath)` 渲染目录页，`:1213`）。

**命名不可反推**（本表已按实抄）：`short_play → NativeShortVideoPlayPane`、`todo → NativeTodoListPane`、`transliter → NativeTransliterFeedPane`、`mall_orders_detail → NativeMallOrderDetailPane`（注意与 iOS 的 `NativeMallOrderDetailPage` 拼写一致但 `transliter_videos` 两侧同名不同缀）、`sms_template → NativeSmsTemplatePane`（id 单数、路径是 `/mine/sms_templates` 复数）。**任何"按 id 生成结构体名"的重构都会漏页。**

**与 iOS 清册逐行对照的结果**：65 个 Pane 与 65 个 Page 在语义上一一对应（`search/all_services/learning_report/purchase_calculator/friend/community_*/mall*/wallet/short_*/live/live_room/ai_stream/scan/strategy/check_in_mall/music/music_now_playing/classroom/homework_*/gift_claim/used_car*/new_car_follow*/club/life_service/todo/web/after_sales*/ledger*/sms_template/store_qr/qa/poster/business/business_card/reminders/feedback/profile/addresses/transliter*/hot_rank/data_analytics*/community_convention/video/video_hub`），差别只在 iOS 把 homework 六条路径合到一个 `NativeHomeworkDetailPage`（用 `title:` 区分，`NativeSecondaryPages.swift:2610`），Harmony 用同一个 `NativeHomeworkDetailPane`（`NativeSecondaryPages.ets:2692`）。**这就是"同一份 UI 写两遍"的精确清单，共 130 个手写页面类、9746 行。**

### 3.5 `libentry.so`：把 blocking Kotlin 变成 Promise

`napi_init.cpp:123-202` 实现了一个通用异步桥：`AuthOp` 枚举 + `AuthAsyncCtx`（含 `napi_deferred`）+ `AuthExecute`（工作线程里调 `KnAuth*Blocking`）+ `AuthComplete`（回主线程 settle promise）+ `StartAuthPromise`。原因写在 `MainArkUIViewController.kt:158-275`：Kotlin 侧认证是 `runBlocking` 的同步函数（内部 `result.fold`，失败信息写进 `errBuf`），NAPI 线程不能阻塞，所以 ArkTS 拿到的是 Promise（`AuthLoginWithPassword` 等 5 个，`:317-321`）。字符串回传用固定 2048B 缓冲区（`NapiCopyStringGetter:264-273`）。

注册顺序也是刻意的：`:310-312` 先 `napi_define_properties`，`:331` 才 `CallComposeArkUiInit(env, exports)`，注释（`:311`）说明「即使 Compose 初始化失败，ArkTS 的 import 也要能成功」；`androidx_compose_ui_arkui_init(env, exports)` 在 `:16-19`。

---

## 4. expect / actual 全景（全仓 27 个 `expect`，M15）

`ohosArm64` 与 `ohosX64` 是**两个不同 target**，能力差别很大；下表「占位侧」直接标出谁在说谎风险最高。

| expect（`路径:行`） | android | ios | ohosArm64 | ohosX64 | 占位/降级 |
|---|---|---|---|---|---|
| `composeApp/…/Platform.kt:7` `getPlatform()` | `Platform.android.kt:9` | `Platform.ios.kt:8` | `Platform.ohos.kt:11`（name "HarmonyOS"） | 同 `ohosMain` | — |
| `core/base/…/AppUuid.kt:10` `object AppUuid` | `uuidCpfMain/AppUuid.cpf.kt:7` | 同上 | 同上（CPF uuid4） | `ohosX64Main/AppUuid.ohosX64.kt:9`（自写 RFC4122，注释「Not product SoT」） | **ohosX64** |
| `core/network/…/ApiClient.kt:69` `createPlatformApiClient` | `androidMain/HttpClientFactory.android.kt:6`（OkHttp） | `iosMain/…:6`（Darwin） | `ohosArm64Main/…:7`（CPF Ktor + CIO，「HTTPS gate passed」） | `ohosX64Main/…:6`（`OhosApiClient` net_http cinterop） | x64 无流式（`:59` `GAP(R5)`） |
| `core/network/…/BackendLocalhost.kt:7` `remapBackendLocalhost` | `androidMain:3`（模拟器 `10.0.2.2` 探测 + 真机告警，3-38） | `iosMain:7` 直通 | `ohosMain:7` 直通 | 同 | — |
| `core/network/…/NetworkBootstrap.kt:25-33` 五个 | `androidMain:3-11`（"android", false, "1.0", "1"） | `iosMain:7-18`（`!Platform.isDebugBinary` + Info.plist 版本键） | `ohosMain:3-11`：**("ohos", false, null, null)** | 同 | **OHOS 无版本号/构建号** |
| `core/network/…/RealtimeWsTransport.kt:65` | `androidMain/realtime/…:7`（Ktor OkHttp） | `iosMain/…:7` | `ohosArm64Main/…:10`（Ktor CIO，**真实**） | `ohosX64Main/…:17`：`throw RealtimeWsError.Unsupported()`，注释「不做伪实现」 | **ohosX64 无 WebSocket** |
| `core/platform/…/PlatformBackHandler.kt:6` | `androidMain:7` | `iosMain:6` | `ohosMain:6` **no-op** | 同 | OHOS 返回拦截靠 ArkTS 侧 `onBackPressed` |
| `core/platform/…/PlatformCameraScan.kt:22`/`:30` | `androidMain:103`/`:129`（CameraX + MLKit，229 行） | `iosMain:99`/`:107`（AVFoundation，188 行） | `ohosMain:29`/`:33`：`Unavailable` + 红字「OHOS 相机扫码未接入（registry: missing）」 | 同 | **OHOS 扫码缺** |
| `core/platform/…/PlatformOpen.kt:4/6/8/14` | `androidMain:15/24/30/44`（真 Toast/Clipboard/Intent） | `iosMain:16/21/25/57`（UIApplication/UIPasteboard/UIAlertController toast） | `ohosMain:6/11/15/21`：**`println` 桩**；`openSystemNotificationSettings` 返回「Deferred · OHOS 系统通知设置未接入」 | 同 | **OHOS 这 4 个全是桩** |
| `core/platform/…/PlatformWebView.kt:25` | `androidMain:49`（`AndroidView`，122 行） | `iosMain:28`（`UIKitView`，187 行） | `ohosMain:21`：诚实失败横幅「OHOS WebView 未接入 Compose …能力状态：missing（registry）。请勿将本页视为验收通过。」 | 同 | **OHOS 岛内 WebView 缺**（ArkTS 自己有 `Web`，见 §6.4） |
| `core/platform/…/StringListStore.kt:34` `platformTodayYmd` | `androidMain:7` | `iosMain:7` | `ohosMain:11`：UTC 近似（11-33） | 同 | 时区近似 |
| `core/storage/…/KeyValueStore.kt:58` | `androidMain:12`（SharedPreferences） | `iosMain:14`（`NSUserDefaults(suiteName:)`） | `ohosMain:41`：**`OH_Preferences` NDK**（`platform.ArkData.Preferences`） | 同一份 `ohosMain` | — （M8.5 已删旧 `/tmp` 文件实现；进程内句柄缓存且不关闭） |
| `core/ui/…/PlatformNetworkImage.kt:23` | `imageCoilMain/…:20`（CPF Coil 真图） | 同 `imageCoilMain` | 同 `imageCoilMain` | `ohosX64Main/…:16`：**占位，url 被忽略** | **ohosX64 无网络图** |
| `feature/home/…/AnalyticsCharts.kt:17`/`:24` | `chartsVicoMain/…:41`/`:107`（Vico，156 行） | 同 | 同 | `feature/home/src/ohosMain/…:15`/`:24`：**空 `Box`**，注释「CPF-KMP-CMP 三方库清单尚无 Vico / KoalaPlot」 | **OHOS 图表空白** |
| `feature/media/…/MediaPlayer.kt:45` | `androidMain:12`（123 行） | `iosMain:22`（97 行） | `ohosMain:7`：**`StubMediaPlayer()`** | 同 | **OHOS 播放缺** |
| `component/…/PayGateway.kt:75`/`:77` | `androidMain:7`/`:16`（**返回 null**，带 `TODO(pay)`） | `iosMain:4`/`:7` `= null` | `ohosMain:7`/`:9` `= null` | 同 | **三端全 null** |
| `component/…/PushBridge.kt:99` `object PlatformPushSdk` | `androidMain:9` | `iosMain:9` | `ohosMain:9` | 同 | **三端 `fetchRegistrationToken() = null`，"Stub until Spike II"** |

读法建议：三端**壳**都是真的；但壳下面的 `expect/actual` 里，OHOS 有 9 行是占位或缺失（本表「占位/降级」列），且 `ohosX64` 比 `ohosArm64` 还多 3 项降级（UUID / 网络图 / WebSocket）。

---

## 5. 符号与 ABI

### 5.1 iOS：导出面

`composeApp/build.gradle.kts:39-48` 把 `:composeApp` 编成**静态 XCFramework `ComposeApp`**；`:61` `export(libs.compose.multiplatform.export)` 把 CMP 运行时导出进来。Swift 可见符号 = `MainViewController.kt` 里 14 个顶层 `fun`（`@objc` 隐式），命名空间前缀是 `MainViewControllerKt`：

| Kotlin | Swift 调用形式 | 出处 |
|--------|----------------|------|
| `MineIslandViewController(route:)` | `MainViewControllerKt.MineIslandViewController(route: "settings")` | `MainViewController.kt:28` ← `ContentView.swift:500` |
| `SecondaryRouteViewController(routeOrLabel:)` | `MainViewControllerKt.SecondaryRouteViewController(routeOrLabel:)` | `:51` ← `ContentView.swift:508` |
| `acceptDeepLink` | `DeepLinkEntryKt.acceptDeepLink(url.absoluteString)` | `iOSApp.swift:14-20`（`AcceptDeepLinkFromIos` 在 `:67`） |
| `Auth*` 8 个 | `MainViewControllerKt.AuthIsLoggedIn()` … | `:79-143` ← `ContentView.swift:65-67`、`NativeAuthPages.swift:311/335/348/428/461/475` |
| `MainViewController()` | 已 `@Deprecated`（`:16`），壳不再使用 | — |

### 5.2 OHOS：`libkn.so` 导出的 16 个 `@CName`

`composeApp/build.gradle.kts:51-56` 声明 `sharedLib { baseName = "kn" }`，产物即 `libkn.so` + 自动生成的 `libkn_api.h`。C 面符号 = `MainArkUIViewController.kt` 上的 `@CName`（`grep -n '@CName'` 得 16 条）：

| `@CName` 符号 | 定义行 | 用途 |
|---|---|---|
| `MainArkUIViewController` | `:33-34` | **ArkTS 必须认识的唯一入口符号** |
| `KnSetOhosHost` | `:81-82` | `(kind, routeCode)`：kind=0 Mine 岛；routeCode 表在 `:83-98` 注释（0 settings/1 personalized/2 membership/3 about/4 profile/5 addresses/6 calculator/7 deal_invoice/8 deal_invoice_upload/9 address_edit） |
| `KnSetOhosSecondaryRoute` | `:101-102` | 字符串路由；默认 `/home/search` |
| `KnApplyAuthSession` | `:113-114` | token/userId/displayName/phone 四指针 |
| `KnAuthLoginWithPasswordBlocking` / `…Otp…` / `…SendPhoneOtp…` / `…Register…` / `…LoginWithHuawei…` | `:158/:182/:206/:228/:254` | `runBlocking`，返回 Int 状态 + `errBuf` |
| `KnAuthCopyToken/DisplayName/UserId/Phone` | `:278/:285/:292/:299` | 2048B 缓冲回填 |
| `KnAuthIsLoggedIn` | `:306-307` | 返回 Int |
| `KnAuthLogout` | `:311-312` | — |
| `KnAudioOnPageHide` | `:318-319` | **`= Unit` 空实现**（桥两端都注册了，实际什么都不做） |

C++ 侧 `extern "C"` 声明在 `napi_init.cpp:44-58`，注册 16 个 ArkTS 可见名（`:313-328`）：`MainArkUIViewController, SetOhosHost, SetOhosSecondaryRoute, ApplyAuthSession, AuthLoginWithPassword, AuthLoginWithOtp, AuthSendPhoneOtp, AuthRegister, AuthLoginWithHuawei, AuthToken, AuthDisplayName, AuthUserId, AuthPhone, AuthIsLoggedIn, AuthLogout, AudioOnPageHide`。

注意这是**两层命名**：Kotlin `@CName("KnAuthLoginWithPasswordBlocking")` → C++ 包装 `NapiAuthLoginWithPassword` → ArkTS 名 `AuthLoginWithPassword`。ArkTS 侧类型契约是 `entry/src/main/cpp/types/libentry/Index.d.ts`（22 行），其中 `MainArkUIViewController: () => ArkUIViewController` 的返回类型 import 自 `compose/src/main/cpp/types/libcompose_arkui_utils`。**ABI 稳定性要求**：`AGENTS.md:127` 明写「keep that symbol stable unless you update both sides」。

### 5.3 运行时 ABI 检查

`Index.ets:827` 读 `deviceInfo.abiList` 并拼进错误消息（`:829`），失败消息有两类：`libentry.so failed (abi=…)`（模块根本没装进来）与 `MainArkUIViewController missing`（装进来了但符号没了）。`CMakeLists.txt:13-17` 按 `LIBKN_ABI_DIR` 选 `arm64-v8a` 或 `x86_64` 的 `libkn.so`，`:40-45` 设 RPATH `$ORIGIN`，让 `libentry.so` 与 `libkn.so` 同目录即可加载。**装错 ABI 的结果是加载期崩溃，不是编译错误。**

### 5.4 重新发布步骤（改完共享代码后）

| 目标 | 命令 | 拷到什么 |
|------|------|----------|
| 真机 arm64 / debug | `./gradlew :composeApp:publishDebugBinariesToHarmonyApp` | `composeApp/build.gradle.kts:261-277` |
| 真机 arm64 / release | `…publishReleaseBinariesToHarmonyApp` | 同结构，`:261-277` 循环 |
| 模拟器 x86_64 | `…publishDebugBinariesToHarmonyAppX64` | `:279-298`；**`:281-283` 的 description 自称 "Currently blocked by toolchain: Konan_cxa_demangle on linkDebugSharedOhosX64"** |

拷贝映射（`:266-276`）：
- `build/bin/ohosArm64/${type}Shared/libkn_api.h` → `entry/src/main/cpp/include/arm64-v8a/`
- `build/bin/ohosArm64/${type}Shared/libkn.so` → `entry/libs/arm64-v8a/`
- `harmonyResourceRoots` → `entry/src/main/resources/rawfile/composeResources/$pkg/`

`harmonyResourceRoots`（`:250-253`）目前**只有一行**：`"my_kmp_project.core.design.generated.resources" → core/design/src/commonMain/composeResources`。注释（`:246-249`）说明「模块目录」与「包名」必须成对维护，将来 feature 自带资源要往这张表加一行。

`:259` 的 `if (!androidOnly)` 意味着带 `-PandroidOnly=true`（CI 用的那套，`AGENTS.md:114`）时**这些任务根本不会注册**。输出根目录可用 `-PharmonyAppPath=/abs/path` 覆盖（`:229-238`，`AGENTS.md:116`）。

手工全流程（DevEco 场景）：publish → DevEco 里 Build/Run `harmonyApp/`；或一条命令 `./scripts/run-harmony.sh`（→ `runscript/runOhosApp-Mac.sh`，内部顺序见 §2.6 与 §6.3）。

---

## 6. 平台专属坑（每条都有文件级出处）

### 6.1 启动图标缓存
`AGENTS.md:98`：改图标后如果启动器显示旧图标，**必须卸载重装**。三端资产：Android 5 密度 `mipmap-*/{ic_launcher,launch_image}.png`（无 adaptive/round，`AGENTS.md:89`）；iOS `AppIcon.appiconset` 15 尺寸 + `LaunchImage.imageset` + `LaunchScreen.storyboard`（`AGENTS.md:96`）；Harmony `AppScope/app.json5` + `module.json5` 的 `$media:app_icon`、`startIcon.png`、`start_window_background = #F7FAFC`（`AGENTS.md:94-95`，`harmonyApp/entry/src/main/resources/base/element/color.json:4-5`）。三处启动底同值 `#F7FAFC`，Android 侧即 `res/values/colors.xml` 的 `splash_background`，且注释写明对齐 Flutter SoT。

### 6.2 `rendererBackend=fusion-renderer`
`gradle.properties` 设定（`AGENTS.md:118`），`composeApp/build.gradle.kts:76-91` 依此追加 **11 个 `-l…` linker opt**。切换它 = 换渲染后端，链接行必须跟着改；不要只改一处。

### 6.3 Harmony 手工流程与工具链门槛
`runOhosApp-Mac.sh` 把坑固化成检查：DevEco 必须存在且 ≥ 6.0.0（`:99-131`）；`HDC_BIN` 走 DevEco 内 `openharmony/toolchains/hdc`（`:137`）；先 `aa force-stop`（`:140-141`）；`ohpm install --all`（`:197`）→ `hvigorw --sync`（`:198`）→ `assembleHap`（`:199-203`）；签名缺失走 exit 6 并给提示（`:208-231`）；设备掉线检查（`:232-236`）；`bm install` 失败时先卸载再重试（`:238-256`）；debug 模式 `aa start -D`（`:273-281`）；**锁屏导致 `10106102` 有专门提示**（`:287-297`）。CI 不跑 iOS/OHOS（`AGENTS.md:114`）。

### 6.4 `ohosArm64` ≠ `ohosX64`
见 §4 表：x64 的 UUID 是自写桩、网络图是占位、WebSocket 直接抛 `Unsupported`、HTTP 走 `net_http` cinterop（`-lnet_http` 只在 x64 加，`build.gradle.kts:69-71`）。**真机（arm64）与模拟器（x64）能跑的功能集合不一样**，用模拟器验收会低估进度。

### 6.5 ArkTS 目录里的两处书写瑕疵
- `NativeFeatureCatalog.ets:57` 与 `:105` **重复键** `['订单中心', '/mall/orders']`（`grep -o "\['[^']*'" | sort | uniq -d` 实测只报出这一个）。值相同，行为无害，但 `Map` 后者覆盖前者，属冗余。
- `NativeFeatureCatalog.ets:174-179` 的缩进/括号异常，实测原文是：第 174 行把三条 `startsWith` 挤成一行，紧接的 175-176 与 177-179 **全部退回 2 空格**，而它上下相邻的 168-173 与 180-186 都是 4 空格。语义无误（classroom 四分支），但这是 `resolveNativePath`（`:132-218`）里唯一需要「看两遍才确定属于哪个 if」的地方，改动它极易误判作用域。

### 6.6 注册了但仓库内无消费者的 interop 名
`ComposeVideoInterops.ets:105-107` 注册 `'vodVideoInline'`、`'vodVideoPreview'`，`ComposeWebInterops.ets:29-30` 注册 `'agreementWebInline'`，均由 `Index.ets:224-225` 在启动时执行。`grep -rn 'vodVideoInline|agreementWebInline'`（全仓）**只命中这两个 ArkTS 文件**，Kotlin 侧当前没有任何地方按这些名字取用。两个文件都在首行注释说明必须深 import `@cpf-kmp-cmp/compose/src/main/ets/compose/ArkUIView`，因为包 Index 在 `compose 1.9.2-0.3.0` 里没有 re-export 这个 API。

### 6.7 Swift 的一个真实陷阱
`ContentView.swift:21-31` 自定义了 `Text(_:color:)` 重载，文件里有注释记录这类「以为改颜色实际改语义」的坑；配合 `:6-19` 的 `DesignTokens` 与 ArkTS 侧 `NativeShellShared.ets:2-10` 的同一组十六进制（`#171717/#4D4D4D/#888888/#FFFFFF/#F5F5F5/#0070F3/#EBEBEB`），三端的"色值契约"是手写三份、靠人工同步。底栏高度同理：`NativeTabRoots.swift:5`（49pt，注释声明与 `JetpackBottomBar` 同契约）与 `Index.ets:1948-1957`（`height(49)` + `#F2FFFFFF`）。

### 6.8 深链 scheme 只注册了 `myai`
三处一致：`AndroidManifest.xml:27-32`（scheme `myai`）+ `:33-42`（https App Link `app.myai.example`）；`iosApp/Info.plist` `CFBundleURLSchemes` 只列 `myai`；`harmonyApp/entry/src/main/module.json5:58-67` 的 `ohos.want.action.viewData` 也只 `scheme: "myai"`。仓库注释里出现过 `xiaomao` 字样但**没有任何一端注册它**。

### 6.9 派发一致性有两个守护脚本
`NativeRouteResolver` 的注释（`NativeFeatureCatalog.swift:47-52`）记录了「前缀吞掉」这一类 bug，并声明与 Harmony `nativePaneIdFor` 互为镜像；两侧分别由 `scripts/check-ios-page-dispatch.sh` 与 `scripts/check-harmony-pane-dispatch.sh` 校验，层级依赖由 `scripts/check-layer-deps.sh` 校验。（本文未运行这些脚本，见 §9。）

---

## 7. 真实状态矩阵

| 能力 | Android | iOS | Harmony arm64 | Harmony x64 |
|------|---------|-----|---------------|-------------|
| 壳是否存在 | ✅ 2680 行 Kotlin（M8） | ✅ 8463 行 Swift（M6） | ✅ 9790 行 ArkTS（M7） | 同一份 ArkTS |
| 原生页面完成度 | **最薄**：20 个自带 `@Composable`（M5）= Home/Chat/Community 三个 Tab 根 + 闪屏/隐私/未映射入口 + 局部件；`when (overlay)` 12 个 case 里 9 个渲染 `feature/*/commonMain` 共享 CMP（含认证四屏，§3.2） | ✅ 65 专用页 + 4 Tab 根 + 登录/注册 + 通用兜底（M1/M2） | ✅ 65 专用 Pane + 通用 Pane（§3.4.2）+ 登录/快登 Pane | 同一份 ArkTS |
| Mine 根 UI | ⚠️ **共享 CMP**（`NativeJetpackTabs.kt:1267-1292`）——与 ADR 0002 §2 冲突 | ✅ SwiftUI（`NativeMineRoot.swift:6`） | ✅ ArkTS（`Index.ets:1651`） | ✅ |
| Mine 岛（Compose） | ✅ `MineIsland`（`NativeAndroidMain.kt:546`） | ✅ `MineIslandViewController`（`ContentView.swift:497-503`） | ✅ `openComposeHost(0, code)`（`Index.ets:826-852`） | 依赖 x64 的 `libkn.so`，而 publish 任务自述 blocked（`composeApp/build.gradle.kts:281-283`） |
| Mine 二级（遗留 `SecondaryRouteIsland`） | ✅ `MineRouteHost` 走 ADR-0002 §8 的 `#23` 岛 | ✅ `MineSecondaryHost`（`ContentView.swift:505-511`） | ✅ `openSecondaryCompose`（`Index.ets:870-875`） | 同 |
| 非 Mine 二级 | ✅ 4 个共享 `RouteHost` + `AllServices/Membership/InAppWeb/Scan` | ✅ `NativeFeatureHost` 78 条原生分支（M13） | ✅ `nativePaneIdFor` + 65 Pane | 同 |
| 登录门 / 软锁 | ✅ `:124-145` + `:87-98` | ✅ `ContentView.swift:267-329` | ✅ `Index.ets:1017-1061` + `:855-868` | ✅ |
| 认证 UI 本体 | ⚠️ **共享 CMP 四屏**（`feature/auth/src/commonMain/…`） | ✅ SwiftUI `NativeAuthPages.swift`（543 行） | ✅ ArkTS `auth/NativeLoginPane.ets`（360）+ `HuaweiQuickLoginPane.ets`（298） | ✅ 同一份 ArkTS |
| 深链 | ✅ `myai` + App Link（Manifest `:27-42`） | ✅ `myai`（Info.plist）+ `onOpenURL`/`AppDelegate` | ✅ `myai`（`harmonyApp/entry/src/main/module.json5:58-67`）+ `AppStorage` 暂存（`EntryAbility.ets:24-31`） | ✅ |
| 认证（真实后端） | ✅ | ✅ | ✅ 走 NAPI promise（`napi_init.cpp:123-202`） | ⚠️ x64 HTTP 无流式（`GAP(R5)`） |
| 华为快登 | ➖ 不适用 | ➖ 不适用 | ✅ ArkTS `HuaweiQuickLoginPane.ets`（298 行）+ `KnAuthLoginWithHuaweiBlocking`（`:254`） | ⚠️ |
| 网络图 | ✅ Coil | ✅ Coil | ✅ Coil（`imageCoilMain`） | ❌ 占位 |
| WebSocket 实时 | ✅ | ✅ | ✅ Ktor CIO | ❌ `throw Unsupported` |
| 图表 | ✅ Vico | ✅ Vico | ❌ 空 `Box`（`AnalyticsCharts.ohos.kt:15/24`） | ❌ |
| 岛内 WebView | ✅ | ✅ | ❌ 横幅自认 missing（`PlatformWebView.ohos.kt:21`）；ArkTS 另有 `WebPage.ets`/`ComposeWebInterops.ets` | ❌ |
| 相机扫码 | ✅ CameraX+MLKit | ✅ AVFoundation | ❌ `Unavailable` + 红字 registry missing | ❌ |
| 音频播放（共享侧） | ✅ | ✅ | ❌ `StubMediaPlayer()`（`MediaPlayer.ohos.kt:7`）；`KnAudioOnPageHide` 也是 `= Unit` | ❌ |
| toast / 剪贴板 / 打开外链 / 通知设置 | ✅ | ✅ | ⚠️ 仅 toast 有 println 语义，其余为桩（`PlatformOpen.ohos.kt:6/11/15/21`）；ArkTS 壳自己有 `promptAction`（`Index.ets:6`） | ⚠️ |
| KV 持久化 | ✅ | ✅ | ✅ `OH_Preferences`（`KeyValueStore.ohos.kt:41`） | ✅ 同一份实现 |
| 客户端标识上报 | ✅ "1.0"/"1" | ✅ Info.plist 版本 | ❌ `null`/`null`（`NetworkBootstrap.ohos.kt:7/9`） | ❌ |
| 支付（微信/支付宝） | ❌ 返回 null | ❌ null | ❌ null | ❌ null |
| 推送 | ❌ stub | ❌ stub | ❌ stub | ❌ stub |
| CI 覆盖 | ✅（`assembleDebug` + `testDebugUnitTest`，`AGENTS.md:114`） | ❌ 不在托管 runner | ❌ | ❌ |
| 产物当前是否在工作树 | APK 未构建（本文不 build） | XCFramework 未构建 | ❌ `entry/libs`、`cpp/include`、`rawfile/composeResources` **均不存在**（M14），且被 gitignore（`harmonyApp/.gitignore:14-17`、`.gitignore:85-90`） | 同 |

**三句话总结**
1. Android：壳是真的，但因为「原生」和「共享」都是 Compose，边界最模糊——`JetpackMineRoot` 直接把 Mine 根交给了共享 CMP，四个 `RouteHost`、`AllServices/Membership/InAppWeb/Scan` 与认证四屏也是共享 CMP。真正只属于 Android 的屏是 Chat/Community 根（`NativeJetpackTabs.kt:86/:840`）、Home 根的壳层与局部件（`NativeJetpackHome.kt`）、闪屏/隐私（`NativeAndroidApp.kt:77/:100`）与未映射入口（`NativeAndroidMain.kt:775-794`）。
2. iOS / Harmony：壳与共享代码物理隔离，各自手写 65 个页面/Pane，语言不同无法复用，是本项目最昂贵也最不可隐藏的一笔重复。Compose 只出现在 2 处（iOS）/ 1 处（Harmony）容器里。
3. Harmony 的"能跑"完全依赖一次显式 publish（§5.4），产物不在版本库里（M14），因此**任何关于 Harmony 渲染通过的断言都必须附一次当场发布记录**，否则读到的可能是旧 `libkn.so`。

---

## 8. 与 ADR 的偏差清单（诚实条款）

| ADR 条款 | 出处 | 实际 | 证据 |
|----------|------|------|------|
| 0002 §1 原生 UI 归各端所有 | `docs/adr/0002-native-shell-mine-compose-island.md:13` | 成立，但**三端厚薄差异极大**：iOS 65 页 / Harmony 65 Pane 各自手写；Android 只有 20 个自带 Composable，且 overlay 12 case 中 9 个交给共享 CMP | M1/M4/M5 + §3.2 表 |
| 0002 §2 Mine **root** 保持原生 | `:14` | **Android 违反**：`JetpackMineRoot` 渲染共享 `MineHomeContent` | `NativeJetpackTabs.kt:1267-1292`（注释 1276） |
| 0002 §3 认证 UI 原生 | `:15` | **iOS / Harmony 遵守**（`NativeAuthPages.swift:23/364`、`auth/NativeLoginPane.ets`、`auth/HuaweiQuickLoginPane.ets`）；**Android 违反**：`NativeAndroidMain.kt:643-667` 渲染的四个屏全在 `feature/auth/src/commonMain/` | `LoginScreen.kt:43`、`LoginPasswordScreen.kt:37`、`LoginOtpScreen.kt:42`、`RegisterScreen.kt:42` |
| 0002 §4 返回先弹岛 | `:16` | Android `closeOverlay`/pop 系列 `:159-260`；iOS `fullScreenCover` 天然隔离；Harmony `Index.ets:2397-2410` | — |
| 0002 §6 一期范围上限 | `:18` | 被 0003 废止（Full Parity Phase） | `docs/adr/0003-full-parity-phase.md` |
| 0002 §8 `SecondaryRouteIsland` 仅 Mine 且属遗留债 | `:20` | 三端确实只在 Mine 二级使用它，iOS/Harmony 未迁移到新岛——**遗留债仍在**，但没被扩用 | `ContentView.swift:505-511`、`Index.ets:870-875`、`MainViewController.kt:51-64`、`MainArkUIViewController.kt:38-44` |
| 0002 结论「Android 承载两套 Compose 栈」 | `:25-26` | 成立，且比措辞更严重：Mine 根也属于「另一套」 | 同上 |
| `AGENTS.md:127` 保持 `MainArkUIViewController` 符号稳定 | `AGENTS.md:127` | 成立；`Index.d.ts` + `napi_init.cpp:313` + `@CName`（`:33`）三处同名 | — |
| `AGENTS.md:129` release 保持 `optimized = false` | `AGENTS.md:129` | 成立（`build.gradle.kts:58-60`，注释避免 DevirtualizationAnalysis OOM） | — |

---

## 9. 本文未验证的疑点（写清楚，别装懂）

| # | 疑点 | 为什么没验 |
|---|------|-----------|
| 1 | 三端 UI 是否与 Flutter SoT 像素级一致 | 本文不 build、不截图（任务禁止）；ADR 0002 §5 的 ≤2% 偏差指标未复核 |
| 2 | `scripts/check-ios-page-dispatch.sh` / `check-harmony-pane-dispatch.sh` 当前是否通过 | 未执行脚本（只读约束）；§6.9 只说明它们的存在与注释声明 |
| 3 | 真机上 `libkn.so` 是否加载成功、岛是否渲染正常 | 需要设备 + publish + DevEco；M14 表明产物缺失，本文只能证明「当前工作树里跑不起来」 |
| 4 | `libkn_api.h` 的实际内容 | 文件是生成物且被 gitignore（`.gitignore:90`），不存在；§5.2 的符号表是从 `@CName` 与 `napi_init.cpp:313-328` 反推的，不是从 header 读的 |
| 5 | `@cpf-kmp-cmp/compose` 内部 `registerComposeInteropBuilder` 与 Kotlin 侧的对接细节 | 三方库源码不在本文范围；§6.6 只陈述「仓库内 Kotlin 无引用」这一 grep 事实，**不断言它无效果** |
| 6 | iOS `XCFramework` 的链接产物、Swift 65 页与 Harmony 65 Pane 的逐页内容等价性 | 计数与结构可比，像素/文案未逐页 diff；`LABEL_TO_PATH`(84) 与 `labelToPath`(83) 的条目差集未逐条列出 |
| 7 | `harmonyApp/entry/src/main/resources/base/media` 的 172 张图与三端引用是否一一对应 | 只做了数量清点；未反查每张图的引用点（例如 `main_tab_*_selected/unselected` 在 `Index.ets:1895` 被引用，其余多数未核） |

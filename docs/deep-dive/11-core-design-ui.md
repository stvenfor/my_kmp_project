# 11 · core:design 与 core:ui 深度文档

> 分册 ④ 水平 commons 中的**设计系统层**。覆盖 `core/design/`（设计令牌 SoT + 沉浸式顶栏 + 全量 composeResources）与 `core/ui/`（跨端 UI 原子件：网络图、Tab chrome、权限门）。
> 每条事实均附仓库相对路径行号；所有结论均来自整篇源码阅读，未跑 Gradle/build。
> 依赖链、target 接线以 `build.gradle.kts` 实证；调用点以 `file:line` 列出。

---

## 1. 模块定位 + 依赖关系（build.gradle.kts 实证）

### 1.1 分层位置

| 模块 | Gradle 路径 | 分层 | 一句话职责 |
|------|-------------|------|-----------|
| `core/design` | `:core:design` | ④ 水平 commons | 设计令牌单一事实源（SoT）+ 沉浸式顶栏 + 我的页图标库 + **全工程唯一**的 composeResources 归属 |
| `core/ui` | `:core:ui` | ④ 水平 commons | 跨端 UI 原子件：网络图 `PlatformNetworkImage`、Tab chrome 控制器、权限门 `PermissionGate` |

`settings.gradle.kts:27` `include(":core:design")`、`settings.gradle.kts:32` `include(":core:ui")`（`core:platform` 在 `:31`）。

### 1.2 依赖链（谁依赖谁）

两个模块自身几乎不往外依赖业务模块，靠**传递依赖**把设计层送进 feature：

```
feature:xxx ──api──▶ core:ui ──api──▶ core:platform ──api──▶ core:design
     │                                      └──api──▶ core:account
     └──（多数 feature 只显式 api(core.ui)，design 靠链路带出）
composeApp ──implementation──▶ core:design（直接）
composeApp ──implementation──▶ core:ui（直接）
```

实证：
- `core/ui/build.gradle.kts:17` `api(projects.core.platform)` —— core:ui 唯一的项目级依赖就是 core:platform；core:design 靠 core:platform 的 `api` 传递（`core/platform/build.gradle.kts:17` `api(projects.core.design)`）。
- `core/design/build.gradle.kts` **没有任何 `projects.*` 依赖**，只有 Compose 库（见 1.3）——它是依赖图最底部的叶子。
- feature 侧普遍 `api(projects.core.ui)` 并注明「传递带出 core.platform → core.design / core.account」：`feature/mine/build.gradle.kts:13`、`feature/home/build.gradle.kts:27`、`feature/chat/build.gradle.kts:15`、`feature/community/build.gradle.kts:14` 等。
- `composeApp/build.gradle.kts:125` `implementation(projects.core.design)`、`:130` `implementation(projects.core.ui)` —— App 装配层直接持有两者。

> 结论：**没有 feature 直接 `api(projects.core.design)`**；设计令牌/顶栏是通过 `core.ui → core.platform → core.design` 这条 api 链被 feature 引用的。若某天 core.platform 把 core.design 降级成 implementation，全线 feature 的 `DemoColors`/`MineTopBar` import 会瞬间断裂——这是一条隐含的强耦合。

### 1.3 两模块 build.gradle.kts 逐行

`core/design/build.gradle.kts`：
- 插件 `:8-11`：`kotlinMultiplatform` + `androidLibrary` + `composeMultiplatform` + `composeCompiler`。
- `commonMain` 依赖 `:16-22`：`api(compose.runtime / foundation / material3 / ui / components.resources)`——全部用 `api` 暴露，保证下游拿到 Compose 类型。
- `compose { resources { publicResClass = true } }` `:26-30`：**这是把 `Res` 类开放给其它模块的关键**（见 §2 资源组织）。
- `android { namespace = "com.example.my_kmp_project.core.design" }` `:32-34`。
- 文件头注释 `:1-6` 明确「资源放这里而不是 :composeApp：任何 feature 都要用 Res.drawable.\*，但它们不能反向依赖 :composeApp」。

`core/ui/build.gradle.kts`：
- 插件同上 `:8-11`。
- `commonMain` `:16-23`：`api(projects.core.platform)` + 5 个 compose api。
- 中间 source set `imageCoilMain` `:24-27`：`maybeCreate("imageCoilMain")` → `dependsOn(commonMain)` + `implementation(libs.coil.compose)`。
- `androidMain` `:28-31`：`dependsOn(imageCoilMain)` + `implementation(libs.coil.network.okhttp)`。
- `iosMain` `:33-36`、`ohosArm64Main` `:37-40`：用 **惰性匹配** `matching { it.name == ... }.all { dependsOn(imageCoilMain); implementation(libs.coil.network.ktor3) }`。注释 `:32` 说明原因——androidOnly CI 不生成 ios/ohos target，`getByName` 会抛异常，`matching` 才不会。
- `android { namespace = "com.example.my_kmp_project.core.ui" }` `:44-46`。
- 文件头注释 `:1-6`：Coil 只挂 android/ios/ohosArm64，ohosX64 无 CPF Coil klib，由 `PlatformNetworkImage.ohosX64.kt` 占位 actual 顶上。

### 1.4 target 与 source set 接线（根 build.gradle.kts 共享段）

两模块自身不声明 target，由根 `build.gradle.kts` 的 `subprojects {}` 统一注入：
- `build.gradle.kts:87-92` androidTarget（JVM_17）。
- `:94-102` 非 androidOnly 时 `iosX64/iosArm64/iosSimulatorArm64/ohosArm64/ohosX64`；库模块**不产 framework/libkn.so**（注释 `:95-96`：全工程只有 `:composeApp` 是二进制产出方）。
- `:118-127` 建 `iosMain`（dependsOn commonMain）并把三个 ios\*Main 挂上；建 `ohosMain`，再把 `ohosArm64Main`、`ohosX64Main` 各 `dependsOn(ohosMain)`（`:125-126`）。

> 关键交互：`imageCoilMain` 的 `expect→actual` 之所以能覆盖 ohosArm64，是因为 `core/ui` 里让 `ohosArm64Main.dependsOn(imageCoilMain)`，而根接线让 `ohosArm64Main.dependsOn(ohosMain)`。`ohosX64Main` **不**dependsOn imageCoilMain，所以它必须自带一个 `actual`（即 `PlatformNetworkImage.ohosX64.kt`）。

---

## 2. 文件清单表（路径 | 作用 | 关键行号）

源码（非 generated）：

| 路径 | 作用 | 关键行号 |
|------|------|---------|
| `core/design/build.gradle.kts` | 设计模块构建；publicResClass | `:26-30` |
| `core/design/src/commonMain/…/DemoColors.kt` | Compose `Color` 适配层，包 DesignTokens | `:10-38` |
| `…/DemoFonts.kt` | Geist 变量字体 FontFamily | `:14-23` |
| `…/DesignSystem.kt` | 渐进式令牌别名面（桥接 MineTheme） | `:10-22` |
| `…/DesignTokens.kt` | **SoT**：ARGB 令牌 + Spacing/Typography/Radius | `:12-72` |
| `…/ImmersiveInsets.kt` | 沉浸式 inset 助手 + `MainBottomBarHeight=49dp` | `:27-53` |
| `…/ImmersiveTopAppBar.kt` | 状态栏沉浸式顶栏 ×2 | `:26-54` / `:58-86` |
| `…/MineIcons.kt` | 20 个自定义 `ImageVector`（OHOS 安全） | `:18-656` |
| `…/MineTopBar.kt` | 二级页通用顶栏（包 ImmersiveCenterTopAppBar） | `:26-69` |
| `core/design/src/commonTest/…/DesignTokensSoTTest.kt` | 锁定令牌 = Flutter SoT 字面量 | `:9-37` |
| `core/design/src/commonMain/composeResources/` | **全工程唯一** drawable/font 资产根 | 见 §2.1 |
| `core/ui/build.gradle.kts` | UI 模块构建 + imageCoilMain 接线 | `:24-40` |
| `core/ui/src/commonMain/…/MainTabChrome.kt` | Tab chrome 控制器 + Local + ReportMainTabRoot | `:11-30` |
| `…/PermissionGate.kt` | 权限 UX 壳 + 3 个面板 | `:31-174` |
| `…/PlatformNetworkImage.kt` | `expect` 网络图 + `secureNetworkImageUrl` | `:9-29` |
| `core/ui/src/imageCoilMain/…/PlatformNetworkImage.coil.kt` | Coil `actual`（android/ios/ohosArm64） | `:19-47` |
| `core/ui/src/ohosX64Main/…/PlatformNetworkImage.ohosX64.kt` | 占位 `actual`（ohosX64） | `:15-33` |

`composeResources` 归属实证：`composeApp/src/commonMain/composeResources` 与 `core/ui/src/commonMain/composeResources` **均不存在**（`ls` 返回 none）——所有位图/字体只在 `core/design`。

### 2.1 composeResources 组织

| 目录 | 数量 | 组成 |
|------|------|------|
| `drawable/` | **170 个文件**（168 `.png` + 2 `.jpg`） | 位图；2 张 jpg 为 `music_defaults_lady.jpg`、`music_defaults_music_record.jpg` |
| `font/` | **1 个** | `geist_variable.ttf`（Geist 可变字体） |

build 注释 `core/design/build.gradle.kts:1` 写「171 个 drawable/font」= 170 + 1，与文件系统一致。

drawable 按**前缀分组**（无子目录，靠命名聚类，前缀≈业务域）：

| 前缀簇 | 代表 | 用途 |
|--------|------|------|
| `main_tab_*` | `main_tab_home_selected/unselected`、chat/community/me | 主底栏 4 tab 图标（选中/未选中各一） |
| `home_feature_*` | `home_feature_data/life/live/market/newcar/…` | 首页功能卡 |
| `home_all_services_*` | `home_all_services_calculator/smart_sale/v_store/…` | 全部服务九宫格 |
| `home_transliter_home_*` | `…_cover_01..12`、`…_rank_icon_*`、`…_feature_*` | 首页「转译/影视」大区块（cover 12 张 + rank 徽标） |
| `home_search_*` | `home_search_rank_1..8`、`home_search_icon_*`、`home_search_microphone` | 搜索页热榜 1–8 名次图 + 图标 |
| `pay_membership_*` | `…_header_ai_svip/svip`、`…_tab_*`、`…_icon_*`、`…_illustration_*` | 会员购买页整块素材（最大簇） |
| `community_*` | `community_post_a/b/dual/video`、`community_avatar` | 社区帖图/头像 |
| `classroom_*` / `video_*` / `mall_*` / `mine_*` / `wallet_*` / `music_*` / `deal_invoice_*` / `settings_*` | 各业务页 body 长图与图标 | 二级页整屏截图型资源 |
| `ic_*` / `bg_*` | `ic_nav_back`、`ic_splash_logo`、`bg_splash` | 通用返回箭头 + 启动 logo/底 |

**生成产物与引用方式**（`core/design/build/generated/compose/resourceGenerator/…`）：
- `Res` object 包名 `my_kmp_project.core.design.generated.resources`（`Res.kt:7`），内含 `object drawable`（`:38`）、`object font`（`:46`）等嵌套。
- 因 `publicResClass = true`，字段生成为 `public val Res.drawable.xxx: DrawableResource`——accessor 被拆成 **Drawable0（100 个）+ Drawable1（70 个）+ Font0（1 个）**（生成器单文件字段上限，100+70=170 与源目录吻合）。
- 引用范式：
  - 字体：`DemoFonts.kt:18-21` `Font(Res.font.geist_variable, weight = …)`。
  - 位图（design 内部）：`MineTopBar.kt:49` `painterResource(Res.drawable.ic_nav_back)`。
  - 位图（feature 跨模块）：先 `import my_kmp_project.core.design.generated.resources.Res` 与逐资源 import（如 `HomeSearchScreen.kt:54-59`），再 `painterResource(Res.drawable.home_search_rank_1)`（`HomeSearchScreen.kt:192`、`AllServicesScreen.kt:132`）。
  - 作为网络图占位：`CommunityRouteHost.kt:623` `placeholder = Res.drawable.community_post_a`。
  - 启动屏：`AppShell.kt:140/153`、`NativeAndroidApp.kt:84/90` 读 `bg_splash` / `ic_splash_logo`。

> AGENTS.md 记录：`core/design/src/commonMain/composeResources/` 持有全部 drawable/font，`:composeApp` 不再有自己的 composeResources——已核实。

---

## 3. 逐个 public API / @Composable

约定：**签名忠实抄自源码**；「调用点」只列 grep 命中的真实行；无命中即标注「仓库内暂无调用点」。

### 3.1 core.design —— 令牌对象（非 Composable）

#### `object DemoColors`（`DemoColors.kt:10-38`）
私有 `fun argb(token: DesignTokens.Argb): Color = Color(token.value)`（`:11`）把 packed Long 转成 Compose `Color`。字段全为 `val`（eager 初始化，非 lazy、非 @Composable）。映射表（**左列是 DemoColors 名，右列实际取自 DesignTokens**）：

| 字段:行号 | 取自 DesignTokens | 十六进制 | 备注 |
|-----------|-------------------|----------|------|
| `Primary :13` | `Color.Link` | `#0070F3` | ⚠️ **不是** DesignTokens.Color.Primary（见坑 §7.2） |
| `OnPrimary :14` | `Color.OnPrimary` | `#FFFFFF` | |
| `Accent :15` | `Color.Link` | `#0070F3` | 与 Primary 同值 |
| `Background :16` | `Color.Canvas` | `#FFFFFF` | |
| `PageBg :17` | `Color.CanvasSoft2` | `#F5F5F5` | MineTopBar 默认底色 |
| `CanvasSoft :19` | `Color.CanvasSoft` | `#FAFAFA` | 注释 `:18` 指标卡底色，比 PageBg 浅一档 |
| `OnBackground :20` | `Color.Ink` | `#171717` | |
| `OnSurface :21` | `Color.Body` | `#4D4D4D` | |
| `Outline :22` | `Color.HairlineStrong` | `#A1A1A1` | |
| `Muted :23` | `Color.Mute` | `#888888` | |
| `Danger :24` | `Color.Error` | `#EE0000` | |
| `TextPrimary :25` | `Color.Ink` | `#171717` | |
| `TextSecondary :26` | `Color.Body` | `#4D4D4D` | |
| `Divider :27` | `Color.Hairline` | `#EBEBEB` | |
| `Toolbar :28` | `Color.Canvas` | `#FFFFFF` | |
| `TabBarBackground :29` | `Color.TabBarBackground` | `#F2FFFFFF` | 带 alpha F2 |
| `LinkDeep :32` | `Color.LinkDeep` | `#0761D1` | VercelTokens 强调族（`:31` 注释：首页九宫格轮转） |
| `LinkBgSoft :33` | `Color.LinkBgSoft` | `#D3E5FF` | |
| `Warning :34` | `Color.Warning` | `#F5A623` | |
| `Violet :35` | `Color.Violet` | `#7928CA` | |
| `Cyan :36` | `Color.Cyan` | `#50E3C2` | |
| `HighlightPink :37` | `Color.HighlightPink` | `#FF0080` | |

调用点（示例，遍布全线）：`AppShell.kt:345` `.background(DemoColors.Background)`、`MainBottomBar.kt:86` `background(DemoColors.TabBarBackground)`、各 feature 顶栏传 `containerColor = DemoColors.PageBg/Background/Toolbar`。

#### `object DemoFonts`（`DemoFonts.kt:14-23`）
唯一成员 `val Geist: FontFamily`，带 `@Composable get()`（`:15-16`）。getter 内用 `FontFamily(Font(Res.font.geist_variable, weight=…))` 注册 Normal/Medium/SemiBold/Bold 四档（`:18-21`），四档都指向同一可变字体文件。注释 `:10-12` 说明动机：Flutter SoT `VercelTypography.fontFamily = Geist`，打包同一 TTF 以便 Android 截图 diff gate 收敛。
调用点：`MainBottomBar.kt:159` `fontFamily = DemoFonts.Geist`、`HomeSections.kt:79/89/99`。

#### `object DesignSystem`（`DesignSystem.kt:10-22`）
纯转发别名面，11 个 `val` 全 `get() = DemoColors.*`（无独立值）：`Accent/Primary/OnPrimary/PageBg/Surface(=Background)/TextPrimary/TextSecondary/Danger/Divider/Muted/Toolbar`。注释 `:3-8` 定位为「Mine/会员/共享 chrome 的渐进式别名面」，MineTheme 可桥接部分字段而不做全量迁移（task 4.4）。
调用点：`feature/mine/MineTheme.kt:15` `Accent = DesignSystem.Accent`、`:16` `Background = DesignSystem.PageBg`。

### 3.2 core.design —— @Composable 顶栏族

#### `ImmersiveCenterTopAppBar`（`ImmersiveTopAppBar.kt:26-54`，`@OptIn(ExperimentalMaterial3Api) @Composable`）
完整签名：
```kotlin
fun ImmersiveCenterTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    containerColor: Color = DemoColors.Background,
    colors: TopAppBarColors? = null,
)
```
参数语义：`title/navigationIcon/actions` 透传给 M3 `CenterAlignedTopAppBar`；`modifier` 作用在**内层 bar**（非外层背景）；`containerColor` 是外层铺到状态栏背后的色，默认白；`colors` 若给则整体覆盖 M3 配色，否则用 `:34-37` 生成的透明 `topAppBarColors`（背景色改由外层 Box 承担）。
内部真实行为：外层 `Box(Modifier.fillMaxWidth().background(containerColor))`（`:38-42`）把颜色**画到状态栏区域**；内层 `CenterAlignedTopAppBar` 加 `.statusBarsPadding()`（`:46`）让内容下沉避开状态栏，并强制 `windowInsets = WindowInsets(0,0,0,0)`（`:47`）关掉 M3 自带 inset，避免双重 padding。类注释 `:18-23`：这样状态栏区在所有平台（尤其 iOS SwiftUI `ignoresSafeArea` 之后）与 toolbar 底色一致。
调用点：`MineTopBar.kt:35`（本模块内部委托）。**feature/composeApp 无直接调用**——二级页统一走 `MineTopBar`。

#### `ImmersiveTopAppBar`（`ImmersiveTopAppBar.kt:58-86`）
签名与默认值与上者**逐字相同**，仅内层用左对齐 `TopAppBar`（`:75`）替代 CenterAligned。
调用点：**仓库内暂无调用点**（仅 AGENTS.md 列为「左对齐顶栏」可用件），属预留 API。

#### `MineTopBar`（`MineTopBar.kt:26-69`，`@OptIn(ExperimentalMaterial3Api) @Composable`）
完整签名：
```kotlin
fun MineTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    containerColor: Color = DemoColors.PageBg,
    titleColor: Color = DemoColors.TextPrimary,
    titleFontSize: TextUnit = 18.sp,
)
```
参数语义：`title` 是 `String`（内部自渲染成 `Text`，非 composable lambda，`:38-43`，`FontWeight.Bold`）；`onBack` 为 null 时**完全不渲染返回按钮**（`:46` 条件），非 null 时渲染 `IconButton` 包 `ic_nav_back` 图（`:48-55`，`Modifier.size(20.dp).rotate(180f)`、`ContentScale.Fit`、contentDescription="返回"）；`containerColor` 默认 `PageBg`(#F5F5F5)，多数调用方会覆盖成 White/Background/Black；`titleColor`、`titleFontSize` 控制标题。
内部真实行为：委托 `ImmersiveCenterTopAppBar`（`:35`），并在 `:61-67` 显式传一套 `TopAppBarDefaults.topAppBarColors`：container/scrolled 透明（背景交给外层 Box），title/navigationIcon/action 三个 contentColor 都设成 `titleColor`。因此 `MineTopBar` 是「居中标题 + 可选返回」的二级页顶栏封装。
调用点（**极多**，二级页标配，节选）：`feature/media/MusicScreens.kt:117`、`VideoRouteHost.kt:179/424/491/518`、`VideoHubScreen.kt:70/175/251`、`feature/friend/FriendScreen.kt:104/418`、`feature/chat/ChatScreen.kt:332`、`feature/web/InAppWebScreen.kt:42`、`feature/auth/RegisterScreen.kt:74`/`LoginOtpScreen.kt:66`/`LoginPasswordScreen.kt:56`、`feature/classroom/ClassroomScreen.kt:139/270/313`/`ClassroomRouteHost.kt:131/175`、`feature/community/CommunityRouteHost.kt:140/410/589/666/700`、`feature/scan/ScanScreen.kt:69`、`feature/live/LiveScreen.kt:71/113`、`feature/mine/MineSettingsScreen.kt:97/192`/`MinePersonalizedSettingsScreen.kt:72`/`DealInvoiceScreens.kt:100`/`MineRouteHost.kt:142/171/278/298/736/903/1063/1285/1325/1381/1423/1497/1546/1571/1613`、`feature/home/HomeDetailScreens.kt:64/136/216`/`LedgerScreens.kt:59/93`/`HomeRouteHost.kt`（十余处）、`feature/shell/SecondaryRouteIsland.kt:99`；android 原生壳 `NativeJetpackTabs.kt:49` import。

#### `object MineIcons`（`MineIcons.kt:18-656`）
非 Composable：20 个 `val …: ImageVector by lazy`（`:19-39`）——`Info/Calendar/Settings/Logout/Login/Person/Bag/CreditCard/PlayRect/DocText/Sms/Calculate/Car/PlayCircle/Build/QrCode/Support/Chart/ChevronDown/Check`。
私有 `fun solid(block: PathBuilder.() -> Unit): ImageVector`（`:41-60`）统一用 `ImageVector.Builder(defaultWidth/Height = 24.dp, viewportWidth/Height = 24f)` + 单条 `path(fill = SolidColor(Color.Black), …, pathFillType = NonZero)`。每个图标 = 一个 `private fun xxx() = solid { moveTo/lineTo/curveTo… }` 手写矢量路径（`:62-655`）。
设计意图（类注释 `:12-17`）：**OHOS 安全**——不引 `material-icons-extended`，全部自绘 ImageVector；填充色恒为 Black，**着色交给调用方** `Icon(imageVector = MineIcons.X, tint = …)`。
调用点：`feature/mine/MineModels.kt:67-154`（快捷服务/功能列表图标枚举）、`MineComponents.kt:233` `Check`、`MineHomeContent.kt:221-226/266-270/346/366`（Info/Calendar/Settings/Person/Login/ChevronDown/CreditCard）、`MineRouteHost.kt:781/797`（Person/Info）、`feature/home/HomeRouteHost.kt:3080/3138`（Build）。`Logout/PlayRect/DocText/Sms/Calculate/Car/PlayCircle/QrCode/Support/Chart` 亦经 `MineModels.kt` 引用。

### 3.3 core.ui —— Tab chrome

#### `class MainTabChromeController`（`MainTabChrome.kt:11-19`）
`var bottomBarVisible by mutableStateOf(true) private set`（`:12-13`，外部只读、可观察）；`fun updateBottomBarVisible(visible: Boolean)`（`:15-18`）——值不变则 early-return，避免无谓重组。

#### `val LocalMainTabChrome`（`:21`）
`staticCompositionLocalOf<MainTabChromeController?> { null }`。默认 null，即没被 provide 时消费方拿到 null 而非抛异常。
provide 点：`AppShell.kt:228` `remember { MainTabChromeController() }`，`:339` `LocalMainTabChrome provides tabChrome`。

#### `ReportMainTabRoot`（`MainTabChrome.kt:23-30`，`@Composable`）
```kotlin
fun ReportMainTabRoot(isRoot: Boolean)
```
语义：当前页声明自己是不是**Tab 根**。内部读 `LocalMainTabChrome.current`（`:25`），`LaunchedEffect(isRoot, chrome) { chrome?.updateBottomBarVisible(isRoot) }`（`:27-29`）。注释 `:26` 明确**不在 onDispose 复位**——否则与 Scaffold padding 打架、抖重组。
调用点：`isRoot = true` 者 = Tab 根：`feature/home/HomeScreen.kt:114`、`feature/mine/MineScreen.kt:59`、`feature/chat/ChatScreen.kt:86`、`feature/community/CommunityScreen.kt:66`；`isRoot = false` 者 = 二级页（隐藏底栏），遍布 media/friend/classroom/community/home/mine/live/web/scan/ai/commerce/shell（如 `feature/media/.../VideoHubScreen.kt:64`、`:91`、`VideoRouteHost.kt:177…581`、`HomeRouteHost.kt` 数十处、`MineRouteHost.kt` 数十处、`ScanScreen.kt:54`、`DeferredDestinationStub.kt:29`、`SecondaryRouteIsland.kt:97`、`MembershipScreen.kt:44`、`AiStreamScreen.kt:40` 等）。
> 注意 `MineScreen.kt` 同时有 `:59 true` 与 `:76 false`，`ChatScreen.kt` 有 `:86 true`/`:76 false`——同文件内按分支（是否处于二级栈）上报，不是 bug。

### 3.4 core.ui —— 权限门

#### `PermissionGate`（`PermissionGate.kt:31-74`，`@Composable`）
```kotlin
fun PermissionGate(
    permission: CameraPermissionController,
    rationale: String,
    deniedTitle: String,
    deniedMessage: String,
    requestLabel: String = "申请权限",
    unavailableTitle: String = "能力不可用",
    unavailableMessage: String,
    autoRequestWhenUnknown: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
)
```
参数语义：`permission` 来自 core.platform 的 `CameraPermissionController`（`PlatformCameraScan.kt:14-19`：`status`/`refresh()`/`requestPermission()`/`openAppSettings()`）；`rationale` 给未决态说明；`denied*` 给拒绝态标题/文案；`unavailable*` 给不可用态；`autoRequestWhenUnknown=true` 时首帧自动弹权限；`content` 仅 Granted 才渲染。
内部真实行为：`LaunchedEffect(permission, autoRequestWhenUnknown)` 先 `permission.refresh()`（`:45`），若 auto 且 `status == Unknown` 则 `requestPermission()`（`:46-48`）。随后 `when(permission.status)`（`:51`）四路分发：
- `Granted`（`:52-54`）→ `Box(modifier.fillMaxSize()){ content() }`。
- `Denied`（`:55-61`）→ `PermissionDeniedPanel`，方法引用直连 `permission::openAppSettings` / `permission::requestPermission`。
- `Unavailable`（`:62-66`）→ `PermissionUnavailablePanel`。
- `Unknown`（`:67-72`）→ `PermissionRationalePanel`。
注释 `:29` 说明当前**仅键于 `CameraPermissionStatus`**，其它权限类型需映射到相同状态。
调用点：唯一——`feature/scan/ScanScreen.kt:130`（扫一扫）。

#### `PermissionDeniedPanel`（`:76-122`）
签名 `(title, message, onOpenSettings, onRetryRequest, modifier = Modifier)`。`Column(padding 16.dp)` 内：红色标题（`DemoColors.Danger`，SemiBold 16sp，`:85-90`）+ 灰文案（`TextSecondary` 13sp，`:92-96`）+ 主按钮「去系统设置」（`:98-110`，`fillMaxWidth().height(48.dp)`，`RoundedCornerShape(8.dp)`，container=`Primary`/content=`OnPrimary`）+ 描边按钮「重新申请权限」（`:112-120`）。
#### `PermissionRationalePanel`（`:124-152`）
`(rationale, requestLabel, onRequest, modifier)`：灰说明 14sp + 一个主按钮（label=`requestLabel`，样式同上）。
#### `PermissionUnavailablePanel`（`:154-174`）
`(title, message, modifier)`：仅红色标题 + 灰文案，**无按钮**（不可用态无可操作动作）。

> `Unavailable` 是 OHOS 的真实落点：`PlatformCameraScan.ohos.kt:17` `status` 恒为 `CameraPermissionStatus.Unavailable`、`refresh/requestPermission` 均 `= Unit`（`:19/21`）。所以扫码页在 OHOS 上永远走 `PermissionUnavailablePanel`。

### 3.5 core.ui —— 网络图

#### `secureNetworkImageUrl`（`PlatformNetworkImage.kt:9-16`，普通函数）
```kotlin
fun secureNetworkImageUrl(url: String?): String?
```
逻辑：null/空白原样返回（`:10`）；已是 `https://` 原样（`:12`）；`http://` 替换前缀成 `https://`（`:13`，`substring(7)`）；其它 scheme（如 `file://`）原样（`:14`）。注释 `:8` 目标：让 iOS ATS 能加载明文图链。
调用点：仅 imageCoil actual `PlatformNetworkImage.coil.kt:40`。虽为 public，但当前无 feature 直接调。

#### `expect fun PlatformNetworkImage`（`PlatformNetworkImage.kt:22-29`，`@Composable`）
```kotlin
@Composable
expect fun PlatformNetworkImage(
    url: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    placeholder: DrawableResource,
    contentDescription: String? = null,
)
```
参数：`url` 可空（空则各端退占位）；`contentScale` 默认 `Crop`；`placeholder` 是必填 `DrawableResource`（由调用方从 core.design 的 `Res.drawable.*` 提供，见 §2.1）；`contentDescription` 无障碍文案，默认 null。注释 `:18-21`：android/ios/ohosArm64 走 Coil，ohosX64 走本地占位。
调用点：`NativeJetpackTabs.kt:437/737/1070/1150`、`FriendScreen.kt:405`、`CommunityRouteHost.kt:619/673`、`HomeRouteHost.kt:435/465/727`（示例传参 `contentScale = ContentScale.Fit`、`placeholder = Res.drawable.community_post_a`，`:622-623`）。

#### `actual`（imageCoilMain，`PlatformNetworkImage.coil.kt:19-47`）
真实行为：先 `placeholderPainter = painterResource(placeholder)`（`:27`）；若 `url` 空白 → `Box(background Color(0xFFF0F0F0)){ Image(placeholder, fillMaxSize, contentScale) }` 并 return（`:28-38`）；否则 `coil3.compose.AsyncImage(model = secureNetworkImageUrl(url), placeholder = placeholderPainter, error = placeholderPainter, …)`（`:39-46`）——加载中与失败**都复用同一占位图**。注释 `:17` 强调 feature 不要直接 import coil3，只调 `PlatformNetworkImage`。

#### `actual`（ohosX64Main，`PlatformNetworkImage.ohosX64.kt:15-33`）
真实行为：**无条件**渲染 `Box(background 0xFFF0F0F0){ Image(painterResource(placeholder), fillMaxSize, contentScale) }`（`:23-30`），`url` 参数被读取但不使用（`:31-32` `@Suppress("UNUSED_EXPRESSION") url`）——纯本地占位，不发任何网络请求。

---

## 4. 设计令牌体系（SoT + 测试校验意图）

### 4.1 分组结构（`DesignTokens.kt`）
- `data class Argb(val value: Long)`（`:14-19`）：packed 颜色，附 `alpha/red/green/blue` 位运算 getter（`shr 24/16/8 and 0xFF`）。约定：绝大多数值是 6 位 RGB 前缀 `0xFF`（alpha=255），唯一带 alpha 的是 `TabBarBackground = 0xF2FFFFFF`（`:48`，注释「Flutter tabBarBackground #F2FFFFFF」）。
- `object Color`（`:21-49`）：27 个令牌，语义分组为 墨色族（Primary/Ink/Body/Mute）、画布族（Canvas/CanvasSoft/CanvasSoft2）、描边族（Hairline/HairlineStrong）、链接族（Link/LinkDeep/LinkBgSoft）、状态族（Success=Link/Error/ErrorSoft/ErrorDeep/Warning/WarningSoft/WarningDeep）、强调族（Violet/Cyan/HighlightPink）、选中族（SelectionBg/SelectionFg）、底栏（TabBarBackground）。
- `object Spacing`（`:51-57`）：`const val` Int——Xs=4/Sm=8/Md=16/Lg=24/Xl=32（**无单位**，dp 由调用方施加）。
- `object Typography`（`:59-65`）：const Int sp——DisplayMdSp=24/BodyMdSp=16/BodySmSp=14/CaptionSp=12/TabLabelSp=10。
- `object Radius`（`:67-71`）：const Int——Sm=8/Md=12/Lg=16。

三层消费关系：**DesignTokens（SoT，跨栈含原生）→ DemoColors（Compose 适配）→ DesignSystem（别名面）→ MineTheme（feature 桥接）**。DesignTokens 刻意不含任何 Compose/Android import（`DesignTokens.kt:1-2` 仅 package 声明），原生壳可直接读或代码生成绑定（注释 `:6-10`）。

### 4.2 与 Flutter SoT 对应
类注释 `DesignTokens.kt:6-8`：值对齐 Flutter 视觉源 `my_ai_project` 的 `DESIGN.md / VercelTokens.light`。DemoColors 注释 `:31` 标注强调色族来自「Flutter VercelTokens」；`:18` 标注 CanvasSoft = Flutter `canvasSoft #FAFAFA`。ImmersiveInsets 注释 `:28` 标注 49dp 来自「Flutter NavigationBarTheme height」。

### 4.3 `DesignTokensSoTTest` 逐条断言意图（`commonTest/DesignTokensSoTTest.kt`）
测试类注释 `:6-8`：「把 Shared Design Tokens 锁死到 Flutter SoT 字面量」。两个 `@Test`：

**`light_palette_matches_flutter_vercel_tokens()`（`:12-28`）** —— 15 条颜色相等断言（断言的是 `DesignTokens.Color`，不是 DemoColors）：

| 行号 | 断言令牌 | 期望字面量 | 校验意图 |
|------|----------|-----------|---------|
| `:13` | Primary | `0xFF171717` | 主色=墨黑（注意≠DemoColors.Primary） |
| `:14` | OnPrimary | `0xFFFFFFFF` | 反白 |
| `:15` | Ink | `0xFF171717` | 正文最重墨 |
| `:16` | Body | `0xFF4D4D4D` | 正文灰 |
| `:17` | Mute | `0xFF888888` | 次要灰 |
| `:18` | Hairline | `0xFFEBEBEB` | 分割线 |
| `:19` | HairlineStrong | `0xFFA1A1A1` | 强描边 |
| `:20` | Canvas | `0xFFFFFFFF` | 画布白 |
| `:21` | CanvasSoft | `0xFFFAFAFA` | 软底 |
| `:22` | CanvasSoft2 | `0xFFF5F5F5` | 页面底 |
| `:23` | Link | `0xFF0070F3` | 品牌蓝 |
| `:24` | LinkDeep | `0xFF0761D1` | 蓝深一档 |
| `:25` | Error | `0xFFEE0000` | 危险红 |
| `:26` | Warning | `0xFFF5A623` | 警告橙 |
| `:27` | TabBarBackground | `0xF2FFFFFF` | 带 alpha 底栏 |

**`spacing_scale_is_stable()`（`:31-36`）** —— 4 条间距断言：`Xs=4`(`:32`)、`Sm=8`(`:33`)、`Md=16`(`:34`)、`Lg=24`(`:35`)。

测试**未覆盖**（改这些不会被 CI 挡下）：`ErrorSoft/ErrorDeep/WarningSoft/WarningDeep/Success/SelectionBg/SelectionFg/Violet/Cyan/HighlightPink` 等颜色；`Spacing.Xl`；**全部 `Typography` 与 `Radius`**。即 SoT 校验只锁「核心调色板 + 前 4 档间距」。

---

## 5. 沉浸式 / inset 规则

### 5.1 `ImmersiveInsets` 各成员（`ImmersiveInsets.kt`）
| 成员 | 行号 | 含义 | 使用/状态 |
|------|------|------|-----------|
| `MainBottomBarHeight: Dp = 49.dp` | `:29` | 主底栏固定高度常量；注释 `:28`「Flutter NavigationBarTheme height — 49dp（曾 56dp Demo）」 | `MainBottomBar.kt:37` import、`:91` `.height(MainBottomBarHeight)`（**唯一消费点**） |
| `SafeDrawing: WindowInsets`（`@Composable get`） | `:32-34` | `statusBars union navigationBars`，给无底栏的全屏页（如登录）用 | **仓库内暂无调用点**，预留 |
| `Modifier.shellContentInsets(bottomBarVisible, bottomBarPadding)` | `:41-48` | Shell 内容区底部 inset 策略 | `AppShell.kt:42` import、`:360-363` 调用 |
| `Modifier.immersiveStatusBarBackground(color)` | `:51-52` | 全屏自定义头：`fillMaxWidth().background(color)`，把色铺到状态栏后 | **仓库内暂无调用点**，预留 |

### 5.2 `shellContentInsets` 两分支算法（`:44-48`）
```
if (bottomBarVisible)  padding(bottom = bottomBarPadding)     // Tab 根：为 MainBottomBar 让位
else                   navigationBarsPadding()                 // 二级栈页：只清系统导航/手势条
```
在 `AppShell.kt` 的实证：`bottomBarVisible` 传的是 `showBottomBar`（`:361`），`bottomBarPadding` 传的是 `Scaffold` 给的 `paddingValues.calculateBottomPadding()`（`:362`）——即由外层 Scaffold 已算好的底部留白（含 49dp 底栏 + nav inset）。`showBottomBar` 本身 = `authOverlay == None && !shellOverlayActive && tabChrome.bottomBarVisible`（`:334-336`），把 §3.3 的 `ReportMainTabRoot` 结果纳入判定。

### 5.3 为什么禁用裸 TopAppBar
AGENTS.md 明确「禁止直接使用裸 `TopAppBar`/`CenterAlignedTopAppBar`（状态栏会白条）」。代码依据在 `ImmersiveTopAppBar.kt:38-53`：裸 M3 顶栏在沉浸式（内容延伸到状态栏下）时，状态栏区不会被 toolbar 底色覆盖 → 出现白条。解法=外层 Box 画 `containerColor` 到状态栏背后 + 内层 bar `.statusBarsPadding()` 下沉内容 + `windowInsets(0,0,0,0)` 关掉 M3 自带 inset。`MineTopBar` 正是这套封装的二级页入口。

### 5.4 平台入口前置条件（AGENTS.md，非本分册源码，仅登记规则）
Android `MainActivity` 需 `installSplashScreen() → enableEdgeToEdge() → setContent`，theme 状态栏/导航栏透明；iOS `ContentView.swift` `ComposeView().ignoresSafeArea(.all)`；Shell `DemoApp` Scaffold `containerColor=Transparent`、`contentWindowInsets=WindowInsets(0)`（`AppShell.kt:346-347` 与之一致：`containerColor = Color.Transparent`、`contentWindowInsets = WindowInsets(0,0,0,0)`）。禁止在 tab 内容外包 `statusBarsPadding()`、禁止二级页根再 `navigationBarsPadding()`（Shell 已处理）、禁止第二个 Scaffold 塞底栏。

---

## 6. 图片加载：PlatformNetworkImage 三端差异

| 维度 | android / ios / ohosArm64（imageCoilMain） | ohosX64 |
|------|-------------------------------------------|---------|
| 依赖 | `libs.coil.compose` + `coil.network.okhttp`（android `build.gradle.kts:30`）/ `coil.network.ktor3`（ios `:35`、ohosArm64 `:39`） | **无 Coil**——CPF Coil 无 ohosX64 klib（`build.gradle.kts:3-4` 注释、AGENTS.md） |
| 网络请求 | `AsyncImage` 真实加载远端图（`coil.kt:39-46`） | 不发请求，恒显示本地 `placeholder`（`PlatformNetworkImage.ohosX64.kt:23-30`） |
| url 处理 | 先过 `secureNetworkImageUrl`（http→https） | url 被读但忽略（`PlatformNetworkImage.ohosX64.kt:31-32`） |
| url 空白 | Box(灰底 0xFFF0F0F0) + 占位图（`coil.kt:28-38`） | 与正常态同：Box(灰底) + 占位图 |
| 失败态 | `error = placeholderPainter`（复用占位，`coil.kt:45`） | 无失败概念 |

为什么 ohosX64 是占位：官方未发布 Coil 的 ohosX64 klib（AGENTS.md「OHOS images」段 + `core/ui/build.gradle.kts:3-4`）。imageCoilMain 只被 `dependsOn` 到 android/ios/ohosArm64（`:28-40`），ohosX64Main 不在其中，故编译器要求 ohosX64Main 自己提供 `actual`——这就是 `PlatformNetworkImage.ohosX64.kt`。ohosArm64（真机/模拟器主流架构）已能通过 CPF Coil + CIO 正常显示网络图。

> 相同降级思路也用在 realtime WS：`core/network/src/ohosX64Main/…/RealtimeWsTransportFactory.ohosX64.kt:8` 注释直接「参照 `PlatformNetworkImage.ohosX64.kt`」，佐证「ohosX64 = 占位/降级」是全工程一致的约定。

---

## 7. 真实状态与坑

### 7.1 降级 / 未实现端
- **ohosX64 网络图 = 纯占位**：不加载任何远端图，只显 placeholder（`PlatformNetworkImage.ohosX64.kt:15-33`）。业务在 ohosX64 上看不到在线图。
- **OHOS 相机/扫码 = Unavailable**：`PermissionGate` 在 OHOS 恒落 `PermissionUnavailablePanel`（`PlatformCameraScan.ohos.kt:17-21` status 写死 Unavailable、refresh/request 空实现）。`ScanScreen.kt:130` 是唯一 `PermissionGate` 调用点，即 OHOS 扫一扫不可用。
- **PermissionGate 只支持相机权限语义**：签名硬绑 `CameraPermissionController`（`PermissionGate.kt:32`），其它权限需自行映射到同一 `CameraPermissionStatus` 枚举（注释 `:29`）。

### 7.2 命名陷阱（重点）
- **`DemoColors.Primary` ≠ `DesignTokens.Color.Primary`**：前者=`Color.Link`(#0070F3 蓝，`DemoColors.kt:13`)，后者=#171717 墨黑（`DesignTokens.kt:22`，且 `DesignTokensSoTTest.kt:13` 锁的就是墨黑）。写色时务必分清用的是适配面还是 SoT 面，否则「primary」在 Compose 里其实是品牌蓝。
- `DemoColors.Surface`/`DesignSystem.Surface` 不存在独立值：`DesignSystem.Surface = DemoColors.Background`（`DesignSystem.kt:15`），且 DemoColors 里没有 `Surface` 字段——别臆造 `DemoColors.Surface`。
- DemoColors 未暴露的 DesignTokens 令牌：`Success/ErrorSoft/ErrorDeep/WarningSoft/WarningDeep/SelectionBg/SelectionFg` 只能用 `DesignTokens.Color.*`（经 `argb()` 无对应 Compose 别名），需自建 `Color(DesignTokens.Color.X.value)`。

### 7.3 预留 / 无调用点的 API
- `ImmersiveTopAppBar`（左对齐版，`ImmersiveTopAppBar.kt:58-86`）：仓库内**无调用点**，仅 AGENTS.md 列为可用。
- `ImmersiveInsets.SafeDrawing`（`:32-34`）与 `immersiveStatusBarBackground`（`:51-52`）：**无调用点**，预留。
- `secureNetworkImageUrl`：public 但当前仅被 coil actual 使用（`:40`）。
- 改动这些不影响任何现有页面，但也**没有测试覆盖**（commonTest 只有 `DesignTokensSoTTest`），删除/重构无 CI 兜底。

### 7.4 资源与依赖的硬约束
- **资源唯一归属 core.design**：`composeApp`、`core/ui` 均无 composeResources（`ls` 证实）；feature 跨模块 import `my_kmp_project.core.design.generated.resources.*`。新增图片/字体**只能放 `core/design/src/commonMain/composeResources/`**（AGENTS.md 同述）。
- **依赖链脆弱点**：feature 拿 `DemoColors/MineTopBar` 全靠 `core.ui → core.platform → core.design` 的连续 `api`；core.design 是叶子无下游、但也**没有任何 feature 直连它**——把 core.platform 的 `api(projects.core.design)` 改成 implementation 会全线编译失败。
- **androidOnly CI 陷阱**：ios/ohos 的 `dependsOn(imageCoilMain)` 用 `matching {}` 惰性写法（`core/ui/build.gradle.kts:32-40`），若误用 `getByName("iosMain")` 会在 `-PandroidOnly=true` 流水线下抛异常（注释 `:32`）。

### 7.5 禁止事项（对齐 AGENTS.md）
- 二级页一律用 `MineTopBar`（或 `ImmersiveCenterTopAppBar`），**不得**用裸 `TopAppBar`/`CenterAlignedTopAppBar`（白条）。
- 不要在 `DemoApp` tab 内容外包 `statusBarsPadding()`；不要在二级页根重复 `navigationBarsPadding()`（`shellContentInsets` 已按分支处理）。
- feature 不得直接 `import coil3`——网络图只调 `PlatformNetworkImage`（`coil.kt:17`）。
- 图标一律用 `MineIcons` 自绘 ImageVector + 调用方 `tint`，**不得**引 `material-icons-extended`（OHOS 不可用，`MineIcons.kt:13`）。

### 7.6 未验证疑点（诚实登记）
- Drawable accessor 拆成 Drawable0/1 的具体阈值（100/文件）是从生成产物反推，未查 Compose 资源生成器文档确证；对运行无影响。
- `Spacing/Typography/Radius` 是裸 Int，本分册未在 feature 中逐一核对「×1.dp/.sp」的施加是否处处一致（属各 feature 内部，超出本模块范围）。
- SoT 与 Flutter `my_ai_project` 的逐项映射仅依据源码注释与测试字面量，未访问外部 SoT 仓库交叉核对。

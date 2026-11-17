## 16B · feature:home 的屏与分区

定位：`feature/home` 是 17 个业务 feature 模块里最重的一个，`commonMain` 单目录 24 个文件。本分册（B 部分）覆盖**首页本体 + 首页分区组件 + 路由宿主 + 首页系二级屏**：`HomeScreen` / `HomeSections` / `HomeRouteHost` / `HomeDetailScreens` / `HomeSearchScreen` / `AllServicesScreen` / `DailyCheckInDialog` / `LearningReportScreen` / `LedgerScreens` / `StrategyScreen` / `HomeServiceAssets` / `HomeWebHandlers`。A 部分负责 `HomeIcons`、`HomeMockData`、各 Store 与 api 契约，C 部分负责 `aftersales`、`newcar`、Todo 等旁支（如与本文重叠，以各自 file:line 为准）。

包根：`com.example.my_kmp_project.feature.home`（源码目录名字面带点：`feature/home/src/commonMain/kotlin/com.example.my_kmp_project/feature/home/`，下文统一简写为 `feature/home/.../home/`）。

### 文件清单

| 路径 | 作用 | 行数 |
|------|------|------|
| `feature/home/.../home/HomeScreen.kt` | 首页 Tab 根（AppShell/iOS/OHOS 共用）+ 本地 destination 栈 + 自动签到弹窗编排 | 287 |
| `feature/home/.../home/HomeSections.kt` | 首页各分区可复用 Composable（问候/搜索框/Banner/金刚区/服务宫格/联系人/资讯/门店指标卡/Hub 入口卡） | 989 |
| `feature/home/.../home/HomeRouteHost.kt` | Home 域**二级路由宿主**：`route: String` → 本模块各屏的大 `when`；含 30+ 私有屏实现与 Mock 数据 | 3574 |
| `feature/home/.../home/HomeDetailScreens.kt` | 首页附属详情/表单屏 | 393 |
| `feature/home/.../home/HomeSearchScreen.kt` | `/home/search` 搜索页（热词/历史/结果） | 578 |
| `feature/home/.../home/AllServicesScreen.kt` | `/home/all_services` 全部服务页 | 325 |
| `feature/home/.../home/DailyCheckInDialog.kt` | 每日签到弹窗（纯展示，无网络） | 194 |
| `feature/home/.../home/LearningReportScreen.kt` | `/home/learning_report` 学习报告页 | 345 |
| `feature/home/.../home/LedgerScreens.kt` | `/home/ledger` 台账 + 明细 | 224 |
| `feature/home/.../home/StrategyScreen.kt` | `/home/strategy` 投资策略页 | 392 |
| `feature/home/.../home/HomeServiceAssets.kt` | 服务宫格图标资源与 label→icon 映射 | 125 |
| `feature/home/.../home/HomeWebHandlers.kt` | H5 JSBridge 方法注册（`WebBridgeRegistry`） | 45 |

### 逐文件详解

#### HomeScreen.kt

文件：`feature/home/src/commonMain/kotlin/com.example.my_kmp_project/feature/home/HomeScreen.kt`（287 行）

| 符号 | 行号 | 签名 | 真实行为 |
|------|------|------|----------|
| `HomeScreen` | 37-105 | `@Composable fun HomeScreen(loggedIn: Boolean = false, displayName: String? = null)` | 首页 Tab 根；**无 modifier 参数**。内部 `var destination by remember { mutableStateOf<String?>(null) }`（L42）+ `LocalAppNavigator.current`（L43），自建一个「单值栈」：destination==null 渲染 `HomeRootContent`，非 null 渲染二级屏。 |
| 局部函数 `go(raw: String)` | 45-65 | 内部 local fun，捕获 `navigator`/`destination` | 用 `ProductRouteDispatch.resolve(raw)`（`core/router/src/commonMain/kotlin/com.example.my_kmp_project/core/router/ProductRouteDispatch.kt:62`）解析中文 label/路径。按 `target.host` 分派：`Toast`→`showPlatformToast`（L48）；`Web`→`navigate(AppRoute.InAppWeb(OfflineWebFixtureUrl))`（L49）；`Scan`→`navigate(AppRoute.Scan)`（L50）；`Content`→ 再按 path 命中 `/friend`→`AppRoute.Friend`、`/live`、`/live/room`→`AppRoute.Live`、`/classroom/my_class`、`/classroom`→`AppRoute.Classroom`、`/media/entry`→`AppRoute.Media`，其余落 `destination`（L51-57）；`AllServices`→`destination="services"`（L58）；`Home/Mine/Community`→`destination=target.path`（L59-60）；`Tab`→Toast「请切换底部 Tab」（L61）；`Auth`→Toast「请先登录」（L62）；`Unknown`→`destination=raw` 兜底交给 `HomeRouteHost`（L63）。**`go` 不发任何网络请求**，只做本地导航。 |
| destination 渲染 | 67-104 | — | `"services"` 特判渲染 `AllServicesScreen(onBack, onOpen)`（L73-79，`onOpen` 先清栈再 `go(label)`）。其它 dest 用 `ProductRouteDispatch.hostForPath(dest)`（`ProductRouteDispatch.kt:112`）四向分派：`Home`→`HomeRouteHost`（L82）、`Content`→`CrossFeatureScreens.ContentRouteScreen`（L87）、`Mine`→`CrossFeatureScreens.MineRouteScreen`（L92）、**else 兜底也走 `HomeRouteHost`**（L97）。所有二级屏统一注入 `onBack = { destination = null }`（单层返回，不是真栈）。 |
| `HomeRootContent` | 107-271 | `@Composable @OptIn(ExperimentalTime::class) private fun HomeRootContent(loggedIn: Boolean, displayName: String?, onNavigate: (String) -> Unit)` | 首页真身。**首行 `ReportMainTabRoot(isRoot = true)`（L114）** —— 声明自己是 Tab 根，让 Shell 底栏按 tab-root 模式加 padding。 |
| `MetricTabs` | 274 | `private val MetricTabs = listOf("今日", "昨日", "近30天")` | 门店指标卡三个 tab 标题，注释标为 Flutter `HomeController.metricTabs`。 |
| `flutterStyleGreeting` | 276-287 | `@OptIn(ExperimentalTime::class) private fun flutterStyleGreeting(displayName: String?): String` | 非 Composable。固定按 UTC+8 换算小时（L279，`(epochMillis/3_600_000 + 8) % 24`），<12「早上好」/<18「下午好」/否则「晚上好」；名字空则「访客」（L285）。 |

`HomeRootContent` 状态来源（全部本地 `remember`，L115-122）：`metricTab`（Int）、`showTodos`、`showSwitchStore`、`storeName`（初值 `HomeMockData.storeName`）、`showCheckIn`、`greeting = remember(displayName){…}`。**L122 单独一行读 `HomePointsStore.version`** —— 靠读这个 `mutableStateOf` 计数器触发 recomposition（`HomePointsStore` 是 object 单例，见 A 分册）。

副作用（`HomeScreen.kt`）：
- L124-127 `LaunchedEffect(Unit)`：延迟 50ms 后置 `showTodos = true`（Todo 卡片条延迟入场）。
- L128-144 `LaunchedEffect(loggedIn, HomePointsStore.checkedInToday, HomePointsStore.dialogAckDate)`：用 `Clock.System.now().toString().take(10)` 当「今天」（L131），调 `HomePointsStore.shouldShowDialog(...)`（L135-140）判定，命中再 `delay(400)` → `showCheckIn = true`（L142-143）。**无请求，纯本地判定**。
- L146-156 `showSwitchStore && loggedIn` 时弹 `CrossFeatureScreens.SwitchStoreDialog(selectedId = "1", …)`；`onPicked` 只改本地 `storeName` + Toast「已切换到 $name」（L150-154）——**未写 KV**，切店结果不持久。
- L158-177 `showCheckIn && loggedIn` 时弹 `DailyCheckInDialog(todayReward, streak, onCheckIn, onDismiss)`；`onCheckIn` 调 `HomePointsStore.checkIn()` + `markDialogAcked(today)` + Toast「签到成功，+N 积分」（L164-171）；`onDismiss` 只 `markDialogAcked`（L172-175）。

布局与 inset：L179-192 `Column(fillMaxSize + background(DemoColors.PageBg))` → `LazyColumn(modifier = fillMaxSize.statusBarsPadding(), contentPadding = PaddingValues(top=16, bottom=24))`。注释（L185-187）明确：安全区交给 `statusBarsPadding()`，16 交给 `contentPadding`，首个 section 自带 24 凑成 Flutter 的 `safeTop+40`。**底部不消费 `navigationBarsPadding`**（Shell 按 tab-root 处理，见合规节）。

LazyColumn 分区顺序（L193-267）：`HomeGreetingSection`(193) → `HomeSearchBar`(195-200，`onSearch={onNavigate(HomeRoutes.Search)}`、`onScan={onNavigate("扫一扫")}`) → `HomeBannerSection`(201) → `HomeFeatureGrid`(203-207，`onFeature={label->onNavigate(label)}`) → `HomeTodoCardStrip`(209-220，`onOpen` 用 `card.actionRoute.ifBlank { HomeRoutes.fromLabel(card.title) ?: card.title }`，L212-216) → `HomeStoreMetricsCard`(222-238，`metrics` 按 `metricTab` 三选一读 `HomeMockData.metricsToday/Yesterday/Month`，L226-230；`onStoreTap`：登录则开切店弹窗否则 `onNavigate("请先登录")` L233-236；`onOpenLedger={onNavigate(HomeRoutes.Ledger)}` L237) → `HomeHubEntryCard("投资策略")`(240-247，`onNavigate(HomeRoutes.Strategy)`) → `HomeServiceGrid`(248-257，**只有 label 为「更多」/「全部」才跳** `HomeRoutes.AllServices`，其余点击是 no-op，L252-254) → `HomeContactList`(258，`onContact = {}` 空实现) → `HomeNewsList`(259，`onOpenAll = {}` 空实现) → `HomeHubEntryCard("学习报告")`(261-267，`onNavigate(HomeRoutes.LearningReport)`)。L268 注释：Flutter HomePage 到学习报告结束，无「更多工具」行。

外部调用点：`composeApp/src/commonMain/kotlin/com.example.my_kmp_project/app/AppShell.kt:390`（`MainTab.Home -> HomeScreen(...)`，import L66）；Android native shell 走自己的 `JetpackHomeRoot`（`composeApp/src/androidMain/.../nativeshell/NativeJetpackHome.kt`，复用同一批 `HomeSections` 组件，import L24-36）。

#### HomeWebHandlers.kt

文件：`feature/home/src/commonMain/kotlin/com.example.my_kmp_project/feature/home/HomeWebHandlers.kt`（45 行）

`object HomeWebHandlers`（L11）——非 Composable，H5 桥注册器；`@Volatile private var registered = false`（L12-13）保证幂等；`var onRefreshDashboard: (() -> Unit)? = null`（L16）是可被 Home 替换的公开钩子，默认 null。

`fun register()`（L18-44）向 `core.platform.WebBridgeRegistry.register(WebBridgeMethods.*, handler)` 注册 **4 个方法**：

| 行号 | 方法 | handler 行为 | 返回 |
|------|------|--------------|------|
| 21-24 | `WebBridgeMethods.RefreshDashboard` | 忽略入参，`onRefreshDashboard?.invoke()`（未接线时是 no-op） | `{"ok":true}` |
| 25-30 | `WebBridgeMethods.ShowToast` | payload 非空白则 `core.platform.showPlatformToast(payload)` | `{"ok":true}` |
| 31-33 | `WebBridgeMethods.GetEnvironment` | **硬编码字符串** | `{"platform":"android","app":"my_kmp_project"}`（iOS/OHOS 上也返回 "android"，未做 expect/actual —— 已知不精确点） |
| 34-43 | `WebBridgeMethods.GetUserInfo` | 读 `core.account.AccountFacade.current()`；未登录 `{"loggedIn":false}`，登录则手工拼 `userId`/`displayName`（L39-41，字符串拼接，**未做 JSON 转义**） | `{"loggedIn":true,"userId":…,"displayName":…}` |

走的 core 能力：`core.platform.WebBridgeRegistry` + `WebBridgeMethods`（注册表）、`core.platform.showPlatformToast`（平台 Toast）、`core.account.AccountFacade`（会话）。**它不处理 URL**，只处理方法名；URL 侧由 `component.webview.OfflineWebFixtureUrl` + `AppRoute.InAppWeb` 决定（见 `HomeScreen.kt:49`）。

唯一调用点：`composeApp/src/androidMain/kotlin/com.example.my_kmp_project/nativeshell/NativeAndroidMain.kt:90`（`HomeWebHandlers.register()`，import L67）——**只有 Android native shell 注册**，与文件注释 L9 一致。commonMain/iOS/OHOS 调用：未找到。命令备查：

```
grep -rn "HomeWebHandlers" --include="*.kt" .
```

#### HomeServiceAssets.kt

文件：`feature/home/src/commonMain/kotlin/com.example.my_kmp_project/feature/home/HomeServiceAssets.kt`（125 行）

`object HomeServiceAssets`（L44）——「Flutter `module_home` all_services PNG → Compose drawable」映射器（注释 L43）。资源全部来自 `:core:design` 的生成包 `my_kmp_project.core.design.generated.resources.Res`（L10-39 import，45 条），**不是** 本模块自己的 composeResources，符合 AGENTS.md「`:core:design` 持有全部 drawable」。

| 函数 | 行号 | 签名 | 行为 |
|------|------|------|------|
| `fromFlutterFile` | 45-69 | `fun fromFlutterFile(assetName: String): DrawableResource?` | 先 `removeSuffix(".png")/.webp`（L46），再对 19 个 Flutter 资产名做 `when`（L48-66，`home_all_services_*`），**未命中返回 null**（L67）——调用方需自行降级。 |
| `featureForLabel` | 75-87 | `fun featureForLabel(label: String): DrawableResource` | 首页金刚区图标按**中文 label** 映射（L76-85，一组 label 共用一张图：如 `"H5 调试"`+`"销售顾问"`→`home_feature_sales`，`"新车跟进"`+`"数据分析"`→`home_feature_data`）；else 兜底 `home_all_services_all_functions`（L86）。返回非空。 |
| `featureAt` | 89-96 | `@Deprecated("Use featureForLabel", ReplaceWith("featureForLabel(label)")) fun featureAt(index: Int): DrawableResource` | 兼容旧索引式调用：内部固定 10 个 label 列表（L91-94）后 `featureForLabel(labels[index % labels.size])`（L95）。**注意 `index` 为负数时 `%` 在 Kotlin 里返回负值 → `labels[负]` 会 IndexOutOfBounds**，未见防护。 |
| `serviceCycle` | 98-107 | `private val` | 8 张 `home_all_services_*` 循环表。 |
| `serviceAt` | 109 | `fun serviceAt(index: Int): DrawableResource` | `serviceCycle[index % size]`，同样未防护负 index。 |

| Composable | 行号 | 签名 | 行为 |
|------------|------|------|------|
| `HomeAssetIcon` | 112-125 | `@Composable fun HomeAssetIcon(resource: DrawableResource, size: Dp = 44.dp, contentDescription: String? = null, contentScale: ContentScale = ContentScale.Crop)` | **无 `modifier` 参数**（Modifier 内部写死 `Modifier.size(size)`，L122）。就一个 `Image(painter = painterResource(resource), …)`（L119-124）；无状态、无副作用、不接 store。默认 `contentDescription = null`（对读屏不可见）。 |

#### DailyCheckInDialog.kt

文件：`feature/home/src/commonMain/kotlin/com.example.my_kmp_project/feature/home/DailyCheckInDialog.kt`（194 行）

| Composable | 行号 | 签名 | 真实行为 |
|------------|------|------|----------|
| `DailyCheckInDialog` | 42-194 | `@Composable fun DailyCheckInDialog(todayReward: Int = 10, streak: Int = 3, onCheckIn: () -> Unit, onDismiss: () -> Unit)` | **纯展示 + 两个回调，零业务**：不读 store、不发请求、不写 KV。无 `modifier` 参数。 |

内部状态与绘制细节：
- L49 `var busy by remember { mutableStateOf(false) }` —— 唯一状态，只用于防重复点击与转圈。
- L50-54 取色：`primaryBlue = DemoColors.Accent`、`coinGold = Color(0xFFF5A623)`、`heroBrush = Brush.linearGradient(0xFF1A8CFF → Accent → 0xFF0050C8)`（后两个是一次性 hex，**未走 `DemoColors` token**）。
- L56-59 `Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false))` —— 用原生 `Dialog` 而不是 `AlertDialog`，宽度自管。
- 结构：外层 `Column(fillMaxWidth.padding(horizontal = 36.dp))`（L60-65）→ 卡片 `Column`（L66-72，`shadow(24.dp, RoundedCornerShape(16.dp)) + clip + background(White)`）。
  - Hero（L74-106）：64dp 圆形半透明容器内 `Text("📅")`（L81-90，**用 emoji 代替图标资源**）；`+$todayReward` 40sp ExtraBold 金色（L92-99）；副文案「今日可领积分」（L101-105）。
  - 白身（L108-178）：标题「每日签到」（L114-119）→ 说明「签到攒积分，可在签到页兑换好物」（L121-127）→ `if (streak > 0)` 才显示「已连续签到 N 天」金色胶囊（L128-140）→ CTA。
- **CTA 行为（L142-168）**：44dp 高、22dp 圆角蓝底 `Box`，`clickable(enabled = !busy)` 里先 `busy = true` 再 `onCheckIn()`（L148-151）。**注意：`busy` 永远不会复位** —— 复位依赖父级把整个 Dialog 从组合中摘掉；调用方 `HomeScreen.kt:164-171` 确实在 `onCheckIn` 里置 `showCheckIn = false`，所以能卸载。若父级不卸载，按钮会永久转圈。busy 时渲染 `CircularProgressIndicator(18.dp, 白, 2.dp)`（L155-159），否则文案「立即签到 · +N积分」（L161-166）。
- 「稍后再说」（L170-177）：`clickable(enabled = !busy, onClick = onDismiss)`；卡片下方独立 36dp 半透明圆「✕」（L182-191）同样只在 `!busy` 时可点。系统返回/点遮罩走 `onDismissRequest = onDismiss`（L57）。

调用方：`HomeScreen.kt:161`（common 首页）、`composeApp/src/androidMain/kotlin/com.example.my_kmp_project/nativeshell/NativeJetpackHome.kt:24`（Android native 首页 import 后复用）。

#### LedgerScreens.kt

文件：`feature/home/src/commonMain/kotlin/com.example.my_kmp_project/feature/home/LedgerScreens.kt`（224 行）

| 符号 | 行号 | 签名 | 真实行为 / 状态 / 副作用 |
|------|------|------|--------------------------|
| `LedgerTransaction` | 34-41 | `data class LedgerTransaction(id: Int, type: String, category: String, amount: Double, date: String, note: String? = null)` | 纯数据，注释对应 Flutter `TransactionModel`。**没有 `isValid`**：`isValid: Boolean` 属于 Pay（见 PayRecordsScreen），本文件只有 `amount: Double`。 |
| `LedgerListScreen` | 47-79 | `@Composable fun LedgerListScreen(items: List<LedgerTransaction>, onBack: () -> Unit, onItem: (LedgerTransaction) -> Unit)` | **数据全靠参数注入**，内部零状态、零 store、零请求。L53 `ReportMainTabRoot(isRoot = false)`（二级页）。L59-63 用 `core.design.MineTopBar(title = "收支", onBack, containerColor = Color.White)` —— 合规的二级顶栏，非裸 TopAppBar。L64-67 `items.isEmpty()` 时居中「暂无收支记录」；否则 `LazyColumn(contentPadding = top12/bottom24)` + `items(items, key = { it.id })`（L69-76），行点击 → `onItem(item)`。根 `Column` 底色是硬编码 `Color(0xFFF5F6F8)`（L57），**未用 `DemoColors.PageBg`**。 |
| `LedgerDetailScreen` | 82-128 | `@Composable fun LedgerDetailScreen(item: LedgerTransaction?, onBack: () -> Unit)` | 可空入参：null → 居中「记录不存在」（L98-101）。非 null 渲染 类别 / `formatLedgerAmount` 大字 / 类型 / 日期 /（非空时）备注（L103-125）。L87 `ReportMainTabRoot(isRoot = false)`，L93-97 `MineTopBar(title = "收支详情")`。**无任何点击副作用**。 |
| `TransactionListItem` | 130-185 | `@Composable private fun TransactionListItem(item: LedgerTransaction, onClick: () -> Unit)` | 白卡行：`TypeTag` + 类别（单行省略）在上（L147-158），金额 22sp（L160-165），日期 + 备注灰字（L167-179，备注用 `weight(1f, fill = false)`），右侧「›」字符代替箭头图标（L183）。整卡 `clickable(onClick)`（L142）。 |
| `TypeTag` | 187-205 | `@Composable private fun TypeTag(type: String)` | 判定用**双向包含**：`"income" in lower \\|\\| "收入" in type` → 绿 `Triple("收入", 0xFF2E7D32, 0xFFE8F5E9)`；expense/支出 → 红；**else 把原始 type 当标签**显示蓝底（L195）。所以未知 type 也能渲染，不崩。 |
| `formatLedgerAmount` | 207-214 | `fun formatLedgerAmount(amount: Double): String`（**public，非 Composable**） | `abs(amount) >= 10_000` 时折万：`"¥ X.XX 万"`，否则 `"¥ X.XX"`（L209-213）。注意负数也按绝对值判阈值、符号保留在数值里。 |
| `formatTwoDecimals` | 216-224 | `private fun formatTwoDecimals(value: Double): String` | **KMP-safe 手写两位小数**（注释 L216 明确不用 JVM `String.format`）：`round(v*100).toLong()` 拆整/分再 `padStart(2,'0')`。 |

数据注入点：`HomeRouteHost.kt:157`（`HomeRoutes.Ledger -> LedgerListScreen(...)`）、`HomeRouteHost.kt:162`（`HomeRoutes.LedgerDetail -> LedgerDetailScreen(...)`）。

#### LearningReportScreen.kt

文件：`feature/home/src/commonMain/kotlin/com.example.my_kmp_project/feature/home/LearningReportScreen.kt`（345 行）

| 符号 | 行号 | 签名 | 真实行为 |
|------|------|------|----------|
| `ReportColors` | 39-59 | `private object ReportColors`（非 Composable） | Flutter `HomeReportColors` 的 19 个 hex 常量（纯黑系暗色板：Background/HighlightCard/RecordCard/Orange/DotYellow/DotBlue/ParentChip/PlayBg…）。**全部硬编码，不走 `DemoColors`** —— 本屏是暗色孤岛。注意 `PlayBg`（L58）定义了但文件内未被引用。 |
| `LearningReportScreen` | 61-154 | `@Composable fun LearningReportScreen(onBack: () -> Unit, onOpenMembership: (() -> Unit)? = null)` | L66 `ReportMainTabRoot(isRoot = false)`。顶栏用 **`FlutterFeatureTopBar`**（L75-80，定义在本分册的 `HomeRouteHost.kt:559`，传 `containerColor`/`titleColor` 适配暗色）。内容 `verticalScroll(rememberScrollState())`（L85）+ `offset(y = (-16).dp)`（L84）上提。**数据 100% 来自 `HomeMockData`**：`reportHighlights`（L98，定义在 `HomeMockData.kt:333`）、`reportRecords`（L120，`HomeMockData.kt:339`）——**没有 api/store 参与，不发请求**。 |
| 悬浮「家长助手」 | 127-142 | — | `Box` 叠加层 `align(Alignment.BottomEnd)` + `navigationBarsPadding()`（L130）+ `padding(end=16, bottom=88)`，点击 **只 `showPlatformToast("家长助手")`**（L135），不跳转。emoji `👨‍👩‍👧` 当图标（L139）。 |
| `MembershipBanner` 挂载 | 144-152 | — | `align(BottomCenter)` + `navigationBarsPadding()`（L147）；`onOpen = { onOpenMembership?.invoke() ?: showPlatformToast("立即开通") }`（L149-151）—— **注入的 lambda 为 null 时降级为 Toast**，这是本屏唯一可被外部接线的动作。 |
| `ReportSectionHeader` | 156-173 | `@Composable private fun ReportSectionHeader(dotColor: Color, title: String)` | 8dp 圆点 + 18sp 标题，无状态。 |
| `HighlightRow` | 175-243 | `@Composable private fun HighlightRow(item: ReportHighlight)` | 类型来自 `HomeMockData.kt:59`。尾部三态判定（L178-179）：`trailing` 全数字 → 橙色分数胶囊（L220-229）；`== "▶"` 或 `"play"`(忽略大小写) → 32dp 蓝色圆形播放（L231-239）；否则原样 24sp 文本（L240，通常是 emoji）。左侧 44dp 蓝渐变方块内直接 `Text(item.emoji)`（L186-198）。**整行不可点**。 |
| `RecordRow` | 245-294 | `@Composable private fun RecordRow(item: ReportRecord)` | `HomeMockData.kt:66` 的模型；右侧两列 `time` + `status`，`statusHighlight` 决定橙色 SemiBold（L288-289）。不可点。 |
| `MembershipBanner` | 296-344 | `@Composable private fun MembershipBanner(modifier: Modifier = Modifier, onOpen: () -> Unit = {})` | **本文件唯一带 `modifier` 参数的私有 Composable**（L298，默认 `Modifier`，调用方 `LearningReportScreen.kt:145` 传入 align/inset/padding 链）。横向渐变卡 + 👑 + 两行文案；「立即开通」文字块自带 `clickable(onClick = onOpen)`（L338-342）。 |

调用点：`HomeRouteHost.kt:124`（`HomeRoutes.LearningReport -> LearningReportScreen(...)`）；路由常量 `core/router/src/commonMain/kotlin/com.example.my_kmp_project/core/router/FeatureRoutes.kt:20`（`"/home/learning_report"`）。

#### StrategyScreen.kt

文件：`feature/home/src/commonMain/kotlin/com.example.my_kmp_project/feature/home/StrategyScreen.kt`（392 行）

| 符号 | 行号 | 签名 | 真实行为 / 状态 / 副作用 |
|------|------|------|--------------------------|
| 色常量 | 44-47 | `private val GainRed = Color(0xFFFF3B30)` / `GainGreen` / `GaugeGreen` / `GaugeRed`（0xFF34C759） | 硬编码 hex。**注意语义是「涨=红」**（A 股习惯）：`AssetCell` L202/L212 里 `positive` 用 `GainRed`。 |
| `StrategyScreen` | 49-95 | `@Composable fun StrategyScreen(onBack: () -> Unit)` | 无 modifier、无数据参数。L51 `ReportMainTabRoot(isRoot = false)`。状态：`selectedTab`（L52，初值 0）、`periodIndex`（L53，**初值 4** = `strategyPeriods` 最后一项「近1年」）。顶栏 `FlutterFeatureTopBar(title = "策略", onBack, containerColor = DemoColors.Background)`（L60-65）后**手工补 0.5dp 下划线** `Box`（L67-72，对应 Flutter AppNavBar hairline）。内容 `verticalScroll(rememberScrollState())` + `padding(horizontal=16)` + `padding(top=8, bottom=24)`（L74-79，注释对齐 Flutter `fromLTRB(16,8,16,24)`）。**整屏零网络请求**。 |
| `StrategySubTabs` | 97-133 | `@Composable private fun StrategySubTabs(tabs: List<String>, selectedIndex: Int, onSelected: (Int) -> Unit)` | 居中 Row，每项 `clickable { onSelected(index) }`（L113），选中态：文字 SemiBold + 24dp 蓝色下划线条（未选中 `width(0.dp)`，L125）。tab 文案来自 `HomeMockData.strategyTabs`（L82；定义 `HomeMockData.kt:347` = 推荐/逆向/趋势）。**切换 tab 不改变下方内容** —— `selectedTab` 只喂给这里，`AssetGridCard`/`StrategyPlanCard` 都不读它（占位式实现）。 |
| `AssetGridCard` | 135-190 | `@Composable private fun AssetGridCard(periodIndex: Int, onPeriodSelected: (Int) -> Unit)` | 固定说明文案（L148-154）+ `Spacer(54.dp)` 硬凑 Flutter 行高差（L156 注释）。九宫格：`HomeMockData.strategyAssets.chunked(3)`（L157，定义 `HomeMockData.kt:349`），每行不足 3 个用 `Spacer(weight(1f))` 补位（L166）。底部周期条 `horizontalScroll`（L172-188）遍历 `HomeMockData.strategyPeriods`（`HomeMockData.kt:348`），点击 `onPeriodSelected(index)`。**选中周期同样不影响任何数值** —— 纯 UI 态。 |
| `AssetCell` | 192-217 | `@Composable private fun AssetCell(cell: StrategyAssetCell, modifier: Modifier = Modifier)` | `aspectRatio(1.35f)`（L199）小方块：label 12sp + value 14sp Bold，红/绿 8% 透明底（L202）。`StrategyAssetCell` 定义在 `HomeMockData.kt:75`。 |
| `StrategyPlanCard` | 219-315 | `@Composable private fun StrategyPlanCard()` | **全硬编码**：标题「黄金恐贪定投 · 第一期」（L232）、「逆向」chip（L239）、「如何跟投」文字（L249-254，**不可点**）、收益率 `"-11.35%"` 字面量（L264）、`FearGreedGauge(score = 63, label = "中立")`（L272）、`InvestProgressBar(current = 36, total = 50)`（L277）、「本周已投 1 份」（L285）。**「订阅」按钮 `clickable { }` 是空 lambda**（L295）—— 不请求、不跳路由、不 Toast。 |
| `InvestProgressBar` | 317-343 | `@Composable private fun InvestProgressBar(current: Int, total: Int)` | `frac = current/total` 再 `coerceIn(0f,1f)`（L320，**total=0 时是 NaN→coerceIn 仍可能异常，未见零保护**）；`fillMaxWidth(frac)` 蓝条 + 居中「current / total」文本。 |
| `FearGreedGauge` | 345-392 | `@Composable private fun FearGreedGauge(score: Int, label: String)` | 无 `modifier` 参数。`score.coerceIn(0,100)`（L354）；`Canvas(88×44dp)`（L361-365）用 `drawArc` 画 3 段各 -60°（`sweepPer = -180f/3`，L372-383），绿→`Color(0xFFFFCC00)`→红、stroke 8dp Round cap、半径 36dp；**没有指针**（与注释 L345-348 一致），下方文本「63 中立」。 |

调用点：`HomeRouteHost.kt:123`（`HomeRoutes.Strategy -> StrategyScreen(onBack = onBack)`）；常量 `core/router/.../FeatureRoutes.kt:19`（`"/home/strategy"`）。

#### AllServicesScreen.kt

文件：`feature/home/src/commonMain/kotlin/com.example.my_kmp_project/feature/home/AllServicesScreen.kt`（325 行）

| 符号 | 行号 | 签名 | 真实行为 / 状态 / 副作用 |
|------|------|------|--------------------------|
| 常量 | 49-52 | `private const val ServiceCellAspectRatio = 0.72f`、`private val FlutterToolbarHeight = 56.dp` | 复刻 Flutter SliverGrid childAspectRatio 与 `kToolbarHeight`（注释 L48/L51）——**明确不用 M3 的 64dp CenterAlignedTopAppBar**。 |
| `AllServicesScreen` | 54-113 | `@Composable fun AllServicesScreen(onBack: () -> Unit, onOpen: (String) -> Unit = {})` | L59 `ReportMainTabRoot(isRoot = false)`。状态：`isEditing`（L60）、`favoriteIds by remember { mutableStateOf(HomeFavoritesStore.snapshot()) }`（L61）、`favoriteItems = remember(favoriteIds) { HomeFavoritesStore.favoriteItems() }`（L62，**依赖 favoriteIds 重算，不是 Flow**）。结构：`AllServicesTopBar`（L70）+ `LazyColumn(contentPadding top8/bottom24)`（L71-77）。首个 item 是「常用服务 / 将按自定义顺序出现在首页」伪分区（L78-96，`showEditButton = true`、`items = favoriteItems`）；其余遍历 `HomeMockData.catalogSections`（L97，定义 `HomeMockData.kt:177`；模型 `AllServiceSection` 在 `HomeMockData.kt:31`、`AllServiceItem` 在 `:25`）。**副作用（唯一写持久化的地方）**：编辑态点角标 → `HomeFavoritesStore.remove(item.id)` / `.add(item.id)` 成功后 `favoriteIds = HomeFavoritesStore.snapshot()` 重新快照（L89-93 / L103-107）；store 内部 `persist()` 经 `core.platform.saveStringList` 写 KV（`HomeFavoritesStore.kt:40-42`，key `home_favorite_service_ids_v2`，下限 3 上限 8，`:11-13`/`:46-48`）。非编辑态点格子 → `onOpen(it.label)`（L94 / L108），由调用方决定跳转；本文件**不直接 navigate**。 |
| `AllServicesTopBar` | 115-151 | `@Composable private fun AllServicesTopBar(onBack: () -> Unit)` | **本模块唯一自绘顶栏**（不用 `ImmersiveTopAppBar`/`MineTopBar`）：`Column.fillMaxWidth.background(DemoColors.Background).statusBarsPadding()`（L118-122）+ 56dp Row（L124-128）；返回键 `IconButton(onClick = onBack)` 内 `Image(painterResource(Res.drawable.ic_nav_back))` **`rotate(180f)` 复用成左箭头**（L130-139，图标本体是右向箭头）；标题「全部服务」`weight(1f)` 居中（L140-147）+ 尾部 `Spacer(48.dp)` 配平（L148）。 |
| `AllServicesSectionBlock` | 153-241 | `@Composable private fun AllServicesSectionBlock(section: AllServiceSection, isEditing: Boolean, isFavoriteSection: Boolean, favoriteIds: Set<String> = emptySet(), onEditTap: (() -> Unit)? = null, onItemBadgeTap: (AllServiceItem) -> Unit = {}, onItemTap: (AllServiceItem) -> Unit = {})` | 标题 + 可选副标题（`section.subtitle != null` 才占 `weight(1f)`，L179-191）；`section.showEditButton && onEditTap != null` 才渲染「编辑/完成」描边按钮（L192-202）。**布局不用 `LazyGrid`**：`BoxWithConstraints` 里算 `cellHeight = maxWidth / 5f / 0.72f`（L206-207），`chunked(5)` 手工成行（L208），行内 `weight(1f).fillMaxHeight()`（L225-227），空位 `repeat(5 - rowItems.size)` 补 `Spacer`（L230-232），行距 12dp（L234-236）。角标语义：`showMinus = isFavoriteSection && isEditing`、`showPlus = !isFavoriteSection && isEditing && item.id !in favoriteIds`（L221-222）。 |
| `ServiceGridCell` | 243-325 | `@Composable private fun ServiceGridCell(item: AllServiceItem, isEditing: Boolean, showMinus: Boolean, showPlus: Boolean, onBadgeTap: () -> Unit, onTap: () -> Unit = {}, cellModifier: Modifier = Modifier)` | 格子本体：48dp 容器（L261-264）里先 `HomeServiceAssets.fromFlutterFile(item.assetName)`（L265），命中则 `HomeAssetIcon(size = 48.dp, contentScale = ContentScale.Fit)`（L267-272）；**未命中降级**为 12dp 圆角浅蓝块 + `item.label.take(1)` 首字（L273-288）。编辑态角标：16dp 圆、`TopEnd` 对齐、「×」/「+」，`clickable(onClick = onBadgeTap)`（L289-311）。整格 `clickable(enabled = !isEditing, onClick = onTap)`（L255）。标签 11sp、`lineHeight = (11*1.2f).sp`、单行省略（L314-323）。 |

调用点（3 处）：`HomeScreen.kt:73`（本地 `destination == "services"` 分支，`onOpen` 里先清栈再 `go(label)`）、`composeApp/src/commonMain/kotlin/com.example.my_kmp_project/app/AppShell.kt:426`（`AppRoute.AllServices`）、`composeApp/src/androidMain/kotlin/com.example.my_kmp_project/nativeshell/NativeAndroidMain.kt:553`。

#### HomeDetailScreens.kt

文件：`feature/home/src/commonMain/kotlin/com.example.my_kmp_project/feature/home/HomeDetailScreens.kt`（393 行）—— 三张「Flutter 详情页」的 KMP 复刻 + 4 个私有子件。三屏都 `ReportMainTabRoot(isRoot = false)`（L57 / L130 / L210），都用 `core.design.MineTopBar`（合规顶栏），都零网络零 store。

| 符号 | 行号 | 签名 | 真实行为 |
|------|------|------|----------|
| `AfterSalesDetailRow` | 38-49 | `data class AfterSalesDetailRow(id, title, kindLabel, customerName, customerPhone, plateNo, mileageKm: Int?, serviceDate, content, appointmentId: Int?)` | 两个可空字段 `mileageKm`/`appointmentId` 决定详情页是否显示对应行（L96 / L104）。 |
| `AfterSalesServiceDetailScreen` | 52-122 | `@Composable fun AfterSalesServiceDetailScreen(row: AfterSalesDetailRow, onBack: () -> Unit)` | 顶栏「服务详情」（L64）。`kindColor`：`kindLabel.contains("修")` → 橙 `0xFFE67E22`，否则绿 `0xFF2E7D32`（L58）。hero 卡 = kind 胶囊 + 20sp 标题（L71-89）；`DetailSection("客户车辆")` 用 `buildList` 条件装配（L90-98）；`DetailSection("服务信息")` 同理（L99-106）；`content` 非空白才渲染「备注」卡（L107-119）。**无点击副作用**。 |
| `AnalyticsDetailScreen` | 124-194 | `@Composable fun AnalyticsDetailScreen(record: AnalyticsRecordRow, onBack: () -> Unit)` | 入参模型 `AnalyticsRecordRow` 定义在 `HomeRouteHost.kt:239`。顶栏「数据详情」（L136）。**提示条二选一**：`record.anomaly` → 红色 `CueBanner("异常记录")`，否则 `record.featured` → 蓝色 `CueBanner("精选记录")`（L143-155）。hero 标题+副标题（L156-166）；`DetailSection("流量漏斗")` 列 PV/点击/转化（L167-174）；「漏斗占比」用 `record.pv.coerceAtLeast(1)` 当分母画 3 条 `MetricBar`（L185-190，**注释 L124 明确「no chart lib」**）。 |
| `NewCarFollowArchiveDetailScreen` | 196-331 | `@Composable fun NewCarFollowArchiveDetailScreen(row: NewCarFollowRow, onBack: () -> Unit)` | 入参模型在 `HomeRouteHost.kt:227`。本地状态 3 个：`level = remember { mutableStateOf(row.intentBand) }`（L202，初值取数据行）、`logBody`（L203）、`logs = remember { listOf(两条写死的跟进文案) }`（L204-209）。顶栏「跟进档案详情」（L216）→ hero 客户名 + `车 · 阶段`（L230-232）→ `DetailSection("档案信息")` 六行（L234-244）→「调整级别」高/中/低胶囊，点击只改本地 `level`（L254-276，**没有 onLevelChange 回调、不回落库**）→「写一条跟进」`OutlinedTextField(minLines = 3)`（L287-293）+「提交跟进」按钮：`logBody.isBlank()` → Toast「请填写跟进内容」，否则 Toast「已记录跟进」并把 `logBody` 清空（L303-309）。**注意：提交不会把内容写进 `logs` 列表** —— 新文案不会出现在下方「跟进记录」，是纯 Toast 占位。L312 用全限定名 `androidx.compose.ui.text.style.TextAlign.Center`（未 import，写法合法但风格不一致）。 |
| `DetailSection` | 333-356 | `@Composable private fun DetailSection(title: String, rows: List<Pair<String, String>>)` | 白卡 key-value 两列 `SpaceBetween`，无 modifier 参数。 |
| `CueBanner` | 358-372 | `@Composable private fun CueBanner(color: Color, title: String, subtitle: String)` | 由传入色派生 10% 底 + 35% 描边（L364-365）。 |
| `MetricBar` | 374-393 | `@Composable private fun MetricBar(label: String, fraction: Float, color: Color)` | 40dp 标签 + 10dp 高轨道；`fraction.coerceIn(0.02f, 1f)`（L387）保证零值也露一点色。 |

调用点：`HomeRouteHost.kt:170`（`HomeRoutes.DataAnalyticsDetail`）、`:187`（`HomeRoutes.AfterSalesDetail`）、`:201`（`HomeRoutes.NewCarFollowDetail`）。路由常量见 `core/router/.../FeatureRoutes.kt:33 / :40 / :43`。

#### HomeSearchScreen.kt

文件：`feature/home/src/commonMain/kotlin/com.example.my_kmp_project/feature/home/HomeSearchScreen.kt`（578 行）—— `/home/search`，Flutter `SearchPage` 复刻。

| 符号 | 行号 | 签名 | 真实行为 / 状态 / 副作用 |
|------|------|------|--------------------------|
| `SearchFieldHeight` | 71 | `private val SearchFieldHeight = 44.dp` | 对应 Flutter `SearchPageTheme.searchFieldHeight`（注释 L70）。 |
| `searchRankCoverRes` | 73-82 | `private fun searchRankCoverRes(rank: Int): DrawableResource` | 把 rank 1-7 映射到 `home_search_rank_1..7`，else 用 `_8`。**全文件唯一声明处、无任何调用点 → 死代码**（8 张榜位图 import L58-65 实际未被渲染；`RankListRow` 改用 Canvas 播放图标）。grep：`grep -rn "searchRankCoverRes" --include="*.kt" feature/home` → 仅命中 L73。 |
| `HomeSearchScreen` | 84-165 | `@OptIn(ExperimentalLayoutApi::class) @Composable fun HomeSearchScreen(onBack: () -> Unit)` | L87 `ReportMainTabRoot(isRoot = false)`。5 个本地状态：`query`（L88）、`history = remember { mutableStateOf(HomeMockData.searchHistory) }`（L91，初值 `HomeMockData.kt:223`）、`discovery`（L92，`HomeMockData.kt:231`）、`selectedRankTab`（L93，初值 0）、`rankItems = remember(selectedRankTab) { HomeMockData.rankItemsForTab(selectedRankTab) }`（L94，`HomeMockData.kt:292`）。`rotatingHint`（L90）= `searchHistory.getOrElse(2)`，即 placeholder 轮换是**取固定下标**、不会随时间轮播。根 `Column.background(DemoColors.PageBg)` → `SearchHeaderBar` → `verticalScroll` 内容列（L112-117，`padding(bottom = 24.dp)`）。**没有真正的搜索结果页**：`onSearch` 只做历史去重前插（L107-110：`listOf(text) + history.filterNot { it == text }.take(9)`，**上限 9 条**），不请求、不导航。`onClear` 只清内存态（L121 `history = emptyList()`），**不写 KV**（对比 `AllServicesScreen` 会持久化）。点历史/发现/筛选标签一律 `query = it`（L122 / L131 / L136）。`onRefresh` = `discovery.reversed()`（L130，「换一换」只是原地反转列表）。 |
| `SearchHeaderBar` | 167-271 | `@Composable private fun SearchHeaderBar(query: String, rotatingHint: String, onQueryChange: (String) -> Unit, onBack: () -> Unit, onCancel: () -> Unit, onSearch: () -> Unit)` | **自绘搜索头，不用任何 TopAppBar**：`Row.fillMaxWidth.background(PageBg).statusBarsPadding()`（L177-182，SafeArea 后再 `padding(start=8, end=16, top=8, bottom=12)`，注释对齐 Flutter `fromLTRB(8,8,16,12)`）。返回键 44dp 命中区 + `ic_nav_back` `rotate(180f)` + `ColorFilter.tint(Accent)`（L185-201）。输入框是 **`BasicTextField`**（L220-238，非 M3 `TextField`）：`singleLine`、`cursorBrush = SolidColor(DemoColors.Accent)`、`decorationBox` 里 `query.isEmpty()` 时叠 `rotatingHint`（L227-237）。放大镜用 **文本字形「⌕」**且可点触发 `onSearch`（L213-218）。空文本时右侧显示麦克风资源图（L239-249），有文本时换成「✕」清空 `onQueryChange("")`（L250-258）。尾部「取消」`clickable(onClick = onCancel)`，父层把 `onCancel` 与 `onBack` 绑成同一个 `onBack`（L105-106）。 |
| `SearchHistorySection` | 273-324 | `@Composable private fun SearchHistorySection(history: List<String>, onClear: () -> Unit, onTagTap: (String) -> Unit)` | 头行 `heightIn(min = 44.dp)`，「清除」= 图标 `home_search_icon_clear_history` + 文本，整行 `clickable(onClear)`（L299-311）。标签用 **`FlowRow`**（L314，需 `@OptIn(ExperimentalLayoutApi::class)`，L273）8dp 间距。 |
| `SearchDiscoverySection` | 326-385 | `@Composable private fun SearchDiscoverySection(discovery: List<String>, onRefresh: () -> Unit, onTagTap: (String) -> Unit)` | 白卡 + 描边（L336-340）；「换一换」有**两个可点区**：18dp `home_search_icon_refresh` 图（L355-362）与右侧文本（L364-369），都绑 `onRefresh`。`index == 0` 的标签 `highlight = true`（L379）——只有第一项高亮。 |
| `SearchFilterSection` | 387-419 | `@Composable private fun SearchFilterSection(tags: List<String>, onTagTap: (String) -> Unit)` | 「快捷筛选」+ 自绘 `SearchSliderIcon`（L406，注释说明刻意不用漏斗位图），tags 来自 `HomeMockData.filterTags`（`HomeScreen` 侧传 `HomeMockData.filterTags`，见 L135）。 |
| `SearchSliderIcon` | 421-437 | `@Composable private fun SearchSliderIcon(modifier: Modifier = Modifier, color: Color)` | **有 `modifier` 参数**（本分册少见）。`Canvas(fillMaxSize)` 画 3 条横线（y = 22%/50%/78%）+ 3 个 2.2dp 圆旋钮（x = 35%/65%/45%），1.5dp Round cap（L425-435）。 |
| `SearchTagChip` | 439-466 | `@Composable private fun SearchTagChip(label: String, onTap: () -> Unit, highlight: Boolean = false, softFill: Boolean = false)` | 20dp 胶囊文本；底色三态 `highlight`→Accent 10%、`softFill`→`DemoColors.Background`、else `PageBg`（L448-452）。**`softFill` 三个调用点都未传**（默认 false），该分支是死代码。 |
| `RankTabBar` | 468-504 | `@Composable private fun RankTabBar(tabs: List<String>, selectedIndex: Int, onSelected: (Int) -> Unit)` | `horizontalScroll` 行 + 20dp 间距；选中态 Accent SemiBold + 20×2dp 下划线（未选中 `width(0.dp)`）。tabs 来自 `HomeMockData.rankTabs`（`HomeMockData.kt:249` = 热配榜/诵读榜/剧集榜/记录榜）。 |
| `RankListRow` | 506-578 | `@Composable private fun RankListRow(item: SearchRankItem)` | 模型 `HomeMockData.kt:38`。名次色：1=金 `0xFFF5A623`、2=`DemoColors.Muted`、3=铜 `0xFFC47B2C`、else TextSecondary（L517-522），固定宽 24dp。封面 72dp 圆角块里**不加载任何图片**，而是 Canvas 手绘「圆圈 + 三角」play 图标（L529-550，注释 L536 说明是因为 Flutter SoT 上 picsum 图加载失败，直接固化成占位视觉）。标题 1 行 + 副标题 2 行（`lineHeight = 19.6.sp`，L568）。尾部「›」字符。**整行不可点**（无 clickable）。 |

调用点：`HomeRouteHost.kt:122`（`HomeRoutes.Search -> HomeSearchScreen(onBack = onBack)`）；常量 `core/router/.../FeatureRoutes.kt:18`。

#### HomeSections.kt

文件：`feature/home/src/commonMain/kotlin/com.example.my_kmp_project/feature/home/HomeSections.kt`（989 行）—— 首页 11 个区块的**唯一实现**。文件头注释（L47-58）说明存在理由：改前 `HomeScreen.kt`（共享）与 `composeApp/androidMain/.../NativeJetpackHome*.kt` 各写一份、11 区块逐行重复必然漂移；现在**两侧只保留宿主**（滚动容器 / 下拉刷新 / 签到弹窗），区块共用本文件。外边距严格照抄 Flutter 各 section 的 `EdgeInsets`，**首块之前的 16 由宿主提供**（L56-57）。

| 符号 | 行号 | 签名 | 真实行为 |
|------|------|------|----------|
| `HomeSectionStyle` | 59-106 | `object HomeSectionStyle`（非 Composable，除 3 个 `@Composable get`） | 设计 token 层：`Accent/Surface/FillSecondary/LabelPrimary/LabelSecondary/LabelTertiary/Separator` 全部**转发 `DemoColors`**（L60-66，`FillSecondary` = `DemoColors.PageBg`）；`RadiusMd = 8.dp`（L69）；`GroupedCardBorder = 1.0f`、`ThinCardBorder = 0.5f`（L72/L75，两种卡片描边宽度）；3 个 `@Composable get` 文字样式 `LargeTitle28` / `SectionTitle18` / `SectionLabel14`（L77-105）统一 `fontFamily = DemoFonts.Geist` + Flutter 的 `lineHeight` 与负 `letterSpacing`（-1.6 / -0.54 / -0.28 sp）。 |
| `HomeGreetingSection` | 110-145 | `@Composable fun HomeGreetingSection(greeting: String, modifier: Modifier = Modifier)` | 区块 1「问候 + 消息胶囊」。`padding(start=16, end=16, top=24)`（L115，即注释所说首块自带的 24）。左 `Text(greeting, style = LargeTitle28)` `weight(1f)`；右侧胶囊是**写死的「3条新消息」**（L138）+ `HomeIcons.NotificationsNone` tint Accent（L130-135）。胶囊**不可点**（无 clickable）→ 消息数与真实未读无关，纯视觉占位。 |
| `HomeSearchBar` | 149-214 | `@Composable fun HomeSearchBar(placeholder: String, onSearch: () -> Unit, onScan: () -> Unit, modifier: Modifier = Modifier)` | 区块 2。**不是真输入框**：44dp 高白底 0.5dp 描边 `Row` 整体 `clickable(onSearch)`（L173），内部只有 `HomeIcons.Search` 图标 + 灰色 `placeholder`（L177-190）——点搜索栏即跳搜索页。右侧独立 44×44 方块 `clickable(onScan)` + `HomeIcons.QrScan`（L193-212）。lambda 语义：`onSearch`/`onScan` 都由宿主注入导航动作（`HomeScreen.kt:197-198`）。 |
| `HomeBannerSection` | 218-284 | `@Composable fun HomeBannerSection(modifier: Modifier = Modifier)` | 区块 3，**代码合成而非整张位图**：132dp 高圆角 Box 叠 4 层 —— `Image(painterResource(Res.drawable.home_banner_bg), Crop)`（L228-233，注释 L227：Flutter 原是 picsum 网络图，已本地化为 `:core:design` drawable）→ 黑色 25% 暗化层（L235-239）→ Accent@55%→透明横向渐变层（L241-249）→ 文案列（L250-282「朋友圈营销」/「一键分享，高效触达客户」/白底胶囊「立即体验」）。**整个 Banner 无任何 clickable**，CTA 是装饰。 |
| `visibleHomeFeatures` | 288-295 | `fun visibleHomeFeatures(items: List<HomeFeatureItem>): List<HomeFeatureItem>`（**public，非 Composable**） | 裁剪规则：`maxItems = 9`；把 `label == "更多"` 的项挑出，非「更多」项在存在「更多」时只取 8 个、否则 9 个，最后把**最后一个**「更多」追加到末尾（L291-294）。保证「更多」恒在末位。 |
| `HomeFeatureGrid` | 297-357 | `@Composable fun HomeFeatureGrid(items: List<HomeFeatureItem>, onFeature: (String) -> Unit, modifier: Modifier = Modifier)` | 区块 4 金刚区。`val visible = remember(items) { visibleHomeFeatures(items) }`（L303，依赖 items 重算）。卡片描边用 `HomeSectionStyle.GroupedCardBorder.dp`（L311）。`visible.chunked(5)` 每行 5 列、行间 8dp（L317-318），不足补 `Spacer(weight(1f))`（L353）。每格 44dp 圆角方块底 `FillSecondary`，内 `HomeAssetIcon(resource = HomeServiceAssets.featureForLabel(item.label), size = 44.dp)`（L334-338，图标按中文 label 映射，未知 label 落 all_functions 兜底）+ 11sp 单行标签（L341-350）。点击 → `onFeature(item.label)`（L324）——**参数是 label 字符串而非路由**，由宿主 `HomeScreen.kt:205` 转交 `go(label)`。 |

| `HomeStoreMetricsCard` | 361-443 | `@Composable fun HomeStoreMetricsCard(storeName: String, selectedTab: Int, tabs: List<String>, metrics: List<HomeMetric>, details: List<HomeMetricDetail>, onTabSelected: (Int) -> Unit, onStoreTap: () -> Unit, onOpenLedger: () -> Unit, modifier: Modifier = Modifier)` | 区块 5「公司数据」。**受控组件**：`selectedTab` 由宿主持有（`HomeScreen.kt:115/232`），本卡不存状态。头部标题 + 右侧「查看更多」+ `HomeIcons.ChevronRight` 整行 `clickable(onOpenLedger)`（L378-398）。卡内顺序：`StorePickerRow`（L412）→ `MetricTabBar`（L414）→ `metrics.chunked(2)` 两列指标，单数时补 `Spacer(weight(1f))`（L416-427）→ `details.isNotEmpty()` 才画 0.5dp 分隔线 + 一排 `DetailActionTile`（L428-440）。指标/明细数据由宿主注入 `HomeMockData.metricsToday/Yesterday/Month` 与 `HomeMockData.metricDetails`（`HomeScreen.kt:226-231`）。 |
| `StorePickerRow` | 445-495 | `@Composable private fun StorePickerRow(storeName: String, onTap: () -> Unit)` | 浅底圆角行 `clickable(onTap)`（L452）：28dp 图标盒用 `HomeStrokeIcons.Storefront`（L463-468）→ 门店名（单行省略）→ `HomeStrokeIcons.ArrowDown` + `HomeStrokeIcons.SwapHoriz`（contentDescription「切换门店」，L481-493）。`onTap` 语义 = 打开切店弹窗（`HomeScreen.kt:233-236` 判登录）。 |
| `MetricTabBar` | 497-545 | `@Composable private fun MetricTabBar(tabs: List<String>, selectedTab: Int, onTabSelected: (Int) -> Unit)` | 分段控件：外层 `FillSecondary` + 3dp 内衬，每项 `weight(1f)`；选中态 `Surface` 底 + `.then(if (active) Modifier.border(0.5dp…) else Modifier)`（L517-527）——**用 `.then()` 做条件 Modifier**，无 modifier 参数（父层已传 weight）。 |
| `MetricTile` | 547-574 | `@Composable private fun MetricTile(metric: HomeMetric, modifier: Modifier = Modifier)` | 值 22sp Bold（`lineHeight = 24.2.sp` 即 height 1.1，`letterSpacing -0.6sp`）+ 标签 12sp 单行省略。**底色 `DemoColors.CanvasSoft`（L552）而非 `HomeSectionStyle.FillSecondary`** —— 本文件唯一直接用 `DemoColors` 的底色，与同卡其它处存在 token 不一致。不可点。 |
| `DetailActionTile` | 576-609 | `@Composable private fun DetailActionTile(detail: HomeMetricDetail, modifier: Modifier = Modifier)` | 三段文本：Accent 18sp 值 + 标签 + **「详情」二字（L602-607）**。底 `DemoColors.LinkBgSoft.copy(alpha = 0.35f)`（L581）。**「详情」没有任何 `clickable`** —— 看起来能点其实不可点，明细详情在本模块无落地页。 |
| `HomeHubEntryCard` | 613-… | `@Composable fun HomeHubEntryCard(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier)` | 区块 6 入口卡：整卡 `clickable(onClick)`（L632），44dp 图标盒（Accent 10% 底 + `Icon(icon, 24dp)`，L636-649）+ 标题 17sp / 副标题 13sp（L651-659）+ 尾部箭头图标。`icon` 是 **`ImageVector`** 参数，宿主传 `HomeIcons.GridView` / `HomeIcons.Analytics`（`HomeScreen.kt:244/264`）。 |

| `HomeHubEntryCard`（续） | 660-666 | — | 尾部箭头 `HomeIcons.ChevronRight` tint `LabelTertiary` 24dp。 |
| `HomeServiceGrid` | 671-741 | `@Composable fun HomeServiceGrid(items: List<HomeServiceItem>, onService: (String) -> Unit, onOpenAll: () -> Unit, modifier: Modifier = Modifier)` | 区块 7「服务推荐」。头部标题 + 「全部」+ 箭头整行 `clickable(onOpenAll)`（L694-710）。卡内 `items.chunked(4)` 4 列（L724），`val index = rowIndex * 4 + colIndex`（L728）用来取循环主色，不足 4 个补 `Spacer(weight(1f))`（L736）。每格 `onClick = { onService(item.label) }`（L732）。**注意本卡的两个出口都是 label/回调**：宿主 `HomeScreen.kt:252-255` 只在 label 为「更多」「全部」时放行跳转，因此宫格内其它服务项点击在当前装配下是 no-op。 |
| `serviceAccent` | 743-753 | `private fun serviceAccent(index: Int): Color` | 8 色循环（注释 L743 直接抄 Flutter `accents`）：`Accent / Violet / Cyan / Warning / HighlightPink / LinkDeep / Violet / TextPrimary`，**index 6 与 0 颜色重复、末位用文字色当装饰色**（原样照抄 Flutter 的列表）。全部走 `DemoColors` token，无硬编码 hex。 |
| `ServiceGridItem` | 755-812 | `@Composable private fun ServiceGridItem(item: HomeServiceItem, accent: Color, onClick: () -> Unit, modifier: Modifier = Modifier)` | 44dp 圆角盒（`accent.copy(alpha = 0.12f)` 底）内 `Icon(HomeStrokeIcons.forServiceLabel(item.label), tint = accent, 22dp)`（L766-780）——**图标按 label 动态选**，映射函数在 `HomeStrokeIcons.kt`。角标：`item.badge?.let { … }`（L781-798），白字 9sp、`TopEnd` 对齐 + `offset(x = 6.dp, y = (-4).dp)` 复刻 Flutter `Positioned(top:-4,right:-6)` 越界显示（注释 L790），**「热门」用 `DemoColors.Warning` 其它用 `Accent`**（L794）。整格 `clickable(onClick)`（L763）。 |
| `HomeContactList` | 816-899 | `@Composable fun HomeContactList(items: List<HomeContactItem>, onContact: (HomeContactItem) -> Unit, modifier: Modifier = Modifier)` | 区块 8「联系汽车之家」，标题文案写死（L827）。列表卡描边用 `ThinCardBorder`（0.5dp，L835）。每行 `heightIn(min = 72.dp)`（L845，注释：Flutter ListTile 两行最小高 72）+ `clickable { onContact(item) }`（L846）；头像 44dp 圆 + `HomeIcons.PersonOutline`（L850-863）；**尾部图标由 `item.trailing` 字符串驱动**：`"chat"`→`HomeIcons.ChatBubble`、`"phone"`→`HomeIcons.Phone`、else→`ChevronRight`（L874-879），前两者 tint Accent 否则灰（L881-885）。行间 `HorizontalDivider(0.5dp, padding(start = 68.dp, end = 16.dp))` 缩进对齐头像右侧（L889-895）。宿主 `HomeScreen.kt:258` 传的是 `onContact = {}` → **整块当前不可用**。 |
| `HomeNewsList` | 903-989 | `@Composable fun HomeNewsList(items: List<HomeNewsItem>, onOpenAll: () -> Unit, modifier: Modifier = Modifier)` | 区块 9「行业动态」。头「查看更多」`clickable(onOpenAll)`（L920-936）；宿主 `HomeScreen.kt:259` 传 `onOpenAll = {}` → 也不可点。每条新闻卡是**纯展示**：标题 15sp 最多 2 行（`lineHeight = 21.sp` height 1.4，L955-963）+ `"${news.source}  ${news.date}"`（L966，两个空格是原样拼的）+ 右侧 96×72dp 灰盒里 `HomeIcons.Article` 28dp 当封面占位（L971-984，**没有真实图片加载，也没有 Coil**）。**整条新闻无 clickable** → 资讯不可点开。 |

状态来源小结：本文件**零 `mutableStateOf`、零 store、零 Flow、零 `LaunchedEffect`**（唯一 `remember` 是 L303 `remember(items)`）。全部区块都是「参数进 / lambda 出」的无状态组件，业务态一律在宿主（`HomeScreen.kt` 或 `NativeJetpackHome.kt`）。图标分三批来源：`HomeIcons.*`（`ImageVector`，`HomeIcons.kt`）、`HomeStrokeIcons.*`（含 `forServiceLabel` 动态映射，`HomeStrokeIcons.kt`）、`HomeAssetIcon`/`Res.drawable`（位图，`:core:design`）。

#### HomeRouteHost.kt

文件：`feature/home/src/commonMain/kotlin/com.example.my_kmp_project/feature/home/HomeRouteHost.kt`（3574 行）—— Home 域二级屏集散地：一个 `when(route)` 入口 + 30 多个私有屏 + 4 个行模型 + 1 个 Mock 仓。

| 符号 | 行号 | 签名 | 真实行为 |
|------|------|------|----------|
| `FlutterToolbarHeight` | 112 | `private val FlutterToolbarHeight = 56.dp` | 与 `AllServicesScreen.kt:52` 重复定义一份（同注释、同值，**两处未提取共用**）。 |
| `HomeRouteHost` | 115-208 | `@Composable fun HomeRouteHost(route: String, onBack: () -> Unit, onNavigate: (String) -> Unit = {})` | 纯分派，本体无状态（详见「HomeRouteHost 专题」）。 |
| `HomeListRow` | 210 | `data class HomeListRow(val title: String, val subtitle: String)` | 通用两行列表项，多个屏/Mock 复用。 |
| `UsedCarOrderRow` | 212-225 | `data class UsedCarOrderRow(id: String, kindLabel, statusLabel, submittedDate, vehicleModel, plateNo, modelYear: Int, mileageKm: Int, amountLabel: String, amount: Int, customerName, vin: String = "")` | 注意金额是 **`Int`（分/元级整数）**，同时存在 `amountLabel` 预格式化串。 |
| `NewCarFollowRow` | 227-237 | `data class NewCarFollowRow(id, customerName, phone, vehicle, stage, intentBand, nextFollow, owner, overdue: Boolean = false)` | `overdue` 供列表标红；`intentBand` 是 `NewCarFollowArchiveDetailScreen` 的「调整级别」初值。 |
| `AnalyticsRecordRow` | 239-251 | `data class AnalyticsRecordRow(code, title, subtitle, status, pv: Int, uv: Int, clicks: Int, converts: Int, roi: Double, featured: Boolean = false, anomaly: Boolean = false)` | `status` 取 `"active"/"paused"/"draft"`（见 L294-303 数据）。 |
| `HomeSecondaryMock` | 253-334 | `object HomeSecondaryMock` | 见下「数据来源」。 |

##### HomeSecondaryMock（L253-334）

注释 L254 自己承认定位：「Prefer [HomeSecondaryStore] for mutable pipelines; kept for ledger/analytics」。分两类：

| 成员 | 行号 | 形态 | 实际来源 |
|------|------|------|----------|
| `usedCarOrders` / `usedCars` | 255-260 | `get()` 计算属性 | **代理 `HomeSecondaryStore.usedCarOrders()`**；`usedCars` 派生成 `HomeListRow`，字符串里用 `amount / 10000.0` 万、`mileageKm / 10000.0` 万公里（L259） |
| `ledger` | 261-286 | **写死 3 条 `LedgerTransaction`** | 日期硬编码 2026-09-22~24，含 `note = null` 的第三条（用来验 `LedgerScreens` 的备注分支） |
| `analytics` | 287-290 | 写死 2 条 `HomeListRow` | 「本周线索转化 12.4%」「试驾到店」 |
| `analyticsTotal` | 292 | `= 48` | 注释 L291：Flutter SoT 共 48 条，此处只落 10 条 |
| `analyticsRecords` | 293-304 | 写死 10 条 `AnalyticsRecordRow` | AN-0001…AN-0010，其中 2 条 `anomaly = true`、4 条 `featured = true`，AN-0004 是 `clicks = 0 / converts = 0 / roi = 0.0` 的零值样本（喂给 `AnalyticsDetailScreen` 的 `coerceAtLeast(1)` 分母） |
| `partners` / `followUps` / `appointments` / `orders` | 305-321 | 各写死 2-3 条 `HomeListRow` | 4 个 Todo 屏的数据源 |
| `afterSales` / `afterSalesDetails` / `newCars` / `newCarFollows` | 322-333 | `get()` 计算属性 | **代理 `HomeSecondaryStore.afterSalesDetails()` / `.newCarFollows()`**，再映射成展示行 |

结论：**没有任何屏走 `feature/home/api/`** —— 全部是 `HomeMockData` / `HomeSecondaryMock` / `HomeSecondaryStore` 三者之一。`DeferredDestinationStub` 定义在 `feature/shell/src/commonMain/kotlin/com.example.my_kmp_project/feature/shell/DeferredDestinationStub.kt:25`，**本模块 12 个文件里 grep 无引用**（`grep -rn "DeferredDestinationStub" --include="*.kt" .` 只命中 shell 模块自身），Home 的占位形式是「屏存在但内容写死 + 控件无 clickable」。

### HomeRouteHost 专题

**对外暴露**：唯一 public 符号 `HomeRouteHost(route: String, onBack: () -> Unit, onNavigate: (String) -> Unit = {})`（`HomeRouteHost.kt:116`）+ public `FlutterFeatureTopBar`（`:559`）+ 4 个行模型 + `HomeSecondaryMock`。**没有 expect/actual，没有 navigator 依赖** —— 它只吃一个 `route` 字符串、吐一个 `onNavigate(String)`，跳转能力完全由宿主决定，因此可以脱离 `LocalAppNavigator` 单测。

调用点（三端）：

| 端 | 调用链 | 文件:行 |
|----|--------|---------|
| 装配层（common，三端共用） | `ShellBootstrap.install()` → `CrossFeatureScreens.install(AppCrossFeatureScreens)` | `composeApp/src/commonMain/kotlin/com.example.my_kmp_project/app/ShellBootstrap.kt:24`（`install` 定义 `:20-24`；由 `AppShell.kt:107` 触发） |
| 端口接口 | `CrossFeatureScreenPort.HomeRouteScreen(route, onBack, onNavigate)`；`CrossFeatureScreens.HomeRouteScreen(...)` 转 `require().…` | `component/src/commonMain/kotlin/com.example.my_kmp_project/component/routing/CrossFeatureScreenPort.kt:40`（接口）与 `:100-105`（外观转发，未 install 时 `:67` 抛「run ShellBootstrap.install() before rendering」） |
| 实现 | `override fun HomeRouteScreen(...) { HomeRouteHost(route, onBack, onNavigate) }` | `composeApp/src/commonMain/kotlin/com.example.my_kmp_project/app/AppCrossFeatureScreens.kt:88-94`（`HomeRouteHost` 调用在 `:93`） |
| Android Compose 壳 | `HomeScreen` 内部 `destination` 非空且 host==Home/兜底时直接本模块函数 | `feature/home/.../HomeScreen.kt:82`、`:97` |
| Android **native** Tab 壳 | `ShellOverlay.HomeRoute -> { val route = homeRoute ?: HomeRoutes.Search; HomeRouteHost(route, onBack = { popHomeRoute() }, onNavigate = { navigateSecondary(it) }) }` | `composeApp/src/androidMain/kotlin/com.example.my_kmp_project/nativeshell/NativeAndroidMain.kt:563`（默认路由 `HomeRoutes.Search`，L 前一行 `:562`；自带栈 `popHomeRoute()`，与 common 的单层 `destination = null` 不同） |
| iOS / OHOS | 无独立调用：`iosApp/` `ContentView.swift` 与 `harmonyApp/` `Index.ets` 只托管 `App()`/`MainArkUIViewController`，二级路由经上面 common 链路 | 见 AGENTS.md「Thin shell」；本模块 grep 无 iOS/OHOS 直接引用 |

**内部分派表（`HomeRouteHost.kt:121-207`，逐条）**：

| 路由常量（值在 `core/router/.../FeatureRoutes.kt`） | 行号 | 目标屏 | onNavigate 语义 |
|---|---|---|---|
| `HomeRoutes.Search` `/home/search` | 122 | `HomeSearchScreen(onBack)` | — |
| `HomeRoutes.Strategy` | 123 | `StrategyScreen(onBack)` | — |
| `HomeRoutes.LearningReport` | 124-127 | `LearningReportScreen` | **`onOpenMembership = { onNavigate("/pay/membership") }`**（L126，**硬编码字符串而非 `MineRoutes.Membership` 常量**，值恰好同 `FeatureRoutes.kt:85`） |
| `HomeRoutes.CheckInMall` | 128 | `CheckInMallScreen(onBack)` | — |
| `HomeRoutes.TransliterFeed` | 129-132 | `TransliterFeedScreen` | `onOpenHotRank = { onNavigate(HomeRoutes.HotRankDetail) }` |
| `HomeRoutes.HotRankDetail` | 133 | `HotRankDetailScreen(onBack)` | — |
| `HomeRoutes.LifeService` | 134 | `LifeServiceScreen(onBack)` | — |
| `HomeRoutes.LiveCommerce` | 135-138 | `LiveCommerceScreen` | `onJoin = { onNavigate(AppRoutePath.community) }` |
| `HomeRoutes.Club` | 139-142 | `ClubScreen` | 同上 `AppRoutePath.community` |
| `HomeRoutes.UsedCar` | 143-150 | `UsedCarListScreen` | **`onItem` 先 `HomeSecondaryStore.selectUsedCar(order.id)` 写入选中态，再 `onNavigate(HomeRoutes.UsedCarDetail)`**（L146-147）；`onCreate = onNavigate(UsedCarCreate)` |
| `HomeRoutes.UsedCarDetail` | 151-155 | `UsedCarDetailScreen` | 入参 `HomeSecondaryStore.findUsedCar(selectedUsedCarId) ?: usedCarOrders().first()`（**先查选中、查不到退回首条**） |
| `HomeRoutes.UsedCarCreate` | 156 | `UsedCarCreateScreen(onBack)` | — |
| `HomeRoutes.Ledger` | 157-161 | `LedgerListScreen` | `items = HomeSecondaryMock.ledger`（**Mock 直传**）；`onItem = onNavigate(LedgerDetail)`（**不传具体 item**） |
| `HomeRoutes.LedgerDetail` | 162-165 | `LedgerDetailScreen` | `item = HomeSecondaryMock.ledger.firstOrNull()` —— **无论点哪条都只展示第一条**，明细选择链路缺失（对比 UsedCar/AfterSales/NewCar 都走 store.select） |
| `HomeRoutes.DataAnalytics` | 166-169 | `AnalyticsListScreen` | `onItem = onNavigate(DataAnalyticsDetail)` |
| `HomeRoutes.DataAnalyticsDetail` | 170-173 | `AnalyticsDetailScreen` | `record = HomeSecondaryMock.analyticsRecords.first()`（**同样恒首条 AN-0001**，且 `.first()` 空列表会抛，L171） |
| `HomeRoutes.TodoPartner` | 174 | `PartnerPendingScreen(onBack)` | 数据 `HomeTodoStore.pendingApplications()`（读屏 L3193-3194），**不是** `HomeSecondaryMock.partners`（该成员全仓 0 引用） |
| `HomeRoutes.TodoFollowUp` | 175 | `FollowUpCustomersScreen(onBack)` | 转发 `TodoListScreen` + **函数内写死的 `Triple` 列表**（L3275-3278）；`.followUps` 未使用 |
| `HomeRoutes.TodoAfterSales` | 176 | `AfterSalesAppointmentsScreen(onBack)` | 同上，硬编码 `Triple`（L3288-3290）；`.appointments` 未使用 |
| `HomeRoutes.TodoOrderReview` | 177 | `StoreReviewOrdersScreen(onBack)` | 同上，硬编码 `Triple`（L3301-3302）；`.orders` 未使用 |
| `HomeRoutes.AfterSales` | 178-185 | `AfterSalesListScreen` | `onItem`：`HomeSecondaryStore.selectAfterSales(row.id)` + `onNavigate(AfterSalesDetail)`；`onCreate` → `AfterSalesCreate` |
| `HomeRoutes.AfterSalesCreate` | 186 | `AfterSalesCreateScreen(onBack)` | — |
| `HomeRoutes.AfterSalesDetail` | 187-191 | `AfterSalesServiceDetailScreen` | `findAfterSales(selectedAfterSalesId) ?: afterSalesDetails().first()` |
| `HomeRoutes.NewCarFollow` | 192-199 | `NewCarFollowListScreen` | `selectNewCarFollow(row.id)` + `onNavigate(NewCarFollowDetail)`；`onCreate` → `NewCarFollowCreate` |
| `HomeRoutes.NewCarFollowCreate` | 200 | `NewCarFollowCreateScreen(onBack)` | — |
| `HomeRoutes.NewCarFollowDetail` | 201-205 | `NewCarFollowArchiveDetailScreen` | `findNewCarFollow(selectedNewCarFollowId) ?: newCarFollows().first()` |
| **else** | 206 | `CrudDetailScreen("未识别路由", "route=$route", onBack)` | 兜底把未知 route 渲染成「未识别路由」详情页 —— 这就是 `HomeScreen.kt:63` 把 `ProductRouteHost.Unknown` 原文塞进 destination 后仍不白屏的原因 |

`ReportMainTabRoot` 上报：**`HomeRouteHost` 本体不调用它**（L116-208 全无）。上报责任在各屏：本分册覆盖的屏**一致上报 `isRoot = false`** —— `HomeSearchScreen.kt:87`、`StrategyScreen.kt:51`、`LearningReportScreen.kt:66`、`AllServicesScreen.kt:59`、`LedgerScreens.kt:53` / `:87`、`HomeDetailScreens.kt:57` / `:130` / `:210`；`HomeRouteHost.kt` 内私有屏同样上报 false（见下节）。唯一 `isRoot = true` 是 `HomeScreen.kt:114` 的 `HomeRootContent`。**为什么 false**：这些屏都在二级栈，Shell 的 `ImmersiveInsets.shellContentInsets` 需按 `bottomBarVisible = false` 走 `navigationBarsPadding()`，若误报 true 会重复叠一层 `MainBottomBar` 高度的 padding。

##### 逐屏要点（有业务动作的才展开）

以下行号均在 `feature/home/src/commonMain/kotlin/com.example.my_kmp_project/feature/home/HomeRouteHost.kt`。

`LifeServiceScreen(onBack)`（`:336-530`）：函数内声明 **3 个局部 data class**（`LifeShortcut`/`LifeDaily`/`LifeCourse`，L339-347），数据是函数体 `listOf(...)` 字面量（L348-362，含 picsum URL）。**所有点击都是 `showPlatformToast(...)`**：快捷入口 L382、每日推荐行 L410、课程卡 L462、底部「进入跟读视频专区」L525 —— 专区按钮本该跳 `HomeRoutes.TransliterFeed` 却只 Toast（占位）。网络图走 `core.ui.PlatformNetworkImage`（L435、L465，`placeholder = Res.drawable.community_avatar / community_post_a`）。`ReportMainTabRoot(isRoot = false)` 在 L364，顶栏 `FlutterFeatureTopBar(title = "生活服务")` L371。

`LifeSectionHeader(title)`（`:532-544`）：左标题 17sp + 右「更多 >」文本（L542）——**不可点**。

`LiveCommerceScreen(onBack, onJoin = {})`（`:546-550`）与 `ClubScreen(onBack, onJoin = {})`（`:552-555`）：两个单行薄壳，分别 `ClubContentBody(title = "直播带货")` / `(title = "Club")`；注释 L548 说明 Flutter 侧直播带货本就复用 `HomeClubTabContent`。

`ClubContentBody(title, onBack, onJoin = {})`（`:611-787`）：状态仅 `var filter by remember { mutableStateOf(0) }`（L614），filters = 最新/嘉宾分享/资料（L615），**切 filter 不过滤 posts**（posts 是 L623-637 写死的 2 条函数内 `ClubPost`，L716 无条件渲染）。唯一真跳转：**「+ 加入」`clickable(onClick = onJoin)`**（L680），宿主注入 `onNavigate(AppRoutePath.community)`（L137/L141）。底部「进入社区查看更多」只 Toast「社区」（L782）。PDF 附件行（L744-758）与 ↗/💬/♡ 操作行（L760-768）**全部不可点**。L646 注释：`Spacer(1.dp)` 为对齐 Flutter gate（mse ≤2%）。

`CheckInMallScreen(onBack)`（`:789-1114`）：**本分册唯一读写 KV store 的屏**。L791 注释「bound to HomePointsStore (mock PointsApi)」；L794 读 `HomePointsStore.version` 建立订阅，随后 `balance`/`streak`/`checkedInToday`（L795-797）、`todayReward`（L808）。本地态 `remind`（L798）、`checkingIn`（L799）。日历 `DayCell` 8 格（L800-809）：前 7 格 `signed = false` 写死，今天一格 `reward = todayReward` + `signed = checkedToday`。任务 `TaskRow` 3 条（L811-815，图标是 emoji）。**副作用**：签到按钮 `clickable(enabled = !checkedToday && !checkingIn)` → `checkingIn = true` → `HomePointsStore.checkIn()` → 复位 → Toast「签到成功，+N积分」/「今日已签到」（L926-937）；任务「领取」再调一次 `checkIn()`（L1070-1077），「去完成」仅 Toast（L1078）；「签到提醒」开关只翻本地 `remind`（L994），**不持久化、不注册通知**；「积分换礼」是空态「暂无积分商品」（L1089-1111）。蓝头与白身用两次 `offset(y = (-18).dp)`（L831/L885）做重叠，注释 L830 记录调参（−28 过头、−18 对齐）。

`TransliterFeedScreen(onBack, onOpenHotRank)`（`:1116-1634`）：`ReportMainTabRoot(false)` L1168。头部**不用 `FlutterFeatureTopBar`**，自绘带 `statusBarsPadding()` 的「返回 + 搜索胶囊 + 历史 + 通知」行（L1170-1220，注释 L1170「Flutter AppBar, not MineTopBar title-only」）。跳转能力只有一条：**热榜入口 `clickable(onClick = onOpenHotRank)`（L1523）→ 宿主 `onNavigate(HomeRoutes.HotRankDetail)`（L131）**。其余全 Toast：搜索 L1201、历史 L1214、通知 L1218、分类 chip L1254、菜单 L1285、功能位 L1355-1357、最近在学 L1380、换一换 L1422。榜单缩略用 `HomeMockData.rankItemsForTab(0).take(3)`（L1528）——**写死取 tab 0**。封面大量用 `Res.drawable.home_transliter_home_cover_01..12`（import L77-88）与 `home_transliter_home_thumb_default`（L1462）。

`TransliterCoverWithDuration(cover: DrawableResource, duration: String, modifier: Modifier = Modifier)`（`:1636-1669`）：参数注入，8dp 圆角（注释 L1643 对齐 Flutter `thumbRadius`）+ 右下角 `home_transliter_home_icon_duration` 时长角标（L1660）。无交互。

`TransliterHistoryClockIcon(modifier: Modifier = Modifier, tint: Color = DemoColors.TextPrimary)`（`:1671-1693`）：Canvas 手绘时钟，无交互。

`TransliterSectionTitle(title: String, subtitle: String? = null, action: String? = null, onAction: (() -> Unit)? = null, trailing: (@Composable () -> Unit)? = null)`（`:1695-1726`）：**两个 lambda 槽** —— `onAction` 普通回调（`action != null` 时渲染文本 `clickable { onAction?.invoke() }`，L1722；`onAction` 传 null 时点击是 no-op）+ `trailing` Composable slot。

`HotRankDetailScreen(onBack)`（`:1728-1989`）：数据 `HomeMockData.hotRankCategories` / `hotRankAgeFilters`（L1731-1732）；`categoryIndex`/`ageIndex` 用 `mutableIntStateOf`，**初值靠 `indexOf("热搜榜").coerceAtLeast(0)` 反查**（L1734-1735，Mock 里没这个字符串时静默回落 0）；`showAgeMenu`（L1736）；`items = remember(categoryIndex) { HomeMockData.hotRankDetailItems(categoryIndex) }`（L1737）。结构：遮罩层 `clickable(enabled = showAgeMenu) { showAgeMenu = false }`（L1751）+ `statusBarsPadding()`（L1759）+ 分享按钮 **Toast「分享（mock）」**（L1783）+ `verticalScroll` 分类条（L1822/L1839）+ 年龄下拉（L1889/L1907）+ **`LazyColumn` 榜单**（L1918，行点击 `showPlatformToast(item.title)` L1930）。

`analyticsUvLabel(uv: Int): String`（`:1991`）与 `analyticsRoiLabel(roi: Double): String`（`:2005-2010`）：非 Composable 纯格式化。

`AnalyticsListScreen(onBack, onItem: (AnalyticsRecordRow) -> Unit)`（`:2012-2162`）：`ReportMainTabRoot(false)` L2027，顶栏 `FlutterFeatureTopBar(title = "数据分析")` L2029。注释 L2016 关键：**「summary + ring/bars tiles (Canvas ≈ wys_chart; CPF 无 Vico/KoalaPlot)」** —— 图表全手绘 Canvas；5 个局部色常量 L2017-2021。数据是 `HomeSecondaryMock.analyticsRecords`（10 条）与 `analyticsTotal = 48`；行点击 → `onItem` → `onNavigate(HomeRoutes.DataAnalyticsDetail)`（L166-169）。

`NewCarFollowListScreen(onBack, onItem: (NewCarFollowRow) -> Unit, onCreate)`（`:2164-2357`）：`tabs = listOf("全部","高意向","中意向","低意向","逾期")`（L2179）+ `var tab by remember { mutableStateOf("全部") }`（L2175）；数据 **`run { HomeSecondaryStore.version; HomeSecondaryStore.newCarFollows() }`**（L2177-2178，读 `version` 这个 `mutableIntStateOf` 建立订阅，新建后自动刷新）。顶栏 `MineTopBar(title = "新车跟进", containerColor = Color.White)`（L2194）。局部色 L2171-2173。行 → `selectNewCarFollow(row.id)` + `onNavigate(NewCarFollowDetail)`（L194-197）；新建 → `onNavigate(NewCarFollowCreate)`（L198）。

`UsedCarCreateScreen(onBack)`（`:2359-2462`）：8 个 `remember` 字段（L2365-2372），`customer` 初值「张先生 138****2101」；`kinds` = 置换/专卖/收车 三对 label→英文码（L2364，**英文码从未使用**，落库的是中文 `kind`）。客户行点击在张/李两值间来回切（L2405-2407，**假选择器**）。**提交（L2425-2457）真实改内存**：`toIntOrNull()` / 金额退路 `toDoubleOrNull()?.toInt()`（L2427-2429）→ `HomeSecondaryStore.validateUsedCarCreate(...)`（L2432）→ 有错 `showPlatformToast(err)`（L2442）；无错 `createUsedCar(...)`（L2444，参数处用 `mileageInt!!`/`yearInt!!`/`amountInt!!` 非空断言 L2448-2450，**安全性依赖 validate 先拦住**）→ Toast「提交成功」→ `onBack()`。**无网络请求**；`HomeSecondaryStore` 是内存 `mutableListOf` + `bump()`（`HomeSecondaryStore.kt:13-31`），**进程重启即丢**。

`UsedCarField(label: String, value: String, onChange: (String) -> Unit)`（`:2464-2473`）：受控 `OutlinedTextField`，`label = { Text(label) }`、`singleLine`。

`UsedCarDetailScreen(order: UsedCarOrderRow, onBack)`（`:2475-2549`）：`ReportMainTabRoot(false)` L2480 + `MineTopBar(title = "业务单详情")` L2482，只读展示；`UsedCarSection`（`:2551-2575`）做 key-value 分组。

`UsedCarListScreen(onBack, onItem: (UsedCarOrderRow) -> Unit, onCreate)`（`:2576-2855`）：局部色 L2581-2583；数据 `run { HomeSecondaryStore.version; HomeSecondaryStore.usedCarOrders() }`（L2587-2588）；筛选态 `status`（初值「全部」）与 `kind`（初值「全部类型」）L2592-2593；顶栏 `FlutterFeatureTopBar`（L2608）；行 → `onItem`（宿主先 `selectUsedCar` 再导航，L145-148）。

`AfterSalesCreateScreen(onBack)`（`:2857-2921`）：7 个 `remember` 字段（L2860-2866），**`date` 初值写死 `"2026-09-25"`**（L2867，不读系统时钟）。提交走 `HomeSecondaryStore.validateAfterSalesCreate`（L2894）→ `createAfterSales`（L2904）。

`AfterSalesDetailScreen(row: HomeListRow, onBack)`（`:2923-2940`）：**旧轻量详情**，吃 2 字段的 `HomeListRow`；路由实际用的是 `HomeDetailScreens.kt` 的 `AfterSalesServiceDetailScreen`（`HomeRouteHost.kt:187`）。`grep -n "AfterSalesDetailScreen(" HomeRouteHost.kt` 只命中定义行 → **未接线的遗留代码**。同情况还有 `NewCarFollowDetailScreen(row: NewCarFollowRow, onBack)`（`:3011-3031`，路由走 `NewCarFollowArchiveDetailScreen`，L201）。

`AfterSalesListScreen(onBack, onItem: (AfterSalesDetailRow) -> Unit, onCreate)`（`:3033-3188`）：`val canCreate = HomeSecondaryStore.afterSalesCanCreate`（L3040，注释 L3039「Logic-first: advisor demo canCreate=true (Flutter can_create for store staff)」）+ `run { version; afterSalesDetails() }`（L3042-3043）；`canCreate == false` 时不渲染新建入口。

`PartnerPendingScreen(onBack)`（`:3190-3268`）：**本模块唯一有真实状态流转的 Todo 屏**。数据 `run { HomeTodoStore.version; HomeTodoStore.pendingApplications() }`（L3193-3194）。计数文案「N 位新成员」/「暂无待审申请」（L3200）+ 空态「有人申请入店后会显示在这里」（L3207）。**「拒绝」→ `HomeTodoStore.rejectJoin(app.applicationId)`，返回 true 才 Toast「已拒绝 X」（L3240-3245）；「确认入店」→ `approveJoin(...)` + Toast「已确认 X」（L3256-3260）**。store 侧只对 `isPending` 项生效并 `bump()` version（`HomeTodoStore.kt:59-74`），列表与首页 `HomeTodoCardStrip` 计数（`HomeTodoStore.kt:76 todoCards()`）同步刷新。卡片字段：`displayName · roleLabel` + 「申请于 createdAtLabel」（L3220-3229）。

`FollowUpCustomersScreen(onBack)`（`:3270-3281`）/ `AfterSalesAppointmentsScreen(onBack)`（`:3283-3294`）/ `StoreReviewOrdersScreen(onBack)`（`:3296-3306`）：**三个只带 `onBack` 的单行薄壳**，各自把**函数内写死的 `Triple` 列表**塞给 `TodoListScreen`（如 L3276「陈先生 / 意向凯美瑞 · 未回访 2 天 / 紧急」、L3289「10:30 · 保养 / 京 A·88888 · 技师小刘 / null」、L3302「订单 #90821 / 定金 ¥5000 · 待店长审 / 待审」）。**它们不读 `HomeSecondaryMock.followUps/appointments/orders`** —— 那三个成员全仓引用数为 0（见「真实状态与坑」）。行不可点。

`TodoListScreen(title: String, subtitle: String, rows: List<Triple<String, String, String?>>, onBack: () -> Unit)`（`:3308-3353`）：`ReportMainTabRoot(false)` L3315 + `MineTopBar(title, containerColor = DemoColors.PageBg)` L3317 + subtitle 文本 L3318-3323；`rows` 用 `Triple.first/second/third`，**`third` 非空才渲染 Accent 10% 角标**（L3339-3349）。整卡无 clickable。

`TodoForbiddenScreen(title: String, onBack: () -> Unit)`（`:3355-3411`）：🔒 emoji（L3378）+「暂无查看权限」+ 多行说明（L3389 内嵌 `\n`）+「返回」按钮 `clickable(onClick = onBack)`（L3405）。**全模块无调用点**（`grep -n "TodoForbiddenScreen(" HomeRouteHost.kt` 只命中 L3356）→ 权限态尚未接入任何路由。

`TodoEmpty(title: String, subtitle: String)`（`:3413-3422`）：居中两行文本，无 modifier、**无调用点**。

`TodoCard(avatarLabel, title, subtitle, badge: String? = null, badgeColor: Color = DemoColors.Accent, avatarTint: Color = DemoColors.Accent.copy(alpha = 0.12f), avatarFg: Color = DemoColors.Accent, footer: (@Composable () -> Unit)? = null)`（`:3424-3478`）：**8 参数、4 个默认色值、1 个 Composable slot**（`footer()` 在 L3475 调用）；头像用 `avatarLabel.take(1)` 首字（L3452）而非图片。**无调用点**（首页待办卡用的是另一文件 `HomeTodoCardStrip.kt`）。

`CrudListScreen(title: String, items: List<HomeListRow>, onBack: () -> Unit, onItem: ((HomeListRow) -> Unit)? = null, onCreate: (() -> Unit)? = null)`（`:3480-3527`）：**两个可空回调**决定渲染：`onCreate != null` 才在 `MineTopBar(actions = { TextButton "新建" })` 出现新建按钮（L3494-3500）；行是 `clickable { onItem?.invoke(row) }`（L3514），传 null 即 no-op。空列表 → 「暂无数据」（L3502-3505）；非空 → `LazyColumn(contentPadding = PaddingValues(16.dp))`（L3507）。**无调用点**。

`CrudDetailScreen(title: String, body: Any, onBack: () -> Unit)`（`:3529-3545`）：`body` 声明为 **`Any`** —— `is HomeListRow` 拼「title\nsubtitle」，else `body.toString()`（L3531-3534）。**全模块未知路由的唯一兜底**（L206 传 `"未识别路由"`、`"route=$route"`）。

`CrudCreateScreen(title: String, onBack: () -> Unit, onSubmit: () -> Unit)`（`:3547-3574`）：单字段 `OutlinedTextField(label = "标题")`（L3554-3559）+ `Button(colors = ButtonDefaults.buttonColors(containerColor = DemoColors.Primary))`（L3561-3571）：空白 → Toast「请填写标题」，否则 `onSubmit()`。**无调用点**。

> 死代码核验命令：`grep -n "<name>(" feature/home/src/commonMain/kotlin/com.example.my_kmp_project/feature/home/HomeRouteHost.kt` —— 只命中定义行即判定未接线（`AfterSalesDetailScreen`、`NewCarFollowDetailScreen`、`TodoForbiddenScreen`、`TodoEmpty`、`TodoCard`、`CrudListScreen`、`CrudCreateScreen`；`searchRankCoverRes` 在 `HomeSearchScreen.kt:73`）。

### 首页 → 二级页跳转链路

**链路模型（4 步，全模块统一）**：首页 tab 上的控件**从不直接持有 navigator**，只把「路由常量或中文标签」交给 `HomeScreen.kt:45` 的局部 `fun go(raw: String)`；`go` 用 `ProductRouteDispatch.resolve(raw)`（`core/router/src/commonMain/kotlin/com.example.my_kmp_project/core/router/ProductRouteDispatch.kt:62`）归一成 `ProductRouteTarget(path, host, toastMessage)`，再按 host 走两条出口之一：

1. **交给 Shell 的 `LocalAppNavigator`**（跨 tab 全屏能力）：`Web → AppRoute.InAppWeb(OfflineWebFixtureUrl)`（`HomeScreen.kt:49`）、`Scan → AppRoute.Scan`（`:50`）。Auth/Tab/Content 三类里，前两类只 Toast（`:61`/`:62`），Content 走 `navigator.navigate(AppRoute.Friend/Live/Classroom/Media)`（`:51-55`）。
2. **落到本地单层栈** `destination`（`HomeScreen.kt:42`）→ 由 `HomeScreen.kt:67-104` 的 `when` 按 `ProductRouteDispatch.hostForPath(dest)`（`:112` 定义）渲染 Home / Content / Mine / 兜底 Home 四类宿主。

> `destination` 只有一层：`onBack = { destination = null }`（`HomeScreen.kt:74/84/89/94/99`）永远回首页，二级页里再 `onNavigate` 会**替换**当前二级页而不是压栈。Android native 壳是另一套多栈实现（`NativeAndroidMain.kt:563` 的 `popHomeRoute()`），行为不同。

#### 表 5-1 · 首页 tab 上的入口（文件均在 `feature/home/src/commonMain/kotlin/com.example.my_kmp_project/feature/home/`）

| # | 入口控件 : 行号 | 回调参数 | `onNavigate` 实参 | `go` 解析结果 | 最终渲染 |
|---|---|---|---|---|---|
| 1 | `HomeSearchBar` `HomeScreen.kt:197` | `onSearch` | `HomeRoutes.Search` = `/home/search`（`FeatureRoutes.kt:18`） | host Home（`ProductRouteDispatch.kt:123` `p.startsWith("/home/")`） | `HomeRouteHost.kt:122` → `HomeSearchScreen` |
| 2 | `HomeSearchBar` `:198` | `onScan` | 字面量 `"扫一扫"` | 别名表 `ProductRouteDispatch.kt:95-96` → `/scan` + host Scan | **不走 destination**：`navigator.navigate(AppRoute.Scan)` `HomeScreen.kt:50` |
| 3 | `HomeFeatureGrid` `:205` | `onFeature(label)` | `HomeMockData.features` 的标签原文 | `HomeRoutes.fromLabel`（`FeatureRoutes.kt:48-75`）；未命中 → host Unknown → `destination = raw`（`:63`） | 命中 → 对应屏；未命中 → `HomeRouteHost.kt:206` `CrudDetailScreen("未识别路由")` |
| 4 | `HomeTodoCardStrip` `:212-216` | `onOpen(card)` | `card.actionRoute.ifBlank { HomeRoutes.fromLabel(card.title) ?: card.title }` | 四条待办路由已在 store 里预置（`HomeTodoStore.kt:84/92/100/108`） | `HomeRouteHost.kt:174/175/176/177` → `PartnerPendingScreen` / `FollowUpCustomersScreen` / `AfterSalesAppointmentsScreen` / `StoreReviewOrdersScreen` |
| 5 | `HomeStoreMetricsCard` `:237` | `onOpenLedger` | `HomeRoutes.Ledger` = `/home/ledger`（`FeatureRoutes.kt:30`） | host Home | `HomeRouteHost.kt:157` → `LedgerListScreen` |
| 6 | `HomeStoreMetricsCard` `:233-235` | `onStoreTap` | 已登录：`showSwitchStore = true`（**不出模块**，`HomeScreen.kt:147` 弹 `CrossFeatureScreens.SwitchStoreDialog`）；未登录：`"请先登录"` | `"请先登录"` 在 `toastOnlyLabels`（`ProductRouteDispatch.kt:33-38`，命中 `:35`）→ host Toast（`:62`） | `HomeScreen.kt:48` `showPlatformToast` |
| 7 | `HomeHubEntryCard`（投资策略）`:245` | `onClick` | `HomeRoutes.Strategy` | host Home | `HomeRouteHost.kt:123` → `StrategyScreen` |
| 8 | `HomeServiceGrid` `:252-253`（仅 `label == "更多" \\|\\| label == "全部"`）与 `:255` `onOpenAll` | `onService` / `onOpenAll` | `HomeRoutes.AllServices` = `/home/all_services`，与 `AppRoutePath.homeAllServices`（`AppRoutePath.kt:13`）同值 | host AllServices（`ProductRouteDispatch.kt:92/115`）→ `destination = "services"` **魔法字符串**（`HomeScreen.kt:58`） | `HomeScreen.kt:73-79` **直接挂 `AllServicesScreen`，绕过 `HomeRouteHost`** |
| 9 | `HomeContactList` `:258` | `onContact = {}` | — | — | **空 lambda，永远不跳** |
| 10 | `HomeNewsList` `:259` | `onOpenAll = {}` | — | — | **空 lambda，永远不跳** |
| 11 | `HomeHubEntryCard`（学习报告）`:265` | `onClick` | `HomeRoutes.LearningReport` | host Home | `HomeRouteHost.kt:124-127` → `LearningReportScreen` |

#### 表 5-2 · `AllServicesScreen` 的回填跳转

`HomeScreen.kt:75-78` 注入 `onOpen = { label -> destination = null; go(label) }` —— **先回首页再解析**，所以「全部服务」页点格子会让 LazyColumn 重建（`showTodos` 的 50ms 延迟 `HomeScreen.kt:124-127` 会重来一次）。标签 → 路由同样只靠 `fromLabel` 表；格子自身 `AllServicesScreen.kt:94/108` 的 `if (!isEditing) onOpen(it.label)` 保证编辑态不跳。

`AppShell.kt:426` 的另一处挂载 `AllServicesScreen(onBack = { clearShellRoute() })` **不传 onOpen**（默认 `{}`，`AllServicesScreen.kt:54`）→ 该路径下所有格子点击无效。

#### 表 5-3 · 二级页之间（`HomeRouteHost.kt` 内部 `onNavigate`，全部 12 处）

| 起点 : 行 | `onNavigate` 实参 | 解析后 host | 落点 |
|---|---|---|---|
| `LearningReportScreen` 会员横幅 `HomeRouteHost.kt:126` | **`"/pay/membership"` 硬编码字符串**（未用 `MineRoutes` 常量，值同 `FeatureRoutes.kt:85`） | Mine（`ProductRouteDispatch.kt:126`）→ `destination` 换栈 | `HomeScreen.kt:92-96` `CrossFeatureScreens.MineRouteScreen` |
| `TransliterFeedScreen` 热榜行 `:131` | `HomeRoutes.HotRankDetail` | Home | `HomeRouteHost.kt:133` `HotRankDetailScreen`（**替换**当前跟读页） |
| `LiveCommerceScreen` / `ClubScreen` 「+ 加入」`:137` / `:141` | `AppRoutePath.community` = `/community`（`AppRoutePath.kt:45`） | **Tab**（`ProductRouteDispatch.kt:122`，`p == AppRoutePath.community`） | `HomeScreen.kt:61` `showPlatformToast("请切换底部 Tab")` —— **不会真的切 tab** |
| `UsedCarListScreen` 行 `:147`（先 `:146` `HomeSecondaryStore.selectUsedCar(order.id)`） | `HomeRoutes.UsedCarDetail` | Home | `HomeRouteHost.kt:151-155` `UsedCarDetailScreen`（详情读 `selectedUsedCarId`） |
| `UsedCarListScreen` 新建 `:149` | `HomeRoutes.UsedCarCreate` | Home | `:156` `UsedCarCreateScreen` |
| `LedgerListScreen` 行 `:160` | `HomeRoutes.LedgerDetail` | Home | `:162-165` `LedgerDetailScreen` —— **只传 `ledger.firstOrNull()`，无选中态** |
| `AnalyticsListScreen` 行 `:168` | `HomeRoutes.DataAnalyticsDetail` | Home | `:170-173` `AnalyticsDetailScreen` —— 同上，恒 `analyticsRecords.first()` |
| `AfterSalesListScreen` 行 `:182`（先 `:181` `selectAfterSales`） | `HomeRoutes.AfterSalesDetail` | Home | `:187-191` `AfterSalesServiceDetailScreen` |
| `AfterSalesListScreen` 新建 `:184` | `HomeRoutes.AfterSalesCreate` | Home | `:186` `AfterSalesCreateScreen` |
| `NewCarFollowListScreen` 行 `:196`（先 `:195` `selectNewCarFollow`） | `HomeRoutes.NewCarFollowDetail` | Home | `:201-205` `NewCarFollowArchiveDetailScreen` |
| `NewCarFollowListScreen` 新建 `:198` | `HomeRoutes.NewCarFollowCreate` | Home | `:200` `NewCarFollowCreateScreen` |

> 只有 UsedCar / AfterSales / NewCarFollow 三条链路走「`store.select(id)` → 详情读 `selectedXxxId` → 查不到退回首条」；Ledger / DataAnalytics 两条**缺选中态**，点任何一行都渲染第一条。

#### 表 5-4 · 看起来能跳、其实断掉的入口

| 位置 | 现象 |
|---|---|
| `HomeSections.kt:138`（`HomeGreetingSection`「3 条新消息」pill） | 文本硬编码，pill **无可点 modifier** |
| `HomeSections.kt:218-284`（`HomeBannerSection`「立即体验」） | pill 只是 `Box` + 文本，**无 clickable** |
| `HomeSections.kt:576-609`（`DetailActionTile`） | 「详情」二字不可点，只有整卡 `onClick` |
| `HomeSections.kt:903-989`（`HomeNewsList`） | 行本身无 clickable，且宿主传 `onOpenAll = {}` |
| `HomeRouteHost.kt:525`（`LifeServiceScreen`「进入跟读视频专区」） | 应跳 `HomeRoutes.TransliterFeed`，实际只 `showPlatformToast` |
| `HomeRouteHost.kt:1201/1214/1218/1254/1285/1357/1380/1422`（`TransliterFeedScreen`） | 8 处 Toast；唯一真跳转是 `:1523` 的 `onOpenHotRank` |
| `StrategyScreen.kt:97-133`、`:295`、`:304` 附近 | 子 Tab 切换只改 `selectedTab` 不换内容；「订阅」是 `clickable { }` 空实现；「如何跟投」纯文本 |
| `HomeRouteHost.kt:782`（`ClubContentBody` 底部） | 「进入社区查看更多」只 Toast「社区」；真跳转只有 `:680` 的 `onJoin`，而 onJoin 又落到表 5-3 的 Tab-Toast |

### 沉浸式 / inset 合规

**结论：本模块一行 `ImmersiveInsets` 都不用。** 核验：`grep -rn "ImmersiveInsets\|shellContentInsets" feature/home/src/commonMain` → **0 命中**（`core/design/.../ImmersiveInsets.kt` 只在 `composeApp/.../AppShell.kt:42` 被 import）。底栏留白完全由 Shell 负责：

| 关注点 | 位置 | 说明 |
|---|---|---|
| 底栏高度常量 | `core/design/src/commonMain/kotlin/com.example.my_kmp_project/core/design/ImmersiveInsets.kt:23`（`MainBottomBarHeight = 49.dp`） | 本模块无引用 |
| inset 策略函数 | `ImmersiveInsets.kt:41-49` `Modifier.shellContentInsets(bottomBarVisible, bottomBarPadding)`：可见 → `padding(bottom=…)`，不可见 → `navigationBarsPadding()` | |
| 唯一调用点 | `composeApp/src/commonMain/kotlin/com.example.my_kmp_project/app/AppShell.kt:360-363`（`bottomBarVisible = showBottomBar`，定义 `:334-336`） | 首页与所有二级页共用这一个 Box 修饰符 |
| 可见性由谁决定 | `core/ui/.../MainTabChrome.kt:24-29` `ReportMainTabRoot(isRoot)` → `chrome.updateBottomBarVisible(isRoot)`（`:16-17` 是 `mutableStateOf(true)`） | 所以**本模块各屏只负责上报，不负责算 padding** |
| 上报值 | 首页根 `HomeScreen.kt:114` = `isRoot = true`，**其余 30 处一律 `false`**：`grep -rn "ReportMainTabRoot(isRoot" feature/home/src/commonMain` → 31 行调用（+8 行 import = 39 行命中）；`HomeRouteHost.kt` 独占 21 处 | **没有漏报屏**：只有 5 个薄壳自己不报（`LiveCommerceScreen:547`、`ClubScreen:553` 由 `ClubContentBody:638` 报；`FollowUpCustomersScreen:3271`、`AfterSalesAppointmentsScreen:3284`、`StoreReviewOrdersScreen:3297` 由 `TodoListScreen:3315` 报） |

**有没有裸 `TopAppBar`？没有。** 核验：`grep -rn "TopAppBar" feature/home/src/commonMain` 只命中 1 行注释 —— `AllServicesScreen.kt:115`「56.dp toolbar (not M3 64.dp CenterAlignedTopAppBar)」。本模块三种顶栏写法：

| 写法 | 位置 | 状态栏处理 |
|---|---|---|
| 自绘条 `AllServicesTopBar` | `AllServicesScreen.kt:115-151`（`statusBarsPadding()` `:122`，高 `FlutterToolbarHeight = 56.dp` `:127`，返回图标是 `ic_nav_back` `rotate(180f)` `:130-139`） | ✅ |
| 自绘条 `FlutterFeatureTopBar` | `HomeRouteHost.kt:559-609`（`statusBarsPadding()` `:570`） | ✅ |
| 复用 `core.ui.MineTopBar` | `LedgerScreens.kt:59` / `:84`、`HomeDetailScreens.kt:64` / `:136` / `:210`、`HomeRouteHost.kt:2194` / `:2482` / `:3317` / `:3490` | 由 core.ui 内部处理 |
| 搜索页自绘头 | `HomeSearchScreen.kt:167-271`（`statusBarsPadding()` `:180`） | ✅ |
| 其余 `statusBarsPadding()` | `HomeScreen.kt:190`（LazyColumn）、`HomeRouteHost.kt:1175`（跟读页头）、`:1759`（热榜页头） | ✅ |
| 悬浮件自带 | `LearningReportScreen.kt:130`（家长助手 chip）、`:147`（`MembershipBanner`）用 `navigationBarsPadding()` —— **全模块仅这两处** | 与 Shell 的 `navigationBarsPadding()` 叠加，见下 |

> ⚠️ **与 AGENTS.md 的张力**：AGENTS.md「Immersive / edge-to-edge」写明*不要在 DemoApp tab 内容外再包 `statusBarsPadding()`*（安全区由 Shell 统一给），但 `HomeScreen.kt:190` 的 `LazyColumn` 上就挂了 `.statusBarsPadding()`，`:185-187` 的注释解释是对齐 Flutter `safeTop + 16` 的刻意行为。`AllServicesScreen` / `HomeSearchScreen` / `HomeRouteHost` 三处自绘条同理（它们都在二级栈，Shell 只加了 `navigationBarsPadding()`，状态栏确实得自己躲）。**底栏方向不会重复**：二级页 `ReportMainTabRoot(false)` → `showBottomBar = false` → Shell 走 `navigationBarsPadding()` 分支（`ImmersiveInsets.kt:41-49`），不再叠 49.dp。
>
> ⚠️ **会重复的方向**：`LearningReportScreen.kt:130/147` 的 `navigationBarsPadding()` 叠在 Shell 已给的 `navigationBarsPadding()` 之上，二级页底部悬浮件在带手势条的设备上会被顶起两个 home-indicator 高度。

### 真实状态与坑

#### 数据源分类（`grep -rn "HomeMockData\." feature/home/src/commonMain/.../feature/home` 全量 32 行命中（含 `HomeFavoritesStore.kt` 的 5 处，屏层 27 处））

| 屏 | 数据来源 | 行号 | 可写吗 |
|---|---|---|---|
| 首页根 `HomeRootContent` | **`HomeMockData` 10 处**：`storeName` `HomeScreen.kt:118`、`searchPlaceholder` `:196`、`features` `:204`、`metricsYesterday/Month/Today` `:227-229`、`metricDetails` `:231`、`services` `:250`、`contacts` `:258`、`news` `:259` | 全部只读；唯一"可变"的是 `storeName` 被 `SwitchStoreDialog.onPicked` 改本地 state（`:151`），**门店列表本身仍来自 Mine 侧 mock** | ❌ |
| 首页 sections | `HomeSections.kt` **0 处** `HomeMockData` —— 数据全由参数注入 | — | ❌（受控组件） |
| `HomeSearchScreen` | 6 处（`:90/91/92/94/135/140`）：历史、发现、榜单、筛选标签、Tab 名 | 历史写进 `remember` 的本地 `history`（`:109`），**退出即丢**，无 KV | 会话内 |
| `AllServicesScreen` | 1 处（`:97` `catalogSections`） | 收藏是唯一真持久化：`HomeFavoritesStore.add/remove` + `snapshot()`（`:90/91/104/105`，底层 `core.platform.saveStringList`，key `home_favorite_service_ids_v2`） | ✅ KV |
| `StrategyScreen` | 3 处（`:82/157/178`）+ `StrategyPlanCard` 整卡写死字面量（`StrategyScreen.kt:219-315`，含 `"-11.35%"` `:264`） | — | ❌ |
| `LearningReportScreen` | 3 处（`:98/100/120`） | — | ❌ |
| `HomeRouteHost` 私有屏 | 4 处，只在跟读/热榜：`:1528`（`rankItemsForTab(0).take(3)`）、`:1731/1732`（分类与年龄筛选）、`:1737`（榜单条目） | — | ❌ |
| 二手车 / 售后 / 新车跟进 | **`HomeSecondaryStore`（内存 `mutableListOf` + `bump()`，无 KV）**：`usedCarOrders()` `:2587-2588`、`afterSalesDetails()` `:3042-3043`、`newCarFollows()` `:2177-2178`；新建走 `validateXxxCreate` + `createXxx`（`:2432/2444`、`:2894/2904`、`:2988/2995`） | 表单真校验、真插入列表 | 进程内 |
| 待办 4 屏 | 伙伴审核用 **`HomeTodoStore`**：`pendingApplications()` `:3193-3194`，`rejectJoin` `:3241` / `approveJoin` `:3257` 真删除条目 | 同样进程内 | 进程内 |
| 签到 / 积分 | **`HomePointsStore`（KV 落盘的积分态）**：`version` `:794`、`balance/streak/checkedInToday/todayReward` `:795-808`、`checkIn()` `:928` 与「领取」`:1071`；首页侧 `HomeScreen.kt:122/135-138/162-166/173`（`markDialogAcked`） | `checkIn()` 返回 `res.points` 供 Toast 文案（`HomeScreen.kt:166-169`），失败/重复签到返回 null 静默 | ✅ KV |

#### 坑（按影响面排序）

| # | 坑 | 证据 |
|---|---|---|
| 1 | **12 个文件里没有任何网络层**：`grep -rn "kotlinx.coroutines.flow\\|ViewModel\\|collectAsState\\|core.network\\|feature.home.api" feature/home/src/commonMain/.../feature/home/*.kt` → **0 命中**。整个 feature 的 `api/` 子包（`HomeTodoApiModels.kt` 等）只被 store 消费，屏层拿不到，页面数据 100% mock 或内存 store | 上面的 grep |
| 2 | `HomeSecondaryMock` **只有 3 个成员真的在用**：`ledger`（`HomeRouteHost.kt:158/163`）、`analyticsRecords`（`:171/2025`）、`analyticsTotal`（`:2026`）。`partners/followUps/appointments/orders/analytics/usedCars/afterSales/newCars` 以及两个代理 getter 全仓 0 引用：`grep -rn "HomeSecondaryMock" --include="*.kt" . \\| grep -v "/build/"` 共 7 行 = 5 处引用 + 定义行 + 对象声明。四个 Todo 屏的真实数据见上表 | 该 grep |
| 3 | **`DeferredDestinationStub` 在本模块 0 引用**：`grep -rn "DeferredDestinationStub" feature/home/src` 无命中（定义在 `feature/shell/.../DeferredDestinationStub.kt:25`）。本模块的"占位"风格不是它，而是 **Toast-only 屏**（`LifeServiceScreen` 全 4 处 Toast、`TransliterFeedScreen` 8 处、`ClubContentBody` 底部）与 **`CrudDetailScreen("未识别路由")` 兜底**（`HomeRouteHost.kt:206`） | 该 grep |
| 4 | **详情选不中**：`LedgerDetailScreen` 恒收 `ledger.firstOrNull()`（`:163`）、`AnalyticsDetailScreen` 恒收 `analyticsRecords.first()`（`:171`，**空列表会 `NoSuchElementException`**，`HomeSecondaryMock.ledger`/`analyticsRecords` 目前硬编码非空才没炸） | `HomeRouteHost.kt:163/171` |
| 5 | **`store.select` 是全局单例态**：`selectedUsedCarId` 等 3 个字段挂在 object 上（`HomeSecondaryStore.kt`），同一进程内 Mine/Home 混用会串；且 `?: xxx().first()` 的退回首条逻辑（`:153/189/203`）会**静默展示错单** | `HomeRouteHost.kt:152-153/188-189/202-203` |
| 6 | `DailyCheckInDialog.busy`（`DailyCheckInDialog.kt:49`）置 true 后**自己永不复位**，靠父级在 `onCheckIn` 里 `showCheckIn = false` 卸载（`HomeScreen.kt:170`）。任何"点完不关弹窗"的宿主（如 `NativeJetpackHome.kt:24`）都会让 CTA 永久禁用 | `DailyCheckInDialog.kt:148-151` |
| 7 | 自动签到用 `LaunchedEffect` 的 key 里**直接读 object 属性**（`HomeScreen.kt:128` `HomePointsStore.checkedInToday` / `.dialogAckDate`）；订阅则靠"裸读 `version`"（`:122`）。漏读 `version` 的屏不会随积分变化重组 —— `HomeRouteHost.kt:794` 是正确示范，`CheckInMallScreen` 之外的屏（如首页头部）没有 | `HomeScreen.kt:122/128` |
| 8 | **`flutterStyleGreeting` 硬编码 UTC+8**（`HomeScreen.kt:276-287`），与 `Clock.System.now().toString().take(10)`（`:131/160`，UTC 日期）不同源：海外时区下「早上好」与「今日是否已签到」的日期判定会差一天 | `HomeScreen.kt:131/160/276-287` |
| 9 | `HomeWebHandlers.GetEnvironment` 无论哪端都返回 `"platform":"android"`（`HomeWebHandlers.kt:31-33`），`GetUserInfo` 用字符串拼接 JSON（`:34-43`）—— **昵称含引号即产出非法 JSON**，且没有任何转义 | `HomeWebHandlers.kt` |
| 10 | `HomeWebHandlers.register()` 全仓唯一调用点是 `composeApp/src/androidMain/.../NativeAndroidMain.kt:90`：**iOS/OHOS 的 H5 bridge 四条 handler 全未注册**，`registered` 的 `@Volatile` 单例保护（`HomeWebHandlers.kt:12`）在这里等于没用到 | 该 grep |
| 11 | `FlutterToolbarHeight = 56.dp` 在两个文件里**重复声明**：`AllServicesScreen.kt:52` 与 `HomeRouteHost.kt:112`（同为文件级 private，不会冲突但值改一处漏一处） | 两处定义 |
| 12 | `HomeServiceAssets.featureAt(index)`（`HomeServiceAssets.kt:89-96`）已 `@Deprecated` 且对负 index 无保护；替代品 `featureForLabel(label)`（`:75-87`）未命中时回落 `ic_service_default`，`fromFlutterFile` 未命中返回 null 让 `ServiceGridCell` 走首字兜底砖块（`AllServicesScreen.kt:273-288`） | `HomeServiceAssets.kt` |
| 13 | `HomeAssetIcon`（`HomeServiceAssets.kt:112-125`）**没有 `modifier` 参数**，尺寸只能靠 `size: Dp = 44.dp`，无法外部加 padding/background —— 与其他 composable「`modifier` 放最后」的约定不一致（`HomeSections.kt` 全量遵守） | 签名对比 |
| 14 | `HomeSearchScreen`：`rotatingHint` 其实是**固定下标** `getOrElse(2)`（`:90`，命名撒谎、不轮播）；`searchRankCoverRes`（`:73-82`）与 8 张 rank 图（`:58-65`）**全为死代码**，72dp 封面是手绘 `Canvas` 播放三角（`:529-550`）；`SearchTagChip(softFill = false)`（`:439`）的 `softFill` 分支**无调用点传 true**；`onClear` 只清内存历史（`:121`） | grep 见「逐文件详解」 |
| 15 | `NewCarFollowArchiveDetailScreen` 的「提交」把输入框清空 + Toast「已记录跟进」（`HomeDetailScreens.kt:303-309`），但**不会 append 到 `logs`**（`:204-209` 硬编码）—— 用户看不到自己刚写的跟进；改意向等级 chip 也只改 `level` 本地 state（`:254-276`），回列表即丢 | `HomeDetailScreens.kt` |
| 16 | `AfterSalesCreateScreen.date` 写死 `"2026-09-25"`（`HomeRouteHost.kt:2867`），新建售后单的日期永远同一天；`UsedCarCreateScreen.kinds`（`:2364`）把 code 算出来后**没有使用**（只用 label） | `HomeRouteHost.kt` |
| 17 | `HomeMockData.ledger` 的日期集中在 2026-09-22~24（`HomeRouteHost.kt:261-286`），任何"按日期分组/排序"的 UI 都会显示成同一周；`analyticsRecords` 里 AN-0004 是全 0 行，`AnalyticsDetailScreen` 的分母用 `coerceAtLeast(1)` 兜住（`HomeDetailScreens.kt:185`），`InvestProgressBar` 只 `coerceIn(0f,1f)` **没有 total=0 保护**（`StrategyScreen.kt:319-343`） | 相应行 |
| 18 | 三个薄壳屏（`FollowUpCustomersScreen` `:3270`、`AfterSalesAppointmentsScreen` `:3283`、`StoreReviewOrdersScreen` `:3296`）把**函数内 `Triple<String,String,String?>` 字面量**塞给 `TodoListScreen`（`:3275-3278/3288-3290/3301-3302`）；`TodoListScreen` 的行**完全不可点**，`third` 只当徽章色用（`:3339-3349`） | `HomeRouteHost.kt` |
| 19 | `MetricTile` 用 `DemoColors.CanvasSoft` 作底（`HomeSections.kt:552`），同卡片其他块用 `CardBg`/`Surface` —— 与 `HomeSectionStyle` 自己声明的 token 组不一致，暗色主题下会跳色 | `HomeSections.kt:547-574` |
| 20 | 死代码汇总（核验方式见「HomeRouteHost 专题」末尾命令）：`AfterSalesDetailScreen`（`:2923`）、`NewCarFollowDetailScreen`（`:3011`）、`TodoForbiddenScreen`（`:3355`）、`TodoEmpty`（`:3413`）、`TodoCard`（`:3424`）、`CrudListScreen`（`:3480`）、`CrudCreateScreen`（`:3547`）、`searchRankCoverRes`（`HomeSearchScreen.kt:73`）、`HomeServiceAssets.featureAt`（已标 Deprecated）、`ReportColors.PlayBg`（`LearningReportScreen.kt:39-59` 内未被引用） | grep |

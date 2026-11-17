# 17 · feature/mine + feature/shell 深度解析

> 适用仓库：`/Users/qpy/Desktop/github/my_kmp_project`（KMP + Compose Multiplatform，Android / iOS / HarmonyOS 三端）。
> 本文覆盖 `feature/mine`（commonMain 28 个 .kt + commonTest 8 个 .kt + `build.gradle.kts`）与 `feature/shell`（commonMain 5 个 .kt + commonTest 1 个 .kt + `build.gradle.kts`），所有行号截至当前工作区。
> 决策背景：`docs/adr/0002-native-shell-mine-compose-island.md`（原生壳 + Mine Compose Island，issue #18/#19/#23；其 §6 已被 ADR 0003 取代）。

**路径缩写**（正文引用均由此展开，仍可按缩写直接检索）：

| 缩写 | 仓库相对前缀 |
|---|---|
| `FM/` | `feature/mine/src/commonMain/kotlin/com.example.my_kmp_project/feature/mine/` |
| `FMt/` | `feature/mine/src/commonTest/kotlin/com.example.my_kmp_project/feature/mine/` |
| `FS/` | `feature/shell/src/commonMain/kotlin/com.example.my_kmp_project/feature/shell/` |
| `FSt/` | `feature/shell/src/commonTest/kotlin/com.example.my_kmp_project/feature/shell/` |

---

## 1. 两个模块的定位差异 + 依赖实证

| 维度 | feature/mine | feature/shell |
|---|---|---|
| 定位 | 「我的」业务域：Mine 首页内容、二级页路由表、二级页 UI、本地 mock 存储、Dart 契约层 HTTP API | 「壳的形状」而非业务域：主 Tab 底栏、二级 Compose 岛、Soft-Auth 呈现层、未排期路由统一占位（`feature/shell/build.gradle.kts:1-5` 头部注释原文） |
| 页面根 | Mine Tab 根在 Android/OHOS/iOS 均由原生壳承担；本模块只以 `MineIsland` / `MineRouteHost` 提供二级页（`FM/MineIsland.kt:13-16` KDoc，引用 ADR 0002 / #18/#19） | 不含任何业务页面，只含跨端复用的壳件；注释明确"三端壳都只依赖这一个形状，不再从 :composeApp 里捞内部符号"（`feature/shell/build.gradle.kts:3-5`） |
| 序列化 | 声明 `kotlinSerialization` 插件，仅 address/calculator/dealinvoice/usedcar 子包的 `@Serializable` 模型需要（`feature/mine/build.gradle.kts:6-7`） | 无序列化依赖（`feature/shell/build.gradle.kts:6-11`） |
| 网络 | `api(projects.core.network)`（`feature/mine/build.gradle.kts:16`）；且因 core.network 对 serialization 是 implementation 不传递，自备 `implementation(libs.kotlinx.serialization.json)`（`feature/mine/build.gradle.kts:18-20`） | 不依赖 core.network（`feature/shell/build.gradle.kts:15-19` 只有 core.ui / core.router / component） |
| commonTest | `implementation(projects.core.network)`（`feature/mine/build.gradle.kts:22-24`，为契约层测试准备） | 无额外测试依赖声明 |
| 公共依赖交集 | 两者都 `api(projects.core.ui)`、`api(projects.core.router)`、`api(projects.component)`（mine：`feature/mine/build.gradle.kts:13-17`；shell：`feature/shell/build.gradle.kts:16-18`）。core.ui 注释说明其传递带出 core.platform → core.design / core.account（`feature/mine/build.gradle.kts:13`） | 同左 |
| namespace | `com.example.my_kmp_project.feature.mine`（`feature/mine/build.gradle.kts:28`） | `com.example.my_kmp_project.feature.shell`（`feature/shell/build.gradle.kts:24`） |
| 相互关系 | mine 不依赖 shell，shell 也不依赖 mine —— 二者仅在上层 assembly（composeApp）里被同一壳装配使用（见 §4/§5 消费点） | 同左 |

> 注意一个依赖细节：`FS/SecondaryRouteIsland.kt:18` import 了 `component.routing.CrossFeatureScreens` 并在 L92 调 `MineRouteScreen`，从而间接渲染 mine 的 `MineRouteHost` —— 方向是 shell → component 端口 → assembly → mine，shell 对 mine 无直接编译依赖（`feature/shell/build.gradle.kts` 无 mine 条目）。

---

## 2. 文件清单表

### 2.1 feature/mine commonMain（28 个文件）

| 路径 | 作用 | 关键行号 |
|---|---|---|
| `FM/MineIsland.kt` | Mine Compose Island 根容器（原生壳内挂载的共享 CMP 岛），内部路由栈 | 签名 18-21；栈 22；`/mine/about` 特判 26-31；`MineRouteHost` 33-41 |
| `FM/MineScreen.kt` | 旧版全 CMP Mine Tab 根（LEGACY，仅剩 AppShell 一个消费者） | `go()` 36-46；登录后补跳 48-54；`ReportMainTabRoot(true)` 59 / `(false)` 76；跨模块渲染 81-90；"logout" 特判 103-106；Snackbar 1800ms 116-129 |
| `FM/MineModels.kt` | Mine 目录数据（写死的 Flutter demo 数据）与类型 | data class 7-39；`MineCatalog` 41；guestStats 42；demoStats 49；goldDemoProfile 57-63；quickServices 66-70；menuItems 73-81；functions 84-158；`profile()` 160-173 |
| `FM/MineRouteHost.kt` | Mine 二级页路由表 + 全部私有二级页（1643 行巨型文件） | 路由表 77-135；屏见 §3.3 |
| `FM/MineHomeContent.kt` | Mine Tab 首页内容（被原生壳嵌入共享渲染） | 常量 54-55；签名 58-69；prefs 观察 70；applyTap 83-89；换店对话框 104-114；折叠导航 182-192/233-276；ProfileCard 296；FunctionSection 497 |
| `FM/MineTapDispatch.kt` | 首页三区块点按分发（纯函数，按目录 id 不按中文标签） | sealed 结果 11-15；onQuickService 18-26；onFunction 28-40；onMenu 42-54；authOrNav 56-57 |
| `FM/MineTheme.kt` | mine 私有尺寸/颜色 token，桥接 core.design | object 14；Accent/Background 桥接 15-16 |
| `FM/MineComponents.kt` | 共享小组件（分组卡/行/开关/分隔/头像/角色徽章） | MineGroupedCard 39；MineSectionHeader 55；MineNavRow 65；MineSwitchRow 140；MineInsetDivider 196；MineAvatarPlaceholder 205；MineRoleBadge 224 |
| `FM/MineContactValidators.kt` | 姓名/手机号校验与掩码（Flutter 同款规则） | 常量 5-6；digitsOnly 8；isNameValid 10-13；isCnMobile 15-17；maskPhone 20-24 |
| `FM/MinePrefsStore.kt` | KV 持久化：当前店、功能排序、深色、语言 | key 17-20；version 22-27；switchStore 43-49；statsForStore 54-62；functionOrderIds 64-71；setDarkMode/setLocaleZh 85-95 |
| `FM/MineProfileLogic.kt` | 资料页保存/登出纯逻辑 | isDirty 8；validateSave 13；saveNickname 19-21；logout 23-25；sessionNickname 27-28；sessionPhoneMasked 30-32 |
| `FM/MineSettingsScreen.kt` | 设置页（环境/深色/语言）+ 关于页 | MineSettingsScreen 37；env 标签 45-48；语言对话框 50-90；环境内联切换 117-145；深色开关 147-155；蓝牙 toast 177；MineAboutScreen 186-217 |
| `FM/MinePersonalizedSettingsScreen.kt` | 个性化设置页（7 个开关/选项，全部落 KV） | 前缀 30；loadBool/persistBool 32-41；7 个 key 49-61；help() 65；装扮中心 82-83 |
| `FM/SwitchStoreDialog.kt` | 换店对话框（demo 店铺目录） | MineStoreOption 31；MineStoreCatalog 37-47；Dialog 60-62；点选中项即关闭 105；✕ 关闭 126-136 |
| `FM/AddressMockStore.kt` | 地址内存 mock（未持久化） | MineAddress 8-25；object 30；种子 41-63；save 98-145；delete 154；resetForTests 170 |
| `FM/WalletMockStore.kt` | 钱包内存 mock（未持久化） | 余额 52；种子卡 55-58；种子流水 59-61；recharge 82-99；bindCard 109；resetForTests 142 |
| `FM/DealInvoiceScreens.kt` | 新车成交两屏（SoT 整图 + 上传表单） | DealInvoiceDemoScreen 46；整图点击 68；onOpenDetail 保留未用 82-84；DealInvoiceUploadScreen 92；写死手机号 112-113；选图 gap 134；提交流程 142-143 |
| `FM/address/AddressApi.kt` | 地址契约层（Dart 对齐，#79-#83） | Paths 21-30；list 52；create 61；update 69；setDefault 77；delete 86；dataOrThrow 106-112 |
| `FM/address/AddressModels.kt` | 地址模型（11 可空字段 + toBridgeJson） | AddressModel 15 起；toBridgeJson 48-59；AddressBridgeRequest 67-78 |
| `FM/calculator/PurchaseCalculatorApi.kt` | 购车计算器契约层 | Paths 18-24；重试差异 KDoc 30-37；listProducts 52；quote 72 |
| `FM/calculator/PurchaseCalculatorModels.kt` | 金融产品/报价模型与请求 | FinanceProduct 21-48；QuoteLine 55-63；PurchaseQuote 70-106；请求 119-127 |
| `FM/dealinvoice/DealInvoiceApi.kt` | 成交单契约层 | Paths 19-34；fetchSummary 49；fetchList 63；fetchDetail 78；fetchCustomers 88；create 104；resubmit 116；companion 127-133 |
| `FM/dealinvoice/DealInvoiceModels.kt` | 成交单模型/状态/Tab | fromApi 22-46；auditLabel 29-35；Tab 52-57；Item 104-150；Customer 160-177；请求 184-208 |
| `FM/httpdebug/MineHttpTestApi.kt` | #14 交易分页调试接口 | 类 28；fetchHarmonyIndex 37；PATH 46；PAGE/SIZE 49-50；EMPTY_DATA_MESSAGE 53 |
| `FM/httpdebug/HarmonyIndexModels.kt` | 交易行→鸿蒙首页模型映射 | 三个模型 19-55；harmonyIndexOf 69-86；stringifyOrNothing 89-93 |
| `FM/usedcar/UsedCarOrderApi.kt` | 二手车订单契约层 | Paths 18-30；fetchSummary 50；fetchList 66；fetchDetail 87；fetchCustomers 98；create 114；companion 120-125 |
| `FM/usedcar/UsedCarOrderModels.kt` | 二手车模型 | Stats 22-61；StatusTab 67-72；KindFilter 79-93；Item 106-157；Customer 163-173；请求 196-206 |
| `feature/mine/build.gradle.kts` | 模块构建（见 §1） | 插件 1-8；依赖 12-24；namespace 28 |

### 2.2 feature/mine commonTest（8 个文件）

| 路径 | 作用 | 关键行号 |
|---|---|---|
| `FMt/MineLogicTest.kt` | 4 个纯逻辑测试类（validators/address mock/wallet mock/profile logic） | 类 11 / 24 / 64 / 86；共 4 个 @Test |
| `FMt/MineRecordingApiClient.kt` | 记录请求的假 ApiClient + 真 ApiEnvelopeParser | 类 14；Call 19 |
| `FMt/MineTapDispatchTest.kt` | 点按分发回归（含 fromLabel 断言） | 6 个 @Test；标签断言 81-87 |
| `FMt/address/AddressApiTest.kt` | AddressApi 契约测试 | 5 个 @Test |
| `FMt/calculator/PurchaseCalculatorApiTest.kt` | PurchaseCalculatorApi 契约测试 | 8 个 @Test |
| `FMt/dealinvoice/DealInvoiceApiTest.kt` | DealInvoiceApi 契约测试 | 7 个 @Test |
| `FMt/httpdebug/MineHttpTestApiTest.kt` | MineHttpTestApi 契约测试 | 8 个 @Test |
| `FMt/usedcar/UsedCarOrderApiTest.kt` | UsedCarOrderApi 契约测试 | 8 个 @Test |

### 2.3 feature/shell（6 个文件）

| 路径 | 作用 | 关键行号 |
|---|---|---|
| `FS/MainBottomBar.kt` | Flutter `IosTabBar` 逐项对齐的底部 TabBar（共享 CMP） | KDoc 规格 51-72；composable 74-111；MainTabItem 114-167；图标映射 169-181 |
| `FS/SecondaryRouteIsland.kt` | Mine-only 二级 Compose 岛（iOS/OHOS 用；Android 主路径不用） | KDoc 26-32；Resolver 33-53；isMineIslandOwned 56-57；岛 61-93；LegacyNonMineBlocked 96-109 |
| `FS/SoftAuthPresenter.kt` | 会话 + 软鉴权共享呈现层（StateFlow） | 注入参数 15-18；UiState 19-24；sync 29-32；clearLocalSession 34-38；trySelectTab 43-51；onLoginSucceeded 53-58；dismissGate 60-63；publish 65-74 |
| `FS/DeferredDestinationStub.kt` | 未排期路由统一占位（当前零消费者，见 §5.4/§8） | `ReportMainTabRoot(false)` 29；「后续开放」48；返回按钮 52-57 |
| `FS/OhosComposeHostRequest.kt` | ArkTS → Kotlin 跨边界建岛请求载体 | mode 11；mineRoute 12；secondaryRoute 13（默认 "/home/search"） |
| `FSt/SoftAuthPresenterTest.kt` | 软鉴权回归（4 个 @Test） | gate/放行/恢复 pending/清会话 |
| `feature/shell/build.gradle.kts` | 模块构建（见 §1） | 定位注释 1-5；依赖 16-18 |

---

## 3. 逐文件解析（feature/mine）

### 3.1 `FM/MineIsland.kt` — 岛根容器

```kotlin
@Composable
fun MineIsland(
    initialRoute: MineIslandRoute = MineIslandRoute.Settings,
    onRequestClose: () -> Unit,
)
```
（`FM/MineIsland.kt:18-21`）

| 项 | 说明 | 证据 |
|---|---|---|
| 参数 `initialRoute` | 岛的起始二级路由（枚举，见 §6.1），默认 Settings | `FM/MineIsland.kt:19` |
| 参数 `onRequestClose` | 栈空时回调，语义为"宿主销毁这个 Compose 容器" | `FM/MineIsland.kt:20, 29, 36` |
| 内部状态 | `stack`，初始为 `listOf(initialRoute.toPath())`，push=追加、pop=dropLast | `FM/MineIsland.kt:22` |
| 特判 | `"/mine/about"` 直接渲染 `MineAboutScreen`（不经过 MineRouteHost） | `FM/MineIsland.kt:26-31` |
| 常规 | 其余一律 `MineRouteHost(route = stack.last(), onBack = pop-or-close, onNavigate = push)` | `FM/MineIsland.kt:33-41` |
| 副作用 | 无网络/KV；纯 UI 栈管理 | 全文件 |
| 被谁调用 | Android：`composeApp/src/androidMain/kotlin/com.example.my_kmp_project/nativeshell/NativeAndroidMain.kt:547`；iOS：`composeApp/src/iosMain/kotlin/com.example.my_kmp_project/MainViewController.kt:35`；OHOS：`composeApp/src/ohosMain/kotlin/com.example.my_kmp_project/MainArkUIViewController.kt:54` | 详见 §4 |

### 3.2 `FM/MineScreen.kt` — 遗留全 CMP Tab 根

签名 `fun MineScreen(loggedIn: Boolean, displayName: String?, onLoginClick: () -> Unit, onLogoutClick: () -> Unit)`（`FM/MineScreen.kt:25-30`）。

| 行为 | 语义 | 证据 |
|---|---|---|
| `go(raw)` | "请先登录"→snackbar；否则 `ProductRouteDispatch.resolve`，Toast 宿主→snackbar，其余→`destination=path` | `FM/MineScreen.kt:36-46` |
| 登录后补跳 | `LaunchedEffect(loggedIn)`：pending 非空且已登录则 `go(pending)` | `FM/MineScreen.kt:48-54` |
| Tab 根上报 | 首页态 `ReportMainTabRoot(true)`，二级态 `(false)`（控制底栏可见） | `FM/MineScreen.kt:59, 76` |
| 跨模块渲染 | dest 归属 Home/Content 时走 `CrossFeatureScreens.HomeRouteScreen` / `ContentRouteScreen` | `FM/MineScreen.kt:81-90` |
| "logout" 哨兵 | `MineRouteHost.onNavigate("logout")` → 回根 + `onLogoutClick()` | `FM/MineScreen.kt:103-106` |
| Snackbar | 自管，1800ms 自动消失 | `FM/MineScreen.kt:116-129` |
| 唯一消费者 | `composeApp/src/commonMain/kotlin/com.example.my_kmp_project/app/AppShell.kt:401`（遗留 App()，见 §8.7） | grep 全仓仅此 |

### 3.3 `FM/MineRouteHost.kt` — 路由表 + 22 个私有屏（1643 行）

公开签名：`fun MineRouteHost(route: String, onBack: () -> Unit, onNavigate: (String) -> Unit = {})`（`FM/MineRouteHost.kt:72-76`）。入口先 `MineRoutes.canonicalize(route)`（L77，`core/router/.../FeatureRoutes.kt:145`）。

路由表（`FM/MineRouteHost.kt:78-135`）：

| 路由条件 | 渲染 | 行号 | 真实状态 |
|---|---|---|---|
| `CrossFeatureScreens.handlesSiblingScreen(route)` | `RenderSiblingScreen`（/classroom、/video、/pay/membership） | 79-80 | 借 assembly 端口，见 §7 |
| `MineRoutes.Mall`（"/mall"） | `MallListScreen` | 81-85 | SoT 整图 mock |
| MallDetail | `MallDetailScreen` | 86 | SoT 整图 mock |
| MallOrders | `OrderListScreen` | 87-90 | SoT 整图 mock |
| MallOrderDetail | `MallOrderDetailScreen` | 91 | 写死数据 |
| Wallet | `WalletScreen(onPay→/pay)` | 92 | 内存 mock |
| Pay | `PayCheckoutScreen` | 93 | 占位 |
| Profile | `ProfileEditScreen(onLoggedOut→"logout")` | 95-98 | 真实会话逻辑 |
| Addresses | `AddressListScreen`（进编辑前 `AddressMockStore.selectForEdit`） | 99-109 | 内存 mock |
| AddressEdit | `AddressEditScreen` | 110 | 内存 mock |
| Calculator | `PurchaseCalculatorScreen` | 111 | 整图 mock |
| CheckIn | `CheckInShortcutScreen` | 112 | 兜底 stub |
| SmsTemplates | `SmsTemplateScreen` | 113 | 静态 |
| ShopQr | `ShopQrScreen` | 114 | 假二维码 |
| HttpTest 或 "/mine/buy_qa" | `BuyQaScreen` | 115 | 静态，不访问网络 |
| Poster | `PosterScreen` | 116 | 静态 |
| Settings / SettingsLegacy | `MineSettingsScreen` | 117-119 | 部分真实（环境切换） |
| PersonalizedSettings | `MinePersonalizedSettingsScreen(snackbar = showPlatformToast)` | 120-123 | KV 真实读写 |
| DealInvoiceDemo | `DealInvoiceDemoScreen(onUpload/onOpenDetail→upload)` | 124-128 | 整图 mock |
| DealInvoiceUpload | `DealInvoiceUploadScreen` | 129 | 无 API |
| Feedback / Cooperation / Reminder / Invite | 各自屏 | 130-133 | toast/静态 |
| else | `UnmappedMineRoute` | 134 | 兜底 |

私有屏（均 `private`，外部只能经路由表到达）：

| 屏 | 行号 | 内部行为要点 | 副作用 |
|---|---|---|---|
| `UnmappedMineRoute` | 139 | `ReportMainTabRoot(false)` + 未映射提示 | 无 |
| `MallDetailScreen` | 148 | `mall_detail_body` 整图；「立即购买」命中区 toast | `showPlatformToast("立即购买")` 159 |
| `MallOrderDetailScreen` | 167 | 写死订单：`MO-1001` 189、`¥79.80` 222/247、`店庆纪念马克杯` 218、`qa_user 138****5172` 234；复制单号 toast 194 | 仅 toast |
| `PayCheckoutScreen` | 274 | 注释自证："Flutter /pay SoT is a module placeholder" 275；居中文本 "Pay 模块" 280 | 无 |
| `CheckInShortcutScreen` | 286 | 兜底快捷版（Home 域 `CheckInMallScreen` 优先）；本地 `points = 1280` 289，签到 +10 319，兑换 toast "（mock）" 344 | 内存自增 + toast |
| `MallListScreen` | 356 | SoT 整图 `mall_list_body`，`statusBarsPadding` 对齐截图；隐藏 `Text("‹")` 命中区 380-389；底部「订单」命中区 390-398 | 导航回调 |
| `OrderListScreen` | 403 | SoT 整图 `mall_orders_body` | 导航回调 |
| `WalletFlutterNavBar` | 436 | 钱包系页面的 Flutter 同款导航条 | 无 |
| `WalletScreen` | 467 | 观察 `WalletMockStore.version` 470；**UI 注释「支付宝=1 / 微信=2 / 银行卡=3」472-473**，充值按钮 569-582 走 `validateRecharge`+`recharge(amount, channel, defaultCardId)`；绑卡 672-680；流水 `ledger.take(10)` 697-700 | toast、内存变更 |
| `ProfileEditScreen` | 721 | `MineProfileLogic` 驱动；保存 741-762；头像选择后 toast「头像已选择（待上传，平台 gap）」776；登出行 878-882 → `onLoggedOut()`（即 "logout"） | KV 外真实：`AccountFacade.updateProfile`、`AuthSessionActions.logout` |
| `AddressListScreen` | 893 | 渲染 `AddressMockStore.addresses()`；设默认 `setDefault` 963、删除 `delete` 974 | 内存 |
| `DemoRegions` | 986-992 | 写死 5 组省市字典 | — |
| `AddressEditScreen` | 995 | `save()` 1009-1028 调 `AddressMockStore.validate/save`；地区 `AlertDialog` 选择器 1031-1059 遍历 `DemoRegions` 1036 | 内存 |
| `AddressField` | 1143 | 表单行小组件 | 无 |
| `FinanceProductUi` | 1184 | 金融产品展示小组件；当前无调用点（`PurchaseCalculatorScreen` 已改整图，见 §8.4） | — |
| `PurchaseCalculatorScreen` | 1191 | 注释自证 "full-frame SoT (FillBounds) for Screenshot Diff Gate" 1192；整图 `ContentScale.FillBounds` 1201；全图点击→toast "计算报价（mock）" 1200 | 仅 toast |
| `CalculatorSection` / `ModeChip` / `CalculatorField` | 1217 / 1230 / 1257 | 为表单版计算器准备的小件，整图方案后未被引用（死代码，见 §8.4） | — |
| `SmsTemplateScreen` | 1276 | 4 条静态模板（如 1281「回访关怀」）；「一键发送」为 mock 1301 附近 | toast |
| `ShopQrScreen` | 1316 | 假二维码方框；「保存到相册」mock 1363 附近 | toast |
| `InviteScreen` | 1372 | 写死邀请码 `DEMO2026` 1394；分享动作 mock（1407 附近） | toast |
| `CooperationScreen` | 1417 | 提交 → toast「已提交」1475，无 API | toast |
| `ReminderScreen` | 1488 | 静态 today/tomorrow 列表 | 无 |
| `BuyQaScreen` | 1538 | 3 行静态问答；**不调用 `MineHttpTestApi`**（见 §8.2） | 无 |
| `PosterScreen` | 1567 | 4 张静态海报 | 无 |
| `FeedbackScreen` | 1609 | 输入 + 提交 toast「已提交」1634；空文本提示 1626 | toast |

消费者：`FM/MineIsland.kt:33`；`FM/MineScreen.kt:91`；Android 壳直挂 `NativeAndroidMain.kt:580`（`ShellOverlay.MineRoute` 态）；assembly `AppCrossFeatureScreens.kt:79-86`（`MineRouteScreen` 实现）。

### 3.4 `FM/MineHomeContent.kt` — Mine 首页内容（680 行）

公开签名（`FM/MineHomeContent.kt:58-69`）：

| 参数 | 语义 | 备注 |
|---|---|---|
| `loggedIn` / `displayName` | 会话展示 | L59-60 |
| `onLoginClick` / `onLogoutClick` | 登录/登出入口 | `onLogoutClick` 标注 `@Suppress("UNUSED_PARAMETER")` L62——声明了但本页未用 |
| `onOpenSettings` / `onOpenPersonalized` | 进岛的两条直达回调 | L63-64 |
| `onNavigate: (String) -> Unit = {}` | 其余跳转（原始路由或标签） | L65 |
| `onRequireLogin(redirect)` | 软鉴权：先登录再续跳（Flutter AuthNavigation.openLogin） | KDoc L66，默认回落 onLoginClick L67 |
| `snackbar: (String) -> Unit` | 提示通道（由宿主注入） | L68 |

内部要点：

- 重组触发：首行读 `MinePrefsStore.version`（L70）与 `selectedStoreId`（L72）；stats 取 `MinePrefsStore.statsForStore(selectedStoreId)` 或 `MineCatalog.guestStats`（L75）。
- `applyTap(MineTapResult)`：Toast→snackbar、Navigate→onNavigate、RequireLogin→onRequireLogin（L83-89 区）。
- 软鉴权入口：`onOpenProfile`/`onCalendar` 未登录时 `onRequireLogin(...)`（L91-102，CheckIn 常量在 L100 附近）。
- 换店：`showSwitchStore && loggedIn` 时渲染 `SwitchStoreDialog`（L104-114），确认后 `MinePrefsStore.switchStore(store.id)` + toast「已切换到 …」。
- 折叠交互：滚动驱动 `MineNavFadeExtent`(72dp, L54) 淡入淡出（L182-192），折叠态导航 `MineCollapsedNavBar`（L233-276，@Composable 定义 234），`MineToolbarHeight`=44dp（L55），`TopIconButton` 44dp（L279）。
- `MineTopChrome` 右上四图标区（定义 L197；调用 L131 附近）。
- `ProfileCard`（L296）：头像点按→snackbar("头像")（平台 gap 注释，L310 附近）；「电子名片」snackbar L361 附近；店铺切换行 L335 附近。
- `StatsBar`（L392）、`QuickServicesSection`（L424，onTap→`MineTapDispatch.onQuickService` L159）、`FunctionSection`（L497，`entries = MinePrefsStore.orderedFunctions()`；`onReorderHint` 现状为"把当前顺序显式落 KV + toast『功能区顺序已保存』"，注释注明拖拽 UI later，见 L160-175 段）、`FunctionCard`（L556，`aspectRatio(0.92f)` L563 附近）、`MenuSection`（L620：settings→`onOpenSettings`，其余→`onOther` L641 附近 →`MineTapDispatch.onMenu` L176）。

消费者：`FM/MineScreen.kt:60`；Android Jetpack Mine 根内嵌共享 CMP：`composeApp/src/androidMain/kotlin/com.example.my_kmp_project/nativeshell/NativeJetpackTabs.kt:1277`（`JetpackMineRoot` 定义 1267 起；注释 1276 说明复用共享渲染；onNavigate 转发 `onDeferred(raw)`，snackbar 走 `showPlatformToast`）。

### 3.5 `FM/MineModels.kt` — 写死的目录数据

- 类型：`MineStat`(7)、`MineQuickService`(9)、`MineMenuEntry`(17)、`MineFunctionEntry`(23)、`MineProfileUi`(33)。
- `object MineCatalog`(41)：guestStats(42)、demoStats(49)、`goldDemoProfile`(57-63，写死 `qa_user`、「销售顾问」、「[4S]北京沃德龙鼎吉利」、`138****5172`)、quickServices(66-70：mall 带 HOT 角标、wallet、order)、menuItems(73-81，7 条)、functions(84-158，9 条；calculator 带 highlight `5830.00`)。
- `profile(loggedIn, displayName)`(160-173)：登录态用 goldDemoProfile 复制并覆盖昵称（162-163）；未登录回落 guest/访客(171 附近)。
- 全部为演示数据，无任何网络来源；消费者：`MineHomeContent.kt`（stats 75、区块）、`MineTapDispatch.kt`（按 id 查 label，23/37/51）、`MinePrefsStore.kt`（demoStats 61、functions 66/74/81）。

### 3.6 `FM/MineTapDispatch.kt` — 点按分发（纯逻辑）

| 函数 | 规则（按目录 id） | 行号 |
|---|---|---|
| `onQuickService` | mall/order/wallet → `authOrNav`；其余 → toast「$label 开发中」 | 18-26 |
| `onFunction` | calculator→Calculator（免登录）29、qa→HttpTest（免登录）30；short_video/used_car/ledger/after_sales → authOrNav 31-34；sms/qr_pay/poster → toast 开发中（注释引 Flutter 行为，35） | 28-40 |
| `onMenu` | address authOrNav 43、settings→Navigate 44；feedback/fan_group/invite/reminder/cooperation → **仅标签文案 toast** 45-49（与二级页真实入口并存，见 §8.5） | 42-54 |
| `authOrNav` | 已登录 Navigate / 未登录 RequireLogin | 56-57 |

sealed `MineTapResult`（11-15）：`Toast(message)` / `Navigate(path)` / `RequireLogin(redirectPath)`。消费者：`FM/MineHomeContent.kt:159,165,176`；测试 `FMt/MineTapDispatchTest.kt`。

### 3.7 `FM/MineTheme.kt`（37 行）/ `FM/MineComponents.kt`（246 行）/ `FM/MineContactValidators.kt`（25 行）

- `MineTheme`(14)：`Accent = DesignSystem.Accent`(15)、`Background = DesignSystem.PageBg`(16)，另有 LabelPrimary(#171717, 19) 与字号（Headline/StatValue 22sp/SectionTitle 16sp/Body 16sp/Caption 14sp，33-36）。作用是把 Flutter 语义色从 core.design 桥进 mine 私有 token。
- `MineComponents`：全部 public 小组件——`MineGroupedCard`(39)、`MineSectionHeader`(55)、`MineNavRow`(65，支持 help "?"、badge、trailingText、chevron；个性化设置页 chevron 用位图 `settings_personalized_settings_chevron_right`，129-135)、`MineSwitchRow`(140)、`MineInsetDivider`(196)、`MineAvatarPlaceholder`(205，占位文字「人」)、`MineRoleBadge`(224)。消费者遍布 `MineRouteHost.kt`、`MineSettingsScreen.kt`、`MinePersonalizedSettingsScreen.kt`。
- `MineContactValidators`：`MaxNameLength=12`(5)、`MaxCnMobileLength=11`(6)、`digitsOnly`(8)、`isNameValid`(10-13，非空且 ≤12)、`isCnMobile`(15-17，11 位且 1 开头)、`maskPhone`(20-24)。消费者：`FM/MineProfileLogic.kt`、`MineRouteHost.kt`（地址/合作表单区）、测试 `FMt/MineLogicTest.kt:11`。

### 3.8 `FM/MinePrefsStore.kt` / `FM/MineProfileLogic.kt` — KV 与真实会话

`MinePrefsStore`（object，L16）：

- 四个 key（17-20，均落 core.platform KV，见 §7.3）：`current_store.selected_store_id`、`mine_function_order_ids`、`settings.dark_mode`、`settings.locale_zh`。
- `version`(22-27)：任何写操作 `bump()`，UI 首行读取以触发重组。
- 深色/语言刻意用显式 `MutableState`（32-37 注释：避免与 `setDarkMode/setLocaleZh` 的 JVM setter 签名冲突）。
- `loadSelectedStoreId`(39-41) 空/空白回落 `MineStoreCatalog.defaultStoreId`；`switchStore`(43-49) 目录外 id 返回 null；`statsForStore`(54-62) store "2" 给另一组演示数字；`functionOrderIds`(64-71)「已存顺序 + 目录新增项」合并（新增函数自动排尾）；`reorderFunctions`(73-78) 过滤非目录 id 后落盘；`orderedFunctions`(80-83)；`setDarkMode/setLocaleZh`(85-95) 存 "true"/"false" 字符串。

`MineProfileLogic`（object，L7）：`isDirty`(8，昵称变化或 pendingAvatar)；`validateSave`(13，"昵称不能为空" / "没有需要保存的修改")；`saveNickname`(19-21) → **真实** `AccountFacade.updateProfile`（`core/account/.../AccountFacade.kt:93`）；`logout`(23-25) → `AuthSessionActions.logout`；`sessionNickname` 回落 "qa_user"(27-28)、`sessionPhoneMasked` 回落 "138****5172"(30-32)。消费者：`MineRouteHost.kt:721` 区（ProfileEditScreen）、`FMt/MineLogicTest.kt:86`。

### 3.9 `FM/MineSettingsScreen.kt` / `FM/MinePersonalizedSettingsScreen.kt` / `FM/SwitchStoreDialog.kt`

`MineSettingsScreen`（L37）：

| 区块 | 行为 | 行号 | 状态 |
|---|---|---|---|
| 版本观察 | `MinePrefsStore.version` | 40 | 真实 |
| 环境标签 | `DemoApiHosts.labelForBaseUrl(NetworkConfig.effectiveBaseUrl())` | 45-48 | 真实（`core/network/.../DemoApiHosts.kt:25` TEST=`http://127.0.0.1:8080`） |
| 语言对话框 | 简体中文/English；`MinePrefsStore.setLocaleZh` + toast | 50-90（✓ 标记 65/78） | 真实 KV |
| 环境内联选择 | 调试 host 列表；`NetworkConfig.switchDebugHost(label, url)` | 117-145（127；NetworkConfig.kt:61/71） | 真实 |
| 恢复默认(TEST) | `NetworkConfig.applyEnvironment(NetEnvironment.Test)` | 136-141 | 真实 |
| 深色开关 | `checked = darkMode` → `setDarkMode` | 147-155 | **仅存 KV，未见主题切换接线** |
| 蓝牙示例行 | toast「蓝牙示例暂未接入」 | 173-177 | 明示未接入 |

`MineAboutScreen`（186-217）：展示 `DemoApiHosts.TEST`(203) 与当前 baseUrl(209)；仅在岛内 `/mine/about` 特判路由（`FM/MineIsland.kt:26`）与 `MineScreen.kt:78` 出现。注意两页均不调 `ReportMainTabRoot`（兄弟屏 MineRouteHost 各屏普遍调用）——见 §8.9。

`MinePersonalizedSettingsScreen`（L44，参数 `onBack`、`snackbar`）：前缀 `personalized_settings.`(30)；`loadBool/persistBool`(32-41)；7 项：`eye_protection_mode` 字符串三态「关闭/开启/跟随系统」(49、104)、`teaching_mode` false(51/134)、`content_recommendation` true(53/148)、`ad_recommendation` true(55/159)、`oral_scoring` true(56/168)、`cellular_video_reminder` false(58/177)、`upload_status_monitor` false(61/186)；「装扮中心」toast 开发中(82-83)；`help()` 统一「$title：功能说明开发中」(65)。所有开关只落 KV，不驱动任何实际功能。消费者：`MineRouteHost.kt:120-123`。

`SwitchStoreDialog`（L54，参数 selectedId/onDismiss/onPicked）：`DialogProperties(usePlatformDefaultWidth = false)`(62)；`MineStoreCatalog`(37-47)：defaultStoreId "1"(38)，两条 4S 店 demo；点已选中项→直接 `onDismiss`(105)；右上 ✕(126-136)。消费者：`FM/MineHomeContent.kt:105`；另有 assembly 包装 `AppCrossFeatureScreens.kt:97-109`（供 feature/home 使用：`feature/home/.../HomeScreen.kt:147`）。

### 3.10 mock 存储：`FM/AddressMockStore.kt` / `FM/WalletMockStore.kt`

| 项 | AddressMockStore | WalletMockStore |
|---|---|---|
| 形态 | `object`（L30），**纯内存，进程内可变、不落 KV** | `object`（L44），同样纯内存 |
| 类型 | `MineAddress`(8-25，含 line/phoneMasked) | `WalletBankCard`(7)、`WalletLedgerEntry`(17) |
| 种子 | addr-1(43)、addr-2(54) | 余额 `12_800` 分(52)；工商银行 6172 默认卡 / 建设银行 8890(55-58)；流水种子(59-61) |
| 读 | `addresses()` 默认卡排前(65-66)、`selectedAddressId`(38)、`selectForEdit`(71-73) | `balanceYuan`(63)、`balanceFen`(68)、`cards`(70 区)、`ledger` |
| 写 | `validate`(75-96)、`save`(98-145：id!=null 走更新；新建插头部，空列表时自动设为默认)、`setDefault`(147)、`delete`(154：删默认后把首个提为默认)、`clearDefaultExcept`(163) | `validateRecharge`(74)、`recharge`(82-99)、`validateBindCard`(101)、`bindCard`(109)、`setDefaultCard`(123)、`deleteCard`(133) |
| 测试钩 | `resetForTests`(170) | `resetForTests`(142) |
| ⚠ 渠道号注释冲突 | — | KDoc `channel: 1=wechat 2=alipay 3=card`（L81-82，仅 channel==3 校验 cardId L84）与 UI 注释「支付宝=1 / 微信=2 / 银行卡=3」（`MineRouteHost.kt:472-473`）**语义相反**；见 §8.8 |
| 消费者 | `MineRouteHost.kt:102,106`（路由表内 selectForEdit）与地址两屏；`FMt/MineLogicTest.kt:24` | `MineRouteHost.kt:467` 区；`FMt/MineLogicTest.kt:64` |

### 3.11 `FM/DealInvoiceScreens.kt` — 成交单两屏

- `DealInvoiceDemoScreen`（L46）：SoT 整图 `deal_invoice_demo_body`(64)；**整图点击→`onUpload`**(68)；参数 `onOpenDetail`(49) 保留了但在 L82-84 处标注当前无命中区接入（保留未用）。消费者：`MineRouteHost.kt:124-128`。
- `DealInvoiceUploadScreen`（L92）：选客户后手机号**写死** "138****5172"(112-113)；拍照/选图 → toast「选图能力：平台相册/相机接入后启用」(134，明示平台 gap)；提交 → 无手机号 toast「请先选择客户」，否则 toast「已提交审核」+ `onBack()`(142-143)——**不调 DealInvoiceApi.create**（见 §8.3）。

### 3.12 契约层五包（address / calculator / dealinvoice / httpdebug / usedcar）

五个 API 类均为「Dart 契约镜像」：信封 `code/message/data`、业务失败 `ApiResponse.Business`（`core/network/.../ApiResponse.kt:30`）、`NetworkCodes.PASSTHROUGH = -1`（`core/network/.../NetworkCodes.kt:32`）、错误优先级 message 后回落固定文案（各文件 `dataOrThrow`：AddressApi.kt:106-112、DealInvoiceApi.kt:137-143、UsedCarOrderApi.kt:139-145 等）。共性分页工具：`ListPage.pageQuery`（1-based）与 `limitOffsetQuery`（0-based）（`core/network/.../ListPage.kt:90-113`）。

| 文件 | 端点与行为要点 | 行号 |
|---|---|---|
| `FM/address/AddressApi.kt` | BASE=`/api/v1/user/addresses`(#79)，detail(26)/setDefault(29) 子路径；`list` 只认 `data.items`（52-58、decodeItemsOnly 94-98，非 object 元素跳过，注释 49）；create POST(61)；update PATCH(69)；setDefault POST 无 body(77)；delete 要求 data 非空(86-91) | 21-30, 40, 52-91 |
| `FM/address/AddressModels.kt` | `AddressModel` 11 个可空字段 + resolved* 便捷层(15-46)；`toBridgeJson` 恰好 10 键、**不含 postal_code**(48-59)；`AddressBridgeRequest`(67-78) | |
| `FM/calculator/PurchaseCalculatorApi.kt` | PRODUCTS(20)/QUOTE(23)；KDoc 说明 Dart RetryInterceptor(≥500 重试) 在 KMP 无对应实现——重复请求风险（30-37）；listProducts 空→「产品列表为空」(52-65)；quote 空→「报价结果为空」(72-85) | |
| `FM/calculator/PurchaseCalculatorModels.kt` | `FinanceProduct`(21-48)；`PurchaseQuoteLine`(55-63)；`PurchaseQuote`(70-106，`effective_annual_rate_percent` ↔ Dart `effectiveAnnualRate` 79)；`PurchaseQuoteRequest`(119-127，3 个必发字段+4 个条件字段，序列化用 `NetworkJson` explicitNulls=false) | |
| `FM/dealinvoice/DealInvoiceApi.kt` | BASE/SUMMARY/CUSTOMERS + detail/resubmit(19-34)；fetchList 的 tab 即使是 "all" 也照发 status(63-75)；fetchCustomers 固定 page=1 size=50、q trim、忽略返回分页(88-97)；create 用 **`NetworkJsonExplicitNulls`** 双键 body(104-110)；resubmit 是唯一的 PATCH(116-125)；常量 DEFAULT_SIZE=10 / CUSTOMERS_SIZE=50(127-133) | |
| `FM/dealinvoice/DealInvoiceModels.kt` | `DealInvoiceStatus.fromApi` 未知值默认 PendingReview(22-46)；auditLabel 对 approved_pending_rating 与 rated 都显示「已通过」(29-35)；`DealInvoiceTab`(52-57)；`DealInvoiceItem` 的 `invoice_id`/`id` 双键字符串化(104-150)，`matchesTab`(142-149)；`Customer` customer_id/id 为 Int + display_name/name 回落(160-177)；三个请求体(184-208) | |
| `FM/httpdebug/MineHttpTestApi.kt` | GET `/api/v1/transactions?page=1&size=20`（**1-based pageQuery**；与 home 域 #12 的 limit/offset 0-based 形成对照，16-19、37-42）；忽略业务 code；data 为空抛固定「接口返回数据为空」(40、53)；行非 object 丢弃(26) | |
| `FM/httpdebug/HarmonyIndexModels.kt` | 三模型(19-55)；`harmonyIndexOf` 把 transactions 行折叠成单个 "transactions" section，title=「category · amount」，缺失回落「未分类」/`-`(69-86)；`stringifyOrNothing`(89-93) | |
| `FM/usedcar/UsedCarOrderApi.kt` | BASE/SUMMARY/CUSTOMERS/detail(18-30)；fetchList 恒发 status、kind=All 时省略(66-84)；fetchCustomers(98-107)；create 用 NetworkJsonExplicitNulls，`image_url` 恒发(114-118)；DEFAULT_SIZE=10/CUSTOMERS_SIZE=50(120-125) | |
| `FM/usedcar/UsedCarOrderModels.kt` | Stats/Summary(22-61)；`UsedCarStatusTab`(67-72)；`UsedCarKindFilter` all→null(79-93)；`UsedCarOrderItem.order_id` 必须保持字符串的注释(106 区)、`statusLabel`(149-156)；`UsedCarCustomer` **没有** `name` 回落键（与成交单 Customer 不同，163-173）；请求体(181-206) | |

> **关键事实：这 5 个 API 类在整个仓库里没有任何生产调用点。** 构造调用 `AddressApi(...)`/`PurchaseCalculatorApi(...)`/`DealInvoiceApi(...)`/`UsedCarOrderApi(...)`/`MineHttpTestApi(...)` 仅出现在 `FMt/**` 五个测试文件（grep 全仓除类定义外零命中）。详见 §8.1。

---

## 4. 专题：MineIsland 的三端挂载

岛本体只有一个共享 Compose 入口 `MineIsland`（§3.1），三端由不同的原生容器把它交进来。

### 4.1 Android（NativeAndroidMain，activity-Compose，非 Fragment）

| 步骤 | 证据（`composeApp/src/androidMain/kotlin/com.example.my_kmp_project/nativeshell/NativeAndroidMain.kt`） |
|---|---|
| 路由进入枚举 | `openMineRoute(route)`：先 `MineIslandRoute.fromPath(route)`（200-205），能映射成岛内枚举 → `islandRoute = island; overlay = ShellOverlay.MineIsland`；映射不到 → `overlay = ShellOverlay.MineRoute` 直挂 `MineRouteHost`（211-218） |
| 浮层渲染 | `ShellOverlay.MineIsland ->` 分支（546-551）：`MineIsland(initialRoute = islandRoute, onRequestClose = { closeOverlay() })` |
| 直挂分支 | `ShellOverlay.MineRoute ->`（579-593）：直接 `MineRouteHost(...)`；其中 "logout" 特判 → `AuthRepository.logout()` + `softAuth.clearLocalSession()` + 关闭（584-587） |
| 触发点 | Jetpack Mine 根 `JetpackMineRoot`（727-748）：`onOpenSettings = { openMineRoute(MineRoutes.Settings) }`（739-741）；`onOpenPersonalized` 直接 `islandRoute = MineIslandRoute.Personalized; overlay = ShellOverlay.MineIsland`（742-746） |
| 备注 | 文件头注释（L82）：「Shared CMP is only [MineIsland]」；入口链 `MainActivity.kt:38-39` setContent → `NativeAndroidApp.kt:72` |

### 4.2 iOS（SwiftUI fullScreenCover → ComposeUIViewController）

| 步骤 | 证据 |
|---|---|
| Kotlin 工厂 | `composeApp/src/iosMain/kotlin/com.example.my_kmp_project/MainViewController.kt:28-43`：`MineIslandViewController(route: String = "settings")`，先 `MineIslandRoute.fromPath` 失败再 `fromHostKey`（32），`ComposeUIViewController { MineIsland(...) }`，`onRequestClose → dismissViewControllerAnimated`（34-41）；同文件 51-64 是 `SecondaryRouteViewController`（内部 `SecondaryRouteIsland`，56）；16-21 的旧 `MainViewController` 已 `@Deprecated` |
| Swift 侧状态 | `iosApp/iosApp/ContentView.swift:69-70` `showMineIsland`/`mineIslandRoute`；路由归属注释 117；`openOwnedRoute` 把岛内 key 映射并置 `showMineIsland = true`（232-238 区，194/197/200/203/215/238/274）；软鉴权先登录再开岛 `openMineIslandWithSoftAuth`（267-274） |
| 挂载 | `.fullScreenCover(isPresented: $showMineIsland)`（435-441，secondary 在 439）；`MineIslandHost: UIViewControllerRepresentable` 在 500 行调 `MainViewControllerKt.MineIslandViewController(route:)`；`MineSecondaryHost` 508 调 `SecondaryRouteViewController` |
| Mine 根钩子 | 设置/个性化按钮置岛 key（376-382）；原生底栏 `NativeBottomBar`（389，不用共享 MainBottomBar，见 §5.2） |

### 4.3 HarmonyOS（ArkTS 建岛 → NAPI → ComposeArkUIViewController）

| 步骤 | 证据 |
|---|---|
| ArkTS 设参并创建 | `harmonyApp/entry/src/main/ets/pages/Index.ets:826-848`：`openComposeHost(kind, routeCode)` 先 `libentry.SetOhosHost`（832）再 `libentry.MainArkUIViewController()`（836）；`openMineIsland(personalized)` → `openComposeHost(0, personalized ? 1 : 0)`（850-852）；软鉴权版 `openMineIslandRouteWithSoftAuth`（855-868，codes 4/5/7/8/9 未登录时记 pendingRouteAfterLogin） |
| 路由码→岛入口 | 首页条目直接开岛：`Index.ets:1658, 1662, 1806, 1841, 1845`；`openDeferred` 里按 path 映射 `(0,code)`/软鉴权（911-950：新车成交→7、/settings→(0,0)、personalized→(0,1)、membership→(0,2)、about→(0,3)、profile→4、addresses/edit→9、addresses→5、calculator→(0,6)、deal_invoice→8/7） |
| NAPI 常量表 | `composeApp/src/ohosMain/kotlin/com.example.my_kmp_project/MainArkUIViewController.kt:82-98`：`@CName("KnSetOhosHost")` 将 kind 写入 `OhosComposeHostRequest.mode`（83），kind==0 时 routeCode 0-9 → `MineIslandRoute`（85-97，Settings 兜底）；`KnSetOhosSecondaryRoute`（100-106，mode=1 + 字符串路由，缺省 "/home/search"）；ArkTS 别名声明 `harmonyApp/entry/src/main/cpp/types/libentry/index.d.ts:3-5` |
| 岛渲染 | `MainArkUIViewController.kt:33-71`：`@CName("MainArkUIViewController")`；`ShellBootstrap.install()` 幂等（40-42 区）；`ComposeArkUIViewController` 内按 mode 分发：1→`SecondaryRouteIsland(secondaryRoute)`（50-52），else→`MineIsland(mineRoute)`（54-56）；**onRequestClose 只把 `closed=true` 渲染「已关闭」页（59-63），真正销毁容器由 ArkTS 侧「关闭」按钮完成** |
| ArkTS 容器 | `Index.ets:150` showIsland、174 islandController；岛宿主 `Compose({controller, onBackPressed → controller.onBackPress()})` + 「关闭」按钮（1963-1986，按钮在 1976：showIsland=false、controller=undefined）；返回键转发 1968-1974 与 2386-2404；ArkTS 自绘底栏高 49（1955-1957，色 '#F2FFFFFF'） |

### 4.4 Kotlin 侧路由如何"交进来"（汇总）

| 平台 | 路由入参形态 | 落到枚举的转换点 |
|---|---|---|
| Android | 字符串 path | `MineIslandRoute.fromPath`（`core/router/.../MineIslandRoute.kt:47`），失败走 `MineRouteHost` 直挂 |
| iOS | 字符串（host key 或 path） | `fromPath` ?: `fromHostKey`（`MainViewController.kt:32`；枚举定义 `MineIslandRoute.kt:34`） |
| OHOS | 整数 routeCode 0-9 | `KnSetOhosHost` 的 when 表（`MainArkUIViewController.kt:85-97`）写入 `OhosComposeHostRequest.mineRoute`（`FS/OhosComposeHostRequest.kt:12`） |

---

## 5. 专题：feature/shell 提供什么、谁消费、与 core/ui、core/design 的关系

### 5.1 提供物总览

| 提供者 | 消费方（file:line） | 备注 |
|---|---|---|
| `FS/MainBottomBar.kt:74` `MainBottomBar(selected, onSelect, modifier)` | `composeApp/src/commonMain/kotlin/com.example.my_kmp_project/app/AppShell.kt:350`（遗留 CMP 壳）；`NativeAndroidMain.kt:674`（Android Jetpack 壳 Scaffold 底栏） | iOS 用 SwiftUI `NativeBottomBar`（`ContentView.swift:389`）；OHOS 用 ArkTS 自绘栏（`Index.ets:1955-1957`）——两端不消费本组件 |
| `FS/SecondaryRouteIsland.kt:61` `SecondaryRouteIsland(initialRoute, onRequestClose)` | `MainViewController.kt:56`（iOS）；`MainArkUIViewController.kt:50`（OHOS mode=1） | KDoc 明示「Android main path does not use this host at all」（`FS/SecondaryRouteIsland.kt:26-32`） |
| `FS/SoftAuthPresenter.kt:15` `SoftAuthPresenter(sessionSync, isLoggedIn)` | `NativeAndroidMain.kt:87`（构造）、89 sync、94 clearLocalSession、121 collectAsState、148 trySelectTab、315 onLoginSucceeded | 仅 Android 壳实例化；iOS/OHOS 各自在 Swift/ArkTS 层实现同类逻辑 |
| `FS/DeferredDestinationStub.kt:25` | **零消费者**（全仓 grep 只有定义） | 见 §5.4 |
| `FS/OhosComposeHostRequest.kt:10` | `MainArkUIViewController.kt:36/49-56/83-89`；由 `KnSetOhosHost/KnSetOhosSecondaryRoute` 写入 | 纯跨边界载体，无逻辑 |

### 5.2 MainBottomBar 与 `ImmersiveInsets` / `MainTabChrome` 的关系（以代码为准）

- 高度常量来自 core/design：`import com.example.my_kmp_project.core.design.ImmersiveInsets.MainBottomBarHeight`（`FS/MainBottomBar.kt:37`），栏体 `height(MainBottomBarHeight)`（91），常量定义 `core/design/.../ImmersiveInsets.kt:41-48`（49dp + `shellContentInsets`）。
- 安全区策略：不用 `shellContentInsets`，而是把底部导航条 inset 包进栏体背景：`Spacer(windowInsetsBottomHeight(WindowInsets.navigationBars))`（103，对应 Flutter `SafeArea(top:false)`，注释 102）；顶部 0.5dp hairline 用 Box 叠加避免 49→49.5（79-82 注释、105-109）。
- 可见性联动：底栏显示与否由 core/ui 的 chrome 状态控制——`MainTabChromeController.bottomBarVisible`（`core/ui/.../MainTabChrome.kt:11-19`）经 `LocalMainTabChrome`（21）下发，各页面用 `ReportMainTabRoot(isRoot)`（23-30，注释说明**无 onDispose 复位**）上报。壳侧组装在 `AppShell.kt:228`（创建 controller）与约 338（`CompositionLocalProvider`）；内容侧消费示例：`FM/MineScreen.kt:59/76`、`FS/SecondaryRouteIsland.kt:97`、`FS/DeferredDestinationStub.kt:29`、`FM/MineRouteHost.kt:140` 等。
- 视觉规格：选中态药丸 44×28 圆角 14、`Accent` 12% alpha、200ms 动画（137-141 与 KDoc 57-60）、图标 22dp `ColorFilter.tint`（144-151）、文字 10sp/lineHeight 11sp Geist（154-165），图标资源 `main_tab_*_selected/unselected`（169-181）；KDoc 68-71 明确记录「不做 backdrop blur」的理由（Flutter 侧 95% 不透明背景下模糊可见度≈0，且三端无统一 backdrop 原语）。

### 5.3 SecondaryRouteIsland 的守门逻辑

`SecondaryRouteResolver.resolve`（`FS/SecondaryRouteIsland.kt:34-53`）：先 `ProductRouteDispatch.resolve`；Toast 宿主→null（36）；非 Mine 宿主仅当 `hostForPath(path)==Mine` 才放行（37-44）；Unknown 且以 "/" 开头时走 `MineRoutes.canonicalize`（47-50）。`isMineIslandOwned`（56-57）用于 push 拒绝（72-75，注释引 #23）与最终渲染前检查：非 Mine 直接渲染 `LegacyNonMineBlocked`（87-89，页面 96-109 文案「非 Mine 路由已迁出 Legacy 岛 (#23)」）。合法路径最终经 `CrossFeatureScreens.MineRouteScreen`（92）落回 mine 的 `MineRouteHost`。

`ProductRouteDispatch`（`core/router/.../ProductRouteDispatch.kt`）：host 枚举(9)、toastOnlyLabels(33)/toastOnlyPaths(41)、`resolve`(62)、`hostForPath`(112，Mine 判定 = /mine、/settings、/mall、/wallet、/pay 前缀，125 行区)。

### 5.4 DeferredDestinationStub：名义与现实的差

模块头注释（`feature/shell/build.gradle.kts:3-5`）称其为「未排期路由的统一占位」，但全仓 grep 仅命中定义文件自身：Android 的 `openDeferred`（`NativeAndroidMain.kt:327-369`）把未排期目标映射为 `ShellOverlay.AllServices/Scan/InAppWeb/UnmappedEntry`（334/338/343/365），并未渲染这个 stub。**事实：当前零消费的导出组件。**

---

## 6. 路由链路

### 6.1 mine 内部二级页跳转（岛内栈）

1. 首页条目点击 → `MineTapDispatch`（§3.6）给出 path → 宿主页 `onNavigate(path)`。
2. 岛内：`MineIsland` 的 `onNavigate = { stack = stack + next }`（`FM/MineIsland.kt:38-40`）；返回栈空才 `onRequestClose`（35-36）。
3. 路由表匹配顺序：sibling 拦截（`MineRouteHost.kt:79-80`）→ Mine 自有路由（81-133）→ `UnmappedMineRoute`（134）。
4. 特例：`AddressEdit` 返回直回 `Addresses` 只在旧 `MineScreen` 路径里有硬编码（`FM/MineScreen.kt:94-99`）；岛内统一按栈弹出。
5. "logout" 是约定哨兵字符串（`MineRouteHost.kt:97` 产生；消费在 `FM/MineScreen.kt:103-106` 与 `NativeAndroidMain.kt:584-587`），不是路由。

### 6.2 跨模块跳转（借 CrossFeatureScreenPort，R7 分层规则）

feature 间禁止互相 import（AGENTS.md 分层规则 R7）。mine 需要的他域页面全部经 `component` 的中立端口转发：

| 端口方法 | 定义 | mine 侧调用点 | assembly 实现 |
|---|---|---|---|
| `handlesSiblingScreen` / `RenderSiblingScreen` | `component/.../CrossFeatureScreenPort.kt:24, 28` | `MineRouteHost.kt:79-80` | `AppCrossFeatureScreens.kt:31-47`（/classroom、/video、`MineRoutes.Membership`） |
| `MineRouteScreen` | 端口 36 | shell 岛：`SecondaryRouteIsland.kt:92` | `AppCrossFeatureScreens.kt:79-86` → `MineRouteHost` |
| `HomeRouteScreen` / `ContentRouteScreen` | 端口 40, 32 | `MineScreen.kt:81-90`（遗留路径） | `AppCrossFeatureScreens.kt:89-95` |
| `SwitchStoreDialog` | 端口 44 | （home 域消费：`feature/home/.../HomeScreen.kt:147`） | `AppCrossFeatureScreens.kt:97-109` |

安装点：`composeApp/src/commonMain/kotlin/com.example.my_kmp_project/app/ShellBootstrap.kt:20-25`（`CrossFeatureScreens.install(AppCrossFeatureScreens)`，且包装 `AuthSessionBootstrap.install()` 保持幂等/顺序）；未安装时门面直接抛带说明的错误（`CrossFeatureScreenPort.kt:64-68`）。三端壳入口（AppShell、MainActivity 链、iOS 工厂、`MainArkUIViewController`）先跑 `ShellBootstrap.install()`（其 KDoc 8-13 列明调用方；OHOS 实测调用 `MainArkUIViewController.kt:40-42` 区）。

### 6.3 路由常量与双向映射

- `MineRoutes`（`core/router/.../FeatureRoutes.kt:78-154`）：全部 path 常量（Mall "/mall" 79 … Invite 108），注意 `Membership = "/pay/membership"`(85) 归 commerce、`CheckIn = "/home/check_in_mall"`(92) 归 home、`ShortVideo = VideoRoutes.Short`(91)；`fromLabel`(110) 中文标签→path；`canonicalize`(145) 归一化（SettingsLegacy→Settings 等）。
- `MineIslandRoute`（`core/router/.../MineIslandRoute.kt:7-64`）：岛内枚举 10 值；`toPath`(20)、`fromHostKey`(34，iOS host key)、`fromPath`(47，Android/OHOS path)。
- 标签→岛：Android `openDeferred`/`navigateSecondary`（`NativeAndroidMain.kt:327-415`，Mine host 已在 MineRoute 态时 push，否则 `openMineRoute` 372-415）；OHOS 中文 toastOnly 名单（`Index.ets:879-888`）。

---

## 7. 端点消费表与 KV 读写表

### 7.1 HTTP 端点（契约层声明 → 实际调用点）

| 端点 | 声明 | 生产调用点 | 测试调用点 |
|---|---|---|---|
| GET/POST/PATCH/DELETE `/api/v1/user/addresses*`（list/create/update/detail/setDefault/delete） | `FM/address/AddressApi.kt:21-30, 52-91` | **无** | `FMt/address/AddressApiTest.kt`（5 例） |
| GET `/api/v1/purchase-calculator/products`、POST `.../quote` | `FM/calculator/PurchaseCalculatorApi.kt:18-24, 52-85` | **无** | `FMt/calculator/PurchaseCalculatorApiTest.kt`（8 例） |
| GET `/api/v1/deal-invoices`(+`/summary`、`/customers`、`/{id}`)、POST `/{id}/resubmit`(PATCH 语义)、POST 创建 | `FM/dealinvoice/DealInvoiceApi.kt:19-34, 49-125` | **无** | `FMt/dealinvoice/DealInvoiceApiTest.kt`（7 例） |
| GET `/api/v1/used-car-orders`(+summary/customers/detail)、POST 创建 | `FM/usedcar/UsedCarOrderApi.kt:18-30, 50-118` | **无** | `FMt/usedcar/UsedCarOrderApiTest.kt`（8 例） |
| GET `/api/v1/transactions?page=1&size=20`（#14） | `FM/httpdebug/MineHttpTestApi.kt:37-50` | **无**（`BuyQaScreen` 不接线，`MineRouteHost.kt:1538`） | `FMt/httpdebug/MineHttpTestApiTest.kt`（8 例） |

> mine 全部 UI（钱包/地址/计算器/成交单/问答）都不发这些请求；真实网络仅出现在 Settings 的「环境切换」改变了后续全局 baseUrl（`MineSettingsScreen.kt:127/139`）。所有假数据来自 `MineCatalog`、两个 MockStore 与各屏写死文案（§3.3/§3.5/§3.10）。

### 7.2 mine 相关 KV key（全部经 `core.platform`，落在同一 prefs 文件）

底层：`loadString/saveString/loadStringList/saveStringList`（`core/platform/src/commonMain/kotlin/com.example.my_kmp_project/core/platform/StringListStore.kt:20-29`），prefs 名 `PLATFORM_PREFS_NAME = "home_favorites"`（16，经 core.storage `createKeyValueStore`，18）。

| Key | 写 | 读 | 值语义 |
|---|---|---|---|
| `current_store.selected_store_id` | `MinePrefsStore.switchStore`（`FM/MinePrefsStore.kt:46`） | 初值 29 ← `loadSelectedStoreId` 39-41；消费 `MineHomeContent.kt:72,105-107` | 店铺 id（"1"/"2"，目录见 `SwitchStoreDialog.kt:37-47`） |
| `mine_function_order_ids` | `reorderFunctions`（76；触发点 `MineHomeContent.kt` onReorderHint 167-171 区） | `functionOrderIds` 64-71；消费 `orderedFunctions` 80-83 ← `MineHomeContent.kt:162` 区 | StringList，函数 id 顺序 |
| `settings.dark_mode` | `setDarkMode`（87；`MineSettingsScreen.kt:147-155`） | 初值 33；消费 41/150 | "true"/"false" |
| `settings.locale_zh` | `setLocaleZh`（93；`MineSettingsScreen.kt:50-90`） | 初值 36；消费 42/65/78/159 | "true"/"false"，缺省视为中文 |
| `personalized_settings.eye_protection_mode` | `MinePersonalizedSettingsScreen.kt:104` | 49 | 字符串三态 |
| `personalized_settings.{teaching_mode, content_recommendation, ad_recommendation, oral_scoring, cellular_video_reminder, upload_status_monitor}` | `persistBool`（`FS` 同文件 40；调用点 134/148/159/168/177/186） | `loadBool` 32-38；调用点 51-61 | "true"/"false" + 各自缺省 |

（`AddressMockStore`/`WalletMockStore` **不**占 KV——纯内存，§3.10。）

### 7.3 会话/账号触点（非 KV、非 mine 自有端点）

| 触点 | 位置 | 说明 |
|---|---|---|
| `AccountFacade.updateProfile` | `FM/MineProfileLogic.kt:19-21` → `core/account/.../AccountFacade.kt:93` | 昵称真实持久化 |
| `AuthSessionActions.logout` | `FM/MineProfileLogic.kt:23-25` | 「退出登录」链路 |
| `AuthRepository.logout`（Android 壳直挂路径） | `NativeAndroidMain.kt:584-587` | "logout" 哨兵消费 |
| `showPlatformToast`（expect/actual） | `core/platform/.../PlatformOpen.kt:8`；actual android:30 / ios:25 / ohos:15 | mine 各 mock 屏的 toast 全部走它 |
| `AuthGate` | `component/auth/AuthGate.kt`（requiresAuth/rememberPending/consumePending） | shell 的 `SoftAuthPresenter` 消费；`MainTab` 定义 `core/router/.../AppRoutes.kt:37-46`（Chat/Community requiresAuth=true） |

---

## 8. 测试与真实状态（mock / 未接线 / 写死数据清单）

### 8.1 契约层 = 只测不用的「协议镜像」

5 个 API 类（§3.12）在 commonMain 无任何构造调用；实例化仅发生在 `FMt/*ApiTest.kt`（grep 实证，见 §7.1）。它们是 Dart→KMP 迁移的协议层验证（信封解码、字段回落、分页风格、explicitNulls 差异），**尚未接 UI**。

### 8.2 BuyQaScreen 不访问网络

路由 `/mine/http_test` 与 `/mine/buy_qa` 都落 `BuyQaScreen`（`MineRouteHost.kt:115`），页面是 3 条静态问答（1538 起），与 `MineHttpTestApi.fetchHarmonyIndex` 无任何调用关系。

### 8.3 UI 全部走内存 mock 或未接线的功能

| 屏 | 状态 | 证据 |
|---|---|---|
| 钱包 | `WalletMockStore` 内存态（余额/卡/流水重启即回种子），充值/绑卡不发请求 | `FM/WalletMockStore.kt:52-61, 82-99, 109` |
| 地址 | `AddressMockStore` 内存态 | `FM/AddressMockStore.kt:41-63, 98-145` |
| 成交单 | Demo 整图 + Upload 表单提交仅 toast「已提交审核」；选图能力明示 gap；手机号写死 | `FM/DealInvoiceScreens.kt:64, 112-113, 134, 142-143` |
| 计算器 | 整图 mock，点击 toast「计算报价（mock）」 | `MineRouteHost.kt:1192-1201` |
| 商城四屏 | SoT 截图位图 + 隐藏命中区（MallListScreen 的不可见 "‹" 380-389、订单命中 390-398）；订单详情数据写死 | `MineRouteHost.kt:148-273, 356-434` |
| Pay | 「Pay 模块」占位文本 | `MineRouteHost.kt:275, 280` |
| 签到（Mine 版） | 兜底 stub，本地积分 1280 起 | `MineRouteHost.kt:286-344` |
| 短信/门店码/邀请/海报/提醒/反馈/合作 | 静态数据 + 动作 mock | `MineRouteHost.kt:1276-1643` |
| 头像上传 / 相册选图 / 蓝牙 | 声明的平台 gap（toast 文案自证） | `MineRouteHost.kt:776`；`DealInvoiceScreens.kt:134`；`MineSettingsScreen.kt:177` |
| 个性化 7 开关 / 深色模式 | 只写 KV，不驱动任何功能行为 | `MinePersonalizedSettingsScreen.kt:104-186`；`MinePrefsStore.kt:85-95` |

### 8.4 死代码（当前零引用）

`FS/DeferredDestinationStub.kt`（§5.4）；`MineRouteHost.kt` 内的 `FinanceProductUi`(1184)、`CalculatorSection`(1217)、`ModeChip`(1230)、`CalculatorField`(1257)——整图方案取代表单版后未被 `PurchaseCalculatorScreen` 引用。另 `MineHomeContent` 的 `onLogoutClick` 参数显式标注未用（`FM/MineHomeContent.kt:62`）。

### 8.5 文案双轨的小矛盾

`MineTapDispatch.onMenu` 把 feedback/invite/reminder/cooperation 一律 toast（`FM/MineTapDispatch.kt:45-49`），但 `MineRouteHost` 路由表里这四个屏真实存在（130-133）——即从首页菜单点不进、从岛内其他入口/路由码可进（OHOS `Index.ets` toastOnly 名单同样含混，879-888）。按注释这是对齐 Flutter 根页白名单的刻意行为，但两模块并存时用户可感知不一致。

### 8.6 Android「Mine 根是原生」的准确含义

`NativeJetpackTabs.kt:1266-1292` 的 `JetpackMineRoot` 内部直接嵌入共享 CMP `MineHomeContent`（1277，注释 1276）。即 ADR 0002 所说"Mine root native"在 Android 端实际是 **Jetpack 壳 + 共享 Compose 首页内容** 的混合，只有二级页才进 `MineIsland` 浮层（`NativeAndroidMain.kt:82` 注释「Shared CMP is only [MineIsland]」应结合此内嵌理解）。

### 8.7 Legacy 全 CMP 壳仍在仓

`FM/MineScreen.kt`（整文件）+ `AppShell.kt:401` + `composeApp/src/commonMain/kotlin/com.example.my_kmp_project/App.kt:9` + `DemoApp.kt:11` 构成旧「CMP Tab 壳」路径；iOS 侧唯一入口是 `@Deprecated MainViewController`（`MainViewController.kt:16-21`）。三端正式路径均不再经过它。

### 8.8 渠道号注释互相矛盾（未解疑点）

`FM/WalletMockStore.kt:81`（KDoc：1=wechat 2=alipay 3=card）vs `MineRouteHost.kt:472-473`（UI 注释：支付宝=1 / 微信=2 / 银行卡=3）。代码仅对 channel==3 校验 cardId（`WalletMockStore.kt:84`），两条注释无法同时为真，需要以 Dart 原实现核对后修正其一——**本文档不裁决**。

### 8.9 其它已验证事实/疑点

- `MineSettingsScreen`/`MineAboutScreen` 不调 `ReportMainTabRoot`（对比：`MineRouteHost.kt:140` 等私有屏普遍上报）——在岛内打开设置页时底栏可见性依赖上一次上报残留（`core/ui/MainTabChrome.kt:23-30` 注释明确无 onDispose 复位）。
- OHOS 岛的关闭是**双向契约**：Kotlin `onRequestClose` 仅切到「已关闭」占位屏（`MainArkUIViewController.kt:59-63`），销毁 `XComponent` 容器依赖 ArkTS「关闭」按钮（`Index.ets:1976`）；用户不点按钮则"僵尸岛"仍在（onBackPress 转发 1968-1974 可栈内消费返回）。
- `AddressMockStore` 的删除会「把首个剩余地址提为默认」（`FM/AddressMockStore.kt:154-161`），与新建空表自动默认（104-140 区）语义一致，但均无 UI 文案佐证，属代码行为。
- mine 单测共 46 个 @Test（4+6+5+8+7+8+8，grep 计数），覆盖纯逻辑/分发/契约层；**UI 屏（MineRouteHost 各屏、MineHomeContent、岛栈）无任何 UI 测试**。
- shell 单测 4 个 @Test（`FSt/SoftAuthPresenterTest.kt`：未登录点 Chat 出 gate、Home 放行、登录后恢复 pending Community、clearLocalSession 关 gate）。MainBottomBar/SecondaryRouteIsland/DeferredDestinationStub 均无测试。

### 8.10 真实（非 mock）清单——本文可确认"接了真东西"的部分

| 项 | 真实链路 | 证据 |
|---|---|---|
| KV 持久化 | 店铺选择/功能排序/深色/语言/个性化 7 键，三端落 SharedPreferences/NSUserDefaults/preferences | §7.2 全表 |
| 昵称保存 / 登出 | AccountFacade / AuthSessionActions（Android 直挂路径还有 AuthRepository） | `FM/MineProfileLogic.kt:19-25`；`NativeAndroidMain.kt:584-587` |
| 调试环境切换 | `NetworkConfig.switchDebugHost` / `applyEnvironment(NetEnvironment.Test)`（影响全局 baseUrl） | `MineSettingsScreen.kt:127, 139` |
| Toast / 岛 / 底栏 | 三端平台实现与壳装配（非 mock） | `PlatformOpen.kt:8`；§4；`NativeAndroidMain.kt:674` |

---

## 附：阅读顺序建议

先 §1 定位与 ADR 0002（`docs/adr/0002-native-shell-mine-compose-island.md`）→ §4 三端挂载（理解"岛"为什么存在）→ §6 路由（path↔枚举↔路由码三层）→ §3 逐文件 → §8 真实状态（做二次开发前先确认哪些是 mock）。

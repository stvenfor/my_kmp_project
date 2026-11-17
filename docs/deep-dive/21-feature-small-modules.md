# 21 — Feature 薄壳模块深读 + Feature 层整体盘点

> 仓库根：`/Users/qpy/Desktop/github/my_kmp_project`
> 所有路径均为仓库相对路径；行号对应撰写时 HEAD。

---

## 1 · 五个薄模块逐个解读

### 1.1 feature/content — ContentRouteHost.kt（19 行）

**定位**：零逻辑转发桩。content 模块拥有 6 条 ContentRoutes 常量（`core/router/.../FeatureRoutes.kt:157-163`），但**无一个渲染页属于自身**——目标页全归兄弟 feature（live/friend/media/ai/classroom/video），故渲染表住在装配层，本模块只做接口适配。

**build.gradle.kts 依赖**（`feature/content/build.gradle.kts:14-15`）：
```kotlin
api(projects.component)    // 跨 feature 渲染端口
api(projects.core.router)  // 路由常量
```

**完整函数**：

| 函数 | 位置 | 签名 | 语义 |
|------|------|------|------|
| `ContentRouteHost` | `feature/content/.../ContentRouteHost.kt:13` | `@Composable public fun ContentRouteHost(route: String, onBack: () -> Unit, onNavigate: (String) -> Unit = {})` | 将三条参数原样传给 `CrossFeatureScreens.ContentRouteScreen` |
| （调用体） | 第 18 行 | `CrossFeatureScreens.ContentRouteScreen(route, onBack, onNavigate)` | — |

副作用：读取全局注册端口；若 ShellBootstrap.install() 未先运行则抛 IllegalStateException。

被谁调用：
- `composeApp/.../nativeshell/NativeAndroidMain.kt:595`（Android 原生壳 ContentRoute overlay）
- `feature/home/.../HomeScreen.kt:87`（Home 内嵌 ContentRouteScreen 端口，实际不经本函数）
- `feature/mine/.../MineScreen.kt:86`（同上）

**为什么只有一个文件**：代码证据 = ContentRouteHost.kt:6-10 注释：
> "every destination here is owned by a sibling feature… the rendering table lives in the assembly layer and is reached through CrossFeatureScreens. The signature is kept stable for shared + native-shell callers."

即：**委派给装配层**，不是未完成。实际渲染表在 `composeApp/.../AppCrossFeatureScreens.kt:54-76`（8 条真实 when 分支 + else 兜底），完全覆盖 ContentRoutes 的全部路径。

---

### 1.2 feature/web — InAppWebScreen.kt（64 行）

**定位**：WebView 页面壳，负责将 `PlatformWebView`（expect/actual，三端各实现）+ `WebBridgeHost` + `MineTopBar` + `PlatformBackHandler` 组装为一个可导航页面。

**build.gradle.kts 依赖**（`feature/web/build.gradle.kts:15`）：
```kotlin
api(projects.core.ui)  // 传递带出 core.platform + core.design
```

**函数解读**：

| 函数 | 位置 | 签名 | 参数语义 | 副作用 |
|------|------|------|---------|--------|
| `InAppWebScreen` | `feature/web/.../InAppWebScreen.kt:21` | `@Composable public fun InAppWebScreen(url: String, onBack: () -> Unit, bridge: WebBridgeHost = remember(onBack){DefaultWebBridgeHost(onClose=onBack)})` | `url`=加载地址；`onBack`=关闭回调；`bridge`=JS 桥，默认 DefaultWebBridgeHost | 调用 `ReportMainTabRoot(false)` 关闭底栏；注册 `PlatformBackHandler` 优先走 WebView history |

流程：
1. 创建 `WebViewNavigationHandle`（`InAppWebScreen.kt:29`）——共享可变句柄，由各端 actual 写入 `goBackImpl`。
2. `PlatformBackHandler`（31-35）：先调 `navigation.goBack()`；false 则走 `onBack()`。
3. Column 布局（37-63）：MineTopBar（标题"网页"，返回按钮逻辑与 PlatformBackHandler 相同）+ Box(weight(1f)) 包裹 `PlatformWebView(url, bridge, fillMaxSize, navigation)`。

被谁调用：
- `composeApp/.../nativeshell/NativeAndroidMain.kt:605`（Android native shell overlay）
- `composeApp/.../AppShell.kt:422`（common Shell，`AppRoute.InAppWeb`）

**三端 WebView 差异（core/platform）**：

| 平台 | actual 实现 | 接入技术 | 状态 |
|------|------------|---------|------|
| Android | `PlatformWebView.android.kt` | AndroidView(WebView) + JS Bridge 名"AndroidBridge" + `NativeBridge.invoke` 注入 | 完整 |
| iOS | `PlatformWebView.ios.kt` | UIKitView(WKWebView) + WKScriptMessageHandler + `data:text/html` 特殊处理 | 完整 |
| OHOS | `PlatformWebView.ohos.kt` | 无 ArkWeb 桥接；渲染红色"未接入"提示文字 | **空壳（诚实 gap UI）** |

OHOS 空壳实现（`core/platform/.../PlatformWebView.ohos.kt:21-58`）：

| 位置 | 操作 | 结果 |
|------|------|------|
| `:27` | `bridge.openUrl(url)` | 记录 url，不实际加载 |
| `:28-29` | `navigation.canGoBack = false; goBackImpl = { false }` | 导航句柄永远 false |
| `:38-56` | 三行 Text：标题/URL/"能力状态：missing" | 纯提示 UI，无任何 JS 执行 |

---

### 1.3 feature/live — LiveMockData.kt（28 行）+ LiveScreen.kt（162 行）

**定位**：纯 mock 直播壳；无网络层、无信令 WebSocket。注释明确说明 realtime/push 仍登记在 platform-gap-registry。

**build.gradle.kts 依赖**（`feature/live/build.gradle.kts:14`）：
```kotlin
api(projects.core.ui)  // 无 core.network
```

**LiveMockData.kt** 完整：

| 类型/成员 | 位置 | 说明 |
|----------|------|------|
| `data class LiveRoomItem(id,title,host)` | `:3-7` | 房间列表条目 |
| `object LiveMockData.rooms` | `:10-28` | 3 个硬编码 mock 房间 |

**LiveScreen.kt** 函数：

| 函数 | 位置 | 签名 | 语义 |
|------|------|------|------|
| `LiveScreen` | `:38` | `@Composable public fun LiveScreen(onBack: () -> Unit, openRoom: Boolean = false)` | 入口：openRoom=true 直接进房；否则显示列表 |
| `LiveListContent` | `:63` | `@Composable private fun LiveListContent(onEnterRoom: () -> Unit)` | 列表+进入按钮（实际只有一个房间被选） |
| `LiveRoomScreen` | `:101` | `@Composable private fun LiveRoomScreen(room: LiveRoomItem, onBack: () -> Unit)` | 房间页：假"连接"按钮，点击后改内存状态，无任何 WS |

副作用：`ReportMainTabRoot(false)` 在 LiveScreen 两分支均调用。

被谁调用：
- `composeApp/.../AppShell.kt:473`（`AppRoute.Live`）
- `composeApp/.../AppCrossFeatureScreens.kt:61`（Content route /live 或 /live/room，`openRoom = route == ContentRoutes.LiveRoom`）

---

### 1.4 feature/scan — ScanScreen.kt（253 行）

**定位**：扫码壳；相机权限管理和真实 decode 由 `core.platform.PlatformBarcodeScanner` 三端 actual 提供；OHOS 端为 Unavailable 状态，页面降级展示 mock chrome + 模拟扫码按钮。

**build.gradle.kts 依赖**（`feature/scan/build.gradle.kts:15`）：
```kotlin
api(projects.core.ui)
```

**函数解读**：

| 函数 | 位置 | 签名 | 语义 |
|------|------|------|------|
| `ScanScreen` | `:50` | `@Composable public fun ScanScreen(onBack: () -> Unit, onScanResult: (payload: String) -> Unit = {})` | 扫码主入口 |
| `WysScanChrome` | `:161` | `@Composable private fun WysScanChrome(torchOn, onToggleTorch, onBack, showMockButton, onMockSuccess, scanner)` | WysScan 风格框线 UI（粉色边框） |

ScanScreen 分支逻辑：
- `lastPayload != null`（63-106）：显示扫码结果视图，提供"打开结果"（调 onScanResult + onBack）和"继续扫码"（重置 lastPayload）。
- `permission.status == Unavailable`（109-123）：OHOS / 无相机环境，WysScanChrome 带 mock 成功按钮（硬编码 payload `"myai://mall/orders?id=A1024"`，第 116 行）。
- else（125-157）：PermissionGate + PlatformBarcodeScanner 接真实相机。

PlatformBarcodeScanner expect（`core/platform/.../PlatformCameraScan.kt:30-33`）：
```kotlin
@Composable expect fun PlatformBarcodeScanner(onBarcode: (String)->Unit, modifier: Modifier)
```
Android actual：CameraX + ML Kit BarcodeScanning（完整实现）。
iOS actual：AVFoundation AVCaptureMetadataOutput（完整实现）。
OHOS actual（`core/platform/.../PlatformCameraScan.ohos.kt:33-52`）：
```kotlin
Box { Text("OHOS 相机扫码未接入（registry: missing）") }
```

被谁调用：
- `composeApp/.../AppShell.kt:432`（`AppRoute.Scan`，onScanResult 接 DeepLinkRouter）
- `composeApp/.../NativeAndroidMain.kt:619`（Android native overlay）

---

### 1.5 feature/classroom — 3 个 commonMain 源文件（共 645 行）+ 1 个测试（44 行）

**定位**：课堂列表、作业统计、礼品卡领取等子页面；纯 mock，无网络层，有 commonTest。

**build.gradle.kts 依赖**（`feature/classroom/build.gradle.kts:14-15`）：
```kotlin
api(projects.core.ui)
api(projects.core.router)  // ClassroomRoutes 常量
```

#### ClassroomLogicModels.kt（78 行）

| 类型/成员 | 位置 | 说明 |
|----------|------|------|
| `data class ClassInfo` | `:9-14` | id/name/inviteCode/memberCount |
| `data class GiftCardInfo` | `:16-23` | 礼品卡 6 字段 |
| `data class HomeworkStatRow` | `:25-30` | 作业统计行 |
| `object ClassroomLogicModels` | `:32-58` | 2 个班级 + 1 个礼品卡 + 3 条作业 |
| `findClass(id: String?)` | `:56-57` | 按 id 查，找不到回退第一个 |
| `object ClassroomGiftClaimStore` | `:61-78` | 一次性领取状态；@Volatile 保护；claim() 返回 false 若已领取 |

#### ClassroomScreen.kt（350 行）

| 函数/类型 | 位置 | 签名/说明 |
|----------|------|----------|
| `data class ClassItem` | `:39-45` | id/title/schedule/teacher/outline |
| `ClassroomMockData` (private object) | `:47-71` | 3 个 mock 班级，与 ClassroomLogicModels.classes 是**不同** mock 集 |
| `enum ClassroomRoute` | `:73-77` | List / Detail / Schedule |
| `ClassroomScreen` | `:83-124` | `@Composable public fun ClassroomScreen(onBack, onHomeworkStats: (()->Unit)?=null)` |
| `ClassroomListContent` | `:127-244` | LazyColumn 列表；"禁用班级""邀请同学""创建班级""排行榜"均 showPlatformToast；"作业统计"触发回调 |
| `ClassActionCell` | `:247-257` | 底部三列可点击单元 |
| `ClassroomDetailScreen` | `:260-301` | 授课信息 + 大纲 + 查看课表按钮 |
| `ClassroomScheduleScreen` | `:304-350` | LazyColumn 课表 |

成员数计算：第 179 行 `val members = 12 + (row.id.toIntOrNull() ?: 0)`——用 id 字符串做假递增，不是真实人数。

#### ClassroomRouteHost.kt（217 行）

| 路由 | 处理函数 | 渲染组件 |
|------|---------|---------|
| `ClassroomRoutes.MyClass` | `:77-80` | `ClassroomScreen(onBack, onHomeworkStats = { onNavigate(HomeworkStats) })` |
| `ClassroomRoutes.HomeworkStats` | `:81-89` | `HomeworkStatsPage(…)` |
| `ClassroomRoutes.HomeworkTeacher` | `:90` | `HomeworkDetailPage("教师作业详情", …)` |
| `ClassroomRoutes.HomeworkStudent` | `:91` | `HomeworkDetailPage("学生作业详情", …)` |
| `ClassroomRoutes.HomeworkTransliter` | `:92-96` | `ClassroomSoTPage(…跟读作业图片…)` + toast "开始录制（mock）" |
| `ClassroomRoutes.HomeworkReview` | `:97` | `HomeworkDetailPage("作业点评", …)` |
| `ClassroomRoutes.GiftClaim` | `:98-109` | `ClassroomSoTPage(…礼品卡图片…)`；tap 调 `ClassroomGiftClaimStore.claim()`，toast 显示 duration/cardType |
| `ClassroomRoutes.VideoDetail` | `:110-114` | `ClassroomSoTPage(…视频详情图片…)` |
| else（fallthrough） | `:115` | `ClassroomScreen` |

私有组件：
- `HomeworkStatsPage`（`:120-164`）：LazyColumn 作业行列表；点击跟读类 → onTransliter；其他 → onTeacher。底部四按钮。
- `HomeworkDetailPage`（`:167-187`）：标题+正文+可选操作按钮。
- `ClassroomSoTPage`（`:190-217`）：全铺设计资源图 + 透明返回区域；无操作按钮时仅图；有则整图可点击。

被谁调用：
- `composeApp/.../AppCrossFeatureScreens.kt:43`（`RenderSiblingScreen` 中 `/classroom` 前缀）
- `composeApp/.../AppCrossFeatureScreens.kt:72`（ContentRouteScreen 中 `/classroom` 前缀）
- `composeApp/.../AppShell.kt:479`（`AppRoute.Classroom`，直接调 `ClassroomScreen`，不经 RouteHost）

#### ClassroomLogicModelsTest.kt（44 行）

4 个测试：班级列表 seed 一致性、礼品卡字段、一次性领取、跟读行存在性。`@BeforeTest` 重置 GiftClaimStore。

---

## 2 · "为什么只有 1–2 个文件"的结论

| 模块 | 原因 | 代码证据 |
|------|------|---------|
| **content** | **设计意图：委派至装配层**。所有目标页归属其他 feature，content 无自有 UI；渲染表在 AppCrossFeatureScreens 内。 | ContentRouteHost.kt:6-10 doc 注释 + AppCrossFeatureScreens.kt:50-76 |
| **web** | **薄壳：逻辑全在 core.platform**。PlatformWebView + WebBridgeHost + PlatformBackHandler 三端 actual 已实现全部复杂工作（JS bridge 注入、history 管理）；feature 仅做页面组合。 | build.gradle.kts:15 唯一依赖 core.ui；PlatformWebView.android.kt 122 行 / .ios.kt 188 行 |
| **live** | **功能确实未完成**。注释明确写 "Realtime / push remain platform-gap-registry (no SDK claimed ready)"（LiveScreen.kt:34-35）；房间页无网络/无 WebSocket，按钮仅改内存状态。 | LiveMockData.kt:9 "no push / realtime SDK"；build.gradle.kts:14 无 core.network；LiveRoomScreen 的 connectionLabel 是 `mutableStateOf("未连接")` 纯本地 |
| **scan** | **薄壳：核心能力在 core.platform**。相机权限、CameraX/MLKit/AVFoundation 全由 PlatformBarcodeScanner/OHOS 三端 actual 提供；feature 仅编排 PermissionGate + Chrome UI。 | build.gradle.kts:15 api(projects.core.ui)；PlatformCameraScan.android.kt（>60 行 CameraX）；.ios.kt（>60 行 AVFoundation） |
| **classroom** | **纯 mock + 设计资源图，无网络层**。build.gradle.kts 不依赖 core.network；HomeworkTransliter/GiftClaim/VideoDetail 三页用全屏 PNG（Res.drawable.classroom_*）代替真实实现；作业统计/详情均为硬编码字符串。 | build.gradle.kts:14-15（core.ui + core.router，无 network）；ClassroomSoTPage（:190-217）仅渲染 Image；HomeworkDetailPage（:167-187）body 为字面量 |

---

## 3 · 平台能力依赖与三端差异

### 3.1 WebView（web）

| 平台 | 源文件 | 技术 | 状态 |
|------|--------|------|------|
| Android | `core/platform/.../PlatformWebView.android.kt` (122 行) | AndroidView(WebView) + JSBridge `AndroidBridge` + `NativeBridge.invoke` JS bootstrap | 完整（JS 双向通信、history back） |
| iOS | `core/platform/.../PlatformWebView.ios.kt` (188 行) | UIKitView(WKWebView) + WKScriptMessageHandler + `data:text/html` 解码 + token 预注入 | 完整（JS 双向通信、history back） |
| OHOS | `core/platform/.../PlatformWebView.ohos.kt` (58 行) | 静态 Box + 三行文字；bridge.openUrl 调用但不渲染 | **空壳：registry missing**；注释说 ArkTS WebPage 存在但未桥入 Compose |

`WebViewNavigationHandle`（`PlatformWebView.kt:10-18`）：跨端共享可变句柄，canGoBack 为 Compose State，goBackImpl 由各端 actual 写入。

### 3.2 相机扫码（scan）

| 平台 | 源文件 | 技术 | 状态 |
|------|--------|------|------|
| Android | `core/platform/.../PlatformCameraScan.android.kt` (229 行) | CameraX ImageAnalysis + ML Kit BarcodeScanning + ActivityResult 权限 | 完整 |
| iOS | `core/platform/.../PlatformCameraScan.ios.kt` (188 行) | AVFoundation AVCaptureMetadataOutput(QR) + requestAccess API | 完整 |
| OHOS | `core/platform/.../PlatformCameraScan.ohos.kt` (52 行) | `OhosCameraPermissionController.status = Unavailable`；PlatformBarcodeScanner 仅渲染文字 "未接入" | **空壳：registry missing** |

ScanScreen 的降级处理：`permission.status == Unavailable` 时走粉色 WysScan mock chrome（无 camera 预览、无 decode），并提供"模拟扫码成功"按钮硬编码 payload。

### 3.3 PlatformOpen（content/classroom/live 共用）

| 函数 | Android | iOS | OHOS |
|------|---------|-----|------|
| `showPlatformToast` | `PlatformOpen.android.kt`：Toast.makeText | `PlatformOpen.ios.kt`：自定义 HUD 或 UILabel | `PlatformOpen.ohos.kt:16-19`：println（仅控制台） |
| `openUrl` | Intent(ACTION_VIEW) | UIApplication.open | `PlatformOpen.ohos.kt:7-8`：println stub |
| `copyToClipboard` | ClipboardManager | UIPasteboard | `PlatformOpen.ohos.kt:11-12`：println stub |
| `openSystemNotificationSettings` | Intent(Settings) | UIApplication.openSettings | `PlatformOpen.ohos.kt:21-22`：返回 tip 字符串"Deferred" |

---

## 4 · Classroom 专题

### 屏（5 个可见屏幕）

| 屏 | 渲染组件 | 行数位置 | 内容来源 |
|----|---------|---------|---------|
| 我的班级 | ClassroomScreen(List) | ClassroomScreen.kt:83-244 | 3 个 `ClassroomMockData.classes` |
| 班级详情 | ClassroomScreen(Detail) | ClassroomScreen.kt:260-301 | 同上，显示 outline |
| 课表 | ClassroomScreen(Schedule) | ClassroomScreen.kt:304-350 | 同上，全量列表 |
| 作业统计 | HomeworkStatsPage | ClassroomRouteHost.kt:120-164 | `ClassroomLogicModels.homeworkRows` |
| 礼品卡领取/跟读作业/视频详情/教师/学生/作业点评 | ClassroomSoTPage / HomeworkDetailPage | ClassroomRouteHost.kt:90-114, 167-187, 190-217 | 全屏 PNG 或硬编码字面量 |

### 端点（路由入口 + 路径）

ClassroomRoutes 定义在 `core/router/.../FeatureRoutes.kt:219-235`：

| 常量 | 路径值 | 入口方式 |
|------|--------|---------|
| `MyClass` | `/classroom/my_class` | HomeScreen go("/classroom") → AppRoute.Classroom |
| `HomeworkStats` | `/classroom/homework_stats` | RouteHost ClassroomScreen onHomeworkStats 回调 |
| `HomeworkTeacher` | `/classroom/homework/detail_teacher` | HomeworkStats 点击非跟读行 |
| `HomeworkStudent` | `/classroom/homework/detail_student` | HomeworkStats 底部按钮 |
| `HomeworkTransliter` | `/classroom/homework/transliter` | HomeworkStats 点击跟读行 |
| `HomeworkReview` | `/classroom/homework/review` | HomeworkStats 底部按钮 |
| `GiftClaim` | `/classroom/gift/claim` | HomeworkStats 底部按钮 + RoutePath 别名 |
| `VideoDetail` | `/classroom/video/detail` | HomeworkStats 底部按钮 |

外部入口：
- `AppShell.kt:479`：`AppRoute.Classroom -> ClassroomScreen(onBack = { clearShellRoute() })`——不传 onHomeworkStats（null），因此 "作业统计" 按钮走 `showPlatformToast("作业统计")`。
- `AppCrossFeatureScreens.kt:43`/`:72`：`route.startsWith("/classroom")` → `ClassroomRouteHost(route, onBack, onNavigate)`——完整 when 分支。

设计资源（SoT PNG）位于 `core/design/src/commonMain/composeResources/drawable/`：
- `classroom_homework_transliter_body.png`
- `classroom_gift_claim_body.png`
- `classroom_video_detail_body.png`

---

## 5 · Feature 层全量盘点表

> 计数方法：`find feature/$m/src -type f -name '*.kt' -not -path '*/build/*'`；deps 取自各模块 `build.gradle.kts` 的 `api(projects.*)`/`implementation(projects.*)`。

| 模块 | commonMain kt | 其他 sourceSet kt | 测试 kt | build.gradle.kts 依赖 | 真实状态 |
|------|:---:|:---:|:---:|------|------|
| **ai** | 2 | 0 | 1 | core.ui, core.network, core.router + impl core.network | 实现（SSE AI 对话） |
| **auth** | 18 | 0 | 4 | core.ui, core.network, component + impl core.network, core.router | 实现（登录/注册/OTP） |
| **chat** | 7 | 0 | 2 | core.ui, core.network, component + impl core.network | 实现（IM 聊天） |
| **classroom** | 3 | 0 | 1 | core.ui, core.router | **mock 实现**（无网络、全屏 PNG） |
| **commerce** | 6 | 0 | 1 | core.ui, core.network, component + impl core.network | 实现（商城/会员） |
| **community** | 6 | 0 | 3 | core.ui, core.router, core.network + impl core.network | 实现（动态社区） |
| **content** | 1 | 0 | 0 | component, core.router | **薄壳**（委派 AppCrossFeatureScreens） |
| **friend** | 3 | 0 | 2 | core.ui, core.network, component + impl core.network | 实现（好友列表/API） |
| **home** | 34 | 2 (chartsVicoMain 1, ohosMain 1) | 7 | core.ui, core.router, core.network, component + impl core.network | 实现（首页/搜索/图表） |
| **live** | 2 | 0 | 0 | core.ui | **mock 未完成**（无 WS/无网络） |
| **media** | 6 | 3 (androidMain 1, iosMain 1, ohosMain 1) | 1 | core.ui, core.router, core.network + impl core.network | 实现（视频/音乐播放器） |
| **mine** | 27 | 0 | 8 | core.ui, core.router, core.network, component + impl core.network | 实现（个人中心） |
| **pay** | 2 | 0 | 1 | core.network + impl core.network | **未接线**（支付契约先行，全仓 0 代码消费者） |
| **scan** | 1 | 0 | 0 | core.ui | **薄壳**（能力在 core.platform） |
| **shell** | 5 | 0 | 1 | core.ui, core.router, component | 实现（底栏/软鉴权/路由岛） |
| **wallet** | 2 | 0 | 1 | core.network + impl core.network | **未接线**（钱包契约先行，全仓 0 代码消费者） |
| **web** | 1 | 0 | 0 | core.ui | **薄壳**（能力在 core.platform） |

统计汇总：
- 总 commonMain kt：136；其他 sourceSet kt：5；测试 kt：33
- 有 commonTest 的模块：13/17；无测试的 4 个 = content / live / scan / web（classroom 有 1 个测试文件）
- 依赖 `:component` 的 feature：auth, chat, commerce, content, friend, home, mine, shell（8 个）
- 依赖 `:core:router` 的 feature：auth(impl), classroom, community, content, home, media, mine, shell（8 个 api）
- 不依赖 core.network 的 feature：classroom, content, live, scan, web, shell（6 个——其中 content 不需要、web/scan 的 IO 全由 core.platform 承担）
- composeApp 引用的 feature（grep `import com.example.my_kmp_project.feature.<name>` 命中 > 0 文件）：ai, auth, chat, classroom, commerce, community, content, friend, home, live, media, mine, scan, shell, web（15 个）。**pay、wallet 无 composeApp import**（实测 0 文件）。

---

## 6 · 附：依赖方向违规实测

执行命令与结果：

```bash
# 1. Gradle build 层 feature→feature / feature→composeApp 依赖
grep -rn -E 'projects\.(feature|composeApp)' feature/*/build.gradle.kts
# 结果：0 命中（exit=1，无匹配）

# 2. Kotlin import 层跨 feature 依赖
grep -rn -E '^import com\.example\.my_kmp_project\.feature\.' feature/ --include='*.kt' \
  | grep -v -E '/feature/([a-z]+)/.*my_kmp_project\.feature\.\1\.'
# 结果：0 命中（exit=1）

# 3. feature 层 import composeApp（app 包）
grep -rn -E '^import com\.example\.my_kmp_project\.app\.' feature/ --include='*.kt'
# 结果：0 命中（exit=1）

# 4. core/component 层向上 import feature/app
grep -rn --include='*.kt' -E '^import com\.example\.my_kmp_project\.(feature|app)\.' core component
# 结果：0 命中（exit=1）

# 5. core/component Gradle 声明 feature 模块
grep -rn -E 'projects\.feature' core/*/build.gradle.kts component/build.gradle.kts
# 结果：0 命中（exit2=1）
```

**结论：0 违规**。分层守卫脚本 `scripts/check-layer-deps.sh` 的三组检查（feature→feature imports、lower→upward imports、feature→feature module deps）均通过。跨模块 UI 渲染正确通过 `CrossFeatureScreens` 端口实现依赖倒置（接口在 `:component`，实现在 `:composeApp/app` 装配层，feature 只 import component 接口）。

补充验证（PayApi/WalletApi 跨模块消费者）：
```bash
grep -rn --include='*.kt' -E '\b(PayApi|WalletApi)\b' . \
  | grep -v '/build/' | grep -v 'feature/pay/\|feature/wallet/'
# 结果：1 命中 = feature/mine/.../WalletMockStore.kt:42 的一条 KDoc 注释提及，非 import、非调用
```
即 `PayApi` / `WalletApi` 在本仓的**代码消费者为 0**：唯一调用点是各自的模块内契约测试（`feature/pay/.../MembershipPayContractTest.kt`、`feature/wallet/.../WalletApiContractTest.kt`）。两模块状态为"HTTP 契约先行、尚未接线到任何 UI"——与 `:component` 的 `PayGateway.kt`（`component/src/commonMain/.../pay/PayGateway.kt`，端口层）并存，但本次 grep 未发现 UI 层实际把两者接通。

---

## 7 · 补充观察

1. **content 无自有测试**：因无逻辑（19 行转发），装配层 AppCrossFeatureScreens 的行为由 `composeApp/src/commonTest` 覆盖。

2. **live 文件注释自证缺口**：`LiveMockData.kt:9` "Mock live rooms for list/entry parity (no push / realtime SDK)"；`LiveScreen.kt:35` "Realtime / push remain platform-gap-registry (no SDK claimed ready)"。component.realtime 模块已有 12 个 kt + 11 个 test（`AppRealtimeClientImpl.kt` 等），但 live 的 build.gradle.kts **不依赖 :component**，故信令功能未接线。

3. **scan OHOS 降级 payload 硬编码**：`ScanScreen.kt:116` mock success 固定返回 `"myai://mall/orders?id=A1024"`，走 DeepLinkRouter 后进入商城订单——仅验证路由链路，不代表真实扫码能力。

4. **classroom 两套 mock 数据并存**：`ClassroomMockData`（3 条，ClassroomScreen.kt:47-71）用于班级列表；`ClassroomLogicModels.classes`（2 条，LogicModels:36-39）用于测试 seed 校验与礼品卡/作业——两者 id 字段含义不同（前者为课序号，后者为班级 id），不是重复代码而是不同用途的 fixture。

5. **web 的 DefaultWebBridgeHost 默认 pay 返回不可用**：`core/platform/.../WebBridgeHost.kt:71-72`：`WebBridgeMethods.Pay -> """{"ok":false,"message":"pay unavailable"}"""`——WebView JS 调 pay 桥时直接收到失败 JSON，feature/web 本身不做支付。

6. **home 有独有 sourceSet `chartsVicoMain`**：包含 `AnalyticsCharts.vico.kt`，为 Android+iOS 提供 Vico 图表渲染；OHOS 使用 `ohosMain/AnalyticsCharts.ohos.kt` 做 actual——说明 home 图表是唯一在 feature 层有 expect/actual 的模块。

7. **所有 feature 模块的 Android namespace 格式统一**：`com.example.my_kmp_project.feature.<name>`（见各 build.gradle.kts 末尾 `android { namespace = … }`）。

8. **media 拥有最多平台 sourceSet**（androidMain/iosMain/ohosMain 各一个 `MediaPlayer.*.kt`），是唯一在 feature 层实现三端 expect/actual 的模块；其余薄壳 feature 均只有 commonMain。

9. **ClassroomRouteHost 有 6 个未使用的路由 import**：`ClassroomRouteHost.kt:53-59` 导入 AiRoutes/CommunityRoutes/ContentRoutes/HomeRoutes/MineRoutes/VideoRoutes，但 when 主体（:76-116）只使用 `ClassroomRoutes.*`——编译仅告警，属装配表迁移（AppCrossFeatureScreens 接管跨表分支）后的残留。

10. **ClassroomSoTPage 的返回区域不可见**：`ClassroomRouteHost.kt:206-215` 用 `Color.Transparent` 的 "‹" Text 做左上角可点击返回（`contentDescription` 只给了 Image），无视觉提示；与同模块其他页用 `MineTopBar` 的风格不一致。

11. **ClassroomLogicModels 部分成员零消费者**：`DefaultClassId`/`DefaultHomeworkId`（ClassroomLogicModels.kt:33-34）与 `findClass`（:56-57）在生产代码中无人调用——`findClass` 仅被 `ClassroomLogicModelsTest.kt:19-20` 使用（实测 grep 全仓 feature+composeApp，排除定义文件与测试后 0 命中）。

12. **AppRoute.Classroom 丢失 onHomeworkStats 回调**：`AppShell.kt:479-481` 直接渲染 `ClassroomScreen(onBack=…)` 不传第二参，此时列表卡"作业统计"按钮走 `showPlatformToast("作业统计")` 兜底（ClassroomScreen.kt:214）；只有经 `ClassroomRouteHost`（`/classroom/my_class` 路由字符串）进入才有完整跳转——同一屏两种入口能力不对等，属已接线的降级路径而非 bug 证据（代码有意写了 `?:` 兜底）。

# 10 · core:base / core:platform / core:analytics 深度解析

> 实证口径：本文全部结论来自逐文件整读（22+6+2 个源文件与 3 个 `build.gradle.kts`）与全仓 grep 调用点检索；
> 每条事实附仓库相对路径行号。查不到调用点的 API 一律明写「未找到调用点」。Gradle 依赖只信 `build.gradle.kts`
> 的 `api(...)/implementation(...)` 语句本身，不信文件里的叙述性注释。
>
> 校验：本文需通过 `docs/deep-dive/_verify/verify_docs.py`（scope = `core/base`、`core/platform`、`core/analytics`，
> scope 内每个文件 basename 必须出现，所有 `path:line` 引用必须真实存在）。

---

## 一、模块定位与依赖实证

### 1.1 一句话定位

| 模块 | 定位 | 实证 |
|------|------|------|
| `:core:base` | 最底层纯 Kotlin 通用件：文本工具（`core.text`）+ UUID 门面（`core.uuid`，expect/actual）；另含一个无调用者的占位类 `BaseUtils` | `core/base/build.gradle.kts:1-5`、文件清单见 2.2 |
| `:core:platform` | 「必须碰平台 API」的最小外壳：返回键、外部打开/剪贴板/Toast/通知设置、应用内 WebView + JS 桥、相机扫码、KV 列表存储；commonMain 只放 expect，三端各 actual | `core/platform/build.gradle.kts:1-6` |
| `:core:analytics` | 埋点模块，当前只有 1 个占位函数 `analyticsInfo()`；文件名叫 `Tracker.kt` 但**没有任何名为 Tracker 的类型** | `core/analytics/src/commonMain/kotlin/com.example.my_kmp_project/core/analytics/Tracker.kt:3-5` |

三个模块都是 `:core:*` 库模块，不产二进制；工程唯一二进制产出方是 `:composeApp`（`build.gradle.kts:94-96` 注释所指的共享接线只给 target，不给 framework）。

### 1.2 各模块自己声明的 Gradle 依赖

**`:core:base`（`core/base/build.gradle.kts:6-31`）**

| 依赖 | 类型 | 挂在哪 | 实证 |
|------|------|--------|------|
| `libs.uuid`（CPF `com.benasher44:uuid:0.8.4-1.0.0`） | implementation | 中间 source set `uuidCpfMain` | `core/base/build.gradle.kts:15-20`（18 行 `implementation(libs.uuid)`）、`gradle/libs.versions.toml:49`（版本）、`gradle/libs.versions.toml:110`（坐标） |
| android namespace | — | `android { namespace = "com.example.my_kmp_project.core.base" }` | `core/base/build.gradle.kts:29-31` |

`uuidCpfMain` 被 `androidMain`（`core/base/build.gradle.kts:21`）、`iosMain`、`ohosArm64Main` 依赖（`core/base/build.gradle.kts:22-25`，`androidOnly=true` 时跳过后两者）。commonMain 本身**不声明任何依赖**；target 集 / coroutines / atomicfu / serialization / kotlin-test 全部来自根工程共享接线（`build.gradle.kts:87-102` targets、`build.gradle.kts:107-117` 公共依赖、`build.gradle.kts:118-127` iosMain/ohosMain 层级）。

**`:core:platform`（`core/platform/build.gradle.kts:7-42`）**

| 依赖 | 类型 | source set | 实证行 |
|------|------|------------|--------|
| `:core:design` | api | commonMain | `core/platform/build.gradle.kts:17` |
| `:core:account` | api | commonMain | `core/platform/build.gradle.kts:18` |
| `:core:storage` | **implementation**（不外泄类型） | commonMain | `core/platform/build.gradle.kts:19` |
| compose.runtime / foundation / material3 / ui / components.resources | api | commonMain | `core/platform/build.gradle.kts:20-24` |
| androidx activity-compose / core-ktx / lifecycle-viewmodel-compose / lifecycle-runtime-compose | implementation | androidMain | `core/platform/build.gradle.kts:27-30` |
| CameraX 四件套（core/camera2/lifecycle/view）+ MLKit barcode-scanning | implementation | androidMain | `core/platform/build.gradle.kts:31-35` |

插件含 `composeMultiplatform` + `composeCompiler`（`core/platform/build.gradle.kts:10-11`），因为 commonMain 有 `@Composable expect`。**iosMain / ohosMain 未声明任何额外依赖**：iOS 用 KGP 自带的 `platform.*` interop，OHOS 端全部是 println/纯 Compose 占位，不触 ArkWeb/Camera NDK。

**`:core:analytics`（`core/analytics/build.gradle.kts:4-10`）**：只有 `kotlinMultiplatform` + `androidLibrary` 两个插件和 namespace，无任何 project/library 依赖。

### 1.3 反向依赖（谁在用这三个模块）

| 消费者 | 依赖方式 | 实证 |
|--------|----------|------|
| `:composeApp` | implementation 三者全收 | `composeApp/build.gradle.kts:126`（core.base）、`:127`（core.analytics）、`:129`（core.platform） |
| `:component` | **api** `:core:base` | `component/build.gradle.kts:39` |
| `:core:ui` | **api** `:core:platform`（把 PlatformWebView / CameraPermissionController 等传递带给 feature） | `core/ui/build.gradle.kts:17` |
| feature 层 | 无一直接依赖本分册三模块；经由 `api(projects.core.ui)` 拿 core.platform（如 `feature/web/build.gradle.kts:15`、`feature/scan/build.gradle.kts:15`、`feature/chat/build.gradle.kts:15`），经由 `api(projects.component)` 拿 core.base（`feature/chat/build.gradle.kts:20`） | 各行号左列 |

`settings.gradle.kts:28,29,31` 分别 include `:core:base`、`:core:analytics`、`:core:platform`。
`composeApp/build.gradle.kts:113-114` 的注释把这套下沉策略总结为「CPF uuid → :core:base、CameraX/MLKit 扫码 → :core:platform」，与上表实证一致。

---

## 二、core:base 逐个 API 解剖

### 2.1 文件清单（7 个文件，全覆盖）

| 路径 | 作用 | 行数/关键行号 |
|------|------|---------------|
| `core/base/build.gradle.kts` | 模块接线：uuidCpfMain + libs.uuid | 31 行；`15-25` uuidCpfMain 创建与挂载 |
| `core/base/src/commonMain/kotlin/com.example.my_kmp_project/core/base/BaseUtils.kt` | 占位类 | 7 行；`5-7` |
| `core/base/src/commonMain/kotlin/com.example.my_kmp_project/core/text/HtmlUtils.kt` | 两个顶层 HTML 工具函数 | 23 行；`3-6` 正则、`8-18` stripHtmlRough、`20-23` extractImageUrlsFromHtml |
| `core/base/src/commonMain/kotlin/com.example.my_kmp_project/core/uuid/AppUuid.kt` | `expect object AppUuid` | 16 行；`10-16` |
| `core/base/src/uuidCpfMain/kotlin/com.example.my_kmp_project/core/uuid/AppUuid.cpf.kt` | android/ios/ohosArm64 共用 actual（CPF 库） | 19 行；`7-18` |
| `core/base/src/ohosX64Main/kotlin/com.example.my_kmp_project/core/uuid/AppUuid.ohosX64.kt` | ohosX64 stub actual（自研实现） | 48 行；`9-47` |
| `core/base/src/commonTest/kotlin/com.example.my_kmp_project/core/uuid/AppUuidTest.kt` | 4 个行为测试 | 41 行；`10-40` |

### 2.2 `BaseUtils`

```kotlin
class BaseUtils {                                    // BaseUtils.kt:5
    fun getBaseInfo(): String = "Base Info from core-base"   // BaseUtils.kt:6
}
```

- 参数：无；返回：固定字符串。无副作用。
- 被谁调用：**未找到任何调用点**（全仓 grep `BaseUtils` 仅命中定义）。
- 真实状态：`BaseUtils.kt:3-4` 注释自陈「占位骨架：对照 kmp-cmp-example 的 lib-base」。**这是占位，不是能力**。

### 2.3 `core.text.HtmlUtils`（注意包名不是 `core.base`）

```kotlin
fun stripHtmlRough(html: String): String             // HtmlUtils.kt:8
fun extractImageUrlsFromHtml(html: String): List<String>  // HtmlUtils.kt:20
```

**`stripHtmlRough(html)`**：参数 `html` 任意 HTML 串；返回去标签纯文本。流水线（`HtmlUtils.kt:10-18`）：删 `<script>`/`<style>` 块（正则带 `(?is)`，忽略大小写且跨行）→ 其余标签替换为空格（12）→ 解 `&nbsp;/&lt;/&gt;/&amp;` 四个实体（13-16）→ 折叠连续空白（17）→ `trim()`（18）。顺序是**先去标签后解实体**，所以正文里的 `&lt;script&gt;` 解出来不会被二次删除；`&quot;` 等实体**不处理**。无副作用（纯函数）。

**`extractImageUrlsFromHtml(html)`**：用 `HtmlUtils.kt:3-6` 的 `<img ... src="...">` 正则（IGNORE_CASE）收集所有 src 值，trim 后丢弃空串，按出现顺序返回（`HtmlUtils.kt:21-23`）。不去重、不解析相对地址。无副作用。

- 被谁调用：两个函数**均未找到调用点**（全仓 grep 函数名与 `core.text` import 皆零命中）。实现是完整的，但属于「已实现、未接线」。

### 2.4 `AppUuid`（expect object，本模块 expect/actual 机制主角）

expect 声明（`core/base/src/commonMain/kotlin/com.example.my_kmp_project/core/uuid/AppUuid.kt:10-16`）：

```kotlin
expect object AppUuid {
    fun random(): String            // AppUuid.kt:12
    fun parseOrNull(value: String): String?   // AppUuid.kt:15
}
```

| 函数 | 参数 | 返回 | 副作用 | 被谁调用 |
|------|------|------|--------|----------|
| `random()` | 无 | 小写 8-4-4-4-12 的 UUID v4 串 | 无（消耗随机源） | `component/src/commonMain/kotlin/com.example.my_kmp_project/component/realtime/AppRealtimeClientImpl.kt:69`（默认参数 `newFrameId`）、`component/src/commonMain/kotlin/com.example.my_kmp_project/component/realtime/RealtimeOutboundQueue.kt:121`（`newMessageId`）、`feature/chat/src/commonMain/kotlin/com.example.my_kmp_project/feature/chat/ImSessionApi.kt:68`（mock 融云 id） |
| `parseOrNull(value)` | `value`：待解析串（先 trim） | 合法→规范化小写串；非法→`null` | 无 | 生产代码**未找到调用点**；仅测试 `AppUuidTest.kt:25,37-39` |

`AppUuid.kt:8` 有硬约束注释：**调用点必须用本门面，feature 禁止直接 import `com.benasher44.uuid`** —— 这正是为了 ohosX64 的分支而设。

**actual 一：`AppUuid.cpf.kt`（androidMain + iosMain + ohosArm64Main 三端共用，`core/base/build.gradle.kts:21-25` 挂载）**

- `random()`：直接 `uuid4().toString()`（`core/base/src/uuidCpfMain/kotlin/com.example.my_kmp_project/core/uuid/AppUuid.cpf.kt:8`），实现细节委托给第三方库。
- `parseOrNull()`：trim 后空串直接 null（12），否则 `uuidFrom(trimmed)` 并捕获 `IllegalArgumentException` 返回 null（13-17）——即「CPF 库的合法性判定为准」。

**actual 二：`AppUuid.ohosX64.kt`（仅 ohosX64Main）**

- `random()`：`kotlin.random.Random` 生成 16 字节（`core/base/src/ohosX64Main/kotlin/com.example.my_kmp_project/core/uuid/AppUuid.ohosX64.kt:14`），手工置版本位 4 与 variant 10xx（16-17），`formatUuid` 手拼 36 字符（27-47）。文件头注释（5-8）明说：*不是产品 SoT，验收走 ohosArm64*。**这是伪随机（非加密级），与 CPF 分支的随机源不同。**
- `parseOrNull()`：自带 canonical 正则校验（10-11、21-24），通过则 `lowercase()` 返回——与 cpf 分支的判定方式（库抛异常）等价但实现独立。

**测试**：`AppUuidTest.kt` 在 commonTest（各编译目标都会跑；kotlin-test 由 `build.gradle.kts:114-116` 注入）覆盖 4 个行为：随机值形状（16-20）、random↔parse 往返（22-28）、连续两次不同（30-33）、非法输入拒绝（35-40）。stub 与 cpf 两个 actual 都能过这组测试。

### 2.5 expect/actual 机制在 core:base 的真实用法

为什么拆：CPF 只发布了 android / ios / ohosArm64 三端 klib，**没有 ohosX64**（`core/base/build.gradle.kts:4-5` 注释、`gradle/libs.versions.toml:47-48` 注释）。若把 `libs.uuid` 放 commonMain，ohosX64 目标直接解析失败。解法是「中间 source set + 每端各一份 actual」：

| source set | 依赖 `libs.uuid` | 提供 `actual object AppUuid` |
|------------|------------------|------------------------------|
| commonMain | 否 | expect（`AppUuid.kt:10`） |
| uuidCpfMain | 是（`core/base/build.gradle.kts:18`） | cpf 版（`AppUuid.cpf.kt:7`） |
| androidMain / iosMain / ohosArm64Main | 经 dependsOn 传递获得 | 复用 cpf 版 |
| ohosX64Main | 否 | 自研 stub（`AppUuid.ohosX64.kt:9`） |

`-PandroidOnly=true` 时根接线不建任何 native target，`core/base/build.gradle.kts:22-25` 的 if 保护避免 `getByName("iosMain")` 找不到源集——改这个文件必须保住这条保护。

---

## 三、core:platform 逐个 API 解剖

### 3.1 文件清单（23 个文件，全覆盖；`src/commonMain` 6 + `androidMain` 6 + `iosMain` 5 + `ohosMain` 5 + build 脚本）

| 路径（前缀 `core/platform/src/`） | 作用 | 关键行号 |
|---|---|---|
| `../build.gradle.kts` | 接线（见 1.2） | 16-25、26-36 |
| `commonMain/.../platform/PlatformBackHandler.kt` | `@Composable expect` 返回键 | 6-9 |
| `commonMain/.../platform/PlatformOpen.kt` | 4 个 expect：openUrl/剪贴板/Toast/通知设置 | 4、6、8、14 |
| `commonMain/.../platform/PlatformCameraScan.kt` | 权限枚举/接口 + 2 个 expect composable | 6-12、14-19、22、30-33 |
| `commonMain/.../platform/PlatformWebView.kt` | 导航句柄 + `@Composable expect` WebView | 10-18、25-30 |
| `commonMain/.../platform/StringListStore.kt` | KV 门面 4 函数 + `platformTodayYmd` expect | 16、18、20、22、27、29、34 |
| `commonMain/.../platform/WebBridgeHost.kt` | JS 桥契约 + 方法常量 + 注册表 + 默认/Stub 宿主 | 6-9、12-26、29-38、44-94、100-116 |
| `androidMain/.../platform/AndroidActivityContext.kt` | Android 独有：前台 Activity 持有器 | 7-20 |
| `androidMain/.../platform/PlatformBackHandler.android.kt` | actual→androidx BackHandler | 11 |
| `androidMain/.../platform/PlatformOpen.android.kt` | 4 个 Android actual | 15-22、24-28、30-42、44-60 |
| `androidMain/.../platform/PlatformCameraScan.android.kt` | CameraX+MLKit 实现 | 46-100、102-125、127-229 |
| `androidMain/.../platform/PlatformWebView.android.kt` | 系统 WebView 实现 | 19、21-38、40-45、47-122 |
| `androidMain/.../platform/StringListStore.android.kt` | platformTodayYmd actual | 7-8 |
| `iosMain/.../platform/PlatformBackHandler.ios.kt` | actual（空实现） | 10-11 |
| `iosMain/.../platform/PlatformOpen.ios.kt` | 4 个 iOS actual | 16-19、21-23、25-44、57-58 |
| `iosMain/.../platform/PlatformCameraScan.ios.kt` | AVFoundation 实现 | 49-96、98-103、105-188 |
| `iosMain/.../platform/PlatformWebView.ios.kt` | WKWebView 实现 | 24、34-52、56-85、99-128、139-187 |
| `iosMain/.../platform/StringListStore.ios.kt` | platformTodayYmd actual | 7-11 |
| `ohosMain/.../platform/PlatformBackHandler.ohos.kt` | actual（空实现） | 10-11 |
| `ohosMain/.../platform/PlatformOpen.ohos.kt` | 4 个 OHOS actual（全降级） | 6-8、11-13、15-19、21-22 |
| `ohosMain/.../platform/PlatformCameraScan.ohos.kt` | 缺口占位 UI | 16-26、28-30、32-52 |
| `ohosMain/.../platform/PlatformWebView.ohos.kt` | 缺口占位 UI | 27-29、31-57 |
| `ohosMain/.../platform/StringListStore.ohos.kt` | platformTodayYmd actual（UTC 近似） | 11-33 |

ohos actual 一份覆盖 ohosArm64 + ohosX64 两个 ABI，因为根接线把两个 leaf 都挂在 `ohosMain` 下（`build.gradle.kts:124-126`）。

### 3.2 `PlatformBackHandler`

```kotlin
@Composable
expect fun PlatformBackHandler(enabled: Boolean = true, onBack: () -> Unit)   // PlatformBackHandler.kt:6-9
```

- 参数：`enabled` 是否拦截（默认 true，Android 透传给 BackHandler）；`onBack` 拦截回调。返回：无。副作用：仅注册/注销组合栈里的回调（Android）。
- 被谁调用：`feature/auth/src/commonMain/kotlin/com.example.my_kmp_project/feature/auth/LoginScreen.kt:50`（仅当 `onBack != null`）；`feature/web/src/commonMain/kotlin/com.example.my_kmp_project/feature/web/InAppWebScreen.kt:31-35`（先试 `navigation.goBack()` 再退页）。
- 三端行为：**Android**＝一行透传 `BackHandler(enabled, onBack)`（`PlatformBackHandler.android.kt:11`）；**iOS**＝空函数体，注释说明滑返回由导航栈负责（`PlatformBackHandler.ios.kt:10-11`）；**OHOS**＝同样空实现（`PlatformBackHandler.ohos.kt:10-11`）。→ 在 iOS/OHOS 上该 API 是**语义上的 no-op**，返回全靠壳内 UI（`MineTopBar` 等）。

### 3.3 `PlatformOpen`（4 个 expect 顶层函数）

expect 声明在 `PlatformOpen.kt:4,6,8,14`。

**`fun openUrl(url: String)`** — 用系统浏览器打开外链。

| 端 | 行为 | 实证 |
|----|------|------|
| Android | `AndroidAppContext.applicationContext` 缺失**静默 return**；否则 `ACTION_VIEW + FLAG_ACTIVITY_NEW_TASK`，`runCatching` 吞异常 | `PlatformOpen.android.kt:15-22`（上下文 import 在 13 行，来自 `:core:storage`） |
| iOS | `UIApplication.sharedApplication.openURL`，非法 URL 静默 return | `PlatformOpen.ios.kt:16-19` |
| OHOS | **stub：仅 `println`**，注释自陈 Degraded | `PlatformOpen.ohos.kt:3-8` |

副作用：拉起外部应用（Android/iOS）。被谁调用：**expect 版 openUrl 未找到任何调用点**——全仓 `openUrl(` 命中都是 `WebBridgeHost.openUrl` 接口方法（3.7 节），与本函数无关。属「已实现、未接线」。

**`fun copyToClipboard(text: String)`** — Android 写 `ClipData.newPlainText("text", …)` 主剪贴板（`PlatformOpen.android.kt:24-28`，无上下文静默 return）；iOS 写 `UIPasteboard.generalPasteboard.string`（`PlatformOpen.ios.kt:21-23`）；OHOS 仅 println 文本长度（`PlatformOpen.ohos.kt:11-13`，注释「no clipboard API wired yet (Degraded)」）。**未找到调用点**（feature/mine 的「已复制订单号」只是 Toast：`feature/mine/src/commonMain/kotlin/com.example.my_kmp_project/feature/mine/MineRouteHost.kt:194`，并未真的写剪贴板）。

**`fun showPlatformToast(message: String)`** — 本模块被调用最多的 API：模块外 **116 个调用点、19 个文件**（grep 统计）。

| 端 | 行为 | 实证 |
|----|------|------|
| Android | trim 空串丢弃；不在主线程则 `Handler(mainLooper).post`，`Toast.LENGTH_SHORT` | `PlatformOpen.android.kt:30-42` |
| iOS | 先 `println("[iOS toast] …")`（28），再取最顶层 VC（`topViewController()` 46-55，keyWindow→rootVC→presented 链）弹**无按钮 UIAlertController**，1.5s 后自动 dismiss（37-42）；找不到 presenter 静默放弃（30） | `PlatformOpen.ios.kt:25-44` |
| OHOS | **只有 println，无任何 UI**（降级但函数注释未标 Degraded） | `PlatformOpen.ohos.kt:15-19` |

模块内调用：`WebBridgeHost.kt:68,74`（bridge showToast/openNative）、`PlatformCameraScan.ohos.kt:24`。代表调用点：`feature/home/.../HomeScreen.kt:48`、`feature/media/.../VideoRouteHost.kt:145`、`composeApp/src/androidMain/.../NativeJetpackTabs.kt:634`。

**`fun openSystemNotificationSettings(): String?`** — 返回契约（`PlatformOpen.kt:10-14` KDoc）：null＝已跳转；非 null＝该平台把跳转 stub 掉了、返回提示文案。Android 走 `ACTION_APP_NOTIFICATION_SETTINGS`（API26+）或 legacy action（低版本带 `app_package`/`app_uid`），成功返回 null、异常返回失败文案（`PlatformOpen.android.kt:44-60`）；iOS 恒返回 `"Deferred · iOS 请在系统设置中开启本应用通知"`（`PlatformOpen.ios.kt:57-58`）；OHOS 恒返回 `"Deferred · OHOS 系统通知设置未接入"`（`PlatformOpen.ohos.kt:21-22`）。**未找到调用点**——三端实现了 expect，但 UI 层没人触发它。

### 3.4 `StringListStore`（common 门面 + 一个 expect）

`StringListStore.kt` 全文 34 行，核心是命名 KV 视图：

```kotlin
private const val PLATFORM_PREFS_NAME = "home_favorites"        // StringListStore.kt:16
private val platformPrefsStore by lazy { createKeyValueStore(PLATFORM_PREFS_NAME) }  // :18
fun loadStringList(key: String): List<String>?                  // :20
fun saveStringList(key: String, values: List<String>)           // :22
fun loadString(key: String): String?                            // :27
fun saveString(key: String, value: String)                      // :29
expect fun platformTodayYmd(): String                           // :34
```

- 副作用（写什么存储）：`:core:storage` 的 `KeyValueStore` 契约（`core/storage/src/commonMain/kotlin/com.example.my_kmp_project/core/storage/KeyValueStore.kt:28-52`，工厂 expect 在 `:58`）；三端落盘分别是 **Android SharedPreferences 文件 `home_favorites`（MODE_PRIVATE）**（`core/storage/src/androidMain/kotlin/com.example.my_kmp_project/core/storage/KeyValueStore.android.kt:12-16`）、**iOS NSUserDefaults suite `home_favorites`**（`core/storage/src/iosMain/kotlin/com.example.my_kmp_project/core/storage/KeyValueStore.ios.kt:14-15`）、**OHOS `OH_Preferences` NDK 文件（app sandbox，两 ABI 共用）**（`core/storage/src/ohosMain/kotlin/com.example.my_kmp_project/core/storage/KeyValueStore.ohos.kt:41-42`）。
- 语义：`loadStringList` 返回 `null`＝用户从未设置（回退默认值的依据），空列表＝真实存的值（`StringListStore.kt:12-14` 与 `KeyValueStore.kt:44-47`）。
- 被谁调用（都是 key 级差异，共用同一份 prefs 文件）：

| 调用方 | key 用途 | 实证 |
|--------|----------|------|
| Home 收藏服务 | `home_favorite_service_ids_v2` | `feature/home/src/commonMain/kotlin/com.example.my_kmp_project/feature/home/HomeFavoritesStore.kt:21,41`（读 21 / 写 41，key 定义 13） |
| Mine 功能排序/深色模式/locale/选中店铺 | 多个 key | `feature/mine/src/commonMain/kotlin/com.example.my_kmp_project/feature/mine/MinePrefsStore.kt:33,36,40,46,65,76,87,93` |
| Mine 个性化开关（`personalized_` 前缀串 key） | 护眼模式等 | `feature/mine/.../MinePersonalizedSettingsScreen.kt:33,40,49,104` |
| Community 公约「每日一次」确认 | `community_convention_ack_date`（列表存 1 个日期） | `feature/community/.../CommunityRouteHost.kt:169,228`（key 定义 `:361`） |
| Chat IM mock id 持久化工厂 | `im_biz_to_im_user_id_json` | `feature/chat/.../ImSessionApi.kt:119-120`（工厂 `createPersistentImMockUserIdRegistry` 在 `:116`；**该工厂全仓未找到调用点**，实际运行时用的是进程内版 mock 注册表——持久化链路未接线） |

**`platformTodayYmd()`** 为什么必须拆三端：要拿「设备时区的本地日历日」。
Android 用 `java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())`（`StringListStore.android.kt:7-8`）；iOS 用 `NSDateFormatter`（`StringListStore.ios.kt:7-11`）；**OHOS 是近似实现**：取 UTC epoch 毫秒除以 86400s 手工推年月日（`StringListStore.ohos.kt:11-33`，12-13 行注释自陈「Approximate local day via UTC until an OHOS calendar API is wired」）——东八区凌晨 0-8 点会返回**前一天**，直接影响 CommunityRouteHost.kt:166-171 的「每天一次」弹窗判定。这是必须明说的降级。

### 3.5 `PlatformCameraScan`（权限 + 实时扫码）

commonMain 契约（`PlatformCameraScan.kt`）：

```kotlin
enum class CameraPermissionStatus { Unknown, Granted, Denied, Unavailable }   // :6-12（Unavailable 注释 :10：平台无适配器）
interface CameraPermissionController {                                        // :14-19
    val status: CameraPermissionStatus; fun refresh(); fun requestPermission(); fun openAppSettings()
}
@Composable expect fun rememberCameraPermissionController(): CameraPermissionController  // :22
@Composable expect fun PlatformBarcodeScanner(onBarcode: (payload: String) -> Unit, modifier: Modifier = Modifier)  // :30-33
```

被谁调用：`feature/scan/src/commonMain/kotlin/com.example.my_kmp_project/feature/scan/ScanScreen.kt:55`（remember）、`:109`（判 Unavailable 走 mock 框）、`:147-153`（挂真预览）；`core/ui/src/commonMain/kotlin/com.example.my_kmp_project/core/ui/PermissionGate.kt:32-74` 消费控制器（44-49：进入即 `refresh()`，Unknown 且允许时自动 `requestPermission()`）。

**Android actual（`PlatformCameraScan.android.kt`，229 行，CameraX + MLKit）**

- `AndroidCameraPermissionController`（46-100）：状态存 `mutableStateOf`（50，Compose 可观察）。首次 `readStatus()`（79-90）：**未授权一律报 Unknown**（87-88 注释「首次请求前无法区分“拒绝过”和“没问过”」，Android RequestPermission 契约决定）。`refresh()`（55-65）只在「之前已是 Denied」时维持 Denied，否则回 Unknown——不会把 Unknown 误升级。`requestPermission()` 只是触发注入的 `request` lambda（67-69）。`openAppSettings()`（71-77）发 `ACTION_APPLICATION_DETAILS_SETTINGS` intent（runCatching 吞失败）。`onRequestResult`（92-95）由系统回调置位；`markDenied()`（97-99）**未找到调用点（死代码）**。
- `rememberCameraPermissionController`（102-125）：`rememberLauncherForActivityResult(RequestPermission)`（106-110），launcher 先于 controller 创建，用 `holder` 状态桥接回调（105、109、114）；`LaunchedEffect(Unit)`（116-123）组合时若已授权直接置 Granted。
- `PlatformBarcodeScanner`（127-229）副作用：起单线程分析 executor（136）并在 dispose 时 shutdown（139-141）；`DisposableEffect(previewView, lifecycleOwner)`（143-201）内异步取 `ProcessCameraProvider`（146、195），构建 `Preview`+`ImageAnalysis(KEEP_ONLY_LATEST)`（149-151、163-165），MLKit 解码器限 6 种码制（QR/Aztec/DataMatrix/PDF417/Code128/EAN13，152-161），绑定**后置摄像头**（185-193，`runCatching` 吞绑定失败——静默黑屏风险）；dispose 时 `unbindAll`（196-200）。**一次性语义**：`AtomicBoolean handled`（135）+ CAS（179）保证 `onBarcode` 只触发一帧；空 image 或已处理后直接 close proxy（167-171）。UI：`AndroidView(PreviewView)` FILL_CENTER/COMPATIBLE（206-219）+ 底部白字提示「将二维码/条码放入框内」（220-227）。

**iOS actual（`PlatformCameraScan.ios.kt`，188 行，AVFoundation）**

- 控制器（49-96）：`readStatus`（87-95）把 AVFoundation 四态映射为 Unknown(NotDetermined)/Granted/Denied(Denied+Restricted)；`requestPermission`（59-80）仅 NotDetermined 才弹系统授权，回调切主线程置状态（65-74）；`openAppSettings`（82-85）开 `UIApplicationOpenSettingsURLString`。`rememberCameraPermissionController`（98-103）remember + LaunchedEffect refresh。
- 扫码（105-188）：`AVCaptureSession` + 默认视频设备输入（137-143）+ `AVCaptureMetadataOutput`，delegate 在自建串行队列收码对象（144-149）；**`metadataObjectTypes` 只设 QR 一种**（148）——与 Android 的 6 种码制是真实能力差（`PlatformCameraScan.kt:27` KDoc 写「iOS: AVFoundation metadata」属实，但未提示只支持 QR）。preset High（150）；`startRunning` 放后台队列避免卡主线程（151-153）；dispose 时 `stopRunning`（154-156）。delegate 命中后同样**一次性**（`handled` 状态 112、120、126），主线程 stop session 后回调 `onBarcode`（127-131）。预览用 `UIKitView` 挂 `AVCaptureVideoPreviewLayer`，update 块同步 layer frame（160-178）；提示文案「将二维码放入框内」（179-186）。

**OHOS actual（`PlatformCameraScan.ohos.kt`，52 行）——诚实缺口**

- `OhosCameraPermissionController.status` 是编译期常量 `Unavailable`（17，`override val` 直接赋值），`refresh/requestPermission` 为 `Unit`（19、21），`openAppSettings` 只 Toast「OHOS 相机权限/扫码未接入」（23-25）。
- `PlatformBarcodeScanner` 渲染占位文案「OHOS 相机扫码未接入（registry: missing）」（32-52，文案在 44-47），并把 `onBarcode` 参数以 `@Suppress("UNUSED_EXPRESSION")` 空引用（49-50）——**回调永远不会触发**。
- 上层对策：`ScanScreen.kt:109-123` 在 Unavailable 时渲染 WysScan 粉色取景框 + 「模拟成功」按钮，伪造 payload `"myai://mall/orders?id=A1024"`（`ScanScreen.kt:116`）。**OHOS 扫码是 mock，不是能力。**

### 3.6 `PlatformWebView` + `WebViewNavigationHandle`

commonMain（`PlatformWebView.kt`，30 行）：

```kotlin
class WebViewNavigationHandle {                          // :10
    var canGoBack: Boolean by mutableStateOf(false); internal set   // :11-12  —— Compose State，UI 可观察；外部只读
    internal var goBackImpl: (() -> Boolean)? = null     // :14 —— 三端 actual 注入的退栈实现
    fun goBack(): Boolean = goBackImpl?.invoke() == true // :17 —— true＝WebView 已消费本次返回
}
@Composable expect fun PlatformWebView(url: String, bridge: WebBridgeHost,
    modifier: Modifier = Modifier, navigation: WebViewNavigationHandle? = null)  // :25-30
```

参数：`url` 加载地址；`bridge` JS 桥宿主（见 3.7）；`navigation` 可选，传入即由 actual 接管 `goBackImpl/canGoBack`。副作用：起一个系统级 Web 内核、加载网络页面。

- 被谁调用：`feature/web/.../InAppWebScreen.kt:56-61`（唯一组合调用点；句柄创建于 `:29`，`PlatformBackHandler` 于 `:31-35` 实现「Web 历史优先、栈后于页」的返回序）；InAppWebScreen 由共享壳 `composeApp/src/commonMain/kotlin/com.example.my_kmp_project/app/AppShell.kt:422` 与 Android 原生壳 `composeApp/src/androidMain/.../NativeAndroidMain.kt:605` 挂载。默认 url 是 component 的离线 data-URL fixture（`component/src/commonMain/kotlin/com.example.my_kmp_project/component/webview/WebViewComponent.kt:20-32`，如 `NativeAndroidMain.kt:104`）。

**Android actual（`PlatformWebView.android.kt`，122 行）**

- JS 桥：`addJavascriptInterface(AndroidJsBridge, "AndroidBridge")`（40-45、86），`@JavascriptInterface invoke` **同步返回** native 结果给 JS（44）——H5 侧 `NativeBridge.invoke(m,p)` 能拿到真实返回值。页面加载完成后注入 bootstrap（21-38、95）把 `window.NativeBridge` 转发到 `AndroidBridge.invoke` 并派发 `NativeBridgeReady`。
- 设置：JS 打开（82，整函数 `@SuppressLint("SetJavaScriptEnabled")` 47）、DOM Storage（83）、默认缓存（84）、混合内容兼容模式（85）。
- 返回栈：`DisposableEffect(navigation)` 注入 `goBackImpl`（58-67），dispose 时清空句柄（68-72）；`onPageStarted/onPageFinished/onReceivedError(mainFrame)` 三处刷新 `canGoBack`（88-107）。
- `bridge.openUrl(url)` 在 factory（109）与 update（117）都会记录；update 仅在 URL 变化时重加载（113-120）。

**iOS actual（`PlatformWebView.ios.kt`，187 行）**

- 桥名 `NativeBridge`（24）：JS 侧走 `WKScriptMessageHandler`——**异步 fire-and-forget**：`invoke` 除 `getToken` 外一律 postMessage 后同步返回 `'{"ok":true}'`（56-77，兜底返回在 72），native 的返回值 H5 **拿不到**。这是与 Android 桥的真实契约差异。
- `getToken` 的答案来自**组合期快照**：`remember(bridge) { bridge.invokeBridge(GetToken, null) }`（34-36）注入成 `window.__NATIVE_TOKEN__`（58）；同一 bridge 实例存续期间 token 轮换不会刷新（坑，见第六节）。
- 消息解码（37-52）：body 为 `{method, payload}` 字典则取字段，否则把 body 字符串当 method、payload=null。
- 加载（115-128）：先 `bridge.openUrl(url)`；`data:text/html` 前缀时手工 percent-decode 后 `loadHTMLString`（117-120，注释 118 说明长 data URL 走 NSURLRequest 会失败）；否则 `loadRequest`。percent 解码是手写的 `decodeUriComponent`（165-187）：`'+'`→空格（176-179）、非法 hex 会让 `toInt(16)` 抛出未捕获异常（173）。
- 返回栈：`didFinishNavigation` 刷 `canGoBack`（91-95）；`goBackImpl`（99-113）成功后同步回写 `canGoBack`；`UIKitView` update 再刷一次（130-136）。
- JS 字符串注入前做了转义（`toJsStringLiteral` 139-155），token 含引号/换行不破坏脚本。

**OHOS actual（`PlatformWebView.ohos.kt`，58 行）——诚实缺口**

- 仍然调用 `bridge.openUrl(url)`、把 `canGoBack=false`、`goBackImpl={false}`（27-29），页面本体是三行占位文案：「OHOS WebView 未接入 Compose」+ url 回显 + 「能力状态：missing（registry）。请勿将本页视为验收通过。」（31-57，文案在 40、51）。头注释（16-19）说明 `harmonyApp/` 里有 ArkTS WebPage 但未桥进 Compose。

### 3.7 `WebBridgeHost.kt`（H5↔Native 契约，116 行）

```kotlin
interface WebBridgeHost {                                  // :6-9
    fun openUrl(url: String)                               // :7  宿主记录/处理本次进入的 url
    fun invokeBridge(method: String, payload: String?): String?  // :8  JS→native 同步调用
}
```

**`object WebBridgeMethods`（12-26）**：8 个方法名常量 + `all` 列表（22-25）：close/getToken/openNative/pay/showToast/getEnvironment/getUserInfo/refreshDashboard。

**`object WebBridgeRegistry`（29-38）**：进程级全局 `mutableMapOf`（30）；`register` 后写覆盖先写（32-34）；`invoke` 未注册返回 null（36-37）。无锁、无注销、无按页面隔离——见第六节坑。当前唯一注册方：`feature/home/src/commonMain/kotlin/com.example.my_kmp_project/feature/home/HomeWebHandlers.kt:18-44`（注册 RefreshDashboard/ShowToast/GetEnvironment/GetUserInfo 四个，行 21/25/31/34），且 `register()` **只在 Android 原生壳被调用一次**（`composeApp/src/androidMain/.../NativeAndroidMain.kt:90`）；共享 Compose 壳（AppShell 路径）不注册，因此同一 H5 页在两种壳下 `getEnvironment` 返回不同（registry 版 `{"platform":"android"}` HomeWebHandlers.kt:31-32 vs 默认版 `{"platform":"kmp"}` WebBridgeHost.kt:77-78）。

**`class DefaultWebBridgeHost(onClose: () -> Unit = {}, onOpenNative: (String?) -> Unit = {})`（44-94）**：`PlatformWebView` 各端从 JS 调 `invokeBridge`（Android 经 `PlatformWebView.android.kt:44`，iOS 经 `PlatformWebView.ios.kt:49`）。分派规则：**registry 优先**（56），命中非 null 即返回；否则走内置 switch：

| method | 行为 | 返回 | 副作用 | 实证 |
|--------|------|------|--------|------|
| close | 调 `onClose()` | `{"ok":true}` | 触发上层关页回调 | `:58-61` |
| getToken | 读 `AccountFacade.current().token` | 非空 token 或 null | 首次触发账号门面惰性水合（读账号 KV、并可能 `NetworkFacade.bindAccessToken`：`core/account/src/commonMain/kotlin/com.example.my_kmp_project/core/account/AccountFacade.kt:44-47,178-188`） | `:62-65` |
| openNative | 调 `onOpenNative(payload)` + `showPlatformToast` | `{"ok":true}` | Toast + 上层跳转回调 | `:66-70` |
| pay | **硬编码不可用，未接线** | `{"ok":false,"message":"pay unavailable"}` | 无 | `:71-72` |
| showToast | `showPlatformToast(payload)` | `{"ok":true}` | 系统 Toast（Android/iOS）或 println（OHOS） | `:73-76` |
| getEnvironment | 固定串 | `{"platform":"kmp","app":"my_kmp_project"}` | 无 | `:77-78` |
| getUserInfo | 按登录态拼 JSON 字符串 | `{"loggedIn":…}` | 读 `AccountFacade.current()`（同 getToken 的副作用） | `:79-88`（手工字符串插值，displayName 含 `"` 会产出非法 JSON——坑） |
| refreshDashboard | 默认实现无动作 | `{"ok":true}` | 无（真实钩子靠 registry 注册版） | `:89-90` |
| 其他 | — | null | 无 | `:91` |

另有 `var lastOpenedUrl: String? private set`（48-49），`openUrl` 只赋值不跳转（51-53）——纯观察记录位，供测试/调试。

**`class StubWebBridgeHost`（100-116）**：文件头注释（96-99）明言「dev-only no-op，不得当作 WebView 已完成的替身」；`invokeBridge` 返回短串（`"ok"`、`null`、`"stub"`、`"unavailable"`，108-115），与 Default 的 JSON 形态不同。**全仓未找到调用点**——目前是死代码/预留。

### 3.8 `AndroidActivityContext`（Android-only，非 expect/actual）

`object AndroidActivityContext`（`AndroidActivityContext.kt:7-20`）：`@Volatile var currentActivity: Activity?`（8-9）、`install(activity)`（11-13）、`clear(activity)` 引用相等才清空（15-19）。类注释（6）声明服务对象是「需要 Activity 上下文的 SDK 调用（pay）」。

- 写入方：`composeApp/src/androidMain/kotlin/com.example.my_kmp_project/MainActivity.kt:27,51,55`。
- **读取方：全仓 `currentActivity` 除本文件外零命中——pay SDK 尚未消费它。**真实状态：已布防、未接线。

### 3.9 expect/actual 机制在 core:platform 的总账

| expect（commonMain） | Android | iOS | OHOS（两 ABI 共用） | 为什么必须拆 |
|---|---|---|---|---|
| `PlatformBackHandler.kt:6` | 透传 BackHandler（`PlatformBackHandler.android.kt:11`） | **空实现**（`PlatformBackHandler.ios.kt:10-11`） | **空实现**（`PlatformBackHandler.ohos.kt:10-11`） | 只有 Android 有系统返回键语义 |
| `PlatformOpen.kt:4` openUrl | ACTION_VIEW intent | UIApplication.openURL | println stub | 系统浏览器 API 各不相同；OHOS 未接 |
| `PlatformOpen.kt:6` copyToClipboard | ClipboardManager | UIPasteboard | println stub | 剪贴板是纯平台 API；OHOS 未接 |
| `PlatformOpen.kt:8` showPlatformToast | Toast（含跨线程 post） | UIAlertController 1.5s 自动关 | println | 无跨端 toast 组件；iOS 借 alert、OHOS 无 UI |
| `PlatformOpen.kt:14` openSystemNotificationSettings | Settings intent（分 API 26 上/下） | 恒返回 Deferred 提示 | 恒返回 Deferred 提示 | iOS 无「通知设置页」深链，OHOS 未接 |
| `PlatformCameraScan.kt:22,30` | CameraX+MLKit，6 码制 | AVFoundation，**仅 QR** | **Unavailable 占位** | 相机管线三端完全不同 |
| `PlatformWebView.kt:25` | android.webkit.WebView（同步桥） | WebKit.WKWebView（异步桥+token 快照） | **占位文案页** | Web 内核无公共 API |
| `StringListStore.kt:34` platformTodayYmd | SimpleDateFormat | NSDateFormatter | UTC 手工推算（近似） | 日历 API 平台各异；OHOS 未接日历 |
| `AppUuid.kt:10`（core:base） | cpf actual | cpf actual | arm64=cpf / x64=stub | 产物缺口，见 2.5 |

### 3.10 build.gradle.kts（core/platform）补充事实

androidMain 的 CameraX/MLKit 全部是 `implementation`（`core/platform/build.gradle.kts:26-36`），类型不外泄——feature 侧只见 `PlatformBarcodeScanner`/`CameraPermissionController` 抽象。这正是文件头注释（4-5 行）说的「扫码只在 androidMain 出现，不污染 composeApp 和 feature」的实证。

---

## 四、core:analytics

| 路径 | 作用 | 关键行号 |
|------|------|----------|
| `core/analytics/build.gradle.kts` | 仅两插件 + namespace，零依赖 | 4-6、8-10；文件头注释（1）称「当前为占位骨架，真实 gRPC 上报见 M4」 |
| `core/analytics/src/commonMain/kotlin/com.example.my_kmp_project/core/analytics/Tracker.kt` | 全模块唯一代码：`fun analyticsInfo(): String = "core-analytics ready"` | 5（共 5 行） |

- **文件名叫 `Tracker.kt`，但不存在 `Tracker` 类型、没有任何埋点事件模型/上报通道/协程**；`analyticsInfo()` 全仓未找到调用点（仅 `:composeApp` 把模块挂上依赖图：`composeApp/build.gradle.kts:127`）。
- 真实状态：**纯占位**。所谓「埋点上报」在本模块尚未开始，M4 属于 openspec 计划（注释口径，非代码事实）。

---

## 五、真实状态总表（stub / mock / 未接线，逐条实证）

| 项 | 状态 | 证据 |
|----|------|------|
| `BaseUtils`（core:base） | 占位，零调用 | `BaseUtils.kt:3-6`；grep 无调用点 |
| `stripHtmlRough` / `extractImageUrlsFromHtml` | 已实现、**未接线**（零调用） | `HtmlUtils.kt:8,20`；grep 无调用点 |
| `AppUuid.parseOrNull` | 生产零调用，仅测试 | `AppUuidTest.kt:25,37-39` |
| `AppUuid.random` ohosX64 分支 | 伪随机 stub（非产品 SoT） | `AppUuid.ohosX64.kt:5-8` |
| `openUrl` / `copyToClipboard` / `openSystemNotificationSettings` | Android/iOS 真实实现，但**全仓无调用者**；OHOS 全为 println/Deferred 降级 | `PlatformOpen.ohos.kt:6-8,11-13,21-22`；grep 无调用点 |
| `showPlatformToast` OHOS 分支 | 降级：仅 stdout，无 UI | `PlatformOpen.ohos.kt:15-19` |
| OHOS 扫码 | 缺口：status 恒 Unavailable、回调永不触发；上层 ScanScreen 用 mock payload 兜底 | `PlatformCameraScan.ohos.kt:17,49-50`；`ScanScreen.kt:116` |
| OHOS WebView | 缺口：占位文案页，明示「请勿视为验收通过」 | `PlatformWebView.ohos.kt:40,51` |
| iOS 扫码 | 能力缩水：仅 QR（Android 支持 6 码制） | `PlatformCameraScan.ios.kt:148` vs `PlatformCameraScan.android.kt:152-161` |
| `markDenied()` | 死代码（零调用） | `PlatformCameraScan.android.kt:97-99` |
| `StubWebBridgeHost` | 预留 dev-only，零调用 | `WebBridgeHost.kt:96-99` |
| bridge `pay` | 硬编码 `pay unavailable`，未接 component.pay | `WebBridgeHost.kt:71-72` |
| `AndroidActivityContext` | 只写不读：pay SDK 未消费 | 写 `MainActivity.kt:27,51,55`；读方零命中 |
| `platformTodayYmd` OHOS | UTC 近似，东八区凌晨差一天 | `StringListStore.ohos.kt:12-13` |
| IM mock 注册表持久化工厂 | `createPersistentImMockUserIdRegistry` 零调用，IM id 目前仅进程内记忆 | `ImSessionApi.kt:116-121` |
| `analyticsInfo()`（core:analytics） | 纯占位，零调用 | `Tracker.kt:3-5` |

---

## 六、改动入口与坑

1. **新增 expect 的固定路径**：commonMain 声明 + android/ios/ohos 三份 actual；`ohosMain` 一份自动覆盖两个 ABI（`build.gradle.kts:124-126`）。OHOS 端碰到没有产物的库（参考 2.5），要么建中间 source set（仿 `core/base/build.gradle.kts:15-25`），要么写 stub actual——绝不把能力库塞进 commonMain。
2. **`:core:storage` 是 `implementation` 不是 `api`**（`core/platform/build.gradle.kts:19`）：消费方拿不到 `KeyValueStore` 类型，`StringListStore` 四函数就是唯一入口；想扩 KV 能力先想清楚要不要把 storage 升级成 api。
3. **所有 StringListStore 数据同挤一个 `home_favorites` 文件**（`StringListStore.kt:16`）：收藏、Mine 设置、社区确认、（若接线的）IM 映射都在里面；新增 key 注意命名冲突，且契约无 `clear()`/`contains()`（`KeyValueStore.kt:11-14`）。
4. **WebBridgeRegistry 是进程级全局**（`WebBridgeHost.kt:29-38`）：register 覆盖式、无锁、无注销、不分页面；`HomeWebHandlers.register()` 只在 Android 原生壳跑（`NativeAndroidMain.kt:90`），共享壳路径不跑——调试 H5 桥行为差异先查这里。
5. **桥的同步性三端不等价**：Android JS 能同步拿到 native 返回值（`PlatformWebView.android.kt:44`）；iOS 除 getToken 外 JS 侧永远拿 `{"ok":true}`（`PlatformWebView.ios.kt:72`），H5 若依赖返回值要用 showToast→回调或轮询，不能照抄 Android 写法。
6. **iOS token 是快照**（`PlatformWebView.ios.kt:34-36`，`remember(bridge)`）：登录/续期后不换 bridge 实例就不刷新；`DefaultWebBridgeHost` 的 getToken 分支反而是实时读（`WebBridgeHost.kt:62-65`）——两种桥时序混用会出现「JS 常量 vs native 实时」不一致。
7. **getUserInfo 手工拼 JSON**（`WebBridgeHost.kt:84-86`）：displayName 带引号即产出非法 JSON；改这里建议先上序列化库（模块内目前没有）。
8. **iOS percent-decode 手写字节**（`PlatformWebView.ios.kt:165-187`）：`+`→空格、奇数/越界 `%`、非法 hex 抛未捕获 `NumberFormatException`（173），只兜 data-URL fixture（`WebViewComponent.kt:20-32`）够用，外部任意 data URL 需谨慎。
9. **OHOS `platformTodayYmd` 用 UTC**（`StringListStore.ohos.kt:11-33`）：影响「每日一次」类判定（`CommunityRouteHost.kt:166-171`）；接 OHOS 日历 API 时只改这一个 actual。
10. **Toast 语义**：空串全端静默（android 33 / ios 27 / ohos 17 行）；Android 允许非主线程调用（`PlatformOpen.android.kt:37-41`），iOS 必须能取到 keyWindow，否则丢；iOS 连续触体会叠 alert 队列（无去抖）。
11. **扫码一次性语义**：Android `AtomicBoolean` CAS（`PlatformCameraScan.android.kt:179`）与 iOS `handled` 标志（`PlatformCameraScan.ios.kt:120-126`）都只回调一次，重扫需重组（ScanScreen 用 `lastPayload=null` 复位，`ScanScreen.kt:100-102`）。Android 相机绑定失败被 `runCatching` 吞掉（`PlatformCameraScan.android.kt:185`），排障先看权限与 LifecycleOwner。
12. **改 Kotlin/Compose 后 OHOS 需重新 publish 二进制**（AGENTS 契约；本模块被 `:composeApp` implementation，`composeApp/build.gradle.kts:129`），否则 `harmonyApp` 里跑的还是旧 `libkn.so`。
13. **`:core:analytics` 想动真埋点**：先补依赖（现在零依赖，`core/analytics/build.gradle.kts`），再决定 Tracker 形态——当前 `Tracker.kt` 里没有任何 Tracker 可继承，重命名/新建类型不影响编译面（无调用者）。
14. **CI `androidOnly` 路径**：`core/base/build.gradle.kts:11,22-25` 与根 `build.gradle.kts:57,94,118` 共同保证 `-PandroidOnly=true` 只建 androidTarget；给 `:core:base` 加新 source-set 接线时照抄这个 guard，否则 GitHub Actions（AGENTS：Android-only pipeline）会红。

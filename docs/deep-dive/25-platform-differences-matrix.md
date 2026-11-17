# 25 · 三端差异性总表（Android、iOS、HarmonyOS）

> 本册是**横向矩阵册**：把散落在 09（构建与配置）、22（装配与路由）、23（三端原生壳）里的事实，按「三端到底差在哪」这一根轴重排成可查表。
> 需要机制解释请回看那三册；本册只在必要处引用它们，不复述。
> 交叉引用写法：详见 `docs/deep-dive/09-build-and-config.md`、`docs/deep-dive/22-composeapp-assembly-routing.md`、`docs/deep-dive/23-native-shells.md`。

---

## 0. 本文要回答的问题与实测方法

### 0.1 要回答的六个问题

1. 一个模块到底被编进了哪几个 target？哪些能力代码住在「中间源集」里、为什么不能待在 `commonMain`？
2. 每条三方依赖坐标在 Android、iOS、ohosArm64、ohosX64 四条线上分别是「可用、占位、还是根本拿不到」？
3. 全仓 `expect` 声明一共多少条？按能力分组后，三端各自的实现质量是「真实、stub、还是 placeholder」？
4. `ohosArm64` 与 `ohosX64` 差在哪，为什么 `ohosX64` 连一条 Ktor klib 都解析不到？
5. 这些差异落到运行时行为上是什么：HTTP 失败类型、KV 落盘、日志、深链 scheme、返回栈？
6. 产物形态与链接参数的差异，以及哪些差异是「等上游 klib 的临时妥协」、哪些是「设计决定」？

### 0.2 实测方法（先给命令，再给数）

以下命令全部在本仓根目录 `/Users/qpy/Desktop/github/my_kmp_project` 实跑，数字为真实输出。

```bash
# A. expect 声明总数与文件数
grep -rnE "(^| )expect (fun|val|var|class|object|interface|enum)" --include="*.kt" . \
  | grep -v "/build/" | wc -l      # => 27
grep -rlE "(^| )expect (fun|val|var|class|object|interface|enum)" --include="*.kt" . \
  | grep -v "/build/" | wc -l      # => 17

# B. actual 声明总数与承载文件数
grep -rnE "(^| )actual (fun|val|var|class|object|internal object|internal fun)" \
  --include="*.kt" . | grep -v "/build/" | wc -l          # => 89
grep -rlE "(^| )actual (fun|val|var|class|object)" \
  --include="*.kt" . | grep -v "/build/" | wc -l          # => 50

# C. 五个能力型中间源集各自的 .kt 文件数
for d in networkKtorMain imageCoilMain uuidCpfMain settingsMain chartsVicoMain; do
  echo "$d: $(find . -path "*/src/$d/*" -name "*.kt" -not -path "*/build/*" | wc -l)"
done

# D. HiLog 是否真的被 Kotlin 侧绑定
grep -rn "hilog\|HiLog\|OH_LOG" --include="*.kt" . | grep -v "/build/" | wc -l   # => 0
```

### 0.3 实测数字速览

| 量 | 值 | 口径 |
|---|---|---|
| `expect` 声明 | **27 条** | 正则 A，排除 `build/` |
| 含 `expect` 的文件 | **17 个** | 正则 A 的 `-l` 版本 |
| `actual` 声明 | **89 条** | 正则 B |
| 含 `actual` 的文件 | **50 个** | 正则 B 的 `-l` 版本 |
| 能力型中间源集 | **5 个** | `networkKtorMain`、`imageCoilMain`、`uuidCpfMain`、`settingsMain`、`chartsVicoMain` |
| 中间源集内 `.kt` 文件 | 3、1、1、1、1（共 7 个） | 命令 C |
| Kotlin 侧 HiLog 绑定 | **0 处** | 命令 D；`-lhilog_ndk.z` 只是链接选项，见 `composeApp/build.gradle.kts:85` |
| 二进制的唯一产出模块 | **1 个**（`:composeApp`） | `build.gradle.kts:94-96` 注释与 `component/build.gradle.kts:28` |

一个立刻能看出的结构性事实：**27 条 `expect`、50 个 actual 文件、89 条 actual 声明**——平均每条 expect 要配约 3.3 个 actual（`27 × 3.3 ≈ 89`），差异正好被这个比例撑开。

---

## 1. target 与源集矩阵

### 1.1 六个 target 从哪儿来：三套接线，不是一个来源

同一份 `commonMain` 会被编成 6 个 target，但**声明位置有三个**，这是查差异时最容易踩空的点：

| 模块类别 | target 声明处 | 命中范围 | 备注 |
|---|---|---|---|
| `:core:*`、`:feature:*` | `build.gradle.kts:87-102` | 根 `subprojects {}` 里被 `libraryModulePaths` 筛中（`build.gradle.kts:35`） | `androidTarget`（:87）、`iosX64/iosArm64/iosSimulatorArm64`（:97-99）、`ohosArm64/ohosX64`（:100-101） |
| `:component` | `component/build.gradle.kts:20-34` | 自己完整声明 | 模块路径是 `:component`，**不落在 `:core:` 前缀里**，所以根接线不命中——见 `component/build.gradle.kts:6-8` |
| `:composeApp` | `composeApp/build.gradle.kts:30-53` | 自己完整声明 | 唯一带 `binaries` 的模块 |
| `:ohosAggregate` | `ohosAggregate/build.gradle.kts:11-13` | **无 target** | 空占位插件块；`ohosAggregate/build.gradle.kts:4` 明写「Today `:composeApp` still owns `libkn.so` linking」 |

`-PandroidOnly=true` 会把 native 那条腿整条砍掉：`build.gradle.kts:57`、`composeApp/build.gradle.kts:17`、`component/build.gradle.kts:17`、`core/network/build.gradle.kts:13` 各读一次这个开关。

源集层级（`iosMain`、`ohosMain` 这两个「聚合层」由谁创建）也分两类：

| 层级 | 创建者 | 出处 |
|---|---|---|
| `:core:*`、`:feature:*` 的 `iosMain`、`ohosMain` | 根共享接线 `maybeCreate` | `build.gradle.kts:118-127` |
| `:component` 的 `iosMain`、`ohosMain` | 模块自己 `maybeCreate` | `component/build.gradle.kts:49-61` |
| `:composeApp` 的 `iosMain`、`ohosMain` | 模块自己 `sourceSets.create` | `composeApp/build.gradle.kts:161-180` |

### 1.2 五个能力型中间源集：谁覆盖哪几端

这五个源集的存在理由**全部是「产物缺口」**，不是架构洁癖。它们的共同写法是：`dependsOn(commonMain)` + 只把依赖挂到**能解析到该 klib 的 target** 上。

| 中间源集 | 所在模块 | 挂到的 target | 未挂到（=缺口） | 声明出处 |
|---|---|---|---|---|
| `networkKtorMain` | `:core:network` | `androidMain`、`iosMain`、`ohosArm64Main` | **`ohosX64Main`** | `core/network/build.gradle.kts:48-58`（源集与 Ktor 依赖）、`:60-65`（android）、`:67-73`（ios）、`:74-80`（ohosArm64 加 CIO） |
| `imageCoilMain` | `:core:ui` | `androidMain`、`iosMain`、`ohosArm64Main` | **`ohosX64Main`** | `core/ui/build.gradle.kts:24-27`（源集 + `coil-compose`）、`:28-31`（android + okhttp 引擎）、`:33-36`（ios + ktor3 引擎）、`:37-40`（ohosArm64 + ktor3 引擎） |
| `uuidCpfMain` | `:core:base` | `androidMain`、`iosMain`、`ohosArm64Main` | **`ohosX64Main`** | `core/base/build.gradle.kts:15-20`（源集 + `libs.uuid`）、`:21`（android）、`:22-25`（ios 与 ohosArm64） |
| `settingsMain` | `:core:storage` | `androidMain`、`iosMain` | **两个 OHOS 架构都不挂** | `core/storage/build.gradle.kts:18-23`（源集 + `multiplatform-settings`）、`:25`（android）、`:28`（ios 用惰性匹配） |
| `chartsVicoMain` | `:feature:home` | `androidMain`、`iosMain` | **两个 OHOS 架构都不挂** | `feature/home/build.gradle.kts:14-20`（源集 + `libs.vico.compose`）、`:21`（android）、`:23`（ios 惰性匹配） |

两个关键细节：

1. `settingsMain`、`chartsVicoMain` 挂 ios 时用的是 `matching { it.name == "iosMain" }` 而不是 `getByName`，源码注释写明原因：**`androidOnly` CI 里根本没有 `iosMain`**（`core/storage/build.gradle.kts:27`、`feature/home/build.gradle.kts:22`）。`imageCoilMain` 同理（`core/ui/build.gradle.kts:32`）。
2. `uuidCpfMain` 用的是 `getByName("iosMain")`（`core/base/build.gradle.kts:23`），但它被包在 `if (!androidOnly)` 里（同文件 `:22`），所以同样安全。**两种防御写法并存**——这是差异，也是后续新增源集时要选的口径。

### 1.3 逐模块落位表（28 个 Gradle 模块的 target 与源集实况）

`find` 实测的 `src/` 目录清单（不含 `androidUnitTest` 之类测试目录的重复列出）：

| 模块 | 实有源集 | 与三端差异的关系 |
|---|---|---|
| `:core:base` | `commonMain`、`uuidCpfMain`、`ohosX64Main`、`commonTest` | 唯一一个「commonMain + 中间源集 + **架构专属目录**」三件套：`core/base/ohosX64Main` 里只有 `AppUuid.ohosX64.kt` |
| `:core:network` | `commonMain`、`networkKtorMain`、`androidMain`、`iosMain`、`ohosMain`、`ohosArm64Main`、`ohosX64Main` | **差异最密集的模块**：7 个非测试源集，两端 OHOS 架构各有各的目录 |
| `:core:storage` | `commonMain`、`settingsMain`、`androidMain`、`iosMain`、`ohosMain`、`commonTest` | `ohosMain` 一份代码同时服务 arm64 与 x64（`KeyValueStore.ohos.kt:30`） |
| `:core:ui` | `commonMain`、`imageCoilMain`、`ohosX64Main` | 只有 arm64 走中间源集；x64 落独立目录 |
| `:core:platform` | `commonMain`、`androidMain`、`iosMain`、`ohosMain` | 6 条 expect 全在这里（详见 §3） |
| `:core:design` | `commonMain`、`commonTest` | 纯 commonMain；`:core:design` 还独占全部 composeResources（`composeApp/build.gradle.kts:246-252`） |
| `:core:account`、`:core:analytics`、`:core:router` | 纯 `commonMain`（+ `commonTest`） | 三端零差异；`core/analytics` 只有一个文件 `Tracker.kt` |
| `:core:database` | `commonMain`、`commonTest` | **SQL 依赖刻意不引**：`core/database/build.gradle.kts:10-13` 说明 OHOS fork 无 Room、SQLDelight klib |
| `:component` | `commonMain`、`androidMain`、`iosMain`、`ohosMain`、`commonTest` | pay、push 两组 actual 全在此（§3.13、§3.14） |
| `:feature:home` | `commonMain`、`chartsVicoMain`、`ohosMain`、`commonTest` | 图表：Vico 一条线、空 Box 一条线 |
| `:feature:media` | `commonMain`、`androidMain`、`iosMain`、`ohosMain`、`commonTest` | 播放器三端质量差异最大的模块 |
| 其余 `:feature:*`（15 个） | `commonMain`（+ 少量 `commonTest`） | 17 个 feature 里除 `home`、`media` 外全部纯共享；它们不碰平台 API |

**这张表的可查结论**：全仓只有 **1 个模块**（`:core:network`）同时存在 `ohosMain`、`ohosArm64Main`、`ohosX64Main` 三层 OHOS 源集；`:core:base`、`:core:ui` 只多一个 `ohosX64Main`。也就是说 **OHOS 双架构的差异只集中在 3 个模块**，其余模块 arm64 与 x64 完全同源。

---

## 2. 依赖坐标三端可用性矩阵

### 2.1 列的定义（四列，不是三列）

「三端差异」在依赖层面其实是**四列差异**——因为 `ohosArm64` 与 `ohosX64` 是两条独立解析线。

| 列 | 判定含义 |
|---|---|
| 可用 | 该 target 有对应 klib、AAR、Framework，且代码真的调用它 |
| 占位 | 依赖能解析、但本仓**故意不挂**，或挂上后由一个空实现顶上 |
| 不可用 | 上游根本没发这个变体（404、无 klib），挂上就整条 target 编译失败 |

### 2.2 主矩阵

| 依赖坐标 | 版本行 | Android | iOS | ohosArm64 | ohosX64 | 依据 |
|---|---|---|---|---|---|---|
| `io.ktor:ktor-client-core` | `gradle/libs.versions.toml:33`（`3.3.3-0.3.0`） | 可用 | 可用 | 可用 | **不可用** | 挂线在 `core/network/build.gradle.kts:48-58`，`:74-80` 只到 arm64；`gradle/libs.versions.toml:32` 注明「Plain 3.3.x has no ohos variant」 |
| `io.ktor:ktor-client-okhttp` | `gradle/libs.versions.toml:90` | 可用 | 占位 | 占位 | 不可用 | 只挂在 `core/network/build.gradle.kts:63`；同坐标在 `:88` 的 `androidUnitTest` 也出现一次 |
| `io.ktor:ktor-client-darwin` | `gradle/libs.versions.toml:91` | 占位 | 可用 | 占位 | 不可用 | 只挂在 `core/network/build.gradle.kts:71` |
| `io.ktor:ktor-client-cio` | `gradle/libs.versions.toml:92` | 占位 | 占位 | **可用** | 不可用 | 只挂在 `core/network/build.gradle.kts:78`；这是 OHOS 唯一真链路 |
| `io.ktor:ktor-client-curl` | `gradle/libs.versions.toml:93` | 占位 | 占位 | 占位 | 占位 | **目录里有、任何 build 文件都没引用**（grep 全仓仅命中 catalog 这一行） |
| `io.ktor:ktor-client-websockets` | `gradle/libs.versions.toml:95` | 可用 | 可用 | 可用 | **不可用** | 挂在 `core/network/build.gradle.kts:56`（`networkKtorMain` 内），arm64 因此继承；x64 侧见 §4.3 |
| `io.ktor:ktor-client-logging` | `gradle/libs.versions.toml:89` | 可用 | 可用 | 可用 | 不可用 | `core/network/build.gradle.kts:54` |
| `io.coil-kt.coil3:coil-compose` | `gradle/libs.versions.toml:41`（`3.3.0-0.3.0`） | 可用 | 可用 | 可用 | **不可用** | `core/ui/build.gradle.kts:26` 挂 `imageCoilMain`，`:37-40` 只到 arm64；`gradle/libs.versions.toml:40` 注明「Keep off commonMain/ohosX64」 |
| `io.coil-kt.coil3:coil-network-okhttp` | `gradle/libs.versions.toml:43`（`3.3.0` **stock**） | 可用 | 占位 | 占位 | 不可用 | `core/ui/build.gradle.kts:30`；`gradle/libs.versions.toml:42` 说明 CPF 版是 metadata-only、没有 `androidJvm` |
| `io.coil-kt.coil3:coil-network-ktor3` | `gradle/libs.versions.toml:103`（CPF 线） | 占位 | 可用 | 可用 | 不可用 | `core/ui/build.gradle.kts:35`（ios）、`:39`（ohosArm64） |
| `com.benasher44:uuid` | `gradle/libs.versions.toml:49`（`0.8.4-1.0.0`） | 可用 | 可用 | 可用 | **不可用** | `core/base/build.gradle.kts:18` 挂 `uuidCpfMain`，`:24` 到 arm64；`gradle/libs.versions.toml:48` 注明「Keep off commonMain/ohosX64」 |
| `com.russhwolf:multiplatform-settings` | `gradle/libs.versions.toml:38`（`1.3.0`） | 可用 | 可用 | **不可用** | **不可用** | `core/storage/build.gradle.kts:21` 只挂 `settingsMain`；`gradle/libs.versions.toml:35-37` 写明两条线的互斥缺口 |
| `com.patrykandpatrick.vico:multiplatform` | `gradle/libs.versions.toml:46`（`2.3.0`） | 可用 | 可用 | **不可用** | **不可用** | `feature/home/build.gradle.kts:18` 挂 `chartsVicoMain`；`gradle/libs.versions.toml:45` 注明「CPF 无 ohos 适配」 |
| `org.jetbrains.kotlinx:kotlinx-serialization-json` | `gradle/libs.versions.toml:31`（`1.9.1-0.3.0`） | 可用 | 可用 | 可用 | 可用 | 根共享接线 `build.gradle.kts:112` 以 `api` 给全 6 target；`gradle/libs.versions.toml:30` 注明「OHOS fork（must include ohos_arm64）」 |
| `org.jetbrains.kotlinx:kotlinx-coroutines-core` | `gradle/libs.versions.toml:28`（`1.10.2-0.3.0`） | 可用 | 可用 | 可用 | 可用 | `build.gradle.kts:110`；`core/database/build.gradle.kts:22` 注释「全 target 有 klib」 |
| `org.jetbrains.kotlinx:atomicfu` | `gradle/libs.versions.toml:29`（`0.31.0-0.3.0`） | 可用 | 可用 | 可用 | 可用 | `build.gradle.kts:111` |
| SQLDelight | 不在目录 | 未引入 | 未引入 | 未引入 | 未引入 | `gradle/libs.versions.toml` 全文 140 行无该坐标；原因见 `core/database/build.gradle.kts:10-11` |
| Room | 不在目录 | 未引入 | 未引入 | 未引入 | 未引入 | 同上，`core/database/build.gradle.kts:10` |
| `androidx.camera:*`（4 件） | `gradle/libs.versions.toml:52` | 可用 | 不可用 | 不可用 | 不可用 | 只出现在 `core/platform/build.gradle.kts:31-34` 的 `androidMain` |
| `com.google.mlkit:barcode-scanning` | `gradle/libs.versions.toml:53`、`:117` | 可用 | 不可用 | 不可用 | 不可用 | `core/platform/build.gradle.kts:35` |
| `androidx.compose.ui:*`（stock） | `gradle/libs.versions.toml:21`、`:119-121` | 可用 | 占位 | 占位 | 占位 | 注释行 `:119` 明写「Android target only」；跨端 Compose 走 `compose.*` 访问器（例 `core/ui/build.gradle.kts:18-22`） |

### 2.3 「占位」里最容易被误读的一条

`ktor-client-curl` 在目录里有坐标（`gradle/libs.versions.toml:93`），但**全仓没有任何 `build.gradle.kts` 引用它**——它是 09 册讨论过的「CPF 示例留下的备选引擎位」。把它读成「iOS 用了 Curl 引擎」是错的：iOS 用 Darwin（`core/network/src/iosMain/kotlin/com.example.my_kmp_project/core/network/HttpClientFactory.ios.kt:10`）。

### 2.4 版本线本身的三端差异（不是可用性、是「必须用 fork」）

| 线 | 本仓版本 | stock 为什么不行 | 出处 |
|---|---|---|---|
| Kotlin | `2.2.21-0.3.0` | stock 无 `ohos_arm64` backend | `gradle/libs.versions.toml:27` |
| CMP | `1.9.2-0.3.0` | stock 无 OHOS 产物 | `gradle/libs.versions.toml:23` |
| coroutines、atomicfu、serialization | `1.10.2-0.3.0`、`0.31.0-0.3.0`、`1.9.1-0.3.0` | 同上（serialization 注释最露骨） | `gradle/libs.versions.toml:28-31` |
| Vico | 锁 `2.3.0` | 3.x 会拉 Compose 1.10 或 1.12，与本仓 CMP `1.9.2-0.3.0`、AGP `8.6.0` 冲突 | `gradle/libs.versions.toml:44-46` |

仓库顺序同样是三端差异的一部分：`build.gradle.kts:47-55` 把 eazytec 排第一并 `excludeGroup("com.android.tools.build")`，注释里写明「Gradle 把传输错误判成整次解析失败、不会接着试下一个仓库」。详见 `docs/deep-dive/09-build-and-config.md:44` 起的根 build 走读。

---

## 3. expect/actual 全景（按能力分组比质量）

### 3.1 分级口径

| 标记 | 含义 | 判定依据 |
|---|---|---|
| **真** | 调用了该平台的原生 API，且行为可在设备上被观察到 | 文件里 import 了 `platform.*`、`android.*`、`androidx.*`、Ktor 引擎或三方库 |
| **stub** | 有实现体但语义是「未接入」，通常返回 `null`、抛类型化异常、或返回文案 | 代码里有 `TODO`、`Stub`、`Deferred`、`throw` |
| **placeholder** | 只画一个框、只打印一行日志；接口不报错但也不产出结果 | 空 `Box`、`println`、忽略入参 |

**注意 stub 与 placeholder 的区别不是「有没有代码」，而是「调用方能否察觉失败」**。stub 让调用方拿到 null 或异常从而显示不可用；placeholder 让调用方以为成功。

### 3.2 分组矩阵

27 条 `expect` 按能力归成 13 组（每组的 expect 声明行已在下表列出）。

| # | 能力组 | expect 声明 | Android | iOS | ohosArm64 | ohosX64 |
|---|---|---|---|---|---|---|
| 1 | Platform 标识 | `composeApp/src/commonMain/kotlin/com.example.my_kmp_project/Platform.kt:7` | 真 `Platform.android.kt:9`（`Build.VERSION.SDK_INT`，:6） | 真 `Platform.ios.kt:8`（`UIDevice`，:5） | 真但**硬编码** `Platform.ohos.kt:11`（name 恒为 "HarmonyOS"，:7） | 同 `ohosMain` |
| 2 | 返回栈拦截 | `core/platform/src/commonMain/kotlin/com.example.my_kmp_project/core/platform/PlatformBackHandler.kt:6` | 真 `PlatformBackHandler.android.kt:7`（Compose `BackHandler`，:11） | **placeholder** `PlatformBackHandler.ios.kt:6`（空体，:10 注释交给导航栈） | **placeholder** `PlatformBackHandler.ohos.kt:6`（空体，:10） | 同 |
| 3 | 相机扫码 | `PlatformCameraScan.kt:22`、`:30` | 真 `PlatformCameraScan.android.kt:103`、`:129`（229 行） | 真 `PlatformCameraScan.ios.kt:99`、`:107`（188 行） | **placeholder** `PlatformCameraScan.ohos.kt:29`、`:33`（52 行，`Unavailable` + 红字，:17、:45） | 同 |
| 4 | WebView | `PlatformWebView.kt:25` | 真 `PlatformWebView.android.kt:49`（122 行） | 真 `PlatformWebView.ios.kt:28`（187 行） | **placeholder** `PlatformWebView.ohos.kt:21`（58 行横幅，:40、:51 明写「missing（registry）」） | 同 |
| 5 | 打开外部、剪贴板、Toast、通知设置 | `PlatformOpen.kt:4`、`:6`、`:8`、`:14` | 真 `PlatformOpen.android.kt:15`、`:24`、`:30`、`:44`（60 行） | 真 `PlatformOpen.ios.kt:16`、`:21`、`:25`、`:57`（`UIApplication.openURL` :18、`UIPasteboard` :22、`UIAlertController` :31；:28 另有一行调试 `println`） | **placeholder** `PlatformOpen.ohos.kt:6`、`:11`、`:15`、`:21`（22 行全 `println`，:7、:12、:18；:22 返回 "Deferred"） | 同 |
| 6 | 字符串表、今日日期 | `StringListStore.kt:34` | 真 `StringListStore.android.kt:7`（`SimpleDateFormat`，:8） | 真 `StringListStore.ios.kt:7`（`NSDateFormatter`，:8） | **stub** `StringListStore.ohos.kt:11`（UTC 近似本地日，:12 注释） | 同 |
| 7 | KV 落盘 | `core/storage/src/commonMain/kotlin/com.example.my_kmp_project/core/storage/KeyValueStore.kt:58` | 真 `KeyValueStore.android.kt:12`（SharedPreferences） | 真 `KeyValueStore.ios.kt:14`（`NSUserDefaults(suiteName:)`） | 真 `KeyValueStore.ohos.kt:41`（`OH_Preferences` NDK） | **同一份 `ohosMain`**（`KeyValueStore.ohos.kt:30` 注释） |
| 8 | HTTP 客户端 | `core/network/src/commonMain/kotlin/com.example.my_kmp_project/core/network/ApiClient.kt:69` | 真 OkHttp `HttpClientFactory.android.kt:6` | 真 Darwin `HttpClientFactory.ios.kt:6` | 真 CIO `HttpClientFactory.ohosArm64.kt:7` | **另一条实现** `HttpClientFactory.ohosX64.kt:6`（cinterop `OhosApiClient`） |
| 9 | 后端 loopback 改写 | `BackendLocalhost.kt:7` | 真 `BackendLocalhost.android.kt:3`（模拟器探测，:11-18；真机告警，:23） | 直通 `BackendLocalhost.ios.kt:7` | 直通 `BackendLocalhost.ohos.kt:7`（:4 注释「10.0.2.2 不适用」） | 同 arm64 |
| 10 | 网络自举与客户端元信息 | `NetworkBootstrap.kt:25`、`:27`、`:29`、`:31`、`:33` | `NetworkBootstrap.android.kt:3-9`：`"android"`、false、`"1.0"`、`"1"` | `NetworkBootstrap.ios.kt:7-15`：`"ios"`、`!Platform.isDebugBinary`、Info.plist 版本与构建号 | **stub** `NetworkBootstrap.ohos.kt:3-9`：`"ohos"`、恒 false、`null`、`null` | 同 |
| 11 | 网络图 | `core/ui/src/commonMain/kotlin/com.example.my_kmp_project/core/ui/PlatformNetworkImage.kt:23` | 真 `core/ui/src/imageCoilMain/kotlin/com.example.my_kmp_project/core/ui/PlatformNetworkImage.coil.kt:20`（`AsyncImage`，:39） | 同 `imageCoilMain` | 同 `imageCoilMain` | **placeholder** `PlatformNetworkImage.ohosX64.kt:16`（`url` 被显式忽略，:31-32） |
| 12 | UUID | `core/base/src/commonMain/kotlin/com.example.my_kmp_project/core/uuid/AppUuid.kt:10` | 真 `core/base/src/uuidCpfMain/kotlin/com.example.my_kmp_project/core/uuid/AppUuid.cpf.kt:7`（`uuid4()`，:8；`uuidFrom`，:14） | 同上 | 同上 | **stub** `AppUuid.ohosX64.kt:9`（自写 RFC 4122 v4，:13-19；:7 注释「Not product SoT」） |
| 13 | 图表 | `feature/home/src/commonMain/kotlin/com.example.my_kmp_project/feature/home/AnalyticsCharts.kt:17`、`:24` | 真 `feature/home/src/chartsVicoMain/kotlin/com.example.my_kmp_project/feature/home/AnalyticsCharts.vico.kt:41`、`:107`（156 行） | 同 `chartsVicoMain` | **placeholder** `AnalyticsCharts.ohos.kt:15`、`:24`（空 `Box`，:20、:32） | 同 |
| 14 | WebSocket | `core/network/src/commonMain/kotlin/com.example.my_kmp_project/core/network/realtime/RealtimeWsTransport.kt:65` | 真 OkHttp `RealtimeWsTransportFactory.android.kt:7` | 真 Darwin `RealtimeWsTransportFactory.ios.kt:7` | 真 CIO `RealtimeWsTransportFactory.ohosArm64.kt:10` | **stub（抛异常）** `RealtimeWsTransportFactory.ohosX64.kt:17`（:18 `throw RealtimeWsError.Unsupported()`） |
| 15 | 支付通道 | `component/src/commonMain/kotlin/com.example.my_kmp_project/component/pay/PayGateway.kt:75`、`:77` | **stub** `PlatformPayAdapters.android.kt:7`、`:16`（:8、:17 `TODO(pay)`；:9、:18 `return null`） | **stub** `PlatformPayAdapters.ios.kt:4`、`:7`（`= null`） | **stub** `PlatformPayAdapters.ohos.kt:7`、`:9`（`= null`；:5 注释保留沙箱通道） | 同 |
| 16 | 推送 | `component/src/commonMain/kotlin/com.example.my_kmp_project/component/push/PushBridge.kt:99` | **stub** `PlatformPushSdk.android.kt:9`（`"android"`，:10；token `null`，:12；:4 「Stub until Spike II」） | **stub** `PlatformPushSdk.ios.kt:9`（:10、:12） | **stub** `PlatformPushSdk.ohos.kt:9`（:10、:12；:6 TODO 指向 Push Kit） | 同 |
| 17 | 播放器 | `feature/media/src/commonMain/kotlin/com.example.my_kmp_project/feature/media/MediaPlayer.kt:45` | 真 `MediaPlayer.android.kt:12`（123 行） | 真 `MediaPlayer.ios.kt:22`（97 行，`AVPlayer`，:10、:25） | **stub** `MediaPlayer.ohos.kt:7`（`StubMediaPlayer()`，:4 注释「no AVPlayer/MediaKit bridge」） | 同 |

上表把 27 条 `expect` 归到了 17 行组里（`PlatformOpen` 4 条、`NetworkBootstrap` 5 条、`PlatformCameraScan`、`AnalyticsCharts`、`PayGateway`、`PlatformPushSdk` 各 2 条成组）。逐函数的实现走读见 `docs/deep-dive/23-native-shells.md:445` 起的 §4；本表只提供「质量分级」这一层新信息。

### 3.3 三端实现质量的量化结论

按 §3.1 的口径把上表 17 个组逐格归档（合计必须等于 17）：

| 分级 | Android | iOS | ohosArm64 | ohosX64 |
|---|---|---|---|---|
| 真 | **15** | **14** | **7** | **4** |
| stub | 2（#15、#16） | 2（#15、#16） | 5（#6、#10、#15、#16、#17） | 7（#6、#10、#12、#14、#15、#16、#17） |
| placeholder | 0 | 1（#2） | 5（#2、#3、#4、#5、#13） | 6（#2、#3、#4、#5、#11、#13） |
| 合计 | 17 | 17 | 17 | 17 |

**Android 与 iOS 的差距是 1 组，arm64 与 Android 的差距是 8 组，x64 与 arm64 的差距是 3 组**（#11 网络图、#12 UUID、#14 WebSocket；#11 在 x64 从真降为 placeholder，#12、#14 从真降为 stub）。这条阶梯就是「三端差异」的全部量级。

两点必须说清楚，否则表会骗人：

1. **#2 与 #13 是两种性质完全不同的「没实现」**。`PlatformBackHandler` 在 iOS、OHOS 上都是空体，但这是**设计决定**——iOS 无物理返回键、OHOS 返回由壳处理（`PlatformBackHandler.ios.kt:10`、`PlatformBackHandler.ohos.kt:10`）。`AnalyticsCharts` 的空 `Box` 是**产物缺口**（`AnalyticsCharts.kt:13` 注释「stub actual until CPF ships Vico/KoalaPlot」）。
2. **OHOS 的 placeholder 一律带显式失败文案**，不是静默成功。`PlatformWebView.ohos.kt:51` 写「能力状态：missing（registry）。请勿将本页视为验收通过。」，`PlatformCameraScan.ohos.kt:45` 写「（registry: missing）」。这条纪律是仓库自定的诚实条款，见 `docs/deep-dive/23-native-shells.md:600` 起的 §8。
3. **#8 的 x64 一格不能算降级成 placeholder**：`OhosApiClient` 走 `net_http` cinterop 是**能真发请求的 HTTP 客户端**（`core/network/src/ohosX64Main/kotlin/com.example.my_kmp_project/core/network/OhosApiClient.kt:12-19`），缺的是流式与 WS（同文件 `:64-71`）。它「真」，只是真得比另外三列少两项能力。

---

## 4. ohosArm64 与 ohosX64：同一个平台的两个世界

这是全仓最容易被误读的一组差异——**它不是「真机与模拟器」的性能差异，而是四套独立的能力实现**。

### 4.1 四个对照点

| 能力 | ohosArm64 | ohosX64 | 差异根源 |
|---|---|---|---|
| HTTP 引擎 | CPF Ktor `3.3.3-0.3.0` + **CIO**（`HttpClientFactory.ohosArm64.kt:11`）；`core/network/build.gradle.kts:75-80` 把 `networkKtorMain` 与 `ktor-client-cio` 挂上 | **`net_http` cinterop**（`HttpClientFactory.ohosX64.kt:9` 返回 `OhosApiClient`）；`core/network/build.gradle.kts:29-39` 只在 `ohosX64` 这个 `KotlinNativeTarget` 上创建 `netHttp` cinterop | `core/network/build.gradle.kts:27-28`、`:74` 两处注释同口径：「no CPF Ktor ohosX64 klib」 |
| SSE 流式 | **可用**（`KtorApiClient.openSseTextStream`，见 `OhosApiClient.kt:62-63` 的交叉说明） | **抛类型化异常** `SseStreamingUnsupportedException(transport = "ohosX64/net_http")`（`OhosApiClient.kt:64-71`） | `libnet_http` 只在完成回调给整包 body（`OhosApiClient.kt:59-60`） |
| WebSocket | **可用**（`RealtimeWsTransportFactory.ohosArm64.kt:11`，`ktor-client-websockets` 随 `networkKtorMain` 继承，`core/network/build.gradle.kts:55-56`） | **抛异常** `RealtimeWsError.Unsupported()`（`RealtimeWsTransportFactory.ohosX64.kt:18`）；注释「不做伪实现」（`:10-11`） | `RealtimeWsTransportFactory.ohosX64.kt:6-9` 列明与 Coil、uuid 同一缺口原因 |
| 网络图 | CPF Coil（`imageCoilMain`，`core/ui/build.gradle.kts:37-40`） | 灰底占位，`url` 被显式忽略（`PlatformNetworkImage.ohosX64.kt:23-32`） | `gradle/libs.versions.toml:40` |
| UUID | CPF `uuid4()`（`AppUuid.cpf.kt:8`） | 自写 RFC 4122 v4（`AppUuid.ohosX64.kt:13-19`），注释「Not product SoT」（`:6-7`） | `gradle/libs.versions.toml:48` |
| 链接选项 | 无 `-lnet_http`（`composeApp/build.gradle.kts:67-71` 用 `if (ohosTarget.name == "ohosX64")` 判定） | 有 `-lnet_http`（`composeApp/build.gradle.kts:70`） | cinterop 静态库路径写死为 `src/ohosMain/cinterop/libs/ohosX64`（`core/network/src/ohosMain/cinterop/net_http.def:5`） |

### 4.2 `net_http.def` 全文与它的架构绑定

`core/network/src/ohosMain/cinterop/net_http.def` 一共 5 行，逐行都是差异证据：

| 行 | 内容 | 意义 |
|---|---|---|
| `:1` | `package = platform.netstack` | Kotlin 侧 import 前缀 |
| `:2` | `headers = demo_net_http_bind.h demo_net_http_ext.h` | 两个头都来自本仓自带 include 目录（`core/network/build.gradle.kts:33`） |
| `:3` | `headerFilter = …` | 只导出这两个头，防止 sysroot 里其它 netstack 符号泄漏 |
| `:4` | `staticLibraries = libdemo_net_http.a` | 预编译静态库，**不是** OHOS 系统库 |
| `:5` | `libraryPaths = src/ohosMain/cinterop/libs/ohosX64` | **目录名直接把这条实现锁死在 x64**；arm64 若挂上会链不到 |

def 文件放在 `src/ohosMain/cinterop/`（共享目录），但 `libraryPaths` 指向 `libs/ohosX64`——这是本仓唯一一个「源集目录叫 `ohosMain`、实际只服务 x64」的位置陷阱。

### 4.3 为什么 `ohosX64` 拿不到 Ktor klib

三层原因，全部有文件出处：

1. **Ktor 的 OHOS 变体不是官方产物**，是 CPF（CPF-KMP-CMP）维护的 `-0.3.0` 线；版本号本身就把线选死了：`gradle/libs.versions.toml:32-33`。
2. **CPF 只发了 `ohosArm64` 一个 OHOS 架构**。`ktor`、`coil`、`uuid` 三个坐标的注释句式完全一致：`gradle/libs.versions.toml:32`、`:39-40`、`:47-48` 都写「Keep off commonMain/ohosX64（no ohosX64 klib）」。
3. **`commonMain` 因此不能引用任何一条能力线**。一旦 Ktor 进 `commonMain`，`ohosX64` 编译立刻断。这就是五个中间源集存在的唯一理由，`core/base/build.gradle.kts:4-5` 把这句话写成了文件头注释：「uuidCpfMain 是 OHOS 产物缺口造成的中间 source set」。

推论（可直接当检查项用）：**任何新增的三方依赖，先问「有没有 ohosX64 klib」；没有就必须开一个中间源集，同时在 `ohosX64Main` 补一个 actual**。全仓现有的 5 个中间源集正是这条规则的 5 次执行结果。

### 4.4 x64 那条腿目前连不上

`composeApp/build.gradle.kts:281-283` 给 `publish*BinariesToHarmonyAppX64` 写的 description 是：
「Currently blocked by toolchain: Konan_cxa_demangle on linkDebugSharedOhosX64.」

也就是说 §4.1 那一整列 x64 降级，**目前连产物都还没进过设备**。跑 OHOS 的默认腿是 arm64：`runscript/runOhosApp-Mac.sh:25` 的 `DEFAULT_PLATFORM="ohosArm64"`，`scripts/run-harmony.sh:7` 的示例同架构。这条事实决定了 §7 里 x64 的妥协可以「等」，不必急着补。

---

## 5. 差异对行为的实际影响

矩阵读到此为止都是构建期事实；这一节回答「用户能感知到什么」。

### 5.1 HTTP 失败类型：同一异常、两种信息量

| 端 | 失败构造点 | `NetworkError.Transport` 的第二参（HTTP 状态） | 出处 |
|---|---|---|---|
| Android、iOS、ohosArm64 | `catch (t: Throwable)` 统一包装 | **有**：`transportStatus()` 从 `ResponseException` 取 `response.status.value` | `core/network/src/networkKtorMain/kotlin/com.example.my_kmp_project/core/network/KtorApiClient.kt:31-32`；调用点 `:48-49`、`:56-57`、`:66-67` |
| ohosX64 | 两条路 | 走 `getRaw`、`postRaw` 时**有**（`statusCode >= 400` 手工抛，`OhosApiClient.kt:15-17`）；cinterop 传输层失败时**没有**（`fail()` 单参构造，`OhosHttpTransport.kt:232`） | 见左列 |

行为后果：上层用 `NetworkError.Transport.status` 区分「401 掉登录态」与「网络挂了」的逻辑（详见 `docs/deep-dive/13-core-network-engine.md`），在 x64 上遇到 DNS、连接超时这类底层失败时只能拿到 `null` 状态码——**不会误判成 401，但会少一个可诊断维度**。arm64 与另两端在这一格完全同构，因为跑的是同一个 `KtorApiClient`。

### 5.2 KV 落盘：两种宿主、一套契约

| 端 | 后端 | 落盘位置 | 关键约束 |
|---|---|---|---|
| Android | `SharedPreferencesSettings`（`KeyValueStore.android.kt:15`） | 应用私有 SharedPreferences 文件，`MODE_PRIVATE`（`:14`） | 文件名即 store name，历史数据仍可读（`:9-10` 注释） |
| iOS | `NSUserDefaultsSettings(NSUserDefaults(suiteName = name))`（`KeyValueStore.ios.kt:15`） | 按 name 分 suite | 迁移过一次：旧实现共享 `standardUserDefaults`，开发机上旧值读不到（`:10-12` 注释） |
| ohosArm64、ohosX64 | `OH_Preferences` NDK（`KeyValueStore.ohos.kt:41-42`） | **应用沙箱 data dir**（`:33-34`） | 句柄按 name 缓存且**永不关闭**（`:47-52`）；读字符串要手工 `OH_Preferences_FreeString`（`:90-92`） |

三条真正的差异：

1. **`multiplatform-settings` 在 OHOS 两个架构都不可用**，所以 `settingsMain` 根本没往 OHOS 挂（`core/storage/build.gradle.kts:18-28`）。OHOS 是**直接实现接口**而不是「换一种 Settings 后端」——见 `core/storage/build.gradle.kts:6` 的说明。
2. **列表编码是跨端一致性的唯一硬保证**：三端都走 `StringListValues` 编解码（`SettingsKeyValueStore.kt:29`、`:32`；`KeyValueStore.ohos.kt:111`、`:114`），契约里明写「so all three ends persist exactly one item order and one empty-list representation」（`SettingsKeyValueStore.kt:9-10`）。OHOS 没有用原生 string-set，就是为了不让「空列表」在三端读出不同结果。
3. **接口刻意砍窄**：没有 `clear()`、没有 `contains()`，`KeyValueStore.kt:10-13` 给的正是「OHOS 等价物只从 API 23 起存在」这一平台差异。
4. 链接层看不到 `-lohpreferences`：`KeyValueStore.ohos.kt:31` 注释「the linker option comes from the platform klib itself」，所以 `composeApp/build.gradle.kts:78-90` 那串 `linkerOpts` 里没有它——检查 OHOS 链接是否完整时别去那里找。

日志侧没有差异：`-lhilog_ndk.z` 只是 fusion-renderer 的链接项（`composeApp/build.gradle.kts:85`），**Kotlin 侧 0 处 HiLog 绑定**（命令 D 实测）。三端的调试输出都是 `println`——OHOS 壳入口也不例外（`composeApp/src/ohosMain/kotlin/com.example.my_kmp_project/MainArkUIViewController.kt:36`）。iOS 甚至在同一函数里既 `println` 又弹真 Alert（`PlatformOpen.ios.kt:28` 与 `:31`）。**结论：日志是四列同构的最差一列，全平台都在用 stdout。**

### 5.3 深链 scheme：三条注册、一条解析

| 平台 | 注册处 | scheme | 额外能力 | 投递到共享层的入口 |
|---|---|---|---|---|
| Android | `composeApp/src/androidMain/AndroidManifest.xml:27-32` | `myai`（`:31`） | 还注册了 `https` App Link，host `app.myai.example`（`:34-42`） | `composeApp/src/androidMain/kotlin/com.example.my_kmp_project/MainActivity.kt:61` `acceptDeepLink(data.toString())` |
| iOS | `iosApp/iosApp/Info.plist:7-17` | `myai`（`:14`） | 无 universal link 配置 | `iosApp/iosApp/iOSApp.swift:19`（冷启）与 `:31`（热启 `onOpenURL`） |
| OHOS | `harmonyApp/entry/src/main/module.json5:49-68` | `myai`（`:64`，配 `ohos.want.action.viewData`，`:60`） | 只有自定义 scheme，无 https 等价物 | ArkTS 先暂存 `AppStorage`（`harmonyApp/entry/src/main/ets/entryability/EntryAbility.ets:29`，键名见 `:7`），再由 Compose 侧取用 |
| 共享解析 | `core/router/src/commonMain/kotlin/com.example.my_kmp_project/core/router/DeepLinkEntry.kt:9-10` | — | scheme 与 host 都可省，只比 path（`DeepLinkRouter.kt:10`） | 三端复用同一份 |

行为差异：**解析层完全一致，注册层 Android 比另两端多一条 https App Link**。OHOS 多一跳 `AppStorage` 暂存（`EntryAbility.ets:17`、`:21` 分别在 `onCreate` 与 `onNewWant` 里 stash），因为 Want 参数无法在 `onNewWant` 直接穿透到已存活的 Compose 岛。

### 5.4 返回栈：三端三个答案

| 平台 | 拦截方式 | 出处 |
|---|---|---|
| Android | Compose `BackHandler` 生效（`PlatformBackHandler.android.kt:11`），同时物理返回键由系统派发 | `core/platform/src/androidMain/kotlin/com.example.my_kmp_project/core/platform/PlatformBackHandler.android.kt:3` |
| iOS | `PlatformBackHandler` 是空体（`:10`）；返回靠 `UINavigationController` 侧滑，与共享层无关 | `core/platform/src/iosMain/kotlin/com.example.my_kmp_project/core/platform/PlatformBackHandler.ios.kt:10` |
| OHOS | 同样空体（`:10`）；返回由 ArkTS 壳的 navigation stack 处理，见 `docs/deep-dive/23-native-shells.md` | `core/platform/src/ohosMain/kotlin/com.example.my_kmp_project/core/platform/PlatformBackHandler.ohos.kt:10` |

这是**唯一一个「Android 有、iOS 与 OHOS 结构性没有」**的差异——不是缺口，是平台输入模型不同。`PlatformBackHandler.kt:6-9` 的 expect 保留空参实现，正是为了让共享层可以无脑调用。

### 5.5 其它可感知的行为差

| 差异 | 后果 | 出处 |
|---|---|---|
| OHOS 无版本号与构建号 | 上报给后端的 client 元信息缺 `version`、`build`；`platformIsReleaseBuild()` 恒 false（OHOS 与 Android 都恒 false，只有 iOS 用 `!Platform.isDebugBinary`） | `NetworkBootstrap.ohos.kt:5`、`:7`、`:9`；`NetworkBootstrap.ios.kt:10`；`NetworkBootstrap.android.kt:5`、`:7`、`:9` |
| Android 模拟器 loopback 自动改写到 `10.0.2.2`，iOS 与 OHOS 直通 | 同一份 `DemoApiHosts` 在 Android emu 上能连本机后端，在 iOS 模拟器上是 loopback 直达，在 OHOS 模拟器上必须显式注入 host | `BackendLocalhost.android.kt:4-6`、`:11-18`；`BackendLocalhost.ohos.kt:4-5`；`BackendLocalhost.ios.kt:4-5` |
| iOS ATS 强制 https | 明文图 URL 在共享层被统一升级 scheme，三端共用 | `core/ui/src/commonMain/kotlin/com.example.my_kmp_project/core/ui/PlatformNetworkImage.kt:9-15`；`iosApp/iosApp/Info.plist:18-21`（`NSAllowsArbitraryLoads` = false） |
| OHOS 岛内无 WebView | Compose 路由到 WebView 页只显示失败横幅，而 ArkTS 壳自己有 Web 页——**同一个功能两种可见性** | `PlatformWebView.ohos.kt:16-19` 注释 |

---

## 6. 产物形态与链接参数差异

### 6.1 三端产物对照

| 端 | 产物 | 声明处 | 落地方式 |
|---|---|---|---|
| Android | `:composeApp` = APK（`alias(libs.plugins.androidApplication)`，`composeApp/build.gradle.kts:10`）；`:core:*`、`:feature:*`、`:component` = AAR（`com.android.library`，例 `core/base/build.gradle.kts:8`） | 各模块 `android { namespace }` | Gradle assemble；CI 见 `docs/deep-dive/24-gates-ci-adr.md` |
| iOS | 3 个架构各一份**静态 framework**，`baseName = "ComposeApp"` | `composeApp/build.gradle.kts:44-47`（`isStatic = true` 在 `:46`） | `iosApp` 的 Xcode 工程链接；`MainViewController.kt` 是可见符号面（详见 `docs/deep-dive/23-native-shells.md:473`） |
| OHOS | 2 个架构各一份**动态库** `libkn.so` + 头文件 `libkn_api.h` | `composeApp/build.gradle.kts:55-61`（`baseName = "kn"` 在 `:56`） | `publish*BinariesToHarmonyApp`（`:261-277`）与 `…X64`（`:279-298`）拷进 `harmonyApp` |

**唯一二进制产出方是 `:composeApp`**：`build.gradle.kts:94-96` 与 `component/build.gradle.kts:28` 两处注释重复声明「库模块一律不产 framework / sharedLib」。

### 6.2 OHOS 与 iOS 的两个本质区别（构建期）

1. **静态对动态**：iOS framework `isStatic = true`（`composeApp/build.gradle.kts:46`）；OHOS 是 `sharedLib`，`harmonyApp` 靠 `dlopen`。所以 `composeApp/build.gradle.kts:62-65` 有一段专门解释为什么不能用 `moduleIncludes`：那会让 `libkn.so` 的 `DT_NEEDED` 指向 `libruntime.so` 与 `libstd.so` 却不随包投递，导致 HAP `dlopen` 失败。
2. **资源投递是 OHOS 独有的构建步骤**：iOS 资源进 asset catalog，Android 进 AAR；OHOS 必须把 composeResources 拷进 rawfile。清单只有一项——`:core:design`（`composeApp/build.gradle.kts:250-253`），目标目录 `entry/src/main/resources/rawfile/composeResources/<pkg>/`（`:274`）。`composeApp` 自己已无 composeResources（`:246-247` 注释）。
3. **输出根可被外部覆盖**：`composeApp/build.gradle.kts:229-238` 的 `harmonyAppDir` 读 `-PharmonyAppPath`，默认仓库内 `harmonyApp`。

### 6.3 链接参数差异表

| 参数 | 值与条件 | 出处 |
|---|---|---|
| `rendererBackend` | 默认 **`fusion-renderer`**（`gradle.properties:22`，`skia` 备选被注释在 `:23`）；命中时追加 11 条 `linkerOpts`：`-lnative_drawing`、`-limage_source`、`-lpixelmap`、`-lpixelmap_ndk.z`、`-lnative_window`、`-lace_napi.z`、`-lhilog_ndk.z`、`-lhitrace_ndk.z`、`-luv`、`-lunwind`、`-licu` | `composeApp/build.gradle.kts:76-91` |
| `-lz`、`-lavplayer` | 两个 OHOS 架构都加 | `composeApp/build.gradle.kts:66` |
| `-lnet_http` | **只有 `ohosX64`** 加（`if (ohosTarget.name == "ohosX64")`） | `composeApp/build.gradle.kts:69-71` |
| `export(libs.compose.multiplatform.export)` | 仅 OHOS `sharedLib`；iOS framework 无对应导出 | `composeApp/build.gradle.kts:61`；坐标 `gradle/libs.versions.toml:78` |
| Release `optimized = false` | **仅 OHOS、仅 RELEASE**；原因是 DevirtualizationAnalysis 内存占用过高易 OOM | `composeApp/build.gradle.kts:57-60` |
| `kotlin.native.cacheKind` | `none`——全平台 native 缓存关闭，影响 iOS 与 OHOS 的编链速度但不改产物语义 | `gradle.properties:16` |
| skiko 版本 | `ohosSkikoVersion=0.9.22.2-0.3.0` 与 `rendererBackend` 成对；换渲染器时要一起动 | `gradle.properties:24`；仓库根 `AGENTS.md:212` 起的「Done when」第 5 条要求改完 Kotlin 后重发 binaries |

**两个架构的 cinterop 也不同**：`:composeApp` 对 arm64 与 x64 **都**创建 `resource` 与 `avplayer` 两个 cinterop（`composeApp/build.gradle.kts:93-107`），而 `netHttp` 只在 x64（`:107` 注释「netHttp cinterop moved to :core:network」）。`avplayer.def` 只有一行 headers，指向 sysroot 的 `multimedia/player_framework/`（`composeApp/src/ohosMain/cinterop/avplayer.def:2`，sysroot 解析在 `composeApp/build.gradle.kts:100-105`）；`resource.def:2` 声明 `raw_file_manager.h` 与 `raw_file.h`。

这两个 def 是**给未来 OHOS 播放器与 rawfile 读取预留的原生面**——实测 `grep -rn "platform.avplayer\|platform.resource" --include="*.kt" . | grep -v "/build/"` **返回 0 行**，即 cinterop 已生成但没有任何 Kotlin 消费方；对应地 `MediaPlayer.ohos.kt:7` 仍是 `StubMediaPlayer()`。这是全仓唯一一处「原生绑定已就位、共享层还没接」的差异。

顺带两条同源的「目录里有、没人用」：`ktor-client-curl`（`gradle/libs.versions.toml:93`）在全仓 `*.kts` 里 0 引用；`sqldelight` 只以注释形式出现在 `core/database/build.gradle.kts:10`。读目录时必须把「声明」与「挂载」分开看。

---

## 7. 差异收口清单

### 7.1 等上游 klib 的临时妥协（本仓无法自行关闭）

| 妥协 | 关闭条件 | 现状证据 |
|---|---|---|
| `uuidCpfMain` 对 `ohosX64` 开 stub | CPF 发 `uuid` 的 ohosX64 klib | `core/base/build.gradle.kts:4-5`；`AppUuid.ohosX64.kt:6-7` |
| `imageCoilMain` 不含 `ohosX64` | CPF 发 Coil 的 ohosX64 klib | `gradle/libs.versions.toml:40`；`PlatformNetworkImage.ohosX64.kt:14` |
| `networkKtorMain` 不含 `ohosX64` | CPF 发 Ktor 的 ohosX64 klib | `core/network/build.gradle.kts:27-28`、`:74`；`gradle/libs.versions.toml:32` |
| `ohosX64` 无 WebSocket | 同上（`ktor-client-websockets` 也缺 x64 变体） | `RealtimeWsTransportFactory.ohosX64.kt:6-9` |
| `ohosX64` 无 SSE | `libnet_http` 提供增量分片通道，或 x64 拿到 Ktor | `OhosApiClient.kt:59-60` |
| `chartsVicoMain` 不含两个 OHOS 架构 | CPF 发 Vico 或 KoalaPlot 的 ohos 产物 | `feature/home/build.gradle.kts:12-13`；`AnalyticsCharts.kt:13`；`AnalyticsCharts.ohos.kt:11` |
| `settingsMain` 不含 OHOS | 任何一条 `multiplatform-settings` 线同时覆盖 Android 与 OHOS | `core/storage/build.gradle.kts:3-8`；`gradle/libs.versions.toml:35-37` |
| 无 SQLDelight 与 Room | OHOS fork 出平台 SQL driver | `core/database/build.gradle.kts:10-13` |
| `publish*BinariesToHarmonyAppX64` 跑不通 | 工具链修掉 `Konan_cxa_demangle` | `composeApp/build.gradle.kts:281-283` |

`openspec/changes/` 下已有四条变更目录专门跟踪这些：`adopt-cpf-ktor-ohos`、`adopt-cpf-coil-image`、`adopt-cpf-uuid`、`unify-core-kv-storage`。AGENTS.md 的 OHOS 段落逐条链回它们。

### 7.2 设计决定（不该被当成缺陷，也不该被「补齐」）

| 决定 | 理由 | 出处 |
|---|---|---|
| `KeyValueStore` 不设 `clear()`、`contains()` | 让三端后端行为可预测、可在 `commonMain` 被测 | `KeyValueStore.kt:10-13` |
| OHOS 两个架构共用一份 `ohosMain` KV 实现 | 避免「模拟器与真机持久化语义不同」——那会让 x64 无法预测 arm64 | `KeyValueStore.ohos.kt:35-36` |
| `StringListValues` 统一列表编码 | 保证空列表与元素顺序三端一致 | `SettingsKeyValueStore.kt:9-11` |
| OHOS 句柄「开一次、永不关闭」 | 同一 name 的多个 view 必须指向同一份数据 | `KeyValueStore.ohos.kt:47-52` |
| `PlatformBackHandler` 在 iOS、OHOS 保留空体 | 这两端没有可拦截的返回输入 | `PlatformBackHandler.ios.kt:10`、`PlatformBackHandler.ohos.kt:10` |
| 支付与推送三端统一返回 `null`、UI 显示不可用 | 绝不伪造 Success；三端同构是刻意的 | `PlatformPayAdapters.android.kt:8`、`PlatformPayAdapters.ios.kt:4`、`PlatformPayAdapters.ohos.kt:5`；`PlatformPushSdk.ohos.kt:6` |
| `remapBackendLocalhost` 在 iOS、OHOS 直通 | 只有 Android emu 有 `10.0.2.2` 这个特殊网关 | `BackendLocalhost.ohos.kt:4-5`、`BackendLocalhost.ios.kt:4-5` |
| OHOS 占位页显式写「missing（registry）」 | 防止把占位当验收 | `PlatformWebView.ohos.kt:17-18`、`:51`；`PlatformCameraScan.ohos.kt:45` |
| `release optimized = false` | 实测 DevirtualizationAnalysis OOM；有度量再开 | `composeApp/build.gradle.kts:57-60` |

### 7.3 一句话收口

**三端差异只有约三分之一是「待补」，其余是缺口 + 设计的混合体**：
构建期的差异（§1、§2、§4）全部源于 **CPF 只发 ohosArm64**；
行为期的差异（§5）里，只有 OHOS 的 4 个 placeholder 组（#2、#3、#4、#5、#13）与 x64 的额外 3 组是真缺口，其余（返回栈、KV 后端、scheme 注册、静态对动态）是平台模型不同带来的设计选择。


---

## 8. 本文的判定边界与未验证疑点

诚实条款：矩阵式文档最容易在「分级」上造假。以下四条是本册里**证据最弱**的判断，复核时优先抽查。

| # | 判断 | 薄弱在哪 | 复核入口 |
|---|---|---|---|
| 1 | §3.3 的四列分级计数（15、14、7、4） | 分档依赖 §3.1 的人为口径。若把「`Platform.ohos.kt:7` 硬编码 name」判成 stub，arm64 与 x64 各掉 1 分；若把 `BackendLocalhost` 的「直通」判成设计决定而非真实现，iOS 与 OHOS 又各掉 1 分。**计数会随口径移动，但阶梯关系（Android ≈ iOS ≫ arm64 > x64）不会** | 逐格看 §3.2 的 `file:line`，不要只读汇总 |
| 2 | 「OHOS 日志=全平台最差」 | 依据是 `grep hilog` 命中 0（命令 D）+ 三处 `println`。但 `-lhilog_ndk.z` 已在链接面（`composeApp/build.gradle.kts:85`），**运行时是否由 Kotlin/Native 自身把 stdout 转进 HiLog 未验证**——本册只看源码，不看设备 logcat | 真机跑一次 `scripts/run-harmony.sh` 再判 |
| 3 | 「`ohosX64` 目前连不上设备」 | 唯一依据是 `composeApp/build.gradle.kts:281-283` 的 description 文本，属**注释级证据**。没有实跑 `linkDebugSharedOhosX64` 复现 `Konan_cxa_demangle` | 实跑该 task 才算闭环 |
| 4 | §5.1 的 x64 状态码缺失后果 | `OhosHttpTransport.kt:232` 单参构造 `NetworkError.Transport` 是读出来的；`NetworkError.Transport` 第二参的默认值与上层是否真读它，本册未展开（属 `docs/deep-dive/13-core-network-engine.md` 的范围） | 追 `NetworkError` 定义与 `classifyAuthHttp` 消费点 |

另外两条**刻意没有写进矩阵**的边界，避免读者以为本册覆盖全量：

- 本文不讲 **iOS 与 OHOS 的壳工程如何装载产物**（Xcode 静态库链接、HAP `dlopen`、`MainArkUIViewController` 符号稳定性），全部见 `docs/deep-dive/23-native-shells.md`。
- 本文不讲 **Gradle 求值顺序、`~/.gradle/init.gradle` 的 plugin-marker 限制、CI 矩阵**，见 `docs/deep-dive/09-build-and-config.md` 与 `docs/deep-dive/24-gates-ci-adr.md`。

### 8.1 与其它四册的分工

| 册 | 轴 | 与本册的重叠 |
|---|---|---|
| `docs/deep-dive/09-build-and-config.md` | 纵：构建配置逐文件 | 本册 §1.1、§6.3 只取它的结论表，不重复逐行走读 |
| `docs/deep-dive/22-composeapp-assembly-routing.md` | 纵：装配与路由 | 本册 §5.3 只讲深链的**注册差异**，解析逻辑归 22 |
| `docs/deep-dive/23-native-shells.md` | 纵：三端原生壳 | 本册 §3 与它的 `23-native-shells.md:445` 表**同批 expect**，但那张表按文件列、本册按能力与质量分级列 |
| `docs/deep-dive/24-gates-ci-adr.md` | 纵：门禁与 ADR | 本册 §7 的「设计决定」判定引用它的诚实条款 |
| 本册 | **横：三端到底差在哪** | 唯一以「四列矩阵」组织的册 |

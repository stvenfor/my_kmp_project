# 27 · 模块 Gradle 接线对照 —— 一个模块必须自己写什么，公共供给帮它省了什么

## 0. 本文回答的问题、统计口径与边界

**本文只回答一条轴**：`settings.gradle.kts` 把 30 个工程拉进来之后，**每个模块自己的 `build.gradle.kts` 到底还需要写哪几行**，以及根工程 `build.gradle.kts` 的 `subprojects {}` 替它挡掉了哪几行。四类模块（`:core:*`、`:feature:*`、`:component`、`:composeApp`）在这条轴上的差别非常大——最短的 `core/analytics/build.gradle.kts` 只有 10 行，最长的 `composeApp/build.gradle.kts` 有 300 行，本文解释这个 30 倍差距从哪来。

**明确不重讲**：`docs/deep-dive/09-build-and-config.md` 已经逐段讲过根 `build.gradle.kts` 的整体结构、`deployment.gradle.kts` 的任务全集、`gradle.properties`、wrapper、`runscript/` 与 publish 任务的产物投递细节。本文引用它们只为对照「模块侧因此少写什么」，不复述其内容。同理，`docs/deep-dive/24-gates-ci-adr.md` 讲门禁的**执行链与事故史**，本文只讲门禁**如何约束 build 文件里的依赖边**。

**统计口径**（全部为本次实际数过、逐文件 `grep -n` 定位的结果）：

| 口径 | 数值 | 出处 |
|------|------|------|
| `include` 语句总数 | 30 条（12 条显式 + 17 条 forEach + 1 条 ohosAggregate） | `settings.gradle.kts:23-34`、`settings.gradle.kts:38-43`、`settings.gradle.kts:44` |
| Gradle project 总数 | 31（root + 30 子工程） | 同上，root 名在 `settings.gradle.kts:1` |
| 被根 `subprojects {}` 接线的模块 | 27（10 个 `:core:*` + 17 个 `:feature:*`） | `build.gradle.kts:35`（过滤谓词）、`build.gradle.kts:38`（不命中即 return） |
| 拿不到任何根接线的子工程 | 3（`:component`、`:composeApp`、`:ohosAggregate`） | `build.gradle.kts:38` |
| 本文整篇读完的 build 文件 | 20 个（19 个模块文件 + 根 `build.gradle.kts`；`deployment.gradle.kts` 只重点读了头部与关键段） | §3 与 §5 的走读清单 |
| 声明中间 source set 的模块 | 5 个（`uuidCpfMain`、`networkKtorMain`、`settingsMain`、`imageCoilMain`、`chartsVicoMain`） | `core/base/build.gradle.kts:15`、`core/network/build.gradle.kts:48`、`core/storage/build.gradle.kts:18`、`core/ui/build.gradle.kts:24`、`feature/home/build.gradle.kts:14` |
| 17 个 feature 里挂 compose 插件的 | 15 个；只有 `feature/pay`、`feature/wallet` 不挂 | `feature/pay/build.gradle.kts:3-4`（原因注释）、`feature/wallet/build.gradle.kts:3-4`（同一注释） |
| `composeApp` 里 feature 装配边 | 17 条，连续 17 行 | `composeApp/build.gradle.kts:133-149` |

模块文件的行数本身也是证据。30 个子工程各有一个 `build.gradle.kts`（10 个 `:core:*`、17 个 `:feature:*`、`:component`、`:composeApp`、`:ohosAggregate`），我用 `wc -l` 实测了全部 30 个，本文引用的样本行数为：`core/analytics` 10 行、`core/router` 20 行、`feature/live` 21 行、`core/database` 31 行、`core/base` 31 行、`core/storage` 34 行、`core/design` 34 行、`core/platform` 42 行、`core/ui` 46 行、`core/network` 96 行、`component` 75 行、`feature/media` 26 行、`feature/pay` 27 行、`feature/chat` 28 行、`ohosAggregate` 27 行、`feature/home` 38 行；另有根 `build.gradle.kts` 130 行、`settings.gradle.kts` 44 行、`composeApp/build.gradle.kts` 300 行。

**撰写约束**：本文只读文件、只跑静态 `grep`，**未运行 Gradle、未 build、未 commit**。凡涉及 Gradle 语义推断（非文件明文）的条目，在 §8 单列。

---

## 1. 「这些你不用写」清单：根 `subprojects {}` 逐段拆解

根脚本一共 130 行（`build.gradle.kts:1-130`），其中真正给模块接线的是 `build.gradle.kts:37-130` 这一个 `subprojects {}` 块。块前的 `build.gradle.kts:4-9` 是插件 `apply false` 去重加载，`build.gradle.kts:20` 是 `apply(from = "deployment.gradle.kts")`——两者都只作用于 root，**模块文件不会从中得到任何依赖或任务**（这正是 `:feature:*` 里没有一行 `tasks.register` 的原因，唯一的例外是 `composeApp/build.gradle.kts:261-298` 的 publish 任务）。

### 1.1 命中范围：一个谓词决定 27 与 3 的分野

```kotlin
// build.gradle.kts:35
val libraryModulePaths: (Project) -> Boolean = { it.path.startsWith(":core:") || it.path.startsWith(":feature:") }
// build.gradle.kts:37-38
subprojects {
    if (!libraryModulePaths(this)) return@subprojects
```

`it.path` 是 Gradle 工程路径（`:core:base`、`:feature:home`），所以这个判断只看**路径前缀**、不看插件、不看目录。`component/build.gradle.kts:6-8` 的注释说的就是这件事：`:component` 的路径不在命中范围内，因此 target 集、android 基础配置、公共依赖都得自己写。注意被挡掉的还有仓库块——`build.gradle.kts:47-55` 的 `repositories {}` 也在这个 early-return 之后，所以 `:component`、`:composeApp`、`:ohosAggregate` 走的是 `settings.gradle.kts:14-21` 那条链（详见 §5.3）。

### 1.2 你不用写的东西（对照清单）

| 你不用写 | 根工程出处 | 覆盖到的模块 | 模块里再写一次会怎样 |
|----------|------------|--------------|----------------------|
| `androidTarget()` + `jvmTarget = JVM_17` | `build.gradle.kts:87-92` | 27 | 全工程 `androidTarget` 只出现 3 次（`build.gradle.kts:87`、`component/build.gradle.kts:20`、`composeApp/build.gradle.kts:30`）；后两处正是因为拿不到根接线 |
| `iosX64()`、`iosArm64()`、`iosSimulatorArm64()` | `build.gradle.kts:97-99` | 27 | 全工程只有 3 处声明过 `iosX64`：根 `build.gradle.kts:97`、`component/build.gradle.kts:29`、`composeApp/build.gradle.kts:40`；27 个库模块一处都没有 |
| `ohosArm64()`、`ohosX64()` | `build.gradle.kts:100-101` | 27 | 同上，只有 `build.gradle.kts:100`、`component/build.gradle.kts:32`、`composeApp/build.gradle.kts:52` 三处；且库模块侧一律**不产 framework 也不产 sharedLib**（`build.gradle.kts:95-96` 注释） |
| `compileSdk` | `build.gradle.kts:73`（读 catalog 的 `android-compileSdk`） | 27 | `component/build.gradle.kts:67` 用另一种读法（`libs.versions.android.compileSdk`），见 §2.3 |
| `minSdk` | `build.gradle.kts:75` | 27 | `component/build.gradle.kts:69` 自备 |
| `sourceCompatibility`、`targetCompatibility` = 17 | `build.gradle.kts:77-80` | 27 | `component/build.gradle.kts:71-74` 自备 |
| `apply kotlin.plugin.serialization` | `build.gradle.kts:107` | 27 | 幂等，不报错但属噪声；仍见 §2.2 |
| `implementation(kotlinx-coroutines-core)` | `build.gradle.kts:110` | 27 的 `commonMain` | 会重复声明同一坐标 |
| `implementation(atomicFu)` | `build.gradle.kts:111` | 27 的 `commonMain` | 同上 |
| `api(kotlinx-serialization-json)` | `build.gradle.kts:112` | 27 的 `commonMain` | **注意是 api**：序列化坐标对全部消费者的编译类路径外泄，见 §4.3 |
| `implementation(kotlin-test)` | `build.gradle.kts:115` | 27 的 `commonTest` | `component/build.gradle.kts:47` 与 `composeApp/build.gradle.kts:184` 必须自备 |
| `iosMain` 中间源集 + 3 个 ios 叶子源集挂接 | `build.gradle.kts:118-123` | 27 | 各模块因此可以直接 `getByName("iosMain")`，如 `core/base/build.gradle.kts:23` |
| `ohosMain` 中间源集 + 2 个 ohos 叶子源集挂接 | `build.gradle.kts:124-126` | 27 | `component/build.gradle.kts:56-60`、`composeApp/build.gradle.kts:169-180` 各自重写一遍 |
| 仓库顺序（eazytec 第一 + 排除 `com.android.tools.build`） | `build.gradle.kts:47-55` | 27 | 模块再写 `repositories {}` 是往同一个工程级仓库容器里**追加**条目而不是替换，顺序会被打乱；实测全工程除 `build.gradle.kts:47`、`settings.gradle.kts:5`、`settings.gradle.kts:15` 外没有任何第三处 |

`build.gradle.kts:104-106` 的注释给出这条清单的动机：「以前只有 `:composeApp` 单独 apply 过插件，拆模块后每个模块重复声明插件是噪声、漏声明是编译红」。

### 1.3 为什么这段接线只能待在根工程

`build.gradle.kts:25-29` 记录的是本机约束：这台机器的 `~/.gradle/init.gradle` 把 plugin marker 解析钉在阿里云与 eazytec 镜像上，`org.gradle.kotlin.kotlin-dsl` 的 marker 在这些源里取不到，所以 **buildSrc 约定插件这条路走不通**（AGENTS.md「Map」一节同样写明 buildSrc does not work）。替代方案是把共享接线写成 root 的 `subprojects {}` 回调，靠 `build.gradle.kts:4-9` 的 `apply false` 把 KGP、AGP 提前放上 root 的 scripts classpath，于是在 `build.gradle.kts:72`、`build.gradle.kts:83-85` 能直接用**类型化 API**（`LibraryExtension`、`KotlinMultiplatformExtension`）配置子工程，而不必退化成 `project.extensions.getByName(...)` 的动态写法。

### 1.4 时序钩子：为什么挂 `com.android.library` 而不是挂 kotlin 插件

`build.gradle.kts:64-70` 的注释把两个失败姿势都写下来了：

- 挂在 `kotlin.multiplatform` 的 `withId` 上 → AGP 还没应用，`androidTarget()` 直接报 "Please apply the Android Gradle Plugin"；
- 挂在 `afterEvaluate` 上 → `androidTarget` 建得太晚，AGP 的 debug、release variant 找不到对应 Kotlin 编译（"KotlinJvmAndroidCompilation with name 'debug' not found"）。

这条时序约束反过来**规定了模块 `plugins {}` 块的顺序**：`build.gradle.kts:65` 明文写「模块插件块一律 kotlinMultiplatform 在前、androidLibrary 在后」。我在本文整篇读过的 20 个 build 脚本里逐个核对了这个顺序，全部成立，例如 `core/base/build.gradle.kts:7-8`、`feature/chat/build.gradle.kts:6-7`、`component/build.gradle.kts:10-11`。

### 1.5 catalog 在回调里怎么取

`build.gradle.kts:59-62` 定义了一个闭包内私有函数 `versionCatalog()`，注释说明原因：`VersionCatalogsExtension` 要到子工程自己求值时才注册，所以不能在块外提前取。于是根侧读版本走 `catalog.findVersion("android-compileSdk")`（`build.gradle.kts:73`）、读坐标走 `catalog.findLibrary("kotlinx-coroutines-core")`（`build.gradle.kts:110`）；模块侧则是编译期生成的 `libs` 访问器（`libs.kotlinx.coroutines.core` 这种驼峰写法）。两种读法的目录来源是同一个 `gradle/libs.versions.toml`，别名分别在该文件 `gradle/libs.versions.toml:81`（`kotlinx-coroutines-core`）、`gradle/libs.versions.toml:4`（`android-compileSdk = "36"`）定义。

### 1.6 `-PandroidOnly=true` 在两侧的表现

`build.gradle.kts:57` 的 `skipNative` 与模块侧的 `androidOnly` 是同一属性的两次读取，例如 `core/base/build.gradle.kts:11`、`component/build.gradle.kts:17`、`composeApp/build.gradle.kts:17`、`deployment.gradle.kts:59`。它带来的实际差别是**源集是否存在**：`build.gradle.kts:118-127` 整段被 `if (!skipNative)` 包住，所以 `-PandroidOnly=true` 时 `iosMain`、`ohosMain` 由谁创建变成未知——模块侧因此发展出两种防御写法：

| 写法 | 出处 | 适用场景 |
|------|------|----------|
| `if (!androidOnly) { getByName("iosMain")… }` | `core/base/build.gradle.kts:22-25`、`core/network/build.gradle.kts:67-81` | 模块自己也要按开关分叉（依赖不同）时 |
| `matching { it.name == "iosMain" }.all { … }` | `core/storage/build.gradle.kts:28`、`feature/home/build.gradle.kts:23`、`core/ui/build.gradle.kts:33-40` | 源集存在与否不确定时；`core/storage/build.gradle.kts:27` 的注释直说「androidOnly CI 没有 ios target，所以用惰性匹配而不是 getByName」 |

`core/ui/build.gradle.kts:37-40` 更进一步对 `ohosArm64Main` 也用惰性匹配，因为该源集只在 native 打开且 arm64 target 存在时才出现。

---

## 2. 四类模块的配置差异对照

### 2.1 总表

| 类别 | 数量 | 命中根接线 | 必须自己写的部分 | 实例行数区间 | 一句话特征 |
|------|------|------------|------------------|--------------|------------|
| `:core:*` 水平公共 | 10 | 是（`build.gradle.kts:35`） | 插件别名、`namespace`、能力型中间源集、本模块特有依赖 | 10–96（`core/analytics/build.gradle.kts:1-10` … `core/network/build.gradle.kts:1-96`） | 「公共供给吃得最干净」的一类：最短文件只剩 plugins 与 namespace |
| `:feature:*` 业务独立 | 17 | 是（`build.gradle.kts:35`） | 插件别名、`namespace`、`api(projects.*)` 边、个别中间源集 | 21–38（`feature/live/build.gradle.kts` 21 行 … `feature/home/build.gradle.kts` 38 行） | 依赖面受门禁约束（§6），彼此不得依赖，所以每条边都是**向上索取公共库** |
| `:component` 端口层 | 1 | **否**（`build.gradle.kts:38` 提前 return） | target 全集、android 基础配置、iosMain 与 ohosMain 层级、coroutines、serialization、kotlin-test 全部自写 | 75（`component/build.gradle.kts:1-75`） | 路径不在 `:core:`、`:feature:` 命中范围内 → 根清单要手抄一份（`component/build.gradle.kts:6-8` 自己承认） |
| `:composeApp` 装配层 | 1 | **否**（同上） | 三端 target、iOS framework、OHOS sharedLib 与 cinterop、publish 任务、application 配置、17 条 feature 装配 | 300（`composeApp/build.gradle.kts:1-300`） | 唯一二进制产出方：其它 29 个工程都不产 framework 与 sharedLib（`build.gradle.kts:95-96`、`component/build.gradle.kts:28`） |
| `:ohosAggregate` 占位 | 1 | **否**（同上） | 空 plugins 块 + 一个 `tasks.register` | 27（`ohosAggregate/build.gradle.kts:1-27`） | 反例证据：命中范围只看路径，`ohosAggregate/build.gradle.kts:11-13` 连 KGP 都没 apply，根接线也不会替它补 |

### 2.2 `:core:*` 与 `:feature:*` 的写法几乎同构

两者被同一个谓词命中（`build.gradle.kts:35`），所以最小形态是一样的四段：

```kotlin
// feature/pay/build.gradle.kts:5-10 —— plugins（顺序受 build.gradle.kts:65 约束）
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinSerialization)
}
// feature/pay/build.gradle.kts:12-23 —— 只有依赖边
// feature/pay/build.gradle.kts:25-27 —— namespace
```

差别只在**内容**不在**结构**：core 侧的依赖边是「我提供能力」，例如 `core/design/build.gradle.kts:17-21` 一连串 `api(compose.*)`；feature 侧的依赖边是「我索取能力」，例如 `feature/media/build.gradle.kts:15-18`。另外 `:core:*` 有 4 个模块持有中间源集（`uuidCpfMain`、`networkKtorMain`、`settingsMain`、`imageCoilMain`），`:feature:*` 只有 1 个（`chartsVicoMain`，`feature/home/build.gradle.kts:14-20`）。

有个可见的小不一致：根在 `build.gradle.kts:107` 已经统一 apply 了 serialization 插件，但 `core/network/build.gradle.kts:10`、`core/account/build.gradle.kts:11`、`feature/pay/build.gradle.kts:9`、`feature/wallet/build.gradle.kts:9` 仍在自己的 plugins 块里声明 `alias(libs.plugins.kotlinSerialization)`。同一个 plugin id 二次 apply 不产生第二个实例（幂等），所以这四行属于「可读性优先于最小化」的重复——它们的注释（如 `feature/pay/build.gradle.kts:8`「MembershipModels 用 @Serializable」）说明声明目的是**标注模块意图**，不是提供能力。

### 2.3 `:component` —— 根清单的手抄副本，逐条可对照

| `component/build.gradle.kts` 里的行 | 内容 | 对应根侧「本来不用写」的那几行 |
|-------------------------------------|------|--------------------------------|
| `:20-25` | `androidTarget` + `jvmTarget` JVM_17 | `build.gradle.kts:87-92` |
| `:27-34` | `iosX64`、`iosArm64`、`iosSimulatorArm64`、`ohosArm64`、`ohosX64`，包在 `if (!androidOnly)` 里 | `build.gradle.kts:94-102` |
| `:37-45` | `commonMain` 依赖：4 条 `api(projects.core.*)` + `api(compose.runtime)` + coroutines、serialization | `build.gradle.kts:108-113`（coroutines、serialization 那三条） |
| `:46-48` | `commonTest` 的 `implementation(libs.kotlin.test)` | `build.gradle.kts:114-116` |
| `:49-61` | `iosMain`、`ohosMain` 中间源集与 5 个叶子源集挂接 | `build.gradle.kts:118-127` |
| `:65-74` | `namespace`、`compileSdk`、`minSdk`、`compileOptions` 17 | `build.gradle.kts:72-81` |

合计约 40 行是根接线的重复实现，占该文件 75 行的一半以上。两处写法还不完全对称：根用 `maybeCreate("iosMain")`（`build.gradle.kts:120`），component 用 `maybeCreate` 但拿的是 `commonMain.get()`（`component/build.gradle.kts:50-52`，因为它在 `kotlin.sourceSets` 命名空间里，容器元素是 `NamedDomainObjectProvider`）；根用 `catalog.findVersion`（`build.gradle.kts:73`），component 用 `libs.versions.android.compileSdk.get()`（`component/build.gradle.kts:67`）。语义相同、入口不同，原因是根的 `versionCatalog()` 是闭包内私有函数（`build.gradle.kts:60-62`），模块侧够不着。

`:component` 还额外拿到一样别人没有的东西：它 apply 了 compose 两个插件（`component/build.gradle.kts:12-13`），因此能用 CMP 插件提供的 `compose.` 扩展直接写 `api(compose.runtime)`（`component/build.gradle.kts:42`）而不必引用 catalog 的 `androidx.compose.*` 别名——`gradle/libs.versions.toml:120-126` 那一整段 AndroidX Compose 别名在全部 30 个子工程的 build 脚本里只有 `compose-ui-tooling` 被真正引用（`composeApp/build.gradle.kts:223`）。

### 2.4 `:composeApp` —— 只有它写「产物」

装配层与库层的根本差别不是依赖多少，而是**它是唯一声明 binaries 的工程**：

- iOS framework：`composeApp/build.gradle.kts:38-48`，`baseName = "ComposeApp"`、`isStatic = true`（`composeApp/build.gradle.kts:45-46`）。
- OHOS sharedLib：`composeApp/build.gradle.kts:51-109`，`baseName = "kn"`（`composeApp/build.gradle.kts:56`）、Release 关优化（`composeApp/build.gradle.kts:58-60`）、`export(libs.compose.multiplatform.export)`（`composeApp/build.gradle.kts:61`）、linkerOpts 与渲染模式分支（`composeApp/build.gradle.kts:66-91`）。
- OHOS cinterop：`composeApp/build.gradle.kts:93-108`（resource、avplayer 两条；`composeApp/build.gradle.kts:107` 注明 netHttp 已迁去 `:core:network`）。
- publish 任务：`composeApp/build.gradle.kts:259-300`，`if (!androidOnly)` 内注册 4 个 Copy 任务。
- Android application 段：`composeApp/build.gradle.kts:192-220`，含 `applicationId`（`:197`）、`targetSdk`（`:199`）、`packaging`（`:206-210`）。

其中三项与本文这条轴直接相关：`composeApp/build.gradle.kts:17` 自读 `androidOnly`（根侧那份在 `build.gradle.kts:57`，互不共享）、`composeApp/build.gradle.kts:116-159` 把 coroutines、atomicfu、serialization 三条公共依赖**重新手写一遍**（`composeApp/build.gradle.kts:157-159`，对照 `build.gradle.kts:110-112`），`composeApp/build.gradle.kts:183-188` 自己给 `commonTest` 与 `androidUnitTest` 补 kotlin-test（对照 `build.gradle.kts:115`）。`:composeApp` 也**不创建任何中间源集**，`composeApp/build.gradle.kts:113-115` 的注释把原因说清了：能力型依赖已全部下沉到 `:core:base`、`:core:ui`、`:core:platform`、`:feature:home`。

---

## 3. 逐文件走读（11 个模块 build 文件 + deployment 脚本）

### 3.1 `core/base/build.gradle.kts`（31 行）——中间源集的教科书最小形

- `:1-5` 头部注释即事实来源：`uuidCpfMain` 存在的原因是「CPF 只发了 android、ios、ohosArm64 的 uuid klib，ohosX64 没有，所以那边用 stub actual」。
- `:6-9` plugins 只有两个别名（`libs.plugins.kotlinMultiplatform`、`libs.plugins.androidLibrary`），没有 compose、没有 serialization——根 `build.gradle.kts:107` 已给 serialization，本模块也确实不需要 compose。
- `:11` 自读 `androidOnly`。
- `:15-20` `create("uuidCpfMain")` + `dependsOn(getByName("commonMain"))` + `implementation(libs.uuid)`；坐标别名定义在 `gradle/libs.versions.toml:110`，版本在 `gradle/libs.versions.toml:49`（`0.8.4-1.0.0`，CPF 线）。
- `:21` `getByName("androidMain").dependsOn(uuidCpfMain)` 无条件；`:22-25` `iosMain`、`ohosArm64Main` 的挂接包在 `if (!androidOnly)` 里。
- `:29-31` 唯一的 `android { namespace = … }`——这是根接线**没给**的字段（`build.gradle.kts:72-81` 只配 compileSdk、minSdk、compileOptions），所以 27 个模块无一例外都要自己写 namespace，形如 `core/base/build.gradle.kts:30`、`feature/chat/build.gradle.kts:28`。

### 3.2 `core/design/build.gradle.kts`（34 行）——资源 SoT 的外泄开关

- `:1-6` 注释给出归属论证：资源不能放 `:composeApp`，因为 feature 不能反向依赖 App（该纪律由 `scripts/check-layer-deps.sh` 第二轴守，见 §6.2）。
- `:7-12` plugins 四个别名，是全工程最常见的一组（对照 `core/ui/build.gradle.kts:7-12`、`feature/media/build.gradle.kts:5-10`）。
- `:16-22` 是 `commonMain` 依赖块，里面 5 条 `api`（`:17-21`）：`compose.runtime`、`compose.foundation`、`compose.material3`、`compose.ui`、`compose.components.resources`。这 5 条 compose 边是**全工程 Compose 可见性的源头**——传播链见 §4.2。
- `:26-30` `compose { resources { publicResClass = true } }`：不加这个，生成的 `Res` 类是 internal，别的模块读不到，`painterResource(Res.drawable.…)` 就无法跨模块使用。
- `:32-34` namespace。注意此模块**没有任何 `projects.*` 边**：它是依赖图的最底层叶子（除根接线给的三条之外）。

### 3.3 `core/network/build.gradle.kts`（96 行）——`networkKtorMain` 怎么挂

最长的库模块文件，五个层次：

1. `:1-2` 两条 import（`KotlinNativeTarget`、`java.io.File`），是模块文件里少见的需要 import 的例子。
2. `:15-23` `ohosNativeSysroot` 三级回退解析：`local.properties` 里以 `local.ohos.native=` 开头的行（`:16-18`）→ 环境变量 `OHOS_NATIVE_SYSROOT`（`:20`）→ DevEco 默认安装路径且存在才取（`:21-22`）。同一段逻辑在 `composeApp/build.gradle.kts:19-27` 逐字重复了一次——两处都是「机器本地路径不进 git」的落地方式。
3. `:26-40` 只在 `!androidOnly` 时给 `ohosX64` 建 `netHttp` cinterop：`targets.getByName("ohosX64") as KotlinNativeTarget`（`:29`）、`defFile(file("src/ohosMain/cinterop/net_http.def"))`（`:32`）。`:27-28` 的注释明确 arm64 走 Ktor CIO，不在此处。
4. `:42-91` source sets。`networkKtorMain` 在 `:48-58` 创建：`create` + `dependsOn(commonMain)` + 5 条 ktor 依赖（`:51-56`）。挂载方式是**三个平台源集各自 `dependsOn(networkKtorMain)`**：`androidMain`（`:60-65`，另加 okhttp engine）、`iosMain`（`:68-73`，另加 darwin engine）、`ohosArm64Main`（`:75-80`，另加 cio engine）。也就是说中间源集只放「三端共有的 ktor 门面与协商」，engine 留在各平台源集——这正是 `build.gradle.kts:31-33` 注释里「能力型中间 source set 留给模块自己声明」的具体形态。
5. `:83-90` 测试依赖：`commonTest` 补 coroutines（`:84`），`androidUnitTest` 补 ktor mock、okhttp、coroutines（`:86-89`）。全工程只有两个文件声明 `androidUnitTest`，另一个是 `composeApp/build.gradle.kts:186-188`。

### 3.4 `core/storage/build.gradle.kts`（34 行）——`settingsMain` 只覆盖一半 target

- `:1-10` 注释是这条决策的完整记录：官方 `multiplatform-settings` 1.3.0 没有 ohos klib，CPF 的 `-0.3.0` 线又没有 android AAR（实测 404），**没有任何一条线能同时覆盖六个 target**，所以 OHOS 两架构都由 `ohosMain` 直接实现 `KeyValueStore` 接口（OH_Preferences NDK），`Settings` 类型只活在 `settingsMain` 里。版本与原因在 `gradle/libs.versions.toml:35-38` 也留了一份。
- `:18-23` `create("settingsMain")` + `dependsOn(commonMain)` + `implementation(libs.multiplatform.settings)`；别名在 `gradle/libs.versions.toml:98`。
- `:25` 只挂 `androidMain`，`:28` 用 `matching { it.name == "iosMain" }.all { dependsOn(settingsMain) }` 挂 iOS。**注意这里没有 ohos 分支**——与 `:3-8` 的注释一致（OHOS 走 `ohosMain` 的接口实现，不引 settings）。
- 与 `core/base/build.gradle.kts:22-25` 对照可看出两种防御写法的分歧：base 是「三端都要这个中间源集，所以按开关分叉」，storage 是「只有 android 与 ios 要，所以用惰性匹配且不写 ohos」。

### 3.5 `core/database/build.gradle.kts`（31 行）——「公共供给」的极限演示

这个文件几乎是空的：plugins 两个别名（`:14-17`）、一条 `commonTest` coroutines（`:23-25`）、namespace（`:29-31`）。`:6-8` 的注释直接写明「target 集、android 基础配置、公共依赖（coroutines、atomicfu、serialization）、iosMain 与 ohosMain 层级全部由根 `build.gradle.kts` 的 `:core:*` 共享接线提供」。`:10-13` 解释为什么默认是 commonMain 内存实现（OHOS fork 无 Room、SQLDelight klib）。

唯一一条依赖 `:23-25` 值得单独理解，它是 §4.4 那条规律的原始出处：commonMain 的 coroutines 由根以 `implementation` 给出（`build.gradle.kts:110`），**不外泄给自己的 test 编译**，所以 commonTest 要读 `StateFlow`、`runBlocking`、`flow.first` 必须再显式补一条 `implementation(libs.kotlinx.coroutines.core)`（别名在 `gradle/libs.versions.toml:81`）。

### 3.6 `component/build.gradle.kts`（75 行）——端口层的完整自供

- `:1-8` 注释两条：依赖只允许向下（`core.network`、`core.base`、`core.account`、`core.router`），绝不允许 `feature.` 或 `app.`；并且明说自己不在根接线命中范围内。
- `:9-14` plugins 四个别名（含 compose 两个）——`:component` 提供带 UI 的端口实现，所以有 compose；对照 `core/network/build.gradle.kts:7-11`（无 compose，只有 serialization）。
- `:17` 自读 `androidOnly`，`:20-34` 自写 target（与 `build.gradle.kts:87-102` 等价）。
- `:36-45` `commonMain`：4 条 `api(projects.core.*)`（`:38-41`）+ `api(compose.runtime)`（`:42`）+ coroutines、serialization 各一条 `implementation`（`:43-44`）。后两条就是根 `build.gradle.kts:110`、`build.gradle.kts:112` 的替代品——但注意**scope 降级了**：根给的是 `api(serialization-json)`，component 自己写的是 `implementation`（`:44`），所以 `:component` 不会替 serialization 往外泄一层，实际外泄仍由根那份 api 完成（详见 §4.3）。
- `:46-48` `commonTest` 的 kotlin-test 替代 `build.gradle.kts:115`。
- `:49-61` 自建 `iosMain`、`ohosMain` 并挂 5 个叶子源集，替代 `build.gradle.kts:118-127`。这里用的是 `maybeCreate`（`:50`、`:56`）与根一致的写法。
- `:63` 闭合 `sourceSets`，`:65-75` android 段：`namespace`（`:66`）、`compileSdk`（`:67`）、`minSdk`（`:69`）、`compileOptions` 17（`:71-74`）。
- **本模块没有任何中间源集**，也没有 cinterop：它只依赖已经处理完产物缺口的 `:core:network`、`:core:base`。这是端口层能保持 75 行的原因。

### 3.7 `feature/home/build.gradle.kts`（38 行）——`chartsVicoMain`

- `:1-2` 注释把「哪些来自根」列成一句话：target 集、android 基础配置、coroutines、atomicfu、serialization、kotlin-test。
- `:14-20` `maybeCreate("chartsVicoMain")` + `dependsOn(getByName("commonMain"))` + `implementation(libs.vico.compose)`；版本理由写在 `gradle/libs.versions.toml:44-46`（Vico 2.3.0 对齐 Compose 1.9.2，3.x 会拉更高 Compose 与本仓 AGP 8.6、CMP 冲突），坐标别名在 `gradle/libs.versions.toml:107`。`:17` 的注释指出 Vico 传递的是 stock Compose 1.9.2，而工程自己的 `compose.*` 由 commonMain 侧胜出——这是「中间源集的依赖不外泄」的直接好处：`implementation` 在 `chartsVicoMain`（`:18`）而不是 commonMain。
- `:21-23` 挂法：`androidMain` 无条件、`iosMain` 用 `matching { it.name == "iosMain" }.all { … }`。`:22` 注释说明 iosMain 由根创建（`build.gradle.kts:120`），androidOnly 时它不存在。
- `:25-31` 四条 `api`：`core.ui`、`core.router`、`core.network`、`component`。`:26` 注明 core.ui 传递带出 core.platform、core.design、core.account；`:30` 注明拿的是 `component.webview`、`component.routing`。
- `:32-34` `commonTest` 再补 `implementation(projects.core.network)`（§4.4）。
- `:38` 单行式 `android { namespace = … }`——与其它文件的三行写法等价，纯风格差异。

这是全部 17 个 feature 里唯一带中间源集的文件，因此它同时演示了两件事：**产物缺口如何决定源集形状**、**feature 的公共依赖如何全靠 api 边**。

### 3.8 `feature/media/build.gradle.kts`（26 行）——feature 的标准形

`:1-4` 注释（播放器 actual 已随模块搬入各自源集，`:composeApp` 不再持有）→ `:5-10` plugins 四别名 → `:14-19` 三条 api（`core.ui`、`core.router`、`core.network`，其中 `:16` 那行注释「按我的清单增删，不要全量塞」是这套写法的纪律本身）→ `:20-22` commonTest 一条 → `:26` namespace。全文没有 `if (!androidOnly)`、没有 `matching`，因为这个模块不需要中间源集：它的平台差异全部走 expect、actual 的普通源集（`androidMain`、`iosMain`、`ohosMain` 都由根与 KGP 备好）。

### 3.9 `feature/pay/build.gradle.kts`（27 行）——不挂 compose 的 feature

`:3-4` 是全仓最硬的一条注释：本模块一个 `@Composable` 都没有，挂了 compose 插件会让 compose 编译器在 runtime 不在 classpath 时抛 `IncompatibleComposeRuntimeVersionException`（实测 `:feature:pay`、`:feature:wallet`）。因此 `:5-10` 的 plugins 只有 `kotlinMultiplatform`、`androidLibrary`、`kotlinSerialization` 三条。`:14-18` 两条 api：`projects.core.network`、`libs.kotlinx.serialization.json`——`:16` 给出判据「JsonElement 出现在公开模型签名里 → api」。`:25-27` namespace 用三行写法。

`feature/wallet/build.gradle.kts:1-27` 是同一模板的第二份拷贝（只有 `:8` 注释里的模型名与 `:26` namespace 不同），这类「纯 HTTP 契约模块」的接线成本因此压到 27 行。

### 3.10 `feature/chat/build.gradle.kts`（28 行）——靠 `:component` 少写三行

`:1-4` 注释先划清职责：引擎抽象（`ImEngine`、`MockImEngine`、`ImEngineStore`）在 `:component`，本模块只做 API 与 UI。`:14-21` 三条 api：`core.ui`（`:15`）、`core.network`（`:17`）、`component`（`:20`）。`:18-19` 的注释正是 §4.2 传播链的仓库自述：「`:component` 以 api 方式带出 `core.base`（`AppUuid` 供 `ImSessionApi` 生成会话 id）与 `core.router`、`core.network`，所以这里不再单列」。

对照 `component/build.gradle.kts:38-41` 的四条 api，这条「不单列」是成立的：chat 通过 `projects.component` 一条边拿到 `core.network`、`core.base`、`core.account`、`core.router` 四个模块的 api 面。而 chat 没有写的 `core.base` 恰恰是 `AppUuid` 的宿主（`core/base/build.gradle.kts:15-20` 的 `uuidCpfMain` 把它挂在 android、ios、ohosArm64 三个源集上）——**中间源集的实现细节完全被 api 边挡住了**，chat 侧看不到 uuid 坐标，只看到 `AppUuid` 的 expect 声明。

### 3.11 `composeApp/build.gradle.kts`（300 行）——17 行装配在哪、为什么是 implementation

- `:8-14` plugins 五条，与库层唯一的差别是 `alias(libs.plugins.androidApplication)`（`:10`）替代 `androidLibrary`（对照 `core/base/build.gradle.kts:8`）；`libs.plugins.androidApplication` 在 `gradle/libs.versions.toml:130`，`androidLibrary` 在 `gradle/libs.versions.toml:131`，两者同版本 `agp = "8.6.0"`（`gradle/libs.versions.toml:3`）。
- `:16-27` 自读 `androidOnly`、自建 `ohosNativeSysroot`（与 `core/network/build.gradle.kts:15-23` 同一段逻辑）。
- `:29-110` target 与 binaries，见 §2.4。
- `:112-115` 注释：能力型依赖全部下沉，`:composeApp` 不再创建任何中间源集。
- `:116-119` 只有 `androidMain` 两条（activity-compose、core-splashscreen）——装配层的平台专属依赖少到只有启动相关。
- `:120-130` 装配段：`implementation(projects.component)`（`:121`）+ 9 条 `implementation(projects.core.*)`（`:122-130`）——**全部 implementation，没有一条 api**。`:131-132` 的注释写明 app 是唯一允许看见全部 feature 的装配层。
- `:133-149` 就是那 17 行 `implementation(projects.feature.*)`，按字母序排（ai、auth、chat、classroom、commerce、community、content、friend、home、live、media、mine、pay、scan、shell、wallet、web），与 `settings.gradle.kts:39-42` 的 listOf 内容一致但**顺序不同**（后者按「新增优先」排列：content、web、live、scan…）。新增 feature 时这两处都要动，见 §7。
- `:150-159` compose 七条 + coroutines、atomicfu、serialization 三条，全 `implementation`；对照根 `build.gradle.kts:110-112`（api 的那条在这里降级成 implementation，`:159`）——因为 App 没有下游消费者，scope 在这里不影响任何人。
- `:161-181` 自建 `iosMain`、`ohosMain`：这里用 `sourceSets.create("iosMain")`（`:162`）而非 `maybeCreate`，因为 `:composeApp` 完全不被根接线覆盖，是这份文件自己第一次创建它们；`ohosMain` 还额外 `api(libs.compose.multiplatform.export)`（`:173`），配合 `:61` 的 `export(...)`。
- `:183-188` `commonTest` 与 `androidUnitTest` 各补一条 kotlin-test。
- `:192-224` android application 段与 `debugImplementation`。
- `:228-253` + `:259-300` publish 契约：`harmonyAppDir` 支持 `-PharmonyAppPath`（`:229-238`）、`harmonyResourceRoots` 把「模块目录 ↔ 生成资源包名」成对钉死（`:250-253`，注释 `:246-249` 明确 composeResources 已归 `:core:design`，`publicResClass` 见 `core/design/build.gradle.kts:26-30`）。

### 3.12 `deployment.gradle.kts`（656 行）——为什么模块文件里从来没有任务

`build.gradle.kts:20` 用 `apply(from = "deployment.gradle.kts")` 把它并进 root，所以里面 `tasks.register` 的 deploy、verify 任务（例如 `deployment.gradle.kts:151`、`deployment.gradle.kts:395`）**全部挂在 root 工程上**，27 个库模块与 `:component` 都拿不到。它甚至按 root 视角解析目录：`deployment.gradle.kts:119` 用 `layout.projectDirectory.asFile` 拼 `harmonyApp`。模块侧因此不需要注册任何任务——工程里唯一在模块内注册任务的两处是 `composeApp/build.gradle.kts:261-298`（publish Copy）与 `ohosAggregate/build.gradle.kts:15-27`（状态打印）。`deployment.gradle.kts:40-45` 的 `ExecOperationsHolder` 与 `:59` 的 `androidOnly` 复用，属于 09 的范围，此处只标注它同样读的是 root 属性、与 `build.gradle.kts:57` 是同一开关。

## 4. `api` 与 `implementation` 的传播链

### 4.1 本仓库两种 scope 的实际分布

| 文件 | `api(` 条数 | `implementation(` 条数 | 特征 |
|------|-------------|------------------------|------|
| `core/design/build.gradle.kts:17-21` | 5（全 compose） | 0 | 纯提供者：它的外泄面就是全工程 Compose 可见性来源 |
| `core/ui/build.gradle.kts:17-22` | 6 | 4（coil 三条线，`:26`、`:30`、`:35`、`:39`） | 能力提供者 + 产物缺口自藏 |
| `core/platform/build.gradle.kts:17-24` | 7 | 1（`projects.core.storage`，`:19`） | 用 `implementation` 挡住 KV 层，见 §4.3 |
| `core/account/build.gradle.kts:17-19` | 0 | 3 | 全私有的公共模块：会话模型靠根的 api 外泄 |
| `core/network/build.gradle.kts:44-90` | 0（本文件内） | 13（9 条非测试 + `commonTest` 1 条 + `androidUnitTest` 3 条） | 无 `projects.*` 边：`:core:network` 不依赖本工程任何模块 |
| `component/build.gradle.kts:38-47` | 5 | 3（`:43-44` 与 `commonTest` 的 `:47`） | 端口层用 api 把四个 core 往上送 |
| `feature/home/build.gradle.kts:27-33` | 4 | 2（`:18` vico、`:33` 测试） | 标准 feature |
| `feature/chat/build.gradle.kts:15-23` | 3 | 1 | 靠 `:component` 省掉三条 |
| `feature/pay/build.gradle.kts:15-20` | 2 | 1 | 无 compose 边 |
| `composeApp/build.gradle.kts:120-160` | 0 | 37 | 装配层不需要外泄，所以一律 implementation |

规律：**越靠下越要 `api`，越靠上越可以 `implementation`**；只有 `:composeApp` 例外地全 `implementation`，因为它没有下游（它唯一的 `api` 在 `composeApp/build.gradle.kts:173`，那是给 OHOS sharedLib 的 `export` 用的，消费者是链接期而不是 Gradle）。

### 4.2 一条真实传播链：`:feature:chat` 写了 3 条，看见了 8 个模块

```
feature/chat/build.gradle.kts:15  api(projects.core.ui)
  └─ core/ui/build.gradle.kts:17      api(projects.core.platform)
       ├─ core/platform/build.gradle.kts:17  api(projects.core.design)
       │    └─ core/design/build.gradle.kts:17-21  api(compose 5 条) + publicResClass(:28)
       ├─ core/platform/build.gradle.kts:18  api(projects.core.account)
       └─ core/ui/build.gradle.kts:18-22     api(compose 5 条)
feature/chat/build.gradle.kts:17  api(projects.core.network)
feature/chat/build.gradle.kts:20  api(projects.component)
  ├─ component/build.gradle.kts:38  api(projects.core.network)
  ├─ component/build.gradle.kts:39  api(projects.core.base)
  ├─ component/build.gradle.kts:40  api(projects.core.account)
  └─ component/build.gradle.kts:41  api(projects.core.router)
```

chat 因此可见 8 个本工程模块（`core.ui`、`core.platform`、`core.design`、`core.account`、`core.network`、`core.base`、`core.router`、`component`）与整套 compose 坐标，却只写了 3 条边。它**看不见**的是被 `implementation` 挡住的三样：`core.storage`（`core/platform/build.gradle.kts:19`、`core/account/build.gradle.kts:18`）、`coil`（`core/ui/build.gradle.kts:26`）、`uuid`（`core/base/build.gradle.kts:18`）——chat 用的是 `AppUuid` 这个 expect 声明，而不是 `com.benasher44:uuid` 坐标本身。

### 4.3 少写的行与一处需要修正的注释

按 §4.2 的图，一个「有 UI、有网络、有路由」的 feature 不需要显式列出的边至少 5 条：`core.platform`、`core.design`、`core.account`、`core.base`、`compose.*`。这条规律是被有意维护的——7 个 feature 文件把传递关系写在注释里：`feature/media/build.gradle.kts:15`、`feature/chat/build.gradle.kts:15`、`feature/community/build.gradle.kts:14`、`feature/friend/build.gradle.kts:15`、`feature/mine/build.gradle.kts:13`、`feature/auth/build.gradle.kts:18` 六处措辞相同（「传递带出 core.platform → core.design、core.account」），`feature/home/build.gradle.kts:26` 是同义变体（多加了一句 compose）。

需要修正的一条：`feature/pay/build.gradle.kts:16` 写「JsonElement 出现在公开模型签名里 → api；`:core:network` 只 implementation」。后半句只描述了 `core/network/build.gradle.kts:45` 那一行，**没有考虑根接线**——`build.gradle.kts:112` 以 `api` 给全部 27 个模块（含 `:core:network`）挂了 `kotlinx-serialization-json`，别名见 `gradle/libs.versions.toml:83`、版本 `gradle/libs.versions.toml:31`。所以 serialization 的编译面事实上已经在往外泄，`:feature:pay` 与 `:feature:wallet` 的 `api(libs.kotlinx.serialization.json)`（`feature/pay/build.gradle.kts:17`、`feature/wallet/build.gradle.kts:17`）在效果上是冗余保险，写它的价值是把「本模块公开签名里有 JsonElement」这件事留在文件里可读。

### 4.4 为什么 `commonTest` 还要再写一遍

11 个 feature 的 `commonTest` 各补一条 `implementation(projects.core.network)`（`feature/home/build.gradle.kts:33`、`feature/chat/build.gradle.kts:23`、`feature/pay/build.gradle.kts:20` 等；实测 17 个 feature 文件里有 11 个声明了 `commonTest`），4 个 core 模块也在 commonTest 补坐标（`core/database/build.gradle.kts:23-25`、`core/network/build.gradle.kts:83-85`）。`core/database/build.gradle.kts:21-22` 给出了这套写法的自述理由：commonMain 的 coroutines 是 `implementation`（不外泄），commonTest 读 `StateFlow`、`Flow` 需要显式补一条。与之呼应的是 `build.gradle.kts:114-116` 只在 `commonTest` 给 `kotlin-test` 一条——所以 feature 文件里看不到 kotlin-test，看得到 `projects.core.network`。

### 4.5 `projects.*` 访问器为什么可用

`settings.gradle.kts:2` `enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")` 打开开关；访问器名由工程路径推导（路径段变嵌套、`-` 变驼峰），所以 `settings.gradle.kts:24` 的 `include(":core:network")` 生成 `projects.core.network`，`settings.gradle.kts:38-43` 的 forEach 生成的 `:feature:pay` 对应 `projects.feature.pay`（用于 `composeApp/build.gradle.kts:145`）。两个隐含前提值得记住：

1. **必须先 include**。访问器只覆盖 settings 里已声明的工程；漏 include 时不是「运行时找不到模块」，而是 `projects.feature.newmod` 在编译 build 脚本阶段就解析不出来。
2. **前缀由 `rootProject.name` 定调**。`settings.gradle.kts:1` 的 `My_kmp_project` 同时也是 Compose 资源生成包名的组成部分——`composeApp/build.gradle.kts:250-253` 的 `harmonyResourceRoots` 里那个 `my_kmp_project.core.design.generated.resources` 就是它拼出来的（注释见 `composeApp/build.gradle.kts:248-249`）。

---

## 5. `settings.gradle.kts`：两条 include 路径与镜像钉死（全 44 行）

### 5.1 12 条显式 + 17 个 forEach，切的是「稳定底座 vs 可增删叶子」

| 段 | 行 | 内容 | 计数 |
|----|----|------|------|
| 工程与开关 | `1-2` | `rootProject.name`、`enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")` | — |
| pluginManagement | `4-12` | 插件仓库：eazytec（`7`）→ google、mavenCentral、gradlePluginPortal（`8-10`）；`mavenLocal()` 被注释（`6`） | 4 源 |
| dependencyResolutionManagement | `14-21` | 依赖仓库：eazytec（`17`）→ google、mavenCentral（`18-19`）；**没有 gradlePluginPortal** | 3 源 |
| 显式 include | `23-34` | `:composeApp`（`23`）+ 10 个 `:core:*`（`24-33`）+ `:component`（`34`） | 12 条 |
| 注释纪律 | `36-37` | 「feature 叶子模块：模块之间禁止相互依赖（R7 + check-layer-deps.sh 第 1/3 条），每个都能单独 `:feature:x:testDebugUnitTest`」 | — |
| forEach include | `38-43` | `listOf(17 个名字).forEach { include(":feature:$it") }` | 17 条 |
| 占位 | `44` | `include(":ohosAggregate")` | 1 条 |

我实际核对过两边的集合：`settings.gradle.kts:24-33` 十个 core 名与 `core/` 下十个目录一一对应；`settings.gradle.kts:39-42` 的 17 个字符串与 `feature/` 下 17 个目录一一对应，无多无少。写法差异不是风格问题：**底座 12 条要按依赖顺序可读**（network 在 base 之前、component 最后，读起来就是分层图），**叶子 17 条要便于增删**（一行一名，新增 feature 只在 listOf 里加一个字符串）。

### 5.2 include 不等于装配：现存的一处缺口

`settings.gradle.kts:33` include 了 `:core:database`，但全工程没有任何 `build.gradle.kts` 声明 `projects.core.database`——`composeApp/build.gradle.kts:122-130` 只列了 9 个 core（network、account、router、design、base、analytics、storage、platform、ui）。这说明两件事：include 只让模块**能被单独构建与测试**（`build.gradle.kts:37-130` 的接线照样生效），进 App 是另一条显式边。`:core:database` 目前是「已接线、已可单测、尚未装配」的形态，与其文件注释 `core/database/build.gradle.kts:10-13`（等平台 SQL driver 就位）一致。

### 5.3 镜像钉死：两处仓库列表 + 一处 exclusion，为什么不冲突

`settings.gradle.kts:14-21` 没有写 `repositoriesMode`，因此 Gradle 默认 **PREFER_PROJECT**：工程级 `repositories {}` 生效、settings 的仅作回退。这条判断的仓库自述在 `build.gradle.kts:46`（「settings 的 dependencyResolutionManagement 默认 PREFER_PROJECT：工程级 repositories 生效」）。于是形成两条解析链：

| 谁 | 用哪条链 | 有无阿里云 | 有无 `com.android.tools.build` 排除 |
|----|----------|-----------|--------------------------------------|
| 27 个 `:core:*`、`:feature:*` | `build.gradle.kts:47-55` | 有（`build.gradle.kts:51-52`） | 有（`build.gradle.kts:49`） |
| `:component`、`:composeApp`、`:ohosAggregate` | `settings.gradle.kts:14-21` | 无 | 无 |

根侧那条链的顺序是被实测钉死的，`build.gradle.kts:40-45` 的注释写了两个理由：OHOS fork 独有坐标（例如 `org.jetbrains.androidx.lifecycle:lifecycle-common:2.9.4-0.3.0` 这类）只在 eazytec，阿里云对它报 socket、SSL 错，而 **Gradle 把传输错误判成整次解析失败、不会接着试下一个仓库**，所以 eazytec 必须排第一；反过来 eazytec 不代理 AGP 二进制（aapt2 同样报 socket 错），所以用 `mavenContent { excludeGroup("com.android.tools.build") }`（`build.gradle.kts:49`）把 aapt2 落到阿里云的 google 镜像（`build.gradle.kts:51`）。

### 5.4 版本目录与接线的一对一关系

根接线不写死任何坐标字符串，全部经 catalog 取（`build.gradle.kts:60-62` 的 `versionCatalog()`）。下表是这条轴上「谁读 catalog 哪一行」的对照：

| catalog 定义 | 消费方 | 用途 |
|--------------|--------|------|
| `gradle/libs.versions.toml:4`（`android-compileSdk = "36"`） | `build.gradle.kts:73`、`component/build.gradle.kts:67`、`composeApp/build.gradle.kts:194` | 同一数字的两种读法：根用 `catalog.findVersion`，模块用 `libs.versions.*` 访问器 |
| `gradle/libs.versions.toml:5`（`android-minSdk = "26"`） | `build.gradle.kts:75`、`component/build.gradle.kts:69`、`composeApp/build.gradle.kts:198` | 库层用 `minSdk`，App 另加 `targetSdk`（`composeApp/build.gradle.kts:199`、catalog `:6`） |
| `gradle/libs.versions.toml:27`（`kotlin = "2.2.21-0.3.0"`） | `gradle/libs.versions.toml:135`、`gradle/libs.versions.toml:138-139`、`gradle/libs.versions.toml:57` | compose 编译器插件、KMP 插件、kotlin-test 都挂这条 |
| `gradle/libs.versions.toml:28` + `:81` | `build.gradle.kts:110`、`core/network/build.gradle.kts:84`、`core/database/build.gradle.kts:24`、`component/build.gradle.kts:43` | 根给一条，缺它的地方补 |
| `gradle/libs.versions.toml:29` + `:82` | `build.gradle.kts:111`、`composeApp/build.gradle.kts:158` | atomicfu：只有 App 需要重抄 |
| `gradle/libs.versions.toml:31` + `:83` | `build.gradle.kts:112`、`core/network/build.gradle.kts:45`、`feature/pay/build.gradle.kts:17` | serialization-json 的三条线（api、implementation、再 api） |
| `gradle/libs.versions.toml:33`（`ktor = "3.3.3-0.3.0"`） | `gradle/libs.versions.toml:86-95` → `core/network/build.gradle.kts:51-56`、`:63`、`:71`、`:78` | 只有 `:core:network` 引 ktor，别的模块走 api 边 |
| `gradle/libs.versions.toml:38` + `:98` | `core/storage/build.gradle.kts:21` | 单点消费，正是「Settings 类型只活在 settingsMain」的实现 |
| `gradle/libs.versions.toml:41-43` + `:101-103` | `core/ui/build.gradle.kts:26`、`:30`、`:35`、`:39` | coil 的 CPF 线与 stock okhttp 线混用，注释在 `gradle/libs.versions.toml:42` |
| `gradle/libs.versions.toml:46` + `:107` | `feature/home/build.gradle.kts:18` | vico 单点消费 |
| `gradle/libs.versions.toml:49` + `:110` | `core/base/build.gradle.kts:18` | uuid 单点消费 |
| `gradle/libs.versions.toml:23`（`composeMultiplatform`） | `build.gradle.kts:6-7`、各模块 plugins 块（如 `core/design/build.gradle.kts:10-11`） | 两处 apply：root `apply false` 去重、模块真 apply |
| `gradle/libs.versions.toml:78`（export 坐标） | `composeApp/build.gradle.kts:61`、`:173` | 唯一二进制产出方专属 |

---

## 6. 门禁如何守住分层

### 6.1 `scripts/check-layer-deps.sh`（全 65 行）三条轴逐行

轴 1 源码层 feature 互引（`scripts/check-layer-deps.sh:18-28`）：`find` 的扫描根是 `composeApp/src` 与 `feature` 两处（`:28`，兼顾「还在 App 里」与「已下沉成模块」两种形态，见 `:10` 注释）；`:20` 用 `sed` 从路径抽出所属 feature 名；`:27` 只喂 `grep -E "^import com\.example\.my_kmp_project\.feature\."` 的结果；`:23-25` 用 `BASH_REMATCH` 取出被引的 feature 名，与自身不同即 `report`。

轴 2 低层向上引（`scripts/check-layer-deps.sh:30-38`）：`:37-38` 的 `find` 有四个扫描根但带 `-path '*/core/*' -o -path '*/component/*'` 过滤，所以真正被检的是 core 与 component 两种形态的 `.kt`；`:36` 的模式是 `^import $PKG\.(feature|app)\.`——一个正则同时挡「向上引业务」和「引装配层」。

轴 3 Gradle 层（`scripts/check-layer-deps.sh:40-59`）：三条同构的循环，扫描对象不是源码而是 **`build.gradle.kts`**，模式统一是 `projects.feature.`：

| 子轴 | 行 | `find` 根 | 判定含义 |
|------|----|-----------|----------|
| 3 | `41-45` | `feature` | feature 模块的 build 文件里出现 `projects.feature.` 即违规（兄弟依赖） |
| 3b | `47-52` | `core` | core 层不许依赖任何 feature |
| 3c | `54-59` | `component` | component 层不许依赖任何 feature |

`scripts/check-layer-deps.sh:61-64` 是唯一的失败出口（`violations > 0` → 打印计数 + `exit 1`），`scripts/check-layer-deps.sh:65` 打印全清。它由 `scripts/verify-parity.sh:168`（`bash scripts/check-layer-deps.sh || exit 1`）调用，因此随 parity 门进 CI（CI 的 Gradle 命令在 `.github/workflows/ci.yml:80-81`，带 `-PandroidOnly=true`）。

### 6.2 它匹配什么、漏什么（对照本文这条轴）

匹配得住的：`implementation(projects.feature.x)`、`api(projects.feature.x)` 任何形式的兄弟边（模式是子串 `projects.feature.`，不挑 scope）；注释里若出现 `projects.feature.` 也会被抓（子串匹配不区分代码与注释——这是偏保守的副作用，值得写模块注释时避开该字面量）。

漏掉的六处，都能在行号上定位到成因：

1. **`:composeApp` 的 build 文件不在轴 3 的扫描根里**（`:45` 只 find `feature`），所以 App 装配边的完整性（是否漏了第 18 个 feature）没有门可查，只能靠 `settings.gradle.kts:38-43` 与 `composeApp/build.gradle.kts:133-149` 人工对齐——§5.2 的 `:core:database` 缺口就是同类问题的 core 版本，而 `:47-52` 的子轴 3b 只查 feature，不查「core 是否被 App 装配」。
2. **`:ohosAggregate` 完全不在三条轴的扫描范围内**（`find` 根只有 `composeApp/src`、`feature`、`core`、`component`）。
3. **轴 1 不看别名坐标**：`implementation("com.example.my_kmp_project.feature:home")` 这种手写 GAV 绕过 `projects.feature.`（`:44`、`:51`、`:58` 三处模式都一样）。仓库惯例是优先 catalog 与 `projects.*`（AGENTS.md Conventions 一节），所以现实中没出现，但脚本堵不住。
4. **轴 1 依赖 `^import`**（`:27`）：不写 import 而用全限定名（`com.example.my_kmp_project.feature.chat.Foo()`）就扫不到；一行 import 里出现两个 feature 包时 `BASH_REMATCH` 只取第一个（`:23-24`）。
5. **轴 2 只查 import，不查「低层实现被上层间接暴露」**：例如 feature 以 `api` 把某个 core 模块送出去（`component/build.gradle.kts:38-41`）属于设计内行为，脚本无法区分「合理的 api 传播」与「不该有的传递」。
6. **传递违规要逐层查**：A→B→C 三个 feature 的链在 feature↛feature 纪律下不可能出现（第一跳已违规），但 `core → component → feature` 这种两跳需要 3b 与 3c 同时生效才拦得住，脚本本身不做闭包计算。

### 6.3 `scripts/with-gradle-lock.sh`（全 37 行）为什么必须串行

`scripts/with-gradle-lock.sh:2-4` 的注释就是理由：它与 `scripts/verify-parity.sh` 用**同一把建议锁**，目的是「一个 agent 的窄构建不会撞上另一个 agent 的 daemon 请求」，并记录了一次本机实测病例——**等锁 666 秒后失败，还被误归因成「别人的文件坏了」**。同一算法在两处各实现一份：`scripts/verify-parity.sh:23-46` 与 `scripts/with-gradle-lock.sh:13-29`。

串行机制的四个部件：`mkdir` 的原子性当 test-and-set（`scripts/with-gradle-lock.sh:18`）；读 `pid` 文件并用 `kill -0` 判活，持锁进程已死则回收锁目录（`:19-22`）；每 10 秒重试（`:28`）；总超时由 `GRADLE_LOCK_WAIT` 控制、默认 3600 秒（`:15`、`:23-26`）。收尾三行是它区别于 `scripts/verify-parity.sh` 的地方：`echo "$$" > "$lock_pid"` 记录持锁者（`:31`）、`trap cleanup EXIT` 保证异常也放锁（`:32-33`）、结尾**原样透传被包命令的退出码**（`:35-37`），因此可以安全套在任何窄构建外面；无参数时 `exit 2`（`:11`）。

为什么这套工程尤其需要它：根接线让一次「窄构建」也要配置全部 31 个工程（`subprojects {}` 在每个子工程求值时都要跑一遍 `build.gradle.kts:70-129`），配置阶段本来就重；并发跑两个 Gradle 时它们抢的是同一个 daemon 与同一份 `build` 目录，撞锁的表现是超时而不是快速失败，事后日志极易误判。**本文未运行任何 Gradle 命令**，上述代价判断来自根脚本的求值范围与脚本注释里的病例记录。

---

## 7. 新增一个 feature 或 core 模块：最小 checklist

**两处必改，一处必补，顺序无关。**

1. **`settings.gradle.kts` 加 include。** core 模块加在 `settings.gradle.kts:24-33` 那一段（与底座放在一起），feature 模块加进 `settings.gradle.kts:39-42` 的 `listOf(...)` 字符串列表。这一步让 `projects.core.newmod`、`projects.feature.newmod` 访问器存在（`settings.gradle.kts:2`）。
2. **写模块自己的 `build.gradle.kts`**，最小三段的参照物：
   - 不带 UI、不带缺口的 core 模块 → 抄 `core/analytics/build.gradle.kts:1-10`（10 行：两个插件别名 + `android { namespace }`，其余全由 `build.gradle.kts:37-130` 给）。
   - 带 Compose UI 的 feature → 抄 `feature/media/build.gradle.kts:1-26`（四个插件别名 + 几条 `api(projects.core.*)` + 一条 `commonTest` + namespace）。
   - 纯 HTTP 契约、不挂 compose 的 feature → 抄 `feature/pay/build.gradle.kts:1-27`，并保留 `feature/pay/build.gradle.kts:3-4` 那条「挂了会抛 IncompatibleComposeRuntimeVersionException」的注释。
   - 需要引入「只有部分 target 有产物」的库 → 抄中间源集形态：`core/base/build.gradle.kts:15-25`（三端挂法 + `if (!androidOnly)`）或 `core/storage/build.gradle.kts:18-28`（只挂 android 与惰性 iosMain）。
   - 硬性约束：plugins 块必须 `kotlinMultiplatform` 在前、`androidLibrary` 在后（`build.gradle.kts:65`）；`namespace` 必须自己写（根不给，见 `build.gradle.kts:72-81` 的字段清单）。
3. **补 App 装配边**：新 feature 在 `composeApp/build.gradle.kts:133-149` 的字母序列表里插一行 `implementation(projects.feature.newmod)`（新 core 则插在 `:122-130`）。不补这行模块照样能单测，但 App 里永远看不见它——`:core:database` 就是当前的活例子（§5.2）。

**改完该跑什么（只列命令，本文未执行）**：

| 目的 | 命令 |
|------|------|
| 只验接线能被配置 | `./gradlew help --console=plain` |
| 验分层门禁 | `bash scripts/check-layer-deps.sh` |
| 验新 feature 单测（Android，最快） | `./gradlew :feature:newmod:testDebugUnitTest -PandroidOnly=true` |
| 验全量 Android 装配 | `./gradlew :composeApp:assembleDebug -PandroidOnly=true` |
| 验 native target（含 OHOS） | `scripts/with-gradle-lock.sh ./gradlew :feature:newmod:build` |
| parity 全套静态门（内含 check-layer-deps） | `bash scripts/verify-parity.sh --static-only` |

`-PandroidOnly=true` 这条路径同时验证 §1.6 的防御写法是否成立（此时 `iosMain`、`ohosMain` 不由根创建），CI 用的正是它（`.github/workflows/ci.yml:80-81`）。

---

## 8. 本文证据强度自评

| 断言 | 依据类型 | 强度 |
|------|----------|------|
| 命中范围、行数、依赖边条数、17 行装配位置 | 直接读文件与 `grep -c` 实测（本文全部行号） | 强 |
| 「同一 plugin id 二次 apply 幂等」（§2.2） | Gradle 通用语义推断，仓库内无明文 | 中：未跑 Gradle 验证 |
| 「`implementation` 不外泄给 commonTest 编译类路径」（§4.4） | 仓库自述 `core/database/build.gradle.kts:21-22`；KMP 内部机制未实测 | 中：机制细节可能因 KGP 版本而异 |
| 「`:component`、`:composeApp` 走 settings 仓库链，因此没有阿里云」（§5.3） | `build.gradle.kts:38` 的 early-return + `settings.gradle.kts:14-21` 内容 | 强（静态事实）；但其运行时后果（aapt2 能否解析）未验证 |
| 「窄构建也要配置全部 31 个工程，因此撞锁代价高」（§6.3） | `subprojects {}` 求值范围推断 + `scripts/with-gradle-lock.sh:2-4` 记录的 666s 病例 | 中：666s 是脚本注释里的历史观测，非本文复现 |

# 09 构建与配置 —— settings / 根工程 / deployment / catalog / wrapper / OHOS 脚本 / composeApp

## 0. 一句话定位与架构位置

这一层是「整个 31 个 Gradle project（root + 30 子工程）如何被组装、三端产物如何被生产并投递到原生壳」的唯一事实源：`settings.gradle.kts` 定义工程边界与仓库镜像，根 `build.gradle.kts` 用 `subprojects{}` 给 27 个库模块统一接线，`composeApp/build.gradle.kts` 是唯一二进制产出方的 target/产物/publish 配置，`deployment.gradle.kts` + `runscript/*.sh` 是把产物装到真机/模拟器的操作层。分层图里它横贯所有层（①composeApp 装配 ← ④core/③component/②feature 接线 ← ⑤三端壳投递），对应 `settings.gradle.kts:23-44` 的 include 全集。

## 1. 文件清单

| 路径 | 作用 | 总行数 | 关键行号 |
|------|------|--------|----------|
| `settings.gradle.kts` | 工程名、feature preview、镜像仓库、30 个 include | 45 | `4-12` pluginManagement、`14-21` dependencyResolutionManagement、`23-44` include |
| `build.gradle.kts`（根） | 插件去重加载、apply deployment、`:core:*`/`:feature:*` 共享 KMP 接线 | 131 | `4-9` apply false、`20` apply(from)、`35` 模块过滤、`70-129` withId("com.android.library") |
| `deployment.gradle.kts` | 三端 deploy/verify 任务（ExecOperations 注入） | 657 | `40-45` Holder、`151-292` verify、`395-655` deploy |
| `gradle.properties` | JVM/缓存/Android/Kotlin-native/渲染模式开关 | 24 | `8` configuration-cache、`22` rendererBackend |
| `gradle/libs.versions.toml` | 版本目录（versions/libraries/plugins 三段） | 141 | `27` kotlin=2.2.21-0.3.0、`128-141` plugins |
| `gradle/wrapper/gradle-wrapper.properties` | Gradle 9.4.1，腾讯镜像下载 | 7 | `3` distributionUrl |
| `gradlew` / `gradlew.bat` | 标准 POSIX/Windows 启动脚本（`gradle/wrapper/gradle-wrapper.jar` 45457 B） | 251 / 94 | `gradlew:205` JVM opts、`gradlew:213-217` `-jar` wrapper |
| `runscript/runOhosApp-Mac.sh` | macOS 一键：Gradle publish → ohpm/hvigor HAP → hdc 装机启动 | 328 | `168-182` gradle 分发、`197-203` HAP |
| `runscript/runOhosApp-Win.bat` | Windows 等价脚本（无 LLDB 调试） | 321 | `155-165` gradle 分发、`187-219` HAP |
| `ohosAggregate/build.gradle.kts` | Spike II OHOS fat-link 占位模块 | 28 | `11-13` 空 plugins、`15-27` 状态 task |
| `composeApp/build.gradle.kts` | 三端 target、iOS framework、OHOS sharedLib "kn"、cinterop、publish 任务 | 301 | `44-47` framework、`55-92` sharedLib、`259-300` publish |
| `docs/architecture/ohos-aggregate.md` | ohosAggregate 迁移计划（对照文档） | 69 | `38-47` 状态表、`59-64` 迁移清单 |

## 2. settings.gradle.kts（全 45 行）

- `settings.gradle.kts:1` `rootProject.name = "My_kmp_project"` —— 决定 `projects.*` typesafe 访问器前缀，也是 Compose 资源生成包名前缀的一部分（见 §10.6 `harmonyResourceRoots`）。
- `settings.gradle.kts:2` `enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")` —— 使 `implementation(projects.core.network)` 这类写法可用（composeApp 大量使用，如 `composeApp/build.gradle.kts:122`）。
- `settings.gradle.kts:4-12` **pluginManagement.repositories**：`maven("https://maven.eazytec-cloud.com/nexus/repository/maven-public/")` 排第一（`7`），其后 google/mavenCentral/gradlePluginPortal（`8-10`）。eazytec 是 CPF OHOS fork 线（`-0.3.0` 系版本）所在的私服；`mavenLocal()` 被注释掉（`6`）。解析 `org.jetbrains.kotlin.multiplatform:2.2.21-0.3.0` 这类非官方 plugin marker 必须命中它。
- `settings.gradle.kts:14-21` **dependencyResolutionManagement.repositories**：同样 eazytec 第一（`17`），无 gradlePluginPortal（依赖仓库不需要 plugin marker）。注意这里没写 `repositoriesMode`，Gradle 默认 **PREFER_PROJECT**，所以根/模块级 `repositories {}` 会覆盖本块（根 `build.gradle.kts:46` 的注释明确了这一点）。
- include 全集（30 个子工程 + root = 31 个 Gradle project）：
  - `23-34`：**显式 12 个 include**，逐条意图：
    - `include(":composeApp")`（`23`）——①装配层、唯一二进制产出方（iOS framework + OHOS libkn.so），Android application 也在这里。
    - `include(":core:network")`（`24`）——④横向 commons，唯一持有 Ktor 的模块（`core/network/build.gradle.kts:3` 头注释）。
    - `include(":core:account")`（`25`）——会话/账号，依赖 :core:network。
    - `include(":core:router")`（`26`）——路由抽象。
    - `include(":core:design")`（`27`）——设计 token + **持有全部 composeResources drawable/font**（`composeApp/build.gradle.kts:246-247` 注释；AGENTS.md:81）。
    - `include(":core:base")`（`28`）——最底层公共件，CPF uuid 住这里（`composeApp/build.gradle.kts:113`）。
    - `include(":core:analytics")`（`29`）、`include(":core:storage")`（`30`，KeyValueStore 契约 + multiplatform-settings 线）、`include(":core:platform")`（`31`，CameraX/MLKit 扫码落点）、`include(":core:ui")`（`32`，Coil 网络图落点）。
    - `include(":core:database")`（`33`）——实体持久化层，与 storage 的 KV 互补（`core/database/build.gradle.kts:1-3` 头注释）；**但目前未被任何模块 api/implementation 引用，见 §12.11**。
    - `include(":component")`（`34`）——③纵向端口层单模块（pay/push/chat/webview/auth/routing/realtime 端口）。
  - `36-43`：**17 个 feature 用 forEach 批量注册**——`listOf("content","web","live","scan","pay","wallet","ai","classroom","friend","commerce","community","media","chat","shell","auth","mine","home").forEach { include(":feature:$it") }`。`36-37` 注释声明规则：feature 叶子模块禁止相互依赖（R7 + `scripts/check-layer-deps.sh` 第 1/3 条），每个都能单独 `./gradlew :feature:x:testDebugUnitTest`。
  - `44`：`include(":ohosAggregate")` —— Spike II OHOS fat-link 占位模块（见 §9），`plugins {}` 为空所以 include 它不产生任何 target。

## 3. 根 build.gradle.kts（全 131 行）

### 3.1 plugins 块：`apply false` 去重

`build.gradle.kts:1-10` 用 catalog alias 声明 androidApplication / androidLibrary / composeMultiplatform / composeCompiler / kotlinMultiplatform / kotlinSerialization 六个插件但 `apply false`（`2-3` 注释：避免插件在每个子工程 classloader 里重复加载）。这同时把 KGP/AGP 放进了根工程 scripts classpath，是后文能用类型化 API（`LibraryExtension`、`KotlinMultiplatformExtension`）配置子工程的前提（`28-29` 注释）。

### 3.2 apply(from = "deployment.gradle.kts")

`build.gradle.kts:20` 引入部署脚本；`12-19` 注释交代三件事：①借鉴 kmp-cmp-example 并适配本仓；②wrapper 升到 Gradle **9.4.1** 后 `Project.exec`/脚本级 `exec` 在 9.0 被移除（官方迁移指南 "Removal of Project#exec, Project#javaexec, and script-level counterparts"），deployment 内 29 处全部改为注入的 `ExecOperations`（取用方式见 `deployment.gradle.kts:40-45`）；③这些任务在 `doLast` 里直接起进程，**与 configuration cache 不兼容**（而 `gradle.properties:8` 开了 cache），且依赖真机/模拟器，CI 门禁从不跑它们。

### 3.3 为什么共享接线在根工程而不是 buildSrc

`build.gradle.kts:22-34` 注释：本机 `~/.gradle/init.gradle` 把插件解析钉在阿里云 + eazytec 镜像上，`org.gradle.kotlin.kotlin-dsl` 的 plugin marker 在这些源解析不到（实测 `:help` 直接失败），buildSrc 约定插件路线走不通；因此改为根工程直接配置子工程，且只放 100% 共用部分——namespace、能力型中间 source set（ktor/coil/uuid/vico/settings）、Compose 插件都留给模块自己声明（`31-33`）。

### 3.4 subprojects{ 共享接线（只对 :core:* 与 :feature:*，逐块）

- `build.gradle.kts:35` 谓词 `libraryModulePaths`：`path.startsWith(":core:") || path.startsWith(":feature:")`；`:composeApp`、`:component`、`:ohosAggregate` **不在接线范围内**（`:component` 因此自己完整声明 target 集——其 `component/build.gradle.kts:6-8` 头注释明说此点，`17` 读 androidOnly、`27-33` 自建五 native target；`:ohosAggregate` 干脆无 plugins）。
- `37-38` `subprojects { if (!libraryModulePaths(this)) return@subprojects }`——对不符合谓词的工程直接跳过，composeApp 的 android 配置因此不被本块重复设置。
- **仓库顺序钉死**（`40-55`，注释 `41-46`）：
  - `maven(eazytec)` 第一且 `mavenContent { excludeGroup("com.android.tools.build") }`（`48-50`）——OHOS fork 独有坐标（如 `org.jetbrains.androidx.lifecycle:lifecycle-common:2.9.4-0.3.0`，`41` 注释举例）只在 eazytec；阿里云对它报 socket/SSL 错误，而 **Gradle 把传输错误判成整次解析失败、不会 fallback 下一个仓库**，所以 eazytec 必须排第一（`42-43`）。
  - 反向：eazytec 不代理 AGP 二进制（aapt2 也 socket 错，`44-45`），故排除 `com.android.tools.build` 后让 aapt2 落到 `maven.aliyun.com/repository/google`（`51`）；再 `aliyun public`（`52`）、`google()`（`53`）、`mavenCentral()`（`54`）。
  - `46` 注释：settings 的 dependencyResolutionManagement 默认 PREFER_PROJECT，所以工程级 repositories 生效、覆盖 §2 的两个块。
- `57` `skipNative = -PandroidOnly` 为 true——CI Android 专用裁剪开关（`.github/workflows/ci.yml` 跑 `testDebugUnitTest :composeApp:assembleDebug -PandroidOnly=true`）。
- `59-62` `versionCatalog()` 辅助函数：VersionCatalogsExtension 要到子工程自己求值时才注册，**只能在回调体里取**，不能提前在根求值。
- `64-69` 关键时序注释（三选一 why）：共享接线挂在 **`plugins.withId("com.android.library")`**（`70`）之后——挂 `kotlin.multiplatform` 时 AGP 还没应用，`androidTarget()` 直接报 "Please apply the Android Gradle Plugin"；挂 `afterEvaluate` 时 androidTarget 建得太晚，AGP debug/release variant 报 "KotlinJvmAndroidCompilation with name 'debug' not found"。前提假设：模块插件块一律 kotlinMultiplatform 在前、androidLibrary 在后（`65` 注释；如 `core/network/build.gradle.kts:8-11` 遵守该顺序）。
- **android 基础配置块**（`72-81`，`extensions.configure(LibraryExtension::class)`）：`compileSdk = catalog android-compileSdk.toInt()`（`73`）、`defaultConfig.minSdk = android-minSdk`（`75`）、`compileOptions` source/target 均 Java 17（`77-80`）。namespace **不在这**（留给模块，`32-33`）。
- **targets 配置**：
  - `83-85` 从 extensions 拿 `KotlinMultiplatformExtension`。
  - `87-92` `kmp.androidTarget { compilerOptions { jvmTarget.set(JVM_17) } }`（`ExperimentalKotlinGradlePluginApi` OptIn）。
  - `94-102` `if (!skipNative)` 里依次 `iosX64()`（`97`）、`iosArm64()`（`98`）、`iosSimulatorArm64()`（`99`）、`ohosArm64()`（`100`）、`ohosX64()`（`101`）——只建 target，**没有任何 `binaries {}` 调用**：`95-96` 注释「库模块一律不产 framework / sharedLib：整个工程只有 :composeApp 是二进制产出方」。库模块的 native 侧只产 klib，供 composeApp 最终链接。
- **序列化公共接线**（`104-107`）：`pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")`。注释（`104-106`）：core/feature 的 `@Serializable` 数据契约满地都是（114 行 API 契约表每行都要请求/响应模型）；以前只有 composeApp apply，拆模块后逐模块重复声明是噪声、漏声明是编译红，所以插件+json 依赖统一给。
- **公共依赖**（`108-116`）：commonMain `implementation(kotlinx-coroutines-core)`（`110`）、`implementation(atomicFu)`（`111`）、**`api(kotlinx-serialization-json)`**（`112`，api 级——序列化契约要透传给上游模块）；commonTest `implementation(kotlin-test)`（`114-116`）。全部经 catalog `findLibrary(...)` 动态取而非 `libs.` 访问器（因为回调在根工程求值，根没有 `libs` 扩展，见 `59-62`）。
- **iOS/OHOS 源集层级**（`118-127`，同样受 skipNative 保护）：`maybeCreate("iosMain").dependsOn(commonMain)`（`120`），`iosX64Main/iosArm64Main/iosSimulatorArm64Main` 三个 per-target source set `dependsOn(iosMain)`（`121-123`）；`ohosMain` 同构挂 `ohosArm64Main`/`ohosX64Main`（`124-126`）。Android/iOS/OHOS 三端 actual 由此有了共享父层。

## 4. deployment.gradle.kts（全 657 行，由根 `build.gradle.kts:20` apply）

来源与适配记录在头部注释 `deployment.gradle.kts:2-25`：借鉴 gitcode `CPF-KMP-CMP/kmp-cmp-example`，做了 7 条适配——包名/Ability/bundle 取本仓真值（`8-12`）、目录大小写 `harmonyApp`（`13`）、HAP 构建用 DevEco 自带 `hvigorw.js`、安装走 `hdc shell bm install -p` 且失败自动卸载重试（`14-16`）、发布产物路径带架构子目录（`17-18`）、本工程无 staticLib 故不生成 verifyHarmonyStaticLib* 任务（`19-20`）、ohosX64 link 被工具链阻塞（Konan_cxa_demangle）默认不注册 X64 任务（`21-22`）、支持 `-PandroidOnly=true` 不注册 iOS/OHOS 任务（`23`）。

### 4.1 Gradle 9 迁移：ExecOperations 注入（`32-45`）

Gradle 9.0 移除 `Project.exec`/脚本级 exec（`33-37` 注释，提到 wrapper 从 8.9 升 9.4.1 后旧写法配置阶段 234 个 `Unresolved reference 'exec'`）。替代写法：声明带 `@get:Inject` 属性接口的 `ExecOperationsHolder`（`40-43`），再用 `objects.newInstance(...)` 实例化取出 `execOperations`（`45`），全文 29 处统一用 `execOperations.exec { commandLine(...) }`（如 `127-131`、`163-167`），API 与旧写法一致。

### 4.2 常量与开关（`47-61`）

| 变量 | 值 | 行号 | 来源 |
|------|----|------|------|
| `androidPackage` | `com.example.my_kmp_project` | `48` | composeApp applicationId |
| `androidLaunchActivity` | `…MainActivity` | `49` | Manifest 推断 |
| `harmonyPackage` | `com.example.harmonyapp` | `50` | AppScope/app.json5 bundleName |
| `harmonyAbility` | `EntryAbility` | `51` | entry module mainElement |
| `harmonyAppDirName` / `harmonyEntryModule` | `harmonyApp` / `entry` | `52-53` | 目录名 |
| `iosBundleId` / `iosScheme` | `com.example.my_kmp_project.my_kmp_project` / `iosApp` | `54-55` | Config.xcconfig（TEAM_ID 空） |
| `androidOnly` | `-PandroidOnly` | `59` | 与 composeApp 同一读取方式 |
| `harmonyX64Enabled` | `-PharmonyX64` | `61` | X64 任务开关 |

### 4.3 工具函数

- `findExecutable(name)`（`67-111`）：先扫 PATH，再查常见位置——adb 查 `ANDROID_HOME`/`ANDROID_SDK_ROOT`/mac `~/Library/Android/sdk/platform-tools`/Win `AppData\Local\Android\Sdk`（`81-90`）；hdc 查 `DEVECO_SDK_HOME` 与 `/Applications/DevEco-Studio.app/...toolchains`（`91-100`）；都找不到就返回裸命令名交给 shell（`110`）。
- `findHvigorw()`（`113-122`）：本仓 harmonyApp 内无 hvigorw 脚本，优先 `DEVECO_HOME/tools/hvigor/bin/hvigorw.js`（默认 `/Applications/DevEco-Studio.app/Contents`），回退 harmonyApp 内脚本或 PATH。
- `resolveHarmonyDeviceId`（`124-145`）：`hdc list targets` 解析序列号；支持 `-Ptarget=<device>` 覆盖；无设备返回空串由调用方跳过安装。

### 4.4 任务清单（跑它会发生什么 / 产出落到哪）

| 任务 | 行号 | dependsOn | 跑起来的效果 / 产物路径 |
|------|------|-----------|--------------------------|
| `verifyHarmonyKexe` | `151-243` | `:composeApp:linkDebugTestOhosArm64` | 产物 `composeApp/build/bin/ohosArm64/debugTest/test.kexe`（`157`）；有设备则 hdc 推到 `/data/local/tmp/test.kexe`、chmod +x、带 `LD_LIBRARY_PATH=/system/lib64/platformsdk:/system/lib64/ark:/system/lib64` 运行（`182-211`）；对 Permission denied（noexec）与 libark/libace 缺失给中文提示（`224-228`）；无设备仅校验产物存在即过（`170-173`）。group=verification。 |
| `verifyHarmonySharedLib` | `245-270` | `:composeApp:linkDebugSharedOhosArm64` | 校验 `composeApp/build/bin/ohosArm64/debugShared/{libkn.so,libkn_api.h}` 存在并打印大小（`254-267`）。 |
| `verifyHarmonyHapIntegration` | `272-292` | `:composeApp:publishDebugBinariesToHarmonyApp` | 校验已拷贝到 `harmonyApp/entry/src/main/cpp/include/arm64-v8a/libkn_api.h` 与 `harmonyApp/entry/libs/arm64-v8a/libkn.so`（`282-283`）。注意 `deployment.gradle.kts:17-18` 特别说明本工程路径带架构子目录、与示例不同。 |
| `verifyHarmonyKexeOhosX64` / `…SharedLibOhosX64` / `…HapIntegrationOhosX64` | `298-389`（整段被 `if (harmonyX64Enabled)` 包住） | `linkDebugTestOhosX64` / `linkDebugSharedOhosX64` / `publishDebugBinariesToHarmonyAppX64` | 与 arm64 版同构，路径换成 `ohosX64/debug*` 与 `x86_64`；仅 `-PharmonyX64=true` 时注册（当前该 link 被 Konan_cxa_demangle 阻塞）。 |
| `deployAndroid` | `395-449` | `:composeApp:assembleDebug` | 在 `composeApp/build/outputs/apk/debug` 取最新非 unaligned APK（`426-433`）；`adb force-stop → install -r → am start -n 包/MainActivity`（`438-446`）；无设备直接打印后返回。group=deployment。 |
| `deployIos` | `455-542`（仅 `isMac()` 注册） | `:composeApp:linkDebugFrameworkIosSimulatorArm64` | 在 `iosApp/` 跑 `xcodebuild -scheme iosApp -sdk iphonesimulator -derivedDataPath build/ios`（`470-479`，derivedData 落在仓库根 `build/ios`，见 `466`）；`xcrun simctl list devices booted` 找模拟器（`487`，可用 `-Ptarget` 指定）；install + 从 Info.plist 用 PlistBuddy 提取 bundle id（`518-535`）+ launch（`538`）。 |
| `deployHarmony` | `548-656`（`if (!androidOnly)` 才注册） | `:composeApp:publishDebugBinariesToHarmonyApp`（`554`；`harmonyX64Enabled` 时追加 X64 publish `555-557`） | 拼 DevEco PATH（`567-579`）→ `hdc shell aa force-stop`（`586`）→ `ohpm install --all`（`590-596`）→ `node hvigorw.js --sync …buildMode=debug`（`597-603`）→ `--mode module … assembleHap`（`604-610`）→ 在 `harmonyApp/entry/build/default/outputs/default` 找 HAP、优先 `*-signed.hap`（`613-621`）→ 推 `/data/local/tmp/debug_install` + `bm install -p`，输出含 failed 则 `bm uninstall` + 5s 后重装（`631-648`）→ `aa start -a EntryAbility -b com.example.harmonyapp`（`652`）。 |

任务调用的公共参数/行为约定：

- 所有 verify/deploy 任务 group 分别是 `verification` / `deployment`（`deployment.gradle.kts:152,246,273,396,457,550`），`./gradlew tasks` 可见。
- `-Ptarget=<device>`：deployAndroid（`416-421`）、deployIos（`482-484`）、deployHarmony（经 `resolveHarmonyDeviceId`，`135-138`）、verifyHarmonyKexe（`174-179`）都认它；不传则自动取第一台在线设备/booted 模拟器。
- 无设备时行为不统一：verify/deployAndroid/deployIos/deployHarmony 均「打印提示后 return@doLast，构建仍算成功」（`170-173,411-414,496-499,625-628`），只有产物缺失才抛 `GradleException`（如 `159,285,430,616`）。
- 这些任务与 configuration cache 不兼容（`build.gradle.kts:18-19`）：doLast 里 `execOperations.exec` 起外部进程、读 `System.getenv`，触发时按 Gradle 9 规则需 `--no-configuration-cache` 或接受 problem report；工程惯例是门禁/CI 不跑它们，日常跑用 `scripts/run-*.sh`。
- `Thread.sleep(5000)` 出现在 deployHarmony 卸载重装之间（`645`），与 runOhosApp-Mac.sh 的 `sleep 5`（`253,255`）等价。

## 5. gradle.properties（全 24 行，逐 key）

| Key | 行号 | 实际作用 | 消费方/依据 |
|-----|------|----------|--------------|
| `kotlin.code.style=official` | `3` | IDE/Kotlin 官方代码风格标记 | 仅风格 |
| `kotlin.daemon.jvmargs=-Xmx3072M` | `4` | Kotlin 编译守护进程堆上限；OHOS/iOS native 编译内存大，配合 `composeApp/build.gradle.kts:57-60` release 关 optimized 防 OOM | 全局 |
| `org.gradle.jvmargs=-Xmx4096M -Dfile.encoding=UTF-8` | `7` | Gradle daemon 堆 + 编码 | 全局 |
| `org.gradle.configuration-cache=true` | `8` | 开启配置缓存；**deployment.gradle.kts 的 doLast 起进程任务与之不兼容**（`build.gradle.kts:18-19` 注释，门禁不跑这些任务） | 全局 |
| `org.gradle.caching=true` | `9` | build cache | 全局 |
| `android.nonTransitiveRClass=true` | `12` | 模块 R 类不传递（AGP 行为） | android 侧 |
| `android.useAndroidX=true` | `13` | 启用 AndroidX | android 侧 |
| `android.suppressUnsupportedCompileSdk=36` | `14` | compileSdk 36 高于 AGP 8.6.0 官方支持范围，压制警告（catalog `libs.versions.toml:3-4`） | android 侧 |
| `kotlin.native.cacheKind=none` | `16` | 关闭 KN framework 原生缓存（framework 静态产物下常见配置，避免缓存不一致） | iOS/native |
| `org.gradle.internal.http.connectionTimeout=100000` / `socketTimeout=100000` | `18-19` | 私服（eazytec/aliyun）慢，拉长 HTTP 超时至 100s | 依赖下载 |
| `rendererBackend=fusion-renderer` | `22` | OHOS 渲染模式选择；被 `composeApp/build.gradle.kts:76` 读取，`==fusion-renderer` 时追加 11 个 `-l` linker 选项（`77-91`） | composeApp OHOS sharedLib |
| `#rendererBackend=skia` | `23` | 注释掉的备选值；改为它则 composeApp 不追加 fusion 链接选项（走 skia 线） | 同上 |
| `ohosSkikoVersion=0.9.22.2-0.3.0` | `24` | **未找到任何 Gradle 脚本读取它**（全仓 `*.kts` grep 无消费方），仅 `AGENTS.md`/工作记忆提及，疑似遗留开关 | — |

注意：`-PandroidOnly`、`-PharmonyAppPath`、`-PharmonyX64`、`-Ptarget` **不写在本文件**，是命令行 `-P` 项目属性，读取点见 `build.gradle.kts:57`、`composeApp/build.gradle.kts:17/230`、`deployment.gradle.kts:59/61/135`。

## 6. gradle/libs.versions.toml（全 141 行）

组织方式：`[versions]`（`1-53`）→ `[libraries]`（`55-126`）→ `[plugins]`（`128-141`），libraries 内按「Testing / AndroidX core / Lifecycle & Compose / CMP export / KMP 并发 / Ktor / settings / Coil / Vico / UUID / CameraX+MLKit / Compose Android」分组注释排列。本工程的核心特征：**凡带 `-0.3.0`（或 `-1.0.0`）后缀的都是 CPF OHOS fork 线**，只存在于 eazytec 镜像。

### 6.1 versions 逐条

| Key | 值 | 行号 | 说明 |
|-----|----|------|------|
| `agp` | 8.6.0 | `3` | androidApplication/androidLibrary 共用 |
| `android-compileSdk/minSdk/targetSdk` | 36 / 26 / 36 | `4-6` | 消费方：根 `build.gradle.kts:73-75`（库模块 compileSdk/minSdk）与 `composeApp/build.gradle.kts:194/198-199`（三值全用） |
| AndroidX 系（`activity`…`splashscreen`、`testExt`、`espresso`） | — | `9-18` | stock Google 版本 |
| `compose` | 1.9.4 | `21` | **Android 平台 androidx.compose** 线（`compose-ui` 等 `120-126`），与 CMP 无关 |
| `compose-material3` | 1.4.0 | `22` | 同上（`compose-material3` alias `126`） |
| `composeMultiplatform` | 1.9.2-0.3.0 | `23` | **CPF 线**：org.jetbrains.compose 插件 + `compose-multiplatform-export`（`78`） |
| `kotlin` | 2.2.21-0.3.0 | `27` | **CPF 线**（stock 无 2.2.21-0.3.0）；驱动 kotlinMultiplatform/kotlinSerialization/composeCompiler/cocoapods 四个 plugin alias（`135-140`）与 kotlin-test*（`57-58`） |
| `kotlinx-coroutines` | 1.10.2-0.3.0 | `28` | CPF 线（含 ohos klib） |
| `atomicFu` | 0.31.0-0.3.0 | `29` | CPF 线 |
| `kotlinx-serialization` | 1.9.1-0.3.0 | `30-31` | `30` 注释：必须带 ohos_arm64，stock 1.8.x 没有 |
| `ktor` | 3.3.3-0.3.0 | `32-33` | CPF OH 线（ohosArm64），注释指向 adopt-cpf-ktor-ohos |
| `multiplatformSettings` | 1.3.0 | `35-38` | **stock**；`35-37` 注释解释无一条 settings 线同时覆盖 Android+OHOS（stock 无 ohos klib，CPF 线无 android AAR 404），OHOS 走 OH_Preferences NDK |
| `coil` | 3.3.0-0.3.0 | `39-41` | CPF 线，禁入 commonMain/ohosX64（无 klib），经 imageCoilMain 接线 |
| `coil-okhttp` | 3.3.0 | `42-43` | **stock Central 专用**——CPF `coil-network-okhttp:3.3.0-0.3.0` 是 metadata-only 无 androidJvm 产物 |
| `vico` | 2.3.0 | `44-46` | Android/iOS only；`45-46` 注释：2.3.0 对齐 Compose 1.9.2，3.x 会拉 Compose 1.10+/1.12 与本仓 AGP 8.6/CMP 冲突 |
| `uuid` | 0.8.4-1.0.0 | `47-49` | CPF OH 线，经 uuidCpfMain |
| `camerax` / `mlkit-barcode` | 1.4.2 / 17.3.0 | `51-53` | androidMain 扫码专用（WebView 用框架自带、无额外依赖，见 `51` 注释） |

### 6.2 libraries → 消费方实测映射（grep 全仓 build.gradle.kts）

| alias（行号） | 消费模块 |
|---------------|----------|
| `kotlin-test`（`57`） | 根共享接线 `build.gradle.kts:115`（findLibrary）+ `component/build.gradle.kts` + `composeApp/build.gradle.kts:184,187` |
| `kotlinx-coroutines-core`（`81`）、`atomicFu`（`82`）、`kotlinx-serialization-json`（`83`） | 根 `build.gradle.kts:110-112` 统一下发；composeApp/component 另有直接引用 |
| `ktor-client-*` 8 个 + `ktor-serialization-kotlinx-json`（`85-95`） | 全部在 `core/network/build.gradle.kts`（cio/darwin/okhttp/mock/websockets/logging/content-negotiation…） |
| `multiplatform-settings`（`98`） | 仅 `core/storage/build.gradle.kts`（Android/iOS settingsMain 线） |
| `coil-compose` / `coil-network-okhttp` / `coil-network-ktor3`（`101-103`） | 仅 `core/ui/build.gradle.kts` |
| `vico-compose`（`107`，注释 `106`：2.x 工件名仍叫 multiplatform） | 仅 `feature/home/build.gradle.kts` |
| `uuid`（`110`） | 仅 `core/base/build.gradle.kts` |
| `androidx-camera-*` 4 个 + `mlkit-barcode-scanning`（`113-117`） | 仅 `core/platform/build.gradle.kts` |
| `androidx-core-ktx`（`60`）、`androidx-activity-compose`（`72`）、`androidx-lifecycle-viewmodelCompose/runtimeCompose`（`74-75`） | `core/platform`；activity-compose/splashscreen 另见 `composeApp/build.gradle.kts:117-118` |
| `compose-multiplatform-export`（`78`） | `composeApp/build.gradle.kts:61`（sharedLib export）与 `:173`（ohosMain api） |
| `compose-ui-tooling`（`121`） | `composeApp/build.gradle.kts:223` debugImplementation |
| `androidx-lifecycle-viewmodel`（`73`）、`compose-ui/runtime/foundation/material/material3/tooling-preview`（`120,123-126`） | 未找到直接 `libs.` 引用（composeApp 用的是 CMP 插件提供的 `compose.*` 访问器，`composeApp/build.gradle.kts:150-156`），视为预留别名 |
| `kotlin-testJunit`（`58`）、`junit`（`59`）、`androidx-testExt-junit`（`61`）、`androidx-espresso-core`（`62`）、`androidx-appcompat`（`65`）、`androidx-collection`（`66`）、`androidx-constraintlayout`（`67`）、`androidx-material`（`69`） | **全仓 build 脚本未找到消费方**（声明保留） |

### 6.3 plugins（`128-141`）

- `androidApplication` / `androidLibrary` → `com.android.application` / `com.android.library`@agp 8.6.0（`129-131`）：前者仅 composeApp（`composeApp/build.gradle.kts:10`），后者全部 `:core:*`/`:feature:*`/`:component`。
- `composeMultiplatform` → `org.jetbrains.compose`@**1.9.2-0.3.0**（`133-134`）：CPF 线 CMP 插件。实测 apply 方：composeApp、component、core:{design,platform,router,ui} 与 15 个 feature（除 pay/wallet 外全部）；不 apply：core:{base,network,account,analytics,storage,database}、feature:{pay,wallet}。它同时提供 `compose.*` 依赖访问器（composeApp `150-156` 用的就是这套，而非 `libs.compose.*`）。
- `composeCompiler` → `org.jetbrains.kotlin.plugin.compose`@kotlin（`135`）：版本跟随 2.2.21-0.3.0 的 KGP fork。
- `kotlinMultiplatform`（`137-138`）、`kotlinSerialization`（`139`）：前者所有 KMP 模块；后者除根 `build.gradle.kts:9,107` 统一 apply false + withId 里 programmatic apply 外，部分模块也自行 alias 声明（如 `composeApp/build.gradle.kts:13`）。
- `cocoapods` → `org.jetbrains.kotlin.native.cocoapods`@kotlin（`140`）：**全仓未找到任何模块 apply 它**——iOS 集成走手工 `binaries.framework` + Xcode 工程（`composeApp/build.gradle.kts:44-47`），该 alias 属预留。
- 根 `build.gradle.kts:4-9` 把前六个插件 `apply false` 预载到根 classpath（去重 + 供 subprojects 类型化 API 用）。

### 6.4 「哪些别名是 OHOS 专用 CPF 线」速判

版本号以 `-0.3.0` / `-1.0.0` 结尾 = CPF OHOS fork，只能从 eazytec 镜像解析：`kotlin`、`composeMultiplatform`、`kotlinx-coroutines`、`atomicFu`、`kotlinx-serialization`、`ktor`、`coil`（仅 compose/network-ktor3）、`uuid`、`compose-multiplatform-export`。stock 专用：`multiplatformSettings 1.3.0`（`35-38` 注释：CPF 线无 android AAR）、`coil-okhttp 3.3.0`（`42-43` 注释：CPF 同名包 metadata-only）、`vico 2.3.0`（`44-46` 注释：CPF 无 ohos 适配）、全部 androidx/Compose-Android/mlkit。混用 consequences 已写在各注释里（如 3.x Vico 拉高 Compose 会撞 AGP 8.6）。

## 7. gradle/wrapper 与 gradlew

- `gradle/wrapper/gradle-wrapper.properties:3` `distributionUrl=https\://mirrors.tencent.com/gradle/gradle-9.4.1-bin.zip`——**Gradle 9.4.1，走腾讯镜像**（与 `deployment.gradle.kts:35` "8.9 升到 9.4.1" 注释互相印证，即 §4.1 迁移的起因）；`distributionBase/Path`、`zipStoreBase/Path` 走默认 GRADLE_USER_HOME（`1-2,6-7`）；`networkTimeout=120000`（`4`）、`validateDistributionUrl=true`（`5`）。
- `gradlew`（251 行）是 Gradle 官方生成的标准 POSIX 模板（头注释 `21-64` 说明来自 `unixStartScript.txt`）：解析符号链接求 APP_HOME（`70-89`）、按 `JAVA_HOME`→PATH 找 java（`121-143`）、`DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'`（`205`）、以 `-jar $APP_HOME/gradle/wrapper/gradle-wrapper.jar` 启动（`213-217`）、`exec "$JAVACMD" "$@"`（`251`）。`gradlew.bat` 为 Windows 对等物（94 行）。`gradle/wrapper/gradle-wrapper.jar` 实存 45457 字节。AGENTS.md 要求一律用 `./gradlew` 而非系统 Gradle。
- 概念区分：`gradlew:205` 的 `-Xmx64m` 只是 **wrapper 引导进程**的堆；真正 daemon 编译堆由 `gradle.properties:7`（4096M）与 `:4` Kotlin daemon（3072M）决定，二者互不影响。
- 镜像三层：Gradle 本体走腾讯镜像（`gradle-wrapper.properties:3`）、插件/依赖走 eazytec+google+central（`settings.gradle.kts:7,17`）、库模块再覆盖一层 eazytec+阿里云（`build.gradle.kts:47-55`）——换任何一个都属敏感改动（见 §12.2、§13）。

## 8. runscript/（OHOS 一键装机脚本，被 scripts/run-harmony.sh 包装）

`scripts/run-harmony.sh:18-24`：macOS 上 `exec runscript/runOhosApp-Mac.sh "$@"`，非 mac 直接提示用 Win.bat——即 runscript 是真正的实现层。

### 8.1 runOhosApp-Mac.sh（328 行，Eazytec CPF 版权头 `3-16`）

1. 参数与默认值（`24-29`）：PLATFORM=ohosArm64、TARGET_ID=`127.0.0.1:5555`（模拟器默认地址）、BUNDLE=`com.example.harmonyapp`、ABILITY=`EntryAbility`；`-m debug|release`、`-b/-a/-d debug|attach`、`-p 外部 OHOS 工程路径`、`-h`（usage `31-52`，选项解析支持任意位置 `63-84`）。
2. 环境校验（`98-133`）：`DEVECO_PATH`（默认 `/Applications/DevEco-Studio.app`）必须存在；从 Info.plist `CFBundleShortVersionString` 读版本，要求 **≥ 6.0.0**（`100,119-131`），不满足退出码 5。
3. 导出环境（`134-141`）：`HDC_BIN=$DEVECO_HOME/sdk/default/openharmony/toolchains/hdc`、`DEVECO_SDK_HOME`、PATH 拼入 jbr/node/ohpm/hvigor；先 `hdc shell aa force-stop` 预停应用（`140`）。
4. Gradle 任务名可被环境变量覆盖（`143-160`）：`OHOS_KMP_RUN_GRADLE_MODULE` 默认 `composeApp`、发布任务按 BUILD_MODE 选 `publishDebug|ReleaseBinariesToHarmonyApp`，拼出 `:composeApp:publish…`；`-p PATH` 时给 gradle 追加 `-PharmonyAppPath=...`（`157-160`）——这正是 `composeApp/build.gradle.kts:229-238` 读取的开关。
5. 构建分发（`162-183`）：PLATFORM=ohosArm64 跑上面拼好的 publish 任务；=iosSimulatorArm64 跑 `linkDebugFrameworkIosSimulatorArm64`（`177-178`）；其他退出码 4。
6. HAP 打包（`186-204`）：cd harmonyApp → `ohpm install --all` → `node …hvigorw.js --sync -p product=default -p buildMode=$BUILD_MODE …` → assembleHap（release 多带 `compileNative`，`199-203`）。
7. 安装（`206-256`）：期望签名 HAP `entry/build/default/outputs/default/entry-default-signed.hap`（`208-210`），缺失则列原因并以退出码 6 提示去 DevEco 配置签名（`213-231`）；设备离线退出码 5（`232-236`）；推 `/data/local/tmp/debug_install` + `bm install -p`，输出含 failed 则 `bm uninstall` + sleep 5 重装（`241-256`）。
8. 启动与收尾（`257-329`）：清临时目录；`aa start -a $ABILITY -b $BUNDLE`，DEBUG_MODE=debug 时加 `-D`（等待 LLDB attach，`273-281`）；识别锁屏错误码 `10106102/screen is locked` 并给人工解锁指引（`287-297`）；最后分段打印 SO 构建/HAP 打包/总耗时（`302-325`）并常驻等待（`326-329`）。

### 8.2 runOhosApp-Win.bat（321 行，`18` 注明 no debug/LLDB）

与 Mac 版同构，差异：参数解析用 `:parse` 循环（`36-50`）；DevEco 路径探测 `C:\Program Files\Huawei\DevEco Studio` / `D:\…`（`67-76`）；版本校验读 `product-info.json`（PowerShell ConvertFrom-Json，`81-119`）；`hdc.exe` 路径与 PATH 拼装（`121-128`）；平台白名单多一个 `ohosX86_64`（`155-156`）；调用 `gradlew.bat`（`161-165`）；hvigor 输出重定向到临时日志并用 `findstr "BUILD FAILED"` 判败（因为 "hvigorw.js may return exit code 0 even when logging BUILD FAILED"，注释 `185` 与 `187-220`）；安装失败按 errorlevel 走卸载重装（`254-261`）；耗时统计经 PowerShell 取 epoch（`53,170`）。

## 9. ohosAggregate/build.gradle.kts ↔ docs/architecture/ohos-aggregate.md

- `ohosAggregate/build.gradle.kts:1-10` 头注释：Spike II 占位——今天 `:composeApp` 仍持有 `libkn.so` 链接 + publish；成熟后把 OHOS sharedLib/cinterop（resource、avplayer）+ export filter 搬进来，并 `api` 依赖 core/feature。`11-13` **空 plugins**（注释：no targets yet，避免重复 OHOS link 入口）。`15-27` 唯一任务 `ohosAggregateStatus`（group=harmony），doLast 打印三行状态文本，不触碰任何产物。
- 对照 `docs/architecture/ohos-aggregate.md`：目标「Android/iOS 可多模块、OHOS 仍只出一个 libkn.so」（`:3`）；当前真实 include 图（`5-15`，其中 `:core:account` 行提到 accountSettingsMain）；运行时依赖边 `composeApp→account/network→network`（`17-27`，注：该文档早于 27 模块大拆，只列了首批）；发布路径不变（`29-36`）；状态表 `38-47`：抽包 Done、中间 source set Done、**`:ohosAggregate` owns link/publish = Not yet**；目标形态 `48-57`（最终 composeApp 只剩 Android application + iOS framework）；迁移清单 `59-64` 四步（搬 sharedLib/linkerOpts/moduleIncludes/cinterop → publish 指向 aggregate → 保持 `MainArkUIViewController` 符号稳定 → 验证 link+DevEco 加载）；归属 `66-68`（task 4.3）。
- 一致性结论与当前真实状态：
  - 文档与代码互相吻合——aggregate 无任何产物（`ohosAggregate/build.gradle.kts:11-13`），link/publish 仍在 composeApp（`composeApp/build.gradle.kts:55-92,259-300`）。
  - 该文档的 include 图（`docs/architecture/ohos-aggregate.md:5-15`）停留在首批抽包期（只画 network/account），落后于 `settings.gradle.kts:23-44` 现在的 30 工程全图；但其「一个 OHOS 二进制」约束与运行时边 `composeApp→network/account` 仍成立。
  - `docs/architecture/ohos-aggregate.md:46` 明确 OHOS link smoke 未入门禁（Deferred / not gated on 4.3），与 §11.6 CI 只跑 Android 一致。

## 10. composeApp/build.gradle.kts 全文（301 行）——唯一二进制产出方

### 10.1 头部与装配（`1-27`）

- imports（`2-6`）：`NativeBuildType`、`android.databinding.tool.ext.capitalizeUS`（`5`，AGP 内部扩展）；但 `240-243` 又本地定义了同名 `String.capitalizeUS()`，本地函数遮蔽导入扩展，实际生效的是本地版——保留的历史噪声。
- plugins（`8-14`）：kotlinMultiplatform → androidApplication → composeMultiplatform → composeCompiler → kotlinSerialization 五连。
- `-PandroidOnly`（`16-17`）：为 true 时整个 `if (!androidOnly)` 块（iOS/OHOS target、源集层级、publish 任务）都不注册，CI 只留 Android（`.github/workflows/ci.yml` 用该旗标）。
- `ohosNativeSysroot`（`19-27`）：三级回退解析 OHOS native sysroot——`local.properties` 的 `local.ohos.native=` 行 → 环境变量 `OHOS_NATIVE_SYSROOT` → DevEco 默认路径 `/Applications/DevEco-Studio.app/Contents/sdk/default/openharmony/native/sysroot`（存在才用）。供 avplayer cinterop includeDirs（`100-105`）；core/network 有同款逻辑（`core/network/build.gradle.kts:15-23`）。注意本机 `local.properties` 实际只有 `sdk.dir`，未设该键。

### 10.2 Kotlin targets 与产物命名（`29-110`）

- androidTarget：`30-36`，JVM_17（`34` 注释：Koog 需要 JDK 17+）。
- **iOS framework**（`38-48`）：三个 ios target 统一 `binaries.framework { baseName = "ComposeApp"; isStatic = true }`（`45-46`）——产物为静态 `ComposeApp.framework`，由 `iosApp/` Xcode 工程经 `linkDebugFrameworkIosSimulatorArm64` 消费（`deployment.gradle.kts:461`）。
- **OHOS sharedLib**（`51-92`）：`ohosArm64()`（真机）+ `ohosX64()`（模拟器）两 target 各配 `binaries.sharedLib { baseName = "kn" }`（`56`）→ 产物即 `libkn.so` + `libkn_api.h`，落在 `composeApp/build/bin/<target>/{debug,release}Shared/`。要点：
  - `57-60`：release 链接 `optimized = false`，注释说明 DevirtualizationAnalysis 内存爆 OOM（AGENTS.md:129 同口径）。
  - `61`：`export(libs.compose.multiplatform.export)` 把 CMP 导出层静态链入。
  - `62-65` 注释（硬约束）：**禁止 moduleIncludes/outputModule**——那会让 libkn DT_NEEDED 缺 libruntime.so/libstd.so 导致 HAP dlopen 失败；emitRuntime/emitStdlib 在 2.2.21-0.3.0 还丢了 @CName/CAdapter 导出；策略是「自包含单一 libkn.so、KN runtime 内链」。
  - `66`：`linkerOpts("-lz", "-lavplayer")`。
  - `69-71`：仅 `ohosX64` 追加 `-lnet_http`（cinterop 回退线），ohosArm64 用 CPF Ktor CIO 不加。
  - `72-91` 渲染模式：读 `rendererBackend` 属性（`76`，缺省 fusion-renderer）；fusion 时补 11 个 `-l`：native_drawing/image_source/pixelmap/pixelmap_ndk.z/native_window/ace_napi.z/hilog_ndk.z/hitrace_ndk.z/uv/unwind/icu（`78-90`），`73-75` 注释解释这是给旧编译产物 DT_NEEDED 兜底（正确构建时 KGP 的 NativeTasksConfiguration.kt 已写入），统一在 build 脚本补全避免 CMakeLists 硬编码。
- **cinterop**（`93-108`）：main compilation 上 `cinterops.creating` 两个——`resource`（def `src/ohosMain/cinterop/resource.def` + includeDirs `95-96`）与 `avplayer`（def `avplayer.def`，sysroot 的 `usr/include` + `usr/include/multimedia/player_framework`，`98-105`）；`107` 注释：netHttp cinterop 已迁往 `:core:network`。

### 10.3 sourceSets 与依赖逐条（`112-189`）

- `113-115` 注释（关键决策）：能力型依赖全部下沉——CPF uuid→`:core:base`、Coil→`:core:ui`、CameraX/MLKit→`:core:platform`、Vico→`:feature:home`；composeApp 只剩 app/ 装配 + 三端入口，**不再创建任何中间 source set**。
- androidMain（`116-119`）：`libs.androidx.activity.compose`、`libs.androidx.core.splashscreen`。
- commonMain 工程依赖（`120-149`）：`projects.component`（`121`）+ 9 个 core——network/account/router/design/base/analytics/storage/platform/ui（`122-130`）+ 17 个 feature 全量（`133-149`）。`:core:database` 虽在 `settings.gradle.kts:33` 被 include，但 **全仓没有任何 `projects.core.database` 消费方（未找到）**，目前是被 include 未装配的叶子模块。`131-132` 注释：app/ 是唯一允许看全 feature 的装配层，与 check-layer-deps.sh 的「feature 不得互依赖」配套。库依赖：CMP `compose.*` 访问器 6 个（`150-156`）+ coroutines/atomicFu/serialization（`157-159`）。
- 层级重建（`161-181`）：与根接线同构（composeApp 不在根接线范围内）——iosMain→三个 ios*Main；ohosMain→ohosArm64Main/ohosX64Main；且 `ohosMain.dependencies { api(libs.compose.multiplatform.export) }`（`172-174`）。
- 测试（`183-188`）：commonTest 与 androidUnitTest 各加 `libs.kotlin.test`。

### 10.4 android 块与根级 dependencies（`192-224`）

namespace/applicationId `com.example.my_kmp_project`（`193,197`；deployment 的 `androidPackage` 与之对应，`deployment.gradle.kts:48`）；compileSdk/min/target 全取 catalog（`194,198-199`）；versionCode=1/versionName="1.0"（`200-201`）；`buildConfig=false`（`203-205`，省构建产物）；packaging 排除 `/META-INF/{AL2.0,LGPL2.1}`（`206-210`）；release `isMinifyEnabled=false`（`211-215`）；Java 17（`216-219`）；根级 `dependencies { debugImplementation(libs.compose.ui.tooling) }`（`222-224`，Android compose 工具线）。

### 10.5 harmonyAppDir（`228-238`）

`-PharmonyAppPath=<绝对路径>` 覆盖发布输出根（`230-237`），缺省 `rootProject.file("harmonyApp")`（`232-233`）。runscript 的 `-p` 参数最终就是传到这里（§8.1 第 4 步）。

### 10.6 harmonyResourceRoots 与 publish 任务（`246-300`）

- `246-253`：OHOS rawfile 投递清单，当前唯一一行——包名 `my_kmp_project.core.design.generated.resources` ↔ 目录 `core/design/src/commonMain/composeResources`。`248-249` 注释：**「模块目录」和「包名」必须成对维护**（harmonyApp 按包名目录读资源），新 feature 自带资源就往表里加行。
- `259-300`：`if (!androidOnly)` 下 `arrayOf("debug","release").forEach` 注册 4 个 Copy 任务（group=harmony）：
  - `publish{Debug|Release}BinariesToHarmonyApp`（`261-277`）：dependsOn `link{Type}SharedOhosArm64`（`263`）；`into(harmonyAppDir)` + `duplicatesStrategy=INCLUDE`（`264-265`）；拷 3 类——①`build/bin/ohosArm64/{type}Shared/libkn_api.h` → `entry/src/main/cpp/include/arm64-v8a/`（`266-268`）；②`libkn.so` → `entry/libs/arm64-v8a/`（`269-271`）；③每个 resourceRoots 项 → `entry/src/main/resources/rawfile/composeResources/<pkg>/`（`272-276`）。**跑它会发生什么**：触发整条 ohosArm64 编译+链接（首次很慢），产出直接落进 harmonyApp 壳工程，DevEco/`runOhosApp-Mac.sh` 随后 assembleHap 即可。
  - `publish{Type}BinariesToHarmonyAppX64`（`279-298`）：同构，但源目录 `ohosX64/{type}Shared`、目标 `x86_64`（`287-291`）；description（`281-283`）明说当前被工具链阻塞（Konan_cxa_demangle）。与 deployment 不同，这 4 个任务**无 harmonyX64 开关、恒注册**（仅受 androidOnly 控制）；deployment 头部注释里「与 publish*BinariesToHarmonyAppX64 一致」（`deployment.gradle.kts:22`）指的是任务名字对齐，不是注册条件对齐。

### 10.7 三端产物速查（跑什么任务、落到哪）

| 端 | 配置出处 | 代表任务 | 产物路径 |
|----|----------|----------|----------|
| Android | `android { }` `192-220` + AGP | `:composeApp:assembleDebug` | `composeApp/build/outputs/apk/debug/*.apk`（deployAndroid `deployment.gradle.kts:426` 在此找 APK；CI artifact 同源，`.github/workflows/ci.yml` upload 步） |
| iOS | framework `baseName="ComposeApp"` isStatic `44-47` | `:composeApp:linkDebugFrameworkIosSimulatorArm64` | KGP 默认的 `composeApp/build/bin/iosSimulatorArm64/debugFramework/`（本仓脚本未直接引用该路径——deployIos 只 dependsOn 该任务后走 xcodebuild，`deployment.gradle.kts:461-479`；Xcode derivedData 明确落仓库根 `build/ios`，`deployment.gradle.kts:466`） |
| OHOS arm64 | sharedLib `baseName="kn"` `55-92` | `:composeApp:linkDebugSharedOhosArm64` → `publishDebugBinariesToHarmonyApp` | 中间：`composeApp/build/bin/ohosArm64/debugShared/{libkn.so,libkn_api.h}`（verify 检查点 `deployment.gradle.kts:254-259`）；最终投递：`harmonyApp/entry/libs/arm64-v8a/libkn.so` + `entry/src/main/cpp/include/arm64-v8a/libkn_api.h` + `entry/src/main/resources/rawfile/composeResources/my_kmp_project.core.design.generated.resources/`（`composeApp/build.gradle.kts:266-276`） |
| OHOS x64 | 同上 + `-lnet_http` `69-71` | `linkDebugSharedOhosX64`（**当前阻塞**）→ `publish*BinariesToHarmonyAppX64` | `…/ohosX64/{type}Shared/` → `entry/libs/x86_64/` + `cpp/include/x86_64/`（`287-291`） |
| OHOS 测试 exe | KGP native test | `:composeApp:linkDebugTestOhosArm64` | `composeApp/build/bin/ohosArm64/debugTest/test.kexe`（verifyHarmonyKexe `deployment.gradle.kts:154-157`） |

## 11. 交互关系（求值顺序与触发链）

1. **求值顺序**：`gradle-wrapper.properties` 决定 Gradle 9.4.1 运行时 → `settings.gradle.kts`（镜像 + include，`2-44`）→ 根 `build.gradle.kts` plugins apply false（`4-9`，把 KGP/AGP 放上 scripts classpath）→ `apply(from="deployment.gradle.kts")`（`20`）→ 根 `subprojects{}` 注册回调（`37`）→ 各子工程求值：`plugins.withId("com.android.library")` 触发共享接线（`70`），时序约束见 `64-69` → composeApp 自身求值（target/publish 任务）。
2. **catalog → 一切**：版本只从 `libs.versions.toml` 出发；根接线用 `findVersion/findLibrary` 动态取（`60-62,73-75,110-115`），其余模块用 `libs.*` 访问器（依赖 `settings.gradle.kts:2` 的 feature preview）。
3. **产物链（OHOS）**：`:composeApp:linkDebugSharedOhosArm64` →（publish Copy）→ `harmonyApp/entry/libs/arm64-v8a/libkn.so` + cpp/include + rawfile 资源 → hvigor assembleHap → `entry-default-signed.hap` → `hdc bm install` → `aa start`；`scripts/run-harmony.sh → runscript/runOhosApp-Mac.sh → ./gradlew :composeApp:publishDebugBinariesToHarmonyApp` 是同一条链的自动化（§8.1）。deployment 的 `deployHarmony`/`verifyHarmonyHapIntegration` 是同链的 Gradle 侧入口（`deployment.gradle.kts:275,554`）。
4. **产物链（iOS）**：`linkDebugFrameworkIosSimulatorArm64`（framework baseName ComposeApp，`composeApp/build.gradle.kts:44-46`）→ `iosApp` Xcode 工程（deployIos 的 xcodebuild，`deployment.gradle.kts:470-479`）→ simctl install/launch。
5. **开关传播**：`-PandroidOnly` 被多处独立读取且必须保持一致——根 `build.gradle.kts:57`、composeApp `:17`、component `:17`、deployment `:59`（core/network、core/storage、core/ui、core/base、feature/home 的模块脚本也各自读取以裁剪中间 source set）。`rendererBackend` 被 composeApp `:76` 读取；`-PharmonyAppPath` 被 composeApp `:230` 读取；`-PharmonyX64`/`-Ptarget` 被 deployment 读取（`:61/:135,174-179,416-421,482-484`）。
6. **CI**：`.github/workflows/ci.yml:79-80` 用 `-PandroidOnly=true --no-daemon` 只跑 `testDebugUnitTest :composeApp:assembleDebug`；静态门禁 `scripts/verify-parity.sh --static-only`（ci.yml `71-72` 注释提到分层检查）。

## 12. 真实约束与坑（全部有文件出处）

1. **buildSrc 不可用**：`~/.gradle/init.gradle` 把 plugin marker 解析钉在阿里云+eazytec，`org.gradle.kotlin.kotlin-dsl` 解析不到，`:help` 实测失败（根 `build.gradle.kts:25-28`；AGENTS.md 同述）。因此共享接线用根工程类型化 API，而非约定插件。
2. **仓库顺序 = 正确性**：库模块 repositories 里 eazytec 必须排第一且排除 `com.android.tools.build`，两个源各有对方没有的东西；Gradle 把传输错误（阿里云对 OHOS fork 坐标 socket/SSL 报错）判成整次解析失败、**不会尝试下一个仓库**（`build.gradle.kts:40-55`）。
3. **接线时机三选一的坑**：`androidTarget()` 必须在 AGP apply 之后、且必须早于 AGP variant 计算——所以只能挂 `plugins.withId("com.android.library")`；挂 kotlin.multiplatform 或 afterEvaluate 都有具体报错文案（`build.gradle.kts:64-69`）。这要求模块插件块顺序固定「kotlinMultiplatform 前、androidLibrary 后」。
4. **Gradle 9 API 迁移**：`Project.exec`/脚本级 exec 在 9.0 移除，wrapper 已 9.4.1（`gradle-wrapper.properties:3`），deployment 29 处改 `ExecOperations` 注入（`build.gradle.kts:14-17`、`deployment.gradle.kts:33-45`）。
5. **configuration-cache 冲突**：`gradle.properties:8` 开 cache，但 deployment 任务 doLast 直接起进程不兼容（`build.gradle.kts:18-19`）——门禁从不跑它们，日常三端跑用 `scripts/run-*.sh`。
6. **OHOS libkn 链接红线**：禁用 moduleIncludes/outputModule（DT_NEEDED 缺 libruntime/libstd → HAP dlopen 失败；emitRuntime/emitStdlib 丢 @CName/CAdapter 导出）（`composeApp/build.gradle.kts:62-65`）；release `optimized=false` 防 DevirtualizationAnalysis OOM（`57-60`）；`rendererBackend` 只可 fusion/skia 二选一（`76-77`、`gradle.properties:22-23`）。
7. **ohosX64 全线阻塞**：`linkDebugSharedOhosX64` 卡 `Konan_cxa_demangle`（`deployment.gradle.kts:21-22`、publish X64 description `composeApp/build.gradle.kts:281-283`）；X64 verify/deploy 追加需 `-PharmonyX64=true`；但 publish X64 任务本身恒注册（仅 androidOnly 才消失）。
8. **hvigorw 假成功**：hvigorw.js 可能 exit 0 但日志含 BUILD FAILED，Win.bat 用 findstr 判败（`runscript/runOhosApp-Win.bat:185,191,206,212`）；Mac 版靠 `set -e` + 签名 HAP 缺失检查（`runOhosApp-Mac.sh:19,213-231`）。
9. **资源投递成对约束**：harmonyResourceRoots 的「包名↔目录」必须成对改（`composeApp/build.gradle.kts:246-253`），包名规则 `<rootProject>.<模块名>.generated.resources` 由 rootProject.name + 模块路径推导（`settings.gradle.kts:1`）。
10. **任务命名限制**：publish 任务名由 `link{Type}Shared{Target}` 反推（`capitalizeUS` 拼 `link${Type}SharedOhosArm64`，`composeApp/build.gradle.kts:261-263`），改 sharedLib baseName 会同时牵动 deployment 校验路径（`deployment.gradle.kts:258-259,282-283`）与 AGENTS.md 的 publish 契约。
11. **AGENTS.md 口径 vs 代码**：AGENTS.md `48` 说「buildSrc does not work」与代码一致；`gradle.properties:24` 的 `ohosSkikoVersion` 在全部 build 脚本中**未找到消费方**；catalog 中 8 个 alias（testJunit/junit/testExt/espresso/appcompat/collection/constraintlayout/material）与 `cocoapods` 插件 alias **未被任何模块引用**；`:core:database` 被 include 但 `projects.core.database` **未找到任何消费方**。
12. **本机 local.properties**：当前仅有 `sdk.dir`（未提交入 git 的机器配置，AGENTS.md guardrail），`local.ohos.native` 未设置 → OHOS sysroot 走环境变量或 DevEco 默认回退（`composeApp/build.gradle.kts:19-27`）。
13. **X64 注册口径不一致**：deployment 头部说 X64 任务默认不注册需 `-PharmonyX64=true`「与 publish*BinariesToHarmonyAppX64 一致」（`deployment.gradle.kts:21-22`），但 composeApp 的 4 个 publish Copy 任务实际**恒注册**、只受 androidOnly 控制（`composeApp/build.gradle.kts:259-300`）——「一致」指任务名，不是注册条件；跑 `publishDebugBinariesToHarmonyAppX64` 不会被开关拦，只是会被工具链在 link 阶段卡住（`281-283` description）。
14. **Mac/Win 脚本能力差**：Mac 版 PLATFORM 白名单是 `ohosArm64` + `iosSimulatorArm64`（`runOhosApp-Mac.sh:168-178`），Win 版是 `ohosArm64` + `ohosX86_64`（`runOhosApp-Win.bat:155-156`）；Win 版头注释自称 "no debug / LLDB"（`runOhosApp-Win.bat:18`），虽然 `aa start` 仍会按 DEBUG_MODE 加 `-D`（`270-276`）；安装失败判定方式不同——Mac grep 输出含 `failed`（`runOhosApp-Mac.sh:250`），Win 看 errorlevel（`runOhosApp-Win.bat:254-255`）；hvigor 假成功（exit 0 但日志 BUILD FAILED）只有 Win 版做了日志兜底（`runOhosApp-Win.bat:185,191,206,212`），Mac 版只靠 `set -e` 与签名 HAP 存在性检查（`runOhosApp-Mac.sh:19,213-231`）。
15. **deployment 常量是硬编码副本**：`androidPackage/harmonyPackage/iosBundleId` 等（`deployment.gradle.kts:47-55`）与各真源（composeApp android 块、app.json5、Config.xcconfig）无编译期联动，改包名必须双改，否则 deploy 任务 force-stop/launch 打错目标。

## 13. 改动入口（Recipe）

| 想做的事 | 动哪里 |
|----------|--------|
| 新增一个 `:feature:x` 模块 | ① `settings.gradle.kts:38-43` 的 listOf 加一项；② 新建 `feature/x/build.gradle.kts`：plugins 顺序 kotlinMultiplatform→androidLibrary（依赖根接线），加 `android { namespace }` + `api(projects.core.*)`（AGENTS.md:51-52、`build.gradle.kts:31-33` 边界）；③ 若要进 app，装配点在 `composeApp/build.gradle.kts:133-149` 加 `implementation(projects.feature.x)`；带资源再登记 `composeApp/build.gradle.kts:250-253`。 |
| 新增一个 `:core:x` | `settings.gradle.kts:23-34` 加 include，其余同上；能力型中间 source set 留给模块自己（`build.gradle.kts:32-33` 注释）。 |
| 换渲染器 fusion→skia | `gradle.properties:22` 改 `rendererBackend=skia`（或删行，缺省即 fusion——`composeApp/build.gradle.kts:76` 的 `?:` 默认值），fusion 专属 11 个 `-l` 自动不加（`77-91`）；改完必须重新 publish 再跑 DevEco（AGENTS.md OHOS publish 契约）。 |
| 改 OHOS 产物名 libkn.so→libfoo.so | `composeApp/build.gradle.kts:56` baseName；同步 `deployment.gradle.kts:258-259,282-283` 文件名、`composeApp/build.gradle.kts:266-271,287-291` 拷贝源文件名、harmonyApp 侧 dlopen/CMakeLists 消费者（AGENTS.md:127 要求壳层同步）。 |
| 改 iOS framework 名 | `composeApp/build.gradle.kts:45` baseName，另查 `iosApp/` Xcode 工程与 Podfile 化引用（本仓不用 cocoapods 插件，catalog `140` 的 alias 无人 apply）。 |
| 让某库模块也产二进制 | **不建议**：违反「只有 composeApp 产二进制」约束（`build.gradle.kts:95-96`）；正路是推进 `:ohosAggregate` 迁移清单（`docs/architecture/ohos-aggregate.md:59-64`）。 |
| 新依赖 | 先加 `gradle/libs.versions.toml`（versions+libraries 成对），再在正确模块/正确 source set 引用（AGENTS.md Conventions:144）；OHOS 兼容性先看是否 CPF 线（§6.1）。 |
| 换 Gradle 版本 | `gradle/wrapper/gradle-wrapper.properties:3`（注意 9.x 的 exec 移除红线，§12.4）。 |
| 换 Maven 镜像 | 三处要一起看：`settings.gradle.kts:7,17`、根 `build.gradle.kts:47-55`（库模块级，含 excludeGroup 技巧）、`~/.gradle/init.gradle`（仓外）。 |
| 外部 OHOS 壳工程调试 | 不改代码：`./gradlew :composeApp:publishDebugBinariesToHarmonyApp -PharmonyAppPath=/abs/path`（`composeApp/build.gradle.kts:229-238`），或 `scripts/run-harmony.sh -p /abs/path`（`runOhosApp-Mac.sh:157-160`）。 |
| 指定设备/模拟器跑一次 | 全部走 deployment：`./gradlew deployAndroid -Ptarget=<serial>`、`deployIos -Ptarget=<udid>`、`deployHarmony -Ptarget=<hdc-id>`（`deployment.gradle.kts:135-138,416-421,482-484`）；无设备任务不失败、只提示。 |
| 换鸿蒙 bundle/ability 名 | 真源在 `harmonyApp/AppScope/app.json5` 与 `entry/src/main/module.json5`；deployment 常量 `deployment.gradle.kts:50-51` 与 runscript 默认值 `runOhosApp-Mac.sh:27-28`、`runOhosApp-Win.bat:25-26` 要同步改，或用 `-b/-a` 临时覆盖（Mac `66-67`）。 |
| 把 OHOS link 迁到 :ohosAggregate | 按 `docs/architecture/ohos-aggregate.md:59-64` 四步：搬 `sharedLib{baseName="kn"}`+linkerOpts+cinterop（即 `composeApp/build.gradle.kts:51-108`）→ publish Copy 源目录改指 aggregate 产物（`composeApp/build.gradle.kts:261-298` 或搬家后的等价物）→ 保持 `MainArkUIViewController` 符号 → 验证 `linkDebugSharedOhosArm64` + DevEco 加载；同时给空 `plugins{}`（`ohosAggregate/build.gradle.kts:11-13`）补 KMP 声明。 |
| feature 自带 composeResources | 在 `composeApp/build.gradle.kts:250-253` 的 `harmonyResourceRoots` 加一对「`my_kmp_project.feature.<名>.generated.resources` ↔ `feature/<名>/src/commonMain/composeResources`」，随后重新 publish（`246-249` 注释明示成对维护）。 |
| 只想快速验证 OHOS 产物 | `./gradlew verifyHarmonySharedLib`（link+查 libkn.so/libkn_api.h，`deployment.gradle.kts:245-270`）或 `verifyHarmonyHapIntegration`（连 publish 带投递一起验，`272-292`），都不需要真机。 |

## 14. 本工程自定义 Gradle 任务速查索引

| 任务（注册工程） | 定义行 | group | 开关影响 |
|------------------|--------|-------|----------|
| `:composeApp:publishDebugBinariesToHarmonyApp` | `composeApp/build.gradle.kts:261-277`（循环内） | harmony | 受 `-PandroidOnly`（`259`）；输出根受 `-PharmonyAppPath`（`229-238`） |
| `:composeApp:publishReleaseBinariesToHarmonyApp` | 同上 `type=release` | harmony | 同上 |
| `:composeApp:publishDebug/ReleaseBinariesToHarmonyAppX64` | `279-298` | harmony | 恒注册（仅 androidOnly 消失）；实际 link 被工具链阻塞（`281-283`） |
| `:ohosAggregate:ohosAggregateStatus` | `ohosAggregate/build.gradle.kts:15-27` | harmony | 无开关，纯打印 |
| `verifyHarmonyKexe` / `verifyHarmonySharedLib` / `verifyHarmonyHapIntegration` | `deployment.gradle.kts:151 / 245 / 272` | verification | 无；`-Ptarget` 选设备 |
| `verifyHarmonyKexeOhosX64` / `…SharedLibOhosX64` / `…HapIntegrationOhosX64` | `299 / 357 / 375` | verification | 仅 `-PharmonyX64=true` 注册（`298`） |
| `deployAndroid` | `395` | deployment | 恒注册 |
| `deployIos` | `456` | deployment | 仅 macOS（`455` `isMac()`） |
| `deployHarmony` | `549` | deployment | `-PandroidOnly=true` 时不注册（`548`）；`-PharmonyX64=true` 追加 X64 依赖（`555-557`） |

（KMP 标准任务如 `assembleDebug`、`linkDebugSharedOhosArm64`、`linkDebugTestOhosArm64`、`linkDebugFrameworkIosSimulatorArm64` 由插件自动生成，本工程脚本只引用不定义。）

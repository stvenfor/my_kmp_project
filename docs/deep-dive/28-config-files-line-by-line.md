# 28 主要配置文件逐行解释（非构建脚本类）

## 0. 本文口径与文件清单

本文只讲「配置文件里每一段管什么」，不讲 Gradle 构建脚本本身的结构（那是 `docs/deep-dive/09-build-and-config.md` 的口径），也不讲三端壳里的代码逻辑（那是 `docs/deep-dive/23-native-shells.md` 的口径）。所有行号均为 2026-10 在当前工作树实测；下表「实测行数」用 `wc -l` 口径（末尾无换行符的文件按实际行计）。

| 文件 | 实测行数 | 归属端 |
|------|----------|--------|
| `gradle.properties` | 24 | Gradle 全局 |
| `gradle/wrapper/gradle-wrapper.properties` | 7 | Gradle 全局 |
| `harmonyApp/build-profile.json5` | 57 | HarmonyOS 工程级 |
| `harmonyApp/entry/build-profile.json5` | 56 | HarmonyOS 模块级 |
| `harmonyApp/hvigorfile.ts` | 6 | HarmonyOS 工程级 |
| `harmonyApp/entry/hvigorfile.ts` | 6 | HarmonyOS 模块级 |
| `harmonyApp/entry/src/main/cpp/types/libentry/oh-package.json5` | 6 | HarmonyOS NAPI 类型包 |
| `harmonyApp/entry/src/main/resources/base/element/color.json` | 8 | HarmonyOS 资源 |
| `harmonyApp/entry/src/main/resources/base/element/float.json` | 8 | HarmonyOS 资源 |
| `harmonyApp/entry/src/main/resources/base/element/string.json` | 23 | HarmonyOS 资源 |
| `harmonyApp/entry/src/main/resources/base/profile/backup_config.json` | 3 | HarmonyOS 资源 |
| `iosApp/iosApp.xcodeproj/project.pbxproj` | 374 | iOS |
| `.github/workflows/ci.yml` | 137 | CI |
| `openspec/config.yaml` | 42 | Agent 工作流 |
| `.gitignore` | 146 | 仓库卫生 |
| `AGENTS.md` | 220 | Agent 约束 |

**安全红线口径**：`harmonyApp/build-profile.json5` 的签名物料、`project.pbxproj` 的 team id、上游 Flutter SoT 仓库里已提交的第三方凭据（慧眼 SDK 的 app id 与 licence blob 一类字段），本文**只写字段名与行号，不复制任何真实值**；这些值也不出现在本文任何代码片段里。

## 1. gradle.properties —— 逐键解释

全文 24 行，按注释分为 Kotlin（`:2-4`）、Gradle（`:6-9`）、Android（`:11-14`）、K/N 与网络（`:16-19`）、渲染模式（`:21-24`）五段。

### 1.1 逐键一览

| 键 | 行号 | 管什么 |
|----|------|--------|
| `kotlin.code.style=official` | `gradle.properties:3` | Kotlin 官方代码风格（IDE/格式化约定），不影响编译产物 |
| `kotlin.daemon.jvmargs=-Xmx3072M` | `gradle.properties:4` | Kotlin 编译守护进程堆上限；KMP 多 target 并发编译时防止 K2/CLI 编译 OOM |
| `org.gradle.jvmargs=-Xmx4096M -Dfile.encoding=UTF-8` | `gradle.properties:7` | Gradle daemon 堆上限与文件编码；本仓含中文注释与 OHOS 链接期大依赖图，4G 是实测下限 |
| `org.gradle.configuration-cache=true` | `gradle.properties:8` | 开启配置缓存，第二次起跳过 configuration 阶段；要求任务配置期不读外部状态 |
| `org.gradle.caching=true` | `gradle.properties:9` | 本地 build cache，跨任务复用输出（CI 里没有远端 cache，仅本机受益） |
| `android.nonTransitiveRClass=true` | `gradle.properties:12` | 每个模块只生成自己的 R 类，资源引用必须带模块包名，杜绝 `R.drawable` 串模块 |
| `android.useAndroidX=true` | `gradle.properties:13` | 允许 AndroidX 坐标（AGP 8 下缺失会直接报错），全仓 androidx 依赖的前提 |
| `android.suppressUnsupportedCompileSdk=36` | `gradle.properties:14` | 压掉「compileSdk 36 超出当前 AGP 已知版本」的告警；36 来自 catalog（`gradle/libs.versions.toml:27` 同段附近），`composeApp/build.gradle.kts:194` 只是透传 |
| `kotlin.native.cacheKind=none` | `gradle.properties:16` | 关闭 K/N 预编译缓存（iOS 侧不做 program 缓存），换取链接行为可预期，规避 CPF 分发表下缓存 klib 缺失问题 |
| `org.gradle.internal.http.connectionTimeout=100000` | `gradle.properties:18` | Gradle 下载依赖/分发的连接超时（100 s），面向内网镜像慢链路 |
| `org.gradle.internal.http.socketTimeout=100000` | `gradle.properties:19` | 同上，socket 读超时；两者共同兜住 `settings.gradle.kts:7`、`settings.gradle.kts:17` 的 eazytec 镜像 |
| `rendererBackend=fusion-renderer` | `gradle.properties:22` | 自定义属性，OHOS 渲染后端选择器（详见 1.2） |
| `#rendererBackend=skia` | `gradle.properties:23` | 被注释掉的备选值，说明切换方式是「注掉一行、放开一行」 |
| `ohosSkikoVersion=0.9.22.2-0.3.0` | `gradle.properties:24` | 遗留开关：全仓 `*.kts` grep 无任何消费方（结论出处 `docs/deep-dive/09-build-and-config.md:141`），改它不会连锁任何东西，但删除前建议先确认无脚本按属性名读取 |

### 1.2 `rendererBackend=fusion-renderer` 为什么在

这是本仓自定义的「属性 → 链接器参数」开关，消费点只有一个：`composeApp/build.gradle.kts:76` 用 `rootProject.findProperty("rendererBackend")` 读它，缺省回退值也是 `fusion-renderer`；`:77` 判断相等后，`:78-90` 给 `ohosArm64` 与 `ohosX64` 的 sharedLib 追加一整串 `-l` 链接选项——`-lnative_drawing`、`-limage_source`、`-lpixelmap`（`composeApp/build.gradle.kts:79-81`）、`-lnative_window`、`-lace_napi.z`、`-lhilog_ndk.z`、`-luv`、`-lunwind`、`-licu` 等（`composeApp/build.gradle.kts:83-89`）。背景注释在 `composeApp/build.gradle.kts:72-75`：fusion-renderer 下 `libkn.so` 的 `DT_NEEDED` 必须带上这些 OHOS NDK 库，否则 HAP 里 dlopen 失败。

**改动连锁**：把 `gradle.properties:22` 改成 `skia`（即只留 `:23` 生效）后，`composeApp/build.gradle.kts:78-90` 整段不再追加，重新链接出的 `libkn.so` 依赖表立刻变化——所以它牵动的不是一行脚本而是三件事：`:composeApp:linkDebugSharedOhosArm64` 的产物、`publish*BinariesToHarmonyApp` 投递的 `.so`（`composeApp/build.gradle.kts:259-300`）、以及 DevEco 侧装机能否启动。`AGENTS.md:118` 把这条约定上升为规则：「linker opts 跟着这个 flag 走，只在有意切换渲染器时改」。

### 1.3 其他键的连锁影响面

- `gradle.properties:7` 的 daemon JVM 参数影响一切 Gradle 调用，包括 Xcode 构建阶段里由脚本回调的 `./gradlew`（`iosApp/iosApp.xcodeproj/project.pbxproj:155`）；把 `-Xmx` 调小最先炸的往往是 K/N 链接而非 Android 编译。
- `gradle.properties:8` 配置缓存与本仓根脚本的 `subprojects {}` 全局接线（`build.gradle.kts:1-9` 的 apply false 去重模式）共存；若某任务在配置期读机器本地文件（如 `composeApp/build.gradle.kts:20-27` 读 `local.properties`），行为以配置缓存快照为准，换路径后可能要 `--no-configuration-cache` 跑一次确认。
- `gradle.properties:12` 的 nonTransitiveRClass 与资源投递契约无关，但 `AGENTS.md:201` 要求 commonMain 用 `Res.drawable` 而非 Android `R`，两者共同保证「资源引用可追到模块」。
- `gradle.properties:16` 只作用于 K/N（iOS、OHOS 两个后端的宿主），改它不动 Android；验证口径是最窄任务 `./gradlew :composeApp:compileDebugKotlinAndroid` 通过不代表它生效，需跑 iOS 链接。

### 1.4 全 24 行逐行覆盖（补齐 1.1 未列的行）

| 行 | 内容 | 说明 |
|----|------|------|
| `:1` | 空行 | 无键 |
| `:2` | `#Kotlin` | 段注释 |
| `:3` | `kotlin.code.style=official` | 见 1.1 |
| `:4` | `kotlin.daemon.jvmargs=-Xmx3072M` | 见 1.1 |
| `:5` | 空行 | 分段 |
| `:6` | `#Gradle` | 段注释 |
| `:7` | `org.gradle.jvmargs=…` | 见 1.1、1.3 |
| `:8` | `org.gradle.configuration-cache=true` | 见 1.3 |
| `:9` | `org.gradle.caching=true` | 本地 build cache |
| `:10` | 空行 | 分段 |
| `:11` | `#Android` | 段注释 |
| `:12` | `android.nonTransitiveRClass=true` | 见 1.3 |
| `:13` | `android.useAndroidX=true` | AndroidX 前提 |
| `:14` | `android.suppressUnsupportedCompileSdk=36` | 压 compileSdk 36 告警 |
| `:15` | 空行 | 分段 |
| `:16` | `kotlin.native.cacheKind=none` | K/N 缓存关闭，见 1.3 |
| `:17` | 空行 | 分段 |
| `:18` | `org.gradle.internal.http.connectionTimeout=100000` | 镜像下载连接超时 |
| `:19` | `org.gradle.internal.http.socketTimeout=100000` | 镜像读取超时 |
| `:20` | 空行 | 分段 |
| `:21` | `# 渲染模式` | 中文段注释，标记 OHOS 开关 |
| `:22` | `rendererBackend=fusion-renderer` | 见 1.2 |
| `:23` | `#rendererBackend=skia` | 备选值注释行 |
| `:24` | `ohosSkikoVersion=0.9.22.2-0.3.0` | 无消费方的遗留键，见 1.1 |

## 2. gradle/wrapper/gradle-wrapper.properties —— 分发钉死

全文 7 行：

| 键 | 行号 | 管什么 |
|----|------|--------|
| `distributionBase=GRADLE_USER_HOME` | `gradle/wrapper/gradle-wrapper.properties:1` | 分发解包根目录选用户级 Gradle home |
| `distributionPath=wrapper/dists` | `gradle/wrapper/gradle-wrapper.properties:2` | 子路径；两者合起来即 `~/.gradle/wrapper/dists` |
| `distributionUrl=https\://mirrors.tencent.com/gradle/gradle-9.4.1-bin.zip` | `gradle/wrapper/gradle-wrapper.properties:3` | **版本与镜像双重钉死**：Gradle 9.4.1（本机已升级到此版），且不走 services.gradle.org 而走腾讯镜像 |
| `networkTimeout=120000` | `gradle/wrapper/gradle-wrapper.properties:4` | wrapper 下载分发的网络超时（2 min） |
| `validateDistributionUrl=true` | `gradle/wrapper/gradle-wrapper.properties:5` | 下载前校验 URL 合法性，镜像改版时快速失败而不是拿坏包 |
| `zipStoreBase` / `zipStorePath` | `gradle/wrapper/gradle-wrapper.properties:6-7` | 原始 zip 的存放位置（与解包目录分离，可复用） |

为什么必须 `./gradlew`：`AGENTS.md:102` 明确「永远用项目 wrapper，不用系统 Gradle」。原因有三层。其一，本仓 Kotlin 插件是 CPF 分发线（`gradle/libs.versions.toml:27` 的 `2.2.21-0.3.0`），对 Gradle 版本敏感，系统 Gradle 哪怕只差一个小版本也可能触发插件 marker 解析失败；其二，构建环境本身有坑——`AGENTS.md:49-52` 记载 buildSrc 在「`~/.gradle/init.gradle` 把 plugin-marker 解析钉到镜像」的环境下不可用，新人用系统 Gradle 更容易踩到；其三，wrapper 的 `distributionUrl`（`:3`）与 `settings.gradle.kts:7`、`settings.gradle.kts:17` 的镜像仓库是配套的：分发走腾讯镜像、依赖走 eazytec 镜像，两者都为了内网可达。CI 里 `gradle/actions/setup-gradle@v4`（`.github/workflows/ci.yml:69-70`）同样尊重 wrapper 版本，不额外指定 distribution。

另一处值得注意的「缺失」：本文件没有 `distributionSha256Sum` 校验行，`validateDistributionUrl=true`（`:5`）只做 URL 合法性检查、不做内容哈希——升级 wrapper 版本（如本机升到 9.4.1）时若想严格验包，需要自行补该键。

## 3. HarmonyOS 侧配置逐段

Harmony 壳里 Gradle 与 hvigor 各管一半：Gradle 产出 `libkn.so` 与资源（`composeApp/build.gradle.kts:259-300` 的 publish 任务拷进 `harmonyApp/entry/`），hvigor 负责把 ArkTS、C++、资源打成 HAP 并签名。下面五个文件全在 hvigor 这一侧。

### 3.1 harmonyApp/build-profile.json5（工程级，57 行）

三段结构：`app.signingConfigs`、`app.products`、`modules`。

- **signingConfigs（`:3-17`）**：一条名为 `default` 的 HarmonyOS 签名配置（`:5-6`）。`material` 块（`:7-15`）里存放的是敏感物料：`certpath`（`:8`）、`keyAlias`（`:9`）、`keyPassword`（`:10`）、`profile`（`:11`）、`signAlg`（`:12`）、`storeFile`（`:13`）、`storePassword`（`:14`）——**此处存放证书路径与密钥口令等敏感值，本文一律不复制**。这些值是 DevEco 自动签名流程写进文件的，路径指向本机 `~/.ohos/config` 下的物料，换机器即失效；它没被 `.gitignore` 覆盖，属于「仓库里已存在的本机签名引用」，改动签名前先确认这套物料在你机器上存在。
- **products（`:18-33`）**：产品 `default`（`:20`）绑定上面的签名配置（`:21 signingConfig`），`targetSdkVersion` 与 `compatibleSdkVersion` 都是 `6.0.0(20)`（`:22-23`），`runtimeOS: HarmonyOS`（`:24`）。`buildOption`（`:25-31`）里 `nativeCompiler: BiSheng`（`:26`）指定用 BiSheng clang 编 C++（影响 `entry/src/main/cpp/CMakeLists.txt` 的产物兼容性）；`strictMode.caseSensitiveCheck`（`:28`）开启大小写敏感校验、`useNormalizedOHMUrl`（`:29`）启用规范化模块 URL——后者是加载 `oh_modules` 里 CPF compose 包（`compose::skikobridge`，见 `harmonyApp/entry/src/main/cpp/CMakeLists.txt:30-32`）的前提。
- **buildModeSet（`:34-41`）**：声明工程级构建模式 `debug`（`:36`）与 `release`（`:39`），供 entry 的 `buildOptionSet` 按名字覆盖。
- **modules（`:43-56`）**：把唯一模块 `entry`（`:45`）映射到 `./entry`（`:46`），其 target `default` 应用于 product `default`（`:47-53`）。新增模块必须同时动 `settings.gradle.kts`（Gradle 侧）和这里（hvigor 侧）。

### 3.2 harmonyApp/entry/build-profile.json5（模块级，56 行）

管「这个 HAP 模块怎么编」，与工程级的「产品与签名」正交。

- `apiType: stageMode`（`harmonyApp/entry/build-profile.json5:2`）：Stage 模型（本仓唯一支持形态）。
- `resOptions.copyCodeResource.enable=false`（`:4-8`）：不拷贝代码资源进 HAP，Compose 资源走 rawfile 投递而不是这条通道。
- `externalNativeOptions`（`:9-17`）：`path` 指向 `./src/main/cpp/CMakeLists.txt`（`:10`）；`:12-13` 的注释记录了关键事实——**`linkDebugSharedOhosX64` 当前因 `Konan_cxa_demangle` 链接失败，所以 `abiFilters` 只留 `arm64-v8a`（`:14-16`）**，x86_64 模拟器暂不可用，需真机或 ARM 模拟器。
- `nativeLib.filter.excludes`（`:19-25`）：剔除 `**/x86_64/*.so` 通配的 HAR 自带桩（`:22`），`:18` 注释说明原因：这些桩里没有 `libentry`、`libkn` 符号，装上会在运行期报 named-export SyntaxError。
- `buildOptionSet` release 段（`:27-47`）：ArkTS 混淆 `enable: false`（`:33`），规则文件 `./obfuscation-rules.txt` 已挂上但没启用（`:34-36`）；`nativeLib.debugSymbol.strip: true`（`:40-44`）在 release 剥 native 符号。
- `targets`（`:48-55`）：`default`（`:50`）与 `ohosTest`（`:53`）两个模块 target。

### 3.3 hvigorfile.ts 两份的插件差异

| 文件 | 行号 | system 插件 | 含义 |
|------|------|-------------|------|
| `harmonyApp/hvigorfile.ts` | `1`、`4` | `appTasks` | 工程级任务集：聚合构建、产 APP/Bundle，负责产品与签名维度 |
| `harmonyApp/entry/hvigorfile.ts` | `1`、`4` | `hapTasks` | 模块级任务集：编译 ArkTS、跑 CMake、装 `entry` HAP |

两份的 `plugins: []` 都是空的（`harmonyApp/hvigorfile.ts:5`、`harmonyApp/entry/hvigorfile.ts:5`），即未挂自定义 hvigor 插件；`:4-5` 的注释标明 system 字段「内置不可改」。差异一句话：**appTasks 管「整包」，hapTasks 管「模块」**；写错位置（如在 entry 里引 appTasks）hvigor 会直接找不到任务。

### 3.4 cpp/types/libentry/oh-package.json5 —— native 模块的「名片」

全文 6 行：`name: "libentry.so"`（`harmonyApp/entry/src/main/cpp/types/libentry/oh-package.json5:2`）、`types: "./Index.d.ts"`（`:3`）、`version`、`description`（`:4-5`）。它不是 npm 包声明，而是把 C++ 产物 `libentry.so` 注册成一个可被 ArkTS `import` 的 ohpm 包。接线链条：

1. 模块 `oh-package.json5` 以文件依赖认领它：`"libentry.so": "file:./src/main/cpp/types/libentry"`（`harmonyApp/entry/oh-package.json5:9`），同段还声明 CPF compose 包（`harmonyApp/entry/oh-package.json5:10`）。
2. `Index.d.ts` 是给 ArkTS 的类型面，导出 `MainArkUIViewController`（`harmonyApp/entry/src/main/cpp/types/libentry/Index.d.ts:3`）与 `SetOhosHost`、`ApplyAuthSession`、一组 `Auth*` 桥接函数（`harmonyApp/entry/src/main/cpp/types/libentry/Index.d.ts:4-20`）。
3. 真正的 `.so` 由 CMake 产出：`add_library(entry SHARED napi_init.cpp)`（`harmonyApp/entry/src/main/cpp/CMakeLists.txt:37`），并 IMPORT 链接 Gradle 发布的 `libkn.so`（`harmonyApp/entry/src/main/cpp/CMakeLists.txt:19`、`:34-35`），缺文件时 FATAL_ERROR 直接提示跑 publish 任务（`harmonyApp/entry/src/main/cpp/CMakeLists.txt:20-23`）。`AGENTS.md:127` 要求 `MainArkUIViewController` 这个符号两侧（d.ts 与 Kotlin cinterop 导出）保持稳定。

注意 `libkn.so`、`libkn_api.h` 本身**不是**在这份 json5 里声明的——它们由 publish 任务落进 `entry/libs/<abi>/` 与 `entry/src/main/cpp/include/<abi>/`（`composeApp/build.gradle.kts:266-271`），并被 `.gitignore:86-90` 整组忽略、每次重新生成。

### 3.5 资源四件套：color.json、float.json、string.json、backup_config.json

- `color.json`（8 行）：只有一条 `start_window_background = #F7FAFC`（`harmonyApp/entry/src/main/resources/base/element/color.json:4-5`），被 ability 的 `startWindowBackground` 引用（`harmonyApp/entry/src/main/module.json5:46`）。`#F7FAFC` 与三端启动底色对齐（`AGENTS.md:95` 记录 Harmony 侧口径：亮暗同值）。
- `float.json`（8 行）：一条 `page_text_font_size = 50fp`（`harmonyApp/entry/src/main/resources/base/element/float.json:4-5`）。全仓 grep 显示除自身外没有任何引用点——DevEco 模板遗留物，删掉不影响构建，留着也不生效。
- `string.json`（23 行）：五条文案。`module_desc`（`:4`）被 `harmonyApp/entry/src/main/module.json5:5` 引用为模块描述；`EntryAbility_desc`（`:8`）、`EntryAbility_label`（`:12`，值就是桌面显示名「KMP Harmony」，`:13`）被 `harmonyApp/entry/src/main/module.json5:42`、`harmonyApp/entry/src/main/module.json5:44` 引用；`vibrate_permission_reason`（`:16`）与 `network_info_permission_reason`（`:20`）是两条权限申请理由，分别被 `harmonyApp/entry/src/main/module.json5:29`、`harmonyApp/entry/src/main/module.json5:19` 的 `reason` 字段消费——**上架审核会核对权限与理由成对存在，这两条不能随手删**。
- `backup_config.json`（3 行）：`allowToBackupRestore: true`（`harmonyApp/entry/src/main/resources/base/profile/backup_config.json:2`），允许云备份恢复。它作为 profile 被备份扩展 Ability 挂名：`harmonyApp/entry/src/main/module.json5:72-83` 声明 `EntryBackupAbility`（`type: backup`，见 `harmonyApp/entry/src/main/module.json5:75`），metadata `ohos.extension.backup` 指向 `$profile:backup_config`（`harmonyApp/entry/src/main/module.json5:79-80`）。改成 false 即对用户关闭备份，代码不用动。

### 3.6 小文件全行速览（六行级文件逐行落位）

两份 hvigorfile 结构相同、只有插件名不同（`appTasks` 对 `hapTasks`）：

| 行 | `harmonyApp/hvigorfile.ts` | `harmonyApp/entry/hvigorfile.ts` |
|----|---------------------------|----------------------------------|
| `:1` | import `appTasks` | import `hapTasks` |
| `:2` | 空行 | 空行 |
| `:3` | `export default {` | 同左 |
| `:4` | `system: appTasks`（内置不可改） | `system: hapTasks`（内置不可改） |
| `:5` | `plugins: []` | `plugins: []` |
| `:6` | `}` | `}` |

`harmonyApp/entry/src/main/cpp/types/libentry/oh-package.json5`（6 行）：`:1` 开括号；`:2` `name: "libentry.so"`——ArkTS 侧 `import … from "libentry.so"` 解析到的包名；`:3` `types: "./Index.d.ts"`——同目录类型面（`Index.d.ts:3` 导出 `MainArkUIViewController`）；`:4` `version: "1.0.0"`；`:5` 占位 description；`:6` 闭括号。注意这里**不写** `.so` 的存放路径：真正的二进制由 `harmonyApp/entry/oh-package.json5:9` 的 `file:` 依赖把整个 types 目录认领进构建，再由 CMake 以同名产物兑现（`harmonyApp/entry/src/main/cpp/CMakeLists.txt:37`）。

四个资源小文件的逐行位（大文件 `string.json` 已在 3.5 逐条讲过）：

| 文件 | 行 | 内容 |
|------|----|------|
| `harmonyApp/entry/src/main/resources/base/element/color.json` | `:2-7` | `color` 数组，唯一条目 name 在 `:4`、value 在 `:5` |
| `harmonyApp/entry/src/main/resources/base/element/float.json` | `:2-7` | `float` 数组，唯一条目 name 在 `:4`、value 在 `:5` |
| `harmonyApp/entry/src/main/resources/base/element/string.json` | `:2-23` | `string` 数组五条，name 分别落在 `:4`、`:8`、`:12`、`:16`、`:20` |
| `harmonyApp/entry/src/main/resources/base/profile/backup_config.json` | `:2` | 唯一键 `allowToBackupRestore: true` |

ArkTS 资源引用语法是 `$color:名字`、`$string:名字`、`$float:名字`、`$profile:文件名`，工程级与模块级 `build-profile.json5` 都不涉及这些键——它们纯粹被 `harmonyApp/entry/src/main/module.json5` 与页面代码消费。

## 4. iOS project.pbxproj —— 与本项目相关的关键字段

`iosApp/iosApp.xcodeproj/project.pbxproj` 共 374 行，是 Xcode 16 格式（`objectVersion = 77`，`project.pbxproj:6`；`preferredProjectObjectVersion = 77`，`project.pbxproj:117`）。只列与 KMP 集成、启动屏、签名、搜索路径直接相关的字段；CLANG/GCC 警告类字段全部略过。

### 4.1 Kotlin 框架如何进包（embed 方式）

- 唯一的自定义构建阶段是 shell script「Compile Kotlin Framework」（`project.pbxproj:138-156`），挂在 target buildPhases 首位（`project.pbxproj:74`）。其 `shellScript`（`project.pbxproj:155`）内容做三件事：先看环境变量 `OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED` 是否等于 `YES`（等于则跳过，供纯预览场景），随后 `cd "$SRCROOT/.."` 回到仓库根，调用 `./gradlew :composeApp:embedAndSignAppleFrameworkForXcode`。也就是说 **framework 的编译、嵌入、重签名全部由 Gradle 任务完成**，工程里没有 Embed Frameworks 构建阶段，`PBXFrameworksBuildPhase` 的 `files` 列表为空（`project.pbxproj:40-46`）——被嵌入的是静态 framework（`composeApp/build.gradle.kts:44-47` 的 `isStatic = true`），不需要 embed-and-sign 之外的拷贝逻辑。
- `alwaysOutOfDate = 1`（`project.pbxproj:140`）：该阶段每次构建都执行，把「Kotlin 源码变了没」的判断完全交给 Gradle 增量。
- 脚本阶段要能写 DerivedData、跑 Gradle，前提是关掉脚本沙箱：project 级 Debug（`project.pbxproj:209`）与 Release（`project.pbxproj:274`）都设 `ENABLE_USER_SCRIPT_SANDBOXING = NO`。**这是本项目最容易被 Xcode 升级重置的字段**。
- 运行期查找路径：target 级 `LD_RUNPATH_SEARCH_PATHS` 含 `@executable_path/Frameworks`（`project.pbxproj:312-315` Debug、`:341-344` Release），嵌入产物落在那里。

### 4.2 设备族、搜索路径与配置分层

| 字段 | 行号（Debug、Release 成对） | 含义 |
|------|------|------|
| `TARGETED_DEVICE_FAMILY = "1,2"` | `project.pbxproj:318`、`:347` | iPhone 与 iPad 双设备族 |
| `ARCHS = arm64` | `project.pbxproj:296`、`:325` | target 级锁 arm64；模拟器可编性由 project 级 Debug `ONLY_ACTIVE_ARCH = YES`（`project.pbxproj:228`）兜住 |
| `IPHONEOS_DEPLOYMENT_TARGET = 18.2` | `project.pbxproj:224`、`:283` | 最低 iOS 版本（project 级，两个 configuration 共用同值） |
| `SDKROOT = iphoneos` | `project.pbxproj:229`、`:287` | 用 iOS SDK 解析 import |
| `baseConfigurationReferenceAnchor` 与 `baseConfigurationReferenceRelativePath` | `project.pbxproj:172-173`（Debug）、`:237-238`（Release） | project 两级 configuration 的基线都指向 `Configuration/Config.xcconfig`；该文件首行 `TEAM_ID=`（`iosApp/Configuration/Config.xcconfig:1`）**存放各人自己的 Apple team id，本机值不进文档**；bundle id 通过 `$(TEAM_ID)` 插值拼出（`iosApp/Configuration/Config.xcconfig:4`） |
| `DEVELOPMENT_TEAM = "${TEAM_ID}"` | `project.pbxproj:302`、`:331` | pbxproj 里只留占位引用，真实 team id 由 xcconfig 注入——这就是本项目「team id 不落 git」的机制 |
| `CODE_SIGN_STYLE = Automatic` 与 `CODE_SIGN_IDENTITY` | `project.pbxproj:299-300`、`:328-329` | 自动签名，证书由钥匙串解析，不落文件 |

framework、header 搜索路径在本 pbxproj 中**没有任何硬编码条目**：`embedAndSignAppleFrameworkForXcode` 任务按 Xcode 传入的 `PLATFORM_NAME`、`ARCHS` 逐 configuration 产出 framework 并注入搜索路径（Kotlin 插件的标准机制，见 4.1 的 `:155`），因此切模拟器、真机不需要改 pbxproj。

### 4.3 启动屏与文件同步

- `INFOPLIST_KEY_UILaunchStoryboardName = LaunchScreen`（`project.pbxproj:309`、`:338`）把冷启动绑定到 `LaunchScreen.storyboard`，同时 `INFOPLIST_KEY_UILaunchScreen_Generation = NO`（`project.pbxproj:308`、`:337`）禁止 Xcode 自动生成 LaunchScreen 段——两者配合 `AGENTS.md:96` 记载的「`#F7FAFC` 纯色底加居中启动图」设计同步。
- `GENERATE_INFOPLIST_FILE = YES` 与 `INFOPLIST_FILE = iosApp/Info.plist` 并存（`project.pbxproj:304-305`、`:333-334`）：以手写 Info.plist 为基底、Xcode 补充生成键。
- 目录成员用 Xcode 16 的文件系统同步组管理：`PBXFileSystemSynchronizedRootGroup` 指向 `iosApp`（`project.pbxproj:24-31`）与 `Configuration`（`project.pbxproj:32-36`），新增 Swift 文件不用改 pbxproj；唯一的例外声明把 `Info.plist` 排除出隐式成员（`project.pbxproj:14-20`，membershipExceptions 在 `:17`）。
- 桌面图标：`ASSETCATALOG_COMPILER_APPICON_NAME = AppIcon`（`project.pbxproj:297`、`:326`），对应 `AGENTS.md:96` 的 15 尺寸 AppIcon.appiconset 同步要求。

### 4.4 工程形态杂项（解释了为什么 pbxproj 这么短）

- 产物引用 `cmp_hello.app`（`project.pbxproj:10`）与 target 名 `iosApp`（`:86`）不同名，属历史遗留命名，改产物名不必动 target。
- `PBXSourcesBuildPhase` 与 `PBXResourcesBuildPhase` 的 `files` 都为空（`:160-166`、`:128-134`）：Swift 源经 4.3 的同步组自动入列，资源（含 storyboard 与 asset catalog）走 `INFOPLIST` 自动生成加编译期 catalog 提取，无需显式文件项。
- 全工程只有一个 native target（`:121-123`）与一组 Debug、Release 配置（`:354-371` 两个 `XCConfigurationList`），没有任何 scheme 级 hack——iOS 侧「配置」的重心实际在 `Config.xcconfig`（可被每台机器覆盖）而非 pbxproj。

## 5. .github/workflows/ci.yml —— 逐 job、逐 step

137 行，两个 job：`android` 与 `release`。

### 5.1 触发与全局

- `on.push`：main 分支加 `v*` tag（`.github/workflows/ci.yml:4-6`），且带 paths 过滤（`:7-19`）：只有 `composeApp`、`core`、`component`、`feature`、`ohosAggregate`、`gradle`、`scripts` 目录与根 `*.gradle.kts`、`gradle.properties`、wrapper 脚本、本 workflow 自身变化才触发——纯文档提交不烧 runner。`pull_request` 用同一组 paths（`:20-34`）。`workflow_dispatch` 允许手动跑（`:35`）。
- `concurrency`（`:37-39`）：同 PR 或同 ref 的新 run 取消旧 run，避免叠车。
- 顶层 `permissions: contents: read`（`:41-42`）最小权限；release job 再单独升级。

### 5.2 android job（`:44-115`）

| step | 行号 | 管什么 |
|------|------|--------|
| Checkout | `:51-52` | `actions/checkout@v4` |
| Set up JDK 17 | `:54-58` | temurin 17——与 `composeApp/build.gradle.kts:34` 的 `JVM_17` 及 `:217-218` 的 compileOptions 对齐（Koog 要求 JDK 17+，注释在 `composeApp/build.gradle.kts:33`） |
| Set up Android SDK 与装包 | `:60-64` | `android-actions/setup-android@v3` 后 `sdkmanager` 装 `platforms;android-36` 与 `build-tools;35.0.0`；36 正是 `gradle.properties:14` 压告警的那个 compileSdk |
| Write local.properties | `:66-67` | 用 CI 的 `ANDROID_HOME` 现场生成 `local.properties`——该文件被 `.gitignore:25` 排除，CI 必须自举（机制见第 8 节） |
| Set up Gradle | `:69-70` | `gradle/actions/setup-gradle@v4`，尊重 wrapper 版本并加 layer cache |
| Static gates | `:74-75` | 跑 `scripts/verify-parity.sh --static-only`：Kotlin 词法扫描加分层门禁（feature 不得依赖 feature 等），`:72-73` 注释说明「拆模块后漏一条就是整片红」 |
| Assemble debug 与单测 | `:77-81` | `./gradlew testDebugUnitTest :composeApp:assembleDebug -PandroidOnly=true --no-daemon`，日志 tee 进 `gradle-ci.log` |
| Summarize failure | `:83-94` | 失败时把日志尾 200 行写进 step summary，并用 grep 提关键行发 `::error::` 注解 |
| Upload log | `:96-106` | 失败才传：日志加分层测试报告目录（glob 写法见 `:101-104`），保留 7 天 |
| Upload debug APK | `:108-115` | **仅 push 或手动触发**（条件在 `:109`）才上传 `composeApp/build/outputs/apk/debug/*.apk`（`:113`），PR 不产 APK；`if-no-files-found: error`（`:114`）防止静默空包 |

**`-PandroidOnly=true` 的作用**：它是 `composeApp/build.gradle.kts:17` 读取的 Gradle 属性，为 true 时跳过整个 Apple 与 OHOS target 块——iOS framework（`composeApp/build.gradle.kts:38-48`）、OHOS sharedLib 与 cinterop（`composeApp/build.gradle.kts:50-109`）、ios/ohos 中间 source set（`composeApp/build.gradle.kts:161-181`）、两组 publish 任务（`composeApp/build.gradle.kts:259-300`）都不再注册。这就是「为什么 iOS、OHOS 不在托管 runner」的技术答案：ubuntu runner 既没有 Xcode/Apple 工具链，也没有 DevEco 的 OHOS native sysroot（本机路径解析见 `composeApp/build.gradle.kts:19-27`，默认回退到 `/Applications/DevEco-Studio.app/...`）；去掉这两个 target 后，K/N 交叉编译不再参与 CI，Android 流水线才能在 60 分钟超时（`.github/workflows/ci.yml:48`）内跑完。`AGENTS.md:114` 的一行描述与此完全一致。

### 5.3 release job（`:117-137`）

- `needs: android`（`:119`）且 `if` 条件是 ref 以 `refs/tags/v` 开头（`:120`）——即**只有 `v*` tag 触发的 run 才会走到这里**，普通 main push 不发包。
- `permissions: contents: write`（`:122-123`）只给这个 job 写 release 的权限。
- 先 `download-artifact` 取回 android job 上传的 APK（`:125-129`），再由 `softprops/action-gh-release@v2` 发布（`:131-135`），`generate_release_notes: true`（`:135`）自动生成说明。token 用 runtime 内置 `secrets.GITHUB_TOKEN`（`:136-137`）——**引用的是 secret 名，不存在任何真实凭据值**。注意 release 用的是 debug APK，签名是 debug key，这点从 `:113` 的路径即可看出。

### 5.4 ci.yml 刻意不做的事

- 不做 iOS 构建：托管 runner 无 Xcode，且 `-PandroidOnly=true` 后 framework target 根本没注册（`composeApp/build.gradle.kts:38-48`），任何 iOS task 都不存在。
- 不做 OHOS 链接：`ohosArm64`、`ohosX64` target 同样被跳过（`composeApp/build.gradle.kts:50-109`），且 sysroot 探测在 CI 机器上必然落空（`composeApp/build.gradle.kts:19-27`）。iOS、OHOS 的真验证走本地 `./scripts/run-ios.sh`、`./scripts/run-harmony.sh` 与 `deployment.gradle.kts` 的 deploy 任务（口径见 `AGENTS.md:114`）。
- 不发布应用商店产物：release job 只挂 debug APK（`.github/workflows/ci.yml:113`、`:134`），没有 bundle、没有签名密钥注入，也就没有任何凭据经由 workflow 常量泄漏的风险面。
- 不做文档校验：paths 过滤（`:7-19`）不含 `docs/**`，本系列文档改动不会触发 CI。

## 6. openspec/config.yaml（以及它和 .claude-flow/config.yaml 的区别）

`openspec/config.yaml` 共 42 行，是 OpenSpec 工作流的说明书，不参与任何构建。

- `schema: spec-driven`（`openspec/config.yaml:1`）：声明规格驱动模板集。
- `context`（`:4-24`）：每次 AI 创建 change 工件时注入的项目事实。含分层与壳结构（`:7`、`:11-13`）、包根（`:9`）、四 tab shell（`:8`）、catalog 唯一事实源与 wrapper 规则（`:17-18`）、commonMain 优先与 feature 互不依赖的约定（`:20-21`）。两条值得注意：`:24` 明令不得重新引入 FanGroup、tfent、T-Family、paopao 的旧品牌与业务模块；`:14` 写「intermediate source sets: networkKtorMain（android+ios）、accountSettingsMain（android+ios）」——与 `AGENTS.md:58-59` 的现名存在**口径漂移**（settings 线的现名是 `settingsMain`，且 `networkKtorMain` 现已含 ohosArm64），以 `AGENTS.md:54-61` 与真实 source set 为准。
- `rules`（`:26-33`）：proposal 不超 500 词（`:28`）、必带 Non-goals（`:29`）、显式标注三端影响（`:30`）；tasks 偏好可验证小步（`:32`）并推荐最窄编译任务 `:composeApp:compileDebugKotlinAndroid`（`:33`）。
- `operations`（`:35-43`）：apply 阶段 commonMain-first 与「改完跑聚焦编译测试」（`:38-39`），archive 阶段总结「落进规格的」与「留在范围外的」（`:42`）。

### 6.1 context 块逐行覆盖（`:5-24`）

| 行 | 事实条目 | 备注 |
|----|----------|------|
| `:5` | 项目名与 KMP、CMP 定位 | 与 `AGENTS.md:3` 同口径 |
| `:6` | 三端均为一等目标 | Android、iOS、HarmonyOS |
| `:7` | 五层架构 app、feature、component、core、platform | 指向 `docs/adr/0001-production-architecture.md` |
| `:8` | 四 tab Shell：Home、Chat、Community、Mine | 与 `AGENTS.md:190` 的底栏规则呼应 |
| `:9` | 包根 `com.example.my_kmp_project` | 与 `composeApp/build.gradle.kts:193` 的 namespace 一致 |
| `:10-13` | 模块布局三行：composeApp 唯一产出方、iosApp 薄 SwiftUI 壳、harmonyApp 薄 ArkTS 壳 | 对应 `AGENTS.md:36`、`:42-43` |
| `:14` | 中间 source set 清单 | **口径偏旧**，见上文漂移说明 |
| `:15` | 设计 token 与沉浸式 helper 归属 core/design | 呼应 `AGENTS.md:203` |
| `:16` | 网络线：DemoApiHosts、DemoRequestHeaders、OHOS 走 `Demo_Http_*` cinterop | 与 `core/network` 文档（13、14 篇）一致 |
| `:17` | catalog 唯一事实源 | 同 `AGENTS.md:5` |
| `:18` | 永远 `./gradlew` | 同 `AGENTS.md:102` |
| `:19-24` | 五条 conventions：commonMain 优先、feature 互不依赖、壳保持薄、中文问中文答、禁回旧品牌 | `:24` 是旧品牌红线 |

**别混淆**：`.claude-flow/config.yaml`（43 行）是 Ruflo 多代理编排器的运行时配置，与 OpenSpec 无关——`version: "3.0.0"`（`.claude-flow/config.yaml:4`）、swarm 拓扑（`:6-10`）、memory 后端与落盘目录 `.claude-flow/data`（`:13-15`）、neural 与 hooks 开关（`:33-39`）、MCP 端口 3000（`:41-43`）。其中 data、logs、sessions 等运行态目录被 `.gitignore:129-134` 排除，本文件只留静态配置。

## 7. .gitignore —— 关键条目机制

146 行，按块讲「为什么排除」。

- **构建产物与缓存**（`.gitignore:1-13` 的 apk、aar、hap、har、ipa 等产物通配；`:16-22` 的 Gradle 块）：`.gradle/`、`.kotlin/`、`**/build/`（`:17-19`），但 `!src/**/build/`（`:20`）放行源码目录里叫 build 的文件夹——这正是 `AGENTS.md:207` 「只提交源码与工程配置」的执行层保障。
- **本机配置与密钥**（`:24-32`）：`local.properties` 与 `**/local.properties`（`:25-26`）、`.env` 系列（`:27-29`）、keystore、jks、pem（`:30-32`）。机制：AGP 只在本机文件系统读 `local.properties`（见第 8 节），提交它等于把每台机器的 SDK 绝对路径写进工程。
- **iOS 块**（`:50-71`）：`xcuserdata/`（`:51`）与 `iosApp/Pods/`（`:64`）之外，`:66` 用 `*.xcodeproj/*` 收紧整个工程包，再用 `!*.xcodeproj/project.pbxproj`（`:67`）把 pbxproj **显式捞回**——第 4 节讨论的那份文件能被 git 跟踪，全靠这两行的「先全拒、再豁免」。
- **HarmonyOS 块**（`:73-83`）：`**/oh_modules/`（`:75`，ohpm 依赖目录，等价于 node 的 node_modules，`:74`）与 `.hvigor/`、`**/.hvigor/`（`:76-77`，hvigor 的增量缓存）。DevEco 每次 sync 都会重建它们，提交只会制造 diff 噪音。
- **KMP 投递产物块**（`:85-90`）：`harmonyApp/entry/libs/`、`harmonyApp/entry/src/main/cpp/include/`、`harmonyApp/entry/src/main/resources/rawfile/composeResources/`（`:86-88`）连同 `**/libkn.so`、`**/libkn_api.h`（`:89-90`）。`:85` 注释点明机制：这些是 `publish*BinariesToHarmonyApp` 的可再生输出（任务定义在 `composeApp/build.gradle.kts:259-300`），git 里存二进制快照必然与 Kotlin 源码漂移，所以「生成物不进库、由 Gradle 现生成」；CMake 侧缺文件时的报错也直接引导去跑 publish（`harmonyApp/entry/src/main/cpp/CMakeLists.txt:20-23`）。
- **Agent 工具块**（`:92-114` 与 `:126-143`）：`.cursor/*` 整体忽略、但 rules、commands、skills、mcp.json 白名单捞回（`:93-100`）；`openspec/` 显式保持跟踪（`:103-104`）；`.agent`、`.claude`、`.codex`、`.gemini` 本地态忽略（`:107-110`）；Ruflo 的 secret 与运行态（`:127-134`）以及向量库文件 `*.rvf` 等（`:138-143`）不进库。

## 8. AGENTS.md —— 结构与它如何约束 agent

`AGENTS.md`（220 行）是每次会话自动注入的项目上下文，它约束 agent 的方式不是「建议」而是给出可判定的规则与验收口径：

| 段 | 行号 | 约束内容 |
|----|------|----------|
| 头部与版本口径 | `AGENTS.md:1-5` | 版本、插件、坐标只信 `gradle/libs.versions.toml`，禁止在本文件复述版本 |
| Agent 工作流栈 | `AGENTS.md:7-27` | OpenSpec propose→apply→archive 循环（`:17-21`）加 Agency Agents、Ruflo 分工 |
| Map | `AGENTS.md:29-46` | 28 模块图：feature 之间禁止依赖（`:35`）、composeApp 是唯一二进制产出方（`:36`）、壳保持薄（`:42-43`） |
| 接线现实 | `AGENTS.md:48-52` | buildSrc 本机不可用的原因（init.gradle 钉镜像），新模块最小三件套 |
| 中间 source set | `AGENTS.md:54-61` | 哪些依赖只能进哪个 source set，防把 OHOS 不支持的工件塞进 commonMain |
| Placement | `AGENTS.md:67-79` | common-first 决策表 |
| 图标与 splash | `AGENTS.md:83-98` | 三端启动资产位置与 `#F7FAFC` 口径；改图标后要卸载重装（`:98`） |
| Build & run | `AGENTS.md:100-118` | 只许 `./gradlew`（`:102`）、CI 一句话口径（`:114`）、`-PharmonyAppPath` 逃生门（`:116`）、`rendererBackend` 只在有意换渲染器时改（`:118`） |
| OHOS publish 契约 | `AGENTS.md:120-137` | 拷贝清单（`:124-125`）、「改了面向 Harmony 的 Kotlin 必须先 publish 再 DevEco run」（`:127`）、release `optimized = false`（`:129`）、HTTP、图片、UUID、KV 各线的架构差异（`:131-137`） |
| Conventions | `AGENTS.md:139-144` | catalog 别名优先、命名对齐邻居、改动单焦点 |
| Compose 硬规则 | `AGENTS.md:146-203` | 只许可编译的真 Compose API（`:148`）、沉浸式顶栏与底栏 inset 规范（`:150-186`）、`MainBottomBar` 仅 tab 根（`:190`）、`Modifier` 构造与 `modifier` 形参的硬区分（`:192-199`）、`Res.drawable` 而非 `R.drawable`（`:201`）、`DemoColors` token 优先（`:203`） |
| Guardrails | `AGENTS.md:205-210` | 提交面、local.properties 私有、优先改 commonMain、动 OHOS 打包时 publish 与 ArkTS 消费方同步改 |
| Done when | `AGENTS.md:212-220` | 五条完成判据：source set 正确、catalog 接线、目标构建通过、Compose 合规、Harmony 二进制已 publish 且 `MainArkUIViewController` 可解析 |

对 agent 的实际效果：写码前 Placement 与 Compose 两段划定合法落点与合法 API；写完 Done when 给出「什么才算完」；CI（第 5 节）用静态门禁把「feature 互不依赖」这类 AGENTS 规则机器化。`AGENTS.md:45` 同时是 `local.properties` 在仓库地图中的定位——「机器本地 SDK 路径，永不提交」。

## 9. local.properties —— 机器本地机制

本机这份 8 行：`:1-6` 是模板注释（`:1` 首行即「此文件绝不可提交到版本系统」，`:4` 说明「SDK 位置，只给 Gradle 用」），`:7` 是生成时间戳，`:8` 是 `sdk.dir` 键（值是本机 Android SDK 绝对路径，**属机器隐私，不复制**）。它不读入 git：`.gitignore:25-26` 双重排除；CI 环境则每次现写（`.github/workflows/ci.yml:66-67`）。本仓还有第二个消费者：`composeApp/build.gradle.kts:20-27` 会在 `local.properties` 里找 `local.ohos.native=` 键来定位 OHOS native sysroot，找不到再退环境变量 `OHOS_NATIVE_SYSROOT`（`:24`）、最后退 DevEco 默认安装路径（`:25`）——本机没写这个键，所以走默认回退。结论：换机器、换 SDK 布局时改这个文件即可，工程零 diff。

## 10. 配置文件 → 改它需要重新跑什么

| 改动对象 | 必须重跑 | 依据 |
|----------|----------|------|
| Kotlin、Compose 共享代码（面向 Harmony） | `./gradlew :composeApp:publishDebugBinariesToHarmonyApp` 后 DevEco run 或 `./scripts/run-harmony.sh` | `AGENTS.md:127`、`AGENTS.md:111-112`；CMake 缺文件即 FATAL（`harmonyApp/entry/src/main/cpp/CMakeLists.txt:20-23`） |
| `gradle.properties:22` `rendererBackend` | 重链 `libkn.so`（link 任务由 publish 任务依赖触发，`composeApp/build.gradle.kts:263`）再重装 HAP | `composeApp/build.gradle.kts:76-91`、`AGENTS.md:118` |
| `gradle.properties:7` JVM 参数 | 首次调用重开 daemon；无需清产物，但 K/N 链接任务实际会重跑 | daemon 参数变化使缓存的 configuration 失效（`gradle.properties:8`） |
| `gradle-wrapper.properties:3` 版本或镜像 | 任何 `./gradlew` 首跑自动下载新分发；三端全部重验 | `AGENTS.md:102` |
| `harmonyApp/build-profile.json5` 签名或 SDK 版本 | DevEco Sync 后重新 assemble 签名 HAP、装机 | 该文件 products 段（`harmonyApp/build-profile.json5:18-33`）直接决定 SDK 与签名 |
| `harmonyApp/entry/build-profile.json5` 的 `abiFilters` 或 nativeLib 过滤 | DevEco Clean 后全量 rebuild（CMake 配置变了） | `harmonyApp/entry/build-profile.json5:14-16`、`:19-25` |
| entry 资源 json（color、string、float）与 backup_config.json | hvigor 重新打包即可，无需动 Gradle | 引用点在 `harmonyApp/entry/src/main/module.json5:19-80` |
| `project.pbxproj` 或 `Config.xcconfig`（team id） | Xcode 直接重 build；脚本阶段 `alwaysOutOfDate` 保证 framework 必重嵌（`project.pbxproj:140`） | `./scripts/run-ios.sh`（`AGENTS.md:107`） |
| `.github/workflows/ci.yml` | push 即触发（paths 含 workflow 自身，`.github/workflows/ci.yml:19`、`:34`） | 无需本地动作 |
| `local.properties` | Gradle Sync 一次；SDK 变化后首跑重建 configure | `composeApp/build.gradle.kts:20-27` |
| `openspec/config.yaml`、`AGENTS.md`、`.gitignore`、`.claude-flow/config.yaml` | 不进构建，改后只影响下一次 agent 会话与工作流 | `openspec/config.yaml:4`、`AGENTS.md:7-27` |

## 11. 本文脱敏口径备忘

以下字段本文刻意只写名称与行号、从未复制值：`harmonyApp/build-profile.json5` 的 `certpath`、`keyAlias`、`keyPassword`、`profile`、`signAlg`、`storeFile`、`storePassword`（`:8-14`）；iOS 真实 team id（pbxproj 中只存在 `${TEAM_ID}` 占位，`project.pbxproj:302`、`:331`，实值在本机 `Config.xcconfig`）；`local.properties` 的 `sdk.dir` 绝对路径（`:8`）；上游 Flutter SoT 仓库历史提交里的第三方凭据（慧眼 SDK 的 app id、licence 一类字段），引用时同样只写字段名。CI 侧仅出现 secret 的**引用名**（`.github/workflows/ci.yml:137`），无任何值。

## 12. 口径边界与延伸阅读

本文不回答「构建脚本怎么写」——target 声明、source set 依赖、publish 任务内部逻辑见 `docs/deep-dive/09-build-and-config.md`；不回答「壳代码怎么跑」——ArkTS 入口加载与 SwiftUI 宿主细节见 `docs/deep-dive/23-native-shells.md`；门禁脚本与 ADR 制度见 `docs/deep-dive/24-gates-ci-adr.md`。三份口径的交集约定：配置文件里的每一处「为什么这么设」，最终以本仓 `AGENTS.md` 与 `openspec/` 的活文档为准；当 `openspec/config.yaml:14` 这类快照行与代码漂移时，以 source set 实际声明与 `AGENTS.md:54-61` 为准。


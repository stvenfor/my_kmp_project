# 从 demo 到生产：KMP 大型项目工程化学习计划

> **依据**：`docs/ARCHITECTURE.md`（提交 `55116c5` 的架构事实）与本对话上一轮《详细架构梳理与差距分析》（A–N 共 14 个维度、全局编号 `#1`–`#94` 的差距清单）。
> **目标**：把 `my_kmp_project` 从「功能演示 + 跨端对齐 SoT」推进到「可上线、可运维、可多人长期迭代」；每一阶段都在本仓库落地可验收的产出，而不是只读资料。
> **本计划不给时间估计**：阶段按依赖顺序排列，用「前置阶段」表达先后；进度以验收结果为准。
> **纪律**：不 push；不提交密钥/证书/`.env`；需厂商凭据、开发者账号或真机才能验证的项，一律先做接口 + 契约测试 + 降级 + gap 登记，不伪称完成。

---

## 0. 怎么用这份计划

### 0.0 两种说法的约定（先读这一段）

**专业说法**：用准确的术语描述机制、约束与副作用，能直接对照官方文档和源码，不含比喻。
**通俗说法**：用生活类比解释「它在解决什么麻烦、不解决会怎样」，不要求先懂术语。

- 专业版：`api()` 会把依赖暴露给下游编译类路径，导致接口变更触发更大范围的重编译。
- 通俗版：`api()` 像把自家钥匙也配了一把给邻居；你换锁，邻居也得跟着换。

本文件每个阶段都有「**通俗讲解 → 专业讲解 → 示例讲解**」三段（示例讲解的标题会带一个括号，写明这个例子在讲什么）；示例讲解里的每段代码/命令都逐行拆开，并再给一次专业/通俗对照。**第 0.1 节是专有名词词典**，按 A–K 分 11 组、约 100 个词条，遇到不认识的词先查那里；词典里每个词条同样给「专业 / 通俗 / 本仓库（或易混提示）」三行。所有内容都按这两种说法各讲一遍，不假设读者已经懂任何一个缩写。

> **哪些是「真实代码」、哪些是「示意代码」**：示例里凡来自仓库的片段都标了具体路径与行号；凡标「示意，非仓库现状」的都是讲解用的目标写法，接入前须按当前 AGP / Compose / KMP 版本核对，不能直接复制编译。这样既讲清「现在是什么」，也讲清「要改成什么」，不会把示意误当成仓库已有实现。

每个阶段仍固定五段：**学什么 → 读哪些代码 → 动手任务（标注差距编号）→ 验收 → 自检问题**。

- 任务里的 `[#n]` 对应上一轮差距清单的全局编号；本文件第 5 节给出 `#1`–`#94` 的全覆盖索引，确保无遗漏。
- 所有代码改动沿用仓库既有风格，最小改动、不顺手重构；改 `@Composable` 遵守 `AGENTS.md` 的 Compose / 沉浸式规范（`ImmersiveCenterTopAppBar` / `Modifier` vs `modifier` / 不重复 `navigationBarsPadding()`）。
- 涉及多模块改动时，先用 `scripts/with-gradle-lock.sh` 包一层窄构建，避免并发 agent 抢 Gradle daemon 锁（脚本注释记录过 666s 病例）。
- 每阶段收尾必跑第 3 节的通用验收命令；新增能力必须带回归测试。
- 本机 `buildSrc` 不可用（`~/.gradle/init.gradle` 钉镜像，解析不到 `org.gradle.kotlin.kotlin-dsl`），因此「convention plugin」类任务允许先以 composite build 试点，或先产出一份评估文档说明约束，不要硬改根工程接线。

### 0.1 专有名词详解（专业 + 通俗 + 本仓库）

> 格式：**词条（全称）** → 专业：… / 通俗：… / 本仓库：…（或易混提示）。按主题分组，可当词典跳读。

#### A 组 · 平台与语言

**KMP（Kotlin Multiplatform，Kotlin 多平台）**
- 专业：一套 Kotlin 代码编译到多个目标平台（JVM / Native / JS / Wasm）的技术，业务逻辑放 `commonMain`，平台 API 通过 `expect`/`actual` 分派。
- 通俗：同一份菜谱，在 Android、iOS、鸿蒙三个厨房各做一遍，不用重写三本菜谱；但「开火」这种每个厨房不一样的动作要各写一句。
- 本仓库：`androidMain` / `iosMain` / `ohosMain` 三套入口，共享逻辑在 `commonMain`。

**CMP（Compose Multiplatform，声明式多平台 UI 框架）**
- 专业：JetBrains 基于 Compose Compiler 的跨平台 UI 运行时；`@Composable` 函数描述 UI，状态变化时按「重组」局部刷新，而非命令式 `setText`。
- 通俗：不是告诉界面「把第 3 行文字改成 X」，而是说「界面长这样，数据变了你照着重新画」；画的时候只重画变了的那一块。
- 本仓库：`composeApp/commonMain` 的 `AppShell.kt`、`core/design` 全部组件；版本号 `1.9.2-0.3.0`（OHOS fork）。

**Source Set（源集）**
- 专业：Kotlin 编译的最小输入单元，一组源码目录 + 依赖 + `dependsOn` 关系；`commonMain` 是根，`androidMain` 等平台源集依赖它。
- 通俗：一堆「只在某种情况下才编译的文件」；共用的放公共箱，Android 专用的放 Android 箱。
- 本仓库：`settings.gradle.kts` 的 30 个模块，每个模块下都有 `commonMain`。

**Intermediate Source Set（中间源集）**
- 专业：介于 `commonMain` 与平台源集之间的自定义源集，用 `dependsOn` 挂到部分平台，用于「只在某些平台可用的第三方库」。
- 通俗：不是「所有厨房」也不是「某一个厨房」，而是「只有其中两个厨房有这台烤箱」时共用的那格柜子。
- 本仓库：`networkKtorMain`（android+ios+ohosArm64）、`settingsMain`（android+ios）、`imageCoilMain`、`uuidCpfMain`、`chartsVicoMain`。

**expect / actual（期望声明 / 平台实现）**
- 专业：`commonMain` 用 `expect` 声明签名，各平台源集用 `actual` 提供实现；编译器逐目标校验一一对应，缺失即编译失败。
- 通俗：公共代码开一张「我需要有人会开火」的欠条，每个厨房各自签收并写自己的开火方式；少签一张就开不了工。
- 本仓库：`ApiClient.kt` 的 `expect fun createPlatformApiClient(...)`，由 `KtorApiClient.android.kt` / `.ios.kt` / ohos 侧实现。

**klib（Kotlin/Native Library）**
- 专业：Kotlin/Native 的产物格式（含序列化 IR 与元数据），不是 JVM 的 `.jar`；依赖必须存在对应目标平台的 klib 才能链接。
- 通俗：给原生平台用的「专用零件包」，JVM 的零件包装不进去；少一个平台的零件，那个平台就装不起来。
- 本仓库：OHOS `ohosArm64` 有 CPF Ktor/Coil/uuid 的 klib，`ohosX64` 没有，所以要 cinterop 或 stub。

**cinterop（C/ObjC 互操作）**
- 专业：Kotlin/Native 通过 `.def` 文件把 C 头文件/系统库映射成 Kotlin 声明的机制，用于没有 klib 的平台能力。
- 通俗：没有现成零件包时，照着说明书自己接线，把系统自带的 C 函数「翻译」成 Kotlin 能调用的样子。
- 本仓库：`core/network/src/ohosMain/cinterop/net_http.def`（ohosX64 的 HTTP 回退）、`composeApp` 的 `resource.def` / `avplayer.def`。

**OHOS / ohosArm64 / ohosX64（HarmonyOS 与两种 CPU 架构）**
- 专业：OHOS 是鸿蒙目标；`ohosArm64` 是真机架构，`ohosX64` 是模拟器/开发机架构；二者可用的第三方 klib 集合不同。
- 通俗：同一个系统的「手机版」和「电脑模拟版」，能装的软件不完全一样。
- 本仓库：`composeApp/build.gradle.kts:50-110` 同时声明两个 target；`libs.versions.toml` 多个 CPF 库注明「no ohosX64 klib」。

**CPF / `-0.3.0` 版本线**
- 专业：本仓库所用 OHOS fork 的第三方库发布线（如 Ktor `3.3.3-0.3.0`、Coil `3.3.0-0.3.0`），由 eazytec 镜像提供，只覆盖部分 OHOS 架构。
- 通俗：原厂零件没有鸿蒙版，有人改了一版「鸿蒙定制件」；版本号后面挂的 `-0.3.0` 就是定制标记。
- 本仓库：`settings.gradle.kts:7` 的 maven 镜像、`gradle/libs.versions.toml:33-49` 的版本注释。

**SoT（Source of Truth，唯一真相源）**
- 专业：某个事实只允许有一处权威定义，其余派生/缓存/文档都必须由它校验或生成，避免多份数据互相矛盾。
- 通俗：全公司只认一本总账，其他本子都只能抄它；抄错了拿总账一对就知道。
- 本仓库：`notes/backend-contract-matrix.md` 声明 Go 后端路由是 `/api/v1/*` 的 SoT；`tasks-ultra-fine-v2.md` 是唯一执行账本。

**AGP / KGP（Android / Kotlin Gradle Plugin）**
- 专业：AGP 提供 `android {}` DSL 与打包任务；KGP 提供 `kotlin {}` 多平台 target 与编译任务；二者版本必须兼容。
- 通俗：一个负责「怎么把代码打成安卓安装包」，一个负责「怎么把 Kotlin 翻成各平台能跑的东西」。
- 本仓库：`libs.versions.toml` 的 `agp = "8.6.0"` / `kotlin = "2.2.21-0.3.0"`。

**`@Composable` / Recomposition（重组）**
- 专业：编译器给函数加 `@Composable` 标记后，运行时按状态快照追踪读取；状态变化时只重新执行读取了该状态的组合函数（跳过未变部分）。
- 通俗：数据变了，界面只擦掉重画受影响的小块，不是整页撕掉重画。
- 易混：`Modifier` 是工厂对象（`Modifier.fillMaxSize()`），`modifier` 只是参数名；写混会编译失败。

#### B 组 · 构建与 Gradle

**Version Catalog（版本目录）**
- 专业：`gradle/libs.versions.toml` 集中声明 version / library / plugin 别名，Gradle 生成类型安全的 `libs.*` 访问器，避免坐标散落在各模块。
- 通俗：全项目共用一张「零件价目表」，改版本只改表里一行，不用挨个车间翻。
- 本仓库：44 个 library 别名、7 个 plugin；实测 32 / 6 被引用，12 / 1 声明未用。

**`api()` vs `implementation()`（依赖可见性）**
- 专业：`api` 把依赖暴露到下游模块的编译类路径（传递依赖）；`implementation` 只在本模块可见，下游需要时必须自己声明。后者能缩小增量重编译范围。
- 通俗：`api` 是「我把工具借给你，还帮你把工具箱也送人了」；`implementation` 是「工具只在我屋里」，别人想用自己买。
- 本仓库：多个 `feature/*/build.gradle.kts` 用 `api(projects.core.ui)`，注释自认「传递带出 core.platform」。

**`subprojects {}`（子工程统一配置）**
- 专业：根工程对所有子项目求值的配置块；本仓库用它统一 30 个库模块的 target 集、Android 基础配置与公共依赖。
- 通俗：总厂给所有车间发同一份操作规程，不用每个车间各抄一份。
- 易混：求值范围是整个工程，所以「窄构建」也要配置全部模块，这是 `with-gradle-lock.sh` 存在的原因。

**Convention Plugin / buildSrc / Composite Build（约定插件 / 构建逻辑）**
- 专业：把重复构建逻辑抽成可复用插件；`buildSrc` 是 Gradle 自动编译的构建逻辑目录，composite build（`includeBuild`）是更解耦的替代。
- 通俗：把「每个车间都要贴的同一张规程」印成一张官方贴纸，谁要谁领，而不是每份文件手抄。
- 本仓库：`build.gradle.kts:25-33` 注释说明本机 `~/.gradle/init.gradle` 钉镜像、解析不到 `org.gradle.kotlin.kotlin-dsl`，`buildSrc` 走不通，才改用 `subprojects {}`。

**Configuration Cache / Build Cache（配置缓存 / 构建缓存）**
- 专业：前者缓存「配置阶段」结果、后者缓存「任务输出」；命中时跳过重复工作。要求构建脚本不能在配置期执行外部进程等副作用。
- 通俗：把「摆工具」和「做出来的半成品」分别拍照存档，下次直接沿用，不用从头摆、从头做。
- 本仓库：`gradle.properties` 两项均为 `true`；`deployment.gradle.kts` 因 29 处 `exec` 改用注入的 `ExecOperations`（Gradle 9 移除了 `Project.exec`）。

**Dependency Verification / Lockfile / SBOM（依赖校验 / 锁文件 / 软件物料清单）**
- 专业：verification 校验下载产物的校验和/签名；lockfile 固定解析版本；SBOM 列出全部依赖及许可证，供合规与漏洞扫描。
- 通俗：收货时核封条、把采购清单钉死、再留一份「这批货都含什么」的清单，防止被人偷偷换料或事后说不清。
- 本仓库：三者均缺；`.gitignore` 已排除 `local.properties` / `.env` / `*.keystore`。

**`explicitApi()` / Binary Compatibility Validator（显式 API 模式 / 二进制兼容校验）**
- 专业：前者强制公共声明显式写可见性（库模块默认不允许「隐式 public」）；后者对比公共 ABI 快照，防止不经意的破坏性变更。
- 通俗：库对外给的每样东西都必须挂名牌；以后改名要先比对旧名牌，别让老客户突然找不到门。
- 本仓库：均未启用，部分文件手写 `public`。

**`-PandroidOnly`（Gradle 项目属性开关）**
- 专业：通过 `-P` 传入的项目属性；本仓库用它跳过 iOS/OHOS target 配置，让 CI 只跑 Android。
- 通俗：一个「今天只开一条生产线」的开关，省掉另外两条线的准备时间。
- 本仓库：`composeApp/build.gradle.kts:17`、`core/network/build.gradle.kts:13`、`component/build.gradle.kts:17`、`.github/workflows/ci.yml:80`。

**R8 / ProGuard / minify / shrinkResources（代码混淆与资源压缩）**
- 专业：R8 在 release 构建做代码裁剪、混淆与优化；ProGuard 规则文件声明「哪些类不能被改/删」；`shrinkResources` 移除未引用资源。
- 通俗：出厂前把说明书里没用的页撕掉、把内部代号改成短名，既省空间也让外人不易看懂；但要事先标好「这几个名字不能改」。
- 本仓库：`composeApp/build.gradle.kts:213` 是 `isMinifyEnabled = false`，无 proguard 文件。

**AAB（Android App Bundle）**
- 专业：Google Play 的发布格式，上传后由商店按设备密度/ABI 拆出最优 APK；本地 `assembleDebug` 产出的是可直接安装的 `.apk`。
- 通俗：不寄一箱通用零件，而是寄一张「配置单」，商店按每台手机的身材现配现装。
- 本仓库：CI 只上传 debug APK，未产 AAB。

**signingConfig / Keystore（签名配置 / 密钥库）**
- 专业：用私钥对 APK/AAB 签名，系统据此验证来源与升级连续性；密钥绝不能入库。
- 通俗：给包裹盖一个只有你有、且能验真的章；章丢了或外泄，别人就能冒充你发版。
- 本仓库：全仓无 `signingConfig`；`.gitignore` 排除 `*.keystore`，`iosApp/Configuration/Config.xcconfig` 的 `TEAM_ID=` 为空。

**Baseline Profile / Macrobenchmark（基准配置 / 宏基准测试）**
- 专业：Baseline Profile 把关键路径的类与方法预先 AOT 编译，降低冷启动 JIT 开销；Macrobenchmark 在真机上测量启动/滚动等用户级耗时。
- 通俗：把上班第一天最常走的几条路提前修好，别等临时踩出来；再用秒表实测走完要多久。
- 本仓库：均缺，只有 `RealtimeRuntime` 的「首连不阻塞冷启动」设计，无量化数据。

**Gradle Daemon / Advisory Lock（守护进程 / 建议锁）**
- 专业：Daemon 是常驻构建进程；多个进程并发请求会争抢锁导致长等待。advisory lock 是协作式互斥（不强制，靠所有调用方自觉遵守）。
- 通俗：一台共用机器，大家约好「谁用谁挂个牌子」，看到牌子就排队；不强制锁门，但不守规矩就会撞车。
- 本仓库：`scripts/with-gradle-lock.sh` 与 `verify-parity.sh` 共用 `/tmp/qoder-gradle-verify.lock`，注释记录过 666s 等待病例。

**`rendererBackend` / fusion-renderer / Skia（渲染后端）**
- 专业：OHOS 上 Compose 的绘制后端选择；`gradle.properties` 默认 `fusion-renderer`，链接参数在 `composeApp/build.gradle.kts:76-91` 按此开关切换。
- 通俗：同一幅画用哪套画笔系统来画，换画笔要连带换一批颜料接口。
- 本仓库：`gradle.properties:22` 与 `ohosSkikoVersion`。

**`libkn.so` / `@CName` / N-API（OHOS 原生产物与导出符号）**
- 专业：`libkn.so` 是 Kotlin/Native 编出的共享库；`@CName("MainArkUIViewController")` 把 Kotlin 函数导出为 C 符号；ArkTS 经 N-API 调用它。
- 通俗：把 Kotlin 代码压成一块「能被鸿蒙系统直接插上」的板子，并在板子上留一个写死名字的插口。
- 本仓库：`MainArkUIViewController.kt:33`、`harmonyApp/entry/src/main/ets/pages/Index.ets:7`。

**Framework（iOS 框架）**
- 专业：KMP 产出的 iOS 二进制（`ComposeApp.framework`），由 SwiftUI 壳链接；静态框架 `isStatic = true`。
- 通俗：给 iPhone 工程用的一个「打包好的功能盒」，Swift 代码直接 `import ComposeApp`。
- 本仓库：`composeApp/build.gradle.kts:44-47`、`iosApp/iosApp/ContentView.swift:3`。

#### C 组 · 架构与状态

**Composition Root（组合根）**
- 专业：应用中唯一负责「把接口与实现组装起来」的位置，越靠近入口越好；其余代码只依赖抽象。
- 通俗：全楼的配电箱只有一间房有，谁要用电都从这里接，别每户自己拉一根主线。
- 本仓库：`AppContainer.kt:18-57`，含 `NetworkComponent` / `SessionComponent` / `BridgeComponent` / `NavigationComponent`。

**DI（Dependency Injection，依赖注入）/ Service Locator / Koin / Scope**
- 专业：DI 由外部把依赖传进来；Service Locator 是对象自己去「容器」里取；Koin 是 Kotlin 的轻量 DI 框架；Scope 定义依赖的存活范围（如登录期间）。
- 通俗：DI 像「食材送到工位」，Service Locator 像「自己去仓库领」；Scope 决定这袋食材是「这一顿」还是「这一整天」有效。
- 本仓库：`AppContainer.kt:16` 注释「migrate to Koin modules after Spike I (ADR 0001 / Q15=D)」，当前是手写组合根 + 单例 `get()`。

**ViewModel / SavedStateHandle / viewModelScope**
- 专业：ViewModel 在配置变更（旋转）后存活；`SavedStateHandle` 在进程被系统回收后恢复少量状态；`viewModelScope` 随 ViewModel 清除自动取消协程。
- 通俗：手机转个屏，页面别把填了一半的表单清掉；哪怕 App 被系统杀掉再打开，也该记住刚才填到哪。
- 本仓库：仅 2 个文件出现 `ViewModel`，状态主要靠 `remember { mutableStateOf }`（275 处）。

**MVI / UDF / Reducer / State / Intent / Effect（单向数据流全家桶）**
- 专业：MVI 把 UI 表达为不可变 `State`，用户动作是 `Intent`，纯函数 `Reducer` 由 `(State, Intent)` 算出新 State，一次性副作用叫 `Effect`；UDF（Unidirectional Data Flow）指数据只沿一个方向流。
- 通俗：界面像一块只认「当前快照」的公告板；谁想改都得交一张申请单，由同一个柜台按固定规则盖章换一张新快照，不许有人偷偷在板上涂改。
- 本仓库：无统一框架，`AiStreamScreen` 里 Wave 2 落了一个纯 reducer + 4 条单测，可作试点样板。

**`remember` / `mutableStateOf` / `StateFlow` / `SharedFlow`**
- 专业：`remember` 在组合中缓存值；`mutableStateOf` 是可被 Compose 观察的状态容器；`StateFlow` 是「有当前值」的热流、`SharedFlow` 是「无当前值、可重放配置」的事件流。
- 通俗：`remember` 是「这张便签别每次重画都丢」；`mutableStateOf` 是便签上的数字；`StateFlow` 像一直亮着的比分牌，`SharedFlow` 像只响一次的铃。
- 本仓库：`core/database/InMemoryTable` 用 `MutableStateFlow<List<T>>` 暴露全量快照。

**CompositionLocal / `staticCompositionLocalOf`（组合内隐式传值）**
- 专业：让深层 Composable 不必逐层传参就能读到某个值；`staticCompositionLocalOf` 在值不变时跳过重组，适合导航器等稳定对象。
- 通俗：像楼里的广播喇叭，任何房间都能听到，不用挨家挨户敲门通知；`static` 版是「内容不变就不重播」。
- 本仓库：`AppNavigator.kt` 的 `LocalAppNavigator = staticCompositionLocalOf<AppNavigator?> { null }`。

**`:api` / `:impl` 拆分 / DIP（依赖倒置原则）**
- 专业：把模块拆成「只放接口的 `:api`」与「放实现的 `:impl`」，上层只依赖 `:api`；DIP 指高层与低层都依赖抽象而非具体实现。
- 通俗：先定好插座形状（接口），再决定买哪个牌子的电器（实现）；换电器不用改墙里的线。
- 本仓库：无 Gradle 级 api/impl 拆分，`feature/home/.../api` 只是包路径。

**Layer Gate（分层门禁）**
- 专业：静态检查源码 import 与 Gradle 依赖，禁止 feature→feature、向上依赖、component 引 feature 等违规。
- 通俗：车间之间只能「上游往下游送货」，门禁保安检查每辆车的通行证方向。
- 本仓库：`scripts/check-layer-deps.sh`，由 `verify-parity.sh` 调用。

**全局可变状态 / `@Volatile`**
- 专业：`@Volatile` 保证可见性但不保证复合操作原子性；进程级单例上的多个可变字段会被任意线程改写，测试间互相污染。
- 通俗：一块公共白板谁都能擦了重写，`@Volatile` 只保证「改完别人立刻看得见」，不保证「两个人同时写不会打架」。
- 本仓库：`NetworkConfig` 十余个 `@Volatile var`、`PayFeatureFlags.sandboxEnabled = true`、`MockAuthConfig.useMockAuth`。

#### D 组 · 导航与路由

**类型安全路由 / `sealed interface`**
- 专业：用密封类型枚举所有路由并携带类型化参数，编译器穷举 `when` 分支；字符串路由只能靠约定与运行时解析。
- 通俗：路线写成「A 站 / B 站 / C 站」的固定站牌，写错站名编译时就报错；字符串路由像手写纸条，写错只有到站才发现。
- 本仓库：`AppNavigator.kt` 的 `AppRoute` 只覆盖 13 个，其余 90+ 靠 `AppRoutes` / `ProductRouteDispatch` 字符串。

**Deep Link / App Link / assetlinks.json / AASA**
- 专业：Deep Link 用自定义 scheme（`myai://`）唤起页面；App Link 用 `https` 域名并需 `assetlinks.json`（Android）/ AASA（iOS）证明域名归属；未验证的 https 链接会弹选择器。
- 通俗：前者像「喊暗号开门」，谁都能喊；后者像「查过房产证才认这把钥匙」，没验证就不算数。
- 本仓库：`AndroidManifest.xml` 的 `myai://` 与 `app.myai.example`，注释自认「needs assetlinks for verified links」。

**Back Stack（返回栈）/ 多栈 Tab**
- 专业：记录页面访问顺序以支持逐级返回；多栈 Tab 指每个底部 Tab 各自维护一条独立返回栈。
- 通俗：像一摞盘子，后进的先拿；四个 Tab 就是四摞分开的盘子，切回来还停在原来那一层。
- 本仓库：`AppNavigator.popToRootTab() = Unit // reserved`，返回栈尚未实现。

**Voyager / Decompose / Navigation Compose**
- 专业：KMP 生态的导航/生命周期库；Decompose 额外提供组件化生命周期与状态保持，Voyager 偏轻量，Navigation Compose 主要面向 Android。
- 通俗：都是「现成的路线图 + 返回栈管理员」，不用自己从零写调度规则。
- 本仓库：均未引入，导航自研。

**RouteHost / Dispatch 守卫 / 65×2 原生页 / 静态元组**
- 专业：RouteHost 把路由字符串分派到对应 Screen；dispatch 守卫用脚本静态核对「每个原生页都有 route → resolve → render 分支」；65×2 指 iOS 与 OHOS 各 65 个原生页；静态元组指页面由硬编码常量数组描述、无 store/API 数据契约。
- 通俗：RouteHost 是总机接线员；守卫是「查接线表有没有漏插的孔」；静态元组像一批只有门牌、屋里没接水电的样板间——门能打开，但没真正通数据。
- 本仓库：`AppCrossFeatureScreens.kt`、`check-ios-page-dispatch.sh` / `check-harmony-pane-dispatch.sh`、`NativeSecondaryPages.swift` / ArkTS `NativeFeaturePane`；registry 明确「页面可开 ≠ 数据契约接线 ≠ Migrated」。

#### E 组 · 网络与协议

**Ktor / HttpClient / Engine（OkHttp / Darwin / CIO / Curl）**
- 专业：Ktor 是 Kotlin 的异步 HTTP 客户端；`HttpClient` 是入口，插件（plugin）横切请求生命周期；Engine 是真正发网络请求的底层实现（Android 用 OkHttp、iOS 用 Darwin、ohosArm64 用 CIO、Linux/Native 常备 Curl）。
- 通俗：Ktor 是「快递公司」，HttpClient 是前台，插件是「贴单/安检/计时」等附加服务，Engine 是各城市实际跑车的车队。
- 本仓库：`core/network/build.gradle.kts:48-80` 按平台挂不同 engine；`HttpClientDefaults.kt` 装插件。

**Plugin / ContentNegotiation / kotlinx.serialization**
- 专业：Plugin 拦截请求/响应做横切逻辑；ContentNegotiation 按 `Content-Type` 自动序列化/反序列化；kotlinx.serialization 用 `@Serializable` 编译期生成转换器。
- 通俗：前者是「过安检贴标签」，后两者是「自动把包裹里的 JSON 折成 Kotlin 对象、再折回去」，不用手写拆箱清单。
- 本仓库：`HttpClientDefaults.kt` 的 `install(ContentNegotiation) { json(NetworkJson) }`。

**HttpTimeout（connect / request / socket 超时）**
- 专业：`connectTimeoutMillis` 限制建连、`requestTimeoutMillis` 限制整体请求、`socketTimeoutMillis` 限制两次数据包之间的间隔；三者语义不同。
- 通俗：分别是「拨号多久没人接就挂」「这通电话总共最多讲多久」「对方一直不吭声最多忍多久」。
- 本仓库：三者都设成 `NetworkConfig.TIMEOUT_MS = 30_000L`（`HttpClientDefaults.kt`）。

**SSE（Server-Sent Events）/ `text/event-stream` / 增量解码**
- 专业：服务端单向长连推送，响应 `Content-Type: text/event-stream`，按 `data:` 帧持续下发；客户端需按 UTF-8 增量解码字节流，不能等整包。
- 通俗：不是「一次给你整篇文章」，而是「打字机一样一个字一个字往外蹦」，读的人要边收边拼，不能等对方写完。
- 本仓库：`core/network/Sse.kt`、`ApiClient.openSseTextStream`；ohosX64 因 cinterop 只给整包 body 而抛 `SseStreamingUnsupportedException`。

**WebSocket / 换票 / 补投 / 序号去重**
- 专业：全双工长连；换票指用 HTTP 换一张 WS 连接凭证；补投指断线重连后按序号补拉漏掉的消息；序号去重用持久化 seq 防止重复处理。
- 通俗：先去窗口换张门票（换票），进场后如果信号断了，回来报「我听到第 8 条」，把 9、10 补给你（补投），且同一条不重复念两遍（去重）。
- 本仓库：`RealtimeWsTicketApi` / `RealtimeWsSyncApi` / `RealtimeSeqStore` / `AppRealtimeClientImpl`；`RealtimeRuntime.kt` 是生产装配点。

**Envelope（信封）`{code,message,data}` / 业务码 / BFF**
- 专业：所有接口统一外层结构 `code`（业务状态）、`message`、`data`（载荷）；HTTP 200 也可能代表业务失败。BFF（Backend For Frontend）是为前端聚合的后端层。
- 通俗：快递盒里永远有一张「结果回执 + 说明 + 内容」；盒子完好（HTTP 200）不代表事情办成了，得看回执上的章。
- 本仓库：`ApiResponse.kt`、`ApiEnvelope.kt`；`NetworkCodes` 自认「BFF 成功码 0 与遗留 200 两套码表并存」。

**401 静默刷新 / 单飞（single-flight）/ `AuthRefreshingApiClient` / 装饰器模式**
- 专业：access token 过期时由拦截器独占 401 流程：刷新 → 重试原请求；单飞指并发的多个 401 只触发一次刷新；装饰器是在原 `ApiClient` 外包一层、不改其内部实现。
- 通俗：门禁刷卡失效时，别把人直接赶去重新排队登录，而是后台悄悄换张新卡再刷一次；十个人同时失效也只派一个人去换卡。
- 本仓库：`NetworkFacade.setAuthRefreshSupport` / `rawApi()`（refresh 用未装饰 client 防递归）、`feature/auth/AuthTokenRefresh`。

**幂等键 / 重试退避 / 429**
- 专业：幂等键让同一业务请求重复提交只生效一次；退避（常为指数 + 抖动）避免重试风暴；HTTP 429 表示限流，应据此降速。
- 通俗：给每笔订单一个「只认一次的号」，网络卡住重发也不会扣两次钱；重试别一秒连按十次，越等越久再试。
- 本仓库：`NetworkCodes.BUSY = 429` 只有常量，无退避策略；无幂等键。

**证书固定（Certificate Pinning）/ TLS / cleartext**
- 专业：把服务端证书或公钥哈希写进客户端，只接受匹配的证书，抵御中间人；TLS 是加密传输层；cleartext 指未加密的 `http://`。
- 通俗：不只认「有盖章的公文」，还把盖章的纹路拍下来，以后纹路不对一律不收；明文则等于把内容写在明信片上寄。
- 本仓库：`network_security_config.xml` 是 `<base-config cleartextTrafficPermitted="true" />`，Manifest `usesCleartextTraffic="true"`。

**OpenAPI / Swagger / 契约测试（Pact）/ IDL**
- 专业：用机器可读的接口描述（OpenAPI）生成客户端模型与路径常量；契约测试验证「消费方期望」与「提供方实际」一致；IDL 是接口定义语言的统称。
- 通俗：别靠人抄电话号码，让接线员直接照着通讯录自动拨号，并且两边定期互报一次「我记的号码和你的一样吗」。
- 本仓库：契约靠 `notes/backend-contract-matrix.md` 94 条人工维护，`audit-flutter-drift.sh` 文本比对。

**ETag / 条件请求 / cache-then-network / 离线优先 / SSOT**
- 专业：ETag 是资源版本标识，配合 `If-None-Match` 得到 304 省流量；cache-then-network 先渲染本地缓存再拉网络；离线优先把本地存储当主数据源；SSOT（Single Source of Truth）指某个数据只有一处权威来源。
- 通俗：先给顾客看旧菜单垫肚子，同时让厨房出新菜；问一句「菜没换吧？」没换就别重端；账本只认一本。
- 本仓库：`HttpCommunityPostRepository` 直连 HTTP，无缓存层；`ListPage` 归一三种分页 data 形态。

**Repository 模式 / 分页（Paging）**
- 专业：Repository 对上层屏蔽「数据来自网络还是本地」；Paging 把分页加载、占位、重试、末尾状态统一建模。
- 通俗：前台只管「要第 2 页」，仓库自己决定从货架拿还是调货；翻页时的「加载中 / 没有了 / 失败重试」由同一套流程管。
- 本仓库：`ListPage` 只定义数据形态，无统一加载状态组件。

**trace-id / 请求签名 / 防重放**
- 专业：trace-id 贯穿一次请求的所有日志便于串联排查；请求签名用密钥对参数做 MAC；防重放靠时间戳 + nonce 拒绝旧请求。
- 通俗：每张工单一个流水号，从前台到仓库都写同一个号；再盖个「一次性骑缝章」，复印件过期就不认。
- 本仓库：只有 `X-Session-ID`（会话 id，不是链路追踪 id），无签名/防重放。

**Drift / Allowlist（漂移 / 白名单）**
- 专业：Drift 指「实现与基准（Flutter 路由 / 后端契约）之间的差异」；allowlist 是逐条写明理由、允许暂时存在的差异，未登记的差异仍应让门禁失败。
- 通俗：拿设计图对现场，量出哪里多/少了；确实要改的地方得有签字单，没签字的一律算违规。
- 本仓库：`audit-flutter-drift.sh` 36 条书面 allowlist + `notes/route-inventory.md`；Wave 1 注入探针验证「未登记差异仍 exit 1」。

#### F 组 · 存储与数据

**KeyValueStore / SharedPreferences / NSUserDefaults / OH_Preferences**
- 专业：KV 键值存储接口与其三端实现；Android `SharedPreferences`、iOS `NSUserDefaults`、OHOS `OH_Preferences` NDK 都是「按 key 存小数据」的原生能力。
- 通俗：一个只放「名字 → 值」小纸条的抽屉；三个系统各有一个这样的抽屉，形状差不多但锁不一样。
- 本仓库：`core/storage/.../KeyValueStore.kt:3-28` 是唯一契约；OHOS 双架构直接实现在 `OH_Preferences` 上，写 app 沙箱，重启不丢。

**multiplatform-settings**
- 专业：把 SharedPreferences / NSUserDefaults 包成 KMP 统一 API 的第三方库；本仓库只在 `settingsMain`（android+ios）挂它。
- 通俗：给前两个抽屉配了同一把通用钥匙，让公共代码不用区分是哪种抽屉。
- 易混：官方 `1.3.0` 没有 OHOS klib，CPF `-0.3.0` 线又没发 android AAR（404），**没有任何一条线能同时覆盖六个 target**，所以 OHOS 自己实现接口。

**SQLDelight / Room / Migration / Schema / 事务**
- 专业：SQLDelight 由 `.sq` 文件生成类型安全 Kotlin API，Room 是 Android 官方 ORM；Migration 是 schema 版本间的升级脚本；事务把多个写操作绑成原子单元。
- 通俗：带格子的账本（不是纸条抽屉）；改账本格式时要写「旧账怎么搬到新账」的说明；一批账要么全记上要么全不算。
- 本仓库：`core/database` 只有接口 + `InMemoryDatabase`，`DatabaseProvider.setFactory` 是换 driver 的预留缝；`build.gradle.kts` 注释说明 OHOS 无 SQL klib。

**Keystore / Keychain / EncryptedSharedPreferences / SQLCipher**
- 专业：把密钥/凭据放进系统级安全硬件或加密容器，而不是明文 KV；SQLCipher 给 SQLite 整库加密。
- 通俗：纸条抽屉换成带锁的保险柜，钥匙由手机系统保管，App 自己都读不到原始钥匙。
- 本仓库：均未引入，token / refreshToken / sessionId 明文存 KV。

**`allowBackup` / `fullBackupContent` / `dataExtractionRules`**
- 专业：Android 是否允许系统备份应用数据，以及用规则文件指定「哪些文件可以/不可以被备份或设备迁移」。
- 通俗：手机换机时帮你搬家；没写「这箱东西别搬」，保险柜里的纸条也可能被一起搬走。
- 本仓库：`AndroidManifest.xml` 是 `android:allowBackup="true"` 且无规则文件。

#### G 组 · 测试与质量

**测试金字塔（Unit / Instrumentation / UI / E2E / 契约 / 截图）**
- 专业：底层大量快而便宜的单元测试，中层集成/契约测试，顶层少量慢而贵的 UI/端到端测试；截图测试比对渲染像素。
- 通俗：先逐个检查零件（便宜快），再检查部件拼起来（中等），最后才整机试运行（贵慢）；截图测试是「拍张照和标准照比一比」。
- 本仓库：65 个 `*Test.kt` / 675 个 `@Test` **全部是 JVM unit test**；无 `androidTest`、无 Compose UI 测试、无 E2E。

**Kover / JaCoCo / 覆盖率（Coverage）**
- 专业：统计测试执行到的代码行/分支比例；阈值门禁防止覆盖率下滑。注意覆盖率只说明「被执行过」，不说明「断言是否正确」。
- 通俗：量一下「这栋楼有多少房间被巡查过」；但巡查过不等于检查得仔细。
- 本仓库：均未配置。

**detekt / ktlint / Android lint / 静态分析（Static Analysis）**
- 专业：不运行程序，直接扫源码找坏味道、格式问题、Android 特有隐患；与「编译通过」是两件事。
- 通俗：不用启动机器，先看图纸有没有画错线、标注有没有漏；能编译只代表机器能转，不代表图纸合规。
- 本仓库：均无；只有自写的 Kotlin 括号/注释词法扫描 + `check-layer-deps.sh` + 两个 dispatch 守卫。

**MockK / Mockito / Turbine / Robolectric**
- 专业：MockK/Mockito 生成测试替身并验证调用；Turbine 测 `Flow`/`StateFlow` 的发射时序；Robolectric 在 JVM 上模拟 Android 运行时。
- 通俗：不用真请外部单位配合，找个「演员」按剧本答话，并检查「该打的电话打了没、顺序对不对」。
- 本仓库：均未引入，替身全是手写 `Mock*` / `Stub*` / `InMemory*`（`Mock` 词频 358）。

**fake / stub / mock / spy（四种测试替身，最易混）**
- 专业：`stub` 只返回预设值；`fake` 是能跑的简化实现（如内存数据库）；`mock` 能验证交互是否发生；`spy` 包住真对象、只改部分行为。
- 通俗：`stub` 是「照稿念的录音」；`fake` 是「真能用的迷你版」；`mock` 是「会记账、事后查你打没打电话」；`spy` 是「真人旁边站个记录员，只在特定时候替他答」。
- 本仓库：`InMemoryDatabase`、`MockCommunityEngine`、`MockRealtimeWsTransport`、`StubPushBridge` 分别对应不同替身类型。

**Seam Test / 像素门禁 / mse_pct / mad_pct**
- 专业：Seam Test 验证「接缝处」的契约（这里指截图门禁脚本本身能被正确调用）；像素门禁比较 Flutter SoT 与 KMP 截图，`mse_pct`（均方误差百分比）与 `mad_pct`（平均绝对差百分比）低于阈值才通过。
- 通俗：先确认「尺子本身准不准」，再用尺子量两张照片差多少；差得少就放行。
- 本仓库：`scripts/screenshot_diff_gate.py` + `test_screenshot_diff_gate.py` 齐备，但**未接进** `verify-parity.sh`；依赖 numpy/PIL，本机默认 `python3` 缺、`/usr/bin/python3` 可用。

**Gate（门禁）/ False Green（假绿）**
- 专业：门禁是合并/CI 前必须通过的自动检查；假绿指检查逻辑本身有缺陷（如只读旧账本）导致「明明没做完却显示通过」。
- 通俗：出厂检验章；假绿是检验员照着一本早作废的旧清单打勾，货其实没齐。
- 本仓库：Wave 1 修复 `parity_task_audit.py` 只读 `tasks-execution.md`（585 done / 0 open）判 PASS 的假绿，改为默认读唯一执行账本 v2、真实剩余 >0 即 FAIL。

**Ktor Mock Engine**
- 专业：`ktor-client-mock` 提供不发真实网络的 `HttpClient` 引擎，可预设响应，用于测请求构造与解析。
- 通俗：给快递公司装一台「模拟柜台」，不用真出车就能检查单子填得对不对。
- 本仓库：只在 `core/network/build.gradle.kts:87` 的 `androidUnitTest` 使用。

#### H 组 · CI/CD 与发布

**CI / CD / Pipeline / Job / Step / Artifact / Matrix**
- 专业：CI（持续集成）自动构建与测试，CD（持续交付/部署）自动发布；Pipeline 由多个 Job 组成，Job 由多个 Step 组成；Artifact 是构建产物；Matrix 是「多版本 × 多平台」的展开组合。
- 通俗：一条自动流水线：先质检（CI），再打包发货（CD）；流水线分几道工序、每道工序几步，做完留下一个包裹，还能同时按多种规格各做一遍。
- 本仓库：`.github/workflows/ci.yml` 只有 1 个 `android` job + 1 个 `release` job，无 matrix。

**GitHub Actions / Workflow / paths filter**
- 专业：`on:` 定义触发事件与路径过滤；`paths` 决定哪些文件改动才触发；`concurrency` 取消同分支的旧运行。
- 通俗：给流水线装个感应器，只有指定区域动了才开工；同一条线上又来新货就把旧的那批撤掉。
- 本仓库：`ci.yml:4-39`，路径过滤 15 项但不含 `docs/` / `openspec/` / `AGENTS.md`。

**debug APK / release APK / tag（`v*`）/ Release**
- 专业：debug 包用默认调试签名、可调试、未混淆；release 包用正式签名、混淆裁剪；`v*` tag 触发 GitHub Release 资产发布。
- 通俗：debug 是「车间样机」，谁都能装来试；release 是「贴封条的正式货」；打上 `v1.2` 标签才允许摆上货架。
- 本仓库：release job 下载的是 `composeApp-debug-apk` 并直接发到 GitHub Release——**把样机当正式货发**。

**CODEOWNERS / PR Template / commitlint / CHANGELOG**
- 专业：CODEOWNERS 按路径指定审查人；PR 模板规范变更说明；commitlint 校验提交信息格式；CHANGELOG 记录对外可见变更。
- 通俗：谁管的片区写清楚、交单要填固定表格、登记簿按统一格式写、给客户看的「这版改了啥」清单。
- 本仓库：四项均缺。

**Fastlane / TestFlight / Play Console**
- 专业：Fastlane 自动化截图/签名/上传；TestFlight 是 iOS 内测分发；Play Console 是 Android 商店后台。
- 通俗：把「打包、送审、发内测」这一串跑腿活交给机器人。
- 本仓库：均未接入。

**SBOM / Renovate / Dependabot**
- 专业：SBOM 列出组件与许可证；Renovate/Dependabot 自动开依赖升级 PR。
- 通俗：一份「这批货的配料表」，以及「厂家出新型号时自动提醒并寄样品」的订阅服务。
- 本仓库：均缺。

**`publish*BinariesToHarmonyApp`（OHOS 产物投递任务）**
- 专业：把 `libkn.so` + `libkn_api.h` 与 Compose 资源拷进 `harmonyApp/entry/libs/` 与 `rawfile/`，之后才能在 DevEco 里构建。
- 通俗：Kotlin 车间做好的板子要搬到鸿蒙车间插上，鸿蒙那边才能开机；搬之前双方接口名（符号）必须一致。
- 本仓库：`AGENTS.md` 的 OHOS publish contract；`-PharmonyAppPath` 可覆盖输出根。

#### I 组 · 安全与合规

**OWASP MASVS（Mobile Application Security Verification Guide）**
- 专业：OWASP 的移动应用安全验证标准，覆盖存储、加密、认证、网络、平台交互、代码质量与反篡改等控制项。
- 通俗：一份「手机 App 安全体检表」，逐项打勾才知道哪块没做。
- 本仓库：未做对照审计（明文、备份、证书固定、防截屏等多项不满足）。

**PrivacyInfo.xcprivacy（Apple 隐私清单）**
- 专业：iOS 要求声明收集的必要数据类别与所用「需声明原因」的 API，未提供会被 App Store Connect 拒绝。
- 通俗：进商场前先交一张「我会带哪些设备、采集什么」的申报单。
- 本仓库：`iosApp` 下无此文件。

**GDPR / 个保法 / 同意撤回 / 数据可携带**
- 专业：GDPR（欧盟通用数据保护条例）与《个人信息保护法》要求告知、最小必要、可撤回同意、可删除/导出数据。
- 通俗：不能默认替用户答应；用户随时能说「我反悔了」「把我那份数据还我/删掉」。
- 本仓库：有 `createPrivacyConsentStore()` 隐私同意（优点），但无撤回、删除、导出与政策版本化。

**Secrets Scanning / 密钥轮换**
- 专业：在提交/CI 中扫描疑似密钥并阻断；轮换是定期或泄露后更换密钥并使旧密钥失效。
- 通俗：出门前检查口袋里有没有把钥匙串落在外面；钥匙丢了要换锁，不是只把钥匙收回来。
- 本仓库：`.gitignore` 已排除 `local.properties` / `.env` / `*.keystore`（优点），但无扫描与轮换流程。

**Provisioning Profile / TEAM_ID / AASA / assetlinks.json**
- 专业：iOS 用 TEAM_ID + Provisioning Profile 决定「哪个开发者、哪台设备、哪个 App ID 可安装」；AASA / assetlinks.json 是域名归属证明，用于验证 App Link。
- 通俗：前者是「施工许可证 + 允许进场的设备名单」，后者是「证明这个网址真归我」的房产证。
- 本仓库：`iosApp/Configuration/Config.xcconfig` 的 `TEAM_ID=` 为空；App Link 注释自认缺 assetlinks。

**Root / 越狱检测 / 防篡改**
- 专业：检测设备是否被提权、App 签名/完整性是否被改，据此限制敏感功能。
- 通俗：先确认「这台手机没被撬过、这个包裹没被拆封」，再谈能不能办敏感业务。
- 本仓库：均未实现；屏幕防截取在 registry 里也是 `Deferred`。

#### J 组 · 可观测性、性能与 UX

**APM / 崩溃上报 / ANR / SLO / 告警**
- 专业：APM 采集性能与错误；崩溃上报收集堆栈与设备上下文；ANR（Application Not Responding）是主线程卡死；SLO 是服务目标，越线触发告警。
- 通俗：给上线后的 App 装黑匣子 + 体温计，卡死/闪退自动记账，指标超线就打电话叫人。
- 本仓库：全部缺失，日志只有 `println`（34 处）与 Ktor INFO。

**结构化日志 / 分级 / 采样 / 脱敏**
- 专业：日志为可查询的字段化事件；按 level（DEBUG/INFO/WARN/ERROR）过滤；采样控制量；脱敏移除 token/会话 id 等敏感字段。
- 通俗：不再写「出事了」这种随手便签，而是「时间/工号/错误码/流水号」的表格；重要的才记、重复的少记、身份证号必须打码。
- 本仓库：`HttpClientDefaults.kt` 用 `LogLevel.INFO` + `println("[Ktor] $message")`，无脱敏、无 release 关闭。

**Feature Flag / 远程配置 / AB 实验 / 灰度 / 回滚**
- 专业：开关与配置由服务端下发，可对不同用户群生效；AB 实验按分组比较指标；灰度按百分比放量；回滚是关掉开关退回旧行为。
- 通俗：新功能先给 1% 的人试，不好就「啪」一下关掉，不用重新发版；像电灯开关而不是拆墙改线。
- 本仓库：`PayFeatureFlags.sandboxEnabled = true`、`MockAuthConfig.useMockAuth` 都是进程内硬编码 `var`，无下发/分组/审计。

**冷启动 / 首帧 / `reportFullyDrawn()`**
- 专业：冷启动是进程从零创建到首帧可交互；`reportFullyDrawn()` 由 App 主动告知系统「内容真正就绪」，用于精确测量。
- 通俗：不是「App 图标变了」就算打开，而是「用户真的能点第一下」；系统自带秒表，你喊一声「好了」它才停表。
- 本仓库：无量化基线；`RealtimeRuntime` 设计上把首连放进后台 scope 不阻塞冷启动，但没测过。

**Compose 稳定性 / `@Stable` / 重组（Recomposition）**
- 专业：编译器根据类型是否「稳定」（不可变或可可靠通知变化）决定能否跳过重组；不稳定类型会导致不必要的重执行。`@Stable` 是给编译器的承诺。
- 通俗：告诉编译器「这个箱子里的东西没变就别再开箱检查」；没贴这个标签，它每次都得开箱翻一遍。
- 本仓库：无稳定性报告、无 `@Stable` 治理、无重组计数。

**LeakCanary / 内存泄漏**
- 专业：对象已不再使用却仍被引用，GC 无法回收；LeakCanary 自动 dump 堆并给出引用链。
- 通俗：东西扔了但还被一根绳子拴着，垃圾车收不走；工具帮你把那根绳子画出来。
- 本仓库：未接入。

**i18n / l10n / `values-night` / 深色模式**
- 专业：i18n（国际化）是把文案抽成可替换资源、支持多语言与格式；l10n（本地化）是具体语言适配；`values-night` 是 Android 的夜间资源限定符。
- 通俗：别把中文写死在代码里，做成「可换的标签贴」；再准备一套晚上看的配色标签。
- 本仓库：`strings.xml` 只有 `app_name` 一条，无 `values-*` / `values-night` / `*.lproj`。

**WindowSizeClass / 折叠屏 / RTL**
- 专业：WindowSizeClass 把窗口按宽度分级（Compact/Medium/Expanded）驱动布局；RTL 是从右到左语言（阿拉伯语等）的布局方向。
- 通俗：同一套界面要能塞进手机、平板、折叠展开态；还要能左右镜像给从右往左读的用户。
- 本仓库：Manifest 有 `supportsRtl="true"`，但无 `layout-sw600dp`、无 WindowSizeClass、无 RTL 验收。

**无障碍 / TalkBack / VoiceOver / 字体缩放 / 对比度**
- 专业：为控件提供语义标签与操作描述，支持屏幕阅读器、系统字体放大与足够对比度。
- 通俗：界面不只能「看」，还要能「听」和「被放大看」，按钮得说清自己是干什么的。
- 本仓库：约 100 处 `contentDescription` / `semantics`，相对 364 文件 / 6 万行偏少，无全流程验收。

**Design Token / 设计系统**
- 专业：把颜色、字号、间距、圆角等抽成具名常量（token），组件与页面只引用 token，保证一致并可整体换肤。
- 通俗：全楼统一用「1 号红、2 号灰、3 号间距」，不许各房间自己调色；换主题只改色卡。
- 本仓库：`core/design` 有 `DemoColors` 与 `ImmersiveInsets`，但大量 Screen 直接写颜色与 Dp 魔法值。

#### K 组 · 工程治理

**ADR（Architecture Decision Record，架构决策记录）**
- 专业：编号化记录一次架构决策的「背景 / 备选 / 结论 / 后果 / 状态」，便于后来者理解为什么这样设计。
- 通俗：给每个重要决定留一张「当时为什么这么选」的小纸条，免得半年后没人记得、又重吵一遍。
- 本仓库：ADR 0001 / 0002 / 0003 散落在代码注释里，无台账与状态。

**OpenSpec / Change / Proposal**
- 专业：规格驱动的变更管理：先在 `openspec/changes/<name>/` 写 proposal / design / tasks，再 apply，最后 archive。
- 通俗：动工前先交图纸和施工清单，做完归档，别边做边改还查不到依据。
- 本仓库：`openspec/changes/` 有 11 个 change。

**唯一执行账本 / 历史账本**
- 专业：只保留一份驱动执行的任务清单，其余标为历史、仅供查漏；门禁必须读这一份。
- 通俗：全工地只认一张施工进度表，旧表盖章作废但留着查漏；检验员不能拿旧表打勾。
- 本仓库：`tasks-ultra-fine-v2.md` 唯一执行（162 条 `[ ]`），`tasks-execution.md` / `tasks-fine.md` 已标注历史。

**五词状态（Migrated / Degraded / Placeholder / Deferred / Partial）**
- 专业：能力缺口登记表限定只用这五个状态词；`Migrated` 要求三端落地 **且数据契约对齐** 且有链接证据。
- 通俗：分别是「真做好了 / 有壳但降级 / 只有样板 / 明确押后 / 做了一部分」；只有「能打开页面」绝不算第一种。
- 本仓库：`platform-gap-registry.md:3-5` 的 Rule (2A)。

**GAP(R5) / blocker / follow_up**
- 专业：源码里 `// GAP(R5)` 标记「因外部依赖或平台能力缺失而无法闭环」；blocker 写清被什么卡住，follow_up 写清解除后要做什么。
- 通俗：在半成品旁边贴张便利贴：「不是忘了做，是差某某零件；等零件到了照这个步骤装」。
- 本仓库：kt 内 `GAP(R5)` 25 处；registry 要求未完成目标必须保留 blocker + follow_up、禁止判绿。

**页面可开 ≠ 数据契约接线 ≠ Migrated**
- 专业：路由可达（页面能打开）只证明 dispatch 接线；数据契约接线指该页有真实 store/API 读写；只有二者齐备且三端有证据才叫 `Migrated`。
- 通俗：样板间的门能推开，不代表通了水电；通了水电、三套房子都一样，才算真交付。
- 本仓库：`in-scope RoutePath UI (78)` 行标 Partial/Placeholder，正是这个判定的应用。

---

## 1. 能力地图与阶段依赖

| 阶段 | 主题 | 覆盖差距编号 | 前置 |
|---|---|---|---|
| 0 | 构建与质量基建 | 3, 4, 51, 53, 54, 55, 60, 67 | — |
| 1 | 架构、依赖治理与 DI | 1, 2, 5, 6, 7, 8, 9, 10, 11, 12 | 0 |
| 2 | 导航与三端一致性 | 13, 14, 15, 16, 45, 46, 47, 85 | 1 |
| 3 | 网络与数据层 | 17–32 | 1 |
| 4 | 三方能力与 SDK 集成 | 33–44, 75 | 1, 3 |
| 5 | 测试体系 | 48, 52, 56, 57, 58, 59 | 0 |
| 6 | 发布、安全与合规 | 49, 50, 61–66, 68–73 | 5 |
| 7 | 可观测性、性能与 UX | 74, 76–84, 86–88 | 3, 6 |
| 8 | 工程治理与协作 | 89–94 | 全部 |

依赖主线：`0 → 1 → (2 ∥ 3) → 4 → 5 → 6 → 7 → 8`。阶段 2 与 3 可并行（文件集基本不重叠）；阶段 4 的 SDK 任务依赖阶段 3 的网络/Repository 契约。

---

## 2. 分阶段计划

### 阶段 0 · 构建与质量基建

**通俗讲解**：这个阶段是「先把工地的水电、质检台和账本立起来，再谈盖楼」。现在项目能编译、能跑 675 个测试，但检验员照的是一张自写的清单、没有量尺（覆盖率）、没有图纸审查（静态分析）、零件清单也没封条（依赖锁定）。这时候直接改架构，出了错分不清是「改动引入的」还是「本来就没查出来」。所以先补验收能力，让后面每一步都能被客观验证。

**专业讲解**：目标是建立可重复、可度量、可归因的构建与质量基线。当前 `verify-parity.sh --static-only` 只做 Kotlin 词法扫描与依赖方向检查；缺 detekt/ktlint、Kover、dependency verification、lockfile 与 CI 分层步骤。同时根工程 `subprojects {}` 承担了 30 个模块的共享接线，配置阶段求值范围过大，是窄构建仍需全工程配置、并需要 advisory lock 串行化的根因。先固化「门禁语义」（例如 `parity_task_audit.py` 必须读唯一执行账本且真实剩余 >0 即 FAIL），再动架构，才能把回归归因到具体改动。

**示例讲解（`api()` 为什么会让所有人跟着重编译）**：

```kotlin
// 现状：feature/mine/build.gradle.kts:13（真实代码）
api(projects.core.ui)      // 注释自认「传递带出 core.platform -> core.design / core.account」
```

- 第 1 行 `api(...)`：专业＝把 `core.ui` 放进本模块对外暴露的编译类路径，下游模块（含 `composeApp`）无需声明即可「看见」`core.ui` 及其传递依赖；通俗＝你不但借了工具，还顺手把工具箱钥匙也配给了所有邻居。
- 注释那句「传递带出」：专业＝`core.ui` 自身 `api(projects.core.platform)`，`core.platform` 又 `api(projects.core.design)`，形成一条泄漏链；通俗＝A 借了 B 的工具箱，B 的箱子里又装着 C 的工具，结果连 C 都跟着进了公共区。
- 后果（专业）：任一下游模块编译都携带整条链；链上任一模块改动会触发更大范围的重编译，且接口变更可能静默破坏下游。后果（通俗）：你换个锁，整条街都得换锁，而且没人提前知道自己也配过钥匙。

**改完长这样（示意，非仓库现状）**：

```kotlin
// 目标写法
implementation(projects.core.ui)      // 只在本模块可见
implementation(projects.core.design)  // 自己用到的，自己显式声明
```

- 专业：`implementation` 把依赖藏在本模块编译类路径内，下游必须显式声明自己用到的部分，增量编译范围收敛；代价是各模块要把缺失依赖补齐，这一步会「暴露」原先靠传递拿到的东西。
- 通俗：工具只放自己屋里，谁要用自己登记领一把；麻烦是第一次整理时会发现「原来我一直白用着别人的三样东西」，得逐样补登记——这正是练习的价值。

**再举一个「假绿」的例子**（`scripts/parity_task_audit.py`，Wave 1 修复后的语义）：

```python
MAIN_LEDGER = "openspec/changes/sync-flutter-full-parity/tasks-ultra-fine-v2.md"  # 唯一执行账本
# 扫描 [ ] / [~]，真实剩余 > 0 即 sys.exit(1)
```

- 专业：门禁的判定输入必须与「唯一执行账本」同源，且未完成的判定不能靠 `[x]` 计数推断；Wave 1 前它只读 `tasks-execution.md`（585 done / 0 open）而直接 PASS。
- 通俗：检验员以前翻的是一本早作废的旧账，账上全打勾就放行；现在强制翻总账，只要还有空格就不许盖章。

**学什么**：Gradle KMP 构建模型、Version Catalog、composite build / convention plugin、配置缓存与构建缓存、依赖锁定与 verification、静态分析、覆盖率、CI 门禁分层、`subprojects {}` 的求值范围与副作用。

**读哪些代码**：
- `gradle/libs.versions.toml`（44 个 library 别名、7 个 plugin，其中 12 个别名与 `cocoapods` 声明未用）
- `settings.gradle.kts:23-44`（30 个 include）、`build.gradle.kts:35-129`（共享接线）、`component/build.gradle.kts:19-74`（不在命中范围、重复声明）
- `composeApp/build.gradle.kts:29-190`（target 集 / source set / `-PandroidOnly`）
- `scripts/verify-parity.sh`、`scripts/with-gradle-lock.sh`、`.github/workflows/ci.yml`、`gradle.properties`
- `composeApp/src/commonMain/kotlin/com.example.my_kmp_project/MODULES.md`

**动手任务**
1. `[#3][#4]` 画出 30 个模块的真实依赖图，与 `MODULES.md` 的 28 库模块口径对齐，产出一份「include 清单 → 职责 → 依赖方向」对照表；评估把 `subprojects {}` 抽成 convention plugin 的可行路径（含本机镜像约束的书面结论）。
2. `[#51]` 记录窄构建会配置全部 30 个工程的成因与影响；评估开启 `kotlin.native.cacheKind` 缓存的收益与风险，保留 `with-gradle-lock.sh` 的 advisory lock 作为过渡。
3. `[#53]` 引入 Kover（或等价覆盖率工具），先测出 675 个 `@Test` 的基线覆盖率，**只报数不设阈值**，避免一开始就假绿。
4. `[#54]` 引入 detekt / ktlint + `.editorconfig`，接到 `verify-parity.sh --static-only`；现有 Kotlin 括号/注释词法扫描与 layer gate 保留。
5. `[#55]` 加 `gradle.lockfile` 或 dependency verification metadata、Renovate/Dependabot 配置、依赖许可证清单；清理 12 个未引用别名与未启用的 `cocoapods` plugin（或写明保留理由）。
6. `[#60][#67]` CI 拆出 lint / detekt / 覆盖率三个独立步骤；修正路径过滤，让 `AGENTS.md` / `docs/` / `openspec/` 的变更也能触发文档校验（或明确写出「不触发」的书面理由）。

**验收**：`./gradlew testDebugUnitTest :composeApp:assembleDebug -PandroidOnly=true` 全绿；`verify-parity.sh --static-only`、`check-layer-deps.sh`、`audit-flutter-drift.sh` 全绿；CI 新增步骤可见且可重复执行；覆盖率报告产出。

**自检**：为什么 `buildSrc` 在这台机器上不可用？`api` 与 `implementation` 在 Gradle 依赖传播上的差别是什么？配置缓存为什么要求 `deployment.gradle.kts` 改用 `ExecOperations`？

---

### 阶段 1 · 架构、依赖治理与 DI

**通俗讲解**：这一阶段管的是「东西由谁造、能用多久、状态放哪」。现在项目像一栋楼：配电箱（`AppContainer`）是手搓的、所有房间共用一块白板（`NetworkConfig` 的全局 `var`）、表单填到一半转个屏就没了（状态只在 `remember` 里）、返回键还没接好线（`popToRootTab()` 是空的）。人少的时候能凑合，人一多就会出现「A 改了白板，B 的测试挂了」「页面返回行为各写各的」。这一步就是把这些约定变成代码里的固定规则。

**专业讲解**：把「对象图构造」从业务代码收拢到 Composition Root，并用 DI 容器的 Scope 表达生命周期边界（app / login / screen）；把全局可变配置收敛为不可变配置 + 单一写入 owner，消除并行测试的共享状态污染；引入 ViewModel + SavedStateHandle 覆盖配置变更与进程重建，用 `viewModelScope` 保证结构化取消；用 MVI 的不可变 State + 纯 Reducer 把状态迁移变成可单测的纯函数；`explicitApi()` 与 binary compatibility validator 保护库模块公开 ABI。

**示例讲解（组合根与作用域）**：

```kotlin
// 现状：composeApp/.../app/AppContainer.kt:18-27（真实代码，节选）
internal class AppContainer(
    val network: NetworkComponent = NetworkComponent(),
    val session: SessionComponent = SessionComponent(),
    val bridges: BridgeComponent = BridgeComponent(),
    val navigation: NavigationComponent = NavigationComponent(),
) {
    companion object {
        private val instance: AppContainer by lazy { AppContainer() }
        fun get(): AppContainer = instance
    }
}
```

- 第 2–5 行：专业＝构造注入式的分组依赖，四个子组件各自封装一类能力；通俗＝配电箱里分了四个闸，照明、插座、空调分开管，不是一个总闸糊在一起。
- 第 7 行 `by lazy`：专业＝进程级单例、首次访问时构造、无显式生命周期边界；通俗＝全楼只有一个总配电箱，从开工到拆楼一直在，没有「住客退租就把他那一路的电断掉」这回事。
- 第 8 行 `get()`：专业＝这是 Service Locator 味道的全局访问点，任何代码都能拿到容器，依赖方向不可静态追踪；通俗＝谁都能跑去配电箱自己拉一根线，久了就没人知道哪根线通向哪。

**换成 Scope 之后长这样（示意，非仓库现状）**：

```kotlin
val appModule = module {
    single<ApiClient> { NetworkFacade.api() }          // 全进程一个
    scope<LoginScope> {
        scoped { SessionRepository(get()) }            // 登录期间才存在
    }
}
```

- 专业：`single` 绑定 app 级生命周期，`scope`/`scoped` 让依赖随登录态创建与销毁；`get()` 是容器内解析，不是全局静态取用。
- 通俗：`single` 是「楼里永远开着的总灯」，`scope<LoginScope>` 是「刷房卡才通电、退房自动断电」的客房灯；用户一登出，那间屋的东西自动清掉，不会残留到下一个账号。

**再看一个状态恢复的对照（示意）**：

```kotlin
class FeedViewModel(private val saved: SavedStateHandle) : ViewModel() {
    val keyword: StateFlow<String> = saved.getStateFlow("keyword", "")
    fun onKeywordChange(v: String) { saved["keyword"] = v }
}
```

- 专业：`SavedStateHandle` 在进程被系统回收后由系统重建并恢复键值；`StateFlow` 提供可被 Compose 观察的当前值；`viewModelScope`（未画出）随 ViewModel 清除自动取消协程。
- 通俗：把「用户搜了什么」写在一张不怕手机重启的便签上；转屏、切后台再回来，输入框还留着原来的字，而不是一片空白。

**学什么**：`api` vs `implementation`、`:api`/`:impl` 模块拆分、依赖倒置、DI 容器与作用域（Koin）、全局可变状态的危害、ViewModel / SavedStateHandle / 结构化协程取消、MVI 单向数据流。

**读哪些代码**：
- 任一 `feature/*/build.gradle.kts`（如 `feature/mine/build.gradle.kts:13-17`、`feature/home/build.gradle.kts:27-30`）的 `api(...)` 用法
- `composeApp/.../app/AppContainer.kt:14-57`、`composeApp/src/commonMain/kotlin/com.example.my_kmp_project/App.kt:10`（两处「migrate to Koin later」注释）
- `core/network/.../NetworkConfig.kt`（十余个 `@Volatile var`）、`feature/auth/.../MockAuthConfig.kt`、`component/pay/PayGateway.kt` 的 `PayFeatureFlags`
- `core/router/.../AppNavigator.kt`（`popToRootTab() = Unit // reserved`）
- `scripts/check-layer-deps.sh`

**动手任务**
1. `[#1]` 把 17 个 feature 的 `api(...)` 逐个改为 `implementation(...)`，逐个跑 `./gradlew :feature:x:testDebugUnitTest`；把因传递依赖断开而暴露的缺失依赖显式补到各模块，记录「泄漏清单」。
2. `[#2][#5]` 选 `:feature:pay` 做 `:api` / `:impl` 拆分试点，定义契约模块并让 `composeApp` 只依赖 `:api`；补依赖所有权说明。
3. `[#6][#93]` 试点启用 `explicitApi()`，评估 binary compatibility validator 对公开 API 的保护；先在 1 个模块落地，不一次性铺开。
4. `[#7][#8]` 把 `AppContainer` 迁到 Koin（兑现 ADR 0001 / Q15=D），定义 `appScope` / `loginScope` / `screenScope` 三级作用域，写登录/登出时依赖释放的测试。
5. `[#9]` 把 `NetworkConfig` 的全局可变 var 收敛为不可变配置 + 明确 owner（会话部分仍由 `AccountFacade` 单点写入），补并发写与测试隔离用例；`PayFeatureFlags` / `MockAuthConfig` 的开关迁到阶段 4 的配置中心前先加读写约束。
6. `[#10]` 选 1 个 Screen 引入 ViewModel + `SavedStateHandle`，补「配置变更 / 进程重建后状态恢复」的回归测试。
7. `[#11]` 实现 `AppNavigator.popToRootTab()`，补返回栈、结果回传与多栈 Tab 的最小实现。
8. `[#12]` 选 1 个模块落地 MVI（State / Intent / Effect + 纯 reducer + 单测），作为后续模块的参考实现。

**验收**：`check-layer-deps.sh` 绿；17 个 feature 均可独立编译；Koin 作用域测试通过；状态恢复测试通过；`AppNavigator` 有返回栈测试。

**自检**：为什么 `api(...)` 会让编译耦合扩散？ViewModel 状态与 `remember { mutableStateOf }` 在配置变更时的行为差异？为什么全局 `@Volatile var` 会让并行单测互相污染？

---

### 阶段 2 · 导航与三端一致性

**通俗讲解**：这一阶段管的是「怎么找到房间、房间里的水电有没有真的接上」。现在路线图有三份：Kotlin 一份、iPhone 的 Swift 一份、鸿蒙的 ArkTS 一份，各写各的；而且 65 个原生页只有门牌和样板布置，点进去没接真实数据。更麻烦的是「返回」这个最基础的动作还没实现。这一步要做的是：把路线图统一、把返回栈补上、并挑几个样板间真正通上水电。

**专业讲解**：把字符串路由迁移到 `sealed interface` 类型安全路由，靠编译器穷举消除拼写与参数解析错误；实现 back stack / 多栈 Tab / 结果回传；为 `myai://` 自定义 scheme 与 `https` App Link 补参数白名单与 `assetlinks.json`/AASA 归属验证；把 iOS SwiftUI 与 ArkTS 的 65×2 静态元组页从「dispatch 文本接线」升级为「路由 → 数据契约」校验；整理 OHOS 双架构（ohosArm64 / ohosX64）的 klib 可用性矩阵，明确哪些能力是有意降级而非遗漏。

**示例讲解（类型安全路由 vs 字符串路由）**：

```kotlin
// 现状：core/router/.../AppNavigator.kt（真实代码，节选）
public sealed interface AppRoute {
    data object Splash : AppRoute
    data class InAppWeb(val url: String) : AppRoute
}
public class AppNavigator(private val onNavigate: (AppRoute) -> Unit) {
    fun navigate(route: AppRoute) = onNavigate(route)
    fun popToRootTab() = Unit // reserved; shell clears overlay
}
```

- 第 2–3 行 `sealed interface` + `data class`：专业＝所有合法路由由类型系统穷举，`InAppWeb` 的参数是类型化的 `String`，`when` 分支缺失会编译报错；通俗＝站牌是固定铸出来的，写错站名当场就过不了关，而且「网页页」这张牌上明确刻了「要带网址」。
- 第 5 行 `onNavigate: (AppRoute) -> Unit`：专业＝导航行为以函数注入，容器不持有 UI 实现；通俗＝接线员只负责把单子递出去，具体谁开门由别人决定。
- 第 6 行 `popToRootTab() = Unit`：专业＝返回栈是空实现，「回到根 Tab」无语义，注释 `reserved` 明确是占位；通俗＝按下「回家」按钮，电梯其实没动，只是贴了张「以后再装」的纸条——这是最直观的「能打开页面 ≠ 导航完整」。

**字符串路由为什么会出事（示意对照）**：

```kotlin
// 字符串写法（示意）
navigate("/web?url=" + url)        // 拼错、少个斜杠、参数没编码，都只有运行时才发现
// 类型安全写法（示意）
navigate(AppRoute.InAppWeb(url))   // 参数类型、必填性由编译器保证
```

- 专业：字符串方案把「路由语法 + 参数编码 + 合法性」推迟到运行时，且无集中解析点；类型安全方案把契约前移到编译期，并可配合 `expect`/`actual` 在各端统一分派。
- 通俗：前者是手写字条「去三楼靠左那间」，字写歪了到现场才发现；后者是刷一张印好的门禁卡，卡面字段填不全就出不了卡。

**再看「样板间」的判定（真实证据）**：`iosApp/iosApp/NativeSecondaryPages.swift` 与 ArkTS `NativeFeaturePane` 的 65 个页多为 15–43 行硬编码 `@State`、无 store/API；`check-ios-page-dispatch.sh` / `check-harmony-pane-dispatch.sh` 只验证「有 route → 有 render 分支」。

- 专业：dispatch 守卫证明的是「路由可达」，不能推出「数据契约接线」；registry 的 Rule (2A) 因此规定只有三端落地且数据契约对齐、有链接证据才可标 `Migrated`。
- 通俗：保安查的是「每间房都贴了门牌、门能推开」，不查「屋里通没通水电」；所以门牌齐全不等于可以入住。

**学什么**：类型安全路由、deeplink / App Link 校验、KMP 与原生壳的数据契约、expect/actual 边界设计、OHOS klib 生态与双架构降级。

**读哪些代码**：`core/router/`（`AppRoute`、`AppRoutes`、`ProductRouteDispatch`、`DeepLinkRouter`、`AppNavigator`）、`composeApp/.../app/AppCrossFeatureScreens.kt`、`composeApp/src/androidMain/AndroidManifest.xml`、`iosApp/iosApp/NativeSecondaryPages.swift` 与 `NativeFeatureCatalog.swift`、`harmonyApp/.../pages/Index.ets`、`core/platform/src/ohosMain/` 各 actual、`scripts/check-ios-page-dispatch.sh` / `check-harmony-pane-dispatch.sh`。

**动手任务**
1. `[#13][#14]` 把字符串路由表迁移到 sealed 类型安全路由，补参数解析与未知路由回退测试；评估引入 Voyager / Decompose 的成本并给出书面结论。
2. `[#16]` 为 `myai://` scheme 与 `app.myai.example` App Link 补参数白名单与来源校验；写 assetlinks / AASA 的接入说明。
3. `[#15][#47]` 为 65×2 原生页设计「路由 → 数据契约」表；先选 3 个页（iOS Swift 1 个、ArkTS 1 个、Compose 1 个）从静态元组切到 store/API 接线，保留剩余页的 gap 登记。
4. `[#45][#46]` 整理 OHOS 双架构能力矩阵（Ktor / Coil / uuid / SSE / WebView / 相机 / MediaKit），把 `Degraded` stub 的「接口齐备、实现待平台能力」写清楚；把两个 dispatch 守卫从文本接线升级为契约校验（先加 1 条规则试点）。
5. `[#85]` 把三端沉浸式（Android `enableEdgeToEdge`、iOS `ignoresSafeArea`、OHOS ArkTS）整理成一张对照表与验收清单，补不一致项的修正。

**验收**：路由参数测试、deeplink 白名单测试、3 个原生页的数据契约接线、三端沉浸式对照表、`check-*-dispatch.sh` 仍绿。

**自检**：为什么「页面可开 ≠ 数据契约接线 ≠ Migrated」？三端各写一份路由表会带来哪些长期维护成本？

---

### 阶段 3 · 网络与数据层

**通俗讲解**：这一阶段管的是「怎么跟后端说话、东西存哪、存得安不安全」。现在拨号、通话、等对方吭声都统一忍 30 秒；通话内容用大喇叭 `println` 念出来，可能连门禁卡号一起念；柜子没锁（token 明文）；账本还是草稿纸（内存数据库，关掉就没）。这一步要做的就是把这些「能用但不够稳」的约定，换成按场景分级、有重试、有缓存、有锁、能落盘的一套规则。

**专业讲解**：在 Ktor 插件层把 connect / request / socket 三类超时拆开，加指数退避重试与幂等键，按 429/5xx/网络异常分类；用结构化日志替换 `println` 并脱敏 `Authorization` / `X-Session-ID` / `X-Device-ID`；收紧 `network_security_config.xml`，仅 debug 与本地回环允许明文、补证书固定；用 OpenAPI 生成契约模型替代手工矩阵；统一 `NetworkCodes` 双码表；落地真实 DB driver 与 schema 迁移、事务与并发安全；凭据迁到 Keystore/Keychain；以 Repository + 单一数据源收敛 feature 的直连调用。

**示例讲解（一个 HTTP 客户端里到底装了什么）**：

```kotlin
// 现状：core/network/src/networkKtorMain/.../HttpClientDefaults.kt（真实代码，节选）
public fun HttpClientConfig<*>.installDemoDefaults() {
    install(ContentNegotiation) { json(NetworkJson) }
    install(HttpTimeout) {
        connectTimeoutMillis = NetworkConfig.TIMEOUT_MS
        requestTimeoutMillis = NetworkConfig.TIMEOUT_MS
        socketTimeoutMillis = NetworkConfig.TIMEOUT_MS
    }
    install(Logging) {
        level = LogLevel.INFO
        logger = object : Logger {
            override fun log(message: String) { println("[Ktor] $message") }
        }
    }
    install(DemoDynamicHeaders)
    defaultRequest {
        url(NetworkConfig.effectiveBaseUrl().trimEnd('/') + "/")
        contentType(ContentType.Application.Json)
    }
}
```

- 第 2 行 `ContentNegotiation`：专业＝按 `Content-Type` 自动把 JSON 反序列化成 `@Serializable` 对象；通俗＝收货时自动按固定格式拆箱，不用每件都手写拆箱单。
- 第 3–7 行 `HttpTimeout`：专业＝三个值语义不同（建连 / 整体请求 / 包间静默）；这里全部指向同一个 `TIMEOUT_MS = 30_000L`，等于放弃区分；通俗＝「拨号多久没人接」「这通电话总共讲多久」「对方一直不吭声忍多久」本来是三件事，现在都写 30 秒。
- 第 8–13 行 `Logging`：专业＝`LogLevel.INFO` 会输出请求行等信息，`println` 无级别、无采样、无脱敏、release 不关闭；通俗＝柜台用大喇叭把每单内容念出来，可能连「工号 + 会话号」一起念，而且下班了也不关喇叭。
- 第 14 行 `DemoDynamicHeaders`：专业＝自研插件在 `onRequest` 里调 `DemoRequestHeaders.forUrl(url)` 逐请求注入头，与 `Authorization` 同一 gating；通俗＝每辆车出门前由同一个人贴单子，贴哪些单子按目的地决定，没登录就不贴身份单。
- 第 15–18 行 `defaultRequest`：专业＝统一基址与 JSON 内容类型；通俗＝所有包裹都从同一个发货地址、用同一种箱子规格发出。

**改进后长这样（示意，非仓库现状）**：

```kotlin
install(HttpTimeout) {
    connectTimeoutMillis = 10_000L      // 建连
    requestTimeoutMillis = 15_000L      // 普通接口
    socketTimeoutMillis  = 60_000L      // 流式 / 大响应
}
install(Logging) {
    level = if (BuildConfig.DEBUG) LogLevel.INFO else LogLevel.NONE
    logger = object : Logger {
        override fun log(message: String) = AppLog.d("HTTP", redact(message))
    }
}
```

- 专业：超时按接口类别分层；日志级别随构建类型切换，`redact()` 移除 token / sessionId / deviceId 后再交给结构化日志；`AppLog` 可带 trace-id 贯穿。注意本仓库 `buildConfig = false`，示意里的 `BuildConfig.DEBUG` 在落地时要换成注入的 `buildMode`（`NetworkConfig.buildMode` 已有）或先开启 buildConfig——这正是阶段 6 任务 2 的联动点。
- 通俗：拨号 10 秒没人接就重拨、普通快递 15 分钟没回音就催、直播流允许慢慢喂；大喇叭换成带编号的登记本，且身份证号一律打码，下班就收摊。

**再看「信封」为什么不能只看 HTTP 200**（`NetworkCodes.kt` / `ApiResponse.kt`，真实代码语义）：

```kotlin
public data class ApiResponse<T>(val code: Int, val message: String?, val data: T?, ...)
val isSuccess: Boolean get() = code == NetworkCodes.BFF_OK || code == NetworkCodes.OK
```

- 专业：BFF 用 `code == 0` 表示业务成功，遗留 iOS 码表用 `200`，且 `0` 在遗留表里还表示「无网络」；解析器在缺 `code` 时回退 `PASSTHROUGH = -1`，避免畸形响应被误判成功。
- 通俗：盒子完好（HTTP 200）不代表事情办成，得看回执上的章；更麻烦的是同一个数字「0」在两本旧手册里一个代表「办成了」、一个代表「没网」，所以缺章时干脆记成「看不懂」，不能瞎猜成功。

**学什么**：Ktor 插件体系与自定义插件、超时分层、重试/退避与幂等、缓存与离线优先、日志脱敏、TLS / 证书固定、OpenAPI 契约生成与契约测试、SQLDelight/Room 迁移、加密存储、Repository 单一数据源、分页状态。

**读哪些代码**：`core/network/.../`（`HttpClientDefaults.kt`、`NetworkConfig.kt`、`NetworkCodes.kt`、`ApiResponse.kt`、`ApiClient.kt`、`ListPage.kt`、`DemoApiHosts.kt`、`DemoRequestHeaders.kt`、`Sse.kt`、`TokenRefresh.kt`）、`core/storage/.../KeyValueStore.kt` 与三端 actual、`core/database/.../`（`Database.kt`、`InMemoryDatabase.kt`、`DatabaseProvider.kt`、`build.gradle.kts`）、`feature/community/.../http/HttpCommunityPostRepository.kt`、`notes/backend-contract-matrix.md`、`notes/route-inventory.md`。

**动手任务**
1. `[#17]` 加指数退避重试插件（区分 429 / 5xx / 网络异常）、幂等键；补超时、重试、取消传播的用例。
2. `[#18]` 把 `NetworkConfig.TIMEOUT_MS = 30_000L` 拆成 connect / 普通请求 / 流式三类超时，按接口分类注入。
3. `[#19]` 用结构化日志框架替换 34 处 `println` 与 Ktor `LogLevel.INFO`，实现分级、脱敏（`Authorization` / `X-Session-ID` / `X-Device-ID`）、release 关闭。
4. `[#20][#68]` 收紧 `network_security_config.xml`：仅 debug 允许明文、按域名白名单、补证书固定；Manifest `usesCleartextTraffic` 收敛。
5. `[#21][#22]` 统一 `NetworkCodes` 双码表（BFF `0` 与遗留 `200`），补畸形响应 / 429 / 403 的分类策略与回归测试；扩 `NetworkError` 的用户可恢复策略。
6. `[#23]` 用后端路由生成 OpenAPI / 契约模型，替换手工维护的路径常量；`notes/backend-contract-matrix.md` 保留为审计记录；补契约测试（Pact 或等价 schema 校验）。
7. `[#24]` `clientType` / `clientVersion` / `clientBuildNumber` 从构建信息注入，去掉写死默认值；补请求签名/防重放的最小实现。
8. `[#25]` 落地 3-flavor 环境系统（Dev / Staging / Product）与构建期注入，替换 `DemoApiHosts` 三个环境同指 `127.0.0.1:8080` 的现状；保留运行时 override 供真机联调。
9. `[#26]` 在 `ListPage` 之上加统一分页状态（Loading / Empty / Error / Retry）组件，先接 1 个列表页。
10. `[#27][#28]` 落地真实 DB driver（先 Android/iOS SQLDelight，OHOS 保持 `InMemoryDatabase` 并登记），带 schema 版本、迁移测试、事务与并发安全（`InMemoryDatabase` 注释自认写路径非线程安全）。
11. `[#29][#30]` token / refreshToken 迁到 Keystore / Keychain 或加密 KV；`KeyValueStore` 评估补命名空间与过期策略（注意 OHOS API 23+ 无 `contains()` / `clear()` 等价物的约束）。
12. `[#31]` 离线队列与业务文件存储抽象：把 `RealtimeOutboundQueue` 的 `InMemoryRealtimeOutboundStore` 换成可持久化实现。
13. `[#32]` 以 `HttpCommunityPostRepository` 为样板，为其余 feature 定义统一 Repository 契约（cache-then-network / 单一数据源 / 冲突解决），先迁 2 个模块。

**验收**：`audit-flutter-drift.sh` 仍绿；契约测试、迁移测试、加密存储用例、超时/重试用例全绿；22 条 `missing(待接线)` 中至少 1 条转为已接线并同步矩阵。

**自检**：为什么 `0` 同值不同义要靠 `PASSTHROUGH=-1` 兜底？证书固定与 `cleartextTrafficPermitted` 在生产包里的正确组合？为什么 OHOS 不能强引 SQL klib？

---

### 阶段 4 · 三方能力与 SDK 集成

**通俗讲解**：这一阶段管的是「哪些活要请外援、外援没到之前怎么不把话说满」。推送、支付、聊天、微信登录这些都要外部公司的 SDK 和账号，本机没有凭据，硬写只会变成「假装成功」。正确做法是先把插座形状定好（接口）、把「没插设备时怎么表现」写清楚（降级）、用测试证明插座没接错（契约测试）、再把差什么零件写进台账（gap）。同时把现在「默认开的沙盒支付」「debug 默认走假登录」这类开关收进一个能下发、能关掉的配置中心。

**专业讲解**：用 `expect`/`actual` 定义平台能力端口，把厂商 SDK 隔离在平台源集；为支付加订单幂等键与验单防重放，并消除 `SandboxPayChannelAdapter` 默认启用带来的「假 Success」语义；把 `core/analytics` 从单行占位扩成事件模型 + 上报端口 + 采样/批处理；把 `PayFeatureFlags` / `MockAuthConfig` 的进程内 `var` 迁到可下发配置（本地默认 + 分组 + 变更审计）；所有需凭据的能力保持「接口 + 契约测试 + 降级 + gap 登记」四件套，registry 状态与 follow_up 同步。

**示例讲解（接口先行：一个还没接 SDK 的端口）**：

```kotlin
// commonMain（示意，非仓库现状）
public interface PushTokenProvider {
    suspend fun fetchToken(): String?
}
public expect fun createPushTokenProvider(): PushTokenProvider

// androidMain（示意，真 SDK 到位前）
public actual fun createPushTokenProvider(): PushTokenProvider =
    object : PushTokenProvider {
        override suspend fun fetchToken(): String? = null   // 诚实返回「拿不到」
    }
```

- 第 2–4 行：专业＝`commonMain` 只声明契约与工厂签名，业务层依赖抽象而非厂商类；通俗＝先把墙上的插座孔位和尺寸定好，至于买哪个牌子的电器以后再说。
- 第 6 行 `expect`：专业＝编译器要求每个目标平台各给一个 `actual`，缺一个就编译失败；通俗＝开了张欠条，三个厨房都必须签收，少一张就开不了工。
- 第 9–11 行返回 `null`：专业＝降级行为是显式的类型化结果（`String?`），调用方必须处理「无 token」分支，不会出现静默假成功；通俗＝没装设备就老实说「这台没有」，而不是伪造一个编号交差。这与仓库里 `PlatformPushSdk.fetchRegistrationToken(): String? = null` 是同一套诚实策略（见 `PushBridge.kt` 的 `TODO(Spike II)`）。

**再看一个「不该默认打开」的开关**（`component/src/commonMain/.../component/pay/PayGateway.kt:48-51`，真实代码）：

```kotlin
public object PayFeatureFlags {
    var sandboxEnabled: Boolean = true
}
```

- 专业：全局可变 `var` 且默认 `true`，意味着任何构建（含 release）在未显式关闭时都会走 `SandboxPayChannelAdapter`；该 adapter 是仿真通道，registry 已标注需复核「假 Success」。同时它是进程级状态，测试间会互相污染。
- 通俗：一台「演练用的收银机」默认就摆在大门口且开着，真顾客来结账也可能被当成演练；更麻烦的是谁都能随手拨它后面的开关，别人测一半被你拨乱了。
- 收敛方向（示意）：默认 `false`、由配置中心下发、release 包加启动期断言、每次变更写审计日志；专业上这叫「安全默认值（secure by default）+ 可观测的配置变更」，通俗上就是「演练机平时锁在库房，要用得登记领钥匙，且正式营业时强制检查它没被推出来」。

**「四件套」逐项对照**：

| 组成 | 专业 | 通俗 |
|---|---|---|
| 接口 | `expect`/`actual` 端口，隔离厂商类型 | 先把插座孔位定好 |
| 契约测试 | 用 fake 验证端口的输入输出与错误分支 | 先拿模型插头试一下孔位对不对 |
| 降级 | 类型化「不可用」结果，不抛未处理异常、不伪造成功 | 没装设备就说没有，不假装办成了 |
| gap 登记 | registry 写 blocker + follow_up，状态保持 `Deferred` | 便利贴写明「差某某零件，到了照步骤装」 |

**学什么**：厂商 SDK 的 expect/actual 桥接、平台权限模型、支付幂等与验单、IM 会话同步、埋点事件模型与采样、远程配置 / Feature Flag / AB 实验、降级与开关审计。

**读哪些代码**：`component/src/commonMain/.../component/push/`（`PushBridge.kt` 的 TODO、`PlatformPushSdk.*.kt` 三端返回 null）、`component/src/commonMain/.../component/pay/PayGateway.kt`（`SandboxPayChannelAdapter`、`PayFeatureFlags`）、`component/src/commonMain/.../component/chat/ImEngine.kt` 与 `feature/chat/src/commonTest/.../MockImEngineTest.kt`、`core/analytics/src/commonMain/.../Tracker.kt`（仅 `analyticsInfo()` 占位）、`feature/auth/src/commonMain/.../MockAuthConfig.kt`、`core/platform/src/ohosMain/.../PlatformWebView.ohos.kt` / `PlatformCameraScan.ohos.kt` / `PlatformOpen.ohos.kt`、`openspec/changes/parity-flutter-to-kmp/notes/platform-gap-registry.md`。

**动手任务**
1. `[#33]` 定义 `PushTokenProvider` 缝与三端 adapter 接口，先做契约测试 + 降级；真 FCM / APNs / HPush 接入列为需凭据任务。
2. `[#34]` 关闭 `SandboxPayChannelAdapter` 的默认启用，补「假 Success」回归测试；加订单幂等键与验单防重放；真 OpenSDK / IAP 列为需凭据任务。
3. `[#35]` 定义 IM 会话 / 消息 / 已读回执端口，把 `MockImEngine` 变成可注入实现；融云 SDK 列为需 AppKey 任务。
4. `[#36]` 定义微信授权码端口与生物识别接口（接口 + 契约测试 + 降级），真 SDK 列为需凭据任务。
5. `[#37][#38][#39]` 逐个评估 OHOS `PlatformWebView` / `PlatformCameraScan` / MediaKit stub：能替换的替换，不能替换的把 `Degraded` 状态、blocker、follow_up 写进 registry。
6. `[#40]` 为 image_picker / 语音录制播放 / 屏幕防截取建立 expect/actual 接口骨架 + gap 登记（不伪实现）。
7. `[#41]` 把 `core/analytics/Tracker.kt` 从 1 行占位扩成事件模型 + 上报端口 + 采样 / 批处理 + 本地 sink（先不上报远端）。
8. `[#43][#75]` 建立配置中心抽象：把 `PayFeatureFlags` / `MockAuthConfig` 的进程内 `var` 迁到「可下发配置 + 本地默认 + 灰度 / AB 分组 + 变更审计」，先落本地实现。
9. `[#42][#44]` gRPC 与崩溃/APM SDK：先写能力评估与接口骨架（Flutter 侧用 BFF `:9090`，KMP 无产物；监控 SDK 需账号），登记为外部前置。

**验收**：每个能力都具备「接口 + 契约测试 + 降级 + gap 登记」四件套；registry 中对应行状态与 follow_up 同步更新；`PayFeatureFlags.sandboxEnabled` 不再默认 true。

**自检**：为什么 registry 要求「未完成目标必须保留 blocker + follow_up，禁止判绿」？接口先行的意义是什么？演示态与真实支付边界如何隔离？

---

### 阶段 5 · 测试体系

**通俗讲解**：这一阶段管的是「怎么证明没改坏」。现在有 675 个测试，听起来很多，但全是在 JVM 上跑的零件级检查——相当于把每个螺丝都量过，却从没把整机装起来通电试过。手机转屏、真机截图、界面点起来顺不顺、两个模块拼起来对不对，都没被自动检查。这一步是把「查零件」补成「查部件、查整机、查外观」，并且先确认量尺本身准不准。

**专业讲解**：按测试金字塔补齐四层——`commonTest` 单元测试（已有 675 个）、契约测试（消费方/提供方一致性）、`androidTest` instrumentation 与 Compose UI 测试（交互路径）、截图/像素门禁（渲染一致性）；引入 Kover 覆盖率并设阈值（注意覆盖率只证明「执行过」不证明「断言正确」）；用 MockK/Turbine 替换部分手写替身并覆盖 `StateFlow` 时序；把 `screenshot_diff_gate.py` 接进门禁但设计为「有配对证据才硬卡、无证据登记 gap 不阻塞」；CI 增加 iOS/OHOS 的 compile-only 校验。

**示例讲解（一个测试里藏着多少概念）**：

```kotlin
// 示意（非仓库现状），用于讲清「全局状态为什么污染测试」
class DemoRequestHeadersTest {
    @AfterTest fun tearDown() {
        NetworkConfig.accessToken = ""
        NetworkConfig.sessionId = ""
        NetworkConfig.deviceId = ""
    }

    @Test fun session_headers_present_only_when_logged_in() {
        NetworkConfig.accessToken = "t"
        NetworkConfig.sessionId = "s"
        NetworkConfig.deviceId = "d"
        val headers = DemoRequestHeaders.forUrl(DemoApiHosts.PRODUCT + "/api/v1/home")
        assertEquals("s", headers[DemoRequestHeaders.HEADER_SESSION_ID])
        assertEquals("d", headers[DemoRequestHeaders.HEADER_DEVICE_ID])
    }
}
```

- 第 3–7 行 `@AfterTest`：专业＝清理共享的进程级可变状态，保证测试可重复、可任意顺序执行；通俗＝谁用完公共白板谁擦干净，否则下一个人看到的是上一个人的字。这也反过来说明阶段 1 为什么要把 `NetworkConfig` 从全局 `var` 收敛掉——**测试要写 tearDown，本身就是设计债的信号**。
- 第 10–12 行：专业＝准备（Arrange）阶段注入登录态三件套；通俗＝先给收银员发一张工牌、一个会话号和一个设备号。
- 第 13 行 `forUrl(...)`：专业＝被测单元是纯函数形式的请求头构造，无需网络；通俗＝只检查「单子贴得对不对」，不用真发车。
- 第 14–15 行 `assertEquals`：专业＝断言具体字段值，而不是「不抛异常」；通俗＝不是「没出事就算过」，而是逐项核对贴的两个号码确实是 s 和 d。

**再看截图门禁的两条命令（真实脚本语义）**：

```bash
/usr/bin/python3 scripts/test_screenshot_diff_gate.py          # ① 先验证尺子本身
python3 scripts/screenshot_diff_gate.py \
  --flutter path/to/flutter.png --kmp path/to/kmp.png \
  --out-dir openspec/changes/parity-flutter-to-kmp/notes/evidence/home/Android \
  --route home --bar 2.0                                       # ② 再量两张照片
```

- 命令 ①：专业＝Seam Test，验证门禁脚本自身可被调用、退出码语义正确；这里特意用 `/usr/bin/python3`，因为本机默认 `python3` 缺 numpy/PIL。通俗＝先确认尺子没断、刻度没花。
- 命令 ② 的每个参数：专业＝`--flutter`/`--kmp` 是两幅待比图，`--out-dir` 写差异报告，`--route` 标注路由，`--bar 2.0` 是 `mse_pct` 允许阈值；退出码 0=通过、1=超阈值、2=用法/IO 错误。通俗＝左边标准照、右边实拍照、结果存到这个抽屉、写上「home 页」、差别超过 2 分就算不合格。
- 为什么现在不能硬接门禁：专业＝仓库缺成对的真机截图证据，接成无条件阻塞会误红；设计成「有证据才比、无证据登记 gap」才能既不假绿也不误伤 CI。通俗＝还没拍照就宣布「照片不合格就停产」，会把整条线卡死；得先说「没照片时先不判，等照片到了自动开始判」。

**测试金字塔四层对照**：

| 层 | 专业 | 通俗 | 本仓库现状 |
|---|---|---|---|
| 单元 | `commonTest` / `testDebugUnitTest`，毫秒级、无设备 | 逐个量螺丝 | 有，675 个 `@Test` |
| 契约 | 消费方期望 ↔ 提供方实际，如 OpenAPI/Pact | 两个车间对着图纸互报尺寸 | 缺 |
| UI / instrumentation | `androidTest` + Compose test，跑在设备/模拟器 | 整机装好通电点一遍 | 缺（`androidx-espresso-core` 声明未用） |
| 截图 / 像素 | 渲染结果与 SoT 比对 | 拍张照和标准照比 | runner 齐备，未接门禁 |

**学什么**：测试金字塔、Compose UI 测试、instrumentation 测试、截图测试与像素门禁、覆盖率阈值、Mock 框架与 StateFlow 测试、多平台测试执行。

**读哪些代码**：现有 65 个 `*Test.kt` / 675 个 `@Test`、`scripts/screenshot_diff_gate.py` 与 `scripts/test_screenshot_diff_gate.py`、`scripts/parity-evidence.sh`、`.github/workflows/ci.yml`、`docs/deep-dive/24-gates-ci-adr.md`（若已入库）。

**动手任务**
1. `[#52]` 补 `androidTest` instrumentation 与 Compose UI 测试，选 2–3 个关键 Screen（登录、社区列表、AI 流式）覆盖交互路径。
2. `[#57]` 把 `screenshot_diff_gate.py` 接进门禁：设计为「有配对 Flutter/KMP 证据才硬卡、无证据登记 gap 不阻塞 CI」；补 numpy/PIL 的环境说明（本机默认 `python3` 缺，`/usr/bin/python3` 可用）。
3. `[#53]` 在阶段 0 的覆盖率基线上，为 `core:network` / `core:account` / `feature:pay` 设合理阈值并接入 CI。
4. `[#56]` 引入 MockK / Turbine，替换部分手写 `Mock*` / `Stub*`，补 `StateFlow` / `Flow` 的时序测试。
5. `[#48]` CI 增加 iOS / OHOS 的 compile-only 或 unit-test job（或先做 `-PandroidOnly=false` 的配置校验），把「托管 runner 不构建 iOS/OHOS」的约束写明并给出替代方案。
6. `[#58][#59]` 建立测试分层规范（单元 / 契约 / UI / 截图各层职责）、共享 fixtures 与 fake 模块清单；把 `parity_task_audit.py` 的 162 条 backlog 与代码状态做一次人工核对（只报差异、不刻意勾选）。

**验收**：测试金字塔四层各有可跑用例；截图门禁可重复执行且不误阻塞；CI 新增 job 通过；覆盖率报告与阈值生效。

**自检**：为什么「675 个 @Test 全是 JVM unit test」仍不足以上生产？截图门禁为什么不能做成「无证据也阻塞」？

---

### 阶段 6 · 发布、安全与合规

**通俗讲解**：这一阶段管的是「怎么把货正式交出去、以及交出去的东西够不够安全」。现在的做法相当于：车间样机（debug 包）贴个标签就当正式货发；货箱没盖章（无签名）、内容没打码（无混淆与脱敏）、窗户大开（明文 HTTP 全放行）、钥匙就放在纸条抽屉里（token 明文 + `allowBackup=true`）。这一步就是把「样机」换成「贴封条的正式货」，并把门窗、锁、申报单补齐。

**专业讲解**：新增 `signingConfig`（凭据从环境变量/`local.properties` 读取、绝不入库），release 开启 `minify` + `shrinkResources` + proguard rules；CI release job 产出 signed release AAB/APK 而非 debug APK；版本号由 CI 注入；`allowBackup` 收敛到 `fullBackupContent` / `dataExtractionRules` 排除凭据；`network_security_config.xml` 改为默认禁明文、仅本地回环域名白名单、补证书固定；App Link 配 `assetlinks.json` / AASA；iOS 补 `TEAM_ID`、Provisioning 与 `PrivacyInfo.xcprivacy`；隐私流程补齐同意撤回、删除、导出与政策版本化；加 secrets scanning 与密钥轮换。

**示例讲解（一行 XML 为什么等于「所有窗户都开着」）**：

```xml
<!-- 现状：composeApp/src/androidMain/res/xml/network_security_config.xml（真实代码） -->
<network-security-config>
    <base-config cleartextTrafficPermitted="true" />
</network-security-config>
```

- 第 3 行 `base-config`：专业＝`base-config` 作用于**所有**未被更具体 `domain-config` 覆盖的域名；`cleartextTrafficPermitted="true"` 允许 `http://` 明文流量；Manifest 同时写 `android:usesCleartextTraffic="true"`，两者叠加。通俗＝不是「给 localhost 开个小窗」，而是「整栋楼所有窗户都拆了」，包括以后新增的正式域名。
- 为什么当初这么写（专业）：Dev/Test/Staging 三个环境都指向 `http://127.0.0.1:8080`，本地联调需要明文；但配置没有按构建类型区分，导致 release 同样放行。通俗＝为了在车间里接一根临时明线，结果把整栋楼的门禁都撤了。

**改完长这样（示意，非仓库现状）**：

```xml
<network-security-config>
    <base-config cleartextTrafficPermitted="false" />
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">127.0.0.1</domain>
        <domain includeSubdomains="true">10.0.2.2</domain>
    </domain-config>
</network-security-config>
```

- 专业：`base-config` 默认禁明文，`domain-config` 只给两个本地回环/模拟器地址开例外；配合证书固定（pin-set，此处未展开）可抵御中间人。通俗＝楼里默认全关窗，只给「车间内部那两条通道」留了小窗，别的窗户一律封上。

**签名与 release 的示意（非仓库现状，接入前按 AGP 版本核对）**：

```kotlin
signingConfigs {
    create("release") {
        storeFile = file(System.getenv("KEYSTORE_PATH") ?: "release.keystore")
        storePassword = System.getenv("KEYSTORE_PASSWORD")
        keyAlias = System.getenv("KEY_ALIAS")
        keyPassword = System.getenv("KEY_PASSWORD")
    }
}
buildTypes {
    getByName("release") {
        signingConfig = signingConfigs.getByName("release")
        isMinifyEnabled = true
        shrinkResources = true
        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
}
```

- 第 2–7 行：专业＝四个值全部来自环境变量，`.gitignore` 已排除 `*.keystore` / `.env`，凭据不进版本库；通俗＝印章放在保险柜，密码写在只有发版机器才有的环境变量里，谁都不许把密码抄进代码。
- 第 11 行 `signingConfig`：专业＝让系统能验证来源与升级连续性；通俗＝给包裹盖一个只有你有、且能验真的章，用户升级时系统才认是同一个 App。
- 第 12–13 行：专业＝R8 裁剪/混淆 + 移除未引用资源，减小包体并提高逆向成本；通俗＝出厂前撕掉没用的页、把内部代号改成短名，但必须先写好 proguard 规则标明「反射用到的名字不能改」。

**再看「把样机当正式货」的 CI 片段**（`.github/workflows/ci.yml` release job，真实语义）：

```yaml
- name: Download debug APK
  uses: actions/download-artifact@v4
  with:
    name: composeApp-debug-apk
- name: Create GitHub Release
  uses: softprops/action-gh-release@v2
  with:
    files: apk/*.apk
```

- 专业：下载的是 `assembleDebug` 产物（调试签名、可调试、未混淆），直接发布为 Release 资产；`composeApp/build.gradle.kts:213` 的 `isMinifyEnabled = false` 进一步说明 release 也没裁剪。
- 通俗：正式货架上摆的是车间测试机——谁都能连调试器、包体没压缩、内部名字全露着；它不是「差一点」，而是「根本不是同一类货」。

**学什么**：Android 签名配置、R8 / ProGuard / 资源压缩、AAB 与 Play 发布、iOS 签名与 Provisioning、Harmony 签名、备份与数据抽取规则、证书固定、隐私清单与 GDPR/个保法、secrets 管理。

**读哪些代码**：`composeApp/build.gradle.kts:192-224`（`buildConfig=false`、`isMinifyEnabled=false`、`versionCode=1`）、`composeApp/src/androidMain/AndroidManifest.xml`（`allowBackup="true"`、`usesCleartextTraffic="true"`、三个 intent-filter）、`composeApp/src/androidMain/res/xml/network_security_config.xml`、`iosApp/Configuration/Config.xcconfig`（`TEAM_ID=` 空）、`harmonyApp/AppScope/app.json5`、`.github/workflows/ci.yml` 的 release job、`.gitignore`。

**动手任务**
1. `[#61][#62][#63]` 加 `signingConfig`（从 `local.properties` / 环境变量读，绝不入库）、release buildType 开启 `minify` + `shrinkResources` + proguard rules；CI release job 改为产出 **signed release AAB/APK**，替换当前上传 debug APK 的做法。
2. `[#64]` 版本号从 CI 构建号注入（`versionCode` / `versionName` 去硬编码）；补 CHANGELOG 与发布检查清单。
3. `[#49][#66]` 把 OHOS `publish*BinariesToHarmonyApp` 与 `deployment.gradle.kts` 的真机/模拟器任务整理成发布 runbook，明确哪些步骤门禁不跑、由谁手工执行。
4. `[#50]` iOS `TEAM_ID` / Provisioning 与 Harmony 签名配置：接口与说明齐备，真配置列为需开发者账号任务。
5. `[#68][#69]` `allowBackup` 收敛：`fullBackupContent` / `dataExtractionRules` 排除凭据；App Link assetlinks / AASA 接入；deeplink 参数白名单（与阶段 2 任务 2 合并验收）。
6. `[#70]` 补 iOS `PrivacyInfo.xcprivacy`、隐私政策版本化、同意撤回、数据删除/导出流程。
7. `[#71]` 给 `MockAuthConfig.useMockAuth` 与 `SandboxPayChannelAdapter` 加「release 包绝不可能启用」的编译期或启动期断言。
8. `[#72][#73]` 补 secrets scanning 与密钥轮换流程；把日志 trace-id 贯穿（与阶段 3 任务 3、阶段 7 任务 2 合并验收）。
9. `[#65]` 补 CODEOWNERS / PR 模板 / issue 模板 / commitlint（与阶段 8 任务 1 合并）。

**验收**：能产出可安装的 Android release 包且签名可验证；CI 产物不再是 debug APK；Manifest 与网络安全配置通过静态检查；发布 runbook 可执行。

**自检**：为什么 `allowBackup="true"` 对明文 token 是风险？debug APK 发 Release 会带来哪些具体问题？

---

### 阶段 7 · 可观测性、性能与 UX

**通俗讲解**：这一阶段管的是「上线以后怎么知道好不好、以及不同用户能不能都顺手用」。现在 App 发出去就像放出去一只没装定位的信鸽：闪退没人报、卡多久没人数、日志全靠 `println` 随手喊；界面只有一套中文、一套亮色、手机尺寸，换语言、开深色、用平板、给视障用户读屏都没系统验过。这一步是装黑匣子、量秒表，再把「只有一种样子」变成「多种人都能用」。

**专业讲解**：建崩溃/ANR 上报 + APM + SLO 告警；结构化日志加 trace-id 贯穿与 release 采样；远程配置支持强制更新、灰度与回滚；用 Baseline Profile / Macrobenchmark / `reportFullyDrawn()` 建立冷启动与首帧基线；输出 Compose 稳定性报告并治理 `@Stable`；接 LeakCanary、R8 与 APK 体积门禁；把硬编码文案抽成资源做 i18n（≥2 语言），补 `values-night` 深色模式、`WindowSizeClass` 平板/折叠适配、字体缩放与 TalkBack/VoiceOver 无障碍；扩 `core/design` 为 design token 体系并统一 Loading/Empty/Error/Retry 组件。

**示例讲解（一个「看起来没问题」的界面为什么会偷偷变慢）**：

```kotlin
// 示意（非仓库现状），用于讲清 Compose 稳定性
data class Post(val title: String)

@Stable
data class FeedUiState(val items: List<Post>, val isLoading: Boolean)

@Composable
fun FeedScreen(state: FeedUiState, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize()) {
        if (state.isLoading) {
            CircularProgressIndicator()
        } else {
            state.items.forEach { Text(text = it.title) }
        }
    }
}
```

- 第 2 行 `data class Post`：专业＝不可变 `val` 字段的数据类型，编译器可判定为稳定；通俗＝一张印好就不改的卡片。
- 第 4 行 `@Stable`：专业＝向 Compose 编译器承诺「该类型的变化可被可靠通知」，使其能在参数未变时跳过重组；未标注或含 `var` / `MutableList` 的类型会被视为不稳定，导致不必要的重执行。通俗＝给箱子贴「里面没换过，别反复开箱检查」的封条；没封条，编译器每次都得开箱翻一遍。
- 第 7 行 `modifier: Modifier = Modifier`：专业＝`Modifier` 是工厂对象（大写），`modifier` 是参数名（小写），链式调用从参数出发；通俗＝前者是「空白贴纸本」，后者是「这次要贴的那张」，写混会直接编译失败——这是 `AGENTS.md` 反复强调的硬规则。
- 第 8–13 行：专业＝按 `isLoading` 分支渲染，状态由外部传入（`state` 提升），函数本身无副作用；通俗＝界面只是「照着这张状态卡画画」，卡变了才重画，不自己偷偷改数据。

**再看「只有一种样子」的证据**（`composeApp/src/androidMain/res/values/strings.xml`，真实代码）：

```xml
<resources>
    <string name="app_name">My AI</string>
</resources>
```

- 专业：全仓 Android 资源里只有 1 条字符串，其余文案硬编码在 Kotlin 里；资源限定符只有密度 `mipmap-*` 与默认 `values/`，无 `values-en` / `values-zh-rCN` / `values-night` / `layout-sw600dp`。
- 通俗：全楼只贴了一张中文门牌，没有备用语言标签、没有夜间配色、没有大户型图纸；想换语言得挨个房间把字刮掉重写，而不是换一张标签贴。

**改成可替换资源后（示意）**：

```xml
<!-- values/strings.xml -->
<string name="retry">重试</string>
<!-- values-en/strings.xml -->
<string name="retry">Retry</string>
```

```kotlin
Text(text = stringResource(Res.string.retry))   // CMP 资源写法示意
```

- 专业：文案资源化 + 按 locale 限定符自动选择；`stringResource` 从 composeResources 读取，配合 `values-night` 的配色资源实现深色模式、`WindowSizeClass` 实现响应式布局。
- 通俗：把「重试」做成可换的标签贴，英文环境自动贴 Retry，晚上自动换深色贴纸，平板自动换成大号排版；代码里不再写死中文。

**日志的对照（一句话记住结构化日志）**：

```kotlin
println("[Ktor] $message")                       // 现状：一行随手便签
AppLog.d("HTTP", mapOf("trace" to traceId,     // 目标示意：字段化事件 + 脱敏
                      "url"  to redact(url),
                      "code" to status))
```

- 专业：结构化日志是可被查询/聚合的字段化事件，配合 trace-id 串联一次请求、采样控量、脱敏移除凭据；`println` 无级别、无字段、无开关。
- 通俗：前者是「出事了」三个字喊一嗓子；后者是「时间/流水号/哪个接口/什么结果」填进表格，且身份证号打码，方便事后按流水号把整条链路翻出来。

**学什么**：崩溃/ANR 上报与 SLO、结构化日志与 trace-id、强制更新与灰度、Baseline Profile / Macrobenchmark、Compose 稳定性与 `@Stable`、LeakCanary、R8 体积治理、i18n、深色模式、WindowSizeClass、无障碍、设计系统 token。

**读哪些代码**：`core/design/`（`DemoColors`、`ImmersiveInsets`、三个 Immersive 顶栏）、`composeApp/src/androidMain/res/values/strings.xml`（只有 `app_name` 一条）、`composeApp/src/androidMain/res/` 的限定符（只有密度 + 默认 values）、`RealtimeRuntime.kt` 的非阻塞首连、`gradle.properties` 的 `kotlin.native.cacheKind=none` 与 `rendererBackend`。

**动手任务**
1. `[#74][#76]` 崩溃/ANR 上报 + APM：先做接口与本地 sink，账号类上报登记为外部前置；结构化日志加 trace-id 贯穿与 release 采样。
2. `[#77][#80]` 建 Baseline Profile / Macrobenchmark 与 `reportFullyDrawn`，产出冷启动/首帧基线数据；给 `RealtimeRuntime` 的「不阻塞冷启动」补一条量化断言。
3. `[#78]` 输出 Compose 稳定性报告，治理 `@Stable` / immutable 集合，补重组计数监控。
4. `[#79]` 接 LeakCanary、R8 与资源压缩，加 APK 体积门禁；评估 PNG 五密度资源的压缩流水线。
5. `[#81]` 把硬编码文案收进资源，至少支持 2 种语言（含 ArkTS / Swift 侧文案）。
6. `[#82]` 补深色模式（`values-night` / `DarkColorScheme`），三端一致。
7. `[#83][#84][#88]` 用 `WindowSizeClass` 做平板/折叠/横屏适配；补字体缩放、对比度、TalkBack/VoiceOver 全流程检查表（当前约 100 处 `contentDescription` / `semantics`，相对 364 文件偏少）。
8. `[#86][#87]` 扩 `core/design` 为设计系统（配色 / 排版 / 间距 / 组件），补统一 Loading / Empty / Error / Retry / Snackbar 通道；把 Screen 里的颜色与 Dp 魔法值收编到 token。

**验收**：性能基线数据可复现；i18n / 深色 / 无障碍三张检查表通过；设计系统 token 覆盖新增组件；日志与崩溃链路有本地闭环。

**自检**：为什么 `remember { mutableStateOf }` 的性能问题要靠稳定性报告而不是肉眼发现？为什么三端沉浸式规范靠文档而非编译保证？

---

### 阶段 8 · 工程治理与协作

**通俗讲解**：这一阶段管的是「人多了以后怎么不吵架、不重复踩坑」。现在重要的决定散在代码注释里（ADR 0001/0002/0003），没有台账；谁负责哪块没写清楚；文档散在 `AGENTS.md`、`MODULES.md`、11 个 openspec change、63 个 docs 文件、`notes/` 里，还有几份没入库；最典型的事故是「三本施工进度表互相矛盾，检验员只翻了最早那本，于是明明没做完却盖了通过章」。这一步就是把「谁说了算、为什么这么定、现在到底做到哪」全部写清楚并只留一个入口。

**专业讲解**：补 LICENSE / CODEOWNERS / CHANGELOG / CONTRIBUTING / PR & issue 模板 / commitlint，提交说明沿用中文并写清原因；把散落的 ADR 收编成带状态（proposed / accepted / superseded）的编号化台账；确定单一「当前状态」文档入口并明确 `docs/deep-dive/`、`docs/plans/`、`notes/`、架构图产物 `.archify/` 的入库归属；推广 `explicitApi()` 与 binary compatibility validator 到 `core:*`；把唯一执行账本与 `parity_task_audit.py` 门禁联动；把「writer 白名单」从 spec 约定升级为可检查清单，并明确 `main` 与 `origin/main` 的差异由人工推送（禁止自动 push）。

**示例讲解（一张 ADR 该写什么）**：

```markdown
# ADR 0004：引入 Koin 作为 DI 容器
- 状态：Accepted
- 背景：AppContainer 是手写 Composition Root，无 Scope，
        全局可变配置导致并行测试互相污染（见差距 #7 #9）。
- 备选：① 继续手写组合根 ② Service Locator 全局单例 ③ Koin Scope
- 决策：采用 ③，定义 app / login / screen 三级 Scope。
- 后果：并行测试不再共享登录态；引入新依赖与 Scope 生命周期约束。
```

- 第 2 行「状态」：专业＝ADR 有生命周期，被新决策取代时标 `superseded` 而不是删掉，保留决策链；通俗＝旧图纸盖「作废」章但归档，不扔，免得以后有人问「当初为什么这么修」。
- 第 3 行「背景」：专业＝写清触发问题与证据编号，把决策与差距清单绑定；通俗＝先说「因为哪间屋漏雨」，别一上来就写「换个屋顶」。
- 第 4 行「备选」：专业＝列出被否决方案，避免后来者重复提议；通俗＝把「考虑过但没选的几条路」也记下来，省得半年后重新吵一遍。
- 第 5 行「决策」：专业＝结论唯一、可执行、可验收；通俗＝最后拍板只写一句，不许含糊。
- 第 6 行「后果」：专业＝同时记录收益与新增约束（不是只写好话）；通俗＝换了屋顶确实不漏了，但以后擦玻璃得按新的脚手架规矩来。

**CODEOWNERS 的示意（非仓库现状）**：

```text
/core/network/        @net-team
/feature/pay/         @pay-team
/docs/                @docs-team
```

- 专业：按路径指定默认审查人，PR 触及这些路径时自动请求对应 owner；与「按目录划分的 writer 白名单」配合，减少多人改同一文件的冲突。
- 通俗：每间房门上贴「这间归谁管」，动这间房的图纸必须先找这人签字，不用每次在群里问一圈。

**再看「三本账」的假绿是怎么发生的**（Wave 1 修复前的真实语义）：

```text
tasks-execution.md      585 [x] / 0 open   ← 门禁当时只读这本，直接 PASS
tasks-fine.md           历史账本
tasks-ultra-fine-v2.md  162 [ ]           ← 真正该读的唯一执行账本
```

- 专业：门禁的判定输入与「唯一真相源」不同源，导致 False Green（假绿）；修复后 `parity_task_audit.py` 默认读 v2、扫描 `[ ]`/`[~]`、真实剩余 >0 即 `sys.exit(1)`，另两本顶部标注「历史账本，只用于查漏，不再驱动执行」。
- 通俗：检验员翻的是一本早作废的旧账，账上全打勾就盖章放行；总账上其实还空着 162 行。修法是「只认总账、旧账盖章作废但留着查漏」，而不是把旧账的空格也补齐来骗自己。

**学什么**：ADR 台账、CODEOWNERS、变更日志、提交规范、`explicitApi()` 与 binary compatibility、文档分层与入库策略、账本与代码同步。

**读哪些代码**：`AGENTS.md`、`MODULES.md`、`openspec/changes/`（11 个 change）、`openspec/changes/parity-flutter-to-kmp/notes/platform-gap-registry.md`、`notes/evidence/index.md`、`sync-flutter-full-parity/tasks-ultra-fine-v2.md`、`docs/`（63 个文件，其中 `deep-dive/` / `plans/` / `ARCHITECTURE.md` 未跟踪）。

**动手任务**
1. `[#89]` 补 LICENSE / CODEOWNERS / CHANGELOG / CONTRIBUTING / PR & issue 模板 / commitlint；提交说明沿用中文、写清原因、不带 `Co-authored-by`。
2. `[#91]` 把散在注释里的 ADR 0001 / 0002 / 0003 收编成编号化台账，补状态（proposed / accepted / superseded）。
3. `[#90]` 确定单一「当前状态」文档入口，明确 `docs/deep-dive/`、`docs/plans/`、`notes/`、`docs/ARCHITECTURE.md`、本文件的入库与归属；把架构图产物目录 `.archify/` 的本机属性写清楚。
4. `[#92]` 把 `tasks-ultra-fine-v2.md` 的勾选状态与 `parity_task_audit.py` 门禁联动，产出一次「账本 ↔ 代码」差异报告（不刻意清空 162 条 backlog）。
5. `[#93]` 在阶段 1 试点基础上，把 `explicitApi()` / binary compatibility validator 推广到 `core:*` 模块。
6. `[#94]` 明确文件所有权：把「writer 白名单」从 spec 约定升级为可检查的清单；确认 `main` 与 `origin/main` 的领先提交由人工推送（**禁止自动 push**）。

**验收**：新成员按文档能独立跑通构建、测试、发布；ADR / 账本 / 注册表 / gap registry 四份文档口径一致；治理文件入库且不含凭据。

**自检**：为什么「唯一执行账本」必须与门禁同源？文档分散在多处时，如何避免再次出现三份账本互相矛盾、门禁只读其中一份的假绿？

---

## 3. 每阶段收尾通用验收

```bash
bash scripts/verify-parity.sh --static-only      # 期望 exit 0
bash scripts/check-layer-deps.sh                 # 期望 exit 0
bash scripts/audit-flutter-drift.sh              # 期望 exit 0
python3 scripts/parity_task_audit.py             # 期望 exit 1（162 条真实未完成；假绿已修）
./gradlew <受影响模块>:testDebugUnitTest --console=plain
git status --short                              # 确认无凭据/工具目录被暂存
```

- 截图 seam 测试用 `/usr/bin/python3 scripts/test_screenshot_diff_gate.py`（本机默认 `python3` 缺 numpy/PIL）。
- 任何阶段都 **不 push**；提交只在明确要求时进行，且提交前并行看 `git status` / `git diff` / `git log -5`。

---

## 4. 外部前置清单（本机无法完成，必须登记 gap）

| 类别 | 需要的外部条件 | 本计划里的处理方式 |
|---|---|---|
| 厂商推送 | FCM / APNs / 华为 Push Kit 的 SDK 与 appKey、证书 | 接口 + 契约测试 + 降级 |
| 支付 | 微信/支付宝 OpenSDK、商户号、IAP 沙箱账号 | 接口 + 幂等 + 假 Success 回归 |
| IM / 登录 | 融云 AppKey、微信 `appId`、生物识别设备 | 接口 + mock 可注入 |
| 监控 / 埋点 | Crashlytics / Sentry / APM 账号与 DSN | 本地 sink + 上报端口 |
| 发布 | Google Play / App Store Connect / Harmony 开发者账号、签名证书、Provisioning | runbook + 配置说明 |
| 真机证据 | 三端真机像素截图、重启回归、e2e SSE / 单设备登录 / realtime 链接 | gap 登记 + 证据目录约定 |
| 后端联调 | Go BFF 可用实例、Supabase 配额、真实 T+1 | 契约测试 + 环境注入 |
| 平台 klib | OHOS `ohosX64` 的 Ktor / Coil / uuid、SQLDelight klib | 中间 source set + 类型化降级 |

---

## 5. 差距编号 → 阶段 全覆盖索引（`#1`–`#94`，无遗漏）

| 维度 | 编号 | 主题（该维度全部差距） | 归属阶段 |
|---|---|---|---|
| A 架构与模块化 | 1–6 | `api` 泄漏、无 api/impl、无 convention plugin、口径不一致、无所有权、无动态模块 | 0（3,4）、1（1,2,5,6） |
| B DI 与生命周期 | 7–12 | 手写 AppContainer、无作用域、全局 var、无 ViewModel、返回栈空、无 MVI | 1 |
| C 导航与路由 | 13–16 | 自研导航、无框架、三端双份 UI、App Link 未验证 | 2 |
| D 网络层 | 17–26 | 无重试/缓存、单一 30s 超时、日志无脱敏、明文全放行、错误模型薄、双码表、手工契约、设备口径写死、环境缺失、无分页框架 | 3 |
| E 持久化与数据 | 27–32 | InMemory DB、无迁移/并发安全、凭据明文、KV 窄、无 Blob/离线队列、无统一 Repository | 3 |
| F 三方能力 | 33–44 | 推送、支付、IM、登录、WebView、相机、媒体、图片选择/语音/防截屏、analytics 空壳、gRPC、无远程配置、无监控 SDK | 4 |
| G 多平台一致性 | 45–51 | OHOS 双架构降级、Degraded stub、三端壳重复、CI 不构建 iOS/OHOS、发布手工、无签名、全量配置 + Native 缓存关闭 | 0（51）、2（45,46,47）、5（48）、6（49,50） |
| H 测试与质量 | 52–59 | 全是 JVM 单测、无覆盖率、无静态分析、无依赖治理、手写替身、截图门禁未接、backlog 计数门禁、无测试规范 | 0（53,54,55）、5（52,56,57,58,59） |
| I CI/CD 与发布 | 60–67 | 单 job、发 debug APK、无签名/混淆/版本号、无多环境灰度、无治理模板、deployment 任务不在门禁、docs 不触发校验 | 0（60,67）、6（61–66） |
| J 安全与合规 | 68–73 | 明文/备份/证书固定、deeplink 未校验、隐私流程不全、mock 与 sandbox 边界、secrets、日志脱敏 | 6 |
| K 可观测性与运维 | 74–76 | 无崩溃/APM/SLO、无配置中心、无结构化日志与 trace | 4（75）、7（74,76） |
| L 性能与启动 | 77–80 | 无 Baseline Profile、无 Compose 稳定性、无泄漏/体积治理、无启动 trace（构建侧缓存问题见 #51） | 7（77–80） |
| M 用户体验与适配 | 81–88 | 无 i18n、无深色、无平板适配、无障碍偏少、三端沉浸式不一致、无设计系统、无统一状态组件、无 RTL 验收 | 2（85）、7（81–84,86–88） |
| N 工程治理与协作 | 89–94 | 无治理文件、文档分散、ADR 散落、账本不同步、无 API 冻结、无文件所有权 | 8 |

合计 `6+6+4+10+6+12+7+8+8+6+3+4+8+6 = 94` 条，全部映射到阶段 0–8。

---

## 6. 资料类型（按主题查官方文档，不在本文编造链接）

- **KMP / Compose**：Kotlin Multiplatform 官方文档、Compose Multiplatform 文档、Kotlin `expect`/`actual` 与中间 source set 指南。
- **构建**：Gradle User Manual（Version Catalogs、Composite Builds、Configuration Cache、Dependency Verification）、AGP 发布与签名指南、`kotlin-dsl` plugin marker 解析说明。
- **Android**：Android Developers（App Bundle、signing、R8/ProGuard、Data Extraction Rules、Network Security Config、Baseline Profiles、Macrobenchmark、WindowSizeClass、Accessibility、Compose 稳定性、LeakCanary）。
- **iOS / Harmony**：Apple HIG / App Store Connect / Privacy Manifest、HarmonyOS ArkTS 与 NDK（`OH_Preferences`、ArkWeb、MediaKit、Push Kit）文档。
- **库**：Ktor（plugins、HttpTimeout、Logging、mock engine、WebSockets）、Koin、Decompose / Voyager、SQLDelight / Room、Coil、Vico、Detekt / ktlint、Kover / Jacoco、MockK / Turbine、OpenAPI Generator 各自官方文档。
- **安全与规范**：OWASP MASVS / Mobile Security Testing Guide、Material Design 3、GDPR / 个人信息保护法合规要点。
- **本仓库（一手资料）**：`AGENTS.md`、`composeApp/.../MODULES.md`、`docs/ARCHITECTURE.md`、`docs/LEARNING_PLAN.md`（本文件）、`notes/backend-contract-matrix.md`、`notes/route-inventory.md`、`openspec/changes/`（11 个 change）、`openspec/changes/parity-flutter-to-kmp/notes/platform-gap-registry.md`、`notes/evidence/index.md`、`docs/deep-dive/`（逐模块深入，未跟踪）。

---

## 7. 完成定义

一个阶段算完成，当且仅当：① 该阶段所有动手任务有代码或文档产出；② 第 3 节通用验收全绿（该阶段新增的门禁也绿）；③ 新增能力有回归测试；④ 受影响的 `platform-gap-registry.md` 行、`notes/evidence/index.md`、契约矩阵同步更新；⑤ 需外部前置的项按第 4 节登记 gap 并写明 blocker + follow_up，不伪称完成；⑥ 未 push、未提交凭据。

整体算完成，当且仅当：阶段 0–8 全部满足上述条件，且 `docs/ARCHITECTURE.md` 与本文件按最终状态修订一次（把「差距」改写为「已具备能力 + 剩余 gap」），`main` 与 `origin/main` 的差异由人工确认后推送。

---

## 8. 当前对齐状态（Wave 3，指向 D1–D5 的结论）

> 本节沿用本文件的「专业 / 通俗」双解释，只说明「本轮把什么分清楚了、去哪儿查」，**不复制整份差距清单**。
> 指针：逐字段结论 [backend-contract-matrix.md](../notes/backend-contract-matrix.md) § F（D1）· 结构性 gap [backend-alignment-gaps.md](../notes/backend-alignment-gaps.md)（D5）· 65×2 页数据契约 [native-page-data-contract.md](../notes/native-page-data-contract.md)（D2）· 非端点字符串 [route-inventory.md](../notes/route-inventory.md) §9（D4）· 状态台账 [platform-gap-registry.md](../openspec/changes/parity-flutter-to-kmp/notes/platform-gap-registry.md)（D3）· 证据成熟度 [evidence/index.md](../openspec/changes/parity-flutter-to-kmp/notes/evidence/index.md)（D7）。

### 8.1 专业讲解

以 `my_go_study` 后端为唯一真相源（只读），本轮完成一次**纯双方**（KMP ↔ 后端、不涉及任何第三方）的契约逐字段复核，结论按三层落到文档：

| 层 | 结论 | 去哪儿查 |
|---|---|---|
| 已逐字段对齐 | realtime / SSE / prepay / push / 分页 / 登录 DTO / 会话三件套 / `AuthBizCode` 的字段与协议层一致——是「不是差距」的澄清，勿误判为待办 | [backend-contract-matrix.md](../notes/backend-contract-matrix.md) § F.1（D1） |
| 契约层待收敛 | 错误码双轨、OHOS `body.platform` 伪值、三头无人读、realtime `connId`、双分页、两层鉴权链等，由本轮 A/B/C 槽改代码处置 | 同文件 § F.2（D1） |
| 结构性 gap（只登记、不改代码） | Repository / 离线 / SSOT、InMemory DB、明文凭据、65×2 静态页、analytics gRPC、`/health` 探活、双方 trace-id、后端占位语义边界（亚分金额 >2dp 返回 502、种子 lab-only、对账 local_missing 未实现）；逐项 blocker + follow_up | [backend-alignment-gaps.md](../notes/backend-alignment-gaps.md)（D5） |
| 页级数据契约 | 65×2 原生页「路由 → 数据契约」表、≥3 个试点页、分发守卫只做文本守卫 | [native-page-data-contract.md](../notes/native-page-data-contract.md)（D2） |
| 非端点字符串 | 3 前缀 + 7 测试夹具 + 4 解析器盲区，不计入 `audit-flutter-drift.sh` 的 drift | [route-inventory.md](../notes/route-inventory.md) §9（D4） |

**口径**：状态词只用 `Migrated | Degraded | Placeholder | Deferred | Partial`，按 registry `Rule (2A)` **页面可开 ≠ 数据契约接线 ≠ `Migrated`**；字段层「已对齐」不构成能力完成（真机 / e2e / 像素证据仍缺，对应第 4 节「外部前置」）；第三方内容（厂商 SDK / IAP / IM / Supabase / 短信 / LLM / 对象存储 / 渠道回调 / Prometheus）一律标为「外部前置，非本轮范围」。

### 8.2 通俗讲解

类比：这轮**不是又修了一堆接口**，而是**先把账本分清楚**——以前容易把三种东西混成一团，现在分开记账：

- **本来就一样**：比如分页从第 1 页算、登录字段名、实时消息信封格式、支付预下单的五个参数……两边逐字段核对过是一致的。通俗说就是「别自己吓自己，这不是毛病，别瞎改」。
- **两边说法不一致**：比如错误码一边是小数字表、一边是五位数表；鸿蒙被伪装成安卓上报。这些交给本轮改代码的同事处理，属于「说好的口径要统一」。
- **地基还没打**：没有统一数据层、数据存内存里、登录凭据明文放、原生页全是写死的假数据……这些不是改两行能完事的，这轮**不硬做**，先老老实实登记「缺什么、被什么卡住、以后怎么补」，不假装已经做完。

一句话：**「页面能点开」不等于「数据真的接上了」**。本轮把这两种状态分开记账，没做完的都留了 blocker + follow_up，**不涂绿**。想逐条看，按上表的路径去查对应的笔记就行，不用把整份清单抄过来。

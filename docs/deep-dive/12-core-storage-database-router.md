# 12 · core/storage · core/database · core/router 深度解析

> 分册范围：KMP 分层 ④ horizontal commons 中的三个模块 —— `:core:storage`（全工程唯一 KV 持久化）、`:core:database`（实体表持久化契约，当前仅内存驱动）、`:core:router`（路由常量 / 路径解析 / 深链 / 导航状态）。
> 所有事实均附仓库相对路径与行号（`文件:行`）。本文档基于 2026-10 当前工作区源码逐文件整读写成；未运行任何 Gradle 任务。

---

## 1. 三个模块的定位与依赖关系

### 1.1 一句话定位

| 模块 | 定位 | 证据 |
|------|------|------|
| `:core:storage` | 全工程**唯一** KV 持久化模块，契约 `KeyValueStore`（String / Boolean / List\<String\> 三个槽位），三端真实落盘 | `core/storage/build.gradle.kts:1`；`KeyValueStore.kt:4-12` |
| `:core:database` | 结构化/实体持久化**契约层**：`Database`/`Entity`/`Table` 接口 + 内存 driver + `DatabaseProvider` 注入缝；与 KV 层互补、互不依赖 | `core/database/build.gradle.kts:1-13`；`Database.kt:6-12` |
| `:core:router` | 路由常量表（Flutter 103 条全量 + 产品壳键）、路径/标签解析、深链入口与待跳状态、类型安全 `AppRoute` + `AppNavigator`；无 `@Composable`，只依赖 `compose.runtime` | `core/router/build.gradle.kts:1-15`；`AppRoutePath.kt:4-7` |

### 1.2 Gradle 依赖图（实证）

`:core:storage` 的上游（谁依赖它）：

| 依赖方 | 依赖形式 | 证据 |
|--------|----------|------|
| `:core:account` | `implementation(projects.core.storage)` | `core/account/build.gradle.kts:18` |
| `:core:platform` | `implementation(projects.core.storage)` | `core/platform/build.gradle.kts:19` |
| `:composeApp` | `implementation(projects.core.storage)` | `composeApp/build.gradle.kts:128` |

`:core:router` 的上游（全部是路由常量的消费方）：

| 依赖方 | 依赖形式 | 证据 |
|--------|----------|------|
| `:composeApp` | implementation | `composeApp/build.gradle.kts:124` |
| `:component` | `api(projects.core.router)` | `component/build.gradle.kts:41` |
| `:feature:shell` / `:feature:home` / `:feature:mine` / `:feature:media` / `:feature:community` / `:feature:ai` / `:feature:classroom` / `:feature:content` | api | `feature/shell/build.gradle.kts:17`、`feature/home/build.gradle.kts:28`、`feature/mine/build.gradle.kts:15`、`feature/media/build.gradle.kts:17`、`feature/community/build.gradle.kts:16`、`feature/ai/build.gradle.kts:16`、`feature/classroom/build.gradle.kts:15`、`feature/content/build.gradle.kts:15` |
| `:feature:auth` | implementation | `feature/auth/build.gradle.kts:29` |

`:core:database` 的上游：**没有**。`settings.gradle.kts:33` include 了它并且它会被编译，但全仓 `**/build.gradle.kts` 中除自身 namespace 外没有任何 `projects.core.database` 引用（grep 实证），也没有任何模块 `import com.example.my_kmp_project.core.database`（grep 零命中）。详见 §5。

`:core:storage`、`:core:database`、`:core:router` 三者之间互不依赖；`:core:router` 不依赖任何本仓模块（只有 `api(compose.runtime)`，`core/router/build.gradle.kts:13`）。

### 1.3 根 build.gradle.kts 的共享接线

三个模块都只声明插件 + 自己的 namespace/差异依赖，target 集与底层公共依赖由根构建统一挂载（`build.gradle.kts:35` 把 `:core:*` 与 `:feature:*` 全部纳入）：

| 接线内容 | 行为 | 证据 |
|----------|------|------|
| 仓库顺序 | eazytec 镜像排第一且排除 `com.android.tools.build`，其后阿里云 google/public、google、mavenCentral | `build.gradle.kts:40-55` |
| android 基础配置 | compileSdk/minSdk 从 catalog 取，Java 17 | `build.gradle.kts:70-81` |
| target 集 | `androidTarget` + `iosX64/iosArm64/iosSimulatorArm64` + `ohosArm64/ohosX64`（`-PandroidOnly=true` 时跳原生） | `build.gradle.kts:87,94-102` |
| commonMain 依赖 | coroutines(implementation)、atomicfu(implementation)、serialization-json(api) + serialization 插件 | `build.gradle.kts:107-113` |
| commonTest 依赖 | kotlin-test | `build.gradle.kts:114-116` |
| iosMain/ohosMain 层级 | 与两个 arch 的 dependsOn 关系 | `build.gradle.kts:118-127` |

### 1.4 源集拓扑（storage 特有的 `settingsMain`）

```
commonMain ── settingsMain ── androidMain
          └─（matching）── iosMain
commonMain ── ohosMain ── ohosArm64Main / ohosX64Main
```

- `settingsMain` 是「按产物缺口」切的中间源集，内含 `implementation(libs.multiplatform.settings)`，只被 androidMain 和 iosMain 继承（`core/storage/build.gradle.kts:18-28`；iosMain 用 `matching` 惰性匹配，因为 androidOnly CI 没有 ios target，注释见 `:27-28`）。
- OHOS 两架构**不**继承 settingsMain，由 `ohosMain` 直接实现接口（§4.1）。

---

## 2. 文件清单

### 2.1 core/storage（8 个文件）

| 路径 | 作用 | 关键行号 |
|------|------|----------|
| `core/storage/build.gradle.kts` | settingsMain 源集创建与继承关系；文件头注释记录「无一条 settings 线覆盖六目标」结论 | 18-28（源集）、1-9（决策注释） |
| `src/commonMain/.../KeyValueStore.kt` | KV 契约接口 + `expect fun createKeyValueStore` | 28-52（接口）、58（expect） |
| `src/commonMain/.../InMemoryKeyValueStore.kt` | 内存实现 + internal `BooleanValues` 线格式 | 8-19（BooleanValues）、28-53（类） |
| `src/commonMain/.../StringListValues.kt` | List\<String\> 编解码（Char(31) 分隔符） | 11-24 |
| `src/settingsMain/.../SettingsKeyValueStore.kt` | multiplatform-settings `Settings` → `KeyValueStore` 适配 | 12-38 |
| `src/androidMain/.../AndroidAppContext.kt` | 进程级 application Context 持有者（fail-fast） | 14-28 |
| `src/androidMain/.../KeyValueStore.android.kt` | Android actual（SharedPreferences 每名一文件） | 12-16 |
| `src/iosMain/.../KeyValueStore.ios.kt` | iOS actual（NSUserDefaults suite per name） | 14-15 |
| `src/ohosMain/.../KeyValueStore.ohos.kt` | OHOS 双架构 actual（OH_Preferences NDK） | 41-120 |
| `src/commonTest/.../KeyValueStoreContractTest.kt` | 8 例契约测试（跑在所有 target） | 14-85 |

（`core/storage` 共 8 个源文件 + 1 测试 + 1 构建脚本 = 上表 10 行；构建脚本与 commonMain 合计 8 个 Kotlin 文件。）

### 2.2 core/database（7 个文件）

| 路径 | 作用 | 关键行号 |
|------|------|----------|
| `core/database/build.gradle.kts` | 模块构建 + commonTest 补 coroutines-core；头注释解释「为什么默认内存实现」 | 10-13（OHOS 无 SQL klib）、21-27（test 依赖） |
| `src/commonMain/.../Database.kt` | `Database`/`Entity`/`Table` 三个接口 | 13-22、25-27、30-50 |
| `src/commonMain/.../DatabaseProvider.kt` | driver 工厂注入缝 | 9-20 |
| `src/commonMain/.../InMemoryDatabase.kt` | 内存 driver + `InMemoryTable` | 15-27、30-62 |
| `src/commonMain/.../SampleRecord.kt` | 示例实体 + `SampleRecordDao` 门面 | 10-16、19-35 |
| `src/commonTest/.../InMemoryDatabaseTest.kt` | driver 契约 3 例 | 11-54 |
| `src/commonTest/.../SampleRecordDaoTest.kt` | Dao 门面冒烟 1 例 | 10-29 |

### 2.3 core/router（10 个文件）

| 路径 | 作用 | 关键行号 |
|------|------|----------|
| `core/router/build.gradle.kts` | 只加 `api(compose.runtime)` 与 Compose 插件（state 用 `mutableStateOf`） | 6-7、12-15 |
| `src/commonMain/.../AppNavigator.kt` | 类型安全 `AppRoute` sealed interface + `AppNavigator` + `LocalAppNavigator` | 9-24、29-37、39 |
| `src/commonMain/.../AppRoutePath.kt` | Flutter 全量路由字符串表（103 条） | 7-111 |
| `src/commonMain/.../AppRoutes.kt` | 产品壳路由键 + `MainTab` 枚举 | 4-35、37-46 |
| `src/commonMain/.../DeepLinkEntry.kt` | 平台入口顶层函数 `acceptDeepLink`（导出给 Swift/ArkTS） | 9-10 |
| `src/commonMain/.../DeepLinkRouter.kt` | 深链解析 + pending 状态 + `ParsedDeepLink` + `webUrlFromDeepLink` | 27-150、158-162、168-192 |
| `src/commonMain/.../FeatureRoutes.kt` | 8 个跨 feature 路由对象（Home/Mine/Content/Video/Community/Classroom/Ai）+ label 映射 | 17-244 |
| `src/commonMain/.../MineIslandRoute.kt` | Mine Compose Island 枚举路由 + path/hostKey 双向映射 | 7-64 |
| `src/commonMain/.../ProductRouteDispatch.kt` | 标签/路径 → 规范路径 + host 归属解析器 | 9-23、25-30、32-154 |
| `src/commonMain/.../RoutePath.kt` | `AppRoutePath`/FeatureRoutes 的便捷别名聚合 | 8-64 |
| `src/commonTest/.../ProductRouteDispatchTest.kt` | 解析器 5 例 | 7-61 |

---

## 3. core/storage 逐个类型解析

### 3.1 `KeyValueStore`（interface，commonMain）

`core/storage/src/commonMain/.../KeyValueStore.kt:28`

| 成员 | 签名（:行） | 语义 |
|------|-------------|------|
| `getString` | `fun getString(key: String): String?`（:30） | 从未写入（或被 remove）返回 null |
| `putString` | `fun putString(key: String, value: String)`（:33） | 空字符串是**真实数据**，删除 key 只能用 `remove` |
| `getBoolean` | `fun getBoolean(key: String, defaultValue: Boolean = false): Boolean`（:39） | 缺 key 回退 default；读异类型 key 是未定义行为（:36-37 注释，同底层 Settings 契约） |
| `putBoolean` | `fun putBoolean(key: String, value: Boolean)`（:41） | — |
| `getStringList` | `fun getStringList(key: String): List<String>?`（:47） | null=「用户从未选过」；空列表读回空列表而非 null |
| `putStringList` | `fun putStringList(key: String, values: List<String>)`（:49） | — |
| `remove` | `fun remove(key: String)`（:51） | 唯一删除手段。**没有** 全量 `clear()`、没有 `contains()`——注释解释：应用不需要，且 OHOS 等价 API 只在 API 23+ 存在（:10-13） |

调用方约束（:6-8）：任何需要跨进程重启存活的数据都必须走这个接口；禁止直接摸 SharedPreferences / NSUserDefaults / OHOS Preferences。store 是**廉价视图**而非持有资源——同名 store 指向同一份数据，可随意多次创建（:24-26）。

被调用点：`createAccountSessionStore()`（`core/account/.../AccountSessionStore.kt:24-25`）、`createPrivacyConsentStore()`（`core/account/.../PrivacyConsentStore.kt:19-20`）、`core/platform` 的 `StringListStore.kt:18` 懒建单例。

### 3.2 `expect fun createKeyValueStore(name: String): KeyValueStore`

`KeyValueStore.kt:58`。不同 `name` 必须指向不同持久数据（:55-57）。三个 actual 见 §3.6-3.8。

### 3.3 `StringListValues`（object，commonMain）

`core/storage/src/commonMain/.../StringListValues.kt:11`

- `SEPARATOR: Char = Char(31)`（:13）—— ASCII unit separator；选它的理由是 Android 旧版 `home_favorites` 存储就用它拼接，**设备上旧字节可继续读**（:5-7、12）。
- `encode(values)`（:15-16）：先剔除每项内的分隔符再 join，保证 round-trip 不虚构多出项。
- `decode(raw)`（:19-23）：null→null、空串→emptyList、其余按分隔符 split 并滤空段。

注意：它是 **public**（供测试断言线格式，`KeyValueStoreContractTest.kt:77-83`）；grep 实证 core/storage 之外没有任何模块直接引用它（各端只经 `KeyValueStore` 间接使用）。

### 3.4 `BooleanValues`（internal object，同文件）

`InMemoryKeyValueStore.kt:8-19`。仅内存 store 与「无原生 boolean 槽位的后端」的线格式：`"1"/"0"`；decode 兼容 `"1"/"true"`/`"0"/"false"`，其余回退 default（:13-18）。注释（:4-6）声称给「ohosX64 file fallback」用——**该 fallback 在 M8.5 已删除**（AGENTS.md：old `/tmp` file actuals are gone），现在实际只有 `InMemoryKeyValueStore` 在用（坑位见 §8）。

### 3.5 `InMemoryKeyValueStore`（class，commonMain）

`InMemoryKeyValueStore.kt:28-53`。基于 `mutableMapOf<String, String>`；boolean 走 `BooleanValues`、list 走 `StringListValues`。设计上**非线程安全**（:25-26 注释：它是契约演练替身，不是生产后端）。

用途：契约测试底座（`KeyValueStoreContractTest.kt:15`）与 `:core:account` 测试替身（`core/account/src/commonTest/.../AccountStoresTest.kt:18,49`）。类注释还提到「平台 store 创建失败时不得崩溃的调用方」可用它兜底（:21-23），但 grep 未发现生产代码实例化它。

### 3.6 `SettingsKeyValueStore`（internal class，settingsMain）

`core/storage/src/settingsMain/.../SettingsKeyValueStore.kt:12-38`。把 `com.russhwolf.settings.Settings` 适配到 `KeyValueStore`：String/Boolean 直映射（:15-25），list 借 `StringListValues` 编码为单 String 槽（:28-33），`remove` 直映射（:35-37）。library 类型不外泄出本模块（:6-7）。

⚠️ 其注释（:6）说「Used by the Android, iOS and ohosArm64 actuals」——已过时：ohosArm64 现在与 x64 一样走 OH_Preferences（§3.8），settingsMain 只服务 android/ios（`core/storage/build.gradle.kts:25-28`）。

被调用点：`KeyValueStore.android.kt:15`、`KeyValueStore.ios.kt:15`。

### 3.7 Android actual：`AndroidAppContext` + `KeyValueStore.android.kt`

`AndroidAppContext`（`src/androidMain/.../AndroidAppContext.kt:14-28`）：

| 成员 | 说明 | 行 |
|------|------|----|
| `applicationContext: Context?` | `@Volatile` 私有字段的可空读口 | 15-19 |
| `install(context)` | 取 `applicationContext` 存入；须在任何 store 创建前由入口 Activity 调用 | 21-23 |
| `requireContext()` | internal，未安装即 `error(...)`——注释明确「静默 no-op store 会无痕丢 session/consent」故 fail-fast | 25-27 |

该类原在 `core:account`（AndroidAccountContext），因「持久化是 Context 的最低消费者」下移到此（:9-12 注释）。

安装点：`composeApp/src/androidMain/.../MainActivity.kt:26`（`AndroidAppContext.install(this)`）。`applicationContext` 还额外被 `core/platform/src/androidMain/.../PlatformOpen.android.kt:16,25,31,45` 消费。

`createKeyValueStore` actual（`KeyValueStore.android.kt:12-16`）：每 store 名一个 SharedPreferences 文件（`MODE_PRIVATE`），包 `SharedPreferencesSettings`（:4-7 注释：store 名沿用 `demo_account` / `home_favorites` 旧文件名，旧机数据保持可读）。

### 3.8 iOS actual：`KeyValueStore.ios.kt`

`src/iosMain/.../KeyValueStore.ios.kt:14-15`：`NSUserDefaultsSettings(NSUserDefaults(suiteName = name))`。注释（:9-12）说明：改造前两 store 共用 `standardUserDefaults`，改成 suite-per-name 后不可能撞键；开发模拟器上的旧值一次性失效（列表本来就换了编码），无正式发布数据损失。

### 3.9 OHOS actual：`KeyValueStore.ohos.kt`（OH_Preferences NDK）

`src/ohosMain/.../KeyValueStore.ohos.kt`，**ohosArm64 与 ohosX64 共用**（:29-39 注释：偏好文件在应用沙箱数据目录，这是 M8.5 修复——session/consent 不再写 `/tmp` 重启即丢；故意不分架构，避免 x64 模拟器与真机持久化语义分裂）。使用的 NDK 符号从 `platform.ArkData.Preferences` import（:18-27）：`OH_PreferencesOption_Create/Destroy/SetFileName`、`OH_Preferences_Open/Delete/GetString/SetString/GetBool/SetBool/FreeString`。

| 成员 | 签名/位置 | 语义与副作用 |
|------|-----------|--------------|
| `createKeyValueStore` actual | `KeyValueStore.ohos.kt:41-42` | `PreferencesKeyValueStore(ohosPreferencesHandle(name))` |
| `PREFERENCES_OK_CODE` | :45 private const | `OH_Preferences_ErrCode.PREFERENCES_OK == 0` |
| `openedHandles` | :52 private val | name→handle 进程级缓存；handle 只开不关（:47-50 注释：调用方会随意建 store，关掉一个名会破坏其他存活视图） |
| `ohosPreferencesHandle(name)` | :54-78 private | 缓存未命中时：Create option → `SetFileName(name)` 校验返回码（:60-63）→ memScoped 内 `OH_Preferences_Open(option, errCode.ptr)` 校验指针与错误码（:64-71）→ finally Destroy option（:72-74）。失败一律 `error(...)` |
| `getString` | :83-94 | `OH_Preferences_GetString` 输出 C 分配缓冲；非 OK 码返回 null（含「key 不存在」），否则 `toKString()` 拷出；finally 里 `OH_Preferences_FreeString` 释放 C 缓冲（:90-92 注释） |
| `putString` | :96-98 | `OH_Preferences_SetString`（未检查返回码） |
| `getBoolean` | :100-104 | `OH_Preferences_GetBool`，非 OK 回退 default |
| `putBoolean` | :106-108 | `OH_Preferences_SetBool`（原生 boolean 槽，不走 BooleanValues） |
| `getStringList/putStringList` | :110-115 | 组合 `StringListValues` 编解码 + 单 String 槽（与 Settings 后端同一线格式） |
| `remove` | :117-119 | `OH_Preferences_Delete` |
| bundle/dataGroup | :37-39 注释 | 故意不设置：沙箱内系统从调用进程推导 |

链接：`-lohpreferences` 由 platform klib 自带（:30-31 注释；AGENTS.md OHOS KV 节）。

### 3.10 `KeyValueStoreContractTest`（commonTest，8 例）

`src/commonTest/.../KeyValueStoreContractTest.kt:14-85`。跑在**每个 target** 的内存后端上（:8-13 注释：平台后端只换「字节如何落盘」，这里钉死三端必须一致的行为——缺失 vs 空、列表顺序、线格式）：

| 用例 | 行 | 锁定的语义 |
|------|----|------------|
| `absentKeyReadsNull` | 18-22 | 未写 key：String 与 List 都读 null |
| `booleanFallsBackToDefaultWhenAbsent` | 25-30 | boolean 缺 key 回 default；写过则用写值 |
| `stringRoundTripKeepsStructuredPayloads` | 33-38 | 内嵌 JSON/换行的字符串原样 round-trip（真实场景 = `demo_account_user_json`） |
| `emptyStringIsNotAbsent` | 41-45 | 空串 ≠ 缺失 |
| `removeMakesKeyAbsentAgain` | 48-53 | remove 后回到「从未写入」态 |
| `listRoundTripKeepsOrderAndEmpties` | 56-63 | 列表保序；空列表读回空列表非 null |
| `separatorInsideValueCannotInventItems` | 66-70 | 值内混入分隔符被剔除：`["a\x1Fb","c"]` 读回 `["ab","c"]` |
| `listWireFormatMatchesTheLegacyAndroidEncoding` | 73-84 | 与旧 `core.platform` (char)31 join 字节一致；decode(null)=null、decode("")=emptyList |

---

## 4. KV 存储专题

### 4.1 为什么 Android/iOS 走 multiplatform-settings、OHOS 直接实现 OH_Preferences

三方证据一致（构建脚本注释、接口文档注释、openspec 提案）：

1. **两条 settings 产品线互相缺口**：官方 `com.russhwolf:multiplatform-settings:1.3.0` 没有任何 ohos klib；CPF 的 `1.3.0-0.3.0` 线又没发 Android AAR（`multiplatform-settings-android-1.3.0-0.3.0.aar` 实测 404）。**没有任何一条线能同时覆盖六个 target**（`core/storage/build.gradle.kts:3-7`；`gradle/libs.versions.toml:36-38` 同样结论的注释；`KeyValueStore.kt:19-22`）。
2. **OHOS 若走第三方库会按架构分裂**：CPF klib 只救 arm64 的话，x64 模拟器会与它「预测的真机」持久化行为不同，所以两个架构统一在 `ohosMain` 直接实现接口（`KeyValueStore.ohos.kt:34-36`；`core/storage/build.gradle.kts:6-7`）。
3. **历史问题（openspec `unify-core-kv-storage`）**：改造前 KV 是三套重复实现——account 的 `accountSettingsMain`（settings 库，OHOS 无 klib）、`core/account/src/ohosMain` 的写死 `/tmp/demo_*` 文件路径的假持久化（**重启即丢**，遗留问题编号 M8.5）、`core/platform.StringListStore` 直接摸 SharedPreferences/NSUserDefaults（`openspec/changes/unify-core-kv-storage/proposal.md`「Why」节）。结果：OHOS 真机重启掉登录态、隐私同意弹窗重复出现。
4. **收口后的形态**：`settingsMain` 中间源集只挂 android/ios（`core/storage/build.gradle.kts:18-28`）；OHOS 直连 `OH_Preferences`（应用沙箱数据目录，真持久化）。proposal 的 Impact 节确认：Android 编译产物不变、唯一行为变化是 context 未安装改为抛错；iOS 改 suite-per-name；OHOS `/tmp` 回落全删、`libkn.so` 已验证弱符号引入 `libohpreferences.so`。`specs/core-storage/spec.md` 目录存在但**为空**（实测 `ls openspec/changes/unify-core-kv-storage/specs/core-storage/` 无文件），capability 细节目前只存在于 proposal 与代码注释中。
5. **兼容性设计**：无 BREAKING——Android 文件名（`demo_account`/`home_favorites`）、键名、列表编码全部沿用（proposal「What Changes」末条；`KeyValueStore.android.kt:9-10`；`StringListValues.kt:5-7`）。

### 4.2 实际在用的 store 与 key（grep 实证）

全仓只有两个 store 名：

| store 名 | 定义处 | 平台落点 |
|----------|--------|----------|
| `demo_account` | `core/account/.../AccountSessionStore.kt:20`（`ACCOUNT_STORE_NAME`） | Android SharedPreferences 文件 / iOS suite / OHOS 偏好文件 |
| `home_favorites` | `core/platform/.../StringListStore.kt:16`（private `PLATFORM_PREFS_NAME`） | 同上 |

`demo_account` 内的 key：

| key | 定义 | 读写方 |
|-----|------|--------|
| `demo_account_user_json` | `AccountSessionStore.kt:22` | `KeyValueAccountSessionStore.readUserJson/writeUserJson/clear`（:34-46；blank 视为删除存 remove，:27-30 注释）；store 创建方 `createAccountSessionStore()`（:24-25） |
| `demo_privacy_accepted` | `core/account/.../PrivacyConsentStore.kt:17`（`PRIVACY_ACCEPTED_KEY`） | `isAccepted/setAccepted`（:25-29）；壳消费：`AppShell.kt:101-124`（splash→consent 判定 + 同意落盘），native 壳同用 `createPrivacyConsentStore` |

`home_favorites` 内的 key（经 `core/platform/StringListStore.kt` 的四个自由函数 `loadStringList:20 / saveStringList:22 / loadString:27 / saveString:29`）：

| key | 定义 | 读写方 |
|-----|------|--------|
| `home_favorite_service_ids_v2` | `feature/home/.../HomeFavoritesStore.kt:13`（`PrefKey`） | 读 `:21`、写 `:41`（List\<String\>） |
| `mine_function_order_ids` | `feature/mine/.../MinePrefsStore.kt:18` | 读 `:65`、写 `:76`（列表排序） |
| `settings.dark_mode` | `MinePrefsStore.kt:19` | 读 `:33`、写 `:87`（经 loadString/saveString 存 `"true"/"false"`，**不是** putBoolean 槽） |
| `settings.locale_zh` | `MinePrefsStore.kt:20` | 读 `:36`、写 `:93`（同上字符串布尔） |
| `current_store.selected_store_id` | `MinePrefsStore.kt:17` | 读 `:40`、写 `:46` |
| `community_convention_ack_date` | `feature/community/.../CommunityRouteHost.kt:361`（private） | 读 `:169`、写 `:228`（存 `listOf(platformTodayYmd())`，用列表槽装单元素） |
| `im_biz_to_im_user_id_json` | `feature/chat/.../ImSessionApi.kt:109`（`IM_BIZ_TO_IM_USER_ID_KEY`，对齐 Dart `SpKeys.imBizToImUserId`） | `createPersistentImMockUserIdRegistry` 的 load/save 闭包（:116-121） |
| `personalized_settings.*`（前缀 + 选项名，如 `personalized_settings.eye_protection_mode`） | `feature/mine/.../MinePersonalizedSettingsScreen.kt:30`（`PrefsPrefix`） | 读 `:33,49`、写 `:40,104` |

未接线说明：`:component` 的 `RealtimeKeyValueStore` / `InMemoryRealtimeKeyValueStore`（`component/src/commonMain/.../RealtimeSeqStore.kt:23,30`）是 realtime 模块自带的独立 KV 抽象，**未消费 `:core:storage`**——proposal Non-goals 明确本轮不动它，接线时再改为消费 `KeyValueStore`。

### 4.3 一个容易踩的类型细节

`settings.dark_mode` 等「布尔偏好」实际用 String 槽存 `"true"/"false"` 字面量（`MinePrefsStore.kt:33,87,93`），而不是 `putBoolean`。这与契约注释「读异类型 key 未定义」并存——目前没人用 boolean 槽读这些 key，所以安全；但新增消费方时必须对齐「同一个 key 同一个槽位类型」，否则 Android/iOS（原生 boolean 槽）与内存 store（`BooleanValues`）会给出不同的错读结果。

---

## 5. core/database 专题（诚实现状）

### 5.1 逐个类型

`Database`（`core/database/src/commonMain/.../Database.kt:13-22`）：`name`（:15 逻辑库名，诊断/多库隔离）；`table<T>(id)`（:18，同 id 返回同实例）；`clear()`（:21，测试/登出重置）。
`Entity`（:25-27）：`id: String` 主键。
`Table<T>`（:30-50）：`all: StateFlow<List<T>>` 全量响应式快照（:34）；`insert/update/delete/findById/query`（:37-49；`update` 语义 = 按主键覆盖 = insert，:39-40；`query` 返回随快照重发的 `Flow`，:48-49）。

`DatabaseProvider`（`DatabaseProvider.kt:9-20`）：单例工厂缝。默认 `factory = { name -> InMemoryDatabase(name) }`（:11）；`create(name = "app")`（:14）；`setFactory`（:17-19）是平台 SQL driver 就位后的**唯一**替换点（:6-7 注释：业务层与接口零改动）。

`InMemoryDatabase`（`InMemoryDatabase.kt:15-27`）：`table(id)` 用 `getOrPut` + 不安全 cast（:19-21，`@Suppress("UNCHECKED_CAST")`）；`clear()` 清表并清表注册（:23-26）。类注释（:9-13）自述：为避免在 OHOS fork 上引 SQL 依赖而做的全 target 可编译占位，**写路径非线程安全**，并发留给平台 driver。
`InMemoryTable<T>`（:30-62）：`LinkedHashMap` 主键表（:32，保插入序）+ `MutableStateFlow` 快照（:33-35）；每次写后 `refresh()` 整体替换 value（:37-39）；`update` 委托 `insert`（:46）；`findById` 直查（:53）；`query = all.map(filter)`（:55-56，谓词每次全量执行）；internal `clear`（:58-61）。

`SampleRecord`（`SampleRecord.kt:10-16`）：`@Serializable data class`，id/title/body/updatedAt——纯示例实体。
`SampleRecordDao`（:19-35）：薄门面藏泛型；`all`（:21）、`upsert→insert`（:23）、`remove→delete`（:25）、`byId`（:27）、`search(keyword)`（:30-34：trim 后空关键词返回全量，否则 title/body 忽略大小写 contains 过滤）。

测试：`InMemoryDatabaseTest.kt:16-28`（CRUD 生命周期）、`:32-42`（同表 id 同实例 `===` + clear 生效）、`:45-53`（query 谓词）；`SampleRecordDaoTest.kt:13-28`（upsert/byId/search/remove 冒烟，顺带断言共享同一张表 `:21`）。commonTest 显式补 `coroutines-core` 依赖（`core/database/build.gradle.kts:21-27`，因 commonMain 的 coroutines 是 implementation 不外泄）。

### 5.2 谁在用（答案：没人）

- 全仓没有任何模块依赖 `:core:database`（§1.2 grep 实证），没有任何源码 import `com.example.my_kmp_project.core.database`。
- 它存在的意义是**预铺的缝**：`build.gradle.kts:10-13` 注释给出理由——OHOS fork 目前没有 Room/SQLDelight klib（登记在 openspec 的 platform-gap-registry），强引 SQL 依赖会挂掉两个 ohos 架构，所以先落「接口 + 内存 driver + Provider 注入缝」。
- `SampleRecord`/`SampleRecordDao` 是**教程性质**的示例（`SampleRecord.kt:6-8` 注释：业务 feature 依赖后仿此定义自己的 Entity/Dao）——目前没有业务仿写者。
- 模块会被 CI 编译并跑两个 commonTest（它是 settings include 里的真模块，`settings.gradle.kts:33`），但产物不进任何 App。

---

## 6. core/router 逐个类型解析

### 6.1 `AppRoute` / `AppNavigator` / `LocalAppNavigator`

`AppNavigator.kt:9-24`：`sealed interface AppRoute`，13 个变体——Splash、PrivacyConsent、Login、Register、`Tab(MainTab)`、MineAbout、AllServices、Membership、`InAppWeb(url)`、Scan、Media、Live、Friend、Classroom（头注释 :5-8：类型安全路由供生产壳 Spike I；字符串 `AppRoutes` 保留给深链解析兼容）。

`AppNavigator`（:29-37）：

| 成员 | 签名 | 语义 | 实况 |
|------|------|------|------|
| 构造 | `AppNavigator(onNavigate: (AppRoute) -> Unit)`（:29-31） | 壳注入回调 | `AppContainer.kt:48-52` 的 `NavigationComponent.bind()` 创建并存单例 |
| `navigate(route)` | :32 | 转发回调 | `HomeScreen.kt:49-55` 经 `LocalAppNavigator.current` 调用 Web/Scan/Friend/Live/Classroom/Media |
| `openTab(tab)` | :34 | `navigate(AppRoute.Tab(tab))` | 未发现调用点 |
| `popToRootTab()` | :36 | `= Unit // reserved; shell clears overlay` | **空占位，全仓无调用**（grep 仅命中定义行） |

`LocalAppNavigator = staticCompositionLocalOf<AppNavigator?> { null }`（:39）：provide 点 `AppShell.kt:338-341`；consume 点 `feature/home/.../HomeScreen.kt:43`。壳的 `bind` 回调在 `AppShell.kt:240-257`——只有 InAppWeb/AllServices/Membership/Scan/Media/Live/Friend/Classroom 八类会置 `shellRoute`，其余（含 Splash/PrivacyConsent/Login/Register/Tab/MineAbout）走 `else -> Unit` 被静默忽略；这些屏在壳里由 `AppPhase`/`AuthOverlay` 私有枚举驱动（`AppShell.kt:79-91,100-130`），不用 AppRoute。

### 6.2 `AppRoutes`（object）与 `MainTab`（enum）

`AppRoutes.kt:4-35`：产品壳字符串键——Splash `/splash`（:6）、Privacy `/privacy/consent`（:10）、Auth `/auth/login` `/auth/register`（:14-15）、Home `/home`（:19）、Chat `/chat` `/chat/detail`（:23-24）、Community `/community`（:28）、Mine `/mine` `/mine/about`（:32-33）。

消费实况（grep 实证）：`AppRoutes.Auth.LOGIN` 被 `AppShell.kt:265`、`NativeAndroidMain.kt:436`、`DeepLinkRouter.kt:74` 使用；`AppRoutes.Home/Chat/Community/Mine(.MINE)`、`Chat.DETAIL` 被 `DeepLinkRouter.parse`（:57-72）与 `NativeAndroidMain.kt:467` 使用；`AppRoutes.Splash`、`AppRoutes.Privacy.CONSENT`、`AppRoutes.Mine.ABOUT` **只定义未消费**（about 的匹配走字符串字面量 `"/mine/about"`，`MineIsland.kt:26`、`MineIslandRoute.kt:23`）。

`MainTab`（:37-46）：四个 tab 枚举，携带 `route` 字符串、中文 `label`、`requiresAuth`（Chat/Community = true）。消费者：`AppShell.kt:225,289-301,309-330,389-417`（legacy Compose 壳）、`feature/shell/.../MainBottomBar.kt:75-94,169-177`（图标+选择）、`component/.../AuthGate.kt:16,22-23,41-49`（软登录挂起/恢复）、`NativeAndroidMain.kt:280-282,352-354`（native 壳 tab 映射）。注意 `requiresAuth` 枚举字段本身并未被 AuthGate 直接读——`AuthGate.requiresAuth` 用 `tab.name`/`tab.route` 前缀再判一遍（`AuthGate.kt:21-37`），枚举字段是声明性 SoT。

### 6.3 `AppRoutePath`（object，103 条 Flutter 全量表）

`AppRoutePath.kt:7-111`：注释标明来源 = Flutter SoT `my_ai_project` 的 `commons/wys_router/.../route_path.dart`（:4-6）。`const val` 全小写 camelCase（`splash "/"` :8 … `transliterWorkDetail "/video/transliter/works/detail"` :110），行 8-110 恰 103 条。它是深链解析与 dispatch 的**字符串真源**；`RoutePath` 与部分 `FeatureRoutes` 常量引用它，`DeepLinkRouter.parse` 也拿它做匹配（:61,69,75,78-85）。使用密度（grep 计数）：`NativeAndroidMain.kt` 13 处、`DeepLinkRouter.kt` 12 处、`ProductRouteDispatch.kt` 10 处、`AppShell.kt` 6 处。

### 6.4 `RoutePath`（object，便捷别名聚合）

`RoutePath.kt:8-64`：把 `AppRoutePath` 小写常量转成 PascalCase 别名（:9-16），并把 Home/Video/Classroom/Mine/Content 五组 FeatureRoutes 常量镜像进嵌套 object（:18-63）。文件头注释（:4-7）自述「Canonical full table: AppRoutePath」。**grep 实证：全仓没有任何代码引用 `RoutePath.<成员>`**（只在注释/文档字符串里出现，如 `ChatScreen.kt:58`、`MainViewController.kt:46`）——它当前是给 Swift/ArkTS 或未来调用方预留的死别名（坑位 §8）。

### 6.5 `FeatureRoutes.kt`（8 个跨 feature 路由对象）

文件头注释（:3-14）：路由常量下移到 feature 层之下，任何 `feature:*` 不必 import 另一个 feature；由各 feature 原 `internal object XxxRoutes` 提取并升为 public。⚠️ 注释里「Stays inside `:composeApp` — the `core/…` Gradle split was reverted」（:11-13）与现状不符——文件实际就在 `core/router` 模块中（历史注释未清，§8）。

| object | 行 | 内容 | 特色函数 |
|--------|----|------|----------|
| `HomeRoutes` | 17-76 | 27 条 `/home/*` 二级路径常量（含 `AllServices`，:45） | `fromLabel(label)`（:48-75）中文标签→路由；「门店」→null（:72，Flutter 行为=切店弹窗）、"新车成交"→`MineRoutes.DealInvoiceDemo`（:70） |
| `MineRoutes` | 78-154 | 30 条 Mine/商城/钱包/设置类路径；`Classroom`/`ShortVideo`/`CheckIn` 是跨组引用（:90-92） | `fromLabel`（:110-142，toast-only 项显式返回 null 并注释原因，:116-117,131-140）、`canonicalize(route)`（:145-153：`/mine/sms_template→SmsTemplates`、`/mine/qa\|/mine/buy_qa→HttpTest`、`/mine/business→Cooperation`、`/mine/reminders→Reminder`；注释警告不要把 business_card/friend 映射到 Invite——crash + 错页，:151） |
| `ContentRoutes` | 157-177 | Live `/live`、LiveRoom `/live/room`、Friend `/friend`、Music `/music/list`、MusicNowPlaying、MediaEntry `/media/entry` | `fromLabel`（:165-176，先级联 Video/Classroom/Ai 三组再自判） |
| `VideoRoutes` | 180-198 | `/video` 九条（Hub/Short/Play/Publish/Help/Transliter 四条） | `fromLabel`（:191-197） |
| `CommunityRoutes` | 200-217 | Publish/Search/Convention/ImagePreview/VideoPlay/TopicSelect/Comment 七条 | `fromLabel`（:209-216） |
| `ClassroomRoutes` | 219-235 | `/classroom/*` 八条 | `fromLabel`（:229-234） |
| `AiRoutes` | 237-244 | Stream `/ai/stream` | `fromLabel`（:240-243：AI小石头/小石头/AI） |

`MineRoutes.BuyQa = HttpTest`（:98）标注 deprecated alias；`SettingsLegacy "/mine/settings"`（:101）用于兼容旧键。

### 6.6 `DeepLinkEntry.kt` —— `acceptDeepLink(uri): Boolean`

`DeepLinkEntry.kt:9-10`：`DeepLinkRouter.accept(uri) != null` 的顶层函数（注释 :3-7：平台入口=Android intent / iOS URL / OHOS skills；导出为 Swift/ArkTS 可见的稳定符号）。调用点：

- Android：`MainActivity.kt:58-62`（`onNewIntent`/启动的 `handleDeepLinkIntent` → `acceptDeepLink(data.toString())`）。
- iOS：`iosApp/iosApp/iOSApp.swift:19,31`（`DeepLinkEntryKt.acceptDeepLink(uri:)`，Swift 壳两处）。
- iOS 另有返回路由串的通道 `AcceptDeepLinkFromIos`（`composeApp/src/iosMain/.../MainViewController.kt:67-70`，直接调 `DeepLinkRouter.accept`）。
- Push：`component/.../PushBridge.kt:68-71`（`handleNotificationClick` 从 payload 抽 `deeplink`/`url` 后 `DeepLinkRouter.accept`；OHOS push 点击仍是 TODO，:97）。
- 扫一扫当深链用：`AppShell.kt:434-435`、`NativeAndroidMain.kt:622`。
- HarmonyOS 不走 Kotlin 入口：ArkTS 侧自己 stash（`harmonyApp/entry/src/main/ets/entryability/EntryAbility.ets:17,21,24`）并在页面内解析（`harmonyApp/.../pages/Index.ets:254,259-273`），其路径规范化注释明确「Align with DeepLinkRouter.extractPath」（`harmonyApp/.../pages/NativeFeatureCatalog.ets:111`）——同一套语义、两份实现。

### 6.7 `DeepLinkRouter`（object）+ `ParsedDeepLink` + `webUrlFromDeepLink`

`DeepLinkRouter.kt:27-150`。这是唯一用 compose runtime 状态的路由对象（`mutableStateOf`，:3-5 import；所以 core/router 需要 `api(compose.runtime)`）：

| 成员 | 行 | 语义与副作用 |
|------|----|--------------|
| `pendingDeepLink: ParsedDeepLink?` | :29-30 | `by mutableStateOf`，`private set`——**可被 Compose 观察**，splash 后待消费队列 |
| `accept(uri)` | :36-40 | `parse` 成功则写入 `pendingDeepLink` 并返回；失败返回 null 不动状态 |
| `consumePending()` | :43-47 | 取走并清空（一次性） |
| `clearPending()` | :49-51 | 清空 |
| `parse(uri)` | :54-113 | **纯函数**，不动 pending |
| `extractPath(uri)` | :115-149 | 见下 |

`extractPath` 规范化管线（:115-149）：trim→若有 `://` 则把 authority 当作首段路径（`myai://mall/orders → /mall/orders`、`myai://auth/login → /auth/login`，:120-128 注释解释了为何不拆 host）→ 剥 `?query` 与 `#fragment`（:131-134）→ 去尾斜杠→空/`/` 返回 null（:136-137，**所以根路径 `/` = splash 不可被深链命中**）→ 补前导 `/` → 剥产品 host 段 `/app`（:140-144）→ **lowercase**（:148）。

`parse` 的匹配是 when 精确匹配（路径常量或去斜杠短形式，如 `"home"`），命中即 `ParsedDeepLink(rawUri, tab, route)`（:56-85）；未命中落入 else 的前缀兜底（:86-110）：`/home/*`→Home tab、`/chat/*`→Chat tab、`/community/*`→Community tab、`/mall*|/wallet*|/pay*|/settings*|/mine/*`→Mine tab、`/video|/music|/classroom|/live|/ai/*` 前缀→tab=null 仅字符串路由；其他返回 null。**注意** query 已被 extractPath 剥掉，文档里 `/chat/detail` 的 `peerName/id` 参数不在 `DeepLinkRouter` 消费——真正的 query 解析在 `component/chat/ImEngine.kt:460-496`（解析 `/chat/detail?peerName=&peerId=&id=&lastMessage=&unread=`，缺 peerName 回退「推送会话」:496）。

`ParsedDeepLink`（:158-162）：`rawUri`（原始串，供 `webUrlFromDeepLink` 重读 query）、`tab: MainTab?`（主 tab 跳转优先用）、`route: String`（字符串路由键）。

`webUrlFromDeepLink(rawUri, fallback)`（:168-192）：从 `rawUri` 的 query 里找 `url=` 参数，手工替换 `%3A/%2F/+`（:181-183，**非**完整 percent 解码），只接受 `http://`/`https://`/`data:` 前缀（:184-188），否则回 fallback。调用点：`AppShell.kt:284,444`、`NativeAndroidMain.kt:453`，fallback 传 `OfflineWebFixtureUrl`（`component/webview`，import 见 `AppShell.kt:58`）。

外部链接 → 一次跳转的完整链（legacy Compose 壳）：平台入口 `acceptDeepLink/accept` 写 pending → `AppShell.kt:260-302` `LaunchedEffect` 里 `consumePending()` → 按 `route` 分派：LOGIN/密码/OTP/注册→`AuthOverlay`（:265-280）、web→`AppRoute.InAppWeb(webUrlFromDeepLink(...))`（:281-288）、`tab != null` → 软鉴权判定（`AuthGate.requiresAuth` 未登录则记住 pending 弹登录，否则直接切 tab，:289-300）。Android native 壳对应 `NativeAndroidMain.kt:430-524`：`snapshotFlow { DeepLinkRouter.pendingDeepLink }` 观察 → `consumePending()` → 同样的 route 分支（chat detail :467-468、settings :521-524 等）。push 通知点击与扫码结果也汇入同一 `accept` 入口（§6.6）。

### 6.8 `ProductRouteDispatch` / `ProductRouteHost` / `ProductRouteTarget`

`ProductRouteDispatch.kt:9-23`：`enum ProductRouteHost { Tab, Home, Mine, Community, Content, AllServices, Web, Scan, Auth, Toast, Unknown }`——头注释（:4-8）说明这是 Native/Shared/SecondaryRouteIsland 共用的 host 归属判定，保证 Mine↔Home↔Content 跳进正确的 overlay 而不是卡错宿主。`ProductRouteTarget(path, host, toastMessage?)`（:25-30）。

`toastOnlyLabels`（:33-39）与 `toastOnlyPaths`（:41-56）：Flutter 里只弹 toast 不跳页的条目（电子名片/商务合作/粉丝群/短信模板等 + 对应 `/mine/*` 路径）。

| 成员 | 行 | 语义 |
|------|----|------|
| `resolve(raw)` | :62-110 | 输入标签/短别名/绝对路径/scheme URL → `ProductRouteTarget`。顺序：toast 名单（:66-76，含定制 toast 文案，如「切换门店」→"请在「我的」页头点击门店名称切换" :68）→ 绝对/scheme/HTTP 路径走 `stripToPath`+`canonicalizePath`+`hostForPath`（:79-84）→ 显式别名表（search/strategy/report/services/web/scan/media/friend/live/classroom/消息，:87-102）→ 依次问 Mine→Home→Community→ContentRoutes 的 `fromLabel`（:104-107）→ 兜底 `Unknown`（:109）。空串返回 null（:63-64） |
| `hostForPath(path)` | :112-133 | canonical 后精确匹配 AllServices/web/http/scan/login~auth~register/四个 tab 根路径，再按前缀归 Home/Community/Mine（含 /mall /wallet /pay /settings）/Content（/ai /video /classroom /live /friend /music /media），否则 Unknown |
| `canonicalizePath` | :140-143 | 剥 query/fragment 后过 `MineRoutes.canonicalize` |
| `stripToPath` | :145-153 | 与 extractPath 类似的 scheme 剥离（不 lowercase、不剥 `/app`） |

调用点：`NativeAndroidMain.kt:263`（openPendingRoute 的 host 大分派 :264-302，Unknown→`ShellOverlay.UnmappedEntry` 兜底页 :298-301）、`:328`（openDeferred）、`:373`（overlay 内跳转选 host）、`SecondaryRouteIsland.kt:35,39,57`（Mine 岛所有权判定）、`HomeScreen.kt:46,81`、`MineScreen.kt:41,80`。

`ProductRouteDispatchTest`（`src/commonTest/.../ProductRouteDispatchTest.kt:7-61`）5 例钉死：Mine 快捷入口三连（:9-14）、跨模块落点（二手车→Home host、小视频→Content host，:16-24）、toast-only 永不导航且带文案（:26-37）、Home 标签+别名（更多→AllServices host、AI小石头→Content、新车成交→Mine，:39-48）、路径别名 canonicalize（/mine/qa→HttpTest、/mine/sms_template→Toast，:50-59）。

### 6.9 `MineIslandRoute`（enum）

`MineIslandRoute.kt:7-64`。头注释（:3-6）：Mine Compose Island 只承载**二级页与子页**，Mine Root 是 Native Shell UI、绝不进岛（ADR 0002 岛式宿主）。10 个值：Settings/Personalized/About/Membership/Profile/Addresses/AddressEdit/Calculator/DealInvoiceDemo/DealInvoiceUpload（:8-17）。

- `toPath()`（:20-31）：枚举→规范路径（`About → "/mine/about"` 字面量 :23，其余回 `MineRoutes` 常量）。
- `fromHostKey(key)`（:34-45）：三端原生宿主用的短键（"personalized"/"about"/…），**未知名回退 Settings**（:44）。
- `fromPath(path)`（:47-62）：先 `MineRoutes.canonicalize` 再精确反查；`SettingsLegacy` 也认（:50）；非岛路径返回 null。

调用点：`MainViewController.kt:32`（iOS `MineIslandViewController`：path 优先、hostKey 兜底）；`MainArkUIViewController.kt:80-97`（OHOS cinterop `KnSetOhosHost(kind, routeCode)` 数字码 0-9 映射枚举，注释 :73-78 还标了 kind=1 走 `KnSetOhosSecondaryRoute`）→ `OhosComposeHostRequest.mineRoute`（`feature/shell/.../OhosComposeHostRequest.kt:10-13`，ArkTS 建控制器前置状态）；`NativeAndroidMain.kt:115,202,743`（Android native 侧岛状态与 `fromPath` 判定）；消费者 `feature/mine/.../MineIsland.kt:18-43`（岛 composable：stack 管理、`"/mine/about"` 特判走 `MineAboutScreen` :26-32，其余交 `MineRouteHost` :34）。

---

## 7. 路由专题收束：四层常量各管什么

| 层 | 类型 | 负责 | 不负责 |
|----|------|------|--------|
| 全量字符串 SoT | `AppRoutePath`（AppRoutePath.kt:7-111） | 与 Flutter RoutePath 一一对应的 103 条路径常量 | 不做解析、不判定 host |
| 壳主键 | `AppRoutes` + `MainTab`（AppRoutes.kt:4-46） | 产品壳四个 tab 根路径、auth 路径、tab 元数据（label/requiresAuth） | 二级页细节 |
| 跨 feature 常量 | `FeatureRoutes.kt` 8 对象 | 各域二级路径常量 + 中文标签映射 `fromLabel` + `MineRoutes.canonicalize` 别名归一 | 不知道目标屏（由 feature RouteHost 匹配） |
| 运行时解析 | `DeepLinkRouter`（外部 URI→pending）、`ProductRouteDispatch`（标签/路径→path+host）、`MineIslandRoute`（岛归属）、`AppNavigator/AppRoute`（类型安全跳转） | 把字符串变成「哪个宿主、哪张屏」 | — |

`RoutePath.kt` 是第五个「别名聚合」，但当前无人使用（§6.4）。

### 7.1 路由常量 → 目标屏对照表（RouteHost 实证；对不上的标注）

Compose 共享侧（feature RouteHost 的 when 匹配）：

| 常量（值） | 目标屏 | 匹配点 |
|------------|--------|--------|
| HomeRoutes.Search `/home/search` | HomeSearchScreen | `HomeRouteHost.kt:122` |
| Strategy `/home/strategy` | StrategyScreen | :123 |
| LearningReport `/home/learning_report` | LearningReportScreen | :124 |
| CheckInMall `/home/check_in_mall` | CheckInMallScreen | :128 |
| TransliterFeed `/home/transliter_feed` | TransliterFeedScreen | :129 |
| HotRankDetail `/home/hot_rank_detail` | HotRankDetailScreen | :133 |
| LifeService `/home/life_service` | LifeServiceScreen | :134 |
| LiveCommerce `/home/live_commerce` | LiveCommerceScreen | :135 |
| Club `/home/club` | ClubScreen | :139 |
| UsedCar / UsedCarDetail / UsedCarCreate | UsedCarListScreen / UsedCarDetailScreen / UsedCarCreateScreen | :143/:151/:156 |
| Ledger / LedgerDetail | LedgerListScreen / LedgerDetailScreen | :157/:162 |
| DataAnalytics / DataAnalyticsDetail | AnalyticsListScreen / AnalyticsDetailScreen | :166/:170 |
| TodoPartner/TodoFollowUp/TodoAfterSales/TodoOrderReview | PartnerPendingScreen / FollowUpCustomersScreen / AfterSalesAppointmentsScreen / StoreReviewOrdersScreen | :174-177 |
| AfterSales / AfterSalesCreate / AfterSalesDetail | AfterSalesListScreen / AfterSalesCreateScreen / AfterSalesServiceDetailScreen | :178/:184/:187 |
| NewCarFollow / Create / Detail | NewCarFollowListScreen / NewCarFollowCreateScreen / NewCarFollowArchiveDetailScreen | :192/:196/:201 |
| （Home 域未匹配） | CrudDetailScreen("未识别路由") | :206 |
| MineRoutes.Mall/MallDetail/MallOrders/MallOrderDetail | MallListScreen/MallDetailScreen/OrderListScreen/MallOrderDetailScreen | `MineRouteHost.kt:81/:86/:87/:91` |
| Wallet / Pay | WalletScreen / PayCheckoutScreen | :92/:93 |
| Profile / Addresses / AddressEdit | ProfileEditScreen / AddressListScreen / AddressEditScreen | :95/:99/:110 |
| Calculator | PurchaseCalculatorScreen | :111 |
| CheckIn | CheckInShortcutScreen（注释：`/home/check_in_mall` 优先走 Home 域） | :112、:287 |
| SmsTemplates / ShopQr / Poster | SmsTemplateScreen / ShopQrScreen / PosterScreen | :113/:114/:116 |
| HttpTest（含 `/mine/buy_qa`） | BuyQaScreen | :115 |
| Settings / SettingsLegacy | MineSettingsScreen | :117 |
| PersonalizedSettings | MinePersonalizedSettingsScreen | :120 |
| DealInvoiceDemo / DealInvoiceUpload | DealInvoiceDemoScreen / DealInvoiceUploadScreen | :124/:129 |
| Feedback / Cooperation / Reminder / Invite | FeedbackScreen / CooperationScreen / ReminderScreen / InviteScreen | :130-133（注：这些路径在 dispatch 层是 toast-only，岛内保留屏仅深链可达） |
| Membership `/pay/membership` | **不在 MineRouteHost**：被 `CrossFeatureScreens.handlesSiblingScreen` 拦截（`AppCrossFeatureScreens.kt:31-34,45`）→ MembershipScreen；MineRouteHost:94 注释确认 | |
| CommunityRoutes.Publish/Search/Convention/TopicSelect/ImagePreview/VideoPlay/Comment | 同名七屏 | `CommunityRouteHost.kt:93/:106/:107/:108/:116/:123/:127` |
| ClassroomRoutes 八条 | `ClassroomScreen`、`HomeworkStatsPage`、`HomeworkDetailPage`(教师)、`…`(学生)、`ClassroomSoTPage`(跟读)、`…`(礼品卡)、`…`(视频详情)、`HomeworkDetailPage`(点评) | `ClassroomRouteHost.kt:77/:81/:90/:91/:92/:97/:98/:110`，else→ClassroomScreen :115 |
| VideoRoutes 九条 | VideoHubLanding/ShortVideoPage/ShortVideoPlayPage/ShortVideoPublishPage/ShortVideoHelpPage/TransliterVideoListPage/TransliterVideoDetailPage/TransliterWorkListPage/TransliterWorkDetailPage | `VideoRouteHost.kt:132/:137/:143/:144/:148/:149/:154/:158/:162` |
| ContentRoutes.Live/LiveRoom/Friend/Music/MusicNowPlaying/MediaEntry、AiRoutes.Stream | LiveScreen（两态 :60-61）/FriendScreen/MusicListScreen/NowPlaying/MediaEntryScreen/AiStreamScreen | `AppCrossFeatureScreens.kt:54-74`（ContentRouteHost.kt:13-18 只是转发口）；else→MediaEntryScreen :74 |

壳 overlay 层（legacy Compose 壳，`AppShell.kt:421-483`）：`AppRoute.InAppWeb→InAppWebScreen` :422、`AllServices→AllServicesScreen` :426、`Membership→MembershipScreen` :429、`Scan→ScanScreen` :432（扫码结果回灌深链 :434-468）、`Media→MediaEntryScreen` :470、`Live→LiveScreen` :473、`Friend→FriendScreen` :476、`Classroom→ClassroomScreen` :479；四个 `MainTab → Home/Chat/Community/MineScreen` :389-417。

**未找到落点的路由常量**（grep 实证，仅定义无匹配屏）：`AppRoutePath.bfui*` 16 条（`/bfui/...`，AppRoutePath.kt:80-95）、`dialogDemo`、`linkingDebug`、`realtimeDebug`、`imDebug`（:66-69）、`bluetoothDemo`（:70；`MineSettingsScreen.kt:176` 注释明示「KMP: platform-gap n/a-out-of-scope」）、`homeStrategy` 之外的 `homeTodo*` 已对上有屏。`/settings/*` 深链会被 dispatch 归 Mine host（ProductRouteDispatch.kt:125-127），未识别的进 native `UnmappedEntry` 兜底页（NativeAndroidMain.kt:298-301）。`AppRoutes.Splash`、`AppRoutes.Privacy.CONSENT` 无深链/匹配消费（§6.2）。

---

## 8. 真实状态与坑（汇总）

1. **core:database 是「编译着但没人用」的预备缝**：无消费方、无业务仿写；文档或需求如果假设「有实体落盘」，当前一律是内存——进程重启即失（`InMemoryDatabase.kt:12-13` 自述非线程安全 + 无落盘）。接入 SQL driver 的动作点只有 `DatabaseProvider.setFactory`（`DatabaseProvider.kt:17-19`）。
2. **storage 模块内两处过时注释**：`SettingsKeyValueStore.kt:6` 仍写「Used by the Android, iOS and ohosArm64 actuals」（OHOS 两架构现均走 OH_Preferences，`KeyValueStore.ohos.kt:29-36`）；`InMemoryKeyValueStore.kt:4-6` 仍提「the ohosX64 file fallback」（`/tmp` 回落在 M8.5 已删除）。行为不受影响，读代码时以 `core/storage/build.gradle.kts:1-9` 与 openspec proposal 为准。
3. **`libs.versions.toml:97` 注释源集名过时**：「Android/iOS via accountSettingsMain」——实际源集叫 `settingsMain`（`core/storage/build.gradle.kts:18`）；`accountSettingsMain` 是 unify 前的旧名（proposal「Why」§1）。
4. **`FeatureRoutes.kt:11-13` 头注释与现状矛盾**：写「Stays inside `:composeApp`」，文件却在 `core/router`。层界校验脚本另有其事（`scripts/check-layer-deps.sh` 存在，FeatureRoutes.kt:13 引用）。
5. **openspec `unify-core-kv-storage/specs/core-storage/` 是空目录**：capability spec 未落文件，规则散在 proposal 与代码注释——评审该变更时没有独立 spec.md 可读。
6. **死代码/未接线路由件**：`RoutePath` 别名零引用（§6.4）；`AppRoute.Splash/PrivacyConsent/Login/Register/Tab/MineAbout` 六变体无消费路径（`AppShell.kt:240-257` else 吞掉）；`AppRoutes.Splash`、`Privacy.CONSENT`、`Mine.ABOUT` 未引用；`AppNavigator.popToRootTab` 是 `= Unit` 占位且无人调用（`AppNavigator.kt:36`）；`openTab` 亦未发现调用点。
7. **toast-only 名单双份维护**：`MineRoutes.fromLabel` 的 null 分支（FeatureRoutes.kt:116-140）与 `ProductRouteDispatch.toastOnlyLabels/Paths`（ProductRouteDispatch.kt:33-56）语义同源但物理分离，新增「Flutter 只弹 toast」项要同时改两处，`ProductRouteDispatchTest.kt:26-37` 只护得住 dispatch 侧。
8. **深链 query 参数不在 DeepLinkRouter 消费**：`extractPath` 直接剥掉 `?/#`（DeepLinkRouter.kt:131-134）；web 的 `url=` 靠 `webUrlFromDeepLink` 从 rawUri 重读且只做 `%3A/%2F/+` 三样替换（:181-183）——含其他 percent 编码的 URL 会原样带过（可能不符合预期）；chat detail 参数由 `ImEngine.kt:466-496` 的另一套解析器处理。
9. **OHOS KV 的实际限制**：`putString/putBoolean/remove` 未检查 NDK 返回码（`KeyValueStore.ohos.kt:96-98,106-108,117-119`），写失败静默；`openedHandles` 是普通 `mutableMapOf`（:52），跨线程首次并发创建同名 store 存在竞态；handle 永不关闭（:47-51 有意为之）。Bundle/dataGroup 依赖调用进程上下文（:37-39）。
10. **iOS 一次性数据断代**：suite-per-name 改造后开发模拟器旧 `standardUserDefaults` 值不再被读（`KeyValueStore.ios.kt:9-12`），真机发布无此顾虑。
11. **HarmonyOS 深链是双实现**：ArkTS 自己解析（`Index.ets:259-273`、`NativeFeatureCatalog.ets:111` 注释「Align with DeepLinkRouter.extractPath」），不 import Kotlin `acceptDeepLink`；改 `extractPath` 规则时 ArkTS 侧必须同步，否则两端深链行为漂移。
12. **`MineIslandRoute.fromHostKey` 未知名静默回退 Settings**（MineIslandRoute.kt:44）——原生侧传错 key 不会报错，只会「永远打开设置页」，调试时注意。
13. **`HomeRoutes`/`MineRoutes` 中文标签映射是行为对齐层不是字典**：多个 null 分支对应 Flutter 的 toast/弹窗行为（FeatureRoutes.kt:72,116-117,131-140），删除任何 null 分支等于把「不跳转」改成「乱跳转」；`ProductRouteDispatchTest` 是其主要回归保险。

## Why

KV 持久化在本仓是三套重复实现，且 OHOS 那套是假持久化：

1. `core/account` 的 `accountSettingsMain` —— android/ios 用 `com.russhwolf:multiplatform-settings`，OHOS 无该 klib；
2. `core/account/src/ohosMain` 的 `AccountSessionStore.ohos.kt` / `PrivacyConsentStore.ohos.kt` —— 写死 `/tmp/demo_*` 文件路径，**重启即丢**（长期遗留，登记为 M8.5）；
3. `core/platform` 的 `StringListStore` —— 自己直接摸 SharedPreferences / NSUserDefaults，收藏列表与会话/隐私同意不同源。

结果：同一类"存一个字符串"的能力有三份代码、三种落盘语义，OHOS 真机重启后掉登录态、隐私同意弹窗重复出现。用户已定：本轮只做 KV，不引入 SQLDelight，不建第二个持久化模块。

## What Changes

- **`:core:storage` 从占位模块变成唯一 KV 模块**：`commonMain` 定义 `KeyValueStore`（只有 String / Boolean / List&lt;String&gt; 三个槽位）+ `expect fun createKeyValueStore(name: String)`；`InMemoryKeyValueStore` 留在 commonMain 作测试替身。
- **两个薄后端**：android/ios 经新中间源集 `settingsMain` 用 `multiplatform-settings`；**两个 OHOS 架构**（ohosArm64 + ohosX64）在 `ohosMain` 直接实现同一接口，落到 `OH_Preferences` NDK API（应用沙箱数据目录）。
- **列表编码上提共享**：`StringListValues`（分隔符 = `Char(31)`）进 commonMain，与既有 Android 磁盘字节完全一致。
- **消费方收口**：`:core:account` 改为纯 commonMain（删 `accountSettingsMain` / `ohosMain` / `androidMain` / `iosMain` 四个源集）；`core.platform.StringListStore` 内部委托 `KeyValueStore`，公开函数签名不变，调用方零改动。
- `AndroidAccountContext` → `core.storage.AndroidAppContext`（未安装即 fail-fast），MainActivity 安装点同步。
- iOS 由 `standardUserDefaults` 改为 `suiteName = name`，两个 store 不再可能撞键。
- **无 BREAKING**：Android 文件名（`demo_account` / `home_favorites`）、键名、列表编码不变，真机数据继续可读。

## Capabilities

### New Capabilities

- `core-storage`: 跨端 KV 契约——命名 store、三值槽位、空串≠缺失、三端落盘语义一致（session / 隐私同意 / 收藏列表）。

### Modified Capabilities

- （无）现有 specs 未规定持久化实现；`platform-bridges` 只描述桥接，不含 KV。

## Impact

- **Android**：编译产物不变；唯一行为改动是 context 未安装时抛错（原来静默 no-op）。
- **iOS**：suite-per-name + 列表重新编码 ⇒ 开发模拟器里的旧值一次性失效（可接受，无正式发布数据）。
- **OHOS**：`/tmp` 文件回落全部删除；`libkn.so` 已验证弱符号引入 `libohpreferences.so` + `OH_Preferences_*`，与既有 ArkUI 调用同一套运行时解析机制；两个架构同源，不再有 arm64 持久 / x64 内存的分裂。
- **依赖 / 文档**：`gradle/libs.versions.toml` 注释固化"无一条 settings 线覆盖六目标"的结论；AGENTS.md、`MODULES.md`、`openspec/config.yaml`、`docs/architecture/ohos-aggregate.md` 的源集清单同步为 `settingsMain`。
- **测试**：`core/storage` 契约测试 8 例 + `core/account` 3 类 7 例（含旧键快照断言），commonTest 三端同源。

## Non-goals

- 不引入 SQLDelight / 关系存储；M8.3–M8.4 仍需先出评估文档。
- 不建第二个持久化模块，不做通用仓储/DAO 层。
- 不改 `RealtimeKeyValueStore`（`:component`，仅内存实现、未接产品链路）——接线时再改为消费 `KeyValueStore`。
- 不做 KV 加密、键迁移框架、`ObservableSettings` 响应式读。
- 不把 settings 升到 CPF 线（已核实该线无 android AAR）。

# 构建 / 依赖 / 打包 / 安全 四块基础设施方案

> 范围：`/Users/qpy/Desktop/github/my_kmp_project`（Android + iOS + HarmonyOS KMP/CMP）。
> CI/CD 只使用 GitHub Actions；环境矩阵、产物、缓存、签名与密钥都按 Actions 的 runner / Secrets 设计。
> 实施避让 `feature/home` 与 `composeApp/app/di`（另一条工作线正在收尾）。

## 1. 现状判断

### 1.1 多环境

| 位置 | 现状 | 问题 |
|------|------|------|
| `core/network/.../DemoApiHosts.kt` | 有 `DEV/TEST/STAGING/PRODUCT`，但前三个都指向 `http://127.0.0.1:8080` | 没有真正的环境差异；环境值写死在源码里 |
| `core/network/.../NetworkConfig.kt` | Release 分支硬编码 `PRODUCT` | 无法构建 staging/test 的 release 包 |
| `core/network/.../NetworkBootstrap.kt` | `isRelease` 时强制 Product | 环境与构建类型耦合 |
| `core/network/.../NetworkBootstrap.android.kt` | `platformIsReleaseBuild() = false`、版本写死 `"1.0"/"1"` | release 包被当成 debug 处理，客户端头上报错误 |
| `composeApp/build.gradle.kts` | 版本号来自 `local.properties` / 环境变量，默认 `1/"1.0"` | 没有统一真源；iOS `Config.xcconfig` 与 Harmony `app.json5` 各写各的 |
| `harmonyApp/AppScope/app.json5` | `versionCode=1000000` / `versionName=1.0.0` | 与 Android/iOS 不一致 |

结论：需要一条**构建期环境通道**（Gradle 属性 → 生成 Kotlin 常量 → 三端 actual 读取），并把环境与构建类型解耦。

### 1.2 依赖管理

- 版本目录集中（`gradle/libs.versions.toml`，51 个 library、7 个 plugin），但 **15 个 library 别名 + `cocoapods` plugin 未被引用**。
- 仓库声明分两处：`settings.gradle.kts`（3 个源）与根 `build.gradle.kts` 的 `subprojects {}`（5 个源，带 eazytec 优先与 AGP 排除）。两处顺序不同，排障时容易看错。
- 没有依赖锁定 / 校验和、没有 Dependabot/Renovate、没有「禁止硬编码坐标」的门禁。
- 注意：本机 `buildSrc` 不可用（`~/.gradle/init.gradle` 镜像解析不到 `kotlin-dsl` marker），依赖治理必须落在 settings / 脚本 / CI，而不是 convention plugin。

### 1.3 自动化打包

- CI 现有 `static / android / ios / harmony / release` 五个 job。
- Android 只产出 **debug APK**；`release` job 把 debug APK 挂到 GitHub Release（`.github/workflows/ci.yml`）。
- 签名配置已在 `composeApp/build.gradle.kts` 落地（`local.properties` / 环境变量四件套），但没有任何 CI 任务真正产出 **signed release APK/AAB**。
- iOS：`iosApp/Configuration/Config.xcconfig` 的 `TEAM_ID` 为空，没有 archive/export 脚本。
- Harmony：`scripts/harmony-signing.sh` 已把签名材料移出 git；`publish*BinariesToHarmonyApp` 需手工执行，HAP 构建依赖 DevEco（托管 runner 没有）。
- 产物没有统一命名、SHA256、保留策略。

### 1.4 应用安全

| 项 | 现状 |
|----|------|
| Android 明文流量 | 已收敛到 debug（`src/debug` overlay + `androidDebug/res/xml/network_security_config.xml`）；main 没有显式 release 配置 |
| Android 备份 | `allowBackup="true"`，无 `dataExtractionRules` / `fullBackupContent` |
| Android 混淆 | release `minifyEnabled=true` + proguard；`shrinkResources` 关闭（Compose 资源在 assets，保持现状） |
| 支付沙箱 | `PayFeatureFlags.sandboxEnabled` 默认 `true`，release 也能返回 Sandbox Success |
| 日志 | Ktor Logging 在 release 为 `NONE`；`AppLog` release 默认 WARN；已有脱敏测试 |
| iOS | `NSAllowsArbitraryLoads=false`（好）；缺 `PrivacyInfo.xcprivacy`；无签名/Provisioning |
| Harmony | `backup_config.json` 为 `allowToBackupRestore: true`；无 release 明文策略说明 |
| 密钥扫描 | 无 secrets scanning；`.gitignore` 已排除 `local.properties`、`*.keystore`、`.env*` |

## 2. 目标形态

### 2.1 多环境 + 版本单一真源

```
gradle/app.env.properties            # 提交入库：默认环境、三环境 host、版本号（无密钥）
        │  -Papp.env / -Papp.release / -Papp.versionCode / -Papp.versionName / APP_ENV ...
        ▼
gradle/build-environment.gradle.kts  # 生成 core/network commonMain 的 BuildEnvironment.kt
        ▼
core/network/BuildEnvironment.kt     # ENV / IS_RELEASE / VERSION_* / API_*
        ▼
DemoApiHosts / NetworkBootstrap / 三端 actual
```

- 环境取值：`dev | test | staging | prod`。
- 默认：`-Papp.release=true` → `prod`；否则 `dev`。CI 与打包脚本显式传 `-Papp.env`。
- Release 安全兜底：release 构建只接受 `https://` host；非 https 自动回落到 `prod` 并记一条 `AppLog` 错误；运行时 `baseUrlOverride` 在 release 被忽略。
- 版本：`app.versionName` / `app.versionCode`；CI 用 `GITHUB_RUN_NUMBER` 覆盖 `versionCode`。
- iOS 版本通过 `xcodebuild MARKETING_VERSION/CURRENT_PROJECT_VERSION` 注入；Harmony 由 `packaging 脚本`写入 `app.json5`（构建后自动还原）。
- **配置缓存**：`generateBuildEnvironment` 的 `doLast` 不捕获脚本对象，任务输入用 Provider 读取 `gradle/app.env.properties`；`local.properties` / `app.env.properties` 在 `composeApp` 中也通过 `providers.fileContents` 读取，文件变化会让配置缓存失效。不要求 `--no-configuration-cache`。

### 2.2 依赖治理

- 仓库唯一权威：`settings.gradle.kts` 的 `dependencyResolutionManagement`；`repositoriesMode = FAIL_ON_PROJECT_REPOS`，模块内再写 `repositories {}` 直接失败。
- 版本目录清理：删除未引用别名；`scripts/check-dependencies.sh` 门禁：
  1. 模块构建脚本禁止硬编码 `group:artifact:version`；
  2. 版本目录 `version.ref` 必须存在、key 不重复；
  3. 未引用别名必须在 `gradle/dependency-allowlist.txt` 登记（只减不增）；
  4. 仓库只允许出现在 `settings.gradle.kts`。
- `.github/dependabot.yml`：Gradle + GitHub Actions，周更、分组、限 PR 数。
- 依赖锁定 / verification metadata 作为后续可选项：KMP native/metadata 配置变动频繁，先不锁，避免假红。

### 2.3 自动化打包（GitHub Actions 矩阵）

| 目标 | Runner | 触发 | 产物 |
|------|--------|------|------|
| Android debug | `ubuntu-latest` | push / PR / 手动 | `composeApp-debug-apk`（保留现状） |
| Android release（signed） | `ubuntu-latest` | `v*` tag / 手动（选 env） | `*-release.apk` + `*.aab` + `SHA256SUMS` |
| iOS compile | `macos-latest` | push / PR | 仅 klib 编译检查（不变） |
| iOS archive/IPA | `macos-latest` | 手动，`vars.ENABLE_IOS_PACKAGING=true` | `*.xcarchive` → `*.ipa`（Secrets 提供证书） |
| Harmony compile | `macos-latest` | push / PR | 仅 klib 编译检查（不变） |
| Harmony HAP | `self-hosted`（DevEco） | 手动，`vars.ENABLE_HARMONY_PACKAGING=true` | `*.hap` + `SHA256SUMS` |

- 签名材料只从 Actions Secrets / 本机私有文件读取；仓库只保留读取入口与模板。
- 统一产物命名 `MyKmpProject-<env>-<versionName>-<versionCode>-<buildNumber>.<ext>`。
- Release job 用 `apksigner verify` 验签后再上传；`SHA256SUMS` 一并发布。

Secrets / Variables 约定：

| 名称 | 类型 | 用途 |
|------|------|------|
| `ANDROID_KEYSTORE_BASE64` | Secret | release keystore（base64） |
| `ANDROID_KEYSTORE_PASSWORD` | Secret | store password |
| `ANDROID_KEY_ALIAS` | Secret | key alias |
| `ANDROID_KEY_PASSWORD` | Secret | key password |
| `IOS_CERTIFICATE_P12_BASE64` | Secret | iOS 分发证书 |
| `IOS_CERTIFICATE_PASSWORD` | Secret | 证书密码 |
| `IOS_PROVISIONING_PROFILE_BASE64` | Secret | mobileprovision |
| `IOS_TEAM_ID` | Secret | Apple Team ID |
| `ENABLE_IOS_PACKAGING` | Variable | `true` 才注册 iOS 打包 job |
| `ENABLE_HARMONY_PACKAGING` | Variable | `true` 才注册 Harmony 打包 job |

### 2.4 应用安全

- Android：显式 release `network-security-config`（`cleartextTrafficPermitted=false`）；debug overlay 保留本地明文；`allowBackup=false` + `dataExtractionRules` / `fullBackupContent` 排除凭据；release 禁止 sandbox 支付。
- iOS：补 `PrivacyInfo.xcprivacy`（无跟踪、无采集声明由业务确认后填写）；ATS 保持禁明文。
- Harmony：`backup_config.json` 关闭敏感数据备份（按需保留白名单）；release 只允许 https。
- 供应链：`scripts/check-secrets.sh` 扫描私钥/keystore/常见密钥模式，接入 CI static job。
- 发布 runbook：本文第 4 节；证书轮换、吊销与回滚写在同节。

## 3. 分批实施与验收

| 批次 | 内容 | 验收 |
|------|------|------|
| B1 环境/版本真源 | `gradle/app.env.properties`、`gradle/build-environment.gradle.kts`、`BuildEnvironment` 生成、三端 actual、`DemoApiHosts`/`NetworkConfig` 改造 | `./gradlew :core:network:testDebugUnitTest -PandroidOnly=true`；`assembleDebug`；`-PandroidOnly=false` 下 `compileKotlinIosSimulatorArm64` 通过；release 非法 http host 回落 prod |
| B2 依赖治理 | settings 仓库收敛 + `FAIL_ON_PROJECT_REPOS`；删除 15 个未用别名 + `cocoapods`；`scripts/check-dependencies.sh`；Dependabot | `scripts/check-dependencies.sh` 绿；`:help` 与窄构建解析正常 |
| B3 打包与 Actions | `scripts/package-android.sh` / `package-ios.sh` / `package-harmony.sh`；CI 环境矩阵；signed release + 验签 + SHA256；Release 上传正式产物 | 无密钥时脚本给出明确非零退出；CI 静态 job 与 debug job 可复跑；release job 只在 tag/手动触发 |
| B4 安全加固 | network config / backup rules / sandbox release 闸门 / `PrivacyInfo.xcprivacy` / `check-secrets.sh` / runbook | `check-secrets.sh` 绿；`assembleDebug` 与 `assembleRelease`（未签名）成功；Manifest 合并检查通过 |

**完成定义**：四批全部实施；B1–B4 验收命令可重复执行；需要真实证书/账号的步骤只留入口与说明，不伪造成功。

## 4. 发布 Runbook（摘要）

1. **Android**：`./scripts/package-android.sh --env prod --version-code "$GITHUB_RUN_NUMBER"`。脚本要求四件套签名材料；产出 APK/AAB、`SHA256SUMS`，并调用 `apksigner verify`。
2. **iOS**：在 macOS 上 `./scripts/package-ios.sh --env prod --team-id <TEAM_ID>`。需要 `.p12` + `.mobileprovision`；CI 由 Secrets 注入。脚本把 `APP_ENV/APP_RELEASE/APP_VERSION_*` 导出给 Xcode，`Compile Kotlin Framework` phase 会转发成 `-Papp.*`，保证 archive 内嵌的 `BuildEnvironment` 与本次 env 一致。
3. **Harmony**：先 `./gradlew :composeApp:publishReleaseBinariesToHarmonyApp -Papp.release=true -Papp.env=prod`，再 `./scripts/package-harmony.sh`（调用 `harmony-signing.sh` + DevEco `hvigorw`）。
4. **回滚**：按版本号回退 tag；证书轮换后更新 Secrets，旧包不再签新版本；Harmony/iOS 重新导出 profile。

## 5. 实施记录与验收证据

| 批次 | 状态 | 关键改动 | 本地验证 |
|------|------|----------|----------|
| B1 环境/版本 | 完成 | `gradle/app.env.properties`、`gradle/build-environment.gradle.kts`、`core/network` 生成 `BuildEnvironment`、`DemoApiHosts/NetworkConfig/NetworkBootstrap` + 三端 actual | `generateBuildEnvironment` 首次 stored / 二次 reused；`-Papp.env=staging -Papp.release=true -Papp.versionCode=42` 生成值正确；`:core:network:testDebugUnitTest`、`:core:network:compileKotlinIosSimulatorArm64`、`:core:network:compileKotlinOhosArm64`、`assembleDebug` 通过 |
| B2 依赖治理 | 完成 | settings 仓库收敛 + `FAIL_ON_PROJECT_REPOS`；删 15 个别名 + `cocoapods`；`scripts/check-dependencies.sh`；`.github/dependabot.yml` | 门禁 OK；仓库收敛后 `:core:network:testDebugUnitTest` 通过 |
| B3 打包与 Actions | 完成 | `package-android.sh` / `package-ios.sh` / `package-harmony.sh`；共享 scheme；CI 增加 `android-release`、`ios-package`、`harmony-package` 与 Release 上传 | 三个脚本 `bash -n` 通过；无签名材料时 Android 脚本退出码 2 并给出说明；CI YAML 解析通过；`assembleRelease`（未签名）通过 |
| B4 安全加固 | 完成 | release network config（明文 false）、debug overlay `tools:replace`、`allowBackup=false` + backup/data-extraction 规则、sandbox 支付 release 闸门、`PrivacyInfo.xcprivacy`、Harmony backup 关闭、`check-secrets.sh` | merged release manifest：`usesCleartextTraffic=false` / `allowBackup=false`；debug manifest：`usesCleartextTraffic=true`；`:component:testDebugUnitTest` 通过；secrets 扫描 OK |

## 6. 已知边界

- iOS / Harmony 的真实签名、Provisioning、开发者账号仍是外部前置；仓库只提供脚本与 Secrets 入口，没有凭据时明确失败（Android 未配 Secrets 时由 CI 构建 unsigned 包并打 warning）。
- Dependabot 不感知 CPF OHOS fork 的私有镜像坐标，升级仍需人工跑三端编译。
- 依赖锁定 / verification metadata 暂未启用：KMP native/metadata 配置变动频繁，先以「catalog 唯一真源 + 门禁」治理，避免假红。
- Harmony release 的 `IS_RELEASE` 依赖打包时传入 `-Papp.release=true`；DevEco 直接构建 debug HAP 不会带该标记。

# 登录策略批次 3 验收报告 — 微信登录契约层

- **日期**：2026-11-26
- **范围**：微信登录策略契约、可用性、注册表装配、授权码通道抽象
- **结论**：**通过（契约层）**
- **明确不做**：微信开放平台 SDK、appId 配置、Android 壳改动、真机微信授权

---

## 1. 需求与验收项

| # | 验收项 | 结果 | 证据 |
|---|--------|------|------|
| 1 | 微信策略实现统一 `LoginStrategy` 接口 | 通过 | `WechatLoginStrategy` |
| 2 | 提供可用性与不可用原因 | 通过 | `availability()`；未配置通道返回 `Unavailable("微信登录未配置授权通道（缺少 appId / SDK 适配器）")` |
| 3 | 注册到策略注册表 | 通过 | `AuthStrategies.registry`、`app/di/AuthModule.kt` |
| 4 | 凭据未配置时策略不可用 | 通过 | 默认 `provider = null` → `Unavailable`，不会发起 HTTP |
| 5 | 授权码走现有通道抽象 | 通过 | `AuthorizationCodeProvider.requestCode()` → `AuthorizationCodeResult` |
| 6 | 不引入微信开放平台依赖 | 通过 | 无 `com.tencent.mm.opensdk` 等依赖；仅复用既有 HTTP 契约 |
| 7 | 不改 Android 壳 | 通过 | 未修改 `MainActivity`、manifest、`NativeAndroidMain.kt` |

---

## 2. 实现说明

### 2.1 策略契约

文件：`feature/auth/src/commonMain/kotlin/com.example.my_kmp_project/feature/auth/LoginStrategies.kt`

- `id = "wechat"`、`displayName = "微信登录"`、`interactive = true`、`order = 40`
- 构造参数：
  - `api: UserAuthHttpApi`
  - `provider: AuthorizationCodeProvider?`，默认取 `PlatformLoginCapabilities.providerOrNull("wechat")`
- 可用性规则：
  - `provider != null` → `LoginAvailability.Available`
  - `provider == null` → `LoginAvailability.Unavailable(...)`
- 登录凭据：
  - `LoginCredentials.Interactive`：拉起 provider 取码
  - `LoginCredentials.ExternalCode(code, deviceId)`：原生壳已自行取码，直接换会话
  - 其他凭据类型：`LoginError.InvalidInput("请使用微信授权登录")`

### 2.2 通道授权码映射

| Provider 返回 | 策略行为 |
|---------------|----------|
| `Success(code)` | 调用 `POST /api/v1/user/wechat/login` |
| `Cancelled` | 返回 `LoginOutcome.Cancelled`，不弹错误 |
| `Unavailable(reason)` | 返回 `LoginError.Unavailable("wechat", reason)` |
| `Failure(message)` | 返回 `LoginError.ProviderRejected(message)` |
| 协程取消 | 原样上抛 `CancellationException` |
| 空授权码 | `LoginError.ProviderRejected("微信授权未返回授权码")` |

### 2.3 注册表

- `AuthStrategies.registry`：`password(10)`、`phone_otp(20)`、`huawei(30)`、`wechat(40)`
- `app/di/AuthModule.kt`：显式静态注册 `WechatLoginStrategy`
- 未注册微信 provider 时：
  - Compose 登录页不会从注册表渲染微信按钮（仅保留原有占位按钮，行为未变）
  - `AuthBridge.availableLoginMethodsJson()` 不包含 `wechat`

### 2.4 复用能力

- 登录主流程、会话落库、错误清理仍由既有 `LoginOrchestrator` / `LoginSessionEstablisher` 统一处理
- 微信换票复用 `UserAuthHttpApi.loginWithWechat(...)`
- `ExternalCode.deviceId`（批次 2 增加）用于原生壳透传设备标识

---

## 3. 验证记录

### 3.1 单元测试

命令：

```bash
./gradlew :feature:auth:testDebugUnitTest \
  --no-configuration-cache -PandroidOnly=true
```

结果：**BUILD SUCCESSFUL，109 个测试，0 失败，0 错误**

新增微信相关用例：

- `wechat_is_unavailable_without_authorization_channel`
- `wechat_interactive_uses_channel_code_and_wechat_endpoint`
- `wechat_cancel_from_channel_is_not_an_error`
- `wechat_external_code_works_without_kotlin_channel`

### 3.2 Android 编译

```bash
./gradlew :composeApp:compileDebugKotlinAndroid \
  --no-configuration-cache -PandroidOnly=true
```

结果：**BUILD SUCCESSFUL**

### 3.3 鸿蒙共享代码发布

```bash
./gradlew :composeApp:publishDebugBinariesToHarmonyApp \
  --no-configuration-cache --no-build-cache
```

结果：**BUILD SUCCESSFUL**

说明：批次 3 未改 OHOS 平台代码，发布目的仅是让共享注册表与 `libkn.so` 保持同步。

> 构建参数说明：并行工作区新增的 `gradle/build-environment.gradle.kts` 与 configuration cache 存在冲突，因此关闭 configuration cache；与本次微信契约改动无关。

---

## 4. 设计边界与已知限制

1. **当前微信在 UI 与编排层均不可用**
   - 未注册 `AuthorizationCodeProvider("wechat")`，所以 `availability()` 为 `Unavailable`；
   - `LoginOrchestrator` 在调用策略前会检查可用性，因此通过 `AuthBridge.loginWithMethod("wechat", code, deviceId)` 走外部授权码时同样会被判为不可用。
   - 这是“凭据未配置时不可用”的预期行为；正式接入时需注册 provider（或按批次 4 方案放行显式外部凭据）。

2. **策略在构造时解析 provider**
   - `AuthModule` 中策略为懒加载单例；若未来在策略首次创建之后才注册 provider，需要在批次 4 改为动态解析（每次 `availability()` / `login()` 重新查询 `PlatformLoginCapabilities`）。

3. **未接入 SDK 与 appId**
   - 未新增微信开放平台依赖；
   - 未读取或提交任何 appId / Universal Link / 签名配置。

4. **未做真机验证**
   - 批次 3 明确只做契约层，真机微信授权留给后续批次。

---

## 5. 后续批次建议

1. **批次 4**：注册 `AuthorizationCodeProvider("wechat")`，接入 Android/iOS 微信 SDK；appId 从本机私有配置读取，不入 git。
2. 将 `WechatLoginStrategy` 的 provider 解析改为动态查询，支持“UI 首次渲染后再注册 SDK”的时序。
3. 为 `AuthBridge.loginWithMethod` 补充原生壳侧联调：iOS Swift 取码后传 `ExternalCode`，必要时在编排层放行显式外部凭据。
4. 微信可用后，`availableLoginMethodsJson()` 会自动包含 `wechat`，Compose 与 ArkTS 的“其他登录方式”列表无需改代码。

---

## 6. 结论

批次 3 的微信登录契约层按范围完成：策略接口、可用性、注册表、授权码通道抽象与单测均已落地；未引入 SDK、未配置 appId、未改 Android 壳。验收通过。

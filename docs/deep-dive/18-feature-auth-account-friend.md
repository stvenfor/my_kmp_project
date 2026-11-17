# 18 · 登录鉴权域：feature/auth + core/account + feature/friend

> 引用约定：本文每条事实都附「仓库相对路径:行号」，区间为闭区间。省略号 `…` 只代表源码目录前缀
> `src/commonMain/kotlin/com.example.my_kmp_project/…`，不改变文件名。所有行号以当前工作树为准。
> 本文覆盖 35 个文件：`feature/auth/`（1 个 `build.gradle.kts` + 18 个主源 kt + 4 个 commonTest kt）、
> `core/account/`（1 个 `build.gradle.kts` + 4 个主源 kt + 1 个 commonTest kt）、
> `feature/friend/`（1 个 `build.gradle.kts` + 3 个主源 kt + 2 个 commonTest kt）。

## 1. 三块定位与依赖实证

### 1.1 各自定位

| 模块 | 定位 | 证据 |
|------|------|------|
| `core/account` | 会话（access/refresh/session/device 四件套）与隐私同意旗标的**唯一本地持久化 SoT**；纯 commonMain，无 expect/actual | `core/account/build.gradle.kts:1-7`、`core/account/build.gradle.kts:14-22`（只声明 commonMain） |
| `feature/auth` | 登录/注册/OTP 的 HTTP 契约 + 会话编排 + 4 个 Compose 屏 + 401 静默续期策略 + 原生壳回调桥 | `feature/auth/build.gradle.kts:1`（模块自述）、`feature/auth/…/AuthRepository.kt:22` |
| `feature/friend` | 通讯录 UI（纯 Mock 数据）+ 好友关系 6 条 BFF HTTP 契约 + 会话聚合 | `feature/friend/build.gradle.kts:1-2` |

三者层级：`core/account` 是横向底座，`feature/auth` 与 `feature/friend` 是同层业务模块，
彼此**不得**互相依赖（`AGENTS.md:35`「no sibling feature deps」，由 `scripts/check-layer-deps.sh` 机器把关）。
`feature/friend` 里那段与 `feature/chat` 同形的 JSON 取值函数是**刻意复制**的，注释直接写明理由：
`feature/friend/…/ImFriendApi.kt:277-278`。

### 1.2 谁依赖 `core/account`（逐条实证）

| 依赖方 | 依赖形态 | 声明位置 | 说明 |
|--------|----------|----------|------|
| `component` | `api(projects.core.account)` | `component/build.gradle.kts:40` | 纵向端口层以 api 透出，`AuthSessionActions.kt`/`AuthSessionState.kt` 直接读写 `AccountFacade` |
| `core/platform` | `api(projects.core.account)` | `core/platform/build.gradle.kts:18` | WebView 桥 `GetToken` 取 `AccountFacade.current().token`（`core/platform/…/WebBridgeHost.kt:63`） |
| `composeApp` | `implementation(projects.core.account)` | `composeApp/build.gradle.kts:123` | 装配层直接创建 `createPrivacyConsentStore()`（`composeApp/…/AppShell.kt:40`、`:101`） |
| `feature/auth` | **传递可见**（无直接声明） | `feature/auth/build.gradle.kts:18` | 经 `api(projects.core.ui)` → `core/ui/build.gradle.kts:17` `api(projects.core.platform)` → `core/platform/build.gradle.kts:18` `api(projects.core.account)` |
| `feature/friend` | **传递可见**（无直接声明） | `feature/friend/build.gradle.kts:15` | 同上链路；friend 实际只用到 `component` 的 IM 引擎，不碰 `AccountFacade` |
| Gradle 注册 | `include(":core:account")` | `settings.gradle.kts:25` | — |

`core/account` 自己的出边只有两条实现依赖：`implementation(projects.core.network)`
（`core/account/build.gradle.kts:17`，为的是复用 `NetworkJson` 序列化器与 `NetworkFacade.bindAccessToken`）
与 `implementation(projects.core.storage)`（`:18`，为的是 `KeyValueStore`）。因为是 `implementation`，
**不会**把 core.network / core.storage 泄漏给下游。

### 1.3 三个 `build.gradle.kts` 逐点

| 文件 | 关键点 | 行号 |
|------|--------|------|
| `core/account/build.gradle.kts` | 插件 3 个（kotlinMultiplatform / androidLibrary / kotlinSerialization） | `:9-11` |
| | `android.namespace = "com.example.my_kmp_project.core.account"` | `:25` |
| | 头注释记录：旧版每平台一份 actual（OHOS 曾把会话写到 `/tmp`，重启即丢）已删除，`accountSettingsMain` 中间源集随之删除 | `:3-5` |
| `feature/auth/build.gradle.kts` | 5 个插件（多一个 composeMultiplatform + composeCompiler） | `:7-12` |
| | `api(projects.core.ui)` / `api(projects.core.network)` / `api(projects.component)` | `:18` / `:20` / `:21` |
| | `implementation(libs.kotlinx.serialization.json)`，注释说明 core.network 对 serialization 是 implementation 不传递，故自备 | `:22-24` |
| | commonTest 额外 `implementation(projects.core.router)`（契约测试用 `MainTab`） | `:26-29` |
| | `android.namespace = "…feature.auth"` | `:34` |
| `feature/friend/build.gradle.kts` | 4 个插件，**无** kotlinSerialization 插件（`ImFriendApi` 手写 `JsonObject` 取值，不用 `@Serializable` 网络 DTO） | `:5-10` |
| | `api(projects.core.ui)` / `api(projects.core.network)` / `api(projects.component)` | `:15` / `:17` / `:19` |
| | 头注释：`ImEngine` / `ImEngineStore` 引擎抽象在 `:component`，本模块只做 API 与 UI | `:1-2` |
| | `android.namespace = "…feature.friend"` | `:27` |

## 2. 文件清单

### 2.1 `core/account`（6 个文件）

| 路径 | 作用 | 关键行号 |
|------|------|----------|
| `core/account/build.gradle.kts` | 模块接线（27 行） | `:8-12` 插件、`:16-20` 依赖、`:24-26` namespace |
| `core/account/src/commonMain/kotlin/com.example.my_kmp_project/core/account/AccountFacade.kt` | 会话门面（233 行） | 接口 `:13-33`、对象 `:35-198`、`LoggedOut :200-213`、`LoggedInUser :215-232` |
| `…/AccountSessionStore.kt` | 会话快照 KV 存储（48 行） | 键名 `:20`、`:22`、工厂 `:24-25`、实现 `:31-47` |
| `…/AccountUserSnapshot.kt` | 可序列化快照 DTO（68 行） | 字段 `:9-21`、`toSession :24-41`、`fromSession :44-59`、编解码 `:61-65` |
| `…/PrivacyConsentStore.kt` | 隐私同意旗标存储（31 行） | 键名 `:17`、工厂 `:19-20`、实现 `:22-30` |
| `core/account/src/commonTest/kotlin/com.example.my_kmp_project/core/account/AccountStoresTest.kt` | 两个存储 + 存储名的行为钉死（79 行） | 三组类 `:17-46`、`:48-70`、`:73-78` |

### 2.2 `feature/auth`（23 个文件）

| 路径（省略 `feature/auth/src/commonMain/kotlin/com.example.my_kmp_project/feature/auth/`） | 作用 | 关键行号 |
|---|---|---|
| `AuthApiContracts.kt` | 契约表 #1–#7 的路径/请求/响应 DTO、手机号与校验工具、设备上下文、失败文案映射（475 行） | 路径 `:14-38`、请求体 `:41-90`、`AuthUserDto :101-141`、`AuthLoginData :147-153`、`AuthRegisterData :162-211`、刷新 `:225-261`、`toPayload :286-316`、`AuthPhoneUtils :318-336`、`AuthValidators :338-347`、`AuthDeviceContext :349-364`、`mapAuthFailure :373-448`、`isConnectionFailure :453-474` |
| `UserAuthHttpApi.kt` | 纯 HTTP 层，返回 `Result<DTO>`，不落库（238 行） | 错误类型 `:18-22`、头缺口 `:32-35`、`login :40-53`、`register :59-83`、`sendPhoneOtp :86-96`、`verifyPhoneOtp :99-112`、`loginWithWechat :121-132`、`refresh :142-164`、`logout :174-185`、`postLogin :191-202`、`guard :216-236` |
| `AuthRepository.kt` | 会话编排：校验 → 调用 → 落库 / 失效清理 + Mock 短路（384 行） | 各方法见 §3.3 |
| `AuthTokenRefresh.kt` | 单飞静默刷新 + 策略实现（106 行） | 状态 `:25-28`、`policy :31-37`、`handler :40`、`refresh :46-61`、`performRefresh :63-93`、`handleRefreshExhausted :99-104` |
| `AuthSessionBootstrap.kt` | 幂等装机入口（29 行） | `:13-27` |
| `AuthHttpDecision.kt` | 401/403 统一分类（72 行） | 枚举 `:15`、`classifyAuthHttp :21-42`、`looksLikeExpiredToken :45-54`、`isAuthHttpHardFailure :59-71` |
| `AuthBizCode.kt` | BFF 业务码常量与谓词（48 行） | 常量 `:13-34`、谓词 `:36`/`:39`/`:42`/`:46` |
| `AuthFailure.kt` | 语义分类的失败家族（66 行） | 密封类 `:7-9`、18 个具体失败 `:13-48`、`fromBizCode :52-57`、`isSessionGoneFailure :62-65` |
| `AuthBridge.kt` | 原生壳（SwiftUI / ArkTS）回调桥（151 行） | 对象 `:18`、作用域 `:19`、读态 `:21-24`、登出 `:26-28`、`applySession :31-58`、7 个动作 `:60-130`、取值 `:132-136`、投递 `:138-149` |
| `MockAuthConfig.kt` | 本地 Mock 认证开关与假数据（94 行） | 白名单 `:27-33`、固定码 `:36`、`:39`、展示名 `:42-48`、开关 `:56`、`issueToken :87-90` |
| `AuthPendingCredentials.kt` | 跨屏暂存账号（10 行，进程级可变全局） | `:7-9` |
| `AuthFormWidgets.kt` | 登录/注册共用表单组件与文案（364 行） | token `:42-57`、问候 `:60-72`、分段器 `:74-128`、输入框 `:130-188`、主按钮 `:190-226`、微信按钮 `:228-258`、隐私行 `:260-311`、错误行 `:313-325`、底部链 `:327-362` |
| `LoginScreen.kt` | 登录枢纽页（邮箱/短信二选一，302 行） | 签名 `:42-48`、状态 `:52-65`、副作用 `:67-76`、发码 `:196-231`、提交 `:252-286` |
| `LoginPasswordScreen.kt` | 枢纽后的下划线密码页（165 行） | 签名 `:36-42`、账号回填 `:44-49`、条件账号输入 `:74-106`、提交 `:139-154` |
| `LoginOtpScreen.kt` | 6 位验证码页（179 行） | 签名 `:41-46`、号码取自 `:52-53`、Mock 提示 `:90-96`、重发 `:129-152`、提交 `:158-173` |
| `RegisterScreen.kt` | 注册页（邮箱/手机二选一，314 行） | 签名 `:41-46`、状态 `:47-61`、发码 `:202-234`、提交 `:254-298` |
| `UserProfileContracts.kt` | 契约表 #8–#11 资料/门店 DTO（308 行） | 路径 `:14-23`、`profileMeQuery :29-30`、`UserProfileData :40-84`、`UserStoreStatsData :94-170`、`UserStoreListItemData :172-205`、`UserStoreListData :213-228`、`UserProfileUpdateBody :238-265`、`UserStoreSwitchBody :268-271`、`mapProfileFailure :279-307` |
| `UserProfileHttpApi.kt` | 资料 lane 的 HTTP 层（114 行） | `:19`、`fetchMe :27-34`、`listMyStores :43-46`、`switchStore :52-59`、`updateMe :68-75`、`requireData :78-85`、`profileFailure :87-92` |
| `feature/auth/build.gradle.kts` | 模块接线（35 行） | `:7-13`、`:17-25`、`:26-30`、`:34` |
| `commonTest/…/AuthHttpDecisionTest.kt` | 决策/谓词/文案回归（192 行） | 类 `:12`、403 矩阵 `:56-84`、刷新 `:89-117`、硬失败 `:131-139`、谓词 `:144-155`、文案回归 `:160-190` |
| `commonTest/…/AuthSessionClearTest.kt` | AuthGate / 会话清理单测（82 行） | 说明 `:12-15`、gate `:19-24`、一次性消费 `:27-32`、登出 `:35-41`、强登出 `:44-54` |
| `commonTest/…/AuthUserContractJsonTest.kt` | 契约 #1–#7 JSON 钉死（364 行） | 路径相等 `:35-44`、4 键登录体 `:47-62`、别名优先级 `:65-142`、无 user 降级 `:145-155`、注册双形态 `:169-245`、刷新省略 `:250-277`、OTP `:305-338`、微信 `:343-362` |
| `commonTest/…/UserProfileContractJsonTest.kt` | 契约 #8–#11 JSON 钉死（364 行） | 前缀拆分 `:36-42`、query 省略 `:47-53`、别名 `:56-158`、整数钳位 `:161-188`、PATCH 省略 `:201-234`、10001 保会话 `:250-264`、切店 `:269-292`、`/me/stores` `:297-362` |

### 2.3 `feature/friend`（6 个文件）

| 路径 | 作用 | 关键行号 |
|------|------|----------|
| `feature/friend/build.gradle.kts` | 模块接线（28 行） | `:5-10`、`:14-20`、`:21-23`、`:27` |
| `feature/friend/src/commonMain/kotlin/com.example.my_kmp_project/feature/friend/FriendDirectory.kt` | 3 个种子好友 + 2 个可搜索联系人 + 建会话（59 行） | `Entry :11-25`、种子 `:28-32`、目录 `:35-38`、`ensureChat :40-46`、`ensureGroupChat :49-57` |
| `…/FriendScreen.kt` | 通讯录列表页 + 详情页（452 行，**纯 Mock**） | `FriendItem :46-52`、`FriendMockData :65-68`、入口 `:70-88`、列表 `:90-294`、私件 `:296-413`、详情 `:415-451` |
| `…/ImFriendApi.kt` | 契约表 #103–#108 好友 HTTP 层（294 行） | 路径 `:23-38`、DTO `:40-101`、类 `:117-119`、6 方法 `:122-213`、错误翻译 `:224-247`、`getItemsApi :259-275`、取值私件 `:279-293` |
| `feature/friend/src/commonTest/…/FriendDirectoryTest.kt` | 目录与建会话钉死（35 行） | `:10-16`、`:18-23`、`:25-33` |
| `feature/friend/src/commonTest/…/ImFriendApiContractTest.kt` | 好友 6 端点契约（354 行） | 契约体 `:26-257`、`FakeFriendApiClient :263-324`、`runNow :334-349` |

## 3. 逐文件函数级详解

### 3.1 `core/account`

#### 3.1.1 `AccountFacade.kt`

| 成员 | 签名 | 内部行为 / 副作用 | 被谁调用 |
|------|------|-------------------|----------|
| `interface AccountSession` | `:13-33`，13 个只读属性 | 无行为，纯契约。`token :16`、`refreshToken :19`（空即无法 refresh）、`sessionId :22`（单设备互踢）、`deviceId :25`、`userId :27`、`avatarUrl :28`、`phone :29`、`email :30`、`companyId :31`、`orgId :32` | 全仓会话读取方 |
| `current()` | `fun current(): AccountSession` `:44-47` | 先 `ensureHydrated()` 再返回内存快照 | `AuthBridge.kt:21`/`:24`/`:132`/`:134`/`:136`、`AuthSessionState.kt:35`、`AuthRepository.kt:211`、`AuthTokenRefresh.kt:64`/`:100`、`AppShell.kt:230`/`:293`/`:310`/`:392`/`:403`、`WebBridgeHost.kt:63`/`:80`、`MineProfileLogic.kt:28`/`:31`、`HomeWebHandlers.kt:35` |
| `setSession(value)` | `:49-58` | `ensureHydrated` → 赋内存 `:51` → `persist` 落 KV `:52` → 若已登录 `NetworkFacade.bindAccessToken(value.token)` `:54`，否则 `clearAccessToken()` `:56` | `AuthRepository.kt:365`（commitSession）、`AuthBridge.kt:42`（applySession） |
| `logout()` | `:60-65` | 内存置 `LoggedOut` `:62` → `store.clear()` `:63` → `clearAccessToken()` `:64`。**不**发远端登出请求 | `AuthSessionActions.kt:13`（登出事务三件套之一）、`AuthRepository.kt:271`/`:284`、`AuthTokenRefresh.kt:101` |
| `updateAuthTokens(token, refreshToken, sessionId)` | `:71-90` | 未登录直接 return `:74`；只换 token 三件套，资料字段逐个原样搬运 `:77-87`；最终仍走 `setSession`（故会重新绑头 + 落盘） | `AuthTokenRefresh.kt:86` |
| `updateProfile(displayName?, avatarUrl?)` | `:93-112` | 未登录 return `:96`；`displayName ?: cur.displayName` `:99`、`avatarUrl ?: cur.avatarUrl` `:105` | `MineProfileLogic.kt:20`（改昵称） |
| `updateProfilePhone(phone)` | `:114-133` | 同上，仅覆盖 `phone` `:127` | **零调用者**（见 §8.3） |
| `updateProfileEmail(email)` | `:135-154` | 仅覆盖 `email` `:149` | **零调用者** |
| `updateCompanyOrg(companyId?, orgId?)` | `:156-176` | 未登录 return `:159`；**两者皆 null 时直接 return** `:160`（即无法用它清空组织） | **零调用者** |
| `ensureHydrated()` | `private :178-188` | 一次性：`store.readUserJson()` `:180` → `AccountUserSnapshot.decode` `:181` → `toSession() ?: LoggedOut` `:182` → 若已登录**重新绑 token** `:185` → `hydrated = true` `:187` | 所有公开方法首行 |
| `persist(value)` | `private :190-197` | `fromSession` 为 null 或 `!hasValidToken` 时 `store.clear()` `:192-194`，否则 `store.writeUserJson(encode(...))` `:195` | `setSession` |
| `LoggedOut` | `data object :200-213` | 13 个字段全 null / 空串，`isLoggedIn = false` `:201` | 默认值 `:37`、`:62`、`:182` |
| `LoggedInUser` | `data class :215-232` | `isLoggedIn = token.isNotBlank()` `:228`；额外派生 `canRefresh = refreshToken.isNotBlank()` `:231`（全仓目前**无人读**该属性） | `AuthRepository.kt:366`、`AuthBridge.kt:43`、`AccountFacade.kt:75`/`:97`/`:118`/`:139`/`:161`、`AccountUserSnapshot.kt:26` |

内存字段 `session` 与 `hydrated` 都标了 `@Volatile`（`:36-37`、`:39-40`），`store` 是 `by lazy`（`:42`）
——首次触碰 `AccountFacade` 才创建 KV。

#### 3.1.2 `AccountSessionStore.kt`

| 成员 | 语义 | 行号 |
|------|------|------|
| `interface AccountSessionStore` | `readUserJson(): String?` / `writeUserJson(json: String?)` / `clear()` | `:13-17` |
| **`ACCOUNT_STORE_NAME = "demo_account"`** | KV **存储名**，即 Android SharedPreferences 文件名 / iOS `NSUserDefaults(suiteName:)` / OHOS Preferences 文件名 | `:20` |
| **`ACCOUNT_USER_JSON_KEY = "demo_account_user_json"`** | 会话快照 JSON 的**唯一键名** | `:22` |
| `createAccountSessionStore()` | `KeyValueAccountSessionStore(createKeyValueStore(ACCOUNT_STORE_NAME))` | `:24-25` |
| `internal class KeyValueAccountSessionStore` | `readUserJson` 直接 `getString(ACCOUNT_USER_JSON_KEY)` `:34`；`writeUserJson` **空白串等价于删键** `:36-42`；`clear` = `remove` `:44-46` | `:31-47` |

三端 KV 行为一致的根据在 `:6-12` 的头注释：实现下沉到 `:core:storage`，
接口见 `core/storage/…/KeyValueStore.kt:28-52`，工厂是 `expect createKeyValueStore`（`:58`），
actual 分别是 `KeyValueStore.android.kt:12-16`（SharedPreferences 文件名 = store 名）、
`KeyValueStore.ios.kt:14-15`（`NSUserDefaults(suiteName = name)`）、
`KeyValueStore.ohos.kt:41-42` + 句柄打开 `:54-66`（`OH_Preferences` NDK）。

#### 3.1.3 `AccountUserSnapshot.kt`

| 成员 | 语义 | 行号 |
|------|------|------|
| `data class AccountUserSnapshot` | 11 个可空字段：`userId/token/refreshToken/sessionId/deviceId/nickname/avatarUrl/phone/email/companyId/orgId` | `:9-21` |
| `nickname` | **持久化的展示名字段**（内存侧叫 `displayName`，落盘改名） | `:15` |
| `hasValidToken` | `!token.isNullOrBlank()` | `:22` |
| `toSession()` | 有效 token → `LoggedInUser`（`displayName = nickname` `:27`，其余 `orEmpty()` 兜底 `:29-31`）；否则 `LoggedOut` `:40` | `:24-41` |
| `fromSession(session)` | `!session.isLoggedIn` → **返回 null**（不落盘）`:45`；`nickname = session.displayName` `:52` | `:44-59` |
| `encode` | `NetworkJson.encodeToString(snapshot)`——`core/network` 的 `explicitNulls = false` 实例，null 字段不写键 | `:61-62` |
| `decode(raw)` | `runCatching { NetworkJson.decodeFromString… }.getOrNull()`——脏数据静默降级为未登录 | `:64-65` |

#### 3.1.4 `PrivacyConsentStore.kt`

| 成员 | 语义 | 行号 |
|------|------|------|
| `interface PrivacyConsentStore` | `isAccepted(): Boolean` / `setAccepted(accepted: Boolean)` | `:12-15` |
| **`PRIVACY_ACCEPTED_KEY = "demo_privacy_accepted"`** | 同意旗标键名 | `:17` |
| `createPrivacyConsentStore()` | `KeyValuePrivacyConsentStore(createKeyValueStore(ACCOUNT_STORE_NAME))` `:20`——**与会话共用同一个 `demo_account` 存储**，注释 `:8-11` 解释这是为了保住 Android 上已有数据 | `:19-20` |
| 实现 | `isAccepted` = `store.getBoolean(PRIVACY_ACCEPTED_KEY)` `:25`（默认 false）；`setAccepted` = `putBoolean` `:27-29`。**能写 false，即支持撤回记录** | `:22-30` |

#### 3.1.5 `AccountStoresTest.kt`

| 测试 | 钉死什么 | 行号 |
|------|----------|------|
| `AccountSessionStoreTest.storesSnapshotUnderTheLegacyKey` | 快照写在 `ACCOUNT_USER_JSON_KEY` 下（旧键名不能改，否则老设备数据失联） | `:21-26` |
| `…blankSnapshotMeansSignedOut` | 写 `""` 与写 `null` 都变成「键不存在」 | `:28-37` |
| `…clearDropsTheSnapshot` | `clear()` 后备存储里也查不到 | `:39-45` |
| `PrivacyConsentStoreTest.startsNotAccepted` | 默认未同意 | `:52-55` |
| `…acceptanceIsPersistedUnderTheLegacyKey` | 写入落在 `PRIVACY_ACCEPTED_KEY` | `:57-62` |
| `…withdrawingAcceptanceIsRemembered` | `setAccepted(false)` 后 `isAccepted()` 恒 false（撤回被记住） | `:64-69` |
| `AccountStoreNameTest.usesTheDemoAccountStore` | 断言字面量 `"demo_account"` | `:74-77` |

三组类都注入 `InMemoryKeyValueStore`（`:18`、`:49`），因此这些测试**不触碰真实平台 KV**。

### 3.2 `feature/auth` — 契约层

#### 3.2.1 `AuthApiPaths`（`AuthApiContracts.kt:14-38`）

| 常量 | 值 | 契约号 | 行号 |
|------|----|--------|------|
| `LOGIN` | `/api/v1/user/login` | #1 | `:16` |
| `REGISTER` | `/api/v1/user/register` | #2 | `:19` |
| `SEND_PHONE_OTP` | `/api/v1/user/phone/otp/send` | #5 | `:22` |
| `VERIFY_PHONE_OTP` | `/api/v1/user/phone/otp/verify` | #6 | `:25` |
| `WECHAT_LOGIN` | `/api/v1/user/wechat/login` | #7 | `:28` |
| `HUAWEI_LOGIN` | `/api/v1/user/huawei/login` | **契约表外**，本仓既有扩展 | `:30-31` |
| `LOGOUT` | `/api/v1/user/logout` | #4 | `:34` |
| `REFRESH` | `/api/v1/user/refresh` | #3 | `:37` |

#### 3.2.2 请求体（全部由 `NetworkJson` 序列化）

| DTO | 键集合 | 行号 | 备注 |
|-----|--------|------|------|
| `AuthPasswordLoginRequest` | `username,password,device_id,platform` **四键总发** | `:41-47` | `device_id` 靠 `@SerialName` `:45` |
| `AuthPhoneOtpSendRequest` | `{phone}`（E.164 由调用方归一） | `:50-53` | — |
| `AuthPhoneOtpVerifyRequest` | `{phone,otp,device_id,platform}` | `:56-62` | — |
| `AuthRegisterRequest` | `{username,password,email,device_id,platform}` | `:65-72` | — |
| `AuthWechatLoginRequest` | `{code,device_id,platform}` | `:78-83` | 注释 `:74-77` 明确「与华为同形状但勿复用」 |
| `AuthHuaweiLoginRequest` | `{code,device_id,platform}`，`platform` **默认 `"android"`** | `:85-90` | `:89` |
| `AuthRefreshRequest` | `refresh_token` 必填；`device_id/session_id/platform` 空白时**整键省略** | `:232-253` | 省略靠 `of()` 把空白折成 null（`:241-251`）+ `explicitNulls=false`（注释 `:229-230`） |

响应 DTO：`AuthUserDto :101-141`（双大小写别名 + `resolved*` 取值，`:116-140`）、
`AuthLoginData :147-153`、`AuthRegisterData :162-211`（**双形态**：带 `user` 走嵌套、不带就把根当用户，
`resolvedUser()` `:184`、`hasSession` `:181`）、`AuthLoginPayload :213-223`、
`AuthRefreshData :256-261`、`AuthRegisterPayload :263-275`。
`firstPresent(vararg String?)` 在 `:278-284`（空白也当缺失，与 Dart 只判 null 不同，差异说明在 `:97-98`）。
`toPayload()` 两个扩展在 `:286-299` 与 `:305-316`。

工具与谓词：`AuthPhoneUtils :318-336`（大陆号 `^1[3-9]\d{9}$` 在 `:319`；`toE164China :328-335`）、
`AuthValidators :338-347`（`MIN_PASSWORD_LENGTH = 6` `:342`、OTP `^\d{6}$` `:346`）、
`AuthDeviceContext :349-364`（`deviceId() = "kmp-<platform>-device"` `:350`；
**OHOS 上报 `android`**，因为 Go `LoginRequest` 当前 `oneof=android|ios`，见 `:359-360`）。
`mapAuthFailure(code,message,statusCode)` 在 `:373-448`——先走 `classifyAuthHttp` `:379-404`，
再按 10003/10002/验证邮件/验证码/用户已存在/参数/Supabase/50000/连接失败逐条给中文文案 `:405-447`。
`isAuthBusinessSuccess(code) = code == 0 || code == NetworkCodes.OK` `:450-451`；
`isConnectionFailure` 用 14 个关键词表兜底 `:453-474`。

#### 3.2.3 `UserAuthHttpApi.kt`

`public object UserAuthHttpApi`（`:37`），职责边界写在 `:28-30`：**只做请求/应答映射，不落库、不清会话**。
文件级 `// GAP(R5)`（`:32-35`）记录：Dart 的 logout 手工带 `Authorization` / `X-Session-ID` / `X-Device-ID`，
而 KMP 只有 `DemoRequestHeaders`（`core/network/…/DemoRequestHeaders.kt:9-12`）注入 `Authorization`，
`ApiClient` 没有逐请求头出口 → **`X-Session-ID` / `X-Device-ID` 目前发不出去**。

| 方法 | 端点 | 成功判据 | 行号 |
|------|------|----------|------|
| `login(username,password,deviceId,platform)` | #1 | `code` 成功 **且** `data != null` **且** `token.isNotBlank()` | `:40-53` → 共用 `postLogin :191-202`（判据 `:197`） |
| `register(username,password,email,deviceId,platform)` | #2 | `code` 成功且 `data != null`（**不要求 token**，因为注册可能只回资料） | `:59-83`（判据 `:78`） |
| `sendPhoneOtp(phone)` | #5 | 只看 `code`，`data` 恒 null，走 `postApiOk` | `:86-96`（`:87`） |
| `verifyPhoneOtp(phone,otp,deviceId,platform)` | #6 | 同 login 三条件 | `:99-112` |
| `loginWithWechat(code,deviceId,platform)` | #7 | 同 login 三条件 | `:121-132`，方法级 `GAP(R5)` `:114-119` |
| `refresh(raw,refreshToken,deviceId?,sessionId?,platform?)` | #3 | 三条件 + **必须用未装饰 `raw` client** | `:142-164`（`:149`、判据 `:159`），理由写在 `:134-141` |
| `logout()` | #4 | 只看 `code`；`postApi(path, body = "", parseData = { })` 即**无 body** | `:174-185`（`:175-179`） |
| `authFailure(response)` | — | 用 `mapAuthFailure` 出中文，同时**原样带上 `code` 与 `statusCode`** 供会话决策 | `:205-210` |
| `guard { }` | — | `AuthHttpFailure` 原样；`CancellationException` **必须重抛** `:222-224`；`NetworkError.Transport` 映射 `:225-231`；其它兜底 `:232-235` | `:216-236` |

`AuthHttpFailure(message, code = null, statusCode = null)` 在 `:18-22`——它和 `AuthFailure` 的分工写在 `:11-17`：
前者携带原始码供决策，后者是语义分类。

#### 3.2.4 `AuthBizCode.kt` / `AuthFailure.kt` / `AuthHttpDecision.kt`

| 码 | 含义 | 行号 | 谓词 |
|----|------|------|------|
| 10001 | 参数错误 | `:13` | — |
| 10002 | 未授权 | `:16` | — |
| 10003 / `FORBIDDEN` 别名 | 账号未注册（与 HTTP 403 同值，故决策只看 status） | `:19`、`:20` | — |
| 10021 | 会话被顶号 | `:23` | `isSessionReplaced :39` |
| 10022 | 会话无效 | `:26` | — |
| 10023 | token 无效 | `:29` | `isTryRefresh :46` |
| 10024 | OTP 错 | `:32` | — |
| 50000 | 内部错误 | `:34` | — |
| — | 「须强登出」= 10021 或 10022 | — | `isForceLogout :36` |
| — | 「会话已废」= 10002/10021/10022/10023 | — | `isSessionGone :42` |

`AuthFailure` 是 `sealed class :7-9` 下 18 个具体失败（`:13-48`），其中 `SessionReplaced :42`、
`SessionInvalid :45`、`SessionCleared :48` 三个是会话级；`fromBizCode(code)` 只映射其中 4 个（`:52-57`）；
`AuthFailure?.isSessionGoneFailure()` 判定上述三个（`:62-65`）。

`AuthHttpDecision`（`:15`，4 值）与 `classifyAuthHttp`（`:21-42`）是 401/403 的**唯一入口**，
优先级为「业务码 → HTTP status → 文案兜底」，写在注释 `:13` 与 `:18`：

1. `isForceLogout(code)`，或 `code == null` 且文案含「其他设备登录」/「会话无效」→ `ForceLogout` `:26-31`；
2. `statusCode == 403` → `Forbidden`（**不登出**）`:32-34`；
3. `isTryRefresh(code)` 或 `statusCode == 401` 或（`code == null` 且 `looksLikeExpiredToken`）→ `TryRefresh` `:35-40`；
4. 其余 → `None` `:41`。

`looksLikeExpiredToken` 是旧包无 code 时的文案兜底（8 个关键词，`:45-54`）；
`isAuthHttpHardFailure` = `ForceLogout || TryRefresh`，**403 不算硬失败**（`:59-71`，注释 `:56-58`）。

#### 3.2.5 `UserProfileContracts.kt` / `UserProfileHttpApi.kt`

路径三条（`ProfileApiPaths :14-23`）：`ME = /api/v1/profiles/me` `:16`、
`ME_STORE = /api/v1/profiles/me/store` `:19`、`MY_STORES = /api/v1/me/stores` `:22`——
注意 `:11-12` 的警告：前两条走 `/profiles/` 前缀，第三条走 `/me/` 前缀，**不可拼字符串**。

| 成员 | 行为 | 行号 |
|------|------|------|
| `profileMeQuery(storeId?)` | 去空白后为空则返回空 map（键省略） | `:29-30` |
| `UserProfileData` | 驼峰/下划线双别名 + `resolved*`；`resolvedStats()` 缺省回 `ZERO` | `:40-84`（`:83`） |
| `UserStoreStatsData` | 6 组双别名整数一律 `coerceAtLeast(0)`（`:164-165`），`labelOfRole` 出中文角色 | `:94-170`（`ZERO :154`、`:157-161`） |
| `UserStoreListItemData.isCurrent` | **只认字面 `true`**（`:204`） | `:172-205` |
| `UserStoreListData` | `rows` 缺省空表、`currentStoreId` 双别名钳位 | `:213-228` |
| `UserProfileUpdateBody.of(...)` | 改昵称时**同时写 `user_name` 与 `display_name` 两键** | `:239-265`（`:254-259`） |
| `DEFAULT_AVATAR_MIME` | `"image/jpeg"`，且 mime 只在 `avatarBase64` 非空时一起发 | `:263` |
| `UserStoreSwitchBody` | `{store_id: Int}` | `:268-271` |
| `mapProfileFailure` | 10001 → 「参数错误」等中文映射 | `:279-307` |

HTTP 层（`UserProfileHttpApi :19`）：`fetchMe(storeId?)` `:27-34`、`listMyStores()` `:43-46`、
`switchStore(storeId: Int)` `:52-59`、`updateMe(request)` `:68-75`（**只有 PATCH，无 PUT/POST 变体**）、
`requireData` `:78-85`、`profileFailure` `:87-92`、`guard` `:95-112`。
**这一整条 lane 在生产代码里没有任何调用点**（全仓 grep 只命中它自身与 `UserProfileContractJsonTest.kt`），
详见 §8.3。

### 3.3 `feature/auth` — 会话编排层

`AuthRepository` 是 `object`（`:22`，无 `public` 修饰），文件头 `:12-21` 说清分工：
HTTP 契约归 `UserAuthHttpApi`，本对象只做「校验 → 调用 → 落库/失效清理」，并解释 Mock 分流语义。

| 方法 | 签名与返回 | 内部行为（按行） | 调用者 |
|------|------------|------------------|--------|
| `loginWithPassword` | `suspend (account: String, password: String): Result<Unit>` `:24-44` | 空账号 `:26-28`；含 `@` 但邮箱非法 `:29-31`；密码 < 6 `:32-34`；**Mock 短路** `:35-37`；否则 `UserAuthHttpApi.login(...)` 带 `AuthDeviceContext.deviceId()/platform()` `:38-43`，再 `.commitLogin()` | `LoginScreen.kt:275`、`LoginPasswordScreen.kt:147`、`AuthBridge.kt:67`、`MainArkUIViewController.kt:167` |
| `sendPhoneOtp` | `suspend (phone): Result<Unit>` `:47-62` | 号码非法即拒 `:48-50`；Mock 下只认白名单 `:51-58`；真实分支把号码转 E.164 `:60` 并 `guardSessionGone()` `:61` | `LoginScreen.kt:210`、`LoginOtpScreen.kt:138`、`RegisterScreen.kt:216`、`AuthBridge.kt:90`、`MainArkUIViewController.kt:214` |
| `loginWithOtp` | `suspend (phone, code): Result<Unit>` `:65-81` | 号码 `:66-68`、OTP 6 位 `:69-71`、Mock 短路 `:72-74`、真实走 #6 `:75-80` | `LoginScreen.kt:277`、`LoginOtpScreen.kt:166`、`AuthBridge.kt:79`、`MainArkUIViewController.kt:191` |
| `register` | `suspend (email, password, displayName = ""): Result<Unit>` `:88-135` | 邮箱/密码校验 `:94-99`；`username` 取昵称，否则取邮箱 local-part `:100-102`；Mock 下「注册即登录」`:103-106`；真实分支 `toPayload()` 后**有 token 就 commitSession，无 token 就回落一次密码登录** `:114-134`（回落写在 `:128-131`） | `RegisterScreen.kt:287`、`AuthBridge.kt:103`、`MainArkUIViewController.kt:238` |
| `registerWithPhone` | `suspend (phone, code): Result<Unit>` `:138-139` | **单行委托 `loginWithOtp`**，没有独立注册端点（Flutter `registerWithPhone` 同款） | `RegisterScreen.kt:289`、`AuthBridge.kt:115` |
| `loginWithHuaweiCode` | `suspend (code, deviceId): Result<Unit>` `:148-178` | 空码拒 `:150-152`；`deviceId` 空白回落到上下文 `:153`；**直连 `NetworkFacade.api().postApi`，不经 `UserAuthHttpApi`** `:155-158`（理由 `:145-146`：该端点在契约表外）；失败走 `handleAuthFailure` `:160-162`；`NetworkError.Transport` 用 `httpStatus` 决策 `:173-174` | `AuthBridge.kt:127`、`MainArkUIViewController.kt:263` |
| `loginWithWechatCode` | `suspend (code, deviceId = ""): Result<Unit>` `:187-197` | 空码拒 `:189-191`；`deviceId` 空白回落 `:194`；走 #7 `:192-196`。**方法上方 `// GAP(R5)` `:183-186` 明确：只有 HTTP 契约可用，取码桥未接入，勿据此判定微信登录已可用** | **零调用者**（Compose/Swift/ArkTS 三端都没有取码入口） |
| `logout` | `fun logout()` `:200-202` | 直接委托 `AuthSessionActions.logout()`——**纯本地，不发远端** | `AuthBridge.kt:27`、`AppShell.kt:411`、`NativeAndroidMain.kt:93`/`:585`/`:736` |
| `logoutWithRemote` | `suspend (): Result<Unit>` `:210-236` | 要求 `token`+`sessionId`+`deviceId` **三者齐备**才打远端 `:211-220`；成功清本地 `:222-225`；失败时**只有**「会话已废」类才清本地，其余**保留本地会话**让用户重试 `:226-234`（Flutter Server-Confirmed Logout 语义，注释 `:204-209`） | **零调用者** |
| `isLogoutSessionGone` | `private (error): Boolean` `:242-261` | 先看 `AuthHttpFailure.code` 是否 10002 或 `isSessionGone` `:243-249`，再看 message 关键词（含 `Unauthorized`/`未登录`/`token 无效`/`session not found` 等 9 项）`:250-260` | 仅 `logoutWithRemote` |
| `handleAuthFailure` | `private (code?, statusCode?, message?): IllegalStateException` `:268-276` | 会话已废时执行**清理三连**：`AccountFacade.logout()` `:271` + `AuthSessionState.clearLocal()` `:272` + `AuthGate.clearPending()` `:273`；返回带中文文案的 `IllegalStateException` `:275`。403 不清会话（注释 `:263-267`） | `loginWithHuaweiCode` |
| `clearSessionIfGone` | `private (Throwable): Throwable` `:279-289` | 同一个清理三连 `:284-286`，只处理已有 `AuthHttpFailure.code` 的情况 | `commitLogin` `:312`、`guardSessionGone` `:352-353` |
| `commitLogin` | `private Result<AuthLoginData>.(): Result<Unit>` `:295-313` | 展示名兜底链 `username → email local-part → "用户"` `:298-300`；`userId` 空白回落展示名 `:303`；失败分支先 `clearSessionIfGone` `:312` | 4 个登录入口 |
| `mockPhoneLogin` | `private (phone, code): Result<Unit>` `:321-337` | 白名单 `:323-325`、固定码 `123456` `:326-328`；`userId = "mock_phone_<digits>"` `:331`、`token = issueToken("phone")` `:332`、`phone` 存的是**脱敏号** `:333`、头像 `picsum.photos` `:335` | `loginWithOtp` |
| `mockEmailLogin` | `private (account): Result<Unit>` `:340-350` | `displayName = "用户<local-part>"` `:343`、`userId = "mock_<local>"` `:344`、头像 `api.dicebear.com` `:348` | `loginWithPassword`、`register` |
| `commitSession` | `private (…8 个默认参数…): Result<Unit>` `:355-382` | 构造 `LoggedInUser`，`deviceId = AuthDeviceContext.deviceId()` `:371`、**`companyId`/`orgId` 恒 null** `:376-377`；`AccountFacade.setSession` `:365-379`；再 `AuthSessionState.sync()` `:380` | 全部成功分支 |

`AuthTokenRefresh`（`public object :23`）用 `Mutex` `:25` + `CompletableDeferred` `:28` 实现**单飞**：

- `policy` `:31-37`：把 `classifyAuthHttp` 的四值折成网络层的三值决策——`Forbidden` 与 `None` 都归 `PassThrough`（`:35`）。
- `handler` `:40`：`TokenRefreshHandler { raw -> refresh(raw) }`。
- `refresh(raw)` `:46-61`：已有在途则 `await()` `:48`；否则建 deferred、写 `inFlight` `:49-50`，成功 `complete`、失败 `completeExceptionally`，**finally 清空 `inFlight`** `:58-60`。
- `performRefresh(raw)` `:63-93`：未登录或 `refreshToken` 空白 → **不请求，直接耗尽并抛** `:64-69`；
  成功码走 `AccountFacade.updateAuthTokens`，且服务端只轮换 access token 时**沿用旧 refresh/session** `:86-91`。
- `handleRefreshExhausted()` `:99-104`：幂等（未登录直接 return `:100`）+ 同一个清理三连 `:101-103`；
  注释 `:95-98` 说明**不再请求远端 logout**（token 已废）。

`AuthSessionBootstrap`（`public object :13`）只有一个 `install()` `:18-27`：`@Volatile installed` 幂等 `:15-16`/`:19-20` →
`AuthSessionState.sync()` `:21` → `NetworkFacade.setAuthRefreshSupport(policy = AuthTokenRefresh.policy, refresher = AuthTokenRefresh.handler, onSessionCleared = { AuthTokenRefresh.handleRefreshExhausted() })` `:22-26`。

`AuthPendingCredentials`（`public object :7`）是两个可变全局字段：`email = ""` `:8`、`phone = "13400000000"` `:9`
（第二个是 Flutter SoT 的测试号，见 `LoginScreen.kt:57-58`）。

`MockAuthConfig`（`public object :21`）关键事实：白名单 5 个号 `:27-33`、`mockOtpCode = "123456"` `:36`、
`mockTestPhone = "13400000000"` `:39`、5 个展示名 `:42-48`、
**`var useMockAuth: Boolean = !platformIsReleaseBuild()`** `:56`、
`rejectedPhoneMessage()` 把 5 个号拼进提示 `:63-64`、`maskedPhone` 生成 `134****0000` `:73-80`、
`issueToken` 形如 `mock_<prefix>_token_<seq>_<6位随机>` `:87-90`（序号 `:92`）。
默认值随平台：Android/OHOS 的 `platformIsReleaseBuild()` 恒 `false`
（`core/network/src/androidMain/…/NetworkBootstrap.android.kt:5`、
`core/network/src/ohosMain/…/NetworkBootstrap.ohos.kt:5`）→ **Mock 默认开**；
iOS 取 `!Platform.isDebugBinary`（`core/network/src/iosMain/…/NetworkBootstrap.ios.kt:10`）。
expect 声明在 `core/network/src/commonMain/…/NetworkBootstrap.kt:29`。
唯一覆盖点在 Android 入口：`composeApp/src/androidMain/…/MainActivity.kt:32-33`
用 `FLAG_DEBUGGABLE` 再算一次真实 debug 标志（注释 `MockAuthConfig.kt:10-12` 解释了为什么要补这一刀）。

`AuthBridge`（`object :18`，同样没有 `public`）为原生壳提供非 suspend 门面，作用域是
`CoroutineScope(SupervisorJob() + Dispatchers.Default)` `:19`；
`applySession(token,userId,displayName,phone?,email?,refreshToken="",sessionId="",deviceId="")` `:31-58`
是 OHOS 华为路径专用（`token` 空白直接 return `:41`，`avatarUrl`/`companyId`/`orgId` 一律写 null `:50`/`:53-54`）；
7 个动作方法（`loginWithPassword :60-70`、`loginWithOtp :72-82`、`sendPhoneOtp :84-93`、`register :95-106`、
`registerWithPhone :108-118`、`loginWithHuaweiCode :120-130`）都在 `scope.launch` 里调 `AuthRepository`，
最后统一交给 `deliver` `:138-149`——`withContext(Dispatchers.Main)` `:143` 把回调切回主线程，
失败文案兜底 `"登录失败，请稍后重试"` `:146`。读态四个 `token()/userId()/phone()` 在 `:132-136`。

### 3.4 `feature/auth` — UI 层

`AuthFormWidgets.kt` 把 Flutter `LoginPage` 的视觉钉成常量：
`AuthUiTokens :42-57`（`Accent = 0xFF007AFF` `:43`、`Background = 0xFFF2F2F7` `:44`、
`RadiusMd/RadiusLg = 8/12.dp` `:53-54`、`FieldHeight/ButtonHeight = 52.dp` `:55-56`）。
`authGreeting()` `:61-65` 用 `Clock.System.now()` 折算 **UTC+8** 小时（`:63`），
`authGreetingForHour(hour)` `:68-72` 产出「早上好/下午好/晚上好，欢迎使用iHome」三档。

| Composable | 签名要点 | 内部行为 | 调用者 |
|---|---|---|---|
| `AuthSegmentedControl(leftLabel,rightLabel,leftSelected,onSelectLeft,onSelectRight,modifier)` `:75-102` | **右片选中态 = `!leftSelected`** `:97` | 两枚 `AuthSegmentChip` 各占 weight(1f) `:89-100` | `LoginScreen.kt:108`、`RegisterScreen.kt:100` |
| `AuthSegmentChip`（private）`:105-128` | 40.dp 高 `:113` | 选中才画白底 `:115` | 上者 |
| `AuthFilledField(value,onValueChange,hint,modifier,keyboardType = Text,isPassword = false,leading?,trailing?)` `:131-188` | 参数默认 `:135-139` | 52.dp 圆角白底 + 0.5dp 描边 `:144-146`；`BasicTextField` 单行 `:154-158` | `LoginScreen.kt:125/135/169/185`、`RegisterScreen.kt:123/130/136/144/175/191` |
| `AuthPrimaryButton(label,enabled,loading,onClick,modifier)` `:191-226` | — | `enabled && !loading` `:200`；loading 时画 22.dp 环形进度 `:212-217` | `LoginScreen.kt:252`、`LoginPasswordScreen.kt:139`、`LoginOtpScreen.kt:158`、`RegisterScreen.kt:254` |
| `AuthWechatButton(enabled,onClick,modifier)` `:230-258` | 注释 `:228` 直言「outline CTA (SDK may still be stubbed)」 | 只画「◎ 微信登录」`":245`/`:251`，**内部不含任何 SDK 逻辑** | `LoginScreen.kt:289` |
| `AuthPrivacyRow(agreed,onToggle,modifier)` `:261-311` | 44dp 触点 + 22dp 圆勾 `:274-296` | 文案「我已阅读并同意 **《某个隐私条款》**」——链接是硬编码纯文本，无点击跳转 `:299-305` | `LoginScreen.kt:245`、`RegisterScreen.kt:247` |
| `AuthErrorText(message?,modifier)` `:314-325` | — | 空白直接 return `:318`；否则 `DemoColors.Danger` 13.sp | `LoginScreen.kt:249`、`RegisterScreen.kt:251` |
| `AuthFooterLinks(onRegister,onForgotPassword,modifier)` `:328-362` | — | 两枚 TextButton「我要注册」`:338-344`/「忘记密码」`:353-359`，中间 1dp 竖线 `:346-352` | `LoginScreen.kt:295`、`LoginPasswordScreen.kt:156` |

`LoginScreen.kt`（302 行）——`public fun LoginScreen(onLoginSuccess, onOpenRegister, onBack: (() -> Unit)? = null, modifier)` `:43-48`；
`onBack != null` 时挂 `PlatformBackHandler` `:49-51`。状态 10 个 `:52-65`，其中：
`emailMode` 决定 `LoginMode.Email/Phone`（私有枚举 `:37`）；
`phone` **初值写死 `"13400000000"`** `:58`；`agreedPrivacy` **初值为 true** `:60`。
两个 `LaunchedEffect`：把当前输入镜像进 `AuthPendingCredentials` `:67-70`；
冷却秒递减 `:72-76`。发码按钮 `:196-231` 依次校验「先同意」`:198-201`、「号码合法」`:202-205`、
「冷却/加载中忽略」`:206`，成功置 60s 冷却并在 Mock 模式提示固定验证码 `:212-217`。
登录按钮 `enabled` 按模式分叉 `:254-263`（邮箱需非空 + 密码 ≥6 + 已同意；手机需号码合法 + 6 位码 + 已同意），
onClick 再兜一次同意 `:266-269`，结果 `fold` 成功走 `onLoginSuccess()`、失败显示 `it.message ?: "登录失败"` `:279-282`。
**微信按钮点击只赋值一句错误文案**：`error = "微信登录暂未开放（见 platform-gap-registry）"` `:291`；
忘记密码同样只赋值文案 `:297`。

`LoginPasswordScreen.kt`（165 行）——`:37-42` 三参数无默认。账号取自 `AuthPendingCredentials.email` `:44`，
`resolvedAccount = account.trim().ifBlank { AuthPendingCredentials.email.trim() }` `:48`，
`canSubmit = password.length >= 6 && resolvedAccount.isNotBlank() && !loading` `:49`——
这里的 6 是**字面量 6，不是 `AuthValidators.MIN_PASSWORD_LENGTH`**。
只有当全局暂存邮箱为空时才多渲染一个邮箱输入框（`:74-106`，输入同时回写全局 `:79`）。
错误文本用文件私有的 `DemoAlertRed = 0xFFFF3B30` `:164`（`:136` 消费）。提交 `:139-154`。

`LoginOtpScreen.kt`（179 行）——`:42-46` 只有 `onLoginSuccess/onBack/modifier`。
号码**只能**从 `AuthPendingCredentials.phone` 取并过滤 11 位 `:52`；`canSubmit` 要求 6 位码 + 号码合法 `:53`。
Mock 模式下页面直接印出测试号与固定验证码 `:90-96`。重发按钮 `:129-152` 与提交 `:158-173`。
**注意：本页没有任何隐私勾选**（对照 `LoginScreen.kt:245-248`）。

`RegisterScreen.kt`（314 行）——`:42-46` 的 `onRegistered: () -> Unit = onBack` 用前一个参数作默认值。
邮箱分支四个框 `:123-150`，`enabled` 要求 `isValidEmail && password == confirm && 长度 >=6 && agreedPrivacy` `:257-261`；
onClick 里再判一次两次密码一致 `:273-281`；
手机分支的发码按钮在 Mock 下**不显示固定码**（`:219` 只写「验证码已发送」，与 `LoginScreen.kt:213-217` 不同）。

### 3.5 `feature/friend`

`FriendDirectory.kt`（59 行，`object :10`，无 `public`）：

| 成员 | 事实 | 行号 |
|------|------|------|
| `data class Entry(peerId,name,phoneMasked,remark = "")` | 派生 `avatarUrl = ChatAvatarUrls.peer(peerId)`（头像 URL 由 peerId 拼出，**不来自服务端**）`:17`；`toFriendItem()` `:18-24` | `:11-25` |
| `seedFriends` | **3 条硬编码**：`mock_peer_01/02/03` + 「Mock好友1/2/3」+ `134****000x` | `:28-32` |
| `directory` | 2 条可搜索联系人：`mock_peer_09`「新同学小周」、`mock_peer_10`「外教 Anna」 | `:35-38` |
| `ensureChat(peerId,title): String` | `ImEngineStore.engine.ensureConversation(id = "private_$peerId", lastMessage = "你们已成为好友", unreadCount = 0)` | `:40-46` |
| `ensureGroupChat(memberPeerIds,title): String` | key = 排序后 `_` 连接，空白回落 `"solo"` `:50`；会话 id `group_<key>` `:52` | `:49-57` |

`FriendScreen.kt`（452 行）：

| 成员 | 事实 | 行号 |
|------|------|------|
| `data class FriendItem(id,name,phoneMasked,avatarUrl,remark = "")` | **public**，UI 内部模型，与 `ImFriendUser` 无映射关系 | `:46-52` |
| `private data class IncomingRequest` | 申请条目，**生产代码里从未被填充** | `:54-59` |
| `private object FriendMockData` | 直接把 `FriendDirectory.seedFriends` / `.directory` 映射成 `FriendItem` | `:65-68` |
| `@Composable fun FriendScreen(onBack)` | 无 `public` 修饰 `:71`；两条分支都先 `ReportMainTabRoot(isRoot = false)` 再渲染 `:76-81`；详情/列表由 `selectedId` 切换 `:72-74` | `:70-88` |
| `private fun FriendListContent(friends,onFriendsChange,onBack,onOpen)` | 顶栏 `MineTopBar("通讯录")`，actions 里「建群」→ `FriendDirectory.ensureGroupChat` `:113-116`，空列表时只 toast「请先添加好友再建群」`:110-112`；搜索框占位「手机号或用户 ID」`:156`；**搜索只在本地两份 Mock 列表里 filter** `:167-177`；`incoming` 初始为空且注释写明「Flutter SoT capture: incoming empty → no『新的朋友』section」`:99-100`，故该 section 永不渲染 `:216`；「加好友」只把命中项追加进本地列表 + `ensureChat` + toast `:193-201`；「拒绝/同意」同样只改本地状态 `:230-243`；「好友」区标题右侧显示条数 `:249`，空态「还没有好友」`:251-261` | `:90-294` |
| `private fun SectionHeader/GroupedCard/FriendRow/PillButton/OutlinedPill/NetworkAvatar` | 纯展示私件；`NetworkAvatar` 走 `PlatformNetworkImage` + 占位图 `Res.drawable.community_avatar` | `:296-413`（`NetworkAvatar :396-413`） |
| `private fun FriendDetailScreen(friend,onBack)` | 「发消息」= `FriendDirectory.ensureChat` + toast「已打开会话 <id>」 | `:415-451`（`:440-446`） |

`ImFriendApi.kt`（294 行）——`public class ImFriendApi(private val client: ApiClient = NetworkFacade.api())` `:117-119`。
文件头 `:108-115` 的 `// GAP(R5)` 说明：好友关系变化后的**会话打开与消息收发属于融云 SDK**，
本工程拿不到 IMLib 产物，「这里不引入 SDK、不伪装 ready」。

| 方法 | 端点/契约 | 关键行为 | 行号 |
|------|-----------|----------|------|
| `search(query): List<ImFriendUser>` | #107 `GET /api/v1/im/users/search` | `q` **原样下发，空串也发**，故不走 `queryOf` 的省略规则（注释 `:125-126`） | `:122-129` |
| `listFriends(): List<ImFriendUser>` | #103 `GET /api/v1/im/friends` | 走 `getItemsApi`，失败兜底「好友列表失败」 | `:132-138` |
| `listIncomingRequests(): List<ImFriendRequest>` | #104 `GET /api/v1/im/friends/requests` | **仅入向** | `:144-150` |
| `requestFriend(toUserId)`（无返回值） | #105 `POST /api/v1/im/friends/requests` | `data` 整个忽略，只看 `code == 0`；失败 **抛 `NetworkError.Business` 且 message 是服务端原始英文**，兜底 `"申请失败"` `:171-176`；翻译留给调用方（注释 `:157-161`） | `:163-177` |
| `respondFriend(requestId, accept)` | #106 `POST /im/friends/requests/<id>/respond` | 同意/拒绝共用一端点，兜底 `"处理失败"` `:191` | `:180-194` |
| `canPrivateChat(peerUserId): Boolean` | #108 `GET /api/v1/im/private/admission` | **fail-closed**：`runCatching` 包住 `getApi`，任何异常/非成功码/非字面 `true` 一律 false `:204-212` | `:203-213` |
| `companion friendlyError(error?, fallback = "操作失败")` | — | 按 `NetworkError` 子型取原始 message | `:224-231` |
| `companion friendlyMessage(raw, fallback)` | — | 5 行中英关键词映射 + 空白/`HttpRequestException` 兜底 | `:238-247` |
| `private suspend ApiClient.getItemsApi(...)` | — | 只认 `data["items"]` 数组 `:273`，非对象元素跳过 `:265`，失败抛 `NetworkError.Business` `:267-272` | `:259-275` |
| `private JsonObject.imFriendStr(vararg keys)` / `imFriendIsTrue(key)` | — | **从 `feature/chat` 刻意复制**，注释 `:277-278` 写明原因是「feature 不得互相 import」 | `:279-293` |

DTO：`ImFriendUser(userId, displayName, avatarUrl = "", phoneMasked = "")` + `fromJson` `:45-59`；
`ImFriendRequest(id, fromUserId, displayName, avatarUrl, phoneMasked, status = "pending", createdAt)` `:66-86`（默认态 `:82`）；
两个请求体 `ImFriendRequestCreateRequest(toUserId)` `:89-92`、`ImFriendRespondRequest(accept)` `:98-101`。

## 4. 登录全链路专题（写透）

### 4.1 装机时序（会话 + 401 能力）

| 步 | 事实 | 证据 |
|----|------|------|
| 1 | 三端入口都调 `ShellBootstrap.install()` | `composeApp/…/ShellBootstrap.kt:20-25`；调用点 `composeApp/…/AppShell.kt:107`、`composeApp/src/androidMain/…/MainActivity.kt:35`、`composeApp/src/iosMain/…/MainViewController.kt:76`、`composeApp/src/ohosMain/…/MainArkUIViewController.kt:153` |
| 2 | `ShellBootstrap` 内部先做网络 bootstrap 再 `AuthSessionBootstrap.install()` | `composeApp/…/ShellBootstrap.kt:23` |
| 3 | `AuthSessionBootstrap.install()` 幂等，先 `AuthSessionState.sync()` 再注入刷新三件套 | `AuthSessionBootstrap.kt:18-27` |
| 4 | `NetworkFacade.setAuthRefreshSupport` 存 policy/refresher/onSessionCleared 后 `rebuildClient()` | `core/network/…/NetworkFacade.kt:57-66` |
| 5 | `rebuildClient()` 关键点：**启用刷新后平台 client 的 `onTokenExpired` 被置 null**，把 401 流的唯一 owner 让给装饰器 | `NetworkFacade.kt:68-74`（`:70`） |
| 6 | `decorate()` 只在 policy 与 refresher 都在时套 `AuthRefreshingApiClient`，否则原样返回 | `NetworkFacade.kt:93-106` |
| 7 | 首次触碰 `AccountFacade` 才水合：读 KV → 解码 → 若有 token 再 `bindAccessToken` | `AccountFacade.kt:178-188`、`:42` |

### 4.2 邮箱密码登录（真实链路，`useMockAuth = false`）

| 步 | 位置 | 发生了什么 |
|----|------|------------|
| 1 | `LoginScreen.kt:125-144` | 用户输入邮箱与密码（`AuthFilledField`，密码框 `isPassword = true` `:140`） |
| 2 | `LoginScreen.kt:67-70` | `LaunchedEffect(email, phone)` 把邮箱镜像进 `AuthPendingCredentials.email` |
| 3 | `LoginScreen.kt:254-258` | `enabled` 卡「非空 + 密码 ≥6 + 已同意」 |
| 4 | `LoginScreen.kt:275` | `AuthRepository.loginWithPassword(email, password)` |
| 5 | `AuthRepository.kt:25-34` | trim 校验 / 邮箱合法性 / 密码长度，失败返回 `IllegalArgumentException`（中文文案） |
| 6 | `AuthRepository.kt:38-43` | 交给 `UserAuthHttpApi.login(username, password, AuthDeviceContext.deviceId(), AuthDeviceContext.platform())` |
| 7 | `AuthApiContracts.kt:350`/`:352-363` | `deviceId` = `kmp-<platform>-device`（**固定模板，非真实设备指纹**），`platform`：OHOS 上报 `android` |
| 8 | `UserAuthHttpApi.kt:45-52` → `AuthApiContracts.kt:42-47` | 组 `AuthPasswordLoginRequest`，四键 `{username,password,device_id,platform}` 总发 |
| 9 | `UserAuthHttpApi.kt:195` | `NetworkFacade.api().postApi<Req, AuthLoginData>(AuthApiPaths.LOGIN, body)`；`LOGIN = "/api/v1/user/login"`（`AuthApiContracts.kt:16`） |
| 10 | `NetworkFacade.kt:34` → `:93-106` | 请求经 `AuthRefreshingApiClient` 装饰（`postApi` 是五个参与刷新的出口之一，`core/network/…/TokenRefresh.kt:34-35`） |
| 11 | `DemoRequestHeaders.kt:9-12` | 若 `NetworkConfig.accessToken` 非空且 URL 不含 `DemoApiHosts.AUTH_SKIP_HOST`（`core/network/…/DemoApiHosts.kt:30`），注入 `Authorization: Bearer <token>`；登录时通常还没 token，故头为空 |
| 12 | `UserAuthHttpApi.kt:197` | 成功判据三连：`isAuthBusinessSuccess(code)`（`code == 0 \\|\\| 200`，`AuthApiContracts.kt:450-451`）+ `data != null` + `token.isNotBlank()` |
| 13 | `AuthApiContracts.kt:286-299` | `toPayload()`：`userId` 走 `resolvedId`（`id` 优先 `:120-121`），`username` 走 `user_name → username → display_name`（`:124-125`），头像 `avatar_url → avatarUrl`（`:139-140`） |
| 14 | `AuthRepository.kt:295-313` | `commitLogin()`：展示名兜底 `:298-300`，失败分支 `clearSessionIfGone` `:312` |
| 15 | `AuthRepository.kt:355-382` | `commitSession()` 组 `LoggedInUser`，`refreshToken`/`sessionId` 直接取 DTO（缺省空串 `:361-362`），`companyId/orgId` 恒 null `:376-377` |
| 16 | `AccountFacade.kt:49-58` | `setSession()`：写内存 `:51` → `persist()` `:52` → `NetworkFacade.bindAccessToken(token)` `:54` |
| 17 | `AccountFacade.kt:190-197` + `AccountUserSnapshot.kt:44-59`/`:61-62` | 快照 → `NetworkJson.encodeToString` |
| 18 | **`AccountSessionStore.kt:34-42`** | **落到 KV：存储名 `demo_account`（`:20`），键名 `demo_account_user_json`（`:22`）**。物理落点：Android 是名为 `demo_account` 的 SharedPreferences 文件（`KeyValueStore.android.kt:12-16`），iOS 是 `NSUserDefaults(suiteName: "demo_account")`（`KeyValueStore.ios.kt:14-15`），OHOS 是同名 Preferences 文件（`KeyValueStore.ohos.kt:41-42`、`:54-66`） |
| 19 | `NetworkFacade.kt:76-79` → `NetworkConfig.kt:30` | access token 进 `NetworkConfig.accessToken`，并 `rebuildClient()` |
| 20 | `AuthRepository.kt:380` | `AuthSessionState.sync()` → `AccountFacade.current()`（`AuthSessionState.kt:35`）→ 刷新 StateFlow + Compose 快照（`:34-37`、`:43-47`） |
| 21 | `LoginScreen.kt:279-280` | `fold(onSuccess = { onLoginSuccess() })` |
| 22 | `AppShell.kt:324-331` | `afterAuthSuccess()`：`refreshSession()` → `AuthGate.consumePending()` 恢复到之前想去的 tab `:326` → 关浮层 |

短信登录（`LoginScreen.kt:277` / `LoginOtpScreen.kt:166` → `AuthRepository.kt:65-81` → 契约 #6，
`UserAuthHttpApi.kt:99-112`）与上面同构，差别只在：号码先 `toE164China`（`AuthApiContracts.kt:328-335`），
发码走 #5 且 `data` 恒 null（`UserAuthHttpApi.kt:86-96`），落库/水合链路完全相同。

### 4.3 `AccountFacade` 对外暴露什么

| 能力 | 成员 | 消费方举例 |
|------|------|------------|
| 是否登录 | `AccountSession.isLoggedIn` `:14` | `AppShell.kt:230`（tab 壳）、`AuthGate` 组合判断 `AppShell.kt:310`、`WebBridgeHost.kt:63` |
| 展示名 | `displayName` `:15` | `AppShell.kt:392`/`:403`、`MineProfileLogic.kt:28`、`AuthBridge.kt:24` |
| access token | `token` `:16` | `AuthBridge.kt:132`、`WebBridgeHost.kt:63`（H5 取 token） |
| refresh token | `refreshToken` `:19` | `AuthTokenRefresh.kt:66`/`:74` |
| 会话 id / 设备 id | `sessionId :22`、`deviceId :25` | `AuthRepository.kt:214-215`（远端登出前置校验）、`AuthTokenRefresh.kt:75-76` |
| 资料 | `userId :27`、`avatarUrl :28`、`phone :29`、`email :30`、`companyId :31`、`orgId :32` | `AuthBridge.kt:134`/`:136`、`MineProfileLogic.kt:31` |
| 写入 | `setSession :49`、`updateAuthTokens :71`、`updateProfile :93` | `AuthRepository.kt:365`、`AuthTokenRefresh.kt:86`、`MineProfileLogic.kt:20` |
| 清除 | `logout :60` | `AuthSessionActions.kt:13` |

### 4.4 401 / 403：网络层与账户层怎么配合

| 阶段 | 事实 | 证据 |
|------|------|------|
| A 收到应答 | `AuthRefreshingApiClient` 包住 `getApi/postApi/putApi/patchApi/deleteApi` 五个出口 | `core/network/…/TokenRefresh.kt:34-35`、`:56-103` |
| B 豁免 | **登录/注册/刷新/登出四条路径直接透传，不参与刷新**（防递归死循环） | `TokenRefresh.kt:47-54`（判据 `:50-53`）、`withRefresh` 里的短路 `:138` |
| B2 | SSE **只转发、完全不进刷新流程**，且注释解释了三条理由（流不可重放、Dart 同理） | `TokenRefresh.kt:105-125`（结论 `:119`） |
| C 决策 | `policy.decide(httpStatus, code, message)` → `Refresh / ForceLogout / PassThrough` | `TokenRefresh.kt:139`；实现在 `AuthTokenRefresh.kt:31-37`，底层是 `classifyAuthHttp`（`AuthHttpDecision.kt:21-42`） |
| D1 `TryRefresh` → `Refresh` | `withRefresh` 调 `refresher.refresh(delegate)`，成功**代发一次原请求** `call()`，失败原样返回**首次应答** | `TokenRefresh.kt:146-155`（`:148`、`:154`） |
| D1a 单飞 | 并发 401 只发一次 refresh：`Mutex` + `CompletableDeferred`，后来者 `await()` | `AuthTokenRefresh.kt:46-61` |
| D1b 无凭据 | `!isLoggedIn \\|\\| refreshToken.isBlank()` → **不发请求**，直接耗尽并抛 `SessionCleared` 文案 | `AuthTokenRefresh.kt:64-69` |
| D1c 请求本身 | `UserAuthHttpApi.refresh(raw = …)` 用的是**未装饰** delegate（Flutter `skipAuthRefresh` 对等），且要求 `token.isNotBlank()` | `TokenRefresh.kt:148`（传的是 `delegate`）、`UserAuthHttpApi.kt:142-164`（判据 `:159`） |
| D1d 落库 | `AccountFacade.updateAuthTokens(token, refreshToken, sessionId)`，服务端没回新 refresh/session 时**沿用旧值** | `AuthTokenRefresh.kt:86-91`；`AccountFacade.kt:71-90`（→ 又回到 §4.2 第 16–19 步：落 KV + 绑头） |
| D2 `ForceLogout`（10021/10022） | 不重试，`onSessionCleared?.invoke()` 后原样返回首次应答 | `TokenRefresh.kt:141-145` |
| D2a 清会话 | `onSessionCleared` = `AuthTokenRefresh.handleRefreshExhausted()` → `AccountFacade.logout()` + `AuthSessionState.clearLocal()` + `AuthGate.clearPending()` | `AuthSessionBootstrap.kt:25`、`AuthTokenRefresh.kt:99-104` |
| D2b 落盘效果 | `AccountFacade.logout()` 清内存 + `store.clear()`（**删掉 `demo_account_user_json` 键**）+ `clearAccessToken()` | `AccountFacade.kt:60-65`、`AccountSessionStore.kt:44-46`、`NetworkFacade.kt:81-84` |
| D3 `Forbidden`（HTTP 403） | 归 `PassThrough`：**既不刷新也不登出**，交业务提示「暂无权限」 | `AuthTokenRefresh.kt:35`、`AuthHttpDecision.kt:32-34`、`mapAuthFailure :393-402`；`core/network/…/NetworkCodes.kt:28` 的注释同样声明 403 不触发登出 |
| D4 `None` | 透传 | `AuthTokenRefresh.kt:35`、`TokenRefresh.kt:140` |
| E 业务层兜底 | 若业务码本身是「会话已废」，`AuthRepository.clearSessionIfGone` / `handleAuthFailure` 执行**同一个清理三连**（幂等） | `AuthRepository.kt:279-289`、`:268-276` |
| F 平台旧通路 | Android Jetpack 壳另挂了一条 `TokenExpiredHandler`：`AuthRepository.logout()` + `softAuth.clearLocalSession()` | `composeApp/src/androidMain/…/NativeAndroidMain.kt:91-96`（离开时摘钩 `:97`）。注意 `NetworkFacade.kt:70`：一旦启用刷新支持，平台侧 `onTokenExpired` 会被置 null，所以**这条钩子只在装机前或没启用刷新时生效** |
| G 软门禁 | `AuthGate.requiresAuth` 只管 Chat / Community 两个 tab | `component/…/AuthGate.kt:22-39`（`AppRoutes.kt:43-44` 也把这两个 tab 标了 `requiresAuth = true`）；消费点 `AppShell.kt:310`、`SoftAuthPresenter.kt:44` |

### 4.5 登出清哪些东西

| 入口 | 走哪条 | 清理集合 |
|------|--------|----------|
| Mine 页登出 | `AppShell.kt:411` / `NativeAndroidMain.kt:585`、`:736` / `AuthBridge.kt:27` / iOS `ContentView.swift:372`（`AuthLogout` → `MainViewController.kt:91`） | 全部走 `AuthRepository.logout()`（`AuthRepository.kt:200-202`）→ `AuthSessionActions.logout()`（`component/…/AuthSessionActions.kt:12-16`）三连 |
| 三连内容 | ① `AccountFacade.logout()` ② `AuthSessionState.clearLocal()` ③ `AuthGate.clearPending()` | `AuthSessionActions.kt:13-15` |
| ① 具体清掉 | 内存 session → `LoggedOut`；**KV 键 `demo_account_user_json` 被 remove**；`NetworkConfig.accessToken = ""` 且重建 client | `AccountFacade.kt:60-65`、`AccountSessionStore.kt:44-46`、`NetworkFacade.kt:81-84`、`NetworkConfig.kt:30` |
| ② 具体清掉 | StateFlow 快照与两个 Compose 字段复位 | `component/…/AuthSessionState.kt:39-41`、`:43-47` |
| ③ 具体清掉 | `pendingTab = null` | `AuthGate.kt:52-54` |
| 保留不动 | **隐私同意旗标 `demo_privacy_accepted` 不清**（登出 ≠ 撤回同意）；OHOS 侧 ArkTS 显示缓存另有 `device_id` 保留逻辑 | `PrivacyConsentStore.kt:27-29`（只有显式 `setAccepted(false)` 才写）、`harmonyApp/entry/src/main/ets/auth/SessionStore.ets:75-83` |
| **远端登出** | `logoutWithRemote()` 才是契约 #4；**当前没有任何 UI/壳调用它** | `AuthRepository.kt:210-236`；全仓 grep 仅命中定义自身 |
| 互踢/耗尽路径**刻意不**打远端 | token 已废，请求无意义 | `AuthTokenRefresh.kt:95-98`、`AuthRepository.kt:263-267` |

### 4.6 三端「非 Compose」登录通路（各端真相）

| 端 | 通路 | 证据 | 真实状态 |
|----|------|------|----------|
| Android | Jetpack 壳直接渲染共享 `LoginScreen` 等 4 屏 | `NativeAndroidMain.kt:644-667` | 共享 UI 复用，走 §4.2 全链路 |
| iOS | SwiftUI 原生页调用导出的 Kotlin 函数 | `iosApp/…/NativeAuthPages.swift:311`/`:335`/`:348`/`:428`/`:461`/`:475` → `MainViewController.kt:94-142` → `AuthBridge` | 走 `AuthBridge`（回调投递依赖 `Dispatchers.Main`，`AuthBridge.kt:143`） |
| iOS 冷启 | 首个 Swift 调用前先 `ensureIosAuthReady()` | `MainViewController.kt:73-77` | 水合 + 刷新装机 |
| OHOS | ArkTS 走 NAPI → Kotlin `runBlocking` | `harmonyApp/entry/src/main/ets/auth/AuthApiClient.ets:39`/`:44`/`:48`/`:57`/`:65` → `napi_init.cpp:46-58` 等 → `MainArkUIViewController.kt:158-179`/`:182-203`/`:206-225`/`:228-251`/`:254-275` | 阻塞式，错误经 `errBuf` 回传（`:172-178`） |
| OHOS 冷启 | ArkTS 把 shell 缓存回灌 Kotlin | `Index.ets:233-250` → `KnApplyAuthSession`（`MainArkUIViewController.kt:112-129`）→ `AuthBridge.applySession` | **回灌会丢字段**：`AuthApiClient.ets:26-27` 把 `refreshToken`/`sessionId` 硬写成 `''`，`deviceId` 硬编码 `'kmp-ohos-device'`（`:31`）→ 冷启后**无法静默续期** |
| OHOS 华为 | 真实 `@kit.AccountKit` 组件取 `authorizationCode` | `harmonyApp/entry/src/main/ets/auth/HuaweiQuickLoginPane.ets:1`、`:45-48`、`:141-146`、`:157`、`:203` | **三端里唯一真实接入的原生登录 SDK**，端点 `/api/v1/user/huawei/login`（`AuthConfig.ets:4`） |

## 5. 隐私合规 / 同意（照实说明）

代码里的 consent 相关逻辑**确实存在**，分成两类，且第二类只存在于共享层：

| 类别 | 存储位置 | 谁写 | 谁读 |
|------|----------|------|------|
| **A. App 级隐私政策同意（跨启动持久）** | KV：存储 `demo_account`（`AccountSessionStore.kt:20`）+ 键 **`demo_privacy_accepted`**（`PrivacyConsentStore.kt:17`），实现在 `PrivacyConsentStore.kt:22-30` | `createPrivacyConsentStore()` 由 `AppShell.kt:101` 与 `NativeAndroidApp.kt:53` 创建；接受时 `setAccepted(true)`（`AppShell.kt:122`、`NativeAndroidApp.kt:64`） | 首屏分流：`AppShell.kt:103` 决定 Splash → Main 还是 PrivacyConsent（`:115`）；Android 同理 `NativeAndroidApp.kt:55`/`:60`。同意之后 Android 才注册推送（`NativeAndroidApp.kt:66-68`）。`AccountStoresTest.kt:57-69` 钉死键名与「撤回被记住」 |
| **B. 登录/注册页内的条款勾选（不持久）** | **仅 Compose 内存状态**，不落任何 KV | `LoginScreen.kt:60`（初值 true）、`:247` 切换；`RegisterScreen.kt:56`、`:249` | 只作为按钮 `enabled` 与点击前置校验：`LoginScreen.kt:258`/`:262`/`:266-269`、`RegisterScreen.kt:261`/`:265`/`:269-272`；发码按钮也校验 `LoginScreen.kt:198-201`、`RegisterScreen.kt:204-207` |

必须点明的三条差异：

1. **B 类勾选与 A 类同意互不相干**：`AuthPrivacyRow`（`AuthFormWidgets.kt:261-311`）只是 UI 组件，
   内部没有任何 store 调用；它显示的《某个隐私条款》是硬编码纯文本（`:305`），**不可点击、无跳转**。
2. **`LoginOtpScreen.kt` 完全没有同意环节**（整文件无 `AuthPrivacyRow`），从枢纽页直接进验证码页时无二次勾选。
3. **三端 consent 存储彼此独立、键名不同**，共享层的 `PrivacyConsentStore` 只覆盖 Kotlin 侧：

| 端 | 实际读的存储 | 键 | 证据 |
|----|--------------|----|------|
| 共享 Compose 壳 / Android Jetpack 壳 | `demo_account` KV | `demo_privacy_accepted` | `AppShell.kt:101-122`、`NativeAndroidApp.kt:53-64`、`PrivacyConsentStore.kt:17` |
| iOS SwiftUI 壳 | `UserDefaults.standard`（**不是** suite `demo_account`） | `privacy_accepted` | `iosApp/…/ContentView.swift:63`（读）、`:93`（写） |
| Harmony ArkTS 壳 | `preferences` 里名为 `native_shell` 的库 | `privacy_accepted` | `harmonyApp/…/Index.ets:229-232`（读）、`:1168-1170`（写） |

结论：**「用户是否同意隐私政策」目前没有单一 SoT**，同一设备上三套键各读各的；
Kotlin 层那份还与会话挤在同一个 `demo_account` 文件里（`PrivacyConsentStore.kt:8-11` 说明了这是为了兼容 Android 历史数据）。

## 6. friend 模块：屏、端点、与 home/mine 的路由关系

### 6.1 屏与数据流

```
HomeScreen（宫格 "/friend"）      ChatScreen（顶栏入口 onOpenFriends）
      │ feature/home/…/HomeScreen.kt:52   │ feature/chat/…/ChatScreen.kt:59,145,198
      ▼                                    ▼
AppShell 把 AppRoute.Friend 映射到 FriendScreen   （composeApp/…/AppShell.kt:476-478）
      │  另一条：跨模块路由表 route == ContentRoutes.Friend → FriendScreen
      │  （composeApp/…/AppCrossFeatureScreens.kt:62）
      ▼
FriendScreen（本地 Mock 列表 + 详情）── ensureChat / ensureGroupChat ──▶ ImEngineStore.engine（:component）
                                                            （FriendDirectory.kt:40-57）
```

`FriendScreen` 的注册路由常量：`AppRoute.Friend`（`core/router/…/AppNavigator.kt:22`）、
`AppRoutePath.friend = "/friend"`（`core/router/…/AppRoutePath.kt:49`）、
`ContentRoutes.Friend = "/friend"`（`core/router/…/FeatureRoutes.kt:160`，中文标签「好友/通讯录/朋友」三词归一 `:171`）、
`RoutePath.kt:59` 亦指向 `ContentRoutes.Friend`。
深链 `/friend` 解析在 `core/router/…/DeepLinkRouter.kt:69-70`（解析成 `tab = null`，因此不会撞 tab 门禁）；
宿主分派把 `/friend` 归到 Content 宿主、把 `/login|/auth|/register` 归到 Auth 宿主
（`core/router/…/ProductRouteDispatch.kt:128-130`、`:119-120`）。
登录页本身的路由常量在 `core/router/…/AppRoutePath.kt:39-42`（`/login`、`/login/password`、`/login/otp`、`/register`）
与 `AppRoutes.kt:13-16`（`/auth/login`、`/auth/register`）。

与 **Mine** 的关系：Mine 页不跳 friend；Mine 只负责登出与资料（`feature/mine/…/MineProfileLogic.kt:19-25`）。
与 **auth** 的关系：`/friend` 不在 `AuthGate` 的强制名单内（`AuthGate.kt:25-31` 只认 chat / community），
即**未登录也能进通讯录**。

### 6.2 端点

好友 lane 六条端点（`ImFriendApiPaths :23-38`）：`/api/v1/im/friends` `:25`、
`/api/v1/im/friends/requests` `:28`、`/api/v1/im/users/search` `:31`、
`/api/v1/im/private/admission` `:34`、respond 由 `respondPath(requestId)` 拼 `:37`。
路径必须与 `feature/chat` 的 `RongImConfig.kt` 保持一致（文件头 `:20-21`）。

**这些端点目前没有任何生产调用**：`FriendScreen` 全程只用 `FriendDirectory` 的硬编码数据
（`FriendScreen.kt:167-177` 搜索本地列表、`:193-201` 加好友只改本地状态、`:230-243` 同意/拒绝同理，
`incoming` 恒空 `:99-100`）。`ImFriendApi` 仅被 `ImFriendApiContractTest.kt` 实例化（`:37`、`:56`、`:114` 等）。

## 7. 端点消费表（method → 调用点行号）

| 端点 | 契约 | HTTP 出口 | 封装方法 | 生产调用点 |
|------|------|-----------|----------|------------|
| `/api/v1/user/login` #1 | POST | `postApi`（`UserAuthHttpApi.kt:195`） | `UserAuthHttpApi.login` `:40-53` | `AuthRepository.kt:38-43` ← `LoginScreen.kt:275`、`LoginPasswordScreen.kt:147`、`AuthBridge.kt:67`、`MainArkUIViewController.kt:167` |
| `/api/v1/user/register` #2 | POST | `postApi<AuthRegisterRequest, AuthRegisterData>`（`:67-75`） | `UserAuthHttpApi.register` `:59-83` | `AuthRepository.kt:107-113` ← `RegisterScreen.kt:287`、`AuthBridge.kt:103`、`MainArkUIViewController.kt:238` |
| `/api/v1/user/refresh` #3 | POST | **raw client** `postApi`（`:149`） | `UserAuthHttpApi.refresh` `:142-164` | `AuthTokenRefresh.kt:72-78` ← 装饰器 `TokenRefresh.kt:148` |
| `/api/v1/user/logout` #4 | POST（无 body） | `postApi<Unit>(body = "", parseData = { })`（`:175-179`） | `UserAuthHttpApi.logout` `:174-185` | `AuthRepository.kt:221`（`logoutWithRemote`）——**该方法无调用者** |
| `/api/v1/user/phone/otp/send` #5 | POST | `postApiOk`（`:87`） | `UserAuthHttpApi.sendPhoneOtp` `:86-96` | `AuthRepository.kt:59-61` ← `LoginScreen.kt:210`、`LoginOtpScreen.kt:138`、`RegisterScreen.kt:216`、`AuthBridge.kt:90`、`MainArkUIViewController.kt:214` |
| `/api/v1/user/phone/otp/verify` #6 | POST | `postApi`（`:195`） | `UserAuthHttpApi.verifyPhoneOtp` `:99-112` | `AuthRepository.kt:75-80` ← `LoginScreen.kt:277`、`LoginOtpScreen.kt:166`、`AuthBridge.kt:79`、`MainArkUIViewController.kt:191` |
| `/api/v1/user/wechat/login` #7 | POST | `postApi`（`:195`） | `UserAuthHttpApi.loginWithWechat` `:121-132` | `AuthRepository.kt:192-196`（`loginWithWechatCode`）——**该方法无调用者** |
| `/api/v1/user/huawei/login` | 契约表外 | `NetworkFacade.api().postApi` **直连** | 无（`AuthRepository.kt:155-158`） | `AuthRepository.kt:148` ← `AuthBridge.kt:127`、`MainArkUIViewController.kt:263` ← `HuaweiQuickLoginPane.ets:157` |
| `/api/v1/profiles/me` #8 | GET | `getApi`（`UserProfileHttpApi.kt:28`） | `fetchMe` `:27-34` | **零调用** |
| `/api/v1/profiles/me` #9 | PATCH | `patchApi`（`:70`） | `updateMe` `:68-75` | **零调用** |
| `/api/v1/profiles/me/store` #10 | POST | `postApi`（`:54`） | `switchStore` `:52-59` | **零调用** |
| `/api/v1/me/stores` #11 | GET | `getApi`（`:44`） | `listMyStores` `:43-46` | **零调用** |
| `/api/v1/im/friends` #103 | GET | `getItemsApi` → `getApi`（`ImFriendApi.kt:265`） | `listFriends` `:132-138` | **零调用**（仅 `ImFriendApiContractTest.kt:37`、`:82`） |
| `/api/v1/im/friends/requests` #104 | GET | 同上 | `listIncomingRequests` `:144-150` | **零调用**（仅测试 `:56`、`:65`、`:87`） |
| `/api/v1/im/friends/requests` #105 | POST | `postApi` + `NetworkJson.encodeToString`（`:164-170`） | `requestFriend` `:163-177` | **零调用**（仅测试 `:93`、`:114`、`:143`、`:151`、`:158`） |
| `/api/v1/im/friends/requests/{id}/respond` #106 | POST | `postApi`（`:181-187`） | `respondFriend` `:180-194` | **零调用** |
| `/api/v1/im/users/search` #107 | GET | `getItemsApi`（`:123-128`） | `search` `:122-129` | **零调用**（仅测试 `:71`、`:166`、`:181`） |
| `/api/v1/im/private/admission` #108 | GET | `getApi`（`:205-209`） | `canPrivateChat` `:203-213` | **零调用**（仅测试 `:199`、`:208`、`:213`、`:217`） |

## 8. 测试与真实状态（不许粉饰）

### 8.1 已有测试能证明什么

| 文件 | 覆盖面 | 证明力 |
|------|--------|--------|
| `AccountStoresTest.kt` | 两个 KV 存储 + 存储名 | 键名与「空白 == 登出」行为钉死（`:21-45`、`:52-69`、`:74-77`）。**用 `InMemoryKeyValueStore`（`:18`、`:49`），不碰真实平台 KV** |
| `AuthHttpDecisionTest.kt` | 决策 / 谓词 / 文案 | 403 矩阵 `:56-84`（含 10003 与 403 同值时仍判 Forbidden 且保留「账号未注册」文案）、刷新 `:89-117`、硬失败 `:131-139`、谓词 `:144-155`、文案回归 `:160-190` |
| `AuthSessionClearTest.kt` | AuthGate / 会话清理 | 只测 `AuthGate` 与 `AuthSessionState`；文件头 `:12-15` 与 `:44-46` 自己承认「不需要平台 `AccountSessionStore`，**真实 `AccountFacade` 登录持久化靠设备/插桩 smoke**」——即 §4.2 第 16–19 步**没有单测覆盖** |
| `AuthUserContractJsonTest.kt` | 契约 #1–#7 的 JSON 形状 | 路径相等 `:35-44`、登录体四键 `:47-62`、别名优先级 `:65-142`、无 user 降级 `:145-155`、注册双形态 `:169-245`、刷新空键省略 `:250-277`、OTP `:305-338`、微信 `:343-362` |
| `UserProfileContractJsonTest.kt` | 契约 #8–#11 的 JSON 形状 | 前缀拆分 `:36-42`、query 省略 `:47-53`、双别名 `:56-158`、整数钳位 `:161-188`、PATCH 省略规则 `:201-234`、10001 保留会话 `:250-264`、切店 `:269-292`、`/me/stores` `:297-362`。**纯序列化契约测试，不代表 lane 已接入** |
| `FriendDirectoryTest.kt` | 本地目录 | 种子好友与 IM 会话一致 `:10-16`、`ensureChat` 返回 `private_mock_peer_09` `:18-23`、群 id 前缀 `:25-33` |
| `ImFriendApiContractTest.kt` | 好友 6 端点 | 用 `FakeFriendApiClient`（`:263-324`）断言 path/query/body/错误映射（`:26-257`）；`runNow` 手搓协程续体（`:334-349`，注释说明 commonTest 没引 `kotlinx-coroutines-test`）。**测试替身，非真实网络** |

**全仓没有 `feature/auth` 或 `feature/friend` 的 UI / 集成 / 网络往返测试**；
`AuthRepository`、`AuthTokenRefresh`、`AuthBridge`、四个 Screen 均无直接单测。

### 8.2 Mock / stub / 假数据清单

| 项 | 事实 | 证据 |
|----|------|------|
| **Mock 认证默认开** | `useMockAuth = !platformIsReleaseBuild()`；Android 与 OHOS 的 `platformIsReleaseBuild()` 恒 false ⇒ **调试构建下邮箱/短信登录完全不触网** | `MockAuthConfig.kt:56`、`NetworkBootstrap.android.kt:5`、`NetworkBootstrap.ohos.kt:5`、`NetworkBootstrap.kt:29`、分流点 `AuthRepository.kt:35-37`/`:51-57`/`:72-74`/`:103-106` |
| Android 修正 | 入口按 `FLAG_DEBUGGABLE` 再覆盖一次 | `MainActivity.kt:32-33` |
| iOS 修正 | 用 `!Platform.isDebugBinary` | `NetworkBootstrap.ios.kt:10` |
| **OHOS 无修正** | 没有任何地方覆盖 ⇒ OHOS release 包也仍是 Mock | 全仓 grep 只有 `MainActivity.kt:32` 一处赋值 |
| 假 token 形状 | `mock_<phone\\|email>_token_<自增>_<100000..999999>` | `MockAuthConfig.kt:87-90` |
| 假会话字段 | Mock 分支的 `refreshToken`/`sessionId` 走 `commitSession` 默认值 `""` ⇒ **Mock 会话无法静默续期** | `AuthRepository.kt:329-336`/`:342-349` + 默认参数 `:361-362` |
| 假头像 | `picsum.photos` / `api.dicebear.com` 外链 | `AuthRepository.kt:335`、`:348` |
| 假资料兜底 | Mine 昵缺少省 `"qa_user"`、手机号缺少省 `"138****5172"` | `feature/mine/…/MineProfileLogic.kt:28`、`:31` |
| 通讯录整页假数据 | 3 个好友 + 2 个联系人硬编码，搜索/加好友/同意/拒绝全在本地内存 | `FriendDirectory.kt:28-38`、`FriendScreen.kt:167-177`、`:193-201`、`:230-243` |
| 「新的朋友」永不出现 | `incoming` 初值空且无任何赋值来源 | `FriendScreen.kt:99-100`、渲染条件 `:216` |
| IM 引擎是 mock | 会话由 `component` 的 mock 引擎持有，`ImFriendApi.kt:108-115` 的 `// GAP(R5)` 明说融云 IMLib 不可用、不伪装 ready | `FriendDirectory.kt:41`、`ImFriendApi.kt:108-115` |

### 8.3 未接入 / 零调用清单（按「不许说成已实现」逐条点明）

| 能力 | 真实状态 | 证据 |
|------|----------|------|
| **微信登录** | **只有 HTTP 契约，没有取码桥**。iOS `WXApi`、Android `wechat-sdk-android`、OHOS 均无对应能力接入；`LoginScreen` 的微信按钮点击只弹一句「微信登录暂未开放」；`AuthRepository.loginWithWechatCode` / `UserAuthHttpApi.loginWithWechat` 无任何调用者 | `AuthRepository.kt:183-186`（`// GAP(R5)`「勿据此判定微信登录已可用」）、`UserAuthHttpApi.kt:114-119`、`AuthFormWidgets.kt:228`（"SDK may still be stubbed"）、`LoginScreen.kt:289-292` |
| **Apple 登录（Sign in with Apple）** | **仓库里根本不存在该 lane**：`AuthApiPaths` 无对应端点（`AuthApiContracts.kt:14-38`），iOS 导出的 5 个动作只有密码/OTP/注册（`MainViewController.kt:94-142`），Swift 原生页也没有对应按钮 | 上述三处即全量证据；无任何 `apple` 端点或 SDK 调用 |
| **华为登录** | **OHOS 端真实接入**（`LoginWithHuaweiIDButton` + `authorizationCode`），但 Android/iOS 无对应；且该端点在契约表外、`platform` 默认硬写 `"android"` | `HuaweiQuickLoginPane.ets:1`、`:141-146`、`:157`、`AuthConfig.ets:4`、`AuthApiContracts.kt:30-31`、`:89` |
| **远端登出** | `logoutWithRemote()` 完整实现了 Flutter 的 Server-Confirmed Logout（三件齐备 + 失败保会话），但**没有任何调用点**；所有登出入口都走纯本地的 `logout()` | `AuthRepository.kt:210-236`；调用点全量见 §4.5 |
| **用户资料 / 门店 lane（契约 #8–#11）** | 四个方法 + 全部 DTO + 364 行契约测试都在，**生产零调用**；改昵称实际只改内存会话，不 PATCH 服务端 | `UserProfileHttpApi.kt:27`/`:43`/`:52`/`:68`；真实昵称路径 `MineProfileLogic.kt:20` → `AccountFacade.kt:93` |
| **好友 lane（契约 #103–#108）** | 六个方法齐备、有 fake client 契约测试，**生产零调用**；UI 仍走 `FriendDirectory` 硬编码 | `ImFriendApi.kt:122-213`；UI 证据 `FriendScreen.kt:167-177`、`:193-201` |
| `X-Session-ID` / `X-Device-ID` 请求头 | **发不出去**：`ApiClient` 无逐请求头出口，只有 `DemoRequestHeaders` 注入 `Authorization`；而远端登出/互踢语义依赖这两个头 | `UserAuthHttpApi.kt:32-35`、`DemoRequestHeaders.kt:4-13` |
| `AccountFacade.updateProfilePhone` / `updateProfileEmail` / `updateCompanyOrg` | 三方法**零调用**；且 `commitSession` 恒把 `companyId/orgId` 写 null，`updateCompanyOrg` 在双 null 时直接 return（无法清空） | `AccountFacade.kt:114-176`（`:160`）、`AuthRepository.kt:376-377` |
| `LoggedInUser.canRefresh` | 定义了但无人读；刷新路径自己直接判 `refreshToken.isBlank()` | `AccountFacade.kt:231`、`AuthTokenRefresh.kt:66` |
| 旧共享壳 `AppShell` | 文件头 `:95-98` 自述是 **LEGACY**，运行时主路径是 Android Jetpack 壳 / iOS SwiftUI / Harmony ArkTS | `composeApp/…/AppShell.kt:95-98` |
| `AuthPendingCredentials` | 进程级可变全局，跨页传账号；没有清理出口（登录失败/换账号靠页面覆盖） | `AuthPendingCredentials.kt:7-9`、`LoginScreen.kt:67-70`、`LoginOtpScreen.kt:52` |
| `harmonyApp/…/SessionStore.ets` 自身不完整 | 该文件缺 `interface AuthSession` 声明行与 `PREF_NAME` 定义（`:8` 之后直接是字段 `:9`；`:22` 引用未定义的 `PREF_NAME`）。属本分册范围外的观察，但会影响 ArkTS 侧冷启回灌链路 | `harmonyApp/entry/src/main/ets/auth/SessionStore.ets:8-9`、`:22` |

## 9. 未验证疑点（照实列出，不下结论）

1. `AuthBridge.deliver` 在 `Dispatchers.Main` 上投递回调（`AuthBridge.kt:143`），而 OHOS/iOS 是从
   NAPI / Swift 线程调进来的（`MainArkUIViewController.kt:166`、`MainViewController.kt:94-142`）；
   `Dispatchers.Main` 在 `ohosMain`/`iosMain` 无 Compose 运行时的场景下是否有可用主调度器，
   本文**未运行验证**（任务禁止 build）。
2. OHOS 冷启回灌把 `refreshToken`/`sessionId` 写成空串（`AuthApiClient.ets:26-27`），
   因此重启后 401 必然走「耗尽 → 清会话」（`AuthTokenRefresh.kt:64-69`）。
   这条链路是否真的在设备上发生，未做设备验证。
3. `MockAuthConfig.useMockAuth` 的默认值取决于 `platformIsReleaseBuild()`，而 Android 的 actual 恒 `false`
   （`NetworkBootstrap.android.kt:5`）；`MainActivity.kt:32-33` 的覆盖只在 `onCreate` 生效，
   是否存在其它 Android 入口（如 `nativeshell` 单独启动）在未覆盖时误用 Mock，未逐一核对运行时序。
4. `NetworkConfig.skipAuthForUrl` 只在 URL 含 `DemoApiHosts.AUTH_SKIP_HOST = "cdn.demo.local"`
   时省略 `Authorization`（`NetworkConfig.kt:54-55`、`DemoApiHosts.kt:30`）。
   即**真实鉴权端点并不会被该开关豁免**；但全仓是否有配置把 baseUrl 指到该 host，未核。
5. `AuthSegmentedControl` 的右片选中态是 `!leftSelected`（`AuthFormWidgets.kt:97`），
   因此它天然是二态；若未来要三态分段器会静默出错。仅指出形状，不判定为缺陷。
6. `LoginScreen.kt:57-58` 与 `AuthPendingCredentials.kt:9` 都把 `13400000000` 写进生产代码，
   `LoginPasswordScreen.kt:49` 又硬写字面量 6 而非 `AuthValidators.MIN_PASSWORD_LENGTH`（`AuthApiContracts.kt:342`）。
   属实现一致性问题，本文只记录不改码。
7. `harmonyApp/entry/src/main/cpp/napi_init.cpp` 不在文档校验器的扩展名白名单里，
   其中 NAPI 函数注册行（`:46-58`、`:115`、`:147`、`:159`、`:304`、`:317`、`:322`）
   只经阅读核对，未经自动校验。

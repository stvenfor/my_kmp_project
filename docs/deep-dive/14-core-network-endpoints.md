# 14 · 业务端点层与契约对账（core/network 消费侧）

> 本册只覆盖**业务端点层**：各业务域的 `*Api.kt` / `*Repository.kt` 及其 DTO/Request/Response/模型。
> `:core:network` 的引擎与基础设施（`ApiClient` 动词表、`NetworkFacade`、两套 JSON 编码器、
> `ApiEnvelopeParser`、`ListPage`/`queryOf` 家族、`AuthRefreshingApiClient`、`SseTransport`、WS 传输）
> 由同系列的《13 · core/network 引擎与基础设施》承担，本册只在必要处引用契约名，不重复展开。
>
> 对账基准（唯一 SoT）：`openspec/changes/sync-flutter-full-parity/notes/api-contract-2026-10-16.md`，
> 表头 **111 个 verb+path 调用对 / 94 个不同 path 模板**；编号实际到 **#114**。
> 本册逐条对齐 #1–#114，另加契约 §2.17 里那条**无编号**的 `POST /realtime/push`。
>
> 硬性口径：path、动词、字段名照抄代码；每条事实附仓库相对路径与行号；未落地/未验证的写明，不猜测。

## 0. 范围与落点声明（先读，否则会找错目录）

任务描述写的是「`core/network` 下所有业务端点文件」。实测 `:core:network` 里**只有两个业务端点文件**
（realtime 换票与补投）；其余业务端点全部住在 feature/component 层，由它们**消费** `core/network` 的契约。
本册按「业务端点层」的真实形态来写，落点如下。

| 契约组（api-contract §7） | KMP 实际落点（相对路径） | 在 `:core:network`？ |
|---|---|---|
| G11 realtime（#97/#98） | `core/network/src/commonMain/kotlin/com.example.my_kmp_project/core/network/realtime/RealtimeWsTicketApi.kt`、同目录 `RealtimeWsSyncApi.kt` | **是**（唯一） |
| G11 push（#99） | `component/src/commonMain/kotlin/com.example.my_kmp_project/component/push/PushRegistrationApi.kt` | 否（`③component`） |
| G11 AI SSE（#96） | `feature/ai/src/commonMain/kotlin/com.example.my_kmp_project/feature/ai/AiSseApi.kt` | 否 |
| G1 auth/profile（#1–#11） | `feature/auth/src/commonMain/kotlin/com.example.my_kmp_project/feature/auth/`（`UserAuthHttpApi.kt`、`AuthApiContracts.kt`、`UserProfileHttpApi.kt`、`UserProfileContracts.kt`） | 否 |
| G2/G3/G6 home 系（#12–#26、#40–#50） | `feature/home/src/commonMain/kotlin/com.example.my_kmp_project/feature/home/{api,newcar,aftersales}/` | 否 |
| G4 mall（#27–#34，含 #26） | `feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MallApi.kt` | 否 |
| G5 二手车/成交发票/购车计算器（#35–#39、#51–#58） | `feature/mine/src/commonMain/kotlin/com.example.my_kmp_project/feature/mine/{usedcar,dealinvoice,calculator}/` | 否 |
| G3 transactions 变体（#14） | `feature/mine/src/commonMain/kotlin/com.example.my_kmp_project/feature/mine/httpdebug/MineHttpTestApi.kt` | 否 |
| G9 地址（#79–#83） | `feature/mine/src/commonMain/kotlin/com.example.my_kmp_project/feature/mine/address/AddressApi.kt` | 否 |
| G3 钱包（#84–#89） | `feature/wallet/src/commonMain/kotlin/com.example.my_kmp_project/feature/wallet/WalletApi.kt` | 否 |
| G10 会员/支付（#90–#95） | `feature/pay/src/commonMain/kotlin/com.example.my_kmp_project/feature/pay/PayApi.kt`、`MembershipModels.kt` | 否 |
| G7 社区（#59–#71） | `feature/community/src/commonMain/kotlin/com.example.my_kmp_project/feature/community/http/`（3 个文件） | 否 |
| G8 短视频（#72–#78） | `feature/media/src/commonMain/kotlin/com.example.my_kmp_project/feature/media/ShortVideoRepository.kt`、`ShortVideoModels.kt` | 否 |
| G12 RongCloud IM（#100–#102、#109–#114） | `feature/chat/src/commonMain/kotlin/com.example.my_kmp_project/feature/chat/`（5 个文件） | 否 |
| G12 好友（#103–#108） | `feature/friend/src/commonMain/kotlin/com.example.my_kmp_project/feature/friend/ImFriendApi.kt` | 否 |
| 课堂 / 内容 / 直播 | **无 HTTP 端点文件**：`feature/content` 只有 `ContentRouteHost.kt`；`feature/classroom` 只有 mock 逻辑 | 否 |

**引用约定**：下文行号引用写作 `<模块>/<文件>.kt:行`，完整路径 = `<模块>/src/commonMain/kotlin/com.example.my_kmp_project/<模块>/<子包>/<文件>.kt`
（`core/network` 与 `component` 的子包分别是 `core/network/…`、`component/…`）。同目录的文件名在首次出现时给全路径，后续用短名。

---

## 1. 端点层组织方式

### 1.1 一个业务域文件的标准结构

以 `feature/wallet/…/WalletApi.kt` 为模板，其余 19 个域文件都是同一套骨架：

```kotlin
// ① 路径常量：object 里放根前缀 + 拼接函数，绝不把路径字符串散在方法体里
public object WalletApiPaths {                       // WalletApi.kt:132
    public const val BASE: String = "/api/v1/wallet" // :133
    public const val LEDGER: String = "$BASE/ledger" // :134
    public fun card(cardId: Int): String = "$CARDS/$cardId"          // :138
    public fun cardDefault(cardId: Int): String = "$CARDS/$cardId/default" // :140
}

// ② 域类：唯一注入点是默认参数 client = NetworkFacade.api()
public class WalletApi(
    private val client: ApiClient = NetworkFacade.api(),   // WalletApi.kt:28-29
) { ... }

// ③ 每个端点 = 一个 suspend fun，契约行号写在 KDoc（#84…#89）
// ④ 私有收敛函数：requireData / dataOrThrow / guard，统一「中文兜底 + 服务端 message 优先」
private fun <T> ApiResponse<T>.requireData(fallback: String): T   // WalletApi.kt:144-150
```

三种变体：

| 变体 | 代表文件 | 差别 |
|---|---|---|
| object 单例（无状态、方法即端点） | `UserAuthHttpApi.kt:37`、`UserProfileHttpApi.kt:19` | 直接 `NetworkFacade.api()` 调用，不持有 client；`refresh` 例外，必须**显式传入 raw client**（`UserAuthHttpApi.kt:142`） |
| class + 注入 client | 其余 17 个域 | `private val client: ApiClient = NetworkFacade.api()`，测试注入 fake |
| 端口接口 + HTTP 实现 | `CommunityPostRepository.kt` + `HttpCommunityPostRepository.kt:75`、`ShortVideoRepository.kt:23` + `ShortVideoRepository.kt:124`（`class HttpShortVideoRepository`）、`ImMessageBackupApi.kt` 的 `ImBackupService`/`HttpImBackupService:148`/`MockImBackupService:262` | 有 UI 侧历史 mock 实现时先立端口；mock 与 http 各自实现，靠 `useMock: () -> Boolean` 选型 |

### 1.2 命名规律

| 规律 | 事实 | 证据 |
|---|---|---|
| 路径常量 object 名 | `AuthApiPaths` / `ProfileApiPaths` / `MallApiPaths` / `WalletApiPaths` / `PayApiPaths` / `AddressApiPaths` / `UsedCarOrderApiPaths` / `DealInvoiceApiPaths` / `PurchaseCalculatorApiPaths` / `CommunityApiPaths` / `ShortVideoApiPaths` / `ImFriendApiPaths` / `RongImConfig` / `RealtimeConfig` | 各域文件；`RealtimeConfig.TICKET_PATH`（`core/network/…/realtime/RealtimeConfig.kt:21`） |
| 根前缀一律写全 `/api/v1/...` | 不依赖 base URL 拼接 | `MallApi.kt:232`、`CommunityApiPaths`（`HttpCommunityPostRepository.kt:32`）、`ShortVideoApiPaths.ROOT`（`ShortVideoRepository.kt:91`） |
| 方法名动词前缀 | 读 `fetch*`/`list*`/`search*`/`get*`；写 `create*`/`update*`/`patch*`/`delete*`/`add*`/`set*`/`toggle*`/`claim*`/`report*`/`sync*` | 全表见 §3 |
| 契约行号进 KDoc | 每个方法 KDoc 首行标 `#NN`，回指 Dart 文件行号 | 例 `PointsApi.kt:34-60`、`AddressApi.kt:52-91` |
| 兜底文案是中文常量 | 每个域自带一组 `FALLBACK_*` 或内联字面量 | `ShortVideoRepository.kt:217-221`（5 条）、`MallApi.kt` 内联「加载商品失败」等 |

### 1.3 「注册」规律：没有注册表

端点层**没有**路由表、没有注解扫描、没有 DI 容器：调用方直接 `new` 出域类（默认参数即生产 client）。
唯一的「登记」是 `composeApp` 的装配根只注入了引擎侧对象（`composeApp/…/app/AppContainer.kt:30-31`
`internal class NetworkComponent { fun api() = NetworkFacade.api() }`），业务 Api 类**尚未**进入装配根——
影响见 §6。跨域复用被 `scripts/check-layer-deps.sh` 强制（feature→feature 禁止 import），
因此出现「同一份逻辑两处副本」的刻意重复：`ImJsonRead.kt:24-27` 注释点明 `feature/friend`
因分层规则自带私有副本（`ImFriendApi.kt:277-293`）。

### 1.4 JSON 编码器与「显式 null」两套机制

| 场景 | 用什么 | 原因 | 证据 |
|---|---|---|---|
| 普通 DTO | `NetworkJson`（`explicitNulls = false`、`isLenient`、`encodeDefaults`） | null 键自动省略，等价 Dart `if (x != null)` | `AuthRefreshRequest`（`AuthApiContracts.kt:233-253`）、`WalletRechargeRequest.cardId`（`WalletModels.kt:100-104`） |
| 契约要求**发显式 null** 的行（#39/#55/#60/#74） | `NetworkJsonExplicitNulls` | 默认编码器会**丢掉** null 键，必须换 | `UsedCarOrderApi.kt:114-118`、`DealInvoiceApi.kt:104-110`、`HttpCommunityPostRepository.kt:149-169`、`ShortVideoRepository.kt:162-165` |
| 手工 `buildJsonObject` | 任意编码器 | `put(key, value: String?)` 写进去的是 **JsonNull 元素**，不受 `explicitNulls` 影响 | `NewCarFollowApi.kt:84-108`（display_name/phone 恒发）、`AfterSalesApi.kt:80-97`、`PushRegistrationApi.kt:59-65` |
| 裸 body（无信封） | `client.postRaw` + 手写解码 | #97/#98/#99 不套 `{code,data}` | `RealtimeWsTicketApi.kt:36-39`、`RealtimeWsSyncApi.kt:31-35`、`PushRegistrationApi.kt:55-66` |

### 1.5 四种分页方言的归一

| 方言 | 语义 | 本册命中端点 | KMP 写法 |
|---|---|---|---|
| **A 型 `ListData`** | `data.list` 或 `data.items` + `pagination{page,size,total,totalPages?}`；`hasMore` 三级推导 | #14、#26、#27、#31、#36、#38、#41、#45、#52、#54、#59、#61、#66–#69、#73 | `client.getListPage<T>(path, pageQuery(page, size, ...))`；`ListPage.hasMore(pageSize)` |
| **B 型 `items[]` 无分页** | 整表返回 | #15、#16、#19–#21、#24、#57、#79、#101、#103、#104、#107 | 自解 `data.items`；home 侧统一走 `getHomeListPage`（`HomeApiWire.kt:57-81`）/ `getItemsApi`（`ImFriendApi.kt:259-275`） |
| **C 型 `limit`/`offset`（page 0-based）** | `limit=size&offset=page*size` | #12、#85 | `limitOffsetQuery`（`HomeTransactionApi.kt:47-63`）、`queryOf`（`WalletApi.kt:63-73`） |
| **裸 map 自解读** | 形状非 ListData | #47（`{list,pagination{total},can_create}`） | `AfterSalesRecordsWire`（`AfterSalesApi.kt:43-54`） |

`page` 基线：契约 §3「服务端 1-based，0-based 本地要 +1」。代码里三个域各写一次 `+1`：
`HttpCommunityPostRepository.kt:82-91`、`ShortVideoRepository.kt:143-147`、
以及 `TransactionApi.fetchPage` 反向——它本来就 0-based 且改用 limit/offset（`HomeTransactionApi.kt:47-63`）。

### 1.6 错误映射的三种写法（同域内不混用）

| 写法 | 用在 | 细节 |
|---|---|---|
| `guard { }` + `mapXxxFailure(code, message, httpStatus)` → `Result.failure(AuthHttpFailure)` | `UserAuthHttpApi.kt:216-236`、`UserProfileHttpApi.kt` 同构（`profileFailure`） | 全量中文文案表：`AuthApiContracts.kt:373-448`、`UserProfileContracts.kt:279-307` |
| `dataOrThrow(fallback)` / `requireData(fallback)` → 抛 `NetworkError.Business(code, message)` | `MallApi.kt:254-260`、`WalletApi.kt:144-150`、`AddressApi.kt:106-112`、`UsedCarOrderApi.kt:139-145`、`DealInvoiceApi.kt:137-143`、`ShortVideoRepository.kt:199-214` | message 非空即盖兜底（契约 §5.13） |
| `homeBareDataOrThrow` / `homeOkOrThrow` / `homeDataOrThrow` → 抛**裸 `Exception`** | `HomeApiWire.kt:114-137`，用于 #43/#44/#46/#49 | Dart 侧 `after_sales_api` / `new_car_follow_api` 失败抛裸 `Exception`（非 `HttpRequestException`），行为等价保留 |

`NetworkCodes.BFF_OK = 0` 是成功码；缺 `code` → `NetworkCodes.PASSTHROUGH = -1` 判失败；
业务码语义表（10001/10002/10003/10021/10022/10023/10024/50000 + HTTP 401/403/≥500）属引擎册，
端点层只在 auth/profile 两处消费（`AuthApiContracts.kt:373-448`、`UserProfileContracts.kt:279-307`）。

## 2. 业务域分组表

| 业务域 | 端点文件（相对路径，均在 `src/commonMain/kotlin/com.example.my_kmp_project/` 下） | 契约行 | 端点数 | DTO/模型文件 | 消费方（详见 §6） |
|---|---|---|---|---|---|
| auth 登录与会话 | `feature/auth/…/feature/auth/UserAuthHttpApi.kt` | #1–#7 | 7 | `AuthApiContracts.kt` | `AuthRepository.kt`、`AuthTokenRefresh.kt`（**唯一已接线的域**） |
| 账号资料 / 门店 | `feature/auth/…/UserProfileHttpApi.kt` | #8–#11 | 4 | `UserProfileContracts.kt` | **无**（仅 `UserProfileContractJsonTest.kt`） |
| 交易流水（home） | `feature/home/…/feature/home/api/HomeTransactionApi.kt` | #12–#13 | 2 | `HomeTransactionModels.kt` | 仅 `HomeTransactionApiTest.kt:22` |
| 交易流水（Mine 调试页） | `feature/mine/…/feature/mine/httpdebug/MineHttpTestApi.kt` | #14 | 1 | `HarmonyIndexModels.kt` | 仅 `MineHttpTestApiTest.kt:20` |
| home 待办条 | `feature/home/…/api/HomeTodoApi.kt` | #15–#21 | 7 | `HomeTodoApiModels.kt` + 共用 `HomeApiWire.kt`/`HomeTimeWire.kt` | 仅 `HomeTodoApiTest.kt:21` |
| 积分签到 | `feature/home/…/api/PointsApi.kt` | #22–#25 | 4 | `PointsModels.kt` | 仅 `PointsApiTest.kt:24` |
| 积分商城投影 | `feature/commerce/…/feature/commerce/MallApi.kt#fetchPointsGifts` | #26 | 1 | 复用 `MallProductModels.kt`（契约 §6 合并） | 仅 `MallApiContractTest.kt:334,346` |
| 商城 | `feature/commerce/…/MallApi.kt` + `MallRequests.kt` | #27–#34 | 8 | `MallProductModels.kt`、`MallOrderModels.kt` | 仅 `MallApiContractTest.kt` |
| 二手车订单 | `feature/mine/…/usedcar/UsedCarOrderApi.kt` | #35–#39 | 5 | `UsedCarOrderModels.kt` | 仅 `UsedCarOrderApiTest.kt:23` |
| 新车跟进 | `feature/home/…/newcar/NewCarFollowApi.kt` | #40–#46 | 7 | `NewCarFollowModels.kt` | 仅 `NewCarFollowApiTest.kt:26` |
| 售后 | `feature/home/…/aftersales/AfterSalesApi.kt` | #47–#50 | 4 | `AfterSalesModels.kt` | 仅 `AfterSalesApiTest.kt:26` |
| 成交发票 | `feature/mine/…/dealinvoice/DealInvoiceApi.kt` | #51–#56 | 6 | `DealInvoiceModels.kt` | 仅 `DealInvoiceApiTest.kt:24` |
| 购车计算器 | `feature/mine/…/calculator/PurchaseCalculatorApi.kt` | #57–#58 | 2 | `PurchaseCalculatorModels.kt` | 仅 `PurchaseCalculatorApiTest.kt:23` |
| 社区 | `feature/community/…/http/HttpCommunityPostRepository.kt`（端口 `CommunityPostRepository.kt`） | #59–#71 | 13 | `CommunityPostModels.kt` | 仅 `HttpCommunityPostRepositoryTest.kt:119,484`；UI 用 `MockCommunityEngine` |
| 短视频 | `feature/media/…/feature/media/ShortVideoRepository.kt`（含接口+实现） | #72–#78 | 7 | `ShortVideoModels.kt` | 仅 `ShortVideoHttpRepositoryTest.kt:113`；UI 用 `VideoMock`（`VideoRouteHost.kt:106`） |
| 收货地址 | `feature/mine/…/address/AddressApi.kt` | #79–#83 | 5 | `AddressModels.kt` | 仅 `AddressApiTest.kt:23` |
| 钱包 | `feature/wallet/…/feature/wallet/WalletApi.kt` | #84–#89 | 6 | `WalletModels.kt` | 仅 `WalletApiContractTest.kt` |
| 会员 / 支付 | `feature/pay/…/feature/pay/PayApi.kt` | #90–#94（#95 有意不落地） | 5 + 1 编排 | `MembershipModels.kt` | 仅 `MembershipPayContractTest.kt:29,65,68` |
| AI SSE | `feature/ai/…/feature/ai/AiSseApi.kt` | #96 | 1 | 同文件内 `AiStreamEvent` | 仅 `AiSseApiTest.kt:71`；`AiStreamScreen.kt` 未接 |
| realtime | `core/network/…/core/network/realtime/RealtimeWsTicketApi.kt`、`RealtimeWsSyncApi.kt` | #97–#98 | 2 | 同文件 DTO + `RealtimeConfig.kt` | `component/…/component/realtime/AppRealtimeClientImpl.kt:51-52`（参数位，无装配点） |
| push | `component/…/component/push/PushRegistrationApi.kt` | #99 | 1 | 同文件内 `buildJsonObject` | **无任何调用方**（含测试） |
| chat / IM | `feature/chat/…/feature/chat/{ImSessionApi,ImUserProfileApi,ImMessageBackupApi,ImGroupApi,RongImConfig}.kt` | #100–#102、#109–#114 | 10 | 各文件内 + `ImJsonRead.kt` | 仅 `ImChatApiContractTest.kt`；UI 用 `MockImEngine`（`component/…/component/chat/ImEngine.kt:146`） |
| 好友 | `feature/friend/…/feature/friend/ImFriendApi.kt` | #103–#108 | 6 | 同文件内 | 仅 `ImFriendApiContractTest.kt:37`；UI 用 `FriendMockData`（`FriendScreen.kt:65`） |
| 课堂 / 内容 / 直播 | **无端点文件** | — | 0 | — | — |

端点合计（按 verb+path 调用对计）：**114 条契约行 → 代码实现 113 条**（#95 有意缺席）+
契约外扩展 1 条（`POST /api/v1/user/huawei/login`）+ `/realtime/push` 0 条 = **114 个实现调用点**。

---

## 3. 逐端点契约

列说明：`请求` 列写**实际发出的键**（含「恒发 / 省略 / 显式 null」语义）；`响应` 列写模型消费的键；
`调用方` 列写 file:line，无生产调用方时明写「仅测试」。

### 3.1 auth（#1–#7，`feature/auth/…/UserAuthHttpApi.kt`，object）

| 动词 + path | Kotlin 签名 | 请求字段与必填 | 响应字段 | 错误 / 特殊码 | 调用方 |
|---|---|---|---|---|---|
| `POST /api/v1/user/login` | `suspend fun login(username: String, password: String, deviceId: String, platform: String): Result<AuthLoginData>`（`UserAuthHttpApi.kt:40-45`） | `AuthPasswordLoginRequest{username, password, device_id, platform}` 四键**恒发**（`AuthApiContracts.kt:42-47`） | `AuthLoginData{token, refresh_token, session_id, user:AuthUserDto?}`（`AuthApiContracts.kt:148-153`）；`user` 双键大小写 `resolved*` 归一（`:102-141`） | `postLogin` 要求 `data.token.isNotBlank()`，否则折 `AuthHttpFailure`（`:191-202`）；`guard` 统一 `mapAuthFailure`（`:216-236`、`AuthApiContracts.kt:373-448`） | `AuthRepository.kt:38` |
| `POST /api/v1/user/register` | `suspend fun register(username: String, password: String, email: String, deviceId: String, platform: String): Result<AuthRegisterData>`（`:59-64`） | `AuthRegisterRequest{username, password, email, device_id, platform}` 恒发（`AuthApiContracts.kt:66-72`） | `AuthRegisterData` **双形态**：`data` 有 `user` 用 `user`，否则把 `data` 自身当用户；`resolvedUser()` 归一（`AuthApiContracts.kt:163-211`） | 同 `guard`；注册失败文案含 `EmailAlreadyRegistered`/`WeakPassword` 分支 | `AuthRepository.kt:107` |
| `POST /api/v1/user/phone/otp/send` | `suspend fun sendPhoneOtp(phone: String): Result<Unit>`（`:86`） | `AuthPhoneOtpSendRequest{phone}`（E.164 由调用方归一，`AuthApiContracts.kt:51-53`） | `data` 被丢弃（`parseData = { }`） | `10024 invalidOtp` → `InvalidOtpFailure`；`50000` → `BackendServiceFailure`（`AuthApiContracts.kt:373-448`） | `AuthRepository.kt:60`（先 `AuthPhoneUtils.isValidChinaMobile` 本地拦截，`:47-58`） |
| `POST /api/v1/user/phone/otp/verify` | `suspend fun verifyPhoneOtp(phone: String, otp: String, deviceId: String, platform: String): Result<AuthLoginData>`（`:99-104`） | `AuthPhoneOtpVerifyRequest{phone, otp, device_id, platform}`（`AuthApiContracts.kt:57-61`） | 同 #1 | 同 #1（走 `postLogin`） | `AuthRepository.kt:75` |
| `POST /api/v1/user/wechat/login` | `suspend fun loginWithWechat(code: String, deviceId: String, platform: String): Result<AuthLoginData>`（`:121-125`） | `AuthWechatLoginRequest{code, device_id, platform}`（`AuthApiContracts.kt:79-83`） | 同 #1 | 同 #1；`// GAP(R5)`：**取码需原生微信 SDK，桥未接入**（`:117-119`、`AuthRepository.kt:180-186`） | `AuthRepository.kt:192` |
| `POST /api/v1/user/refresh` | `suspend fun refresh(raw: ApiClient, refreshToken: String, deviceId: String?, sessionId: String?, platform: String?): Result<AuthRefreshData>`（`:142-148`） | `AuthRefreshRequest{refresh_token 必填, device_id?, session_id?, platform?}`；后三键 **null 或空白串省略 key**（`AuthRefreshRequest.of`，`AuthApiContracts.kt:233-253`） | `AuthRefreshData{token, refresh_token, session_id}`（`:256-260`） | **必须传 raw client**（绕过 401 装饰器，避免刷新递归，`:142-164`）；耗尽 → `handleRefreshExhausted` | `AuthTokenRefresh.kt:72`（`performRefresh`，`:63`） |
| `POST /api/v1/user/logout` | `suspend fun logout(): Result<Unit>`（`:174`） | **无 body**：`postApi(path, body = "", parseData = { })`（`:175-179`） | `data` 丢弃 | `// GAP(R5)`：契约 §2.1 #4 的**手工 `Authorization`/`X-Session-ID`/`X-Device-ID` 逐请求头未实现**，靠引擎全局拦截器注入（`UserAuthHttpApi.kt:32-35`）；会话已死时**故意不调**（`AuthRepository.kt:215-220`） | `AuthRepository.kt:221`（`logoutWithRemote`，`:210`） |
| —（**契约外扩展**）`POST /api/v1/user/huawei/login` | 不经 `UserAuthHttpApi`，`AuthRepository.loginWithHuaweiCode(code, deviceId)` 内直连 `NetworkFacade.api().postApi<AuthHuaweiLoginRequest, AuthLoginData>`（`AuthRepository.kt:147-157`） | `AuthHuaweiLoginRequest{code, device_id, platform = "android"}`（`AuthApiContracts.kt:89-94`，`platform` 有默认值） | 同 #1 | 空授权码本地拦（`AuthRepository.kt:149-152`）；`data.token.isBlank()` 判失败（`:158-161`） | `AuthRepository.kt:154`；外部入口 `composeApp/src/ohosMain/…/MainArkUIViewController.kt:263` |

`AuthDeviceContext.platform()` 把 `ohos` 映射成 `android` 后再发（`AuthApiContracts.kt:349-364`）——契约未定义该取值，属实现选择，见 §7。

### 3.2 账号资料 / 门店（#8–#11，`feature/auth/…/UserProfileHttpApi.kt`，object）

| 动词 + path | Kotlin 签名 | 请求字段与必填 | 响应字段 | 错误 / 特殊码 | 调用方 |
|---|---|---|---|---|---|
| `GET /api/v1/profiles/me` | `suspend fun fetchMe(storeId: String? = null): Result<UserProfileData>`（`:27-34`） | query `store_id`：`profileMeQuery` 经 `queryOf` 做 trim + 空则省略（`UserProfileContracts.kt:29-30`） | `UserProfileData`：snake/camel **双键并收**（`user_name`/`userName`、`display_name`/`displayName`、`avatar_url`/`avatarUrl`，`resolved*` 按 Dart `??` 顺序择先，`:41-84`）；`stats:UserStoreStatsData` 缺失或非 object → `ZERO`（`:95-167`） | `10001` → 「资料参数无效」（`mapProfileFailure`，`:279-307`） | **无生产调用方**；仅 `UserProfileContractJsonTest.kt` |
| `PATCH /api/v1/profiles/me` | `suspend fun updateMe(request: UserProfileUpdateBody): Result<UserProfileData>`（`:68-75`） | `{user_name?, display_name?, avatar_base64?, avatar_mime?}`：**稀疏 PATCH，null 键省略**；改名时 `user_name` 与 `display_name` **同时写**；`avatar_mime` 只在带 `avatar_base64` 时发且缺省 `image/jpeg`（`UserProfileContracts.kt:239-265`） | 同 #8 | 同 #8；10001 保留会话（契约 §4） | 同上（无） |
| `POST /api/v1/profiles/me/store` | `suspend fun switchStore(storeId: Int): Result<UserStoreStatsData>`（`:52-59`） | `UserStoreSwitchBody{store_id: Int}` 单键恒发（`UserProfileContracts.kt:269-271`） | `UserStoreStatsData{store_id, store_name, days_joined, employee_count, store_days, total_customers, role, role_label}`，全键 snake/camel 双收 + `FlexibleIntSerializer`；`role_label` 空则由 `labelOfRole` 按 0 销售顾问/1 销售经理/2 总经理 推导（`:95-167`） | **返回新 stats 而非布尔**（契约 §5.5 语义保留） | 同上（无） |
| `GET /api/v1/me/stores` | `suspend fun listMyStores(): Result<UserStoreListData>`（`:43-46`） | 无 query | `UserStoreListData{list:[UserStoreListItemData{store_id, store_name, role, role_label, is_current}], current_store_id}`；**只认 `list`，不认 `items`**；`currentStoreId == 0` 表示服务端没表态（`UserProfileContracts.kt:214-228`） | `list` 缺失 → 空列表而非失败（`:224-226`） | 同上（无）。注意：**前缀是 `/me` 而非 `/profiles`**（`ProfileApiPaths.MY_STORES`，`:22`） |

### 3.3 交易流水（#12–#14，同一 path 两套查询方言）

| 动词 + path | Kotlin 签名 | 请求字段与必填 | 响应字段 | 错误 / 特殊码 | 调用方 |
|---|---|---|---|---|---|
| `GET /api/v1/transactions`（limit/offset） | `TransactionApi.fetchPage(page: Int, size: Int = DEFAULT_SIZE, type: String? = null): TransactionListResult`（`HomeTransactionApi.kt:47-52`，`DEFAULT_SIZE = 20` 在 `:38`） | `limitOffsetQuery(page, size, "type" to type)` ⇒ `limit=size&offset=page*size`，**page 0-based**；`type` 空则省略 | `typealias TransactionListResult = ListPage<TransactionModel>`（`HomeTransactionModels.kt:69`）；行 `TransactionModel{type, category, amount:Double, date, id:Int!, user_id/userId, created_at/createdAt, updated_at/updatedAt, note}`（`:15-45`） | **双信封**：`code` 有则走 ListData 分页；无 `code` 时把裸 `{items:[]}` 当成功。**`id` 缺失即抛**（`HomeTransactionModels.kt:26` 是唯一的非空无默认字段）；非 object 行按 `whereType<Map>` 跳过（`:89-93`） | `HomeTransactionApiTest.kt:22`（仅测试） |
| `GET /api/v1/transactions/:id` | `TransactionApi.getTransaction(id: Int): TransactionDetailResult`（`HomeTransactionApi.kt:70`） | 路径参数插值 `"$TRANSACTIONS_PATH/$id"`（`:71`） | `TransactionModel`（同上） | `data == null` → `NetworkError.Business(resp.code, "交易记录不存在")`（`:70-82`）；服务端 message 优先 | `HomeTransactionApiTest.kt:22` |
| `GET /api/v1/transactions`（page/size，Mine 调试页） | `MineHttpTestApi.fetchHarmonyIndex(): HarmonyIndexModel`（`MineHttpTestApi.kt:37`） | `pageQuery(page = PAGE, size = SIZE)`，常量 `PAGE = 1`、`SIZE = 20`（`:49-50`）；path 常量独立声明（`:46`） | 走 `decodeListPage` 的宽松行：`items`/`list` 都吃、非 object 跳过，再由 `harmonyIndexOf` 折成**单个 transactions section**（`HarmonyIndexModels.kt:69-86`，`DEFAULT_CATEGORY = "未分类"` 在 `:97`） | **完全不看业务码**（不判 `code`、不判 `isSuccess`），只要求 `data` 非 null，否则 `NetworkError.Business(code, "接口返回数据为空")`（`:37-42`）；与 #12 分开实现以避免跨 feature import（契约 §7.1） | `MineHttpTestApiTest.kt:20`；`MineRouteHost` 的 `BuyQaScreen` 仍是硬编码 mock（契约 §7.1 遗留，本册确认仍未接） |
### 3.4 home 待办条（#15–#21，`feature/home/…/api/HomeTodoApi.kt`）

5 条路径常量：`TODO_CARDS_PATH`（`:22`）、`JOIN_APPLICATIONS_PATH`（`:25`）、`FOLLOW_UP_PATH`（`:28`）、
`APPOINTMENTS_PATH`（`:31`）、`REVIEW_ORDERS_PATH`（`:34`）。全部 GET 无 query、无 body；
#17/#18 是**无 body 的动作 POST**（`postActionOk(path, fallback)`，`HomeTodoApi.kt:85-86`，`body = ""`）。

| 动词 + path | Kotlin 签名 | 请求 | 响应字段（B 型，`getHomeListPage`） | 错误 / 特殊码 | 调用方 |
|---|---|---|---|---|---|
| `GET /api/v1/home/todo-cards` | `fetchTodoCards(): List<HomeTodoCard>`（`:38`） | 无 | `HomeTodoCard{type, title, subtitle, action_label, action_route, count, image_url?}`；卡片尺寸 `HomeTodoCardSize.fromType(type)` 客户端推导（`HomeTodoApiModels.kt:33-37`） | `getHomeListPage`（`HomeApiWire.kt:57-81`，`data.items` 或**裸数组都吃**）→ `homeDataOrThrow("加载待办卡失败")`（`:114-118`）；服务端 message 非空即盖兜底 | `HomeTodoApiTest.kt:21` |
| `GET /api/v1/home/join-applications` | `fetchJoinApplications(): List<HomeTodoJoinApplication>`（`:44`） | 无 | `{application_id, store_id, applicant_user_id, applicant_name, status, created_at→DateTime?}`（`homeIsoDateTimeOrNull` 正则解析，`HomeTimeWire.kt:18-27`） | 兜底「加载入店申请失败」（`:44-47`） | 同上 |
| `POST /api/v1/home/join-applications/:applicationId/approve` | `approveJoin(applicationId: Int): Unit`（`:53-55`） | **无 body**（`body = ""` ⇒ `postApi` 不调 `setBody`，与 Dart「不传 data」等价，KDoc `:80-84`），只看 `code` | `data` 丢弃 | `postActionOk` → `homeOkOrThrow("确认失败")`（`:85-90`、`HomeApiWire.kt:121-124`） | 同上 |
| `POST /api/v1/home/join-applications/:applicationId/reject` | `rejectJoin(applicationId: Int): Unit`（`:57-59`） | 同上 | 同上 | 兜底「拒绝失败」 | 同上 |
| `GET /api/v1/home/follow-up-customers` | `fetchFollowUpCustomers(): List<HomeTodoFollowUpCustomer>`（`:61`） | 无 | `{customer_id, display_name, next_follow_up_at:DateTime?}`；`displayName` 缺省「客户」（`HomeTodoApiModels.kt`） | 兜底「加载待跟进客户失败」 | 同上 |
| `GET /api/v1/home/after-sales-appointments` | `fetchAppointments(): List<HomeTodoAppointment>`（`:67`） | 无 | `{appointment_id, customer_name, appointment_date:DateTime?, status}`；`customerName` 缺省「预约客户」 | 兜底「加载售后预约失败」 | 同上 |
| `GET /api/v1/home/store-review-orders` | `fetchReviewOrders(): List<HomeTodoReviewOrder>`（`:73`） | 无 | `{order_id, title, status, created_at:DateTime?}`；`title` 缺省「审核单」 | 兜底「加载店务审核单失败」 | 同上 |

### 3.5 积分（#22–#26）

| 动词 + path | Kotlin 签名 | 请求 | 响应字段 | 错误 / 特殊码 | 调用方 |
|---|---|---|---|---|---|
| `GET /api/v1/points/status` | `PointsApi.fetchStatus(): PointsStatus`（`PointsApi.kt:34`） | 无 | `PointsStatus{balance, checked_in_today, streak, today_reward(默认 5), calendar:[CheckInDayView{day, signed, reward, is_today}]}`（`PointsModels.kt`） | 兜底「加载签到状态失败」（`:35`）；服务端 message 优先 | `PointsApiTest.kt:24` |
| `POST /api/v1/points/check-in` | `checkIn(): CheckInResult`（`:38-41`） | **无 body**（`postApiDecoded(body = "")`） | `CheckInResult{points, balance, streak, day}` | at-least-once：幂等**只靠客户端防双击**（契约 §5.3）；兜底「签到失败」（`:40`） | 同上 |
| `GET /api/v1/points/tasks` | `fetchTasks(): List<GrowthTask>`（`:50-53`） | 无 | `[GrowthTask{code:String, title, reward, progress:'incomplete'\|'claimable'\|'claimed'}]`；`TaskProgress.parse` 未知值 → `Incomplete`（`PointsModels.kt:107-112`） | 手搓转换器：`data.items` 否则**根 `items`** ⇒ `getHomeListPage(rootFallbackWhenDataMissing = true)`（`:51`）；兜底「加载成长任务失败」（`:52`） | 同上 |
| `POST /api/v1/points/tasks/:code/claim` | `claimTask(code: String): ClaimTaskResult`（`:59-61`） | 无 body；`:code` 是**字符串任务码**（`"$TASKS_PATH/$code/claim"`，`:60`） | `ClaimTaskResult{points, balance, day}` | 兜底「领取失败」（`:61`）；同 at-least-once | 同上 |
| `GET /api/v1/mall/stores/:storeId/products`（积分投影，#26） | `MallApi.fetchPointsGifts(storeId: Int = DEFAULT_POINTS_STORE_ID, page: Int = 1, size: Int = POINTS_GIFT_PAGE_SIZE): List<MallProductCardModel>`（`MallApi.kt:171-182`，常量 `:194-201`） | `pageQuery(page, size)`；**`storeId` 默认 1**（Dart 硬编码） | 复用商城行 DTO：只保留 `pricePoints > 0`（纯函数 `pointsGiftProjection`，`:223-224`） | **不看业务码**：`code != 0` 也照读；`data` 为 null → 空列表；**整个调用 `runCatching` 吞掉一切异常返回 `emptyList()`**（`:178-182`）。`PointsApi.kt:12-14` KDoc 明确声明 #26 已从积分域移入 `MallApi` | `MallApiContractTest.kt:334, 346` |

### 3.6 商城（#27–#34，`feature/commerce/…/MallApi.kt`）

| 动词 + path | Kotlin 签名 | 请求字段与必填 | 响应字段 | 错误 / 特殊码 | 调用方 |
|---|---|---|---|---|---|
| `GET /api/v1/mall/stores/:storeId/products` | `fetchShelfPage(storeId: Int, page: Int, size: Int = PAGE_SIZE): ListPage<MallProductCardModel>`（`:33-43`，`PAGE_SIZE = 10` `:194`） | `pageQuery(page, size)`，page **1-based** | `MallProductCardModel{product_id?:id → id, sku_id, title, price:String("0.00"), cover_url, cover_aspect, kind, subtitle?, price_points}`；`soldLabel`/`badges`/`priceLabel` 由 **`id.hashCode` 客户端派生**（`MallProductModels.kt:64-73`） | 兜底「加载商品失败」（`requireData` `:254-260`） | `MallApiContractTest.kt:34` |
| `GET /api/v1/mall/stores/:storeId/products/:productId` | `fetchDetail(storeId: Int, productId: String): MallProductDetailResponse`（`:46-54`） | 路径参数；`productId` 是 **String** | `{product: MallProductPayload, skus:[MallSkuOffer{sku_id, title, price, price_points, stock_qty, specs:JsonObject, label, deliver_type?}]}` 摊平成详情（`MallProductModels.kt:157-168`） | 兜底「加载详情失败」（`:53`） | `MallApiContractTest.kt:81` |
| `POST /api/v1/mall/cart` | `addToCart(skuId: Int, qty: Int)`（`:57-60`） | `MallAddToCartRequest{sku_id, qty}`（`MallRequests.kt:19-23`） | `data` 只判非空后即丢 | 走 `requireRawMapPost`（`:185-190`）：解码成 `JsonObject` 再 `requireData`，兜底「加入购物车失败」 | `MallApiContractTest.kt:274` |
| `POST /api/v1/mall/orders` | `createOrder(storeId: Int, skuId: Int, qty: Int, receiverName: String = "", receiverPhone: String = "", receiverAddress: String = ""): Int`（`:69-77`） | `MallCreateOrderRequest{idempotency_key, store_id, lines:[{sku_id, qty}], receiver_name, receiver_phone, receiver_address}`；receiver 三件套默认 `''` 且**恒发**（`MallRequests.kt:36-44`）；键值 `mallIdempotencyKey(skuId) = "$skuId-$micros"`（`:80-81`） | `MallCreateOrderResponse{order: MallOrderHead?}`，只取 `order.order_id` | `order_id <= 0` → 抛 `NetworkError.Business(NetworkCodes.PASSTHROUGH, "下单失败：缺少订单号")`（`:90-94`）；**幂等键每次重新生成 ⇒ 客户端超时重试会产生第二单**（契约 §5.3，KDoc `MallRequests.kt:71-79`） | `MallApiContractTest.kt:109` |
| `GET /api/v1/mall/orders` | `fetchOrders(page: Int, size: Int = PAGE_SIZE, status: String? = null): ListPage<MallOrderListRow>`（`:127-137`） | `pageQuery(page, size, "status" to status)`；**`status` 是逗号串**：`MallOrderStatusQuery.UNPAID = "0"` / `PAID = "1,2"` / `CLOSED = "3,4"` / `null`或`""`=全部（`:208-217`） | `MallOrderListRow{order_id, order_no, store_id, status, amount, created_at, pay_deadline_at, items:[MallOrderItem]}`（`MallOrderModels.kt`） | 兜底「加载订单失败」；空串走 `pageQuery` 省略规则 | `MallApiContractTest.kt:174-194` |
| `GET /api/v1/mall/orders/:orderId` | `fetchOrderDetail(orderId: Int): MallOrderDetailResponse`（`:140-143`） | 无 query | `{order:{…}, items:[MallOrderItem], pay_deadline_at}`——**`pay_deadline_at` 读根 `data`，不在 `order` 里**（`MallOrderModels.kt:62-67`） | 兜底「加载订单详情失败」 | `MallApiContractTest.kt:229` |
| `POST /api/v1/mall/orders/:orderId/pay` | `payOrder(orderId: Int, paymentChannel: Int): MallOrderDetailResponse`（`:146-152`） | `MallPayOrderRequest{payment_channel: Int}`（`MallRequests.kt:47-50`） | 同 #32 | 兜底「支付失败」 | `MallApiContractTest.kt:258` |
| `POST /api/v1/mall/orders/:orderId/cancel` | `cancelOrder(orderId: Int)`（`:158-161`） | **无 body**（`body = ""`）；取消是 POST，不是 PATCH/DELETE | 原始 Map，丢弃 | `requireRawMapPost`，兜底「取消订单失败」 | `MallApiContractTest.kt:261, 288` |
| —（兼容包装，非独立契约行） | `buy(storeId: Int, skuId: Int, qty: Int, paymentChannel: Int, receiverName/Phone/Address: String = ""): String`（`:100-118`） | 内部 `createOrder` → `payOrder` | 返回 `orderNo` | **两步下单不可合并**（契约 §5.2）；#30→#33 | `MallApiContractTest.kt:156` |

契约 §2.6 附注的 `mall_order_detail_controller.dart:75` 只读 `GET /wallet` 的 `data.balance`（失败吞掉）——
KMP 由 `WalletApi.fetchBalance()` 承担，见 §3.15。

### 3.7 二手车订单（#35–#39，`feature/mine/…/usedcar/UsedCarOrderApi.kt`）

路径：`BASE = "/api/v1/used-car-orders"`（`:20`）、`SUMMARY`（`:23`）、`CUSTOMERS`（`:26`）、`detail(orderId)`（`:29`）。

| 动词 + path | Kotlin 签名 | 请求 | 响应字段 | 错误 / 特殊码 | 调用方 |
|---|---|---|---|---|---|
| `GET /api/v1/used-car-orders/summary` | `fetchSummary(): UsedCarOrderSummary`（`:50`） | 无 | `{display_name, avatar_url, position_label, store_name, stats:{submitted, pending_review, approved, rejected}}`；`stats` 缺省 `{}`（`UsedCarOrderModels.kt`） | 兜底「加载摘要失败」（`:54`） | `UsedCarOrderApiTest.kt:23` |
| `GET /api/v1/used-car-orders` | `fetchList(statusTab: UsedCarStatusTab, kindFilter: UsedCarKindFilter, page: Int, size: Int = DEFAULT_SIZE): UsedCarOrderListResult`（`:66-71`） | `pageQuery(page, size, "status" to tab, "kind" to filter.apiKind)`；`kind` 为 `all` 时**省略键**（`UsedCarKindFilter.apiKind: all → null`，`UsedCarOrderModels.kt:67-93`） | `ListPage<UsedCarOrderItem>`；`hasMore` 用 `ListPage.hasMore(pageSize = size)` 即 `page*size < total` | 兜底「加载列表失败」 | 同上 |
| `GET /api/v1/used-car-orders/:orderId` | `fetchDetail(orderId: String): UsedCarOrderItem`（`:87`） | 路径参数是 **String** | `UsedCarOrderItem{order_id:**String**, kind, kind_label, amount_label, customer_name, phone, vehicle_model, plate_no, vin, mileage_km, model_year, amount:Double, status, submitted_at, reject_reason?, rating_stars?, image_url?}`；`statusLabel` 四态 + 未知原样（`UsedCarOrderModels.kt:149-156`） | 兜底「加载订单详情失败」 | 同上 |
| `GET /api/v1/used-car-orders/customers` | `fetchCustomers(page: Int = 1, size: Int = CUSTOMERS_SIZE, q: String? = null): List<UsedCarCustomer>`（`:98-107`，`CUSTOMERS_SIZE = 50` `:125`） | `pageQuery(page, size, "q" to q?.trim())`——trim 后空则省略 | `List<UsedCarCustomer{customer_id, display_name, phone}>`；**没有 `name` 兜底键**（与成交发票客户模型不同，`UsedCarOrderModels.kt:164-173`） | 兜底「加载客户失败」 | 同上 |
| `POST /api/v1/used-car-orders` | `create(request: UsedCarOrderCreateRequest): UsedCarOrderItem`（`:114-118`） | `{kind, customer_id, vehicle_model, plate_no, vin, mileage_km, model_year, amount, image_url?}`：8 键必填 + `image_url` **恒发（含 null）** ⇒ 用 `NetworkJsonExplicitNulls`（`UsedCarOrderModels.kt:196-206`） | `UsedCarOrderItem` | 建单即 `pending_review`；兜底「提交失败」 | 同上 |

### 3.8 新车跟进（#40–#46，`feature/home/…/newcar/NewCarFollowApi.kt`，`BASE_PATH = "/api/v1/new-car-follow-files"` `:32`）

| 动词 + path | Kotlin 签名 | 请求 | 响应字段 | 错误 / 特殊码 | 调用方 |
|---|---|---|---|---|---|
| `GET …/summary` | `fetchSummary(): NewCarFollowSummary`（`:42`） | 无 | `{display_name, avatar_url, position_label, store_name, stats:{active, overdue, high_intent, lost}}`；stats 缺省全 0（`NewCarFollowModels.kt`） | 兜底「加载摘要失败」（`:44`） | `NewCarFollowApiTest.kt:26` |
| `GET …`（列表） | `fetchList(tab: NewCarFollowTab, page: Int, size: Int = DEFAULT_LIST_SIZE): NewCarFollowPage<NewCarFollowFile>`（`:53-58`） | `pageQuery(page, size)` + tab：`intent_band` 取 **中文字面量 `高`/`中`/`低`**，`overdue` 时带 `overdue = "1"`，`all` 时不带（`NewCarFollowModels.kt:21-28`）；page 1-based | `NewCarFollowPage{list, hasMore}`（`:195-198`）；行 `NewCarFollowFile{file_id, customer_id, customer_name, customer_phone, follow_level, intent_band, stage, vehicle_interest, next_follow_up_at?, owner_user_id, owner_display_name}`，**file_id/customer_id 一律字符串化**（`:92-140`） | 走 `getHomeListPageOrNull`（`:92-105`）——data 缺失时返回 null 页而非抛，交由上层保留旧列表 | 同上 |
| `GET …/:fileId` | `fetchDetail(fileId: String): NewCarFollowFile`（`:69-71`） | 路径参数 String | `NewCarFollowFile` | `homeBareDataOrThrow("加载详情失败")` ⇒ **裸 `Exception`** | 同上 |
| `POST …` | `create(followLevel: String, customerId: Int? = null, displayName: String? = null, phone: String? = null, vehicleInterest: String? = null, nextFollowUpAt: String? = null): NewCarFollowFile`（`:84-91`） | `buildJsonObject` **互斥分支**：`customerId != null && customerId > 0` 时只发 `customer_id`；否则发 `follow_level + display_name + phone`，且两键**即使 null 也发**（JsonNull 不受 `explicitNulls` 影响）；`vehicle_interest` 只在 trim 非空时发（`:92-108`） | `NewCarFollowFile` | 裸 `Exception`，兜底「建档失败」（`:107`） | 同上 |
| `PATCH …/:fileId` | `patch(fileId: String, followLevel: String? = null, stage: String? = null, nextFollowUpAt: String? = null): NewCarFollowFile`（`:116-125`） | 稀疏 `{follow_level?, stage?, next_follow_up_at?}`，null 省略（`NewCarFollowApi.kt:170-174`） | `NewCarFollowFile`；`stage`: `new/following/test_drive/quoted/won/lost`，level `H/A/B/E` | 裸 `Exception`，兜底「更新失败」（`:125`） | 同上 |
| `GET …/:fileId/logs` | `fetchLogs(fileId: String, page: Int = 1, size: Int = DEFAULT_LOG_SIZE): NewCarFollowPage<NewCarFollowLog>`（`:128-133`，`DEFAULT_LOG_SIZE = 20` `:38`） | `pageQuery(page, size)` | `ListPage<NewCarFollowLog{log_id, file_id, author_user_id, author_display_name, body, follow_level, intent_band, next_follow_up_at?, created_at}>` | 兜底「加载流水失败」（`:136`） | 同上 |
| `POST …/:fileId/logs` | `createLog(fileId: String, body: String, followLevel: String? = null, nextFollowUpAt: String? = null): NewCarFollowLog`（`:152-165`） | `{body 必填, follow_level?, next_follow_up_at?}`：`follow_level` **非空串才发**，`next_follow_up_at` 只要非 null 就发（**空串也发**）（`:160-164`） | `NewCarFollowLog` | 裸 `Exception`，兜底「写流水失败」；服务端可借此移动档案 level | 同上 |

### 3.9 售后（#47–#50，`feature/home/…/aftersales/AfterSalesApi.kt`，`BASE_PATH = "/api/v1/after-sales"` `:26`）

| 动词 + path | Kotlin 签名 | 请求 | 响应字段 | 错误 / 特殊码 | 调用方 |
|---|---|---|---|---|---|
| `GET …/records` | `fetchRecords(page: Int, size: Int = DEFAULT_LIST_SIZE): AfterSalesListResult`（`:39-42`，`DEFAULT_LIST_SIZE = 10` `:29`） | `pageQuery(page, size)` | **裸 map**：`AfterSalesRecordsWire{list:[AfterSalesRecord], pagination{total}, can_create}`（`:43-54`）；`canCreate` 保留为「新建」入口 gate（`AfterSalesModels.kt:112-116`），且只认 **JSON 字面量 true**（`homeWireLiteralTrue`，`HomeApiWire.kt:176-177`） | 兜底「加载列表失败」；`service_kind` **读 Int / 写 String 不对称**（`AfterSalesModels.kt:53-57`） | `AfterSalesApiTest.kt:26` |
| `GET …/records/:recordId` | `fetchDetail(recordId: Int): AfterSalesRecord`（`:57-58`） | 路径参数 Int | `AfterSalesRecord{record_id, store_id, appointment_id?, customer_name, customer_phone, service_kind, service_kind_label, title, content, service_date, plate_no, mileage?}` | 兜底「加载详情失败」；裸 `Exception` | 同上 |
| `POST …/records` | `create(serviceKind: String, title: String, serviceDate: String, customerName: String? = null, customerPhone: String? = null, appointmentId: Int? = null, plateNo: String? = null, mileage: Int? = null, content: String? = null): AfterSalesRecord`（`:70-79`） | `buildJsonObject`：`service_kind/title/service_date/customer_name/customer_phone` **5 键恒发**（null 也落 JsonNull）+ 条件 4 键 `appointment_id`、`plate_no`（trim）、`mileage`、`content`（trim）（`:80-94`） | `AfterSalesRecord` | **失败抛裸 `Exception`（非 HttpRequestException 等价物）**，兜底「新建失败」（`:95-96`） | 同上 |
| `GET …/pending-appointments` | `fetchPendingAppointments(): List<AfterSalesAppointment>`（`:103-107`） | 无 | `{list:[{appointment_id, customer_name, appointment_date}]}`；日期按 `'T'` 截断（`homeDatePart`，`HomeTimeWire.kt:37`——**null 会变字符串 "null"，bug-for-bug 保留**） | 兜底「加载预约失败」（`:105`） | 同上 |
### 3.10 成交发票（#51–#56，`feature/mine/…/dealinvoice/DealInvoiceApi.kt`）

路径：`BASE = "/api/v1/deal-invoices"`（`:21`）、`SUMMARY`（`:24`）、`CUSTOMERS`（`:27`）、
`detail(invoiceId)`（`:30`）、`resubmit(invoiceId)`（`:33`）。

| 动词 + path | Kotlin 签名 | 请求 | 响应字段 | 错误 / 特殊码 | 调用方 |
|---|---|---|---|---|---|
| `GET …/summary` | `fetchSummary(): DealInvoiceSummary`（`:49`） | 无 | `{display_name, avatar_url, position_label, store_name, stats:{uploaded, pending_review, approved, rejected}}`；stats 缺省全 0（`DealInvoiceModels.kt`） | 兜底「加载摘要失败」（`:53`） | `DealInvoiceApiTest.kt:24` |
| `GET …` | `fetchList(tab: DealInvoiceTab, page: Int, size: Int = DEFAULT_SIZE): DealInvoiceListResult`（`:63-66`，`DEFAULT_SIZE = 10` `:129`） | `pageQuery(page, size, "status" to tab.apiStatus)`；**`status = 'all'` 也照发**（`DealInvoiceTab.apiStatus`，`DealInvoiceModels.kt:52-57`） | `ListPage<DealInvoiceItem{invoice_id\|id:**String**, phone, customer_name?, status, submitted_at, reject_reason?, rating_stars?, image_url?}>`（`:104-116`）；`matchesTab` 的 approved 档**吃两态**（`approved_pending_rating` + `rated`，`:142-149`） | 兜底「加载列表失败」（`:70`）；`DealInvoiceStatus.fromApi` 未知值 → `PendingReview`（`:39-44`） | 同上 |
| `GET …/:invoiceId` | `fetchDetail(invoiceId: String): DealInvoiceItem`（`:78`） | 路径参数 String | `DealInvoiceItem` | 兜底「加载详情失败」（`:82`） | 同上 |
| `GET …/customers` | `fetchCustomers(page: Int = 1, size: Int = CUSTOMERS_SIZE, q: String? = null): List<DealInvoiceCustomer>`（`:88-91`，`CUSTOMERS_SIZE = 50` `:132`） | `pageQuery(page, size, "q" to q?.trim())` | `List<DealInvoiceCustomer{customer_id\|id:**Int**, phone, display_name\|name}>`（`:160-177`，**有 `name` 兜底键**，与二手车客户模型不同） | 兜底「加载客户失败」（`:95`） | 同上 |
| `POST …` | `create(customerId: Int, imageUrl: String? = null): DealInvoiceItem`（`:104`） | `DealInvoiceCreateRequest{customer_id, image_url}` **两键恒发（含 null）** ⇒ 用 `NetworkJsonExplicitNulls`（`:104-110`、`DealInvoiceModels.kt:195-198`） | `DealInvoiceItem` | 兜底「提交失败」（`:109`） | 同上 |
| `PATCH …/:invoiceId/resubmit` | `resubmit(invoiceId: String, imageUrl: String? = null): DealInvoiceItem`（`:116`） | `DealInvoiceResubmitRequest{image_url}` **恒发**（含 null，同样走 ExplicitNulls，`:116-125`、`DealInvoiceModels.kt:206-208`） | `DealInvoiceItem` | 兜底「重提失败」（`:124`）；驳回后重提 | 同上 |

### 3.11 购车计算器（#57–#58，`feature/mine/…/calculator/PurchaseCalculatorApi.kt`）

| 动词 + path | Kotlin 签名 | 请求 | 响应字段 | 错误 / 特殊码 | 调用方 |
|---|---|---|---|---|---|
| `GET /api/v1/purchase-calculator/products` | `listProducts(): List<FinanceProduct>`（`:52`） | 无 | `{items:[FinanceProduct{id, name, annual_rate_percent, allowed_terms_months:[Int], min_down_payment_percent, one_time_fee, subsidy_type, subsidy_rate_cut_percent, subsidy_amount_cut, compulsory_insurance, commercial_rate_percent}]}`（`PurchaseCalculatorModels.kt`，11 字段） | `items` 非 List → `NetworkError.Business(NetworkCodes.PASSTHROUGH, "产品列表为空")`（`:52-65`）；非 object 行跳过 | `PurchaseCalculatorApiTest.kt:23` |
| `POST /api/v1/purchase-calculator/quote` | `quote(request: PurchaseQuoteRequest): PurchaseQuote`（`:72`） | `PurchaseQuoteRequest{mode, bare_price, disable_commercial = false}` **3 键恒发** + 条件 `taxable_price, product_id, down_payment, term_months`（`PurchaseCalculatorModels.kt:119-127`） | `PurchaseQuote` 15 键：`mode, product_id?, product_name, bare_price, taxable_price, down_payment, loan_amount, term_months, effective_annual_rate_percent, monthly_payment, total_interest, total_repayment, initial_payment, total_due, lines:[PurchaseQuoteLine{code, label, amount}]}`（JSON 键 `effective_annual_rate_percent` ↔ Kotlin 名 `effectiveAnnualRate`，`:79`） | `data` null → `NetworkError.Business(code, "报价结果为空")`（`:72-85`）；报价**以服务端为准** | 同上 |

**重试语义未复刻（重要偏差，见 §5.2）**：Dart 该模块配 `maxRetries: 3`（契约 §0/§4），
KMP `:core:network` **没有 RetryInterceptor 对等物**；`PurchaseCalculatorApi.kt:26-37` 的 KDoc 明确记录
「GET 可能重复 / POST 可能重复下单」的风险未被承接。

### 3.12 社区（#59–#71，`feature/community/…/http/HttpCommunityPostRepository.kt`，`PREFIX = "/api/v1/community"` `:32`）

端口 `CommunityPostRepository.kt:32-130` 共 14 个方法，覆盖 13 条契约行（#63/#64 合并为 `toggleLike`）。
**本地 page 0-based，实现内 `+1`**（`CommunityPostRepository.kt:9-16`、`HttpCommunityPostRepository.kt:82-91`）。
`listPage` 私有实现**不复用 `getListPage`**：`data` 缺失也抛，避免静默空页（`:273-282`）；
失败文案 `communityFailure` 服务端 message 优先（`:305-312`）；`communityQuery` **无条件带键**（`:320-324`）。

| 动词 + path | Kotlin 签名 | 请求 | 响应字段 | 错误 / 特殊码 | 调用方 |
|---|---|---|---|---|---|
| `GET /community/posts` | `fetchPosts(page: Int, pageSize: Int = 10, tab: String = "latest"): List<PostModel>`（`:82-91`；端口 `CommunityPostRepository.kt:32-36`） | `{page: page+1, size: pageSize, tab}`；tab `latest`/`hot`/`following` | `List<PostModel>`（**只返 list**）；`PostModel` 19 个 raw 键，`images` 优先再 `image_urls` 且过滤空串（`CommunityPostModels.kt:275-276`），`isAskEveryone` 可由 `topic.is_ask_everyone` 兜底（`:304-305`） | `data` 缺失即抛「加载失败」族文案 | `HttpCommunityPostRepositoryTest.kt:119` |
| `POST /community/posts` | `createPost(content: String, mediaType: String = "none", topicId: String? = null, isAskEveryone: Boolean = false, source: String = CommunityDefaultSource): PostModel`（`:149-169`） | `PostCreateRequest{content, media_type = "none", image_urls = []（恒发）, video_url = null, video_cover_url = null, topic_id?, is_ask_everyone, source = "来自 iPhone"}`（`CommunityPostModels.kt:380-389`，`CommunityDefaultSource` 在 `:31`）⇒ **`NetworkJsonExplicitNulls`** | `PostModel` | 兜底「发布失败」（`:168`） | 同上 |
| `GET /community/posts/:postId/comments` | `fetchComments(postId: String): List<CommentModel>`（`:94-99`） | **写死 `page=1&size=50`** | `List<CommentModel{id, post_id, nickname, avatar, content, create_time, reply_to_nickname?}>`；`createTimeMs` 解析失败回落 `now`（`CommunityPostModels.kt`） | 同上 | 同上 |
| `POST /community/posts/:postId/comments` | `addComment(postId: String, content: String, replyToNickname: String? = null): CommentModel`（`:102-113`） | `CommentCreateRequest{content, reply_to_nickname?}`；null 省略（`CommunityPostModels.kt:397-400`） | `CommentModel` | 兜底「评论失败」（`:113`） | 同上 |
| `POST`/`DELETE` `/community/posts/:postId/like` | `toggleLike(postId: String, liked: Boolean): PostModel`（`:120-128`） | **同 path 切动词**，无 body | 片段 `{id, like_count, is_liked}` → `postModelFromLikeFragment`（`CommunityPostModels.kt`） | 兜底「操作失败」（`:127`） | 同上 |
| `DELETE /community/posts/:postId` | `deletePost(postId: String)`（`:134-139`） | 无 body | `data` 可空 | `deleteApiOk` **只看 `code`** | 同上 |
| `GET /community/topics` | `fetchTopics(page: Int = 0, pageSize: Int = 20): List<TopicModel>`（`:172-181`） | `{page: page+1, size}` | `List<TopicModel{id, name, heat, is_ask_everyone}>`；`heatLabel` 万位规则、`displayName = "#name"` | 同上 | 同上 |
| `GET /community/topics/search` | `searchTopics(query: String, page: Int = 0, pageSize: Int = 20): List<TopicModel>`（`:183-192`） | `{q（总发，含空串）, page+1, size}` | `List<TopicModel>` | **与 #68/#69 的 `/community/search` 是两个不同端点**（KDoc `:86-89`） | 同上 |
| `GET /community/search?type=all` | `searchAll(q: String, page: Int = 0, pageSize: Int = 5): CommunitySearchAllResult`（`:216-224`） | `{q, type = "all", page+1, size = 5}` | `{q, posts, topics, users}` 三个**独立分页子对象**，各自 `JsonElement → decodeListPage`（`CommunityPostModels.kt:358-372`） | 兜底「搜索失败」（`:224`） | 同上 |
| `GET /community/search?type=post\\|topic\\|user` | `searchPosts(q, page = 0, pageSize = 10): ListPage<PostModel>`（`:227-233`）、`searchTopicsPage(...): ListPage<TopicModel>`（`:234-240`）、`searchUsers(...): ListPage<CommunityUserHit>`（`:241-246`） | 同上，仅 `type` 变 | **同端点靠 `type` 切响应形状**（契约 §5.9）；`CommunityUserHit.isFollowed` 只认 `== true` | 同上 | 同上 |
| `POST`/`DELETE` `/community/users/:userId/follow` | `followUser(userId: String)`（`:198-203`）/ `unfollowUser(userId: String)`（`:208-213`） | 无 body，同 path 切动词 | 原始 Map | `*ApiOk` 只看 `code` | 同上 |

### 3.13 短视频（#72–#78，`feature/media/…/ShortVideoRepository.kt`）

`ROOT = "/api/v1/short-videos"`、`PROFILE = "$ROOT/profile"`、`like(id) = "$ROOT/$id/like"`、
`view(id) = "$ROOT/$id/view"`、`item(id) = "$ROOT/$id"`（`:90-97`）。
5 条兜底文案：`FALLBACK_PROFILE = "加载资料失败"`、`FALLBACK_LIST = "加载小视频失败"`、
`FALLBACK_CREATE = "发布失败"`、`FALLBACK_DELETE = "删除失败"`、`FALLBACK_TOGGLE = "操作失败"`（`:216-222`）。

| 动词 + path | Kotlin 签名 | 请求 | 响应字段 | 错误 / 特殊码 | 调用方 |
|---|---|---|---|---|---|
| `GET /api/v1/short-videos/profile` | `HttpShortVideoRepository.fetchProfile(userId: String? = null): ShortVideoProfileModel`（`:129-134`） | `queryOf("user_id" to userId)`：null/空省略 | `ShortVideoProfileModel{user_id?, display_name = "", avatar_url?, role_badge = "", store_name = "", is_me, stats}`；`is_me` 只认 JSON 字面量 `true`（`ShortVideoModels.kt:202`）；`stats` 数字 → **字符串**，缺省 `"0"`，非 object → `EMPTY`（`:203-212`） | `requireData` ⇒ 成功但缺 `data` 也算失败，兜底「加载资料失败」 | `ShortVideoHttpRepositoryTest.kt:113` |
| `GET /api/v1/short-videos` | `fetchList(scope: String, userId: String? = null, page: Int = 0, pageSize: Int = 5): ShortVideoListPage`（`:137-158`） | `scope` **无条件带键**；`page = 本地 0-based +1`；`size`；`user_id` 空则省略（`:143-147`） | `ShortVideoListPage{list, hasMore}`；行 `ShortVideoItemModel`（`aspectRatio` 缺省 **1.25**、`like_count` 缺省 0、`status` 缺省 `normal`、`is_liked`/`is_mine` 只认字面量 true，`ShortVideoModels.kt:93-110,151-173`） | `hasMore = data.hasMore(pageSize)`（等价 Dart `pagination?.hasMore ?? (list.length >= pageSize)`，`:155-156`）；兜底「加载小视频失败」 | 同上 |
| `POST /api/v1/short-videos` | `create(title: String, topicId: String? = null): ShortVideoItemModel`（`:161-168`） | `ShortVideoCreateRequest{title, video_url = null, cover_url = null, duration = null, aspect_ratio = null, topic_id = null}` 六键恒发 ⇒ **`NetworkJsonExplicitNulls`**（`:103-111,162-165`） | `ShortVideoItemModel` | 兜底「发布失败」 | 同上 |
| `DELETE /api/v1/short-videos/:id` | `delete(id: String)`（`:171-176`） | 无 body（`deleteApi(path, emptyMap()) { }`） | 丢弃 | **只看 `isSuccess`**（`_dataAllowEmpty` 语义），兜底「删除失败」 | 同上 |
| `POST`/`DELETE` `/api/v1/short-videos/:id/like` | `toggleLike(id: String, liked: Boolean): ShortVideoItemModel`（`:179-187`） | `liked == true` → POST，否则 DELETE，**同 path** | 片段 `{id, like_count, is_liked}` → `fromLikeFragment`（`ShortVideoModels.kt:133-141,180-187`），其余字段保持默认；消费方 `withCounts` 就地合并（`:115-125`） | 兜底「操作失败」 | 同上 |
| `POST /api/v1/short-videos/:id/view` | `reportView(id: String): Int?`（`:190-196`） | 无 body `{}`（`postApi(path, "")`） | `{view_count: Int}`，缺失 → `0` | **从不抛错**：整个调用 `runCatching{}`（等价 Dart 播放页 `catch (_) {}`），失败 → `null`（KDoc `:66-75`）；at-least-once（契约 §5.3） | 同上 |

### 3.14 收货地址（#79–#83，`feature/mine/…/address/AddressApi.kt`）

`BASE = "/api/v1/user/addresses"`（`:23`）、`detail(addressId) = "$BASE/$addressId"`（`:26`）、
`setDefault(addressId) = "$BASE/$addressId/default"`（`:29`）。

| 动词 + path | Kotlin 签名 | 请求 | 响应字段 | 错误 / 特殊码 | 调用方 |
|---|---|---|---|---|---|
| `GET /api/v1/user/addresses` | `list(): List<AddressModel>`（`:52-58`） | 无 | **只读 `data.items`**（`decodeItemsOnly` `:94-98`）；`AddressModel` 11 个可空 raw 键（`AddressModels.kt:15-27`），UI 侧用 `resolved*` 取值 | 兜底「加载地址失败」（`:57`） | `AddressApiTest.kt:23` |
| `POST /api/v1/user/addresses` | `create(body: AddressBridgeRequest): AddressModel`（`:61-63`） | `AddressBridgeRequest` **10 键全非空默认、恒发**：`address_id = 0, receiver_name = "", receiver_phone = "", province, city, district, detail_address, full_address, is_default = false, label = ""`（`AddressModels.kt:67-78`）；**不含 `postal_code`**（`toBridgeJson()` `:48-59`） | `AddressModel` | 兜底「保存失败」 | 同上 |
| `PATCH /api/v1/user/addresses/:addressId` | `update(addressId: Int, body: AddressBridgeRequest): AddressModel`（`:69-71`） | 同 map；**更新是 PATCH 不是 PUT** | `AddressModel` | 兜底「保存失败」 | 同上 |
| `POST /api/v1/user/addresses/:addressId/default` | `setDefault(addressId: Int): AddressModel`（`:77-79`） | **无 body 的动作 POST** | `AddressModel` | 兜底「设置默认失败」 | 同上 |
| `DELETE /api/v1/user/addresses/:addressId` | `delete(addressId: Int)`（`:86-91`） | 无 body | 原始 Map，但**要求 `data` 非空 map**（Dart `_data` 语义，`:86-91`） | 兜底「删除失败」 | 同上 |

### 3.15 钱包（#84–#89，`feature/wallet/…/WalletApi.kt`）

| 动词 + path | Kotlin 签名 | 请求 | 响应字段 | 错误 / 特殊码 | 调用方 |
|---|---|---|---|---|---|
| `GET /api/v1/wallet` | `fetchSummary(): WalletSummary`（`:31-34`） | 无 | `WalletSummary{balance = "0.00", balance_fen, cards:[BankCard{card_id, bank_name, card_last4, holder_name, is_default}]}`；`BankCard.display = "$bankName(****$cardLast4)"`（`WalletModels.kt`） | 兜底「加载钱包失败」（`:33`） | `WalletApiContractTest.kt:29` |
| `GET /api/v1/wallet`（只读余额，非阻塞） | `fetchBalance(): String?`（`:43-46`） | 无 | 只取 `data.balance` | **`runCatching` 可空、永不抛**（对齐 `mall_order_detail_controller.dart:75` 的吞错读取，契约 §2.6 附注） | `WalletApiContractTest.kt:52, 56` |
| `GET /api/v1/wallet/ledger` | `fetchLedger(limit: Int = LEDGER_LIMIT, offset: Int = 0): List<WalletLedgerEntry>`（`:63-73`，`LEDGER_LIMIT = 20` `:127`） | `queryOf("limit" to limit, "offset" to offset)` | `{items:[WalletLedgerEntry{ledger_id, delta_fen, balance_fen, reason, ref_id}]}`——**无 `list`、无 pagination**；`deltaYuan` 手工分→带符号元串（`WalletModels.kt:54-60`），`reasonLabel` 三态（`:62-69`） | 手搓转换器：`code != 0` 但 `data` 有值时**放行**（`NetworkCodes.PASSTHROUGH` 分支，`:63-73`）；兜底「加载流水失败」 | 同上 |
| `POST /api/v1/wallet/cards` | `bindCard(bankName: String, cardLast4: String, holderName: String = "", isDefault: Boolean = false): BankCard`（`:76-80`） | `WalletBindCardRequest{bank_name, card_last4, holder_name = ""（恒发）, is_default = false（恒发）}`（`WalletModels.kt:85-90`） | `BankCard` | 兜底「绑卡失败」（`:91`） | 同上 |
| `POST /api/v1/wallet/cards/:cardId/default` | `setDefaultCard(cardId: Int)`（`:98-104`） | **无 body**（`body = ""`） | 原始 Map，仍要求 `data` 非空 | 兜底「设置默认卡失败」 | 同上 |
| `DELETE /api/v1/wallet/cards/:cardId` | `deleteCard(cardId: Int)`（`:107-110`） | 无 body | 原始 Map | 兜底「删除银行卡失败」 | 同上 |
| `POST /api/v1/wallet/recharge` | `recharge(amount: String, channel: Int, cardId: Int? = null): WalletRechargeResult`（`:117-123`） | `WalletRechargeRequest{amount: **十进制「元」字符串**（不是分）, channel: Int, card_id?（null 省略）}`（`WalletModels.kt:100-104`） | `WalletRechargeResult{balance, balance_fen, delta_fen}` | 兜底「充值失败」（`:122`） | 同上 |

`WalletApiPaths`（`:132-141`）：`BASE`/`LEDGER`/`CARDS`/`RECHARGE` + `card(cardId)` + `cardDefault(cardId)`。
### 3.16 会员 / 支付（#90–#95，`feature/pay/…/PayApi.kt`）

`PayApiPaths`（`:240-246`）：`ME`/`BUYOUT`/`BUYOUT_CONFIRM`/`HUAWEI_VERIFY`/`APPLE_VERIFY` 五个常量。
所有端点走私有 `getBffMap`/`postBffMap` + `toBffMapOrNull`：**`data` 为 null → 抛路径信息；非 object → 当作 `{}`**
（`:252-256`、`:201-215`），对应契约 §2.16「响应只按裸 Map 取键」。

| 动词 + path | Kotlin 签名 | 请求 | 响应字段 | 错误 / 特殊码 | 调用方 |
|---|---|---|---|---|---|
| `GET /api/v1/membership/me` | `fetchMe(): MembershipMeResponse`（`:41-48`） | 无 | `{catalog:[MembershipPlan{plan_id, tier:'svip'\\|'ai_svip', title, price, price_fen, months, huawei_product_id, apple_product_id, auto_renew_eligible}], entitlements:[{tier, cta}]}`；`catalogPlans` **过滤空 id**（`MembershipModels.kt:105`）；`MembershipTier.fromWire` 未知 → `Svip`（`:29-30`） | `data` 非 map → `{}`（等价 Dart map 转换器）；null → 抛 | `MembershipPayContractTest.kt:29, 65, 68` |
| `POST /api/v1/membership/buyout` | `buyout(planId: String, channel: String): MembershipBuyoutResponse`（`:50-61`） | `MembershipBuyoutRequest{plan_id: String, channel}`（`MembershipModels.kt:155-158`），`channel`: `wechat`/`alipay`/`balance` | `MembershipBuyoutResponse{status, order_id: Int, prepay_params}`；`isPaid = status == "paid"`，`weChatPrepayId` 兼容 `prepayid`/`prepayId` 两键（`:123-143`） | 兜底文案见 `postBffMap`；`prepay_params` 原样交 SDK | 同上 |
| `POST /api/v1/membership/buyout/confirm` | `confirmBuyout(orderId: Int)`（`:63-70`） | `MembershipConfirmRequest{order_id: Int}`（`:162-164`） | 丢弃 | **`order_id <= 0` 静默跳过 confirm**（见编排行） | 同上 |
| `POST /api/v1/membership/huawei/verify` | `verifyHuawei(productId: String, purchaseToken: String, purchaseOrderId: String = "", subscriptionId: String = "", jwsPurchaseOrder: String = ""): MembershipHuaweiVerifyResponse`（`:79-85`） | 5 键恒发，后三键默认 `''`（`MembershipModels.kt:183-189`） | `{finish_purchase: Boolean = false}`（`:166-170`）；`true` ⇒ 客户端需 `HuaweiIapBridge.finishPurchase` | `// GAP(R5)`：**结单桥未接**，本层只回传标志位（`PayApi.kt:86`）；`restoreHuaweiPurchases()` 会循环调用本端点（契约 #93） | `MembershipPayContractTest.kt` |
| `POST /api/v1/membership/apple/verify` | `verifyApple(productId: String, transactionId: String, originalTransactionId: String = "", receiptData: String = ""): MembershipAppleVerifyResponse`（`:112-117`） | 4 键恒发，后两键默认 `''`（`MembershipModels.kt:196-201`） | `{complete_purchase: Boolean}`（`:172-176`） | 同上（`AppleIapBridge.completePurchase` 未接） | 同上 |
| （编排，非独立契约行） | `payBuyout(method: PaymentMethodType, planId: String, sdkPay: suspend (MembershipBuyoutResponse) -> Boolean): MembershipPayOutcome`（`:153-192`） | 内部 `buyout` → `sdkPay` → `confirmBuyout` | `MembershipPayOutcome(status, orderId?, message?)`；`MembershipPayStatus`: `Paid/Confirming/LaunchFailed/SdkError/…` | **四步反直觉语义全保留**（契约 §5.1）：① 非 buyout 通道（huawei/apple）直接 `LaunchFailed("请使用对应内购通道")`；② `isPaid` 短路，不调 SDK 也不 confirm；③ **SDK 成功后才 confirm**；④ `orderId <= 0` **静默跳过** confirm；异常一律折成 `SdkError`，不外抛 | 同上 |
| `POST /api/v1/payments/prepay`（#95） | **未实现（有意缺席）** | — | — | `PayApi.kt:33-36` KDoc 明确：Dart 侧该常量是死码（`membership_payment_gateway.dart:17` 无调用点），真实 prepay 走 #91 的 `prepay_params`；**KMP 不为其编造端点** | 无 |

### 3.17 AI SSE（#96，`feature/ai/…/AiSseApi.kt`）

| 动词 + path | Kotlin 签名 | 请求 | 响应 | 错误 / 特殊码 | 调用方 |
|---|---|---|---|---|---|
| `POST /api/v1/sse/completions` | `AiSseApi.streamCompletion(prompt: String, conversationId: String? = null, clientRequestId: String? = null): Flow<AiStreamEvent>`（`:51-67`，**冷流**；`COMPLETIONS_PATH` 在 `:32`） | `completionsRequestBody`：**camelCase** 三键 `{prompt, conversationId?, clientRequestId?}`，null 或空串**不发键**（`:74-85`）——与全仓 snake_case 不同（契约 §2.17 #96） | **SSE 帧，非信封**：`event: meta{requestId, conversationId, model, clientRequestId}`、`delta{text}`、`done{finishReason}`、`error{code, message}` → `AiStreamEvent` | 构造注入 `SseTransport`（`:27-29`）；**流前**失败抛 `SseException`；**流内**失败只可能是 `event: error` 帧 ⇒ 发 `AiStreamEvent.Error` 而不抛（不得中途改 JSON 信封）；无法识别/空 delta 帧直接丢弃；EOF 仍 `flush()`（`:56-66`） | `AiSseApiTest.kt:71, 101, 123, 136, 147`；`AiStreamScreen.kt` **未接** |

### 3.18 realtime（#97–#98，`:core:network` 唯二的业务端点）

| 动词 + path | Kotlin 签名 | 请求 | 响应 | 错误 / 特殊码 | 调用方 |
|---|---|---|---|---|---|
| `POST /api/v1/realtime/ws-ticket` | `RealtimeWsTicketApi.fetchTicket(accessToken: String): RealtimeWsTicketData`（`RealtimeWsTicketApi.kt:31`） | `RealtimeWsTicketRequest{platform = RealtimeConfig.PLATFORM_MOBILE /* "mobile" */}`（`:52-54`、`RealtimeConfig.kt:25`），经 `client.postRaw`（**绕过信封解析**，`:36-39`） | **裸 body camelCase、四键全必填**：`RealtimeWsTicketData{ticket, wsUrl, expiresInSeconds, connId}`（`:65-74`） | `accessToken` 为空 → `IllegalStateException("缺少 access token，请先登录")`（`:34`，等价 Dart `StateError`）；解析失败 → `RealtimeWsError.BodyUnparsable`；`resolveRealtimeWsUrl = remapBackendLocalhost(wsUrl)`（`:81`）——**除本地回环重映射外，wsUrl 原样拨号，禁止本地拼**（契约 §5.4） | `component/…/component/realtime/AppRealtimeClientImpl.kt:51`（构造参数，**无默认值 ⇒ 生产装配点缺失**）；测试 `AppRealtimeClientImplTest.kt:75`、`core/network/src/commonTest/…/realtime/RealtimeWsApiContractTest.kt:100-189` |
| `POST /api/v1/realtime/sync` | `RealtimeWsSyncApi.sync(sinceSeq: Long, topics: List<String>): RealtimeSyncData`（`RealtimeWsSyncApi.kt:26-38`） | `RealtimeSyncRequest{sinceSeq, topics}`（**camelCase**，`:41-45`） | **裸 body**：`RealtimeSyncData{events:[RealtimeEnvelope], latestSeq}`（`:52-56`），`fromJson(JsonObject)` 手工解码（`:62-69`） | 解析失败 → `RealtimeWsError.BodyUnparsable`（`:33-37`）；`RealtimeTextSerializer`/`RealtimeLongSerializer` 容忍字符串数字；调用侧**失败只记 telemetry 不阻断连接**（KDoc `:19-22`）；WS 关闭码 4001/4002/4003 ⇒ 重新换票（`RealtimeConfig.kt:49-63`，`requiresNewTicket`） | `AppRealtimeClientImpl.kt:52`；测试 `AppRealtimeClientImplTest.kt:76` |
| `POST /api/v1/realtime/push`（契约 §2.17 **无编号行**） | **未实现** | `{title, body, topic?, name?}` | `{envelope, delivered}` | 契约注明 **Dart 无调用点**（仅 `my_ai_project/AGENTS.md:434`（Dart 仓库）、`my_ai_project/docs/BACKEND_INTEGRATION.md:405`（Dart 仓库） 文档提及），dev 推流测试用；M1 规则「任何 KMP 新增调用必须能在契约表找到对应行」⇒ **不得编造** | 无（全仓 grep `realtime/push` 只命中契约表与 gap 注册文档） |

### 3.19 push（#99，`component/…/PushRegistrationApi.kt`）

| 动词 + path | Kotlin 签名 | 请求 | 响应 | 错误 / 特殊码 | 调用方 |
|---|---|---|---|---|---|
| `POST /api/v1/push/devices` | `PushRegistrationApi.report(registrationId: String, alias: String? = null, deviceId: String? = null, platform: String? = null, mock: Boolean = false)`（`:35-41`；`PATH` 常量 `:76`） | `buildJsonObject{registration_id, alias?, device_id?, platform, mock}`：`alias`/`device_id` 为空则**省略键**（`:59-65`）；`platform` 由 `detectPlatform()` 把 `ohos → "harmony"`（`:96-101`）；经 `postRaw`（**完全不解析信封**） | 原始 map 或 `{}`，结果丢弃 | `mock == true` 或 `registrationId.isEmpty()` → **提前 return，不发请求**（`:52`）；整个网络段 `try/catch` 吞掉并 `log("[PushRegistrationApi] report failed: $t")`，**不重抛**（`:54-71`）；`code` 不校验；`analytics.trackPushRegister` 先于早退发生（`:46-50`、`PushRegistrationAnalytics`/`NoopPushRegistrationAnalytics` `:109-116`） | **无任何调用方**：`PushBridge.kt:46-50`（`StubPushBridge`）只取 `PlatformPushSdk.fetchRegistrationToken()`，未上报 BFF；`// GAP(R5)` JPush/HPush 注册 ID 链路仍是占位（`PushBridge.kt:99-104`、`PushRegistrationApi.kt:26`） |

### 3.20 chat / IM（#100–#102、#109–#114，`feature/chat/…`）

路径集中在 `RongImConfig`（`:24-36`）：`SESSION_PATH = "/api/v1/im/session"`、
`USERS_PROFILE_PATH = "/api/v1/im/users/profile"`、`MESSAGES_BACKUP_PATH = "/api/v1/im/messages/backup"`、
`GROUPS_FREE_PATH = "/api/v1/im/groups/free"`、`GROUPS_STORE_SYNC_PATH = "/api/v1/im/groups/store/sync"`；
动作路径 `imFreeGroupActionPath(groupId, action) = "$GROUPS_FREE_PATH/$groupId/$action"`（`:77-78`）。
mock 开关三级：`forceMockIm`、`rongAppKey` 空/占位默认 mock、`useMockImFor(...)`（`:48-67`）。

| 动词 + path | Kotlin 签名 | 请求字段与必填 | 响应字段 | 错误 / 特殊码 | 调用方 |
|---|---|---|---|---|---|
| `POST /api/v1/im/session` | `ImSessionApi.createSession(bizUserId: String, displayName: String? = null): ImSessionResult`（`:151-154`） | `ImSessionRequest{display_name?}`：trim 后空则省略；**`bizUserId` 不进 body**（真实分支忽略，仅 mock 用来换本地 id，`:165-170`、KDoc `:149-150`） | `ImSessionResult{imUserId, token, expiresInSeconds}`：`user_id` 优先、回落 `im_user_id`；`expires_in_seconds` 缺失 → `0`（`:29-41`） | `token.isEmpty()` 即抛 `NetworkError.Business(code, "获取 IM Token 失败")`（`:175-180`）；**mock 分支**：`sleep(150ms)` + `mock_rong_token_<nowMs>` + 3600（`:155-163`），身份经 `ImMockUserIdRegistry` 生成 `im_u_<uuid16>` 并持久化 key `im_biz_to_im_user_id_json`（`:63-121`） | `ImChatApiContractTest.kt:38, 56, 69, 78, 88`。`// GAP(R5)`：融云 SDK（`rongcloud_im_wrapper_plugin: ^5.44.0`）无产物，connect/收发仍由 `component/…/component/chat/ImEngine.kt:146` 的 `MockImEngine` 承担 |
| `GET /api/v1/im/users/profile` | `ImUserProfileApi.fetchProfiles(imUserIds: List<String>): Map<String, ImUserProfile>`（`:45`） | `query = mapOf("user_ids" to ids.joinToString(","))`——**逗号拼接，不是数组也不是重复键**；ids 先 trim 去空，**空列表直接不发 HTTP**（`:46-48, 61-66`） | `{items:[{user_id\\|im_user_id, display_name, avatar_url}]}` → Map，**空 id 行跳过**（`:74-85`） | mock 分支 80ms + picsum 头像 + `用户<id 前 4 位>`（`:50-59, 89-95`） | `ImChatApiContractTest.kt:153, 171, 190, 194` |
| `POST /api/v1/im/messages/backup` | `HttpImBackupService.backupOutbound(imUserId, conversationId, messageUid, type, payload: JsonObject, sentAtEpochMs)`（`:155-161`）、`backupInbound(…)`（`:173-179`）、`backupRecall(imUserId, conversationId, messageUid, recalledAtEpochMs)`（`:191-196`）、`flushPending()`（`:220`）；`imUserId` **不进请求体**（`:25` KDoc） | `{events:[ImMessageBackupEvent{message_uid, direction:'in'\\|'out'\\|'recall', conversation_type:'private', target_id, message_type, payload, sent_at: UTC ISO-8601}]}` 7 键 `linkedMapOf`（`:42-52`）；`sent_at` 由手写 `ImIso8601Utc.format/parseToEpochMillis`（Hinnant 算法，`:396-513`） | 忽略 | **攒够 ≥5 才 flush**（`FLUSH_THRESHOLD = 5`，`:250`）；flush 整批出队，失败/异常 `pending.addAll(0, batch)` **回头重排 ⇒ at-least-once**，服务端按 `message_uid` 去重（`:220-244`）；mock 选型 `createImBackupService(client, useMock, sleep)`（`:352-356`），mock 的 `backupRecall` **不入队**（`:307-314`），30ms/条 | `ImChatApiContractTest.kt:215, 236, 249, 273, 314, 335, 341, 355-358` |
| `POST /api/v1/im/groups/store/sync` | `ImGroupApi.syncStoreGroup(storeId: String): ImStoreGroupResult`（`:89-99`） | `ImStoreGroupSyncRequest{store_id: **String**}`（`:60-62`） | `ImStoreGroupResult{store_id: String, group_id: String}`，手工 `fromJson` | `postApiJson`（`:184-200`）失败兜底「门店群同步失败」（`:96`）；服务端幂等 provisioning | `ImChatApiContractTest.kt:407, 430` |
| `POST /api/v1/im/groups/free` | `createFreeGroup(name: String, memberIds: List<String> = emptyList()): ImFreeGroupResult`（`:101-112`） | `ImFreeGroupCreateRequest{name, member_ids = []}`（`:47-50`） | `ImFreeGroupResult{group_id, name, owner_user_id}` | 兜底「建群失败」（`:110`） | 同上 |
| `POST …/free/:groupId/invite` 与 `…/kick` | `invite(groupId: String, memberIds: List<String>)`（`:118-120`）、`kick(groupId: String, memberIds: List<String>)`（`:128-130`） | `ImGroupMembersRequest{member_ids}`：**空列表也发**（`:54-56`） | 原始 Map，丢弃 | `postOk(path, body)`（`:149-158`）**只看 `code`**，兜底「群操作失败」 | 同上（`invite` 断言在 `:452`、空表 `:464`） |
| `POST …/free/:groupId/quit` 与 `…/dismiss` | `quit(groupId: String)`（`:138-140`）、`dismiss(groupId: String)`（`:145-147`） | **显式 `{}`**（`EMPTY_OBJECT_BODY = "{}"`，`:138-146`）——不是 null body、不是空串（契约 §5.12） | 原始 Map | 同上 `postOk` | 同上 |

### 3.21 好友（#103–#108，`feature/friend/…/ImFriendApi.kt`）

`ImFriendApiPaths`（`:23-38`）：`FRIENDS_PATH = "/api/v1/im/friends"`、`FRIEND_REQUESTS_PATH = "/api/v1/im/friends/requests"`、
`USERS_SEARCH_PATH = "/api/v1/im/users/search"`、`PRIVATE_ADMISSION_PATH = "/api/v1/im/private/admission"`、
`respondPath(requestId) = "$FRIEND_REQUESTS_PATH/$requestId/respond"`（`:37`）。
**这些常量与 `feature/chat/RongImConfig` 的取值必须一致**（`:19-21` KDoc 说明因分层规则各留一份）。

| 动词 + path | Kotlin 签名 | 请求 | 响应字段 | 错误 / 特殊码 | 调用方 |
|---|---|---|---|---|---|
| `GET /api/v1/im/users/search` | `search(query: String): List<ImFriendUser>`（`:122-129`） | `q` ——**空串也发**（`:124-127`） | `{items:[ImFriendUser{user_id, display_name, avatar_url, phone_masked}]}`（`getItemsApi`，`:259-275`） | 兜底经 `friendlyError`（`:224-247`） | `ImFriendApiContractTest.kt:71` |
| `GET /api/v1/im/friends` | `listFriends(): List<ImFriendUser>`（`:132-138`） | 无 | 同上 | 兜底「加载好友失败」 | `ImFriendApiContractTest.kt:37, 82` |
| `GET /api/v1/im/friends/requests` | `listIncomingRequests(): List<ImFriendRequest>`（`:144-150`） | 无（仅入向） | `{items:[ImFriendRequest{id: **String**, from_user_id, display_name, avatar_url, phone_masked, status = "pending", created_at}]}`（`:66-73`） | 兜底「加载申请失败」 | `ImFriendApiContractTest.kt:56, 65` |
| `POST /api/v1/im/friends/requests` | `requestFriend(toUserId: String)`（`:163-177`） | `ImFriendRequestCreateRequest{to_user_id: String}`（`:90-92`） | 忽略 | **原样抛服务端英文 message**，兜底「申请失败」（`:171-177`）；`friendlyError` 把 5 条英文（`already pending`/`already friends`/…）映射成中文（`:224-247`） | 同上 |
| `POST /api/v1/im/friends/requests/:requestId/respond` | `respondFriend(requestId: String, accept: Boolean)`（`:180-190`） | `ImFriendRespondRequest{accept: Boolean}`（`:99-101`）——**同意/拒绝共用一端点，只切取值** | 原始 Map | 兜底「操作失败」 | 同上 |
| `GET /api/v1/im/private/admission` | `canPrivateChat(peerUserId: String): Boolean`（`:203-213`） | `queryOf("peer_user_id" to peerUserId)` | `{allowed: Boolean}` | **fail-closed**：任何异常/非 `true` 一律返回 `false`，不抛（`:203-213`） | 同上 |

私有 JSON 读取副本 `imFriendStr` / `imFriendIsTrue`（`:277-293`）——`isTrue` **只认 JSON 字面量 true**；
它们与 `feature/chat/ImJsonRead.kt:27-60` 同构，重复是分层门禁（feature→feature 禁 import）的结果，不是疏忽。
---

## 4. DTO 与模型

### 4.1 跨域复用与「一个端点两个模型」

| 情况 | 事实 | 证据 |
|---|---|---|
| **合并**（契约 §6 要求） | #26 积分礼物与 #27 商城货架解的是**同一个** `GET /mall/stores/:storeId/products` 行 ⇒ KMP 只用一份 `MallProductCardModel`，不再另立 `PointsGiftItem` | `MallApi.kt:165-182` KDoc、`pointsGiftProjection`（`:223-224`）；`PointsApi.kt:12-14` 声明 #26 已移出积分域 |
| **同端点双模型（未合并，属刻意）** | `GET /mall/stores/:id/products`：货架用 `MallProductCardModel`，积分投影同一 DTO 但只消费 `pricePoints`；`GET /transactions` 双方言则**两份实现**（home `TransactionApi` + mine `MineHttpTestApi`），因为跨 feature import 被门禁禁止 | `HomeTransactionApi.kt:30-63` vs `MineHttpTestApi.kt:37-46`；契约 §7.1 |
| **一模型两调用点** | `MallOrderItem` 同时是订单列表行与订单详情行（8 键：`item_id, order_id, product_title, cover_url, qty, line_amount, kind, content_url`，`kind == 1` 虚拟商品） | `MallOrderModels.kt`（`MallOrderListRow`/`MallOrderDetailResponse` 都持有 `items`） |
| **`BankCard` 复用** | #84 的 `data.cards[]` 与 #86 的响应共用 `BankCard` | `WalletModels.kt`、`WalletApi.kt:76-92` |
| **`TransactionModel` 复用** | #12 列表与 #13 详情共用；`typealias TransactionListResult = ListPage<TransactionModel>`（`:69`）、`typealias TransactionDetailResult = TransactionModel`（`:71-72`） | `HomeTransactionModels.kt` |
| **故意重复（不 import）** | `ShortVideoTopicBrief{id,name}` 是 community `TopicModel` 的重复；`ImFriendApiPaths` 与 `RongImConfig` 路径常量重复；`imFriendStr/imFriendIsTrue` 与 `ImJsonRead` 重复 | `ShortVideoModels.kt:16-19`、`ImFriendApi.kt:19-21,277-293`、`ImJsonRead.kt:24-27` |
| **同名不同类** | `MembershipTier`/`MembershipPlan` 有两份：`feature/commerce/MembershipCatalog.kt:5-18` 是 **UI 假目录**（硬编码 `svip_12m` 等 6 档 + 价格），`feature/pay/MembershipModels.kt` 才是 #90 的 wire DTO。两者无转换关系 | 两处文件 |

### 4.2 nullable 与默认值策略

线上字段一律**可空 raw + `resolved*`/派生属性给 Dart 默认值**。根因：`NetworkJson` 没有开
`coerceInputValues`，null 落到非空字段会直接抛，所以「缺省语义」必须由 Kotlin 侧属性表达。

| 策略 | 代表 | 证据 |
|---|---|---|
| 可空 raw + `resolved*` | `AuthUserDto`（10 个可空键 + 7 个 `resolved*`）、`UserProfileData`、`AddressModel`（11 可空键）、`UsedCarOrderItem`、`DealInvoiceItem` | `AuthApiContracts.kt:102-141`、`UserProfileContracts.kt:41-84`、`AddressModels.kt:15-27` |
| 直接给非空默认值 | `ShortVideoItemModel.aspectRatio = 1.25`、`ShortVideoProfileModel.isMe = true`、`MallProductCardModel.price = "0.00"`、`PointsStatus.todayReward = 5` | `ShortVideoModels.kt:76-110`、`PointsModels.kt`、`MallProductModels.kt` |
| **唯一「缺失即抛」的业务字段** | `TransactionModel.id: Int`（非空、无默认）——契约 §2.3 #12 明写「`id` 缺失会抛」，KMP 原样 | `HomeTransactionModels.kt:26` |
| 未知枚举值降级 | `ShortVideoStatus.fromWire` → `Normal`；`DealInvoiceStatus.fromApi` → `PendingReview`；`TaskProgress.parse` → `Incomplete`；`MembershipTier.fromWire` → `Svip`；`UsedCarKindFilter.apiKind`（`all` → `null` 即省略键） | `ShortVideoModels.kt:33-37`、`DealInvoiceModels.kt:39-44`、`PointsModels.kt:107-112`、`MembershipModels.kt:29-30`、`UsedCarOrderModels.kt:67-93` |
| 数字 → 字符串化 | `ShortVideoStatsModel` 三键全 String（`(v as num?)?.toInt() ?? 0).toString()`）；`NewCarFollowFile.file_id/customer_id`、`UsedCarOrderItem.order_id`、`DealInvoiceItem.invoice_id` 均 String | `ShortVideoModels.kt:58-67,203-212`、`NewCarFollowModels.kt:92-140`、`UsedCarOrderModels.kt:107`、`DealInvoiceModels.kt:104-116` |
| 严格布尔（只认 JSON 字面量 `true`） | `homeWireLiteralTrue`（`HomeApiWire.kt:176-177`，用于 #47 `can_create`）、`imFriendIsTrue`（`ImFriendApi.kt:277-293`）、`ShortVideoModels.isTrue`（`:223-224`，用于 `is_liked/is_mine/is_me`）、`UserStoreListItemData.isCurrent`（双键都要 `== true`） | 各处已注 |
| 双键大小写并收 | `UserProfileData`（`user_name`/`userName` 等三组）、`UserStoreStatsData`/`UserStoreListItemData`（几乎每键两组）、`AuthUserDto`（`avatar_url`/`avatarUrl`） | `UserProfileContracts.kt:41-84,95-205` |
| 逗号拼接的非数组查询 | `user_ids`（#101）、`status`（#31） | `ImUserProfileApi.kt:61-66`、`MallApi.kt:208-217` |

### 4.3 序列化注解注意点

1. `@SerialName` 必带：wire 是 snake_case 而 Kotlin 名是 camelCase；漏写就静默丢字段。
   例外是刻意 camelCase 的三个 DTO：`RealtimeWsTicketRequest/RealtimeSyncRequest`（`RealtimeWsTicketApi.kt:52-54`、
   `RealtimeWsSyncApi.kt:42-45`）与 `AiSseApi` body（`AiSseApi.kt:74-85`）——契约 §2.17 明确它们不走 snake_case。
2. `@Serializable(with = FlexibleIntSerializer::class)` 用于「服务端可能给字符串数字」的 Int 字段，
   出现在 7 个文件：`ApiEnvelope.kt`、`AuthApiContracts.kt`、`UserProfileContracts.kt`、
   `HomeTodoApiModels.kt`、`PointsModels.kt`、`NewCarFollowModels.kt`、`AfterSalesModels.kt`。
   media 域用**本地等价物** `flexibleInt/flexibleDouble`（`ShortVideoModels.kt:226-236`），未跨模块复用。
3. `encodeDefaults` 与 `explicitNulls = false` 的组合决定了「默认值是否出现在请求体里」：
   `MallCreateOrderRequest` 的 receiver 三件套、`WalletBindCardRequest` 的 `holder_name`/`is_default`
   依赖 `encodeDefaults` 才能恒发（`MallRequests.kt:36-44`、`WalletModels.kt:85-90`）。
4. 需要显式 null 的四个写端点**必须**换 `NetworkJsonExplicitNulls`：#39、#55、#56、#60、#74
   （`UsedCarOrderApi.kt:114-118`、`DealInvoiceApi.kt:104-110,116-125`、
   `HttpCommunityPostRepository.kt:149-169`、`ShortVideoRepository.kt:162-165`）。
   同一份「显式 null」语义若用手工 `buildJsonObject` 则**不需要**换编码器（§1.4）。
5. 手写 `JsonObject` 解码的三类落点：`ImGroupApi`（`postApiJson` + `fromJson`，`:184-200`）、
   `ImMessageBackupApi`（`linkedMapOf` 7 键，`:42-52`）、`RealtimeWsSyncApi.fromJson`（`:62-69`）。
   它们绕开 `@Serializable`，字段名拼错不会有编译期提示，属本层最大回归风险。
6. 时间一律手工解析：`homeIsoDateTimeOrNull`（正则，`HomeTimeWire.kt:18-27`）、
   `parseMallTime`（`Instant.parseOrNull` 两试，`MallOrderModels.kt:143-147`）、
   `parseCommunityTimeMs`（**无时区串按 UTC**，与 Dart 按本地时区解释不同，已记录偏差，
   `CommunityPostModels.kt:44-46`）、`ImIso8601Utc`（手写 Hinnant civil-from-days，`:396-513`）。
7. `PayApi` 的 `PaymentMethodType` wire 值映射：`wechat/alipay/balance/''`，huawei、apple 的 wire 串是**空串**
   （`MembershipModels.kt:41-51`）——#91 的 `channel` 只对三种外置渠道有意义。

---

## 5. 契约对账（逐域三态）

| 域 | 契约行 | 契约里有 | 代码里有 | 是否一致 | 偏差 / 缺失（列原文条目与编号） |
|---|---|---|---|---|---|
| auth 会话 | #1–#7 | 7 | 7 | **一致** | ① 契约 §0/§2.1 #4：logout「手工带 `Authorization`/`X-Session-ID`/`X-Device-ID` + `skipAuthRefresh`」——KMP 只有全局 `Authorization`，**逐请求头出口缺失**（`UserAuthHttpApi.kt:32-35`，`// GAP(R5)`）；② `skipAuthRefresh` 靠 `refresh(raw:)` 传裸 client 等价实现（`:142`）。会话死时不调 logout 的守卫在 `AuthRepository.kt:215-220` |
| 契约外扩展 | — | 0 | 1 | **超出契约** | `POST /api/v1/user/huawei/login`（`AuthApiPaths.HUAWEI_LOGIN`，`AuthApiContracts.kt:31`；调用点 `AuthRepository.kt:154`）。契约 §2.1 无此行，属本仓既有扩展（`AuthRepository.kt:140-146` KDoc 自述） |
| 账号资料/门店 | #8–#11 | 4 | 4 | **一致** | 无字段偏差；`/me/stores` 前缀分裂（`ProfileApiPaths.MY_STORES`，`UserProfileContracts.kt:22`）与 `current_store_id` 覆盖语义（`:227`）都保留。**但整域零生产调用方**（§6.1） |
| transactions | #12–#14 | 3 | 3 | **一致** | 双方言按要求分开实现；#14 **不看业务码**（`MineHttpTestApi.kt:37-42`）与契约「缺 `code` 判失败」不同，属该调试端点的 Dart 事实延续，需读者知悉 |
| home 待办 | #15–#21 | 7 | 7 | **一致** | 无 |
| 积分 | #22–#26 | 5 | 5 | **一致**（#26 落点变更） | #26 实现在 `MallApi.fetchPointsGifts`（`MallApi.kt:171-182`）而非 `PointsApi`；`PointsApi.kt:12-14` 有明确移交说明。`PointsStatus.calendar` 的**标量元素容错**与 Dart 略有出入（见 §7 第 3 条） |
| 商城 | #27–#34 | 8 | 8 | **一致** | `parseMallTime` 对无时区串返回 `null`（`MallOrderModels.kt:138-147`，代码内已自记「已知偏差」）；`soldLabel` 客户端派生而非服务端字段（契约 §2.6 #27）已保留 |
| 二手车订单 | #35–#39 | 5 | 5 | **一致** | 无 |
| 新车跟进 | #40–#46 | 7 | 7 | **一致** | 失败抛裸 `Exception`（契约 §2.9 #49 同款说明也适用 `new_car_follow`）——`homeBareDataOrThrow`（`HomeApiWire.kt:134-137`） |
| 售后 | #47–#50 | 4 | 4 | **一致** | `service_kind` 读 Int / 写 String 不对称保留（`AfterSalesModels.kt:53-57`）；`homeDatePart(null)` → `"null"` 字符串的 bug-for-bug 保留（`HomeTimeWire.kt:37`） |
| 成交发票 | #51–#56 | 6 | 6 | **一致** | 无 |
| 购车计算器 | #57–#58 | 2 | 2 | **端点一致，重试语义不一致** | 契约 §0/§4：该模块 `maxRetries: 3`（HTTP ≥500 重试）⇒ #57 GET 与 #58 POST 可能重复。**KMP 无 RetryInterceptor 对等物**，重试未实现（`PurchaseCalculatorApi.kt:26-37` KDoc 记录该风险）。这是本册唯一「端点齐、传输行为不等价」的域 |
| 社区 | #59–#71 | 13 | 13 | **一致** | `parseCommunityTimeMs` 无时区串按 UTC（`CommunityPostModels.kt:44-46`，已自记偏差）；#63/#64 合并为一个 `toggleLike`（与 Dart 同构，调用点数不变） |
| 短视频 | #72–#78 | 7 | 7 | **一致** | #78 `reportView` 返回 `Int?` 且从不抛，把 Dart「仓库抛 + UI 吞」合并成契约层单步语义（接口 KDoc `ShortVideoRepository.kt:66-75`） |
| 收货地址 | #79–#83 | 5 | 5 | **一致** | `toBridgeJson()` 恒发 10 键且**不含 `postal_code`**（响应里有、请求里没有）——`AddressModels.kt:48-59`，与 Dart 一致 |
| 钱包 | #84–#89 | 6 | 6 | **一致** | #84 的「3 个调用点 2 种形状」在 KMP 拆成 `fetchSummary()` + `fetchBalance()`（后者吞错可空）——调用点数量不同，端点仍是一条 |
| 会员/支付 | #90–#95 | 6 | **5** | **不一致（有意）** | **缺失行 #95 `POST /payments/prepay`**：契约原文「**常量为死码**（`membership_payment_gateway.dart:17` 无任何调用点）。文档形状 `{channel,amount_fen,subject}`；真实 prepay 走 #91 的 `prepay_params`」。KMP 明确不落地（`PayApi.kt:33-36`）。另：#93/#94 的服务端驱动结单（`finish_purchase`/`complete_purchase` → 原生 bridge）**未接线**（`// GAP(R5)`，`PayApi.kt:86`） |
| AI SSE | #96 | 1 | 1 | **一致** | 绕开 `HttpManager` 的 `Accept: text/event-stream` / `receiveTimeout: 0` 语义在引擎侧 `SseTransport`（13 册）；本层只见 path 与 camelCase body |
| realtime | #97–#98 | 2 | 2 | **一致** | `wsUrl` 原样拨号 + `remapBackendLocalhost`（`RealtimeWsTicketApi.kt:81`）；契约 §5.4 的 4001/4002/4003 重取票在 `RealtimeConfig.requiresNewTicket`（`:62-63`）。**装配缺失**：`AppRealtimeClientImpl` 的 ticketApi/syncApi 无默认值且无生产构造点 |
| realtime（无编号行） | `POST /realtime/push` | 1（标注「**无 Dart 调用点**」） | **0** | **按契约缺席，合规** | 契约原文：「**无 Dart 调用点**（仅文档 `my_ai_project/AGENTS.md:434`（Dart 仓库） / `my_ai_project/docs/BACKEND_INTEGRATION.md:405`（Dart 仓库）），dev 推流测试用」⇒ 不属于缺口 |
| push | #99 | 1 | 1 | **端点一致，链路不一致** | `registration_id` 来源（JPush/HPush SDK）仍是占位（`PlatformPushSdk.fetchRegistrationToken`，`PushBridge.kt:99-104`），且 `PushRegistrationApi.report` **零调用方** ⇒ 实际上报链路端到端未通 |
| chat/IM | #100–#102、#109–#114 | 10 | 10 | **HTTP 契约一致，SDK 侧不一致** | `// GAP(R5)`：融云 `RCIMIWEngine`（`rongcloud_im_wrapper_plugin: ^5.44.0`）无产物 ⇒ 会话取 token 后**并未真正 connect**，消息备份只在 mock 引擎产出的事件上工作；`RongImConfig.rongAppKey = ""` 默认落 mock（`:48-67`） |
| 好友 | #103–#108 | 6 | 6 | **一致** | 无 |
| 课堂 / 内容 / 直播 | 契约表内无对应行 | 0 | 0 | **一致（双方都无）** | `feature/content` 只有 `ContentRouteHost.kt`；`feature/classroom` 只有 mock；直播壳无网络（`docs/deep-dive/21-feature-small-modules.md:85` 与 `openspec/changes/parity-flutter-to-kmp/notes/platform-gap-registry.md:29` 均登记 realtime/push 为 gap） |

**总计**：114 条编号契约行 → 113 条已实现（唯一缺席是 #95 死码），1 条无编号行（`/realtime/push`）按契约缺席，
契约外扩展 1 条（华为登录）。verb/path 与字段名逐字核对未发现拼写偏差；语义偏差集中在
**重试（#57/#58）、逐请求头（#4）、SDK 侧（#93/#94/#99/#100–#114）与零装配**四处。

---

## 6. 与 feature 层的接线关系

### 6.1 真实接线状态（以 grep 为准，不采信注释）

结论：**除 auth 一条链路外，本册 22 个端点类没有任何生产调用方**（既不在 `composeApp` 装配根，
也不在任何 Store/Screen 里被构造）。唯一「引用」是各域的 `commonTest` 契约测试（注入 fake `ApiClient`）。

| 端点类 | 注入方式 | 生产调用方 | 测试调用方（file:line） |
|---|---|---|---|
| `UserAuthHttpApi`（object） | `NetworkFacade.api()` | **有**：`AuthRepository.kt:38/60/75/107/192/221`、`AuthTokenRefresh.kt:72` | `feature/auth/src/commonTest/…/AuthHttpDecisionTest.kt` 等 4 个文件 |
| `UserProfileHttpApi`（object） | 同上 | **无**（全仓仅定义处命中） | `UserProfileContractJsonTest.kt`（只测 DTO 映射） |
| `TransactionApi` / `PointsApi` / `HomeTodoApi` / `NewCarFollowApi` / `AfterSalesApi` | 构造参数 `client` | **无**。`HomeTodoStore.kt:15`、`HomePointsStore.kt:22`、`HomeRouteHost.kt:791` 提到的 `PointsApi`/`HomeTodoApi` 是 **KDoc 里的 Dart 对标名**，Store 本身是进程内 mock | `HomeTransactionApiTest.kt:22`、`PointsApiTest.kt:24`、`HomeTodoApiTest.kt:21`、`NewCarFollowApiTest.kt:26`、`AfterSalesApiTest.kt:26` |
| `MallApi` | 构造参数（`public val client`，`MallApi.kt:27-28`） | **无**。`feature/pay/PayApi.kt:29` 提到 `MallApi` 只是交叉注释，`PayApi.kt` 的 import 里没有它 | `MallApiContractTest.kt:34/81/109/156/174-194/229/258/261/274/334/346` |
| `WalletApi` | 构造参数 | **无**。`feature/mine/…/WalletMockStore.kt:42` 是 mock 状态机的对标说明 | `WalletApiContractTest.kt:29/52/56` |
| `PayApi` | 构造参数 | **无**。`feature/commerce/…/MembershipScreen.kt` 内无 `PayApi` 引用（grep 0 命中），页面仍吃 `MembershipCatalog` 假目录 | `MembershipPayContractTest.kt:29/65/68` |
| `AddressApi` | 构造参数 | **无**。`AddressMockStore.kt:28` 同上属注释 | `AddressApiTest.kt:23` |
| `UsedCarOrderApi` / `DealInvoiceApi` / `PurchaseCalculatorApi` / `MineHttpTestApi` | 构造参数 | **无**（`MineHomeContent.kt:109` 的 `switchStore` 是 `MinePrefsStore` 本地方法，不是 #10） | 各域同名 ApiTest 测试文件（构造行约第 20–24 行） |
| `HttpCommunityPostRepository` | 构造参数（`:75-77`） | **无**。社区 UI 走 `MockCommunityEngine`，且该引擎**不实现** `CommunityPostRepository` 端口（`CommunityPostRepository.kt:20-24`） | `HttpCommunityPostRepositoryTest.kt:119, 484` |
| `HttpShortVideoRepository` | 构造参数（`:124-126`） | **无**。`VideoRouteHost.kt:106` 的 `VideoMock` 仍是 UI 私有 mock | `ShortVideoHttpRepositoryTest.kt:107-113` |
| `ImSessionApi` / `ImUserProfileApi` / `ImGroupApi` / `HttpImBackupService` | 构造参数 + `useMock: () -> Boolean`（`ImSessionApi.kt:140-145`） | **无**。聊天走 `component/…/component/chat/ImEngine.kt:146` 的 `MockImEngine` | `ImChatApiContractTest.kt:38/56/69/78/88/153/171/190/194/215/236/249/273/314/335/341/355/407/430/452/464` |
| `ImFriendApi` | 构造参数（`:117-119`） | **无**。`FriendScreen.kt:65` `FriendMockData`、`FriendDirectory.kt:29-37` 硬编码 `mock_peer_*` | `ImFriendApiContractTest.kt:37/56/65/71/82` |
| `AiSseApi` | 构造参数 `SseTransport`（`:27-29`） | **无**。`AiStreamScreen.kt` 只做静态 UI | `AiSseApiTest.kt:71/101/123/136/147` |
| `RealtimeWsTicketApi` / `RealtimeWsSyncApi` | `AppRealtimeClientImpl` 的**必填构造参数**（`component/…/realtime/AppRealtimeClientImpl.kt:51-52`） | **无装配点**：`RealtimeStartupGate.kt:34` 只接受 `AppRealtimeClient` 接口，全仓无 `AppRealtimeClientImpl(...)` 生产构造 | `AppRealtimeClientImplTest.kt:75-76, 484-485`、`RealtimeStartupGateTest.kt:46/57/245`、`RealtimeWsApiContractTest.kt:100-189` |
| `PushRegistrationApi` | 构造参数（`component/…/push/PushRegistrationApi.kt:29-33`） | **无（连测试也没有）**；`StubPushBridge`（`PushBridge.kt:46-50`）不调它 | — |

### 6.2 已接线的 auth 链路（唯一完整的一条）

```
composeApp/src/androidMain/…/NativeAndroidMain.kt:93/585/736 ─┐
composeApp/src/ohosMain/…/MainArkUIViewController.kt:167/191/  ├─ AuthRepository（会话编排）
  214/238/263 ────────────────────────────────────────────────┘        │
composeApp/src/commonMain/…/app/AppShell.kt:411（logout）              │
feature/auth/…/AuthBridge.kt:67/79/90/103（非挂起桥，原生入口）        │
                                                                        ▼
                                            UserAuthHttpApi（#1–#7）+ AuthRepository.kt:154（契约外华为）
                                                                        ▼
                                            NetworkFacade.api() → AuthRefreshingApiClient（401 刷新，13 册）
                                                                        ▲
                              AuthTokenRefresh.kt:63-72（refresh 需 raw client，避免递归）
```

要点：端点对象只做「请求/应答 → `Result<DTO>`」，不落库、不清会话（`UserAuthHttpApi.kt:28-30` KDoc），
所以 `#3 refresh` 必须由 `AuthTokenRefresh` 传裸 client；`#4 logout` 必须在清本地会话**之前**发
（`UserAuthHttpApi.kt:170-173` 注释说明头依赖当前 token）。

### 6.3 mock 开关的两侧

| 开关 | 位置 | 默认 | 影响 |
|---|---|---|---|
| `RongImConfig.forceMockIm` / `rongAppKey = ""` / `useMockImFor` | `feature/chat/…/RongImConfig.kt:48-67` | **默认 mock** | #100/#101/#102 的真实分支在生产上不会被走到 |
| `createImBackupService(client, useMock, sleep)` | `ImMessageBackupApi.kt:352-356` | 由 `useMock()` 决定 | #102 http/mock 二选一 |
| `RealtimeConfig.useMockGateway` | `core/network/…/realtime/RealtimeConfig.kt:17-18`（`@Volatile`，默认 `false`） | 真实传输 | 但因无装配点（§6.1），实际两条都不跑 |
| `MockAuthConfig.useMockAuth` | `feature/auth/…/MockAuthConfig.kt:19` | Release 为 false | 只影响 auth；命中时 `AuthRepository.kt:51-57` 直接短路，不发 #5 |
| `MockCommunityEngine` / `VideoMock` / `FriendMockData` / `MembershipCatalog` | 各 feature UI | 常驻 | 社区/短视频/好友/会员**UI 完全不走 HTTP**，与 §5「端点齐」并存 |

### 6.4 接线缺口的后果

1. 「契约对账 113/114」衡量的是**端点层**完成度，不代表任一业务域端到端可用；
   `AGENTS.md` 的分层口径里 `②feature` 屏仍连 mock store。
2. 接线时最容易踩的三个坑已在代码里留好注释：
   ① `MallApi.buy` 与 `PayApi.payBuyout` 都是**两步**，不要各自合并（`MallApi.kt:96-118`、`PayApi.kt:153-192`）；
   ② `fetchPointsGifts` 与 `fetchBalance` **永不抛**，UI 必须能渲染空态（`MallApi.kt:178-182`、`WalletApi.kt:43-46`）；
   ③ `MineHttpTestApi` 不看 `code`，接入真机时不要把它的判据当模板（`MineHttpTestApi.kt:37-42`）。
3. `scripts/check-layer-deps.sh`（`②feature` 门禁）会阻止「把 `TransactionApi` 直接给 Mine 屏用」这类
   跨 feature 复用；接线时要么在 `composeApp` 装配、要么把共享 DTO 下沉到 `③component`。

---

## 7. 未验证疑点（本册不下结论的事项）

1. **`#8–#11` 零调用是否属计划内**：`UserProfileHttpApi` 全仓无引用（含测试的构造调用也没有）。
   未找到 openspec 任务项说明「profile hydrate 属 M10」，故只记状态、不判定该缺失是否合规。
2. **`/realtime/push` 的 KMP 侧是否需要**：契约标为「无 Dart 调用点」，本册按缺席处理；
   若后端文档 `my_ai_project/docs/BACKEND_INTEGRATION.md:405`（Dart 仓库） 描述的 dev 推流要在 KMP 复用，需要产品侧决策（未验证）。
3. **`PointsStatus.calendar` 的标量元素容错**：`PointsModels.kt` 对 `calendar` 元素的处理是否与 Dart
   `PointsStatus.fromJson` 完全同构，本册只确认字段名与默认值（`today_reward` 默认 5），
   未逐行比对 Dart 原文（Dart 源不在本仓）。
4. **`AuthDeviceContext.platform()` 把 `ohos → "android"`**（`AuthApiContracts.kt:349-364`）：
   契约 §2.1 只约束 `platform: String` 非空，未定义取值域；`PushRegistrationApi.detectPlatform()`
   却把 `ohos → "harmony"`（`:96-101`）。两处映射不一致是否有意，未见文档说明（待确认）。
5. **`#57/#58` 的重试缺口是否需要补**：`PurchaseCalculatorApi.kt:26-37` 只记录风险，
   openspec 中未检索到「KMP 需要 RetryInterceptor 对等物」的验收条目（未验证）。
6. **`POST /community/posts` 的 `video_url` 显式 null**：契约 §2.12 #60 要求发 null，
   KMP 用 `NetworkJsonExplicitNulls` 达成；但 `image_urls` 恒 `[]`（当前签名没有传图入口），
   与 Dart `createPost` 是否支持带图发帖未在本册核对（端口 `CommunityPostRepository.kt:69-75` 无 image 参数）。
7. **`MineHttpTestApiTest` 之外没有 #14 的联调证据**：契约 §7.1 已声明 `BuyQaScreen` 仍是 mock，
   本册确认该状态未变，但**没有运行任何构建/测试**（任务禁止），故不断言当前测试全绿。

---

## 附：本册覆盖的端点文件清单（22 个，逐篇整读）

`core/network/…/realtime/{RealtimeWsTicketApi,RealtimeWsSyncApi,RealtimeConfig}.kt`、
`component/…/push/{PushRegistrationApi,PushBridge}.kt`、
`feature/auth/…/{UserAuthHttpApi,AuthApiContracts,UserProfileHttpApi,UserProfileContracts,AuthRepository,AuthTokenRefresh,MockAuthConfig,AuthBridge}.kt`、
`feature/home/…/api/{HomeApiWire,HomeTimeWire,HomeTodoApi,HomeTodoApiModels,HomeTransactionApi,HomeTransactionModels,PointsApi,PointsModels}.kt`、
`feature/home/…/newcar/{NewCarFollowApi,NewCarFollowModels}.kt`、
`feature/home/…/aftersales/{AfterSalesApi,AfterSalesModels}.kt`、
`feature/mine/…/httpdebug/{MineHttpTestApi,HarmonyIndexModels}.kt`、
`feature/mine/…/usedcar/{UsedCarOrderApi,UsedCarOrderModels}.kt`、
`feature/mine/…/dealinvoice/{DealInvoiceApi,DealInvoiceModels}.kt`、
`feature/mine/…/calculator/{PurchaseCalculatorApi,PurchaseCalculatorModels}.kt`、
`feature/mine/…/address/{AddressApi,AddressModels}.kt`、
`feature/commerce/…/{MallApi,MallRequests,MallProductModels,MallOrderModels,MembershipCatalog}.kt`、
`feature/pay/…/{PayApi,MembershipModels}.kt`、
`feature/wallet/…/{WalletApi,WalletModels}.kt`、
`feature/community/…/http/{CommunityPostRepository,HttpCommunityPostRepository,CommunityPostModels}.kt`、
`feature/media/…/{ShortVideoRepository,ShortVideoModels}.kt`、
`feature/chat/…/{RongImConfig,ImSessionApi,ImUserProfileApi,ImGroupApi,ImMessageBackupApi,ImJsonRead}.kt`、
`feature/friend/…/ImFriendApi.kt`、`feature/ai/…/AiSseApi.kt`。





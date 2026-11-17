# 13 · `core/network` 深度文档（引擎 / 基础设施层）

> 本册覆盖 `:core:network` 的**引擎与基础设施**：客户端构造、信封解析、错误/鉴权、SSE 传输、realtime WS 基础件、序列化、三端 actual 与全部测试。
> 业务端点册（`14-core-network-endpoints.md`）负责 feature 层各 `*Api.kt`；本模块内唯二的 `RealtimeWsTicketApi.kt` / `RealtimeWsSyncApi.kt` 是 realtime WS 的 HTTP 腿、属 WS 基础件，**归本册展开**（与 14 册不重复）。
> 引用规则：正文与表格中的 `文件名.kt:行号` 均为仓库内**唯一 basename**（已验证全仓无同名冲突），路径见 §2 清单表；`build.gradle.kts`（根工程）与 `core/network/build.gradle.kts` 用全路径区分。行号以当前工作区为准。

---

## 1. 模块定位与构建实证

### 1.1 定位

`:core:network` 是全工程**唯一持有 Ktor 的模块**（`core/network/build.gradle.kts:4` 头部注释自证），承担四条 lane：

| lane | 契约来源 | 本模块入口 |
|---|---|---|
| 普通 HTTP（`{code,message,data}` 信封） | 契约表 114 行（Flutter d7876a1 parity） | `ApiClient.kt` / `KtorApiClient.kt` / `OhosApiClient.kt` |
| SSE 流式（`POST /api/v1/sse/completions`） | 契约表 #96、ADR 0005 §1 | `Sse.kt`（传输）+ `core/sse`（帧协议，同模块共居） |
| realtime WS 基础件（换票/补投/传输抽象） | 契约表 §2.17 #97/#98、ADR 0005 §2 | `realtime/` 包 8 个文件 |
| host/环境/会话配置 | Flutter `BackendHttpConfig` | `NetworkConfig.kt` / `DemoApiHosts.kt` / `BackendHostPolicy.kt` |

### 1.2 依赖：纯叶子模块

`core/network/build.gradle.kts:44-46` 的 `commonMain` 只声明 `implementation(libs.kotlinx.serialization.json)`；**没有任何 `api(projects(...))`** —— 本模块不依赖 `:core:*` 兄弟模块，会话 token 等外部状态全部由上层经回调/参数注入（`NetworkFacade.setTokenExpiredHandler`、`TokenRefresh.kt` 的 policy/refresher）。这是 core 层里少见的「零项目依赖」模块，可单独编译与单独测试。

根工程 `build.gradle.kts:35` 把 `:core:*`/`:feature:*` 纳入共享接线；`build.gradle.kts:87-102` 声明 target：androidTarget + `iosX64/iosArm64/iosSimulatorArm64` + `ohosArm64/ohosX64`（`-PandroidOnly=true` 时跳过全部 native，见 `build.gradle.kts:57` 与本模块 `core/network/build.gradle.kts:13,26,67`）；`build.gradle.kts:107-117` 统一注入 coroutines/atomicfu/serialization-json(api) 与 commonTest 的 kotlin-test；`build.gradle.kts:118-127` 建立 `iosMain`、`ohosMain` 中间层。

### 1.3 源集 ↔ 三端映射

| 源集 | 被哪些 target 使用 | 内容 | 关键证据 |
|---|---|---|---|
| `commonMain` | 全部 6 target | 接口/DTO/解析器/错误码/配置/`core/sse`/`realtime/` | `core/network/build.gradle.kts:43-46` |
| `networkKtorMain`（dependsOn commonMain） | androidMain、iosMain、ohosArm64Main | Ktor 版 `KtorApiClient`、`HttpClientDefaults`、`KtorRealtimeWsTransport` | `core/network/build.gradle.kts:48-58`；`:60-65`（android）、`:68-72`（ios）、`:75-80`（ohosArm64） |
| `androidMain` | androidTarget | OkHttp 引擎 actual + 模拟器识别 + bootstrap | `HttpClientFactory.android.kt:10` |
| `iosMain` | iosX64/Arm64/SimulatorArm64 | Darwin 引擎 actual + NSBundle 版本 | `NetworkBootstrap.ios.kt:12-16` |
| `ohosMain` | ohosArm64Main + ohosX64Main（根工程 `build.gradle.kts:124-126`） | 两架构共享的 `remapBackendLocalhost`/bootstrap actual + **cinterop def 目录** | `BackendLocalhost.ohos.kt`、`NetworkBootstrap.ohos.kt`、`src/ohosMain/cinterop/` |
| `ohosArm64Main` | ohosArm64 | CIO 引擎 actual | `HttpClientFactory.ohosArm64.kt:11` |
| `ohosX64Main` | ohosX64 | `OhosApiClient` + `OhosHttpTransport`（net_http cinterop）+ WS 桩 | `core/network/build.gradle.kts:29-39` |
| `commonTest` | 全部 target 的测试编译 | 10 个纯逻辑测试（不依赖 Ktor 类型，见 `RealtimeWsApiContractTest.kt:29` 注释） | `core/network/build.gradle.kts:83-85` |
| `androidUnitTest` | androidTarget 单测 | 3 个 MockEngine 测试（ktor-client-mock） | `core/network/build.gradle.kts:86-90` |

### 1.4 为什么是 CPF Ktor `3.3.3-0.3.0` 与 net_http 回落（真实原因）

- 版本目录 `gradle/libs.versions.toml:32-33` 注明：「CPF OH line (ohosArm64). Plain 3.3.x has no ohos variant — see adopt-cpf-ktor-ohos」，全部 ktor 坐标（`libs.versions.toml:86-95`）统一走该 fork 版本。
- `core/network/build.gradle.kts:27-28` 注释：「net_http cinterop only on ohosX64 (no CPF Ktor ohosX64 klib)」。**ohosArm64 已退役 cinterop**（`HttpClientFactory.ohosArm64.kt:6`：`CIO (HTTPS gate passed — cinterop retired on this target)`）；**ohosX64 因 CPF 未发布 ohosX64 klib 而保留 cinterop 兜底**（`HttpClientFactory.ohosX64.kt:4`）。
- WS 同理：CPF `ktor-client-websockets` 有 ohosArm64 klib、无 ohosX64 klib → ohosX64 直接抛类型化 `RealtimeWsError.Unsupported`，不做伪实现（`RealtimeWsTransportFactory.ohosX64.kt:4-16`）。
- 结论（不许粉饰）：**ohosX64 上 SSE 与 WS 都不存在**，只有阻塞式整包 HTTP；`Sse.kt:136-148` 的 `SseStreamingUnsupportedException` 与 GAP(R5) 注释就是官方承认渠道。

### 1.5 cinterop 资产（ohosX64）

| 文件 | 作用 |
|---|---|
| `net_http.def` | `package = platform.netstack`、`staticLibraries = libdemo_net_http.a`、`libraryPaths = src/ohosMain/cinterop/libs/ohosX64`（相对路径） |
| `demo_net_http_bind.c`（27 行） | 对 OHOS `OH_Http_*` API 的薄包装：CreateHeaders/SetHeaderValue/CreateRequest/Request/Destroy |
| `include/demo_net_http_bind.h`（23 行） | 上述 `Demo_Http_*` 声明（cinterop 头） |
| `include/demo_net_http_ext.h`（15 行） | `Demo_HttpRequestOptions` 等扩展结构（`OhosHttpTransport.kt:135,166` 使用） |
| `libs/ohosX64/libdemo_net_http.a`、`libs/ohosArm64/...` | 预编译产物；`ohosArm64` 目录是 cinterop 退役后的遗留物，**当前只有 ohosX64 编译消费**（`core/network/build.gradle.kts:29-39` 只给 ohosX64 建 cinterop） |

sysroot 解析链见 `core/network/build.gradle.kts:15-23`：`local.properties` 的 `local.ohos.native=` → 环境变量 `OHOS_NATIVE_SYSROOT` → DevEco 默认路径。

---

## 2. 全量文件清单（62 个 .kt + 构建脚本 + cinterop）

图例：**引擎** = 本册展开；**端点(WS)** = `*Api.kt` 形态但属 WS 基础件，本册展开、14 册只列名。commonMain 公共前缀 `src/commonMain/kotlin/com.example.my_kmp_project/`，其余源集同构，表中省略为 `…/`。

### 2.1 commonMain · `core/network/`（19 个）

| 文件 | 归属 | 作用 | 关键行 |
|---|---|---|---|
| `…/network/ApiClient.kt` | 引擎 | 统一出口接口 + expect 工厂 + 13 个 typed 扩展 | 17, 62-66, 69-72, 75-157 |
| `…/network/ApiEnvelope.kt` | 引擎 | `{code,message,data}` + OAuth 兜底字段 DTO；`FlexibleIntSerializer` | 23-31, 36-52, 54-55 |
| `…/network/ApiEnvelopeParser.kt` | 引擎 | 信封解析 + 错误码分派 + `decodeEnvelopeData` | 13-93, 101-104 |
| `…/network/ApiResponse.kt` | 引擎 | `ApiResponse`/`NetworkError` sealed/`TokenExpiredHandler` | 10-22, 24-33, 35-37 |
| `…/network/BackendHostPolicy.kt` | 引擎 | loopback host 改写策略（纯函数） | 16-24, 27, 33-48 |
| `…/network/BackendLocalhost.kt` | 引擎 | `expect fun remapBackendLocalhost` | 7 |
| `…/network/DemoApiHosts.kt` | 引擎 | 环境枚举 + host 常量 + 环境↔URL 换算 | 23-66 |
| `…/network/DemoRequestHeaders.kt` | 引擎 | 统一请求头构造（Bearer/Client-Type…） | 5-13 |
| `…/network/HttpTextResponse.kt` | 引擎 | 非信封 POST 的状态+原文载体（oauth 风格） | 6-9 |
| `…/network/LanHost.kt` | 引擎 | 局域网回退 host 常量（**当前有回归，见 §13.1**） | 19, 21-22 |
| `…/network/ListPage.kt` | 引擎 | 分页方言归一 + `queryOf`/`pageQuery`/`limitOffsetQuery` | 20-33, 35-45, 52-63, 92-114 |
| `…/network/NetworkBootstrap.kt` | 引擎 | 启动装配（clientType/版本/Release→Product） | 6-23, 25-33 |
| `…/network/NetworkBusinessHandlers.kt` | 引擎 | 非 token 业务码回调组 | 9-15 |
| `…/network/NetworkCodes.kt` | 引擎 | 双码表常量 | 13-33 |
| `…/network/NetworkConfig.kt` | 引擎 | 可变会话配置（token/baseUrl/host 注入） | 6-79 |
| `…/network/NetworkFacade.kt` | 引擎 | 进程级门面 + 装饰器装配 | 9-107 |
| `…/network/NetworkJson.kt` | 引擎 | 两个全局 `Json` 实例 | 6-11, 19-24 |
| `…/network/Sse.kt` | 引擎 | SSE 传输 seam、异常词汇表、开流闸门、UTF-8 增量解码器 | 21-36, 54-65, 73-148, 161-182, 194-281 |
| `…/network/TokenRefresh.kt` | 引擎 | 401 决策枚举 + `AuthRefreshingApiClient` 装饰器 | 14, 20-31, 40-158 |

### 2.2 commonMain · `core/sse/`（4 个，包与模块同仓不同目录）

| 文件 | 归属 | 作用 | 关键行 |
|---|---|---|---|
| `…/sse/SseFrame.kt` | 引擎 | 一条空行分隔的逻辑帧 | 12-20 |
| `…/sse/SseParser.kt` | 引擎 | 增量行协议解析器（WHATWG） | 15-120 |
| `…/sse/AiStreamEvent.kt` | 引擎 | AI 流领域事件 sealed | 8-36 |
| `…/sse/AiSseDecoder.kt` | 引擎 | 帧 → `AiStreamEvent` 解码 | 36-93 |

### 2.3 commonMain · `core/network/realtime/`（7 个）

| 文件 | 归属 | 作用 | 关键行 |
|---|---|---|---|
| `…/realtime/RealtimeConfig.kt` | 引擎 | 心跳/退避/关闭码/topic 常量 + 判定函数 | 15-74, 80-89 |
| `…/realtime/RealtimeEnvelope.kt` | 引擎 | 统一 WS 帧 DTO + serializer | 27-137 |
| `…/realtime/RealtimeJson.kt` | 引擎 | realtime 专用宽容取值工具与 3 个 serializer | 30-133 |
| `…/realtime/RealtimeWsError.kt` | 引擎 | WS lane 专用 sealed 异常 | 11-46 |
| `…/realtime/RealtimeWsSyncApi.kt` | 端点(WS) | `POST /api/v1/realtime/sync` 补投 | 23-71 |
| `…/realtime/RealtimeWsTicketApi.kt` | 端点(WS) | `POST /api/v1/realtime/ws-ticket` 换票 + URL 落地 | 24-81 |
| `…/realtime/RealtimeWsTransport.kt` | 引擎 | WS 传输抽象 + expect 工厂 + `RealtimeWsClose` | 27-73 |

### 2.4 networkKtorMain（3 个）

| 文件 | 归属 | 作用 | 关键行 |
|---|---|---|---|
| `…/network/HttpClientDefaults.kt` | 引擎 | Ktor 插件装配（内容协商/超时/日志/动态头/baseUrl） | 16-23, 25-51 |
| `…/network/KtorApiClient.kt` | 引擎 | Ktor 版 `ApiClient`（android/ios/ohosArm64 共用） | 31-266 |
| `…/realtime/KtorRealtimeWsTransport.kt` | 引擎 | Ktor WebSockets 传输实现 + 最小插件安装 | 37-180 |

### 2.5 androidMain（4）/ iosMain（4）/ ohosMain（2）/ ohosArm64Main（2）/ ohosX64Main（4）

| 文件 | 归属 | 作用 | 关键行 |
|---|---|---|---|
| `androidMain/…/BackendLocalhost.android.kt` | 引擎 | 模拟器→10.0.2.2；真机告警 | 3-9, 11-18, 23-31 |
| `androidMain/…/HttpClientFactory.android.kt` | 引擎 | OkHttp actual 工厂 | 6-13 |
| `androidMain/…/NetworkBootstrap.android.kt` | 引擎 | android 身份 actual（isRelease 恒 false！） | 3-13 |
| `androidMain/…/realtime/RealtimeWsTransportFactory.android.kt` | 引擎 | OkHttp WS actual | 7-8 |
| `iosMain/…/BackendLocalhost.ios.kt` | 引擎 | 模拟器同机直达（不映射） | 7-10 |
| `iosMain/…/HttpClientFactory.ios.kt` | 引擎 | Darwin actual 工厂 | 6-13 |
| `iosMain/…/NetworkBootstrap.ios.kt` | 引擎 | NSBundle 版本 + `Platform.isDebugBinary` | 7-20 |
| `iosMain/…/realtime/RealtimeWsTransportFactory.ios.kt` | 引擎 | Darwin WS actual | 7-8 |
| `ohosMain/…/BackendLocalhost.ohos.kt` | 引擎 | 双架构共享：注入优先，无 10.0.2.2 | 7-10 |
| `ohosMain/…/NetworkBootstrap.ohos.kt` | 引擎 | ohos 身份 actual（isRelease 恒 false） | 3-13 |
| `ohosArm64Main/…/HttpClientFactory.ohosArm64.kt` | 引擎 | CIO actual 工厂 | 6-14 |
| `ohosArm64Main/…/realtime/RealtimeWsTransportFactory.ohosArm64.kt` | 引擎 | CIO WS actual | 10-11 |
| `ohosX64Main/…/HttpClientFactory.ohosX64.kt` | 引擎 | 回落：`OhosApiClient` | 6-9 |
| `ohosX64Main/…/OhosApiClient.kt` | 引擎 | cinterop 版 `ApiClient`（SSE 抛类型化错） | 7-217 |
| `ohosX64Main/…/OhosHttpTransport.kt` | 引擎 | net_http 桥 + 单飞互斥 + holder 生命周期 | 14-291 |
| `ohosX64Main/…/realtime/RealtimeWsTransportFactory.ohosX64.kt` | 引擎 | 无 WS：抛 `RealtimeWsError.Unsupported` | 17-19 |

### 2.6 测试（13 个）

| 文件 | 归属 | 覆盖 |
|---|---|---|
| `commonTest/…/network/ApiEnvelopeParserTest.kt` | 引擎 | 信封/401/403/429/畸形 JSON/字符串 code（12 用例） |
| `commonTest/…/network/AuthRefreshingApiClientTest.kt` | 引擎 | 刷新/重试/互踢/豁免路径/动词面（11 用例） |
| `commonTest/…/network/BackendHostPolicyTest.kt` | 引擎 | loopback 策略（**当前 2 断言失败，见 §13.1**） |
| `commonTest/…/network/ListPageTest.kt` | 引擎 | 三种分页方言 + 查询构造（8 用例） |
| `commonTest/…/network/SseContractTest.kt` | 引擎 | 闸门/异常键优先级/UTF-8 解码/装饰器转发（11 用例） |
| `commonTest/…/network/realtime/RealtimeConfigAndCloseCodeTest.kt` | 引擎 | 常量与关闭码语义（8 用例） |
| `commonTest/…/network/realtime/RealtimeEnvelopeTest.kt` | 引擎 | WS 帧编解码契约（10 用例） |
| `commonTest/…/network/realtime/RealtimeWsApiContractTest.kt` | 引擎 | #97/#98 裸 body 契约（15 用例） |
| `commonTest/…/sse/AiSseDecoderTest.kt` | 引擎 | 帧→领域事件（18 用例） |
| `commonTest/…/sse/SseParserTest.kt` | 引擎 | 行协议全分支（21 用例） |
| `androidUnitTest/…/KtorApiClientMockTest.kt` | 引擎 | Bearer 注入与 AUTH_SKIP_HOST 跳过 |
| `androidUnitTest/…/KtorApiClientVerbTest.kt` | 引擎 | PUT/PATCH/DELETE 动词与 query 编码落线 |
| `androidUnitTest/…/KtorSseTransportTest.kt` | 引擎 | SSE 真增量、多字节切分、前流错误类型 |

### 2.7 构建与 cinterop（非 .kt，同属清单）

`core/network/build.gradle.kts`（96 行）、`net_http.def`、`demo_net_http_bind.c`、`include/demo_net_http_bind.h`、`include/demo_net_http_ext.h`。

---

## 3. 类型/函数详解 —— 门面与请求构造（commonMain）

### 3.1 `ApiClient.kt`

| 成员 | 签名（摘要） | 语义与副作用 | 被谁调用 |
|---|---|---|---|
| `interface ApiClient : SseTransport` | 17 | 信封调用 + 流式调用的**唯一出口**；feature 只拿 `NetworkFacade.api()` | 全仓 feature/component 测试替身均实现此接口 |
| `getRaw(path, query)` | 18 | 返回原始响应文本；异常语义按平台 lane 不同（§6 步 3） | `HomeTransactionApi.kt` 等端点册 |
| `getAbsoluteRaw(url)` | 22 | 绝对 URL（COS 资产），不走业务 host | **生产无调用者**，仅 `KtorApiClientMockTest.kt:62` / 测试替身（§13.5） |
| `postRaw(path, body)` | 23 | 整包 POST，不解析信封 | `RealtimeWsTicketApi.kt:36`、`RealtimeWsSyncApi.kt:30` |
| `postForm(path, fields)` | 25 | x-www-form-urlencoded，返回 `HttpTextResponse`，**不剥信封**（为 OAuth token 风格预留） | 生产无调用者（§13.5） |
| `putRaw/patchRaw/deleteRaw` | 26-28 | 其余动词语义 | 端点册 |
| `getApi/postApi/putApi/patchApi/deleteApi` | 29-53 | 带 `parseData: (JsonElement?) -> T?` 的信封调用 | 装饰器 `TokenRefresh.kt:75-103` 全部转接 |
| `openSseTextStream`（默认实现） | 62-66 | **不 override 即抛** `SseStreamingUnsupportedException`，杜绝静默空流；被 `SseContractTest.kt:127-134` 锁定 | — |
| `expect fun createPlatformApiClient(onTokenExpired, businessHandlers)` | 69-72 | 平台工厂；四个 actual 见 §5 | `NetworkFacade.kt:26-29,69-72` |
| 扩展 `getApi<T>` / `getListPage<T>` | 75-78 / 84-87 | reified 版，`parseData = ::decodeEnvelopeData` / `decodeListPage<T>` | `WalletApi.kt:64`、`ShortVideoRepository.kt:148` |
| 扩展 `postApiDecoded/postApi(Req,Res)/postApiOk` | 92-110 | 请求体由 `NetworkJson.encodeToString(body)` 编码；`*Ok` 只关心 code | 端点册各 `*Api.kt` |
| 扩展 `putApi/putApiOk/patchApi/patchApiOk/deleteApi/deleteApiOk/getApiString` | 113-157 | 同型；`getApiString` 把 `data` 宽容读成字符串（148-157） | `KtorApiClientVerbTest.kt:36-38` |

### 3.2 `NetworkFacade.kt`（进程级门面）

- 状态：5 个 `@Volatile` 注入槽（`NetworkFacade.kt:10-23`）+ 双 client：`rawClient`（未装饰，26-29）与 `client = decorate(rawClient)`（31-32）。
- `api()`（34）给业务；`rawApi()`（40）给刷新请求——Flutter `skipAuthRefresh` 的 KMP 对等，防递归（`TokenRefresh.kt:25-28` 注释同义）。
- `setTokenExpiredHandler` / `setBusinessHandlers` / `setAuthRefreshSupport`（42-66）都会 `rebuildClient()`。
- **对象初始化顺序（eager）**：`rawClient`（25-29）在 object 加载时刻就构建，此时 :10-23 声明的五个注入槽全部仍是 null —— 因此 bootstrap 之前 `api()` 拿到的就是「无回调、无装饰」的平台 client（`decorate` :96 条件不满足、直接走 103-104 返回 platform），不存在空窗。
- **两个 volatile 字段成对交换非原子**：`rebuildClient()` 先写 `rawClient`（69-72）后写 `client`（73），并发读者在两次写之间可见「旧 platform 装饰出的新 client」与 `rawApi()` 旧引用短暂不一致。当前装配全部在主线程顺序执行（`AuthSessionBootstrap.kt:22-26`、`NativeAndroidMain.kt:91-97`），不显现；安全性靠调用方纪律，无类型层保证。
- `rebuildClient()`（68-74）关键细节：**刷新启用时平台 client 不再挂 onTokenExpired**（70 行 `if (isAuthRefreshEnabled()) null else tokenExpiredHandler`），401 流的所有权整体移交给装饰器，避免「先跳登录再刷新成功」。
- internal 读取器 `currentTokenExpiredHandler()` / `currentBusinessHandlers()`（:86-88）**全仓零调用者**（grep 仅命中定义处），与 §13.6 同类死代码。
- `decorate`（93-106）：policy 与 refresher **同时非空**才包 `AuthRefreshingApiClient`。
- `bindAccessToken/clearAccessToken`（76-84）：写 `NetworkConfig.accessToken` 后重建 client —— 因为 baseUrl 是构建期快照（§5.2），token 却是每请求动态头（`DemoRequestHeaders.kt:9-12`），所以绑定 token 本身不强制重建，重建是为了 baseUrl 环境同步。
- 生产调用者：`AccountFacade.kt:54,56,64,185`（登录态↔token 绑定/清除）；`AuthSessionBootstrap.kt:22-26`（装机刷新策略，policy/handler 实现见 `AuthTokenRefresh.kt:31-40`，其中决策经 `classifyAuthHttp`：TryRefresh→Refresh、ForceLogout→ForceLogout、Forbidden/None→PassThrough）；`AppContainer.kt:31`；`NativeAndroidMain.kt:91-97`（Android 原生 Tab 挂 onTokenExpired、离开时摘除）。

### 3.3 请求头与内容协商 —— `DemoRequestHeaders.kt` + `HttpClientDefaults.kt`

`DemoRequestHeaders.forUrl`（`DemoRequestHeaders.kt:5-13`）：恒发 `Client-Type`；`Client-Version`/`Client-BuildNumber` 非空才发；`accessToken` 非空白且 URL 不含 `AUTH_SKIP_HOST`（`cdn.demo.local`，`DemoApiHosts.kt:30`）才发 `Authorization: Bearer …`。跳过判据在 `NetworkConfig.skipAuthForUrl`（`NetworkConfig.kt:54-55`，大小写不敏感的 `contains`）。

Ktor 侧经 `DemoDynamicHeaders` 插件每请求执行（`HttpClientDefaults.kt:16-23`）——**动态**，改 token 不用重建 client；ohosX64 侧则在每个方法里手工调用同一函数（`OhosApiClient.kt:14,22,31,49,79,97,114,128,140,153,169,186,195`），两端点同源保证头集合一致。

内容协商与超时（`HttpClientDefaults.kt:25-47`）：`ContentNegotiation(json(NetworkJson))`（26-28，实际引擎全程按 String 收发，此插件仅兜底）、`HttpTimeout` connect/request/socket 均 `NetworkConfig.TIMEOUT_MS = 30_000`（29-33 + `NetworkConfig.kt:7`）、`Logging` INFO 级打到 `println("[Ktor] …")`（34-41，Release 也打，见 §13.8）、`defaultRequest { url(baseUrl + "/"); contentType(json) }`（43-46）——baseUrl 在**构建时快照** `NetworkConfig.effectiveBaseUrl()`。

### 3.4 `NetworkConfig.kt` / `DemoApiHosts.kt` / host 改写

`effectiveBaseUrl()`（`NetworkConfig.kt:41-52`）决策序：Release → 强制 `PRODUCT`；Debug → `baseUrlOverride` 优先，否则 `DemoApiHosts.forEnvironment(env, override)`（`DemoApiHosts.kt:38-44`，Custom 空值回退 TEST）；最后统一过 `remapBackendLocalhost`。

`DemoApiHosts.kt:24-27`：DEV/TEST/STAGING **三个常量同值** `http://127.0.0.1:8080`（演示骨架事实），PRODUCT 是唯一真实差异 host；`debugSwitchHosts`（32-36）给调试切屏用；`environmentForHost`/`labelForBaseUrl`（46-65）做 URL↔标签回读，注意 49-51 行把 `127.0.0.1:8080` 与 `10.0.2.2:8080` **都归为 Test**。

`resolveLoopbackHostMapping`（`BackendHostPolicy.kt:16-24`）：注入 > Android 模拟器 `10.0.2.2` > 不改写（返回 null）。`replaceLocalhostHost`（33-48）手工切 scheme/authority/port/path，只重写 host 部分且仅当 host ∈ {127.0.0.1, localhost}（27 行）。平台差异由三个 `remapBackendLocalhost` actual 承担（§5.4/§5.6/§5.7）。

### 3.5 HTTP 动词封装与分页 —— `KtorApiClient.kt` + `ListPage.kt`

动词面（Ktor 共用实现）：GET 独立两条（43-59, 158-177），POST 独立两条（61-69, 179-198），PUT/PATCH/DELETE 走泛化 `sendRaw`/`sendApi`（200-225 + 227-240 + 242-265），内部统一 `http.request(path.trimStart('/')) { method = …; if (body.isNotEmpty()) setBody(body); query→parameter }`。`KtorApiClientVerbTest.kt:34-68` 断言动词与 query 编码真上线（`DELETE …?reason=user`、`GET …?page=2&size=10`）。

分页三件套（`ListPage.kt`）：
- `queryOf`（92-93）：丢弃 null 与空白值（对齐 Dart 逐键 `if (x != null)`）。
- `pageQuery`（99-103）：`page/size`，**服务端 page 为 1-based**。
- `limitOffsetQuery`（109-114）：`limit/offset`，**page 0-based**，`offset = page * size`（Dart `/transactions`、`/wallet/ledger` 方言；调用例 `HomeTransactionApi.kt:54`、`MallApi.kt:40`）。
- 游标式分页引擎层**没有**（全仓唯一游标是 realtime sync 的 `sinceSeq`，见 §4.5）。

`ListPage`/`ListPagination`（35-45, 20-33）归一三种 BFF 方言：`{list,pagination}`、`{items}`、裸数组（`decodeListPage` 52-63 的 when 三分支）；`hasMore` 优先级 `totalPages` > `page*size<total` > false（27-32），无 pagination 时退化为「拿满即有下一页」（43-44）；`decodeListPagination`（70-79）同时接受 `totalPages`/`total_pages`（77 行）；非 object 数组项跳过（66-67 `filterIsInstance<JsonObject>`）；畸形 data → 空页不抛（53, 61）。

### 3.6 统一信封解析 —— `ApiEnvelope.kt` + `ApiResponse.kt` + `ApiEnvelopeParser.kt`

`ApiEnvelope`（`ApiEnvelope.kt:23-31`）：`code` 用 `FlexibleIntSerializer`（36-52，数字或数字字符串都吃，BFF 两种都发过）；`data: JsonElement?` 留待泛型解码；`error`/`error_description`（28-30，OAuth 401 裸体兜底字段）。`dataOrNull`（54-55）把 `JsonNull` 归 null。

`ApiResponse`（`ApiResponse.kt:10-22`）：`code/message/data/raw/httpStatus`；`isSuccess = code ∈ {BFF_OK(0), OK(200)}`（21 行，两套码表并存的历史原因见 `NetworkCodes.kt:6-11`）。

`ApiEnvelopeParser.parse`（`ApiEnvelopeParser.kt:13-93`）完整分支序：
1. 尝试解码信封（20-22，失败得 null）。
2. **HTTP 401 特判**（25-39）：若 `httpStatus == 401` 且体**没有**业务 code ⇒ 构造 `NetworkError.TokenExpired`，调 `onTokenExpired`，直接回 `code=401`（消息取 `error_description`→`error`→固定文案「已退出登录，请重新登录」，28-29 + 95-97）。若 401 体里带业务 code ⇒ 不在此拦截，继续走通用流程。
3. **非信封体**（41-49）：`code = httpStatus(≥400) 否则 PASSTHROUGH(-1)`，消息「响应格式错误」——注释（43 行）明言对齐 Dart「缺 code 记 -1，不可当成功」。
4. 通用流程：`code = envelope.code ?: httpStatus(≥400) ?: -1`（51-53）；`message = envelope.message(非空白) ?: error_description ?: error`（54-55）；随后按 code 分派回调（58-84）——`401→onTokenExpired`、`100/301/429/500→businessHandlers 对应槽`、`403→onForbidden`（80 行注释：**不登出、不刷新**）。注意分派**只触发回调，不裁剪响应**：任何 code 都原样返回 `ApiResponse`（86-92）。
5. `decodeEnvelopeData<T>`（101-104）：`runCatching` 包裹类型解码，**失败静默得 null data 而 code 仍成功**——坑，见 §13.7。

### 3.7 小文件签名速查（`NetworkBootstrap` / `NetworkBusinessHandlers` / `HttpTextResponse` / `BackendLocalhost`）

| 成员 | 签名与语义 | 调用者 / 生产状态 |
|---|---|---|
| `applyNetworkBootstrap`（`NetworkBootstrap.kt:6-23`） | 四个参数默认值全部取自同文件 expect（:27-33）；写 `NetworkConfig` 身份三字段（12-14）；Release 分支（15-18）设 `BuildMode.Release` + `NetEnvironment.Product` 且**清空 `baseUrlOverride`**；Debug 分支（19-21）只改 buildMode、不动 env/override；末尾统一 `NetworkFacade.rebuildClient()`（:22）——因为 baseUrl 是 client 构建期快照（§3.3） | 唯一入口 `platformNetworkBootstrap()`（:25），三平台 actual 函数体都只有 `applyNetworkBootstrap()` 一行（`NetworkBootstrap.android.kt:11-13`、`NetworkBootstrap.ios.kt:18-20`、`NetworkBootstrap.ohos.kt:11-13`）；由 composeApp 启动点调用（`MainActivity.kt:28`、`MainViewController.kt:18`、`MainArkUIViewController.kt:39`） |
| `NetworkBusinessHandlers`（`NetworkBusinessHandlers.kt:9-15`） | data class，五个槽全为 `((NetworkError.Business) -> Unit)?` 默认 null：`onInvalidAuth`(100) / `onInvalidMember`(301) / `onBusy`(429) / `onInvalidAccount`(500) / `onForbidden`(403)；注释 6-7 行记录 Flutter d7876a1 渊源「403 → 不登出、不刷新（与 onTokenExpired 互斥）」 | 唯一消费者是 `ApiEnvelopeParser.parse` 的 code 分派（`ApiEnvelopeParser.kt:58-84`）；注入通道 = `createPlatformApiClient(businessHandlers=…)`（`ApiClient.kt:71`）与 `NetworkFacade.setBusinessHandlers`（`NetworkFacade.kt:47-50`）——但**全仓无生产调用者**，构造只出现在 `ApiEnvelopeParserTest.kt:72`，上线态悬空（§13.13） |
| `HttpTextResponse`（`HttpTextResponse.kt:6-9`） | `data class (statusCode: Int, body: String)`；非信封整包响应载体，注释 3-4 行点名 oauth/token 风格；不经信封解析 | 只被 `ApiClient.postForm`（`ApiClient.kt:25`）返回；Ktor lane 因 expectSuccess 抛异常拿不到 4xx 载体、ohosX64 lane 原样返回（不对称，§13.4）；`postForm` 生产零调用（§13.5） |
| `remapBackendLocalhost`（`BackendLocalhost.kt:7`） | `public expect fun (baseUrl: String): String`；Dart `BackendHttpConfig` 对等（注释 3-4 行），只重写 loopback URL 的 host 部分（策略纯函数见 §3.4） | 生产调用点两处：`NetworkConfig.effectiveBaseUrl`（`NetworkConfig.kt:41-52`）与 `resolveRealtimeWsUrl`（`RealtimeWsTicketApi.kt:81`）；三个 actual 见 §5.4/§5.5/§5.6 |

---

## 4. 类型/函数详解 —— 错误、鉴权、SSE、realtime（commonMain）

### 4.1 错误码与异常词汇 —— `NetworkCodes.kt` / `ApiResponse.kt` / `NetworkBusinessHandlers.kt`

`NetworkCodes`（`NetworkCodes.kt:13-33`）：`BFF_OK=0`(15)、`NOT_NETWORK=0`(17，遗留 iOS 表，与 BFF_OK **同值不同义**，文件头 6-11 行明确「勿合并」)、`OK=200`(18)、`INVALID_AUTH=100`(19)、`INVALID_MEMBER=301`(20)、`EXPIRE_TOKEN=401`(21)、`FORBIDDEN=403`(23-28，业务码或 HTTP 回退都映射至此，不登出不刷新)、`BUSY=429`(30)、`INVALID_ACCOUNT=500`(31)、`PASSTHROUGH=-1`(32，缺 code 回退专用)。

`NetworkError` sealed（`ApiResponse.kt:24-33`）：`Transport(message, httpStatus?)`(29)——传输失败，`httpStatus` 仅在服务器真应答错误时非空；`Business(code,message)`(30)；`TokenExpired`(31-32，默认文案)。`TokenExpiredHandler` fun interface（35-37）。**realtime lane 刻意不共用这套 sealed**，独立 `RealtimeWsError`（理由见 `RealtimeWsError.kt:6-9`：sealed 归 HTTP lane 独占）。

### 4.2 401/403 与刷新重试 —— `TokenRefresh.kt`

- `AuthRefreshDecision`（14）：`Refresh / ForceLogout / PassThrough` 三值；core 只给枚举，判定表由 feature/auth 注入（20-22 `AuthRefreshPolicy.decide(statusCode, code, message)`；语义注释 9-12：10023/HTTP401→Refresh、10021/10022 互踢→ForceLogout、其它含 403→PassThrough）。
- `TokenRefreshHandler.refresh(raw)`（29-31）：入参是**未装饰** client；刷新失败/无 refreshToken 由实现方自清会话后抛（`AuthTokenRefresh.kt:40,63-81,99` 是生产实现）。
- `AuthRefreshingApiClient`（40-158）：
  - 7 个 `*Raw`/`postForm`/`getAbsoluteRaw` 全部**直转发、永不刷新**（56-73；`AuthRefreshingApiClientTest.kt:311-324` 锁定该语义）；
  - 5 个 `*Api` 走 `withRefresh`（75-103）；
  - `isAuthExemptPath`（48-54）：`/user/login|register|refresh|logout` 四条路径免刷新（`AuthRefreshingApiClientTest.kt:208-220` 锁定）；
  - `withRefresh`（131-157）：先 `call()`；policy/refresher 任一为 null 直通（135-136）；决策三分——PassThrough 原样、ForceLogout 调 `onSessionCleared` 后原样返回首次应答（141-145）、Refresh 成功后**只代发一次**、失败返回首次应答（146-155）。**重试语义 = 至多一次，且不 catch 传输异常**（意味着 lane 上抛 `NetworkError.Transport` 的 HTTP 401 不会进入决策，见 §13.3）；
  - `openSseTextStream` **只转发不进刷新流**（121-125），三条理由的长注释在 105-119：会话头建流时已注入、流体不可重放（重发 POST = 服务端重生成，契约表 §5.3 at-least-once 雷区）、Dart 侧 `validateStatus < 600` 使 401 根本不构成 DioException；`SseContractTest.kt:137-148` 用计数替身锁定「不重放」。

### 4.3 SSE 传输层 —— `Sse.kt`（契约表 #96）

- 常量：`SSE_ACCEPT_HEADER_VALUE="text/event-stream"`(21)、`SSE_UPGRADE_STATUS=200`(23-27，注释：状态码本身从不构成失败，判据是 content-type)、`SSE_ERROR_CONNECTION`(30)、`SSE_ERROR_TRANSPORT_UNSUPPORTED`(33-36，KMP 独有)。
- `SseTransport.openSseTextStream(path, body, headers)`（54-65）：**冷流**，collect 才发请求；元素为 UTF-8 增量解码后的文本分片，边界=TCP/HTTP 块，不保证按行；`headers` 追加在 `Accept` 之后、同名覆盖（56-58 注释）；取消=取消收集协程（Dart CancelToken 对等，52 行）。
- `SseException`（73-133）：`statusCode: Int?`（可 null=连接未建立）、`error`（稳定错误码）、`rawBody`。`fromPreStream(statusCode, body)`（91-111）键优先级 `error` → `message` → 整段正文 → 空体时 `"SSE request failed (status)"`；`defaultErrorCode`（114-122）：400/401/403/404/429/503 → 稳定词，其余 `http_$status`（**无 200 分支**，所以「200 但 content-type 不是流」得 `http_200`，被 `SseContractTest.kt:56-69` 与 `KtorSseTransportTest.kt:156-171` 双重锁定）。
- `SseStreamingUnsupportedException`（142-148）：继承 `SseException`、`error=streaming_unsupported`、无 statusCode——GAP(R5) 的类型化承认，不是降级实现。
- `SseOpenGate`（161-182）：三件套闸门。`isEventStream`(167-168，contentType 大小写不敏感含 `text/event-stream`)；`eventStreamText()`(171-174) 仅 `status==200 && isEventStream` 放行分片流，否则 `readAllText()` 拼净正文再抛 `fromPreStream`；注释 156-159 记录与 Dart 的无害差异（无 `empty_body` 分支）。
- `Utf8ChunkDecoder`（internal，194-281）：手写多字节解码——`sequenceWidth`(273-279) 判前导字节（拒绝 0xC0/0xC1 孤立延续与 0xF8+），`decode`(198-249) 把**被切断的序列尾字节存入 `pending`** 等下一片（220-223），非法字节替换 U+FFFD（对齐 Dart allowMalformed，190-192 注释），`finish`(252-256) 残留尾巴逐字节替换。surrogate pair 手工拆分（239-245）。测试锚点：`SseContractTest.kt:152-186`（逐字节喂「中a文😀」）、`KtorSseTransportTest.kt:115-134`（真 channel 切「你好」首字节）。

**ADR 0005「SSE 只服务 AI 补全」的实现证据**：传输出口全仓唯一生产消费者是 `AiSseApi.kt:28,58`（feature/ai）；`docs/adr/0005-three-long-lived-channels-separate-transports.md:21,32` 规定端点与模块分工；门禁表述在同文件 :33。`AuthRefreshingApiClient` 对流式只做转发（§4.2）即「三条通道失败语义互不翻译」的落地（ADR :24）。

### 4.4 SSE 帧协议 —— `core/sse/`（与传输同模块、不同包）

- `SseFrame.kt:12-20`：`event` 默认 `"message"`(18)、`data` = `\n` 连接的多行载荷、`id` 可选；`retry:` 不属于帧（10 行注释）。
- `SseParser.kt` 每个分支（对照 Dart `sse_parser.dart`，移植说明 6-13 行）：
  - `feed(chunk)`（34-57）逐字符状态机：`'\n' && pendingCr` ⇒ 吞掉（`\r\n` 跨片不误拆，41 行）；`'\r'` ⇒ 结束一行并置 `pendingCr`（43-46）；`'\n'` ⇒ 结束一行（48）；其余入 carry（50-53）。**刻意偏离 Dart**：`\n`/`\r\n`/`\r` 三种行终止全支持（9-13 行注释）。
  - `handleLine`（79-99）：空行 ⇒ `emitIfReady`（80-83）；`:` 开头 ⇒ 注释/keepalive 丢弃（84）；冒号前为字段名、冒号后**只剥一个前导空格**（86-90）；字段 switch（92-98）：`event` 覆盖、`data` 追加列表、`id` 覆盖、`retry` 仅纯 ASCII 数字才记 `retryMillis`（96，118-119 判数字）、未知字段忽略。
  - `emitIfReady`（101-110）：三字段全空 ⇒ 不产帧；否则 `event ?: "message"`、`data = joinToString("\n")`。
  - `flush()`（65-71）：EOF 解最后一行，但**未收到空行的半帧直接丢弃**（60-64 注释与 `SseParserTest.kt:172-177` 锁定）。
  - `retryMillis`（30-31）：全仓生产代码**无读取点**（重连计时属 component/realtime 的退避常量，不用服务端 retry；§13.9）。
- `AiStreamEvent.kt:8-36`：sealed 四事件 `Meta/Done/Delta/Error`（Dart `ai_stream_event.dart` 同构；`Error` 嵌套命名避免遮蔽 `kotlin.Error`，30-31 注释）。
- `AiSseDecoder.kt`：`toAiStreamEventOrNull`（36-62）——类型判定 `event:` 优先于载荷 `type`（41，Dart quirk 保留，注释 31-34：无显式 `event:` 行的帧**永远解不出**，BFF 恒发 event 所以不咬人；`AiSseDecoderTest.kt:36-45` 双向锁定）；meta/delta/done/error 四分派（43-61），空 delta 文本→null（53）；`decodeError`（64-79）三级：`error` 对象取 `code/message`、非对象取内容、都没有回落顶层 `message`/默认「生成失败」（15-16）。SSE 载荷是**平铺 JSON 不是信封**（20-26 注释），`AiSseDecoderTest.kt:111-115` 锁定「信封形态解出 null」。私有 `AiSseJson` 仅 `ignoreUnknownKeys`（11-13）。

### 4.5 realtime 基础件 —— `realtime/` 包

- `RealtimeConfig.kt`（15-74）：全毫秒 Long 的心跳/退避常量（28-37），`OUTBOUND_ACK_TIMEOUT_MS`(40)、`NOTIFY_DEDUP_MAX=200`(43)、关闭码 4001/4002/4003(49-51) 与判定函数 `requiresNewTicket`(62-63)、`isAuthCloseCode`(66-67，4002 互踢**不**属鉴权类)、`WS_CLOSE_NORMAL=1000`(70)、`WS_CLOSE_NO_STATUS=1005`(73)；62-63 行注释记录 Dart 事实「三码声明但全仓无读取点」。`useMockGateway`(17-18) 是装配开关，选择逻辑在 component 层。`RealtimeTopics`（80-89）与 Go `entity.Topic*` 字符串逐一对齐。
- `RealtimeEnvelope.kt`（26-137）：`type` 缺键回退 `"unknown"`(100)、`ts/seq` 数字或数字字符串(102-103)、`payload/raw` 默认空对象(33-34)、`eventName` 是**强类型读法**（42，只认加引号字符串——修 bug 记录在 36-41 注释）、`isControl`(45，八个控制帧 83-92)、`toJson()` **省略 null 条件键、必带 type/payload、从不带 raw**(48-55)、`fromJson`(98-106 存 raw)、`fromRawJson`(109-110 畸形→null)、`RealtimeEnvelopeSerializer`(115-133 以 JsonObject 委托复刻省略规则)、`asWsText()`(136-137)。它同时是 WS 帧与 `/realtime/sync` 的 `events[]` 元素（22-24 注释），**不走信封解析**。
- `RealtimeJson.kt`：宽容取值函数群（30-82）+ 三个 serializer（88-133）：`RealtimeTextSerializer`（null/非标量→空串、缺键仍抛）、`RealtimeLongSerializer`（读不出→0）、`RealtimeNullableLongSerializer`。`realtimeBool`（64-67）只认未加引号 JSON 布尔——57-63 注释记录这是一处**生产 bug 修复**。
- `RealtimeWsError.kt`（11-43）：`Unsupported`（20-22，ohosX64 无 WS）、`BodyUnparsable(raw, cause)`（30-33，消息只带前 200 字符，46）、`TicketInvalid`（40-42，wsUrl 为空时提前失败）。ADR 0005 §4 的「失败语义互不翻译」在这里体现为独立 sealed + 与 `NetworkError` 并列抛（6-9 注释）。
- `RealtimeWsTicketApi.kt`（24-48）：`fetchTicket(accessToken)`——空 token **先抛** `IllegalStateException("缺少 access token，请先登录")`（32-35，不发请求）；`client.postRaw(TICKET_PATH, {"platform":"mobile"})`（36-39，头由统一注入带 Bearer，20-23 注释解释为何换票走 HTTP 而非 WS 首帧 JWT）；`NetworkJson` 直接解码裸 body（40-42，**四键必填**，信封体必炸——测试 `RealtimeWsApiContractTest.kt:143-156`）；`wsUrl.isEmpty()` ⇒ `TicketInvalid`（43-45）。`resolveRealtimeWsUrl`（81）只做 loopback host 改写（`remapBackendLocalhost`），scheme/端口/路径原样（契约表 §5「原样拨号」；测试 :277-283）。生产消费者：`AppRealtimeClientImpl.kt:157`。
- `RealtimeWsSyncApi.kt`（23-71）：`sync(sinceSeq, topics)`——camelCase 请求体（41-45，全仓 snake_case 的两条例外之一）；响应 `RealtimeSyncData{events[], latestSeq}`（51-56），失败同样 `BodyUnparsable`（34-36）；手搓入口 `fromJson`（62-69）跳过非 object 项、`latestSeq` 缺键归 0。调用时机与「失败不阻断连接」由 component 层负责（19-21 注释）。
- `RealtimeWsTransport.kt`（27-73）：接口五元组 `inbound/closed/isConnected/connect/send/close/dispose`（Dart 对照表 9-16 行）；**刻意扩展** `closed: Flow<RealtimeWsClose>` 携带关闭码（19-24 注释）；`send` 未连接抛 `IllegalStateException`（40-41）；`close` 缺省 1000（43-44）。`expect createPlatformRealtimeWsTransport`（65）。包装函数 `createRealtimeWsTransport()`（72-73）**生产无人调用**——component 直接调 expect 工厂并在自己那侧处理 mock（`AppRealtimeClientImpl.kt:661-665`）；注释 68-71 说明 core 不反向依赖组件层（§13.6）。

**与 component/realtime 的分工**：core 提供「票/同步 HTTP、帧 DTO、传输抽象、平台 actual」；`component/realtime` 的 `AppRealtimeClient` 负责重连状态机、心跳调度、seq 水位与 notifyId 去重环（ADR 0005 §2）以及 mock 网关实现（`MockRealtimeWsTransport.kt`）。

---

## 5. 类型/函数详解 —— networkKtorMain 与平台 actual

### 5.1 `KtorApiClient.kt`（android/ios/ohosArm64 共用）

- `Throwable.transportStatus()`（31-32，internal）：仅 `ResponseException` 携 HTTP 状态——`expectSuccess` 默认 true 时非 2xx 走这条路。
- `SSE_READ_WINDOW_BYTES = 8192`（35）：只影响分片粒度，帧边界由 `SseParser` 自切。
- `getRaw/getAbsoluteRaw/postRaw/postForm`（43-88）：每个都 `try { … bodyAsText() } catch (t) { throw NetworkError.Transport(msg, t.transportStatus()) }`。`postForm`（71-88）用 `Parameters.build + formUrlEncode` 手拼 urlencoded 并覆盖 contentType——但注意 **`expectSuccess` 未关**，非 2xx 会抛而非返回 `HttpTextResponse(4xx)`，与 ohosX64 的实现不对称（§13.4）。
- `openSseTextStream`（106-156）——本类最重函数，逐步：
  1. `preparePost`：`expectSuccess=false`（112，状态码降级为判定输入而非异常）、`Accept: text/event-stream`（113）、追加 headers 可覆盖 Accept（114）、JSON content-type（115）、`timeout{ request/socket = INFINITE, connect 仍 30s }`（116-119，注释 96-99 对应 Dart `receiveTimeout: Duration.zero`）、`setBody(body)`（120）。
  2. `statement.execute { response -> … }`（123）：Ktor 流式唯一正确出口（100-102 注释：`execute()` 不带 block 会整包读进内存）。
  3. 分片流（125-137）：`bodyAsChannel()` + 8 KiB 窗口 `readAvailable` + `Utf8ChunkDecoder` 增量解码，EOF 后 `decoder.finish()` 收尾（135-136）。
  4. `SseOpenGate`（138-143）判定并放行/抛前流错误；`emitAll(gate.eventStreamText())`（145）把流接到外冷流。
  5. catch（147-155）：`CancellationException`（用户停止生成）与已类型化的 `SseException` 原样传播；其余一切折算 `SseException(error=connection_error, statusCode=transportStatus)`——「失败一律 SSE 词汇表，不混进 NetworkError.Transport」（104 行注释）。
- `getApi/postApi`（158-198）与 `sendApi`（242-265）：拿到 response 后把 `bodyAsText + response.status.value` 交给 `ApiEnvelopeParser.parse`（170-176/258-264）并注入 `onTokenExpired/businessHandlers`。`putRaw/patchRaw/deleteRaw`（200-207）走 `sendRaw`（227-240）。

### 5.2 `HttpClientDefaults.kt`

见 §3.3；补充：`configureDemoHttpClient()`（49-51）只是 `installDemoDefaults()` 的别名，三个 Ktor actual 工厂统一调用它。

### 5.3 `KtorRealtimeWsTransport.kt`（24-180）

- 双 `MutableSharedFlow`：`_inbound`（replay 0 / 64 buffer / SUSPEND，41-45）、`_closed`（8 buffer / **DROP_OLDEST**，46-50——关闭事件不许丢新留旧的反向错）。
- `connect(url)`（63-77）：**先 `close()` 旧连接**（64，Dart 同款）→ `webSocketSession{url(url)}`；取消异常原样传播（67-68），其余包 `NetworkError.Transport("WS 拨号失败…", ResponseException 状态)`（69-74）；成功后用 session 自身作用域 `launch { readLoop }`（76）。
- `send`（79-82）：`Frame.Text(envelope.asWsText())`；`close(code,reason)`（84-95）：先清 `session` 再 cancel readJob、`CloseReason(code ?: 1000)`、`runCatching` 吞关闭异常。
- `readLoop`（108-141）：Text 帧解析失败**只 println 丢帧不断流**（114-119，Dart :29-34 同语义）；Close 帧 → `RealtimeWsClose`（121-124，空载荷按 1005，144-150）；循环正常结束/异常后 `finally` 里发一次兜底关闭事件（134-140，`awaitedClose` 153-159 读 `closeReason` Deferred）——状态机感知掉线的唯一途径。
- `dispose`（97-102）：连同 `http.close()`。
- `installRealtimeWebSockets`（174-180）：**刻意不复用** `installDemoDefaults`（167-173 注释：HTTP baseUrl/内容协商/30s socket 超时都会污染长连接；鉴权走 `auth` 帧携 ticket 而非 HTTP 头）；`pingIntervalMillis = 0L`（178，协议层 ping 关闭，心跳归 `HeartbeatScheduler`）。

### 5.4 androidMain actuals

- `HttpClientFactory.android.kt:6-13`：`HttpClient(OkHttp) { configureDemoHttpClient() }`。
- `BackendLocalhost.android.kt:3-9`：`isEmulator()`（11-18，Build.FINGERPRINT/MODEL 关键词法）→ `resolveLoopbackHostMapping` → 命中则改写；真机未注入时 `warnRealDeviceWithoutOverride`（23-31，进程内**只 println 一次**、不阻断）。
- `NetworkBootstrap.android.kt:3-13`：clientType `"android"`、**`platformIsReleaseBuild()` 恒 false**、版本硬编码 `"1.0"/"1"`——坑 §13.10。
- `RealtimeWsTransportFactory.android.kt:7-8`：OkHttp 引擎独立 client 实例。

### 5.5 iosMain actuals

`HttpClientFactory.ios.kt:6-13`（Darwin）；`BackendLocalhost.ios.kt:7-10`（模拟器与后端同机，不注入就不改）；`NetworkBootstrap.ios.kt:7-20`（`platformClientType()="ios"`、`Platform.isDebugBinary` 取反作 isRelease、`NSBundle` 取 CFBundleShortVersionString/CFBundleVersion）；`RealtimeWsTransportFactory.ios.kt:7-8`。注意 iosMain 是三个 iOS target 的共享层（根 `build.gradle.kts:120-122`），**iosX64 也走完整 Ktor 路径**，没有任何降级。

### 5.6 ohosMain / ohosArm64Main actuals

`BackendLocalhost.ohos.kt:7-10`（两架构共享：注入优先，`10.0.2.2` 不适用）；`NetworkBootstrap.ohos.kt:3-13`（clientType `"ohos"`、isRelease 恒 false、版本 null——`Client-Version` 头在 OHOS 上**不发送**，对照 `DemoRequestHeaders.kt:7-8`）。`HttpClientFactory.ohosArm64.kt:6-14`（CIO）与 `RealtimeWsTransportFactory.ohosArm64.kt:10-11`——ohosArm64 与 android/ios 走**同一份** networkKtorMain 代码。

### 5.7 ohosX64Main（回落 lane 全解）

- `HttpClientFactory.ohosX64.kt:6-9`：`createPlatformApiClient → OhosApiClient(onTokenExpired, businessHandlers)`。
- `OhosApiClient.kt:7-217`：与 Ktor 版的**行为对照**——
  - `*Raw` 系（12-27, 110-145）：`statusCode >= 400` 时 `throw NetworkError.Transport("… HTTP $status")`（**不带 httpStatus 字段**，16/24/39…行——与 Ktor lane 的 `Transport(msg, status)` 相比丢状态）；
  - `postForm`（44-57）：非 2xx **正常返回** `HttpTextResponse`（与 Ktor lane 不对称，§13.4）；percent 编码用本地 `encodeComponent`（205-216，unreserved 集 `A-Za-z0-9-_.~`，与 §encodeQuery 同实现）；
  - `*Api` 系（73-203）：任何状态都喂 `ApiEnvelopeParser.parse(raw, …, httpStatus)` —— **ohosX64 是全仓唯一在真流量中走通 parser 的 `httpStatus=401/403` 分支的平台**；
  - `openSseTextStream`（64-71）：抛 `SseStreamingUnsupportedException("ohosX64/net_http", path)`，注释 59-63 明确 GAP(R5) 且提醒 ohosArm64 不落此类。
- `OhosHttpTransport.kt:31-123`：`flightMutex` + 单一 `inFlightRef: StableRef<Holder>`（32-35）——net_http 回调**无 user-data**（29-30 注释），所以**所有请求全局串行**（73 行 `withLock`），`suspendCancellableCoroutine` 挂起等回调（74-90），取消时 `cancelRequest + dispose`（84-88）。`buildBusinessUrl`（93-102）手工拼 `effectiveBaseUrl + path + ?query`（对 Ktor 版 `defaultRequest.url` 的复刻）。`completeFlight/currentHolder`（117-122）供回调侧收尾。
- `OhosHttpTransport.kt:125-291`（`OhosHttpRequestHolder` 类，同文件）：`start()`（139-219）逐结构体填 `Demo_HttpRequestOptions`（nativeHeap alloc + pin 字节数组，166-199），method 标签映射（168-174），timeout 秒化 `TIMEOUT_MS/1000` 下限 1s（165），HTTP/1.1 固定（182），`events` 回调组**全部置 null**（203-210，即不订阅增量数据——这就是无法流式的底层原因）；`responseCallbackStatic` 是 `staticCFunction`（286-289），只能经全局 `currentHolder()` 找回协程续体；`onResponse`（260-276）读整包 body、销毁原生 response 后 `complete`；`fail`（228-234）与 `complete` 都先 `cleanup()`（244-258，headers/pin/options 逐一释放）+ `completeFlight()`。`errCode != 0u` ⇒ `Transport("OHOS HTTP error: $errCode")`（261-264）。
- `RealtimeWsTransportFactory.ohosX64.kt:17-19`：工厂函数体即 `throw RealtimeWsError.Unsupported()`——**构造期失败**，上层状态机据此记 `ws_connect_fail` 停重连（注释 10-12）。

---

## 6. 一次请求的完整生命周期

以 `api.postApi<Req,Res>("/api/v1/x", req)` 为例（Ktor lane）：

| 步 | 位置 | 内容 |
|---|---|---|
| 0 | `ApiClient.kt:101-104` | reified 扩展：`NetworkJson.encodeToString(req)`（explicitNulls=false，null 键被裁）→ `postApiDecoded`（92-95）→ 字符串版 `postApi` |
| 1 | `NetworkFacade.kt:34` | 业务从 `api()` 拿到的若是刷新启用态则是 `AuthRefreshingApiClient`（TokenRefresh.kt:40） |
| 2 | `TokenRefresh.kt:81-85` | `withRefresh(path) { delegate.postApi(...) }` |
| 3 | `KtorApiClient.kt:179-190` | `http.post(path.trimStart('/')) { if (body.isNotEmpty()) setBody(body) }`；插件链：`DemoDynamicHeaders` 注入 Client-*/Bearer（HttpClientDefaults.kt:16-23 → DemoRequestHeaders.kt:5-13）、defaultRequest 补绝对 URL 与 JSON content-type（43-46）、30s 三段超时（29-33） |
| 3a | **失败分支（传输层）** | 连接超时/非 2xx ⇒ `ResponseException` ⇒ `KtorApiClient.kt:188-190` 转 `NetworkError.Transport(msg, httpStatus)` **向上抛**；`withRefresh` 的 `call()`（TokenRefresh.kt:137）不捕获 ⇒ **HTTP 401 在 Ktor lane 不会触发刷新重试**（§13.3）；`onTokenExpired` 也不会被调用（它只在 parser 内部触发） |
| 4 | `KtorApiClient.kt:191-197` | 2xx 响应：`bodyAsText()` + `response.status.value` 交 `ApiEnvelopeParser.parse`（并带上 onTokenExpired/businessHandlers——刷新启用时前者已被 `NetworkFacade.kt:70` 摘走） |
| 5 | `ApiEnvelopeParser.kt:20-55` | 解码信封（字符串 code 也吃，ApiEnvelope.kt:36-52）；非信封体→`-1`/「响应格式错误」（41-49）；缺 code→`PASSTHROUGH`（51-53） |
| 6 | `ApiEnvelopeParser.kt:58-84` | code 分派：200+`{"code":401}`（BFF 以 HTTP 200 携带业务 401 的场景）→ `TokenExpired`；100/301/429/500/403 → 业务回调（**均不裁剪响应体**） |
| 7 | `ApiEnvelopeParser.kt:101-104` | `decodeEnvelopeData<Res>`：`runCatching` 类型解码，失败静默 null |
| 8 | `TokenRefresh.kt:137-156` | 回到装饰器：`policy.decide(httpStatus, code, message)`（生产实现 `AuthTokenRefresh.kt:31-36`，10021/10022→ForceLogout、10023/401→Refresh、403→PassThrough）；Refresh⇒`refresher.refresh(rawClient)` 成功后**再走一遍步 2-7**（仅一次），失败原样返回首次应答；ForceLogout⇒`onSessionCleared()`（即 `AuthTokenRefresh.handleRefreshExhausted`，`NetworkFacade.kt:60` 注入） |
| 9 | 调用方 | `ApiResponse.isSuccess`（`ApiResponse.kt:21`，code∈{0,200}）决定消费 `data` |

**ohosX64 编号生命周期**（同一 `postApi` 调用；步 0-2 与主表完全相同——装饰器与 lane 无关，差异全部在引擎腿，替换主表步 3-4）：

| 步 | 位置 | 内容 |
|---|---|---|
| X1 | `OhosApiClient.kt:89-108` | `postApi` 先 `buildBusinessUrl(path, emptyMap())`（`OhosHttpTransport.kt:93-102`）手工拼 `effectiveBaseUrl + '/' + path + ?query`，query 逐字符 percent 编码（`encodeQuery`，104-115，unreserved 集 `A-Za-z0-9-_.~`）；请求头每请求手搓 `DemoRequestHeaders.forUrl(url)`（`OhosApiClient.kt:97`，与 Ktor lane 同源） |
| X2 | `OhosHttpTransport.kt:73-90` | `execute`：`flightMutex.withLock`（:73，**全局串行单飞**）→ `suspendCancellableCoroutine`（:74）→ `StableRef.create(holder)`（:83）→ 挂取消钩子 `invokeOnCancellation { cancelRequest; dispose; inFlightRef=null }`（:84-88）→ `holder.start()`（:89） |
| X3 | `OhosHttpTransport.kt:139-219` | `start()` 装配：CreateRequest/CreateHeaders 返 null 即 `fail`（141-152）；非 GET 且有 body 且未显式给 Content-Type 时补 `contentType ?: "application/json"`（156-160）；逐条 `SetHeaderValue`（161-163）；read/connect 超时 `TIMEOUT_MS/1000`、下限 1s（165，30s 见 `NetworkConfig.kt:7`）；固定 HTTP/1.1（182）；`httpProxy/caPath/clientCert/dnsOverHttps` 全 null（183-188，无代理/自定义 CA/DoH/mTLS）、`resumeFrom/resumeTo=0` 无断点续传（185-186）；body 字节 pin 进 native（191-195）；**events 六个回调槽全置 null**（203-210）= 不订阅任何增量数据（SSE 不存在的底层根因）；`Demo_Http_Request` 非 0 ⇒ `fail`（212-215）；装配期任何 Throwable ⇒ `fail`（216-218） |
| X4 | `OhosHttpTransport.kt:286-289` | 原生回调经 `staticCFunction`（回调**不携带 user-data**，:29-30 注释）只能回查全局 `currentHolder()`（:122）派发；若取消先于回调到达，holder 已被 :84-88 清空 ⇒ 回调静默丢弃、无泄漏也无人再等（§13.12） |
| X5a | **失败分支** | `onResponse`（260-276）判 `response == null \|\| errCode != 0u` ⇒ `fail("OHOS HTTP error: $errCode")`（261-264）：`cleanup()`（244-258）+ `completeFlight()`（117-120）+ `resumeWithException(NetworkError.Transport(msg))`（231-233）——**不带 httpStatus 字段**；原生超时/失败同样以非 0 `errCode`（或 null response）到达此分支，具体码值语义由 libnet_http 决定，本册不臆测 |
| X5b | **成功分支** | `readBuffer(resp.body)` 整包取 body（266、278-283）、`resp.responseCode` 取状态（267）、调用原生 `destroyResponse`（268-274）、`complete`（275）：`cleanup` + `completeFlight` + `continuation.resume(OhosHttpResult)`（236-242） |
| X6 | `OhosApiClient.kt:101-107` | `*Api` 系五方法（getApi 73-87、postApi、putApi 147-166、patchApi 168-187、deleteApi 189-203）**不看状态码**，`body + statusCode` 原样喂 `ApiEnvelopeParser.parse(httpStatus=…)` —— parser 的 :25-39（HTTP 401 OAuth 体）与 :79-83（HTTP 403）分支**只有这条 lane 真实命中**；对照：`*Raw` 系 ≥400 直接抛 `Transport`（15-17、38-40、118-120、132-134、141-143，不进 parser）、`postForm` 原样返回（44-57） |
| X7 | 主表步 5-9 | 之后与 Ktor lane 完全同路（信封解析/code 分派/`decodeEnvelopeData`/装饰器决策/至多重试一次）；重试 = 重跑 X1-X6，**再次排 `flightMutex` 的队** |

SSE 生命周期：`AiSseApi.streamCompletion`（`AiSseApi.kt:51-67`）组装 camelCase 请求体（74-85，null/空串 id 不发）→ `openSseTextStream`（`KtorApiClient.kt:106-156` §5.1）→ 每分片 `SseParser.feed`（`SseParser.kt:34-57`）→ 每帧 `toAiStreamEventOrNull`（`AiSseDecoder.kt:36-62`）→ EOF `flush`（`SseApi` 63-66，`SseParser.kt:65-71`）；前流失败在闸门抛 `SseException`（`Sse.kt:171-174`）且**绝不喂解析器**；ohosX64 在 `OhosApiClient.kt:64-71` 抛 `streaming_unsupported`；装饰器只转发（`TokenRefresh.kt:121-125`）。

WS 生命周期（core 侧半程）：`fetchTicket`（`RealtimeWsTicketApi.kt:31-47`，postRaw 经**装饰后** client——`RealtimeWsTicketApi.kt:25` 默认 `NetworkFacade.api()`；但 `postRaw` 不在刷新动词面内，`TokenRefresh.kt:61-62` 直转发）→ `resolveRealtimeWsUrl`（81）→ `createPlatformRealtimeWsTransport()`（各 actual）→ `connect`（`KtorRealtimeWsTransport.kt:63-77`）→ `auth` 帧/心跳/重连归 `AppRealtimeClientImpl.kt`（component，:157,257,661-665）→ 掉线经 `closed`（`RealtimeWsTransport.kt:31-32`；`RealtimeWsClose.requiresNewTicket` 51-57）。

---

## 7. 三端差异矩阵

| 能力 | android | ios（X64/Arm64/SimArm64） | ohosArm64 | ohosX64 |
|---|---|---|---|---|
| HTTP client | Ktor+OkHttp（`HttpClientFactory.android.kt:10`） | Ktor+Darwin（`HttpClientFactory.ios.kt:10`） | Ktor+CIO（`HttpClientFactory.ohosArm64.kt:11`） | **net_http cinterop**（`OhosApiClient.kt:7`） |
| 信封解析/错误码 | `ApiEnvelopeParser` 同一份 | 同 | 同 | 同（且 `httpStatus≥400` 分支才真实生效） |
| 超时 | HttpTimeout 30s×3（`HttpClientDefaults.kt:29-33`） | 同 | 同 | options read/connect 30s（`OhosHttpTransport.kt:165`） |
| 请求日志 | Ktor Logging INFO→stdout | 同 | 同 | **无**（不经 Ktor 插件；仅手工 `DemoRequestHeaders.forUrl`） |
| 内容协商 | 装但基本不用（体恒 String） | 同 | 同 | n/a |
| 并发 | OkHttp/Darwin/CIO 原生并发 | 同 | 同 | **全局串行单飞**（`OhosHttpTransport.kt:32-35,73`） |
| SSE | 真流式（`KtorApiClient.kt:106-156`） | 同 | **真流式（同一条 Ktor 路）** | **不支持**：`SseStreamingUnsupportedException`（`OhosApiClient.kt:64-71`，GAP R5） |
| WS 传输 | KtorRealtimeWsTransport+OkHttp | +Darwin | +CIO | **不支持**：工厂抛 `RealtimeWsError.Unsupported`（`RealtimeWsTransportFactory.ohosX64.kt:17-19`） |
| loopback 改写 | 模拟器→`10.0.2.2`；真机告警一次（`BackendLocalhost.android.kt:3-31`） | 不映射（同机直达，`BackendLocalhost.ios.kt:7-10`） | 共享 `ohosMain`：仅注入生效（`BackendLocalhost.ohos.kt:7-10`） | 同左（同一 actual） |
| bootstrap 身份 | `"android"`/isRelease**恒 false**/版本 "1.0"（`NetworkBootstrap.android.kt:3-9`） | `"ios"`/`Platform.isDebugBinary`/Info.plist（`NetworkBootstrap.ios.kt:7-16`） | `"ohos"`/恒 false/版本 null（`NetworkBootstrap.ohos.kt:3-9`） | 同左（同一 actual） |
| commonTest | ✓ | ✓（各 iOS target） | ✓ | ✓（`RealtimeWsApiContractTest.kt:29` 注释点名可在含 ohosX64 的每个 target 编译） |
| Ktor 专项测试 | androidUnitTest 3 文件 | ✗ | ✗ | ✗ |

**不许说成一致能力**：ohosX64 相对其他三端缺 SSE、缺 WS、缺请求日志、缺并发、非 2xx 异常语义不同（§13.3/13.4）；ohosArm64 与 android/ios 功能面等价（HTTP/SSE/WS 全通，HTTPS gate 已过，`HttpClientFactory.ohosArm64.kt:6` 注释）。

---

## 8. 序列化与配置小结

| 主题 | 事实 | 证据 |
|---|---|---|
| 全局 Json | `ignoreUnknownKeys + isLenient + encodeDefaults + explicitNulls=false` | `NetworkJson.kt:6-11` |
| 显式 null 特例 | `NetworkJsonExplicitNulls`（`explicitNulls=true`）供「必须发 null」的端点（community posts 的 `video_url`、deal-invoices/used-car 的 `image_url`、short-videos 占位） | `NetworkJson.kt:13-24`；调用方 `HttpCommunityPostRepository.kt:165`、`ShortVideoRepository.kt:162`、`DealInvoiceApi.kt:105` |
| AI SSE 专用 Json | 仅 `ignoreUnknownKeys` 的私有实例 | `AiSseDecoder.kt:11-13` |
| 自定义 serializer | `FlexibleIntSerializer`（code 数字/字符串）、`RealtimeTextSerializer`/`RealtimeLongSerializer`/`RealtimeNullableLongSerializer`、`RealtimeEnvelopeSerializer`（JsonObject 委托、复刻 Dart 键省略） | `ApiEnvelope.kt:36-52`、`RealtimeJson.kt:88-133`、`RealtimeEnvelope.kt:115-133` |
| nullable/默认值策略 | 信封 DTO 全字段可空带默认；`data` 保持 `JsonElement` 由 `decodeEnvelopeData` reified 解；realtime 四必填键缺键抛（信封体必炸机制） | `ApiEnvelope.kt:23-31`、`ApiEnvelopeParser.kt:101-104`、`RealtimeWsTicketApi.kt:64-74` |
| 插件 | kotlinSerialization 在模块与根工程各 apply 一次 | `core/network/build.gradle.kts:10`、`build.gradle.kts:107` |

---

## 9. 测试地图（全部 13 个测试文件）

| 文件 | target 侧 | 锁定的引擎行为（代表用例） |
|---|---|---|
| `ApiEnvelopeParserTest.kt`（167 行） | commonTest | BFF 0 与 200 双成功码（22-33,13-19）；畸形 JSON→-1（36-42）；缺 code≠成功（44-50）；HTTP401 OAuth 体→TokenExpired+handler（81-93）；403 与 401 语义互斥三连（106-146）；字符串 code（96-101）；httpStatus 透传（149-154） |
| `AuthRefreshingApiClientTest.kt`（325 行） | commonTest | 401 刷新+重试一次（106-119）；10023 刷新（122-128）；并发两条 401 各自成功、装饰器不丢请求（131-205，注释 133：单飞去重归 feature 协调器）；登录路径豁免（208-220）；10021 清会话不刷新（223-237）；403 透传（240-254）；刷新失败回原始 401（257-267）；无 policy 全透传（270-276）；GET/PUT/PATCH/DELETE 动词面刷新（279-308）；`*Raw` 永不刷新（311-324） |
| `BackendHostPolicyTest.kt`（79 行） | commonTest | 注入优先（16-25）、模拟器 10.0.2.2（27-31）、iOS 保持 loopback（33-35）、**LanHost 恒空断言（43-47，现失败）**、只改 loopback（49-64）、effectiveBackendHost（66-78，现失败于 :73） |
| `ListPageTest.kt`（102 行） | commonTest | `list`+`totalPages`（26-37）、`page*size<total` 回退（39-45）、snake `total_pages`（47-52）、裸数组退化（54-62）、畸形→空页（64-70）、分页默认值（72-80）、queryOf 弃 null/空（82-88）、1-based vs 0-based（90-101） |
| `SseContractTest.kt`（245 行） | commonTest | 闸门四态（27-82）、fromPreStream 键优先级与空体（87-104）、defaultErrorCode 全表含 `http_200`（107-115）、unsupported 类型化（118-134）、装饰器转发不重放（137-148）、UTF-8 三分支（153-186） |
| `RealtimeConfigAndCloseCodeTest.kt`（112 行） | commonTest | 心跳/退避常量对 Dart（23-34）、路径与 platform（37-41）、关闭码表（44-51）、三码换票（54-61）、互踢与鉴权类区分（64-70）、topics 命名（82-88）、Unsupported 消息含 `ohosX64`+`WebSocket`（103-111） |
| `RealtimeEnvelopeTest.kt`（215 行） | commonTest | 全字段裸帧解析（24-39）、缺 type→unknown（42-51）、ts/seq 数字与字符串（54-72）、payload 非标量归空（75-86）、控制帧八类（89-98）、`eventName` 强类型（101-105）、toJson 条件键不带 raw（108-128）、asWsText 往返（131-150）、fromRawJson 丢畸形（153-158）、realtimeBool 只认真布尔（201-214，注明「曾为生产 bug」） |
| `RealtimeWsApiContractTest.kt`（284 行） | commonTest（**不依赖 Ktor，ohosX64 也可编译**，:29） | #97 固定体（96-108）、camelCase 解码（111-120）、宽容回退 `??''/??0`（123-132）、未知键忽略（135-140）、信封体拒绝（143-156）、空 token 先抛（159-167）、wsUrl 空→TicketInvalid（170-179）、Transport 原样传播（182-192）；#98 camel 请求（197-206）、events 解码（209-226）、手搓路径跳过非 object（246-265）、200 字符预览（268-274）、wsUrl 原样拨号（277-283） |
| `AiSseDecoderTest.kt`（159 行） | commonTest | 类型优先级/event 空缺丢弃（31-45）、error 三级回退（56-83）、非 object/畸形→null（86-93）、信封形态非流契约（111-115）、多行 data 拼 JSON（128-132）、全流顺序（135-158） |
| `SseParserTest.kt`（201 行） | commonTest | 三帧解析（19-38）、注释/keepalive（40-61）、多行 data（63-68）、跨块帧与空行（70-83）、CRLF/裸 CR/跨块 CR-LF（85-106）、单空格剥离（109-119）、无冒号字段（122-126）、空 event 值保持空（129-134）、id→默认 message（137-149）、retry 记录与拒非数字（152-165）、未知字段（168-170）、flush 丢半帧与复位（172-187）、空 chunk（190-194）、顺序保持（197-200） |
| `KtorApiClientMockTest.kt`（65 行） | androidUnitTest | Bearer 上线（35-48）、`cdn.demo.local` 跳鉴权（50-64） |
| `KtorApiClientVerbTest.kt`（69 行） | androidUnitTest | PUT/PATCH/DELETE 真动词 + BFF 0 判成功（34-53）、DELETE 带 query、pageQuery 编码（56-68） |
| `KtorSseTransportTest.kt`（176 行） | androidUnitTest | POST+Accept+JSON body 落线（46-74）、**服务端还在写时逐片增量**（77-112）、多字节切分不乱码（115-134）、401 JSON→SseException 非 Ktor 异常（137-153）、200 信封体不喂解析器（156-171） |

未覆盖清单（如实记录）：`OhosApiClient`/`OhosHttpTransport` 零测试；`KtorRealtimeWsTransport` 在 core 内零测试（由 `component/src/commonTest/.../AppRealtimeClientImplTest.kt` 用替身间接覆盖）；`NetworkFacade`/`NetworkConfig`/`applyNetworkBootstrap` 无直接单测；`SseStreamingUnsupportedException` 的 ohosX64 抛点仅被同型默认实现测试（`SseContractTest.kt:127-134`）镜像。

---

## 10. 真实状态与坑（不粉饰）

1. **LanHost 回归（可复现的测试失败）**：`LanHost.kt:18` 注释与文件名约束都说「恒为空、机器 IP 不入库」，但 :19 实值 `"192.168.2.175"` 是 commit `8e3027a`（"fix"，2026-10-22）把空串改回硬编码 IP 的结果。`BackendHostPolicyTest.kt:46`（`assertEquals("", LanHost.fallback)`）与 :73（注入空白→期望 effectiveBackendHost 为空）按静态阅读**必挂**；CI 跑 `./gradlew testDebugUnitTest`（`.github/workflows/ci.yml:80`）会命中（本册按任务要求未执行构建，此为代码阅读结论）。连带语义：任何未注入 host 的设备（含 OHOS/真机）`NetworkConfig.effectiveBackendHost`（`NetworkConfig.kt:26-27`）都会把这个私有 IP 当兜底注入点。
2. **`Sse.kt:14-15` 注释过期**：称 `SseParser`/`AiSseDecoder` 「住在 composeApp 的 core/sse」——实际它们就在本模块 `core/network/src/commonMain/.../core/sse/`（§2.2）。包名 `com.example.my_kmp_project.core.sse` 易加深误解。
3. **`expectSuccess` 未关 ⇒ Ktor lane 的 HTTP 401 绕过整套信封 401 机制**：`KtorApiClient` 的 `*Api` 方法（如 :163-169, :184-190）不关 expectSuccess，非 2xx 一律先抛 `NetworkError.Transport(msg, status)`，`ApiEnvelopeParser` 的 :25-39（HTTP 401 OAuth 体）与 :79-83（HTTP 403）分支在 android/ios/ohosArm64 的真实流量里**不可达**（只有 ohosX64 lane 和非 2xx 不抛的测试替身能触发）；`AuthRefreshingApiClient.withRefresh`（`TokenRefresh.kt:131-157`）不 catch 异常 ⇒ Ktor lane 上 HTTP 级 401 不会静默刷新，刷新只对「HTTP 200 + 信封 code=401/10023」生效。BFF 若真返回 HTTP 401，三端 KMP 与 ohosX64 行为分叉。**这是设计意图还是缺口，代码证据不足以裁决**——注释（如 `NetworkFacade.kt:52-56`）暗示想要 HTTP 401 参与刷新。
4. **`postForm` 两端不对称 + 三端丢状态**：Ktor lane 非 2xx 抛（`KtorApiClient.kt:71-88`），ohosX64 lane 原样返回 `HttpTextResponse(status,body)`（`OhosApiClient.kt:44-57`）——为 OAuth token 交换设计的返回类型，在 Ktor 上拿不到 4xx body。目前无生产调用者（见下条），属潜伏差异。另外 `OhosApiClient.kt:16` 等抛的 `Transport` 不带 `httpStatus`，与 `KtorApiClient.kt:49` 带状态的构造相比上游可读性降级。
5. **三个接口方法生产零调用**：`postForm`/`getAbsoluteRaw`/`getApiString`（`ApiClient.kt:22,25` + :148）全仓只有平台实现与测试替身引用（grep 证据：feature/component 命中全在 `src/commonTest`），接口仍是为 Dart 对等预留的面。
6. **`createRealtimeWsTransport()` 死代码**（`RealtimeWsTransport.kt:72-73`）：component 直接调 expect 工厂（`AppRealtimeClientImpl.kt:665`）；注释 68-71 与实现相互印证「core 不做 mock 选路」。
7. **`decodeEnvelopeData` 静默吞类型错误**（`ApiEnvelopeParser.kt:103` runCatching→null）：`isSuccess==true && data==null` 是合法组合，调用方无法区分「服务端 data 本就 null」与「DTO 字段类型不符」。端点册读者须知。
8. **日志级别与目标恒定**：Logging INFO 到 `println`（`HttpClientDefaults.kt:34-41`），Release 也打、OHOS 上走 stdout 而非 HiLog；`Utf8ChunkDecoder` 非法序列静默替换（设计如此，`Sse.kt:190-192`）。
9. **`retryMillis` 无消费者**（`SseParser.kt:30-31`）：解析了但全仓没人读；重连节奏完全由 `RealtimeConfig` 常量驱动。
10. **Android/OHOS 永远 Debug**：`NetworkBootstrap.android.kt:5`、`NetworkBootstrap.ohos.kt:5` 恒 false ⇒ `applyNetworkBootstrap`（`NetworkBootstrap.kt:15-18`）的 Release→Product 自动切换只对 iOS 生效；三端启动入口分别在 `MainActivity.kt:28`、`MainViewController.kt:18` 等、`MainArkUIViewController.kt:39` 调用该 bootstrap。
11. **DEV/TEST/STAGING 三环境同值**（`DemoApiHosts.kt:24-26`）：环境切换在本地演示场景是摆设，唯一实质分支是 Product；`environmentForHost` 甚至把 loopback 8080 归入 Test（49-51）。
12. **ohosX64 的全局串行**：`flightMutex` + 单 holder（`OhosHttpTransport.kt:32-35,73`）意味着首页并发多接口在 x64 模拟器上逐个排队，且任何取消路径 dispose `StableRef` 后再回调会走 `currentHolder()==null`（:122,287——回调丢失即协程靠 `invokeOnCancellation` 已恢复，无泄漏路径，但依赖「取消先于回调」假设）。
13. **业务码回调上线态悬空**：`NetworkFacade.setBusinessHandlers`（`NetworkFacade.kt:47-50`）全仓**无生产调用者**，`NetworkBusinessHandlers(...)` 构造只出现在测试（`ApiEnvelopeParserTest.kt:72`）。因此 100/301/429/500/403 五路回调（`ApiEnvelopeParser.kt:58-84`）在真实链路恒为 null、**永不触发**——Flutter d7876a1 的「403 无权限提示」对等只是预留接口（响应仍原样返回 code=403，feature 可自行判断，但没有全局 UI 钩子）。同类小项：internal 读取器 `currentTokenExpiredHandler`/`currentBusinessHandlers`（`NetworkFacade.kt:86-88`）全仓零调用者（§13.6 同型死代码）。

### 未验证疑点（返回时汇报）

- 疑点 A（=坑 3）：Ktor lane HTTP 401 抛异常绕过刷新装饰器是有意（BFF 恒 200+code）还是缺口，需与 `14` 册/契约表交叉确认。
- 疑点 B（=坑 1）：`LanHost.kt:19` 硬编码 IP 是回归还是故意私配；对应两测试当前红（静态判定）。
- 疑点 C：`net_http.def` 的 `libraryPaths` 为相对路径（相对模块目录），在 CI 无 OHOS sysroot 时 `core/network/build.gradle.kts:15-23` 回退链是否总能命中，未实机验证。

---

## 11. 与相邻文档的边界

| 主题 | 归本册 | 归别册 |
|---|---|---|
| `RealtimeWsTicketApi.kt` / `RealtimeWsSyncApi.kt` | ✔（WS 基础件的 HTTP 腿） | 14 册只列名 |
| feature 层 `*Api.kt`（`AiSseApi.kt` 等 24 个） | 仅作为调用方引用 | 14 册 |
| `AppRealtimeClient` 状态机 / 心跳 / seq 水位 / mock 网关 | ✗ | 15 册（component 端口） |
| 重连退避常量的消费逻辑 | 只给常量定义 | 15 册 |
| RouteHost / AppContainer 装配 | 仅 `AppContainer.kt:31` 一处引用 | 22 册 |

（本册 11 个 `##` 小节；文件清单 62 kt + build 脚本 + cinterop 资产全覆盖。）

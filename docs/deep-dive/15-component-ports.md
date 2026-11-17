# 15 · component —— ③ 垂直端口层（auth / chat / pay / push / realtime / routing / webview）

> 范围：`:component` 模块全部 41 个可验收文件（40 个 `.kt` + `component/build.gradle.kts`）。
> 所有结论一律以「仓库相对路径:行号」为证；凡代码里没有的行为，本文不写。
> 状态口径（真实实现 / stub / mock / 未接线）见第 5 节，realtime 专题见第 4 节。

---

## 1. 端口层为什么存在：依赖实证

### 1.1 分层位置

`AGENTS.md:34` 把 `component/` 定义为「③ vertical ports: pay / push / chat / webview / auth / routing / realtime (`:component`)」，位于 ④ `core/*`（横向公共）与 ② `feature/*`（17 个业务模块）之间，① `composeApp/` 只做装配。

它存在的唯一理由是**分层门禁**：`feature` 之间不得互相 import、也不得声明兄弟 feature 的模块依赖，而「聊天要弹登录页」「首页要渲染 mine 的弹窗」「会员页要调支付通道」这类需求天然跨 feature。
于是把这些**跨垂直能力的契约**下沉成端口，由 `:component` 定义接口、由 ① 装配层注入实现。

门禁本身是一个 shell 脚本，三条轴：

| 轴 | 拦什么 | 证据 |
|---|---|---|
| 1 | `feature` 源码 import 兄弟 `feature.*` | `check-layer-deps.sh:18-28` |
| 2 | `core/`、`component/` 向上 import `feature.` / `app.` | `check-layer-deps.sh:30-38` |
| 3 / 3b / 3c | build 脚本里出现 `projects.feature.`（feature 之间、core→feature、**component→feature**） | `check-layer-deps.sh:40-59`（3c 专门扫 `component` 目录：`:54-59`） |

轴 2 的扫描根把 `component` 单独列进去了（`check-layer-deps.sh:37`），所以 `:component` 是**被强制的低层**，这也解释了为什么 realtime 那一堆接口要靠注入而不是直接读全局登录态（见 4.8）。

### 1.2 `component/build.gradle.kts` 的真实内容

`:component` 不在根 `build.gradle.kts` 的 `subprojects {}` 共享接线命中范围内（`component/build.gradle.kts:6-8` 自己写明了这一点），所以 target 集、android 基础配置、公共依赖都在本模块重复声明一遍。

依赖面只有 4 个仓库模块 + compose runtime + 两个 kotlinx 库（`component/build.gradle.kts:37-45`）：

| 依赖 | 形式 | 实际提供 |
|---|---|---|
| `projects.core.network` | `api` | `ApiClient`、`NetworkFacade`、`NetworkError`、整个 `core.network.realtime.*`（换票 / sync / 传输 / 配置） |
| `projects.core.base` | `api` | `AppUuid`（出站帧 id / 队列 messageId 的唯一来源） |
| `projects.core.account` | `api` | `AccountFacade`（auth 端口唯一的会话读写口） |
| `projects.core.router` | `api` | `MainTab` / `AppRoutes` / `DeepLinkRouter` |
| `compose.runtime` | `api` | `mutableStateOf`（`AuthGate`、`AuthSessionState` 的 Compose 可观察字段） |
| `kotlinx.coroutines.core`、`kotlinx.serialization.json` | `implementation` | Flow / JsonObject |
| `kotlin.test` | commonTest `implementation` | 唯一测试依赖 —— **没有** `kotlinx-coroutines-test`（见 4.9） |

注意这 4 个都是 `api` 而非 `implementation`：`:component` 把 core 的类型**透传**给下游。
消费方因此只需声明一条 `api(projects.component)`：
`feature/auth/build.gradle.kts:21`、`feature/chat:20`、`feature/commerce:20`、`feature/content:14`、`feature/friend:19`、`feature/home:30`、`feature/mine:17`、`feature/shell:18`；
`composeApp/build.gradle.kts:121` 用的是 `implementation(projects.component)`（装配层不需要再往下透传）。

其余构建事实：`-PandroidOnly=true` 时跳过全部 native target（`component/build.gradle.kts:17`、`:27-34`）；库模块一律不产 framework/sharedLib（`:28` 注释）；`iosMain`/`ohosMain` 是 `maybeCreate` 手工挂上去的中间 source set（`:49-61`）；
android namespace 为 `com.example.my_kmp_project.component`（`:66`）；模块在 `settings.gradle.kts:34` 注册为 `include(":component")`。

### 1.3 与 ADR 0005 的关系

`docs/adr/0005-three-long-lived-channels-separate-transports.md` 定了三条长连各归各的传输：
AI 走 SSE、全局通知/补投走 WS（`:21-22`）、聊天只走 IM 端口（`:23`），且「三条通道的失败语义不许互相翻译」（`:24`）。
落到本模块的就是 `component/realtime`（WS 那一条的全部状态机）与 `component/chat`（IM 那一条的端口），
`docs/adr/0005-three-long-lived-channels-separate-transports.md:32` 还写明门禁可以静态检查「`feature/ai` 里出现 `AppRealtimeClient` / `RealtimeWsTransport` 引用即为违规」。

---

## 2. 文件清单（41 个，含行数与关键行号）

路径前缀省略：`commonMain` = `component/src/commonMain/kotlin/com.example.my_kmp_project/component`，`commonTest` = `component/src/commonTest/kotlin/com.example.my_kmp_project/component/realtime`。

### 2.1 构建与 commonMain（25 个）

| 路径 | 行数 | 作用 | 关键行号 |
|---|---|---|---|
| `component/build.gradle.kts` | 76 | 模块自身接线（见 1.2） | `:1-8` 分层声明、`:37-45` api 面、`:49-61` 中间 source set |
| `commonMain/auth/AuthGate.kt` | 56 | 软鉴权闸门：哪些 tab 要登录 + 待恢复 tab | `:14` object、`:22-31` requiresAuth、`:46-50` consumePending |
| `commonMain/auth/AuthSessionState.kt` | 49 | 会话响应式唯一真值（ADR 0001） | `:18` object、`:19-22` Snapshot、`:34-41` sync/clearLocal、`:43-47` publish |
| `commonMain/auth/AuthSessionActions.kt` | 18 | 跨 feature 的「本地登出」事务 | `:11-17` |
| `commonMain/chat/ChatPort.kt` | 17 | IM SDK 端口 + stub 实现 | `:7-10` 接口、`:12-16` `StubChatPort` |
| `commonMain/chat/ImEngine.kt` | 544 | 会话/消息模型 + **Mock** 引擎 + 深链解析 | `:17-36` 接口、`:145-147` 单例、`:149-453` MockImEngine、`:470-501` 深链 |
| `commonMain/pay/PayGateway.kt` | 131 | 支付端口 + 结果代数 + 沙箱适配器 + 带旗标的网关 | `:4-7` 接口、`:14-27` PayResult、`:48-52` 旗标、`:64-69` 沙箱、`:83-94` 注册表、`:101-130` FlaggedPayGateway |
| `commonMain/push/PushBridge.kt` | 105 | 推送端口 + stub 桥 + 厂商 SDK 缝隙 | `:19-35` 接口、`:37-40` PushRegistration、`:46-90` StubPushBridge、`:99-104` expect object |
| `commonMain/push/PushRegistrationApi.kt` | 117 | `POST /api/v1/push/devices` 上报（对齐 Dart） | `:35-72` report、`:76` PATH、`:96-101` detectPlatform、`:109-116` 埋点缝隙 |
| `commonMain/realtime/AppRealtimeClient.kt` | 137 | 客户端契约 + 4 个注入缝隙 | `:18-75` 接口、`:87-91` SessionPort、`:97-106` SessionGuard、`:114-116` NotifyHandler、`:125-136` NetworkMonitor |
| `commonMain/realtime/AppRealtimeClientImpl.kt` | 667 | **状态机本体**（第 4 节全篇围绕它） | `:50-70` 构造、`:137-222` connectInternal、`:270-307` onEnvelope、`:378-406` handleTransportLost、`:658-666` 传输选路 |
| `commonMain/realtime/HeartbeatScheduler.kt` | 93 | 应用层 ping/pong 心跳 | `:37-50` start/stop、`:53-59` onPong、`:70-91` firePing |
| `commonMain/realtime/MockRealtimeWsTransport.kt` | 283 | 进程内 mock 网关（模拟协议） | `:79-125` 出站应答表、`:147-150` simulateClose、`:194-251` pushTick |
| `commonMain/realtime/RealtimeAuthFailure.kt` | 119 | 鉴权硬失败判定（401 停 / 403 不停） | `:25-76` 纯规则、`:82-114` 默认策略、`:117-118` 顶层入口 |
| `commonMain/realtime/RealtimeConnectionState.kt` | 35 | 7 态枚举 + `isActive` + 中文 `label` | `:10-18`、`:21-22`、`:25-34` |
| `commonMain/realtime/RealtimeInboundRouter.kt` | 80 | topic/event 分发（懒注册 + 同步投递） | `:34-38` watch*、`:44-52` dispatch、`:66-70` 频道参数 |
| `commonMain/realtime/RealtimeOutboundQueue.kt` | 193 | 离线出站队列（表缝隙 + depth 口径） | `:25-58` 行/状态/缝隙、`:61-86` InMemory、`:132-186` 队列本体 |
| `commonMain/realtime/RealtimeScheduler.kt` | 62 | 时间缝隙：Clock / Ticker / 生命周期枚举 | `:18-26`、`:40-55`、`:61` |
| `commonMain/realtime/RealtimeSeqStore.kt` | 152 | seq 水位 + notifyId 去重环 + KV 缝隙 | `:23-40` RealtimeKeyValueStore/InMemory、`:59-93` SeqStore、`:107-151` NotifyDedupStore |
| `commonMain/realtime/RealtimeStartupGate.kt` | 194 | 启动闸门（隐私/登录/活跃三道检查 + 两个超时） | `:59-100` tryConnectIfReady、`:103-138` 五个链路口、`:153-181` 两个等待 |
| `commonMain/realtime/RealtimeTelemetry.kt` | 87 | metric/error/trace 三入口 + sink 缝隙 | `:21-37`、`:45-85` |
| `commonMain/realtime/ReconnectPolicy.kt` | 52 | 指数退避 + 抖动 + 地板 | `:38-46` nextDelayMs、`:49` MAX_EXPONENT |
| `commonMain/routing/CrossFeatureScreenPort.kt` | 117 | 跨 feature 屏幕注入端口 + 可设置持有者 | `:18-49` 接口、`:55-68` install/require、`:70-115` 门面 |
| `commonMain/webview/WebViewComponent.kt` | 33 | WebView 归属标记 + 离线 fixture URL | `:11-13` internal object、`:20-32` OfflineWebFixtureUrl |

### 2.2 平台 actual（6 个）

| 路径 | 行数 | 实际内容 |
|---|---|---|
| `component/src/androidMain/…/pay/PlatformPayAdapters.android.kt` | 20 | 两个函数体只有一行 `return null`（`:7-10`、`:16-19`），上面各挂一条 `TODO(pay)` |
| `component/src/iosMain/…/pay/PlatformPayAdapters.ios.kt` | 8 | `:4` 与 `:7` 两个 actual 直接 `= null` |
| `component/src/ohosMain/…/pay/PlatformPayAdapters.ohos.kt` | 10 | `:7` 与 `:9` 两个 actual 直接 `= null` |
| `component/src/androidMain/…/push/PlatformPushSdk.android.kt` | 14 | `:10` `"android"`；`:12` `fetchRegistrationToken(): String? = null` |
| `component/src/iosMain/…/push/PlatformPushSdk.ios.kt` | 14 | `:10` `"ios"`；`:12` `= null` |
| `component/src/ohosMain/…/push/PlatformPushSdk.ohos.kt` | 14 | `:10` `"ohos"`；`:12` `= null` |

### 2.3 commonTest（10 个，全部属于 realtime lane）

| 路径（`commonTest/`） | 行数 | 钉住的语义 |
|---|---|---|
| `RealtimeTestDoubles.kt` | 484 | 12 个假件 + 3 个报文夹具（不是测试类，被其余 9 个测试共享） |
| `AppRealtimeClientImplTest.kt` | 708 | 状态机决策：阶梯、auth 帧、sync 补投、ack、关闭码、闩、鉴权停止 |
| `RealtimeStartupGateTest.kt` | 273 | 闸门检查顺序、两个超时只打日志、四个链路口的下发 |
| `MockRealtimeWsTransportTest.kt` | 365 | 模拟协议逐帧、`seq++` 在 topic 循环体内、closed 通道不外泄 |
| `RealtimeOutboundQueueTest.kt` | 232 | `depth` 三处口径、pending 视图、SQL 冲突替换语义 |
| `RealtimeSeqStoreTest.kt` | 236 | 水位严格递增、脏值归零、去重环截断与容错 |
| `HeartbeatSchedulerTest.kt` | 209 | 不等 pong 也排下一次、id 匹配才归零、两拍致命 |
| `RealtimeInboundRouterTest.kt` | 191 | 懒注册丢弃、控制帧不进业务流、下游过滤、dispose 终态 |
| `RealtimeAuthFailureTest.kt` | 164 | 401/403 结论、业务码、文案兜底的门控条件 |
| `RealtimeStateAndTelemetryTest.kt` | 141 | `isActive` 只两个、`label` 逐字、payload 键序 |

---

## 3. 逐端口：public 类型与函数

> 「实现者」列写**这个仓库里真实存在**的实现；「消费者」列写调用点的 `文件:行号`。

### 3.1 `auth/` —— 三个 object 构成一套「软鉴权」契约

| 成员 | 签名与语义 | 副作用 | 实现者 / 消费者 |
|---|---|---|---|
| `AuthGate.pendingTab` | `var pendingTab: MainTab?`，`mutableStateOf` 承载（`AuthGate.kt:16`） | 写它即触发 Compose 重组 | 写：`SoftAuthPresenter.kt:45`、`AppShell.kt:294`/`:311`；读：`SoftAuthPresenter.kt:70` |
| `AuthGate.requiresAuth(tab: MainTab)` | `= requiresAuth(tab.name) \|\| requiresAuthRoute(tab.route)`（`:22-23`） | 无 | `SoftAuthPresenter.kt:44`、`AppShell.kt:293`/`:310` |
| `AuthGate.requiresAuth(String)` | 去空白、去前导 `/`、转小写后匹配 `chat` / `chat/…` / `community` / `community/…`（`:25-31`） | 无 | 上一条的字符串重载 |
| `AuthGate.requiresAuthRoute` | **private**，按 `/chat`、`/community` 前缀判定（`:33-39`） | 无 | 仅 `:23` |
| `AuthGate.rememberPending(tab)` | 直接赋值（`:41-43`） | 置 pendingTab | `SoftAuthPresenter.kt:45`、`AppShell.kt:294`/`:311` |
| `AuthGate.consumePending()` | 取出并清空，返回 `MainTab?`（`:46-50`） | 清 pendingTab | `AppShell.kt:326`、`SoftAuthPresenter.kt:55` |
| `AuthGate.clearPending()` | 置 null（`:52-54`） | 清 pendingTab | `AuthSessionActions.kt:15`、`AppShell.kt:320`、`AuthTokenRefresh.kt:103`、`AuthRepository.kt:273`/`:286` |
| `AuthSessionState.Snapshot` | `data class(isLoggedIn=false, displayName=null)`（`AuthSessionState.kt:19-22`） | — | `snapshot` 的类型 |
| `AuthSessionState.snapshot` | `StateFlow<Snapshot>`（`:24-25`） | — | 新代码优先读这里（`:15-16` KDoc） |
| `AuthSessionState.isLoggedIn` / `.displayName` | `mutableStateOf` + `private set`（`:27-31`）——迁移期遗留口 | — | `SoftAuthPresenter.kt:17`/`:69` |
| `AuthSessionState.sync()` | `AccountFacade.current()` 拉一次再 `publish`（`:34-37`） | 写三处状态 | `AuthSessionBootstrap.kt:21`、`AuthRepository.kt:380`、`AuthBridge.kt:57`、`AppShell.kt:305` |
| `AuthSessionState.clearLocal()` | `publish(false, null)`（`:39-41`） | 同上 | `AuthSessionActions.kt:14`、`AuthTokenRefresh.kt:102`、`AuthRepository.kt:272`/`:285` |
| `AuthSessionActions.logout()` | 三连：`AccountFacade.logout()` → `AuthSessionState.clearLocal()` → `AuthGate.clearPending()`（`AuthSessionActions.kt:12-16`） | 清 KV 会话 + 清网络层 access token + 清响应式态 + 清 pending | `AuthRepository.kt:201`/`:218`/`:223`/`:228`、`MineProfileLogic.kt:24` |

`AuthSessionActions.kt:5-10` 的 KDoc 点明了它为什么在 `:component` 而不是 `feature/auth`：
规则 R7（feature 不得 import 兄弟 feature），而 mine / shell / auth 都需要「本地登出」这一事务；远端登录流程仍留在 `feature/auth`。

### 3.2 `chat/` —— 两个契约，一个是空口，一个是 mock

`ChatPort.kt`（17 行）只有两个成员：

- `val isReady: Boolean`（`ChatPort.kt:8`）
- `suspend fun ensureLoggedIn(userId: String, userSig: String): Result<Unit>`（`:9`）

唯一实现者 `StubChatPort`（`:12-16`）：`isReady` **恒为 false**，`ensureLoggedIn` **恒返回**`Result.failure(IllegalStateException("Chat SDK not wired"))`（`:15`）。
装配层把它放进容器：`AppContainer.kt:42` `val chat: ChatPort = StubChatPort`。
**全仓库没有任何调用点读取 `bridges.chat`**（`ChatPort`/`StubChatPort` 的引用只出现在 `AppContainer.kt:3-4`、`:42`）——也就是说这个端口目前是「装了但没人按」的占位，不是可用能力。

`ImEngine`（`ImEngine.kt:17-36`）是另一套**实际在用**的聊天契约，11 个成员：

| 成员 | 语义 | 副作用 / 返回 |
|---|---|---|
| `conversations()` | 会话列表，按 `lastMessageAtMs` 降序 | 先 `ensureSeed()`（`:225-228`、`:221-223`）；返回副本 |
| `messages(conversationId)` | **最新在前**的线程 | `:239-243`，副本 |
| `sendText(id, body)` | 走 `sendPipeline`，`body.trim()` | 空串返回 `null`（`:314-315`）；返回处于 `Sending` 的消息 |
| `sendImage(id, urlOrPath)` | 预览文案固定 `"[图片]"`（`:267-268`） | 同上 |
| `sendVoice(id, label="[语音]")` | 预览文案固定 `"[语音]"`（`:270-271`） | 同上 |
| `sendCustom(id, title="Demo 名片")` | 预览文案固定 `"[自定义消息]"`（`:273-274`） | 同上 |
| `markConversationRead(id)` | 非自己的 `Unread` 全部置 `Read`，会话 `unreadCount=0` 并重排 | `:245-262`，通知监听器 |
| `ensureConversation(id,title,lastMessage,unreadCount)` | **id 规范化**后 upsert，返回规范化 id | `:398-452`（见下） |
| `recallMessage(id,msgId)` | 3 分钟内且是自己、非 Time/System ⇒ 原地替换为 System「你撤回了一条消息」 | `:276-306`，返回 Boolean |
| `observe(listener)` | 返回反注册闭包 | `:168-173` |
| `filterConversations(query)` | 按 title/lastMessage 子串（小写）过滤 | `:230-237` |

`sendPipeline`（`:308-351`）的时序是**这个 mock 的核心可观测行为**：
插入时间分隔符 → 生成 `local_<seq>_<now>` → `list.add(0, pending)` → 更新会话预览并 `sortConversations()`→ 通知监听器 → 后台协程 `delay(280)` 后置 `Success` 并补 `messageUid = "uid_$now"` → 再 `delay(2000)` 后置为已读
（`:336-349`）。后台作用域是自建 `CoroutineScope(SupervisorJob() + Dispatchers.Default)`（`:152`）。

`ensureConversation` 的 id 规范化只有三条规则（`:405-409`）：以 `private_` / `group_` 开头原样用；非空则补 `private_` 前缀；空则落到哨兵 `private_conv_mock_push`。
新建会话时 `title` 空 ⇒ 「推送会话」，`lastMessage` 空 ⇒ 「来自 Push/Deeplink 的 mock 会话」（`:427-428`），并在列表为空且有 `lastMessage` 时补一条 `m_push_<now>` 的对方未读消息（`:435-447`）。

数据类附带三个「展示即逻辑」的派生属性：
`ImConversation.updatedAtLabel`（`:54`）、`ImMessage.timeLabel`（`:69`）、`ImMessage.canRecall`（`:72-76`：`isSelf` ∧ 非 Time/System ∧ `nowMs()-createdAtMs <= 180_000L`）、
`ImMessage.statusLabel()`（`:78-87`：`""` / `发送中` / `发送失败` / `已读` / `送达`）。
时间格式化是**手算 UTC+8**的 `internal object ChatTimeFormat`（`:95-138`），没有引时区库。

单例入口：`ImEngineStore.engine by lazy { MockImEngine(seedDemo = true) }`（`:145-147`）。
消费面（全部直接依赖 mock 单例，而不是 `ChatPort`）：
`ChatScreen.kt:57`（默认参数注入引擎）、`:67` `observe`、`:72`/`:77`/`:80`/`:82`/`:83`/`:88`；`FriendDirectory.kt:41`、`:51` 用 `ensureConversation` 打通「好友 ⇒ 会话」；
`NativeJetpackTabs.kt:94`（Android 原生 tab 也直接 `ImEngineStore.engine`）；`NativeAndroidMain.kt:471` 调 `chatDetailArgsFromDeepLink(pending.rawUri)`。
测试：`MockImEngineTest.kt:15-35`（发送流水线 + unread、`private_`/`group_` 规范化）。

`chatDetailArgsFromDeepLink(rawUri)`（`:470-501`）：取 `?` 之后、`#` 之前的片段，按 `&`/`=` 手工切表并做`percentDecode`（`private fun`，`:504-543`，支持 `%HH` 与 `+`，UTF-8 组字节）；
`peerName`/`id`/`peerId` 三者全空 ⇒ 返回 `null`（`:482-487`，对应 Flutter 的「缺少会话参数」）；`unread` 解析失败 ⇒ 0（`:499`）。返回类型 `ChatDetailDeepLinkArgs`（`:458-463`）。

### 3.3 `pay/` —— 端口 + 结果代数 + 沙箱 + 旗标网关

`PayGateway`（`PayGateway.kt:4-7`）两成员：`availableChannels(): List<PayChannel>`、`suspend pay(channel, planId): PayResult`。`PayChannel` 只有 `WeChat` / `Alipay`（`:9-12`）。
`PayResult` 是 sealed 代数（`:14-27`）：`Success(channel, planId, sandbox=false)`、`Cancel`、`Unavailable(channel)`、`Failure(message)`；`:15-17` 明确 `sandbox=true` 只代表沙箱、不是真扣款。

| 类型 | 关键行为 | 证据 |
|---|---|---|
| `StubPayGateway` | `availableChannels()` 空表；`pay()` **恒 `Unavailable`**；KDoc 声明「绝不合成 Success」（`:29-38`） | `:33-38` |
| `PayFeatureFlags` | 三个可变旗标：`weChatEnabled=false`、`alipayEnabled=false`、`sandboxEnabled=true`（**默认开**） | `:48-52` |
| `PayChannelAdapter` | `val channel` + `suspend pay(planId)` | `:55-58` |
| `SandboxPayChannelAdapter` | `pay()` 直接 `Success(channel, planId, sandbox = true)` | `:64-69` |
| `platformWeChatPayAdapterOrNull()` / `platformAlipayPayAdapterOrNull()` | **internal expect**，三端 actual 全部返回 `null`（见 2.2） | `:75-77` |
| `PlatformPayAdapters` | `weChatOrNull()` / `alipayOrNull()` = 平台适配器 ?: 沙箱适配器（沙箱受 `sandboxEnabled` 控制） | `:83-94` |
| `FlaggedPayGateway` | 默认参数即 `PlatformPayAdapters.weChatOrNull()/alipayOrNull()`；`isChannelOpen` = `realFlag \|\| (sandboxEnabled && adapter is SandboxPayChannelAdapter)` | `:101-130`，判定在 `:121-129` |

净效果（代码可直接推出，不需要跑）：三端真实适配器都是 `null` ⇒ 落到 `SandboxPayChannelAdapter` ⇒`sandboxEnabled` 默认 `true` ⇒ `availableChannels()` **非空**（微信、支付宝都在），`pay()` 返回 `Success(sandbox=true)`。
消费者只有会员页：`MembershipScreen.kt:42` 把 `FlaggedPayGateway()` 作为默认参数，`:46` 取渠道列表，`:78-81` 调 `gateway.pay(...)`，`:83-88` 把四种结果映射成 toast 文案
（沙箱那条是「沙箱支付成功（非真实扣款）」`:84`；不可用是「渠道不可用（未接入真实 SDK）」`:86`），`:89` 弹 toast。

### 3.4 `push/` —— 端口、stub 桥、上报 API 三层

`PushBridge`（`PushBridge.kt:19-35`）四成员：
`suspend registerForPush(): PushRegistration`、`suspend uploadToken(token): Result<Unit>`、`currentToken(): StateFlow<String?>`、`handleNotificationClick(payload: Map<String,String>)`。
`PushRegistration(token: String?, platform: String)`（`:37-40`）。

`StubPushBridge`（`:46-90`）逐成员：

| 成员 | 实际做了什么 | 证据 |
|---|---|---|
| `registerForPush()` | 调 `PlatformPushSdk.fetchRegistrationToken()`；**非 null 才**写 `_token`；返回 `PushRegistration(_token.value, platformName())`。三端 actual 都返回 `null` ⇒ 结果恒为 `PushRegistration(null, "android"/"ios"/"ohos")` | `:49-58` |
| `uploadToken(token)` | **不请求网络**：只把 token 写进内存 StateFlow 然后 `Result.success(Unit)`；原地挂着 `TODO(Spike II)` | `:60-64` |
| `currentToken()` | `asStateFlow()` | `:66` |
| `handleNotificationClick(payload)` | `extractDeepLink` 取 `deeplink`→`url`→`route` 第一个非空；裸键 `home/chat/community/mine/login(auth/login)` 映射成 `AppRoutes` 常量；交给 `DeepLinkRouter.accept(uri)` | `:68-89`、`DeepLinkRouter.kt:36` |

`PlatformPushSdk` 是 `internal expect object`，两函数（`:99-104`），`:95-97` 逐端挂着 FCM / APNs / PushKit 的 TODO。

`PushRegistrationApi`（`PushRegistrationApi.kt:29-33`）是**类**，默认参数 `client = NetworkFacade.api()`、`analytics = NoopPushRegistrationAnalytics`、`log = println`。
`suspend report(registrationId, alias?, deviceId?, platform?, mock=false)`（`:35-72`）的顺序是：
打日志（`:42-45`）→ `analytics.trackPushRegister`（`:46-50`）→ **`if (mock || registrationId.isEmpty()) return`**（`:52`）
→ `client.postRaw(PATH, buildJsonObject{ registration_id; alias/device_id 仅非空才写键; platform ?: detectPlatform(); mock=false })`（`:54-67`）→ **`catch (t: Throwable)` 全部吞掉，只打一行日志**（`:68-71`）。
常量：`PATH = "/api/v1/push/devices"`（`:76`）、四个键名（`:78-82`）、四个平台字面量 `harmony/ios/android/unknown`（`:85-88`）、`detectPlatform()` 把 `ohos` 映射成 `harmony`、映射不到 ⇒ `unknown`（`:96-101`）。
埋点缝隙 `PushRegistrationAnalytics` 是 `fun interface`（`:109-111`），默认实现什么都不做（`:114-116`）。
`:24-27` 用 `// GAP(R5)` 说明 JPush / 华为 HPush 缺失，因此「拿到真实 token」这条链路只有接口没有实现。

**调用点核查**：`PushRegistrationApi` 在本仓库没有任何构造/调用点（除自身文件与 KDoc 引用）；`StubPushBridge` 只在 `AppContainer.kt:39` 被放进容器，唯一被真实调用的成员是
`registerForPush()`，位置在 `NativeAndroidApp.kt:67`，且被 `runCatching` 包住（隐私同意通过时触发，`:63-68`）。
`uploadToken` / `currentToken` / `handleNotificationClick` 无生产调用点。

### 3.5 `realtime/` 契约面（AppRealtimeClient.kt）

`AppRealtimeClient`（`AppRealtimeClient.kt:18-75`）13 个成员：
6 个只读属性 `connectionState: Flow<RealtimeConnectionState>`（`:21`）、`currentState`（`:24`）、`lastSeq: Long`（`:27`）、`reconnectCount: Int`（`:30`）、`outboundQueueDepth: Int`（`:33`）、
`keepAliveInBackground: Boolean`（`:36`）；7 个操作 `suspend prepare()`（`:42`）、`connect()`（`:45`）、`disconnect(reason=null, clearQueue=false)`（`:48`）、
`subscribeTopics(topics)`（`:51`）、`unsubscribeTopics(topics)`（`:54`）、`watchTopic(topic): Flow<RealtimeEnvelope>`（`:57`）、`watchEvents(eventName=null)`（`:60`）、
`sendEvent(topic, eventName, payload: JsonObject?=null, requireAck=true)`（`:66-71`）、`setKeepAliveInBackground(enabled)`（`:74`）。
`:15-16` 说明相对 Dart 只刻意扩展了 `prepare`（为让装配层不依赖实现类）。

同一文件里的 5 个注入缝隙：

| 缝隙 | 契约要点 | 默认实现 |
|---|---|---|
| `RealtimeSessionPort` | `isLoggedIn()` 只回答有无登录态；`accessToken()` **无 token 时抛 `IllegalStateException`**（该异常会被鉴权硬失败策略认下 ⇒ 停重连） | 无默认；装配层注入（`:87-91`） |
| `RealtimeSessionGuard` | `isForceLogoutError` / `suspend tryRecover(): Boolean` / `suspend handleIfForceLogout` | 构造参数默认 `null`（`AppRealtimeClientImpl.kt:65`）⇒ 互踢分支整体跳过 |
| `RealtimeNotifyHandler` | `fun interface`，`sys.notify` 落地口；实现方**自己**负责 notifyId 去重 | 无默认，必传（`:114-116`、`:56`） |
| `RealtimeNetworkMonitor` | `start(onOnline)` / `stop()`；`:122-124` 用 `// GAP(R5)` 说明本仓无跨端 connectivity 插件 | `NoopRealtimeNetworkMonitor`（`:133-136`，默认值在 `:66`） |
| `RealtimeTicker` / `RealtimeClock` / `RealtimeCancellable` / `RealtimeAppLifecycle` | 时间与调度缝隙；**句柄一次性语义**（动作体第一行须把自己那个字段置 null，`RealtimeScheduler.kt:33-39`） | `SystemRealtimeClock`（`:23-26`）、`CoroutineRealtimeTicker(scope)`（`:45-55`），默认值在 `AppRealtimeClientImpl.kt:60-61` |

### 3.6 `routing/CrossFeatureScreenPort.kt`

见第 7 节专题。

### 3.7 `webview/WebViewComponent.kt`

`internal object WebViewComponent { const val JS_BRIDGE_NAME = "NativeBridge" }`（`:11-13`）。
`:3-10` 的 KDoc 说明运行时 composable **仍在** `core.platform.PlatformWebView`，这里只是「归属标记」，等 expect/actual 包搬迁完成后才名副其实。核查结果：`WebViewComponent` 与 `JS_BRIDGE_NAME`在 `:component` 之外**没有任何引用**。

`public const val OfflineWebFixtureUrl`（`:20-32`）是真正被使用的部分：
一条 percent-encode 过的 `data:text/html;charset=utf-8,…`，标题「网页测试」、正文「离线网页」，不需要联网（`:15-19` 说明目的是 CI/模拟器无外网，并且让 `feature/home` 不必 import `feature/web`）。
消费者：`HomeScreen.kt:20` import、`:49` `navigator?.navigate(AppRoute.InAppWeb(OfflineWebFixtureUrl))`；`AppShell.kt:58` import；`NativeAndroidMain.kt:49` import。

---

## 4. realtime 专题（`component/realtime`，12 个 commonMain 文件 + 10 个 commonTest 文件）

这一 lane 是 `:component` 里唯一「成体系但没接线」的部分：逻辑完整、协议齐全、测试密集，但整个仓库里**没有任何装配层构造它**。证据见 5.2。

### 4.1 连接状态机：7 个状态与每一条转移

状态定义在 `RealtimeConnectionState.kt:10-18`：`Disconnected`、`Connecting`、`Authenticating`、`Connected`、`Reconnecting`、`Degraded`、`Failed`。
`isActive` **只**包含 `Connected` 与 `Degraded`（`:21-22`，被 `RealtimeStateAndTelemetryTest.kt:23-35` 逐值钉住）；`label` 七个中文文案：未连接 / 连接中 / 鉴权中 / 已连接 / 重连中 / 降级 / 失败（`:25-34`）。

**`Failed` 从来没有被状态机写入。** 全模块搜索 `RealtimeConnectionState.Failed` 只有三处：
枚举文案（`RealtimeConnectionState.kt:33`）、闸门等待条件的读（`RealtimeStartupGate.kt:172`）、测试（`RealtimeStateAndTelemetryTest.kt:33`/`:46`）。
`AppRealtimeClientImpl.kt` 里 9 个 `setState` 调用点（`:141`、`:149`、`:187`、`:229`、`:276`、`:412`、`:448`、`:505`，定义在 `:594`）没有一个是 `Failed`。这不是遗漏，`:7-8` 的 KDoc 明确写了 Dart 侧同样只把 `failed` 当「别再等了」的终止条件。

转移表（全部来自 `AppRealtimeClientImpl.kt`）：

| 起 | 止 | 触发条件 | 证据 |
|---|---|---|---|
| Disconnected | Connecting | `connect()` 通过两道前置检查（已登录 ∧ 当前不在 Connected/Connecting/Authenticating），且 `isReconnect=false` | `:117-131`、`:149-151` |
| — | Reconnecting | 同上入口但 `isReconnect=true`（退避定时器到点、`Resumed` 补连、恢复联网补连） | `:149-151`、`:419-422` |
| Connecting | Authenticating | 换票成功、`transport.connect(url)` 成功、`listenInbound()` 挂好后，发 `auth` 帧**之前** | `:156-163` |
| Authenticating | Connected | 收到 `type == "auth_ok"` | `:272-283` |
| Connected | Degraded | `sendEvent()` 时 `!currentState.isActive`（消息已先入队） | `:496-507` |
| 任意 | Reconnecting | `scheduleReconnect()`：非手动 ∧ 已登录时 `_reconnectCount++` 后排定时器 | `:409-413` |
| 任意 | Disconnected | ① `disconnect()`；② `stopReconnectPermanently()`；③ `connectInternal` 开头发现 `_manualDisconnect \|\| !isLoggedIn()` | `:437-449`、`:225-230`、`:140-147` |

同值不发（`setState` 首行 `if (_state == state) return`，`:595`），发流用 `tryEmit`（`:598`），`MutableSharedFlow(replay = STATE_REPLAY)` 且 `STATE_REPLAY = 1`（`:72`、`:630-631`）——
`:630` 的注释解释了这条相对 Dart broadcast 流的**刻意差别**：留 1 条让后到的订阅者立刻拿到当前态。

永久停止重连的四个入口（都走 `stopReconnectPermanently`，`:225-231`）：
互踢且同设备恢复失败（`:198`）、`RealtimeWsError.Unsupported`（`:203-205`）、鉴权硬失败且恢复失败（`:216`）、关闭码 4002（`:399-400`）。

### 4.2 换票 → 建连 → 鉴权 → `/realtime/sync` 全链路

`connectInternal(isReconnect)`（`:137-222`）按顺序做七件事：

1. 取消待触发的重连任务（`:138`），再做「已手动断开 / 已登出」的短路检查（`:140-147`）；
2. 复位 `transportLostHandled` 闩（`:148`）并置 Connecting|Reconnecting（`:149-151`）；
3. `session.accessToken()` → `ticketApi.fetchTicket(token)` → `resolveRealtimeWsUrl(ticket.wsUrl)`（`:155-157`）。
换票请求体是**裸** `{"platform":"mobile"}`，路径 `RealtimeConfig.TICKET_PATH = "/api/v1/realtime/ws-ticket"`（`RealtimeWsTicketApi.kt:36-39`、`RealtimeConfig.kt:21`、`:25`）；
 `accessToken` 为空串时 `fetchTicket` 直接抛 `IllegalStateException("缺少 access token，请先登录")`（`:31-35`）；响应解不开 ⇒ `RealtimeWsError.BodyUnparsable`（`:40-42`）；`wsUrl` 为空 ⇒ `TicketInvalid`（`:43-45`）；
 `resolveRealtimeWsUrl` 只做 loopback 改写，不动 scheme/端口/路径（`:81`）；
4. `activeTransport().connect(url)` + `listenInbound()`（`:159-160`）；
5. 置 Authenticating 并发 `auth` 帧：`payload = {ticket, platform="mobile", connId}`，帧 id 来自注入的 `newFrameId()`（默认 `AppUuid.random()`），`ts` 取注入时钟（`:162-174`）；
6. `telemetry.trace("ws_connect", durationMs, params={mock: useMockGateway})`（`:176-180`）；
7. `auth_ok` 到达后（`:272-283`）：`reconnectPolicy.reset()` + `_reconnectCount = 0` + 置 Connected + `startHeartbeat()` + `afterConnected()`。

`afterConnected()`（`:314-340`）是「补齐离线期」的四步：

1. `syncApi.sync(sinceSeq = seqStore.lastSeq, topics = subscribedTopics.toList())` ——
请求体 camelCase `{sinceSeq, topics}`，路径 `/api/v1/realtime/sync`（`RealtimeWsSyncApi.kt:26-37`、`:42-45`、`RealtimeConfig.kt:22`）；
响应 `events` 逐条走**与入站帧相同**的 `acceptSeq` + `dispatch` + notify 双路径（`:321-325`），最后 `seqStore.setLastSeq(sync.latestSeq)` 把水位抬到服务端声明的最新值（`:326`）；
 `RealtimeSyncData` 的 `events`/`latestSeq` 两个键都必须存在，非 object 元素被跳过（`RealtimeWsSyncApi.kt:47-55`、`:18`）；
2. `telemetry.trace("ws_sync", …)`；**catch 全部 Throwable，只 `error("ws_sync_fail")` + 打日志，绝不影响连接**（`:327-331`）；
3. `replayOutboundQueue()`：`outboundQueue.pendingItems()` 逐条 `sendOutbound(...)`，
 **不传 `requireAck` ⇒ 沿用默认 true**（`:343-353`、`:527`）；
4. `subscribedTopics` 非空则补发 `sub` 帧，然后 `ensureNetworkWatcher()`（`:335-339`）。

`sub` / `unsub` 帧的 payload 固定 `{"topics": […]}`（`:618-623`）。
`subscribeTopics` 在任何状态都先记集合，只有 `isActive` 才真发帧（`:453-456`）；`unsubscribeTopics` 同理（`:459-471`）。这就是「离线期登记的 topic 在 `auth_ok` 后被补投」的机制。

入站分派表 `onEnvelope`（`:270-307`）：

| `type` | 行为 | 行号 |
|---|---|---|
| `auth_ok` | 见上一条链路 | `:272-283` |
| `pong` | `heartbeat?.onPong(envelope.id ?: "")` —— **用帧 id 而不是 payload** | `:285` |
| `ack` | 取 `payload["refId"]`（`RealtimeEnvelope.kt:80`）非空才 `completeAck` ⇒ 删定时器 + `markAcked` | `:287-291`、`:564-567` |
| `error` | 只 `telemetry.error("ws_server_error", IllegalStateException(code))`，**不断连、不改状态** | `:293-297` |
| `event` | `seqAccepted = seqStore.acceptSeq(seq)`；仅 accepted 才 `router.dispatch`；**独立地**，`topic == "sys.notify"` 就交给 `notifyHandler` | `:299-303` |
| 其它 | `else -> Unit` | `:305` |

### 4.3 心跳参数与真实行为

参数来自 `RealtimeConfig.kt:28-30`：`HEARTBEAT_INTERVAL_MS = 25_000`、`HEARTBEAT_TIMEOUT_MS = 10_000`、`HEARTBEAT_MAX_MISS = 2`；`HeartbeatScheduler` 的三个构造默认值直接引用它们（`HeartbeatScheduler.kt:21-23`）。

`firePing()`（`:70-91`）的四条「勿顺手改掉」的行为（`:9-16` KDoc 逐条点名）：

1. ping id = `"ping_${clock.nowMs()}"`，先写 `pendingPingId` 再发（`:71-72`）；
2. 发送用 `runCatching` **吞掉异常**，只打日志（`:73-74`）；
3. armed 超时后**无条件**再排下一次 interval —— 不等 pong（`:89-90`）⇒ 同一时刻可以并行跑着多个超时计时；
4. 超时回调体：`missCount++` → 达 `maxMiss` ⇒ `onTimeout()` 后 **`return@after`**（不再排下一次）；未达 ⇒ 清 `pendingPingId` 后 `scheduleNext()`（`:77-87`）。

`onPong(pingId)` **只有 id 与当前 pending 相同**才取消超时计时并清零 miss（`:53-59`）。
`start()` 是 `stop() + scheduleNext()`（`:37-40`），所以重复 `start()` 不会叠定时器（`:43-50` 会把两个句柄与两个计数都清掉）。
观测口 `currentMiss` / `currentPendingPingId`（`:33-34`）只读、不参与协议。

状态机侧：`startHeartbeat()` 先 `heartbeat?.stop()` 再新建并 `start()`（`:356-367`）；ping 帧只有 `id`/`type`/`ts`（`:361-365`）；`onTimeout` 归一化为 `handleTransportLost(REASON_HEARTBEAT_TIMEOUT)`（`:366`），
常量字面量 `"heartbeat_timeout"`（`:647`）。

### 4.4 重连退避与断线归一化

`ReconnectPolicy` 参数默认取自 `RealtimeConfig.kt:33-37`：`initial=1_000`、`max=60_000`、`multiplier=2.0`、`jitterRatio=0.2`、`floor=500`。`nextDelayMs()`（`ReconnectPolicy.kt:38-46`）有**副作用**（每次调用 `_attempt++`）：

```
exponent = (_attempt-1).coerceAtMost(20)          // :40 + MAX_EXPONENT :49
baseMs   = initialMs * multiplier^exponent        // :41
capped   = min(baseMs, maxMs)                     // :42
jitter   = capped * jitterRatio 取整              // :43
delta    = jitter==0 ? 0 : random.nextInt(jitter*2) - jitter   // :44
返回      maxOf(floorMs, capped + delta)          // :45
```

`:15-17` 与 `:17` 的说明指出唯一刻意差别是 `MAX_EXPONENT = 20`（Dart 的 `pow(2.0, 1024).toInt()` 会抛）。
**上界不夹回 `maxMs`**：`capped + jitter` 可以超过 60000，这一点由 `ReconnectPolicyTest.kt:14-15` 的注释明确保留。

`handleTransportLost(reason, close?)`（`:378-406`）是所有断线的归一化入口，顺序是：
`_manualDisconnect` 直接 return → `transportLostHandled` 闩 return → 置闩 → `heartbeat.stop()` →`metric("ws_transport_lost", {reason, closeCode?})` → `runCatching { transport.close() }` →
若 `close.requiresNewTicket` 则 `metric("ws_ticket_refresh", {code})` + `reconnectPolicy.reset()` →若 `close.code == 4002` 则 `stopReconnectPermanently("ws_close_4002")` + 把
`IllegalStateException("ws_kicked")` 交给 `sessionGuard`（可为 null ⇒ 什么都不做）→ 否则 `scheduleReconnect(reason)`。

入站流的正常收尾与异常都归到这里（`:238-254`，reason 分别是常量 `"stream_done"`（`:646`）与 `t.toString()`），关闭码通道 `listenClosed` 是 KMP 独有（`:256-263`，Dart 读不到 close code，`RealtimeWsTransport.kt:20-26` 说明了这点）。
这个闩的存在理由写在 `:92-97`：Ktor 会**同时**给 `closed` 与入站收尾，不夹一道就多算一次重连。

三个关闭码常量与判定：`WS_CLOSE_AUTH_FAILED=4001`、`WS_CLOSE_KICKED=4002`、`WS_CLOSE_TICKET_EXPIRED=4003`（`RealtimeConfig.kt:49-51`），`requiresNewTicket` 三码全真（`:62-63`），
`isAuthCloseCode` 只含 4001/4003（`:66-67`）—— 但**状态机没有调用 `isAuthCloseCode`**，4002 与另两码的区分是直接在 `:395`（requiresNewTicket）与 `:399`（== WS_CLOSE_KICKED）两个分支里写的。
`WS_CLOSE_NORMAL = 1000`（`:70`）用于 `disconnect`（`AppRealtimeClientImpl.kt:446`）。

`scheduleReconnect`（`:409-423`）：`_reconnectCount++` → 置 Reconnecting →`metric("ws_reconnect_scheduled", {reason, delayMs, attempt})` → 用 `ticker.after(delayMs)` 排 `connectInternal(true)`，
动作体第一行把 `reconnectTask` 置 null（遵守 `RealtimeScheduler.kt:33-39` 的句柄一次性约定）。

`disconnect(reason, clearQueue)`（`:437-450`）：置 `_manualDisconnect = true` → 取消重连任务、心跳、两个订阅 Job →`transport?.close(WS_CLOSE_NORMAL, reason)` → `if (clearQueue) outboundQueue.clear()` → 置 Disconnected。

### 4.5 seq / 去重 / 乱序：出站队列与入站路由怎么配合

**seq 水位**（`RealtimeSeqStore.kt:59-93`）：`acceptSeq(null)` 恒 true（`:79`）；`seq <= _lastSeq` ⇒ false（`:80`，**等号也算重复**）；接受则抬水位并**同步** `save()`（`:81-83`）。
`setLastSeq` 只抬不降、等号不写（`:87-92`）。`load()` 把缺失/脏值一律归 0（`:68-70`）。

**去重与乱序的实际语义**：`onEnvelope` 的 `event` 分支（`AppRealtimeClientImpl.kt:299-303`）里seq 判定与 notify 处理是**两条独立路径** —— 旧 seq 的 `sys.notify` 帧不进路由，但仍交给 `NotifyHandler`。
`:266-268` 的注释给出理由（避免「seq 已更新但 Banner 未展示」），该行为由`AppRealtimeClientImplTest.kt:268-284` 直接钉住（`seq=5` 而水位=10 ⇒ `notifyFrames` 空但 `notifier.handled.size == 1`）。

**notifyId 去重环**（`RealtimeSeqStore.kt:107-151`）：空 id 恒放行且不记账（`:136`）；命中 ⇒ 拒绝（`:137`）；新 id `add(0, …)` 插**头部**，超过 `NOTIFY_DEDUP_MAX = 200`（`RealtimeConfig.kt:43`）
从尾部逐个截（`:138-141`）；随后整体持久化为紧凑 JSON 数组（`:142`、`:147-150`）。
`load()` 的容错分三档：空串/缺失**提前 return**（保留内存环，`:117-118`）；解析失败或非数组 ⇒ **清空环**（`:119-126`）；正常 ⇒ 覆盖（`:127-128`）。
注意：`NotifyDedupStore` 目前**没有生产调用点**（全仓搜索只有自身文件与其测试），去重实际由 `RealtimeNotifyHandler` 的实现方负责（`AppRealtimeClient.kt:108-113` KDoc 就是这么约定的）。

**入站路由**（`RealtimeInboundRouter.kt`）三条容易踩坑的语义（`:13-27` KDoc + 代码一致）：

1. topic 频道**懒注册**：`dispatch` 只在 `topicFlows.containsKey(topic)` 时投递（`:46-48`），没 `watchTopic` 过的 topic 静默丢弃；
2. 只有 `type == "event"` 进全局 event 频道（`:49-51`，常量在 `:74`）⇒ 控制帧不污染业务流；
3. `watchEvents(eventName)` 的过滤在**下游** `filter`（`:37-38`），不是按名字建频道；`eventName == null` 时返回**同一个**共享流实例。

多播实现用 `MutableSharedFlow(replay = 0, extraBufferCapacity = 64, DROP_OLDEST)`（`:66-70`、`:77`），`dispatch` 是同步 `tryEmit` ⇒ 慢订阅者不会阻塞入站循环（`:40-43`）。
`dispose()` 只清空登记表（`:58-60`）：已交出去的 Flow 变成静默流，之后 `watchTopic` 可以重新登记。

**出站队列**（`RealtimeOutboundQueue.kt`）：`RealtimeOutboundStatus` 三个 SQL 字面量 `pending/sent/failed`（`:25-29`）；行模型 `RealtimeOutboundRow` 七列（`:32-40`）；缝隙 `RealtimeOutboundStore` 五操作（`:48-58`），
KDoc `:42-47` 写清对应 Dart SQL（upsert 冲突替换、`status IN ('pending','sent') ORDER BY created_at ASC`、`updateStatus` 同时刷 `updated_at`）。

`depth` 是**自增计数器**而不是查询，三处口径全部刻意保留（`:18-21` + 代码）：

| 操作 | 对 depth 的影响 | 行号 |
|---|---|---|
| `enqueue` | 落 PENDING 行（created=updated=now）后 `_depth++`，返回 messageId | `:132-152` |
| `markSent` | 只改状态，**不动 depth** | `:155-157` |
| `markAcked` | 删行 + `(_depth-1).coerceIn(0, 9999)` | `:160-163`、上界常量 `:190` |
| `markFailed` | 只改状态；行留在表里但**不在 pending 视图** ⇒ 不再重发 | `RealtimeOutboundQueue.kt:166-168` |
| `pendingItems()` | 读行的同时 `_depth = rows.size`（**覆盖**） | `:171-175` |
| `clear()` / `refreshDepth()` | 归零 / 用真实行数覆盖 | `:178-186` |

`RealtimeOutboundItem.decodePayload()` 解析失败回 `{}`（`:107-110`），数组也算失败。
`InMemoryRealtimeOutboundStore` 的顺序 = 插入顺序（`:60-86`），`updateStatus`/`delete` 对不存在的 id 静默忽略。

**发送侧**（`AppRealtimeClientImpl.kt:495-561`）：`sendEvent` **先入队再发送**（`:496-503`），不活跃就置 Degraded 直接 return（`:504-507`）⇒ 消息不丢；
`sendOutbound` 里 `markSent` → 发 `type=event` 帧，`payload = {name: eventName, ...payload}`（`:537-540`）→`requireAck` 时挂 `ticker.after(OUTBOUND_ACK_TIMEOUT_MS = 15_000)`（`RealtimeConfig.kt:40`），
回调体先 `pendingAcks.remove(messageId)` 再 `metric("ws_outbound_ack_timeout")` + `markFailed`（`:544-551`）；
`requireAck=false` 直接 `markAcked`（`:552-554`）；最后 `trace("ws_send", …, {topic, eventName})`（`:556-560`）。
**帧 id 就是队列的 messageId**（`:533`、`:496`）⇒ `ack.refId` 才能对上（`:289-290`）。

### 4.6 鉴权失败怎么降级

`RealtimeAuthFailure.kt` 自带一份判定规则，`:12-16` 说明原因：分层不允许 import `feature/auth`，所以这里复刻 `feature/auth/AuthHttpDecision.kt:21-54` 的等价逻辑，且要求两处变更同步。

纯规则面 `RealtimeAuthHttpRules`（`:25-76`）：业务码 `10021` 互踢 / `10022` 会话无效 / `10023` token 无效（`:28-32`）；`EXPIRED_TOKEN_HINTS` 八个关键词（`:35-44`）、`FORCE_LOGOUT_HINTS` 两个文案（`:47`）；
`isHardFailure(statusCode?, code?, message="")`（`:61-75`）的顺序是：
互踢码（或无码且命中互踢文案）⇒ true → **`statusCode == 403` 立刻 false**（`:71`，先于关键词判定）→`10023` / `401` / （无码且命中过期文案）⇒ true。

默认策略 `DefaultRealtimeAuthHardFailurePolicy`（`:82-114`）按异常类型分流：
`NetworkError.Business` ⇒ 取 `code` + `message`；`NetworkError.Transport` ⇒ 取 `httpStatus` + `message`（`ApiResponse.kt:24-33` 定义了这三者）；`NetworkError.TokenExpired` ⇒ **恒 true**（`:103-106`）；
`IllegalStateException` ⇒ 文案含 `"token"` 或 `"未登录"`（`:107-111`）；其余 ⇒ 整条 `toString()` 走文案判定（`:112`）。
顶层便捷入口 `isRealtimeAuthHardFailure(error)`（`:117-118`）。

**降级路径写在状态机里**（`AppRealtimeClientImpl.kt:181-221`），catch 顺序明确不可调（`:135` 的注释）：
`CancellationException` 先重抛 → `telemetry.error("ws_connect_fail", t)` →已手动断开/已登出 ⇒ 取消重连并置 Disconnected →
`guard.isForceLogoutError(t)` ⇒ `tryRecover()` 成功就**递归** `connectInternal`，失败就 `stopReconnectPermanently("session invalid")`+ `handleIfForceLogout(t)` →
`RealtimeWsError.Unsupported` ⇒ 永久停（`:202-206`）→`authFailurePolicy.isHardFailure(t)` ⇒ 有 guard 且 `tryRecover() && isLoggedIn()` 就重试，否则永久停 →其它 ⇒ `scheduleReconnect`。
注意：`sessionGuard` 默认为 `null`（`:65`），所以「静默恢复」这条支路完全依赖装配层注入，而装配层目前**没有**构造 realtime（5.2）⇒ 现实中走不到。

`RealtimeSessionPort.accessToken()` 的「无 token 就抛」正是接进这条链的：
抛出的 `IllegalStateException` 文案含 `token` ⇒ 被策略认成硬失败 ⇒ 停止重连（`AppRealtimeClient.kt:82-86`）。

### 4.7 StartupGate 挡住了什么、Telemetry 记了什么

`RealtimeStartupGate`（`RealtimeStartupGate.kt:33-43`）构造 9 个参数：
`client`、`session`、`scope`、`privacyGranted: () -> Boolean`、`ticker`、`defaultTopics = [sys.notify, presence.bulk]`（`:39`，常量来自 `RealtimeConfig.kt:81-82`）、
`connectTimeoutMs = 12_000`（`:40`、`:185`）、`connectedWaitMs = 8_000`（`:41`、`:188`）、`log`。

`tryConnectIfReady(trigger = "manual")`（`:59-100`）的**检查顺序本身就是契约**：

1. 先打一行诊断日志（含 privacy/loggedIn/state 中文 label，`:63-66`）；
2. **隐私未同意 ⇒ return**（`:68-71`）；
3. 未登录 ⇒ return（`:72-75`）；
4. 已 `isActive` ⇒ return（`:77-80`）；
5. `awaitWithTimeout(connectTimeoutMs) { client.connect() }`（`:83-84`）；
6. **无论超时与否**，`client.subscribeTopics(defaultTopics)` 照发（`:86`）；
7. `waitUntilConnected(8s)` 只用于打成功/失败日志（`:88-99`），不改变状态机。

两个超时的实现细节都要照实理解：`awaitWithTimeout`（`:153-163`）**不取消**底层工作（`ticker.after` 只 complete `false`，`work()` 继续在 scope 里跑，与 Dart `Future.timeout` 一致）；
`waitUntilConnected`（`:166-181`）命中 `isActive` 或 `Failed` 即结束等待，返回前取消定时器与 watcher Job。
类 KDoc `:25-28` 还写明第 4 步的动机：「不得阻塞 runApp，真机会一直停在原生启动图」。

五个链路口就是把 Dart 的四条回调链拆成方法：`onPrivacyGranted()` ⇒ trigger `"privacy_granted"`（`:103-105`）、`onAfterLogin()` ⇒ `"after_login"`（`:108-111`）、
`onAfterLogout()` ⇒ `disconnect("logout", clearQueue = false)`（`:114-116`）、`onEnvChanged()` ⇒ `disconnect("env_changed")`（用**默认** clearQueue=false）再重连（`:122-125`）、
`onAppLifecycle(state)` ⇒ 转交 `AppRealtimeClientImpl.onAppLifecycle`，不是具体实现时退化为「resumed 且不活跃就 `client.connect()`」（`:131-138`）。
`prepare()` 缓存 impl 句柄并转发（`:50-53`）；`dispose()` 只转发 impl 的 dispose 并清句柄（`:141-144`）；`hasLifecycleCapableClient` 是观测口（`:147`）。
`:46-49` 明确写了：Dart 在 `initDeferred` 里还会 `NotifyDedupStore.load()`，本类**不做**，交给装配层——这也是 5.2 里「dedup 无人 load」的直接原因。

遥测出口：`RealtimeTelemetry`（`RealtimeTelemetry.kt:45-85`）三入口 `metric` / `error` / `trace`，私有 `report` 组装 `payload = {type, name, ...params}`（`:78-83`）后以事件名 `"realtime_$type"` 交给 sink（`:84`）。
`error` 在 params **之后**追加 `error = throwable.toString()`（`:54-63`），`trace` 追加 `durationMs`（`:66-75`）。
默认 sink 只 `println`（`:33-37`），`:9-13` 说明 Dart 的第 2 步（全局 EventBus）在 KMP 侧被抽成缝隙且**不假装已上报**。

`AppRealtimeClientImpl` 实际产生的事件清单（全部有代码位置）：

| 事件名 | 类型 | name/键 | 触发点 |
|---|---|---|---|
| `realtime_trace` | trace | `ws_connect{mock,durationMs}` | `:176-180` |
| `realtime_error` | error | `ws_connect_fail{error}` | `:184` |
| `realtime_error` | error | `ws_server_error{error}` | `:296` |
| `realtime_trace` | trace | `ws_sync{durationMs}` | `:327` |
| `realtime_error` | error | `ws_sync_fail{error}` | `:329` |
| `realtime_metric` | metric | `ws_transport_lost{reason[,closeCode]}` | `:388` |
| `realtime_metric` | metric | `ws_ticket_refresh{code}` | `:396` |
| `realtime_metric` | metric | `ws_reconnect_scheduled{reason,delayMs,attempt}` | `:414-417` |
| `realtime_metric` | metric | `ws_outbound_ack_timeout{messageId}` | `:549` |
| `realtime_trace` | trace | `ws_send{topic,eventName,durationMs}` | `:556-560` |
| `realtime_metric` | metric | `ws_state{state}` | `:600` |

生命周期：`onAppLifecycle`（`:579-591`）——`Resumed` 且非手动且不活跃 ⇒ 补 `connect()`；`Paused` 且未开后台保活且活跃 ⇒ `disconnect("app_paused", clearQueue=false)`（**注意这会置 `_manualDisconnect`**）。
`setKeepAliveInBackground` 只翻一个布尔并打日志（`:570-573`）。
`dispose()`（`:608-615`）：`disconnect("dispose", clearQueue=false)` → `networkMonitor.stop()` →清 `networkWatcherStarted` → `router.dispose()` → `transport.dispose()` 并丢弃句柄。

### 4.8 mock ↔ 真传输的替换点

替换点是**一个函数 + 一个开关**，位置在 `AppRealtimeClientImpl.kt:658-666`：

```kotlin
public fun createRealtimeTransport(ticker, clock = SystemRealtimeClock): RealtimeWsTransport =
    if (RealtimeConfig.useMockGateway) MockRealtimeWsTransport(ticker, clock)
    else createPlatformRealtimeWsTransport()
```

- 开关是 `RealtimeConfig.useMockGateway`，生产默认 **false**（`RealtimeConfig.kt:16-18`）；
- 它是 `transportFactory` 构造参数的**默认值**（`:62-64`），测试通过传 `{ transport }` 整个换掉（`AppRealtimeClientImplTest.kt:86`）；
- 关键差别：选路发生在**首次使用时**（`activeTransport()` 的 `?: transportFactory()`，`:626-627`），不在构造期 —— `:44-47` 解释这是为了让 ohosX64 的 `RealtimeWsError.Unsupported` 桩不在依赖装配阶段就炸；
- `createPlatformRealtimeWsTransport()` 是 core 侧 expect（`RealtimeWsTransport.kt:65`），android/ios/ohosArm64 是 Ktor 实现，**ohosX64 直接 `throw RealtimeWsError.Unsupported()`**
（`RealtimeWsTransportFactory.ohosX64.kt:17-19`，`:10-12` 说明「不做伪实现」）；
- core 另有一个 `createRealtimeWsTransport()`（`RealtimeWsTransport.kt:73-74`）**不做 mock 分支**，与组件层的 `createRealtimeTransport` 是两个不同函数，别混用。

`MockRealtimeWsTransport`（283 行）的模拟协议表（`:79-125`）：
`auth` ⇒ 置 `authed`、回 `auth_ok`（`id = "auth_ok_<now>"`，payload `userId="mock_user"`/`sessionId="mock_session_<now>"`/`serverTime`）；`ping` ⇒ 回 `pong` **且 id 原样回带**（`:101-107`，`:24-25` 说改了就等价于丢包）；
`sub` ⇒ `topics.addAll` + `ack`（含 `refId` + topics 快照）+ 起周期推送；`unsub` ⇒ 摘除 + `ack` + topics 空则停计时；`event` ⇒ `ack{accepted:true}`；其它 ⇒ 只打日志不回帧。
未连接时 `send` 抛 `IllegalStateException("Mock WS not connected")`（`:80`）。
`close()` **不**往 `closed` 发（`:127-136`，与 Dart mock 从不触发 `onDone` 一致），只有测试/调试专用的 `simulateClose(code, reason)` 会发（`:142-150`）。
周期推送：间隔 `MOCK_PUSH_INTERVAL_MS = 20_000`（`:274`），`pushTick()` 在 **topic 循环体内 `seq++`**（`:197-199`）⇒ 同拍多 topic 各占一个 seq；
三种前缀分别造 `evt_notify_N` / `evt_live_N`（`seatIndex = seq % 4L`，`:227`）/ `evt_presence_N`（`count = 100L + seq`，`:242`）；未命中前缀的 topic 仍消耗 seq 但不发帧（`:200-246` 的 `when` 无 default）；
末尾自约下一次（`:248-250`）。建连耗时默认 80ms 但可注入 0（`:43`、`:76`、`:277`）。

### 4.9 十个测试文件各自保证什么

**公共测试基建 `RealtimeTestDoubles.kt`**（`:21-31` 说明为什么这样搭：
commonTest 无 `kotlinx-coroutines-test`，且 commonTest 要连 ohosX64 一起编译 ⇒ **禁止 Ktor 类型**；协程统一挂 `Dispatchers.Unconfined` ⇒ 「推一帧入站 ⇒ 状态机在同一调用栈里跑完」）：

| 假件 | 提供的能力 | 行号 |
|---|---|---|
| `SteppingRealtimeClock` | 可步进墙钟 | `:34-39` |
| `ManualRealtimeTicker` | 只登记不启动；`createdDelays` / `pendingDelays` / `runFirst` / `runAtMost` / `runWithDelay` | `:45-95` |
| `ImmediateRealtimeTicker` | 登记即触发，专为 `await()` 型等待（否则永久挂起） | `:97-113` |
| `RealtimeTestScope` | Unconfined 作用域 + `activeChildren()` + `cancel()` | `:115-126` |
| `FakeRealtimeWsTransport` | 记录 `sent`/`dialedUrls`/`closeCalls`/`disposedCount`，可注入 `connectFailure`/`sendFailure`，`pushInbound`/`pushClosed` 手动推帧 | `:128-193` |
| `RecordingOutboundStore` | 委托生产 InMemory + `statusLog` | `:195-212` |
| `RecordingTelemetrySink` | `events`/`ofType`/`lastPayload` | `:214-227` |
| `FakeSessionPort` | 无 token 时抛含「token」的 `IllegalStateException` | `:229-243` |
| `RecordingNotifyHandler` / `FakeSessionGuard` / `RecordingNetworkMonitor` | handled 列表 / 可配恢复结果 + handledErrors / start·stop 计数 + `fireOnline` | `:245-288` |
| `FakeRealtimeClient` | 只记录闸门下发的指令，不跑状态机 | `:290-361` |
| `RecordingRawApiClient` | 响应队列 + `failNext(times,error)`（**必须只失败一次**，否则鉴权恢复分支的递归会栈溢出，`:376-385`）；未实装的 ApiClient 方法一律 `AssertionError` | `:363-443` |
| 夹具 | `TICKET_OK_BODY`、`syncBody()`、`EMPTY_SYNC_BODY`、`eventEnvelope()`、`controlEnvelope()` | `:445-483` |

**`AppRealtimeClientImplTest.kt`（24 个用例，断言意图逐条）**：

| 用例 | 钉住的语义 |
|---|---|
| `:132-141` | 换票 body 就是裸 `{"platform":"mobile"}`、`wsUrl` **原样拨号**、状态阶梯只有 `Connecting → Authenticating` |
| `:143-158` | `auth` 帧三键 `ticket/platform/connId` + 帧 id 来自 `newFrameId` + `ts` 来自注入时钟 + `ws_connect` 是 **trace** 且带 `mock=false` |
| `:160-185` | `auth_ok` 后心跳第一拍恰为 25000ms；未活跃时 `subscribeTopics` 只登记不发帧，连上后补发 `sub`；`/realtime/sync` body 是 `{"sinceSeq":0,"topics":["sys.notify"]}`；网络监听只挂一次 |
| `:187-195` | 未登录 ⇒ 不换票、不拨号、状态不变 |
| `:197-207` | 在途与已连两种情形都去重（换票只发生一次）⇒ 防多条 WS |
| `:209-224` | `prepare()` 读持久化水位 + 用 pending 视图覆盖 depth；**第二次 prepare 被 `_initialized` 吃掉 ⇒ 不重新读盘** |
| `:229-252` | sync 补投：`sinceSeq` 取盘上水位；两条事件都进路由，但只有 `sys.notify` 进 handler；水位抬到 `latestSeq`（比事件里最大 seq 更高也照抬）并落盘 |
| `:254-265` | sync 失败只发 `ws_sync_fail`，连接仍是 Connected、不排重连、心跳计时仍在 |
| `:267-284` | 旧 seq 不进路由但仍交给 notify handler（两条独立路径）；新 seq 正常进路由并抬水位 |
| `:286-298` | pong/error/sub/unsub/ping/auth/ack 七种控制帧都不进任何业务频道 |
| `:300-310` | `error` 帧只产生 `ws_server_error`，payload 里能看到 code 4003；状态与计数不变 |
| `:314-342` | 出站帧 `payload` 合并了 `name`；**帧 id = messageId**；`markSent` 后 pending 视图状态是 `sent`；心跳 25s 与 ack 超时 15s **并行**；收到 `ack.refId` ⇒ 深度归零、行被删除、状态流转日志只有 `sent` |
| `:344-359` | 15s 无 ack ⇒ `sent → failed`、发 `ws_outbound_ack_timeout`、行**仍在表里但不在 pending 视图**，而 `depth` 仍为 1（markFailed 不动计数器） |
| `:361-369` | 不活跃时 `sendEvent` ⇒ 入队 + 置 Degraded + **不发帧** |
| `:371-387` | 离线期消息在重连后被重放，顺序是 `auth` 之后、`sub` 之前，payload 原样恢复 |
| `:389-399` | `requireAck=false` ⇒ 不起 15s 计时、直接 `markAcked`（深度归零、行被删） |
| `:403-420` | 关闭码 4002 ⇒ 永久停：Disconnected、重连计数 0、无待命定时器、先记 `ws_ticket_refresh{4002}` 再记 `ws_transport_lost{reason=ws_close_4002,closeCode=4002}`、错误交给 guard（message 含 `ws_kicked`）、传输只被 close 一次且 code/reason 都是 null |
| `:422-443` | 4001 ⇒ `reconnectPolicy.reset()` 生效：下一档仍是 1000ms 而不是 2000ms，重连计数递增，三次拨号三次换票 |
| `:445-462` | 普通断线（1005）走指数退避：`ws_reconnect_scheduled{reason=ws_close_1005,delayMs=1000,attempt=1}`，第二次是 2000ms |
| `:464-476` | **闩生效**：连推三次 closed 只算一次 transport lost、只排一次重连 |
| `:479-507` | `RealtimeWsError.Unsupported` ⇒ 永久停止（Disconnected、计数 0、**没有任何定时器**）+ `ws_connect_fail` |
| `:509-519` | 换票 401 ⇒ 停重连（同上断言形状） |
| `:521-535` | 401 + `tryRecover()` 成功 ⇒ **递归**再换一次票（2 次换票、1 次拨号、状态 Authenticating、无退避定时器） |
| `:537-548` | 换票 403 ⇒ **不退化为停重连**：正常退避 1000ms，且**不**打扰 guard |
| `:550-575` | 心跳两拍丢失 ⇒ 归一化为 `heartbeat_timeout` 重连；ping 帧 id 就是 `ping_<clock>` |
| `:577-590` | id 精确匹配的 pong 取消 10s 计时；id 不匹配的 pong 完全无效 |
| `:592-607` | 已连时「恢复联网」不再起第二条 WS；不活跃时才补连 |
| `:609-626` | 手动 `disconnect(clearQueue=true)` ⇒ 以 1000 关闭、清空队列、清所有定时器，且之后恢复联网与任何定时器都拉不起 WS |
| `:628-649` | `Paused` 且未开保活 ⇒ 以 `app_paused` 断开，且这次断开是「手动」的 ⇒ `Resumed` 不自动补连；开 `setKeepAliveInBackground(true)` 后 `Paused` 不断开 |
| `:651-663` | 非手动断开（1006）后 `Resumed` ⇒ 立即补连到 Authenticating |
| `:665-682` | `dispose()` ⇒ 以 `dispose` 关闭、传输释放一次、监听 stop 一次、无待命定时器、路由登记表清空 ⇒ 后续入站帧不再投递 |
| `:684-696` | 发送异常**不被吞**（抛给调用方），但消息已在队列里（深度仍为 1） |

**`RealtimeStartupGateTest.kt`（13 个用例）**：
`:74-86` 隐私未同意 ⇒ 不连且**不打印**「not logged in」那一行（检查顺序本身就是契约）；`:88-97` 隐私过了但未登录 ⇒ 不连；`:99-119` Connected 与 Degraded 都算活跃 ⇒ 不重复连（日志用中文 label）；
`:121-144` 快乐路径：连上 ⇒ 只花掉 12s 那一拍（8s 短路）、固定订阅 `sys.notify,presence.bulk`、横幅文案 `… success=true state=已连接 lastSeq=7 topics=sys.notify,presence.bulk`；`:146-153` 自定义 `defaultTopics` 原样转发；
`:155-171` 两个超时**都只打日志**：`createdDelays == [12000, 8000]`，connect 调用仍 1 次、订阅仍下发；`:173-186` 两个链路口各带自己的 trigger；`:188-195` 登出 ⇒ `("logout", clearQueue=false)`；
`:197-208` 切环境 ⇒ `("env_changed", false)` 然后重连；`:210-223` 非具体实现走兜底分支：只有 Resumed 且不活跃才补一次 connect，Paused/Other 不动；
`:225-238` `prepare()` 缓存 impl 句柄 ⇒ `hasLifecycleCapableClient` true，`dispose()` 后 false；`:240-272` 真 impl 被驱动到 Authenticating（帧里 `ticket=tk-1`），闸门仍把 12s/8s 两拍走完并打「not ready」日志。

**`MockRealtimeWsTransportTest.kt`（16 个用例）**：
`:85-92` 未连接就 `send` ⇒ `IllegalStateException` 且消息含 "not connected"；`:94-104` `connect` 复位上一条会话（二次拨号后 pending 定时器为空）；
`:106-117` `auth_ok` 载荷逐字（`auth_ok_5000`、`mock_user`、`mock_session_5000`、`serverTime`）；`:119-129` **pong 原样回带 ping id**（心跳靠这个匹配）；`:131-143` `sub` ⇒ ack 带 `refId` 与按登记顺序的 topics 快照，并排 20s 推送；
`:145-153` 未鉴权时 `sub` 只回 ack、**不排推送**；`:155-166` `unsub` 清空 topic ⇒ 停推送；`:168-183` `event` ⇒ `ack{refId, accepted:true}` 且**不带** topics 键；
`:185-191` 未知出站类型不回帧；`:193-203` `close()` 复位但**不**往 closed 通道发；`:205-215` `close()` 取消已排的推送；`:217-230` `simulateClose` 三码依次发布且 `requiresNewTicket` 三真；`:232-252` 重拨后必须先重新鉴权，`sub` 才会再排推送；
`:254-291` **一拍三 topic 各占一个 seq**（`1,2,3` + 三个不同 id 前缀 + 字段逐值：`seatIndex = seq % 4L`、`count = 100+seq`、`roomId = topic.removePrefix`）；
`:293-307` 自约下一次 ⇒ `createdDelays` 三个 20000，`presence.bulk` 也命中 `presence.` 前缀；`:309-324` 未命中前缀的 topic **空转仍推进 seq**；`:326-341` 部分退订 ⇒ 剩余 topic 继续推且 seq 从 1 计；
`:343-353` `dispose()` 静默复位；`:355-364` `replay = 0` ⇒ 迟到订阅者不重放。

**`HeartbeatSchedulerTest.kt`（12 个用例）**：
`:38-44` start 只排一个 25s；`:46-53` 重复 start 不叠定时器；`:55-63` ping id 格式 `ping_<epochMillis>` 且暴露为 pending；`:65-72` 一次 ping 后 10s 与 25s **并行**；`:74-91` 先制造 miss 再用同 id pong 归零；`:93-102` 错 id 的 pong 完全无效；
`:104-117` 单次 miss 不触发重连、只重排（并断言旧 interval 被取消）；`:119-134` 连续第 2 次 miss ⇒ 触发一次重连且**不再排下一次**；`:136-148` 两次 miss 之间来 pong ⇒ 计数归零，第三拍不致命；`:150-162` 发送异常被吞、计时照跑；`:164-174` `stop()` 取消全部并清计数；
`:176-192` `maxMiss=1` 时第一拍即致命（验边界而非常量）；`:194-207` 自定义 interval/timeout 参被尊重（`[1000, 250, 1000]`）。

**`RealtimeOutboundQueueTest.kt`（16 个用例）**：
`:37-52` enqueue 落 PENDING 行且 `createdAt == updatedAt`，**返回 id 前 depth 已 +1**；`:54-60` 逐条计数；`:62-73` `markSent` 只改状态与 `updatedAt`，depth 不动；
`:75-84` `markAcked` 删行并 -1；`:86-97` 对不存在的 id 反复 ack 也不会负数（夹到 0）；`:99-108` `markFailed` 行留着但出不了 pending 视图，depth 仍 1；`:110-122` `pendingItems()` 用真实行数**覆盖** depth；
`:124-134` 按 `createdAt` 升序；`:136-150` 模拟进程重启 ⇒ `refreshDepth()` 同步真实行数；`:152-160` `clear()` 双归零；`:162-182` 同 messageId 的 upsert 是**整行替换**（SQLite `ConflictAlgorithm.replace` 等价）；
`:184-201` `decodePayload()` 对垃圾与 JSON 数组都回空对象；`:203-208` 三个状态字面量拼写不得改；`:210-219` `fromRow` 不带时间戳；`:221-231` InMemory 实现对未知 id 的状态更新静默忽略。

**`RealtimeSeqStoreTest.kt`（16 个用例）**：
`:27-31` 两个 prefs 键名字面量；`:33-42` 新 store 从 0 起、接受首个 seq 且**立即落盘**；`:44-55` `load()` 对 null / "" / "abc" / "1.5" / " 12 " 全部归 0；`:57-62` 正常读回 41；`:64-74` 等号与更小都拒、`null` 恒接受且**不抬水位**；
`:76-85` 被拒的 seq 不重写存储；`:87-100` `setLastSeq` 只抬不降、等号不写；`:102-110` `save()` 显式写且**不碰** dedup 键；`:112-122` 水位能跨实例接上（断线重启后 sync 起点正确）；`:126-135` 空 notifyId 恒放行且不记账；`:137-145` 首见放行、重复拒绝、新 id 插**头部**；
`:147-161` 落盘为 JSON 数组且能 reload；`:163-178` 脏 JSON ⇒ 清空环且不抛（环清后老 id 会再展示一次）；`:180-186` 非数组同样清空；`:188-196` 空串 ⇒ **保留**内存环（对应 Dart 的提前 return）；`:198-212` 超过 200 从尾部截断，被截掉的 id 重新放行；
`:214-225` 落盘的是**截断后**的环；`:227-234` 数字/布尔元素也被 `toString` 收进环。

**`RealtimeInboundRouterTest.kt`（17 个用例）**：
`:46-52` 懒注册后该 topic 才收得到；`:54-60` 未登记 topic 静默丢弃；`:62-68` 别的 topic 不漏进已登记 topic；`:70-77` `topic == null` 不进 topic 频道但进 event 频道；`:79-89` 八种控制帧都不进 event 频道；`:91-100` `type=unknown` 的帧不投任何频道；
`:102-109` 按名过滤发生在下游；`:111-121` `name` 非字符串 ⇒ 根本不产生事件名 ⇒ 过滤不命中；`:123-130` 同 topic 多观察者都收到；`:132-141` `dispose()` 后已交出的 Flow 变静默；`:143-152` 200 条连投不阻塞且最后一条 seq 到位；
`:154-157` 无过滤的 `watchEvents()` 返回**同一实例**；`:159-163` 同 topic 复用同一频道、不同 topic 不同实例；`:165-173` 过滤后的 Flow 仍可继续 compose；`:175-180` 空串是合法 topic 键；`:182-190` 无订阅者时 dispatch 不抛，且**注册前**的帧不重放。

**`RealtimeAuthFailureTest.kt`（16 个用例）**：
`:25-30` 三个业务码；`:32-44` `isForceLogout` 只含 10021/10022、`isTryRefresh` 只含 10023；`:46-64` 互踢码无需 HTTP status、401 是硬失败、403 不是、10023 是；`:66-72` **403 短路优先于文案关键词**；
`:74-84` 文案兜底只在 `code == null` 时生效（`code=50000` + "token expired" ⇒ false）；`:86-92` 八个关键词逐个命中、无关文案不命中；`:96-121` 三类 `NetworkError` 的分流 + `TokenExpired` 恒硬 + 5xx 不算鉴权废；
`:123-131` `IllegalStateException` 只看自身文案（含 token/未登录才算）；`:133-139` 其它 Throwable 走整条 message 判定；`:141-146` `RealtimeWsError.Unsupported` / `TicketInvalid` **不是**鉴权失败（另有专门停止分支，不能混淆）；
`:148-154` 注入的策略覆盖默认规则。

**`RealtimeStateAndTelemetryTest.kt`（10 个用例）**：
`:22-35` `isActive` 恰为 Connected/Degraded 两个（逐枚举值遍历 + 反向断言）；`:37-50` 七个中文 label 逐字；`:52-75` metric 的 payload 键序 `type,name,` + params **平铺**、无 params 时只有两键；
`:77-93` error 在 params **之后**追加 `error`，且 `toString()` 含异常类型名；`:95-104` trace 追加 `durationMs`；`:106-130` 三个类型字面量 + 三个不同事件名；`:132-140` 默认 sink 只打日志、不抛。

---

## 5. 真实状态矩阵（禁止粉饰）

### 5.1 逐端口

| 端口 | 契约 | 真实实现 | stub / mock | 未接线部分 |
|---|---|---|---|---|
| `auth` | 3 个 object | **真实**：`AuthGate`、`AuthSessionState`、`AuthSessionActions` 都被 `feature/auth`、`feature/shell`、`feature/mine`、`composeApp/app` 真实读写（见 3.1 的消费者列） | 无 | 无 |
| `routing` | `CrossFeatureScreenPort` | **真实**：装配层实现 `AppCrossFeatureScreens.kt:29-47`，由 `ShellBootstrap.kt:24` 安装 | 无 | 未安装 ⇒ `require()` 直接 `error`（`CrossFeatureScreenPort.kt:64-68`）——这是设计上的硬失败，不是遗漏 |
| `chat`（`ChatPort`） | 2 成员接口 | **无真实实现**。唯一实现 `StubChatPort` 的 `isReady` **恒 false**、`ensureLoggedIn` **恒 `Result.failure(IllegalStateException("Chat SDK not wired"))`**（`ChatPort.kt:12-16`） | 是 stub | 而且**没有任何调用点**：`AppContainer.kt:42` 装了它，全仓库无人读 `bridges.chat` |
| `chat`（`ImEngine`） | 11 成员接口 | **只有 mock**：`ImEngineStore.engine` 是 `MockImEngine(seedDemo = true)`（`ImEngine.kt:145-147`）；数据来自内置三个 `mock_peer_01..03`（`:179-181`），发送流水线是 `delay(280)` / `delay(2000)` 的**计时表演**（`:336-349`），撤回是本地替换成 System 气泡（`:283-288`） | 是 mock，但**已被 UI 当真实能力使用**（`ChatScreen.kt:57`、`NativeJetpackTabs.kt:94`、`FriendDirectory.kt:41`/`:51`） | 无任何网络/SDK 调用；`platform-gap-registry.md:41` 把 RongCloud IM 三端登记为 `missing`，并注明「`USE_MOCK_IM` 开关也未对等」 |
| `pay` | 1 接口 + 2 适配器 | **三端适配器函数全为 `null`**：`PlatformPayAdapters.android.kt:9`/`:18`、`PlatformPayAdapters.ios.kt:4`/`:7`、`PlatformPayAdapters.ohos.kt:7`/`:9` | 真实可用的是 `SandboxPayChannelAdapter`，它**无条件**返回 `Success(sandbox = true)`（`PayGateway.kt:67-68`） | 净行为：`sandboxEnabled` 默认 true（`:51`）⇒ `availableChannels()` 非空、`pay()` 返回假成功；`FlaggedPayGateway` 自己「不伪造 Success」（`:99`），伪造来自适配器。`platform-gap-registry.md:44` 三端 `missing`；`must-matrix.md:13` 标 **Partial** 且写「Not production charge」；`tasks-ultra-fine-v2.md:180` 记「**与 v1 G.1『无假 Success』自相矛盾，需复核**」，`:186` 的 M5.5 仍是未勾选项 |
| `push` | 4 成员接口 + 上报类 | **`PlatformPushSdk` 三端 `fetchRegistrationToken()` 恒 `null`**（`PlatformPushSdk.android.kt:12`、`PlatformPushSdk.ios.kt:12`、`PlatformPushSdk.ohos.kt:12`），`platformName()` 只回 `"android"/"ios"/"ohos"` | `StubPushBridge.uploadToken` **不发网络请求就返回 `Result.success(Unit)`**（`PushBridge.kt:60-64`，原地挂 TODO）；`PushRegistrationApi` 是完整可用的 HTTP 实现，但异常一律吞（`PushRegistrationApi.kt:68-71`） | `PushRegistrationApi` 无任何构造/调用点；`registerForPush()` 唯一调用点在 `NativeAndroidApp.kt:67` 且被 `runCatching` 包住；`handleNotificationClick` / `currentToken` 无生产调用点 ⇒ `must-matrix.md:14` 标 **Partial**、「Vendor SDK not wired ⇒ not release-ready Must」 |
| `realtime` | 1 主接口 + 7 缝隙 | 逻辑层**完整**（4.1-4.8 全部由代码与测试支撑） | `MockRealtimeWsTransport` 是**显式选路**的模拟网关，开关 `useMockGateway` 生产默认 false（`RealtimeConfig.kt:18`）；`InMemoryRealtimeKeyValueStore` / `InMemoryRealtimeOutboundStore` 是仅有的落地实现 | **整条 lane 未接线**（5.2）；ohosX64 无 WS 产物 ⇒ 工厂抛 `Unsupported`（`RealtimeWsTransportFactory.ohosX64.kt:17-19`）；无跨端 connectivity ⇒ 默认 `NoopRealtimeNetworkMonitor`（`AppRealtimeClient.kt:122-136`），`platform-gap-registry.md:38-39` 标 partial/partial/**missing** 与 connectivity **missing** |
| `webview` | 1 internal object + 1 常量 | `OfflineWebFixtureUrl` **真实可用**并被三端使用（3.7） | `WebViewComponent.JS_BRIDGE_NAME`（`WebViewComponent.kt:11-13`）是无人引用的归属标记；真正的 WebView 能力还在 `core.platform.PlatformWebView` | `platform-gap-registry.md:24` 把 WebView+JS 桥记为 ready/partial/partial |

### 5.2 「未接线」的四条硬证据

| 断言 | 证据 |
|---|---|
| realtime 状态机**没有任何生产构造点** | `AppRealtimeClientImpl(` 只出现在 4 个位置，全部在 commonTest：`AppRealtimeClientImplTest.kt:74`、`:483`、`RealtimeStartupGateTest.kt:57`、`:245` |
| 闸门也没有 | `RealtimeStartupGate(` 仅 `RealtimeStartupGateTest.kt:46` |
| feature / composeApp / core 里没有一处 import `component.realtime` | 全仓搜索 `component.realtime` / `AppRealtimeClient` / `RealtimeSeqStore` 在 `feature`、`composeApp`、`core`、`scripts` 四个根下**零命中** |
| `RealtimeSeqStore` 至今**只活在内存里，没有接 `core.storage.KeyValueStore`** | 见 5.3 |

### 5.3 SeqStore 现在存在哪 —— 用代码说话

结论：**只有进程内内存，未接任何平台持久化，也没有生产持有者。**

1. 契约是组件层**自带**的 2 方法接口：`RealtimeKeyValueStore { getString; setString }`（`RealtimeSeqStore.kt:23-27`），同步语义的理由写在 `:14-22`（对齐 Dart `SpManager` 的 `unawaited(save())`）。
2. 仓库里 `RealtimeKeyValueStore` 的**唯一实现**是 `InMemoryRealtimeKeyValueStore`，内部就是一个 `MutableMap`（`RealtimeSeqStore.kt:30-40`），`:29` 的注释自己写明「单测与 ohosX64 占位用（**不假装跨进程持久化**）」。
3. 它的**全部构造点都在测试**：`AppRealtimeClientImplTest.kt:46`、`RealtimeStartupGateTest.kt:61`、`:249`、
 `RealtimeSeqStoreTest.kt:35`、`:51`、`:59`、`:66`、`:78`、`:89`、`:104`、`:115`、`:129`、`:149`、`:167`、`:182`、`:191`、`:201`、`:216`、`:230`。
4. `:component` **完全没有引用** `core.storage.KeyValueStore`（`core/storage/KeyValueStore.kt:28-52` 提供的是`getString/putString/getBoolean/…/remove` 的更宽接口，`:58` 的 `expect fun createKeyValueStore(name)` 才落到
 Android SharedPreferences / iOS NSUserDefaults / OHOS Preferences；`:15-22` 说明后端分布）。
 `RealtimeKeyValueStore` 的适配缺口因此**至今没有实现者**——`AccountSessionStore.kt:24-25`给同类需求做的是 `KeyValueAccountSessionStore(createKeyValueStore(ACCOUNT_STORE_NAME))`，realtime 这边**没有对应物**。
5. `NotifyDedupStore` 同样没有生产持有者（`RealtimeSeqStore.kt:107-151`，全仓引用只有自身 + 测试），而 `RealtimeStartupGate.kt:46-49` 明确把「装配层自己去 `load()`」这件事留在了自己职责之外。
6. 两处 openspec 记录与代码一致：
 `openspec/changes/unify-core-kv-storage/proposal.md:43` ——「不改 `RealtimeKeyValueStore`（`:component`，仅内存实现、未接产品链路）——接线时再改为消费 `KeyValueStore`」；
 `openspec/changes/sync-flutter-full-parity/tasks-ultra-fine-v2.md:338` ——「`component/realtime` 的 `RealtimeKeyValueStore` 仍自带 InMemory 实现、生产侧无构造点 → 归 M2 接线（任务 #23）」。
另外 `platform-gap-registry.md:51` 把 OHOS 持久化目录记为三端 `missing`，点名文件就是`component/realtime/RealtimeSeqStore`。

因此 `realtime_last_seq` / `realtime_notify_dedup_json` 这两个键（`RealtimeSeqStore.kt:43-46`）**今天不会跨进程留下任何东西**；
「重启后 sync 起点接得上」目前只由 `RealtimeSeqStoreTest.kt:112-122` 在**同一个 `InMemoryRealtimeKeyValueStore` 实例**上验证。
出站队列同理：`RealtimeOutboundStore` 只有 `InMemoryRealtimeOutboundStore`（`RealtimeOutboundQueue.kt:61-86`）和测试替身 `RecordingOutboundStore`（`RealtimeTestDoubles.kt:196`）两个实现，
Dart 的 SQLite 表 `ws_outbound_queue` 在这里只是 KDoc 里的语义注记（`:9-15`）。

---

## 6. auth 专题：会话态、Actions、401 联动与 token 落点

### 6.1 会话状态的三套「同一真值」

`AuthSessionState` 用一次 `publish()` 同时写三处（`AuthSessionState.kt:43-47`）：
Compose 字段 `isLoggedIn` / `displayName`（`:27-31`，`private set`）与 `StateFlow<Snapshot>`（`:24-25`）。
`:11-17` 的 KDoc 声明它是「UI 软闸门的唯一响应式会话真值」（ADR 0001），新代码优先收 `snapshot`，旧字段是 Spike I 迁移期的兼容口。
写入方只有三个：`sync()`（从 `AccountFacade` 拉）、`clearLocal()`、`publish()` —— 因此「外部直接改 isLoggedIn」在编译期就不可能（`private set`）。

`AuthGate.pendingTab` 是**独立**的一份 UI 态（`AuthGate.kt:16`），它不反映会话、只反映「用户想去的受保护 tab」。
`requiresAuth` 的双轨判定值得注意：`:22-23` 同时按 `MainTab.name` 与 `tab.route` 判，`:19-21` 的注释说明原因——枚举名保证「条目还没加进 AppRoutes 也能用」，route 前缀保证「shell 加上后仍然对」。
而 `core/router/…/AppRoutes.kt:42-45` 里 `Chat` 与 `Community` 本身也已经标了 `requiresAuth = true`，所以 `AuthGate` 的字符串匹配是**第二道**而非唯一来源。

### 6.2 Actions 的事务

`AuthSessionActions.logout()`（`AuthSessionActions.kt:12-16`）三步的顺序是刻意的：
先 `AccountFacade.logout()`（清 KV 会话 + `NetworkFacade.clearAccessToken()`，`AccountFacade.kt:60-65`），再 `AuthSessionState.clearLocal()`（UI 态），最后 `AuthGate.clearPending()`（待恢复 tab）。
`KDoc :5-10` 声明它和 `AuthRepository.logout()` 是**同一事务**——`AuthRepository.kt:200-202` 的`fun logout()` 就是把整个实现委托给 `AuthSessionActions.logout()`；`logoutWithRemote()`（`:210-236`）在三种情形下也调它：远端成功（`:218`）、
远端失败但判定会话已废（`:223`/`:228`，判定函数 `isLogoutSessionGone` 在 `:242-261`）。
`feature/mine/MineProfileLogic.kt:24` 是**跨 feature** 调用的实例——这正是它住在 `:component` 的原因。

### 6.3 401 联动：谁在什么时机把这三样一起清掉

链路是**响应式的、由 `core/network` 触发，`feature/auth` 提供回调，`:component` 提供清理口**：

1. 装配期注册：`AuthSessionBootstrap.install()` 先 `AuthSessionState.sync()`，再把 `AuthTokenRefresh.policy` / `AuthTokenRefresh.handler` / `onSessionCleared = { AuthTokenRefresh.handleRefreshExhausted() }`
交给 `NetworkFacade.setAuthRefreshSupport(...)`（`AuthSessionBootstrap.kt:18-27`；实现在 `NetworkFacade.kt:57-66`）。
入口是 `ShellBootstrap.install()`（`ShellBootstrap.kt:23`）。
2. 运行期分派：`AuthRefreshingApiClient` 对每个响应按 policy 决定`PassThrough` / `ForceLogout` / `Refresh`；`ForceLogout` 分支**直接调用 `onSessionCleared?.invoke()`**
并原样返回响应（`TokenRefresh.kt:139-146`；决策枚举 `TokenRefresh.kt:14`）。
3. 会话清理：`AuthTokenRefresh.handleRefreshExhausted()`（`AuthTokenRefresh.kt:99-104`）先 `if (!AccountFacade.current().isLoggedIn) return`（幂等 + token 已废不再打远端 logout），
然后 `AccountFacade.logout()` → `AuthSessionState.clearLocal()` → `AuthGate.clearPending()`。
4. 业务侧同类清理：`AuthRepository.handleAuthFailure`（`:268-276`）与`clearSessionIfGone`（`:279-289`）也各清 `AuthSessionState` + `AuthGate`（`:272-273`、`:285-286`）；
登录成功路径 `commitSession` 收尾必调 `AuthSessionState.sync()`（`:355`、`:380`）。

`policy` 的映射表在 `AuthTokenRefresh.kt:31-37`（把 `feature/auth/AuthHttpDecision` 的三档结论转成刷新决策），单飞刷新在 `:46-61`，真实续期在 `:63-93`。
realtime lane 侧的对应物是**另一份**等价规则（`RealtimeAuthFailure.kt:12-16` 明确要求两处同步变更）：
两边都同意「401 ⇒ 会话已废，别再退避；403 ⇒ 不是会话已废」。

### 6.4 token 到底存在哪

- 结构：`AccountSession` 含 `token` / `refreshToken` / `sessionId` / `deviceId` 等（`AccountFacade.kt:13-33`）；
判定条件 `isLoggedIn = token.isNotBlank()`（`:228`），`canRefresh = refreshToken.isNotBlank()`（`:231`）。
- 写盘：`AccountFacade.setSession` 会 `ensureHydrated()` 并把 token 绑给网络层（`NetworkFacade.bindAccessToken(value.token)` / `clearAccessToken()`，`:49-58`），
随后 `persist(...)` 走 `AccountUserSnapshot`（`:190-197`）。
- 存储实现：`createAccountSessionStore() = KeyValueAccountSessionStore(createKeyValueStore("demo_account"))`（`AccountSessionStore.kt:20-25`），键 `demo_account_user_json`（`:22`），
 **空串与缺失等价 ⇒ 存成 remove**（`:36-42`），`clear()` 也是 remove（`:44-46`）。
- 落到的平台介质由 `core/storage` 决定：Android SharedPreferences / iOS NSUserDefaults（`settingsMain` 线）、OHOS 两个 arch 直接实现 `OH_Preferences` NDK（`KeyValueStore.kt:15-22`、`AGENTS.md:54-56`）。
- 启动时回灌：`ensureHydrated()` 读表并重新绑 token（`AccountFacade.kt:178-188`）。

对照 `:component` 自己：**auth 端口不碰磁盘**（`AuthSessionState` 的写入全部来自 `AccountFacade`），而 realtime 想碰磁盘却没有实现（5.3）——这是本模块里两种「持久化态度」的鲜明对比。

---

## 7. routing / `CrossFeatureScreenPort`

定义（`CrossFeatureScreenPort.kt:18-49`）5 个成员，全部 `@Composable` 除第一个：

| 成员 | 用途 | 行号 |
|---|---|---|
| `handlesSiblingScreen(route: String): Boolean` | 该 route 是否解析到**兄弟** feature 的屏幕 | `:24` |
| `RenderSiblingScreen(route, onBack, onNavigate)` | 渲染兄弟 feature 屏幕；只在上一条为 true 时调用 | `:27-28` |
| `ContentRouteScreen(...)` | content 的路由表（live / friend / music / media / ai / classroom / video） | `:31-32` |
| `MineRouteScreen(...)` | mine 的路由表（mall / wallet / settings / profile + 兄弟子屏） | `:35-36` |
| `HomeRouteScreen(...)` | home 的路由表（search / strategy / learning report…） | `:39-40` |
| `SwitchStoreDialog(selectedId, onDismiss, onPicked)` | Flutter `SwitchStoreDialog`：mine 拥有的门店选择器，从 home 根弹出 | `:43-48` |

持有者 `CrossFeatureScreens`（`:55-116`）是本模块唯一的**运行时注入**样板：
`@kotlin.concurrent.Volatile private var installed`（`:57-58`）+`install(port)`（`:60-62`）+ 私有 `require()`，未安装时报错文案**直接点名该调谁**：
「shell entrypoints must run `com.example.my_kmp_project.app.ShellBootstrap.install()` before rendering」（`:64-68`），其余 5 个方法只是镜像端口的纯转发（`:70-115`，`onNavigate` 带 `{}` 默认值）。

feature 因此**不必**依赖兄弟 feature：`HomeScreen.kt:87`（Content）、`:92`（Mine）、`:147`（SwitchStoreDialog）；
`MineScreen.kt:81`（Home）、`:86`（Content）；`MineRouteHost.kt:79-80`（先 `handlesSiblingScreen` 再 `RenderSiblingScreen`）；`ContentRouteHost.kt:18`、`SecondaryRouteIsland.kt:92`。
实现方在装配层：`AppCrossFeatureScreens.kt:29-34`（`/classroom`、`/video`、`MineRoutes.Membership`）与`:36-47`（分派到 `ClassroomRouteHost` / `VideoRouteHost` / `MembershipScreen`），安装点 `ShellBootstrap.kt:24`。

设计口径写在 `:5-16` 的 KDoc：路由字符串一律用 `core/router` 的规范路径，本端口**不引入新路径**；惯用法刻意对齐 `NetworkFacade` 的运行时注入与 `AuthSessionBootstrap.install()` 的装配期接线；
`门禁`侧的对应说明在 `check-layer-deps.sh:4-5`（跨模块 UI 走 app 装配 + CrossFeatureScreens）。

---

## 8. 改动入口与坑

### 8.1 想改 X，就动 Y

| 想做的事 | 唯一入口 | 同时要看 |
|---|---|---|
| 让某个新 tab/路径需要登录 | `AuthGate.kt:25-31`（字符串名）与 `:33-39`（route 前缀） | `AppRoutes.kt:37-46` 的 `requiresAuth`；`AppShell.kt:310-317`、`SoftAuthPresenter.kt:43-51` |
| 加一个跨 feature 可渲染的屏幕 | 端口里加方法（`CrossFeatureScreenPort.kt:18-49`）+ 门面转发（`:70-115`）+ `AppCrossFeatureScreens.kt` 实现 | 装配必须在任何渲染之前跑 `ShellBootstrap.install()`（`:24`），否则 `require()` 直接 error |
| 接真实微信/支付宝 SDK | 只改三端 `platformWeChatPayAdapterOrNull` / `platformAlipayPayAdapterOrNull`（`PlatformPayAdapters.android.kt:7-19`、`PlatformPayAdapters.ios.kt:4`、`:7`、`PlatformPayAdapters.ohos.kt:7`、`:9`） | 真适配器需要把 `PayFeatureFlags.weChatEnabled/alipayEnabled` 打开（`PayGateway.kt:49-50`）否则 `isChannelOpen` 不放行（`:121-129`）；同时决定 sandbox 是否继续兜底（`:51`、`tasks-ultra-fine-v2.md:186` M5.5） |
| 接真实推送 token | 三端 `PlatformPushSdk.fetchRegistrationToken()`（`PlatformPushSdk.android.kt:12`、`PlatformPushSdk.ios.kt:12`、`PlatformPushSdk.ohos.kt:12`） | 拿到 token 后应改 `StubPushBridge.uploadToken`（`PushBridge.kt:60-64`）接 `PushRegistrationApi.report`（`PushRegistrationApi.kt:35-72`）；上报失败不会抛（`:68-71`），需要自己观测 `log` |
| 接 IM 厂商 SDK | `ChatPort`（`ChatPort.kt:7-10`）是为此预留的入口 | 但 UI 现在**绕开**它直连 `ImEngineStore.engine`（`ImEngine.kt:145-147`、`ChatScreen.kt:57`、`NativeJetpackTabs.kt:94`），替换要同时处理这条 mock 面 |
| 把 realtime 真正接上 | 装配层构造 `AppRealtimeClientImpl(...)`（`AppRealtimeClientImpl.kt:50-70`）→ `RealtimeStartupGate(...)`（`RealtimeStartupGate.kt:33-43`）→ 生命周期与登录链路转发（`:103-138`） | 必须补三件生产实现：`RealtimeSessionPort`（含「无 token 就抛」契约，`AppRealtimeClient.kt:82-91`）、`RealtimeNotifyHandler`（自己去用 `NotifyDedupStore`，`:108-116`）、`RealtimeKeyValueStore`（改接 `core.storage.KeyValueStore`）+ `RealtimeOutboundStore`（DB）；`RealtimeSessionGuard` 不注入则互踢分支静默失效（`AppRealtimeClientImpl.kt:65`、`:191-201`） |
| 调心跳/退避/ack 超时/去重上限 | `RealtimeConfig.kt:28-43`（全在 core，不在 component） | `HeartbeatScheduler` 与 `ReconnectPolicy` 的默认值引用它（`HeartbeatScheduler.kt:21-23`、`ReconnectPolicy.kt:21-25`）；改数值要同步改 `HeartbeatSchedulerTest.kt`、`ReconnectPolicyTest.kt` 的字面量断言 |
| 加一种入站帧类型 | `AppRealtimeClientImpl.kt:270-307` 的分派表 + 常量 `:633-641` | 若它是业务事件，还要进 `RealtimeInboundRouter.kt:49-51` 的 `type == "event"` 判定；控制帧需加进 `RealtimeEnvelope.kt:83-92` 的 `CONTROL_TYPES` |
| 加一条遥测 | `RealtimeTelemetry.kt:49-75` 三选一，别新开第四个 | payload 键序由 `:78-83` 决定（`type`/`name` 恒定在前），事件名 `realtime_<type>`；`RealtimeStateAndTelemetryTest.kt:52-130` 在钉键序 |

### 8.2 坑（每一条都有代码位置）

1. **`:component` 不在根 `subprojects {}` 接线范围内**（`component/build.gradle.kts:6-8`）：
新增 target/依赖只改这一个文件即可，但也意味着根构建脚本里的统一改动**不会**作用到它。
2. **`api` 透传是刻意的**（`:38-42`）：feature 只声明 `api(projects.component)` 就能看见 core 类型。
一旦把某个 `api` 降成 `implementation`，8 个 feature 模块与 `composeApp` 都会断。
3. **分层门禁的第三条轴专防 component→feature**（`check-layer-deps.sh:54-59`）；轴 2 会把 `component/` 下任何 `import …feature.` / `…app.` 判为违规（`:33-38`）。
这就是 `RealtimeAuthFailure.kt` 宁可复刻一份规则也不 import `feature/auth` 的原因（`:12-16`），代价是**两份规则必须人工同步**。
4. **`-PandroidOnly=true` 时 `iosMain`/`ohosMain` 根本不存在**（`component/build.gradle.kts:27-34`、`:49-61`）：
任何只写在 iOS/OHOS 源集的代码在 Android CI 上不会有任何编译信号。
5. **`commonTest` 要连 ohosX64 一起编译 ⇒ 禁止 Ktor 类型，且没有 `kotlinx-coroutines-test`**（`RealtimeTestDoubles.kt:21-31`、`component/build.gradle.kts:46-48`）。
新测试只能用 `RealtimeTicker`/`RealtimeClock` 缝隙 + `Dispatchers.Unconfined`；涉及 `CompletableDeferred.await()` 的用例**必须**用 `ImmediateRealtimeTicker`，否则永久挂起（`RealtimeTestDoubles.kt:97-113`）。
6. **`RealtimeTicker` 句柄是一次性的**：动作体第一行要把自己那个字段置 null，否则「动作里再排下一次」会取消掉正在运行的自己（`RealtimeScheduler.kt:33-39`，
代码遵守点 `HeartbeatScheduler.kt:61-68`、`AppRealtimeClientImpl.kt:419-422`、`:546-551`、`MockRealtimeWsTransport.kt:194-195`）。
7. **`authFailurePolicy` 与心跳的「反直觉」细节都是 Dart 等价性要求**，不许顺手修好：
心跳不等 pong 也排下一次、pong 只认 id（`HeartbeatScheduler.kt:9-16`）；`depth` 是自增计数器而 `pendingItems()` 会覆盖它（`RealtimeOutboundQueue.kt:18-21`）；`markSent/markFailed` 不动 depth（`:154-168`）；
去重环「空串 load 不清内存态、脏 JSON 才清」（`RealtimeSeqStore.kt:117-126`）；`acceptSeq` 等号算重复、`null` 恒接受（`:78-84`）；mock 的 `seq++` 在 topic 循环体内（`MockRealtimeWsTransport.kt:197-199`）。
8. **`STATE_REPLAY = 1` 是与 Dart 的刻意差别**（`AppRealtimeClientImpl.kt:630-631`）：
别按「broadcast 流不重放」的直觉把它改成 0，否则晚订阅的 UI 拿不到当前态。
9. **`transportLostHandled` 闩只能靠 `connectInternal` 复位**（`:92-97`、`:148`）：
删掉它会因为 Ktor 同时给 `closed` 与入站收尾而**双算重连**（`AppRealtimeClientImplTest.kt:464-476`）。
10. **`Failed` 状态不可达**（4.1）：`isActive` 与 `waitUntilConnected` 都会读它（`RealtimeStartupGate.kt:172`），但没有任何代码写入；想用它必须同时补状态机分支与对应测试，否则 8s 等待永远不会因它提前结束。
11. **`disconnect()` 会把 `_manualDisconnect` 置 true**（`AppRealtimeClientImpl.kt:437-438`），而 `Paused` 生命周期内部就是走 `disconnect("app_paused")`（`:586-590`）⇒ `Resumed` 不会自动补连。
这条由 `AppRealtimeClientImplTest.kt:628-649` 明确钉住，是最容易被误判为 bug 的行为。
12. **`RealtimeStartupGate` 的 `onAfterLogout` 不清队列、`onEnvChanged` 用默认参数（也不清）**（`RealtimeStartupGate.kt:113-125`，`RealtimeStartupGateTest.kt:188-208`）：
「切环境=清干净」是直觉，但不是 Dart 的行为。
13. **`PushRegistrationApi.report` 吞掉所有 `Throwable`**（`PushRegistrationApi.kt:68-71`）：
上报失败在日志里只有一行，调用方拿不到失败信号；`StubPushBridge.uploadToken` 更是**不请求就成功**（`PushBridge.kt:60-64`）。这两个「乐观」行为叠加时最容易高估推送可用性。
14. **`ImEngine` 与 `ChatPort` 是两套并行契约**（`ChatPort.kt:7-10` vs `ImEngine.kt:17-36`）：
前者是官方留给厂商 SDK 的口但无人调用，后者是 mock 却已被 UI 直接依赖；改动聊天能力时务必分清是在填哪个口（ADR 0005 的约束写在 `docs/adr/0005-three-long-lived-channels-separate-transports.md:23`：
 **不得**用 SSE 或自研 WS 伪造「聊天已接通」）。
15. **`ImEngine` 的展示逻辑内嵌在模型里且依赖 `Clock.System`**：`canRecall` 的 180s 窗口（`ImEngine.kt:72-76`）与 `ChatTimeFormat` 的手算 UTC+8（`:95-138`）都没有可注入时钟，跨时区/跨年 (`(days % 365)/30`) 的
 `listLabel` 只是近似文案（`:112-117`）——不要把它当日期库用。
16. **文档验收门禁**：本文每条引用要能被 `docs/deep-dive/_verify/verify_docs.py` 解析（`:12-15` 的引用正则、`:135-138` 的 PASS 条件、`:147-165` 的「范围内每个文件名必须被提及」）。

### 8.3 一句话状态

`:component` 的 ③ 层里，**auth 与 routing 是真实且已接线的薄契约**，
**pay / push / chat 是「端口真实、实现是 stub 或 mock、且假成功路径默认开着」**，
**realtime 是全模块唯一「逻辑完整、测试最密、生产零接线」**的部分——它的 seq 与去重至今只活在 `InMemoryRealtimeKeyValueStore` 里（5.3），出站表至今只有内存实现，
而 ohosX64 的 WS 工厂被明确设计成「抛 `Unsupported` 而不做伪实现」（`RealtimeWsTransportFactory.ohosX64.kt:10-19`）。

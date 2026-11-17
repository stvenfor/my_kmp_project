## 16A · feature:home 的数据与接口层

**定位**：`:feature:home` 是 17 个业务模块里**最大**的一个 —— commonMain **34 个 `.kt`、11 094 行**（`wc -l feature/home/src/commonMain/**/*.kt`），其中 `HomeRouteHost.kt` 单文件 3 574 行；commonTest 7 个文件 1 433 行。它同时是**唯一带「契约层 / mock 引擎」双轨结构**的模块：`api/` + `aftersales/` + `newcar/` 三个子包（12 个文件，1 502 行）是逐行照抄 Dart 侧 BFF 行为的**HTTP 契约层**，而根包下的 6 个 Store/MockData（1 041 行）是**进程内 mock 引擎**；两者同名类型并存、互不调用。分册分三部分：**16A = 数据与接口层**（本节）、16B = UI 组件与分区、16C = 路由宿主与详情页。本册只覆盖 A 的 20 个文件（+ `settings.gradle.kts`/根 `build.gradle.kts` 的接线证据）。

**核心结论（先给，证据见 16A.9）**：契约层 6 个 API 类共 24 个端点**已全部实现，共 **61 个测试用例**覆盖（49 个契约层 + 12 个 Store/打包器）**，但**生产代码里零调用**；界面上看到的所有 home 数据仍来自 `HomeMockData` + 4 个 `object` Store。唯一真正接了后端的 home 状态是收藏（`HomeFavoritesStore` → KV），而它是纯本地、无端点。

### 16A.1 模块依赖（build.gradle.kts 实证）

| 声明 | 行号 | 内容 / 后果 |
|---|---|---|
| `plugins { kotlinMultiplatform / androidLibrary / composeMultiplatform / composeCompiler }` | `feature/home/build.gradle.kts:3-8` | 四个插件；模块自己声明 Compose 插件（根工程注释 `build.gradle.kts:32-33` 说明 Compose 插件留给模块） |
| `val chartsVicoMain = maybeCreate("chartsVicoMain") { dependsOn(commonMain); implementation(libs.vico.compose) }` | `:14-20` | **中间 source set**。注释 `:12-13`：Vico 只在 Android + iOS 可用（CPF 无 ohos 产物），故图表实现不能进 `commonMain`，`commonMain` 只留 expect；`:17` 补充 Vico 2.3 传递 stock Compose 1.9.2，工程 `compose.*` 由 commonMain 胜出 |
| `getByName("androidMain").dependsOn(chartsVicoMain)` | `:21` | Android 挂上 Vico |
| `matching { it.name == "iosMain" }.all { dependsOn(chartsVicoMain) }` | `:23` | iOS 用 `matching` 挂载 —— 注释 `:22`：`iosMain` 由**根工程**共享接线创建，`-PandroidOnly=true` 时它不存在，所以必须 `matching` |
| `commonMain`: `api(projects.core.ui)` | `:27` | 注释 `:26`：`core:ui` 传递带出 `core.platform` → `core.design` / `core.account` + `compose.*`。这就是 `HomePointsStore` 能用 `mutableIntStateOf`、`HomeFavoritesStore` 能用 `core.platform.loadStringList` 的来源 |
| `commonMain`: `api(projects.core.router)` | `:28` | `HomeRoutes`/`MineRoutes` 等路由常量（`HomeTodoStore.kt:6-12` 全量 import） |
| `commonMain`: `api(projects.core.network)` | `:29` | 注释标注「含 `core.sse` 包」；契约层的 `ApiClient`/`ApiResponse`/`ListPage`/`NetworkJson`/`FlexibleIntSerializer`/`decodeListPage` 全出于此 |
| `commonMain`: `api(projects.component)` | `:30` | `component.webview` / `component.routing`（供 B/C 部分，本 A 部分文件**未使用**） |
| `commonTest`: `implementation(projects.core.network)` | `:32-34` | 测试直接依赖 core.network 才能实现 `ApiClient` 接口（`HomeRecordingApiClient`）—— 注意是 `implementation` 而非 `api`，测试不继承 commonMain 的传递依赖可见性时会需要这一行 |
| `android { namespace = "com.example.my_kmp_project.feature.home" }` | `:38` | 唯一在此模块声明的 Android 配置 |

**本模块自己不声明 target 与基础 Android 配置**：`:feature:home` 被根 `build.gradle.kts:35` 的 `libraryModulePaths`（`path.startsWith(":core:") || ":feature:"`）命中，于是从根 `subprojects {}` 获得 —— `compileSdk`、`minSdk`、Java 17（`build.gradle.kts:72-81`）、`androidTarget` + `iosX64/iosArm64/iosSimulatorArm64/ohosArm64/ohosX64`（`:87-102`，且注释 `:95-96` 明确**库模块一律不产 framework 与 sharedLib**）、`serialization` 插件（`:107`）、`commonMain` 的 coroutines-core / atomicFu / serialization-json 与 `commonTest` 的 kotlin-test（`:108-116`）、`iosMain`、`ohosMain` 层级（`:118-126`）。`-PandroidOnly=true`（`:57`）时 `skipNative` 生效，iOS/OHOS target 与 `iosMain` 都不创建 —— 这就是 `:23` 用 `matching` 的原因。

**上游接入**：`settings.gradle.kts:43` 的 `include(":feature:$it")` 按名字列表批量包含（`home` 在列表中）；`composeApp/build.gradle.kts:141` `implementation(projects.feature.home)`（在 OHOS/真机分支内），注释 `:114` 提到「Vico 图表 → `:feature:home`」。`:feature:home` **不被任何其它 feature 模块依赖**（AGENTS.md 规定 feature 间无兄弟依赖）。

### 16A.2 文件清单

| 路径（相对仓库根） | 行数 | 作用 | 关键行号 |
|---|---|---|---|
| `feature/home/build.gradle.kts` | 39 | 模块 Gradle：4 插件 + `chartsVicoMain` 中间 source set + 4 个 `api` 依赖 + namespace | `:14-23` Vico 挂载，`:25-34` 依赖 |
| `feature/home/src/commonMain/.../home/api/HomeApiWire.kt` | 177 | home 契约层公共读法：双信封、`data = null` vs 抛、裸 `Exception` vs `NetworkError.Business`、请求页 `hasMore`、字面 `true` | `:57` `getHomeListPage`、`:92` `getHomeListPageOrNull`、`:114` `homeDataOrThrow`、`:134` `homeBareDataOrThrow`、`:176` `homeWireLiteralTrue` |
| `.../home/api/HomeTimeWire.kt` | 37 | 无 `kotlinx-datetime` 下的 ISO 判定 + 按 `T` 截断 | `:18` `homeIsoDateTimeOrNull`、`:25` 正则、`:37` `homeDatePart` |
| `.../home/api/HomeTodoApi.kt` | 87 | 待办契约 #15–#21 + 空 body 动作 POST 扩展 | `:22-34` 路径常量、`:53/:57` approve/reject、`:85` `postActionOk` |
| `.../home/api/HomeTodoApiModels.kt` | 175 | #15–#21 HTTP DTO + `HomeTodoCardSize.fromType` | `:22-39` 尺寸枚举、`:43` `HomeTodoCard`、`:76` `HomeTodoJoinApplication` |
| `.../home/api/HomeTransactionApi.kt` | 94 | 流水契约 #12/#13（类名 `TransactionApi`），`limit`/`offset` 0-based + 逐行严格解码 | `:35` path、`:47` `fetchPage`、`:70` `getTransaction`、`:89` `decodeStrictTransactionPage` |
| `.../home/api/HomeTransactionModels.kt` | 72 | `TransactionModel`（**`id` 非空不兜底**）+ 两个 typealias；可空性策略规范出处 | `:14-22` 策略、`:26` `id`、`:69/:72` typealias |
| `.../home/api/PointsApi.kt` | 62 | 签到/积分 #22–#25，明确排除 #26 | `:24/:27/:30` path、`:50` `fetchTasks`（唯一用 `rootFallbackWhenDataMissing`）、`:59` `claimTask` |
| `.../home/api/PointsModels.kt` | 158 | #22–#25 DTO：`reward`/`today_reward` 缺省 **5**、布尔读 `JsonElement`、`TaskProgress.parse` | `:100-113` 枚举、`:124-139` `GrowthTask` |
| `.../home/aftersales/AfterSalesApi.kt` | 108 | 售后 #47–#50；`data` 裸 map 读法保住 `canCreate`；9 键互斥/条件 body | `:43-53` `fetchRecords`、`:81-94` `create` body |
| `.../home/aftersales/AfterSalesModels.kt` | 147 | `AfterSalesRecord`/`Appointment`/`ListResult` + 两个 Wire | `:112-116` `AfterSalesListResult`（非 Serializable）、`:138-141` `totalRaw` 可空 |
| `.../home/newcar/NewCarFollowApi.kt` | 187 | 新车跟进 #40–#46（7 个方法，home 最大 API 类）+ 两个请求体 DTO | `:58/:133` `getHomeListPageOrNull`、`:92-105` 互斥 body、`:169/:183` 请求 DTO |
| `.../home/newcar/NewCarFollowModels.kt` | 198 | `NewCarFollowTab`（中文 query 字面量）、`Stats`/`Summary`/`File`/`Log`、`NewCarFollowPage<T>` | `:21-28` 枚举、`:85-90` 字符串主键说明 |
| `.../home/HomeMockData.kt` | 360 | 静态 mock 目录（15 个 UI data class + `object HomeMockData`） | `:47` 封面 URL 生成、`:279` `hotRankDetailItems`、`:292` `rankItemsForTab` |
| `.../home/HomePointsStore.kt` | 107 | 签到状态机 mock + 弹窗 gate 纯函数 | `:26` `PrefsKey`、`:61` `shouldShowDialog`、`:89` `checkIn` |
| `.../home/HomeSecondaryStore.kt` | 292 | 二手车/售后/新车三条演示流水线（8 条种子 + validate/create） | `:151/:214/:265` 三个 validate、`:169/:226/:270` 三个 create |
| `.../home/HomeTodoStore.kt` | 124 | 待办 mock：3 条入店申请种子 + `todoCards()` | `:59/:67` approve/reject、`:76` `todoCards` |
| `.../home/HomeTodoModels.kt` | 86 | UI 侧待办模型 + `HomeTodoPacker`（2×2 装箱） | `:7-20` 尺寸、`:56/:62` 两个纯函数 |
| `.../home/HomeFavoritesStore.kt` | 72 | **唯一真落盘** Store：`home_favorite_service_ids_v2`，3/8 上下界 + 归一 | `:13` key、`:25-38` `normalize`、`:50/:57` remove/add |
| `feature/home/src/commonTest/.../home/api/HomeRecordingApiClient.kt` | 115 | 测试替身：记录请求 + 走真实 `ApiEnvelopeParser` | `:38-41` `record`、`:70` parse、`:113` `EMPTY_OK` |
| `.../home/api/HomeTodoApiTest.kt` | 186 | 7 用例锁 #15–#21 行为 | `:42-50` 尺寸、`:99-112` 空 body、`:151-185` 文案优先级 |
| `.../home/api/HomeTransactionApiTest.kt` | 155 | 9 用例锁 #12/#13 | `:31` limit/offset、`:112-117` 缺 id 必抛 |
| `.../home/api/PointsApiTest.kt` | 229 | 11 用例锁 #22–#25 | `:51` "yes"→false、`:183-210` 手搓转换器 |
| `.../home/aftersales/AfterSalesApiTest.kt` | 243 | 8 用例锁 #47–#50 | `:133-146` 总发键、`:214` `"null"` |
| `.../home/newcar/NewCarFollowApiTest.kt` | 282 | 13 用例锁 #40–#46 | `:73-86` 中文 tab、`:185/:199` 互斥分支、`:232` `{}` |
| `.../home/HomeLogicTest.kt` | 223 | 4 个测试类：Packer/Points/Secondary/Todo | `:22-69` 装箱、`:85` 重复审批 false |

其余 home 文件（`HomeRouteHost.kt` 3574 行、`HomeSections.kt` 989、`HomeSearchScreen.kt` 578、`HomeServiceAssets.kt`、`HomeIcons.kt`、`HomeStrokeIcons.kt`、`HomeWebHandlers.kt`、`LedgerScreens.kt`、`StrategyScreen.kt`、`LearningReportScreen.kt`、`AllServicesScreen.kt`、`AnalyticsCharts.kt`(+`chartsVicoMain`/`ohosMain` actual)、`DailyCheckInDialog.kt`、`HomeDetailScreens.kt`）属 **16B/16C** 范围；本册只在给调用点时按行引用。

### 16A.3 api/ 逐文件精读

#### 16A.3.1 `api/HomeApiWire.kt`（177 行 · home 契约层公共读法）

包 `com.example.my_kmp_project.feature.home.api`。文件头注释（`HomeApiWire.kt:16-21`）声明定位：只放「多个 home API 都要遵守的读法」，**不改 `core/network`**、不在每个 API 对象里重新实现分页方言。全部为顶层函数 / `ApiClient` 扩展函数，无类、无状态。

| 签名 | 行号 | 参数语义 | 返回 | 真实行为 | 副作用 |
|---|---|---|---|---|---|
| `fun homeJsonRootOrNull(raw: String?): JsonElement?` | `:30-34` | `raw` = HTTP 响应体原文 | 根 JSON（object/array），空体或畸形 JSON → `null` | `raw?.trim().orEmpty()` → 空则 null → `runCatching { NetworkJson.parseToJsonElement(text) }.getOrNull()` | 无（纯函数） |
| `fun homeEnvelopeCarriesCode(raw: String?): Boolean` | `:40-41` | 响应体原文 | body 是 object 且含键 `code` | 解析根 → `as? JsonObject` → `containsKey("code") == true` | 无 |
| `suspend inline fun <reified T> ApiClient.getHomeListPage(path, query = emptyMap(), rootFallbackWhenDataMissing = false): ApiResponse<ListPage<T>>` | `:57-81` | `path` 请求路径；`query` 查询串；`rootFallbackWhenDataMissing` 是否为「data 缺失时改读根 map」的手搓转换器保留 | 组装后的 `ApiResponse<ListPage<T>>` | ① `getApi<JsonElement>(path, query)`；② body **不含** `code` → 合成 `code = NetworkCodes.BFF_OK`、`message = resp.message ?: "success"`、整份 body 当列表体 `decodeListPage<T>(homeJsonRootOrNull(resp.raw))`；③ 含 `code` → `source = resp.data ?: 根.takeIf { rootFallbackWhenDataMissing }`，`data = source?.let { decodeListPage<T>(it) }`，保留原业务码 | **发一次 GET**；透传 `raw` / `httpStatus` |
| `suspend inline fun <reified T> ApiClient.getHomeListPageOrNull(path, query = emptyMap()): ApiResponse<ListPage<T>>` | `:92-105` | 同上（无 rootFallback 参数） | `data` 为 `ListPage<T>?` | `getApi<JsonElement>` → **仅** `resp.data?.let { decodeListPage }`；`data` 键缺失/JSON null → `data = null`，不归一成空页 | 发一次 GET |
| `fun <T> ApiResponse<T>.homeDataOrThrow(fallback: String): T` | `:114-118` | `fallback` = 中文兜底文案 | 非空 `data` | `failure = message?.takeIf{isNotBlank()} ?: fallback`；`!isSuccess` 或 `data == null` → `throw NetworkError.Business(code, failure)` | 抛异常 |
| `fun ApiResponse<*>.homeOkOrThrow(fallback: String)` | `:121-124` | 同上 | Unit | 只看 `isSuccess`，`data` 忽略；失败抛 `NetworkError.Business` | 抛异常 |
| `fun <T> ApiResponse<T>.homeBareDataOrThrow(fallback: String): T` | `:134-137` | 同上 | 非空 `data` | 抛 **裸** `Exception(message ?: fallback)`（非 `NetworkError.Business`）—— 与 home_todo/points 路线**有意不同**，见注释 `:126-133` | 抛异常 |
| `fun homeFirstNonNullString(vararg values: String?): String?` | `:143-144` | snake/camel 双写字段值 | 第一个非 null（空白串**保留**） | `values.firstOrNull { it != null }` | 无 |
| `fun homeRequestedPageHasMore(page: Int, size: Int, pagination: ListPagination?, listSize: Int): Boolean` | `:157-162` | `page`/`size` = **请求**页码页长；`pagination` 响应分页体；`listSize` 本页条数 | `page * size < (pagination?.total ?: listSize)` | 与 `core/network` 的 `ListPagination.hasMore` 方言不同（用请求 page/size） | 无 |
| `fun ListPage<*>.totalOrNull(): Int?` | `:165` | — | `pagination?.total` | 单表达式 | 无 |
| `fun homeWireLiteralTrue(el: JsonElement?): Boolean` | `:176-177` | JSON 元素 | 仅当布尔字面量 `true` | `el is JsonPrimitive && !el.isString && el.content == "true"`；字符串 `"true"`/`1`/null 全 false | 无 |

设计要点（注释实证）：`homeWireLiteralTrue` 之所以让线上字段读成 `JsonElement` 而非 `Boolean?`，是因为 kotlinx `TreeJsonDecoder.decodeTaggedBoolean` 遇非布尔原始值会抛，而 `decodeEnvelopeData` 的 `runCatching` 会把**整个对象**静默丢掉（`:167-175`）。

调用点见 16A.5 端点总表与各 API 小节（grep 命令：`rg -n "homeDataOrThrow|homeBareDataOrThrow|getHomeListPage|homeWireLiteralTrue|homeRequestedPageHasMore" feature/home/src`）。

#### 16A.3.2 `api/HomeTimeWire.kt`（37 行 · 日期读法）

无 import（`HomeTimeWire.kt:1-2` 只有 package）。文件头 `:3-11` 说明：`composeApp` 无 `kotlinx-datetime` 依赖，故 `java.time` 在 commonMain 不可用；KMP **保留 ISO 串本身**，只把「解析不出来的值」归一为 null，格式化留给 UI lane。

| 签名 | 行号 | 行为 |
|---|---|---|
| `fun homeIsoDateTimeOrNull(raw: String?): String?` | `:18-22` | trim 后空 → null；命中 `HOME_ISO_DATE_TIME` → 返回 **trim 后的原串**；否则 null。对等 Dart `_parseDateTime`（`home_todo_models.dart:174-179`：null/空串/`DateTime.tryParse` 失败 → null） |
| `private val HOME_ISO_DATE_TIME: Regex` | `:25-27` | `^\d{4}-\d{2}-\d{2}([T ]\d{2}:\d{2}(:\d{2}(\.\d{1,9})?)?)?(Z\\|[+-]\d{2}:?\d{2})?$` —— 容忍 `T` 与空格分隔、缺秒、小数秒 1-9 位、`Z` 或 `+08:00`/`+0800` |
| `fun homeDatePart(raw: String?): String` | `:37` | `raw?.substringBefore("T") ?: "null"`。**bug-for-bug 对齐** Dart `'$dateRaw'` 插值：JSON null → 字符串 `"null"` 而非空串（`after_sales_models.dart:63-66`，契约表 #50），见 `:29-36` 注释 |

#### 16A.3.3 `api/HomeTodoApi.kt`（87 行 · 待办契约，#15–#21）

`public class HomeTodoApi(private val client: ApiClient = NetworkFacade.api())`（`HomeTodoApi.kt:17-19`）。构造函数唯一参数 `client` 默认取 `core.network.NetworkFacade.api()` 全局默认客户端 —— 这是 home 全部 API 类的可测性接缝（测试注入 `HomeRecordingApiClient`）。头注释 `:7-16`：对位 Flutter `features/home/lib/home/api/home_todo_api.dart`（d7876a1），契约表 #15–#21，**全部需要 Bearer**；响应形状为契约表 §3 B 型（`data.items[]` 或裸数组，core `decodeListPage` 额外接受 `data.list[]`，是 Dart `_parseItems`（:114-132）的宽容超集）；失败语义 = 非 `code:0` 或 `data` 缺失 → 抛 `NetworkError.Business`。

`companion object` 常量（全部 `internal const val`，即完整路径含 `/api/v1` 前缀）：

| 常量 | 值 | 行号 |
|---|---|---|
| `TODO_CARDS_PATH` | `/api/v1/home/todo-cards` | `:22` |
| `JOIN_APPLICATIONS_PATH` | `/api/v1/home/join-applications` | `:25` |
| `FOLLOW_UP_PATH` | `/api/v1/home/follow-up-customers` | `:28` |
| `APPOINTMENTS_PATH` | `/api/v1/home/after-sales-appointments` | `:31` |
| `REVIEW_ORDERS_PATH` | `/api/v1/home/store-review-orders` | `:34` |

方法（全部 `public suspend fun`，无返回值缓存、无协程启动、无 KV 读写）：

| 方法 | 契约 | HTTP | 实现行号 | 真实行为 |
|---|---|---|---|---|
| `fetchTodoCards(): List<HomeTodoCard>` | #15 | GET `TODO_CARDS_PATH` | `:38-41` | `getHomeListPage<HomeTodoCard>` → `homeDataOrThrow("加载待办卡失败")` → `.items`。注释明确卡片尺寸由 `HomeTodoCard.size` **客户端推导** |
| `fetchJoinApplications(): List<HomeTodoJoinApplication>` | #16 | GET `JOIN_APPLICATIONS_PATH` | `:44-47` | 同上，兜底文案「加载入店申请失败」 |
| `approveJoin(applicationId: Int)` | #17 | POST `JOIN_APPLICATIONS_PATH/{id}/approve` | `:53-54` | 走 `postActionOk`；**无 body 动作 POST**，只看 `code == 0`，`data` 忽略 |
| `rejectJoin(applicationId: Int)` | #18 | POST `.../{id}/reject` | `:57-58` | 同上，兜底「拒绝失败」 |
| `fetchFollowUpCustomers(): List<HomeTodoFollowUpCustomer>` | #19 | GET `FOLLOW_UP_PATH` | `:61-64` | 兜底「加载待跟进客户失败」 |
| `fetchAppointments(): List<HomeTodoAppointment>` | #20 | GET `APPOINTMENTS_PATH` | `:67-70` | 兜底「加载售后预约失败」 |
| `fetchReviewOrders(): List<HomeTodoReviewOrder>` | #21 | GET `REVIEW_ORDERS_PATH` | `:73-76` | 兜底「加载店务审核单失败」 |

顶层扩展 `suspend fun ApiClient.postActionOk(path: String, fallback: String): Unit`（`:85-87`）：`postApi<Unit>(path, body = "") { Unit }.homeOkOrThrow(fallback)`。注释 `:79-84`（契约表 §5.12）指出关键实现约束：**必须真的发一次空 body POST**，而不是省略请求；`body = ""` 时 `KtorApiClient.postApi` 不调 `setBody`，与 Dart `HttpManager.post(path)`（`data == null`）等价。

#### 16A.3.4 `api/HomeTodoApiModels.kt`（175 行 · #15–#21 HTTP 契约模型）

头注释 `:7-16` 明确双轨策略：本份是 **HTTP 契约模型**（字段名与 Dart 一致、可空性对齐 BFF），而 `feature/home/HomeTodoModels.kt` 是 **mock 引擎在用的内部 UI 模型**，两者刻意分离，UI 接线（M2/M3）时才由上层把契约模型喂给页面。可空性策略统一为「线上字段可空 + 派生属性给 Dart 默认值」。

**`enum class HomeTodoCardSize(public val cells: Int)`（`:22-39`）** —— 对位 Dart `HomeTodoSize`（ADR 0009）：卡片尺寸由 `type` **客户端推导**，服务端不下发。`Small(1)`/`Medium(2)`/`Large(4)`（`:23-25`，`cells` = 网格占格数）；`companion object fun fromType(type: String): HomeTodoCardSize`（`:33-37`）：`partner_pending`→Large、`follow_up_customer`/`after_sales_appointment`→Medium、其余→Small。

**`@Serializable data class HomeTodoCard`（#15 行模型，`:42-72`）**：7 个构造属性全是 `String?`/`Int?`（`:44-50` = `type`/`title`/`subtitle`/`action_label`/`action_route`/`count`(FlexibleInt)/`image_url`），派生属性 7 个（`:53,56,59,62,65,68,71`）：`type`/`title`/`subtitle`/`actionRoute` 走 `orEmpty()`，`actionLabel` 兜底 **"去查看"**，`count` `?: 0`，`size = HomeTodoCardSize.fromType(type)`；`imageUrl` **无派生属性**（线上可空即最终可空）。

**`HomeTodoJoinApplication`（#16，`:76-111`）**：`applicationIdRaw`/`storeIdRaw`/`statusRaw` 均为 `Int?` + `FlexibleIntSerializer`（`:77-84`），`applicantUserIdRaw`/`applicantNameRaw`/`createdAtRaw` 为 `String?`。派生：`applicationId`(`:88`)、`storeId`(`:91`)、`applicantUserId`(`:94`)、`applicantName`(`:97`)、`status`(`:100`) 全部 0/空串兜底；`createdAt: String? get() = homeIsoDateTimeOrNull(createdAtRaw)`（`:106`，Dart `_parseDateTime` 对等，KMP 保留 ISO 串）；`displayName`（`:109-110`）= `applicantName.trim().ifEmpty { applicantUserId }` —— **名字为空则退化到用户 ID**。

**`HomeTodoFollowUpCustomer`（#19，`:115-129`）**：`customerIdRaw: Int?`(FlexibleInt, `:116-117`)、`displayNameRaw`(`:118`)、`nextFollowUpAtRaw`(`:119`)。派生：`customerId` `?: 0`(`:122`)、`displayName` `?: "客户"`(`:125`)、`nextFollowUpAt` → `homeIsoDateTimeOrNull`(`:128`)。

**`HomeTodoAppointment`（#20，`:133-152`）**：`appointmentIdRaw`(FlexibleInt, `:134-135`)、`customerNameRaw`(`:136`)、`appointmentDateRaw`(`:137`)、`statusRaw`(FlexibleInt, `:138-139`)。派生：`appointmentId` `?: 0`、`customerName` `?: "预约客户"`(`:145`)、`appointmentDate` → `homeIsoDateTimeOrNull`(`:148`)、`status` `?: 0`。

**`HomeTodoReviewOrder`（#21，`:156-175`）**：`orderIdRaw`(FlexibleInt)、`titleRaw`、`statusRaw`(FlexibleInt)、`createdAtRaw`。派生：`orderId` `?: 0`(`:165`)、`title` `?: "审核单"`(`:168`)、`status` `?: 0`(`:171`)、`createdAt` → `homeIsoDateTimeOrNull`(`:174`)。

注：`:15` 的 KDoc 引用 `[TransactionModel]`（位于同目录 `HomeTransactionModels.kt`）作为可空性策略的规范文档源。

#### 16A.3.5 `api/HomeTransactionApi.kt`（94 行 · #12 / #13 流水）

**注意类名是 `TransactionApi`，文件名却是 `HomeTransactionApi.kt`**（`HomeTransactionApi.kt:30`）；测试文件名同理为 `HomeTransactionApiTest.kt`（`commonTest/.../api/HomeTransactionApiTest.kt:19`）。实例化：`class TransactionApi(private val client: ApiClient = NetworkFacade.api())`。

头注释 `:14-29` 三条硬约束（均给 Dart 行号）：

| 约束 | 出处 |
|---|---|
| 方言：**`limit`/`offset`，`page` 是 0-based**（`offset = page * size`，与 Go `listLegacy` 对齐） | `:18-19`（transaction_api.dart:16-29） |
| Mine 调试页的 `page`/`size` 变体是 #14，**不在此类** | `:19` |
| 行缺 `id` → 解码抛异常；业务码非 0 且无 `data` → **静默空页**；详情 `data` 缺失 → 抛 `NetworkError.Business("交易记录不存在")` | `:21-28`（transaction_api.dart:32-37、:43、transaction_repository.dart:27-31） |

常量：`TRANSACTIONS_PATH = "/api/v1/transactions"`（`:35`）、`DEFAULT_SIZE = 20`（`:38`，Dart `TransactionRepository.pageSize`）。

| 方法 | 行号 | 参数 | 真实行为 | 副作用 |
|---|---|---|---|---|
| `suspend fun fetchPage(page: Int, size: Int = DEFAULT_SIZE, type: String? = null): TransactionListResult` | `:47-63` | `page` **0-based**；`size` 页长；`type` 为空白或 null 时省略 key | `client.getApi<JsonElement>(TRANSACTIONS_PATH, limitOffsetQuery(page, size, "type" to type))` → `homeEnvelopeCarriesCode(resp.raw)` 决定 `body = resp.data` 还是整份根 body（`:57-61`）→ `decodeStrictTransactionPage(body)` | GET 一次；**不走 `getHomeListPage`**（自实现双信封，因为要「静默空页」而不是 `data = null`） |
| `suspend fun getTransaction(id: Int): TransactionDetailResult` | `:70-82` | `id` 拼进 path | GET `$TRANSACTIONS_PATH/$id` → 同款双信封取 `body`；`body == null` → `throw NetworkError.Business(resp.code, "交易记录不存在")`（`:77-79`）；否则 `NetworkJson.decodeFromJsonElement<TransactionModel>(body)`，**畸形模型让序列化异常原样上抛**（`:80`） | GET 一次 |
| `private fun decodeStrictTransactionPage(el: JsonElement?): TransactionListResult` | `:89-93` | 已选出的列表体 | `decodeListPage<JsonElement>(el)` 先归一 A/B/C 形状（`list`/`items`/裸数组，非 object 行按 Dart `whereType<Map>` 跳过）→ **逐行** `NetworkJson.decodeFromJsonElement<TransactionModel>(it)` → `ListPage(rows, rawPage.pagination)` | 无；关键差异：默认 `decodeListPage<TransactionModel>` 会静默丢掉解码失败的行，这里改成缺 `id` 即抛 |

依赖的 core 符号：`limitOffsetQuery`（`core.network`，`:10` import）、`decodeListPage`（`:8`）、`getApi`（`:9`）、`NetworkJson`（`:7`）、`ListPage`（`:4`）、`NetworkError`（`:5`）。

**生产调用状态：未找到**任何非测试调用点 —— 唯一引用是 `commonTest/.../api/HomeTransactionApiTest.kt`。grep：`rg -n "fetchPage\(|getTransaction\(|TransactionApi" --glob '**/*.kt' .`。

#### 16A.3.6 `api/HomeTransactionModels.kt`（72 行 · #12/#13 DTO）

可空性策略（`:14-22`，全 home 契约层统一，本文件是规范出处）：线上传输字段一律「可空 + 派生属性给 Dart 默认值」，因为 **Go BFF 会对 Dart 里 `json['x'] ?? ''` 的字段显式下发 `null`**，而 `NetworkJson` 未开 `coerceInputValues` —— 非空字段遇 `null` 会整行解码失败、被 core 列表解码**静默丢弃**。**唯一例外是 `id`**：Dart `(json['id'] as num).toInt()` 在缺失时直接抛，故声明为非空无默认值。

**`@Serializable data class TransactionModel`（`:24-62`）** —— 12 个构造属性，其中 4 组是 snake/camel 双写：

| 组 | 属性 / SerialName / 行号 | 派生属性 / 行号 / 语义 |
|---|---|---|
| 主键 | `id` = **`Int` 非空、无默认**（`:26`） | 无派生；缺失或非数字即解码抛（刻意不兜底） |
| 单写标量 | `typeRaw`(`:27`)、`categoryRaw`(`:28`)、`amountRaw: Double?`(`:29`)、`dateRaw`(`:30`) | `type` `:40`、`category` `:43`、`amount` **`?: 0.0`** `:46`、`date` `:49` —— 前三 + `orEmpty()` |
| **snake/camel 双写** | `user_id`/`userId` `:31-32`、`created_at`/`createdAt` `:34-35`、`updated_at`/`updatedAt` `:36-37` | `userId` `:52`、`createdAt` `:58`、`updatedAt` `:61` = `homeFirstNonNullString(snake, camel)`，**结果仍可空** |
| 透传 | `noteRaw` / `note`（`:33`） | `note: String? get() = noteRaw`（`:55`，空白保留） |

类型别名（`:64-72`）：

| typealias | 值 | 行号 | 说明 |
|---|---|---|---|
| `TransactionListResult` | `ListPage<TransactionModel>` | `:69` | Dart `ResultModel<ListData<TransactionModel>>`；KMP 信封由 `ApiResponse` 承担、列表体归一为 `ListPage` |
| `TransactionDetailResult` | `TransactionModel` | `:72` | Dart `ResultModel<TransactionModel>` |

跨模块复用意图（`:11-12`，契约表 §6）：home 收支列表 + 详情 + Mine 调试页 #14 共用同一 DTO，只是查询方言从 `limit`/`offset` 换成 `page`/`size`。

注：`userId`/`note`/`createdAt`/`updatedAt` 四个派生属性走 `homeFirstNonNullString`（`api/HomeApiWire.kt:143`），只把 **null** 当缺失，故空白串保留 —— 与 Dart `_readString` 一致。查询方言由 `core.network.limitOffsetQuery` 实现（`core/network/src/commonMain/kotlin/com.example.my_kmp_project/core/network/ListPage.kt:109-114`：`limit = size`、`offset = page * size`），可选 key 经 `queryOf`（`:92-93`）**丢弃 null 与空白串**，这就是 `type = "   "` 也会省略 key 的原因。

#### 16A.3.7 `api/PointsApi.kt`（62 行 · #22–#25 签到/积分）

`public class PointsApi(private val client: ApiClient = NetworkFacade.api())`（`PointsApi.kt:19-21`）。两条刻意的边界（`:8-18`）：

- **不含 #26**（`GET /mall/stores/:storeId/products`）——归 commerce lane，`PointsGiftItem`/`MallProductCardModel` 在契约表 §6 要求合并成一个商品 DTO，本类不重复声明（对应实现见 `feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MallApi.kt:164,196,199`，其中硬编码 `storeId = 1` 与 Dart `PointsApi.fetchPointsGifts` 的 `size`）。
- **幂等提示（契约表 §5.3）**：#23 check-in、#25 claim 都是无 body 动作 POST，**服务端不去重**，重复点击只能由 UI 侧禁用按钮防住（`:16-17`）。

常量：`STATUS_PATH = "/api/v1/points/status"`（`:24`）、`CHECK_IN_PATH = "/api/v1/points/check-in"`（`:27`）、`TASKS_PATH = "/api/v1/points/tasks"`（`:30`，#25 在其后拼 `/:code/claim`）。

| 方法 | 契约 | HTTP | 行号 | 实现细节 | 失败语义 |
|---|---|---|---|---|---|
| `suspend fun fetchStatus(): PointsStatus` | #22 | GET `STATUS_PATH` | `:34-35` | `client.getApi<PointsStatus>(STATUS_PATH)`（core `ApiClient.kt:75-78`，`parseData = ::decodeEnvelopeData`）→ `homeDataOrThrow` | 非 0 码/缺 data → `NetworkError.Business`，文案「加载签到状态失败」 |
| `suspend fun checkIn(): CheckInResult` | #23 | POST `CHECK_IN_PATH` | `:38-40` | `postApiDecoded<CheckInResult>(CHECK_IN_PATH, body = "")`（core `ApiClient.kt:92-95`）→ `homeDataOrThrow("签到失败")` | 同上 |
| `suspend fun fetchTasks(): List<GrowthTask>` | #24 | GET `TASKS_PATH` | `:50-53` | **`getHomeListPage<GrowthTask>(TASKS_PATH, rootFallbackWhenDataMissing = true)`** —— 全仓库唯一使用该 flag 的调用点，精确覆盖 Dart 手搓转换器（points_api.dart:42-59）：`data` 是 map 读 `data['items']`、否则读**根** `items`、缺 `code` 合成 `code:0`、永不因 data 缺失抛 | 「加载成长任务失败」 |
| `suspend fun claimTask(code: String): ClaimTaskResult` | #25 | POST `$TASKS_PATH/$code/claim` | `:59-61` | `postApiDecoded<ClaimTaskResult>(..., body = "")` → `homeDataOrThrow("领取失败")`；`code` 是**字符串任务码**，原样拼进 URL（**未做 URL 编码**，见 16A.9） | 同上 |

生产调用点：**未找到**（UI 层用的是 `HomePointsStore.checkIn()`，见 `HomeScreen.kt:165`、`HomeRouteHost.kt:928,1071`）。grep：`rg -n "PointsApi|fetchStatus\(\)|claimTask\(" feature composeApp --glob '**/*.kt'`。

#### 16A.3.8 `api/PointsModels.kt`（158 行 · #22–#25 DTO）

头注释 `:8-15`：可空性策略见 `[TransactionModel]` KDoc（即 16A.3.6）；**布尔线上字段一律读成 `JsonElement`**，判断走 `homeWireLiteralTrue`，因为 Dart 的 `json['k'] == true` 对非布尔值宽容当 false，而 kotlinx 遇 `"signed":"yes"` 会抛并让整个 `data` 被静默丢弃。

**`CheckInDayView`（#22 日历格，`:19-37`）**：`dayRaw: String?`(`:20`)→`day orEmpty()`(`:27`)；**`signedRaw: JsonElement?`**(`:21`)→`signed = homeWireLiteralTrue(...)`(`:30`)；`rewardRaw: Int?`+FlexibleInt(`:22-23`)→**`?: 5`**(`:33`)；**`isTodayRaw: JsonElement?`**(`:24`)→字面 true(`:36`)。

**`PointsStatus`（#22 的 `data`，`:41-69`）**：`balanceRaw`(FlexibleInt,`:42-43`)→`balance ?: 0`(`:52`)；`checkedInTodayRaw`(**JsonElement?**,`:44`)→`checkedInToday`(`:55`)；`streakRaw`(FlexibleInt,`:45-46`)→`?: 0`(`:58`)；`todayRewardRaw`(FlexibleInt,`:47-48`)→**`?: 5`**(`:61`)；`calendarRaw: List<CheckInDayView>?`(`:49`)→`calendar: List<CheckInDayView> get() = calendarRaw ?: emptyList()`(`:68`)。`:63-67` 记录**唯一已知偏差**：日历里混入标量元素时 Dart 静默跳过，KMP 会让整个 `PointsStatus` 解码失败并被调用点抛「加载签到状态失败」。

**`CheckInResult`（#23 的 `data`，`:73-93`）**：`points`/`balance`/`streak` 均 FlexibleInt `?: 0`（`:83,86,89`），`day: String orEmpty()`（`:92`）。

**`enum class TaskProgress`（`:100-113`）**：`Incomplete` / `Claimable` / `Claimed`；`companion fun parse(raw: String?)`（`:107-111`）只识别 `"claimable"`、`"claimed"`，**其他值一律归 `Incomplete`**（对位 Dart `TaskProgressX.parse` default 分支）。

**`GrowthTask`（#24 行模型，`:117-139`）**：`codeRaw`/`titleRaw` `String?`、`rewardRaw` FlexibleInt、`progressRaw` `String?`。派生：`code`（`:129`，注释 `:124-128` 说明任务码是**字符串**、#25 原样拼进 URL，数字下发时由 `NetworkJson.isLenient` 读成字面量以等价于 Dart `toString()`）、`title orEmpty()`、`reward ?: 0`、`progress: TaskProgress get() = TaskProgress.parse(progressRaw)`（`:138`）。

**`ClaimTaskResult`（#25 的 `data`，`:143-158`）**：`points ?: 0`（`:151`）、`balance ?: 0`（`:154`）、`day orEmpty()`（`:157`）。

### 16A.4 aftersales/ 与 newcar/

#### 16A.4.1 `aftersales/AfterSalesApi.kt`（108 行 · #47–#50）

`public class AfterSalesApi(private val client: ApiClient = NetworkFacade.api())`（`AfterSalesApi.kt:21-23`）。头注释 `:14-20`：契约表 #47–#50，失败语义用 `homeBareDataOrThrow`（抛**裸** `Exception`，非 `HttpRequestException`/`NetworkError.Business`，契约表 §2.9 特别标注）。常量：`BASE_PATH = "/api/v1/after-sales"`（`:26`）、`DEFAULT_LIST_SIZE = 10`（`:29`，Dart `fetchRecords(size = 10)`）。

| 方法 | 契约 | HTTP | 行号 | 真实行为 |
|---|---|---|---|---|
| `suspend fun fetchRecords(page: Int, size: Int = DEFAULT_LIST_SIZE): AfterSalesListResult` | #47 | GET `$BASE_PATH/records` + `pageQuery(page, size)` | `:39-54` | `getApi<AfterSalesRecordsWire>(...).homeBareDataOrThrow("加载列表失败")`；`list = data.list ?: emptyList()`；`total = data.pagination?.totalRaw ?: list.size`；返回 `AfterSalesListResult(list, hasMore = page * size < total, canCreate = data.canCreate)`。注释 `:36-37`：**`data` 按裸 map 读、不走 `ListPage`，否则丢 `canCreate`**（它 gate「新建工单」入口）。`page` 服务端 **1-based**（`:33-34`，after_sales_list_viewmodel.dart:28） |
| `suspend fun fetchDetail(recordId: Int): AfterSalesRecord` | #48 | GET `$BASE_PATH/records/$recordId` | `:57-59` | `getApi<AfterSalesRecord>` → `homeBareDataOrThrow("加载详情失败")` |
| `suspend fun create(serviceKind: String, title: String, serviceDate: String, customerName: String? = null, customerPhone: String? = null, appointmentId: Int? = null, plateNo: String? = null, mileage: Int? = null, content: String? = null): AfterSalesRecord` | #49 | POST `$BASE_PATH/records` | `:70-97` | `buildJsonObject` 逐键落笔（`:81-94`）后 `NetworkJson.encodeToString` → `postApiDecoded<AfterSalesRecord>(path, body)` → `homeBareDataOrThrow("新建失败")` |
| `suspend fun fetchPendingAppointments(): List<AfterSalesAppointment>` | #50 | GET `$BASE_PATH/pending-appointments` | `:103-107` | `getApi<AfterSalesPendingAppointmentsWire>` → `homeBareDataOrThrow("加载预约失败")` → `data.list ?: emptyList()` |

`create` 的 body 规则（`:61-69` 注释 + `:81-94` 代码，两类键并存）：

| 键 | 规则 | 行号 |
|---|---|---|
| `service_kind` | **总发**，字符串（Dart 侧 `_kind = '1'`） | `:83` |
| `title` / `service_date` | 总发 | `:84-85` |
| `customer_name` / `customer_phone` | **即使 null 也显式发 null**（`put(key, String?)` 写入 `JsonNull`，与 Dart 把 null 直接放进 map 一致） | `:86-88` |
| `appointment_id` | 仅非 null 才发 | `:89` |
| `plate_no` | 仅 trim 后非空才发，且**发 trim 值** | `:90` |
| `mileage` | 仅非 null 才发 | `:91` |
| `content` | 仅 trim 后非空才发，且发 trim 值 | `:92` |

#### 16A.4.2 `aftersales/AfterSalesModels.kt`（147 行）

| 类型 | 行号 | 性质 | 关键派生 |
|---|---|---|---|
| `@Serializable data class AfterSalesRecord` | `:19-76` | #47/#48/#49 工单行，12 个 `*Raw` 属性 | `recordId ?: 0`(`:39`)、`storeId ?: 0`(`:42`)、**`appointmentId: Int?` 原样可空**(`:45`)、`customerName`/`customerPhone` `orEmpty`(`:48,51`)、`serviceKind: Int ?: 0`(`:57`)、`serviceKindLabel`(`:60`)、`title`(`:63`)、`content`(`:66`)、`serviceDate`(`:69`)、`plateNo`(`:72`)、**`mileage: Int?` 可空**(`:75`) |
| `@Serializable data class AfterSalesAppointment` | `:84-102` | #50 行模型 | `appointmentId ?: 0`(`:91`)、`customerName orEmpty`(`:94`)、`appointmentDate: String get() = homeDatePart(appointmentDateRaw)`(`:101`) |
| `data class AfterSalesListResult`（**非 @Serializable**） | `:112-116` | #47 对外返回：`list` / `hasMore` / `canCreate` | `:104-111` KDoc 说明为何不用 `ListPage`（丢 `canCreate`，见 `after_sales_list_viewmodel.dart:33`）；`hasMore` 用请求页方言 `page * size < (total ?? list.size)`（`after_sales_api.dart:30-34`）。注意：**本方法没有复用 `homeRequestedPageHasMore`**，而是在 `AfterSalesApi.kt:51` 就地内联计算 |
| `@Serializable data class AfterSalesRecordsWire`（默认 public，KDoc `:119-120` 声明「仅契约层内部使用」） | `:124-134` | #47 `data` 裸 map：`list` / `pagination` / `can_create` | `canCreate: Boolean get() = homeWireLiteralTrue(canCreateRaw)`(`:133`)，`canCreateRaw` 声明为 `JsonElement?`(`:127`) |
| `@Serializable data class AfterSalesPaginationWire` | `:138-141` | `pagination`，**只有 `total`** | `totalRaw: Int?`（FlexibleInt，`:140`）—— 可空是刻意的：缺键回 null → 落到 `list.size`（见 `HomeApiWire.kt:154-155` 注释） |
| `@Serializable data class AfterSalesPendingAppointmentsWire` | `:145-147` | #50 `data` 裸 map | `list: List<AfterSalesAppointment>? = null` |

两处「读写不对称」的坑（`:53-57` KDoc 明示保留）：`service_kind` **读**是整数（0 维修 / 1 保养），#49 的**写**入参却是字符串（`after_sales_create_page.dart:27` 传 `'1'`）。#50 的日期**不做解析**，只按 `T` 截断，且 bug-for-bug 保留 Dart `'$null'` 插值 → 字符串 `"null"`（`:96-101`）。

#### 16A.4.3 `newcar/NewCarFollowApi.kt`（187 行 · #40–#46，home 里最大的 API 类）

`public class NewCarFollowApi(private val client: ApiClient = NetworkFacade.api())`（`NewCarFollowApi.kt:27-29`）。失败语义同样用 `homeBareDataOrThrow`（抛裸 `Exception`，契约表 §2.8，`:20-26`）。常量：`BASE_PATH = "/api/v1/new-car-follow-files"`（`:32`）、`DEFAULT_LIST_SIZE = 10`（`:35`）、`DEFAULT_LOG_SIZE = 20`（`:38`）。

| 方法 | 契约 | HTTP | 行号 | 真实行为 |
|---|---|---|---|---|
| `fetchSummary(): NewCarFollowSummary` | #40 | GET `$BASE_PATH/summary` | `:42-44` | `getApi<NewCarFollowSummary>` → `homeBareDataOrThrow("加载摘要失败")` |
| `fetchList(tab: NewCarFollowTab, page: Int, size: Int = DEFAULT_LIST_SIZE): NewCarFollowPage<NewCarFollowFile>` | #41 | GET `BASE_PATH` + `pageQuery(page, size) + tab.query` | `:53-66` | **`getHomeListPageOrNull`**（缺 `data` → null，由 `homeBareDataOrThrow` 抛，见 `HomeApiWire.kt:83-91`）→ `NewCarFollowPage(list = pageData.items, hasMore = homeRequestedPageHasMore(page, size, pageData.pagination, pageData.items.size))`。`page` 服务端 **1-based**（`:49`）；tab 过滤发**中文字面量** `intent_band=高\\|中\\|低` 或 `overdue=1`，`all` 不带键（`:50-51`） |
| `fetchDetail(fileId: String): NewCarFollowFile` | #42 | GET `$BASE_PATH/$fileId` | `:69-71` | `fileId` 是**字符串主键**，直接拼 URL；兜底「加载详情失败」 |
| `create(followLevel: String, customerId: Int? = null, displayName: String? = null, phone: String? = null, vehicleInterest: String? = null, nextFollowUpAt: String? = null): NewCarFollowFile` | #43 | POST `BASE_PATH` | `:84-108` | **互斥分支**（`:74-82`）：`customerId != null && customerId > 0` → 只发 `customer_id`、**省略** `display_name`/`phone`（`:95-96`）；否则 `display_name` + `phone` **两键总发**、null 也落 `JsonNull`（`:97-101`）。`vehicle_interest` 仅 trim 非空才发（`:102`）；`next_follow_up_at` **只判非空串、不 trim**（`:103`）。用 `buildJsonObject` 是因为同一 body 里「显式 null」与「省略键」并存 |
| `patch(fileId: String, followLevel: String? = null, stage: String? = null, nextFollowUpAt: String? = null): NewCarFollowFile` | #44 | PATCH `$BASE_PATH/$fileId` | `:116-125` | `patchApi<NewCarFollowPatchRequest, NewCarFollowFile>` —— **稀疏 body**：三键全可省，靠 `NetworkJson.explicitNulls = false`（`core/network/.../NetworkJson.kt:10`）实现「null 整键省略」，与 Dart `patch()` :103-106 等价。`stage` ∈ `new/following/test_drive/quoted/won/lost`，`followLevel` ∈ `H/A/B/E`（`:114`） |
| `fetchLogs(fileId: String, page: Int = 1, size: Int = DEFAULT_LOG_SIZE): NewCarFollowPage<NewCarFollowLog>` | #45 | GET `$BASE_PATH/$fileId/logs` + `pageQuery` | `:128-141` | 与 #41 同构：`getHomeListPageOrNull` + `homeBareDataOrThrow("加载流水失败")` + `homeRequestedPageHasMore`（page 1-based） |
| `createLog(fileId: String, body: String, followLevel: String? = null, nextFollowUpAt: String? = null): NewCarFollowLog` | #46 | POST `$BASE_PATH/$fileId/logs` | `:152-165` | `postApi<NewCarFollowCreateLogRequest, NewCarFollowLog>`；`followLevel` 归一为 `takeIf { it.isNotEmpty() }`（`:162`，空串→省略键），`nextFollowUpAt` **原样传**（`:163`，非 null 就发，**空串也发**）——两键省略条件不同（`:146-147`）。注释 `:149-150`：写流水可由服务端移动档案的 `follow_level`/`next_follow_up_at`，故**只回服务端解码的这条流水，不做本地乐观改写** |

顶层请求体 DTO（同文件，非独立文件）：

| 类型 | 行号 | 键 | 说明 |
|---|---|---|---|
| `@Serializable data class NewCarFollowPatchRequest` | `:169-174` | `follow_level` / `stage` / `next_follow_up_at`（三键全 `String? = null`） | #44 稀疏 PATCH body |
| `@Serializable data class NewCarFollowCreateLogRequest` | `:183-187` | `body: String`（必发）、`follow_level: String? = null`、`next_follow_up_at: String? = null` | `:176-181` KDoc：`follow_level` 用 null 表达「省略」；`next_follow_up_at` **保留空串**，因 Dart 只判 `!= null`（new_car_follow_api.dart:153） |

#### 16A.4.4 `newcar/NewCarFollowModels.kt`（198 行）

| 类型 | 行号 | 内容 |
|---|---|---|
| `enum class NewCarFollowTab(label: String, query: Map<String,String>)` | `:21-28` | `All("全部", emptyMap())`、`High("高意向", intent_band=高)`、`Medium(中)`、`Low(低)`、`Overdue("逾期", overdue=1)`。**query 里是中文字面量**，必须原样发出（`:18-19`）；`query` 在 `NewCarFollowApi.kt:60` 与 `pageQuery` 合并 |
| `@Serializable data class NewCarFollowStats` | `:32-58` | `active`/`overdue`/`highIntent`/`lost` 四键全 FlexibleInt，派生 `?: 0`（`:43,46,49,52`）；`companion val Empty get() = NewCarFollowStats()`（`:56`）对位 Dart「`stats` 缺失按 `const {}` 解析 → 全 0」 |
| `@Serializable data class NewCarFollowSummary` | `:62-83` | `displayName`/`avatarUrl`/`positionLabel`/`storeName` 派生 `orEmpty()`（`:70-79`）；`stats: NewCarFollowStats get() = statsRaw ?: NewCarFollowStats.Empty`（`:82`） |
| `@Serializable data class NewCarFollowFile` | `:92-140` | 11 个 `*Raw`。`:85-90` 关键说明：**`file_id`/`customer_id` 是字符串**（Dart `'${json['file_id'] ?? ''}'`，数字会被字符串化、缺失变空串），#42/#44/#45 的路径参数用它。派生：`fileId`(`:106`)、`customerId`(`:109`)、`customerName`(`:112`)、`customerPhone`(`:115`)、`followLevel`(H/A/B/E, `:118`)、`intentBand`（服务端可回中文，`:121`)、`stage`(`:124`)、`vehicleInterest`(`:127`)、**`nextFollowUpAt: String?` 原样可空**(`:133`，`:129-132` 明确**不套** `homeIsoDateTimeOrNull`，因 Dart 侧不解析)、`ownerUserId`(`:136`)、`ownerDisplayName`(`:139`) |
| `@Serializable data class NewCarFollowLog` | `:149-186` | `logId`/`fileId`/`createdAt` 三个是**字符串化**（`orEmpty()`，`:161,164,185`）；`authorUserId`/`authorDisplayName`/`body`/`followLevel`/`intentBand` 同（`:167-179`）；**`nextFollowUpAt: String?` 可空**(`:182`)。`:142-147` KDoc 重申 #46 不做本地乐观改写 |
| `data class NewCarFollowPage<T>(list: List<T>, hasMore: Boolean)` | `:195-198` | 对位 Dart 记录型返回值 `({List<T> list, bool hasMore})`；`hasMore` 走请求页方言，**不是** core 的 `ListPagination.hasMore`（`:188-194`） |

### 16A.5 端点总表

「调用点」列给的是**实际发起该方法的 file:line**。`feature/home` 的六个 API 类在生产代码里**没有任何调用点**（见 16A.9），因此调用点全部落在 `commonTest`；下表路径常量省略 `/api/v1` 前缀。

| 契约 | 类.方法 | HTTP + path | 请求 DTO / query | 响应 DTO | 定义行号 | 调用点（测试） |
|---|---|---|---|---|---|---|
| #12 | `TransactionApi.fetchPage` | GET `/transactions?limit&offset[&type]` | `limitOffsetQuery(page,size,"type" to type)` | `ListPage<TransactionModel>` | `api/HomeTransactionApi.kt:47` | `api/HomeTransactionApiTest.kt:28,33,36,40,60,91,100,108,115` |
| #13 | `TransactionApi.getTransaction` | GET `/transactions/{id}` | — | `TransactionModel` | `:70` | `:137,146,152` |
| #15 | `HomeTodoApi.fetchTodoCards` | GET `/home/todo-cards` | 无 query | `List<HomeTodoCard>` | `api/HomeTodoApi.kt:38` | `api/HomeTodoApiTest.kt:36,63,183` |
| #16 | `HomeTodoApi.fetchJoinApplications` | GET `/home/join-applications` | — | `List<HomeTodoJoinApplication>` | `:44` | `:82,179` |
| #17 | `HomeTodoApi.approveJoin` | POST `/home/join-applications/{id}/approve`（**空 body**） | 无 | 只看 `code` | `:53` | `:102,117` |
| #18 | `HomeTodoApi.rejectJoin` | POST `/home/join-applications/{id}/reject`（空 body） | 无 | 只看 `code` | `:57` | `:108,122` |
| #19 | `HomeTodoApi.fetchFollowUpCustomers` | GET `/home/follow-up-customers` | — | `List<HomeTodoFollowUpCustomer>` | `:61` | `:128,155` |
| #20 | `HomeTodoApi.fetchAppointments` | GET `/home/after-sales-appointments` | — | `List<HomeTodoAppointment>` | `:67` | `:135,175` |
| #21 | `HomeTodoApi.fetchReviewOrders` | GET `/home/store-review-orders` | — | `List<HomeTodoReviewOrder>` | `:73` | `:143,163,171` |
| #22 | `PointsApi.fetchStatus` | GET `/points/status` | — | `PointsStatus` | `api/PointsApi.kt:34` | `api/PointsApiTest.kt:40,61,79,87,95,103,111` |
| #23 | `PointsApi.checkIn` | POST `/points/check-in`（空 body） | `body = ""` | `CheckInResult` | `:38` | `:120,133,140` |
| #24 | `PointsApi.fetchTasks` | GET `/points/tasks` | — | `List<GrowthTask>` | `:50` | `:155,174,188,198,209` |
| #25 | `PointsApi.claimTask` | POST `/points/tasks/{code}/claim`（空 body） | `code` **原样拼 URL** | `ClaimTaskResult` | `:59` | `:215,226` |
| #40 | `NewCarFollowApi.fetchSummary` | GET `/new-car-follow-files/summary` | — | `NewCarFollowSummary` | `newcar/NewCarFollowApi.kt:42` | `newcar/NewCarFollowApiTest.kt:44,59,139` |
| #41 | `NewCarFollowApi.fetchList` | GET `/new-car-follow-files?page&size[&intent_band\\|overdue]` | `pageQuery + tab.query` | `NewCarFollowPage<NewCarFollowFile>` | `:53` | `:72,75,78,81,84,101,115,122,131` |
| #42 | `NewCarFollowApi.fetchDetail` | GET `/new-car-follow-files/{fileId}` | — | `NewCarFollowFile` | `:69` | `:149,161` |
| #43 | `NewCarFollowApi.create` | POST `/new-car-follow-files` | `buildJsonObject` 逐键（见 16A.4.3） | `NewCarFollowFile` | `:84` | `:171,196,207` |
| #44 | `NewCarFollowApi.patch` | PATCH `/new-car-follow-files/{fileId}` | `NewCarFollowPatchRequest`（稀疏） | `NewCarFollowFile` | `:116` | `:221,227,231` |
| #45 | `NewCarFollowApi.fetchLogs` | GET `/new-car-follow-files/{fileId}/logs?page&size` | `pageQuery` | `NewCarFollowPage<NewCarFollowLog>` | `:128` | `:153,246` |
| #46 | `NewCarFollowApi.createLog` | POST `/new-car-follow-files/{fileId}/logs` | `NewCarFollowCreateLogRequest` | `NewCarFollowLog` | `:152` | `:266,275,279` |
| #47 | `AfterSalesApi.fetchRecords` | GET `/after-sales/records?page&size` | `pageQuery(page,size)` | `AfterSalesListResult`（经 `AfterSalesRecordsWire`） | `aftersales/AfterSalesApi.kt:39` | `aftersales/AfterSalesApiTest.kt:49,82,88,91,99,106,228` |
| #48 | `AfterSalesApi.fetchDetail` | GET `/after-sales/records/{recordId}` | — | `AfterSalesRecord` | `:57` | `:116,235` |
| #49 | `AfterSalesApi.create` | POST `/after-sales/records` | `buildJsonObject` 逐键 | `AfterSalesRecord` | `:70` | `:127,152,177,238` |
| #50 | `AfterSalesApi.fetchPendingAppointments` | GET `/after-sales/pending-appointments` | — | `List<AfterSalesAppointment>`（经 `…PendingAppointmentsWire`） | `:103` | `:199,222,241` |

未覆盖：契约 #14（Mine 调试页的 `page`/`size` 变体，`HomeTransactionModels.kt:11-12` 声明复用同一 DTO，但**本模块无实现**，属 mine lane）；#26（`GET /mall/stores/:storeId/products` 明确移交 commerce，见 `PointsApi.kt:12-14` 与 `feature/commerce/.../MallApi.kt:164,196,199`）。

### 16A.6 Store 专题

四个 Store 全是 `object`（**进程级单例**），全部靠 Compose `mutableStateOf` / `mutableIntStateOf` 承载状态，全部**不落 KeyValueStore**。这一点与 16A.9 的结论一致：home 的「状态层」目前是 mock 引擎，不是网络层。

#### 16A.6.1 `HomePointsStore.kt`（107 行 · 签到状态机）

`object HomePointsStore`（`HomePointsStore.kt:25`），KDoc `:21-24`：对位 Flutter `DailyCheckInDialog` + mock PointsApi 状态机，**in-process**；对话框 ack 以本地日历日为键（`check_in_dialog_ack_date`）。

持有的状态（`mutableXxx` + `private set` ⇒ 外部只能读、只能经方法改）：

状态字段（全 `by mutableXxx` + `private set` ⇒ 外部只能读、只能经方法改）：`version: Int = 0`（`:29-30`，变更计数器供 Compose 侧重组；`private bump()` `:32-34` 在任何写操作后 `version++`）、`balance: Int = 0`（`:36-37`）、`streak: Int = **1**`（`:38-39`，**初值 1** 而非 0）、`checkedInToday: Boolean = false`（`:40-41`）、`todayReward: Int = DefaultTodayReward`（`:42-43`）、`dialogAckDate: String? = null`（`:46-47`，最后一次「签到或关掉弹窗」的本地日期串）。

常量：`PrefsKey = "check_in_dialog_ack_date"`（`:26`）、`DefaultTodayReward = 5`（`:27`，与契约层 `PointsModels.kt:33,61` 的 `?: 5` 缺省一致）。

| 函数 | 行号 | 签名与返回 | 真实行为 / 副作用 |
|---|---|---|---|
| `todayLocalString(year: Int, month: Int, day: Int): String` | `:49-58` | → `"yyyy-MM-dd"` | 纯格式化：年 padStart(4,'0')、月/日 padStart(2,'0')。**参数由调用方提供**（Store 自己不取系统时间） |
| `shouldShowDialog(isLoggedIn: Boolean, checkedInToday: Boolean, ackDate: String?, todayLocal: String): Boolean` | `:61-71` | → Boolean | **纯函数**（参数而非字段），三连否定：未登录 → false；今日已签 → false；`ackDate == todayLocal` → false；否则 true |
| `status(): HomePointsStatus` | `:73-78` | → 快照数据类 | 打包 `balance`/`checkedInToday`/`streak`/`todayReward` |
| `markDialogAcked(todayLocal: String)` | `:80-83` | Unit | 写 `dialogAckDate` + `bump()` |
| `checkIn(): HomeCheckInResult?` | `:89-97` | 已签过 → **null** | 未签：`gained = todayReward` → `balance += gained`、`streak += 1`、`checkedInToday = true`、`bump()` → 返回 `HomeCheckInResult(points=gained, streak, balance)`。注释 `:85-88`：「每进程日一次」 |
| `resetForTests()` | `:99-106` | Unit | 回到 `balance=0, streak=1, checkedInToday=false, todayReward=5, dialogAckDate=null` + bump |

数据类：`HomePointsStatus(balance, checkedInToday, streak, todayReward)`（`:8-13`）、`HomeCheckInResult(points, streak, balance)`（`:15-19`）——**字段顺序与契约层 `PointsStatus`/`CheckInResult` 不同**，是 UI 侧模型。

并发处理：**没有任何同步原语**（无 `Mutex`、无 `AtomicInteger`、非 `@Volatile`），依赖 Compose snapshot state 的线程安全 + 单主线程 UI 模型。协程：**不启动任何协程**、不发 HTTP。

持久化：**无**。`PrefsKey` 在 KMP 侧**没有任何读写点** —— grep `rg -n "HomePointsStore\.PrefsKey|check_in_dialog_ack_date" --glob '**/*.kt' .` 只命中 `HomePointsStore.kt:23,26`（声明）与 Android 原生壳 `composeApp/src/androidMain/kotlin/com.example.my_kmp_project/nativeshell/NativeJetpackHome.kt:83`（`prefs.getString("check_in_dialog_ack_date", null)`）、`:215`（`.edit().putString(..., today).apply()`）。即：**同一个 key 由 Jetpack 原生页用 SharedPreferences 独立读写，Compose 侧的 `HomePointsStore` 从不落盘**，两条 lane 各存各的。

调用点：`HomeScreen.kt:165`（`HomePointsStore.checkIn()`）、`HomeRouteHost.kt:928` 与 `:1071`（同为 `checkIn()`）、`HomeRouteHost.kt:791` 注释「CheckInMallPage — bound to HomePointsStore (mock PointsApi)」。

#### 16A.6.2 `HomeSecondaryStore.kt`（292 行 · 二手车/售后/新车跟进三条演示流水线）

`object HomeSecondaryStore`（`HomeSecondaryStore.kt:12`），KDoc `:8-11`：可变的 home 二级流水线，选中 id 驱动详情页（对位 Flutter route arg / controller selection）。

状态：

| 字段 | 类型/初值 | 行号 | 可变性 |
|---|---|---|---|
| `version` | `Int = 0`（`private set` + `bump()` `:16-18`） | `:13-14` | 只增 |
| `selectedUsedCarId` / `selectedAfterSalesId` / `selectedNewCarFollowId` | `String? = null`，**无 `private set`** | `:20-22` | 外部可直接赋值 |
| `afterSalesCanCreate` | `Boolean = true` | `:25` | 「顾问演示：Flutter `can_create` 对门店员工为 true」——契约层里它来自 #47 的 `can_create`，这里是写死的 true |
| `usedCarSeq = 3` / `afterSalesSeq = 441` / `newCarSeq = 3` | `private var Int` | `:27-29` | 自增主键种子 |
| `usedCars` = `mutableListOf(3 条 uc-1/uc-2/uc-3)` | `:31-74` | 私有可变列表 | **硬编码假数据**：置换/专卖/收车三单，含 VIN、车牌、里程、金额、客户名（张先生/李女士/王先生） |
| `afterSales` = `mutableListOf(2 条 as-441/as-438)` | `:76-101` | 同上 | 保养/维修两工单，`as-441` 带 `appointmentId = 8821`、`as-438` 为 null |
| `_newCarFollows` = `mutableListOf(3 条 nc-1..nc-3)` | `:103-135` | 同上 | 跟进中/报价/试驾三档，`nc-3` 是唯一 `overdue = true` |

API 面（每条流水线四件套：list / find / select / validate + create）：

| 函数 | 行号 | 行为 |
|---|---|---|
| `usedCarOrders(): List<UsedCarOrderRow>` | `:137` | `usedCars.toList()` —— **防御性拷贝**，外部拿不到可变列表 |
| `findUsedCar(id: String?): UsedCarOrderRow?` | `:139-141` | `id?.let { firstOrNull } ?: firstOrNull()` —— **id 传 null 或未命中都回退到第一条**，只有列表全空才 null |
| `selectUsedCar(id: String)` | `:143-145` | 仅写 `selectedUsedCarId`，**不 bump** |
| `validateUsedCarCreate(customerName, vehicleModel, plateNo, vin, mileageKm: Int?, modelYear: Int?, amount: Int?): String?` | `:151-167` | 返回**错误文案或 null**：`customerName.isBlank()` → 「请选择客户」；车型/车牌/VIN 任空白 或 里程/年款/金额为 null 或 `amount <= 0` → 「请完整填写车况与金额」 |
| `createUsedCar(kindLabel, customerName, vehicleModel, plateNo, vin, mileageKm, modelYear, amount): UsedCarOrderRow` | `:169-202` | `usedCarSeq += 1` → id `"uc-$seq"`；`statusLabel = "已提交"`；**`submittedDate` 写死 `"2026-09-29"`**（`:184`）；`amountLabel` 按 `kindLabel` 映射（置换→评估价、专卖→成交价、else→收车价，`:189-193`）；`customerName = customerName.substringBefore(' ').ifBlank { customerName }`（取空格前首段，`:195`）；`add(0, row)` **头插**、选中它、`bump()` |
| `afterSalesDetails()` / `findAfterSales(id?)` / `selectAfterSales(id)` | `:204` / `:206-208` / `:210-212` | 与二手车同构 |
| `validateAfterSalesCreate(name, phone, title, plate, content): String?` | `:214-224` | 姓名或手机空白 → 「请填写客户姓名和手机号」；标题/车牌/内容任一空白 → 「请完整填写工单信息」 |
| `createAfterSales(name, phone, title, plate, mileageKm: Int?, date, content, kindLabel: String = "保养"): AfterSalesDetailRow` | `:226-253` | `afterSalesSeq += 1` → `"as-$seq"`；`title.ifBlank { "工单 AS-$seq" }`；**`appointmentId` 恒为 null**（`:247`）；头插 + 选中 + bump |
| `newCarFollows()` / `findNewCarFollow(id?)` / `selectNewCarFollow(id)` | `:255` / `:257-259` / `:261-263` | 同构（内部字段名带下划线 `_newCarFollows`，对外方法无下划线） |
| `validateNewCarFollowCreate(name, phone): String?` | `:265-268` | 只校验姓名+手机 |
| `createNewCarFollow(name, phone, vehicle, intentBand): NewCarFollowRow` | `:270-291` | `"nc-$seq"`；`vehicle.ifBlank { "意向车型待定" }`；`stage` 写死「跟进中」、`nextFollow` 写死「今日待排」、`owner` 写死「销售顾问」；头插 + 选中 + bump |

副作用：**无 HTTP、无 KV、无协程**；`create*` 直接改进程内列表。并发：同 HomePointsStore，无同步原语；`mutableListOf` 的结构性修改与 `toList()` 读取之间**无保护**，跨线程调用会有竞态（当前只在 UI 主线程用）。

#### 16A.6.3 `HomeTodoStore.kt`（124 行 · 待办/入店申请 mock 引擎）

`object HomeTodoStore`（`HomeTodoStore.kt:18`），KDoc `:14-17`：进程级 home 待办/伙伴待确认 mock，「mirrors Flutter HomeTodoApi seed + approveJoin / rejectJoin」。它镜像的是**同名契约 API 的形状**，而不是调用它 —— 与 `api/HomeTodoApi.kt` 无任何依赖关系。

状态：`version`（`:19-20`，`private set` + `bump()` `:22-24`）、`private val applications = mutableListOf(3 条 HomeTodoJoinApplication)`（`:26-54`）—— 种子数据 1001 赵倩（`u_zhao`，销售顾问）、1002 孙浩（`u_sun`，售后技师）、1003 王小明（`u_wang`，网销），`storeId` 恒 1、`status` 全 0（pending），`createdAtLabel` 是「昨天申请/今天申请/前天申请」这类**中文标签串**（不是日期，契约层对应字段是可空的 `createdAt: String?`）。

| 函数 | 行号 | 行为 / 返回 |
|---|---|---|
| `pendingApplications(): List<HomeTodoJoinApplication>` | `:56-57` | `applications.filter { it.isPending }`（即 `status == 0`）——返回新列表 |
| `approveJoin(applicationId: Int): Boolean` | `:59-65` | `indexOfFirst { id 匹配 && isPending }`；未命中 → **false**；命中 → `copy(status = 1)` 原位替换 + `bump()` → true。**重复审批第二次返回 false**（已被 `status=1` 过滤掉） |
| `rejectJoin(applicationId: Int): Boolean` | `:67-73` | 同构，写入 `status = 2` |
| `todoCards(): List<HomeTodoCard>` | `:76-112` | 每次调用**现构造 4 张卡**（无缓存）。仅第 1 张是真派生：`count = pendingApplications().size`、`subtitle` 随 0/非 0 变「暂无待审申请」/「N 位新成员等待审核」（`:82`）。其余三张是硬编码：待跟进客户 `count = 5`、订单待审核 `count = 2`、售后预约 `count = 4`（`:94,102,110`） |
| `resetForTests()` | `:115-123` | `applications.clear()` 后用**位置参数**重新塞回同 3 条种子 + `bump()` |

路由：`todoCards()` 的 `actionRoute` 取 `core.router.HomeRoutes.{TodoPartner,TodoFollowUp,TodoOrderReview,TodoAfterSales}`（`:84,92,100,108`）。文件还 import 了 `AiRoutes`/`ClassroomRoutes`/`CommunityRoutes`/`ContentRoutes`/`MineRoutes`/`VideoRoutes`（`:6-12`），但**本文件内未使用**（未找到引用；grep `rg -n "AiRoutes|ClassroomRoutes|CommunityRoutes|ContentRoutes|MineRoutes|VideoRoutes" feature/home/src/commonMain/kotlin/com.example.my_kmp_project/feature/home/HomeTodoStore.kt`）—— 遗留 import。

副作用：无 HTTP、无 KV、无协程。调用点：`HomeRouteHost.kt:3241`（`rejectJoin`）、`HomeRouteHost.kt:3257`（`approveJoin`）、`HomeLogicTest.kt:75,81,83,85,92`。

#### 16A.6.4 `HomeFavoritesStore.kt`（72 行 · **唯一真落盘的 home Store**）

`object HomeFavoritesStore`（`HomeFavoritesStore.kt:10`），KDoc `:6-9`：AllServices 的收藏服务，对位 Flutter `AllServicesRepository`，key `home_favorite_service_ids_v2`，**最少 3 / 最多 8**。

| 成员 | 行号 | 语义 |
|---|---|---|
| `const val MinFavorites = 3` / `MaxFavorites = 8` | `:11-12` | 收藏数量上下界 |
| `private const val PrefKey = "home_favorite_service_ids_v2"` | `:13` | **真实 KV key 名**（`_v2` 说明换过一次 schema） |
| `private val defaultIds = HomeMockData.favoriteServices.map { it.id }` | `:15-16` | 默认 8 个 id：`intro/glass/diet/drawer/diary/training/running/wave`（`HomeMockData.kt:166-175`） |
| `private var ids: LinkedHashSet<String> = LinkedHashSet(loadOrDefault())` | `:18` | **首次访问该 object 时同步读盘**（Compose 里由 `HomeServicesGrid`/AllServices 页引用时触发）；用 `LinkedHashSet` 保序 |

持久化链路（唯一一处 home 侧真 IO）：`loadStringList(PrefKey)` / `saveStringList(PrefKey, ids.toList())`（`:21`、`:41`），二者定义在 `core/platform/src/commonMain/kotlin/com.example.my_kmp_project/core/platform/StringListStore.kt:20-24`，转发到 `platformPrefsStore`（`getStringList`/`putStringList`）。该 store 由 `createKeyValueStore(PLATFORM_PREFS_NAME)` 惰性创建，`PLATFORM_PREFS_NAME = "home_favorites"`（`:16-18`）；`:5-15` KDoc 说明这是**单一 KV 契约之上的共享 prefs 桶**（home 收藏、Mine 功能顺序/深色/语言、社区公约确认、IM 会话 id 全在这一个文件里），并明确「**Absent vs empty**」语义：`null` = 用户从未选过 → 调用方回落默认；空列表 = 真实存储值。

| 函数 | 行号 | 行为 |
|---|---|---|
| `private fun loadOrDefault(): List<String>` | `:20-23` | `loadStringList(PrefKey) ?: return defaultIds`；有值则 `normalize(stored)` |
| `private fun normalize(raw: List<String>): List<String>` | `:25-38` | 字典 = `favoriteServices + catalogSections.flatMap { it.items }` 按 id 建索引；① 过滤未知 id + `distinct()` ②>8 截断 `take(8)` ③<3 则按 `defaultIds` 顺序补齐到 3 ④仍<3（默认集本身不足时）直接 `defaultIds.take(3)`。**因此磁盘脏数据永远被清洗成合法区间** |
| `private fun persist()` | `:40-42` | `saveStringList(PrefKey, ids.toList())`（保序落盘） |
| `fun snapshot(): Set<String>` | `:44` | `ids.toSet()` —— 返回不可变副本 |
| `fun canRemove(): Boolean` | `:46` | `ids.size > MinFavorites` |
| `fun canAdd(): Boolean` | `:48` | `ids.size < MaxFavorites` |
| `fun remove(id: String): Boolean` | `:50-55` | `!canRemove() \|\| id !in ids` → false；否则删 + `persist()` → true |
| `fun add(id: String): Boolean` | `:57-62` | `!canAdd() \|\| id in ids` → false；否则增 + `persist()` → true |
| `fun favoriteItems(): List<AllServiceItem>` | `:64-71` | 用「收藏 + 全目录」字典 `mapNotNull`；**若结果为空则回落到 `favoriteServices.take(3)`** —— 保证 UI 永不出空 |

并发/协程：**无 `version` 快照计数器**（与其他三个 Store 不同，它靠 `snapshot()` 返回新 `Set` 让 Compose `remember` 感知）、无同步原语、无协程；读写是**同步阻塞**的 KV 调用，发生在调用它的组合/事件线程上。

#### 16A.6.5 `HomeTodoModels.kt`（86 行 · UI 侧待办模型 + 打包器）

KDoc `:3-6`：Flutter `home_todo_models.dart` + `home_todo_packer.dart` 的移植；卡片尺寸由 `type` 推导（ADR 0009）。

| 类型 | 行号 | 内容 |
|---|---|---|
| `enum class HomeTodoSize(val cells: Int)` | `:7-20` | `Small(1)`/`Medium(2)`/`Large(4)`；`companion fun fromType(type: String)`：`partner_pending`→Large、`follow_up_customer`/`after_sales_appointment`→Medium、其余→Small。**与契约层的 `HomeTodoCardSize`（`api/HomeTodoApiModels.kt:22-39`）规则逐字相同、类型名不同** |
| `data class HomeTodoCard(type, title, subtitle, actionLabel, actionRoute, count, imageUrl: String? = null)` | `:22-32` | `val size: HomeTodoSize get() = HomeTodoSize.fromType(type)`（`:31`）。全字段**非空**（除 `imageUrl`），与契约层「Raw 可空 + 派生兜底」形态形成对照 |
| `data class HomeTodoJoinApplication(applicationId, storeId, applicantUserId, applicantName, status, createdAtLabel, roleLabel = "销售顾问")` | `:35-49` | `status` 注释「0 = pending, 1 = approved, 2 = rejected」（`:40-41`）；`displayName get() = applicantName.trim().ifEmpty { applicantUserId }`（`:45-46`，与契约层同逻辑）；**`isPending get() = status == 0`**（`:48`，契约层无此属性）；多出 `roleLabel`、用 `createdAtLabel` 取代 `createdAt` |
| `object HomeTodoPacker` | `:55-86` | 见下 |

`HomeTodoPacker` 两个纯函数（2×2 页、容量 4 格）：

| 函数 | 行号 | 算法 |
|---|---|---|
| `shouldWrapOnly(cards: List<HomeTodoCard>): Boolean` | `:56-60` | 空列表 → true；`size > 2` → false；否则「全部是 Small」才 true（即两张小卡可并排不翻页） |
| `packPages(cards: List<HomeTodoCard>): List<List<HomeTodoCard>>` | `:62-85` | 空 → `emptyList()`；局部 `flush()`（`:68-73`，空页不产出、重置 `current`/`used`）；遍历：`need = card.size.cells`，① `need == 4 && used > 0` → 先 flush（Large 不能跟在别的卡后）②`used + need > 4` → flush ③累加 ④`used >= 4` → flush；末尾再 flush。返回**不可变快照**（`current.toList()`） |


### 16A.7 HomeMockData 与真假数据边界

`HomeMockData.kt`（360 行）文件头注释即定性：`/** Static mock models aligned with Flutter module_home sample data. */`（`HomeMockData.kt:3`）。它**没有任何函数式取数**，除两个纯生成器（`hotRankDetailItems`、`rankItemsForTab`）外全是 `val` 常量列表。

**模型定义（`:5-79`，15 个 UI data class）**：`HomeFeatureItem(label)` `:5`、`HomeQuickAction(title,subtitle,actionLabel)` `:6-10`、`HomeMetric(value,label)` `:11`、`HomeMetricDetail` `:12`、`HomeServiceItem(label, badge: String? = null)` `:13`、`HomeContactItem(title,subtitle,trailing: String?)` `:14-18`、`HomeNewsItem(title,source,date)` `:19-23`、`AllServiceItem(id,label,assetName = "all_functions.png")` `:25-30`、`AllServiceSection(title,subtitle?,showEditButton=false,items)` `:31-36`、`SearchRankItem(id,rank,title,subtitle,coverUrl = picsum 派生)` `:38-44`、`HotRankDetailItem(id,rank,title,subtitle,heat)` `:51-57`、`ReportHighlight(emoji,...)` `:59-64`、`ReportRecord(emoji,...,statusHighlight=false)` `:66-73`、`StrategyAssetCell(label,value,positive)` `:75-79`。顶层辅助 `fun searchRankCoverUrl(rank: Int): String`（`:47-48`，返回 `https://picsum.photos/seed/search-rank-$rank/144/144`）。

**`object HomeMockData`（`:81-360`）内容与消费点**：

| 常量 | 行号 | 内容 | 消费点（file:line） |
|---|---|---|---|
| `greeting` | `:82` | 「早上好，沃德龙鼎」 | 未找到消费点（grep `rg -n "HomeMockData\.greeting" .`） |
| `storeName` | `:83` | 「[4S]北京沃德龙鼎吉利」 | `HomeScreen.kt:118`、`NativeJetpackHome.kt:68` |
| `searchPlaceholder` | `:85` | 首页搜索 chip | `HomeScreen.kt:196`、`NativeJetpackHome.kt:141` |
| `searchPagePlaceholder` | `:88` | 搜索页占位 | `HomeSearchScreen.kt:90`（经 `getOrElse`） |
| `features`（9 项） | `:90-100` | H5 调试/生活服务/二手车/新车成交/新车跟进/AI小石头/Club/直播带货/更多 | `HomeScreen.kt:204`、`NativeJetpackHome.kt:149` |
| `quickActions`（4 项） | `:102-107` | **与 `HomeTodoStore.todoCards()` 的 4 张卡文案逐字重复**（「3 位新成员等待审核」等） | 未找到 `HomeMockData.quickActions` 消费点（`HomeScreen` 用的是 `HomeTodoCardStrip`，见 `HomeTodoCardStrip.kt:43`）——grep `rg -n "HomeMockData\.quickActions" .` |
| `metricsToday`/`metricsYesterday`/`metricsMonth` | `:109-128` | 硬编码数字 99/2/999.8/15、86/1/520.0/12、1280/45/8600.5/320 | `HomeScreen.kt:227-229`、`NativeJetpackHome.kt:166-168` |
| `metricDetails` | `:130-134` | 待交车 8 / 待回访 3 / 待跟进 12 | `HomeScreen.kt:231`、`NativeJetpackHome.kt:170` |
| `services`（8 项） | `:136-145` | 朋友圈(热门)/视频号/直播(新品)/素材库/话术库/培训/竞品分析/更多 | `HomeScreen.kt:250`、`NativeJetpackHome.kt:188` |
| `contacts` | `:148-151` | 销售顾问小王、售后服务热线 400-800-8888 | `HomeScreen.kt:258`、`NativeJetpackHome.kt:195` |
| `news`（1 项） | `:154-156` | 吉利银河 E8 获年度车型奖 | `HomeScreen.kt:259`、`NativeJetpackHome.kt:196` |
| `toolEntries`（4 对） | `:159-164` | `HomeFeatureItem to "media"/"web"/"friend"/"classroom"` | 未找到消费点（grep `rg -n "toolEntries" .`） |
| `favoriteServices`（8 项） | `:166-175` | id/中文名/asset 文件名三元组 | `HomeFavoritesStore.kt:16,26,65,69`（**默认收藏集与归一字典的来源**） |
| `catalogSections`（4 节 23 项） | `:177-221` | 线索/营销/教学/其他服务 | `AllServicesScreen.kt:97`、`HomeFavoritesStore.kt:26,66` |
| `searchHistory` / `searchDiscovery` / `filterTags` | `:223-246` | 搜索历史/发现/筛选标签（含「罗振宇2026跨年演讲」重复项） | `HomeSearchScreen.kt:91,92,135` |
| `rankTabs`（4） | `:248-249` | 热配榜/诵读榜/剧集榜/记录榜（注释说明 Flutter SoT 只有 4 tab，合作榜不在 gold） | `HomeSearchScreen.kt:140` |
| `hotRankCategories`（6）/`hotRankAgeFilters`（4） | `:251-255` | 侧栏标签 | `HomeRouteHost.kt:1731,1732` |
| `private hotRankTitles/Subtitles/Heats/Ranks` | `:257-276` | 生成 20 条明细的素材池；`hotRankRanks` 里第 4 个是 **88**（刻意的脏值） | 仅 `hotRankDetailItems` 内部 |
| `fun hotRankDetailItems(categoryIndex: Int): List<HotRankDetailItem>` | `:279-290` | 每类 20 条；`id = "${前缀}_item_${i+1}"`；标题/副标题/热度按取模复用，rank 直取 `hotRankRanks[i]`；越界类别回落 `"hot"` | `HomeRouteHost.kt:1737` |
| `fun rankItemsForTab(tabIndex: Int)` | `:292-331` | 5 个分支（1 诵读/2 剧集/3 记录/4 合作/else 热配 8 条带 picsum 封面） | `HomeRouteHost.kt:1528`（`rankItemsForTab(0).take(3)`）、`HomeSearchScreen.kt:94` |
| `reportHighlights`(3) / `reportRecords`(5) | `:333-345` | emoji + 文案的学习报告 | `LearningReportScreen.kt:98,100,120` |
| `strategyTabs`/`strategyPeriods`/`strategyAssets`(9) | `:347-359` | 理财策略演示（A股 +19.22% … 现金 +1.20%） | `StrategyScreen.kt:82,157,178` |

**边界结论（逐条）**：

| 数据域 | 当前来源 | 证据 |
|---|---|---|
| 首页仪表盘（问候/门店/指标/服务/联系人/资讯/搜索） | **100% mock** | 上表 `HomeScreen.kt:118-259` 全部读 `HomeMockData` |
| 待办卡片与入店申请 | **mock Store**（`HomeTodoStore`），契约层 `HomeTodoApi` 已实现但无人调用 | `HomeTodoCardStrip.kt:43`、`HomeRouteHost.kt:3194,3241,3257` |
| 签到/积分 | **mock Store**（`HomePointsStore`），契约层 `PointsApi` 无调用 | `HomeScreen.kt:165`、`HomeRouteHost.kt:928,1071` |
| 二手车/售后/新车跟进（列表+详情+新建） | **mock Store**（`HomeSecondaryStore`），契约层 `AfterSalesApi`/`NewCarFollowApi`/`TransactionApi` 无调用 | `HomeRouteHost.kt:146-333,2432-3043` |
| 全部服务收藏 | **mock 目录 + 真实 KV**（唯一落盘项） | `HomeFavoritesStore.kt:13,18,41` |
| 搜索/热榜/学习报告/策略 | **100% mock**（含 picsum 外链封面） | `HomeSearchScreen.kt:90-140`、`HomeRouteHost.kt:1731-1737`、`LearningReportScreen.kt:98-120`、`StrategyScreen.kt:82-178` |

**重复定义坑**：`HomeMockData.quickActions`（`:102-107`）与 `HomeTodoStore.todoCards()`（`HomeTodoStore.kt:78-111`）是同一份文案的两个副本，前者无消费点；前者把「3 位新成员」写死，后者会随 `pendingApplications().size` 变化。改动待办文案时两处都要看。

### 16A.8 测试

#### 16A.8.1 `api/HomeRecordingApiClient.kt`（115 行 · 测试替身）

`internal class HomeRecordingApiClient(private var raw: String = EMPTY_OK) : ApiClient`（`HomeRecordingApiClient.kt:16-18`）。KDoc `:9-15` 说明它是什么：**记录 verb/path/query/body，把预置的 BFF 原始响应体交给真实解析器 `ApiEnvelopeParser`**（与 `KtorApiClient` 同一条解析路径），所以测试覆盖的是「请求形状 + 信封解码」两件事，**不发网络请求**。与 `feature/mine` 的 `MineRecordingApiClient` 同形，各 lane 各持一份以避免跨 feature 依赖（`:14`）。

| 成员 | 行号 | 行为 |
|---|---|---|
| `data class Call(method, path, query, body)` | `:21-26` | 一次调用记录；注释明确 GET/DELETE 的 `body` 是空串，**无 body 的动作 POST 也是空串** |
| `val calls: MutableList<Call>` | `:28` | 全量历史（测试可断言调用次数） |
| `val last: Call get() = calls.last()` | `:31-32` | 最近一次，用于断言请求形状 |
| `fun stub(raw: String)` | `:34-36` | 替换下一次的应答原文（`raw` 是 `private var`，一次改全局生效直到再改） |
| `private fun record(...): String` | `:38-41` | 唯一记录点：`calls += Call(...)` 然后 `return raw` |
| `getRaw` / `getAbsoluteRaw` | `:43-47` | 记 `"GET"`（后者把整 URL 当 path、query 为空 map） |
| `postRaw` / `putRaw` / `patchRaw` / `deleteRaw` | `:49-64` | 记对应 verb；`postForm` `:52-55` 记 `"POST_FORM"`（fields 当 query）并返回 `HttpTextResponse(200, raw)` |
| `getApi` / `postApi` / `putApi` / `patchApi` / `deleteApi` | `:66-109` | **`ApiEnvelopeParser.parse(raw = record(...), parseData = parseData)`** —— home 的六个 API 类只走 GET/POST/PATCH 三个，其余接口是为满足 `ApiClient` 契约而补齐 |
| `private const val EMPTY_OK = """{"code":0,"message":"ok","data":null}"""` | `:111-114` | 默认应答：空成功信封，注释说明用意是「避免未 stub 时静默当失败」 |

关键性质：因为 `record` 返回同一份 `raw`，一个测试里连续多次调用会拿到**同一响应体**（`approveJoin(42)` + `rejectJoin(43)` 那种用例靠这一点共用 stub，见 `HomeTodoApiTest.kt:100-111`）。

#### 16A.8.2 `api/HomeTodoApiTest.kt`（186 行 · 7 个用例）

夹具：`client = HomeRecordingApiClient()`、`api = HomeTodoApi(client)`（`:20-21`）。文件头 `:11-17` 声明覆盖面 = 契约表 §2.4 #15–#21：B 型形状、尺寸客户端推导（ADR 0009）、无 body 动作 POST（§5.12）、`_data()` 的「message 优先、空则中文兜底」。

| 用例 | 行号 | 断言的语义（逐条） |
|---|---|---|
| `todoCardsReadItemsArrayAndDeriveSizeFromType` | `:24-58` | ① path 恰为 `/api/v1/home/todo-cards`、query **必须为空**（`:37-38`）；② 4 行 `data.items` 全解析；③ **尺寸只由 type 推导**：`partner_pending`→Large 且 `cells == 4`、`follow_up_customer`/`after_sales_appointment`→Medium(`cells 2`)、`order_pending_review` 走 default→Small(`cells 1`)（`:42-50`）；④ `image_url: null` 保留为 null（`:54`）；⑤ 缺 `action_label` 时回落 **"去查看"**（`:56`）；`action_route` 原样（`:57`） |
| `todoCardsAlsoAcceptBareArrayData` | `:61-68` | `data` 直接是数组也能读；缺失字段 → `title == ""`、`size` 由 type 推导为 Large |
| `joinApplicationsReadItemsShapeAndParseCreatedAt` | `:71-96` | ① 数字字段（`application_id`/`store_id`/`status`）取到值；② 可解析 ISO `2026-10-16T09:30:00Z` **原样保留**（`:91`）；③ `"not-a-date"` → `createdAt == null`（`:95`）；④ `applicant_name = " 李雷 "` → `displayName == "李雷"`（trim 生效，`:93`）；⑤ 名字为空 → 回落 `applicant_user_id` `"u-10"`（`:94`） |
| `approveAndRejectAreBodylessActionPosts` | `:99-112` | 两次动作分别记为 `POST` + `/api/v1/home/join-applications/{42,43}/{approve,reject}` + **`body == ""`** —— 即「真的发出请求且 body 为空」，而非省略（契约表 §5.12） |
| `approveFailureUsesEnvelopeMessageThenChineseFallback` | `:115-123` | `code:403` + message → `NetworkError.Business.code == 403`、message 用服务端文案；`code:500` + `message:""` → 落到中文兜底「拒绝失败」 |
| `followUpCustomersAppointmentsAndReviewOrdersApplyDartDefaults` | `:126-148` | 三个端点的 Dart 默认值：缺 `display_name`→「客户」、缺 `customer_name`→「预约客户」、缺 `title`→「审核单」；`appointment_date` 缺失 → null；**带空格分隔的 `2026-10-16 08:00` 也算可解析 ISO**（`:147`） |
| `listFailureThrowsWithFallbackCopyPerEndpoint` | `:151-185` | 文案优先级三连：① `code:500`+message → 用服务端「后端炸了」；② `code:0,message:"success",data:null` → 抛 **`"success"`**（Dart `_data()` 非空 message 赢过兜底，`:158-164`）；③ 只有信封**不带 message 键**时才轮到各调用点中文兜底：「加载店务审核单失败」「加载售后预约失败」「加载入店申请失败」「加载待办卡失败」（`:168-184`） |

#### 16A.8.3 `api/HomeTransactionApiTest.kt`（155 行 · 9 个用例）

夹具 `api = TransactionApi(client)`（`HomeTransactionApiTest.kt:22`，注意类名无 Home 前缀）。覆盖面声明 `:13-18`：`limit`/`offset` 方言（page 0-based）、可选 `type`、双信封、`id` 缺失必须抛、详情的「交易记录不存在」。

| 用例 | 行号 | 断言语义 |
|---|---|---|
| `listQueryIsLimitOffsetWithZeroBasedPage` | `:25-42` | `page=2,size=20` → query **恰为** `{limit:"20", offset:"40"}`；`page=0,size=7` → `{limit:"7",offset:"0"}`；带 `type="income"` → 追加 `"type" to "income"`；**`type="   "` 时 query 不含 `type` 键**（空白也算省略，`:40-41`） |
| `listEnvelopeDecodesBothKeyCasingAndDefaults` | `:45-85` | snake 行全字段（`user_id`/`note`/`created_at`/`updated_at`）取到；camel 行 `userId`/`createdAt` 照收；缺 `category` → `""`，缺 `note` → **null**（两种语义并存）；`pagination{page,size:20,total:41}` → `page.hasMore(20)` 为 true（Dart `ListData.resolveHasMore`） |
| `bareBodyWithoutCodeIsTreatedAsSuccess` | `:88-95` | body 无 `code` 键（`{"items":[...]}`）→ 整份当 ListData、合成 `code=0`，正常返回 1 行 |
| `bareArrayBodyAlsoDecodes` | `:98-101` | **根级裸数组** `[{"id":1},{"id":2}]` → id 列表 `[1,2]` |
| `businessFailureWithoutDataYieldsEmptyPageLikeDart` | `:104-109` | `{"code":403,"message":"无权限"}`（有 code、无 data）→ **空页且不抛**（Dart 仓储完全不看 `code`） |
| `rowWithoutIdFailsLoudlyInsteadOfDefaultingToZero` | `:112-117` | 行缺 `id` → 抛 `Exception` 且 **message 含 "id"**（`decodeStrictTransactionPage` 的逐行严格解码生效） |
| `transactionModelRequiresIdOnTheWire` | `:120-129` | 绕过 API 直接测 DTO：`{"type":"expense"}` 抛；`{"id":1}` 得 `id == 1` |
| `detailDecodesEnvelopeAndBareBody` | `:132-147` | 信封形态 path = `/api/v1/transactions/7`、`amount` 整数 1000 → `1000.0`；裸 body `{"id":8,...}` 也能解码 |
| `detailThrowsTransactionNotFoundWhenDataMissing` | `:150-154` | `code:0` + `data:null` → `NetworkError.Business`，message **恰为「交易记录不存在」** |

#### 16A.8.4 `api/PointsApiTest.kt`（229 行 · 11 个用例）

覆盖面声明 `PointsApiTest.kt:11-20`：`today_reward`/`reward` 的 **5** 缺省、三个布尔位只认字面 `true`、#23/#25 无 body 动作 POST（`:code` 是字符串任务码）、#24 手搓转换器三种形状、`progress` 域；并明确 **#26 不在此类**。

| 用例 | 行号 | 断言语义 |
|---|---|---|
| `statusDecodesCalendarAndDartDefaults` | `:27-56` | path `/api/v1/points/status`；`balance 320`/`streak 3`/`today_reward 8`；3 格日历：第 1 格 `signed:true` → true；第 2 格 **`"signed":"yes"` → false**（字符串不算签过，`:51`）且 `reward:0` 保留 0；第 3 格只有 `day` → **`reward` 缺省 5**（`:54`）、`isToday` 缺省 false |
| `statusAppliesTodayRewardDefaultFiveWhenAbsent` | `:59-65` | `data` 无 `today_reward`/`checked_in_today`/`calendar` → `todayReward == 5`、`checkedInToday false`、`calendar` **空表而非 null** |
| `statusTreatsNonBooleanFlagsAsFalseInsteadOfFailingDecode` | `:68-88` | 脏值全链：`checked_in_today:"true"`（字符串）→ false；`signed:"yes"`、`is_today:1` → false；**`reward:"6"` 数字串 → 6**（FlexibleInt 生效）；随后 `checked_in_today:true` → true。用例注释 `:69-70` 点明这正是把线上字段声明成 `JsonElement` 的理由（否则整个 `data` 被静默丢弃） |
| `statusFailureThrowsLoadSignInStateFailed` | `:91-113` | 文案优先级三段：`code:500,message:"boom"` → 「boom」；`code:0,message:"success",data:null` → **抛 "success"**；信封**不带 message 键** → 「加载签到状态失败」 |
| `checkInIsBodylessPost` | `:116-142` | `POST` + `/api/v1/points/check-in` + **`body == ""`**；四字段解码正确；随后同上的文案优先级（"success" / 「签到失败」） |
| `tasksReadDataItemsAndProgressDomain` | `:145-166` | `data.items` 3 行：`claimable`→Claimable、`claimed`→Claimed、**`"weird-value"`→Incomplete**（default 分支）；注释 `:163-164` 特别澄清 invite 行**带** `reward:30` 所以读 30，不是缺省 |
| `taskRewardAndProgressDefaultWhenKeysAbsent` | `:169-180` | 真正缺键形状 `{code,title}` → `reward == 0`、`progress == Incomplete`（`parse(null)` 走 default） |
| `tasksReadRootItemsWhenDataIsNotAMap` | `:183-192` | **`data` 不存在、`items` 在根** → 仍读到 1 条（`rootFallbackWhenDataMissing` 的效果） |
| `tasksSynthesizeCodeZeroWhenCodeAbsent` | `:195-203` | 裸 body `{"items":[...]}`（无 `code`）→ 成功读到；注释 `:196` 说明「否则 core 会判 -1 失败」 |
| `tasksYieldEmptyListWhenEnvelopeHasNoData` | `:206-210` | `{"code":0,"message":"success"}`（无 data 也无 items）→ **空列表而非抛**，注释明确这是 #24 手搓转换器与 #15–#21 的行为差异 |
| `claimTaskUsesStringCodeInPathAndSendsNoBody` | `:213-228` | `POST` + path **`/api/v1/points/tasks/first_order/claim`**（字符串码原样入 URL）+ 空 body；`code:1,message:""` → 「领取失败」 |

#### 16A.8.5 `aftersales/AfterSalesApiTest.kt`（243 行 · 9 个用例）

夹具：`HomeRecordingApiClient` 跨包复用（`aftersales/AfterSalesApiTest.kt:5` import `feature.home.api.HomeRecordingApiClient`；`internal` 在同一测试编译单元内可见）。测试私有辅助 `lastBody(): JsonObject` / `lastBodyKeys(): Set<String>`（`:29-31`）—— 用 `JsonObject.keys` 断言，**能区分「键缺失」与「键为显式 null」**，这是 #49 body 规则唯一的验证手段。覆盖面声明 `:15-22`。

| 用例 | 行号 | 断言语义 |
|---|---|---|
| `recordsReadRawMapAndPreserveCanCreate` | `:34-77` | query 恰为 `{page:"1", size:"10"}`；`pagination.total=25` → `1*10<25` ⇒ hasMore true；`can_create:true` 保住；行 1：`appointment_id:null` → **`null` 而非 0**、`content:""` → ""、**`mileage:"12000"` 数字串 → 12000**（FlexibleInt）；行 2：**`record_id:"12"` 数字串 → 12**、`mileage:null` → null、缺 `customer_name` → ""；`service_kind` 读成整数 1/0（注释 `:63` 点明读写不对称） |
| `recordsCanCreateRequiresLiteralTrueAndGatesEntry` | `:80-92` | `can_create:"true"` → **false**；`false` → false；**键缺失** → false |
| `recordsHasMoreFallsBackToPageLengthWhenTotalKeyMissing` | `:95-109` | `pagination:{}`（有对象无 total）+ 3 行 + `size=2` → `1*2 < 3` ⇒ hasMore **true**（total 回 null 用本页条数兜底）；`{list:[],pagination:{total:25}},page=3,size=10` → `30<25` false |
| `detailUsesIntRecordIdInPath` | `:112-122` | path `/api/v1/after-sales/records/42`；缺 `store_id` → 0、缺 `plate_no` → "" |
| `createAlwaysSendsFiveKeysIncludingExplicitNulls` | `:125-147` | 只传 3 个必填时 body 键集合**恰为** `{service_kind,title,service_date,customer_name,customer_phone}`；`service_kind` 是 `JsonPrimitive("1")`（字符串）；`customer_name`/`customer_phone` 存在且 `.toString() == "null"`（显式 JsonNull）；四个条件键全缺 |
| `createSendsConditionalKeysWithTrimmedValues` | `:150-185` | 全传时 9 个键齐发；`plateNo="  浙A12345  "` → 发 **trim 值**；`content` 同；`appointment_id=5`、**`mileage=0` 也发**（非 null 即发）；第二次只传空白 plate/content + null mileage/appointment → 键集合退回 5 个 |
| `pendingAppointmentsTruncateDateAtLetterT` | `:188-217` | 四种日期输入：`"...T09:30:00Z"`→`"2026-10-20"`；无 `T` 的 `"2026-10-21"` 原样；**`null` → 字符串 `"null"`**（bug-for-bug，`:213-214`）；**数字时间戳 `1760000000` → `"1760000000"`**（`isLenient` 读成字面量）；`appointment_id:"2"` → 2；缺 `customer_name` → "" |
| `pendingAppointmentsReturnEmptyListWhenListKeyMissing` | `:220-223` | `data:{}` → 空列表（不抛） |
| `failuresThrowBareExceptionWithEnvelopeMessage` | `:226-242` | **`assertFalse(error is NetworkError.Business)`** —— 售后路线抛裸 `Exception`（契约表 §2.9）；`message:""` → 中文兜底「加载详情失败」；`message:"success"` + `data:null` → `create` 与 `fetchPendingAppointments` 都报 "success" |

#### 16A.8.6 `newcar/NewCarFollowApiTest.kt`（282 行 · 13 个用例，最严的 body 形状测试）

同样自带 `lastBody()` / `lastBodyKeys()` 辅助（`:29-31`）。覆盖面 `:15-22`：tab 中文字面量、请求页 `hasMore` 方言、#43 互斥分支、#44 稀疏 PATCH、#46 两键不同省略条件、失败抛裸 `Exception`。

| 用例 | 行号 | 断言语义 |
|---|---|---|
| `summaryDecodesStatsWithDartDefaults` | `:34-54` | path `/api/v1/new-car-follow-files/summary`；**`"active":"7"` 数字串 → 7**（FlexibleInt），其余三个整数照收 |
| `summaryTreatsMissingStatsAsAllZero` | `:57-66` | `data` 无 `stats` → 四个统计全 0（走 `NewCarFollowStats.Empty`）；缺 `avatar_url` → "" |
| `listSendsChineseTabLiteralsAndOmitsFilterForAll` | `:69-87` | `All` → query 恰 `{page:"1",size:"10"}`；`High` page2 size5 → `{page:"2",size:"5",intent_band:"高"}`；`Medium`/`Low` → `中`/`低`；`Overdue` → `{...,overdue:"1"}` 且**不含 `intent_band`** |
| `listDecodesFileWithStringifiedIds` | `:90-116` | **`file_id:123` → 字符串 `"123"`**、`customer_id:456` → `"456"`（`isLenient` + `orEmpty`，对位 Dart `'${...}'`）；`nextFollowUpAt` 原样保留 ISO；`total=25` page1 size10 → hasMore true；`page=3` → false |
| `listHasMoreFallsBackToPageLengthWhenPaginationMissing` | `:119-125` | 无 `pagination` + 3 行 + size=2 → `1*2 < 3` ⇒ true |
| `listThrowsWhenDataMissingInsteadOfReturningEmptyPage` | `:128-134` | `data:null` → **抛**（不能当空列表）；`assertFalse(error is NetworkError.Business)`；message = "success" |
| `failureThrowsBareExceptionNotBusinessError` | `:137-154` | `code:500` → 裸 Exception「服务不可用」；`message:""`+`data:null` → 中文兜底「加载详情失败」；**`stub("not json at all")` 时 `fetchLogs` 也抛**（畸形 body 路径） |
| `detailUsesStringFileIdInPath` | `:157-166` | path `/api/v1/new-car-follow-files/F-9`；缺 `customer_phone` → "" |
| `createWithCustomerIdOmitsDisplayNameAndPhoneKeys` | `:169-191` | `customerId=88` 分支：body 键集合**恰为** `{follow_level, customer_id, vehicle_interest, next_follow_up_at}` —— 传了 `displayName`/`phone` 也**整键不出现**；`customer_id` 发 `JsonPrimitive(88)`；`vehicleInterest="  轿车  "` → 发 trim 值 |
| `createWithoutCustomerSendsBothNullableKeysExplicitly` | `:194-215` | `customerId=0` 分支（`> 0` 才走前一分支）：键集合 `{follow_level, display_name, phone}`，两值 **显式 `"null"`**；`vehicle_interest`/`next_follow_up_at` 省略。第二次 `phone=""` → **发 `JsonPrimitive("")`**（空串仍算「总发」），`vehicleInterest="   "` → 整键省略 |
| `patchSendsSparseBodyWithMethodPatch` | `:218-233` | method **`PATCH`**；`stage` 单独 → 键集合恰 `{stage}`；`followLevel="H" + nextFollowUpAt=""` → `{follow_level, next_follow_up_at}` 且后者是 `JsonPrimitive("")`（空串**非 null**，不被 `explicitNulls=false` 省略）；**`api.patch("F-3")` 全空参 → 键集合为空集**（发 `{}`） |
| `logsAreOneBasedAndDecodeInterpolatedIdFields` | `:236-259` | path `/api/v1/new-car-follow-files/123/logs`；默认 query `{page:"1",size:"20"}`；`log_id:7`→"7"、`file_id:123`→"123"、`created_at:1760000000`→"1760000000"；`next_follow_up_at:null`→**null**；`total=7`、`1*20<7` ⇒ hasMore false |
| `createLogOmitsEmptyFollowLevelButSendsEmptyNextFollowUpAt` | `:262-281` | `followLevel=""` → **省略键**，`nextFollowUpAt=""` → **发空串**（键集合 `{body, next_follow_up_at}`）；两键都有 → `{body, follow_level, next_follow_up_at}`；只传 body → 键集合 `{body}` |

#### 16A.8.7 `HomeLogicTest.kt`（223 行 · **4 个测试类挤在一个文件**）

这个文件测的是 mock Store 与打包器，不是契约层（`commonTest/.../home/HomeLogicTest.kt`）。

**`class HomeTodoPackerTest`（`:11-70`）** —— 私有辅助 `card(type)` 造 `HomeTodoCard`（UI 版，`title = type`、`count = 1`，`:12-19`）。`order_pending_review` → Small(1)、`follow_up_customer`/`after_sales_appointment` → Medium(2)、`partner_pending` → Large(4)。

| 用例 | 行号 | 意图 |
|---|---|---|
| `wrapOnly_twoSmall` | `:22-26` | 两张 Small → `shouldWrapOnly` true 且**只 1 页**（不翻页） |
| `largeAloneOnPage` | `:29-33` | 单张 Large → 1 页、页内只有它 |
| `largeThenMediumsSeparatePages` | `:36-47` | Large + 2×Medium → **2 页**；第 1 页只有 Large，第 2 页 2 张（验证 `need == 4 && used > 0` 的先 flush 规则，`HomeTodoModels.kt:77`） |
| `mediumPlusTwoSmallOnePage` | `:50-60` | Medium(2)+2×Small(1) = 4 格 → 恰好 1 页 3 张 |
| `fourSmallOnePage_notWrapOnly` | `:63-69` | 4 张 Small 装满 1 页，但 `shouldWrapOnly` 为 **false**（`size > 2` 直接否决，`HomeTodoModels.kt:58`）——证明「不翻页」与「装满」是两回事 |

**`class HomeTodoStoreTest`（`:72-96`）** —— `@BeforeTest reset()` 调 `HomeTodoStore.resetForTests()`（`:73-76`）。

| 用例 | 行号 | 意图 |
|---|---|---|
| `approveAndRejectReducePending` | `:79-86` | 种子 3 条 pending → `approveJoin(1001)` true 且降到 2 → `rejectJoin(1002)` true 降到 1 → **再次 `approveJoin(1001)` 返回 false**（`assertFalse` 带注释「already approved」，锁定幂等语义） |
| `partnerCardCountTracksPending` | `:89-95` | `todoCards()` 里 `partner_pending` 卡的 `count` 从 3 变 2 —— 唯一验证「卡片计数真派生自 pending 列表」的断言 |

**`class HomePointsStoreTest`（`:98-163`）** —— `@BeforeTest` 调 `HomePointsStore.resetForTests()`。

| 用例 | 行号 | 意图 |
|---|---|---|
| `shouldShowDialog_mirrorsFlutter` | `:105-146` | 五个分支：未登录 false；已登录但今日已签 false；`ackDate == todayLocal` false；未签且未 ack → true；`ackDate` 是昨天 → true |
| `checkInOncePerDay` | `:149-156` | 首次 `checkIn()` 非 null、`points == 5`（即 `DefaultTodayReward`）、`checkedInToday` 转 true、`balance == 5`；**第二次返回 null** |
| `dialogAckPersists` | `:159-162` | `markDialogAcked("2026-09-29")` → `dialogAckDate` 读到同值。注意名字有误导：它只验证**进程内字段**，没有任何落盘断言 |

**`class HomeSecondaryStoreTest`（`:165-223`）** —— **没有 `@BeforeTest`**（`HomeSecondaryStore` 也不提供 `resetForTests`）。

| 用例 | 行号 | 意图 |
|---|---|---|
| `usedCarValidateAndCreate` | `:167-193` | 三条校验文案断言（空客户名→「请选择客户」、空 VIN→「请完整填写车况与金额」、合法→ null）；随后 `createUsedCar(customerName = "张三 138")` → 列表 `before + 1`、`selectedUsedCarId == row.id`、`findUsedCar(row.id) == row`。**注意它没断言 `customerName` 被 `substringBefore(' ')` 截成「张三」**（`HomeSecondaryStore.kt:195` 的行为无测试覆盖） |
| `afterSalesAndNewCarCreate` | `:196-222` | 两条流水线的「姓名/手机必填」文案 + 创建后计数 +1 + 选中 id 同步。**未断言 `submittedDate`/`statusLabel` 等写死值** |

**未覆盖清单**：`HomeFavoritesStore`（归一逻辑 `normalize`、3/8 上下界、`add`/`remove` 返回值）**没有任何测试**；`HomeSecondaryStore.findXxx(null)` 的「回退第一条」行为、`create*` 的写死字段、`HomeApiWire` 的 `homeRequestedPageHasMore`/`totalOrNull`、`HomeTimeWire.todayLocalString` 的 padStart、`HomeMockData` 全部无测试。grep：`rg -n "HomeFavoritesStore" feature/home/src/commonTest` → **零命中**。

### 16A.9 真实状态与坑

| # | 事实 | 证据 |
|---|---|---|
| 1 | **契约层零生产调用**：`HomeTodoApi`/`TransactionApi`/`PointsApi`/`AfterSalesApi`/`NewCarFollowApi` 五个类只出现在各自测试与类文件自身，UI 层一处没用 | grep `rg -n "HomeTodoApi\|HomeTransactionApi\|PointsApi\|AfterSalesApi\|NewCarFollowApi" --glob '**/*.kt' .` → 命中仅 `feature/home/src/commonMain/.../{api,aftersales,newcar}/*` 的定义行与 `commonTest/*` 夹具行（`HomeTodoApiTest.kt:21`、`HomeTransactionApiTest.kt:22`、`PointsApiTest.kt:24`、`AfterSalesApiTest.kt:26`、`NewCarFollowApiTest.kt:26`） |
| 2 | 界面用的是 mock：`HomePointsStore.checkIn()`（`HomeScreen.kt:165`、`HomeRouteHost.kt:928,1071`）、`HomeTodoStore.approveJoin/rejectJoin`（`HomeRouteHost.kt:3241,3257`）、`HomeSecondaryStore.create*`（`HomeRouteHost.kt:2444,2904,2995`） | 见 16A.6/16A.7 |
| 3 | **类名/文件名不一致**：文件 `HomeTransactionApi.kt` 里类是 `TransactionApi`；文件 `HomeTransactionModels.kt` 里 `TransactionModel`。而 `HomeTodoApiModels.kt` 与 `HomeTodoApi.kt` 的类都带 `Home` 前缀 | `api/HomeTransactionApi.kt:30`、`api/HomeTransactionModels.kt:24` |
| 4 | **同名类型双份**：`HomeTodoCard`、`HomeTodoJoinApplication`、尺寸枚举（`HomeTodoCardSize` vs `HomeTodoSize`）在 `feature.home` 与 `feature.home.api` 各有一份，字段可空性不同。`HomeTodoStore.kt` 用的是 UI 版（同包），任何 `import com.example.my_kmp_project.feature.home.api.*` 的改动都会引发歧义 | `HomeTodoModels.kt:22,35,7` vs `api/HomeTodoApiModels.kt:43,76,22` |
| 5 | `ListPage<*>.totalOrNull()`（`HomeApiWire.kt:165`）**零调用者** —— 死代码 | grep `rg -n "totalOrNull" .` → 只有定义行与 KDoc 提及 |
| 6 | `AfterSalesApi.fetchRecords` **没复用** `homeRequestedPageHasMore`，而是就地写 `page * size < total`（`AfterSalesApi.kt:51`）；`homeRequestedPageHasMore` 只被 newcar 两处使用 | grep 结果：`newcar/NewCarFollowApi.kt:64,139` 两处 |
| 7 | `HomeTodoStore.kt:6-12` 七个 router import 里 **六个未使用**（只 `HomeRoutes` 被 `:84,92,100,108` 用到） | 见 16A.6.3 |
| 8 | **两条错误语义并存**：`home_todo`/`points` 路线抛 `NetworkError.Business`（→ 上层可按业务码分类），`aftersales`/`newcar` 路线抛**裸** `Exception`（丢失 code）。这是照抄 Dart 的刻意差异，测试用 `assertFalse(error is NetworkError.Business)` 锁定（`AfterSalesApiTest.kt:230`、`NewCarFollowApiTest.kt:132,141`）。接线时若统一 catch `NetworkError` 会漏掉后两条线 | `HomeApiWire.kt:126-137` |
| 9 | **`message` 缺省差异**：Dart `ResultModel` 缺 message 记空串 ⇒ `Exception('')`；core 解析器把空白归一为 null ⇒ KMP 落到中文兜底。`HomeApiWire.kt:130-133` 声明为「刻意为之」，测试 `NewCarFollowApiTest.kt:144-150` 覆盖 | — |
| 10 | **`code:0` + `data` 缺失也抛**，且抛出文案是服务端的 `"success"`（不是中文兜底）—— 这是 `_data()` 方言的真实后果，接线时必须把 `message` 当不可信文案 | `HomeTodoApiTest.kt:160-164`、`PointsApiTest.kt:100-104`、`AfterSalesApiTest.kt:237-241` |
| 11 | **#12/#13 流水的失败语义与别的列表相反**：业务码非 0 且无 `data` ⇒ **静默空页不抛**（因为 Dart 仓储完全不看 code）；而行缺 `id` ⇒ **整次调用抛**。两个方向相反的规则同时生效 | `HomeTransactionApi.kt:21-28`、`HomeTransactionApiTest.kt:104-117` |
| 12 | **`claimTask(code)` / `fetchDetail(fileId)` / `patch(fileId)` 都把入参原样拼进 URL，无百分号编码**（`PointsApi.kt:60`、`NewCarFollowApi.kt:70,122`）。`NewCarFollowTab` 的 query 里则**必须发中文字面量**（`NewCarFollowModels.kt:21-27`），两条路径的编码策略不对称，接线时都要实测 | — |
| 13 | **page 基数三套并存**：`/transactions` 是 **0-based + limit/offset**（`HomeTransactionApi.kt:18-19`）；`/after-sales/records`、`/new-car-follow-files`、`.../logs` 是 **1-based + page/size**（`AfterSalesApi.kt:33-34`、`NewCarFollowApi.kt:49`）；`/points/tasks` 无分页。`queryOf`（`core/network/.../ListPage.kt:92-93`）丢 null **与空白串**，所以 `type = "   "` 不发键 | — |
| 14 | **`HomePointsStore` 不落盘，但 key 名是真的**：`PrefsKey` 在 KMP 侧零读写；同名 key `check_in_dialog_ack_date` 由 Android 原生壳用 SharedPreferences 独立读写（`composeApp/src/androidMain/.../NativeJetpackHome.kt:83,215`）。切日/重装后 Compose 侧的 `checkedInToday` 会重置 ⇒ **可以重复签到领积分** | grep 见 16A.6.1 |
| 15 | **唯一真持久化**是 `HomeFavoritesStore`：key `home_favorite_service_ids_v2`，落在 `createKeyValueStore("home_favorites")` 这个共享桶里（`core/platform/.../StringListStore.kt:16-18`），且是 `object` 初始化时**同步读盘**（`HomeFavoritesStore.kt:18`）—— 主线程 IO，无预热/协程包装 | — |
| 16 | **四个 Store 全无同步原语**（无 Mutex/Atomic/@Volatile），且 `HomeSecondaryStore` 的 `selected*Id` **没有 `private set`**（`:20-22`），外部可绕过 `selectXxx()` 直接赋值而不 `bump()`，导致依赖 `version` 的 Compose 观察者不刷新 | `HomeSecondaryStore.kt:20-22` vs `HomePointsStore.kt:36-47` |
| 17 | **`HomeSecondaryStore` 无 `resetForTests`**，`HomeLogicTest` 里 `HomeSecondaryStoreTest` 也没有 `@BeforeTest`（`HomeLogicTest.kt:165-167`）⇒ 用例间状态累积、依赖 `before + 1` 相对断言；`HomeTodoStore`/`HomePointsStore` 有 reset。测试执行顺序变化时结果会变 | — |
| 18 | **写死的时间/字段**：`createUsedCar` 的 `submittedDate = "2026-09-29"`（`HomeSecondaryStore.kt:184`）、`createNewCarFollow` 的 `stage="跟进中"`/`nextFollow="今日待排"`/`owner="销售顾问"`（`:282-285`）、`createAfterSales` 的 `appointmentId = null`（`:247`）。`afterSalesCanCreate` 恒 true（`:25`），而契约层它来自 #47 的 `can_create` 字面 true 判定 —— 接线时这里必须改成读 `AfterSalesListResult.canCreate` | — |
| 19 | `HomeMockData.quickActions`（`:102-107`）与 `toolEntries`（`:159-164`）、`greeting`（`:82`）**没有消费点**；`hotRankRanks` 第 4 项是脏值 **88**（`:276`），是刻意的演示数据 | grep `rg -n "quickActions\|toolEntries\|HomeMockData\.greeting" .` |
| 20 | `home` 收藏与 `HomeMockData` 目录**耦合死**：`normalize()` 会把 KV 里存在但目录中已不存在的 id 直接丢弃（`HomeFavoritesStore.kt:26-28`）。改 `catalogSections`/`favoriteServices` 的 id 会静默清掉用户收藏，且 `_v2` key 已说明历史上换过一次 schema | — |
| 21 | **`decodeListPage` 的静默丢行**是本层多个「严格化」补丁的根因：`NetworkJson` 未开 `coerceInputValues`（`HomeTransactionModels.kt:14-18`），所以契约层把线上字段全声明为可空；`decodeStrictTransactionPage`（`HomeTransactionApi.kt:89-93`）与 `homeWireLiteralTrue`（`HomeApiWire.kt:167-175`）都是为了绕开这个静默行为而加的 | — |
| 22 | `chartsVicoMain` 只在 `androidOnly=false` 时才有意义：`build.gradle.kts:23` 用 `matching` 是因为根工程 `build.gradle.kts:118` 的 `if (!skipNative)` 才创建 `iosMain`；CI 用 `-PandroidOnly=true`（AGENTS.md），此时 home 模块只有 Android + commonMain + chartsVicoMain 参与 | — |
| 23 | **未找到** home 契约层任何 SSE/WebSocket 或 `component.*` 的使用：本册 20 个文件的 import 集合里没有 `core.sse`、`component.webview`、`component.routing`（虽然 `build.gradle.kts:29-30` 把它们 `api` 出来了）。grep `rg -n "core\.sse\|component\.webview\|component\.routing" feature/home/src/commonMain/kotlin/com.example.my_kmp_project/feature/home/{api,aftersales,newcar}` → 零命中 | — |
| 24 | `NewCarFollowApi.create` 的 `nextFollowUpAt` **不 trim**（`:103`，只判非空串），而 `vehicleInterest` **trim**（`:102`）；`AfterSalesApi.create` 的 `plateNo`/`content` 则都 trim（`:90,92`）。三个 create 的清洗规则互不一致，接线时不能共用一个表单归一函数 | — |

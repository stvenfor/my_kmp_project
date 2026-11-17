# 16 · feature:home 详解

## 16.0 合订导读

`feature/home` 是全仓最大的业务模块，实测规模（`find feature/home -name '*.kt'`，不含 `build/`）：

| 源集 | 文件数 | 行数 |
|-|-|-|
| `commonMain` | 34 | 11094 |
| `commonTest` | 7 | 1433 |
| `ohosMain` | 1 | 33（只有 `AnalyticsCharts.ohos.kt` 的图表占位 actual） |
| 合计 | 43 | 12716 |

单一 agent 读不完，因此本册由三个互斥子域合订：

- **16A** 数据与接口层：`api/` 8 个文件逐符号、aftersales 与 newcar、24 个端点总表、四个 Store、`HomeMockData` 消费点、6 个测试文件逐用例。
- **16B** 屏与分区：12 个屏文件逐函数、`HomeRouteHost` 的 26 行分派表与 20 个二级屏、首页→二级页跳转链路、沉浸式/inset 合规。
- **16C** 图标与图表：`HomeIcons` 资产矩阵、`AnalyticsCharts` 三端实现差异（含 OHOS 只有 `Box` 占位这一事实）。

读法建议：接手首页业务先 16A 的端点总表，改 UI 先 16B 的 `HomeRouteHost` 分派表，评估图表回归成本只看 16C.5。

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

## 16B · feature:home 的屏与分区

定位：`feature/home` 是 17 个业务 feature 模块里最重的一个，`commonMain` 单目录 24 个文件。本分册（B 部分）覆盖**首页本体 + 首页分区组件 + 路由宿主 + 首页系二级屏**：`HomeScreen` / `HomeSections` / `HomeRouteHost` / `HomeDetailScreens` / `HomeSearchScreen` / `AllServicesScreen` / `DailyCheckInDialog` / `LearningReportScreen` / `LedgerScreens` / `StrategyScreen` / `HomeServiceAssets` / `HomeWebHandlers`。A 部分负责 `HomeIcons`、`HomeMockData`、各 Store 与 api 契约，C 部分负责 `aftersales`、`newcar`、Todo 等旁支（如与本文重叠，以各自 file:line 为准）。

包根：`com.example.my_kmp_project.feature.home`（源码目录名字面带点：`feature/home/src/commonMain/kotlin/com.example.my_kmp_project/feature/home/`，下文统一简写为 `feature/home/.../home/`）。

### 文件清单

| 路径 | 作用 | 行数 |
|------|------|------|
| `feature/home/.../home/HomeScreen.kt` | 首页 Tab 根（AppShell/iOS/OHOS 共用）+ 本地 destination 栈 + 自动签到弹窗编排 | 287 |
| `feature/home/.../home/HomeSections.kt` | 首页各分区可复用 Composable（问候/搜索框/Banner/金刚区/服务宫格/联系人/资讯/门店指标卡/Hub 入口卡） | 989 |
| `feature/home/.../home/HomeRouteHost.kt` | Home 域**二级路由宿主**：`route: String` → 本模块各屏的大 `when`；含 30+ 私有屏实现与 Mock 数据 | 3574 |
| `feature/home/.../home/HomeDetailScreens.kt` | 首页附属详情/表单屏 | 393 |
| `feature/home/.../home/HomeSearchScreen.kt` | `/home/search` 搜索页（热词/历史/结果） | 578 |
| `feature/home/.../home/AllServicesScreen.kt` | `/home/all_services` 全部服务页 | 325 |
| `feature/home/.../home/DailyCheckInDialog.kt` | 每日签到弹窗（纯展示，无网络） | 194 |
| `feature/home/.../home/LearningReportScreen.kt` | `/home/learning_report` 学习报告页 | 345 |
| `feature/home/.../home/LedgerScreens.kt` | `/home/ledger` 台账 + 明细 | 224 |
| `feature/home/.../home/StrategyScreen.kt` | `/home/strategy` 投资策略页 | 392 |
| `feature/home/.../home/HomeServiceAssets.kt` | 服务宫格图标资源与 label→icon 映射 | 125 |
| `feature/home/.../home/HomeWebHandlers.kt` | H5 JSBridge 方法注册（`WebBridgeRegistry`） | 45 |

### 逐文件详解

#### HomeScreen.kt

文件：`feature/home/src/commonMain/kotlin/com.example.my_kmp_project/feature/home/HomeScreen.kt`（287 行）

| 符号 | 行号 | 签名 | 真实行为 |
|------|------|------|----------|
| `HomeScreen` | 37-105 | `@Composable fun HomeScreen(loggedIn: Boolean = false, displayName: String? = null)` | 首页 Tab 根；**无 modifier 参数**。内部 `var destination by remember { mutableStateOf<String?>(null) }`（L42）+ `LocalAppNavigator.current`（L43），自建一个「单值栈」：destination==null 渲染 `HomeRootContent`，非 null 渲染二级屏。 |
| 局部函数 `go(raw: String)` | 45-65 | 内部 local fun，捕获 `navigator`/`destination` | 用 `ProductRouteDispatch.resolve(raw)`（`core/router/src/commonMain/kotlin/com.example.my_kmp_project/core/router/ProductRouteDispatch.kt:62`）解析中文 label/路径。按 `target.host` 分派：`Toast`→`showPlatformToast`（L48）；`Web`→`navigate(AppRoute.InAppWeb(OfflineWebFixtureUrl))`（L49）；`Scan`→`navigate(AppRoute.Scan)`（L50）；`Content`→ 再按 path 命中 `/friend`→`AppRoute.Friend`、`/live`、`/live/room`→`AppRoute.Live`、`/classroom/my_class`、`/classroom`→`AppRoute.Classroom`、`/media/entry`→`AppRoute.Media`，其余落 `destination`（L51-57）；`AllServices`→`destination="services"`（L58）；`Home/Mine/Community`→`destination=target.path`（L59-60）；`Tab`→Toast「请切换底部 Tab」（L61）；`Auth`→Toast「请先登录」（L62）；`Unknown`→`destination=raw` 兜底交给 `HomeRouteHost`（L63）。**`go` 不发任何网络请求**，只做本地导航。 |
| destination 渲染 | 67-104 | — | `"services"` 特判渲染 `AllServicesScreen(onBack, onOpen)`（L73-79，`onOpen` 先清栈再 `go(label)`）。其它 dest 用 `ProductRouteDispatch.hostForPath(dest)`（`ProductRouteDispatch.kt:112`）四向分派：`Home`→`HomeRouteHost`（L82）、`Content`→`CrossFeatureScreens.ContentRouteScreen`（L87）、`Mine`→`CrossFeatureScreens.MineRouteScreen`（L92）、**else 兜底也走 `HomeRouteHost`**（L97）。所有二级屏统一注入 `onBack = { destination = null }`（单层返回，不是真栈）。 |
| `HomeRootContent` | 107-271 | `@Composable @OptIn(ExperimentalTime::class) private fun HomeRootContent(loggedIn: Boolean, displayName: String?, onNavigate: (String) -> Unit)` | 首页真身。**首行 `ReportMainTabRoot(isRoot = true)`（L114）** —— 声明自己是 Tab 根，让 Shell 底栏按 tab-root 模式加 padding。 |
| `MetricTabs` | 274 | `private val MetricTabs = listOf("今日", "昨日", "近30天")` | 门店指标卡三个 tab 标题，注释标为 Flutter `HomeController.metricTabs`。 |
| `flutterStyleGreeting` | 276-287 | `@OptIn(ExperimentalTime::class) private fun flutterStyleGreeting(displayName: String?): String` | 非 Composable。固定按 UTC+8 换算小时（L279，`(epochMillis/3_600_000 + 8) % 24`），<12「早上好」/<18「下午好」/否则「晚上好」；名字空则「访客」（L285）。 |

`HomeRootContent` 状态来源（全部本地 `remember`，L115-122）：`metricTab`（Int）、`showTodos`、`showSwitchStore`、`storeName`（初值 `HomeMockData.storeName`）、`showCheckIn`、`greeting = remember(displayName){…}`。**L122 单独一行读 `HomePointsStore.version`** —— 靠读这个 `mutableStateOf` 计数器触发 recomposition（`HomePointsStore` 是 object 单例，见 A 分册）。

副作用（`HomeScreen.kt`）：
- L124-127 `LaunchedEffect(Unit)`：延迟 50ms 后置 `showTodos = true`（Todo 卡片条延迟入场）。
- L128-144 `LaunchedEffect(loggedIn, HomePointsStore.checkedInToday, HomePointsStore.dialogAckDate)`：用 `Clock.System.now().toString().take(10)` 当「今天」（L131），调 `HomePointsStore.shouldShowDialog(...)`（L135-140）判定，命中再 `delay(400)` → `showCheckIn = true`（L142-143）。**无请求，纯本地判定**。
- L146-156 `showSwitchStore && loggedIn` 时弹 `CrossFeatureScreens.SwitchStoreDialog(selectedId = "1", …)`；`onPicked` 只改本地 `storeName` + Toast「已切换到 $name」（L150-154）——**未写 KV**，切店结果不持久。
- L158-177 `showCheckIn && loggedIn` 时弹 `DailyCheckInDialog(todayReward, streak, onCheckIn, onDismiss)`；`onCheckIn` 调 `HomePointsStore.checkIn()` + `markDialogAcked(today)` + Toast「签到成功，+N 积分」（L164-171）；`onDismiss` 只 `markDialogAcked`（L172-175）。

布局与 inset：L179-192 `Column(fillMaxSize + background(DemoColors.PageBg))` → `LazyColumn(modifier = fillMaxSize.statusBarsPadding(), contentPadding = PaddingValues(top=16, bottom=24))`。注释（L185-187）明确：安全区交给 `statusBarsPadding()`，16 交给 `contentPadding`，首个 section 自带 24 凑成 Flutter 的 `safeTop+40`。**底部不消费 `navigationBarsPadding`**（Shell 按 tab-root 处理，见合规节）。

LazyColumn 分区顺序（L193-267）：`HomeGreetingSection`(193) → `HomeSearchBar`(195-200，`onSearch={onNavigate(HomeRoutes.Search)}`、`onScan={onNavigate("扫一扫")}`) → `HomeBannerSection`(201) → `HomeFeatureGrid`(203-207，`onFeature={label->onNavigate(label)}`) → `HomeTodoCardStrip`(209-220，`onOpen` 用 `card.actionRoute.ifBlank { HomeRoutes.fromLabel(card.title) ?: card.title }`，L212-216) → `HomeStoreMetricsCard`(222-238，`metrics` 按 `metricTab` 三选一读 `HomeMockData.metricsToday/Yesterday/Month`，L226-230；`onStoreTap`：登录则开切店弹窗否则 `onNavigate("请先登录")` L233-236；`onOpenLedger={onNavigate(HomeRoutes.Ledger)}` L237) → `HomeHubEntryCard("投资策略")`(240-247，`onNavigate(HomeRoutes.Strategy)`) → `HomeServiceGrid`(248-257，**只有 label 为「更多」/「全部」才跳** `HomeRoutes.AllServices`，其余点击是 no-op，L252-254) → `HomeContactList`(258，`onContact = {}` 空实现) → `HomeNewsList`(259，`onOpenAll = {}` 空实现) → `HomeHubEntryCard("学习报告")`(261-267，`onNavigate(HomeRoutes.LearningReport)`)。L268 注释：Flutter HomePage 到学习报告结束，无「更多工具」行。

外部调用点：`composeApp/src/commonMain/kotlin/com.example.my_kmp_project/app/AppShell.kt:390`（`MainTab.Home -> HomeScreen(...)`，import L66）；Android native shell 走自己的 `JetpackHomeRoot`（`composeApp/src/androidMain/.../nativeshell/NativeJetpackHome.kt`，复用同一批 `HomeSections` 组件，import L24-36）。

#### HomeWebHandlers.kt

文件：`feature/home/src/commonMain/kotlin/com.example.my_kmp_project/feature/home/HomeWebHandlers.kt`（45 行）

`object HomeWebHandlers`（L11）——非 Composable，H5 桥注册器；`@Volatile private var registered = false`（L12-13）保证幂等；`var onRefreshDashboard: (() -> Unit)? = null`（L16）是可被 Home 替换的公开钩子，默认 null。

`fun register()`（L18-44）向 `core.platform.WebBridgeRegistry.register(WebBridgeMethods.*, handler)` 注册 **4 个方法**：

| 行号 | 方法 | handler 行为 | 返回 |
|------|------|--------------|------|
| 21-24 | `WebBridgeMethods.RefreshDashboard` | 忽略入参，`onRefreshDashboard?.invoke()`（未接线时是 no-op） | `{"ok":true}` |
| 25-30 | `WebBridgeMethods.ShowToast` | payload 非空白则 `core.platform.showPlatformToast(payload)` | `{"ok":true}` |
| 31-33 | `WebBridgeMethods.GetEnvironment` | **硬编码字符串** | `{"platform":"android","app":"my_kmp_project"}`（iOS/OHOS 上也返回 "android"，未做 expect/actual —— 已知不精确点） |
| 34-43 | `WebBridgeMethods.GetUserInfo` | 读 `core.account.AccountFacade.current()`；未登录 `{"loggedIn":false}`，登录则手工拼 `userId`/`displayName`（L39-41，字符串拼接，**未做 JSON 转义**） | `{"loggedIn":true,"userId":…,"displayName":…}` |

走的 core 能力：`core.platform.WebBridgeRegistry` + `WebBridgeMethods`（注册表）、`core.platform.showPlatformToast`（平台 Toast）、`core.account.AccountFacade`（会话）。**它不处理 URL**，只处理方法名；URL 侧由 `component.webview.OfflineWebFixtureUrl` + `AppRoute.InAppWeb` 决定（见 `HomeScreen.kt:49`）。

唯一调用点：`composeApp/src/androidMain/kotlin/com.example.my_kmp_project/nativeshell/NativeAndroidMain.kt:90`（`HomeWebHandlers.register()`，import L67）——**只有 Android native shell 注册**，与文件注释 L9 一致。commonMain/iOS/OHOS 调用：未找到。命令备查：

```
grep -rn "HomeWebHandlers" --include="*.kt" .
```

#### HomeServiceAssets.kt

文件：`feature/home/src/commonMain/kotlin/com.example.my_kmp_project/feature/home/HomeServiceAssets.kt`（125 行）

`object HomeServiceAssets`（L44）——「Flutter `module_home` all_services PNG → Compose drawable」映射器（注释 L43）。资源全部来自 `:core:design` 的生成包 `my_kmp_project.core.design.generated.resources.Res`（L10-39 import，45 条），**不是** 本模块自己的 composeResources，符合 AGENTS.md「`:core:design` 持有全部 drawable」。

| 函数 | 行号 | 签名 | 行为 |
|------|------|------|------|
| `fromFlutterFile` | 45-69 | `fun fromFlutterFile(assetName: String): DrawableResource?` | 先 `removeSuffix(".png")/.webp`（L46），再对 19 个 Flutter 资产名做 `when`（L48-66，`home_all_services_*`），**未命中返回 null**（L67）——调用方需自行降级。 |
| `featureForLabel` | 75-87 | `fun featureForLabel(label: String): DrawableResource` | 首页金刚区图标按**中文 label** 映射（L76-85，一组 label 共用一张图：如 `"H5 调试"`+`"销售顾问"`→`home_feature_sales`，`"新车跟进"`+`"数据分析"`→`home_feature_data`）；else 兜底 `home_all_services_all_functions`（L86）。返回非空。 |
| `featureAt` | 89-96 | `@Deprecated("Use featureForLabel", ReplaceWith("featureForLabel(label)")) fun featureAt(index: Int): DrawableResource` | 兼容旧索引式调用：内部固定 10 个 label 列表（L91-94）后 `featureForLabel(labels[index % labels.size])`（L95）。**注意 `index` 为负数时 `%` 在 Kotlin 里返回负值 → `labels[负]` 会 IndexOutOfBounds**，未见防护。 |
| `serviceCycle` | 98-107 | `private val` | 8 张 `home_all_services_*` 循环表。 |
| `serviceAt` | 109 | `fun serviceAt(index: Int): DrawableResource` | `serviceCycle[index % size]`，同样未防护负 index。 |

| Composable | 行号 | 签名 | 行为 |
|------------|------|------|------|
| `HomeAssetIcon` | 112-125 | `@Composable fun HomeAssetIcon(resource: DrawableResource, size: Dp = 44.dp, contentDescription: String? = null, contentScale: ContentScale = ContentScale.Crop)` | **无 `modifier` 参数**（Modifier 内部写死 `Modifier.size(size)`，L122）。就一个 `Image(painter = painterResource(resource), …)`（L119-124）；无状态、无副作用、不接 store。默认 `contentDescription = null`（对读屏不可见）。 |

#### DailyCheckInDialog.kt

文件：`feature/home/src/commonMain/kotlin/com.example.my_kmp_project/feature/home/DailyCheckInDialog.kt`（194 行）

| Composable | 行号 | 签名 | 真实行为 |
|------------|------|------|----------|
| `DailyCheckInDialog` | 42-194 | `@Composable fun DailyCheckInDialog(todayReward: Int = 10, streak: Int = 3, onCheckIn: () -> Unit, onDismiss: () -> Unit)` | **纯展示 + 两个回调，零业务**：不读 store、不发请求、不写 KV。无 `modifier` 参数。 |

内部状态与绘制细节：
- L49 `var busy by remember { mutableStateOf(false) }` —— 唯一状态，只用于防重复点击与转圈。
- L50-54 取色：`primaryBlue = DemoColors.Accent`、`coinGold = Color(0xFFF5A623)`、`heroBrush = Brush.linearGradient(0xFF1A8CFF → Accent → 0xFF0050C8)`（后两个是一次性 hex，**未走 `DemoColors` token**）。
- L56-59 `Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false))` —— 用原生 `Dialog` 而不是 `AlertDialog`，宽度自管。
- 结构：外层 `Column(fillMaxWidth.padding(horizontal = 36.dp))`（L60-65）→ 卡片 `Column`（L66-72，`shadow(24.dp, RoundedCornerShape(16.dp)) + clip + background(White)`）。
  - Hero（L74-106）：64dp 圆形半透明容器内 `Text("📅")`（L81-90，**用 emoji 代替图标资源**）；`+$todayReward` 40sp ExtraBold 金色（L92-99）；副文案「今日可领积分」（L101-105）。
  - 白身（L108-178）：标题「每日签到」（L114-119）→ 说明「签到攒积分，可在签到页兑换好物」（L121-127）→ `if (streak > 0)` 才显示「已连续签到 N 天」金色胶囊（L128-140）→ CTA。
- **CTA 行为（L142-168）**：44dp 高、22dp 圆角蓝底 `Box`，`clickable(enabled = !busy)` 里先 `busy = true` 再 `onCheckIn()`（L148-151）。**注意：`busy` 永远不会复位** —— 复位依赖父级把整个 Dialog 从组合中摘掉；调用方 `HomeScreen.kt:164-171` 确实在 `onCheckIn` 里置 `showCheckIn = false`，所以能卸载。若父级不卸载，按钮会永久转圈。busy 时渲染 `CircularProgressIndicator(18.dp, 白, 2.dp)`（L155-159），否则文案「立即签到 · +N积分」（L161-166）。
- 「稍后再说」（L170-177）：`clickable(enabled = !busy, onClick = onDismiss)`；卡片下方独立 36dp 半透明圆「✕」（L182-191）同样只在 `!busy` 时可点。系统返回/点遮罩走 `onDismissRequest = onDismiss`（L57）。

调用方：`HomeScreen.kt:161`（common 首页）、`composeApp/src/androidMain/kotlin/com.example.my_kmp_project/nativeshell/NativeJetpackHome.kt:24`（Android native 首页 import 后复用）。

#### LedgerScreens.kt

文件：`feature/home/src/commonMain/kotlin/com.example.my_kmp_project/feature/home/LedgerScreens.kt`（224 行）

| 符号 | 行号 | 签名 | 真实行为 / 状态 / 副作用 |
|------|------|------|--------------------------|
| `LedgerTransaction` | 34-41 | `data class LedgerTransaction(id: Int, type: String, category: String, amount: Double, date: String, note: String? = null)` | 纯数据，注释对应 Flutter `TransactionModel`。**没有 `isValid`**：`isValid: Boolean` 属于 Pay（见 PayRecordsScreen），本文件只有 `amount: Double`。 |
| `LedgerListScreen` | 47-79 | `@Composable fun LedgerListScreen(items: List<LedgerTransaction>, onBack: () -> Unit, onItem: (LedgerTransaction) -> Unit)` | **数据全靠参数注入**，内部零状态、零 store、零请求。L53 `ReportMainTabRoot(isRoot = false)`（二级页）。L59-63 用 `core.design.MineTopBar(title = "收支", onBack, containerColor = Color.White)` —— 合规的二级顶栏，非裸 TopAppBar。L64-67 `items.isEmpty()` 时居中「暂无收支记录」；否则 `LazyColumn(contentPadding = top12/bottom24)` + `items(items, key = { it.id })`（L69-76），行点击 → `onItem(item)`。根 `Column` 底色是硬编码 `Color(0xFFF5F6F8)`（L57），**未用 `DemoColors.PageBg`**。 |
| `LedgerDetailScreen` | 82-128 | `@Composable fun LedgerDetailScreen(item: LedgerTransaction?, onBack: () -> Unit)` | 可空入参：null → 居中「记录不存在」（L98-101）。非 null 渲染 类别 / `formatLedgerAmount` 大字 / 类型 / 日期 /（非空时）备注（L103-125）。L87 `ReportMainTabRoot(isRoot = false)`，L93-97 `MineTopBar(title = "收支详情")`。**无任何点击副作用**。 |
| `TransactionListItem` | 130-185 | `@Composable private fun TransactionListItem(item: LedgerTransaction, onClick: () -> Unit)` | 白卡行：`TypeTag` + 类别（单行省略）在上（L147-158），金额 22sp（L160-165），日期 + 备注灰字（L167-179，备注用 `weight(1f, fill = false)`），右侧「›」字符代替箭头图标（L183）。整卡 `clickable(onClick)`（L142）。 |
| `TypeTag` | 187-205 | `@Composable private fun TypeTag(type: String)` | 判定用**双向包含**：`"income" in lower \\|\\| "收入" in type` → 绿 `Triple("收入", 0xFF2E7D32, 0xFFE8F5E9)`；expense/支出 → 红；**else 把原始 type 当标签**显示蓝底（L195）。所以未知 type 也能渲染，不崩。 |
| `formatLedgerAmount` | 207-214 | `fun formatLedgerAmount(amount: Double): String`（**public，非 Composable**） | `abs(amount) >= 10_000` 时折万：`"¥ X.XX 万"`，否则 `"¥ X.XX"`（L209-213）。注意负数也按绝对值判阈值、符号保留在数值里。 |
| `formatTwoDecimals` | 216-224 | `private fun formatTwoDecimals(value: Double): String` | **KMP-safe 手写两位小数**（注释 L216 明确不用 JVM `String.format`）：`round(v*100).toLong()` 拆整/分再 `padStart(2,'0')`。 |

数据注入点：`HomeRouteHost.kt:157`（`HomeRoutes.Ledger -> LedgerListScreen(...)`）、`HomeRouteHost.kt:162`（`HomeRoutes.LedgerDetail -> LedgerDetailScreen(...)`）。

#### LearningReportScreen.kt

文件：`feature/home/src/commonMain/kotlin/com.example.my_kmp_project/feature/home/LearningReportScreen.kt`（345 行）

| 符号 | 行号 | 签名 | 真实行为 |
|------|------|------|----------|
| `ReportColors` | 39-59 | `private object ReportColors`（非 Composable） | Flutter `HomeReportColors` 的 19 个 hex 常量（纯黑系暗色板：Background/HighlightCard/RecordCard/Orange/DotYellow/DotBlue/ParentChip/PlayBg…）。**全部硬编码，不走 `DemoColors`** —— 本屏是暗色孤岛。注意 `PlayBg`（L58）定义了但文件内未被引用。 |
| `LearningReportScreen` | 61-154 | `@Composable fun LearningReportScreen(onBack: () -> Unit, onOpenMembership: (() -> Unit)? = null)` | L66 `ReportMainTabRoot(isRoot = false)`。顶栏用 **`FlutterFeatureTopBar`**（L75-80，定义在本分册的 `HomeRouteHost.kt:559`，传 `containerColor`/`titleColor` 适配暗色）。内容 `verticalScroll(rememberScrollState())`（L85）+ `offset(y = (-16).dp)`（L84）上提。**数据 100% 来自 `HomeMockData`**：`reportHighlights`（L98，定义在 `HomeMockData.kt:333`）、`reportRecords`（L120，`HomeMockData.kt:339`）——**没有 api/store 参与，不发请求**。 |
| 悬浮「家长助手」 | 127-142 | — | `Box` 叠加层 `align(Alignment.BottomEnd)` + `navigationBarsPadding()`（L130）+ `padding(end=16, bottom=88)`，点击 **只 `showPlatformToast("家长助手")`**（L135），不跳转。emoji `👨‍👩‍👧` 当图标（L139）。 |
| `MembershipBanner` 挂载 | 144-152 | — | `align(BottomCenter)` + `navigationBarsPadding()`（L147）；`onOpen = { onOpenMembership?.invoke() ?: showPlatformToast("立即开通") }`（L149-151）—— **注入的 lambda 为 null 时降级为 Toast**，这是本屏唯一可被外部接线的动作。 |
| `ReportSectionHeader` | 156-173 | `@Composable private fun ReportSectionHeader(dotColor: Color, title: String)` | 8dp 圆点 + 18sp 标题，无状态。 |
| `HighlightRow` | 175-243 | `@Composable private fun HighlightRow(item: ReportHighlight)` | 类型来自 `HomeMockData.kt:59`。尾部三态判定（L178-179）：`trailing` 全数字 → 橙色分数胶囊（L220-229）；`== "▶"` 或 `"play"`(忽略大小写) → 32dp 蓝色圆形播放（L231-239）；否则原样 24sp 文本（L240，通常是 emoji）。左侧 44dp 蓝渐变方块内直接 `Text(item.emoji)`（L186-198）。**整行不可点**。 |
| `RecordRow` | 245-294 | `@Composable private fun RecordRow(item: ReportRecord)` | `HomeMockData.kt:66` 的模型；右侧两列 `time` + `status`，`statusHighlight` 决定橙色 SemiBold（L288-289）。不可点。 |
| `MembershipBanner` | 296-344 | `@Composable private fun MembershipBanner(modifier: Modifier = Modifier, onOpen: () -> Unit = {})` | **本文件唯一带 `modifier` 参数的私有 Composable**（L298，默认 `Modifier`，调用方 `LearningReportScreen.kt:145` 传入 align/inset/padding 链）。横向渐变卡 + 👑 + 两行文案；「立即开通」文字块自带 `clickable(onClick = onOpen)`（L338-342）。 |

调用点：`HomeRouteHost.kt:124`（`HomeRoutes.LearningReport -> LearningReportScreen(...)`）；路由常量 `core/router/src/commonMain/kotlin/com.example.my_kmp_project/core/router/FeatureRoutes.kt:20`（`"/home/learning_report"`）。

#### StrategyScreen.kt

文件：`feature/home/src/commonMain/kotlin/com.example.my_kmp_project/feature/home/StrategyScreen.kt`（392 行）

| 符号 | 行号 | 签名 | 真实行为 / 状态 / 副作用 |
|------|------|------|--------------------------|
| 色常量 | 44-47 | `private val GainRed = Color(0xFFFF3B30)` / `GainGreen` / `GaugeGreen` / `GaugeRed`（0xFF34C759） | 硬编码 hex。**注意语义是「涨=红」**（A 股习惯）：`AssetCell` L202/L212 里 `positive` 用 `GainRed`。 |
| `StrategyScreen` | 49-95 | `@Composable fun StrategyScreen(onBack: () -> Unit)` | 无 modifier、无数据参数。L51 `ReportMainTabRoot(isRoot = false)`。状态：`selectedTab`（L52，初值 0）、`periodIndex`（L53，**初值 4** = `strategyPeriods` 最后一项「近1年」）。顶栏 `FlutterFeatureTopBar(title = "策略", onBack, containerColor = DemoColors.Background)`（L60-65）后**手工补 0.5dp 下划线** `Box`（L67-72，对应 Flutter AppNavBar hairline）。内容 `verticalScroll(rememberScrollState())` + `padding(horizontal=16)` + `padding(top=8, bottom=24)`（L74-79，注释对齐 Flutter `fromLTRB(16,8,16,24)`）。**整屏零网络请求**。 |
| `StrategySubTabs` | 97-133 | `@Composable private fun StrategySubTabs(tabs: List<String>, selectedIndex: Int, onSelected: (Int) -> Unit)` | 居中 Row，每项 `clickable { onSelected(index) }`（L113），选中态：文字 SemiBold + 24dp 蓝色下划线条（未选中 `width(0.dp)`，L125）。tab 文案来自 `HomeMockData.strategyTabs`（L82；定义 `HomeMockData.kt:347` = 推荐/逆向/趋势）。**切换 tab 不改变下方内容** —— `selectedTab` 只喂给这里，`AssetGridCard`/`StrategyPlanCard` 都不读它（占位式实现）。 |
| `AssetGridCard` | 135-190 | `@Composable private fun AssetGridCard(periodIndex: Int, onPeriodSelected: (Int) -> Unit)` | 固定说明文案（L148-154）+ `Spacer(54.dp)` 硬凑 Flutter 行高差（L156 注释）。九宫格：`HomeMockData.strategyAssets.chunked(3)`（L157，定义 `HomeMockData.kt:349`），每行不足 3 个用 `Spacer(weight(1f))` 补位（L166）。底部周期条 `horizontalScroll`（L172-188）遍历 `HomeMockData.strategyPeriods`（`HomeMockData.kt:348`），点击 `onPeriodSelected(index)`。**选中周期同样不影响任何数值** —— 纯 UI 态。 |
| `AssetCell` | 192-217 | `@Composable private fun AssetCell(cell: StrategyAssetCell, modifier: Modifier = Modifier)` | `aspectRatio(1.35f)`（L199）小方块：label 12sp + value 14sp Bold，红/绿 8% 透明底（L202）。`StrategyAssetCell` 定义在 `HomeMockData.kt:75`。 |
| `StrategyPlanCard` | 219-315 | `@Composable private fun StrategyPlanCard()` | **全硬编码**：标题「黄金恐贪定投 · 第一期」（L232）、「逆向」chip（L239）、「如何跟投」文字（L249-254，**不可点**）、收益率 `"-11.35%"` 字面量（L264）、`FearGreedGauge(score = 63, label = "中立")`（L272）、`InvestProgressBar(current = 36, total = 50)`（L277）、「本周已投 1 份」（L285）。**「订阅」按钮 `clickable { }` 是空 lambda**（L295）—— 不请求、不跳路由、不 Toast。 |
| `InvestProgressBar` | 317-343 | `@Composable private fun InvestProgressBar(current: Int, total: Int)` | `frac = current/total` 再 `coerceIn(0f,1f)`（L320，**total=0 时是 NaN→coerceIn 仍可能异常，未见零保护**）；`fillMaxWidth(frac)` 蓝条 + 居中「current / total」文本。 |
| `FearGreedGauge` | 345-392 | `@Composable private fun FearGreedGauge(score: Int, label: String)` | 无 `modifier` 参数。`score.coerceIn(0,100)`（L354）；`Canvas(88×44dp)`（L361-365）用 `drawArc` 画 3 段各 -60°（`sweepPer = -180f/3`，L372-383），绿→`Color(0xFFFFCC00)`→红、stroke 8dp Round cap、半径 36dp；**没有指针**（与注释 L345-348 一致），下方文本「63 中立」。 |

调用点：`HomeRouteHost.kt:123`（`HomeRoutes.Strategy -> StrategyScreen(onBack = onBack)`）；常量 `core/router/.../FeatureRoutes.kt:19`（`"/home/strategy"`）。

#### AllServicesScreen.kt

文件：`feature/home/src/commonMain/kotlin/com.example.my_kmp_project/feature/home/AllServicesScreen.kt`（325 行）

| 符号 | 行号 | 签名 | 真实行为 / 状态 / 副作用 |
|------|------|------|--------------------------|
| 常量 | 49-52 | `private const val ServiceCellAspectRatio = 0.72f`、`private val FlutterToolbarHeight = 56.dp` | 复刻 Flutter SliverGrid childAspectRatio 与 `kToolbarHeight`（注释 L48/L51）——**明确不用 M3 的 64dp CenterAlignedTopAppBar**。 |
| `AllServicesScreen` | 54-113 | `@Composable fun AllServicesScreen(onBack: () -> Unit, onOpen: (String) -> Unit = {})` | L59 `ReportMainTabRoot(isRoot = false)`。状态：`isEditing`（L60）、`favoriteIds by remember { mutableStateOf(HomeFavoritesStore.snapshot()) }`（L61）、`favoriteItems = remember(favoriteIds) { HomeFavoritesStore.favoriteItems() }`（L62，**依赖 favoriteIds 重算，不是 Flow**）。结构：`AllServicesTopBar`（L70）+ `LazyColumn(contentPadding top8/bottom24)`（L71-77）。首个 item 是「常用服务 / 将按自定义顺序出现在首页」伪分区（L78-96，`showEditButton = true`、`items = favoriteItems`）；其余遍历 `HomeMockData.catalogSections`（L97，定义 `HomeMockData.kt:177`；模型 `AllServiceSection` 在 `HomeMockData.kt:31`、`AllServiceItem` 在 `:25`）。**副作用（唯一写持久化的地方）**：编辑态点角标 → `HomeFavoritesStore.remove(item.id)` / `.add(item.id)` 成功后 `favoriteIds = HomeFavoritesStore.snapshot()` 重新快照（L89-93 / L103-107）；store 内部 `persist()` 经 `core.platform.saveStringList` 写 KV（`HomeFavoritesStore.kt:40-42`，key `home_favorite_service_ids_v2`，下限 3 上限 8，`:11-13`/`:46-48`）。非编辑态点格子 → `onOpen(it.label)`（L94 / L108），由调用方决定跳转；本文件**不直接 navigate**。 |
| `AllServicesTopBar` | 115-151 | `@Composable private fun AllServicesTopBar(onBack: () -> Unit)` | **本模块唯一自绘顶栏**（不用 `ImmersiveTopAppBar`/`MineTopBar`）：`Column.fillMaxWidth.background(DemoColors.Background).statusBarsPadding()`（L118-122）+ 56dp Row（L124-128）；返回键 `IconButton(onClick = onBack)` 内 `Image(painterResource(Res.drawable.ic_nav_back))` **`rotate(180f)` 复用成左箭头**（L130-139，图标本体是右向箭头）；标题「全部服务」`weight(1f)` 居中（L140-147）+ 尾部 `Spacer(48.dp)` 配平（L148）。 |
| `AllServicesSectionBlock` | 153-241 | `@Composable private fun AllServicesSectionBlock(section: AllServiceSection, isEditing: Boolean, isFavoriteSection: Boolean, favoriteIds: Set<String> = emptySet(), onEditTap: (() -> Unit)? = null, onItemBadgeTap: (AllServiceItem) -> Unit = {}, onItemTap: (AllServiceItem) -> Unit = {})` | 标题 + 可选副标题（`section.subtitle != null` 才占 `weight(1f)`，L179-191）；`section.showEditButton && onEditTap != null` 才渲染「编辑/完成」描边按钮（L192-202）。**布局不用 `LazyGrid`**：`BoxWithConstraints` 里算 `cellHeight = maxWidth / 5f / 0.72f`（L206-207），`chunked(5)` 手工成行（L208），行内 `weight(1f).fillMaxHeight()`（L225-227），空位 `repeat(5 - rowItems.size)` 补 `Spacer`（L230-232），行距 12dp（L234-236）。角标语义：`showMinus = isFavoriteSection && isEditing`、`showPlus = !isFavoriteSection && isEditing && item.id !in favoriteIds`（L221-222）。 |
| `ServiceGridCell` | 243-325 | `@Composable private fun ServiceGridCell(item: AllServiceItem, isEditing: Boolean, showMinus: Boolean, showPlus: Boolean, onBadgeTap: () -> Unit, onTap: () -> Unit = {}, cellModifier: Modifier = Modifier)` | 格子本体：48dp 容器（L261-264）里先 `HomeServiceAssets.fromFlutterFile(item.assetName)`（L265），命中则 `HomeAssetIcon(size = 48.dp, contentScale = ContentScale.Fit)`（L267-272）；**未命中降级**为 12dp 圆角浅蓝块 + `item.label.take(1)` 首字（L273-288）。编辑态角标：16dp 圆、`TopEnd` 对齐、「×」/「+」，`clickable(onClick = onBadgeTap)`（L289-311）。整格 `clickable(enabled = !isEditing, onClick = onTap)`（L255）。标签 11sp、`lineHeight = (11*1.2f).sp`、单行省略（L314-323）。 |

调用点（3 处）：`HomeScreen.kt:73`（本地 `destination == "services"` 分支，`onOpen` 里先清栈再 `go(label)`）、`composeApp/src/commonMain/kotlin/com.example.my_kmp_project/app/AppShell.kt:426`（`AppRoute.AllServices`）、`composeApp/src/androidMain/kotlin/com.example.my_kmp_project/nativeshell/NativeAndroidMain.kt:553`。

#### HomeDetailScreens.kt

文件：`feature/home/src/commonMain/kotlin/com.example.my_kmp_project/feature/home/HomeDetailScreens.kt`（393 行）—— 三张「Flutter 详情页」的 KMP 复刻 + 4 个私有子件。三屏都 `ReportMainTabRoot(isRoot = false)`（L57 / L130 / L210），都用 `core.design.MineTopBar`（合规顶栏），都零网络零 store。

| 符号 | 行号 | 签名 | 真实行为 |
|------|------|------|----------|
| `AfterSalesDetailRow` | 38-49 | `data class AfterSalesDetailRow(id, title, kindLabel, customerName, customerPhone, plateNo, mileageKm: Int?, serviceDate, content, appointmentId: Int?)` | 两个可空字段 `mileageKm`/`appointmentId` 决定详情页是否显示对应行（L96 / L104）。 |
| `AfterSalesServiceDetailScreen` | 52-122 | `@Composable fun AfterSalesServiceDetailScreen(row: AfterSalesDetailRow, onBack: () -> Unit)` | 顶栏「服务详情」（L64）。`kindColor`：`kindLabel.contains("修")` → 橙 `0xFFE67E22`，否则绿 `0xFF2E7D32`（L58）。hero 卡 = kind 胶囊 + 20sp 标题（L71-89）；`DetailSection("客户车辆")` 用 `buildList` 条件装配（L90-98）；`DetailSection("服务信息")` 同理（L99-106）；`content` 非空白才渲染「备注」卡（L107-119）。**无点击副作用**。 |
| `AnalyticsDetailScreen` | 124-194 | `@Composable fun AnalyticsDetailScreen(record: AnalyticsRecordRow, onBack: () -> Unit)` | 入参模型 `AnalyticsRecordRow` 定义在 `HomeRouteHost.kt:239`。顶栏「数据详情」（L136）。**提示条二选一**：`record.anomaly` → 红色 `CueBanner("异常记录")`，否则 `record.featured` → 蓝色 `CueBanner("精选记录")`（L143-155）。hero 标题+副标题（L156-166）；`DetailSection("流量漏斗")` 列 PV/点击/转化（L167-174）；「漏斗占比」用 `record.pv.coerceAtLeast(1)` 当分母画 3 条 `MetricBar`（L185-190，**注释 L124 明确「no chart lib」**）。 |
| `NewCarFollowArchiveDetailScreen` | 196-331 | `@Composable fun NewCarFollowArchiveDetailScreen(row: NewCarFollowRow, onBack: () -> Unit)` | 入参模型在 `HomeRouteHost.kt:227`。本地状态 3 个：`level = remember { mutableStateOf(row.intentBand) }`（L202，初值取数据行）、`logBody`（L203）、`logs = remember { listOf(两条写死的跟进文案) }`（L204-209）。顶栏「跟进档案详情」（L216）→ hero 客户名 + `车 · 阶段`（L230-232）→ `DetailSection("档案信息")` 六行（L234-244）→「调整级别」高/中/低胶囊，点击只改本地 `level`（L254-276，**没有 onLevelChange 回调、不回落库**）→「写一条跟进」`OutlinedTextField(minLines = 3)`（L287-293）+「提交跟进」按钮：`logBody.isBlank()` → Toast「请填写跟进内容」，否则 Toast「已记录跟进」并把 `logBody` 清空（L303-309）。**注意：提交不会把内容写进 `logs` 列表** —— 新文案不会出现在下方「跟进记录」，是纯 Toast 占位。L312 用全限定名 `androidx.compose.ui.text.style.TextAlign.Center`（未 import，写法合法但风格不一致）。 |
| `DetailSection` | 333-356 | `@Composable private fun DetailSection(title: String, rows: List<Pair<String, String>>)` | 白卡 key-value 两列 `SpaceBetween`，无 modifier 参数。 |
| `CueBanner` | 358-372 | `@Composable private fun CueBanner(color: Color, title: String, subtitle: String)` | 由传入色派生 10% 底 + 35% 描边（L364-365）。 |
| `MetricBar` | 374-393 | `@Composable private fun MetricBar(label: String, fraction: Float, color: Color)` | 40dp 标签 + 10dp 高轨道；`fraction.coerceIn(0.02f, 1f)`（L387）保证零值也露一点色。 |

调用点：`HomeRouteHost.kt:170`（`HomeRoutes.DataAnalyticsDetail`）、`:187`（`HomeRoutes.AfterSalesDetail`）、`:201`（`HomeRoutes.NewCarFollowDetail`）。路由常量见 `core/router/.../FeatureRoutes.kt:33 / :40 / :43`。

#### HomeSearchScreen.kt

文件：`feature/home/src/commonMain/kotlin/com.example.my_kmp_project/feature/home/HomeSearchScreen.kt`（578 行）—— `/home/search`，Flutter `SearchPage` 复刻。

| 符号 | 行号 | 签名 | 真实行为 / 状态 / 副作用 |
|------|------|------|--------------------------|
| `SearchFieldHeight` | 71 | `private val SearchFieldHeight = 44.dp` | 对应 Flutter `SearchPageTheme.searchFieldHeight`（注释 L70）。 |
| `searchRankCoverRes` | 73-82 | `private fun searchRankCoverRes(rank: Int): DrawableResource` | 把 rank 1-7 映射到 `home_search_rank_1..7`，else 用 `_8`。**全文件唯一声明处、无任何调用点 → 死代码**（8 张榜位图 import L58-65 实际未被渲染；`RankListRow` 改用 Canvas 播放图标）。grep：`grep -rn "searchRankCoverRes" --include="*.kt" feature/home` → 仅命中 L73。 |
| `HomeSearchScreen` | 84-165 | `@OptIn(ExperimentalLayoutApi::class) @Composable fun HomeSearchScreen(onBack: () -> Unit)` | L87 `ReportMainTabRoot(isRoot = false)`。5 个本地状态：`query`（L88）、`history = remember { mutableStateOf(HomeMockData.searchHistory) }`（L91，初值 `HomeMockData.kt:223`）、`discovery`（L92，`HomeMockData.kt:231`）、`selectedRankTab`（L93，初值 0）、`rankItems = remember(selectedRankTab) { HomeMockData.rankItemsForTab(selectedRankTab) }`（L94，`HomeMockData.kt:292`）。`rotatingHint`（L90）= `searchHistory.getOrElse(2)`，即 placeholder 轮换是**取固定下标**、不会随时间轮播。根 `Column.background(DemoColors.PageBg)` → `SearchHeaderBar` → `verticalScroll` 内容列（L112-117，`padding(bottom = 24.dp)`）。**没有真正的搜索结果页**：`onSearch` 只做历史去重前插（L107-110：`listOf(text) + history.filterNot { it == text }.take(9)`，**上限 9 条**），不请求、不导航。`onClear` 只清内存态（L121 `history = emptyList()`），**不写 KV**（对比 `AllServicesScreen` 会持久化）。点历史/发现/筛选标签一律 `query = it`（L122 / L131 / L136）。`onRefresh` = `discovery.reversed()`（L130，「换一换」只是原地反转列表）。 |
| `SearchHeaderBar` | 167-271 | `@Composable private fun SearchHeaderBar(query: String, rotatingHint: String, onQueryChange: (String) -> Unit, onBack: () -> Unit, onCancel: () -> Unit, onSearch: () -> Unit)` | **自绘搜索头，不用任何 TopAppBar**：`Row.fillMaxWidth.background(PageBg).statusBarsPadding()`（L177-182，SafeArea 后再 `padding(start=8, end=16, top=8, bottom=12)`，注释对齐 Flutter `fromLTRB(8,8,16,12)`）。返回键 44dp 命中区 + `ic_nav_back` `rotate(180f)` + `ColorFilter.tint(Accent)`（L185-201）。输入框是 **`BasicTextField`**（L220-238，非 M3 `TextField`）：`singleLine`、`cursorBrush = SolidColor(DemoColors.Accent)`、`decorationBox` 里 `query.isEmpty()` 时叠 `rotatingHint`（L227-237）。放大镜用 **文本字形「⌕」**且可点触发 `onSearch`（L213-218）。空文本时右侧显示麦克风资源图（L239-249），有文本时换成「✕」清空 `onQueryChange("")`（L250-258）。尾部「取消」`clickable(onClick = onCancel)`，父层把 `onCancel` 与 `onBack` 绑成同一个 `onBack`（L105-106）。 |
| `SearchHistorySection` | 273-324 | `@Composable private fun SearchHistorySection(history: List<String>, onClear: () -> Unit, onTagTap: (String) -> Unit)` | 头行 `heightIn(min = 44.dp)`，「清除」= 图标 `home_search_icon_clear_history` + 文本，整行 `clickable(onClear)`（L299-311）。标签用 **`FlowRow`**（L314，需 `@OptIn(ExperimentalLayoutApi::class)`，L273）8dp 间距。 |
| `SearchDiscoverySection` | 326-385 | `@Composable private fun SearchDiscoverySection(discovery: List<String>, onRefresh: () -> Unit, onTagTap: (String) -> Unit)` | 白卡 + 描边（L336-340）；「换一换」有**两个可点区**：18dp `home_search_icon_refresh` 图（L355-362）与右侧文本（L364-369），都绑 `onRefresh`。`index == 0` 的标签 `highlight = true`（L379）——只有第一项高亮。 |
| `SearchFilterSection` | 387-419 | `@Composable private fun SearchFilterSection(tags: List<String>, onTagTap: (String) -> Unit)` | 「快捷筛选」+ 自绘 `SearchSliderIcon`（L406，注释说明刻意不用漏斗位图），tags 来自 `HomeMockData.filterTags`（`HomeScreen` 侧传 `HomeMockData.filterTags`，见 L135）。 |
| `SearchSliderIcon` | 421-437 | `@Composable private fun SearchSliderIcon(modifier: Modifier = Modifier, color: Color)` | **有 `modifier` 参数**（本分册少见）。`Canvas(fillMaxSize)` 画 3 条横线（y = 22%/50%/78%）+ 3 个 2.2dp 圆旋钮（x = 35%/65%/45%），1.5dp Round cap（L425-435）。 |
| `SearchTagChip` | 439-466 | `@Composable private fun SearchTagChip(label: String, onTap: () -> Unit, highlight: Boolean = false, softFill: Boolean = false)` | 20dp 胶囊文本；底色三态 `highlight`→Accent 10%、`softFill`→`DemoColors.Background`、else `PageBg`（L448-452）。**`softFill` 三个调用点都未传**（默认 false），该分支是死代码。 |
| `RankTabBar` | 468-504 | `@Composable private fun RankTabBar(tabs: List<String>, selectedIndex: Int, onSelected: (Int) -> Unit)` | `horizontalScroll` 行 + 20dp 间距；选中态 Accent SemiBold + 20×2dp 下划线（未选中 `width(0.dp)`）。tabs 来自 `HomeMockData.rankTabs`（`HomeMockData.kt:249` = 热配榜/诵读榜/剧集榜/记录榜）。 |
| `RankListRow` | 506-578 | `@Composable private fun RankListRow(item: SearchRankItem)` | 模型 `HomeMockData.kt:38`。名次色：1=金 `0xFFF5A623`、2=`DemoColors.Muted`、3=铜 `0xFFC47B2C`、else TextSecondary（L517-522），固定宽 24dp。封面 72dp 圆角块里**不加载任何图片**，而是 Canvas 手绘「圆圈 + 三角」play 图标（L529-550，注释 L536 说明是因为 Flutter SoT 上 picsum 图加载失败，直接固化成占位视觉）。标题 1 行 + 副标题 2 行（`lineHeight = 19.6.sp`，L568）。尾部「›」字符。**整行不可点**（无 clickable）。 |

调用点：`HomeRouteHost.kt:122`（`HomeRoutes.Search -> HomeSearchScreen(onBack = onBack)`）；常量 `core/router/.../FeatureRoutes.kt:18`。

#### HomeSections.kt

文件：`feature/home/src/commonMain/kotlin/com.example.my_kmp_project/feature/home/HomeSections.kt`（989 行）—— 首页 11 个区块的**唯一实现**。文件头注释（L47-58）说明存在理由：改前 `HomeScreen.kt`（共享）与 `composeApp/androidMain/.../NativeJetpackHome*.kt` 各写一份、11 区块逐行重复必然漂移；现在**两侧只保留宿主**（滚动容器 / 下拉刷新 / 签到弹窗），区块共用本文件。外边距严格照抄 Flutter 各 section 的 `EdgeInsets`，**首块之前的 16 由宿主提供**（L56-57）。

| 符号 | 行号 | 签名 | 真实行为 |
|------|------|------|----------|
| `HomeSectionStyle` | 59-106 | `object HomeSectionStyle`（非 Composable，除 3 个 `@Composable get`） | 设计 token 层：`Accent/Surface/FillSecondary/LabelPrimary/LabelSecondary/LabelTertiary/Separator` 全部**转发 `DemoColors`**（L60-66，`FillSecondary` = `DemoColors.PageBg`）；`RadiusMd = 8.dp`（L69）；`GroupedCardBorder = 1.0f`、`ThinCardBorder = 0.5f`（L72/L75，两种卡片描边宽度）；3 个 `@Composable get` 文字样式 `LargeTitle28` / `SectionTitle18` / `SectionLabel14`（L77-105）统一 `fontFamily = DemoFonts.Geist` + Flutter 的 `lineHeight` 与负 `letterSpacing`（-1.6 / -0.54 / -0.28 sp）。 |
| `HomeGreetingSection` | 110-145 | `@Composable fun HomeGreetingSection(greeting: String, modifier: Modifier = Modifier)` | 区块 1「问候 + 消息胶囊」。`padding(start=16, end=16, top=24)`（L115，即注释所说首块自带的 24）。左 `Text(greeting, style = LargeTitle28)` `weight(1f)`；右侧胶囊是**写死的「3条新消息」**（L138）+ `HomeIcons.NotificationsNone` tint Accent（L130-135）。胶囊**不可点**（无 clickable）→ 消息数与真实未读无关，纯视觉占位。 |
| `HomeSearchBar` | 149-214 | `@Composable fun HomeSearchBar(placeholder: String, onSearch: () -> Unit, onScan: () -> Unit, modifier: Modifier = Modifier)` | 区块 2。**不是真输入框**：44dp 高白底 0.5dp 描边 `Row` 整体 `clickable(onSearch)`（L173），内部只有 `HomeIcons.Search` 图标 + 灰色 `placeholder`（L177-190）——点搜索栏即跳搜索页。右侧独立 44×44 方块 `clickable(onScan)` + `HomeIcons.QrScan`（L193-212）。lambda 语义：`onSearch`/`onScan` 都由宿主注入导航动作（`HomeScreen.kt:197-198`）。 |
| `HomeBannerSection` | 218-284 | `@Composable fun HomeBannerSection(modifier: Modifier = Modifier)` | 区块 3，**代码合成而非整张位图**：132dp 高圆角 Box 叠 4 层 —— `Image(painterResource(Res.drawable.home_banner_bg), Crop)`（L228-233，注释 L227：Flutter 原是 picsum 网络图，已本地化为 `:core:design` drawable）→ 黑色 25% 暗化层（L235-239）→ Accent@55%→透明横向渐变层（L241-249）→ 文案列（L250-282「朋友圈营销」/「一键分享，高效触达客户」/白底胶囊「立即体验」）。**整个 Banner 无任何 clickable**，CTA 是装饰。 |
| `visibleHomeFeatures` | 288-295 | `fun visibleHomeFeatures(items: List<HomeFeatureItem>): List<HomeFeatureItem>`（**public，非 Composable**） | 裁剪规则：`maxItems = 9`；把 `label == "更多"` 的项挑出，非「更多」项在存在「更多」时只取 8 个、否则 9 个，最后把**最后一个**「更多」追加到末尾（L291-294）。保证「更多」恒在末位。 |
| `HomeFeatureGrid` | 297-357 | `@Composable fun HomeFeatureGrid(items: List<HomeFeatureItem>, onFeature: (String) -> Unit, modifier: Modifier = Modifier)` | 区块 4 金刚区。`val visible = remember(items) { visibleHomeFeatures(items) }`（L303，依赖 items 重算）。卡片描边用 `HomeSectionStyle.GroupedCardBorder.dp`（L311）。`visible.chunked(5)` 每行 5 列、行间 8dp（L317-318），不足补 `Spacer(weight(1f))`（L353）。每格 44dp 圆角方块底 `FillSecondary`，内 `HomeAssetIcon(resource = HomeServiceAssets.featureForLabel(item.label), size = 44.dp)`（L334-338，图标按中文 label 映射，未知 label 落 all_functions 兜底）+ 11sp 单行标签（L341-350）。点击 → `onFeature(item.label)`（L324）——**参数是 label 字符串而非路由**，由宿主 `HomeScreen.kt:205` 转交 `go(label)`。 |

| `HomeStoreMetricsCard` | 361-443 | `@Composable fun HomeStoreMetricsCard(storeName: String, selectedTab: Int, tabs: List<String>, metrics: List<HomeMetric>, details: List<HomeMetricDetail>, onTabSelected: (Int) -> Unit, onStoreTap: () -> Unit, onOpenLedger: () -> Unit, modifier: Modifier = Modifier)` | 区块 5「公司数据」。**受控组件**：`selectedTab` 由宿主持有（`HomeScreen.kt:115/232`），本卡不存状态。头部标题 + 右侧「查看更多」+ `HomeIcons.ChevronRight` 整行 `clickable(onOpenLedger)`（L378-398）。卡内顺序：`StorePickerRow`（L412）→ `MetricTabBar`（L414）→ `metrics.chunked(2)` 两列指标，单数时补 `Spacer(weight(1f))`（L416-427）→ `details.isNotEmpty()` 才画 0.5dp 分隔线 + 一排 `DetailActionTile`（L428-440）。指标/明细数据由宿主注入 `HomeMockData.metricsToday/Yesterday/Month` 与 `HomeMockData.metricDetails`（`HomeScreen.kt:226-231`）。 |
| `StorePickerRow` | 445-495 | `@Composable private fun StorePickerRow(storeName: String, onTap: () -> Unit)` | 浅底圆角行 `clickable(onTap)`（L452）：28dp 图标盒用 `HomeStrokeIcons.Storefront`（L463-468）→ 门店名（单行省略）→ `HomeStrokeIcons.ArrowDown` + `HomeStrokeIcons.SwapHoriz`（contentDescription「切换门店」，L481-493）。`onTap` 语义 = 打开切店弹窗（`HomeScreen.kt:233-236` 判登录）。 |
| `MetricTabBar` | 497-545 | `@Composable private fun MetricTabBar(tabs: List<String>, selectedTab: Int, onTabSelected: (Int) -> Unit)` | 分段控件：外层 `FillSecondary` + 3dp 内衬，每项 `weight(1f)`；选中态 `Surface` 底 + `.then(if (active) Modifier.border(0.5dp…) else Modifier)`（L517-527）——**用 `.then()` 做条件 Modifier**，无 modifier 参数（父层已传 weight）。 |
| `MetricTile` | 547-574 | `@Composable private fun MetricTile(metric: HomeMetric, modifier: Modifier = Modifier)` | 值 22sp Bold（`lineHeight = 24.2.sp` 即 height 1.1，`letterSpacing -0.6sp`）+ 标签 12sp 单行省略。**底色 `DemoColors.CanvasSoft`（L552）而非 `HomeSectionStyle.FillSecondary`** —— 本文件唯一直接用 `DemoColors` 的底色，与同卡其它处存在 token 不一致。不可点。 |
| `DetailActionTile` | 576-609 | `@Composable private fun DetailActionTile(detail: HomeMetricDetail, modifier: Modifier = Modifier)` | 三段文本：Accent 18sp 值 + 标签 + **「详情」二字（L602-607）**。底 `DemoColors.LinkBgSoft.copy(alpha = 0.35f)`（L581）。**「详情」没有任何 `clickable`** —— 看起来能点其实不可点，明细详情在本模块无落地页。 |
| `HomeHubEntryCard` | 613-… | `@Composable fun HomeHubEntryCard(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier)` | 区块 6 入口卡：整卡 `clickable(onClick)`（L632），44dp 图标盒（Accent 10% 底 + `Icon(icon, 24dp)`，L636-649）+ 标题 17sp / 副标题 13sp（L651-659）+ 尾部箭头图标。`icon` 是 **`ImageVector`** 参数，宿主传 `HomeIcons.GridView` / `HomeIcons.Analytics`（`HomeScreen.kt:244/264`）。 |

| `HomeHubEntryCard`（续） | 660-666 | — | 尾部箭头 `HomeIcons.ChevronRight` tint `LabelTertiary` 24dp。 |
| `HomeServiceGrid` | 671-741 | `@Composable fun HomeServiceGrid(items: List<HomeServiceItem>, onService: (String) -> Unit, onOpenAll: () -> Unit, modifier: Modifier = Modifier)` | 区块 7「服务推荐」。头部标题 + 「全部」+ 箭头整行 `clickable(onOpenAll)`（L694-710）。卡内 `items.chunked(4)` 4 列（L724），`val index = rowIndex * 4 + colIndex`（L728）用来取循环主色，不足 4 个补 `Spacer(weight(1f))`（L736）。每格 `onClick = { onService(item.label) }`（L732）。**注意本卡的两个出口都是 label/回调**：宿主 `HomeScreen.kt:252-255` 只在 label 为「更多」「全部」时放行跳转，因此宫格内其它服务项点击在当前装配下是 no-op。 |
| `serviceAccent` | 743-753 | `private fun serviceAccent(index: Int): Color` | 8 色循环（注释 L743 直接抄 Flutter `accents`）：`Accent / Violet / Cyan / Warning / HighlightPink / LinkDeep / Violet / TextPrimary`，**index 6 与 0 颜色重复、末位用文字色当装饰色**（原样照抄 Flutter 的列表）。全部走 `DemoColors` token，无硬编码 hex。 |
| `ServiceGridItem` | 755-812 | `@Composable private fun ServiceGridItem(item: HomeServiceItem, accent: Color, onClick: () -> Unit, modifier: Modifier = Modifier)` | 44dp 圆角盒（`accent.copy(alpha = 0.12f)` 底）内 `Icon(HomeStrokeIcons.forServiceLabel(item.label), tint = accent, 22dp)`（L766-780）——**图标按 label 动态选**，映射函数在 `HomeStrokeIcons.kt`。角标：`item.badge?.let { … }`（L781-798），白字 9sp、`TopEnd` 对齐 + `offset(x = 6.dp, y = (-4).dp)` 复刻 Flutter `Positioned(top:-4,right:-6)` 越界显示（注释 L790），**「热门」用 `DemoColors.Warning` 其它用 `Accent`**（L794）。整格 `clickable(onClick)`（L763）。 |
| `HomeContactList` | 816-899 | `@Composable fun HomeContactList(items: List<HomeContactItem>, onContact: (HomeContactItem) -> Unit, modifier: Modifier = Modifier)` | 区块 8「联系汽车之家」，标题文案写死（L827）。列表卡描边用 `ThinCardBorder`（0.5dp，L835）。每行 `heightIn(min = 72.dp)`（L845，注释：Flutter ListTile 两行最小高 72）+ `clickable { onContact(item) }`（L846）；头像 44dp 圆 + `HomeIcons.PersonOutline`（L850-863）；**尾部图标由 `item.trailing` 字符串驱动**：`"chat"`→`HomeIcons.ChatBubble`、`"phone"`→`HomeIcons.Phone`、else→`ChevronRight`（L874-879），前两者 tint Accent 否则灰（L881-885）。行间 `HorizontalDivider(0.5dp, padding(start = 68.dp, end = 16.dp))` 缩进对齐头像右侧（L889-895）。宿主 `HomeScreen.kt:258` 传的是 `onContact = {}` → **整块当前不可用**。 |
| `HomeNewsList` | 903-989 | `@Composable fun HomeNewsList(items: List<HomeNewsItem>, onOpenAll: () -> Unit, modifier: Modifier = Modifier)` | 区块 9「行业动态」。头「查看更多」`clickable(onOpenAll)`（L920-936）；宿主 `HomeScreen.kt:259` 传 `onOpenAll = {}` → 也不可点。每条新闻卡是**纯展示**：标题 15sp 最多 2 行（`lineHeight = 21.sp` height 1.4，L955-963）+ `"${news.source}  ${news.date}"`（L966，两个空格是原样拼的）+ 右侧 96×72dp 灰盒里 `HomeIcons.Article` 28dp 当封面占位（L971-984，**没有真实图片加载，也没有 Coil**）。**整条新闻无 clickable** → 资讯不可点开。 |

状态来源小结：本文件**零 `mutableStateOf`、零 store、零 Flow、零 `LaunchedEffect`**（唯一 `remember` 是 L303 `remember(items)`）。全部区块都是「参数进 / lambda 出」的无状态组件，业务态一律在宿主（`HomeScreen.kt` 或 `NativeJetpackHome.kt`）。图标分三批来源：`HomeIcons.*`（`ImageVector`，`HomeIcons.kt`）、`HomeStrokeIcons.*`（含 `forServiceLabel` 动态映射，`HomeStrokeIcons.kt`）、`HomeAssetIcon`/`Res.drawable`（位图，`:core:design`）。

#### HomeRouteHost.kt

文件：`feature/home/src/commonMain/kotlin/com.example.my_kmp_project/feature/home/HomeRouteHost.kt`（3574 行）—— Home 域二级屏集散地：一个 `when(route)` 入口 + 30 多个私有屏 + 4 个行模型 + 1 个 Mock 仓。

| 符号 | 行号 | 签名 | 真实行为 |
|------|------|------|----------|
| `FlutterToolbarHeight` | 112 | `private val FlutterToolbarHeight = 56.dp` | 与 `AllServicesScreen.kt:52` 重复定义一份（同注释、同值，**两处未提取共用**）。 |
| `HomeRouteHost` | 115-208 | `@Composable fun HomeRouteHost(route: String, onBack: () -> Unit, onNavigate: (String) -> Unit = {})` | 纯分派，本体无状态（详见「HomeRouteHost 专题」）。 |
| `HomeListRow` | 210 | `data class HomeListRow(val title: String, val subtitle: String)` | 通用两行列表项，多个屏/Mock 复用。 |
| `UsedCarOrderRow` | 212-225 | `data class UsedCarOrderRow(id: String, kindLabel, statusLabel, submittedDate, vehicleModel, plateNo, modelYear: Int, mileageKm: Int, amountLabel: String, amount: Int, customerName, vin: String = "")` | 注意金额是 **`Int`（分/元级整数）**，同时存在 `amountLabel` 预格式化串。 |
| `NewCarFollowRow` | 227-237 | `data class NewCarFollowRow(id, customerName, phone, vehicle, stage, intentBand, nextFollow, owner, overdue: Boolean = false)` | `overdue` 供列表标红；`intentBand` 是 `NewCarFollowArchiveDetailScreen` 的「调整级别」初值。 |
| `AnalyticsRecordRow` | 239-251 | `data class AnalyticsRecordRow(code, title, subtitle, status, pv: Int, uv: Int, clicks: Int, converts: Int, roi: Double, featured: Boolean = false, anomaly: Boolean = false)` | `status` 取 `"active"/"paused"/"draft"`（见 L294-303 数据）。 |
| `HomeSecondaryMock` | 253-334 | `object HomeSecondaryMock` | 见下「数据来源」。 |

##### HomeSecondaryMock（L253-334）

注释 L254 自己承认定位：「Prefer [HomeSecondaryStore] for mutable pipelines; kept for ledger/analytics」。分两类：

| 成员 | 行号 | 形态 | 实际来源 |
|------|------|------|----------|
| `usedCarOrders` / `usedCars` | 255-260 | `get()` 计算属性 | **代理 `HomeSecondaryStore.usedCarOrders()`**；`usedCars` 派生成 `HomeListRow`，字符串里用 `amount / 10000.0` 万、`mileageKm / 10000.0` 万公里（L259） |
| `ledger` | 261-286 | **写死 3 条 `LedgerTransaction`** | 日期硬编码 2026-09-22~24，含 `note = null` 的第三条（用来验 `LedgerScreens` 的备注分支） |
| `analytics` | 287-290 | 写死 2 条 `HomeListRow` | 「本周线索转化 12.4%」「试驾到店」 |
| `analyticsTotal` | 292 | `= 48` | 注释 L291：Flutter SoT 共 48 条，此处只落 10 条 |
| `analyticsRecords` | 293-304 | 写死 10 条 `AnalyticsRecordRow` | AN-0001…AN-0010，其中 2 条 `anomaly = true`、4 条 `featured = true`，AN-0004 是 `clicks = 0 / converts = 0 / roi = 0.0` 的零值样本（喂给 `AnalyticsDetailScreen` 的 `coerceAtLeast(1)` 分母） |
| `partners` / `followUps` / `appointments` / `orders` | 305-321 | 各写死 2-3 条 `HomeListRow` | 4 个 Todo 屏的数据源 |
| `afterSales` / `afterSalesDetails` / `newCars` / `newCarFollows` | 322-333 | `get()` 计算属性 | **代理 `HomeSecondaryStore.afterSalesDetails()` / `.newCarFollows()`**，再映射成展示行 |

结论：**没有任何屏走 `feature/home/api/`** —— 全部是 `HomeMockData` / `HomeSecondaryMock` / `HomeSecondaryStore` 三者之一。`DeferredDestinationStub` 定义在 `feature/shell/src/commonMain/kotlin/com.example.my_kmp_project/feature/shell/DeferredDestinationStub.kt:25`，**本模块 12 个文件里 grep 无引用**（`grep -rn "DeferredDestinationStub" --include="*.kt" .` 只命中 shell 模块自身），Home 的占位形式是「屏存在但内容写死 + 控件无 clickable」。

### HomeRouteHost 专题

**对外暴露**：唯一 public 符号 `HomeRouteHost(route: String, onBack: () -> Unit, onNavigate: (String) -> Unit = {})`（`HomeRouteHost.kt:116`）+ public `FlutterFeatureTopBar`（`:559`）+ 4 个行模型 + `HomeSecondaryMock`。**没有 expect/actual，没有 navigator 依赖** —— 它只吃一个 `route` 字符串、吐一个 `onNavigate(String)`，跳转能力完全由宿主决定，因此可以脱离 `LocalAppNavigator` 单测。

调用点（三端）：

| 端 | 调用链 | 文件:行 |
|----|--------|---------|
| 装配层（common，三端共用） | `ShellBootstrap.install()` → `CrossFeatureScreens.install(AppCrossFeatureScreens)` | `composeApp/src/commonMain/kotlin/com.example.my_kmp_project/app/ShellBootstrap.kt:24`（`install` 定义 `:20-24`；由 `AppShell.kt:107` 触发） |
| 端口接口 | `CrossFeatureScreenPort.HomeRouteScreen(route, onBack, onNavigate)`；`CrossFeatureScreens.HomeRouteScreen(...)` 转 `require().…` | `component/src/commonMain/kotlin/com.example.my_kmp_project/component/routing/CrossFeatureScreenPort.kt:40`（接口）与 `:100-105`（外观转发，未 install 时 `:67` 抛「run ShellBootstrap.install() before rendering」） |
| 实现 | `override fun HomeRouteScreen(...) { HomeRouteHost(route, onBack, onNavigate) }` | `composeApp/src/commonMain/kotlin/com.example.my_kmp_project/app/AppCrossFeatureScreens.kt:88-94`（`HomeRouteHost` 调用在 `:93`） |
| Android Compose 壳 | `HomeScreen` 内部 `destination` 非空且 host==Home/兜底时直接本模块函数 | `feature/home/.../HomeScreen.kt:82`、`:97` |
| Android **native** Tab 壳 | `ShellOverlay.HomeRoute -> { val route = homeRoute ?: HomeRoutes.Search; HomeRouteHost(route, onBack = { popHomeRoute() }, onNavigate = { navigateSecondary(it) }) }` | `composeApp/src/androidMain/kotlin/com.example.my_kmp_project/nativeshell/NativeAndroidMain.kt:563`（默认路由 `HomeRoutes.Search`，L 前一行 `:562`；自带栈 `popHomeRoute()`，与 common 的单层 `destination = null` 不同） |
| iOS / OHOS | 无独立调用：`iosApp/` `ContentView.swift` 与 `harmonyApp/` `Index.ets` 只托管 `App()`/`MainArkUIViewController`，二级路由经上面 common 链路 | 见 AGENTS.md「Thin shell」；本模块 grep 无 iOS/OHOS 直接引用 |

**内部分派表（`HomeRouteHost.kt:121-207`，逐条）**：

| 路由常量（值在 `core/router/.../FeatureRoutes.kt`） | 行号 | 目标屏 | onNavigate 语义 |
|---|---|---|---|
| `HomeRoutes.Search` `/home/search` | 122 | `HomeSearchScreen(onBack)` | — |
| `HomeRoutes.Strategy` | 123 | `StrategyScreen(onBack)` | — |
| `HomeRoutes.LearningReport` | 124-127 | `LearningReportScreen` | **`onOpenMembership = { onNavigate("/pay/membership") }`**（L126，**硬编码字符串而非 `MineRoutes.Membership` 常量**，值恰好同 `FeatureRoutes.kt:85`） |
| `HomeRoutes.CheckInMall` | 128 | `CheckInMallScreen(onBack)` | — |
| `HomeRoutes.TransliterFeed` | 129-132 | `TransliterFeedScreen` | `onOpenHotRank = { onNavigate(HomeRoutes.HotRankDetail) }` |
| `HomeRoutes.HotRankDetail` | 133 | `HotRankDetailScreen(onBack)` | — |
| `HomeRoutes.LifeService` | 134 | `LifeServiceScreen(onBack)` | — |
| `HomeRoutes.LiveCommerce` | 135-138 | `LiveCommerceScreen` | `onJoin = { onNavigate(AppRoutePath.community) }` |
| `HomeRoutes.Club` | 139-142 | `ClubScreen` | 同上 `AppRoutePath.community` |
| `HomeRoutes.UsedCar` | 143-150 | `UsedCarListScreen` | **`onItem` 先 `HomeSecondaryStore.selectUsedCar(order.id)` 写入选中态，再 `onNavigate(HomeRoutes.UsedCarDetail)`**（L146-147）；`onCreate = onNavigate(UsedCarCreate)` |
| `HomeRoutes.UsedCarDetail` | 151-155 | `UsedCarDetailScreen` | 入参 `HomeSecondaryStore.findUsedCar(selectedUsedCarId) ?: usedCarOrders().first()`（**先查选中、查不到退回首条**） |
| `HomeRoutes.UsedCarCreate` | 156 | `UsedCarCreateScreen(onBack)` | — |
| `HomeRoutes.Ledger` | 157-161 | `LedgerListScreen` | `items = HomeSecondaryMock.ledger`（**Mock 直传**）；`onItem = onNavigate(LedgerDetail)`（**不传具体 item**） |
| `HomeRoutes.LedgerDetail` | 162-165 | `LedgerDetailScreen` | `item = HomeSecondaryMock.ledger.firstOrNull()` —— **无论点哪条都只展示第一条**，明细选择链路缺失（对比 UsedCar/AfterSales/NewCar 都走 store.select） |
| `HomeRoutes.DataAnalytics` | 166-169 | `AnalyticsListScreen` | `onItem = onNavigate(DataAnalyticsDetail)` |
| `HomeRoutes.DataAnalyticsDetail` | 170-173 | `AnalyticsDetailScreen` | `record = HomeSecondaryMock.analyticsRecords.first()`（**同样恒首条 AN-0001**，且 `.first()` 空列表会抛，L171） |
| `HomeRoutes.TodoPartner` | 174 | `PartnerPendingScreen(onBack)` | 数据 `HomeTodoStore.pendingApplications()`（读屏 L3193-3194），**不是** `HomeSecondaryMock.partners`（该成员全仓 0 引用） |
| `HomeRoutes.TodoFollowUp` | 175 | `FollowUpCustomersScreen(onBack)` | 转发 `TodoListScreen` + **函数内写死的 `Triple` 列表**（L3275-3278）；`.followUps` 未使用 |
| `HomeRoutes.TodoAfterSales` | 176 | `AfterSalesAppointmentsScreen(onBack)` | 同上，硬编码 `Triple`（L3288-3290）；`.appointments` 未使用 |
| `HomeRoutes.TodoOrderReview` | 177 | `StoreReviewOrdersScreen(onBack)` | 同上，硬编码 `Triple`（L3301-3302）；`.orders` 未使用 |
| `HomeRoutes.AfterSales` | 178-185 | `AfterSalesListScreen` | `onItem`：`HomeSecondaryStore.selectAfterSales(row.id)` + `onNavigate(AfterSalesDetail)`；`onCreate` → `AfterSalesCreate` |
| `HomeRoutes.AfterSalesCreate` | 186 | `AfterSalesCreateScreen(onBack)` | — |
| `HomeRoutes.AfterSalesDetail` | 187-191 | `AfterSalesServiceDetailScreen` | `findAfterSales(selectedAfterSalesId) ?: afterSalesDetails().first()` |
| `HomeRoutes.NewCarFollow` | 192-199 | `NewCarFollowListScreen` | `selectNewCarFollow(row.id)` + `onNavigate(NewCarFollowDetail)`；`onCreate` → `NewCarFollowCreate` |
| `HomeRoutes.NewCarFollowCreate` | 200 | `NewCarFollowCreateScreen(onBack)` | — |
| `HomeRoutes.NewCarFollowDetail` | 201-205 | `NewCarFollowArchiveDetailScreen` | `findNewCarFollow(selectedNewCarFollowId) ?: newCarFollows().first()` |
| **else** | 206 | `CrudDetailScreen("未识别路由", "route=$route", onBack)` | 兜底把未知 route 渲染成「未识别路由」详情页 —— 这就是 `HomeScreen.kt:63` 把 `ProductRouteHost.Unknown` 原文塞进 destination 后仍不白屏的原因 |

`ReportMainTabRoot` 上报：**`HomeRouteHost` 本体不调用它**（L116-208 全无）。上报责任在各屏：本分册覆盖的屏**一致上报 `isRoot = false`** —— `HomeSearchScreen.kt:87`、`StrategyScreen.kt:51`、`LearningReportScreen.kt:66`、`AllServicesScreen.kt:59`、`LedgerScreens.kt:53` / `:87`、`HomeDetailScreens.kt:57` / `:130` / `:210`；`HomeRouteHost.kt` 内私有屏同样上报 false（见下节）。唯一 `isRoot = true` 是 `HomeScreen.kt:114` 的 `HomeRootContent`。**为什么 false**：这些屏都在二级栈，Shell 的 `ImmersiveInsets.shellContentInsets` 需按 `bottomBarVisible = false` 走 `navigationBarsPadding()`，若误报 true 会重复叠一层 `MainBottomBar` 高度的 padding。

##### 逐屏要点（有业务动作的才展开）

以下行号均在 `feature/home/src/commonMain/kotlin/com.example.my_kmp_project/feature/home/HomeRouteHost.kt`。

`LifeServiceScreen(onBack)`（`:336-530`）：函数内声明 **3 个局部 data class**（`LifeShortcut`/`LifeDaily`/`LifeCourse`，L339-347），数据是函数体 `listOf(...)` 字面量（L348-362，含 picsum URL）。**所有点击都是 `showPlatformToast(...)`**：快捷入口 L382、每日推荐行 L410、课程卡 L462、底部「进入跟读视频专区」L525 —— 专区按钮本该跳 `HomeRoutes.TransliterFeed` 却只 Toast（占位）。网络图走 `core.ui.PlatformNetworkImage`（L435、L465，`placeholder = Res.drawable.community_avatar / community_post_a`）。`ReportMainTabRoot(isRoot = false)` 在 L364，顶栏 `FlutterFeatureTopBar(title = "生活服务")` L371。

`LifeSectionHeader(title)`（`:532-544`）：左标题 17sp + 右「更多 >」文本（L542）——**不可点**。

`LiveCommerceScreen(onBack, onJoin = {})`（`:546-550`）与 `ClubScreen(onBack, onJoin = {})`（`:552-555`）：两个单行薄壳，分别 `ClubContentBody(title = "直播带货")` / `(title = "Club")`；注释 L548 说明 Flutter 侧直播带货本就复用 `HomeClubTabContent`。

`ClubContentBody(title, onBack, onJoin = {})`（`:611-787`）：状态仅 `var filter by remember { mutableStateOf(0) }`（L614），filters = 最新/嘉宾分享/资料（L615），**切 filter 不过滤 posts**（posts 是 L623-637 写死的 2 条函数内 `ClubPost`，L716 无条件渲染）。唯一真跳转：**「+ 加入」`clickable(onClick = onJoin)`**（L680），宿主注入 `onNavigate(AppRoutePath.community)`（L137/L141）。底部「进入社区查看更多」只 Toast「社区」（L782）。PDF 附件行（L744-758）与 ↗/💬/♡ 操作行（L760-768）**全部不可点**。L646 注释：`Spacer(1.dp)` 为对齐 Flutter gate（mse ≤2%）。

`CheckInMallScreen(onBack)`（`:789-1114`）：**本分册唯一读写 KV store 的屏**。L791 注释「bound to HomePointsStore (mock PointsApi)」；L794 读 `HomePointsStore.version` 建立订阅，随后 `balance`/`streak`/`checkedInToday`（L795-797）、`todayReward`（L808）。本地态 `remind`（L798）、`checkingIn`（L799）。日历 `DayCell` 8 格（L800-809）：前 7 格 `signed = false` 写死，今天一格 `reward = todayReward` + `signed = checkedToday`。任务 `TaskRow` 3 条（L811-815，图标是 emoji）。**副作用**：签到按钮 `clickable(enabled = !checkedToday && !checkingIn)` → `checkingIn = true` → `HomePointsStore.checkIn()` → 复位 → Toast「签到成功，+N积分」/「今日已签到」（L926-937）；任务「领取」再调一次 `checkIn()`（L1070-1077），「去完成」仅 Toast（L1078）；「签到提醒」开关只翻本地 `remind`（L994），**不持久化、不注册通知**；「积分换礼」是空态「暂无积分商品」（L1089-1111）。蓝头与白身用两次 `offset(y = (-18).dp)`（L831/L885）做重叠，注释 L830 记录调参（−28 过头、−18 对齐）。

`TransliterFeedScreen(onBack, onOpenHotRank)`（`:1116-1634`）：`ReportMainTabRoot(false)` L1168。头部**不用 `FlutterFeatureTopBar`**，自绘带 `statusBarsPadding()` 的「返回 + 搜索胶囊 + 历史 + 通知」行（L1170-1220，注释 L1170「Flutter AppBar, not MineTopBar title-only」）。跳转能力只有一条：**热榜入口 `clickable(onClick = onOpenHotRank)`（L1523）→ 宿主 `onNavigate(HomeRoutes.HotRankDetail)`（L131）**。其余全 Toast：搜索 L1201、历史 L1214、通知 L1218、分类 chip L1254、菜单 L1285、功能位 L1355-1357、最近在学 L1380、换一换 L1422。榜单缩略用 `HomeMockData.rankItemsForTab(0).take(3)`（L1528）——**写死取 tab 0**。封面大量用 `Res.drawable.home_transliter_home_cover_01..12`（import L77-88）与 `home_transliter_home_thumb_default`（L1462）。

`TransliterCoverWithDuration(cover: DrawableResource, duration: String, modifier: Modifier = Modifier)`（`:1636-1669`）：参数注入，8dp 圆角（注释 L1643 对齐 Flutter `thumbRadius`）+ 右下角 `home_transliter_home_icon_duration` 时长角标（L1660）。无交互。

`TransliterHistoryClockIcon(modifier: Modifier = Modifier, tint: Color = DemoColors.TextPrimary)`（`:1671-1693`）：Canvas 手绘时钟，无交互。

`TransliterSectionTitle(title: String, subtitle: String? = null, action: String? = null, onAction: (() -> Unit)? = null, trailing: (@Composable () -> Unit)? = null)`（`:1695-1726`）：**两个 lambda 槽** —— `onAction` 普通回调（`action != null` 时渲染文本 `clickable { onAction?.invoke() }`，L1722；`onAction` 传 null 时点击是 no-op）+ `trailing` Composable slot。

`HotRankDetailScreen(onBack)`（`:1728-1989`）：数据 `HomeMockData.hotRankCategories` / `hotRankAgeFilters`（L1731-1732）；`categoryIndex`/`ageIndex` 用 `mutableIntStateOf`，**初值靠 `indexOf("热搜榜").coerceAtLeast(0)` 反查**（L1734-1735，Mock 里没这个字符串时静默回落 0）；`showAgeMenu`（L1736）；`items = remember(categoryIndex) { HomeMockData.hotRankDetailItems(categoryIndex) }`（L1737）。结构：遮罩层 `clickable(enabled = showAgeMenu) { showAgeMenu = false }`（L1751）+ `statusBarsPadding()`（L1759）+ 分享按钮 **Toast「分享（mock）」**（L1783）+ `verticalScroll` 分类条（L1822/L1839）+ 年龄下拉（L1889/L1907）+ **`LazyColumn` 榜单**（L1918，行点击 `showPlatformToast(item.title)` L1930）。

`analyticsUvLabel(uv: Int): String`（`:1991`）与 `analyticsRoiLabel(roi: Double): String`（`:2005-2010`）：非 Composable 纯格式化。

`AnalyticsListScreen(onBack, onItem: (AnalyticsRecordRow) -> Unit)`（`:2012-2162`）：`ReportMainTabRoot(false)` L2027，顶栏 `FlutterFeatureTopBar(title = "数据分析")` L2029。注释 L2016 关键：**「summary + ring/bars tiles (Canvas ≈ wys_chart; CPF 无 Vico/KoalaPlot)」** —— 图表全手绘 Canvas；5 个局部色常量 L2017-2021。数据是 `HomeSecondaryMock.analyticsRecords`（10 条）与 `analyticsTotal = 48`；行点击 → `onItem` → `onNavigate(HomeRoutes.DataAnalyticsDetail)`（L166-169）。

`NewCarFollowListScreen(onBack, onItem: (NewCarFollowRow) -> Unit, onCreate)`（`:2164-2357`）：`tabs = listOf("全部","高意向","中意向","低意向","逾期")`（L2179）+ `var tab by remember { mutableStateOf("全部") }`（L2175）；数据 **`run { HomeSecondaryStore.version; HomeSecondaryStore.newCarFollows() }`**（L2177-2178，读 `version` 这个 `mutableIntStateOf` 建立订阅，新建后自动刷新）。顶栏 `MineTopBar(title = "新车跟进", containerColor = Color.White)`（L2194）。局部色 L2171-2173。行 → `selectNewCarFollow(row.id)` + `onNavigate(NewCarFollowDetail)`（L194-197）；新建 → `onNavigate(NewCarFollowCreate)`（L198）。

`UsedCarCreateScreen(onBack)`（`:2359-2462`）：8 个 `remember` 字段（L2365-2372），`customer` 初值「张先生 138****2101」；`kinds` = 置换/专卖/收车 三对 label→英文码（L2364，**英文码从未使用**，落库的是中文 `kind`）。客户行点击在张/李两值间来回切（L2405-2407，**假选择器**）。**提交（L2425-2457）真实改内存**：`toIntOrNull()` / 金额退路 `toDoubleOrNull()?.toInt()`（L2427-2429）→ `HomeSecondaryStore.validateUsedCarCreate(...)`（L2432）→ 有错 `showPlatformToast(err)`（L2442）；无错 `createUsedCar(...)`（L2444，参数处用 `mileageInt!!`/`yearInt!!`/`amountInt!!` 非空断言 L2448-2450，**安全性依赖 validate 先拦住**）→ Toast「提交成功」→ `onBack()`。**无网络请求**；`HomeSecondaryStore` 是内存 `mutableListOf` + `bump()`（`HomeSecondaryStore.kt:13-31`），**进程重启即丢**。

`UsedCarField(label: String, value: String, onChange: (String) -> Unit)`（`:2464-2473`）：受控 `OutlinedTextField`，`label = { Text(label) }`、`singleLine`。

`UsedCarDetailScreen(order: UsedCarOrderRow, onBack)`（`:2475-2549`）：`ReportMainTabRoot(false)` L2480 + `MineTopBar(title = "业务单详情")` L2482，只读展示；`UsedCarSection`（`:2551-2575`）做 key-value 分组。

`UsedCarListScreen(onBack, onItem: (UsedCarOrderRow) -> Unit, onCreate)`（`:2576-2855`）：局部色 L2581-2583；数据 `run { HomeSecondaryStore.version; HomeSecondaryStore.usedCarOrders() }`（L2587-2588）；筛选态 `status`（初值「全部」）与 `kind`（初值「全部类型」）L2592-2593；顶栏 `FlutterFeatureTopBar`（L2608）；行 → `onItem`（宿主先 `selectUsedCar` 再导航，L145-148）。

`AfterSalesCreateScreen(onBack)`（`:2857-2921`）：7 个 `remember` 字段（L2860-2866），**`date` 初值写死 `"2026-09-25"`**（L2867，不读系统时钟）。提交走 `HomeSecondaryStore.validateAfterSalesCreate`（L2894）→ `createAfterSales`（L2904）。

`AfterSalesDetailScreen(row: HomeListRow, onBack)`（`:2923-2940`）：**旧轻量详情**，吃 2 字段的 `HomeListRow`；路由实际用的是 `HomeDetailScreens.kt` 的 `AfterSalesServiceDetailScreen`（`HomeRouteHost.kt:187`）。`grep -n "AfterSalesDetailScreen(" HomeRouteHost.kt` 只命中定义行 → **未接线的遗留代码**。同情况还有 `NewCarFollowDetailScreen(row: NewCarFollowRow, onBack)`（`:3011-3031`，路由走 `NewCarFollowArchiveDetailScreen`，L201）。

`AfterSalesListScreen(onBack, onItem: (AfterSalesDetailRow) -> Unit, onCreate)`（`:3033-3188`）：`val canCreate = HomeSecondaryStore.afterSalesCanCreate`（L3040，注释 L3039「Logic-first: advisor demo canCreate=true (Flutter can_create for store staff)」）+ `run { version; afterSalesDetails() }`（L3042-3043）；`canCreate == false` 时不渲染新建入口。

`PartnerPendingScreen(onBack)`（`:3190-3268`）：**本模块唯一有真实状态流转的 Todo 屏**。数据 `run { HomeTodoStore.version; HomeTodoStore.pendingApplications() }`（L3193-3194）。计数文案「N 位新成员」/「暂无待审申请」（L3200）+ 空态「有人申请入店后会显示在这里」（L3207）。**「拒绝」→ `HomeTodoStore.rejectJoin(app.applicationId)`，返回 true 才 Toast「已拒绝 X」（L3240-3245）；「确认入店」→ `approveJoin(...)` + Toast「已确认 X」（L3256-3260）**。store 侧只对 `isPending` 项生效并 `bump()` version（`HomeTodoStore.kt:59-74`），列表与首页 `HomeTodoCardStrip` 计数（`HomeTodoStore.kt:76 todoCards()`）同步刷新。卡片字段：`displayName · roleLabel` + 「申请于 createdAtLabel」（L3220-3229）。

`FollowUpCustomersScreen(onBack)`（`:3270-3281`）/ `AfterSalesAppointmentsScreen(onBack)`（`:3283-3294`）/ `StoreReviewOrdersScreen(onBack)`（`:3296-3306`）：**三个只带 `onBack` 的单行薄壳**，各自把**函数内写死的 `Triple` 列表**塞给 `TodoListScreen`（如 L3276「陈先生 / 意向凯美瑞 · 未回访 2 天 / 紧急」、L3289「10:30 · 保养 / 京 A·88888 · 技师小刘 / null」、L3302「订单 #90821 / 定金 ¥5000 · 待店长审 / 待审」）。**它们不读 `HomeSecondaryMock.followUps/appointments/orders`** —— 那三个成员全仓引用数为 0（见「真实状态与坑」）。行不可点。

`TodoListScreen(title: String, subtitle: String, rows: List<Triple<String, String, String?>>, onBack: () -> Unit)`（`:3308-3353`）：`ReportMainTabRoot(false)` L3315 + `MineTopBar(title, containerColor = DemoColors.PageBg)` L3317 + subtitle 文本 L3318-3323；`rows` 用 `Triple.first/second/third`，**`third` 非空才渲染 Accent 10% 角标**（L3339-3349）。整卡无 clickable。

`TodoForbiddenScreen(title: String, onBack: () -> Unit)`（`:3355-3411`）：🔒 emoji（L3378）+「暂无查看权限」+ 多行说明（L3389 内嵌 `\n`）+「返回」按钮 `clickable(onClick = onBack)`（L3405）。**全模块无调用点**（`grep -n "TodoForbiddenScreen(" HomeRouteHost.kt` 只命中 L3356）→ 权限态尚未接入任何路由。

`TodoEmpty(title: String, subtitle: String)`（`:3413-3422`）：居中两行文本，无 modifier、**无调用点**。

`TodoCard(avatarLabel, title, subtitle, badge: String? = null, badgeColor: Color = DemoColors.Accent, avatarTint: Color = DemoColors.Accent.copy(alpha = 0.12f), avatarFg: Color = DemoColors.Accent, footer: (@Composable () -> Unit)? = null)`（`:3424-3478`）：**8 参数、4 个默认色值、1 个 Composable slot**（`footer()` 在 L3475 调用）；头像用 `avatarLabel.take(1)` 首字（L3452）而非图片。**无调用点**（首页待办卡用的是另一文件 `HomeTodoCardStrip.kt`）。

`CrudListScreen(title: String, items: List<HomeListRow>, onBack: () -> Unit, onItem: ((HomeListRow) -> Unit)? = null, onCreate: (() -> Unit)? = null)`（`:3480-3527`）：**两个可空回调**决定渲染：`onCreate != null` 才在 `MineTopBar(actions = { TextButton "新建" })` 出现新建按钮（L3494-3500）；行是 `clickable { onItem?.invoke(row) }`（L3514），传 null 即 no-op。空列表 → 「暂无数据」（L3502-3505）；非空 → `LazyColumn(contentPadding = PaddingValues(16.dp))`（L3507）。**无调用点**。

`CrudDetailScreen(title: String, body: Any, onBack: () -> Unit)`（`:3529-3545`）：`body` 声明为 **`Any`** —— `is HomeListRow` 拼「title\nsubtitle」，else `body.toString()`（L3531-3534）。**全模块未知路由的唯一兜底**（L206 传 `"未识别路由"`、`"route=$route"`）。

`CrudCreateScreen(title: String, onBack: () -> Unit, onSubmit: () -> Unit)`（`:3547-3574`）：单字段 `OutlinedTextField(label = "标题")`（L3554-3559）+ `Button(colors = ButtonDefaults.buttonColors(containerColor = DemoColors.Primary))`（L3561-3571）：空白 → Toast「请填写标题」，否则 `onSubmit()`。**无调用点**。

> 死代码核验命令：`grep -n "<name>(" feature/home/src/commonMain/kotlin/com.example.my_kmp_project/feature/home/HomeRouteHost.kt` —— 只命中定义行即判定未接线（`AfterSalesDetailScreen`、`NewCarFollowDetailScreen`、`TodoForbiddenScreen`、`TodoEmpty`、`TodoCard`、`CrudListScreen`、`CrudCreateScreen`；`searchRankCoverRes` 在 `HomeSearchScreen.kt:73`）。

### 首页 → 二级页跳转链路

**链路模型（4 步，全模块统一）**：首页 tab 上的控件**从不直接持有 navigator**，只把「路由常量或中文标签」交给 `HomeScreen.kt:45` 的局部 `fun go(raw: String)`；`go` 用 `ProductRouteDispatch.resolve(raw)`（`core/router/src/commonMain/kotlin/com.example.my_kmp_project/core/router/ProductRouteDispatch.kt:62`）归一成 `ProductRouteTarget(path, host, toastMessage)`，再按 host 走两条出口之一：

1. **交给 Shell 的 `LocalAppNavigator`**（跨 tab 全屏能力）：`Web → AppRoute.InAppWeb(OfflineWebFixtureUrl)`（`HomeScreen.kt:49`）、`Scan → AppRoute.Scan`（`:50`）。Auth/Tab/Content 三类里，前两类只 Toast（`:61`/`:62`），Content 走 `navigator.navigate(AppRoute.Friend/Live/Classroom/Media)`（`:51-55`）。
2. **落到本地单层栈** `destination`（`HomeScreen.kt:42`）→ 由 `HomeScreen.kt:67-104` 的 `when` 按 `ProductRouteDispatch.hostForPath(dest)`（`:112` 定义）渲染 Home / Content / Mine / 兜底 Home 四类宿主。

> `destination` 只有一层：`onBack = { destination = null }`（`HomeScreen.kt:74/84/89/94/99`）永远回首页，二级页里再 `onNavigate` 会**替换**当前二级页而不是压栈。Android native 壳是另一套多栈实现（`NativeAndroidMain.kt:563` 的 `popHomeRoute()`），行为不同。

#### 表 5-1 · 首页 tab 上的入口（文件均在 `feature/home/src/commonMain/kotlin/com.example.my_kmp_project/feature/home/`）

| # | 入口控件 : 行号 | 回调参数 | `onNavigate` 实参 | `go` 解析结果 | 最终渲染 |
|---|---|---|---|---|---|
| 1 | `HomeSearchBar` `HomeScreen.kt:197` | `onSearch` | `HomeRoutes.Search` = `/home/search`（`FeatureRoutes.kt:18`） | host Home（`ProductRouteDispatch.kt:123` `p.startsWith("/home/")`） | `HomeRouteHost.kt:122` → `HomeSearchScreen` |
| 2 | `HomeSearchBar` `:198` | `onScan` | 字面量 `"扫一扫"` | 别名表 `ProductRouteDispatch.kt:95-96` → `/scan` + host Scan | **不走 destination**：`navigator.navigate(AppRoute.Scan)` `HomeScreen.kt:50` |
| 3 | `HomeFeatureGrid` `:205` | `onFeature(label)` | `HomeMockData.features` 的标签原文 | `HomeRoutes.fromLabel`（`FeatureRoutes.kt:48-75`）；未命中 → host Unknown → `destination = raw`（`:63`） | 命中 → 对应屏；未命中 → `HomeRouteHost.kt:206` `CrudDetailScreen("未识别路由")` |
| 4 | `HomeTodoCardStrip` `:212-216` | `onOpen(card)` | `card.actionRoute.ifBlank { HomeRoutes.fromLabel(card.title) ?: card.title }` | 四条待办路由已在 store 里预置（`HomeTodoStore.kt:84/92/100/108`） | `HomeRouteHost.kt:174/175/176/177` → `PartnerPendingScreen` / `FollowUpCustomersScreen` / `AfterSalesAppointmentsScreen` / `StoreReviewOrdersScreen` |
| 5 | `HomeStoreMetricsCard` `:237` | `onOpenLedger` | `HomeRoutes.Ledger` = `/home/ledger`（`FeatureRoutes.kt:30`） | host Home | `HomeRouteHost.kt:157` → `LedgerListScreen` |
| 6 | `HomeStoreMetricsCard` `:233-235` | `onStoreTap` | 已登录：`showSwitchStore = true`（**不出模块**，`HomeScreen.kt:147` 弹 `CrossFeatureScreens.SwitchStoreDialog`）；未登录：`"请先登录"` | `"请先登录"` 在 `toastOnlyLabels`（`ProductRouteDispatch.kt:33-38`，命中 `:35`）→ host Toast（`:62`） | `HomeScreen.kt:48` `showPlatformToast` |
| 7 | `HomeHubEntryCard`（投资策略）`:245` | `onClick` | `HomeRoutes.Strategy` | host Home | `HomeRouteHost.kt:123` → `StrategyScreen` |
| 8 | `HomeServiceGrid` `:252-253`（仅 `label == "更多" \\|\\| label == "全部"`）与 `:255` `onOpenAll` | `onService` / `onOpenAll` | `HomeRoutes.AllServices` = `/home/all_services`，与 `AppRoutePath.homeAllServices`（`AppRoutePath.kt:13`）同值 | host AllServices（`ProductRouteDispatch.kt:92/115`）→ `destination = "services"` **魔法字符串**（`HomeScreen.kt:58`） | `HomeScreen.kt:73-79` **直接挂 `AllServicesScreen`，绕过 `HomeRouteHost`** |
| 9 | `HomeContactList` `:258` | `onContact = {}` | — | — | **空 lambda，永远不跳** |
| 10 | `HomeNewsList` `:259` | `onOpenAll = {}` | — | — | **空 lambda，永远不跳** |
| 11 | `HomeHubEntryCard`（学习报告）`:265` | `onClick` | `HomeRoutes.LearningReport` | host Home | `HomeRouteHost.kt:124-127` → `LearningReportScreen` |

#### 表 5-2 · `AllServicesScreen` 的回填跳转

`HomeScreen.kt:75-78` 注入 `onOpen = { label -> destination = null; go(label) }` —— **先回首页再解析**，所以「全部服务」页点格子会让 LazyColumn 重建（`showTodos` 的 50ms 延迟 `HomeScreen.kt:124-127` 会重来一次）。标签 → 路由同样只靠 `fromLabel` 表；格子自身 `AllServicesScreen.kt:94/108` 的 `if (!isEditing) onOpen(it.label)` 保证编辑态不跳。

`AppShell.kt:426` 的另一处挂载 `AllServicesScreen(onBack = { clearShellRoute() })` **不传 onOpen**（默认 `{}`，`AllServicesScreen.kt:54`）→ 该路径下所有格子点击无效。

#### 表 5-3 · 二级页之间（`HomeRouteHost.kt` 内部 `onNavigate`，全部 12 处）

| 起点 : 行 | `onNavigate` 实参 | 解析后 host | 落点 |
|---|---|---|---|
| `LearningReportScreen` 会员横幅 `HomeRouteHost.kt:126` | **`"/pay/membership"` 硬编码字符串**（未用 `MineRoutes` 常量，值同 `FeatureRoutes.kt:85`） | Mine（`ProductRouteDispatch.kt:126`）→ `destination` 换栈 | `HomeScreen.kt:92-96` `CrossFeatureScreens.MineRouteScreen` |
| `TransliterFeedScreen` 热榜行 `:131` | `HomeRoutes.HotRankDetail` | Home | `HomeRouteHost.kt:133` `HotRankDetailScreen`（**替换**当前跟读页） |
| `LiveCommerceScreen` / `ClubScreen` 「+ 加入」`:137` / `:141` | `AppRoutePath.community` = `/community`（`AppRoutePath.kt:45`） | **Tab**（`ProductRouteDispatch.kt:122`，`p == AppRoutePath.community`） | `HomeScreen.kt:61` `showPlatformToast("请切换底部 Tab")` —— **不会真的切 tab** |
| `UsedCarListScreen` 行 `:147`（先 `:146` `HomeSecondaryStore.selectUsedCar(order.id)`） | `HomeRoutes.UsedCarDetail` | Home | `HomeRouteHost.kt:151-155` `UsedCarDetailScreen`（详情读 `selectedUsedCarId`） |
| `UsedCarListScreen` 新建 `:149` | `HomeRoutes.UsedCarCreate` | Home | `:156` `UsedCarCreateScreen` |
| `LedgerListScreen` 行 `:160` | `HomeRoutes.LedgerDetail` | Home | `:162-165` `LedgerDetailScreen` —— **只传 `ledger.firstOrNull()`，无选中态** |
| `AnalyticsListScreen` 行 `:168` | `HomeRoutes.DataAnalyticsDetail` | Home | `:170-173` `AnalyticsDetailScreen` —— 同上，恒 `analyticsRecords.first()` |
| `AfterSalesListScreen` 行 `:182`（先 `:181` `selectAfterSales`） | `HomeRoutes.AfterSalesDetail` | Home | `:187-191` `AfterSalesServiceDetailScreen` |
| `AfterSalesListScreen` 新建 `:184` | `HomeRoutes.AfterSalesCreate` | Home | `:186` `AfterSalesCreateScreen` |
| `NewCarFollowListScreen` 行 `:196`（先 `:195` `selectNewCarFollow`） | `HomeRoutes.NewCarFollowDetail` | Home | `:201-205` `NewCarFollowArchiveDetailScreen` |
| `NewCarFollowListScreen` 新建 `:198` | `HomeRoutes.NewCarFollowCreate` | Home | `:200` `NewCarFollowCreateScreen` |

> 只有 UsedCar / AfterSales / NewCarFollow 三条链路走「`store.select(id)` → 详情读 `selectedXxxId` → 查不到退回首条」；Ledger / DataAnalytics 两条**缺选中态**，点任何一行都渲染第一条。

#### 表 5-4 · 看起来能跳、其实断掉的入口

| 位置 | 现象 |
|---|---|
| `HomeSections.kt:138`（`HomeGreetingSection`「3 条新消息」pill） | 文本硬编码，pill **无可点 modifier** |
| `HomeSections.kt:218-284`（`HomeBannerSection`「立即体验」） | pill 只是 `Box` + 文本，**无 clickable** |
| `HomeSections.kt:576-609`（`DetailActionTile`） | 「详情」二字不可点，只有整卡 `onClick` |
| `HomeSections.kt:903-989`（`HomeNewsList`） | 行本身无 clickable，且宿主传 `onOpenAll = {}` |
| `HomeRouteHost.kt:525`（`LifeServiceScreen`「进入跟读视频专区」） | 应跳 `HomeRoutes.TransliterFeed`，实际只 `showPlatformToast` |
| `HomeRouteHost.kt:1201/1214/1218/1254/1285/1357/1380/1422`（`TransliterFeedScreen`） | 8 处 Toast；唯一真跳转是 `:1523` 的 `onOpenHotRank` |
| `StrategyScreen.kt:97-133`、`:295`、`:304` 附近 | 子 Tab 切换只改 `selectedTab` 不换内容；「订阅」是 `clickable { }` 空实现；「如何跟投」纯文本 |
| `HomeRouteHost.kt:782`（`ClubContentBody` 底部） | 「进入社区查看更多」只 Toast「社区」；真跳转只有 `:680` 的 `onJoin`，而 onJoin 又落到表 5-3 的 Tab-Toast |

### 沉浸式 / inset 合规

**结论：本模块一行 `ImmersiveInsets` 都不用。** 核验：`grep -rn "ImmersiveInsets\|shellContentInsets" feature/home/src/commonMain` → **0 命中**（`core/design/.../ImmersiveInsets.kt` 只在 `composeApp/.../AppShell.kt:42` 被 import）。底栏留白完全由 Shell 负责：

| 关注点 | 位置 | 说明 |
|---|---|---|
| 底栏高度常量 | `core/design/src/commonMain/kotlin/com.example.my_kmp_project/core/design/ImmersiveInsets.kt:23`（`MainBottomBarHeight = 49.dp`） | 本模块无引用 |
| inset 策略函数 | `ImmersiveInsets.kt:41-49` `Modifier.shellContentInsets(bottomBarVisible, bottomBarPadding)`：可见 → `padding(bottom=…)`，不可见 → `navigationBarsPadding()` | |
| 唯一调用点 | `composeApp/src/commonMain/kotlin/com.example.my_kmp_project/app/AppShell.kt:360-363`（`bottomBarVisible = showBottomBar`，定义 `:334-336`） | 首页与所有二级页共用这一个 Box 修饰符 |
| 可见性由谁决定 | `core/ui/.../MainTabChrome.kt:24-29` `ReportMainTabRoot(isRoot)` → `chrome.updateBottomBarVisible(isRoot)`（`:16-17` 是 `mutableStateOf(true)`） | 所以**本模块各屏只负责上报，不负责算 padding** |
| 上报值 | 首页根 `HomeScreen.kt:114` = `isRoot = true`，**其余 30 处一律 `false`**：`grep -rn "ReportMainTabRoot(isRoot" feature/home/src/commonMain` → 31 行调用（+8 行 import = 39 行命中）；`HomeRouteHost.kt` 独占 21 处 | **没有漏报屏**：只有 5 个薄壳自己不报（`LiveCommerceScreen:547`、`ClubScreen:553` 由 `ClubContentBody:638` 报；`FollowUpCustomersScreen:3271`、`AfterSalesAppointmentsScreen:3284`、`StoreReviewOrdersScreen:3297` 由 `TodoListScreen:3315` 报） |

**有没有裸 `TopAppBar`？没有。** 核验：`grep -rn "TopAppBar" feature/home/src/commonMain` 只命中 1 行注释 —— `AllServicesScreen.kt:115`「56.dp toolbar (not M3 64.dp CenterAlignedTopAppBar)」。本模块三种顶栏写法：

| 写法 | 位置 | 状态栏处理 |
|---|---|---|
| 自绘条 `AllServicesTopBar` | `AllServicesScreen.kt:115-151`（`statusBarsPadding()` `:122`，高 `FlutterToolbarHeight = 56.dp` `:127`，返回图标是 `ic_nav_back` `rotate(180f)` `:130-139`） | ✅ |
| 自绘条 `FlutterFeatureTopBar` | `HomeRouteHost.kt:559-609`（`statusBarsPadding()` `:570`） | ✅ |
| 复用 `core.ui.MineTopBar` | `LedgerScreens.kt:59` / `:84`、`HomeDetailScreens.kt:64` / `:136` / `:210`、`HomeRouteHost.kt:2194` / `:2482` / `:3317` / `:3490` | 由 core.ui 内部处理 |
| 搜索页自绘头 | `HomeSearchScreen.kt:167-271`（`statusBarsPadding()` `:180`） | ✅ |
| 其余 `statusBarsPadding()` | `HomeScreen.kt:190`（LazyColumn）、`HomeRouteHost.kt:1175`（跟读页头）、`:1759`（热榜页头） | ✅ |
| 悬浮件自带 | `LearningReportScreen.kt:130`（家长助手 chip）、`:147`（`MembershipBanner`）用 `navigationBarsPadding()` —— **全模块仅这两处** | 与 Shell 的 `navigationBarsPadding()` 叠加，见下 |

> ⚠️ **与 AGENTS.md 的张力**：AGENTS.md「Immersive / edge-to-edge」写明*不要在 DemoApp tab 内容外再包 `statusBarsPadding()`*（安全区由 Shell 统一给），但 `HomeScreen.kt:190` 的 `LazyColumn` 上就挂了 `.statusBarsPadding()`，`:185-187` 的注释解释是对齐 Flutter `safeTop + 16` 的刻意行为。`AllServicesScreen` / `HomeSearchScreen` / `HomeRouteHost` 三处自绘条同理（它们都在二级栈，Shell 只加了 `navigationBarsPadding()`，状态栏确实得自己躲）。**底栏方向不会重复**：二级页 `ReportMainTabRoot(false)` → `showBottomBar = false` → Shell 走 `navigationBarsPadding()` 分支（`ImmersiveInsets.kt:41-49`），不再叠 49.dp。
>
> ⚠️ **会重复的方向**：`LearningReportScreen.kt:130/147` 的 `navigationBarsPadding()` 叠在 Shell 已给的 `navigationBarsPadding()` 之上，二级页底部悬浮件在带手势条的设备上会被顶起两个 home-indicator 高度。

### 真实状态与坑

#### 数据源分类（`grep -rn "HomeMockData\." feature/home/src/commonMain/.../feature/home` 全量 32 行命中（含 `HomeFavoritesStore.kt` 的 5 处，屏层 27 处））

| 屏 | 数据来源 | 行号 | 可写吗 |
|---|---|---|---|
| 首页根 `HomeRootContent` | **`HomeMockData` 10 处**：`storeName` `HomeScreen.kt:118`、`searchPlaceholder` `:196`、`features` `:204`、`metricsYesterday/Month/Today` `:227-229`、`metricDetails` `:231`、`services` `:250`、`contacts` `:258`、`news` `:259` | 全部只读；唯一"可变"的是 `storeName` 被 `SwitchStoreDialog.onPicked` 改本地 state（`:151`），**门店列表本身仍来自 Mine 侧 mock** | ❌ |
| 首页 sections | `HomeSections.kt` **0 处** `HomeMockData` —— 数据全由参数注入 | — | ❌（受控组件） |
| `HomeSearchScreen` | 6 处（`:90/91/92/94/135/140`）：历史、发现、榜单、筛选标签、Tab 名 | 历史写进 `remember` 的本地 `history`（`:109`），**退出即丢**，无 KV | 会话内 |
| `AllServicesScreen` | 1 处（`:97` `catalogSections`） | 收藏是唯一真持久化：`HomeFavoritesStore.add/remove` + `snapshot()`（`:90/91/104/105`，底层 `core.platform.saveStringList`，key `home_favorite_service_ids_v2`） | ✅ KV |
| `StrategyScreen` | 3 处（`:82/157/178`）+ `StrategyPlanCard` 整卡写死字面量（`StrategyScreen.kt:219-315`，含 `"-11.35%"` `:264`） | — | ❌ |
| `LearningReportScreen` | 3 处（`:98/100/120`） | — | ❌ |
| `HomeRouteHost` 私有屏 | 4 处，只在跟读/热榜：`:1528`（`rankItemsForTab(0).take(3)`）、`:1731/1732`（分类与年龄筛选）、`:1737`（榜单条目） | — | ❌ |
| 二手车 / 售后 / 新车跟进 | **`HomeSecondaryStore`（内存 `mutableListOf` + `bump()`，无 KV）**：`usedCarOrders()` `:2587-2588`、`afterSalesDetails()` `:3042-3043`、`newCarFollows()` `:2177-2178`；新建走 `validateXxxCreate` + `createXxx`（`:2432/2444`、`:2894/2904`、`:2988/2995`） | 表单真校验、真插入列表 | 进程内 |
| 待办 4 屏 | 伙伴审核用 **`HomeTodoStore`**：`pendingApplications()` `:3193-3194`，`rejectJoin` `:3241` / `approveJoin` `:3257` 真删除条目 | 同样进程内 | 进程内 |
| 签到 / 积分 | **`HomePointsStore`（KV 落盘的积分态）**：`version` `:794`、`balance/streak/checkedInToday/todayReward` `:795-808`、`checkIn()` `:928` 与「领取」`:1071`；首页侧 `HomeScreen.kt:122/135-138/162-166/173`（`markDialogAcked`） | `checkIn()` 返回 `res.points` 供 Toast 文案（`HomeScreen.kt:166-169`），失败/重复签到返回 null 静默 | ✅ KV |

#### 坑（按影响面排序）

| # | 坑 | 证据 |
|---|---|---|
| 1 | **12 个文件里没有任何网络层**：`grep -rn "kotlinx.coroutines.flow\\|ViewModel\\|collectAsState\\|core.network\\|feature.home.api" feature/home/src/commonMain/.../feature/home/*.kt` → **0 命中**。整个 feature 的 `api/` 子包（`HomeTodoApiModels.kt` 等）只被 store 消费，屏层拿不到，页面数据 100% mock 或内存 store | 上面的 grep |
| 2 | `HomeSecondaryMock` **只有 3 个成员真的在用**：`ledger`（`HomeRouteHost.kt:158/163`）、`analyticsRecords`（`:171/2025`）、`analyticsTotal`（`:2026`）。`partners/followUps/appointments/orders/analytics/usedCars/afterSales/newCars` 以及两个代理 getter 全仓 0 引用：`grep -rn "HomeSecondaryMock" --include="*.kt" . \\| grep -v "/build/"` 共 7 行 = 5 处引用 + 定义行 + 对象声明。四个 Todo 屏的真实数据见上表 | 该 grep |
| 3 | **`DeferredDestinationStub` 在本模块 0 引用**：`grep -rn "DeferredDestinationStub" feature/home/src` 无命中（定义在 `feature/shell/.../DeferredDestinationStub.kt:25`）。本模块的"占位"风格不是它，而是 **Toast-only 屏**（`LifeServiceScreen` 全 4 处 Toast、`TransliterFeedScreen` 8 处、`ClubContentBody` 底部）与 **`CrudDetailScreen("未识别路由")` 兜底**（`HomeRouteHost.kt:206`） | 该 grep |
| 4 | **详情选不中**：`LedgerDetailScreen` 恒收 `ledger.firstOrNull()`（`:163`）、`AnalyticsDetailScreen` 恒收 `analyticsRecords.first()`（`:171`，**空列表会 `NoSuchElementException`**，`HomeSecondaryMock.ledger`/`analyticsRecords` 目前硬编码非空才没炸） | `HomeRouteHost.kt:163/171` |
| 5 | **`store.select` 是全局单例态**：`selectedUsedCarId` 等 3 个字段挂在 object 上（`HomeSecondaryStore.kt`），同一进程内 Mine/Home 混用会串；且 `?: xxx().first()` 的退回首条逻辑（`:153/189/203`）会**静默展示错单** | `HomeRouteHost.kt:152-153/188-189/202-203` |
| 6 | `DailyCheckInDialog.busy`（`DailyCheckInDialog.kt:49`）置 true 后**自己永不复位**，靠父级在 `onCheckIn` 里 `showCheckIn = false` 卸载（`HomeScreen.kt:170`）。任何"点完不关弹窗"的宿主（如 `NativeJetpackHome.kt:24`）都会让 CTA 永久禁用 | `DailyCheckInDialog.kt:148-151` |
| 7 | 自动签到用 `LaunchedEffect` 的 key 里**直接读 object 属性**（`HomeScreen.kt:128` `HomePointsStore.checkedInToday` / `.dialogAckDate`）；订阅则靠"裸读 `version`"（`:122`）。漏读 `version` 的屏不会随积分变化重组 —— `HomeRouteHost.kt:794` 是正确示范，`CheckInMallScreen` 之外的屏（如首页头部）没有 | `HomeScreen.kt:122/128` |
| 8 | **`flutterStyleGreeting` 硬编码 UTC+8**（`HomeScreen.kt:276-287`），与 `Clock.System.now().toString().take(10)`（`:131/160`，UTC 日期）不同源：海外时区下「早上好」与「今日是否已签到」的日期判定会差一天 | `HomeScreen.kt:131/160/276-287` |
| 9 | `HomeWebHandlers.GetEnvironment` 无论哪端都返回 `"platform":"android"`（`HomeWebHandlers.kt:31-33`），`GetUserInfo` 用字符串拼接 JSON（`:34-43`）—— **昵称含引号即产出非法 JSON**，且没有任何转义 | `HomeWebHandlers.kt` |
| 10 | `HomeWebHandlers.register()` 全仓唯一调用点是 `composeApp/src/androidMain/.../NativeAndroidMain.kt:90`：**iOS/OHOS 的 H5 bridge 四条 handler 全未注册**，`registered` 的 `@Volatile` 单例保护（`HomeWebHandlers.kt:12`）在这里等于没用到 | 该 grep |
| 11 | `FlutterToolbarHeight = 56.dp` 在两个文件里**重复声明**：`AllServicesScreen.kt:52` 与 `HomeRouteHost.kt:112`（同为文件级 private，不会冲突但值改一处漏一处） | 两处定义 |
| 12 | `HomeServiceAssets.featureAt(index)`（`HomeServiceAssets.kt:89-96`）已 `@Deprecated` 且对负 index 无保护；替代品 `featureForLabel(label)`（`:75-87`）未命中时回落 `ic_service_default`，`fromFlutterFile` 未命中返回 null 让 `ServiceGridCell` 走首字兜底砖块（`AllServicesScreen.kt:273-288`） | `HomeServiceAssets.kt` |
| 13 | `HomeAssetIcon`（`HomeServiceAssets.kt:112-125`）**没有 `modifier` 参数**，尺寸只能靠 `size: Dp = 44.dp`，无法外部加 padding/background —— 与其他 composable「`modifier` 放最后」的约定不一致（`HomeSections.kt` 全量遵守） | 签名对比 |
| 14 | `HomeSearchScreen`：`rotatingHint` 其实是**固定下标** `getOrElse(2)`（`:90`，命名撒谎、不轮播）；`searchRankCoverRes`（`:73-82`）与 8 张 rank 图（`:58-65`）**全为死代码**，72dp 封面是手绘 `Canvas` 播放三角（`:529-550`）；`SearchTagChip(softFill = false)`（`:439`）的 `softFill` 分支**无调用点传 true**；`onClear` 只清内存历史（`:121`） | grep 见「逐文件详解」 |
| 15 | `NewCarFollowArchiveDetailScreen` 的「提交」把输入框清空 + Toast「已记录跟进」（`HomeDetailScreens.kt:303-309`），但**不会 append 到 `logs`**（`:204-209` 硬编码）—— 用户看不到自己刚写的跟进；改意向等级 chip 也只改 `level` 本地 state（`:254-276`），回列表即丢 | `HomeDetailScreens.kt` |
| 16 | `AfterSalesCreateScreen.date` 写死 `"2026-09-25"`（`HomeRouteHost.kt:2867`），新建售后单的日期永远同一天；`UsedCarCreateScreen.kinds`（`:2364`）把 code 算出来后**没有使用**（只用 label） | `HomeRouteHost.kt` |
| 17 | `HomeMockData.ledger` 的日期集中在 2026-09-22~24（`HomeRouteHost.kt:261-286`），任何"按日期分组/排序"的 UI 都会显示成同一周；`analyticsRecords` 里 AN-0004 是全 0 行，`AnalyticsDetailScreen` 的分母用 `coerceAtLeast(1)` 兜住（`HomeDetailScreens.kt:185`），`InvestProgressBar` 只 `coerceIn(0f,1f)` **没有 total=0 保护**（`StrategyScreen.kt:319-343`） | 相应行 |
| 18 | 三个薄壳屏（`FollowUpCustomersScreen` `:3270`、`AfterSalesAppointmentsScreen` `:3283`、`StoreReviewOrdersScreen` `:3296`）把**函数内 `Triple<String,String,String?>` 字面量**塞给 `TodoListScreen`（`:3275-3278/3288-3290/3301-3302`）；`TodoListScreen` 的行**完全不可点**，`third` 只当徽章色用（`:3339-3349`） | `HomeRouteHost.kt` |
| 19 | `MetricTile` 用 `DemoColors.CanvasSoft` 作底（`HomeSections.kt:552`），同卡片其他块用 `CardBg`/`Surface` —— 与 `HomeSectionStyle` 自己声明的 token 组不一致，暗色主题下会跳色 | `HomeSections.kt:547-574` |
| 20 | 死代码汇总（核验方式见「HomeRouteHost 专题」末尾命令）：`AfterSalesDetailScreen`（`:2923`）、`NewCarFollowDetailScreen`（`:3011`）、`TodoForbiddenScreen`（`:3355`）、`TodoEmpty`（`:3413`）、`TodoCard`（`:3424`）、`CrudListScreen`（`:3480`）、`CrudCreateScreen`（`:3547`）、`searchRankCoverRes`（`HomeSearchScreen.kt:73`）、`HomeServiceAssets.featureAt`（已标 Deprecated）、`ReportColors.PlayBg`（`LearningReportScreen.kt:39-59` 内未被引用） | grep |

## 16C · feature:home 的图标与图表

> 定位说明：本分册覆盖 `:feature:home` 的**纯视觉资产层**——两组手写 `ImageVector` 图标库
> （`HomeIcons` 实心 / `HomeStrokeIcons` 描边）与 `AnalyticsCharts` expect/actual 三端图表。
> 数据与状态见 16A，屏幕编排见 16B；本分册对这些文件只留 grep 行号引用，不展开。
>
> 全部行号以仓库根为前缀 `feature/home/src/.../`（下表省略公共前缀
> `commonMain/kotlin/com.example.my_kmp_project/feature/home/`）。

### 16C.1 文件清单

| 路径（仓库相对） | 作用 | 规模/关键行号 |
|---|---|---|
| `feature/home/src/commonMain/.../home/HomeIcons.kt` | 10 个 Material 实心风 ImageVector（OHOS-safe，无 material-icons-extended） | 424 行；val 17–26，工厂 `solid()` 28–47 |
| `feature/home/src/commonMain/.../home/HomeStrokeIcons.kt` | 10 个描边风 ImageVector + 服务名→图标分发 | 252 行；val 26–53，`forServiceLabel` 56–65，工厂 `stroked()` 72–90 |
| `feature/home/src/commonMain/.../home/AnalyticsCharts.kt` | 图表 **expect** 契约（2 个 @Composable） | 32 行；expect 17–21 / 24–31 |
| `feature/home/src/chartsVicoMain/.../home/AnalyticsCharts.vico.kt` | Android+iOS actual：**纯 Canvas** 环图+柱图（Vico 已弃用，见 16C.5） | 156 行；ring 40–104，bars 106–156 |
| `feature/home/src/ohosMain/.../home/AnalyticsCharts.ohos.kt` | Harmony actual：**空白占位 Box** | 33 行；stub 15–21 / 24–32 |
| `feature/home/build.gradle.kts` | chartsVicoMain 中间源集接线 + `libs.vico.compose` 依赖 | 38 行；源集 14–23 |
| `feature/home/src/commonMain/composeResources/` | **未找到**（见 16C.6 实测命令） | — |

### 16C.2 HomeIcons —— 实心（fill）图标组

**组织方式**：不是资源引用，是**逐个手绘的 Material path 数据**。每个图标 = 一个
`private fun xxx() = solid { moveTo/curveTo/… }`，`solid()`（HomeIcons.kt:28–47）用
`ImageVector.Builder`（24×24dp、viewport 24f）生成**单条 fill 路径**：
`fill = SolidColor(Color.Black)`、`stroke = null`、`PathFillType.NonZero`（36–44 行）。
描边轮廓类图标（如 notifications_none）靠「外轮廓 + 反向内轮廓」在同一路径里挖洞。
public 项全部 `by lazy`（17–26 行），消费方以 `imageVector = HomeIcons.X` 传给 `Icon(...)`。

| public val（行号） | 私有构造器（行号） | 视觉语义（KDoc 注释） | 使用处（grep file:line） |
|---|---|---|---|
| `NotificationsNone` :17 | `notificationsNone()` :50 | 铃铛（`Icons.notifications_none_rounded`） | `HomeSections.kt:131` |
| `Search` :18 | `search()` :81 | 放大镜（`search_rounded`） | `HomeSections.kt:178` |
| `QrScan` :19 | `qrScan()` :106 | 扫码框（`qr_code_scanner_rounded`） | `HomeSections.kt:207` |
| `GridView` :20 | `gridView()` :226 | 四宫格（`grid_view_rounded`） | `HomeScreen.kt:244`（投资策略入口卡）、`composeApp/src/androidMain/.../nativeshell/NativeJetpackHome.kt:182` |
| `Analytics` :21 | `analytics()` :250 | 柱状图框（`analytics_outlined`） | `HomeScreen.kt:264`（学习报告入口卡）、`NativeJetpackHome.kt:201` |
| `PersonOutline` :22 | `personOutline()` :285 | 人形（`person_outline`） | `HomeSections.kt:858` |
| `ChatBubble` :23 | `chatBubble()` :313 | 聊天气泡（`chat_bubble_outline`） | `HomeSections.kt:876`（"chat" 分支）、`HomeStrokeIcons.kt:61`（"话术库"） |
| `Phone` :24 | `phone()` :334 | 听筒（`phone_outlined`） | `HomeSections.kt:877`（"phone" 分支） |
| `ChevronRight` :25 | `chevronRight()` :367 | 右箭头（`chevron_right_rounded`） | `HomeSections.kt:392, 661, 705, 878, 931` |
| `Article` :26 | `article()` :382 | 资讯（`article_outlined`） | `HomeSections.kt:979` |

数量统计（`grep -c "val .*: ImageVector by lazy"`）：**10 个 public val + 10 个 private 构造器 + 1 个 `solid()` 工厂**。
10 个 val 全部有消费处（Android `NativeJetpackHome` 是 commons 图标跨进 app 层的例子）。

### 16C.3 HomeStrokeIcons —— 描边（stroke）图标组

**为什么与 HomeIcons 分家**（文件头 KDoc 12–23 行，原文归纳）：
1. 描边图标直接 `stroke` 画开放路径，绕开 NonZero「外轮廓+反向内轮廓」挖洞问题，24×24 视口下像素可控；
2. Flutter SoT 首页服务九宫格/门店行用的是 outlined 变体，实心 Material 视觉偏重、与截图对不上；
3. 约束同 HomeIcons：全部手写 ImageVector，不引入 material-icons-extended，保证 ohosArm64/ohosX64 可编译。

**构造器**：`stroked()`（72–90 行）—— `fill = null`、`stroke = SolidColor(Color.Black)`、
线宽常量 `STROKE = 1.7f`（69 行）、`StrokeCap.Round`/`StrokeJoin.Round`/`strokeLineMiter = 4f`。
另有 `PathBuilder.roundRect(l,t,r,b,rad)` 扩展（93–104 行）：描边路径没有 arcTo 简写，
用四段 `curveToRelative` 以对角近似圆角半径。

| public val（行号） | 私有构造器（行号） | 对齐的 Flutter 图标 | 使用处（file:line） |
|---|---|---|---|
| `Storefront` :26 | `storefront()` :108 | `storefront_rounded`（遮阳棚+波浪+店体+门） | `HomeSections.kt:464`（门店选择行） |
| `ArrowDown` :29 | `arrowDown()` :137 | `keyboard_arrow_down_rounded` | `HomeSections.kt:482` |
| `SwapHoriz` :32 | `swapHoriz()` :143 | `swap_horiz_rounded`（换门店） | `HomeSections.kt:489` |
| `PhotoCamera` :35 | `photoCamera()` :156 | `photo_camera_outlined`（roundRect 机身+镜头） | 仅经 `forServiceLabel` "朋友圈" :57 |
| `Videocam` :38 | `videocam()` :173 | `videocam_outlined` | 仅 "视频号" :58 |
| `LiveTv` :41 | `liveTv()` :181 | `live_tv_outlined`（信号点+天线） | 仅 "直播" :59 |
| `PhotoLibrary` :44 | `photoLibrary()` :197 | `photo_library_outlined`（双卡+山水线） | 仅 "素材库" :60 |
| `School` :47 | `school()` :213 | `school_outlined`（学位帽+流苏） | 仅 "培训" :62 |
| `Insights` :50 | `insights()` :231 | `insights_outlined`（折线+箭头+基线） | 仅 "竞品分析" :63 |
| `Apps` :53 | `apps()` :246 | `apps_rounded`（四个 roundRect） | `forServiceLabel` else 分支 :64 |

**唯一的分发 API**：`fun forServiceLabel(label: String): ImageVector`（56–65 行），
when 覆盖 朋友圈/视频号/直播/素材库/话术库(→`HomeIcons.ChatBubble`)/培训/竞品分析，else→`Apps`。
消费处在 `HomeSections.kt:775`（44dp 圆角瓦片、accent@12% 底，服务九宫格 tile）。

数量统计：**10 个描边 val + 10 个 private 构造器 + 1 个 `stroked()` + 1 个 `roundRect()` 扩展 + 1 个 `forServiceLabel`**。
`HomeSections.kt` 直接点名的只有 3 个 val（Storefront :464 / ArrowDown :482 / SwapHoriz :489）
加 `forServiceLabel`（:775）；其余 7 个 val（PhotoCamera/Videocam/LiveTv/PhotoLibrary/School/
Insights/Apps）只经分发器 when 间接消费。

#### public 签名总览（两组图标）

`HomeIcons.kt:17–26` 全部同构：`val X: ImageVector by lazy { x() }`，共 10 项
（NotificationsNone / Search / QrScan / GridView / Analytics / PersonOutline / ChatBubble /
Phone / ChevronRight / Article）。`HomeStrokeIcons.kt:26–53` 同构 10 项（Storefront /
ArrowDown / SwapHoriz / PhotoCamera / Videocam / LiveTv / PhotoLibrary / School / Insights /
Apps）+ 唯一函数 `fun forServiceLabel(label: String): ImageVector`（:56）。类型均为
`androidx.compose.ui.graphics.vector.ImageVector`；两对象对外只有这 21 个符号，
没有任何带参 composable 或资源 id。

#### 描边几何要点（供改图标者，行号=HomeStrokeIcons.kt）

| 图标 | 分段结构 | 备注 |
|---|---|---|
| `storefront` | 遮阳棚 :110–115 → 棚下波浪 curveToRelative :117–124 → 店体 :126–129 → 门 :131–134 | 波浪以 `c(0,1.4,1.1,2.2,2.4,2.2)` 单元重复 |
| `photoCamera` | roundRect 机身 :158 → 取景凸台 :160–163 → 镜头 4 段 curve :165–170 | 机身 r=2.6f；镜头圆心约 (12,13.2)、半径 4f |
| `videocam` | roundRect 机身 :174 → 右侧梯形镜头 :175–178 | 机身右缘 15.5f，梯形外扩到 21.5f |
| `liveTv` | roundRect 屏体 :182 → 信号点 :184–189 → 双天线 :191–194 | 天线顶点 y=4.4f 高于屏体上沿 7.5f |
| `photoLibrary` | 后卡两边（不闭合）:199–202 → 前卡 roundRect :204 → 山水折线 :206–210 | 后卡故意不闭合，模拟叠卡遮挡 |
| `school` | 帽面平行四边形 `close()` :215–219 → 袍体 :221–225 → 流苏 :227–228 | 帽面 moveTo/lineTo×4+close |
| `insights` | 折线 :233–236 → 右上箭头 :238–240 → 基线 :242–243 | 与 HomeIcons.Analytics（实心柱）刻意区分 |
| `apps` | 4 个 roundRect :247–250 | r=1.6f，四角对齐 3.2f/14.8f 网格 |

### 16C.4 AnalyticsCharts —— expect 契约与三端矩阵

**expect 契约**（commonMain/AnalyticsCharts.kt，32 行，仅注释+两个 expect）：

| 签名（行号） | 参数语义 |
|---|---|
| `expect fun AnalyticsConversionRing(rate: Float?, modifier: Modifier = Modifier, size: Dp = 64.dp)` :16–21 | `rate` = **0..1 分数**（可空）。actual 端校验 `!= null && isFinite && >= 0`，再 `coerceIn(0,1)`；非法值环为空轨+中央显示 `—`（vico.kt:46–48） |
| `expect fun AnalyticsMiniBars(pv: Float, uv: Float, clicks: Float, converts: Float, modifier: Modifier = Modifier, height: Dp = 64.dp)` :23–31 | 4 根柱 = PV/UV/点击/转化；归一化基准是 **`pv.coerceAtLeast(1f)`**（vico.kt:118），即 PV 柱恒撑满高度 |

文件头 KDoc :8–14 说明这是对 Flutter `wys_chart` 列表 tile 的替身（stand-in）。
expect 声明逐字（:16–31）：

```kotlin
@Composable expect fun AnalyticsConversionRing(
    rate: Float?, modifier: Modifier = Modifier, size: Dp = 64.dp)
@Composable expect fun AnalyticsMiniBars(
    pv: Float, uv: Float, clicks: Float, converts: Float,
    modifier: Modifier = Modifier, height: Dp = 64.dp)
```

唯一消费方：`HomeRouteHost.kt:2118`（ring，size=64.dp）、`:2120–2126`（bars，
`Modifier.weight(1f).height(64.dp)`）—— 报表列表行「环+四柱」组合；:2017 注释同样写明
"Canvas ≈ wys_chart; CPF 无 Vico/KoalaPlot"。

#### chartsVicoMain 环图几何（AnalyticsCharts.vico.kt:40–104）

| 步骤 | 行号 | 实现 |
|---|---|---|
| 合法性 | :46–48 | `valid = rate != null && isFinite && >= 0`；`clamped = coerceIn(0f,1f)`；文案 `"${round(clamped*100)}%"` 或 `—` |
| 线宽 | :49/:57–58 | 轨道 7.dp，值弧 7.dp+2.dp（Butt cap），值弧比轨道粗 2dp |
| 直径/居中 | :59–63 | `diam = (minDimension - valueStroke.width).coerceAtLeast(1f)`，topLeft 双向居中 |
| 轨道弧 | :65–73 | 从 `-90° + 360°*clamped` 起画剩余 `360°*(1-clamped)`（下限 0.0001°，故 100% 时轨道几乎不可见） |
| 值弧 | :74–84 | 仅 valid 时画：-90° 起、sweep `360°*clamped` 下限 1°（0% 也留 1° 小头） |
| 中央文案 | :86–102 | 百分比 ExtraBold、字号 `size*0.18` sp；「转化率」SemiBold、`size*0.11` sp；行间距 `size*0.02` dp |

色板常量 :29–33：`ChartInk #171717 / ChartLink #0070F3 / ChartAccent #F5A623 / ChartGrid #EBEBEB / ChartMute #888888`。

**三端矩阵**：

| 源集 | 服务对象 | actual 实现 | 画出什么 |
|---|---|---|---|
| `chartsVicoMain` | androidMain + iosMain（build.gradle.kts:21–23） | 纯 Compose `Canvas`（drawArc/drawRoundRect），**零 Vico API 调用** | 环图：轨道弧 `ChartGrid`(#EBEBEB) :65–73 + 值弧 `ChartInk`(#171717) :74–84，线宽 7dp(+2dp 值弧) :49/:57；中央 `${round(rate*100)}%` + 「转化率」两行文字，字号按 size 的 18%/11% 缩放 :86–102。柱图：宽 10dp、圆角 4dp 的 `drawRoundRect` 四柱 :126–139，色序 `[#171717, #0070F3, #F5A623, #0070F3]` :117，等分槽位居中 :127/:132，下方 9sp 标签 PV/UV/点/转 :115/:143–154 |
| `ohosMain`（含 ohosArm64 + ohosX64） | 两端 OHOS target | `Box(modifier.size(size))` :20、`Box(modifier.height(height))` :32 | **什么都不画**——纯空白占位，仅保住布局尺寸使编译与排版通过（KDoc :10–13："CPF-KMP-CMP 三方库清单尚无 Vico / KoalaPlot…先留空占位"） |

**为什么拆中间源集**（对应 AGENTS.md 的 OHOS 限制模式）：
Vico（`com.patrykandpatrick.vico:multiplatform`）只出 Android/iOS klib/AAR，CPF 适配线没有
ohos 产物——放进 `commonMain` 会直接毒死 OHOS 编译。所以 `chartsVicoMain` 只挂
`androidMain`（build.gradle.kts:21）和 `iosMain`（:23，`matching{}` 惰性挂载，因为
`-PandroidOnly=true` 时 iosMain 不存在，见 :22 注释）；`ohosMain` 保持
`dependsOn(commonMain)`（根 build.gradle.kts:124–126），不会传递拿到 chartsVicoMain，
由 ohosMain 自己提供占位 actual。这与 AGENTS.md 表格中 `networkKtorMain`/`imageCoilMain`/
`uuidCpfMain` 是同一套路数，只是 chartsVicoMain 是 **feature:home 模块私有**源集，不在 AGENTS.md 的全局表里。

### 16C.5 build.gradle.kts 与版本目录核对

| 事实 | 出处 |
|---|---|
| `chartsVicoMain` 依赖仅一条：`implementation(libs.vico.compose)` | feature/home/build.gradle.kts:18 |
| 别名 `vico-compose` = `com.patrykandpatrick.vico:multiplatform`，`version.ref = "vico"` | gradle/libs.versions.toml:107 |
| `vico = "2.3.0"` | gradle/libs.versions.toml:46 |
| 目录注释明确"CPF 无 ohos 适配 — keep off commonMain/ohosMain" | gradle/libs.versions.toml:44 |
| 建源集注释："Vico 传递的是 stock Compose 1.9.2；工程自己的 compose.* 由 commonMain 胜出" | feature/home/build.gradle.kts:17 |
| **但当前源码零处 import Vico**：`grep -rn "import com.patrykandpatrick" --include="*.kt" .`（排除 build/）→ 无输出（exit=1） | 实测，见 16C.6 |

即：**依赖仍声明、名字仍叫 vico，实现已整体退化为 Canvas**——vico.kt:36–38 给出退化原因：
"Vico multi-series clusters all rods into one x-group and blows MSE"（Vico 多序列柱会把 4 根杆
聚进同一 x 组，视觉误差爆炸），于是 bars 也改用 Canvas；commonMain KDoc :11 仍写着
"Vico multiplatform 2.3 (bars) + Canvas ring"，属于**过期注释**（详见 16C.7 坑 1）。

### 16C.6 资源目录规模实测

`feature/home` **没有任何 composeResources**：commonMain 下只有 `kotlin/` 一个目录，
本模块的"资产"就是上述代码内 ImageVector。执行命令与输出：

```
$ ls feature/home/src/commonMain/
kotlin
$ find feature/home/src/commonMain/composeResources -type f
find: feature/home/src/commonMain/composeResources: No such file or directory
$ find feature/home -type d -name composeResources        # 全模块范围
（无输出）
$ find feature/home -type f \( -name "*.png" -o -name "*.jpg" -o -name "*.webp" \
    -o -name "*.ttf" -o -name "*.otf" -o -name "*.svg" \)
（无输出）
```

对照（AGENTS.md 约定资源集中在 `:core:design`）：

```
$ find core/design/src/commonMain/composeResources -type f | awk -F. '{print $NF}' | sort | uniq -c
   2 jpg
 168 png
   1 ttf
$ find core/design/src/commonMain/composeResources -type f | wc -l
171
```

目录结构为 `core/design/src/commonMain/composeResources/{drawable,font}/`（png 168 + jpg 2 +
ttf 1，共 171 文件）。home 需要的位图都从这里取，不落地在自己的模块里。

### 16C.7 真实状态与坑

1. **"vico" 名不副实**：源集名、文件名（AnalyticsCharts.vico.kt）、commonMain KDoc
   （AnalyticsCharts.kt:11 "Vico … (bars)"）、build.gradle.kts 头注释（:1 "Vico 图表"）、
   以及 `libs.vico.compose` 依赖（build.gradle.kts:18）都指向 Vico，但代码实际零 Vico 调用
   （16C.5 实测 grep）。依赖目前是**死重量**：仍参与解析、仍传递 stock Compose 1.9.2 进
   android/ios 编译（:17 注释解释过冲突如何被 commonMain 仲裁压住）。删除前需确认无人计划回归 Vico。
2. **Harmony 图表区为空白**：ohosMain 两个 actual 是纯 `Box`（`AnalyticsCharts.ohos.kt:20`、`:32`），
   报表列表行在鸿蒙上只剩文字/状态胶囊（HomeRouteHost.kt:2114–2127 的行内布局），环与柱不渲染。
   占位只保尺寸，无任何"降级绘简图"逻辑。
3. **柱图归一化基准写死在 PV**：`maxY = pv.coerceAtLeast(1f)`（vico.kt:118），若业务数据
   uv>pv 会超出后 `coerceIn(0,1)` 截顶（:131），4 柱恒等高的场景需要注意。
4. **环图 0% 与非法值视觉不同但都近空**：`rate=0f` 合法（`valid=true`），值弧仍画 `coerceAtLeast(1f)`
   度（:78），显示 "0%"；`rate=null/NaN/负` 才显示 "—"（:48）。调用方语义要传分数不是百分数。
5. **挖洞 vs 描边两套 fill 规则并存**：HomeIcons 的 NonZero 反向内轮廓（如 :56–77 铃铛）与
   HomeStrokeIcons 的开放 stroke 路径（roundRect 近似 :93–104）不可混用；改图标时先判断该进哪个对象，
   判据即文件头 KDoc（HomeStrokeIcons.kt:12–23）。
6. **颜色全部硬编码**：图表 5 色（vico.kt:29–33）与描边/填充黑 `Color(0xFF000000)` 均未走
   `DemoColors` token（AGENTS.md 建议优先 token；这里是例外，改动需同步 Flutter SoT 色值）。
7. **forServiceLabel 跨对象引用**："话术库" 返回 `HomeIcons.ChatBubble`（HomeStrokeIcons.kt:61），
   是唯一一个实心图标从描边分发器流出的路径；字符串匹配服务名，改名即失配（fallback Apps :64）。
8. **chart 文案写死中文**（"转化率" vico.kt:96、标签 PV/UV/点/转 :115），无字符串资源层，
   与本模块其余屏幕的硬编码文案风格一致，非本分册引入的问题。

---
*校验方式：以上每条行号均来自对 5 个源文件整读 + 对消费方（HomeScreen/HomeSections/HomeRouteHost/
NativeJetpackHome）的 grep 行号引用；未通读消费方全文。*




## 16.99 三册口径冲突与统一验收

三册各自的「未验证疑点」原文保留在各节末尾，跨册汇总如下：

1. `api/` 里 6 个 API 类在生产代码零调用（16A 以 grep 实证）——契约实现已完成、UI 尚未接线，不是缺实现。
2. iOS 与 OHOS 的二级路由分发由 common 链路推断（16B 未见 swift/ets 侧引用），需实机确认。
3. 图表依赖是死重量：`chartsVicoMain` 已接线但代码里没有任何 Vico 调用（16C.5 grep 实测），OHOS 两个 actual 只剩 `Box`。
4. 三册重叠处以各自 `file:line` 为准（16B 已把 4 行误标的 Todo 数据源改回真实来源）。

机械验收（`_verify/verify_docs.py`）：`feature/home` 范围内 44 个文件全部被写到、无缺失目录、每条 `path:line` 引用可解析且行号不越界。

# Deep dive 20 · feature/commerce · wallet · pay · ai

> 范围：`feature/commerce/`（7 kt）、`feature/wallet/`（3 kt）、`feature/pay/`（3 kt）、`feature/ai/`（3 kt）+ 四份 `build.gradle.kts`。
> 证据来源：仅本仓库代码，行号为写入时快照。所有路径为仓库相对路径。
> 方法：整篇阅读上述 20 个文件；对外部模块（`core/network`、`core/router`、`component`、`composeApp`）只做定向 grep，不整篇通读。

## 1. 四模块定位 + 依赖实证

| 模块 | 定位 | Gradle 插件 | commonMain 依赖（行号） |
|------|------|-------------|--------------------------|
| `:feature:commerce` | 商城（商品/订单）+ 会员售卖页，共享 Compose + HTTP | `kotlinMultiplatform` `androidLibrary` `composeMultiplatform` `composeCompiler` `kotlinSerialization`（`feature/commerce/build.gradle.kts:5-10`） | `api(projects.core.ui)` `:17`、`api(projects.core.network)` `:18`、`api(projects.component)` `:20`、`api(libs.kotlinx.serialization.json)` `:22`；commonTest `implementation(projects.core.network)` `:25` |
| `:feature:wallet` | 钱包/卡包 HTTP 层，纯网络 + 序列化模型，**零 @Composable** | `kotlinMultiplatform` `androidLibrary` `kotlinSerialization`（`feature/wallet/build.gradle.kts:6-9`；注释 `:3-4` 明确不挂 compose 插件的原因） | `api(projects.core.network)` `:15`、`api(libs.kotlinx.serialization.json)` `:17`；commonTest `implementation(projects.core.network)` `:20` |
| `:feature:pay` | 支付网关与会员支付契约，纯 HTTP + 序列化模型，**零 @Composable** | `kotlinMultiplatform` `androidLibrary` `kotlinSerialization`（`feature/pay/build.gradle.kts:6-9`；注释 `:3-4` 同上） | `api(projects.core.network)` `:15`、`api(libs.kotlinx.serialization.json)` `:17`；commonTest `implementation(projects.core.network)` `:20` |
| `:feature:ai` | AI 流式对话页 + SSE 接口层，共享 Compose | `kotlinMultiplatform` `androidLibrary` `composeMultiplatform` `composeCompiler`（`feature/ai/build.gradle.kts:4-9`；无 serialization 插件——注释 `:2-3` 说明 SSE 编解码住在 `:core:network`） | `api(projects.core.ui)` `:14`、`api(projects.core.network)` `:15`、`api(projects.core.router)` `:16`、`api(libs.kotlinx.serialization.json)` `:18`；commonTest `implementation(projects.core.network)` `:21` |

依赖注释原文要点：

- `feature/commerce/build.gradle.kts:16` —「core:ui 传递带出 core.platform -> core.design / core.account + compose.*」。
- `feature/commerce/build.gradle.kts:19` —「component.pay（PayGateway / FlaggedPayGateway）」。
- `feature/commerce/build.gradle.kts:21`、`feature/wallet/build.gradle.kts:16`、`feature/pay/build.gradle.kts:16` —「JsonElement 出现在公开模型签名里 → api；:core:network 只 implementation」。
- `feature/ai/build.gradle.kts:2-3` —「core.sse（AiStreamEvent / SseParser / AiSseDecoder）住在 :core:network 模块里」。
- 四个模块都未声明 `:core:router` 依赖，**仅 `:feature:ai` 例外**（`feature/ai/build.gradle.kts:16`）。

## 2. 文件清单表

路径前缀（省略以便对齐，实际目录名含点号）：`SRC = <module>/src/commonMain/kotlin/com.example.my_kmp_project/feature/<module>`、`TST = <module>/src/commonTest/kotlin/com.example.my_kmp_project/feature/<module>`。

| 路径 | 作用 | 关键行号 |
|------|------|----------|
| `feature/commerce/build.gradle.kts` | 模块装配（5 插件 + 4 条 api 依赖） | `:5-10` 插件、`:17-22` 依赖、`:31` namespace |
| `feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MallApi.kt` | 商城 HTTP 契约层（10 个 suspend 方法）+ 路径常量 + 统一错误闸门 | `:27` class、`:192-201` 常量、`:208-217` status 域、`:223` 纯投影、`:231-248` `MallApiPaths`、`:254-260` `requireData` |
| `feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MallProductModels.kt` | 商品卡 / 详情 / SKU DTO + 客户端派生字段 | `:26-94` `MallProductCardModel`、`:97` `fixedOneDecimal`、`:109` `MallProductPayload`、`:118-154` `MallSkuOffer`、`:157-168` `MallProductDetailResponse`、`:174` `renderJsonScalar` |
| `feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MallOrderModels.kt` | 订单行 / 主体 / 详情 / 列表行 + 状态文案 + 时间/倒计时 | `:19-32` `MallOrderItem`、`:39-53` `MallOrderHead`、`:63-92` `MallOrderDetailResponse`、`:96-122` `MallOrderListRow`、`:125-132` `mallOrderStatusLabel`、`:144-147` `parseMallTime`、`:153-160` `formatPayCountdown` |
| `feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MallRequests.kt` | 写操作请求体 + 幂等键 | `:20-23` 加购、`:27-30` 行项、`:39-46` 下单、`:50-52` 支付、`:59-61` 下单响应、`:68-71` `nowMicros`、`:80-81` `mallIdempotencyKey` |
| `feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MembershipCatalog.kt` | 会员售卖页**硬编码**目录 + 设计 token（无网络） | `:5-8` tier、`:10-28` 三个 plan/promo/feature 数据类、`:31-62` `MembershipTokens`、`:64-142` `MembershipCatalog` |
| `feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MembershipScreen.kt` | 会员页（Flutter SoT 整图 + 两个热区），CTA 走 PayGateway | `:40-43` 签名、`:46` 渠道、`:52-57` 背景图、`:69-92` CTA 热区、`:94-101` 协议热区 |
| `feature/commerce/src/commonTest/kotlin/com.example.my_kmp_project/feature/commerce/MallApiContractTest.kt` | 22 条契约回归 + 共享 `FakeApiClient` | `:27` class、`:32-400` 用例、`:404-468` JSON 夹具、`:472-563` `FakeApiClient` |

| `feature/wallet/build.gradle.kts` | 纯 HTTP 模块装配（**无 compose 插件**） | `:6-9` 插件、`:3-4` 不挂 compose 的原因、`:15-17` 依赖、`:26` namespace |
| `feature/wallet/src/commonMain/kotlin/com.example.my_kmp_project/feature/wallet/WalletApi.kt` | 钱包 HTTP 契约层（7 方法）+ 路径常量 + 第三份 `requireData` | `:27` class、`:31/:43/:63/:76/:98/:107/:117` 七个端点方法、`:127` `LEDGER_LIMIT`、`:132-141` paths、`:144-150` `requireData` |
| `feature/wallet/src/commonMain/kotlin/com.example.my_kmp_project/feature/wallet/WalletModels.kt` | 汇总 / 卡 / 流水 / 充值 DTO + 分↔元派生 | `:16-20`、`:27-36`、`:43-70`、`:74-78`、`:85-90`、`:100-104` |
| `feature/wallet/src/commonTest/kotlin/com.example.my_kmp_project/feature/wallet/WalletApiContractTest.kt` | 13 条契约回归 + `FakeWalletClient` | `:22` class、`:27-206` 用例、`:210-251` 夹具、`:255-346` fake |
| `feature/pay/build.gradle.kts` | 纯 HTTP 模块装配（**无 compose 插件**） | `:6-9` 插件、`:3-4` 原因注释、`:15-17` 依赖、`:26` namespace |
| `feature/pay/src/commonMain/kotlin/com.example.my_kmp_project/feature/pay/PayApi.kt` | 会员支付 HTTP + 两步买断编排 + 三处 GAP(R5) | `:37` class、`:41/:50/:63/:79/:112` 五个端点、`:153-192` `payBuyout`、`:215-237` 结论类型、`:240-246` paths、`:252-256` Map 折变、`:263-269` `requireData` |
| `feature/pay/src/commonMain/kotlin/com.example.my_kmp_project/feature/pay/MembershipModels.kt` | tier / 渠道枚举 + 目录、买断、验单 DTO | `:19-32`、`:41-51`、`:61-81`、`:85-90`、`:100-113`、`:123-143`、`:155-201` |
| `feature/pay/src/commonTest/kotlin/com.example.my_kmp_project/feature/pay/MembershipPayContractTest.kt` | 18 条契约回归 + `FakePayClient` | `:22` class、`:27-344` 用例、`:353-384` 夹具、`:388-479` fake |
| `feature/ai/build.gradle.kts` | AI 屏装配（compose 插件 + 三条 core api） | `:4-9` 插件、`:2-3` SSE 住在 `:core:network` 的说明、`:14-18` 依赖、`:27` namespace |
| `feature/ai/src/commonMain/kotlin/com.example.my_kmp_project/feature/ai/AiSseApi.kt` | SSE 组包 + 帧→领域事件（唯一端点 #96） | `:27` class、`:32` `COMPLETIONS_PATH`、`:51-67` `streamCompletion`、`:74-85` `completionsRequestBody` |
| `feature/ai/src/commonMain/kotlin/com.example.my_kmp_project/feature/ai/AiStreamScreen.kt` | AI 页（SoT 整图 + 3 个 mock toast 热区） | `:39` 签名、`:46-51` 整图、`:52-61` 返回、`:63-70` 快捷问、`:72-79` 发送 |
| `feature/ai/src/commonTest/kotlin/com.example.my_kmp_project/feature/ai/AiSseApiTest.kt` | 6 条流协议回归 + `FakeSseTransport` | `:24` class、`:28-54` fake、`:57-185` 用例 |
| `component/src/commonMain/kotlin/com.example.my_kmp_project/component/pay/PayGateway.kt` | （外部端口，为第 5 节实证）支付接口 / 结果 / flag / 沙箱适配器 | `:4-7`、`:9-12`、`:14-27`、`:33-38`、`:48-52`、`:64-69`、`:83-94`、`:101-129` |
| `component/src/androidMain/kotlin/com.example.my_kmp_project/component/pay/PlatformPayAdapters.android.kt`、`…/iosMain/kotlin/com.example.my_kmp_project/component/pay/PlatformPayAdapters.ios.kt`、`…/ohosMain/kotlin/com.example.my_kmp_project/component/pay/PlatformPayAdapters.ohos.kt` | （外部）三端 SDK 注入点 | Android `:7-10`/`:16-19`、iOS `:4`/`:7`、OHOS `:7`/`:9`（全为 null） |


## 3. 逐文件逐函数

### 3.1 feature/commerce

#### 3.1.1 `MallApi.kt`

`public class MallApi(public val client: ApiClient = NetworkFacade.api())`（`feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MallApi.kt:27-29`）。默认 client 取 `NetworkFacade.api()`（`core/network/src/commonMain/kotlin/com.example.my_kmp_project/core/network/NetworkFacade.kt:34`）。类级语义（`:18-26`）：业务码非 0 或 `data` 缺失一律**抛** `NetworkError.Business`，UI lane 只消费返回值。

| 成员（file:line） | 完整签名 | 参数语义 | 返回 | 内部真实行为 / 副作用 |
|---|---|---|---|---|
| `feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MallApi.kt:33-43` | `suspend fun fetchShelfPage(storeId: Int, page: Int, size: Int = PAGE_SIZE): ListPage<MallProductCardModel>` | `storeId` 店铺、`page` 1-based、`size` 页长默认 10 | 分页壳 | `GET /api/v1/mall/stores/{storeId}/products?page&size`（`client.getListPage`，path 由 `MallApiPaths.products` `:234` 生成，query 由 `pageQuery` `core/network/src/commonMain/kotlin/com.example.my_kmp_project/core/network/ListPage.kt:99-103` 构造）→ `requireData("加载商品失败")` |
| `feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MallApi.kt:46-54` | `suspend fun fetchDetail(storeId: Int, productId: String): MallProductDetailResponse` | `productId` 为**字符串**（与 #27 的 `id` 对齐） | 详情 | `GET …/products/{productId}`（`MallApiPaths.productDetail` `:236`）→ `requireData("加载详情失败")` |
| `feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MallApi.kt:57-60` | `suspend fun addToCart(skuId: Int, qty: Int): Unit` | SKU 与数量 | 无（响应丢弃） | `NetworkJson.encodeToString(MallAddToCartRequest)` `:58` → `requireRawMapPost(CART, body, "加入购物车失败")` `:59`；`POST /api/v1/mall/cart` |
| `feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MallApi.kt:69-95` | `suspend fun createOrder(storeId: Int, skuId: Int, qty: Int, receiverName: String = "", receiverPhone: String = "", receiverAddress: String = ""): Int` | 三件套默认空串且**总发** | `order_id` | `POST /api/v1/mall/orders`，body 见 `MallCreateOrderRequest`；`idempotencyKey = mallIdempotencyKey(skuId)` `:80`（每次新微秒）；`lines` 恒为**单行** `:82`；`order_id <= 0` → `throw NetworkError.Business(NetworkCodes.PASSTHROUGH, "下单失败：缺少订单号")` `:90-93`（`PASSTHROUGH = -1`，`core/network/src/commonMain/kotlin/com.example.my_kmp_project/core/network/NetworkCodes.kt:32`） |
| `feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MallApi.kt:100-118` | `suspend fun buy(storeId, skuId, qty, paymentChannel: Int, receiver*: String = ""): String` | `paymentChannel` int 渠道码 | `orderNo` | **两次调用**：`createOrder(...)` `:109` → `payOrder(orderId, paymentChannel).orderNo` `:117`。无本地事务/补偿；第二步失败则第一步的单已存在 |
| `feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MallApi.kt:127-137` | `suspend fun fetchOrders(page: Int, size: Int = PAGE_SIZE, status: String? = null): ListPage<MallOrderListRow>` | `status` 是逗号串不是数组 | 分页壳 | `GET /api/v1/mall/orders?page&size&status`；`status` null/空串被 `queryOf` 丢弃（`core/network/src/commonMain/kotlin/com.example.my_kmp_project/core/network/ListPage.kt:92-93`）；fallback「加载订单失败」 |
| `feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MallApi.kt:140-143` | `suspend fun fetchOrderDetail(orderId: Int): MallOrderDetailResponse` | 订单 int id | 详情 | `GET /api/v1/mall/orders/{orderId}`；fallback「加载订单详情失败」 |
| `feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MallApi.kt:146-152` | `suspend fun payOrder(orderId: Int, paymentChannel: Int): MallOrderDetailResponse` | 渠道 int | 详情（同 #32 形状） | `POST /api/v1/mall/orders/{orderId}/pay`，body `{"payment_channel":n}`；fallback「支付失败」。**纯 HTTP 标记支付，不触任何 SDK** |
| `feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MallApi.kt:158-160` | `suspend fun cancelOrder(orderId: Int): Unit` | 订单 id | 无 | `requireRawMapPost(orderCancel(orderId), body = "", "取消订单失败")` —— **POST 且空 body**（`:154-157` KDoc 明示不是 PATCH/DELETE） |
| `feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MallApi.kt:171-182` | `suspend fun fetchPointsGifts(storeId: Int = 1, page: Int = 1, size: Int = POINTS_GIFT_PAGE_SIZE): List<MallProductCardModel>` | 默认店 1 / 页 1 / 20 条 | 过滤后列表 | 与 #27 **同一端点**；整个块 `runCatching { … }.getOrElse { emptyList() }` `:175,:182` ⇒ 业务码非 0、畸形 JSON、Transport 异常**全部静默**；只看 `response.data`（`:181`，不走 `requireData`）→ `pointsGiftProjection` |
| `feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MallApi.kt:185-190` | `private suspend fun requireRawMapPost(path: String, body: String, fallback: String): Unit` | — | — | `client.postApi(path, body) { el -> el as? JsonObject }` 把 `data` 只当「是不是 object」用，再 `requireData(fallback)`；供 `addToCart`/`cancelOrder` |
| `feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MallApi.kt:192-201` | `companion object { const val PAGE_SIZE = 10; POINTS_GIFT_PAGE_SIZE = 20; DEFAULT_POINTS_STORE_ID = 1 }` | — | — | 三个常量，注释逐条对齐 Dart 侧同名字段 |
| `feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MallApi.kt:208-217` | `object MallOrderStatusQuery { UNPAID = "0"; PAID = "1,2"; CLOSED = "3,4" }` | — | — | 唯一合法 `status` 取值集合（+ null/空 = 全部） |
| `feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MallApi.kt:223-224` | `fun pointsGiftProjection(page: ListPage<MallProductCardModel>?): List<MallProductCardModel>` | 可空页 | `pricePoints > 0` 的行 | 纯函数：`page?.items.orEmpty().filter { it.pricePoints > 0 }`，无网络无 IO |
| `feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MallApi.kt:231-248` | `object MallApiPaths` | — | — | `BASE = "/api/v1/mall"` `:232`；函数式 `products/productDetail/order/orderPay/orderCancel` `:234-247`；常量 `CART` `:239`、`ORDERS` `:241`。写全 `/api/v1` 前缀，与 `feature/auth` 一致（`:228-229`） |
| `feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MallApi.kt:254-260` | `fun <T> ApiResponse<T>.requireData(fallback: String): T` | 兜底中文文案 | 非空 `data` | `if (!isSuccess \\|\\| payload == null) throw NetworkError.Business(code, message?.takeIf{isNotEmpty} ?: fallback)`。`isSuccess` 定义在 `core/network/src/commonMain/kotlin/com.example.my_kmp_project/core/network/ApiResponse.kt:21`（`code == BFF_OK \\|\\| code == OK`）⇒ **message 优先级高于兜底文案，即使 code == 0** |

#### 3.1.2 `MallProductModels.kt`

`@Serializable public data class MallProductCardModel`（`:26-94`）10 个序列化字段：`productIdRaw`(`product_id`)、`idRaw`(`id`)、`skuId`(`sku_id`,Int)、`title`、`price:String="0.00"`、`coverUrl`(`cover_url`)、`coverAspect:Double`(`cover_aspect`,默认 1.0)、`kind:Int`、`subtitle:String?`、`pricePoints:Int`(`price_points`)。

派生成员（不参与序列化，`:21-23` KDoc 明确「不是服务端字段」）：

| 成员 | 行号 | 规则（以代码为准） |
|---|---|---|
| `val id: String` | `:43-44` | `productIdRaw` trim 非空优先，否则 `idRaw.orEmpty()` |
| `val imageAspectRatio: Double` | `:47-48` | `coverAspect < 0.5` 视为脏数据回退 1.0 |
| `val points: Int?` | `:51-52` | `pricePoints.takeIf { it > 0 }` |
| `val isVirtual: Boolean` | `:54-55` | `kind == 1` |
| `val soldLabel: String` | `:64-73` | `seed = abs(id.hashCode())`、`soldRaw = 20 + seed % 9990`；≥10000 走 `"已兑${fixedOneDecimal(soldRaw/10000.0)}万"` 否则 `"已兑$soldRaw"`。`:57-63` KDoc 记录已知偏差：JVM 与 Dart `String.hashCode` 不同，数值不可能逐位相同，保留的是规则 |
| `val badges: List<String>` | `:76-81` | `kind==1` → 「虚拟发放」；`pricePoints>0 && price.toDoubleOrNull()>0` → 「积分+现金」，仅 `pricePoints>0` → 「积分兑换」 |
| `val priceLabel: String` | `:84-93` | 三态：`"${p}积分+${price}元"` / `"${p}积分"` / `"${price}元"`；**直接拼接 BFF 字符串**，不做数值格式化 |

`public fun fixedOneDecimal(value: Double): String`（`:97-102`）：`round(value*10).toLong()` → `whole = scaled/10`、`frac = abs(scaled%10)` → `"$whole.$frac"`；存在理由见 `:96`（commonMain 无 `String.format`）。注意负数时整数部分保留符号、小数取绝对值。

`@Serializable MallProductPayload`（`:109-115`）：详情 `data.product` 五字段。
`@Serializable MallSkuOffer`（`:118-154`）：`skuId/title/price:String/pricePoints/stockQty/specs:JsonObject?/deliverType:Int?`；派生 `priceLabel`（`:130-138`，同商品卡三态）、`label`（`:144-149`：无规格 → `title.ifEmpty { "默认规格" }`，有规格 → `joinToString(" / ") { "$k $v" }`）、`specEntries()`（`:152-153`：`specs?.entries?.map { key to renderJsonScalar(value) }.orEmpty()`，键序即 BFF 键序）。
`@Serializable MallProductDetailResponse`（`:157-168`）：`product:MallProductPayload?`、`skus:List<MallSkuOffer>`，摊平只读属性 6 个（`:162-167`）。
`public fun renderJsonScalar(value: JsonElement?): String`（`:174-178`）：`null → "null"`、`JsonPrimitive → content`、其余 `toString()`。

#### 3.1.3 `MallOrderModels.kt`

| 类型 / 成员 | 行号 | 要点 |
|---|---|---|
| `MallOrderItem` | `:19-32` | `itemId/orderId/productTitle/coverUrl/qty/lineAmount:String/kind/contentUrl`；`isVirtual = kind == 1` `:31`。列表行与详情共用（`:13-14`） |
| `MallOrderHead` | `:39-53` | `orderId/orderNo/storeId/status/amount:String/createdAt/receiver*3/paymentChannel:Int?/paidAt/totalPoints` |
| `MallOrderDetailResponse` | `:63-92` | `order:MallOrderHead?` + `items` + `payDeadlineAt`(`pay_deadline_at`)；**13 个摊平属性** `:68-79`；`statusLabel` `:81`、`isUnpaid` `:82`、`payDeadline:Instant?` `:84-85`、`hasReceiver`（三件套任一 trim 非空）`:88-91`。`:56-61` KDoc 强调 `pay_deadline_at` 读**根 data**，`order` 里同名字段被类型结构忽略（测试 `:454-467` 专门放了干扰键） |
| `MallOrderListRow` | `:96-122` | 自带 `payDeadlineAt` + `items`；`statusLabel/isUnpaid/payDeadline` `:106-110`；`primaryTitle` `:116-117`：**只有空列表**才回退 `"订单 $orderNo"`（`:112-115` 警示勿写成 `?:`）；`primaryCover` `:119`；`totalQty = items.sumOf { it.qty }` `:121` |
| `fun mallOrderStatusLabel(status: Int)` | `:125-132` | 0 待支付 / 1 已支付 / 2 已履约 / 3 已取消 / 4 已关闭 / else 未知 |
| `fun parseMallTime(raw: String): Instant?` | `:144-147` | 空串 null；`Instant.parseOrNull(raw)` 失败再试 `raw.replace(' ', 'T')`。`:134-142` 记录偏差：不带时区偏移的串在 KMP 返回 null（commonMain 无时区库），Go BFF 恒发 RFC3339 故实际链路等价 |
| `fun formatPayCountdown(remaining: Duration): String` | `:153-160` | 负数 clamp 到 `Duration.ZERO`；`HH:MM:SS` 全部 `padStart(2,'0')`；只做 `inWholeSeconds` 截断（无四舍五入） |

#### 3.1.4 `MallRequests.kt`

`MallAddToCartRequest{skuId,qty}`（`:20-23`）、`MallOrderLine{skuId,qty}`（`:27-30`）、`MallCreateOrderRequest{idempotencyKey,storeId,lines,receiver*3 = ""}`（`:39-46`）、`MallPayOrderRequest{paymentChannel}`（`:50-52`）、`MallCreateOrderResponse{order:MallOrderHead?}`（`:59-61`）。全部 snake_case `@SerialName`。`:9-16` KDoc：`NetworkJson` 关了 `explicitNulls`，这些 body 无需显式 null。

`public fun nowMicros(): Long`（`:68-71`）：`Clock.System.now().epochSeconds * 1_000_000L + nanosecondsOfSecond / 1_000L`。
`public fun mallIdempotencyKey(skuId: Int, micros: Long = nowMicros()): String = "$skuId-$micros"`（`:80-81`）。`:73-79` KDoc 明确保留 Dart 缺陷：键每次重新生成 ⇒ 只能去重同一请求重发，**不能**去重客户端超时重试（重试会生成第二单），契约表 §5.3 要求 parity 优先。

#### 3.1.5 `MembershipCatalog.kt`（纯静态，无网络）

`enum MembershipTier { Svip, AiSvip }`（`:5-8`）—— 与 `feature/pay` 的 `MembershipTier`（`feature/pay/src/commonMain/kotlin/com.example.my_kmp_project/feature/pay/MembershipModels.kt:19`，带 `wireValue`）**同名不同包不同类型**，阅读时勿混。
`data class MembershipPlan(id, tier, title, price: Double, originalPrice: Double, badge: String?, dailyHint: String?)`（`:10-18`）、`MembershipPromo(title, subtitle)`（`:20-23`）、`MembershipFeature(title, subtitle)`（`:25-28`）。
`object MembershipTokens`（`:31-62`）：13 个 `Color` 常量（如 `PageBg=0xFFF7F7F7` `:32`、`SvipCtaStart/End` `:47-48`、`AiCtaStart/End` `:55-56`）+ 2 个 `const val`（`PlanCardHeight=148` `:60`、`PlanCardRadius=12` `:61`）。
`object MembershipCatalog`（`:64-142`）：`statusExpired` `:65`、`displayName="小趣友腻腻"` `:67`、`DeductionAmount=12.0` `:68`、`svipPlans`（3 条：`svip_12m` 380/488、`svip_24m` 488/888、`svip_year_auto` 288/488）`:70-96`、`aiSvipPlans`（`ai_12m` 488/688、`ai_24m` 688/1288、`ai_year_auto` 398/688）`:98-124`、`plansFor(tier)` `:126-129`、`promoFor(tier)` `:131-134`、`aiFeatures`（4 条）`:136-141`。
金额形态：此处 price/originalPrice 是 **Double 硬编码**，与商城 BFF 的字符串价格完全独立；没有格式化函数参与。
消费者：`grep -rn --include='*.kt' -E '\b(MembershipCatalog|MembershipTier|MembershipPlan|MembershipTokens)\b' . | grep -v '^./feature/commerce/'` 只命中 `feature/pay/…` 的同名符号 ⇒ **`MembershipCatalog` / `MembershipTokens` 在 commerce 模块外无任何 Kotlin 消费者**，`MembershipScreen` 也未引用它（见 3.1.6）。

#### 3.1.6 `MembershipScreen.kt`

```kotlin
@Composable
public fun MembershipScreen(
    onBack: () -> Unit,                              // :41 返回回调，绑定到左上透明热区
    gateway: PayGateway = FlaggedPayGateway(),       // :42 支付端口，默认「标记式」假网关
)
```

真实行为（全文件仅 103 行）：

1. `ReportMainTabRoot(isRoot = false)` `:44` —— 声明为二级页（AGENTS.md 底部栏规则）。
2. `rememberCoroutineScope()` `:45`；`val available = remember(gateway) { gateway.availableChannels() }` `:46` —— key 只有 gateway，渠道列表不随 recomposition 刷新。
3. 视觉层 = **一张 Flutter SoT 整图**：`Image(painterResource(Res.drawable.pay_membership_body), contentScale = FillBounds)` `:52-57`；容器底色 `Color(0xFFFFF8EE)` `:50`。**没有**用 `MembershipCatalog`/`MembershipTokens` 逐项绘制套餐、抵扣、渠道（对照 `feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MembershipCatalog.kt` 无引用，import 列表 `:3-33` 可证）。
4. 返回热区 `:58-67`：文本 `"‹"` 且 `color = Color.Transparent`（`:63`）—— 不可见但可点，配 `statusBarsPadding()`。
5. **CTA 热区** `:69-92`：底部 `padding(bottom=72.dp)`、高 52.dp 的透明 Box，点击 `scope.launch { … }` `:76`：
   - `val channel = available.firstOrNull() ?: PayChannel.WeChat` `:77`；
   - `gateway.pay(channel = channel, planId = "svip_1m")` `:78-81` —— planId 是**写死的字符串**，与 `MembershipCatalog.svipPlans` 的三个 id（`svip_12m/svip_24m/svip_year_auto`）**都对不上**，也不来自 `:feature:pay` 的 catalog；
   - 结果映射为文案 `:82-88`：`Success` → `result.sandbox` 决定「沙箱支付成功（非真实扣款）」/「支付成功」；`Cancel` → 「已取消支付」；`Unavailable` → 「渠道不可用（未接入真实 SDK）」；`Failure` → 「支付失败：${result.message}」；
   - `showPlatformToast(msg)` `:89`（`core.platform`）。
   - 无进度态、无 loading、无重复点击保护、不发任何 HTTP（不触 `PayApi`/`MallApi`）。
6. **协议热区** `:94-101`：底部 36.dp、160×28.dp，点击只 `showPlatformToast("iHome会员协议")`，不跳 WebView。

调用点（file:line）：
- `composeApp/src/commonMain/kotlin/com.example.my_kmp_project/app/AppShell.kt:429`（`AppRoute.Membership ->`，import `:62`）
- `composeApp/src/commonMain/kotlin/com.example.my_kmp_project/app/AppCrossFeatureScreens.kt:45`（import `:10`）
- `composeApp/src/androidMain/kotlin/com.example.my_kmp_project/nativeshell/NativeAndroidMain.kt:602`（overlay，import `:59`）

三处均未显式传 `gateway` ⇒ 全部落到默认 `FlaggedPayGateway()`。

#### 3.1.7 `MallApiContractTest.kt`（563 行，22 个用例）

夹具是真实 BFF 信封 `{code,message,data,timestamp}`（`:404-468`）。用例与断言重点：

| 用例（行号） | 覆盖 |
|---|---|
| `shelfPageDecodesListPaginationAndDerivedFields` `:32-60` | #27 path+query 精确匹配 `("GET","/api/v1/mall/stores/7/products",page=2,size=10)`；`hasMore(pageSize=10)`；`cover_aspect=0.4 → 1.0`；`points=null`；第二行 `id=1002`（`id` 别名）/`badges=[虚拟发放,积分兑换]`/`priceLabel=800积分` |
| `shelfPageDerivedSoldLabelUsesAbsHashCodeSeedRange` `:63-74` | `soldRaw` 落在 20…9999 或「1.0万」 |
| `productDetailDecodesProductAndSkus` `:79-102` | #28；`sku.label="颜色 松香 / 规格 100ml"`、空 title → 「默认规格」、`deliverType` null/1 |
| `createOrderSendsFullBodyAndReturnsOrderId` `:107-128` | #30 body 逐字断言 `store_id`/`lines`/三个 receiver 空串；幂等键正则 `(\d+)-(\d+)` 且 micros > 1e15 |
| `idempotencyKeyIsRegeneratedPerAttempt_notStable` `:131-137` | 锁死「每次重新生成」这一**缺陷** |
| `createOrderThrowsWhenOrderIdMissing` `:140-145` | `order_id=0` → `Business("下单失败：缺少订单号")` |
| `buyIsTheTwoCallWrapperCreateThenPay` `:148-165` | 断言调用序列两次 POST，且 `#33` body 恰为 `{"payment_channel":2}` |
| `ordersListStatusIsCommaJoinedQueryOrOmitted` `:170-189` | 四种 status（含空串省略）逐条比对 query map |
| `orderListRowStatusLabelsAndDerivedAggregates` `:192-209` | #31 摊平/`totalQty=3`/`payDeadline` 非空 |
| `orderListRowEmptyItemsFallsBackToOrderNoTitle` `:212-222` | 空 items → `"订单 MO5"` |
| `orderDetailReadsPayDeadlineFromRootNotOrder` `:227-249` | 根 `pay_deadline_at=10:15` 覆盖 order 内 `09:15` 干扰键 |
| `payOrderReturnsDetailAndCancelIsPostWithoutBody` `:252-267` | 取消是 POST 且 `body == ""` |
| `addToCartBodyIsSnakeCaseSkuQty` `:272-277` | body 恰为 `{"sku_id":11,"qty":3}` |
| `rawMapEndpointsRequireNonNullDataLikeDartUnderscoreData` `:280-291` | `code=0,data=null` 仍抛，用中文兜底 |
| `serverMessageWinsOverChineseFallbackEvenOnCodeZero` `:294-298` | message 优先级 |
| `businessErrorThrowsWithServerMessageOrFallback` `:301-327` | 403/「无权限」；空 message → 各 fallback 文案 |
| `pointsGiftsDefaultStore1Size20AndFilterPricePoints` `:332-341` | #26 默认值与过滤 |
| `pointsGiftsHonorExplicitStoreAndPage` `:344-349` | 显式参数 |
| `pointsGiftsSwallowEveryFailureLikeDartTryCatch` `:352-367` | 业务失败 / 非 JSON / Transport 抛 三种都返回空列表 |
| `mallOrderStatusLabelTable` `:372-380` | 全表 + 9/-1 → 未知 |
| `parseMallTimeSemantics` `:383-391` | 空/非法 null；`Z`、`+08:00`、空格分隔都非空 |
| `formatPayCountdownPadsHoursAndClampsNegative` `:394-400` | `01:02:03` / `00:00:45` / `10:00:00` / 0 与负数都 `00:00:00` |

`public class FakeApiClient(responder: (verb, path, query, body) -> String) : ApiClient`（`:472-563`）：实现 `getRaw/getAbsoluteRaw/postRaw/postForm/putRaw/patchRaw/deleteRaw/getApi/postApi/putApi/patchApi/deleteApi`（`:499-562`），记录 `calls: MutableList<Call>`（`:482`，`Call` 定义 `:475-480`），`parse()` `:484-497` 复用真实 `ApiEnvelopeParser.parse(httpStatus = 200)`。它是 **public** 且被 wallet/pay/ai 的契约测试复用（见 3.2.3 / 3.3.3 / 3.4.3）。


### 3.2 feature/wallet

#### 3.2.1 `WalletApi.kt`

`public class WalletApi(public val client: ApiClient = NetworkFacade.api())`（`feature/wallet/src/commonMain/kotlin/com.example.my_kmp_project/feature/wallet/WalletApi.kt:27-29`）。类 KDoc `:17-26`：`GET /api/v1/wallet` 在 Dart 有 **3 个调用点 / 2 种形状**，KMP 拆成 `fetchSummary`（抛）与 `fetchBalance`（永不抛、可空）。

| 成员（file:line） | 完整签名 | 参数语义 | 返回 | 内部真实行为 / 副作用 |
|---|---|---|---|---|
| `feature/wallet/src/commonMain/kotlin/com.example.my_kmp_project/feature/wallet/WalletApi.kt:31-34` | `suspend fun fetchSummary(): WalletSummary` | — | 汇总 | `GET /api/v1/wallet`（`client.getApi`，path = `WalletApiPaths.BASE` `:133`，**无 query**）→ `requireData("加载钱包失败")`；失败即抛 |
| `feature/wallet/src/commonMain/kotlin/com.example.my_kmp_project/feature/wallet/WalletApi.kt:43-46` | `suspend fun fetchBalance(): String?` | — | 余额元串或 null | **同一端点**但只取 `.data?.balance`；`runCatching{…}.getOrNull()` ⇒ 业务失败 / `data` 缺失 / Transport 异常一律 null，不抛（对齐 Dart 非阻塞读取 `:36-42`）。成功但缺 `balance` 键时返回默认 `"0.00"`（`feature/wallet/src/commonMain/kotlin/com.example.my_kmp_project/feature/wallet/WalletModels.kt:17`） |
| `feature/wallet/src/commonMain/kotlin/com.example.my_kmp_project/feature/wallet/WalletApi.kt:63-73` | `suspend fun fetchLedger(limit: Int = LEDGER_LIMIT, offset: Int = 0): List<WalletLedgerEntry>` | `limit` 默认 **20**，`offset` 默认 0（**0-based 游标，调用方自行推进**） | 流水列表 | `GET /api/v1/wallet/ledger?limit&offset`（query 走 `queryOf` `:66`）→ `getListPage` 只为复用 `decodeListPage` 的「`list` 或 `items` 都吃」能力（`:48-53` KDoc）；`:69` 特殊放行：`data != null && (isSuccess \\|\\| code == NetworkCodes.PASSTHROUGH)` 时直接返回 `page.items`（复刻 Dart「无 `code` 键则合成 0」，`:55-58`）；否则 `requireData("加载流水失败").items` `:72` ⇒ 连 `data` 都解不出的畸形 body 仍抛（`code = -1`） |
| `feature/wallet/src/commonMain/kotlin/com.example.my_kmp_project/feature/wallet/WalletApi.kt:76-92` | `suspend fun bindCard(bankName: String, cardLast4: String, holderName: String = "", isDefault: Boolean = false): BankCard` | 持卡人默认空、默认卡默认 false | 新卡 | `POST /api/v1/wallet/cards`，body `WalletBindCardRequest` **四键总发**；`requireData("绑卡失败")` |
| `feature/wallet/src/commonMain/kotlin/com.example.my_kmp_project/feature/wallet/WalletApi.kt:98-104` | `suspend fun setDefaultCard(cardId: Int): Unit` | 卡 id | 无 | `POST /api/v1/wallet/cards/{cardId}/default`，`body = ""` `:99`（`:94-97` KDoc：动作 POST 不是 PUT）；`data` 解码为 `JsonObject?` 仅判空 `:100-102` → `requireData("设置默认卡失败")` |
| `feature/wallet/src/commonMain/kotlin/com.example.my_kmp_project/feature/wallet/WalletApi.kt:107-110` | `suspend fun deleteCard(cardId: Int): Unit` | 卡 id | 无 | `DELETE /api/v1/wallet/cards/{cardId}`（`client.deleteApi<JsonObject>`）→ `requireData("删除银行卡失败")` |
| `feature/wallet/src/commonMain/kotlin/com.example.my_kmp_project/feature/wallet/WalletApi.kt:117-123` | `suspend fun recharge(amount: String, channel: Int, cardId: Int? = null): WalletRechargeResult` | **`amount` 是十进制「元」串**（`:112-116` KDoc 称之为「本表最容易被写错的一行」），不是分 | 充值结果 | `POST /api/v1/wallet/recharge`，body `WalletRechargeRequest`；`cardId = null` 时该键被省略（`explicitNulls=false`，`feature/wallet/src/commonMain/kotlin/com.example.my_kmp_project/feature/wallet/WalletModels.kt:96-97`）→ `requireData("充值失败")`。**不发任何支付 SDK，只打服务端** |
| `feature/wallet/src/commonMain/kotlin/com.example.my_kmp_project/feature/wallet/WalletApi.kt:125-128` | `companion object { const val LEDGER_LIMIT = 20 }` | — | — | 唯一常量 |
| `feature/wallet/src/commonMain/kotlin/com.example.my_kmp_project/feature/wallet/WalletApi.kt:132-141` | `object WalletApiPaths` | — | — | `BASE/LEDGER/CARDS/RECHARGE` 四个常量 `:133-136` + `card(id)` `:138`、`cardDefault(id)` `:140` |
| `feature/wallet/src/commonMain/kotlin/com.example.my_kmp_project/feature/wallet/WalletApi.kt:144-150` | `fun <T> ApiResponse<T>.requireData(fallback: String): T` | — | — | 与 `feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MallApi.kt:254-260` **逐字符重复实现**（同逻辑、不同包），两份都是 public 扩展；`:143` KDoc 指回 Dart `WalletApi._data` |

#### 3.2.2 `WalletModels.kt`

金额口径（`:6-12` KDoc）：**元字符串 + 分整数双字段**；`balance:String` 不 Double 化，`*_fen` 是整数分。

| 类型 | 行号 | 字段 / 派生 |
|---|---|---|
| `@Serializable data class WalletSummary` | `:16-20` | `balance:String="0.00"`、`balanceFen:Int`(`balance_fen`)、`cards:List<BankCard>` |
| `@Serializable data class BankCard` | `:27-36` | `cardId/bankName/cardLast4/holderName/isDefault`；`display = "$bankName(****$cardLast4)"` `:35`。同时用作 `cards` 元素与 `POST /wallet/cards` 响应（`:22-25`） |
| `@Serializable data class WalletLedgerEntry` | `:43-70` | `ledgerId/deltaFen/balanceFen/reason/refId`；`deltaYuan` `:54-60` 纯整型运算：取符号 → `v/100` + `"."` + `(v%100).padStart(2,'0')` → 前缀 `+`/`-`（`:50-53` 说明刻意避开浮点与 Locale）；`reasonLabel` `:63-69`：`recharge→充值`、`mall_pay→商城支付`、`mall_refund→商城退款入账`、其余原样 |
| `@Serializable data class WalletRechargeResult` | `:74-78` | `balance/balanceFen/deltaFen` |
| `@Serializable data class WalletBindCardRequest` | `:85-90` | 四键全 snake_case，后两个带默认值 ⇒ 总发 |
| `@Serializable data class WalletRechargeRequest` | `:100-104` | `amount:String`、`channel:Int`、`cardId:Int?`（null 省略） |

#### 3.2.3 `WalletApiContractTest.kt`（346 行，13 个用例）

| 用例（行号） | 断言重点 |
|---|---|
| `summaryDecodesBalanceBalanceFenAndCards` `:27-46` | 无 query、`balance="128.50"` 保持字符串、`balance_fen=12850`、`display="招商银行(****1234)"` |
| `balanceOnlyAccessorSwallowsFailuresLikeDartNonBlockingRead` `:49-70` | 成功取 `"128.50"`；403 / `data:null` / Transport 三种都返回 null；同一 403 走 `fetchSummary` 必抛「无权限」 |
| `balanceDefaultsToZeroWhenBalanceKeyMissing` `:73-77` | `data` 无 `balance` → `"0.00"` |
| `ledgerSendsLimitOffsetQueryAndReadsItemsOnly` `:82-104` | `limit=20&offset=0`；只有 `items` 也能解出 3 行；`+100.00` / `-25.00` / `+0.05` 三个 `deltaYuan`；未知 reason 原样 |
| `ledgerOffsetAdvancesExplicitly` `:107-111` | 显式 `offset=40` 原样进 query |
| `ledgerBusinessFailureThrows` `:114-119` | 500 + message → `Business("流水服务异常")` |
| `ledgerSynthesizesSuccessWhenBodyHasNoCodeKey` `:122-134` | 无 `code` 键 body 正常返回 2 行；`"not json at all"` 仍抛且 `code == NetworkCodes.PASSTHROUGH` |
| `bindCardAlwaysSendsHolderNameAndIsDefault` `:139-147` | body 逐字 `{"bank_name":…,"card_last4":…,"holder_name":"","is_default":false}` |
| `setDefaultCardIsPostWithoutBodyAndNeedsDataMap` `:150-159` | `POST …/cards/3/default` 无 query/body；`data:null` → 「设置默认卡失败」 |
| `deleteCardIsDeleteOnCardPath` `:162-169` | `DELETE …/cards/3`；403 message 透传 |
| `rechargeSendsAmountAsDecimalStringNotFen` `:174-184` | **body 恰为 `{"amount":"12.34","channel":1}`**（parity 关键）；结果三字段 |
| `rechargeOmitsCardIdOnlyWhenNull` `:187-195` | 有卡 → `"card_id":3`；null → body 不含 `card_id` |
| `rechargeFailureUsesServerMessageThenFallback` `:198-206` | 「余额不足」优先，缺 message 用「充值失败」 |

`public class FakeWalletClient`（`:255-346`）与 commerce 的 `FakeApiClient`（`feature/commerce/src/commonTest/kotlin/com.example.my_kmp_project/feature/commerce/MallApiContractTest.kt:472-563`）是**同构重复实现**（各自记录 `Call`、各自 `ApiEnvelopeParser.parse(httpStatus = 200)`），因为跨模块 test source set 不共享。

#### 3.2.4 消费者

`grep -rn --include='*.kt' -E 'feature\.wallet' . | grep -v '^./feature/wallet/'` → **无命中**（唯一提及是注释 `feature/mine/src/commonMain/kotlin/com.example.my_kmp_project/feature/mine/WalletMockStore.kt:42`，讲的是 Flutter 侧 mock，不是本模块）。⇒ `:feature:wallet` 全部 7 个方法在 app 内**零 UI 消费者**，只有本模块契约测试调用。钱包页实际走的是 `feature/mine` 的 `WalletMockStore`（mock）。


### 3.3 feature/pay

#### 3.3.1 `PayApi.kt`

`public class PayApi(public val client: ApiClient = NetworkFacade.api())`（`feature/pay/src/commonMain/kotlin/com.example.my_kmp_project/feature/pay/PayApi.kt:37-39`）。
类 KDoc `:14-36` 三条要点：① 五个端点共用一对转换器（Dart `_mapConverter` / `_requireData`）；② `data` 非 Map 折成 `{}` **不算失败**，`data` 键缺失/为 null **才抛**；③ **`POST /api/v1/payments/prepay` 故意不落地**（`:32-35`）——Dart 只有 `MembershipPaymentGateway.prepayPath` 常量、全仓零调用，按 M1 纪律「不得为死常量编造调用」。

| 成员（file:line） | 完整签名 | 参数语义 | 返回 | 内部真实行为 |
|---|---|---|---|---|
| `feature/pay/src/commonMain/kotlin/com.example.my_kmp_project/feature/pay/PayApi.kt:41-42` | `suspend fun fetchMe(): MembershipMeResponse` | — | 目录 + 权益 | `GET /api/v1/membership/me`（`PayApiPaths.ME` `:241`）→ `getBffMap(…, "加载会员信息失败")` → `membershipDto` |
| `feature/pay/src/commonMain/kotlin/com.example.my_kmp_project/feature/pay/PayApi.kt:50-57` | `suspend fun buyout(planId: String, channel: String): MembershipBuyoutResponse` | `channel` 为 `PaymentMethodType.channelWire` 串 | 买断响应 | `POST /api/v1/membership/buyout`，body `{"plan_id":…,"channel":…}`（`MembershipBuyoutRequest`），fallback「下单失败」；`:44-49` KDoc：`status=='paid'` 表示服务端已扣款，**必须短路不再 confirm** |
| `feature/pay/src/commonMain/kotlin/com.example.my_kmp_project/feature/pay/PayApi.kt:63-69` | `suspend fun confirmBuyout(orderId: Int): Unit` | 订单 int | 无 | `POST /api/v1/membership/buyout/confirm`，body `{"order_id":n}`，fallback「确认支付失败」；响应内容被忽略但 `data` 必须存在（`:59-62`） |
| `feature/pay/src/commonMain/kotlin/com.example.my_kmp_project/feature/pay/PayApi.kt:79-103` | `suspend fun verifyHuawei(productId: String, purchaseToken: String, purchaseOrderId: String = "", subscriptionId: String = "", jwsPurchaseOrder: String = ""): MembershipHuaweiVerifyResponse` | 后三键默认空串且总发 | `finish_purchase` 标志 | `POST /api/v1/membership/huawei/verify`，fallback「华为内购验单失败」。`:77`、`:86`、`:102` 三处 **`GAP(R5)`** 注释：真正的 `HuaweiIapBridge.finishPurchase` 属 ohosMain SDK lane，本层**只回传布尔** |
| `feature/pay/src/commonMain/kotlin/com.example.my_kmp_project/feature/pay/PayApi.kt:112-131` | `suspend fun verifyApple(productId: String, transactionId: String, originalTransactionId: String = "", receiptData: String = ""): MembershipAppleVerifyResponse` | 后两键默认空串且总发 | `complete_purchase` 标志 | `POST /api/v1/membership/apple/verify`，fallback「苹果内购验单失败」；`:110` `GAP(R5)`：StoreKit 桥接本体缺 |
| `feature/pay/src/commonMain/kotlin/com.example.my_kmp_project/feature/pay/PayApi.kt:153-192` | `suspend fun payBuyout(method: PaymentMethodType, planId: String, sdkPay: suspend (MembershipBuyoutResponse) -> Boolean): MembershipPayOutcome` | `sdkPay` 是**注入的 SDK 交接点**（返回是否成功） | 结论对象，**不外抛** | 见下方流程分解 |
| `feature/pay/src/commonMain/kotlin/com.example.my_kmp_project/feature/pay/PayApi.kt:198-199` | `private inline fun <reified T : Any> membershipDto(map: JsonObject, empty: () -> T): T` | — | — | `decodeEnvelopeData<T>(map) ?: empty()` —— 单字段脏 ⇒ 整份退化为全默认 DTO |
| `feature/pay/src/commonMain/kotlin/com.example.my_kmp_project/feature/pay/PayApi.kt:201-211` | `private suspend fun getBffMap(path, fallback)` / `postBffMap(path, body, fallback): JsonObject` | — | 裸 Map | `client.getApi/postApi(path) { el -> el.toBffMapOrNull() }` → `requireData(fallback)` |
| `feature/pay/src/commonMain/kotlin/com.example.my_kmp_project/feature/pay/PayApi.kt:215-227` | `enum class MembershipPayStatus { Success, LaunchFailed, SdkNotSucceeded, SdkError }` | — | — | Dart `WysPaymentStatus` 的契约层子集 |
| `feature/pay/src/commonMain/kotlin/com.example.my_kmp_project/feature/pay/PayApi.kt:230-237` | `data class MembershipPayOutcome(status, message: String = "", code: Int? = null, orderId: Int = 0)` | — | — | `isSuccess = status == Success` `:236` |
| `feature/pay/src/commonMain/kotlin/com.example.my_kmp_project/feature/pay/PayApi.kt:240-246` | `object PayApiPaths` | — | — | `ME / BUYOUT / BUYOUT_CONFIRM / HUAWEI_VERIFY / APPLE_VERIFY` 五个常量，无 `PREPAY` |
| `feature/pay/src/commonMain/kotlin/com.example.my_kmp_project/feature/pay/PayApi.kt:252-256` | `private fun JsonElement?.toBffMapOrNull(): JsonObject?` | — | — | null/JsonNull → **null**（让 `requireData` 抛）；JsonObject → 原样；其它（数组/标量）→ `JsonObject(emptyMap())` |
| `feature/pay/src/commonMain/kotlin/com.example.my_kmp_project/feature/pay/PayApi.kt:263-269` | `fun <T> ApiResponse<T>.requireData(fallback: String): T` | — | — | **第三份重复实现**（同 `feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MallApi.kt:254`、`feature/wallet/src/commonMain/kotlin/com.example.my_kmp_project/feature/wallet/WalletApi.kt:144`）；`:27-30` KDoc 记录偏差：Dart 的 `model?.message ?? fallback` 永不生效（`ResultModel.message` 非空），本层统一「空文案 ⇒ 中文兜底」 |

`payBuyout` 真实流程（`:133-152` KDoc + `:153-192` 实现）：

1. `if (!method.usesBuyoutChannel) return Outcome(LaunchFailed, message = "请使用对应内购通道")` `:158-161` —— huawei/apple **不发任何 HTTP**。
2. `buyout(planId, method.channelWire)` `:163`。
3. `buyout.isPaid` ⇒ `return Outcome(Success, orderId)` `:164-167`，**不碰 SDK、不发 confirm**（`:165` 注释：Dart 原版不带 order_id，本层额外带上以便对账）。
4. `if (!sdkPay(buyout)) return Outcome(SdkNotSucceeded, orderId)` `:169-174` —— `:168` `GAP(R5)`：真实 `WysLoginSharePayService.pay` 在平台 lane，这里只吃布尔。
5. `if (buyout.orderId > 0) confirmBuyout(...)` `:175-177` —— `order_id <= 0` **静默跳过**（契约表 §5.1，合并成一步会丢「已支付未确认」单）。
6. `catch (NetworkError) → Outcome(SdkError, message=e.message.orEmpty(), code=e.code)` `:179-184`；`catch (Exception) → Outcome(SdkError, message = e.message ?: e.toString())` `:185-191`。
7. 未实现项（`:150-151`）：Dart 的「微信未配置 ⇒ `LaunchFailed('请先在 WysWechatConfig 填写…')`」前置检查**不存在于本层**（留给平台 lane）。

#### 3.3.2 `MembershipModels.kt`

| 类型 | 行号 | 字段 / 派生（以代码为准） |
|---|---|---|
| `enum MembershipTier(wireValue)` | `:19-32` | `Svip("svip")`、`AiSvip("ai_svip")`；`fromWire(raw)` `:29-30`：**除 `"ai_svip"` 外一律落回 Svip**（含 null/脏值，`:26-28` 明令勿改 null）。与 `feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MembershipCatalog.kt:5-8` 的同名枚举**不是同一类型**（那个无 `wireValue`） |
| `enum PaymentMethodType(channelWire)` | `:41-51` | `WeChat("wechat")`、`Alipay("alipay")`、`Balance("balance")`、`Huawei("")`、`Apple("")`；`usesBuyoutChannel = channelWire.isNotEmpty()` `:50` |
| `@Serializable MembershipPlan` | `:61-81` | `id`(`plan_id`)、`tier:String`、`title`、`price:String=""`（**BFF 字符串，缺失按空串**）、`priceFen`(`price_fen`)、`months=1`、`huaweiProductId`、`appleProductId`、`autoRenew`(`auto_renew_eligible`)；派生 `resolvedTier` `:74`、`priceValue = price.toDoubleOrNull() ?: (priceFen / 100.0)` `:77`、`originalPriceValue = priceValue` `:80`（目录无划线价） |
| `@Serializable MembershipEntitlement` | `:85-90` | `tier`、`cta`，`resolvedTier` `:89`（Dart 只读这两键） |
| `@Serializable MembershipMeResponse` | `:100-113` | `catalog`、`entitlements` 默认空列表；`catalogPlans = catalog.filter { it.id.isNotEmpty() }` `:105`；`ctaFor(tier) = entitlements.firstOrNull { it.tier == tier.wireValue }?.cta` `:111-112`（未命中返回 null，`'开通'` 兜底属 UI lane） |
| `@Serializable MembershipBuyoutResponse` | `:123-143` | `status:String`、`orderId`(`order_id`)、`prepayParams:JsonObject?`(`prepay_params`)；`isPaid = status == "paid"` `:129`；`prepayEntries` `:132-135`；`weChatPrepayId()` `:141-142` **兼容 `prepayid` / `prepayId` 两种拼法**；私有 `prepayString` `:145-146`（取 `JsonPrimitive.content` 且非空）、`renderScalar` `:148-151` |
| `@Serializable MembershipBuyoutRequest` | `:155-158` | `{plan_id, channel}` |
| `@Serializable MembershipConfirmRequest` | `:162-164` | `{order_id:int}` |
| `@Serializable MembershipHuaweiVerifyResponse` | `:168-170` | `finishPurchase:Boolean=false` |
| `@Serializable MembershipAppleVerifyResponse` | `:174-176` | `completePurchase:Boolean=false` |
| `@Serializable MembershipHuaweiVerifyRequest` | `:183-189` | 5 键，后 3 个默认 `""` 且总发（`:179-181`） |
| `@Serializable MembershipAppleVerifyRequest` | `:196-201` | 4 键，后 2 个默认 `""` 且总发 |

#### 3.3.3 `MembershipPayContractTest.kt`（479 行，18 个 `@Test`）

| 用例（行号） | 断言重点 |
|---|---|
| `fetchMeDecodesCatalogAndEntitlements` `:27-60` | 3 条 catalog → `catalogPlans` 过滤掉空 `plan_id` 剩 2；`price="198.00"` + `priceFen=19800` → `priceValue=198.0`；`price=""` → 回退 `2990/100=29.9`；`ctaFor` 两个 tier 各自文案；`MembershipEntitlement().cta == ""` |
| `fetchMeTreatsNonMapDataAsEmptyLikeDartMapConverter` `:63-83` | `data:[]` / `data:"x"` ⇒ 空目录**不抛**；`{"code":0}`（无 data）与 `data:null` ⇒ 抛「加载会员信息失败」；带 message 时服务端文案优先（`"ok"`） |
| `fetchMeBusinessFailureThrows` `:86-94` | 403「无权限」/ 缺 message → 兜底 |
| `buyoutSendsPlanIdAndChannelString` `:99-113` | body 逐字 `{"plan_id":"plan-svip-y","channel":"wechat"}`；`isPaid=false`；`weChatPrepayId()="wx_prepay_id"`；`prepayEntries` 三键有序 |
| `balanceChannelPaidShortCircuitsWithoutConfirm` `:116-131` | `status:"paid"` ⇒ 只有 1 次 HTTP、`sdkCalls` 为空、`Success` |
| `wechatFlowCallsConfirmOnlyAfterSdkSuccess` `:134-152` | 两次 POST 序列；confirm body `{"order_id":55001}`；`:146` 注释再次声明 `/payments/prepay` 是死常量不可调出 |
| `sdkFailureKeepsConfirmBack` `:155-166` | `sdkPay=false` ⇒ `SdkNotSucceeded` + `orderId=55001`，无 `/confirm` |
| `orderIdZeroSilentlySkipsConfirmAfterSdkSuccess` `:169-180` | `order_id:0` 且 SDK 成功 ⇒ **仍 `Success`**、只 1 次 HTTP（静默跳过被锁死） |
| `huaweiAndAppleNeverUseBuyoutChannel` `:183-193` | responder 里 `error("must not call HTTP")`；两种 method 都 `LaunchFailed("请使用对应内购通道")`、`channelWire == ""`、`fake.calls` 为空 |
| `buyoutFailureThrowsWithFallbackCopy` `:196-219` | 5001→「下单失败」；5002「订单已关闭」透传；`{"code":0}` → 「确认支付失败」；`{"code":0,"message":"ok"}` → `"ok"`（附 `membership_api.dart:99` / `result_model.dart:17` 溯源注释 `:208-215`） |
| `payBuyoutFoldsBusinessFailureIntoResultInsteadOfThrowing` `:222-235` | 5001「渠道维护中」→ `SdkError` + `code=5001`，不外抛 |
| `payBuyoutFoldsSdkThrowIntoResultToo` `:238-252` | `sdkPay` 抛 `IllegalStateException` → `SdkError("微信 SDK 崩了")`、`code == null`、无 confirm |
| `nonMapBuyoutDataStillHandsWithEmptyParamsLikeDart` `:255-273` | `data:[]` ⇒ SDK 拿到全默认对象（`status=""`、`orderId=0`、`prepayEntries=[]`），流程走完返回 `Success` 且只 1 次 HTTP；`:270-271` 注释纠正了断言写法必须带动词 |
| `huaweiVerifySendsAllFiveKeysAndReturnsFinishPurchase` `:278-289` | body 逐字五键（后三空串）；`finish_purchase=true` |
| `huaweiVerifyCanReportNoFinishPurchaseForRestoreLoop` `:292-295` | 五参数全传 ⇒ `finishPurchase=false`（对应 `restoreHuaweiPurchases()` 循环验单场景） |
| `appleVerifySendsAllFourKeysAndReturnsCompletePurchase` `:298-313` | body 逐字四键；`complete_purchase=true`；4003 缺 message → 「苹果内购验单失败」 |
| `tierAndChannelWireMapping` `:318-330` | 五个 `channelWire` 全表 + `fromWire(null/"SVIP"/"ai_svip")` |
| `wechatPrepayIdAcceptsBothKeySpellings` `:333-344` | `prepayId` 与 `prepayid` 都吃；无 `prepay_params` ⇒ `""` + 空 entries |

`public class FakePayClient`（`:388-479`）：第三个同构假 client（`Call` `:391-396`、`calls` `:398`、`parse` `:400-413` 复用 `ApiEnvelopeParser.parse`）。私有辅助 `decodeBuyout(dataJson)` `:348-351` 用真实 `buyout()` 走解码。夹具 `:353-384`。

#### 3.3.4 消费者

`grep -rn --include='*.kt' -E 'feature\.pay' . | grep -v '^./feature/pay/'` → **无命中**。`:feature:pay` 与 app 的 UI 层**完全未接线**：`payBuyout` 的 `sdkPay` 注入点在仓库里没有任何生产调用方（只有本测试文件的 lambda）。它和 `:component` 的 `PayGateway`（3.3.5）是**两条平行且未合拢的支付路径**。

#### 3.3.5 `:component` 支付端口现状（外部证据，逐条 file:line）

`component/src/commonMain/kotlin/com.example.my_kmp_project/component/pay/PayGateway.kt`：

| 行号 | 声明 | 真实语义 |
|---|---|---|
| `:4-7` | `interface PayGateway { fun availableChannels(): List<PayChannel>; suspend fun pay(channel: PayChannel, planId: String): PayResult }` | 端口只有两方法，**无金额、无订单号、无 HTTP** |
| `:9-12` | `enum PayChannel { WeChat, Alipay }` | 只有两个渠道；**没有 Balance/Huawei/Apple**（`:feature:pay` 的 `PaymentMethodType` 有 5 个）——两套枚举未对齐 |
| `:14-27` | `sealed interface PayResult { Success(channel, planId, sandbox=false), Cancel, Unavailable(channel), Failure(message) }` | `sandbox` 注释 `:16` 明确「true 仅代表 SandboxPayChannelAdapter，不是真实扣款」 |
| `:33-38` | `class StubPayGateway : PayGateway` | `availableChannels() = emptyList()`；`pay()` 恒 `Unavailable`；`:29-32` 注释「never synthesizes Success」 |
| `:48-52` | `object PayFeatureFlags { var weChatEnabled = false; var alipayEnabled = false; var sandboxEnabled = true }` | 真实 SDK 两个开关**默认关**；沙箱**默认开**（`:40-47` 注释：Spike II debug 默认开，接真实 SDK 后 release 关闭） |
| `:55-58` | `interface PayChannelAdapter { val channel; suspend fun pay(planId): PayResult }` | 单渠道适配器契约 |
| `:64-69` | `class SandboxPayChannelAdapter(channel)` | `pay()` **无条件返回 `Success(sandbox = true)`** `:68` —— 这是仓库里唯一的「支付成功」来源 |
| `:75-77` | `internal expect fun platformWeChatPayAdapterOrNull(): PayChannelAdapter?` / `platformAlipayPayAdapterOrNull()` | 平台注入点（internal） |
| `:83-94` | `object PlatformPayAdapters { weChatOrNull(), alipayOrNull() }` | 先取平台 actual，为 null 时回落 `sandboxOrNull` `:92-93`（`sandboxEnabled` 才给 `SandboxPayChannelAdapter`） |
| `:101-129` | `class FlaggedPayGateway(weChat = PlatformPayAdapters.weChatOrNull(), alipay = PlatformPayAdapters.alipayOrNull())` | `availableChannels` `:105-108` 与 `pay` `:110-119` 都过 `isChannelOpen` `:121-129`：`adapter == null` → false；渠道 flag 开 → true；否则 **仅当 adapter 是 `SandboxPayChannelAdapter` 且 `sandboxEnabled`** 才 true |

三端 actual（全部返回 null，SDK 未接）：

| 平台 | file:line | 内容 |
|---|---|---|
| Android | `component/src/androidMain/kotlin/com.example.my_kmp_project/component/pay/PlatformPayAdapters.android.kt:7-10` | `platformWeChatPayAdapterOrNull()` 体是 `// TODO(pay): return WeChatOpenSdkPayAdapter(appId, api) when OpenSDK + keys are configured` + `return null` |
| Android | 同上 `:16-19` | `platformAlipayPayAdapterOrNull()` → `// TODO(pay): return AlipaySdkPayAdapter(...)` + `return null` |
| iOS | `component/src/iosMain/kotlin/com.example.my_kmp_project/component/pay/PlatformPayAdapters.ios.kt:4`、`:7` | 两个 actual 都是表达式体 `= null`（注释「null until WXApi / AlipaySDK is wired」） |
| OHOS | `component/src/ohosMain/kotlin/com.example.my_kmp_project/component/pay/PlatformPayAdapters.ohos.kt:7`、`:9` | 两个 actual `= null`（`:3-6` 注释：本 source set 无 Android OpenSDK） |

⇒ **三端零真实 SDK**：仓库内没有 WXAPIFactory / PayReq / PayTask / AlipaySDK / StoreKit / Huawei IAP 的任何 Kotlin 调用（grep `PayAdapter` 只命中上述 4 个文件，见第 5 节命令）。当前线上行为链是：`MembershipScreen` → `FlaggedPayGateway` → `PlatformPayAdapters.*OrNull()`（null）→ `SandboxPayChannelAdapter` → `Success(sandbox=true)` → toast「沙箱支付成功（非真实扣款）」。


### 3.4 feature/ai

#### 3.4.1 `AiSseApi.kt`

`public class AiSseApi(private val transport: SseTransport = NetworkFacade.api())`（`feature/ai/src/commonMain/kotlin/com.example.my_kmp_project/feature/ai/AiSseApi.kt:27-29`）——默认注入的 `NetworkFacade.api()` 返回 `ApiClient`，而 `ApiClient` 继承 `SseTransport`（`core/network/src/commonMain/kotlin/com.example.my_kmp_project/core/network/ApiClient.kt:14`、`:59-66`）。

`companion object { const val COMPLETIONS_PATH = "/api/v1/sse/completions" }`（`:30-33`，对齐 Dart `ai_sse_api.dart:10`）。

| 成员（file:line） | 完整签名 | 参数语义 | 返回 | 内部真实行为 |
|---|---|---|---|---|
| `feature/ai/src/commonMain/kotlin/com.example.my_kmp_project/feature/ai/AiSseApi.kt:51-67` | `fun streamCompletion(prompt: String, conversationId: String? = null, clientRequestId: String? = null): Flow<AiStreamEvent>` | `prompt` 必发；两个 id 为 null **或空串**都不发（`:70-73` KDoc，`:82-83` 实现用 `isNullOrEmpty`；注意**纯空格串仍会发**，测试 `feature/ai/src/commonTest/kotlin/com.example.my_kmp_project/feature/ai/AiSseApiTest.kt:158-160` 锁死） | 冷流 `Flow<AiStreamEvent>` | `:56` 组 body → `:57` **每次 collect 新建一个 `SseParser()`**（有状态、不可复用）→ `:58` `transport.openSseTextStream(COMPLETIONS_PATH, body).collect { chunk -> parser.feed(chunk).forEach { frame -> frame.toAiStreamEventOrNull()?.let { emit(it) } } }` → `:64-66` 流自然结束后 `parser.flush()` 再补一轮（Dart EOF flush） |
| `feature/ai/src/commonMain/kotlin/com.example.my_kmp_project/feature/ai/AiSseApi.kt:74-85` | `fun completionsRequestBody(prompt: String, conversationId: String?, clientRequestId: String?): String` | 同上 | JSON 串 | `NetworkJson.encodeToString(JsonObject.serializer(), buildJsonObject { put("prompt", …); if (!conversationId.isNullOrEmpty()) put("conversationId", …); if (!clientRequestId.isNullOrEmpty()) put("clientRequestId", …) })` ⇒ **camelCase 键**，与 `/api/v1` 其余端点的 snake_case 不同（`:24-25`、测试 `:152`） |

`streamCompletion` 不声明 `suspend`：它是 builder，发请求发生在 `collect` 时（`feature/ai/src/commonTest/kotlin/com.example.my_kmp_project/feature/ai/AiSseApiTest.kt:138-141` 断言未 collect 前 `openCount == 0`）。
`AiSseApi` 自身**没有**取消、重试、超时、心跳、错误码常量：全部下沉到传输层（见第 6 节）。
调用点：`grep -rn --include='*.kt' -E 'feature\.ai' . | grep -v '^./feature/ai/'` 只命中 `composeApp/src/commonMain/kotlin/com.example.my_kmp_project/app/AppCrossFeatureScreens.kt:8`（`import …feature.ai.AiStreamScreen`）——**`AiSseApi` 在 commonMain 无任何生产调用方**，唯一消费者是同模块测试。

#### 3.4.2 `AiStreamScreen.kt`

```kotlin
@Composable
public fun AiStreamScreen(onBack: () -> Unit)   // :39
```

真实行为（81 行，全部为静态图 + 热区）：

1. `ReportMainTabRoot(isRoot = false)` `:40`。
2. `Box(Modifier.fillMaxSize().background(Color.White))` `:41-45`。
3. 整页视觉 = 一张 Flutter SoT 图 `Res.drawable.ai_stream_body`，`ContentScale.FillBounds` `:46-51`，`contentDescription = "AI 小石头：欢迎语、快捷问、输入框"` `:48`。
4. 返回热区：`Text("‹", fontSize = 28.sp, color = Color.Transparent)` + `statusBarsPadding()` `:52-61`（与 `MembershipScreen` 同一套「隐形字符当按钮」写法）。
5. 快捷问热区 `:63-70`：`BottomStart`、`padding(start=16.dp, bottom=120.dp)`、280×40.dp，点击 `showPlatformToast("快捷问（mock 流）")` `:69`。
6. 发送热区 `:72-79`：`BottomEnd`、`padding(end=16.dp, bottom=48.dp)`、56×40.dp，点击 `showPlatformToast("发送（mock；真 SSE 见 gap）")` `:78`。

结论（诚实）：**本屏不发 SSE 请求、不 collect `AiSseApi`、没有消息列表 state、没有流式文本、没有「停止生成」按钮、没有错误气泡**。文件头 KDoc `:34-37` 自己写明「Quick-ask / stop stay toast-level mock; true SSE → platform-gap-registry」。
`:25-31` 那 7 条 `core.router.*Routes` import（AiRoutes/ClassroomRoutes/CommunityRoutes/ContentRoutes/HomeRoutes/MineRoutes/VideoRoutes）在本文件正文中**没有任何使用点**（正文只用到 `ReportMainTabRoot`、`showPlatformToast`、`Res.drawable.ai_stream_body`），属残留 import；`:feature:ai` 对 `:core:router` 的 `api` 依赖（`feature/ai/build.gradle.kts:16`）因此只被这些未使用 import 支撑。

调用点：`composeApp/src/commonMain/kotlin/com.example.my_kmp_project/app/AppCrossFeatureScreens.kt:71`（`route == AiRoutes.Stream -> AiStreamScreen(onBack = onBack)`，import `:8`）。

#### 3.4.3 `AiSseApiTest.kt`（186 行，6 个用例）

`private class FakeSseTransport(statusCode = 200, contentType = "text/event-stream", bodyChunks = listOf("event: done\ndata: {}\n\n")) : SseTransport`（`:28-54`）：记录 `requestedPath`/`requestedBody`/`openCount`（`:33-35`），`openSseTextStream` 返回 `flow { emitAll(SseOpenGate(statusCode, contentType, flowOf(*chunks)).eventStreamText()) }` `:45-52` —— **复用真实前流闸门**，因此这条 fake 能真实复刻「前流不喂解析器」。

| 用例（行号） | 锁定的行为 |
|---|---|
| `meta_delta_times_n_done_decode_in_order_across_split_chunks` `:57-88` | 分片故意切在 `da`/`ta:` 字段名中间、meta 空行落到下一片（`:60-63`）⇒ 仍按序出 `Meta(r1,c1,qwen,cr1)` → `Delta("你")` → `Delta("好")` → `Delta("！")` → `Done("stop")`；其中 `{"text":"", "note":…}` 空 delta 被丢（`:64`） |
| `in_stream_error_frame_is_an_event_and_does_not_throw` `:91-111` | `event: error` + `data.error.{code,message}` ⇒ 发 `AiStreamEvent.Error("upstream_timeout","模型上游超时")`，**不抛** |
| `pre_stream_json_envelope_is_reported_as_an_error_and_never_parsed` `:114-131` | 401 + `application/json` + `{"code":10023,"message":"登录失效，请重新登录"}` ⇒ `assertFailsWith<SseException>`，`statusCode=401`、`message` 取信封 `message`、`error="unauthorized"`（status 映射），且 `events.isEmpty()`「前流 body 绝不进 SSE 解析器」 |
| `request_is_camel_case_and_blank_ids_are_omitted_not_sent_empty` `:134-161` | 冷流惰性（`openCount == 0`）；`{"prompt":"hi"}`；带两 id ⇒ `{"prompt":…,"conversationId":"c-7","clientRequestId":"r-7"}`；`conversationId=""` 省略而 `clientRequestId="  "` 保留 |
| `trailing_frame_without_blank_line_is_dropped_like_dart` `:164-171` | 结尾半帧 `event: delta\ndata: {"te` ⇒ 只出 `Delta("完整")`，截断 JSON 不发事件 |
| `keepalive_comments_and_unknown_events_are_ignored` `:174-185` | `: keep-alive` 注释帧、`event: ping` 未知类型、`data: not-json` 三种都被丢 ⇒ 只 `Delta("走")` |

#### 3.4.4 事件契约（外部 `core/sse`，行号实证）

`AiStreamEvent` 定义在 `:core:network` 模块的 `core.sse` 包（`core/network/src/commonMain/kotlin/com.example.my_kmp_project/core/sse/AiStreamEvent.kt`）：`sealed interface` `:8`、`Meta(requestId, conversationId, model, clientRequestId)` `:11-16`、`Delta(text)` `:19-21`、`Done(finishReason)` `:24-26`、`Error(code, message)` `:32-35`（`:28-31` 注释：Dart 叫 `AiStreamError`，嵌在这里以免遮蔽 `kotlin.Error`）。

`SseFrame`（`core/network/src/commonMain/kotlin/com.example.my_kmp_project/core/sse/SseFrame.kt:12-18`）：`event` 默认 `DEFAULT_EVENT = "message"` `:18`。
`SseParser`（`core/network/src/commonMain/kotlin/com.example.my_kmp_project/core/sse/SseParser.kt:15`）：`feed(chunk: String): List<SseFrame>` `:34`、`flush(): List<SseFrame>` `:65`。
`toAiStreamEventOrNull()`（`core/network/src/commonMain/kotlin/com.example.my_kmp_project/core/sse/AiSseDecoder.kt:36-62`）：`type = if (event.isNotEmpty()) event else payload?.optString("type") ?: "message"` `:41`（**`event:` 优先于 payload `type`**，`:31-34` 注明这是 Dart 怪癖）；`meta/delta/done/error` 四分支 `:44-58`，`else -> null` `:60`；`delta` 空文本 → null `:51-54`；`decodeError` `:64-79` 依次吃 `error` 对象（`code`/`message`）、非对象 `error`、payload `message`，兜底常量 `DEFAULT_ERROR_CODE = "stream_error"` `:15`、`DEFAULT_ERROR_MESSAGE = "生成失败"` `:16`。


## 4. commerce / wallet 专题：端点 · 分页 · 金额

### 4.1 商品 / 订单 / 余额 / 流水 各走哪个端点（method → 行号）

| 业务数据 | method + path | 发起处（file:line） | path 常量出处 |
|---|---|---|---|
| 商品（货架） | `GET /api/v1/mall/stores/{storeId}/products?page&size` | `feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MallApi.kt:38-41` | `feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MallApi.kt:234` |
| 商品（详情） | `GET /api/v1/mall/stores/{storeId}/products/{productId}` | `feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MallApi.kt:50-52` | `feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MallApi.kt:236-237` |
| 商品（积分商城投影） | 同货架端点，`size=20` + 客户端过滤 | `feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MallApi.kt:176-179` | `feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MallApi.kt:234` |
| 购物车 | `POST /api/v1/mall/cart` | `feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MallApi.kt:58-59`（经 `requireRawMapPost` `:186-189`） | `feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MallApi.kt:239` |
| 订单（创建） | `POST /api/v1/mall/orders` | `feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MallApi.kt:77-87` | `feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MallApi.kt:241` |
| 订单（列表） | `GET /api/v1/mall/orders?page&size&status` | `feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MallApi.kt:132-135` | `feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MallApi.kt:241` |
| 订单（详情） | `GET /api/v1/mall/orders/{orderId}` | `feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MallApi.kt:141` | `feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MallApi.kt:243` |
| 订单（支付标记） | `POST /api/v1/mall/orders/{orderId}/pay` | `feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MallApi.kt:147-150` | `feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MallApi.kt:245` |
| 订单（取消） | `POST /api/v1/mall/orders/{orderId}/cancel`（空 body） | `feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MallApi.kt:159` | `feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MallApi.kt:247` |
| 余额 / 卡列表（全量） | `GET /api/v1/wallet` | `feature/wallet/src/commonMain/kotlin/com.example.my_kmp_project/feature/wallet/WalletApi.kt:32` | `feature/wallet/src/commonMain/kotlin/com.example.my_kmp_project/feature/wallet/WalletApi.kt:133` |
| 余额（只读、可空） | `GET /api/v1/wallet` | `feature/wallet/src/commonMain/kotlin/com.example.my_kmp_project/feature/wallet/WalletApi.kt:45` | `feature/wallet/src/commonMain/kotlin/com.example.my_kmp_project/feature/wallet/WalletApi.kt:133` |
| 流水 | `GET /api/v1/wallet/ledger?limit&offset` | `feature/wallet/src/commonMain/kotlin/com.example.my_kmp_project/feature/wallet/WalletApi.kt:64-67` | `feature/wallet/src/commonMain/kotlin/com.example.my_kmp_project/feature/wallet/WalletApi.kt:134` |
| 绑卡 | `POST /api/v1/wallet/cards` | `feature/wallet/src/commonMain/kotlin/com.example.my_kmp_project/feature/wallet/WalletApi.kt:82-90` | `feature/wallet/src/commonMain/kotlin/com.example.my_kmp_project/feature/wallet/WalletApi.kt:135` |
| 设默认卡 | `POST /api/v1/wallet/cards/{cardId}/default`（空 body） | `feature/wallet/src/commonMain/kotlin/com.example.my_kmp_project/feature/wallet/WalletApi.kt:100` | `feature/wallet/src/commonMain/kotlin/com.example.my_kmp_project/feature/wallet/WalletApi.kt:140` |
| 删卡 | `DELETE /api/v1/wallet/cards/{cardId}` | `feature/wallet/src/commonMain/kotlin/com.example.my_kmp_project/feature/wallet/WalletApi.kt:108` | `feature/wallet/src/commonMain/kotlin/com.example.my_kmp_project/feature/wallet/WalletApi.kt:138` |
| 充值 | `POST /api/v1/wallet/recharge` | `feature/wallet/src/commonMain/kotlin/com.example.my_kmp_project/feature/wallet/WalletApi.kt:118-121` | `feature/wallet/src/commonMain/kotlin/com.example.my_kmp_project/feature/wallet/WalletApi.kt:136` |

四模块合计 16 个 HTTP 端点 + 1 个 SSE 端点；**除 `MallApi.buy` 内部串两次（#30→#33）外，没有任何方法把两条支付链路合起来**。

### 4.2 分页（以代码为准）

| 场景 | 游标形态 | 页长 | 还有下一页 | 证据 |
|---|---|---|---|---|
| 商城货架 / 订单列表 | `page` **1-based** + `size` | `MallApi.PAGE_SIZE = 10`（`feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MallApi.kt:194`），调用方可覆盖 `feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MallApi.kt:36`、`:129` | `ListPage.hasMore(pageSize)`：有 `pagination` 用服务端 `hasMore`（`totalPages>0 → page<totalPages`；否则 `page*size<total`；再否则 false），无 `pagination` 退化为「本页拿满 = 还有下一页」（`core/network/src/commonMain/kotlin/com.example.my_kmp_project/core/network/ListPage.kt:27-32`、`:43-44`） | `pageQuery` 丢弃 null 与空串（`core/network/src/commonMain/kotlin/com.example.my_kmp_project/core/network/ListPage.kt:92-103`）；`decodeListPage` 同时吃 `data` 为数组、`list`、`items` 三种形状（`core/network/src/commonMain/kotlin/com.example.my_kmp_project/core/network/ListPage.kt:52-63`）；`totalPages` / `total_pages` 等价取先出现者（`core/network/src/commonMain/kotlin/com.example.my_kmp_project/core/network/ListPage.kt:77`） |
| 积分商城投影 | `page`/`size` 参数存在但**默认恒为第 1 页 20 条**（`feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MallApi.kt:171-174`） | `POINTS_GIFT_PAGE_SIZE = 20`（`feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MallApi.kt:197`） | **不做 hasMore 判断**：返回值是 `List`，翻页信息被丢弃（`feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MallApi.kt:223-224` 只 filter） | `feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MallApi.kt:181` 只读 `response.data` |
| 钱包流水 | `limit` / `offset`（**0-based 偏移**，非 page） | `LEDGER_LIMIT = 20`（`feature/wallet/src/commonMain/kotlin/com.example.my_kmp_project/feature/wallet/WalletApi.kt:127`），`offset` 默认 0（`feature/wallet/src/commonMain/kotlin/com.example.my_kmp_project/feature/wallet/WalletApi.kt:63`） | **无**：响应形状 `data.items[]` 不带 `pagination`，游标由调用方自行推进（`feature/wallet/src/commonMain/kotlin/com.example.my_kmp_project/feature/wallet/WalletApi.kt:48-53` KDoc；测试 `feature/wallet/src/commonTest/kotlin/com.example.my_kmp_project/feature/wallet/WalletApiContractTest.kt:107-111`） | 返回类型是 `List<WalletLedgerEntry>`（`feature/wallet/src/commonMain/kotlin/com.example.my_kmp_project/feature/wallet/WalletApi.kt:63`），分页壳被丢弃 |
| 会员目录（pay） | 无分页 | — | — | `feature/pay/src/commonMain/kotlin/com.example.my_kmp_project/feature/pay/PayApi.kt:41-42` 一次性返回 `catalog` 全量 |

### 4.3 金额处理（单位 / 格式化 / 四舍五入）

| 数据 | 单位与类型 | 格式化实现 | 四舍五入 |
|---|---|---|---|
| 商城商品价 `price` | **BFF 字符串「元」**，KMP 不 Double 化（`feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MallProductModels.kt:33-34`） | `priceLabel` 直接拼接原串：`"${price}元"`（`:84-93`）；`MallSkuOffer.priceLabel` 同规则（`:130-138`） | **无**：展示零处理，`"39.9"` 与 `"39.90"` 会原样显示 |
| 商品积分价 `price_points` | Int | `points = takeIf { > 0 }`（`:51-52`）、徽标判定用 `price.toDoubleOrNull() ?: 0.0` 只为判断「是否 >0」（`:79-80`、`:87`） | 无 |
| 伪销量 `soldLabel` | 派生整数 20…10009 | ≥10000 走 `fixedOneDecimal(soldRaw / 10000.0)` + `"万"`（`:64-73`） | `fixedOneDecimal` 用 `kotlin.math.round(value*10).toLong()` ⇒ **四舍五入到 1 位小数**（`:97-102`） |
| 订单金额 `amount` / 行金额 `line_amount` | BFF 字符串「元」，默认 `"0.00"`（`feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MallOrderModels.kt:45`、`:26`） | 无格式化，摊平属性原样透出（`feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MallOrderModels.kt:72`） | 无；`totalPoints` 是 Int 直接透出（`:79`） |
| 钱包余额 | **双字段**：`balance:String`（元）+ `balance_fen:Int`（分）（`feature/wallet/src/commonMain/kotlin/com.example.my_kmp_project/feature/wallet/WalletModels.kt:17-18`、`:74-77`） | `WalletSummary.balance` 原串展示；成功但缺键时取默认 `"0.00"`（`feature/wallet/src/commonMain/kotlin/com.example.my_kmp_project/feature/wallet/WalletApi.kt:43-46` + `feature/wallet/src/commonMain/kotlin/com.example.my_kmp_project/feature/wallet/WalletModels.kt:17`） | 无 |
| 流水增量 `delta_fen` | 整数分，可负 | `deltaYuan`：整型除法 + `padStart(2,'0')` + 强制 `+`/`-` 前缀（`feature/wallet/src/commonMain/kotlin/com.example.my_kmp_project/feature/wallet/WalletModels.kt:54-60`） | **无浮点**：`:50-53` KDoc 说明刻意避开 `String.format`/Locale 差异；截断即精确（分是整数） |
| 充值入参 `amount` | **十进制元串**（`"12.34"`），不是分（`feature/wallet/src/commonMain/kotlin/com.example.my_kmp_project/feature/wallet/WalletApi.kt:112-116`、`feature/wallet/src/commonMain/kotlin/com.example.my_kmp_project/feature/wallet/WalletModels.kt:92-98`） | 无（原串进 body）；测试锁死 `{"amount":"12.34","channel":1}`（`feature/wallet/src/commonTest/kotlin/com.example.my_kmp_project/feature/wallet/WalletApiContractTest.kt:174-184`） | 无 |
| 会员卡展示 `display` | 无金额 | `"$bankName(****$cardLast4)"`（`feature/wallet/src/commonMain/kotlin/com.example.my_kmp_project/feature/wallet/WalletModels.kt:35`） | — |
| 会员套餐价（pay） | `price:String` + `price_fen:Int`（`feature/pay/src/commonMain/kotlin/com.example.my_kmp_project/feature/pay/MembershipModels.kt:66-67`） | `priceValue = price.toDoubleOrNull() ?: (priceFen / 100.0)`（`:77`）、`originalPriceValue = priceValue`（`:80`） | `priceFen / 100.0` 是 Double 除法，**不做小数位约束**（测试用 `29.9` 带 1e-9 容差，`feature/pay/src/commonTest/kotlin/com.example.my_kmp_project/feature/pay/MembershipPayContractTest.kt:54`） |
| 会员套餐价（commerce mock） | `price: Double` / `originalPrice: Double` 硬编码（`feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MembershipCatalog.kt:70-124`） | **未找到格式化函数**：`MembershipPlan` 只有裸 Double，无任何 `priceLabel`/`toStringAsFixed` 类派生 | 未找到。命令 `grep -rn --include='*.kt' -E 'String.format\\|toFixed\\|round\(\\|formatCurrency' feature/commerce feature/wallet feature/pay` → 只有 3 处命中且均非金额格式化：`feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MembershipScreen.kt:50`（`background(` 含子串 `round(`）、`feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MallProductModels.kt:96`（注释里提到 Dart `toStringAsFixed`）、`feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MallProductModels.kt:98`（唯一的 `kotlin.math.round`，只服务 `soldLabel`） |
| `MembershipCatalog.DeductionAmount = 12.0` | Double 常量（`feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MembershipCatalog.kt:68`） | 无消费方（见 3.1.5） | 未找到 |

## 5. pay 专题：PayGateway 与三端适配器真实进度（诚实版）

### 5.1 三层支付链路各自的边界

| 层 | 代码位置 | 做到哪一步 | 明显没做的事 |
|---|---|---|---|
| **A. `:feature:pay`（BFF 契约 + 编排）** | `feature/pay/src/commonMain/kotlin/com.example.my_kmp_project/feature/pay/PayApi.kt:37-212` | 5 个 HTTP 端点全部实现并有测试；`payBuyout` 完整复刻两步买断（含 `paid` 短路、`order_id<=0` 静默跳过、异常折成结果对象） | 真正的 SDK 调用**以函数参数形式留白**：`sdkPay: suspend (MembershipBuyoutResponse) -> Boolean`（`feature/pay/src/commonMain/kotlin/com.example.my_kmp_project/feature/pay/PayApi.kt:156`）；`prepay` 端点故意不落地（`feature/pay/src/commonMain/kotlin/com.example.my_kmp_project/feature/pay/PayApi.kt:32-35`） |
| **B. `:component`（UI 侧端口）** | `component/src/commonMain/kotlin/com.example.my_kmp_project/component/pay/PayGateway.kt` | 端口 / 结果 / flag / 沙箱适配器 / `FlaggedPayGateway` 判定逻辑齐全（`:4-129`） | `pay()` **不发任何 HTTP**、不知道订单号与金额、不接 `PayApi`（`:6` 签名只有 `channel` + `planId`） |
| **C. `:feature:commerce` UI** | `feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MembershipScreen.kt:69-92` | 唯一把「点按钮 → PayGateway → toast」串起来的代码 | 不接 `PayApi`：既不 `buyout` 也不 `confirm`，因此**服务端会员订单从未被创建**（`grep -rn --include='*.kt' 'PayApi' feature/commerce` → 无命中） |

A 与 B 两条线**从未在仓库里合拢**：`grep -rn --include='*.kt' -E 'feature\.pay|PayGateway' component composeApp feature/commerce` 的命中集合里，`PayGateway` 只出现在 `component` 自身与 `feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MembershipScreen.kt:24-27,42`，`PayApi` 只出现在 `feature/pay` 内部。

### 5.2 真实支付 SDK 接没接（逐条）

| SDK | 结论 | 证据 file:line |
|---|---|---|
| 微信 OpenSDK（Android） | **未接**，函数体是 TODO + `return null` | `component/src/androidMain/kotlin/com.example.my_kmp_project/component/pay/PlatformPayAdapters.android.kt:7-10`（`:8` `// TODO(pay): return WeChatOpenSdkPayAdapter(appId, api) when OpenSDK + keys are configured`） |
| 支付宝 SDK（Android） | **未接**，同上 | `…PlatformPayAdapters.android.kt:16-19`（`:17` `// TODO(pay): return AlipaySdkPayAdapter(...) when Alipay SDK + keys are configured`） |
| 微信（iOS） | **未接** | `component/src/iosMain/kotlin/com.example.my_kmp_project/component/pay/PlatformPayAdapters.ios.kt:4`（`= null`，注释「null until WXApi is wired」） |
| 支付宝（iOS） | **未接** | `…PlatformPayAdapters.ios.kt:7`（`= null`） |
| OHOS（两侧） | **未接** | `component/src/ohosMain/kotlin/com.example.my_kmp_project/component/pay/PlatformPayAdapters.ohos.kt:7`、`:9`（均 `= null`；`:3-6` 注释说明本 source set 无 Android OpenSDK） |
| Apple IAP（StoreKit） | **未接**；HTTP 验单有、结单本体缺 | `feature/pay/src/commonMain/kotlin/com.example.my_kmp_project/feature/pay/PayApi.kt:110`（`GAP(R5): StoreKit 桥接本体属平台 SDK lane，本层只回传标志位`）、`:131`；仓库内 `verifyApple` 无生产调用方（同 `:3.3.4` grep） |
| Huawei IAP | **未接**；三处 `GAP(R5)` | `feature/pay/src/commonMain/kotlin/com.example.my_kmp_project/feature/pay/PayApi.kt:77`、`:86`、`:102`（「需真实 HuaweiIapBridge（ohosMain SDK lane），本层不代跑」）；`restoreHuaweiPurchases()` **只出现在 KDoc** `feature/pay/src/commonMain/kotlin/com.example.my_kmp_project/feature/pay/PayApi.kt:75`，仓库内无该函数定义（`grep -rn --include='*.kt' restoreHuaweiPurchases .` → 唯一命中即 `feature/pay/src/commonMain/kotlin/com.example.my_kmp_project/feature/pay/PayApi.kt:75` 这行注释） |
| 微信 / 支付宝 SDK 交接点（buyout 流程内） | **未接**，只吃注入的布尔 | `feature/pay/src/commonMain/kotlin/com.example.my_kmp_project/feature/pay/PayApi.kt:148-151`（`GAP(R5): sdkPay 就是微信/支付宝 SDK 的交接点…由平台 SDK lane 注入真实实现`）、`:168`；Dart 的「微信未配置 ⇒ LaunchFailed 提示填 WysWechatConfig」前置检查也**未实现**（`:150-151` 明说留给平台 lane） |

### 5.3 哪些是占位 / 假成功

| 占位物 | 位置 | 行为 |
|---|---|---|
| `SandboxPayChannelAdapter` | `component/src/commonMain/kotlin/com.example.my_kmp_project/component/pay/PayGateway.kt:64-69` | `pay()` **无条件** `Success(channel, planId, sandbox = true)` `:68` —— 仓库唯一「支付成功」来源 |
| `PayFeatureFlags.sandboxEnabled = true`（默认） | `component/src/commonMain/kotlin/com.example.my_kmp_project/component/pay/PayGateway.kt:48-52` | 沙箱默认开、两个真实渠道 flag 默认 false；`:44-46` 注释说明「spike 调试默认开，接真实 SDK 后 release 关闭」 |
| `PlatformPayAdapters` 回落 | `component/src/commonMain/kotlin/com.example.my_kmp_project/component/pay/PayGateway.kt:83-94` | 平台 actual 为 null ⇒ 用沙箱适配器顶上 `:92-93` |
| `StubPayGateway` | `component/src/commonMain/kotlin/com.example.my_kmp_project/component/pay/PayGateway.kt:33-38` | 恒 `Unavailable`，供测试/CI；`:29-32` 注明「绝不伪造 Success」 |
| `MembershipScreen` 的 CTA | `feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MembershipScreen.kt:77-89` | `planId` 写死 `"svip_1m"` `:80`；`Success` 分支按 `sandbox` 分别 toast「沙箱支付成功（非真实扣款）」/「支付成功」`:83-84` |
| 会员页视觉 | `feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MembershipScreen.kt:52-57` | 整页是**一张静态 SoT 图**，套餐/抵扣/渠道不可交互、不可选，`MembershipCatalog` 的数据没被渲染（3.1.6） |
| `MallApi.payOrder` | `feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MallApi.kt:146-152` | 只是「POST 通知服务端我付了」，与 SDK 无耦合 ⇒ 商城侧「支付成功」是**服务端状态位**，不是客户端扣款证据 |
| `:feature:pay` 整体 | 3.3.4 | HTTP 契约先行、**未接线**（零外部 import） |

当前端到端真实结论：Android / iOS / OHOS 三端点击会员 CTA，都会走 `FlaggedPayGateway`（`feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MembershipScreen.kt:42` 默认）→ 沙箱适配器 → `Success(sandbox=true)` → toast「沙箱支付成功（非真实扣款）」；**没有任何路径能产生真实扣款**，也没有任何路径把 `PayApi.buyout/confirm` 打出去。

## 6. ai 专题：SSE 全链路

### 6.1 POST 是怎么发出去的

调用链：`AiSseApi.streamCompletion`（`feature/ai/src/commonMain/kotlin/com.example.my_kmp_project/feature/ai/AiSseApi.kt:51-67`）→ `SseTransport.openSseTextStream(path, body, headers)`（接口 `core/network/src/commonMain/kotlin/com.example.my_kmp_project/core/network/Sse.kt:54-65`）→ Ktor actual `core/network/src/networkKtorMain/kotlin/com.example.my_kmp_project/core/network/KtorApiClient.kt:106-156`。

| 步骤 | 真实代码 | file:line |
|---|---|---|
| 组 body | `buildJsonObject { put("prompt", …); 两个可选 id 非空才 put }`，经 `NetworkJson.encodeToString(JsonObject.serializer(), …)` | `feature/ai/src/commonMain/kotlin/com.example.my_kmp_project/feature/ai/AiSseApi.kt:74-85` |
| 请求形态 | `http.preparePost(path.trimStart('/'))`；`expectSuccess = false`；`header(Accept, SSE_ACCEPT_HEADER_VALUE)`（值 `"text/event-stream"`，`core/network/src/commonMain/kotlin/com.example.my_kmp_project/core/network/Sse.kt:21`）；`headers` 追加可覆盖同名；`contentType(ContentType.Application.Json)`；`setBody(body)` | `core/network/src/networkKtorMain/kotlin/com.example.my_kmp_project/core/network/KtorApiClient.kt:111-121`、`:113` |
| 流式出口 | 必须 `statement.execute { response -> … }`（不带 block 会把整包读进内存，`:100-101` 注释） | `core/network/src/networkKtorMain/kotlin/com.example.my_kmp_project/core/network/KtorApiClient.kt:123` |
| 增量读 | `response.bodyAsChannel()` + `ByteArray(SSE_READ_WINDOW_BYTES = 8 * 1024)`（`core/network/src/networkKtorMain/kotlin/com.example.my_kmp_project/core/network/KtorApiClient.kt:35`）循环 `readAvailable`，`read < 0` 退出、`read == 0` 继续；每段过 `Utf8ChunkDecoder.decode`，非空才 `emit`；收尾 `decoder.finish()` 的尾巴再 emit | `core/network/src/networkKtorMain/kotlin/com.example.my_kmp_project/core/network/KtorApiClient.kt:124-137` |
| UTF-8 增量 | 有状态解码器，跨分片多字节字符留尾字节；非法字节用 U+FFFD（对齐 Dart `allowMalformed: true`） | `core/network/src/commonMain/kotlin/com.example.my_kmp_project/core/network/Sse.kt:184-194`（`Utf8ChunkDecoder` `:194`，`decode` `:198`、`finish` `:252`） |
| 前流判定 | `SseOpenGate(statusCode, contentType, chunks)`：`statusCode == SSE_UPGRADE_STATUS(200, core/network/src/commonMain/kotlin/com.example.my_kmp_project/core/network/Sse.kt:27)` **且** `contentType` 含 `text/event-stream`（忽略大小写）⇒ 原样返回分片流，否则 `readAllText()` 读净后 `throw SseException.fromPreStream(...)` | `core/network/src/commonMain/kotlin/com.example.my_kmp_project/core/network/Sse.kt:161-182`、`:167-168`、`:171-174`；Ktor 侧挂载点 `core/network/src/networkKtorMain/kotlin/com.example.my_kmp_project/core/network/KtorApiClient.kt:138-145` |
| 会话头 | `Accept` 之外不手动加：Bearer / `X-Session-ID` 由平台 client 既有拦截逻辑注入 | `core/network/src/commonMain/kotlin/com.example.my_kmp_project/core/network/Sse.kt:55-59`（KDoc） |
| token 刷新装饰 | `TokenRefresh` 装饰器把 `openSseTextStream` 原样委托给内层 | `core/network/src/commonMain/kotlin/com.example.my_kmp_project/core/network/TokenRefresh.kt:121-125` |

### 6.2 SSE 帧如何驱动 UI 流式输出

1. 帧协议解析：`SseParser`（`core/network/src/commonMain/kotlin/com.example.my_kmp_project/core/sse/SseParser.kt:15`）是**有状态行缓冲**，`feed(chunk): List<SseFrame>` `:34` 负责跨分片切行，`flush(): List<SseFrame>` `:65` 收尾；分片边界与 TCP/HTTP 块一致、不保证按行（`core/network/src/commonMain/kotlin/com.example.my_kmp_project/core/network/Sse.kt:41-42` KDoc）。`feature/ai/src/commonMain/kotlin/com.example.my_kmp_project/feature/ai/AiSseApi.kt:57` 每次 collect 新建 parser，保证一次提问一条流。
2. 帧 → 领域事件：`toAiStreamEventOrNull()`（`core/network/src/commonMain/kotlin/com.example.my_kmp_project/core/sse/AiSseDecoder.kt:36-62`）。`event:` 优先于 payload `type`（`:41`），四类映射 `meta/delta/done/error`（`:44-58`），其余 `null` 丢弃（`:60`）；空 `delta.text` 丢弃（`:51-54`）；无法解析/非 object 的 `data` → `payload == null`（`:81-82`）同样不产出内容。
3. `emit`：`feature/ai/src/commonMain/kotlin/com.example.my_kmp_project/feature/ai/AiSseApi.kt:60` 与 `:65` 逐帧 `emit`，因此 `Flow<AiStreamEvent>` 的元素顺序 = 帧顺序 = UI 追加顺序；`Meta` 携 4 个标识（`core/network/src/commonMain/kotlin/com.example.my_kmp_project/core/sse/AiStreamEvent.kt:11-16`）、`Delta.text` 是**增量文本**（`:19-21`，UI 侧应 append）、`Done.finishReason`（`:24-26`）。
4. **UI 侧当前不消费**：`AiStreamScreen`（`feature/ai/src/commonMain/kotlin/com.example.my_kmp_project/feature/ai/AiStreamScreen.kt:39-81`）没有 `collect`/`collectAsState`/`LaunchedEffect`（import 列表 `:3-31` 无 `kotlinx.coroutines.flow.*`、无 `androidx.compose.runtime.collectAsState`），发送键只弹 toast `:78`。⇒ 流式输出在 UI 层**尚未落地**，链路停在「接口层可用 + 测试覆盖」。搜索证据：`grep -rn --include='*.kt' -E 'streamCompletion|AiSseApi' feature composeApp` → 只命中 `feature/ai/src/commonMain/kotlin/com.example.my_kmp_project/feature/ai/AiSseApi.kt` 自身与 `feature/ai/src/commonTest/kotlin/com.example.my_kmp_project/feature/ai/AiSseApiTest.kt`。

### 6.3 取消 / 错误 / 超时

| 维度 | 真实处理 | file:line |
|---|---|---|
| 取消 | 冷流：collect 才发请求（`feature/ai/src/commonMain/kotlin/com.example.my_kmp_project/feature/ai/AiSseApi.kt:35-38` KDoc、`feature/ai/src/commonTest/kotlin/com.example.my_kmp_project/feature/ai/AiSseApiTest.kt:138-141` 断言）；取消 = 取消收集协程（`core/network/src/commonMain/kotlin/com.example.my_kmp_project/core/network/Sse.kt:52`）；Ktor 侧 `catch (t: Throwable)` 时 **`CancellationException` 原样传播**（注释「取消是正常出口（用户『停止生成』），Dart CancelToken 同义」） | `core/network/src/networkKtorMain/kotlin/com.example.my_kmp_project/core/network/KtorApiClient.kt:147-149` |
| 前流错误 | `SseOpenGate.eventStreamText()` 抛 `SseException.fromPreStream(status, 读净的 body)`：`{"error":…}` 优先取 `error`，否则取 `message`，再否则整段正文；空正文 ⇒ `"SSE request failed ($status)"`；`error` 码缺失时按 status 走 `defaultErrorCode` | `core/network/src/commonMain/kotlin/com.example.my_kmp_project/core/network/Sse.kt:171-174`、`:91-109`、`:114`、`:125`；feature 侧语义说明 `feature/ai/src/commonMain/kotlin/com.example.my_kmp_project/feature/ai/AiSseApi.kt:40-45`；测试 `feature/ai/src/commonTest/kotlin/com.example.my_kmp_project/feature/ai/AiSseApiTest.kt:114-131` |
| 流内错误 | **只允许** `event: error` 帧 ⇒ `AiStreamEvent.Error(code, message)`，不抛（`feature/ai/src/commonMain/kotlin/com.example.my_kmp_project/feature/ai/AiSseApi.kt:46-48`；`core/network/src/commonMain/kotlin/com.example.my_kmp_project/core/network/Sse.kt:49-51`）；解码兜底 `stream_error` / `生成失败` | `core/network/src/commonMain/kotlin/com.example.my_kmp_project/core/sse/AiSseDecoder.kt:58`、`:64-79`、常量 `:15-16`；测试 `feature/ai/src/commonTest/kotlin/com.example.my_kmp_project/feature/ai/AiSseApiTest.kt:91-111` |
| 连接层异常 | 非取消、非 `SseException` 的 `Throwable` 一律折成 `SseException(message = t.message ?: "SSE connection failed", statusCode = t.transportStatus(), error = SSE_ERROR_CONNECTION("connection_error", `core/network/src/commonMain/kotlin/com.example.my_kmp_project/core/network/Sse.kt:30`))`；**不混进 `NetworkError.Transport`** | `core/network/src/networkKtorMain/kotlin/com.example.my_kmp_project/core/network/KtorApiClient.kt:150-154`、`:104` |
| 超时 | **明确不设超时**：`requestTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS`、`socketTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS` | `core/network/src/networkKtorMain/kotlin/com.example.my_kmp_project/core/network/KtorApiClient.kt:116-119`；feature 层无任何 timeout/retry/心跳代码——`grep -rn --include='*.kt' -E 'timeout\\|retry\\|withTimeout' feature/ai` 只命中测试夹具里的错误码字符串 `upstream_timeout`（`feature/ai/src/commonTest/kotlin/com.example.my_kmp_project/feature/ai/AiSseApiTest.kt:97`、`:107`），不是超时实现 |
| 保活/杂帧 | `: keep-alive` 注释帧、`event: ping`、`data: not-json` 均被丢（不报错） | 测试 `feature/ai/src/commonTest/kotlin/com.example.my_kmp_project/feature/ai/AiSseApiTest.kt:174-185` |
| 截断尾帧 | EOF 时未闭合到空行的半帧被丢弃，不把截断 JSON 当事件发出 | `AiSseParser` 收尾语义 + 测试 `feature/ai/src/commonTest/kotlin/com.example.my_kmp_project/feature/ai/AiSseApiTest.kt:164-171` |
| 平台不支持 | `ApiClient` 默认实现抛 `SseStreamingUnsupportedException`；**ohosX64** 的 `net_http` cinterop 只在完成回调给整包 body，无法增量 ⇒ 覆写为直接抛 | `core/network/src/commonMain/kotlin/com.example.my_kmp_project/core/network/ApiClient.kt:59-66`；`core/network/src/ohosX64Main/kotlin/com.example.my_kmp_project/core/network/OhosApiClient.kt:64-68`（`:63` 注释：ohosArm64 走 `networkKtorMain` 的 Ktor 路径才支持流式）；异常类定义 `core/network/src/commonMain/kotlin/com.example.my_kmp_project/core/network/Sse.kt:142` |

### 6.4 ADR 0005「SSE 只服务 AI 补全」在代码里的体现

| 边界条款 | 代码证据 |
|---|---|
| AI 流式只走 SSE，端点固定 `POST /api/v1/sse/completions` | 路径常量唯一：`feature/ai/src/commonMain/kotlin/com.example.my_kmp_project/feature/ai/AiSseApi.kt:32`；`streamCompletion` 里唯一使用点 `:58`（无任何其它 path） |
| 一次提问一条流 | `SseParser()` 在 flow builder 内现场创建（`feature/ai/src/commonMain/kotlin/com.example.my_kmp_project/feature/ai/AiSseApi.kt:57`），不复用、不共享 |
| 请求体 camelCase，禁止顺手统一成 snake_case | `feature/ai/src/commonMain/kotlin/com.example.my_kmp_project/feature/ai/AiSseApi.kt:24-25`、`:70-73`、实现 `:82-83`；测试 `feature/ai/src/commonTest/kotlin/com.example.my_kmp_project/feature/ai/AiSseApiTest.kt:134-161`（含中文 prompt 与两种省略规则） |
| 禁止用 WS 承载 AI token 流 / `feature/ai` 不需要 `:component:realtime` | `feature/ai/build.gradle.kts:14-18` 依赖只有 `core.ui` / `core.network` / `core.router` / serialization，**没有 `api(projects.component)`**（对比 `feature/commerce/build.gradle.kts:20` 有）；`grep -rn --include='*.kt' -iE 'realtime\\|websocket' feature/ai` → 无命中 |
| 协程取消即断流（不引入自定义 CancelToken） | `feature/ai/src/commonMain/kotlin/com.example.my_kmp_project/feature/ai/AiSseApi.kt:38` KDoc；`core/network/src/commonMain/kotlin/com.example.my_kmp_project/core/network/Sse.kt:52`；`core/network/src/networkKtorMain/kotlin/com.example.my_kmp_project/core/network/KtorApiClient.kt:148-149` 传播 `CancellationException` |
| 三条通道失败语义不许互相翻译 | 双通道写死在接口层 KDoc（`feature/ai/src/commonMain/kotlin/com.example.my_kmp_project/feature/ai/AiSseApi.kt:40-49`）与实现（前流靠传输层抛 `SseException`、流内靠 `AiStreamEvent.Error`）；`core/network/src/commonMain/kotlin/com.example.my_kmp_project/core/network/Sse.kt:49-51` 明确「升级后传输层不翻译错误，那是 AiSseDecoder 的事」；测试各锁一条（`feature/ai/src/commonTest/kotlin/com.example.my_kmp_project/feature/ai/AiSseApiTest.kt:91-111` 与 `:114-131`） |

ADR 原文：`docs/adr/0005-three-long-lived-channels-separate-transports.md:17`、`:21`、`:24`、`:30`。

## 7. 端点消费表 + 真实状态矩阵

### 7.1 端点消费表（16 HTTP + 1 SSE）

| # | method + path | 生产方法（file:line） | 生产调用方 | 契约测试 |
|---|---|---|---|---|
| 27 | `GET /api/v1/mall/stores/{s}/products` | `feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MallApi.kt:33-43` | **无（未接线）** | `feature/commerce/src/commonTest/kotlin/com.example.my_kmp_project/feature/commerce/MallApiContractTest.kt:32` |
| 28 | `GET /api/v1/mall/stores/{s}/products/{p}` | `feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MallApi.kt:46-54` | 无 | `feature/commerce/src/commonTest/kotlin/com.example.my_kmp_project/feature/commerce/MallApiContractTest.kt:79` |
| 29 | `POST /api/v1/mall/cart` | `feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MallApi.kt:57-60` | 无 | `feature/commerce/src/commonTest/kotlin/com.example.my_kmp_project/feature/commerce/MallApiContractTest.kt:272` |
| 30 | `POST /api/v1/mall/orders` | `feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MallApi.kt:69-95`（+ `buy` `:100-118`） | 无 | `feature/commerce/src/commonTest/kotlin/com.example.my_kmp_project/feature/commerce/MallApiContractTest.kt:107`、`:148` |
| 31 | `GET /api/v1/mall/orders` | `feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MallApi.kt:127-137` | 无 | `feature/commerce/src/commonTest/kotlin/com.example.my_kmp_project/feature/commerce/MallApiContractTest.kt:170`、`:192`、`:212` |
| 32 | `GET /api/v1/mall/orders/{id}` | `feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MallApi.kt:140-143` | 无 | `feature/commerce/src/commonTest/kotlin/com.example.my_kmp_project/feature/commerce/MallApiContractTest.kt:227` |
| 33 | `POST /api/v1/mall/orders/{id}/pay` | `feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MallApi.kt:146-152` | 无 | `feature/commerce/src/commonTest/kotlin/com.example.my_kmp_project/feature/commerce/MallApiContractTest.kt:252` |
| 34 | `POST /api/v1/mall/orders/{id}/cancel` | `feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MallApi.kt:158-160` | 无 | `feature/commerce/src/commonTest/kotlin/com.example.my_kmp_project/feature/commerce/MallApiContractTest.kt:252` |
| 26 | `GET /api/v1/mall/stores/1/products`（积分投影） | `feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MallApi.kt:171-182` | 无 | `feature/commerce/src/commonTest/kotlin/com.example.my_kmp_project/feature/commerce/MallApiContractTest.kt:332`、`:344`、`:352` |
| 84 | `GET /api/v1/wallet`（汇总） | `feature/wallet/src/commonMain/kotlin/com.example.my_kmp_project/feature/wallet/WalletApi.kt:31-34` | 无（钱包 UI 走 `feature/mine` mock） | `feature/wallet/src/commonTest/kotlin/com.example.my_kmp_project/feature/wallet/WalletApiContractTest.kt:27`、`:49` |
| 84′ | `GET /api/v1/wallet`（只读余额） | `feature/wallet/src/commonMain/kotlin/com.example.my_kmp_project/feature/wallet/WalletApi.kt:43-46` | 无 | `feature/wallet/src/commonTest/kotlin/com.example.my_kmp_project/feature/wallet/WalletApiContractTest.kt:49`、`:73` |
| 85 | `GET /api/v1/wallet/ledger` | `feature/wallet/src/commonMain/kotlin/com.example.my_kmp_project/feature/wallet/WalletApi.kt:63-73` | 无 | `feature/wallet/src/commonTest/kotlin/com.example.my_kmp_project/feature/wallet/WalletApiContractTest.kt:82`、`:107`、`:114`、`:122` |
| 86 | `POST /api/v1/wallet/cards` | `feature/wallet/src/commonMain/kotlin/com.example.my_kmp_project/feature/wallet/WalletApi.kt:76-92` | 无 | `feature/wallet/src/commonTest/kotlin/com.example.my_kmp_project/feature/wallet/WalletApiContractTest.kt:139` |
| 87 | `POST /api/v1/wallet/cards/{id}/default` | `feature/wallet/src/commonMain/kotlin/com.example.my_kmp_project/feature/wallet/WalletApi.kt:98-104` | 无 | `feature/wallet/src/commonTest/kotlin/com.example.my_kmp_project/feature/wallet/WalletApiContractTest.kt:150` |
| 88 | `DELETE /api/v1/wallet/cards/{id}` | `feature/wallet/src/commonMain/kotlin/com.example.my_kmp_project/feature/wallet/WalletApi.kt:107-110` | 无 | `feature/wallet/src/commonTest/kotlin/com.example.my_kmp_project/feature/wallet/WalletApiContractTest.kt:162` |
| 89 | `POST /api/v1/wallet/recharge` | `feature/wallet/src/commonMain/kotlin/com.example.my_kmp_project/feature/wallet/WalletApi.kt:117-123` | 无 | `feature/wallet/src/commonTest/kotlin/com.example.my_kmp_project/feature/wallet/WalletApiContractTest.kt:174`、`:187`、`:198` |
| 90 | `GET /api/v1/membership/me` | `feature/pay/src/commonMain/kotlin/com.example.my_kmp_project/feature/pay/PayApi.kt:41-42` | 无 | `feature/pay/src/commonTest/kotlin/com.example.my_kmp_project/feature/pay/MembershipPayContractTest.kt:27`、`:63`、`:86` |
| 91 | `POST /api/v1/membership/buyout` | `feature/pay/src/commonMain/kotlin/com.example.my_kmp_project/feature/pay/PayApi.kt:50-57`（`payBuyout` `:163`） | 无 | `feature/pay/src/commonTest/kotlin/com.example.my_kmp_project/feature/pay/MembershipPayContractTest.kt:99`、`:116`、`:134`、`:155`、`:169`、`:222`、`:255` |
| 92 | `POST /api/v1/membership/buyout/confirm` | `feature/pay/src/commonMain/kotlin/com.example.my_kmp_project/feature/pay/PayApi.kt:63-69`（`payBuyout` `:176`） | 无 | `feature/pay/src/commonTest/kotlin/com.example.my_kmp_project/feature/pay/MembershipPayContractTest.kt:196`、`:134` |
| 93 | `POST /api/v1/membership/huawei/verify` | `feature/pay/src/commonMain/kotlin/com.example.my_kmp_project/feature/pay/PayApi.kt:79-103` | 无 | `feature/pay/src/commonTest/kotlin/com.example.my_kmp_project/feature/pay/MembershipPayContractTest.kt:278`、`:292` |
| 94 | `POST /api/v1/membership/apple/verify` | `feature/pay/src/commonMain/kotlin/com.example.my_kmp_project/feature/pay/PayApi.kt:112-131` | 无 | `feature/pay/src/commonTest/kotlin/com.example.my_kmp_project/feature/pay/MembershipPayContractTest.kt:298` |
| — | `POST /api/v1/payments/prepay` | **未找到**：`feature/pay/src/commonMain/kotlin/com.example.my_kmp_project/feature/pay/PayApi.kt:32-35` 明示 Dart 侧是死常量、M1 纪律禁止落地；`PayApiPaths` 无该键（`:240-246`） | 无 | 负向断言 `feature/pay/src/commonTest/kotlin/com.example.my_kmp_project/feature/pay/MembershipPayContractTest.kt:146-150` |
| 96 | `POST /api/v1/sse/completions`（SSE） | `feature/ai/src/commonMain/kotlin/com.example.my_kmp_project/feature/ai/AiSseApi.kt:51-67` | 无（`AiStreamScreen` 不 collect） | `feature/ai/src/commonTest/kotlin/com.example.my_kmp_project/feature/ai/AiSseApiTest.kt:57`、`:91`、`:114`、`:134`、`:164`、`:174` |

消费面汇总命令（可复核）：

```
grep -rn --include='*.kt' -E 'feature\.wallet|feature\.pay|feature\.ai' . | grep -v -E '^\./(feature/(wallet|pay|ai)/)'
grep -rn --include='*.kt' -E '\b(MallApi|MembershipCatalog|MembershipTokens)\b' . | grep -v '^./feature/commerce/'
```

前者只命中 `composeApp/src/commonMain/kotlin/com.example.my_kmp_project/app/AppCrossFeatureScreens.kt:8`（`AiStreamScreen`）；后者只命中 `feature/pay/src/commonMain/kotlin/com.example.my_kmp_project/feature/pay/MembershipModels.kt` 的同名符号。

### 7.2 真实状态矩阵

| 模块 | 结论 | 分项 |
|---|---|---|
| `:feature:commerce` · `MallApi` + 4 份模型/请求 | **实现（契约层完整）+ 未接线（UI 零消费）** | 9 个端点全实现、`requireData` 语义与 Dart 对齐；派生字段（`soldLabel`/`badges`/`priceLabel`/`primaryTitle`）逐条有测试；无任何 app 侧调用点（7.1 表） |
| `:feature:commerce` · `MembershipCatalog` | **占位（静态 mock，无消费者）** | 价格/套餐/文案硬编码（`feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MembershipCatalog.kt:64-142`），模块外零引用（3.1.5） |
| `:feature:commerce` · `MembershipScreen` | **薄壳** | 一张 SoT 整图 + 2 个隐形热区；CTA 真调 `PayGateway` 但 `planId` 写死 `"svip_1m"`（`feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MembershipScreen.kt:80`）；`MembershipCatalog` 的数据没渲染 |
| `:feature:wallet` | **实现（HTTP 层完整）+ 未接线** | 7 方法 + 6 DTO 全实现、13 测试通过；app 内零 import，钱包页走 `feature/mine/WalletMockStore`（`feature/mine/src/commonMain/kotlin/com.example.my_kmp_project/feature/mine/WalletMockStore.kt:42` 自述为 mock） |
| `:feature:pay` | **实现（HTTP 契约 + 编排）+ 未接线 + SDK 留白** | 5 端点 + `payBuyout` 完整；`sdkPay` 是注入点无生产实现（`feature/pay/src/commonMain/kotlin/com.example.my_kmp_project/feature/pay/PayApi.kt:156`）；三处 `GAP(R5)`（`:77`/`:86`/`:102`/`:110`/`:148-151`/`:168`）；`/payments/prepay` 按纪律未落地（`:32-35`） |
| `:component` pay 端口 | **stub + 沙箱占位** | 端口/判定逻辑真；`StubPayGateway` 恒 Unavailable（`component/src/commonMain/kotlin/com.example.my_kmp_project/component/pay/PayGateway.kt:33-38`）；三端 actual 恒 null（5.2）；默认唯一成功路径是 `SandboxPayChannelAdapter`（`:64-69`） |
| `:feature:ai` · `AiSseApi` | **实现（含平台不支持分支）** | 组包/帧解码/双错误通道/取消传播齐全并有测试；`SseStreamingUnsupportedException` 覆盖 ohosX64（`core/network/src/ohosX64Main/kotlin/com.example.my_kmp_project/core/network/OhosApiClient.kt:64-68`）；无生产消费者 |
| `:feature:ai` · `AiStreamScreen` | **stub（mock toast）** | 文件 KDoc 自述「Quick-ask / stop stay toast-level mock」（`feature/ai/src/commonMain/kotlin/com.example.my_kmp_project/feature/ai/AiStreamScreen.kt:34-37`）；不发请求、不渲染消息、无停止生成；7 条 router import 未使用（`:25-31`） |

### 7.3 未验证疑点（留在文档，不做推断）

1. `MembershipScreen` 用 `Modifier.statusBarsPadding()`（`feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MembershipScreen.kt:63`）与 `AiStreamScreen`（`:57`）同写法，而 AGENTS.md 顶栏规则要求使用 `core.design.Immersive*TopAppBar` / 禁止在 tab 内容外裸包 `statusBarsPadding()`；这两屏属二级页且无 TopAppBar，是否违规**未判定**（未跑构建、未看 Screenshot Diff Gate 结果）。
2. `:feature:pay` 与 `:feature:commerce` 各有一份 `MembershipTier` / `MembershipPlan`（`feature/pay/src/commonMain/kotlin/com.example.my_kmp_project/feature/pay/MembershipModels.kt:19-32`+`:61-81`；`feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MembershipCatalog.kt:5-8`+`:10-18`），字段集不同；哪一份是「未来接线的目标」**未找到决策记录**。
3. `requireData` 在 commerce / wallet / pay 三处逐字重复（`feature/commerce/src/commonMain/kotlin/com.example.my_kmp_project/feature/commerce/MallApi.kt:254`、`feature/wallet/src/commonMain/kotlin/com.example.my_kmp_project/feature/wallet/WalletApi.kt:144`、`feature/pay/src/commonMain/kotlin/com.example.my_kmp_project/feature/pay/PayApi.kt:263`），三份同为 public 顶层扩展；是否计划下沉到 `:core:network` **未找到说明**。
4. `MallApi.buy` / `createOrder` 的真实服务端幂等表现（键每次变 ⇒ 重试生成第二单）**未做集成验证**，只有单元测试锁形状（`feature/commerce/src/commonTest/kotlin/com.example.my_kmp_project/feature/commerce/MallApiContractTest.kt:131-137`）。
5. 四模块的 Android/iOS/OHOS 三端运行期证据**未验证**：本文只读源码 + 跑 grep，未执行任何 gradle 任务；`openspec/changes/sync-flutter-full-parity/notes/evidence/unit-tests-2026-10-17.md:28`、`:40`、`:48`、`:76` 记有历史测试通过数（22 / 6 / 13），但那是证据文档，非本次运行结果。


@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.example.my_kmp_project.feature.commerce

import com.example.my_kmp_project.core.network.ApiClient
import com.example.my_kmp_project.core.network.ApiEnvelopeParser
import com.example.my_kmp_project.core.network.ApiResponse
import com.example.my_kmp_project.core.network.HttpTextResponse
import com.example.my_kmp_project.core.network.NetworkError
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * 商城契约回归（契约表 #26、#27–#34；Dart
 * `features/mall/lib/mall/api/mall_api.dart` + `mall/model/mall_order.dart`，
 * my_ai_project d7876a1）。夹具是 BFF 真实信封 `{code,message,data,timestamp}`。
 */
class MallApiContractTest {

    // ---- #27 货架 ----

    @Test
    fun shelfPageDecodesListPaginationAndDerivedFields() = runBlocking {
        val fake = FakeApiClient { _, _, _, _ -> SHELF_PAGE_JSON }
        val page = MallApi(fake).fetchShelfPage(storeId = 7, page = 2)

        assertEquals(listOf(FakeApiClient.Call("GET", "/api/v1/mall/stores/7/products", mapOf("page" to "2", "size" to "10"))), fake.calls)
        assertEquals(2, page.items.size)
        assertTrue(page.hasMore(pageSize = 10))

        val first = page.items[0]
        // product_id 优先；cover_aspect < 0.5 视为脏数据回退 1.0（Dart imageAspectRatio）
        assertEquals("p-1001", first.id)
        assertEquals(1.0, first.imageAspectRatio)
        assertNull(first.points)
        assertEquals("39.90元", first.priceLabel)
        assertEquals(emptyList<String>(), first.badges)
        assertTrue(first.soldLabel.startsWith("已兑"))
        // soldLabel / badges 是客户端派生，同 id 恒定
        assertEquals(first.soldLabel, page.items[0].soldLabel)

        // 别名键 `id` + kind=1 + 积分 ⇒ 虚拟发放 / 积分兑换
        val second = page.items[1]
        assertEquals("1002", second.id)
        assertEquals(1.6, second.imageAspectRatio)
        assertEquals(800, second.points)
        assertTrue(second.isVirtual)
        assertEquals(listOf("虚拟发放", "积分兑换"), second.badges)
        assertEquals("800积分", second.priceLabel)
        assertEquals("电子券", second.subtitle)
    }

    @Test
    fun shelfPageDerivedSoldLabelUsesAbsHashCodeSeedRange() {
        // Dart: seed = |id.hashCode|; soldRaw = 20 + seed % 9990 ⇒ 20…10009
        val model = MallProductCardModel(productIdRaw = "p-1001")
        val label = model.soldLabel
        val raw = label.removePrefix("已兑").removeSuffix("万")
        if (label.endsWith("万")) {
            assertEquals("1.0", raw)
        } else {
            val n = raw.toInt()
            assertTrue(n in 20..9999, "actual=$n")
        }
    }

    // ---- #28 详情 ----

    @Test
    fun productDetailDecodesProductAndSkus() = runBlocking {
        val fake = FakeApiClient { _, _, _, _ -> DETAIL_JSON }
        val detail = MallApi(fake).fetchDetail(storeId = 1, productId = "p-1001")

        assertEquals("/api/v1/mall/stores/1/products/p-1001", fake.calls.single().path)
        assertEquals("p-1001", detail.productId)
        assertEquals("车载香薰", detail.title)
        assertEquals(1.2, detail.coverAspect)
        assertFalse(detail.isVirtual)
        assertEquals(2, detail.skus.size)

        val sku = detail.skus[0]
        assertEquals(11, sku.skuId)
        assertEquals("39.90", sku.price)
        assertEquals(5, sku.stockQty)
        assertEquals("颜色 松香 / 规格 100ml", sku.label)
        assertEquals("39.90元", sku.priceLabel)
        assertNull(sku.deliverType)

        // 无规格 ⇒ title；title 也空 ⇒ 默认规格（Dart `label`）
        assertEquals("默认规格", detail.skus[1].label)
        assertEquals(1, detail.skus[1].deliverType)
        assertEquals("500积分", detail.skus[1].priceLabel)
    }

    // ---- #30 / #33 两步下单 ----

    @Test
    fun createOrderSendsFullBodyAndReturnsOrderId() = runBlocking {
        val fake = FakeApiClient { _, _, _, _ -> CREATE_ORDER_JSON }
        val orderId = MallApi(fake).createOrder(storeId = 7, skuId = 11, qty = 2)

        assertEquals(9_001, orderId)
        val call = fake.calls.single()
        assertEquals("POST", call.verb)
        assertEquals("/api/v1/mall/orders", call.path)
        assertTrue(call.body.contains("\"store_id\":7"), call.body)
        assertTrue(call.body.contains("\"lines\":[{\"sku_id\":11,\"qty\":2}]"), call.body)
        // receiver 三件套默认 '' 且总发（Dart mall_api.dart:83）
        assertTrue(call.body.contains("\"receiver_name\":\"\""), call.body)
        assertTrue(call.body.contains("\"receiver_phone\":\"\""), call.body)
        assertTrue(call.body.contains("\"receiver_address\":\"\""), call.body)

        // 幂等键形状 `$skuId-<micros>`
        val match = Regex("\"idempotency_key\":\"(\\d+)-(\\d+)\"").find(call.body)
        assertNotNull(match)
        assertEquals("11", match.groupValues[1])
        val micros = match.groupValues[2].toLong()
        assertTrue(micros > 1_000_000_000_000_000L, "micros=$micros")
    }

    @Test
    fun idempotencyKeyIsRegeneratedPerAttempt_notStable() {
        // 契约表 §5.3：键每次重新生成 ⇒ 只能去重同一请求重发，不能去重客户端重试。
        val a = mallIdempotencyKey(11, micros = 1_760_000_000_000_000L)
        val b = mallIdempotencyKey(11, micros = 1_760_000_000_000_001L)
        assertEquals("11-1760000000000000", a)
        assertFalse(a == b)
    }

    @Test
    fun createOrderThrowsWhenOrderIdMissing() = runBlocking {
        val api = MallApi(FakeApiClient { _, _, _, _ -> """{"code":0,"message":"ok","data":{"order":{"order_id":0}}}""" })
        val error = runCatching { api.createOrder(storeId = 1, skuId = 11, qty = 1) }.exceptionOrNull()
        assertTrue(error is NetworkError.Business)
        assertEquals("下单失败：缺少订单号", error.message)
    }

    @Test
    fun buyIsTheTwoCallWrapperCreateThenPay() = runBlocking {
        val fake = FakeApiClient { verb, path, _, _ ->
            when {
                verb == "POST" && path == "/api/v1/mall/orders" -> CREATE_ORDER_JSON
                path == "/api/v1/mall/orders/9001/pay" -> ORDER_DETAIL_JSON
                else -> error("unexpected $verb $path")
            }
        }
        val orderNo = MallApi(fake).buy(storeId = 7, skuId = 11, qty = 2, paymentChannel = 2)

        assertEquals("MO20261016000901", orderNo)
        assertEquals(
            listOf("POST /api/v1/mall/orders", "POST /api/v1/mall/orders/9001/pay"),
            fake.calls.map { "${it.verb} ${it.path}" },
        )
        // #33 只发 payment_channel
        assertEquals("""{"payment_channel":2}""", fake.calls.last().body)
    }

    // ---- #31 列表 + status 逗号串 ----

    @Test
    fun ordersListStatusIsCommaJoinedQueryOrOmitted() = runBlocking {
        val fake = FakeApiClient { _, _, _, _ -> ORDERS_LIST_JSON }
        val api = MallApi(fake)

        api.fetchOrders(page = 1)
        assertEquals(mapOf("page" to "1", "size" to "10"), fake.calls.last().query)

        api.fetchOrders(page = 1, status = MallOrderStatusQuery.PAID)
        assertEquals("1,2", fake.calls.last().query["status"])

        api.fetchOrders(page = 3, status = MallOrderStatusQuery.CLOSED)
        assertEquals(mapOf("page" to "3", "size" to "10", "status" to "3,4"), fake.calls.last().query)

        api.fetchOrders(page = 1, status = MallOrderStatusQuery.UNPAID)
        assertEquals("0", fake.calls.last().query["status"])

        // 空串按 Dart `status.isNotEmpty` 省略
        api.fetchOrders(page = 1, status = "")
        assertNull(fake.calls.last().query["status"])
    }

    @Test
    fun orderListRowStatusLabelsAndDerivedAggregates() = runBlocking {
        val api = MallApi(FakeApiClient { _, _, _, _ -> ORDERS_LIST_JSON })
        val page = api.fetchOrders(page = 1)
        val row = page.items.single()

        assertEquals(9_001, row.orderId)
        assertEquals("MO20261016000901", row.orderNo)
        assertEquals(7, row.storeId)
        assertEquals("39.90", row.amount)
        assertEquals(0, row.status)
        assertEquals("待支付", row.statusLabel)
        assertTrue(row.isUnpaid)
        assertEquals(3, row.totalQty)
        assertEquals("车载香薰", row.primaryTitle)
        assertEquals("https://cdn/1.jpg", row.primaryCover)
        assertNotNull(row.payDeadline)
        assertTrue(page.hasMore(pageSize = 10))
    }

    @Test
    fun orderListRowEmptyItemsFallsBackToOrderNoTitle() = runBlocking {
        val api = MallApi(
            FakeApiClient { _, _, _, _ -> """{"code":0,"message":"ok","data":{"list":[{"order_id":5,"order_no":"MO5","status":4,"items":[]}]}}""" },
        )
        val row = api.fetchOrders(page = 1).items.single()
        assertEquals("订单 MO5", row.primaryTitle)
        assertEquals("", row.primaryCover)
        assertEquals(0, row.totalQty)
        assertEquals("已关闭", row.statusLabel)
        assertNull(row.payDeadline)
    }

    // ---- #32 详情：pay_deadline_at 在根 data ----

    @Test
    fun orderDetailReadsPayDeadlineFromRootNotOrder() = runBlocking<Unit> {
        val fake = FakeApiClient { _, _, _, _ -> ORDER_DETAIL_JSON }
        val detail = MallApi(fake).fetchOrderDetail(orderId = 9001)

        assertEquals("/api/v1/mall/orders/9001", fake.calls.single().path)
        assertEquals(9_001, detail.orderId)
        assertEquals(1, detail.status)
        assertEquals("已支付", detail.statusLabel)
        assertFalse(detail.isUnpaid)
        assertEquals(2, detail.paymentChannel)
        assertEquals("2026-10-16T10:02:00Z", detail.paidAt)
        assertEquals(800, detail.totalPoints)
        assertTrue(detail.hasReceiver)
        assertEquals("张三", detail.receiverName)
        // 行项目字段（#32 items 与 #31 共用 MallOrderItem；line_amount 保持字符串）
        val item = detail.items.single()
        assertEquals("25.00", item.lineAmount)
        assertTrue(item.isVirtual)
        assertEquals("https://cdn/code.pdf", item.contentUrl)
        // pay_deadline_at 只在根 data 上；order 里的同名干扰键必须被忽略
        assertEquals("2026-10-16T10:15:00Z", detail.payDeadlineAt)
        assertNotNull(detail.payDeadline)
    }

    @Test
    fun payOrderReturnsDetailAndCancelIsPostWithoutBody() = runBlocking {
        val fake = FakeApiClient { _, path, _, _ ->
            if (path.endsWith("/cancel")) """{"code":0,"message":"ok","data":{}}""" else ORDER_DETAIL_JSON
        }
        val api = MallApi(fake)

        val paid = api.payOrder(orderId = 9001, paymentChannel = 1)
        assertEquals(1, paid.status)

        api.cancelOrder(orderId = 9001)
        val cancel = fake.calls.last()
        assertEquals("POST", cancel.verb)
        // 取消是 POST（不是 PATCH/DELETE），且无 body
        assertEquals("/api/v1/mall/orders/9001/cancel", cancel.path)
        assertEquals("", cancel.body)
    }

    // ---- #29 / #34 「原始 Map」端点 ----

    @Test
    fun addToCartBodyIsSnakeCaseSkuQty() = runBlocking {
        val fake = FakeApiClient { _, _, _, _ -> """{"code":0,"message":"ok","data":{"ok":true}}""" }
        MallApi(fake).addToCart(skuId = 11, qty = 3)
        assertEquals("/api/v1/mall/cart", fake.calls.single().path)
        assertEquals("""{"sku_id":11,"qty":3}""", fake.calls.single().body)
    }

    @Test
    fun rawMapEndpointsRequireNonNullDataLikeDartUnderscoreData() = runBlocking {
        // Dart `_data`：`code == 0` 但 `data` 为 null 依旧抛；无 message ⇒ 用中文兜底文案。
        val api = MallApi(FakeApiClient { _, _, _, _ -> """{"code":0,"data":null}""" })

        val addToCart = runCatching { api.addToCart(skuId = 11, qty = 1) }.exceptionOrNull()
        assertTrue(addToCart is NetworkError.Business)
        assertEquals("加入购物车失败", addToCart.message)

        val cancel = runCatching { api.cancelOrder(orderId = 1) }.exceptionOrNull()
        assertTrue(cancel is NetworkError.Business)
        assertEquals("取消订单失败", cancel.message)
    }

    @Test
    fun serverMessageWinsOverChineseFallbackEvenOnCodeZero() = runBlocking {
        // Dart `_data` 的 message 优先级：只要 message 非空就拿它，哪怕 code == 0 只是 data 缺失。
        val api = MallApi(FakeApiClient { _, _, _, _ -> """{"code":0,"message":"ok","data":null}""" })
        assertEquals("ok", runCatching { api.addToCart(skuId = 11, qty = 1) }.exceptionOrNull()?.message)
    }

    @Test
    fun businessErrorThrowsWithServerMessageOrFallback() = runBlocking {
        val api = MallApi(FakeApiClient { _, _, _, _ -> """{"code":403,"message":"无权限","data":null}""" })
        val error = runCatching { api.fetchShelfPage(storeId = 1, page = 1) }.exceptionOrNull()
        assertTrue(error is NetworkError.Business)
        assertEquals(403, error.code)
        assertEquals("无权限", error.message)

        val blank = MallApi(FakeApiClient { _, _, _, _ -> """{"code":403,"message":"","data":null}""" })
        assertEquals(
            "加载商品失败",
            runCatching { blank.fetchShelfPage(storeId = 1, page = 1) }.exceptionOrNull()?.message,
        )
        assertEquals(
            "加载订单详情失败",
            runCatching {
                MallApi(FakeApiClient { _, _, _, _ -> """{"code":403,"data":null}""" })
                    .fetchOrderDetail(orderId = 1)
            }.exceptionOrNull()?.message,
        )
        assertEquals(
            "支付失败",
            runCatching {
                MallApi(FakeApiClient { _, _, _, _ -> """{"code":403,"data":null}""" })
                    .payOrder(orderId = 1, paymentChannel = 1)
            }.exceptionOrNull()?.message,
        )
    }

    // ---- #26 积分商城投影：同一端点、统一 DTO、过滤 + 吞错 ----

    @Test
    fun pointsGiftsDefaultStore1Size20AndFilterPricePoints() = runBlocking {
        val fake = FakeApiClient { _, _, _, _ -> SHELF_PAGE_JSON }
        val gifts = MallApi(fake).fetchPointsGifts()

        assertEquals("/api/v1/mall/stores/1/products", fake.calls.single().path)
        assertEquals(mapOf("page" to "1", "size" to "20"), fake.calls.single().query)
        assertEquals(1, gifts.size)
        assertEquals("1002", gifts.single().id)
        assertEquals(800, gifts.single().pricePoints)
    }

    @Test
    fun pointsGiftsHonorExplicitStoreAndPage() = runBlocking {
        val fake = FakeApiClient { _, _, _, _ -> SHELF_PAGE_JSON }
        MallApi(fake).fetchPointsGifts(storeId = 9, page = 2, size = 5)
        assertEquals("/api/v1/mall/stores/9/products", fake.calls.single().path)
        assertEquals(mapOf("page" to "2", "size" to "5"), fake.calls.single().query)
    }

    @Test
    fun pointsGiftsSwallowEveryFailureLikeDartTryCatch() = runBlocking {
        // 业务失败：Dart `catch (_) => const []`
        val bizFail = MallApi(FakeApiClient { _, _, _, _ -> """{"code":500,"message":"服务器内部错误"}""" })
        assertEquals(emptyList(), bizFail.fetchPointsGifts())

        // 畸形 / 非信封体（解析器回退 -1）
        val malformed = MallApi(FakeApiClient { _, _, _, _ -> "not json at all" })
        assertEquals(emptyList(), malformed.fetchPointsGifts())

        // 传输异常（Ktor client 抛 NetworkError.Transport）
        val transport = MallApi(FakeApiClient { _, _, _, _ -> throw NetworkError.Transport("GET failed") })
        assertEquals(emptyList(), transport.fetchPointsGifts())

        // 纯函数投影：只看 price_points > 0；畸形 data → 空页
        assertEquals(emptyList(), pointsGiftProjection(null))
    }

    // ---- helpers（Dart mall_order.dart:39,98,179-209）----

    @Test
    fun mallOrderStatusLabelTable() {
        assertEquals("待支付", mallOrderStatusLabel(0))
        assertEquals("已支付", mallOrderStatusLabel(1))
        assertEquals("已履约", mallOrderStatusLabel(2))
        assertEquals("已取消", mallOrderStatusLabel(3))
        assertEquals("已关闭", mallOrderStatusLabel(4))
        assertEquals("未知", mallOrderStatusLabel(9))
        assertEquals("未知", mallOrderStatusLabel(-1))
    }

    @Test
    fun parseMallTimeSemantics() {
        assertNull(parseMallTime(""))
        assertNull(parseMallTime("not-a-time"))
        // 带偏移（Go RFC3339）⇒ 解析成功
        assertNotNull(parseMallTime("2026-10-16T10:15:00Z"))
        assertNotNull(parseMallTime("2026-10-16T10:15:00+08:00"))
        // Dart 容忍空格分隔
        assertNotNull(parseMallTime("2026-10-16 10:15:00Z"))
    }

    @Test
    fun formatPayCountdownPadsHoursAndClampsNegative() {
        assertEquals("01:02:03", formatPayCountdown(1.hours + 2.minutes + 3.seconds))
        assertEquals("00:00:45", formatPayCountdown(45.seconds))
        assertEquals("10:00:00", formatPayCountdown(10.hours))
        assertEquals("00:00:00", formatPayCountdown(0.seconds))
        assertEquals("00:00:00", formatPayCountdown(-(10.seconds)))
    }

    // ---- 夹具（BFF 真实信封形状）----

    companion object {
        private val SHELF_PAGE_JSON = """
            {"code":0,"message":"success","data":{
              "list":[
                {"product_id":"p-1001","sku_id":11,"title":"车载香薰","price":"39.90",
                 "cover_url":"https://cdn/1.jpg","cover_aspect":0.4,"kind":0,"price_points":0},
                {"id":1002,"sku_id":12,"title":"洗车券","price":"0.00","cover_url":"https://cdn/2.jpg",
                 "cover_aspect":1.6,"kind":1,"subtitle":"电子券","price_points":800}
              ],
              "pagination":{"page":2,"size":10,"total":21,"total_pages":3}
            },"timestamp":1760000000}
        """.trimIndent()

        private val DETAIL_JSON = """
            {"code":0,"message":"success","data":{
              "product":{"product_id":"p-1001","title":"车载香薰","cover_url":"https://cdn/1.jpg",
                         "cover_aspect":1.2,"kind":0},
              "skus":[
                {"sku_id":11,"title":"松香 100ml","price":"39.90","price_points":0,"stock_qty":5,
                 "specs":{"颜色":"松香","规格":"100ml"}},
                {"sku_id":12,"title":"","price":"0.00","price_points":500,"stock_qty":0,
                 "specs":{},"deliver_type":1}
              ]
            }}
        """.trimIndent()

        private val CREATE_ORDER_JSON = """
            {"code":0,"message":"success","data":{
              "order":{"order_id":9001,"order_no":"MO20261016000901","store_id":7,"status":0,
                       "amount":"79.80","created_at":"2026-10-16T09:00:00Z"}
            }}
        """.trimIndent()

        private val ORDERS_LIST_JSON = """
            {"code":0,"message":"success","data":{
              "list":[
                {"order_id":9001,"order_no":"MO20261016000901","store_id":7,"status":0,
                 "amount":"39.90","created_at":"2026-10-16T09:00:00Z",
                 "pay_deadline_at":"2026-10-16T09:15:00Z",
                 "items":[{"item_id":1,"order_id":9001,"product_title":"车载香薰",
                           "cover_url":"https://cdn/1.jpg","qty":3,"line_amount":"39.90","kind":0}]}
              ],
              "pagination":{"page":1,"size":10,"total":11}
            }}
        """.trimIndent()

        /**
         * #32 关键形状：`pay_deadline_at` 在**根 `data`**，
         * 且 `order` 里故意放一个同名干扰键（Dart 不读它）。
         */
        private val ORDER_DETAIL_JSON = """
            {"code":0,"message":"success","data":{
              "order":{"order_id":9001,"order_no":"MO20261016000901","store_id":7,"status":1,
                       "amount":"39.90","created_at":"2026-10-16T09:00:00Z",
                       "pay_deadline_at":"2026-10-16T09:15:00Z",
                       "receiver_name":"张三","receiver_phone":"13800000000",
                       "receiver_address":"深圳市南山区","payment_channel":2,
                       "paid_at":"2026-10-16T10:02:00Z","total_points":800},
              "items":[{"item_id":1,"order_id":9001,"product_title":"车载香薰",
                        "cover_url":"https://cdn/1.jpg","qty":1,"line_amount":"25.00","kind":1,
                        "content_url":"https://cdn/code.pdf"}],
              "pay_deadline_at":"2026-10-16T10:15:00Z"
            }}
        """.trimIndent()
    }
}

/** 可编程假 client：按 (verb, path, query, body) 出参，走真实 [ApiEnvelopeParser] 解析。 */
public class FakeApiClient(
    private val responder: (verb: String, path: String, query: Map<String, String>, body: String) -> String,
) : ApiClient {
    data class Call(
        val verb: String,
        val path: String,
        val query: Map<String, String> = emptyMap(),
        val body: String = "",
    )

    val calls = mutableListOf<Call>()

    private fun <T> parse(
        verb: String,
        path: String,
        query: Map<String, String>,
        body: String,
        parseData: (JsonElement?) -> T?,
    ): ApiResponse<T> {
        calls += Call(verb, path, query, body)
        return ApiEnvelopeParser.parse(
            raw = responder(verb, path, query, body),
            parseData = parseData,
            httpStatus = 200,
        )
    }

    override suspend fun getRaw(path: String, query: Map<String, String>): String {
        calls += Call("GET", path, query)
        return responder("GET", path, query, "")
    }

    override suspend fun getAbsoluteRaw(url: String): String {
        calls += Call("GET", url)
        return responder("GET", url, emptyMap(), "")
    }

    override suspend fun postRaw(path: String, body: String): String {
        calls += Call("POST", path, body = body)
        return responder("POST", path, emptyMap(), body)
    }

    override suspend fun postForm(path: String, fields: Map<String, String>): HttpTextResponse {
        calls += Call("POST-FORM", path)
        return HttpTextResponse(200, "")
    }

    override suspend fun putRaw(path: String, body: String): String {
        calls += Call("PUT", path, body = body)
        return responder("PUT", path, emptyMap(), body)
    }

    override suspend fun patchRaw(path: String, body: String): String {
        calls += Call("PATCH", path, body = body)
        return responder("PATCH", path, emptyMap(), body)
    }

    override suspend fun deleteRaw(path: String): String {
        calls += Call("DELETE", path)
        return responder("DELETE", path, emptyMap(), "")
    }

    override suspend fun <T> getApi(
        path: String,
        query: Map<String, String>,
        parseData: (JsonElement?) -> T?,
    ): ApiResponse<T> = parse("GET", path, query, "", parseData)

    override suspend fun <T> postApi(
        path: String,
        body: String,
        parseData: (JsonElement?) -> T?,
    ): ApiResponse<T> = parse("POST", path, emptyMap(), body, parseData)

    override suspend fun <T> putApi(
        path: String,
        body: String,
        parseData: (JsonElement?) -> T?,
    ): ApiResponse<T> = parse("PUT", path, emptyMap(), body, parseData)

    override suspend fun <T> patchApi(
        path: String,
        body: String,
        parseData: (JsonElement?) -> T?,
    ): ApiResponse<T> = parse("PATCH", path, emptyMap(), body, parseData)

    override suspend fun <T> deleteApi(
        path: String,
        query: Map<String, String>,
        parseData: (JsonElement?) -> T?,
    ): ApiResponse<T> = parse("DELETE", path, query, "", parseData)
}

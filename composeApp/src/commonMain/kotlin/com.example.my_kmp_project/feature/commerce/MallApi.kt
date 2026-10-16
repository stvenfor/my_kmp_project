package com.example.my_kmp_project.feature.commerce

import com.example.my_kmp_project.core.network.ApiClient
import com.example.my_kmp_project.core.network.ApiResponse
import com.example.my_kmp_project.core.network.ListPage
import com.example.my_kmp_project.core.network.NetworkCodes
import com.example.my_kmp_project.core.network.NetworkError
import com.example.my_kmp_project.core.network.NetworkJson
import com.example.my_kmp_project.core.network.NetworkFacade
import com.example.my_kmp_project.core.network.getApi
import com.example.my_kmp_project.core.network.getListPage
import com.example.my_kmp_project.core.network.pageQuery
import com.example.my_kmp_project.core.network.postApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * 商城 HTTP 契约层（契约表 #26、#27–#34；Dart
 * `features/mall/lib/mall/api/mall_api.dart` +
 * `features/home/lib/home/api/points_api.dart:87`，my_ai_project d7876a1）。
 *
 * 失败语义与 Dart 一致：业务码非 0（或 `data` 缺失）**抛** [NetworkError.Business]，
 * message 取服务端文案、否则用各调用点的中文兜底文案（Dart `MallApi._data`）。
 * UI lane 只消费本类的返回值/异常，不再自己判 `code`。
 */
public class MallApi(
    public val client: ApiClient = NetworkFacade.api(),
) {
    /**
     * #27 货架分页：`GET /api/v1/mall/stores/{storeId}/products?page&size`（page 1-based，size 默认 10）。
     */
    public suspend fun fetchShelfPage(
        storeId: Int,
        page: Int,
        size: Int = PAGE_SIZE,
    ): ListPage<MallProductCardModel> {
        val response = client.getListPage<MallProductCardModel>(
            path = MallApiPaths.products(storeId),
            query = pageQuery(page = page, size = size),
        )
        return response.requireData("加载商品失败")
    }

    /** #28 商品详情：`GET /api/v1/mall/stores/{storeId}/products/{productId}`。 */
    public suspend fun fetchDetail(
        storeId: Int,
        productId: String,
    ): MallProductDetailResponse {
        val response = client.getApi<MallProductDetailResponse>(
            path = MallApiPaths.productDetail(storeId, productId),
        )
        return response.requireData("加载详情失败")
    }

    /** #29 加购：`POST /api/v1/mall/cart {sku_id, qty}`；响应是原始 Map，Dart 丢弃。 */
    public suspend fun addToCart(skuId: Int, qty: Int) {
        val body = NetworkJson.encodeToString(MallAddToCartRequest(skuId = skuId, qty = qty))
        requireRawMapPost(path = MallApiPaths.CART, body = body, fallback = "加入购物车失败")
    }

    /**
     * #30 只创建**未支付**订单并返回 `order_id`（两步下单之一，Dart `createOrder`）。
     *
     * `idempotency_key` 每次调用重新生成 ⇒ 不能去重客户端超时重试（见
     * [mallIdempotencyKey] 的 KDoc，契约表 §5.3）。
     * `order_id <= 0` 视为服务端畸形响应，抛「下单失败：缺少订单号」。
     */
    public suspend fun createOrder(
        storeId: Int,
        skuId: Int,
        qty: Int,
        receiverName: String = "",
        receiverPhone: String = "",
        receiverAddress: String = "",
    ): Int {
        val response = client.postApi<MallCreateOrderRequest, MallCreateOrderResponse>(
            path = MallApiPaths.ORDERS,
            body = MallCreateOrderRequest(
                idempotencyKey = mallIdempotencyKey(skuId),
                storeId = storeId,
                lines = listOf(MallOrderLine(skuId = skuId, qty = qty)),
                receiverName = receiverName,
                receiverPhone = receiverPhone,
                receiverAddress = receiverAddress,
            ),
        )
        val order = response.requireData("下单失败").order
        val orderId = order?.orderId ?: 0
        if (orderId <= 0) {
            // Dart: throw HttpRequestException(message: '下单失败：缺少订单号')（无业务码）。
            throw NetworkError.Business(NetworkCodes.PASSTHROUGH, "下单失败：缺少订单号")
        }
        return orderId
    }

    /**
     * #30 → #33 两步下单的兼容包装（Dart `buy()`：先建未付单，再按渠道支付，返回订单号）。
     */
    public suspend fun buy(
        storeId: Int,
        skuId: Int,
        qty: Int,
        paymentChannel: Int,
        receiverName: String = "",
        receiverPhone: String = "",
        receiverAddress: String = "",
    ): String {
        val orderId = createOrder(
            storeId = storeId,
            skuId = skuId,
            qty = qty,
            receiverName = receiverName,
            receiverPhone = receiverPhone,
            receiverAddress = receiverAddress,
        )
        return payOrder(orderId = orderId, paymentChannel = paymentChannel).orderNo
    }

    /**
     * #31 订单列表：`GET /api/v1/mall/orders?page&size&status`。
     *
     * [status] 是**逗号拼接的查询串**（不是数组、不是重复 key）：
     * `null`/空 全部、`"0"` 待付、`"1,2"` 已付+履约、`"3,4"` 取消+关闭；
     * 空串走 [pageQuery] 的省略规则（Dart `if (status != null && status.isNotEmpty())`）。
     */
    public suspend fun fetchOrders(
        page: Int,
        size: Int = PAGE_SIZE,
        status: String? = null,
    ): ListPage<MallOrderListRow> {
        val response = client.getListPage<MallOrderListRow>(
            path = MallApiPaths.ORDERS,
            query = pageQuery(page = page, size = size, "status" to status),
        )
        return response.requireData("加载订单失败")
    }

    /** #32 订单详情：`GET /api/v1/mall/orders/{orderId}`（`pay_deadline_at` 在根 `data`）。 */
    public suspend fun fetchOrderDetail(orderId: Int): MallOrderDetailResponse {
        val response = client.getApi<MallOrderDetailResponse>(MallApiPaths.order(orderId))
        return response.requireData("加载订单详情失败")
    }

    /** #33 支付：`POST /api/v1/mall/orders/{orderId}/pay {payment_channel}`，响应同 #32。 */
    public suspend fun payOrder(orderId: Int, paymentChannel: Int): MallOrderDetailResponse {
        val response = client.postApi<MallPayOrderRequest, MallOrderDetailResponse>(
            path = MallApiPaths.orderPay(orderId),
            body = MallPayOrderRequest(paymentChannel = paymentChannel),
        )
        return response.requireData("支付失败")
    }

    /**
     * #34 取消：`POST /api/v1/mall/orders/{orderId}/cancel`，**无 body**（取消是 POST，
     * 不是 PATCH/DELETE），响应是原始 Map，Dart 丢弃。
     */
    public suspend fun cancelOrder(orderId: Int) {
        requireRawMapPost(path = MallApiPaths.orderCancel(orderId), body = "", fallback = "取消订单失败")
    }

    /**
     * #26 积分商城投影：同一个 `GET /api/v1/mall/stores/{storeId}/products` 行，
     * Dart 侧由 `PointsApi.fetchPointsGifts` 消费（`points_api.dart:87`）：
     * `storeId` **默认 1**、`size=20`、客户端过滤 `price_points > 0`，
     * 并且**整个调用 try/catch 吞掉一切异常返回空列表**（失败静默、非阻塞）。
     *
     * 行模型统一为 [MallProductCardModel]（原 `PointsGiftItem` 的字段是其子集，
     * `priceLabel` 逻辑也一致），故本方法不再另立 DTO。
     */
    public suspend fun fetchPointsGifts(
        storeId: Int = DEFAULT_POINTS_STORE_ID,
        page: Int = 1,
        size: Int = POINTS_GIFT_PAGE_SIZE,
    ): List<MallProductCardModel> = runCatching {
            val response = client.getListPage<MallProductCardModel>(
                path = MallApiPaths.products(storeId),
                query = pageQuery(page = page, size = size),
            )
            // Dart 只看 data 是否为 null，code != 0 也照样读空列表，不外抛。
            pointsGiftProjection(response.data)
        }.getOrElse { emptyList() }

    /** `POST` 且只吃「原始 Map」的端点：解码 `data` 为 object，仅用于判空（Dart `_data` 要求非空 map）。 */
    private suspend fun requireRawMapPost(path: String, body: String, fallback: String) {
        val response: ApiResponse<JsonObject> = client.postApi(path, body) { el: JsonElement? ->
            el as? JsonObject
        }
        response.requireData(fallback)
    }

    public companion object {
        /** Dart `MallApi.pageSize`。 */
        public const val PAGE_SIZE: Int = 10

        /** Dart `PointsApi.fetchPointsGifts` 的 `size`。 */
        public const val POINTS_GIFT_PAGE_SIZE: Int = 20

        /** Dart `PointsApi.fetchPointsGifts` 硬编码的 `storeId = 1`。 */
        public const val DEFAULT_POINTS_STORE_ID: Int = 1
    }
}

/**
 * #31 `status` 查询串的四种合法取值（契约表 #31；null/空 = 全部）。
 * 状态域：0 待支付 1 已支付 2 已履约 3 已取消 4 已关闭。
 */
public object MallOrderStatusQuery {
    /** 待支付。 */
    public const val UNPAID: String = "0"

    /** 已支付 + 已履约。 */
    public const val PAID: String = "1,2"

    /** 已取消 + 已关闭。 */
    public const val CLOSED: String = "3,4"
}

/**
 * 积分投影的纯函数部分（便于无网络单测）：只保留 `price_points > 0` 的行。
 * 畸形/空 `data` → 空列表（[decodeListPage] 已保证）。
 */
public fun pointsGiftProjection(page: ListPage<MallProductCardModel>?): List<MallProductCardModel> =
    page?.items.orEmpty().filter { it.pricePoints > 0 }

/**
 * 路径常量：与 Dart 一样把路径参数直接插值进字符串，不猜端点。
 * 前缀 `/api/v1` 与 [com.example.my_kmp_project.core.network.NetworkConfig] 的 base 组合方式
 * 保持和 `feature/auth` 一致（写全 `/api/v1/...`）。
 */
public object MallApiPaths {
    private const val BASE: String = "/api/v1/mall"

    public fun products(storeId: Int): String = "$BASE/stores/$storeId/products"

    public fun productDetail(storeId: Int, productId: String): String =
        "$BASE/stores/$storeId/products/$productId"

    public const val CART: String = "$BASE/cart"

    public const val ORDERS: String = "$BASE/orders"

    public fun order(orderId: Int): String = "$ORDERS/$orderId"

    public fun orderPay(orderId: Int): String = "$ORDERS/$orderId/pay"

    public fun orderCancel(orderId: Int): String = "$ORDERS/$orderId/cancel"
}

/**
 * Dart `MallApi._data`：`code == 0` 且 `data != null` 才算成功，
 * 否则抛业务异常（message 优先取服务端文案，空则用中文兜底）。
 */
internal fun <T> ApiResponse<T>.requireData(fallback: String): T {
    val payload = data
    if (!isSuccess || payload == null) {
        throw NetworkError.Business(code, message?.takeIf { it.isNotEmpty() } ?: fallback)
    }
    return payload
}


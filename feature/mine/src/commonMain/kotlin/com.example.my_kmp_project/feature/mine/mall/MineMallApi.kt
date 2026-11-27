package com.example.my_kmp_project.feature.mine.mall

import com.example.my_kmp_project.core.network.ApiClient
import com.example.my_kmp_project.core.network.ApiResponse
import com.example.my_kmp_project.core.network.NetworkError
import com.example.my_kmp_project.core.network.getApi
import com.example.my_kmp_project.core.network.getListPage
import com.example.my_kmp_project.core.network.pageQuery
import com.example.my_kmp_project.core.network.postApi
import com.example.my_kmp_project.core.network.postApiOk
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * 商城页面（Mine 四页：列表 / 详情 / 订单 / 订单详情）的 HTTP 契约。
 *
 * 与 `feature/commerce` 的 `MallApi` 打的是同一组后端接口
 * （`/api/v1/mall/stores/{storeId}/products`、`/api/v1/mall/orders…`），
 * 但 feature 之间禁止互相依赖（脚本 `check-layer-deps.sh` R7），
 * 因此 Mine 侧只保留这四个页面实际需要的字段。
 */

@Serializable
public data class MineMallProduct(
    @SerialName("product_id") val productId: Int = 0,
    @SerialName("sku_id") val skuId: Int = 0,
    val title: String = "",
    val price: String = "0.00",
    @SerialName("cover_url") val coverUrl: String = "",
    @SerialName("cover_aspect") val coverAspect: Double = 1.0,
    val kind: Int = 0,
    val subtitle: String? = null,
    @SerialName("price_points") val pricePoints: Int = 0,
) {
    public val isVirtual: Boolean get() = kind == 1
    public val priceLabel: String get() = "¥$price"
}

@Serializable
public data class MineMallSku(
    @SerialName("sku_id") val skuId: Int = 0,
    val title: String = "",
    val price: String = "0.00",
    @SerialName("price_points") val pricePoints: Int = 0,
    @SerialName("stock_qty") val stockQty: Int = 0,
) {
    public val label: String get() = title.ifBlank { "默认规格" }
    public val priceLabel: String get() = "¥$price"
}

@Serializable
public data class MineMallProductDetail(
    val product: MineMallProduct? = null,
    val skus: List<MineMallSku> = emptyList(),
)

@Serializable
public data class MineMallOrderItem(
    @SerialName("product_title") val productTitle: String = "",
    @SerialName("cover_url") val coverUrl: String = "",
    val qty: Int = 0,
    @SerialName("line_amount") val lineAmount: String = "0.00",
    val kind: Int = 0,
)

@Serializable
public data class MineMallOrderRow(
    @SerialName("order_id") val orderId: Int = 0,
    @SerialName("order_no") val orderNo: String = "",
    val status: Int = 0,
    val amount: String = "0.00",
    @SerialName("created_at") val createdAt: String = "",
    val items: List<MineMallOrderItem> = emptyList(),
) {
    public val statusLabel: String get() = mineMallOrderStatusLabel(status)
    public val primaryTitle: String
        get() = items.firstOrNull()?.productTitle?.takeIf { it.isNotBlank() } ?: "订单 $orderNo"
    public val totalQty: Int get() = items.sumOf { it.qty }
    public val primaryCover: String get() = items.firstOrNull()?.coverUrl.orEmpty()
}

@Serializable
public data class MineMallOrderHead(
    @SerialName("order_id") val orderId: Int = 0,
    @SerialName("order_no") val orderNo: String = "",
    val status: Int = 0,
    val amount: String = "0.00",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("receiver_name") val receiverName: String = "",
    @SerialName("receiver_phone") val receiverPhone: String = "",
    @SerialName("receiver_address") val receiverAddress: String = "",
    @SerialName("payment_channel") val paymentChannel: Int? = null,
    @SerialName("paid_at") val paidAt: String = "",
)

@Serializable
public data class MineMallOrderDetail(
    val order: MineMallOrderHead? = null,
    val items: List<MineMallOrderItem> = emptyList(),
) {
    public val orderId: Int get() = order?.orderId ?: 0
    public val orderNo: String get() = order?.orderNo.orEmpty()
    public val status: Int get() = order?.status ?: 0
    public val statusLabel: String get() = mineMallOrderStatusLabel(status)
    public val amount: String get() = order?.amount ?: "0.00"
    public val createdAt: String get() = order?.createdAt.orEmpty()
    public val receiverName: String get() = order?.receiverName.orEmpty()
    public val receiverPhone: String get() = order?.receiverPhone.orEmpty()
    public val receiverAddress: String get() = order?.receiverAddress.orEmpty()
    public val isUnpaid: Boolean get() = status == 0
    public val isCancellable: Boolean get() = status == 0
}

public fun mineMallOrderStatusLabel(status: Int): String = when (status) {
    0 -> "待支付"
    1 -> "已支付"
    2 -> "已履约"
    3 -> "已取消"
    4 -> "已关闭"
    else -> "未知"
}

/** RFC3339 → `yyyy-MM-dd HH:mm`（commonMain 无时区库，直接截取不做时区换算）。 */
public fun mineMallTimeLabel(raw: String): String {
    if (raw.length < 16) return raw
    return raw.substring(0, 16).replace('T', ' ')
}

public object MineMallPaths {
    private const val BASE: String = "/api/v1/mall"

    public fun products(storeId: Int): String = "$BASE/stores/$storeId/products"
    public fun productDetail(storeId: Int, productId: Int): String = "${products(storeId)}/$productId"
    public const val ORDERS: String = "$BASE/orders"
    public fun order(orderId: Int): String = "$ORDERS/$orderId"
    public fun orderPay(orderId: Int): String = "${order(orderId)}/pay"
    public fun orderCancel(orderId: Int): String = "${order(orderId)}/cancel"
}

@Serializable
private data class MineMallCreateOrderRequest(
    @SerialName("idempotency_key") val idempotencyKey: String,
    @SerialName("store_id") val storeId: Int,
    val lines: List<MineMallOrderLine>,
    @SerialName("receiver_name") val receiverName: String = "",
    @SerialName("receiver_phone") val receiverPhone: String = "",
    @SerialName("receiver_address") val receiverAddress: String = "",
)

@Serializable
private data class MineMallOrderLine(val sku_id: Int, val qty: Int)

@Serializable
private data class MineMallOrderCreated(val order: MineMallOrderHead? = null)

@Serializable
private data class MineMallPayRequest(@SerialName("payment_channel") val paymentChannel: Int)

/** Mall HTTP 契约；`api` 由 App 层注入（feature 内不再直取单例）。 */
public class MineMallApi(
    private val api: ApiClient,
) {

    public suspend fun fetchProducts(storeId: Int, page: Int = 1, size: Int = 50): List<MineMallProduct> {
        val response = api.getListPage<MineMallProduct>(
            path = MineMallPaths.products(storeId),
            query = pageQuery(page = page, size = size),
        )
        return response.requireData("加载商品失败").items
    }

    public suspend fun fetchProductDetail(storeId: Int, productId: Int): MineMallProductDetail {
        val response = api.getApi<MineMallProductDetail>(
            path = MineMallPaths.productDetail(storeId, productId),
        )
        return response.requireData("加载商品详情失败")
    }

    public suspend fun fetchOrders(page: Int = 1, size: Int = 50, status: String? = null): List<MineMallOrderRow> {
        val response = api.getListPage<MineMallOrderRow>(
            path = MineMallPaths.ORDERS,
            query = pageQuery(page = page, size = size, "status" to status),
        )
        return response.requireData("加载订单失败").items
    }

    public suspend fun fetchOrderDetail(orderId: Int): MineMallOrderDetail {
        val response = api.getApi<MineMallOrderDetail>(path = MineMallPaths.order(orderId))
        return response.requireData("加载订单详情失败")
    }

    /** 创建未支付订单，返回订单号。`idempotencyKey` 由调用方生成。 */
    public suspend fun createOrder(
        storeId: Int,
        skuId: Int,
        qty: Int,
        idempotencyKey: String,
        receiverName: String = "",
        receiverPhone: String = "",
        receiverAddress: String = "",
    ): Int {
        val response = api.postApi<MineMallCreateOrderRequest, MineMallOrderCreated>(
            path = MineMallPaths.ORDERS,
            body = MineMallCreateOrderRequest(
                idempotencyKey = idempotencyKey,
                storeId = storeId,
                lines = listOf(MineMallOrderLine(sku_id = skuId, qty = qty)),
                receiverName = receiverName,
                receiverPhone = receiverPhone,
                receiverAddress = receiverAddress,
            ),
        )
        val orderId = response.requireData("下单失败").order?.orderId ?: 0
        if (orderId <= 0) throw NetworkError.Business(response.code, "下单失败：缺少订单号")
        return orderId
    }

    public suspend fun payOrder(orderId: Int, paymentChannel: Int) {
        val response = api.postApiOk(
            path = MineMallPaths.orderPay(orderId),
            body = MineMallPayRequest(paymentChannel = paymentChannel),
        )
        if (!response.isSuccess) {
            throw NetworkError.Business(
                response.code,
                response.message?.takeIf { it.isNotEmpty() } ?: "支付失败",
            )
        }
    }

    public suspend fun cancelOrder(orderId: Int) {
        val response = api.postApiOk(
            path = MineMallPaths.orderCancel(orderId),
            body = JsonObject(emptyMap()),
        )
        if (!response.isSuccess) {
            throw NetworkError.Business(
                response.code,
                response.message?.takeIf { it.isNotEmpty() } ?: "取消订单失败",
            )
        }
    }
}

private fun <T> ApiResponse<T>.requireData(fallback: String): T {
    val payload = data
    if (!isSuccess || payload == null) {
        throw NetworkError.Business(code, message?.takeIf { it.isNotEmpty() } ?: fallback)
    }
    return payload
}

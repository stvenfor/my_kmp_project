package com.example.my_kmp_project.feature.commerce

import com.example.my_kmp_project.core.network.ApiClient
import com.example.my_kmp_project.core.network.ApiResponse
import com.example.my_kmp_project.core.network.postApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * 商城后台写接口契约（3 条 `missing(待接线)`，后端 `mall/router.go:42-44`）。
 *
 * 真相源改为 **Go 后端 my_go_study**（不再是 Flutter 的死常量）：
 * - `POST /api/v1/mall/products` —— `mall/handler.go:34` CreateProduct，body 五键；
 * - `POST /api/v1/mall/products/:product_id/skus` —— `mall/handler.go:60` CreateSKU，body 九键；
 * - `POST /api/v1/mall/skus/:sku_id/codes` —— `mall/handler.go:106` AddVirtualCodes，body `{codes}`。
 * - 响应 `data` 形态见 `model/entity/mall.go:180`（WysMallProduct）、`:229`（WysMallSKU）
 *   与 `handler.go:128`（`gin.H{"ok":true}`）。
 *
 * 仿 [PaymentPrepayApi] 样板：**只登记后端真实路由契约**（路径常量 + 请求/响应模型 +
 * 可调用方法 + 契约测试），不自造 UI 调用点，也不改后端仓库。
 * 逐条对照 `notes/backend-contract-matrix.md` B 区 #2/#3/#4。
 */
public class MallAdminApi(
    private val client: ApiClient,
) {
    /** `POST /api/v1/mall/products`（`mall/router.go:42` / `mall/handler.go:34`）。 */
    public suspend fun createProduct(request: MallCreateProductRequest): ApiResponse<MallProductCreatedResponse> =
        client.postApi<MallCreateProductRequest, MallProductCreatedResponse>(
            path = MallAdminPaths.PRODUCTS,
            body = request,
        )

    /** `POST /api/v1/mall/products/:product_id/skus`（`mall/router.go:43` / `mall/handler.go:60`）。 */
    public suspend fun createSku(productId: Long, request: MallCreateSkuRequest): ApiResponse<MallSkuCreatedResponse> =
        client.postApi<MallCreateSkuRequest, MallSkuCreatedResponse>(
            path = MallAdminPaths.productSkus(productId),
            body = request,
        )

    /** `POST /api/v1/mall/skus/:sku_id/codes`（`mall/router.go:44` / `mall/handler.go:106`）。 */
    public suspend fun addVirtualCodes(skuId: Long, request: MallAddVirtualCodesRequest): ApiResponse<MallVirtualCodesAck> =
        client.postApi<MallAddVirtualCodesRequest, MallVirtualCodesAck>(
            path = MallAdminPaths.skuCodes(skuId),
            body = request,
        )
}

/** 商城后台写接口路径常量（对齐 `mall/router.go:42-44`）。 */
public object MallAdminPaths {
    private const val BASE: String = "/api/v1/mall"

    /** `POST /api/v1/mall/products`。 */
    public const val PRODUCTS: String = "$BASE/products"

    /** `POST /api/v1/mall/products/:product_id/skus`。 */
    public fun productSkus(productId: Long): String = "$PRODUCTS/$productId/skus"

    /** `POST /api/v1/mall/skus/:sku_id/codes`。 */
    public fun skuCodes(skuId: Long): String = "$BASE/skus/$skuId/codes"
}

/** `POST /mall/products` body（`mall/handler.go:40-46`，`cover_url` 为可空指针）。 */
@Serializable
public data class MallCreateProductRequest(
    @SerialName("store_id") val storeId: Int,
    val kind: Int,
    val title: String,
    @SerialName("cover_url") val coverUrl: String? = null,
    val status: Int,
)

/** `POST /mall/products/:product_id/skus` body（`mall/handler.go:71-81`，`specs` 为任意 JSON）。 */
@Serializable
public data class MallCreateSkuRequest(
    @SerialName("sku_code") val skuCode: String,
    val title: String,
    val specs: JsonObject = JsonObject(emptyMap()),
    val price: String,
    @SerialName("price_points") val pricePoints: Long,
    @SerialName("stock_qty") val stockQty: Int,
    val status: Int,
    @SerialName("deliver_type") val deliverType: Int? = null,
    @SerialName("content_url") val contentUrl: String? = null,
)

/** `POST /mall/skus/:sku_id/codes` body（`mall/handler.go:117-119`）。 */
@Serializable
public data class MallAddVirtualCodesRequest(
    val codes: List<String>,
)

/** `POST /mall/products` 的 `data`（`model/entity/mall.go:180` WysMallProduct 的子集）。 */
@Serializable
public data class MallProductCreatedResponse(
    @SerialName("product_id") val productId: Long = 0,
    @SerialName("store_id") val storeId: Int = 0,
    val kind: Int = 0,
    val title: String = "",
    val status: Int = 0,
)

/** `POST /mall/products/:product_id/skus` 的 `data`（`model/entity/mall.go:229` WysMallSKU 的子集）。 */
@Serializable
public data class MallSkuCreatedResponse(
    @SerialName("sku_id") val skuId: Long = 0,
    @SerialName("product_id") val productId: Long = 0,
    @SerialName("sku_code") val skuCode: String = "",
    val title: String = "",
    val price: String = "",
    val status: Int = 0,
)

/** `POST /mall/skus/:sku_id/codes` 的 `data`（`mall/handler.go:128` `gin.H{"ok":true}`）。 */
@Serializable
public data class MallVirtualCodesAck(
    val ok: Boolean = false,
)

package com.example.my_kmp_project.feature.commerce

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.math.abs

/**
 * 商城商品契约（契约表 #27、#28；Dart
 * `features/mall/lib/mall/model/mall_product_model.dart` +
 * `mall_product_detail.dart`，my_ai_project d7876a1）。
 *
 * **统一商品 DTO（audit 结论）**：Dart 里 `GET /api/v1/mall/stores/:storeId/products`
 * 被两个 feature 用两种模型解码——商城货架 [MallProductCardModel]
 * （`mall_product_model.dart`）与积分商城 `PointsGiftItem`
 * （`features/home/lib/home/model/points_models.dart:137`）。两者字段是子集关系，
 * KMP 侧只保留 [MallProductCardModel] 一份，积分投影见 [MallApi.fetchPointsGifts]。
 *
 * BFF 里 `price` / `price_points` 语义：**金额是字符串**（`price:String`，不做 Double 化），
 * 积分是整数；`soldLabel` / `badges` 是 Dart 客户端派生展示字段，**不是服务端字段**，
 * 因此这里以计算属性暴露、不参与序列化。
 */
@Serializable
public data class MallProductCardModel(
    /** Dart `json['product_id'] ?? json['id']` 的前半；大小写/别名容错走 [resolvedId]。 */
    @SerialName("product_id") val productIdRaw: String? = null,
    /** 别名键：`product_id` 缺失时 Dart 回退 `id`。 */
    @SerialName("id") val idRaw: String? = null,
    @SerialName("sku_id") val skuId: Int = 0,
    val title: String = "",
    /** 售价，BFF 下发字符串（`'0.00'` 兜底同 Dart）。 */
    val price: String = "0.00",
    @SerialName("cover_url") val coverUrl: String = "",
    @SerialName("cover_aspect") val coverAspect: Double = 1.0,
    /** 1 ⇒ 虚拟商品（虚拟发放）。 */
    val kind: Int = 0,
    val subtitle: String? = null,
    @SerialName("price_points") val pricePoints: Int = 0,
) {
    /** Dart `productId?.toString() ?? ''`：`product_id` 优先，缺失才用 `id`。 */
    public val id: String
        get() = productIdRaw?.takeIf { it.isNotBlank() } ?: idRaw.orEmpty()

    /** Dart `imageAspectRatio`：宽高比小于 0.5 视为脏数据，回退 1.0。 */
    public val imageAspectRatio: Double
        get() = if (coverAspect < 0.5) 1.0 else coverAspect

    /** Dart `points`：`price_points > 0` 才有值，否则 null（区分「纯现金」与「积分+现金」）。 */
    public val points: Int?
        get() = pricePoints.takeIf { it > 0 }

    public val isVirtual: Boolean
        get() = kind == 1

    /**
     * Dart `MallProductCardModel.fromJson` 的伪销量：
     * `seed = id.hashCode().abs(); soldRaw = 20 + seed % 9990`，≥10000 走「x.x万」。
     *
     * 注：Dart 与 JVM 的 `String.hashCode` 算法不同，数值本身不可能与 Flutter 逐位相同；
     * 这里保留的是「同一 id 恒定、区间 20…10009、万位格式化」这套规则。
     */
    public val soldLabel: String
        get() {
            val seed = abs(id.hashCode())
            val soldRaw = 20 + (seed % 9990)
            return if (soldRaw >= 10000) {
                "已兑${fixedOneDecimal(soldRaw / 10000.0)}万"
            } else {
                "已兑$soldRaw"
            }
        }

    /** Dart 派生徽标：虚拟发放 / 积分+现金 / 积分兑换。 */
    public val badges: List<String>
        get() = buildList {
            if (kind == 1) add("虚拟发放")
            val cny = price.toDoubleOrNull() ?: 0.0
            if (pricePoints > 0 && cny > 0) add("积分+现金") else if (pricePoints > 0) add("积分兑换")
        }

    /** Dart `priceLabel`：积分 / 积分+元 / 纯元。 */
    public val priceLabel: String
        get() {
            val p = points
            val cny = price.toDoubleOrNull() ?: 0.0
            return when {
                p != null && p > 0 && cny > 0 -> "${p}积分+${price}元"
                p != null && p > 0 -> "${p}积分"
                else -> "${price}元"
            }
        }
}

/** Dart `toStringAsFixed(1)` 的无 Locale 对等实现（commonMain 无 `String.format`）。 */
internal fun fixedOneDecimal(value: Double): String {
    val scaled = kotlin.math.round(value * 10.0).toLong()
    val whole = scaled / 10
    val frac = abs(scaled % 10)
    return "$whole.$frac"
}

/**
 * 商品详情响应（契约表 #28）：`data = { product: {...}, skus: [...] }`。
 * Dart `MallProductDetail.fromJson` 把 `product` 摊平，这里保留嵌套结构 + 摊平计算属性。
 */
@Serializable
public data class MallProductPayload(
    @SerialName("product_id") val productId: String = "",
    val title: String = "",
    @SerialName("cover_url") val coverUrl: String = "",
    @SerialName("cover_aspect") val coverAspect: Double = 1.0,
    val kind: Int = 0,
)

@Serializable
public data class MallSkuOffer(
    @SerialName("sku_id") val skuId: Int = 0,
    val title: String = "",
    /** 售价字符串（Dart `json['price']?.toString() ?? '0.00'`）。 */
    val price: String = "0.00",
    @SerialName("price_points") val pricePoints: Int = 0,
    @SerialName("stock_qty") val stockQty: Int = 0,
    /** Dart `specs is Map<String, dynamic> ? specs : const {}`——规格字典，键值顺序由 BFF 决定。 */
    val specs: JsonObject? = null,
    @SerialName("deliver_type") val deliverType: Int? = null,
) {
    /** Dart `priceLabel`：积分+元 / 积分 / 元。 */
    public val priceLabel: String
        get() {
            val cny = price.toDoubleOrNull() ?: 0.0
            return when {
                pricePoints > 0 && cny > 0 -> "${pricePoints}积分+${price}元"
                pricePoints > 0 -> "${pricePoints}积分"
                else -> "${price}元"
            }
        }

    /**
     * Dart `label`：无规格时退化为标题（标题也为空则「默认规格」），
     * 有规格时按 `key value` 用 ` / ` 连接。
     */
    public val label: String
        get() {
            val entries = specEntries()
            if (entries.isEmpty()) return title.ifEmpty { "默认规格" }
            return entries.joinToString(" / ") { (k, v) -> "$k $v" }
        }

    /** `specs` 的按键序展开（值按 Dart `'$value'` 规则转字符串）。 */
    public fun specEntries(): List<Pair<String, String>> =
        specs?.entries?.map { (key, value) -> key to renderJsonScalar(value) }.orEmpty()
}

@Serializable
public data class MallProductDetailResponse(
    val product: MallProductPayload? = null,
    val skus: List<MallSkuOffer> = emptyList(),
) {
    /** 摊平后的详情（对齐 Dart `MallProductDetail` 的字段集合）。 */
    public val productId: String get() = product?.productId.orEmpty()
    public val title: String get() = product?.title.orEmpty()
    public val coverUrl: String get() = product?.coverUrl.orEmpty()
    public val coverAspect: Double get() = product?.coverAspect ?: 1.0
    public val kind: Int get() = product?.kind ?: 0
    public val isVirtual: Boolean get() = kind == 1
}

/**
 * Dart `json['x']?.toString()` 的标量渲染：字符串/数字/布尔取 `content`，
 * `null` 取 `null`（与 Dart 的 `'$value'` 插值一致）。
 */
internal fun renderJsonScalar(value: JsonElement?): String = when (value) {
    null -> "null"
    is JsonPrimitive -> value.content
    else -> value.toString()
}

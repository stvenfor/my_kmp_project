package com.example.my_kmp_project.feature.mine.usedcar

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 二手车业务单模型 —— Flutter
 * `features/home/lib/home/model/used_car_order_models.dart`（my_ai_project d7876a1）的
 * KMP 对等；契约表 §2.7 #35 / #37 / #38。
 *
 * 空值语义：Dart 每个字段都写成 `json['k'] as String? ?? ''` /
 * `(json['k'] as num?)?.toInt() ?? 0`，即 **JSON null 与缺省等价**。
 * `NetworkJson` 未开 `coerceInputValues`，非空字段遇到显式 JSON null 会让整行解码失败，
 * 因此这里把线上字段建模为可空，并把 Dart 的默认值放在 `resolved*` 访问器上
 * （语义与 Dart 逐字段一致，调用侧拿到的仍是非空值）。
 */

/**
 * Dart `UsedCarOrderStats`（used_car_order_models.dart:29-50）。
 * 外层 `stats` 缺省为 `{}`（:18），所以四个计数全部默认 0。
 */
@Serializable
public data class UsedCarOrderStats(
    val submitted: Int? = null,
    @SerialName("pending_review") val pendingReview: Int? = null,
    val approved: Int? = null,
    val rejected: Int? = null,
) {
    /** Dart `(json['submitted'] as num?)?.toInt() ?? 0`。 */
    public val resolvedSubmitted: Int get() = submitted ?: 0

    /** Dart `(json['pending_review'] as num?)?.toInt() ?? 0`。 */
    public val resolvedPendingReview: Int get() = pendingReview ?: 0

    /** Dart `(json['approved'] as num?)?.toInt() ?? 0`。 */
    public val resolvedApproved: Int get() = approved ?: 0

    /** Dart `(json['rejected'] as num?)?.toInt() ?? 0`。 */
    public val resolvedRejected: Int get() = rejected ?: 0
}

/**
 * Dart `UsedCarOrderSummary`（used_car_order_models.dart:2-27）—— 契约表 #35。
 * `stats` 缺省 `{}`（:18）→ [UsedCarOrderStats] 的四个计数全 0。
 */
@Serializable
public data class UsedCarOrderSummary(
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("position_label") val positionLabel: String? = null,
    @SerialName("store_name") val storeName: String? = null,
    val stats: UsedCarOrderStats? = null,
) {
    public val resolvedDisplayName: String get() = displayName.orEmpty()
    public val resolvedAvatarUrl: String get() = avatarUrl.orEmpty()
    public val resolvedPositionLabel: String get() = positionLabel.orEmpty()
    public val resolvedStoreName: String get() = storeName.orEmpty()

    /** Dart `json['stats'] as Map? ?? const {}` 后仍构造全 0 计数。 */
    public val resolvedStats: UsedCarOrderStats get() = stats ?: UsedCarOrderStats()
}

/**
 * Dart `UsedCarStatusTab`（used_car_order_models.dart:52-68）—— 列表 `status` 查询值。
 * 注意：**没有中文字面量**（中文字面量只出现在 `/new-car-follow-files` 的 `intent_band`）。
 */
public enum class UsedCarStatusTab(public val apiStatus: String) {
    All("all"),
    PendingReview("pending_review"),
    Approved("approved"),
    Rejected("rejected"),
}

/**
 * Dart `UsedCarKindFilter`（used_car_order_models.dart:70-86）。
 * `all` → [apiKind] 为 null ⇒ 查询串**省略 `kind` 键**（Dart `if (kind != null) query['kind'] = kind`，
 * used_car_order_api.dart:35）。
 */
public enum class UsedCarKindFilter {
    All,
    TradeIn,
    Consign,
    Purchase;

    /** Dart `UsedCarKindFilterX.apiKind`：all → null。 */
    public val apiKind: String?
        get() = when (this) {
            All -> null
            TradeIn -> "trade_in"
            Consign -> "consign"
            Purchase -> "purchase"
        }
}

/**
 * Dart `UsedCarOrderItem`（used_car_order_models.dart:88-157）—— 契约表 #37。
 *
 * **`order_id` 在 KMP 侧必须是 String**：Dart 用 `'${json['order_id'] ?? ""}'` 把服务端的
 * 数字单号字符串化（:137），下游（详情路径、列表去重）都按字符串处理，不要「修正」成 Int。
 *
 * `submitted_at`：Dart `DateTime.tryParse(...) ?? DateTime.fromMillisecondsSinceEpoch(0)`
 * （:150）；本仓库 `commonMain` 无 kotlinx-datetime 依赖，故保留原始 ISO-8601 串，
 * 由展示层解析（缺省/不可解析 == Dart 的 epoch 0 语义）。
 */
@Serializable
public data class UsedCarOrderItem(
    @SerialName("order_id") val orderId: String? = null,
    val kind: String? = null,
    @SerialName("kind_label") val kindLabel: String? = null,
    @SerialName("amount_label") val amountLabel: String? = null,
    @SerialName("customer_name") val customerName: String? = null,
    val phone: String? = null,
    @SerialName("vehicle_model") val vehicleModel: String? = null,
    @SerialName("plate_no") val plateNo: String? = null,
    val vin: String? = null,
    @SerialName("mileage_km") val mileageKm: Int? = null,
    @SerialName("model_year") val modelYear: Int? = null,
    val amount: Double? = null,
    val status: String? = null,
    @SerialName("submitted_at") val submittedAt: String? = null,
    @SerialName("reject_reason") val rejectReason: String? = null,
    @SerialName("rating_stars") val ratingStars: Int? = null,
    @SerialName("image_url") val imageUrl: String? = null,
) {
    /** Dart `'${json['order_id'] ?? ""}'`。 */
    public val resolvedOrderId: String get() = orderId.orEmpty()
    public val resolvedKind: String get() = kind.orEmpty()
    public val resolvedKindLabel: String get() = kindLabel.orEmpty()

    /** Dart `json['amount_label'] as String? ?? '业务金额'`（:140，默认值非空串）。 */
    public val resolvedAmountLabel: String get() = amountLabel ?: "业务金额"
    public val resolvedCustomerName: String get() = customerName.orEmpty()
    public val resolvedPhone: String get() = phone.orEmpty()
    public val resolvedVehicleModel: String get() = vehicleModel.orEmpty()
    public val resolvedPlateNo: String get() = plateNo.orEmpty()
    public val resolvedVin: String get() = vin.orEmpty()
    public val resolvedMileageKm: Int get() = mileageKm ?: 0
    public val resolvedModelYear: Int get() = modelYear ?: 0

    /** Dart `(json['amount'] as num?)?.toDouble() ?? 0`：金额是 double，不是定价串。 */
    public val resolvedAmount: Double get() = amount ?: 0.0
    public val resolvedStatus: String get() = status.orEmpty()
    public val resolvedSubmittedAt: String get() = submittedAt.orEmpty()

    /**
     * Dart `statusLabel`（:127-133）：状态域含 `approved_pending_rating` / `rated`，
     * 未知状态原样返回。
     */
    public val statusLabel: String
        get() = when (resolvedStatus) {
            "pending_review" -> "待审核"
            "approved_pending_rating" -> "已通过待评价"
            "rated" -> "已通过已评价"
            "rejected" -> "未通过"
            else -> resolvedStatus
        }
}

/**
 * Dart `UsedCarCustomer`（used_car_order_models.dart:159-177）—— 契约表 #38。
 * 这里只有 `display_name`，**没有** `name` 兜底键（`/deal-invoices/customers` 才有）。
 */
@Serializable
public data class UsedCarCustomer(
    @SerialName("customer_id") val customerId: Int? = null,
    @SerialName("display_name") val displayName: String? = null,
    val phone: String? = null,
) {
    /** Dart `(json['customer_id'] as num?)?.toInt() ?? 0`。 */
    public val resolvedCustomerId: Int get() = customerId ?: 0
    public val resolvedDisplayName: String get() = displayName.orEmpty()
    public val resolvedPhone: String get() = phone.orEmpty()
}

/**
 * Dart `fetchList` 的 record 返回值 `({List list, bool hasMore})`
 * （used_car_order_api.dart:22,48）。
 * `hasMore` 由共享 [com.example.my_kmp_project.core.network.ListPage] 给出
 * （`pagination` 缺失时退化为「本页拿满」），feature 侧不再自己算 `page*size<total`。
 */
public data class UsedCarOrderListResult(
    public val items: List<UsedCarOrderItem>,
    public val hasMore: Boolean,
)

/**
 * `POST /api/v1/used-car-orders` 请求体 —— Dart used_car_order_api.dart:102-112（契约表 #39）。
 *
 * **`image_url` 恒发**（Dart 直接写死 `'image_url': imageUrl`，null 也发），
 * 所以这个 body 必须用
 * [com.example.my_kmp_project.core.network.NetworkJsonExplicitNulls] 编码，
 * 不能用默认的 `NetworkJson`（它关掉了 `explicitNulls`）。
 * 建单后服务端状态即 `pending_review`。
 */
@Serializable
public data class UsedCarOrderCreateRequest(
    val kind: String,
    @SerialName("customer_id") val customerId: Int,
    @SerialName("vehicle_model") val vehicleModel: String,
    @SerialName("plate_no") val plateNo: String,
    val vin: String,
    @SerialName("mileage_km") val mileageKm: Int,
    @SerialName("model_year") val modelYear: Int,
    val amount: Double,
    @SerialName("image_url") val imageUrl: String? = null,
)

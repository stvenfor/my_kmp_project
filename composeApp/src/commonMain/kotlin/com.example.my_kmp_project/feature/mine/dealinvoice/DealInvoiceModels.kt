package com.example.my_kmp_project.feature.mine.dealinvoice

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 成交发票模型 —— Flutter
 * `features/settings/lib/deal_invoice/model/deal_invoice_models.dart`（d7876a1）的 KMP 对等；
 * 契约表 §2.10 #51-#54。
 *
 * 空值语义同 [com.example.my_kmp_project.feature.mine.usedcar.UsedCarOrderItem]：
 * Dart 用 `'${json['k'] ?? ''}'` 消化 null，而 `NetworkJson` 未开 `coerceInputValues`，
 * 所以线上字段建模为可空、默认值放在 `resolved*` 访问器上。
 * Dart 侧的 `auditColor`（:17-22）属于 UI，不在此契约层建模。
 */

/**
 * Dart `DealInvoiceStatus`（deal_invoice_models.dart:4-32）。
 * 状态域含 `approved_pending_rating` 与 `rated`；**未知/缺省都落到 [PendingReview]**
 * （`fromApi` 的 `_` 分支，:24-31）。
 */
public enum class DealInvoiceStatus {
    PendingReview,
    ApprovedPendingRating,
    Rated,
    Rejected;

    /** Dart `auditLabel`（:10-15）：`approved_pending_rating` 与 `rated` 都显示「已通过」。 */
    public val auditLabel: String
        get() = when (this) {
            PendingReview -> "待审核"
            ApprovedPendingRating -> "已通过"
            Rated -> "已通过"
            Rejected -> "未通过"
        }

    public companion object {
        /** Dart `DealInvoiceStatus.fromApi`（:24-31）。 */
        public fun fromApi(raw: String?): DealInvoiceStatus = when (raw) {
            "approved_pending_rating" -> ApprovedPendingRating
            "rated" -> Rated
            "rejected" -> Rejected
            else -> PendingReview
        }
    }
}

/**
 * Dart `DealInvoiceTab`（deal_invoice_models.dart:88-97）—— 列表 `status` 查询值；
 * `all` 也照发（契约表 §2.10 #52）。
 */
public enum class DealInvoiceTab(public val apiStatus: String) {
    All("all"),
    PendingReview("pending_review"),
    Approved("approved"),
    Rejected("rejected"),
}

/**
 * Dart `DealInvoiceStats`（deal_invoice_models.dart:100-126）—— 契约表 #51 的 `stats`。
 * `_intOf`（:122）接受 int 与数字串，缺省 0；外层 `stats` 非 Map 时整块取全 0（:151-158）。
 */
@Serializable
public data class DealInvoiceStats(
    val uploaded: Int? = null,
    @SerialName("pending_review") val pendingReview: Int? = null,
    val approved: Int? = null,
    val rejected: Int? = null,
) {
    public val resolvedUploaded: Int get() = uploaded ?: 0
    public val resolvedPendingReview: Int get() = pendingReview ?: 0
    public val resolvedApproved: Int get() = approved ?: 0
    public val resolvedRejected: Int get() = rejected ?: 0
}

/**
 * Dart `DealInvoiceSummary`（deal_invoice_models.dart:129-177）—— 契约表 #51。
 */
@Serializable
public data class DealInvoiceSummary(
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("position_label") val positionLabel: String? = null,
    @SerialName("store_name") val storeName: String? = null,
    val stats: DealInvoiceStats? = null,
) {
    public val resolvedDisplayName: String get() = displayName.orEmpty()
    public val resolvedAvatarUrl: String get() = avatarUrl.orEmpty()
    public val resolvedPositionLabel: String get() = positionLabel.orEmpty()
    public val resolvedStoreName: String get() = storeName.orEmpty()

    /** Dart `statsRaw is Map ? fromJson : 全 0`（:151-158）。 */
    public val resolvedStats: DealInvoiceStats get() = stats ?: DealInvoiceStats()
}

/**
 * Dart `DealInvoiceItem`（deal_invoice_models.dart:180-234）—— 契约表 #52/#53/#55/#56。
 *
 * **id 是 String**：Dart `'${json['invoice_id'] ?? json['id'] ?? ''}'`（:210）把服务端
 * 数字发票号字符串化；`??` 只在 null 时向下兜底，所以 `invoice_id:""` 的结果是空串。
 * `rating_stars` 可空（`approved_pending_rating` 之前没有评分）。
 */
@Serializable
public data class DealInvoiceItem(
    @SerialName("invoice_id") val invoiceId: String? = null,
    val id: String? = null,
    val phone: String? = null,
    @SerialName("customer_name") val customerName: String? = null,
    val status: String? = null,
    @SerialName("submitted_at") val submittedAt: String? = null,
    @SerialName("reject_reason") val rejectReason: String? = null,
    @SerialName("rating_stars") val ratingStars: Int? = null,
    @SerialName("image_url") val imageUrl: String? = null,
) {
    /** Dart `'${json['invoice_id'] ?? json['id'] ?? ''}'`。 */
    public val resolvedId: String get() = invoiceId ?: id ?: ""

    public val resolvedPhone: String get() = phone.orEmpty()

    /** `customer_name` 在 Dart 里可空（`?.toString()`，:212），这里保持可空。 */
    public val resolvedCustomerName: String? get() = customerName

    /** Dart `DealInvoiceStatus.fromApi(json['status']?.toString())`（:213）。 */
    public val resolvedStatus: DealInvoiceStatus get() = DealInvoiceStatus.fromApi(status)

    /** 原始状态串（服务端字面值），缺失为 `""`。 */
    public val statusApiValue: String get() = status.orEmpty()

    /**
     * Dart `DateTime.tryParse('${json['submitted_at'] ?? ''}') ?? epoch0`（:214）；
     * 本模块 `commonMain` 无 kotlinx-datetime，保留原始 ISO-8601 串给展示层。
     */
    public val resolvedSubmittedAt: String get() = submittedAt.orEmpty()

    /**
     * Dart `customerDisplay`（:201-206）：有客户名时 `"$phone $customerName"`，否则只有电话。
     */
    public val customerDisplay: String
        get() = if (!customerName.isNullOrEmpty()) "$resolvedPhone $customerName" else resolvedPhone

    /** Dart `matchesTab`（:224-233）：`approved` 同时吃 `approved_pending_rating` 与 `rated`。 */
    public fun matchesTab(tab: DealInvoiceTab): Boolean = when (tab) {
        DealInvoiceTab.All -> true
        DealInvoiceTab.PendingReview -> resolvedStatus == DealInvoiceStatus.PendingReview
        DealInvoiceTab.Approved ->
            resolvedStatus == DealInvoiceStatus.ApprovedPendingRating ||
                resolvedStatus == DealInvoiceStatus.Rated
        DealInvoiceTab.Rejected -> resolvedStatus == DealInvoiceStatus.Rejected
    }
}

/**
 * Dart `DealInvoiceCustomer`（deal_invoice_models.dart:35-60）—— 契约表 #54。
 *
 * 与 `/used-car-orders/customers` 不同，这里两组键都有兜底：
 * `customer_id ?? id`（**Int**）与 `display_name ?? name`（String），
 * `_intOf`（:56-59）还接受数字串。
 */
@Serializable
public data class DealInvoiceCustomer(
    @SerialName("customer_id") val customerId: Int? = null,
    val id: Int? = null,
    val phone: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    val name: String? = null,
) {
    /** Dart `_intOf(json['customer_id'] ?? json['id'])`，缺省 0。 */
    public val resolvedId: Int get() = customerId ?: id ?: 0

    public val resolvedPhone: String get() = phone.orEmpty()

    /** Dart `'${json['display_name'] ?? json['name'] ?? ''}'`。 */
    public val resolvedName: String get() = displayName ?: name ?: ""

    /** Dart `display`（:46）：`'$phone $name'`。 */
    public val display: String get() = "$resolvedPhone ${resolvedName}"
}

/**
 * Dart `fetchList` 的 record 返回值 `({List list, bool hasMore})`
 * （deal_invoice_api.dart:23,45）；`hasMore` 复用共享
 * [com.example.my_kmp_project.core.network.ListPage.hasMore]。
 */
public data class DealInvoiceListResult(
    public val items: List<DealInvoiceItem>,
    public val hasMore: Boolean,
)

/**
 * `POST /api/v1/deal-invoices` 请求体 —— Dart deal_invoice_api.dart:94-97（契约表 #55）。
 * 两键**恒发**：`customer_id`（Int）与 `image_url`（可为 null），
 * 因此必须用 [com.example.my_kmp_project.core.network.NetworkJsonExplicitNulls] 编码。
 */
@Serializable
public data class DealInvoiceCreateRequest(
    @SerialName("customer_id") val customerId: Int,
    @SerialName("image_url") val imageUrl: String? = null,
)

/**
 * `PATCH /api/v1/deal-invoices/{invoiceId}/resubmit` 请求体 ——
 * Dart deal_invoice_api.dart:115（契约表 #56）：驳回后重提，
 * `{'image_url': imageUrl}` 同样恒发（含 null）。
 */
@Serializable
public data class DealInvoiceResubmitRequest(
    @SerialName("image_url") val imageUrl: String? = null,
)

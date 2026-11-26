package com.example.my_kmp_project.feature.home.usedcar

import com.example.my_kmp_project.core.network.ApiClient
import com.example.my_kmp_project.core.network.ApiResponse
import com.example.my_kmp_project.core.network.NetworkError
import com.example.my_kmp_project.core.network.NetworkFacade
import com.example.my_kmp_project.core.network.NetworkJsonExplicitNulls
import com.example.my_kmp_project.core.network.decodeEnvelopeData
import com.example.my_kmp_project.core.network.getListPage
import com.example.my_kmp_project.core.network.pageQuery
import com.example.my_kmp_project.core.network.postApiDecoded
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

/**
 * 二手车业务单契约的 home 侧副本（Go `used_car/router.go:40-44`，对齐 Flutter
 * `used_car_order_api.dart`）。
 *
 * 为什么不在 home 直接 import `feature/mine/usedcar/UsedCarOrderApi.kt`：仓库
 * `scripts/check-layer-deps.sh` 第 1/3 条禁止 feature → feature（源码 import 与
 * Gradle 依赖都算违规）。本模块只用读列表/摘要、客户候选与建单四个端点，
 * 故按最小集落地在本模块内。
 *
 * 失败语义：`code != 0` 或 `data` 缺失 → 抛 [NetworkError.Business]（message 优先）。
 */
public class HomeUsedCarApi(
    private val api: ApiClient = NetworkFacade.api(),
) {
    /** #35 `GET /used-car-orders/summary`。 */
    public suspend fun fetchSummary(): HomeUsedCarSummary =
        api.getApi(
            path = SUMMARY_PATH,
            parseData = { el -> decodeEnvelopeData<HomeUsedCarSummary>(el) },
        ).dataOrThrow("加载摘要失败")

    /** #36 `GET /used-car-orders?page=&size=&status=&kind=`（page 1-based）。 */
    public suspend fun fetchList(
        status: String = "all",
        kind: String? = null,
        page: Int = 1,
        size: Int = DEFAULT_SIZE,
    ): List<HomeUsedCarOrderItem> =
        api.getListPage<HomeUsedCarOrderItem>(
            BASE_PATH,
            pageQuery(page, size, "status" to status, "kind" to kind),
        ).dataOrThrow("加载列表失败").items

    /** #38 `GET /used-car-orders/customers`（建单必须带 `customer_id`）。 */
    public suspend fun fetchCustomers(
        page: Int = 1,
        size: Int = CUSTOMERS_SIZE,
        q: String? = null,
    ): List<HomeUsedCarCustomer> =
        api.getListPage<HomeUsedCarCustomer>(
            CUSTOMERS_PATH,
            pageQuery(page, size, "q" to q?.trim()),
        ).dataOrThrow("加载客户失败").items

    /** #39 `POST /used-car-orders`（`image_url` 恒发，故用 explicit-nulls 编码）。 */
    public suspend fun create(request: HomeUsedCarOrderCreateRequest): HomeUsedCarOrderItem {
        val body = NetworkJsonExplicitNulls.encodeToString(request)
        return api.postApiDecoded<HomeUsedCarOrderItem>(BASE_PATH, body)
            .dataOrThrow("提交失败")
    }

    private companion object {
        const val BASE_PATH: String = "/api/v1/used-car-orders"
        const val SUMMARY_PATH: String = "$BASE_PATH/summary"
        const val CUSTOMERS_PATH: String = "$BASE_PATH/customers"
        const val DEFAULT_SIZE: Int = 50
        const val CUSTOMERS_SIZE: Int = 50
    }
}

/** 二手车业务单摘要（`used_car/summary` 的 `data`，此处只取统计栏）。 */
@Serializable
public data class HomeUsedCarSummary(
    val stats: HomeUsedCarStats? = null,
) {
    public val resolvedStats: HomeUsedCarStats get() = stats ?: HomeUsedCarStats()
}

/** 摘要里的四个计数（缺省全 0）。 */
@Serializable
public data class HomeUsedCarStats(
    val submitted: Int? = null,
    @SerialName("pending_review") val pendingReview: Int? = null,
    val approved: Int? = null,
    val rejected: Int? = null,
) {
    public val resolvedSubmitted: Int get() = submitted ?: 0
    public val resolvedPendingReview: Int get() = pendingReview ?: 0
    public val resolvedApproved: Int get() = approved ?: 0
    public val resolvedRejected: Int get() = rejected ?: 0
}

/** 列表/建单返回的业务单行。 */
@Serializable
public data class HomeUsedCarOrderItem(
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
) {
    public val resolvedOrderId: String get() = orderId.orEmpty()
    public val resolvedKind: String get() = kind.orEmpty()
    public val resolvedKindLabel: String get() = kindLabel.orEmpty()
    public val resolvedAmountLabel: String get() = amountLabel ?: "业务金额"
    public val resolvedCustomerName: String get() = customerName.orEmpty()
    public val resolvedPhone: String get() = phone.orEmpty()
    public val resolvedVehicleModel: String get() = vehicleModel.orEmpty()
    public val resolvedPlateNo: String get() = plateNo.orEmpty()
    public val resolvedVin: String get() = vin.orEmpty()
    public val resolvedMileageKm: Int get() = mileageKm ?: 0
    public val resolvedModelYear: Int get() = modelYear ?: 0
    public val resolvedAmount: Double get() = amount ?: 0.0
    public val resolvedSubmittedAt: String get() = submittedAt.orEmpty()

    /** 状态文案（`pending_review` / `approved_pending_rating` / `rated` / `rejected`）。 */
    public val statusLabel: String
        get() = when (status.orEmpty()) {
            "pending_review" -> "待审核"
            "approved_pending_rating" -> "已通过待评价"
            "rated" -> "已通过已评价"
            "rejected" -> "未通过"
            else -> status.orEmpty()
        }
}

/** 建单可选的客户（`GET /used-car-orders/customers`）。 */
@Serializable
public data class HomeUsedCarCustomer(
    @SerialName("customer_id") val customerId: Int? = null,
    @SerialName("display_name") val displayName: String? = null,
    val phone: String? = null,
) {
    public val resolvedCustomerId: Int get() = customerId ?: 0
    public val resolvedDisplayName: String get() = displayName.orEmpty()
    public val resolvedPhone: String get() = phone.orEmpty()
}

/** `POST /used-car-orders` 请求体（建单即 `pending_review`）。 */
@Serializable
public data class HomeUsedCarOrderCreateRequest(
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

/** 与 `feature/mine` 版 `dataOrThrow` 同义：业务码失败或 `data` 缺失都抛错。 */
private fun <T> ApiResponse<T>.dataOrThrow(fallback: String): T {
    val text = message?.takeIf { it.isNotEmpty() }
    if (!isSuccess) {
        throw NetworkError.Business(code, text ?: "业务请求失败")
    }
    return data ?: throw NetworkError.Business(code, text ?: fallback)
}

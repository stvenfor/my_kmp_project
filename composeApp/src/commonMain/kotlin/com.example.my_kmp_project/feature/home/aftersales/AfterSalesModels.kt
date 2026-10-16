package com.example.my_kmp_project.feature.home.aftersales

import com.example.my_kmp_project.core.network.FlexibleIntSerializer
import com.example.my_kmp_project.feature.home.api.homeDatePart
import com.example.my_kmp_project.feature.home.api.homeWireLiteralTrue
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Flutter 售后专区契约模型（`features/home/lib/after_sales/model/after_sales_models.dart`，
 * d7876a1）。契约表 #47–#50。
 *
 * 可空性策略与 `feature/home/api` 其余 DTO 一致（见 `TransactionModel` KDoc）。
 */

/** #47/#48/#49 的工单行（Dart `AfterSalesRecord`，after_sales_models.dart:3-49）。 */
@Serializable
public data class AfterSalesRecord(
    @SerialName("record_id")
    @Serializable(with = FlexibleIntSerializer::class) val recordIdRaw: Int? = null,
    @SerialName("store_id")
    @Serializable(with = FlexibleIntSerializer::class) val storeIdRaw: Int? = null,
    @SerialName("appointment_id")
    @Serializable(with = FlexibleIntSerializer::class) val appointmentIdRaw: Int? = null,
    @SerialName("customer_name") val customerNameRaw: String? = null,
    @SerialName("customer_phone") val customerPhoneRaw: String? = null,
    @SerialName("service_kind")
    @Serializable(with = FlexibleIntSerializer::class) val serviceKindRaw: Int? = null,
    @SerialName("service_kind_label") val serviceKindLabelRaw: String? = null,
    @SerialName("title") val titleRaw: String? = null,
    @SerialName("content") val contentRaw: String? = null,
    @SerialName("service_date") val serviceDateRaw: String? = null,
    @SerialName("plate_no") val plateNoRaw: String? = null,
    @SerialName("mileage")
    @Serializable(with = FlexibleIntSerializer::class) val mileageRaw: Int? = null,
) {
    /** Dart `_int(json['record_id'])`：数字/数字串都吃，缺省 0（:33,87-91）。 */
    public val recordId: Int get() = recordIdRaw ?: 0

    /** Dart `_int(json['store_id'])`（:34）。 */
    public val storeId: Int get() = storeIdRaw ?: 0

    /** Dart `json['appointment_id'] == null ? null : _int(...)`（:35-37）——**可空**。 */
    public val appointmentId: Int? get() = appointmentIdRaw

    /** Dart `(json['customer_name'] as String?) ?? ''`（:38）。 */
    public val customerName: String get() = customerNameRaw.orEmpty()

    /** Dart `(json['customer_phone'] as String?) ?? ''`（:39）。 */
    public val customerPhone: String get() = customerPhoneRaw.orEmpty()

    /**
     * Dart `_int(json['service_kind'])`（:40）：**读**是整数（0 维修 / 1 保养），
     * 但 #49 的**写**入参是字符串（`after_sales_create_page.dart:27` 传 `'1'`），两侧不对称保留。
     */
    public val serviceKind: Int get() = serviceKindRaw ?: 0

    /** Dart `(json['service_kind_label'] as String?) ?? ''`（:41）。 */
    public val serviceKindLabel: String get() = serviceKindLabelRaw.orEmpty()

    /** Dart `(json['title'] as String?) ?? ''`（:42）。 */
    public val title: String get() = titleRaw.orEmpty()

    /** Dart `(json['content'] as String?) ?? ''`（:43）。 */
    public val content: String get() = contentRaw.orEmpty()

    /** Dart `(json['service_date'] as String?) ?? ''`（:44）。 */
    public val serviceDate: String get() = serviceDateRaw.orEmpty()

    /** Dart `(json['plate_no'] as String?) ?? ''`（:45）。 */
    public val plateNo: String get() = plateNoRaw.orEmpty()

    /** Dart `json['mileage'] == null ? null : _int(...)`（:46）——**可空**。 */
    public val mileage: Int? get() = mileageRaw
}

/**
 * #50 `GET /after-sales/pending-appointments` 的行（Dart `AfterSalesAppointment`，:51-73）。
 *
 * 日期是**按 `T` 截断的字符串**，不是解析后的 `DateTime`（:63-66）。
 */
@Serializable
public data class AfterSalesAppointment(
    @SerialName("appointment_id")
    @Serializable(with = FlexibleIntSerializer::class) val appointmentIdRaw: Int? = null,
    @SerialName("customer_name") val customerNameRaw: String? = null,
    @SerialName("appointment_date") val appointmentDateRaw: String? = null,
) {
    /** Dart `_int(json['appointment_id'])`（:68）。 */
    public val appointmentId: Int get() = appointmentIdRaw ?: 0

    /** Dart `(json['customer_name'] as String?) ?? ''`（:69）。 */
    public val customerName: String get() = customerNameRaw.orEmpty()

    /**
     * Dart `dateRaw is String ? dateRaw.split('T').first : '$dateRaw'`（:63-66,70）：
     * 字符串按 `T` 截断；非字符串（数字时间戳）走插值；JSON `null` 在 Dart 里得到字面量
     * `"null"`，此处 bug-for-bug 保留。
     */
    public val appointmentDate: String get() = homeDatePart(appointmentDateRaw)
}

/**
 * #47 的返回值（Dart `AfterSalesListResult`，after_sales_models.dart:75-85）。
 *
 * `data` 是**裸 map** `{list, pagination{total}, can_create}`，**不能**塞进
 * `core/network` 的 `ListPage` —— 那样会丢掉 [canCreate]，而它决定「新建工单」入口是否出现
 * （`after_sales_list_viewmodel.dart:33`）。[hasMore] 用 Dart 的请求页方言
 * `page * size < (pagination.total ?? list.length)`（`after_sales_api.dart:30-34`）。
 */
public data class AfterSalesListResult(
    val list: List<AfterSalesRecord>,
    val hasMore: Boolean,
    val canCreate: Boolean,
)

/**
 * #47 `data` 的裸 map 读法（仅契约层内部使用；对外只暴露 [AfterSalesListResult]）。
 *
 * Dart 直接把 `data` 当 `Map<String, dynamic>` 取键（after_sales_api.dart:18-31）。
 */
@Serializable
internal data class AfterSalesRecordsWire(
    @SerialName("list") val list: List<AfterSalesRecord>? = null,
    @SerialName("pagination") val pagination: AfterSalesPaginationWire? = null,
    @SerialName("can_create") val canCreateRaw: JsonElement? = null,
) {
    /**
     * Dart `data['can_create'] == true`（:31）——只有真 `true` 才放开新建入口；
     * 字符串 / 数字 / null 全当 false（判断见 [homeWireLiteralTrue]）。
     */
    val canCreate: Boolean get() = homeWireLiteralTrue(canCreateRaw)
}

/** #47 的 `data.pagination`；Dart 只读 `total`，且 `pagination` 缺失时退化为 `list.length`。 */
@Serializable
internal data class AfterSalesPaginationWire(
    @SerialName("total")
    @Serializable(with = FlexibleIntSerializer::class) val totalRaw: Int? = null,
)

/** #50 的 `data` 裸 map（`{list: [...]}`，after_sales_api.dart:97-112）。 */
@Serializable
internal data class AfterSalesPendingAppointmentsWire(
    @SerialName("list") val list: List<AfterSalesAppointment>? = null,
)

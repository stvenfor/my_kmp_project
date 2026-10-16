package com.example.my_kmp_project.feature.home.api

import com.example.my_kmp_project.core.network.FlexibleIntSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Flutter 首页待办条契约模型（`features/home/lib/home/model/home_todo_models.dart`，d7876a1）。
 * 契约表 #15–#21。
 *
 * 与同目录内 `feature/home/HomeTodoModels.kt`（mock 引擎在用）的关系：那份是**内部 UI 模型**，
 * 本份是**HTTP 契约模型**（字段名与 Dart 一致、可空性对齐 BFF）。两者刻意分离，
 * UI 接线（M2/M3）时才由上层把契约模型喂给页面，mock 引擎不受影响。
 *
 * 可空性策略见 [TransactionModel] 的 KDoc（线上传输字段可空 + 派生属性给 Dart 默认值）。
 */

/**
 * Dart `HomeTodoSize`（home_todo_models.dart:2-21，ADR 0009）：
 * **卡片尺寸由 `type` 客户端推导**，服务端不下发尺寸。
 */
public enum class HomeTodoCardSize(public val cells: Int) {
    Small(1),
    Medium(2),
    Large(4),
    ;

    public companion object {
        /**
         * Dart `HomeTodoSize.fromType`：`partner_pending` → large；
         * `follow_up_customer` / `after_sales_appointment` → medium；其余 → small。
         */
        public fun fromType(type: String): HomeTodoCardSize = when (type) {
            "partner_pending" -> Large
            "follow_up_customer", "after_sales_appointment" -> Medium
            else -> Small
        }
    }
}

/** #15 `GET /home/todo-cards` 的行模型（Dart `HomeTodoCard`，:23-74）。 */
@Serializable
public data class HomeTodoCard(
    @SerialName("type") val typeRaw: String? = null,
    @SerialName("title") val titleRaw: String? = null,
    @SerialName("subtitle") val subtitleRaw: String? = null,
    @SerialName("action_label") val actionLabelRaw: String? = null,
    @SerialName("action_route") val actionRouteRaw: String? = null,
    @SerialName("count") @Serializable(with = FlexibleIntSerializer::class) val countRaw: Int? = null,
    @SerialName("image_url") val imageUrl: String? = null,
) {
    /** Dart `json['type']?.toString() ?? ''`（:46）。 */
    public val type: String get() = typeRaw.orEmpty()

    /** Dart `json['title']?.toString() ?? ''`（:47）。 */
    public val title: String get() = titleRaw.orEmpty()

    /** Dart `json['subtitle']?.toString() ?? ''`（:48）。 */
    public val subtitle: String get() = subtitleRaw.orEmpty()

    /** Dart `json['action_label']?.toString() ?? '去查看'`（:49）。 */
    public val actionLabel: String get() = actionLabelRaw ?: "去查看"

    /** Dart `json['action_route']?.toString() ?? ''`（:50）。 */
    public val actionRoute: String get() = actionRouteRaw.orEmpty()

    /** Dart `(json['count'] as num?)?.toInt() ?? 0`（:51）。 */
    public val count: Int get() = countRaw ?: 0

    /** Dart `HomeTodoCard.size`（:42）——尺寸推导见 [HomeTodoCardSize.fromType]。 */
    public val size: HomeTodoCardSize get() = HomeTodoCardSize.fromType(type)
}

/** #16 `GET /home/join-applications` 的行模型（Dart `HomeTodoJoinApplication`，:76-106）。 */
@Serializable
public data class HomeTodoJoinApplication(
    @SerialName("application_id")
    @Serializable(with = FlexibleIntSerializer::class) val applicationIdRaw: Int? = null,
    @SerialName("store_id")
    @Serializable(with = FlexibleIntSerializer::class) val storeIdRaw: Int? = null,
    @SerialName("applicant_user_id") val applicantUserIdRaw: String? = null,
    @SerialName("applicant_name") val applicantNameRaw: String? = null,
    @SerialName("status")
    @Serializable(with = FlexibleIntSerializer::class) val statusRaw: Int? = null,
    @SerialName("created_at") val createdAtRaw: String? = null,
) {
    /** Dart `(json['application_id'] as num?)?.toInt() ?? 0`（:98）。 */
    public val applicationId: Int get() = applicationIdRaw ?: 0

    /** Dart `(json['store_id'] as num?)?.toInt() ?? 0`（:99）。 */
    public val storeId: Int get() = storeIdRaw ?: 0

    /** Dart `json['applicant_user_id']?.toString() ?? ''`（:100）。 */
    public val applicantUserId: String get() = applicantUserIdRaw.orEmpty()

    /** Dart `json['applicant_name']?.toString() ?? ''`（:101）。 */
    public val applicantName: String get() = applicantNameRaw.orEmpty()

    /** Dart `(json['status'] as num?)?.toInt() ?? 0`（:102）。 */
    public val status: Int get() = statusRaw ?: 0

    /**
     * Dart `_parseDateTime(json['created_at'])` → `DateTime?`（:103）。
     * KMP 保留 ISO 串，不可解析 / 缺失 → null（见 `homeIsoDateTimeOrNull`）。
     */
    public val createdAt: String? get() = homeIsoDateTimeOrNull(createdAtRaw)

    /** Dart `displayName`：`applicant_name` trim 后为空则用 `applicant_user_id`（:93-94）。 */
    public val displayName: String
        get() = applicantName.trim().ifEmpty { applicantUserId }
}

/** #19 `GET /home/follow-up-customers` 的行模型（Dart `HomeTodoFollowUpCustomer`，:108-126）。 */
@Serializable
public data class HomeTodoFollowUpCustomer(
    @SerialName("customer_id")
    @Serializable(with = FlexibleIntSerializer::class) val customerIdRaw: Int? = null,
    @SerialName("display_name") val displayNameRaw: String? = null,
    @SerialName("next_follow_up_at") val nextFollowUpAtRaw: String? = null,
) {
    /** Dart `(json['customer_id'] as num?)?.toInt() ?? 0`（:121）。 */
    public val customerId: Int get() = customerIdRaw ?: 0

    /** Dart `json['display_name']?.toString() ?? '客户'`（:122）。 */
    public val displayName: String get() = displayNameRaw ?: "客户"

    /** Dart `_parseDateTime(json['next_follow_up_at'])` → `DateTime?`（:123）。 */
    public val nextFollowUpAt: String? get() = homeIsoDateTimeOrNull(nextFollowUpAtRaw)
}

/** #20 `GET /home/after-sales-appointments` 的行模型（Dart `HomeTodoAppointment`，:128-149）。 */
@Serializable
public data class HomeTodoAppointment(
    @SerialName("appointment_id")
    @Serializable(with = FlexibleIntSerializer::class) val appointmentIdRaw: Int? = null,
    @SerialName("customer_name") val customerNameRaw: String? = null,
    @SerialName("appointment_date") val appointmentDateRaw: String? = null,
    @SerialName("status")
    @Serializable(with = FlexibleIntSerializer::class) val statusRaw: Int? = null,
) {
    /** Dart `(json['appointment_id'] as num?)?.toInt() ?? 0`（:143）。 */
    public val appointmentId: Int get() = appointmentIdRaw ?: 0

    /** Dart `json['customer_name']?.toString() ?? '预约客户'`（:144）。 */
    public val customerName: String get() = customerNameRaw ?: "预约客户"

    /** Dart `_parseDateTime(json['appointment_date'])` → `DateTime?`（:145）。 */
    public val appointmentDate: String? get() = homeIsoDateTimeOrNull(appointmentDateRaw)

    /** Dart `(json['status'] as num?)?.toInt() ?? 0`（:146）。 */
    public val status: Int get() = statusRaw ?: 0
}

/** #21 `GET /home/store-review-orders` 的行模型（Dart `HomeTodoReviewOrder`，:151-172）。 */
@Serializable
public data class HomeTodoReviewOrder(
    @SerialName("order_id")
    @Serializable(with = FlexibleIntSerializer::class) val orderIdRaw: Int? = null,
    @SerialName("title") val titleRaw: String? = null,
    @SerialName("status")
    @Serializable(with = FlexibleIntSerializer::class) val statusRaw: Int? = null,
    @SerialName("created_at") val createdAtRaw: String? = null,
) {
    /** Dart `(json['order_id'] as num?)?.toInt() ?? 0`（:165）。 */
    public val orderId: Int get() = orderIdRaw ?: 0

    /** Dart `json['title']?.toString() ?? '审核单'`（:166）。 */
    public val title: String get() = titleRaw ?: "审核单"

    /** Dart `(json['status'] as num?)?.toInt() ?? 0`（:167）。 */
    public val status: Int get() = statusRaw ?: 0

    /** Dart `_parseDateTime(json['created_at'])` → `DateTime?`（:168）。 */
    public val createdAt: String? get() = homeIsoDateTimeOrNull(createdAtRaw)
}

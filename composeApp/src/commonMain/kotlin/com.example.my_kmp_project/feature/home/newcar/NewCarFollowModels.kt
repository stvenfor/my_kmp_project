package com.example.my_kmp_project.feature.home.newcar

import com.example.my_kmp_project.core.network.FlexibleIntSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Flutter 新车跟进契约模型（`features/home/lib/new_car_follow/model/new_car_follow_models.dart`，
 * d7876a1）。契约表 #40–#46。
 *
 * 可空性策略与 `feature/home/api` 其余 DTO 一致（见 `TransactionModel` KDoc）：
 * 线上字段可空，Dart 的默认值由派生属性给出。
 */

/**
 * Dart `NewCarFollowTab`（new_car_follow_models.dart:2-26）。
 *
 * [query] 必须保留**中文字面量** `intent_band=高|中|低` 与 `overdue=1`；
 * `all` 不带任何过滤键（契约表 #41）。
 */
public enum class NewCarFollowTab(public val label: String, public val query: Map<String, String>) {
    All("全部", emptyMap()),
    High("高意向", mapOf("intent_band" to "高")),
    Medium("中意向", mapOf("intent_band" to "中")),
    Low("低意向", mapOf("intent_band" to "低")),
    Overdue("逾期", mapOf("overdue" to "1")),
    ;
}

/** #40 `GET /new-car-follow-files/summary` 的 `stats`（Dart `NewCarFollowStats`，:28-49）。 */
@Serializable
public data class NewCarFollowStats(
    @SerialName("active")
    @Serializable(with = FlexibleIntSerializer::class) val activeRaw: Int? = null,
    @SerialName("overdue")
    @Serializable(with = FlexibleIntSerializer::class) val overdueRaw: Int? = null,
    @SerialName("high_intent")
    @Serializable(with = FlexibleIntSerializer::class) val highIntentRaw: Int? = null,
    @SerialName("lost")
    @Serializable(with = FlexibleIntSerializer::class) val lostRaw: Int? = null,
) {
    /** Dart `_int(json['active'])`：数字/数字串都吃，缺省 0（:43,189-193）。 */
    public val active: Int get() = activeRaw ?: 0

    /** Dart `_int(json['overdue'])`（:44）。 */
    public val overdue: Int get() = overdueRaw ?: 0

    /** Dart `_int(json['high_intent'])`（:45）。 */
    public val highIntent: Int get() = highIntentRaw ?: 0

    /** Dart `_int(json['lost'])`（:46）。 */
    public val lost: Int get() = lostRaw ?: 0

    public companion object {
        /** Dart `stats` 缺失时按 `const {}` 解析 → 全 0（:72-74）。 */
        public val Empty: NewCarFollowStats get() = NewCarFollowStats()
    }
}

/** #40 `GET /new-car-follow-files/summary` 的 `data`（Dart `NewCarFollowSummary`，:51-77）。 */
@Serializable
public data class NewCarFollowSummary(
    @SerialName("display_name") val displayNameRaw: String? = null,
    @SerialName("avatar_url") val avatarUrlRaw: String? = null,
    @SerialName("position_label") val positionLabelRaw: String? = null,
    @SerialName("store_name") val storeNameRaw: String? = null,
    @SerialName("stats") val statsRaw: NewCarFollowStats? = null,
) {
    /** Dart `(json['display_name'] as String?) ?? ''`（:68）。 */
    public val displayName: String get() = displayNameRaw.orEmpty()

    /** Dart `(json['avatar_url'] as String?) ?? ''`（:69）。 */
    public val avatarUrl: String get() = avatarUrlRaw.orEmpty()

    /** Dart `(json['position_label'] as String?) ?? ''`（:70）。 */
    public val positionLabel: String get() = positionLabelRaw.orEmpty()

    /** Dart `(json['store_name'] as String?) ?? ''`（:71）。 */
    public val storeName: String get() = storeNameRaw.orEmpty()

    /** Dart `stats` 缺失 → `const {}` → 全 0（:72-74）。 */
    public val stats: NewCarFollowStats get() = statsRaw ?: NewCarFollowStats.Empty
}

/**
 * #41/#42/#43/#44 的档案行（Dart `NewCarFollowFile`，new_car_follow_models.dart:79-121）。
 *
 * **`file_id` / `customer_id` 是字符串**：Dart 写的是 `'${json['file_id'] ?? ''}'`
 * （:108-109），即数字会被字符串化、缺失变空串，`#42`/`#44`/`#45` 的路径参数也用它。
 */
@Serializable
public data class NewCarFollowFile(
    @SerialName("file_id") val fileIdRaw: String? = null,
    @SerialName("customer_id") val customerIdRaw: String? = null,
    @SerialName("customer_name") val customerNameRaw: String? = null,
    @SerialName("customer_phone") val customerPhoneRaw: String? = null,
    @SerialName("follow_level") val followLevelRaw: String? = null,
    @SerialName("intent_band") val intentBandRaw: String? = null,
    @SerialName("stage") val stageRaw: String? = null,
    @SerialName("vehicle_interest") val vehicleInterestRaw: String? = null,
    @SerialName("next_follow_up_at") val nextFollowUpAtRaw: String? = null,
    @SerialName("owner_user_id") val ownerUserIdRaw: String? = null,
    @SerialName("owner_display_name") val ownerDisplayNameRaw: String? = null,
) {
    /** Dart `'${json['file_id'] ?? ''}'`（:108）——字符串，缺失为 `''`。 */
    public val fileId: String get() = fileIdRaw.orEmpty()

    /** Dart `'${json['customer_id'] ?? ''}'`（:109）——字符串，缺失为 `''`。 */
    public val customerId: String get() = customerIdRaw.orEmpty()

    /** Dart `(json['customer_name'] as String?) ?? ''`（:110）。 */
    public val customerName: String get() = customerNameRaw.orEmpty()

    /** Dart `(json['customer_phone'] as String?) ?? ''`（:111）。 */
    public val customerPhone: String get() = customerPhoneRaw.orEmpty()

    /** Dart 跟进级别 `H/A/B/E`（:112，取值语义见 new_car_follow_models.dart:173-186）。 */
    public val followLevel: String get() = followLevelRaw.orEmpty()

    /** Dart 意向带（服务端可回中文 `高/中/低`，:113）。 */
    public val intentBand: String get() = intentBandRaw.orEmpty()

    /** Dart 阶段 `new/following/test_drive/quoted/won/lost`（:114）。 */
    public val stage: String get() = stageRaw.orEmpty()

    /** Dart `(json['vehicle_interest'] as String?) ?? ''`（:115）。 */
    public val vehicleInterest: String get() = vehicleInterestRaw.orEmpty()

    /**
     * Dart `json['next_follow_up_at'] as String?`（:116）：原样保留，可为 null。
     * 与 home 待办的 `DateTime?` 字段不同，这里 Dart 不做日期解析，故不套 [homeIsoDateTimeOrNull]。
     */
    public val nextFollowUpAt: String? get() = nextFollowUpAtRaw

    /** Dart `(json['owner_user_id'] as String?) ?? ''`（:117）。 */
    public val ownerUserId: String get() = ownerUserIdRaw.orEmpty()

    /** Dart `(json['owner_display_name'] as String?) ?? ''`（:118）。 */
    public val ownerDisplayName: String get() = ownerDisplayNameRaw.orEmpty()
}

/**
 * #45/#46 的跟进流水（Dart `NewCarFollowLog`，new_car_follow_models.dart:123-159）。
 *
 * #46 写流水可以**由服务端**移动档案的 `follow_level` / `next_follow_up_at`
 * （契约表 #46 备注）：因此写接口只返回服务端解码出的这条流水，绝不做本地乐观改写。
 */
@Serializable
public data class NewCarFollowLog(
    @SerialName("log_id") val logIdRaw: String? = null,
    @SerialName("file_id") val fileIdRaw: String? = null,
    @SerialName("author_user_id") val authorUserIdRaw: String? = null,
    @SerialName("author_display_name") val authorDisplayNameRaw: String? = null,
    @SerialName("body") val bodyRaw: String? = null,
    @SerialName("follow_level") val followLevelRaw: String? = null,
    @SerialName("intent_band") val intentBandRaw: String? = null,
    @SerialName("next_follow_up_at") val nextFollowUpAtRaw: String? = null,
    @SerialName("created_at") val createdAtRaw: String? = null,
) {
    /** Dart `'${json['log_id'] ?? ''}'`（:148）——字符串化，缺失为 `''`。 */
    public val logId: String get() = logIdRaw.orEmpty()

    /** Dart `'${json['file_id'] ?? ''}'`（:149）。 */
    public val fileId: String get() = fileIdRaw.orEmpty()

    /** Dart `(json['author_user_id'] as String?) ?? ''`（:150）。 */
    public val authorUserId: String get() = authorUserIdRaw.orEmpty()

    /** Dart `(json['author_display_name'] as String?) ?? ''`（:151）。 */
    public val authorDisplayName: String get() = authorDisplayNameRaw.orEmpty()

    /** Dart `(json['body'] as String?) ?? ''`（:152）。 */
    public val body: String get() = bodyRaw.orEmpty()

    /** Dart `(json['follow_level'] as String?) ?? ''`（:153）——这条流水落定的级别。 */
    public val followLevel: String get() = followLevelRaw.orEmpty()

    /** Dart `(json['intent_band'] as String?) ?? ''`（:154）。 */
    public val intentBand: String get() = intentBandRaw.orEmpty()

    /** Dart `json['next_follow_up_at'] as String?`（:155）——可为 null，原样保留。 */
    public val nextFollowUpAt: String? get() = nextFollowUpAtRaw

    /** Dart `'${json['created_at'] ?? ''}'`（:156）——字符串化，缺失为 `''`。 */
    public val createdAt: String get() = createdAtRaw.orEmpty()
}

/**
 * Dart 记录型返回值 `({List<T> list, bool hasMore})`
 * （new_car_follow_api.dart:23 / :120）。
 *
 * [hasMore] 走 Dart 的**请求页方言** `page * size < (pagination.total ?: list.length)`
 * （:41-43、:136-138），不是 core 的 `ListPagination.hasMore`。
 */
public data class NewCarFollowPage<T>(
    val list: List<T>,
    val hasMore: Boolean,
)

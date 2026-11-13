package com.example.my_kmp_project.feature.home.api

import com.example.my_kmp_project.core.network.ListPage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Flutter `TransactionModel`（`features/home/lib/home/model/transaction_model.dart`，d7876a1）。
 * 契约表 #12 / #13：`GET /transactions`（`limit`/`offset` 方言）与 `GET /transactions/:id`。
 *
 * 跨模块复用（契约表 §6）：home 收支列表 + 详情、Mine 调试页（#14 走同一 DTO，
 * 只是查询方言换成 `page`/`size`）。
 *
 * 可空性策略（全 home 契约层统一）：
 * 线上传输字段一律「可空 + `resolved*`/派生属性给 Dart 的默认值」，
 * 因为 Go BFF 会对 Dart 里 `json['x'] ?? ''` 的字段显式下发 `null`，
 * 而 `NetworkJson` 未开 `coerceInputValues` —— 非空字段遇 `null` 会整行解码失败，
 * 被 `core/network` 的列表解码静默丢弃。
 *
 * **唯一例外是 [id]**：Dart `(json['id'] as num).toInt()`（transaction_model.dart:18）在
 * `id` 缺失时直接抛，绝不退化成 0，故此处声明为非空无默认值，缺失即解码异常。
 */
@Serializable
public data class TransactionModel(
    /** Dart `(json['id'] as num).toInt()`：缺失 / 非数字 → 抛（刻意不兜底）。 */
    @SerialName("id") val id: Int,
    @SerialName("type") val typeRaw: String? = null,
    @SerialName("category") val categoryRaw: String? = null,
    @SerialName("amount") val amountRaw: Double? = null,
    @SerialName("date") val dateRaw: String? = null,
    @SerialName("user_id") val userIdSnake: String? = null,
    @SerialName("userId") val userIdCamel: String? = null,
    @SerialName("note") val noteRaw: String? = null,
    @SerialName("created_at") val createdAtSnake: String? = null,
    @SerialName("createdAt") val createdAtCamel: String? = null,
    @SerialName("updated_at") val updatedAtSnake: String? = null,
    @SerialName("updatedAt") val updatedAtCamel: String? = null,
) {
    /** Dart `json['type']?.toString() ?? ''`（:20）。 */
    public val type: String get() = typeRaw.orEmpty()

    /** Dart `json['category']?.toString() ?? ''`（:21）。 */
    public val category: String get() = categoryRaw.orEmpty()

    /** Dart `(json['amount'] as num?)?.toDouble() ?? 0`（:22）。 */
    public val amount: Double get() = amountRaw ?: 0.0

    /** Dart `json['date']?.toString() ?? ''`（:23）。 */
    public val date: String get() = dateRaw.orEmpty()

    /** Dart `_readString(json['user_id'] ?? json['userId'])`（:19,30-33）——可空。 */
    public val userId: String? get() = homeFirstNonNullString(userIdSnake, userIdCamel)

    /** Dart `json['note']?.toString()`（:24）——可空，空白保留。 */
    public val note: String? get() = noteRaw

    /** Dart `_readString(json['created_at'] ?? json['createdAt'])`（:25）。 */
    public val createdAt: String? get() = homeFirstNonNullString(createdAtSnake, createdAtCamel)

    /** Dart `_readString(json['updated_at'] ?? json['updatedAt'])`（:26）。 */
    public val updatedAt: String? get() = homeFirstNonNullString(updatedAtSnake, updatedAtCamel)
}

/**
 * Dart `typedef TransactionListResult = ResultModel<ListData<TransactionModel>>`
 * （transaction_model.dart:46）。KMP 侧信封由 `ApiResponse` 承担，
 * 列表体归一为 [ListPage]（Dart `ListData`），故 typedef 只映射到 [ListPage]。
 */
public typealias TransactionListResult = ListPage<TransactionModel>

/** Dart `typedef TransactionDetailResult = ResultModel<TransactionModel>`（:47）。 */
public typealias TransactionDetailResult = TransactionModel

/**
 * `PUT /api/v1/transactions/:id` body（后端 `transaction/handler.go:244-268` `UpdateLegacy` →
 * `entity.UpdateTransactionInput`：`model/entity/transaction.go:56-62`，五键全 `*string`/`*float64`
 * 即**稀疏**）。`NetworkJson` 关掉 `explicitNulls`，null 键整键省略，与 Go 指针语义一致。
 */
@Serializable
public data class TransactionUpdateRequest(
    val type: String? = null,
    val category: String? = null,
    val amount: Double? = null,
    val date: String? = null,
    val note: String? = null,
)

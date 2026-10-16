package com.example.my_kmp_project.core.network

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull

/**
 * Go BFF 列表页契约（Flutter `commons/network/lib/api/result_model.dart` 的
 * `PaginationModel` / `ListData` / `PageResult` 对等，d7876a1）。
 *
 * `data` 有三种真实形态，全部在这里归一，feature 层不再各写一份：
 * 1. `{ "list": [...] , "pagination": {...} }`（ mall / community / used-car …）
 * 2. `{ "items": [...] }`（wallet ledger、addresses、home todo-cards、IM friends …）
 * 3. 裸数组 `[...]`（部分 `/home/` 前缀的卡片接口）
 */
public data class ListPagination(
    val page: Int,
    val size: Int,
    val total: Int,
    val totalPages: Int?,
) {
    /** Dart `PaginationModel.hasMore`：totalPages 优先，其次 page*size&lt;total，否则 false。 */
    public val hasMore: Boolean
        get() = when {
            totalPages != null && totalPages > 0 -> page < totalPages
            size > 0 -> page * size < total
            else -> false
        }
}

public data class ListPage<T>(
    val items: List<T>,
    val pagination: ListPagination? = null,
) {
    /**
     * Dart `ListData.resolveHasMore(pageSize)`：无 `pagination` 时退化为
     * 「本页拿满 = 还有下一页」；[pageSize] ≤ 0 视为无下一页。
     */
    public fun hasMore(pageSize: Int): Boolean =
        pagination?.hasMore ?: (pageSize > 0 && items.size >= pageSize)
}

/**
 * 解码 `data` 为 [ListPage]（Dart `ListData.fromJson`）：
 * 非 object / 非 array 的畸形 `data` → 空页，不抛异常。
 * 数组元素里的非 object 项直接跳过（Dart `whereType<Map>`）。
 */
public inline fun <reified T> decodeListPage(el: JsonElement?): ListPage<T> {
    if (el == null || el is JsonNull) return ListPage(emptyList(), null)
    return when (el) {
        is JsonArray -> ListPage(decodeListItems<T>(el), null)
        is JsonObject -> {
            val rawList = el["list"] ?: el["items"]
            val items = if (rawList is JsonArray) decodeListItems<T>(rawList) else emptyList()
            ListPage(items, decodeListPagination(el["pagination"] as? JsonObject))
        }
        else -> ListPage(emptyList(), null)
    }
}

@PublishedApi
internal inline fun <reified T> decodeListItems(arr: JsonArray): List<T> =
    arr.filterIsInstance<JsonObject>().mapNotNull { decodeEnvelopeData<T>(it) }

@PublishedApi
internal fun decodeListPagination(obj: JsonObject?): ListPagination? {
    if (obj == null) return null
    return ListPagination(
        page = objFlexibleInt(obj["page"]) ?: 1,
        size = objFlexibleInt(obj["size"]) ?: 0,
        total = objFlexibleInt(obj["total"]) ?: 0,
        // BFF 侧 totalPages / total_pages 都出现过，两者等价取先出现的。
        totalPages = objFlexibleInt(obj["totalPages"]) ?: objFlexibleInt(obj["total_pages"]),
    )
}

@PublishedApi
internal fun objFlexibleInt(el: JsonElement?): Int? = when (el) {
    null, is JsonNull -> null
    is JsonPrimitive -> el.intOrNull ?: el.contentOrNull?.toDoubleOrNull()?.toInt()
    else -> null
}

/**
 * 构造查询串：**丢弃 null 与空串**（Dart 侧每个可选 query 都手写 `if (x != null) 'k': x`，
 * 空串同理省略，如 `/me/stores`、`/used-car-orders/customers?q=`）。
 */
public fun queryOf(vararg pairs: Pair<String, String?>): Map<String, String> =
    pairs.mapNotNull { (k, v) -> v?.takeIf { it.isNotBlank() }?.let { k to it } }.toMap()

/**
 * 分页查询（服务端 `page` 是 **1-based**；本地 0-based 调用方需先 `+1`，
 * 与 Flutter `page: localPage + 1` 一致）。可选参数走 [queryOf] 省略规则。
 */
public fun pageQuery(
    page: Int,
    size: Int,
    vararg extra: Pair<String, String?>,
): Map<String, String> = queryOf("page" to page.toString(), "size" to size.toString(), *extra)

/**
 * `limit`/`offset` 分页（Dart `/transactions`、`/wallet/ledger`：page 是 **0-based**，
 * `offset = page * size`）。
 */
public fun limitOffsetQuery(
    page: Int,
    size: Int,
    vararg extra: Pair<String, String?>,
): Map<String, String> =
    queryOf("limit" to size.toString(), "offset" to (page * size).toString(), *extra)

package com.example.my_kmp_project.feature.home.api

import com.example.my_kmp_project.core.network.ApiClient
import com.example.my_kmp_project.core.network.ApiResponse
import com.example.my_kmp_project.core.network.ListPagination
import com.example.my_kmp_project.core.network.ListPage
import com.example.my_kmp_project.core.network.NetworkCodes
import com.example.my_kmp_project.core.network.NetworkError
import com.example.my_kmp_project.core.network.NetworkJson
import com.example.my_kmp_project.core.network.decodeListPage
import com.example.my_kmp_project.core.network.getApi
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * `feature/home` 契约层共用的 BFF 读取语义（契约表 §0 / §2.3 / §2.5 / §3）。
 *
 * 只放「多个 home API 都要遵守的读法」：不改 `core/network`，也不在每个 API 对象里
 * 重新实现分页方言。
 */

/**
 * 根 JSON（object / array）；空体或畸形 JSON → null。
 *
 * Dart 侧 converter 先做 `json as Map<String, dynamic>`，类型不符即抛，再由
 * `HttpManager.request` 的 try/catch 包成 `HttpRequestException`
 * （`commons/network/lib/http/http.dart:455-473`）。
 */
fun homeJsonRootOrNull(raw: String?): JsonElement? {
    val text = raw?.trim().orEmpty()
    if (text.isEmpty()) return null
    return runCatching { NetworkJson.parseToJsonElement(text) }.getOrNull()
}

/**
 * Dart `map.containsKey('code')` —— 决定走 `ResultModel` 信封还是「裸 body 即数据」
 * （`transaction_api.dart:63`、`points_api.dart:51`）。
 */
fun homeEnvelopeCarriesCode(raw: String?): Boolean =
    (homeJsonRootOrNull(raw) as? JsonObject)?.containsKey("code") == true

/**
 * 「双信封」列表页读取（契约表 #12 / #24 的缺 `code` 分支）。
 *
 * - body 含 `code`：`data` 走 core 的 [decodeListPage]（A/B/C 型归一），保留原业务码；
 *   `data` 缺失 → 返回 `data = null`（Dart `ResultModel.fromJson` 的
 *   `rawData == null → null`，`result_model.dart:18`），由调用点按 Dart `_data()` 抛错。
 * - body **不含** `code`：整份 body 当列表体，并**合成 `code:0`**
 *   （Dart `transaction_api.dart:66-70` / `points_api.dart:58`）。
 *
 * [rootFallbackWhenDataMissing] 只为 #24 `GET /points/tasks` 的**手搓转换器**保留：
 * Dart 里 `data` 不是 map（含缺失）时改读**根 map** 的 `items`，且永远返回一个 list，
 * 不会像 `ResultModel.fromJson` 那样把 `data = null` 交给 `_data()` 抛错
 * （points_api.dart:43-59）。其余 home 列表（#15–#21）必须保持 `data = null` → 抛。
 */
suspend inline fun <reified T> ApiClient.getHomeListPage(
    path: String,
    query: Map<String, String> = emptyMap(),
    rootFallbackWhenDataMissing: Boolean = false,
): ApiResponse<ListPage<T>> {
    val resp = getApi<JsonElement>(path, query)
    if (!homeEnvelopeCarriesCode(resp.raw)) {
        return ApiResponse(
            code = NetworkCodes.BFF_OK,
            message = resp.message ?: "success",
            data = decodeListPage<T>(homeJsonRootOrNull(resp.raw)),
            raw = resp.raw,
            httpStatus = resp.httpStatus,
        )
    }
    val source: JsonElement? = resp.data
        ?: homeJsonRootOrNull(resp.raw)?.takeIf { rootFallbackWhenDataMissing }
    return ApiResponse(
        code = resp.code,
        message = resp.message,
        data = source?.let { decodeListPage<T>(it) },
        raw = resp.raw,
        httpStatus = resp.httpStatus,
    )
}

/**
 * Dart `ResultModel.listPage` 的读法（`new_car_follow_api.dart:31-40`、`:126-135`）：
 * **`data` 键缺失 / 为 JSON null 时得到 `data = null`**，再由调用点 `_data()` 抛错
 * （`result_model.dart:18`）。
 *
 * core 的 `getListPage` 会把缺失的 `data` 归一成空页，等于把「后端没返回 data」
 * 静默变成「列表为空」，所以 #41 / #45 用本函数。body 缺 `code` 时仍走 core 的 -1
 * 兜底（Dart `ResultModel.fromJson` 同款：缺 `code` 记 -1 → 失败）。
 */
suspend inline fun <reified T> ApiClient.getHomeListPageOrNull(
    path: String,
    query: Map<String, String> = emptyMap(),
): ApiResponse<ListPage<T>> {
    val resp = getApi<JsonElement>(path, query)
    val page: ListPage<T>? = resp.data?.let { decodeListPage<T>(it) }
    return ApiResponse(
        code = resp.code,
        message = resp.message,
        data = page,
        raw = resp.raw,
        httpStatus = resp.httpStatus,
    )
}

/**
 * Dart `ResultModel._data()`（`home_todo_api.dart:134-142`、`points_api.dart:113-121`）：
 * 非 `code:0` 或 `data` 缺失 → 抛 `HttpRequestException{message, code}`。
 *
 * KMP 对等异常是 [NetworkError.Business]（携带业务码，交给上层错误映射）；
 * 文案优先取信封 `message`，空白则用调用点的中文兜底。
 */
fun <T> ApiResponse<T>.homeDataOrThrow(fallback: String): T {
    val failure = message?.takeIf { it.isNotBlank() } ?: fallback
    if (!isSuccess) throw NetworkError.Business(code, failure)
    return data ?: throw NetworkError.Business(code, failure)
}

/** Dart `_ensureOk()`（`home_todo_api.dart:144-151`）：动作 POST 只看 `code`，`data` 忽略。 */
fun ApiResponse<*>.homeOkOrThrow(fallback: String) {
    val failure = message?.takeIf { it.isNotBlank() } ?: fallback
    if (!isSuccess) throw NetworkError.Business(code, failure)
}

/**
 * Dart `_data()`（`new_car_follow_api.dart:174-179`、`after_sales_api.dart:120-125`）抛**裸**
 * `Exception(result?.message ?? fallback)` —— 与 home_todo / points 的
 * `HttpRequestException` **有意不同**（契约表 §2.8 / §2.9），保留差异以免上层错误分类被改写。
 *
 * 与 Dart 的细微差别（刻意为之）：Dart 的 `message` 缺省是空串，会得到 `Exception('')`；
 * `core/network` 解析器把空白 message 归一为 null，因此这里落到中文兜底文案。
 */
fun <T> ApiResponse<T>.homeBareDataOrThrow(fallback: String): T {
    if (!isSuccess) throw Exception(message ?: fallback)
    return data ?: throw Exception(message ?: fallback)
}

/**
 * Dart `_readString(json['a'] ?? json['b'])`（`transaction_model.dart:19-26`）：
 * 只把 **null** 当缺失（空白串保留），`snake_case` / `camelCase` 双写都接受。
 */
fun homeFirstNonNullString(vararg values: String?): String? =
    values.firstOrNull { it != null }

/**
 * Dart 列表页 `hasMore` 的**调用点方言**：`page * size < (pagination.total ?: list.length)`
 * （`new_car_follow_api.dart:41-43`、`new_car_follow_api.dart:136-138`、
 * `after_sales_api.dart:30-34`）。
 *
 * 与 `core/network` 的 [ListPagination.hasMore] 不同：这里用的是**请求** page/size。
 * `pagination` 整体缺失时 Dart 的 `ListData.pagination` 也是 null → 同样落到 `list.length`；
 * `pagination` 在但缺 `total` 时两边都是 0（Dart `PaginationModel.total ?? 0`，core 同款），
 * 故无需特殊处理。#47 不同：它直接读裸 map，`_asInt(pagination?['total'])` 缺键回 **null**
 * → 落到 `list.length`（见 `AfterSalesPaginationWire.totalRaw` 的可空声明）。
 */
fun homeRequestedPageHasMore(
    page: Int,
    size: Int,
    pagination: ListPagination?,
    listSize: Int,
): Boolean = page * size < (pagination?.total ?: listSize)

/** 分页体里读 `pagination.total`（Dart `data['pagination']?['total']`）。 */
fun ListPage<*>.totalOrNull(): Int? = pagination?.total

/**
 * Dart 的 `json['k'] == true`（`points_models.dart:15,43,45`、`after_sales_api.dart:31`）：
 * **只有 JSON 字面量 `true` 算真**，字符串 `"true"`、`1`、`null` 全是 false。
 *
 * 线上字段读成 [JsonElement] 而不是 `Boolean?`：kotlinx 遇到非布尔原始值会抛
 * （`TreeJsonDecoder.decodeTaggedBoolean` → `booleanOrNull` 为 null 即抛），
 * 而 `decodeEnvelopeData` 的 `runCatching` 会把**整个**对象静默丢掉、调用点变成「data 缺失」；
 * Dart 则是宽容地当 false。用元素级判断保住这个差别。
 */
fun homeWireLiteralTrue(el: JsonElement?): Boolean =
    el is JsonPrimitive && !el.isString && el.content == "true"

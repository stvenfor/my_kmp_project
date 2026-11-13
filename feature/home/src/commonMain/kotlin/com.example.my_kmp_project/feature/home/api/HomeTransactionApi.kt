package com.example.my_kmp_project.feature.home.api

import com.example.my_kmp_project.core.network.ApiClient
import com.example.my_kmp_project.core.network.ApiResponse
import com.example.my_kmp_project.core.network.ListPage
import com.example.my_kmp_project.core.network.NetworkError
import com.example.my_kmp_project.core.network.NetworkFacade
import com.example.my_kmp_project.core.network.NetworkJson
import com.example.my_kmp_project.core.network.decodeListPage
import com.example.my_kmp_project.core.network.deleteApiOk
import com.example.my_kmp_project.core.network.getApi
import com.example.my_kmp_project.core.network.limitOffsetQuery
import com.example.my_kmp_project.core.network.pageQuery
import com.example.my_kmp_project.core.network.putApi
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement

/**
 * Flutter `TransactionApi`（`features/home/lib/home/api/transaction_api.dart`，d7876a1）。
 * 契约表 #12 / #13。
 *
 * 方言：**`limit`/`offset`，`page` 是 0-based**（`offset = page * size`，与 Go `listLegacy` 对齐，
 * transaction_api.dart:16-29）。Mine 调试页的 `page`/`size` 变体是 #14，不在此类。
 *
 * 抛错语义（保留 Dart 行为）：
 * - 列表：行缺 `id` → [TransactionModel] 解码抛异常（Dart `(json['id'] as num)` 同样整次调用抛，
 *   `commons/network/lib/http/http.dart:455-473` 会包成 `HttpRequestException`）。
 * - 业务码非 0 且无 `data` → **静默空页**（Dart `fetchPage` 的
 *   `result.data ?? ResultModel(code:0, data: ListData(list:[]))`，transaction_api.dart:32-37，
 *   且 `transaction_repository.dart:27-31` 完全不看 `code`）。
 * - 详情：`data` 缺失 → 抛 [NetworkError.Business]，文案 `交易记录不存在`
 *   （Dart `HttpRequestException(message: '交易记录不存在')`，:43）。
 */
public class TransactionApi(
    private val client: ApiClient = NetworkFacade.api(),
) {
    public companion object {
        /** Dart `TransactionApi.transactionsPath`（transaction_api.dart:14）。 */
        internal const val TRANSACTIONS_PATH: String = "/api/v1/transactions"

        /** Dart `TransactionRepository.pageSize`（transaction_repository.dart:16）。 */
        internal const val DEFAULT_SIZE: Int = 20
    }

    /**
     * #12 `GET /transactions?limit=&offset=`（legacy 方言，C3）。
     *
     * [page] **0-based**（Dart `ledger_list_controller.dart:31` 直接传控制器页码）；
     * [type] 为空串或 null 时省略 key（transaction_api.dart:28）。
     */
    public suspend fun fetchPage(
        page: Int,
        size: Int = DEFAULT_SIZE,
        type: String? = null,
    ): TransactionListResult = loadPage(limitOffsetQuery(page, size, "type" to type))

    /**
     * C3 双分页调用封装：`GET /api/v1/transactions` 上带 `page`（1-based manage）→
     * `page/size` 命中后端 `ListPage`；不带 → `limit/offset` 命中 `listLegacy`
     * （`transaction/handler.go:44-46` 的 `if c.Query("page") != "" { ListPage } else { listLegacy }`）。
     * 两种 query 串形状由 [transactionListQuery] 决定，契约测试逐一断言。
     */
    public suspend fun fetchPageDual(
        managePage: Int? = null,
        size: Int = DEFAULT_SIZE,
        legacyPage: Int = 0,
        type: String? = null,
    ): TransactionListResult = loadPage(transactionListQuery(managePage, size, legacyPage, type))

    private suspend fun loadPage(query: Map<String, String>): TransactionListResult {
        val resp = client.getApi<JsonElement>(TRANSACTIONS_PATH, query)
        // Dart `_parseListResult`：有 `code` 用 `data`，否则把整份 body 当 ListData（:61-71）。
        val body: JsonElement? = if (homeEnvelopeCarriesCode(resp.raw)) {
            resp.data
        } else {
            homeJsonRootOrNull(resp.raw)
        }
        return decodeStrictTransactionPage(body)
    }

    /**
     * C2 `PUT /transactions/:id`（后端 `transaction/router.go:50` `UpdateLegacy`）——
     * 走 `putApi`，稀疏 body 见 [TransactionUpdateRequest]。
     */
    public suspend fun update(id: Int, request: TransactionUpdateRequest): ApiResponse<TransactionModel> =
        client.putApi<TransactionUpdateRequest, TransactionModel>("$TRANSACTIONS_PATH/$id", request)

    /**
     * C2 `DELETE /transactions/:id`（后端 `transaction/router.go:51` `DeleteLegacy`，204）——
     * 走 `deleteApi`（`deleteApiOk` 内部即低层 `deleteApi`）。
     */
    public suspend fun delete(id: Int): ApiResponse<Unit> =
        client.deleteApiOk("$TRANSACTIONS_PATH/$id")

    /**
     * #13 `GET /transactions/:id`.
     *
     * 同 #12 的双信封：body 缺 `code` 时把整份 body 当作模型（:73-83）。
     */
    public suspend fun getTransaction(id: Int): TransactionDetailResult {
        val resp = client.getApi<JsonElement>("$TRANSACTIONS_PATH/$id")
        val body: JsonElement? = if (homeEnvelopeCarriesCode(resp.raw)) {
            resp.data
        } else {
            homeJsonRootOrNull(resp.raw)
        }
        if (body == null) {
            throw NetworkError.Business(resp.code, "交易记录不存在")
        }
        // 缺 `id` 等畸形模型：Dart `(json['id'] as num)` 直接抛，这里让序列化异常原样上抛。
        return NetworkJson.decodeFromJsonElement<TransactionModel>(body)
    }

    /**
     * 用 core 的 [decodeListPage] 归一 A/B/C 型形状（`list` / `items` / 裸数组，
     * 非 object 行按 Dart `whereType<Map>` 跳过），再**逐行严格解码**：
     * 任何一行缺 `id` 都会抛，而不是像默认解码那样静默丢行。
     */
    private fun decodeStrictTransactionPage(el: JsonElement?): TransactionListResult {
        val rawPage = decodeListPage<JsonElement>(el)
        val rows = rawPage.items.map { NetworkJson.decodeFromJsonElement<TransactionModel>(it) }
        return ListPage(rows, rawPage.pagination)
    }
}

/**
 * C3 双分页 query 构造（后端 `transaction/handler.go:44-46`）：
 * - [managePage] 非 null（1-based）→ [pageQuery] 的 `page/size`，后端走 manage 分页（ListPage）；
 * - [managePage] 为 null → [limitOffsetQuery] 的 `limit/offset`，后端走 Flutter legacy（listLegacy）。
 *
 * 两种 query 形状互斥：`page/size` 不含 `limit/offset`，反之亦然；契约测试逐一断言。
 */
public fun transactionListQuery(
    managePage: Int?,
    size: Int,
    legacyPage: Int = 0,
    type: String? = null,
): Map<String, String> = if (managePage != null) {
    pageQuery(managePage, size, "type" to type)
} else {
    limitOffsetQuery(legacyPage, size, "type" to type)
}

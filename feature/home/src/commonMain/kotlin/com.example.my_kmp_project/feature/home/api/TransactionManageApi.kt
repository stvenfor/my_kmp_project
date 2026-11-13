package com.example.my_kmp_project.feature.home.api

import com.example.my_kmp_project.core.network.ApiClient
import com.example.my_kmp_project.core.network.ApiResponse
import com.example.my_kmp_project.core.network.ListPage
import com.example.my_kmp_project.core.network.NetworkFacade
import com.example.my_kmp_project.core.network.deleteApiOk
import com.example.my_kmp_project.core.network.getApi
import com.example.my_kmp_project.core.network.getListPage
import com.example.my_kmp_project.core.network.pageQuery
import com.example.my_kmp_project.core.network.postApi
import com.example.my_kmp_project.core.network.putApi
import com.example.my_kmp_project.core.network.queryOf
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 收支「统一管理接口」契约（后端 `transaction/router.go:48,58-62`，B 区 #19–#21）。
 *
 * 真相源：Go `my_go_study` `transaction/handler.go`：
 * - `GET /api/v1/transactions/manage` —— `ListPage`（:55），`page`/`size` 1-based + `type`，返回 `list + pagination`；
 * - `POST /api/v1/transactions/manage` —— `Create`（:155），body `{type,category,amount,date,note}`；
 * - `GET /api/v1/transactions/manage/:id` —— `Get`（:110）；
 * - `PUT /api/v1/transactions/manage/:id` —— `Update`（:210），body 稀疏（`*string`/`*float64`）；
 * - `DELETE /api/v1/transactions/manage/:id` —— `Delete`（:273）；
 * - `GET /api/v1/transactions/search` —— `Search`（:295），`q` + `limit`，返回 `items`。
 *
 * 与 [TransactionApi] 分开：后者是 Flutter legacy `/transactions`（`limit`/`offset`，0-based），
 * 本类是 `/transactions/manage` 与 `/transactions/search` 的独立路由（统一响应格式）。
 * 仿 [PaymentPrepayApi] 样板：**只登记后端真实路由契约**（路径常量 + 请求/响应模型 +
 * 可调用方法 + 契约测试），不自造 UI 调用点，也不改后端仓库。
 */
public class TransactionManageApi(
    private val client: ApiClient = NetworkFacade.api(),
) {
    /** `GET /api/v1/transactions/manage?page=&size=&type=`（page 1-based）。 */
    public suspend fun listPage(
        page: Int = 1,
        size: Int = DEFAULT_SIZE,
        type: String? = null,
    ): ApiResponse<ListPage<TransactionManageRow>> =
        client.getListPage<TransactionManageRow>(
            path = TransactionManagePaths.MANAGE,
            query = pageQuery(page, size, "type" to type),
        )

    /** `POST /api/v1/transactions/manage`（`transaction/router.go:59`）。 */
    public suspend fun create(request: TransactionManageCreateRequest): ApiResponse<TransactionManageRow> =
        client.postApi<TransactionManageCreateRequest, TransactionManageRow>(
            path = TransactionManagePaths.MANAGE,
            body = request,
        )

    /** `GET /api/v1/transactions/manage/:id`（`transaction/router.go:60`）。 */
    public suspend fun get(id: Int): ApiResponse<TransactionManageRow> =
        client.getApi<TransactionManageRow>(TransactionManagePaths.item(id))

    /** `PUT /api/v1/transactions/manage/:id`（`transaction/router.go:61`）。 */
    public suspend fun update(id: Int, request: TransactionManageUpdateRequest): ApiResponse<TransactionManageRow> =
        client.putApi<TransactionManageUpdateRequest, TransactionManageRow>(
            path = TransactionManagePaths.item(id),
            body = request,
        )

    /** `DELETE /api/v1/transactions/manage/:id`（`transaction/router.go:62`），走 `deleteApi`。 */
    public suspend fun delete(id: Int): ApiResponse<Unit> =
        client.deleteApiOk(TransactionManagePaths.item(id))

    /**
     * `GET /api/v1/transactions/search?q=&limit=`（`transaction/router.go:48`）。
     *
     * 契约登记说明：后端 `Search` 用 `BackendJSON` 回**裸 body** `{items:[...]}`
     * （无 `{code,data}` 信封）。本方法沿用 core [getListPage]（按信封读 `data`），
     * 真实裸体 `items` 解码需按 [TransactionApi] 的 `homeEnvelopeCarriesCode` 分支处理，
     * 留待实际接线时补齐 —— 当前只登记请求契约（路径/verb/query）。
     */
    public suspend fun search(q: String, limit: Int? = null): ApiResponse<ListPage<TransactionManageRow>> =
        client.getListPage<TransactionManageRow>(
            path = TransactionManagePaths.SEARCH,
            query = queryOf("q" to q, "limit" to limit?.toString()),
        )

    private companion object {
        const val DEFAULT_SIZE: Int = 20
    }
}

/** 收支统一管理接口路径常量（对齐 `transaction/router.go:48,58-62`）。 */
public object TransactionManagePaths {
    /** `GET|POST /api/v1/transactions/manage`。 */
    public const val MANAGE: String = "/api/v1/transactions/manage"

    /** `GET /api/v1/transactions/search`。 */
    public const val SEARCH: String = "/api/v1/transactions/search"

    /** `GET|PUT|DELETE /api/v1/transactions/manage/:id`。 */
    public fun item(id: Int): String = "$MANAGE/$id"
}

/** manage 列表 / 单条 / search 行（`response.FromTransaction`，字段宽松可空）。 */
@Serializable
public data class TransactionManageRow(
    val id: Int? = null,
    val type: String? = null,
    val category: String? = null,
    val amount: Double? = null,
    @SerialName("amount_fen") val amountFen: Long? = null,
    val date: String? = null,
    val note: String? = null,
)

/** `POST /transactions/manage` body（`transaction/request.go:5` CreateTransactionRequest，四键必填）。 */
@Serializable
public data class TransactionManageCreateRequest(
    val type: String,
    val category: String,
    val amount: Double,
    val date: String,
    val note: String? = null,
)

/** `PUT /transactions/manage/:id` body（`transaction/request.go:14` UpdateTransactionRequest，全可省）。 */
@Serializable
public data class TransactionManageUpdateRequest(
    val type: String? = null,
    val category: String? = null,
    val amount: Double? = null,
    val date: String? = null,
    val note: String? = null,
)

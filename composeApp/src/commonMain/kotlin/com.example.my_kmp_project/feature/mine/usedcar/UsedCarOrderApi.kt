package com.example.my_kmp_project.feature.mine.usedcar

import com.example.my_kmp_project.core.network.ApiClient
import com.example.my_kmp_project.core.network.ApiResponse
import com.example.my_kmp_project.core.network.NetworkError
import com.example.my_kmp_project.core.network.NetworkFacade
import com.example.my_kmp_project.core.network.NetworkJsonExplicitNulls
import com.example.my_kmp_project.core.network.decodeEnvelopeData
import com.example.my_kmp_project.core.network.getListPage
import com.example.my_kmp_project.core.network.pageQuery
import com.example.my_kmp_project.core.network.postApiDecoded
import kotlinx.serialization.encodeToString

/**
 * Dart `UsedCarOrderApi._base`（`features/home/lib/home/api/used_car_order_api.dart:6`）
 * 的 verb+path 契约（契约表 §2.7 #35-#39）。
 */
public object UsedCarOrderApiPaths {
    /** `/api/v1/used-car-orders`：列表（#36）与建单（#39）。 */
    public const val BASE: String = "/api/v1/used-car-orders"

    /** #35 */
    public const val SUMMARY: String = "/api/v1/used-car-orders/summary"

    /** #38 */
    public const val CUSTOMERS: String = "/api/v1/used-car-orders/customers"

    /** #37 —— Dart `'$_base/$orderId'`，`orderId` 本身就是字符串化单号。 */
    public fun detail(orderId: String): String = "$BASE/$orderId"
}

/**
 * 二手车业务单 HTTP 契约 —— 逐一对应 Dart `UsedCarOrderApi`
 * （used_car_order_api.dart:5-136，契约表 #35-#39）。
 *
 * 失败语义与 Dart 一致（`_data`，used_car_order_api.dart:127-135 +
 * `backend_response_parser.dart:40-53`）：业务 `code != 0` 抛携带 envelope message
 * 的错误（message 空则为「业务请求失败」）；`code == 0` 但 `data` 为 null 抛本方法的
 * 中文兜底文案（「加载摘要失败」等）。异常类型用
 * [NetworkError.Business]（KMP 侧 `HttpRequestException` 的对等物）。
 *
 * 未登录 / 401 刷新由 [com.example.my_kmp_project.core.network.NetworkFacade] 的
 * 装饰 client 负责，与 Dart `HeaderInterceptor` 等价。
 */
public class UsedCarOrderApi(
    private val api: ApiClient = NetworkFacade.api(),
) {

    /** #35 `GET /used-car-orders/summary`（Dart :8-20）。 */
    public suspend fun fetchSummary(): UsedCarOrderSummary =
        api.getApi(
            path = UsedCarOrderApiPaths.SUMMARY,
            parseData = { el -> decodeEnvelopeData<UsedCarOrderSummary>(el) },
        ).dataOrThrow("加载摘要失败")

    /**
     * #36 `GET /used-car-orders`（Dart :22-50）。
     *
     * 查询：`page`（**1-based**）、`size`（默认 10）、`status`（`all|pending_review|approved|rejected`，
     * `all` 也照发）、`kind`（`trade_in|consign|purchase`，**all 时省略**，Dart :35）。
     * `hasMore`：Dart `page*size < total`（:47）；这里复用共享
     * [com.example.my_kmp_project.core.network.ListPage.hasMore]，不重新推导
     * （BFF 把请求 `page` 原样回显在 `pagination.page`，两者同值；共享实现在
     * `totalPages > 0` 时优先 `page < totalPages`，无 `pagination` 时退化为「本页拿满」）。
     */
    public suspend fun fetchList(
        statusTab: UsedCarStatusTab,
        kindFilter: UsedCarKindFilter,
        page: Int,
        size: Int = DEFAULT_SIZE,
    ): UsedCarOrderListResult {
        val query = pageQuery(
            page = page,
            size = size,
            "status" to statusTab.apiStatus,
            "kind" to kindFilter.apiKind,
        )
        val pageData = api.getListPage<UsedCarOrderItem>(UsedCarOrderApiPaths.BASE, query)
            .dataOrThrow("加载列表失败")
        return UsedCarOrderListResult(
            items = pageData.items,
            hasMore = pageData.hasMore(pageSize = size),
        )
    }

    /** #37 `GET /used-car-orders/{orderId}`（Dart :52-64）。 */
    public suspend fun fetchDetail(orderId: String): UsedCarOrderItem =
        api.getApi(
            path = UsedCarOrderApiPaths.detail(orderId),
            parseData = { el -> decodeEnvelopeData<UsedCarOrderItem>(el) },
        ).dataOrThrow("加载详情失败")

    /**
     * #38 `GET /used-car-orders/customers`（Dart :66-85）。
     * `page=1&size=50` 固定、`q` 先 trim、空则省略（:73）；
     * **只取列表，忽略 pagination**（Dart 直接 `.list`）。
     */
    public suspend fun fetchCustomers(
        page: Int = 1,
        size: Int = CUSTOMERS_SIZE,
        q: String? = null,
    ): List<UsedCarCustomer> {
        val query = pageQuery(page = page, size = size, "q" to q?.trim())
        return api.getListPage<UsedCarCustomer>(UsedCarOrderApiPaths.CUSTOMERS, query)
            .dataOrThrow("加载客户失败")
            .items
    }

    /**
     * #39 `POST /used-car-orders`（Dart :87-120）。
     * body 用 [NetworkJsonExplicitNulls] 编码 ⇒ `image_url` 为 null 时**仍然发送**
     * （Dart :111 无条件写 `'image_url': imageUrl`）。建单即 `pending_review`。
     */
    public suspend fun create(request: UsedCarOrderCreateRequest): UsedCarOrderItem {
        val body = NetworkJsonExplicitNulls.encodeToString(request)
        return api.postApiDecoded<UsedCarOrderItem>(UsedCarOrderApiPaths.BASE, body)
            .dataOrThrow("提交失败")
    }

    private companion object {
        /** Dart `fetchList(..., int size = 10)`（:26）。 */
        const val DEFAULT_SIZE: Int = 10

        /** Dart `fetchCustomers(..., int size = 50)`（:68）。 */
        const val CUSTOMERS_SIZE: Int = 50
    }
}

/**
 * Dart `UsedCarOrderApi._data`（used_car_order_api.dart:127-135）配合
 * `BackendResponseParser._parseEnvelope`（backend_response_parser.dart:40-53）：
 *
 * 1. `code != 0` —— Dart 在解析层就抛，message 取 envelope 的，空则为「业务请求失败」；
 * 2. `code == 0` 但 `data == null` —— 走到 `_data`，**message 仍然优先**
 *    （`model.message.isNotEmpty ? model.message : fallback`），空才用调用点中文兜底。
 *
 * KMP 的 client 不因业务码抛错，所以两条分支都在这里复刻。
 */
private fun <T> ApiResponse<T>.dataOrThrow(fallback: String): T {
    val text = message?.takeIf { it.isNotEmpty() }
    if (!isSuccess) {
        throw NetworkError.Business(code, text ?: "业务请求失败")
    }
    return data ?: throw NetworkError.Business(code, text ?: fallback)
}

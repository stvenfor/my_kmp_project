package com.example.my_kmp_project.feature.mine.dealinvoice

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
 * Dart `DealInvoiceApi._base`
 * （`features/settings/lib/deal_invoice/api/deal_invoice_api.dart:7`）
 * 的 verb+path 契约（契约表 §2.10 #51-#56）。
 */
public object DealInvoiceApiPaths {
    /** `/api/v1/deal-invoices`：列表（#52）与上传（#55）。 */
    public const val BASE: String = "/api/v1/deal-invoices"

    /** #51 */
    public const val SUMMARY: String = "/api/v1/deal-invoices/summary"

    /** #54 */
    public const val CUSTOMERS: String = "/api/v1/deal-invoices/customers"

    /** #53 —— Dart `'$_base/$invoiceId'`。 */
    public fun detail(invoiceId: String): String = "$BASE/$invoiceId"

    /** #56 —— 本组里唯一的 PATCH（契约表 §1「PATCH = 4」之一）。 */
    public fun resubmit(invoiceId: String): String = "$BASE/$invoiceId/resubmit"
}

/**
 * 成交发票 HTTP 契约 —— 逐一对应 Dart `DealInvoiceApi`
 * （deal_invoice_api.dart:6-139，契约表 #51-#56）。
 *
 * 失败语义与 [UsedCarOrderApi] 同源（Dart `_data` + `BackendResponseParser._parseEnvelope`）：
 * `code != 0` 抛带 envelope message 的 [NetworkError.Business]；`code == 0` 而 `data` 为 null
 * 抛调用点中文兜底（「加载摘要失败」/「提交失败」/「重提失败」…）。
 */
public class DealInvoiceApi(
    private val api: ApiClient = NetworkFacade.api(),
) {

    /** #51 `GET /deal-invoices/summary`（Dart :9-21）。 */
    public suspend fun fetchSummary(): DealInvoiceSummary =
        api.getApi(
            path = DealInvoiceApiPaths.SUMMARY,
            parseData = { el -> decodeEnvelopeData<DealInvoiceSummary>(el) },
        ).dataOrThrow("加载摘要失败")

    /**
     * #52 `GET /deal-invoices`（Dart :23-47）。
     * 查询只有 `page`（1-based）/`size`（默认 10）/`status`
     * （`all|pending_review|approved|rejected`，`all` 照发）；
     * 响应行的状态域另有 `approved_pending_rating`、`rated`。
     * `hasMore` 复用共享 [com.example.my_kmp_project.core.network.ListPage.hasMore]
     * （Dart :44 的 `page*size < total`）。
     */
    public suspend fun fetchList(
        tab: DealInvoiceTab,
        page: Int,
        size: Int = DEFAULT_SIZE,
    ): DealInvoiceListResult {
        val query = pageQuery(page = page, size = size, "status" to tab.apiStatus)
        val pageData = api.getListPage<DealInvoiceItem>(DealInvoiceApiPaths.BASE, query)
            .dataOrThrow("加载列表失败")
        return DealInvoiceListResult(
            items = pageData.items,
            hasMore = pageData.hasMore(pageSize = size),
        )
    }

    /** #53 `GET /deal-invoices/{invoiceId}`（Dart :49-61）。 */
    public suspend fun fetchDetail(invoiceId: String): DealInvoiceItem =
        api.getApi(
            path = DealInvoiceApiPaths.detail(invoiceId),
            parseData = { el -> decodeEnvelopeData<DealInvoiceItem>(el) },
        ).dataOrThrow("加载详情失败")

    /**
     * #54 `GET /deal-invoices/customers`（Dart :63-84）。
     * `page=1&size=50` 固定、`q` trim 后为空则省略（:70）；**忽略 pagination，只取列表**。
     */
    public suspend fun fetchCustomers(
        page: Int = 1,
        size: Int = CUSTOMERS_SIZE,
        q: String? = null,
    ): List<DealInvoiceCustomer> {
        val query = pageQuery(page = page, size = size, "q" to q?.trim())
        return api.getListPage<DealInvoiceCustomer>(DealInvoiceApiPaths.CUSTOMERS, query)
            .dataOrThrow("加载客户失败")
            .items
    }

    /**
     * #55 `POST /deal-invoices`（Dart :86-105）。
     * `customer_id` 与 `image_url` **两键恒发** ⇒ 用
     * [NetworkJsonExplicitNulls] 编码（null 也要出现在 body 里）。
     */
    public suspend fun create(customerId: Int, imageUrl: String? = null): DealInvoiceItem {
        val body = NetworkJsonExplicitNulls.encodeToString(
            DealInvoiceCreateRequest(customerId = customerId, imageUrl = imageUrl),
        )
        return api.postApiDecoded<DealInvoiceItem>(DealInvoiceApiPaths.BASE, body)
            .dataOrThrow("提交失败")
    }

    /**
     * #56 `PATCH /deal-invoices/{invoiceId}/resubmit`（Dart :107-123）：驳回后重提，
     * body 同样恒发 `image_url`（含 null）。
     */
    public suspend fun resubmit(invoiceId: String, imageUrl: String? = null): DealInvoiceItem {
        val body = NetworkJsonExplicitNulls.encodeToString(
            DealInvoiceResubmitRequest(imageUrl = imageUrl),
        )
        return api.patchApi(
            path = DealInvoiceApiPaths.resubmit(invoiceId),
            body = body,
            parseData = { el -> decodeEnvelopeData<DealInvoiceItem>(el) },
        ).dataOrThrow("重提失败")
    }

    private companion object {
        /** Dart `fetchList(..., int size = 10)`（:26）。 */
        const val DEFAULT_SIZE: Int = 10

        /** Dart `fetchCustomers(..., int size = 50)`（:66）。 */
        const val CUSTOMERS_SIZE: Int = 50
    }
}

/** Dart `DealInvoiceApi._data`（:130-138）+ `BackendResponseParser`（非 0 码先抛）。 */
private fun <T> ApiResponse<T>.dataOrThrow(fallback: String): T {
    val text = message?.takeIf { it.isNotEmpty() }
    if (!isSuccess) {
        throw NetworkError.Business(code, text ?: "业务请求失败")
    }
    return data ?: throw NetworkError.Business(code, text ?: fallback)
}

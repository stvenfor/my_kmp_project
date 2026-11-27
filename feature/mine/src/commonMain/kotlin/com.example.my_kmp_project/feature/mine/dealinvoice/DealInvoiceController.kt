package com.example.my_kmp_project.feature.mine.dealinvoice

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.my_kmp_project.core.network.ApiClient

/**
 * 成交发票两屏（列表 / 上传）的页面级状态。
 *
 * 全部走 [DealInvoiceApi]：摘要、列表、详情、客户、提交。失败只写 [errorMessage]，
 * 不回落本地样例数据。
 */
internal class DealInvoiceController(api: ApiClient) {
    private val invoiceApi = DealInvoiceApi(api)

    var summary by mutableStateOf<DealInvoiceSummary?>(null)
        private set
    var items by mutableStateOf<List<DealInvoiceItem>>(emptyList())
        private set
    var tab by mutableStateOf(DealInvoiceTab.All)
        private set
    var loading by mutableStateOf(false)
        private set
    var hasMore by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    var detail by mutableStateOf<DealInvoiceItem?>(null)
        private set
    var detailLoading by mutableStateOf(false)
        private set

    var customers by mutableStateOf<List<DealInvoiceCustomer>>(emptyList())
        private set
    var customersLoading by mutableStateOf(false)
        private set
    var submitting by mutableStateOf(false)
        private set

    private var page: Int = 1

    fun selectTab(value: DealInvoiceTab) {
        if (tab == value) return
        tab = value
    }

    suspend fun load() {
        loading = true
        errorMessage = null
        page = 1
        try {
            summary = invoiceApi.fetchSummary()
            val result = invoiceApi.fetchList(tab = tab, page = 1)
            items = result.items
            hasMore = result.hasMore
        } catch (e: Exception) {
            errorMessage = e.message ?: "加载成交发票失败"
        } finally {
            loading = false
        }
    }

    suspend fun loadMore() {
        if (loading || !hasMore) return
        loading = true
        errorMessage = null
        try {
            val next = page + 1
            val result = invoiceApi.fetchList(tab = tab, page = next)
            items = items + result.items
            page = next
            hasMore = result.hasMore
        } catch (e: Exception) {
            errorMessage = e.message ?: "加载更多失败"
        } finally {
            loading = false
        }
    }

    suspend fun openDetail(invoiceId: String) {
        detailLoading = true
        detail = null
        errorMessage = null
        try {
            detail = invoiceApi.fetchDetail(invoiceId)
        } catch (e: Exception) {
            errorMessage = e.message ?: "加载详情失败"
        } finally {
            detailLoading = false
        }
    }

    fun closeDetail() {
        detail = null
    }

    suspend fun loadCustomers() {
        customersLoading = true
        errorMessage = null
        try {
            customers = invoiceApi.fetchCustomers()
        } catch (e: Exception) {
            errorMessage = e.message ?: "加载客户失败"
        } finally {
            customersLoading = false
        }
    }

    /** 图片上传依赖对象存储（平台 gap），此处只提交客户维度。 */
    suspend fun submit(customerId: Int): Result<DealInvoiceItem> {
        submitting = true
        errorMessage = null
        return try {
            Result.success(invoiceApi.create(customerId = customerId, imageUrl = null))
        } catch (e: Exception) {
            Result.failure(IllegalStateException(e.message ?: "提交失败"))
        } finally {
            submitting = false
        }
    }

    fun clearError() {
        errorMessage = null
    }
}

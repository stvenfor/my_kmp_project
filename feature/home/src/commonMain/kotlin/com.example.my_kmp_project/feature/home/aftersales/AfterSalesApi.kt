package com.example.my_kmp_project.feature.home.aftersales

import com.example.my_kmp_project.core.network.ApiClient
import com.example.my_kmp_project.core.network.NetworkFacade
import com.example.my_kmp_project.core.network.NetworkJson
import com.example.my_kmp_project.core.network.getApi
import com.example.my_kmp_project.core.network.pageQuery
import com.example.my_kmp_project.core.network.postApiDecoded
import com.example.my_kmp_project.feature.home.api.homeBareDataOrThrow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Flutter `AfterSalesApi`（`features/home/lib/after_sales/api/after_sales_api.dart`，d7876a1）。
 * 契约表 #47–#50。
 *
 * 失败语义：Dart `_data()`（:120-125）抛**裸** `Exception(message ?? fallback)`，
 * 不是 `HttpRequestException`（契约表 §2.9 特别标注），这里用 [homeBareDataOrThrow] 保留。
 */
public class AfterSalesApi(
    private val client: ApiClient = NetworkFacade.api(),
) {
    public companion object {
        /** Dart `AfterSalesApi._base`（after_sales_api.dart:7）。 */
        internal const val BASE_PATH: String = "/api/v1/after-sales"

        /** Dart `fetchRecords(size = 10)`（:11）。 */
        internal const val DEFAULT_LIST_SIZE: Int = 10
    }

    /**
     * #47 `GET /after-sales/records?page=&size=`（[page] 服务端 1-based，
     * `after_sales_list_viewmodel.dart:28`）。
     *
     * `data` 按**裸 map** 读：`{list:[...], pagination:{total}, can_create:bool}`，
     * 不走 `ListPage`，否则会丢 [AfterSalesListResult.canCreate]（它 gate 「新建工单」入口）。
     */
    public suspend fun fetchRecords(
        page: Int,
        size: Int = DEFAULT_LIST_SIZE,
    ): AfterSalesListResult {
        val data = client.getApi<AfterSalesRecordsWire>(
            "$BASE_PATH/records",
            pageQuery(page, size),
        ).homeBareDataOrThrow("加载列表失败")
        val list = data.list ?: emptyList()
        val total = data.pagination?.totalRaw ?: list.size
        return AfterSalesListResult(
            list = list,
            hasMore = page * size < total,
            canCreate = data.canCreate,
        )
    }

    /** #48 `GET /after-sales/records/:recordId`。 */
    public suspend fun fetchDetail(recordId: Int): AfterSalesRecord =
        client.getApi<AfterSalesRecord>("$BASE_PATH/records/$recordId")
            .homeBareDataOrThrow("加载详情失败")

    /**
     * #49 `POST /after-sales/records`（after_sales_api.dart:65-80）。
     *
     * body 两类键并存，故用 [buildJsonObject] 逐键落笔：
     * - **总发**：`service_kind`（字符串，Dart 侧 `_kind = '1'`）、`title`、`service_date`、
     *   `customer_name`、`customer_phone`（后两键即使 null 也显式发 null）；
     * - **条件发**：`appointment_id`（非 null）、`plate_no`（trim 后非空，发 trim 值）、
     *   `mileage`（非 null）、`content`（trim 后非空，发 trim 值）。
     */
    public suspend fun create(
        serviceKind: String,
        title: String,
        serviceDate: String,
        customerName: String? = null,
        customerPhone: String? = null,
        appointmentId: Int? = null,
        plateNo: String? = null,
        mileage: Int? = null,
        content: String? = null,
    ): AfterSalesRecord {
        val body: String = NetworkJson.encodeToString(
            buildJsonObject {
                put("service_kind", serviceKind)
                put("title", title)
                put("service_date", serviceDate)
                // 两键总发，null 也落键：`put(key, String?)` 写入 JsonNull（Dart 直接把 null 放进 map）
                put("customer_name", customerName)
                put("customer_phone", customerPhone)
                appointmentId?.let { put("appointment_id", it) }
                plateNo?.trim()?.takeIf { it.isNotEmpty() }?.let { put("plate_no", it) }
                mileage?.let { put("mileage", it) }
                content?.trim()?.takeIf { it.isNotEmpty() }?.let { put("content", it) }
            },
        )
        return client.postApiDecoded<AfterSalesRecord>("$BASE_PATH/records", body)
            .homeBareDataOrThrow("新建失败")
    }

    /**
     * #50 `GET /after-sales/pending-appointments` —— `data` 是裸 map `{list:[...]}`，
     * 日期按 `T` 截断（[AfterSalesAppointment.appointmentDate]）。
     */
    public suspend fun fetchPendingAppointments(): List<AfterSalesAppointment> {
        val data = client.getApi<AfterSalesPendingAppointmentsWire>("$BASE_PATH/pending-appointments")
            .homeBareDataOrThrow("加载预约失败")
        return data.list ?: emptyList()
    }
}

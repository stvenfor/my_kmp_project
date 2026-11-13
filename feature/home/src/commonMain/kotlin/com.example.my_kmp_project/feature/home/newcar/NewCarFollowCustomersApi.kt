package com.example.my_kmp_project.feature.home.newcar

import com.example.my_kmp_project.core.network.ApiClient
import com.example.my_kmp_project.core.network.ApiResponse
import com.example.my_kmp_project.core.network.ListPage
import com.example.my_kmp_project.core.network.NetworkFacade
import com.example.my_kmp_project.core.network.getListPage
import com.example.my_kmp_project.core.network.pageQuery
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `GET /api/v1/new-car-follow-files/customers` 契约（后端 `new_car_follow/router.go:40`，B 区 #8）。
 *
 * 真相源：Go `my_go_study` `new_car_follow/handler.go:214-232` ListCustomers ——
 * `page`/`size`（1-based）+ 可选 `q`（TrimSpace）查询，返回 `response.SuccessList`
 * 分页体（`{list, pagination}`）；行形态见 `model/entity/home_todo.go:43` WysStoreCustomer。
 *
 * 仿 [PaymentPrepayApi] 样板：**只登记后端真实路由契约**（路径常量 + 行模型 +
 * 可调用方法 + 契约测试），不自造 UI 调用点，也不改后端仓库。
 * 与 [NewCarFollowApi] 分开：后者是已接线的 #40–#46，本端点是 `missing(待接线)`。
 */
public class NewCarFollowCustomersApi(
    private val client: ApiClient = NetworkFacade.api(),
) {
    /** `GET /api/v1/new-car-follow-files/customers?page=&size=&q=`（page 1-based）。 */
    public suspend fun listCustomers(
        page: Int,
        size: Int = DEFAULT_SIZE,
        q: String? = null,
    ): ApiResponse<ListPage<NewCarFollowCustomer>> =
        client.getListPage<NewCarFollowCustomer>(
            path = NewCarFollowCustomersPaths.CUSTOMERS,
            query = pageQuery(page, size, "q" to q),
        )

    private companion object {
        const val DEFAULT_SIZE: Int = 20
    }
}

/** 新潜客客户列表路径常量（对齐 `new_car_follow/router.go:40`）。 */
public object NewCarFollowCustomersPaths {
    /** `GET /api/v1/new-car-follow-files/customers`。 */
    public const val CUSTOMERS: String = "/api/v1/new-car-follow-files/customers"
}

/** `GET /new-car-follow-files/customers` 行（`model/entity/home_todo.go:43` WysStoreCustomer）。 */
@Serializable
public data class NewCarFollowCustomer(
    @SerialName("customer_id") val customerId: Long = 0,
    @SerialName("store_id") val storeId: Int = 0,
    @SerialName("display_name") val displayName: String = "",
    val phone: String = "",
    @SerialName("next_follow_up_at") val nextFollowUpAt: String? = null,
)

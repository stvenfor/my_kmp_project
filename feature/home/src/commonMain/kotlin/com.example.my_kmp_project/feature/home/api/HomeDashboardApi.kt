package com.example.my_kmp_project.feature.home.api

import com.example.my_kmp_project.core.network.ApiClient
import com.example.my_kmp_project.core.network.getApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 首页聚合看板 `GET /api/v1/home/dashboard`（需 Bearer）。
 *
 * 回到后端：宫格是服务端常量；contacts / news 查真实表，查不到给空数组。
 * 信封统一 `{code,message,data}`，data 为本文件的 [HomeDashboardPayload]。
 */
public class HomeDashboardApi(
    private val client: ApiClient,
) {
    public companion object {
        internal const val DASHBOARD_PATH: String = "/api/v1/home/dashboard"
    }

    /** 拉取首页看板；非 `code:0` 或 `data` 缺失抛 [com.example.my_kmp_project.core.network.NetworkError.Business]。 */
    public suspend fun fetchDashboard(): HomeDashboardPayload =
        client.getApi<HomeDashboardPayload>(DASHBOARD_PATH)
            .homeDataOrThrow("加载首页数据失败")
}

/** 首页看板 data。字段全部给默认值，宽容后端缺省。 */
@Serializable
public data class HomeDashboardPayload(
    @SerialName("store_name") val storeName: String = "",
    @SerialName("stats") val stats: HomeDashboardStats = HomeDashboardStats(),
    @SerialName("metrics") val metrics: HomeDashboardMetrics = HomeDashboardMetrics(),
    @SerialName("features") val features: List<HomeDashboardItem> = emptyList(),
    @SerialName("services") val services: List<HomeDashboardItem> = emptyList(),
    @SerialName("contacts") val contacts: List<HomeDashboardContact> = emptyList(),
    @SerialName("news") val news: List<HomeDashboardNews> = emptyList(),
)

/** 公司数据三档（今日 / 昨日 / 本月）；后端无行时各档四项为 0。 */
@Serializable
public data class HomeDashboardMetrics(
    @SerialName("today") val today: HomeDashboardMetric = HomeDashboardMetric(),
    @SerialName("yesterday") val yesterday: HomeDashboardMetric = HomeDashboardMetric(),
    @SerialName("month") val month: HomeDashboardMetric = HomeDashboardMetric(),
)

/** 单档指标；deal_amount 单位为元。 */
@Serializable
public data class HomeDashboardMetric(
    @SerialName("customers") val customers: Int = 0,
    @SerialName("new_orders") val newOrders: Int = 0,
    @SerialName("deal_amount") val dealAmount: Double = 0.0,
    @SerialName("test_drives") val testDrives: Int = 0,
)

/** 门店统计数字；无统计行时全 0。 */
@Serializable
public data class HomeDashboardStats(
    @SerialName("days_joined") val daysJoined: Int = 0,
    @SerialName("employee_count") val employeeCount: Int = 0,
    @SerialName("store_days") val storeDays: Int = 0,
    @SerialName("total_customers") val totalCustomers: Int = 0,
)

/** 宫格项：icon 为客户端图标 key。 */
@Serializable
public data class HomeDashboardItem(
    @SerialName("key") val key: String = "",
    @SerialName("label") val label: String = "",
    @SerialName("icon") val icon: String = "",
    @SerialName("badge") val badge: String? = null,
)

/** 门店客户行。 */
@Serializable
public data class HomeDashboardContact(
    @SerialName("id") val id: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("subtitle") val subtitle: String = "",
)

/** 资讯行。 */
@Serializable
public data class HomeDashboardNews(
    @SerialName("id") val id: String = "",
    @SerialName("title") val title: String = "",
    @SerialName("summary") val summary: String = "",
    @SerialName("published_at") val publishedAt: String = "",
)

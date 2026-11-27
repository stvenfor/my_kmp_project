package com.example.my_kmp_project.feature.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.my_kmp_project.feature.home.api.HomeDashboardApi
import com.example.my_kmp_project.feature.home.api.HomeDashboardMetric
import com.example.my_kmp_project.feature.home.api.HomeDashboardPayload
import kotlin.math.round

/**
 * 首页看板（功能宫格 / 服务宫格 / 联系人 / 资讯 / 门店名 / 公司数据）远端缓存。
 *
 * 数据来自 `GET /api/v1/home/dashboard`：宫格文案由服务端下发，contacts/news 查真实表。
 * 刷新失败只写 [errorMessage] 并保留上次成功缓存（首次即空列表），绝不回落客户端 mock。
 *
 * DI 第四批：从 `object` 改为实例，由 app 装配层（`AppContainer.home`）持有 AppScope 单例，
 * [HomeDashboardApi] 通过构造参数注入。
 */
class HomeDashboardStore(
    private val api: HomeDashboardApi,
) {
    var version by mutableIntStateOf(0)
        private set

    private fun bump() {
        version++
    }

    var loading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    private var payload by mutableStateOf<HomeDashboardPayload?>(null)

    /** 门店名（users.current_store_id → wys_store.name；取不到为空串）。 */
    val storeName: String get() = payload?.storeName.orEmpty()

    /** 功能九宫格（服务端常量，icon 为图标 key）。 */
    fun features(): List<HomeFeatureItem> =
        payload?.features.orEmpty().map { HomeFeatureItem(label = it.label, icon = it.icon) }

    /** 服务宫格（服务端常量，保留 badge）。 */
    fun services(): List<HomeServiceItem> =
        payload?.services.orEmpty().map {
            HomeServiceItem(label = it.label, badge = it.badge?.takeIf { b -> b.isNotBlank() }, icon = it.icon)
        }

    /** 门店客户（wys_store_customer 最近 5 条）。 */
    fun contacts(): List<HomeContactItem> =
        payload?.contacts.orEmpty().map { c ->
            HomeContactItem(
                title = c.name,
                subtitle = c.subtitle,
                trailing = if (c.subtitle.isNotBlank()) "phone" else null,
            )
        }

    /** 行业资讯（wys_articles 已发布最近 3 条；表不存在时为空）。 */
    fun news(): List<HomeNewsItem> =
        payload?.news.orEmpty().map { n ->
            HomeNewsItem(
                title = n.title,
                source = "",
                date = n.publishedAt.take(10),
            )
        }

    /**
     * 公司数据 4 指标（真实表 wys_home_metrics）。
     * tab：0 今日 / 1 昨日 / 2 本月；后端无行时全 0，不编造。
     */
    fun metrics(tab: Int): List<HomeMetric> {
        val metrics = payload?.metrics
        val m: HomeDashboardMetric = when (tab) {
            1 -> metrics?.yesterday
            2 -> metrics?.month
            else -> metrics?.today
        } ?: HomeDashboardMetric()
        return listOf(
            HomeMetric(m.customers.toString(), "意向客户"),
            HomeMetric(m.newOrders.toString(), "新车订单"),
            HomeMetric(formatWan(m.dealAmount), "成交额"),
            HomeMetric(m.testDrives.toString(), "试驾预约"),
        )
    }

    /** 成交额按万元展示，保留一位小数（commonMain 无 String.format）。 */
    private fun formatWan(amount: Double): String {
        val wan = round(amount / 10_000.0 * 10.0) / 10.0
        return if (wan == round(wan)) "${wan.toInt()}万" else "${wan}万"
    }

    /** 拉取首页看板；失败只记录 [errorMessage]，保留已有缓存。 */
    suspend fun refresh(): Result<Unit> {
        loading = true
        return try {
            payload = api.fetchDashboard()
            errorMessage = null
            Result.success(Unit)
        } catch (e: Exception) {
            errorMessage = e.message ?: "加载首页数据失败"
            Result.failure(e)
        } finally {
            loading = false
            bump()
        }
    }

    /** Test helper — 只清空内存缓存。 */
    fun resetForTests() {
        payload = null
        loading = false
        errorMessage = null
        bump()
    }
}

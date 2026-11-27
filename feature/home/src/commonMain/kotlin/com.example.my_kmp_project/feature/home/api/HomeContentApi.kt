package com.example.my_kmp_project.feature.home.api

import com.example.my_kmp_project.core.network.ApiClient
import com.example.my_kmp_project.core.network.deleteApi
import com.example.my_kmp_project.core.network.getApi
import com.example.my_kmp_project.core.network.postApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 首页本地静态内容的后端接口（全部需 Bearer + SessionAuth）。
 *
 * 覆盖：全部服务 / 收藏（catalog+favorites）、搜索页与热榜（search/meta、hot-rank）、
 * 学习报告、投资策略。信封统一 `{code,message,data}`，data 见各 Payload。
 */
public class HomeContentApi(
    private val client: ApiClient,
) {
    public companion object {
        internal const val CATALOG_PATH: String = "/api/v1/home/catalog"
        internal const val FAVORITES_PATH: String = "/api/v1/home/favorites"
        internal const val SEARCH_META_PATH: String = "/api/v1/home/search/meta"
        internal const val HOT_RANK_PATH: String = "/api/v1/home/hot-rank"
        internal const val LEARNING_REPORT_PATH: String = "/api/v1/home/learning-report"
        internal const val STRATEGY_PATH: String = "/api/v1/home/strategy"
        internal const val LEDGER_PATH: String = "/api/v1/home/ledger"
    }

    /** 全部服务分组 + 当前用户收藏 id。 */
    public suspend fun fetchCatalog(): HomeCatalogPayload =
        client.getApi<HomeCatalogPayload>(CATALOG_PATH)
            .homeDataOrThrow("加载全部服务失败")

    /** 新增收藏，返回最新 favorite_ids。 */
    public suspend fun addFavorite(serviceId: String): HomeFavoritePayload =
        client.postApi<HomeFavoriteRequest, HomeFavoritePayload>(
            FAVORITES_PATH,
            HomeFavoriteRequest(serviceId),
        ).homeDataOrThrow("收藏失败")

    /** 取消收藏，返回最新 favorite_ids。 */
    public suspend fun removeFavorite(serviceId: String): HomeFavoritePayload =
        client.deleteApi<HomeFavoritePayload>("$FAVORITES_PATH/$serviceId")
            .homeDataOrThrow("取消收藏失败")

    /** 搜索页：历史 / 发现 / 快捷筛选 / 热榜 tab 与默认榜单。 */
    public suspend fun fetchSearchMeta(): HomeSearchMetaPayload =
        client.getApi<HomeSearchMetaPayload>(SEARCH_META_PATH)
            .homeDataOrThrow("加载搜索页失败")

    /** 热榜详情；tab 为空后端默认 hot，category 为空默认首个分类。 */
    public suspend fun fetchHotRank(tab: String? = null, category: String? = null): HomeHotRankPayload {
        val query = buildMap {
            if (!tab.isNullOrBlank()) put("tab", tab)
            if (!category.isNullOrBlank()) put("category", category)
        }
        return client.getApi<HomeHotRankPayload>(HOT_RANK_PATH, query)
            .homeDataOrThrow("加载热榜失败")
    }

    /** 学习报告：今日高光 + 今日学习记录。 */
    public suspend fun fetchLearningReport(): HomeLearningReportPayload =
        client.getApi<HomeLearningReportPayload>(LEARNING_REPORT_PATH)
            .homeDataOrThrow("加载学习报告失败")

    /** 投资策略：子 tab / 周期 / 大类资产。 */
    public suspend fun fetchStrategy(): HomeStrategyPayload =
        client.getApi<HomeStrategyPayload>(STRATEGY_PATH)
            .homeDataOrThrow("加载投资策略失败")

    /** 数据台账：最近收支记录（limit 后端默认 20、上限 50）。 */
    public suspend fun ledger(limit: Int = 20): HomeLedgerPayload =
        client.getApi<HomeLedgerPayload>(LEDGER_PATH, mapOf("limit" to limit.toString()))
            .homeDataOrThrow("加载数据台账失败")
}

/** 全部服务 data。 */
@Serializable
public data class HomeCatalogPayload(
    @SerialName("sections") val sections: List<HomeCatalogSectionDto> = emptyList(),
    @SerialName("favorite_ids") val favoriteIds: List<String> = emptyList(),
)

/** 服务分组；title 为服务端分组标题。 */
@Serializable
public data class HomeCatalogSectionDto(
    @SerialName("title") val title: String = "",
    @SerialName("items") val items: List<HomeCatalogItemDto> = emptyList(),
)

/** 服务项；icon 为客户端图标 key（历史资产文件名）。 */
@Serializable
public data class HomeCatalogItemDto(
    @SerialName("id") val id: String = "",
    @SerialName("label") val label: String = "",
    @SerialName("icon") val icon: String = "",
    @SerialName("badge") val badge: String? = null,
)

/** 收藏增删 data。 */
@Serializable
public data class HomeFavoritePayload(
    @SerialName("favorite_ids") val favoriteIds: List<String> = emptyList(),
)

/** 收藏请求体。 */
@Serializable
public data class HomeFavoriteRequest(
    @SerialName("service_id") val serviceId: String = "",
)

/** 搜索页 data。 */
@Serializable
public data class HomeSearchMetaPayload(
    @SerialName("history") val history: List<String> = emptyList(),
    @SerialName("discovery") val discovery: List<String> = emptyList(),
    @SerialName("filter_tags") val filterTags: List<String> = emptyList(),
    @SerialName("rank_tabs") val rankTabs: List<String> = emptyList(),
    @SerialName("rank_items") val rankItems: Map<String, List<HomeRankItemDto>> = emptyMap(),
)

/** 榜单行；meta 承载热度等展示文案。 */
@Serializable
public data class HomeRankItemDto(
    @SerialName("id") val id: Long = 0,
    @SerialName("title") val title: String = "",
    @SerialName("subtitle") val subtitle: String = "",
    @SerialName("meta") val meta: String = "",
    @SerialName("badge") val badge: String? = null,
)

/** 热榜详情 data。 */
@Serializable
public data class HomeHotRankPayload(
    @SerialName("categories") val categories: List<String> = emptyList(),
    @SerialName("age_filters") val ageFilters: List<String> = emptyList(),
    @SerialName("items") val items: List<HomeRankItemDto> = emptyList(),
)

/** 学习报告 data。 */
@Serializable
public data class HomeLearningReportPayload(
    @SerialName("highlights") val highlights: List<HomeReportItemDto> = emptyList(),
    @SerialName("records") val records: List<HomeReportItemDto> = emptyList(),
)

/** 报告行（高光 / 记录共用）。 */
@Serializable
public data class HomeReportItemDto(
    @SerialName("title") val title: String = "",
    @SerialName("value") val value: String = "",
    @SerialName("description") val description: String = "",
)

/** 投资策略 data。 */
@Serializable
public data class HomeStrategyPayload(
    @SerialName("tabs") val tabs: List<String> = emptyList(),
    @SerialName("periods") val periods: List<String> = emptyList(),
    @SerialName("assets") val assets: List<HomeStrategyAssetDto> = emptyList(),
)

/** 大类资产；weight 为区间收益文案。 */
@Serializable
public data class HomeStrategyAssetDto(
    @SerialName("name") val name: String = "",
    @SerialName("code") val code: String = "",
    @SerialName("weight") val weight: String = "",
)

/** 数据台账 data。 */
@Serializable
public data class HomeLedgerPayload(
    @SerialName("items") val items: List<LedgerEntry> = emptyList(),
)

/** 台账行；amount 单位为元，type 为「收入」/「支出」。 */
@Serializable
public data class LedgerEntry(
    @SerialName("id") val id: Long = 0,
    @SerialName("type") val type: String = "",
    @SerialName("category") val category: String = "",
    @SerialName("amount") val amount: Double = 0.0,
    @SerialName("date") val date: String = "",
    @SerialName("note") val note: String? = null,
)

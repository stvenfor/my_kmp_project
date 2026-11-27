package com.example.my_kmp_project.feature.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.my_kmp_project.feature.home.api.HomeCatalogPayload
import com.example.my_kmp_project.feature.home.api.HomeContentApi
import com.example.my_kmp_project.feature.home.api.HomeHotRankPayload
import com.example.my_kmp_project.feature.home.api.HomeLearningReportPayload
import com.example.my_kmp_project.feature.home.api.HomeSearchMetaPayload
import com.example.my_kmp_project.feature.home.api.HomeStrategyPayload
import com.example.my_kmp_project.feature.home.api.LedgerEntry

/**
 * 首页本地静态内容的远端缓存：全部服务 / 收藏 / 搜索页 / 热榜 / 学习报告 / 投资策略。
 *
 * 每次 refresh 拉取对应接口；失败只写 [errorMessage]，保留上次成功缓存（首次即空），
 * 绝不回落客户端 mock 常量。
 *
 * DI：由 app 装配层 `AppContainer.home` 持有 AppScope 单例，[HomeContentApi] 构造注入。
 */
class HomeContentStore(
    private val api: HomeContentApi,
) {
    var version by mutableIntStateOf(0)
        private set

    var loading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    private var catalog by mutableStateOf<HomeCatalogPayload?>(null)
    private var searchMeta by mutableStateOf<HomeSearchMetaPayload?>(null)
    private var hotRank by mutableStateOf<HomeHotRankPayload?>(null)
    private var report by mutableStateOf<HomeLearningReportPayload?>(null)
    private var strategy by mutableStateOf<HomeStrategyPayload?>(null)
    private var favoriteIds by mutableStateOf<List<String>>(emptyList())
    private var ledger by mutableStateOf<List<LedgerEntry>>(emptyList())

    private fun bump() {
        version++
    }

    // ---- 全部服务 / 收藏 ----

    /** 全部服务分组（服务端顺序）。 */
    fun catalogSections(): List<AllServiceSection> =
        catalog?.sections.orEmpty().map { section ->
            AllServiceSection(
                title = section.title,
                items = section.items.map { AllServiceItem(id = it.id, label = it.label, assetName = it.icon) },
            )
        }

    /** 所有服务项（分组 + 服务推荐池），用于把收藏 id 还原成展示项。 */
    private fun allItemsById(): Map<String, AllServiceItem> {
        val items = catalogSections().flatMap { it.items }
        return items.associateBy { it.id }
    }

    /** 当前收藏 id 集合。 */
    fun favoriteIds(): Set<String> = favoriteIds.toSet()

    /** 常用服务展示项（按收藏加入顺序）。 */
    fun favoriteItems(): List<AllServiceItem> {
        val byId = allItemsById()
        return favoriteIds.mapNotNull { byId[it] }
    }

    /** 是否可新增收藏（上限由 [HomeFavoritesStore] 约束）。 */
    fun canAddFavorite(): Boolean = favoriteIds.size < HomeFavoritesStore.MaxFavorites

    /** 是否可移除收藏（下限由 [HomeFavoritesStore] 约束）。 */
    fun canRemoveFavorite(): Boolean = favoriteIds.size > HomeFavoritesStore.MinFavorites

    /** 拉取全部服务 + 收藏 ids。 */
    suspend fun refreshCatalog(): Result<Unit> = runRefresh {
        val payload = api.fetchCatalog()
        catalog = payload
        favoriteIds = payload.favoriteIds
    }

    /** 收藏增删；以服务端返回的 favorite_ids 为准。 */
    suspend fun setFavorite(serviceId: String, add: Boolean): Boolean {
        return try {
            val payload = if (add) api.addFavorite(serviceId) else api.removeFavorite(serviceId)
            favoriteIds = payload.favoriteIds
            errorMessage = null
            bump()
            true
        } catch (e: Exception) {
            errorMessage = e.message ?: "收藏操作失败"
            bump()
            false
        }
    }

    // ---- 搜索页 ----

    fun searchHistory(): List<String> = searchMeta?.history.orEmpty()

    fun searchDiscovery(): List<String> = searchMeta?.discovery.orEmpty()

    fun searchFilterTags(): List<String> = searchMeta?.filterTags.orEmpty()

    fun rankTabs(): List<String> = searchMeta?.rankTabs.orEmpty()

    /** 指定 tab 的排榜行（rank 按接口顺序 1-based）。 */
    fun rankItems(tab: String): List<SearchRankItem> =
        searchMeta?.rankItems?.get(tab).orEmpty().mapIndexed { index, item ->
            SearchRankItem(
                id = item.id.toString(),
                rank = index + 1,
                title = item.title,
                subtitle = item.subtitle,
            )
        }

    /** 按 tab 下标取榜单；越界返回空。 */
    fun rankItemsForTabIndex(tabIndex: Int): List<SearchRankItem> {
        val tab = rankTabs().getOrNull(tabIndex) ?: return emptyList()
        return rankItems(tab)
    }

    suspend fun refreshSearchMeta(): Result<Unit> = runRefresh {
        searchMeta = api.fetchSearchMeta()
    }

    // ---- 热榜详情 ----

    fun hotRankCategories(): List<String> = hotRank?.categories.orEmpty()

    fun hotRankAgeFilters(): List<String> = hotRank?.ageFilters.orEmpty()

    fun hotRankItems(): List<HotRankDetailItem> =
        hotRank?.items.orEmpty().mapIndexed { index, item ->
            HotRankDetailItem(
                id = item.id.toString(),
                rank = index + 1,
                title = item.title,
                subtitle = item.subtitle,
                heat = item.meta.toIntOrNull() ?: 0,
            )
        }

    suspend fun refreshHotRank(tab: String? = null, category: String? = null): Result<Unit> = runRefresh {
        hotRank = api.fetchHotRank(tab = tab, category = category)
    }

    // ---- 学习报告 ----

    fun reportHighlights(): List<ReportHighlight> =
        report?.highlights.orEmpty().map { item ->
            ReportHighlight(
                emoji = "",
                title = item.title,
                subtitle = item.description,
                trailing = item.value,
            )
        }

    fun reportRecords(): List<ReportRecord> =
        report?.records.orEmpty().map { item ->
            ReportRecord(
                emoji = "",
                title = item.title,
                subtitle = item.description,
                time = item.value,
                status = "",
                statusHighlight = false,
            )
        }

    suspend fun refreshReport(): Result<Unit> = runRefresh {
        report = api.fetchLearningReport()
    }

    // ---- 投资策略 ----

    fun strategyTabs(): List<String> = strategy?.tabs.orEmpty()

    fun strategyPeriods(): List<String> = strategy?.periods.orEmpty()

    fun strategyAssets(): List<StrategyAssetCell> =
        strategy?.assets.orEmpty().map { asset ->
            val weight = asset.weight
            StrategyAssetCell(
                label = asset.name,
                value = weight,
                positive = !weight.trimStart().startsWith("-"),
            )
        }

    suspend fun refreshStrategy(): Result<Unit> = runRefresh {
        strategy = api.fetchStrategy()
    }

    // ---- 数据台账 ----

    /** 台账行（amount 保留两位小数展示，± 符号由 type 决定）。 */
    fun ledgerEntries(): List<LedgerTransaction> = ledger.map { entry ->
        val negative = entry.type == "支出" || entry.type.equals("expense", ignoreCase = true)
        LedgerTransaction(
            id = entry.id.toInt(),
            type = entry.type,
            category = entry.category,
            amount = if (negative) -entry.amount else entry.amount,
            date = entry.date,
            note = entry.note,
        )
    }

    suspend fun refreshLedger(): Result<Unit> = runRefresh {
        ledger = api.ledger().items
    }

    private suspend fun runRefresh(block: suspend () -> Unit): Result<Unit> {
        loading = true
        return try {
            block()
            errorMessage = null
            Result.success(Unit)
        } catch (e: Exception) {
            errorMessage = e.message ?: "加载失败"
            Result.failure(e)
        } finally {
            loading = false
            bump()
        }
    }

    /** Test helper — 只清空内存缓存。 */
    fun resetForTests() {
        catalog = null
        searchMeta = null
        hotRank = null
        report = null
        strategy = null
        favoriteIds = emptyList()
        ledger = emptyList()
        loading = false
        errorMessage = null
        bump()
    }
}

package com.example.my_kmp_project.feature.home

/** 首页展示模型（数据均来自服务端接口，本文件只剩类型定义）。 */

data class HomeFeatureItem(val label: String, val icon: String = "")
data class HomeMetric(val value: String, val label: String)
data class HomeMetricDetail(val value: String, val label: String)
data class HomeServiceItem(val label: String, val badge: String? = null, val icon: String = "")
data class HomeContactItem(
    val title: String,
    val subtitle: String,
    val trailing: String? = null,
)
data class HomeNewsItem(
    val title: String,
    val source: String,
    val date: String,
)

data class AllServiceItem(
    val id: String,
    val label: String,
    /** 服务端下发的图标 key（历史资产文件名），如 calculator.png */
    val assetName: String = "all_functions.png",
)
data class AllServiceSection(
    val title: String,
    val subtitle: String? = null,
    val showEditButton: Boolean = false,
    val items: List<AllServiceItem>,
)

data class SearchRankItem(
    val id: String,
    val rank: Int,
    val title: String,
    val subtitle: String,
)

/** 热榜详情列表行。 */
data class HotRankDetailItem(
    val id: String,
    val rank: Int,
    val title: String,
    val subtitle: String,
    val heat: Int,
)

data class ReportHighlight(
    val emoji: String,
    val title: String,
    val subtitle: String,
    val trailing: String,
)

data class ReportRecord(
    val emoji: String,
    val title: String,
    val subtitle: String,
    val time: String,
    val status: String,
    val statusHighlight: Boolean = false,
)

data class StrategyAssetCell(
    val label: String,
    val value: String,
    val positive: Boolean,
)

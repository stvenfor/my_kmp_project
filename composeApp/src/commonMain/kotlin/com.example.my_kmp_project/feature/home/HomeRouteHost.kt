package com.example.my_kmp_project.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.MineTopBar
import com.example.my_kmp_project.core.platform.showPlatformToast
import com.example.my_kmp_project.core.ui.ReportMainTabRoot

/** Flutter `RoutePath` home secondaries — string keys used by Native shell overlay. */
internal object HomeRoutes {
    const val Search = "/home/search"
    const val Strategy = "/home/strategy"
    const val LearningReport = "/home/learning_report"
    const val CheckInMall = "/home/check_in_mall"
    const val DubbingFeed = "/home/dubbing_feed"
    const val HotRankDetail = "/home/hot_rank_detail"
    const val LifeService = "/home/life_service"
    const val LiveCommerce = "/home/live_commerce"
    const val Club = "/home/club"
    const val UsedCar = "/home/used_car"
    const val UsedCarDetail = "/home/used_car/detail"
    const val UsedCarCreate = "/home/used_car/create"
    const val Ledger = "/home/ledger"
    const val LedgerDetail = "/home/ledger/detail"
    const val DataAnalytics = "/home/data_analytics"
    const val DataAnalyticsDetail = "/home/data_analytics/detail"
    const val TodoPartner = "/home/todo/partner-pending"
    const val TodoFollowUp = "/home/todo/follow-up-customers"
    const val TodoAfterSales = "/home/todo/after-sales-appointments"
    const val TodoOrderReview = "/home/todo/order-pending-review"
    const val AfterSales = "/home/after_sales"
    const val AfterSalesCreate = "/home/after_sales/create"
    const val AfterSalesDetail = "/home/after_sales/detail"
    const val NewCarFollow = "/home/new_car_follow"
    const val NewCarFollowCreate = "/home/new_car_follow/create"
    const val NewCarFollowDetail = "/home/new_car_follow/detail"

    /** Map Home feature / todo labels → route. */
    fun fromLabel(label: String): String? = when (label.trim()) {
        "搜索" -> Search
        "投资策略", "策略" -> Strategy
        "学习报告" -> LearningReport
        "签到商城", "积分商城" -> CheckInMall
        "配音", "配音首页" -> DubbingFeed
        "热榜", "热配榜" -> HotRankDetail
        "生活服务" -> LifeService
        "直播带货" -> LiveCommerce
        "Club" -> Club
        "二手车" -> UsedCar
        "台账", "公司数据" -> Ledger
        "数据分析" -> DataAnalytics
        "新伙伴待确认" -> TodoPartner
        "待跟进客户" -> TodoFollowUp
        "售后预约" -> TodoAfterSales
        "订单待审核" -> TodoOrderReview
        "售后" -> AfterSales
        "新车跟进", "新车成交" -> NewCarFollow
        else -> null
    }
}

@Composable
internal fun HomeRouteHost(
    route: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit = {},
) {
    when (route) {
        HomeRoutes.Search -> HomeSearchScreen(onBack = onBack)
        HomeRoutes.Strategy -> StrategyScreen(onBack = onBack)
        HomeRoutes.LearningReport -> LearningReportScreen(onBack = onBack)
        HomeRoutes.CheckInMall -> CheckInMallScreen(onBack = onBack)
        HomeRoutes.DubbingFeed -> DubbingFeedScreen(
            onBack = onBack,
            onOpenHotRank = { onNavigate(HomeRoutes.HotRankDetail) },
        )
        HomeRoutes.HotRankDetail -> HotRankDetailScreen(onBack = onBack)
        HomeRoutes.LifeService -> FeatureContentScreen("生活服务", onBack)
        HomeRoutes.LiveCommerce -> FeatureContentScreen("直播带货", onBack)
        HomeRoutes.Club -> FeatureContentScreen("Club", onBack)
        HomeRoutes.UsedCar -> CrudListScreen(
            title = "二手车",
            items = HomeSecondaryMock.usedCars,
            onBack = onBack,
            onItem = { onNavigate(HomeRoutes.UsedCarDetail) },
            onCreate = { onNavigate(HomeRoutes.UsedCarCreate) },
        )
        HomeRoutes.UsedCarDetail -> CrudDetailScreen("二手车详情", HomeSecondaryMock.usedCars.first(), onBack)
        HomeRoutes.UsedCarCreate -> CrudCreateScreen("发布二手车", onBack) {
            showPlatformToast("已提交（mock）")
            onBack()
        }
        HomeRoutes.Ledger -> CrudListScreen(
            title = "收支",
            items = HomeSecondaryMock.ledger,
            onBack = onBack,
            onItem = { onNavigate(HomeRoutes.LedgerDetail) },
        )
        HomeRoutes.LedgerDetail -> CrudDetailScreen("收支详情", HomeSecondaryMock.ledger.first(), onBack)
        HomeRoutes.DataAnalytics -> CrudListScreen(
            title = "数据分析",
            items = HomeSecondaryMock.analytics,
            onBack = onBack,
            onItem = { onNavigate(HomeRoutes.DataAnalyticsDetail) },
        )
        HomeRoutes.DataAnalyticsDetail ->
            CrudDetailScreen("分析详情", HomeSecondaryMock.analytics.first(), onBack)
        HomeRoutes.TodoPartner -> CrudListScreen("新伙伴待确认", HomeSecondaryMock.partners, onBack)
        HomeRoutes.TodoFollowUp -> CrudListScreen("待跟进客户", HomeSecondaryMock.followUps, onBack)
        HomeRoutes.TodoAfterSales -> CrudListScreen("售后预约", HomeSecondaryMock.appointments, onBack)
        HomeRoutes.TodoOrderReview -> CrudListScreen("订单待审核", HomeSecondaryMock.orders, onBack)
        HomeRoutes.AfterSales -> CrudListScreen(
            title = "售后专区",
            items = HomeSecondaryMock.afterSales,
            onBack = onBack,
            onItem = { onNavigate(HomeRoutes.AfterSalesDetail) },
            onCreate = { onNavigate(HomeRoutes.AfterSalesCreate) },
        )
        HomeRoutes.AfterSalesCreate -> CrudCreateScreen("创建售后", onBack) {
            showPlatformToast("已创建（mock）")
            onBack()
        }
        HomeRoutes.AfterSalesDetail ->
            CrudDetailScreen("售后详情", HomeSecondaryMock.afterSales.first(), onBack)
        HomeRoutes.NewCarFollow -> CrudListScreen(
            title = "新车跟进",
            items = HomeSecondaryMock.newCars,
            onBack = onBack,
            onItem = { onNavigate(HomeRoutes.NewCarFollowDetail) },
            onCreate = { onNavigate(HomeRoutes.NewCarFollowCreate) },
        )
        HomeRoutes.NewCarFollowCreate -> CrudCreateScreen("新建跟进", onBack) {
            showPlatformToast("已保存（mock）")
            onBack()
        }
        HomeRoutes.NewCarFollowDetail ->
            CrudDetailScreen("跟进详情", HomeSecondaryMock.newCars.first(), onBack)
        else -> CrudDetailScreen("未识别路由", "route=$route", onBack)
    }
}

internal data class HomeListRow(val title: String, val subtitle: String)

internal object HomeSecondaryMock {
    val usedCars = listOf(
        HomeListRow("2021 帝豪", "8.6万 · 3.2万公里 · 北京"),
        HomeListRow("2020 星越L", "15.2万 · 4.1万公里 · 上海"),
        HomeListRow("2019 博越", "7.9万 · 5.5万公里 · 广州"),
    )
    val ledger = listOf(
        HomeListRow("门店日结 09-24", "收入 128,600 · 支出 32,400"),
        HomeListRow("门店日结 09-23", "收入 96,200 · 支出 28,100"),
    )
    val analytics = listOf(
        HomeListRow("本周线索转化", "转化率 12.4% · 环比 +1.2pp"),
        HomeListRow("试驾到店", "到店 86 · 成交 11"),
    )
    val partners = listOf(
        HomeListRow("王小明", "销售顾问 · 待确认加入"),
        HomeListRow("李华", "售后技师 · 待确认加入"),
        HomeListRow("赵倩", "网销 · 待确认加入"),
    )
    val followUps = listOf(
        HomeListRow("张先生 · 星越L", "意向跟进 · 今日回访"),
        HomeListRow("刘女士 · 帝豪", "报价中 · 明电联"),
    )
    val appointments = listOf(
        HomeListRow("陈先生 · 保养", "今日 14:00 · 工位 A2"),
        HomeListRow("周女士 · 钣喷", "今日 16:30 · 工位 B1"),
    )
    val orders = listOf(
        HomeListRow("订单 #NC-9021", "星瑞 · 待门店审核"),
        HomeListRow("订单 #NC-9018", "缤越 · 待门店审核"),
    )
    val afterSales = listOf(
        HomeListRow("工单 AS-441", "保养套餐 · 进行中"),
        HomeListRow("工单 AS-438", "索赔 · 待配件"),
    )
    val newCars = listOf(
        HomeListRow("客户 孙某", "银河 L7 · 试驾完成"),
        HomeListRow("客户 吴某", "星愿 · 报价跟进"),
    )
}

@Composable
private fun FeatureContentScreen(title: String, onBack: () -> Unit) {
    ReportMainTabRoot(isRoot = false)
    Column(
        Modifier
            .fillMaxSize()
            .background(DemoColors.PageBg),
    ) {
        MineTopBar(title = title, onBack = onBack, containerColor = DemoColors.PageBg)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            repeat(6) { i ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(88.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DemoColors.Background)
                        .padding(16.dp),
                ) {
                    Text(
                        "$title 内容卡 ${i + 1}",
                        color = DemoColors.TextPrimary,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun CheckInMallScreen(onBack: () -> Unit) {
    ReportMainTabRoot(isRoot = false)
    var points by remember { mutableStateOf(1280) }
    Column(Modifier.fillMaxSize().background(DemoColors.PageBg)) {
        MineTopBar(title = "签到商城", onBack = onBack, containerColor = DemoColors.PageBg)
        Column(Modifier.padding(16.dp)) {
            Text("当前积分 $points", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    points += 10
                    showPlatformToast("签到成功 +10")
                },
                colors = ButtonDefaults.buttonColors(containerColor = DemoColors.Primary),
            ) { Text("今日签到") }
            Spacer(Modifier.height(16.dp))
            listOf("流量券", "洗车券", "精品周边").forEach { name ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                        .clickable { showPlatformToast("兑换 $name（mock）") },
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(name, color = DemoColors.TextPrimary)
                    Text("兑换", color = DemoColors.Accent)
                }
                HorizontalDivider(color = DemoColors.Divider)
            }
        }
    }
}

@Composable
private fun DubbingFeedScreen(onBack: () -> Unit, onOpenHotRank: () -> Unit) {
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(DemoColors.PageBg)) {
        MineTopBar(title = "配音", onBack = onBack, containerColor = DemoColors.PageBg)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DemoColors.Accent.copy(alpha = 0.1f))
                    .clickable(onClick = onOpenHotRank)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("热配榜", fontWeight = FontWeight.SemiBold, color = DemoColors.TextPrimary)
                Text("查看 >", color = DemoColors.Accent)
            }
            HomeMockData.rankItemsForTab(0).take(5).forEach { item ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(DemoColors.Background)
                        .padding(14.dp),
                ) {
                    Text("${item.rank}. ${item.title}", fontWeight = FontWeight.Medium)
                    Text(item.subtitle, color = DemoColors.TextSecondary, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun HotRankDetailScreen(onBack: () -> Unit) {
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(DemoColors.PageBg)) {
        MineTopBar(title = "热配榜", onBack = onBack, containerColor = DemoColors.PageBg)
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(HomeMockData.rankItemsForTab(0)) { item ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(DemoColors.Background)
                        .padding(14.dp),
                ) {
                    Text("#${item.rank} ${item.title}", fontWeight = FontWeight.SemiBold)
                    Text(item.subtitle, color = DemoColors.TextSecondary, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun CrudListScreen(
    title: String,
    items: List<HomeListRow>,
    onBack: () -> Unit,
    onItem: ((HomeListRow) -> Unit)? = null,
    onCreate: (() -> Unit)? = null,
) {
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(DemoColors.PageBg)) {
        MineTopBar(
            title = title,
            onBack = onBack,
            containerColor = DemoColors.PageBg,
            actions = {
                if (onCreate != null) {
                    TextButton(onClick = onCreate) {
                        Text("新建", color = DemoColors.Accent)
                    }
                }
            },
        )
        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无数据", color = DemoColors.TextSecondary)
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(16.dp)) {
                items(items) { row ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DemoColors.Background)
                            .clickable { onItem?.invoke(row) }
                            .padding(14.dp)
                            .padding(bottom = 10.dp),
                    ) {
                        Text(row.title, fontWeight = FontWeight.Medium, color = DemoColors.TextPrimary)
                        Spacer(Modifier.height(4.dp))
                        Text(row.subtitle, color = DemoColors.TextSecondary, fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun CrudDetailScreen(title: String, body: Any, onBack: () -> Unit) {
    val text = when (body) {
        is HomeListRow -> "${body.title}\n${body.subtitle}"
        else -> body.toString()
    }
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(DemoColors.PageBg)) {
        MineTopBar(title = title, onBack = onBack, containerColor = DemoColors.PageBg)
        Text(
            text,
            modifier = Modifier.padding(16.dp),
            color = DemoColors.TextPrimary,
            lineHeight = 22.sp,
        )
    }
}

@Composable
private fun CrudCreateScreen(title: String, onBack: () -> Unit, onSubmit: () -> Unit) {
    var field by remember { mutableStateOf("") }
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(DemoColors.PageBg)) {
        MineTopBar(title = title, onBack = onBack, containerColor = DemoColors.PageBg)
        Column(Modifier.padding(16.dp)) {
            OutlinedTextField(
                value = field,
                onValueChange = { field = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("标题") },
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    if (field.isBlank()) {
                        showPlatformToast("请填写标题")
                    } else {
                        onSubmit()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = DemoColors.Primary),
            ) { Text("提交") }
        }
    }
}

package com.example.my_kmp_project.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
        HomeRoutes.UsedCar -> UsedCarListScreen(
            onBack = onBack,
            onItem = { onNavigate(HomeRoutes.UsedCarDetail) },
            onCreate = { onNavigate(HomeRoutes.UsedCarCreate) },
        )
        HomeRoutes.UsedCarDetail -> CrudDetailScreen(
            "二手车详情",
            HomeSecondaryMock.usedCarOrders.first().let {
                "${it.vehicleModel}\n${it.plateNo} · ${it.modelYear}款 · ${it.mileageKm}km\n${it.amountLabel} ¥${it.amount}"
            },
            onBack,
        )
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
        HomeRoutes.AfterSales -> AfterSalesListScreen(
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

internal data class UsedCarOrderRow(
    val kindLabel: String,
    val statusLabel: String,
    val submittedDate: String,
    val vehicleModel: String,
    val plateNo: String,
    val modelYear: Int,
    val mileageKm: Int,
    val amountLabel: String,
    val amount: Int,
    val customerName: String,
)

internal object HomeSecondaryMock {
    val usedCarOrders = listOf(
        UsedCarOrderRow(
            kindLabel = "置换",
            statusLabel = "待审核",
            submittedDate = "2026-09-22",
            vehicleModel = "2021 帝豪",
            plateNo = "京A·88X21",
            modelYear = 2021,
            mileageKm = 32000,
            amountLabel = "评估价",
            amount = 86000,
            customerName = "张先生",
        ),
        UsedCarOrderRow(
            kindLabel = "专卖",
            statusLabel = "已通过",
            submittedDate = "2026-09-18",
            vehicleModel = "2020 星越L",
            plateNo = "沪B·6K902",
            modelYear = 2020,
            mileageKm = 41000,
            amountLabel = "成交价",
            amount = 152000,
            customerName = "李女士",
        ),
        UsedCarOrderRow(
            kindLabel = "收车",
            statusLabel = "已提交",
            submittedDate = "2026-09-15",
            vehicleModel = "2019 博越",
            plateNo = "粤C·19H33",
            modelYear = 2019,
            mileageKm = 55000,
            amountLabel = "收车价",
            amount = 79000,
            customerName = "王先生",
        ),
    )
    val usedCars = usedCarOrders.map {
        HomeListRow(it.vehicleModel, "${it.amount / 10000.0}万 · ${it.mileageKm / 10000.0}万公里")
    }
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
private fun UsedCarListScreen(
    onBack: () -> Unit,
    onItem: (UsedCarOrderRow) -> Unit,
    onCreate: () -> Unit,
) {
    // Flutter UsedCarListPage: green summary + status/kind chips + order cards
    val accent = Color(0xFF0B6E4F)
    val ink = Color(0xFF1C2430)
    val bg = Color(0xFFF3F5F8)
    val allOrders = HomeSecondaryMock.usedCarOrders
    val statusTabs = listOf("全部", "待审核", "已通过", "未通过")
    val kindTabs = listOf("全部类型", "置换", "专卖", "收车")
    var status by remember { mutableStateOf("全部") }
    var kind by remember { mutableStateOf("全部类型") }
    val filtered = allOrders.filter { row ->
        val statusOk = status == "全部" || row.statusLabel == status ||
            (status == "待审核" && row.statusLabel == "已提交")
        val kindOk = kind == "全部类型" || row.kindLabel == kind
        statusOk && kindOk
    }
    val submitted = allOrders.count { it.statusLabel == "已提交" }
    val pending = allOrders.count { it.statusLabel == "待审核" || it.statusLabel == "已提交" }
    val approved = allOrders.count { it.statusLabel == "已通过" }
    val rejected = allOrders.count { it.statusLabel == "未通过" }

    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(bg)) {
        MineTopBar(
            title = "二手车",
            onBack = onBack,
            containerColor = Color.White,
            actions = {
                TextButton(onClick = onCreate) {
                    Text("新建", color = accent)
                }
            },
        )
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                Column(
                    Modifier
                        .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(listOf(Color(0xFF0B6E4F), Color(0xFF149E6F))),
                        )
                        .padding(16.dp),
                ) {
                    Text("销售顾问", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "门店顾问 · 演示门店",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth()) {
                        listOf(
                            "已提交" to submitted,
                            "待审核" to pending,
                            "已通过" to approved,
                            "未通过" to rejected,
                        ).forEach { (label, value) ->
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$value", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Text(label, color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
            item {
                Column {
                    Row(
                        Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                    ) {
                        statusTabs.forEach { tab ->
                            val selected = status == tab
                            Text(
                                tab,
                                color = if (selected) Color.White else ink,
                                fontSize = 13.sp,
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (selected) accent else Color(0xFFE8ECF0))
                                    .clickable { status = tab }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                    ) {
                        kindTabs.forEach { tab ->
                            val selected = kind == tab
                            Text(
                                tab,
                                color = if (selected) accent else DemoColors.TextSecondary,
                                fontSize = 13.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (selected) Color(0x140B6E4F) else Color.White)
                                    .clickable { kind = tab }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                            )
                        }
                    }
                }
            }
            if (filtered.isEmpty()) {
                item {
                    Column(
                        Modifier.fillMaxWidth().padding(48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("暂无业务单", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "点击右上角新建置换 / 专卖 / 收车单",
                            color = DemoColors.TextSecondary,
                            fontSize = 13.sp,
                        )
                    }
                }
            } else {
                items(filtered) { row ->
                    Column(
                        Modifier
                            .padding(horizontal = 16.dp, vertical = 5.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White)
                            .clickable { onItem(row) }
                            .padding(14.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                row.kindLabel,
                                color = accent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0x140B6E4F))
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                row.statusLabel,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFF0F2F5))
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                            Spacer(Modifier.weight(1f))
                            Text(row.submittedDate, fontSize = 12.sp, color = DemoColors.TextSecondary)
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(row.vehicleModel, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ink)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${row.plateNo} · ${row.modelYear}款 · ${row.mileageKm}km",
                            fontSize = 13.sp,
                            color = DemoColors.TextSecondary,
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(row.amountLabel, fontSize = 12.sp, color = DemoColors.TextSecondary)
                            Spacer(Modifier.width(6.dp))
                            Text("¥${row.amount}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ink)
                            Spacer(Modifier.weight(1f))
                            Text(row.customerName, fontSize = 13.sp, color = DemoColors.TextSecondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AfterSalesListScreen(
    onBack: () -> Unit,
    onItem: (HomeListRow) -> Unit,
    onCreate: () -> Unit,
) {
    // Flutter AfterSalesListPage: orange hero + 待处理预约 + 服务记录 list
    val items = HomeSecondaryMock.afterSales
    val appointments = HomeSecondaryMock.appointments
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        MineTopBar(
            title = "售后专区",
            onBack = onBack,
            containerColor = Color.White,
            actions = {
                TextButton(onClick = onCreate) {
                    Text("新建", color = Color(0xFFFF9500))
                }
            },
        )
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                Column(
                    Modifier
                        .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFE67E22), Color(0xFFFF9500)),
                            ),
                        )
                        .padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 16.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .width(40.dp)
                                .height(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("修", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "维修保养档案",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "当前店服务记录与预约跟进",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 13.sp,
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${items.size}", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Text("记录", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                        }
                        Box(Modifier.width(1.dp).height(28.dp).background(Color.White.copy(alpha = 0.25f)))
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "${appointments.size}",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text("待预约", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                        }
                    }
                }
            }
            if (appointments.isNotEmpty()) {
                item {
                    Text(
                        "待处理预约",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
                    )
                }
                items(appointments) { row ->
                    Column(
                        Modifier
                            .padding(horizontal = 16.dp, vertical = 5.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White)
                            .clickable { onItem(row) }
                            .padding(14.dp),
                    ) {
                        Text(row.title, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(4.dp))
                        Text(row.subtitle, color = DemoColors.TextSecondary, fontSize = 13.sp)
                    }
                }
            }
            item {
                Text(
                    "维修保养记录",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 8.dp),
                )
            }
            items(items) { row ->
                Column(
                    Modifier
                        .padding(horizontal = 16.dp, vertical = 5.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White)
                        .clickable { onItem(row) }
                        .padding(14.dp),
                ) {
                    Text(row.title, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))
                    Text(row.subtitle, color = DemoColors.TextSecondary, fontSize = 13.sp)
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

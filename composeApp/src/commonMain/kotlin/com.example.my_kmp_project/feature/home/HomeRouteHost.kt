package com.example.my_kmp_project.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
        HomeRoutes.LifeService -> LifeServiceScreen(onBack = onBack)
        HomeRoutes.LiveCommerce -> LiveCommerceScreen(onBack = onBack)
        HomeRoutes.Club -> ClubScreen(onBack = onBack)
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
        HomeRoutes.DataAnalytics -> AnalyticsListScreen(
            onBack = onBack,
            onItem = { onNavigate(HomeRoutes.DataAnalyticsDetail) },
        )
        HomeRoutes.DataAnalyticsDetail ->
            CrudDetailScreen(
                "分析详情",
                HomeSecondaryMock.analyticsRecords.first().let {
                    "${it.title}\nPV ${it.pv} · 点击 ${it.clicks} · 转化 ${it.converts}\n${it.subtitle}"
                },
                onBack,
            )
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
        HomeRoutes.NewCarFollow -> NewCarFollowListScreen(
            onBack = onBack,
            onItem = { onNavigate(HomeRoutes.NewCarFollowDetail) },
            onCreate = { onNavigate(HomeRoutes.NewCarFollowCreate) },
        )
        HomeRoutes.NewCarFollowCreate -> CrudCreateScreen("新建跟进", onBack) {
            showPlatformToast("已保存（mock）")
            onBack()
        }
        HomeRoutes.NewCarFollowDetail ->
            CrudDetailScreen(
                "跟进详情",
                HomeSecondaryMock.newCarFollows.first().let {
                    "${it.customerName}\n${it.phone}\n意向: ${it.vehicle}\n阶段 ${it.stage}"
                },
                onBack,
            )
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

internal data class NewCarFollowRow(
    val customerName: String,
    val phone: String,
    val vehicle: String,
    val stage: String,
    val intentBand: String,
    val nextFollow: String,
    val owner: String,
    val overdue: Boolean = false,
)

internal data class AnalyticsRecordRow(
    val title: String,
    val subtitle: String,
    val pv: Int,
    val clicks: Int,
    val converts: Int,
    val featured: Boolean = false,
    val anomaly: Boolean = false,
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
    val analyticsRecords = listOf(
        AnalyticsRecordRow(
            title = "本周线索转化",
            subtitle = "门店线索漏斗 · 高意向优先",
            pv = 12840,
            clicks = 962,
            converts = 119,
            featured = true,
            anomaly = false,
        ),
        AnalyticsRecordRow(
            title = "试驾到店",
            subtitle = "预约试驾 → 到店完成",
            pv = 4520,
            clicks = 610,
            converts = 86,
            featured = false,
            anomaly = false,
        ),
        AnalyticsRecordRow(
            title = "直播线索异常",
            subtitle = "点击骤降 · 需排查投放",
            pv = 2100,
            clicks = 42,
            converts = 3,
            featured = false,
            anomaly = true,
        ),
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
    val newCarFollows = listOf(
        NewCarFollowRow(
            customerName = "孙某",
            phone = "138****2101",
            vehicle = "银河 L7",
            stage = "跟进中",
            intentBand = "高",
            nextFollow = "今日 15:00",
            owner = "销售顾问",
        ),
        NewCarFollowRow(
            customerName = "吴某",
            phone = "139****8820",
            vehicle = "星愿",
            stage = "报价",
            intentBand = "中",
            nextFollow = "明日 10:30",
            owner = "销售顾问",
        ),
        NewCarFollowRow(
            customerName = "赵某",
            phone = "186****4412",
            vehicle = "星越 L",
            stage = "试驾",
            intentBand = "低",
            nextFollow = "09-20 已逾期",
            owner = "网销",
            overdue = true,
        ),
    )
}

@Composable
private fun LifeServiceScreen(onBack: () -> Unit) {
    val items = listOf(
        "洗车美容" to "到店立减 · 预约免排队",
        "代驾服务" to "夜间 / 酒后代驾",
        "道路救援" to "一键呼叫 · 30 分钟达",
        "年检代办" to "免上线 · 资料代跑",
        "加油优惠" to "合作油站满减",
        "车险续保" to "比价出单 · 顾问跟进",
    )
    FeatureListScreen(
        title = "生活服务",
        heroTint = Color(0xFF00A870),
        heroTitle = "车生活一站办",
        heroSub = "洗车 · 代驾 · 救援 · 年检",
        items = items,
        onBack = onBack,
    )
}

@Composable
private fun LiveCommerceScreen(onBack: () -> Unit) {
    val items = listOf(
        "今晚 20:00 直播" to "星愿限时权益 · 预约提醒",
        "回放：试驾实录" to "播放 1.2 万 · 线索 86",
        "爆款配件专场" to "脚垫 / 行车记录仪",
        "直播线索池" to "待跟进 23 · 已转化 5",
        "主播排班" to "本周 4 场已排",
        "带货数据看板" to "GMV 12.8 万 · 转化 3.1%",
    )
    FeatureListScreen(
        title = "直播带货",
        heroTint = Color(0xFFE53935),
        heroTitle = "直播间进行中",
        heroSub = "演示门店 · 在线 328 人",
        items = items,
        onBack = onBack,
    )
}

@Composable
private fun ClubScreen(onBack: () -> Unit) {
    val items = listOf(
        "车友聚会 · 本周六" to "已报名 42 · 名额 60",
        "自驾游线路" to "京郊两日 · 招募中",
        "积分兑换专区" to "周边 / 洗车券",
        "会员日活动" to "每月 15 日到店礼",
        "俱乐部公告" to "新规：活动签到得双倍积分",
        "我的社群" to "演示门店 Club · 已加入",
    )
    FeatureListScreen(
        title = "Club",
        heroTint = Color(0xFF5B6CFF),
        heroTitle = "车友 Club",
        heroSub = "活动 · 自驾 · 积分权益",
        items = items,
        onBack = onBack,
    )
}

@Composable
private fun FeatureListScreen(
    title: String,
    heroTint: Color,
    heroTitle: String,
    heroSub: String,
    items: List<Pair<String, String>>,
    onBack: () -> Unit,
) {
    ReportMainTabRoot(isRoot = false)
    Column(
        Modifier
            .fillMaxSize()
            .background(DemoColors.PageBg)
            .verticalScroll(rememberScrollState()),
    ) {
        MineTopBar(title = title, onBack = onBack, containerColor = DemoColors.PageBg)
        Box(
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(heroTint)
                .padding(18.dp),
        ) {
            Column {
                Text(heroTitle, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(4.dp))
                Text(heroSub, color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        items.forEach { (name, sub) ->
            Column(
                Modifier
                    .padding(horizontal = 16.dp, vertical = 5.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .clickable { showPlatformToast(name) }
                    .padding(14.dp),
            ) {
                Text(name, fontWeight = FontWeight.SemiBold, color = DemoColors.TextPrimary)
                Spacer(Modifier.height(4.dp))
                Text(sub, fontSize = 13.sp, color = DemoColors.TextSecondary)
            }
        }
        Spacer(Modifier.height(24.dp))
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
private fun AnalyticsListScreen(
    onBack: () -> Unit,
    onItem: (AnalyticsRecordRow) -> Unit,
) {
    // Flutter AnalyticsListPage: summary bar + metric cards
    val primary = Color(0xFF0070F3)
    val bg = Color(0xFFF5F5F5)
    val items = HomeSecondaryMock.analyticsRecords
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(bg)) {
        MineTopBar(title = "数据分析", onBack = onBack, containerColor = Color.White)
        Row(
            Modifier
                .padding(start = 12.dp, end = 12.dp, top = 12.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .border(1.dp, Color(0xFFEAEAEA), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("已加载 ${items.size} / 共 ${items.size} · 第 1 页", color = DemoColors.TextSecondary, fontSize = 13.sp)
            Spacer(Modifier.weight(1f))
            Text(
                "gRPC",
                color = primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(primary.copy(alpha = 0.08f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
        LazyColumn(contentPadding = PaddingValues(12.dp)) {
            items(items) { row ->
                val cue = when {
                    row.anomaly -> Color(0xFFE53935)
                    row.featured -> Color(0xFFFF9500)
                    else -> Color.Transparent
                }
                val rate = if (row.clicks > 0) row.converts * 100f / row.clicks else 0f
                Row(
                    Modifier
                        .padding(bottom = 8.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .clickable { onItem(row) },
                ) {
                    Box(Modifier.width(4.dp).height(96.dp).background(cue))
                    Column(Modifier.padding(14.dp).weight(1f)) {
                        Text(row.title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(row.subtitle, color = DemoColors.TextSecondary, fontSize = 12.sp)
                        Spacer(Modifier.height(10.dp))
                        Row {
                            Text("PV ${row.pv}", fontSize = 12.sp, color = DemoColors.TextSecondary)
                            Spacer(Modifier.width(12.dp))
                            Text("点击 ${row.clicks}", fontSize = 12.sp, color = DemoColors.TextSecondary)
                            Spacer(Modifier.width(12.dp))
                            Text("转化 ${row.converts}", fontSize = 12.sp, color = DemoColors.TextSecondary)
                            Spacer(Modifier.weight(1f))
                            Text("${(rate * 10).toInt() / 10.0}%", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NewCarFollowListScreen(
    onBack: () -> Unit,
    onItem: (NewCarFollowRow) -> Unit,
    onCreate: () -> Unit,
) {
    // Flutter NewCarFollowListPage: profile header + intent tabs + cards + FAB
    val bg = Color(0xFFF5F6F8)
    val accent = Color(0xFF3B8CFF)
    val ink = Color(0xFF1A1A1A)
    val tabs = listOf("全部", "高意向", "中意向", "低意向", "逾期")
    var tab by remember { mutableStateOf("全部") }
    val all = HomeSecondaryMock.newCarFollows
    val filtered = when (tab) {
        "高意向" -> all.filter { it.intentBand == "高" }
        "中意向" -> all.filter { it.intentBand == "中" }
        "低意向" -> all.filter { it.intentBand == "低" }
        "逾期" -> all.filter { it.overdue }
        else -> all
    }
    val active = all.count { !it.overdue }
    val overdue = all.count { it.overdue }
    val high = all.count { it.intentBand == "高" }

    ReportMainTabRoot(isRoot = false)
    Box(Modifier.fillMaxSize().background(bg)) {
        Column(Modifier.fillMaxSize()) {
            MineTopBar(title = "新车跟进", onBack = onBack, containerColor = Color.White)
            LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
                item {
                    Column(
                        Modifier
                            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 20.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("销售顾问", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = ink)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "顾问",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(accent)
                                            .padding(horizontal = 8.dp, vertical = 3.dp),
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Text("演示门店", color = DemoColors.TextSecondary, fontSize = 13.sp)
                            }
                            Box(
                                Modifier
                                    .width(56.dp)
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(28.dp))
                                    .background(Color(0xFFE8EEF8)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("销", color = accent, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                        Row(Modifier.fillMaxWidth()) {
                            listOf(
                                "$active" to "跟进中",
                                "$overdue" to "逾期",
                                "$high" to "高意向",
                                "0" to "战败",
                            ).forEach { (v, label) ->
                                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(v, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ink)
                                    Spacer(Modifier.height(4.dp))
                                    Text(label, fontSize = 12.sp, color = DemoColors.TextSecondary)
                                }
                            }
                        }
                    }
                }
                item {
                    Row(
                        Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        tabs.forEach { t ->
                            val sel = tab == t
                            Column(
                                Modifier
                                    .clickable { tab = t }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    t,
                                    fontSize = 15.sp,
                                    fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (sel) ink else DemoColors.TextSecondary,
                                )
                                Spacer(Modifier.height(6.dp))
                                Box(
                                    Modifier
                                        .width(28.dp)
                                        .height(3.dp)
                                        .background(if (sel) accent else Color.Transparent),
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
                            Text("暂无跟进档案", fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(8.dp))
                            Text("点击下方按钮新建客户跟进", color = DemoColors.TextSecondary, fontSize = 13.sp)
                        }
                    }
                } else {
                    items(filtered) { row ->
                        Column(
                            Modifier
                                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White)
                                .clickable { onItem(row) }
                                .padding(14.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "客户",
                                    fontSize = 11.sp,
                                    color = accent,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0x1A3B8CFF))
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(row.customerName, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = ink)
                                Spacer(Modifier.weight(1f))
                                Text(
                                    "${row.intentBand}意向",
                                    fontSize = 12.sp,
                                    color = when (row.intentBand) {
                                        "高" -> Color(0xFFE53935)
                                        "中" -> Color(0xFFFF9500)
                                        else -> DemoColors.TextSecondary
                                    },
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(row.phone, fontSize = 13.sp, color = DemoColors.TextSecondary)
                            Spacer(Modifier.height(6.dp))
                            Text("意向车型: ${row.vehicle}", fontSize = 13.sp, color = DemoColors.TextSecondary)
                            Spacer(Modifier.height(8.dp))
                            Row {
                                Text("阶段 ${row.stage}", fontSize = 12.sp, color = DemoColors.TextSecondary)
                                Spacer(Modifier.width(12.dp))
                                Text("下次跟进 ${row.nextFollow}", fontSize = 12.sp, color = DemoColors.TextSecondary)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text("负责人 ${row.owner}", fontSize = 12.sp, color = DemoColors.TextSecondary)
                        }
                    }
                }
            }
        }
        Button(
            onClick = onCreate,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = accent),
        ) {
            Text("新建跟进", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
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

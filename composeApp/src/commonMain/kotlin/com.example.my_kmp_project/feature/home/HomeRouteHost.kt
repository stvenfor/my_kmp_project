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
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
        HomeRoutes.UsedCarDetail -> UsedCarDetailScreen(
            order = HomeSecondaryMock.usedCarOrders.first(),
            onBack = onBack,
        )
        HomeRoutes.UsedCarCreate -> UsedCarCreateScreen(onBack = onBack)
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
        HomeRoutes.TodoPartner -> PartnerPendingScreen(onBack = onBack)
        HomeRoutes.TodoFollowUp -> FollowUpCustomersScreen(onBack = onBack)
        HomeRoutes.TodoAfterSales -> AfterSalesAppointmentsScreen(onBack = onBack)
        HomeRoutes.TodoOrderReview -> StoreReviewOrdersScreen(onBack = onBack)
        HomeRoutes.AfterSales -> AfterSalesListScreen(
            onBack = onBack,
            onItem = { onNavigate(HomeRoutes.AfterSalesDetail) },
            onCreate = { onNavigate(HomeRoutes.AfterSalesCreate) },
        )
        HomeRoutes.AfterSalesCreate -> AfterSalesCreateScreen(onBack = onBack)
        HomeRoutes.AfterSalesDetail -> AfterSalesDetailScreen(
            row = HomeSecondaryMock.afterSales.first(),
            onBack = onBack,
        )
        HomeRoutes.NewCarFollow -> NewCarFollowListScreen(
            onBack = onBack,
            onItem = { onNavigate(HomeRoutes.NewCarFollowDetail) },
            onCreate = { onNavigate(HomeRoutes.NewCarFollowCreate) },
        )
        HomeRoutes.NewCarFollowCreate -> NewCarFollowCreateScreen(onBack = onBack)
        HomeRoutes.NewCarFollowDetail -> NewCarFollowDetailScreen(
            row = HomeSecondaryMock.newCarFollows.first(),
            onBack = onBack,
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
    // Flutter: HomeFeatureContentPage(title: 生活服务, child: HomeVideoTabContent)
    val shortcuts = listOf(
        "会员专享" to Color(0xFF0070F3),
        "配音专栏" to Color(0xFFFF9500),
        "其他课程" to Color(0xFF5856D6),
        "功能教程" to Color(0xFF34C759),
    )
    val daily = listOf(
        Triple("带你玩转 ETF", "直播中", true),
        Triple("新能源赛道解读", "回放", false),
        Triple("门店短视频运营", "直播中", true),
    )
    val courses = listOf(
        Triple("【配置】当星舰撞上算力", "尤国梁", true),
        Triple("黄金恐贪定投实战", "策略组", false),
    )
    ReportMainTabRoot(isRoot = false)
    Column(
        Modifier
            .fillMaxSize()
            .background(DemoColors.PageBg)
            .verticalScroll(rememberScrollState()),
    ) {
        MineTopBar(title = "生活服务", onBack = onBack, containerColor = DemoColors.PageBg)
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            shortcuts.forEach { (label, tint) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showPlatformToast(label) },
                ) {
                    Box(
                        Modifier
                            .width(52.dp)
                            .height(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(tint.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(label.take(1), color = tint, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(label, fontSize = 12.sp, color = DemoColors.TextPrimary, maxLines = 1)
                }
            }
        }
        LifeSectionHeader(title = "每日推荐")
        Column(Modifier.padding(horizontal = 16.dp)) {
            daily.forEach { (title, tag, isLive) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(12.dp))
                        .clickable { showPlatformToast(title) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        tag,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isLive) Color(0xFFFF3B30) else DemoColors.TextSecondary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (isLive) Color(0x14FF3B30) else Color(0xFFF2F2F7),
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Box(
                        Modifier
                            .width(36.dp)
                            .height(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFFE8EEF5)),
                    )
                }
            }
        }
        LifeSectionHeader(title = "热门课程")
        Row(
            Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            courses.forEach { (title, author, isMember) ->
                Column(
                    Modifier
                        .width(168.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(12.dp))
                        .clickable { showPlatformToast(title) },
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(96.dp)
                            .background(Color(0xFFE8EEF5)),
                    ) {
                        Text(
                            "直播中",
                            color = Color.White,
                            fontSize = 10.sp,
                            modifier = Modifier
                                .padding(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = 0.45f))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                    Column(Modifier.padding(10.dp)) {
                        Text(
                            title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 17.sp,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(author, fontSize = 12.sp, color = DemoColors.TextSecondary)
                        if (isMember) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "V 会员专属",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFFF9500),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0x14FF9500))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "进入配音视频专区",
            color = DemoColors.Accent,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DemoColors.Accent.copy(alpha = 0.08f))
                .border(1.dp, DemoColors.Accent.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                .clickable { showPlatformToast("配音") }
                .padding(vertical = 14.dp),
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun LifeSectionHeader(title: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
        Text("更多 >", fontSize = 13.sp, color = DemoColors.TextSecondary)
    }
}

@Composable
private fun LiveCommerceScreen(onBack: () -> Unit) {
    // Flutter wires 直播带货 → HomeClubTabContent (same as Club)
    ClubContentBody(title = "直播带货", onBack = onBack)
}

@Composable
private fun ClubScreen(onBack: () -> Unit) {
    ClubContentBody(title = "Club", onBack = onBack)
}

@Composable
private fun ClubContentBody(title: String, onBack: () -> Unit) {
    // Flutter: HomeClubTabContent
    var filter by remember { mutableStateOf(0) }
    val filters = listOf("最新", "嘉宾分享", "资料")
    data class ClubPost(
        val author: String,
        val date: String,
        val content: String,
        val pdfName: String? = null,
    )
    val posts = listOf(
        ClubPost(
            "莫听官方",
            "06-24",
            "【官方纪要】本期聚焦 AI 算力与产业趋势，内容仅供合格投资者参考。",
            "【莫听Club第78期】聊聊AI最靓的仔.pdf",
        ),
        ClubPost(
            "策略研究员",
            "06-20",
            "当星舰遇到算力：嘉宾分享回顾与延伸阅读。",
        ),
    )
    ReportMainTabRoot(isRoot = false)
    Column(
        Modifier
            .fillMaxSize()
            .background(DemoColors.PageBg)
            .verticalScroll(rememberScrollState()),
    ) {
        MineTopBar(title = title, onBack = onBack, containerColor = DemoColors.PageBg)
        Row(
            Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(12.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .width(52.dp)
                    .height(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1C1C3A)),
                contentAlignment = Alignment.Center,
            ) {
                Text("Club", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("莫听Club", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                Spacer(Modifier.height(4.dp))
                Text("动态 127 | 成员 1040", fontSize = 12.sp, color = DemoColors.TextSecondary)
            }
            Text(
                "+ 加入",
                color = Color.White,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(DemoColors.Accent)
                    .clickable { showPlatformToast("加入 Club") }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                fontSize = 14.sp,
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            filters.forEachIndexed { i, label ->
                val selected = filter == i
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { filter = i },
                ) {
                    Text(
                        label,
                        fontSize = 15.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) DemoColors.TextPrimary else DemoColors.TextSecondary,
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier
                            .width(if (selected) 20.dp else 0.dp)
                            .height(2.dp)
                            .background(DemoColors.Accent),
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        posts.forEach { post ->
            Column(
                Modifier
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(12.dp))
                    .padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .width(40.dp)
                            .height(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFE8EEF5)),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(post.author, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text(post.date, fontSize = 12.sp, color = DemoColors.TextSecondary)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(post.content, fontSize = 15.sp, color = DemoColors.TextPrimary)
                post.pdfName?.let { pdf ->
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF2F2F7))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("📄", fontSize = 16.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(pdf, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    listOf("分享", "评论", "点赞").forEach { action ->
                        Text(action, fontSize = 13.sp, color = DemoColors.TextSecondary)
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "进入社区查看更多",
            color = DemoColors.Accent,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(12.dp))
                .clickable { showPlatformToast("社区") }
                .padding(vertical = 14.dp),
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun CheckInMallScreen(onBack: () -> Unit) {
    // Flutter 签到商城：蓝顶栏积分区 + 连签卡 + 成长任务 + 积分换礼
    val headerBlue = Color(0xFF2F6BFF)
    var points by remember { mutableStateOf(0) }
    var streak by remember { mutableStateOf(1) }
    var checkedToday by remember { mutableStateOf(false) }
    var remind by remember { mutableStateOf(false) }
    val dayLabels = listOf("19", "20", "21", "22", "23", "24", "今天")
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(Color(0xFFF3F5F8))) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(headerBlue)
                .statusBarsPadding(),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "‹",
                    color = Color.White,
                    fontSize = 28.sp,
                    modifier = Modifier
                        .clickable(onClick = onBack)
                        .padding(8.dp),
                )
                Text(
                    "签到商城",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.width(44.dp))
            }
            Text(
                "温馨提示：本页面只保留近3个月内的积分记录",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 12.sp,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text("我的积分", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                    Text("$points", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("连续签到", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                    Text("$streak 天", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .padding(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("连签可得更多积分", fontWeight = FontWeight.SemiBold)
                        Text("已连续签到 $streak 天", fontSize = 13.sp, color = DemoColors.TextSecondary)
                    }
                    Text(
                        if (checkedToday) "已签到" else "立即签到",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (checkedToday) DemoColors.Muted else headerBlue)
                            .clickable(enabled = !checkedToday) {
                                checkedToday = true
                                points += 5
                                showPlatformToast("签到成功 +5")
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    dayLabels.forEachIndexed { i, label ->
                        val today = i == dayLabels.lastIndex
                        Column(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (today) Color(0xFFFFB020) else Color(0xFFF2F3F7))
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                label,
                                fontSize = 11.sp,
                                color = if (today) Color.White else DemoColors.TextSecondary,
                            )
                            Text(
                                "+5",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (today) Color.White else DemoColors.TextPrimary,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("断签或者签完需重新开始", fontSize = 12.sp, color = DemoColors.Muted)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("签到提醒", fontSize = 12.sp, color = DemoColors.TextSecondary)
                        Spacer(Modifier.width(6.dp))
                        Box(
                            Modifier
                                .width(40.dp)
                                .height(22.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(if (remind) headerBlue else Color(0xFFD0D3D8))
                                .clickable { remind = !remind },
                        )
                    }
                }
            }
            Text("成长任务", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White),
            ) {
                listOf(
                    Triple("每日登录", "+5积分", "领取"),
                    Triple("发一条动态", "+10积分", "去完成"),
                    Triple("商城下单", "+20积分", "去完成"),
                ).forEachIndexed { idx, (title, pts, action) ->
                    if (idx > 0) HorizontalDivider(color = DemoColors.Divider)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(title, fontWeight = FontWeight.Medium)
                            Text(pts, fontSize = 12.sp, color = DemoColors.TextSecondary)
                        }
                        Text(
                            action,
                            color = headerBlue,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(headerBlue.copy(alpha = 0.1f))
                                .clickable { showPlatformToast(action) }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
            }
            Text("积分换礼", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                Text("礼", fontSize = 40.sp, color = DemoColors.Muted)
            }
            Spacer(Modifier.height(16.dp))
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
private fun UsedCarCreateScreen(onBack: () -> Unit) {
    // Flutter UsedCarCreatePage — 新建业务单
    val accent = Color(0xFF0B6E4F)
    val bg = Color(0xFFF3F5F8)
    val kinds = listOf("置换" to "trade_in", "专卖" to "consign", "收车" to "purchase")
    var kind by remember { mutableStateOf("置换") }
    var customer by remember { mutableStateOf("张先生 138****2101") }
    var model by remember { mutableStateOf("") }
    var plate by remember { mutableStateOf("") }
    var vin by remember { mutableStateOf("") }
    var mileage by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(bg)) {
        MineTopBar(title = "新建业务单", onBack = onBack, containerColor = Color.White)
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("业务类型", fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                kinds.forEach { (label, _) ->
                    val selected = kind == label
                    Text(
                        label,
                        color = if (selected) Color.White else Color(0xFF1C2430),
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (selected) accent else Color.White)
                            .border(0.5.dp, if (selected) accent else DemoColors.Divider, RoundedCornerShape(16.dp))
                            .clickable { kind = label }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
            Text("客户", fontWeight = FontWeight.SemiBold)
            Text(
                customer,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(8.dp))
                    .clickable {
                        customer = if (customer.startsWith("张")) "李女士 139****8820" else "张先生 138****2101"
                    }
                    .padding(14.dp),
            )
            UsedCarField("车型名", model) { model = it }
            UsedCarField("车牌", plate) { plate = it }
            UsedCarField("VIN", vin) { vin = it }
            UsedCarField("里程(km)", mileage) { mileage = it }
            UsedCarField("年款", year) { year = it }
            UsedCarField("金额(元)", amount) { amount = it }
            Spacer(Modifier.height(8.dp))
            Text(
                "提交",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent)
                    .clickable {
                        showPlatformToast("已提交（$kind）")
                        onBack()
                    }
                    .padding(vertical = 14.dp),
            )
        }
    }
}

@Composable
private fun UsedCarField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )
}

@Composable
private fun UsedCarDetailScreen(order: UsedCarOrderRow, onBack: () -> Unit) {
    // Flutter UsedCarDetailPage
    val bg = Color(0xFFF3F5F8)
    val ink = Color(0xFF1C2430)
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(bg)) {
        MineTopBar(title = "业务单详情", onBack = onBack, containerColor = Color.White)
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .padding(18.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        order.kindLabel,
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF0B6E4F))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                    Text(
                        order.statusLabel,
                        color = Color(0xFF0B6E4F),
                        fontSize = 12.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF0B6E4F).copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(order.vehicleModel, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = ink)
                Spacer(Modifier.height(6.dp))
                Text(
                    "${order.amountLabel} ¥${order.amount}",
                    color = Color(0xFF0B6E4F),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                )
            }
            UsedCarSection(
                "客户",
                listOf("姓名" to order.customerName, "手机" to "138****0000"),
            )
            UsedCarSection(
                "车辆",
                listOf(
                    "车型" to order.vehicleModel,
                    "车牌" to order.plateNo,
                    "VIN" to "L6T********DEMO",
                    "里程" to "${order.mileageKm} km",
                    "年款" to "${order.modelYear}",
                ),
            )
            UsedCarSection(
                "审核",
                listOf(
                    "状态" to order.statusLabel,
                    "提交时间" to order.submittedDate,
                ),
            )
        }
    }
}

@Composable
private fun UsedCarSection(title: String, rows: List<Pair<String, String>>) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(16.dp),
    ) {
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        Spacer(Modifier.height(10.dp))
        rows.forEach { (k, v) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(k, color = DemoColors.TextSecondary, fontSize = 13.sp)
                Text(v, color = DemoColors.TextPrimary, fontSize = 13.sp)
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
private fun AfterSalesCreateScreen(onBack: () -> Unit) {
    // Flutter AfterSalesCreatePage
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var plate by remember { mutableStateOf("") }
    var mileage by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("2026-09-25") }
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(Color(0xFFF3F5F8))) {
        MineTopBar(title = "创建售后", onBack = onBack, containerColor = Color.White)
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            UsedCarField("客户姓名", name) { name = it; if (title.isBlank()) title = "$it 售后服务" }
            UsedCarField("手机号", phone) { phone = it }
            UsedCarField("工单标题", title) { title = it }
            UsedCarField("车牌", plate) { plate = it }
            UsedCarField("里程(km)", mileage) { mileage = it }
            UsedCarField("预约日", date) { date = it }
            UsedCarField("问题描述", content) { content = it }
            Spacer(Modifier.height(8.dp))
            Text(
                "提交",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DemoColors.Accent)
                    .clickable {
                        showPlatformToast("已创建售后工单")
                        onBack()
                    }
                    .padding(vertical = 14.dp),
            )
        }
    }
}

@Composable
private fun AfterSalesDetailScreen(row: HomeListRow, onBack: () -> Unit) {
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(Color(0xFFF3F5F8))) {
        MineTopBar(title = "售后详情", onBack = onBack, containerColor = Color.White)
        Column(Modifier.padding(16.dp)) {
            UsedCarSection(
                "工单",
                listOf(
                    "编号" to row.title,
                    "内容" to row.subtitle,
                    "状态" to "进行中",
                    "预约日" to "2026-09-25",
                ),
            )
        }
    }
}

@Composable
private fun NewCarFollowCreateScreen(onBack: () -> Unit) {
    // Flutter NewCarFollowCreatePage
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var vehicle by remember { mutableStateOf("") }
    var level by remember { mutableStateOf("高") }
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(Color(0xFFF5F6F8))) {
        MineTopBar(title = "新建跟进", onBack = onBack, containerColor = Color.White)
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            UsedCarField("客户姓名", name) { name = it }
            UsedCarField("手机号", phone) { phone = it }
            UsedCarField("意向车型", vehicle) { vehicle = it }
            Text("意向等级", fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("高", "中", "低").forEach { label ->
                    val selected = level == label
                    Text(
                        label,
                        color = if (selected) Color.White else DemoColors.TextPrimary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (selected) DemoColors.Accent else Color.White)
                            .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(16.dp))
                            .clickable { level = label }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "提交",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DemoColors.Accent)
                    .clickable {
                        if (name.isBlank() || phone.isBlank()) {
                            showPlatformToast("请填写客户姓名和手机号")
                        } else {
                            showPlatformToast("已保存跟进")
                            onBack()
                        }
                    }
                    .padding(vertical = 14.dp),
            )
        }
    }
}

@Composable
private fun NewCarFollowDetailScreen(row: NewCarFollowRow, onBack: () -> Unit) {
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(Color(0xFFF5F6F8))) {
        MineTopBar(title = "跟进详情", onBack = onBack, containerColor = Color.White)
        Column(Modifier.padding(16.dp)) {
            UsedCarSection(
                "客户",
                listOf(
                    "姓名" to row.customerName,
                    "手机" to row.phone,
                    "意向车型" to row.vehicle,
                    "阶段" to row.stage,
                    "意向" to row.intentBand,
                    "下次跟进" to row.nextFollow,
                    "顾问" to row.owner,
                ),
            )
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
private fun PartnerPendingScreen(onBack: () -> Unit) {
    // Flutter PartnerPendingPage + _TodoForbiddenPanel (403 / 非店管)
    TodoForbiddenScreen(title = "新伙伴待确认", onBack = onBack)
}

@Composable
private fun FollowUpCustomersScreen(onBack: () -> Unit) {
    TodoForbiddenScreen(title = "待跟进客户", onBack = onBack)
}

@Composable
private fun AfterSalesAppointmentsScreen(onBack: () -> Unit) {
    TodoForbiddenScreen(title = "售后预约", onBack = onBack)
}

@Composable
private fun StoreReviewOrdersScreen(onBack: () -> Unit) {
    TodoForbiddenScreen(title = "订单待审核", onBack = onBack)
}

@Composable
private fun TodoForbiddenScreen(title: String, onBack: () -> Unit) {
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(DemoColors.PageBg)) {
        MineTopBar(title = title, onBack = onBack, containerColor = DemoColors.PageBg)
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                Modifier
                    .padding(horizontal = 28.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .padding(horizontal = 22.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .width(64.dp)
                        .height(64.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(DemoColors.Accent.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("🔒", fontSize = 26.sp)
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    "暂无查看权限",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = DemoColors.TextPrimary,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "该待办仅门店管理员可查看。\n如需处理，请联系店管开通权限，或切换有权限的账号。",
                    fontSize = 13.sp,
                    color = DemoColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                )
                Spacer(Modifier.height(22.dp))
                Text(
                    "返回",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DemoColors.Accent)
                        .clickable(onClick = onBack)
                        .padding(vertical = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun TodoEmpty(title: String, subtitle: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontWeight = FontWeight.SemiBold, color = DemoColors.TextPrimary)
            Spacer(Modifier.height(6.dp))
            Text(subtitle, fontSize = 13.sp, color = DemoColors.TextSecondary)
        }
    }
}

@Composable
private fun TodoCard(
    avatarLabel: String,
    title: String,
    subtitle: String,
    badge: String? = null,
    badgeColor: Color = DemoColors.Accent,
    avatarTint: Color = DemoColors.Accent.copy(alpha = 0.12f),
    avatarFg: Color = DemoColors.Accent,
    footer: (@Composable () -> Unit)? = null,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(12.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .width(44.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(avatarTint),
                contentAlignment = Alignment.Center,
            ) {
                Text(avatarLabel.take(1), color = avatarFg, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, fontSize = 13.sp, color = DemoColors.TextSecondary)
            }
            if (badge != null) {
                Text(
                    badge,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = badgeColor,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(badgeColor.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }
        if (footer != null) {
            Spacer(Modifier.height(12.dp))
            footer()
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

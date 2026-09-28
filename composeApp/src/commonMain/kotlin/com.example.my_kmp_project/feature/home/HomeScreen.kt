package com.example.my_kmp_project.feature.home

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.component.webview.OfflineWebFixtureUrl
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.platform.showPlatformToast
import com.example.my_kmp_project.core.router.AppRoute
import com.example.my_kmp_project.core.router.LocalAppNavigator
import com.example.my_kmp_project.core.ui.ReportMainTabRoot
import com.example.my_kmp_project.feature.mine.SwitchStoreDialog
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.delay
import my_kmp_project.composeapp.generated.resources.Res
import my_kmp_project.composeapp.generated.resources.home_banner_sot
import org.jetbrains.compose.resources.painterResource

/**
 * Shared home tab root (AppShell / iOS / OHOS). Layout + biz aligned to Flutter
 * `HomePage` / Android [com.example.my_kmp_project.nativeshell.JetpackHomeRoot].
 */
@Composable
internal fun HomeScreen(
    loggedIn: Boolean = false,
    displayName: String? = null,
) {
    var destination by remember { mutableStateOf<String?>(null) }
    val navigator = LocalAppNavigator.current

    when (val dest = destination) {
        null -> HomeRootContent(
            loggedIn = loggedIn,
            displayName = displayName,
            onNavigate = { route ->
                when (route) {
                    "web" -> navigator?.navigate(AppRoute.InAppWeb(OfflineWebFixtureUrl))
                    "services" -> destination = "services"
                    "media" -> navigator?.navigate(AppRoute.Media)
                    "scan" -> navigator?.navigate(AppRoute.Scan)
                    "friend" -> navigator?.navigate(AppRoute.Friend)
                    "live" -> navigator?.navigate(AppRoute.Live)
                    "classroom" -> navigator?.navigate(AppRoute.Classroom)
                    "请先登录" -> showPlatformToast("请先登录")
                    else -> destination = route
                }
            },
        )
        "search" -> HomeSearchScreen(onBack = { destination = null })
        "report" -> LearningReportScreen(onBack = { destination = null })
        "strategy" -> StrategyScreen(onBack = { destination = null })
        "services" -> AllServicesScreen(onBack = { destination = null })
        else -> {
            if (HomeRoutes.fromLabel(dest) != null || dest.startsWith("/home/")) {
                HomeRouteHost(
                    route = HomeRoutes.fromLabel(dest) ?: dest,
                    onBack = { destination = null },
                    onNavigate = { destination = it },
                )
            } else {
                AllServicesScreen(onBack = { destination = null })
            }
        }
    }
}

@Composable
private fun HomeRootContent(
    loggedIn: Boolean,
    displayName: String?,
    onNavigate: (String) -> Unit,
) {
    ReportMainTabRoot(isRoot = true)
    var metricTab by remember { mutableIntStateOf(0) }
    var showTodos by remember { mutableStateOf(false) }
    var showSwitchStore by remember { mutableStateOf(false) }
    var storeName by remember { mutableStateOf(HomeMockData.storeName) }
    var showCheckIn by remember { mutableStateOf(false) }
    var checkInAcked by remember { mutableStateOf(false) }
    val greeting = remember(displayName) { flutterStyleGreeting(displayName) }

    LaunchedEffect(Unit) {
        delay(50)
        showTodos = true
    }
    LaunchedEffect(loggedIn, checkInAcked) {
        if (!loggedIn || checkInAcked) return@LaunchedEffect
        delay(400)
        showCheckIn = true
    }

    if (showSwitchStore && loggedIn) {
        SwitchStoreDialog(
            selectedId = "1",
            onDismiss = { showSwitchStore = false },
            onPicked = { store ->
                storeName = store.name
                showSwitchStore = false
                showPlatformToast("已切换到 ${store.name}")
            },
        )
    }

    if (showCheckIn && loggedIn) {
        DailyCheckInDialog(
            todayReward = 10,
            streak = 3,
            onCheckIn = {
                checkInAcked = true
                showPlatformToast("签到成功，+10积分")
                showCheckIn = false
            },
            onDismiss = {
                checkInAcked = true
                showCheckIn = false
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DemoColors.PageBg),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item { GreetingSection(greeting = greeting) }
            item {
                HomeSearchBarRow(
                    onSearch = { onNavigate("search") },
                    onScan = { onNavigate("scan") },
                )
            }
            item { BannerSection() }
            item {
                FeatureGrid(
                    onFeature = { label ->
                        when (label) {
                            "更多" -> onNavigate("services")
                            else -> onNavigate(label)
                        }
                    },
                )
            }
            if (showTodos) {
                item {
                    QuickActionsSection(
                        onAction = { title ->
                            onNavigate(HomeRoutes.fromLabel(title) ?: title)
                        },
                    )
                }
            }
            item {
                StoreMetricsCard(
                    selectedTab = metricTab,
                    storeName = storeName,
                    onTabSelected = { metricTab = it },
                    onStoreTap = {
                        if (loggedIn) showSwitchStore = true
                        else onNavigate("请先登录")
                    },
                    onOpenLedger = { onNavigate(HomeRoutes.Ledger) },
                )
            }
            item {
                HubEntryCard(
                    title = "投资策略",
                    subtitle = "资产九宫格 · 恐贪定投 · 趋势策略",
                    onClick = { onNavigate("strategy") },
                )
            }
            item {
                ServiceGridSection(
                    onService = { label ->
                        // Flutter HomeServiceGrid: only「更多」/「全部」opens AllServices.
                        if (label == "更多" || label == "全部") onNavigate("services")
                    },
                )
            }
            item { ContactsSection() }
            item { NewsSection() }
            item {
                HubEntryCard(
                    title = "学习报告",
                    subtitle = "今日高光 · 学习记录",
                    onClick = { onNavigate("report") },
                )
            }
            item { ToolsSection(onNavigate = onNavigate) }
        }
    }
}

@OptIn(ExperimentalTime::class)
private fun flutterStyleGreeting(displayName: String?): String {
    // Demo locale ≈ UTC+8 (China); matches Flutter HomeController period buckets.
    val hour = (((Clock.System.now().toEpochMilliseconds() / 3_600_000L) + 8) % 24).toInt()
    val period = when {
        hour < 12 -> "早上好"
        hour < 18 -> "下午好"
        else -> "晚上好"
    }
    val name = displayName?.takeIf { it.isNotBlank() } ?: "访客"
    return "$period，$name"
}

@Composable
private fun GreetingSection(greeting: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = greeting,
            color = DemoColors.TextPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 28.sp,
            letterSpacing = (-1.6).sp,
            modifier = Modifier.weight(1f),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(DemoColors.Accent.copy(alpha = 0.1f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text("◌", color = DemoColors.Accent, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "3条新消息",
                color = DemoColors.Accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun HomeSearchBarRow(
    onSearch: () -> Unit,
    onScan: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(DemoColors.Background)
                .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(8.dp))
                .clickable(onClick = onSearch)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("⌕", color = DemoColors.Muted, fontSize = 18.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(HomeMockData.searchPlaceholder, color = DemoColors.Muted, fontSize = 15.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(DemoColors.Background)
                .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(8.dp))
                .clickable(onClick = onScan),
            contentAlignment = Alignment.Center,
        ) {
            Text("▣", color = DemoColors.Accent, fontSize = 18.sp)
        }
    }
}

@Composable
private fun BannerSection() {
    // Flutter HomeBannerSection: display-only. Asset includes title/CTA paint.
    Image(
        painter = painterResource(Res.drawable.home_banner_sot),
        contentDescription = "朋友圈营销",
        contentScale = ContentScale.FillBounds,
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, top = 16.dp)
            .fillMaxWidth()
            .height(132.dp)
            .clip(RoundedCornerShape(8.dp)),
    )
}

@Composable
private fun FeatureGrid(onFeature: (String) -> Unit) {
    val visible = remember { visibleHomeFeatures(HomeMockData.features) }
    Column(
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, top = 12.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DemoColors.Background)
            .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(8.dp))
            .padding(horizontal = 4.dp, vertical = 8.dp),
    ) {
        visible.chunked(5).forEachIndexed { rowIndex, row ->
            if (rowIndex > 0) Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                row.forEach { item ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onFeature(item.label) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(DemoColors.PageBg),
                            contentAlignment = Alignment.Center,
                        ) {
                            HomeAssetIcon(
                                resource = HomeServiceAssets.featureForLabel(item.label),
                                size = 44.dp,
                                contentDescription = item.label,
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            item.label,
                            fontSize = 11.sp,
                            color = DemoColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                repeat(5 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

private fun visibleHomeFeatures(items: List<HomeFeatureItem>): List<HomeFeatureItem> {
    val maxItems = 9
    val more = items.filter { it.label == "更多" }
    val head = items.filter { it.label != "更多" }
        .take(if (more.isEmpty()) maxItems else maxItems - 1)
    return if (more.isEmpty()) head else head + more.last()
}

@Composable
private fun QuickActionsSection(onAction: (String) -> Unit) {
    val cards = HomeMockData.quickActions
    if (cards.isEmpty()) return
    Column(
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, top = 12.dp)
            .fillMaxWidth(),
    ) {
        cards.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { action ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(DemoColors.Background)
                            .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(10.dp))
                            .clickable { onAction(action.title) }
                            .padding(12.dp),
                    ) {
                        Text(
                            action.title,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = DemoColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            action.subtitle,
                            fontSize = 12.sp,
                            color = DemoColors.TextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            action.actionLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = DemoColors.Accent,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(DemoColors.Accent.copy(alpha = 0.1f))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StoreMetricsCard(
    selectedTab: Int,
    storeName: String,
    onTabSelected: (Int) -> Unit,
    onStoreTap: () -> Unit,
    onOpenLedger: () -> Unit,
) {
    val tabs = listOf("今日", "昨日", "近30天")
    val metrics = when (selectedTab) {
        1 -> HomeMockData.metricsYesterday
        2 -> HomeMockData.metricsMonth
        else -> HomeMockData.metricsToday
    }
    Column(
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, top = 16.dp)
            .fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "公司数据",
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                color = DemoColors.TextPrimary,
            )
            Spacer(modifier = Modifier.weight(1f))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(onClick = onOpenLedger),
            ) {
                Text(
                    "查看更多",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = DemoColors.Accent,
                )
                Text("›", fontSize = 16.sp, color = DemoColors.Accent)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(DemoColors.Background)
                .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(8.dp))
                .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 16.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DemoColors.PageBg)
                    .clickable(onClick = onStoreTap)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DemoColors.Accent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("店", color = DemoColors.Accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    storeName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DemoColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text("▾", color = DemoColors.TextSecondary, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(2.dp))
                Text("⇄", color = DemoColors.Muted, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DemoColors.PageBg)
                    .padding(4.dp),
            ) {
                tabs.forEachIndexed { i, t ->
                    val sel = i == selectedTab
                    Text(
                        t,
                        fontSize = 13.sp,
                        fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (sel) DemoColors.TextPrimary else DemoColors.TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (sel) DemoColors.Background else Color.Transparent)
                            .clickable { onTabSelected(i) }
                            .padding(vertical = 8.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            metrics.chunked(2).forEachIndexed { rowIndex, row ->
                if (rowIndex > 0) Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    row.forEach { m ->
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(DemoColors.PageBg)
                                .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                        ) {
                            Text(
                                m.value,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 22.sp,
                                color = DemoColors.TextPrimary,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(m.label, fontSize = 12.sp, color = DemoColors.TextSecondary)
                        }
                    }
                    if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }
            if (HomeMockData.metricDetails.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = DemoColors.Divider, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    HomeMockData.metricDetails.forEach { detail ->
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(DemoColors.Accent.copy(alpha = 0.06f))
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                        ) {
                            Text(
                                detail.value,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                color = DemoColors.Accent,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(detail.label, fontSize = 11.sp, color = DemoColors.TextSecondary)
                            Text("详情", fontSize = 11.sp, color = DemoColors.Accent, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HubEntryCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(DemoColors.Background)
            .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DemoColors.Accent.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = title.take(1),
                color = DemoColors.Accent,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = DemoColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
            )
            Text(
                text = subtitle,
                color = DemoColors.TextSecondary,
                fontSize = 13.sp,
            )
        }
        Text(text = "›", color = DemoColors.TextSecondary, fontSize = 22.sp)
    }
}

@Composable
private fun ServiceGridSection(onService: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "服务推荐",
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                color = DemoColors.TextPrimary,
                modifier = Modifier.weight(1f),
            )
            Text(
                "全部",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = DemoColors.Accent,
                modifier = Modifier.clickable { onService("全部") },
            )
            Text("›", fontSize = 16.sp, color = DemoColors.Accent)
        }
        HomeMockData.services.chunked(4).forEachIndexed { rowIndex, row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                row.forEachIndexed { colIndex, item ->
                    val index = rowIndex * 4 + colIndex
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onService(item.label) }
                            .padding(vertical = 8.dp),
                    ) {
                        Box {
                            HomeAssetIcon(
                                resource = HomeServiceAssets.serviceAt(index),
                                size = 44.dp,
                                contentDescription = item.label,
                            )
                            if (item.badge != null) {
                                Text(
                                    text = item.badge,
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(DemoColors.Danger)
                                        .padding(horizontal = 4.dp, vertical = 1.dp),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = item.label,
                            color = DemoColors.TextPrimary,
                            fontSize = 12.sp,
                        )
                    }
                }
                repeat(4 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun ContactsSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(DemoColors.Background)
            .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(12.dp)),
    ) {
        Text(
            text = "联系汽车之家",
            color = DemoColors.TextPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp,
            modifier = Modifier.padding(16.dp),
        )
        HomeMockData.contacts.forEachIndexed { index, contact ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(DemoColors.Accent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = contact.title.take(1),
                        color = DemoColors.Accent,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = contact.title,
                        color = DemoColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                    )
                    Text(
                        text = contact.subtitle,
                        color = DemoColors.TextSecondary,
                        fontSize = 12.sp,
                    )
                }
                if (contact.trailing != null) {
                    Text(
                        text = contact.trailing,
                        color = DemoColors.Accent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            if (index < HomeMockData.contacts.lastIndex) {
                HorizontalDivider(
                    color = DemoColors.Divider,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun NewsSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp),
    ) {
        Text(
            text = "行业动态",
            color = DemoColors.TextPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp,
        )
        Spacer(modifier = Modifier.height(10.dp))
        HomeMockData.news.forEach { news ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DemoColors.Background)
                    .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(12.dp))
                    .padding(14.dp),
            ) {
                Text(
                    text = news.title,
                    color = DemoColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${news.source} · ${news.date}",
                    color = DemoColors.TextSecondary,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun ToolsSection(onNavigate: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp),
    ) {
        Text(
            text = "更多工具",
            color = DemoColors.TextPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            HomeMockData.toolEntries.forEach { (item, dest) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(72.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DemoColors.Background)
                        .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(12.dp))
                        .clickable { onNavigate(dest) }
                        .padding(vertical = 12.dp),
                ) {
                    Text(
                        text = item.label.take(1),
                        color = DemoColors.Accent,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = item.label,
                        color = DemoColors.TextPrimary,
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}

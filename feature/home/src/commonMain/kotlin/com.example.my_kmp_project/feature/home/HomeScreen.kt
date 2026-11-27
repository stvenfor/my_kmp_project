package com.example.my_kmp_project.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.my_kmp_project.component.routing.CrossFeatureScreens
import com.example.my_kmp_project.component.webview.OfflineWebFixtureUrl
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.platform.showPlatformToast
import com.example.my_kmp_project.core.router.AppRoute
import com.example.my_kmp_project.core.router.HomeRoutes
import com.example.my_kmp_project.core.router.LocalAppNavigator
import com.example.my_kmp_project.core.router.ProductRouteDispatch
import com.example.my_kmp_project.core.router.ProductRouteHost
import com.example.my_kmp_project.core.ui.ReportMainTabRoot
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Shared home tab root (AppShell / iOS / OHOS). Layout + biz aligned to Flutter
 * `HomePage` / Android [com.example.my_kmp_project.nativeshell.JetpackHomeRoot].
 */
@Composable
fun HomeScreen(
    dashboard: HomeDashboardStore,
    content: HomeContentStore,
    loggedIn: Boolean = false,
    displayName: String? = null,
) {
    var destination by remember { mutableStateOf<String?>(null) }
    val navigator = LocalAppNavigator.current

    fun go(raw: String) {
        val target = ProductRouteDispatch.resolve(raw) ?: return
        when (target.host) {
            ProductRouteHost.Toast -> showPlatformToast(target.toastMessage ?: raw)
            ProductRouteHost.Web -> navigator?.navigate(AppRoute.InAppWeb(OfflineWebFixtureUrl))
            ProductRouteHost.Scan -> navigator?.navigate(AppRoute.Scan)
            ProductRouteHost.Content -> when (target.path) {
                "/friend" -> navigator?.navigate(AppRoute.Friend)
                "/live", "/live/room" -> navigator?.navigate(AppRoute.Live)
                "/classroom/my_class", "/classroom" -> navigator?.navigate(AppRoute.Classroom)
                "/media/entry" -> navigator?.navigate(AppRoute.Media)
                else -> destination = target.path
            }
            ProductRouteHost.AllServices -> destination = "services"
            ProductRouteHost.Home, ProductRouteHost.Mine, ProductRouteHost.Community ->
                destination = target.path
            ProductRouteHost.Tab -> showPlatformToast("请切换底部 Tab")
            ProductRouteHost.Auth -> showPlatformToast("请先登录")
            ProductRouteHost.Unknown -> destination = raw
        }
    }

    when (val dest = destination) {
        null -> HomeRootContent(
            loggedIn = loggedIn,
            displayName = displayName,
            dashboard = dashboard,
            onNavigate = ::go,
        )
        "services" -> AllServicesScreen(
            onBack = { destination = null },
            content = content,
            onOpen = { label ->
                destination = null
                go(label)
            },
        )
        else -> {
            when (ProductRouteDispatch.hostForPath(dest)) {
                ProductRouteHost.Home -> HomeRouteHost(
                    route = dest,
                    onBack = { destination = null },
                    content = content,
                    onNavigate = { go(it) },
                )
                ProductRouteHost.Content -> CrossFeatureScreens.ContentRouteScreen(
                    route = dest,
                    onBack = { destination = null },
                    onNavigate = { go(it) },
                )
                ProductRouteHost.Mine -> CrossFeatureScreens.MineRouteScreen(
                    route = dest,
                    onBack = { destination = null },
                    onNavigate = { go(it) },
                )
                else -> HomeRouteHost(
                    route = dest,
                    onBack = { destination = null },
                    content = content,
                    onNavigate = { go(it) },
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalTime::class)
private fun HomeRootContent(
    loggedIn: Boolean,
    displayName: String?,
    dashboard: HomeDashboardStore,
    onNavigate: (String) -> Unit,
) {
    ReportMainTabRoot(isRoot = true)
    var showTodos by remember { mutableStateOf(false) }
    var showSwitchStore by remember { mutableStateOf(false) }
    var showCheckIn by remember { mutableStateOf(false) }
    var metricTab by remember { mutableStateOf(0) }
    val greeting = remember(displayName) { flutterStyleGreeting(displayName) }
    // Read for recomposition when points / dashboard / dialog ack change.
    HomePointsStore.version
    dashboard.version
    val scope = rememberCoroutineScope()

    LaunchedEffect(loggedIn) {
        if (loggedIn) {
            HomePointsStore.refresh()
            HomeTodoStore.refresh()
            dashboard.refresh()
        }
    }

    LaunchedEffect(Unit) {
        delay(50)
        showTodos = true
    }
    LaunchedEffect(loggedIn, HomePointsStore.checkedInToday, HomePointsStore.dialogAckDate) {
        val today = run {
            @OptIn(ExperimentalTime::class)
            val now = Clock.System.now().toString().take(10) // YYYY-MM-DD from Instant ISO
            now
        }
        // Prefer calendar-like yyyy-mm-dd; Instant.toString is ISO-8601 starting with date.
        val should = HomePointsStore.shouldShowDialog(
            isLoggedIn = loggedIn,
            checkedInToday = HomePointsStore.checkedInToday,
            ackDate = HomePointsStore.dialogAckDate,
            todayLocal = today,
        )
        if (!should) return@LaunchedEffect
        delay(400)
        showCheckIn = true
    }

    if (showSwitchStore && loggedIn) {
        CrossFeatureScreens.SwitchStoreDialog(
            selectedId = "1",
            onDismiss = { showSwitchStore = false },
            onPicked = { _, name ->
                showSwitchStore = false
                showPlatformToast("已切换到 $name")
                // 切换后重新拉取看板，门店名/统计以服务端为准。
                scope.launch { dashboard.refresh() }
            },
        )
    }

    if (showCheckIn && loggedIn) {
        @OptIn(ExperimentalTime::class)
        val today = Clock.System.now().toString().take(10)
        DailyCheckInDialog(
            todayReward = HomePointsStore.todayReward,
            streak = HomePointsStore.streak,
            onCheckIn = {
                scope.launch {
                    val res = HomePointsStore.checkIn()
                    HomePointsStore.markDialogAcked(today)
                    if (res != null) {
                        showPlatformToast("签到成功，+${res.points}积分")
                    } else {
                        showPlatformToast(HomePointsStore.errorMessage ?: "签到失败")
                    }
                    showCheckIn = false
                }
            },
            onDismiss = {
                HomePointsStore.markDialogAcked(today)
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
            // Flutter: AppPageScaffold(mainTabRoot) 出安全区 + ListView(top = safeTop + 16)。
            // 这里把安全区交给 statusBarsPadding，16 交给 contentPadding，
            // 首个 section（Greeting）自身再带 24 → 与 Flutter 的 safeTop+40 一致。
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
        ) {
            item { HomeGreetingSection(greeting = greeting) }
            item {
                HomeSearchBar(
                    placeholder = HomeSearchPlaceholder,
                    onSearch = { onNavigate(HomeRoutes.Search) },
                    onScan = { onNavigate("扫一扫") },
                )
            }
            item { HomeBannerSection() }
            val features = dashboard.features()
            if (features.isNotEmpty()) {
                item {
                    HomeFeatureGrid(
                        items = features,
                        onFeature = { label -> onNavigate(label) },
                    )
                }
            }
            if (showTodos) {
                item {
                    HomeTodoCardStrip(
                        onOpen = { card ->
                            onNavigate(
                                card.actionRoute.ifBlank {
                                    HomeRoutes.fromLabel(card.title) ?: card.title
                                },
                            )
                        },
                    )
                }
            }
            item {
                HomeStoreMetricsCard(
                    storeName = dashboard.storeName,
                    selectedTab = metricTab,
                    tabs = DashboardMetricTabs,
                    metrics = dashboard.metrics(metricTab),
                    details = emptyList(),
                    onTabSelected = { metricTab = it },
                    onStoreTap = {
                        if (loggedIn) showSwitchStore = true
                        else onNavigate("请先登录")
                    },
                    onOpenLedger = { onNavigate(HomeRoutes.Ledger) },
                )
            }
            item {
                HomeHubEntryCard(
                    title = "投资策略",
                    subtitle = "资产九宫格 · 恐贪定投 · 趋势策略",
                    icon = HomeIcons.GridView,
                    onClick = { onNavigate(HomeRoutes.Strategy) },
                )
            }
            val services = dashboard.services()
            if (services.isNotEmpty()) {
                item {
                    HomeServiceGrid(
                        items = services,
                        // Flutter HomeServiceGrid: 只有「更多」/「全部」跳全部服务。
                        onService = { label ->
                            if (label == "更多" || label == "全部") onNavigate(HomeRoutes.AllServices)
                        },
                        onOpenAll = { onNavigate(HomeRoutes.AllServices) },
                    )
                }
            }
            val contacts = dashboard.contacts()
            if (contacts.isNotEmpty()) {
                item { HomeContactList(items = contacts, onContact = {}) }
            }
            val news = dashboard.news()
            if (news.isNotEmpty()) {
                item { HomeNewsList(items = news, onOpenAll = {}) }
            }
            item {
                HomeHubEntryCard(
                    title = "学习报告",
                    subtitle = "今日高光 · 学习记录",
                    icon = HomeIcons.Analytics,
                    onClick = { onNavigate(HomeRoutes.LearningReport) },
                )
            }
            // Flutter HomePage ends at LearningReport — no「更多工具」row.
        }
    }
}

/** 公司数据三档：今日 / 昨日 / 本月（wys_home_metrics.period）。 */
internal val DashboardMetricTabs = listOf("今日", "昨日", "本月")

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

package com.example.my_kmp_project.nativeshell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.my_kmp_project.app.AppContainer
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.platform.showPlatformToast
import com.example.my_kmp_project.feature.home.DailyCheckInDialog
import com.example.my_kmp_project.feature.home.HomeBannerSection
import com.example.my_kmp_project.feature.home.HomeContactList
import com.example.my_kmp_project.feature.home.HomeDashboardStore
import com.example.my_kmp_project.feature.home.HomeFeatureGrid
import com.example.my_kmp_project.feature.home.HomeGreetingSection
import com.example.my_kmp_project.feature.home.HomeHubEntryCard
import com.example.my_kmp_project.feature.home.HomeIcons
import com.example.my_kmp_project.feature.home.HomePointsStore
import com.example.my_kmp_project.feature.home.HomeSearchPlaceholder
import com.example.my_kmp_project.feature.home.HomeTodoStore
import com.example.my_kmp_project.feature.home.HomeNewsList
import com.example.my_kmp_project.feature.home.HomeSearchBar
import com.example.my_kmp_project.feature.home.HomeServiceGrid
import com.example.my_kmp_project.feature.home.HomeStoreMetricsCard
import com.example.my_kmp_project.feature.home.HomeTodoCardStrip
import com.example.my_kmp_project.feature.mine.MineStoreOption
import com.example.my_kmp_project.feature.mine.SwitchStoreDialog
import com.example.my_kmp_project.feature.mine.profile.MineAccountController
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Android 原生壳首页宿主（Jetpack Compose）。
 *
 * **只保留宿主职责**：安全区、下拉刷新、签到弹窗（Android 侧用 SharedPreferences
 * 持久化 ack 日期）、门店切换弹窗、以及把各区块串进 LazyColumn。
 * 所有区块的视觉实现统一在 `feature/home/HomeSections.kt`，与共享
 * `HomeScreen` 用的是同一套 composable —— 改前这里另有一份逐行重复的
 * `JetpackHomeBanner` / `JetpackFeatureGrid` / `JetpackStoreMetrics` 等，
 * 视觉修正必然双份漂移。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun JetpackHomeRoot(
    onDeferred: (String) -> Unit,
    dashboard: HomeDashboardStore,
    displayName: String? = null,
    loggedIn: Boolean = false,
) {
    val greeting = remember(displayName) { flutterStyleGreeting(displayName) }
    var refreshing by remember { mutableStateOf(false) }
    // Flutter SoT: empty todoCards → strip hidden.
    var showTodos by remember { mutableStateOf(false) }
    var showSwitchStore by remember { mutableStateOf(false) }
    var showCheckIn by remember { mutableStateOf(false) }
    var metricTab by remember { mutableStateOf(0) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val account = remember { MineAccountController(AppContainer.get().network.api()) }
    val refreshState = rememberPullToRefreshState()
    // 读取看板缓存版本，数据更新后触发重组。
    dashboard.version

    LaunchedEffect(loggedIn) {
        if (!loggedIn) return@LaunchedEffect
        // 真实积分状态、待办与首页看板（门店名/宫格/客户/资讯）。
        HomePointsStore.refresh()
        HomeTodoStore.refresh()
        dashboard.refresh()
        account.loadStores()
        showTodos = HomeTodoStore.todoCards().isNotEmpty()
        val prefs = context.getSharedPreferences("home_check_in", 0)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val ack = prefs.getString("check_in_dialog_ack_date", null)
        // 已签（服务端 checkedInToday）或今日已 ack 则不再弹。
        if (ack != today && !HomePointsStore.checkedInToday) showCheckIn = true
    }

    if (showSwitchStore && loggedIn) {
        SwitchStoreDialog(
            selectedId = "1",
            stores = account.stores.map { MineStoreOption(it.storeId.toString(), it.storeName) },
            loading = account.storesLoading,
            onDismiss = { showSwitchStore = false },
            onPicked = { store ->
                val id = store.id.toIntOrNull()
                if (id == null) {
                    showSwitchStore = false
                } else {
                    scope.launch {
                        account.switchStore(id).fold(
                            onSuccess = {
                                showSwitchStore = false
                                // 切换后重新拉取看板，门店名/统计以服务端为准。
                                dashboard.refresh()
                            },
                            onFailure = { showPlatformToast(it.message ?: "切换门店失败") },
                        )
                    }
                }
            },
        )
    }

    if (showCheckIn && loggedIn) {
        DailyCheckInDialog(
            todayReward = HomePointsStore.todayReward,
            streak = HomePointsStore.streak,
            onCheckIn = {
                scope.launch {
                    val res = HomePointsStore.checkIn()
                    markCheckInAcked(context)
                    if (res != null) {
                        showPlatformToast("签到成功，+${res.points}积分")
                    } else {
                        showPlatformToast(HomePointsStore.errorMessage ?: "签到失败")
                    }
                    showCheckIn = false
                }
            },
            onDismiss = {
                markCheckInAcked(context)
                showCheckIn = false
            },
        )
    }

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = {
            scope.launch {
                refreshing = true
                HomePointsStore.refresh()
                HomeTodoStore.refresh()
                dashboard.refresh()
                // 待办条：真实 HomeTodoStore 为空则隐藏。
                showTodos = HomeTodoStore.todoCards().isNotEmpty()
                refreshing = false
            }
        },
        state = refreshState,
        modifier = Modifier
            .fillMaxSize()
            .background(DemoColors.PageBg)
            .statusBarsPadding(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            // 与 Flutter ListView(top = safeTop + 16) 对齐：safeTop 由外层
            // statusBarsPadding 提供，16 在这里，首块 Greeting 自带 24。
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
        ) {
            item { HomeGreetingSection(greeting = greeting) }
            item {
                HomeSearchBar(
                    placeholder = HomeSearchPlaceholder,
                    onSearch = { onDeferred("搜索") },
                    onScan = { onDeferred("扫一扫") },
                )
            }
            item { HomeBannerSection() }
            val features = dashboard.features()
            if (features.isNotEmpty()) {
                item {
                    HomeFeatureGrid(
                        items = features,
                        onFeature = { label ->
                            if (label == "更多") onDeferred("全部服务") else onDeferred(label)
                        },
                    )
                }
            }
            if (showTodos) {
                item {
                    HomeTodoCardStrip(onOpen = { card -> onDeferred(card.title) })
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
                        if (loggedIn) showSwitchStore = true else onDeferred("请先登录")
                    },
                    onOpenLedger = { onDeferred("台账") },
                )
            }
            item {
                HomeHubEntryCard(
                    title = "投资策略",
                    subtitle = "资产九宫格 · 恐贪定投 · 趋势策略",
                    icon = HomeIcons.GridView,
                    onClick = { onDeferred("投资策略") },
                )
            }
            val services = dashboard.services()
            if (services.isNotEmpty()) {
                item {
                    HomeServiceGrid(
                        items = services,
                        onService = { label ->
                            if (label == "更多" || label == "全部") onDeferred("全部服务")
                        },
                        onOpenAll = { onDeferred("全部服务") },
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
                    onClick = { onDeferred("学习报告") },
                )
            }
        }
    }
}

/** 公司数据三档：今日 / 昨日 / 本月（wys_home_metrics.period）。 */
private val DashboardMetricTabs = listOf("今日", "昨日", "本月")

private fun markCheckInAcked(context: android.content.Context) {
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    context.getSharedPreferences("home_check_in", 0)
        .edit().putString("check_in_dialog_ack_date", today).apply()
}

private fun flutterStyleGreeting(displayName: String?): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val period = when {
        hour < 12 -> "早上好"
        hour < 18 -> "下午好"
        else -> "晚上好"
    }
    val name = displayName?.takeIf { it.isNotBlank() } ?: "访客"
    return "$period，$name"
}

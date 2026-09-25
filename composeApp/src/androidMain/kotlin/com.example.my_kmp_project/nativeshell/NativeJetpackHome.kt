package com.example.my_kmp_project.nativeshell

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.feature.home.HomeAssetIcon
import com.example.my_kmp_project.feature.home.HomeFeatureItem
import com.example.my_kmp_project.feature.home.HomeMockData
import com.example.my_kmp_project.feature.home.HomeServiceAssets
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import my_kmp_project.composeapp.generated.resources.Res
import my_kmp_project.composeapp.generated.resources.home_banner_sot
import org.jetbrains.compose.resources.painterResource
import java.util.Calendar

/** Jetpack Home root — layout aligned to Flutter `HomePage` dashboard. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun JetpackHomeRoot(
    onDeferred: (String) -> Unit,
    displayName: String? = null,
) {
    var metricTab by remember { mutableIntStateOf(0) } // Flutter HomeController default: 今日
    val greeting = remember(displayName) { flutterStyleGreeting(displayName) }
    var refreshing by remember { mutableStateOf(false) }
    // Flutter SoT (live API): empty todoCards → HomeTodoCardStrip shrinks.
    var showTodos by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val refreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = {
            scope.launch {
                refreshing = true
                delay(600)
                // Flutter: todo API failure → hide strip
                showTodos = true
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
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 40.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        greeting,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 28.sp,
                        lineHeight = (28 * 36f / 32f).sp,
                        color = DemoColors.TextPrimary,
                        letterSpacing = (-1.6).sp,
                        modifier = Modifier.weight(1f),
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(DemoColors.Accent.copy(alpha = 0.1f))
                            .clickable { onDeferred("消息") }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(
                            "◌",
                            color = DemoColors.Accent,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "3条新消息",
                            color = DemoColors.Accent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
            item {
                Row(
                    Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DemoColors.Background)
                            .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(8.dp))
                            .clickable { onDeferred("搜索") }
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("⌕", color = DemoColors.Muted, fontSize = 18.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(HomeMockData.searchPlaceholder, color = DemoColors.Muted, fontSize = 15.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DemoColors.Background)
                            .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(8.dp))
                            .clickable { onDeferred("扫一扫") },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("▣", color = DemoColors.Accent, fontSize = 18.sp)
                    }
                }
            }
            item { JetpackHomeBanner(onDeferred) }
            item { JetpackFeatureGrid(onDeferred) }
            if (showTodos) {
                item { JetpackTodoStrip(onDeferred) }
            }
            item { JetpackStoreMetrics(metricTab, onDeferred) { metricTab = it } }
            item { JetpackStrategyEntry(onDeferred) }
            item { JetpackServiceGrid(onDeferred) }
            item { JetpackContactList(onDeferred) }
            item { JetpackNewsList() }
            item { JetpackLearningReportEntry(onDeferred) }
        }
    }
}

@Composable
private fun JetpackHomeBanner(onDeferred: (String) -> Unit) {
    // Pixel-locked crop from Flutter SoT `flutter-new/home.png` (includes overlay text/CTA).
    Image(
        painter = painterResource(Res.drawable.home_banner_sot),
        contentDescription = "朋友圈营销",
        contentScale = ContentScale.FillBounds,
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, top = 16.dp)
            .fillMaxWidth()
            .height(132.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onDeferred("朋友圈营销") },
    )
}

@Composable
private fun JetpackFeatureGrid(onDeferred: (String) -> Unit) {
    val visible = remember { visibleHomeFeatures(HomeMockData.features) }
    Column(
        Modifier
            .padding(start = 16.dp, end = 16.dp, top = 12.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DemoColors.Background)
            .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(8.dp))
            .padding(horizontal = 4.dp, vertical = 8.dp),
    ) {
        visible.chunked(5).forEachIndexed { rowIndex, row ->
            if (rowIndex > 0) Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                row.forEach { item ->
                    Column(
                        Modifier
                            .weight(1f)
                            .clickable { onFeatureTap(item.label, onDeferred) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            Modifier
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
                        Spacer(Modifier.height(4.dp))
                        Text(
                            item.label,
                            fontSize = 11.sp,
                            color = DemoColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                repeat(5 - row.size) {
                    Spacer(Modifier.weight(1f))
                }
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

private fun onFeatureTap(label: String, onDeferred: (String) -> Unit) {
    when (label) {
        "更多" -> onDeferred("全部服务")
        else -> onDeferred(label)
    }
}

@Composable
private fun JetpackStoreMetrics(
    selected: Int,
    onDeferred: (String) -> Unit,
    onSelect: (Int) -> Unit,
) {
    // Flutter HomeStoreMetricsCard + HomeController.metricTabs
    val tabs = listOf("今日", "昨日", "近30天")
    val metrics = when (selected) {
        1 -> HomeMockData.metricsYesterday
        2 -> HomeMockData.metricsMonth
        else -> HomeMockData.metricsToday
    }
    Column(
        Modifier
            .padding(start = 16.dp, end = 16.dp, top = 16.dp)
            .fillMaxWidth(),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "公司数据",
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                color = DemoColors.TextPrimary,
            )
            Spacer(Modifier.weight(1f))
            Text(
                "查看更多",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = DemoColors.Accent,
                modifier = Modifier.clickable { onDeferred("公司数据") },
            )
            Text("›", fontSize = 16.sp, color = DemoColors.Accent)
        }
        Spacer(Modifier.height(12.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(DemoColors.Background)
                .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(8.dp))
                .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 16.dp),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DemoColors.PageBg)
                    .clickable { onDeferred("门店") }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DemoColors.Accent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("店", color = DemoColors.Accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    HomeMockData.storeName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DemoColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text("▾", color = DemoColors.TextSecondary, fontSize = 14.sp)
                Spacer(Modifier.width(2.dp))
                Text("⇄", color = DemoColors.Muted, fontSize = 14.sp)
            }
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DemoColors.PageBg)
                    .padding(4.dp),
            ) {
                tabs.forEachIndexed { i, t ->
                    val sel = i == selected
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
                            .clickable { onSelect(i) }
                            .padding(vertical = 8.dp),
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            metrics.chunked(2).forEachIndexed { rowIndex, row ->
                if (rowIndex > 0) Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    row.forEach { m ->
                        Column(
                            Modifier
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
                            Spacer(Modifier.height(4.dp))
                            Text(m.label, fontSize = 12.sp, color = DemoColors.TextSecondary)
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            if (HomeMockData.metricDetails.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = DemoColors.Divider, thickness = 0.5.dp)
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    HomeMockData.metricDetails.forEach { detail ->
                        Column(
                            Modifier
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
                            Spacer(Modifier.height(2.dp))
                            Text(detail.label, fontSize = 11.sp, color = DemoColors.TextSecondary)
                            Text("详情", fontSize = 11.sp, color = DemoColors.Accent, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
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

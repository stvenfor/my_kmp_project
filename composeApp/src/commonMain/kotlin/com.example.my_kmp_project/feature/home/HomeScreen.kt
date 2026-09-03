package com.example.my_kmp_project.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.ImmersiveCenterTopAppBar
import com.example.my_kmp_project.feature.classroom.ClassroomScreen
import com.example.my_kmp_project.feature.friend.FriendScreen
import com.example.my_kmp_project.feature.live.LiveScreen
import com.example.my_kmp_project.feature.media.MediaEntryScreen
import com.example.my_kmp_project.feature.scan.ScanScreen
import com.example.my_kmp_project.feature.shell.ReportMainTabRoot
import com.example.my_kmp_project.feature.web.InAppWebScreen
import com.example.my_kmp_project.feature.web.OfflineWebFixtureUrl

@Composable
internal fun HomeScreen() {
    var destination by remember { mutableStateOf<String?>(null) }

    when (val dest = destination) {
        null -> HomeRootContent(onNavigate = { destination = it })
        "services" -> AllServicesScreen(onBack = { destination = null })
        "search" -> HomeSearchScreen(onBack = { destination = null })
        "report" -> LearningReportScreen(onBack = { destination = null })
        "strategy" -> StrategyScreen(onBack = { destination = null })
        "media" -> MediaEntryScreen(onBack = { destination = null })
        "web" -> InAppWebScreen(
            // Offline fixture: emu without outbound net still shows non-blank body.
            url = OfflineWebFixtureUrl,
            onBack = { destination = null },
        )
        "scan" -> ScanScreen(onBack = { destination = null })
        "friend" -> FriendScreen(onBack = { destination = null })
        "live" -> LiveScreen(onBack = { destination = null })
        "classroom" -> ClassroomScreen(onBack = { destination = null })
        else -> AllServicesScreen(onBack = { destination = null })
    }
}

@Composable
private fun HomeRootContent(onNavigate: (String) -> Unit) {
    ReportMainTabRoot(isRoot = true)
    var metricTab by remember { mutableStateOf(0) }
    val metricTabs = listOf("今日", "昨日", "本月")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DemoColors.PageBg),
    ) {
        ImmersiveCenterTopAppBar(
            title = {
                Text(
                    text = "首页",
                    fontWeight = FontWeight.Bold,
                    color = DemoColors.TextPrimary,
                    fontSize = 18.sp,
                )
            },
            containerColor = DemoColors.Toolbar,
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
        ) {
            item {
                GreetingSection()
            }
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
                            "直播带货" -> onNavigate("live")
                            else -> Unit
                        }
                    },
                )
            }
            item { QuickActionsSection() }
            item {
                StoreMetricsCard(
                    selectedTab = metricTab,
                    tabs = metricTabs,
                    onTabSelected = { metricTab = it },
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
                        when (label) {
                            "更多" -> onNavigate("services")
                            "直播" -> onNavigate("live")
                            else -> Unit
                        }
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
            item {
                ToolsSection(onNavigate = onNavigate)
            }
        }
    }
}

@Composable
private fun GreetingSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = HomeMockData.greeting,
            color = DemoColors.TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "3条新消息",
            color = DemoColors.Accent,
            fontSize = 12.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(DemoColors.Accent.copy(alpha = 0.1f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
        )
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
                .clip(RoundedCornerShape(12.dp))
                .background(DemoColors.Background)
                .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(12.dp))
                .clickable(onClick = onSearch)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "⌕", color = DemoColors.TextSecondary, fontSize = 18.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = HomeMockData.searchPlaceholder,
                color = DemoColors.TextSecondary,
                fontSize = 15.sp,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DemoColors.Background)
                .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(12.dp))
                .clickable(onClick = onScan),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "▦", color = DemoColors.Accent, fontSize = 18.sp)
        }
    }
}

@Composable
private fun BannerSection() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp)
            .height(132.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(DemoColors.Accent, DemoColors.Accent.copy(alpha = 0.55f)),
                ),
            )
            .padding(20.dp),
    ) {
        Column {
            Text(
                text = "朋友圈营销",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "一键分享，高效触达客户",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 14.sp,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "立即体验",
                color = DemoColors.Accent,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun FeatureGrid(onFeature: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp),
    ) {
        HomeMockData.features.chunked(5).forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                row.forEachIndexed { colIndex, item ->
                    val index = rowIndex * 5 + colIndex
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onFeature(item.label) }
                            .padding(vertical = 8.dp),
                    ) {
                        HomeAssetIcon(
                            resource = HomeServiceAssets.featureAt(index),
                            size = 44.dp,
                            contentDescription = item.label,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = item.label,
                            color = DemoColors.TextPrimary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                repeat(5 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun QuickActionsSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        HomeMockData.quickActions.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                row.forEach { action ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DemoColors.Background)
                            .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(12.dp))
                            .padding(14.dp),
                    ) {
                        Text(
                            text = action.title,
                            color = DemoColors.TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = action.subtitle,
                            color = DemoColors.TextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = action.actionLabel,
                            color = DemoColors.Accent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
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
    tabs: List<String>,
    onTabSelected: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(DemoColors.Background)
            .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(12.dp))
            .padding(16.dp),
    ) {
        Text(
            text = HomeMockData.storeName,
            color = DemoColors.TextPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            tabs.forEachIndexed { index, label ->
                val active = index == selectedTab
                Text(
                    text = label,
                    color = if (active) DemoColors.Accent else DemoColors.TextSecondary,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = 14.sp,
                    modifier = Modifier.clickable { onTabSelected(index) },
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            HomeMockData.metricsToday.forEach { metric ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = metric.value,
                        color = DemoColors.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = metric.label,
                        color = DemoColors.TextSecondary,
                        fontSize = 11.sp,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = DemoColors.Divider)
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            HomeMockData.metricDetails.forEach { detail ->
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = detail.value,
                        color = DemoColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                    )
                    Text(
                        text = "${detail.label} 详情 >",
                        color = DemoColors.TextSecondary,
                        fontSize = 12.sp,
                    )
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
        Text(
            text = "营销服务",
            color = DemoColors.TextPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp,
        )
        Spacer(modifier = Modifier.height(12.dp))
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
                repeat(4 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
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
            text = "联系与咨询",
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
            text = "行业资讯",
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

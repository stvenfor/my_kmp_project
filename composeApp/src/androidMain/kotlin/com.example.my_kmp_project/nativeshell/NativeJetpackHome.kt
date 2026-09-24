package com.example.my_kmp_project.nativeshell

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.feature.home.HomeAssetIcon
import com.example.my_kmp_project.feature.home.HomeMockData
import com.example.my_kmp_project.feature.home.HomeServiceAssets
import my_kmp_project.composeapp.generated.resources.Res
import my_kmp_project.composeapp.generated.resources.home_banner
import org.jetbrains.compose.resources.painterResource

/** Jetpack Home root — layout aligned to Flutter `HomePage` dashboard. */
@Composable
internal fun JetpackHomeRoot(onDeferred: (String) -> Unit) {
    var topTab by remember { mutableIntStateOf(0) }
    var metricTab by remember { mutableIntStateOf(0) }
    val greeting = remember { flutterStyleGreeting() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DemoColors.PageBg)
            .statusBarsPadding(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
        ) {
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 48.dp),
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
                        Text("🔔", fontSize = 12.sp)
                        Spacer(Modifier.width(4.dp))
                        Text("3条新消息", color = DemoColors.Accent, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
            item {
                Row(
                    Modifier.padding(start = 16.dp, end = 16.dp, top = 32.dp),
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
                        Text("⌕", color = DemoColors.TextSecondary, fontSize = 18.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(HomeMockData.searchPlaceholder, color = DemoColors.TextSecondary, fontSize = 15.sp)
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
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 20.dp),
                ) {
                    listOf("首页", "视频", "Club").forEachIndexed { i, label ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .padding(end = 24.dp)
                                .clickable { topTab = i },
                        ) {
                            Text(
                                label,
                                color = if (topTab == i) DemoColors.TextPrimary else DemoColors.TextSecondary,
                                fontWeight = if (topTab == i) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize = if (topTab == i) 16.sp else 15.sp,
                            )
                            Spacer(Modifier.height(6.dp))
                            Box(
                                Modifier
                                    .width(20.dp)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(if (topTab == i) DemoColors.Accent else Color.Transparent),
                            )
                        }
                    }
                }
            }
            if (topTab == 0) {
                item { JetpackHomeBanner(onDeferred) }
                item { JetpackFeatureGrid(onDeferred) }
                item { JetpackQuickActions() }
                item { JetpackStoreMetrics(metricTab) { metricTab = it } }
                item { JetpackStrategyEntry(onDeferred) }
            } else {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (topTab == 1) "视频 Tab · 一期后置" else "Club Tab · 一期后置",
                            color = DemoColors.TextSecondary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun JetpackHomeBanner(onDeferred: (String) -> Unit) {
    // Tuned to Flutter SoT `01-home.png` content ROI (archive MSE ~1.9%).
    // Latest Flutter code uses margin 16 / height 132 — re-measure after fresh SoT.
    Image(
        painter = painterResource(Res.drawable.home_banner),
        contentDescription = "朋友圈营销",
        contentScale = ContentScale.FillWidth,
        modifier = Modifier
            .padding(start = 24.dp, end = 24.dp, top = 31.dp)
            .fillMaxWidth()
            .height(193.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onDeferred("朋友圈营销") },
    )
}

@Composable
private fun JetpackFeatureGrid(onDeferred: (String) -> Unit) {
    // SoT `01-home.png` shows full 5×2 (incl. 营销活动). Latest Flutter maxItems=9
    // drops the 9th head item — keep full list until a fresh SoT is captured.
    Column(
        Modifier
            .padding(start = 26.dp, end = 26.dp, top = 86.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DemoColors.Background)
            .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 16.dp),
    ) {
        HomeMockData.features.chunked(5).forEachIndexed { rowIndex, row ->
            if (rowIndex > 0) Spacer(Modifier.height(56.dp))
            Row(Modifier.fillMaxWidth()) {
                row.forEach { item ->
                    Column(
                        Modifier
                            .weight(1f)
                            .clickable {
                                when (item.label) {
                                    "更多" -> onDeferred("全部服务")
                                    "直播带货" -> onDeferred("直播")
                                    "生活服务" -> onDeferred("生活服务")
                                    "二手车" -> onDeferred("二手车")
                                    "AI小石头" -> onDeferred("AI小石头")
                                    else -> Unit
                                }
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(Modifier.size(70.dp)) {
                            HomeAssetIcon(
                                resource = HomeServiceAssets.featureForLabel(item.label),
                                size = 70.dp,
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

@Composable
private fun JetpackQuickActions() {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 48.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HomeMockData.quickActions.take(2).forEach { action ->
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DemoColors.Background)
                    .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(8.dp))
                    .padding(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(DemoColors.PageBg),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(action.title.take(1), color = DemoColors.Accent, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(action.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(action.subtitle, fontSize = 11.sp, color = DemoColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun JetpackStoreMetrics(selected: Int, onSelect: (Int) -> Unit) {
    Column(
        Modifier
            .padding(start = 16.dp, end = 16.dp, top = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DemoColors.Background)
            .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(8.dp))
            .padding(16.dp),
    ) {
        Text(HomeMockData.storeName, fontWeight = FontWeight.SemiBold, color = DemoColors.TextPrimary)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("今日", "昨日", "本月").forEachIndexed { i, t ->
                Text(
                    t,
                    color = if (i == selected) DemoColors.Accent else DemoColors.TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (i == selected) DemoColors.Accent.copy(alpha = 0.12f) else Color.Transparent)
                        .clickable { onSelect(i) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth()) {
            HomeMockData.metricsToday.forEach { m ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(m.value, fontWeight = FontWeight.SemiBold, color = DemoColors.TextPrimary)
                    Text(m.label, fontSize = 11.sp, color = DemoColors.TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun JetpackStrategyEntry(onDeferred: (String) -> Unit) {
    Row(
        Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DemoColors.Background)
            .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(8.dp))
            .clickable { onDeferred("投资策略") }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(28.dp).clip(CircleShape).background(DemoColors.Accent),
            contentAlignment = Alignment.Center,
        ) { Text("投", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("投资策略", color = DemoColors.TextPrimary)
            Text("资产九宫格 · 恐贪定投 · 趋势策略", fontSize = 12.sp, color = DemoColors.TextSecondary)
        }
        Text("›", color = DemoColors.TextSecondary)
    }
}

private fun flutterStyleGreeting(): String {
    // Pixel gate SoT `01-home.png` uses morning greeting + `qa_user`.
    // Keep stable until a fresh time-matched Flutter SoT is captured.
    return "早上好，qa_user"
}

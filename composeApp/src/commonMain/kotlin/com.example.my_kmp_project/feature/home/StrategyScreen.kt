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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.MineTopBar
import com.example.my_kmp_project.feature.shell.ReportMainTabRoot

private val GainRed = Color(0xFFFF3B30)
private val GainGreen = Color(0xFF34C759)

@Composable
internal fun StrategyScreen(onBack: () -> Unit) {
    ReportMainTabRoot(isRoot = false)
    var selectedTab by remember { mutableStateOf(0) }
    var periodIndex by remember { mutableStateOf(4) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DemoColors.PageBg),
    ) {
        MineTopBar(title = "策略", onBack = onBack, containerColor = DemoColors.PageBg)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 24.dp),
        ) {
            StrategySubTabs(
                tabs = HomeMockData.strategyTabs,
                selectedIndex = selectedTab,
                onSelected = { selectedTab = it },
            )
            Spacer(modifier = Modifier.height(16.dp))
            AssetGridCard(
                periodIndex = periodIndex,
                onPeriodSelected = { periodIndex = it },
            )
            Spacer(modifier = Modifier.height(16.dp))
            StrategyPlanCard(tabLabel = HomeMockData.strategyTabs.getOrElse(selectedTab) { "推荐" })
        }
    }
}

@Composable
private fun StrategySubTabs(
    tabs: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        tabs.forEachIndexed { index, label ->
            val active = index == selectedIndex
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onSelected(index) }
                    .padding(horizontal = 16.dp),
            ) {
                Text(
                    text = label,
                    color = if (active) DemoColors.TextPrimary else DemoColors.TextSecondary,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = 16.sp,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .width(if (active) 24.dp else 0.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(DemoColors.Accent),
                )
            }
        }
    }
}

@Composable
private fun AssetGridCard(
    periodIndex: Int,
    onPeriodSelected: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DemoColors.Background)
            .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(12.dp))
            .padding(16.dp),
    ) {
        Text(
            text = "「大类资产九宫格策略」通过分散配置降低波动，帮助你在不同市场环境下保持稳健收益。",
            color = DemoColors.TextPrimary,
            fontSize = 13.sp,
            lineHeight = 20.sp,
        )
        Spacer(modifier = Modifier.height(16.dp))
        val rows = HomeMockData.strategyAssets.chunked(3)
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { cell ->
                    AssetCell(
                        cell = cell,
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(3 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            HomeMockData.strategyPeriods.forEachIndexed { index, label ->
                val active = index == periodIndex
                Text(
                    text = label,
                    color = if (active) DemoColors.Accent else DemoColors.TextSecondary,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable { onPeriodSelected(index) },
                )
            }
        }
    }
}

@Composable
private fun AssetCell(
    cell: StrategyAssetCell,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .aspectRatio(1.35f)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (cell.positive) GainRed.copy(alpha = 0.08f) else GainGreen.copy(alpha = 0.08f),
            )
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = cell.label, color = DemoColors.TextPrimary, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = cell.value,
            color = if (cell.positive) GainRed else GainGreen,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun StrategyPlanCard(tabLabel: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DemoColors.Background)
            .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(12.dp))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "黄金恐贪定投 · 第一期",
                    color = DemoColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = tabLabel,
                    color = DemoColors.Accent,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(DemoColors.Accent.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
            Text(
                text = "如何跟投",
                color = DemoColors.Accent,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Column {
                Text(
                    text = "-11.35%",
                    color = GainGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                )
                Text(text = "本期收益率", color = DemoColors.TextSecondary, fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "恐贪指数", color = DemoColors.TextSecondary, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "63 中立",
                    color = DemoColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(text = "定投进度", color = DemoColors.TextSecondary, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Box(contentAlignment = Alignment.Center) {
            LinearProgressIndicator(
                progress = { 36f / 50f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .clip(RoundedCornerShape(6.dp)),
                color = DemoColors.Accent,
                trackColor = DemoColors.PageBg,
            )
            Text(
                text = "36 / 50",
                color = DemoColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "本周已投 1 份",
                color = DemoColors.TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "订阅",
                color = DemoColors.OnPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(DemoColors.Accent)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "在恐慌时买入、贪婪时卖出，通过定期定额降低择时压力，适合长期持有的投资者。",
            color = DemoColors.TextSecondary,
            fontSize = 13.sp,
            lineHeight = 20.sp,
        )
    }
}

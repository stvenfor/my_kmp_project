package com.example.my_kmp_project.feature.home

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.ui.ReportMainTabRoot

private val GainRed = Color(0xFFFF3B30)
private val GainGreen = Color(0xFF34C759)
private val GaugeGreen = Color(0xFF34C759)
private val GaugeRed = Color(0xFFFF3B30)

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
        FlutterFeatureTopBar(
            title = "策略",
            onBack = onBack,
            // Flutter AppNavBar solid → tokens.canvas (white), not page soft gray.
            containerColor = DemoColors.Background,
        )
        // Flutter AppNavBar solid bottom hairline
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(DemoColors.Divider),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                // Flutter ListView padding: fromLTRB(16, 8, 16, 24)
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
            StrategyPlanCard()
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
                // Flutter: EdgeInsets.only(right: index < last ? 32 : 0)
                modifier = Modifier
                    .clickable { onSelected(index) }
                    .padding(end = if (index < tabs.lastIndex) 32.dp else 0.dp),
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
            // Flutter HomeDashboardTheme.sectionLabel: fontSize 14, height 1.5
            fontSize = 14.sp,
            lineHeight = 21.sp,
        )
        // Flutter SoT: intro block sits taller (font metrics) → grid starts ~38dp lower.
        Spacer(modifier = Modifier.height(54.dp))
        val rows = HomeMockData.strategyAssets.chunked(3)
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { cell ->
                    AssetCell(cell = cell, modifier = Modifier.weight(1f))
                }
                repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        // Flutter: SizedBox(height: 16) before period row
        Spacer(modifier = Modifier.height(16.dp))
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
private fun StrategyPlanCard() {
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
                    text = "逆向",
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
        // Flutter: CrossAxisAlignment.end + Spacer between return and gauge
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column {
                Text(
                    text = "-11.35%",
                    color = GainGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                )
                Text(text = "本期收益率", color = DemoColors.TextSecondary, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.weight(1f))
            FearGreedGauge(score = 63, label = "中立")
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(text = "定投进度", color = DemoColors.TextSecondary, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))
        InvestProgressBar(current = 36, total = 50)
        Spacer(modifier = Modifier.height(12.dp))
        // Flutter SoT: Row(本周已投 1 份 + FilledButton 订阅)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "本周已投 1 份",
                color = DemoColors.TextSecondary,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(DemoColors.Accent)
                    .clickable { }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "订阅",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "在恐慌时买入、贪婪时卖出，通过定期定额降低择时压力，适合长期持有的投资者。",
            color = DemoColors.TextSecondary,
            fontSize = 14.sp,
            lineHeight = 21.sp,
        )
    }
}

/** Flutter-style filled progress with centered caption. */
@Composable
private fun InvestProgressBar(current: Int, total: Int) {
    val frac = (current.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(DemoColors.PageBg),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(frac)
                .height(24.dp)
                .background(DemoColors.Accent),
        )
        Text(
            text = "$current / $total",
            color = DemoColors.TextPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

/**
 * Flutter `_buildGauge`: SizedBox(88×56) + CustomPaint(88×44) arc (no needle) +
 * bottom text "63 中立". Colors green → #FFCC00 → red, stroke 8, radius 36.
 */
@Composable
private fun FearGreedGauge(
    score: Int,
    label: String,
) {
    val clamped = score.coerceIn(0, 100)
    Box(
        modifier = Modifier
            .width(88.dp)
            .height(56.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Canvas(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .width(88.dp)
                .height(44.dp),
        ) {
            val stroke = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
            val cx = this.size.width / 2f
            val cy = this.size.height // Flutter center at bottom of paint size
            val radius = 36.dp.toPx()
            val colors = listOf(GaugeGreen, Color(0xFFFFCC00), GaugeRed)
            val sweepPer = -180f / colors.size
            colors.forEachIndexed { i, c ->
                drawArc(
                    color = c,
                    startAngle = 180f + sweepPer * i,
                    sweepAngle = sweepPer,
                    useCenter = false,
                    topLeft = Offset(cx - radius, cy - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = stroke,
                )
            }
        }
        Text(
            text = "$clamped $label",
            color = DemoColors.TextPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
        )
    }
}

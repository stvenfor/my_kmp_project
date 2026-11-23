package com.example.my_kmp_project.feature.home

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.DemoFonts
import my_kmp_project.core.design.generated.resources.Res
import my_kmp_project.core.design.generated.resources.home_banner_bg
import org.jetbrains.compose.resources.painterResource

/**
 * 首页看板各区块 —— Flutter `features/home/lib/home/view/widgets/home_dashboard_widgets.dart`
 * 的逐块对齐实现（Compose 共享源码集）。
 *
 * 为什么抽到这里：改前 `feature/home/HomeScreen.kt`（共享）与
 * `composeApp/androidMain/.../NativeJetpackHome*.kt`（Android 原生壳）各写了一份
 * 首页，同样 11 个区块逐行重复，任何一次视觉修正都要改两遍、且必然漂移。
 * 现在两侧都只保留「宿主」（滚动容器 / 下拉刷新 / 签到弹窗），区块本身共用本文件。
 *
 * 对外边距严格照抄 Flutter 每个 section 自己的 `EdgeInsets`，**首块之前的 16
 * 由宿主提供**（Flutter `ListView(padding: top = safeTop + 16)`）。
 */
object HomeSectionStyle {
    val Accent = DemoColors.Accent
    val Surface = DemoColors.Background
    val FillSecondary = DemoColors.PageBg
    val LabelPrimary = DemoColors.TextPrimary
    val LabelSecondary = DemoColors.TextSecondary
    val LabelTertiary = DemoColors.Muted
    val Separator = DemoColors.Divider

    /** Flutter `HomeDashboardTheme.radiusMd` */
    val RadiusMd: Dp = 8.dp

    /** 全页统一细描边：与待办卡 / 入口卡 / 联系人 / 资讯卡及 Mine 分组卡同一线宽。 */
    const val GroupedCardBorder = 0.5f

    /** 手写 `Border.all(width: 0.5)` 的卡片（联系人 / 资讯） */
    const val ThinCardBorder = 0.5f

    val LargeTitle28: TextStyle
        @Composable get() = TextStyle(
            fontFamily = DemoFonts.Geist,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            color = LabelPrimary,
            lineHeight = 31.5.sp, // largeTitle height 36/32 × 28
            letterSpacing = (-1.6).sp,
        )

    val SectionTitle18: TextStyle
        @Composable get() = TextStyle(
            fontFamily = DemoFonts.Geist,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = LabelPrimary,
            lineHeight = 24.sp,
            letterSpacing = (-0.54).sp,
        )

    val SectionLabel14: TextStyle
        @Composable get() = TextStyle(
            fontFamily = DemoFonts.Geist,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = LabelSecondary,
            lineHeight = 20.sp,
            letterSpacing = (-0.28).sp,
        )
}

// ── 1. 问候 + 消息胶囊 ────────────────────────────────────────────────────────

@Composable
fun HomeGreetingSection(greeting: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 24.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = greeting,
            style = HomeSectionStyle.LargeTitle28,
            modifier = Modifier.weight(1f),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(HomeSectionStyle.Accent.copy(alpha = 0.1f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Icon(
                imageVector = HomeIcons.NotificationsNone,
                contentDescription = null,
                tint = HomeSectionStyle.Accent,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "3条新消息",
                color = HomeSectionStyle.Accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

// ── 2. 搜索栏 + 扫码 ──────────────────────────────────────────────────────────

@Composable
fun HomeSearchBar(
    placeholder: String,
    onSearch: () -> Unit,
    onScan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .clip(RoundedCornerShape(HomeSectionStyle.RadiusMd))
                .background(HomeSectionStyle.Surface)
                .border(
                    0.5.dp,
                    HomeSectionStyle.Separator,
                    RoundedCornerShape(HomeSectionStyle.RadiusMd),
                )
                .clickable(onClick = onSearch)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = HomeIcons.Search,
                contentDescription = null,
                tint = HomeSectionStyle.LabelSecondary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = placeholder,
                color = HomeSectionStyle.LabelTertiary,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(HomeSectionStyle.RadiusMd))
                .background(HomeSectionStyle.Surface)
                .border(
                    0.5.dp,
                    HomeSectionStyle.Separator,
                    RoundedCornerShape(HomeSectionStyle.RadiusMd),
                )
                .clickable(onClick = onScan),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = HomeIcons.QrScan,
                contentDescription = "扫一扫",
                tint = HomeSectionStyle.Accent,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

// ── 3. Banner（代码渲染：底色图 + 暗化 + 渐变 + 文案 + CTA）────────────────────

@Composable
fun HomeBannerSection(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp)
            .height(132.dp)
            .clip(RoundedCornerShape(HomeSectionStyle.RadiusMd)),
    ) {
        // Flutter: CacheImageUtils.network('…/seed/banner/800/400', cover) —— 本地同图。
        Image(
            painter = painterResource(Res.drawable.home_banner_bg),
            contentDescription = "朋友圈营销",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        // Flutter: color = black@25%, blendMode = darken
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.25f)),
        )
        // Flutter: accent@55% → transparent，left → right
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(HomeSectionStyle.Accent.copy(alpha = 0.55f), Color.Transparent),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 20.dp, top = 24.dp),
        ) {
            Text(
                text = "朋友圈营销",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "一键分享，高效触达客户",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    text = "立即体验",
                    color = HomeSectionStyle.Accent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

// ── 4. 功能九宫格（5 列，末位固定「更多」）───────────────────────────────────

/** Flutter `HomeFeatureGrid` 的可见项裁剪：最多 9 格，末位固定「更多」。 */
fun visibleHomeFeatures(items: List<HomeFeatureItem>): List<HomeFeatureItem> {
    val maxItems = 9
    val more = items.filter { it.label == "更多" }
    val head = items.filter { it.label != "更多" }
        .take(if (more.isEmpty()) maxItems else maxItems - 1)
    return if (more.isEmpty()) head else head + more.last()
}

@Composable
fun HomeFeatureGrid(
    items: List<HomeFeatureItem>,
    onFeature: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val visible = remember(items) { visibleHomeFeatures(items) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp)
            .clip(RoundedCornerShape(HomeSectionStyle.RadiusMd))
            .background(HomeSectionStyle.Surface)
            .border(
                HomeSectionStyle.GroupedCardBorder.dp,
                HomeSectionStyle.Separator,
                RoundedCornerShape(HomeSectionStyle.RadiusMd),
            )
            .padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
    ) {
        visible.chunked(5).forEachIndexed { rowIndex, row ->
            if (rowIndex > 0) Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
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
                                .background(HomeSectionStyle.FillSecondary),
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
                            text = item.label,
                            color = HomeSectionStyle.LabelPrimary,
                            fontSize = 12.sp,
                            // 主入口标签与「服务推荐」同级，避免一级入口比二级入口字更小。
                            fontWeight = FontWeight.Medium,
                            lineHeight = 14.4.sp, // height: 1.2
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 2.dp),
                        )
                    }
                }
                repeat(5 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

// ── 5. 公司数据（门店选择 + 分段 Tab + 指标 + 明细）───────────────────────────

@Composable
fun HomeStoreMetricsCard(
    storeName: String,
    selectedTab: Int,
    tabs: List<String>,
    metrics: List<HomeMetric>,
    details: List<HomeMetricDetail>,
    onTabSelected: (Int) -> Unit,
    onStoreTap: () -> Unit,
    onOpenLedger: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("公司数据", style = HomeSectionStyle.SectionTitle18)
            Spacer(Modifier.weight(1f))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(onClick = onOpenLedger),
            ) {
                Text(
                    text = "查看更多",
                    color = HomeSectionStyle.Accent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
                Icon(
                    imageVector = HomeIcons.ChevronRight,
                    contentDescription = null,
                    tint = HomeSectionStyle.Accent,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(HomeSectionStyle.RadiusMd))
                .background(HomeSectionStyle.Surface)
                .border(
                    HomeSectionStyle.GroupedCardBorder.dp,
                    HomeSectionStyle.Separator,
                    RoundedCornerShape(HomeSectionStyle.RadiusMd),
                )
                .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 16.dp),
        ) {
            StorePickerRow(storeName = storeName, onTap = onStoreTap)
            Spacer(Modifier.height(14.dp))
            MetricTabBar(tabs = tabs, selectedTab = selectedTab, onTabSelected = onTabSelected)
            Spacer(Modifier.height(14.dp))
            metrics.chunked(2).forEachIndexed { rowIndex, row ->
                if (rowIndex > 0) Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    row.forEach { m ->
                        MetricTile(metric = m, modifier = Modifier.weight(1f))
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            if (details.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                HorizontalDivider(thickness = 0.5.dp, color = HomeSectionStyle.Separator)
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    details.forEach { d ->
                        DetailActionTile(detail = d, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun StorePickerRow(storeName: String, onTap: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(HomeSectionStyle.FillSecondary)
            .clickable(onClick = onTap)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(HomeSectionStyle.Accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = HomeStrokeIcons.Storefront,
                contentDescription = null,
                tint = HomeSectionStyle.Accent,
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = storeName,
            color = HomeSectionStyle.LabelPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 16.8.sp, // height: 1.2
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = HomeStrokeIcons.ArrowDown,
            contentDescription = null,
            tint = HomeSectionStyle.LabelSecondary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(2.dp))
        Icon(
            imageVector = HomeStrokeIcons.SwapHoriz,
            contentDescription = "切换门店",
            tint = HomeSectionStyle.LabelTertiary,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun MetricTabBar(
    tabs: List<String>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(HomeSectionStyle.FillSecondary)
            .padding(3.dp),
    ) {
        tabs.forEachIndexed { index, label ->
            val active = index == selectedTab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (active) HomeSectionStyle.Surface else Color.Transparent)
                    .then(
                        if (active) {
                            Modifier.border(
                                0.5.dp,
                                HomeSectionStyle.Separator,
                                RoundedCornerShape(8.dp),
                            )
                        } else {
                            Modifier
                        },
                    )
                    .clickable { onTabSelected(index) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (active) {
                        HomeSectionStyle.LabelPrimary
                    } else {
                        HomeSectionStyle.LabelSecondary
                    },
                )
            }
        }
    }
}

@Composable
private fun MetricTile(metric: HomeMetric, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DemoColors.CanvasSoft)
            .border(0.5.dp, HomeSectionStyle.Separator, RoundedCornerShape(12.dp))
            .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 10.dp),
    ) {
        Text(
            text = metric.value,
            color = HomeSectionStyle.LabelPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 24.2.sp, // height: 1.1
            letterSpacing = (-0.6).sp,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = metric.label,
            color = HomeSectionStyle.LabelSecondary,
            fontSize = 12.sp,
            lineHeight = 14.4.sp, // height: 1.2
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DetailActionTile(detail: HomeMetricDetail, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(DemoColors.LinkBgSoft.copy(alpha = 0.35f))
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = detail.value,
            color = HomeSectionStyle.Accent,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 19.8.sp, // height: 1.1
            letterSpacing = (-0.4).sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = detail.label,
            color = HomeSectionStyle.LabelSecondary,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "详情",
            color = HomeSectionStyle.Accent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

// ── 6. 入口卡（投资策略 / 学习报告）───────────────────────────────────────────

@Composable
fun HomeHubEntryCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp)
            .clip(RoundedCornerShape(HomeSectionStyle.RadiusMd))
            .background(HomeSectionStyle.Surface)
            .border(
                0.5.dp,
                HomeSectionStyle.Separator,
                RoundedCornerShape(HomeSectionStyle.RadiusMd),
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(HomeSectionStyle.Accent.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = HomeSectionStyle.Accent,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = HomeSectionStyle.LabelPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(text = subtitle, color = HomeSectionStyle.LabelSecondary, fontSize = 13.sp)
        }
        Icon(
            imageVector = HomeIcons.ChevronRight,
            contentDescription = null,
            tint = HomeSectionStyle.LabelTertiary,
            modifier = Modifier.size(24.dp),
        )
    }
}

// ── 7. 服务推荐（4 列彩色描边图标 + 角标）─────────────────────────────────────

@Composable
fun HomeServiceGrid(
    items: List<HomeServiceItem>,
    onService: (String) -> Unit,
    onOpenAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "服务推荐",
                style = HomeSectionStyle.SectionTitle18,
                modifier = Modifier.weight(1f),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(onClick = onOpenAll),
            ) {
                Text(
                    text = "全部",
                    color = HomeSectionStyle.Accent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
                Icon(
                    imageVector = HomeIcons.ChevronRight,
                    contentDescription = null,
                    tint = HomeSectionStyle.Accent,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(HomeSectionStyle.RadiusMd))
                .background(HomeSectionStyle.Surface)
                .border(
                    HomeSectionStyle.GroupedCardBorder.dp,
                    HomeSectionStyle.Separator,
                    RoundedCornerShape(HomeSectionStyle.RadiusMd),
                )
                .padding(start = 4.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
        ) {
            items.chunked(4).forEachIndexed { rowIndex, row ->
                if (rowIndex > 0) Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    row.forEachIndexed { colIndex, item ->
                        val index = rowIndex * 4 + colIndex
                        ServiceGridItem(
                            item = item,
                            accent = serviceAccent(index),
                            onClick = { onService(item.label) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/** Flutter `HomeServiceGrid.accents`：link / violet / cyan / warning / pink / linkDeep / violet / ink */
private fun serviceAccent(index: Int): Color = when (index % 8) {
    0 -> DemoColors.Accent
    1 -> DemoColors.Violet
    2 -> DemoColors.Cyan
    3 -> DemoColors.Warning
    4 -> DemoColors.HighlightPink
    5 -> DemoColors.LinkDeep
    6 -> DemoColors.Violet
    else -> DemoColors.TextPrimary
}

@Composable
private fun ServiceGridItem(
    item: HomeServiceItem,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = HomeStrokeIcons.forServiceLabel(item.label),
                    contentDescription = item.label,
                    tint = accent,
                    modifier = Modifier.size(22.dp),
                )
            }
            item.badge?.let { badge ->
                Text(
                    text = badge,
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 9.9.sp, // height: 1.1
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        // Flutter Positioned(top: -4, right: -6)，Stack clipBehavior: none
                        .offset(x = 6.dp, y = (-4).dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (badge == "热门") DemoColors.Warning else DemoColors.Accent,
                        )
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = item.label,
            color = HomeSectionStyle.LabelPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 13.2.sp, // height: 1.1
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ── 8. 联系汽车之家 ──────────────────────────────────────────────────────────

@Composable
fun HomeContactList(
    items: List<HomeContactItem>,
    onContact: (HomeContactItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 24.dp),
    ) {
        Text("联系汽车之家", style = HomeSectionStyle.SectionTitle18)
        Spacer(Modifier.height(12.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(HomeSectionStyle.RadiusMd))
                .background(HomeSectionStyle.Surface)
                .border(
                    HomeSectionStyle.ThinCardBorder.dp,
                    HomeSectionStyle.Separator,
                    RoundedCornerShape(HomeSectionStyle.RadiusMd),
                ),
        ) {
            items.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        // Flutter ListTile 两行文本最小高 72
                        .heightIn(min = 72.dp)
                        .clickable { onContact(item) }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(HomeSectionStyle.FillSecondary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = HomeIcons.PersonOutline,
                            contentDescription = null,
                            tint = HomeSectionStyle.Accent,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            color = HomeSectionStyle.LabelPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(text = item.subtitle, style = HomeSectionStyle.SectionLabel14)
                    }
                    Icon(
                        imageVector = when (item.trailing) {
                            "chat" -> HomeIcons.ChatBubble
                            "phone" -> HomeIcons.Phone
                            else -> HomeIcons.ChevronRight
                        },
                        contentDescription = null,
                        tint = if (item.trailing == "chat" || item.trailing == "phone") {
                            HomeSectionStyle.Accent
                        } else {
                            HomeSectionStyle.LabelTertiary
                        },
                        modifier = Modifier.size(20.dp),
                    )
                }
                if (index < items.lastIndex) {
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = HomeSectionStyle.Separator,
                        modifier = Modifier.padding(start = 68.dp, end = 16.dp),
                    )
                }
            }
        }
    }
}

// ── 9. 行业动态（右图 96×72）─────────────────────────────────────────────────

@Composable
fun HomeNewsList(
    items: List<HomeNewsItem>,
    onOpenAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "行业动态",
                style = HomeSectionStyle.SectionTitle18,
                modifier = Modifier.weight(1f),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(onClick = onOpenAll),
            ) {
                Text(
                    text = "查看更多",
                    color = HomeSectionStyle.Accent,
                    fontSize = 14.sp,
                    letterSpacing = (-0.28).sp,
                )
                Icon(
                    imageVector = HomeIcons.ChevronRight,
                    contentDescription = null,
                    tint = HomeSectionStyle.Accent,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        items.forEach { news ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(HomeSectionStyle.RadiusMd))
                    .background(HomeSectionStyle.Surface)
                    .border(
                        HomeSectionStyle.ThinCardBorder.dp,
                        HomeSectionStyle.Separator,
                        RoundedCornerShape(HomeSectionStyle.RadiusMd),
                    )
                    .padding(12.dp),
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = news.title,
                            color = HomeSectionStyle.LabelPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 21.sp, // height: 1.4
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "${news.source}  ${news.date}",
                            style = HomeSectionStyle.SectionLabel14,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .size(width = 96.dp, height = 72.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(HomeSectionStyle.FillSecondary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = HomeIcons.Article,
                            contentDescription = null,
                            tint = HomeSectionStyle.LabelTertiary,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
            }
        }
    }
}

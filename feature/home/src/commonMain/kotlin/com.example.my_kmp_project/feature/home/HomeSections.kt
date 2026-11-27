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
import androidx.compose.ui.draw.shadow
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
import com.example.my_kmp_project.core.design.DemoElevation
import com.example.my_kmp_project.core.design.DemoFonts
import com.example.my_kmp_project.core.design.DemoGradients
import com.example.my_kmp_project.core.design.DemoRadius
import com.example.my_kmp_project.core.design.demoCard
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
/** 首页搜索框文案（服务端不再下发，纯 UI 常量，非 mock 数据）。 */
const val HomeSearchPlaceholder = "搜索客户、订单、资讯"

object HomeSectionStyle {
    val Accent = DemoColors.Accent
    val Surface = DemoColors.SurfaceRaised
    val FillSecondary = DemoColors.SurfaceSunken
    val LabelPrimary = DemoColors.TextPrimary
    val LabelSecondary = DemoColors.TextSecondary
    val LabelTertiary = DemoColors.Muted
    val Separator = DemoColors.Divider

    /** 卡片统一圆角：随设计基线走 core:design 的 16dp。 */
    val RadiusMd: Dp = DemoRadius.Card

    /** 卡片统一柔和阴影。 */
    val CardElevation: Dp = DemoElevation.Card

    /** 全页统一细描边：与待办卡 / 入口卡 / 联系人 / 资讯卡及 Mine 分组卡同一线宽。 */
    const val GroupedCardBorder = 0.5f

    /** 手写 `Border.all(width: 0.5)` 的卡片（联系人 / 资讯） */
    const val ThinCardBorder = 0.5f

    val LargeTitle28: TextStyle
        @Composable get() = TextStyle(
            fontFamily = DemoFonts.Geist,
            fontSize = 30.sp,
            fontWeight = FontWeight.SemiBold,
            color = LabelPrimary,
            lineHeight = 34.sp,
            letterSpacing = (-1.2).sp,
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

/** 首页卡片统一表面：阴影 + 圆角 + 表面色 + 低对比描边。 */
fun Modifier.homeCard(radius: Dp = HomeSectionStyle.RadiusMd): Modifier = demoCard(
    shape = RoundedCornerShape(radius),
    color = HomeSectionStyle.Surface,
    elevation = HomeSectionStyle.CardElevation,
)

// ── 1. 问候 + 消息胶囊 ────────────────────────────────────────────────────────

@Composable
fun HomeGreetingSection(greeting: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 20.dp),
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
                .clip(DemoRadius.Pill)
                .background(DemoColors.PrimarySoft)
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
                .height(46.dp)
                .homeCard(radius = DemoRadius.Card)
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
                .size(46.dp)
                .clip(RoundedCornerShape(DemoRadius.Card))
                .background(DemoGradients.brandHorizontal())
                .clickable(onClick = onScan),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = HomeIcons.QrScan,
                contentDescription = "扫一扫",
                tint = Color.White,
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
            .padding(start = 16.dp, end = 16.dp, top = 18.dp)
            .height(152.dp)
            .shadow(
                elevation = DemoElevation.Card,
                shape = RoundedCornerShape(DemoRadius.Hero),
                ambientColor = DemoElevation.CardShadow,
                spotColor = DemoElevation.CardShadow,
            )
            .clip(RoundedCornerShape(DemoRadius.Hero)),
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
                .background(Color.Black.copy(alpha = 0.22f)),
        )
        // 品牌渐变：link → violet，从左侧压暗到透明，提升文字可读性并强化品牌感。
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            DemoColors.GradientStart.copy(alpha = 0.78f),
                            DemoColors.GradientEnd.copy(alpha = 0.28f),
                            Color.Transparent,
                        ),
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
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.4).sp,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "一键分享，高效触达客户",
                color = Color.White.copy(alpha = 0.88f),
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .clip(DemoRadius.Pill)
                    .background(Color.White)
                    .padding(horizontal = 18.dp, vertical = 9.dp),
            ) {
                Text(
                    text = "立即体验",
                    color = DemoColors.GradientStart,
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
            .padding(start = 16.dp, end = 16.dp, top = 16.dp)
            .homeCard()
            .padding(start = 8.dp, end = 8.dp, top = 14.dp, bottom = 14.dp),
    ) {
        visible.chunked(5).forEachIndexed { rowIndex, row ->
            if (rowIndex > 0) Spacer(Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                row.forEachIndexed { colIndex, item ->
                    val accent = serviceAccent(rowIndex * 5 + colIndex)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onFeature(item.label) }
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(15.dp))
                                .background(accent.copy(alpha = 0.10f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = HomeStrokeIcons.forFeatureIcon(item.icon, item.label),
                                contentDescription = item.label,
                                tint = accent,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = item.label,
                            color = HomeSectionStyle.LabelPrimary,
                            fontSize = 12.sp,
                            // 主入口标签与「服务推荐」同级，避免一级入口比二级入口字更小。
                            fontWeight = FontWeight.Medium,
                            lineHeight = 15.sp,
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
            .padding(start = 16.dp, end = 16.dp, top = 24.dp),
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
                .homeCard()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 18.dp),
        ) {
            StorePickerRow(storeName = storeName, onTap = onStoreTap)
            Spacer(Modifier.height(14.dp))
            MetricTabBar(tabs = tabs, selectedTab = selectedTab, onTabSelected = onTabSelected)
            Spacer(Modifier.height(16.dp))
            metrics.chunked(2).forEachIndexed { rowIndex, row ->
                if (rowIndex > 0) Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
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
                    .then(
                        if (active) {
                            Modifier.shadow(
                                elevation = 2.dp,
                                shape = RoundedCornerShape(DemoRadius.Sm),
                                ambientColor = DemoElevation.CardShadow,
                                spotColor = DemoElevation.CardShadow,
                            )
                        } else {
                            Modifier
                        },
                    )
                    .clip(RoundedCornerShape(DemoRadius.Sm))
                    .background(if (active) HomeSectionStyle.Surface else Color.Transparent)
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
            .clip(RoundedCornerShape(DemoRadius.Md))
            .background(HomeSectionStyle.FillSecondary)
            .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 12.dp),
    ) {
        Text(
            text = metric.value,
            color = HomeSectionStyle.LabelPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 26.sp,
            letterSpacing = (-0.7).sp,
        )
        Spacer(Modifier.height(5.dp))
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
            .clip(RoundedCornerShape(DemoRadius.Md))
            .background(DemoColors.PrimarySoft)
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
            .padding(start = 16.dp, end = 16.dp, top = 20.dp)
            .homeCard()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(DemoRadius.Md))
                .background(DemoGradients.brandSoftHorizontal()),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = HomeSectionStyle.Accent,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = HomeSectionStyle.LabelPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(2.dp))
            Text(text = subtitle, color = HomeSectionStyle.LabelSecondary, fontSize = 13.sp)
        }
        Icon(
            imageVector = HomeIcons.ChevronRight,
            contentDescription = null,
            tint = HomeSectionStyle.LabelTertiary,
            modifier = Modifier.size(22.dp),
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
            .padding(start = 16.dp, end = 16.dp, top = 24.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
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
                .homeCard()
                .padding(start = 6.dp, end = 6.dp, top = 14.dp, bottom = 14.dp),
        ) {
            items.chunked(4).forEachIndexed { rowIndex, row ->
                if (rowIndex > 0) Spacer(Modifier.height(16.dp))
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
                    .size(48.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(accent.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = HomeStrokeIcons.forServiceIcon(item.icon, item.label),
                    contentDescription = item.label,
                    tint = accent,
                    modifier = Modifier.size(24.dp),
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
        Spacer(Modifier.height(8.dp))
        Text(
            text = item.label,
            color = HomeSectionStyle.LabelPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 14.sp,
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
            .padding(start = 16.dp, end = 16.dp, top = 28.dp),
    ) {
        Text("联系汽车之家", style = HomeSectionStyle.SectionTitle18)
        Spacer(Modifier.height(12.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .homeCard(),
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
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(DemoColors.PrimarySoft),
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
            .padding(start = 16.dp, end = 16.dp, top = 28.dp, bottom = 28.dp),
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
                    .padding(bottom = 14.dp)
                    .homeCard()
                    .padding(14.dp),
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
                            text = listOfNotNull(
                                news.source.takeIf { it.isNotBlank() },
                                news.date.takeIf { it.isNotBlank() },
                            ).joinToString("  "),
                            style = HomeSectionStyle.SectionLabel14,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .size(width = 96.dp, height = 72.dp)
                            .clip(RoundedCornerShape(12.dp))
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

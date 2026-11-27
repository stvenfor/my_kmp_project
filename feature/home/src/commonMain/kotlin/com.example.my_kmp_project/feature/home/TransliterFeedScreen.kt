package com.example.my_kmp_project.feature.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.demoCard
import com.example.my_kmp_project.core.design.MineTopBar
import com.example.my_kmp_project.core.platform.showPlatformToast
import com.example.my_kmp_project.core.ui.ReportMainTabRoot
import my_kmp_project.core.design.generated.resources.Res
import my_kmp_project.core.design.generated.resources.home_transliter_home_banner_main
import my_kmp_project.core.design.generated.resources.home_transliter_home_bar_icon_top
import my_kmp_project.core.design.generated.resources.home_transliter_home_cover_01
import my_kmp_project.core.design.generated.resources.home_transliter_home_cover_02
import my_kmp_project.core.design.generated.resources.home_transliter_home_cover_03
import my_kmp_project.core.design.generated.resources.home_transliter_home_cover_04
import my_kmp_project.core.design.generated.resources.home_transliter_home_cover_05
import my_kmp_project.core.design.generated.resources.home_transliter_home_cover_06
import my_kmp_project.core.design.generated.resources.home_transliter_home_cover_07
import my_kmp_project.core.design.generated.resources.home_transliter_home_cover_08
import my_kmp_project.core.design.generated.resources.home_transliter_home_cover_09
import my_kmp_project.core.design.generated.resources.home_transliter_home_cover_10
import my_kmp_project.core.design.generated.resources.home_transliter_home_cover_11
import my_kmp_project.core.design.generated.resources.home_transliter_home_cover_12
import my_kmp_project.core.design.generated.resources.home_transliter_home_feature_all_videos
import my_kmp_project.core.design.generated.resources.home_transliter_home_feature_check_in
import my_kmp_project.core.design.generated.resources.home_transliter_home_feature_classic_theater
import my_kmp_project.core.design.generated.resources.home_transliter_home_feature_movie_words
import my_kmp_project.core.design.generated.resources.home_transliter_home_feature_rank
import my_kmp_project.core.design.generated.resources.home_transliter_home_icon_chevron_right
import my_kmp_project.core.design.generated.resources.home_transliter_home_icon_duration
import my_kmp_project.core.design.generated.resources.home_transliter_home_icon_notification
import my_kmp_project.core.design.generated.resources.home_transliter_home_icon_search
import my_kmp_project.core.design.generated.resources.home_transliter_home_icon_swap
import my_kmp_project.core.design.generated.resources.home_transliter_home_thumb_default
import my_kmp_project.core.design.generated.resources.ic_nav_back
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun TransliterFeedScreen(
    onBack: () -> Unit,
    content: HomeContentStore,
    onOpenHotRank: () -> Unit,
) {
    // Flutter TransliterHomePage — header / categories / banner / features / recent / expert + lower sections
    // 顶部热榜条：取首个热榜 tab 的前 3 条（来自 /home/search/meta）。
    val topRankTab = content.rankTabs().firstOrNull()
    val topRankItems = if (topRankTab != null) content.rankItems(topRankTab).take(3) else emptyList()
    LaunchedEffect(Unit) { content.refreshSearchMeta() }
    val tabAccent = DemoColors.Accent // Flutter VercelTokens.link
    val adBlue = Color(0xFF3B82F6)
    val svipGold = Color(0xFFE6A23C)
    val searchPillBg = Color(0xFFF2F3F5)
    val categories = listOf("SVIP", "跟读", "听力", "小剧场", "专题")
    val selectedCategory = 1 // 跟读
    val features = listOf(
        Triple("每日打卡", Res.drawable.home_transliter_home_feature_check_in, false),
        Triple("影视单词", Res.drawable.home_transliter_home_feature_movie_words, false),
        Triple("经典剧场", Res.drawable.home_transliter_home_feature_classic_theater, false),
        Triple("排行榜", Res.drawable.home_transliter_home_feature_rank, true),
        Triple("全部视频", Res.drawable.home_transliter_home_feature_all_videos, false),
    )
    val recent = listOf(
        Triple("穿梭在迷宫的勇士", "03:24", Res.drawable.home_transliter_home_cover_05),
        Triple("萌宠部落", "02:18", Res.drawable.home_transliter_home_cover_06),
        Triple("完美的世界", "04:05", Res.drawable.home_transliter_home_cover_07),
        Triple("小猪佩奇", "01:56", Res.drawable.home_transliter_home_cover_08),
    )
    // Flutter TransliterHomeExpertShowcase — 2×2 grid + avatar/user/subtitle
    data class ExpertCard(
        val title: String,
        val userName: String,
        val subtitle: String,
        val cover: DrawableResource,
    )
    val expert = listOf(
        ExpertCard("英语启蒙课堂", "蓝儿老师Joyue", "跟读练习 · 初级", Res.drawable.home_transliter_home_cover_09),
        ExpertCard("趣味跟读挑战", "跟读达人", "动画跟读 · 中级", Res.drawable.home_transliter_home_cover_10),
        ExpertCard("诵读之星", "朗读爱好者", "经典诵读", Res.drawable.home_transliter_home_cover_11),
        ExpertCard("动画跟读秀", "动画迷", "角色模仿", Res.drawable.home_transliter_home_cover_12),
    )
    val editor = listOf(
        Triple("经典动画跟读", "跟佩奇一起快乐学英语", Res.drawable.home_transliter_home_cover_01),
        Triple("英语启蒙课堂", "零基础也能开口说", Res.drawable.home_transliter_home_cover_02),
        Triple("趣味跟读挑战", "模仿经典电影片段", Res.drawable.home_transliter_home_cover_03),
        Triple("每日跟读练习", "坚持打卡领奖励", Res.drawable.home_transliter_home_cover_04),
    )
    val albums = listOf(
        Triple("小猪佩奇", "52集", Res.drawable.home_transliter_home_cover_05),
        Triple("冰雪奇缘", "12集", Res.drawable.home_transliter_home_cover_06),
        Triple("狮子王", "8集", Res.drawable.home_transliter_home_cover_07),
        Triple("海底总动员", "16集", Res.drawable.home_transliter_home_cover_08),
    )
    val guess = listOf(
        Triple("趣味英语跟读", "6.8万播放 · 05:12", Res.drawable.home_transliter_home_cover_09),
        Triple("经典电影片段", "9.2万播放 · 03:45", Res.drawable.home_transliter_home_cover_10),
    )

    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(DemoColors.PageBg)) {
        // Header: back + search pill + history + notification (Flutter AppBar, not MineTopBar title-only)
        Column(
            Modifier
                .fillMaxWidth()
                .background(DemoColors.PageBg)
                .statusBarsPadding(),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(FlutterFeatureToolbarHeight)
                    .padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Image(
                        painter = painterResource(Res.drawable.ic_nav_back),
                        contentDescription = "返回",
                        modifier = Modifier
                            .size(24.dp)
                            .rotate(180f),
                        contentScale = ContentScale.Fit,
                        colorFilter = ColorFilter.tint(DemoColors.TextPrimary),
                    )
                }
                Row(
                    Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(searchPillBg)
                        .clickable { showPlatformToast("搜索") }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        painter = painterResource(Res.drawable.home_transliter_home_icon_search),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        contentScale = ContentScale.Fit,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("学英语", fontSize = 14.sp, color = DemoColors.Muted)
                }
                IconButton(onClick = { showPlatformToast("历史") }) {
                    TransliterHistoryClockIcon(Modifier.size(22.dp), tint = DemoColors.TextPrimary)
                }
                Box {
                    IconButton(onClick = { showPlatformToast("通知") }) {
                        Image(
                            painter = painterResource(Res.drawable.home_transliter_home_icon_notification),
                            contentDescription = "通知",
                            modifier = Modifier.size(22.dp),
                            contentScale = ContentScale.Fit,
                        )
                    }
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 10.dp, end = 10.dp)
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF3B30)),
                    )
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    categories.forEachIndexed { i, label ->
                        val selected = i == selectedCategory
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { showPlatformToast(label) },
                        ) {
                            Text(
                                label,
                                // Flutter: selected w600 / unselected w400, all 16.sp
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                color = when {
                                    label == "SVIP" -> svipGold
                                    selected -> tabAccent
                                    else -> DemoColors.TextSecondary
                                },
                                fontSize = 16.sp,
                            )
                            // Flutter: SizedBox(8.h) then indicator 24.w × 3.h
                            Spacer(Modifier.height(8.dp))
                            Box(
                                Modifier
                                    .width(if (selected) 24.dp else 0.dp)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(if (selected) tabAccent else Color.Transparent),
                            )
                        }
                    }
                }
                Image(
                    painter = painterResource(Res.drawable.home_transliter_home_bar_icon_top),
                    contentDescription = "菜单",
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(36.dp)
                        .clickable { showPlatformToast("菜单") },
                    contentScale = ContentScale.Fit,
                )
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
        ) {
            // Flutter TransliterHomeBannerCarousel: padding LTRB(16,8,16,8), height 156, radius 8
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(156.dp)
                        .clip(RoundedCornerShape(8.dp)),
                ) {
                    Image(
                        painter = painterResource(Res.drawable.home_transliter_home_banner_main),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                    Text(
                        "AD",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(tabAccent) // Flutter primaryGreen = link
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(3) { i ->
                        val active = i == 0
                        Box(
                            Modifier
                                .padding(horizontal = 3.dp)
                                .width(if (active) 14.dp else 6.dp)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (active) tabAccent else Color(0xFFD0D0D5)),
                        )
                    }
                }
            }

            // Flutter TransliterHomeFeatureRow: pad LTRB(4,4,4,12), icon 52, gap 8
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 16.dp),
            ) {
                features.forEach { (label, icon, openRank) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                if (openRank) onOpenHotRank()
                                else showPlatformToast(label)
                            },
                    ) {
                        Image(
                            painter = painterResource(icon),
                            contentDescription = label,
                            modifier = Modifier.size(52.dp),
                            contentScale = ContentScale.Fit,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(label, fontSize = 11.sp, maxLines = 1, color = DemoColors.TextPrimary)
                    }
                }
            }

            TransliterSectionTitle(
                title = "最近在学",
                trailing = {
                    Image(
                        painter = painterResource(Res.drawable.home_transliter_home_icon_chevron_right),
                        contentDescription = null,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { showPlatformToast("最近在学") },
                        contentScale = ContentScale.Fit,
                    )
                },
            )
            Row(
                Modifier
                    .height(118.dp)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                recent.forEach { (title, dur, cover) ->
                    // Flutter RecentLearning: width 120.w, cover 68.h, title 12.sp
                    Column(Modifier.width(120.dp)) {
                        TransliterCoverWithDuration(
                            cover = cover,
                            duration = dur,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(68.dp),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            title,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = DemoColors.TextPrimary,
                            lineHeight = 16.2.sp,
                        )
                    }
                }
            }

            TransliterSectionTitle(
                title = "新手赛场",
                trailing = {
                    // Flutter TransliterSectionHeaderStyle.refresh
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { showPlatformToast("换一换") },
                    ) {
                        Image(
                            painter = painterResource(Res.drawable.home_transliter_home_icon_swap),
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            contentScale = ContentScale.Fit,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("换一换", fontSize = 13.sp, color = DemoColors.TextSecondary)
                    }
                },
            )
            // Flutter GridView: crossAxisCount=2, aspectRatio=0.72, cover 96.h
            BoxWithConstraints(Modifier.padding(horizontal = 16.dp)) {
                val gap = 12.dp
                val cardW = (maxWidth - gap) / 2
                val cardH = cardW / 0.72f
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    expert.chunked(2).forEach { rowItems ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(gap),
                        ) {
                            rowItems.forEach { card ->
                                Column(
                                    Modifier
                                        .width(cardW)
                                        .height(cardH),
                                ) {
                                    TransliterCoverWithDuration(
                                        cover = card.cover,
                                        duration = "02:30",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(96.dp),
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Image(
                                            painter = painterResource(Res.drawable.home_transliter_home_thumb_default),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop,
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            card.userName,
                                            fontSize = 11.sp,
                                            color = DemoColors.TextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        card.title,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = DemoColors.TextPrimary,
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        card.subtitle,
                                        fontSize = 11.sp,
                                        color = DemoColors.TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            if (rowItems.size == 1) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            TransliterSectionTitle(
                title = "热榜",
                action = "查看 >",
                onAction = onOpenHotRank,
            )
            if (topRankTab != null && topRankItems.isNotEmpty()) {
                Row(
                    Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    listOf(topRankTab to Color(0xFFFFE4EC)).forEach { (board, bg) ->
                        Column(
                            Modifier
                                .width(200.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(bg)
                                .clickable(onClick = onOpenHotRank)
                                .padding(12.dp),
                        ) {
                            Text(board, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(Modifier.height(8.dp))
                            topRankItems.forEach { item ->
                                Text(
                                    "${item.rank}. ${item.title}",
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    modifier = Modifier.padding(vertical = 2.dp),
                                )
                            }
                        }
                    }
                }
            }

            TransliterSectionTitle("小编精选")
            Row(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                editor.forEach { (title, sub, cover) ->
                    Column(Modifier.width(140.dp)) {
                        Image(
                            painter = painterResource(cover),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(84.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            title,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            sub,
                            fontSize = 11.sp,
                            color = DemoColors.TextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            TransliterSectionTitle(title = "专属为你", subtitle = "根据你的学习兴趣为你推荐")
            Row(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                albums.forEach { (title, eps, cover) ->
                    Column(Modifier.width(110.dp)) {
                        Image(
                            painter = painterResource(cover),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(title, fontWeight = FontWeight.Medium, fontSize = 13.sp, maxLines = 1)
                        Text(eps, fontSize = 11.sp, color = DemoColors.TextSecondary)
                    }
                }
            }

            TransliterSectionTitle("猜你喜欢")
            Column(
                Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                guess.forEach { (title, meta, cover) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .demoCard()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Image(
                            painter = painterResource(cover),
                            contentDescription = null,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop,
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(meta, fontSize = 12.sp, color = DemoColors.TextSecondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransliterCoverWithDuration(
    cover: DrawableResource,
    duration: String,
    modifier: Modifier = Modifier,
) {
    // Flutter TransliterHomeTheme.thumbRadius = 8
    Box(modifier.clip(RoundedCornerShape(8.dp))) {
        Image(
            painter = painterResource(cover),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Row(
            Modifier
                .align(Alignment.BottomStart)
                .padding(6.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(Res.drawable.home_transliter_home_icon_duration),
                contentDescription = null,
                modifier = Modifier.size(10.dp),
                contentScale = ContentScale.Fit,
            )
            Spacer(Modifier.width(3.dp))
            Text(duration, color = Color.White, fontSize = 10.sp)
        }
    }
}

@Composable
private fun TransliterHistoryClockIcon(modifier: Modifier = Modifier, tint: Color = DemoColors.TextPrimary) {
    Canvas(modifier) {
        val stroke = 1.7.dp.toPx()
        val r = size.minDimension / 2f - stroke
        val c = Offset(size.width / 2f, size.height / 2f)
        drawCircle(color = tint, radius = r, center = c, style = Stroke(width = stroke))
        drawLine(
            color = tint,
            start = c,
            end = Offset(c.x, c.y - r * 0.55f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = tint,
            start = c,
            end = Offset(c.x + r * 0.4f, c.y + r * 0.15f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun TransliterSectionTitle(
    title: String,
    subtitle: String? = null,
    action: String? = null,
    onAction: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            // Flutter TransliterHomeSectionHeader: fromLTRB(16, 24, 16, 12)
            .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            if (subtitle != null) {
                Text(subtitle, fontSize = 12.sp, color = DemoColors.TextSecondary)
            }
        }
        when {
            trailing != null -> trailing()
            action != null -> Text(
                action,
                color = DemoColors.Accent,
                fontSize = 13.sp,
                modifier = Modifier.clickable { onAction?.invoke() },
            )
        }
    }
}

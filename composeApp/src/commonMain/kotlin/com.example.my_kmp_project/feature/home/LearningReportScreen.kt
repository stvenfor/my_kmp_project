package com.example.my_kmp_project.feature.home

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.platform.showPlatformToast
import com.example.my_kmp_project.core.ui.ReportMainTabRoot

/** Flutter `HomeReportColors` — map onto Vercel dark canvas semantics. */
private object ReportColors {
    val Background = Color(0xFF000000)
    val HighlightCard = Color(0xFF0A0A0A)
    val HighlightBorder = Color(0xFF2E2E2E)
    val RecordCard = Color(0xFF111111)
    val RecordItem = Color(0xFF1A1A1A)
    val IconTeal = Color(0xFF0070F3)
    val IconTealLight = Color(0xFF3291FF)
    val TitleWhite = Color(0xFFEDEDED)
    val SubtitleGrey = Color(0xFFA1A1A1)
    val MetaGrey = Color(0xFF666666)
    val Orange = Color(0xFFF5A623)
    val OrangeDeep = Color(0xFFAB570A)
    val DotYellow = Color(0xFFF9CB28)
    val DotBlue = Color(0xFF0070F3)
    val Divider = Color(0xFF2E2E2E)
    val BannerStart = Color(0xFF111111)
    val BannerEnd = Color(0xFF0A0A0A)
    val ParentChip = Color(0xCC1A1A1A)
    val PlayBg = Color(0xFF2E3340)
}

@Composable
internal fun LearningReportScreen(
    onBack: () -> Unit,
    onOpenMembership: (() -> Unit)? = null,
) {
    ReportMainTabRoot(isRoot = false)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ReportColors.Background),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            FlutterFeatureTopBar(
                title = "学习报告",
                onBack = onBack,
                containerColor = ReportColors.Background,
                titleColor = ReportColors.TitleWhite,
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(y = (-16).dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(top = 12.dp, bottom = 120.dp),
            ) {
                ReportSectionHeader(dotColor = ReportColors.DotYellow, title = "今日高光")
                Spacer(modifier = Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(ReportColors.HighlightCard)
                        .border(1.dp, ReportColors.HighlightBorder, RoundedCornerShape(16.dp)),
                ) {
                    HomeMockData.reportHighlights.forEachIndexed { index, item ->
                        HighlightRow(item)
                        if (index < HomeMockData.reportHighlights.lastIndex) {
                            HorizontalDivider(
                                color = ReportColors.Divider,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                ReportSectionHeader(dotColor = ReportColors.DotBlue, title = "今日学习记录")
                Spacer(modifier = Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(ReportColors.RecordCard)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    HomeMockData.reportRecords.forEach { record ->
                        RecordRow(record)
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 16.dp, bottom = 88.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(ReportColors.ParentChip)
                .border(1.dp, ReportColors.Divider, RoundedCornerShape(20.dp))
                .clickable { showPlatformToast("家长助手") }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("👨‍👩‍👧", fontSize = 14.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text("家长助手", color = ReportColors.SubtitleGrey, fontSize = 12.sp)
        }

        MembershipBanner(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            onOpen = {
                onOpenMembership?.invoke() ?: showPlatformToast("立即开通")
            },
        )
    }
}

@Composable
private fun ReportSectionHeader(dotColor: Color, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            color = ReportColors.TitleWhite,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
        )
    }
}

@Composable
private fun HighlightRow(item: ReportHighlight) {
    // Flutter: teal gradient icon tile + orangeDeep score pill / emoji / play square.
    val isScore = item.trailing.isNotEmpty() && item.trailing.all { it.isDigit() }
    val isPlay = item.trailing == "▶" || item.trailing.equals("play", ignoreCase = true)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.linearGradient(
                        listOf(ReportColors.IconTealLight, ReportColors.IconTeal),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = item.emoji, fontSize = 22.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                color = ReportColors.TitleWhite,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.subtitle,
                color = ReportColors.SubtitleGrey,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        when {
            isScore -> Text(
                text = item.trailing,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(ReportColors.OrangeDeep)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
            // Flutter SoT: blue circular play (code uses dark square + blue icon; pixels favor circle).
            isPlay -> Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(ReportColors.DotBlue),
                contentAlignment = Alignment.Center,
            ) {
                Text("▶", color = Color.White, fontSize = 12.sp)
            }
            else -> Text(text = item.trailing, fontSize = 24.sp)
        }
    }
}

@Composable
private fun RecordRow(item: ReportRecord) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ReportColors.RecordItem)
            .border(1.dp, ReportColors.Divider, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(ReportColors.RecordItem)
                .border(1.dp, ReportColors.Divider, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = item.emoji, fontSize = 22.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                color = ReportColors.TitleWhite,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.subtitle,
                color = ReportColors.SubtitleGrey,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(text = item.time, color = ReportColors.MetaGrey, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.status,
                color = if (item.statusHighlight) ReportColors.Orange else ReportColors.MetaGrey,
                fontWeight = if (item.statusHighlight) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun MembershipBanner(
    modifier: Modifier = Modifier,
    onOpen: () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(ReportColors.BannerStart, ReportColors.BannerEnd),
                ),
            )
            .border(1.dp, Color(0xFF3D2A20), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = "👑", fontSize = 28.sp)
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "开通会员，解锁全部内容",
                color = ReportColors.TitleWhite,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "全量剧集 · AI外教不限时 · 专属勋章",
                color = ReportColors.SubtitleGrey,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "立即开通",
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(ReportColors.OrangeDeep)
                .clickable(onClick = onOpen)
                .padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

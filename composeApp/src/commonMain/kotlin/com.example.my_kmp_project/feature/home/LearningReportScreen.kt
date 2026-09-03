package com.example.my_kmp_project.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.my_kmp_project.core.design.MineTopBar
import com.example.my_kmp_project.feature.shell.ReportMainTabRoot

/** Flutter `HomeReportColors` — local to report screen (DemoColors is light chrome). */
private object ReportColors {
    val Background = Color(0xFF0B0C11)
    val HighlightCard = Color(0xFF121A1F)
    val HighlightBorder = Color(0xFF1E3330)
    val RecordCard = Color(0xFF17161F)
    val RecordItem = Color(0xFF242630)
    val TitleWhite = Color(0xFFF5F6F8)
    val SubtitleGrey = Color(0xFF8B9099)
    val MetaGrey = Color(0xFF6B7078)
    val Orange = Color(0xFFFF8A34)
    val DotYellow = Color(0xFFFFD54F)
    val DotBlue = Color(0xFF5EB3FF)
    val Divider = Color(0xFF2A2D35)
    val BannerStart = Color(0xFF2A1810)
    val BannerEnd = Color(0xFF1A1210)
}

@Composable
internal fun LearningReportScreen(onBack: () -> Unit) {
    ReportMainTabRoot(isRoot = false)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ReportColors.Background),
    ) {
        MineTopBar(
            title = "学习报告",
            onBack = onBack,
            containerColor = ReportColors.Background,
            titleColor = ReportColors.TitleWhite,
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp, bottom = 32.dp),
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

            Spacer(modifier = Modifier.height(20.dp))
            MembershipBanner()
        }
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF1A3B38), Color(0xFF2A5A54)),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = item.emoji, fontSize = 18.sp)
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
        Text(
            text = item.trailing,
            color = ReportColors.Orange,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
        )
    }
}

@Composable
private fun RecordRow(item: ReportRecord) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ReportColors.RecordItem)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = item.emoji, fontSize = 22.sp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                color = ReportColors.TitleWhite,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.subtitle,
                color = ReportColors.SubtitleGrey,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(text = item.time, color = ReportColors.MetaGrey, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.status,
                color = if (item.statusHighlight) ReportColors.Orange else ReportColors.SubtitleGrey,
                fontWeight = if (item.statusHighlight) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun MembershipBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(ReportColors.BannerStart, ReportColors.BannerEnd),
                ),
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "开通会员 · 解锁完整学习报告",
                color = ReportColors.TitleWhite,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "周报洞察 · 发音进步曲线 · 家长助手",
                color = ReportColors.SubtitleGrey,
                fontSize = 12.sp,
            )
        }
        Text(
            text = "去开通",
            color = ReportColors.Orange,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
        )
    }
}

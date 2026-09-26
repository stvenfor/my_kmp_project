package com.example.my_kmp_project.nativeshell

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.feature.home.HomeAssetIcon
import com.example.my_kmp_project.feature.home.HomeMockData
import com.example.my_kmp_project.feature.home.HomeServiceAssets

/**
 * Flutter `HomeTodoCardStrip` — no section title; 2-col white cards.
 * When todo API empty, parent hides this composable.
 */
@Composable
internal fun JetpackTodoStrip(onDeferred: (String) -> Unit) {
    val cards = HomeMockData.quickActions
    if (cards.isEmpty()) return
    Column(
        Modifier
            .padding(start = 16.dp, end = 16.dp, top = 12.dp)
            .fillMaxWidth(),
    ) {
        cards.chunked(2).forEach { row ->
            Row(
                Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { action ->
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(DemoColors.Background)
                            .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(10.dp))
                            .clickable { onDeferred(action.title) }
                            .padding(12.dp),
                    ) {
                        Text(
                            action.title,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = DemoColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            action.subtitle,
                            fontSize = 12.sp,
                            color = DemoColors.TextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            action.actionLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = DemoColors.Accent,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(DemoColors.Accent.copy(alpha = 0.1f))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** Flutter `HomeServiceGrid` — title 服务推荐 + 全部 › */
@Composable
internal fun JetpackServiceGrid(onDeferred: (String) -> Unit) {
    Column(
        Modifier
            .padding(start = 16.dp, end = 16.dp, top = 16.dp)
            .fillMaxWidth(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "服务推荐",
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                color = DemoColors.TextPrimary,
                modifier = Modifier.weight(1f),
            )
            Text(
                "全部",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = DemoColors.Accent,
                modifier = Modifier.clickable { onDeferred("全部服务") },
            )
            Text("›", fontSize = 16.sp, color = DemoColors.Accent)
        }
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(DemoColors.Background)
                .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 12.dp),
        ) {
            HomeMockData.services.chunked(4).forEachIndexed { rowIndex, row ->
                Row(Modifier.fillMaxWidth()) {
                    row.forEachIndexed { colIndex, item ->
                        val index = rowIndex * 4 + colIndex
                        Column(
                            Modifier
                                .weight(1f)
                                .clickable {
                                    when (item.label) {
                                        "更多" -> onDeferred("全部服务")
                                        "直播" -> onDeferred("直播带货")
                                        else -> onDeferred(item.label)
                                    }
                                }
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box {
                                HomeAssetIcon(
                                    resource = HomeServiceAssets.serviceAt(index),
                                    size = 44.dp,
                                    contentDescription = item.label,
                                )
                                item.badge?.let { badge ->
                                    val bg = if (badge == "热门") Color(0xFFFF9500) else DemoColors.Accent
                                    Text(
                                        badge,
                                        fontSize = 9.sp,
                                        color = Color.White,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(bg)
                                            .padding(horizontal = 4.dp, vertical = 1.dp),
                                    )
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(item.label, fontSize = 12.sp, color = DemoColors.TextPrimary)
                        }
                    }
                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/** Flutter `HomeContactList` — 联系汽车之家 */
@Composable
internal fun JetpackContactList(onDeferred: (String) -> Unit) {
    Column(
        Modifier
            .padding(start = 16.dp, end = 16.dp, top = 16.dp)
            .fillMaxWidth(),
    ) {
        Text(
            "联系汽车之家",
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            color = DemoColors.TextPrimary,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(DemoColors.Background)
                .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(8.dp)),
        ) {
            HomeMockData.contacts.forEachIndexed { index, c ->
                // Flutter HomeContactList: display-only — no onTap.
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(DemoColors.Accent.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (c.trailing == "phone") "☎" else c.title.take(1),
                            color = DemoColors.Accent,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(c.title, fontWeight = FontWeight.Medium, color = DemoColors.TextPrimary)
                        Text(c.subtitle, fontSize = 12.sp, color = DemoColors.TextSecondary)
                    }
                    Text(
                        if (c.trailing == "phone") "拨打" else "聊",
                        color = DemoColors.Accent,
                        fontSize = 13.sp,
                    )
                }
                if (index < HomeMockData.contacts.lastIndex) {
                    HorizontalDivider(
                        Modifier.padding(start = 70.dp),
                        color = DemoColors.Divider,
                        thickness = 0.5.dp,
                    )
                }
            }
        }
    }
}

/** Flutter `HomeNewsList` — 行业动态 */
@Composable
internal fun JetpackNewsList() {
    Column(
        Modifier
            .padding(start = 16.dp, end = 16.dp, top = 16.dp)
            .fillMaxWidth(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "行业动态",
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                color = DemoColors.TextPrimary,
                modifier = Modifier.weight(1f),
            )
            Text("查看更多", fontSize = 13.sp, color = DemoColors.Accent, fontWeight = FontWeight.Medium)
            Text("›", fontSize = 16.sp, color = DemoColors.Accent)
        }
        HomeMockData.news.forEach { n ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(DemoColors.Background)
                    .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(8.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        n.title,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        color = DemoColors.TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "${n.source}  ${n.date}",
                        fontSize = 12.sp,
                        color = DemoColors.TextSecondary,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Box(
                    Modifier
                        .width(96.dp)
                        .height(72.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DemoColors.PageBg),
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** Flutter `_LearningReportEntry`. */
@Composable
internal fun JetpackLearningReportEntry(onDeferred: (String) -> Unit) {
    Row(
        Modifier
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DemoColors.Background)
            .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(8.dp))
            .clickable { onDeferred("学习报告") }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DemoColors.Accent.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Text("报", color = DemoColors.Accent, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("学习报告", fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = DemoColors.TextPrimary)
            Text("今日高光 · 学习记录", fontSize = 13.sp, color = DemoColors.TextSecondary)
        }
        Text("›", color = DemoColors.TextSecondary, fontSize = 18.sp)
    }
}

@Composable
internal fun JetpackStrategyEntry(onDeferred: (String) -> Unit) {
    Row(
        Modifier
            .padding(start = 16.dp, end = 16.dp, top = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DemoColors.Background)
            .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(8.dp))
            .clickable { onDeferred("投资策略") }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DemoColors.Accent.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Text("策", color = DemoColors.Accent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("投资策略", fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = DemoColors.TextPrimary)
            Text("资产九宫格 · 恐贪定投 · 趋势策略", fontSize = 13.sp, color = DemoColors.TextSecondary)
        }
        Text("›", color = DemoColors.TextSecondary)
    }
}

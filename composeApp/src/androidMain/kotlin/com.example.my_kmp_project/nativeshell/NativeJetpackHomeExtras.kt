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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.feature.home.HomeMockData

@Composable
internal fun JetpackTodoStrip(onDeferred: (String) -> Unit) {
    Column(
        Modifier
            .padding(start = 16.dp, end = 16.dp, top = 12.dp)
            .fillMaxWidth(),
    ) {
        Text(
            "待办",
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = DemoColors.TextPrimary,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        HomeMockData.quickActions.forEach { action ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(DemoColors.Background)
                    .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(10.dp))
                    .clickable { onDeferred(action.title) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(action.title, fontWeight = FontWeight.Medium, color = DemoColors.TextPrimary)
                    Text(action.subtitle, fontSize = 12.sp, color = DemoColors.TextSecondary)
                }
                Text(action.actionLabel, color = DemoColors.Accent, fontSize = 13.sp)
            }
        }
    }
}

@Composable
internal fun JetpackServiceGrid(onDeferred: (String) -> Unit) {
    Column(
        Modifier
            .padding(start = 16.dp, end = 16.dp, top = 16.dp)
            .fillMaxWidth(),
    ) {
        Text(
            "常用服务",
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = DemoColors.TextPrimary,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        HomeMockData.services.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { item ->
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
                        Text(item.label, fontSize = 12.sp, color = DemoColors.TextPrimary)
                        item.badge?.let {
                            Text(it, fontSize = 10.sp, color = DemoColors.Accent)
                        }
                    }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
internal fun JetpackContactList(onDeferred: (String) -> Unit) {
    Column(
        Modifier
            .padding(start = 16.dp, end = 16.dp, top = 16.dp)
            .fillMaxWidth(),
    ) {
        Text(
            "联系人",
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        HomeMockData.contacts.forEach { c ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DemoColors.Background)
                    .clickable { onDeferred(c.title) }
                    .padding(14.dp)
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(c.title, fontWeight = FontWeight.Medium)
                    Text(c.subtitle, fontSize = 12.sp, color = DemoColors.TextSecondary)
                }
                c.trailing?.let { Text(it, color = DemoColors.Accent) }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
internal fun JetpackNewsList() {
    Column(
        Modifier
            .padding(start = 16.dp, end = 16.dp, top = 8.dp)
            .fillMaxWidth(),
    ) {
        Text(
            "资讯",
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        HomeMockData.news.forEach { n ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
            ) {
                Text(n.title, fontWeight = FontWeight.Medium, maxLines = 2)
                Text("${n.source} · ${n.date}", fontSize = 12.sp, color = DemoColors.TextSecondary)
            }
            HorizontalDivider(color = DemoColors.Divider)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
internal fun JetpackLearningReportEntry(onDeferred: (String) -> Unit) {
    Row(
        Modifier
            .padding(start = 16.dp, end = 16.dp, top = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DemoColors.Accent.copy(alpha = 0.08f))
            .clickable { onDeferred("学习报告") }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("学习报告", fontWeight = FontWeight.SemiBold, color = DemoColors.TextPrimary)
        Text("查看 >", color = DemoColors.Accent)
    }
}

@Composable
internal fun JetpackStrategyEntry(onDeferred: (String) -> Unit) {
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
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DemoColors.Accent.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Text("投", color = DemoColors.Accent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("投资策略", color = DemoColors.TextPrimary)
            Text("资产九宫格 · 恐贪定投 · 趋势策略", fontSize = 12.sp, color = DemoColors.TextSecondary)
        }
        Text("›", color = DemoColors.TextSecondary)
    }
}

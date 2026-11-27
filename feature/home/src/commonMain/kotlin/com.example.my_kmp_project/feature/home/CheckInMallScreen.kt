package com.example.my_kmp_project.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.DemoRadius
import com.example.my_kmp_project.core.design.demoCard
import com.example.my_kmp_project.core.platform.showPlatformToast
import com.example.my_kmp_project.core.ui.ReportMainTabRoot
import com.example.my_kmp_project.feature.home.api.TaskProgress
import kotlinx.coroutines.launch

@Composable
internal fun CheckInMallScreen(onBack: () -> Unit) {
    // Flutter CheckInMallPage — 真实 PointsApi（status / tasks / check-in / claim）。
    val headerBlue = DemoColors.Accent
    val coinGold = Color(0xFFF5A623)
    HomePointsStore.version
    val scope = rememberCoroutineScope()
    val points = HomePointsStore.balance
    val streak = HomePointsStore.streak
    val checkedToday = HomePointsStore.checkedInToday
    var remind by remember { mutableStateOf(false) }
    var checkingIn by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { HomePointsStore.refresh() }

    data class DayCell(val label: String, val reward: Int, val signed: Boolean, val isToday: Boolean)
    val calendar = HomePointsStore.calendar.map {
        DayCell(
            label = if (it.isToday) "今天" else it.day,
            reward = it.reward,
            signed = it.signed,
            isToday = it.isToday,
        )
    }
    data class TaskRow(
        val code: String,
        val title: String,
        val points: Int,
        val action: String,
        val icon: String,
    )
    val tasks = HomePointsStore.tasks.map { task ->
        TaskRow(
            code = task.code,
            title = task.title,
            points = task.reward,
            action = when (task.progress) {
                TaskProgress.Claimable -> "领取"
                TaskProgress.Claimed -> "已领"
                TaskProgress.Incomplete -> "去完成"
            },
            icon = "➡️",
        )
    }
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(DemoColors.PageBg)) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(headerBlue),
        ) {
            FlutterFeatureTopBar(
                title = "签到商城",
                onBack = onBack,
                containerColor = headerBlue,
                titleColor = Color.White,
            )
            Column(
                // Tuned vs Flutter SoT (1440): −28 overshot; −18 keeps card/tasks aligned.
                Modifier.offset(y = (-18).dp),
            ) {
            Row(
                Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF3A8EE6))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("🔊", fontSize = 14.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    "温馨提示：本页面只保留近3个月内的积分记录",
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 16.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text("我的积分", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("$points", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                }
                Column(Modifier.weight(1f)) {
                    Text("连续签到", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("$streak", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "天",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 6.dp),
                        )
                    }
                }
            }
            } // offset header body
        }
        Column(
            Modifier
                .weight(1f)
                .offset(y = (-18).dp)
                .verticalScroll(rememberScrollState())
                .padding(top = 0.dp, bottom = 24.dp),
        ) {
            Spacer(Modifier.height(10.dp))
            // Check-in card
            Column(
                Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .demoCard()
                    .padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "连签可得更多积分",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = DemoColors.TextPrimary,
                        )
                        Spacer(Modifier.height(4.dp))
                        Row {
                            Text("已连续签到 ", fontSize = 12.sp, color = DemoColors.TextSecondary)
                            Text("$streak", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = headerBlue)
                            Text(" 天", fontSize = 12.sp, color = DemoColors.TextSecondary)
                        }
                    }
                    Text(
                        when {
                            checkedToday -> "已签到"
                            checkingIn -> "签到中…"
                            else -> "立即签到"
                        },
                        color = if (checkedToday) DemoColors.Muted else Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .clip(DemoRadius.Pill)
                            .background(if (checkedToday) Color(0xFFF5F6F8) else headerBlue)
                            .clickable(enabled = !checkedToday && !checkingIn) {
                                scope.launch {
                                    checkingIn = true
                                    val res = HomePointsStore.checkIn()
                                    checkingIn = false
                                    if (res != null) {
                                        showPlatformToast("签到成功，+${res.points}积分")
                                    } else {
                                        showPlatformToast(HomePointsStore.errorMessage ?: "今日已签到")
                                    }
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth()) {
                    calendar.forEach { day ->
                        val bg = when {
                            day.signed -> headerBlue
                            day.isToday -> coinGold
                            else -> Color(0xFFF5F6F8)
                        }
                        val fg = when {
                            day.signed || day.isToday -> Color.White
                            else -> DemoColors.TextSecondary
                        }
                        Column(
                            Modifier.weight(1f).padding(horizontal = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(bg),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text("+${day.reward}", color = fg, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                // Flutter Icons.arrow_forward
                                Text("→", color = fg.copy(alpha = 0.8f), fontSize = 10.sp)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                if (day.signed) "已签" else day.label,
                                fontSize = 11.sp,
                                color = if (day.signed) headerBlue else DemoColors.TextSecondary,
                                maxLines = 1,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("断签或者签完需重新开始", fontSize = 12.sp, color = DemoColors.Muted)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("签到提醒", fontSize = 12.sp, color = DemoColors.TextSecondary)
                        Spacer(Modifier.width(8.dp))
                        Box(
                            Modifier
                                .width(44.dp)
                                .height(24.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (remind) headerBlue else DemoColors.Muted.copy(alpha = 0.3f))
                                .clickable { remind = !remind }
                                .padding(2.dp),
                            contentAlignment = if (remind) Alignment.CenterEnd else Alignment.CenterStart,
                        ) {
                            Box(
                                Modifier
                                    .size(20.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "成长任务",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = DemoColors.TextPrimary,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(12.dp))
            Column(
                Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .demoCard(),
            ) {
                tasks.forEachIndexed { idx, task ->
                    if (idx > 0) {
                        HorizontalDivider(
                            color = DemoColors.Divider,
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(start = 56.dp, end = 16.dp),
                        )
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFE8F1FA)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(task.icon, fontSize = 16.sp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                task.title,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                color = DemoColors.TextPrimary,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text("+${task.points}积分", fontSize = 12.sp, color = DemoColors.TextSecondary)
                        }
                        // Flutter solid primary pill (4.dp), white label.
                        Text(
                            task.action,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(headerBlue)
                                .clickable {
                                    when (task.action) {
                                        "领取" -> scope.launch {
                                            val gained = HomePointsStore.claimTask(task.code)
                                            if (gained != null) {
                                                showPlatformToast("领取成功，+${gained}积分")
                                            } else {
                                                showPlatformToast(HomePointsStore.errorMessage ?: "领取失败")
                                            }
                                        }
                                        "去完成" -> showPlatformToast("去完成：${task.title}")
                                        else -> showPlatformToast(task.action)
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "积分换礼",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = DemoColors.TextPrimary,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(12.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Flutter Icons.card_giftcard_outlined @ 80.sp, textHint@0.3
                Text(
                    "❐",
                    fontSize = 72.sp,
                    color = DemoColors.Muted.copy(alpha = 0.3f),
                )
                Spacer(Modifier.height(16.dp))
                Text("暂无积分商品", fontSize = 14.sp, color = DemoColors.Muted)
            }
        }
    }
}

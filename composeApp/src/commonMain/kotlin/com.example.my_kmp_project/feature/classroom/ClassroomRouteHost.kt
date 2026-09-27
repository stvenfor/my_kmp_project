package com.example.my_kmp_project.feature.classroom

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.MineTopBar
import com.example.my_kmp_project.core.platform.showPlatformToast
import com.example.my_kmp_project.core.ui.ReportMainTabRoot

internal object ClassroomRoutes {
    const val MyClass = "/classroom/my_class"
    const val HomeworkStats = "/classroom/homework_stats"
    const val HomeworkTeacher = "/classroom/homework/detail_teacher"
    const val HomeworkStudent = "/classroom/homework/detail_student"
    const val HomeworkDubbing = "/classroom/homework/dubbing"
    const val HomeworkReview = "/classroom/homework/review"
    const val GiftClaim = "/classroom/gift/claim"
    const val VideoDetail = "/classroom/video/detail"

    fun fromLabel(label: String): String? = when (label.trim()) {
        "我的课程", "课程", "课堂", "我的班级", "班级教学", "培训" -> MyClass
        "作业统计" -> HomeworkStats
        "领取礼品卡", "礼品卡" -> GiftClaim
        else -> null
    }
}

private data class HomeworkRow(val id: String, val title: String, val type: String, val due: String)

private object ClassroomHwMock {
    val rows = listOf(
        HomeworkRow("h1", "语法练习 3", "书面", "今日 23:59"),
        HomeworkRow("h2", "配音作业 · 致橡树", "配音", "明日 18:00"),
        HomeworkRow("h3", "听力精听", "听力", "本周六"),
    )
}

@Composable
internal fun ClassroomRouteHost(
    route: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit = {},
) {
    when (route) {
        ClassroomRoutes.MyClass -> ClassroomScreen(
            onBack = onBack,
            onHomeworkStats = { onNavigate(ClassroomRoutes.HomeworkStats) },
        )
        ClassroomRoutes.HomeworkStats -> HomeworkStatsPage(
            onBack = onBack,
            onTeacher = { onNavigate(ClassroomRoutes.HomeworkTeacher) },
            onStudent = { onNavigate(ClassroomRoutes.HomeworkStudent) },
            onDubbing = { onNavigate(ClassroomRoutes.HomeworkDubbing) },
            onReview = { onNavigate(ClassroomRoutes.HomeworkReview) },
            onVideo = { onNavigate(ClassroomRoutes.VideoDetail) },
            onGift = { onNavigate(ClassroomRoutes.GiftClaim) },
        )
        ClassroomRoutes.HomeworkTeacher -> HomeworkDetailPage("教师作业详情", "批改进度 12/30 · 平均分 86", onBack)
        ClassroomRoutes.HomeworkStudent -> HomeworkDetailPage("学生作业详情", "已提交 · 待批改 · 附件 2", onBack)
        ClassroomRoutes.HomeworkDubbing -> HomeworkDetailPage("配音作业", "录制入口 stub · 见 media gap", onBack) {
            showPlatformToast("开始录制（mock）")
        }
        ClassroomRoutes.HomeworkReview -> HomeworkDetailPage("作业点评", "教师评语：语速适中，注意连读。", onBack)
        ClassroomRoutes.GiftClaim -> GiftClaimPage(onBack = onBack)
        ClassroomRoutes.VideoDetail -> HomeworkDetailPage("课堂视频", "播放头 mock · 非 short 链路", onBack)
        else -> ClassroomScreen(onBack = onBack, onHomeworkStats = { onNavigate(ClassroomRoutes.HomeworkStats) })
    }
}

@Composable
private fun HomeworkStatsPage(
    onBack: () -> Unit,
    onTeacher: () -> Unit,
    onStudent: () -> Unit,
    onDubbing: () -> Unit,
    onReview: () -> Unit,
    onVideo: () -> Unit,
    onGift: () -> Unit,
) {
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(DemoColors.PageBg)) {
        MineTopBar(title = "作业统计", onBack = onBack)
        LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Text("班级作业概览（mock）", color = DemoColors.TextSecondary, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
            }
            items(ClassroomHwMock.rows, key = { it.id }) { row ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(DemoColors.Background)
                        .clickable {
                            when (row.type) {
                                "配音" -> onDubbing()
                                else -> onTeacher()
                            }
                        }
                        .padding(14.dp),
                ) {
                    Text(row.title, fontWeight = FontWeight.SemiBold, color = DemoColors.TextPrimary)
                    Text("${row.type} · 截止 ${row.due}", fontSize = 12.sp, color = DemoColors.Muted)
                }
            }
            item {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onStudent) { Text("学生视角详情 →", color = DemoColors.Primary) }
                TextButton(onClick = onReview) { Text("作业点评 →", color = DemoColors.Primary) }
                TextButton(onClick = onVideo) { Text("课堂视频 →", color = DemoColors.Primary) }
                TextButton(onClick = onGift) { Text("领取礼品卡 →", color = DemoColors.Primary) }
            }
        }
    }
}

@Composable
private fun HomeworkDetailPage(
    title: String,
    body: String,
    onBack: () -> Unit,
    onAction: (() -> Unit)? = null,
) {
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(DemoColors.PageBg)) {
        MineTopBar(title = title, onBack = onBack)
        Text(body, modifier = Modifier.padding(16.dp), color = DemoColors.TextPrimary, fontSize = 15.sp)
        if (onAction != null) {
            Button(
                onClick = onAction,
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = DemoColors.Primary),
            ) {
                Text("操作", color = DemoColors.OnPrimary)
            }
        }
    }
}

@Composable
private fun GiftClaimPage(onBack: () -> Unit) {
    // Flutter ClaimGiftCardPage + ClassroomMockData.giftCard
    var claimed by remember { mutableStateOf(false) }
    val noteBg = Color(0xFFF7F5F0)
    val green = Color(0xFF34C759)
    ReportMainTabRoot(isRoot = false)
    Column(
        Modifier
            .fillMaxSize()
            .background(noteBg),
    ) {
        MineTopBar(title = "领取礼品卡", onBack = onBack, containerColor = noteBg)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .padding(16.dp),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(listOf(Color(0xFF1677FF), Color(0xFF0958D9))),
                        )
                        .padding(16.dp),
                ) {
                    Column(Modifier.fillMaxSize()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color.White),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("🦜", fontSize = 14.sp)
                            }
                            Spacer(Modifier.width(8.dp))
                            Text("iHome", color = Color.White, fontSize = 13.sp)
                        }
                        Spacer(Modifier.weight(1f))
                        Text(
                            "Way to go ✨",
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            fontStyle = FontStyle.Italic,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        )
                        Spacer(Modifier.weight(1f))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Column {
                                Text("1天 AI SVIP", color = Color.White, fontSize = 14.sp)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "班级会员卡",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.White.copy(alpha = 0.2f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp),
                                )
                            }
                            Spacer(Modifier.weight(1f))
                            Text("🧑‍🎓", fontSize = 48.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White)
                    .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 20.dp),
            ) {
                Text("📎", fontSize = 18.sp, color = Color(0xFF999999))
                Spacer(Modifier.height(4.dp))
                Text("乌克丽丽 同学：", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                Spacer(Modifier.height(12.dp))
                Text(
                    "本次作业完成的很棒！老师送你一张体验卡，以资鼓励",
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    color = DemoColors.TextPrimary,
                )
                Spacer(Modifier.height(24.dp))
                Column(Modifier.align(Alignment.End), horizontalAlignment = Alignment.End) {
                    Text("老坛酸菜", fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("2026-05-20", fontSize = 13.sp, color = DemoColors.TextSecondary)
                }
            }
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = {
                    if (claimed) return@Button
                    claimed = true
                    showPlatformToast("领取成功，可在背包中查看")
                },
                enabled = !claimed,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = green,
                    disabledContainerColor = DemoColors.TextSecondary.copy(alpha = 0.4f),
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            ) {
                Text(
                    if (claimed) "已领取" else "立即领取",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

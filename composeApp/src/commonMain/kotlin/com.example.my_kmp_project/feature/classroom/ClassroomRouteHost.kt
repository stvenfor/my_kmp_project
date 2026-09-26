package com.example.my_kmp_project.feature.classroom

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    var claimed by remember { mutableStateOf(false) }
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(DemoColors.PageBg)) {
        MineTopBar(title = "领取礼品卡", onBack = onBack)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                if (claimed) "已领取 · 卡号 GC-2026-001" else "完成课堂任务后可领取礼品卡",
                color = DemoColors.TextPrimary,
            )
            Button(
                onClick = {
                    claimed = true
                    showPlatformToast("领取成功（mock）")
                },
                enabled = !claimed,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = DemoColors.Primary),
            ) {
                Text(if (claimed) "已领取" else "立即领取", color = DemoColors.OnPrimary)
            }
        }
    }
}

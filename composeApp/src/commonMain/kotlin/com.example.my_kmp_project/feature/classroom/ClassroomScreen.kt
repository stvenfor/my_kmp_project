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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.MineTopBar
import com.example.my_kmp_project.core.platform.showPlatformToast
import com.example.my_kmp_project.core.ui.ReportMainTabRoot

internal data class ClassItem(
    val id: String,
    val title: String,
    val schedule: String,
    val teacher: String,
    val outline: List<String>,
)

private object ClassroomMockData {
    val classes = listOf(
        ClassItem(
            id = "1",
            title = "基础语法第 3 课",
            schedule = "今日 19:00",
            teacher = "王老师",
            outline = listOf("时态复习", "练习题 1–10", "课堂测验"),
        ),
        ClassItem(
            id = "2",
            title = "听力精听工作坊",
            schedule = "明日 10:00",
            teacher = "李老师",
            outline = listOf("精听片段", "跟读练习", "答疑"),
        ),
        ClassItem(
            id = "3",
            title = "写作批改答疑",
            schedule = "本周六 15:00",
            teacher = "张老师",
            outline = listOf("范文讲解", "常见错误", "作业点评"),
        ),
    )
}

private enum class ClassroomRoute {
    List,
    Detail,
    Schedule,
}

/**
 * Classroom multi-page: list → detail → schedule (mock; not list-only).
 */
@Composable
internal fun ClassroomScreen(
    onBack: () -> Unit,
    onHomeworkStats: (() -> Unit)? = null,
) {
    var route by remember { mutableStateOf(ClassroomRoute.List) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    val selected = selectedId?.let { id -> ClassroomMockData.classes.firstOrNull { it.id == id } }

    ReportMainTabRoot(isRoot = false)
    when (route) {
        ClassroomRoute.List -> ClassroomListContent(
            classes = ClassroomMockData.classes,
            onBack = onBack,
            onOpen = {
                selectedId = it
                route = ClassroomRoute.Detail
            },
            onOpenSchedule = { route = ClassroomRoute.Schedule },
            onHomeworkStats = onHomeworkStats,
        )
        ClassroomRoute.Detail -> {
            if (selected == null) {
                route = ClassroomRoute.List
            } else {
                ClassroomDetailScreen(
                    item = selected,
                    onBack = {
                        selectedId = null
                        route = ClassroomRoute.List
                    },
                    onOpenSchedule = { route = ClassroomRoute.Schedule },
                )
            }
        }
        ClassroomRoute.Schedule -> ClassroomScheduleScreen(
            classes = ClassroomMockData.classes,
            onBack = {
                route = if (selectedId != null) ClassroomRoute.Detail else ClassroomRoute.List
            },
        )
    }
}

@Composable
private fun ClassroomListContent(
    classes: List<ClassItem>,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    onOpenSchedule: () -> Unit,
    onHomeworkStats: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DemoColors.PageBg),
    ) {
        MineTopBar(
            title = "我的班级",
            onBack = onBack,
            containerColor = DemoColors.PageBg,
            actions = {
                Text(
                    text = "课表",
                    color = DemoColors.Primary,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clickable(onClick = onOpenSchedule)
                        .padding(horizontal = 12.dp),
                )
            },
        )
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("班级", color = DemoColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text(
                        text = "禁用班级",
                        color = DemoColors.Primary,
                        fontSize = 14.sp,
                        modifier = Modifier.clickable {
                            showPlatformToast("禁用班级功能开发中")
                        },
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            items(classes, key = { it.id }) { row ->
                val members = 12 + (row.id.toIntOrNull() ?: 0)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .shadow(4.dp, RoundedCornerShape(8.dp))
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White),
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onOpen(row.id) }
                            .padding(16.dp),
                    ) {
                        Text(
                            text = row.title,
                            color = DemoColors.TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("邀请码：${row.id}ABC", color = DemoColors.Muted, fontSize = 13.sp)
                            Text("班级成员：$members", color = DemoColors.Muted, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${row.schedule} · ${row.teacher}", color = DemoColors.TextSecondary, fontSize = 12.sp)
                    }
                    HorizontalDivider(color = DemoColors.Divider, thickness = 0.5.dp)
                    Row(Modifier.fillMaxWidth()) {
                        ClassActionCell("邀请同学", Modifier.weight(1f)) {
                            showPlatformToast("邀请同学功能开发中")
                        }
                        ClassActionCell("作业统计", Modifier.weight(1f)) {
                            onHomeworkStats?.invoke() ?: showPlatformToast("作业统计")
                        }
                        ClassActionCell("排行榜", Modifier.weight(1f)) {
                            showPlatformToast("排行榜功能开发中")
                        }
                    }
                    Text(
                        "作业点评 >",
                        color = DemoColors.Primary,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp, top = 4.dp)
                            .clickable { onHomeworkStats?.invoke() },
                    )
                }
            }
        }
        Button(
            onClick = { showPlatformToast("创建班级功能开发中") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(48.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DemoColors.Primary),
            elevation = ButtonDefaults.buttonElevation(0.dp),
        ) {
            Text("创建班级", color = DemoColors.OnPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun ClassActionCell(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Text(
        text = label,
        color = DemoColors.TextPrimary,
        fontSize = 13.sp,
        textAlign = TextAlign.Center,
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
    )
}

@Composable
private fun ClassroomDetailScreen(
    item: ClassItem,
    onBack: () -> Unit,
    onOpenSchedule: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DemoColors.PageBg),
    ) {
        MineTopBar(title = item.title, onBack = onBack, containerColor = DemoColors.PageBg)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(text = "授课：${item.teacher}", color = DemoColors.TextPrimary, fontSize = 15.sp)
            Text(text = "时间：${item.schedule}", color = DemoColors.TextSecondary, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "本节大纲", fontWeight = FontWeight.SemiBold, color = DemoColors.TextPrimary)
            item.outline.forEachIndexed { index, line ->
                Text(
                    text = "${index + 1}. $line",
                    color = DemoColors.TextSecondary,
                    fontSize = 14.sp,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onOpenSchedule,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DemoColors.Primary,
                    contentColor = DemoColors.OnPrimary,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("查看完整课表")
            }
        }
    }
}

@Composable
private fun ClassroomScheduleScreen(
    classes: List<ClassItem>,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DemoColors.PageBg),
    ) {
        MineTopBar(title = "课表", onBack = onBack, containerColor = DemoColors.PageBg)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(classes, key = { it.id }) { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DemoColors.Background)
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = row.title,
                            color = DemoColors.TextPrimary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                        )
                        Text(
                            text = row.teacher,
                            color = DemoColors.Muted,
                            fontSize = 12.sp,
                        )
                    }
                    Text(
                        text = row.schedule,
                        color = DemoColors.Primary,
                        fontSize = 13.sp,
                    )
                }
            }
        }
    }
}

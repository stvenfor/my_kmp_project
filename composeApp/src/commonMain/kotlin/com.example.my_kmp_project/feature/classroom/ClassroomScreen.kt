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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.MineTopBar
import com.example.my_kmp_project.feature.shell.ReportMainTabRoot

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
internal fun ClassroomScreen(onBack: () -> Unit) {
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
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DemoColors.PageBg),
    ) {
        MineTopBar(
            title = "课堂",
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
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "列表 → 详情 → 课表 · 教室实时态见 gap registry",
                    color = DemoColors.TextSecondary,
                    fontSize = 13.sp,
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            items(classes, key = { it.id }) { row ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpen(row.id) }
                        .padding(vertical = 12.dp),
                ) {
                    Text(
                        text = row.title,
                        color = DemoColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${row.schedule} · ${row.teacher}",
                        color = DemoColors.Muted,
                        fontSize = 12.sp,
                    )
                }
                HorizontalDivider(color = DemoColors.Divider)
            }
        }
    }
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

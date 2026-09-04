package com.example.my_kmp_project.feature.friend

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.MineTopBar
import com.example.my_kmp_project.core.ui.ReportMainTabRoot

internal data class FriendItem(
    val id: String,
    val name: String,
    val lastSeen: String,
    val remark: String,
)

private object FriendMockData {
    val friends = listOf(
        FriendItem("1", "小明", "刚刚在线", "班级同学 · 周末约图书馆"),
        FriendItem("2", "阿哲", "三天前", "口语搭子"),
        FriendItem("3", "林林", "一周前", "活动组织"),
        FriendItem("4", "客服小助手", "昨天", "官方客服"),
    )
}

/**
 * Friend list → detail graph (mock relation data; realtime IM still registry `partial`).
 */
@Composable
internal fun FriendScreen(onBack: () -> Unit) {
    var selectedId by remember { mutableStateOf<String?>(null) }
    val selected = selectedId?.let { id -> FriendMockData.friends.firstOrNull { it.id == id } }

    if (selected != null) {
        ReportMainTabRoot(isRoot = false)
        FriendDetailScreen(
            friend = selected,
            onBack = { selectedId = null },
        )
    } else {
        ReportMainTabRoot(isRoot = false)
        FriendListContent(
            friends = FriendMockData.friends,
            onBack = onBack,
            onOpen = { selectedId = it },
        )
    }
}

@Composable
private fun FriendListContent(
    friends: List<FriendItem>,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DemoColors.PageBg),
    ) {
        MineTopBar(title = "好友", onBack = onBack, containerColor = DemoColors.PageBg)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "共 ${friends.size} 位好友 · 关系链/IM SDK 见 gap registry",
                    color = DemoColors.TextSecondary,
                    fontSize = 13.sp,
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            items(friends, key = { it.id }) { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpen(row.id) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(DemoColors.Accent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = row.name.take(1),
                            color = DemoColors.Accent,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = row.name,
                            color = DemoColors.TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = row.lastSeen,
                            color = DemoColors.Muted,
                            fontSize = 13.sp,
                        )
                    }
                }
                HorizontalDivider(color = DemoColors.Divider, thickness = 0.5.dp)
            }
        }
    }
}

@Composable
private fun FriendDetailScreen(
    friend: FriendItem,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DemoColors.PageBg),
    ) {
        MineTopBar(title = friend.name, onBack = onBack, containerColor = DemoColors.PageBg)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = friend.remark,
                color = DemoColors.TextPrimary,
                fontSize = 15.sp,
            )
            Text(
                text = "最近活跃：${friend.lastSeen}",
                color = DemoColors.TextSecondary,
                fontSize = 13.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { /* IM entry reserved — MockImEngine lives under Chat tab */ },
                colors = ButtonDefaults.buttonColors(
                    containerColor = DemoColors.Primary,
                    contentColor = DemoColors.OnPrimary,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("发消息（走聊天 Tab / IM 引擎）")
            }
            Text(
                text = "实时关系链与推送未接 SDK，本页仅导航与资料展示。",
                color = DemoColors.Muted,
                fontSize = 12.sp,
            )
        }
    }
}

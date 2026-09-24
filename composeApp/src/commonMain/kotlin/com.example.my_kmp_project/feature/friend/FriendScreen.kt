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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.MineTopBar
import com.example.my_kmp_project.core.platform.showPlatformToast
import com.example.my_kmp_project.core.ui.ReportMainTabRoot

internal data class FriendItem(
    val id: String,
    val name: String,
    val lastSeen: String,
    val remark: String,
)

private data class IncomingRequest(val id: String, val name: String)

private object FriendMockData {
    val friends = listOf(
        FriendItem("1", "小明", "刚刚在线", "班级同学 · 周末约图书馆"),
        FriendItem("2", "阿哲", "三天前", "口语搭子"),
        FriendItem("3", "林林", "一周前", "活动组织"),
        FriendItem("4", "客服小助手", "昨天", "官方客服"),
    )
    val directory = listOf(
        FriendItem("9", "新同学小周", "未添加", "同校"),
        FriendItem("10", "外教 Anna", "未添加", "口语"),
    )
}

/**
 * IM 通讯录：搜索 / 新的朋友 / 好友列表 / 建群（mock；真 IM 见 gap）。
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
    var query by remember { mutableStateOf("") }
    var searchHits by remember { mutableStateOf<List<FriendItem>>(emptyList()) }
    var incoming by remember {
        mutableStateOf(
            listOf(
                IncomingRequest("i1", "王同学"),
                IncomingRequest("i2", "李老师"),
            ),
        )
    }
    var friendList by remember { mutableStateOf(friends) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DemoColors.PageBg),
    ) {
        MineTopBar(
            title = "通讯录",
            onBack = onBack,
            containerColor = DemoColors.PageBg,
            actions = {
                TextButton(onClick = { showPlatformToast("建群成功（mock）· 请到聊天 Tab") }) {
                    Text("建群", color = DemoColors.Accent)
                }
            },
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(DemoColors.Background)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        textStyle = TextStyle(fontSize = 15.sp, color = DemoColors.TextPrimary),
                        modifier = Modifier.weight(1f),
                        decorationBox = { inner ->
                            if (query.isEmpty()) Text("搜索好友", color = DemoColors.Muted)
                            inner()
                        },
                    )
                    Text(
                        "搜索",
                        color = DemoColors.Primary,
                        fontSize = 14.sp,
                        modifier = Modifier.clickable {
                            if (query.isBlank()) {
                                searchHits = emptyList()
                            } else {
                                searchHits = FriendMockData.directory.filter {
                                    it.name.contains(query.trim())
                                }
                                if (searchHits.isEmpty()) {
                                    showPlatformToast("未找到用户")
                                }
                            }
                        },
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            if (searchHits.isNotEmpty()) {
                item {
                    Text("搜索结果", fontWeight = FontWeight.SemiBold, color = DemoColors.TextPrimary)
                    Spacer(Modifier.height(8.dp))
                }
                items(searchHits, key = { "hit-${it.id}" }) { hit ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Avatar(hit.name)
                        Spacer(Modifier.width(12.dp))
                        Text(hit.name, Modifier.weight(1f), color = DemoColors.TextPrimary)
                        Text(
                            "加好友",
                            color = DemoColors.Primary,
                            fontSize = 13.sp,
                            modifier = Modifier.clickable {
                                showPlatformToast("已发送好友申请")
                            },
                        )
                    }
                    HorizontalDivider(color = DemoColors.Divider, thickness = 0.5.dp)
                }
            }
            item {
                Spacer(Modifier.height(12.dp))
                Text(
                    "新的朋友（${incoming.size}）",
                    fontWeight = FontWeight.SemiBold,
                    color = DemoColors.TextPrimary,
                )
                Spacer(Modifier.height(8.dp))
            }
            items(incoming, key = { it.id }) { req ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Avatar(req.name)
                    Spacer(Modifier.width(12.dp))
                    Text(req.name, Modifier.weight(1f), color = DemoColors.TextPrimary)
                    Text(
                        "接受",
                        color = DemoColors.Primary,
                        fontSize = 13.sp,
                        modifier = Modifier.clickable {
                            friendList = friendList + FriendItem(req.id, req.name, "刚刚", "新朋友")
                            incoming = incoming.filterNot { it.id == req.id }
                            showPlatformToast("已添加")
                        },
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "拒绝",
                        color = DemoColors.Muted,
                        fontSize = 13.sp,
                        modifier = Modifier.clickable {
                            incoming = incoming.filterNot { it.id == req.id }
                        },
                    )
                }
                HorizontalDivider(color = DemoColors.Divider, thickness = 0.5.dp)
            }
            item {
                Spacer(Modifier.height(12.dp))
                Text(
                    "好友（${friendList.size}）",
                    fontWeight = FontWeight.SemiBold,
                    color = DemoColors.TextPrimary,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (friendList.isEmpty()) {
                item {
                    Text("暂无好友", color = DemoColors.Muted, fontSize = 13.sp)
                }
            }
            items(friendList, key = { it.id }) { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpen(row.id) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Avatar(row.name)
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
private fun Avatar(name: String) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(DemoColors.Accent.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.take(1),
            color = DemoColors.Accent,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
        )
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
                onClick = { showPlatformToast("请到聊天 Tab 打开会话（mock）") },
                colors = ButtonDefaults.buttonColors(
                    containerColor = DemoColors.Primary,
                    contentColor = DemoColors.OnPrimary,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("发消息")
            }
        }
    }
}

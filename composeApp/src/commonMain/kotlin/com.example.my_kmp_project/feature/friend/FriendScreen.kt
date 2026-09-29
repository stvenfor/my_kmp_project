package com.example.my_kmp_project.feature.friend

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.MineTopBar
import com.example.my_kmp_project.core.platform.showPlatformToast
import com.example.my_kmp_project.core.ui.PlatformNetworkImage
import com.example.my_kmp_project.core.ui.ReportMainTabRoot
import my_kmp_project.composeapp.generated.resources.Res
import my_kmp_project.composeapp.generated.resources.community_avatar

internal data class FriendItem(
    val id: String,
    val name: String,
    val phoneMasked: String,
    val avatarUrl: String,
    val remark: String = "",
)

private data class IncomingRequest(
    val id: String,
    val name: String,
    val phoneMasked: String,
    val avatarUrl: String,
)

/**
 * Friend list + detail — logic aligned with MockIm seed peers (logic-first).
 * Accept / 发消息 → [FriendDirectory.ensureChat] → shared [ImEngineStore].
 */
private object FriendMockData {
    val friends = FriendDirectory.seedFriends.map { it.toFriendItem() }
    val directory = FriendDirectory.directory.map { it.toFriendItem() }
}

@Composable
internal fun FriendScreen(onBack: () -> Unit) {
    var selectedId by remember { mutableStateOf<String?>(null) }
    var friendList by remember { mutableStateOf(FriendMockData.friends) }
    val selected = selectedId?.let { id -> friendList.firstOrNull { it.id == id } }

    if (selected != null) {
        ReportMainTabRoot(isRoot = false)
        FriendDetailScreen(friend = selected, onBack = { selectedId = null })
    } else {
        ReportMainTabRoot(isRoot = false)
        FriendListContent(
            friends = friendList,
            onFriendsChange = { friendList = it },
            onBack = onBack,
            onOpen = { selectedId = it },
        )
    }
}

@Composable
private fun FriendListContent(
    friends: List<FriendItem>,
    onFriendsChange: (List<FriendItem>) -> Unit,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var searchHits by remember { mutableStateOf<List<FriendItem>>(emptyList()) }
    // Flutter SoT capture: incoming empty → no「新的朋友」section.
    var incoming by remember { mutableStateOf<List<IncomingRequest>>(emptyList()) }
    val friendList = friends

    Column(Modifier.fillMaxSize().background(DemoColors.PageBg)) {
        MineTopBar(
            title = "通讯录",
            onBack = onBack,
            containerColor = DemoColors.PageBg,
            actions = {
                TextButton(onClick = {
                    if (friendList.isEmpty()) {
                        showPlatformToast("请先添加好友再建群")
                    } else {
                        val id = FriendDirectory.ensureGroupChat(
                            memberPeerIds = friendList.map { it.id },
                            title = friendList.take(3).joinToString("、") { it.name } + "的群聊",
                        )
                        showPlatformToast("建群成功 · $id")
                    }
                }) {
                    Text("建群", color = DemoColors.Accent, fontWeight = FontWeight.SemiBold)
                }
            },
        )
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                // Flutter _SearchBar: field + FilledButton 搜索
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DemoColors.Background)
                            .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("⌕", fontSize = 16.sp, color = DemoColors.Muted)
                        Spacer(Modifier.width(8.dp))
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            textStyle = TextStyle(fontSize = 15.sp, color = DemoColors.TextPrimary),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            decorationBox = { inner ->
                                if (query.isEmpty()) {
                                    Text("手机号或用户 ID", color = DemoColors.Muted, fontSize = 14.sp)
                                }
                                inner()
                            },
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Box(
                        Modifier
                            .height(48.dp)
                            .width(72.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DemoColors.Accent)
                            .clickable {
                                val q = query.trim()
                                if (q.isEmpty()) {
                                    searchHits = emptyList()
                                } else {
                                    searchHits = FriendMockData.directory.filter {
                                        it.name.contains(q) || it.phoneMasked.contains(q) || it.id.contains(q)
                                    } + friendList.filter {
                                        it.name.contains(q) || it.phoneMasked.contains(q) || it.id.contains(q)
                                    }
                                    if (searchHits.isEmpty()) showPlatformToast("未找到用户")
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("搜索", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                    }
                }
            }
            if (searchHits.isNotEmpty()) {
                item {
                    SectionHeader("搜索结果")
                    GroupedCard {
                        searchHits.forEachIndexed { i, hit ->
                            FriendRow(
                                name = hit.name,
                                subtitle = hit.phoneMasked.ifBlank { hit.id },
                                avatarUrl = hit.avatarUrl,
                                trailing = {
                                        PillButton("加好友") {
                                            FriendDirectory.ensureChat(hit.id, hit.name)
                                            onFriendsChange((friendList + hit).distinctBy { it.id })
                                            searchHits = emptyList()
                                            showPlatformToast("已添加并创建会话")
                                        }
                                },
                            )
                            if (i < searchHits.lastIndex) {
                                HorizontalDivider(
                                    Modifier.padding(start = 70.dp),
                                    color = DemoColors.Divider,
                                    thickness = 0.5.dp,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
            if (incoming.isNotEmpty()) {
                item {
                    SectionHeader("新的朋友", badge = incoming.size)
                    GroupedCard {
                        incoming.forEachIndexed { i, req ->
                            FriendRow(
                                name = req.name,
                                subtitle = if (req.phoneMasked.isBlank()) {
                                    "请求加你为好友"
                                } else {
                                    "${req.phoneMasked} · 请求加你为好友"
                                },
                                avatarUrl = req.avatarUrl,
                                trailing = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        OutlinedPill("拒绝") {
                                            incoming = incoming.filterNot { it.id == req.id }
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        PillButton("同意") {
                                            FriendDirectory.ensureChat(req.id, req.name)
                                            onFriendsChange(
                                                friendList + FriendItem(
                                                    req.id, req.name, req.phoneMasked, req.avatarUrl, "新朋友",
                                                ),
                                            )
                                            incoming = incoming.filterNot { it.id == req.id }
                                            showPlatformToast("已添加并创建会话")
                                        }
                                    }
                                },
                            )
                            if (i < incoming.lastIndex) {
                                HorizontalDivider(
                                    Modifier.padding(start = 70.dp),
                                    color = DemoColors.Divider,
                                    thickness = 0.5.dp,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
            item {
                // Flutter: title left + trailing count (not badge / not「好友（n）」)
                SectionHeader("好友", trailing = "${friendList.size}")
                if (friendList.isEmpty()) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("还没有好友", color = DemoColors.Muted, fontSize = 15.sp)
                    }
                } else {
                    GroupedCard {
                        friendList.forEachIndexed { i, row ->
                            FriendRow(
                                name = row.name,
                                subtitle = row.phoneMasked.ifBlank { row.id },
                                avatarUrl = row.avatarUrl,
                                onClick = { onOpen(row.id) },
                            )
                            if (i < friendList.lastIndex) {
                                HorizontalDivider(
                                    Modifier.padding(start = 70.dp),
                                    color = DemoColors.Divider,
                                    thickness = 0.5.dp,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, badge: Int? = null, trailing: String? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, fontSize = 12.sp, color = DemoColors.TextSecondary)
        if (badge != null && badge > 0) {
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFEE0000))
                    .padding(horizontal = 7.dp, vertical = 2.dp),
            ) {
                Text("$badge", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.weight(1f))
        if (trailing != null) {
            Text(trailing, fontSize = 12.sp, color = DemoColors.TextSecondary)
        }
    }
}

@Composable
private fun GroupedCard(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(DemoColors.Background)
            .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(8.dp)),
    ) { content() }
}

@Composable
private fun FriendRow(
    name: String,
    subtitle: String,
    avatarUrl: String,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NetworkAvatar(name = name, url = avatarUrl)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                name,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = DemoColors.TextPrimary,
                maxLines = 1,
            )
            Spacer(Modifier.height(2.dp))
            Text(subtitle, fontSize = 12.sp, color = DemoColors.TextSecondary, maxLines = 1)
        }
        trailing?.invoke()
    }
}

@Composable
private fun PillButton(label: String, onClick: () -> Unit) {
    Text(
        label,
        color = Color.White,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(DemoColors.Accent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun OutlinedPill(label: String, onClick: () -> Unit) {
    Text(
        label,
        color = DemoColors.TextSecondary,
        fontSize = 13.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun NetworkAvatar(name: String, url: String) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(DemoColors.PageBg),
        contentAlignment = Alignment.Center,
    ) {
        PlatformNetworkImage(
            url = url,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            placeholder = Res.drawable.community_avatar,
            contentDescription = name,
        )
    }
}

@Composable
private fun FriendDetailScreen(friend: FriendItem, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(DemoColors.PageBg)) {
        MineTopBar(title = friend.name, onBack = onBack, containerColor = DemoColors.Background)
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(DemoColors.Background)
                .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(8.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NetworkAvatar(friend.name, friend.avatarUrl)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(friend.name, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                    Text(friend.phoneMasked, fontSize = 13.sp, color = DemoColors.Muted)
                }
            }
            if (friend.remark.isNotBlank()) {
                Text(friend.remark, color = DemoColors.TextPrimary, fontSize = 15.sp)
            }
            TextButton(
                onClick = {
                    val id = FriendDirectory.ensureChat(friend.id, friend.name)
                    showPlatformToast("已打开会话 $id")
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("发消息", color = DemoColors.Accent, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

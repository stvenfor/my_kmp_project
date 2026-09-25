package com.example.my_kmp_project.nativeshell

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.MineTopBar
import com.example.my_kmp_project.core.platform.showPlatformToast
import com.example.my_kmp_project.core.ui.PlatformNetworkImage
import com.example.my_kmp_project.feature.chat.ChatDetailDeepLinkArgs
import com.example.my_kmp_project.feature.chat.ImConversation
import com.example.my_kmp_project.feature.chat.ImEngine
import com.example.my_kmp_project.feature.chat.MockImEngine
import com.example.my_kmp_project.feature.community.CommunityPublishBus
import com.example.my_kmp_project.feature.home.HomeRoutes
import com.example.my_kmp_project.feature.mine.MineHomeContent
import com.example.my_kmp_project.feature.mine.MineRoutes
import my_kmp_project.composeapp.generated.resources.Res
import my_kmp_project.composeapp.generated.resources.community_avatar
import my_kmp_project.composeapp.generated.resources.community_post_a
import org.jetbrains.compose.resources.painterResource
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.IconButton
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding

/** Flutter ChatTheme / ConversationListItem — WeChat-blue for @/#/reply prefixes. */
private val CommunityLinkBlue = Color(0xFF576B95)
private val ChatOnline = DemoColors.Accent
private val UnreadBadge = Color(0xFFEE0000)
private val LikeRed = Color(0xFFEE0000)

@Composable
internal fun JetpackChatRoot(
    onOpenContacts: () -> Unit = {},
    pendingDetail: ChatDetailDeepLinkArgs? = null,
    showMissingDetailParams: Boolean = false,
    onPendingDetailConsumed: () -> Unit = {},
    onMissingDetailConsumed: () -> Unit = {},
    onDetailVisibilityChanged: (Boolean) -> Unit = {},
) {
    val engine = remember { MockImEngine(seedDemo = true) }
    var searchOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var openId by remember { mutableStateOf<String?>(null) }
    var listEpoch by remember { mutableStateOf(0) }
    var missingDetail by remember { mutableStateOf(false) }

    LaunchedEffect(pendingDetail, showMissingDetailParams) {
        if (showMissingDetailParams) {
            missingDetail = true
            openId = null
            onMissingDetailConsumed()
            return@LaunchedEffect
        }
        val args = pendingDetail ?: return@LaunchedEffect
        engine.ensureConversation(
            id = args.id,
            title = args.peerName,
            lastMessage = args.lastMessage,
            unreadCount = args.unreadCount,
        )
        listEpoch += 1
        missingDetail = false
        openId = args.id
        onPendingDetailConsumed()
    }

    LaunchedEffect(openId, missingDetail) {
        onDetailVisibilityChanged(openId != null || missingDetail)
    }

    if (missingDetail) {
        Box(
            Modifier
                .fillMaxSize()
                .background(DemoColors.PageBg)
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("缺少会话参数", color = DemoColors.TextPrimary, fontSize = 16.sp)
                Spacer(Modifier.height(16.dp))
                Text(
                    "返回",
                    color = DemoColors.Accent,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable { missingDetail = false }
                        .padding(12.dp),
                )
            }
        }
        return
    }

    val conversations = remember(listEpoch) { engine.conversations() }
    val filtered = remember(conversations, query) {
        val q = query.trim()
        if (q.isEmpty()) conversations
        else conversations.filter {
            it.title.contains(q, ignoreCase = true) ||
                it.lastMessage.contains(q, ignoreCase = true)
        }
    }
    val open = openId?.let { id -> conversations.firstOrNull { it.id == id } }
    if (open != null) {
        JetpackChatDetail(
            conversation = open,
            engine = engine,
            onBack = {
                openId = null
                listEpoch += 1
            },
        )
        return
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(DemoColors.PageBg)
            .statusBarsPadding(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "消息",
                fontSize = 32.sp,
                fontWeight = FontWeight.SemiBold,
                color = DemoColors.TextPrimary,
                letterSpacing = (-1.6).sp,
                lineHeight = 36.sp,
                modifier = Modifier.weight(1f),
            )
            Box(
                Modifier
                    .size(44.dp)
                    .clickable {
                        searchOpen = !searchOpen
                        if (!searchOpen) query = ""
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (searchOpen) "✕" else "⌕",
                    color = DemoColors.Accent,
                    fontSize = 20.sp,
                )
            }
            Box(
                Modifier
                    .size(44.dp)
                    .clickable(onClick = onOpenContacts),
                contentAlignment = Alignment.Center,
            ) {
                // Flutter square_pencil → RoutePath.friend（通讯录）
                Text("✎", color = DemoColors.Accent, fontSize = 20.sp)
            }
        }
        if (searchOpen) {
            Row(
                Modifier
                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DemoColors.Background)
                    .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("⌕", color = DemoColors.Muted, fontSize = 18.sp)
                Spacer(Modifier.width(8.dp))
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    textStyle = TextStyle(fontSize = 14.sp, color = DemoColors.TextPrimary),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (query.isEmpty()) {
                            Text("搜索会话名称或消息", color = DemoColors.Muted, fontSize = 14.sp)
                        }
                        inner()
                    },
                )
            }
        }
        if (conversations.isEmpty()) {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                ChatConversationEmpty(
                    connectionHint = "IM 已连接",
                    onGoContacts = onOpenContacts,
                    onRefreshHint = { listEpoch += 1 },
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            ) {
                item {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DemoColors.Background)
                            .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(8.dp)),
                    ) {
                        if (filtered.isEmpty()) {
                            Text(
                                "没有匹配的会话",
                                color = DemoColors.TextSecondary,
                                modifier = Modifier.padding(16.dp),
                            )
                        } else {
                            filtered.forEachIndexed { index, conv ->
                                ChatConversationRow(
                                    peer = ChatPeer(
                                        name = conv.title,
                                        snippet = conv.lastMessage,
                                        time = conv.updatedAtLabel,
                                        badge = conv.unreadCount.takeIf { it > 0 }?.toString(),
                                        online = index % 2 == 0,
                                    ),
                                    onClick = { openId = conv.id },
                                )
                                if (index != filtered.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(start = 72.dp),
                                        thickness = 0.5.dp,
                                        color = DemoColors.Divider,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Flutter `ConversationListEmpty`. */
@Composable
private fun ChatConversationEmpty(
    connectionHint: String,
    onGoContacts: () -> Unit,
    onRefreshHint: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(DemoColors.Background)
                .border(0.5.dp, DemoColors.Divider, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("💬", fontSize = 36.sp, color = DemoColors.Muted)
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "还没有消息",
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = DemoColors.TextPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "加个好友，发一条问候吧",
            fontSize = 15.sp,
            color = DemoColors.TextSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            connectionHint,
            fontSize = 12.sp,
            color = DemoColors.Muted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DemoColors.Accent)
                .clickable(onClick = onGoContacts),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "去通讯录",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "下拉也可刷新",
            fontSize = 12.sp,
            color = DemoColors.Muted,
            modifier = Modifier.clickable(onClick = onRefreshHint),
        )
    }
}

@Composable
private fun JetpackChatDetail(
    conversation: ImConversation,
    engine: ImEngine,
    onBack: () -> Unit,
) {
    var epoch by remember { mutableStateOf(0) }
    val messages = remember(conversation.id, epoch) { engine.messages(conversation.id) }
    var draft by remember { mutableStateOf("") }
    var previewUrl by remember { mutableStateOf<String?>(null) }
    var sending by remember { mutableStateOf(false) }

    if (previewUrl != null) {
        ChatImagePreview(
            url = previewUrl!!,
            onBack = { previewUrl = null },
        )
        return
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(DemoColors.PageBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        MineTopBar(title = conversation.title, onBack = onBack, containerColor = DemoColors.PageBg)
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(messages, key = { it.id }) { bubble ->
                val isImage = bubble.body.startsWith("[image]")
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = if (bubble.isSelf) Arrangement.End else Arrangement.Start,
                ) {
                    Column(horizontalAlignment = if (bubble.isSelf) Alignment.End else Alignment.Start) {
                        if (isImage) {
                            val url = bubble.body.removePrefix("[image]").trim()
                            Box(
                                Modifier
                                    .size(140.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DemoColors.Background)
                                    .clickable { previewUrl = url.ifBlank { "https://picsum.photos/seed/chat/600" } },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("图片", color = DemoColors.Accent)
                            }
                        } else {
                            Text(
                                bubble.body,
                                color = if (bubble.isSelf) Color.White else DemoColors.TextPrimary,
                                fontSize = 15.sp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (bubble.isSelf) DemoColors.Accent else DemoColors.Background,
                                    )
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                            )
                        }
                        Text(
                            bubble.timeLabel,
                            fontSize = 11.sp,
                            color = DemoColors.Muted,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .background(DemoColors.Background)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "图",
                color = DemoColors.Accent,
                modifier = Modifier
                    .clickable {
                        engine.sendText(
                            conversation.id,
                            "[image]https://picsum.photos/seed/${conversation.id}/600",
                        )
                        epoch += 1
                    }
                    .padding(8.dp),
            )
            BasicTextField(
                value = draft,
                onValueChange = { draft = it },
                textStyle = TextStyle(fontSize = 15.sp, color = DemoColors.TextPrimary),
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(DemoColors.PageBg)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                decorationBox = { inner ->
                    if (draft.isEmpty()) {
                        Text("发送消息…", color = DemoColors.Muted, fontSize = 15.sp)
                    }
                    inner()
                },
            )
            Spacer(Modifier.width(8.dp))
            Text(
                if (sending) "…" else "发送",
                color = DemoColors.Accent,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clickable {
                        val text = draft.trim()
                        if (text.isEmpty() || sending) return@clickable
                        sending = true
                        val sent = engine.sendText(conversation.id, text)
                        if (sent != null) {
                            draft = ""
                            epoch += 1
                        } else {
                            showPlatformToast("发送失败，请重试")
                        }
                        sending = false
                    }
                    .padding(8.dp),
            )
        }
    }
}

@Composable
private fun ChatImagePreview(url: String, onBack: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .clickable(onClick = onBack),
        contentAlignment = Alignment.Center,
    ) {
        PlatformNetworkImage(
            url = url,
            contentDescription = "预览",
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentScale = ContentScale.Fit,
            placeholder = Res.drawable.community_post_a,
        )
        Text(
            "关闭",
            color = Color.White,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .clickable(onClick = onBack),
        )
    }
}

@Composable
private fun ChatConversationRow(peer: ChatPeer, onClick: () -> Unit) {
    // Flutter ConversationListItem: pad h16/v12, avatar 52, name|time then snippet|badge
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            Box(
                Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(DemoColors.Accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(peer.name.takeLast(1), color = DemoColors.Accent, fontWeight = FontWeight.SemiBold)
            }
            if (peer.online) {
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 1.dp, bottom = 1.dp)
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(ChatOnline)
                        .border(2.dp, DemoColors.Background, CircleShape),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    peer.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = DemoColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(peer.time, fontSize = 12.sp, color = DemoColors.TextSecondary)
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    peer.snippet,
                    fontSize = 14.sp,
                    color = DemoColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (peer.badge != null) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        peer.badge,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(UnreadBadge)
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
}

private data class ChatPeer(
    val name: String,
    val snippet: String,
    val time: String,
    val badge: String?,
    val online: Boolean,
)

@Composable
internal fun JetpackCommunityRoot(
    onOpen: (String) -> Unit = {},
    onPreviewImages: (List<String>, Int) -> Unit = { _, _ -> },
    onPlayVideo: (String) -> Unit = {},
) {
    var filter by remember { mutableStateOf("最新") }
    var like0 by remember { mutableStateOf(false to 0) }
    var like1 by remember { mutableStateOf(false to 0) }
    val published = remember { CommunityPublishBus.lastPublishedBody }

    fun postsForFilter(): List<CommunityFeedPost> {
        // Aligned to Flutter live SoT feed (HTTP posts on device: 测试甲 + 九宫格图).
        val base = listOf(
            CommunityFeedPost(
                id = "post_0",
                name = "测试甲",
                meta = "19小时前 · 来自 iPhone",
                body = "这么擦擦 8\n#Flutter开发",
                videoCoverUrl = null,
                imageUrls = List(9) { "https://picsum.photos/seed/sot_a_$it/400/400" },
                likes = like0.second,
                liked = like0.first,
                comments = "0",
                hotScore = 200,
                thread = emptyList(),
            ),
            CommunityFeedPost(
                id = "post_1",
                name = "测试甲",
                meta = "19小时前 · 来自 iPhone",
                body = "好喜欢的好喜欢的好\n#纳指大涨超2%再创新高",
                videoCoverUrl = null,
                imageUrls = List(9) { "https://picsum.photos/seed/sot_b_$it/400/400" },
                likes = like1.second,
                liked = like1.first,
                comments = "0",
                hotScore = 90,
                thread = emptyList(),
            ),
        )
        val withPublished = if (published != null) {
            listOf(
                CommunityFeedPost(
                    id = "post_new",
                    name = "我",
                    meta = "刚刚 · 来自 Android",
                    body = published,
                    videoCoverUrl = null,
                    imageUrls = emptyList(),
                    likes = 0,
                    liked = false,
                    comments = "0",
                    hotScore = 999,
                    thread = emptyList(),
                ),
            ) + base
        } else base
        return when (filter) {
            "热门" -> withPublished.sortedByDescending { it.hotScore }
            "关注" -> withPublished // Flutter live may be empty; keep feed visible for parity
            else -> withPublished
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(DemoColors.PageBg)
            .statusBarsPadding(),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 0.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "社区",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DemoColors.TextPrimary,
                    letterSpacing = (-1.6).sp,
                    lineHeight = 36.sp,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    Modifier.size(44.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(DemoColors.Accent)
                            .clickable { onOpen("发布动态") },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("+", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DemoColors.Background)
                    .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(8.dp))
                    .clickable { onOpen("社区搜索") }
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("⌕", color = DemoColors.TextSecondary, fontSize = 18.sp)
                Spacer(Modifier.width(8.dp))
                Text("搜索动态、话题、用户", color = Color(0xFF888888), fontSize = 14.sp)
            }
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(36.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                listOf("最新", "热门", "关注").forEach { item ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { filter = item },
                    ) {
                        Text(
                            item,
                            color = if (filter == item) DemoColors.TextPrimary else DemoColors.TextSecondary,
                            fontWeight = if (filter == item) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 15.sp,
                        )
                        Spacer(Modifier.height(6.dp))
                        Box(
                            Modifier
                                .width(if (filter == item) 20.dp else 0.dp)
                                .height(2.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(if (filter == item) DemoColors.Accent else Color.Transparent),
                        )
                    }
                }
            }
        }
        val feed = postsForFilter()
        if (feed.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无动态", color = DemoColors.TextSecondary)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(feed, key = { it.id }) { post ->
                    CommunityPostCard(
                        name = post.name,
                        meta = post.meta,
                        body = post.body,
                        videoCoverUrl = post.videoCoverUrl,
                        imageUrls = post.imageUrls,
                        postId = post.id,
                        likes = post.likes.toString(),
                        comments = post.comments,
                        liked = post.liked,
                        thread = post.thread,
                        onToggleLike = {
                            when (post.id) {
                                "post_0" -> like0 = if (like0.first) false to (like0.second - 1) else true to (like0.second + 1)
                                "post_1" -> like1 = if (like1.first) false to (like1.second - 1) else true to (like1.second + 1)
                            }
                        },
                        onPreviewImages = onPreviewImages,
                        onPlayVideo = onPlayVideo,
                        onOpenConvention = { onOpen("社区公约") },
                    )
                }
            }
        }
    }
}

private data class CommunityFeedPost(
    val id: String,
    val name: String,
    val meta: String,
    val body: String,
    val videoCoverUrl: String?,
    val imageUrls: List<String>,
    val likes: Int,
    val liked: Boolean,
    val comments: String,
    val hotScore: Int,
    val thread: List<CommunityThreadLine>,
)

private sealed class CommunityThreadLine {
    data class Comment(val name: String, val body: String) : CommunityThreadLine()
    data class Reply(val from: String, val to: String, val body: String) : CommunityThreadLine()
}

@Composable
private fun CommunityPostCard(
    name: String,
    meta: String,
    body: String,
    videoCoverUrl: String?,
    imageUrls: List<String>,
    postId: String,
    likes: String,
    comments: String,
    liked: Boolean,
    thread: List<CommunityThreadLine>,
    onToggleLike: () -> Unit = {},
    onPreviewImages: (List<String>, Int) -> Unit = { _, _ -> },
    onPlayVideo: (String) -> Unit = {},
    onOpenConvention: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val gridUrls = remember(postId, imageUrls) { nineGridUrls(postId, imageUrls) }
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DemoColors.Background)
            .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(8.dp))
            .padding(start = 16.dp, top = 14.dp, end = 12.dp, bottom = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Image(
                painter = painterResource(Res.drawable.community_avatar),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(44.dp).clip(CircleShape),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = DemoColors.TextPrimary)
                Spacer(Modifier.height(2.dp))
                Text(meta, fontSize = 14.sp, color = DemoColors.TextSecondary)
            }
            Box(
                Modifier
                    .size(44.dp)
                    .clickable(onClick = onOpenConvention),
                contentAlignment = Alignment.Center,
            ) {
                Text("⋯", color = DemoColors.TextSecondary, fontSize = 20.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = communityRichBody(body),
            fontSize = 16.sp,
            lineHeight = (16 * 1.45).sp,
        )
        Spacer(Modifier.height(12.dp))
        if (videoCoverUrl != null) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onPlayVideo(videoCoverUrl) },
                contentAlignment = Alignment.Center,
            ) {
                PlatformNetworkImage(
                    url = videoCoverUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    placeholder = Res.drawable.community_post_a,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.38f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("▶", color = Color.White, fontSize = 22.sp)
                }
            }
        } else {
            CommunityImageGrid(
                urls = gridUrls,
                onTap = { index -> onPreviewImages(gridUrls, index) },
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
            // Flutter LikeBarWidget: count > 0 → number, else 「赞」/「评论」
            val likeLabel = likes.toIntOrNull()?.takeIf { it > 0 }?.toString() ?: "赞"
            val commentLabel = comments.toIntOrNull()?.takeIf { it > 0 }?.toString() ?: "评论"
            CommunityAction(
                label = likeLabel,
                tint = if (liked) LikeRed else DemoColors.TextSecondary,
                icon = if (liked) "♥" else "♡",
                onClick = onToggleLike,
            )
            CommunityAction(label = commentLabel, tint = DemoColors.TextSecondary, icon = "💬")
            CommunityAction(label = "分享", tint = DemoColors.TextSecondary, icon = "↗")
        }
        if (thread.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(DemoColors.PageBg.copy(alpha = 0.5f))
                    .padding(10.dp),
            ) {
                thread.forEachIndexed { index, line ->
                    when (line) {
                        is CommunityThreadLine.Comment -> CommunityCommentLine(
                            prefix = "${line.name}：",
                            body = line.body,
                        )
                        is CommunityThreadLine.Reply -> CommunityReplyLine(
                            from = line.from,
                            to = line.to,
                            body = line.body,
                        )
                    }
                    if (index != thread.lastIndex) Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

/** Flutter [ImageGridWidget]: max 9, 3 columns, pad with picsum seeds. */
@Composable
private fun CommunityImageGrid(urls: List<String>, onTap: (Int) -> Unit) {
    if (urls.isEmpty()) return
    val gap = 4.dp
    var flatIndex = 0
    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
        urls.chunked(3).forEach { row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gap),
            ) {
                row.forEach { url ->
                    val i = flatIndex
                    flatIndex += 1
                    PlatformNetworkImage(
                        url = url,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        placeholder = Res.drawable.community_post_a,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { onTap(i) },
                    )
                }
                repeat(3 - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

private fun nineGridUrls(postId: String, images: List<String>): List<String> {
    val out = images.filter { it.isNotBlank() }.take(9).toMutableList()
    var i = out.size
    while (out.size < 9) {
        out.add("https://picsum.photos/seed/${postId}_$i/400/400")
        i++
    }
    return out
}

@Composable
private fun CommunityAction(
    label: String,
    tint: Color,
    icon: String,
    onClick: (() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 4.dp),
    ) {
        Text(icon, fontSize = 20.sp, color = tint)
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 13.sp, color = tint)
    }
}

@Composable
private fun CommunityCommentLine(prefix: String, body: String) {
    // Flutter CommentPreviewWidget: entire prefix (incl. name：) is #576B95
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = CommunityLinkBlue, fontWeight = FontWeight.SemiBold)) {
                append(prefix)
            }
            withStyle(SpanStyle(color = DemoColors.TextPrimary)) {
                append(body)
            }
        },
        fontSize = 14.sp,
        lineHeight = (14 * 1.35).sp,
    )
}

@Composable
private fun CommunityReplyLine(from: String, to: String, body: String) {
    // Flutter: '${nickname} 回复 ${replyTo}：' entirely #576B95
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = CommunityLinkBlue, fontWeight = FontWeight.SemiBold)) {
                append("$from 回复 $to：")
            }
            withStyle(SpanStyle(color = DemoColors.TextPrimary)) {
                append(body)
            }
        },
        fontSize = 14.sp,
        lineHeight = (14 * 1.35).sp,
    )
}

/** Mentions / hashtags / links — Flutter `RichTextContentWidget` uses `#576B95`. */
private fun communityRichBody(text: String) = buildAnnotatedString {
    val regex = Regex("""(@[^\s@]+)|(#[^\s#]+)|(https?://\S+)""")
    var last = 0
    for (match in regex.findAll(text)) {
        if (match.range.first > last) {
            withStyle(SpanStyle(color = DemoColors.TextPrimary)) {
                append(text.substring(last, match.range.first))
            }
        }
        val isLink = match.groups[3] != null
        withStyle(
            SpanStyle(
                color = CommunityLinkBlue,
                fontWeight = FontWeight.Medium,
                textDecoration = if (isLink) {
                    androidx.compose.ui.text.style.TextDecoration.Underline
                } else {
                    null
                },
            ),
        ) {
            append(match.value)
        }
        last = match.range.last + 1
    }
    if (last < text.length) {
        withStyle(SpanStyle(color = DemoColors.TextPrimary)) {
            append(text.substring(last))
        }
    }
}

@Composable
internal fun JetpackMineRoot(
    loggedIn: Boolean,
    displayName: String? = null,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPersonalized: () -> Unit,
    onDeferred: (String) -> Unit,
) {
    // CMP MineHomeContent already mirrors Flutter MinePage (header / quick / functions / menu).
    MineHomeContent(
        loggedIn = loggedIn,
        displayName = displayName,
        onLoginClick = onLogin,
        onLogoutClick = onLogout,
        onOpenSettings = onOpenSettings,
        onOpenPersonalized = onOpenPersonalized,
        snackbar = { label ->
            val routeable = MineRoutes.fromLabel(label) != null ||
                HomeRoutes.fromLabel(label) != null
            if (routeable) onDeferred(label)
            else showPlatformToast(label)
        },
    )
}

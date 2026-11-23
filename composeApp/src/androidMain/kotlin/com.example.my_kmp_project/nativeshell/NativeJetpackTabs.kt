package com.example.my_kmp_project.nativeshell

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
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
import com.example.my_kmp_project.core.design.DemoShapes
import com.example.my_kmp_project.core.design.MineTopBar
import com.example.my_kmp_project.core.platform.showPlatformToast
import com.example.my_kmp_project.core.ui.PlatformNetworkImage
import com.example.my_kmp_project.core.ui.StateView
import com.example.my_kmp_project.core.ui.StateViewType
import com.example.my_kmp_project.component.chat.ChatDetailDeepLinkArgs
import com.example.my_kmp_project.component.chat.ImConversation
import com.example.my_kmp_project.component.chat.ImEngine
import com.example.my_kmp_project.component.chat.ImEngineStore
import com.example.my_kmp_project.feature.community.CommunityPublishBus
import com.example.my_kmp_project.feature.community.MockCommunityEngine
import com.example.my_kmp_project.feature.community.http.HttpCommunityPostRepository
import com.example.my_kmp_project.feature.community.http.PostModel
import com.example.my_kmp_project.core.router.CommunityRoutes
import com.example.my_kmp_project.core.router.ContentRoutes
import com.example.my_kmp_project.core.router.HomeRoutes
import com.example.my_kmp_project.feature.mine.MineHomeContent
import com.example.my_kmp_project.core.router.MineRoutes
import my_kmp_project.core.design.generated.resources.Res
import my_kmp_project.core.design.generated.resources.community_avatar
import my_kmp_project.core.design.generated.resources.community_post_a
import my_kmp_project.core.design.generated.resources.home_transliter_home_icon_search
import my_kmp_project.core.design.generated.resources.ic_nav_back
import org.jetbrains.compose.resources.painterResource
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.IconButton
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import com.example.my_kmp_project.core.router.AiRoutes
import com.example.my_kmp_project.core.router.ClassroomRoutes
import com.example.my_kmp_project.core.router.VideoRoutes
import kotlinx.coroutines.launch

/** Flutter ChatTheme / ConversationListItem — WeChat-blue for @/#/reply prefixes. */
private const val CommunityFeedPageSize = 10

private val CommunityLinkBlue = Color(0xFF576B95)
private val ChatOnline = DemoColors.Accent
/** Flutter ChatTheme 在线绿；DesignTokens 无对应语义色，保留 SoT 值。 */
private val ChatOnlineGreen = Color(0xFF07C160)
/** 聊天列表卡 / 社区帖子卡共用圆角（DesignTokens.Radius.Md），消除各写一份的 8dp 魔数。 */
private val ContentCardShape = DemoShapes.medium

@Composable
internal fun JetpackChatRoot(
    onOpenContacts: () -> Unit = {},
    pendingDetail: ChatDetailDeepLinkArgs? = null,
    showMissingDetailParams: Boolean = false,
    onPendingDetailConsumed: () -> Unit = {},
    onMissingDetailConsumed: () -> Unit = {},
    onDetailVisibilityChanged: (Boolean) -> Unit = {},
) {
    val engine = remember { ImEngineStore.engine }
    var searchOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var openId by remember { mutableStateOf<String?>(null) }
    var revision by remember { mutableStateOf(0) }
    var missingDetail by remember { mutableStateOf(false) }

    DisposableEffect(engine) {
        val unsub = engine.observe { revision += 1 }
        onDispose { unsub() }
    }

    LaunchedEffect(pendingDetail, showMissingDetailParams) {
        if (showMissingDetailParams) {
            missingDetail = true
            openId = null
            onMissingDetailConsumed()
            return@LaunchedEffect
        }
        val args = pendingDetail ?: return@LaunchedEffect
        val id = engine.ensureConversation(
            id = args.id,
            title = args.peerName,
            lastMessage = args.lastMessage,
            unreadCount = args.unreadCount,
        )
        missingDetail = false
        openId = id
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

    val conversations = remember(revision) { engine.conversations() }
    val filtered = remember(conversations, query, revision) {
        engine.filterConversations(query)
    }
    val open = openId?.let { id -> conversations.firstOrNull { it.id == id } }
    if (open != null) {
        LaunchedEffect(open.id) {
            engine.markConversationRead(open.id)
        }
        JetpackChatDetail(
            conversation = open,
            engine = engine,
            revision = revision,
            onBack = {
                openId = null
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
                if (searchOpen) {
                    Text("✕", color = DemoColors.Accent, fontSize = 20.sp)
                } else {
                    Image(
                        painter = painterResource(Res.drawable.home_transliter_home_icon_search),
                        contentDescription = "搜索会话",
                        modifier = Modifier.size(22.dp),
                        contentScale = ContentScale.Fit,
                        colorFilter = ColorFilter.tint(DemoColors.Accent),
                    )
                }
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
                Image(
                    painter = painterResource(Res.drawable.home_transliter_home_icon_search),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(DemoColors.Muted),
                )
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
                    onRefreshHint = { revision += 1 },
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
                            .clip(ContentCardShape)
                            .background(DemoColors.Background)
                            .border(0.5.dp, DemoColors.Divider, ContentCardShape),
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
                                        badge = when {
                                            conv.unreadCount <= 0 -> null
                                            conv.unreadCount > 99 -> "99+"
                                            else -> conv.unreadCount.toString()
                                        },
                                        online = conv.isOnline,
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
            modifier = Modifier
                .clickable(onClick = onRefreshHint)
                .padding(vertical = 12.dp),
        )
    }
}

@Composable
private fun JetpackChatDetail(
    conversation: ImConversation,
    engine: ImEngine,
    revision: Int,
    onBack: () -> Unit,
) {
    val messages = remember(conversation.id, revision) { engine.messages(conversation.id) }
    var draft by remember { mutableStateOf("") }
    var previewUrl by remember { mutableStateOf<String?>(null) }
    var sending by remember { mutableStateOf(false) }
    var voiceMode by remember { mutableStateOf(false) }
    var showEmoji by remember { mutableStateOf(false) }
    var showMore by remember { mutableStateOf(false) }

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
        // Flutter _ChatDetailHeader: blue back + avatar + name/online
        Column(Modifier.fillMaxWidth().background(DemoColors.Background)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 8.dp, bottom = 10.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(Res.drawable.ic_nav_back),
                        contentDescription = "返回",
                        modifier = Modifier
                            .size(22.dp)
                            .rotate(180f),
                        contentScale = ContentScale.Fit,
                        colorFilter = ColorFilter.tint(DemoColors.Accent),
                    )
                }
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(DemoColors.PageBg),
                ) {
                    PlatformNetworkImage(
                        url = conversation.portraitUrl.ifBlank {
                            "https://picsum.photos/seed/chat_${conversation.peerId}/150/150"
                        },
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        placeholder = Res.drawable.community_avatar,
                        contentDescription = conversation.title,
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        conversation.title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DemoColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        if (conversation.isOnline) "在线" else "离线",
                        fontSize = 12.sp,
                        color = if (conversation.isOnline) ChatOnlineGreen else DemoColors.Muted,
                    )
                }
            }
            HorizontalDivider(thickness = 0.5.dp, color = DemoColors.Divider)
        }
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            reverseLayout = true,
        ) {
            // reverseLayout: index 0 is bottom — Flutter MessageListView reverse + newest-first store
            items(messages, key = { it.id }) { bubble ->
                if (bubble.type == com.example.my_kmp_project.component.chat.ImMessageType.Time) {
                    Text(
                        bubble.body,
                        fontSize = 12.sp,
                        color = DemoColors.Muted,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                    return@items
                }
                val isImage = bubble.type == com.example.my_kmp_project.component.chat.ImMessageType.Image
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = if (bubble.isSelf) Arrangement.End else Arrangement.Start,
                ) {
                    Column(horizontalAlignment = if (bubble.isSelf) Alignment.End else Alignment.Start) {
                        if (isImage) {
                            val url = bubble.body.trim().ifBlank { "https://picsum.photos/seed/chat/600" }
                            Box(
                                Modifier
                                    .size(140.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DemoColors.Background)
                                    .clickable { previewUrl = url },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("图片", color = DemoColors.Accent)
                            }
                        } else {
                            Text(
                                when (bubble.type) {
                                    com.example.my_kmp_project.component.chat.ImMessageType.Voice -> "[语音]"
                                    com.example.my_kmp_project.component.chat.ImMessageType.Custom ->
                                        if (bubble.body == "Demo 名片" || bubble.body.contains("名片")) "Demo 名片"
                                        else bubble.body
                                    else -> bubble.body
                                },
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
                        val status = bubble.statusLabel()
                        Text(
                            if (status.isNotEmpty()) status else bubble.timeLabel,
                            fontSize = 11.sp,
                            color = DemoColors.Muted,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
            }
        }
        // Flutter InputPanel: voice ↔ keyboard, emoji, more
        Column(Modifier.fillMaxWidth().background(DemoColors.PageBg)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(DemoColors.Background)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    if (voiceMode) "⌨" else "🎤",
                    fontSize = 20.sp,
                    modifier = Modifier
                        .clickable {
                            voiceMode = !voiceMode
                            showEmoji = false
                            showMore = false
                        }
                        .padding(4.dp),
                )
                if (voiceMode) {
                    Box(
                        Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DemoColors.Background)
                            .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(8.dp))
                            .clickable {
                                engine.sendVoice(conversation.id)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("按住 说话", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = DemoColors.TextPrimary)
                    }
                } else {
                    BasicTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        textStyle = TextStyle(fontSize = 16.sp, color = DemoColors.TextPrimary),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(DemoColors.PageBg)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        decorationBox = { inner ->
                            if (draft.isEmpty()) {
                                Text("发消息…", color = DemoColors.Muted, fontSize = 16.sp)
                            }
                            inner()
                        },
                    )
                }
                Text(
                    "☺",
                    fontSize = 22.sp,
                    color = if (showEmoji) DemoColors.Accent else DemoColors.TextSecondary,
                    modifier = Modifier
                        .clickable {
                            showEmoji = !showEmoji
                            showMore = false
                            voiceMode = false
                        }
                        .padding(4.dp),
                )
                if (draft.trim().isEmpty()) {
                    Text(
                        "＋",
                        fontSize = 28.sp,
                        color = if (showMore) DemoColors.Accent else DemoColors.TextSecondary,
                        modifier = Modifier
                            .clickable {
                                showMore = !showMore
                                showEmoji = false
                            }
                            .padding(4.dp),
                    )
                } else {
                    Text(
                        if (sending) "…" else "发送",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(DemoColors.Accent)
                            .clickable {
                                val text = draft.trim()
                                if (text.isEmpty() || sending) return@clickable
                                sending = true
                                val sent = engine.sendText(conversation.id, text)
                                if (sent != null) {
                                    draft = ""
                                    voiceMode = false
                                    showEmoji = false
                                    showMore = false
                                } else {
                                    showPlatformToast("发送失败")
                                }
                                sending = false
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
            if (showEmoji) {
                // Flutter ChatDetailViewModel.emojiList
                val emojis = listOf(
                    "😀", "😂", "🥰", "😎", "🤔", "👍", "🙏", "🎉",
                    "❤️", "🔥", "👋", "😭", "🤣", "😊", "🥳", "💪",
                )
                Column(
                    Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(DemoColors.Background)
                        .padding(12.dp),
                ) {
                    emojis.chunked(8).forEach { row ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            row.forEach { e ->
                                Text(
                                    e,
                                    fontSize = 28.sp,
                                    modifier = Modifier.clickable { draft += e },
                                )
                            }
                        }
                    }
                }
            }
            if (showMore) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(DemoColors.Background)
                        .padding(vertical = 20.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    ChatMoreAction("照片") {
                        engine.sendImage(
                            conversation.id,
                            "https://picsum.photos/seed/${conversation.peerId}/600",
                        )
                        showMore = false
                    }
                    ChatMoreAction("拍摄") {
                        engine.sendImage(
                            conversation.id,
                            "https://picsum.photos/seed/${conversation.peerId}_cam/600",
                        )
                        showMore = false
                    }
                    ChatMoreAction("文件") {
                        engine.sendCustom(conversation.id, "文件")
                        showMore = false
                    }
                    ChatMoreAction("位置") {
                        engine.sendCustom(conversation.id, "位置")
                        showMore = false
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatMoreAction(title: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Box(
            Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DemoColors.PageBg),
            contentAlignment = Alignment.Center,
        ) {
            Text(title.take(1), color = DemoColors.Accent, fontSize = 18.sp)
        }
        Spacer(Modifier.height(8.dp))
        Text(title, fontSize = 12.sp, color = DemoColors.TextSecondary)
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
                            .background(DemoColors.Danger)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun JetpackCommunityRoot(
    onOpen: (String) -> Unit = {},
    onOpenComments: (postId: String) -> Unit = {},
    onPreviewImages: (List<String>, Int) -> Unit = { _, _ -> },
    onPlayVideo: (String) -> Unit = {},
) {
    val repository = remember { HttpCommunityPostRepository() }
    val scope = rememberCoroutineScope()
    var filter by remember { mutableStateOf("最新") }
    var posts by remember { mutableStateOf<List<PostModel>>(emptyList()) }
    var feedPage by remember { mutableIntStateOf(0) }
    var feedHasMore by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var feedError by remember { mutableStateOf<String?>(null) }
    // 代次隔离：切 tab / 刷新后旧请求回包不写回当前 tab。
    var feedGeneration by remember { mutableIntStateOf(0) }
    // 已组合时以发布版本号触发刷新；首屏由 LaunchedEffect(filter) 负责，跳过进入时的初值。
    var lastSeenPublishRevision by remember { mutableIntStateOf(CommunityPublishBus.publishRevision) }
    val pullRefreshState = rememberPullToRefreshState()
    val listState = rememberLazyListState()

    fun tabKeyOf(item: String): String = when (item) {
        "热门" -> "hot"
        "关注" -> "following"
        else -> "latest"
    }

    suspend fun loadFirstPage() {
        val generation = ++feedGeneration
        val key = tabKeyOf(filter)
        isRefreshing = true
        isLoadingMore = false
        feedError = null
        try {
            val response = repository.fetchPostsPage(
                page = 0,
                pageSize = CommunityFeedPageSize,
                tab = key,
            )
            if (generation != feedGeneration || key != tabKeyOf(filter)) return
            posts = response.items
            feedPage = 0
            feedHasMore = response.hasMore(CommunityFeedPageSize)
        } catch (e: Exception) {
            if (generation != feedGeneration || key != tabKeyOf(filter)) return
            posts = emptyList()
            feedPage = 0
            feedHasMore = false
            feedError = e.message?.takeIf { it.isNotBlank() } ?: "加载动态失败"
        } finally {
            if (generation == feedGeneration && key == tabKeyOf(filter)) isRefreshing = false
        }
    }

    fun loadMore() {
        if (isLoadingMore || !feedHasMore || isRefreshing) return
        val generation = feedGeneration
        val key = tabKeyOf(filter)
        val targetPage = feedPage + 1
        isLoadingMore = true
        feedError = null
        scope.launch {
            try {
                val response = repository.fetchPostsPage(
                    page = targetPage,
                    pageSize = CommunityFeedPageSize,
                    tab = key,
                )
                if (generation != feedGeneration || key != tabKeyOf(filter)) return@launch
                if (response.items.isNotEmpty()) posts = posts + response.items
                feedPage = targetPage
                feedHasMore = response.hasMore(CommunityFeedPageSize)
            } catch (e: Exception) {
                if (generation != feedGeneration || key != tabKeyOf(filter)) return@launch
                feedError = e.message?.takeIf { it.isNotBlank() } ?: "加载更多失败"
            } finally {
                if (generation == feedGeneration && key == tabKeyOf(filter)) isLoadingMore = false
            }
        }
    }

    LaunchedEffect(filter) {
        loadFirstPage()
    }

    // 发布成功由 CommunityPublishBus.publishRevision 通知，无需轮询；失败发布不递增、不触发刷新。
    LaunchedEffect(CommunityPublishBus.publishRevision) {
        val revision = CommunityPublishBus.publishRevision
        if (revision == lastSeenPublishRevision) return@LaunchedEffect
        lastSeenPublishRevision = revision
        loadFirstPage()
    }

    val nearEnd by remember {
        derivedStateOf {
            val lastIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastIndex >= posts.size - 2
        }
    }
    LaunchedEffect(nearEnd, posts.size, feedHasMore, isLoadingMore) {
        if (nearEnd && feedHasMore && !isLoadingMore && feedError == null && posts.isNotEmpty()) {
            loadMore()
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { scope.launch { loadFirstPage() } },
        state = pullRefreshState,
        modifier = Modifier.fillMaxSize().background(DemoColors.PageBg),
    ) {
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
                    Modifier
                        .size(44.dp)
                        .clickable { onOpen("发布动态") },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(DemoColors.Accent),
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
                Image(
                    painter = painterResource(Res.drawable.home_transliter_home_icon_search),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(DemoColors.TextSecondary),
                )
                Spacer(Modifier.width(8.dp))
                Text("搜索动态、话题、用户", color = DemoColors.Muted, fontSize = 14.sp)
            }
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                listOf("最新", "热门", "关注").forEach { item ->
                    val selected = filter == item
                    // 短促、可中断：只跟随 filter 状态，不触碰滚动位置与 IM/社区引擎。
                    val indicatorWidth by animateDpAsState(
                        targetValue = if (selected) 20.dp else 0.dp,
                        animationSpec = tween(durationMillis = 120),
                        label = "communityFilterIndicator",
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .height(44.dp)
                            .clickable { filter = item }
                            .padding(horizontal = 4.dp),
                    ) {
                        Text(
                            item,
                            color = if (selected) DemoColors.TextPrimary else DemoColors.TextSecondary,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 15.sp,
                        )
                        Spacer(Modifier.height(6.dp))
                        Box(
                            Modifier
                                .width(indicatorWidth)
                                .height(2.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(if (selected) DemoColors.Accent else Color.Transparent),
                        )
                    }
                }
            }
        }
        when {
            posts.isEmpty() && isRefreshing -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("加载中…", color = DemoColors.TextSecondary)
                }
            }
            posts.isEmpty() && feedError != null -> {
                StateView(
                    type = StateViewType.Error,
                    title = "加载失败",
                    description = "网络似乎出了点问题，请稍后重试",
                    actionLabel = "重试",
                    onAction = { scope.launch { loadFirstPage() } },
                )
            }
            posts.isEmpty() -> {
                StateView(
                    type = StateViewType.Empty,
                    title = "暂无数据",
                    description = if (filter == "关注") "还没有关注的人，去最新里看看吧" else null,
                )
            }
            else -> {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(posts, key = { it.id }) { post ->
                        val thread = post.previewComments.map { c ->
                            val replyTo = c.replyToNickname
                            if (replyTo != null) {
                                CommunityThreadLine.Reply(c.nickname, replyTo, c.content)
                            } else {
                                CommunityThreadLine.Comment(c.nickname, c.content)
                            }
                        }
                        CommunityPostCard(
                            name = post.nickname,
                            meta = "${MockCommunityEngine.formatPublishTime(post.publishTimeMs())} · ${post.source}",
                            body = post.content,
                            videoCoverUrl = post.videoCoverUrl,
                            imageUrls = post.images,
                            postId = post.id,
                            likes = post.likeCount.toString(),
                            comments = post.commentCount.toString(),
                            liked = post.isLiked,
                            thread = thread,
                            onToggleLike = {
                                scope.launch {
                                    try {
                                        val fragment = repository.toggleLike(post.id, liked = !post.isLiked)
                                        posts = posts.map { if (it.id == post.id) fragment else it }
                                    } catch (_: Exception) {
                                        showPlatformToast("点赞失败，请稍后重试")
                                    }
                                }
                            },
                            onPreviewImages = onPreviewImages,
                            // 封面只用于展示；播放用真实 video_url，不把 cover 当视频源。
                            onPlayVideo = { onPlayVideo(post.videoUrl.orEmpty()) },
                            onOpenConvention = { onOpen("社区公约") },
                            onOpenComment = { onOpenComments(post.id) },
                        )
                    }
                    item {
                        Box(
                            Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            when {
                                isLoadingMore -> Text("加载中…", color = DemoColors.Muted, fontSize = 13.sp)
                                feedError != null -> TextButton(onClick = ::loadMore) {
                                    Text("加载失败，点击重试", color = DemoColors.Accent, fontSize = 13.sp)
                                }
                                !feedHasMore -> Text("没有更多了", color = DemoColors.Muted, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
    }
}

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
    onOpenComment: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val gridUrls = remember(postId, imageUrls) { nineGridUrls(postId, imageUrls) }
    Column(
        modifier
            .fillMaxWidth()
            .clip(ContentCardShape)
            .background(DemoColors.Background)
            .border(0.5.dp, DemoColors.Divider, ContentCardShape)
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
                tint = if (liked) DemoColors.Danger else DemoColors.TextSecondary,
                icon = if (liked) "♥" else "♡",
                onClick = onToggleLike,
            )
            CommunityAction(label = commentLabel, tint = DemoColors.TextSecondary, icon = "💬", onClick = onOpenComment)
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
            .padding(horizontal = 4.dp, vertical = 12.dp),
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
        onNavigate = { raw ->
            // Paths + Chinese labels → NativeAndroidMain.openDeferred / ProductRouteDispatch.
            onDeferred(raw)
        },
        // Soft-auth: openDeferred already gates mall/wallet/home routes via openWithSoftAuth.
        onRequireLogin = { redirect -> onDeferred(redirect) },
        snackbar = { label -> showPlatformToast(label) },
    )
}

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.feature.mine.MineHomeContent
import my_kmp_project.composeapp.generated.resources.Res
import my_kmp_project.composeapp.generated.resources.community_avatar
import my_kmp_project.composeapp.generated.resources.community_post_a
import org.jetbrains.compose.resources.painterResource
import com.example.my_kmp_project.core.ui.PlatformNetworkImage
import com.example.my_kmp_project.core.design.MineTopBar
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
internal fun JetpackChatRoot() {
    // Flutter MockImChatStore peers — titles/snippets/unread/online parity.
    val peers = listOf(
        ChatPeer("Mock好友1", "晚上一起吃饭吗？", "22:50", "2", true),
        ChatPeer("Mock好友2", "你好", "22:45", null, false),
        ChatPeer("Mock好友3", "你好", "22:40", null, true),
    )
    var searchOpen by remember { mutableStateOf(false) }
    var openPeer by remember { mutableStateOf<ChatPeer?>(null) }
    val open = openPeer
    if (open != null) {
        JetpackChatDetail(
            peer = open,
            onBack = { openPeer = null },
        )
        return
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(DemoColors.PageBg)
            .statusBarsPadding(),
    ) {
        // Flutter ChatPage: EdgeInsets.fromLTRB(16, top+8, 8, 8)
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
                    .clickable { searchOpen = !searchOpen },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (searchOpen) "✕" else "⌕",
                    color = DemoColors.Accent,
                    fontSize = 20.sp,
                )
            }
            Box(
                Modifier.size(44.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("✎", color = DemoColors.Accent, fontSize = 20.sp)
            }
        }
        if (searchOpen) {
            // Flutter: hint「搜索会话名称或消息」, radiusLg 12
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
                Text("搜索会话名称或消息", color = DemoColors.Muted, fontSize = 14.sp)
            }
        }
        // Flutter ListView padding: fromLTRB(16, 8, 16, 24); card radiusMd 8
        LazyColumn(
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
                    peers.forEachIndexed { index, peer ->
                        ChatConversationRow(peer, onClick = { openPeer = peer })
                        if (index != peers.lastIndex) {
                            // Flutter ChatTheme.groupedDivider indent 72 (avatar 52 + gap 12 + pad 8)
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

@Composable
private fun JetpackChatDetail(peer: ChatPeer, onBack: () -> Unit) {
    val messages = remember(peer.name) {
        mutableStateListOf(
            ChatBubble(peer.snippet, fromMe = false),
            ChatBubble("收到，稍后再回你", fromMe = true),
        )
    }
    var draft by remember { mutableStateOf("") }
    Column(
        Modifier
            .fillMaxSize()
            .background(DemoColors.PageBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        MineTopBar(title = peer.name, onBack = onBack, containerColor = DemoColors.PageBg)
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            reverseLayout = false,
        ) {
            items(messages.size) { i ->
                val bubble = messages[i]
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = if (bubble.fromMe) Arrangement.End else Arrangement.Start,
                ) {
                    Text(
                        bubble.text,
                        color = if (bubble.fromMe) Color.White else DemoColors.TextPrimary,
                        fontSize = 15.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (bubble.fromMe) DemoColors.Accent else DemoColors.Background)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
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
                "发送",
                color = DemoColors.Accent,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clickable {
                        val text = draft.trim()
                        if (text.isNotEmpty()) {
                            messages.add(ChatBubble(text, fromMe = true))
                            draft = ""
                        }
                    }
                    .padding(8.dp),
            )
        }
    }
}

private data class ChatBubble(val text: String, val fromMe: Boolean)

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
internal fun JetpackCommunityRoot() {
    var filter by remember { mutableStateOf("最新") }
    Column(
        Modifier
            .fillMaxSize()
            .background(DemoColors.PageBg)
            .statusBarsPadding(),
    ) {
        // Flutter _CommunityHeader: pad LTRB(16, top+8, 16, 0) then title / 16 / search44 / 12 / tabs
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
                            .clickable { },
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
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            // Flutter ListView.separated item spacing 12
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Flutter MockPostRepository seed Random(42) — post_0 / post_1
            item {
                CommunityPostCard(
                    name = "张三",
                    meta = "7分钟前 · 来自 iPhone",
                    body = "今天去了 @张三 推荐的咖啡店，环境不错。\n#Flutter开发\n欢迎访问：https://flutter.dev",
                    videoCoverUrl = "https://picsum.photos/seed/video_0/640/360",
                    imageUrls = emptyList(),
                    postId = "post_0",
                    likes = "158",
                    comments = "6",
                    liked = true,
                    thread = listOf(
                        CommunityThreadLine.Comment("李四", "说得对！"),
                        CommunityThreadLine.Reply("赵六", "张三", "同感 +1"),
                    ),
                )
            }
            item {
                CommunityPostCard(
                    name = "李四",
                    meta = "42分钟前 · 来自 Android",
                    body = "周末 hiking，天气太好了！#户外",
                    videoCoverUrl = null,
                    // Flutter post_1: imgCount=2 then ImageGridWidget pads to 9
                    imageUrls = listOf(
                        "https://picsum.photos/seed/post_1_0/400/400",
                        "https://picsum.photos/seed/post_1_1/400/400",
                    ),
                    postId = "post_1",
                    likes = "77",
                    comments = "2",
                    liked = false,
                    thread = listOf(
                        CommunityThreadLine.Comment("王五", "说得对！"),
                        CommunityThreadLine.Reply("小明", "李四", "同感 +1"),
                    ),
                )
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
    modifier: Modifier = Modifier,
) {
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
            Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                Text("⋯", color = DemoColors.TextSecondary, fontSize = 20.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = communityRichBody(body),
            fontSize = 16.sp,
            lineHeight = (16 * 1.45).sp,
        )
        // Flutter PostCard: body ↔ media ↔ actions = 12
        Spacer(Modifier.height(12.dp))
        if (videoCoverUrl != null) {
            // Flutter VideoCardWidget: AspectRatio(16/9) + BoxFit.cover + play overlay
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(6.dp)),
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
            CommunityImageGrid(postId = postId, images = imageUrls)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
            CommunityAction(
                label = likes,
                tint = if (liked) LikeRed else DemoColors.TextSecondary,
                icon = if (liked) "♥" else "♡",
            )
            CommunityAction(label = comments, tint = DemoColors.TextSecondary, icon = "💬")
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
private fun CommunityImageGrid(postId: String, images: List<String>) {
    val urls = remember(postId, images) { nineGridUrls(postId, images) }
    if (urls.isEmpty()) return
    val gap = 4.dp
    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
        urls.chunked(3).forEach { row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gap),
            ) {
                row.forEach { url ->
                    PlatformNetworkImage(
                        url = url,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        placeholder = Res.drawable.community_post_a,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(4.dp)),
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
private fun CommunityAction(label: String, tint: Color, icon: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 4.dp),
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
    onLogin: () -> Unit,
    onLogout: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPersonalized: () -> Unit,
    onDeferred: (String) -> Unit,
) {
    // CMP MineHomeContent already mirrors Flutter MinePage (header / quick / functions / menu).
    MineHomeContent(
        loggedIn = loggedIn,
        displayName = if (loggedIn) "qa_user" else null,
        onLoginClick = onLogin,
        onLogoutClick = onLogout,
        onOpenSettings = onOpenSettings,
        onOpenPersonalized = onOpenPersonalized,
        snackbar = { onDeferred(it) },
    )
}

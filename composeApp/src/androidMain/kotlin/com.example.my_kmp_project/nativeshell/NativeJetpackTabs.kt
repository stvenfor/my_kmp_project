package com.example.my_kmp_project.nativeshell

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import my_kmp_project.composeapp.generated.resources.community_post_a
import my_kmp_project.composeapp.generated.resources.community_post_b
import my_kmp_project.composeapp.generated.resources.community_post_video
import my_kmp_project.composeapp.generated.resources.community_avatar
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun JetpackChatRoot() {
    val peers = listOf(
        ChatPeer("Mock好友1", "晚上一起吃饭吗？", "22:50", "2", true),
        ChatPeer("Mock好友2", "你好", "22:45", null, false),
        ChatPeer("Mock好友3", "你好", "22:40", null, true),
    )
    Column(
        Modifier
            .fillMaxSize()
            .background(DemoColors.PageBg)
            .statusBarsPadding(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("消息", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = DemoColors.TextPrimary, modifier = Modifier.weight(1f))
            Text("⌕", color = DemoColors.Accent, fontSize = 22.sp)
            Spacer(Modifier.width(16.dp))
            Text("✎", color = DemoColors.Accent, fontSize = 20.sp)
        }
        Column(
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DemoColors.Background),
        ) {
            peers.forEachIndexed { index, peer ->
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box {
                        Box(
                            Modifier.size(48.dp).clip(CircleShape).background(DemoColors.Accent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center,
                        ) { Text(peer.name.takeLast(1), color = DemoColors.Accent) }
                        if (peer.online) {
                            Box(
                                Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(DemoColors.Accent)
                                    .border(1.5.dp, Color.White, CircleShape),
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(peer.name, fontWeight = FontWeight.SemiBold, color = DemoColors.TextPrimary)
                        Text(peer.snippet, fontSize = 13.sp, color = DemoColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(peer.time, fontSize = 11.sp, color = DemoColors.TextSecondary)
                        if (peer.badge != null) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                peer.badge,
                                color = Color.White,
                                fontSize = 11.sp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFEE0000))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
                if (index != peers.lastIndex) HorizontalDivider(color = DemoColors.Divider)
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
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "社区",
                fontSize = 32.sp,
                fontWeight = FontWeight.SemiBold,
                color = DemoColors.TextPrimary,
                letterSpacing = (-1.6).sp,
                modifier = Modifier.weight(1f),
            )
            Box(
                Modifier
                    .size(44.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(DemoColors.Accent)
                        .clickable { },
                    contentAlignment = Alignment.Center,
                ) { Text("+", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
            }
        }
        Row(
            Modifier
                .padding(horizontal = 16.dp)
                .padding(top = 14.dp)
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
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp, bottom = 0.dp)
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
        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 8.dp, bottom = 16.dp),
        ) {
            item {
                CommunityPostCard(
                    name = "张三",
                    meta = "7分钟前 · 来自 iPhone",
                    body = "今天去了 @张三 推荐的咖啡店，环境不错。\n#Flutter开发\n欢迎访问：https://flutter.dev",
                    video = true,
                    likes = "158",
                    comments = "6",
                    liked = true,
                    showThread = true,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                )
            }
            item {
                CommunityPostCard(
                    name = "李四",
                    meta = "42分钟前 · 来自 Android",
                    body = "周末 hiking，天气太好了！#户外",
                    video = false,
                    likes = "赞",
                    comments = "评论",
                    liked = false,
                    showThread = false,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun CommunityPostCard(
    name: String,
    meta: String,
    body: String,
    video: Boolean,
    likes: String,
    comments: String,
    liked: Boolean,
    showThread: Boolean,
    modifier: Modifier = Modifier,
) {
    val likeRed = Color(0xFFEE0000)
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
        Spacer(Modifier.height(16.dp))
        if (video) {
            // Flutter VideoCardWidget: AspectRatio(16/9) + BoxFit.cover; SoT crop is full box incl. pale sky.
            Image(
                painter = painterResource(Res.drawable.community_post_video),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(6.dp)),
            )
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(Res.drawable.community_post_a, Res.drawable.community_post_b).forEach { res ->
                    Image(
                        painter = painterResource(res),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(4.dp)),
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
            CommunityAction(
                label = likes,
                tint = if (liked) likeRed else DemoColors.TextSecondary,
                icon = if (liked) "♥" else "♡",
            )
            CommunityAction(label = comments, tint = DemoColors.TextSecondary, icon = "💬")
            CommunityAction(label = "分享", tint = DemoColors.TextSecondary, icon = "↗")
        }
        if (showThread) {
            Spacer(Modifier.height(10.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(DemoColors.PageBg.copy(alpha = 0.5f))
                    .padding(10.dp),
            ) {
                CommunityCommentLine(prefix = "李四：", body = "说得对！")
                Spacer(Modifier.height(4.dp))
                CommunityReplyLine(
                    from = "赵六",
                    to = "张三",
                    body = "同感 +1",
                )
            }
        }
    }
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
    val nameBlue = Color(0xFF576B95)
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = nameBlue, fontWeight = FontWeight.SemiBold)) {
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
    val nameBlue = Color(0xFF576B95)
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = nameBlue, fontWeight = FontWeight.SemiBold)) {
                append(from)
            }
            withStyle(SpanStyle(color = DemoColors.TextPrimary)) {
                append(" 回复 ")
            }
            withStyle(SpanStyle(color = nameBlue, fontWeight = FontWeight.SemiBold)) {
                append("${to}：")
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
    val link = Color(0xFF576B95)
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
                color = link,
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

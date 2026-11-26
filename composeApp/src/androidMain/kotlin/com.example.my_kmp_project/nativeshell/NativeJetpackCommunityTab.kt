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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.platform.showPlatformToast
import com.example.my_kmp_project.core.ui.PlatformNetworkImage
import com.example.my_kmp_project.core.ui.StateView
import com.example.my_kmp_project.core.ui.StateViewType
import com.example.my_kmp_project.feature.community.CommunityPublishBus
import com.example.my_kmp_project.feature.community.formatPublishTime
import com.example.my_kmp_project.feature.community.http.HttpCommunityPostRepository
import com.example.my_kmp_project.feature.community.http.PostModel
import my_kmp_project.core.design.generated.resources.Res
import my_kmp_project.core.design.generated.resources.community_avatar
import my_kmp_project.core.design.generated.resources.community_post_a
import my_kmp_project.core.design.generated.resources.home_transliter_home_icon_search
import org.jetbrains.compose.resources.painterResource
import kotlinx.coroutines.launch

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
                            meta = "${formatPublishTime(post.publishTimeMs())} · ${post.source}",
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
    // 只展示服务端返回的图片，不再用占位图补满九宫格。
    val gridUrls = remember(postId, imageUrls) { imageUrls.filter { it.isNotBlank() }.take(9) }
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

/** Flutter [ImageGridWidget]: max 9, 3 columns; 只排服务端返回的图片。 */
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

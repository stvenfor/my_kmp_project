package com.example.my_kmp_project.feature.community

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.DemoElevation
import com.example.my_kmp_project.core.design.DemoGradients
import com.example.my_kmp_project.core.design.DemoRadius
import com.example.my_kmp_project.core.design.demoCard
import com.example.my_kmp_project.core.ui.ReportMainTabRoot
import com.example.my_kmp_project.core.ui.PlatformNetworkImage
import com.example.my_kmp_project.core.ui.StateView
import com.example.my_kmp_project.core.ui.StateViewType
import com.example.my_kmp_project.core.router.AiRoutes
import com.example.my_kmp_project.core.router.ClassroomRoutes
import com.example.my_kmp_project.core.router.CommunityRoutes
import com.example.my_kmp_project.core.router.ContentRoutes
import com.example.my_kmp_project.core.router.HomeRoutes
import com.example.my_kmp_project.core.router.MineRoutes
import com.example.my_kmp_project.core.router.VideoRoutes
import com.example.my_kmp_project.feature.community.http.CommentModel
import com.example.my_kmp_project.feature.community.http.CommunityUserHit as HttpUserHit
import com.example.my_kmp_project.feature.community.http.HttpCommunityPostRepository
import com.example.my_kmp_project.feature.community.http.PostModel
import com.example.my_kmp_project.feature.community.http.TopicModel
import my_kmp_project.core.design.generated.resources.Res
import my_kmp_project.core.design.generated.resources.community_post_a
import kotlinx.coroutines.launch

private const val FEED_PAGE_SIZE = 10

private fun feedTabKey(index: Int): String = when (index) {
    1 -> "hot"
    2 -> "following"
    else -> "latest"
}

/**
 * HTTP 契约模型 → UI 行类型适配。社区屏幕仍以 [CommunityFeedItem] 等既有行类型渲染
 * （保持 UI 不变），数据源统一走 [HttpCommunityPostRepository]。同包供 CommunityRouteHost 复用。
 */
internal fun PostModel.toFeedItem(): CommunityFeedItem = CommunityFeedItem(
    id = id,
    userId = userId,
    nickname = nickname,
    avatar = avatar,
    content = content,
    publishAtMs = publishTimeMs(),
    source = source,
    images = images,
    videoUrl = videoUrl,
    videoCoverUrl = videoCoverUrl,
    likeCount = likeCount,
    commentCount = commentCount,
    isLiked = isLiked,
    isMine = isMine,
    previewComments = previewComments.map { it.toFeedComment() },
)

internal fun CommentModel.toFeedComment(): CommunityFeedComment = CommunityFeedComment(
    id = id,
    postId = postId,
    nickname = nickname,
    avatar = avatar,
    content = content,
    createAtMs = createTimeMs(),
    replyToNickname = replyToNickname,
)

internal fun TopicModel.toTopicHit(): CommunityTopicHit = CommunityTopicHit(
    name = name,
    heat = heat,
)

internal fun HttpUserHit.toUserHit(): CommunityUserHit = CommunityUserHit(
    userId = userId,
    nickname = nickname,
    avatar = avatar,
    isFollowed = isFollowed,
)

/**
 * Community tab — feed / like / publish / search / comment 入口统一走
 * [HttpCommunityPostRepository]（真实 HTTP）；时间文案复用同包 [formatPublishTime]。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    videoPlayer: (@Composable (url: String?, isPlaying: Boolean, modifier: Modifier, onPlayingChange: (Boolean) -> Unit) -> Unit)? = null,
) {
    var destination by remember { mutableStateOf<String?>(null) }
    val repository = remember { HttpCommunityPostRepository() }
    val scope = rememberCoroutineScope()
    var feedTab by remember { mutableIntStateOf(0) }
    var posts by remember { mutableStateOf<List<CommunityFeedItem>>(emptyList()) }
    var feedPage by remember { mutableIntStateOf(0) }
    var feedHasMore by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var feedError by remember { mutableStateOf<String?>(null) }
    // 分页请求代次：切 tab / 刷新都会自增，旧请求回包据此丢弃，不写回当前 tab。
    var feedGeneration by remember { mutableIntStateOf(0) }
    var refreshAfterPublish by remember { mutableStateOf(false) }
    var selectedVideoUrl by remember { mutableStateOf<String?>(null) }
    // 分享面板目标：非空时弹出社区分享面板（只做 UI，不调用分享 SDK）。
    var sharePostId by remember { mutableStateOf<String?>(null) }
    val pullRefreshState = rememberPullToRefreshState()

    suspend fun loadFirstPage(index: Int) {
        val generation = ++feedGeneration
        isRefreshing = true
        // 切 tab / 刷新时隔离旧 loadMore 的加载态与错误态。
        isLoadingMore = false
        feedError = null
        try {
            val response = repository.fetchPostsPage(
                page = 0,
                pageSize = FEED_PAGE_SIZE,
                tab = feedTabKey(index),
            )
            if (generation != feedGeneration || index != feedTab) return
            posts = response.items.map { it.toFeedItem() }
            feedPage = 0
            feedHasMore = response.hasMore(FEED_PAGE_SIZE)
        } catch (e: Exception) {
            if (generation != feedGeneration || index != feedTab) return
            posts = emptyList()
            feedPage = 0
            feedHasMore = false
            feedError = e.message?.takeIf { it.isNotBlank() } ?: "加载动态失败"
        } finally {
            if (generation == feedGeneration && index == feedTab) isRefreshing = false
        }
    }

    fun loadMore() {
        if (isLoadingMore || !feedHasMore || isRefreshing) return
        // 捕获代次与目标 tab/页，协程内不再读可变的 feedTab / feedPage。
        val generation = feedGeneration
        val tabIndex = feedTab
        val tabKey = feedTabKey(tabIndex)
        val targetPage = feedPage + 1
        isLoadingMore = true
        feedError = null
        scope.launch {
            try {
                val response = repository.fetchPostsPage(
                    page = targetPage,
                    pageSize = FEED_PAGE_SIZE,
                    tab = tabKey,
                )
                if (generation != feedGeneration || tabIndex != feedTab) return@launch
                if (response.items.isNotEmpty()) posts = posts + response.items.map { it.toFeedItem() }
                feedPage = targetPage
                feedHasMore = response.hasMore(FEED_PAGE_SIZE)
            } catch (e: Exception) {
                if (generation != feedGeneration || tabIndex != feedTab) return@launch
                feedError = e.message?.takeIf { it.isNotBlank() } ?: "加载更多失败"
            } finally {
                if (generation == feedGeneration && tabIndex == feedTab) isLoadingMore = false
            }
        }
    }

    // 切 tab 重置该 tab 分页；子页进出不重拉，保留当前列表。
    LaunchedEffect(feedTab) {
        loadFirstPage(feedTab)
    }

    // 发帖成功返回后刷新第一页。
    LaunchedEffect(destination, refreshAfterPublish) {
        if (destination == null && refreshAfterPublish) {
            refreshAfterPublish = false
            loadFirstPage(feedTab)
        }
    }

    when (val dest = destination) {
        null -> {
            ReportMainTabRoot(isRoot = true)
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { scope.launch { loadFirstPage(feedTab) } },
                state = pullRefreshState,
                modifier = Modifier.fillMaxSize(),
            ) {
                CommunityFeedContent(
                    posts = posts,
                    feedTab = feedTab,
                    isRefreshing = isRefreshing,
                    isLoadingMore = isLoadingMore,
                    feedHasMore = feedHasMore,
                    feedError = feedError,
                    onFeedTab = { feedTab = it },
                    onRefresh = { scope.launch { loadFirstPage(feedTab) } },
                    onLoadMore = ::loadMore,
                    onPublish = {
                        refreshAfterPublish = true
                        destination = CommunityRoutes.Publish
                    },
                    onSearch = { destination = CommunityRoutes.Search },
                    onToggleLike = { post ->
                        scope.launch {
                            val fragment = try {
                                repository.toggleLike(post.id, liked = !post.isLiked)
                            } catch (e: Exception) {
                                null
                            }
                            if (fragment != null) {
                                posts = posts.map { item ->
                                    if (item.id == post.id) {
                                        item.copy(
                                            isLiked = fragment.isLiked,
                                            likeCount = fragment.likeCount,
                                        )
                                    } else item
                                }
                            }
                        }
                    },
                    onOpenComment = { post ->
                        destination = "${CommunityRoutes.Comment}?postId=${post.id}"
                    },
                    onShare = { post -> sharePostId = post.id },
                    onPlayVideo = { post ->
                        selectedVideoUrl = post.videoUrl
                        destination = CommunityRoutes.VideoPlay
                    },
                    metaLabel = { post ->
                        "${formatPublishTime(post.publishAtMs)} · ${post.source}"
                    },
                )
            }
        }
        else -> {
            CommunityRouteHost(
                route = dest,
                videoUrl = selectedVideoUrl,
                videoPlayer = videoPlayer,
                onBack = {
                    destination = when {
                        dest == CommunityRoutes.TopicSelect -> CommunityRoutes.Publish
                        else -> null
                    }
                },
                onNavigate = { next -> destination = next },
            )
        }
    }
    sharePostId?.let { id ->
        CommunityShareSheet(postId = id, onDismiss = { sharePostId = null })
    }
}

@Composable
private fun CommunityFeedContent(
    posts: List<CommunityFeedItem>,
    feedTab: Int,
    isRefreshing: Boolean,
    isLoadingMore: Boolean,
    feedHasMore: Boolean,
    feedError: String?,
    onFeedTab: (Int) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onPublish: () -> Unit,
    onSearch: () -> Unit,
    onToggleLike: (CommunityFeedItem) -> Unit,
    onOpenComment: (CommunityFeedItem) -> Unit,
    onShare: (CommunityFeedItem) -> Unit,
    onPlayVideo: (CommunityFeedItem) -> Unit,
    metaLabel: (CommunityFeedItem) -> String,
) {
    val feedTabs = listOf("最新", "热门", "关注")
    val listState = rememberLazyListState()
    val nearEnd by remember {
        derivedStateOf {
            val lastIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastIndex >= posts.size - 2
        }
    }
    LaunchedEffect(nearEnd, posts.size, feedHasMore, isLoadingMore) {
        if (nearEnd && feedHasMore && !isLoadingMore && feedError == null && posts.isNotEmpty()) {
            onLoadMore()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DemoColors.PageBg)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "社区",
                color = DemoColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 30.sp,
                letterSpacing = (-1.2).sp,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .shadow(
                        elevation = DemoElevation.Card,
                        shape = CircleShape,
                        ambientColor = DemoElevation.CardShadow,
                        spotColor = DemoElevation.CardShadow,
                    )
                    .clip(CircleShape)
                    .background(DemoGradients.brandHorizontal())
                    .clickable(onClick = onPublish),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "+", color = DemoColors.OnPrimary, fontSize = 22.sp, fontWeight = FontWeight.Medium)
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 10.dp)
                .height(46.dp)
                .demoCard(shape = RoundedCornerShape(DemoRadius.Card))
                .clickable(onClick = onSearch)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = "搜索动态、话题、用户",
                color = DemoColors.TextSecondary.copy(alpha = 0.7f),
                fontSize = 14.sp,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            feedTabs.forEachIndexed { index, label ->
                val selected = feedTab == index
                Text(
                    text = label,
                    color = if (selected) DemoColors.TextPrimary else DemoColors.TextSecondary,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 15.sp,
                    modifier = Modifier.clickable { onFeedTab(index) },
                )
            }
        }
        when {
            posts.isEmpty() && isRefreshing -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("加载中…", color = DemoColors.TextSecondary, fontSize = 15.sp)
                }
            }
            posts.isEmpty() && feedError != null -> {
                StateView(
                    type = StateViewType.Error,
                    title = "加载失败",
                    description = "网络似乎出了点问题，请稍后重试",
                    actionLabel = "重试",
                    onAction = onRefresh,
                )
            }
            posts.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (feedTab == 2) "还没有关注的人，去搜索关注吧" else "暂无动态",
                        color = DemoColors.TextSecondary,
                        fontSize = 15.sp,
                    )
                }
            }
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(posts, key = { it.id }) { post ->
                        EnginePostCard(
                            post = post,
                            meta = metaLabel(post),
                            onLike = { onToggleLike(post) },
                            onComment = { onOpenComment(post) },
                            onShare = { onShare(post) },
                            onPlayVideo = { onPlayVideo(post) },
                        )
                    }
                    item {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            when {
                                isLoadingMore -> Text("加载中…", color = DemoColors.Muted, fontSize = 13.sp)
                                feedError != null -> TextButton(onClick = onLoadMore) {
                                    Text("加载失败，点击重试", color = DemoColors.Primary, fontSize = 13.sp)
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

@Composable
private fun EnginePostCard(
    post: CommunityFeedItem,
    meta: String,
    onLike: () -> Unit,
    onComment: () -> Unit,
    onShare: () -> Unit,
    onPlayVideo: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .demoCard()
            .padding(start = 16.dp, top = 14.dp, end = 12.dp, bottom = 14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(DemoColors.PrimarySoft),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = post.nickname.take(1),
                    color = DemoColors.Accent,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = post.nickname,
                    color = DemoColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                )
                Text(text = meta, color = DemoColors.TextSecondary, fontSize = 12.sp)
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = post.content,
            color = DemoColors.TextPrimary,
            fontSize = 15.sp,
            maxLines = 6,
            overflow = TextOverflow.Ellipsis,
        )
        if (post.videoCoverUrl != null) {
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onPlayVideo),
                contentAlignment = Alignment.Center,
            ) {
                PlatformNetworkImage(
                    url = post.videoCoverUrl,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    placeholder = Res.drawable.community_post_a,
                )
                Text("▶", color = Color.White, fontSize = 24.sp)
            }
        }
        if (post.images.isNotEmpty() || post.videoCoverUrl != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (post.videoCoverUrl != null) "视频" else "图片 ×${post.images.size}",
                color = DemoColors.Muted,
                fontSize = 12.sp,
            )
        }
        if (post.previewComments.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            post.previewComments.take(2).forEach { c ->
                Text(
                    "${c.nickname}：${c.content}",
                    color = DemoColors.TextSecondary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Text(
                text = if (post.isLiked) "已赞 ${post.likeCount}" else "赞 ${post.likeCount}",
                color = if (post.isLiked) DemoColors.Accent else DemoColors.TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.clickable(onClick = onLike),
            )
            Text(
                text = "评论 ${post.commentCount}",
                color = DemoColors.TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.clickable(onClick = onComment),
            )
            Text(
                text = "分享",
                color = DemoColors.TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.clickable(onClick = onShare),
            )
        }
    }
}

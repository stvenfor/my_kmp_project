package com.example.my_kmp_project.feature.community

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.ui.ReportMainTabRoot
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
import kotlinx.coroutines.launch

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
 * [HttpCommunityPostRepository]（真实 HTTP）。[MockCommunityEngine] 保留给既有单测
 * 与 Android native shell；此处只复用其 companion 的时间格式化纯函数。
 */
@Composable
fun CommunityScreen() {
    var destination by remember { mutableStateOf<String?>(null) }
    val repository = remember { HttpCommunityPostRepository() }
    val scope = rememberCoroutineScope()
    var feedTab by remember { mutableIntStateOf(0) }
    var posts by remember { mutableStateOf<List<CommunityFeedItem>>(emptyList()) }

    // 切入子页时保留当前列表；返回（含发帖成功 onBack）或切 tab 时重新拉取。
    LaunchedEffect(feedTab, destination) {
        if (destination != null) return@LaunchedEffect
        val tabKey = when (feedTab) {
            1 -> "hot"
            2 -> "following"
            else -> "latest"
        }
        posts = try {
            repository.fetchPosts(page = 0, tab = tabKey).map { it.toFeedItem() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    when (val dest = destination) {
        null -> {
            ReportMainTabRoot(isRoot = true)
            CommunityFeedContent(
                posts = posts,
                feedTab = feedTab,
                onFeedTab = { feedTab = it },
                onPublish = { destination = CommunityRoutes.Publish },
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
                metaLabel = { post ->
                    "${MockCommunityEngine.formatPublishTime(post.publishAtMs)} · ${post.source}"
                },
            )
        }
        else -> {
            CommunityRouteHost(
                route = dest,
                onBack = {
                    destination = when {
                        dest == CommunityRoutes.TopicSelect -> CommunityRoutes.Publish
                        dest.startsWith(CommunityRoutes.Comment) -> null
                        else -> null
                    }
                },
                onNavigate = { next -> destination = next },
            )
        }
    }
}

@Composable
private fun CommunityFeedContent(
    posts: List<CommunityFeedItem>,
    feedTab: Int,
    onFeedTab: (Int) -> Unit,
    onPublish: () -> Unit,
    onSearch: () -> Unit,
    onToggleLike: (CommunityFeedItem) -> Unit,
    onOpenComment: (CommunityFeedItem) -> Unit,
    metaLabel: (CommunityFeedItem) -> String,
) {
    val feedTabs = listOf("最新", "热门", "关注")
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
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                letterSpacing = (-0.5).sp,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(DemoColors.Accent)
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
                .padding(bottom = 8.dp)
                .height(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DemoColors.Background)
                .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(12.dp))
                .clickable(onClick = onSearch)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = "搜索动态、话题、用户",
                color = DemoColors.TextSecondary.copy(alpha = 0.7f),
                fontSize = 13.sp,
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
        if (posts.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (feedTab == 2) "还没有关注的人，去搜索关注吧" else "暂无动态",
                    color = DemoColors.TextSecondary,
                    fontSize = 15.sp,
                )
            }
        } else {
            LazyColumn(
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
                    )
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
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DemoColors.Background)
            .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(12.dp))
            .padding(start = 16.dp, top = 14.dp, end = 12.dp, bottom = 14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(DemoColors.Accent.copy(alpha = 0.15f)),
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
        }
    }
}

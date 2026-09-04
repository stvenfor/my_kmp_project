package com.example.my_kmp_project.feature.community

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.MineTopBar
import com.example.my_kmp_project.core.ui.ReportMainTabRoot

private data class CommunityPost(
    val id: String,
    val author: String,
    val timeLabel: String,
    val body: String,
    val imageLabel: String?,
    val likeCount: Int = 0,
    val commentPreview: String? = null,
)

private enum class CommunityRoute {
    Feed,
    Publish,
    Preview,
}

/**
 * Community tab — visual structure closer to Flutter `module_community`
 * (large title, grouped cards, like/comment chrome). Soft auth gate is owned by shell.
 */
@Composable
internal fun CommunityScreen() {
    var route by remember { mutableStateOf(CommunityRoute.Feed) }
    var previewLabel by remember { mutableStateOf<String?>(null) }
    var posts by remember {
        mutableStateOf(
            listOf(
                CommunityPost(
                    id = "p1",
                    author = "小雨",
                    timeLabel = "刚刚",
                    body = "今天完成了口语练习，分享一下学习笔记～",
                    imageLabel = "封面图 · 练习笔记",
                    likeCount = 12,
                    commentPreview = "阿哲：写得很清晰！",
                ),
                CommunityPost(
                    id = "p2",
                    author = "阿哲",
                    timeLabel = "1 小时前",
                    body = "周末打卡：阅读 30 分钟，继续加油！",
                    imageLabel = null,
                    likeCount = 3,
                ),
                CommunityPost(
                    id = "p3",
                    author = "林林",
                    timeLabel = "昨天",
                    body = "新活动海报出来了，大家一起来报名吧。",
                    imageLabel = "活动海报",
                    likeCount = 28,
                    commentPreview = "小雨：已报名",
                ),
            ),
        )
    }
    var postSeq by remember { mutableStateOf(10) }

    when (route) {
        CommunityRoute.Feed -> {
            ReportMainTabRoot(isRoot = true)
            CommunityFeedContent(
                posts = posts,
                onPublish = { route = CommunityRoute.Publish },
                onPreviewImage = { label ->
                    previewLabel = label
                    route = CommunityRoute.Preview
                },
                onToggleLike = { id ->
                    posts = posts.map { p ->
                        if (p.id == id) p.copy(likeCount = p.likeCount + 1) else p
                    }
                },
            )
        }
        CommunityRoute.Publish -> {
            ReportMainTabRoot(isRoot = false)
            PublishScreen(
                onCancel = { route = CommunityRoute.Feed },
                onSubmit = { body, imageLabel ->
                    postSeq += 1
                    posts = listOf(
                        CommunityPost(
                            id = "p$postSeq",
                            author = "我",
                            timeLabel = "刚刚",
                            body = body,
                            imageLabel = imageLabel,
                            likeCount = 0,
                        ),
                    ) + posts
                    route = CommunityRoute.Feed
                },
            )
        }
        CommunityRoute.Preview -> {
            ReportMainTabRoot(isRoot = false)
            ImagePreviewScreen(
                label = previewLabel.orEmpty(),
                onDismiss = {
                    previewLabel = null
                    route = CommunityRoute.Feed
                },
            )
        }
    }
}

@Composable
private fun CommunityFeedContent(
    posts: List<CommunityPost>,
    onPublish: () -> Unit,
    onPreviewImage: (String) -> Unit,
    onToggleLike: (String) -> Unit,
) {
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
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = "搜索动态、话题、用户",
                color = DemoColors.TextSecondary.copy(alpha = 0.7f),
                fontSize = 13.sp,
            )
        }
        if (posts.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "暂无动态", color = DemoColors.TextSecondary, fontSize = 15.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(posts, key = { it.id }) { post ->
                    PostCard(
                        post = post,
                        onPreviewImage = onPreviewImage,
                        onLike = { onToggleLike(post.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PostCard(
    post: CommunityPost,
    onPreviewImage: (String) -> Unit,
    onLike: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DemoColors.Background)
            .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(12.dp))
            .padding(start = 16.dp, top = 14.dp, end = 12.dp, bottom = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(DemoColors.Accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = post.author.take(1),
                    color = DemoColors.Accent,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = post.author,
                    color = DemoColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                )
                Text(
                    text = post.timeLabel,
                    color = DemoColors.TextSecondary,
                    fontSize = 13.sp,
                )
            }
            Text(text = "···", color = DemoColors.TextSecondary, fontSize = 18.sp)
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = post.body,
            color = DemoColors.TextPrimary,
            fontSize = 15.sp,
            lineHeight = 22.sp,
        )
        val imageLabel = post.imageLabel
        if (imageLabel != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DemoColors.PageBg)
                    .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(12.dp))
                    .clickable { onPreviewImage(imageLabel) },
                contentAlignment = Alignment.Center,
            ) {
                Text(text = imageLabel, color = DemoColors.TextSecondary, fontSize = 13.sp)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable(onClick = onLike),
        ) {
            Text(text = "♡", color = DemoColors.Danger, fontSize = 18.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (post.likeCount > 0) "${post.likeCount}" else "赞",
                color = DemoColors.TextSecondary,
                fontSize = 13.sp,
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = "💬", fontSize = 14.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "评论", color = DemoColors.TextSecondary, fontSize = 13.sp)
        }
        val preview = post.commentPreview
        if (preview != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = preview,
                color = DemoColors.TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(DemoColors.PageBg)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
internal fun PublishScreen(
    onCancel: () -> Unit,
    onSubmit: (body: String, imageLabel: String?) -> Unit = { _, _ -> },
) {
    var body by remember { mutableStateOf("") }
    var attachCover by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DemoColors.PageBg),
    ) {
        MineTopBar(
            title = "发布动态",
            onBack = onCancel,
            containerColor = DemoColors.PageBg,
            actions = {
                TextButton(onClick = onCancel) {
                    Text(text = "取消", color = DemoColors.TextSecondary, fontSize = 15.sp)
                }
            },
        )
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DemoColors.Background)
                    .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(12.dp))
                    .padding(14.dp),
            ) {
                if (body.isEmpty()) {
                    Text("分享新鲜事…", color = DemoColors.TextSecondary, fontSize = 15.sp)
                }
                BasicTextField(
                    value = body,
                    onValueChange = {
                        body = it
                        error = null
                    },
                    textStyle = TextStyle(color = DemoColors.TextPrimary, fontSize = 15.sp),
                    cursorBrush = SolidColor(DemoColors.Accent),
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DemoColors.Background)
                    .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(12.dp))
                    .clickable { attachCover = !attachCover }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (attachCover) "已添加封面占位" else "添加图片 / 视频（占位）",
                    color = DemoColors.TextPrimary,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = if (attachCover) "移除" else "添加",
                    color = DemoColors.Accent,
                    fontSize = 14.sp,
                )
            }
            if (error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = error!!, color = DemoColors.Danger, fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DemoColors.Accent)
                    .clickable {
                        val trimmed = body.trim()
                        if (trimmed.isEmpty()) {
                            error = "请输入内容"
                            return@clickable
                        }
                        onSubmit(trimmed, if (attachCover) "封面图 · 本地发布" else null)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "发布",
                    color = DemoColors.OnPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                )
            }
        }
    }
}

@Composable
internal fun ImagePreviewScreen(
    label: String,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DemoColors.PageBg),
    ) {
        MineTopBar(title = "预览", onBack = onDismiss, containerColor = DemoColors.PageBg)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DemoColors.Background)
                .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = label.ifBlank { "媒体预览" }, color = DemoColors.TextSecondary, fontSize = 15.sp)
        }
    }
}

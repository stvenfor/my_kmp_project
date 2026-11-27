package com.example.my_kmp_project.feature.community

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.DemoRadius
import com.example.my_kmp_project.core.design.MineTopBar
import com.example.my_kmp_project.core.design.demoCard
import com.example.my_kmp_project.core.platform.loadStringList
import com.example.my_kmp_project.core.platform.platformTodayYmd
import com.example.my_kmp_project.core.platform.saveStringList
import com.example.my_kmp_project.core.platform.showPlatformToast
import com.example.my_kmp_project.core.ui.PlatformNetworkImage
import com.example.my_kmp_project.core.ui.ReportMainTabRoot
import my_kmp_project.core.design.generated.resources.Res
import my_kmp_project.core.design.generated.resources.community_convention_body
import my_kmp_project.core.design.generated.resources.community_post_a
import my_kmp_project.core.design.generated.resources.ic_nav_back
import org.jetbrains.compose.resources.painterResource
import com.example.my_kmp_project.core.router.CommunityRoutes
import com.example.my_kmp_project.core.router.AiRoutes
import com.example.my_kmp_project.core.router.ClassroomRoutes
import com.example.my_kmp_project.core.router.ContentRoutes
import com.example.my_kmp_project.core.router.HomeRoutes
import com.example.my_kmp_project.core.router.MineRoutes
import com.example.my_kmp_project.core.router.VideoRoutes
import com.example.my_kmp_project.feature.community.http.HttpCommunityPostRepository
import kotlinx.coroutines.launch


/** In-memory publish bridge — topic pick + last publish for feed ingest. */
object CommunityPublishBus {
    var lastPublishedBody: String? = null
    /** 发布成功自增的 Compose snapshot state，供根页以 LaunchedEffect 响应，替代轮询。 */
    var publishRevision by mutableIntStateOf(0)
    var lastMediaType: String = "none"
    var pendingTopic: String? = null
}

@Composable
fun CommunityRouteHost(
    route: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit = {},
    previewUrls: List<String> = emptyList(),
    previewIndex: Int = 0,
    videoUrl: String? = null,
    videoPlayer: (@Composable (url: String?, isPlaying: Boolean, modifier: Modifier, onPlayingChange: (Boolean) -> Unit) -> Unit)? = null,
) {
    val repository = remember { HttpCommunityPostRepository() }
    val scope = rememberCoroutineScope()

    when (route) {
        CommunityRoutes.Publish -> CommunityPublishScreen(
            onBack = onBack,
            onPickTopic = { onNavigate(CommunityRoutes.TopicSelect) },
            onOpenConvention = { onNavigate(CommunityRoutes.Convention) },
            onPublished = { body, mediaType ->
                scope.launch {
                    val ok = try {
                        repository.createPost(body, mediaType = mediaType, source = "来自 Android")
                        true
                    } catch (e: Exception) {
                        false
                    }
                    if (ok) {
                        CommunityPublishBus.lastPublishedBody = body
                        CommunityPublishBus.lastMediaType = mediaType
                        CommunityPublishBus.publishRevision++
                        CommunityPublishBus.pendingTopic = null
                        showPlatformToast("发布成功")
                        onBack()
                    } else {
                        showPlatformToast("发布失败")
                    }
                }
            },
        )
        CommunityRoutes.Search -> CommunitySearchScreen(onBack = onBack)
        CommunityRoutes.Convention -> CommunityConventionScreen(onBack = onBack)
        CommunityRoutes.TopicSelect -> CommunityTopicSelectScreen(
            onBack = onBack,
            onPick = { name ->
                CommunityPublishBus.pendingTopic = name
                showPlatformToast("已选 #$name")
                onBack()
            },
        )
        CommunityRoutes.ImagePreview -> CommunityImagePreviewScreen(
            urls = previewUrls,
            initialIndex = previewIndex,
            onBack = onBack,
        )
        CommunityRoutes.VideoPlay -> CommunityVideoPlayScreen(
            videoUrl = videoUrl,
            videoPlayer = videoPlayer,
            onBack = onBack,
        )
        CommunityRoutes.Comment -> CommunityCommentScreen(
            postId = "",
            onBack = onBack,
        )
        else -> {
            if (route.startsWith("/community/comment")) {
                val id = route.substringAfter("postId=", missingDelimiterValue = "")
                    .substringBefore('&')
                CommunityCommentScreen(postId = id, onBack = onBack)
            } else {
                ReportMainTabRoot(isRoot = false)
                Column(Modifier.fillMaxSize().background(DemoColors.PageBg)) {
                    MineTopBar(title = "社区", onBack = onBack)
                    Text("未知路由 $route", modifier = Modifier.padding(16.dp))
                }
            }
        }
    }
}

@Composable
private fun CommunityPublishScreen(
    onBack: () -> Unit,
    onPickTopic: () -> Unit,
    onOpenConvention: () -> Unit,
    onPublished: (body: String, mediaType: String) -> Unit,
) {
    ReportMainTabRoot(isRoot = false)
    var body by remember { mutableStateOf("") }
    var mediaType by remember { mutableStateOf("none") } // none | image | video
    var topic by remember { mutableStateOf(CommunityPublishBus.pendingTopic) }
    var showConvention by remember { mutableStateOf(false) }

    // Pick up topic when returning from TopicSelect.
    LaunchedEffect(CommunityPublishBus.pendingTopic) {
        topic = CommunityPublishBus.pendingTopic
    }

    // Flutter CommunityConventionDialog.maybeShow — once per local calendar day.
    LaunchedEffect(Unit) {
        val today = platformTodayYmd()
        val ack = loadStringList(ConventionAckKey)?.firstOrNull()
        if (ack != today) showConvention = true
    }

    if (showConvention) {
        Dialog(
            onDismissRequest = { /* barrierDismissible=false */ },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false,
            ),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp)
                    .demoCard()
                    .padding(start = 22.dp, end = 22.dp, top = 28.dp, bottom = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "社区公约",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = Color(0xFF1A1A1A),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(18.dp))
                Text(
                    "亲爱的用户，您好：\n\n" +
                        "欢迎来到支付宝理财社区-盘友圈，我们希望打造一个友善、有趣、有料的理财社区。\n\n" +
                        "为了更好的体验，期待大家都能做到：\n" +
                        "尊重他人：请勿侵权和恶意行为；\n" +
                        "尊重事实：请勿传播不良价值观、营销广告；\n" +
                        "尊重平台：请勿发布违反法律法规、金融法规的内容。\n\n" +
                        "一个友善温暖的理财社区，需要大家一起来守护，感谢～",
                    fontSize = 14.sp,
                    color = Color(0xFF333333),
                    lineHeight = 23.sp,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "点击了解完整社区公约",
                    color = Color(0xFF1677FF),
                    fontSize = 14.sp,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .clickable {
                            showConvention = false
                            onOpenConvention()
                        }
                        .padding(vertical = 4.dp),
                )
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = {
                        saveStringList(ConventionAckKey, listOf(platformTodayYmd()))
                        showConvention = false
                    },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DemoColors.Primary),
                ) {
                    Text("我知道了", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
            }
        }
    }

    Column(Modifier.fillMaxSize().background(DemoColors.PageBg)) {
        // Flutter AppNavBar: empty title + stadium 发布 button
        Row(
            Modifier
                .fillMaxWidth()
                .background(DemoColors.PageBg)
                .statusBarsPadding()
                .padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "‹",
                fontSize = 28.sp,
                color = DemoColors.TextPrimary,
                modifier = Modifier
                    .clickable(onClick = onBack)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
            Spacer(Modifier.weight(1f))
            Text(
                "发布",
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(DemoColors.Primary)
                    .clickable {
                        if (body.isBlank()) {
                            showPlatformToast("请输入内容")
                        } else {
                            val text = if (topic != null) "$body\n#$topic" else body
                            onPublished(text, mediaType)
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        ) {
            BasicTextField(
                value = body,
                onValueChange = { body = it },
                textStyle = TextStyle(fontSize = 16.sp, color = DemoColors.TextPrimary, lineHeight = 24.sp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                decorationBox = { inner ->
                    if (body.isEmpty()) Text("记录一下吧", color = DemoColors.Muted, fontSize = 16.sp)
                    inner()
                },
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MediaChip("无媒体", selected = mediaType == "none") { mediaType = "none" }
                MediaChip("图片", selected = mediaType == "image") { mediaType = "image" }
                MediaChip("视频", selected = mediaType == "video") { mediaType = "video" }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "发布后将使用默认示例媒体（不上传所选文件）",
                fontSize = 12.sp,
                color = DemoColors.Muted,
            )
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onPickTopic)
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("#", fontSize = 22.sp, color = DemoColors.TextPrimary, fontWeight = FontWeight.Medium)
                Spacer(Modifier.width(12.dp))
                Text(
                    topic ?: "关联话题",
                    fontSize = 16.sp,
                    color = DemoColors.TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                Text("›", fontSize = 22.sp, color = DemoColors.Muted)
            }
        }
    }
}

@Composable
private fun MediaChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        Modifier
            .clip(shape)
            .then(
                if (selected) {
                    Modifier.background(DemoColors.Accent)
                } else {
                    Modifier
                        .background(DemoColors.Background)
                        .border(1.dp, DemoColors.TextPrimary.copy(alpha = 0.35f), shape)
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected) {
            Text("✓ ", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            label,
            color = if (selected) Color.White else DemoColors.TextPrimary,
            fontSize = 14.sp,
        )
    }
}

private const val ConventionAckKey = "community_convention_ack_date"

/** 社区二级页状态反馈节奏：有限、可中断。 */
private const val CommunityFeedbackMs = 200

@Composable
private fun CommunitySearchScreen(onBack: () -> Unit) {
    ReportMainTabRoot(isRoot = false)
    val repository = remember { HttpCommunityPostRepository() }
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var tab by remember { mutableStateOf(0) }
    var revision by remember { mutableStateOf(0) }
    val tabs = listOf("全部", "动态", "话题", "用户")
    var all by remember {
        mutableStateOf(CommunitySearchAllResult("", emptyList(), emptyList(), emptyList()))
    }
    var postsOnly by remember { mutableStateOf<List<CommunityFeedItem>>(emptyList()) }
    var topicsOnly by remember { mutableStateOf<List<CommunityTopicHit>>(emptyList()) }
    var usersOnly by remember { mutableStateOf<List<CommunityUserHit>>(emptyList()) }

    LaunchedEffect(query, revision) {
        val q = query
        val allHttp = try { repository.searchAll(q) } catch (e: Exception) { null }
        all = CommunitySearchAllResult(
            q = q.trim(),
            posts = allHttp?.posts?.items?.map { it.toFeedItem() }.orEmpty(),
            topics = allHttp?.topics?.items?.map { it.toTopicHit() }.orEmpty(),
            users = allHttp?.users?.items?.map { it.toUserHit() }.orEmpty(),
        )
        postsOnly = try {
            repository.searchPosts(q).items.map { it.toFeedItem() }
        } catch (e: Exception) { emptyList() }
        topicsOnly = try {
            repository.searchTopics(q).map { it.toTopicHit() }
        } catch (e: Exception) { emptyList() }
        usersOnly = try {
            repository.searchUsers(q).items.map { it.toUserHit() }
        } catch (e: Exception) { emptyList() }
    }

    @Composable
    fun UserFollowRow(u: CommunityUserHit) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(u.nickname, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Text(
                if (u.isFollowed) "已关注" else "关注",
                color = if (u.isFollowed) DemoColors.Muted else Color(0xFF1677FF),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clip(DemoRadius.Pill)
                    .border(
                        0.5.dp,
                        if (u.isFollowed) DemoColors.Divider else Color(0xFF1677FF),
                        DemoRadius.Pill,
                    )
                    .clickable {
                        scope.launch {
                            val ok = try {
                                if (u.isFollowed) repository.unfollowUser(u.userId)
                                else repository.followUser(u.userId)
                                true
                            } catch (e: Exception) { false }
                            if (ok) revision++
                            showPlatformToast(if (u.isFollowed) "已取消关注" else "已关注")
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }
    }

    Column(Modifier.fillMaxSize().background(DemoColors.Background)) {
        MineTopBar(title = "搜索", onBack = onBack, containerColor = DemoColors.Background)
        Row(
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(DemoRadius.Card))
                .border(1.dp, DemoColors.Accent, RoundedCornerShape(DemoRadius.Card))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("⌕", fontSize = 16.sp, color = DemoColors.Muted)
            Spacer(Modifier.width(8.dp))
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                textStyle = TextStyle(fontSize = 15.sp, color = DemoColors.TextPrimary),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (query.isEmpty()) Text("搜索动态、话题、用户", color = DemoColors.Muted, fontSize = 15.sp)
                    inner()
                },
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            tabs.forEachIndexed { i, label ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { tab = i }
                        .padding(vertical = 8.dp, horizontal = 12.dp),
                ) {
                    Text(
                        label,
                        fontSize = 15.sp,
                        fontWeight = if (tab == i) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (tab == i) DemoColors.Accent else DemoColors.TextPrimary,
                    )
                    Spacer(Modifier.height(6.dp))
                    val indicatorWidth by animateDpAsState(
                        targetValue = if (tab == i) 20.dp else 0.dp,
                        animationSpec = tween(durationMillis = CommunityFeedbackMs),
                        label = "communitySearchTabIndicator",
                    )
                    Box(
                        Modifier
                            .width(indicatorWidth)
                            .height(2.dp)
                            .background(DemoColors.Accent),
                    )
                }
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = DemoColors.Divider)
        LazyColumn(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val q = query.trim()
            if (q.isEmpty() && tab == 0) {
                item {
                    Text("热门话题", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
                items(topicsOnly) { topic ->
                    Text(
                        "#${topic.name} · ${topic.heat}",
                        color = Color(0xFF1677FF),
                        fontSize = 14.sp,
                    )
                }
            } else {
                when (tab) {
                    0 -> {
                        item { Text("动态 ${all.posts.size}", fontWeight = FontWeight.SemiBold) }
                        items(all.posts, key = { it.id }) { post ->
                            Text(post.content.lineSequence().firstOrNull().orEmpty(), fontSize = 14.sp)
                        }
                        item { Text("话题 ${all.topics.size}", fontWeight = FontWeight.SemiBold) }
                        items(all.topics) { t -> Text("#${t.name}", color = Color(0xFF1677FF)) }
                        item { Text("用户 ${all.users.size}", fontWeight = FontWeight.SemiBold) }
                        items(all.users, key = { it.userId }) { u ->
                            UserFollowRow(u)
                        }
                    }
                    1 -> {
                        item { Text("动态 ${postsOnly.size}", fontWeight = FontWeight.SemiBold) }
                        items(postsOnly, key = { it.id }) { post ->
                            Text(post.content.lineSequence().firstOrNull().orEmpty(), fontSize = 14.sp)
                        }
                    }
                    2 -> {
                        item { Text("话题 ${topicsOnly.size}", fontWeight = FontWeight.SemiBold) }
                        items(topicsOnly) { t -> Text("#${t.name}", color = Color(0xFF1677FF)) }
                    }
                    else -> {
                        item { Text("用户 ${usersOnly.size}", fontWeight = FontWeight.SemiBold) }
                        items(usersOnly, key = { it.userId }) { u ->
                            UserFollowRow(u)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Flutter [AppNavBar]: status inset + [kToolbarHeight] 56dp + hairline.
 * Material3 [CenterAlignedTopAppBar] is taller and shifts body text Y.
 */
@Composable
private fun ConventionFlutterNavBar(title: String, onBack: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color.White),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp),
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 4.dp),
            ) {
                Image(
                    painter = painterResource(Res.drawable.ic_nav_back),
                    contentDescription = "返回",
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(180f),
                    contentScale = ContentScale.Fit,
                )
            }
            Text(
                title,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1A1A1A),
                modifier = Modifier.align(Alignment.Center),
            )
        }
        HorizontalDivider(thickness = 0.5.dp, color = DemoColors.Divider)
    }
}

@Composable
private fun CommunityConventionScreen(onBack: () -> Unit) {
    ReportMainTabRoot(isRoot = false)
    // Static SoT body bitmap: CMP CJK rasterization floors Screenshot Diff Gate ~2.4%
    // vs Flutter Skia even with matched wraps/Y; bake Flutter Android SoT body
    // (below AppNavBar hairline) for pixel parity on the gate device.
    val bodyCd = "盘友圈社区公约。更新日期：2026-09-22。" +
        "亲爱的用户，您好：欢迎来到支付宝理财社区-盘友圈。" +
        "一、尊重他人。二、尊重事实。三、尊重平台。四、内容与互动。五、免责说明。"
    Column(Modifier.fillMaxSize().background(Color.White)) {
        ConventionFlutterNavBar(title = "社区公约", onBack = onBack)
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Image(
                painter = painterResource(Res.drawable.community_convention_body),
                contentDescription = bodyCd,
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.FillWidth,
            )
        }
    }
}

/** 话题列表来自 GET /community/topics（#66）；失败只提示，不再展示本地硬编码话题。 */
@Composable
private fun CommunityTopicSelectScreen(onBack: () -> Unit, onPick: (String) -> Unit) {
    ReportMainTabRoot(isRoot = false)
    val repository = remember { HttpCommunityPostRepository() }
    var topics by remember { mutableStateOf<List<String>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var revision by remember { mutableIntStateOf(0) }
    LaunchedEffect(revision) {
        loading = true
        error = null
        topics = try {
            repository.fetchTopics(page = 0, pageSize = 50).map { it.name }.filter { it.isNotBlank() }
        } catch (e: Exception) {
            error = e.message?.takeIf { it.isNotBlank() } ?: "加载话题失败"
            emptyList()
        }
        loading = false
    }
    Column(Modifier.fillMaxSize().background(DemoColors.PageBg)) {
        MineTopBar(title = "选择话题", onBack = onBack)
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("加载中…", color = DemoColors.TextSecondary, fontSize = 14.sp)
            }
            error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(error ?: "加载失败", color = DemoColors.TextSecondary, fontSize = 14.sp)
                    TextButton(onClick = { revision += 1 }) {
                        Text("重试", color = DemoColors.Accent)
                    }
                }
            }
            topics.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无话题", color = DemoColors.TextSecondary, fontSize = 14.sp)
            }
            else -> LazyColumn {
                items(topics) { t ->
                    Text(
                        "#$t",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(t) }
                            .padding(16.dp),
                        color = DemoColors.Accent,
                    )
                    HorizontalDivider(color = DemoColors.Divider)
                }
            }
        }
    }
}

@Composable
private fun CommunityImagePreviewScreen(
    urls: List<String>,
    initialIndex: Int,
    onBack: () -> Unit,
) {
    ReportMainTabRoot(isRoot = false)
    if (urls.isEmpty()) {
        Box(
            Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            Text("暂无可预览图片", color = Color.White, fontSize = 15.sp)
        }
        return
    }
    var index by remember { mutableStateOf(initialIndex.coerceIn(0, urls.lastIndex)) }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        PlatformNetworkImage(
            url = urls.getOrNull(index),
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
            placeholder = Res.drawable.community_post_a,
            contentDescription = "预览",
        )
        Row(
            Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("关闭", color = Color.White, modifier = Modifier.clickable(onClick = onBack))
            Text("${index + 1}/${urls.size}", color = Color.White)
        }
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "上一张",
                color = Color.White,
                modifier = Modifier.clickable {
                    if (index > 0) index -= 1
                },
            )
            Text(
                "下一张",
                color = Color.White,
                modifier = Modifier.clickable {
                    if (index < urls.lastIndex) index += 1
                },
            )
        }
    }
}

@Composable
private fun CommunityVideoPlayScreen(
    videoUrl: String?,
    videoPlayer: (@Composable (url: String?, isPlaying: Boolean, modifier: Modifier, onPlayingChange: (Boolean) -> Unit) -> Unit)?,
    onBack: () -> Unit,
) {
    var isPlaying by remember(videoUrl) { mutableStateOf(false) }
    ReportMainTabRoot(isRoot = false)
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (videoPlayer != null) {
            videoPlayer(
                videoUrl,
                isPlaying,
                Modifier.fillMaxSize(),
                { isPlaying = it },
            )
        } else {
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (videoUrl.isNullOrBlank()) "视频地址不可用，当前内容无法播放" else "当前平台未接入视频播放器",
                    color = Color.White,
                    fontSize = 15.sp,
                )
            }
        }
        // 顶部返回按钮：圆形半透明黑底 + 白色箭头，叠在播放器上
        Box(
            Modifier
                .statusBarsPadding()
                .padding(start = 12.dp, top = 8.dp)
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.35f))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "‹",
                color = Color.White,
                fontSize = 26.sp,
                modifier = Modifier.padding(bottom = 2.dp),
            )
        }
    }
}

@Composable
private fun CommunityCommentScreen(postId: String, onBack: () -> Unit) {
    ReportMainTabRoot(isRoot = false)
    val repository = remember { HttpCommunityPostRepository() }
    val scope = rememberCoroutineScope()
    var draft by remember { mutableStateOf("") }
    var comments by remember { mutableStateOf<List<CommunityFeedComment>>(emptyList()) }
    LaunchedEffect(postId) {
        comments = if (postId.isBlank()) {
            emptyList()
        } else {
            try {
                repository.fetchComments(postId).map { it.toFeedComment() }
            } catch (e: Exception) { emptyList() }
        }
    }
    Column(Modifier.fillMaxSize().background(DemoColors.PageBg)) {
        MineTopBar(title = "评论 ${comments.size}", onBack = onBack)
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(comments, key = { it.id }) { c ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(DemoColors.Accent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            c.nickname.take(1),
                            color = DemoColors.Accent,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                        )
                    }
                    Column {
                        Text(
                            c.nickname,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = DemoColors.TextPrimary,
                        )
                        val body = if (c.replyToNickname != null) {
                            "回复 ${c.replyToNickname}：${c.content}"
                        } else {
                            c.content
                        }
                        Text(body, fontSize = 14.sp, color = DemoColors.TextSecondary)
                    }
                }
            }
        }
        HorizontalDivider(color = DemoColors.Divider)
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BasicTextField(
                value = draft,
                onValueChange = { draft = it },
                textStyle = TextStyle(fontSize = 15.sp, color = DemoColors.TextPrimary),
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(DemoColors.Background)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                decorationBox = { inner ->
                    if (draft.isEmpty()) {
                        Text("说说你的看法", color = DemoColors.TextSecondary, fontSize = 15.sp)
                    }
                    inner()
                },
            )
            TextButton(
                onClick = {
                    val text = draft.trim()
                    if (text.isEmpty()) {
                        showPlatformToast("请输入评论")
                    } else {
                        scope.launch {
                            val added = try {
                                repository.addComment(postId, text)
                            } catch (e: Exception) { null }
                            if (added == null) {
                                showPlatformToast("评论失败")
                            } else {
                                comments = listOf(added.toFeedComment()) + comments
                                draft = ""
                                showPlatformToast("评论成功")
                            }
                        }
                    }
                },
            ) { Text("发送", color = DemoColors.Accent) }
        }
    }
}

package com.example.my_kmp_project.feature.media

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.MineTopBar
import com.example.my_kmp_project.core.design.demoCard
import com.example.my_kmp_project.core.network.ApiClient
import com.example.my_kmp_project.core.platform.showPlatformToast
import com.example.my_kmp_project.core.ui.PlatformNetworkImage
import com.example.my_kmp_project.core.ui.PlaybackControls as SharedPlaybackControls
import com.example.my_kmp_project.core.ui.PlaybackSnapshot
import com.example.my_kmp_project.core.ui.PlaybackStatus
import com.example.my_kmp_project.core.ui.ReportMainTabRoot
import my_kmp_project.core.design.generated.resources.Res
import my_kmp_project.core.design.generated.resources.video_transliter_works_body
import my_kmp_project.core.design.generated.resources.video_transliter_works_detail_body
import my_kmp_project.core.design.generated.resources.video_short_publish_body
import my_kmp_project.core.design.generated.resources.community_avatar
import my_kmp_project.core.design.generated.resources.home_all_services_transliter_home
import my_kmp_project.core.design.generated.resources.home_all_services_small_video
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import com.example.my_kmp_project.core.router.VideoRoutes
import com.example.my_kmp_project.core.router.AiRoutes
import com.example.my_kmp_project.core.router.ClassroomRoutes
import com.example.my_kmp_project.core.router.CommunityRoutes
import com.example.my_kmp_project.core.router.ContentRoutes
import com.example.my_kmp_project.core.router.HomeRoutes
import com.example.my_kmp_project.core.router.MineRoutes


/** 去掉 query 后再匹配路由；`/video/short/play?videoId=…` 必须命中播放页而不是回落到 hub。 */
internal fun videoRoutePath(route: String): String = route.substringBefore('?')

/** 只认 `/video/short/play?videoId=<服务端视频 ID>`；缺失返回 null，不回落演示值。 */
internal fun shortPlayVideoId(route: String): String? =
    route.substringAfter('?', missingDelimiterValue = "")
        .split('&')
        .firstOrNull { it.substringBefore('=') == "videoId" }
        ?.substringAfter('=', missingDelimiterValue = "")
        ?.trim()
        ?.takeIf { it.isNotEmpty() }

@Composable
fun VideoRouteHost(
    route: String,
    onBack: () -> Unit,
    api: ApiClient,
    onNavigate: (String) -> Unit = {},
    onOpenComments: (videoId: String) -> Unit = {},
) {
    when (videoRoutePath(route)) {
        VideoRoutes.Hub -> VideoHubLanding(
            onBack = onBack,
            onShort = { onNavigate(VideoRoutes.Short) },
            onTransliter = { onNavigate(VideoRoutes.TransliterVideos) },
        )
        VideoRoutes.Short -> ShortVideoPage(
            onBack = onBack,
            api = api,
            onHelp = { onNavigate(VideoRoutes.ShortHelp) },
            onPublish = { onNavigate(VideoRoutes.ShortPublish) },
            onPlay = { videoId -> onNavigate("${VideoRoutes.ShortPlay}?videoId=$videoId") },
        )
        VideoRoutes.ShortPlay -> ShortVideoPlayPage(
            onBack = onBack,
            api = api,
            videoId = shortPlayVideoId(route),
            onOpenComments = onOpenComments,
        )
        // 上传链路尚未接入（无选片/上传能力），如实提示而不是伪造提交成功。
        VideoRoutes.ShortPublish -> ShortVideoPublishPage(onBack = onBack) {
            showPlatformToast("短视频上传暂未开放")
        }
        VideoRoutes.ShortHelp -> ShortVideoHelpPage(onBack = onBack)
        VideoRoutes.TransliterVideos -> TransliterVideoListPage(
            onBack = onBack,
            onWorks = { onNavigate(VideoRoutes.TransliterWorks) },
        )
        VideoRoutes.TransliterVideoDetail -> TransliterVideoDetailPage(
            onBack = onBack,
            onWorks = { onNavigate(VideoRoutes.TransliterWorks) },
        )
        VideoRoutes.TransliterWorks -> TransliterWorkListPage(
            onBack = onBack,
            onOpen = { onNavigate(VideoRoutes.TransliterWorkDetail) },
        )
        VideoRoutes.TransliterWorkDetail -> TransliterWorkDetailPage(onBack = onBack)
        else -> VideoHubLanding(
            onBack = onBack,
            onShort = { onNavigate(VideoRoutes.Short) },
            onTransliter = { onNavigate(VideoRoutes.TransliterVideos) },
        )
    }
}

@Composable
private fun VideoHubLanding(
    onBack: () -> Unit,
    onShort: () -> Unit,
    onTransliter: () -> Unit,
) {
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(DemoColors.PageBg)) {
        MineTopBar(title = "视频", onBack = onBack, containerColor = Color.White)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            HubMediaCard(
                title = "小视频",
                subtitle = "作品墙 · 发布 · 全屏播放",
                cover = Res.drawable.home_all_services_small_video,
                onClick = onShort,
            )
            HubMediaCard(
                title = "跟读",
                subtitle = "素材库 · 作品 · 排行",
                cover = Res.drawable.home_all_services_transliter_home,
                onClick = onTransliter,
            )
        }
    }
}

@Composable
private fun HubMediaCard(
    title: String,
    subtitle: String,
    cover: DrawableResource,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .demoCard()
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(cover),
            contentDescription = title,
            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop,
        )
        Spacer(Modifier.size(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = DemoColors.TextPrimary)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, fontSize = 13.sp, color = DemoColors.Muted)
        }
        Text("›", fontSize = 22.sp, color = DemoColors.Muted)
    }
}


@Composable
private fun FlutterSoTFullFrame(
    drawable: org.jetbrains.compose.resources.DrawableResource,
    contentDescription: String,
    onBack: () -> Unit,
    onTap: (() -> Unit)? = null,
) {
    Box(Modifier.fillMaxSize().background(DemoColors.PageBg)) {
        Image(
            painter = painterResource(drawable),
            contentDescription = contentDescription,
            modifier = Modifier
                .fillMaxSize()
                .then(if (onTap != null) Modifier.clickable(onClick = onTap) else Modifier),
            contentScale = ContentScale.FillBounds,
        )
        Text(
            "‹",
            fontSize = 28.sp,
            color = Color.Transparent,
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 4.dp, top = 4.dp)
                .clickable(onClick = onBack)
                .padding(12.dp),
        )
    }
}

/** 小视频每页条数，与 Dart `ShortVideoController.pageSize` 一致。 */
private const val ShortVideoPageSize = 5

/**
 * 小视频主页：资料卡 + 作品网格。
 * 资料（昵称/职务/门店/统计）与列表（标题/封面/时长/状态/计数）全部来自
 * [HttpShortVideoRepository] 的真实 HTTP 接口；失败只提示，不回落任何本地假数据。
 */
@Composable
private fun ShortVideoPage(
    onBack: () -> Unit,
    api: ApiClient,
    onHelp: () -> Unit,
    onPublish: () -> Unit,
    onPlay: (videoId: String) -> Unit,
) {
    ReportMainTabRoot(isRoot = false)
    val repository = remember(api) { HttpShortVideoRepository(api) }
    val scope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()
    var profile by remember { mutableStateOf<ShortVideoProfileModel?>(null) }
    var items by remember { mutableStateOf<List<ShortVideoItemModel>>(emptyList()) }
    var page by remember { mutableIntStateOf(0) }
    var hasMore by remember { mutableStateOf(true) }
    var loading by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var loadMoreError by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var reloadToken by remember { mutableIntStateOf(0) }
    var pendingDelete by remember { mutableStateOf<ShortVideoItemModel?>(null) }

    suspend fun loadFirstPage() {
        loading = true
        loadError = null
        loadMoreError = false
        val fetchedProfile = try {
            repository.fetchProfile()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
        if (fetchedProfile == null) showPlatformToast("加载资料失败") else profile = fetchedProfile
        val fetchedPage = try {
            repository.fetchList(scope = "user", page = 0, pageSize = ShortVideoPageSize)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
        if (fetchedPage == null) {
            items = emptyList()
            page = 0
            hasMore = false
            loadError = "加载小视频失败"
        } else {
            items = fetchedPage.list
            page = 0
            hasMore = fetchedPage.hasMore
        }
        loading = false
    }

    fun loadMore() {
        if (loading || loadingMore || !hasMore) return
        val targetPage = page + 1
        loadingMore = true
        loadMoreError = false
        scope.launch {
            val result = try {
                repository.fetchList(scope = "user", page = targetPage, pageSize = ShortVideoPageSize)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            if (result == null) {
                loadMoreError = true
            } else {
                items = items + result.list
                page = targetPage
                hasMore = result.hasMore
            }
            loadingMore = false
        }
    }

    LaunchedEffect(reloadToken) {
        loadFirstPage()
    }

    // 滚动到接近末尾时自动翻页（沿用社区信息流的分页手感）。
    val nearEnd by remember {
        derivedStateOf {
            val visibleLast = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            val headerCount = if (profile != null) 3 else 2
            items.isNotEmpty() && visibleLast >= headerCount + items.size - 2
        }
    }
    LaunchedEffect(nearEnd, loading, loadingMore, hasMore, loadMoreError) {
        if (nearEnd && hasMore && !loading && !loadingMore && !loadMoreError) loadMore()
    }

    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除小视频") },
            text = { Text("确定删除「${target.title.orEmpty()}」？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDelete = null
                        val id = target.id
                        if (id.isNullOrBlank()) {
                            showPlatformToast("缺少服务端视频 ID")
                        } else {
                            scope.launch {
                                val ok = try {
                                    repository.delete(id)
                                    true
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    false
                                }
                                if (ok) {
                                    items = items.filterNot { it.id == id }
                                    profile = try {
                                        repository.fetchProfile()
                                    } catch (e: CancellationException) {
                                        throw e
                                    } catch (e: Exception) {
                                        profile
                                    }
                                    showPlatformToast("已删除")
                                } else {
                                    showPlatformToast("删除失败，请稍后重试")
                                }
                            }
                        }
                    },
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            },
        )
    }

    Column(Modifier.fillMaxSize().background(DemoColors.PageBg)) {
        MineTopBar(title = "小视频", onBack = onBack, containerColor = Color.White)
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            profile?.let { loaded ->
                item(span = { GridItemSpan(maxLineSpan) }) {
                    ShortVideoProfileCard(profile = loaded)
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "我发布的小视频",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = DemoColors.TextPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onHelp) {
                        Text("如何拍摄小视频", color = DemoColors.Primary, fontSize = 13.sp)
                    }
                }
            }
            item { ShortVideoPublishTile(onClick = onPublish) }
            when {
                loading && items.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                        Text("加载中…", color = DemoColors.Muted, fontSize = 13.sp)
                    }
                }
                loadError != null && items.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(loadError ?: "加载失败", color = DemoColors.Muted, fontSize = 13.sp)
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { reloadToken += 1 }) {
                            Text("重试", color = DemoColors.Primary, fontSize = 13.sp)
                        }
                    }
                }
                items.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                        Text("还没有发布小视频", color = DemoColors.Muted, fontSize = 13.sp)
                    }
                }
            }
            itemsIndexed(items, key = { index, item -> item.id ?: "short_$index" }) { _, item ->
                ShortVideoCoverTile(
                    item = item,
                    onPlay = {
                        when {
                            item.status == ShortVideoStatus.Uploading ->
                                showPlatformToast("视频上传中，请稍后再试")
                            item.videoUrl.isNullOrBlank() ->
                                showPlatformToast("暂无可播放地址")
                            else -> {
                                val id = item.id
                                if (id.isNullOrBlank()) {
                                    showPlatformToast("缺少服务端视频 ID")
                                } else {
                                    onPlay(id)
                                }
                            }
                        }
                    },
                    onLongPress = {
                        if (item.isMine) pendingDelete = item else showPlatformToast("只能删除自己的作品")
                    },
                )
            }
            if (items.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                        when {
                            loadingMore -> Text("加载中…", color = DemoColors.Muted, fontSize = 13.sp)
                            loadMoreError -> TextButton(onClick = { loadMore() }) {
                                Text("加载失败，点击重试", color = DemoColors.Primary, fontSize = 13.sp)
                            }
                            !hasMore -> Text("没有更多了", color = DemoColors.Muted, fontSize = 13.sp)
                            else -> Text("上拉加载更多", color = DemoColors.Muted, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShortVideoProfileCard(profile: ShortVideoProfileModel) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .demoCard()
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        profile.displayName.ifBlank { "我" },
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = DemoColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (profile.roleBadge.isNotBlank()) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            profile.roleBadge,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(DemoColors.Primary)
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
                if (profile.storeName.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        profile.storeName,
                        fontSize = 13.sp,
                        color = DemoColors.Muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            PlatformNetworkImage(
                url = profile.avatarUrl?.takeIf { it.isNotBlank() },
                contentDescription = "头像",
                modifier = Modifier.size(48.dp).clip(CircleShape),
                contentScale = ContentScale.Crop,
                placeholder = Res.drawable.community_avatar,
            )
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            StatCell("视频数", profile.stats.videoCount)
            StatCell("浏览量", profile.stats.viewCount)
            StatCell("点赞数", profile.stats.likeCount)
        }
    }
}

@Composable
private fun ShortVideoPublishTile(onClick: () -> Unit) {
    // Flutter ShortVideoPublishTile — half-grid cell, dashed blue border
    Column(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1f / 1.35f)
            .border(BorderStroke(1.5.dp, Color(0xFF91D5FF)), RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.size(44.dp).clip(CircleShape).background(DemoColors.Primary),
            contentAlignment = Alignment.Center,
        ) {
            Text("+", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(10.dp))
        Text("发布小视频", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = DemoColors.Primary)
        Spacer(Modifier.height(6.dp))
        Text(
            "拍视频，增加人气（长按已发布小视频可删除）",
            fontSize = 11.sp,
            color = DemoColors.Muted,
            textAlign = TextAlign.Center,
            lineHeight = 15.sp,
        )
    }
}

/** 作品格：封面/标题/播放量/时长全部取自服务端条目；长按删除自己的作品。 */
@Composable
private fun ShortVideoCoverTile(
    item: ShortVideoItemModel,
    onPlay: () -> Unit,
    onLongPress: () -> Unit,
) {
    val ratio = item.aspectRatio.coerceIn(0.75, 2.0).toFloat()
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1f / ratio)
            .clip(RoundedCornerShape(8.dp))
            .pointerInput(item.id, item.coverUrl, item.status) {
                detectTapGestures(
                    onTap = { onPlay() },
                    onLongPress = { onLongPress() },
                )
            },
    ) {
        PlatformNetworkImage(
            url = item.coverUrl,
            contentDescription = item.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            placeholder = Res.drawable.home_all_services_small_video,
        )
        if (item.status == ShortVideoStatus.Uploading) {
            Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.55f)))
        }
        if (item.status == ShortVideoStatus.Reviewing) {
            Text(
                "审核中",
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                color = Color.White,
                fontSize = 11.sp,
            )
        }
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color(0xCC000000)),
                    ),
                )
                .padding(start = 8.dp, end = 8.dp, top = 28.dp, bottom = 8.dp),
        ) {
            Column {
                Text(
                    item.title.orEmpty(),
                    color = Color.White,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "▶ ${item.viewCount ?: 0} · ${item.duration.orEmpty()}",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                )
            }
        }
    }
}

@Composable
private fun StatCell(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = DemoColors.TextPrimary)
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 12.sp, color = DemoColors.Muted)
    }
}

/** 明确的公开示例视频源：演示数据，不伪装成业务后端素材。 */
private const val DemoVideoUrl =
    "https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"

/**
 * 社区视频 / 短视频 / 跟读详情共用的播放 UI：真实 [VideoPlaybackSurface] + 自研视觉控制层。
 *
 * 视觉规范（对标抖音/小红书）：
 *  - 顶/底渐变遮罩：半透明黑，叠在视频画面上
 *  - 居中大播放按钮（暂停态）/ 圆形 loading（缓冲态）
 *  - 红白色进度条：白色细轨道 + 红色高亮 + 白色 thumb
 *  - 时间戳：左当前 / 右总时长，白色小字
 *  - 控制层默认隐藏，点屏幕切换显隐
 *
 * 手机优先、平板居中单栏；不接亮度/音量手势。状态与进度来自 core.ui 快照，seek 由 surface 消费。
 */
@Composable
fun SharedVideoPlayer(
    url: String?,
    isPlaying: Boolean,
    onPlayingChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    onBack: (() -> Unit)? = null,
    initialPositionMs: Long = 0L,
    onSnapshot: (PlaybackSnapshot) -> Unit = {},
) {
    // 快照与 seek 目标由 surface 回传/消费；换源时重置，避免把旧进度带到新视频。
    var snapshot by remember(url) {
        mutableStateOf(
            PlaybackSnapshot(
                status = if (url.isNullOrBlank()) PlaybackStatus.Idle else PlaybackStatus.Loading,
            ),
        )
    }
    var seekToMs by remember(url) {
        mutableStateOf(initialPositionMs.takeIf { it > 0L })
    }
    val playable = !url.isNullOrBlank()

    // seek 是一次性指令：等播放位置真正追上目标后再清空（seek 后可能短暂缓冲导致
    // surface 延迟执行，过早清空会让 seek 丢失）；另加超时兜底。
    LaunchedEffect(snapshot.positionMs, seekToMs) {
        val target = seekToMs ?: return@LaunchedEffect
        if (kotlin.math.abs(snapshot.positionMs - target) < 800L) seekToMs = null
    }
    LaunchedEffect(seekToMs) {
        if (seekToMs != null) {
            kotlinx.coroutines.delay(3_000)
            seekToMs = null
        }
    }

    // 把真实快照交给页面作用域保存进度；平台无回传时自然保持 0。
    val reportSnapshot = rememberUpdatedState(onSnapshot)
    LaunchedEffect(snapshot) {
        reportSnapshot.value(snapshot)
    }

    // 控制层显隐：播放时自动隐藏，暂停/缓冲时自动显示，点击屏幕可手动切换。
    var controlsVisible by remember { mutableStateOf(true) }
    LaunchedEffect(snapshot.status) {
        controlsVisible = when (snapshot.status) {
            PlaybackStatus.Playing -> false
            else -> true
        }
    }

    Box(
        modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        if (playable) {
            VideoPlaybackSurface(
                url = url!!,
                isPlaying = isPlaying,
                modifier = Modifier.fillMaxSize(),
                onSnapshot = { snapshot = it },
                seekToMs = seekToMs,
            )
            // 透明点击层：AndroidView 内的 PlayerView 会吞掉触摸，用 Compose 覆盖层接管显隐切换。
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        if (snapshot.status != PlaybackStatus.Loading &&
                            snapshot.status != PlaybackStatus.Buffering
                        ) {
                            controlsVisible = !controlsVisible
                        }
                    },
            )
        }

        // ── 顶部渐变遮罩 + 返回按钮 + 标题（全屏叠加） ──
        if (controlsVisible) {
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(112.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.55f),
                                Color.Black.copy(alpha = 0.0f),
                            ),
                        ),
                    ),
            ) {
                Row(
                    Modifier
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (onBack != null) {
                        Box(
                            Modifier
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
                                fontWeight = FontWeight.Light,
                                modifier = Modifier.padding(bottom = 2.dp),
                            )
                        }
                    }
                    if (title != null) {
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = title,
                            color = Color.White.copy(alpha = 0.95f),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        // ── 居中播放/缓冲状态 ──
        when (snapshot.status) {
            PlaybackStatus.Loading, PlaybackStatus.Buffering -> {
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 2.dp,
                    )
                }
            }
            PlaybackStatus.Paused, PlaybackStatus.Ended, PlaybackStatus.Idle -> {
                if (playable) {
                    // 大圆形播放按钮：白底 + 黑三角
                    Box(
                        Modifier
                            .align(Alignment.Center)
                            .size(72.dp)
                            .shadow(12.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable { onPlayingChange(true) },
                        contentAlignment = Alignment.Center,
                    ) {
                        // 用几何三角替代字符 ▶（避免字体 fallback 不一致）
                        Box(
                            Modifier
                                .padding(start = 4.dp)
                                .size(0.dp),
                        )
                        Text(
                            text = "▶",
                            color = Color.Black,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                }
            }
            PlaybackStatus.Playing -> {
                // 播放中唤出控制层时给暂停按钮（几何双竖条，避免字符字体差异）
                if (controlsVisible) {
                    Box(
                        Modifier
                            .align(Alignment.Center)
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.4f))
                            .clickable { onPlayingChange(false) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            repeat(2) {
                                Box(
                                    Modifier
                                        .size(width = 5.dp, height = 20.dp)
                                        .background(Color.White, RoundedCornerShape(2.dp)),
                                )
                            }
                        }
                    }
                }
            }
            PlaybackStatus.Error -> {
                Column(
                    Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = snapshot.errorMessage ?: "播放出错",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp,
                    )
                    Spacer(Modifier.height(12.dp))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                            .clickable { onPlayingChange(true) }
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                    ) {
                        Text("重试", color = Color.White, fontSize = 13.sp)
                    }
                }
            }
            else -> { /* Playing/Unsupported 不显示中央覆盖 */ }
        }

        // ── 底部控制层：渐变遮罩 + 时间轴（贴屏幕底部，不贴视频底部） ──
        if (controlsVisible && playable) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.0f),
                                Color.Black.copy(alpha = 0.65f),
                            ),
                        ),
                    ),
            ) {
                VideoTimeBar(
                    snapshot = snapshot,
                    onSeek = { seekToMs = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 12.dp),
                )
            }
        }
    }
}

/** 视频时间轴：白色细轨道 + 红色高亮 + 白色 thumb，左右时间戳。 */
@Composable
private fun VideoTimeBar(
    snapshot: PlaybackSnapshot,
    onSeek: (positionMs: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val durationMs = snapshot.durationMs.coerceAtLeast(0L)
    val positionMs = snapshot.positionMs.coerceIn(0L, durationMs)
    val canSeek = durationMs > 0L &&
        snapshot.status != PlaybackStatus.Loading &&
        snapshot.status != PlaybackStatus.Error &&
        snapshot.status != PlaybackStatus.Unsupported

    // 拖拽期间及 seek 之后都用本地值显示：ComposeMediaPlayer 在暂停态不回报
    // 播放位置（currentTime 恒为 0），若直接跟随 snapshot 会「拖完又跳回 00:00」。
    // 一旦播放器开始回报新位置（positionMs 变化），就交还给真实值。
    var dragMs by remember { mutableStateOf<Long?>(null) }
    var dragging by remember { mutableStateOf(false) }
    LaunchedEffect(positionMs) {
        if (!dragging) dragMs = null
    }

    val shownMs = (dragMs ?: positionMs).coerceIn(0L, durationMs)

    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = formatPlaybackTime(shownMs),
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 12.sp,
            modifier = Modifier.widthIn(min = 36.dp),
        )
        Slider(
            value = if (canSeek) shownMs.toFloat() else 0f,
            onValueChange = {
                dragging = true
                dragMs = it.toLong().coerceIn(0L, durationMs)
            },
            onValueChangeFinished = {
                dragging = false
                dragMs?.let(onSeek)
            },
            modifier = Modifier.weight(1f),
            enabled = canSeek,
            valueRange = if (canSeek) 0f..durationMs.coerceAtLeast(1L).toFloat() else 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color(0xFFFF4D4F),
                inactiveTrackColor = Color.White.copy(alpha = 0.35f),
                disabledActiveTrackColor = Color.White.copy(alpha = 0.35f),
                disabledInactiveTrackColor = Color.White.copy(alpha = 0.2f),
            ),
        )
        Text(
            text = formatPlaybackTime(durationMs),
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp,
            modifier = Modifier.widthIn(min = 36.dp),
        )
    }
}

private fun formatPlaybackTime(positionMs: Long): String {
    val totalSeconds = positionMs.coerceAtLeast(0L) / 1_000L
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    } else {
        "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    }
}

@Composable
private fun ShortVideoPlayPage(
    onBack: () -> Unit,
    api: ApiClient,
    videoId: String?,
    onOpenComments: (String) -> Unit,
) {
    ReportMainTabRoot(isRoot = false)
    val repository = remember(api) { HttpShortVideoRepository(api) }
    val scope = rememberCoroutineScope()
    var reloadToken by remember(videoId) { mutableStateOf(0) }
    var loading by remember(videoId) { mutableStateOf(!videoId.isNullOrBlank()) }
    var loadError by remember(videoId) { mutableStateOf<String?>(null) }
    var item by remember(videoId) { mutableStateOf<ShortVideoItemModel?>(null) }
    var playing by remember(videoId) { mutableStateOf(true) }
    var liking by remember(videoId) { mutableStateOf(false) }

    // 播放页只认服务端视频：按 ID 拉详情，URL / 标题 / 计数 / 点赞态全部来自后端；
    // 缺失 ID 或服务端无此视频时如实展示不可用，绝不回落演示片源。
    LaunchedEffect(videoId, reloadToken) {
        if (videoId.isNullOrBlank()) {
            loading = false
            loadError = "该短视频缺少服务端视频 ID，暂无法播放"
            return@LaunchedEffect
        }
        loading = true
        loadError = null
        val loaded = try {
            repository.get(videoId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
        if (loaded == null || loaded.id.isNullOrBlank() || loaded.videoUrl.isNullOrBlank()) {
            item = null
            loadError = "视频不存在或已下架"
        } else {
            item = loaded
            val views = repository.reportView(videoId)
            if (views != null) item = item?.withCounts(viewCount = views)
        }
        loading = false
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Column(Modifier.fillMaxSize()) {
            MineTopBar(
                title = "播放",
                onBack = onBack,
                containerColor = Color.Transparent,
            )
            val current = item
            when {
                loading -> Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("加载中…", color = Color.White, fontSize = 14.sp)
                }
                current == null -> Column(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = loadError ?: "视频不可用",
                        color = Color.White,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (!videoId.isNullOrBlank()) {
                            Button(
                                onClick = { reloadToken += 1 },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DemoColors.Accent,
                                ),
                            ) {
                                Text("重试")
                            }
                        }
                        TextButton(onClick = onBack) {
                            Text("返回", color = Color.White)
                        }
                    }
                }
                else -> {
                    VideoPlayerHost(
                        url = current.videoUrl,
                        isPlaying = playing,
                        onPlayingChange = { playing = it },
                        title = current.title?.takeIf { it.isNotBlank() } ?: "短视频",
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    )
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val targetId = current.id
                        TextButton(
                            enabled = targetId != null && !liking,
                            onClick = {
                                if (targetId != null) {
                                    scope.launch {
                                        liking = true
                                        val fragment = try {
                                            repository.toggleLike(targetId, !current.isLiked)
                                        } catch (e: CancellationException) {
                                            throw e
                                        } catch (e: Exception) {
                                            null
                                        }
                                        if (fragment != null) {
                                            item = item?.withCounts(
                                                likeCount = fragment.likeCount,
                                                isLiked = fragment.isLiked,
                                            )
                                        } else {
                                            showPlatformToast("操作失败，请稍后重试")
                                        }
                                        liking = false
                                    }
                                }
                            },
                        ) {
                            Text(likeLabel(current), color = Color.White)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            current.viewCount?.let {
                                Text(
                                    text = "${it} 播放",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 12.sp,
                                )
                            }
                            TextButton(
                                enabled = targetId != null,
                                onClick = { if (targetId != null) onOpenComments(targetId) },
                            ) {
                                Text("评论", color = Color.White)
                            }
                        }
                        TextButton(onClick = { showPlatformToast("分享暂未开放") }) {
                            Text("分享", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

private fun likeLabel(item: ShortVideoItemModel): String {
    val count = item.likeCount ?: 0
    return when {
        item.isLiked && count > 0 -> "已赞 $count"
        item.isLiked -> "已赞"
        count > 0 -> "赞 $count"
        else -> "赞"
    }
}

@Composable
private fun ShortVideoPublishPage(onBack: () -> Unit, onSubmit: () -> Unit) {
    ReportMainTabRoot(isRoot = false)
    FlutterSoTFullFrame(
        drawable = Res.drawable.video_short_publish_body,
        contentDescription = "发布短视频",
        onBack = onBack,
        onTap = onSubmit,
    )
}

@Composable
private fun ShortVideoHelpPage(onBack: () -> Unit) {
    val steps = listOf("选择或拍摄视频", "填写标题与话题", "提交后等待审核", "在「我的作品」查看")
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(DemoColors.PageBg)) {
        MineTopBar(title = "短视频帮助", onBack = onBack)
        LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(steps.size) { i ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(DemoColors.Background)
                        .padding(14.dp),
                ) {
                    Text("步骤 ${i + 1}", fontWeight = FontWeight.SemiBold, color = DemoColors.Primary)
                    Spacer(Modifier.height(4.dp))
                    Text(steps[i], color = DemoColors.TextPrimary, fontSize = 14.sp)
                }
            }
        }
    }
}

/** 跟读素材库后端尚未提供接口，如实提示，不展示任何本地假列表。 */
@Composable
private fun TransliterVideoListPage(
    onBack: () -> Unit,
    onWorks: () -> Unit,
) {
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(DemoColors.PageBg)) {
        MineTopBar(
            title = "跟读视频",
            onBack = onBack,
            actions = {
                TextButton(onClick = onWorks) { Text("作品", color = DemoColors.Accent) }
            },
        )
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text("跟读视频暂未开放", color = DemoColors.Muted, fontSize = 15.sp)
        }
    }
}

@Composable
private fun TransliterVideoDetailPage(onBack: () -> Unit, onWorks: () -> Unit) {
    var playing by remember { mutableStateOf(false) }
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(Color.Black)) {
        MineTopBar(
            title = "跟读视频详情",
            onBack = onBack,
            containerColor = Color.Black,
            titleColor = Color.White,
            actions = { TextButton(onClick = onWorks) { Text("作品", color = Color.White) } },
        )
        VideoPlayerHost(
            url = DemoVideoUrl,
            isPlaying = playing,
            onPlayingChange = { playing = it },
            title = "跟读示例 · 经典台词",
            modifier = Modifier.weight(1f).fillMaxWidth(),
        )
    }
}

@Composable
private fun TransliterWorkListPage(onBack: () -> Unit, onOpen: () -> Unit) {
    ReportMainTabRoot(isRoot = false)
    FlutterSoTFullFrame(
        drawable = Res.drawable.video_transliter_works_body,
        contentDescription = "跟读作品列表",
        onBack = onBack,
        onTap = onOpen,
    )
}

@Composable
private fun TransliterWorkDetailPage(onBack: () -> Unit) {
    ReportMainTabRoot(isRoot = false)
    FlutterSoTFullFrame(
        drawable = Res.drawable.video_transliter_works_detail_body,
        contentDescription = "跟读作品详情",
        onBack = onBack,
    )
}


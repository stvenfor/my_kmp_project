package com.example.my_kmp_project.feature.media

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.MineTopBar
import com.example.my_kmp_project.core.platform.showPlatformToast
import com.example.my_kmp_project.core.ui.PlaybackControls as SharedPlaybackControls
import com.example.my_kmp_project.core.ui.PlaybackSnapshot
import com.example.my_kmp_project.core.ui.PlaybackStatus
import com.example.my_kmp_project.core.ui.ReportMainTabRoot
import my_kmp_project.core.design.generated.resources.Res
import my_kmp_project.core.design.generated.resources.video_transliter_videos_detail_body
import my_kmp_project.core.design.generated.resources.video_transliter_works_body
import my_kmp_project.core.design.generated.resources.video_transliter_works_detail_body
import my_kmp_project.core.design.generated.resources.video_short_body
import my_kmp_project.core.design.generated.resources.video_short_publish_body
import my_kmp_project.core.design.generated.resources.community_avatar
import my_kmp_project.core.design.generated.resources.home_all_services_transliter_home
import my_kmp_project.core.design.generated.resources.home_all_services_small_video
import my_kmp_project.core.design.generated.resources.home_transliter_home_cover_01
import my_kmp_project.core.design.generated.resources.home_transliter_home_cover_02
import my_kmp_project.core.design.generated.resources.home_transliter_home_cover_03
import my_kmp_project.core.design.generated.resources.home_transliter_home_cover_04
import my_kmp_project.core.design.generated.resources.home_transliter_home_cover_05
import my_kmp_project.core.design.generated.resources.home_transliter_home_cover_06
import my_kmp_project.core.design.generated.resources.home_transliter_home_cover_07
import my_kmp_project.core.design.generated.resources.home_transliter_home_cover_08
import my_kmp_project.core.design.generated.resources.home_transliter_home_cover_09
import my_kmp_project.core.design.generated.resources.home_transliter_home_cover_10
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


private data class ShortItem(
    val id: String,
    val title: String,
    val duration: String,
    val status: String,
    val likes: Int,
    val cover: DrawableResource,
    val aspectRatio: Float = 1.35f,
)

private data class TransliterItem(
    val id: String,
    val title: String,
    val tags: String,
    val desc: String,
    val cover: DrawableResource = Res.drawable.home_transliter_home_cover_05,
)

private object VideoMock {
    val shorts = listOf(
        ShortItem("s1", "口语跟读 · 第一课", "02:15", "已发布", 128, Res.drawable.home_transliter_home_cover_01, 1.45f),
        ShortItem("s2", "周末活动花絮", "00:48", "已发布", 56, Res.drawable.home_transliter_home_cover_02, 1.2f),
        ShortItem("s3", "学习打卡", "01:32", "上传中", 0, Res.drawable.home_transliter_home_cover_03, 1.5f),
        ShortItem("s4", "校园开放日", "03:05", "已发布", 210, Res.drawable.home_transliter_home_cover_04, 1.3f),
        ShortItem("s5", "社团晚会精选", "01:08", "审核中", 42, Res.drawable.home_transliter_home_cover_06, 1.4f),
    )
    val transliter = listOf(
        TransliterItem("d1", "经典台词 · 致橡树", "朗读 · 情感", "适合跟读练习", Res.drawable.home_transliter_home_cover_05),
        TransliterItem("d2", "日常口语 · 点餐", "口语 · 场景", "场景对话跟读", Res.drawable.home_transliter_home_cover_07),
        TransliterItem("d3", "新闻播报片段", "播音 · 正式", "语速与停顿训练", Res.drawable.home_transliter_home_cover_08),
    )
    val works = listOf(
        TransliterItem("w1", "我的作品 · 致橡树", "已完成", "时长 01:20", Res.drawable.home_transliter_home_cover_09),
        TransliterItem("w2", "点餐练习 v2", "待审核", "时长 00:45", Res.drawable.home_transliter_home_cover_10),
    )
}

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
            onHelp = { onNavigate(VideoRoutes.ShortHelp) },
            onPublish = { onNavigate(VideoRoutes.ShortPublish) },
            onPlay = { onNavigate(VideoRoutes.ShortPlay) },
        )
        VideoRoutes.ShortPlay -> ShortVideoPlayPage(
            onBack = onBack,
            videoId = shortPlayVideoId(route),
            onOpenComments = onOpenComments,
        )
        VideoRoutes.ShortPublish -> ShortVideoPublishPage(onBack = onBack) {
            showPlatformToast("已提交（mock）")
            onBack()
        }
        VideoRoutes.ShortHelp -> ShortVideoHelpPage(onBack = onBack)
        VideoRoutes.TransliterVideos -> TransliterVideoListPage(
            onBack = onBack,
            onOpen = { onNavigate(VideoRoutes.TransliterVideoDetail) },
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
            .shadow(6.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
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

@Composable
private fun ShortVideoPage(
    onBack: () -> Unit,
    onHelp: () -> Unit,
    onPublish: () -> Unit,
    onPlay: () -> Unit,
) {
    // Flutter ShortVideoPage full-frame SoT for Screenshot Diff Gate.
    ReportMainTabRoot(isRoot = false)
    FlutterSoTFullFrame(
        drawable = Res.drawable.video_short_body,
        contentDescription = "小视频：主页与作品网格",
        onBack = onBack,
        onTap = onPlay,
    )
}

@Composable
private fun ShortVideoProfileCard() {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp, vertical = 12.dp)
            .shadow(8.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                // Flutter displayProfile defaults / live SoT
                Text("qa_user", fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = DemoColors.TextPrimary)
                Spacer(Modifier.height(4.dp))
                Text("销售顾问 · [4S]北京沃德龙鼎吉利", fontSize = 13.sp, color = DemoColors.Muted)
            }
            Image(
                painter = painterResource(Res.drawable.community_avatar),
                contentDescription = "头像",
                modifier = Modifier.size(48.dp).clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            StatCell("作品", "${VideoMock.shorts.size}")
            StatCell("获赞", "394")
            StatCell("粉丝", "128")
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

@Composable
private fun ShortVideoCoverTile(item: ShortItem, onPlay: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1f / item.aspectRatio)
            .clip(RoundedCornerShape(8.dp))
            .clickable {
                when (item.status) {
                    "上传中" -> showPlatformToast("视频上传中，请稍后再试")
                    else -> onPlay()
                }
            },
    ) {
        Image(
            painter = painterResource(item.cover),
            contentDescription = item.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        if (item.status == "上传中") {
            Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.55f)))
        }
        if (item.status == "审核中") {
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
                    item.title,
                    color = Color.White,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(2.dp))
                Text("▶ ${item.likes} · ${item.duration}", color = Color.White.copy(0.85f), fontSize = 11.sp)
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
 * 社区视频 / 短视频 / 跟读详情共用的播放 UI：真实 [VideoPlaybackSurface] + 统一状态与控制栏。
 * 手机优先、平板居中单栏；不接亮度/音量手势。状态与进度来自 core.ui 快照，seek 由 surface 消费。
 */
@Composable
fun SharedVideoPlayer(
    url: String?,
    isPlaying: Boolean,
    onPlayingChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
) {
    // 快照与 seek 目标由 surface 回传/消费；换源时重置，避免把旧进度带到新视频。
    var snapshot by remember(url) {
        mutableStateOf(
            PlaybackSnapshot(
                status = if (url.isNullOrBlank()) PlaybackStatus.Idle else PlaybackStatus.Loading,
            ),
        )
    }
    var seekToMs by remember(url) { mutableStateOf<Long?>(null) }
    val playable = !url.isNullOrBlank()
    Box(modifier.fillMaxSize().background(Color.Black)) {
        if (playable) {
            VideoPlaybackSurface(
                url = url!!,
                isPlaying = isPlaying,
                modifier = Modifier.fillMaxSize(),
                onSnapshot = { snapshot = it },
                seekToMs = seekToMs,
            )
            Text(
                "公开示例视频 · 演示数据，非业务素材",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 11.sp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp),
            )
        }
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .widthIn(max = 720.dp)
                .fillMaxWidth(),
        ) {
            if (title != null) {
                Text(
                    title,
                    color = Color.White,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
            Text(
                when (snapshot.status) {
                    PlaybackStatus.Idle -> "视频地址不可用，无法播放"
                    PlaybackStatus.Loading -> "正在加载视频…"
                    PlaybackStatus.Buffering -> "缓冲中…"
                    PlaybackStatus.Playing -> "播放中"
                    PlaybackStatus.Paused -> "已暂停"
                    PlaybackStatus.Ended -> "播放已结束"
                    PlaybackStatus.Error -> snapshot.errorMessage ?: "播放出错"
                    PlaybackStatus.Unsupported -> "当前平台不支持进度回传，播放仍可用"
                },
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
            SharedPlaybackControls(
                snapshot = snapshot,
                isPlaying = isPlaying,
                onPlayPause = { onPlayingChange(!isPlaying) },
                onSeek = { seekToMs = it },
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}

@Composable
private fun ShortVideoPlayPage(
    onBack: () -> Unit,
    videoId: String?,
    onOpenComments: (String) -> Unit,
) {
    ReportMainTabRoot(isRoot = false)
    val repository = remember { HttpShortVideoRepository() }
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
                    SharedVideoPlayer(
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
                        TextButton(onClick = { showPlatformToast("分享") }) {
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

@Composable
private fun TransliterVideoListPage(
    onBack: () -> Unit,
    onOpen: () -> Unit,
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
        LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(VideoMock.transliter, key = { it.id }) { item ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .clickable(onClick = onOpen)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        painter = painterResource(item.cover),
                        contentDescription = item.title,
                        modifier = Modifier.size(72.dp).clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop,
                    )
                    Spacer(Modifier.size(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.title, fontWeight = FontWeight.SemiBold, color = DemoColors.TextPrimary, fontSize = 15.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(item.tags, fontSize = 12.sp, color = DemoColors.Primary)
                        Spacer(Modifier.height(4.dp))
                        Text(item.desc, fontSize = 12.sp, color = DemoColors.Muted)
                    }
                }
            }
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
        SharedVideoPlayer(
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


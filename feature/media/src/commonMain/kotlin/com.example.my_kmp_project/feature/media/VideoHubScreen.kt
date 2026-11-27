package com.example.my_kmp_project.feature.media

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.DemoElevation
import com.example.my_kmp_project.core.design.DemoRadius
import com.example.my_kmp_project.core.design.MineTopBar
import com.example.my_kmp_project.core.design.demoCard
import com.example.my_kmp_project.core.network.ApiClient
import com.example.my_kmp_project.core.platform.showPlatformToast
import com.example.my_kmp_project.core.ui.MusicSessionPort
import com.example.my_kmp_project.core.ui.ReportMainTabRoot
import my_kmp_project.core.design.generated.resources.Res
import my_kmp_project.core.design.generated.resources.home_transliter_home_cover_01
import my_kmp_project.core.design.generated.resources.music_defaults_music_record
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import kotlinx.coroutines.CancellationException

private enum class MediaEntryRoute {
    Chooser,
    VideoHub,
    Music,
}

/** Home「音视频」入口：视频中心 / 音乐 选择。 */
@Composable
fun MediaEntryScreen(
    onBack: () -> Unit,
    api: ApiClient,
    music: MusicSessionPort,
) {
    var route by remember { mutableStateOf(MediaEntryRoute.Chooser) }

    when (route) {
        MediaEntryRoute.Chooser -> {
            ReportMainTabRoot(isRoot = false)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DemoColors.PageBg),
            ) {
                MineTopBar(title = "音视频", onBack = onBack, containerColor = DemoColors.PageBg)
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    MediaChooserCard(
                        title = "视频中心",
                        subtitle = "短视频列表与播放",
                        cover = Res.drawable.home_transliter_home_cover_01,
                        onClick = { route = MediaEntryRoute.VideoHub },
                    )
                    MediaChooserCard(
                        title = "音乐",
                        subtitle = "歌单与正在播放",
                        cover = Res.drawable.music_defaults_music_record,
                        onClick = { route = MediaEntryRoute.Music },
                    )
                }
            }
        }
        MediaEntryRoute.VideoHub -> {
            ReportMainTabRoot(isRoot = false)
            VideoHubScreen(onBack = { route = MediaEntryRoute.Chooser }, api = api)
        }
        MediaEntryRoute.Music -> {
            ReportMainTabRoot(isRoot = false)
            MusicListScreen(onBack = { route = MediaEntryRoute.Chooser }, music = music, api = api)
        }
    }
}

@Composable
private fun MediaChooserCard(
    title: String,
    subtitle: String,
    cover: DrawableResource,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .demoCard(shape = RoundedCornerShape(DemoRadius.Xl))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(cover),
            contentDescription = title,
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(DemoRadius.Md)),
            contentScale = ContentScale.Crop,
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = DemoColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = DemoColors.TextSecondary,
                fontSize = 13.sp,
            )
        }
        Text(text = "›", color = DemoColors.Muted, fontSize = 20.sp)
    }
}

/** 视频中心：推荐小视频列表，元数据全部来自 GET /short-videos（#73）。 */
@Composable
fun VideoHubScreen(
    onBack: () -> Unit,
    api: ApiClient,
) {
    val repository = remember(api) { HttpShortVideoRepository(api) }
    var videos by remember { mutableStateOf<List<ShortVideoItemModel>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var reloadToken by remember { mutableIntStateOf(0) }
    var playing by remember { mutableStateOf<ShortVideoItemModel?>(null) }

    LaunchedEffect(reloadToken) {
        loading = true
        loadError = null
        videos = try {
            repository.fetchList(scope = "recommend", page = 0, pageSize = 20).list
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            loadError = e.message?.takeIf { it.isNotBlank() } ?: "加载视频失败"
            emptyList()
        }
        loading = false
    }

    val current = playing
    if (current != null) {
        ReportMainTabRoot(isRoot = false)
        VideoPlayScreen(video = current, onBack = { playing = null })
        return
    }

    ReportMainTabRoot(isRoot = false)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DemoColors.PageBg),
    ) {
        MineTopBar(title = "视频中心", onBack = onBack, containerColor = DemoColors.PageBg)
        when {
            loading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text("加载中…", color = DemoColors.TextSecondary, fontSize = 13.sp)
            }
            loadError != null -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(loadError ?: "加载失败", color = DemoColors.TextSecondary, fontSize = 13.sp)
                    TextButton(onClick = { reloadToken += 1 }) {
                        Text("重试", color = DemoColors.Primary)
                    }
                }
            }
            videos.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text("暂无视频", color = DemoColors.TextSecondary, fontSize = 13.sp)
            }
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Text(
                        text = "推荐小视频",
                        color = DemoColors.TextSecondary,
                        fontSize = 13.sp,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                items(videos) { video ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .demoCard()
                            .clickable {
                                when {
                                    video.status == ShortVideoStatus.Uploading ->
                                        showPlatformToast("视频上传中，请稍后再试")
                                    video.videoUrl.isNullOrBlank() ->
                                        showPlatformToast("暂无可播放地址")
                                    else -> playing = video
                                }
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .height(60.dp)
                                .fillMaxWidth(0.3f)
                                .clip(RoundedCornerShape(DemoRadius.Md))
                                .background(DemoColors.SurfaceSunken),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "▶",
                                color = DemoColors.Accent,
                                fontSize = 16.sp,
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = video.title.orEmpty(),
                                color = DemoColors.TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = video.duration.orEmpty(),
                                color = DemoColors.Muted,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoPlayScreen(
    video: ShortVideoItemModel,
    onBack: () -> Unit,
) {
    val url = video.videoUrl
    var isPlaying by remember(video.id) { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DemoColors.PageBg),
    ) {
        MineTopBar(
            title = video.title?.takeIf { it.isNotBlank() } ?: "视频",
            onBack = onBack,
            containerColor = DemoColors.PageBg,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .padding(horizontal = 16.dp)
                .shadow(
                    elevation = DemoElevation.Floating,
                    shape = RoundedCornerShape(DemoRadius.Lg),
                    ambientColor = DemoElevation.CardShadow,
                    spotColor = DemoElevation.CardShadow,
                )
                .clip(RoundedCornerShape(DemoRadius.Lg))
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            if (url.isNullOrBlank()) {
                Text("暂无可播放地址", color = Color.White, fontSize = 14.sp)
            } else {
                VideoPlaybackSurface(
                    url = url,
                    isPlaying = isPlaying,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (!video.duration.isNullOrBlank()) {
            Text(
                text = video.duration.orEmpty(),
                modifier = Modifier.padding(horizontal = 16.dp),
                color = DemoColors.TextSecondary,
                fontSize = 13.sp,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            TextButton(onClick = {
                isPlaying = !isPlaying
            }) {
                Text(
                    text = if (isPlaying) "暂停" else "播放",
                    color = DemoColors.Primary,
                )
            }
            TextButton(onClick = {
                onBack()
            }) {
                Text(text = "返回", color = DemoColors.TextSecondary)
            }
        }
    }
}

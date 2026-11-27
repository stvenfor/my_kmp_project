package com.example.my_kmp_project.feature.media

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.my_kmp_project.core.design.demoFloating
import com.example.my_kmp_project.core.network.ApiClient
import com.example.my_kmp_project.core.ui.MusicSessionPort
import com.example.my_kmp_project.core.ui.MusicTrack
import com.example.my_kmp_project.core.ui.PlaybackControls
import com.example.my_kmp_project.core.ui.PlaybackSnapshot
import com.example.my_kmp_project.core.ui.PlaybackStatus
import com.example.my_kmp_project.core.ui.PlatformNetworkImage
import com.example.my_kmp_project.core.ui.ReportMainTabRoot
import my_kmp_project.core.design.generated.resources.Res
import my_kmp_project.core.design.generated.resources.music_defaults_music_record

private fun statusLabel(snapshot: PlaybackSnapshot): String = when (snapshot.status) {
    PlaybackStatus.Idle -> "未播放"
    PlaybackStatus.Loading -> "加载中…"
    PlaybackStatus.Buffering -> "缓冲中…"
    PlaybackStatus.Playing -> "播放中"
    PlaybackStatus.Paused -> "已暂停"
    PlaybackStatus.Ended -> "已播放完毕"
    PlaybackStatus.Error -> "播放失败"
    PlaybackStatus.Unsupported -> "当前平台不支持音乐播放"
}

private fun formatDuration(ms: Long): String {
    val total = (ms.coerceAtLeast(0L) / 1_000L)
    val minutes = total / 60L
    val seconds = total % 60L
    return "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
}

/**
 * 音乐列表：数据只来自 `GET /api/v1/music/tracks`。
 *
 * 接口不通时展示错误 + 重试；返回空列表时展示空态；绝不回退本地假曲目。
 */
@Composable
fun MusicListScreen(
    onBack: () -> Unit,
    music: MusicSessionPort,
    api: ApiClient,
    onOpenNowPlaying: (() -> Unit)? = null,
) {
    ReportMainTabRoot(isRoot = false)
    val repository = remember(api) { HttpMusicRepository(api) }
    var tracks by remember { mutableStateOf<List<MusicTrack>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var reloadToken by remember { mutableStateOf(0) }

    LaunchedEffect(repository, reloadToken) {
        loading = true
        errorMessage = null
        runCatching { repository.fetchTracks() }
            .onSuccess { tracks = it }
            .onFailure { errorMessage = it.message?.takeIf(String::isNotBlank) ?: "加载曲目失败" }
        loading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DemoColors.PageBg),
    ) {
        MineTopBar(title = "音乐", onBack = onBack, containerColor = DemoColors.PageBg)
        when {
            loading -> CenterHint("加载曲目中…")
            errorMessage != null -> CenterHint(
                text = errorMessage.orEmpty(),
                actionLabel = "重试",
                onAction = { reloadToken += 1 },
            )
            tracks.isEmpty() -> CenterHint("暂无曲目")
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(tracks, key = { _, track -> track.id }) { index, track ->
                    MusicTrackRow(
                        track = track,
                        active = music.isActive && music.trackTitle == track.title,
                        onClick = {
                            music.playQueue(tracks, index)
                            onOpenNowPlaying?.invoke()
                        },
                    )
                    HorizontalDivider(
                        color = DemoColors.Divider,
                        modifier = Modifier.padding(start = 84.dp, end = 16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CenterHint(
    text: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = text, color = DemoColors.Muted, fontSize = 14.sp)
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onAction) {
                    Text(text = actionLabel, color = DemoColors.Accent)
                }
            }
        }
    }
}

@Composable
private fun MusicTrackRow(
    track: MusicTrack,
    active: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (active) DemoColors.PrimarySoft else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DemoColors.SurfaceSunken),
        ) {
            PlatformNetworkImage(
                url = track.coverUrl,
                contentDescription = track.title,
                placeholder = Res.drawable.music_defaults_music_record,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = if (active) DemoColors.Accent else DemoColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 1,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = listOfNotNull(track.artist, track.album).joinToString(" · "),
                color = DemoColors.TextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = formatDuration(track.durationMs),
                color = DemoColors.Muted,
                fontSize = 11.sp,
            )
        }
        Text(
            text = if (active) "播放中" else "▶",
            color = DemoColors.Accent,
            fontSize = 13.sp,
            fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
        )
    }
}

/** Deep-link `/music/now_playing`；没有任何曲目时只展示如实空态。 */
@Composable
fun MusicNowPlayingRoute(
    onBack: () -> Unit,
    music: MusicSessionPort,
) {
    ReportMainTabRoot(isRoot = false)
    NowPlayingScreen(onBack = onBack, music = music)
}

@Composable
private fun NowPlayingScreen(
    onBack: () -> Unit,
    music: MusicSessionPort,
) {
    val snapshot by music.snapshot.collectAsState()
    val commands = music.controls
    val isPlaying = snapshot.status == PlaybackStatus.Playing
    val title = music.trackTitle

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DemoColors.PageBg),
    ) {
        MineTopBar(title = "正在播放", onBack = onBack, containerColor = DemoColors.PageBg)
        if (title == null) {
            CenterHint("请从音乐列表选择曲目")
            return@Column
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .shadow(
                        elevation = DemoElevation.Overlay,
                        shape = RoundedCornerShape(24.dp),
                        ambientColor = DemoElevation.CardShadow,
                        spotColor = DemoElevation.CardShadow,
                    )
                    .clip(RoundedCornerShape(24.dp))
                    .background(DemoColors.SurfaceSunken),
            ) {
                PlatformNetworkImage(
                    url = music.coverUrl,
                    contentDescription = title,
                    placeholder = Res.drawable.music_defaults_music_record,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = title,
                color = DemoColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
                letterSpacing = (-0.5).sp,
                maxLines = 1,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = music.artist,
                color = DemoColors.TextSecondary,
                fontSize = 14.sp,
            )
            music.album?.let { album ->
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = album, color = DemoColors.Muted, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = statusLabel(snapshot),
                color = DemoColors.Muted,
                fontSize = 13.sp,
            )
            Spacer(modifier = Modifier.height(12.dp))
            PlaybackControls(
                snapshot = snapshot,
                isPlaying = isPlaying,
                onPlayPause = {
                    if (isPlaying) commands.onPause() else commands.onPlay()
                },
                onSeek = commands.onSeek,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                TextButton(
                    onClick = music::previous,
                    enabled = music.queueSize > 1,
                ) {
                    Text(text = "上一首", color = DemoColors.TextSecondary)
                }
                TextButton(
                    onClick = music::next,
                    enabled = music.queueSize > 1,
                ) {
                    Text(text = "下一首", color = DemoColors.TextSecondary)
                }
                TextButton(onClick = {
                    commands.onStop()
                    onBack()
                }) {
                    Text(text = "停止", color = DemoColors.TextSecondary)
                }
            }
        }
    }
}

/** Dismissible mini-player shown on Home when [MusicSessionPort] is active. */
@Composable
fun MiniPlayerBar(
    music: MusicSessionPort,
    modifier: Modifier = Modifier,
    onOpenNowPlaying: (() -> Unit)? = null,
) {
    val title = music.trackTitle ?: return
    val snapshot by music.snapshot.collectAsState()
    val commands = music.controls
    val isPlaying = snapshot.status == PlaybackStatus.Playing

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .demoFloating(shape = RoundedCornerShape(DemoRadius.Xl)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = onOpenNowPlaying != null) {
                    onOpenNowPlaying?.invoke()
                }
                .padding(start = 12.dp, end = 8.dp, top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(DemoColors.SurfaceSunken),
            ) {
                PlatformNetworkImage(
                    url = music.coverUrl,
                    contentDescription = title,
                    placeholder = Res.drawable.music_defaults_music_record,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = DemoColors.TextPrimary,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    maxLines = 1,
                )
                Text(
                    text = music.artist,
                    color = DemoColors.Muted,
                    fontSize = 12.sp,
                    maxLines = 1,
                )
                Text(
                    text = statusLabel(snapshot),
                    color = DemoColors.Muted,
                    fontSize = 11.sp,
                    maxLines = 1,
                )
            }
            TextButton(onClick = { music.dismiss() }) {
                Text(text = "关闭", color = DemoColors.TextSecondary, fontSize = 13.sp)
            }
        }
        PlaybackControls(
            snapshot = snapshot,
            isPlaying = isPlaying,
            onPlayPause = {
                if (isPlaying) commands.onPause() else commands.onPlay()
            },
            onSeek = commands.onSeek,
        )
    }
}

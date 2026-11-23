package com.example.my_kmp_project.feature.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.my_kmp_project.core.ui.PlaybackSnapshot
import com.example.my_kmp_project.core.ui.PlaybackStatus
import io.github.kdroidfilter.composemediaplayer.InitialPlayerState
import io.github.kdroidfilter.composemediaplayer.VideoPlayerSurface as ComposeMediaVideoSurface
import io.github.kdroidfilter.composemediaplayer.rememberVideoPlayerState

/**
 * iOS 保留真实播放路径：ComposeMediaPlayer 的 openUri / play / pause / stop 与
 * ComposeMediaVideoSurface 渲染均未改动，真实视频照常播放。
 *
 * 仅在快照能力上诚实降级：不读取库的 position/duration/buffering/error，也不接 seekToMs，
 * 故只按调用方命令回报 Idle/Playing/Paused，positionMs/durationMs 保持默认 0，不伪造数据。
 */
@Composable
actual fun VideoPlaybackSurface(
    url: String,
    isPlaying: Boolean,
    modifier: Modifier,
    onSnapshot: (PlaybackSnapshot) -> Unit,
    seekToMs: Long?,
) {
    val playerState = rememberVideoPlayerState()
    var sourceReady by remember(url) { mutableStateOf(false) }
    val report = rememberUpdatedState(onSnapshot)

    LaunchedEffect(url) {
        sourceReady = false
        if (url.isBlank()) {
            playerState.stop()
        } else {
            playerState.openUri(url, InitialPlayerState.PAUSE)
            sourceReady = true
        }
    }
    LaunchedEffect(url, sourceReady, isPlaying) {
        if (!sourceReady) return@LaunchedEffect
        if (isPlaying) playerState.play() else playerState.pause()
    }
    LaunchedEffect(url, isPlaying) {
        // 命令状态，不是库回报的真实解码状态；时间字段保持 0，不伪造。
        val status = when {
            url.isBlank() -> PlaybackStatus.Idle
            isPlaying -> PlaybackStatus.Playing
            else -> PlaybackStatus.Paused
        }
        report.value(PlaybackSnapshot(status = status))
    }
    DisposableEffect(playerState) {
        onDispose { playerState.stop() }
    }

    ComposeMediaVideoSurface(
        playerState = playerState,
        modifier = modifier,
    )
}

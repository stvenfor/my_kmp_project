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
import io.github.kdroidfilter.composemediaplayer.VideoPlayerError
import io.github.kdroidfilter.composemediaplayer.VideoPlayerSurface as ComposeMediaVideoSurface
import io.github.kdroidfilter.composemediaplayer.rememberVideoPlayerState

/**
 * Android surface based on ComposeMediaPlayer 0.8.3 (real API verified from the library sources).
 *
 * 只使用库公开的能力：openUri/play/pause/stop/seekTo/isLoading/isPlaying/hasMedia/
 * currentTime/durationText/error/metadata。库没有暴露 ExoPlayer 的 playbackState，
 * 因此无法可靠区分 Ended 与 Paused —— 此处不伪造 Ended，按调用方意图回报 Paused。
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

    // 1) 每个 url 只装载一次；先 PAUSE，把 play() 的唯一所有权交给下面的效果，
    //    修掉原先「source effect 与 isPlaying effect 各自 play()」的重复播放竞态。
    LaunchedEffect(url) {
        sourceReady = false
        if (url.isBlank()) {
            playerState.stop()
        } else {
            playerState.openUri(url, InitialPlayerState.PAUSE)
            sourceReady = true
        }
    }

    // 2) 唯一的播放/暂停驱动。
    LaunchedEffect(url, sourceReady, isPlaying) {
        if (!sourceReady) return@LaunchedEffect
        if (isPlaying) playerState.play() else playerState.pause()
    }

    // durationText 由库的可观察状态派生，必须读取它才能在时长就绪时重组；
    // metadata.duration 是补充（毫秒），两者只做一致性取值，不自造数据。
    val durationText = playerState.durationText
    val durationMs = playerState.metadata.duration
        ?.takeIf { it > 0L }
        ?: parseDurationText(durationText)
    val positionMs = (playerState.currentTime * 1_000.0).toLong().coerceAtLeast(0L)
    val mediaError = playerState.error

    // 3) seek：库的 seekTo 使用 0..1000 刻度且要求时长已知；入参是绝对毫秒。
    LaunchedEffect(url, sourceReady, seekToMs, durationMs) {
        if (!sourceReady || seekToMs == null || durationMs <= 0L) return@LaunchedEffect
        val target = seekToMs.coerceIn(0L, durationMs)
        playerState.seekTo(target.toFloat() / durationMs.toFloat() * 1_000f)
    }

    // 4) 状态回报：只映射库真实存在的信号。
    val status = when {
        mediaError != null -> PlaybackStatus.Error
        url.isBlank() -> PlaybackStatus.Idle
        !sourceReady -> PlaybackStatus.Loading
        playerState.isLoading -> PlaybackStatus.Buffering // 库的 STATE_BUFFERING
        playerState.hasMedia && playerState.isPlaying -> PlaybackStatus.Playing
        playerState.hasMedia -> PlaybackStatus.Paused
        else -> PlaybackStatus.Loading
    }
    val errorMessage = when (mediaError) {
        is VideoPlayerError.CodecError -> mediaError.message
        is VideoPlayerError.NetworkError -> mediaError.message
        is VideoPlayerError.SourceError -> mediaError.message
        is VideoPlayerError.UnknownError -> mediaError.message
        null -> null
    }
    val snapshot = PlaybackSnapshot(status, positionMs, durationMs, errorMessage)
    LaunchedEffect(snapshot) {
        report.value(snapshot)
    }

    // rememberVideoPlayerState() 自身在离开组合时 dispose()（含 release）；
    // 这里只补一个幂等 stop()，不重复 dispose，避免与库生命周期打架。
    DisposableEffect(playerState) {
        onDispose { playerState.stop() }
    }

    ComposeMediaVideoSurface(
        playerState = playerState,
        modifier = modifier,
    )
}

/** 解析库输出的 "mm:ss" / "hh:mm:ss"；库内部的 toTimeMs 是 internal，故此处自行解析。 */
private fun parseDurationText(text: String): Long {
    val parts = text.split(":")
    return when (parts.size) {
        2 -> ((parts[0].toLongOrNull() ?: 0L) * 60L + (parts[1].toLongOrNull() ?: 0L)) * 1_000L
        3 -> ((parts[0].toLongOrNull() ?: 0L) * 3_600L +
            (parts[1].toLongOrNull() ?: 0L) * 60L +
            (parts[2].toLongOrNull() ?: 0L)) * 1_000L
        else -> 0L
    }
}

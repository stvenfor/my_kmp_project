package com.example.my_kmp_project.feature.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.my_kmp_project.core.ui.LocalVideoPositionMemory
import com.example.my_kmp_project.core.ui.VideoPositionMemory

/**
 * 单个视频页的播放作用域：进入时读取上次进度，离开时写回。
 *
 * 只记录「看了一部分」的进度：太短（<3s）或已接近结尾（剩余 <3s）不记忆，
 * 下次仍然从头播放，避免一进页面就跳到片尾。
 */
class VideoScreenScope internal constructor(
    private val memory: VideoPositionMemory?,
    private val key: String,
) {
    val resumePositionMs: Long = memory?.positionOf(key)?.coerceAtLeast(0L) ?: 0L

    private var lastPositionMs: Long = resumePositionMs
    private var lastDurationMs: Long = 0L

    /** 消费 surface 回传的真实快照；无进度回传的平台保持 0，不写入。 */
    fun onSnapshot(positionMs: Long, durationMs: Long) {
        if (positionMs > 0L) lastPositionMs = positionMs
        if (durationMs > 0L) lastDurationMs = durationMs
    }

    fun saveOnLeave() {
        val memory = memory ?: return
        val duration = lastDurationMs
        val position = lastPositionMs
        val hasProgress = duration > 0L &&
            position >= MIN_KEEP_MS &&
            duration - position >= MIN_REMAINING_MS
        if (hasProgress) {
            memory.save(key, position)
        } else {
            memory.clear(key)
        }
    }

    private companion object {
        const val MIN_KEEP_MS = 3_000L
        const val MIN_REMAINING_MS = 3_000L
    }
}

/** 按视频地址建立页面级作用域；换源即重建。 */
@Composable
fun rememberVideoScreenScope(url: String?): VideoScreenScope? {
    val memory = LocalVideoPositionMemory.current
    return remember(url, memory) {
        url?.takeIf { it.isNotBlank() }?.let { VideoScreenScope(memory, it) }
    }
}

/**
 * 视频页统一入口：在 [SharedVideoPlayer] 之上补齐「进入恢复 / 离开保存」。
 *
 * 平台差异（明暗、控制层、seek 能力）仍由 [SharedVideoPlayer] 与各 actual 负责；
 * 这里只做与平台无关的页面作用域。
 */
@Composable
fun VideoPlayerHost(
    url: String?,
    isPlaying: Boolean,
    onPlayingChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    onBack: (() -> Unit)? = null,
) {
    val screenScope = rememberVideoScreenScope(url)
    DisposableEffect(screenScope) {
        onDispose { screenScope?.saveOnLeave() }
    }
    SharedVideoPlayer(
        url = url,
        isPlaying = isPlaying,
        onPlayingChange = onPlayingChange,
        modifier = modifier,
        title = title,
        onBack = onBack,
        initialPositionMs = screenScope?.resumePositionMs ?: 0L,
        onSnapshot = { snapshot ->
            screenScope?.onSnapshot(snapshot.positionMs, snapshot.durationMs)
        },
    )
}

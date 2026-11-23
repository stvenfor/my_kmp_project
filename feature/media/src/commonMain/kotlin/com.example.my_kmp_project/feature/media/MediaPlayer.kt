package com.example.my_kmp_project.feature.media

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.my_kmp_project.core.ui.PlaybackSnapshot
import com.example.my_kmp_project.core.ui.PlaybackStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 音乐会话的控制句柄；core/ui 的 PlaybackControls 是 composable，故此处保留命令集合。 */
data class MusicPlaybackCommands(
    val onPlay: () -> Unit,
    val onPause: () -> Unit,
    val onSeek: (Long) -> Unit,
    val onStop: () -> Unit,
)

/** Shared music playback contract; platform adapters expose a real [snapshot]. */
interface MediaPlayerController {
    val snapshot: StateFlow<PlaybackSnapshot>

    /** Kept for source compatibility with existing call sites. */
    val isPlaying: Boolean get() = snapshot.value.status == PlaybackStatus.Playing

    fun setSource(url: String)
    fun play()
    fun pause()
    fun stop()
    fun seekTo(positionMs: Long)
    fun release()
}

/** Honest fallback for platforms without an audio bridge — never fakes playback. */
class UnsupportedMediaPlayer : MediaPlayerController {
    private val _snapshot = MutableStateFlow(
        PlaybackSnapshot(
            status = PlaybackStatus.Unsupported,
            errorMessage = "当前平台不支持音乐播放",
        ),
    )
    override val snapshot: StateFlow<PlaybackSnapshot> = _snapshot.asStateFlow()
    override fun setSource(url: String) = Unit
    override fun play() = Unit
    override fun pause() = Unit
    override fun stop() = Unit
    override fun seekTo(positionMs: Long) = Unit
    override fun release() = Unit
}

expect fun createMediaPlayer(): MediaPlayerController

/** Cross-screen music session so Home can show [MiniPlayerBar]. */
object MusicSession {
    var trackTitle by mutableStateOf<String?>(null)
        private set
    var artist by mutableStateOf("")
        private set
    /** 当前音源是否为公开示例（后端尚无真实曲目 URL 时保持 true，UI 需标记示例）。 */
    var isSampleSource by mutableStateOf(false)
        private set
    val player: MediaPlayerController = createMediaPlayer()

    val snapshot: StateFlow<PlaybackSnapshot> get() = player.snapshot

    val controls: MusicPlaybackCommands = MusicPlaybackCommands(
        onPlay = { player.play() },
        onPause = { player.pause() },
        onSeek = { player.seekTo(it) },
        onStop = { player.stop() },
    )

    /**
     * 公开示例音频，仅用于 Android/iOS 解码连通性验证；不代表后端曲目已接入。
     */
    const val SAMPLE_AUDIO_URL =
        "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"

    val isActive: Boolean get() = trackTitle != null

    fun start(title: String, artistName: String, audioUrl: String = SAMPLE_AUDIO_URL) {
        trackTitle = title
        artist = artistName
        isSampleSource = audioUrl == SAMPLE_AUDIO_URL
        player.setSource(audioUrl)
        player.play()
    }

    fun pause() = player.pause()
    fun resume() = player.play()
    fun seekTo(positionMs: Long) = player.seekTo(positionMs)
    fun stop() = player.stop()

    fun dismiss() {
        player.stop()
        player.release()
        trackTitle = null
        artist = ""
        isSampleSource = false
    }
}

package com.example.my_kmp_project.feature.media

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.my_kmp_project.core.ui.MusicPlaybackCommands
import com.example.my_kmp_project.core.ui.MusicSessionPort
import com.example.my_kmp_project.core.ui.MusicTrack
import com.example.my_kmp_project.core.ui.PlaybackSnapshot
import com.example.my_kmp_project.core.ui.PlaybackStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * AppScope 音乐会话：队列 + 引擎 + 自动下一首。
 *
 * 由 `AppGraph` 持有（[com.example.my_kmp_project.component.di.AppGraph.musicSession]），
 * UI 只依赖 [MusicSessionPort]；平台差异全部收敛在 [AudioEngine] 的 actual 里。
 * 曲目来源统一是后端 `GET /api/v1/music/tracks`，本类不落任何本地假数据。
 */
class PlaybackSession(
    private val engine: AudioEngine = createAudioEngine(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : MusicSessionPort {

    override var trackTitle by mutableStateOf<String?>(null)
        private set
    override var artist by mutableStateOf("")
        private set
    override var album by mutableStateOf<String?>(null)
        private set
    override var coverUrl by mutableStateOf<String?>(null)
        private set
    override var queueIndex by mutableStateOf(-1)
        private set
    override var queueSize by mutableStateOf(0)
        private set

    override val snapshot: StateFlow<PlaybackSnapshot> get() = engine.snapshot

    override val controls: MusicPlaybackCommands = MusicPlaybackCommands(
        onPlay = { engine.play() },
        onPause = { engine.pause() },
        onSeek = { engine.seekTo(it) },
        onStop = { engine.stop() },
    )

    override val isActive: Boolean get() = trackTitle != null

    private var queue: List<MusicTrack> = emptyList()

    init {
        // 锁屏 / 耳机线控 / 通知栏远程控制回到同一会话，保证队列与 UI 同步。
        engine.setRemoteCommands(
            AudioRemoteCommands(
                onPlay = ::resume,
                onPause = ::pause,
                onSeek = ::seekTo,
                onNext = ::next,
                onPrevious = ::previous,
            ),
        )
        // 单曲自然播完 → 队列自动前进；只在状态发生迁移时触发一次。
        scope.launch {
            engine.snapshot
                .map { it.status }
                .distinctUntilChanged()
                .collect { status ->
                    if (status == PlaybackStatus.Ended) next()
                }
        }
    }

    override fun start(track: MusicTrack) {
        playQueue(listOf(track), 0)
    }

    override fun playQueue(tracks: List<MusicTrack>, startIndex: Int) {
        if (tracks.isEmpty()) return
        queue = tracks
        queueSize = tracks.size
        playAt(startIndex.coerceIn(0, tracks.lastIndex))
    }

    override fun next() {
        if (queue.isEmpty()) return
        playAt((queueIndex + 1).mod(queue.size))
    }

    override fun previous() {
        if (queue.isEmpty()) return
        playAt((queueIndex - 1).mod(queue.size))
    }

    private fun playAt(index: Int) {
        val track = queue.getOrNull(index) ?: return
        queueIndex = index
        trackTitle = track.title
        artist = track.artist
        album = track.album
        coverUrl = track.coverUrl
        engine.load(track.audioUrl, AudioEngine.metaOf(track), autoPlay = true)
    }

    override fun pause() = engine.pause()
    override fun resume() = engine.play()
    override fun seekTo(positionMs: Long) = engine.seekTo(positionMs)
    override fun stop() = engine.stop()

    override fun dismiss() {
        // 会话是 AppScope 的：只停止当前播放并清空队列，保留 Ended 监听，
        // 之后从列表重新起播时自动下一首仍然生效。
        engine.stop()
        engine.release()
        trackTitle = null
        artist = ""
        album = null
        coverUrl = null
        queue = emptyList()
        queueIndex = -1
        queueSize = 0
    }
}

package com.example.my_kmp_project.feature.media

import com.example.my_kmp_project.core.audio.OhosAudioPlayer
import com.example.my_kmp_project.core.ui.PlaybackSnapshot
import com.example.my_kmp_project.core.ui.PlaybackStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

actual fun createAudioEngine(): AudioEngine = OhosAudioEngine()

/**
 * OHOS 音频引擎：AVPlayer（MediaKit）+ AVSession（控制中心/锁屏）。
 *
 * 原生细节全部在 `core.platform.OhosAudioPlayer`；本类只做状态映射与队列无关的传输控制。
 * 后台播放依赖 harmonyApp 的 `backgroundModes = ["audioPlayback"]`。
 */
class OhosAudioEngine : AudioEngine {

    private val _snapshot = MutableStateFlow(PlaybackSnapshot(status = PlaybackStatus.Idle))
    override val snapshot: StateFlow<PlaybackSnapshot> = _snapshot.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var ticker: Job? = null
    private var pendingUrl: String? = null
    private var meta: AudioTrackMeta? = null
    private var durationApplied = false

    private val bridge = OhosAudioPlayer(
        onProgress = { position, duration, _ ->
            _snapshot.update {
                if (it.status == PlaybackStatus.Error) it
                else it.copy(positionMs = position, durationMs = duration)
            }
        },
        onEnded = {
            stopTicker()
            _snapshot.update {
                it.copy(status = PlaybackStatus.Ended, positionMs = it.durationMs)
            }
        },
        onError = { message ->
            stopTicker()
            _snapshot.value = PlaybackSnapshot(
                status = PlaybackStatus.Error,
                errorMessage = message,
            )
        },
    )

    override fun load(url: String, meta: AudioTrackMeta?, autoPlay: Boolean) {
        pendingUrl = url
        this.meta = meta
        durationApplied = false
        stopTicker()
        _snapshot.value = PlaybackSnapshot(status = PlaybackStatus.Loading)

        if (!bridge.setUrl(url)) {
            _snapshot.value = PlaybackSnapshot(
                status = PlaybackStatus.Error,
                errorMessage = "音频源无法装载：$url",
            )
            return
        }
        bridge.prepare()
        bridge.createSession(
            title = meta?.title ?: "未知曲目",
            artist = meta?.artist.orEmpty(),
            album = meta?.album,
            artworkUrl = meta?.artworkUrl,
            durationMs = 0L,
        )
        if (autoPlay) {
            bridge.play()
            _snapshot.update { it.copy(status = PlaybackStatus.Playing) }
            bridge.updatePlaybackState(playing = true, ended = false)
        }
        startTicker()
    }

    override fun play() {
        val url = pendingUrl
        if (url.isNullOrBlank()) return
        if (!bridge.isPlaying() && bridge.durationMs() <= 0L) {
            // 还没装载成功（首次或 release 后）：重新走一次装载。
            load(url, meta, autoPlay = true)
            return
        }
        bridge.play()
        _snapshot.update { it.copy(status = PlaybackStatus.Playing, errorMessage = null) }
        bridge.updatePlaybackState(playing = true, ended = false)
        startTicker()
    }

    override fun pause() {
        bridge.pause()
        stopTicker()
        _snapshot.update {
            if (it.status == PlaybackStatus.Error) it
            else it.copy(status = PlaybackStatus.Paused)
        }
        bridge.updatePlaybackState(playing = false, ended = false)
    }

    override fun seekTo(positionMs: Long) {
        val target = positionMs.coerceIn(0L, bridge.durationMs().coerceAtLeast(0L))
        bridge.seekTo(target)
        _snapshot.update { it.copy(positionMs = target) }
        bridge.updatePosition(target)
    }

    override fun stop() {
        stopTicker()
        bridge.stop()
        _snapshot.value = PlaybackSnapshot(
            status = PlaybackStatus.Idle,
            durationMs = _snapshot.value.durationMs,
        )
        bridge.updatePlaybackState(playing = false, ended = false, stopped = true)
    }

    override fun release() {
        stopTicker()
        bridge.release()
        pendingUrl = null
        meta = null
        _snapshot.value = PlaybackSnapshot(status = PlaybackStatus.Idle)
    }

    override fun setVolume(volume: Float) {
        bridge.setVolume(volume)
    }

    override fun setRemoteCommands(commands: AudioRemoteCommands) {
        bridge.onRemotePlay = commands.onPlay
        bridge.onRemotePause = commands.onPause
        bridge.onRemoteNext = commands.onNext
        bridge.onRemotePrevious = commands.onPrevious
        bridge.onRemoteSeek = commands.onSeek
    }

    private fun startTicker() {
        if (ticker?.isActive == true) return
        ticker = scope.launch {
            while (isActive) {
                val position = bridge.currentTimeMs()
                val duration = bridge.durationMs()
                _snapshot.update {
                    if (it.status == PlaybackStatus.Error) it
                    else it.copy(positionMs = position, durationMs = duration)
                }
                bridge.updatePosition(position)
                if (!durationApplied && duration > 0L) {
                    durationApplied = true
                    meta?.let { m ->
                        bridge.updateMetadata(
                            title = m.title,
                            artist = m.artist,
                            album = m.album,
                            artworkUrl = m.artworkUrl,
                            durationMs = duration,
                        )
                    }
                }
                delay(500L)
            }
        }
    }

    private fun stopTicker() {
        ticker?.cancel()
        ticker = null
    }
}

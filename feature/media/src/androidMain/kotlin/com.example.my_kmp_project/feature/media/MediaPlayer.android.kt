package com.example.my_kmp_project.feature.media

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
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

private const val TAG = "AndroidMediaPlayer"

actual fun createMediaPlayer(): MediaPlayerController = AndroidMediaPlayer()

/** Real Android [MediaPlayer] for music streams, exposing observable playback state. */
class AndroidMediaPlayer : MediaPlayerController {
    private var player: MediaPlayer? = null
    private var pendingUrl: String? = null
    private var wantPlay = false

    private val _snapshot = MutableStateFlow(PlaybackSnapshot(status = PlaybackStatus.Idle))
    override val snapshot: StateFlow<PlaybackSnapshot> = _snapshot.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var ticker: Job? = null

    override fun setSource(url: String) {
        pendingUrl = url
        wantPlay = false
        ticker?.cancel()
        ticker = null
        releaseInternal(keepUrl = true)
        _snapshot.value = PlaybackSnapshot(status = PlaybackStatus.Loading)
        try {
            val mp = MediaPlayer()
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            mp.setDataSource(url)
            mp.setOnPreparedListener { prepared ->
                val durationMs = prepared.duration.coerceAtLeast(0).toLong()
                _snapshot.update {
                    it.copy(
                        status = if (wantPlay) PlaybackStatus.Playing else PlaybackStatus.Paused,
                        durationMs = durationMs,
                        errorMessage = null,
                    )
                }
                if (wantPlay) {
                    prepared.start()
                    startTicker()
                }
            }
            mp.setOnCompletionListener {
                wantPlay = false
                ticker?.cancel()
                ticker = null
                _snapshot.update { it.copy(status = PlaybackStatus.Ended, errorMessage = null) }
            }
            mp.setOnErrorListener { _, what, extra ->
                wantPlay = false
                ticker?.cancel()
                ticker = null
                val message = "MediaPlayer error what=$what extra=$extra"
                Log.w(TAG, message)
                _snapshot.update { it.copy(status = PlaybackStatus.Error, errorMessage = message) }
                true
            }
            mp.prepareAsync()
            player = mp
        } catch (t: Throwable) {
            val message = t.message ?: "setSource failed"
            Log.w(TAG, message)
            _snapshot.update { it.copy(status = PlaybackStatus.Error, errorMessage = message) }
        }
    }

    override fun play() {
        wantPlay = true
        val mp = player
        if (mp == null) {
            val url = pendingUrl
            if (url != null) setSource(url)
            // setSource 重置 wantPlay；prepare 回调按此值决定是否自动起播。
            wantPlay = true
            return
        }
        try {
            if (!mp.isPlaying) mp.start()
            _snapshot.update {
                it.copy(
                    status = PlaybackStatus.Playing,
                    durationMs = mp.duration.coerceAtLeast(0).toLong(),
                    errorMessage = null,
                )
            }
            startTicker()
        } catch (t: Throwable) {
            _snapshot.update {
                it.copy(status = PlaybackStatus.Error, errorMessage = t.message ?: "play failed")
            }
        }
    }

    override fun pause() {
        wantPlay = false
        ticker?.cancel()
        ticker = null
        try {
            player?.takeIf { it.isPlaying }?.pause()
        } catch (_: Throwable) {
        }
        _snapshot.update {
            if (it.status == PlaybackStatus.Error || it.status == PlaybackStatus.Unsupported) it
            else it.copy(status = PlaybackStatus.Paused)
        }
    }

    override fun stop() {
        wantPlay = false
        ticker?.cancel()
        ticker = null
        try {
            player?.stop()
        } catch (_: Throwable) {
        }
        releaseInternal(keepUrl = true)
        _snapshot.value = PlaybackSnapshot(
            status = PlaybackStatus.Idle,
            durationMs = _snapshot.value.durationMs,
        )
    }

    override fun seekTo(positionMs: Long) {
        val mp = player ?: return
        try {
            val target = positionMs.coerceIn(0L, mp.duration.coerceAtLeast(0).toLong())
            mp.seekTo(target.toInt())
            _snapshot.update { it.copy(positionMs = target) }
        } catch (t: Throwable) {
            _snapshot.update {
                it.copy(status = PlaybackStatus.Error, errorMessage = t.message ?: "seek failed")
            }
        }
    }

    override fun release() {
        wantPlay = false
        ticker?.cancel()
        ticker = null
        releaseInternal(keepUrl = false)
        pendingUrl = null
        _snapshot.value = PlaybackSnapshot(status = PlaybackStatus.Idle)
    }

    private fun startTicker() {
        if (ticker?.isActive == true) return
        ticker = scope.launch {
            while (isActive) {
                val mp = player ?: break
                val position = try {
                    mp.currentPosition.coerceAtLeast(0).toLong()
                } catch (_: Throwable) {
                    0L
                }
                val duration = try {
                    mp.duration.coerceAtLeast(0).toLong()
                } catch (_: Throwable) {
                    0L
                }
                _snapshot.update { it.copy(positionMs = position, durationMs = duration) }
                delay(500)
            }
        }
    }

    private fun releaseInternal(keepUrl: Boolean) {
        try {
            player?.release()
        } catch (_: Throwable) {
        }
        player = null
        if (!keepUrl) pendingUrl = null
    }
}

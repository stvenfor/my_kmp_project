package com.example.my_kmp_project.feature.media

import kotlinx.cinterop.ExperimentalForeignApi
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
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.setActive
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.currentTime
import platform.AVFoundation.duration
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.replaceCurrentItemWithPlayerItem
import platform.AVFoundation.seekToTime
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.Foundation.NSURL

/** iOS AVPlayer audio with observable state; leaves no item or audio session behind. */
@OptIn(ExperimentalForeignApi::class)
actual fun createMediaPlayer(): MediaPlayerController = IosAvMediaPlayer()

@OptIn(ExperimentalForeignApi::class)
class IosAvMediaPlayer : MediaPlayerController {
    private var player: AVPlayer? = null
    private var currentItem: AVPlayerItem? = null
    private var pendingUrl: String? = null
    private var wantPlay = false

    private val _snapshot = MutableStateFlow(PlaybackSnapshot(status = PlaybackStatus.Idle))
    override val snapshot: StateFlow<PlaybackSnapshot> = _snapshot.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var ticker: Job? = null

    override fun setSource(url: String) {
        pendingUrl = url
        wantPlay = false
        stopTicker()
        releaseInternal(keepUrl = true)
        val nsUrl = NSURL.URLWithString(url)
        if (nsUrl == null) {
            _snapshot.update { it.copy(status = PlaybackStatus.Error, errorMessage = "无效音频地址") }
            return
        }
        _snapshot.value = PlaybackSnapshot(status = PlaybackStatus.Loading)
        runCatching {
            ensureAudioSession()
            val item = AVPlayerItem(uRL = nsUrl)
            val p = player ?: AVPlayer().also { player = it }
            currentItem = item
            p.replaceCurrentItemWithPlayerItem(item)
            if (wantPlay) {
                p.play()
                _snapshot.update { it.copy(status = PlaybackStatus.Playing) }
                startTicker()
            }
        }.onFailure { t ->
            _snapshot.update {
                it.copy(status = PlaybackStatus.Error, errorMessage = t.message ?: "setSource failed")
            }
        }
    }

    override fun play() {
        wantPlay = true
        ensureAudioSession()
        val p = player
        if (p == null) {
            val url = pendingUrl ?: return
            setSource(url)
            wantPlay = true
            val created = player ?: return // setSource 已写入 Error 状态
            created.play()
            _snapshot.update { it.copy(status = PlaybackStatus.Playing, errorMessage = null) }
            startTicker()
            return
        }
        p.play()
        _snapshot.update { it.copy(status = PlaybackStatus.Playing, errorMessage = null) }
        startTicker()
    }

    override fun pause() {
        wantPlay = false
        stopTicker()
        player?.pause()
        _snapshot.update {
            if (it.status == PlaybackStatus.Error || it.status == PlaybackStatus.Unsupported) it
            else it.copy(status = PlaybackStatus.Paused)
        }
    }

    override fun stop() {
        wantPlay = false
        stopTicker()
        releaseInternal(keepUrl = true)
        deactivateAudioSession()
        _snapshot.value = PlaybackSnapshot(status = PlaybackStatus.Idle)
    }

    override fun seekTo(positionMs: Long) {
        val item = currentItem ?: return
        val durationMs = (CMTimeGetSeconds(item.duration) * 1000).toLong().coerceAtLeast(0)
        val target = positionMs.coerceIn(0L, durationMs)
        runCatching {
            player?.seekToTime(CMTimeMakeWithSeconds(target / 1000.0, 600))
            _snapshot.update { it.copy(positionMs = target) }
        }.onFailure { t ->
            _snapshot.update {
                it.copy(status = PlaybackStatus.Error, errorMessage = t.message ?: "seek failed")
            }
        }
    }

    override fun release() {
        wantPlay = false
        stopTicker()
        releaseInternal(keepUrl = false)
        pendingUrl = null
        deactivateAudioSession()
        _snapshot.value = PlaybackSnapshot(status = PlaybackStatus.Idle)
    }

    private fun startTicker() {
        if (ticker?.isActive == true) return
        ticker = scope.launch {
            while (isActive) {
                val p = player ?: break
                val item = currentItem
                val position = (CMTimeGetSeconds(p.currentTime()) * 1000).toLong().coerceAtLeast(0)
                val duration = if (item != null) {
                    (CMTimeGetSeconds(item.duration) * 1000).toLong().coerceAtLeast(0)
                } else {
                    0L
                }
                val ended = duration > 0 && position >= duration - 250
                _snapshot.update {
                    if (ended) {
                        it.copy(status = PlaybackStatus.Ended, positionMs = duration, durationMs = duration)
                    } else {
                        it.copy(positionMs = position, durationMs = duration)
                    }
                }
                if (ended) break
                delay(500)
            }
        }
    }

    private fun stopTicker() {
        ticker?.cancel()
        ticker = null
    }

    private fun releaseInternal(keepUrl: Boolean) {
        player?.pause()
        player?.replaceCurrentItemWithPlayerItem(null)
        currentItem = null
        player = null
        if (!keepUrl) pendingUrl = null
    }

    private fun ensureAudioSession() {
        runCatching {
            val session = AVAudioSession.sharedInstance()
            session.setCategory(AVAudioSessionCategoryPlayback, error = null)
            session.setActive(true, error = null)
        }
    }

    private fun deactivateAudioSession() {
        runCatching {
            AVAudioSession.sharedInstance().setActive(false, error = null)
        }
    }
}

package com.example.my_kmp_project.feature.media

import com.example.my_kmp_project.core.ui.PlaybackSnapshot
import com.example.my_kmp_project.core.ui.PlaybackStatus
import kotlinx.cinterop.ExperimentalForeignApi
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
import platform.AVFAudio.AVAudioSessionInterruptionNotification
import platform.AVFAudio.AVAudioSessionInterruptionOptionKey
import platform.AVFAudio.AVAudioSessionInterruptionTypeKey
import platform.AVFAudio.AVAudioSessionRouteChangeNotification
import platform.AVFAudio.AVAudioSessionRouteChangeReasonKey
import platform.AVFAudio.setActive
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerItemDidPlayToEndTimeNotification
import platform.AVFoundation.currentTime
import platform.AVFoundation.duration
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.replaceCurrentItemWithPlayerItem
import platform.AVFoundation.seekToTime
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSNumber
import platform.Foundation.NSObjectProtocol
import platform.Foundation.NSURL
import platform.MediaPlayer.MPChangePlaybackPositionCommandEvent
import platform.MediaPlayer.MPMediaItemPropertyArtist
import platform.MediaPlayer.MPMediaItemPropertyPlaybackDuration
import platform.MediaPlayer.MPMediaItemPropertyTitle
import platform.MediaPlayer.MPNowPlayingInfoCenter
import platform.MediaPlayer.MPNowPlayingInfoPropertyElapsedPlaybackTime
import platform.MediaPlayer.MPNowPlayingInfoPropertyPlaybackRate
import platform.MediaPlayer.MPRemoteCommandCenter
import platform.MediaPlayer.MPRemoteCommandHandlerStatusSuccess

@OptIn(ExperimentalForeignApi::class)
actual fun createAudioEngine(): AudioEngine = IosAudioEngine()

/**
 * iOS 音频引擎：AVPlayer + Now Playing / Remote Command + 后台音频会话。
 *
 * - 锁屏与通知中心展示标题/艺人/进度，耳机线控与锁屏按钮回到同一队列；
 * - 电话 / 其他 App 抢占音频时暂停，恢复后按用户意图续播；
 * - 蓝牙/耳机拔出（旧路由不可用）立即暂停，对齐系统音乐行为。
 */
@OptIn(ExperimentalForeignApi::class)
class IosAudioEngine : AudioEngine {

    private var player: AVPlayer? = null
    private var currentItem: AVPlayerItem? = null
    private var pendingUrl: String? = null
    private var wantPlay = false
    private var meta: AudioTrackMeta? = null
    private var volume = 1f

    private val _snapshot = MutableStateFlow(PlaybackSnapshot(status = PlaybackStatus.Idle))
    override val snapshot: StateFlow<PlaybackSnapshot> = _snapshot.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var ticker: Job? = null
    private var endObserver: NSObjectProtocol? = null
    private val sessionObservers = mutableListOf<NSObjectProtocol>()

    init {
        observeAudioSession()
    }

    override fun load(url: String, meta: AudioTrackMeta?, autoPlay: Boolean) {
        pendingUrl = url
        this.meta = meta
        wantPlay = autoPlay
        stopTicker()
        removeEndObserver()
        releasePlayer()

        val nsUrl = NSURL.URLWithString(url)
        if (nsUrl == null) {
            _snapshot.value = PlaybackSnapshot(
                status = PlaybackStatus.Error,
                errorMessage = "无效音频地址",
            )
            return
        }
        _snapshot.value = PlaybackSnapshot(status = PlaybackStatus.Loading)
        runCatching {
            ensureAudioSession()
            val item = AVPlayerItem(uRL = nsUrl)
            val p = AVPlayer().also { player = it }
            currentItem = item
            p.replaceCurrentItemWithPlayerItem(item)
            p.volume = volume
            observeEnd(item)
            if (autoPlay) {
                p.play()
                _snapshot.update { it.copy(status = PlaybackStatus.Playing) }
                startTicker()
            }
            updateNowPlaying(isPlaying = autoPlay)
        }.onFailure { t ->
            _snapshot.value = PlaybackSnapshot(
                status = PlaybackStatus.Error,
                errorMessage = t.message ?: "load failed",
            )
        }
    }

    override fun play() {
        wantPlay = true
        ensureAudioSession()
        val p = player
        if (p == null) {
            pendingUrl?.let { load(it, meta, autoPlay = true) }
            return
        }
        p.play()
        _snapshot.update { it.copy(status = PlaybackStatus.Playing, errorMessage = null) }
        startTicker()
        updateNowPlaying(isPlaying = true)
    }

    override fun pause() {
        wantPlay = false
        stopTicker()
        player?.pause()
        _snapshot.update {
            if (it.status == PlaybackStatus.Error || it.status == PlaybackStatus.Unsupported) it
            else it.copy(status = PlaybackStatus.Paused)
        }
        updateNowPlaying(isPlaying = false)
    }

    override fun seekTo(positionMs: Long) {
        val p = player ?: return
        val durationMs = currentDurationMs()
        val target = positionMs.coerceIn(0L, durationMs)
        runCatching {
            p.seekToTime(CMTimeMakeWithSeconds(target / 1000.0, 600))
            _snapshot.update { it.copy(positionMs = target) }
            updateNowPlaying(isPlaying = p.rate > 0f)
        }.onFailure { t ->
            _snapshot.update {
                it.copy(status = PlaybackStatus.Error, errorMessage = t.message ?: "seek failed")
            }
        }
    }

    override fun stop() {
        wantPlay = false
        stopTicker()
        removeEndObserver()
        releasePlayer()
        deactivateAudioSession()
        _snapshot.value = PlaybackSnapshot(status = PlaybackStatus.Idle)
        clearNowPlaying()
    }

    override fun release() {
        wantPlay = false
        stopTicker()
        removeEndObserver()
        releasePlayer()
        pendingUrl = null
        meta = null
        deactivateAudioSession()
        _snapshot.value = PlaybackSnapshot(status = PlaybackStatus.Idle)
        clearNowPlaying()
    }

    override fun setVolume(volume: Float) {
        this.volume = volume.coerceIn(0f, 1f)
        player?.volume = this.volume
    }

    override fun setRemoteCommands(commands: AudioRemoteCommands) {
        val center = MPRemoteCommandCenter.sharedCommandCenter()
        center.playCommand.isEnabled = true
        center.pauseCommand.isEnabled = true
        center.nextTrackCommand.isEnabled = true
        center.previousTrackCommand.isEnabled = true
        center.changePlaybackPositionCommand.isEnabled = true

        center.playCommand.addTargetWithHandler {
            commands.onPlay()
            MPRemoteCommandHandlerStatusSuccess
        }
        center.pauseCommand.addTargetWithHandler {
            commands.onPause()
            MPRemoteCommandHandlerStatusSuccess
        }
        center.nextTrackCommand.addTargetWithHandler {
            commands.onNext()
            MPRemoteCommandHandlerStatusSuccess
        }
        center.previousTrackCommand.addTargetWithHandler {
            commands.onPrevious()
            MPRemoteCommandHandlerStatusSuccess
        }
        center.changePlaybackPositionCommand.addTargetWithHandler { event ->
            (event as? MPChangePlaybackPositionCommandEvent)?.let {
                commands.onSeek((it.positionTime * 1000.0).toLong())
            }
            MPRemoteCommandHandlerStatusSuccess
        }
    }

    private fun startTicker() {
        if (ticker?.isActive == true) return
        ticker = scope.launch {
            while (isActive) {
                val p = player ?: break
                val position = (CMTimeGetSeconds(p.currentTime()) * 1000.0).toLong().coerceAtLeast(0L)
                val duration = currentDurationMs()
                _snapshot.update { it.copy(positionMs = position, durationMs = duration) }
                refreshElapsed(position, duration, isPlaying = p.rate > 0f)
                delay(500)
            }
        }
    }

    private fun stopTicker() {
        ticker?.cancel()
        ticker = null
    }

    private fun currentDurationMs(): Long {
        val item = currentItem ?: return 0L
        return (CMTimeGetSeconds(item.duration) * 1000.0).toLong().coerceAtLeast(0L)
    }

    private fun releasePlayer() {
        player?.pause()
        player?.replaceCurrentItemWithPlayerItem(null)
        currentItem = null
        player = null
    }

    private fun observeEnd(item: AVPlayerItem) {
        endObserver = NSNotificationCenter.defaultCenter.addObserverForName(
            name = AVPlayerItemDidPlayToEndTimeNotification,
            object = item,
            queue = null,
        ) {
            wantPlay = false
            stopTicker()
            _snapshot.update {
                val duration = currentDurationMs()
                it.copy(status = PlaybackStatus.Ended, positionMs = duration, durationMs = duration)
            }
            updateNowPlaying(isPlaying = false)
        }
    }

    private fun removeEndObserver() {
        endObserver?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
        endObserver = null
    }

    private fun observeAudioSession() {
        val center = NSNotificationCenter.defaultCenter
        sessionObservers += center.addObserverForName(
            name = AVAudioSessionInterruptionNotification,
            object = null,
            queue = null,
        ) { notification -> handleInterruption(notification) }
        sessionObservers += center.addObserverForName(
            name = AVAudioSessionRouteChangeNotification,
            object = null,
            queue = null,
        ) { notification -> handleRouteChange(notification) }
    }

    private fun handleInterruption(notification: NSNotification?) {
        val info = notification?.userInfo ?: return
        val type = (info[AVAudioSessionInterruptionTypeKey] as? NSNumber)?.unsignedLongValue ?: return
        if (type == 1UL) {
            // Began：被电话 / 其他 App 抢占，先暂停但保留续播意图。
            val resumeAfter = wantPlay
            player?.pause()
            stopTicker()
            _snapshot.update {
                if (it.status == PlaybackStatus.Playing) it.copy(status = PlaybackStatus.Paused) else it
            }
            updateNowPlaying(isPlaying = false)
            wantPlay = resumeAfter
        } else if (type == 0UL) {
            // Ended：仅当系统给出 shouldResume 且用户此前仍在播放时恢复。
            val options = (info[AVAudioSessionInterruptionOptionKey] as? NSNumber)?.unsignedLongValue ?: 0UL
            val shouldResume = (options and 1UL) != 0UL
            if (shouldResume && wantPlay) {
                ensureAudioSession()
                play()
            }
        }
    }

    private fun handleRouteChange(notification: NSNotification?) {
        val info = notification?.userInfo ?: return
        val reason = (info[AVAudioSessionRouteChangeReasonKey] as? NSNumber)?.unsignedLongValue ?: return
        // OldDeviceUnavailable：耳机 / 蓝牙断开，系统期望暂停而不是外放。
        if (reason == 2UL) pause()
    }

    private fun updateNowPlaying(isPlaying: Boolean) {
        val meta = meta ?: return
        val duration = currentDurationMs()
        val position = (CMTimeGetSeconds(player?.currentTime() ?: return) * 1000.0).toLong()
        MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo = mapOf(
            MPMediaItemPropertyTitle to meta.title,
            MPMediaItemPropertyArtist to meta.artist,
            MPMediaItemPropertyPlaybackDuration to (duration / 1000.0),
            MPNowPlayingInfoPropertyElapsedPlaybackTime to (position / 1000.0),
            MPNowPlayingInfoPropertyPlaybackRate to if (isPlaying) 1.0 else 0.0,
        )
    }

    private fun refreshElapsed(positionMs: Long, durationMs: Long, isPlaying: Boolean) {
        val meta = meta ?: return
        MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo = mapOf(
            MPMediaItemPropertyTitle to meta.title,
            MPMediaItemPropertyArtist to meta.artist,
            MPMediaItemPropertyPlaybackDuration to (durationMs / 1000.0),
            MPNowPlayingInfoPropertyElapsedPlaybackTime to (positionMs / 1000.0),
            MPNowPlayingInfoPropertyPlaybackRate to if (isPlaying) 1.0 else 0.0,
        )
    }

    private fun clearNowPlaying() {
        MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo = null
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

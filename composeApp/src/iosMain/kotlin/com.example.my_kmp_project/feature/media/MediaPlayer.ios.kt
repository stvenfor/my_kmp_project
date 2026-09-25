package com.example.my_kmp_project.feature.media

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.setActive
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.replaceCurrentItemWithPlayerItem
import platform.Foundation.NSURL

/**
 * iOS AVPlayer audio — parity with Android MediaPlayer for music smoke.
 * Video surface hosting remains a follow-up.
 */
@OptIn(ExperimentalForeignApi::class)
internal actual fun createMediaPlayer(): MediaPlayerController = IosAvMediaPlayer()

@OptIn(ExperimentalForeignApi::class)
internal class IosAvMediaPlayer : MediaPlayerController {
    private var player: AVPlayer? = null
    private var pendingUrl: String? = null
    private var wantPlay = false

    override var isPlaying by mutableStateOf(false)
        private set

    override fun setSource(url: String) {
        pendingUrl = url
        wantPlay = false
        releaseInternal(keepUrl = true)
        val nsUrl = NSURL.URLWithString(url) ?: return
        ensureAudioSession()
        val item = AVPlayerItem(uRL = nsUrl)
        val p = player ?: AVPlayer().also { player = it }
        p.replaceCurrentItemWithPlayerItem(item)
        if (wantPlay) {
            p.play()
            isPlaying = true
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
            player?.play()
            isPlaying = true
            return
        }
        p.play()
        isPlaying = true
    }

    override fun pause() {
        wantPlay = false
        player?.pause()
        isPlaying = false
    }

    override fun stop() {
        wantPlay = false
        player?.pause()
        isPlaying = false
    }

    override fun release() {
        releaseInternal(keepUrl = false)
    }

    private fun releaseInternal(keepUrl: Boolean) {
        player?.pause()
        player?.replaceCurrentItemWithPlayerItem(null)
        if (!keepUrl) {
            player = null
            pendingUrl = null
        }
        isPlaying = false
    }

    private fun ensureAudioSession() {
        runCatching {
            val session = AVAudioSession.sharedInstance()
            session.setCategory(AVAudioSessionCategoryPlayback, error = null)
            session.setActive(true, error = null)
        }
    }
}

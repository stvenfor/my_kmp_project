package com.example.my_kmp_project.feature.media

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

private const val TAG = "AndroidMediaPlayer"

internal actual fun createMediaPlayer(): MediaPlayerController = AndroidMediaPlayer()

/**
 * Real Android [MediaPlayer] for audio streams (music / sample URL).
 * Video surface hosting remains a follow-up; audio decode is enough for 7.2 smoke.
 */
internal class AndroidMediaPlayer : MediaPlayerController {
    private var player: MediaPlayer? = null
    private var pendingUrl: String? = null
    private var wantPlay = false

    override var isPlaying by mutableStateOf(false)
        private set

    override fun setSource(url: String) {
        pendingUrl = url
        wantPlay = false
        releaseInternal(keepUrl = true)
        try {
            val host = this
            val mp = MediaPlayer()
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            mp.setDataSource(url)
            mp.setOnPreparedListener {
                if (host.wantPlay) {
                    try {
                        it.start()
                        host.isPlaying = true
                    } catch (t: Throwable) {
                        Log.w(TAG, "start after prepare failed: ${t.message}")
                    }
                }
            }
            mp.setOnCompletionListener {
                host.isPlaying = false
                host.wantPlay = false
            }
            mp.setOnErrorListener { _, what, extra ->
                Log.w(TAG, "MediaPlayer error what=$what extra=$extra")
                host.isPlaying = false
                host.wantPlay = false
                true
            }
            mp.prepareAsync()
            player = mp
        } catch (t: Throwable) {
            Log.w(TAG, "setSource failed: ${t.message}")
            player = null
        }
    }

    override fun play() {
        wantPlay = true
        val mp = player
        if (mp == null) {
            val url = pendingUrl
            if (url != null) setSource(url)
            isPlaying = true
            return
        }
        try {
            if (!mp.isPlaying) {
                mp.start()
            }
            isPlaying = true
        } catch (t: Throwable) {
            // Not prepared yet — OnPreparedListener will start.
            Log.w(TAG, "play deferred: ${t.message}")
            isPlaying = true
        }
    }

    override fun pause() {
        wantPlay = false
        try {
            player?.takeIf { it.isPlaying }?.pause()
        } catch (_: Throwable) {
        }
        isPlaying = false
    }

    override fun stop() {
        wantPlay = false
        try {
            player?.stop()
        } catch (_: Throwable) {
        }
        isPlaying = false
        releaseInternal(keepUrl = true)
    }

    override fun release() {
        wantPlay = false
        releaseInternal(keepUrl = false)
        pendingUrl = null
        isPlaying = false
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

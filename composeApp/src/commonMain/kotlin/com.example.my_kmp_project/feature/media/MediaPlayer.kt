package com.example.my_kmp_project.feature.media

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Shared playback contract; platform adapters replace [StubMediaPlayer] via [createMediaPlayer]. */
internal interface MediaPlayerController {
    val isPlaying: Boolean
    fun setSource(url: String)
    fun play()
    fun pause()
    fun stop()
    fun release()
}

/** In-memory stub — toggles state only, no real audio/video. Used when platform lacks a player. */
internal class StubMediaPlayer : MediaPlayerController {
    override var isPlaying by mutableStateOf(false)
        private set
    private var source: String? = null

    override fun setSource(url: String) {
        source = url
    }

    override fun play() {
        isPlaying = true
    }

    override fun pause() {
        isPlaying = false
    }

    override fun stop() {
        isPlaying = false
    }

    override fun release() {
        isPlaying = false
        source = null
    }
}

internal expect fun createMediaPlayer(): MediaPlayerController

/** Cross-screen music session so Home can show [MiniPlayerBar]. */
internal object MusicSession {
    var trackTitle by mutableStateOf<String?>(null)
        private set
    var artist by mutableStateOf("")
        private set
    val player: MediaPlayerController = createMediaPlayer()

    /** Public domain sample used for real Android/iOS decode smoke when URL available. */
    const val SAMPLE_AUDIO_URL =
        "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"

    val isActive: Boolean get() = trackTitle != null

    fun start(title: String, artistName: String, audioUrl: String = SAMPLE_AUDIO_URL) {
        trackTitle = title
        artist = artistName
        player.setSource(audioUrl)
        player.play()
    }

    fun dismiss() {
        player.stop()
        trackTitle = null
        artist = ""
    }
}

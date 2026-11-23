package com.example.my_kmp_project.feature.media

/**
 * OHOS: no MediaKit/AVPlayer bridge for music yet. Emit an explicit Unsupported
 * snapshot instead of a stub that pretends playback succeeded. Video uses its own
 * ArkUI surface; this controller is audio-only.
 */
actual fun createMediaPlayer(): MediaPlayerController = UnsupportedMediaPlayer()

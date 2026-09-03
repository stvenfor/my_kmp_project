package com.example.my_kmp_project.feature.media

/**
 * iOS: AVPlayer wiring deferred — keep stub behavior until native audio session is linked.
 * Enumerated in platform-gap-registry as partial for media playback.
 */
internal actual fun createMediaPlayer(): MediaPlayerController = StubMediaPlayer()

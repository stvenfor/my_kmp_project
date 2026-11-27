package com.example.my_kmp_project.core.ui

/**
 * 后端 `GET /api/v1/music/tracks` 返回的单首曲目（跨端共享模型）。
 *
 * 放在 core.ui 是为了让 [MusicSessionPort] 只暴露端口模型，feature 不必把
 * `HttpMusicRepository` 的 DTO 泄漏给其它模块。
 */
data class MusicTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val coverUrl: String? = null,
    val audioUrl: String,
    val durationMs: Long = 0L,
)

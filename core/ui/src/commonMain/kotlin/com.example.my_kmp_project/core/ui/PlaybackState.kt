package com.example.my_kmp_project.core.ui

/**
 * 共享播放状态——供播放器 surface 与各业务页复用。
 *
 * 状态由平台按真实能力回报：无法可靠判定的状态（例如某些平台无法区分 Ended/Paused）
 * 不得臆造，应使用 [PlaybackStatus.Unsupported]；播放失败用 [PlaybackStatus.Error]
 * 并附带 [PlaybackSnapshot.errorMessage]。
 */
enum class PlaybackStatus {
    Idle,
    Loading,
    Buffering,
    Playing,
    Paused,
    Ended,
    Error,
    Unsupported,
}

/** 某一时刻的播放快照。时间单位为毫秒，未知时用默认值 0。 */
data class PlaybackSnapshot(
    val status: PlaybackStatus,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val errorMessage: String? = null,
)

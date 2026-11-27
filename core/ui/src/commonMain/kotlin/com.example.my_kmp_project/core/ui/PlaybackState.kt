package com.example.my_kmp_project.core.ui

import kotlinx.coroutines.flow.StateFlow

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

/** 音乐会话的控制句柄；UI 只依赖命令集合，不直接持有平台播放器。 */
data class MusicPlaybackCommands(
    val onPlay: () -> Unit,
    val onPause: () -> Unit,
    val onSeek: (Long) -> Unit,
    val onStop: () -> Unit,
)

/**
 * 应用级音乐会话端口（DI 第四批）。
 *
 * 实例由 `AppGraph` 持有（AppScope 单例），平台实现留在 `feature/media`；
 * feature 内的 UI 只依赖本接口，不再直接引用 `object MusicSession`。
 */
interface MusicSessionPort {
    val trackTitle: String?
    val artist: String
    val album: String?
    val coverUrl: String?
    /** 当前曲目在播放队列中的位置；无曲目时为 -1。 */
    val queueIndex: Int
    val queueSize: Int
    val snapshot: StateFlow<PlaybackSnapshot>
    val controls: MusicPlaybackCommands
    val isActive: Boolean

    /** 播放来自后端的单曲（不落本地假数据）。 */
    fun start(track: MusicTrack)

    /** 播放整张队列；[startIndex] 越界时钳到合法范围。 */
    fun playQueue(tracks: List<MusicTrack>, startIndex: Int = 0)

    fun next()
    fun previous()
    fun pause()
    fun resume()
    fun seekTo(positionMs: Long)
    fun stop()
    fun dismiss()
}

package com.example.my_kmp_project.feature.media

import com.example.my_kmp_project.core.ui.MusicTrack
import com.example.my_kmp_project.core.ui.PlaybackSnapshot
import kotlinx.coroutines.flow.StateFlow

/** 锁屏 / 通知 / 车机展示用的曲目元数据；平台不支持的字段可忽略。 */
data class AudioTrackMeta(
    val title: String,
    val artist: String,
    val album: String? = null,
    val artworkUrl: String? = null,
)

/** 远程控制（锁屏、耳机线控、通知栏）回调；平台不支持时静默忽略。 */
class AudioRemoteCommands(
    val onPlay: () -> Unit,
    val onPause: () -> Unit,
    val onSeek: (Long) -> Unit,
    val onNext: () -> Unit,
    val onPrevious: () -> Unit,
)

/**
 * 纯音频引擎契约：只负责「一个音频源」的装载与传输控制。
 *
 * 队列、曲目元数据、自动下一首由 [PlaybackSession] 承担，平台实现只回报真实状态；
 * 无法可靠判定的状态用 [com.example.my_kmp_project.core.ui.PlaybackStatus.Unsupported]，
 * 绝不臆造播放进度。
 *
 * Android = Media3 ExoPlayer + MediaSessionService（后台/锁屏），
 * iOS = AVPlayer + Now Playing / Remote Command，OHOS = AVPlayer NDK + AVSession。
 */
interface AudioEngine {
    val snapshot: StateFlow<PlaybackSnapshot>

    /** 装载音频源；[autoPlay] 为 false 时只 prepare，不自动起播。 */
    fun load(url: String, meta: AudioTrackMeta?, autoPlay: Boolean)

    fun play()
    fun pause()
    fun seekTo(positionMs: Long)
    fun stop()
    fun release()

    /** 0f..1f；平台不支持音量控制时静默忽略。 */
    fun setVolume(volume: Float)

    /** 注册锁屏 / 通知栏远程控制；默认实现表示该平台不支持。 */
    fun setRemoteCommands(commands: AudioRemoteCommands) = Unit

    companion object {
        fun metaOf(track: MusicTrack): AudioTrackMeta = AudioTrackMeta(
            title = track.title,
            artist = track.artist,
            album = track.album,
            artworkUrl = track.coverUrl,
        )
    }
}

expect fun createAudioEngine(): AudioEngine

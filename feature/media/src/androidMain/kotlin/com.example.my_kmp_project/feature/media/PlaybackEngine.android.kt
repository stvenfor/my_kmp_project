package com.example.my_kmp_project.feature.media

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import com.example.my_kmp_project.core.storage.AndroidAppContext
import com.example.my_kmp_project.core.ui.PlaybackSnapshot
import com.example.my_kmp_project.core.ui.PlaybackStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

private const val TAG = "AndroidAudioEngine"

actual fun createAudioEngine(): AudioEngine = AndroidAudioEngine(context = AndroidAppContext.applicationContext)

/**
 * 进程级播放器 + MediaSession 持有者。
 *
 * 引擎（UI 驱动）与 [MusicPlaybackService]（后台/锁屏）必须操作同一个 ExoPlayer，
 * 否则通知栏与控制台会播放两个音轨；因此由本 holder 统一创建与释放。
 */
internal object AndroidAudioHolder {
    private var exo: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    fun player(context: Context): ExoPlayer = exo ?: ExoPlayer.Builder(context)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(),
            /* handleAudioFocus = */ true,
        )
        .setHandleAudioBecomingNoisy(true)
        .build()
        .also { exo = it }

    /** 只读当前实例：回调期间不得因为 holder 已释放而新建播放器。 */
    fun currentPlayer(): ExoPlayer? = exo

    fun session(context: Context): MediaSession = mediaSession ?: MediaSession.Builder(
        context,
        player(context),
    ).build().also { mediaSession = it }

    fun release() {
        mediaSession?.release()
        mediaSession = null
        exo?.release()
        exo = null
    }
}

/** Android 音频引擎：Media3 ExoPlayer + MediaSessionService。 */
class AndroidAudioEngine(private val context: Context?) : AudioEngine {

    private val _snapshot = MutableStateFlow(PlaybackSnapshot(status = PlaybackStatus.Idle))
    override val snapshot: StateFlow<PlaybackSnapshot> = _snapshot.asStateFlow()

    private val mainHandler = Handler(Looper.getMainLooper())
    private var pendingUrl: String? = null
    private var volume = 1f

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            val player = AndroidAudioHolder.currentPlayer() ?: return
            _snapshot.update {
                when (playbackState) {
                    Player.STATE_BUFFERING -> it.copy(status = PlaybackStatus.Buffering, errorMessage = null)
                    Player.STATE_READY -> it.copy(
                        status = if (player.playWhenReady) PlaybackStatus.Playing else PlaybackStatus.Paused,
                        durationMs = player.duration.coerceAtLeast(0L),
                        errorMessage = null,
                    )
                    Player.STATE_ENDED -> it.copy(status = PlaybackStatus.Ended, errorMessage = null)
                    Player.STATE_IDLE -> it
                    else -> it
                }
            }
            if (playbackState == Player.STATE_READY || playbackState == Player.STATE_BUFFERING) {
                startTicker()
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _snapshot.update {
                when {
                    it.status == PlaybackStatus.Error || it.status == PlaybackStatus.Unsupported -> it
                    isPlaying -> it.copy(status = PlaybackStatus.Playing, errorMessage = null)
                    it.status == PlaybackStatus.Ended -> it
                    it.status == PlaybackStatus.Loading || it.status == PlaybackStatus.Buffering -> it
                    else -> it.copy(status = PlaybackStatus.Paused)
                }
            }
            if (isPlaying) startTicker()
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.w(TAG, "player error: ${error.errorCodeName} ${error.message}")
            _snapshot.update {
                it.copy(
                    status = PlaybackStatus.Error,
                    errorMessage = error.message ?: error.errorCodeName,
                )
            }
        }
    }

    private val ticker = object : Runnable {
        override fun run() {
            val player = AndroidAudioHolder.currentPlayer() ?: return
            if (player.isPlaying || player.playbackState == Player.STATE_BUFFERING) {
                _snapshot.update {
                    it.copy(
                        positionMs = player.currentPosition.coerceAtLeast(0L),
                        durationMs = player.duration.coerceAtLeast(0L),
                    )
                }
                mainHandler.postDelayed(this, 500L)
            }
        }
    }

    override fun load(url: String, meta: AudioTrackMeta?, autoPlay: Boolean) {
        val ctx = context
        if (ctx == null) {
            _snapshot.value = PlaybackSnapshot(
                status = PlaybackStatus.Error,
                errorMessage = "AndroidAppContext 未安装，无法创建播放器",
            )
            return
        }
        if (url.isBlank()) {
            _snapshot.value = PlaybackSnapshot(status = PlaybackStatus.Error, errorMessage = "音频地址为空")
            return
        }
        pendingUrl = url
        val player = AndroidAudioHolder.player(ctx)
        player.removeListener(listener)
        player.addListener(listener)
        player.setMediaItem(buildItem(url, meta))
        player.volume = volume
        player.playWhenReady = autoPlay
        player.prepare()
        _snapshot.value = PlaybackSnapshot(status = PlaybackStatus.Loading)
        ensureService(ctx)
        startTicker()
    }

    override fun play() {
        val url = pendingUrl
        if (url.isNullOrBlank()) return
        val ctx = context ?: return
        val player = AndroidAudioHolder.player(ctx)
        player.playWhenReady = true
        ensureService(ctx)
        startTicker()
    }

    override fun pause() {
        context?.let { AndroidAudioHolder.player(it).playWhenReady = false }
        mainHandler.removeCallbacks(ticker)
        _snapshot.update {
            if (it.status == PlaybackStatus.Error || it.status == PlaybackStatus.Unsupported) it
            else it.copy(status = PlaybackStatus.Paused)
        }
    }

    override fun seekTo(positionMs: Long) {
        val ctx = context ?: return
        val player = AndroidAudioHolder.player(ctx)
        val target = positionMs.coerceIn(0L, player.duration.coerceAtLeast(0L))
        player.seekTo(target)
        _snapshot.update { it.copy(positionMs = target) }
    }

    override fun stop() {
        mainHandler.removeCallbacks(ticker)
        context?.let { AndroidAudioHolder.player(it).stop() }
        _snapshot.value = PlaybackSnapshot(
            status = PlaybackStatus.Idle,
            durationMs = _snapshot.value.durationMs,
        )
    }

    override fun release() {
        mainHandler.removeCallbacks(ticker)
        AndroidAudioHolder.release()
        pendingUrl = null
        _snapshot.value = PlaybackSnapshot(status = PlaybackStatus.Idle)
        context?.let { ctx ->
            runCatching { ctx.stopService(Intent(ctx, MusicPlaybackService::class.java)) }
        }
    }

    override fun setVolume(volume: Float) {
        this.volume = volume.coerceIn(0f, 1f)
        context?.let { AndroidAudioHolder.player(it).volume = this.volume }
    }

    override fun setRemoteCommands(commands: AudioRemoteCommands) {
        // 通知栏 play/pause/seek 由 MediaSession 直接驱动同一个 ExoPlayer；
        // 这里确保会话已创建，后续后台播放才有可用的媒体会话。
        context?.let { ctx ->
            AndroidAudioHolder.session(ctx)
            ensureService(ctx)
        }
    }

    private fun ensureService(ctx: Context) {
        runCatching { ctx.startService(Intent(ctx, MusicPlaybackService::class.java)) }
            .onFailure { Log.w(TAG, "startService failed: ${it.message}") }
    }

    private fun startTicker() {
        mainHandler.removeCallbacks(ticker)
        mainHandler.post(ticker)
    }

    private fun buildItem(url: String, meta: AudioTrackMeta?): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(meta?.title)
            .setArtist(meta?.artist)
            .setAlbumTitle(meta?.album)
            .setArtworkUri(meta?.artworkUrl?.takeIf { it.isNotBlank() }?.let(Uri::parse))
            .build()
        return MediaItem.Builder()
            .setUri(url)
            .setMediaMetadata(metadata)
            .build()
    }
}

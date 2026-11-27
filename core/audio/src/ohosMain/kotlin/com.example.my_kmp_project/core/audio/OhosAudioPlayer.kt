package com.example.my_kmp_project.core.audio

import cnames.structs.OH_AVPlayer
import cnames.structs.OH_AVSession
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.alloc
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import platform.avplayer.AV_ERR_OK
import platform.avplayer.AV_INFO_TYPE_EOS
import platform.avplayer.AV_SEEK_CLOSEST
import platform.avplayer.OH_AVPlayerOnErrorCallback
import platform.avplayer.OH_AVPlayerOnInfoCallback
import platform.avplayer.OH_AVPlayer_Create
import platform.avplayer.OH_AVPlayer_GetCurrentTime
import platform.avplayer.OH_AVPlayer_GetDuration
import platform.avplayer.OH_AVPlayer_IsPlaying
import platform.avplayer.OH_AVPlayer_Pause
import platform.avplayer.OH_AVPlayer_Play
import platform.avplayer.OH_AVPlayer_Prepare
import platform.avplayer.OH_AVPlayer_Release
import platform.avplayer.OH_AVPlayer_Reset
import platform.avplayer.OH_AVPlayer_Seek
import platform.avplayer.OH_AVPlayer_SetOnErrorCallback
import platform.avplayer.OH_AVPlayer_SetOnInfoCallback
import platform.avplayer.OH_AVPlayer_SetURLSource
import platform.avplayer.OH_AVPlayer_SetVolume
import platform.avplayer.OH_AVPlayer_Stop
import platform.avsession.AVSESSION_CALLBACK_RESULT_SUCCESS
import platform.avsession.AVSession_PlaybackPosition
import platform.avsession.CONTROL_CMD_PAUSE
import platform.avsession.CONTROL_CMD_PLAY
import platform.avsession.CONTROL_CMD_PLAY_NEXT
import platform.avsession.CONTROL_CMD_PLAY_PREVIOUS
import platform.avsession.CONTROL_CMD_STOP
import platform.avsession.OH_AVMetadata
import platform.avsession.OH_AVMetadataBuilder
import platform.avsession.OH_AVMetadataBuilder_Create
import platform.avsession.OH_AVMetadataBuilder_Destroy
import platform.avsession.OH_AVMetadataBuilder_GenerateAVMetadata
import platform.avsession.OH_AVMetadataBuilder_SetAlbum
import platform.avsession.OH_AVMetadataBuilder_SetArtist
import platform.avsession.OH_AVMetadataBuilder_SetAssetId
import platform.avsession.OH_AVMetadataBuilder_SetDuration
import platform.avsession.OH_AVMetadataBuilder_SetMediaImageUri
import platform.avsession.OH_AVMetadataBuilder_SetTitle
import platform.avsession.OH_AVMetadata_Destroy
import platform.avsession.OH_AVSessionCallback_OnCommand
import platform.avsession.OH_AVSessionCallback_OnSeek
import platform.avsession.OH_AVSession_Activate
import platform.avsession.OH_AVSession_Create
import platform.avsession.OH_AVSession_Destroy
import platform.avsession.OH_AVSession_RegisterCommandCallback
import platform.avsession.OH_AVSession_RegisterSeekCallback
import platform.avsession.OH_AVSession_SetAVMetadata
import platform.avsession.OH_AVSession_SetPlaybackPosition
import platform.avsession.OH_AVSession_SetPlaybackState
import platform.avsession.PLAYBACK_STATE_COMPLETED
import platform.avsession.PLAYBACK_STATE_PAUSED
import platform.avsession.PLAYBACK_STATE_PLAYING
import platform.avsession.PLAYBACK_STATE_STOPPED
import platform.avsession.SESSION_TYPE_AUDIO

/**
 * OHOS 音频桥：AVPlayer（MediaKit）+ AVSession（控制中心/锁屏）。
 *
 * cinterop 只在本模块可见，所以 feature 侧只依赖这个 Kotlin 外观。所有 C 回调通过
 * `StableRef` 回到本实例；释放时必须同时销毁 player / session / StableRef。
 *
 * 后台播放还需要 `module.json5` 的 `backgroundModes: ["audioPlayback"]` 与
 * `ohos.permission.KEEP_BACKGROUND_RUNNING`（见 harmonyApp）。
 */
@OptIn(ExperimentalForeignApi::class)
class OhosAudioPlayer(
    private val onProgress: (positionMs: Long, durationMs: Long, playing: Boolean) -> Unit,
    private val onEnded: () -> Unit,
    private val onError: (String) -> Unit,
) {
    private var player: CPointer<OH_AVPlayer>? = null
    private var session: CPointer<OH_AVSession>? = null
    private var playerRef: StableRef<OhosAudioPlayer>? = null
    private var sessionRef: StableRef<OhosAudioPlayer>? = null

    var onRemotePlay: (() -> Unit)? = null
    var onRemotePause: (() -> Unit)? = null
    var onRemoteStop: (() -> Unit)? = null
    var onRemoteNext: (() -> Unit)? = null
    var onRemotePrevious: (() -> Unit)? = null
    var onRemoteSeek: ((Long) -> Unit)? = null

    private val infoCallback: OH_AVPlayerOnInfoCallback = staticCFunction { _, type, _, userData ->
        val self = userData?.asStableRef<OhosAudioPlayer>()?.get() ?: return@staticCFunction
        if (type == AV_INFO_TYPE_EOS) {
            self.onEnded()
        } else {
            self.emitProgress()
        }
    }

    private val errorCallback: OH_AVPlayerOnErrorCallback = staticCFunction { _, code, message, userData ->
        val self = userData?.asStableRef<OhosAudioPlayer>()?.get() ?: return@staticCFunction
        self.onError(message?.toKString()?.takeIf { it.isNotBlank() } ?: "AVPlayer error $code")
    }

    private val commandCallback: OH_AVSessionCallback_OnCommand = staticCFunction { _, command, userData ->
        val self = userData?.asStableRef<OhosAudioPlayer>()?.get()
        when (command) {
            CONTROL_CMD_PLAY -> self?.onRemotePlay?.invoke()
            CONTROL_CMD_PAUSE -> self?.onRemotePause?.invoke()
            CONTROL_CMD_STOP -> self?.onRemoteStop?.invoke()
            CONTROL_CMD_PLAY_NEXT -> self?.onRemoteNext?.invoke()
            CONTROL_CMD_PLAY_PREVIOUS -> self?.onRemotePrevious?.invoke()
        }
        AVSESSION_CALLBACK_RESULT_SUCCESS
    }

    private val seekCallback: OH_AVSessionCallback_OnSeek = staticCFunction { _, seekTime, userData ->
        userData?.asStableRef<OhosAudioPlayer>()?.get()?.onRemoteSeek?.invoke(seekTime.toLong())
        AVSESSION_CALLBACK_RESULT_SUCCESS
    }

    /** 装载音频源；返回 false 表示原生层拒绝（此时已通过 [onError] 报告）。 */
    fun setUrl(url: String): Boolean {
        val p = ensurePlayer() ?: return false
        // 播完/停止后复用同一 player：先 Reset 回 idle，否则 SetURLSource 会被拒绝。
        OH_AVPlayer_Reset(p)
        return OH_AVPlayer_SetURLSource(p, url) == AV_ERR_OK
    }

    fun prepare() {
        player?.let { OH_AVPlayer_Prepare(it) }
    }

    fun play() {
        player?.let { OH_AVPlayer_Play(it) }
    }

    fun pause() {
        player?.let { OH_AVPlayer_Pause(it) }
    }

    fun stop() {
        player?.let { OH_AVPlayer_Stop(it) }
    }

    fun seekTo(positionMs: Long) {
        player?.let { OH_AVPlayer_Seek(it, positionMs.coerceAtLeast(0L).toInt(), AV_SEEK_CLOSEST) }
    }

    fun setVolume(volume: Float) {
        val v = volume.coerceIn(0f, 1f)
        player?.let { OH_AVPlayer_SetVolume(it, v, v) }
    }

    fun isPlaying(): Boolean = player?.let { OH_AVPlayer_IsPlaying(it) } ?: false

    fun currentTimeMs(): Long = memScoped {
        val p = player ?: return@memScoped 0L
        val out = alloc<IntVar>()
        if (OH_AVPlayer_GetCurrentTime(p, out.ptr) == AV_ERR_OK) out.value.toLong() else 0L
    }

    fun durationMs(): Long = memScoped {
        val p = player ?: return@memScoped 0L
        val out = alloc<IntVar>()
        if (OH_AVPlayer_GetDuration(p, out.ptr) == AV_ERR_OK) out.value.toLong() else 0L
    }

    fun release() {
        session?.let { OH_AVSession_Destroy(it) }
        session = null
        sessionRef?.dispose()
        sessionRef = null
        player?.let { OH_AVPlayer_Release(it) }
        player = null
        playerRef?.dispose()
        playerRef = null
    }

    // ---------------------------------------------------------------------
    // AVSession（锁屏 / 控制中心）
    // ---------------------------------------------------------------------

    fun createSession(
        title: String,
        artist: String,
        album: String?,
        artworkUrl: String?,
        durationMs: Long,
        bundleName: String = DEFAULT_BUNDLE_NAME,
        abilityName: String = DEFAULT_ABILITY_NAME,
        sessionTag: String = DEFAULT_SESSION_TAG,
    ) {
        if (session != null) {
            updateMetadata(title, artist, album, artworkUrl, durationMs)
            return
        }
        memScoped {
            val out = alloc<CPointerVar<OH_AVSession>>()
            OH_AVSession_Create(
                SESSION_TYPE_AUDIO,
                sessionTag,
                bundleName,
                abilityName,
                out.ptr,
            )
            val created = out.value
            if (created == null) {
                onError("AVSession 创建失败")
                return@memScoped
            }
            session = created
            val ref = StableRef.create(this@OhosAudioPlayer)
            sessionRef = ref
            listOf(
                CONTROL_CMD_PLAY,
                CONTROL_CMD_PAUSE,
                CONTROL_CMD_STOP,
                CONTROL_CMD_PLAY_NEXT,
                CONTROL_CMD_PLAY_PREVIOUS,
            ).forEach { command ->
                OH_AVSession_RegisterCommandCallback(created, command, commandCallback, ref.asCPointer())
            }
            OH_AVSession_RegisterSeekCallback(created, seekCallback, ref.asCPointer())
            OH_AVSession_Activate(created)
        }
        updateMetadata(title, artist, album, artworkUrl, durationMs)
    }

    fun updatePlaybackState(playing: Boolean, ended: Boolean, stopped: Boolean = false) {
        val s = session ?: return
        val state = when {
            ended -> PLAYBACK_STATE_COMPLETED
            stopped -> PLAYBACK_STATE_STOPPED
            playing -> PLAYBACK_STATE_PLAYING
            else -> PLAYBACK_STATE_PAUSED
        }
        OH_AVSession_SetPlaybackState(s, state)
    }

    fun updatePosition(positionMs: Long) {
        val s = session ?: return
        memScoped {
            val pos = alloc<AVSession_PlaybackPosition>()
            pos.elapsedTime = positionMs.coerceAtLeast(0L)
            pos.updateTime = 0L
            OH_AVSession_SetPlaybackPosition(s, pos.ptr)
        }
    }

    fun updateMetadata(
        title: String,
        artist: String,
        album: String?,
        artworkUrl: String?,
        durationMs: Long,
    ) {
        val s = session ?: return
        memScoped {
            val builderOut = alloc<CPointerVar<OH_AVMetadataBuilder>>()
            if (OH_AVMetadataBuilder_Create(builderOut.ptr) != 0u) return@memScoped
            val builder = builderOut.value ?: return@memScoped
            OH_AVMetadataBuilder_SetTitle(builder, title)
            OH_AVMetadataBuilder_SetArtist(builder, artist)
            OH_AVMetadataBuilder_SetAssetId(builder, title)
            album?.takeIf { it.isNotBlank() }?.let { OH_AVMetadataBuilder_SetAlbum(builder, it) }
            artworkUrl?.takeIf { it.isNotBlank() }
                ?.let { OH_AVMetadataBuilder_SetMediaImageUri(builder, it) }
            if (durationMs > 0L) OH_AVMetadataBuilder_SetDuration(builder, durationMs)

            val metadataOut = alloc<CPointerVar<OH_AVMetadata>>()
            if (OH_AVMetadataBuilder_GenerateAVMetadata(builder, metadataOut.ptr) == 0u) {
                metadataOut.value?.let { metadata ->
                    OH_AVSession_SetAVMetadata(s, metadata)
                    OH_AVMetadata_Destroy(metadata)
                }
            }
            OH_AVMetadataBuilder_Destroy(builder)
        }
    }

    private fun ensurePlayer(): CPointer<OH_AVPlayer>? {
        player?.let { return it }
        val created = OH_AVPlayer_Create() ?: run {
            onError("AVPlayer 创建失败")
            return null
        }
        val ref = StableRef.create(this)
        playerRef = ref
        OH_AVPlayer_SetOnInfoCallback(created, infoCallback, ref.asCPointer())
        OH_AVPlayer_SetOnErrorCallback(created, errorCallback, ref.asCPointer())
        player = created
        return created
    }

    private fun emitProgress() {
        onProgress(currentTimeMs(), durationMs(), isPlaying())
    }

    private companion object {
        // 与 harmonyApp/AppScope/app.json5 和 entry/module.json5 保持一致。
        const val DEFAULT_BUNDLE_NAME = "com.example.my_kmp_project"
        const val DEFAULT_ABILITY_NAME = "EntryAbility"
        const val DEFAULT_SESSION_TAG = "my_kmp_music"
    }
}

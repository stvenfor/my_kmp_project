package com.example.my_kmp_project.feature.media

import android.content.Intent
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * 后台 / 锁屏播放服务：与 UI 共用 [AndroidAudioHolder] 里的同一个 ExoPlayer + MediaSession。
 *
 * 清单声明 `foregroundServiceType="mediaPlayback"`；Media3 在起播后自动转前台并出通知，
 * 因此应用退到后台或锁屏时音频不中断。
 */
class MusicPlaybackService : MediaSessionService() {

    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        session = AndroidAudioHolder.session(this)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player: Player? = session?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        session = null
        AndroidAudioHolder.release()
        super.onDestroy()
    }
}

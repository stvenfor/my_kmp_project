package com.example.my_kmp_project.nativeshell

import android.content.pm.ActivityInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.example.my_kmp_project.core.platform.AndroidActivityContext

/**
 * 视频播放页临时允许横屏：进入时解锁为跟随传感器，离开时恢复进入前的方向。
 *
 * 手机端 MainActivity 默认锁竖屏（平板为 UNSPECIFIED），所以只在播放页解锁，
 * 避免影响其它页面的竖屏体验。
 */
@Composable
fun PlayerLandscapeEffect() {
    DisposableEffect(Unit) {
        val activity = AndroidActivityContext.currentActivity
        val previous = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
        onDispose {
            activity?.requestedOrientation =
                previous ?: ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }
}

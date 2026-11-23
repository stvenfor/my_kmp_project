package com.example.my_kmp_project.feature.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.arkui.TouchableArkUIView
import androidx.compose.ui.napi.JsEnv
import com.example.my_kmp_project.core.ui.PlaybackSnapshot
import com.example.my_kmp_project.core.ui.PlaybackStatus

/**
 * OHOS 仅同步新增的共享签名，ArkUI 视频 surface 的原有参数保持不变。
 *
 * 限制：当前 ArkUI 桥接没有播放状态/进度回传通道，故不读取真实状态、不接 seekToMs；
 * 快照只回报 Idle/Unsupported，不伪造状态或时间数据。
 */
@Composable
actual fun VideoPlaybackSurface(
    url: String,
    isPlaying: Boolean,
    modifier: Modifier,
    onSnapshot: (PlaybackSnapshot) -> Unit,
    seekToMs: Long?,
) {
    val parameters = remember(url, isPlaying) {
        JsEnv.createObject()?.also { args ->
            JsEnv.setProperty(args, JsEnv.createStringUtf8("url"), JsEnv.createStringUtf8(url))
            JsEnv.setProperty(args, JsEnv.createStringUtf8("autoPlay"), JsEnv.createInt32(0))
            JsEnv.setProperty(args, JsEnv.createStringUtf8("muted"), JsEnv.createInt32(0))
            JsEnv.setProperty(args, JsEnv.createStringUtf8("showControls"), JsEnv.createInt32(0))
            JsEnv.setProperty(args, JsEnv.createStringUtf8("startPositionMs"), JsEnv.createInt32(0))
            JsEnv.setProperty(args, JsEnv.createStringUtf8("playing"), JsEnv.createInt32(if (isPlaying) 1 else 0))
            JsEnv.setProperty(args, JsEnv.createStringUtf8("seekToken"), JsEnv.createInt32(0))
            JsEnv.setProperty(args, JsEnv.createStringUtf8("seekPositionMs"), JsEnv.createInt32(0))
        } ?: error("Unable to create ArkUI video parameters")
    }

    val report = rememberUpdatedState(onSnapshot)
    LaunchedEffect(url) {
        report.value(
            PlaybackSnapshot(
                status = if (url.isBlank()) PlaybackStatus.Idle else PlaybackStatus.Unsupported,
            ),
        )
    }

    TouchableArkUIView(
        name = "vodVideoInline",
        modifier = modifier,
        parameter = parameters,
        background = Color.Black,
    )
}

package com.example.my_kmp_project.feature.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.interop.TouchableArkUIView
import androidx.compose.ui.napi.JsEnv
import androidx.compose.ui.napi.JsObject
import com.example.my_kmp_project.core.ui.PlaybackSnapshot
import com.example.my_kmp_project.core.ui.PlaybackStatus

/**
 * OHOS ArkUI 视频 surface。
 *
 * - 播放/暂停通过参数 `playing` 下发；
 * - seek 通过自增 `seekToken` + `seekPositionMs` 下发（ArkTS 侧按 token 变化消费一次）；
 * - 当前 ArkUI 桥没有播放状态回传通道，快照仍只报 Idle/Unsupported，不伪造进度。
 */
@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
@Composable
actual fun VideoPlaybackSurface(
    url: String,
    isPlaying: Boolean,
    modifier: Modifier,
    onSnapshot: (PlaybackSnapshot) -> Unit,
    seekToMs: Long?,
) {
    var seekToken by remember(url) { mutableIntStateOf(0) }
    var seekPositionMs by remember(url) { mutableLongStateOf(0L) }

    LaunchedEffect(url, seekToMs) {
        if (seekToMs != null) {
            seekPositionMs = seekToMs.coerceAtLeast(0L)
            seekToken += 1
        }
    }

    val parameters = remember(url, isPlaying, seekToken, seekPositionMs) {
        JsEnv.createObject()?.also { args ->
            JsEnv.setProperty(args, JsEnv.createStringUtf8("url"), JsEnv.createStringUtf8(url))
            JsEnv.setProperty(args, JsEnv.createStringUtf8("autoPlay"), JsEnv.createInt32(0))
            JsEnv.setProperty(args, JsEnv.createStringUtf8("muted"), JsEnv.createInt32(0))
            JsEnv.setProperty(args, JsEnv.createStringUtf8("showControls"), JsEnv.createInt32(0))
            JsEnv.setProperty(args, JsEnv.createStringUtf8("startPositionMs"), JsEnv.createInt32(0))
            JsEnv.setProperty(args, JsEnv.createStringUtf8("playing"), JsEnv.createInt32(if (isPlaying) 1 else 0))
            JsEnv.setProperty(args, JsEnv.createStringUtf8("seekToken"), JsEnv.createInt32(seekToken))
            JsEnv.setProperty(args, JsEnv.createStringUtf8("seekPositionMs"), JsEnv.createInt32(seekPositionMs.toInt()))
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
        parameter = JsObject(parameters),
        background = Color.Black,
    )
}

package com.example.my_kmp_project.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors

/**
 * 共享播放控制条：播放/暂停、时间轴拖动、无障碍标签。
 *
 * 只消费 [PlaybackSnapshot] 与调用方传入的 [isPlaying]，不持有播放器实例；
 * 拖动回调吐出绝对毫秒位置，由调用方决定是否落到平台播放器。
 */
@Composable
fun PlaybackControls(
    snapshot: PlaybackSnapshot,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onSeek: (positionMs: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val durationMs = snapshot.durationMs.coerceAtLeast(0L)
    val positionMs = snapshot.positionMs.coerceIn(0L, durationMs)
    val canSeek = durationMs > 0L && snapshot.status != PlaybackStatus.Unsupported
    val playPauseLabel = if (isPlaying) "暂停" else "播放"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DemoColors.Background)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onPlayPause,
                modifier = Modifier
                    .size(44.dp)
                    .semantics { contentDescription = playPauseLabel },
            ) {
                Text(
                    text = if (isPlaying) "❚❚" else "▶",
                    color = DemoColors.Accent,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = formatPlaybackTime(positionMs),
                color = DemoColors.TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.semantics {
                    contentDescription = "当前播放时间 ${formatPlaybackTime(positionMs)}"
                },
            )
            Slider(
                value = if (canSeek) positionMs.toFloat() else 0f,
                onValueChange = { onSeek(it.toLong().coerceIn(0L, durationMs)) },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
                    .semantics(mergeDescendants = true) { contentDescription = "播放进度" },
                enabled = canSeek,
                valueRange = if (canSeek) 0f..durationMs.toFloat() else 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = DemoColors.Accent,
                    activeTrackColor = DemoColors.Accent,
                    inactiveTrackColor = DemoColors.Divider,
                ),
            )
            Text(
                text = formatPlaybackTime(durationMs),
                color = DemoColors.TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.semantics {
                    contentDescription = "总时长 ${formatPlaybackTime(durationMs)}"
                },
            )
        }
        if (snapshot.status == PlaybackStatus.Error && !snapshot.errorMessage.isNullOrBlank()) {
            Text(
                text = snapshot.errorMessage.orEmpty(),
                color = DemoColors.Danger,
                fontSize = 12.sp,
                modifier = Modifier.semantics { contentDescription = "播放错误：${snapshot.errorMessage}" },
            )
        }
    }
}

/** mm:ss / hh:mm:ss，与播放器自身时间格式一致。 */
private fun formatPlaybackTime(positionMs: Long): String {
    val totalSeconds = (positionMs.coerceAtLeast(0L) / 1_000L)
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    } else {
        "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    }
}

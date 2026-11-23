package com.example.my_kmp_project.feature.media

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.my_kmp_project.core.ui.PlaybackSnapshot

/** Platform video surface: ComposeMediaPlayer on Android/iOS and ArkUI AVPlayer on OHOS. */
@Composable
expect fun VideoPlaybackSurface(
    url: String,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    onSnapshot: (PlaybackSnapshot) -> Unit = {},
    seekToMs: Long? = null,
)

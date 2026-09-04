package com.example.my_kmp_project.feature.media

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.MineTopBar
import com.example.my_kmp_project.core.ui.ReportMainTabRoot

private data class MockTrack(
    val id: String,
    val title: String,
    val artist: String,
)

@Composable
internal fun MusicListScreen(onBack: () -> Unit) {
    var nowPlaying by remember { mutableStateOf<MockTrack?>(null) }
    val tracks = remember {
        listOf(
            MockTrack("t1", "晨读轻音乐", "Studio A"),
            MockTrack("t2", "专注 · 白噪音", "Focus Lab"),
            MockTrack("t3", "校园广播主题曲", "校园之声"),
            MockTrack("t4", "放松钢琴曲", "Piano Day"),
        )
    }

    val current = nowPlaying
    if (current != null) {
        ReportMainTabRoot(isRoot = false)
        NowPlayingScreen(
            track = current,
            onBack = { nowPlaying = null },
        )
        return
    }

    ReportMainTabRoot(isRoot = false)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DemoColors.PageBg),
    ) {
        MineTopBar(title = "音乐", onBack = onBack, containerColor = DemoColors.PageBg)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(
                    text = "点选曲目进入正在播放（Android 真实 MediaPlayer；其他端见 gap registry）",
                    color = DemoColors.TextSecondary,
                    fontSize = 13.sp,
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            items(tracks, key = { it.id }) { track ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            MusicSession.start(track.title, track.artist)
                            nowPlaying = track
                        },
                    colors = CardDefaults.cardColors(containerColor = DemoColors.Background),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = track.title,
                            color = DemoColors.TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = track.artist,
                            color = DemoColors.Muted,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NowPlayingScreen(
    track: MockTrack,
    onBack: () -> Unit,
) {
    val player = MusicSession.player

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DemoColors.PageBg),
    ) {
        MineTopBar(title = "正在播放", onBack = onBack, containerColor = DemoColors.PageBg)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = track.title,
                color = DemoColors.TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = track.artist,
                color = DemoColors.TextSecondary,
                fontSize = 14.sp,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = if (player.isPlaying) "播放中（Stub）" else "已暂停",
                color = DemoColors.Muted,
                fontSize = 13.sp,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = {
                    if (player.isPlaying) player.pause() else player.play()
                }) {
                    Text(
                        text = if (player.isPlaying) "暂停" else "播放",
                        color = DemoColors.Primary,
                    )
                }
                TextButton(onClick = {
                    MusicSession.dismiss()
                    onBack()
                }) {
                    Text(text = "停止", color = DemoColors.TextSecondary)
                }
            }
        }
    }
}

/** Dismissible mini-player shown on Home when [MusicSession] is active. */
@Composable
internal fun MiniPlayerBar(
    modifier: Modifier = Modifier,
    onOpenNowPlaying: (() -> Unit)? = null,
) {
    val title = MusicSession.trackTitle ?: return
    val player = MusicSession.player

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DemoColors.Background),
    ) {
        HorizontalDivider(color = DemoColors.Divider)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = onOpenNowPlaying != null) {
                    onOpenNowPlaying?.invoke()
                }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = DemoColors.TextPrimary,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    maxLines = 1,
                )
                Text(
                    text = MusicSession.artist,
                    color = DemoColors.Muted,
                    fontSize = 12.sp,
                    maxLines = 1,
                )
            }
            TextButton(onClick = {
                if (player.isPlaying) player.pause() else player.play()
            }) {
                Text(
                    text = if (player.isPlaying) "暂停" else "播放",
                    color = DemoColors.Primary,
                    fontSize = 13.sp,
                )
            }
            TextButton(onClick = { MusicSession.dismiss() }) {
                Text(text = "关闭", color = DemoColors.TextSecondary, fontSize = 13.sp)
            }
        }
    }
}

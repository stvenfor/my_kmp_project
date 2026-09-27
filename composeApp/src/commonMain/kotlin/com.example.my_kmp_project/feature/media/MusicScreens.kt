package com.example.my_kmp_project.feature.media

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.MineTopBar
import com.example.my_kmp_project.core.ui.ReportMainTabRoot
import kotlin.random.Random

private data class MockTrack(
    val id: String,
    val title: String,
    val artist: String,
)

/** Flutter audio list SoT titles (module_music). */
private val FlutterTracks = listOf(
    MockTrack("t1", "Ya Ali - DJMaza.Com", "DJMaza"),
    MockTrack("t2", "Ek Do Teen - DJMaza.Info", "DJMaza"),
    MockTrack("t3", "16 yeh dil diwana hai", "Classic"),
    MockTrack("t4", "Shape of You", "Ed Sheeran"),
    MockTrack("t5", "Blinding Lights", "The Weeknd"),
    MockTrack("t6", "Levitating", "Dua Lipa"),
)

@Composable
internal fun MusicListScreen(
    onBack: () -> Unit,
    onOpenNowPlaying: (() -> Unit)? = null,
) {
    val tracks = remember { FlutterTracks }
    val sessionActive = MusicSession.isActive
    val miniInset = if (sessionActive) 64.dp else 0.dp

    fun playAt(index: Int) {
        val track = tracks[index]
        MusicSession.start(track.title, track.artist)
        onOpenNowPlaying?.invoke()
    }

    ReportMainTabRoot(isRoot = false)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DemoColors.PageBg),
    ) {
        Column(Modifier.fillMaxSize()) {
            MineTopBar(
                title = "音频列表",
                onBack = onBack,
                containerColor = Color.White,
                actions = {
                    if (sessionActive && onOpenNowPlaying != null) {
                        TextButton(onClick = onOpenNowPlaying) {
                            Text("Now Playing", color = DemoColors.Primary, fontSize = 14.sp)
                        }
                    }
                },
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .padding(bottom = miniInset),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(tracks, key = { it.id }) { track ->
                    val index = tracks.indexOf(track)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { playAt(index) },
                        colors = CardDefaults.cardColors(containerColor = DemoColors.Background),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = track.title,
                                color = DemoColors.TextPrimary,
                                fontWeight = FontWeight.Medium,
                                fontSize = 15.sp,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "By ${track.artist}",
                                color = DemoColors.Muted,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 16.dp, bottom = miniInset + 16.dp)
                .size(56.dp)
                .clip(CircleShape)
                .background(DemoColors.Primary)
                .clickable {
                    playAt(Random.nextInt(tracks.size))
                },
            contentAlignment = Alignment.Center,
        ) {
            Text("🔀", fontSize = 22.sp)
        }

        if (sessionActive) {
            MiniPlayerBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding(),
                onOpenNowPlaying = onOpenNowPlaying,
            )
        }
    }
}

/** Deep-link `/music/now_playing` — Flutter Now Playing. */
@Composable
internal fun MusicNowPlayingRoute(onBack: () -> Unit) {
    val fallback = FlutterTracks.first()
    if (MusicSession.trackTitle == null) {
        MusicSession.start(fallback.title, fallback.artist)
    }
    val title = MusicSession.trackTitle ?: fallback.title
    val artist = MusicSession.artist.ifBlank { fallback.artist }
    ReportMainTabRoot(isRoot = false)
    NowPlayingScreen(
        track = MockTrack("np", title, artist),
        onBack = onBack,
    )
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
        MineTopBar(title = "Now Playing", onBack = onBack, containerColor = Color.White)
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

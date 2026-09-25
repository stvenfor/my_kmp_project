package com.example.my_kmp_project.feature.content

import androidx.compose.runtime.Composable
import com.example.my_kmp_project.feature.ai.AiRoutes
import com.example.my_kmp_project.feature.ai.AiStreamScreen
import com.example.my_kmp_project.feature.classroom.ClassroomRouteHost
import com.example.my_kmp_project.feature.classroom.ClassroomRoutes
import com.example.my_kmp_project.feature.friend.FriendScreen
import com.example.my_kmp_project.feature.live.LiveScreen
import com.example.my_kmp_project.feature.media.MusicListScreen
import com.example.my_kmp_project.feature.media.MusicNowPlayingRoute
import com.example.my_kmp_project.feature.media.VideoRouteHost
import com.example.my_kmp_project.feature.media.VideoRoutes
import com.example.my_kmp_project.feature.media.MediaEntryScreen

/** Cross-module content routes (Video / Classroom / Live / Friend / Music / AI). */
internal object ContentRoutes {
    const val Live = "/live"
    const val LiveRoom = "/live/room"
    const val Friend = "/friend"
    const val Music = "/music/list"
    const val MusicNowPlaying = "/music/now_playing"
    const val MediaEntry = "/media/entry"

    fun fromLabel(label: String): String? {
        VideoRoutes.fromLabel(label)?.let { return it }
        ClassroomRoutes.fromLabel(label)?.let { return it }
        AiRoutes.fromLabel(label)?.let { return it }
        return when (label.trim()) {
            "直播", "直播间" -> Live
            "好友", "通讯录", "朋友" -> Friend
            "音乐", "歌单", "音频列表" -> Music
            "音视频" -> MediaEntry
            else -> null
        }
    }
}

@Composable
internal fun ContentRouteHost(
    route: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit = {},
) {
    when {
        route == ContentRoutes.Live || route == ContentRoutes.LiveRoom ->
            LiveScreen(onBack = onBack, openRoom = route == ContentRoutes.LiveRoom)
        route == ContentRoutes.Friend -> FriendScreen(onBack = onBack)
        route == ContentRoutes.Music ->
            MusicListScreen(
                onBack = onBack,
                onOpenNowPlaying = { onNavigate(ContentRoutes.MusicNowPlaying) },
            )
        route == ContentRoutes.MusicNowPlaying ->
            MusicNowPlayingRoute(onBack = onBack)
        route == ContentRoutes.MediaEntry -> MediaEntryScreen(onBack = onBack)
        route == AiRoutes.Stream -> AiStreamScreen(onBack = onBack)
        route.startsWith("/classroom") -> ClassroomRouteHost(route, onBack, onNavigate)
        route.startsWith("/video") -> VideoRouteHost(route, onBack, onNavigate)
        else -> MediaEntryScreen(onBack = onBack)
    }
}

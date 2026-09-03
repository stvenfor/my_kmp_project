package com.example.my_kmp_project.feature.live

internal data class LiveRoomItem(
    val id: String,
    val title: String,
    val host: String,
)

/** Mock live rooms for list/entry parity (no push / realtime SDK). */
internal object LiveMockData {
    val rooms: List<LiveRoomItem> = listOf(
        LiveRoomItem(
            id = "mock_room_001",
            title = "晚间答疑直播",
            host = "主播 · 小智",
        ),
        LiveRoomItem(
            id = "mock_room_002",
            title = "口语陪练公开课",
            host = "主播 · 阿语",
        ),
        LiveRoomItem(
            id = "mock_room_003",
            title = "周末分享会",
            host = "主播 · Demo",
        ),
    )
}

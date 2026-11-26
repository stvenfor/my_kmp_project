package com.example.my_kmp_project.feature.community

import kotlin.math.max
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * 社区信息流的 UI 行类型：由 [com.example.my_kmp_project.feature.community.http.HttpCommunityPostRepository]
 * 返回的 HTTP 契约模型适配而来（见 `CommunityScreen.kt` 的 `PostModel.toFeedItem`），
 * 仅用于渲染，不再持有任何进程内假数据。
 */
data class CommunityFeedComment(
    val id: String,
    val postId: String,
    val nickname: String,
    val avatar: String,
    val content: String,
    val createAtMs: Long,
    val replyToNickname: String? = null,
)

data class CommunityFeedItem(
    val id: String,
    val userId: String,
    val nickname: String,
    val avatar: String,
    val content: String,
    val publishAtMs: Long,
    val source: String,
    val images: List<String>,
    val videoUrl: String? = null,
    val videoCoverUrl: String? = null,
    val likeCount: Int,
    val commentCount: Int,
    val isLiked: Boolean,
    val isMine: Boolean = false,
    val previewComments: List<CommunityFeedComment> = emptyList(),
)

data class CommunityUserHit(
    val userId: String,
    val nickname: String,
    val avatar: String,
    val isFollowed: Boolean = false,
)

data class CommunityTopicHit(
    val name: String,
    val heat: Int,
)

data class CommunitySearchAllResult(
    val q: String,
    val posts: List<CommunityFeedItem>,
    val topics: List<CommunityTopicHit>,
    val users: List<CommunityUserHit>,
)

@OptIn(ExperimentalTime::class)
private fun nowMs(): Long = Clock.System.now().toEpochMilliseconds()

/** 动态发布时间的展示文案；服务端只给时间戳，展示口径留在客户端。 */
fun formatPublishTime(atMs: Long, now: Long = nowMs()): String {
    val diffMin = max(0, (now - atMs) / 60_000L)
    return when {
        diffMin < 1 -> "刚刚"
        diffMin < 60 -> "${diffMin}分钟前"
        diffMin < 60 * 24 -> "${diffMin / 60}小时前"
        diffMin < 60 * 48 -> "昨天"
        else -> {
            val days = diffMin / (60 * 24)
            if (days < 30) "${days}天前" else "更早"
        }
    }
}

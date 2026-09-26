package com.example.my_kmp_project.feature.community

import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
private fun nowMs(): Long = Clock.System.now().toEpochMilliseconds()

internal data class CommunityFeedComment(
    val id: String,
    val postId: String,
    val nickname: String,
    val avatar: String,
    val content: String,
    val createAtMs: Long,
    val replyToNickname: String? = null,
)

internal data class CommunityFeedItem(
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

/**
 * Flutter `MockPostRepository` port — seed (Random 42 / 35 posts), tabs
 * (`latest`/`hot`/`following`), toggleLike, createPost, follow set.
 */
internal class MockCommunityEngine {
    private val posts = mutableListOf<CommunityFeedItem>()
    private val commentsByPost = mutableMapOf<String, MutableList<CommunityFeedComment>>()
    private val followedUserIds = mutableSetOf<String>()
    private val listeners = mutableListOf<() -> Unit>()
    private var seeded = false
    private var commentSeq = 0

    fun observe(listener: () -> Unit): () -> Unit {
        listeners += listener
        return { listeners.remove(listener) }
    }

    private fun notifyListeners() {
        listeners.toList().forEach { it() }
    }

    fun posts(
        tab: String,
        page: Int = 0,
        pageSize: Int = 10,
    ): List<CommunityFeedItem> {
        ensureSeed()
        var source = posts.toList()
        when (tab) {
            "hot", "热门" -> {
                source = source.sortedWith(
                    compareByDescending<CommunityFeedItem> { it.likeCount * 2 + it.commentCount }
                        .thenByDescending { it.publishAtMs },
                )
            }
            "following", "关注" -> {
                source = source.filter { followedUserIds.contains(it.userId) }
            }
            else -> {
                source = source.sortedByDescending { it.publishAtMs }
            }
        }
        val start = page * pageSize
        if (start >= source.size) return emptyList()
        val end = min(start + pageSize, source.size)
        return source.subList(start, end)
    }

    fun toggleLike(postId: String, liked: Boolean): CommunityFeedItem? {
        ensureSeed()
        val index = posts.indexOfFirst { it.id == postId }
        if (index < 0) return null
        val post = posts[index]
        val updated = post.copy(
            isLiked = liked,
            likeCount = max(0, post.likeCount + if (liked) 1 else -1),
        )
        posts[index] = updated
        notifyListeners()
        return updated
    }

    fun followUser(userId: String) {
        followedUserIds += userId
        notifyListeners()
    }

    fun unfollowUser(userId: String) {
        followedUserIds -= userId
        notifyListeners()
    }

    fun isFollowed(userId: String): Boolean = followedUserIds.contains(userId)

    fun createPost(
        content: String,
        mediaType: String = "none",
        source: String = "来自 Android",
    ): CommunityFeedItem {
        ensureSeed()
        val body = content.trim()
        val id = "post_new_${nowMs()}"
        val isImage = mediaType == "image"
        val isVideo = mediaType == "video"
        val post = CommunityFeedItem(
            id = id,
            userId = "me",
            nickname = "我",
            avatar = avatarUrl(12),
            content = body,
            publishAtMs = nowMs(),
            source = source,
            images = if (isImage) {
                listOf(
                    "https://picsum.photos/seed/wys_post_1/400/400",
                    "https://picsum.photos/seed/wys_post_2/400/400",
                    "https://picsum.photos/seed/wys_post_3/400/400",
                )
            } else {
                emptyList()
            },
            videoUrl = if (isVideo) {
                "https://flutter.github.io/assets-for-api-docs/assets/videos/bee.mp4"
            } else {
                null
            },
            videoCoverUrl = if (isVideo) {
                "https://picsum.photos/seed/wys_post_video/640/360"
            } else {
                null
            },
            likeCount = 0,
            commentCount = 0,
            isLiked = false,
            isMine = true,
        )
        posts.add(0, post)
        commentsByPost[id] = mutableListOf()
        notifyListeners()
        return post
    }

    fun ingestPublishedBody(body: String?, source: String = "来自 Android") {
        if (body.isNullOrBlank()) return
        ensureSeed()
        if (posts.any { it.isMine && it.content == body }) return
        createPost(body, source = source)
    }

    fun comments(postId: String): List<CommunityFeedComment> {
        ensureSeed()
        return commentsByPost[postId].orEmpty().toList()
    }

    fun metaLabel(post: CommunityFeedItem): String =
        "${formatPublishTime(post.publishAtMs)} · ${post.source}"

    companion object {
        private val sampleContents = listOf(
            "今天去了 @张三 推荐的咖啡店，环境不错。\n#Flutter开发\n欢迎访问：https://flutter.dev",
            "周末 hiking，天气太好了！#户外",
            "刚读完一本好书，推荐 @李四 也看看。",
            "分享一张随手拍～",
            "项目上线啦，感谢团队！#Flutter开发 https://dart.dev",
            "午餐打卡 @王五",
            "学习 GetX 状态管理中…",
        )

        private val nicknames = listOf(
            "张三", "李四", "王五", "赵六", "小明", "小红", "开发者", "产品经理",
        )

        fun avatarUrl(seed: Int): String = "https://i.pravatar.cc/200?img=$seed"

        fun formatPublishTime(atMs: Long, now: Long = nowMs()): String {
            val diffMin = max(0, (now - atMs) / 60_000L)
            return when {
                diffMin < 1 -> "刚刚"
                diffMin < 60 -> "${diffMin}分钟前"
                diffMin < 60 * 24 -> "${diffMin / 60}小时前"
                diffMin < 60 * 48 -> "昨天"
                else -> {
                    // month/day from epoch — good enough for mock labels
                    val days = diffMin / (60 * 24)
                    if (days < 30) "${days}天前" else "更早"
                }
            }
        }
    }

    private fun ensureSeed() {
        if (seeded) return
        seeded = true
        val now = nowMs()
        val random = Random(42)
        for (i in 0 until 35) {
            val id = "post_$i"
            val isVideo = i % 10 == 0
            val imgCount = if (isVideo) 0 else (i % 9) + 1
            val images = if (isVideo) {
                emptyList()
            } else {
                List(imgCount) { j -> "https://picsum.photos/seed/${id}_$j/400/400" }
            }
            val preview = seedComments(id, now, i)
            val post = CommunityFeedItem(
                id = id,
                userId = "user_${i % 8}",
                nickname = nicknames[i % nicknames.size],
                avatar = avatarUrl((i % 70) + 1),
                content = sampleContents[i % sampleContents.size],
                publishAtMs = now - (i * 17L + random.nextInt(30)) * 60_000L,
                source = if (i % 2 == 0) "来自 iPhone" else "来自 Android",
                images = images,
                videoUrl = if (isVideo) {
                    "https://flutter.github.io/assets-for-api-docs/assets/videos/bee.mp4"
                } else {
                    null
                },
                videoCoverUrl = if (isVideo) {
                    "https://picsum.photos/seed/video_$i/640/360"
                } else {
                    null
                },
                likeCount = random.nextInt(200),
                commentCount = 2 + random.nextInt(8),
                isLiked = i % 4 == 0,
                isMine = i == 0,
                previewComments = preview,
            )
            posts += post
            commentsByPost[id] = preview.toMutableList()
        }
    }

    private fun seedComments(postId: String, now: Long, index: Int): List<CommunityFeedComment> =
        listOf(
            CommunityFeedComment(
                id = "c_${postId}_1",
                postId = postId,
                nickname = nicknames[(index + 1) % nicknames.size],
                avatar = avatarUrl((index + 2) % 70 + 1),
                content = "说得对！",
                createAtMs = now - (index * 5L + 3) * 60_000L,
            ),
            CommunityFeedComment(
                id = "c_${postId}_2",
                postId = postId,
                nickname = nicknames[(index + 3) % nicknames.size],
                avatar = avatarUrl((index + 4) % 70 + 1),
                content = "同感 +1",
                createAtMs = now - (index * 5L + 1) * 60_000L,
                replyToNickname = nicknames[index % nicknames.size],
            ),
        )
}

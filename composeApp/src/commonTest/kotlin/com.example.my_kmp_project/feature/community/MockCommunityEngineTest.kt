package com.example.my_kmp_project.feature.community

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Logic Acceptance Packet — Community mock engine vs Flutter MockPostRepository depth.
 */
class MockCommunityEngineTest {

    @Test
    fun seed_has_thirty_five_posts_for_latest_tab() {
        val engine = MockCommunityEngine()
        val page0 = engine.posts(tab = "latest", page = 0, pageSize = 50)
        assertEquals(35, page0.size)
    }

    @Test
    fun search_posts_matches_content() {
        val engine = MockCommunityEngine()
        val hits = engine.searchPosts("Flutter")
        assertTrue(hits.isNotEmpty())
        assertTrue(hits.all { it.content.contains("Flutter", ignoreCase = true) })
    }

    @Test
    fun search_users_and_topics() {
        val engine = MockCommunityEngine()
        val users = engine.searchUsers("张")
        assertTrue(users.any { it.nickname.contains("张") })
        val topics = engine.searchTopics("户外")
        assertEquals(1, topics.size)
        assertEquals("户外", topics.first().name)
    }

    @Test
    fun add_comment_increments_count() {
        val engine = MockCommunityEngine()
        val postId = engine.defaultPostId()
        val before = engine.posts("latest", pageSize = 1).first { it.id == postId }.commentCount
        val added = engine.addComment(postId, "逻辑验收评论")
        assertNotNull(added)
        assertEquals("我", added.nickname)
        val after = engine.comments(postId)
        assertTrue(after.any { it.content == "逻辑验收评论" })
        val post = engine.posts("latest", pageSize = 50).first { it.id == postId }
        assertEquals(before + 1, post.commentCount)
    }

    @Test
    fun create_post_and_toggle_like() {
        val engine = MockCommunityEngine()
        val created = engine.createPost("logic-first publish")
        assertTrue(created.isMine)
        val liked = engine.toggleLike(created.id, liked = true)
        assertNotNull(liked)
        assertTrue(liked.isLiked)
        assertEquals(1, liked.likeCount)
    }

    @Test
    fun follow_user_fills_following_tab_and_search_flag() {
        val engine = MockCommunityEngine()
        val user = engine.searchUsers("").first()
        assertTrue(engine.posts(tab = "following").isEmpty())
        engine.followUser(user.userId)
        assertTrue(engine.isFollowed(user.userId))
        assertTrue(engine.posts(tab = "following").isNotEmpty())
        assertTrue(engine.searchUsers(user.nickname).any { it.userId == user.userId && it.isFollowed })
        engine.unfollowUser(user.userId)
        assertTrue(engine.posts(tab = "following").isEmpty())
    }

    @Test
    fun create_post_with_media_type() {
        val engine = MockCommunityEngine()
        val image = engine.createPost("pic", mediaType = "image")
        assertTrue(image.images.isNotEmpty())
        val video = engine.createPost("vid", mediaType = "video")
        assertNotNull(video.videoUrl)
    }
}

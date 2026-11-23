package com.example.my_kmp_project.core.router

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CommentRoutesTest {
    @Test
    fun legacyPostIdMapsToPost() {
        val target = CommentRoutes.parse("/community/comment?postId=abc-123")
        assertEquals(CommentRouteTarget("post", "abc-123"), target)
    }

    @Test
    fun genericTargetsKeepTheirType() {
        assertEquals(
            CommentRouteTarget("post", "p1"),
            CommentRoutes.parse("/community/comment?targetType=post&targetId=p1"),
        )
        assertEquals(
            CommentRouteTarget("video", "v1"),
            CommentRoutes.parse("/community/comment?targetType=video&targetId=v1"),
        )
        assertEquals(
            CommentRouteTarget("article", "a1"),
            CommentRoutes.parse("/community/comment?target_type=article&target_id=a1"),
        )
    }

    @Test
    fun explicitTargetWinsOverLegacyPostId() {
        assertEquals(
            CommentRouteTarget("video", "v1"),
            CommentRoutes.parse("/community/comment?postId=p1&targetType=video&targetId=v1"),
        )
    }

    @Test
    fun unknownOrIncompleteTargetsAreInvalid() {
        assertNull(CommentRoutes.parse("/community/comment"))
        assertNull(CommentRoutes.parse("/community/comment?postId="))
        assertNull(CommentRoutes.parse("/community/comment?targetType=video"))
        assertNull(CommentRoutes.parse("/community/comment?targetId=v1"))
        assertNull(CommentRoutes.parse("/community/comment?targetType=short_video&targetId=v1"))
        assertNull(CommentRoutes.parse("/community/comment?targetType=video&targetId= "))
        assertNull(CommentRoutes.parse("/community/other?postId=p1"))
    }

    @Test
    fun routeDetectionIsScopedToCommentPath() {
        assertTrue(CommentRoutes.isCommentRoute(CommunityRoutes.Comment))
        assertTrue(CommentRoutes.isCommentRoute("/community/comment?postId=p1"))
        assertTrue(CommentRoutes.isCommentRoute("/community/comment/extra"))
        assertFalse(CommentRoutes.isCommentRoute("/community/commentary"))
        assertFalse(CommentRoutes.isCommentRoute("/community/publish"))
    }

    @Test
    fun buildersRoundTrip() {
        assertEquals(
            CommentRouteTarget("video", "v1"),
            CommentRoutes.parse(CommentRoutes.target("Video", "v1")),
        )
        assertEquals(
            CommentRouteTarget("post", "p1"),
            CommentRoutes.parse(CommentRoutes.post("p1")),
        )
    }
}

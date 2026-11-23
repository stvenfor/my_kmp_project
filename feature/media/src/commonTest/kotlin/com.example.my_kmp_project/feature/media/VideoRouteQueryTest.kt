package com.example.my_kmp_project.feature.media

import com.example.my_kmp_project.core.router.VideoRoutes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * 播放页入口契约：带 query 的 `/video/short/play?videoId=<uuid>` 必须命中播放页，
 * 且 ID 只从 query 取；缺失 / 空白一律 null（由播放页展示不可用，不回落演示片源）。
 */
class VideoRouteQueryTest {

    @Test
    fun query_bearing_route_dispatches_to_short_play() {
        assertEquals(
            VideoRoutes.ShortPlay,
            videoRoutePath("/video/short/play?videoId=33333333-3333-4333-8333-333333333333"),
        )
        assertEquals(VideoRoutes.ShortPlay, videoRoutePath(VideoRoutes.ShortPlay))
        assertEquals(VideoRoutes.Short, videoRoutePath("/video/short?tab=new"))
    }

    @Test
    fun video_id_is_read_from_query_only() {
        val id = "33333333-3333-4333-8333-333333333333"
        assertEquals(id, shortPlayVideoId("/video/short/play?videoId=$id"))
        assertEquals(id, shortPlayVideoId("/video/short/play?from=feed&videoId=$id&x=1"))
        assertEquals("a=b", shortPlayVideoId("/video/short/play?videoId=a=b"))
    }

    @Test
    fun missing_or_blank_video_id_is_null_without_demo_fallback() {
        assertNull(shortPlayVideoId(VideoRoutes.ShortPlay))
        assertNull(shortPlayVideoId("/video/short/play?videoId="))
        assertNull(shortPlayVideoId("/video/short/play?videoId=   "))
        assertNull(shortPlayVideoId("/video/short/play?other=1"))
        assertNull(shortPlayVideoId("/video/short/play?video_id=s1"))
    }
}

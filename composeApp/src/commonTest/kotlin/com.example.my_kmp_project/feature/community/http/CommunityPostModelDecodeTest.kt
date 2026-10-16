package com.example.my_kmp_project.feature.community.http

import com.example.my_kmp_project.core.network.NetworkJson
import com.example.my_kmp_project.core.network.decodeEnvelopeData
import com.example.my_kmp_project.core.network.decodeListPage
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * §2.12 #59–#71 的响应体解码契约：直接吃真实 BFF 信封 `{"code":0,…,"data":…}`，
 * 断言 Dart 各 `fromJson` 的反直觉语义 ——
 * `post_model.dart:26-77`、`comment_model.dart:12-23`、`topic_model.dart:9-16`、
 * `community_search_models.dart:45-52`（my_ai_project d7876a1）。
 */
class CommunityPostModelDecodeTest {

    private val json = NetworkJson

    /** 取信封里的 `data`（与 `ApiEnvelopeParser` 同一条解码路径）。 */
    private fun dataOf(envelope: String): JsonObject =
        json.parseToJsonElement(envelope).jsonObject["data"]!!.jsonObject

    private inline fun <reified T> modelOf(body: String): T {
        val decoded = decodeEnvelopeData<T>(json.parseToJsonElement(body))
        return assertNotNull(decoded, "解码失败：$body")
    }

    private val postsPageEnvelope = """
        {
          "code": 0,
          "message": "ok",
          "timestamp": 1792139400,
          "data": {
            "list": [
              {
                "id": 1001,
                "user_id": "u_9",
                "nickname": "张三",
                "avatar": "https://i.pravatar.cc/200?img=3",
                "content": "周末看车 #新车跟进",
                "publish_time": "2026-10-16T08:30:00Z",
                "source": "来自 iPhone",
                "images": ["https://cdn/a.jpg", "", "https://cdn/b.jpg"],
                "video_url": null,
                "video_cover_url": null,
                "like_count": 12,
                "comment_count": 2,
                "is_liked": true,
                "is_mine": false,
                "preview_comments": [
                  {
                    "id": "c1",
                    "post_id": "1001",
                    "nickname": "李四",
                    "avatar": "https://i.pravatar.cc/200?img=4",
                    "content": "说得对",
                    "create_time": "2026-10-16T09:00:00+08:00"
                  },
                  {"junk": true}
                ],
                "media_type": "image",
                "is_ask_everyone": false,
                "topic": {"id": "t1", "name": "新车跟进", "heat": 12800, "is_ask_everyone": true}
              },
              {
                "id": "2002",
                "user_id": "u_1",
                "nickname": "王五",
                "avatar": "",
                "content": "提问：预算 15 万买什么？",
                "publish_time": "bad-timestamp",
                "image_urls": ["https://cdn/c.jpg"],
                "video_url": "https://cdn/v.mp4",
                "video_cover_url": "https://cdn/v.jpg",
                "like_count": 0,
                "comment_count": 0,
                "is_liked": false,
                "is_mine": true,
                "preview_comments": null,
                "media_type": "video",
                "is_ask_everyone": true
              }
            ],
            "pagination": {"page": 1, "size": 10, "total": 35, "totalPages": 4}
          }
        }
    """.trimIndent()

    @Test
    fun posts_page_decodes_full_dart_field_set() {
        // Dart `ResultModel.listPage(json)` 吃的是整个 `data`（list + pagination），不是 list 本身
        val page = decodeListPage<PostModel>(dataOf(postsPageEnvelope))
        assertEquals(2, page.items.size)

        val first = page.items[0]
        // Dart `json['id']?.toString()`（post_model.dart:41）——数字 id 也读成字符串
        assertEquals("1001", first.id)
        assertEquals("u_9", first.userId)
        assertEquals("张三", first.nickname)
        assertEquals("周末看车 #新车跟进", first.content)
        // publish_time 字符串 → 时间戳（:46-48）
        assertEquals(1792139400000L, first.publishTimeMs(nowMs = 0L))
        assertEquals("来自 iPhone", first.source)
        // images 过滤空串（:34-36）
        assertEquals(listOf("https://cdn/a.jpg", "https://cdn/b.jpg"), first.images)
        assertTrue(first.hasImages)
        assertFalse(first.hasVideo)
        assertNull(first.videoUrl)
        assertNull(first.videoCoverUrl)
        assertEquals(12, first.likeCount)
        assertEquals(2, first.commentCount)
        assertTrue(first.isLiked)
        assertFalse(first.isMine)
        assertEquals("image", first.mediaType)
        // 话题兜底：顶层 is_ask_everyone=false、topic.is_ask_everyone=true → true（:59-60）
        assertTrue(first.isAskEveryone)
        assertEquals("t1", first.topic?.id)
        assertEquals("新车跟进", first.topic?.name)
        assertEquals(12800, first.topic?.heat)
        assertEquals("1.3万", first.topic?.heatLabel)
        assertEquals("#新车跟进", first.topic?.displayName)
        // preview_comments：Dart whereType<Map>，缺字段的 Map 走 '' 兜底（:79-85）
        assertEquals(2, first.previewComments.size)
        assertEquals("c1", first.previewComments[0].id)
        assertEquals(1792112400000L, first.previewComments[0].createTimeMs(nowMs = 0L))
        assertNull(first.previewComments[0].replyToNickname)
        assertEquals("", first.previewComments[1].content)

        val second = page.items[1]
        assertEquals("2002", second.id)
        // images 缺键 → 用 image_urls（:33）
        assertEquals(listOf("https://cdn/c.jpg"), second.images)
        assertTrue(second.hasVideo)
        assertTrue(second.isMine)
        assertTrue(second.isAskEveryone)
        assertNull(second.topic)
        // 非法 publish_time → 回落「现在」（:46-48）
        assertEquals(777L, second.publishTimeMs(nowMs = 777L))
        // preview_comments 显式 null → 空表
        assertTrue(second.previewComments.isEmpty())

        // A 型分页方言：totalPages 优先（result_model.dart:74-82）
        assertEquals(4, page.pagination?.totalPages)
        assertTrue(page.hasMore(pageSize = 10))
    }

    @Test
    fun bare_array_and_items_keys_still_decode() {
        // 契约表 §3 A 型：data 可能是裸数组或 items
        val bare = decodeListPage<PostModel>(
            json.parseToJsonElement("""[{"id":1,"content":"周末户外","like_count":3}]"""),
        )
        assertEquals("1", bare.items[0].id)
        assertEquals(3, bare.items[0].likeCount)
        assertEquals(0, bare.items[0].commentCount)
        assertNull(bare.pagination)
        // 无 pagination → 「拿满即还有下一页」（result_model.dart:129-135）
        assertTrue(bare.hasMore(pageSize = 1))
        assertFalse(bare.hasMore(pageSize = 2))

        val items = decodeListPage<PostModel>(
            json.parseToJsonElement(
                """{"items":[{"id":"x"}],"pagination":{"page":1,"size":10,"total":10}}""",
            ),
        )
        assertEquals("x", items.items[0].id)
        // 无 totalPages → page*size < total（1*10 < 10 为 false）
        assertFalse(items.hasMore(pageSize = 10))
    }

    @Test
    fun images_key_beats_image_urls_even_when_explicit_null() {
        // Dart `json['images'] ?? json['image_urls']`：images 为 JSON null 时继续回落（:33）
        val fallback = modelOf<PostModel>("""{"id":"p","images":null,"image_urls":["https://cdn/z.jpg"]}""")
        assertEquals(listOf("https://cdn/z.jpg"), fallback.images)

        val both = modelOf<PostModel>(
            """{"id":"p","images":["https://cdn/a.jpg"],"image_urls":["https://cdn/b.jpg"]}""",
        )
        assertEquals(listOf("https://cdn/a.jpg"), both.images)
    }

    @Test
    fun source_default_only_applies_when_key_is_absent_or_null() {
        // Dart `json['source']?.toString() ?? '来自 iPhone'`（:49）：空串仍是空串
        assertEquals("来自 iPhone", modelOf<PostModel>("""{"id":"p"}""").source)
        assertEquals("来自 iPhone", modelOf<PostModel>("""{"id":"p","source":null}""").source)
        assertEquals("", modelOf<PostModel>("""{"id":"p","source":""}""").source)
    }

    @Test
    fun like_fragment_keeps_from_like_fragment_defaults() {
        // Dart `PostModel.fromLikeFragment`（post_model.dart:66-77）：只吃 id / like_count / is_liked
        val fragment = postModelFromLikeFragment(
            json.parseToJsonElement("""{"id": 55, "like_count": 8, "is_liked": true}"""),
        )
        assertNotNull(fragment)
        assertEquals("55", fragment.id)
        assertEquals(8, fragment.likeCount)
        assertTrue(fragment.isLiked)
        assertEquals("", fragment.userId)
        assertEquals("", fragment.nickname)
        assertEquals("", fragment.avatar)
        assertEquals("", fragment.content)
        assertEquals(777L, fragment.publishTimeMs(nowMs = 777L))
        assertEquals("来自 iPhone", fragment.source)
        assertEquals(0, fragment.commentCount)
        assertFalse(fragment.isMine)
        assertTrue(fragment.previewComments.isEmpty())
        assertNull(fragment.mediaType)
        assertFalse(fragment.isAskEveryone)
        assertNull(fragment.topic)

        // unlike 片段：`is_liked` 严格 == true 判定（:55）
        val off = postModelFromLikeFragment(
            json.parseToJsonElement("""{"id":"56","like_count":0,"is_liked":false}"""),
        )
        assertNotNull(off)
        assertEquals("56", off.id)
        assertFalse(off.isLiked)
        assertEquals(0, off.likeCount)
    }

    @Test
    fun comments_page_fixture_dart_field_set() {
        val page = decodeListPage<CommentModel>(
            json.parseToJsonElement(
                """
                {
                  "list": [
                    {"id": 7, "post_id": "1001", "nickname": "李四", "avatar": "a.png",
                     "content": "同感 +1", "create_time": "2026-10-16T08:30:00.123+08:00",
                     "reply_to_nickname": "张三"},
                    {"id": null, "post_id": null, "nickname": null, "avatar": null,
                     "content": null, "create_time": null, "reply_to_nickname": null}
                  ],
                  "pagination": {"page": 1, "size": 50, "total": 2}
                }
                """.trimIndent(),
            ),
        )
        assertEquals(2, page.items.size)
        val first = page.items[0]
        assertEquals("7", first.id)
        assertEquals("1001", first.postId)
        assertEquals("张三", first.replyToNickname)
        assertEquals(1792110600123L, first.createTimeMs(nowMs = 0L))

        val second = page.items[1]
        // 全 null 也不丢行：Dart `?.toString() ?? ''`（comment_model.dart:14-18）
        assertEquals("", second.id)
        assertEquals("", second.postId)
        assertEquals("", second.nickname)
        assertEquals("", second.content)
        assertNull(second.replyToNickname)
        assertEquals(42L, second.createTimeMs(nowMs = 42L))
        // 有 pagination 时按 page*size<total 判定（1*50 < 2 → false）
        assertFalse(page.hasMore(pageSize = 50))
    }

    @Test
    fun topics_fixture_tolerates_missing_heat_and_bool() {
        val page = decodeListPage<TopicModel>(
            json.parseToJsonElement(
                """
                {"list":[{"id":3,"name":"户外","heat":"860"},
                         {"id":"t9","name":"学习打卡","is_ask_everyone":true},
                         {"name":"缺 id"}],
                 "pagination":{"page":2,"size":20,"total":60,"totalPages":3}}
                """.trimIndent(),
            ),
        )
        assertEquals(3, page.items.size)
        assertEquals("3", page.items[0].id)
        assertEquals(860, page.items[0].heat)
        assertEquals("860", page.items[0].heatLabel)
        assertEquals(0, page.items[1].heat)
        assertTrue(page.items[1].isAskEveryone)
        assertEquals("", page.items[2].id)
        assertFalse(page.items[2].isAskEveryone)
        assertTrue(page.hasMore(pageSize = 20))
    }

    @Test
    fun search_all_fixture_splits_three_own_pages() {
        // 契约表 #68：data = {q, posts, topics, users}，三个子对象各自带 list/items + pagination
        val result: CommunitySearchAllResult = modelOf(
            """
            {
              "q": "户外",
              "posts": {"list":[{"id":1,"content":"周末户外","like_count":5}],
                         "pagination":{"page":1,"size":5,"total":12,"totalPages":3}},
              "topics": {"items":[{"id":"t2","name":"户外","heat":860}]},
              "users": {"list":[{"user_id":88,"nickname":"小明","avatar":"a.png","is_followed":true},
                                {"user_id":"89","nickname":"小红"}]}
            }
            """.trimIndent(),
        )
        assertEquals("户外", result.q)
        assertEquals(1, result.posts.items.size)
        assertEquals("1", result.posts.items[0].id)
        assertEquals(3, result.posts.pagination?.totalPages)
        assertTrue(result.posts.hasMore(pageSize = 5))
        assertEquals("t2", result.topics.items[0].id)
        assertNull(result.topics.pagination)
        assertEquals(2, result.users.items.size)
        assertEquals("88", result.users.items[0].userId)
        assertTrue(result.users.items[0].isFollowed)
        assertFalse(result.users.items[1].isFollowed)

        val empty = modelOf<CommunitySearchAllResult>("""{}""")
        assertEquals("", empty.q)
        assertTrue(empty.posts.items.isEmpty())
        assertTrue(empty.topics.items.isEmpty())
        assertTrue(empty.users.items.isEmpty())
    }

    @Test
    fun user_hit_fixture_dart_defaults() {
        val page = decodeListPage<CommunityUserHit>(
            json.parseToJsonElement(
                """[{"user_id":null,"nickname":null,"avatar":null,"is_followed":false},
                    {"user_id":"u2","nickname":"阿飞","avatar":"f.png","is_followed":true}]""",
            ),
        )
        assertEquals("", page.items[0].userId)
        assertFalse(page.items[0].isFollowed)
        assertEquals("u2", page.items[1].userId)
        assertTrue(page.items[1].isFollowed)
    }

    @Test
    fun publish_time_shapes_match_dart_try_parse_subset() {
        assertEquals(1792108800000L, parseCommunityTimeMs("2026-10-16"))
        assertEquals(1792108800000L, parseCommunityTimeMs("2026-10-16T00:00:00Z"))
        assertEquals(1792108800000L, parseCommunityTimeMs("2026-10-16T00:00:00"))
        assertEquals(1792108800000L, parseCommunityTimeMs("2026-10-16 00:00:00Z"))
        assertEquals(1767323045000L, parseCommunityTimeMs("2026-01-02T03:04:05Z"))
        assertEquals(1792110600123L, parseCommunityTimeMs("2026-10-16T08:30:00.123+08:00"))
        assertEquals(1792110600123L, parseCommunityTimeMs("2026-10-16T08:30:00.123+0800"))
        assertEquals(1709227800000L, parseCommunityTimeMs("2024-02-29T12:00:00-05:30"))
        assertEquals(0L, parseCommunityTimeMs("1970-01-01T00:00:00Z"))
        // 空 / 非法 / 越界 → null（调用方回落 now，与 Dart `tryParse ?? now()` 一致）
        assertNull(parseCommunityTimeMs(null))
        assertNull(parseCommunityTimeMs(""))
        assertNull(parseCommunityTimeMs("bad-timestamp"))
        assertNull(parseCommunityTimeMs("2026-13-01T00:00:00Z"))
        assertNull(parseCommunityTimeMs("2026-10-16T25:00:00Z"))
    }
}

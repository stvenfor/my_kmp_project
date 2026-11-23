package com.example.my_kmp_project.feature.media

import com.example.my_kmp_project.core.network.ApiClient
import com.example.my_kmp_project.core.network.ApiEnvelopeParser
import com.example.my_kmp_project.core.network.ApiResponse
import com.example.my_kmp_project.core.network.HttpTextResponse
import com.example.my_kmp_project.core.network.NetworkError
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 短视频 HTTP 契约（#72–#78）fixture 测试：真实 BFF 信封 `{"code":0,…}` 直接解码，
 * 逐条对齐 Dart `http_short_video_repository.dart` / `short_video_models.dart`（d7876a1）。
 */
class ShortVideoHttpRepositoryTest {

    // ── fake ApiClient：按 "VERB path" 存夹具、记录调用 ──────────────────────────

    private data class Call(
        val verb: String,
        val path: String,
        val query: Map<String, String>,
        val body: String?,
    )

    private class FakeApiClient : ApiClient {
        val calls = mutableListOf<Call>()
        private val fixtures = mutableMapOf<String, String>()
        var transportError: Throwable? = null

        fun stub(verb: String, path: String, raw: String) {
            fixtures["$verb $path"] = raw
        }

        private fun take(verb: String, path: String, query: Map<String, String>, body: String?): String {
            transportError?.let { throw it }
            calls += Call(verb, path, query, body)
            return fixtures["$verb $path"] ?: """{"code":-1,"message":"missing fixture"}"""
        }

        override suspend fun getRaw(path: String, query: Map<String, String>): String =
            take("GET", path, query, null)

        override suspend fun getAbsoluteRaw(url: String): String = error("unused in short-video")

        override suspend fun postRaw(path: String, body: String): String =
            take("POST", path, emptyMap(), body)

        override suspend fun postForm(path: String, fields: Map<String, String>): HttpTextResponse =
            error("unused in short-video")

        override suspend fun putRaw(path: String, body: String): String = error("PUT 不在 /api/v1 契约内")

        override suspend fun patchRaw(path: String, body: String): String = error("PATCH 不在短视频契约内")

        override suspend fun deleteRaw(path: String): String =
            take("DELETE", path, emptyMap(), null)

        override suspend fun <T> getApi(
            path: String,
            query: Map<String, String>,
            parseData: (JsonElement?) -> T?,
        ): ApiResponse<T> = ApiEnvelopeParser.parse(getRaw(path, query), parseData)

        override suspend fun <T> postApi(
            path: String,
            body: String,
            parseData: (JsonElement?) -> T?,
        ): ApiResponse<T> = ApiEnvelopeParser.parse(postRaw(path, body), parseData)

        override suspend fun <T> putApi(
            path: String,
            body: String,
            parseData: (JsonElement?) -> T?,
        ): ApiResponse<T> = error("PUT 不在 /api/v1 契约内")

        override suspend fun <T> patchApi(
            path: String,
            body: String,
            parseData: (JsonElement?) -> T?,
        ): ApiResponse<T> = error("PATCH 不在短视频契约内")

        override suspend fun <T> deleteApi(
            path: String,
            query: Map<String, String>,
            parseData: (JsonElement?) -> T?,
        ): ApiResponse<T> =
            ApiEnvelopeParser.parse(
                deleteRaw(path),
                parseData,
                httpStatus = null,
            )
    }

    private fun envelope(data: String): String =
        """{"code":0,"message":"ok","data":$data,"timestamp":1729000000}"""

    private fun repoWith(vararg fixtures: Pair<String, String>): Pair<HttpShortVideoRepository, FakeApiClient> {
        val fake = FakeApiClient()
        fixtures.forEach { (key, raw) ->
            val (verb, path) = key.split(" ", limit = 2)
            fake.stub(verb, path, raw)
        }
        return HttpShortVideoRepository(fake) to fake
    }

    // ── #72 profile ──────────────────────────────────────────────────────────────

    @Test
    fun profile_stats_are_numbers_on_wire_stringified_for_ui() = runBlocking {
        val (repo, fake) = repoWith(
            "GET /api/v1/short-videos/profile" to envelope(
                """{"user_id":42,"display_name":"qa_user","avatar_url":null,"role_badge":"销售顾问",
                    "store_name":"[4S]北京沃德龙鼎吉利","is_me":true,
                    "stats":{"video_count":5,"view_count":1203,"like_count":394}}""",
            ),
        )
        val profile = repo.fetchProfile(userId = "42")
        // Dart :184 `?.toString()` —— 数字 user_id 转字符串
        assertEquals("42", profile.userId)
        assertEquals("qa_user", profile.displayName)
        assertNull(profile.avatarUrl)
        assertEquals("销售顾问", profile.roleBadge)
        assertTrue(profile.isMe)
        // stats 数字 → 字符串（short_video_models.dart:149-154,167）
        assertEquals(ShortVideoStatsModel("5", "1203", "394"), profile.stats)
        assertEquals(mapOf("user_id" to "42"), fake.calls.single().query)
    }

    @Test
    fun profile_omits_blank_user_id_query_and_defaults_stats() = runBlocking {
        val (repo, fake) = repoWith(
            "GET /api/v1/short-videos/profile" to envelope(
                """{"display_name":"","stats":"not-a-map"}""",
            ),
        )
        val profile = repo.fetchProfile(userId = "")
        // Dart :17 —— null/空串都不带 user_id 键
        assertTrue(fake.calls.single().query.isEmpty())
        // is_me 缺省 → false（Dart 工厂 `json['is_me'] == true` :189）
        assertFalse(profile.isMe)
        // stats 非 map → empty '0'（:190-192）
        assertEquals(ShortVideoStatsModel.EMPTY, profile.stats)
    }

    @Test
    fun profile_failure_uses_message_then_chinese_fallback() = runBlocking {
        val (repoWithMsg, _) = repoWith(
            "GET /api/v1/short-videos/profile" to """{"code":1,"message":"未登录","data":null}""",
        )
        val err = runCatching { repoWithMsg.fetchProfile() }.exceptionOrNull()
        assertIs<NetworkError.Business>(err)
        assertEquals("未登录", err.message)
        assertEquals(1, err.code)

        val (repoBlank, _) = repoWith(
            "GET /api/v1/short-videos/profile" to """{"code":1,"message":"","data":null}""",
        )
        val err2 = runCatching { repoBlank.fetchProfile() }.exceptionOrNull()
        assertIs<NetworkError.Business>(err2)
        assertEquals("加载资料失败", err2.message)
    }

    // ── #73 list ─────────────────────────────────────────────────────────────────

    private val itemFixture = """
        {"id":7,"title":"周末活动花絮","cover_url":"https://cdn/c.jpg","video_url":null,
         "view_count":120,"like_count":13,"duration":45,"status":"reviewing",
         "is_liked":true,"is_mine":false,"user_id":"42","nickname":"qa_user",
         "avatar":"https://cdn/a.png","topic":{"id":3,"name":"口语"},
         "publish_time":"2026-10-15T20:54:00Z"}
    """.trimIndent()

    @Test
    fun list_query_sends_scope_one_based_page_and_size_5_with_full_item_mapping() = runBlocking {
        val (repo, fake) = repoWith(
            "GET /api/v1/short-videos" to envelope(
                """{"list":[$itemFixture],"pagination":{"page":1,"size":5,"total":7}}""",
            ),
        )
        val page = repo.fetchList(scope = "recommend", userId = "42", page = 0)
        // Dart :38-43：scope 总发、本地 0-based +1、size=5、user_id 带上
        assertEquals(
            mapOf("scope" to "recommend", "page" to "1", "size" to "5", "user_id" to "42"),
            fake.calls.single().query,
        )
        val item = page.list.single()
        assertEquals("7", item.id) // 数字 id → toString（Dart :45）
        assertEquals("周末活动花絮", item.title)
        assertNull(item.videoUrl)
        assertEquals(120, item.viewCount)
        assertEquals(13, item.likeCount)
        assertEquals("45", item.duration) // 数字 duration → "45"（Dart :51 ?.toString()）
        assertEquals(1.25, item.aspectRatio) // aspect_ratio 缺省 1.25（Dart :52）
        assertEquals(ShortVideoStatus.Reviewing, item.status)
        assertTrue(item.isLiked)
        assertFalse(item.isMine)
        assertEquals(ShortVideoTopicBrief("3", "口语"), item.topic)
        assertEquals("#口语", item.topic?.displayName)
        assertEquals("2026-10-15T20:54:00Z", item.publishTime)
        // pagination.hasMore：1*5 < 7 → true
        assertTrue(page.hasMore)
    }

    @Test
    fun list_hasMore_falls_back_to_full_page_without_pagination() = runBlocking {
        fun items(count: Int): String = (1..count).joinToString(",") { """{"id":$it}""" }
        val (repoFull, _) = repoWith(
            "GET /api/v1/short-videos" to envelope("""{"list":[${items(5)}]}"""),
        )
        assertTrue(repoFull.fetchList(scope = "mine").hasMore) // 拿满 size=5 → 还有下一页

        val (repoShort, fake) = repoWith(
            "GET /api/v1/short-videos" to envelope("""{"list":[${items(4)}]}"""),
        )
        assertFalse(repoShort.fetchList(scope = "mine", page = 2).hasMore)
        // page=2（本地）→ 服务端 3；不带 user_id 键（Dart :42）
        assertEquals(
            mapOf("scope" to "mine", "page" to "3", "size" to "5"),
            fake.calls.single().query,
        )
    }

    @Test
    fun list_pagination_has_more_false_wins_over_full_page_size() = runBlocking {
        val (repo, _) = repoWith(
            "GET /api/v1/short-videos" to envelope(
                """{"list":[${(1..5).joinToString(",") { """{"id":$it}""" }}],
                    "pagination":{"page":2,"size":5,"total":7}}""",
            ),
        )
        // pagination.hasMore（2*5 >= 7）优先于「拿满即有下一页」
        assertFalse(repo.fetchList(scope = "recommend").hasMore)
    }

    // ── #74 create ───────────────────────────────────────────────────────────────

    @Test
    fun create_sends_explicit_null_upload_fields_and_decodes_item() = runBlocking {
        val (repo, fake) = repoWith(
            "POST /api/v1/short-videos" to envelope(itemFixture),
        )
        val created = repo.create(title = "新作品", topicId = "3")
        val call = fake.calls.single()
        assertEquals("POST", call.verb)
        assertEquals("/api/v1/short-videos", call.path)
        // Dart :65-72：六键总发，上传字段显式 null（NetworkJsonExplicitNulls）
        val body = Json.parseToJsonElement(call.body!!).jsonObject
        assertEquals("新作品", body["title"]?.toString()?.trim('"'))
        for (key in listOf("video_url", "cover_url", "duration", "aspect_ratio", "topic_id")) {
            assertTrue(body.containsKey(key), "缺少显式键 $key")
        }
        assertEquals(JsonNull, body["video_url"])
        assertEquals(JsonNull, body["cover_url"])
        assertEquals(JsonNull, body["duration"])
        assertEquals(JsonNull, body["aspect_ratio"])
        assertEquals("\"3\"", body["topic_id"].toString())
        assertEquals("7", created.id)
        assertEquals(1.25, created.aspectRatio)
    }

    @Test
    fun create_with_no_topic_still_sends_topic_id_null() = runBlocking {
        val (repo, fake) = repoWith(
            "POST /api/v1/short-videos" to envelope("""{"id":"8","title":"t"}"""),
        )
        repo.create(title = "t")
        val body = Json.parseToJsonElement(fake.calls.single().body!!).jsonObject
        assertEquals(JsonNull, body["topic_id"])
    }

    // ── #75 delete ───────────────────────────────────────────────────────────────

    @Test
    fun delete_success_accepts_missing_or_empty_data_map() = runBlocking {
        val (repoNoData, fake) = repoWith(
            "DELETE /api/v1/short-videos/7" to """{"code":0,"message":"ok","timestamp":1729000000}""",
        )
        repoNoData.delete("7")
        assertEquals(Call("DELETE", "/api/v1/short-videos/7", emptyMap(), null), fake.calls.single())

        val (repoEmptyMap, _) = repoWith(
            "DELETE /api/v1/short-videos/7" to envelope("{}"),
        )
        repoEmptyMap.delete("7") // `_dataAllowEmpty`（Dart :92,159-169）只看 code
    }

    @Test
    fun delete_failure_throws_business_error() = runBlocking {
        val (repo, _) = repoWith(
            "DELETE /api/v1/short-videos/7" to """{"code":3,"message":"","data":null}""",
        )
        val err = assertIs<NetworkError.Business>(runCatching { repo.delete("7") }.exceptionOrNull())
        assertEquals("删除失败", err.message)
        assertEquals(3, err.code)
    }

    // ── #76/#77 like：同 path 切动词，返回片段就地合并 ────────────────────────────

    @Test
    fun like_posts_and_returns_fragment_without_refetch() = runBlocking {
        val (repo, fake) = repoWith(
            "POST /api/v1/short-videos/9/like" to envelope("""{"id":9,"like_count":13,"is_liked":true}"""),
        )
        val fragment = repo.toggleLike("9", liked = true)
        val call = fake.calls.single()
        assertEquals("POST", call.verb)
        assertEquals("/api/v1/short-videos/9/like", call.path)
        assertEquals("", call.body) // 无 body 的动作 POST 也要真实发出（契约 §5.12）
        // fromLikeFragment（Dart short_video_models.dart:69-76）：只带 id/like_count/is_liked
        assertEquals("9", fragment.id)
        assertEquals(13, fragment.likeCount)
        assertTrue(fragment.isLiked)
        assertNull(fragment.title)
        assertEquals(1.25, fragment.aspectRatio)

        // 就地合并（controller :261-270 copyWith 语义）：其余字段保持
        val existing = ShortVideoItemModel(id = "9", title = "t", viewCount = 120, likeCount = 12, isLiked = false)
        val merged = existing.withCounts(likeCount = fragment.likeCount, isLiked = fragment.isLiked)
        assertEquals(13, merged.likeCount)
        assertTrue(merged.isLiked)
        assertEquals("t", merged.title)
        assertEquals(120, merged.viewCount)
    }

    @Test
    fun unlike_uses_delete_on_same_path() = runBlocking {
        val (repo, fake) = repoWith(
            "DELETE /api/v1/short-videos/9/like" to envelope("""{"id":9,"like_count":12,"is_liked":false}"""),
        )
        val fragment = repo.toggleLike("9", liked = false)
        assertEquals("DELETE", fake.calls.single().verb)
        assertFalse(fragment.isLiked)
        assertEquals(12, fragment.likeCount)
    }

    @Test
    fun like_failure_falls_back_to_caozuo_shibai() = runBlocking {
        val (repo, _) = repoWith(
            "POST /api/v1/short-videos/9/like" to """{"code":5,"message":null,"data":null}""",
        )
        val err = assertIs<NetworkError.Business>(runCatching { repo.toggleLike("9", true) }.exceptionOrNull())
        assertEquals("操作失败", err.message)
    }

    // ── #78 view：fire-and-forget，全部错误吞掉 ──────────────────────────────────

    @Test
    fun view_returns_count_and_swallows_every_failure() = runBlocking {
        val (repo, fake) = repoWith(
            "POST /api/v1/short-videos/9/view" to envelope("""{"view_count":88}"""),
        )
        assertEquals(88, repo.reportView("9"))
        val call = fake.calls.single()
        assertEquals("POST", call.verb)
        assertEquals("", call.body)

        // view_count 缺失但 data 是 map → 0（Dart :133 `?? 0`）
        val (repoNoKey, _) = repoWith(
            "POST /api/v1/short-videos/9/view" to envelope("{}"),
        )
        assertEquals(0, repoNoKey.reportView("9"))

        // code 非 0 → 吞（Dart _data 抛 + controller catch(_) {} :284）
        val (repoBizFail, _) = repoWith(
            "POST /api/v1/short-videos/9/view" to """{"code":1,"message":"x","data":null}""",
        )
        assertNull(repoBizFail.reportView("9"))

        // 成功但 data 缺失 → 吞（Dart `_data` 视 null data 为失败）
        val (repoNoData, _) = repoWith(
            "POST /api/v1/short-videos/9/view" to """{"code":0,"message":"ok","data":null}""",
        )
        assertNull(repoNoData.reportView("9"))

        // 传输层异常 → 吞
        val (repoTransport, transportFake) = repoWith()
        transportFake.transportError = IllegalStateException("socket closed")
        assertNull(repoTransport.reportView("9"))
    }

    // ── C2：/short-videos/:v 与 /like 的四个 verb 显式登记 ────────────────────
    // 矩阵 D 区此前「复用 item()/like()，verb 由调用方隐式给定」的盲区：改为在
    // HttpShortVideoRepository 内为每个动词各给一个显式方法，逐一断言 verb + path。

    @Test
    fun c2ExplicitVerbsForItemAndLikePaths() = runBlocking {
        // GET /short-videos/:v（#67 GET）
        val (getRepo, getFake) = repoWith("GET /api/v1/short-videos/7" to envelope(itemFixture))
        getRepo.get("7")
        assertEquals(Call("GET", "/api/v1/short-videos/7", emptyMap(), null), getFake.calls.single())

        // DELETE /short-videos/:v（#67 DELETE）
        val (delRepo, delFake) = repoWith("DELETE /api/v1/short-videos/7" to envelope("{}"))
        delRepo.delete("7")
        assertEquals(Call("DELETE", "/api/v1/short-videos/7", emptyMap(), null), delFake.calls.single())

        // POST /short-videos/:v/like（#68 POST）
        val (likeRepo, likeFake) = repoWith(
            "POST /api/v1/short-videos/9/like" to envelope("""{"id":9,"like_count":13,"is_liked":true}"""),
        )
        likeRepo.like("9")
        assertEquals(Call("POST", "/api/v1/short-videos/9/like", emptyMap(), ""), likeFake.calls.single())

        // DELETE /short-videos/:v/like（#68 DELETE）
        val (unlikeRepo, unlikeFake) = repoWith(
            "DELETE /api/v1/short-videos/9/like" to envelope("""{"id":9,"like_count":12,"is_liked":false}"""),
        )
        unlikeRepo.unlike("9")
        assertEquals(Call("DELETE", "/api/v1/short-videos/9/like", emptyMap(), null), unlikeFake.calls.single())
    }

    @Test
    fun get_binds_play_page_identity_and_server_video_url() = runBlocking {
        val fixture = """
            {"id":"33333333-3333-4333-8333-333333333333","title":"口语跟读",
             "video_url":"https://cdn.example/v.mp4","like_count":7,"view_count":42,"is_liked":true}
        """.trimIndent()
        val (repo, _) = repoWith(
            "GET /api/v1/short-videos/33333333-3333-4333-8333-333333333333" to envelope(fixture),
        )

        val item = repo.get("33333333-3333-4333-8333-333333333333")

        assertEquals("33333333-3333-4333-8333-333333333333", item.id)
        assertEquals("https://cdn.example/v.mp4", item.videoUrl)
        assertEquals("口语跟读", item.title)
        assertEquals(7, item.likeCount)
        assertTrue(item.isLiked)
    }
}

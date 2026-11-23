package com.example.my_kmp_project.feature.community.http

import com.example.my_kmp_project.core.network.ApiClient
import com.example.my_kmp_project.core.network.ApiEnvelopeParser
import com.example.my_kmp_project.core.network.ApiResponse
import com.example.my_kmp_project.core.network.HttpTextResponse
import com.example.my_kmp_project.core.network.NetworkError
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * §2.12 #59–#71 的**调用形状**契约：verb、path、query、body 与 Dart
 * `features/community/lib/community/repository/http_post_repository.dart`（d7876a1）逐一对齐，
 * 并用真实 BFF 信封 `{"code":0,…}` 作为响应夹具走完整条解码链。
 *
 * 覆盖的反直觉点（契约表 §5.9 / §5.10 / §5.12）：
 * - 点赞 / 关注是**同 path 切 POST/DELETE**；
 * - `POST /community/posts` 的 `video_url`/`video_cover_url` **显式发 null**、`image_urls` 恒发 `[]`；
 * - `GET /community/posts` 的 `page` 是本地页 `+1`；评论页写死 `page=1&size=50`；
 * - `GET /community/search` 同一端点靠 `type` 切响应形状；
 * - `DELETE /community/posts/:id` 与关注/取关的 `data` 可为空 map 或 null（`_dataAllowEmpty`）。
 */
class HttpCommunityPostRepositoryTest {

    /** 记录一次调用（verb + 带前导斜杠的完整 path + query + 原始 body 串）。 */
    private data class Call(
        val verb: String,
        val path: String,
        val query: Map<String, String>,
        val body: String,
    )

    /**
     * 只 fake 传输层：响应仍交给 [ApiEnvelopeParser]，与线上同一套信封解析
     * （缺 `code` → `-1`、`data:null` → parseData 收到 null）。
     */
    private class RecordingApiClient(
        private val responder: (Call) -> String,
    ) : ApiClient {
        val calls = mutableListOf<Call>()

        private fun record(verb: String, path: String, query: Map<String, String>, body: String): Call =
            Call(verb, path, query, body).also { calls += it }

        private fun <T> answer(
            verb: String,
            path: String,
            query: Map<String, String>,
            body: String,
            parseData: (JsonElement?) -> T?,
        ): ApiResponse<T> = ApiEnvelopeParser.parse(
            raw = responder(record(verb, path, query, body)),
            parseData = parseData,
            httpStatus = 200,
        )

        fun last(): Call = calls.last()

        override suspend fun getRaw(path: String, query: Map<String, String>): String =
            error("getRaw 不属于社区契约调用链")

        override suspend fun getAbsoluteRaw(url: String): String =
            error("getAbsoluteRaw 不属于社区契约调用链")

        override suspend fun postRaw(path: String, body: String): String =
            error("postRaw 不属于社区契约调用链")

        override suspend fun postForm(path: String, fields: Map<String, String>): HttpTextResponse =
            error("postForm 不属于社区契约调用链")

        override suspend fun putRaw(path: String, body: String): String =
            error("社区契约表里 PUT 为 0 条（§1）")

        override suspend fun patchRaw(path: String, body: String): String =
            error("社区契约表里 PATCH 为 0 条（§1）")

        override suspend fun deleteRaw(path: String): String =
            error("deleteRaw 不属于社区契约调用链")

        override suspend fun <T> getApi(
            path: String,
            query: Map<String, String>,
            parseData: (JsonElement?) -> T?,
        ): ApiResponse<T> = answer("GET", path, query, "", parseData)

        override suspend fun <T> postApi(
            path: String,
            body: String,
            parseData: (JsonElement?) -> T?,
        ): ApiResponse<T> = answer("POST", path, emptyMap(), body, parseData)

        override suspend fun <T> putApi(
            path: String,
            body: String,
            parseData: (JsonElement?) -> T?,
        ): ApiResponse<T> = error("社区契约表里 PUT 为 0 条（§1）")

        override suspend fun <T> patchApi(
            path: String,
            body: String,
            parseData: (JsonElement?) -> T?,
        ): ApiResponse<T> = error("社区契约表里 PATCH 为 0 条（§1）")

        override suspend fun <T> deleteApi(
            path: String,
            query: Map<String, String>,
            parseData: (JsonElement?) -> T?,
        ): ApiResponse<T> = answer("DELETE", path, query, "", parseData)
    }

    /** 依次吐出 [responses]（多出来的调用重复最后一条）。 */
    private fun repo(vararg responses: String): Pair<HttpCommunityPostRepository, RecordingApiClient> {
        var index = 0
        val client = RecordingApiClient { responses.getOrElse(index++) { responses.last() } }
        return HttpCommunityPostRepository(client) to client
    }

    private val postsPageBody = """
        {"code":0,"message":"ok","data":{"list":[
          {"id":1001,"user_id":"u_9","nickname":"张三","avatar":"a.png","content":"周末看车",
           "publish_time":"2026-10-16T08:30:00Z","source":"来自 iPhone","images":["a.jpg"],
           "video_url":null,"video_cover_url":null,"like_count":12,"comment_count":2,
           "is_liked":true,"is_mine":false,"preview_comments":[],"media_type":"image",
           "is_ask_everyone":false,"topic":{"id":"t1","name":"新车跟进","heat":12800,"is_ask_everyone":true}},
          {"id":"2002","user_id":"u_1","nickname":"王五","avatar":"","content":"提问",
           "publish_time":"2026-10-16T09:00:00Z","image_urls":["c.jpg"],"like_count":0,
           "comment_count":0,"is_liked":false,"is_mine":true,"is_ask_everyone":true}
        ],"pagination":{"page":2,"size":10,"total":35,"totalPages":4}}}
    """.trimIndent()

    /** Dart `POST /community/posts` 成功后回单的**单条动态**（`_data` + `PostModel.fromJson`）。 */
    private val createdPostBody = """
        {"code":0,"message":"ok","data":{"id":1001,"user_id":"u_9","nickname":"张三","avatar":"a.png",
         "content":"首条动态","publish_time":"2026-10-16T08:30:00Z","source":"来自 iPhone",
         "images":[],"video_url":null,"video_cover_url":null,"like_count":0,"comment_count":0,
         "is_liked":false,"is_mine":true,"preview_comments":[],"media_type":"none",
         "is_ask_everyone":false,"topic":null}}
    """.trimIndent()

    /** #59 */
    @Test
    fun fetch_posts_uses_server_side_page_number_size_and_tab() = runBlocking {
        val (repository, client) = repo(postsPageBody)
        val posts = repository.fetchPosts(page = 1, pageSize = 10, tab = "hot")
        val call = client.last()
        assertEquals("GET", call.verb)
        assertEquals("/api/v1/community/posts", call.path)
        // 本地页 1 → 服务端 page=2，size=10，tab 恒发（Dart :24-27）
        assertEquals(mapOf("page" to "2", "size" to "10", "tab" to "hot"), call.query)
        assertEquals(2, posts.size)
        assertEquals("1001", posts[0].id)
        assertEquals(1792139400000L, posts[0].publishTimeMs(nowMs = 0L))
        assertTrue(posts[0].isAskEveryone)
        assertEquals(listOf("c.jpg"), posts[1].images)
    }

    /** #59 默认档：latest / size=10 / page 0-based → 1-based */
    @Test
    fun fetch_posts_defaults_match_dart_latest_tab() = runBlocking {
        val (repository, client) = repo(postsPageBody)
        repository.fetchPosts(page = 0)
        assertEquals(mapOf("page" to "1", "size" to "10", "tab" to "latest"), client.last().query)
    }

    /** #59 分页视图：同一 query 契约，并解码服务端 pagination 元数据。 */
    @Test
    fun fetch_posts_page_keeps_query_contract_and_decodes_pagination() = runBlocking {
        val (repository, client) = repo(postsPageBody)
        val page = repository.fetchPostsPage(page = 1, pageSize = 10, tab = "hot")
        val call = client.last()
        assertEquals("GET", call.verb)
        assertEquals("/api/v1/community/posts", call.path)
        assertEquals(mapOf("page" to "2", "size" to "10", "tab" to "hot"), call.query)
        assertEquals(2, page.items.size)
        assertEquals("1001", page.items[0].id)
        val pagination = page.pagination!!
        assertEquals(2, pagination.page)
        assertEquals(10, pagination.size)
        assertEquals(35, pagination.total)
        assertEquals(4, pagination.totalPages)
        assertTrue(page.hasMore(10))
    }

    /** 列表兼容视图与分页视图同源：items 一致，不重复发明解码逻辑。 */
    @Test
    fun fetch_posts_is_items_view_of_fetch_posts_page() = runBlocking {
        val (repository, _) = repo(postsPageBody)
        val paged = repository.fetchPostsPage(page = 0)
        val listed = repository.fetchPosts(page = 0)
        assertEquals(paged.items.map { it.id }, listed.map { it.id })
    }

    /** #61 */
    @Test
    fun fetch_comments_is_hardcoded_to_page_one_size_fifty() = runBlocking {
        val (repository, client) = repo(
            """{"code":0,"message":"ok","data":{"list":[
              {"id":7,"post_id":"1001","nickname":"李四","avatar":"a.png","content":"说得对",
               "create_time":"2026-10-16T09:00:00+08:00","reply_to_nickname":"张三"}
            ],"pagination":{"page":1,"size":50,"total":1}}}""",
        )
        val comments = repository.fetchComments("1001")
        assertEquals("GET", client.last().verb)
        assertEquals("/api/v1/community/posts/1001/comments", client.last().path)
        assertEquals(mapOf("page" to "1", "size" to "50"), client.last().query)
        assertEquals(1, comments.size)
        assertEquals("7", comments[0].id)
        assertEquals("张三", comments[0].replyToNickname)
        assertEquals(1792112400000L, comments[0].createTimeMs(nowMs = 0L))
    }

    /** #62 —— reply_to_nickname 为 null 时整键省略（Dart :62） */
    @Test
    fun add_comment_body_omits_reply_key_only_when_null() = runBlocking {
        val commentBody = """{"code":0,"message":"ok","data":{"id":"c9","post_id":"1001",
            "nickname":"我","avatar":"me.png","content":"同感","create_time":"2026-10-16T09:00:00Z"}}"""
        val (repository, client) = repo(commentBody)

        val created = repository.addComment(postId = "1001", content = "同感")
        assertEquals("POST", client.last().verb)
        assertEquals("/api/v1/community/posts/1001/comments", client.last().path)
        assertEquals("""{"content":"同感"}""", client.last().body)
        assertEquals("c9", created.id)

        repository.addComment(postId = "1001", content = "回复", replyToNickname = "李四")
        assertEquals(
            """{"content":"回复","reply_to_nickname":"李四"}""",
            client.last().body,
        )
    }

    /** #60 —— 显式 null + 恒发 image_urls:[]（NetworkJsonExplicitNulls） */
    @Test
    fun create_post_sends_explicit_nulls_and_empty_image_urls() = runBlocking {
        val (repository, client) = repo(createdPostBody)
        val created = repository.createPost(content = "首条动态")
        val call = client.last()
        assertEquals("POST", call.verb)
        assertEquals("/api/v1/community/posts", call.path)
        // 键与顺序都照 Dart map 字面量（http_post_repository.dart:122-131）
        assertEquals(
            """{"content":"首条动态","media_type":"none","image_urls":[],"video_url":null,""" +
                """"video_cover_url":null,"topic_id":null,"is_ask_everyone":false,"source":"来自 iPhone"}""",
            call.body,
        )
        assertTrue(call.body.contains("\"video_url\":null"), call.body)
        assertTrue(call.body.contains("\"video_cover_url\":null"), call.body)
        assertTrue(call.body.contains("\"image_urls\":[]"), call.body)
        assertTrue(call.body.contains("\"topic_id\":null"), call.body)
        assertEquals("1001", created.id)
    }

    /** #60 —— 带话题与提问标记时仍发全部键 */
    @Test
    fun create_post_carries_topic_and_ask_everyone_flags() = runBlocking {
        val (repository, client) = repo(createdPostBody)
        repository.createPost(
            content = "提问",
            mediaType = "video",
            topicId = "t1",
            isAskEveryone = true,
            source = "来自 Android",
        )
        val body = client.last().body
        assertTrue(body.contains("\"media_type\":\"video\""), body)
        assertTrue(body.contains("\"topic_id\":\"t1\""), body)
        assertTrue(body.contains("\"is_ask_everyone\":true"), body)
        assertTrue(body.contains("\"source\":\"来自 Android\""), body)
    }

    /** #63 / #64 —— 同 path 切动词（§5.10） */
    @Test
    fun toggle_like_switches_verb_on_the_same_path() = runBlocking {
        val (repository, client) = repo(
            """{"code":0,"message":"ok","data":{"id":1001,"like_count":13,"is_liked":true}}""",
            """{"code":0,"message":"ok","data":{"id":1001,"like_count":12,"is_liked":false}}""",
        )

        val liked = repository.toggleLike(postId = "1001", liked = true)
        assertEquals("POST", client.last().verb)
        assertEquals("/api/v1/community/posts/1001/like", client.last().path)
        // Dart 不发 body，但仍真的发请求（§5.12）
        assertEquals("", client.last().body)
        assertEquals("1001", liked.id)
        assertEquals(13, liked.likeCount)
        assertTrue(liked.isLiked)
        // 片段其余字段 = Dart PostModel.fromLikeFragment 的默认值
        assertEquals("", liked.content)
        assertEquals(0, liked.commentCount)
        assertEquals("来自 iPhone", liked.source)

        val unliked = repository.toggleLike(postId = "1001", liked = false)
        assertEquals("DELETE", client.last().verb)
        assertEquals("/api/v1/community/posts/1001/like", client.last().path)
        assertEquals(12, unliked.likeCount)
        assertTrue(!unliked.isLiked)
    }

    /** #65 —— `_dataAllowEmpty`：空 map / null / 缺 data 都只看 code */
    @Test
    fun delete_post_accepts_empty_or_null_data() = runBlocking {
        for (body in listOf(
            """{"code":0,"message":"ok","data":{}}""",
            """{"code":0,"message":"ok","data":null}""",
            """{"code":0,"message":"ok"}""",
        )) {
            val (repository, client) = repo(body)
            repository.deletePost("1001")
            assertEquals("DELETE", client.last().verb)
            assertEquals("/api/v1/community/posts/1001", client.last().path)
        }
        val (repository, _) = repo("""{"code":500,"message":"服务器内部错误","data":null}""")
        val error = assertFailsWith<NetworkError.Business> { repository.deletePost("1001") }
        assertEquals(500, error.code)
        assertEquals("服务器内部错误", error.message)
    }

    /** #70 / #71 —— 关注/取关同 path 切动词，原始 Map 可空 */
    @Test
    fun follow_and_unfollow_switch_verb_and_tolerate_empty_data() = runBlocking {
        val (repository, client) = repo(
            """{"code":0,"message":"ok","data":{}}""",
            """{"code":0,"message":"ok","data":null}""",
        )
        repository.followUser("u_7")
        assertEquals("POST", client.last().verb)
        assertEquals("/api/v1/community/users/u_7/follow", client.last().path)
        assertEquals("", client.last().body)

        repository.unfollowUser("u_7")
        assertEquals("DELETE", client.last().verb)
        assertEquals("/api/v1/community/users/u_7/follow", client.last().path)

        val (failing, _) = repo("""{"code":403,"message":"无权限","data":{}}""")
        val error = assertFailsWith<NetworkError.Business> { failing.followUser("u_7") }
        assertEquals(403, error.code)
        assertEquals("无权限", error.message)
    }

    /** #66 / #67 —— topics 与 topics/search 是两个不同端点 */
    @Test
    fun topics_and_topics_search_are_different_endpoints() = runBlocking {
        val topicsBody = """{"code":0,"message":"ok","data":{"list":[
            {"id":"t1","name":"新车跟进","heat":12800,"is_ask_everyone":true}
        ],"pagination":{"page":1,"size":20,"total":1}}}"""
        val (repository, client) = repo(topicsBody, topicsBody)

        val topics = repository.fetchTopics()
        assertEquals("GET", client.last().verb)
        assertEquals("/api/v1/community/topics", client.last().path)
        assertEquals(mapOf("page" to "1", "size" to "20"), client.last().query)
        assertEquals(1, topics.size)
        assertTrue(topics[0].isAskEveryone)

        repository.searchTopics("户外", page = 2)
        assertEquals("/api/v1/community/topics/search", client.last().path)
        assertEquals(mapOf("page" to "3", "size" to "20", "q" to "户外"), client.last().query)
    }

    /** #67 —— `q` 为空串也照发（Dart 未走省略规则） */
    @Test
    fun topics_search_sends_empty_query_string() = runBlocking {
        val (repository, client) = repo("""{"code":0,"message":"ok","data":{"list":[]}}""")
        repository.searchTopics("")
        assertEquals("", client.last().query["q"])
    }

    /** #68 —— type=all：一份 body 里三个各自带分页的子对象 */
    @Test
    fun search_all_reads_three_independent_sub_pages() = runBlocking {
        val (repository, client) = repo(
            """{"code":0,"message":"ok","data":{"q":"户外",
              "posts":{"list":[{"id":1,"content":"户外"}],"pagination":{"page":1,"size":5,"total":12,"totalPages":3}},
              "topics":{"items":[{"id":"t2","name":"户外","heat":860}]},
              "users":{"list":[{"user_id":88,"nickname":"小明","is_followed":true}]}}}""",
        )
        val result = repository.searchAll(q = "户外")
        assertEquals("GET", client.last().verb)
        assertEquals("/api/v1/community/search", client.last().path)
        assertEquals(
            mapOf("page" to "1", "size" to "5", "q" to "户外", "type" to "all"),
            client.last().query,
        )
        assertEquals("户外", result.q)
        assertEquals(1, result.posts.items.size)
        assertTrue(result.posts.hasMore(pageSize = 5))
        assertEquals("t2", result.topics.items[0].id)
        assertEquals("88", result.users.items[0].userId)
        assertTrue(result.users.items[0].isFollowed)
    }

    /** #69 —— 同端点靠 type 切成「单个列表页」 */
    @Test
    fun search_by_type_uses_one_endpoint_with_single_page_shape() = runBlocking {
        val postPageBody = """{"code":0,"message":"ok","data":{"list":[
            {"id":1,"content":"户外"},"id2","raw",
            {"id":2,"content":"露营"}
        ],"pagination":{"page":2,"size":10,"total":25,"totalPages":3}}}"""
        val (repository, client) = repo(postPageBody, postPageBody, postPageBody)

        val posts = repository.searchPosts(q = "户外", page = 1)
        assertEquals(
            mapOf("page" to "2", "size" to "10", "q" to "户外", "type" to "post"),
            client.last().query,
        )
        // 非 object 项（"id2" / "raw"）被跳过（Dart whereType<Map>）
        assertEquals(2, posts.items.size)
        assertEquals(listOf("1", "2"), posts.items.map { it.id })
        assertTrue(posts.hasMore(pageSize = 10))

        repository.searchTopicsPage(q = "户外")
        assertEquals(
            mapOf("page" to "1", "size" to "10", "q" to "户外", "type" to "topic"),
            client.last().query,
        )

        repository.searchUsers(q = "小明")
        assertEquals(
            mapOf("page" to "1", "size" to "10", "q" to "小明", "type" to "user"),
            client.last().query,
        )
    }

    /** 契约表 §5.9：`/community/search` 只有 type 在变，path 恒定 */
    @Test
    fun search_all_and_search_by_type_share_one_path() = runBlocking {
        val (repository, client) = repo(
            """{"code":0,"message":"ok","data":{"q":"","posts":{"list":[]},"topics":{"list":[]},"users":{"list":[]}}}""",
            """{"code":0,"message":"ok","data":{"list":[]}}""",
        )
        repository.searchAll(q = "")
        repository.searchPosts(q = "")
        assertTrue(client.calls.all { it.path == "/api/v1/community/search" })
        assertEquals(listOf("all", "post"), client.calls.map { it.query["type"] })
    }

    /** Dart `_list`：`code==0` 但 `data` 缺失/为 null 仍是失败（http_post_repository.dart:184-192） */
    @Test
    fun missing_data_on_success_code_still_throws() = runBlocking {
        val nullData = """{"code":0,"message":"","data":null}"""

        val (feedRepository, _) = repo(nullData)
        val feedError = assertFailsWith<NetworkError.Business> { feedRepository.fetchPosts(page = 0) }
        assertEquals(0, feedError.code)
        assertEquals("加载动态失败", feedError.message)

        val (commentRepository, _) = repo(nullData)
        assertEquals(
            "加载评论失败",
            assertFailsWith<NetworkError.Business> { commentRepository.fetchComments("1001") }.message,
        )

        val (likeRepository, _) = repo(nullData)
        assertEquals(
            "操作失败",
            assertFailsWith<NetworkError.Business> { likeRepository.toggleLike("1001", liked = true) }.message,
        )

        val (publishRepository, _) = repo(nullData)
        assertEquals(
            "发布失败",
            assertFailsWith<NetworkError.Business> { publishRepository.createPost("x") }.message,
        )

        val (topicsRepository, _) = repo(nullData)
        assertEquals(
            "加载话题失败",
            assertFailsWith<NetworkError.Business> { topicsRepository.fetchTopics() }.message,
        )

        val (searchRepository, _) = repo(nullData)
        assertEquals(
            "搜索失败",
            assertFailsWith<NetworkError.Business> { searchRepository.searchAll(q = "x") }.message,
        )
    }

    /** 业务码非 0：优先用服务端文案（Dart `_list`/`_data` 的 message 透传） */
    @Test
    fun business_error_prefers_server_message_and_falls_back_to_chinese_copy() = runBlocking {
        val (repository, _) = repo("""{"code":401,"message":"已退出登录","data":null}""")
        val error = assertFailsWith<NetworkError.Business> { repository.fetchPosts(page = 0) }
        assertEquals(401, error.code)
        assertEquals("已退出登录", error.message)

        val (blank, _) = repo("""{"code":10001,"message":"","data":null}""")
        assertEquals(
            "搜索话题失败",
            assertFailsWith<NetworkError.Business> { blank.searchTopics("户外") }.message,
        )
    }

    /** 缺 `code` 的畸形体不能当成功（`NetworkCodes.PASSTHROUGH = -1`） */
    @Test
    fun envelope_without_code_is_failure_not_success() = runBlocking {
        val (repository, _) = repo("""{"message":"网关返回了怪东西","data":{"list":[]}}""")
        val error = assertFailsWith<NetworkError.Business> { repository.fetchPosts(page = 0) }
        assertEquals(-1, error.code)
        assertEquals("网关返回了怪东西", error.message)
    }

    /** 传输失败（NetworkError.Transport）原样上抛，Dart 同样不吞 */
    @Test
    fun transport_failure_propagates() = runBlocking {
        val client = RecordingApiClient {
            throw NetworkError.Transport("连接失败", httpStatus = 502)
        }
        val repository = HttpCommunityPostRepository(client)
        val error = assertFailsWith<NetworkError.Transport> { repository.fetchPosts(page = 0) }
        assertTrue(error.message.orEmpty().contains("连接失败"))
        assertEquals(502, error.httpStatus)
    }
}

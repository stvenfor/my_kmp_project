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
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 统一评论 HTTP 契约测试：verb / path / query / body 与 Go
 * `app/internal/community/unified_comment_handler.go` + `unified_comment_dto.go` 逐一对齐，
 * 响应夹具走真实的 BFF 信封（[ApiEnvelopeParser]）。
 *
 * 锁定的反直觉点：
 * - `target_type` 只有 `post|video|article`；`cursor` 不透明且原样放进 query map；
 * - `client_msg_id` 由调用方持有，仓库重试**不改写**；
 * - 总数 / 计数是后端 Long，不用 `list.size` 反推；
 * - 墓碑 / 隐藏正文永不下发，能力位缺失 fail closed，非法时间不得回落「现在」；
 * - 409 / 401 / 403 / 503 一律抛强类型失败，不返回伪造成功或空数据。
 */
class HttpUnifiedCommentRepositoryTest {

    private data class Call(
        val verb: String,
        val path: String,
        val query: Map<String, String>,
        val body: String,
    )

    private data class Reply(val status: Int, val body: String)

    /** 只 fake 传输层；响应仍交给共享信封解析器，与线上链路一致。 */
    private class RecordingApiClient(
        private val responder: (Call) -> Reply,
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
        ): ApiResponse<T> {
            val reply = responder(record(verb, path, query, body))
            return ApiEnvelopeParser.parse(
                raw = reply.body,
                parseData = parseData,
                httpStatus = reply.status,
            )
        }

        fun last(): Call = calls.last()

        override suspend fun getRaw(path: String, query: Map<String, String>): String =
            error("getRaw 不属于统一评论契约调用链")

        override suspend fun getAbsoluteRaw(url: String): String =
            error("getAbsoluteRaw 不属于统一评论契约调用链")

        override suspend fun postRaw(path: String, body: String): String =
            error("postRaw 不属于统一评论契约调用链")

        override suspend fun postForm(path: String, fields: Map<String, String>): HttpTextResponse =
            error("postForm 不属于统一评论契约调用链")

        override suspend fun putRaw(path: String, body: String): String =
            error("PUT 不在统一评论端点内")

        override suspend fun patchRaw(path: String, body: String): String =
            error("patchRaw 不属于统一评论契约调用链（审核走 patchApi）")

        override suspend fun deleteRaw(path: String): String =
            error("deleteRaw 不属于统一评论契约调用链")

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
        ): ApiResponse<T> = error("PUT 不在统一评论端点内")

        override suspend fun <T> patchApi(
            path: String,
            body: String,
            parseData: (JsonElement?) -> T?,
        ): ApiResponse<T> = answer("PATCH", path, emptyMap(), body, parseData)

        override suspend fun <T> deleteApi(
            path: String,
            query: Map<String, String>,
            parseData: (JsonElement?) -> T?,
        ): ApiResponse<T> = answer("DELETE", path, query, "", parseData)
    }

    private fun repo(vararg replies: Reply): Pair<HttpUnifiedCommentRepository, RecordingApiClient> {
        var index = 0
        val client = RecordingApiClient { replies.getOrElse(index++) { replies.last() } }
        return HttpUnifiedCommentRepository(client) to client
    }

    private fun ok(body: String): Reply = Reply(status = 200, body = body)

    private fun pageBody(
        list: String,
        pinned: String = "null",
        next: String = "null",
        hasMore: Boolean = false,
        totalVisible: String = "0",
        totalRoots: String = "0",
    ): String = """
        {"code":0,"message":"success","data":{"list":[$list],"pinned":$pinned,
         "cursor":{"next":$next,"has_more":$hasMore},
         "total_visible":$totalVisible,"total_roots":$totalRoots}}
    """.trimIndent()

    private val postId = "11111111-1111-4111-8111-111111111111"
    private val videoId = "33333333-3333-4333-8333-333333333333"
    private val articleId = "44444444-4444-4444-8444-444444444444"
    private val rootId = "22222222-2222-4222-8222-222222222222"

    private val visibleComment = """
        {"id":"c1","subject_id":"s1","target_type":"post","target_id":"$postId",
         "root_id":null,"reply_to_id":null,
         "author":{"user_id":"u1","nickname":"Alice","avatar":"a.png","is_author":true},
         "content":"hello","content_state":"visible","legacy_reply_to_nickname":null,
         "reply_to_author":null,"is_pinned":false,"is_author_reply":false,
         "like_count":3,"reply_count":2,"is_liked":true,
         "can_reply":true,"can_like":true,"can_delete":false,"can_pin":true,"can_moderate":false,
         "created_at":"2026-11-20T12:34:56.123456789Z","updated_at":"2026-11-20T12:34:56.123456789Z"}
    """.trimIndent()

    private val createdComment = """
        {"id":"c-new","subject_id":"s1","target_type":"post","target_id":"$postId",
         "content":"hi","content_state":"visible"}
    """.trimIndent()

    // ---- 读：根列表 / 后代列表 ----

    @Test
    fun fetch_roots_uses_exact_path_query_and_decodes_full_envelope() = runBlocking {
        val pinned = """
            {"id":"p1","content":"pinned","content_state":"visible","is_pinned":true}
        """.trimIndent()
        val (repository, client) = repo(
            ok(pageBody(list = visibleComment, pinned = pinned, next = "\"next-token\"", hasMore = true,
                totalVisible = "9000000000", totalRoots = "42")),
        )

        val page = repository.fetchRoots(
            targetType = UnifiedCommentTargetType.Post,
            targetId = postId,
        )

        assertEquals("GET", client.last().verb)
        assertEquals("/api/v1/comments", client.last().path)
        assertEquals(
            mapOf(
                "target_type" to "post",
                "target_id" to postId,
                "sort" to "latest",
                "limit" to "20",
            ),
            client.last().query,
        )
        assertEquals(1, page.list.size)
        assertEquals("c1", page.list[0].id)
        assertEquals(3L, page.list[0].likeCount)
        assertEquals(2L, page.list[0].replyCount)
        assertTrue(page.list[0].isLiked)
        assertTrue(page.list[0].canPin)
        assertEquals("Alice", page.list[0].author?.nickname)
        assertTrue(page.list[0].author?.isAuthor == true)
        assertEquals("p1", page.pinned?.id)
        assertTrue(page.pinned?.isPinned == true)
        assertEquals("next-token", page.nextCursor)
        assertTrue(page.hasMore)
        assertEquals(9_000_000_000L, page.totalVisible)
        assertEquals(42L, page.totalRoots)
    }

    @Test
    fun fetch_roots_hot_passes_opaque_cursor_verbatim() = runBlocking {
        val (repository, client) = repo(ok(pageBody(list = "")))
        val token = "eyJjIjoiYSBiKy9jPSJ9"

        repository.fetchRoots(
            targetType = UnifiedCommentTargetType.Post,
            targetId = postId,
            sort = UnifiedCommentSort.Hot,
            limit = 50,
            cursor = token,
        )

        assertEquals("hot", client.last().query["sort"])
        assertEquals("50", client.last().query["limit"])
        // token 原样入 query map，由传输层安全转义，仓库不二次编码。
        assertEquals(token, client.last().query["cursor"])
    }

    @Test
    fun fetch_roots_omits_cursor_on_first_page() = runBlocking {
        val (repository, client) = repo(ok(pageBody(list = "")))
        repository.fetchRoots(UnifiedCommentTargetType.Post, postId)
        assertTrue("cursor" !in client.last().query, client.last().query.toString())
    }

    @Test
    fun fetch_replies_uses_replies_path_and_root_totals() = runBlocking {
        val reply = """
            {"id":"r1","root_id":"$rootId","reply_to_id":"$rootId","content_state":"visible",
             "content":"reply","reply_count":0,
             "reply_to_author":{"user_id":"u1","nickname":"Alice","avatar":"a.png","is_author":false}}
        """.trimIndent()
        val (repository, client) = repo(
            ok(pageBody(list = reply, totalVisible = "7", totalRoots = "0")),
        )

        val page = repository.fetchReplies(rootCommentId = rootId, limit = 20, cursor = "reply-cursor")

        assertEquals("GET", client.last().verb)
        assertEquals("/api/v1/comments/$rootId/replies", client.last().path)
        assertEquals(mapOf("limit" to "20", "cursor" to "reply-cursor"), client.last().query)
        assertEquals(1, page.list.size)
        assertEquals(rootId, page.list[0].rootId)
        assertEquals(rootId, page.list[0].replyToId)
        assertEquals("Alice", page.list[0].replyToAuthor?.nickname)
        assertEquals(7L, page.totalVisible)
        assertEquals(0L, page.totalRoots)
        assertNull(page.pinned)
        assertNull(page.nextCursor)
        assertEquals(false, page.hasMore)
    }

    @Test
    fun all_three_target_types_use_canonical_wire_values() = runBlocking {
        val (repository, client) = repo(
            ok(pageBody(list = "")),
            ok(pageBody(list = "")),
            ok(pageBody(list = "")),
        )

        repository.fetchRoots(UnifiedCommentTargetType.Post, postId)
        repository.fetchRoots(UnifiedCommentTargetType.Video, videoId)
        repository.fetchRoots(UnifiedCommentTargetType.Article, articleId)

        assertEquals(
            listOf("post", "video", "article"),
            client.calls.map { it.query["target_type"] },
        )
        assertTrue(client.calls.none { it.query["target_type"] == "short_video" })
    }

    // ---- 写：创建（幂等键 / 201 vs 200 / 409） ----

    @Test
    fun create_comment_sends_exact_body_for_root_and_reply() = runBlocking {
        val envelope = """{"code":0,"data":$createdComment}"""
        val (repository, client) = repo(ok(envelope), ok(envelope))

        repository.createComment(
            targetType = UnifiedCommentTargetType.Post,
            targetId = postId,
            content = "hi",
            clientMsgId = "key-1",
        )
        assertEquals("POST", client.last().verb)
        assertEquals("/api/v1/comments", client.last().path)
        assertEquals(
            """{"target_type":"post","target_id":"$postId","content":"hi","client_msg_id":"key-1"}""",
            client.last().body,
        )

        repository.createComment(
            targetType = UnifiedCommentTargetType.Video,
            targetId = videoId,
            content = "reply",
            clientMsgId = "key-2",
            replyToId = rootId,
        )
        assertEquals(
            """{"target_type":"video","target_id":"$videoId","content":"reply",""" +
                """"reply_to_id":"$rootId","client_msg_id":"key-2"}""",
            client.last().body,
        )
    }

    @Test
    fun create_accepts_both_new_201_and_replay_200() = runBlocking {
        val body = """{"code":0,"message":"ok","data":$createdComment}"""
        val (repository, client) = repo(Reply(201, body), Reply(200, body))

        val created = repository.createComment(
            UnifiedCommentTargetType.Post, postId, "hi", "key-1",
        )
        val replayed = repository.createComment(
            UnifiedCommentTargetType.Post, postId, "hi", "key-1",
        )

        assertEquals("c-new", created.id)
        assertEquals("c-new", replayed.id)
        assertEquals("POST", client.calls[0].verb)
        assertEquals("POST", client.calls[1].verb)
    }

    @Test
    fun create_never_generates_or_changes_client_msg_id_across_retries() = runBlocking {
        val (repository, client) = repo(
            Reply(201, """{"code":0,"data":$createdComment}"""),
            Reply(200, """{"code":0,"data":$createdComment}"""),
        )

        repository.createComment(UnifiedCommentTargetType.Post, postId, "hi", "stable-key")
        repository.createComment(UnifiedCommentTargetType.Post, postId, "hi", "stable-key")

        assertEquals(2, client.calls.size)
        assertEquals(client.calls[0].body, client.calls[1].body)
        assertTrue(client.calls.all { it.body.contains("\"client_msg_id\":\"stable-key\"") })
    }

    @Test
    fun conflict_409_carries_conflict_with_and_safe_copy() = runBlocking {
        val (repository, _) = repo(
            Reply(
                409,
                """{"code":10001,"message":"同一 client_msg_id 对应不同内容","data":{"conflict_with":"c-99"}}""",
            ),
        )

        val error = assertFailsWith<UnifiedCommentError.Conflict> {
            repository.createComment(UnifiedCommentTargetType.Post, postId, "changed", "stable-key")
        }
        assertEquals("c-99", error.conflictWith)
        assertEquals("该评论已提交过，请刷新后查看", error.message)
        assertTrue(!error.message.orEmpty().contains("client_msg_id"))
    }

    // ---- 空页 / 大 Long 计数 ----

    @Test
    fun empty_page_keeps_backend_totals_not_list_size() = runBlocking {
        val (repository, _) = repo(
            ok(pageBody(list = "", totalVisible = "9000000000", totalRoots = "7000000000")),
        )
        val page = repository.fetchRoots(UnifiedCommentTargetType.Post, postId)
        assertTrue(page.list.isEmpty())
        assertNull(page.pinned)
        assertEquals(9_000_000_000L, page.totalVisible)
        assertEquals(7_000_000_000L, page.totalRoots)
    }

    @Test
    fun big_long_counts_are_not_truncated() = runBlocking {
        val huge = """
            {"id":"c1","content":"x","content_state":"visible",
             "like_count":1234567890123,"reply_count":9000000000}
        """.trimIndent()
        val (repository, _) = repo(
            ok(pageBody(list = huge, totalVisible = "1234567890123", totalRoots = "9000000000")),
        )
        val page = repository.fetchRoots(UnifiedCommentTargetType.Post, postId)
        assertEquals(1_234_567_890_123L, page.list[0].likeCount)
        assertEquals(9_000_000_000L, page.list[0].replyCount)
        assertEquals(1_234_567_890_123L, page.totalVisible)
    }

    // ---- 墓碑 / 隐藏 / 能力位 fail closed ----

    @Test
    fun deleted_tombstone_hides_body_and_identity_and_capabilities() = runBlocking {
        val tombstone = """
            {"id":"d1","content":"","content_state":"deleted",
             "author":{"user_id":"","nickname":"","avatar":"","is_author":false},
             "reply_to_author":null,"legacy_reply_to_nickname":null,
             "can_reply":false,"can_like":false,"can_delete":false,"can_pin":false,"can_moderate":false,
             "created_at":"2026-11-20T12:34:56.123456789Z"}
        """.trimIndent()
        val (repository, _) = repo(ok(pageBody(list = tombstone)))
        val comment = repository.fetchRoots(UnifiedCommentTargetType.Post, postId).list.single()

        assertEquals(UnifiedCommentContentState.Deleted, comment.contentState)
        assertEquals("", comment.content)
        assertEquals("", comment.author?.nickname)
        assertTrue(!comment.canReply && !comment.canLike && !comment.canDelete && !comment.canPin)
        assertEquals(1_795_178_096_123L, comment.createdAtMillis())
    }

    @Test
    fun hidden_comment_keeps_author_but_never_body_and_disables_reply() = runBlocking {
        val hidden = """
            {"id":"h1","content":"","content_state":"hidden",
             "author":{"user_id":"u1","nickname":"Alice","avatar":"a.png","is_author":false},
             "can_reply":false,"can_like":false,"can_delete":true,"can_pin":false}
        """.trimIndent()
        val (repository, _) = repo(ok(pageBody(list = hidden)))
        val comment = repository.fetchRoots(UnifiedCommentTargetType.Post, postId).list.single()

        assertEquals(UnifiedCommentContentState.Hidden, comment.contentState)
        assertEquals("", comment.content)
        assertEquals("Alice", comment.author?.nickname)
        assertTrue(!comment.canReply)
        assertTrue(!comment.canLike)
        assertTrue(comment.canDelete)
    }

    @Test
    fun unknown_content_state_fails_closed() = runBlocking {
        val weird = """{"id":"w1","content":"secret","content_state":"mystery"}"""
        val (repository, _) = repo(ok(pageBody(list = weird)))
        val comment = repository.fetchRoots(UnifiedCommentTargetType.Post, postId).list.single()

        assertEquals(UnifiedCommentContentState.Unknown, comment.contentState)
        assertEquals("", comment.content)
    }

    @Test
    fun missing_capabilities_and_counts_default_to_false_and_zero() = runBlocking {
        val sparse = """{"id":"m1"}"""
        val (repository, _) = repo(ok(pageBody(list = sparse)))
        val comment = repository.fetchRoots(UnifiedCommentTargetType.Post, postId).list.single()

        assertTrue(!comment.canReply && !comment.canLike && !comment.canDelete)
        assertTrue(!comment.canPin && !comment.canModerate)
        assertTrue(!comment.isPinned && !comment.isLiked)
        assertTrue(comment.author?.isAuthor != true)
        assertEquals(0L, comment.likeCount)
        assertEquals(0L, comment.replyCount)
        assertEquals(UnifiedCommentContentState.Unknown, comment.contentState)
    }

    @Test
    fun optional_null_fields_decode_as_null() = runBlocking {
        val (repository, _) = repo(
            ok(
                pageBody(
                    list = """{"id":"c1","content_state":"visible","content":"x"}""",
                    next = "null",
                ),
            ),
        )
        val page = repository.fetchRoots(UnifiedCommentTargetType.Post, postId)
        val comment = page.list.single()
        assertNull(comment.rootId)
        assertNull(comment.replyToId)
        assertNull(comment.author)
        assertNull(comment.replyToAuthor)
        assertNull(comment.legacyReplyToNickname)
        assertNull(comment.createdAtMillis())
        assertNull(comment.updatedAtMillis())
        assertNull(page.pinned)
        assertNull(page.nextCursor)
    }

    @Test
    fun invalid_dates_stay_null_instead_of_now() = runBlocking {
        val bad = """{"id":"c1","content_state":"visible","content":"x","created_at":"not-a-date"}"""
        val (repository, _) = repo(ok(pageBody(list = bad)))
        val comment = repository.fetchRoots(UnifiedCommentTargetType.Post, postId).list.single()
        assertNull(comment.createdAtMillis())
    }

    // ---- 变更：like / delete / pin / moderation ----

    @Test
    fun like_and_unlike_switch_verb_on_the_same_path() = runBlocking {
        val (repository, client) = repo(
            ok("""{"code":0,"data":{"id":"c1","like_count":9000000000,"is_liked":true}}"""),
            ok("""{"code":0,"data":{"id":"c1","like_count":8999999999,"is_liked":false}}"""),
        )

        val liked = repository.setLiked("c1", liked = true)
        assertEquals("POST", client.last().verb)
        assertEquals("/api/v1/comments/c1/like", client.last().path)
        assertEquals("", client.last().body)
        assertEquals(9_000_000_000L, liked.likeCount)
        assertTrue(liked.isLiked)

        val unliked = repository.setLiked("c1", liked = false)
        assertEquals("DELETE", client.last().verb)
        assertEquals("/api/v1/comments/c1/like", client.last().path)
        assertEquals(8_999_999_999L, unliked.likeCount)
        assertTrue(!unliked.isLiked)
    }

    @Test
    fun delete_comment_uses_delete_and_decodes_tombstone_status() = runBlocking {
        val (repository, client) = repo(ok("""{"code":0,"data":{"id":"c1","status":"deleted"}}"""))
        val status = repository.deleteComment("c1")
        assertEquals("DELETE", client.last().verb)
        assertEquals("/api/v1/comments/c1", client.last().path)
        assertEquals("c1", status.id)
        assertEquals("deleted", status.status)
    }

    @Test
    fun pin_and_unpin_switch_verb_on_pin_path() = runBlocking {
        val (repository, client) = repo(
            ok("""{"code":0,"data":{"id":"c1","is_pinned":true}}"""),
            ok("""{"code":0,"data":{"id":"c1","is_pinned":false}}"""),
        )

        val pinned = repository.setPinned("c1", pinned = true)
        assertEquals("POST", client.last().verb)
        assertEquals("/api/v1/comments/c1/pin", client.last().path)
        assertTrue(pinned.isPinned)

        val unpinned = repository.setPinned("c1", pinned = false)
        assertEquals("DELETE", client.last().verb)
        assertEquals("/api/v1/comments/c1/pin", client.last().path)
        assertTrue(!unpinned.isPinned)
    }

    @Test
    fun moderation_patches_action_and_optional_reason() = runBlocking {
        val (repository, client) = repo(
            ok("""{"code":0,"data":{"id":"c1","status":"hidden"}}"""),
            ok("""{"code":0,"data":{"id":"c1","status":"visible"}}"""),
        )

        val hidden = repository.moderate("c1", UnifiedCommentModerationAction.Hide)
        assertEquals("PATCH", client.last().verb)
        assertEquals("/api/v1/comments/c1/moderation", client.last().path)
        assertEquals("""{"action":"hide"}""", client.last().body)
        assertEquals("hidden", hidden.status)

        repository.moderate("c1", UnifiedCommentModerationAction.Unhide, reason = "policy ok")
        assertEquals("""{"action":"unhide","reason":"policy ok"}""", client.last().body)
    }

    // ---- 强类型失败：不伪造成功 ----

    @Test
    fun unauthorized_forbidden_and_not_enabled_are_typed_with_safe_copy() = runBlocking {
        val (unauthenticated, _) = repo(
            Reply(401, """{"code":10002,"message":"未授权","data":{}}"""),
        )
        val unauthorized = assertFailsWith<UnifiedCommentError.Unauthorized> {
            unauthenticated.setLiked("c1", liked = true)
        }
        assertEquals(10002, unauthorized.code)
        assertTrue(!unauthorized.message.orEmpty().contains("未授权"))

        val (forbidden, _) = repo(
            Reply(403, """{"code":10003,"message":"无权限","data":{}}"""),
        )
        assertFailsWith<UnifiedCommentError.Forbidden> { forbidden.deleteComment("c1") }

        val (notEnabled, _) = repo(
            Reply(503, """{"code":10005,"message":"统一评论尚未启用","data":{}}"""),
        )
        val disabled = assertFailsWith<UnifiedCommentError.NotEnabled> {
            notEnabled.createComment(UnifiedCommentTargetType.Post, postId, "hi", "k")
        }
        assertEquals(10005, disabled.code)
        assertTrue(!disabled.message.orEmpty().contains("尚未启用"))
    }

    @Test
    fun failed_writes_never_become_fake_success_or_data() = runBlocking {
        val (invalid, _) = repo(ok("""{"code":10001,"message":"参数错误","data":null}"""))
        assertFailsWith<UnifiedCommentError.Invalid> {
            invalid.createComment(UnifiedCommentTargetType.Post, postId, "", "k")
        }

        val (nullData, _) = repo(ok("""{"code":0,"message":"","data":null}"""))
        val failure = assertFailsWith<UnifiedCommentError.Failure> {
            nullData.createComment(UnifiedCommentTargetType.Post, postId, "hi", "k")
        }
        assertEquals(0, failure.serverCode)

        val (likeNull, _) = repo(ok("""{"code":0,"data":null}"""))
        assertFailsWith<UnifiedCommentError.Failure> { likeNull.setLiked("c1", liked = true) }
        Unit
    }

    @Test
    fun transport_failure_is_typed_and_keeps_cause() = runBlocking {
        val client = RecordingApiClient {
            throw NetworkError.Transport("socket exploded", httpStatus = 502)
        }
        val repository = HttpUnifiedCommentRepository(client)

        val error = assertFailsWith<UnifiedCommentError.Unavailable> {
            repository.fetchRoots(UnifiedCommentTargetType.Post, postId)
        }
        assertEquals("连接评论服务失败，请稍后重试", error.message)
        assertTrue(error.cause is NetworkError.Transport)
    }

    @Test
    fun mislabeled_non_2xx_with_code_zero_is_still_failure() = runBlocking {
        // 网关把 5xx 误标成 code=0：HTTP 状态优先，不能当成功（读 / 写都要拦）。
        val (write, _) = repo(Reply(500, """{"code":0,"data":$createdComment}"""))
        assertFailsWith<UnifiedCommentError.Failure> {
            write.createComment(UnifiedCommentTargetType.Post, postId, "hi", "k")
        }

        val (read, _) = repo(Reply(502, pageBody(list = visibleComment)))
        assertFailsWith<UnifiedCommentError.Failure> {
            read.fetchRoots(UnifiedCommentTargetType.Post, postId)
        }
        Unit
    }

    @Test
    fun empty_id_write_fragments_are_rejected_as_malformed() = runBlocking {
        val (create, _) = repo(
            ok("""{"code":0,"data":{"id":"","content":"hi","content_state":"visible"}}"""),
        )
        assertFailsWith<UnifiedCommentError.Failure> {
            create.createComment(UnifiedCommentTargetType.Post, postId, "hi", "k")
        }

        val (like, _) = repo(ok("""{"code":0,"data":{"id":"","like_count":1,"is_liked":true}}"""))
        assertFailsWith<UnifiedCommentError.Failure> { like.setLiked("c1", liked = true) }

        val (delete, _) = repo(ok("""{"code":0,"data":{"id":"","status":"deleted"}}"""))
        assertFailsWith<UnifiedCommentError.Failure> { delete.deleteComment("c1") }

        val (pin, _) = repo(ok("""{"code":0,"data":{"id":"","is_pinned":true}}"""))
        assertFailsWith<UnifiedCommentError.Failure> { pin.setPinned("c1", pinned = true) }

        val (moderate, _) = repo(ok("""{"code":0,"data":{"id":"","status":"hidden"}}"""))
        assertFailsWith<UnifiedCommentError.Failure> {
            moderate.moderate("c1", UnifiedCommentModerationAction.Hide)
        }
        Unit
    }
}

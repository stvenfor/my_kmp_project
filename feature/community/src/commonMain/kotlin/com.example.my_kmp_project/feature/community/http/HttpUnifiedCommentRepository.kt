package com.example.my_kmp_project.feature.community.http

import com.example.my_kmp_project.core.network.ApiClient
import com.example.my_kmp_project.core.network.ApiResponse
import com.example.my_kmp_project.core.network.BackendCodes
import com.example.my_kmp_project.core.network.NetworkError
import com.example.my_kmp_project.core.network.NetworkFacade
import com.example.my_kmp_project.core.network.NetworkJson
import com.example.my_kmp_project.core.network.decodeEnvelopeData
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * 统一评论路径表（后端 `app/internal/community/router.go:73-91`）。
 *
 * path 里的 `:id` 都是后端 UUID，与社区旧仓储一样按字符串插值、不做额外编码；
 * 游标 / 目标 ID 这类 query 值交给 `ApiClient` 的 query map 由传输层安全转义，
 * 这里不预先 urlEncode（避免二次编码）。
 */
public object UnifiedCommentApiPaths {
    /** 根列表（GET）/ 创建评论（POST）。 */
    public const val PREFIX: String = "/api/v1/comments"

    public fun replies(commentId: String): String = "$PREFIX/$commentId/replies"

    public fun like(commentId: String): String = "$PREFIX/$commentId/like"

    public fun comment(commentId: String): String = "$PREFIX/$commentId"

    public fun pin(commentId: String): String = "$PREFIX/$commentId/pin"

    public fun moderation(commentId: String): String = "$PREFIX/$commentId/moderation"
}

/**
 * [UnifiedCommentRepository] 的真实 HTTP 实现，对接 Go BFF `/api/v1/comments`。
 *
 * 与 [HttpCommunityPostRepository] 同一绑定方式：默认每次请求现取
 * [NetworkFacade.api]（token 刷新后拿到重建的 client），单测可注入录制用 [ApiClient]。
 * 生产环境**只有这一份**实现，没有 mock / fallback 仓储。
 *
 * 成功判定走共享信封（`code == 0` 且 `data` 可解码），失败统一转成
 * [UnifiedCommentError] 强类型异常——绝不把失败响应当成空数据或伪造成功。
 */
public class HttpUnifiedCommentRepository(
    private val client: ApiClient? = null,
) : UnifiedCommentRepository {

    private val api: ApiClient get() = client ?: NetworkFacade.api()

    override suspend fun fetchRoots(
        targetType: UnifiedCommentTargetType,
        targetId: String,
        sort: UnifiedCommentSort,
        limit: Int,
        cursor: String?,
    ): UnifiedCommentPage {
        val query = linkedMapOf(
            "target_type" to targetType.wireValue,
            "target_id" to targetId,
            "sort" to sort.wireValue,
            "limit" to limit.toString(),
        )
        cursor?.takeIf { it.isNotBlank() }?.let { query["cursor"] = it }
        return request { api.getApi(UnifiedCommentApiPaths.PREFIX, query) { it } }
            .requireData<UnifiedCommentPage>()
    }

    override suspend fun fetchReplies(
        rootCommentId: String,
        limit: Int,
        cursor: String?,
    ): UnifiedCommentPage {
        val query = linkedMapOf("limit" to limit.toString())
        cursor?.takeIf { it.isNotBlank() }?.let { query["cursor"] = it }
        return request { api.getApi(UnifiedCommentApiPaths.replies(rootCommentId), query) { it } }
            .requireData<UnifiedCommentPage>()
    }

    override suspend fun createComment(
        targetType: UnifiedCommentTargetType,
        targetId: String,
        content: String,
        clientMsgId: String,
        replyToId: String?,
    ): UnifiedComment {
        val body = NetworkJson.encodeToString(
            UnifiedCommentCreateRequest(
                targetType = targetType.wireValue,
                targetId = targetId,
                content = content,
                replyToId = replyToId?.takeIf { it.isNotBlank() },
                // 幂等键原样透传：调用方在重试之间复用它，仓库不改写、不生成。
                clientMsgId = clientMsgId,
            ),
        )
        val created = request { api.postApi(UnifiedCommentApiPaths.PREFIX, body) { it } }
            .requireData<UnifiedComment>()
        return created.requireConfirmedId(created.id)
    }

    override suspend fun setLiked(commentId: String, liked: Boolean): UnifiedCommentLike {
        val path = UnifiedCommentApiPaths.like(commentId)
        val response = if (liked) {
            request { api.postApi(path, body = "") { it } }
        } else {
            request { api.deleteApi(path = path, parseData = { it }) }
        }
        val like = response.requireData<UnifiedCommentLike>()
        return like.requireConfirmedId(like.id)
    }

    override suspend fun deleteComment(commentId: String): UnifiedCommentStatus {
        val status = request {
            api.deleteApi(path = UnifiedCommentApiPaths.comment(commentId), parseData = { it })
        }.requireData<UnifiedCommentStatus>()
        return status.requireConfirmedId(status.id)
    }

    override suspend fun setPinned(commentId: String, pinned: Boolean): UnifiedCommentPin {
        val path = UnifiedCommentApiPaths.pin(commentId)
        val response = if (pinned) {
            request { api.postApi(path, body = "") { it } }
        } else {
            request { api.deleteApi(path = path, parseData = { it }) }
        }
        val pin = response.requireData<UnifiedCommentPin>()
        return pin.requireConfirmedId(pin.id)
    }

    override suspend fun moderate(
        commentId: String,
        action: UnifiedCommentModerationAction,
        reason: String?,
    ): UnifiedCommentStatus {
        val body = NetworkJson.encodeToString(
            UnifiedCommentModerationRequest(
                action = action.wireValue,
                reason = reason?.takeIf { it.isNotBlank() },
            ),
        )
        val status = request {
            api.patchApi(UnifiedCommentApiPaths.moderation(commentId), body) { it }
        }.requireData<UnifiedCommentStatus>()
        return status.requireConfirmedId(status.id)
    }

    /** 传输层失败转成 [UnifiedCommentError.Unavailable]，原始异常保留在 `cause`。 */
    private suspend fun request(
        block: suspend () -> ApiResponse<JsonElement?>,
    ): ApiResponse<JsonElement?> = try {
        block()
    } catch (error: NetworkError.Transport) {
        throw UnifiedCommentError.Unavailable(cause = error)
    }

    /**
     * 成功：HTTP 2xx（或未带状态）且 `code == 0`、`data` 能解码成 [T]；任一不满足都抛
     * [UnifiedCommentError]。误标 `code=0` 的 5xx 也不得当成功。
     */
    private inline fun <reified T> ApiResponse<JsonElement?>.requireData(): T {
        if (!isSuccess || (httpStatus != null && httpStatus !in 200..299)) throw toUnifiedError()
        val element = data ?: throw UnifiedCommentError.Failure(serverCode = code)
        return decodeEnvelopeData<T>(element)
            ?: throw UnifiedCommentError.Failure(serverCode = code)
    }

    /** 写回包必须是「已确认某条评论」的片段：空 ID 视为畸形，不能当作状态转移成功。 */
    private fun <T> T.requireConfirmedId(id: String): T {
        if (id.isBlank()) throw UnifiedCommentError.Failure()
        return this
    }

    /** 后端错误码 / HTTP 状态 → 强类型失败（顺序有意义：409 冲突先于 10001）。 */
    private fun ApiResponse<*>.toUnifiedError(): UnifiedCommentError {
        // 409 的 `data` 携带 `{conflict_with: "<原评论 id>"}`。
        val conflictWith = ((data as? JsonObject)?.get("conflict_with") as? JsonPrimitive)?.contentOrNull
        val businessCode = code
        return when {
            httpStatus == 409 -> UnifiedCommentError.Conflict(conflictWith = conflictWith)
            httpStatus == 401 -> UnifiedCommentError.Unauthorized()
            httpStatus == 403 -> UnifiedCommentError.Forbidden()
            httpStatus == 404 -> UnifiedCommentError.NotFound()
            httpStatus == 503 || httpStatus == 501 ||
                businessCode == BackendCodes.NOT_IMPLEMENTED -> UnifiedCommentError.NotEnabled()
            businessCode == BackendCodes.UNAUTHORIZED ||
                businessCode == BackendCodes.TOKEN_INVALID ||
                businessCode == BackendCodes.SESSION_INVALID ||
                businessCode == BackendCodes.SESSION_REPLACED -> UnifiedCommentError.Unauthorized()
            businessCode == BackendCodes.FORBIDDEN -> UnifiedCommentError.Forbidden()
            businessCode == BackendCodes.NOT_FOUND -> UnifiedCommentError.NotFound()
            businessCode == BackendCodes.INVALID_PARAMS -> UnifiedCommentError.Invalid()
            else -> UnifiedCommentError.Failure(serverCode = businessCode)
        }
    }
}

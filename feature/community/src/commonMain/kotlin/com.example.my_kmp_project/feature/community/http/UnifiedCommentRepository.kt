package com.example.my_kmp_project.feature.community.http

import com.example.my_kmp_project.core.network.BackendCodes

/**
 * 统一评论仓库端口（KMP ↔ Go `app/internal/community/unified_comment_*`）。
 *
 * 端点（设计 §7，字段名冻结）：
 * - `GET /api/v1/comments?target_type=&target_id=&sort=&limit=&cursor=`
 * - `GET /api/v1/comments/:id/replies?limit=&cursor=`
 * - `POST /api/v1/comments`
 * - `POST|DELETE /api/v1/comments/:id/like`
 * - `DELETE /api/v1/comments/:id`
 * - `POST|DELETE /api/v1/comments/:id/pin`
 * - `PATCH /api/v1/comments/:id/moderation`
 *
 * 认证 / 会话头由 `NetworkFacade` 的 client 在请求时统一注入；读接口匿名可用、
 * 写接口由后端严格鉴权，失败会以 [UnifiedCommentError] 的强类型形式抛出。
 */
public interface UnifiedCommentRepository {

    /** 根评论分页。`cursor` 是后端不透明 token（首页传 null）。 */
    public suspend fun fetchRoots(
        targetType: UnifiedCommentTargetType,
        targetId: String,
        sort: UnifiedCommentSort = UnifiedCommentSort.Latest,
        limit: Int = 20,
        cursor: String? = null,
    ): UnifiedCommentPage

    /** 某根评论下的全部可见后代分页（`pinned` 恒 null，`total_roots` 恒 0）。 */
    public suspend fun fetchReplies(
        rootCommentId: String,
        limit: Int = 20,
        cursor: String? = null,
    ): UnifiedCommentPage

    /**
     * 发表评论 / 回复。
     *
     * [clientMsgId] 是**调用方稳定持有**的幂等键（1..64 字符）：同一次撰写在重试 / 超时后
     * 必须复用同一个 key，只有用户新起一次撰写才生成新 key。仓库层**绝不**自行生成或改写它。
     *
     * 后端新写入返回 HTTP 201、幂等重放返回 HTTP 200，两者都是同一份评论信封；
     * 同 key 不同载荷返回 409 + `conflict_with`（映射为 [UnifiedCommentError.Conflict]）。
     */
    public suspend fun createComment(
        targetType: UnifiedCommentTargetType,
        targetId: String,
        content: String,
        clientMsgId: String,
        replyToId: String? = null,
    ): UnifiedComment

    /** 点赞 / 取消点赞（同 path 切 POST/DELETE）。 */
    public suspend fun setLiked(commentId: String, liked: Boolean): UnifiedCommentLike

    /** 作者删除（终态墓碑）或管理员删除；重放返回墓碑状态。 */
    public suspend fun deleteComment(commentId: String): UnifiedCommentStatus

    /** 置顶 / 取消置顶（仅可见根，权限由后端派生）。 */
    public suspend fun setPinned(commentId: String, pinned: Boolean): UnifiedCommentPin

    /** 平台管理员审核：`hide` / `unhide`（不复活已删除评论）。 */
    public suspend fun moderate(
        commentId: String,
        action: UnifiedCommentModerationAction,
        reason: String? = null,
    ): UnifiedCommentStatus
}

/**
 * 统一评论的强类型失败。
 *
 * `message` 恒为**可直接展示的中文兜底文案**，绝不携带后端 / 传输层原始诊断
 * （原始异常挂在 `cause` 上仅供日志排查）。UI 只按类型分支，不解析文案。
 */
public sealed class UnifiedCommentError(
    message: String,
    val code: Int? = null,
    cause: Throwable? = null,
) : Exception(message, cause) {

    /** 401 / 会话或 token 失效：需要重新登录后再操作。 */
    public class Unauthorized(cause: Throwable? = null) :
        UnifiedCommentError("请先登录再操作评论", BackendCodes.UNAUTHORIZED, cause)

    /** 403：缺少删除 / 置顶 / 审核等权限。 */
    public class Forbidden(cause: Throwable? = null) :
        UnifiedCommentError("没有操作该评论的权限", BackendCodes.FORBIDDEN, cause)

    /** 404：评论或目标不存在。 */
    public class NotFound(cause: Throwable? = null) :
        UnifiedCommentError("评论不存在或已删除", BackendCodes.NOT_FOUND, cause)

    /** 503 / 10005：统一评论在当前切换阶段尚未启用（不可重试、不得当成功）。 */
    public class NotEnabled(cause: Throwable? = null) :
        UnifiedCommentError("评论功能暂未开放", BackendCodes.NOT_IMPLEMENTED, cause)

    /**
     * 409：同一 `client_msg_id` 已用于不同载荷。
     * [conflictWith] 是后端返回的原评论 ID（`conflict_with`），可能为 null。
     */
    public class Conflict(
        public val conflictWith: String? = null,
        cause: Throwable? = null,
    ) : UnifiedCommentError("该评论已提交过，请刷新后查看", BackendCodes.INVALID_PARAMS, cause)

    /** 400 / 10001：参数不合法（空内容、非法 cursor、非法 target 等）。 */
    public class Invalid(cause: Throwable? = null) :
        UnifiedCommentError("评论请求参数不合法", BackendCodes.INVALID_PARAMS, cause)

    /** 连接失败 / 超时等传输层失败；原始异常在 [cause]。 */
    public class Unavailable(cause: Throwable? = null) :
        UnifiedCommentError("连接评论服务失败，请稍后重试", cause = cause)

    /** 其他未分类失败；[serverCode] 保留后端业务码便于排查。 */
    public class Failure(
        public val serverCode: Int? = null,
        cause: Throwable? = null,
    ) : UnifiedCommentError("评论操作失败，请稍后重试", serverCode, cause)
}

package com.example.my_kmp_project.feature.community.http

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 统一评论（帖子 / 视频 / 文章共用）公开契约 DTO，对应后端
 * `app/internal/community/unified_comment_dto.go`（字段名冻结，设计 §7）。
 *
 * 解码策略与社区旧模型一致：线上字段一律 **raw 可空 + 只读派生属性**，
 * 缺键 / 显式 null 都不会让整条记录解码失败。[NetworkJson] 是 lenient 的。
 *
 * 安全口径（设计 §4.3 / §8）：
 * - 正文只在 `content_state == visible` 时下发；隐藏 / 删除一律空串，
 *   缺 `content_state` 也按不可见处理（fail closed）。
 * - 能力位 `can_*` 只在后端显式为 `true` 时成立，缺失一律 false。
 * - 作者 / 被回复者 / 计数 / 时间全部透传后端值；**不伪造身份、不按列表长度推总数、
 *   不把非法时间回落到「现在」**（[UnifiedComment.createdAtMillis] 返回 null 表示未知）。
 */

/** 后端 `target_type` 的规范三值（**没有** `short_video` 别名）。 */
public enum class UnifiedCommentTargetType(public val wireValue: String) {
    Post("post"),
    Video("video"),
    Article("article"),
}

/** 根列表排序：`latest`（不可变 `created_at` 键）/ `hot`（`score` 键）。 */
public enum class UnifiedCommentSort(public val wireValue: String) {
    Latest("latest"),
    Hot("hot"),
}

/** 审核动作（`PATCH /comments/:id/moderation`）。 */
public enum class UnifiedCommentModerationAction(public val wireValue: String) {
    Hide("hide"),
    Unhide("unhide"),
}

/** `content_state` 三值；无法识别按 [Unknown] 处理（正文视为不可见）。 */
public enum class UnifiedCommentContentState {
    Visible,
    Hidden,
    Deleted,
    Unknown,
    ;

    public companion object {
        public fun fromWire(value: String?): UnifiedCommentContentState = when (value) {
            "visible" -> Visible
            "hidden" -> Hidden
            "deleted" -> Deleted
            else -> Unknown
        }
    }
}

/** 评论作者 / 被回复者（`author` / `reply_to_author`）。删除态为空值对象。 */
@Serializable
public data class UnifiedCommentAuthor(
    @SerialName("user_id") val userIdRaw: String? = null,
    @SerialName("nickname") val nicknameRaw: String? = null,
    @SerialName("avatar") val avatarRaw: String? = null,
    @SerialName("is_author") val isAuthorRaw: Boolean? = null,
) {
    public val userId: String get() = userIdRaw.orEmpty()

    public val nickname: String get() = nicknameRaw.orEmpty()

    public val avatar: String get() = avatarRaw.orEmpty()

    /** 本条是否由目标作者发出；缺失按 false。 */
    public val isAuthor: Boolean get() = isAuthorRaw == true
}

/** 单条统一评论的公开读模型。 */
@Serializable
public data class UnifiedComment(
    @SerialName("id") val idRaw: String? = null,
    @SerialName("subject_id") val subjectIdRaw: String? = null,
    @SerialName("target_type") val targetTypeRaw: String? = null,
    @SerialName("target_id") val targetIdRaw: String? = null,
    @SerialName("root_id") val rootIdRaw: String? = null,
    @SerialName("reply_to_id") val replyToIdRaw: String? = null,
    @SerialName("author") val authorRaw: UnifiedCommentAuthor? = null,
    @SerialName("content") val contentRaw: String? = null,
    @SerialName("content_state") val contentStateRaw: String? = null,
    @SerialName("legacy_reply_to_nickname") val legacyReplyToNicknameRaw: String? = null,
    @SerialName("reply_to_author") val replyToAuthorRaw: UnifiedCommentAuthor? = null,
    @SerialName("is_pinned") val isPinnedRaw: Boolean? = null,
    @SerialName("is_author_reply") val isAuthorReplyRaw: Boolean? = null,
    @SerialName("like_count") val likeCountRaw: Long? = null,
    @SerialName("reply_count") val replyCountRaw: Long? = null,
    @SerialName("is_liked") val isLikedRaw: Boolean? = null,
    @SerialName("can_reply") val canReplyRaw: Boolean? = null,
    @SerialName("can_like") val canLikeRaw: Boolean? = null,
    @SerialName("can_delete") val canDeleteRaw: Boolean? = null,
    @SerialName("can_pin") val canPinRaw: Boolean? = null,
    @SerialName("can_moderate") val canModerateRaw: Boolean? = null,
    @SerialName("created_at") val createdAtRaw: String? = null,
    @SerialName("updated_at") val updatedAtRaw: String? = null,
) {
    public val id: String get() = idRaw.orEmpty()

    public val subjectId: String get() = subjectIdRaw.orEmpty()

    /** 规范 `target_type` 原文（`post` / `video` / `article`）。 */
    public val targetType: String get() = targetTypeRaw.orEmpty()

    public val targetId: String get() = targetIdRaw.orEmpty()

    /** 根评论为 null；非根为所属根 ID。 */
    public val rootId: String? get() = rootIdRaw?.takeIf { it.isNotBlank() }

    /** 真实被回复评论 ID；根为 null。 */
    public val replyToId: String? get() = replyToIdRaw?.takeIf { it.isNotBlank() }

    public val author: UnifiedCommentAuthor? get() = authorRaw

    /**
     * 正文：仅 `content_state == visible` 时返回原值；隐藏 / 删除 / 无法识别一律空串，
     * 与后端裁剪规则一致（墓碑正文永不下发）。
     */
    public val content: String
        get() = if (contentState == UnifiedCommentContentState.Visible) contentRaw.orEmpty() else ""

    public val contentState: UnifiedCommentContentState
        get() = UnifiedCommentContentState.fromWire(contentStateRaw)

    /** 仅旧数据回填的历史回复昵称（新回复为 null）。 */
    public val legacyReplyToNickname: String?
        get() = legacyReplyToNicknameRaw?.takeIf { it.isNotBlank() }

    /** 被回复评论的作者；`reply_to_id` 指向已删除评论或根评论时为 null。 */
    public val replyToAuthor: UnifiedCommentAuthor? get() = replyToAuthorRaw

    public val isPinned: Boolean get() = isPinnedRaw == true

    public val isAuthorReply: Boolean get() = isAuthorReplyRaw == true

    public val likeCount: Long get() = likeCountRaw ?: 0L

    /** 根下全部可见后代数；非根恒 0（后端口径）。 */
    public val replyCount: Long get() = replyCountRaw ?: 0L

    public val isLiked: Boolean get() = isLikedRaw == true

    public val canReply: Boolean get() = canReplyRaw == true

    public val canLike: Boolean get() = canLikeRaw == true

    public val canDelete: Boolean get() = canDeleteRaw == true

    public val canPin: Boolean get() = canPinRaw == true

    public val canModerate: Boolean get() = canModerateRaw == true

    /** `created_at`（RFC3339Nano）→ epoch 毫秒；缺失 / 非法返回 null，绝不回落「现在」。 */
    public fun createdAtMillis(): Long? = parseCommunityTimeMs(createdAtRaw)

    /** `updated_at` 同上。 */
    public fun updatedAtMillis(): Long? = parseCommunityTimeMs(updatedAtRaw)
}

/** 唯一嵌套游标：`next` 为 null 或后端不透明 token，`has_more` 与之一致。 */
@Serializable
public data class UnifiedCommentCursor(
    @SerialName("next") val nextRaw: String? = null,
    @SerialName("has_more") val hasMoreRaw: Boolean? = null,
) {
    public val next: String? get() = nextRaw?.takeIf { it.isNotBlank() }

    public val hasMore: Boolean get() = hasMoreRaw == true
}

/**
 * 根列表 / 后代列表共用信封（字段不省略）：
 * `{list, pinned, cursor:{next,has_more}, total_visible, total_roots}`。
 *
 * 根列表：`total_visible` = 可见内容数、`total_roots` = 容器根数；
 * 后代列表：`pinned` 恒 null、`total_roots` 恒 0、`total_visible` = 该根可见回复数。
 * 总数只能取后端值，**不得用 `list.size` 反推**。
 */
@Serializable
public data class UnifiedCommentPage(
    @SerialName("list") val listRaw: List<UnifiedComment>? = null,
    @SerialName("pinned") val pinnedRaw: UnifiedComment? = null,
    @SerialName("cursor") val cursorRaw: UnifiedCommentCursor? = null,
    @SerialName("total_visible") val totalVisibleRaw: Long? = null,
    @SerialName("total_roots") val totalRootsRaw: Long? = null,
) {
    public val list: List<UnifiedComment> get() = listRaw.orEmpty()

    /** 独立置顶通道（至多 1 条）；与 [list] 互斥，按 id 去重。 */
    public val pinned: UnifiedComment? get() = pinnedRaw

    public val cursor: UnifiedCommentCursor get() = cursorRaw ?: UnifiedCommentCursor()

    public val nextCursor: String? get() = cursor.next

    public val hasMore: Boolean get() = cursor.hasMore

    public val totalVisible: Long get() = totalVisibleRaw ?: 0L

    public val totalRoots: Long get() = totalRootsRaw ?: 0L
}

/** 点赞片段 `{id, like_count, is_liked}`。 */
@Serializable
public data class UnifiedCommentLike(
    @SerialName("id") val idRaw: String? = null,
    @SerialName("like_count") val likeCountRaw: Long? = null,
    @SerialName("is_liked") val isLikedRaw: Boolean? = null,
) {
    public val id: String get() = idRaw.orEmpty()

    public val likeCount: Long get() = likeCountRaw ?: 0L

    public val isLiked: Boolean get() = isLikedRaw == true
}

/** 置顶片段 `{id, is_pinned}`。 */
@Serializable
public data class UnifiedCommentPin(
    @SerialName("id") val idRaw: String? = null,
    @SerialName("is_pinned") val isPinnedRaw: Boolean? = null,
) {
    public val id: String get() = idRaw.orEmpty()

    public val isPinned: Boolean get() = isPinnedRaw == true
}

/** 状态片段 `{id, status}`：删除为 `deleted`，审核为 `visible` / `hidden`。 */
@Serializable
public data class UnifiedCommentStatus(
    @SerialName("id") val idRaw: String? = null,
    @SerialName("status") val statusRaw: String? = null,
) {
    public val id: String get() = idRaw.orEmpty()

    public val status: String get() = statusRaw.orEmpty()
}

/** `POST /api/v1/comments` 请求体；`reply_to_id` 为 null 时整键省略（后端可空）。 */
@Serializable
internal data class UnifiedCommentCreateRequest(
    @SerialName("target_type") val targetType: String,
    @SerialName("target_id") val targetId: String,
    val content: String,
    @SerialName("reply_to_id") val replyToId: String? = null,
    @SerialName("client_msg_id") val clientMsgId: String,
)

/** `PATCH /api/v1/comments/:id/moderation` 请求体；`reason` 为 null 时整键省略。 */
@Serializable
internal data class UnifiedCommentModerationRequest(
    val action: String,
    val reason: String? = null,
)

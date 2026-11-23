package com.example.my_kmp_project.feature.community

import com.example.my_kmp_project.core.router.CommentRoutes
import com.example.my_kmp_project.feature.community.http.UnifiedCommentTargetType

/**
 * 评论区打开目标。
 *
 * [Invalid] 是**显式**的非法态：路由缺类型 / 缺 ID / 未知类型时由
 * [fromRoute] 返回，UI 必须展示安全的「目标无效」提示，绝不静默回落成 post。
 */
public sealed interface UnifiedCommentTarget {

    public data class Valid(
        val type: UnifiedCommentTargetType,
        val id: String,
    ) : UnifiedCommentTarget

    public data object Invalid : UnifiedCommentTarget

    public companion object {
        /** 解析 `/community/comment?...`；非评论路由或非法参数一律 [Invalid]。 */
        public fun fromRoute(route: String): UnifiedCommentTarget {
            val parsed = CommentRoutes.parse(route) ?: return Invalid
            val type = UnifiedCommentTargetType.entries.firstOrNull {
                it.wireValue == parsed.targetType
            } ?: return Invalid
            return Valid(type, parsed.targetId)
        }
    }
}

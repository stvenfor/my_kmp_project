package com.example.my_kmp_project.core.router

/**
 * 统一评论入口的**目标解析**（`/community/comment`）。
 *
 * 只做路由字符串 → 目标 tuple 的纯解析，不渲染、不访问网络。三种规范目标
 * （post / video / article）与后端 `target_type` 完全一致；**没有** `short_video` 别名。
 *
 * 支持的查询参数（任意顺序）：
 * - `targetType` / `target_type` + `targetId` / `target_id`：通用目标；
 * - `postId`：旧版帖子评论入口，等价于 `targetType=post`。
 *
 * 解析失败（缺类型 / 缺 ID / 未知类型 / 空白值）返回 null，由调用方展示安全的
 * 「目标无效」状态；**绝不**把 video / article 静默回落成 post。
 */
public data class CommentRouteTarget(
    /** 规范 target_type：`post` / `video` / `article`。 */
    public val targetType: String,
    public val targetId: String,
)

public object CommentRoutes {
    public const val Base: String = CommunityRoutes.Comment

    public const val ParamTargetType: String = "targetType"
    public const val ParamTargetId: String = "targetId"
    public const val ParamTargetTypeSnake: String = "target_type"
    public const val ParamTargetIdSnake: String = "target_id"
    public const val ParamLegacyPostId: String = "postId"

    private val knownTypes: Set<String> = setOf("post", "video", "article")

    /** 是否为统一评论路由（含无查询参数的裸路径）。 */
    public fun isCommentRoute(route: String): Boolean =
        route == Base || route.startsWith("$Base?") || route.startsWith("$Base/")

    /** 解析目标；任何不合法输入返回 null（调用方据此展示 invalid-target 状态）。 */
    public fun parse(route: String): CommentRouteTarget? {
        if (!isCommentRoute(route)) return null
        val query = route.substringAfter('?', missingDelimiterValue = "")
        val params = parseQuery(query)

        val explicitType = params[ParamTargetType] ?: params[ParamTargetTypeSnake]
        val explicitId = params[ParamTargetId] ?: params[ParamTargetIdSnake]
        if (explicitType != null || explicitId != null) {
            val type = explicitType?.trim()?.lowercase() ?: return null
            val id = explicitId?.trim()?.orEmpty() ?: return null
            if (type !in knownTypes || id.isEmpty()) return null
            return CommentRouteTarget(targetType = type, targetId = id)
        }

        val legacyPostId = params[ParamLegacyPostId]?.trim()?.orEmpty() ?: return null
        if (legacyPostId.isEmpty()) return null
        return CommentRouteTarget(targetType = "post", targetId = legacyPostId)
    }

    /** 构造通用目标路由（值原样拼接；ID 为 UUID，不做额外编码）。 */
    public fun target(type: String, id: String): String =
        "$Base?$ParamTargetType=${type.trim().lowercase()}&$ParamTargetId=${id.trim()}"

    /** 构造旧版帖子评论路由（保留兼容入口）。 */
    public fun post(postId: String): String = "$Base?$ParamLegacyPostId=${postId.trim()}"

    private fun parseQuery(query: String): Map<String, String> {
        if (query.isEmpty()) return emptyMap()
        val result = LinkedHashMap<String, String>()
        for (part in query.split('&')) {
            if (part.isEmpty()) continue
            val key = part.substringBefore('=', missingDelimiterValue = part).trim()
            if (key.isEmpty() || key in result) continue
            result[key] = part.substringAfter('=', missingDelimiterValue = "")
        }
        return result
    }
}

package com.example.my_kmp_project.feature.media

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull

/**
 * 短视频（小视频）域模型 —— Dart `features/video/lib/short_video/model/short_video_models.dart`
 * （my_ai_project @ d7876a1）的 KMP 对等。
 *
 * 契约行：#72–#78（`openspec/changes/sync-flutter-full-parity/notes/api-contract-2026-10-16.md` §2.13）。
 * Dart 侧 `ShortVideoTopicBrief{id,name}` 是对 community `TopicModel` 的**故意重复**（契约 §6），
 * KMP 只保留本包这一份 DTO，不做 feature→feature import。
 */

/**
 * Dart `ShortVideoStatus`（short_video_models.dart:3）：status 域
 * `normal | reviewing | uploading`；未知/缺失一律按 `normal`（`_parseStatus` :130-139）。
 */
public enum class ShortVideoStatus {
    Normal,
    Reviewing,
    Uploading,
    ;

    public companion object {
        /** Dart `_parseStatus`（short_video_models.dart:130-139）。 */
        public fun fromWire(raw: String?): ShortVideoStatus = when (raw) {
            "reviewing" -> Reviewing
            "uploading" -> Uploading
            else -> Normal
        }
    }
}

/**
 * Dart `ShortVideoTopicBrief`（short_video_models.dart:5-19）：
 * `id`/`name` wire 值经 `?.toString() ?? ''` 宽容转字符串，缺省空串。
 */
public data class ShortVideoTopicBrief(
    public val id: String,
    public val name: String,
) {
    /** Dart `displayName`（short_video_models.dart:18）：话题前缀 `#`。 */
    public val displayName: String get() = "#$name"
}

/**
 * Dart `ShortVideoStatsModel`（short_video_models.dart:142-168）：
 * **wire 上是数字，进 UI 前字符串化**（`_n` :167 `((v as num?)?.toInt() ?? 0).toString()`）；
 * `stats` 缺失或非 map → [EMPTY]（`ShortVideoProfileModel.fromJson` :190-192）。
 */
public data class ShortVideoStatsModel(
    public val videoCount: String,
    public val viewCount: String,
    public val likeCount: String,
) {
    public companion object {
        /** Dart `ShortVideoStatsModel.empty`（short_video_models.dart:161-165）。 */
        public val EMPTY: ShortVideoStatsModel = ShortVideoStatsModel("0", "0", "0")
    }
}

/**
 * Dart `ShortVideoProfileModel`（short_video_models.dart:170-203，#72）。
 *
 * `is_me` 走 Dart 工厂的 `json['is_me'] == true` 严格布尔（:189）——缺省即 false；
 * `display_name`/`role_badge`/`store_name` 缺省空串（:185-188），
 * `user_id`/`avatar_url` 可空（:184,186）。
 */
public data class ShortVideoProfileModel(
    public val userId: String? = null,
    public val displayName: String = "",
    public val avatarUrl: String? = null,
    public val roleBadge: String = "",
    public val storeName: String = "",
    public val isMe: Boolean = true,
    public val stats: ShortVideoStatsModel = ShortVideoStatsModel.EMPTY,
)

/**
 * Dart `ShortVideoItemModel`（short_video_models.dart:21-140，#73/#74/#76/#77）。
 *
 * 默认值即 Dart 构造默认（:31-34）：`aspectRatio` 缺省 **1.25**（fromJson :52 同款回退）、
 * `status` 缺省 `normal`、`is_liked`/`is_mine` 仅当 wire 值严格为 `true`（:54-55）。
 * `like_count` 经 `fromJson` 解码时缺省 0（:50），故 wire 转换器总是给出非空值。
 */
public data class ShortVideoItemModel(
    public val id: String? = null,
    public val title: String? = null,
    public val coverUrl: String? = null,
    public val videoUrl: String? = null,
    public val viewCount: Int? = null,
    public val likeCount: Int? = null,
    public val duration: String? = null,
    public val aspectRatio: Double = 1.25,
    public val status: ShortVideoStatus = ShortVideoStatus.Normal,
    public val isLiked: Boolean = false,
    public val isMine: Boolean = false,
    public val userId: String? = null,
    public val nickname: String? = null,
    public val avatar: String? = null,
    public val topic: ShortVideoTopicBrief? = null,
    public val publishTime: String? = null,
) {
    /**
     * Dart `copyWith`（short_video_models.dart:103-128）：传 null 表示保持原值。
     * 消费方用它就地应用点赞片段 / 播放计数（controller :261-283）。
     */
    public fun withCounts(
        viewCount: Int? = null,
        likeCount: Int? = null,
        isLiked: Boolean? = null,
        status: ShortVideoStatus? = null,
    ): ShortVideoItemModel = copy(
        viewCount = viewCount ?: this.viewCount,
        likeCount = likeCount ?: this.likeCount,
        isLiked = isLiked ?: this.isLiked,
        status = status ?: this.status,
    )

    public companion object {
        /**
         * Dart `ShortVideoItemModel.fromLikeFragment`（short_video_models.dart:69-76）：
         * 点赞接口只回 `{id, like_count, is_liked}` 片段，其余字段保持默认；
         * `like_count` 缺省 0（:73）。
         */
        public fun fromLikeFragment(
            id: String?,
            likeCount: Int,
            isLiked: Boolean,
        ): ShortVideoItemModel = ShortVideoItemModel(
            id = id,
            likeCount = likeCount,
            isLiked = isLiked,
        )
    }
}

// ── wire 转换器（对应 Dart `fromJson(Map<String, dynamic>)` 手搓读取） ──────────────

/**
 * Dart `ShortVideoItemModel.fromJson`（short_video_models.dart:42-66）。
 * 非 object 行返回 null（Dart `ListData.fromJson` 的 `whereType<Map>` 语义，result_model.dart:112）。
 */
internal fun shortVideoItemFromJson(el: JsonElement?): ShortVideoItemModel? {
    val json = el as? JsonObject ?: return null
    return ShortVideoItemModel(
        id = json.textOf("id"),
        title = json.textOf("title"),
        coverUrl = json.textOf("cover_url"),
        videoUrl = json.textOf("video_url"),
        viewCount = json.intOf("view_count"),
        likeCount = json.intOf("like_count") ?: 0,
        duration = json.textOf("duration"),
        aspectRatio = json.doubleOf("aspect_ratio") ?: 1.25,
        status = ShortVideoStatus.fromWire(json.textOf("status")),
        isLiked = json.isTrue("is_liked"),
        isMine = json.isTrue("is_mine"),
        userId = json.textOf("user_id"),
        nickname = json.textOf("nickname"),
        avatar = json.textOf("avatar"),
        topic = (json["topic"] as? JsonObject)?.let {
            ShortVideoTopicBrief(id = it.textOf("id") ?: "", name = it.textOf("name") ?: "")
        },
        publishTime = json.textOf("publish_time"),
    )
}

/**
 * Dart `ShortVideoItemModel.fromLikeFragment`（short_video_models.dart:69-76），
 * 用于 #76/#77 like/unlike 片段；非 object `data` → null（由仓库抛「操作失败」，
 * http_short_video_repository.dart:118 `_data`）。
 */
internal fun shortVideoLikeFragmentFromJson(el: JsonElement?): ShortVideoItemModel? {
    val json = el as? JsonObject ?: return null
    return ShortVideoItemModel.fromLikeFragment(
        id = json.textOf("id"),
        likeCount = json.intOf("like_count") ?: 0,
        isLiked = json.isTrue("is_liked"),
    )
}

/**
 * Dart `ShortVideoProfileModel.fromJson`（short_video_models.dart:181-193）；
 * 非 object `data` → null（http_short_video_repository.dart:24 `_data` 抛「加载资料失败」）。
 */
internal fun shortVideoProfileFromJson(el: JsonElement?): ShortVideoProfileModel? {
    val json = el as? JsonObject ?: return null
    val stats = json["stats"] as? JsonObject
    return ShortVideoProfileModel(
        userId = json.textOf("user_id"),
        displayName = json.textOf("display_name") ?: "",
        avatarUrl = json.textOf("avatar_url"),
        roleBadge = json.textOf("role_badge") ?: "",
        storeName = json.textOf("store_name") ?: "",
        isMe = json.isTrue("is_me"),
        stats = if (stats != null) {
            // Dart `_n`（short_video_models.dart:167）：数字（宽容字符串数字）→ 字符串，缺省 "0"。
            ShortVideoStatsModel(
                videoCount = (stats.intOf("video_count") ?: 0).toString(),
                viewCount = (stats.intOf("view_count") ?: 0).toString(),
                likeCount = (stats.intOf("like_count") ?: 0).toString(),
            )
        } else {
            ShortVideoStatsModel.EMPTY
        },
    )
}

/** Dart `json['k']?.toString()`：数字/布尔取字面量，null/JsonNull → null。 */
internal fun JsonObject.textOf(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull

internal fun JsonObject.intOf(key: String): Int? = flexibleInt(this[key])

internal fun JsonObject.doubleOf(key: String): Double? = flexibleDouble(this[key])

internal fun JsonObject.isTrue(key: String): Boolean =
    (this[key] as? JsonPrimitive)?.booleanOrNull == true

internal fun flexibleInt(el: JsonElement?): Int? = when (el) {
    null, is JsonNull -> null
    is JsonPrimitive -> el.intOrNull ?: el.contentOrNull?.toDoubleOrNull()?.toInt()
    else -> null
}

internal fun flexibleDouble(el: JsonElement?): Double? = when (el) {
    null, is JsonNull -> null
    is JsonPrimitive -> el.doubleOrNull ?: el.contentOrNull?.toDoubleOrNull()
    else -> null
}

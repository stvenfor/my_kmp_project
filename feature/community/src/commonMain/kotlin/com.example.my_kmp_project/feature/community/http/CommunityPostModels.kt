package com.example.my_kmp_project.feature.community.http

import com.example.my_kmp_project.core.network.NetworkJson
import com.example.my_kmp_project.core.network.decodeEnvelopeData
import com.example.my_kmp_project.core.network.decodeListPage
import com.example.my_kmp_project.core.network.ListPage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * 社区（§2.12 #59–#71）响应 / 请求 DTO。
 *
 * Dart 侧唯一事实来源：
 * - `features/community/lib/community/models/post_model.dart`
 * - `features/community/lib/community/models/comment_model.dart`
 * - `features/community/lib/community/models/topic_model.dart`
 * - `features/community/lib/community/models/community_search_models.dart`
 *
 * 字段宽松性说明（与 Dart 的 `json['k']?.toString() ?? ''` 等价）：
 * 所有线上字段都声明为**可空 raw + 解析函数/属性**，因此 BFF 显式发 `null`
 * 或整体缺键都不会让整条记录解码失败（KMP 的 `decodeEnvelopeData` 解码失败
 * 会返回 null，导致该行被丢弃，故不能把可能为 null 的键声明成非空类型）。
 * [NetworkJson] 是 `isLenient` 的，`{"id": 123}` 也会读成 `"123"`，
 * 与 Dart 的 `.toString()` 行为一致（post_model.dart:41）。
 */

/** 点赞 POST/DELETE 片段的 `is_ask_everyone` / `source` 默认值（post_model.dart:12、:49）。 */
const val CommunityDefaultSource: String = "来自 iPhone"

@OptIn(ExperimentalTime::class)
fun communityNowMs(): Long = Clock.System.now().toEpochMilliseconds()

/**
 * `publish_time` / `create_time` 是 **ISO-8601 字符串**，Dart 用
 * `DateTime.tryParse(...) ?? DateTime.now()` 兜底（post_model.dart:46-48、comment_model.dart:19-20）。
 *
 * 解析成功返回 epoch **毫秒**；缺省 / 空串 / 非法格式返回 `null`，由调用方回落到「现在」。
 * 接受的形状：`YYYY-MM-DD`、`YYYY-MM-DDTHH:mm[:ss[.fff…]]`，日期与时间之间允许 `T`/`t`/空格，
 * 时区允许 `Z`、`±HH`、`±HH:mm`、`±HHmm`。
 *
 * 与 Dart 的唯一差异：无时区后缀时 Dart 按**本机时区**解释，KMP common 无本机时区概念，
 * 这里按 **UTC** 解释（跨端一致、可单测）。
 */
fun parseCommunityTimeMs(text: String?): Long? {
    val raw = text?.trim()?.ifBlank { return null } ?: return null
    val split = raw.indexOfFirst { it == 'T' || it == 't' || it == ' ' }
    val datePart = if (split >= 0) raw.substring(0, split) else raw
    val rest = if (split >= 0) raw.substring(split + 1) else ""

    val date = parseCivilDate(datePart) ?: return null
    if (split < 0) return dayMillis(date)

    // 拆出时区后缀
    var body = rest
    var offsetSeconds = 0L
    var hasZone = false
    val zoneStart = body.indexOfFirst { it == 'Z' || it == 'z' || it == '+' || it == '-' }
    if (zoneStart >= 0) {
        val zone = body.substring(zoneStart)
        body = body.substring(0, zoneStart)
        hasZone = true
        if (zone == "Z" || zone == "z") {
            offsetSeconds = 0L
        } else {
            val sign = if (zone.first() == '-') -1 else 1
            val digits = zone.substring(1).filter { it.isDigit() }
            val hour = when {
                digits.length >= 2 -> digits.substring(0, 2).toIntOrNull()
                else -> null
            } ?: return null
            val minute = when {
                digits.length >= 4 -> digits.substring(2, 4).toIntOrNull()
                digits.length == 3 -> digits.substring(2).toIntOrNull()
                else -> 0
            } ?: return null
            if (hour > 23 || minute > 59) return null
            offsetSeconds = sign * (hour * 3600L + minute * 60L)
        }
    }
    if (!hasZone && body.isNotEmpty() && body.last() == ' ') body = body.dropLast(1)

    val time = parseTimeOfDay(body) ?: return null
    return dayMillis(date) + time - offsetSeconds * 1000L
}

/** 返回 epoch 天（1970-01-01 起算，可为负）。 */
private fun parseCivilDate(text: String): Long? {
    val parts = text.split('-')
    if (parts.size != 3) return null
    val year = parts[0].toIntStrict(minus = false) ?: return null
    if (parts[0].length != 4) return null
    val month = parts[1].toIntStrict(minus = false) ?: return null
    val day = parts[2].toIntStrict(minus = false) ?: return null
    if (month !in 1..12 || day !in 1..31) return null
    return daysFromCivil(year, month, day)
}

private fun dayMillis(days: Long): Long = days * 86_400_000L

/** `HH:mm[:ss[.fff]]` → 当日毫秒；无秒/小数按 0 计。 */
private fun parseTimeOfDay(text: String): Long? {
    if (text.isEmpty()) return 0L
    val parts = text.split(':')
    val hour = parts[0].toIntStrict(minus = false) ?: return null
    if (hour > 23) return null
    if (parts.size == 1) return null // Dart 需要至少到分钟
    val minute = parts[1].toIntStrict(minus = false) ?: return null
    if (minute > 59) return null
    var millis = 0L
    var second = 0
    if (parts.size >= 3) {
        val sec = parts[2]
        val dot = sec.indexOf('.')
        val secText = if (dot >= 0) sec.substring(0, dot) else sec
        val fracText = if (dot >= 0) sec.substring(dot + 1) else ""
        second = secText.toIntStrict(minus = false) ?: return null
        if (second > 59) return null
        if (fracText.isNotEmpty()) {
            if (!fracText.all { it.isDigit() }) return null
            millis = fracText.take(3).padEnd(3, '0').toLong()
        }
        // Dart 还接受小数秒之后的多余段，这里只保证前三位（毫秒）精度
        if (parts.size > 3 && fracText.isEmpty()) return null
    }
    return hour * 3_600_000L + minute * 60_000L + second * 1_000L + millis
}

/** 纯数字串（可选前导负号）严格转 Int，带符号或带非数字字符即失败。 */
private fun String.toIntStrict(minus: Boolean): Int? {
    if (isEmpty()) return null
    val body = if (minus && first() == '-') substring(1) else this
    if (body.isEmpty() || !body.all { it.isDigit() }) return null
    return body.toIntOrNull()?.let { if (minus && first() == '-') -it else it }
}

/** Howard Hinnant `days_from_civil`（无时区依赖，KMP 三端一致）。 */
private fun daysFromCivil(y: Int, m: Int, d: Int): Long {
    val year = if (m <= 2) y - 1 else y
    val era = (if (year >= 0) year else year - 399) / 400
    val yoe = year - era * 400
    val doy = (153 * (if (m > 2) m - 3 else m + 9) + 2) / 5 + d - 1
    val doe = yoe.toLong() * 365 + yoe / 4 - yoe / 100 + doy
    return era.toLong() * 146_097L + doe - 719_468L
}

/**
 * Flutter `TopicModel`（topic_model.dart:1-36）。
 *
 * `heat` 用 `num?.toInt() ?? 0`（:13），`is_ask_everyone` 用 `== true`（:14）——
 * 非布尔真值（含字符串 `"true"`）一律按 `false`，此处保持同样的严格语义。
 */
@Serializable
public data class TopicModel(
    @SerialName("id") val idRaw: String? = null,
    @SerialName("name") val nameRaw: String? = null,
    @SerialName("heat") val heatRaw: Int? = null,
    @SerialName("is_ask_everyone") val isAskEveryoneRaw: Boolean? = null,
) {
    /** `json['id']?.toString() ?? ''`（topic_model.dart:11）。 */
    public val id: String get() = idRaw.orEmpty()

    /** `json['name']?.toString() ?? ''`（topic_model.dart:12）。 */
    public val name: String get() = nameRaw.orEmpty()

    /** `json['heat'] as num?` 兜 0（topic_model.dart:13）。 */
    public val heat: Int get() = heatRaw ?: 0

    /** `json['is_ask_everyone'] == true`（topic_model.dart:14）。 */
    public val isAskEveryone: Boolean get() = isAskEveryoneRaw == true

    /** 热度展示：≥1 万用「x.x万」（topic_model.dart:24-33）。 */
    public val heatLabel: String
        get() {
            if (heat < 10_000) return heat.toString()
            val tenths = ((heat.toLong() * 10 + 5_000) / 10_000).toInt()
            return if (heat % 10_000 == 0) {
                "${heat / 10_000}万"
            } else {
                "${tenths / 10}.${tenths % 10}万"
            }
        }

    /** `'#$name'`（topic_model.dart:35）。 */
    public val displayName: String get() = "#$name"
}

/**
 * Flutter `CommentModel`（comment_model.dart:1-52）。
 * `create_time` 是字符串→时间戳，非法/缺失回落「现在」（:19-20）。
 */
@Serializable
public data class CommentModel(
    @SerialName("id") val idRaw: String? = null,
    @SerialName("post_id") val postIdRaw: String? = null,
    @SerialName("nickname") val nicknameRaw: String? = null,
    @SerialName("avatar") val avatarRaw: String? = null,
    @SerialName("content") val contentRaw: String? = null,
    @SerialName("create_time") val createTimeRaw: String? = null,
    @SerialName("reply_to_nickname") val replyToNicknameRaw: String? = null,
) {
    public val id: String get() = idRaw.orEmpty()

    public val postId: String get() = postIdRaw.orEmpty()

    public val nickname: String get() = nicknameRaw.orEmpty()

    public val avatar: String get() = avatarRaw.orEmpty()

    public val content: String get() = contentRaw.orEmpty()

    /** `json['reply_to_nickname']?.toString()`：无该键或值为 null → null（:21）。 */
    public val replyToNickname: String? get() = replyToNicknameRaw

    /** Dart `createTime`（:20）——字符串解析失败回落 [nowMs]。 */
    public fun createTimeMs(nowMs: Long = communityNowMs()): Long =
        parseCommunityTimeMs(createTimeRaw) ?: nowMs
}

/**
 * Flutter `PostModel`（post_model.dart:1-152）。
 *
 * 同一个类同时吃两种 body：
 * 1. 完整动态（`fromJson`，:26-63）；
 * 2. 点赞片段 `{id, like_count, is_liked}`（`fromLikeFragment`，:66-77）——
 *    片段的其余字段在 Dart 里取构造默认值，本类的「raw 可空 + 解析属性」正好等价：
 *    `userId/nickname/avatar/content = ''`、`publishTime = now`、
 *    `likeCount = 0`、`commentCount = 0`、`isLiked == true` 判定、
 *    `isMine = false`、`previewComments = []`、`source = '来自 iPhone'`、
 *    `mediaType = null`、`isAskEveryone = false`、`topic = null`。
 *    见 [postModelFromLikeFragment]。
 */
@Serializable
public data class PostModel(
    @SerialName("id") val idRaw: String? = null,
    @SerialName("user_id") val userIdRaw: String? = null,
    @SerialName("nickname") val nicknameRaw: String? = null,
    @SerialName("avatar") val avatarRaw: String? = null,
    @SerialName("content") val contentRaw: String? = null,
    @SerialName("publish_time") val publishTimeRaw: String? = null,
    @SerialName("source") val sourceRaw: String? = null,
    @SerialName("images") val imagesRaw: List<String>? = null,
    @SerialName("image_urls") val imageUrlsRaw: List<String>? = null,
    @SerialName("video_url") val videoUrlRaw: String? = null,
    @SerialName("video_cover_url") val videoCoverUrlRaw: String? = null,
    @SerialName("like_count") val likeCountRaw: Int? = null,
    @SerialName("comment_count") val commentCountRaw: Int? = null,
    @SerialName("is_liked") val isLikedRaw: Boolean? = null,
    @SerialName("is_mine") val isMineRaw: Boolean? = null,
    @SerialName("preview_comments") val previewCommentsRaw: List<CommentModel>? = null,
    @SerialName("media_type") val mediaTypeRaw: String? = null,
    @SerialName("is_ask_everyone") val isAskEveryoneRaw: Boolean? = null,
    @SerialName("topic") val topicRaw: TopicModel? = null,
) {
    public val id: String get() = idRaw.orEmpty()

    public val userId: String get() = userIdRaw.orEmpty()

    public val nickname: String get() = nicknameRaw.orEmpty()

    public val avatar: String get() = avatarRaw.orEmpty()

    public val content: String get() = contentRaw.orEmpty()

    /** `json['source']?.toString() ?? '来自 iPhone'`（:49）——空串仍保留为空串。 */
    public val source: String get() = sourceRaw ?: CommunityDefaultSource

    /**
     * Dart `json['images'] ?? json['image_urls']`（:33-36）：
     * 两键都吃、`images` 优先（含 `images` 显式为 null 时也回落到 `image_urls`），
     * 再过滤空串。
     */
    public val images: List<String>
        get() = (imagesRaw ?: imageUrlsRaw).orEmpty().filter { it.isNotEmpty() }

    public val videoUrl: String? get() = videoUrlRaw

    public val videoCoverUrl: String? get() = videoCoverUrlRaw

    public val likeCount: Int get() = likeCountRaw ?: 0

    public val commentCount: Int get() = commentCountRaw ?: 0

    /** `json['is_liked'] == true`（:55）。 */
    public val isLiked: Boolean get() = isLikedRaw == true

    /** `json['is_mine'] == true`（:56）。 */
    public val isMine: Boolean get() = isMineRaw == true

    /** 非 List / 缺键 → 空表，只取 Map 元素（:79-85 `whereType<Map>`）。 */
    public val previewComments: List<CommentModel> get() = previewCommentsRaw.orEmpty()

    /** `json['media_type']?.toString()`：`none`/`image`/`video`，可为 null（:38）。 */
    public val mediaType: String? get() = mediaTypeRaw

    public val topic: TopicModel? get() = topicRaw

    /**
     * `json['is_ask_everyone'] == true || (topic?.isAskEveryone ?? false)`（:59-60）——
     * 顶层标记缺失时由话题兜底。
     */
    public val isAskEveryone: Boolean
        get() = isAskEveryoneRaw == true || topicRaw?.isAskEveryone == true

    /** Dart `publishTime`（:46-48）：字符串解析失败回落 [nowMs]。 */
    public fun publishTimeMs(nowMs: Long = communityNowMs()): Long =
        parseCommunityTimeMs(publishTimeRaw) ?: nowMs

    /** `images.isNotEmpty`（:108）。 */
    public val hasImages: Boolean get() = images.isNotEmpty()

    /** `videoUrl != null && videoUrl.isNotEmpty()`（:109）。 */
    public val hasVideo: Boolean get() = !videoUrl.isNullOrEmpty()
}

/**
 * Dart `PostModel.fromLikeFragment`（post_model.dart:66-77）的 KMP 入口：
 * 只读 `id` / `like_count` / `is_liked`，其余走默认值。
 */
public fun postModelFromLikeFragment(data: JsonElement?): PostModel? =
    decodeEnvelopeData<PostModel>(data)

/**
 * Dart `CommunityUserHit`（community_search_models.dart:5-35）。
 *
 * 注意：`feature/community/MockCommunityEngine.kt` 里另有一个同名的 mock 行类型
 * （`feature.community` 包），本类在 `feature.community.http` 包，二者不冲突；
 * mock 那份不参与序列化，HTTP 契约一律用本类。
 */
@Serializable
public data class CommunityUserHit(
    @SerialName("user_id") val userIdRaw: String? = null,
    @SerialName("nickname") val nicknameRaw: String? = null,
    @SerialName("avatar") val avatarRaw: String? = null,
    @SerialName("is_followed") val isFollowedRaw: Boolean? = null,
) {
    public val userId: String get() = userIdRaw.orEmpty()

    public val nickname: String get() = nicknameRaw.orEmpty()

    public val avatar: String get() = avatarRaw.orEmpty()

    /** `json['is_followed'] == true`（:18）。 */
    public val isFollowed: Boolean get() = isFollowedRaw == true
}

/**
 * Dart `CommunitySearchAllResult`（community_search_models.dart:37-58）——
 * `GET /community/search?type=all` 的 `data`，三个子对象各自是 `ListData`
 * （`list`/`items` + 自带 `pagination`，契约表 #68）。
 *
 * 子对象保持 [JsonElement]，由 [decodeListPage] 归一（`core/network/ListPage.kt`），
 * 这样 `list`/`items`/裸数组/畸形形状都走同一份 A 型分页方言。
 */
@Serializable
public data class CommunitySearchAllResult(
    @SerialName("q") val qRaw: String? = null,
    @SerialName("posts") val postsRaw: JsonElement? = null,
    @SerialName("topics") val topicsRaw: JsonElement? = null,
    @SerialName("users") val usersRaw: JsonElement? = null,
) {
    /** `json['q']?.toString() ?? ''`（:47）。 */
    public val q: String get() = qRaw.orEmpty()

    public val posts: ListPage<PostModel> get() = decodeListPage(postsRaw)

    public val topics: ListPage<TopicModel> get() = decodeListPage(topicsRaw)

    public val users: ListPage<CommunityUserHit> get() = decodeListPage(usersRaw)
}

/**
 * Dart `POST /community/posts` 请求体（http_post_repository.dart:120-131）——
 * **`video_url` / `video_cover_url` 显式为 null 也必须发出去**，`image_urls` 恒发空数组，
 * 因此本 body 只能用 [com.example.my_kmp_project.core.network.NetworkJsonExplicitNulls] 编码。
 */
@Serializable
public data class PostCreateRequest(
    val content: String,
    @SerialName("media_type") val mediaType: String = "none",
    @SerialName("image_urls") val imageUrls: List<String> = emptyList(),
    @SerialName("video_url") val videoUrl: String? = null,
    @SerialName("video_cover_url") val videoCoverUrl: String? = null,
    @SerialName("topic_id") val topicId: String? = null,
    @SerialName("is_ask_everyone") val isAskEveryone: Boolean = false,
    val source: String = CommunityDefaultSource,
)

/**
 * Dart `POST /community/posts/:postId/comments` 请求体
 * （http_post_repository.dart:58-64）：`reply_to_nickname` **为 null 时整键省略**，
 * 所以走默认 [NetworkJson]（`explicitNulls = false`）。
 */
@Serializable
public data class CommentCreateRequest(
    val content: String,
    @SerialName("reply_to_nickname") val replyToNickname: String? = null,
)

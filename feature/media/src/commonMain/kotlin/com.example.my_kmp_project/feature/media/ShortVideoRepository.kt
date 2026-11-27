package com.example.my_kmp_project.feature.media

import com.example.my_kmp_project.core.network.ApiClient
import com.example.my_kmp_project.core.network.ApiResponse
import com.example.my_kmp_project.core.network.NetworkError
import com.example.my_kmp_project.core.network.NetworkJsonExplicitNulls
import com.example.my_kmp_project.core.network.queryOf
import com.example.my_kmp_project.core.network.getListPage
import com.example.my_kmp_project.core.network.deleteApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * 短视频数据源端口 —— Dart `features/video/lib/short_video/repository/short_video_repository.dart:13-34`
 * （abstract class `ShortVideoRepository`）的 KMP 对等。
 *
 * 本包此前缺少数据源端口；本文件即契约层落点，HTTP 实现见 [HttpShortVideoRepository]（#72–#78）。
 */
public interface ShortVideoRepository {

    /**
     * #72 GET `/short-videos/profile`（Dart http_short_video_repository.dart:11-25）：
     * `user_id` 可选 query，null/空串省略（:17）。失败抛 [NetworkError.Business]
     * （Dart `_data(result.data, '加载资料失败')` :24）。
     */
    public suspend fun fetchProfile(userId: String? = null): ShortVideoProfileModel

    /**
     * #73 GET `/short-videos`（Dart http_short_video_repository.dart:28-54）：
     * query `{scope, page = 本地 0-based +1, size = 5, user_id?}`；
     * `hasMore` 取 `pagination.hasMore`，否则 `list.size >= pageSize`
     * （:52，与共享 `ListPage.hasMore(pageSize)` 完全同式）。
     */
    public suspend fun fetchList(
        scope: String,
        userId: String? = null,
        page: Int = 0,
        pageSize: Int = 5,
    ): ShortVideoListPage

    /**
     * #74 POST `/short-videos`（Dart http_short_video_repository.dart:57-79）：
     * `video_url`/`cover_url`/`duration`/`aspect_ratio` 为**显式 null 占位**、
     * `topic_id` 键总发（:65-72），故请求体走 [NetworkJsonExplicitNulls]。
     */
    public suspend fun create(title: String, topicId: String? = null): ShortVideoItemModel

    /**
     * #67 GET `/short-videos/:id`（`short_video/router.go:44`）—— 单条详情。
     *
     * 与 [delete] 共用 `ShortVideoApiPaths.item(id)` 路径，但 **verb 在本方法内显式给定**（GET），
     * 不再由调用方隐式挑选；矩阵 D 区此前「复用 item()、verb 由调用方给」的盲区由此消除。
     */
    public suspend fun get(id: String): ShortVideoItemModel

    /**
     * #67 DELETE `/short-videos/:id`（Dart http_short_video_repository.dart:82-93）：
     * 返回原始 map 且**可能为空**——只看 `code`（`_dataAllowEmpty` :92,159-169）。
     * verb 在本方法内显式给定（DELETE）。
     */
    public suspend fun delete(id: String)

    /**
     * #68 POST `/short-videos/:id/like`（`short_video/router.go:46`）—— 点赞。
     * verb 显式给定（POST），与 [unlike] 分开，不再依赖 `toggleLike` 的布尔参数隐式决定动词。
     */
    public suspend fun like(id: String): ShortVideoItemModel

    /**
     * #68 DELETE `/short-videos/:id/like`（`short_video/router.go:47`）—— 取消点赞。
     * verb 显式给定（DELETE）。
     */
    public suspend fun unlike(id: String): ShortVideoItemModel

    /**
     * #76/#77 like/unlike **同 path 切动词**（Dart http_short_video_repository.dart:96-119）：
     * `liked == true` 走 [like]（POST），否则 [unlike]（DELETE）`/short-videos/:id/like`；
     * 返回 `{id, like_count, is_liked}` 片段（消费方用 [ShortVideoItemModel.withCounts] 就地合并，
     * 不重新拉列表 —— controller :261-275）。
     *
     * 保留为兼容包装：动词本身已由 [like] / [unlike] 显式登记，本方法只做布尔分派。
     */
    public suspend fun toggleLike(id: String, liked: Boolean): ShortVideoItemModel

    /**
     * #78 POST `/short-videos/:id/view`（Dart http_short_video_repository.dart:122-134）：
     * 无 body 的 fire-and-forget，成功回 `{view_count:int}`。
     *
     * 语义合并说明：Dart 仓库在失败/缺 `data` 时抛 `HttpRequestException`，播放页/控制器
     * `catch (_) {}` 全量吞掉（short_video_controller.dart:277-285）。KMP 契约层无 UI 半边，
     * 故本方法**从不抛错**：失败 → `null`（Dart 的「吞掉、不更新」），成功 → 计数
     * （`view_count` 缺失时 0，对齐 repository :133 `?? 0`）。
     */
    public suspend fun reportView(id: String): Int?
}

/**
 * Dart `ShortVideoListPage`（short_video_repository.dart:3-11）：列表 + `hasMore` 两字段。
 */
public data class ShortVideoListPage(
    public val list: List<ShortVideoItemModel>,
    public val hasMore: Boolean,
)

/**
 * Dart `HttpShortVideoRepository` 路径常量（http_short_video_repository.dart:8
 * `static const _prefix = '/api/v1/short-videos'`）。
 */
object ShortVideoApiPaths {
    const val ROOT: String = "/api/v1/short-videos"
    const val PROFILE: String = "$ROOT/profile"

    fun like(id: String): String = "$ROOT/$id/like"
    fun view(id: String): String = "$ROOT/$id/view"
    fun item(id: String): String = "$ROOT/$id"
}

/**
 * #74 建单请求体 —— Dart 总发六键、上传字段占位 null（http_short_video_repository.dart:65-72）；
 * 由 [NetworkJsonExplicitNulls] 编码，null 也保留在 JSON 里。
 */
@Serializable
data class ShortVideoCreateRequest(
    val title: String,
    @SerialName("video_url") val videoUrl: String? = null,
    @SerialName("cover_url") val coverUrl: String? = null,
    val duration: String? = null,
    @SerialName("aspect_ratio") val aspectRatio: Double? = null,
    @SerialName("topic_id") val topicId: String? = null,
)

/**
 * 短视频 HTTP 仓库（契约 #72–#78），逐行对齐 Dart
 * `features/video/lib/short_video/repository/http_short_video_repository.dart`（d7876a1）。
 *
 * 失败映射：Dart `_data`/`_listData`/`_dataAllowEmpty`（:136-169）在 `!isSuccess` 或缺
 * `data` 时抛 `HttpRequestException{message, code}` —— message 取响应 `message`，
 * 空则用各处中文兜底；KMP 对应抛 [NetworkError.Business]（携带 [NetworkError.code]）。
 *
 * 依赖注入默认取 [NetworkFacade.api]（feature 只依赖 façade，不在 feature 内构造引擎）；
 * 测试可注入 fake [ApiClient]。
 */
public class HttpShortVideoRepository(
    private val api: ApiClient,
) : ShortVideoRepository {

    /** #72 —— Dart :11-25。 */
    override suspend fun fetchProfile(userId: String?): ShortVideoProfileModel {
        val response = api.getApi(ShortVideoApiPaths.PROFILE, queryOf("user_id" to userId)) {
            shortVideoProfileFromJson(it)
        }
        return requireData(response, FALLBACK_PROFILE)
    }

    /** #73 —— Dart :28-54。`scope` 是无条件键（Dart :39），`user_id` 空则省略（Dart :42）。 */
    override suspend fun fetchList(
        scope: String,
        userId: String?,
        page: Int,
        pageSize: Int,
    ): ShortVideoListPage {
        val query = mapOf(
            "scope" to scope,
            "page" to (page + 1).toString(),
            "size" to pageSize.toString(),
        ) + queryOf("user_id" to userId)
        val response = api.getListPage<JsonObject>(ShortVideoApiPaths.ROOT, query)
        val data = response.data
        if (!response.isSuccess || data == null) {
            throw businessError(response, FALLBACK_LIST)
        }
        return ShortVideoListPage(
            list = data.items.mapNotNull { shortVideoItemFromJson(it) },
            // Dart :52 `data.pagination?.hasMore ?? (data.list.length >= pageSize)`
            hasMore = data.hasMore(pageSize),
        )
    }

    /** #74 —— Dart :57-79：上传字段显式 null。 */
    override suspend fun create(title: String, topicId: String?): ShortVideoItemModel {
        val body = NetworkJsonExplicitNulls.encodeToString(
            ShortVideoCreateRequest.serializer(),
            ShortVideoCreateRequest(title = title, topicId = topicId),
        )
        val response = api.postApi(ShortVideoApiPaths.ROOT, body) { shortVideoItemFromJson(it) }
        return requireData(response, FALLBACK_CREATE)
    }

    /** #67 GET —— 单条详情；verb 显式（GET），与 [delete] 共用 item 路径。 */
    override suspend fun get(id: String): ShortVideoItemModel {
        val response = api.getApi(ShortVideoApiPaths.item(id)) { shortVideoItemFromJson(it) }
        return requireData(response, FALLBACK_GET)
    }

    /** #67 DELETE —— Dart :82-93：`_dataAllowEmpty` 只看 `code`，data 可为空 map/缺失。verb 显式（DELETE）。 */
    override suspend fun delete(id: String) {
        val response = api.deleteApi(ShortVideoApiPaths.item(id), emptyMap()) { }
        if (!response.isSuccess) {
            throw businessError(response, FALLBACK_DELETE)
        }
    }

    /** #68 POST —— 点赞；verb 显式（POST），`short_video/router.go:46`。 */
    override suspend fun like(id: String): ShortVideoItemModel {
        val response = api.postApi(ShortVideoApiPaths.like(id), "") { shortVideoLikeFragmentFromJson(it) }
        return requireData(response, FALLBACK_TOGGLE)
    }

    /** #68 DELETE —— 取消点赞；verb 显式（DELETE），`short_video/router.go:47`。 */
    override suspend fun unlike(id: String): ShortVideoItemModel {
        val response = api.deleteApi(ShortVideoApiPaths.like(id), emptyMap()) { shortVideoLikeFragmentFromJson(it) }
        return requireData(response, FALLBACK_TOGGLE)
    }

    /** #76/#77 —— 兼容包装：布尔分派到已显式登记动词的 [like] / [unlike]。 */
    override suspend fun toggleLike(id: String, liked: Boolean): ShortVideoItemModel =
        if (liked) like(id) else unlike(id)

    /** #78 —— Dart :122-134 + controller :277-285（吞错语义见接口 KDoc）。 */
    override suspend fun reportView(id: String): Int? = runCatching {
        val response = api.postApi(ShortVideoApiPaths.view(id), "") { el ->
            // Dart (data['view_count'] as num?)?.toInt() ?? 0（:133）；data 非 map 时 _data 抛→吞。
            (el as? JsonObject)?.let { flexibleInt(it["view_count"]) ?: 0 }
        }
        if (response.isSuccess) response.data else null
    }.getOrNull()

    /** Dart `_data`/`_listData`（:136-157）：成功但缺 `data` 同样算失败。 */
    private fun <T> requireData(response: ApiResponse<T>, fallback: String): T {
        val payload = response.data
        if (!response.isSuccess || payload == null) {
            throw businessError(response, fallback)
        }
        return payload
    }

    /** Dart `HttpRequestException{message, code}`：message 空用中文兜底。 */
    private fun businessError(
        response: ApiResponse<*>,
        fallback: String,
    ): NetworkError.Business = NetworkError.Business(
        code = response.code,
        message = response.message?.takeIf { it.isNotBlank() } ?: fallback,
    )

    private companion object {
        const val FALLBACK_PROFILE = "加载资料失败"
        const val FALLBACK_LIST = "加载小视频失败"
        const val FALLBACK_CREATE = "发布失败"
        const val FALLBACK_GET = "加载详情失败"
        const val FALLBACK_DELETE = "删除失败"
        const val FALLBACK_TOGGLE = "操作失败"
    }
}

package com.example.my_kmp_project.feature.community.http

import com.example.my_kmp_project.core.network.ApiClient
import com.example.my_kmp_project.core.network.ApiResponse
import com.example.my_kmp_project.core.network.ListPage
import com.example.my_kmp_project.core.network.NetworkError
import com.example.my_kmp_project.core.network.NetworkFacade
import com.example.my_kmp_project.core.network.NetworkJson
import com.example.my_kmp_project.core.network.NetworkJsonExplicitNulls
import com.example.my_kmp_project.core.network.decodeEnvelopeData
import com.example.my_kmp_project.core.network.decodeListPage
import com.example.my_kmp_project.core.network.deleteApiOk
import com.example.my_kmp_project.core.network.getApi
import com.example.my_kmp_project.core.network.pageQuery
import com.example.my_kmp_project.core.network.postApi
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.encodeToString

/**
 * 社区契约路径表（§2.12 #59–#71）。
 *
 * Dart 前缀 `/api/v1/community`（http_post_repository.dart:11 `_prefix`）；
 * 未发明任何 Dart 没有的端点。**path 参数不做 URL 编码**，与 Dart 的字符串插值
 * （`'$_prefix/posts/$postId/like'`）逐字一致。
 *
 * Dart 每个调用点都先 `AuthHttpConfig.ensureInitialized()`（:19 等）；
 * KMP 侧 `Authorization` / `X-Session-ID` / `X-Device-ID` 由 [NetworkFacade] 的拦截层
 * 统一注入（契约表 §0 Auth 头），因此这里不需要等价调用。
 */
public object CommunityApiPaths {
    /** Dart `_prefix`（http_post_repository.dart:11）。 */
    public const val PREFIX: String = "/api/v1/community"

    /** #59 GET / #60 POST（:22、:121）。 */
    public const val POSTS: String = "$PREFIX/posts"

    /** #66（:148）。 */
    public const val TOPICS: String = "$PREFIX/topics"

    /** #67 —— 与 [SEARCH] 是不同端点（:170）。 */
    public const val TOPICS_SEARCH: String = "$TOPICS/search"

    /** #68 / #69 —— 同一端点靠 `type` 切响应形状（:253、:327）。 */
    public const val SEARCH: String = "$PREFIX/search"

    /** #61 GET / #62 POST（:41、:59）。 */
    public fun postComments(postId: String): String = "$POSTS/$postId/comments"

    /** #63 POST / #64 DELETE（:75）。 */
    public fun postLike(postId: String): String = "$POSTS/$postId/like"

    /** #65 DELETE（:102）。 */
    public fun postById(postId: String): String = "$POSTS/$postId"

    /** #70 POST / #71 DELETE（:221、:235）。 */
    public fun userFollow(userId: String): String = "$PREFIX/users/$userId/follow"
}

/**
 * Flutter `HttpPostRepository`（`features/community/lib/community/repository/http_post_repository.dart`，
 * d7876a1）的 KMP 对等实现，覆盖契约表 #59–#71。
 *
 * 端口见 [CommunityPostRepository]（= Dart `PostRepository`）。
 * 现有 `MockCommunityEngine` 未被改动，也不实现本端口（详见接口 KDoc）。
 *
 * 失败语义与 Dart 一致：`code != 0` **或** `data` 缺失都抛 [NetworkError.Business]
 * （Dart `_list` / `_data` / `_listData` 抛 `HttpRequestException`，:184-214、:343-354），
 * message 优先用服务端文案，空文案回落到 Dart 同款中文兜底串。
 * 例外是 #65 / #70 / #71：Dart `_dataAllowEmpty`（:204-214）**只看 `code`**，
 * `data` 为空 map 或 null 都算成功。
 *
 * @param client 注入 client（单测用）；线上走 [NetworkFacade.api()]，
 *   每次调用现取，保证 token 刷新后拿到的是重建后的 client。
 */
public class HttpCommunityPostRepository(
    private val client: ApiClient? = null,
) : CommunityPostRepository {

    private val api: ApiClient get() = client ?: NetworkFacade.api()

    /** #59 分页视图 —— `page = 本地页 + 1`、`size`、`tab`（Dart :21-33），保留完整 [ListPage]。 */
    override suspend fun fetchPostsPage(
        page: Int,
        pageSize: Int,
        tab: String,
    ): ListPage<PostModel> =
        listPage<PostModel>(
            path = CommunityApiPaths.POSTS,
            query = communityQuery(page + 1, pageSize, "tab" to tab),
            fallback = "加载动态失败",
        )

    /** #59 列表兼容视图：与分页视图同一请求，只取 items。 */
    override suspend fun fetchPosts(
        page: Int,
        pageSize: Int,
        tab: String,
    ): List<PostModel> = fetchPostsPage(page, pageSize, tab).items

    /** #61 —— Dart 写死 `page=1&size=50`（:40-42），返回 `.list`。 */
    override suspend fun fetchComments(postId: String): List<CommentModel> =
        listPage<CommentModel>(
            path = CommunityApiPaths.postComments(postId),
            query = pageQuery(page = 1, size = 50),
            fallback = "加载评论失败",
        ).items

    /** #62 —— `{content, reply_to_nickname?}`，`reply_to_nickname` 为 null 时整键省略（:58-64）。 */
    override suspend fun addComment(
        postId: String,
        content: String,
        replyToNickname: String?,
    ): CommentModel =
        api.postApi<CommentCreateRequest, CommentModel>(
            path = CommunityApiPaths.postComments(postId),
            body = CommentCreateRequest(
                content = content,
                replyToNickname = replyToNickname,
            ),
        ).requireData("评论失败")

    /**
     * #63 / #64 —— 同一条 path 切动词：`liked == true` 走 POST，否则 DELETE（:73-95）。
     * 无请求体（Dart 也不发 body）；响应是 `{id, like_count, is_liked}` 片段，
     * 用 [postModelFromLikeFragment]（Dart `PostModel.fromLikeFragment`）解码。
     */
    override suspend fun toggleLike(postId: String, liked: Boolean): PostModel {
        val path = CommunityApiPaths.postLike(postId)
        val response: ApiResponse<PostModel> = if (liked) {
            api.postApi(path = path, body = "", parseData = ::decodePostFragment)
        } else {
            api.deleteApi(path = path, query = emptyMap(), parseData = ::decodePostFragment)
        }
        return response.requireData("操作失败")
    }

    /**
     * #65 —— `DELETE /community/posts/:postId`，无 body。
     * Dart `_dataAllowEmpty` 只看 `code`（:98-109），`data` 为 `null` 或 `{}` 都成功。
     */
    override suspend fun deletePost(postId: String) {
        requireCodeOnly(
            response = api.deleteApiOk(CommunityApiPaths.postById(postId)),
            fallback = "删除失败",
        )
    }

    /**
     * #60 —— `POST /community/posts`，body 逐键对齐 Dart :120-131：
     * `content` / `media_type` / **恒发 `image_urls: []`** / **显式 `video_url: null`** /
     * **显式 `video_cover_url: null`** / `topic_id`（可 null 但仍发）/ `is_ask_everyone` / `source`。
     *
     * 因此必须用 [NetworkJsonExplicitNulls] 编码（默认 [NetworkJson] 关掉了 `explicitNulls`，
     * 会把三个 null 键整体丢掉）。
     */
    override suspend fun createPost(
        content: String,
        mediaType: String,
        topicId: String?,
        isAskEveryone: Boolean,
        source: String,
    ): PostModel {
        val body = PostCreateRequest(
            content = content,
            mediaType = mediaType,
            topicId = topicId,
            isAskEveryone = isAskEveryone,
            source = source,
        )
        val response: ApiResponse<PostModel> = api.postApi(
            path = CommunityApiPaths.POSTS,
            body = NetworkJsonExplicitNulls.encodeToString(body),
            parseData = ::decodePostFragment,
        )
        return response.requireData("发布失败")
    }

    /** #66 —— `page = 本地页 + 1`、`size = 20`（:141-158）。 */
    override suspend fun fetchTopics(
        page: Int,
        pageSize: Int,
    ): List<TopicModel> =
        listPage<TopicModel>(
            path = CommunityApiPaths.TOPICS,
            query = communityQuery(page + 1, pageSize),
            fallback = "加载话题失败",
        ).items

    /** #67 —— `/community/topics/search?q=&page=&size=`，`q` **总发**（含空串，:169-181）。 */
    override suspend fun searchTopics(
        query: String,
        page: Int,
        pageSize: Int,
    ): List<TopicModel> =
        listPage<TopicModel>(
            path = CommunityApiPaths.TOPICS_SEARCH,
            query = communityQuery(page + 1, pageSize, "q" to query),
            fallback = "搜索话题失败",
        ).items

    /**
     * #70 —— `POST /community/users/:userId/follow`，无 body，`data` 原样丢弃
     * （Dart `_dataAllowEmpty`，:216-228）。
     */
    override suspend fun followUser(userId: String) {
        val response: ApiResponse<Unit> = api.postApi(
            path = CommunityApiPaths.userFollow(userId),
            body = "",
            parseData = { null },
        )
        requireCodeOnly(response, "关注失败")
    }

    /** #71 —— 与 [followUser] 同 path，只把动词换成 DELETE（:230-242）。 */
    override suspend fun unfollowUser(userId: String) {
        requireCodeOnly(
            response = api.deleteApiOk(CommunityApiPaths.userFollow(userId)),
            fallback = "取消关注失败",
        )
    }

    /** #68 —— `type=all`、`page+1`、`size=5`；`data` 是 `{q, posts, topics, users}`（:244-266）。 */
    override suspend fun searchAll(
        q: String,
        page: Int,
        pageSize: Int,
    ): CommunitySearchAllResult =
        api.getApi<CommunitySearchAllResult>(
            path = CommunityApiPaths.SEARCH,
            query = communityQuery(page + 1, pageSize, "q" to q, "type" to "all"),
        ).requireData("搜索失败")

    /** #69 —— `type=post`（:268-282）。 */
    override suspend fun searchPosts(
        q: String,
        page: Int,
        pageSize: Int,
    ): ListPage<PostModel> = searchPage<PostModel>(q, "post", page, pageSize, "搜索动态失败")

    /** #69 —— `type=topic`（:284-298）。 */
    override suspend fun searchTopicsPage(
        q: String,
        page: Int,
        pageSize: Int,
    ): ListPage<TopicModel> = searchPage<TopicModel>(q, "topic", page, pageSize, "搜索话题失败")

    /** #69 —— `type=user`（:300-314）。 */
    override suspend fun searchUsers(
        q: String,
        page: Int,
        pageSize: Int,
    ): ListPage<CommunityUserHit> =
        searchPage<CommunityUserHit>(q, "user", page, pageSize, "搜索用户失败")

    /**
     * Dart `_searchPage`（:316-341）：**同一个** `GET /community/search`，
     * 靠 `type` 把响应切成单个 `ListData<T>`；`PageResult.fromListData(pageSize)`
     * 在 KMP 由 [ListPage.hasMore] 承担，故直接返回 [ListPage]。
     */
    private suspend inline fun <reified T> searchPage(
        q: String,
        type: String,
        page: Int,
        pageSize: Int,
        fallback: String,
    ): ListPage<T> = listPage<T>(
        path = CommunityApiPaths.SEARCH,
        query = communityQuery(page + 1, pageSize, "q" to q, "type" to type),
        fallback = fallback,
    )

    /**
     * Dart `_list` / `_listData`：`code != 0` **或 `data` 缺失/为 null** 都算失败；
     * 成功时把 `data` 交给 `core/network` 的 `decodeListPage`（A 型分页方言：
     * `list` / `items` / 裸数组 + `pagination`）。
     *
     * 这里没有直接用 `getListPage<T>()`，因为 `decodeListPage` 会把畸形/缺失的 `data`
     * 归一成「空页」，而 Dart 在这种 body 下是抛错的 —— 先拿住 `data` 元素再解码才能保住该语义。
     */
    private suspend inline fun <reified T> listPage(
        path: String,
        query: Map<String, String>,
        fallback: String,
    ): ListPage<T> {
        val response: ApiResponse<JsonElement?> = api.getApi(path, query) { element -> element }
        if (!response.isSuccess) throw communityFailure(response.code, response.message, fallback)
        val data = response.data ?: throw communityFailure(response.code, response.message, fallback)
        return decodeListPage<T>(data)
    }

    private fun <T> ApiResponse<T>.requireData(fallback: String): T {
        if (!isSuccess) throw communityFailure(code, message, fallback)
        return data ?: throw communityFailure(code, message, fallback)
    }

    /** Dart `_dataAllowEmpty`（:204-214）：只校验 `code`，`data` 可为 null / 空 map。 */
    private fun requireCodeOnly(response: ApiResponse<*>, fallback: String) {
        if (!response.isSuccess) {
            throw communityFailure(response.code, response.message, fallback)
        }
    }
}

/** [PostModel] 解码入口：完整动态与点赞片段共用一套默认值（见 [postModelFromLikeFragment]）。 */
private fun decodePostFragment(element: JsonElement?): PostModel? =
    decodeEnvelopeData<PostModel>(element)

/**
 * Dart `_list` / `_data` / `_dataAllowEmpty` 的抛错入口（http_post_repository.dart:184-214）：
 * 服务端 message 非空则原样透传，空文案回落到调用点给的中文兜底串。
 */
fun communityFailure(
    code: Int,
    message: String?,
    fallback: String,
): NetworkError.Business = NetworkError.Business(
    code = code,
    message = message?.takeIf { it.isNotBlank() } ?: fallback,
)

/**
 * Dart 的 query 一律「无条件带上」（`'q': query` 即使是空串也发），
 * 因此 page/size 走 `pageQuery`，其余键显式补齐、不套用 `queryOf` 的空串省略规则。
 *
 * [page] 已是服务端 **1-based**（调用方传 `localPage + 1`）。
 */
private fun communityQuery(
    page: Int,
    size: Int,
    vararg required: Pair<String, String>,
): Map<String, String> = pageQuery(page = page, size = size) + required.toMap()

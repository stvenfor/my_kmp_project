package com.example.my_kmp_project.feature.community.http

import com.example.my_kmp_project.core.network.ListPage

/**
 * 社区仓库端口 —— Dart `features/community/lib/community/repository/post_repository.dart:7-73`
 * 的 1:1 对等接口（14 个方法，契约表 §2.12 #59–#71）。
 *
 * 约定与 Dart 完全一致：
 * - **所有 `page` 入参都是本地 0-based**，实现内部再 `+1` 变成服务端的 1-based
 *   （post_repository.dart:8、:36；http_post_repository.dart:24、:150、:173、:257、:331）。
 * - `searchPosts` / `searchTopicsPage` / `searchUsers` 返回带分页的 [ListPage]
 *   （Dart `PageResult<T>`：`list` + `hasMore` + `pagination`，result_model.dart:139-157；
 *   KMP 用 `core/network/ListPage.kt` 的 `hasMore(pageSize)` 归一，不再造一个 PageResult）。
 * - 动态流同时提供分页视图 [fetchPostsPage] 与列表兼容视图 [fetchPosts]；
 *   其余列表方法仍是 `List<T>`（`fetchComments` / `fetchTopics` / `searchTopics` 忽略 `pagination`）。
 * - 失败一律抛出异常（Dart 的 `_list` / `_data` 抛 `HttpRequestException`，
 *   http_post_repository.dart:184-214）；KMP 用 `core/network` 的 [com.example.my_kmp_project.core.network.NetworkError.Business]。
 *
 * 本仓库现有的 `MockCommunityEngine`（`feature/community/MockCommunityEngine.kt`）**不实现本接口**：
 * 它是同步方法 + mock 私有行类型（`CommunityFeedItem` / `CommunityFeedComment`），
 * 且没有 `deletePost` / `fetchTopics` / 分页搜索三兄弟。保持原样未改动，
 * 后续 UI 收口时再决定是「适配 mock 到本端口」还是「直接切 HTTP 实现」。
 */
public interface CommunityPostRepository {

    /**
     * #59 `GET /community/posts` 的**分页视图**（Dart post_repository.dart:10-14）。
     *
     * [tab]：`latest` | `hot` | `following`（社区顶栏三档）。
     * 返回服务端分页元数据（[ListPage.pagination] / [ListPage.hasMore]），供 UI 消费；
     * [page] 仍是本地 0-based，由实现内部 +1 变服务端 1-based。
     */
    public suspend fun fetchPostsPage(
        page: Int,
        pageSize: Int = 10,
        tab: String = "latest",
    ): ListPage<PostModel>

    /**
     * #59 的**列表兼容视图**：只取 [fetchPostsPage] 的 [ListPage.items]，
     * 供尚未迁移到分页元数据的调用方使用；契约（page/size/tab）与分页视图完全一致。
     */
    public suspend fun fetchPosts(
        page: Int,
        pageSize: Int = 10,
        tab: String = "latest",
    ): List<PostModel>

    /**
     * #61 `GET /community/posts/:postId/comments`（post_repository.dart:16）。
     * Dart 侧写死 `page=1&size=50`，端口不暴露分页参数。
     */
    public suspend fun fetchComments(postId: String): List<CommentModel>

    /** #62 `POST /community/posts/:postId/comments`（post_repository.dart:18-22）。 */
    public suspend fun addComment(
        postId: String,
        content: String,
        replyToNickname: String? = null,
    ): CommentModel

    /**
     * #63 / #64 点赞与取消点赞（post_repository.dart:24）。
     *
     * **同一条 path 只切动词**：`liked == true` → `POST /community/posts/:id/like`，
     * `liked == false` → `DELETE` 同路径（http_post_repository.dart:75-93、§5.10 动词不对称）。
     * 返回的是 `{id, like_count, is_liked}` 片段 → [PostModel]（`fromLikeFragment`）。
     */
    public suspend fun toggleLike(postId: String, liked: Boolean): PostModel

    /** #65 `DELETE /community/posts/:postId`（post_repository.dart:26）；`data` 可为 null/空 map。 */
    public suspend fun deletePost(postId: String)

    /**
     * #60 `POST /community/posts`（post_repository.dart:28-34）。
     *
     * 默认值与 Dart 逐字对齐：`mediaType = 'none'`、`source = '来自 iPhone'`、
     * 请求体恒发 `image_urls: []` 且 `video_url` / `video_cover_url` 显式为 null。
     */
    public suspend fun createPost(
        content: String,
        mediaType: String = "none",
        topicId: String? = null,
        isAskEveryone: Boolean = false,
        source: String = CommunityDefaultSource,
    ): PostModel

    /** #66 `GET /community/topics`（post_repository.dart:37）。 */
    public suspend fun fetchTopics(
        page: Int = 0,
        pageSize: Int = 20,
    ): List<TopicModel>

    /**
     * #67 `GET /community/topics/search`（post_repository.dart:39-43）。
     *
     * 与综合搜索 `GET /community/search?type=topic`（[searchTopicsPage]）是**两个不同端点**。
     */
    public suspend fun searchTopics(
        query: String,
        page: Int = 0,
        pageSize: Int = 20,
    ): List<TopicModel>

    /** #70 `POST /community/users/:userId/follow`（post_repository.dart:46）。 */
    public suspend fun followUser(userId: String)

    /** #71 `DELETE /community/users/:userId/follow`（post_repository.dart:47）；与关注同 path 切动词。 */
    public suspend fun unfollowUser(userId: String)

    /**
     * #68 综合搜索 `GET /community/search?type=all`（post_repository.dart:50-54）。
     * 一份 body 里同时给 posts / topics / users 三个独立分页子对象。
     */
    public suspend fun searchAll(
        q: String,
        page: Int = 0,
        pageSize: Int = 5,
    ): CommunitySearchAllResult

    /** #69 分区搜索 `GET /community/search?type=post`（post_repository.dart:56-60）。 */
    public suspend fun searchPosts(
        q: String,
        page: Int = 0,
        pageSize: Int = 10,
    ): ListPage<PostModel>

    /** #69 分区搜索 `GET /community/search?type=topic`（post_repository.dart:62-66）。 */
    public suspend fun searchTopicsPage(
        q: String,
        page: Int = 0,
        pageSize: Int = 10,
    ): ListPage<TopicModel>

    /** #69 分区搜索 `GET /community/search?type=user`（post_repository.dart:68-72）。 */
    public suspend fun searchUsers(
        q: String,
        page: Int = 0,
        pageSize: Int = 10,
    ): ListPage<CommunityUserHit>
}

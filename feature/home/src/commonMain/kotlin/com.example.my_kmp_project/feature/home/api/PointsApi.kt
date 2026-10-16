package com.example.my_kmp_project.feature.home.api

import com.example.my_kmp_project.core.network.ApiClient
import com.example.my_kmp_project.core.network.NetworkFacade
import com.example.my_kmp_project.core.network.getApi
import com.example.my_kmp_project.core.network.postApiDecoded

/**
 * Flutter `PointsApi`（`features/home/lib/home/api/points_api.dart`，d7876a1）。
 * 契约表 #22–#25。
 *
 * **不含 #26**（`GET /mall/stores/:storeId/products`）：该端点归 commerce lane
 * （`PointsGiftItem` / `MallProductCardModel` 在契约表 §6 已要求合并成一个商品 DTO），
 * 本类不重复声明，避免两条 lane 解同一个端点。
 *
 * 幂等提示（契约表 §5.3）：`#23 check-in`、`#25 claim` 都是**无 body 的动作 POST**，
 * 服务端不去重，重复点击只能由 UI 侧禁用按钮防住。
 */
public class PointsApi(
    private val client: ApiClient = NetworkFacade.api(),
) {
    public companion object {
        /** Dart `PointsApi.statusPath`（points_api.dart:7）。 */
        internal const val STATUS_PATH: String = "/api/v1/points/status"

        /** Dart `PointsApi.checkInPath`（:8）。 */
        internal const val CHECK_IN_PATH: String = "/api/v1/points/check-in"

        /** Dart `PointsApi.tasksPath`（:9）；#25 在其后拼 `/:code/claim`。 */
        internal const val TASKS_PATH: String = "/api/v1/points/tasks"
    }

    /** #22 `GET /points/status` —— `today_reward` / `reward` 缺省都是 5。 */
    public suspend fun fetchStatus(): PointsStatus =
        client.getApi<PointsStatus>(STATUS_PATH).homeDataOrThrow("加载签到状态失败")

    /** #23 `POST /points/check-in` —— 无 body（points_api.dart:26）。 */
    public suspend fun checkIn(): CheckInResult =
        client.postApiDecoded<CheckInResult>(CHECK_IN_PATH, body = "")
            .homeDataOrThrow("签到失败")

    /**
     * #24 `GET /points/tasks`。
     *
     * Dart 手搓转换器（points_api.dart:42-59）：`data` 是 map 时读 `data['items']`，
     * 否则读**根** `items`；body 缺 `code` 时**合成 `code:0`**；且永远返回 list，
     * 不会因为 `data` 缺失而抛。该形状由 [getHomeListPage] 的
     * `rootFallbackWhenDataMissing = true` 精确覆盖（契约表 §3 B 型）。
     */
    public suspend fun fetchTasks(): List<GrowthTask> =
        client.getHomeListPage<GrowthTask>(TASKS_PATH, rootFallbackWhenDataMissing = true)
            .homeDataOrThrow("加载成长任务失败")
            .items

    /**
     * #25 `POST /points/tasks/:code/claim` —— 无 body；
     * [code] 是**字符串任务码**（Dart `claimTask(String code)`，:65-77）。
     */
    public suspend fun claimTask(code: String): ClaimTaskResult =
        client.postApiDecoded<ClaimTaskResult>("$TASKS_PATH/$code/claim", body = "")
            .homeDataOrThrow("领取失败")
}

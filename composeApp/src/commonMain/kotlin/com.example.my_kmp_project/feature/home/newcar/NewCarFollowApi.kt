package com.example.my_kmp_project.feature.home.newcar

import com.example.my_kmp_project.core.network.ApiClient
import com.example.my_kmp_project.core.network.NetworkFacade
import com.example.my_kmp_project.core.network.NetworkJson
import com.example.my_kmp_project.core.network.getApi
import com.example.my_kmp_project.core.network.pageQuery
import com.example.my_kmp_project.core.network.patchApi
import com.example.my_kmp_project.core.network.postApi
import com.example.my_kmp_project.core.network.postApiDecoded
import com.example.my_kmp_project.feature.home.api.getHomeListPageOrNull
import com.example.my_kmp_project.feature.home.api.homeBareDataOrThrow
import com.example.my_kmp_project.feature.home.api.homeRequestedPageHasMore
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Flutter `NewCarFollowApi`（`features/home/lib/new_car_follow/api/new_car_follow_api.dart`，
 * d7876a1）。契约表 #40–#46。
 *
 * 失败语义：Dart 的 `_data()`（:174-179）抛**裸** `Exception(message ?? fallback)`，
 * 不是 `HttpRequestException` —— 这里用 [homeBareDataOrThrow] 保留该差异（契约表 §2.8）。
 */
public class NewCarFollowApi(
    private val client: ApiClient = NetworkFacade.api(),
) {
    public companion object {
        /** Dart `NewCarFollowApi._base`（new_car_follow_api.dart:7）。 */
        internal const val BASE_PATH: String = "/api/v1/new-car-follow-files"

        /** Dart `fetchList(size = 10)`（:26）。 */
        internal const val DEFAULT_LIST_SIZE: Int = 10

        /** Dart `fetchLogs(size = 20)`（:123）。 */
        internal const val DEFAULT_LOG_SIZE: Int = 20
    }

    /** #40 `GET /new-car-follow-files/summary`。 */
    public suspend fun fetchSummary(): NewCarFollowSummary =
        client.getApi<NewCarFollowSummary>("$BASE_PATH/summary")
            .homeBareDataOrThrow("加载摘要失败")

    /**
     * #41 `GET /new-car-follow-files?page=&size=` + tab 过滤。
     *
     * [page] 服务端 **1-based**（Dart `new_car_follow_list_viewmodel.dart:63` 首次就传 1）；
     * tab 过滤发**中文字面量** `intent_band=高|中|低` 或 `overdue=1`，`all` 不带
     * （`new_car_follow_models.dart:19-25` + `new_car_follow_api.dart:29-30`）。
     */
    public suspend fun fetchList(
        tab: NewCarFollowTab,
        page: Int,
        size: Int = DEFAULT_LIST_SIZE,
    ): NewCarFollowPage<NewCarFollowFile> {
        val pageData = client.getHomeListPageOrNull<NewCarFollowFile>(
            BASE_PATH,
            pageQuery(page, size) + tab.query,
        ).homeBareDataOrThrow("加载列表失败")
        return NewCarFollowPage(
            list = pageData.items,
            hasMore = homeRequestedPageHasMore(page, size, pageData.pagination, pageData.items.size),
        )
    }

    /** #42 `GET /new-car-follow-files/:fileId`（[fileId] 是字符串主键）。 */
    public suspend fun fetchDetail(fileId: String): NewCarFollowFile =
        client.getApi<NewCarFollowFile>("$BASE_PATH/$fileId")
            .homeBareDataOrThrow("加载详情失败")

    /**
     * #43 `POST /new-car-follow-files` —— **互斥分支**（new_car_follow_api.dart:70-82）：
     *
     * - `customerId != null && customerId > 0` → 只发 `customer_id`，**省略** `display_name` / `phone` 两个键；
     * - 否则 → `display_name` 与 `phone` **两键总发**，即使值为 null（显式 null，
     *   等价于 Dart 把 null 放进 map 后由 Dio 编码）；
     * - `vehicle_interest` 仅 trim 后非空才发；`next_follow_up_at` 仅非空串才发。
     *
     * 因为同一份 body 里「显式 null」与「省略键」两种策略并存，这里用 [buildJsonObject]
     * 逐键落笔，而不是单个 `@Serializable` 请求体。
     */
    public suspend fun create(
        followLevel: String,
        customerId: Int? = null,
        displayName: String? = null,
        phone: String? = null,
        vehicleInterest: String? = null,
        nextFollowUpAt: String? = null,
    ): NewCarFollowFile {
        val body: String = NetworkJson.encodeToString(
            buildJsonObject {
                put("follow_level", followLevel)
                if (customerId != null && customerId > 0) {
                    put("customer_id", customerId)
                } else {
                    // null 也落键：`put(key, String?)` 写入 JsonNull（本工程 kotlinx 版本无 putNull）
                    put("display_name", displayName)
                    put("phone", phone)
                }
                vehicleInterest?.trim()?.takeIf { it.isNotEmpty() }?.let { put("vehicle_interest", it) }
                nextFollowUpAt?.takeIf { it.isNotEmpty() }?.let { put("next_follow_up_at", it) }
            },
        )
        return client.postApiDecoded<NewCarFollowFile>(BASE_PATH, body)
            .homeBareDataOrThrow("建档失败")
    }

    /**
     * #44 `PATCH /new-car-follow-files/:fileId` —— **稀疏 body**：null 键整键省略
     * （new_car_follow_api.dart:103-106；`NetworkJson.explicitNulls = false` 正好等价）。
     *
     * [stage] 取值 `new/following/test_drive/quoted/won/lost`，[followLevel] 取值 `H/A/B/E`。
     */
    public suspend fun patch(
        fileId: String,
        followLevel: String? = null,
        stage: String? = null,
        nextFollowUpAt: String? = null,
    ): NewCarFollowFile =
        client.patchApi<NewCarFollowPatchRequest, NewCarFollowFile>(
            "$BASE_PATH/$fileId",
            NewCarFollowPatchRequest(followLevel, stage, nextFollowUpAt),
        ).homeBareDataOrThrow("更新失败")

    /** #45 `GET /new-car-follow-files/:fileId/logs?page=&size=`（page 1-based，:120-139）。 */
    public suspend fun fetchLogs(
        fileId: String,
        page: Int = 1,
        size: Int = DEFAULT_LOG_SIZE,
    ): NewCarFollowPage<NewCarFollowLog> {
        val pageData = client.getHomeListPageOrNull<NewCarFollowLog>(
            "$BASE_PATH/$fileId/logs",
            pageQuery(page, size),
        ).homeBareDataOrThrow("加载流水失败")
        return NewCarFollowPage(
            list = pageData.items,
            hasMore = homeRequestedPageHasMore(page, size, pageData.pagination, pageData.items.size),
        )
    }

    /**
     * #46 `POST /new-car-follow-files/:fileId/logs`（new_car_follow_api.dart:142-166）。
     *
     * body：`body` 必发；`follow_level` 仅**非空串**才发；`next_follow_up_at` 只要**非 null**
     * 就发（空串也发 —— 两个键的省略条件不同，见 :150-154）。
     *
     * 写流水可以由服务端移动档案的 `follow_level` / `next_follow_up_at`，
     * 因此本方法只回服务端解码的这条流水，**不做本地乐观改写**。
     */
    public suspend fun createLog(
        fileId: String,
        body: String,
        followLevel: String? = null,
        nextFollowUpAt: String? = null,
    ): NewCarFollowLog =
        client.postApi<NewCarFollowCreateLogRequest, NewCarFollowLog>(
            "$BASE_PATH/$fileId/logs",
            NewCarFollowCreateLogRequest(
                body = body,
                followLevel = followLevel?.takeIf { it.isNotEmpty() },
                nextFollowUpAt = nextFollowUpAt,
            ),
        ).homeBareDataOrThrow("写流水失败")
}

/** #44 稀疏 PATCH body（三键全可省，Dart `patch()` :103-106）。 */
@Serializable
internal data class NewCarFollowPatchRequest(
    @SerialName("follow_level") val followLevel: String? = null,
    @SerialName("stage") val stage: String? = null,
    @SerialName("next_follow_up_at") val nextFollowUpAt: String? = null,
)

/**
 * #46 写流水 body。
 *
 * `follow_level` 用 `null` 表达「省略」（Dart 还额外要求非空串，调用点已归一）；
 * `next_follow_up_at` 保留空串，因为 Dart 只判 `!= null`（new_car_follow_api.dart:153）。
 */
@Serializable
internal data class NewCarFollowCreateLogRequest(
    @SerialName("body") val body: String,
    @SerialName("follow_level") val followLevel: String? = null,
    @SerialName("next_follow_up_at") val nextFollowUpAt: String? = null,
)

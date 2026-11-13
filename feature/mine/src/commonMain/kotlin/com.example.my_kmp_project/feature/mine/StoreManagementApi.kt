package com.example.my_kmp_project.feature.mine

import com.example.my_kmp_project.core.network.ApiClient
import com.example.my_kmp_project.core.network.ApiResponse
import com.example.my_kmp_project.core.network.NetworkFacade
import com.example.my_kmp_project.core.network.decodeEnvelopeData
import com.example.my_kmp_project.core.network.deleteApi
import com.example.my_kmp_project.core.network.postApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * 门店 / 成员管理写接口契约（4 条 `missing(待接线)`）。
 *
 * 真相源改为 **Go 后端 my_go_study**：
 * - `POST /api/v1/stores` —— `access/router.go:40` / `access/handler.go:27` CreateStore，body `{store_id,name}`；
 * - `POST /api/v1/stores/:store_id/join-applications` —— `home/router.go:48` / `home/handler.go:40`
 *   ApplyToStore，**无 body**（身份取会话用户、`store_id` 取路径参数）；
 * - `POST /api/v1/stores/:store_id/members` —— `access/router.go:41` / `access/handler.go:50`
 *   UpsertMember，body `{target_user_id,position}`；
 * - `DELETE /api/v1/stores/:store_id/members/:user_id` —— `access/router.go:42` / `access/handler.go:78`
 *   RemoveMember，**无 body**，响应 `{ok:true}`。
 *
 * 这几条是 access 域门店/角色管理接口，客户端 KMP 暂未接线（矩阵 B 区 #15–#18）。
 * 仿 [PaymentPrepayApi] 样板：**只登记后端真实路由契约**（路径常量 + 请求/响应模型 +
 * 可调用方法 + 契约测试），不自造 UI 调用点，也不改后端仓库。
 * 归属：放在 `feature/mine`（门店/店铺管理属「我的」域），不新建 Gradle 模块。
 */
public class StoreManagementApi(
    private val client: ApiClient = NetworkFacade.api(),
) {
    /** `POST /api/v1/stores`（`access/router.go:40`）。 */
    public suspend fun createStore(request: StoreCreateRequest): ApiResponse<StoreMutationResponse> =
        client.postApi<StoreCreateRequest, StoreMutationResponse>(
            path = StoreManagementPaths.STORES,
            body = request,
        )

    /** `POST /api/v1/stores/:store_id/join-applications`（`home/router.go:48`），无 body。 */
    public suspend fun applyToStore(storeId: Int): ApiResponse<StoreMutationResponse> =
        client.postApi(
            path = StoreManagementPaths.joinApplications(storeId),
            body = "",
        ) { el: JsonElement? -> decodeEnvelopeData<StoreMutationResponse>(el) }

    /** `POST /api/v1/stores/:store_id/members`（`access/router.go:41`）。 */
    public suspend fun upsertMember(storeId: Int, request: StoreUpsertMemberRequest): ApiResponse<StoreMutationResponse> =
        client.postApi<StoreUpsertMemberRequest, StoreMutationResponse>(
            path = StoreManagementPaths.members(storeId),
            body = request,
        )

    /** `DELETE /api/v1/stores/:store_id/members/:user_id`（`access/router.go:42`），无 body。 */
    public suspend fun removeMember(storeId: Int, userId: String): ApiResponse<StoreMutationResponse> =
        client.deleteApi<StoreMutationResponse>(StoreManagementPaths.member(storeId, userId))
}

/** 门店管理路径常量（对齐 `access/router.go:40-42` 与 `home/router.go:48`）。 */
public object StoreManagementPaths {
    /** `POST /api/v1/stores`。 */
    public const val STORES: String = "/api/v1/stores"

    /** `POST /api/v1/stores/:store_id/join-applications`。 */
    public fun joinApplications(storeId: Int): String = "$STORES/$storeId/join-applications"

    /** `POST /api/v1/stores/:store_id/members`。 */
    public fun members(storeId: Int): String = "$STORES/$storeId/members"

    /** `DELETE /api/v1/stores/:store_id/members/:user_id`。 */
    public fun member(storeId: Int, userId: String): String = "$STORES/$storeId/members/$userId"
}

/** `POST /stores` body（`access/handler.go:33-36`）。 */
@Serializable
public data class StoreCreateRequest(
    @SerialName("store_id") val storeId: Int,
    val name: String,
)

/** `POST /stores/:store_id/members` body（`access/handler.go:61-64`，注意键是 `target_user_id`）。 */
@Serializable
public data class StoreUpsertMemberRequest(
    @SerialName("target_user_id") val targetUserId: String,
    val position: Int,
)

/** 四个写端点的 `data`：后端返回各自实体或 `{ok:true}`，这里用宽松可空字段透传。 */
@Serializable
public data class StoreMutationResponse(
    @SerialName("store_id") val storeId: Int = 0,
    val name: String = "",
    val ok: Boolean = false,
)

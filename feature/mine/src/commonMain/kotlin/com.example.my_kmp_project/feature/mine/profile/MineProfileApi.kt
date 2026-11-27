package com.example.my_kmp_project.feature.mine.profile

import com.example.my_kmp_project.core.network.ApiClient
import com.example.my_kmp_project.core.network.ApiEnvelope
import com.example.my_kmp_project.core.network.ApiResponse
import com.example.my_kmp_project.core.network.NetworkError
import com.example.my_kmp_project.core.network.NetworkJson
import com.example.my_kmp_project.core.network.getApi
import com.example.my_kmp_project.core.network.postApi
import com.example.my_kmp_project.core.network.queryOf
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Mine 个人资料 / 门店切换的实际后端契约（`ApiClient` 由 App 层注入）。
 *
 * - `GET|PATCH /api/v1/me/profile`：Go `response.BackendJSON` 直出（非信封），snake_case；
 * - `GET /api/v1/me/stores`：标准信封，`data.list` + `data.current_store_id`；
 * - `POST /api/v1/profiles/me/store`：标准信封，`data` 为切换后的门店统计。
 */
public object MineProfilePaths {
    public const val PROFILE: String = "/api/v1/me/profile"
    public const val STORES: String = "/api/v1/me/stores"
    public const val SWITCH_STORE: String = "/api/v1/profiles/me/store"
}

/** Go `response.ProfileStatsItem`（snake_case）。 */
@Serializable
public data class MineStoreStats(
    @SerialName("store_id") val storeId: Int = 0,
    @SerialName("store_name") val storeName: String = "",
    @SerialName("days_joined") val daysJoined: Int = 0,
    @SerialName("employee_count") val employeeCount: Int = 0,
    @SerialName("store_days") val storeDays: Int = 0,
    @SerialName("total_customers") val totalCustomers: Int = 0,
    @SerialName("role") val role: Int? = null,
    @SerialName("role_label") val roleLabel: String = "",
) {
    public val resolvedRoleLabel: String
        get() = roleLabel.ifBlank {
            when (role) {
                1 -> "销售经理"
                2 -> "总经理"
                else -> "销售顾问"
            }
        }
}

/** Go `response.ProfileLegacyBody`。 */
@Serializable
public data class MineProfileData(
    val id: String? = null,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("user_name") val userName: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val phone: String? = null,
    val stats: MineStoreStats? = null,
) {
    public val resolvedDisplayName: String
        get() = (userName?.takeIf { it.isNotBlank() } ?: displayName.orEmpty()).trim()

    public val resolvedAvatarUrl: String get() = avatarUrl.orEmpty()

    public val resolvedPhone: String get() = phone?.trim().orEmpty()

    public val resolvedStats: MineStoreStats get() = stats ?: MineStoreStats()
}

/** Go `response.UserStoreListItem`。 */
@Serializable
public data class MineStoreRow(
    @SerialName("store_id") val storeId: Int = 0,
    @SerialName("store_name") val storeName: String = "",
    @SerialName("role_label") val roleLabel: String = "",
    @SerialName("is_current") val isCurrent: Boolean = false,
)

/** Go `response.UserStoreListBody`。 */
@Serializable
public data class MineStoreListData(
    val list: List<MineStoreRow>? = null,
    @SerialName("current_store_id") val currentStoreId: Int = 0,
) {
    public val rows: List<MineStoreRow> get() = list ?: emptyList()
}

@Serializable
private data class MineStoreSwitchBody(
    @SerialName("store_id") val storeId: Int,
)

@Serializable
private data class MineProfilePatchBody(
    @SerialName("user_name") val userName: String,
    @SerialName("display_name") val displayName: String,
)

public class MineProfileApi(
    private val api: ApiClient,
) {
    /** `GET /api/v1/me/profile?store_id=`（BackendJSON 直出）。 */
    public suspend fun fetchProfile(storeId: String? = null): MineProfileData =
        decodeLegacy(
            api.getRaw(
                path = MineProfilePaths.PROFILE,
                query = queryOf("store_id" to storeId?.trim()),
            ),
        )

    /** `PATCH /api/v1/me/profile`：后端按 `user_name` 优先落库，故两键同写。 */
    public suspend fun updateDisplayName(name: String): MineProfileData {
        val trimmed = name.trim()
        val body = NetworkJson.encodeToString(
            MineProfilePatchBody.serializer(),
            MineProfilePatchBody(userName = trimmed, displayName = trimmed),
        )
        return decodeLegacy(api.patchRaw(MineProfilePaths.PROFILE, body))
    }

    /** `GET /api/v1/me/stores`（标准信封）。 */
    public suspend fun fetchStores(): MineStoreListData {
        val response: ApiResponse<MineStoreListData> = api.getApi(MineProfilePaths.STORES)
        val data = response.data
        if (!response.isSuccess || data == null) {
            throw NetworkError.Business(
                response.code,
                response.message?.takeIf { it.isNotEmpty() } ?: "加载门店失败",
            )
        }
        return data
    }

    /** `POST /api/v1/profiles/me/store`（标准信封，`data` = 门店统计）。 */
    public suspend fun switchStore(storeId: Int): MineStoreStats {
        val response: ApiResponse<MineStoreStats> =
            api.postApi(MineProfilePaths.SWITCH_STORE, MineStoreSwitchBody(storeId))
        val data = response.data
        if (!response.isSuccess || data == null) {
            throw NetworkError.Business(
                response.code,
                response.message?.takeIf { it.isNotEmpty() } ?: "切换门店失败",
            )
        }
        return data
    }

    private fun decodeLegacy(raw: String): MineProfileData {
        val envelope = runCatching {
            NetworkJson.decodeFromString(ApiEnvelope.serializer(), raw)
        }.getOrNull()
        if (envelope?.error != null) {
            throw NetworkError.Business(envelope.code ?: -1, envelope.error!!)
        }
        return runCatching {
            NetworkJson.decodeFromString(MineProfileData.serializer(), raw)
        }.getOrElse { throw NetworkError.Business(-1, "资料请求失败") }
    }
}

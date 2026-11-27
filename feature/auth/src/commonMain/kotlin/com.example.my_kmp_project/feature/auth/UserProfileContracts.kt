package com.example.my_kmp_project.feature.auth

import com.example.my_kmp_project.core.network.ApiClient
import com.example.my_kmp_project.core.network.ApiEnvelope
import com.example.my_kmp_project.core.network.ApiResponse
import com.example.my_kmp_project.core.network.FlexibleIntSerializer
import com.example.my_kmp_project.core.network.NetworkError
import com.example.my_kmp_project.core.network.NetworkJson
import com.example.my_kmp_project.core.network.decodeEnvelopeData
import com.example.my_kmp_project.core.network.queryOf
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 契约表 §2.2（`features/auth/lib/api/user_profile_api.dart` + `user_profile_models.dart`，d7876a1）。
 *
 * 注意 **前缀分裂**：资料走 `/api/v1/profiles/` 下的 `me` 与 `me/store`，可切换门店列表走 `/api/v1/me/stores`
 * （`user_profile_api.dart:9-10`；契约表 #11 备注「前缀是 `/me`，不是 `/profiles`」）。
 */
public object ProfileApiPaths {
    /** 契约表 #8 / #9（`user_profile_api.dart:9` mePath）。 */
    const val ME = "/api/v1/profiles/me"

    /** 契约表 #10（`user_profile_api.dart:46` `'$mePath/store'`）。 */
    const val ME_STORE = "/api/v1/profiles/me/store"

    /** 契约表 #11（`user_profile_api.dart:10` storesPath）——**不是** `/profiles/me/stores`。 */
    const val MY_STORES = "/api/v1/me/stores"

    /** Go `profile_routes.go:24-25`：`GET|PATCH /api/v1/me/profile`（BackendJSON 直出，非信封）。 */
    const val ME_PROFILE = "/api/v1/me/profile"

    /** Go `access/router.go:55`：`GET /api/v1/me/permissions`（统一信封，data.permissions）。 */
    const val ME_PERMISSIONS = "/api/v1/me/permissions"
}

/**
 * GET `/api/v1/profiles/me` 的 query 构造（契约表 #8；`user_profile_api.dart:17-19`）：
 * `store_id` **trim 后为空则整个 key 省略**（走 [queryOf] 的丢弃规则）。
 */
public fun profileMeQuery(storeId: String?): Map<String, String> =
    queryOf("store_id" to storeId?.trim())

/**
 * Flutter `UserProfile`（`user_profile_models.dart:2-50`；契约表 #8 / #9 的 `data`）。
 *
 * 大小写混用两键都接受，取值顺序**逐字段照抄 Dart**（BFF 实际只发一种形状，
 * 顺序仅在两键同时出现时才有意义）：
 * `user_name`(snake 优先) / `displayName`(camel 优先) / `avatarUrl`(camel 优先)。
 * [stats] 缺失或非 object 时按 Dart 退化为 [UserStoreStatsData.ZERO]。
 */
@Serializable
public data class UserProfileData(
    val id: String? = null,
    @SerialName("user_id") val userIdAlt: String? = null,
    @SerialName("user_name") val userNameSnake: String? = null,
    @SerialName("userName") val userNameCamel: String? = null,
    @SerialName("display_name") val displayNameSnake: String? = null,
    @SerialName("displayName") val displayNameCamel: String? = null,
    val username: String? = null,
    val email: String? = null,
    @Serializable(with = FlexibleIntSerializer::class)
    val status: Int? = null,
    @SerialName("avatar_url") val avatarUrlSnake: String? = null,
    @SerialName("avatarUrl") val avatarUrlCamel: String? = null,
    val phone: String? = null,
    val stats: UserStoreStatsData? = null,
) {
    /** Dart `UserProfile.id`：`id` 优先，空则回落 `user_id`（:32）。 */
    val resolvedId: String get() = firstPresent(id, userIdAlt)

    /** Dart `UserProfile.userId`：`user_id` 优先，空则回落 `id`（:33）。 */
    val resolvedUserId: String get() = firstPresent(userIdAlt, id)

    /** Dart `UserProfile.userName`：`user_name|userName`，空则取 displayName（:29,34）。 */
    val resolvedUserName: String
        get() = firstPresent(userNameSnake, userNameCamel).ifBlank { resolvedDisplayName }

    /** Dart `UserProfile.displayName`：`displayName|display_name|username`，空则取 userName（:30,37）。 */
    val resolvedDisplayName: String
        get() = firstPresent(displayNameCamel, displayNameSnake, username)
            .ifBlank { firstPresent(userNameSnake, userNameCamel) }

    val resolvedEmail: String get() = email?.trim().orEmpty()

    /** Dart `status: UserStoreStats._intOf(json['status'])`——负数夹到 0。 */
    val resolvedStatus: Int get() = (status ?: 0).coerceAtLeast(0)

    /** Dart `UserProfile.avatarUrl`：`avatarUrl|avatar_url`（camel 优先，:38）。 */
    val resolvedAvatarUrl: String get() = firstPresent(avatarUrlCamel, avatarUrlSnake)

    val resolvedPhone: String get() = phone?.trim().orEmpty()

    /** `stats` 非 object / 缺失 → 全零（Dart :40-42）。 */
    fun resolvedStats(): UserStoreStatsData = stats ?: UserStoreStatsData.ZERO
}

/**
 * Flutter `UserStoreStats`（`user_profile_models.dart:108-172`；契约表 #8 的 `data.stats`
 * 与 #10 的 `data`）。
 *
 * 全部字段 snake_case，camelCase 仅作容错别名；[resolvedX] 系列镜像 Dart
 * `_intOf`：负数与非数字一律 0（:167-171）。
 * role：0 销售顾问 / 1 销售经理 / 2 总经理。
 */
@Serializable
public data class UserStoreStatsData(
    @SerialName("store_id")
    @Serializable(with = FlexibleIntSerializer::class)
    val storeIdSnake: Int? = null,
    @SerialName("storeId")
    @Serializable(with = FlexibleIntSerializer::class)
    val storeIdCamel: Int? = null,
    @SerialName("store_name") val storeNameSnake: String? = null,
    @SerialName("storeName") val storeNameCamel: String? = null,
    @SerialName("days_joined")
    @Serializable(with = FlexibleIntSerializer::class)
    val daysJoinedSnake: Int? = null,
    @SerialName("daysJoined")
    @Serializable(with = FlexibleIntSerializer::class)
    val daysJoinedCamel: Int? = null,
    @SerialName("employee_count")
    @Serializable(with = FlexibleIntSerializer::class)
    val employeeCountSnake: Int? = null,
    @SerialName("employeeCount")
    @Serializable(with = FlexibleIntSerializer::class)
    val employeeCountCamel: Int? = null,
    @SerialName("store_days")
    @Serializable(with = FlexibleIntSerializer::class)
    val storeDaysSnake: Int? = null,
    @SerialName("storeDays")
    @Serializable(with = FlexibleIntSerializer::class)
    val storeDaysCamel: Int? = null,
    @SerialName("total_customers")
    @Serializable(with = FlexibleIntSerializer::class)
    val totalCustomersSnake: Int? = null,
    @SerialName("totalCustomers")
    @Serializable(with = FlexibleIntSerializer::class)
    val totalCustomersCamel: Int? = null,
    @SerialName("role")
    @Serializable(with = FlexibleIntSerializer::class)
    val roleCode: Int? = null,
    @SerialName("role_label") val roleLabelSnake: String? = null,
    @SerialName("roleLabel") val roleLabelCamel: String? = null,
) {
    val resolvedStoreId: Int get() = firstInt(storeIdSnake, storeIdCamel)

    val resolvedStoreName: String get() = firstPresent(storeNameSnake, storeNameCamel)

    val resolvedDaysJoined: Int get() = firstInt(daysJoinedSnake, daysJoinedCamel)

    val resolvedEmployeeCount: Int get() = firstInt(employeeCountSnake, employeeCountCamel)

    val resolvedStoreDays: Int get() = firstInt(storeDaysSnake, storeDaysCamel)

    val resolvedTotalCustomers: Int get() = firstInt(totalCustomersSnake, totalCustomersCamel)

    val resolvedRole: Int get() = (roleCode ?: 0).coerceAtLeast(0)

    /** Dart：`role_label` 非空用之，否则由 [labelOfRole] 客户端推导（:147）。 */
    val resolvedRoleLabel: String
        get() = firstPresent(roleLabelSnake, roleLabelCamel).ifBlank { labelOfRole(resolvedRole) }

    companion object {
        /** Dart `UserStoreStats.zero`（:120-125）：全零 + `roleLabel = 销售顾问`。 */
        val ZERO = UserStoreStatsData()

        /** Dart `labelOfRole`（:151-160）。 */
        fun labelOfRole(role: Int): String = when (role) {
            1 -> "销售经理"
            2 -> "总经理"
            else -> "销售顾问"
        }

        /** Dart `_intOf`：负数与解析失败一律 0（:167-171）。 */
        private fun firstInt(primary: Int?, secondary: Int?): Int =
            (primary ?: secondary ?: 0).coerceAtLeast(0)
    }
}

/**
 * Flutter `UserStoreListItem`（`user_profile_models.dart:78-104`；契约表 #11 的 `list[]` 行）。
 */
@Serializable
public data class UserStoreListItemData(
    @SerialName("store_id")
    @Serializable(with = FlexibleIntSerializer::class)
    val storeIdSnake: Int? = null,
    @SerialName("storeId")
    @Serializable(with = FlexibleIntSerializer::class)
    val storeIdCamel: Int? = null,
    @SerialName("store_name") val storeNameSnake: String? = null,
    @SerialName("storeName") val storeNameCamel: String? = null,
    @SerialName("role")
    @Serializable(with = FlexibleIntSerializer::class)
    val roleCode: Int? = null,
    @SerialName("role_label") val roleLabelSnake: String? = null,
    @SerialName("roleLabel") val roleLabelCamel: String? = null,
    @SerialName("is_current") val isCurrentSnake: Boolean? = null,
    @SerialName("isCurrent") val isCurrentCamel: Boolean? = null,
) {
    val storeId: Int get() = (storeIdSnake ?: storeIdCamel ?: 0).coerceAtLeast(0)

    val storeName: String get() = firstPresent(storeNameSnake, storeNameCamel)

    /** Dart `_intOf(json['role'])`：负数 / 非数字 → 0（销售顾问）。 */
    val role: Int get() = (roleCode ?: 0).coerceAtLeast(0)

    /** 空 label → 客户端按 role 推导（Dart :100）。 */
    val roleLabel: String
        get() = firstPresent(roleLabelSnake, roleLabelCamel).ifBlank {
            UserStoreStatsData.labelOfRole(role)
        }

    /** Dart `json['is_current'] == true || json['isCurrent'] == true`（:101）——非 true 一律 false。 */
    val isCurrent: Boolean get() = isCurrentSnake == true || isCurrentCamel == true
}

/**
 * Flutter `UserStoreListResult`（`user_profile_models.dart:53-75`；契约表 #11 的 `data`）。
 *
 * 契约只读 `list`（Dart 不认 `items`）；`current_store_id` 非 0 时**覆盖**本地持久化的
 * `mine_selected_store_id`（契约表 §5.5）。
 */
@Serializable
public data class UserStoreListData(
    val list: List<UserStoreListItemData>? = null,
    @SerialName("current_store_id")
    @Serializable(with = FlexibleIntSerializer::class)
    val currentStoreIdSnake: Int? = null,
    @SerialName("currentStoreId")
    @Serializable(with = FlexibleIntSerializer::class)
    val currentStoreIdCamel: Int? = null,
) {
    /** Dart `raw is List ? … : []`——`list` 缺失或为 null 时是空列表而非失败。 */
    val rows: List<UserStoreListItemData> get() = list ?: emptyList()

    /** Dart `UserStoreListResult.currentStoreId`：0 = 服务端没表态，本地选中值继续生效。 */
    val currentStoreId: Int get() = (currentStoreIdSnake ?: currentStoreIdCamel ?: 0).coerceAtLeast(0)
}

/**
 * Flutter `UpdateUserProfileRequest`（`user_profile_models.dart:174-198`；契约表 #9）。
 *
 * **omit 规则是硬语义**（`NetworkJson` 的 `explicitNulls = false` 正好对等）：
 * - 改名时 `user_name` 与 `display_name` **同时写**（Go `ResolvedUserName` 优先 `user_name`，:188-190）
 * - `avatar_mime` **只在带 `avatar_base64` 时**发，缺省补 `image/jpeg`（:192-195）
 * - 其余键为 null 时整个省略（稀疏 PATCH，绝不发 `null`）
 */
@Serializable
public data class UserProfileUpdateBody(
    @SerialName("user_name") val userName: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("avatar_base64") val avatarBase64: String? = null,
    @SerialName("avatar_mime") val avatarMime: String? = null,
) {
    companion object {
        /** Dart `UpdateUserProfileRequest(displayName:, avatarBase64:, avatarMime:)` 的对等构造。 */
        fun of(
            displayName: String? = null,
            avatarBase64: String? = null,
            avatarMime: String? = null,
        ): UserProfileUpdateBody {
            val name = displayName?.trim()?.takeIf { it.isNotEmpty() }
            val avatar = avatarBase64?.takeIf { it.isNotEmpty() }
            return UserProfileUpdateBody(
                userName = name,
                displayName = name,
                avatarBase64 = avatar,
                avatarMime = avatar?.let { avatarMime?.takeIf { it.isNotEmpty() } ?: DEFAULT_AVATAR_MIME },
            )
        }

        /** Dart `avatarMime ?? 'image/jpeg'`（:194）。 */
        const val DEFAULT_AVATAR_MIME = "image/jpeg"
    }
}

/** 契约表 #10 请求体：`{store_id:int}` 单键总发（`user_profile_api.dart:47`）。 */
@Serializable
public data class UserStoreSwitchBody(
    @SerialName("store_id") val storeId: Int,
)

/**
 * Flutter `UserProfileApi._mapFailure`（`user_profile_api.dart:95-115`）对等文案映射。
 *
 * 资料通道专属分支在前（10001 → 资料参数无效），其余会话语义复用 [mapAuthFailure]
 * （10021/10022/10023/401/403 与登录页同一套决策，不重复实现）。
 */
public fun mapProfileFailure(
    code: Int?,
    message: String?,
    statusCode: Int? = null,
): String {
    val text = message?.trim().orEmpty()
    if (code == AuthBizCode.PROFILE_PARAM_INVALID ||
        text.contains("avatar_base64") ||
        text.contains("avatar_mime") ||
        text.contains("头像过大") ||
        text.contains("参数错误")
    ) {
        // 保留会话：参数错不视为登录失效（Dart UnknownAuthFailure）。
        return text.ifBlank { "资料参数无效" }
    }
    if (statusCode == 401 ||
        code == AuthBizCode.UNAUTHORIZED ||
        text.contains("未授权") ||
        text.contains("token 无效") ||
        text.contains("token 已过期") ||
        text.contains("登录已失效")
    ) {
        // 全局拦截器应已清会话；此处不再冒充成可重试业务错（Dart SessionClearedFailure）。
        return AuthFailure.SessionCleared().message
    }
    // 其余：有 message 时复用 auth 的成文文案，无 message 时按 Dart 回落到「资料请求失败」。
    if (text.isEmpty()) return "资料请求失败"
    return mapAuthFailure(code, message, statusCode)
}

// ── Go 后端 legacy 资料 / 权限契约（与 §2.2 的 `/profiles/me` 不是同一组端点）─────

/**
 * `GET /me/profile?store_id=` 与 `GET /me/permissions?store_id=` 的 query 构造：
 * 空白 / null 时整个 key 省略（[queryOf] 丢弃规则）。后端 `me/profile` 用
 * `c.Query("store_id")`（字符串），`me/permissions` 用 `strconv.Atoi`（正整数）。
 */
public fun meProfileQuery(storeId: String?): Map<String, String> =
    queryOf("store_id" to storeId?.trim())

public fun mePermissionsQuery(storeId: Int?): Map<String, String> =
    queryOf("store_id" to storeId?.takeIf { it > 0 }?.toString())

/**
 * Go `response.ProfileLegacyBody`（`dto/response/profile.go:109-122`）：
 * `/me/profile` 的 **snake_case 直出**（与 §2.2 的 camelCase 别名 `UserProfileData` 不同）。
 * [status] 用 [FlexibleIntSerializer] 容忍数字/数字串；`stats` 复用 [UserStoreStatsData]
 * （后端 `ProfileStatsItem` 即 snake_case 的同一组统计键）。
 */
@Serializable
public data class MeProfileData(
    val id: String? = null,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("user_name") val userName: String? = null,
    val email: String? = null,
    @Serializable(with = FlexibleIntSerializer::class)
    val status: Int? = null,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val phone: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    val stats: UserStoreStatsData? = null,
) {
    val resolvedId: String get() = firstPresent(id, userId)

    val resolvedUserName: String get() = firstPresent(userName, displayName)

    val resolvedStats: UserStoreStatsData get() = stats ?: UserStoreStatsData.ZERO
}

/**
 * Go `entity.UpdateProfileInput`（`model/entity/profile.go:26-35`）稀疏 PATCH 请求体。
 * null 键由 [NetworkJson] 的 `explicitNulls = false` 整个省略，绝不发 null。
 */
@Serializable
public data class MeProfilePatchBody(
    @SerialName("user_name") val userName: String? = null,
    val username: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("avatar_base64") val avatarBase64: String? = null,
    @SerialName("avatar_mime") val avatarMime: String? = null,
    val phone: String? = null,
    val status: Int? = null,
)

/** Go `access/handler.go:155` `gin.H{"permissions": codes}` 经 `response.Success` 包成信封的 data。 */
@Serializable
public data class MePermissionsData(
    val permissions: List<String> = emptyList(),
)

/**
 * Go 后端「legacy 资料 + 权限」契约（**与 §2.2 的 `/profiles/me` 不是同一组端点**）。
 *
 * 只登记后端真实存在的路由，不自造 UI 调用点、不改后端：
 * - `profile_routes.go:24-25` `GET|PATCH /api/v1/me/profile` → `ProfileController.GetMeLegacy`
 *   / `UpdateMeLegacy`，用 `response.BackendJSON` **直接 JSON 直出**（非 `{code,message,data}`
 *   信封），故本类用 `getRaw` / `patchRaw` 自行反序列化 [MeProfileData]；
 *   错误体是 `response.BackendError` → `{error,code}`，由 [decodeLegacy] 识别。
 * - `access/router.go:55` `GET /api/v1/me/permissions` → `handler.ListMyPermissions`，
 *   用 `response.Success` 包成标准信封，data = `{permissions:[...]}`，故走 `getApi`。
 *
 * 逐条对照见 `notes/backend-contract-matrix.md`。
 */
public class MeProfileApi(
    private val client: ApiClient,
) {
    /** `GET /api/v1/me/profile`（BackendJSON 直出）。 */
    public suspend fun getProfile(storeId: String? = null): MeProfileData =
        decodeLegacy(client.getRaw(ProfileApiPaths.ME_PROFILE, meProfileQuery(storeId)))

    /** `PATCH /api/v1/me/profile`（BackendJSON 直出）。 */
    public suspend fun updateProfile(body: MeProfilePatchBody): MeProfileData =
        decodeLegacy(client.patchRaw(ProfileApiPaths.ME_PROFILE, NetworkJson.encodeToString(MeProfilePatchBody.serializer(), body)))

    /** `GET /api/v1/me/permissions`（统一信封，data = `{permissions:[...]}`）。 */
    public suspend fun getPermissions(storeId: Int? = null): MePermissionsData {
        val response: ApiResponse<MePermissionsData> = client.getApi(
            path = ProfileApiPaths.ME_PERMISSIONS,
            query = mePermissionsQuery(storeId),
            parseData = { el -> decodeEnvelopeData<MePermissionsData>(el) },
        )
        if (!response.isSuccess || response.data == null) {
            throw NetworkError.Business(
                response.code,
                response.message?.takeIf { it.isNotEmpty() } ?: "获取权限失败",
            )
        }
        return response.data!!
    }

    private fun decodeLegacy(raw: String): MeProfileData {
        val envelope = runCatching {
            NetworkJson.decodeFromString(ApiEnvelope.serializer(), raw)
        }.getOrNull()
        if (envelope?.error != null) {
            throw NetworkError.Business(envelope.code ?: -1, envelope.error!!)
        }
        return runCatching {
            NetworkJson.decodeFromString(MeProfileData.serializer(), raw)
        }.getOrElse { throw NetworkError.Business(-1, "资料请求失败") }
    }
}

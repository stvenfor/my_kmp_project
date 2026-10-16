package com.example.my_kmp_project.feature.chat

import com.example.my_kmp_project.core.network.ApiClient
import com.example.my_kmp_project.core.network.NetworkError
import com.example.my_kmp_project.core.network.NetworkFacade
import kotlinx.coroutines.delay
import kotlinx.serialization.json.JsonObject

/**
 * 契约表 §6 要求跨模块共享的 Dart `ImUserProfile`
 * （`commons/core/lib/model/im/im_user_profile.dart:1-27`）。
 *
 * IM 契约族里它的解码形状由 #101 规定：`{user_id|im_user_id, display_name, avatar_url}`
 * ——三个键都容忍 snake_case / camelCase 别名，缺失即空串
 * （Dart im_user_profile_api.dart:52-58 的 `(m['x'] ?? '').toString()`）。
 * Dart 的 `extra` 字段在 BFF 契约里没有来源，故 KMP 不保留。
 */
public data class ImUserProfile(
    public val imUserId: String,
    public val displayName: String,
    public val avatarUrl: String,
)

/**
 * Dart `components/rongcloud_im/lib/api/im_user_profile_api.dart:8-71` ——
 * `GET /api/v1/im/users/profile`。
 *
 * 必须保留的语义（契约表 #101）：
 * - query key 固定 `user_ids`，值是**逗号拼接**的 id 串（:35）；
 * - 入参先 `trim()` 再丢弃空串，**空 id 列表直接不发 HTTP** 返回空表（:16-17）；
 * - 响应 `{items:[…]}` 按 id 收敛成 `Map`，id 为空的行跳过（:48-59）；
 * - mock 分支（:19-31）等 80ms 后本地造数据，头像 `https://picsum.photos/seed/<id>/200/200`，
 *   昵称走 [_mockNameFor]（:64-70，与 Dart `_mockName` 逐字对齐）。
 *
 * // GAP(R5): Dart 侧本 API 只负责取资料；真实融云 SDK 的 userId 解析/在线状态
 * // （components/rongcloud_im/lib/engine/rong_engine_holder.dart:92 attachMessageListener）
 * // 不在本工程可用产物内，故此处不引入任何 SDK 依赖。
 */
public class ImUserProfileApi(
    private val client: ApiClient = NetworkFacade.api(),
    private val useMock: () -> Boolean = { RongImConfig.useMockIm },
    private val sleep: suspend (Long) -> Unit = { delay(it) },
) {
    /** Dart `ImUserProfileApi.fetchProfiles`（im_user_profile_api.dart:15-62）。 */
    public suspend fun fetchProfiles(imUserIds: List<String>): Map<String, ImUserProfile> {
        val ids = imUserIds.map { it.trim() }.filter { it.isNotEmpty() }
        // 空列表短路，不发请求（:17）
        if (ids.isEmpty()) return emptyMap()

        if (useMock()) {
            sleep(MOCK_PROFILE_DELAY_MS)
            return ids.associateWith { id ->
                ImUserProfile(
                    imUserId = id,
                    displayName = mockNameFor(id),
                    avatarUrl = "https://picsum.photos/seed/$id/200/200",
                )
            }
        }

        val response = client.getApi(
            path = RongImConfig.USERS_PROFILE_PATH,
            // 逗号拼接（:35）；这里刻意不用 queryOf，空列表已在上面短路
            query = mapOf("user_ids" to ids.joinToString(separator = ",")),
            parseData = { element -> (element as? JsonObject) },
        )
        val data = response.data
        if (!response.isSuccess || data == null) {
            throw NetworkError.Business(
                code = response.code,
                message = response.message ?: FAILURE_FALLBACK,
            )
        }
        val out = LinkedHashMap<String, ImUserProfile>(data.size)
        data.imJsonItems().forEach { row ->
            // Dart :52-53：user_id 优先、im_user_id 兜底；空 id 跳过
            val id = row.imJsonStr("user_id", "im_user_id")
            if (id.isEmpty()) return@forEach
            out[id] = ImUserProfile(
                imUserId = id,
                displayName = row.imJsonStr("display_name", "displayName"),
                avatarUrl = row.imJsonStr("avatar_url", "avatarUrl"),
            )
        }
        return out
    }

    /** Dart `ImUserProfileApi._mockName`（im_user_profile_api.dart:64-70）。 */
    private fun mockNameFor(imUserId: String): String {
        if (imUserId.contains("mock_peer")) {
            return "Mock好友" + imUserId.takeLast(2)
        }
        val tail = if (imUserId.length <= 4) imUserId else imUserId.substring(0, 4)
        return "用户$tail"
    }

    private companion object {
        /** Dart `Future.delayed(80ms)`（im_user_profile_api.dart:20）。 */
        const val MOCK_PROFILE_DELAY_MS: Long = 80L

        /** Dart `'拉取 IM 用户资料失败'`（im_user_profile_api.dart:44）。 */
        const val FAILURE_FALLBACK: String = "拉取 IM 用户资料失败"
    }
}

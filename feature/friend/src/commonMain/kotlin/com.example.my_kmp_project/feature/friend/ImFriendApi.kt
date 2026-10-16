package com.example.my_kmp_project.feature.friend

import com.example.my_kmp_project.core.network.ApiClient
import com.example.my_kmp_project.core.network.NetworkError
import com.example.my_kmp_project.core.network.NetworkFacade
import com.example.my_kmp_project.core.network.NetworkJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * 好友域（契约表 §2.18 行 #103 / #104 / #105 / #106 / #107 / #108）路径常量。
 *
 * Dart 侧这些常量集中在 `components/rongcloud_im/lib/config/rong_im_config.dart:21-24`
 * （`friendsPath` / `friendRequestsPath` / `usersSearchPath` / `privateAdmissionPath`）；
 * KMP 因「feature 不得互相 import」按 lane 拆分，取值必须与
 * `feature/chat/RongImConfig.kt` 保持一致。
 */
public object ImFriendApiPaths {
    /** Dart `RongImConfig.friendsPath`（rong_im_config.dart:21）。 */
    public const val FRIENDS_PATH: String = "/api/v1/im/friends"

    /** Dart `RongImConfig.friendRequestsPath`（rong_im_config.dart:22）。 */
    public const val FRIEND_REQUESTS_PATH: String = "/api/v1/im/friends/requests"

    /** Dart `RongImConfig.usersSearchPath`（rong_im_config.dart:23）。 */
    public const val USERS_SEARCH_PATH: String = "/api/v1/im/users/search"

    /** Dart `RongImConfig.privateAdmissionPath`（rong_im_config.dart:24）。 */
    public const val PRIVATE_ADMISSION_PATH: String = "/api/v1/im/private/admission"

    /** Dart `'${friendRequestsPath}/$requestId/respond'`（im_friend_api.dart:159）。 */
    public fun respondPath(requestId: String): String = "$FRIEND_REQUESTS_PATH/$requestId/respond"
}

/**
 * 契约表 #103 / #107 —— Dart `ImFriendUser`（im_friend_api.dart:4-25）。
 * 解码容忍 `user_id|im_user_id` 与 `display_name`/`avatar_url` 的 camelCase 别名，
 * 缺失即空串（Dart `(json['x'] ?? '').toString()`）。
 */
public data class ImFriendUser(
    public val userId: String,
    public val displayName: String,
    public val avatarUrl: String = "",
    public val phoneMasked: String = "",
) {
    public companion object {
        public fun fromJson(json: JsonObject): ImFriendUser = ImFriendUser(
            userId = json.imFriendStr("user_id", "im_user_id"),
            displayName = json.imFriendStr("display_name", "displayName"),
            avatarUrl = json.imFriendStr("avatar_url", "avatarUrl"),
            phoneMasked = json.imFriendStr("phone_masked", "phoneMasked"),
        )
    }
}

/**
 * 契约表 #104 —— Dart `ImFriendRequest`（im_friend_api.dart:27-57）。
 * `status` 缺失时兜底 `'pending'`（:53），且服务端只回**入向**申请；
 * `created_at` 按字符串原样保留（Dart 未解析成 DateTime）。
 */
public data class ImFriendRequest(
    public val id: String,
    public val fromUserId: String,
    public val displayName: String,
    public val avatarUrl: String = "",
    public val phoneMasked: String = "",
    public val status: String = "pending",
    public val createdAt: String = "",
) {
    public companion object {
        public fun fromJson(json: JsonObject): ImFriendRequest = ImFriendRequest(
            id = json.imFriendStr("id"),
            fromUserId = json.imFriendStr("from_user_id", "fromUserId"),
            displayName = json.imFriendStr("display_name", "displayName"),
            avatarUrl = json.imFriendStr("avatar_url", "avatarUrl"),
            phoneMasked = json.imFriendStr("phone_masked", "phoneMasked"),
            status = json.imFriendStr("status").ifEmpty { "pending" },
            createdAt = json.imFriendStr("created_at", "createdAt"),
        )
    }
}

/** `POST /im/friends/requests` body：`{to_user_id: String}`（im_friend_api.dart:143）。 */
@Serializable
public data class ImFriendRequestCreateRequest(
    @SerialName("to_user_id") val toUserId: String,
)

/**
 * `POST /im/friends/requests/:requestId/respond` body：`{accept: bool}`（im_friend_api.dart:160）。
 * 契约表 #106：同意与拒绝**共用同一端点**，只切 `accept` 取值。
 */
@Serializable
public data class ImFriendRespondRequest(
    @SerialName("accept") val accept: Boolean,
)

/**
 * Dart `ImFriendApi`（im_friend_api.dart:59-185）——
 * 好友搜索 #107、好友列表 #103、入向申请列表 #104、发起好友申请 #105、
 * 同意/拒绝申请 #106、单聊准入 #108。
 *
 * 本 lane 全部为 BFF 侧 HTTP 契约；好友关系变化后的会话打开、消息收发属于融云 SDK，
 * 仍由 `component/chat/ImEngine.kt` 的 mock 引擎承担（见下方 GAP(R5)）。
 *
 * // GAP(R5): Dart 在成为好友后才通过 SDK 建会话/发消息
 * // （`features/chat/lib/chat/repository/im_chat_repository.dart:103-130` 的 sendText/sendImage
 * // 与 :405-420 的收信回调，底层 `components/rongcloud_im/lib/engine/rong_engine_holder.dart:167-227`
 * // 的 `RCIMIWEngine.sendMessage`，调用点在 :210）；融云 IMLib 产物在本工程不可用，
 * // 这里不引入 SDK、不伪装 ready。
 */
public class ImFriendApi(
    private val client: ApiClient = NetworkFacade.api(),
) {

    /** Dart `ImFriendApi.search`（im_friend_api.dart:75-96）：`GET /im/users/search?q=`。 */
    public suspend fun search(query: String): List<ImFriendUser> =
        client.getItemsApi(
            path = ImFriendApiPaths.USERS_SEARCH_PATH,
            // Dart 原样把 q 传下去（空串也发），故不走 queryOf 的省略规则
            query = mapOf("q" to query),
            fallbackMessage = "搜索失败",
            parse = ::imFriendUserFrom,
        )

    /** Dart `ImFriendApi.listFriends`（im_friend_api.dart:98-117）：`GET /im/friends`。 */
    public suspend fun listFriends(): List<ImFriendUser> =
        client.getItemsApi(
            path = ImFriendApiPaths.FRIENDS_PATH,
            query = emptyMap(),
            fallbackMessage = "好友列表失败",
            parse = ::imFriendUserFrom,
        )

    /**
     * Dart `ImFriendApi.listIncomingRequests`（im_friend_api.dart:119-138）：
     * `GET /im/friends/requests`，**仅入向**申请。
     */
    public suspend fun listIncomingRequests(): List<ImFriendRequest> =
        client.getItemsApi(
            path = ImFriendApiPaths.FRIEND_REQUESTS_PATH,
            query = emptyMap(),
            fallbackMessage = "申请列表失败",
            parse = ::imFriendRequestFrom,
        )

    /**
     * 契约表 #105 —— Dart `ImFriendApi.requestFriend`（im_friend_api.dart:140-155）：
     * `POST /im/friends/requests`，body `{to_user_id: String}`，**只看 `code==0`**，
     * `data` 整个忽略（Dart 连 `data` 是否为 null 都不看，:149）。
     *
     * 英文错误转中文（表行 #105 的那一列备注）在 Dart 是**调用点**契约而不是本方法契约：
     * `features/friend/lib/friend/view/friend_page.dart:104-114` 把 `requestFriend` 包在
     * try/catch 里，catch 后 `_toast(ImFriendApi.friendlyError(e, fallback: '申请失败'))`。
     * 故本方法**必须原样抛出服务端英文 `message`**（兜底 `'申请失败'`，:151），
     * 由调用方经 [friendlyError] 映射；在本方法里提前翻译会破坏 Dart 的分工。
     */
    public suspend fun requestFriend(toUserId: String) {
        val response = client.postApi(
            ImFriendApiPaths.FRIEND_REQUESTS_PATH,
            NetworkJson.encodeToString(
                ImFriendRequestCreateRequest.serializer(),
                ImFriendRequestCreateRequest(toUserId = toUserId),
            ),
        ) { Unit }
        if (!response.isSuccess) {
            throw NetworkError.Business(
                code = response.code,
                message = response.message ?: "申请失败",
            )
        }
    }

    /** Dart `ImFriendApi.respondFriend`（im_friend_api.dart:157-172）：同意/拒绝共用一端点。 */
    public suspend fun respondFriend(requestId: String, accept: Boolean) {
        val response = client.postApi(
            ImFriendApiPaths.respondPath(requestId),
            NetworkJson.encodeToString(
                ImFriendRespondRequest.serializer(),
                ImFriendRespondRequest(accept = accept),
            ),
        ) { Unit }
        if (!response.isSuccess) {
            throw NetworkError.Business(
                code = response.code,
                message = response.message ?: "处理失败",
            )
        }
    }

    /**
     * Dart `ImFriendApi.canPrivateChat`（im_friend_api.dart:174-184）——
     * `GET /im/private/admission?peer_user_id=`，判定是 `data['allowed'] == true`。
     *
     * **fail-closed（契约表 #108）**：非布尔 `true` 一律 false；
     * 信封失败、`data` 缺失、传输异常也一律 false（Dart 侧调用点包在 try/catch 里）。
     */
    public suspend fun canPrivateChat(peerUserId: String): Boolean {
        val response = runCatching {
            client.getApi(
                path = ImFriendApiPaths.PRIVATE_ADMISSION_PATH,
                query = mapOf("peer_user_id" to peerUserId),
                parseData = { element -> element as? JsonObject },
            )
        }.getOrNull() ?: return false
        if (!response.isSuccess) return false
        return response.data?.imFriendIsTrue("allowed") == true
    }

    public companion object {
        /**
         * Dart `ImFriendApi.friendlyError`（im_friend_api.dart:61-73）——
         * 后端英文错误 → 友好中文映射表，顺序与 Dart 逐条一致（先匹配先返回）。
         *
         * `raw` 取法对齐 Dart 的 `e is HttpRequestException ? e.message : e.toString()`：
         * [NetworkError] 是我们的 HttpRequestException 对等物，故取其 `message`；
         * 其它异常取 `toString()`。
         */
        public fun friendlyError(error: Throwable?, fallback: String = "操作失败"): String {
            val raw = when (error) {
                null -> ""
                is NetworkError -> error.message.orEmpty()
                else -> error.toString()
            }
            return friendlyMessage(raw, fallback)
        }

        /**
         * 纯文本入口：直接喂服务端 `message`（Dart 归一后的 `raw`）。
         * 空串与 `HttpRequestException` 前缀回落 [fallback] —— 后者是 Dart
         * `HttpRequestException.toString()` 的形态（`commons/network/lib/http/http.dart:104`）。
         */
        public fun friendlyMessage(raw: String, fallback: String = "操作失败"): String {
            val m = raw.lowercase()
            if (m.contains("already pending")) return "已发送过申请，等待对方同意"
            if (m.contains("already friends")) return "你们已经是好友了"
            if (m.contains("cannot friend yourself")) return "不能添加自己"
            if (m.contains("user not found")) return "用户不存在"
            if (m.contains("not the request recipient")) return "无权处理该申请"
            if (raw.trim().isEmpty() || raw.startsWith("HttpRequestException")) return fallback
            return raw
        }
    }
}

private fun imFriendUserFrom(json: JsonObject): ImFriendUser = ImFriendUser.fromJson(json)

private fun imFriendRequestFrom(json: JsonObject): ImFriendRequest = ImFriendRequest.fromJson(json)

/**
 * Dart `(data['items'] as List? ?? const []).whereType<Map>()` 的读法
 * （im_friend_api.dart:91-94/:113-116/:134-137）：`items` 缺失/非数组 → 空表，非对象项跳过。
 */
private suspend fun <Item> ApiClient.getItemsApi(
    path: String,
    query: Map<String, String>,
    fallbackMessage: String,
    parse: (JsonObject) -> Item,
): List<Item> {
    val response = getApi(path, query) { element -> element as? JsonObject }
    val data = response.data
    if (!response.isSuccess || data == null) {
        throw NetworkError.Business(
            code = response.code,
            message = response.message ?: fallbackMessage,
        )
    }
    val items = data["items"] as? JsonArray ?: return emptyList()
    return items.filterIsInstance<JsonObject>().map(parse)
}

// 以下为 feature/friend lane 自带的宽松取值副本：与 feature/chat 的 ImJsonRead.kt 语义相同，
// 但 Spike I 分层规则禁止 feature→feature import，故此处独立实现（见 check-layer-deps.sh）。
private fun JsonObject.imFriendStr(vararg keys: String): String {
    for (key in keys) {
        val el = this[key]
        if (el == null || el is JsonNull) continue
        val text = (el as? JsonPrimitive)?.contentOrNull ?: el.toString()
        return text
    }
    return ""
}

/** 只有 JSON 布尔 true 才算通过（Dart `data['allowed'] == true`）。 */
private fun JsonObject.imFriendIsTrue(key: String): Boolean {
    val el = this[key] as? JsonPrimitive ?: return false
    return !el.isString && el.content == "true"
}

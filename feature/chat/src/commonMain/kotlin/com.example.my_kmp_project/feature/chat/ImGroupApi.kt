package com.example.my_kmp_project.feature.chat

import com.example.my_kmp_project.core.network.ApiClient
import com.example.my_kmp_project.core.network.NetworkError
import com.example.my_kmp_project.core.network.NetworkJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * 契约表 §2.18 行 #109 —— Dart `components/rongcloud_im/lib/api/im_group_api.dart:4-22`
 * 的 `ImFreeGroupResult`。三个键全部 `(... ?? '').toString()`：数字 group_id 也会被转成字符串。
 */
public data class ImFreeGroupResult(
    public val groupId: String,
    public val name: String,
    public val ownerUserId: String,
) {
    public companion object {
        public fun fromJson(json: JsonObject): ImFreeGroupResult = ImFreeGroupResult(
            groupId = json.imJsonStr("group_id"),
            name = json.imJsonStr("name"),
            ownerUserId = json.imJsonStr("owner_user_id"),
        )
    }
}

/**
 * 契约表 #114 —— Dart `ImStoreGroupResult`（im_group_api.dart:24-39）。
 * `store_id` / `group_id` 都是 **String**（服务端按字符串回显）。
 */
public data class ImStoreGroupResult(
    public val storeId: String,
    public val groupId: String,
) {
    public companion object {
        public fun fromJson(json: JsonObject): ImStoreGroupResult = ImStoreGroupResult(
            storeId = json.imJsonStr("store_id"),
            groupId = json.imJsonStr("group_id"),
        )
    }
}

/** `POST /im/groups/free` body：`{name, member_ids:[String]}`（im_group_api.dart:67-70）。 */
@Serializable
public data class ImFreeGroupCreateRequest(
    public val name: String,
    @SerialName("member_ids") val memberIds: List<String> = emptyList(),
)

/** `invite` / `kick` body：`{member_ids:[String]}`（im_group_api.dart:91/:100）。 */
@Serializable
public data class ImGroupMembersRequest(
    @SerialName("member_ids") val memberIds: List<String>,
)

/** `POST /im/groups/store/sync` body：`store_id` 是**字符串**，不是 int（im_group_api.dart:45）。 */
@Serializable
public data class ImStoreGroupSyncRequest(
    @SerialName("store_id") val storeId: String,
)

/**
 * Dart `ImGroupApi`（im_group_api.dart:41-128）—— 自由群 + 门店群的 BFF 侧。
 *
 * 逐条保留的反直觉点（契约表 #109 / #110 / #111 / #112 / #113 / #114、§5.12）：
 * - #110 `invite`、#111 `kick`、#112 `quit`、#113 `dismiss` 走同一个 `_postOk`（:112-127）：
 *   Dart 的返回类型是 `Future<void>`（:86/:95/:104/:108），表里那一列「原始 Map」说的是
 *   `ResultModel.object(json, (m) => m)` 的解码形态，即 `data` 收下即丢弃，
 *   因此 KMP 侧同样返回 `Unit`，**不看 `data` 也不校验它是不是对象**（区别于 #109/#114）；
 * - #112 `quit` / #113 `dismiss` 发的是**显式空对象 `{}`**（:105/:109 传的是 `{}` 字面量），
 *   不是「省略 body」——见 [EMPTY_OBJECT_BODY]；
 * - #110 / #111 的 `member_ids` 即使是空表也要发出去（Dart `{'member_ids': memberIds}`，:91/:100），
 *   [NetworkJson] 开了 `encodeDefaults`，键不会被裁掉；
 * - #114 `store/sync` 是服务端幂等 provisioning（Dart 登录补拉 im_initializer.dart:111-120），
 *   `store_id` 必须按字符串发送。
 *
 * // GAP(R5): 建群成功后 Dart 还要 `RongEngineHolder.joinGroup/dismissGroup` 等 SDK 侧动作
 * // （融云超级群/聊天室不在范围，见 tasks-ultra-fine-v2 M11 + ADR-0012；
 * // 引擎连接与群会话消息仍依赖融云 IM（未接入，无本地 mock 引擎），
 * // 本 lane 只移植 HTTP 契约。
 */
public class ImGroupApi(
    private val client: ApiClient,
) {

    /** Dart `ImGroupApi.syncStoreGroup`（im_group_api.dart:42-59）。 */
    public suspend fun syncStoreGroup(storeId: String): ImStoreGroupResult =
        client.postApiJson(
            path = RongImConfig.GROUPS_STORE_SYNC_PATH,
            body = NetworkJson.encodeToString(
                ImStoreGroupSyncRequest.serializer(),
                ImStoreGroupSyncRequest(storeId = storeId),
            ),
            fallbackMessage = "门店群同步失败",
            parse = { row -> ImStoreGroupResult.fromJson(row) },
        )

    /** Dart `ImGroupApi.createFreeGroup`（im_group_api.dart:61-84）。 */
    public suspend fun createFreeGroup(
        name: String,
        memberIds: List<String> = emptyList(),
    ): ImFreeGroupResult = client.postApiJson(
        path = RongImConfig.GROUPS_FREE_PATH,
        body = NetworkJson.encodeToString(
            ImFreeGroupCreateRequest.serializer(),
            ImFreeGroupCreateRequest(name = name, memberIds = memberIds),
        ),
        fallbackMessage = "建群失败",
        parse = { row -> ImFreeGroupResult.fromJson(row) },
    )

    /**
     * 契约表 #110 —— Dart `ImGroupApi.invite`（im_group_api.dart:86-93）：
     * `POST /im/groups/free/:groupId/invite`，body `{member_ids:[String]}`，走 `_postOk`（:112）。
     */
    public suspend fun invite(groupId: String, memberIds: List<String>): Unit =
        postOk(
            path = imFreeGroupActionPath(groupId, ACTION_INVITE),
            body = encodedMembers(memberIds),
        )

    /**
     * 契约表 #111 —— Dart `ImGroupApi.kick`（im_group_api.dart:95-102）：
     * `POST /im/groups/free/:groupId/kick`，body `{member_ids:[String]}`，走 `_postOk`（:112）。
     */
    public suspend fun kick(groupId: String, memberIds: List<String>): Unit =
        postOk(
            path = imFreeGroupActionPath(groupId, ACTION_KICK),
            body = encodedMembers(memberIds),
        )

    /**
     * 契约表 #112 —— Dart `ImGroupApi.quit`（im_group_api.dart:104-106）：
     * `POST /im/groups/free/:groupId/quit`，body 是**显式空对象 `{}`**，不是省略 body。
     */
    public suspend fun quit(groupId: String): Unit =
        postOk(path = imFreeGroupActionPath(groupId, ACTION_QUIT), body = EMPTY_OBJECT_BODY)

    /**
     * 契约表 #113 —— Dart `ImGroupApi.dismiss`（im_group_api.dart:108-110）：
     * `POST /im/groups/free/:groupId/dismiss`，body 同样是**显式空对象 `{}`**。
     */
    public suspend fun dismiss(groupId: String): Unit =
        postOk(path = imFreeGroupActionPath(groupId, ACTION_DISMISS), body = EMPTY_OBJECT_BODY)

    /** Dart `ImGroupApi._postOk`（im_group_api.dart:112-127）：只看 `code==0`，`data` 丢弃。 */
    private suspend fun postOk(path: String, body: String) {
        val response = client.postApi(path, body) { Unit }
        if (!response.isSuccess) {
            throw NetworkError.Business(
                code = response.code,
                message = response.message ?: GROUP_FALLBACK,
            )
        }
    }

    private fun encodedMembers(memberIds: List<String>): String = NetworkJson.encodeToString(
        ImGroupMembersRequest.serializer(),
        ImGroupMembersRequest(memberIds = memberIds),
    )

    private companion object {
        /** §5.12：动作 POST 要真的带 `{}`，不能省略 body（Ktor 侧空串=不发 body）。 */
        const val EMPTY_OBJECT_BODY: String = "{}"

        /** Dart `'群操作失败'`（im_group_api.dart:123）。 */
        const val GROUP_FALLBACK: String = "群操作失败"

        /** 四个动作路径尾段（im_group_api.dart:90/:99/:105/:109）。 */
        const val ACTION_INVITE: String = "invite"
        const val ACTION_KICK: String = "kick"
        const val ACTION_QUIT: String = "quit"
        const val ACTION_DISMISS: String = "dismiss"
    }
}

/**
 * POST + 手工解码（Dart `ResultModel.object(json, fromJson)` 的 KMP 对等）：
 * 信封非成功码、或 `data` 不是对象 ⇒ 抛 [NetworkError.Business]，
 * 文案取信封 `message`，兜底 [fallbackMessage]。
 */
private suspend fun <Res> ApiClient.postApiJson(
    path: String,
    body: String,
    fallbackMessage: String,
    parse: (JsonObject) -> Res,
): Res {
    val response = postApi(path, body) { element ->
        (element as? JsonObject)?.let(parse)
    }
    val data = response.data
    if (!response.isSuccess || data == null) {
        throw NetworkError.Business(
            code = response.code,
            message = response.message ?: fallbackMessage,
        )
    }
    return data
}

package com.example.my_kmp_project.core.network.realtime

import com.example.my_kmp_project.core.network.ApiClient
import com.example.my_kmp_project.core.network.NetworkFacade
import com.example.my_kmp_project.core.network.NetworkJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

/**
 * 重连后增量补投：`POST /api/v1/realtime/sync`（契约表 §2.17 行 #98）。
 *
 * 对齐 Dart `components/realtime/lib/api/ws_sync_api.dart:7-69`（d7876a1）：
 * - 请求 **camelCase**：`{sinceSeq:int, topics:[String]}`（:14-17）——与全仓 snake_case 习惯不同
 * - 响应同样是**裸 body**：`{events:[RealtimeEnvelope…], latestSeq:int}`（:47-63）
 * - 元素解析只吃 object 项，非 object 项跳过（Dart `if (item is Map)`，:52）
 *
 * 调用时机：`auth_ok` 之后由状态机发起（`app_realtime_client_impl.dart:276-289`），
 * 失败**只记 telemetry + 日志，不阻断连接**（:291-294）。
 */
public class RealtimeWsSyncApi(
    private val client: ApiClient = NetworkFacade.api(),
) {
    public suspend fun sync(
        sinceSeq: Long,
        topics: List<String>,
    ): RealtimeSyncData {
        val raw = client.postRaw(
            path = RealtimeConfig.SYNC_PATH,
            body = NetworkJson.encodeToString(RealtimeSyncRequest(sinceSeq = sinceSeq, topics = topics)),
        )
        return runCatching {
            NetworkJson.decodeFromString(RealtimeSyncData.serializer(), raw)
        }.getOrElse { throw RealtimeWsError.BodyUnparsable(raw, it) }
    }
}

/** 补投请求体（**camelCase**，ws_sync_api.dart:14-17）。 */
@Serializable
public class RealtimeSyncRequest(
    public val sinceSeq: Long,
    public val topics: List<String>,
)

/**
 * 补投响应（ws_sync_api.dart:38-63）：`events` 缺省空表、`latestSeq` 缺省 0 ——
 * 但两个键本身**必须存在**，否则按信封体误用处理直接失败。
 */
@Serializable
public class RealtimeSyncData(
    public val events: List<RealtimeEnvelope>,
    @Serializable(with = RealtimeLongSerializer::class)
    public val latestSeq: Long,
) {
    public companion object {
        /**
         * 手搓解码入口（保留给「拿到已是 JsonObject」的调用方，等价 Dart
         * `WsSyncResult.fromJson`：非 object 项跳过、`latestSeq` 宽容取 0）。
         */
        public fun fromJson(json: JsonObject): RealtimeSyncData {
            val arr = json["events"] as? JsonArray
            val events = arr?.filterIsInstance<JsonObject>()?.map(RealtimeEnvelope::fromJson) ?: emptyList()
            return RealtimeSyncData(
                events = events,
                latestSeq = json.realtimeLong("latestSeq") ?: 0L,
            )
        }
    }
}

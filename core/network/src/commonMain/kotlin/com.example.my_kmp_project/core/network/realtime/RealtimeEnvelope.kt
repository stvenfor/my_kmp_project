package com.example.my_kmp_project.core.network.realtime

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import com.example.my_kmp_project.core.network.NetworkJson

/**
 * 统一 WS Envelope（ping / pong / auth / auth_ok / ack / error / sub / unsub / event）。
 *
 * 逐字段对齐 Dart `commons/core/lib/model/realtime/realtime_envelope.dart:1-57`（d7876a1）：
 * - `id` / `topic` / `ts` / `seq` 可空；`type` 缺键回退 `"unknown"`（:41）
 * - `payload` / `raw` 默认空对象（:9-10）；`ts` / `seq` 同时接受 JSON 数字与数字字符串（:52-56）
 * - [toJson] **省略 null 键**、**总带 `payload`**、**从不带 `raw`**（:28-35）
 *
 * 它既是 `/realtime/sync` 响应里 `events[]` 的元素（契约表 §2.17 #98），也是 WS 双向帧的载荷，
 * 所以编解码**直接吃裸 JSON**，不经过 `{code,data}` 信封解析器。
 */
@Serializable(with = RealtimeEnvelopeSerializer::class)
public class RealtimeEnvelope(
    public val id: String? = null,
    public val type: String = TYPE_UNKNOWN,
    public val topic: String? = null,
    public val ts: Long? = null,
    public val seq: Long? = null,
    public val payload: JsonObject = emptyRealtimeObject,
    public val raw: JsonObject = emptyRealtimeObject,
) {
    /**
     * Dart `payload['name'] as String?`（realtime_envelope.dart:21）—— 强类型读法。
     *
     * 修 bug：此前走宽容的 `realtimeString`，数字 `name` 会变成 "404" 参与按名过滤；
     * Dart 的 `as String?` 对非字符串取值根本不会产生事件名。
     */
    public val eventName: String? get() = payload.realtimeStringValue(KEY_NAME)

    /** Dart `isControl`：控制帧不参与 seq / 路由（realtime_envelope.dart:23-26）。 */
    public val isControl: Boolean get() = type in CONTROL_TYPES

    /** Dart `toJson()`：条件键（id/topic/ts/seq）+ 必带 type/payload（realtime_envelope.dart:28-35）。 */
    public fun toJson(): JsonObject = buildJsonObject {
        id?.let { put(KEY_ID, it) }
        put(KEY_TYPE, type)
        topic?.let { put(KEY_TOPIC, it) }
        ts?.let { put(KEY_TS, it) }
        seq?.let { put(KEY_SEQ, it) }
        put(KEY_PAYLOAD, payload)
    }

    /** `payload` 快捷读取（Dart 侧一律是 `payload['x']` 手搓取值）。 */
    public fun payloadString(key: String): String? = payload.realtimeString(key)

    public fun payloadLong(key: String): Long? = payload.realtimeLong(key)

    public fun payloadBool(key: String): Boolean? = payload.realtimeBool(key)

    /** `payload['topics']`（`sub` / `unsub` 出站帧与 mock 网关回读）。 */
    public fun payloadTopics(): List<String> = payload.realtimeStringList(KEY_TOPICS)

    override fun toString(): String =
        "RealtimeEnvelope(type=$type, topic=$topic, seq=$seq, id=$id)"

    public companion object {
        public const val TYPE_UNKNOWN: String = "unknown"
        public const val KEY_ID: String = "id"
        public const val KEY_TYPE: String = "type"
        public const val KEY_TOPIC: String = "topic"
        public const val KEY_TS: String = "ts"
        public const val KEY_SEQ: String = "seq"
        public const val KEY_PAYLOAD: String = "payload"
        public const val KEY_NAME: String = "name"
        public const val KEY_TOPICS: String = "topics"
        public const val KEY_REF_ID: String = "refId"

        /** Dart switch 分支里的控制帧类型（realtime_envelope.dart:24）。 */
        public val CONTROL_TYPES: Set<String> = setOf(
            "ping",
            "pong",
            "auth",
            "auth_ok",
            "ack",
            "error",
            "sub",
            "unsub",
        )

        /**
         * Dart `RealtimeEnvelope.fromJson`（realtime_envelope.dart:37-50）。
         * 非 object 帧（畸形 JSON）由传输层按 Dart 语义记日志丢弃，所以这里只接受 [JsonObject]。
         */
        public fun fromJson(json: JsonObject): RealtimeEnvelope = RealtimeEnvelope(
            id = realtimeStringOf(json[KEY_ID]),
            type = json.realtimeString(KEY_TYPE) ?: TYPE_UNKNOWN,
            topic = realtimeStringOf(json[KEY_TOPIC]),
            ts = realtimeLongOf(json[KEY_TS]),
            seq = realtimeLongOf(json[KEY_SEQ]),
            payload = json.realtimeObject(KEY_PAYLOAD) ?: emptyRealtimeObject,
            raw = json,
        )

        /** 裸 JSON 文本 → 帧；解析失败或非 object → null。 */
        public fun fromRawJson(raw: String): RealtimeEnvelope? =
            realtimeJsonObjectOrNull(raw)?.let(::fromJson)
    }
}

/** 把 [RealtimeEnvelope] 当作 JsonObject 编解码，复刻 Dart 的键省略规则。 */
public object RealtimeEnvelopeSerializer : KSerializer<RealtimeEnvelope> {
    private val delegate = JsonObject.serializer()
    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun deserialize(decoder: Decoder): RealtimeEnvelope {
        if (decoder is JsonDecoder) {
            return RealtimeEnvelope.fromJson(decoder.decodeJsonElement() as? JsonObject ?: emptyRealtimeObject)
        }
        return RealtimeEnvelope.fromJson(decoder.decodeSerializableValue(delegate))
    }

    override fun serialize(encoder: Encoder, value: RealtimeEnvelope) {
        if (encoder is JsonEncoder) {
            encoder.encodeJsonElement(value.toJson())
            return
        }
        encoder.encodeSerializableValue(delegate, value.toJson())
    }
}

/** 出站帧 → 裸 JSON 文本（Dart `jsonEncode(envelope.toJson())`，websocket_transport.dart:48）。 */
public fun RealtimeEnvelope.asWsText(): String =
    NetworkJson.encodeToString(JsonObject.serializer(), toJson())

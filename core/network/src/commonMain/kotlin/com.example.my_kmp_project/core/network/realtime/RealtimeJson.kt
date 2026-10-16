package com.example.my_kmp_project.core.network.realtime

import com.example.my_kmp_project.core.network.NetworkJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * Realtime（换票 / sync / 入站帧）专用的**宽容标量读取**工具。
 *
 * 为什么要单独一套：`/realtime/ws-ticket` 与 `/realtime/sync` 是**裸 body、camelCase、
 * 无 `{code,data}` 信封**（契约表 §2.17 行 #97/#98；`api-contract-2026-10-16.md:226-227`），
 * Dart 用手搓转换器逐键取值、缺键回退空串或 0
 * （`ws_ticket_api.dart:61-74`、`ws_sync_api.dart:47-69`、`realtime_envelope.dart:37-56`），
 * 这里保持同样语义；但**必填键整体缺失仍会抛** `MissingFieldException` ——
 * 这正是「裸 body 被套进信封后必然失败」的根据（见 `RealtimeWsApiContractTest`）。
 */

/** JSON 数字或数字字符串 → Long；其它 null（Dart `_asInt`，realtime_envelope.dart:52-56）。 */
public fun realtimeLongOf(el: JsonElement?): Long? = when (el) {
    null, is JsonNull -> null
    is JsonPrimitive -> el.contentOrNull?.toLongOrNull()
    else -> null
}

/** 标量取内容，对象/数组取 JSON 文本，null/缺键取 null（Dart `json['x']?.toString()`）。 */
public fun realtimeStringOf(el: JsonElement?): String? = when (el) {
    null, is JsonNull -> null
    is JsonPrimitive -> el.contentOrNull
    else -> el.toString()
}

/**
 * Dart `json['x']?.toString()` 的宽容读法：标量取内容（数字也成字符串），对象/数组取 JSON 文本。
 *
 * 注意与 [realtimeStringValue] 的区别：Dart 只有 `payload['name'] as String?` 一处是**强类型**读法。
 */
public fun JsonObject.realtimeString(key: String): String? =
    (this[key] as? JsonPrimitive)?.contentOrNull

public fun JsonObject.realtimeLong(key: String): Long? = realtimeLongOf(this[key])

/** `payload['name'] as String?`（realtime_envelope.dart:21）：只取**加引号的字符串**标量。 */
public fun JsonObject.realtimeStringValue(key: String): String? =
    (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

/**
 * Dart `extras['silent'] == true`（global_notify_handler.dart:22）：只认**未加引号的 JSON 布尔**。
 *
 * 修 bug：原先走 `contentOrNull?.toBooleanStrictOrNull()`，把字符串 `"true"` 也认成 true；
 * `booleanOrNull` 同样不看 `isString`。Dart 侧 `extras['silent']` 拿到的是 JSON 解码结果，
 * 引号包起来的 "true" 是 String，`== true` 不成立 ⇒ 必须显式排除字符串。
 */
public fun JsonObject.realtimeBool(key: String): Boolean? = when (val el = this[key]) {
    is JsonPrimitive -> if (el.isString) null else el.contentOrNull?.toBooleanStrictOrNull()
    else -> null
}

public fun JsonObject.realtimeObject(key: String): JsonObject? = this[key] as? JsonObject

/** `payload['topics']` 字符串数组（`sub` / `unsub` 帧；Dart `payload['topics'] is List`）。 */
public fun JsonObject.realtimeStringList(key: String): List<String> {
    val arr = this[key] as? JsonArray ?: return emptyList()
    return arr.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
}

/** [JsonObject] 空对象单例（Dart `payload = const {}` / `raw = const {}`，realtime_envelope.dart:9-10）。 */
public val emptyRealtimeObject: JsonObject = JsonObject(emptyMap())

/** 解析一段 JSON 文本为 object；非 object 或畸形 → null（入站帧解析失败时按 Dart 记日志丢弃）。 */
public fun realtimeJsonObjectOrNull(raw: String): JsonObject? =
    runCatching { NetworkJson.parseToJsonElement(raw) as? JsonObject }.getOrNull()

/**
 * 字符串字段宽容序列化器：`null` / 非标量 → 空串（Dart `json['ticket']?.toString() ?? ''`）。
 * 键**缺失**不由本序列化器兜住，仍判缺字段。
 */
public object RealtimeTextSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("RealtimeText", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String {
        val json = decoder as? JsonDecoder ?: return decoder.decodeString()
        return realtimeStringOf(json.decodeJsonElement()) ?: ""
    }

    override fun serialize(encoder: Encoder, value: String) {
        encoder.encodeString(value)
    }
}

/**
 * 64 位整型宽容序列化器（Dart `int` 为 64 位 ⇒ KMP 用 [Long]）。
 * 读不出数字 → `0`（Dart `?? 0`：`ws_ticket_api.dart:65`、`ws_sync_api.dart:61`）。
 */
public object RealtimeLongSerializer : KSerializer<Long> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("RealtimeLong", PrimitiveKind.LONG)

    override fun deserialize(decoder: Decoder): Long {
        val json = decoder as? JsonDecoder ?: return decoder.decodeLong()
        return realtimeLongOf(json.decodeJsonElement()) ?: 0L
    }

    override fun serialize(encoder: Encoder, value: Long) {
        encoder.encodeLong(value)
    }
}

/** 可空 64 位整型宽容序列化器（Dart `int?`：`ts` / `seq`，缺键与非数字都归 null）。 */
public object RealtimeNullableLongSerializer : KSerializer<Long?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("RealtimeLong?", PrimitiveKind.LONG)

    override fun deserialize(decoder: Decoder): Long? {
        val json = decoder as? JsonDecoder ?: return if (decoder.decodeNotNullMark()) decoder.decodeLong() else null
        return realtimeLongOf(json.decodeJsonElement())
    }

    override fun serialize(encoder: Encoder, value: Long?) {
        if (value == null) encoder.encodeNull() else encoder.encodeLong(value)
    }
}

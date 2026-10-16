package com.example.my_kmp_project.core.network.realtime

import com.example.my_kmp_project.core.network.NetworkJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * 统一 WS Envelope 的编解码契约（契约表 §2.17 行 #97–#98）。
 *
 * 真值：Dart `commons/core/lib/model/realtime/realtime_envelope.dart:1-57`（my_ai_project d7876a1）。
 * 锁定的都是「顺手改就破坏等价性」的点：`type` 缺键回退 `unknown`（:41）、
 * `ts`/`seq` 同时接受数字与数字字符串（:52-56）、`payload` 非对象归空（:44-46）、
 * `toJson` 省略 null 键且**从不**带 `raw`（:28-35）。
 */
class RealtimeEnvelopeTest {

    @Test
    fun fromJson_parsesEveryFieldOfABareServerFrame() {
        val frame = requireNotNull(
            RealtimeEnvelope.fromRawJson(
                """{"id":"evt_1","type":"event","topic":"sys.notify","ts":1700000000000,"seq":42,"payload":{"name":"sys.notify.show","notifyId":"n1"}}""",
            ),
        )
        assertEquals("evt_1", frame.id)
        assertEquals("event", frame.type)
        assertEquals("sys.notify", frame.topic)
        assertEquals(1700000000000L, frame.ts)
        assertEquals(42L, frame.seq)
        assertEquals("sys.notify.show", frame.eventName)
        assertEquals("n1", frame.payloadString("notifyId"))
        // Dart `raw = Map<String, dynamic>.from(json)`（:49）：原始帧整份留存。
        assertEquals("sys.notify.show", frame.raw["payload"]?.let { (it as? JsonObject)?.realtimeString("name") })
    }

    @Test
    fun fromJson_missingTypeFallsBackToUnknownLikeDart() {
        // Dart `json['type']?.toString() ?? 'unknown'`（realtime_envelope.dart:41）。
        val frame = RealtimeEnvelope.fromJson(buildJsonObject { put("id", "x") })
        assertEquals(RealtimeEnvelope.TYPE_UNKNOWN, frame.type)
        assertNull(frame.topic)
        assertNull(frame.ts)
        assertNull(frame.seq)
        assertEquals(emptyRealtimeObject, frame.payload)
        assertFalse(frame.isControl)
    }

    @Test
    fun fromJson_readsTsAndSeqFromNumbersAndNumericStringsOnly() {
        // Dart `_asInt`：int 原样、String tryParse、其它 null（realtime_envelope.dart:52-56）。
        val numeric = requireNotNull(
            RealtimeEnvelope.fromRawJson("""{"type":"event","ts":"1700000000001","seq":"7"}"""),
        )
        assertEquals(1700000000001L, numeric.ts)
        assertEquals(7L, numeric.seq)

        val garbage = requireNotNull(
            RealtimeEnvelope.fromRawJson("""{"type":"event","ts":"abc","seq":{}}"""),
        )
        assertNull(garbage.ts)
        assertNull(garbage.seq)

        val explicitNull = requireNotNull(
            RealtimeEnvelope.fromRawJson("""{"type":"event","seq":null}"""),
        )
        assertNull(explicitNull.seq)
    }

    @Test
    fun fromJson_nonObjectPayloadBecomesEmptyObject() {
        // Dart `payloadRaw is Map ? ... : const {}`（realtime_envelope.dart:44-46）。
        for (raw in listOf(
            """{"type":"event","payload":[1,2]}""",
            """{"type":"event","payload":"text"}""",
            """{"type":"event","payload":null}""",
        )) {
            val frame = requireNotNull(RealtimeEnvelope.fromRawJson(raw))
            assertEquals(emptyRealtimeObject, frame.payload, raw)
            assertNull(frame.eventName)
        }
    }

    @Test
    fun isControl_coversExactlyTheEightDartControlTypes() {
        // Dart switch 分支（realtime_envelope.dart:23-26）。
        val control = setOf("ping", "pong", "auth", "auth_ok", "ack", "error", "sub", "unsub")
        assertEquals(control, RealtimeEnvelope.CONTROL_TYPES)
        for (type in control) {
            assertTrue(RealtimeEnvelope(type = type).isControl, type)
        }
        assertFalse(RealtimeEnvelope(type = "event").isControl)
        assertFalse(RealtimeEnvelope(type = RealtimeEnvelope.TYPE_UNKNOWN).isControl)
    }

    @Test
    fun eventName_ignoresNonStringNames() {
        // Dart `payload['name'] as String?`：非字符串一律 null。
        assertNull(RealtimeEnvelope.fromRawJson("""{"type":"event","payload":{"name":404}}""")?.eventName)
        assertNull(RealtimeEnvelope.fromRawJson("""{"type":"event","payload":{}}""")?.eventName)
    }

    @Test
    fun toJson_omitsNullConditionalKeysAndNeverWritesRaw() {
        // Dart toJson（realtime_envelope.dart:28-35）：id/topic/ts/seq 条件键，type/payload 必带。
        val minimal = RealtimeEnvelope(type = "ping")
        val json = minimal.toJson()
        assertEquals(setOf("type", "payload"), json.keys)
        assertEquals(emptyRealtimeObject, json["payload"])

        val full = RealtimeEnvelope(
            id = "m1",
            type = "event",
            topic = "live.signal.r1",
            ts = 5L,
            seq = 6L,
            payload = buildJsonObject { put("name", "live.start") },
            raw = buildJsonObject { put("leak", "must-not-travel") },
        )
        val outbound = full.toJson()
        assertEquals(listOf("id", "type", "topic", "ts", "seq", "payload"), outbound.keys.toList())
        assertNull(outbound["raw"])
        assertEquals("6", outbound.realtimeLong("seq")?.toString())
    }

    @Test
    fun asWsText_roundTripsBackIntoTheSameFrame() {
        // Dart 出站是 `jsonEncode(envelope.toJson())`（websocket_transport.dart:48）。
        val sent = RealtimeEnvelope(
            id = "auth_1",
            type = "auth",
            ts = 9L,
            payload = buildJsonObject {
                put("ticket", "t")
                put("platform", RealtimeConfig.PLATFORM_MOBILE)
            },
        )
        val text = sent.asWsText()
        val back = requireNotNull(RealtimeEnvelope.fromRawJson(text))
        assertEquals("auth_1", back.id)
        assertEquals("auth", back.type)
        assertEquals(9L, back.ts)
        assertEquals("t", back.payloadString("ticket"))
        assertEquals("mobile", back.payloadString("platform"))
        assertNull(back.topic)
    }

    @Test
    fun fromRawJson_dropsMalformedAndNonObjectLikeDartTransport() {
        // 传输层对畸形帧「记日志后丢弃」（websocket_transport.dart:29-34），不抛。
        assertNull(RealtimeEnvelope.fromRawJson("not json"))
        assertNull(RealtimeEnvelope.fromRawJson("[1,2,3]"))
        assertNull(RealtimeEnvelope.fromRawJson("\"just a string\""))
    }

    @Test
    fun kotlinxSerializer_roundTripsThroughNetworkJson() {
        val frame = RealtimeEnvelope(
            id = "e1",
            type = "event",
            topic = RealtimeTopics.SYS_NOTIFY,
            seq = 3L,
            payload = buildJsonObject { put("notifyId", "n1") },
        )
        val text = NetworkJson.encodeToString(RealtimeEnvelope.serializer(), frame)
        val decoded = NetworkJson.decodeFromString(RealtimeEnvelope.serializer(), text)
        assertEquals(frame.id, decoded.id)
        assertEquals(frame.type, decoded.type)
        assertEquals(frame.topic, decoded.topic)
        assertEquals(frame.seq, decoded.seq)
        assertEquals("n1", decoded.payloadString("notifyId"))
    }

    @Test
    fun payloadHelpers_readTolerantly() {
        val payload: JsonObject = requireNotNull(
            RealtimeEnvelope.fromRawJson(
                """{"type":"sub","payload":{"topics":["sys.notify","presence.bulk"],"n":"7","flag":true,"bad":"nope","obj":{"a":1}}}""",
            ),
        ).payload

        assertEquals(listOf("sys.notify", "presence.bulk"), payload.realtimeStringList("topics"))
        assertEquals(emptyList(), payload.realtimeStringList("missing"))
        assertEquals(7L, payload.realtimeLong("n"))
        assertNull(payload.realtimeLong("topics"))
        assertEquals("nope", payload.realtimeString("bad"))
        assertNull(payload.realtimeString("obj"))
        assertEquals(true, payload.realtimeBool("flag"))
        assertNull(payload.realtimeBool("missing"))
        assertNull(payload.realtimeString("obj"))
        assertNull(payload.realtimeString("topics"))
        assertEquals(buildJsonObject { put("a", 1) }, payload.realtimeObject("obj"))
        assertNull(payload.realtimeObject("topics"))
    }

    @Test
    fun realtimeBool_onlyAcceptsAnUnquotedJsonBoolean() {
        // Dart `extras['silent'] == true`（global_notify_handler.dart:22）：只认真布尔。
        val obj: JsonObject = requireNotNull(
            RealtimeEnvelope.fromRawJson(
                """{"type":"event","payload":{"yes":true,"no":false,"str":"true","num":1}}""",
            ),
        ).payload
        assertEquals(true, obj.realtimeBool("yes"))
        assertEquals(false, obj.realtimeBool("no"))
        assertNull(obj.realtimeBool("num"))
        // 引号包起来的 "true" 在 Dart 里 `== true` 不成立 ⇒ 必须解析为 null（曾为生产 bug）。
        assertNull(obj.realtimeBool("str"))
        assertNull(obj.realtimeBool("missing"))
    }
}

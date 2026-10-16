package com.example.my_kmp_project.component.realtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 状态枚举与遥测组装 —— 真值 Dart
 * `commons/core/lib/model/realtime/realtime_connection_state.dart:2-25` 与
 * `components/realtime/lib/telemetry/realtime_telemetry.dart:4-49`（d7876a1）。
 *
 * 这两处都很小，但都是**对外契约**：
 * - `isActive` 只有 `connected` / `degraded` 两个（客户端拿它决定要不要发 `sub` / `unsub` 帧，
 *   以及闸门拿它决定「已经连上了就别再连」）；
 * - `label` 是调试页与日志的中文文案，逐字对齐；
 * - 遥测的**事件名** `realtime_<type>` 和 payload 里 `type`/`name` 恒定在前，
 *   是 Dart 侧总线订阅方的读法（`EventBusUtils.post(CustomEvent(...))`）。
 */
class RealtimeStateAndTelemetryTest {

    @Test
    fun isActiveCoversExactlyConnectedAndDegraded(): Unit = RealtimeConnectionState.entries.forEach {
        val expected = it == RealtimeConnectionState.Connected || it == RealtimeConnectionState.Degraded
        assertEquals(expected, it.isActive, it.name)
    }

    @Test
    fun statesThatMustNotCountAsActive(): Unit {
        assertFalse(RealtimeConnectionState.Reconnecting.isActive)
        assertFalse(RealtimeConnectionState.Connecting.isActive)
        assertFalse(RealtimeConnectionState.Authenticating.isActive)
        assertFalse(RealtimeConnectionState.Failed.isActive)
        assertFalse(RealtimeConnectionState.Disconnected.isActive)
    }

    @Test
    fun labelsMatchTheDartChineseTextVerbatim(): Unit {
        val expected = mapOf(
            RealtimeConnectionState.Disconnected to "未连接",
            RealtimeConnectionState.Connecting to "连接中",
            RealtimeConnectionState.Authenticating to "鉴权中",
            RealtimeConnectionState.Connected to "已连接",
            RealtimeConnectionState.Reconnecting to "重连中",
            RealtimeConnectionState.Degraded to "降级",
            RealtimeConnectionState.Failed to "失败",
        )
        assertEquals(expected.keys.size, RealtimeConnectionState.entries.size)
        expected.forEach { (state, label) -> assertEquals(label, state.label, state.name) }
    }

    @Test
    fun metricPayloadPutsTypeAndNameFirstThenFlattensParams(): Unit {
        val sink = RecordingTelemetrySink()
        RealtimeTelemetry(sink).metric("ws_state", mapOf("state" to "Connected", "seq" to 7L))

        val event = sink.events.single()
        assertEquals("realtime_metric", event.eventType)
        // Dart `_report`（:31-48）：type、name 打底，params **平铺**进来（不是嵌一层）。
        assertEquals(
            listOf("type", "name", "state", "seq"),
            event.payload.keys.toList(),
        )
        assertEquals("metric", event.payload["type"])
        assertEquals("ws_state", event.payload["name"])
        assertEquals(7L, event.payload["seq"])
    }

    @Test
    fun metricWithoutParamsCarriesOnlyTypeAndName(): Unit {
        val sink = RecordingTelemetrySink()
        RealtimeTelemetry(sink).metric("ws_noop")

        assertEquals(listOf("type", "name"), sink.events.single().payload.keys.toList())
    }

    @Test
    fun errorAppendsTheThrowableToStringAfterTheParams(): Unit {
        val sink = RecordingTelemetrySink()
        val boom = IllegalStateException("握手被拒")
        RealtimeTelemetry(sink).error("ws_connect_fail", boom, mapOf("attempt" to 2))

        val event = sink.events.single()
        assertEquals("realtime_error", event.eventType)
        assertEquals(
            listOf("type", "name", "attempt", "error"),
            event.payload.keys.toList(),
        )
        // Dart :16 用 `'$error'` 插值 ⇒ 等价于 toString()，含异常类型名。
        assertTrue((event.payload["error"] ?: "").toString().contains("握手被拒"))
        assertTrue((event.payload["error"] ?: "").toString().contains("IllegalStateException"))
        assertEquals(2, event.payload["attempt"])
    }

    @Test
    fun traceRequiresDurationAndAppendsItLast(): Unit {
        val sink = RecordingTelemetrySink()
        RealtimeTelemetry(sink).trace("ws_sync", 42L)

        val event = sink.events.single()
        assertEquals("realtime_trace", event.eventType)
        assertEquals(listOf("type", "name", "durationMs"), event.payload.keys.toList())
        assertEquals(42L, event.payload["durationMs"])
    }

    @Test
    fun telemetryTypeLiteralsAreTheBusEventSuffixes(): Unit {
        // 事件名 = "realtime_" + 这些字面量；改一个就断一条订阅。
        assertEquals("metric", RealtimeTelemetryTypes.METRIC)
        assertEquals("error", RealtimeTelemetryTypes.ERROR)
        assertEquals("trace", RealtimeTelemetryTypes.TRACE)
    }

    @Test
    fun threeEntryPointsProduceThreeDistinctEventNames(): Unit {
        val sink = RecordingTelemetrySink()
        val telemetry = RealtimeTelemetry(sink)
        telemetry.metric("m")
        telemetry.error("e", IllegalStateException("x"))
        telemetry.trace("t", 1L)

        assertEquals(
            listOf("realtime_metric", "realtime_error", "realtime_trace"),
            sink.events.map { it.eventType },
        )
        assertEquals(
            listOf("metric", "error", "trace"),
            sink.events.map { it.payload["type"] },
        )
    }

    @Test
    fun defaultSinkIsLogOnlyAndNeverThrows(): Unit {
        // 装配层没接总线时走这个：只打日志，不假装已经上报（等价 Dart 的第 1 步）。
        val telemetry = RealtimeTelemetry(LoggingRealtimeTelemetrySink)
        telemetry.metric("ws_state", mapOf("state" to "Connected"))
        telemetry.error("ws_sync_fail", IllegalStateException("x"))
        telemetry.trace("ws_send", 3L)
        telemetry.metric("ws_noop")
    }
}

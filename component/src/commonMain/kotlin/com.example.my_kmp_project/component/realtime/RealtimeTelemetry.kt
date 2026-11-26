package com.example.my_kmp_project.component.realtime

import com.example.my_kmp_project.core.log.AppLog

/**
 * 指标 / 异常 / 耗时上报 —— 逐行对齐 Dart
 * `components/realtime/lib/telemetry/realtime_telemetry.dart:4-49`（d7876a1）。
 *
 * Dart 的 `_report`（:31-48）做两件事：
 * 1. `LogUtils.i('[RealtimeTelemetry] $payload')`
 * 2. `EventBusUtils.post(CustomEvent(eventType: 'realtime_$type', eventValue: payload))`
 *
 * KMP 侧没有全局事件总线依赖（`core/analytics` 目前是占位骨架），
 * 因此第 2 步抽成 [RealtimeTelemetrySink] 缝隙：装配层可接自己的总线 / APM，
 * 默认 [LoggingRealtimeTelemetrySink] 只打日志（等价于 Dart 的第 1 步，且不会假装已上报）。
 *
 * payload 组装规则与 Dart 一致：`type`、`name` 两个键**恒定在前**，
 * params 平铺进来（Dart 的 `if (params != null) ...params`），
 * `error` / `durationMs` 由各自方法追加（:15、:27）。
 */

/** 遥测类型字面量（Dart :6、:12、:22；同时决定事件名 `realtime_<type>`）。 */
public object RealtimeTelemetryTypes {
    public const val METRIC: String = "metric"
    public const val ERROR: String = "error"
    public const val TRACE: String = "trace"
}

/** 上报出口：[eventType] 形如 `realtime_metric`；[payload] 为 Dart 那份扁平 map。 */
public fun interface RealtimeTelemetrySink {
    public fun report(eventType: String, payload: Map<String, Any?>)
}

/** 默认出口：与 Dart 一样只落日志，不外发。 */
public object LoggingRealtimeTelemetrySink : RealtimeTelemetrySink {
    override fun report(eventType: String, payload: Map<String, Any?>) {
        AppLog.d("RealtimeTelemetry", "$eventType $payload")
    }
}

/**
 * 三个入口对应 Dart 的 `metric` / `error` / `trace`。
 *
 * [error] 的入参用 [Throwable]（Dart 是 `Object error`，调用点全是 `Exception(...)` 或原异常），
 * 取值一律走 `toString()`，与 Dart :16 相同。
 */
public class RealtimeTelemetry(
    private val sink: RealtimeTelemetrySink = LoggingRealtimeTelemetrySink,
) {
    /** Dart `metric(name, {params})`（:5-7）。 */
    public fun metric(name: String, params: Map<String, Any?>? = null) {
        report(RealtimeTelemetryTypes.METRIC, name, params)
    }

    /** Dart `error(name, error, {params})`（:9-18）：params 里追加 `error` 键。 */
    public fun error(name: String, error: Throwable, params: Map<String, Any?>? = null) {
        report(
            RealtimeTelemetryTypes.ERROR,
            name,
            buildMap {
                params?.let { putAll(it) }
                put("error", error.toString())
            },
        )
    }

    /** Dart `trace(name, {required durationMs, params})`（:20-29）：params 里追加 `durationMs`。 */
    public fun trace(name: String, durationMs: Long, params: Map<String, Any?>? = null) {
        report(
            RealtimeTelemetryTypes.TRACE,
            name,
            buildMap {
                params?.let { putAll(it) }
                put("durationMs", durationMs)
            },
        )
    }

    /** Dart `_report`（:31-48）：`type` + `name` 打底，params 平铺，事件名 `realtime_$type`。 */
    private fun report(type: String, name: String, params: Map<String, Any?>?) {
        val payload = buildMap {
            put("type", type)
            put("name", name)
            params?.let { putAll(it) }
        }
        sink.report("realtime_$type", payload)
    }
}

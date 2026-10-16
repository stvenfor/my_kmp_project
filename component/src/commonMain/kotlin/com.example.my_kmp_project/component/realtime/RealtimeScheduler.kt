package com.example.my_kmp_project.component.realtime

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Realtime 的时间缝隙 —— 状态机所有定时行为都从这里取时间，
 * 于是测试可以用虚拟时钟驱动，不碰 `delay()` 竞争（本仓库不引 `kotlinx-coroutines-test`）。
 *
 * Dart 侧直接用手 `Timer` / `DateTime.now()`：
 * `connection/heartbeat_scheduler.dart:53-82`、`client/app_realtime_client_impl.dart:341-354`。
 */

/** 墙钟（毫秒）。Dart `DateTime.now().millisecondsSinceEpoch`（int 是 64 位 ⇒ 用 [Long]）。 */
public fun interface RealtimeClock {
    public fun nowMs(): Long
}

/** 生产时钟：与 feature 层同款 `Clock.System`（Kotlin 2.2 stdlib，需 [ExperimentalTime]）。 */
public object SystemRealtimeClock : RealtimeClock {
    @OptIn(ExperimentalTime::class)
    override fun nowMs(): Long = Clock.System.now().toEpochMilliseconds()
}

/** 一次已排定的回调的取消句柄（Dart `Timer?.cancel()`）。 */
public fun interface RealtimeCancellable {
    public fun cancel()
}

/**
 * 相对定时调度：`after(delayMs) { … }` = Dart `Timer(delay, callback)`。
 *
 * 约定（实现必须遵守）：**任务体开始执行时句柄即失效**，
 * 调用方要在动作体第一行把自己那个字段置 null，
 * 否则「动作里再排下一次」会取消掉正在运行的自己（Dart 的 `Timer` 无此问题）。
 */
public interface RealtimeTicker {
    public fun after(delayMs: Long, action: suspend () -> Unit): RealtimeCancellable
}

/** 生产实现：挂在调用方给的 [scope] 上（app 装配层持有 scope，组件层不建全局作用域）。 */
public class CoroutineRealtimeTicker(
    private val scope: CoroutineScope,
) : RealtimeTicker {
    override fun after(delayMs: Long, action: suspend () -> Unit): RealtimeCancellable {
        val job = scope.launch {
            if (delayMs > 0L) delay(delayMs)
            action()
        }
        return RealtimeCancellable { job.cancel() }
    }
}

/**
 * Dart `AppLifecycleState` 的最小对等（`app_realtime_client_impl.dart:497-509` 只用到
 * resumed / paused 两支；其余状态在 Dart 里也是 `default` 什么都不做）。
 */
public enum class RealtimeAppLifecycle { Resumed, Paused, Other }

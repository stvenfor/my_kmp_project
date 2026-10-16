package com.example.my_kmp_project.component.realtime

import com.example.my_kmp_project.core.network.realtime.RealtimeConfig

/**
 * 应用层 ping/pong 心跳 —— 逐行对齐 Dart
 * `components/realtime/lib/connection/heartbeat_scheduler.dart:16-82`（d7876a1）。
 *
 * 保留的「反直觉」细节（勿顺手改掉，否则行为不等价）：
 * 1. **不等 pong 也排下一次 ping**（:81）⇒ 同一时刻可能有多个 pong 超时计时在跑；
 * 2. `onPong(id)` **只有 id 与当前 pending 相同**才清零 missCount（:47）；
 * 3. miss 计数在超时回调里 `++`，达到 [RealtimeConfig.HEARTBEAT_MAX_MISS]（=2）才通知重连并**直接 return**
 *    （不再排下一次，:72-75）；未达上限则清 pending 后重新排程（:76-77）；
 * 4. ping 发送异常只记日志，不影响计时（:61-65）。
 *
 * ping id 格式 `ping_<epochMillis>`（:59）—— Go 侧按同 id 回 pong，故 id 必须可重复比较。
 */
public class HeartbeatScheduler(
    private val ticker: RealtimeTicker,
    private val clock: RealtimeClock = SystemRealtimeClock,
    private val intervalMs: Long = RealtimeConfig.HEARTBEAT_INTERVAL_MS,
    private val timeoutMs: Long = RealtimeConfig.HEARTBEAT_TIMEOUT_MS,
    private val maxMiss: Int = RealtimeConfig.HEARTBEAT_MAX_MISS,
    private val onSendPing: suspend (pingId: String) -> Unit,
    private val onTimeout: () -> Unit,
) {
    private var intervalTask: RealtimeCancellable? = null
    private var timeoutTask: RealtimeCancellable? = null
    private var missCount: Int = 0
    private var pendingPingId: String? = null

    /** 观测口（Dart 没有 getter，但调试页 / 单测需要；只读，不参与协议）。 */
    public val currentMiss: Int get() = missCount
    public val currentPendingPingId: String? get() = pendingPingId

    /** Dart `start()`：先 `stop()` 再排第一次（:31-34）。 */
    public fun start() {
        stop()
        scheduleNext()
    }

    /** Dart `stop()`：取消两个计时、miss 与 pending 归零（:36-43）。 */
    public fun stop() {
        intervalTask?.cancel()
        timeoutTask?.cancel()
        intervalTask = null
        timeoutTask = null
        missCount = 0
        pendingPingId = null
    }

    /** Dart `onPong(pingId)`：id 不匹配直接忽略（:46-51）。 */
    public fun onPong(pingId: String) {
        if (pendingPingId != pingId) return
        timeoutTask?.cancel()
        timeoutTask = null
        pendingPingId = null
        missCount = 0
    }

    private fun scheduleNext() {
        intervalTask?.cancel()
        intervalTask = ticker.after(intervalMs) {
            // 句柄失效：动作体第一行清空，避免后续 scheduleNext() 取消掉正在跑的自己。
            intervalTask = null
            firePing()
        }
    }

    private suspend fun firePing() {
        val pingId = "ping_${clock.nowMs()}"
        pendingPingId = pingId
        runCatching { onSendPing(pingId) }
            .onFailure { println("[Realtime] heartbeat send failed: ${it.message}") }

        timeoutTask?.cancel()
        timeoutTask = ticker.after(timeoutMs) {
            timeoutTask = null
            missCount++
            println("[Realtime] heartbeat timeout miss=$missCount pingId=$pingId")
            if (missCount >= maxMiss) {
                onTimeout()
                return@after
            }
            pendingPingId = null
            scheduleNext()
        }

        // 不等待 pong 也排下一次 ping（超时计时并行跑）。
        scheduleNext()
    }
}

package com.example.my_kmp_project.component.realtime

import com.example.my_kmp_project.core.network.realtime.RealtimeConfig
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

/**
 * 重连指数退避 + 抖动 —— 复刻 Dart `components/realtime/lib/connection/reconnect_policy.dart:6-24`（d7876a1）。
 *
 * 序列（[RealtimeConfig]：1s 起、×2、上限 60s、抖动 ±20%、地板 500ms）：
 * `1000 → 2000 → 4000 → 8000 → 16000 → 32000 → 60000 → 60000 …`，
 * 每档再叠加 `random.nextInt(jitter*2) - jitter`（jitter = `capped*0.2` 取整），最后 `max(500, …)`。
 *
 * 与 Dart 的唯一差别是刻意加的指数上限（[MAX_EXPONENT]）：
 * Dart 用 `pow(2.0, n).toInt()`，n≥1024 时 `Infinity.toInt()` 会抛；
 * 这里把指数夹到 20（2^20 ms ≈ 17 分钟）早已超过 60s 上限，取到的值仍是 60000。
 */
public class ReconnectPolicy(
    private val random: Random = Random.Default,
    private val initialMs: Long = RealtimeConfig.RECONNECT_INITIAL_MS,
    private val maxMs: Long = RealtimeConfig.RECONNECT_MAX_MS,
    private val multiplier: Double = RealtimeConfig.RECONNECT_MULTIPLIER,
    private val jitterRatio: Double = RealtimeConfig.RECONNECT_JITTER_RATIO,
    private val floorMs: Long = RealtimeConfig.RECONNECT_FLOOR_MS,
) {
    private var _attempt: Int = 0

    /** Dart `attempt`：已消耗的重连次数（`reset()` 后归零）。 */
    public val attempt: Int get() = _attempt

    /** Dart `reset()`：`auth_ok` 后清零（app_realtime_client_impl.dart:240）。 */
    public fun reset() {
        _attempt = 0
    }

    /** 下一次等待时长（毫秒）。每次调用自增 [attempt]（Dart `nextDelay()` 同款副作用）。 */
    public fun nextDelayMs(): Long {
        _attempt++
        val exponent = (_attempt - 1).coerceAtMost(MAX_EXPONENT)
        val baseMs = initialMs * multiplier.pow(exponent.toDouble()).toLong()
        val capped = min(baseMs, maxMs)
        val jitter = (capped * jitterRatio).toLong()
        val delta = if (jitter == 0L) 0L else random.nextInt((jitter * 2).toInt()).toLong() - jitter
        return maxOf(floorMs, capped + delta)
    }

    private companion object {
        const val MAX_EXPONENT: Int = 20
    }
}

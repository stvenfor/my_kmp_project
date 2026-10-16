package com.example.my_kmp_project.component.realtime

import com.example.my_kmp_project.core.network.realtime.RealtimeConfig
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 重连退避 —— 真值 Dart `components/realtime/lib/connection/reconnect_policy.dart:6-24`（d7876a1），
 * 以及 Dart 自己的单测 `components/realtime/test/reconnect_policy_test.dart`（`Random(0)`：
 * 首拍 ≥500ms、第三拍 ≤60000ms、`reset()` 后 `attempt == 0`）。
 *
 * 序列（`jitterRatio = 0` 时去掉随机项）必须是 `1000 → 2000 → 4000 → 8000 → 16000 → 32000 → 60000 → 60000`，
 * 抖动区间是 `capped ± (capped*0.2)`；**上界不夹回 maxMs**（Dart 同款：`max(500, capped + delta)`）。
 */
class ReconnectPolicyTest {

    /** 关掉抖动的确定性退避（对齐 Dart 公式里 jitter == 0 ⇒ delta == 0 的分支）。 */
    private fun deterministic(): ReconnectPolicy =
        ReconnectPolicy(random = Random(0), jitterRatio = 0.0)

    @Test
    fun exponentialSequenceFollowsDartUntilTheCap() {
        val policy = deterministic()
        val delays = (1..8).map { policy.nextDelayMs() }
        assertEquals(listOf(1_000L, 2_000L, 4_000L, 8_000L, 16_000L, 32_000L, 60_000L, 60_000L), delays)
    }

    @Test
    fun everyCallBumpsTheAttemptAndResetZeroesIt() {
        // Dart 测试的 `policy.reset(); expect(policy.attempt, 0)`。
        val policy = deterministic()
        assertEquals(0, policy.attempt)
        policy.nextDelayMs()
        policy.nextDelayMs()
        assertEquals(2, policy.attempt)
        policy.reset()
        assertEquals(0, policy.attempt)
        assertEquals(1_000L, policy.nextDelayMs())
    }

    @Test
    fun dartTestShape_firstCallAtLeastFloorAndThirdCallWithinCap() {
        // 复刻 reconnect_policy_test.dart：`ReconnectPolicy(random: Random(0))`。
        val policy = ReconnectPolicy(random = Random(0))
        assertTrue(policy.nextDelayMs() >= 500L)
        policy.nextDelayMs()
        val third = policy.nextDelayMs()
        assertTrue(third <= 60_000L, "third=$third")
        policy.reset()
        assertEquals(0, policy.attempt)
    }

    @Test
    fun jitterStaysInsideTheDartWindowAndNeverDropsBelowTheFloor() {
        // Dart delta = nextInt(jitter*2) - jitter ⇒ 落在 [-jitter, +jitter]。
        val capped = 10_000L
        val jitter = (capped * RealtimeConfig.RECONNECT_JITTER_RATIO).toLong() // 2000
        repeat(200) { seed ->
            val policy = ReconnectPolicy(
                random = Random(seed.toLong()),
                initialMs = capped,
                maxMs = 60_000L,
            )
            val delay = policy.nextDelayMs()
            assertTrue(delay in (capped - jitter)..(capped + jitter), "seed=$seed delay=$delay")
        }
    }

    @Test
    fun floorClampsTinyWindowsLikeDartMax500() {
        // Dart `max(500, capped + delta)`：初始 200ms、抖动 20% ⇒ 结果恒为地板 500。
        val policy = ReconnectPolicy(
            random = Random(7),
            initialMs = 200L,
            maxMs = 300L,
            floorMs = 500L,
        )
        repeat(50) {
            assertEquals(500L, policy.nextDelayMs())
        }
    }

    @Test
    fun jitterOfZeroNeverCallsTheRandomSource() {
        // Dart jitter == 0 时走 delta = 0 分支（reconnect_policy.dart:21）。
        val boom = object : Random() {
            override fun nextBits(bitCount: Int): Int = throw AssertionError("不该取随机数")
        }
        val policy = ReconnectPolicy(
            random = boom,
            initialMs = 1_000L,
            jitterRatio = 0.0,
        )
        assertEquals(1_000L, policy.nextDelayMs())
    }

    @Test
    fun veryHighAttemptDoesNotOverflowTheDartPowPath() {
        // Dart `pow(2.0, n).toInt()` 在 n≥1024 会因 Infinity.toInt() 抛；
        // KMP 把指数夹到 20（见 MAX_EXPONENT），第 300 次仍必须是 60000ms 档。
        val policy = ReconnectPolicy(random = Random(0), jitterRatio = 0.0)
        repeat(300) { policy.nextDelayMs() }
        assertEquals(300, policy.attempt)
        assertEquals(60_000L, policy.nextDelayMs())
    }

    @Test
    fun multiplierIsHonouredSoBackoffGrowsSlowerWhenConfigured() {
        val policy = ReconnectPolicy(
            random = Random(0),
            initialMs = 1_000L,
            maxMs = 60_000L,
            multiplier = 3.0,
            jitterRatio = 0.0,
        )
        assertEquals(listOf(1_000L, 3_000L, 9_000L, 27_000L, 60_000L), (1..5).map { policy.nextDelayMs() })
    }
}

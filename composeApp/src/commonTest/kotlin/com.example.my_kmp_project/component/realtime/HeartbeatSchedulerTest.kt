package com.example.my_kmp_project.component.realtime

import com.example.my_kmp_project.core.network.realtime.RealtimeConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 应用层心跳 —— 真值 Dart `components/realtime/lib/connection/heartbeat_scheduler.dart:16-82`（d7876a1）。
 *
 * 四个「勿顺手改掉」的反直觉点（KDoc 已点名，这里逐条钉住）：
 * 1. **不等 pong 也排下一次 ping**（:81）⇒ 同一时刻可能并行跑着多个 pong 超时计时；
 * 2. `onPong(id)` **只有 id 与当前 pending 相同**才清零 missCount（:47）；
 * 3. miss 计数在超时回调里 `++`，达到 `HEARTBEAT_MAX_MISS`（=2）才通知重连并**直接 return**
 *    （不再排下一次，:72-75）；未达上限则清 pending 后重新排程（:76-77）；
 * 4. ping 发送异常只记日志，不影响计时（:61-65）。
 *
 * ping id 格式 `ping_<epochMillis>`（:59）—— Go 按同 id 回 pong，故 id 必须可比较。
 */
class HeartbeatSchedulerTest {

    private val ticker = ManualRealtimeTicker()
    private val clock = SteppingRealtimeClock(2_000L)
    private val pings = mutableListOf<String>()
    private var timeouts = 0

    private fun scheduler(
        onSendPing: suspend (String) -> Unit = { pings += it },
    ) = HeartbeatScheduler(
        ticker = ticker,
        clock = clock,
        onSendPing = onSendPing,
        onTimeout = { timeouts++ },
    )

    @Test
    fun startSchedulesExactlyOneIntervalOfTwentyFiveSeconds() {
        // Dart start() = stop() + _scheduleNext()（:31-34）。
        scheduler().start()
        assertEquals(listOf(RealtimeConfig.HEARTBEAT_INTERVAL_MS), ticker.createdDelays)
        assertTrue(pings.isEmpty())
    }

    @Test
    fun startTwiceDoesNotStackTimers() {
        val hb = scheduler()
        hb.start()
        hb.start()
        assertEquals(listOf(25_000L, 25_000L), ticker.createdDelays)
        assertEquals(1, ticker.pendingDelays.size)
    }

    @Test
    fun pingIdIsPingUnderscoreEpochMillisAndIsExposedAsPending() {
        val hb = scheduler()
        hb.start()
        ticker.runFirst()
        assertEquals(listOf("ping_2000"), pings)
        assertEquals("ping_2000", hb.currentPendingPingId)
        assertEquals(0, hb.currentMiss)
    }

    @Test
    fun afterPingBothTheTimeoutAndTheNextIntervalAreArmedInParallel() {
        // Dart _firePing 末尾无条件 _scheduleNext()（:81），并行跑 pong 超时计时（:70）。
        scheduler().start()
        ticker.runFirst()
        assertEquals(listOf(25_000L, 10_000L, 25_000L), ticker.createdDelays)
        assertEquals(listOf(10_000L, 25_000L), ticker.pendingDelays)
    }

    @Test
    fun matchingPongCancelsTheTimeoutAndClearsMissAndPending() {
        val hb = scheduler()
        hb.start()
        ticker.runFirst()
        // 先制造一次 miss，再让同 id 的 pong 归零。
        ticker.runFirst() // 10s 超时 ⇒ miss=1
        assertEquals(1, hb.currentMiss)
        clock.advance(1L)
        hb.onPong("ping_2000") // 上一拍的 id 已被清成 null ⇒ 不匹配，什么都不做
        assertEquals(1, hb.currentMiss)
        ticker.runFirst() // 下一次 ping（id = ping_2001）
        assertEquals("ping_2001", hb.currentPendingPingId)
        hb.onPong("ping_2001")
        assertEquals(0, hb.currentMiss)
        assertNull(hb.currentPendingPingId)
        assertEquals(0, timeouts)
    }

    @Test
    fun pongWithAWrongIdIsIgnoredEntirely() {
        // Dart `if (_pendingPingId != pingId) return;`（:47）。
        val hb = scheduler()
        hb.start()
        ticker.runFirst()
        hb.onPong("ping_9999")
        assertEquals("ping_2000", hb.currentPendingPingId)
        assertEquals(listOf(10_000L, 25_000L), ticker.pendingDelays)
    }

    @Test
    fun aSingleMissDoesNotTriggerReconnectButReschedulesTheLoop() {
        val hb = scheduler()
        hb.start()
        ticker.runFirst() // ping 1
        ticker.runFirst() // 超时 1 ⇒ miss=1 < 2 ⇒ 不重连
        assertEquals(0, timeouts)
        assertEquals(1, hb.currentMiss)
        assertNull(hb.currentPendingPingId)
        // 超时分支里重新排了 interval（Dart :77），并且**旧的 interval 被取消**：
        // 所以任何时刻最多只有一个 interval 与至多一个 timeout 计时在跑。
        assertEquals(listOf(25_000L, 10_000L, 25_000L, 25_000L), ticker.createdDelays)
        assertEquals(listOf(25_000L), ticker.pendingDelays)
    }

    @Test
    fun secondConsecutiveMissNotifiesTimeoutOnceAndStopsRescheduling() {
        // Dart :72-75：达到 maxMiss ⇒ onTimeout() 后 return，**不再排下一次**。
        val hb = scheduler()
        hb.start()
        ticker.runFirst() // ping 1
        ticker.runFirst() // miss 1 ⇒ 重新排程
        ticker.runFirst() // interval ⇒ ping 2
        ticker.runFirst() // miss 2 ⇒ 触发重连
        assertEquals(1, timeouts)
        assertEquals(2, pings.size)
        // 致命超时那一步没有新增任何计时器（剩下的只是上一拍 ping 已经排好的 interval，
        // 生产里它会被 handleTransportLost ⇒ heartbeat.stop() 一并取消）。
        assertEquals(listOf(25_000L, 10_000L, 25_000L, 25_000L, 10_000L, 25_000L), ticker.createdDelays)
        assertEquals(listOf(25_000L), ticker.pendingDelays)
    }

    @Test
    fun pongBetweenTwoMissesResetsTheCounterSoTheThirdMissIsNotFatal() {
        val hb = scheduler()
        hb.start()
        ticker.runFirst() // ping 1
        ticker.runFirst() // miss 1
        ticker.runFirst() // ping 2（pending = ping_2000，时钟未推进）
        hb.onPong(hb.currentPendingPingId ?: error("no pending")) // 归零 + 取消超时
        assertEquals(0, hb.currentMiss)
        ticker.runFirst() // 只剩 interval ⇒ ping 3
        ticker.runFirst() // miss 1
        assertEquals(0, timeouts)
    }

    @Test
    fun sendFailureIsSwallowedAndTheTimersKeepRunning() {
        // Dart try/catch 只打日志（:61-65）。
        val hb = scheduler(
            onSendPing = { throw IllegalStateException("ws closed") },
        )
        hb.start()
        ticker.runFirst()
        assertEquals("ping_2000", hb.currentPendingPingId)
        assertEquals(listOf(10_000L, 25_000L), ticker.pendingDelays)
        ticker.runFirst() // 超时仍按 Dart 语义计数
        assertEquals(1, hb.currentMiss)
    }

    @Test
    fun stopCancelsEveryTimerAndResetsCounters() {
        val hb = scheduler()
        hb.start()
        ticker.runFirst()
        ticker.runFirst() // miss=1
        hb.stop()
        assertEquals(0, hb.currentMiss)
        assertNull(hb.currentPendingPingId)
        assertTrue(ticker.pendingDelays.isEmpty())
    }

    @Test
    fun configuredThresholdOfOneMakesTheFirstMissFatal() {
        // maxMiss 可注入：Dart 用常量 2，这里验证「达到即停」的边界而不是常量本身。
        val hb = HeartbeatScheduler(
            ticker = ticker,
            clock = clock,
            maxMiss = 1,
            onSendPing = { pings += it },
            onTimeout = { timeouts++ },
        )
        hb.start()
        ticker.runFirst()
        ticker.runFirst()
        assertEquals(1, timeouts)
        assertEquals(listOf(25_000L, 10_000L, 25_000L), ticker.createdDelays)
        assertEquals(1, ticker.pendingDelays.size)
    }

    @Test
    fun heartbeatIntervalAndTimeoutFollowTheConfiguredConstants() {
        val hb = HeartbeatScheduler(
            ticker = ticker,
            clock = clock,
            intervalMs = 1_000L,
            timeoutMs = 250L,
            onSendPing = { pings += it },
            onTimeout = { timeouts++ },
        )
        hb.start()
        ticker.runFirst()
        assertEquals(listOf(1_000L, 250L, 1_000L), ticker.createdDelays)
    }
}

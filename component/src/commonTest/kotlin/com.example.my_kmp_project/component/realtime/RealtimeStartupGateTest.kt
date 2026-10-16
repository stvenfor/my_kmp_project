package com.example.my_kmp_project.component.realtime

import com.example.my_kmp_project.core.network.realtime.RealtimeTopics
import com.example.my_kmp_project.core.network.realtime.RealtimeWsSyncApi
import com.example.my_kmp_project.core.network.realtime.RealtimeWsTicketApi
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

/**
 * 启动闸门 —— 真值 Dart `components/realtime/lib/realtime_initializer.dart:19-180`（d7876a1）。
 *
 * 这里锁的是**「什么时候才允许建连」与「首连失败后还要不要往下走」**：
 * 1. 隐私未同意 ⇒ 直接不连，且**先于**登录检查（:103-106）；
 * 2. 未登录 ⇒ 不连（:107-110）；
 * 3. 已经活跃 ⇒ 不重复连（:117-122）；
 * 4. 首连有 12s 上限、连完再等 8s 看结果，两个超时**只打日志**、不打断流程，
 *    更不阻塞启动（:57 注释：「不得阻塞 runApp，真机会一直停在原生启动图」）；
 * 5. 无论连没连上，固定订阅 `sys.notify` + `presence.bulk` 都要下发（:130）；
 * 6. 登出走 `clearQueue=false` 的 disconnect（:166-168），切环境用**默认** clearQueue（:170-173）。
 *
 * 超时分支用 [ImmediateRealtimeTicker]（登记即触发）来代表「期限已过」，
 * 因此既不会真的等 12s/8s，也不会像 [ManualRealtimeTicker] 那样把 `await()` 挂死。
 */
class RealtimeStartupGateTest {

    private val holder = RealtimeTestScope()
    private val ticker = ManualRealtimeTicker()
    private val session = FakeSessionPort()
    private val client = FakeRealtimeClient()
    private var privacyGranted = false
    private val logs = mutableListOf<String>()

    @AfterTest
    fun tearDown() {
        holder.cancel()
    }

    private fun gate(
        target: AppRealtimeClient = client,
        ticker: RealtimeTicker = this.ticker,
        defaultTopics: List<String> = listOf(RealtimeTopics.SYS_NOTIFY, RealtimeTopics.PRESENCE_BULK),
    ): RealtimeStartupGate = RealtimeStartupGate(
        client = target,
        session = session,
        scope = holder.scope,
        privacyGranted = { privacyGranted },
        ticker = ticker,
        defaultTopics = defaultTopics,
        log = { logs += it },
    )

    /** 只给「`client as? AppRealtimeClientImpl`」那一条分支用：闸门要把生命周期转交给具体实现。 */
    private fun realImpl(): AppRealtimeClientImpl = AppRealtimeClientImpl(
        ticketApi = RealtimeWsTicketApi(RecordingRawApiClient()),
        syncApi = RealtimeWsSyncApi(RecordingRawApiClient()),
        outboundQueue = RealtimeOutboundQueue(InMemoryRealtimeOutboundStore(), SteppingRealtimeClock()),
        seqStore = RealtimeSeqStore(InMemoryRealtimeKeyValueStore()),
        router = RealtimeInboundRouter(),
        notifyHandler = RecordingNotifyHandler(),
        telemetry = RealtimeTelemetry(RecordingTelemetrySink()),
        session = session,
        scope = holder.scope,
        ticker = ManualRealtimeTicker(),
        clock = SteppingRealtimeClock(),
        transportFactory = { FakeRealtimeWsTransport() },
    )

    private fun logContaining(fragment: String): List<String> = logs.filter { it.contains(fragment) }

    @Test
    fun privacyNotGrantedBlocksConnectEvenWhenLoggedIn(): Unit = runBlocking {
        privacyGranted = false
        session.loggedIn = true

        gate().tryConnectIfReady(trigger = "app_startup")

        assertEquals(0, client.connectCalls)
        assertTrue(client.subscribeCalls.isEmpty())
        assertTrue(logContaining("skip connect: privacy not granted (trigger=app_startup)").isNotEmpty())
        // 检查顺序本身就是契约：登录态那一行**不该**被打印。
        assertTrue(logContaining("skip connect: not logged in").isEmpty())
    }

    @Test
    fun notLoggedInBlocksConnectAfterPrivacy(): Unit = runBlocking {
        privacyGranted = true
        session.loggedIn = false

        gate().tryConnectIfReady(trigger = "after_login")

        assertEquals(0, client.connectCalls)
        assertTrue(logContaining("skip connect: not logged in (trigger=after_login)").isNotEmpty())
    }

    @Test
    fun alreadyActiveStateIsNotReconnected(): Unit = runBlocking {
        privacyGranted = true
        client.state = RealtimeConnectionState.Connected

        gate().tryConnectIfReady()

        assertEquals(0, client.connectCalls)
        assertTrue(logContaining("skip connect: already 已连接 (trigger=manual)").isNotEmpty())
    }

    @Test
    fun degradedAlsoCountsAsActiveAndIsNotReconnected(): Unit = runBlocking {
        privacyGranted = true
        client.state = RealtimeConnectionState.Degraded

        gate().tryConnectIfReady()

        assertEquals(0, client.connectCalls)
        assertTrue(logContaining("already 降级").isNotEmpty())
    }

    @Test
    fun happyPathConnectsThenSubscribesTheFixedTopicPair(): Unit = runBlocking {
        privacyGranted = true

        gate().tryConnectIfReady(trigger = "app_startup")

        assertEquals(1, client.connectCalls)
        assertEquals(listOf(listOf(RealtimeTopics.SYS_NOTIFY, RealtimeTopics.PRESENCE_BULK)), client.subscribeCalls)
        // 只花掉 12s 那一拍：连上之后 `_waitUntilConnected` 直接短路（:148）。
        assertEquals(listOf(12_000L), ticker.createdDelays)
        assertTrue(ticker.pendingDelays.isEmpty(), ticker.pendingDelays.toString())
        assertEquals(
            listOf(
                "[Realtime] connecting... trigger=app_startup mock=false",
                "[Realtime] connect result trigger=app_startup success=true state=已连接 " +
                    "lastSeq=7 topics=sys.notify,presence.bulk",
            ),
            logContaining("trigger=app_startup").filter {
                it.startsWith("[Realtime] connecting") || it.startsWith("[Realtime] connect result")
            },
        )
        // 首连横幅里的状态字面量与 Dart 的中文 label 一致。
        assertTrue(logs.any { it.contains("state=未连接") })
    }

    @Test
    fun customDefaultTopicsAreForwardedVerbatim(): Unit = runBlocking {
        privacyGranted = true

        gate(defaultTopics = listOf(RealtimeTopics.liveSignal("r7"))).tryConnectIfReady()

        assertEquals(listOf(listOf("live.signal.r7")), client.subscribeCalls)
    }

    @Test
    fun bothTimeoutsOnlyLogAndTheSubscribeStillGoesOut(): Unit = runBlocking {
        privacyGranted = true
        // connectSucceeds=false ⇒ 停在「连接中」，12s 与 8s 两个期限都会走到。
        client.connectSucceeds = false
        val immediate = ImmediateRealtimeTicker(holder.scope)

        gate(ticker = immediate).tryConnectIfReady(trigger = "privacy_granted")

        assertEquals(listOf(12_000L, 8_000L), immediate.createdDelays)
        assertEquals(1, client.connectCalls)
        // 关键：超时**不**回滚后续动作，订阅照发（Dart :130 在 timeout 之后）。
        assertEquals(1, client.subscribeCalls.size)
        assertTrue(logContaining("[Realtime] connect timed out trigger=privacy_granted").isNotEmpty())
        assertTrue(logContaining("connect not ready within timeout trigger=privacy_granted").isNotEmpty())
        assertTrue(logContaining("success=false").isNotEmpty())
    }

    @Test
    fun privacyAndLoginChainsCarryTheirOwnTrigger(): Unit = runBlocking {
        privacyGranted = true
        val g = gate()

        g.onPrivacyGranted()
        // 上一轮已经连上了，这里回到未连接才能再次触发（Dart 的链路就是每次都重新判）。
        client.state = RealtimeConnectionState.Disconnected
        g.onAfterLogin()

        assertEquals(2, client.connectCalls)
        assertTrue(logContaining("trigger=privacy_granted").isNotEmpty())
        assertTrue(logContaining("trigger=after_login").isNotEmpty())
    }

    @Test
    fun logoutDisconnectsWithoutClearingTheOutboundQueue(): Unit = runBlocking {
        gate().onAfterLogout()

        // Dart onLogout()（:166-168）：清连接、不清离线队列。
        val expected: List<Pair<String?, Boolean>> = listOf("logout" to false)
        assertEquals(expected, client.disconnectCalls)
    }

    @Test
    fun envChangedDisconnectsWithDefaultClearQueueThenReconnects(): Unit = runBlocking {
        privacyGranted = true

        gate().onEnvChanged()

        // Dart `_onEnvChanged`（:170-173）用的是 disconnect() 的**默认** clearQueue=false。
        val expected: List<Pair<String?, Boolean>> = listOf("env_changed" to false)
        assertEquals(expected, client.disconnectCalls)
        assertEquals(1, client.connectCalls)
        assertTrue(logContaining("trigger=env_changed").isNotEmpty())
    }

    @Test
    fun lifecycleResumedFallsBackToConnectWhenTheClientIsNotTheConcreteImpl(): Unit = runBlocking {
        val g = gate()
        assertFalse(g.hasLifecycleCapableClient)

        g.onAppLifecycle(RealtimeAppLifecycle.Resumed)

        // 非具体实现走 `?: run { … }` 兜底：只有 resumed 且不活跃才补一次 connect。
        assertEquals(1, client.connectCalls)

        g.onAppLifecycle(RealtimeAppLifecycle.Paused)
        g.onAppLifecycle(RealtimeAppLifecycle.Other)
        assertEquals(1, client.connectCalls)
    }

    @Test
    fun prepareIsForwardedAndCachesTheConcreteImplHandle(): Unit = runBlocking {
        val impl = realImpl()
        val g = gate(target = impl)

        g.prepare()

        assertTrue(g.hasLifecycleCapableClient)
        assertEquals(RealtimeConnectionState.Disconnected, impl.currentState)

        g.dispose()

        assertFalse(g.hasLifecycleCapableClient)
    }

    @Test
    fun gateDrivesTheRealImplThroughTicketExchangeAndAuth(): Unit = runBlocking {
        privacyGranted = true
        val transport = FakeRealtimeWsTransport()
        val immediate = ImmediateRealtimeTicker(holder.scope)
        val impl = AppRealtimeClientImpl(
            ticketApi = RealtimeWsTicketApi(RecordingRawApiClient(listOf(TICKET_OK_BODY))),
            syncApi = RealtimeWsSyncApi(RecordingRawApiClient(listOf(EMPTY_SYNC_BODY))),
            outboundQueue = RealtimeOutboundQueue(InMemoryRealtimeOutboundStore(), SteppingRealtimeClock()),
            seqStore = RealtimeSeqStore(InMemoryRealtimeKeyValueStore()),
            router = RealtimeInboundRouter(),
            notifyHandler = RecordingNotifyHandler(),
            telemetry = RealtimeTelemetry(RecordingTelemetrySink()),
            session = session,
            scope = holder.scope,
            ticker = immediate,
            clock = SteppingRealtimeClock(9_000L),
            transportFactory = { transport },
            networkMonitor = RecordingNetworkMonitor(),
            reconnectPolicy = ReconnectPolicy(jitterRatio = 0.0),
            newFrameId = { "f-1" },
        )

        // 真实现只走到 Authenticating（auth_ok 要等服务器回帧），闸门照旧把订阅发出去。
        gate(target = impl, ticker = immediate).tryConnectIfReady(trigger = "after_login")

        assertEquals(RealtimeConnectionState.Authenticating, impl.currentState)
        assertEquals(listOf("auth"), transport.sentTypes())
        assertEquals("tk-1", transport.sent.single().payloadString("ticket"))
        // 状态没活跃 ⇒ 8s 那一拍也要走完（两个期限都只打日志）。
        assertEquals(listOf(12_000L, 8_000L), immediate.createdDelays)
        assertTrue(logContaining("connect not ready within timeout trigger=after_login").isNotEmpty())
    }
}

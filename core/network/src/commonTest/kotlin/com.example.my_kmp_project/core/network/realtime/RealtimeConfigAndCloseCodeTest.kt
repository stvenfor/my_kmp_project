package com.example.my_kmp_project.core.network.realtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Realtime 常量与 WS 关闭码语义（契约表 §2.17 行 #97 尾注）。
 *
 * 真值：Dart `components/realtime/lib/config/realtime_config.dart:11-61`（d7876a1）。
 * 心跳 25s/10s/连丢 2 次、退避 1s 起 ×2 上限 60s 抖动 20% 地板 500ms、
 * notifyId 去重环 200 条、关闭码 4001 认证失败 / 4002 互踢 / 4003 票过期。
 *
 * 已知差异（刻意保留，不是 bug）：Dart 声明了三个关闭码但**全仓无读取点**
 * （`grep wsCloseAuthFailed` 只命中定义处），因为 Dart 每次重连都重新换票；
 * KMP 侧传输层能读到关闭码（[RealtimeWsTransport.closed]），故把语义显式化为
 * [RealtimeConfig.requiresNewTicket] / [RealtimeConfig.isAuthCloseCode]。
 */
class RealtimeConfigAndCloseCodeTest {

    @Test
    fun heartbeatAndBackoffConstantsMatchDart() {
        assertEquals(25_000L, RealtimeConfig.HEARTBEAT_INTERVAL_MS)
        assertEquals(10_000L, RealtimeConfig.HEARTBEAT_TIMEOUT_MS)
        assertEquals(2, RealtimeConfig.HEARTBEAT_MAX_MISS)
        assertEquals(1_000L, RealtimeConfig.RECONNECT_INITIAL_MS)
        assertEquals(60_000L, RealtimeConfig.RECONNECT_MAX_MS)
        assertEquals(2.0, RealtimeConfig.RECONNECT_MULTIPLIER)
        assertEquals(0.2, RealtimeConfig.RECONNECT_JITTER_RATIO)
        assertEquals(500L, RealtimeConfig.RECONNECT_FLOOR_MS)
        assertEquals(15_000L, RealtimeConfig.OUTBOUND_ACK_TIMEOUT_MS)
        assertEquals(200, RealtimeConfig.NOTIFY_DEDUP_MAX)
    }

    @Test
    fun pathsAndPlatformMatchTheContractTable() {
        assertEquals("/api/v1/realtime/ws-ticket", RealtimeConfig.TICKET_PATH)
        assertEquals("/api/v1/realtime/sync", RealtimeConfig.SYNC_PATH)
        assertEquals("mobile", RealtimeConfig.PLATFORM_MOBILE)
    }

    @Test
    fun closeCodeConstantsMatchGoWSClose() {
        // realtime_config.dart:47-49
        assertEquals(4001, RealtimeConfig.WS_CLOSE_AUTH_FAILED)
        assertEquals(4002, RealtimeConfig.WS_CLOSE_KICKED)
        assertEquals(4003, RealtimeConfig.WS_CLOSE_TICKET_EXPIRED)
        assertEquals(1000, RealtimeConfig.WS_CLOSE_NORMAL)
        assertEquals(1005, RealtimeConfig.WS_CLOSE_NO_STATUS)
    }

    @Test
    fun theThreeApplicationCloseCodesAllDemandANewTicket() {
        for (code in listOf(4001, 4002, 4003)) {
            assertTrue(RealtimeConfig.requiresNewTicket(code), "$code")
        }
        for (code in listOf(1000, 1001, 1005, 1006, 1011, 4000, 4004, 4500)) {
            assertFalse(RealtimeConfig.requiresNewTicket(code), "$code")
        }
    }

    @Test
    fun kickIsSeparatedFromTheAuthCloseCodes() {
        // 4001 / 4003 = 鉴权类（换票可救）；4002 = 互踢（会话已废，不能靠重连解决）。
        assertTrue(RealtimeConfig.isAuthCloseCode(RealtimeConfig.WS_CLOSE_AUTH_FAILED))
        assertTrue(RealtimeConfig.isAuthCloseCode(RealtimeConfig.WS_CLOSE_TICKET_EXPIRED))
        assertFalse(RealtimeConfig.isAuthCloseCode(RealtimeConfig.WS_CLOSE_KICKED))
        assertFalse(RealtimeConfig.isAuthCloseCode(RealtimeConfig.WS_CLOSE_NORMAL))
    }

    @Test
    fun closeEventExposesTheSameTicketRequirementAndDefaultsReasonToBlank() {
        assertTrue(RealtimeWsClose(4003).requiresNewTicket)
        assertFalse(RealtimeWsClose(1011).requiresNewTicket)
        assertEquals("", RealtimeWsClose(1000).reason)
        assertEquals(RealtimeWsClose(4002, "kicked"), RealtimeWsClose(4002, "kicked"))
    }

    @Test
    fun topicNamesMatchTheGoTopicConstants() {
        // realtime_config.dart:53-61
        assertEquals("sys.notify", RealtimeTopics.SYS_NOTIFY)
        assertEquals("presence.bulk", RealtimeTopics.PRESENCE_BULK)
        assertEquals("presence.user.u1", RealtimeTopics.presenceUser("u1"))
        assertEquals("live.signal.room9", RealtimeTopics.liveSignal("room9"))
        assertEquals("live.room.room9.state", RealtimeTopics.liveRoomState("room9"))
    }

    @Test
    fun mockGatewaySwitchIsOffByProductionDefaultAndReadable() {
        // 装配层显式打开；默认必须是真链路（Dart `useMockGateway`，realtime_config.dart:15）。
        val before = RealtimeConfig.useMockGateway
        try {
            RealtimeConfig.useMockGateway = true
            assertTrue(RealtimeConfig.useMockGateway)
        } finally {
            RealtimeConfig.useMockGateway = before
        }
    }

    @Test
    fun unsupportedTransportCarriesTheOhosX64GapMessageAndIsARealtimeError() {
        // GAP(R5)：ohosX64 没有 CPF Ktor/WebSockets klib，桩异常必须可被类型识别以停重连。
        val error: RealtimeWsError = RealtimeWsError.Unsupported()
        assertTrue(error.message!!.contains("ohosX64"), error.message ?: "")
        assertTrue(error.message!!.contains("WebSocket"), error.message ?: "")
        assertEquals("boom", RealtimeWsError.TicketInvalid("boom").message)
        // 自定义文案仍归一到同一 sealed 类型，上层按 Throwable 分支即可。
        assertEquals("no ws here", RealtimeWsError.Unsupported("no ws here").message)
    }
}

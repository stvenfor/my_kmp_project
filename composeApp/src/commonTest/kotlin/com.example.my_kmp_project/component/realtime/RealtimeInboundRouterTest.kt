package com.example.my_kmp_project.component.realtime

import com.example.my_kmp_project.core.network.realtime.RealtimeEnvelope
import com.example.my_kmp_project.core.network.realtime.RealtimeTopics
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch

/**
 * 入站路由 —— 真值 Dart `components/realtime/lib/router/inbound_router.dart:6-45`（d7876a1）。
 *
 * 三条容易踩坑的 Dart 语义：
 * 1. **topic 频道是懒注册的**：没先调过 [RealtimeInboundRouter.watchTopic] 建键，
 *    `dispatch` 就不往那个 topic 投（:23 `containsKey(topic)`）⇒ 未登记的 topic 静默丢弃；
 * 2. `type != 'event'` 的帧**不进** event 频道（:26）⇒ pong / ack / error 不污染业务流；
 * 3. [RealtimeInboundRouter.watchEvents] 的 `eventName` 过滤发生在**下游**（:16-18 `where`），
 *    不是按名字各建一条频道。
 *
 * 多播实现差异是刻意的：Dart 用 broadcast StreamController（无监听者 ⇒ `add` 丢弃、不阻塞），
 * KMP 用 `MutableSharedFlow(replay = 0, DROP_OLDEST)`（无订阅者 ⇒ `tryEmit` 同样丢弃）。
 * 因此每个用例都**先挂订阅再 dispatch**，与生产入站循环的顺序一致。
 */
class RealtimeInboundRouterTest {

    private val holder = RealtimeTestScope()
    private val router = RealtimeInboundRouter()

    @AfterTest
    fun tearDown() {
        holder.cancel()
    }

    /** 在 Unconfined 作用域上挂收集器；`tryEmit` 会就地投递，故返回时列表已含该帧。 */
    private fun collectInto(flow: Flow<RealtimeEnvelope>): MutableList<RealtimeEnvelope> {
        val seen = mutableListOf<RealtimeEnvelope>()
        holder.scope.launch { flow.toList(seen) }
        return seen
    }

    @Test
    fun watchTopicRegistersTheKeyAndLaterFramesOfThatTopicArrive() {
        val seen = collectInto(router.watchTopic(RealtimeTopics.SYS_NOTIFY))
        router.dispatch(eventEnvelope(seq = 1L, topic = RealtimeTopics.SYS_NOTIFY, name = "sys.notify.show"))
        assertEquals(1, seen.size)
        assertEquals("sys.notify.show", seen.single().eventName)
    }

    @Test
    fun unregisteredTopicIsSilentlyDroppedLikeDartContainsKey() {
        // 只登记了 A 的键 ⇒ 投给 B 的帧没人收得到（Dart inbound_router.dart:23）。
        val watched = collectInto(router.watchTopic("presence.user.u1"))
        router.dispatch(eventEnvelope(seq = 2L, topic = "live.signal.r9"))
        assertTrue(watched.isEmpty())
    }

    @Test
    fun otherTopicsDoNotLeakIntoAWatchedTopic() {
        val seen = collectInto(router.watchTopic(RealtimeTopics.PRESENCE_BULK))
        router.dispatch(eventEnvelope(seq = 3L, topic = RealtimeTopics.SYS_NOTIFY))
        router.dispatch(eventEnvelope(seq = 4L, topic = RealtimeTopics.PRESENCE_BULK))
        assertEquals(listOf(4L), seen.map { it.seq })
    }

    @Test
    fun nullTopicNeverReachesATopicChannelButStillHitsTheEventChannel() {
        val topic = collectInto(router.watchTopic(RealtimeTopics.SYS_NOTIFY))
        val events = collectInto(router.watchEvents())
        router.dispatch(eventEnvelope(seq = 5L, topic = null))
        assertTrue(topic.isEmpty())
        assertEquals(1, events.size)
    }

    @Test
    fun onlyEventFramesEnterTheEventChannel() {
        // Dart :26 `if (envelope.type == 'event')`。
        val events = collectInto(router.watchEvents())
        for (type in listOf("ping", "pong", "auth", "auth_ok", "ack", "error", "sub", "unsub")) {
            router.dispatch(controlEnvelope(type = type, id = "f-$type"))
        }
        assertTrue(events.isEmpty())
        router.dispatch(eventEnvelope(seq = 6L))
        assertEquals(1, events.size)
    }

    @Test
    fun unknownTypeFramesAreNotRoutedAnywhere() {
        // sync 裸 body 里非 object 的元素会解码成 type=unknown（Dart 是直接跳过 item），
        // 这里锁定它在 KMP 侧同样不会污染任何业务频道。
        val events = collectInto(router.watchEvents())
        val topic = collectInto(router.watchTopic(RealtimeTopics.SYS_NOTIFY))
        router.dispatch(requireNotNull(RealtimeEnvelope.fromRawJson("""{"payload":{}}""")))
        assertTrue(events.isEmpty())
        assertTrue(topic.isEmpty())
    }

    @Test
    fun watchEventsWithANameFiltersDownstreamNotByBuildingChannels() {
        // Dart :16-18 `where((e) => e.eventName == eventName)`。
        val shows = collectInto(router.watchEvents(eventName = "sys.notify.show"))
        router.dispatch(eventEnvelope(seq = 7L, name = "other.thing"))
        router.dispatch(eventEnvelope(seq = 8L, name = "sys.notify.show"))
        assertEquals(listOf(8L), shows.map { it.seq })
    }

    @Test
    fun nameFilterIgnoresFramesWhoseNameIsNotAString() {
        val wanted = collectInto(router.watchEvents(eventName = "wanted"))
        router.dispatch(eventEnvelope(seq = 9L, name = "other"))
        router.dispatch(
            requireNotNull(
                RealtimeEnvelope.fromRawJson("""{"type":"event","seq":10,"payload":{"name":404}}"""),
            ),
        )
        assertTrue(wanted.isEmpty())
    }

    @Test
    fun severalWatchersOnTheSameTopicAllReceiveTheFrame() {
        val a = collectInto(router.watchTopic("live.signal.r1"))
        val b = collectInto(router.watchTopic("live.signal.r1"))
        router.dispatch(eventEnvelope(seq = 11L, topic = "live.signal.r1"))
        assertEquals(1, a.size)
        assertEquals(1, b.size)
    }

    @Test
    fun disposeStopsTopicDeliveryAndReRegistrationIsPossibleAgain() {
        // Dart dispose 会 close 掉 controller；KMP 清空登记表 ⇒ 已交出去的 Flow 变静默。
        val seen = collectInto(router.watchTopic(RealtimeTopics.SYS_NOTIFY))
        router.dispatch(eventEnvelope(seq = 12L, topic = RealtimeTopics.SYS_NOTIFY))
        assertEquals(1, seen.size)
        router.dispose()
        router.dispatch(eventEnvelope(seq = 13L, topic = RealtimeTopics.SYS_NOTIFY))
        assertEquals(1, seen.size)
    }

    @Test
    fun dispatchIsSynchronousAndNeverBlocksTheInboundLoop() {
        // Dart `controller.add` 不挂起；KMP `tryEmit` + DROP_OLDEST 同理。
        val seen = collectInto(router.watchTopic(RealtimeTopics.SYS_NOTIFY))
        repeat(200) { i ->
            router.dispatch(eventEnvelope(seq = i.toLong() + 1L, topic = RealtimeTopics.SYS_NOTIFY))
        }
        assertTrue(seen.isNotEmpty())
        assertEquals(200L, seen.last().seq)
    }

    @Test
    fun watchEventsWithoutAFilterReusesTheSingleEventChannel() {
        assertSame(router.watchEvents(), router.watchEvents())
    }

    @Test
    fun watchTopicReusesTheChannelPerTopic() {
        assertSame(router.watchTopic("t"), router.watchTopic("t"))
        assertTrue(router.watchTopic("t") !== router.watchTopic("t2"))
    }

    @Test
    fun filteredWatcherIsStillAnOrdinaryFlowYouCanCompose() {
        val seen = collectInto(
            router.watchEvents(eventName = "live.seat_changed").filter { (it.seq ?: 0L) > 5L },
        )
        router.dispatch(eventEnvelope(seq = 1L, name = "live.seat_changed"))
        router.dispatch(eventEnvelope(seq = 9L, name = "live.seat_changed"))
        assertEquals(listOf(9L), seen.map { it.seq })
    }

    @Test
    fun emptyTopicStringIsALegalKeyLikeDartMap() {
        val seen = collectInto(router.watchTopic(""))
        router.dispatch(eventEnvelope(seq = 1L, topic = ""))
        assertEquals(1, seen.size)
    }

    @Test
    fun dispatchWithoutAnySubscriberDoesNotThrow() {
        // 无订阅者 ⇒ 丢弃（等价 Dart broadcast controller），且不会把入站循环带崩。
        router.dispatch(eventEnvelope(seq = 1L, topic = "nobody.is.watching"))
        val late = collectInto(router.watchTopic("nobody.is.watching"))
        assertTrue(late.isEmpty())
        router.dispatch(eventEnvelope(seq = 2L, topic = "nobody.is.watching"))
        assertEquals(1, late.size)
    }
}

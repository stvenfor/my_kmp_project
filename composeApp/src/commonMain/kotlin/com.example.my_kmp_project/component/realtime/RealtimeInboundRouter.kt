package com.example.my_kmp_project.component.realtime

import com.example.my_kmp_project.core.network.realtime.RealtimeEnvelope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter

/**
 * Topic / event 分发 —— 逐行对齐 Dart
 * `components/realtime/lib/router/inbound_router.dart:6-45`（d7876a1）。
 *
 * 保留的 Dart 语义（容易踩坑的三条）：
 * 1. **topic 频道是懒注册的**：只有先调过 [watchTopic] 建过键，[dispatch] 才会往那个 topic 投
 *    （:23 `containsKey(topic)` 判断）；没登记过的 topic 静默丢弃。
 * 2. `type != "event"` 的帧**不进** event 频道（:26），控制帧（pong / ack / error）不会污染业务流。
 * 3. [watchEvents] 的 `eventName` 过滤发生在**下游**（:16-18 `where`），
 *    与 Dart 一样是「每条 event 都过一遍谓词」，而不是按名字建频道。
 *
 * 多播实现差异（刻意）：Dart 用 broadcast `StreamController`，无监听者时 `add` 直接丢弃、
 * 有监听者时各自无界缓冲。这里用 `MutableSharedFlow(replay = 0)` ——
 * 无订阅者同样丢弃（等价）；订阅者极慢时按 `DROP_OLDEST` 丢最旧，
 * 因为组件层不能反过来阻塞入站循环（Dart 的 `add` 同样不阻塞）。
 *
 * 线程约定与 Dart 单 isolate 一致：[watchTopic] 注册与 [dispatch] 派发生在同一装配作用域，
 * 所以 topic 登记表用普通 [MutableMap]（不引锁、不引 atomicfu）。
 */
public class RealtimeInboundRouter {

    private val topicFlows: MutableMap<String, MutableSharedFlow<RealtimeEnvelope>> = mutableMapOf()
    private val eventFlow: MutableSharedFlow<RealtimeEnvelope> = newChannel()

    /** Dart `watchTopic(topic)`（:10-12）：首次调用即把该 topic 登记进表。 */
    public fun watchTopic(topic: String): Flow<RealtimeEnvelope> = topicController(topic)

    /** Dart `watchEvents({String? eventName})`（:14-19）：`null` 表示不过滤。 */
    public fun watchEvents(eventName: String? = null): Flow<RealtimeEnvelope> =
        if (eventName == null) eventFlow else eventFlow.filter { it.eventName == eventName }

    /**
     * Dart `dispatch(envelope)`（:21-29）：**同步**投递（`add` 不挂起），
     * 所以调用方（入站循环）不会被某个慢订阅者卡住。
     */
    public fun dispatch(envelope: RealtimeEnvelope) {
        val topic = envelope.topic
        if (topic != null && topicFlows.containsKey(topic)) {
            topicController(topic).tryEmit(envelope)
        }
        if (envelope.type == TYPE_EVENT) {
            eventFlow.tryEmit(envelope)
        }
    }

    /**
     * Dart `dispose()`（:38-44）：关闭频道并清空登记表。
     * 这里是**终态**：登记被清空后 [dispatch] 不再投递，已交给调用方的 [Flow] 变成静默流。
     */
    public fun dispose() {
        topicFlows.clear()
    }

    /** Dart `_topicController(topic)` 的 `putIfAbsent`（:31-36）。 */
    private fun topicController(topic: String): MutableSharedFlow<RealtimeEnvelope> =
        topicFlows.getOrPut(topic) { newChannel() }

    private fun newChannel(): MutableSharedFlow<RealtimeEnvelope> = MutableSharedFlow(
        replay = 0,
        extraBufferCapacity = CHANNEL_BUFFER,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private companion object {
        /** Dart `envelope.type == 'event'`（:26）。 */
        const val TYPE_EVENT: String = "event"

        /** 每条频道的背压缓冲深度：只兜「订阅者短暂落后」，不参与协议语义。 */
        const val CHANNEL_BUFFER: Int = 64
    }
}

package com.example.my_kmp_project.component.realtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 离线出站队列 —— 真值 Dart `components/realtime/lib/queue/outbound_queue_manager.dart:7-97`（d7876a1），
 * 表语义对齐 `commons/storage/lib/db/app_database.dart:101-155`（`ws_outbound_queue`）。
 *
 * 最反直觉、也最该锁死的是 [RealtimeOutboundQueue.depth] 的三处口径
 * （Dart 就是这么写的，别「顺手修正」）：
 * - `enqueue` ⇒ **+1**（:31）
 * - `markAcked` ⇒ **-1 后 clamp(0, 9999)**（:41）
 * - `pendingItems()` / `refreshDepth()` ⇒ 用真实行数**覆盖**（:49-51、:60-61）
 * - `markSent` / `markFailed` ⇒ **不动 depth**（:36、:45）
 * 另外 `pending` 视图只含 `pending` + `sent`（:136-143），所以 `failed` 行不再重发但仍留在表里。
 */
class RealtimeOutboundQueueTest {

    private val clock = SteppingRealtimeClock(1_000L)
    private var ids = 0
    private lateinit var store: RecordingOutboundStore
    private lateinit var queue: RealtimeOutboundQueue

    private fun newQueue(): RealtimeOutboundQueue {
        store = RecordingOutboundStore()
        ids = 0
        queue = RealtimeOutboundQueue(
            store = store,
            clock = clock,
            newMessageId = { "m${++ids}" },
        )
        return queue
    }

    @Test
    fun enqueuePersistsAPendingRowAndRaisesDepthBeforeReturningTheId() {
        val queue = newQueue()
        val id = queue.enqueue(topic = "presence.bulk", eventName = "presence.report", payloadJson = """{"a":1}""")
        assertEquals("m1", id)
        assertEquals(1, queue.depth)
        val row = store.pending().single()
        assertEquals(id, row.messageId)
        assertEquals("presence.bulk", row.topic)
        assertEquals("presence.report", row.eventName)
        assertEquals("""{"a":1}""", row.payloadJson)
        assertEquals(RealtimeOutboundStatus.PENDING, row.status)
        // Dart 用 DateTime.now()：created_at 与 updated_at 同刻。
        assertEquals(1_000L, row.createdAt)
        assertEquals(1_000L, row.updatedAt)
    }

    @Test
    fun depthCountsEveryEnqueuedMessage() {
        val queue = newQueue()
        repeat(5) { queue.enqueue("t", "e", "{}") }
        assertEquals(5, queue.depth)
        assertEquals(5, store.pending().size)
    }

    @Test
    fun markSentOnlyChangesTheStatusAndLeavesDepthAlone() {
        // Dart markSent（:35-37）压根不碰 _depth。
        val queue = newQueue()
        val id = queue.enqueue("t", "e", "{}")
        clock.advance(50L)
        queue.markSent(id)
        assertEquals(1, queue.depth)
        assertEquals(RealtimeOutboundStatus.SENT, store.pending().single().status)
        assertEquals(1_050L, store.pending().single().updatedAt)
        assertEquals(listOf(id to RealtimeOutboundStatus.SENT), store.statusLog)
    }

    @Test
    fun markAckedDeletesTheRowAndDecrementsDepth() {
        val queue = newQueue()
        val a = queue.enqueue("t", "e", "{}")
        val b = queue.enqueue("t", "e", "{}")
        assertEquals(2, queue.depth)
        queue.markAcked(a)
        assertEquals(1, queue.depth)
        assertEquals(listOf(b), store.pending().map { it.messageId })
    }

    @Test
    fun markAckedNeverGoesBelowZeroLikeDartClamp() {
        // Dart `(depth - 1).clamp(0, 9999)`（:41）。
        val queue = newQueue()
        queue.markAcked("never-existed")
        assertEquals(0, queue.depth)
        val id = queue.enqueue("t", "e", "{}")
        queue.markAcked(id)
        queue.markAcked(id)
        queue.markAcked(id)
        assertEquals(0, queue.depth)
    }

    @Test
    fun markFailedKeepsTheRowButRemovesItFromTheResendView() {
        // failed 不在 Dart 的 `status IN ('pending','sent')` 里 ⇒ 不再重发。
        val queue = newQueue()
        val id = queue.enqueue("t", "e", "{}")
        queue.markFailed(id)
        assertEquals(1, queue.depth)
        assertTrue(store.pending().isEmpty())
        assertEquals(listOf(id to RealtimeOutboundStatus.FAILED), store.statusLog)
    }

    @Test
    fun pendingItemsOverwritesDepthWithTheRealRowCount() {
        // Dart pendingItems（:48-52）：读行 + `_depth = rows.length`。
        val queue = newQueue()
        val a = queue.enqueue("t", "e", "{}")
        queue.enqueue("t", "e", "{}")
        queue.markFailed(a) // 行还在表里但不在 pending 视图
        assertEquals(2, queue.depth)
        val items = queue.pendingItems()
        assertEquals(1, items.size)
        assertEquals(1, queue.depth)
        assertEquals("m2", items.single().messageId)
    }

    @Test
    fun pendingItemsAreOrderedByCreatedAtAscending() {
        // Dart `ORDER BY created_at ASC`（app_database.dart:136-143）。
        val queue = newQueue()
        queue.enqueue("t", "first", "{}")
        clock.advance(10L)
        queue.enqueue("t", "second", "{}")
        clock.advance(10L)
        queue.enqueue("t", "third", "{}")
        assertEquals(listOf("first", "second", "third"), queue.pendingItems().map { it.eventName })
    }

    @Test
    fun refreshDepthSyncsWithTheTableWithoutReturningAnything() {
        val queue = newQueue()
        queue.enqueue("t", "e", "{}")
        queue.enqueue("t", "e", "{}")
        // 模拟进程重启：depth 归零但行还在。
        val restarted = RealtimeOutboundQueue(
            store = store,
            clock = clock,
            newMessageId = { "x" },
        )
        assertEquals(0, restarted.depth)
        restarted.refreshDepth()
        assertEquals(2, restarted.depth)
    }

    @Test
    fun clearWipesTheTableAndZeroesDepth() {
        val queue = newQueue()
        queue.enqueue("t", "e", "{}")
        queue.enqueue("t", "e", "{}")
        queue.clear()
        assertEquals(0, queue.depth)
        assertTrue(store.pending().isEmpty())
    }

    @Test
    fun enqueueWithTheSameMessageIdReplacesTheRowLikeSqliteConflictReplace() {
        // Dart `ConflictAlgorithm.replace`（app_database.dart:120）；这里用 store 直接验证。
        val queue = newQueue()
        queue.enqueue("t", "e", "{}")
        store.upsert(
            RealtimeOutboundRow(
                messageId = "m1",
                topic = "t2",
                eventName = "e2",
                payloadJson = """{"z":2}""",
                status = RealtimeOutboundStatus.PENDING,
                createdAt = 9_999L,
                updatedAt = 9_999L,
            ),
        )
        val item = queue.pendingItems().single()
        assertEquals("e2", item.eventName)
        assertEquals(9_999L, store.pending().single().createdAt)
        assertEquals(1, store.pending().size)
    }

    @Test
    fun decodePayloadReturnsAnEmptyObjectForGarbageInsteadOfThrowing() {
        // Dart `try { jsonDecode } catch (_) { return {} }`（:80-86）。
        val queue = newQueue()
        queue.enqueue("t", "e", "not-json")
        queue.enqueue("t", "e2", """{"ok":true}""")
        val items = queue.pendingItems()
        assertEquals(0, items.first { it.eventName == "e" }.decodePayload().size)
        assertEquals(true, items.first { it.eventName == "e2" }.decodePayload()["ok"]?.toString()?.toBoolean())
    }

    @Test
    fun decodePayloadTreatsAJsonArrayAsEmptyObject() {
        // Dart `jsonDecode(payload) as Map<String, dynamic>` ⇒ 数组会抛 ⇒ 落 {}。
        val queue = newQueue()
        queue.enqueue("t", "e", "[1,2,3]")
        assertTrue(queue.pendingItems().single().decodePayload().isEmpty())
    }

    @Test
    fun statusLiteralsMatchTheSqlTheDartSideWrites() {
        assertEquals("pending", RealtimeOutboundStatus.PENDING)
        assertEquals("sent", RealtimeOutboundStatus.SENT)
        assertEquals("failed", RealtimeOutboundStatus.FAILED)
    }

    @Test
    fun itemFromRowCarriesEveryColumnWithoutTheTimestamps() {
        val row = RealtimeOutboundRow("m", "t", "e", "{}", RealtimeOutboundStatus.SENT, 1L, 2L)
        val item = RealtimeOutboundItem.fromRow(row)
        assertEquals("m", item.messageId)
        assertEquals("t", item.topic)
        assertEquals("e", item.eventName)
        assertEquals("{}", item.payloadJson)
        assertEquals(RealtimeOutboundStatus.SENT, item.status)
    }

    @Test
    fun inMemoryStoreIgnoresStatusUpdatesForUnknownIds() {
        val store = InMemoryRealtimeOutboundStore()
        val queue = RealtimeOutboundQueue(store = store, clock = clock, newMessageId = { "m1" })
        queue.enqueue("t", "e", "{}")
        queue.markSent("ghost")
        assertEquals(RealtimeOutboundStatus.PENDING, store.pending().single().status)
        queue.markAcked("ghost")
        assertEquals(0, queue.depth)
        assertEquals(1, store.pending().size)
    }
}

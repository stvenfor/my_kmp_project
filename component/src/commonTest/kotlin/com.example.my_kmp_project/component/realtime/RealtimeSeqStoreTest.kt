package com.example.my_kmp_project.component.realtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * seq 水位 + notifyId 去重环 —— 真值 Dart
 * `components/realtime/lib/store/realtime_seq_store.dart:9-71`（d7876a1），
 * 键名对齐 `commons/storage/lib/prefs/sp_keys.dart:4-5`。
 *
 * 锁定的都是「顺手修好就破坏等价性」的点：
 * - `load()` 脏数据归 `0`（Dart `int.tryParse(raw ?? '') ?? 0`，:16）；
 * - `acceptSeq(null)` **恒接受**（:27）—— 控制帧与无 seq 的业务帧不参与排序；
 * - 只有**严格大于**水位才接受，等号算重复（:28 `seq <= _lastSeq ⇒ false`）；
 * - 接受即写盘（Dart `unawaited(save())`，:30）；
 * - `setLastSeq` 单调不回退、等号不写（:34-39）；
 * - 去重环空 id 恒放行、命中拒绝、新 id 插头部、超过 200 从尾部截断（:58-70）。
 */
class RealtimeSeqStoreTest {

    // ── RealtimeSeqStore ───────────────────────────────────────────────────────

    @Test
    fun prefKeysMatchTheDartSpKeysVerbatim() {
        assertEquals("realtime_last_seq", RealtimePrefKeys.LAST_SEQ)
        assertEquals("realtime_notify_dedup_json", RealtimePrefKeys.NOTIFY_DEDUP)
    }

    @Test
    fun freshStoreStartsAtZeroAndAcceptsTheFirstSeqAndPersistsIt() {
        val kv = InMemoryRealtimeKeyValueStore()
        val store = RealtimeSeqStore(kv)
        assertEquals(0L, store.lastSeq)
        assertTrue(store.acceptSeq(7L))
        assertEquals(7L, store.lastSeq)
        // Dart 接受即 save（unawaited），这里同步写 ⇒ 立即可读。
        assertEquals("7", kv.getString(RealtimePrefKeys.LAST_SEQ))
    }

    @Test
    fun loadTolerantlyMapsDirtyAndMissingValuesToZero() {
        // Dart `int.tryParse(raw ?? '') ?? 0`。
        for (dirty in listOf(null, "", "abc", "1.5", " 12 ")) {
            // null 这一档代表「键根本不存在」；其余是脏值。
            val initial: Map<String, String> =
                if (dirty == null) emptyMap() else mapOf(RealtimePrefKeys.LAST_SEQ to dirty)
            val store = RealtimeSeqStore(InMemoryRealtimeKeyValueStore(initial))
            store.load()
            assertEquals(0L, store.lastSeq, "raw=$dirty")
        }
    }

    @Test
    fun loadReadsThePersistedWatermark() {
        val store = RealtimeSeqStore(InMemoryRealtimeKeyValueStore(mapOf(RealtimePrefKeys.LAST_SEQ to "41")))
        store.load()
        assertEquals(41L, store.lastSeq)
    }

    @Test
    fun acceptSeqRejectsEqualAndOlderButAcceptsNull() {
        val store = RealtimeSeqStore(InMemoryRealtimeKeyValueStore(mapOf(RealtimePrefKeys.LAST_SEQ to "10")))
        store.load()
        assertFalse(store.acceptSeq(10L)) // 等号 = 重复
        assertFalse(store.acceptSeq(9L))
        assertTrue(store.acceptSeq(null)) // 无 seq 帧放行且不抬水位
        assertEquals(10L, store.lastSeq)
        assertTrue(store.acceptSeq(11L))
        assertEquals(11L, store.lastSeq)
    }

    @Test
    fun rejectedSeqDoesNotRewriteTheStore() {
        val kv = InMemoryRealtimeKeyValueStore()
        val store = RealtimeSeqStore(kv)
        store.acceptSeq(5L)
        val after = kv.getString(RealtimePrefKeys.LAST_SEQ)
        assertFalse(store.acceptSeq(1L))
        assertFalse(store.acceptSeq(5L))
        assertEquals(after, kv.getString(RealtimePrefKeys.LAST_SEQ))
    }

    @Test
    fun setLastSeqOnlyRaisesAndSkipsEqual() {
        val kv = InMemoryRealtimeKeyValueStore()
        val store = RealtimeSeqStore(kv)
        store.setLastSeq(20L)
        assertEquals(20L, store.lastSeq)
        store.setLastSeq(20L) // 等号：Dart `if (seq > _lastSeq)` ⇒ 不写
        assertEquals(20L, store.lastSeq)
        store.setLastSeq(5L) // 不回退
        assertEquals(20L, store.lastSeq)
        assertEquals("20", kv.getString(RealtimePrefKeys.LAST_SEQ))
        store.setLastSeq(99L)
        assertEquals("99", kv.getString(RealtimePrefKeys.LAST_SEQ))
    }

    @Test
    fun saveWritesTheCurrentWatermarkExplicitly() {
        val kv = InMemoryRealtimeKeyValueStore()
        val store = RealtimeSeqStore(kv)
        store.setLastSeq(3L)
        store.save()
        assertEquals("3", kv.getString(RealtimePrefKeys.LAST_SEQ))
        assertNull(kv.getString(RealtimePrefKeys.NOTIFY_DEDUP))
    }

    @Test
    fun watermarkSurvivesANewStoreInstanceOverTheSameKv() {
        // 断线重启后 sync 起点必须接得上。
        val kv = InMemoryRealtimeKeyValueStore()
        val first = RealtimeSeqStore(kv)
        first.acceptSeq(64L)
        val second = RealtimeSeqStore(kv)
        second.load()
        assertEquals(64L, second.lastSeq)
        assertFalse(second.acceptSeq(64L))
    }

    // ── NotifyDedupStore ───────────────────────────────────────────────────────

    @Test
    fun emptyNotifyIdIsAlwaysProcessedAndNeverRecorded() {
        // Dart `if (notifyId.isEmpty) return true`（:59）。
        val kv = InMemoryRealtimeKeyValueStore()
        val dedup = NotifyDedupStore(kv)
        assertTrue(dedup.shouldProcess(""))
        assertTrue(dedup.shouldProcess(""))
        assertTrue(dedup.snapshot.isEmpty())
        assertNull(kv.getString(RealtimePrefKeys.NOTIFY_DEDUP))
    }

    @Test
    fun firstSightingPassesAndRepeatsAreRejected() {
        val dedup = NotifyDedupStore(InMemoryRealtimeKeyValueStore())
        assertTrue(dedup.shouldProcess("n1"))
        assertFalse(dedup.shouldProcess("n1"))
        assertTrue(dedup.shouldProcess("n2"))
        // 新 id 插到**头部**（Dart `_ids.insert(0, …)`，:61）。
        assertEquals(listOf("n2", "n1"), dedup.snapshot)
    }

    @Test
    fun processedIdsArePersistedAsAJsonArrayAndReloaded() {
        val kv = InMemoryRealtimeKeyValueStore()
        val dedup = NotifyDedupStore(kv)
        dedup.shouldProcess("n1")
        dedup.shouldProcess("n2")
        val raw = assertNotNull(kv.getString(RealtimePrefKeys.NOTIFY_DEDUP))
        assertTrue(raw.startsWith("["), raw)
        assertTrue(raw.contains("n2"), raw)

        val reloaded = NotifyDedupStore(kv)
        reloaded.load()
        assertFalse(reloaded.shouldProcess("n1"))
        assertFalse(reloaded.shouldProcess("n2"))
    }

    @Test
    fun corruptStoredJsonResetsTheRingInsteadOfThrowing() {
        // Dart try/catch ⇒ `_ids.clear()`（:53-55）。
        // 注意顺序：`shouldProcess` 会顺手把整环写回 kv（:67），所以必须在 load 之前不写。
        val kv = InMemoryRealtimeKeyValueStore(mapOf(RealtimePrefKeys.NOTIFY_DEDUP to "not-json"))
        val dedup = NotifyDedupStore(kv)
        dedup.load()
        assertTrue(dedup.snapshot.isEmpty())

        // 环清空后老 id 重新算「没见过」⇒ 会再展示一次（Dart 的容错语义，不抛）。
        assertTrue(dedup.shouldProcess("n1"))
        val reloaded = NotifyDedupStore(kv)
        reloaded.load()
        assertEquals(listOf("n1"), reloaded.snapshot)
        assertFalse(reloaded.shouldProcess("n1"))
    }

    @Test
    fun nonArrayStoredJsonAlsoResetsTheRing() {
        val kv = InMemoryRealtimeKeyValueStore(mapOf(RealtimePrefKeys.NOTIFY_DEDUP to """{"a":1}"""))
        val dedup = NotifyDedupStore(kv)
        dedup.load()
        assertTrue(dedup.snapshot.isEmpty())
    }

    @Test
    fun blankStoredValueKeepsTheInMemoryRingLikeDartEarlyReturn() {
        // Dart `if (raw == null || raw.isEmpty) return;` ⇒ 空串**不清空**内存态。
        val kv = InMemoryRealtimeKeyValueStore(mapOf(RealtimePrefKeys.NOTIFY_DEDUP to ""))
        val dedup = NotifyDedupStore(kv)
        dedup.shouldProcess("a")
        dedup.load()
        assertEquals(listOf("a"), dedup.snapshot)
    }

    @Test
    fun ringTrimsOldestEntriesOnceItExceedsTwoHundred() {
        // Dart `removeRange(notifyDedupMax, length)`（:62-64）：头部是最新，尾部被截。
        val kv = InMemoryRealtimeKeyValueStore()
        val dedup = NotifyDedupStore(kv)
        repeat(250) { i -> dedup.shouldProcess("n$i") }
        assertEquals(200, dedup.snapshot.size)
        assertEquals("n249", dedup.snapshot.first())
        assertEquals("n50", dedup.snapshot.last())
        // 被截掉的老 id 重新出现时会再次放行（与 Dart 一致：去重环不是无限历史）。
        assertTrue(dedup.shouldProcess("n0"))
        // 仍在环内的最新 id 依然被拒。
        assertFalse(dedup.shouldProcess("n249"))
        assertEquals(200, dedup.snapshot.size)
    }

    @Test
    fun trimmedRingIsWhatGetsPersistedNotTheFullHistory() {
        val kv = InMemoryRealtimeKeyValueStore()
        val dedup = NotifyDedupStore(kv)
        repeat(201) { i -> dedup.shouldProcess("n$i") }
        assertEquals(200, dedup.snapshot.size)
        val persisted = kv.getString(RealtimePrefKeys.NOTIFY_DEDUP)!!
        assertTrue(!persisted.contains("n0"), persisted)
        val reloaded = NotifyDedupStore(kv)
        reloaded.load()
        assertEquals(200, reloaded.snapshot.size)
    }

    @Test
    fun numericIdsReloadWithoutBeingStringifiedWrongly() {
        // Dart `list.map((e) => e.toString())`：数字元素也收进环。
        val kv = InMemoryRealtimeKeyValueStore(mapOf(RealtimePrefKeys.NOTIFY_DEDUP to """["n1",7,true]"""))
        val dedup = NotifyDedupStore(kv)
        dedup.load()
        assertEquals(listOf("n1", "7", "true"), dedup.snapshot)
    }

}

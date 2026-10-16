package com.example.my_kmp_project.component.realtime

import com.example.my_kmp_project.core.network.realtime.emptyRealtimeObject
import com.example.my_kmp_project.core.network.realtime.realtimeJsonObjectOrNull
import com.example.my_kmp_project.core.uuid.AppUuid
import kotlinx.serialization.json.JsonObject

/**
 * 离线出站队列 —— 逐行对齐 Dart
 * `components/realtime/lib/queue/outbound_queue_manager.dart:7-97`（d7876a1），
 * 持久化语义对齐 `commons/storage/lib/db/app_database.dart:101-155`（表 `ws_outbound_queue`）。
 *
 * 本仓库 commonMain 没有 SQLDelite/Room，故把「表」抽成 [RealtimeOutboundStore] 缝隙，
 * 由装配层接平台 DB；[InMemoryRealtimeOutboundStore] 复刻 Dart 用到的四条 SQL 语义，
 * 因此队列逻辑本身（含 `_depth` 的三处赋值）可脱离真实 DB 单测。
 *
 * 刻意保留的行为：
 * - `enqueue` 先落库（status=`pending`）再返回 messageId ⇒ 断线期消息不丢（Dart client:428-433）；
 * - [depth] 是**自增计数器**，不是实时查询：`enqueue +1`、`markAcked -1（夹到 0..9999）`、
 *   `pendingItems()` / `refreshDepth()` 用行数**覆盖**、`clear()` 归零；
 * - `markSent` / `markFailed` **不动 depth**（它们的行仍在 pending 视图里）。
 */

/** 行状态字面量：与 Dart SQL 里的 `'pending' / 'sent' / 'failed'` 一致，禁止改名。 */
public object RealtimeOutboundStatus {
    public const val PENDING: String = "pending"
    public const val SENT: String = "sent"
    public const val FAILED: String = "failed"
}

/** `ws_outbound_queue` 的一行（列名对照 app_database.dart:88-97）。 */
public data class RealtimeOutboundRow(
    val messageId: String,
    val topic: String,
    val eventName: String,
    val payloadJson: String,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long,
)

/**
 * 出站队列表的缝隙。实现需满足（Dart 侧全部由 SQLite 保证）：
 * - [upsert]：按 `message_id` 冲突替换（`ConflictAlgorithm.replace`，:120）
 * - [pending]：`status IN ('pending','sent') ORDER BY created_at ASC`（:136-143）
 * - [updateStatus] 同时刷新 `updated_at`（:124-134）
 */
public interface RealtimeOutboundStore {
    public fun upsert(row: RealtimeOutboundRow)

    public fun updateStatus(messageId: String, status: String, nowMs: Long)

    public fun delete(messageId: String)

    public fun pending(): List<RealtimeOutboundRow>

    public fun clear()
}

/** 进程内实现：顺序 = 插入顺序（SQLite 未写 ORDER BY 次级键时的同款行为）。 */
public class InMemoryRealtimeOutboundStore : RealtimeOutboundStore {
    private val rows: MutableList<RealtimeOutboundRow> = mutableListOf()

    override fun upsert(row: RealtimeOutboundRow) {
        val idx = rows.indexOfFirst { it.messageId == row.messageId }
        if (idx >= 0) rows[idx] = row else rows.add(row)
    }

    override fun updateStatus(messageId: String, status: String, nowMs: Long) {
        val idx = rows.indexOfFirst { it.messageId == messageId }
        if (idx < 0) return
        rows[idx] = rows[idx].copy(status = status, updatedAt = nowMs)
    }

    override fun delete(messageId: String) {
        rows.removeAll { it.messageId == messageId }
    }

    override fun pending(): List<RealtimeOutboundRow> =
        rows.filter { it.status == RealtimeOutboundStatus.PENDING || it.status == RealtimeOutboundStatus.SENT }
            .sortedBy { it.createdAt }

    override fun clear() {
        rows.clear()
    }
}

/** Dart `OutboundQueueItem`（:65-97）：`decodePayload()` 解析失败回空对象（:80-86）。 */
public class RealtimeOutboundItem(
    public val messageId: String,
    public val topic: String,
    public val eventName: String,
    public val payloadJson: String,
    public val status: String,
) {
    /** Dart `OutboundQueueItem.fromRow`（:88-96）。 */
    public companion object {
        public fun fromRow(row: RealtimeOutboundRow): RealtimeOutboundItem = RealtimeOutboundItem(
            messageId = row.messageId,
            topic = row.topic,
            eventName = row.eventName,
            payloadJson = row.payloadJson,
            status = row.status,
        )
    }

    /** Dart `decodePayload()`：`jsonDecode` 失败 ⇒ `{}`。 */
    public fun decodePayload(): JsonObject =
        realtimeJsonObjectOrNull(payloadJson) ?: emptyRealtimeObject
}

/**
 * 出站队列管理器（Dart `OutboundQueueManager`）。
 *
 * `nowMs` / `messageId` 都可注入，保证单测确定性：
 * Dart 用 `DateTime.now().millisecondsSinceEpoch`（:108）与 `Uuid().v4()`（:23）。
 */
public class RealtimeOutboundQueue(
    private val store: RealtimeOutboundStore,
    private val clock: RealtimeClock = SystemRealtimeClock,
    private val newMessageId: () -> String = { AppUuid.random() },
) {
    private var _depth: Int = 0

    /** Dart `int get depth`（:16）。 */
    public val depth: Int get() = _depth

    /**
     * Dart `enqueue({topic, eventName, payload})`（:18-33）：
     * 落库 status=`pending` + `created_at = updated_at = now`，深度 +1，返回 messageId。
     */
    public fun enqueue(
        topic: String,
        eventName: String,
        payloadJson: String,
    ): String {
        val messageId = newMessageId()
        val now = clock.nowMs()
        store.upsert(
            RealtimeOutboundRow(
                messageId = messageId,
                topic = topic,
                eventName = eventName,
                payloadJson = payloadJson,
                status = RealtimeOutboundStatus.PENDING,
                createdAt = now,
                updatedAt = now,
            ),
        )
        _depth++
        return messageId
    }

    /** Dart `markSent`（:35-37）：只改状态，不改 depth。 */
    public fun markSent(messageId: String) {
        store.updateStatus(messageId, RealtimeOutboundStatus.SENT, clock.nowMs())
    }

    /** Dart `markAcked`（:39-42）：删行 + depth 减一，夹到 `0..9999`。 */
    public fun markAcked(messageId: String) {
        store.delete(messageId)
        _depth = (_depth - 1).coerceIn(0, DEPTH_CLAMP_MAX)
    }

    /** Dart `markFailed`（:44-46）：只改状态，行留在表里但**不再重发**（不在 pending 视图）。 */
    public fun markFailed(messageId: String) {
        store.updateStatus(messageId, RealtimeOutboundStatus.FAILED, clock.nowMs())
    }

    /** Dart `pendingItems()`（:48-52）：读行的同时**覆盖** depth。 */
    public fun pendingItems(): List<RealtimeOutboundItem> {
        val rows = store.pending()
        _depth = rows.size
        return rows.map { RealtimeOutboundItem.fromRow(it) }
    }

    /** Dart `clear()`（:54-57）。 */
    public fun clear() {
        store.clear()
        _depth = 0
    }

    /** Dart `refreshDepth()`（:59-62）：`prepare()` 时同步一次真实行数。 */
    public fun refreshDepth() {
        _depth = store.pending().size
    }

    private companion object {
        /** Dart `(depth - 1).clamp(0, 9999)`（:41）里的上界，原样保留。 */
        const val DEPTH_CLAMP_MAX: Int = 9999
    }
}

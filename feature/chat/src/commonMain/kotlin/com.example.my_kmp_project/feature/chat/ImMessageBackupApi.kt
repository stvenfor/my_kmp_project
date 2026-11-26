package com.example.my_kmp_project.feature.chat

import com.example.my_kmp_project.core.network.ApiClient
import com.example.my_kmp_project.core.network.NetworkFacade
import com.example.my_kmp_project.core.network.NetworkJson
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put

/**
 * 契约表 §2.18 行 #102 —— Dart `components/rongcloud_im/lib/backup/http_im_backup_service.dart`
 * 的单条备份事件（`_pending.add({...})`，:19-27 / :40-48 / :59-67）。
 *
 * 线上字段固定 7 个，顺序与 Dart map 字面量一致：
 * `message_uid`、`direction`(`in`/`out`/`recall`)、`conversation_type`(恒 `private`)、
 * `target_id`（会话 storageId）、`message_type`、`payload`、`sent_at`（**UTC ISO-8601**）。
 * Dart 入参 `imUserId` **不进请求体**（`HttpImBackupService` 收下即忽略），
 * 本 DTO 因而也没有它，只在 [ImBackupService] 方法签名上保留同名参数。
 */
public data class ImMessageBackupEvent(
    public val messageUid: String,
    public val direction: ImBackupDirection,
    public val conversationId: String,
    public val messageType: String,
    public val payload: JsonObject,
    public val sentAtEpochMs: Long,
    public val conversationType: String = "private",
) {

    /** Dart `sentAt.toUtc().toIso8601String()`（:26/:47/:66）。 */
    public val sentAtIso8601Utc: String get() = ImIso8601Utc.format(sentAtEpochMs)

    /** 事件 → `events[]` 的单条 JSON（字段名严格按 Dart 侧）。 */
    public fun toJson(): JsonObject = JsonObject(
        linkedMapOf<String, JsonElement>(
            "message_uid" to JsonPrimitive(messageUid),
            "direction" to JsonPrimitive(direction.wireValue),
            "conversation_type" to JsonPrimitive(conversationType),
            "target_id" to JsonPrimitive(conversationId),
            "message_type" to JsonPrimitive(messageType),
            "payload" to payload,
            "sent_at" to JsonPrimitive(sentAtIso8601Utc),
        ),
    )

    public companion object {
        /** 反向解析（服务端回读 / 测试断言用）；缺失键按 Dart 的 `?? ''` 兜底。 */
        public fun fromJson(json: JsonObject): ImMessageBackupEvent = ImMessageBackupEvent(
            messageUid = json.imJsonStr("message_uid"),
            direction = ImBackupDirection.fromWire(json.imJsonStr("direction")),
            conversationId = json.imJsonStr("target_id"),
            messageType = json.imJsonStr("message_type"),
            payload = (json["payload"] as? JsonObject) ?: buildJsonObject { },
            sentAtEpochMs = ImIso8601Utc.parseToEpochMillis(
                json.imJsonStr("sent_at"),
            ) ?: 0L,
            conversationType = json.imJsonStr("conversation_type").ifEmpty { "private" },
        )
    }
}

/** Dart 的三种 `direction` 字面量（http_im_backup_service.dart:21/:42/:62）。 */
public enum class ImBackupDirection(public val wireValue: String) {
    /** `backupOutbound` —— 本端发出。 */
    Out("out"),

    /** `backupInbound` —— 对端发来。 */
    In("in"),

    /** `backupRecall` —— 撤回（`message_type` 固定 `recall`）。 */
    Recall("recall"),
    ;

    public companion object {
        public fun fromWire(value: String): ImBackupDirection =
            entries.firstOrNull { it.wireValue == value } ?: Out
    }
}

/**
 * Dart `commons/core/lib/service/im_backup_service.dart:1-29` 的接口对等。
 *
 * // GAP(R5): 事件的生产者是融云 SDK 回调 —— Dart 在
 * // `features/chat/lib/chat/repository/im_chat_repository.dart` 里
 * // 发送成功后 `backupOutbound`（:220-227）、收到消息时 `backupInbound`（:413-420）、
 * // 撤回时 `backupRecall`（:300-306）；SDK 监听挂载点是
 * // `components/rongcloud_im/lib/engine/rong_engine_holder.dart:92 attachMessageListener`
 * // （底层 `RCIMIWEngine`，插件 `rongcloud_im_wrapper_plugin: ^5.44.0`）。
 * // 本工程无该产物，收发尚未接入（无本地 mock 引擎）；
 * // 本 lane 只提供「拿到事件之后」的入队 + HTTP 上报，不得伪称 SDK ready。
 */
public interface ImBackupService {
    /** Dart `backupOutbound`（http_im_backup_service.dart:11-29）。 */
    public suspend fun backupOutbound(
        imUserId: String,
        conversationId: String,
        messageUid: String,
        type: String,
        payload: JsonObject,
        sentAtEpochMs: Long,
    )

    /** Dart `backupInbound`（http_im_backup_service.dart:32-50）。 */
    public suspend fun backupInbound(
        imUserId: String,
        conversationId: String,
        messageUid: String,
        type: String,
        payload: JsonObject,
        sentAtEpochMs: Long,
    )

    /** Dart `backupRecall`（http_im_backup_service.dart:53-69）：`message_type` = `recall`，`payload` = `{}`。 */
    public suspend fun backupRecall(
        imUserId: String,
        conversationId: String,
        messageUid: String,
        recalledAtEpochMs: Long,
    )

    /** Dart `flushPending`（http_im_backup_service.dart:72-95）。 */
    public suspend fun flushPending()

    /** Dart `int get pendingCount`（http_im_backup_service.dart:97）。 */
    public val pendingCount: Int
}

/**
 * Dart `HttpImBackupService`（http_im_backup_service.dart:7-98）——
 * 真实 BFF 上报 `POST /api/v1/im/messages/backup`，body `{events:[…]}`。
 *
 * 反直觉语义必须原样保留（契约表 #102、§5.3）：
 * - **攒够 ≥5 条才 flush**（:28/:49/:68），入队本身不发网络；
 * - flush 先整批出队再发送；**失败（`code != 0`）或抛异常**时
 *   `insertAll(0, batch)` 把整批**插回队头**（:87 / :93），顺序不变；
 * - 因此是 **at-least-once**，服务端按 `message_uid` 去重 —— 客户端不做去重、不丢批。
 *
 * [imUserId] 参数按 Dart 一样被忽略（不进 body）。
 */
public class HttpImBackupService(
    private val client: ApiClient = NetworkFacade.api(),
) : ImBackupService {

    private val pending: MutableList<ImMessageBackupEvent> = mutableListOf()
    private val queueMutex = Mutex()

    override suspend fun backupOutbound(
        imUserId: String,
        conversationId: String,
        messageUid: String,
        type: String,
        payload: JsonObject,
        sentAtEpochMs: Long,
    ) = enqueue(
        ImMessageBackupEvent(
            messageUid = messageUid,
            direction = ImBackupDirection.Out,
            conversationId = conversationId,
            messageType = type,
            payload = payload,
            sentAtEpochMs = sentAtEpochMs,
        ),
    )

    override suspend fun backupInbound(
        imUserId: String,
        conversationId: String,
        messageUid: String,
        type: String,
        payload: JsonObject,
        sentAtEpochMs: Long,
    ) = enqueue(
        ImMessageBackupEvent(
            messageUid = messageUid,
            direction = ImBackupDirection.In,
            conversationId = conversationId,
            messageType = type,
            payload = payload,
            sentAtEpochMs = sentAtEpochMs,
        ),
    )

    override suspend fun backupRecall(
        imUserId: String,
        conversationId: String,
        messageUid: String,
        recalledAtEpochMs: Long,
    ) = enqueue(
        // Dart :59-67：direction=recall、message_type=recall、payload 为空对象
        ImMessageBackupEvent(
            messageUid = messageUid,
            direction = ImBackupDirection.Recall,
            conversationId = conversationId,
            messageType = RECALL_MESSAGE_TYPE,
            payload = buildJsonObject { },
            sentAtEpochMs = recalledAtEpochMs,
        ),
    )

    /**
     * Dart 的 `_pending.add(...)` 之后立刻 `if (_pending.length >= 5) await flushPending()`
     * （:28/:49/:68）—— 判断与入队同处一步，flush 在其后。
     */
    private suspend fun enqueue(event: ImMessageBackupEvent) {
        val shouldFlush = queueMutex.withLock {
            pending.add(event)
            pending.size >= FLUSH_THRESHOLD
        }
        if (shouldFlush) flushPending()
    }

    override suspend fun flushPending() {
        val batch: List<ImMessageBackupEvent> = queueMutex.withLock {
            if (pending.isEmpty()) {
                emptyList()
            } else {
                val snapshot = pending.toList()
                pending.clear()
                snapshot
            }
        }
        if (batch.isEmpty()) return

        val body = NetworkJson.encodeToString(
            JsonObject.serializer(),
            JsonObject(mapOf("events" to JsonArray(batch.map { it.toJson() }))),
        )
        val outcome = runCatching {
            client.postApi(RongImConfig.MESSAGES_BACKUP_PATH, body) { Unit }
        }
        val succeeded = outcome.getOrNull()?.isSuccess == true
        if (!succeeded) {
            // Dart :85-93：整批回头重排（insertAll(0, batch)），at-least-once
            queueMutex.withLock { pending.addAll(0, batch) }
        }
    }

    override val pendingCount: Int get() = pending.size

    private companion object {
        /** Dart `if (_pending.length >= 5)`（:28/:49/:68）。 */
        const val FLUSH_THRESHOLD: Int = 5

        /** Dart `'recall'`（:64）。 */
        const val RECALL_MESSAGE_TYPE: String = "recall"
    }
}

/**
 * Dart `MockImBackupService`（mock_im_backup_service.dart:7-58）—— mock 分支只排队 + 假发送，
 * 不打 HTTP：出队每条等 30ms（:88）。
 *
 * 与 Dart 一致的**不对称**：`backupRecall` 在 mock 里只记日志、**不入队**（:53-63）。
 */
public class MockImBackupService(
    private val sleep: suspend (Long) -> Unit = { delay(it) },
) : ImBackupService {

    private val pending: MutableList<ImMessageBackupEvent> = mutableListOf()
    private val queueMutex = Mutex()

    override suspend fun backupOutbound(
        imUserId: String,
        conversationId: String,
        messageUid: String,
        type: String,
        payload: JsonObject,
        sentAtEpochMs: Long,
    ) = enqueue(
        ImMessageBackupEvent(
            messageUid = messageUid,
            direction = ImBackupDirection.Out,
            conversationId = conversationId,
            messageType = type,
            payload = payload,
            sentAtEpochMs = sentAtEpochMs,
        ),
    )

    override suspend fun backupInbound(
        imUserId: String,
        conversationId: String,
        messageUid: String,
        type: String,
        payload: JsonObject,
        sentAtEpochMs: Long,
    ) = enqueue(
        ImMessageBackupEvent(
            messageUid = messageUid,
            direction = ImBackupDirection.In,
            conversationId = conversationId,
            messageType = type,
            payload = payload,
            sentAtEpochMs = sentAtEpochMs,
        ),
    )

    /** Dart mock 分支的 recall 不入队（mock_im_backup_service.dart:52-63）。 */
    override suspend fun backupRecall(
        imUserId: String,
        conversationId: String,
        messageUid: String,
        recalledAtEpochMs: Long,
    ) {
        // 仅日志语义，KMP 无 LogUtils 对等实现，故留空
    }

    private suspend fun enqueue(event: ImMessageBackupEvent) {
        val shouldFlush = queueMutex.withLock {
            pending.add(event)
            pending.size >= FLUSH_THRESHOLD
        }
        if (shouldFlush) flushPending()
    }

    override suspend fun flushPending() {
        while (true) {
            val drainedOne = queueMutex.withLock {
                if (pending.isEmpty()) false else {
                    pending.removeAt(0)
                    true
                }
            }
            if (!drainedOne) return
            // Dart 侧每条假发送等 30ms（仅日志/延时，不打 HTTP）
            sleep(MOCK_POST_DELAY_MS)
        }
    }

    override val pendingCount: Int get() = pending.size

    private companion object {
        const val FLUSH_THRESHOLD: Int = 5

        /** Dart `Future.delayed(30ms)`（mock_im_backup_service.dart:88）。 */
        const val MOCK_POST_DELAY_MS: Long = 30L
    }
}

/**
 * Dart `ImBinding.dependencies()` 里备份服务的选型（im_binding.dart:52-60）：
 * mock 开关为真用 [MockImBackupService]，否则 [HttpImBackupService]。
 */
public fun createImBackupService(
    client: ApiClient = NetworkFacade.api(),
    useMock: () -> Boolean = { RongImConfig.useMockIm },
    sleep: suspend (Long) -> Unit = { delay(it) },
): ImBackupService = if (useMock()) MockImBackupService(sleep) else HttpImBackupService(client)

/**
 * 备份 `payload` 构造器 —— 与 Dart `im_chat_repository.dart` 的字面量逐条对齐：
 * 文本 `{'text': text}`（:110）、图片 `{'localPath': …}`（:122）、
 * 语音 `{'localPath': …, 'duration': seconds}`（:139）、自定义 `{'customType': …, ...payload}`（:156）、
 * 入站 `{'content': …}`（:418）。
 */
public object ImBackupPayloads {
    public fun text(text: String): JsonObject = buildJsonObject { put("text", text) }

    public fun image(localPath: String): JsonObject = buildJsonObject { put("localPath", localPath) }

    public fun voice(localPath: String, durationSeconds: Int): JsonObject = buildJsonObject {
        put("localPath", localPath)
        put("duration", durationSeconds)
    }

    public fun custom(customType: String, fields: Map<String, String> = emptyMap()): JsonObject =
        buildJsonObject {
            put("customType", customType)
            fields.forEach { (key, value) -> put(key, value) }
        }

    public fun content(content: String): JsonObject = buildJsonObject { put("content", content) }

    public fun empty(): JsonObject = buildJsonObject { }

    /** 宽松取值：`{"text":"x"}` 里的 `x`；键缺失返回 null。 */
    public fun stringValue(payload: JsonObject, key: String): String? =
        (payload[key] as? JsonPrimitive)?.takeIf { it !is JsonNull }?.contentOrNull
}

/**
 * UTC ISO-8601 格式化（Dart `DateTime.toUtc().toIso8601String()`，http_im_backup_service.dart:26）。
 *
 * 手写而不用 `Instant.toString()`：各端 `kotlin.time` 实现的分秒位数不一致，
 * 而这里要跟 Dart 一样稳定 —— 毫秒为 0 时**不带**小数，否则带 3 位小数，尾缀 `Z`。
 * 日期换算用 Hinnant `civil_from_days`（纯整数，跨平台零漂移）。
 */
public object ImIso8601Utc {
    private const val MILLIS_PER_DAY = 86_400_000L
    private const val MILLIS_PER_SECOND = 1_000L
    private const val MILLIS_PER_MINUTE = 60_000L
    private const val MILLIS_PER_HOUR = 3_600_000L

    /** 1970-01-01 →  civil 的偏移天数（Howard Hinnant days_from_civil 逆变换常量）。 */
    private const val DAYS_TO_EPOCH_SHIFT = 719_468L
    private const val DAYS_PER_ERA = 146_097L

    public fun format(epochMs: Long): String {
        val days = floorDiv(epochMs, MILLIS_PER_DAY)
        val millisOfDay = floorMod(epochMs, MILLIS_PER_DAY)
        val hour = millisOfDay / MILLIS_PER_HOUR
        val minute = (millisOfDay / MILLIS_PER_MINUTE) % 60L
        val second = (millisOfDay / MILLIS_PER_SECOND) % 60L
        val millis = floorMod(epochMs, 1000L)
        val (year, month, day) = civilFromDays(days)

        return buildString(32) {
            append(year.pad(4))
            append('-')
            append(month.pad(2))
            append('-')
            append(day.pad(2))
            append('T')
            append(hour.pad(2))
            append(':')
            append(minute.pad(2))
            append(':')
            append(second.pad(2))
            if (millis != 0L) {
                append('.')
                append(millis.pad(3))
            }
            append('Z')
        }
    }

    /** Howard Hinnant `civil_from_days`：days = 距 1970-01-01 的天数。 */
    private fun civilFromDays(days: Long): Triple<Long, Long, Long> {
        val z = days + DAYS_TO_EPOCH_SHIFT
        val era = floorDiv(z, DAYS_PER_ERA)
        val doe = z - era * DAYS_PER_ERA // [0, 146096]
        val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365 // [0, 399]
        val y = yoe + era * 400
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100) // [0, 365]
        val mp = (5 * doy + 2) / 153 // [0, 11]
        val d = doy - (153 * mp + 2) / 5 + 1 // [1, 31]
        val m = if (mp < 10) mp + 3 else mp - 9 // [1, 12]
        return Triple(if (m <= 2) y + 1 else y, m, d)
    }

    private fun floorDiv(a: Long, b: Long): Long {
        val q = a / b
        if ((a % b != 0L) && ((a < 0L) != (b < 0L))) return q - 1L
        return q
    }

    private fun floorMod(a: Long, b: Long): Long = a - floorDiv(a, b) * b

    private fun Long.pad(width: Int): String = toString().padStart(width, '0')

    /**
     * [format] 的逆变换：解析 `yyyy-MM-ddTHH:mm:ss[.SSS]Z`（也接受带 `+00:00` 的
     * Dart ISO-8601 UTC 形态）为 epoch 毫秒；无法解析返回 null。
     */
    public fun parseToEpochMillis(iso: String): Long? {
        val text = iso.trim()
        if (text.length < 20) return null
        val year = text.substring(0, 4).toLongOrNull() ?: return null
        val month = text.substring(5, 7).toLongOrNull() ?: return null
        val day = text.substring(8, 10).toLongOrNull() ?: return null
        if (text[10] != 'T' && text[10] != ' ') return null
        val hour = text.substring(11, 13).toLongOrNull() ?: return null
        val minute = text.substring(14, 16).toLongOrNull() ?: return null
        val second = text.substring(17, 19).toLongOrNull() ?: return null
        var millis = 0L
        var index = 19
        if (index < text.length && text[index] == '.') {
            val digits = StringBuilder()
            index++
            while (index < text.length && text[index].isDigit()) {
                digits.append(text[index])
                index++
            }
            // Dart 小数位可能是 3 或 6 位，这里按毫秒取前 3 位
            millis = digits.toString().padEnd(3, '0').take(3).toLongOrNull() ?: return null
        }
        val rest = text.substring(index)
        val offsetMinutes = when {
            rest.isEmpty() || rest == "Z" -> 0L
            rest.matches(Regex("[+-]\\d{2}:\\d{2}")) -> {
                val sign = if (rest[0] == '-') -1L else 1L
                val oh = rest.substring(1, 3).toLong()
                val om = rest.substring(4, 6).toLong()
                sign * (oh * 60L + om)
            }
            else -> return null
        }
        val days = daysFromCivil(year, month, day)
        return ((days * MILLIS_PER_DAY) +
            (hour * MILLIS_PER_HOUR) +
            (minute * MILLIS_PER_MINUTE) +
            (second * MILLIS_PER_SECOND) +
            millis - offsetMinutes * MILLIS_PER_MINUTE)
    }

    /** Howard Hinnant `days_from_civil`：[format] 所用算法的正变换。 */
    private fun daysFromCivil(y: Long, m: Long, d: Long): Long {
        val yy = if (m <= 2) y - 1 else y
        val era = floorDiv(yy, 400L)
        val yoe = yy - era * 400L // [0, 399]
        val doy = (153 * (if (m > 2) m - 3 else m + 9) + 2) / 5 + d - 1 // [0, 365]
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy // [0, 146096]
        return era * DAYS_PER_ERA + doe - DAYS_TO_EPOCH_SHIFT
    }
}

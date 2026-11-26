package com.example.my_kmp_project.component.chat

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
private fun nowMs(): Long = Clock.System.now().toEpochMilliseconds()

/**
 * Pluggable IM engine 契约（对应 Flutter `ChatRepository`）。
 *
 * 融云 SDK 尚未接入：本仓库不再提供任何进程内 mock 实现，聊天 UI 统一显示「未接入」。
 * 接口保留给后续 `RongImEngine` 实现，避免届时再改调用方签名。
 */
public interface ImEngine {
    fun conversations(): List<ImConversation>
    fun messages(conversationId: String): List<ImMessage>
    fun sendText(conversationId: String, body: String): ImMessage?
    fun sendImage(conversationId: String, urlOrPath: String): ImMessage?
    fun sendVoice(conversationId: String, label: String = "[语音]"): ImMessage?
    fun sendCustom(conversationId: String, title: String = "名片"): ImMessage?
    fun markConversationRead(conversationId: String)
    fun ensureConversation(
        id: String,
        title: String,
        lastMessage: String = "",
        unreadCount: Int = 0,
    ): String
    fun recallMessage(conversationId: String, messageId: String): Boolean
    fun observe(listener: () -> Unit): () -> Unit
    fun filterConversations(query: String): List<ImConversation>
}

public enum class ImMessageType { Text, Image, Voice, Custom, Time, System }

public enum class ImSendStatus { Sending, Success, Failed }

public enum class ImReadStatus { Unread, Read }

public data class ImConversation(
    val id: String,
    val peerId: String,
    val title: String,
    val lastMessage: String,
    val lastMessageAtMs: Long,
    val unreadCount: Int = 0,
    val isOnline: Boolean = false,
    val portraitUrl: String = "",
) {
    val updatedAtLabel: String get() = ChatTimeFormat.listLabel(lastMessageAtMs)
}

public data class ImMessage(
    val id: String,
    val conversationId: String,
    val senderName: String,
    val body: String,
    val isSelf: Boolean,
    val createdAtMs: Long,
    val type: ImMessageType = ImMessageType.Text,
    val sendStatus: ImSendStatus = ImSendStatus.Success,
    val readStatus: ImReadStatus = ImReadStatus.Read,
    val messageUid: String? = null,
) {
    val timeLabel: String get() = ChatTimeFormat.hm(createdAtMs)

    /** Flutter `MessageModel.canRecall` — self, text-like, within 180s. */
    val canRecall: Boolean
        get() = isSelf &&
            type != ImMessageType.Time &&
            type != ImMessageType.System &&
            (nowMs() - createdAtMs) <= 180_000L

    fun statusLabel(): String {
        if (type == ImMessageType.Time || type == ImMessageType.System) return ""
        if (!isSelf) return ""
        return when (sendStatus) {
            ImSendStatus.Sending -> "发送中"
            ImSendStatus.Failed -> "发送失败"
            ImSendStatus.Success ->
                if (readStatus == ImReadStatus.Read) "已读" else "送达"
        }
    }
}

internal object ChatTimeFormat {
    fun hm(epochMs: Long): String {
        val totalSec = (epochMs / 1000L) + 8 * 3600
        val daySec = ((totalSec % 86400) + 86400) % 86400
        val hour = (daySec / 3600).toInt()
        val minute = ((daySec % 3600) / 60).toInt()
        return "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
    }

    fun listLabel(epochMs: Long): String {
        val now = nowMs()
        val dayMs = 86_400_000L
        val todayStart = now - ((now + 8 * 3600_000L) % dayMs)
        val msgDayStart = epochMs - ((epochMs + 8 * 3600_000L) % dayMs)
        return when {
            msgDayStart == todayStart -> hm(epochMs)
            msgDayStart == todayStart - dayMs -> "昨天"
            else -> {
                val days = (epochMs / dayMs).toInt()
                val month = ((days % 365) / 30).coerceIn(1, 12)
                val day = (days % 30).coerceIn(1, 28)
                "$month/$day"
            }
        }
    }

    fun detailDividerLabel(epochMs: Long): String {
        val now = nowMs()
        val dayMs = 86_400_000L
        val todayStart = now - ((now + 8 * 3600_000L) % dayMs)
        val msgDayStart = epochMs - ((epochMs + 8 * 3600_000L) % dayMs)
        val hm = hm(epochMs)
        return when {
            msgDayStart == todayStart -> hm
            msgDayStart == todayStart - dayMs -> "昨天 $hm"
            else -> {
                val days = (epochMs / dayMs).toInt()
                val month = ((days % 365) / 30).coerceIn(1, 12)
                val day = (days % 30).coerceIn(1, 28)
                "$month/$day $hm"
            }
        }
    }
}

/**
 * Deeplink args for `/chat/detail` (Flutter `_mockConversation` / Get.arguments map).
 */
public data class ChatDetailDeepLinkArgs(
    val id: String,
    val peerName: String,
    val lastMessage: String,
    val unreadCount: Int,
)

/**
 * Parse Flutter-style `/chat/detail?peerName=&peerId=&id=&lastMessage=&unread=` query.
 * Returns null when the URI has no usable conversation args — matches Flutter
 * `ChatModule` route builder showing「缺少会话参数」for null [Get.arguments].
 */
public fun chatDetailArgsFromDeepLink(rawUri: String): ChatDetailDeepLinkArgs? {
    val query = rawUri.substringAfter('?', missingDelimiterValue = "")
        .substringBefore('#')
    if (query.isBlank()) return null
    val map = mutableMapOf<String, String>()
    for (part in query.split('&')) {
        val eq = part.indexOf('=')
        if (eq <= 0) continue
        val key = part.substring(0, eq)
        val value = percentDecode(part.substring(eq + 1))
        map[key] = value
    }
    if (map["peerName"].isNullOrBlank() &&
        map["id"].isNullOrBlank() &&
        map["peerId"].isNullOrBlank()
    ) {
        return null
    }
    val peerId = map["peerId"].orEmpty().ifBlank { map["id"].orEmpty() }
    val id = when {
        map["id"].orEmpty().startsWith("private_") -> map["id"]!!
        peerId.isNotBlank() -> "private_$peerId"
        else -> "private_conv"
    }
    return ChatDetailDeepLinkArgs(
        id = id,
        peerName = map["peerName"].orEmpty().ifBlank { "会话" },
        // Empty lastMessage → empty thread.
        lastMessage = map["lastMessage"].orEmpty(),
        unreadCount = map["unread"]?.toIntOrNull() ?: 0,
    )
}

/** Minimal percent-decode for deeplink query values (UTF-8). */
private fun percentDecode(raw: String): String {
    if (!raw.contains('%') && !raw.contains('+')) return raw
    val sb = StringBuilder(raw.length)
    var i = 0
    val bytes = ArrayList<Byte>()
    fun flushBytes() {
        if (bytes.isEmpty()) return
        sb.append(bytes.toByteArray().decodeToString())
        bytes.clear()
    }
    while (i < raw.length) {
        val c = raw[i]
        when {
            c == '+' -> {
                flushBytes()
                sb.append(' ')
                i++
            }
            c == '%' && i + 2 < raw.length -> {
                val hex = raw.substring(i + 1, i + 3)
                val b = hex.toIntOrNull(16)
                if (b != null) {
                    bytes.add(b.toByte())
                    i += 3
                } else {
                    flushBytes()
                    sb.append(c)
                    i++
                }
            }
            else -> {
                flushBytes()
                sb.append(c)
                i++
            }
        }
    }
    flushBytes()
    return sb.toString()
}

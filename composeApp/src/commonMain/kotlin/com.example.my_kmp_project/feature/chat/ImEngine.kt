package com.example.my_kmp_project.feature.chat

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
private fun nowMs(): Long = Clock.System.now().toEpochMilliseconds()

/**
 * Pluggable IM engine — contract mirrors Flutter `ChatRepository` + `MockImChatStore`.
 */
internal interface ImEngine {
    fun conversations(): List<ImConversation>
    fun messages(conversationId: String): List<ImMessage>
    /** Newest-first insert with Flutter mock send pipeline (sending → 280ms success → 2s peer-read). */
    fun sendText(conversationId: String, body: String): ImMessage?
    fun sendImage(conversationId: String, urlOrPath: String): ImMessage?
    fun sendVoice(conversationId: String, label: String = "[语音]"): ImMessage?
    fun sendCustom(conversationId: String, title: String = "Demo 名片"): ImMessage?
    fun markConversationRead(conversationId: String)
    fun ensureConversation(
        id: String,
        title: String,
        lastMessage: String = "",
        unreadCount: Int = 0,
    ): String
    fun observe(listener: () -> Unit): () -> Unit
    fun filterConversations(query: String): List<ImConversation>
}

internal enum class ImMessageType { Text, Image, Voice, Custom, Time, System }

internal enum class ImSendStatus { Sending, Success, Failed }

internal enum class ImReadStatus { Unread, Read }

internal data class ImConversation(
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

internal data class ImMessage(
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

internal object ChatAvatarUrls {
    const val SELF = "https://picsum.photos/seed/chat_self/150/150"
    fun peer(peerId: String): String = "https://picsum.photos/seed/chat_$peerId/150/150"
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
 * Flutter `MockImChatStore` + mock branch of `ImChatRepository._send`.
 *
 * Default [seedDemo]=true matches Flutter when Rong SDK is not ready (`enableSeed: true`).
 */
internal class MockImEngine(
    seedDemo: Boolean = true,
) : ImEngine {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val conversationState = mutableListOf<ImConversation>()
    /** Newest-first (Flutter store insert(0)). */
    private val messagesByConversation: MutableMap<String, MutableList<ImMessage>> = mutableMapOf()
    private val listeners = mutableListOf<() -> Unit>()
    private var localSeq = 0
    private var seeded = false

    init {
        if (seedDemo) ensureSeed()
    }

    private fun notifyListeners() {
        listeners.toList().forEach { it() }
    }

    override fun observe(listener: () -> Unit): () -> Unit {
        listeners += listener
        return {
            listeners.remove(listener)
        }
    }

    private fun ensureSeed() {
        if (seeded) return
        seeded = true
        val now = nowMs()
        val peers = listOf("mock_peer_01", "mock_peer_02", "mock_peer_03")
        peers.forEachIndexed { i, peer ->
            val storageId = "private_$peer"
            conversationState += ImConversation(
                id = storageId,
                peerId = peer,
                title = "Mock好友${i + 1}",
                lastMessage = if (i == 0) "晚上一起吃饭吗？" else "你好",
                lastMessageAtMs = now - 5L * 60_000L * (i + 1),
                isOnline = i % 2 == 0,
                unreadCount = if (i == 0) 2 else 0,
                portraitUrl = ChatAvatarUrls.peer(peer),
            )
            if (i == 0) {
                // Newest-first — matches sendPipeline insert(0) + LazyColumn reverseLayout.
                messagesByConversation[storageId] = mutableListOf(
                    ImMessage(
                        id = "m_2",
                        conversationId = storageId,
                        senderName = "我",
                        body = "在的，有什么事？",
                        isSelf = true,
                        createdAtMs = now - 28L * 60_000L,
                        messageUid = "uid_m_2",
                        readStatus = ImReadStatus.Read,
                    ),
                    ImMessage(
                        id = "m_1",
                        conversationId = storageId,
                        senderName = "Mock好友1",
                        body = "你好，在吗？",
                        isSelf = false,
                        createdAtMs = now - 30L * 60_000L,
                        messageUid = "uid_m_1",
                        readStatus = ImReadStatus.Read,
                    ),
                )
            }
        }
        sortConversations()
    }

    private fun sortConversations() {
        conversationState.sortByDescending { it.lastMessageAtMs }
    }

    override fun conversations(): List<ImConversation> {
        ensureSeed()
        return conversationState.toList()
    }

    override fun filterConversations(query: String): List<ImConversation> {
        val q = query.trim().lowercase()
        val all = conversations()
        if (q.isEmpty()) return all
        return all.filter {
            it.title.lowercase().contains(q) || it.lastMessage.lowercase().contains(q)
        }
    }

    override fun messages(conversationId: String): List<ImMessage> {
        ensureSeed()
        // Newest-first — Flutter store + MessageListView(reverse: true).
        return messagesByConversation[conversationId].orEmpty().toList()
    }

    override fun markConversationRead(conversationId: String) {
        ensureSeed()
        val list = messagesByConversation[conversationId]
        if (list != null) {
            for (i in list.indices) {
                val m = list[i]
                if (!m.isSelf && m.readStatus == ImReadStatus.Unread) {
                    list[i] = m.copy(readStatus = ImReadStatus.Read)
                }
            }
        }
        val idx = conversationState.indexOfFirst { it.id == conversationId }
        if (idx >= 0 && conversationState[idx].unreadCount != 0) {
            conversationState[idx] = conversationState[idx].copy(unreadCount = 0)
            sortConversations()
        }
        notifyListeners()
    }

    override fun sendText(conversationId: String, body: String): ImMessage? =
        sendPipeline(conversationId, ImMessageType.Text, body.trim()) { it }

    override fun sendImage(conversationId: String, urlOrPath: String): ImMessage? =
        sendPipeline(conversationId, ImMessageType.Image, urlOrPath) { "[图片]" }

    override fun sendVoice(conversationId: String, label: String): ImMessage? =
        sendPipeline(conversationId, ImMessageType.Voice, label) { "[语音]" }

    override fun sendCustom(conversationId: String, title: String): ImMessage? =
        sendPipeline(conversationId, ImMessageType.Custom, title) { "[自定义消息]" }

    private fun sendPipeline(
        conversationId: String,
        type: ImMessageType,
        content: String,
        preview: (String) -> String,
    ): ImMessage? {
        val text = content.trim()
        if (text.isEmpty()) return null
        ensureSeed()
        val now = nowMs()
        maybeInsertTimeDivider(conversationId, now)
        val localId = "local_${++localSeq}_${now}"
        val pending = ImMessage(
            id = localId,
            conversationId = conversationId,
            senderName = "我",
            body = text,
            isSelf = true,
            createdAtMs = now,
            type = type,
            sendStatus = ImSendStatus.Sending,
            readStatus = ImReadStatus.Unread,
        )
        val list = messagesByConversation.getOrPut(conversationId) { mutableListOf() }
        list.add(0, pending)
        upsertPreview(conversationId, preview(text), now, isSelf = true)
        notifyListeners()

        scope.launch {
            delay(280)
            val cur = messagesByConversation[conversationId] ?: return@launch
            val idx = cur.indexOfFirst { it.id == localId }
            if (idx < 0) return@launch
            val saved = cur[idx].copy(
                sendStatus = ImSendStatus.Success,
                messageUid = "uid_$now",
            )
            cur[idx] = saved
            notifyListeners()
            delay(2000)
            simulatePeerRead(conversationId, localId)
        }
        return pending
    }

    private fun maybeInsertTimeDivider(conversationId: String, createdAtMs: Long) {
        val list = messagesByConversation.getOrPut(conversationId) { mutableListOf() }
        val latest = list.firstOrNull()
        if (latest != null && latest.type == ImMessageType.Time) return
        if (latest != null && kotlin.math.abs(createdAtMs - latest.createdAtMs) < 5 * 60_000L) return
        list.add(
            0,
            ImMessage(
                id = "time_$createdAtMs",
                conversationId = conversationId,
                senderName = "",
                body = ChatTimeFormat.detailDividerLabel(createdAtMs),
                isSelf = false,
                createdAtMs = createdAtMs,
                type = ImMessageType.Time,
            ),
        )
    }

    private fun upsertPreview(
        conversationId: String,
        preview: String,
        atMs: Long,
        isSelf: Boolean,
    ) {
        val idx = conversationState.indexOfFirst { it.id == conversationId }
        if (idx >= 0) {
            val old = conversationState[idx]
            conversationState[idx] = old.copy(
                lastMessage = preview,
                lastMessageAtMs = atMs,
                unreadCount = if (isSelf) old.unreadCount else old.unreadCount + 1,
            )
            sortConversations()
        }
    }

    private fun simulatePeerRead(conversationId: String, messageId: String) {
        val list = messagesByConversation[conversationId] ?: return
        val idx = list.indexOfFirst { it.id == messageId && it.isSelf }
        if (idx < 0) return
        list[idx] = list[idx].copy(readStatus = ImReadStatus.Read)
        notifyListeners()
    }

    override fun ensureConversation(
        id: String,
        title: String,
        lastMessage: String,
        unreadCount: Int,
    ): String {
        ensureSeed()
        val storageId = when {
            id.startsWith("private_") || id.startsWith("group_") -> id
            id.isNotBlank() -> "private_$id"
            else -> "private_conv_mock_push"
        }
        val peerId = storageId.removePrefix("private_").removePrefix("group_")
        val existing = conversationState.indexOfFirst { it.id == storageId }
        val now = nowMs()
        if (existing >= 0) {
            val old = conversationState[existing]
            conversationState[existing] = old.copy(
                title = title.ifBlank { old.title },
                lastMessage = lastMessage.ifBlank { old.lastMessage },
                unreadCount = unreadCount.takeIf { it > 0 } ?: old.unreadCount,
                lastMessageAtMs = now,
            )
        } else {
            conversationState.add(
                0,
                ImConversation(
                    id = storageId,
                    peerId = peerId,
                    title = title.ifBlank { "推送会话" },
                    lastMessage = lastMessage.ifBlank { "来自 Push/Deeplink 的 mock 会话" },
                    lastMessageAtMs = now,
                    unreadCount = unreadCount,
                    isOnline = false,
                    portraitUrl = ChatAvatarUrls.peer(peerId),
                ),
            )
            messagesByConversation.getOrPut(storageId) { mutableListOf() }.also { list ->
                if (list.isEmpty() && lastMessage.isNotBlank()) {
                    list += ImMessage(
                        id = "m_push_$now",
                        conversationId = storageId,
                        senderName = title.ifBlank { "对方" },
                        body = lastMessage,
                        isSelf = false,
                        createdAtMs = now,
                        readStatus = ImReadStatus.Unread,
                    )
                }
            }
        }
        sortConversations()
        notifyListeners()
        return storageId
    }
}

/**
 * Deeplink args for `/chat/detail` (Flutter `_mockConversation` / Get.arguments map).
 */
internal data class ChatDetailDeepLinkArgs(
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
internal fun chatDetailArgsFromDeepLink(rawUri: String): ChatDetailDeepLinkArgs? {
    val query = rawUri.substringAfter('?', missingDelimiterValue = "")
        .substringBefore('#')
    if (query.isBlank()) return null
    val map = mutableMapOf<String, String>()
    for (part in query.split('&')) {
        val eq = part.indexOf('=')
        if (eq <= 0) continue
        val key = part.substring(0, eq)
        val value = part.substring(eq + 1)
            .replace("%20", " ")
            .replace("+", " ")
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
        else -> "private_conv_mock_push"
    }
    return ChatDetailDeepLinkArgs(
        id = id,
        peerName = map["peerName"].orEmpty().ifBlank { "推送会话" },
        lastMessage = map["lastMessage"].orEmpty().ifBlank { "来自 Push/Deeplink 的 mock 会话" },
        unreadCount = map["unread"]?.toIntOrNull() ?: 1,
    )
}

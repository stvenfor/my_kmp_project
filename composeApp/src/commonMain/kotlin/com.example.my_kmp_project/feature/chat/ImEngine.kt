package com.example.my_kmp_project.feature.chat

/** Pluggable IM engine; UI depends on this contract, not a third-party SDK. */
internal interface ImEngine {
    fun conversations(): List<ImConversation>
    fun messages(conversationId: String): List<ImMessage>
    fun sendText(conversationId: String, body: String): ImMessage?

    /** Ensure a conversation exists (deeplink / push mock). Returns conversation id. */
    fun ensureConversation(
        id: String,
        title: String,
        lastMessage: String = "",
        unreadCount: Int = 0,
    ): String
}

internal data class ImConversation(
    val id: String,
    val title: String,
    val lastMessage: String,
    val updatedAtLabel: String,
    val unreadCount: Int = 0,
)

internal data class ImMessage(
    val id: String,
    val conversationId: String,
    val senderName: String,
    val body: String,
    val isSelf: Boolean,
    val timeLabel: String,
)

/**
 * In-memory mock engine.
 * Default is empty to match Flutter live SoT (ConversationListEmpty).
 * Pass [seedDemo] = true for local demos / tests that need sample threads.
 */
internal class MockImEngine(
    seedDemo: Boolean = false,
) : ImEngine {
    private val conversationState = mutableListOf<ImConversation>()
    private val messagesByConversation: MutableMap<String, MutableList<ImMessage>> = mutableMapOf()
    private var seq = 100

    init {
        if (seedDemo) {
            conversationState += listOf(
                ImConversation(
                    id = "c1",
                    title = "Mock好友1",
                    lastMessage = "晚上一起吃饭吗？",
                    updatedAtLabel = "22:50",
                    unreadCount = 2,
                ),
                ImConversation(
                    id = "c2",
                    title = "Mock好友2",
                    lastMessage = "你好",
                    updatedAtLabel = "22:45",
                ),
                ImConversation(
                    id = "c3",
                    title = "Mock好友3",
                    lastMessage = "你好",
                    updatedAtLabel = "22:40",
                ),
            )
            messagesByConversation["c1"] = mutableListOf(
                ImMessage("m1", "c1", "Mock好友1", "你好，在吗？", isSelf = false, timeLabel = "22:20"),
                ImMessage("m2", "c1", "我", "在的，有什么事？", isSelf = true, timeLabel = "22:22"),
                ImMessage("m3", "c1", "Mock好友1", "晚上一起吃饭吗？", isSelf = false, timeLabel = "22:50"),
            )
            messagesByConversation["c2"] = mutableListOf(
                ImMessage("m4", "c2", "Mock好友2", "你好", isSelf = false, timeLabel = "22:45"),
            )
            messagesByConversation["c3"] = mutableListOf(
                ImMessage("m5", "c3", "Mock好友3", "你好", isSelf = false, timeLabel = "22:40"),
            )
        }
    }

    override fun conversations(): List<ImConversation> = conversationState.toList()

    override fun messages(conversationId: String): List<ImMessage> =
        messagesByConversation[conversationId].orEmpty().toList()

    override fun sendText(conversationId: String, body: String): ImMessage? {
        val text = body.trim()
        if (text.isEmpty()) return null
        val list = messagesByConversation.getOrPut(conversationId) { mutableListOf() }
        val msg = ImMessage(
            id = "m${++seq}",
            conversationId = conversationId,
            senderName = "我",
            body = text,
            isSelf = true,
            timeLabel = "刚刚",
        )
        list.add(msg)
        val idx = conversationState.indexOfFirst { it.id == conversationId }
        if (idx >= 0) {
            val old = conversationState[idx]
            conversationState[idx] = old.copy(lastMessage = text, updatedAtLabel = "刚刚")
        }
        return msg
    }

    override fun ensureConversation(
        id: String,
        title: String,
        lastMessage: String,
        unreadCount: Int,
    ): String {
        val existing = conversationState.indexOfFirst { it.id == id }
        if (existing >= 0) {
            val old = conversationState[existing]
            conversationState[existing] = old.copy(
                title = title.ifBlank { old.title },
                lastMessage = lastMessage.ifBlank { old.lastMessage },
                unreadCount = unreadCount.takeIf { it > 0 } ?: old.unreadCount,
            )
        } else {
            conversationState.add(
                0,
                ImConversation(
                    id = id,
                    title = title.ifBlank { "会话" },
                    lastMessage = lastMessage.ifBlank { "来自 Push/Deeplink 的 mock 会话" },
                    updatedAtLabel = "刚刚",
                    unreadCount = unreadCount,
                ),
            )
            messagesByConversation.getOrPut(id) { mutableListOf() }.also { list ->
                if (list.isEmpty() && lastMessage.isNotBlank()) {
                    list += ImMessage(
                        id = "m${++seq}",
                        conversationId = id,
                        senderName = title.ifBlank { "对方" },
                        body = lastMessage,
                        isSelf = false,
                        timeLabel = "刚刚",
                    )
                }
            }
        }
        return id
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
    return ChatDetailDeepLinkArgs(
        id = map["id"].orEmpty().ifBlank { "conv_mock_push" },
        peerName = map["peerName"].orEmpty().ifBlank { "推送会话" },
        lastMessage = map["lastMessage"].orEmpty().ifBlank { "来自 Push/Deeplink 的 mock 会话" },
        unreadCount = map["unread"]?.toIntOrNull() ?: 1,
    )
}

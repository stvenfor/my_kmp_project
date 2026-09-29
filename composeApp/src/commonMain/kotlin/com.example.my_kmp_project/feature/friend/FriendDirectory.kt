package com.example.my_kmp_project.feature.friend

import com.example.my_kmp_project.feature.chat.ChatAvatarUrls
import com.example.my_kmp_project.feature.chat.ImEngineStore

/**
 * Friend directory aligned with Flutter / [MockImEngine] seed peers
 * (`mock_peer_01`…`03` →「Mock好友1」…). Accept / open-chat calls [ImEngine.ensureConversation].
 */
internal object FriendDirectory {
    data class Entry(
        val peerId: String,
        val name: String,
        val phoneMasked: String,
        val remark: String = "",
    ) {
        val avatarUrl: String get() = ChatAvatarUrls.peer(peerId)
        fun toFriendItem(): FriendItem = FriendItem(
            id = peerId,
            name = name,
            phoneMasked = phoneMasked,
            avatarUrl = avatarUrl,
            remark = remark,
        )
    }

    /** Seed friends that already have IM conversations (Flutter MockImChatStore peers). */
    val seedFriends: List<Entry> = listOf(
        Entry("mock_peer_01", "Mock好友1", "134****0001", "同事"),
        Entry("mock_peer_02", "Mock好友2", "134****0002"),
        Entry("mock_peer_03", "Mock好友3", "134****0003"),
    )

    /** Searchable directory (not yet friends) — add → friend + IM conversation. */
    val directory: List<Entry> = listOf(
        Entry("mock_peer_09", "新同学小周", "138****0009", "同校"),
        Entry("mock_peer_10", "外教 Anna", "139****0010", "口语"),
    )

    fun ensureChat(peerId: String, title: String): String =
        ImEngineStore.engine.ensureConversation(
            id = "private_$peerId",
            title = title,
            lastMessage = "你们已成为好友",
            unreadCount = 0,
        )

    /** Flutter MockImChatStore.ensureGroupConversation — free group from friend list. */
    fun ensureGroupChat(memberPeerIds: List<String>, title: String): String {
        val key = memberPeerIds.sorted().joinToString("_").ifBlank { "solo" }
        return ImEngineStore.engine.ensureConversation(
            id = "group_$key",
            title = title,
            lastMessage = "群聊已创建",
            unreadCount = 0,
        )
    }
}

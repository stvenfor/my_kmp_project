package com.example.my_kmp_project.feature.friend

import com.example.my_kmp_project.feature.chat.ImEngineStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FriendDirectoryTest {

    @Test
    fun seed_friends_match_im_peers() {
        val peers = FriendDirectory.seedFriends.map { it.peerId }
        assertEquals(listOf("mock_peer_01", "mock_peer_02", "mock_peer_03"), peers)
        val convIds = ImEngineStore.engine.conversations().map { it.peerId }.toSet()
        assertTrue(peers.all { it in convIds })
    }

    @Test
    fun add_friend_ensures_conversation() {
        val id = FriendDirectory.ensureChat("mock_peer_09", "新同学小周")
        assertEquals("private_mock_peer_09", id)
        assertTrue(ImEngineStore.engine.conversations().any { it.id == id })
    }

    @Test
    fun ensure_group_chat() {
        val id = FriendDirectory.ensureGroupChat(
            memberPeerIds = listOf("mock_peer_01", "mock_peer_02"),
            title = "Mock好友1、Mock好友2的群聊",
        )
        assertTrue(id.startsWith("group_"))
        assertTrue(ImEngineStore.engine.conversations().any { it.id == id })
    }
}

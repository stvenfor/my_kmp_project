package com.example.my_kmp_project.feature.chat

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MockImEngineTest {

    @Test
    fun sendText_pipelineAndUnread() {
        val engine = MockImEngine(seedDemo = true)
        val conv = engine.conversations().first()
        engine.markConversationRead(conv.id)
        val msg = engine.sendText(conv.id, "hello logic")
        assertNotNull(msg)
        assertTrue(msg.isSelf)
        assertEquals(ImSendStatus.Sending, msg.sendStatus)
        val listed = engine.messages(conv.id)
        assertTrue(listed.any { it.id == msg.id })
    }

    @Test
    fun ensureConversation_privateAndGroup() {
        val engine = MockImEngine(seedDemo = false)
        val a = engine.ensureConversation("private_peer_x", "Peer X", "hi")
        assertEquals("private_peer_x", a)
        val g = engine.ensureConversation("group_abc", "群聊", "created")
        assertEquals("group_abc", g)
        assertTrue(engine.conversations().any { it.id == g })
    }

    @Test
    fun recallMessage_withinWindow() {
        val engine = MockImEngine(seedDemo = true)
        val conv = engine.conversations().first()
        val msg = engine.sendText(conv.id, "will recall")!!
        // Immediately recallable (within 180s).
        assertTrue(msg.canRecall)
        assertTrue(engine.recallMessage(conv.id, msg.id))
        val after = engine.messages(conv.id).first { it.id == msg.id }
        assertEquals(ImMessageType.System, after.type)
        assertEquals("你撤回了一条消息", after.body)
        assertFalse(engine.recallMessage(conv.id, msg.id)) // already system
    }

    @Test
    fun filterConversations_byTitle() {
        val engine = MockImEngine(seedDemo = true)
        val all = engine.conversations()
        assertTrue(all.isNotEmpty())
        val q = all.first().title.take(2)
        val filtered = engine.filterConversations(q)
        assertTrue(filtered.all { it.title.contains(q) || it.lastMessage.contains(q) })
    }
}

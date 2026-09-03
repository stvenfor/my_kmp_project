package com.example.my_kmp_project.feature.chat

/** Pluggable IM engine; UI depends on this contract, not a third-party SDK. */
internal interface ImEngine {
    fun conversations(): List<ImConversation>
    fun messages(conversationId: String): List<ImMessage>
    fun sendText(conversationId: String, body: String): ImMessage?
}

internal data class ImConversation(
    val id: String,
    val title: String,
    val lastMessage: String,
    val updatedAtLabel: String,
)

internal data class ImMessage(
    val id: String,
    val conversationId: String,
    val senderName: String,
    val body: String,
    val isSelf: Boolean,
    val timeLabel: String,
)

/** In-memory mock engine for development and CI (Flutter Rong wrapper is also mock). */
internal class MockImEngine : ImEngine {
    private val conversationState = mutableListOf(
        ImConversation(
            id = "c1",
            title = "学习助手",
            lastMessage = "今天的学习报告已生成，点击查看。",
            updatedAtLabel = "刚刚",
        ),
        ImConversation(
            id = "c2",
            title = "班级群 · 三年级二班",
            lastMessage = "明天下午的活动改到三点。",
            updatedAtLabel = "10:24",
        ),
        ImConversation(
            id = "c3",
            title = "客服小助手",
            lastMessage = "您好，有什么可以帮您？",
            updatedAtLabel = "昨天",
        ),
        ImConversation(
            id = "c4",
            title = "好友 · 小明",
            lastMessage = "周末一起复习吧",
            updatedAtLabel = "周一",
        ),
    )

    private val messagesByConversation: MutableMap<String, MutableList<ImMessage>> = mutableMapOf(
        "c1" to mutableListOf(
            ImMessage("m1", "c1", "学习助手", "欢迎回来，继续今日学习吧。", isSelf = false, timeLabel = "09:00"),
            ImMessage("m2", "c1", "我", "好的，今天学什么？", isSelf = true, timeLabel = "09:01"),
            ImMessage("m3", "c1", "学习助手", "今天的学习报告已生成，点击查看。", isSelf = false, timeLabel = "刚刚"),
        ),
        "c2" to mutableListOf(
            ImMessage("m4", "c2", "班主任", "各位家长注意通知。", isSelf = false, timeLabel = "10:20"),
            ImMessage("m5", "c2", "我", "收到，谢谢老师。", isSelf = true, timeLabel = "10:22"),
            ImMessage("m6", "c2", "班主任", "明天下午的活动改到三点。", isSelf = false, timeLabel = "10:24"),
        ),
        "c3" to mutableListOf(
            ImMessage("m7", "c3", "客服小助手", "您好，有什么可以帮您？", isSelf = false, timeLabel = "昨天"),
        ),
        "c4" to mutableListOf(
            ImMessage("m8", "c4", "小明", "周末一起复习吧", isSelf = false, timeLabel = "周一"),
            ImMessage("m9", "c4", "我", "好啊，约图书馆。", isSelf = true, timeLabel = "周一"),
        ),
    )

    private var seq = 100

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
}

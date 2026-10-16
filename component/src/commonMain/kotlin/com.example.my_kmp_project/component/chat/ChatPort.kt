package com.example.my_kmp_project.component.chat

/**
 * IM / chat SDK port (tpj-flt `tf_chat` analogue). Reserved in Spike I.
 * Features must depend on this port, not vendor TIM/JMessage APIs directly.
 */
public interface ChatPort {
    val isReady: Boolean
    suspend fun ensureLoggedIn(userId: String, userSig: String): Result<Unit>
}

public object StubChatPort : ChatPort {
    override val isReady: Boolean = false
    override suspend fun ensureLoggedIn(userId: String, userSig: String): Result<Unit> =
        Result.failure(IllegalStateException("Chat SDK not wired"))
}

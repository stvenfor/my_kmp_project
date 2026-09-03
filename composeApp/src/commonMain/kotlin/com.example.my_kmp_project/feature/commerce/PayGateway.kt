package com.example.my_kmp_project.feature.commerce

/** Pluggable pay gateway; UI depends on this contract, not a native SDK. */
internal interface PayGateway {
    fun availableChannels(): List<PayChannel>
    suspend fun pay(channel: PayChannel, planId: String): PayResult
}

internal enum class PayChannel {
    WeChat,
    Alipay,
}

internal sealed interface PayResult {
    data class Success(val channel: PayChannel, val planId: String) : PayResult
    data object Cancel : PayResult
    data class Unavailable(val channel: PayChannel) : PayResult
    data class Failure(val message: String) : PayResult
}

/**
 * Honest empty stub for tests/CI. Never synthesizes [PayResult.Success] —
 * real success requires a platform SDK adapter.
 */
internal class StubPayGateway : PayGateway {
    override fun availableChannels(): List<PayChannel> = emptyList()

    override suspend fun pay(channel: PayChannel, planId: String): PayResult =
        PayResult.Unavailable(channel)
}

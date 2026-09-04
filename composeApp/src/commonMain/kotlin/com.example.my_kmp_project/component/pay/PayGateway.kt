package com.example.my_kmp_project.component.pay

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
    /**
     * @param sandbox true when paid via [SandboxPayChannelAdapter] only — not a real charge.
     */
    data class Success(
        val channel: PayChannel,
        val planId: String,
        val sandbox: Boolean = false,
    ) : PayResult

    data object Cancel : PayResult
    data class Unavailable(val channel: PayChannel) : PayResult
    data class Failure(val message: String) : PayResult
}

/**
 * Honest empty stub for tests/CI. Never synthesizes [PayResult.Success] —
 * real success requires a platform SDK adapter or an explicit sandbox path.
 */
internal class StubPayGateway : PayGateway {
    override fun availableChannels(): List<PayChannel> = emptyList()

    override suspend fun pay(channel: PayChannel, planId: String): PayResult =
        PayResult.Unavailable(channel)
}

/**
 * Feature flags for pay channels.
 *
 * - [weChatEnabled] / [alipayEnabled]: gate real OpenSDK adapters (flags alone ≠ Success).
 * - [sandboxEnabled]: registers [SandboxPayChannelAdapter] when platform adapters are null.
 *   Spike II debug default is on so Must matrix has a non-Unavailable path; turn off for release
 *   once real SDK adapters are wired.
 */
internal object PayFeatureFlags {
    var weChatEnabled: Boolean = false
    var alipayEnabled: Boolean = false
    var sandboxEnabled: Boolean = true
}

/** Native SDK (or sandbox) pay adapter for one channel. */
internal interface PayChannelAdapter {
    val channel: PayChannel
    suspend fun pay(planId: String): PayResult
}

/**
 * Sandbox-only adapter. Returns [PayResult.Success] with `sandbox = true`.
 * Explicit debug/sandbox path — not a production payment confirmation.
 */
internal class SandboxPayChannelAdapter(
    override val channel: PayChannel,
) : PayChannelAdapter {
    override suspend fun pay(planId: String): PayResult =
        PayResult.Success(channel = channel, planId = planId, sandbox = true)
}

/**
 * Platform OpenSDK hooks (androidMain / iosMain / ohosMain).
 * Return a real adapter when wired; null keeps the sandbox fallback available.
 */
internal expect fun platformWeChatPayAdapterOrNull(): PayChannelAdapter?

internal expect fun platformAlipayPayAdapterOrNull(): PayChannelAdapter?

/**
 * Registry of platform pay adapters.
 * Prefer real SDK via expect/actual; fall back to sandbox when [PayFeatureFlags.sandboxEnabled].
 */
internal object PlatformPayAdapters {
    fun weChatOrNull(): PayChannelAdapter? =
        platformWeChatPayAdapterOrNull()
            ?: sandboxOrNull(PayChannel.WeChat)

    fun alipayOrNull(): PayChannelAdapter? =
        platformAlipayPayAdapterOrNull()
            ?: sandboxOrNull(PayChannel.Alipay)

    private fun sandboxOrNull(channel: PayChannel): PayChannelAdapter? =
        if (PayFeatureFlags.sandboxEnabled) SandboxPayChannelAdapter(channel) else null
}

/**
 * Production gateway: channel available when adapter present and channel gate is on.
 * Sandbox gate: [PayFeatureFlags.sandboxEnabled] + [SandboxPayChannelAdapter].
 * Real SDK gate: channel flag + non-sandbox adapter. Never fabricates Success itself.
 */
internal class FlaggedPayGateway(
    private val weChat: PayChannelAdapter? = PlatformPayAdapters.weChatOrNull(),
    private val alipay: PayChannelAdapter? = PlatformPayAdapters.alipayOrNull(),
) : PayGateway {
    override fun availableChannels(): List<PayChannel> = buildList {
        if (isChannelOpen(PayChannel.WeChat, weChat)) add(PayChannel.WeChat)
        if (isChannelOpen(PayChannel.Alipay, alipay)) add(PayChannel.Alipay)
    }

    override suspend fun pay(channel: PayChannel, planId: String): PayResult {
        val adapter = when (channel) {
            PayChannel.WeChat -> weChat
            PayChannel.Alipay -> alipay
        }
        if (!isChannelOpen(channel, adapter) || adapter == null) {
            return PayResult.Unavailable(channel)
        }
        return adapter.pay(planId)
    }

    private fun isChannelOpen(channel: PayChannel, adapter: PayChannelAdapter?): Boolean {
        if (adapter == null) return false
        val realFlag = when (channel) {
            PayChannel.WeChat -> PayFeatureFlags.weChatEnabled
            PayChannel.Alipay -> PayFeatureFlags.alipayEnabled
        }
        if (realFlag) return true
        return PayFeatureFlags.sandboxEnabled && adapter is SandboxPayChannelAdapter
    }
}

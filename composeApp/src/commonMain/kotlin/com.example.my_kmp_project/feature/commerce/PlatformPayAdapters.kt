package com.example.my_kmp_project.feature.commerce

/**
 * Feature flags for native pay channels. Keep false until a real SDK adapter
 * is registered for the current target. Flags alone MUST NOT imply Success.
 */
internal object PayFeatureFlags {
    var weChatEnabled: Boolean = false
    var alipayEnabled: Boolean = false
}

/**
 * Native SDK pay adapter for one channel.
 * Implementations must invoke the real WeChat / Alipay SDK and map SDK outcomes
 * to [PayResult]. Do not return [PayResult.Success] without an SDK callback.
 */
internal interface PayChannelAdapter {
    val channel: PayChannel
    suspend fun pay(planId: String): PayResult
}

/**
 * Registry of platform pay adapters.
 * Returns null until WeChat / Alipay OpenSDK (or OHOS equivalent) is wired.
 */
internal object PlatformPayAdapters {
    /** Live WeChat pay adapter, or null when SDK is missing on this target. */
    fun weChatOrNull(): PayChannelAdapter? = null

    /** Live Alipay adapter, or null when SDK is missing on this target. */
    fun alipayOrNull(): PayChannelAdapter? = null
}

/**
 * Production gateway: a channel is available only when its feature flag is on
 * **and** a real [PayChannelAdapter] is present. Never fabricates Success.
 */
internal class FlaggedPayGateway(
    private val weChat: PayChannelAdapter? = PlatformPayAdapters.weChatOrNull(),
    private val alipay: PayChannelAdapter? = PlatformPayAdapters.alipayOrNull(),
) : PayGateway {
    override fun availableChannels(): List<PayChannel> = buildList {
        if (PayFeatureFlags.weChatEnabled && weChat != null) add(PayChannel.WeChat)
        if (PayFeatureFlags.alipayEnabled && alipay != null) add(PayChannel.Alipay)
    }

    override suspend fun pay(channel: PayChannel, planId: String): PayResult {
        val adapter = when (channel) {
            PayChannel.WeChat -> weChat
            PayChannel.Alipay -> alipay
        }
        val flagOn = when (channel) {
            PayChannel.WeChat -> PayFeatureFlags.weChatEnabled
            PayChannel.Alipay -> PayFeatureFlags.alipayEnabled
        }
        if (!flagOn || adapter == null || channel !in availableChannels()) {
            return PayResult.Unavailable(channel)
        }
        return adapter.pay(planId)
    }
}

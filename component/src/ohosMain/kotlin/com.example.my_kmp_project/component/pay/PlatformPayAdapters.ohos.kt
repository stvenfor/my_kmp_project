package com.example.my_kmp_project.component.pay

/**
 * OHOS pay SDK hook — no Android OpenSDK on this source set.
 * Null keeps [SandboxPayChannelAdapter] available when sandboxEnabled.
 */
internal actual fun platformWeChatPayAdapterOrNull(): PayChannelAdapter? = null

internal actual fun platformAlipayPayAdapterOrNull(): PayChannelAdapter? = null

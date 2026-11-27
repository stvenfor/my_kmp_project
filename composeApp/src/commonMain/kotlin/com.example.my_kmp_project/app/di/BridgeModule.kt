package com.example.my_kmp_project.app.di

import com.example.my_kmp_project.component.chat.ChatPort
import com.example.my_kmp_project.component.chat.StubChatPort
import com.example.my_kmp_project.component.pay.FlaggedPayGateway
import com.example.my_kmp_project.component.pay.PayGateway
import com.example.my_kmp_project.component.push.PushBridge
import com.example.my_kmp_project.component.push.StubPushBridge
import com.example.my_kmp_project.component.routing.CrossFeatureScreenRegistry
import com.example.my_kmp_project.component.routing.DelegatingCrossFeatureScreenRegistry

/**
 * 平台能力桥绑定（DI 第三批）：推送 / 支付 / IM / 跨功能屏幕。
 *
 * 默认实现与第三批拆分前 `AppContainer.BridgeComponent` 一一对应
 * （Stub 推送 / 带 sandbox 的 FlaggedPayGateway / 未接 SDK 的 StubChatPort）。
 */
internal object BridgeModule {
    fun crossFeatureScreens(): CrossFeatureScreenRegistry = DelegatingCrossFeatureScreenRegistry

    fun push(): PushBridge = StubPushBridge

    /** FlaggedPayGateway：sandbox 适配器在 PayFeatureFlags.sandboxEnabled（默认）时可用。 */
    fun pay(): PayGateway = FlaggedPayGateway()

    fun chat(): ChatPort = StubChatPort
}

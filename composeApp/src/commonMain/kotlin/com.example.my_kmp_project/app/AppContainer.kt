package com.example.my_kmp_project.app

import com.example.my_kmp_project.component.chat.ChatPort
import com.example.my_kmp_project.component.chat.StubChatPort
import com.example.my_kmp_project.component.pay.FlaggedPayGateway
import com.example.my_kmp_project.component.pay.PayGateway
import com.example.my_kmp_project.component.push.PushBridge
import com.example.my_kmp_project.component.push.StubPushBridge
import com.example.my_kmp_project.core.network.NetworkFacade
import com.example.my_kmp_project.core.router.AppNavigator
import com.example.my_kmp_project.core.router.AppRoute
import com.example.my_kmp_project.core.router.DeepLinkRouter

/**
 * Composition root (Spike I). Grouped subcomponents avoid a flat god object;
 * migrate to Koin modules after Spike I (ADR 0001 / Q15=D).
 */
internal class AppContainer(
    val network: NetworkComponent = NetworkComponent(),
    val session: SessionComponent = SessionComponent(),
    val bridges: BridgeComponent = BridgeComponent(),
    val navigation: NavigationComponent = NavigationComponent(),
) {
    companion object {
        private val instance: AppContainer by lazy { AppContainer() }
        fun get(): AppContainer = instance
    }
}

internal class NetworkComponent {
    fun api() = NetworkFacade.api()
}

internal class SessionComponent {
    // AuthSessionState remains the reactive SoT; this holds future SessionRepository.
}

internal class BridgeComponent(
    val push: PushBridge = StubPushBridge,
    /** FlaggedPayGateway with sandbox adapters when PayFeatureFlags.sandboxEnabled (default). */
    val pay: PayGateway = FlaggedPayGateway(),
    val chat: ChatPort = StubChatPort,
)

internal class NavigationComponent {
    private var navigator: AppNavigator? = null

    fun bind(onNavigate: (AppRoute) -> Unit): AppNavigator {
        val nav = AppNavigator(onNavigate)
        navigator = nav
        return nav
    }

    fun navigatorOrNull(): AppNavigator? = navigator

    fun consumeDeepLink() = DeepLinkRouter.consumePending()
}

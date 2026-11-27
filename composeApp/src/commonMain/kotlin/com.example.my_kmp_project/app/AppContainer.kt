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
import com.example.my_kmp_project.core.ui.VideoPositionMemory
import com.example.my_kmp_project.feature.home.HomeContentStore
import com.example.my_kmp_project.feature.home.HomeDashboardStore
import com.example.my_kmp_project.feature.home.api.HomeContentApi
import com.example.my_kmp_project.feature.home.api.HomeDashboardApi
import com.example.my_kmp_project.feature.media.InMemoryVideoPositionMemory

/**
 * Composition root (Spike I). Grouped subcomponents avoid a flat god object;
 * migrate to Koin modules after Spike I (ADR 0001 / Q15=D).
 */
internal class AppContainer(
    val network: NetworkComponent = NetworkComponent(),
    val session: SessionComponent = SessionComponent(),
    val bridges: BridgeComponent = BridgeComponent(),
    val navigation: NavigationComponent = NavigationComponent(),
    val home: HomeComponent = HomeComponent(),
    /** 跨页视频进度记忆（AppScope，进程内）。 */
    val videoPositions: VideoPositionMemory = InMemoryVideoPositionMemory(),
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

/**
 * AppScope 首页看板：共享壳 [HomeScreen] 与 Android 原生壳 [JetpackHomeRoot]
 * 复用同一份内存缓存，避免两套壳各拉一份。
 */
internal class HomeComponent {
    val dashboard: HomeDashboardStore =
        HomeDashboardStore(HomeDashboardApi(NetworkFacade.api()))

    /** 首页本地静态内容（全部服务 / 搜索 / 热榜 / 学习报告 / 投资策略）的 AppScope 缓存。 */
    val content: HomeContentStore =
        HomeContentStore(HomeContentApi(NetworkFacade.api()))
}

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

package com.example.my_kmp_project.app.di

import com.example.my_kmp_project.component.chat.ChatPort
import com.example.my_kmp_project.component.di.AppGraph
import com.example.my_kmp_project.component.di.RealtimeGraph
import com.example.my_kmp_project.component.di.SessionGraph
import com.example.my_kmp_project.component.di.StorageProvider
import com.example.my_kmp_project.component.pay.PayGateway
import com.example.my_kmp_project.component.push.PushBridge
import com.example.my_kmp_project.component.routing.CrossFeatureScreenRegistry
import com.example.my_kmp_project.core.network.ApiClientProvider
import com.example.my_kmp_project.core.ui.MusicSessionPort
import com.example.my_kmp_project.feature.media.PlaybackSession
import kotlin.concurrent.Volatile

/**
 * 应用图默认实现（DI 第三批：基础设施迁移）。
 *
 * 全部绑定来自 [CoreModule] / [BridgeModule]，默认值仍是过渡期单例的接口视图，
 * 所以换绑不改变现有行为；Session 作用域由 [AppBootstrap] 驱动 attach / detach。
 */
internal class AppGraphImpl(
    override val network: ApiClientProvider = CoreModule.network(),
    override val storage: StorageProvider = CoreModule.storage(),
    override val realtime: RealtimeGraph = CoreModule.realtime(storage = storage),
    override val crossFeatureScreens: CrossFeatureScreenRegistry = BridgeModule.crossFeatureScreens(),
    override val push: PushBridge = BridgeModule.push(),
    override val pay: PayGateway = BridgeModule.pay(),
    override val chat: ChatPort = BridgeModule.chat(),
) : AppGraph {

    /**
     * AppScope 音乐会话：惰性创建，避免建图（含单测）时实例化平台播放器。
     */
    override val musicSession: MusicSessionPort by lazy { PlaybackSession() }

    @Volatile
    private var sessionGraph: SessionGraph? = null

    override val sessionOrNull: SessionGraph? get() = sessionGraph

    override fun requireSession(): SessionGraph =
        sessionGraph ?: error("AppGraph has no active SessionGraph — call AppBootstrap.sessionReady() after login.")

    override fun attachSession(session: SessionGraph) {
        sessionGraph = session
    }

    override fun detachSession() {
        sessionGraph = null
    }
}

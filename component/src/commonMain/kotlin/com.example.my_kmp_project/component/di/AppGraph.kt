package com.example.my_kmp_project.component.di

import com.example.my_kmp_project.component.chat.ChatPort
import com.example.my_kmp_project.component.pay.PayGateway
import com.example.my_kmp_project.component.push.PushBridge
import com.example.my_kmp_project.component.routing.CrossFeatureScreenRegistry
import com.example.my_kmp_project.core.account.SessionRepository
import com.example.my_kmp_project.core.account.SessionState
import com.example.my_kmp_project.core.network.ApiClientProvider
import com.example.my_kmp_project.core.ui.MusicSessionPort

/**
 * 应用级依赖图契约（DI 第一批）。
 *
 * 只暴露接口，实现（默认单例 / 测试替身）在 app 装配层。
 * 本批不接线：现有入口继续走旧 object，[AppGraph] 是新代码与测试的入口。
 */
public interface AppGraph {
    public val network: ApiClientProvider

    /** 持久化：命名 KV 视图（账号 / 隐私 / 实时水位都从这里取）。 */
    public val storage: StorageProvider

    /** 实时长连接：客户端 + 启动闸门（何时首连由 app 层驱动）。 */
    public val realtime: RealtimeGraph

    public val crossFeatureScreens: CrossFeatureScreenRegistry

    public val push: PushBridge

    public val pay: PayGateway

    public val chat: ChatPort

    /** 音乐会话（AppScope 单例）：跨屏 mini-player 与 Now Playing 共用。 */
    public val musicSession: MusicSessionPort

    /** 登录后才有会话作用域；未登录为 null。 */
    public val sessionOrNull: SessionGraph?

    public fun requireSession(): SessionGraph

    public fun attachSession(session: SessionGraph)

    public fun detachSession()
}

/** 会话作用域：登录建立、登出销毁；会话级仓库与响应式状态都挂在这里。 */
public interface SessionGraph {
    public val repository: SessionRepository

    public val state: SessionState
}

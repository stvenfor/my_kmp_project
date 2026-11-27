package com.example.my_kmp_project.app.di

import com.example.my_kmp_project.component.auth.AuthSessionState
import com.example.my_kmp_project.component.di.RealtimeGraph
import com.example.my_kmp_project.component.di.StorageProvider
import com.example.my_kmp_project.component.realtime.AppRealtimeClient
import com.example.my_kmp_project.component.realtime.AppRealtimeClientImpl
import com.example.my_kmp_project.component.realtime.InMemoryRealtimeKeyValueStore
import com.example.my_kmp_project.component.realtime.InMemoryRealtimeOutboundStore
import com.example.my_kmp_project.component.realtime.RealtimeInboundRouter
import com.example.my_kmp_project.component.realtime.RealtimeNotifyHandler
import com.example.my_kmp_project.component.realtime.RealtimeOutboundQueue
import com.example.my_kmp_project.component.realtime.RealtimeSeqStore
import com.example.my_kmp_project.component.realtime.RealtimeSessionPort
import com.example.my_kmp_project.component.realtime.RealtimeStartupGate
import com.example.my_kmp_project.component.realtime.RealtimeTelemetry
import com.example.my_kmp_project.core.account.ACCOUNT_STORE_NAME
import com.example.my_kmp_project.core.account.AccountFacade
import com.example.my_kmp_project.core.account.PrivacyConsentStore
import com.example.my_kmp_project.core.account.SessionRepository
import com.example.my_kmp_project.core.account.SessionState
import com.example.my_kmp_project.core.account.createPrivacyConsentStore
import com.example.my_kmp_project.core.network.ApiClientProvider
import com.example.my_kmp_project.core.network.NetworkFacade
import com.example.my_kmp_project.core.network.realtime.RealtimeWsSyncApi
import com.example.my_kmp_project.core.network.realtime.RealtimeWsTicketApi
import com.example.my_kmp_project.core.storage.KeyValueStore
import com.example.my_kmp_project.core.storage.createKeyValueStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Core 绑定（DI 第三批）：网络 / 存储 / 账号 / 实时。
 *
 * 全部指向过渡期单例的接口视图或既有平台工厂，行为与拆分前一致；
 * 后续把单例内部实现换掉时，只需要改这里的绑定。
 */
internal object CoreModule {

    /** Realtime 客户端与闸门共用；只用于并发调度，不持有业务状态。 */
    private val realtimeScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun network(): ApiClientProvider = NetworkFacade.provider()

    fun storage(): StorageProvider = DefaultStorageProvider

    fun accountRepository(): SessionRepository = AccountFacade.repository()

    fun sessionState(): SessionState = AuthSessionState.state()

    fun realtime(
        storage: StorageProvider = storage(),
        session: RealtimeSessionPort = LegacyRealtimeSession,
    ): RealtimeGraph = DefaultRealtimeGraph(
        scope = realtimeScope,
        storage = storage,
        session = session,
    )
}

/** 默认存储绑定：沿用平台 KV 工厂（同 name = 同数据）。 */
internal object DefaultStorageProvider : StorageProvider {
    override fun keyValueStore(name: String): KeyValueStore = createKeyValueStore(name)
}

/**
 * Realtime 会话缝隙：登录态与 token 从 [AccountFacade] 现取。
 * 无 token 时抛异常，由默认鉴权硬失败策略认成 hard failure（既有契约）。
 */
internal object LegacyRealtimeSession : RealtimeSessionPort {
    override fun isLoggedIn(): Boolean = AccountFacade.current().isLoggedIn

    override fun accessToken(): String {
        val token = AccountFacade.current().token
        if (token.isBlank()) throw IllegalStateException("未登录或 token 为空，无法建 WS")
        return token
    }
}

/**
 * Realtime 绑定实现：构造序列与第三批拆分前 [com.example.my_kmp_project.app.RealtimeRuntime]
 * 完全一致（换票 / 补投 / 状态机 / 闸门），只把「何时首连」留给 RealtimeRuntime。
 */
internal class DefaultRealtimeGraph(
    scope: CoroutineScope,
    private val storage: StorageProvider,
    private val session: RealtimeSessionPort,
) : RealtimeGraph {

    /** 惰性创建：构造图（含单测）不触碰平台持久化，首次闸门检查才读。 */
    private val privacyStore: PrivacyConsentStore by lazy {
        createPrivacyConsentStore(storage.keyValueStore(ACCOUNT_STORE_NAME))
    }

    override val client: AppRealtimeClient = AppRealtimeClientImpl(
        ticketApi = RealtimeWsTicketApi(),
        syncApi = RealtimeWsSyncApi(),
        outboundQueue = RealtimeOutboundQueue(InMemoryRealtimeOutboundStore()),
        seqStore = RealtimeSeqStore(InMemoryRealtimeKeyValueStore()),
        router = RealtimeInboundRouter(),
        // 通知横幅属 UI wave：留无操作落地口，不假装已展示。
        notifyHandler = RealtimeNotifyHandler { },
        telemetry = RealtimeTelemetry(),
        session = session,
        scope = scope,
    )

    override val startupGate: RealtimeStartupGate = RealtimeStartupGate(
        client = client,
        session = session,
        scope = scope,
        privacyGranted = { privacyStore.isAccepted() },
    )
}

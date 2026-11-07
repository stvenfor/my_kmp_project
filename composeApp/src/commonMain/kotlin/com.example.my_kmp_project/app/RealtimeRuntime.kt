package com.example.my_kmp_project.app

import com.example.my_kmp_project.component.realtime.AppRealtimeClientImpl
import com.example.my_kmp_project.component.realtime.InMemoryRealtimeKeyValueStore
import com.example.my_kmp_project.component.realtime.InMemoryRealtimeOutboundStore
import com.example.my_kmp_project.component.realtime.RealtimeAppLifecycle
import com.example.my_kmp_project.component.realtime.RealtimeInboundRouter
import com.example.my_kmp_project.component.realtime.RealtimeNotifyHandler
import com.example.my_kmp_project.component.realtime.RealtimeOutboundQueue
import com.example.my_kmp_project.component.realtime.RealtimeSeqStore
import com.example.my_kmp_project.component.realtime.RealtimeSessionPort
import com.example.my_kmp_project.component.realtime.RealtimeStartupGate
import com.example.my_kmp_project.component.realtime.RealtimeTelemetry
import com.example.my_kmp_project.core.account.AccountFacade
import com.example.my_kmp_project.core.account.createPrivacyConsentStore
import com.example.my_kmp_project.core.network.realtime.RealtimeWsSyncApi
import com.example.my_kmp_project.core.network.realtime.RealtimeWsTicketApi
import kotlin.concurrent.Volatile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Realtime 生产构造点 —— 对齐 Dart `RealtimeInitializer.initDeferred()`
 * （components/realtime/lib/realtime_initializer.dart:28-59，d7876a1）。
 *
 * Wave 1 把状态机 / 启动闸门下沉到 component/realtime、换票·补投·WS 传输下沉到
 * core/network/realtime，但生产侧没有任何装配点，Ktor 传输也从未被实例化。这里把
 * [AppRealtimeClientImpl]（默认 transportFactory 走 createRealtimeTransport →
 * createPlatformRealtimeWsTransport → Ktor 传输）与 [RealtimeStartupGate] 接进壳层。
 *
 * 五个回调的装配位置：
 * - 首连 app_startup：[ShellBootstrap.install] 调 [install]，本对象把 prepare 与首连
 *   全部丢进 [scope] 后台协程，install 本身只构造对象、不 await ⇒ 不阻塞冷启动。
 *   12s / 8s 两个期限只记日志；隐私未同意 / 未登录由闸门自行跳过（见 RealtimeStartupGate）。
 * - onPrivacyGranted / onAfterLogin / onAfterLogout：AppShell 三个交互点调下面的方法。
 * - onEnvChanged / onAppLifecycle：装配层已暴露入口，但触发源在平台 source set 与
 *   feature/mine 环境切换 UI（不在本 wave 白名单），属书面 GAP，见验收报告，不塞伪实现。
 */
internal object RealtimeRuntime {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    private var installed = false

    @Volatile
    private var gate: RealtimeStartupGate? = null

    fun install() {
        if (installed) return
        installed = true

        // 会话缝隙：登录态与 token 从 AccountFacade 现取。无 token 时抛 StateError 等价异常，
        // 由 DefaultRealtimeAuthHardFailurePolicy 认成鉴权硬失败（对齐 AppRealtimeClient 契约）。
        val sessionPort = object : RealtimeSessionPort {
            override fun isLoggedIn(): Boolean = AccountFacade.current().isLoggedIn
            override fun accessToken(): String {
                val token = AccountFacade.current().token
                if (token.isBlank()) throw IllegalStateException("未登录或 token 为空，无法建 WS")
                return token
            }
        }

        // 隐私同意持久化：与 AppShell / platform-gap-registry 同一份 KV（core.storage）。
        val privacyStore = createPrivacyConsentStore()

        val client = AppRealtimeClientImpl(
            ticketApi = RealtimeWsTicketApi(),
            syncApi = RealtimeWsSyncApi(),
            outboundQueue = RealtimeOutboundQueue(InMemoryRealtimeOutboundStore()),
            seqStore = RealtimeSeqStore(InMemoryRealtimeKeyValueStore()),
            router = RealtimeInboundRouter(),
            // 通知横幅属 UI wave（本 wave 不搬）：留无操作落地口，不假装已展示。
            notifyHandler = RealtimeNotifyHandler { },
            telemetry = RealtimeTelemetry(),
            session = sessionPort,
            scope = scope,
            // 默认 transportFactory = createRealtimeTransport()：生产路径实例化 Ktor 传输；
            // ohosX64 无 CPF WS klib 时该工厂抛类型化 Unsupported，状态机停重连（GAP(R5)）。
        )

        val startupGate = RealtimeStartupGate(
            client = client,
            session = sessionPort,
            scope = scope,
            privacyGranted = { privacyStore.isAccepted() },
        )
        gate = startupGate

        // 非阻塞首连：install() 到此为止只构造对象；prepare 与「12s 超时 + 8s 等待」的首连
        // 全部在后台 scope 里跑，不 await、不阻塞 runApp，也就不拖慢冷启动或停在原生启动图。
        // 隐私未同意 / 未登录时 tryConnectIfReady 直接跳过，不会拨号。
        scope.launch {
            startupGate.prepare()
            startupGate.tryConnectIfReady(trigger = "app_startup")
        }
    }

    /** 隐私同意后补一次首连（AppShell 隐私页 onAccept）。 */
    fun onPrivacyGranted() {
        gate?.let { g -> scope.launch { g.onPrivacyGranted() } }
    }

    /** 登录成功后补连（AppShell afterAuthSuccess）。 */
    fun onAfterLogin() {
        gate?.let { g -> scope.launch { g.onAfterLogin() } }
    }

    /** 登出清连接、不清出站队列（AppShell onLogoutClick）。 */
    fun onAfterLogout() {
        gate?.let { g -> scope.launch { g.onAfterLogout() } }
    }

    /**
     * 切环境：断开再重连。GAP：环境切换在 feature/mine MineSettingsScreen 写
     * NetworkConfig，未暴露 commonMain 可见的变更事件；后续 wave 在切换处转发本方法。
     */
    fun onEnvChanged() {
        gate?.let { g -> scope.launch { g.onEnvChanged() } }
    }

    /**
     * 平台生命周期：GAP：resumed / paused 事件在平台 source set
     * （MainActivity.onResume/onPause、iOS scene phase、OHOS ability 生命周期）里，
     * 不在本 wave 白名单；后续 wave 转发 RealtimeAppLifecycle 进来。
     */
    fun onAppLifecycle(state: RealtimeAppLifecycle) {
        gate?.onAppLifecycle(state)
    }
}

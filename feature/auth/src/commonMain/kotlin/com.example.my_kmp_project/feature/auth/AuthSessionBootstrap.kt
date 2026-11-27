package com.example.my_kmp_project.feature.auth

import com.example.my_kmp_project.component.auth.AuthSessionState
import com.example.my_kmp_project.core.account.SessionRepository
import com.example.my_kmp_project.core.network.ApiClient
import com.example.my_kmp_project.core.network.NetworkFacade

/**
 * 会话 + 刷新装机入口（幂等，三端原生壳与 legacy shell 启动时调用）。
 *
 * 注册后 [com.example.my_kmp_project.core.network.NetworkFacade] 的 401 流由
 * [AuthTokenRefresh] 独占：先静默刷新再重试；刷新耗尽/互踢才清会话回登录
 * （Flutter d7876a1 `AuthTokenRefreshInterceptor` 语义）。
 *
 * DI 第四批：本模块的单例编排对象不再直接摸 [NetworkFacade] / `AccountFacade`，
 * 依赖由 app 装配层在启动时一次性注入（[install] 的必填参数）。
 */
public object AuthSessionBootstrap {

    @kotlin.concurrent.Volatile
    private var installed = false

    /**
     * 注入网络 client 与会话仓库，并登记 401 刷新策略。
     *
     * @param session 会话读写端口（生产传 `CoreModule.accountRepository()`）。
     * @param client 装饰后的 [ApiClient]（生产传 `AppGraph.network.api()`）。
     */
    public fun install(session: SessionRepository, client: ApiClient) {
        AuthDependencies.install(
            api = UserAuthHttpApi(client),
            client = client,
            session = session,
        )
        if (installed) return
        installed = true
        AuthSessionState.sync()
        NetworkFacade.setAuthRefreshSupport(
            policy = AuthTokenRefresh.policy,
            refresher = AuthTokenRefresh.handler,
            onSessionCleared = { AuthTokenRefresh.handleRefreshExhausted() },
        )
    }
}

/**
 * feature/auth 内部的依赖注入点（过渡期）：只由 [AuthSessionBootstrap.install]
 * 写入一次，供本模块的 object 编排层读取。
 *
 * 单例（[AuthRepository] / [AuthTokenRefresh] / [AuthBridge]）保留 object 形态以
 * 兼容三端原生调用点，但依赖全部来自这里，不再直接引用 `NetworkFacade` /
 * `AccountFacade`。后续若把单例改成 class，只需把本对象替换成构造参数。
 */
internal object AuthDependencies {

    private class Services(
        val api: UserAuthHttpApi,
        val client: ApiClient,
        val session: SessionRepository,
    )

    @kotlin.concurrent.Volatile
    private var services: Services? = null

    internal fun install(api: UserAuthHttpApi, client: ApiClient, session: SessionRepository) {
        services = Services(api = api, client = client, session = session)
    }

    internal val api: UserAuthHttpApi get() = require().api

    internal val client: ApiClient get() = require().client

    internal val session: SessionRepository get() = require().session

    private fun require(): Services =
        services ?: error("AuthDependencies not installed — call AuthSessionBootstrap.install() at shell start.")
}

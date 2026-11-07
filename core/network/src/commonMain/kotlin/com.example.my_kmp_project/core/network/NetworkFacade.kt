package com.example.my_kmp_project.core.network

import kotlin.concurrent.Volatile

/**
 * Process-wide network entry for the demo skeleton.
 * Features depend on this façade only — never construct engines in feature code.
 */
public object NetworkFacade {
    @Volatile
    private var tokenExpiredHandler: TokenExpiredHandler? = null

    @Volatile
    private var businessHandlers: NetworkBusinessHandlers? = null

    @Volatile
    private var authRefreshPolicy: AuthRefreshPolicy? = null

    @Volatile
    private var tokenRefresher: TokenRefreshHandler? = null

    @Volatile
    private var onSessionCleared: (() -> Unit)? = null

    @Volatile
    private var rawClient: ApiClient = createPlatformApiClient(
        onTokenExpired = tokenExpiredHandler,
        businessHandlers = businessHandlers,
    )

    @Volatile
    private var client: ApiClient = decorate(rawClient)

    fun api(): ApiClient = client

    /**
     * 未装饰 client（refresh 请求专用）——Flutter `skipAuthRefresh` 的 KMP 对等：
     * 用它发出的请求不会递归触发刷新。
     */
    fun rawApi(): ApiClient = rawClient

    fun setTokenExpiredHandler(handler: TokenExpiredHandler?) {
        tokenExpiredHandler = handler
        rebuildClient()
    }

    fun setBusinessHandlers(handlers: NetworkBusinessHandlers?) {
        businessHandlers = handlers
        rebuildClient()
    }

    /**
     * 启用 Flutter `AuthTokenRefreshInterceptor` 对等能力（feature/auth 在启动时调用）。
     * 启用后平台 client 不再直接触发 [tokenExpiredHandler]——401 流由
     * [AuthRefreshingApiClient] 独占（刷新→重试 / 清会话），避免「先跳登录再刷新成功」。
     */
    fun setAuthRefreshSupport(
        policy: AuthRefreshPolicy?,
        refresher: TokenRefreshHandler?,
        onSessionCleared: (() -> Unit)? = null,
    ) {
        authRefreshPolicy = policy
        tokenRefresher = refresher
        this.onSessionCleared = onSessionCleared
        rebuildClient()
    }

    fun rebuildClient() {
        rawClient = createPlatformApiClient(
            onTokenExpired = if (isAuthRefreshEnabled()) null else tokenExpiredHandler,
            businessHandlers = businessHandlers,
        )
        client = decorate(rawClient)
    }

    /**
     * 绑定登录会话三件套：access token + 单设备会话头（`X-Session-ID` / `X-Device-ID`）。
     *
     * 后端 `LocalSessionAuth` / `SupabaseSessionAuth` 读这两个头交给
     * `DeviceSessionUsecase.Validate` 做单设备登录校验；会话变更（登录 / 静默续期 /
     * 冷启动恢复）必须把三者一起绑定，否则真机上只发 `Authorization`、会话/设备口径不一致。
     * `sessionId` / `deviceId` 缺省为空 = 无会话头，兼容仅换 token 的旧调用。
     */
    fun bindAccessToken(
        token: String,
        sessionId: String = "",
        deviceId: String = "",
    ) {
        NetworkConfig.accessToken = token
        NetworkConfig.sessionId = sessionId
        NetworkConfig.deviceId = deviceId
        rebuildClient()
    }

    fun clearAccessToken() {
        NetworkConfig.accessToken = ""
        NetworkConfig.sessionId = ""
        NetworkConfig.deviceId = ""
        rebuildClient()
    }

    internal fun currentTokenExpiredHandler(): TokenExpiredHandler? = tokenExpiredHandler

    internal fun currentBusinessHandlers(): NetworkBusinessHandlers? = businessHandlers

    private fun isAuthRefreshEnabled(): Boolean =
        authRefreshPolicy != null && tokenRefresher != null

    private fun decorate(platform: ApiClient): ApiClient {
        val p = authRefreshPolicy
        val r = tokenRefresher
        return if (p != null && r != null) {
            AuthRefreshingApiClient(
                delegate = platform,
                policy = p,
                refresher = r,
                onSessionCleared = onSessionCleared,
            )
        } else {
            platform
        }
    }
}

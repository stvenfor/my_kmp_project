package com.example.my_kmp_project.core.network

/** Shared request headers for demo HTTP clients. */
public object DemoRequestHeaders {
    /** MockAuthConfig.issueToken 的前缀：本地演示凭证，不是 BFF 可校验的 JWT。 */
    private const val MOCK_TOKEN_PREFIX = "mock_"

    /**
     * 后端实际读取的头名，勿自造：
     * my_go_study `app/internal/delivery/http/middleware/supabase_session_auth.go`
     * `HeaderSessionID` / `HeaderDeviceID`；`LocalSessionAuth` / `SupabaseSessionAuth`
     * 读这两个头交给 `DeviceSessionUsecase.Validate` 做单设备登录校验。
     */
    public const val HEADER_SESSION_ID: String = "X-Session-ID"
    public const val HEADER_DEVICE_ID: String = "X-Device-ID"

    fun forUrl(url: String): Map<String, String> = buildMap {
        /**
         * Client-Type / Client-Version / Client-BuildNumber：后端当前**不读取**这三个头，
         * 保留仅为契约兼容（对齐 Flutter 头注入 legacy），勿据此做服务端分支。
         */
        put("Client-Type", NetworkConfig.clientType)
        NetworkConfig.clientVersion?.let { put("Client-Version", it) }
        NetworkConfig.clientBuildNumber?.let { put("Client-BuildNumber", it) }
        val token = NetworkConfig.accessToken
        // debug 的 Mock 会话不请求 BFF（见 MockAuthConfig），把假 token 发出去只会被
        // Go LocalSessionAuth 判为无效 JWT（401）。联调环境社区读接口已允许匿名访问。
        val usableToken = token.takeIf { it.isNotBlank() && !it.startsWith(MOCK_TOKEN_PREFIX) }
        if (usableToken != null && !NetworkConfig.skipAuthForUrl(url)) {
            put("Authorization", "Bearer $usableToken")
            // 与 Authorization 同一 gating：登录态 + 非免鉴权 host 才带会话头，
            // 空白值省略，避免把空会话/旧设备口径发给后端。
            NetworkConfig.sessionId.takeIf { it.isNotBlank() }?.let { put(HEADER_SESSION_ID, it) }
            NetworkConfig.deviceId.takeIf { it.isNotBlank() }?.let { put(HEADER_DEVICE_ID, it) }
        }
    }
}

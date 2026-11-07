package com.example.my_kmp_project.core.network

/** Shared request headers for demo HTTP clients. */
public object DemoRequestHeaders {
    /**
     * 后端实际读取的头名，勿自造：
     * my_go_study `app/internal/delivery/http/middleware/supabase_session_auth.go`
     * `HeaderSessionID` / `HeaderDeviceID`；`LocalSessionAuth` / `SupabaseSessionAuth`
     * 读这两个头交给 `DeviceSessionUsecase.Validate` 做单设备登录校验。
     */
    public const val HEADER_SESSION_ID: String = "X-Session-ID"
    public const val HEADER_DEVICE_ID: String = "X-Device-ID"

    fun forUrl(url: String): Map<String, String> = buildMap {
        put("Client-Type", NetworkConfig.clientType)
        NetworkConfig.clientVersion?.let { put("Client-Version", it) }
        NetworkConfig.clientBuildNumber?.let { put("Client-BuildNumber", it) }
        val token = NetworkConfig.accessToken
        if (token.isNotBlank() && !NetworkConfig.skipAuthForUrl(url)) {
            put("Authorization", "Bearer $token")
            // 与 Authorization 同一 gating：登录态 + 非免鉴权 host 才带会话头，
            // 空白值省略，避免把空会话/旧设备口径发给后端。
            NetworkConfig.sessionId.takeIf { it.isNotBlank() }?.let { put(HEADER_SESSION_ID, it) }
            NetworkConfig.deviceId.takeIf { it.isNotBlank() }?.let { put(HEADER_DEVICE_ID, it) }
        }
    }
}

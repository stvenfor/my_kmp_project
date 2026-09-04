package com.example.my_kmp_project.component.push

import com.example.my_kmp_project.core.router.AppRoutes
import com.example.my_kmp_project.core.router.DeepLinkRouter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Push bridge port (tpj-flt `tf_push` analogue).
 *
 * Spike II Must (skeleton here):
 * - device token obtain + upload
 * - notification click → deep link via [DeepLinkRouter] / [AppRoutes]
 *
 * Platform vendor SDKs (FCM / APNs / PushKit) wire through [PlatformPushSdk];
 * until then token APIs are stubbed and Must matrix stays **Partial**.
 */
internal interface PushBridge {
    /** Request OS / vendor registration; may return null token while SDK is unwired. */
    suspend fun registerForPush(): PushRegistration

    /** Upload [token] to backend (stub succeeds until API exists). */
    suspend fun uploadToken(token: String): Result<Unit>

    /** Latest known device token; null until register/upload. */
    fun currentToken(): StateFlow<String?>

    /**
     * Notification click entry.
     * Payload keys (first match wins): `deeplink`, `url`, `route`
     * (route may be an [AppRoutes] path such as `/mine` or `/auth/login`).
     */
    fun handleNotificationClick(payload: Map<String, String>)
}

internal data class PushRegistration(
    val token: String?,
    val platform: String,
)

/**
 * Default stub bridge: in-memory token Flow + deep-link handoff.
 * Replace [PlatformPushSdk] actuals to complete three-platform Must.
 */
internal object StubPushBridge : PushBridge {
    private val _token = MutableStateFlow<String?>(null)

    override suspend fun registerForPush(): PushRegistration {
        val token = PlatformPushSdk.fetchRegistrationToken()
        if (token != null) {
            _token.value = token
        }
        return PushRegistration(
            token = _token.value,
            platform = PlatformPushSdk.platformName(),
        )
    }

    override suspend fun uploadToken(token: String): Result<Unit> {
        // TODO(Spike II): POST token to session-bound push registration API.
        _token.value = token
        return Result.success(Unit)
    }

    override fun currentToken(): StateFlow<String?> = _token.asStateFlow()

    override fun handleNotificationClick(payload: Map<String, String>) {
        val uri = extractDeepLink(payload) ?: return
        DeepLinkRouter.accept(uri)
    }

    private fun extractDeepLink(payload: Map<String, String>): String? {
        val raw = payload["deeplink"]
            ?: payload["url"]
            ?: payload["route"]
            ?: return null
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        // Allow bare AppRoutes keys without leading slash from some vendors.
        return when (trimmed.lowercase()) {
            "home" -> AppRoutes.Home.HOME
            "chat" -> AppRoutes.Chat.CHAT
            "community" -> AppRoutes.Community.COMMUNITY
            "mine" -> AppRoutes.Mine.MINE
            "login", "auth/login" -> AppRoutes.Auth.LOGIN
            else -> trimmed
        }
    }
}

/**
 * Platform SDK hook. Actuals are stubs until FCM / APNs / Harmony PushKit land.
 *
 * TODO(android): FirebaseMessaging.getInstance().token
 * TODO(ios): UNUserNotificationCenter + APNs device token
 * TODO(ohos): Push Kit getToken + click Intent → [DeepLinkRouter.accept]
 */
internal expect object PlatformPushSdk {
    fun platformName(): String

    /** Null while vendor SDK is not wired. */
    fun fetchRegistrationToken(): String?
}

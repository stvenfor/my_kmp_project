package com.example.my_kmp_project.core.account

import com.example.my_kmp_project.core.network.NetworkFacade
import kotlin.concurrent.Volatile

/**
 * Cross-platform account façade for the demo skeleton.
 *
 * Session carries access/refresh tokens plus session/device ids
 * (Flutter `User` model, d7876a1) so the shared network layer can silently
 * refresh an expired access token without bouncing the user to login.
 */
public interface AccountSession {
    val isLoggedIn: Boolean
    val displayName: String?
    val token: String

    /** 静默续期凭据；空 = 无法 refresh，401 时直接失效（Flutter 同款语义）。 */
    val refreshToken: String

    /** 服务端会话 id（单设备互踢 / refresh 请求携带）。 */
    val sessionId: String

    /** 登录时绑定的设备 id（refresh 时回传）。 */
    val deviceId: String

    val userId: String?
    val avatarUrl: String?
    val phone: String?
    val email: String?
    val companyId: Long?
    val orgId: Long?
}

/**
 * DI 过渡（第二批）：真实状态与持久化在 [DefaultSessionRepository]，
 * 本对象只做逐方法转发，并实现 [SessionRepository]。
 * `repository()` 返回同一个仓库实例，供 `AppGraph` 注入。
 */
public object AccountFacade : SessionRepository by repository {

    override fun current(): AccountSession = repository.current()

    override fun setSession(value: AccountSession) {
        repository.setSession(value)
    }

    override fun logout() {
        repository.logout()
    }

    /**
     * Flutter `UserService.updateAuthTokens`：refresh 成功后原子替换三件套。
     * 不触碰资料字段；token 变更会重新绑定网络层。
     */
    override fun updateAuthTokens(token: String, refreshToken: String, sessionId: String) {
        repository.updateAuthTokens(token, refreshToken, sessionId)
    }

    /** Patch profile fields on the current logged-in session (nick / avatar). */
    override fun updateProfile(displayName: String?, avatarUrl: String?) {
        repository.updateProfile(displayName, avatarUrl)
    }

    override fun updateProfilePhone(phone: String) {
        repository.updateProfilePhone(phone)
    }

    override fun updateProfileEmail(email: String) {
        repository.updateProfileEmail(email)
    }

    override fun updateCompanyOrg(companyId: Long?, orgId: Long?) {
        repository.updateCompanyOrg(companyId, orgId)
    }

    /** DI：注入用的接口视图，与单例共享同一仓库。 */
    fun repository(): SessionRepository = repository
}

/**
 * 单例背后的真实实现：会话内存态 + 本地持久化 + 网络层 token 绑定。
 *
 * [store] 可注入（测试用内存实现）；缺省仍走 `createAccountSessionStore()`，
 * 且保持惰性创建，行为与拆分前一致。
 */
internal class DefaultSessionRepository(
    private val store: AccountSessionStore? = null,
) : SessionRepository {
    @Volatile
    private var session: AccountSession = LoggedOut

    @Volatile
    private var hydrated: Boolean = false

    private val resolvedStore: AccountSessionStore by lazy { store ?: createAccountSessionStore() }

    override fun current(): AccountSession {
        ensureHydrated()
        return session
    }

    override fun setSession(value: AccountSession) {
        ensureHydrated()
        session = value
        persist(value)
        if (value.isLoggedIn) {
            NetworkFacade.bindAccessToken(value.token, value.sessionId, value.deviceId)
        } else {
            NetworkFacade.clearAccessToken()
        }
    }

    override fun logout() {
        ensureHydrated()
        session = LoggedOut
        resolvedStore.clear()
        NetworkFacade.clearAccessToken()
    }

    override fun updateAuthTokens(token: String, refreshToken: String, sessionId: String) {
        ensureHydrated()
        val cur = session
        if (!cur.isLoggedIn) return
        setSession(
            LoggedInUser(
                displayName = cur.displayName,
                token = token,
                refreshToken = refreshToken,
                sessionId = sessionId,
                deviceId = cur.deviceId,
                userId = cur.userId,
                avatarUrl = cur.avatarUrl,
                phone = cur.phone,
                email = cur.email,
                companyId = cur.companyId,
                orgId = cur.orgId,
            ),
        )
    }

    override fun updateProfile(displayName: String?, avatarUrl: String?) {
        ensureHydrated()
        val cur = session
        if (!cur.isLoggedIn) return
        setSession(
            LoggedInUser(
                displayName = displayName ?: cur.displayName,
                token = cur.token,
                refreshToken = cur.refreshToken,
                sessionId = cur.sessionId,
                deviceId = cur.deviceId,
                userId = cur.userId,
                avatarUrl = avatarUrl ?: cur.avatarUrl,
                phone = cur.phone,
                email = cur.email,
                companyId = cur.companyId,
                orgId = cur.orgId,
            ),
        )
    }

    override fun updateProfilePhone(phone: String) {
        ensureHydrated()
        val cur = session
        if (!cur.isLoggedIn) return
        setSession(
            LoggedInUser(
                displayName = cur.displayName,
                token = cur.token,
                refreshToken = cur.refreshToken,
                sessionId = cur.sessionId,
                deviceId = cur.deviceId,
                userId = cur.userId,
                avatarUrl = cur.avatarUrl,
                phone = phone,
                email = cur.email,
                companyId = cur.companyId,
                orgId = cur.orgId,
            ),
        )
    }

    override fun updateProfileEmail(email: String) {
        ensureHydrated()
        val cur = session
        if (!cur.isLoggedIn) return
        setSession(
            LoggedInUser(
                displayName = cur.displayName,
                token = cur.token,
                refreshToken = cur.refreshToken,
                sessionId = cur.sessionId,
                deviceId = cur.deviceId,
                userId = cur.userId,
                avatarUrl = cur.avatarUrl,
                phone = cur.phone,
                email = email,
                companyId = cur.companyId,
                orgId = cur.orgId,
            ),
        )
    }

    override fun updateCompanyOrg(companyId: Long?, orgId: Long?) {
        ensureHydrated()
        val cur = session
        if (!cur.isLoggedIn) return
        if (companyId == null && orgId == null) return
        setSession(
            LoggedInUser(
                displayName = cur.displayName,
                token = cur.token,
                refreshToken = cur.refreshToken,
                sessionId = cur.sessionId,
                deviceId = cur.deviceId,
                userId = cur.userId,
                avatarUrl = cur.avatarUrl,
                phone = cur.phone,
                email = cur.email,
                companyId = companyId ?: cur.companyId,
                orgId = orgId ?: cur.orgId,
            ),
        )
    }

    private fun ensureHydrated() {
        if (hydrated) return
        val raw = resolvedStore.readUserJson()
        val snapshot = raw?.let { AccountUserSnapshot.decode(it) }
        val restored = snapshot?.toSession() ?: LoggedOut
        session = restored
        if (restored.isLoggedIn) {
            NetworkFacade.bindAccessToken(restored.token, restored.sessionId, restored.deviceId)
        }
        hydrated = true
    }

    private fun persist(value: AccountSession) {
        val snapshot = AccountUserSnapshot.fromSession(value)
        if (snapshot == null || !snapshot.hasValidToken) {
            resolvedStore.clear()
        } else {
            resolvedStore.writeUserJson(AccountUserSnapshot.encode(snapshot))
        }
    }
}

private val repository: DefaultSessionRepository = DefaultSessionRepository()

public data object LoggedOut : AccountSession {
    override val isLoggedIn: Boolean = false
    override val displayName: String? = null
    override val token: String = ""
    override val refreshToken: String = ""
    override val sessionId: String = ""
    override val deviceId: String = ""
    override val userId: String? = null
    override val avatarUrl: String? = null
    override val phone: String? = null
    override val email: String? = null
    override val companyId: Long? = null
    override val orgId: Long? = null
}

public data class LoggedInUser(
    override val displayName: String?,
    override val token: String,
    override val refreshToken: String = "",
    override val sessionId: String = "",
    override val deviceId: String = "",
    override val userId: String? = null,
    override val avatarUrl: String? = null,
    override val phone: String? = null,
    override val email: String? = null,
    override val companyId: Long? = null,
    override val orgId: Long? = null,
) : AccountSession {
    override val isLoggedIn: Boolean = token.isNotBlank()

    /** 可尝试静默续期（Flutter interceptor 的 refreshToken.isEmpty 分支对等）。 */
    val canRefresh: Boolean get() = refreshToken.isNotBlank()
}

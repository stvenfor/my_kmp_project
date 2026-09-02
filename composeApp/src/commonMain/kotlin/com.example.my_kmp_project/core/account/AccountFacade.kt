package com.example.my_kmp_project.core.account

import com.example.my_kmp_project.core.network.NetworkFacade
import kotlin.concurrent.Volatile

/**
 * Cross-platform account façade for the demo skeleton.
 */
internal interface AccountSession {
    val isLoggedIn: Boolean
    val displayName: String?
    val token: String
    val userId: String?
    val avatarUrl: String?
    val phone: String?
    val email: String?
    val companyId: Long?
    val orgId: Long?
}

internal object AccountFacade {
    @Volatile
    private var session: AccountSession = LoggedOut

    @Volatile
    private var hydrated: Boolean = false

    private val store: AccountSessionStore by lazy { createAccountSessionStore() }

    fun current(): AccountSession {
        ensureHydrated()
        return session
    }

    fun setSession(value: AccountSession) {
        ensureHydrated()
        session = value
        persist(value)
        if (value.isLoggedIn) {
            NetworkFacade.bindAccessToken(value.token)
        } else {
            NetworkFacade.clearAccessToken()
        }
    }

    fun logout() {
        ensureHydrated()
        session = LoggedOut
        store.clear()
        NetworkFacade.clearAccessToken()
    }

    /** Patch profile fields on the current logged-in session (nick / avatar). */
    fun updateProfile(displayName: String? = null, avatarUrl: String? = null) {
        ensureHydrated()
        val cur = session
        if (!cur.isLoggedIn) return
        setSession(
            LoggedInUser(
                displayName = displayName ?: cur.displayName,
                token = cur.token,
                userId = cur.userId,
                avatarUrl = avatarUrl ?: cur.avatarUrl,
                phone = cur.phone,
                email = cur.email,
                companyId = cur.companyId,
                orgId = cur.orgId,
            ),
        )
    }

    fun updateProfilePhone(phone: String) {
        ensureHydrated()
        val cur = session
        if (!cur.isLoggedIn) return
        setSession(
            LoggedInUser(
                displayName = cur.displayName,
                token = cur.token,
                userId = cur.userId,
                avatarUrl = cur.avatarUrl,
                phone = phone,
                email = cur.email,
                companyId = cur.companyId,
                orgId = cur.orgId,
            ),
        )
    }

    fun updateProfileEmail(email: String) {
        ensureHydrated()
        val cur = session
        if (!cur.isLoggedIn) return
        setSession(
            LoggedInUser(
                displayName = cur.displayName,
                token = cur.token,
                userId = cur.userId,
                avatarUrl = cur.avatarUrl,
                phone = cur.phone,
                email = email,
                companyId = cur.companyId,
                orgId = cur.orgId,
            ),
        )
    }

    fun updateCompanyOrg(companyId: Long?, orgId: Long?) {
        ensureHydrated()
        val cur = session
        if (!cur.isLoggedIn) return
        if (companyId == null && orgId == null) return
        setSession(
            LoggedInUser(
                displayName = cur.displayName,
                token = cur.token,
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
        val raw = store.readUserJson()
        val snapshot = raw?.let { AccountUserSnapshot.decode(it) }
        val restored = snapshot?.toSession() ?: LoggedOut
        session = restored
        if (restored.isLoggedIn) {
            NetworkFacade.bindAccessToken(restored.token)
        }
        hydrated = true
    }

    private fun persist(value: AccountSession) {
        val snapshot = AccountUserSnapshot.fromSession(value)
        if (snapshot == null || !snapshot.hasValidToken) {
            store.clear()
        } else {
            store.writeUserJson(AccountUserSnapshot.encode(snapshot))
        }
    }
}

internal data object LoggedOut : AccountSession {
    override val isLoggedIn: Boolean = false
    override val displayName: String? = null
    override val token: String = ""
    override val userId: String? = null
    override val avatarUrl: String? = null
    override val phone: String? = null
    override val email: String? = null
    override val companyId: Long? = null
    override val orgId: Long? = null
}

internal data class LoggedInUser(
    override val displayName: String?,
    override val token: String,
    override val userId: String? = null,
    override val avatarUrl: String? = null,
    override val phone: String? = null,
    override val email: String? = null,
    override val companyId: Long? = null,
    override val orgId: Long? = null,
) : AccountSession {
    override val isLoggedIn: Boolean = token.isNotBlank()
}

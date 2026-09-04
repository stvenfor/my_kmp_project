package com.example.my_kmp_project.core.account

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
public data class AccountUserSnapshot(
    val userId: String? = null,
    val token: String? = null,
    val nickname: String? = null,
    val avatarUrl: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val companyId: Long? = null,
    val orgId: Long? = null,
) {
    val hasValidToken: Boolean get() = !token.isNullOrBlank()

    fun toSession(): AccountSession =
        if (hasValidToken) {
            LoggedInUser(
                displayName = nickname,
                token = token.orEmpty(),
                userId = userId,
                avatarUrl = avatarUrl,
                phone = phone,
                email = email,
                companyId = companyId,
                orgId = orgId,
            )
        } else {
            LoggedOut
        }

    companion object {
        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

        fun fromSession(session: AccountSession): AccountUserSnapshot? {
            if (!session.isLoggedIn) return null
            return AccountUserSnapshot(
                userId = session.userId,
                token = session.token,
                nickname = session.displayName,
                avatarUrl = session.avatarUrl,
                phone = session.phone,
                email = session.email,
                companyId = session.companyId,
                orgId = session.orgId,
            )
        }

        fun encode(snapshot: AccountUserSnapshot): String = json.encodeToString(snapshot)

        fun decode(raw: String): AccountUserSnapshot? =
            runCatching { json.decodeFromString<AccountUserSnapshot>(raw) }.getOrNull()
    }
}

package com.example.my_kmp_project.core.account

/**
 * 会话读写契约（DI 第一批）：与 [AccountFacade] 的公共方法一一对应。
 *
 * 过渡期 [AccountFacade] 自身实现本接口，`AccountFacade.repository()` 返回同一实例；
 * 调用方从「直接引用 object」迁到「构造注入 SessionRepository」时行为不变。
 */
public interface SessionRepository {
    public fun current(): AccountSession

    public fun setSession(value: AccountSession)

    public fun logout()

    /**
     * Flutter `UserService.updateAuthTokens`：refresh 成功后原子替换三件套。
     * 不触碰资料字段；token 变更会重新绑定网络层。
     */
    public fun updateAuthTokens(token: String, refreshToken: String, sessionId: String)

    /** Patch profile fields on the current logged-in session (nick / avatar). */
    public fun updateProfile(displayName: String? = null, avatarUrl: String? = null)

    public fun updateProfilePhone(phone: String)

    public fun updateProfileEmail(email: String)

    public fun updateCompanyOrg(companyId: Long?, orgId: Long?)
}

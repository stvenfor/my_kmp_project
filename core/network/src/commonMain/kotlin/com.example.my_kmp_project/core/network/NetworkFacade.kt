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
    private var client: ApiClient = createDefaultClient()

    fun api(): ApiClient = client

    fun setTokenExpiredHandler(handler: TokenExpiredHandler?) {
        tokenExpiredHandler = handler
        rebuildClient()
    }

    fun setBusinessHandlers(handlers: NetworkBusinessHandlers?) {
        businessHandlers = handlers
        rebuildClient()
    }

    fun rebuildClient() {
        client = createDefaultClient()
    }

    fun bindAccessToken(token: String) {
        NetworkConfig.accessToken = token
        rebuildClient()
    }

    fun clearAccessToken() {
        NetworkConfig.accessToken = ""
        rebuildClient()
    }

    internal fun currentTokenExpiredHandler(): TokenExpiredHandler? = tokenExpiredHandler

    internal fun currentBusinessHandlers(): NetworkBusinessHandlers? = businessHandlers

    private fun createDefaultClient(): ApiClient =
        createPlatformApiClient(tokenExpiredHandler, businessHandlers)
}

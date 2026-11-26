package com.example.my_kmp_project.core.network

import kotlin.test.Test
import kotlin.test.assertSame

/**
 * DI 第一批：`NetworkFacade.provider()` 是单例的接口视图，
 * 多次取用必须拿到同一个 engine（同一份 client），迁移期不能出现两套网络状态。
 */
class NetworkFacadeProviderTest {

    @Test
    fun providerIsStable() {
        assertSame(NetworkFacade.provider(), NetworkFacade.provider())
    }

    @Test
    fun providerAndFacadeShareTheSameClients() {
        assertSame(NetworkFacade.api(), NetworkFacade.provider().api())
        assertSame(NetworkFacade.rawApi(), NetworkFacade.provider().rawApi())
    }
}

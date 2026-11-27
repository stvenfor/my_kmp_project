package com.example.my_kmp_project.core.network

/**
 * 网络出口契约（DI 第一批）。
 *
 * feature / graph 只依赖这个接口，不再直接引用 [NetworkFacade] 单例；
 * 过渡期 [NetworkFacade] 自身实现本接口，所以两种取法拿到的是同一份 client。
 */
public interface ApiClientProvider {
    /** 装饰后的 client（业务请求；启用 401 刷新时走刷新-重试）。 */
    public fun api(): ApiClient

    /** 未装饰 client（refresh 请求专用，避免递归触发刷新）。 */
    public fun rawApi(): ApiClient
}

package com.example.my_kmp_project.core.router

/**
 * 统一路由入口契约（路由改造第 1 批，骨架）。
 *
 * 只定义接口与默认实现，**不接线**：三个壳与 AppShell 仍直接调用旧 object，
 * 行为完全不变。后续批次把本接口放进 AppGraph，feature 通过构造注入使用。
 */
public interface AppRouter {
    public val registry: RouteRegistry

    /** 产品宿主归类（壳决定 overlay 用哪套页面）。 */
    public fun hostFor(path: String): ProductRouteHost

    /** UI 标签 / 短别名 / 绝对路径 → 可导航目标；空串返回 null。 */
    public fun resolve(raw: String): ProductRouteTarget?

    /** 深链 URI → tab / route；不支持返回 null。 */
    public fun parseDeepLink(uri: String): ParsedDeepLink?
}

/** 默认实现：全部委托现有 API，保证与旧调用点同一份行为。 */
public object DefaultAppRouter : AppRouter {

    override val registry: RouteRegistry = DefaultRouteRegistry

    override fun hostFor(path: String): ProductRouteHost = registry.hostFor(path)

    override fun resolve(raw: String): ProductRouteTarget? = ProductRouteDispatch.resolve(raw)

    override fun parseDeepLink(uri: String): ParsedDeepLink? = DeepLinkRouter.parse(uri)
}

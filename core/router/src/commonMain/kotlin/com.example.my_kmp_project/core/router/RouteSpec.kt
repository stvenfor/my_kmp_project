package com.example.my_kmp_project.core.router

/** 路由行的匹配方式：精确路径，或前缀（含前缀本身）。 */
public enum class RoutePattern {
    Exact,
    Prefix,
}

/**
 * 路由表的一行（路由改造第 1 批）。
 *
 * 同一行同时描述两件事，替代原先分散在
 * [ProductRouteDispatch.hostForPath] 与 [DeepLinkRouter.parse] 里的前缀判断：
 * - [host]：产品页归到哪个壳宿主；
 * - [tab] / [canonicalPath]：深链解析出的目标。
 *
 * [deepLink] = false 的行只参与宿主归类、不参与深链解析 —— 保住旧行为里
 * 「某些前缀可导航但深链不接受」的差异（如 `/media`、`/friend/…`、`/ai`）。
 */
public data class RouteSpec(
    /** 匹配模式：精确时是完整路径，前缀时必须带结尾 `/`（如 `/home/`）。 */
    val path: String,
    val host: ProductRouteHost,
    val tab: MainTab? = null,
    val pattern: RoutePattern = RoutePattern.Exact,
    /** 深链精确命中时返回的规范路径；前缀命中沿用输入路径，不受此字段影响。 */
    val canonicalPath: String = path,
    val deepLink: Boolean = true,
) {
    public fun matches(value: String): Boolean = when (pattern) {
        RoutePattern.Exact -> value == path
        RoutePattern.Prefix -> value.startsWith(path)
    }
}

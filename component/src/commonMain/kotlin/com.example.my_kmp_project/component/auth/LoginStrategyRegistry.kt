package com.example.my_kmp_project.component.auth

/**
 * 登录策略注册表（策略模式第 0 批）。
 *
 * 采用**显式静态注册**，不做反射 / ServiceLoader：
 * Kotlin/Native 没有可靠的服务发现机制，登录方式数量也有限，显式列表更可测试、
 * 顺序确定、能按平台裁剪。
 */
public interface LoginStrategyRegistry {

    /** 所有策略，按 order / id 稳定排序。 */
    public fun all(): List<LoginStrategy>

    /** 所有方式描述（含不可用），供 UI 决定隐藏或置灰。 */
    public fun descriptors(): List<LoginMethodDescriptor>

    /** 仅当前可用的方式。 */
    public fun available(): List<LoginMethodDescriptor>

    /** 按稳定 id 查找；未知 id 返回 null。 */
    public fun find(id: String): LoginStrategy?
}

/**
 * 默认实现：构造时校验 id 唯一，之后列表不可变。
 *
 * `availability()` 每次实时求值，因此平台 SDK 晚注册（Activity / URL scheme 就绪后）
 * 也能被下一次 `descriptors()` 看到；不要在构造时缓存可用性。
 */
public class DefaultLoginStrategyRegistry(
    strategies: List<LoginStrategy>,
) : LoginStrategyRegistry {

    private val ordered: List<LoginStrategy>
    private val byId: Map<String, LoginStrategy>

    init {
        val duplicates = strategies.groupBy { it.id }
            .filterValues { it.size > 1 }
            .keys
        require(duplicates.isEmpty()) {
            "Duplicate login strategy id(s): ${duplicates.joinToString()}"
        }
        ordered = strategies.sortedWith(compareBy({ it.order }, { it.id }))
        byId = ordered.associateBy { it.id }
    }

    override fun all(): List<LoginStrategy> = ordered

    override fun descriptors(): List<LoginMethodDescriptor> =
        ordered.map { strategy ->
            LoginMethodDescriptor(
                id = strategy.id,
                displayName = strategy.displayName,
                interactive = strategy.interactive,
                order = strategy.order,
                availability = strategy.availability(),
            )
        }

    override fun available(): List<LoginMethodDescriptor> =
        descriptors().filter { it.available }

    override fun find(id: String): LoginStrategy? = byId[id]
}

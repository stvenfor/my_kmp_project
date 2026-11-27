package com.example.my_kmp_project.component.di

import kotlin.concurrent.Volatile

/**
 * Screen 作用域（DI 第一批）：一次屏幕生命周期内的对象缓存。
 *
 * 关闭后读写一律抛 [IllegalStateException] —— 让「页面退出后还在用旧作用域」
 * 这类泄漏在测试里立刻暴露，而不是静默返回过期对象。
 */
public interface ScreenScope {
    public val isClosed: Boolean

    public fun <T : Any> put(key: String, value: T)

    public fun <T : Any> get(key: String): T?

    public fun <T : Any> require(key: String): T

    public fun close()
}

/** 默认实现：线程安全的关闭标记 + 普通 map（页面内的对象本身不要求并发写）。 */
public class DefaultScreenScope : ScreenScope {
    private val values = mutableMapOf<String, Any>()

    @Volatile
    private var closed: Boolean = false

    override val isClosed: Boolean get() = closed

    override fun <T : Any> put(key: String, value: T) {
        checkNotClosed()
        values[key] = value
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> get(key: String): T? {
        checkNotClosed()
        return values[key] as T?
    }

    override fun <T : Any> require(key: String): T =
        get<T>(key) ?: error("ScreenScope entry '$key' not found")

    override fun close() {
        closed = true
        values.clear()
    }

    private fun checkNotClosed() {
        check(!closed) { "ScreenScope is closed" }
    }
}

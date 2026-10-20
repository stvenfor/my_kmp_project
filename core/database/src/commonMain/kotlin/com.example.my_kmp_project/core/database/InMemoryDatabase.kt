package com.example.my_kmp_project.core.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/**
 * 默认内存 driver：全 target 可编译（android / ios / ohosArm64 / ohosX64）。
 *
 * 说明：为避免在 OHOS fork 上引 SQL 依赖，这里先用内存表作默认实现。并发安全留待接入
 * 平台 driver 时统一处理（当前为 demo 占位，写路径非线程安全）。
 */
class InMemoryDatabase(override val name: String) : Database {

    private val tables = mutableMapOf<String, InMemoryTable<*>>()

    @Suppress("UNCHECKED_CAST")
    override fun <T : Entity> table(id: String): Table<T> =
        tables.getOrPut(id) { InMemoryTable<T>(id) } as Table<T>

    override fun clear() {
        tables.values.forEach { it.clear() }
        tables.clear()
    }
}

/** 单张内存表：主键 map + 响应式全量快照。 */
class InMemoryTable<T : Entity>(override val id: String) : Table<T> {

    private val store = LinkedHashMap<String, T>()
    private val snapshot = MutableStateFlow<List<T>>(emptyList())

    override val all: StateFlow<List<T>> = snapshot.asStateFlow()

    private fun refresh() {
        snapshot.value = store.values.toList()
    }

    override suspend fun insert(item: T) {
        store[item.id] = item
        refresh()
    }

    override suspend fun update(item: T) = insert(item)

    override suspend fun delete(id: String) {
        store.remove(id)
        refresh()
    }

    override suspend fun findById(id: String): T? = store[id]

    override fun query(predicate: (T) -> Boolean): Flow<List<T>> =
        all.map { rows -> rows.filter(predicate) }

    internal fun clear() {
        store.clear()
        refresh()
    }
}

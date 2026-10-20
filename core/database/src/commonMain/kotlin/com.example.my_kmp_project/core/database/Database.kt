package com.example.my_kmp_project.core.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * 结构化持久化契约（core:database）。
 *
 * 与 [com.example.my_kmp_project.core.storage.KeyValueStore]（KV）互补：这里面向
 * 「带主键、可查询、可响应式观察」的实体表。默认 driver 为内存实现
 * （[InMemoryDatabase]）；平台 SQL driver 就位后经 [DatabaseProvider] 注入，接口不变。
 */
interface Database {
    /** 逻辑库名（诊断 / 多库隔离用）。 */
    val name: String

    /** 取（或懒建）一张实体表；同一 [id] 返回同一实例。 */
    fun <T : Entity> table(id: String): Table<T>

    /** 清空所有表（测试 / 登出重置用）。 */
    fun clear()
}

/** 有稳定主键的持久化实体。 */
interface Entity {
    val id: String
}

/** 单张实体表的读写 + 响应式查询面。 */
interface Table<T : Entity> {
    val id: String

    /** 全量快照，随写入更新（响应式）。 */
    val all: StateFlow<List<T>>

    /** 插入或按主键覆盖。 */
    suspend fun insert(item: T)

    /** 按主键更新（等价于 [insert] 覆盖）。 */
    suspend fun update(item: T)

    /** 按主键删除。 */
    suspend fun delete(id: String)

    /** 按主键查单条。 */
    suspend fun findById(id: String): T?

    /** 条件查询，随全量快照变化重新发射。 */
    fun query(predicate: (T) -> Boolean): Flow<List<T>>
}

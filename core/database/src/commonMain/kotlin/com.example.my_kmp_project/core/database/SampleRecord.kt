package com.example.my_kmp_project.core.database

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

/**
 * 示例实体 + 仓储：演示如何在 [Table] 之上做类型安全的 CRUD / 查询。
 * 业务 feature 依赖 `:core:database` 后，可仿此定义自己的 Entity / Dao。
 */
@Serializable
data class SampleRecord(
    override val id: String,
    val title: String,
    val body: String,
    val updatedAt: Long,
) : Entity

/** [SampleRecord] 的类型安全门面（薄封装 [Table]，隐藏泛型擦除）。 */
class SampleRecordDao(private val table: Table<SampleRecord>) {

    val all: Flow<List<SampleRecord>> get() = table.all

    suspend fun upsert(record: SampleRecord) = table.insert(record)

    suspend fun remove(id: String) = table.delete(id)

    suspend fun byId(id: String): SampleRecord? = table.findById(id)

    /** 标题或正文包含 [keyword] 的记录（忽略大小写）。 */
    fun search(keyword: String): Flow<List<SampleRecord>> {
        val q = keyword.trim()
        if (q.isEmpty()) return all
        return table.query { it.title.contains(q, ignoreCase = true) || it.body.contains(q, ignoreCase = true) }
    }
}

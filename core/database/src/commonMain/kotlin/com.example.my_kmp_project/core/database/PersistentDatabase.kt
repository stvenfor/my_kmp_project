package com.example.my_kmp_project.core.database

import com.example.my_kmp_project.core.storage.KeyValueStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

/**
 * 落盘 driver：每张表的每一行序列化成一条 JSON，写进 [KeyValueStore]。
 *
 * 三端后端（Android SharedPreferences / iOS NSUserDefaults / OHOS OH_Preferences）都落应用
 * 沙箱，因此进程重启后数据仍在。行级 key + 行级 value 让单个值保持很小，避开 Preferences
 * 的单键长度限制；表内顺序由 id 索引（一个 `StringList`）保留。
 *
 * 为什么不是 SQL：OHOS fork 当前没有 Room / SQLDelight 的 klib，强引会在 ohosArm64 /
 * ohosX64 编译期挂掉（见模块 KDoc）。改用 KV 后接口不变，将来接入 SQL driver 仍然只换
 * [DatabaseProvider] 的工厂。
 *
 * key 约定：行 = `<tableId>::<rowId>`，索引 = `<tableId>::#index`；tableId / rowId 里
 * 不要出现 `::`。
 */
class PersistentDatabase(
    private val store: KeyValueStore,
    override val name: String = "app",
    private val json: Json = DEFAULT_JSON,
) : Database {

    private val tables = mutableMapOf<String, Table<*>>()

    @Suppress("UNCHECKED_CAST")
    override fun <T : Entity> table(id: String, serializer: KSerializer<T>?): Table<T> =
        tables.getOrPut(id) {
            if (serializer == null) {
                InMemoryTable<T>(id)
            } else {
                PersistentTable(id, store, json, serializer)
            }
        } as Table<T>

    override fun clear() {
        tables.values.forEach { table ->
            when (table) {
                is InMemoryTable<*> -> table.clear()
                is PersistentTable<*> -> table.clear()
            }
        }
        tables.clear()
    }

    private companion object {
        val DEFAULT_JSON: Json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
}

/**
 * 单张落盘表：内存 map 承担读/查询，写入时同步刷 JSON 与 id 索引。
 *
 * 写路径与 [InMemoryTable] 一样不做加锁（demo 规模，单写者）；接入并发写入前需统一加锁。
 */
internal class PersistentTable<T : Entity>(
    override val id: String,
    private val store: KeyValueStore,
    private val json: Json,
    private val serializer: KSerializer<T>,
) : Table<T> {

    private val indexKey: String = "$id::#index"
    private val rows = LinkedHashMap<String, T>()
    private val snapshot = MutableStateFlow<List<T>>(emptyList())

    override val all: StateFlow<List<T>> = snapshot.asStateFlow()

    init {
        store.getStringList(indexKey).orEmpty().forEach { rowId ->
            val raw = store.getString(rowKey(rowId)) ?: return@forEach
            val item = runCatching { json.decodeFromString(serializer, raw) }.getOrNull()
                ?: return@forEach
            rows[rowId] = item
        }
        snapshot.value = rows.values.toList()
    }

    private fun rowKey(rowId: String): String = "$id::$rowId"

    private fun writeIndex() {
        store.putStringList(indexKey, rows.keys.toList())
    }

    private fun refresh() {
        snapshot.value = rows.values.toList()
    }

    override suspend fun insert(item: T) {
        rows[item.id] = item
        store.putString(rowKey(item.id), json.encodeToString(serializer, item))
        writeIndex()
        refresh()
    }

    override suspend fun update(item: T) = insert(item)

    override suspend fun delete(id: String) {
        if (rows.remove(id) == null) return
        store.remove(rowKey(id))
        writeIndex()
        refresh()
    }

    override suspend fun findById(id: String): T? = rows[id]

    override fun query(predicate: (T) -> Boolean): Flow<List<T>> =
        all.map { rows -> rows.filter(predicate) }

    internal fun clear() {
        rows.keys.toList().forEach { store.remove(rowKey(it)) }
        store.remove(indexKey)
        rows.clear()
        refresh()
    }
}

package com.example.my_kmp_project.core.database

import com.example.my_kmp_project.core.storage.InMemoryKeyValueStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 落盘 driver 契约：[InMemoryKeyValueStore] 充当进程退出后的「磁盘」，
 * 用新的 [PersistentDatabase] 实例复读同一个 store 验证数据仍在。
 */
class PersistentDatabaseTest {

    @Serializable
    private data class Note(override val id: String, val text: String) : Entity

    @Test
    fun rowsSurviveNewDatabaseInstance() = runBlocking {
        val store = InMemoryKeyValueStore()
        val first = PersistentDatabase(store, name = "test")
        val notes = first.table("notes", Note.serializer())
        notes.insert(Note("n1", "hello"))
        notes.insert(Note("n2", "world"))

        val reopened = PersistentDatabase(store, name = "test")
        val reloaded = reopened.table("notes", Note.serializer())
        assertEquals(listOf("n1", "n2"), reloaded.all.value.map { it.id })
        assertEquals("hello", reloaded.findById("n1")?.text)
    }

    @Test
    fun updateAndDeleteArePersisted() = runBlocking {
        val store = InMemoryKeyValueStore()
        val db = PersistentDatabase(store, name = "test")
        val notes = db.table("notes", Note.serializer())
        notes.insert(Note("n1", "hello"))
        notes.update(Note("n1", "edited"))
        notes.insert(Note("n2", "world"))
        notes.delete("n2")

        val reopened = PersistentDatabase(store, name = "test")
        val reloaded = reopened.table("notes", Note.serializer())
        assertEquals(listOf("edited"), reloaded.all.value.map { it.text })
        assertNull(reloaded.findById("n2"))
    }

    @Test
    fun queryFiltersAndClearDropsPersistedRows() = runBlocking {
        val store = InMemoryKeyValueStore()
        val db = PersistentDatabase(store, name = "test")
        val notes = db.table("notes", Note.serializer())
        notes.insert(Note("a", "kotlin"))
        notes.insert(Note("b", "java"))

        assertEquals(
            listOf("kotlin"),
            notes.query { it.text == "kotlin" }.first().map { it.text },
        )

        db.clear()
        assertTrue(notes.all.value.isEmpty())
        val reopened = PersistentDatabase(store, name = "test")
        assertTrue(reopened.table("notes", Note.serializer()).all.value.isEmpty())
    }

    @Test
    fun tableWithoutSerializerStaysInMemory() = runBlocking {
        val store = InMemoryKeyValueStore()
        val db = PersistentDatabase(store, name = "test")
        db.table<Note>("volatile").insert(Note("v1", "tmp"))

        val reopened = PersistentDatabase(store, name = "test")
        assertTrue(reopened.table<Note>("volatile").all.value.isEmpty())
    }
}

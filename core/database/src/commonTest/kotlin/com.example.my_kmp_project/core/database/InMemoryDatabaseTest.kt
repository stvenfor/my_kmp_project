package com.example.my_kmp_project.core.database

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/** 内存 driver 契约验证：CRUD + 响应式快照 + 条件查询。 */
class InMemoryDatabaseTest {

    private data class Note(override val id: String, val text: String) : Entity

    @Test
    fun insert_findById_update_delete() = runBlocking {
        val db = DatabaseProvider.create("test-db")
        val notes = db.table<Note>("notes")

        notes.insert(Note("n1", "hello"))
        assertEquals("hello", notes.findById("n1")?.text)

        notes.update(Note("n1", "world"))
        assertEquals("world", notes.findById("n1")?.text)

        notes.delete("n1")
        assertNull(notes.findById("n1"))
        assertTrue(notes.all.value.isEmpty())
    }

    @Test
    fun sameTableId_returnsSameInstance_and_clearResets() = runBlocking {
        val db = DatabaseProvider.create("test-db")
        val a = db.table<Note>("shared")
        val b = db.table<Note>("shared")
        assertTrue(a === b, "same table id must yield the same instance")

        a.insert(Note("x", "1"))
        assertEquals(1, b.all.value.size)
        db.clear()
        assertEquals(0, b.all.value.size)
    }

    @Test
    fun query_filtersByPredicate() = runBlocking {
        val db = DatabaseProvider.create("test-db")
        val notes = db.table<Note>("notes")
        notes.insert(Note("a", "kotlin"))
        notes.insert(Note("b", "java"))

        val result = notes.query { it.text == "kotlin" }.first()
        assertEquals(listOf("kotlin"), result.map { it.text })
    }
}

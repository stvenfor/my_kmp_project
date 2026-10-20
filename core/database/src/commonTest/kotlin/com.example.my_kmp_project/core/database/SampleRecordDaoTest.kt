package com.example.my_kmp_project.core.database

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/** [SampleRecordDao] 门面：upsert / byId / search 冒烟。 */
class SampleRecordDaoTest {

    @Test
    fun dao_upsert_byId_search() = runBlocking {
        val db = DatabaseProvider.create("test-db")
        val dao = SampleRecordDao(db.table("records"))

        dao.upsert(SampleRecord("r1", "Kotlin Tips", "coroutines & flow", updatedAt = 1L))
        dao.upsert(SampleRecord("r2", "Compose", "declarative UI", updatedAt = 2L))

        assertEquals("Kotlin Tips", dao.byId("r1")?.title)
        assertEquals(2, db.table<SampleRecord>("records").all.value.size)

        val hits = dao.search("kotlin").first()
        assertEquals(listOf("r1"), hits.map { it.id })

        dao.remove("r1")
        assertNull(dao.byId("r1"))
    }
}

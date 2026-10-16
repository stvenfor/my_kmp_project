package com.example.my_kmp_project.core.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Flutter `commons/network/lib/api/result_model.dart`（`ListData` / `PaginationModel` /
 * `PageResult.resolveHasMore`，d7876a1）的 KMP 对等测试。
 */
class ListPageTest {

    @Serializable
    private data class Row(val id: Int = 0, val title: String = "")

    private fun element(raw: String): JsonElement = Json.parseToJsonElement(raw)

    private fun rows(raw: String): ListPage<Row> = decodeListPage(element(raw))

    @Test
    fun list_key_with_total_pages() {
        val page = rows(
            """{"list":[{"id":1,"title":"a"},{"unknown":true},"raw"],"pagination":{"page":2,"size":10,"total":25,"totalPages":3}}""",
        )
        // 非 object 项（"raw"）跳过；缺字段的 object 走默认值（Dart whereType<Map> 同款宽容）
        assertEquals(2, page.items.size)
        assertEquals(0, page.items[1].id)
        assertEquals(2, page.pagination?.page)
        assertEquals(3, page.pagination?.totalPages)
        // totalPages 优先：2 < 3
        assertTrue(page.hasMore(pageSize = 10))
    }

    @Test
    fun pagination_without_total_pages_uses_page_times_size() {
        val withMore = rows("""{"items":[{"id":2}],"pagination":{"page":1,"size":10,"total":25}}""")
        val lastPage = rows("""{"items":[{"id":2}],"pagination":{"page":3,"size":10,"total":25}}""")
        assertTrue(withMore.hasMore(pageSize = 10)) // 1*10 < 25
        assertFalse(lastPage.hasMore(pageSize = 10)) // 3*10 >= 25
    }

    @Test
    fun snake_case_total_pages_is_accepted() {
        val page = rows("""{"list":[],"pagination":{"page":1,"size":10,"total":30,"total_pages":3}}""")
        assertEquals(3, page.pagination?.totalPages)
        assertTrue(page.hasMore(pageSize = 10))
    }

    @Test
    fun bare_array_has_no_pagination_and_falls_back_to_full_page() {
        val page = rows("""[{"id":7,"title":"x"},{"id":8,"title":"y"}]""")
        assertEquals(listOf(7, 8), page.items.map { it.id })
        assertNull(page.pagination)
        assertTrue(page.hasMore(pageSize = 2)) // 拿满 → 还有
        assertFalse(page.hasMore(pageSize = 3))
        assertFalse(page.hasMore(pageSize = 0)) // pageSize<=0 视为无下一页
    }

    @Test
    fun malformed_data_yields_empty_page() {
        assertEquals(emptyList(), rows(""""I am not a list"""").items)
        assertEquals(emptyList(), rows("""{"list":null}""").items)
        assertEquals(emptyList(), rows("""{"other":[{"id":1}]}""").items)
        assertTrue(rows("null").items.isEmpty())
    }

    @Test
    fun pagination_defaults_match_dart() {
        val page = rows("""{"list":[],"pagination":{}}""")
        assertEquals(1, page.pagination?.page) // page 缺省 1
        assertEquals(0, page.pagination?.size)
        assertEquals(0, page.pagination?.total)
        assertNull(page.pagination?.totalPages)
        assertFalse(page.pagination?.hasMore ?: true) // size<=0 → false
    }

    @Test
    fun query_of_drops_null_and_blank_values() {
        assertEquals(
            mapOf("page" to "1", "size" to "20"),
            queryOf("page" to "1", "size" to "20", "q" to null, "type" to ""),
        )
    }

    @Test
    fun page_query_is_one_based_and_limit_offset_is_zero_based() {
        assertEquals(
            mapOf("page" to "2", "size" to "10", "status" to "approved"),
            pageQuery(page = 2, size = 10, "status" to "approved"),
        )
        // Dart `/transactions`：offset = page * size，page 从 0 起
        assertEquals(
            mapOf("limit" to "20", "offset" to "40", "type" to "in"),
            limitOffsetQuery(page = 2, size = 20, "type" to "in"),
        )
    }
}

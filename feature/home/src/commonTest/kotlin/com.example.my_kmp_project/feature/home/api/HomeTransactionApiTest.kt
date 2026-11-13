package com.example.my_kmp_project.feature.home.api

import com.example.my_kmp_project.core.network.NetworkError
import com.example.my_kmp_project.core.network.NetworkJson
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 契约表 §2.3 #12–#13（Dart `transaction_api.dart` + `transaction_model.dart`，d7876a1）。
 *
 * 覆盖：`limit`/`offset` 方言（**page 0-based**）、可选 `type`、双信封（缺 `code` 的裸
 * `{items:[...]}` 也算成功）、`id` 缺失必须抛（不兜底 0）、详情的 `交易记录不存在`。
 */
class HomeTransactionApiTest {

    private val client = HomeRecordingApiClient()
    private val api = TransactionApi(client)

    @Test
    fun listQueryIsLimitOffsetWithZeroBasedPage() = runBlocking {
        client.stub("""{"code":0,"message":"success","data":{"list":[]}}""")

        api.fetchPage(page = 2, size = 20)
        assertEquals("/api/v1/transactions", client.last.path)
        // Dart transaction_api.dart:25-29 —— limit=size、offset=page*size（page 0-based）
        assertEquals(mapOf("limit" to "20", "offset" to "40"), client.last.query)

        api.fetchPage(page = 0, size = 7)
        assertEquals(mapOf("limit" to "7", "offset" to "0"), client.last.query)

        api.fetchPage(page = 1, size = 20, type = "income")
        assertEquals(mapOf("limit" to "20", "offset" to "20", "type" to "income"), client.last.query)

        // `type` 为 null / 空白都省略（Dart `if (type != null && type.isNotEmpty)`）
        api.fetchPage(page = 1, type = "   ")
        assertFalse(client.last.query.containsKey("type"))
    }

    @Test
    fun listEnvelopeDecodesBothKeyCasingAndDefaults() = runBlocking {
        client.stub(
            """
            {"code":0,"message":"success","data":{
              "list":[
                {"id":11,"type":"expense","category":"油费","amount":128.5,
                 "date":"2026-10-16","user_id":"u-1","note":"高速",
                 "created_at":"2026-10-16T09:00:00Z","updated_at":"2026-10-16T09:05:00Z"},
                {"id":12,"type":"income","amount":99.25,"userId":"u-2",
                 "createdAt":"2026-10-16T10:00:00Z"}
              ],
              "pagination":{"page":1,"size":20,"total":41}
            }}
            """.trimIndent(),
        )
        val page = api.fetchPage(page = 0)
        assertEquals(2, page.items.size)

        val first = page.items.first()
        assertEquals(11, first.id)
        assertEquals("expense", first.type)
        assertEquals("油费", first.category)
        assertEquals(128.5, first.amount)
        assertEquals("2026-10-16", first.date)
        assertEquals("u-1", first.userId)
        assertEquals("高速", first.note)
        assertEquals("2026-10-16T09:00:00Z", first.createdAt)
        assertEquals("2026-10-16T09:05:00Z", first.updatedAt)

        // 缺 category/note → Dart 的 `?? ''` 与「保持 null」两种语义；camelCase 别名照收
        val second = page.items[1]
        assertEquals(12, second.id)
        assertEquals("", second.category)
        assertEquals(99.25, second.amount)
        assertEquals("u-2", second.userId)
        assertNull(second.note)
        assertEquals("2026-10-16T10:00:00Z", second.createdAt)

        // Dart `ListData.resolveHasMore(20)`：有 pagination → page*size<total
        assertTrue(page.hasMore(20))
    }

    @Test
    fun bareBodyWithoutCodeIsTreatedAsSuccess() = runBlocking {
        // Dart `_parseListResult`：body 不含 `code` → 整份当 ListData 且合成 code=0（:66-70）
        client.stub("""{"items":[{"id":1,"type":"expense","amount":3.5,"date":"2026-10-01"}]}""")
        val page = api.fetchPage(page = 0)
        assertEquals(1, page.items.size)
        assertEquals(1, page.items.single().id)
        assertEquals(3.5, page.items.single().amount)
    }

    @Test
    fun bareArrayBodyAlsoDecodes() = runBlocking {
        client.stub("""[{"id":1},{"id":2}]""")
        assertEquals(listOf(1, 2), api.fetchPage(page = 0).items.map { it.id })
    }

    @Test
    fun businessFailureWithoutDataYieldsEmptyPageLikeDart() = runBlocking {
        // Dart transaction_api.dart:32-37 + transaction_repository.dart:27-31：
        // 业务失败（data 缺失）→ 空页且不报错，调用点完全不看 `code`
        client.stub("""{"code":403,"message":"无权限"}""")
        assertTrue(api.fetchPage(page = 0).items.isEmpty())
    }

    @Test
    fun rowWithoutIdFailsLoudlyInsteadOfDefaultingToZero() {
        // Dart `(json['id'] as num).toInt()`（transaction_model.dart:18）→ 整次调用抛
        client.stub("""{"code":0,"data":{"list":[{"type":"expense","amount":1}]}}""")
        val error = assertFailsWith<Exception> { runBlocking { api.fetchPage(page = 0) } }
        assertTrue(error.message?.contains("id") == true, "unexpected error: ${error.message}")
    }

    @Test
    fun transactionModelRequiresIdOnTheWire() {
        // 非空无默认值的 [TransactionModel.id]：缺字段即抛，绝不静默变 0
        assertFailsWith<Exception> {
            NetworkJson.decodeFromString(TransactionModel.serializer(), """{"type":"expense"}""")
        }
        assertEquals(
            1,
            NetworkJson.decodeFromString(TransactionModel.serializer(), """{"id":1}""").id,
        )
    }

    @Test
    fun detailDecodesEnvelopeAndBareBody() = runBlocking {
        client.stub(
            """{"code":0,"message":"success","data":{"id":7,"type":"income","category":"工资",
               "amount":1000,"date":"2026-10-01","user_id":"u-7"}}""".trimIndent(),
        )
        val item = api.getTransaction(7)
        assertEquals("/api/v1/transactions/7", client.last.path)
        assertEquals(7, item.id)
        assertEquals("工资", item.category)
        assertEquals(1000.0, item.amount)
        assertEquals("u-7", item.userId)

        // 裸 body（无 `code`）→ Dart `_parseDetailResult` 把整份 body 当模型（:73-83）
        client.stub("""{"id":8,"type":"expense","amount":2.0,"date":"2026-10-02"}""")
        assertEquals(8, api.getTransaction(8).id)
    }

    @Test
    fun detailThrowsTransactionNotFoundWhenDataMissing() {
        client.stub("""{"code":0,"message":"success","data":null}""")
        val error = assertFailsWith<NetworkError.Business> { runBlocking { api.getTransaction(9) } }
        assertEquals("交易记录不存在", error.message)
    }

    // ---- C2：PUT|DELETE /transactions/:v ----

    @Test
    fun updateUsesPutApiAndDeleteUsesDeleteApi() = runBlocking {
        val c = HomeRecordingApiClient()
        c.stub("""{"code":0,"message":"ok","data":{"id":7,"type":"expense","amount":1.0,"date":"2026-10-01"}}""")
        val txApi = TransactionApi(c)

        txApi.update(7, TransactionUpdateRequest(note = "补记"))
        assertEquals("PUT", c.last.method)
        assertEquals("/api/v1/transactions/7", c.last.path)
        // 稀疏 body：只发非 null 字段（Go entity.UpdateTransactionInput 指针语义）
        assertEquals("""{"note":"补记"}""", c.last.body)

        txApi.delete(7)
        assertEquals("DELETE", c.last.method)
        assertEquals("/api/v1/transactions/7", c.last.path)
        assertEquals("", c.last.body)
    }

    // ---- C3：带 page→manage / 不带→legacy 的双分页 ----

    @Test
    fun dualPaginationQueryShapesAreMutuallyExclusive() {
        // 带 page → page/size（manage 分页）
        val manage = transactionListQuery(managePage = 1, size = 20)
        assertEquals(mapOf("page" to "1", "size" to "20"), manage)
        assertFalse(manage.containsKey("limit"))
        assertFalse(manage.containsKey("offset"))

        // 不带 page → limit/offset（legacy，offset = legacyPage * size）
        val legacy = transactionListQuery(managePage = null, size = 20, legacyPage = 2)
        assertEquals(mapOf("limit" to "20", "offset" to "40"), legacy)
        assertFalse(legacy.containsKey("page"))
        assertFalse(legacy.containsKey("size"))
    }

    @Test
    fun fetchPageDualEmitsManageQueryForPageAndLegacyOtherwise() = runBlocking {
        val c = HomeRecordingApiClient()
        c.stub("""{"code":0,"message":"success","data":{"list":[]}}""")
        val txApi = TransactionApi(c)

        txApi.fetchPageDual(managePage = 1, size = 20)
        assertEquals("/api/v1/transactions", c.last.path)
        assertEquals(mapOf("page" to "1", "size" to "20"), c.last.query)

        txApi.fetchPageDual(managePage = null, size = 20, legacyPage = 2)
        assertEquals(mapOf("limit" to "20", "offset" to "40"), c.last.query)
    }
}

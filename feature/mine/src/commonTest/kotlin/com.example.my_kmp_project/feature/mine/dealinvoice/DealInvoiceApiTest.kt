package com.example.my_kmp_project.feature.mine.dealinvoice

import com.example.my_kmp_project.core.network.NetworkError
import com.example.my_kmp_project.core.network.NetworkJson
import com.example.my_kmp_project.feature.mine.MineRecordingApiClient
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 契约表 §2.10 #51-#56（Dart `deal_invoice_api.dart` + `deal_invoice_models.dart`，d7876a1）
 * 的 JSON fixture 测试：`invoice_id|id` 字符串化、`customer_id|id` 保持 Int、
 * `display_name|name` 兜底、状态域含 `approved_pending_rating`/`rated`、
 * `image_url` 恒发、`PATCH …/resubmit`。
 */
class DealInvoiceApiTest {

    private val client = MineRecordingApiClient()
    private val api = DealInvoiceApi(client)

    @Test
    fun summaryStatsDefaultsToZeroBlock() = runBlocking {
        client.stub(
            """
            {"code":0,"message":"ok","data":{
              "display_name":"王小明","avatar_url":"https://cdn/a.png",
              "position_label":"销售经理","store_name":"杭州一店",
              "stats":{"uploaded":4,"pending_review":1,"approved":2,"rejected":1}
            },"timestamp":1760000000}
            """.trimIndent(),
        )
        val summary = api.fetchSummary()
        assertEquals("/api/v1/deal-invoices/summary", client.last.path)
        assertEquals("杭州一店", summary.resolvedStoreName)
        assertEquals(4, summary.resolvedStats.resolvedUploaded)
        assertEquals(1, summary.resolvedStats.resolvedRejected)

        // stats 整块缺失 → Dart :151-158 的全 0 兜底
        client.stub("""{"code":0,"message":"ok","data":{"store_name":"杭州一店"}}""")
        val noStats = api.fetchSummary()
        assertEquals(DealInvoiceStats(), noStats.resolvedStats)
        assertEquals(0, noStats.resolvedStats.resolvedUploaded)
        assertEquals("杭州一店", noStats.resolvedStoreName)
    }

    @Test
    fun listSendsStatusAndDerivesHasMoreFromTotal() = runBlocking {
        client.stub(
            """
            {"code":0,"data":{
              "list":[
                {"invoice_id":"INV-1","phone":"13800138000","customer_name":"李雷",
                 "status":"approved_pending_rating","submitted_at":"2026-10-16T09:00:00Z",
                 "reject_reason":null,"rating_stars":null,"image_url":null},
                {"id":88,"phone":"13900139000","status":"rated","rating_stars":5,
                 "submitted_at":"2026-10-16T10:00:00Z"}
              ],
              "pagination":{"page":1,"size":10,"total":25}
            }}
            """.trimIndent(),
        )
        val page = api.fetchList(DealInvoiceTab.All, page = 1)
        assertEquals(mapOf("page" to "1", "size" to "10", "status" to "all"), client.last.query)
        assertTrue(page.hasMore) // 1*10 < 25
        assertEquals(2, page.items.size)

        val first = page.items[0]
        assertEquals("INV-1", first.resolvedId)
        assertEquals(DealInvoiceStatus.ApprovedPendingRating, first.resolvedStatus)
        assertEquals("已通过", first.resolvedStatus.auditLabel)
        assertEquals("13800138000 李雷", first.customerDisplay)
        assertEquals(null, first.ratingStars)

        // Dart `'${json['invoice_id'] ?? json['id'] ?? ""}'`：数字发票号也字符串化
        val second = page.items[1]
        assertEquals("88", second.resolvedId)
        assertEquals(DealInvoiceStatus.Rated, second.resolvedStatus)
        assertEquals(5, second.ratingStars)
        // customer_name 缺失 → Dart 保持 null，display 只剩电话
        assertEquals(null, second.resolvedCustomerName)
        assertEquals("13900139000", second.customerDisplay)
        assertTrue(second.matchesTab(DealInvoiceTab.Approved))
        assertFalse(second.matchesTab(DealInvoiceTab.PendingReview))

        client.stub("""{"code":0,"data":{"list":[],"pagination":{"page":3,"size":10,"total":25}}}""")
        val lastPage = api.fetchList(DealInvoiceTab.Rejected, page = 3)
        assertFalse(lastPage.hasMore) // 3*10 >= 25
        assertEquals("rejected", client.last.query["status"])
    }

    @Test
    fun detailUsesInvoiceIdInPath() = runBlocking {
        client.stub(
            """
            {"code":0,"data":{"invoice_id":"INV-7","phone":"13700137000",
              "status":"rejected","reject_reason":"图片不清晰","rating_stars":null,
              "submitted_at":"2026-10-15T18:00:00Z","image_url":"https://cdn/i.png"}}
            """.trimIndent(),
        )
        val item = api.fetchDetail("INV-7")
        assertEquals("/api/v1/deal-invoices/INV-7", client.last.path)
        assertEquals("INV-7", item.resolvedId)
        assertEquals(DealInvoiceStatus.Rejected, item.resolvedStatus)
        assertEquals("图片不清晰", item.rejectReason)
        assertEquals("https://cdn/i.png", item.imageUrl)
    }

    @Test
    fun customersResolveIdAndNameAlternatives() = runBlocking {
        client.stub(
            """
            {"code":0,"data":{"list":[
              {"customer_id":11,"display_name":"李雷","phone":"13800138000"},
              {"id":"12","name":"韩梅梅","phone":"13900139000"},
              {"phone":"13700137000"}
            ]}}
            """.trimIndent(),
        )
        val rows = api.fetchCustomers(q = " 李 ")
        assertEquals(mapOf("page" to "1", "size" to "50", "q" to "李"), client.last.query)
        // customer_id|id 是 Int（`_intOf` 还吃数字串），display_name|name 二选一
        assertEquals(listOf(11, 12, 0), rows.map { it.resolvedId })
        assertEquals(listOf("李雷", "韩梅梅", ""), rows.map { it.resolvedName })
        assertEquals("13800138000 李雷", rows[0].display)

        // 空白 q 省略（Dart :70）
        api.fetchCustomers(q = "  ")
        assertEquals(mapOf("page" to "1", "size" to "50"), client.last.query)
    }

    @Test
    fun createAlwaysSendsCustomerIdAndImageUrl() = runBlocking {
        client.stub("""{"code":0,"data":{"invoice_id":"INV-9","status":"pending_review"}}""")
        val created = api.create(customerId = 11, imageUrl = null)
        assertEquals("POST", client.last.method)
        assertEquals("/api/v1/deal-invoices", client.last.path)
        val sent = NetworkJson.parseToJsonElement(client.last.body).jsonObject
        assertEquals(setOf("customer_id", "image_url"), sent.keys)
        assertEquals(11, sent["customer_id"]!!.toString().toInt())
        assertEquals(JsonNull, sent["image_url"])
        assertEquals("INV-9", created.resolvedId)
    }

    @Test
    fun resubmitIsTheOnlyPatchAndStillSendsImageUrl() = runBlocking {
        client.stub("""{"code":0,"data":{"invoice_id":"INV-9","status":"pending_review"}}""")
        api.resubmit(invoiceId = "INV-9", imageUrl = "https://cdn/new.png")
        assertEquals("PATCH", client.last.method)
        assertEquals("/api/v1/deal-invoices/INV-9/resubmit", client.last.path)
        var sent = NetworkJson.parseToJsonElement(client.last.body).jsonObject
        assertEquals(setOf("image_url"), sent.keys)
        assertEquals("\"https://cdn/new.png\"", sent["image_url"].toString())

        api.resubmit(invoiceId = "INV-9")
        sent = NetworkJson.parseToJsonElement(client.last.body).jsonObject
        assertEquals(JsonNull, sent["image_url"])
    }

    @Test
    fun failureMessagesFollowDartFormula() = runBlocking {
        client.stub("""{"code":403,"message":"无权限","data":null}""")
        val biz = assertFailsWith<NetworkError.Business> {
            api.fetchList(DealInvoiceTab.All, page = 1)
        }
        assertEquals("无权限", biz.message)

        client.stub("""{"code":0,"message":"","data":null}""")
        val fallback = assertFailsWith<NetworkError.Business> {
            api.create(customerId = 1, imageUrl = "https://cdn/i.png")
        }
        assertEquals("提交失败", fallback.message)
    }
}

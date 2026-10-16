package com.example.my_kmp_project.feature.mine.usedcar

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
 * 契约表 §2.7 #35-#39（Dart `used_car_order_api.dart` + `used_car_order_models.dart`，d7876a1）
 * 的 JSON fixture 测试：真实 BFF 信封 `{"code":0,...}`，重点覆盖
 * `order_id` 字符串化、`stats` 缺省 `{}`、`kind` 在 all 时省略、`image_url` 恒发。
 */
class UsedCarOrderApiTest {

    private val client = MineRecordingApiClient()
    private val api = UsedCarOrderApi(client)

    @Test
    fun summaryDecodesStatsBlock() {
        client.stub(
            """
            {"code":0,"message":"ok","data":{
              "display_name":"王小明","avatar_url":"https://cdn/a.png",
              "position_label":"销售顾问","store_name":"杭州一店",
              "stats":{"submitted":12,"pending_review":3,"approved":8,"rejected":1}
            },"timestamp":1760000000}
            """.trimIndent(),
        )
        val summary = runBlocking { api.fetchSummary() }
        assertEquals("/api/v1/used-car-orders/summary", client.last.path)
        assertEquals("GET", client.last.method)
        assertEquals("王小明", summary.resolvedDisplayName)
        assertEquals(12, summary.resolvedStats.resolvedSubmitted)
        assertEquals(3, summary.resolvedStats.resolvedPendingReview)
        assertEquals(8, summary.resolvedStats.resolvedApproved)
        assertEquals(1, summary.resolvedStats.resolvedRejected)
    }

    @Test
    fun summaryStatsDefaultsToEmptyMap() {
        // Dart :18 `json['stats'] as Map? ?? const {}` ⇒ 四个计数全 0；缺字段 → ""
        client.stub("""{"code":0,"message":"","data":{"display_name":null}}""")
        val summary = runBlocking { api.fetchSummary() }
        assertEquals("", summary.resolvedDisplayName)
        assertEquals(UsedCarOrderStats(), summary.resolvedStats)
        assertEquals(0, summary.resolvedStats.resolvedSubmitted)
    }

    @Test
    fun listQuerySendsStatusAlwaysAndOmitsKindForAll() {
        client.stub(listFixture(page = 1, total = 25))
        val all = runBlocking {
            api.fetchList(UsedCarStatusTab.All, UsedCarKindFilter.All, page = 1)
        }
        assertEquals(mapOf("page" to "1", "size" to "10", "status" to "all"), client.last.query)
        assertFalse(client.last.query.containsKey("kind"))
        assertEquals(1, all.items.size)
        // Dart :47 page*size < total ⇒ 1*10 < 25
        assertTrue(all.hasMore)

        runBlocking {
            api.fetchList(UsedCarStatusTab.Approved, UsedCarKindFilter.TradeIn, page = 2, size = 10)
        }
        assertEquals(
            mapOf("page" to "2", "size" to "10", "status" to "approved", "kind" to "trade_in"),
            client.last.query,
        )
    }

    @Test
    fun listHasMoreStopsWhenPageReachesTotal() {
        client.stub(listFixture(page = 3, total = 25))
        val page = runBlocking {
            api.fetchList(UsedCarStatusTab.Rejected, UsedCarKindFilter.Consign, page = 3)
        }
        // 3*10 >= 25 → 没有下一页；kind 非 all 时带上
        assertFalse(page.hasMore)
        assertEquals("consign", client.last.query["kind"])
    }

    @Test
    fun detailStringifiesOrderIdAndKeepsDartDefaults() {
        client.stub(
            """
            {"code":0,"message":"ok","data":{
              "order_id":202601,"kind":"trade_in","kind_label":"置换",
              "customer_name":"李雷","phone":"13800138000",
              "vehicle_model":"Model Y","plate_no":"浙A12345","vin":"LRW12",
              "mileage_km":32000,"model_year":2023,"amount":128800.5,
              "status":"approved_pending_rating","submitted_at":"2026-10-16T09:12:33Z",
              "reject_reason":null,"rating_stars":null,"image_url":null
            }}
            """.trimIndent(),
        )
        val item = runBlocking { api.fetchDetail("202601") }
        assertEquals("/api/v1/used-car-orders/202601", client.last.path)
        // Dart `'${json['order_id'] ?? ""}'`：服务端数字单号在 KMP 里仍是 String
        assertEquals("202601", item.resolvedOrderId)
        assertEquals("业务金额", item.resolvedAmountLabel)
        assertEquals(128800.5, item.resolvedAmount)
        assertEquals(32000, item.resolvedMileageKm)
        assertEquals("2026-10-16T09:12:33Z", item.resolvedSubmittedAt)
        assertEquals("已通过待评价", item.statusLabel)
        assertEquals(null, item.ratingStars)
        assertEquals(null, item.imageUrl)
    }

    @Test
    fun customersIgnoresPaginationAndTrimsQuery() {
        client.stub(
            """
            {"code":0,"data":{"list":[
              {"customer_id":7,"display_name":"李雷","phone":"13800138000"},
              "垃圾行",
              {"customer_id":8,"display_name":"韩梅梅","phone":"13900139000"}
            ]}}
            """.trimIndent(),
        )
        val rows = runBlocking { api.fetchCustomers(q = "  李雷 ") }
        assertEquals(
            mapOf("page" to "1", "size" to "50", "q" to "李雷"),
            client.last.query,
        )
        assertEquals(listOf(7, 8), rows.map { it.resolvedCustomerId })
        assertEquals("李雷", rows[0].resolvedDisplayName)

        // 空白 q 不发（Dart :73）
        runBlocking { api.fetchCustomers(q = "   ") }
        assertEquals(mapOf("page" to "1", "size" to "50"), client.last.query)
    }

    @Test
    fun createAlwaysSendsImageUrlEvenWhenNull() {
        client.stub(
            """{"code":0,"data":{"order_id":99,"kind":"purchase","status":"pending_review"}}""",
        )
        val created = runBlocking {
            api.create(
                UsedCarOrderCreateRequest(
                    kind = "purchase",
                    customerId = 7,
                    vehicleModel = "Model 3",
                    plateNo = "浙A66666",
                    vin = "LRW99",
                    mileageKm = 1200,
                    modelYear = 2025,
                    amount = 99000.0,
                    imageUrl = null,
                ),
            )
        }
        assertEquals("POST", client.last.method)
        assertEquals("/api/v1/used-car-orders", client.last.path)
        val sent = NetworkJson.parseToJsonElement(client.last.body).jsonObject
        assertEquals(
            setOf(
                "kind", "customer_id", "vehicle_model", "plate_no", "vin",
                "mileage_km", "model_year", "amount", "image_url",
            ),
            sent.keys,
        )
        // NetworkJson 默认吃掉 null，这里必须显式发 null（Dart :111）
        assertEquals(JsonNull, sent["image_url"])
        assertEquals("99", created.resolvedOrderId)
        assertEquals("pending_review", created.resolvedStatus)
    }

    @Test
    fun businessCodeAndMissingDataUseDartMessages() {
        client.stub("""{"code":400,"message":"单号不存在","data":null}""")
        val biz = assertFailsWith<NetworkError.Business> {
            runBlocking { api.fetchDetail("1") }
        }
        assertEquals(400, biz.code)
        assertEquals("单号不存在", biz.message)

        // code==0 但 data 为 null：Dart `_data` 仍优先用 envelope message（:130）…
        client.stub("""{"code":0,"message":"ok","data":null}""")
        val echoed = assertFailsWith<NetworkError.Business> {
            runBlocking { api.fetchSummary() }
        }
        assertEquals("ok", echoed.message)
        assertEquals(0, echoed.code)

        // …message 为空才用调用点中文兜底
        client.stub("""{"code":0,"message":"","data":null}""")
        val empty = assertFailsWith<NetworkError.Business> {
            runBlocking { api.fetchSummary() }
        }
        assertEquals("加载摘要失败", empty.message)

        // 非 0 且 message 为空 → 「业务请求失败」（backend_response_parser.dart:46-48）
        client.stub("""{"code":500,"message":"","data":null}""")
        val blank = assertFailsWith<NetworkError.Business> {
            runBlocking { api.fetchCustomers() }
        }
        assertEquals("业务请求失败", blank.message)
    }

    private fun listFixture(page: Int, total: Int): String =
        """
        {"code":0,"message":"ok","data":{
          "list":[{
            "order_id":202601,"kind":"consign","kind_label":"专卖",
            "customer_name":"李雷","phone":"13800138000","vehicle_model":"Model Y",
            "plate_no":"浙A12345","vin":"LRW12","mileage_km":32000,"model_year":2023,
            "amount":128800.5,"status":"rated","submitted_at":"2026-10-16T09:12:33Z",
            "rating_stars":5,"image_url":"https://cdn/i.png"
          }],
          "pagination":{"page":$page,"size":10,"total":$total}
        },"timestamp":1760000000}
        """.trimIndent()
}

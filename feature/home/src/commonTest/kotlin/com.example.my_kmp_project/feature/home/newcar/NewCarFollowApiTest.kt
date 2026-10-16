package com.example.my_kmp_project.feature.home.newcar

import com.example.my_kmp_project.core.network.NetworkError
import com.example.my_kmp_project.core.network.NetworkJson
import com.example.my_kmp_project.feature.home.api.HomeRecordingApiClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * 契约表 §2.8 #40–#46（Dart `new_car_follow_api.dart` + `new_car_follow_models.dart`，d7876a1）。
 *
 * 覆盖：tab 过滤的**中文字面量**、#41/#45 的请求页 `hasMore` 方言、
 * #43 的**互斥 body 分支**（有 `customer_id>0` 时省略 `display_name`/`phone` 两键，
 * 否则两键总发且可为显式 null）、#44 的稀疏 PATCH、#46 两键不同的省略条件，
 * 以及失败时抛**裸** `Exception`（不是 `NetworkError.Business`，契约表 §2.8）。
 */
class NewCarFollowApiTest {

    private val client = HomeRecordingApiClient()
    private val api = NewCarFollowApi(client)

    /** 记录到的请求体按 JSON 键集合断言（区分「键缺失」与「键为 null」）。 */
    private fun lastBody(): JsonObject = NetworkJson.parseToJsonElement(client.last.body) as JsonObject

    private fun lastBodyKeys(): Set<String> = lastBody().keys

    @Test
    fun summaryDecodesStatsWithDartDefaults() = runBlocking {
        client.stub(
            """
            {"code":0,"message":"success","data":{
              "display_name":"张三","avatar_url":"https://cdn/a.png",
              "position_label":"销售顾问","store_name":"杭州一店",
              "stats":{"active":"7","overdue":2,"high_intent":1,"lost":3}
            }}
            """.trimIndent(),
        )
        val summary = api.fetchSummary()
        assertEquals("GET", client.last.method)
        assertEquals("/api/v1/new-car-follow-files/summary", client.last.path)
        assertEquals("张三", summary.displayName)
        assertEquals("杭州一店", summary.storeName)
        // Dart `_int` 吃数字也吃数字串（new_car_follow_models.dart:43,189-193）
        assertEquals(7, summary.stats.active)
        assertEquals(2, summary.stats.overdue)
        assertEquals(1, summary.stats.highIntent)
        assertEquals(3, summary.stats.lost)
    }

    @Test
    fun summaryTreatsMissingStatsAsAllZero() = runBlocking {
        client.stub("""{"code":0,"message":"success","data":{"display_name":"李四"}}""")
        val summary = api.fetchSummary()
        assertEquals("李四", summary.displayName)
        assertEquals("", summary.avatarUrl)
        assertEquals(0, summary.stats.active)
        assertEquals(0, summary.stats.overdue)
        assertEquals(0, summary.stats.highIntent)
        assertEquals(0, summary.stats.lost)
    }

    @Test
    fun listSendsChineseTabLiteralsAndOmitsFilterForAll() = runBlocking {
        client.stub("""{"code":0,"message":"success","data":{"list":[],"pagination":{"total":0}}}""")

        api.fetchList(NewCarFollowTab.All, page = 1)
        assertEquals(mapOf("page" to "1", "size" to "10"), client.last.query)

        api.fetchList(NewCarFollowTab.High, page = 2, size = 5)
        assertEquals(mapOf("page" to "2", "size" to "5", "intent_band" to "高"), client.last.query)

        api.fetchList(NewCarFollowTab.Medium, page = 1)
        assertEquals("中", client.last.query["intent_band"])

        api.fetchList(NewCarFollowTab.Low, page = 1)
        assertEquals("低", client.last.query["intent_band"])

        api.fetchList(NewCarFollowTab.Overdue, page = 1)
        assertEquals(mapOf("page" to "1", "size" to "10", "overdue" to "1"), client.last.query)
        assertFalse("intent_band" in client.last.query)
    }

    @Test
    fun listDecodesFileWithStringifiedIds() = runBlocking {
        // Dart `'${json['file_id'] ?? ''}'`（:108-109）：数字也会被字符串化
        client.stub(
            """
            {"code":0,"message":"success","data":{"list":[
              {"file_id":123,"customer_id":456,"customer_name":"王五","customer_phone":"13800000000",
               "follow_level":"H","intent_band":"高","stage":"following","vehicle_interest":"SUV",
               "next_follow_up_at":"2026-10-20T09:00:00Z","owner_user_id":"u1","owner_display_name":"张三"}
            ],"pagination":{"page":1,"size":10,"total":25}}}
            """.trimIndent(),
        )
        val page = api.fetchList(NewCarFollowTab.All, page = 1)
        assertEquals("/api/v1/new-car-follow-files", client.last.path)
        assertEquals(1, page.list.size)
        assertEquals("123", page.list.single().fileId)
        assertEquals("456", page.list.single().customerId)
        assertEquals("王五", page.list.single().customerName)
        assertEquals("H", page.list.single().followLevel)
        assertEquals("2026-10-20T09:00:00Z", page.list.single().nextFollowUpAt)
        // Dart 请求页方言：1 * 10 < 25（new_car_follow_api.dart:41-43）
        assertTrue(page.hasMore)

        client.stub(
            """{"code":0,"message":"success","data":{"list":[],"pagination":{"page":3,"size":10,"total":25}}}""",
        )
        assertFalse(api.fetchList(NewCarFollowTab.All, page = 3).hasMore)
    }

    @Test
    fun listHasMoreFallsBackToPageLengthWhenPaginationMissing() = runBlocking {
        // Dart `pagination?.total ?? list.length` → 3 条 / size 2 → 1*2 < 3 为真
        client.stub("""{"code":0,"message":"success","data":{"list":[{},{},{}]}}""")
        val page = api.fetchList(NewCarFollowTab.All, page = 1, size = 2)
        assertEquals(3, page.list.size)
        assertTrue(page.hasMore)
    }

    @Test
    fun listThrowsWhenDataMissingInsteadOfReturningEmptyPage() {
        // Dart `_data()`：`data` 为 null 也抛（result_model.dart:18），不能当空列表
        client.stub("""{"code":0,"message":"success","data":null}""")
        val error = assertFailsWith<Exception> { runBlocking { api.fetchList(NewCarFollowTab.All, page = 1) } }
        assertFalse(error is NetworkError.Business)
        assertEquals("success", error.message)
    }

    @Test
    fun failureThrowsBareExceptionNotBusinessError() {
        client.stub("""{"code":500,"message":"服务不可用"}""")
        val error = assertFailsWith<Exception> { runBlocking { api.fetchSummary() } }
        // 契约表 §2.8：newcar / 售后抛裸 Exception（home_todo / points 才是 HttpRequestException 对等）
        assertFalse(error is NetworkError.Business)
        assertEquals("服务不可用", error.message)

        client.stub("""{"code":0,"message":"","data":null}""")
        // 空白 message：Dart 会抛 `Exception('')`（ResultModel.message 缺省是空串），
        // core 把空白归一成 null，这里落到调用点的中文兜底（已记录的有意差异）。
        assertEquals(
            "加载详情失败",
            assertFailsWith<Exception> { runBlocking { api.fetchDetail("F-1") } }.message,
        )

        client.stub("not json at all")
        assertFailsWith<Exception> { runBlocking { api.fetchLogs("F-1") } }
    }

    @Test
    fun detailUsesStringFileIdInPath() = runBlocking {
        client.stub(
            """{"code":0,"message":"success","data":{"file_id":"F-9","customer_name":"赵六","stage":"won"}}""",
        )
        val file = api.fetchDetail("F-9")
        assertEquals("/api/v1/new-car-follow-files/F-9", client.last.path)
        assertEquals("F-9", file.fileId)
        assertEquals("won", file.stage)
        assertEquals("", file.customerPhone)
    }

    @Test
    fun createWithCustomerIdOmitsDisplayNameAndPhoneKeys() = runBlocking {
        client.stub("""{"code":0,"message":"success","data":{"file_id":"F-1","follow_level":"A"}}""")
        val file = api.create(
            followLevel = "A",
            customerId = 88,
            displayName = "不该出现",
            phone = "13800000000",
            vehicleInterest = "  轿车  ",
            nextFollowUpAt = "2026-10-22T10:00:00Z",
        )
        assertEquals("POST", client.last.method)
        assertEquals("/api/v1/new-car-follow-files", client.last.path)
        assertEquals("F-1", file.fileId)

        val body = lastBody()
        // 有 customer_id>0 时，display_name / phone 两个键根本不出现在 body 里
        assertEquals(setOf("follow_level", "customer_id", "vehicle_interest", "next_follow_up_at"), body.keys)
        assertEquals(JsonPrimitive("A"), body["follow_level"])
        assertEquals(JsonPrimitive(88), body["customer_id"])
        // trim 后非空才发，且发的是 trim 值（new_car_follow_api.dart:77-79）
        assertEquals(JsonPrimitive("轿车"), body["vehicle_interest"])
        assertEquals(JsonPrimitive("2026-10-22T10:00:00Z"), body["next_follow_up_at"])
    }

    @Test
    fun createWithoutCustomerSendsBothNullableKeysExplicitly() = runBlocking {
        client.stub("""{"code":0,"message":"success","data":{"file_id":"F-2"}}""")
        api.create(followLevel = "B", customerId = 0, displayName = null, phone = null)
        // 互斥分支的另一侧：两键**总发**，null 也显式发（new_car_follow_api.dart:71-75，
        // Dart 把 null 直接放进 map，jsonEncode 序列化为 JSON null）
        assertEquals(setOf("follow_level", "display_name", "phone"), lastBodyKeys())
        assertTrue(lastBody().containsKey("display_name"))
        assertEquals("null", lastBody()["display_name"].toString())
        assertEquals("null", lastBody()["phone"].toString())
        // 未传（null）的 vehicle_interest 与空串 next_follow_up_at 都省略（:77-82）
        assertFalse("vehicle_interest" in lastBodyKeys())
        assertFalse("next_follow_up_at" in lastBodyKeys())

        api.create(followLevel = "E", displayName = "散客", phone = "", vehicleInterest = "   ")
        // Dart `if (vehicleInterest != null && vehicleInterest.trim().isNotEmpty)`
        // （new_car_follow_api.dart:77-79）：纯空白 trim 后**整键省略**，与 null 分支同路；
        // 发送值是 trim 后的（:78），空白串两者都落不进 body。
        assertEquals(setOf("follow_level", "display_name", "phone"), lastBodyKeys())
        assertFalse("vehicle_interest" in lastBodyKeys())
        assertEquals(JsonPrimitive("散客"), lastBody()["display_name"])
        assertEquals(JsonPrimitive(""), lastBody()["phone"])
    }

    @Test
    fun patchSendsSparseBodyWithMethodPatch() = runBlocking {
        client.stub("""{"code":0,"message":"success","data":{"file_id":"F-3","stage":"test_drive"}}""")

        api.patch("F-3", stage = "test_drive")
        assertEquals("PATCH", client.last.method)
        assertEquals("/api/v1/new-car-follow-files/F-3", client.last.path)
        // null 键整键省略（Dart 的 `if (x != null)` :100-103；NetworkJson.explicitNulls = false 等价）
        assertEquals(setOf("stage"), lastBodyKeys())

        api.patch("F-3", followLevel = "H", nextFollowUpAt = "")
        assertEquals(setOf("follow_level", "next_follow_up_at"), lastBodyKeys())
        assertEquals(JsonPrimitive(""), lastBody()["next_follow_up_at"])

        api.patch("F-3")
        assertEquals(emptySet<String>(), lastBodyKeys())
    }

    @Test
    fun logsAreOneBasedAndDecodeInterpolatedIdFields() = runBlocking {
        client.stub(
            """
            {"code":0,"message":"success","data":{"items":[
              {"log_id":7,"file_id":123,"author_user_id":"u1","author_display_name":"张三",
               "body":"到店试驾","follow_level":"H","intent_band":"高",
               "next_follow_up_at":null,"created_at":1760000000}
            ],"pagination":{"page":1,"size":20,"total":7}}}
            """.trimIndent(),
        )
        val page = api.fetchLogs("123")
        assertEquals("GET", client.last.method)
        assertEquals("/api/v1/new-car-follow-files/123/logs", client.last.path)
        // Dart fetchLogs 的 page 默认就是 1（:123）
        assertEquals(mapOf("page" to "1", "size" to "20"), client.last.query)
        val log = page.list.single()
        assertEquals("7", log.logId)
        assertEquals("123", log.fileId)
        assertEquals("到店试驾", log.body)
        assertEquals("1760000000", log.createdAt)
        // `next_follow_up_at` 是原样保留的可空串（new_car_follow_models.dart:155）
        assertEquals(null, log.nextFollowUpAt)
        assertFalse(page.hasMore)
    }

    @Test
    fun createLogOmitsEmptyFollowLevelButSendsEmptyNextFollowUpAt() = runBlocking {
        client.stub(
            """{"code":0,"message":"success","data":{"log_id":"9","body":"电话跟进","follow_level":"A"}}""",
        )
        val log = api.createLog("F-4", body = "电话跟进", followLevel = "", nextFollowUpAt = "")
        assertEquals("POST", client.last.method)
        assertEquals("/api/v1/new-car-follow-files/F-4/logs", client.last.path)
        assertEquals("9", log.logId)
        assertEquals("A", log.followLevel)
        // follow_level 判非空串（:148-150），next_follow_up_at 只判 != null（:153）→ 空串也发
        assertEquals(setOf("body", "next_follow_up_at"), lastBodyKeys())
        assertEquals(JsonPrimitive(""), lastBody()["next_follow_up_at"])

        api.createLog("F-4", body = "到店", followLevel = "H", nextFollowUpAt = "2026-10-30T09:00:00Z")
        assertEquals(setOf("body", "follow_level", "next_follow_up_at"), lastBodyKeys())
        assertEquals(JsonPrimitive("H"), lastBody()["follow_level"])

        api.createLog("F-4", body = "只记一笔")
        assertEquals(setOf("body"), lastBodyKeys())
    }
}

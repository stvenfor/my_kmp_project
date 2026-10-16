package com.example.my_kmp_project.feature.home.aftersales

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
 * 契约表 §2.9 #47–#50（Dart `after_sales_api.dart` + `after_sales_models.dart`，d7876a1）。
 *
 * 覆盖：#47 的 `data` **裸 map** 读法（`can_create` 必须保住，它 gate「新建工单」入口）、
 * `hasMore` 的 `page*size < (total ?? list.length)` 方言（`total` 缺键回 null，
 * 与 newcar 的 `PaginationModel ?? 0` 不同）、#49 的「总发键 + 条件发键」，
 * #50 日期按 `'T'` 截断（JSON null → 字面量 `"null"`），以及失败抛**裸** `Exception`。
 */
class AfterSalesApiTest {

    private val client = HomeRecordingApiClient()
    private val api = AfterSalesApi(client)

    /** 记录到的请求体按 JSON 键集合断言（区分「键缺失」与「键为显式 null」）。 */
    private fun lastBody(): JsonObject = NetworkJson.parseToJsonElement(client.last.body) as JsonObject

    private fun lastBodyKeys(): Set<String> = lastBody().keys

    @Test
    fun recordsReadRawMapAndPreserveCanCreate() = runBlocking {
        client.stub(
            """
            {"code":0,"message":"success","data":{
              "list":[
                {"record_id":11,"store_id":3,"appointment_id":null,"customer_name":"张三","customer_phone":"13800000000",
                 "service_kind":1,"service_kind_label":"保养","title":"常规保养","content":"",
                 "service_date":"2026-10-16","plate_no":"浙A12345","mileage":"12000"},
                {"record_id":"12","appointment_id":9,"service_kind":0,"title":"维修","mileage":null}
              ],
              "pagination":{"page":1,"size":10,"total":25},
              "can_create":true
            }}
            """.trimIndent(),
        )
        val result = api.fetchRecords(page = 1)
        assertEquals("GET", client.last.method)
        assertEquals("/api/v1/after-sales/records", client.last.path)
        assertEquals(mapOf("page" to "1", "size" to "10"), client.last.query)
        assertEquals(2, result.list.size)
        // 1 基于服务端的 page：1 * 10 < 25
        assertTrue(result.hasMore)
        assertTrue(result.canCreate)

        val first = result.list[0]
        assertEquals(11, first.recordId)
        assertEquals(3, first.storeId)
        assertEquals(null, first.appointmentId)
        assertEquals("张三", first.customerName)
        // 读侧 service_kind 是整数（Dart `_int`），写侧却是字符串 —— 两侧不对称
        assertEquals(1, first.serviceKind)
        assertEquals("保养", first.serviceKindLabel)
        assertEquals("", first.content)
        // Dart `_int` 吃数字串（after_sales_models.dart:87-91）
        assertEquals(12000, first.mileage)

        val second = result.list[1]
        assertEquals(12, second.recordId)
        assertEquals(9, second.appointmentId)
        assertEquals(0, second.serviceKind)
        assertEquals(null, second.mileage)
        // Dart `?? ''`：缺失字符串是空串
        assertEquals("", second.customerName)
    }

    @Test
    fun recordsCanCreateRequiresLiteralTrueAndGatesEntry() = runBlocking {
        client.stub("""{"code":0,"message":"success","data":{"list":[],"can_create":"true"}}""")
        val result = api.fetchRecords(page = 1)
        // Dart `data['can_create'] == true`：字符串 "true" 不算放开入口
        assertFalse(result.canCreate)
        assertEquals(0, result.list.size)

        client.stub("""{"code":0,"message":"success","data":{"list":[],"can_create":false}}""")
        assertFalse(api.fetchRecords(page = 1).canCreate)

        client.stub("""{"code":0,"message":"success","data":{"list":[]}}""")
        assertFalse(api.fetchRecords(page = 1).canCreate)
    }

    @Test
    fun recordsHasMoreFallsBackToPageLengthWhenTotalKeyMissing() = runBlocking {
        // Dart `_asInt(pagination?['total']) ?? list.length`（after_sales_api.dart:29）：
        // pagination 在但没有 total → 回 null → 用本页条数兜底
        client.stub("""{"code":0,"message":"success","data":{"list":[{},{},{}],"pagination":{},"can_create":true}}""")
        val result = api.fetchRecords(page = 1, size = 2)
        assertEquals(3, result.list.size)
        assertTrue(result.hasMore)

        client.stub(
            """{"code":0,"message":"success","data":{"list":[],"pagination":{"total":25},"can_create":true}}""",
        )
        val last = api.fetchRecords(page = 3, size = 10)
        assertFalse(last.hasMore)
        assertTrue(last.canCreate)
    }

    @Test
    fun detailUsesIntRecordIdInPath() = runBlocking {
        client.stub(
            """{"code":0,"message":"success","data":{"record_id":42,"title":"钣喷","service_date":"2026-10-01"}}""",
        )
        val record = api.fetchDetail(42)
        assertEquals("/api/v1/after-sales/records/42", client.last.path)
        assertEquals(42, record.recordId)
        assertEquals("钣喷", record.title)
        assertEquals(0, record.storeId)
        assertEquals("", record.plateNo)
    }

    @Test
    fun createAlwaysSendsFiveKeysIncludingExplicitNulls() = runBlocking {
        client.stub("""{"code":0,"message":"success","data":{"record_id":7,"title":"保养"}}""")
        val record = api.create(serviceKind = "1", title = "保养", serviceDate = "2026-10-16")
        assertEquals("POST", client.last.method)
        assertEquals("/api/v1/after-sales/records", client.last.path)
        assertEquals(7, record.recordId)

        // 总发键：service_kind（字符串）/ title / service_date / customer_name / customer_phone
        assertEquals(
            setOf("service_kind", "title", "service_date", "customer_name", "customer_phone"),
            lastBodyKeys(),
        )
        assertEquals(JsonPrimitive("1"), lastBody()["service_kind"])
        // Dart 把 null 直接放进 map（:66-70）→ 两键仍存在且是显式 null
        assertTrue(lastBody().containsKey("customer_name"))
        assertEquals("null", lastBody()["customer_name"].toString())
        assertEquals("null", lastBody()["customer_phone"].toString())
        // 条件键全缺
        assertFalse("appointment_id" in lastBodyKeys())
        assertFalse("plate_no" in lastBodyKeys())
        assertFalse("mileage" in lastBodyKeys())
        assertFalse("content" in lastBodyKeys())
    }

    @Test
    fun createSendsConditionalKeysWithTrimmedValues() = runBlocking {
        client.stub("""{"code":0,"message":"success","data":{"record_id":8}}""")
        api.create(
            serviceKind = "0",
            title = "维修",
            serviceDate = "2026-10-17",
            customerName = "李四",
            customerPhone = "13900000000",
            appointmentId = 5,
            plateNo = "  浙A12345  ",
            mileage = 0,
            content = "  更换机油  ",
        )
        assertEquals(
            setOf(
                "service_kind", "title", "service_date", "customer_name", "customer_phone",
                "appointment_id", "plate_no", "mileage", "content",
            ),
            lastBodyKeys(),
        )
        // plate_no / content 发 trim 后的值（:72-79）；mileage 只要非 null 就发，0 也发
        assertEquals(JsonPrimitive("浙A12345"), lastBody()["plate_no"])
        assertEquals(JsonPrimitive("更换机油"), lastBody()["content"])
        assertEquals(JsonPrimitive(5), lastBody()["appointment_id"])
        assertEquals(JsonPrimitive(0), lastBody()["mileage"])

        // 空白 plate_no / content 整键省略
        api.create(
            serviceKind = "1", title = "保养", serviceDate = "2026-10-18",
            plateNo = "   ", content = "", mileage = null, appointmentId = null,
        )
        assertEquals(
            setOf("service_kind", "title", "service_date", "customer_name", "customer_phone"),
            lastBodyKeys(),
        )
    }

    @Test
    fun pendingAppointmentsTruncateDateAtLetterT() = runBlocking {
        client.stub(
            """
            {"code":0,"message":"success","data":{"list":[
              {"appointment_id":1,"customer_name":"张三","appointment_date":"2026-10-20T09:30:00Z"},
              {"appointment_id":"2","customer_name":"李四","appointment_date":"2026-10-21"},
              {"appointment_id":3,"appointment_date":null},
              {"appointment_id":4,"appointment_date":1760000000}
            ]}}
            """.trimIndent(),
        )
        val list = api.fetchPendingAppointments()
        assertEquals("GET", client.last.method)
        assertEquals("/api/v1/after-sales/pending-appointments", client.last.path)
        assertEquals(4, list.size)
        // 字符串按 'T' 截断（after_sales_models.dart:63-66）
        assertEquals("2026-10-20", list[0].appointmentDate)
        assertEquals(1, list[0].appointmentId)
        assertEquals("张三", list[0].customerName)
        // 没有 T 就原样；数字时间戳走 Dart 的 '$dateRaw' 插值（isLenient 已把数字读成字面量）
        assertEquals("2026-10-21", list[1].appointmentDate)
        assertEquals(2, list[1].appointmentId)
        // Dart `(json['customer_name'] as String?) ?? ''`（after_sales_models.dart:69）：
        // 键存在且为字符串就读原值，缺键才是空串
        assertEquals("李四", list[1].customerName)
        // bug-for-bug：JSON null 在 Dart 里是 `'$null'` → 字符串 "null"
        assertEquals("null", list[2].appointmentDate)
        assertEquals(3, list[2].appointmentId)
        assertEquals("1760000000", list[3].appointmentDate)
    }

    @Test
    fun pendingAppointmentsReturnEmptyListWhenListKeyMissing() = runBlocking {
        client.stub("""{"code":0,"message":"success","data":{}}""")
        assertTrue(api.fetchPendingAppointments().isEmpty())
    }

    @Test
    fun failuresThrowBareExceptionWithEnvelopeMessage() {
        client.stub("""{"code":500,"message":"服务不可用"}""")
        val error = assertFailsWith<Exception> { runBlocking { api.fetchRecords(page = 1) } }
        // 契约表 §2.9：售后抛裸 Exception，不是 HttpRequestException 对等的 NetworkError.Business
        assertFalse(error is NetworkError.Business)
        assertEquals("服务不可用", error.message)

        client.stub("""{"code":0,"message":"","data":null}""")
        // 空白 message：Dart 抛 `Exception('')`，core 把空白归一成 null 后落到中文兜底
        assertEquals("加载详情失败", assertFailsWith<Exception> { runBlocking { api.fetchDetail(1) } }.message)

        client.stub("""{"code":0,"message":"success","data":null}""")
        assertEquals("success", assertFailsWith<Exception> { runBlocking { api.create(serviceKind = "1", title = "t", serviceDate = "d") } }.message)

        client.stub("""{"code":0,"message":"success","data":null}""")
        assertEquals("success", assertFailsWith<Exception> { runBlocking { api.fetchPendingAppointments() } }.message)
    }
}

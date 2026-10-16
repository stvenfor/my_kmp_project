package com.example.my_kmp_project.feature.home.api

import com.example.my_kmp_project.core.network.NetworkError
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 契约表 §2.4 #15–#21（Dart `home_todo_api.dart` + `home_todo_models.dart`，d7876a1）。
 *
 * 覆盖：B 型「`data.items[]` 或裸数组」、卡片尺寸由 `type` **客户端**推导（ADR 0009）、
 * `approve` / `reject` 是**无 body 的动作 POST**（契约表 §5.12），以及 Dart `_data()`
 * 的「message 优先、空则中文兜底」抛错文案。
 */
class HomeTodoApiTest {

    private val client = HomeRecordingApiClient()
    private val api = HomeTodoApi(client)

    @Test
    fun todoCardsReadItemsArrayAndDeriveSizeFromType() = runBlocking {
        client.stub(
            """
            {"code":0,"message":"success","data":{"items":[
              {"type":"partner_pending","title":"入店申请","subtitle":"3 条待确认",
               "action_route":"/home/join-applications","count":3,"image_url":null},
              {"type":"follow_up_customer","title":"待跟进","subtitle":"5 位客户","count":5},
              {"type":"after_sales_appointment","title":"售后预约","subtitle":"今天 2 单","count":2},
              {"type":"order_pending_review","title":"店务审核","subtitle":"1 单","count":1}
            ]}}
            """.trimIndent(),
        )
        val cards = api.fetchTodoCards()
        assertEquals("/api/v1/home/todo-cards", client.last.path)
        assertTrue(client.last.query.isEmpty())
        assertEquals(4, cards.size)

        // ADR 0009：尺寸只由 type 推导，服务端不下发
        assertEquals(HomeTodoCardSize.Large, cards[0].size)
        assertEquals(4, cards[0].size.cells)
        assertEquals(HomeTodoCardSize.Medium, cards[1].size)
        assertEquals(HomeTodoCardSize.Medium, cards[2].size)
        assertEquals(HomeTodoCardSize.Small, cards[3].size)
        // Dart `HomeTodoSize` 的 cells：small(1) / medium(2) / large(4)（home_todo_models.dart:3-5）
        // ⇒ `order_pending_review` 走 default 分支（:17-18）拿 small，cells 是 **1**
        assertEquals(1, cards[3].size.cells)
        assertEquals(2, cards[1].size.cells)

        assertEquals("partner_pending", cards[0].type)
        assertEquals(3, cards[0].count)
        assertNull(cards[0].imageUrl)
        // Dart `json['action_label']?.toString() ?? '去查看'`（home_todo_models.dart:49）
        assertEquals("去查看", cards[0].actionLabel)
        assertEquals("/home/join-applications", cards[0].actionRoute)
    }

    @Test
    fun todoCardsAlsoAcceptBareArrayData() = runBlocking {
        client.stub("""{"code":0,"message":"success","data":[{"type":"partner_pending","count":2}]}""")
        val cards = api.fetchTodoCards()
        assertEquals(1, cards.size)
        assertEquals(HomeTodoCardSize.Large, cards.single().size)
        assertEquals("", cards.single().title)
        assertEquals(2, cards.single().count)
    }

    @Test
    fun joinApplicationsReadItemsShapeAndParseCreatedAt() = runBlocking {
        client.stub(
            """
            {"code":0,"message":"success","data":{"items":[
              {"application_id":9,"store_id":3,"applicant_user_id":"u-9","applicant_name":" 李雷 ",
               "status":0,"created_at":"2026-10-16T09:30:00Z"},
              {"application_id":10,"store_id":3,"applicant_user_id":"u-10","applicant_name":"",
               "status":1,"created_at":"not-a-date"}
            ]}}
            """.trimIndent(),
        )
        val rows = api.fetchJoinApplications()
        assertEquals("/api/v1/home/join-applications", client.last.path)
        assertEquals(2, rows.size)

        assertEquals(9, rows[0].applicationId)
        assertEquals(3, rows[0].storeId)
        assertEquals("u-9", rows[0].applicantUserId)
        assertEquals(0, rows[0].status)
        // Dart `_parseDateTime` → DateTime?；KMP 保留可解析的 ISO 串
        assertEquals("2026-10-16T09:30:00Z", rows[0].createdAt)
        // applicant_name trim 后为空 → 回落 applicant_user_id（:93-94）
        assertEquals("李雷", rows[0].displayName)
        assertEquals("u-10", rows[1].displayName)
        assertNull(rows[1].createdAt)
    }

    @Test
    fun approveAndRejectAreBodylessActionPosts() = runBlocking {
        client.stub("""{"code":0,"message":"success","data":null}""")

        api.approveJoin(42)
        assertEquals("POST", client.last.method)
        assertEquals("/api/v1/home/join-applications/42/approve", client.last.path)
        // 契约表 §5.12：动作 POST 真的发出（空 body），不是省略请求
        assertEquals("", client.last.body)

        api.rejectJoin(43)
        assertEquals("POST", client.last.method)
        assertEquals("/api/v1/home/join-applications/43/reject", client.last.path)
        assertEquals("", client.last.body)
    }

    @Test
    fun approveFailureUsesEnvelopeMessageThenChineseFallback() {
        client.stub("""{"code":403,"message":"无权限操作该申请"}""")
        val error = assertFailsWith<NetworkError.Business> { runBlocking { api.approveJoin(1) } }
        assertEquals(403, error.code)
        assertEquals("无权限操作该申请", error.message)

        client.stub("""{"code":500,"message":""}""")
        assertEquals("拒绝失败", assertFailsWith<NetworkError.Business> { runBlocking { api.rejectJoin(1) } }.message)
    }

    @Test
    fun followUpCustomersAppointmentsAndReviewOrdersApplyDartDefaults() = runBlocking {
        client.stub("""{"code":0,"data":{"items":[{"customer_id":5,"next_follow_up_at":"2026-10-17"}]}}""")
        val customers = api.fetchFollowUpCustomers()
        assertEquals("/api/v1/home/follow-up-customers", client.last.path)
        assertEquals(5, customers.single().customerId)
        assertEquals("客户", customers.single().displayName)
        assertEquals("2026-10-17", customers.single().nextFollowUpAt)

        client.stub("""{"code":0,"data":{"items":[{"appointment_id":6,"status":2}]}}""")
        val appointments = api.fetchAppointments()
        assertEquals("/api/v1/home/after-sales-appointments", client.last.path)
        assertEquals(6, appointments.single().appointmentId)
        assertEquals("预约客户", appointments.single().customerName)
        assertEquals(2, appointments.single().status)
        assertNull(appointments.single().appointmentDate)

        client.stub("""{"code":0,"data":{"items":[{"order_id":7,"created_at":"2026-10-16 08:00"}]}}""")
        val orders = api.fetchReviewOrders()
        assertEquals("/api/v1/home/store-review-orders", client.last.path)
        assertEquals(7, orders.single().orderId)
        assertEquals("审核单", orders.single().title)
        assertEquals("2026-10-16 08:00", orders.single().createdAt)
    }

    @Test
    fun listFailureThrowsWithFallbackCopyPerEndpoint() {
        client.stub("""{"code":500,"message":"后端炸了"}""")
        assertEquals(
            "后端炸了",
            assertFailsWith<NetworkError.Business> { runBlocking { api.fetchFollowUpCustomers() } }.message,
        )

        // Dart `_data()`（home_todo_api.dart:137）取 `(model?.message.isNotEmpty ?? false) ? model!.message : fallback`
        // ⇒ 服务端 message **非空就赢过**中文兜底，`code:0` 但 `data` 缺失时也一样，这里拿到 "success"。
        client.stub("""{"code":0,"message":"success","data":null}""")
        assertEquals(
            "success",
            assertFailsWith<NetworkError.Business> { runBlocking { api.fetchReviewOrders() } }.message,
        )

        // 想覆盖调用点的中文兜底，信封必须**不带** message
        // （Dart `ResultModel.fromJson` 缺省成空串，result_model.dart:17 ⇒ 三元落到 fallback）。
        client.stub("""{"code":0,"data":null}""")
        assertEquals(
            "加载店务审核单失败",
            assertFailsWith<NetworkError.Business> { runBlocking { api.fetchReviewOrders() } }.message,
        )
        assertEquals(
            "加载售后预约失败",
            assertFailsWith<NetworkError.Business> { runBlocking { api.fetchAppointments() } }.message,
        )
        assertEquals(
            "加载入店申请失败",
            assertFailsWith<NetworkError.Business> { runBlocking { api.fetchJoinApplications() } }.message,
        )
        assertEquals(
            "加载待办卡失败",
            assertFailsWith<NetworkError.Business> { runBlocking { api.fetchTodoCards() } }.message,
        )
    }
}

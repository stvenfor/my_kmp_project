package com.example.my_kmp_project.feature.home

import com.example.my_kmp_project.feature.home.api.HomeRecordingApiClient
import com.example.my_kmp_project.feature.home.api.PointsApi
import kotlinx.coroutines.runBlocking
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HomeTodoPackerTest {
    private fun card(type: String) = HomeTodoCard(
        type = type,
        title = type,
        subtitle = "",
        actionLabel = "去",
        actionRoute = "/",
        count = 1,
    )

    @Test
    fun wrapOnly_twoSmall() {
        val cards = listOf(card("order_pending_review"), card("order_pending_review"))
        assertTrue(HomeTodoPacker.shouldWrapOnly(cards))
        assertEquals(1, HomeTodoPacker.packPages(cards).size)
    }

    @Test
    fun largeAloneOnPage() {
        val pages = HomeTodoPacker.packPages(listOf(card("partner_pending")))
        assertEquals(1, pages.size)
        assertEquals("partner_pending", pages.first().single().type)
    }

    @Test
    fun largeThenMediumsSeparatePages() {
        val pages = HomeTodoPacker.packPages(
            listOf(
                card("partner_pending"),
                card("follow_up_customer"),
                card("after_sales_appointment"),
            ),
        )
        assertEquals(2, pages.size)
        assertEquals("partner_pending", pages[0].single().type)
        assertEquals(2, pages[1].size)
    }

    @Test
    fun mediumPlusTwoSmallOnePage() {
        val pages = HomeTodoPacker.packPages(
            listOf(
                card("follow_up_customer"),
                card("order_pending_review"),
                card("order_pending_review"),
            ),
        )
        assertEquals(1, pages.size)
        assertEquals(3, pages.first().size)
    }

    @Test
    fun fourSmallOnePage_notWrapOnly() {
        val pages = HomeTodoPacker.packPages(
            List(4) { card("order_pending_review") },
        )
        assertEquals(1, pages.size)
        assertFalse(HomeTodoPacker.shouldWrapOnly(pages.first()))
    }
}

class HomeTodoStoreTest {
    @BeforeTest
    fun reset() {
        HomeTodoStore.resetForTests()
    }

    /** 无 seed：reset 后内存为空，数据一律等远端 refresh。 */
    @Test
    fun emptyUntilRemoteRefresh() {
        assertTrue(HomeTodoStore.todoCards().isEmpty())
        assertTrue(HomeTodoStore.pendingApplications().isEmpty())
        assertTrue(HomeTodoStore.followUpCustomers().isEmpty())
        assertTrue(HomeTodoStore.afterSalesAppointments().isEmpty())
        assertTrue(HomeTodoStore.reviewOrders().isEmpty())
        assertNull(HomeTodoStore.errorMessage)
    }
}

class HomePointsStoreTest {
    @BeforeTest
    fun reset() {
        HomePointsStore.resetForTests()
    }

    @Test
    fun shouldShowDialog_mirrorsFlutter() {
        assertFalse(
            HomePointsStore.shouldShowDialog(
                isLoggedIn = false,
                checkedInToday = false,
                ackDate = null,
                todayLocal = "2026-09-29",
            ),
        )
        assertFalse(
            HomePointsStore.shouldShowDialog(
                isLoggedIn = true,
                checkedInToday = true,
                ackDate = null,
                todayLocal = "2026-09-29",
            ),
        )
        assertFalse(
            HomePointsStore.shouldShowDialog(
                isLoggedIn = true,
                checkedInToday = false,
                ackDate = "2026-09-29",
                todayLocal = "2026-09-29",
            ),
        )
        assertTrue(
            HomePointsStore.shouldShowDialog(
                isLoggedIn = true,
                checkedInToday = false,
                ackDate = null,
                todayLocal = "2026-09-29",
            ),
        )
        assertTrue(
            HomePointsStore.shouldShowDialog(
                isLoggedIn = true,
                checkedInToday = false,
                ackDate = "2026-09-28",
                todayLocal = "2026-09-29",
            ),
        )
    }

    @Test
    fun checkInOncePerDay() = runBlocking {
        val client = HomeRecordingApiClient()
        client.stub(
            """{"code":0,"message":"success","data":{"points":5,"balance":5,"streak":1,"day":"2026-09-29"}}""",
        )
        val api = PointsApi(client)
        val first = HomePointsStore.checkIn(api)
        assertNotNull(first)
        assertEquals(5, first.points)
        assertTrue(HomePointsStore.checkedInToday)
        assertEquals(5, HomePointsStore.balance)
        assertNull(HomePointsStore.checkIn(api))
    }

    @Test
    fun dialogAckPersists() {
        HomePointsStore.markDialogAcked("2026-09-29")
        assertEquals("2026-09-29", HomePointsStore.dialogAckDate)
    }
}

class HomeSecondaryStoreTest {
    @BeforeTest
    fun reset() {
        HomeSecondaryStore.resetForTests()
    }

    @Test
    fun usedCarValidate() {
        assertEquals(
            "请选择客户",
            HomeSecondaryStore.validateUsedCarCreate("", "车", "牌", "VIN", 1, 2020, 100),
        )
        assertEquals(
            "请完整填写车况与金额",
            HomeSecondaryStore.validateUsedCarCreate("张", "车", "牌", "", 1, 2020, 100),
        )
        assertNull(
            HomeSecondaryStore.validateUsedCarCreate("张三", "帝豪", "京A1", "VIN12345678901234", 1000, 2021, 80000),
        )
    }

    @Test
    fun afterSalesAndNewCarValidate() {
        assertEquals(
            "请填写客户姓名和手机号",
            HomeSecondaryStore.validateAfterSalesCreate("", "1", "t", "p", "c"),
        )
        assertEquals(
            "请填写客户姓名和手机号",
            HomeSecondaryStore.validateNewCarFollowCreate("", "1"),
        )
        assertNull(HomeSecondaryStore.validateNewCarFollowCreate("李", "138"))
    }

    /** 空态：内存缓存不预置任何假数据。 */
    @Test
    fun emptyUntilRemoteRefresh() {
        assertTrue(HomeSecondaryStore.usedCarOrders().isEmpty())
        assertTrue(HomeSecondaryStore.afterSalesDetails().isEmpty())
        assertTrue(HomeSecondaryStore.newCarFollows().isEmpty())
        assertNull(HomeSecondaryStore.findUsedCar(null))
    }

    @Test
    fun newCarPureMappings() {
        assertEquals("A", newCarFollowLevel("高"))
        assertEquals("B", newCarFollowLevel("中"))
        assertEquals("E", newCarFollowLevel("低"))
        assertEquals("H", newCarFollowLevel("H"))
        assertEquals("试驾", newCarStageLabel("test_drive"))
        assertEquals("战败", newCarStageLabel("lost"))
        assertTrue(newCarFollowOverdue("2026-09-01T09:00:00Z", "following", "2026-09-29"))
        assertFalse(newCarFollowOverdue("2026-09-01T09:00:00Z", "won", "2026-09-29"))
        assertFalse(newCarFollowOverdue(null, "following", "2026-09-29"))
        assertEquals("置换", usedCarKindLabel("trade_in"))
    }
}

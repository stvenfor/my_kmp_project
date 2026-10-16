package com.example.my_kmp_project.feature.home

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

    @Test
    fun approveAndRejectReducePending() {
        assertEquals(3, HomeTodoStore.pendingApplications().size)
        assertTrue(HomeTodoStore.approveJoin(1001))
        assertEquals(2, HomeTodoStore.pendingApplications().size)
        assertTrue(HomeTodoStore.rejectJoin(1002))
        assertEquals(1, HomeTodoStore.pendingApplications().size)
        assertFalse(HomeTodoStore.approveJoin(1001)) // already approved
    }

    @Test
    fun partnerCardCountTracksPending() {
        val before = HomeTodoStore.todoCards().first { it.type == "partner_pending" }.count
        assertEquals(3, before)
        HomeTodoStore.approveJoin(1001)
        val after = HomeTodoStore.todoCards().first { it.type == "partner_pending" }.count
        assertEquals(2, after)
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
    fun checkInOncePerDay() {
        val first = HomePointsStore.checkIn()
        assertNotNull(first)
        assertEquals(5, first.points)
        assertTrue(HomePointsStore.checkedInToday)
        assertEquals(5, HomePointsStore.balance)
        assertNull(HomePointsStore.checkIn())
    }

    @Test
    fun dialogAckPersists() {
        HomePointsStore.markDialogAcked("2026-09-29")
        assertEquals("2026-09-29", HomePointsStore.dialogAckDate)
    }
}

class HomeSecondaryStoreTest {
    @Test
    fun usedCarValidateAndCreate() {
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
        val before = HomeSecondaryStore.usedCarOrders().size
        val row = HomeSecondaryStore.createUsedCar(
            kindLabel = "置换",
            customerName = "张三 138",
            vehicleModel = "帝豪",
            plateNo = "京A1",
            vin = "VIN12345678901234",
            mileageKm = 1000,
            modelYear = 2021,
            amount = 80000,
        )
        assertEquals(before + 1, HomeSecondaryStore.usedCarOrders().size)
        assertEquals(row.id, HomeSecondaryStore.selectedUsedCarId)
        assertEquals(row, HomeSecondaryStore.findUsedCar(row.id))
    }

    @Test
    fun afterSalesAndNewCarCreate() {
        assertEquals(
            "请填写客户姓名和手机号",
            HomeSecondaryStore.validateAfterSalesCreate("", "1", "t", "p", "c"),
        )
        val asBefore = HomeSecondaryStore.afterSalesDetails().size
        val asRow = HomeSecondaryStore.createAfterSales(
            name = "陈",
            phone = "139",
            title = "工单新",
            plate = "京A",
            mileageKm = 100,
            date = "2026-09-29",
            content = "保养",
        )
        assertEquals(asBefore + 1, HomeSecondaryStore.afterSalesDetails().size)
        assertEquals(asRow.id, HomeSecondaryStore.selectedAfterSalesId)

        assertEquals(
            "请填写客户姓名和手机号",
            HomeSecondaryStore.validateNewCarFollowCreate("", "1"),
        )
        val ncBefore = HomeSecondaryStore.newCarFollows().size
        val nc = HomeSecondaryStore.createNewCarFollow("李", "138", "银河", "高")
        assertEquals(ncBefore + 1, HomeSecondaryStore.newCarFollows().size)
        assertEquals(nc.id, HomeSecondaryStore.selectedNewCarFollowId)
    }
}

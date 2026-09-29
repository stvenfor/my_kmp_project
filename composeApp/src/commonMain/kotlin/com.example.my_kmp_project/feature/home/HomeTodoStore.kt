package com.example.my_kmp_project.feature.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

/**
 * Process-wide home todo / partner-pending mock — mirrors Flutter HomeTodoApi
 * seed + approveJoin / rejectJoin.
 */
internal object HomeTodoStore {
    var version by mutableIntStateOf(0)
        private set

    private fun bump() {
        version++
    }

    private val applications = mutableListOf(
        HomeTodoJoinApplication(
            applicationId = 1001,
            storeId = 1,
            applicantUserId = "u_zhao",
            applicantName = "赵倩",
            status = 0,
            createdAtLabel = "昨天申请",
            roleLabel = "销售顾问",
        ),
        HomeTodoJoinApplication(
            applicationId = 1002,
            storeId = 1,
            applicantUserId = "u_sun",
            applicantName = "孙浩",
            status = 0,
            createdAtLabel = "今天申请",
            roleLabel = "售后技师",
        ),
        HomeTodoJoinApplication(
            applicationId = 1003,
            storeId = 1,
            applicantUserId = "u_wang",
            applicantName = "王小明",
            status = 0,
            createdAtLabel = "前天申请",
            roleLabel = "网销",
        ),
    )

    fun pendingApplications(): List<HomeTodoJoinApplication> =
        applications.filter { it.isPending }

    fun approveJoin(applicationId: Int): Boolean {
        val idx = applications.indexOfFirst { it.applicationId == applicationId && it.isPending }
        if (idx < 0) return false
        applications[idx] = applications[idx].copy(status = 1)
        bump()
        return true
    }

    fun rejectJoin(applicationId: Int): Boolean {
        val idx = applications.indexOfFirst { it.applicationId == applicationId && it.isPending }
        if (idx < 0) return false
        applications[idx] = applications[idx].copy(status = 2)
        bump()
        return true
    }

    /** Dashboard strip cards — count for partner_pending tracks pending list. */
    fun todoCards(): List<HomeTodoCard> {
        val pending = pendingApplications().size
        return listOf(
            HomeTodoCard(
                type = "partner_pending",
                title = "新伙伴待确认",
                subtitle = if (pending == 0) "暂无待审申请" else "$pending 位新成员等待审核",
                actionLabel = "去处理",
                actionRoute = HomeRoutes.TodoPartner,
                count = pending,
            ),
            HomeTodoCard(
                type = "follow_up_customer",
                title = "待跟进客户",
                subtitle = "今日 5 位意向客户",
                actionLabel = "去查看",
                actionRoute = HomeRoutes.TodoFollowUp,
                count = 5,
            ),
            HomeTodoCard(
                type = "order_pending_review",
                title = "订单待审核",
                subtitle = "2 笔新车订单",
                actionLabel = "去处理",
                actionRoute = HomeRoutes.TodoOrderReview,
                count = 2,
            ),
            HomeTodoCard(
                type = "after_sales_appointment",
                title = "售后预约",
                subtitle = "4 位客户今日到店",
                actionLabel = "去查看",
                actionRoute = HomeRoutes.TodoAfterSales,
                count = 4,
            ),
        )
    }

    /** Test helper — restore seed. */
    fun resetForTests() {
        applications.clear()
        applications += listOf(
            HomeTodoJoinApplication(1001, 1, "u_zhao", "赵倩", 0, "昨天申请", "销售顾问"),
            HomeTodoJoinApplication(1002, 1, "u_sun", "孙浩", 0, "今天申请", "售后技师"),
            HomeTodoJoinApplication(1003, 1, "u_wang", "王小明", 0, "前天申请", "网销"),
        )
        bump()
    }
}

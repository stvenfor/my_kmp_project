package com.example.my_kmp_project.feature.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.my_kmp_project.feature.home.api.HomeTodoApi
import com.example.my_kmp_project.feature.home.api.HomeTodoAppointment
import com.example.my_kmp_project.feature.home.api.HomeTodoCard as ApiHomeTodoCard
import com.example.my_kmp_project.feature.home.api.HomeTodoFollowUpCustomer
import com.example.my_kmp_project.feature.home.api.HomeTodoJoinApplication as ApiJoinApplication
import com.example.my_kmp_project.feature.home.api.HomeTodoReviewOrder

/** 待跟进客户展示行（`GET /home/follow-up-customers`）。 */
data class HomeTodoFollowUpRow(
    val id: Int,
    val name: String,
    val nextFollowUp: String?,
)

/** 售后预约展示行（`GET /home/after-sales-appointments`）。 */
data class HomeTodoAppointmentRow(
    val id: Int,
    val customerName: String,
    val appointmentDate: String?,
)

/** 店务审核单展示行（`GET /home/store-review-orders`）。 */
data class HomeTodoReviewRow(
    val id: Int,
    val title: String,
    val statusLabel: String,
)

/**
 * 首页待办 / 入店申请 / 跟进客户 / 售后预约 / 审核单：远端 [HomeTodoApi] 缓存。
 *
 * 审批走 `POST /home/join-applications/:id/approve|reject`，成功后重新拉取列表；
 * 网络失败只写 [errorMessage]，绝不本地伪造成功。
 */
object HomeTodoStore {
    var version by mutableIntStateOf(0)
        private set

    private fun bump() {
        version++
    }

    var loading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    private var cardItems by mutableStateOf<List<HomeTodoCard>>(emptyList())
    private var applicationItems by mutableStateOf<List<HomeTodoJoinApplication>>(emptyList())
    private var followUpItems by mutableStateOf<List<HomeTodoFollowUpRow>>(emptyList())
    private var appointmentItems by mutableStateOf<List<HomeTodoAppointmentRow>>(emptyList())
    private var reviewItems by mutableStateOf<List<HomeTodoReviewRow>>(emptyList())

    /** 待办卡片（远端全量，尺寸由 [HomeTodoCard.size] 推导）。 */
    fun todoCards(): List<HomeTodoCard> = cardItems

    /** 待审入店申请（远端列表里的 pending 子集）。 */
    fun pendingApplications(): List<HomeTodoJoinApplication> =
        applicationItems.filter { it.isPending }

    fun followUpCustomers(): List<HomeTodoFollowUpRow> = followUpItems

    fun afterSalesAppointments(): List<HomeTodoAppointmentRow> = appointmentItems

    fun reviewOrders(): List<HomeTodoReviewRow> = reviewItems

    /**
     * 拉取首页待办全量数据；每个端点独立降级（如审核单仅管理员可见，
     * 403 时只记录 [errorMessage]，其它列表照常展示）。
     */
    suspend fun refresh(api: HomeTodoApi = HomeTodoApi()): Result<Unit> {
        loading = true
        var firstError: String? = null
        suspend fun <T> fetch(block: suspend () -> T): T? = try {
            block()
        } catch (e: Exception) {
            if (firstError == null) firstError = e.message ?: "加载待办失败"
            null
        }
        val cards = fetch { api.fetchTodoCards() }
        val applications = fetch { api.fetchJoinApplications() }
        val followUps = fetch { api.fetchFollowUpCustomers() }
        val appointments = fetch { api.fetchAppointments() }
        val reviews = fetch { api.fetchReviewOrders() }
        cards?.let { list -> cardItems = list.map { it.toUiCard() } }
        applications?.let { list -> applicationItems = list.map { it.toUiApplication() } }
        followUps?.let { list -> followUpItems = list.map { it.toUiFollowUp() } }
        appointments?.let { list -> appointmentItems = list.map { it.toUiAppointment() } }
        reviews?.let { list -> reviewItems = list.map { it.toUiReview() } }
        errorMessage = firstError
        loading = false
        bump()
        val error = firstError
        return if (error == null) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException(error))
        }
    }

    /** 确认入店；成功后重新拉取列表，失败返回 false 并记录 [errorMessage]。 */
    suspend fun approveJoin(applicationId: Int, api: HomeTodoApi = HomeTodoApi()): Boolean =
        try {
            api.approveJoin(applicationId)
            refresh(api)
            true
        } catch (e: Exception) {
            errorMessage = e.message ?: "确认失败"
            bump()
            false
        }

    /** 拒绝入店；语义同 [approveJoin]。 */
    suspend fun rejectJoin(applicationId: Int, api: HomeTodoApi = HomeTodoApi()): Boolean =
        try {
            api.rejectJoin(applicationId)
            refresh(api)
            true
        } catch (e: Exception) {
            errorMessage = e.message ?: "拒绝失败"
            bump()
            false
        }

    /** Test helper — 只清空内存缓存。 */
    fun resetForTests() {
        cardItems = emptyList()
        applicationItems = emptyList()
        followUpItems = emptyList()
        appointmentItems = emptyList()
        reviewItems = emptyList()
        loading = false
        errorMessage = null
        bump()
    }
}

private fun ApiHomeTodoCard.toUiCard(): HomeTodoCard = HomeTodoCard(
    type = type,
    title = title,
    subtitle = subtitle,
    actionLabel = actionLabel,
    actionRoute = actionRoute,
    count = count,
    imageUrl = imageUrl?.takeIf { it.isNotBlank() },
)

private fun ApiJoinApplication.toUiApplication(): HomeTodoJoinApplication = HomeTodoJoinApplication(
    applicationId = applicationId,
    storeId = storeId,
    applicantUserId = applicantUserId,
    applicantName = displayName,
    status = status,
    createdAtLabel = createdAt?.take(10)?.takeIf { it.isNotBlank() } ?: "近期申请",
)

private fun HomeTodoFollowUpCustomer.toUiFollowUp(): HomeTodoFollowUpRow = HomeTodoFollowUpRow(
    id = customerId,
    name = displayName,
    nextFollowUp = nextFollowUpAt,
)

private fun HomeTodoAppointment.toUiAppointment(): HomeTodoAppointmentRow = HomeTodoAppointmentRow(
    id = appointmentId,
    customerName = customerName,
    appointmentDate = appointmentDate,
)

private fun HomeTodoReviewOrder.toUiReview(): HomeTodoReviewRow = HomeTodoReviewRow(
    id = orderId,
    title = title,
    statusLabel = when (status) {
        0 -> "待审核"
        1 -> "已通过"
        2 -> "未通过"
        else -> "状态 $status"
    },
)

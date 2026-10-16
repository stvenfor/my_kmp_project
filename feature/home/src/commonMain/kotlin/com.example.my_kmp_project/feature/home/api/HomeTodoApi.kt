package com.example.my_kmp_project.feature.home.api

import com.example.my_kmp_project.core.network.ApiClient
import com.example.my_kmp_project.core.network.NetworkFacade
import com.example.my_kmp_project.core.network.postApi

/**
 * Flutter `HomeTodoApi`（`features/home/lib/home/api/home_todo_api.dart`，d7876a1）。
 * 契约表 #15–#21，**全部需要 Bearer**（`AuthHttpConfig.ensureInitialized()`，:110）。
 *
 * 形状（契约表 §3 B 型）：`data.items[]` **或**裸数组；core 的 `decodeListPage` 还额外接受
 * `data.list[]`，是 Dart `_parseItems`（:114-132）的宽容超集。
 *
 * 失败语义与 Dart 一致：非 `code:0` 或 `data` 缺失 → 抛 [com.example.my_kmp_project.core.network.NetworkError.Business]
 * （`HttpRequestException` 对等，中文文案见各调用点）。
 */
public class HomeTodoApi(
    private val client: ApiClient = NetworkFacade.api(),
) {
    public companion object {
        /** Dart `HomeTodoApi.todoCardsPath`（home_todo_api.dart:7）。 */
        internal const val TODO_CARDS_PATH: String = "/api/v1/home/todo-cards"

        /** Dart `HomeTodoApi.joinApplicationsPath`（:8）。 */
        internal const val JOIN_APPLICATIONS_PATH: String = "/api/v1/home/join-applications"

        /** Dart `HomeTodoApi.followUpPath`（:9）。 */
        internal const val FOLLOW_UP_PATH: String = "/api/v1/home/follow-up-customers"

        /** Dart `HomeTodoApi.appointmentsPath`（:10）。 */
        internal const val APPOINTMENTS_PATH: String = "/api/v1/home/after-sales-appointments"

        /** Dart `HomeTodoApi.reviewOrdersPath`（:11）。 */
        internal const val REVIEW_ORDERS_PATH: String = "/api/v1/home/store-review-orders"
    }

    /** #15 `GET /home/todo-cards` —— 卡片尺寸由 [HomeTodoCard.size] 客户端推导。 */
    public suspend fun fetchTodoCards(): List<HomeTodoCard> =
        client.getHomeListPage<HomeTodoCard>(TODO_CARDS_PATH)
            .homeDataOrThrow("加载待办卡失败")
            .items

    /** #16 `GET /home/join-applications`（入店申请）。 */
    public suspend fun fetchJoinApplications(): List<HomeTodoJoinApplication> =
        client.getHomeListPage<HomeTodoJoinApplication>(JOIN_APPLICATIONS_PATH)
            .homeDataOrThrow("加载入店申请失败")
            .items

    /**
     * #17 `POST /home/join-applications/:applicationId/approve` ——
     * **无 body 的动作 POST**（Dart `post()` 不传 `data`），只看 `code == 0`，`data` 忽略（:41-52）。
     */
    public suspend fun approveJoin(applicationId: Int): Unit =
        client.postActionOk("$JOIN_APPLICATIONS_PATH/$applicationId/approve", "确认失败")

    /** #18 `POST /home/join-applications/:applicationId/reject` —— 同样无 body（:54-65）。 */
    public suspend fun rejectJoin(applicationId: Int): Unit =
        client.postActionOk("$JOIN_APPLICATIONS_PATH/$applicationId/reject", "拒绝失败")

    /** #19 `GET /home/follow-up-customers`（待跟进客户）。 */
    public suspend fun fetchFollowUpCustomers(): List<HomeTodoFollowUpCustomer> =
        client.getHomeListPage<HomeTodoFollowUpCustomer>(FOLLOW_UP_PATH)
            .homeDataOrThrow("加载待跟进客户失败")
            .items

    /** #20 `GET /home/after-sales-appointments`（售后预约）。 */
    public suspend fun fetchAppointments(): List<HomeTodoAppointment> =
        client.getHomeListPage<HomeTodoAppointment>(APPOINTMENTS_PATH)
            .homeDataOrThrow("加载售后预约失败")
            .items

    /** #21 `GET /home/store-review-orders`（店务审核单）。 */
    public suspend fun fetchReviewOrders(): List<HomeTodoReviewOrder> =
        client.getHomeListPage<HomeTodoReviewOrder>(REVIEW_ORDERS_PATH)
            .homeDataOrThrow("加载店务审核单失败")
            .items
}

/**
 * 无 body 的动作 POST（契约表 §5.12：必须真的发空 body，而不是省略请求）。
 *
 * `body = ""` 时 `KtorApiClient.postApi` 不调 `setBody`，与 Dart
 * `HttpManager.post(path)`（`data` 为 null）等价；返回值只用于判 `code`。
 */
suspend fun ApiClient.postActionOk(path: String, fallback: String): Unit {
    postApi<Unit>(path, body = "") { Unit }.homeOkOrThrow(fallback)
}

package com.example.my_kmp_project.feature.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.my_kmp_project.feature.home.api.CheckInDayView
import com.example.my_kmp_project.feature.home.api.GrowthTask
import com.example.my_kmp_project.feature.home.api.PointsApi
import com.example.my_kmp_project.feature.home.api.PointsStatus

data class HomePointsStatus(
    val balance: Int,
    val checkedInToday: Boolean,
    val streak: Int,
    val todayReward: Int,
)

data class HomeCheckInResult(
    val points: Int,
    val streak: Int,
    val balance: Int,
)

/**
 * 签到状态缓存 + 真实 `PointsApi` 读写（`GET /points/status` / `POST /points/check-in`）。
 * 弹窗确认日期仍按本地日历日记录（`check_in_dialog_ack_date`）。
 */
object HomePointsStore {
    const val PrefsKey = "check_in_dialog_ack_date"
    const val DefaultTodayReward = 5

    var version by mutableIntStateOf(0)
        private set

    private fun bump() {
        version++
    }

    var balance by mutableIntStateOf(0)
        private set
    var streak by mutableIntStateOf(0)
        private set
    var checkedInToday by mutableStateOf(false)
        private set
    var todayReward by mutableIntStateOf(DefaultTodayReward)
        private set

    /** 服务端签到日历（最近 N 天）。 */
    var calendar by mutableStateOf<List<CheckInDayView>>(emptyList())
        private set

    /** 成长任务（`GET /points/tasks`）。 */
    var tasks by mutableStateOf<List<GrowthTask>>(emptyList())
        private set

    var loading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    /** Last local date string when dialog was acked (check-in or dismiss). */
    var dialogAckDate by mutableStateOf<String?>(null)
        private set

    fun todayLocalString(
        year: Int,
        month: Int,
        day: Int,
    ): String {
        val y = year.toString().padStart(4, '0')
        val m = month.toString().padStart(2, '0')
        val d = day.toString().padStart(2, '0')
        return "$y-$m-$d"
    }

    /** Pure logic — Flutter `DailyCheckInDialog.shouldShow`. */
    fun shouldShowDialog(
        isLoggedIn: Boolean,
        checkedInToday: Boolean,
        ackDate: String?,
        todayLocal: String,
    ): Boolean {
        if (!isLoggedIn) return false
        if (checkedInToday) return false
        if (ackDate == todayLocal) return false
        return true
    }

    fun status(): HomePointsStatus = HomePointsStatus(
        balance = balance,
        checkedInToday = checkedInToday,
        streak = streak,
        todayReward = todayReward,
    )

    fun markDialogAcked(todayLocal: String) {
        dialogAckDate = todayLocal
        bump()
    }

    /** 拉取签到状态 + 成长任务；失败只记录 [errorMessage]，保留上一次展示值。 */
    suspend fun refresh(api: PointsApi = PointsApi()): Result<Unit> = runCatching {
        loading = true
        applyStatus(api.fetchStatus())
        tasks = api.fetchTasks()
    }.onFailure {
        errorMessage = it.message ?: "加载签到状态失败"
    }.also {
        loading = false
    }

    /** 线上签到；已签或失败返回 null。 */
    suspend fun checkIn(api: PointsApi = PointsApi()): HomeCheckInResult? {
        if (checkedInToday) return null
        val result = runCatching { api.checkIn() }.getOrElse {
            errorMessage = it.message ?: "签到失败"
            return null
        }
        balance = result.balance
        streak = result.streak
        checkedInToday = true
        bump()
        return HomeCheckInResult(points = result.points, streak = result.streak, balance = result.balance)
    }

    /**
     * 领取成长任务奖励（`POST /points/tasks/:code/claim`）；成功后刷新任务列表。
     * 返回本次获得的积分，失败返回 null。
     */
    suspend fun claimTask(code: String, api: PointsApi = PointsApi()): Int? {
        val result = runCatching { api.claimTask(code) }.getOrElse {
            errorMessage = it.message ?: "领取失败"
            return null
        }
        balance = result.balance
        tasks = runCatching { api.fetchTasks() }.getOrDefault(tasks)
        bump()
        return result.points
    }

    fun applyStatus(status: PointsStatus) {
        balance = status.balance
        streak = status.streak
        checkedInToday = status.checkedInToday
        todayReward = status.todayReward
        calendar = status.calendar
        errorMessage = null
        bump()
    }

    fun resetForTests() {
        balance = 0
        streak = 0
        checkedInToday = false
        todayReward = DefaultTodayReward
        calendar = emptyList()
        tasks = emptyList()
        dialogAckDate = null
        loading = false
        errorMessage = null
        bump()
    }
}

package com.example.my_kmp_project.feature.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

internal data class HomePointsStatus(
    val balance: Int,
    val checkedInToday: Boolean,
    val streak: Int,
    val todayReward: Int,
)

internal data class HomeCheckInResult(
    val points: Int,
    val streak: Int,
    val balance: Int,
)

/**
 * Flutter `DailyCheckInDialog` + mock PointsApi state machine (in-process).
 * Dialog ack is keyed by local calendar day (`check_in_dialog_ack_date`).
 */
internal object HomePointsStore {
    const val PrefsKey = "check_in_dialog_ack_date"
    const val DefaultTodayReward = 5

    var version by mutableIntStateOf(0)
        private set

    private fun bump() {
        version++
    }

    var balance by mutableIntStateOf(0)
        private set
    var streak by mutableIntStateOf(1)
        private set
    var checkedInToday by mutableStateOf(false)
        private set
    var todayReward by mutableIntStateOf(DefaultTodayReward)
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

    /**
     * Check in once per process-day. Returns null if already checked in.
     * Dialog reward on home uses [todayReward]; mall SoT historically showed +5.
     */
    fun checkIn(): HomeCheckInResult? {
        if (checkedInToday) return null
        val gained = todayReward
        balance += gained
        streak += 1
        checkedInToday = true
        bump()
        return HomeCheckInResult(points = gained, streak = streak, balance = balance)
    }

    fun resetForTests() {
        balance = 0
        streak = 1
        checkedInToday = false
        todayReward = DefaultTodayReward
        dialogAckDate = null
        bump()
    }
}

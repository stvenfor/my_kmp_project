package com.example.my_kmp_project.core.platform

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * KV lives in :core:storage now (see commonMain StringListStore.kt); only the local-date
 * helper is platform-specific.
 */
@OptIn(ExperimentalTime::class)
actual fun platformTodayYmd(): String {
    // Approximate local day via UTC until an OHOS calendar API is wired.
    val ms = Clock.System.now().toEpochMilliseconds()
    val days = ms / 86_400_000L
    var y = 1970
    var rem = days.toInt()
    while (true) {
        val diy = if (y % 4 == 0 && (y % 100 != 0 || y % 400 == 0)) 366 else 365
        if (rem < diy) break
        rem -= diy
        y++
    }
    val leap = y % 4 == 0 && (y % 100 != 0 || y % 400 == 0)
    val md = intArrayOf(31, if (leap) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    var m = 1
    for (d in md) {
        if (rem < d) break
        rem -= d
        m++
    }
    val day = rem + 1
    return "$y-${m.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
}

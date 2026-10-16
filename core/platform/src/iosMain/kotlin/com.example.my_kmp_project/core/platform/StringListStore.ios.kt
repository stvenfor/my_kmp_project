package com.example.my_kmp_project.core.platform

/**
 * KV lives in :core:storage now (see commonMain StringListStore.kt); only the local-date
 * helper is platform-specific.
 */
actual fun platformTodayYmd(): String {
    val formatter = platform.Foundation.NSDateFormatter()
    formatter.dateFormat = "yyyy-MM-dd"
    return formatter.stringFromDate(platform.Foundation.NSDate())
}

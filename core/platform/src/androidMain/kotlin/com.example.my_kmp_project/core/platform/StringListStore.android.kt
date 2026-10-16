package com.example.my_kmp_project.core.platform

/**
 * KV lives in :core:storage now (see commonMain StringListStore.kt); only the local-date
 * helper is platform-specific.
 */
actual fun platformTodayYmd(): String =
    java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())

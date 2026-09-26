package com.example.my_kmp_project.core.platform

import android.content.Context
import com.example.my_kmp_project.core.account.AndroidAccountContext

internal actual fun loadStringList(key: String): List<String>? {
    val ctx = AndroidAccountContext.applicationContext ?: return null
    val prefs = ctx.getSharedPreferences("home_favorites", Context.MODE_PRIVATE)
    if (!prefs.contains(key)) return null
    val raw = prefs.getString(key, null) ?: return emptyList()
    if (raw.isEmpty()) return emptyList()
    return raw.split('\u001f').filter { it.isNotEmpty() }
}

internal actual fun saveStringList(key: String, values: List<String>) {
    val ctx = AndroidAccountContext.applicationContext ?: return
    val prefs = ctx.getSharedPreferences("home_favorites", Context.MODE_PRIVATE)
    prefs.edit().putString(key, values.joinToString("\u001f")).apply()
}

internal actual fun platformTodayYmd(): String =
    java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())

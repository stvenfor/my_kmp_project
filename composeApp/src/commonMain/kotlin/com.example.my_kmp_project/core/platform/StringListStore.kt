package com.example.my_kmp_project.core.platform

/** String-list KV for Flutter SpUtils-style favorites (key = home_favorite_service_ids_v2). */
internal expect fun loadStringList(key: String): List<String>?

internal expect fun saveStringList(key: String, values: List<String>)

/** Scalar string KV (Flutter SpUtils.getString / setString). */
internal expect fun loadString(key: String): String?

internal expect fun saveString(key: String, value: String)

/** Local calendar yyyy-MM-dd (device timezone). */
internal expect fun platformTodayYmd(): String

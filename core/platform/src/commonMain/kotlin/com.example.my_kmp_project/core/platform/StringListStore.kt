package com.example.my_kmp_project.core.platform

import com.example.my_kmp_project.core.storage.createKeyValueStore

/**
 * Flutter SpUtils-style platform preferences, on top of the single :core:storage KV contract.
 *
 * One named store ([PLATFORM_PREFS_NAME] -- the Android prefs file name already in use) holds
 * the Home favorites, the Mine function order / dark mode / locale flags, the community
 * convention acknowledgement and the IM session id, so all of them behave the same on
 * Android, iOS and OHOS and none of them re-implements persistence.
 *
 * Absent vs empty matters: a null return means "the user never chose", which callers use to
 * fall back to defaults, while an empty list is a real stored value.
 */
private const val PLATFORM_PREFS_NAME = "home_favorites"

private val platformPrefsStore by lazy { createKeyValueStore(PLATFORM_PREFS_NAME) }

fun loadStringList(key: String): List<String>? = platformPrefsStore.getStringList(key)

fun saveStringList(key: String, values: List<String>) {
    platformPrefsStore.putStringList(key, values)
}

/** Scalar string KV (Flutter SpUtils.getString / setString). */
fun loadString(key: String): String? = platformPrefsStore.getString(key)

fun saveString(key: String, value: String) {
    platformPrefsStore.putString(key, value)
}

/** Local calendar yyyy-MM-dd (device timezone). */
expect fun platformTodayYmd(): String

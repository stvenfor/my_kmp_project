package com.example.my_kmp_project.core.platform

/** OHOS: process-memory until Preferences cinterop lands (Flutter SpUtils parity on Android/iOS). */
private val ohosStringLists = mutableMapOf<String, List<String>>()

internal actual fun loadStringList(key: String): List<String>? = ohosStringLists[key]

internal actual fun saveStringList(key: String, values: List<String>) {
    ohosStringLists[key] = values.toList()
}

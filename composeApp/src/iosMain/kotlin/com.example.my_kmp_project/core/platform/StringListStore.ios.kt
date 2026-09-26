package com.example.my_kmp_project.core.platform

import platform.Foundation.NSUserDefaults

internal actual fun loadStringList(key: String): List<String>? {
    val defaults = NSUserDefaults.standardUserDefaults
    if (defaults.objectForKey(key) == null) return null
    val arr = defaults.arrayForKey(key) ?: return emptyList()
    return arr.mapNotNull { it as? String }
}

internal actual fun saveStringList(key: String, values: List<String>) {
    NSUserDefaults.standardUserDefaults.setObject(values, key)
}

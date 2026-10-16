package com.example.my_kmp_project.core.platform

import platform.Foundation.NSUserDefaults

actual fun loadStringList(key: String): List<String>? {
    val defaults = NSUserDefaults.standardUserDefaults
    if (defaults.objectForKey(key) == null) return null
    val arr = defaults.arrayForKey(key) ?: return emptyList()
    return arr.mapNotNull { it as? String }
}

actual fun saveStringList(key: String, values: List<String>) {
    NSUserDefaults.standardUserDefaults.setObject(values, key)
}

actual fun loadString(key: String): String? {
    val defaults = NSUserDefaults.standardUserDefaults
    if (defaults.objectForKey(key) == null) return null
    return defaults.stringForKey(key)
}

actual fun saveString(key: String, value: String) {
    NSUserDefaults.standardUserDefaults.setObject(value, key)
}

actual fun platformTodayYmd(): String {
    val formatter = platform.Foundation.NSDateFormatter()
    formatter.dateFormat = "yyyy-MM-dd"
    return formatter.stringFromDate(platform.Foundation.NSDate())
}

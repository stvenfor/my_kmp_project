@file:OptIn(ExperimentalNativeApi::class)

package com.example.my_kmp_project.core.log

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform

/** iOS：release 二进制（framework 的 Release 产物）默认只留 WARN 及以上。 */
internal actual fun platformDefaultMinLevel(): AppLog.Level =
    if (Platform.isDebugBinary) AppLog.Level.Debug else AppLog.Level.Warn

internal actual fun platformWriteLog(level: AppLog.Level, tag: String, message: String) {
    println("${level.label}/$tag: $message")
}

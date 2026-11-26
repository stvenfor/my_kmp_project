package com.example.my_kmp_project.core.log

import android.util.Log

/**
 * Android 侧无法在库模块里无条件判断 buildType（buildConfig 关闭、也没有 Application
 * 上下文），所以默认按 release 保守处理（WARN）；debug 包由 `MainActivity` 依据
 * `ApplicationInfo.FLAG_DEBUGGABLE` 提升到 DEBUG。
 */
internal actual fun platformDefaultMinLevel(): AppLog.Level = AppLog.Level.Warn

/**
 * JVM 单测（`testDebugUnitTest`）里跑的是 android.jar 桩：AGP 默认
 * `returnDefaultValues = false`，任何 `Log.*` 调用都会抛
 * `RuntimeException: Method d in android.util.Log not mocked`。
 * 首次遇到后永久降级到 stdout，保证单测 / CI 不因日志实现而红。
 */
private var logcatAvailable: Boolean = true

internal actual fun platformWriteLog(level: AppLog.Level, tag: String, message: String) {
    if (logcatAvailable && writeToLogcat(level, tag, message)) return
    println("${level.label}/$tag: $message")
}

/** 写 Logcat；桩实现抛异常时返回 false（并关掉后续尝试）。 */
private fun writeToLogcat(level: AppLog.Level, tag: String, message: String): Boolean {
    val written = runCatching {
        when (level) {
            AppLog.Level.Verbose -> Log.v(tag, message)
            AppLog.Level.Debug -> Log.d(tag, message)
            AppLog.Level.Info -> Log.i(tag, message)
            AppLog.Level.Warn -> Log.w(tag, message)
            AppLog.Level.Error -> Log.e(tag, message)
            AppLog.Level.Off -> Unit
        }
    }.isSuccess
    if (!written) logcatAvailable = false
    return written
}

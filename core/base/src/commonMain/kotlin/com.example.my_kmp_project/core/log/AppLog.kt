package com.example.my_kmp_project.core.log

/**
 * 全工程唯一的日志出口：所有模块不要再直接 `println`。
 *
 * 行为：
 *  - 低于 [minLevel] 的级别直接丢弃；[enabled] 是总开关，置 false 后一条都不输出。
 *  - 默认级别由平台决定：iOS / OHOS 读 `Platform.isDebugBinary`，release 二进制默认只留
 *    WARN 及以上；Android 默认 WARN，由 `MainActivity` 在 debuggable 包中提升到 DEBUG。
 *    因此 release 包里 DEBUG / INFO 日志天然不可见。
 *  - Android 走 `android.util.Log`（logcat 带 tag/级别），iOS / OHOS 走 `println`。
 *
 * 用法：`AppLog.d("Realtime", "connect failed: $t")`。
 */
public object AppLog {

    /** 日志级别（数值越大越严重，[Off] 用于关闭）。 */
    public enum class Level(public val priority: Int, public val label: String) {
        Verbose(0, "V"),
        Debug(1, "D"),
        Info(2, "I"),
        Warn(3, "W"),
        Error(4, "E"),
        Off(5, "OFF"),
    }

    /** 总开关；与 [minLevel] 同时生效。 */
    public var enabled: Boolean = true

    /** 低于该级别的日志不输出；默认值见 [platformDefaultMinLevel]。 */
    public var minLevel: Level = platformDefaultMinLevel()

    public fun v(tag: String, message: String) {
        log(Level.Verbose, tag, message, null)
    }

    public fun d(tag: String, message: String) {
        log(Level.Debug, tag, message, null)
    }

    public fun i(tag: String, message: String) {
        log(Level.Info, tag, message, null)
    }

    public fun w(tag: String, message: String, throwable: Throwable? = null) {
        log(Level.Warn, tag, message, throwable)
    }

    public fun e(tag: String, message: String, throwable: Throwable? = null) {
        log(Level.Error, tag, message, throwable)
    }

    private fun log(level: Level, tag: String, message: String, throwable: Throwable?) {
        if (!enabled || level.priority < minLevel.priority) return
        val text = if (throwable == null) message else "$message\n${throwable.stackTraceToString()}"
        platformWriteLog(level, tag, text)
    }
}

/** 平台默认门槛：native 读 debug 二进制标记；Android 由入口按 debuggable 提升。 */
internal expect fun platformDefaultMinLevel(): AppLog.Level

/** 平台日志落地：Android → Logcat，iOS / OHOS → stdout。 */
internal expect fun platformWriteLog(level: AppLog.Level, tag: String, message: String)

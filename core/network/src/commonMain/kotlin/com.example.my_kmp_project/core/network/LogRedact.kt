package com.example.my_kmp_project.core.network

/**
 * Ktor Logging 日志脱敏：[HttpClientDefaults] 在 println 前调用，防止会话凭证随 INFO 日志
 * 落到 stdout / logcat。
 *
 * 移除范围：
 * - `Authorization` / [DemoRequestHeaders.HEADER_SESSION_ID] / [HEADER_DEVICE_ID] 三个头的值；
 * - [NetworkConfig.accessToken] / [NetworkConfig.sessionId] / [NetworkConfig.deviceId] 的字面值
 *   （任意位置兜底，即便头名大小写不同或出现在 URL query / body 里）。
 */
public fun redact(message: String): String {
    val secrets = listOf(
        NetworkConfig.accessToken,
        NetworkConfig.sessionId,
        NetworkConfig.deviceId,
    ).filter { it.isNotBlank() }

    var result = message
    // 先按字面值兜底（长串优先，避免短串先命中留下片段）。
    secrets.sortedByDescending { it.length }.forEach { secret ->
        result = result.replace(secret, REDACTED)
    }
    // 再按头名整行脱敏（Ktor INFO 头形如 "-> Authorization: Bearer xxx"）。
    return result.lines().joinToString("\n") { line ->
        val colon = line.indexOf(':')
        if (colon < 0) return@joinToString line
        val key = line.substring(0, colon).trim().removePrefix("->").trim()
        if (key.equals("Authorization", ignoreCase = true) ||
            key.equals(DemoRequestHeaders.HEADER_SESSION_ID, ignoreCase = true) ||
            key.equals(DemoRequestHeaders.HEADER_DEVICE_ID, ignoreCase = true)
        ) {
            line.substring(0, colon) + ": $REDACTED"
        } else {
            line
        }
    }
}

private const val REDACTED = "[REDACTED]"

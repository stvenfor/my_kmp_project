package com.example.my_kmp_project.component.push

import com.example.my_kmp_project.component.realtime.RecordingRawApiClient
import com.example.my_kmp_project.core.network.NetworkJson
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * C4：`POST /api/v1/push/devices` 的 `alias` 是**无效参数**回归。
 *
 * 后端 `jpush_controller.go:53-59` `RegisterDevice` 强制忽略客户端 alias，以
 * `Alias: user.ID`（会话用户）覆盖（h4 越权防护口径）。本测试锁定：
 * 请求体仍可含 `alias` 键（不改请求行为），但换 alias 值不影响请求是否发出、
 * 也不影响其余任何字段 —— 即客户端结果不依赖该值。
 */
class PushRegistrationApiTest {

    @Test
    fun aliasKeyStillPresentButRequestDoesNotDependOnItsValue() = runBlocking {
        val first = RecordingRawApiClient()
        first.respond("{}")
        PushRegistrationApi(first, log = {}).report(
            registrationId = "reg-1",
            alias = "attacker",
            platform = "android",
        )

        val second = RecordingRawApiClient()
        second.respond("{}")
        PushRegistrationApi(second, log = {}).report(
            registrationId = "reg-1",
            alias = "someone-else",
            platform = "android",
        )

        val body1 = first.calls.single().second
        val body2 = second.calls.single().second
        assertEquals("/api/v1/push/devices", first.calls.single().first)

        // 请求体仍可含 alias 键（对齐 Dart，不删字段、不改请求行为）。
        assertTrue(body1.contains("\"alias\":\"attacker\""), body1)
        assertTrue(body2.contains("\"alias\":\"someone-else\""), body2)

        // 去掉 alias 键后两次请求完全一致 ⇒ 结果不依赖 alias 的值；
        // 两次都照样发出（后端一律用会话用户覆盖）。
        assertEquals(
            NetworkJson.parseToJsonElement(body1).jsonObject.minus("alias"),
            NetworkJson.parseToJsonElement(body2).jsonObject.minus("alias"),
        )
        assertEquals(1, first.calls.size)
        assertEquals(1, second.calls.size)
    }
}

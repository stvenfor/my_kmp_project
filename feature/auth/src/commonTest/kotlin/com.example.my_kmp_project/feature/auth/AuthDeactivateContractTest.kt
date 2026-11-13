package com.example.my_kmp_project.feature.auth

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B2：`POST /api/v1/user/deactivate`（Go `user_routes.go:26`）契约 + Server-Confirmed
 * 会话编排（成功清 / 普通失败不清 / 账号不可用失败清）。
 */
class AuthDeactivateContractTest {

    @Test
    fun deactivate_path_matches_backend_route() {
        assertEquals("/api/v1/user/deactivate", AuthApiPaths.DEACTIVATE)
    }

    @Test
    fun deactivate_uses_post_with_empty_body_and_unified_client() = runBlocking {
        val fake = FakeAuthApiClient { _, _, _, _ -> """{"code":0,"message":"success","data":{"ok":true}}""" }
        val result = UserAuthHttpApi.deactivate(fake)
        assertTrue(result.isSuccess)
        val call = fake.calls.single()
        assertEquals("POST", call.verb)
        assertEquals("/api/v1/user/deactivate", call.path)
        assertEquals("", call.body) // 无 body（与 /user/logout 同形状）
    }

    @Test
    fun deactivate_business_failure_maps_to_auth_http_failure() = runBlocking {
        val fake = FakeAuthApiClient { _, _, _, _ -> """{"code":50000,"message":"服务端异常","data":null}""" }
        val error = UserAuthHttpApi.deactivate(fake).exceptionOrNull()
        assertTrue(error is AuthHttpFailure)
        assertEquals(50000, error.code)
        assertEquals("服务端异常", error.message)
    }

    @Test
    fun deactivate_success_clears_local_session() = runBlocking {
        var cleared = 0
        val result = AuthRepository.deactivate(
            remote = { Result.success(Unit) },
            onClearLocal = { cleared++ },
        )
        assertTrue(result.isSuccess)
        assertEquals(1, cleared)
    }

    @Test
    fun deactivate_generic_failure_keeps_local_session() = runBlocking {
        var cleared = 0
        val networkError = AuthHttpFailure("无法连接服务端")
        val result = AuthRepository.deactivate(
            remote = { Result.failure(networkError) },
            onClearLocal = { cleared++ },
        )
        assertTrue(result.isFailure)
        assertEquals(0, cleared)

        // 普通 403 无权限也不清。
        AuthRepository.deactivate(
            remote = { Result.failure(AuthHttpFailure("暂无权限", code = 10003, statusCode = 403)) },
            onClearLocal = { cleared++ },
        )
        assertEquals(0, cleared)
    }

    @Test
    fun deactivate_account_unavailable_failure_clears_local_session() = runBlocking {
        var cleared = 0
        val result = AuthRepository.deactivate(
            remote = { Result.failure(AuthHttpFailure("账号不可用", code = 10003, statusCode = 403)) },
            onClearLocal = { cleared++ },
        )
        assertTrue(result.isFailure)
        assertEquals(1, cleared)
    }

    @Test
    fun deactivate_body_is_never_a_json_object() = runBlocking {
        // 无 body 端点：fake 收到的 body 必须为空串，不能是 "{}" 或带字段。
        val fake = FakeAuthApiClient { _, _, _, _ -> """{"code":0,"data":null}""" }
        UserAuthHttpApi.deactivate(fake)
        assertTrue(fake.calls.single().body.isEmpty())
    }
}

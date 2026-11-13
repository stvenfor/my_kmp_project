package com.example.my_kmp_project.feature.mine

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 门店 / 成员管理写接口契约回归（`access/router.go:40-42`、`home/router.go:48`，B 区 #15–#18）。
 * 只断言请求形状（路径 / HTTP verb / 请求体字段名），不接 UI。
 */
class StoreManagementApiContractTest {

    private fun client(): MineRecordingApiClient {
        val c = MineRecordingApiClient()
        c.stub("""{"code":0,"message":"ok","data":{}}""")
        return c
    }

    @Test
    fun storePathsMatchBackendRoutes() {
        assertEquals("/api/v1/stores", StoreManagementPaths.STORES)
        assertEquals("/api/v1/stores/3/join-applications", StoreManagementPaths.joinApplications(3))
        assertEquals("/api/v1/stores/3/members", StoreManagementPaths.members(3))
        assertEquals("/api/v1/stores/3/members/u-1", StoreManagementPaths.member(3, "u-1"))
    }

    @Test
    fun createStorePostsStoreIdAndName() = runBlocking {
        val client = client()
        StoreManagementApi(client).createStore(StoreCreateRequest(storeId = 3, name = "北京店"))
        val call = client.last
        assertEquals("POST", call.method)
        assertEquals("/api/v1/stores", call.path)
        // access/handler.go:33-36
        assertTrue(call.body.contains("\"store_id\":3"), call.body)
        assertTrue(call.body.contains("\"name\":\"北京店\""), call.body)
    }

    @Test
    fun applyToStorePostsEmptyBody() = runBlocking {
        val client = client()
        StoreManagementApi(client).applyToStore(3)
        val call = client.last
        assertEquals("POST", call.method)
        assertEquals("/api/v1/stores/3/join-applications", call.path)
        // home/handler.go:40 无 body（身份取会话用户、store_id 取路径参数）
        assertEquals("", call.body)
    }

    @Test
    fun upsertMemberPostsTargetUserIdAndPosition() = runBlocking {
        val client = client()
        StoreManagementApi(client).upsertMember(3, StoreUpsertMemberRequest(targetUserId = "u-1", position = 2))
        val call = client.last
        assertEquals("POST", call.method)
        assertEquals("/api/v1/stores/3/members", call.path)
        // access/handler.go:61-64：键是 target_user_id（不是 user_id）
        assertTrue(call.body.contains("\"target_user_id\":\"u-1\""), call.body)
        assertTrue(call.body.contains("\"position\":2"), call.body)
    }

    @Test
    fun removeMemberDeletesNoBody() = runBlocking {
        val client = client()
        StoreManagementApi(client).removeMember(3, "u-1")
        val call = client.last
        assertEquals("DELETE", call.method)
        assertEquals("/api/v1/stores/3/members/u-1", call.path)
        assertEquals("", call.body)
    }
}

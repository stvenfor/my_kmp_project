package com.example.my_kmp_project.feature.home.api

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 收支统一管理 / 搜索接口契约回归（`transaction/router.go:48,58-62`，B 区 #19–#21）。
 * 只断言请求形状（路径 / HTTP verb / 查询串 / 请求体字段名），不接 UI。
 */
class TransactionManageApiTest {

    private fun client(): HomeRecordingApiClient {
        val c = HomeRecordingApiClient()
        c.stub("""{"code":0,"message":"ok","data":{}}""")
        return c
    }

    @Test
    fun managePathsMatchBackendRoutes() {
        assertEquals("/api/v1/transactions/manage", TransactionManagePaths.MANAGE)
        assertEquals("/api/v1/transactions/search", TransactionManagePaths.SEARCH)
        assertEquals("/api/v1/transactions/manage/7", TransactionManagePaths.item(7))
    }

    @Test
    fun listPageUsesGetPageSize() = runBlocking {
        val client = client()
        client.stub("""{"code":0,"message":"ok","data":{"list":[],"pagination":{"page":1,"size":20,"total":0}}}""")
        TransactionManageApi(client).listPage(page = 1, size = 20, type = "income")
        val call = client.last
        assertEquals("GET", call.method)
        assertEquals("/api/v1/transactions/manage", call.path)
        assertEquals(mapOf("page" to "1", "size" to "20", "type" to "income"), call.query)
        // 不是 legacy 的 limit/offset
        assertFalse(call.query.containsKey("limit"))
        assertFalse(call.query.containsKey("offset"))
    }

    @Test
    fun createPostsFiveKeys() = runBlocking {
        val client = client()
        TransactionManageApi(client).create(
            TransactionManageCreateRequest(type = "expense", category = "油费", amount = 128.5, date = "2026-10-16"),
        )
        val call = client.last
        assertEquals("POST", call.method)
        assertEquals("/api/v1/transactions/manage", call.path)
        // transaction/request.go:5-11
        for (key in listOf("type", "category", "amount", "date")) {
            assertTrue(call.body.contains("\"$key\""), "missing $key in ${call.body}")
        }
    }

    @Test
    fun getUpdateDeleteUseExplicitVerbsOnManageItem() = runBlocking {
        val getClient = client()
        TransactionManageApi(getClient).get(7)
        assertEquals("GET", getClient.last.method)
        assertEquals("/api/v1/transactions/manage/7", getClient.last.path)

        val putClient = client()
        TransactionManageApi(putClient).update(7, TransactionManageUpdateRequest(note = "补记"))
        assertEquals("PUT", putClient.last.method)
        assertEquals("/api/v1/transactions/manage/7", putClient.last.path)
        // 稀疏 body：只发非 null 字段
        assertEquals("""{"note":"补记"}""", putClient.last.body)

        val delClient = client()
        TransactionManageApi(delClient).delete(7)
        assertEquals("DELETE", delClient.last.method)
        assertEquals("/api/v1/transactions/manage/7", delClient.last.path)
    }

    @Test
    fun searchUsesGetQAndLimit() = runBlocking {
        val client = client()
        client.stub("""{"code":0,"message":"ok","data":{"items":[]}}""")
        TransactionManageApi(client).search(q = "油费", limit = 10)
        val call = client.last
        assertEquals("GET", call.method)
        assertEquals("/api/v1/transactions/search", call.path)
        assertEquals(mapOf("q" to "油费", "limit" to "10"), call.query)
    }
}

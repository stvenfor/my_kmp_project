package com.example.my_kmp_project.feature.home.newcar

import com.example.my_kmp_project.feature.home.api.HomeRecordingApiClient
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * `GET /api/v1/new-car-follow-files/customers` 契约回归（`new_car_follow/router.go:40`，B 区 #8）。
 * 只断言请求形状（路径 / HTTP verb / 查询串），不接 UI。
 */
class NewCarFollowCustomersApiTest {

    @Test
    fun customersPathMatchesBackendRoute() {
        assertEquals("/api/v1/new-car-follow-files/customers", NewCarFollowCustomersPaths.CUSTOMERS)
    }

    @Test
    fun listCustomersUsesGetPageSizeAndOptionalQ() = runBlocking {
        val client = HomeRecordingApiClient()
        client.stub("""{"code":0,"message":"ok","data":{"list":[],"pagination":{"page":1,"size":20,"total":0}}}""")

        NewCarFollowCustomersApi(client).listCustomers(page = 1, size = 20, q = "张")
        val call = client.last
        assertEquals("GET", call.method)
        assertEquals("/api/v1/new-car-follow-files/customers", call.path)
        // new_car_follow/handler.go:220-225：page/size 1-based + 可选 q。
        assertEquals(mapOf("page" to "1", "size" to "20", "q" to "张"), call.query)
        // 不是 legacy 的 limit/offset 方言
        assertFalse(call.query.containsKey("limit"))
        assertFalse(call.query.containsKey("offset"))
    }

    @Test
    fun blankQIsOmitted() = runBlocking {
        val client = HomeRecordingApiClient()
        client.stub("""{"code":0,"message":"ok","data":{"list":[]}}""")
        NewCarFollowCustomersApi(client).listCustomers(page = 2, size = 10, q = "  ")
        assertEquals(mapOf("page" to "2", "size" to "10"), client.last.query)
    }
}

package com.example.my_kmp_project.feature.commerce

import com.example.my_kmp_project.core.network.NetworkJson
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 商城后台写接口契约回归（`mall/router.go:42-44`，契约表 B 区 #2/#3/#4）。
 * 真相源：Go `my_go_study` mall 域（handler.go:34/60/106，entity/mall.go:180/229）。
 * 只断言请求形状（路径 / HTTP verb / 请求体字段名），不接 UI、不自造调用点。
 */
class MallAdminApiContractTest {

    private fun ok(): (String, String, Map<String, String>, String) -> String =
        { _, _, _, _ -> """{"code":0,"message":"ok","data":{}}""" }

    @Test
    fun adminPathsMatchBackendRoutes() {
        assertEquals("/api/v1/mall/products", MallAdminPaths.PRODUCTS)
        assertEquals("/api/v1/mall/products/5/skus", MallAdminPaths.productSkus(5))
        assertEquals("/api/v1/mall/skus/9/codes", MallAdminPaths.skuCodes(9))
    }

    @Test
    fun createProductPostsBackendFiveKeys() = runBlocking {
        val fake = FakeApiClient(ok())
        MallAdminApi(fake).createProduct(
            MallCreateProductRequest(
                storeId = 7,
                kind = 1,
                title = "洗车券",
                coverUrl = "https://cdn/c.jpg",
                status = 1,
            ),
        )
        val call = fake.calls.single()
        assertEquals("POST", call.verb)
        assertEquals("/api/v1/mall/products", call.path)
        // mall/handler.go:40-46 的五键与 wire 名（snake_case）。
        assertTrue(call.body.contains("\"store_id\":7"), call.body)
        assertTrue(call.body.contains("\"kind\":1"), call.body)
        assertTrue(call.body.contains("\"title\":\"洗车券\""), call.body)
        assertTrue(call.body.contains("\"cover_url\":\"https://cdn/c.jpg\""), call.body)
        assertTrue(call.body.contains("\"status\":1"), call.body)
    }

    @Test
    fun createSkuPostsBackendNineKeys() = runBlocking {
        val fake = FakeApiClient(ok())
        MallAdminApi(fake).createSku(
            productId = 5,
            request = MallCreateSkuRequest(
                skuCode = "SKU-1",
                title = "松香 100ml",
                specs = buildJsonObject { put("颜色", "松香") },
                price = "39.90",
                pricePoints = 0,
                stockQty = 5,
                status = 1,
                deliverType = 1,
                contentUrl = "https://cdn/code.pdf",
            ),
        )
        val call = fake.calls.single()
        assertEquals("POST", call.verb)
        assertEquals("/api/v1/mall/products/5/skus", call.path)
        // mall/handler.go:71-81 九键。
        for (key in listOf("sku_code", "title", "specs", "price", "price_points", "stock_qty", "status", "deliver_type", "content_url")) {
            assertTrue(call.body.contains("\"$key\""), "missing $key in ${call.body}")
        }
    }

    @Test
    fun addVirtualCodesPostsCodesOnly() = runBlocking {
        val fake = FakeApiClient(ok())
        MallAdminApi(fake).addVirtualCodes(9, MallAddVirtualCodesRequest(listOf("A", "B")))
        val call = fake.calls.single()
        assertEquals("POST", call.verb)
        assertEquals("/api/v1/mall/skus/9/codes", call.path)
        assertEquals("""{"codes":["A","B"]}""", call.body)
    }

    @Test
    fun mallAmountIsDecimalAsJsonString() {
        // C5：后端金额三套口径中的 B decimal-as-string（mall 域，JSON string）。
        // 与 PrepayRequest.amountFen 的 JSON number 形状刻意不同；后端收敛口径时三处一起改，
        // 见 MallOrderModels.kt 文件头与 PrepayRequest.amountFen 的 KDoc。
        val head = NetworkJson.encodeToString(MallOrderHead(amount = "39.90"))
        assertTrue(head.contains("\"amount\":\"39.90\""), head)
        assertFalse(head.contains("\"amount\":39"), head)

        val row = NetworkJson.encodeToString(MallOrderListRow(amount = "12.00"))
        assertTrue(row.contains("\"amount\":\"12.00\""), row)
        assertFalse(row.contains("\"amount\":12"), row)
    }
}

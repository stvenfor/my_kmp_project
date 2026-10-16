package com.example.my_kmp_project.feature.mine.address

import com.example.my_kmp_project.core.network.NetworkError
import com.example.my_kmp_project.core.network.NetworkJson
import com.example.my_kmp_project.feature.mine.MineRecordingApiClient
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 契约表 §2.14 #79-#83（Dart `address_api.dart` + `address_model.dart`，d7876a1）
 * 的 JSON fixture 测试：列表键是 `data.items`（不是 `list`）、请求体＝`toBridgeJson()`
 * 十字键（无 `postal_code`）、设默认是无 body 的 POST、删除要求 `data` 非空。
 */
class AddressApiTest {

    private val client = MineRecordingApiClient()
    private val api = AddressApi(client)

    @Test
    fun listReadsItemsKeyNotListKey() = runBlocking {
        client.stub(
            """
            {"code":0,"message":"ok","data":{"items":[
              {"address_id":1,"receiver_name":"李雷","receiver_phone":"13800138000",
               "province":"浙江省","city":"杭州市","district":"西湖区",
               "detail_address":"文三路 1 号","postal_code":"310012","is_default":true,
               "label":"家","full_address":"浙江省杭州市西湖区文三路 1 号"},
              "垃圾行",
              {"address_id":2,"receiver_name":null,"is_default":null,"label":null}
            ]},"timestamp":1760000000}
            """.trimIndent(),
        )
        val rows = api.list()
        assertEquals("GET", client.last.method)
        assertEquals("/api/v1/user/addresses", client.last.path)
        assertEquals(2, rows.size)

        val home = rows[0]
        assertEquals(1, home.resolvedAddressId)
        assertEquals("李雷", home.resolvedReceiverName)
        assertEquals("310012", home.resolvedPostalCode) // postal_code 只在响应侧存在
        assertTrue(home.resolvedIsDefault)
        assertEquals("浙江省杭州市西湖区文三路 1 号", home.resolvedFullAddress)

        // JSON null 与缺字段：Dart `?? ''` / `== true` 语义
        // （address_model.dart:19 receiver_name、:26 is_default、:27 label）
        val sparse = rows[1]
        assertEquals("", sparse.resolvedReceiverName)
        assertEquals("", sparse.resolvedLabel)
        assertFalse(sparse.resolvedIsDefault)
        // 真正缺键的字段才走 Dart 的 `?.toString() ?? ''`
        // （address_model.dart:20 receiver_phone、:22 city、:25 postal_code、:28 full_address）
        assertEquals("", sparse.resolvedReceiverPhone)
        assertEquals("", sparse.resolvedCity)
        assertEquals("", sparse.resolvedPostalCode)
        // 这一行**是带 `address_id: 2` 的**，Dart address_model.dart:18
        // `(json['address_id'] as num?)?.toInt() ?? 0` 照读该键 ⇒ 期望 2，不是缺省 0。
        assertEquals(2, sparse.resolvedAddressId)

        // `data.list` 不被接受（Dart :18 只读 items）⇒ 空表
        client.stub("""{"code":0,"data":{"list":[{"address_id":9}]}}""")
        assertTrue(api.list().isEmpty())

        // `data` 不是 object / 为 null ⇒ 加载地址失败
        client.stub("""{"code":0,"message":"","data":null}""")
        val err = assertFailsWith<NetworkError.Business> { api.list() }
        assertEquals("加载地址失败", err.message)
    }

    @Test
    fun createBodyIsExactlyToBridgeJsonTenKeys() = runBlocking {
        client.stub(
            """{"code":0,"data":{"address_id":7,"receiver_name":"李雷","postal_code":"310012"}}""",
        )
        val model = AddressModel(
            addressId = 7,
            receiverName = "李雷",
            receiverPhone = "13800138000",
            province = "浙江省",
            city = "杭州市",
            district = "西湖区",
            detailAddress = "文三路 1 号",
            postalCode = "310012",
            isDefault = true,
            label = "家",
            fullAddress = "浙江省杭州市西湖区文三路 1 号",
        )
        val created = api.create(model.toBridgeJson())
        assertEquals("POST", client.last.method)
        assertEquals("/api/v1/user/addresses", client.last.path)
        assertEquals(7, created.resolvedAddressId)

        val sent = NetworkJson.parseToJsonElement(client.last.body).jsonObject
        assertEquals(
            setOf(
                "address_id", "receiver_name", "receiver_phone", "province", "city",
                "district", "detail_address", "full_address", "is_default", "label",
            ),
            sent.keys,
        )
        // toBridgeJson 不含 postal_code（address_model.dart:44-55）
        assertFalse(sent.containsKey("postal_code"))
        assertEquals(JsonPrimitive(true), sent["is_default"])
        assertEquals(JsonPrimitive(7), sent["address_id"])
    }

    @Test
    fun updateIsPatchOnAddressIdPath() = runBlocking {
        client.stub("""{"code":0,"data":{"address_id":7,"label":"公司"}}""")
        val updated = api.update(7, AddressBridgeRequest(addressId = 7, label = "公司"))
        assertEquals("PATCH", client.last.method)
        assertEquals("/api/v1/user/addresses/7", client.last.path)
        assertEquals("公司", updated.resolvedLabel)
        // 空字符串键也照发（Dart 透传整份 map）
        val sent = NetworkJson.parseToJsonElement(client.last.body).jsonObject
        assertEquals(10, sent.keys.size)
        assertEquals(JsonPrimitive(""), sent["receiver_name"])
    }

    @Test
    fun setDefaultIsPostWithoutBody() = runBlocking {
        client.stub("""{"code":0,"data":{"address_id":7,"is_default":true}}""")
        val model = api.setDefault(7)
        assertEquals("POST", client.last.method)
        assertEquals("/api/v1/user/addresses/7/default", client.last.path)
        assertEquals("", client.last.body) // §5.12：动作式 POST 不带 body
        assertTrue(model.resolvedIsDefault)

        client.stub("""{"code":0,"message":"","data":null}""")
        assertEquals("设置默认失败", assertFailsWith<NetworkError.Business> { api.setDefault(7) }.message)
    }

    @Test
    fun deleteRequiresNonNullData() = runBlocking {
        client.stub("""{"code":0,"message":"ok","data":{}}""")
        api.delete(7)
        assertEquals("DELETE", client.last.method)
        assertEquals("/api/v1/user/addresses/7", client.last.path)

        // Dart `_data` 要求 data 非空 map（address_api.dart:80）→ null 即失败
        client.stub("""{"code":0,"message":"","data":null}""")
        val err = assertFailsWith<NetworkError.Business> { api.delete(7) }
        assertEquals("删除失败", err.message)

        // 非 0 码沿用 message
        client.stub("""{"code":404,"message":"地址不存在","data":null}""")
        val missing = assertFailsWith<NetworkError.Business> { api.delete(7) }
        assertEquals("地址不存在", missing.message)
        assertEquals(404, missing.code)
    }
}

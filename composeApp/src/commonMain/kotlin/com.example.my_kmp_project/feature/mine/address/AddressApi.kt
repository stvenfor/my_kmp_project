package com.example.my_kmp_project.feature.mine.address

import com.example.my_kmp_project.core.network.ApiClient
import com.example.my_kmp_project.core.network.ApiResponse
import com.example.my_kmp_project.core.network.NetworkError
import com.example.my_kmp_project.core.network.NetworkFacade
import com.example.my_kmp_project.core.network.decodeEnvelopeData
import com.example.my_kmp_project.core.network.patchApi
import com.example.my_kmp_project.core.network.postApi
import com.example.my_kmp_project.core.network.postApiDecoded
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject

/**
 * Dart `AddressApi` 的 path 常量
 * （`features/settings/lib/mine/address/api/address_api.dart:11,30,45,60,74`，
 * 契约表 §2.14 #79-#83）。
 */
public object AddressApiPaths {
    /** #79 列表 / #80 新建。 */
    public const val BASE: String = "/api/v1/user/addresses"

    /** #81 更新（PATCH）/ #83 删除（DELETE）。 */
    public fun detail(addressId: Int): String = "$BASE/$addressId"

    /** #82 设为默认：动作式 **POST**，无 body。 */
    public fun setDefault(addressId: Int): String = "$BASE/$addressId/default"
}

/**
 * 收货地址 HTTP 契约 —— 对应 Dart `AddressApi`（address_api.dart:6-98）。
 *
 * 身份只认 Session 头（Dart 注释 :5），KMP 侧由
 * [com.example.my_kmp_project.core.network.NetworkFacade] 统一注入 `Authorization`。
 * 失败语义同 Dart `_data`（:89-97）：`code != 0` 用 envelope message（空则「业务请求失败」），
 * `code == 0` 但 `data == null` 用调用点中文兜底。
 */
public class AddressApi(
    private val api: ApiClient = NetworkFacade.api(),
) {

    /**
     * #79 `GET /user/addresses`（Dart :7-24）。
     *
     * **列表键是 `data.items`，不是 `data.list`**（:18 `data['items'] as List? ?? const []`），
     * 所以这里不走 `ListPage` 的 `list|items` 归一，只读 `items`；
     * 非 object 的元素直接跳过（Dart `if (e is Map<String, dynamic>)`，:21）。
     * 没有分页字段，整表返回（契约表 §3 B 型）。
     */
    public suspend fun list(): List<AddressModel> {
        val response = api.getApi(
            path = AddressApiPaths.BASE,
            parseData = { el -> decodeItemsOnly(el) },
        )
        return response.dataOrThrow("加载地址失败")
    }

    /** #80 `POST /user/addresses`（Dart :26-39）：body 是 UI 的 `toBridgeJson()` 十键。 */
    public suspend fun create(body: AddressBridgeRequest): AddressModel =
        api.postApi<AddressBridgeRequest, AddressModel>(AddressApiPaths.BASE, body)
            .dataOrThrow("保存失败")

    /**
     * #81 `PATCH /user/addresses/{addressId}`（Dart :41-54）。
     * **更新是 PATCH，不是 PUT**（契约表 §1「PUT = 0 调用点」）。
     */
    public suspend fun update(addressId: Int, body: AddressBridgeRequest): AddressModel =
        api.patchApi<AddressBridgeRequest, AddressModel>(AddressApiPaths.detail(addressId), body)
            .dataOrThrow("保存失败")

    /**
     * #82 `POST /user/addresses/{addressId}/default`（Dart :56-68）：
     * 设默认是动作式 POST 且**不带 body**（§5.12），返回新的默认 [AddressModel]。
     */
    public suspend fun setDefault(addressId: Int): AddressModel =
        api.postApiDecoded<AddressModel>(AddressApiPaths.setDefault(addressId))
            .dataOrThrow("设置默认失败")

    /**
     * #83 `DELETE /user/addresses/{addressId}`（Dart :70-82）。
     * Dart 只校验 `data` 是否为**非空 map**（`_data` :80）→ 这里把 `data` 读成原始 JSON 串，
     * 缺失/JsonNull 时抛「删除失败」；内容本身按 Dart 一样丢弃。
     */
    public suspend fun delete(addressId: Int) {
        api.deleteApi(
            path = AddressApiPaths.detail(addressId),
            parseData = { el -> el.takeUnless { it is JsonNull }?.toString() },
        ).dataOrThrow("删除失败")
    }

    /** `data` 必须是 object（否则视同 Dart 的 `_data` 失败）；`items` 缺失 → 空列表。 */
    private fun decodeItemsOnly(el: JsonElement?): List<AddressModel>? {
        val obj = el as? JsonObject ?: return null
        val items = obj["items"] as? JsonArray ?: return emptyList()
        return items.filterIsInstance<JsonObject>().mapNotNull { decodeEnvelopeData<AddressModel>(it) }
    }
}

/**
 * Dart `AddressApi._data`（address_api.dart:89-97）+ `BackendResponseParser`：
 * 非 0 码 message 空则「业务请求失败」；`code == 0` 而 `data == null` 时 message 仍优先，
 * 空才用调用点兜底（「加载地址失败」/「保存失败」/「设置默认失败」/「删除失败」）。
 */
private fun <T> ApiResponse<T>.dataOrThrow(fallback: String): T {
    val text = message?.takeIf { it.isNotEmpty() }
    if (!isSuccess) {
        throw NetworkError.Business(code, text ?: "业务请求失败")
    }
    return data ?: throw NetworkError.Business(code, text ?: fallback)
}

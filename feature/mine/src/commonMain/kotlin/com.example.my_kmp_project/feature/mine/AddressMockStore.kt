package com.example.my_kmp_project.feature.mine

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.my_kmp_project.feature.mine.address.AddressApi
import com.example.my_kmp_project.feature.mine.address.AddressBridgeRequest
import com.example.my_kmp_project.feature.mine.address.AddressModel

data class MineAddress(
    val id: String,
    val receiverName: String,
    val receiverPhone: String,
    val province: String,
    val city: String,
    val district: String,
    val detailAddress: String,
    val label: String = "",
    val isDefault: Boolean = false,
) {
    val line: String
        get() = listOf(province, city, district, detailAddress)
            .filter { it.isNotBlank() }
            .joinToString(" ")

    val phoneMasked: String get() = MineContactValidators.maskPhone(receiverPhone)
}

/**
 * 收货地址页数据源 —— 远端 `/api/v1/user/addresses` 的内存缓存（原 `AddressMockStore`）。
 *
 * 契约复用本模块 [AddressApi]（契约表 #79-#83，Dart `AddressApi`）：
 * 列表 `data.items[]`、新增 POST、更新 PATCH、设默认动作式 POST、删除 DELETE。
 * 无任何假地址种子：网络失败只写 [errorMessage]，列表保留上一次成功拉到的值。
 */
object AddressMockStore {
    var version by mutableIntStateOf(0)
        private set

    private fun bump() {
        version++
    }

    var loading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var selectedAddressId by mutableStateOf<String?>(null)

    private var items by mutableStateOf<List<MineAddress>>(emptyList())

    fun addresses(): List<MineAddress> =
        items.sortedByDescending { it.isDefault }

    fun find(id: String?): MineAddress? =
        id?.let { wanted -> items.firstOrNull { it.id == wanted } }

    fun selectForEdit(id: String?) {
        selectedAddressId = id
    }

    fun validate(
        name: String,
        phone: String,
        province: String,
        city: String,
        district: String,
        detail: String,
    ): String? {
        val n = name.trim()
        if (!MineContactValidators.isNameValid(n)) {
            return if (n.isEmpty()) "请填写收货人"
            else "收货人不超过 ${MineContactValidators.MaxNameLength} 个字"
        }
        if (!MineContactValidators.isCnMobile(phone)) {
            return "请输入以 1 开头的 11 位手机号"
        }
        if (detail.trim().isEmpty()) return "请填写详细地址"
        if (province.isBlank() || city.isBlank() || district.isBlank()) {
            return "请选择省市区"
        }
        return null
    }

    /** 拉取地址列表；失败只写 [errorMessage]，不清空旧缓存。 */
    suspend fun refresh(): Result<Unit> {
        loading = true
        return try {
            items = AddressApi().list().map { it.toMineAddress() }
            errorMessage = null
            Result.success(Unit)
        } catch (e: Exception) {
            val message = e.message ?: "加载地址失败"
            errorMessage = message
            Result.failure(IllegalStateException(message))
        } finally {
            loading = false
            bump()
        }
    }

    /** 新增（[id] 为 null）或更新；成功后按服务端返回体刷新列表，失败返回 null。 */
    suspend fun save(
        id: String?,
        name: String,
        phone: String,
        province: String,
        city: String,
        district: String,
        detail: String,
        label: String,
        isDefault: Boolean,
    ): MineAddress? {
        val invalid = validate(name, phone, province, city, district, detail)
        if (invalid != null) {
            errorMessage = invalid
            bump()
            return null
        }
        val addressId = id?.toIntOrNull()
        val body = AddressBridgeRequest(
            addressId = addressId ?: 0,
            receiverName = name.trim(),
            receiverPhone = MineContactValidators.digitsOnly(phone),
            province = province.trim(),
            city = city.trim(),
            district = district.trim(),
            detailAddress = detail.trim(),
            fullAddress = listOf(province, city, district, detail)
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .joinToString(" "),
            isDefault = isDefault,
            label = label.trim(),
        )
        return try {
            val api = AddressApi()
            val saved = if (addressId != null) api.update(addressId, body) else api.create(body)
            val ui = saved.toMineAddress()
            items = if (addressId != null) {
                items.map { if (it.id == id) ui else it }
            } else {
                items + ui
            }
            if (ui.isDefault) {
                items = items.map { if (it.id == ui.id) it else it.copy(isDefault = false) }
            }
            errorMessage = null
            bump()
            ui
        } catch (e: Exception) {
            errorMessage = e.message ?: "保存失败"
            bump()
            null
        }
    }

    /** 设为默认；成功后重新拉取（服务端会清掉其它卡的默认位），失败返回 false。 */
    suspend fun setDefault(id: String): Boolean {
        val addressId = id.toIntOrNull() ?: return false
        return try {
            AddressApi().setDefault(addressId)
            errorMessage = null
            refresh()
            true
        } catch (e: Exception) {
            errorMessage = e.message ?: "设置默认失败"
            bump()
            false
        }
    }

    /** 删除；成功后重新拉取（服务端可能自动提升新的默认地址），失败返回 false。 */
    suspend fun delete(id: String): Boolean {
        val addressId = id.toIntOrNull() ?: return false
        return try {
            AddressApi().delete(addressId)
            errorMessage = null
            refresh()
            true
        } catch (e: Exception) {
            errorMessage = e.message ?: "删除失败"
            bump()
            false
        }
    }

    /** Test helper — 只清空内存缓存。 */
    fun resetForTests() {
        selectedAddressId = null
        items = emptyList()
        loading = false
        errorMessage = null
        bump()
    }
}

private fun AddressModel.toMineAddress(): MineAddress = MineAddress(
    id = resolvedAddressId.toString(),
    receiverName = resolvedReceiverName,
    receiverPhone = resolvedReceiverPhone,
    province = resolvedProvince,
    city = resolvedCity,
    district = resolvedDistrict,
    detailAddress = resolvedDetailAddress,
    label = resolvedLabel,
    isDefault = resolvedIsDefault,
)

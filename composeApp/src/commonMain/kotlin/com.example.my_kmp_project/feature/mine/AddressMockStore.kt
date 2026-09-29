package com.example.my_kmp_project.feature.mine

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

internal data class MineAddress(
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
 * Flutter AddressList/Edit controllers + AddressApi mock.
 */
internal object AddressMockStore {
    var version by mutableIntStateOf(0)
        private set

    private fun bump() {
        version++
    }

    var selectedAddressId by mutableStateOf<String?>(null)

    private var seq = 2
    private val items = mutableListOf(
        MineAddress(
            id = "addr-1",
            receiverName = "qa_user",
            receiverPhone = "13800115172",
            province = "北京市",
            city = "北京市",
            district = "朝阳区",
            detailAddress = "演示路 1 号",
            label = "家",
            isDefault = true,
        ),
        MineAddress(
            id = "addr-2",
            receiverName = "测试乙",
            receiverPhone = "13900000000",
            province = "上海市",
            city = "上海市",
            district = "浦东新区",
            detailAddress = "世纪大道 100 号",
            isDefault = false,
        ),
    )

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

    fun save(
        id: String?,
        name: String,
        phone: String,
        province: String,
        city: String,
        district: String,
        detail: String,
        label: String,
        isDefault: Boolean,
    ): MineAddress {
        val phoneDigits = MineContactValidators.digitsOnly(phone)
        if (id != null) {
            val idx = items.indexOfFirst { it.id == id }
            if (idx >= 0) {
                val updated = items[idx].copy(
                    receiverName = name.trim(),
                    receiverPhone = phoneDigits,
                    province = province.trim(),
                    city = city.trim(),
                    district = district.trim(),
                    detailAddress = detail.trim(),
                    label = label.trim(),
                    isDefault = isDefault,
                )
                items[idx] = updated
                if (isDefault) clearDefaultExcept(updated.id)
                bump()
                return updated
            }
        }
        seq += 1
        val created = MineAddress(
            id = "addr-$seq",
            receiverName = name.trim(),
            receiverPhone = phoneDigits,
            province = province.trim(),
            city = city.trim(),
            district = district.trim(),
            detailAddress = detail.trim(),
            label = label.trim(),
            isDefault = isDefault || items.isEmpty(),
        )
        items.add(0, created)
        if (created.isDefault) clearDefaultExcept(created.id)
        bump()
        return created
    }

    fun setDefault(id: String): Boolean {
        if (items.none { it.id == id }) return false
        clearDefaultExcept(id)
        bump()
        return true
    }

    fun delete(id: String): Boolean {
        val removed = items.removeAll { it.id == id }
        if (removed && items.isNotEmpty() && items.none { it.isDefault }) {
            items[0] = items[0].copy(isDefault = true)
        }
        if (removed) bump()
        return removed
    }

    private fun clearDefaultExcept(id: String) {
        for (i in items.indices) {
            val a = items[i]
            items[i] = a.copy(isDefault = a.id == id)
        }
    }

    fun resetForTests() {
        selectedAddressId = null
        seq = 2
        items.clear()
        items += listOf(
            MineAddress(
                id = "addr-1",
                receiverName = "qa_user",
                receiverPhone = "13800115172",
                province = "北京市",
                city = "北京市",
                district = "朝阳区",
                detailAddress = "演示路 1 号",
                label = "家",
                isDefault = true,
            ),
            MineAddress(
                id = "addr-2",
                receiverName = "测试乙",
                receiverPhone = "13900000000",
                province = "上海市",
                city = "上海市",
                district = "浦东新区",
                detailAddress = "世纪大道 100 号",
            ),
        )
        bump()
    }
}

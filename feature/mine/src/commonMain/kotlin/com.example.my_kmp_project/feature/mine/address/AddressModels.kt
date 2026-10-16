package com.example.my_kmp_project.feature.mine.address

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 收货地址模型 —— Flutter
 * `features/settings/lib/mine/address/model/address_model.dart`（d7876a1）的 KMP 对等；
 * 契约表 §2.14 #79-#83。
 *
 * `AddressModel` 既能解 `GET /user/addresses` 的 `data.items[]` 行，也是
 * 创建/更新/设默认的返回体（Dart 同一模型复用，§6「需要在 KMP commonMain 共享的模型」)。
 */
@Serializable
public data class AddressModel(
    @SerialName("address_id") val addressId: Int? = null,
    @SerialName("receiver_name") val receiverName: String? = null,
    @SerialName("receiver_phone") val receiverPhone: String? = null,
    val province: String? = null,
    val city: String? = null,
    val district: String? = null,
    @SerialName("detail_address") val detailAddress: String? = null,
    @SerialName("postal_code") val postalCode: String? = null,
    @SerialName("is_default") val isDefault: Boolean? = null,
    val label: String? = null,
    @SerialName("full_address") val fullAddress: String? = null,
) {
    /** Dart `(json['address_id'] as num?)?.toInt() ?? 0`（address_model.dart:18）。 */
    public val resolvedAddressId: Int get() = addressId ?: 0
    public val resolvedReceiverName: String get() = receiverName.orEmpty()
    public val resolvedReceiverPhone: String get() = receiverPhone.orEmpty()
    public val resolvedProvince: String get() = province.orEmpty()
    public val resolvedCity: String get() = city.orEmpty()
    public val resolvedDistrict: String get() = district.orEmpty()
    public val resolvedDetailAddress: String get() = detailAddress.orEmpty()
    public val resolvedPostalCode: String get() = postalCode.orEmpty()

    /** Dart `json['is_default'] == true`（:26）：严格布尔比较，null 或缺省都是 false。 */
    public val resolvedIsDefault: Boolean get() = isDefault == true
    public val resolvedLabel: String get() = label.orEmpty()
    public val resolvedFullAddress: String get() = fullAddress.orEmpty()

    /**
     * Dart `toBridgeJson()`（address_model.dart:44-55）：UI 侧透传给
     * `POST /user/addresses` 与 `PATCH /user/addresses/:addressId` 的固定 10 键
     * （**不含 `postal_code`**）。
     */
    public fun toBridgeJson(): AddressBridgeRequest = AddressBridgeRequest(
        addressId = resolvedAddressId,
        receiverName = resolvedReceiverName,
        receiverPhone = resolvedReceiverPhone,
        province = resolvedProvince,
        city = resolvedCity,
        district = resolvedDistrict,
        detailAddress = resolvedDetailAddress,
        fullAddress = resolvedFullAddress,
        isDefault = resolvedIsDefault,
        label = resolvedLabel,
    )
}

/**
 * Dart `toBridgeJson()` 的键集合（address_model.dart:45-55）——契约表 #80/#81 的请求体。
 * 十个键都非空 ⇒ `NetworkJson`（`encodeDefaults = true`）恒发全部键，与 Dart 一致。
 */
@Serializable
public data class AddressBridgeRequest(
    @SerialName("address_id") val addressId: Int = 0,
    @SerialName("receiver_name") val receiverName: String = "",
    @SerialName("receiver_phone") val receiverPhone: String = "",
    val province: String = "",
    val city: String = "",
    val district: String = "",
    @SerialName("detail_address") val detailAddress: String = "",
    @SerialName("full_address") val fullAddress: String = "",
    @SerialName("is_default") val isDefault: Boolean = false,
    val label: String = "",
)

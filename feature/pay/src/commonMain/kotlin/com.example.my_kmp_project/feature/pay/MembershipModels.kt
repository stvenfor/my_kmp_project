package com.example.my_kmp_project.feature.pay

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * 会员 / 支付契约（契约表 #90–#94；Dart
 * `features/pay/lib/membership/model/membership_models.dart` +
 * `features/pay/lib/membership/payment/membership_api.dart`，my_ai_project d7876a1）。
 *
 * Dart 侧 `/membership` 前缀接口的响应**只按裸 Map 取键**（契约表 §2.16），
 * 因此这里保持「DTO 字段 = Dart 读取的键集合」，不额外发明字段。
 */

/** Dart `enum MembershipTier { svip, aiSvip }`；`wireValue` 即 BFF 的 `tier` 串。 */
public enum class MembershipTier(public val wireValue: String) {
    Svip("svip"),
    AiSvip("ai_svip"),
    ;

    public companion object {
        /**
         * Dart `MembershipPlan.fromCatalogJson`：`tierRaw == 'ai_svip' ? aiSvip : svip`
         * ——**任何**其它值（含缺失）都落回 [Svip]，不要改成 null。
         */
        public fun fromWire(raw: String?): MembershipTier =
            if (raw == "ai_svip") AiSvip else Svip
    }
}

/**
 * Dart `enum PaymentMethodType { wechat, alipay, balance, huawei, apple }`。
 *
 * [channelWire] 是 `POST /membership/buyout` 的 `channel` 串：
 * `wechat` / `alipay` / `balance`，而 huawei / apple 走内购通道、
 * Dart 里映射成**空串**并直接返回「请使用对应内购通道」，不会打到 buyout。
 */
public enum class PaymentMethodType(public val channelWire: String) {
    WeChat("wechat"),
    Alipay("alipay"),
    Balance("balance"),
    Huawei(""),
    Apple(""),
    ;

    /** true 表示可以走 `/membership/buyout` 两步买断。 */
    public val usesBuyoutChannel: Boolean get() = channelWire.isNotEmpty()
}

/**
 * 目录项（`GET /membership/me` 的 `data.catalog[]`）。
 *
 * `price` 保留 BFF 字符串；[priceValue] 复刻 Dart 的取值顺序
 * （`double.tryParse(price) ?: price_fen / 100.0`），只作为展示派生值。
 * Dart 的 `originalPrice` 直接等于 `price`（目录无划线价）。
 */
@Serializable
public data class MembershipPlan(
    @SerialName("plan_id") val id: String = "",
    val tier: String = "",
    val title: String = "",
    /** 价格字符串（BFF 下发 `price:String`；缺失按 `''` 同 Dart）。 */
    val price: String = "",
    @SerialName("price_fen") val priceFen: Int = 0,
    val months: Int = 1,
    @SerialName("huawei_product_id") val huaweiProductId: String = "",
    @SerialName("apple_product_id") val appleProductId: String = "",
    @SerialName("auto_renew_eligible") val autoRenew: Boolean = false,
) {
    /** Dart `MembershipTier tier` 解析结果（未知值 → [MembershipTier.Svip]）。 */
    public val resolvedTier: MembershipTier get() = MembershipTier.fromWire(tier)

    /** Dart `price: double.tryParse(priceStr) ?? (fen / 100.0)`。 */
    public val priceValue: Double get() = price.toDoubleOrNull() ?: (priceFen / 100.0)

    /** Dart `originalPrice`（与 `price` 同值）。 */
    public val originalPriceValue: Double get() = priceValue
}

/** 权益行（`GET /membership/me` 的 `data.entitlements[]`，Dart 只读 `tier` + `cta`）。 */
@Serializable
public data class MembershipEntitlement(
    val tier: String = "",
    val cta: String = "",
) {
    public val resolvedTier: MembershipTier get() = MembershipTier.fromWire(tier)
}

/**
 * `GET /membership/me` 的 `data`（契约表 #90）。
 *
 * Dart 里 `data` 非 map 时退化成 `{}` ⇒ catalog / entitlements 都读不到，
 * 所以这里两个列表默认空；控制器另有 `where((p) => p.id.isNotEmpty)` 过滤，
 * 见 [catalogPlans]。
 */
@Serializable
public data class MembershipMeResponse(
    val catalog: List<MembershipPlan> = emptyList(),
    val entitlements: List<MembershipEntitlement> = emptyList(),
) {
    /** Dart `catalogPlans.assignAll(... where((p) => p.id.isNotEmpty))`。 */
    public val catalogPlans: List<MembershipPlan> get() = catalog.filter { it.id.isNotEmpty() }

    /**
     * Dart `refreshMe` 的 CTA 取值：在 `entitlements` 里找第一条 `tier` 匹配的，
     * 命中才覆盖（文案兜底 `'开通'` 属于 UI lane，这里用 null 表示「不覆盖」）。
     */
    public fun ctaFor(tier: MembershipTier): String? =
        entitlements.firstOrNull { it.tier == tier.wireValue }?.cta
}

/**
 * `POST /membership/buyout` 的响应（契约表 #91，两步支付之一）。
 *
 * - [status] `'paid'` ⇒ 余额渠道已就地扣款完成，**直接结束，不调 confirm**；
 * - [orderId] `<= 0` ⇒ Dart 在 SDK 成功后**静默跳过** `/buyout/confirm`；
 * - [prepayParams] 原样交给 SDK（微信 / 支付宝），KMP 侧不做任何拼装。
 */
@Serializable
public data class MembershipBuyoutResponse(
    val status: String = "",
    @SerialName("order_id") val orderId: Int = 0,
    @SerialName("prepay_params") val prepayParams: JsonObject? = null,
) {
    /** Dart `status == 'paid'` 短路（余额渠道）。 */
    public val isPaid: Boolean get() = status == "paid"

    /** `prepay_params` 的键值视图（值按 Dart `'${v}'` 规则转字符串）。 */
    public val prepayEntries: List<Pair<String, String>>
        get() = prepayParams?.entries
            ?.map { (key, value) -> key to renderScalar(value) }
            .orEmpty()

    /**
     * 微信 SDK 取 `prepayid`：Dart 兼容 `prepayid` / `prepayId` 两种键
     * （契约表 #91 备注），BFF 用哪种都吃。
     */
    public fun weChatPrepayId(): String =
        prepayParams.prepayString("prepayid") ?: prepayParams.prepayString("prepayId") ?: ""
}

private fun JsonObject?.prepayString(key: String): String? =
    (this?.get(key) as? JsonPrimitive)?.content?.takeIf { it.isNotEmpty() }

private fun renderScalar(value: JsonElement): String = when (value) {
    is JsonPrimitive -> value.content
    else -> value.toString()
}

/** `POST /membership/buyout` body：`{plan_id:String, channel:'wechat'|'alipay'|'balance'}`。 */
@Serializable
public data class MembershipBuyoutRequest(
    @SerialName("plan_id") val planId: String,
    val channel: String,
)

/** `POST /membership/buyout/confirm` body：`{order_id:int}`。 */
@Serializable
public data class MembershipConfirmRequest(
    @SerialName("order_id") val orderId: Int,
)

/** `POST /membership/huawei/verify` 响应：`{finish_purchase:bool}`（服务端驱动客户端结单）。 */
@Serializable
public data class MembershipHuaweiVerifyResponse(
    @SerialName("finish_purchase") val finishPurchase: Boolean = false,
)

/** `POST /membership/apple/verify` 响应：`{complete_purchase:bool}`。 */
@Serializable
public data class MembershipAppleVerifyResponse(
    @SerialName("complete_purchase") val completePurchase: Boolean = false,
)

/**
 * `POST /membership/huawei/verify` body（契约表 #93）。
 * 后三键在 Dart 里默认 `''` 且**总发**，不要按需省略。
 */
@Serializable
public data class MembershipHuaweiVerifyRequest(
    @SerialName("product_id") val productId: String,
    @SerialName("purchase_token") val purchaseToken: String,
    @SerialName("purchase_order_id") val purchaseOrderId: String = "",
    @SerialName("subscription_id") val subscriptionId: String = "",
    @SerialName("jws_purchase_order") val jwsPurchaseOrder: String = "",
)

/**
 * `POST /membership/apple/verify` body（契约表 #94）。
 * 后两键在 Dart 里默认 `''` 且**总发**。
 */
@Serializable
public data class MembershipAppleVerifyRequest(
    @SerialName("product_id") val productId: String,
    @SerialName("transaction_id") val transactionId: String,
    @SerialName("original_transaction_id") val originalTransactionId: String = "",
    @SerialName("receipt_data") val receiptData: String = "",
)

package com.example.my_kmp_project.feature.pay

import com.example.my_kmp_project.core.network.ApiClient
import com.example.my_kmp_project.core.network.ApiResponse
import com.example.my_kmp_project.core.network.NetworkError
import com.example.my_kmp_project.core.network.NetworkFacade
import com.example.my_kmp_project.core.network.NetworkJson
import com.example.my_kmp_project.core.network.decodeEnvelopeData
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject

/**
 * 会员 / 支付 HTTP 契约层（契约表 #90–#94；Dart
 * `features/pay/lib/membership/payment/membership_api.dart` +
 * `membership_payment_gateway.dart`，my_ai_project d7876a1）。
 *
 * Dart 侧这五个端点**共用同一对转换器**，本层照抄，不各写一份：
 * - `_mapConverter`（`membership_api.dart:88`）：`data` 是 Map ⇒ 原样；
 *   `data` 是数组 / 标量 ⇒ 折成 `{}`（**不算失败**，目录读空而已）；
 * - `_requireData`（`:97`）：`code != 0` **或 `data` 键缺失/为 null** ⇒ 抛。
 *
 * 因此 `confirmBuyout` / `verify*` 虽然忽略内容，仍**要求 `data` 存在**——
 * Dart 就是这么判的（`{"code":0}` 无 `data` 会抛「确认支付失败」）。
 *
 * 偏差记录：Dart `_requireData` 写的是 `model?.message ?? fallback`，而
 * `ResultModel.message` 是非空 String（缺省为 `''`），所以那个 `??` 在 Dart 里
 * **永远不生效**（BFF 不带 message 时抛的是空串）。本层与 `MallApi` / `WalletApi`
 * 统一成「空文案 ⇒ 用中文兜底」，与 Dart 的 `_data` 多数调用点一致。
 *
 * **`POST /api/v1/payments/prepay` 不在本类里**（契约表 #95）：Dart 只有
 * `MembershipPaymentGateway.prepayPath` 这个常量、**全仓零调用点**（死码），
 * 真实 prepay 参数是 #91 `/membership/buyout` 响应里的 `prepay_params`。
 * 按 M1 纪律「不得为死常量编造调用」，本类的两步买断流程不调用它。
 *
 * 以 **Go 后端为唯一真相源**时该路由真实存在（`payment/router.go:49`），其独立契约
 * （路径常量 / 请求响应模型 / 契约测试）已登记在 [PaymentPrepayApi]，见
 * `notes/backend-contract-matrix.md`；本类仍不接线、不自造调用点。
 */
public class PayApi(
    public val client: ApiClient = NetworkFacade.api(),
) {
    /** #90 `GET /api/v1/membership/me`（目录 + 权益；畸形 `data` ⇒ 空目录而非失败）。 */
    public suspend fun fetchMe(): MembershipMeResponse =
        membershipDto(getBffMap(PayApiPaths.ME, "加载会员信息失败")) { MembershipMeResponse() }

    /**
     * #91 买断下单（两步支付之一）：`POST /api/v1/membership/buyout {plan_id, channel}`。
     *
     * 返回体里的 `status == 'paid'`（余额渠道）表示服务端已就地扣款，调用方**必须**短路，
     * 不能再调 confirm；否则会把已确认的单子重复确认。
     */
    public suspend fun buyout(planId: String, channel: String): MembershipBuyoutResponse =
        membershipDto(
            postBffMap(
                path = PayApiPaths.BUYOUT,
                body = NetworkJson.encodeToString(MembershipBuyoutRequest(planId = planId, channel = channel)),
                fallback = "下单失败",
            ),
        ) { MembershipBuyoutResponse() }

    /**
     * #92 买断确认（两步支付之二）：`POST /api/v1/membership/buyout/confirm {order_id}`，
     * 只在渠道 SDK 支付成功后调用；`data` 内容被 Dart 忽略，但**仍要求存在**。
     */
    public suspend fun confirmBuyout(orderId: Int) {
        postBffMap(
            path = PayApiPaths.BUYOUT_CONFIRM,
            body = NetworkJson.encodeToString(MembershipConfirmRequest(orderId = orderId)),
            fallback = "确认支付失败",
        )
    }

    /**
     * #93 华为内购验单：`POST /api/v1/membership/huawei/verify`。
     *
     * 服务端驱动结单：响应 `finish_purchase == true` ⇒ 客户端需调
     * `HuaweiIapBridge.finishPurchase`；`restoreHuaweiPurchases()` 会对每笔历史购买**循环**调用本方法。
     *
     * GAP(R5): 桥接本体属平台 SDK lane，本层只把 [MembershipHuaweiVerifyResponse.finishPurchase] 交回调用方。
     */
    public suspend fun verifyHuawei(
        productId: String,
        purchaseToken: String,
        purchaseOrderId: String = "",
        subscriptionId: String = "",
        jwsPurchaseOrder: String = "",
    ): MembershipHuaweiVerifyResponse {
        // GAP(R5): 验单成功后需 HuaweiIapBridge.finishPurchase（ohosMain SDK lane）；本层只回传标志位。
        return membershipDto(
            postBffMap(
                path = PayApiPaths.HUAWEI_VERIFY,
                body = NetworkJson.encodeToString(
                    MembershipHuaweiVerifyRequest(
                        productId = productId,
                        purchaseToken = purchaseToken,
                        purchaseOrderId = purchaseOrderId,
                        subscriptionId = subscriptionId,
                        jwsPurchaseOrder = jwsPurchaseOrder,
                    ),
                ),
                fallback = "华为内购验单失败",
            ),
        ) { MembershipHuaweiVerifyResponse() }
        // GAP(R5): finishPurchase 需真实 HuaweiIapBridge（ohosMain SDK lane），本层不代跑。
    }

    /**
     * #94 苹果内购验单：`POST /api/v1/membership/apple/verify`。
     *
     * `complete_purchase == true` ⇒ 客户端需调 `AppleIapBridge.completePurchase`。
     *
     * GAP(R5): StoreKit 桥接本体属平台 SDK lane，本层只回传标志位。
     */
    public suspend fun verifyApple(
        productId: String,
        transactionId: String,
        originalTransactionId: String = "",
        receiptData: String = "",
    ): MembershipAppleVerifyResponse =
        membershipDto(
            postBffMap(
                path = PayApiPaths.APPLE_VERIFY,
                body = NetworkJson.encodeToString(
                    MembershipAppleVerifyRequest(
                        productId = productId,
                        transactionId = transactionId,
                        originalTransactionId = originalTransactionId,
                        receiptData = receiptData,
                    ),
                ),
                fallback = "苹果内购验单失败",
            ),
        ) { MembershipAppleVerifyResponse() }

    /**
     * 两步买断编排（Dart `MembershipPaymentGateway.payBuyout`，
     * `membership_payment_gateway.dart:24-88`）：
     *
     * 1. huawei / apple 的 `channel` 是空串 ⇒ **不发 HTTP**，直接
     *    `LaunchFailed('请使用对应内购通道')`；
     * 2. `POST /membership/buyout {plan_id, channel}`；
     * 3. `status == 'paid'` ⇒ **立即成功返回**（余额渠道在服务端就地扣款），不碰 SDK、不发 confirm；
     * 4. 否则把 `prepay_params` 原样交给渠道 SDK，**只有 SDK 成功**才
     *    `POST /membership/buyout/confirm {order_id}`；SDK 失败/取消 ⇒ 原样返回其结论；
     * 5. `order_id <= 0` ⇒ Dart **静默跳过** confirm（合并成一步会丢「已支付未确认」订单，契约表 §5.1）；
     * 6. 任何异常（业务码 / 传输 / SDK 抛出）都折成结果对象，**不外抛**
     *    （Dart `on HttpRequestException catch (e) → sdkError(message, code)` +
     *    `catch (e) → sdkError('$e')`，`:78-87`）。
     *
     * GAP(R5): [sdkPay] 就是微信 / 支付宝 SDK 的交接点（Dart
     * `WysLoginSharePayService.instance.pay`），由平台 SDK lane 注入真实实现；
     * Dart 还有一条「微信未配置 ⇒ `LaunchFailed('请先在 WysWechatConfig 填写…')`」
     * 的前置检查，属于 SDK 配置态，同样留给平台 lane，本层不伪造。
     */
    public suspend fun payBuyout(
        method: PaymentMethodType,
        planId: String,
        sdkPay: suspend (MembershipBuyoutResponse) -> Boolean,
    ): MembershipPayOutcome {
        if (!method.usesBuyoutChannel) {
            // Dart: huawei / apple 走内购通道，buyout 直接 launchFailed('请使用对应内购通道')。
            return MembershipPayOutcome(MembershipPayStatus.LaunchFailed, message = "请使用对应内购通道")
        }
        return try {
            val buyout = buyout(planId = planId, channel = method.channelWire)
            if (buyout.isPaid) {
                // Dart 这里返回的 success 不带 order_id，本层额外带上以便调用方对账。
                return MembershipPayOutcome(MembershipPayStatus.Success, orderId = buyout.orderId)
            }
            // GAP(R5): 真实 SDK 调用（WysLoginSharePayService.pay）在平台 lane；这里只吃它的布尔结果。
            if (!sdkPay(buyout)) {
                return MembershipPayOutcome(
                    MembershipPayStatus.SdkNotSucceeded,
                    orderId = buyout.orderId,
                )
            }
            if (buyout.orderId > 0) {
                confirmBuyout(orderId = buyout.orderId)
            }
            MembershipPayOutcome(MembershipPayStatus.Success, orderId = buyout.orderId)
        } catch (e: NetworkError) {
            MembershipPayOutcome(
                MembershipPayStatus.SdkError,
                message = e.message.orEmpty(),
                code = e.code,
            )
        } catch (e: Exception) {
            // Dart 的兜底 `catch (e)` 把任何异常（含 SDK 抛出）折成 sdkError。
            MembershipPayOutcome(
                MembershipPayStatus.SdkError,
                message = e.message ?: e.toString(),
            )
        }
    }

    /**
     * `data` → DTO：Dart `_mapConverter` 已保证拿到的是 Map（非 Map 折成 `{}`），
     * 所以这里只可能因单个字段类型脏而失败，失败即退化成全默认 DTO（同 Dart 读不到键）。
     */
    private inline fun <reified T : Any> membershipDto(map: JsonObject, empty: () -> T): T =
        decodeEnvelopeData<T>(map) ?: empty()

    private suspend fun getBffMap(path: String, fallback: String): JsonObject {
        val response: ApiResponse<JsonObject> =
            client.getApi(path) { el: JsonElement? -> el.toBffMapOrNull() }
        return response.requireData(fallback)
    }

    private suspend fun postBffMap(path: String, body: String, fallback: String): JsonObject {
        val response: ApiResponse<JsonObject> =
            client.postApi(path, body) { el: JsonElement? -> el.toBffMapOrNull() }
        return response.requireData(fallback)
    }
}

/** Dart `MembershipPaymentGateway` 的支付结论（`WysPaymentStatus` 的契约层对等子集）。 */
public enum class MembershipPayStatus {
    /** 已完成（余额短路，或 SDK 成功 + confirm 已发）。 */
    Success,

    /** 渠道不可走 buyout（huawei / apple），或 SDK 未配置。 */
    LaunchFailed,

    /** SDK 未成功（取消 / 失败），confirm 不会发出。 */
    SdkNotSucceeded,

    /** 请求或 SDK 抛异常（Dart `WysPaymentStatus.sdkError`）。 */
    SdkError,
}

/** [PayApi.payBuyout] 的返回值（Dart `WysPaymentResult` 的对等子集）。 */
public data class MembershipPayOutcome(
    val status: MembershipPayStatus,
    val message: String = "",
    val code: Int? = null,
    val orderId: Int = 0,
) {
    public val isSuccess: Boolean get() = status == MembershipPayStatus.Success
}

/** 会员 / 支付路径常量（与 Dart `MembershipApi` 五个静态常量一一对应）。 */
public object PayApiPaths {
    public const val ME: String = "/api/v1/membership/me"
    public const val BUYOUT: String = "/api/v1/membership/buyout"
    public const val BUYOUT_CONFIRM: String = "/api/v1/membership/buyout/confirm"
    public const val HUAWEI_VERIFY: String = "/api/v1/membership/huawei/verify"
    public const val APPLE_VERIFY: String = "/api/v1/membership/apple/verify"
}

/**
 * Dart `_mapConverter` 的入参侧：`data` 缺失 / `null` ⇒ **null**（让 [requireData] 抛，
 * 同 Dart `rawData == null ⇒ data = null`）；非 Map ⇒ `{}`。
 */
private fun JsonElement?.toBffMapOrNull(): JsonObject? = when {
    this == null || this is JsonNull -> null
    this is JsonObject -> this
    else -> JsonObject(emptyMap())
}

/**
 * Dart `MembershipApi._requireData`：`code == 0` 且 `data != null` 才算成功。
 * 注意 Dart 的 `model?.message ?? fallback` 永远取到空串（`ResultModel.message` 非空），
 * 这里按仓库多数写法用「空文案 ⇒ 中文兜底」，偏差已记在类 KDoc。
 */
public fun <T> ApiResponse<T>.requireData(fallback: String): T {
    val payload = data
    if (!isSuccess || payload == null) {
        throw NetworkError.Business(code, message?.takeIf { it.isNotEmpty() } ?: fallback)
    }
    return payload
}

package com.example.my_kmp_project.feature.pay

import com.example.my_kmp_project.core.network.ApiClient
import com.example.my_kmp_project.core.network.ApiResponse
import com.example.my_kmp_project.core.network.NetworkError
import com.example.my_kmp_project.core.network.NetworkFacade
import com.example.my_kmp_project.core.network.NetworkJson
import com.example.my_kmp_project.core.network.decodeEnvelopeData
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * `POST /api/v1/payments/prepay` 契约（契约表 #95）。
 *
 * 真相源改为 **Go 后端 my_go_study**（不再是 Flutter 的死常量）：
 * - 路由：`app/internal/payment/router.go:49` 在 `/payments` 组挂 POST /prepay，走 SessionAuth 链；
 * - 请求：`app/internal/payment/handler.go:28` `prepayRequest`
 *   （`channel` / `amount_fen` / `subject` / `ref_type` / `ref_id` 五键全 required）；
 * - 响应：`app/internal/payment/service.go:66` `PrepayResult`
 *   （`channel` / `out_trade_no` / `params`），经 `response.Success` 包成信封的 `data`。
 *
 * 与会员两步买断 [PayApi] 分开：`/payments/prepay` 属支付域，不是 `/membership` 前缀；
 * `payBuyout` 仍走 `/membership/buyout` → `/membership/buyout/confirm`，**不调用**本端点
 * （Dart `MembershipPaymentGateway.prepayPath` 为零调用点死常量，见 `PayApi` KDoc）。
 * 本文件只登记后端真实存在的路由契约，不自造调用点，也不改后端仓库。
 * 逐条对照见 `notes/backend-contract-matrix.md`。
 */
public class PaymentPrepayApi(
    private val client: ApiClient = NetworkFacade.api(),
) {
    /** `POST /api/v1/payments/prepay`，返回渠道 SDK 唤起参数。 */
    public suspend fun prepay(request: PrepayRequest): PrepayResponse {
        val response: ApiResponse<PrepayResponse> = client.postApi(
            path = PaymentPrepayPaths.PREPAY,
            body = NetworkJson.encodeToString(request),
        ) { el: JsonElement? -> decodeEnvelopeData<PrepayResponse>(el) }
        if (!response.isSuccess || response.data == null) {
            throw NetworkError.Business(
                response.code,
                response.message?.takeIf { it.isNotEmpty() } ?: "预下单失败",
            )
        }
        return response.data!!
    }
}

/** 支付预下单路径常量（对齐 `payment/router.go:49`）。 */
public object PaymentPrepayPaths {
    public const val PREPAY: String = "/api/v1/payments/prepay"
}

/** `POST /payments/prepay` body（Go `handler.go:28` prepayRequest，五键全 required）。 */
@Serializable
public data class PrepayRequest(
    val channel: String,
    @SerialName("amount_fen") val amountFen: Long,
    val subject: String,
    @SerialName("ref_type") val refType: String,
    @SerialName("ref_id") val refId: String,
)

/** `POST /payments/prepay` 的 `data`（Go `service.go:66` PrepayResult；`params` 为任意键值）。 */
@Serializable
public data class PrepayResponse(
    val channel: String = "",
    @SerialName("out_trade_no") val outTradeNo: String = "",
    val params: JsonObject = JsonObject(emptyMap()),
)

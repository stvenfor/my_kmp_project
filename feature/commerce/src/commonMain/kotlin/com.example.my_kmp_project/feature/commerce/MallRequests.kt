package com.example.my_kmp_project.feature.commerce

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * 商城写操作请求体（契约表 #29、#30、#33；Dart
 * `features/mall/lib/mall/api/mall_api.dart:60,83,188`，my_ai_project d7876a1）。
 *
 * 所有键名 snake_case 与 BFF 一致；`NetworkJson` 关掉了 `explicitNulls`，
 * 而这些 body **没有需要显式发 null 的字段**（Dart 侧 receiver 三件套是 `''` 而非 null），
 * 所以统一用 [NetworkJson] 编码即可。
 */

/** `POST /api/v1/mall/cart` body：`{sku_id, qty}`（Dart `addToCart`，返回值原样丢弃）。 */
@Serializable
public data class MallAddToCartRequest(
    @SerialName("sku_id") val skuId: Int,
    val qty: Int,
)

/** `POST /api/v1/mall/orders` 的行项。 */
@Serializable
public data class MallOrderLine(
    @SerialName("sku_id") val skuId: Int,
    val qty: Int,
)

/**
 * `POST /api/v1/mall/orders` body（Dart `createOrder`，`mall_api.dart:83`）。
 *
 * receiver 三件套默认 `''` 且**总发**（Dart 无条件写进 map），
 * `store_id` / `lines` 同 Dart 一次只下一行的形状。
 */
@Serializable
public data class MallCreateOrderRequest(
    @SerialName("idempotency_key") val idempotencyKey: String,
    @SerialName("store_id") val storeId: Int,
    val lines: List<MallOrderLine>,
    @SerialName("receiver_name") val receiverName: String = "",
    @SerialName("receiver_phone") val receiverPhone: String = "",
    @SerialName("receiver_address") val receiverAddress: String = "",
)

/** `POST /api/v1/mall/orders/:orderId/pay` body：`{payment_channel:int}`。 */
@Serializable
public data class MallPayOrderRequest(
    @SerialName("payment_channel") val paymentChannel: Int,
)

/**
 * `POST /api/v1/mall/orders` 的 `data`：`{order:{order_id,...}}`
 * （Dart 只取 `data['order']['order_id']`，其余字段在 #32/#33 详情里再取）。
 */
@Serializable
public data class MallCreateOrderResponse(
    val order: MallOrderHead? = null,
)

/**
 * Dart `DateTime.now().microsecondsSinceEpoch` 对等：stdlib 只有 [Instant] 的
 * epochSeconds + nanosecondsOfSecond，组合出真正的微秒时间戳。
 */
@OptIn(ExperimentalTime::class)
public fun nowMicros(): Long {
    val now = Clock.System.now()
    return now.epochSeconds * 1_000_000L + now.nanosecondsOfSecond / 1_000L
}

/**
 * 下单幂等键：Dart `'$skuId-${DateTime.now().microsecondsSinceEpoch}'`（`mall_api.dart:86`）。
 *
 * **保持逐次重新生成**（parity 优先，不做「稳定键」修复）：键里带当前微秒，
 * 因此它只能去重「同一请求重发」，**不能**去重客户端超时后的重试——
 * 重试会拿到新键并在服务端生成第二单。契约表 §5.3 明确要求保留该行为。
 */
public fun mallIdempotencyKey(skuId: Int, micros: Long = nowMicros()): String =
    "$skuId-$micros"

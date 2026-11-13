package com.example.my_kmp_project.feature.commerce

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * 商城订单契约（契约表 #31、#32、#33；Dart
 * `features/mall/lib/mall/model/mall_order.dart`，my_ai_project d7876a1）。
 *
 * 状态域（`mall_order.dart:179`）：0 待支付 / 1 已支付 / 2 已履约 / 3 已取消 / 4 已关闭。
 * 列表行与详情共用 [MallOrderItem]（Dart 里两端点一模型，见契约表 §6）。
 *
 * 金额口径：本文件所有 `amount: String`（如 `"0.00"`）对应后端
 * `docs/placeholder-inventory.md` §28 的 **B decimal-as-string**（mall 域 `numeric(10,2)`，
 * wire 上是 JSON string）。另有两套并存：**A 整数分 `int64`**（payment / membership /
 * wallet / auto_finance 域，`PrepayRequest.amountFen`，JSON number）、**C float64**
 * （transaction 域，`TransactionModel.amount`，JSON number）。`transactions` 已落
 * `amount_fen` 整数分 + 双写（h6b），used_car / auto_finance / mall 仍各自口径。
 * 后端收敛金额口径时，`PrepayRequest.amountFen`、`MallOrderModels.amount`、
 * `WalletModels.amount` 三处要一起改，勿单独「顺手统一」。契约测试
 * `MallAdminApiContractTest.mallAmountIsDecimalAsJsonString` 锁定本形状。
 */

/** `MallOrderItem`（`mall_order.dart:2`）。 */
@Serializable
public data class MallOrderItem(
    @SerialName("item_id") val itemId: Int = 0,
    @SerialName("order_id") val orderId: Int = 0,
    @SerialName("product_title") val productTitle: String = "",
    @SerialName("cover_url") val coverUrl: String = "",
    val qty: Int = 0,
    /** 行金额，BFF 字符串（Dart 兜底 `'0.00'`）。 */
    @SerialName("line_amount") val lineAmount: String = "0.00",
    /** `kind == 1` ⇒ 虚拟商品。 */
    val kind: Int = 0,
    @SerialName("content_url") val contentUrl: String = "",
) {
    public val isVirtual: Boolean get() = kind == 1
}

/**
 * 订单主体：`GET /mall/orders/:orderId` 与 `POST /mall/orders/:orderId/pay` 的
 * `data.order`，同时也是 `POST /mall/orders` 响应里的 `data.order`。
 */
@Serializable
public data class MallOrderHead(
    @SerialName("order_id") val orderId: Int = 0,
    @SerialName("order_no") val orderNo: String = "",
    @SerialName("store_id") val storeId: Int = 0,
    val status: Int = 0,
    /** 订单金额：**B decimal-as-string**（mall 域，JSON string，见文件头金额口径说明）。 */
    val amount: String = "0.00",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("receiver_name") val receiverName: String = "",
    @SerialName("receiver_phone") val receiverPhone: String = "",
    @SerialName("receiver_address") val receiverAddress: String = "",
    @SerialName("payment_channel") val paymentChannel: Int? = null,
    @SerialName("paid_at") val paidAt: String = "",
    @SerialName("total_points") val totalPoints: Int = 0,
)

/**
 * 订单详情响应（契约表 #32）。
 *
 * **`pay_deadline_at` 读根 `data`，不在 `order` 里**（Dart `mall_order.dart:146`：
 * `payDeadlineAt: json['pay_deadline_at']`）——把 `order` 建成嵌套 DTO 正是为了让这条
 * 语义在类型上无法被写错。
 */
@Serializable
public data class MallOrderDetailResponse(
    val order: MallOrderHead? = null,
    val items: List<MallOrderItem> = emptyList(),
    @SerialName("pay_deadline_at") val payDeadlineAt: String = "",
) {
    public val orderId: Int get() = order?.orderId ?: 0
    public val orderNo: String get() = order?.orderNo.orEmpty()
    public val storeId: Int get() = order?.storeId ?: 0
    public val status: Int get() = order?.status ?: 0
    public val amount: String get() = order?.amount ?: "0.00"
    public val createdAt: String get() = order?.createdAt.orEmpty()
    public val receiverName: String get() = order?.receiverName.orEmpty()
    public val receiverPhone: String get() = order?.receiverPhone.orEmpty()
    public val receiverAddress: String get() = order?.receiverAddress.orEmpty()
    public val paymentChannel: Int? get() = order?.paymentChannel
    public val paidAt: String get() = order?.paidAt.orEmpty()
    public val totalPoints: Int get() = order?.totalPoints ?: 0

    public val statusLabel: String get() = mallOrderStatusLabel(status)
    public val isUnpaid: Boolean get() = status == 0

    @OptIn(ExperimentalTime::class)
    public val payDeadline: Instant? get() = parseMallTime(payDeadlineAt)

    /** Dart `hasReceiver`：三件套任一 trim 非空。 */
    public val hasReceiver: Boolean
        get() = receiverName.trim().isNotEmpty() ||
            receiverPhone.trim().isNotEmpty() ||
            receiverAddress.trim().isNotEmpty()
}

/** 订单列表行（契约表 #31；Dart `MallOrderListRow`，`mall_order.dart:39`）。 */
@Serializable
public data class MallOrderListRow(
    @SerialName("order_id") val orderId: Int = 0,
    @SerialName("order_no") val orderNo: String = "",
    @SerialName("store_id") val storeId: Int = 0,
    val status: Int = 0,
    /** 订单金额：**B decimal-as-string**（mall 域，JSON string，见文件头金额口径说明）。 */
    val amount: String = "0.00",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("pay_deadline_at") val payDeadlineAt: String = "",
    val items: List<MallOrderItem> = emptyList(),
) {
    public val statusLabel: String get() = mallOrderStatusLabel(status)
    public val isUnpaid: Boolean get() = status == 0

    @OptIn(ExperimentalTime::class)
    public val payDeadline: Instant? get() = parseMallTime(payDeadlineAt)

    /**
     * Dart `primaryTitle`：`items.isEmpty ? '订单 $orderNo' : items.first.productTitle`。
     * 注意是**空列表**才回退，首行标题为空串时原样返回空串（勿写成 `?:`）。
     */
    public val primaryTitle: String
        get() = if (items.isEmpty()) "订单 $orderNo" else items.first().productTitle

    public val primaryCover: String get() = items.firstOrNull()?.coverUrl.orEmpty()

    public val totalQty: Int get() = items.sumOf { it.qty }
}

/** Dart `mallOrderStatusLabel`（`mall_order.dart:179`）。 */
public fun mallOrderStatusLabel(status: Int): String = when (status) {
    0 -> "待支付"
    1 -> "已支付"
    2 -> "已履约"
    3 -> "已取消"
    4 -> "已关闭"
    else -> "未知"
}

/**
 * Dart `parseMallTime`（`mall_order.dart:196`）：`DateTime.tryParse(raw)?.toLocal()`，
 * 空串返回 null。
 *
 * 已知偏差（记录勿隐瞒）：Dart 对**不带时区偏移**的串按本地时区解释，KMP commonMain 没有
 * 时区库，故这类串返回 `null`；Go BFF 的 `time.Time` 序列化为 RFC3339（必带 `Z`/偏移），
 * 实际链路等价。[Instant] 与时区无关，Dart 的 `toLocal()` 在这里是同一时刻的另一种显示，
 * 不影响倒计时（倒计时只做 `deadline - now`）。
 */
@OptIn(ExperimentalTime::class)
public fun parseMallTime(raw: String): Instant? {
    if (raw.isEmpty()) return null
    return Instant.parseOrNull(raw) ?: Instant.parseOrNull(raw.replace(' ', 'T'))
}

/**
 * Dart `formatPayCountdown`（`mall_order.dart:202`）：剩余时间格式化为 `HH:MM:SS`，
 * 负数 clamp 到 0，不足 1 小时也补零小时。
 */
public fun formatPayCountdown(remaining: Duration): String {
    val clamped = if (remaining.isNegative()) Duration.ZERO else remaining
    val totalSeconds = clamped.inWholeSeconds
    val h = (totalSeconds / 3600).toString().padStart(2, '0')
    val m = ((totalSeconds / 60) % 60).toString().padStart(2, '0')
    val s = (totalSeconds % 60).toString().padStart(2, '0')
    return "$h:$m:$s"
}

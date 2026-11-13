package com.example.my_kmp_project.feature.wallet

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 钱包契约（契约表 #84、#85、#89；Dart
 * `features/wallet/lib/wallet/model/wallet_models.dart`，my_ai_project d7876a1）。
 *
 * 金额口径与 Dart 一致：**元字符串 + 分整数**双字段
 * （`balance:String` 保持字符串，不做 Double 化；`*_fen` 是整数分）。
 */

/** `GET /api/v1/wallet` 的完整汇总（Dart `WalletSummary`）。 */
@Serializable
public data class WalletSummary(
    val balance: String = "0.00",
    @SerialName("balance_fen") val balanceFen: Int = 0,
    val cards: List<BankCard> = emptyList(),
)

/**
 * 银行卡（Dart `BankCard`，`wallet_models.dart:25`）：
 * 既用于 `GET /wallet` 的 `cards`，也是 `POST /wallet/cards` 的响应模型（契约表 §6）。
 */
@Serializable
public data class BankCard(
    @SerialName("card_id") val cardId: Int = 0,
    @SerialName("bank_name") val bankName: String = "",
    @SerialName("card_last4") val cardLast4: String = "",
    @SerialName("holder_name") val holderName: String = "",
    @SerialName("is_default") val isDefault: Boolean = false,
) {
    /** Dart `display`：`招商银行(****1234)` 形态。 */
    public val display: String get() = "$bankName(****$cardLast4)"
}

/**
 * 钱包流水行（Dart `WalletLedgerEntry`）。`reason` 是开放枚举，
 * Dart 只对已知值转中文，未知值原样展示（`reasonLabel`）。
 */
@Serializable
public data class WalletLedgerEntry(
    @SerialName("ledger_id") val ledgerId: Int = 0,
    @SerialName("delta_fen") val deltaFen: Int = 0,
    @SerialName("balance_fen") val balanceFen: Int = 0,
    val reason: String = "",
    @SerialName("ref_id") val refId: String = "",
) {
    /**
     * Dart `deltaYuan`：分 → 带符号的两位小数元串（`+12.34` / `-0.05`），
     * 整型除法 + `padLeft(2,'0')` 逐位照搬，不走浮点格式化以免 Locale 差异。
     */
    public val deltaYuan: String
        get() {
            val neg = deltaFen < 0
            val v = if (deltaFen < 0) -deltaFen else deltaFen
            val s = "${v / 100}.${(v % 100).toString().padStart(2, '0')}"
            return if (neg) "-$s" else "+$s"
        }

    /** Dart `reasonLabel`：recharge/mall_pay/mall_refund → 中文，其余原样。 */
    public val reasonLabel: String
        get() = when (reason) {
            "recharge" -> "充值"
            "mall_pay" -> "商城支付"
            "mall_refund" -> "商城退款入账"
            else -> reason
        }
}

/** `POST /api/v1/wallet/recharge` 响应（Dart `WalletRechargeResult`）。 */
@Serializable
public data class WalletRechargeResult(
    val balance: String = "0.00",
    @SerialName("balance_fen") val balanceFen: Int = 0,
    @SerialName("delta_fen") val deltaFen: Int = 0,
)

/**
 * `POST /api/v1/wallet/cards` body（契约表 #86）：Dart 总发四键，
 * `holder_name` 默认 `''`、`is_default` 默认 `false`。
 */
@Serializable
public data class WalletBindCardRequest(
    @SerialName("bank_name") val bankName: String,
    @SerialName("card_last4") val cardLast4: String,
    @SerialName("holder_name") val holderName: String = "",
    @SerialName("is_default") val isDefault: Boolean = false,
)

/**
 * `POST /api/v1/wallet/recharge` body（契约表 #89）。
 *
 * **`amount` 是十进制「元」定价串**（Dart `{amount: '12.34', channel: int, card_id?: int}`），
 * 不是分；`card_id` 仅在非 null 时带上——[com.example.my_kmp_project.core.network.NetworkJson]
 * 关掉了 `explicitNulls`，null 字段自动省略，与 Dart 的 `if (cardId != null)` 同形。
 *
 * 金额口径：后端 `docs/placeholder-inventory.md` §28 登记「金额三套口径并存」，
 * 本 `amount` 对应其中 **B decimal-as-string**（`numeric(10,2)`，wire 上是 JSON string）。
 * 另有两套并存：**A 整数分 `int64`**（`PrepayRequest.amountFen`，JSON number）、
 * **C float64**（transaction 域，`TransactionModel.amount`，JSON number）。
 * `transactions` 已落 `amount_fen` 整数分 + 双写（h6b），used_car / auto_finance / mall
 * 仍各自口径。后端收敛金额口径时，`PrepayRequest.amountFen`、
 * `MallOrderModels.amount`、本 `WalletRechargeRequest.amount` 三处要一起改，
 * 勿单独「顺手统一」。契约测试 `WalletMoneyShapeTest` 锁定本形状为 JSON string。
 */
@Serializable
public data class WalletRechargeRequest(
    val amount: String,
    val channel: Int,
    @SerialName("card_id") val cardId: Int? = null,
)

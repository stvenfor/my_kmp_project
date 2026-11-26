package com.example.my_kmp_project.feature.mine

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.my_kmp_project.core.network.ApiClient
import com.example.my_kmp_project.core.network.ApiResponse
import com.example.my_kmp_project.core.network.NetworkCodes
import com.example.my_kmp_project.core.network.NetworkError
import com.example.my_kmp_project.core.network.NetworkFacade
import com.example.my_kmp_project.core.network.deleteApi
import com.example.my_kmp_project.core.network.getApi
import com.example.my_kmp_project.core.network.getListPage
import com.example.my_kmp_project.core.network.postApi
import com.example.my_kmp_project.core.network.postApiDecoded
import com.example.my_kmp_project.core.network.queryOf
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** 钱包银行卡展示行（`GET /api/v1/wallet` 的 `cards[]`）。 */
@Serializable
data class WalletBankCard(
    @SerialName("card_id") val cardId: Int = 0,
    @SerialName("bank_name") val bankName: String = "",
    @SerialName("card_last4") val cardLast4: String = "",
    @SerialName("holder_name") val holderName: String = "",
    @SerialName("is_default") val isDefault: Boolean = false,
) {
    val display: String get() = "$bankName(****$cardLast4)"
}

/** 钱包流水展示行（`GET /api/v1/wallet/ledger` 的 `data.items[]`）。 */
@Serializable
data class WalletLedgerEntry(
    @SerialName("ledger_id") val ledgerId: Int = 0,
    @SerialName("delta_fen") val deltaFen: Int = 0,
    @SerialName("balance_fen") val balanceFen: Int = 0,
    val reason: String = "",
    @SerialName("ref_id") val refId: String = "",
) {
    val deltaYuan: String
        get() {
            val neg = deltaFen < 0
            val v = kotlin.math.abs(deltaFen)
            val s = "${v / 100}.${(v % 100).toString().padStart(2, '0')}"
            return if (neg) "-$s" else "+$s"
        }

    val reasonLabel: String
        get() = when (reason) {
            "recharge" -> "充值"
            "mall_pay" -> "商城支付"
            "mall_refund" -> "商城退款入账"
            else -> reason
        }
}

/**
 * 钱包页数据源 —— 远端 `GET/POST /api/v1/wallet*` 的内存缓存（原 `WalletMockStore`）。
 *
 * 契约与 `feature/wallet/WalletApi.kt` 同源（Dart `WalletApi`），但
 * `scripts/check-layer-deps.sh` 第 1/3 条禁止 feature → feature（源码 import 与 Gradle
 * 依赖都算违规），故按页面所需最小集在本模块内独立实现，**不做任何假数据兜底**：
 * 网络失败只写 [errorMessage]，余额/卡/流水保留上一次成功拉到的值。
 */
object WalletMockStore {
    var version by mutableIntStateOf(0)
        private set

    private fun bump() {
        version++
    }

    var loading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    private var balanceYuanValue by mutableStateOf("0.00")
    private var balanceFenValue by mutableIntStateOf(0)
    private var cardItems by mutableStateOf<List<WalletBankCard>>(emptyList())
    private var ledgerItems by mutableStateOf<List<WalletLedgerEntry>>(emptyList())

    fun balanceYuan(): String = balanceYuanValue

    fun balanceFen(): Int = balanceFenValue

    fun cards(): List<WalletBankCard> = cardItems

    fun ledger(): List<WalletLedgerEntry> = ledgerItems

    fun validateRecharge(amount: String): String? {
        if (amount.trim().isEmpty()) return "请输入金额"
        val yuan = amount.trim().toDoubleOrNull()
        if (yuan == null || yuan <= 0) return "请输入有效金额"
        return null
    }

    fun validateBindCard(bankName: String, cardLast4: String): String? {
        if (bankName.trim().isEmpty() || cardLast4.trim().length != 4) {
            return "请填写银行名与卡号后四位"
        }
        if (!cardLast4.trim().all { it.isDigit() }) return "卡号后四位须为数字"
        return null
    }

    /**
     * 拉取钱包汇总（余额 + 银行卡）与流水；汇总失败时保留旧余额/卡，
     * 流水独立降级（失败只记 [errorMessage]，不影响余额展示）。
     */
    suspend fun refresh(): Result<Unit> {
        loading = true
        val api = WalletStoreApi()
        var error: String? = null
        val summary = try {
            api.fetchSummary()
        } catch (e: Exception) {
            error = e.message ?: "加载钱包失败"
            null
        }
        summary?.let {
            balanceYuanValue = it.balance
            balanceFenValue = it.balanceFen
            cardItems = it.cards
        }
        val ledger = try {
            api.fetchLedger()
        } catch (e: Exception) {
            if (error == null) error = e.message ?: "加载流水失败"
            null
        }
        ledger?.let { ledgerItems = it }
        val failure = error
        errorMessage = failure
        loading = false
        bump()
        return if (failure == null) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException(failure))
        }
    }

    /**
     * 充值。[amount] 是**元串**（例 `"12.34"`），不是分；
     * [channel] 1=支付宝 2=微信 3=银行卡（Dart `wallet_controller.dart:23`），
     * 仅银行卡渠道带 [cardId]（Dart `:76`）。成功后重新拉取，失败返回 false。
     */
    suspend fun recharge(amount: String, channel: Int, cardId: Int?): Boolean {
        val invalid = validateRecharge(amount)
        if (invalid != null) {
            errorMessage = invalid
            bump()
            return false
        }
        return try {
            WalletStoreApi().recharge(
                amount = amount.trim(),
                channel = channel,
                cardId = cardId.takeIf { channel == 3 },
            )
            errorMessage = null
            refresh()
            true
        } catch (e: Exception) {
            errorMessage = e.message ?: "充值失败"
            bump()
            false
        }
    }

    /** 绑卡；成功后重新拉取卡列表，失败返回 null。 */
    suspend fun bindCard(bankName: String, cardLast4: String): WalletBankCard? {
        val invalid = validateBindCard(bankName, cardLast4)
        if (invalid != null) {
            errorMessage = invalid
            bump()
            return null
        }
        return try {
            val card = WalletStoreApi().bindCard(bankName.trim(), cardLast4.trim())
            errorMessage = null
            refresh()
            card
        } catch (e: Exception) {
            errorMessage = e.message ?: "绑卡失败"
            bump()
            null
        }
    }

    /** 设为默认卡；成功后重新拉取，失败返回 false。 */
    suspend fun setDefaultCard(cardId: Int): Boolean = try {
        WalletStoreApi().setDefaultCard(cardId)
        errorMessage = null
        refresh()
        true
    } catch (e: Exception) {
        errorMessage = e.message ?: "设置默认卡失败"
        bump()
        false
    }

    /** 解绑银行卡；成功后重新拉取，失败返回 false。 */
    suspend fun deleteCard(cardId: Int): Boolean = try {
        WalletStoreApi().deleteCard(cardId)
        errorMessage = null
        refresh()
        true
    } catch (e: Exception) {
        errorMessage = e.message ?: "解除绑定失败"
        bump()
        false
    }

    /** Test helper — 只清空内存缓存。 */
    fun resetForTests() {
        balanceYuanValue = "0.00"
        balanceFenValue = 0
        cardItems = emptyList()
        ledgerItems = emptyList()
        loading = false
        errorMessage = null
        bump()
    }
}

/** `GET /api/v1/wallet` 的响应体（Dart `WalletSummary`）。 */
@Serializable
private data class WalletSummaryPayload(
    val balance: String = "0.00",
    @SerialName("balance_fen") val balanceFen: Int = 0,
    val cards: List<WalletBankCard> = emptyList(),
)

/** `POST /api/v1/wallet/cards` 的请求体（契约表 #86，Dart 恒发四键）。 */
@Serializable
private data class WalletBindCardBody(
    @SerialName("bank_name") val bankName: String,
    @SerialName("card_last4") val cardLast4: String,
    @SerialName("holder_name") val holderName: String = "",
    @SerialName("is_default") val isDefault: Boolean = false,
)

/**
 * `POST /api/v1/wallet/recharge` 的请求体（契约表 #89）。
 * [amount] 是十进制元串，`card_id` 为 null 时不发键（`NetworkJson` 关闭了 explicitNulls）。
 */
@Serializable
private data class WalletRechargeBody(
    val amount: String,
    val channel: Int,
    @SerialName("card_id") val cardId: Int? = null,
)

/**
 * 钱包 HTTP 契约（`/api/v1/wallet` 族）。只被 [WalletMockStore] 使用，
 * 端点/语义与 `feature/wallet/WalletApi.kt` 保持一致（见该文件 KDoc）。
 */
private class WalletStoreApi(
    private val client: ApiClient = NetworkFacade.api(),
) {
    suspend fun fetchSummary(): WalletSummaryPayload =
        client.getApi<WalletSummaryPayload>(WalletPaths.BASE).requireWalletData("加载钱包失败")

    /** `GET /wallet/ledger?limit=&offset=`；`data.items[]`（无 `list`/`pagination`）。 */
    suspend fun fetchLedger(limit: Int = LEDGER_LIMIT, offset: Int = 0): List<WalletLedgerEntry> {
        val response = client.getListPage<WalletLedgerEntry>(
            path = WalletPaths.LEDGER,
            query = queryOf("limit" to limit.toString(), "offset" to offset.toString()),
        )
        val page = response.data
        if (page != null && (response.isSuccess || response.code == NetworkCodes.PASSTHROUGH)) {
            return page.items
        }
        return response.requireWalletData("加载流水失败").items
    }

    suspend fun bindCard(bankName: String, cardLast4: String): WalletBankCard {
        val response = client.postApi<WalletBindCardBody, WalletBankCard>(
            path = WalletPaths.CARDS,
            body = WalletBindCardBody(bankName = bankName, cardLast4 = cardLast4),
        )
        return response.requireWalletData("绑卡失败")
    }

    /** 动作式 POST，无 body（Dart `:56-68`）。 */
    suspend fun setDefaultCard(cardId: Int) {
        client.postApiDecoded<JsonObject>(WalletPaths.cardDefault(cardId))
            .requireWalletData("设置默认卡失败")
    }

    suspend fun deleteCard(cardId: Int) {
        client.deleteApi<JsonObject>(WalletPaths.card(cardId))
            .requireWalletData("删除银行卡失败")
    }

    suspend fun recharge(amount: String, channel: Int, cardId: Int?) {
        client.postApi<WalletRechargeBody, JsonObject>(
            path = WalletPaths.RECHARGE,
            body = WalletRechargeBody(amount = amount, channel = channel, cardId = cardId),
        ).requireWalletData("充值失败")
    }

    private companion object {
        const val LEDGER_LIMIT = 20
    }
}

private object WalletPaths {
    const val BASE = "/api/v1/wallet"
    const val LEDGER = "$BASE/ledger"
    const val CARDS = "$BASE/cards"
    const val RECHARGE = "$BASE/recharge"

    fun card(cardId: Int): String = "$CARDS/$cardId"

    fun cardDefault(cardId: Int): String = "$CARDS/$cardId/default"
}

/** Dart `WalletApi._data`：`code == 0` 且 `data != null` 才成功，否则抛业务异常。 */
private fun <T> ApiResponse<T>.requireWalletData(fallback: String): T {
    val payload = data
    if (!isSuccess || payload == null) {
        throw NetworkError.Business(code, message?.takeIf { it.isNotEmpty() } ?: fallback)
    }
    return payload
}

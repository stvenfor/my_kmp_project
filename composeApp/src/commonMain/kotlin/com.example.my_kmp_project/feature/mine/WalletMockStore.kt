package com.example.my_kmp_project.feature.mine

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

internal data class WalletBankCard(
    val cardId: Int,
    val bankName: String,
    val cardLast4: String,
    val holderName: String = "",
    val isDefault: Boolean = false,
) {
    val display: String get() = "$bankName(****$cardLast4)"
}

internal data class WalletLedgerEntry(
    val ledgerId: Int,
    val deltaFen: Int,
    val balanceFen: Int,
    val reason: String,
    val refId: String = "",
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
 * Flutter WalletController + WalletApi mock (balance / cards / ledger / recharge / bind).
 */
internal object WalletMockStore {
    var version by mutableIntStateOf(0)
        private set

    private fun bump() {
        version++
    }

    private var balanceFen = 12_800 // ¥128.00
    private var cardSeq = 2
    private var ledgerSeq = 1
    private val cards = mutableListOf(
        WalletBankCard(1, "工商银行", "6172", "qa_user", isDefault = true),
        WalletBankCard(2, "建设银行", "8890", "qa_user"),
    )
    private val ledger = mutableListOf(
        WalletLedgerEntry(1, 5_000, 12_800, "recharge", "r-seed"),
    )

    fun balanceYuan(): String {
        val v = balanceFen
        return "${v / 100}.${(v % 100).toString().padStart(2, '0')}"
    }

    fun balanceFen(): Int = balanceFen

    fun cards(): List<WalletBankCard> = cards.toList()

    fun ledger(): List<WalletLedgerEntry> = ledger.toList()

    fun validateRecharge(amount: String): String? {
        if (amount.trim().isEmpty()) return "请输入金额"
        val yuan = amount.trim().toDoubleOrNull()
        if (yuan == null || yuan <= 0) return "请输入有效金额"
        return null
    }

    /** channel: 1=wechat 2=alipay 3=card (cardId required for 3). */
    fun recharge(amount: String, channel: Int, cardId: Int?): WalletLedgerEntry? {
        if (validateRecharge(amount) != null) return null
        if (channel == 3 && (cardId == null || cards.none { it.cardId == cardId })) return null
        val yuan = amount.trim().toDouble()
        val fen = (yuan * 100).toInt()
        balanceFen += fen
        ledgerSeq += 1
        val entry = WalletLedgerEntry(
            ledgerId = ledgerSeq,
            deltaFen = fen,
            balanceFen = balanceFen,
            reason = "recharge",
            refId = "ch-$channel",
        )
        ledger.add(0, entry)
        bump()
        return entry
    }

    fun validateBindCard(bankName: String, cardLast4: String): String? {
        if (bankName.trim().isEmpty() || cardLast4.trim().length != 4) {
            return "请填写银行名与卡号后四位"
        }
        if (!cardLast4.trim().all { it.isDigit() }) return "卡号后四位须为数字"
        return null
    }

    fun bindCard(bankName: String, cardLast4: String): WalletBankCard? {
        if (validateBindCard(bankName, cardLast4) != null) return null
        cardSeq += 1
        val card = WalletBankCard(
            cardId = cardSeq,
            bankName = bankName.trim(),
            cardLast4 = cardLast4.trim(),
            isDefault = cards.isEmpty(),
        )
        cards += card
        bump()
        return card
    }

    fun setDefaultCard(cardId: Int): Boolean {
        if (cards.none { it.cardId == cardId }) return false
        for (i in cards.indices) {
            val c = cards[i]
            cards[i] = c.copy(isDefault = c.cardId == cardId)
        }
        bump()
        return true
    }

    fun deleteCard(cardId: Int): Boolean {
        val removed = cards.removeAll { it.cardId == cardId }
        if (removed && cards.isNotEmpty() && cards.none { it.isDefault }) {
            cards[0] = cards[0].copy(isDefault = true)
        }
        if (removed) bump()
        return removed
    }

    fun resetForTests() {
        balanceFen = 12_800
        cardSeq = 2
        ledgerSeq = 1
        cards.clear()
        cards += listOf(
            WalletBankCard(1, "工商银行", "6172", "qa_user", isDefault = true),
            WalletBankCard(2, "建设银行", "8890", "qa_user"),
        )
        ledger.clear()
        ledger += WalletLedgerEntry(1, 5_000, 12_800, "recharge", "r-seed")
        bump()
    }
}

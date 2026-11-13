package com.example.my_kmp_project.feature.wallet

import com.example.my_kmp_project.core.network.NetworkJson
import kotlinx.serialization.encodeToString
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * C5 金额口径回归：后端 `docs/placeholder-inventory.md` §28「金额三套口径并存」。
 * 本模块 `WalletRechargeRequest.amount` 是 B decimal-as-string → wire 上 JSON string，
 * 与 `PrepayRequest.amountFen`（JSON number）刻意不同；后端收敛口径时三处一起改。
 */
class WalletMoneyShapeTest {

    @Test
    fun rechargeAmountIsDecimalAsJsonString() {
        val json = NetworkJson.encodeToString(
            WalletRechargeRequest(amount = "12.34", channel = 1),
        )
        assertTrue(json.contains("\"amount\":\"12.34\""), json)
        assertFalse(json.contains("\"amount\":12"), json)
        // card_id 为 null 时省略（explicitNulls=false），不影响 amount 形状
        assertFalse(json.contains("card_id"), json)
    }
}

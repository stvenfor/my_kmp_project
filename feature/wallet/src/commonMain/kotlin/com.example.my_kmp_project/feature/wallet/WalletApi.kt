package com.example.my_kmp_project.feature.wallet

import com.example.my_kmp_project.core.network.ApiClient
import com.example.my_kmp_project.core.network.ApiResponse
import com.example.my_kmp_project.core.network.NetworkCodes
import com.example.my_kmp_project.core.network.NetworkError
import com.example.my_kmp_project.core.network.NetworkFacade
import com.example.my_kmp_project.core.network.decodeListPage
import com.example.my_kmp_project.core.network.deleteApi
import com.example.my_kmp_project.core.network.getApi
import com.example.my_kmp_project.core.network.getListPage
import com.example.my_kmp_project.core.network.postApi
import com.example.my_kmp_project.core.network.queryOf
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * 钱包 HTTP 契约层（契约表 #84–#89；Dart
 * `features/wallet/lib/wallet/api/wallet_api.dart`，my_ai_project d7876a1）。
 *
 * `GET /api/v1/wallet` 在 Dart 里有 **3 个调用点、2 种形状**（契约表 #84）：
 * - [fetchSummary]：钱包页读完整汇总（`balance` + `balance_fen` + `cards`）；
 * - [fetchBalance]：商城详情（`mall_order_detail_controller.dart:75`）与会员续费
 *   （`membership_renew_controller.dart:145`）只取 `data.balance`，
 *   **失败一律吞掉且不阻塞主流程**——本方法因此返回可空且永不抛。
 */
public class WalletApi(
    public val client: ApiClient = NetworkFacade.api(),
) {
    /** #84 完整钱包汇总：`GET /api/v1/wallet`。 */
    public suspend fun fetchSummary(): WalletSummary {
        val response = client.getApi<WalletSummary>(WalletApiPaths.BASE)
        return response.requireData("加载钱包失败")
    }

    /**
     * #84 的余额专用出口：**只读 `data.balance`**，任何失败（业务码非 0 / 传输异常 /
     * `data` 缺失）都吞掉返回 null，调用方保留上一次展示值。
     *
     * Dart 侧 `walletBalance.value = data['balance']?.toString() ?? '0.00'`，
     * 且外层 `try/catch(_)` ——所以这里成功但缺字段时回 `'0.00'`（[WalletSummary] 的默认值）。
     */
    public suspend fun fetchBalance(): String? =
        runCatching {
            client.getApi<WalletSummary>(WalletApiPaths.BASE).data?.balance
        }.getOrNull()

    /**
     * #85 流水：`GET /api/v1/wallet/ledger?limit=20&offset=0`。
     *
     * 响应是 **`data.items[]` 且无 `list`/`pagination`**（契约表 §3 C 型），
     * 因此走 [decodeListPage]（它同时吃 `list` 与 `items`）后只取 `items`，
     * 分页游标由调用方按 limit/offset 自行推进。
     *
     * Dart 的转换器（`wallet_api.dart:32-47`）在 body **没有 `code` 键**时合成
     * `code = 0` 并照常返回；[com.example.my_kmp_project.core.network.ApiEnvelopeParser]
     * 把同一情况记为 [NetworkCodes.PASSTHROUGH]（且此时 `data` 必然已解出，说明 body
     * 确实是 JSON object），故下面按 Dart 语义放行，其余非成功码照旧抛出。
     *
     * 已知偏差（记录勿隐瞒）：Dart 转换器在 `data` 不是 map 时还会退回去读**根上**的
     * `items`；[decodeListPage] 只看 `data`，这种畸形 body 在 KMP 侧得到空列表。
     */
    public suspend fun fetchLedger(limit: Int = LEDGER_LIMIT, offset: Int = 0): List<WalletLedgerEntry> {
        val response = client.getListPage<WalletLedgerEntry>(
            path = WalletApiPaths.LEDGER,
            query = queryOf("limit" to limit.toString(), "offset" to offset.toString()),
        )
        val page = response.data
        if (page != null && (response.isSuccess || response.code == NetworkCodes.PASSTHROUGH)) {
            return page.items
        }
        return response.requireData("加载流水失败").items
    }

    /** #86 绑卡：`POST /api/v1/wallet/cards {bank_name, card_last4, holder_name:'', is_default:false}`。 */
    public suspend fun bindCard(
        bankName: String,
        cardLast4: String,
        holderName: String = "",
        isDefault: Boolean = false,
    ): BankCard {
        val response = client.postApi<WalletBindCardRequest, BankCard>(
            path = WalletApiPaths.CARDS,
            body = WalletBindCardRequest(
                bankName = bankName,
                cardLast4 = cardLast4,
                holderName = holderName,
                isDefault = isDefault,
            ),
        )
        return response.requireData("绑卡失败")
    }

    /**
     * #87 设默认卡：`POST /api/v1/wallet/cards/{cardId}/default`，**无 body**（动作 POST，不是 PUT），
     * 响应是原始 Map（Dart 丢弃，但 `_data` 仍要求 `data` 非空）。
     */
    public suspend fun setDefaultCard(cardId: Int) {
        val body: String = ""
        val response: ApiResponse<JsonObject> = client.postApi(WalletApiPaths.cardDefault(cardId), body) {
            el: JsonElement? -> el as? JsonObject
        }
        response.requireData("设置默认卡失败")
    }

    /** #88 删卡：`DELETE /api/v1/wallet/cards/{cardId}`，响应是原始 Map。 */
    public suspend fun deleteCard(cardId: Int) {
        val response = client.deleteApi<JsonObject>(WalletApiPaths.card(cardId))
        response.requireData("删除银行卡失败")
    }

    /**
     * #89 充值：`POST /api/v1/wallet/recharge`。
     *
     * [amount] 是**十进制元串**（例 `"12.34"`），不是分——这是本表最容易被写错的一行。
     */
    public suspend fun recharge(amount: String, channel: Int, cardId: Int? = null): WalletRechargeResult {
        val response = client.postApi<WalletRechargeRequest, WalletRechargeResult>(
            path = WalletApiPaths.RECHARGE,
            body = WalletRechargeRequest(amount = amount, channel = channel, cardId = cardId),
        )
        return response.requireData("充值失败")
    }

    public companion object {
        /** Dart `WalletApi.fetchLedger` 的默认 `limit`。 */
        public const val LEDGER_LIMIT: Int = 20
    }
}

/** 钱包路径常量（`/api/v1/wallet` 族；路径参数按 Dart 原样插值）。 */
public object WalletApiPaths {
    public const val BASE: String = "/api/v1/wallet"
    public const val LEDGER: String = "$BASE/ledger"
    public const val CARDS: String = "$BASE/cards"
    public const val RECHARGE: String = "$BASE/recharge"

    public fun card(cardId: Int): String = "$CARDS/$cardId"

    public fun cardDefault(cardId: Int): String = "$CARDS/$cardId/default"
}

/** Dart `WalletApi._data`：`code == 0` 且 `data != null` 才成功，否则抛业务异常。 */
public fun <T> ApiResponse<T>.requireData(fallback: String): T {
    val payload = data
    if (!isSuccess || payload == null) {
        throw NetworkError.Business(code, message?.takeIf { it.isNotEmpty() } ?: fallback)
    }
    return payload
}


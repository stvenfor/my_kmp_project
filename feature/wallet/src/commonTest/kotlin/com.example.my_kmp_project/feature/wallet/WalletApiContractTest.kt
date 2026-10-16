package com.example.my_kmp_project.feature.wallet

import com.example.my_kmp_project.core.network.ApiClient
import com.example.my_kmp_project.core.network.ApiEnvelopeParser
import com.example.my_kmp_project.core.network.ApiResponse
import com.example.my_kmp_project.core.network.HttpTextResponse
import com.example.my_kmp_project.core.network.NetworkCodes
import com.example.my_kmp_project.core.network.NetworkError
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 钱包契约回归（契约表 #84–#89；Dart
 * `features/wallet/lib/wallet/api/wallet_api.dart` +
 * `wallet/model/wallet_models.dart`，my_ai_project d7876a1）。
 */
class WalletApiContractTest {

    // ---- #84 GET /wallet：两种形状 ----

    @Test
    fun summaryDecodesBalanceBalanceFenAndCards() = runBlocking {
        val fake = FakeWalletClient { _, _, _, _ -> WALLET_JSON }
        val summary = WalletApi(fake).fetchSummary()

        assertEquals(listOf("GET /api/v1/wallet"), fake.calls.map { "${it.verb} ${it.path}" })
        assertEquals(emptyMap(), fake.calls.single().query)
        // balance 是字符串，不做 Double 化
        assertEquals("128.50", summary.balance)
        assertEquals(12_850, summary.balanceFen)
        assertEquals(2, summary.cards.size)

        val card = summary.cards.first()
        assertEquals(3, card.cardId)
        assertEquals("招商银行", card.bankName)
        assertEquals("1234", card.cardLast4)
        assertEquals("张三", card.holderName)
        assertTrue(card.isDefault)
        assertEquals("招商银行(****1234)", card.display)
        assertFalse(summary.cards[1].isDefault)
    }

    @Test
    fun balanceOnlyAccessorSwallowsFailuresLikeDartNonBlockingRead() = runBlocking {
        // 成功：只取 data.balance
        val ok = FakeWalletClient { _, _, _, _ -> WALLET_JSON }
        assertEquals("128.50", WalletApi(ok).fetchBalance())

        // 业务失败（Dart `try/catch(_)`：余额展示失败不挡支付）
        val biz = FakeWalletClient { _, _, _, _ -> """{"code":403,"message":"无权限","data":null}""" }
        assertNull(WalletApi(biz).fetchBalance())

        // `data` 缺失 ⇒ 读不到 balance ⇒ null（Dart 保留旧值）
        val noData = FakeWalletClient { _, _, _, _ -> """{"code":0,"message":"ok","data":null}""" }
        assertNull(WalletApi(noData).fetchBalance())

        // 传输异常也必须吞掉
        val transport = FakeWalletClient { _, _, _, _ -> throw NetworkError.Transport("GET failed") }
        assertNull(WalletApi(transport).fetchBalance())

        // 完整汇总入口则必须抛（两种形状的失败语义不同）
        val error = runCatching { WalletApi(biz).fetchSummary() }.exceptionOrNull()
        assertTrue(error is NetworkError.Business)
        assertEquals("无权限", error.message)
    }

    @Test
    fun balanceDefaultsToZeroWhenBalanceKeyMissing() = runBlocking {
        val fake = FakeWalletClient { _, _, _, _ -> """{"code":0,"message":"ok","data":{"cards":[]}}""" }
        // Dart: data['balance']?.toString() ?? '0.00'
        assertEquals("0.00", WalletApi(fake).fetchBalance())
    }

    // ---- #85 GET /wallet/ledger：只有 items，无 list/pagination ----

    @Test
    fun ledgerSendsLimitOffsetQueryAndReadsItemsOnly() = runBlocking {
        val fake = FakeWalletClient { _, _, _, _ -> LEDGER_JSON }
        val entries = WalletApi(fake).fetchLedger()

        assertEquals("/api/v1/wallet/ledger", fake.calls.single().path)
        assertEquals(mapOf("limit" to "20", "offset" to "0"), fake.calls.single().query)
        assertEquals(3, entries.size)

        val recharge = entries[0]
        assertEquals(71, recharge.ledgerId)
        assertEquals("+100.00", recharge.deltaYuan)
        assertEquals(22_850, recharge.balanceFen)
        assertEquals("充值", recharge.reasonLabel)
        assertEquals("rc_1", recharge.refId)

        // 消费为负分：'-25.00'，reason 未知名原样展示
        val pay = entries[1]
        assertEquals("-25.00", pay.deltaYuan)
        assertEquals("商城支付", pay.reasonLabel)
        assertEquals("mall_refund", entries[2].reason)
        assertEquals("+0.05", entries[2].deltaYuan)
        assertEquals("商城退款入账", entries[2].reasonLabel)
    }

    @Test
    fun ledgerOffsetAdvancesExplicitly() = runBlocking {
        val fake = FakeWalletClient { _, _, _, _ -> LEDGER_JSON }
        WalletApi(fake).fetchLedger(limit = 20, offset = 40)
        assertEquals(mapOf("limit" to "20", "offset" to "40"), fake.calls.single().query)
    }

    @Test
    fun ledgerBusinessFailureThrows() = runBlocking {
        val api = WalletApi(FakeWalletClient { _, _, _, _ -> """{"code":500,"message":"流水服务异常"}""" })
        val error = runCatching { api.fetchLedger() }.exceptionOrNull()
        assertTrue(error is NetworkError.Business)
        assertEquals("流水服务异常", error.message)
    }

    @Test
    fun ledgerSynthesizesSuccessWhenBodyHasNoCodeKey() = runBlocking {
        // Dart `wallet_api.dart:41-46`：`map.containsKey('code')` 为假时合成 code=0。
        val fake = FakeWalletClient { _, _, _, _ -> LEDGER_NO_CODE_JSON }
        val entries = WalletApi(fake).fetchLedger()
        assertEquals(2, entries.size)
        assertEquals(71, entries.first().ledgerId)

        // 但畸形 body（连 data 都解不出来）仍然抛，不伪装成空列表成功。
        val malformed = WalletApi(FakeWalletClient { _, _, _, _ -> "not json at all" })
        val error = runCatching { malformed.fetchLedger() }.exceptionOrNull()
        assertTrue(error is NetworkError.Business)
        assertEquals(NetworkCodes.PASSTHROUGH, error.code)
    }

    // ---- #86 / #87 / #88 银行卡 ----

    @Test
    fun bindCardAlwaysSendsHolderNameAndIsDefault() = runBlocking {
        val fake = FakeWalletClient { _, _, _, _ -> BIND_CARD_JSON }
        val card = WalletApi(fake).bindCard(bankName = "工商银行", cardLast4 = "5678")

        assertEquals("/api/v1/wallet/cards", fake.calls.single().path)
        assertEquals("""{"bank_name":"工商银行","card_last4":"5678","holder_name":"","is_default":false}""", fake.calls.single().body)
        assertEquals(9, card.cardId)
        assertEquals("工商银行(****5678)", card.display)
    }

    @Test
    fun setDefaultCardIsPostWithoutBodyAndNeedsDataMap() = runBlocking {
        val fake = FakeWalletClient { _, _, _, _ -> """{"code":0,"message":"ok","data":{}}""" }
        WalletApi(fake).setDefaultCard(cardId = 3)
        assertEquals(listOf(FakeWalletClient.Call("POST", "/api/v1/wallet/cards/3/default")), fake.calls)

        val noData = WalletApi(FakeWalletClient { _, _, _, _ -> """{"code":0,"data":null}""" })
        val error = runCatching { noData.setDefaultCard(cardId = 3) }.exceptionOrNull()
        assertTrue(error is NetworkError.Business)
        assertEquals("设置默认卡失败", error.message)
    }

    @Test
    fun deleteCardIsDeleteOnCardPath() = runBlocking {
        val fake = FakeWalletClient { _, _, _, _ -> """{"code":0,"message":"ok","data":{"deleted":true}}""" }
        WalletApi(fake).deleteCard(cardId = 3)
        assertEquals(listOf(FakeWalletClient.Call("DELETE", "/api/v1/wallet/cards/3")), fake.calls)

        val forbidden = WalletApi(FakeWalletClient { _, _, _, _ -> """{"code":403,"message":"无权限","data":null}""" })
        assertEquals("无权限", runCatching { forbidden.deleteCard(cardId = 3) }.exceptionOrNull()?.message)
    }

    // ---- #89 POST /wallet/recharge：amount 是十进制元串 ----

    @Test
    fun rechargeSendsAmountAsDecimalStringNotFen() = runBlocking {
        val fake = FakeWalletClient { _, _, _, _ -> RECHARGE_JSON }
        val result = WalletApi(fake).recharge(amount = "12.34", channel = 1)

        assertEquals("/api/v1/wallet/recharge", fake.calls.single().path)
        // 关键 parity：不是 1234 分，而是定价串 "12.34"
        assertEquals("""{"amount":"12.34","channel":1}""", fake.calls.single().body)
        assertEquals("112.34", result.balance)
        assertEquals(11_234, result.balanceFen)
        assertEquals(1_234, result.deltaFen)
    }

    @Test
    fun rechargeOmitsCardIdOnlyWhenNull() = runBlocking {
        val withCard = FakeWalletClient { _, _, _, _ -> RECHARGE_JSON }
        WalletApi(withCard).recharge(amount = "50.00", channel = 2, cardId = 3)
        assertEquals("""{"amount":"50.00","channel":2,"card_id":3}""", withCard.calls.single().body)

        val noCard = FakeWalletClient { _, _, _, _ -> RECHARGE_JSON }
        WalletApi(noCard).recharge(amount = "50.00", channel = 2, cardId = null)
        assertFalse(noCard.calls.single().body.contains("card_id"))
    }

    @Test
    fun rechargeFailureUsesServerMessageThenFallback() = runBlocking {
        val api = WalletApi(FakeWalletClient { _, _, _, _ -> """{"code":4002,"message":"余额不足","data":null}""" })
        val error = runCatching { api.recharge(amount = "1.00", channel = 1) }.exceptionOrNull()
        assertTrue(error is NetworkError.Business)
        assertEquals("余额不足", error.message)

        val silent = WalletApi(FakeWalletClient { _, _, _, _ -> """{"code":4002,"data":null}""" })
        assertEquals("充值失败", runCatching { silent.recharge("1.00", 1) }.exceptionOrNull()?.message)
    }

    // ---- 夹具（BFF 真实信封）----

    companion object {
        private val WALLET_JSON = """
            {"code":0,"message":"success","data":{
              "balance":"128.50","balance_fen":12850,
              "cards":[
                {"card_id":3,"bank_name":"招商银行","card_last4":"1234","holder_name":"张三","is_default":true},
                {"card_id":4,"bank_name":"中国银行","card_last4":"9007","is_default":false}
              ]
            },"timestamp":1760000000}
        """.trimIndent()

        /** #85：`data.items[]`，**没有** `list` 也没有 `pagination`。 */
        private val LEDGER_JSON = """
            {"code":0,"message":"success","data":{
              "items":[
                {"ledger_id":71,"delta_fen":10000,"balance_fen":22850,"reason":"recharge","ref_id":"rc_1"},
                {"ledger_id":70,"delta_fen":-2500,"balance_fen":12850,"reason":"mall_pay","ref_id":"9001"},
                {"ledger_id":69,"delta_fen":5,"balance_fen":12855,"reason":"mall_refund","ref_id":""}
              ]
            }}
        """.trimIndent()

        /** #85 的 Dart 特殊分支：**无 `code` 键** ⇒ 转换器合成 `code = 0`。 */
        private val LEDGER_NO_CODE_JSON = """
            {"message":"success","data":{
              "items":[
                {"ledger_id":71,"delta_fen":10000,"balance_fen":22850,"reason":"recharge","ref_id":"rc_1"},
                {"ledger_id":70,"delta_fen":-2500,"balance_fen":12850,"reason":"mall_pay","ref_id":"9001"}
              ]
            }}
        """.trimIndent()

        private val BIND_CARD_JSON = """
            {"code":0,"message":"success","data":{
              "card_id":9,"bank_name":"工商银行","card_last4":"5678","holder_name":"","is_default":false
            }}
        """.trimIndent()

        private val RECHARGE_JSON = """
            {"code":0,"message":"success","data":{"balance":"112.34","balance_fen":11234,"delta_fen":1234}}
        """.trimIndent()
    }
}

/** 钱包侧假 client：记录调用并走真实 [ApiEnvelopeParser]。 */
public class FakeWalletClient(
    private val responder: (verb: String, path: String, query: Map<String, String>, body: String) -> String,
) : ApiClient {
    data class Call(
        val verb: String,
        val path: String,
        val query: Map<String, String> = emptyMap(),
        val body: String = "",
    )

    val calls = mutableListOf<Call>()

    private fun <T> parse(
        verb: String,
        path: String,
        query: Map<String, String>,
        body: String,
        parseData: (JsonElement?) -> T?,
    ): ApiResponse<T> {
        calls += Call(verb, path, query, body)
        return ApiEnvelopeParser.parse(
            raw = responder(verb, path, query, body),
            parseData = parseData,
            httpStatus = 200,
        )
    }

    override suspend fun getRaw(path: String, query: Map<String, String>): String {
        calls += Call("GET", path, query)
        return responder("GET", path, query, "")
    }

    override suspend fun getAbsoluteRaw(url: String): String {
        calls += Call("GET", url)
        return responder("GET", url, emptyMap(), "")
    }

    override suspend fun postRaw(path: String, body: String): String {
        calls += Call("POST", path, body = body)
        return responder("POST", path, emptyMap(), body)
    }

    override suspend fun postForm(path: String, fields: Map<String, String>): HttpTextResponse {
        calls += Call("POST-FORM", path)
        return HttpTextResponse(200, "")
    }

    override suspend fun putRaw(path: String, body: String): String {
        calls += Call("PUT", path, body = body)
        return responder("PUT", path, emptyMap(), body)
    }

    override suspend fun patchRaw(path: String, body: String): String {
        calls += Call("PATCH", path, body = body)
        return responder("PATCH", path, emptyMap(), body)
    }

    override suspend fun deleteRaw(path: String): String {
        calls += Call("DELETE", path)
        return responder("DELETE", path, emptyMap(), "")
    }

    override suspend fun <T> getApi(
        path: String,
        query: Map<String, String>,
        parseData: (JsonElement?) -> T?,
    ): ApiResponse<T> = parse("GET", path, query, "", parseData)

    override suspend fun <T> postApi(
        path: String,
        body: String,
        parseData: (JsonElement?) -> T?,
    ): ApiResponse<T> = parse("POST", path, emptyMap(), body, parseData)

    override suspend fun <T> putApi(
        path: String,
        body: String,
        parseData: (JsonElement?) -> T?,
    ): ApiResponse<T> = parse("PUT", path, emptyMap(), body, parseData)

    override suspend fun <T> patchApi(
        path: String,
        body: String,
        parseData: (JsonElement?) -> T?,
    ): ApiResponse<T> = parse("PATCH", path, emptyMap(), body, parseData)

    override suspend fun <T> deleteApi(
        path: String,
        query: Map<String, String>,
        parseData: (JsonElement?) -> T?,
    ): ApiResponse<T> = parse("DELETE", path, query, "", parseData)
}

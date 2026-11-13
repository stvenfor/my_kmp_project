package com.example.my_kmp_project.feature.pay

import com.example.my_kmp_project.core.network.NetworkJson
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * `POST /api/v1/payments/prepay` 契约回归（契约表 #95）。
 * 真相源：Go `my_go_study` payment 域（router.go:49 / handler.go:28 / service.go:66），
 * 逐条对照见 `notes/backend-contract-matrix.md`。
 */
class PaymentPrepayContractTest {

    @Test
    fun prepayPathMatchesBackendRoute() {
        assertEquals("/api/v1/payments/prepay", PaymentPrepayPaths.PREPAY)
    }

    @Test
    fun prepaySendsBackendRequiredKeysAndDecodesParams() = runBlocking {
        val fake = FakePayClient { _, _, _, _ -> PREPAY_JSON }
        val result = PaymentPrepayApi(fake).prepay(
            PrepayRequest(
                channel = "wechat",
                amountFen = 19_900,
                subject = "会员年费",
                refType = "membership",
                refId = "OT-1",
            ),
        )

        val call = fake.calls.single()
        assertEquals("POST", call.verb)
        assertEquals("/api/v1/payments/prepay", call.path)
        // Go handler.go:28 的五键与 wire 名；amount_fen 是 int64。
        assertEquals(
            """{"channel":"wechat","amount_fen":19900,"subject":"会员年费","ref_type":"membership","ref_id":"OT-1"}""",
            call.body,
        )
        assertEquals("wechat", result.channel)
        assertEquals("OT-1", result.outTradeNo)
        assertEquals("wx_prepay_id", result.params["prepayid"]?.jsonPrimitive?.content)
    }

    @Test
    fun prepayBusinessFailureThrowsWithFallbackCopy() = runBlocking {
        val api = PaymentPrepayApi(FakePayClient { _, _, _, _ -> """{"code":5001,"data":null}""" })
        assertEquals("预下单失败", runCatching { api.prepay(request()) }.exceptionOrNull()?.message)

        val serverCopy = PaymentPrepayApi(
            FakePayClient { _, _, _, _ -> """{"code":5001,"message":"渠道维护中","data":null}""" },
        )
        assertEquals("渠道维护中", runCatching { serverCopy.prepay(request()) }.exceptionOrNull()?.message)
    }

    @Test
    fun amountFenIsIntegerFenJsonNumber_notString() {
        // C5：后端金额三套口径中的 A 整数分 → wire 上必须是 JSON number（无引号）。
        // 与 MallOrderModels.amount / WalletModels.amount 的 JSON string 形状刻意不同；
        // 后端收敛口径时三处要一起改，见 PrepayRequest.amountFen 的 KDoc。
        val json = NetworkJson.encodeToString(request())
        // JSON number（无引号），不是 "19900"；与另两处的 JSON string 形状刻意不同。
        assertTrue(json.contains("\"amount_fen\":19900"), json)
        assertFalse(json.contains("\"amount_fen\":\""), json)
    }

    private fun request() = PrepayRequest("wechat", 19_900, "会员年费", "membership", "OT-1")

    companion object {
        /** Go `service.go:66` PrepayResult 经 `response.Success` 包成 data 的形态。 */
        private val PREPAY_JSON = """
            {"code":0,"message":"success","data":{
              "channel":"wechat","out_trade_no":"OT-1",
              "params":{"prepayid":"wx_prepay_id","timestamp":"1760000000"}
            }}
        """.trimIndent()
    }
}

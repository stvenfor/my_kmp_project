package com.example.my_kmp_project.feature.mine.calculator

import com.example.my_kmp_project.core.network.NetworkError
import com.example.my_kmp_project.core.network.NetworkJson
import com.example.my_kmp_project.feature.mine.MineRecordingApiClient
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 契约表 §2.11 #57-#58（Dart `purchase_calculator_api.dart` +
 * `purchase_calculator_models.dart`，d7876a1）的 JSON fixture 测试：
 * `data.items` 严格读取（否则「产品列表为空」）、quote body 的恒发/条件发、
 * 报价行解码。
 */
class PurchaseCalculatorApiTest {

    private val client = MineRecordingApiClient()
    private val api = PurchaseCalculatorApi(client)

    @Test
    fun productsDecodesItemsArrayAndSkipsNonObjectRows() = runBlocking {
        client.stub(
            """
            {"code":0,"message":"ok","data":{"items":[
              {"id":3,"name":"金融A","annual_rate_percent":4.35,
               "allowed_terms_months":[12,24,36],"min_down_payment_percent":20,
               "one_time_fee":1200,"subsidy_type":1,"subsidy_rate_cut_percent":0.5,
               "subsidy_amount_cut":800,"compulsory_insurance":950,
               "commercial_rate_percent":3.2},
              "垃圾行",
              {"id":4}
            ]},"timestamp":1760000000}
            """.trimIndent(),
        )
        val products = api.listProducts()
        assertEquals("/api/v1/purchase-calculator/products", client.last.path)
        assertEquals("GET", client.last.method)
        assertEquals(emptyMap<String, String>(), client.last.query)
        assertEquals(2, products.size)

        val first = products[0]
        assertEquals(3, first.resolvedId)
        assertEquals("金融A", first.resolvedName)
        assertEquals(4.35, first.resolvedAnnualRatePercent)
        assertEquals(listOf(12, 24, 36), first.resolvedAllowedTermsMonths)
        assertEquals(20.0, first.resolvedMinDownPaymentPercent)
        assertEquals(1, first.resolvedSubsidyType)
        assertEquals(3.2, first.resolvedCommercialRatePercent)
        // 缺字段的行按 Dart 的 `?? 0` / `?? []` 兜底，不抛
        // 但这一行**是带 `id: 4` 的**，Dart purchase_calculator_models.dart:23
        // `id: (json['id'] as num?)?.toInt() ?? 0` 照读该键 ⇒ 期望 4，不是缺省 0。
        assertEquals(4, products[1].resolvedId)
        assertEquals("", products[1].resolvedName)
        assertEquals(emptyList(), products[1].resolvedAllowedTermsMonths)
        // 真正缺键的数值字段才走 Dart 的 `?? 0`
        // （purchase_calculator_models.dart:25 annual_rate_percent、:30 subsidy_type）
        assertEquals(0.0, products[1].resolvedAnnualRatePercent)
        assertEquals(0, products[1].resolvedSubsidyType)
    }

    @Test
    fun productsThrowsWhenItemsIsNotAList() = runBlocking {
        val messages = mutableListOf<String>()

        // 1) data 为 null
        client.stub("""{"code":0,"message":"ok","data":null}""")
        messages += assertFailsWith<NetworkError.Business> { api.listProducts() }.message!!

        // 2) data 是 object 但没有 items
        client.stub("""{"code":0,"message":"ok","data":{"other":[]}}""")
        messages += assertFailsWith<NetworkError.Business> { api.listProducts() }.message!!

        // 3) items 不是 List（Dart :22 `items is! List`）
        client.stub("""{"code":0,"message":"ok","data":{"items":{"a":1}}}""")
        messages += assertFailsWith<NetworkError.Business> { api.listProducts() }.message!!

        // 4) items 是空数组：合法，返回空表（不抛）
        client.stub("""{"code":0,"message":"ok","data":{"items":[]}}""")
        assertTrue(api.listProducts().isEmpty())

        assertEquals(listOf("产品列表为空", "产品列表为空", "产品列表为空"), messages)
    }

    @Test
    fun productsPropagatesBusinessCodeMessage() = runBlocking {
        client.stub("""{"code":403,"message":"无权限","data":null}""")
        val err = assertFailsWith<NetworkError.Business> { api.listProducts() }
        assertEquals(403, err.code)
        assertEquals("无权限", err.message)
    }

    @Test
    fun quoteBodyAlwaysSendsModeBarePriceAndDisableCommercial() = runBlocking {
        client.stub("""{"code":0,"data":{"mode":"full","initial_payment":1000.0,"total_due":9000.0}}""")
        api.quote(
            PurchaseQuoteRequest(mode = "full", barePrice = 128800.5, disableCommercial = false),
        )
        assertEquals("POST", client.last.method)
        assertEquals("/api/v1/purchase-calculator/quote", client.last.path)
        val sent = NetworkJson.parseToJsonElement(client.last.body).jsonObject
        // Dart :42-50：三个恒发键 + 四个 `if (x != null)` 条件键
        assertEquals(setOf("mode", "bare_price", "disable_commercial"), sent.keys)
        assertEquals(128800.5, sent["bare_price"].toString().toDouble())
        assertEquals("false", sent["disable_commercial"].toString())
    }

    @Test
    fun quoteBodyIncludesConditionalsWhenPresent() = runBlocking {
        client.stub("""{"code":0,"data":{"mode":"finance"}}""")
        api.quote(
            PurchaseQuoteRequest(
                mode = "finance",
                barePrice = 100000.0,
                taxablePrice = 113000.0,
                productId = 3,
                downPayment = 20000.0,
                termMonths = 36,
                disableCommercial = true,
            ),
        )
        val sent = NetworkJson.parseToJsonElement(client.last.body).jsonObject
        assertEquals(
            setOf(
                "mode", "bare_price", "disable_commercial", "taxable_price",
                "product_id", "down_payment", "term_months",
            ),
            sent.keys,
        )
        assertEquals("3", sent["product_id"].toString())
        assertEquals("36", sent["term_months"].toString())
        assertEquals("true", sent["disable_commercial"].toString())
    }

    @Test
    fun quoteDecodesServerAuthoritativeAmountsAndLines() = runBlocking {
        client.stub(
            """
            {"code":0,"message":"ok","data":{
              "mode":"finance","product_id":3,"product_name":"金融A",
              "bare_price":128800.5,"taxable_price":145600.0,"down_payment":38800.5,
              "loan_amount":106800.0,"term_months":36,"effective_annual_rate_percent":4.1,
              "monthly_payment":3167.4,"total_interest":7322.4,"total_repayment":114122.4,
              "initial_payment":41850.5,"total_due":145850.5,
              "lines":[
                {"code":"down_payment","label":"首付","amount":38800.5},
                {"code":"one_time_fee","label":"一次性手续费","amount":1200.0},
                {"amount":5.5}
              ]
            }}
            """.trimIndent(),
        )
        val quote = api.quote(PurchaseQuoteRequest(mode = "finance", barePrice = 128800.5))
        assertEquals("finance", quote.resolvedMode)
        assertEquals(3, quote.resolvedProductId)
        assertEquals("金融A", quote.resolvedProductName)
        // effective_annual_rate_percent ↔ Dart 字段名 effectiveAnnualRate
        assertEquals(4.1, quote.resolvedEffectiveAnnualRate)
        assertEquals(3167.4, quote.resolvedMonthlyPayment)
        assertEquals(145850.5, quote.resolvedTotalDue)
        assertEquals(3, quote.resolvedLines.size)
        assertEquals("down_payment", quote.resolvedLines[0].resolvedCode)
        assertEquals("首付", quote.resolvedLines[0].resolvedLabel)
        // 缺 code/label 的行按 Dart `?? ''` 兜底
        assertEquals("", quote.resolvedLines[2].resolvedCode)
        assertEquals(5.5, quote.resolvedLines[2].resolvedAmount)
    }

    @Test
    fun quoteWithoutProductAndLinesKeepsDartDefaults() = runBlocking {
        client.stub("""{"code":0,"message":"ok","data":{"mode":"cash"}}""")
        val quote = api.quote(PurchaseQuoteRequest(mode = "cash", barePrice = 1000.0))
        assertNull(quote.resolvedProductId) // Dart 不给 product_id 默认值 → null
        assertEquals("", quote.resolvedProductName)
        assertEquals(0.0, quote.resolvedInitialPayment)
        assertEquals(emptyList(), quote.resolvedLines)
    }

    @Test
    fun quoteEmptyDataThrowsChineseFallback() = runBlocking {
        client.stub("""{"code":0,"message":"","data":null}""")
        val err = assertFailsWith<NetworkError.Business> {
            api.quote(PurchaseQuoteRequest(mode = "cash", barePrice = 1.0))
        }
        assertEquals("报价结果为空", err.message)
    }
}

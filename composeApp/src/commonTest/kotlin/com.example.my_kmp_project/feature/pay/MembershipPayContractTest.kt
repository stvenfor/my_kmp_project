package com.example.my_kmp_project.feature.pay

import com.example.my_kmp_project.core.network.ApiClient
import com.example.my_kmp_project.core.network.ApiEnvelopeParser
import com.example.my_kmp_project.core.network.ApiResponse
import com.example.my_kmp_project.core.network.HttpTextResponse
import com.example.my_kmp_project.core.network.NetworkError
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 会员 / 支付契约回归（契约表 #90–#95；Dart
 * `features/pay/lib/membership/payment/membership_api.dart` +
 * `membership_payment_gateway.dart` + `model/membership_models.dart`，
 * my_ai_project d7876a1）。
 */
class MembershipPayContractTest {

    // ---- #90 GET /membership/me ----

    @Test
    fun fetchMeDecodesCatalogAndEntitlements() = runBlocking {
        val fake = FakePayClient { _, _, _, _ -> ME_JSON }
        val me = PayApi(fake).fetchMe()

        assertEquals(listOf(FakePayClient.Call("GET", "/api/v1/membership/me")), fake.calls)
        assertEquals(3, me.catalog.size)

        val plans = me.catalogPlans
        // Dart `where((p) => p.id.isNotEmpty)`：plan_id 为空的行被丢掉
        assertEquals(2, plans.size)
        val svip = plans[0]
        assertEquals("plan-svip-y", svip.id)
        assertEquals(MembershipTier.Svip, svip.resolvedTier)
        // price 保持 BFF 字符串；priceValue 才做 Dart 的 double.tryParse
        assertEquals("198.00", svip.price)
        assertEquals(19_800, svip.priceFen)
        assertEquals(198.0, svip.priceValue)
        assertEquals(198.0, svip.originalPriceValue)
        assertEquals(12, svip.months)
        assertEquals("huawei_svip_year", svip.huaweiProductId)
        assertEquals("com.demo.svip.year", svip.appleProductId)
        assertTrue(svip.autoRenew)

        // price 缺失 ⇒ Dart 回退 fen/100（'19.9' 解析不出来时才走这条）
        val ai = plans[1]
        assertEquals(MembershipTier.AiSvip, ai.resolvedTier)
        assertEquals("", ai.price)
        assertEquals(29.9, ai.priceValue, 1e-9)

        assertEquals("立即续费", me.ctaFor(MembershipTier.Svip))
        assertEquals("开通", me.ctaFor(MembershipTier.AiSvip))
        // 权益行缺 cta ⇒ 空串（Dart 只在命中 tier 时覆盖 UI 文案）
        assertEquals("", MembershipEntitlement().cta)
    }

    @Test
    fun fetchMeTreatsNonMapDataAsEmptyLikeDartMapConverter() = runBlocking {
        // Dart `_mapConverter`：data 非 Map ⇒ `{}` ⇒ 目录空但不算失败
        val array = PayApi(FakePayClient { _, _, _, _ -> """{"code":0,"message":"ok","data":[]}""" })
        assertEquals(emptyList(), array.fetchMe().catalogPlans)

        val scalar = PayApi(FakePayClient { _, _, _, _ -> """{"code":0,"message":"ok","data":"x"}""" })
        assertEquals(0, scalar.fetchMe().catalog.size)

        // 但 `data` 键缺失 / 为 null 是 Dart `_requireData` 的抛点（`rawData == null` ⇒ data=null）
        val missing = PayApi(FakePayClient { _, _, _, _ -> """{"code":0}""" })
        val missingError = runCatching { missing.fetchMe() }.exceptionOrNull()
        assertTrue(missingError is NetworkError.Business)
        assertEquals("加载会员信息失败", missingError.message)

        val explicitNull = PayApi(FakePayClient { _, _, _, _ -> """{"code":0,"data":null}""" })
        assertEquals("加载会员信息失败", runCatching { explicitNull.fetchMe() }.exceptionOrNull()?.message)

        // Dart `_requireData` 用 `model?.message ?? fallback`：message 非空时**优先服务端文案**
        val serverCopy = PayApi(FakePayClient { _, _, _, _ -> """{"code":0,"message":"ok","data":null}""" })
        assertEquals("ok", runCatching { serverCopy.fetchMe() }.exceptionOrNull()?.message)
    }

    @Test
    fun fetchMeBusinessFailureThrows() = runBlocking {
        val api = PayApi(FakePayClient { _, _, _, _ -> """{"code":403,"message":"无权限","data":null}""" })
        val error = runCatching { api.fetchMe() }.exceptionOrNull()
        assertTrue(error is NetworkError.Business)
        assertEquals("无权限", error.message)

        val blank = PayApi(FakePayClient { _, _, _, _ -> """{"code":403,"data":null}""" })
        assertEquals("加载会员信息失败", runCatching { blank.fetchMe() }.exceptionOrNull()?.message)
    }

    // ---- #91 / #92 两步买断 ----

    @Test
    fun buyoutSendsPlanIdAndChannelString() = runBlocking {
        val fake = FakePayClient { _, _, _, _ -> BUYOUT_UNPAID_JSON }
        val buyout = PayApi(fake).buyout(planId = "plan-svip-y", channel = PaymentMethodType.WeChat.channelWire)

        assertEquals("/api/v1/membership/buyout", fake.calls.single().path)
        assertEquals("""{"plan_id":"plan-svip-y","channel":"wechat"}""", fake.calls.single().body)
        assertFalse(buyout.isPaid)
        assertEquals("pending", buyout.status)
        assertEquals(5_5001, buyout.orderId)
        assertEquals("wx_prepay_id", buyout.weChatPrepayId())
        assertEquals(
            listOf("appid" to "wx_app", "prepayid" to "wx_prepay_id", "timestamp" to "1760000000"),
            buyout.prepayEntries,
        )
    }

    @Test
    fun balanceChannelPaidShortCircuitsWithoutConfirm() = runBlocking {
        val fake = FakePayClient { _, _, _, _ -> BUYOUT_PAID_JSON }
        val sdkCalls = mutableListOf<MembershipBuyoutResponse>()

        val outcome = PayApi(fake).payBuyout(
            method = PaymentMethodType.Balance,
            planId = "plan-svip-y",
            sdkPay = { resp -> sdkCalls += resp; true },
        )

        assertEquals(MembershipPayStatus.Success, outcome.status)
        assertTrue(outcome.isSuccess)
        // status == 'paid' ⇒ 不再打 confirm，也不碰 SDK
        assertEquals(listOf("POST /api/v1/membership/buyout"), fake.calls.map { "${it.verb} ${it.path}" })
        assertEquals(emptyList(), sdkCalls)
    }

    @Test
    fun wechatFlowCallsConfirmOnlyAfterSdkSuccess() = runBlocking {
        val fake = FakePayClient { _, path, _, _ -> if (path.endsWith("/confirm")) CONFIRM_JSON else BUYOUT_UNPAID_JSON }
        var sdkHit = false

        val outcome = PayApi(fake).payBuyout(
            method = PaymentMethodType.WeChat,
            planId = "plan-svip-y",
            sdkPay = { sdkHit = true; true },
        )

        assertTrue(sdkHit)
        assertEquals(MembershipPayStatus.Success, outcome.status)
        // 两步：buyout → confirm（#95 的 /payments/prepay 是 Dart 死常量，不能被调出来）
        assertEquals(
            listOf("POST /api/v1/membership/buyout", "POST /api/v1/membership/buyout/confirm"),
            fake.calls.map { "${it.verb} ${it.path}" },
        )
        assertEquals("""{"order_id":55001}""", fake.calls.last().body)
    }

    @Test
    fun sdkFailureKeepsConfirmBack() = runBlocking {
        val fake = FakePayClient { _, _, _, _ -> BUYOUT_UNPAID_JSON }
        val outcome = PayApi(fake).payBuyout(
            method = PaymentMethodType.Alipay,
            planId = "plan-svip-y",
            sdkPay = { false },
        )
        assertEquals(MembershipPayStatus.SdkNotSucceeded, outcome.status)
        assertEquals(55_001, outcome.orderId)
        assertEquals(1, fake.calls.size)
        assertFalse(fake.calls.any { it.path.contains("/confirm") })
    }

    @Test
    fun orderIdZeroSilentlySkipsConfirmAfterSdkSuccess() = runBlocking {
        // Dart: `if (orderId > 0) await _api.confirmBuyout(...)` —— 静默跳过，不报错
        val fake = FakePayClient { _, _, _, _ -> """{"code":0,"message":"ok","data":{"status":"pending","order_id":0,"prepay_params":{}}}""" }
        val outcome = PayApi(fake).payBuyout(
            method = PaymentMethodType.WeChat,
            planId = "plan-x",
            sdkPay = { true },
        )
        assertEquals(MembershipPayStatus.Success, outcome.status)
        assertEquals(0, outcome.orderId)
        assertEquals(listOf("POST /api/v1/membership/buyout"), fake.calls.map { "${it.verb} ${it.path}" })
    }

    @Test
    fun huaweiAndAppleNeverUseBuyoutChannel() = runBlocking {
        val fake = FakePayClient { _, _, _, _ -> error("must not call HTTP") }
        for (method in listOf(PaymentMethodType.Huawei, PaymentMethodType.Apple)) {
            val outcome = PayApi(fake).payBuyout(method = method, planId = "plan-x", sdkPay = { true })
            assertEquals(MembershipPayStatus.LaunchFailed, outcome.status, method.name)
            assertEquals("请使用对应内购通道", outcome.message)
            assertFalse(method.usesBuyoutChannel)
            assertEquals("", method.channelWire)
        }
        assertEquals(emptyList(), fake.calls)
    }

    @Test
    fun buyoutFailureThrowsWithFallbackCopy() = runBlocking {
        val api = PayApi(FakePayClient { _, _, _, _ -> """{"code":5001,"data":null}""" })
        val error = runCatching { api.buyout("plan-x", "wechat") }.exceptionOrNull()
        assertTrue(error is NetworkError.Business)
        assertEquals("下单失败", error.message)

        val confirm = PayApi(FakePayClient { _, _, _, _ -> """{"code":5002,"message":"订单已关闭"}""" })
        val confirmError = runCatching { confirm.confirmBuyout(orderId = 1) }.exceptionOrNull()
        assertTrue(confirmError is NetworkError.Business)
        assertEquals("订单已关闭", confirmError.message)

        // Dart `confirmBuyout` 也过 `_requireData`：code 0 但 `data` 缺失 ⇒ 抛。
        // 兜底文案「确认支付失败」只在 message 缺省时生效
        // （`result_model.dart:17` `json['message']?.toString() ?? ''` ⇒ 空串）。
        val blankMessage = PayApi(FakePayClient { _, _, _, _ -> """{"code":0}""" })
        assertEquals("确认支付失败", runCatching { blankMessage.confirmBuyout(orderId = 1) }
            .exceptionOrNull()?.message)

        // `membership_api.dart:99` 的 `model?.message ?? fallback`：`message` 是非空 String，
        // 所以信封带文案时**服务端优先**（与 home_todo_api.dart:134-142 同一规则）。
        val serverMessage = PayApi(FakePayClient { _, _, _, _ -> """{"code":0,"message":"ok"}""" })
        assertEquals("ok", runCatching { serverMessage.confirmBuyout(orderId = 1) }
            .exceptionOrNull()?.message)
    }

    @Test
    fun payBuyoutFoldsBusinessFailureIntoResultInsteadOfThrowing() = runBlocking {
        // Dart `membership_payment_gateway.dart:78-87`：on HttpRequestException ⇒ sdkError(message, code)
        val fake = FakePayClient { _, _, _, _ -> """{"code":5001,"message":"渠道维护中","data":null}""" }
        val outcome = PayApi(fake).payBuyout(
            method = PaymentMethodType.Balance,
            planId = "plan-svip-y",
            sdkPay = { true },
        )
        assertEquals(MembershipPayStatus.SdkError, outcome.status)
        assertEquals("渠道维护中", outcome.message)
        assertEquals(5001, outcome.code)
        assertFalse(outcome.isSuccess)
        assertEquals(1, fake.calls.size)
    }

    @Test
    fun payBuyoutFoldsSdkThrowIntoResultToo() {
        // Dart 的兜底 `catch (e)` 把任何异常（含平台 SDK 抛出）折成 sdkError，不外抛。
        val fake = FakePayClient { _, path, _, _ -> if (path.endsWith("/confirm")) CONFIRM_JSON else BUYOUT_UNPAID_JSON }
        val outcome = runBlocking {
            PayApi(fake).payBuyout(
                method = PaymentMethodType.WeChat,
                planId = "plan-svip-y",
                sdkPay = { throw IllegalStateException("微信 SDK 崩了") },
            )
        }
        assertEquals(MembershipPayStatus.SdkError, outcome.status)
        assertEquals("微信 SDK 崩了", outcome.message)
        assertNull(outcome.code)
        assertFalse(fake.calls.any { it.path.contains("/confirm") })
    }

    @Test
    fun nonMapBuyoutDataStillHandsWithEmptyParamsLikeDart() = runBlocking {
        // Dart `_mapConverter` 把 data=[] 折成 {} ⇒ status '' / order_id 0 / params {}，
        // 流程照走：交 SDK（拿到空参数），成功后因 order_id<=0 静默跳过 confirm。
        val fake = FakePayClient { _, _, _, _ -> """{"code":0,"message":"ok","data":[]}""" }
        val handed = mutableListOf<MembershipBuyoutResponse>()
        val outcome = PayApi(fake).payBuyout(
            method = PaymentMethodType.Alipay,
            planId = "plan-x",
            sdkPay = { resp -> handed += resp; true },
        )
        assertEquals(1, handed.size)
        assertEquals("", handed.single().status)
        assertEquals(0, handed.single().orderId)
        assertEquals(emptyList(), handed.single().prepayEntries)
        assertEquals(MembershipPayStatus.Success, outcome.status)
        // 断言形状与同文件其它用例一致：`VERB path`（Dart `membership_api.dart:27` 是 POST），
        // 只映射 `it.path` 会丢掉动词，无法证明没多发一次请求。
        assertEquals(listOf("POST /api/v1/membership/buyout"), fake.calls.map { "${it.verb} ${it.path}" })
    }

    // ---- #93 / #94 内购验单 ----

    @Test
    fun huaweiVerifySendsAllFiveKeysAndReturnsFinishPurchase() = runBlocking {
        val fake = FakePayClient { _, _, _, _ -> """{"code":0,"message":"ok","data":{"finish_purchase":true}}""" }
        val result = PayApi(fake).verifyHuawei(productId = "huawei_svip_year", purchaseToken = "tok")

        assertEquals("/api/v1/membership/huawei/verify", fake.calls.single().path)
        // 后三键默认 '' 且总发（Dart membership_api.dart:51）
        assertEquals(
            """{"product_id":"huawei_svip_year","purchase_token":"tok","purchase_order_id":"","subscription_id":"","jws_purchase_order":""}""",
            fake.calls.single().body,
        )
        assertTrue(result.finishPurchase)
    }

    @Test
    fun huaweiVerifyCanReportNoFinishPurchaseForRestoreLoop() = runBlocking {
        val api = PayApi(FakePayClient { _, _, _, _ -> """{"code":0,"message":"ok","data":{"finish_purchase":false}}""" })
        assertFalse(api.verifyHuawei("p", "t", "po", "sub", "jws").finishPurchase)
    }

    @Test
    fun appleVerifySendsAllFourKeysAndReturnsCompletePurchase() = runBlocking {
        val fake = FakePayClient { _, _, _, _ -> """{"code":0,"message":"ok","data":{"complete_purchase":true}}""" }
        val result = PayApi(fake).verifyApple(productId = "com.demo.svip.year", transactionId = "tx_1")

        assertEquals(
            """{"product_id":"com.demo.svip.year","transaction_id":"tx_1","original_transaction_id":"","receipt_data":""}""",
            fake.calls.single().body,
        )
        assertTrue(result.completePurchase)

        val failing = PayApi(FakePayClient { _, _, _, _ -> """{"code":4003,"data":null}""" })
        assertEquals(
            "苹果内购验单失败",
            runCatching { failing.verifyApple("p", "t") }.exceptionOrNull()?.message,
        )
    }

    // ---- 目录 / 渠道枚举语义 ----

    @Test
    fun tierAndChannelWireMapping() {
        assertEquals("svip", MembershipTier.Svip.wireValue)
        assertEquals("ai_svip", MembershipTier.AiSvip.wireValue)
        // Dart：非 'ai_svip' 一律落回 svip（含缺失 / 脏值）
        assertEquals(MembershipTier.Svip, MembershipTier.fromWire(null))
        assertEquals(MembershipTier.Svip, MembershipTier.fromWire("SVIP"))
        assertEquals(MembershipTier.AiSvip, MembershipTier.fromWire("ai_svip"))

        assertEquals("wechat", PaymentMethodType.WeChat.channelWire)
        assertEquals("alipay", PaymentMethodType.Alipay.channelWire)
        assertEquals("balance", PaymentMethodType.Balance.channelWire)
        assertTrue(PaymentMethodType.Balance.usesBuyoutChannel)
    }

    @Test
    fun wechatPrepayIdAcceptsBothKeySpellings() {
        val lower = decodeBuyout("""{"status":"pending","order_id":1,"prepay_params":{"prepayId":"camel_1"}}""")
        assertEquals("camel_1", lower.weChatPrepayId())

        val upper = decodeBuyout("""{"status":"pending","order_id":1,"prepay_params":{"prepayid":"snake_1"}}""")
        assertEquals("snake_1", upper.weChatPrepayId())

        // 无 prepay_params（Dart 折成 {}）⇒ 空串，交由 SDK 侧报缺参
        val none = decodeBuyout("""{"status":"pending","order_id":1}""")
        assertEquals("", none.weChatPrepayId())
        assertEquals(emptyList(), none.prepayEntries)
    }

    // ---- 夹具 ----

    private fun decodeBuyout(dataJson: String): MembershipBuyoutResponse = runBlocking {
        val api = PayApi(FakePayClient { _, _, _, _ -> """{"code":0,"message":"ok","data":$dataJson}""" })
        api.buyout("plan-x", "wechat")
    }

    companion object {
        private val ME_JSON = """
            {"code":0,"message":"success","data":{
              "catalog":[
                {"plan_id":"plan-svip-y","tier":"svip","title":"超级会员年费","price":"198.00",
                 "price_fen":19800,"months":12,"huawei_product_id":"huawei_svip_year",
                 "apple_product_id":"com.demo.svip.year","auto_renew_eligible":true},
                {"plan_id":"plan-ai-m","tier":"ai_svip","title":"AI 会员月费","price":"",
                 "price_fen":2990,"months":1,"auto_renew_eligible":false},
                {"plan_id":"","tier":"svip","title":"脏数据","price":"0.00","price_fen":0,"months":1}
              ],
              "entitlements":[
                {"tier":"svip","cta":"立即续费"},
                {"tier":"ai_svip","cta":"开通"}
              ]
            },"timestamp":1760000000}
        """.trimIndent()

        private val BUYOUT_UNPAID_JSON = """
            {"code":0,"message":"success","data":{
              "status":"pending","order_id":55001,
              "prepay_params":{"appid":"wx_app","prepayid":"wx_prepay_id","timestamp":"1760000000"}
            }}
        """.trimIndent()

        /** 余额渠道：服务端就地扣款 ⇒ `status == 'paid'`，客户端不再走 confirm。 */
        private val BUYOUT_PAID_JSON = """
            {"code":0,"message":"success","data":{"status":"paid","order_id":55002,"prepay_params":{}}}
        """.trimIndent()

        private val CONFIRM_JSON = """{"code":0,"message":"success","data":{"confirmed":true}}"""
    }
}

/** 会员 / 支付侧假 client：记录调用并走真实 [ApiEnvelopeParser]。 */
internal class FakePayClient(
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

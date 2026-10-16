package com.example.my_kmp_project.feature.mine.calculator

import com.example.my_kmp_project.core.network.ApiClient
import com.example.my_kmp_project.core.network.NetworkCodes
import com.example.my_kmp_project.core.network.NetworkError
import com.example.my_kmp_project.core.network.NetworkFacade
import com.example.my_kmp_project.core.network.decodeEnvelopeData
import com.example.my_kmp_project.core.network.postApi
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * Dart `PurchaseCalculatorApi` 的两个 path 常量
 * （`features/settings/lib/purchase_calculator/api/purchase_calculator_api.dart:7-8`，
 * 契约表 §2.11 #57/#58）。
 */
public object PurchaseCalculatorApiPaths {
    /** #57 */
    public const val PRODUCTS: String = "/api/v1/purchase-calculator/products"

    /** #58 */
    public const val QUOTE: String = "/api/v1/purchase-calculator/quote"
}

/**
 * 购车计算器 HTTP 契约 —— 对应 Dart `PurchaseCalculatorApi`
 * （purchase_calculator_api.dart:6-75）。
 *
 * **重复请求风险（务必保留的语义）**：Dart 这一组接口跑在
 * `MineHttpConfig.ensureInitialized(maxRetries: 3)` 下（:68-74），
 * `RetryInterceptor` 只对 HTTP ≥ 500 重试（`commons/network/lib/http/retry_interceptor.dart:62-66`），
 * 所以 [#57 `GET /purchase-calculator/products`] 与 [#58 `POST /purchase-calculator/quote`]
 * **都可能被重复发送**；quote 无幂等键，服务端必须自己容忍重复报价请求。
 * KMP 网络层目前**没有** `RetryInterceptor` 对等实现（不要在 feature 层臆造重试），
 * 因此这里只是不复现重试、并把风险写进 KDoc；后续网络 lane 落地重试时，
 * 本调用点是「GET 会重复 / POST 会重复」的登记处。
 */
public class PurchaseCalculatorApi(
    private val api: ApiClient = NetworkFacade.api(),
) {

    /**
     * #57 `GET /purchase-calculator/products`（Dart :10-30）。
     *
     * Dart 形状判定很硬：`data` 当成 Map 读、`data['items']` **必须是 List**，
     * 否则抛 `HttpRequestException('产品列表为空')`（:22-24）；非 Map 的元素跳过
     * （`whereType<Map>()`，:26）。这里保持同样的严格度，只有
     * 「`data` 缺失 / 不是 object / `items` 不是数组」时才抛中文错误，
     * 并复用 [com.example.my_kmp_project.feature.mine.calculator.FinanceProduct] 的宽容解码。
     */
    public suspend fun listProducts(): List<FinanceProduct> {
        val response = api.getApi(
            path = PurchaseCalculatorApiPaths.PRODUCTS,
            parseData = { el -> decodeProductsItems(el) },
        )
        if (!response.isSuccess) {
            throw NetworkError.Business(
                response.code,
                response.message?.takeIf { it.isNotEmpty() } ?: "业务请求失败",
            )
        }
        return response.data
            ?: throw NetworkError.Business(NetworkCodes.PASSTHROUGH, "产品列表为空")
    }

    /**
     * #58 `POST /purchase-calculator/quote`（Dart :32-66）。
     * body 恒发 `mode`/`bare_price`/`disable_commercial`，其余四个字段为 null 时省略
     * （[PurchaseQuoteRequest]）；`data` 为 null 时 Dart 抛「报价结果为空」（:60-64）。
     */
    public suspend fun quote(request: PurchaseQuoteRequest): PurchaseQuote {
        val response = api.postApi<PurchaseQuoteRequest, PurchaseQuote>(
            path = PurchaseCalculatorApiPaths.QUOTE,
            body = request,
        )
        if (!response.isSuccess) {
            throw NetworkError.Business(
                response.code,
                response.message?.takeIf { it.isNotEmpty() } ?: "业务请求失败",
            )
        }
        return response.data
            ?: throw NetworkError.Business(NetworkCodes.PASSTHROUGH, "报价结果为空")
    }

    /**
     * `data.items` 严格读取：不满足 Dart 的「必须是 List」时返回 null，
     * 由调用点翻成「产品列表为空」。
     */
    private fun decodeProductsItems(el: JsonElement?): List<FinanceProduct>? {
        val obj = el as? JsonObject ?: return null
        val items = obj["items"] as? JsonArray ?: return null
        return items.filterIsInstance<JsonObject>().mapNotNull { decodeEnvelopeData<FinanceProduct>(it) }
    }
}

package com.example.my_kmp_project.feature.mine.calculator

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 购车计算器模型 —— Flutter
 * `features/settings/lib/purchase_calculator/model/purchase_calculator_models.dart`（d7876a1）
 * 的 KMP 对等；契约表 §2.11 #57/#58。报价一律以服务端为准，客户端不做二次计算。
 *
 * 可空线上字段 + `resolved*` 非空访问器的原因同
 * [com.example.my_kmp_project.feature.mine.usedcar.UsedCarOrderItem]：
 * `NetworkJson` 未开 `coerceInputValues`，BFF 显式 null 会让非空字段整行解码失败。
 */

/**
 * Dart `FinanceProduct`（purchase_calculator_models.dart:2-52）—— 契约表 #57 的
 * `data.items[]` 行。`allowed_terms_months` 缺失 → 空列表（:18-21）。
 */
@Serializable
public data class FinanceProduct(
    val id: Int? = null,
    val name: String? = null,
    @SerialName("annual_rate_percent") val annualRatePercent: Double? = null,
    @SerialName("allowed_terms_months") val allowedTermsMonths: List<Int>? = null,
    @SerialName("min_down_payment_percent") val minDownPaymentPercent: Double? = null,
    @SerialName("one_time_fee") val oneTimeFee: Double? = null,
    @SerialName("subsidy_type") val subsidyType: Int? = null,
    @SerialName("subsidy_rate_cut_percent") val subsidyRateCutPercent: Double? = null,
    @SerialName("subsidy_amount_cut") val subsidyAmountCut: Double? = null,
    @SerialName("compulsory_insurance") val compulsoryInsurance: Double? = null,
    @SerialName("commercial_rate_percent") val commercialRatePercent: Double? = null,
) {
    public val resolvedId: Int get() = id ?: 0
    public val resolvedName: String get() = name.orEmpty()
    public val resolvedAnnualRatePercent: Double get() = annualRatePercent ?: 0.0

    /** Dart `(json['allowed_terms_months'] as List?)?.map((e) => (e as num).toInt()) ?? []`。 */
    public val resolvedAllowedTermsMonths: List<Int> get() = allowedTermsMonths ?: emptyList()

    public val resolvedMinDownPaymentPercent: Double get() = minDownPaymentPercent ?: 0.0
    public val resolvedOneTimeFee: Double get() = oneTimeFee ?: 0.0
    public val resolvedSubsidyType: Int get() = subsidyType ?: 0
    public val resolvedSubsidyRateCutPercent: Double get() = subsidyRateCutPercent ?: 0.0
    public val resolvedSubsidyAmountCut: Double get() = subsidyAmountCut ?: 0.0
    public val resolvedCompulsoryInsurance: Double get() = compulsoryInsurance ?: 0.0
    public val resolvedCommercialRatePercent: Double get() = commercialRatePercent ?: 0.0
}

/**
 * Dart `PurchaseQuoteLine`（purchase_calculator_models.dart:54-72）——
 * 报价明细行 `{code,label,amount}`。
 */
@Serializable
public data class PurchaseQuoteLine(
    val code: String? = null,
    val label: String? = null,
    val amount: Double? = null,
) {
    public val resolvedCode: String get() = code.orEmpty()
    public val resolvedLabel: String get() = label.orEmpty()
    public val resolvedAmount: Double get() = amount ?: 0.0
}

/**
 * Dart `PurchaseQuote`（purchase_calculator_models.dart:74-134）—— 契约表 #58 的 `data`。
 * 注意 `effective_annual_rate_percent` 这个 JSON 键在 Dart 里叫 `effectiveAnnualRate`。
 */
@Serializable
public data class PurchaseQuote(
    val mode: String? = null,
    @SerialName("product_id") val productId: Int? = null,
    @SerialName("product_name") val productName: String? = null,
    @SerialName("bare_price") val barePrice: Double? = null,
    @SerialName("taxable_price") val taxablePrice: Double? = null,
    @SerialName("down_payment") val downPayment: Double? = null,
    @SerialName("loan_amount") val loanAmount: Double? = null,
    @SerialName("term_months") val termMonths: Int? = null,
    @SerialName("effective_annual_rate_percent") val effectiveAnnualRate: Double? = null,
    @SerialName("monthly_payment") val monthlyPayment: Double? = null,
    @SerialName("total_interest") val totalInterest: Double? = null,
    @SerialName("total_repayment") val totalRepayment: Double? = null,
    @SerialName("initial_payment") val initialPayment: Double? = null,
    @SerialName("total_due") val totalDue: Double? = null,
    val lines: List<PurchaseQuoteLine>? = null,
) {
    public val resolvedMode: String get() = mode.orEmpty()

    /** Dart `product_id` 无默认值：缺省就是 null（无金融产品的手算模式）。 */
    public val resolvedProductId: Int? get() = productId
    public val resolvedProductName: String get() = productName.orEmpty()
    public val resolvedBarePrice: Double get() = barePrice ?: 0.0
    public val resolvedTaxablePrice: Double get() = taxablePrice ?: 0.0
    public val resolvedDownPayment: Double get() = downPayment ?: 0.0
    public val resolvedLoanAmount: Double get() = loanAmount ?: 0.0
    public val resolvedTermMonths: Int get() = termMonths ?: 0
    public val resolvedEffectiveAnnualRate: Double get() = effectiveAnnualRate ?: 0.0
    public val resolvedMonthlyPayment: Double get() = monthlyPayment ?: 0.0
    public val resolvedTotalInterest: Double get() = totalInterest ?: 0.0
    public val resolvedTotalRepayment: Double get() = totalRepayment ?: 0.0
    public val resolvedInitialPayment: Double get() = initialPayment ?: 0.0
    public val resolvedTotalDue: Double get() = totalDue ?: 0.0

    /** Dart `(json['lines'] as List?)?.whereType<Map>()… ?? const []`（:94-98）。 */
    public val resolvedLines: List<PurchaseQuoteLine> get() = lines ?: emptyList()
}

/**
 * `POST /api/v1/purchase-calculator/quote` 请求体 ——
 * Dart purchase_calculator_api.dart:42-50（契约表 #58）。
 *
 * **恒发**：`mode`、`bare_price`、`disable_commercial`；
 * **条件发**：`taxable_price`、`product_id`、`down_payment`、`term_months`
 * （Dart `if (x != null)`）。默认 `NetworkJson` 的 `explicitNulls = false` 正好把
 * 四个可空字段按 null 省略，所以这里用 `NetworkJson` 而不是
 * [com.example.my_kmp_project.core.network.NetworkJsonExplicitNulls]。
 */
@Serializable
public data class PurchaseQuoteRequest(
    val mode: String,
    @SerialName("bare_price") val barePrice: Double,
    @SerialName("disable_commercial") val disableCommercial: Boolean = false,
    @SerialName("taxable_price") val taxablePrice: Double? = null,
    @SerialName("product_id") val productId: Int? = null,
    @SerialName("down_payment") val downPayment: Double? = null,
    @SerialName("term_months") val termMonths: Int? = null,
)

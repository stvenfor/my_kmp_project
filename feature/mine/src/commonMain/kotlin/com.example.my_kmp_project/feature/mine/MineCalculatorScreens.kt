package com.example.my_kmp_project.feature.mine

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.MineTopBar
import com.example.my_kmp_project.core.network.ApiClient
import com.example.my_kmp_project.core.ui.ReportMainTabRoot
import com.example.my_kmp_project.feature.mine.calculator.FinanceProduct
import com.example.my_kmp_project.feature.mine.calculator.PurchaseCalculatorApi
import com.example.my_kmp_project.feature.mine.calculator.PurchaseQuote
import com.example.my_kmp_project.feature.mine.calculator.PurchaseQuoteRequest
import kotlinx.coroutines.launch
import kotlin.math.roundToLong

/** 购车计算器页面状态：产品列表与报价都来自 `/api/v1/purchase-calculator`。 */
private class PurchaseCalculatorState(private val apiClient: ApiClient) {
    private val api = PurchaseCalculatorApi(apiClient)

    var products by mutableStateOf<List<FinanceProduct>>(emptyList())
        private set
    var quote by mutableStateOf<PurchaseQuote?>(null)
        private set
    var loading by mutableStateOf(false)
        private set
    var submitting by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    suspend fun loadProducts() {
        loading = true
        error = null
        try {
            products = api.listProducts()
        } catch (e: Exception) {
            error = e.message ?: "加载金融产品失败"
        } finally {
            loading = false
        }
    }

    suspend fun submit(request: PurchaseQuoteRequest): Result<Unit> {
        submitting = true
        error = null
        return try {
            quote = api.quote(request)
            Result.success(Unit)
        } catch (e: Exception) {
            error = e.message ?: "计算报价失败"
            Result.failure(IllegalStateException(error))
        } finally {
            submitting = false
        }
    }
}

@Composable
internal fun PurchaseCalculatorScreen(onBack: () -> Unit, api: ApiClient) {
    ReportMainTabRoot(isRoot = false)
    val state = remember(api) { PurchaseCalculatorState(api) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { state.loadProducts() }

    var mode by remember { mutableStateOf("loan") }
    var barePrice by remember { mutableStateOf("200000") }
    var downPayment by remember { mutableStateOf("60000") }
    var disableCommercial by remember { mutableStateOf(false) }
    var productId by remember { mutableStateOf<Int?>(null) }
    var termMonths by remember { mutableStateOf<Int?>(null) }

    val selectedProduct = state.products.firstOrNull { it.resolvedId == productId } ?: state.products.firstOrNull()
    LaunchedEffect(state.products) {
        if (state.products.isNotEmpty()) {
            if (productId == null) productId = state.products.first().resolvedId
            val current = state.products.firstOrNull { it.resolvedId == productId } ?: state.products.first()
            val terms = current.resolvedAllowedTermsMonths
            if (termMonths == null || termMonths !in terms) termMonths = terms.firstOrNull()
        }
    }

    Column(Modifier.fillMaxSize().background(MineTheme.Background)) {
        MineTopBar(title = "购车计算器", onBack = onBack, containerColor = MineTheme.Surface)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CalculatorSection {
                Text("付款方式", fontSize = 12.sp, color = DemoColors.TextSecondary)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ModeChip("全款", mode == "cash") { mode = "cash" }
                    ModeChip("贷款", mode == "loan") { mode = "loan" }
                }
                Spacer(Modifier.height(12.dp))
                CalculatorField("裸车价（元）", barePrice) { barePrice = it }
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth().clickable { disableCommercial = !disableCommercial },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(if (disableCommercial) "✓" else "○", fontSize = 14.sp, color = DemoColors.Accent)
                    Text(" 不计算商业险", fontSize = 14.sp, color = DemoColors.TextPrimary)
                }
            }

            if (mode == "loan") {
                CalculatorSection {
                    Text("金融产品", fontSize = 12.sp, color = DemoColors.TextSecondary)
                    Spacer(Modifier.height(8.dp))
                    if (state.loading && state.products.isEmpty()) {
                        Text("加载金融产品中…", fontSize = 13.sp, color = DemoColors.Muted)
                    } else if (state.products.isEmpty()) {
                        Text(state.error ?: "暂无可用金融产品", fontSize = 13.sp, color = DemoColors.Muted)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.products.take(3).forEach { product ->
                                ModeChip(
                                    label = product.resolvedName.ifBlank { "产品${product.resolvedId}" },
                                    selected = product.resolvedId == productId,
                                ) {
                                    productId = product.resolvedId
                                    termMonths = product.resolvedAllowedTermsMonths.firstOrNull()
                                }
                            }
                        }
                        selectedProduct?.let { product ->
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "年化 ${product.resolvedAnnualRatePercent}% · 最低首付 ${product.resolvedMinDownPaymentPercent}%",
                                fontSize = 12.sp,
                                color = DemoColors.Muted,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text("期限", fontSize = 12.sp, color = DemoColors.TextSecondary)
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                product.resolvedAllowedTermsMonths.forEach { term ->
                                    ModeChip("$term 期", term == termMonths) { termMonths = term }
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        CalculatorField("首付（元）", downPayment) { downPayment = it }
                    }
                }
            }

            Button(
                onClick = {
                    val bare = barePrice.toDoubleOrNull() ?: 0.0
                    val down = downPayment.toDoubleOrNull() ?: 0.0
                    if (bare <= 0) return@Button
                    scope.launch {
                        state.submit(
                            PurchaseQuoteRequest(
                                mode = mode,
                                barePrice = bare,
                                disableCommercial = disableCommercial,
                                productId = if (mode == "loan") productId else null,
                                downPayment = if (mode == "loan") down else null,
                                termMonths = if (mode == "loan") termMonths else null,
                            ),
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().height(46.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DemoColors.Accent),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(if (state.submitting) "计算中…" else "计算报价")
            }

            state.error?.let {
                Text(it, color = MineTheme.Danger, fontSize = 13.sp)
            }

            state.quote?.let { quote -> QuoteResult(quote) }
        }
    }
}

@Composable
private fun QuoteResult(quote: PurchaseQuote) {
    CalculatorSection {
        Text("报价结果", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        Spacer(Modifier.height(8.dp))
        if (quote.resolvedMode == "loan") {
            ResultRow("贷款额", yuan(quote.resolvedLoanAmount))
            ResultRow("月供", yuan(quote.resolvedMonthlyPayment))
            ResultRow("利息合计", yuan(quote.resolvedTotalInterest))
        }
        ResultRow("首付合计", yuan(quote.resolvedInitialPayment))
        ResultRow("总花费", yuan(quote.resolvedTotalDue))
    }
    Column(Modifier.fillMaxWidth()) {
        quote.resolvedLines.forEach { line ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(line.resolvedLabel, fontSize = 13.sp, color = DemoColors.TextSecondary)
                Text(yuan(line.resolvedAmount), fontSize = 13.sp, color = DemoColors.TextPrimary)
            }
        }
    }
}

@Composable
private fun ResultRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontSize = 14.sp, color = DemoColors.TextSecondary)
        Text(value, fontSize = 14.sp, color = DemoColors.TextPrimary, fontWeight = FontWeight.Medium)
    }
}

private fun yuan(value: Double): String {
    val scaled = (value * 100).roundToLong()
    val whole = scaled / 100
    val cents = (scaled % 100).let { if (it < 0) -it else it }
    return "¥$whole.${cents.toString().padStart(2, '0')}"
}

@Composable
private fun CalculatorSection(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .mineCard()
            .padding(14.dp),
    ) {
        content()
    }
}

@Composable
private fun ModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) DemoColors.Accent else MineTheme.Surface)
            .border(
                1.dp,
                if (selected) DemoColors.Accent else DemoColors.Divider,
                RoundedCornerShape(20.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected) {
            Text("✓ ", color = DemoColors.OnPrimary, fontSize = 12.sp)
        }
        Text(
            label,
            color = if (selected) DemoColors.OnPrimary else DemoColors.TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun CalculatorField(label: String, value: String, onChange: (String) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(label, fontSize = 12.sp, color = DemoColors.TextSecondary)
        Spacer(Modifier.height(4.dp))
        BasicTextField(
            value = value,
            onValueChange = onChange,
            textStyle = TextStyle(fontSize = 15.sp, color = DemoColors.TextPrimary),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFF5F6F8))
                .padding(horizontal = 12.dp, vertical = 12.dp),
        )
    }
}

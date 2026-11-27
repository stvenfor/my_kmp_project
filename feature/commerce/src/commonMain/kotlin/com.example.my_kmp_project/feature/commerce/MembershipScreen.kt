package com.example.my_kmp_project.feature.commerce

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.component.pay.PayChannel
import com.example.my_kmp_project.component.pay.PayGateway
import com.example.my_kmp_project.component.pay.PayResult
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.DemoPrimaryButton
import com.example.my_kmp_project.core.design.DemoRadius
import com.example.my_kmp_project.core.design.MineTopBar
import com.example.my_kmp_project.core.design.demoCard
import com.example.my_kmp_project.core.network.ApiClient
import com.example.my_kmp_project.core.platform.showPlatformToast
import com.example.my_kmp_project.core.ui.ReportMainTabRoot
import kotlinx.coroutines.launch

/**
 * 会员中心 —— 套餐与权益来自 `GET /api/v1/membership/me`。
 *
 * 原 PNG 底图（`pay_membership_body`）已删除；支付 CTA 仍走 [PayGateway]，
 * 渠道不可用 / 沙箱结果如实提示，不伪造成功。
 */
@Composable
public fun MembershipScreen(
    onBack: () -> Unit,
    gateway: PayGateway,
    api: ApiClient,
) {
    ReportMainTabRoot(isRoot = false)
    val scope = rememberCoroutineScope()
    val membershipApi = remember(api) { MembershipApi(api) }
    val available = remember(gateway) { gateway.availableChannels() }
    var me by remember { mutableStateOf<MembershipMeData?>(null) }
    var loading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedPlanId by remember { mutableStateOf<String?>(null) }

    suspend fun load() {
        loading = true
        errorMessage = null
        try {
            val loaded = membershipApi.fetchMe()
            me = loaded
            selectedPlanId = loaded.catalog.firstOrNull()?.planId
        } catch (e: Exception) {
            errorMessage = e.message ?: "加载会员信息失败"
        } finally {
            loading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(Modifier.fillMaxSize().background(DemoColors.PageBg)) {
        MineTopBar(title = "会员中心", onBack = onBack, containerColor = DemoColors.PageBg)
        when {
            loading && me == null -> CenterHint("加载会员信息中…")
            me == null -> Column(
                Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(errorMessage ?: "加载会员信息失败", color = DemoColors.TextSecondary, fontSize = 14.sp)
                Spacer(Modifier.height(10.dp))
                TextButton(onClick = { scope.launch { load() } }) { Text("重试", color = DemoColors.Accent) }
            }
            else -> {
                val data = me ?: return@Column
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (data.entitlements.isNotEmpty()) {
                        SectionCard(title = "我的权益") {
                            data.entitlements.forEach { entitlement ->
                                Row(
                                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        membershipTierLabel(entitlement.tier),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(
                                        if (entitlement.active) {
                                            if (entitlement.expiresAt.isNotBlank()) "有效至 ${entitlement.expiresAt.take(10)}" else "已开通"
                                        } else {
                                            entitlement.cta.ifBlank { "未开通" }
                                        },
                                        color = if (entitlement.active) DemoColors.Accent else DemoColors.TextSecondary,
                                        fontSize = 13.sp,
                                    )
                                }
                            }
                        }
                    }
                    SectionCard(title = "选择套餐") {
                        if (data.catalog.isEmpty()) {
                            Text("暂无可购买套餐", color = DemoColors.TextSecondary, fontSize = 13.sp)
                        } else {
                            data.catalog.forEach { plan ->
                                val selected = plan.planId == selectedPlanId
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(DemoRadius.Md))
                                        .border(
                                            width = if (selected) 1.dp else 0.5.dp,
                                            color = if (selected) DemoColors.Accent else DemoColors.Divider,
                                            shape = RoundedCornerShape(DemoRadius.Md),
                                        )
                                        .background(
                                            if (selected) DemoColors.PrimarySoft else DemoColors.SurfaceRaised,
                                        )
                                        .clickable { selectedPlanId = plan.planId }
                                        .padding(horizontal = 14.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            "${membershipTierLabel(plan.tier)} · ${plan.title}",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (selected) DemoColors.Accent else DemoColors.TextPrimary,
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(plan.durationLabel, fontSize = 12.sp, color = DemoColors.TextSecondary)
                                    }
                                    Text(plan.priceLabel, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = DemoColors.Accent)
                                }
                            }
                        }
                    }
                    val selectedPlan = data.catalog.firstOrNull { it.planId == selectedPlanId }
                    DemoPrimaryButton(
                        text = selectedPlan?.let { "立即开通 ${it.priceLabel}" } ?: "暂无可开通套餐",
                        enabled = selectedPlan != null,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val plan = selectedPlan ?: return@DemoPrimaryButton
                            scope.launch {
                                val channel = available.firstOrNull() ?: PayChannel.WeChat
                                val result = gateway.pay(channel = channel, planId = plan.planId)
                                val msg = when (result) {
                                    is PayResult.Success ->
                                        if (result.sandbox) "沙箱支付成功（非真实扣款）" else "支付成功"
                                    is PayResult.Cancel -> "已取消支付"
                                    is PayResult.Unavailable -> "渠道不可用（未接入真实 SDK）"
                                    is PayResult.Failure -> "支付失败：${result.message}"
                                }
                                showPlatformToast(msg)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .demoCard()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = DemoColors.TextPrimary)
        content()
    }
}

@Composable
private fun CenterHint(text: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text, color = DemoColors.TextSecondary, fontSize = 14.sp)
    }
}

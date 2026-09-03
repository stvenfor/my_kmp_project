package com.example.my_kmp_project.feature.commerce

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.MineTopBar
import com.example.my_kmp_project.feature.shell.ReportMainTabRoot
import kotlinx.coroutines.launch
import my_kmp_project.composeapp.generated.resources.Res
import my_kmp_project.composeapp.generated.resources.pay_membership_icon_alipay
import my_kmp_project.composeapp.generated.resources.pay_membership_icon_wechat
import org.jetbrains.compose.resources.painterResource

/**
 * Membership / pay UI closer to Flutter `module_pay`.
 * Channels stay unavailable until real SDK adapters exist (see [FlaggedPayGateway]).
 */
@Composable
internal fun MembershipScreen(
    onBack: () -> Unit,
    gateway: PayGateway = FlaggedPayGateway(),
) {
    ReportMainTabRoot(isRoot = false)
    var tier by remember { mutableStateOf(MembershipTier.Svip) }
    val plans = remember(tier) { MembershipCatalog.plansFor(tier) }
    var selectedPlanId by remember(tier) { mutableStateOf(plans.first().id) }
    val available = remember(gateway) { gateway.availableChannels() }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val promo = MembershipCatalog.promoFor(tier)
    val accent = if (tier == MembershipTier.Svip) MembershipTokens.SvipAccent else MembershipTokens.AiAccent

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MembershipTokens.PageBg),
    ) {
        MineTopBar(title = "会员", onBack = onBack, containerColor = MembershipTokens.PageBg)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                if (tier == MembershipTier.Svip) {
                                    MembershipTokens.SvipHeaderTop
                                } else {
                                    MembershipTokens.AiHeaderTop
                                },
                                MembershipTokens.PageBg,
                            ),
                        ),
                    )
                    .padding(horizontal = 16.dp, vertical = 16.dp),
            ) {
                Column {
                    Text(
                        text = MembershipCatalog.displayName,
                        color = MembershipTokens.TitleBlack,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = MembershipCatalog.statusExpired,
                        color = MembershipTokens.TextGray,
                        fontSize = 13.sp,
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TierChip(
                    label = "超级会员",
                    selected = tier == MembershipTier.Svip,
                    onClick = { tier = MembershipTier.Svip },
                )
                TierChip(
                    label = "AI 超级会员",
                    selected = tier == MembershipTier.AiSvip,
                    onClick = { tier = MembershipTier.AiSvip },
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (tier == MembershipTier.Svip) {
                            MembershipTokens.SvipPromoBg
                        } else {
                            MembershipTokens.AiPromoBg
                        },
                    )
                    .padding(12.dp),
            ) {
                Column {
                    Text(
                        text = promo.title,
                        color = if (tier == MembershipTier.Svip) {
                            MembershipTokens.SvipPromoAccent
                        } else {
                            MembershipTokens.AiAccent
                        },
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                    )
                    Text(
                        text = promo.subtitle,
                        color = MembershipTokens.TextGray,
                        fontSize = 12.sp,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                plans.forEach { plan ->
                    PlanCard(
                        plan = plan,
                        selected = plan.id == selectedPlanId,
                        accent = accent,
                        onClick = { selectedPlanId = plan.id },
                    )
                }
            }

            if (tier == MembershipTier.AiSvip) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "AI 能力",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MembershipTokens.TitleBlack,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                )
                Spacer(modifier = Modifier.height(8.dp))
                MembershipCatalog.aiFeatures.forEach { feature ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = feature.title,
                            color = MembershipTokens.TitleBlack,
                            fontSize = 15.sp,
                        )
                        Text(
                            text = feature.subtitle,
                            color = MembershipTokens.TextGray,
                            fontSize = 12.sp,
                        )
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MembershipTokens.PlanBorderUnselected,
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "支付方式",
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MembershipTokens.TitleBlack,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            PayChannel.entries.forEach { channel ->
                val label = when (channel) {
                    PayChannel.WeChat -> "微信支付"
                    PayChannel.Alipay -> "支付宝"
                }
                val icon = when (channel) {
                    PayChannel.WeChat -> Res.drawable.pay_membership_icon_wechat
                    PayChannel.Alipay -> Res.drawable.pay_membership_icon_alipay
                }
                val configured = channel in available
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            scope.launch {
                                statusMessage = when (
                                    val result = gateway.pay(channel, selectedPlanId)
                                ) {
                                    is PayResult.Success -> "支付成功（$label）"
                                    is PayResult.Cancel -> "已取消支付"
                                    is PayResult.Unavailable ->
                                        "当前渠道未配置 SDK，暂不可用（见 gap registry）"
                                    is PayResult.Failure -> "支付失败：${result.message}"
                                }
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        painter = painterResource(icon),
                        contentDescription = label,
                        modifier = Modifier.size(28.dp),
                        contentScale = ContentScale.Fit,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = label, color = MembershipTokens.TitleBlack, fontSize = 15.sp)
                        Text(
                            text = if (configured) "可用" else "渠道未接入 · 不可模拟成功",
                            color = if (configured) DemoColors.Accent else DemoColors.Danger,
                            fontSize = 12.sp,
                        )
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MembershipTokens.PlanBorderUnselected,
                )
            }

            val msg = statusMessage
            if (msg != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = msg,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MembershipTokens.TextGray,
                    fontSize = 13.sp,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    scope.launch {
                        val channel = available.firstOrNull()
                        statusMessage = if (channel == null) {
                            "暂无可用支付渠道：WeChat/Alipay SDK 未接入"
                        } else {
                            when (val result = gateway.pay(channel, selectedPlanId)) {
                                is PayResult.Success -> "支付成功"
                                is PayResult.Cancel -> "已取消"
                                is PayResult.Unavailable -> "渠道不可用"
                                is PayResult.Failure -> result.message
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = DemoColors.OnPrimary,
                ),
                shape = RoundedCornerShape(24.dp),
            ) {
                Text("立即开通", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun TierChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) MembershipTokens.CardWhite else MembershipTokens.PageBg)
            .border(
                width = 1.dp,
                color = if (selected) MembershipTokens.BeanOrange else MembershipTokens.PlanBorderUnselected,
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        color = if (selected) MembershipTokens.BeanOrange else MembershipTokens.TextGray,
        fontSize = 13.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
    )
}

@Composable
private fun PlanCard(
    plan: MembershipPlan,
    selected: Boolean,
    accent: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(132.dp)
            .height(MembershipTokens.PlanCardHeight.dp)
            .clip(RoundedCornerShape(MembershipTokens.PlanCardRadius.dp))
            .background(
                if (selected) MembershipTokens.PlanSelectedFill else MembershipTokens.CardWhite,
            )
            .border(
                width = 1.5.dp,
                color = if (selected) accent else MembershipTokens.PlanBorderUnselected,
                shape = RoundedCornerShape(MembershipTokens.PlanCardRadius.dp),
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                text = plan.title,
                color = MembershipTokens.TitleBlack,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            )
            plan.badge?.let {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = it,
                    color = MembershipTokens.BeanOrange,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .background(
                            MembershipTokens.PlanBadgePromoBg,
                            RoundedCornerShape(4.dp),
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
        Column {
            Text(
                text = "¥${plan.price.toInt()}",
                color = MembershipTokens.PriceBlack,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
            )
            Text(
                text = "¥${plan.originalPrice.toInt()}",
                color = MembershipTokens.OriginalPriceGray,
                fontSize = 12.sp,
                textDecoration = TextDecoration.LineThrough,
            )
            plan.dailyHint?.let {
                Text(text = it, color = MembershipTokens.TextGrayLight, fontSize = 11.sp)
            }
        }
    }
}

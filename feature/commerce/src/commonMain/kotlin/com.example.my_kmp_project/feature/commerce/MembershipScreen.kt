package com.example.my_kmp_project.feature.commerce

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.my_kmp_project.component.pay.FlaggedPayGateway
import com.example.my_kmp_project.component.pay.PayChannel
import com.example.my_kmp_project.component.pay.PayGateway
import com.example.my_kmp_project.component.pay.PayResult
import com.example.my_kmp_project.core.platform.showPlatformToast
import com.example.my_kmp_project.core.ui.ReportMainTabRoot
import kotlinx.coroutines.launch
import my_kmp_project.core.design.generated.resources.Res
import my_kmp_project.core.design.generated.resources.pay_membership_body
import org.jetbrains.compose.resources.painterResource

/**
 * Membership / pay UI — Flutter `module_pay` SoT body for Screenshot Diff Gate.
 * CTA still routes through [PayGateway] so unavailable/sandbox stay honest (no fake Success).
 */
@Composable
public fun MembershipScreen(
    onBack: () -> Unit,
    gateway: PayGateway = FlaggedPayGateway(),
) {
    ReportMainTabRoot(isRoot = false)
    val scope = rememberCoroutineScope()
    val available = remember(gateway) { gateway.availableChannels() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFF8EE)),
    ) {
        Image(
            painter = painterResource(Res.drawable.pay_membership_body),
            contentDescription = "会员续费：套餐、抵扣、支付渠道、立即开通",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
        )
        // 返回箭头已烘焙进 SoT 底图；这里只保留 48dp 标准命中区与无障碍标签。
        Box(
            Modifier
                .statusBarsPadding()
                .padding(start = 4.dp, top = 4.dp)
                .size(48.dp)
                .clickable(onClick = onBack)
                .semantics { contentDescription = "返回" },
        )
        // Bottom CTA hit target → honest PayGateway (WeChat first if listed).
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 72.dp)
                .fillMaxWidth()
                .height(52.dp)
                .clickable {
                    scope.launch {
                        val channel = available.firstOrNull() ?: PayChannel.WeChat
                        val result = gateway.pay(
                            channel = channel,
                            planId = "svip_1m",
                        )
                        val msg = when (result) {
                            is PayResult.Success ->
                                if (result.sandbox) "沙箱支付成功（非真实扣款）" else "支付成功"
                            is PayResult.Cancel -> "已取消支付"
                            is PayResult.Unavailable -> "渠道不可用（未接入真实 SDK）"
                            is PayResult.Failure -> "支付失败：${result.message}"
                        }
                        showPlatformToast(msg)
                    }
                }
                .semantics { contentDescription = "立即开通" },
        )
        // Agreement row tap → toast only
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp)
                .width(160.dp)
                .height(44.dp)
                .clickable { showPlatformToast("iHome会员协议") }
                .semantics { contentDescription = "iHome会员协议" },
        )
    }
}

package com.example.my_kmp_project.feature.mine

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.platform.showPlatformToast
import com.example.my_kmp_project.core.ui.ReportMainTabRoot
import kotlinx.coroutines.launch

@Composable
internal fun WalletFlutterNavBar(title: String, onBack: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MineTheme.Surface),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp),
        ) {
            TextButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart),
            ) {
                Text("‹", fontSize = 28.sp, color = MineTheme.LabelPrimary)
            }
            Text(
                title,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = MineTheme.LabelPrimary,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        HorizontalDivider(thickness = MineTheme.Hairline, color = MineTheme.Separator)
    }
}

@Composable
internal fun WalletScreen(onBack: () -> Unit, onPay: () -> Unit) {
    WalletMockStore.version
    val scope = rememberCoroutineScope()
    // 进入页面即拉取远端钱包汇总与流水（失败只提示，保留旧缓存）
    LaunchedEffect(Unit) { WalletMockStore.refresh() }
    var amount by remember { mutableStateOf("") }
    var bank by remember { mutableStateOf("") }
    var last4 by remember { mutableStateOf("") }
    // Flutter WalletPage: 支付宝=1 / 微信=2 / 银行卡=3
    var channel by remember { mutableStateOf(1) }
    val cards = WalletMockStore.cards()
    val ledger = WalletMockStore.ledger()
    // Dart wallet_controller.dart:47-53：默认卡优先，否则取第一张
    val defaultCardId = cards.firstOrNull { it.isDefault }?.cardId ?: cards.firstOrNull()?.cardId
    val cardShape = RoundedCornerShape(MineTheme.RadiusMd)
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(MineTheme.Background)) {
        WalletFlutterNavBar(title = "我的钱包", onBack = onBack)
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(cardShape)
                    .background(MineTheme.Surface)
                    .border(MineTheme.Hairline, MineTheme.Separator, cardShape)
                    .padding(horizontal = 16.dp, vertical = 20.dp),
            ) {
                Text("余额（元）", color = MineTheme.LabelTertiary, fontSize = MineTheme.CaptionSize)
                Spacer(Modifier.height(8.dp))
                Text(
                    WalletMockStore.balanceYuan(),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MineTheme.LabelPrimary,
                )
            }
            if (WalletMockStore.loading) {
                Text("加载中…", color = MineTheme.LabelTertiary, fontSize = MineTheme.CaptionSize)
            }
            WalletMockStore.errorMessage?.let { message ->
                Text(message, color = MineTheme.Danger, fontSize = MineTheme.CaptionSize)
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(cardShape)
                    .background(MineTheme.Surface)
                    .border(MineTheme.Hairline, MineTheme.Separator, cardShape)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("充值", fontWeight = FontWeight.SemiBold, fontSize = MineTheme.HeadlineSize, color = MineTheme.LabelPrimary)
                BasicTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = MineTheme.BodySize, color = MineTheme.LabelPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(cardShape)
                        .border(MineTheme.Hairline, MineTheme.Separator, cardShape)
                        .padding(12.dp),
                    decorationBox = { inner ->
                        if (amount.isEmpty()) {
                            Text("请输入金额", color = MineTheme.LabelTertiary, fontSize = MineTheme.BodySize)
                        }
                        inner()
                    },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("10", "50", "100").forEach { quick ->
                        Text(
                            "¥$quick",
                            color = MineTheme.Accent,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .border(MineTheme.Hairline, MineTheme.Accent, RoundedCornerShape(16.dp))
                                .clickable { amount = quick }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1 to "支付宝", 2 to "微信", 3 to "银行卡").forEach { (id, label) ->
                        val selected = channel == id
                        Text(
                            label,
                            color = if (selected) DemoColors.OnPrimary else MineTheme.LabelPrimary,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (selected) MineTheme.Accent else MineTheme.FillSecondary)
                                .clickable { channel = id }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
                Text(
                    "确认充值",
                    color = DemoColors.OnPrimary,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(MineTheme.Accent)
                        .clickable {
                            val err = WalletMockStore.validateRecharge(amount)
                            if (err != null) {
                                showPlatformToast(err)
                            } else {
                                scope.launch {
                                    if (WalletMockStore.recharge(amount, channel, defaultCardId)) {
                                        amount = ""
                                        showPlatformToast("充值成功")
                                    } else {
                                        showPlatformToast(WalletMockStore.errorMessage ?: "充值失败")
                                    }
                                }
                            }
                        }
                        .padding(vertical = 12.dp),
                )
                Text(
                    "去支付模块",
                    color = MineTheme.Accent,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable(onClick = onPay).padding(top = 4.dp),
                )
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(cardShape)
                    .background(MineTheme.Surface)
                    .border(MineTheme.Hairline, MineTheme.Separator, cardShape)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("银行卡", fontWeight = FontWeight.SemiBold, fontSize = MineTheme.HeadlineSize, color = MineTheme.LabelPrimary)
                cards.forEach { card ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(card.display, fontSize = MineTheme.CaptionSize, color = MineTheme.LabelPrimary)
                            if (card.isDefault) {
                                Text("默认", color = MineTheme.Accent, fontSize = 12.sp)
                            }
                        }
                        if (!card.isDefault) {
                            Text(
                                "设默认",
                                color = MineTheme.Accent,
                                fontSize = 13.sp,
                                modifier = Modifier
                                    .clickable {
                                        scope.launch {
                                            if (WalletMockStore.setDefaultCard(card.cardId)) {
                                                showPlatformToast("已设为默认卡")
                                            } else {
                                                showPlatformToast(WalletMockStore.errorMessage ?: "设置失败")
                                            }
                                        }
                                    }
                                    .padding(vertical = 12.dp),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "解绑",
                            color = MineTheme.LabelTertiary,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .clickable {
                                    scope.launch {
                                        if (WalletMockStore.deleteCard(card.cardId)) {
                                            showPlatformToast("已解绑")
                                        } else {
                                            showPlatformToast(WalletMockStore.errorMessage ?: "解绑失败")
                                        }
                                    }
                                }
                                .padding(vertical = 12.dp),
                        )
                    }
                }
                BasicTextField(
                    value = bank,
                    onValueChange = { bank = it },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = MineTheme.BodySize, color = MineTheme.LabelPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(cardShape)
                        .border(MineTheme.Hairline, MineTheme.Separator, cardShape)
                        .padding(12.dp),
                    decorationBox = { inner ->
                        if (bank.isEmpty()) Text("银行名", color = MineTheme.LabelTertiary, fontSize = MineTheme.BodySize)
                        inner()
                    },
                )
                BasicTextField(
                    value = last4,
                    onValueChange = { if (it.length <= 4) last4 = it.filter { c -> c.isDigit() } },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = MineTheme.BodySize, color = MineTheme.LabelPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(cardShape)
                        .border(MineTheme.Hairline, MineTheme.Separator, cardShape)
                        .padding(12.dp),
                    decorationBox = { inner ->
                        if (last4.isEmpty()) Text("卡号后四位", color = MineTheme.LabelTertiary, fontSize = MineTheme.BodySize)
                        inner()
                    },
                )
                Text(
                    "绑定银行卡",
                    color = MineTheme.Accent,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .border(MineTheme.Hairline, MineTheme.Accent, RoundedCornerShape(22.dp))
                        .clickable {
                            val err = WalletMockStore.validateBindCard(bank, last4)
                            if (err != null) {
                                showPlatformToast(err)
                            } else {
                                scope.launch {
                                    if (WalletMockStore.bindCard(bank, last4) != null) {
                                        bank = ""
                                        last4 = ""
                                        showPlatformToast("绑卡成功")
                                    } else {
                                        showPlatformToast(WalletMockStore.errorMessage ?: "绑卡失败")
                                    }
                                }
                            }
                        }
                        .padding(vertical = 12.dp),
                )
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(cardShape)
                    .background(MineTheme.Surface)
                    .border(MineTheme.Hairline, MineTheme.Separator, cardShape)
                    .padding(16.dp),
            ) {
                Text("流水", fontWeight = FontWeight.SemiBold, fontSize = MineTheme.HeadlineSize, color = MineTheme.LabelPrimary)
                Spacer(Modifier.height(8.dp))
                if (ledger.isEmpty()) {
                    Text("暂无流水", color = MineTheme.LabelTertiary, fontSize = MineTheme.CaptionSize)
                } else {
                    ledger.take(10).forEach { e ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text(e.reasonLabel, fontSize = MineTheme.CaptionSize, color = MineTheme.LabelPrimary)
                                Text(
                                    "余额 ¥${e.balanceFen / 100}.${(e.balanceFen % 100).toString().padStart(2, '0')}",
                                    fontSize = 12.sp,
                                    color = MineTheme.LabelTertiary,
                                )
                            }
                            Text(
                                e.deltaYuan,
                                color = if (e.deltaFen >= 0) Color(0xFF16A34A) else MineTheme.Danger,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        }
    }
}

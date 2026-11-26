package com.example.my_kmp_project.feature.mine

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.my_kmp_project.core.platform.showPlatformToast
import com.example.my_kmp_project.core.ui.ReportMainTabRoot

@Composable
internal fun SmsTemplateScreen(onBack: () -> Unit) {
    val templates = listOf(
        "到店提醒" to "尊敬的客户，预约保养已排至今日 14:00，请准时到店。",
        "试驾确认" to "您好，试驾预约已确认，顾问将提前电话联系您。",
        "活动邀约" to "本周末门店试驾会，到店即送礼品，欢迎莅临。",
        "回访关怀" to "购车已满一周，如有用车问题请随时联系您的顾问。",
    )
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(MineTheme.Background)) {
        MineTopBar(title = "短信模板", onBack = onBack, containerColor = MineTheme.Surface)
        LazyColumn(contentPadding = PaddingValues(16.dp)) {
            items(templates) { (title, body) ->
                Column(
                    Modifier
                        .padding(bottom = 10.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(MineTheme.RadiusMd))
                        .background(MineTheme.Surface)
                        .border(MineTheme.Hairline, MineTheme.Separator, RoundedCornerShape(MineTheme.RadiusMd))
                        .padding(14.dp),
                ) {
                    Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(body, color = DemoColors.TextSecondary, fontSize = 13.sp)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "一键发送",
                        color = MineTheme.Accent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .align(Alignment.End)
                            .clickable { showPlatformToast("已复制并打开短信（mock）") },
                    )
                }
            }
        }
    }
}

@Composable
internal fun ShopQrScreen(onBack: () -> Unit) {
    ReportMainTabRoot(isRoot = false)
    Column(
        Modifier
            .fillMaxSize()
            .background(MineTheme.Background)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MineTopBar(title = "店铺收款码", onBack = onBack, containerColor = MineTheme.Surface)
        Spacer(Modifier.height(24.dp))
        Column(
            Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MineTheme.Surface)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("演示门店", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(4.dp))
            Text("扫码向本店付款", color = DemoColors.TextSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(20.dp))
            Box(
                Modifier
                    .width(200.dp)
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF111111)),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .width(160.dp)
                        .height(160.dp)
                        .background(MineTheme.Surface),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("QR", fontWeight = FontWeight.Bold, fontSize = 28.sp, color = Color(0xFF111111))
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("支持微信 / 支付宝", color = DemoColors.TextSecondary, fontSize = 12.sp)
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "保存到相册",
            color = MineTheme.Accent,
            modifier = Modifier.clickable { showPlatformToast("已保存（mock）") },
        )
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
internal fun InviteScreen(onBack: () -> Unit) {
    ReportMainTabRoot(isRoot = false)
    Column(
        Modifier
            .fillMaxSize()
            .background(MineTheme.Background)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MineTopBar(title = "邀请好友", onBack = onBack, containerColor = MineTheme.Surface)
        Spacer(Modifier.height(20.dp))
        Column(
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MineTheme.Accent)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("邀请码", color = DemoColors.OnPrimary.copy(alpha = 0.85f), fontSize = 13.sp)
            Spacer(Modifier.height(6.dp))
            Text("DEMO2026", color = DemoColors.OnPrimary, fontWeight = FontWeight.Bold, fontSize = 28.sp)
            Spacer(Modifier.height(8.dp))
            Text("好友注册并登录 · 双方各得 50 积分", color = DemoColors.OnPrimary.copy(alpha = 0.9f), fontSize = 13.sp)
        }
        Spacer(Modifier.height(16.dp))
        listOf("微信好友", "朋友圈", "复制邀请链接").forEach { action ->
            Text(
                action,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 5.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(MineTheme.RadiusMd))
                    .background(MineTheme.Surface)
                    .clickable { showPlatformToast("$action（mock）") }
                    .padding(16.dp),
                fontWeight = FontWeight.Medium,
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
internal fun CooperationScreen(onBack: () -> Unit) {
    var company by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(MineTheme.Background)) {
        MineTopBar(title = "商务合作", onBack = onBack, containerColor = MineTheme.Surface)
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("提交合作意向，运营将在 1–2 个工作日内联系您。", color = DemoColors.TextSecondary, fontSize = 13.sp)
            listOf(
                Triple("公司 / 品牌", company) { v: String -> company = v },
                Triple("联系人手机", contact) { v: String -> contact = v },
            ).forEach { (label, value, onChange) ->
                Column {
                    Text(label, fontSize = 13.sp, color = DemoColors.TextSecondary)
                    Spacer(Modifier.height(4.dp))
                    BasicTextField(
                        value = value,
                        onValueChange = onChange,
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 15.sp, color = DemoColors.TextPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(MineTheme.RadiusMd))
                            .background(MineTheme.Surface)
                            .border(MineTheme.Hairline, MineTheme.Separator, RoundedCornerShape(MineTheme.RadiusMd))
                            .padding(14.dp),
                    )
                }
            }
            Column {
                Text("合作说明", fontSize = 13.sp, color = DemoColors.TextSecondary)
                Spacer(Modifier.height(4.dp))
                BasicTextField(
                    value = note,
                    onValueChange = { note = it },
                    textStyle = TextStyle(fontSize = 15.sp, color = DemoColors.TextPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(MineTheme.RadiusMd))
                        .background(MineTheme.Surface)
                        .border(MineTheme.Hairline, MineTheme.Separator, RoundedCornerShape(MineTheme.RadiusMd))
                        .padding(14.dp),
                    decorationBox = { inner ->
                        if (note.isEmpty()) Text("品牌露出 / 渠道合作 / 活动联办…", color = DemoColors.Muted)
                        inner()
                    },
                )
            }
            Button(
                onClick = {
                    when {
                        company.isBlank() || contact.isBlank() -> showPlatformToast("请填写公司与联系人")
                        else -> {
                            showPlatformToast("已提交")
                            onBack()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MineTheme.Accent),
            ) { Text("提交申请") }
        }
    }
}

@Composable
internal fun ReminderScreen(onBack: () -> Unit) {
    val today = listOf(
        "14:00 张先生试驾回访" to "高意向 · 销售顾问",
        "16:30 保养交车提醒" to "工单 AS-441",
        "20:00 直播线索复盘" to "运营协作",
    )
    val tomorrow = listOf("10:00 门店晨会" to "全员")
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(MineTheme.Background)) {
        MineTopBar(title = "提醒事项", onBack = onBack, containerColor = MineTheme.Surface)
        LazyColumn(contentPadding = PaddingValues(16.dp)) {
            item {
                Text("今日 · ${today.size} 条", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
            }
            items(today) { (title, sub) ->
                Column(
                    Modifier
                        .padding(bottom = 8.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(MineTheme.RadiusMd))
                        .background(MineTheme.Surface)
                        .border(MineTheme.Hairline, MineTheme.Separator, RoundedCornerShape(MineTheme.RadiusMd))
                        .padding(14.dp),
                ) {
                    Text(title, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))
                    Text(sub, fontSize = 13.sp, color = DemoColors.TextSecondary)
                }
            }
            item {
                Text("明日 · ${tomorrow.size} 条", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
            }
            items(tomorrow) { (title, sub) ->
                Column(
                    Modifier
                        .padding(bottom = 8.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(MineTheme.RadiusMd))
                        .background(MineTheme.Surface)
                        .border(MineTheme.Hairline, MineTheme.Separator, RoundedCornerShape(MineTheme.RadiusMd))
                        .padding(14.dp),
                ) {
                    Text(title, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))
                    Text(sub, fontSize = 13.sp, color = DemoColors.TextSecondary)
                }
            }
        }
    }
}

@Composable
internal fun BuyQaScreen(onBack: () -> Unit) {
    val rows = listOf(
        "全款和贷款怎么选？" to "已解答 · 顾问回复",
        "置换能抵多少？" to "待回复 · 客户追问",
        "保养周期多久一次？" to "已解答 · 知识库",
    )
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(MineTheme.Background)) {
        MineTopBar(title = "选买问答", onBack = onBack, containerColor = MineTheme.Surface)
        LazyColumn(contentPadding = PaddingValues(16.dp)) {
            items(rows) { (q, status) ->
                Column(
                    Modifier
                        .padding(bottom = 10.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(MineTheme.RadiusMd))
                        .background(MineTheme.Surface)
                        .border(MineTheme.Hairline, MineTheme.Separator, RoundedCornerShape(MineTheme.RadiusMd))
                        .padding(14.dp),
                ) {
                    Text(q, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(status, color = DemoColors.TextSecondary, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
internal fun PosterScreen(onBack: () -> Unit) {
    val posters = listOf("置换专场", "专卖精选", "估价引流", "到店礼")
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(MineTheme.Background)) {
        MineTopBar(title = "商家海报", onBack = onBack, containerColor = MineTheme.Surface)
        LazyColumn(contentPadding = PaddingValues(16.dp)) {
            items(posters.chunked(2)) { row ->
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    row.forEach { title ->
                        Column(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MineTheme.Surface)
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFFFF3E0)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(title.take(2), fontWeight = FontWeight.Bold, color = Color(0xFFFF9500))
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
internal fun FeedbackScreen(onBack: () -> Unit) {
    var text by remember { mutableStateOf("") }
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(MineTheme.Background)) {
        MineTopBar(title = "意见反馈", onBack = onBack, containerColor = MineTheme.Surface)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                textStyle = TextStyle(fontSize = 15.sp, color = DemoColors.TextPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(MineTheme.RadiusMd))
                    .background(MineTheme.Surface)
                    .border(MineTheme.Hairline, MineTheme.Separator, RoundedCornerShape(MineTheme.RadiusMd))
                    .padding(14.dp),
                decorationBox = { inner ->
                    if (text.isEmpty()) Text("请描述问题或建议…", color = DemoColors.Muted)
                    inner()
                },
            )
            Button(
                onClick = {
                    if (text.isBlank()) showPlatformToast("请填写反馈内容")
                    else {
                        showPlatformToast("已提交")
                        onBack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MineTheme.Accent),
            ) { Text("提交") }
        }
    }
}

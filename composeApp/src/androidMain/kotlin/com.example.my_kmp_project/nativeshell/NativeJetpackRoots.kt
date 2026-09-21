package com.example.my_kmp_project.nativeshell

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.feature.home.HomeAssetIcon
import com.example.my_kmp_project.feature.home.HomeServiceAssets

@Composable
internal fun JetpackHomeRoot(onDeferred: (String) -> Unit) {
    val features = listOf(
        "销售顾问", "生活服务", "二手车", "新车关注", "AI小石头",
        "订单中心", "数据分析", "直播带货", "营销活动", "更多",
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DemoColors.PageBg)
            .statusBarsPadding(),
    ) {
        Text(
            "首页",
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = DemoColors.TextPrimary,
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("早上好，沃德龙鼎", fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = DemoColors.TextPrimary, modifier = Modifier.weight(1f))
                    Text(
                        "3条新消息",
                        color = DemoColors.Accent,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(DemoColors.Accent.copy(alpha = 0.12f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
            item {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DemoColors.Background)
                            .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(12.dp))
                            .clickable { onDeferred("搜索") }
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("⌕", color = DemoColors.TextSecondary, fontSize = 18.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("搜索客户、订单、资讯", color = DemoColors.TextSecondary, fontSize = 15.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DemoColors.Background)
                            .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(12.dp))
                            .clickable { onDeferred("扫一扫") },
                        contentAlignment = Alignment.Center,
                    ) { Text("▦", color = DemoColors.Accent, fontSize = 18.sp) }
                }
            }
            item {
                Column(
                    Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.horizontalGradient(listOf(DemoColors.Accent, DemoColors.Accent.copy(alpha = 0.55f))))
                        .padding(20.dp),
                ) {
                    Text("朋友圈营销", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                    Text("一键分享，高效触达客户", color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "立即体验",
                        color = DemoColors.Accent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
            item {
                Column(Modifier.padding(16.dp)) {
                    features.chunked(5).forEach { row ->
                        Row(Modifier.fillMaxWidth()) {
                            row.forEach { label ->
                                Column(
                                    Modifier
                                        .weight(1f)
                                        .clickable {
                                            when (label) {
                                                "更多" -> onDeferred("全部服务")
                                                "直播带货" -> onDeferred("直播")
                                                else -> Unit
                                            }
                                        }
                                        .padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Box(
                                        Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(12.dp)),
                                    ) {
                                        HomeAssetIcon(
                                            resource = HomeServiceAssets.featureForLabel(label),
                                            size = 48.dp,
                                            contentDescription = label,
                                        )
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Text(label, fontSize = 11.sp, color = DemoColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
            item {
                Column(
                    Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DemoColors.Background)
                        .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                ) {
                    Text("[4S]北京沃德龙鼎吉利", fontWeight = FontWeight.SemiBold, color = DemoColors.TextPrimary)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("今日", "昨日", "本月").forEachIndexed { i, t ->
                            Text(
                                t,
                                color = if (i == 0) DemoColors.Accent else DemoColors.TextSecondary,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (i == 0) DemoColors.Accent.copy(alpha = 0.12f) else Color.Transparent)
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth()) {
                        listOf("99" to "意向客户", "2" to "新车订单", "999.8" to "成交额(万)", "15" to "试驾预约").forEach { (v, l) ->
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(v, fontWeight = FontWeight.SemiBold, color = DemoColors.TextPrimary)
                                Text(l, fontSize = 11.sp, color = DemoColors.TextSecondary)
                            }
                        }
                    }
                }
            }
            item {
                Row(
                    Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DemoColors.Background)
                        .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(12.dp))
                        .clickable { onDeferred("投资策略") }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(28.dp).clip(CircleShape).background(DemoColors.Accent),
                        contentAlignment = Alignment.Center,
                    ) { Text("投", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("投资策略", color = DemoColors.TextPrimary)
                        Text("资产九宫格 · 恐贪定投 · 趋势策略", fontSize = 12.sp, color = DemoColors.TextSecondary)
                    }
                    Text("›", color = DemoColors.TextSecondary)
                }
            }
        }
    }
}

@Composable
internal fun JetpackChatRoot() {
    val peers = listOf(
        Triple("Mock好友1", "你好，最近怎么样？", "2"),
        Triple("Mock好友2", "明天一起开会吧", null),
        Triple("Mock好友3", "收到，谢谢", null),
    )
    Column(
        Modifier
            .fillMaxSize()
            .background(DemoColors.PageBg)
            .statusBarsPadding(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("消息", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = DemoColors.TextPrimary, modifier = Modifier.weight(1f))
            Text("⌕", fontSize = 20.sp, color = DemoColors.TextPrimary, modifier = Modifier.padding(end = 12.dp))
            Text("✎", fontSize = 20.sp, color = DemoColors.TextPrimary)
        }
        peers.forEach { (name, snippet, badge) ->
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(48.dp).clip(CircleShape).background(DemoColors.Accent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) { Text(name.takeLast(1), color = DemoColors.Accent) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(name, fontWeight = FontWeight.SemiBold, color = DemoColors.TextPrimary)
                    Text(snippet, fontSize = 13.sp, color = DemoColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (badge != null) {
                    Text(
                        badge,
                        color = Color.White,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFEE0000))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            HorizontalDivider(color = DemoColors.Divider)
        }
    }
}

@Composable
internal fun JetpackCommunityRoot() {
    var filter by remember { mutableStateOf("最新") }
    val posts = listOf(
        Triple("张三", "7分钟前 · 来自 iPhone", "今天去了 @张三 推荐的咖啡店，环境不错。\n#Flutter开发"),
        Triple("李四", "42分钟前 · 来自 Android", "周末 hiking，天气太好了！#户外"),
        Triple("王五", "61分钟前 · 来自 iPhone", "刚读完一本好书，推荐 @李四 也看看。"),
    )
    Column(
        Modifier
            .fillMaxSize()
            .background(DemoColors.PageBg)
            .statusBarsPadding(),
    ) {
        Text(
            "社区",
            Modifier.fillMaxWidth().padding(16.dp),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = DemoColors.TextPrimary,
        )
        Row(Modifier.fillMaxWidth().background(DemoColors.Background).padding(vertical = 10.dp)) {
            listOf("最新", "热门", "关注").forEach { item ->
                Text(
                    item,
                    color = if (filter == item) DemoColors.Accent else DemoColors.TextSecondary,
                    fontWeight = if (filter == item) FontWeight.SemiBold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f).clickable { filter = item },
                )
            }
        }
        LazyColumn {
            items(posts.size) { i ->
                val p = posts[i]
                Column(Modifier.padding(16.dp)) {
                    Text(p.first, fontWeight = FontWeight.SemiBold, color = DemoColors.TextPrimary)
                    Text(p.second, fontSize = 11.sp, color = DemoColors.TextSecondary)
                    Spacer(Modifier.height(8.dp))
                    Text(p.third, color = DemoColors.TextSecondary, fontSize = 14.sp)
                }
                HorizontalDivider(color = DemoColors.Divider)
            }
        }
    }
}

@Composable
internal fun JetpackMineRoot(
    loggedIn: Boolean,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPersonalized: () -> Unit,
    onDeferred: (String) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(DemoColors.PageBg)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(bottom = 24.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            Text("ⓘ", modifier = Modifier.padding(end = 12.dp).clickable(onClick = onOpenPersonalized))
            Text("⚙", modifier = Modifier.padding(end = 12.dp).clickable(onClick = onOpenSettings))
            Text(if (loggedIn) "⎋" else "⇢", modifier = Modifier.clickable { if (loggedIn) onLogout() else onLogin() })
        }
        Column(
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DemoColors.Background)
                .padding(16.dp),
        ) {
            Text(if (loggedIn) "用户0000" else "未登录", fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = DemoColors.TextPrimary)
            if (loggedIn) {
                Text("销售经理", color = DemoColors.Accent, fontSize = 12.sp)
                Text("[4S]北京大兴兴荣丰田汽车销售服务有限公司", color = DemoColors.TextSecondary, fontSize = 12.sp)
            } else {
                Spacer(Modifier.height(8.dp))
                Button(onClick = onLogin, colors = ButtonDefaults.buttonColors(containerColor = DemoColors.Primary)) {
                    Text("登录")
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DemoColors.Background)
                .padding(12.dp),
        ) {
            listOf(
                (if (loggedIn) "1028" else "0") to "加入天数",
                (if (loggedIn) "28" else "0") to "员工数",
                (if (loggedIn) "2059" else "0") to "店铺天数",
                (if (loggedIn) "9366" else "0") to "累计客户",
            ).forEach { (v, l) ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(v, fontWeight = FontWeight.SemiBold, color = DemoColors.TextPrimary)
                    Text(l, fontSize = 11.sp, color = DemoColors.TextSecondary)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("常用服务", Modifier.padding(horizontal = 16.dp), fontWeight = FontWeight.SemiBold, color = DemoColors.TextPrimary)
        Row(Modifier.fillMaxWidth().padding(16.dp)) {
            listOf("商城", "我的钱包", "我的课程", "我的订单").forEach { label ->
                Column(
                    Modifier.weight(1f).clickable { onDeferred(label) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(DemoColors.Accent.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) { Text(label.take(1), color = DemoColors.Accent) }
                    Spacer(Modifier.height(6.dp))
                    Text(label, fontSize = 11.sp, color = DemoColors.TextPrimary)
                }
            }
        }
        Column(
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DemoColors.Background),
        ) {
            listOf("商务合作", "提醒事项", "邀请好友", "粉丝群", "意见反馈", "设置").forEach { label ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            when (label) {
                                "设置" -> onOpenSettings()
                                else -> onDeferred(label)
                            }
                        }
                        .padding(16.dp),
                ) {
                    Text(label, Modifier.weight(1f), color = DemoColors.TextPrimary)
                    Text("›", color = DemoColors.TextSecondary)
                }
                HorizontalDivider(color = DemoColors.Divider)
            }
        }
    }
}

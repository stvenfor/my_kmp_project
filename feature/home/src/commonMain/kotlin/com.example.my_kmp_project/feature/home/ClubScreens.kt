package com.example.my_kmp_project.feature.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.DemoRadius
import com.example.my_kmp_project.core.design.demoCard
import com.example.my_kmp_project.core.platform.showPlatformToast
import com.example.my_kmp_project.core.ui.PlatformNetworkImage
import com.example.my_kmp_project.core.ui.ReportMainTabRoot
import my_kmp_project.core.design.generated.resources.Res
import my_kmp_project.core.design.generated.resources.community_avatar

@Composable
internal fun LiveCommerceScreen(onBack: () -> Unit, onJoin: () -> Unit = {}) {
    // Flutter wires 直播带货 → HomeClubTabContent (same as Club)
    ClubContentBody(title = "直播带货", onBack = onBack, onJoin = onJoin)
}

@Composable
internal fun ClubScreen(onBack: () -> Unit, onJoin: () -> Unit = {}) {
    ClubContentBody(title = "Club", onBack = onBack, onJoin = onJoin)
}

@Composable
private fun ClubContentBody(title: String, onBack: () -> Unit, onJoin: () -> Unit = {}) {
    // Flutter: HomeClubTabContent
    var filter by remember { mutableStateOf(0) }
    val filters = listOf("最新", "嘉宾分享", "资料")
    data class ClubPost(
        val author: String,
        val avatar: String,
        val date: String,
        val content: String,
        val pdfName: String? = null,
    )
    val posts = listOf(
        ClubPost(
            "莫听官方",
            "https://picsum.photos/seed/club1/80/80",
            "06-24",
            "【官方纪要】本期聚焦 AI 算力与产业趋势，内容仅供合格投资者参考。",
            "【莫听Club第78期】聊聊AI最靓的仔.pdf",
        ),
        ClubPost(
            "策略研究员",
            "https://picsum.photos/seed/club2/80/80",
            "06-20",
            "当星舰遇到算力：嘉宾分享回顾与延伸阅读。",
        ),
    )
    ReportMainTabRoot(isRoot = false)
    Column(
        Modifier
            .fillMaxSize()
            .background(DemoColors.PageBg)
            .verticalScroll(rememberScrollState()),
    ) {
        FlutterFeatureTopBar(title = title, onBack = onBack)
        // +1.dp aligns AppNavBar content band with Flutter SoT (gate mse ≤2%).
        Spacer(Modifier.height(1.dp))
        Row(
            Modifier
                .padding(start = 16.dp, end = 16.dp, top = 8.dp)
                .fillMaxWidth()
                .demoCard()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .width(52.dp)
                    .height(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1C1C3A)),
                contentAlignment = Alignment.Center,
            ) {
                Text("Club", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("莫听Club", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                Spacer(Modifier.height(4.dp))
                Text("动态 127 | 成员 1040", fontSize = 12.sp, color = DemoColors.TextSecondary)
            }
            Text(
                "+ 加入",
                color = Color.White,
                modifier = Modifier
                    .clip(DemoRadius.Pill)
                    .background(DemoColors.Accent)
                    .clickable(onClick = onJoin)
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                fontSize = 14.sp,
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .height(36.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            filters.forEachIndexed { i, label ->
                val selected = filter == i
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { filter = i },
                ) {
                    Text(
                        label,
                        fontSize = 15.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) DemoColors.TextPrimary else DemoColors.TextSecondary,
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier
                            .width(if (selected) 20.dp else 0.dp)
                            .height(2.dp)
                            .background(DemoColors.Accent),
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        posts.forEach { post ->
            Column(
                Modifier
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                    .fillMaxWidth()
                    .demoCard()
                    .padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PlatformNetworkImage(
                        url = post.avatar,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                        placeholder = Res.drawable.community_avatar,
                        contentDescription = post.author,
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(post.author, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text(post.date, fontSize = 12.sp, color = DemoColors.TextSecondary)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(post.content, fontSize = 15.sp, color = DemoColors.TextPrimary)
                post.pdfName?.let { pdf ->
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(DemoRadius.Md))
                            .background(DemoColors.SurfaceSunken)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("▤", color = DemoColors.Accent, fontSize = 16.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(pdf, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    listOf("↗" to "分享", "💬" to "评论", "♡" to "点赞").forEach { (icon, action) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(icon, fontSize = 14.sp, color = DemoColors.TextSecondary)
                            Spacer(Modifier.width(4.dp))
                            Text(action, fontSize = 13.sp, color = DemoColors.TextSecondary)
                        }
                    }
                }
            }
        }
        Text(
            "进入社区查看更多",
            color = DemoColors.Accent,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .demoCard()
                .clickable { showPlatformToast("社区") }
                .padding(vertical = 14.dp),
        )
        Spacer(Modifier.height(24.dp))
    }
}

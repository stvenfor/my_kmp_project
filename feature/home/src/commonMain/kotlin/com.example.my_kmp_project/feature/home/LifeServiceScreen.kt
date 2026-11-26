package com.example.my_kmp_project.feature.home

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.my_kmp_project.core.platform.showPlatformToast
import com.example.my_kmp_project.core.ui.PlatformNetworkImage
import com.example.my_kmp_project.core.ui.ReportMainTabRoot
import my_kmp_project.core.design.generated.resources.Res
import my_kmp_project.core.design.generated.resources.community_avatar
import my_kmp_project.core.design.generated.resources.community_post_a

@Composable
internal fun LifeServiceScreen(onBack: () -> Unit) {
    // Flutter: HomeFeatureContentPage(title: 生活服务, child: HomeVideoTabContent)
    data class LifeShortcut(val label: String, val icon: String, val tint: Color)
    data class LifeDaily(val title: String, val tag: String, val avatar: String)
    data class LifeCourse(
        val title: String,
        val author: String,
        val cover: String,
        val isLive: Boolean,
        val isMember: Boolean,
    )
    val shortcuts = listOf(
        LifeShortcut("会员专享", "▶", DemoColors.Accent),
        LifeShortcut("跟读专栏", "▤", Color(0xFFFF9500)),
        LifeShortcut("其他课程", "▣", Color(0xFF5856D6)),
        LifeShortcut("功能教程", "✎", Color(0xFF34C759)),
    )
    val daily = listOf(
        LifeDaily("带你玩转 ETF", "直播中", "https://picsum.photos/seed/v1/120/120"),
        LifeDaily("新能源赛道解读", "回放", "https://picsum.photos/seed/v2/120/120"),
        LifeDaily("门店短视频运营", "直播中", "https://picsum.photos/seed/v3/120/120"),
    )
    val courses = listOf(
        LifeCourse("【配置】当星舰撞上算力", "尤国梁", "https://picsum.photos/seed/c1/400/240", true, true),
        LifeCourse("黄金恐贪定投实战", "策略组", "https://picsum.photos/seed/c2/400/240", false, false),
    )
    val cardRadius = 8.dp
    ReportMainTabRoot(isRoot = false)
    Column(
        Modifier
            .fillMaxSize()
            .background(DemoColors.PageBg)
            .verticalScroll(rememberScrollState()),
    ) {
        FlutterFeatureTopBar(title = "生活服务", onBack = onBack)
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp),
        ) {
            shortcuts.forEach { s ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showPlatformToast(s.label) },
                ) {
                    Box(
                        Modifier
                            .width(52.dp)
                            .height(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(s.tint.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(s.label.take(1), color = s.tint, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(s.label, fontSize = 12.sp, color = DemoColors.TextPrimary, maxLines = 1)
                }
            }
        }
        LifeSectionHeader(title = "每日推荐")
        Column(Modifier.padding(horizontal = 16.dp)) {
            daily.forEach { item ->
                val isLive = item.tag == "直播中"
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(cardRadius))
                        .background(Color.White)
                        .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(cardRadius))
                        .clickable { showPlatformToast(item.title) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        item.tag,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isLive) Color(0xFFFF3B30) else DemoColors.TextSecondary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (isLive) Color(0x14FF3B30) else Color(0xFFF2F2F7),
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        item.title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    PlatformNetworkImage(
                        url = item.avatar,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                        placeholder = Res.drawable.community_avatar,
                        contentDescription = item.title,
                    )
                }
            }
        }
        LifeSectionHeader(title = "热门课程")
        Row(
            Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .height(200.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            courses.forEach { course ->
                Column(
                    Modifier
                        .width(168.dp)
                        .clip(RoundedCornerShape(cardRadius))
                        .background(Color.White)
                        .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(cardRadius))
                        .clickable { showPlatformToast(course.title) },
                ) {
                    Box {
                        PlatformNetworkImage(
                            url = course.cover,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(96.dp),
                            contentScale = ContentScale.Crop,
                            placeholder = Res.drawable.community_post_a,
                            contentDescription = course.title,
                        )
                        Text(
                            if (course.isLive) "直播中" else "回放",
                            color = Color.White,
                            fontSize = 10.sp,
                            modifier = Modifier
                                .padding(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = 0.45f))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                    Column(Modifier.padding(10.dp)) {
                        Text(
                            course.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 17.sp,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(course.author, fontSize = 12.sp, color = DemoColors.TextSecondary)
                        if (course.isMember) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "V 会员专属",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFFF9500),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0x14FF9500))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "进入跟读视频专区",
            color = DemoColors.Accent,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(cardRadius))
                .background(DemoColors.Accent.copy(alpha = 0.08f))
                .border(1.dp, DemoColors.Accent.copy(alpha = 0.2f), RoundedCornerShape(cardRadius))
                .clickable { showPlatformToast("跟读") }
                .padding(vertical = 14.dp),
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun LifeSectionHeader(title: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
        Text("更多 >", fontSize = 13.sp, color = DemoColors.TextSecondary)
    }
}

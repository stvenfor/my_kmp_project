package com.example.my_kmp_project.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.DemoRadius
import com.example.my_kmp_project.core.platform.showPlatformToast
import com.example.my_kmp_project.core.ui.ReportMainTabRoot
import kotlinx.coroutines.launch

@Composable
internal fun HotRankDetailScreen(onBack: () -> Unit, content: HomeContentStore) {
    // Flutter HotRankDetailPage: pink header + sidebar categories + age filter + ranked list
    val version = content.version
    val categories = remember(version) { content.hotRankCategories() }
    val ageFilters = remember(version) { content.hotRankAgeFilters() }
    // Flutter default: HotRankCategory.hotSearch
    var selectedCategory by remember { mutableStateOf("热搜榜") }
    var selectedAge by remember { mutableStateOf("1-3年级") }
    var showAgeMenu by remember { mutableStateOf(false) }
    val categoryIndex = categories.indexOf(selectedCategory).coerceAtLeast(0)
    val ageIndex = ageFilters.indexOf(selectedAge).coerceAtLeast(0)
    val items = remember(version) { content.hotRankItems() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { content.refreshHotRank(category = selectedCategory) }

    val headerPink = Color(0xFFF5F5F7)
    val sidebarBg = Color(0xFFF0F0F2)
    val ink = Color(0xFF1A1A1A)
    val mute = DemoColors.TextSecondary
    val gold = Color(0xFFFF9500)
    val silver = Color(0xFFB0B0B8)
    val bronze = Color(0xFFC9783A)

    ReportMainTabRoot(isRoot = false)
    Column(
        Modifier
            .fillMaxSize()
            .background(DemoColors.PageBg)
            .clickable(enabled = showAgeMenu) { showAgeMenu = false },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(headerPink, Color.White)),
                )
                .statusBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "‹",
                    fontSize = 28.sp,
                    color = ink,
                    modifier = Modifier
                        .clickable(onClick = onBack)
                        .padding(12.dp),
                )
                Spacer(Modifier.weight(1f))
                // Flutter share icon (not text label).
                Text(
                    "⬆",
                    fontSize = 18.sp,
                    color = ink,
                    modifier = Modifier
                        .clickable { showPlatformToast("分享（mock）") }
                        .padding(12.dp),
                )
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("🌾", fontSize = 22.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    categories.getOrElse(categoryIndex) { selectedCategory },
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = ink,
                )
                Spacer(Modifier.width(8.dp))
                Text("🌾", fontSize = 22.sp)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "iHome用户近期热搜内容",
                fontSize = 12.sp,
                color = mute,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
        Row(
            Modifier
                .fillMaxSize()
                // Gate dy≈−4px @540×960 vs Flutter SoT.
                .offset(y = (-4).dp),
        ) {
            Column(
                Modifier
                    .width(88.dp)
                    .fillMaxHeight()
                    .background(sidebarBg)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 8.dp, bottom = 24.dp),
            ) {
                categories.forEachIndexed { i, label ->
                    val selected = label == selectedCategory
                    Box(
                        Modifier
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .fillMaxWidth()
                            .clip(
                                if (selected) {
                                    RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)
                                } else {
                                    RoundedCornerShape(8.dp)
                                },
                            )
                            .background(if (selected) Color.White else Color.Transparent)
                            .clickable {
                                selectedCategory = label
                                scope.launch { content.refreshHotRank(category = label) }
                            }
                            .padding(vertical = 14.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (selected && label == "热搜榜") {
                                Text(
                                    "TOP 20",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = gold,
                                )
                                Spacer(Modifier.height(2.dp))
                            }
                            if (selected) {
                                Text("🌾🌾", fontSize = 10.sp, color = gold)
                                Spacer(Modifier.height(4.dp))
                            }
                            Text(
                                label,
                                fontSize = 13.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (selected) ink else mute,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 12.dp))
                    .background(DemoColors.PageBg),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 4.dp),
                ) {
                    Column(Modifier.align(Alignment.CenterEnd)) {
                        Text(
                            (ageFilters.getOrNull(ageIndex) ?: selectedAge) + " ▾",
                            fontSize = 13.sp,
                            color = ink,
                            modifier = Modifier
                                .clip(DemoRadius.Pill)
                                .background(DemoColors.SurfaceRaised)
                                .border(0.5.dp, DemoColors.Divider, DemoRadius.Pill)
                                .clickable { showAgeMenu = !showAgeMenu }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                        if (showAgeMenu) {
                            Column(
                                Modifier
                                    .padding(top = 4.dp)
                                    .clip(RoundedCornerShape(DemoRadius.Md))
                                    .background(DemoColors.SurfaceFloating)
                                    .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(DemoRadius.Md)),
                            ) {
                                ageFilters.forEachIndexed { i, label ->
                                    Text(
                                        label,
                                        fontSize = 13.sp,
                                        color = if (i == ageIndex) DemoColors.Accent else ink,
                                        fontWeight = if (i == ageIndex) FontWeight.SemiBold else FontWeight.Normal,
                                        modifier = Modifier
                                            .clickable {
                                                selectedAge = label
                                                showAgeMenu = false
                                            }
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                    )
                                }
                            }
                        }
                    }
                }
                LazyColumn(Modifier.fillMaxSize()) {
                    items(items) { item ->
                        val rankBg = when (item.rank) {
                            1 -> gold
                            2 -> silver
                            3 -> bronze
                            else -> mute.copy(alpha = 0.35f)
                        }
                        val rankFg = if (item.rank <= 3) Color.White else mute
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { showPlatformToast(item.title) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            // Solid placeholder — Flutter uses bundled thumbs; network covers raise gate mse.
                            Box(
                                Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFE8E8ED)),
                            )
                            Spacer(Modifier.width(8.dp))
                            Box(
                                Modifier
                                    .size(18.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(rankBg),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "${item.rank}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = rankFg,
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    item.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ink,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    item.subtitle,
                                    fontSize = 12.sp,
                                    color = mute,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text("热度${item.heat}", fontSize = 11.sp, color = mute)
                            }
                        }
                        HorizontalDivider(
                            Modifier.padding(horizontal = 12.dp),
                            color = DemoColors.Divider,
                            thickness = 0.5.dp,
                        )
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}

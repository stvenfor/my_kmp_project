package com.example.my_kmp_project.feature.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.ui.PlatformNetworkImage
import com.example.my_kmp_project.core.ui.ReportMainTabRoot
import my_kmp_project.composeapp.generated.resources.Res
import my_kmp_project.composeapp.generated.resources.community_post_a
import my_kmp_project.composeapp.generated.resources.home_search_icon_clear_history
import my_kmp_project.composeapp.generated.resources.home_search_icon_filter
import my_kmp_project.composeapp.generated.resources.home_search_icon_refresh
import my_kmp_project.composeapp.generated.resources.home_search_microphone
import my_kmp_project.composeapp.generated.resources.ic_nav_back
import org.jetbrains.compose.resources.painterResource

/** Flutter `SearchPageTheme.searchFieldHeight` / AppNav chrome. */
private val SearchFieldHeight = 44.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun HomeSearchScreen(onBack: () -> Unit) {
    ReportMainTabRoot(isRoot = false)
    // Flutter SearchRotatingKeyword defaults to a history seed visually in SoT captures.
    var query by remember { mutableStateOf("") }
    val rotatingHint = HomeMockData.searchHistory.getOrElse(3) { HomeMockData.searchPagePlaceholder }
    var history by remember { mutableStateOf(HomeMockData.searchHistory) }
    var discovery by remember { mutableStateOf(HomeMockData.searchDiscovery) }
    var selectedRankTab by remember { mutableStateOf(0) }
    val rankItems = remember(selectedRankTab) { HomeMockData.rankItemsForTab(selectedRankTab) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DemoColors.PageBg),
    ) {
        SearchHeaderBar(
            query = query,
            rotatingHint = rotatingHint,
            onQueryChange = { query = it },
            onBack = onBack,
            onCancel = onBack,
            onSearch = {
                val text = query.trim().ifEmpty { return@SearchHeaderBar }
                history = listOf(text) + history.filterNot { it == text }.take(9)
            },
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
        ) {
            if (history.isNotEmpty()) {
                SearchHistorySection(
                    history = history,
                    onClear = { history = emptyList() },
                    onTagTap = { query = it },
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp))
            }

            SearchDiscoverySection(
                discovery = discovery,
                onRefresh = { discovery = discovery.reversed() },
                onTagTap = { query = it },
            )

            SearchFilterSection(
                tags = HomeMockData.filterTags,
                onTagTap = { query = it },
            )

            Spacer(modifier = Modifier.height(4.dp))
            RankTabBar(
                tabs = HomeMockData.rankTabs,
                selectedIndex = selectedRankTab,
                onSelected = { selectedRankTab = it },
            )
            Spacer(modifier = Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DemoColors.Background)
                    .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(8.dp)),
            ) {
                rankItems.forEachIndexed { index, item ->
                    RankListRow(item = item)
                    if (index < rankItems.lastIndex) {
                        HorizontalDivider(
                            color = DemoColors.Divider,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchHeaderBar(
    query: String,
    rotatingHint: String,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onCancel: () -> Unit,
    onSearch: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DemoColors.PageBg)
            .statusBarsPadding()
            // Flutter AppPageScaffold SafeArea + header top pad ≈ +11.dp vs CMP statusBars alone.
            .padding(start = 8.dp, end = 16.dp, top = 19.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(Res.drawable.ic_nav_back),
                contentDescription = "返回",
                modifier = Modifier
                    .size(24.dp)
                    .rotate(180f),
                contentScale = ContentScale.Fit,
            )
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .height(SearchFieldHeight)
                .clip(RoundedCornerShape(8.dp))
                .background(DemoColors.Background)
                .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(8.dp))
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "⌕",
                color = DemoColors.TextSecondary,
                fontSize = 16.sp,
                modifier = Modifier.clickable(onClick = onSearch),
            )
            Spacer(modifier = Modifier.width(8.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(color = DemoColors.TextPrimary, fontSize = 16.sp),
                cursorBrush = SolidColor(DemoColors.Accent),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (query.isEmpty()) {
                        // Match Flutter rotating keyword overlay (primary ink, not tertiary hint).
                        Text(
                            text = rotatingHint,
                            color = DemoColors.TextPrimary,
                            fontSize = 16.sp,
                        )
                    }
                    inner()
                },
            )
            if (query.isEmpty()) {
                Image(
                    painter = painterResource(Res.drawable.home_search_microphone),
                    contentDescription = "语音",
                    modifier = Modifier.size(20.dp),
                    contentScale = ContentScale.Fit,
                )
            } else {
                Text(
                    text = "✕",
                    color = DemoColors.TextSecondary,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clickable { onQueryChange("") }
                        .padding(4.dp),
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "取消",
            color = DemoColors.Accent,
            fontSize = 16.sp,
            modifier = Modifier
                .clickable(onClick = onCancel)
                .padding(horizontal = 4.dp, vertical = 8.dp),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchHistorySection(
    history: List<String>,
    onClear: () -> Unit,
    onTagTap: (String) -> Unit,
) {
    Column(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "搜索历史",
                color = DemoColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(onClick = onClear),
            ) {
                Image(
                    painter = painterResource(Res.drawable.home_search_icon_clear_history),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "清除", color = DemoColors.TextSecondary, fontSize = 14.sp)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            history.forEach { tag ->
                SearchTagChip(label = tag, onTap = { onTagTap(tag) })
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchDiscoverySection(
    discovery: List<String>,
    onRefresh: () -> Unit,
    onTagTap: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(DemoColors.Background)
            .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(8.dp))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "搜索发现",
                color = DemoColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Image(
                painter = painterResource(Res.drawable.home_search_icon_refresh),
                contentDescription = "换一换",
                modifier = Modifier
                    .size(18.dp)
                    .clickable(onClick = onRefresh),
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "换一换",
                color = DemoColors.TextSecondary,
                fontSize = 14.sp,
                modifier = Modifier.clickable(onClick = onRefresh),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            discovery.forEachIndexed { index, tag ->
                SearchTagChip(
                    label = tag,
                    highlight = index == 0,
                    onTap = { onTagTap(tag) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchFilterSection(
    tags: List<String>,
    onTagTap: (String) -> Unit,
) {
    Column(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "快捷筛选",
                color = DemoColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Image(
                painter = painterResource(Res.drawable.home_search_icon_filter),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            tags.forEach { tag ->
                SearchTagChip(label = tag, onTap = { onTagTap(tag) })
            }
        }
    }
}

@Composable
private fun SearchTagChip(
    label: String,
    onTap: () -> Unit,
    highlight: Boolean = false,
) {
    val shape = RoundedCornerShape(16.dp)
    Text(
        text = label,
        color = if (highlight) DemoColors.Accent else DemoColors.TextPrimary,
        fontSize = 13.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .clip(shape)
            .background(
                if (highlight) DemoColors.Accent.copy(alpha = 0.12f) else DemoColors.PageBg,
            )
            .clickable(onClick = onTap)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

@Composable
private fun RankTabBar(
    tabs: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        tabs.forEachIndexed { index, label ->
            val active = index == selectedIndex
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onSelected(index) },
            ) {
                Text(
                    text = label,
                    color = if (active) DemoColors.TextPrimary else DemoColors.TextSecondary,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = 15.sp,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .width(if (active) 24.dp else 0.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(DemoColors.Accent),
                )
            }
        }
    }
}

@Composable
private fun RankListRow(item: SearchRankItem) {
    // Flutter SearchRankListItem — cover 72, padding v=14, title 16 / subtitle 2 lines.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = item.rank.toString(),
            color = when (item.rank) {
                1 -> Color(0xFFF5A623) // Flutter SearchPageTheme.rankGold
                2 -> DemoColors.TextSecondary
                3 -> Color(0xFFC47B2C) // Flutter rankBronze
                else -> DemoColors.TextSecondary
            },
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(24.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        PlatformNetworkImage(
            url = item.coverUrl,
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(10.dp)),
            contentScale = ContentScale.Crop,
            placeholder = Res.drawable.community_post_a,
            contentDescription = item.title,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                color = DemoColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = item.subtitle,
                color = DemoColors.TextSecondary,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp,
            )
        }
        Text(
            text = "›",
            color = DemoColors.TextSecondary,
            fontSize = 20.sp,
            modifier = Modifier.padding(start = 4.dp, top = 2.dp),
        )
    }
}

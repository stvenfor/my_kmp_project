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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.feature.shell.ReportMainTabRoot

@Composable
internal fun HomeSearchScreen(onBack: () -> Unit) {
    ReportMainTabRoot(isRoot = false)
    var query by remember { mutableStateOf("") }
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
            onQueryChange = { query = it },
            onBack = onBack,
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
                SectionHeader(
                    title = "搜索历史",
                    action = "清除",
                    onAction = { history = emptyList() },
                )
                TagFlow(
                    tags = history,
                    onTagTap = { query = it },
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp))
            }

            SectionHeader(
                title = "搜索发现",
                action = "换一批",
                onAction = { discovery = discovery.reversed() },
            )
            TagFlow(
                tags = discovery,
                onTagTap = { query = it },
            )

            SectionHeader(title = "筛选标签")
            TagFlow(
                tags = HomeMockData.filterTags,
                onTagTap = { query = it },
                outlined = true,
            )

            Spacer(modifier = Modifier.height(8.dp))
            RankTabBar(
                tabs = HomeMockData.rankTabs,
                selectedIndex = selectedRankTab,
                onSelected = { selectedRankTab = it },
            )
            Spacer(modifier = Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DemoColors.Background)
                    .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(12.dp)),
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
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onSearch: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DemoColors.Toolbar)
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "返回",
            color = DemoColors.Accent,
            fontSize = 15.sp,
            modifier = Modifier
                .clickable(onClick = onBack)
                .padding(end = 8.dp, top = 8.dp, bottom = 8.dp),
        )
        Row(
            modifier = Modifier
                .weight(1f)
                .height(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(DemoColors.PageBg)
                .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "⌕", color = DemoColors.TextSecondary, fontSize = 16.sp)
            Spacer(modifier = Modifier.width(8.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(color = DemoColors.TextPrimary, fontSize = 15.sp),
                cursorBrush = SolidColor(DemoColors.Accent),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (query.isEmpty()) {
                        Text(
                            text = HomeMockData.searchPlaceholder,
                            color = DemoColors.TextSecondary,
                            fontSize = 15.sp,
                        )
                    }
                    inner()
                },
            )
        }
        Text(
            text = "搜索",
            color = DemoColors.Accent,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            modifier = Modifier
                .clickable(onClick = onSearch)
                .padding(start = 12.dp, top = 8.dp, bottom = 8.dp),
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = DemoColors.TextPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f),
        )
        if (action != null && onAction != null) {
            Text(
                text = action,
                color = DemoColors.Accent,
                fontSize = 13.sp,
                modifier = Modifier.clickable(onClick = onAction),
            )
        }
    }
}

@Composable
private fun TagFlow(
    tags: List<String>,
    onTagTap: (String) -> Unit,
    outlined: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tags.forEach { tag ->
            val shape = RoundedCornerShape(16.dp)
            Text(
                text = tag,
                color = if (outlined) DemoColors.Accent else DemoColors.TextPrimary,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .clip(shape)
                    .then(
                        if (outlined) {
                            Modifier.border(1.dp, DemoColors.Accent.copy(alpha = 0.4f), shape)
                        } else {
                            Modifier.background(DemoColors.Background)
                        },
                    )
                    .clickable { onTagTap(tag) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = item.rank.toString(),
            color = when (item.rank) {
                1 -> DemoColors.Danger
                2 -> DemoColors.Accent
                3 -> DemoColors.TextSecondary
                else -> DemoColors.TextSecondary
            },
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.width(28.dp),
        )
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(DemoColors.Accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = item.title.take(1),
                color = DemoColors.Accent,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                color = DemoColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.subtitle,
                color = DemoColors.TextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

package com.example.my_kmp_project.feature.home

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.MineTopBar
import com.example.my_kmp_project.feature.shell.ReportMainTabRoot

@Composable
internal fun AllServicesScreen(onBack: () -> Unit) {
    ReportMainTabRoot(isRoot = false)
    var isEditing by remember { mutableStateOf(false) }
    var favoriteIds by remember {
        mutableStateOf(HomeMockData.favoriteServices.map { it.id }.toSet())
    }
    val favoriteItems = remember(favoriteIds) {
        HomeMockData.favoriteServices.filter { it.id in favoriteIds } +
            HomeMockData.catalogSections
                .flatMap { it.items }
                .filter { it.id in favoriteIds && it.id !in HomeMockData.favoriteServices.map { f -> f.id } }
                .distinctBy { it.id }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DemoColors.PageBg),
    ) {
        MineTopBar(title = "全部服务", onBack = onBack, containerColor = DemoColors.PageBg)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                top = 8.dp,
                bottom = 24.dp,
            ),
        ) {
            item {
                AllServicesSectionBlock(
                    section = AllServiceSection(
                        title = "常用服务",
                        subtitle = "将按自定义顺序出现在首页",
                        showEditButton = true,
                        items = favoriteItems.ifEmpty { HomeMockData.favoriteServices.take(3) },
                    ),
                    isEditing = isEditing,
                    isFavoriteSection = true,
                    onEditTap = { isEditing = !isEditing },
                    onItemBadgeTap = { item ->
                        if (isEditing && favoriteIds.size > 3) {
                            favoriteIds = favoriteIds - item.id
                        }
                    },
                )
            }
            items(HomeMockData.catalogSections, key = { it.title }) { section ->
                AllServicesSectionBlock(
                    section = section,
                    isEditing = isEditing,
                    isFavoriteSection = false,
                    favoriteIds = favoriteIds,
                    onItemBadgeTap = { item ->
                        if (isEditing && favoriteIds.size < 8) {
                            favoriteIds = favoriteIds + item.id
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun AllServicesSectionBlock(
    section: AllServiceSection,
    isEditing: Boolean,
    isFavoriteSection: Boolean,
    favoriteIds: Set<String> = emptySet(),
    onEditTap: (() -> Unit)? = null,
    onItemBadgeTap: (AllServiceItem) -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = section.title,
                color = DemoColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
            )
            if (section.subtitle != null) {
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = section.subtitle,
                    color = DemoColors.TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
            if (section.showEditButton && onEditTap != null) {
                Text(
                    text = if (isEditing) "完成" else "编辑",
                    color = DemoColors.Accent,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .border(1.dp, DemoColors.Accent, RoundedCornerShape(14.dp))
                        .clickable(onClick = onEditTap)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        val rows = section.items.chunked(5)
        rows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                rowItems.forEach { item ->
                    ServiceGridCell(
                        item = item,
                        isEditing = isEditing,
                        showMinus = isFavoriteSection && isEditing,
                        showPlus = !isFavoriteSection && isEditing && item.id !in favoriteIds,
                        onBadgeTap = { onItemBadgeTap(item) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(5 - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ServiceGridCell(
    item: AllServiceItem,
    isEditing: Boolean,
    showMinus: Boolean,
    showPlus: Boolean,
    onBadgeTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                val icon = HomeServiceAssets.fromFlutterFile(item.assetName)
                if (icon != null) {
                    HomeAssetIcon(resource = icon, size = 44.dp, contentDescription = item.label)
                } else {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DemoColors.Accent.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = item.label.take(1),
                            color = DemoColors.Accent,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                        )
                    }
                }
            }
            if (isEditing && (showMinus || showPlus)) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(if (showMinus) DemoColors.Danger else DemoColors.Accent)
                        .clickable(onClick = onBadgeTap),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (showMinus) "−" else "+",
                        color = DemoColors.OnPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = item.label,
            color = DemoColors.TextPrimary,
            fontSize = 11.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

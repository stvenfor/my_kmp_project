package com.example.my_kmp_project.feature.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.ui.ReportMainTabRoot
import my_kmp_project.composeapp.generated.resources.Res
import my_kmp_project.composeapp.generated.resources.ic_nav_back
import org.jetbrains.compose.resources.painterResource

/** Flutter `SliverGridDelegateWithFixedCrossAxisCount` childAspectRatio. */
private const val ServiceCellAspectRatio = 0.72f

/** Flutter `kToolbarHeight` / `AppSafeInsets.toolbarHeight`. */
private val FlutterToolbarHeight = 56.dp

@Composable
internal fun AllServicesScreen(
    onBack: () -> Unit,
    onOpen: (String) -> Unit = {},
) {
    ReportMainTabRoot(isRoot = false)
    var isEditing by remember { mutableStateOf(false) }
    var favoriteIds by remember { mutableStateOf(HomeFavoritesStore.snapshot()) }
    val favoriteItems = remember(favoriteIds) { HomeFavoritesStore.favoriteItems() }

    // Flutter AllServicesTheme.background = canvas (white), not canvasSoft2.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DemoColors.Background),
    ) {
        AllServicesTopBar(onBack = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
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
                        items = favoriteItems,
                    ),
                    isEditing = isEditing,
                    isFavoriteSection = true,
                    onEditTap = { isEditing = !isEditing },
                    onItemBadgeTap = { item ->
                        if (isEditing && HomeFavoritesStore.remove(item.id)) {
                            favoriteIds = HomeFavoritesStore.snapshot()
                        }
                    },
                    onItemTap = { if (!isEditing) onOpen(it.label) },
                )
            }
            items(HomeMockData.catalogSections, key = { it.title }) { section ->
                AllServicesSectionBlock(
                    section = section,
                    isEditing = isEditing,
                    isFavoriteSection = false,
                    favoriteIds = favoriteIds,
                    onItemBadgeTap = { item ->
                        if (isEditing && HomeFavoritesStore.add(item.id)) {
                            favoriteIds = HomeFavoritesStore.snapshot()
                        }
                    },
                    onItemTap = { if (!isEditing) onOpen(it.label) },
                )
            }
        }
    }
}

/** Flutter `AppNavBar` — 56.dp toolbar (not M3 64.dp CenterAlignedTopAppBar). */
@Composable
private fun AllServicesTopBar(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DemoColors.Background)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(FlutterToolbarHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Image(
                    painter = painterResource(Res.drawable.ic_nav_back),
                    contentDescription = "返回",
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(180f),
                    contentScale = ContentScale.Fit,
                )
            }
            Text(
                text = "全部服务",
                fontWeight = FontWeight.Bold,
                color = DemoColors.TextPrimary,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.size(48.dp))
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
    onItemTap: (AllServiceItem) -> Unit = {},
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
                    color = DemoColors.Muted,
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
        // Match Flutter GridView: 5 cols, mainAxisSpacing 12, childAspectRatio 0.72.
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val cellHeight = maxWidth / 5f / ServiceCellAspectRatio
            val rows = section.items.chunked(5)
            Column(modifier = Modifier.fillMaxWidth()) {
                rows.forEachIndexed { index, rowItems ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(cellHeight),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        rowItems.forEach { item ->
                            ServiceGridCell(
                                item = item,
                                isEditing = isEditing,
                                showMinus = isFavoriteSection && isEditing,
                                showPlus = !isFavoriteSection && isEditing && item.id !in favoriteIds,
                                onBadgeTap = { onItemBadgeTap(item) },
                                onTap = { onItemTap(item) },
                                cellModifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                            )
                        }
                        repeat(5 - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f).fillMaxHeight())
                        }
                    }
                    if (index < rows.lastIndex) {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
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
    onTap: () -> Unit = {},
    cellModifier: Modifier = Modifier,
) {
    Column(
        modifier = cellModifier
            .clickable(enabled = !isEditing, onClick = onTap)
            .padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Flutter `_ServiceGridCell`: 48.w asset, label 11.sp height 1.2, maxLines 1.
        // Prefer gate captures on a 375dp-wide viewport so ScreenUtil `.w` == Compose `dp`.
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center,
        ) {
            val icon = HomeServiceAssets.fromFlutterFile(item.assetName)
            if (icon != null) {
                HomeAssetIcon(
                    resource = icon,
                    size = 48.dp,
                    contentDescription = item.label,
                    contentScale = ContentScale.Fit,
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
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
            if (isEditing && (showMinus || showPlus)) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(if (showMinus) DemoColors.OnPrimary else DemoColors.Accent)
                        .border(
                            width = 1.dp,
                            color = if (showMinus) DemoColors.Divider else DemoColors.Accent,
                            shape = CircleShape,
                        )
                        .clickable(onClick = onBadgeTap),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (showMinus) "×" else "+",
                        color = if (showMinus) DemoColors.TextSecondary else DemoColors.OnPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = item.label,
            color = DemoColors.TextSecondary,
            fontSize = 11.sp,
            lineHeight = (11 * 1.2f).sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

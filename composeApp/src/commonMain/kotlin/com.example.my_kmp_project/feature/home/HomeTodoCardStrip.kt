package com.example.my_kmp_project.feature.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors

/**
 * Flutter `HomeTodoCardStrip` — size-aware Large / Medium / Small cards + pager.
 * Hidden when [HomeTodoStore.todoCards] is empty.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun HomeTodoCardStrip(onOpen: (HomeTodoCard) -> Unit) {
    HomeTodoStore.version
    val cards = HomeTodoStore.todoCards()
    if (cards.isEmpty()) return

    if (HomeTodoPacker.shouldWrapOnly(cards)) {
        Row(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, top = 12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            cards.forEach { card ->
                Box(modifier = Modifier.weight(1f)) {
                    HomeTodoCardView(card = card, onTap = { onOpen(card) })
                }
            }
            repeat(2 - cards.size) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
        return
    }

    val pages = HomeTodoPacker.packPages(cards)
    val pagerState = rememberPagerState(pageCount = { pages.size })
    Column(
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, top = 12.dp)
            .fillMaxWidth(),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            pageSpacing = 0.dp,
        ) { index ->
            TodoPageGrid(page = pages[index], onOpen = onOpen)
        }
        if (pages.size > 1) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                pages.indices.forEach { i ->
                    if (i > 0) Spacer(modifier = Modifier.width(6.dp))
                    val active = i == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .height(6.dp)
                            .width(if (active) 14.dp else 6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (active) DemoColors.Primary else DemoColors.Divider),
                    )
                }
            }
        }
    }
}

@Composable
private fun TodoPageGrid(
    page: List<HomeTodoCard>,
    onOpen: (HomeTodoCard) -> Unit,
) {
    val rows = buildTodoRows(page)
    val hasLarge = page.any { it.size == HomeTodoSize.Large }
    // Flutter: non-large pages always reserve 2 row slots so card height stays stable.
    val rowCount = if (hasLarge) 1 else 2
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        for (r in 0 until rowCount) {
            if (r < rows.size) {
                val row = rows[r]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    row.forEach { card ->
                        val flex = if (card.size == HomeTodoSize.Small) 1f else 2f
                        Box(
                            modifier = Modifier
                                .weight(flex)
                                .fillMaxHeight(),
                        ) {
                            HomeTodoCardView(
                                card = card,
                                onTap = { onOpen(card) },
                                expandFill = true,
                            )
                        }
                    }
                    if (row.size == 1 && row.first().size == HomeTodoSize.Small) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

private fun buildTodoRows(page: List<HomeTodoCard>): List<List<HomeTodoCard>> {
    val rows = mutableListOf<List<HomeTodoCard>>()
    var i = 0
    while (i < page.size) {
        val card = page[i]
        if (card.size == HomeTodoSize.Large || card.size == HomeTodoSize.Medium) {
            rows += listOf(card)
            i++
            continue
        }
        val row = mutableListOf(card)
        i++
        if (i < page.size && page[i].size == HomeTodoSize.Small) {
            row += page[i]
            i++
        }
        rows += row
    }
    return rows
}

@Composable
private fun HomeTodoCardView(
    card: HomeTodoCard,
    onTap: () -> Unit,
    expandFill: Boolean = false,
) {
    when (card.size) {
        HomeTodoSize.Large -> LargeTodoCard(card, onTap)
        HomeTodoSize.Medium -> MediumTodoCard(card, onTap)
        HomeTodoSize.Small -> SmallTodoCard(card, onTap, expandFill)
    }
}

@Composable
private fun TodoCardShell(
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DemoColors.Background)
            .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(10.dp))
            .clickable(onClick = onTap)
            .padding(14.dp),
        content = content,
    )
}

@Composable
private fun TodoThumb(size: Dp, label: String) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(DemoColors.PageBg),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label.take(1),
            color = DemoColors.Accent,
            fontWeight = FontWeight.SemiBold,
            fontSize = (size.value * 0.4f).sp,
        )
    }
}

@Composable
private fun TodoActionChip(label: String, large: Boolean = false) {
    Text(
        text = label,
        fontSize = if (large) 13.sp else 12.sp,
        fontWeight = FontWeight.Medium,
        color = DemoColors.Accent,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DemoColors.Accent.copy(alpha = 0.1f))
            .padding(horizontal = if (large) 12.dp else 10.dp, vertical = if (large) 6.dp else 4.dp),
    )
}

@Composable
private fun SmallTodoCard(
    card: HomeTodoCard,
    onTap: () -> Unit,
    expandFill: Boolean,
) {
    TodoCardShell(
        onTap = onTap,
        modifier = if (expandFill) Modifier.fillMaxHeight() else Modifier,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TodoThumb(size = 40.dp, label = card.title)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    card.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = DemoColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    card.subtitle,
                    fontSize = 11.sp,
                    color = DemoColors.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (expandFill) Spacer(modifier = Modifier.weight(1f))
        else Spacer(modifier = Modifier.height(10.dp))
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            TodoActionChip(card.actionLabel)
        }
    }
}

@Composable
private fun MediumTodoCard(card: HomeTodoCard, onTap: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .clip(RoundedCornerShape(10.dp))
            .background(DemoColors.Background)
            .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(10.dp))
            .clickable(onClick = onTap)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TodoThumb(size = 48.dp, label = card.title)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                card.title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = DemoColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                card.subtitle,
                fontSize = 12.sp,
                color = DemoColors.Muted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        TodoActionChip(card.actionLabel)
    }
}

@Composable
private fun LargeTodoCard(card: HomeTodoCard, onTap: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(10.dp))
            .background(DemoColors.Background)
            .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(10.dp))
            .clickable(onClick = onTap)
            .padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            TodoThumb(size = 64.dp, label = card.title)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                if (card.count > 0) {
                    Text(
                        text = "${card.count}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        color = DemoColors.Primary,
                        letterSpacing = (-0.8).sp,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
                Text(
                    card.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                    color = DemoColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    card.subtitle,
                    fontSize = 13.sp,
                    color = DemoColors.TextSecondary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (card.count > 0) {
                Text(
                    text = "待处理 ${card.count}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = DemoColors.TextSecondary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(DemoColors.PageBg)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            TodoActionChip(card.actionLabel, large = true)
        }
    }
}

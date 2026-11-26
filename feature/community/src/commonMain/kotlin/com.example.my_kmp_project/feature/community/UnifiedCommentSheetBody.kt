package com.example.my_kmp_project.feature.community

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.feature.community.http.UnifiedComment
import com.example.my_kmp_project.feature.community.http.UnifiedCommentModerationAction
import com.example.my_kmp_project.feature.community.http.UnifiedCommentSort
import com.example.my_kmp_project.feature.community.http.UnifiedCommentTargetType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
internal fun UnifiedCommentBody(
    targetType: UnifiedCommentTargetType,
    targetId: String,
    onClose: () -> Unit,
    onSignIn: ((draft: String, reply: UnifiedCommentController.ReplyContext?) -> Unit)?,
    initialDraft: String,
    initialReply: UnifiedCommentController.ReplyContext?,
    newClientMsgId: () -> String,
) {
    val controller = remember(targetType, targetId) {
        UnifiedCommentController(
            repository = UnifiedCommentController.defaultRepository(),
            targetType = targetType,
            targetId = targetId,
            newClientMsgId = newClientMsgId,
        )
    }
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    // 对话框必须绑定目标：切目标后旧评论的操作不能泄漏到新面板。
    var deleteTarget by remember(targetType, targetId) { mutableStateOf<UnifiedComment?>(null) }
    var hideTarget by remember(targetType, targetId) { mutableStateOf<UnifiedComment?>(null) }

    LaunchedEffect(controller) {
        // 登录返回恢复草稿 / 回复上下文后再刷新，让服务端复核回复目标是否仍可回复。
        if (initialDraft.isNotEmpty()) controller.updateDraft(initialDraft)
        if (initialReply != null) controller.restoreReply(initialReply)
        controller.refresh()
    }
    LaunchedEffect(controller.replyTo?.commentId) {
        if (controller.replyTo != null) {
            runCatching { focusRequester.requestFocus() }
        }
    }

    Column(Modifier.fillMaxSize().background(DemoColors.Background)) {
        SheetHeader(totalVisible = controller.totalVisible, onClose = onClose)
        if (!controller.notEnabled) {
            SortRow(controller = controller, scope = scope)
        }
        Banners(
            controller = controller,
            scope = scope,
            onSignIn = onSignIn?.let { signIn ->
                { signIn(controller.draft, controller.replyTo) }
            },
        )

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                controller.notEnabled -> UnavailableState(onClose = onClose)
                controller.firstLoading -> LoadingState()
                controller.refreshError != null &&
                    controller.pinned == null &&
                    controller.roots.isEmpty() -> ErrorState(
                    message = controller.refreshError?.message ?: "加载失败，请稍后重试",
                    onRetry = { scope.launch { controller.refresh() } },
                )
                controller.isEmpty -> EmptyState()
                else -> CommentList(
                    controller = controller,
                    scope = scope,
                    onRequestDelete = { deleteTarget = it },
                    onRequestHide = { hideTarget = it },
                )
            }
        }

        if (!controller.notEnabled) {
            Composer(
                controller = controller,
                scope = scope,
                focusRequester = focusRequester,
                onSignIn = onSignIn?.let { signIn ->
                    { signIn(controller.draft, controller.replyTo) }
                },
            )
        }
    }

    deleteTarget?.let { comment ->
        DeleteCommentDialog(
            onConfirm = {
                deleteTarget = null
                scope.launch { controller.deleteComment(comment) }
            },
            onDismiss = { deleteTarget = null },
        )
    }
    hideTarget?.let { comment ->
        HideCommentDialog(
            onConfirm = { reason ->
                hideTarget = null
                scope.launch {
                    controller.moderate(comment, UnifiedCommentModerationAction.Hide, reason)
                }
            },
            onDismiss = { hideTarget = null },
        )
    }
}

@Composable
private fun SheetHeader(totalVisible: Long, onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "评论",
            color = DemoColors.TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        if (totalVisible > 0L) {
            Spacer(Modifier.size(6.dp))
            Text(
                text = totalVisible.toString(),
                color = DemoColors.Muted,
                fontSize = 14.sp,
            )
        }
        Spacer(Modifier.weight(1f))
        TextButton(
            onClick = onClose,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text("关闭", color = DemoColors.TextSecondary, fontSize = 14.sp)
        }
    }
    HorizontalDivider(color = DemoColors.Divider)
}

@Composable
private fun SortRow(controller: UnifiedCommentController, scope: CoroutineScope) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SortTab(
            label = "最新",
            selected = controller.sort == UnifiedCommentSort.Latest,
            onClick = { scope.launch { controller.changeSort(UnifiedCommentSort.Latest) } },
        )
        Spacer(Modifier.size(4.dp))
        SortTab(
            label = "最热",
            selected = controller.sort == UnifiedCommentSort.Hot,
            onClick = { scope.launch { controller.changeSort(UnifiedCommentSort.Hot) } },
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = "共 ${controller.totalVisible} 条",
            color = DemoColors.Muted,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun SortTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) DemoColors.SelectionBg else Color.Transparent)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) DemoColors.SelectionFg else DemoColors.TextSecondary,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun Banners(
    controller: UnifiedCommentController,
    scope: CoroutineScope,
    onSignIn: (() -> Unit)?,
) {
    val hasContent = controller.pinned != null || controller.roots.isNotEmpty()
    controller.refreshError?.let { error ->
        if (hasContent) {
            InlineBanner(
                text = "刷新失败：${error.message ?: "请稍后重试"}",
                actionLabel = "重试",
                onAction = { scope.launch { controller.refresh() } },
            )
        }
    }
    controller.actionError?.let { message ->
        InlineBanner(
            text = message,
            actionLabel = "知道了",
            onAction = { controller.clearActionError() },
        )
    }
    if (controller.authRequired) {
        InlineBanner(
            text = "登录后即可参与评论",
            actionLabel = if (onSignIn != null) "去登录" else null,
            onAction = onSignIn,
        )
    }
}

@Composable
private fun InlineBanner(text: String, actionLabel: String?, onAction: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DemoColors.ErrorSoft)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            color = DemoColors.ErrorDeep,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f),
        )
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(actionLabel, color = DemoColors.ErrorDeep, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun CommentList(
    controller: UnifiedCommentController,
    scope: CoroutineScope,
    onRequestDelete: (UnifiedComment) -> Unit,
    onRequestHide: (UnifiedComment) -> Unit,
) {
    val listState = rememberLazyListState()
    val nearEnd by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            info.totalItemsCount > 0 && last >= info.totalItemsCount - 3
        }
    }
    // 只自动补页一次：有错 / 正在翻页时不触发，避免错误页无限重试。
    LaunchedEffect(
        nearEnd,
        controller.hasMore,
        controller.loadingMore,
        controller.loadMoreError,
        controller.refreshing,
        controller.roots.size,
    ) {
        if (nearEnd &&
            controller.hasMore &&
            !controller.loadingMore &&
            controller.loadMoreError == null &&
            !controller.refreshing
        ) {
            controller.loadMore()
        }
    }
    PullToRefreshBox(
        isRefreshing = controller.refreshing,
        onRefresh = { scope.launch { controller.refresh() } },
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 8.dp),
        ) {
            controller.pinned?.let { pinned ->
                item(key = "pinned-${pinned.id}") {
                    Column {
                        Text(
                            text = "置顶",
                            color = DemoColors.Accent,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                        )
                        CommentBlock(
                            comment = pinned,
                            isPinned = true,
                            controller = controller,
                            scope = scope,
                            onRequestDelete = onRequestDelete,
                            onRequestHide = onRequestHide,
                        )
                        HorizontalDivider(color = DemoColors.Divider)
                    }
                }
            }
            items(controller.roots, key = { it.id }) { root ->
                Column {
                    CommentBlock(
                        comment = root,
                        isPinned = false,
                        controller = controller,
                        scope = scope,
                        onRequestDelete = onRequestDelete,
                        onRequestHide = onRequestHide,
                    )
                    HorizontalDivider(color = DemoColors.Divider)
                }
            }
            item(key = "load-more") {
                LoadMoreFooter(controller = controller, scope = scope)
            }
        }
    }
}

@Composable
private fun LoadMoreFooter(controller: UnifiedCommentController, scope: CoroutineScope) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        when {
            controller.loadingMore -> Text("加载中…", color = DemoColors.Muted, fontSize = 13.sp)
            controller.loadMoreError != null -> TextButton(
                onClick = { scope.launch { controller.loadMore() } },
            ) {
                Text("加载失败，点击重试", color = DemoColors.Danger, fontSize = 13.sp)
            }
            controller.hasMore -> TextButton(onClick = { scope.launch { controller.loadMore() } }) {
                Text("加载更多评论", color = DemoColors.Accent, fontSize = 13.sp)
            }
            else -> Text("没有更多评论了", color = DemoColors.Muted, fontSize = 12.sp)
        }
    }
}

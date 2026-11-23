package com.example.my_kmp_project.feature.community

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.ui.PlatformNetworkImage
import com.example.my_kmp_project.feature.community.http.UnifiedComment
import com.example.my_kmp_project.feature.community.http.UnifiedCommentContentState
import com.example.my_kmp_project.feature.community.http.UnifiedCommentModerationAction
import com.example.my_kmp_project.feature.community.http.UnifiedCommentSort
import com.example.my_kmp_project.feature.community.http.UnifiedCommentTargetType
import com.example.my_kmp_project.feature.community.http.communityNowMs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import my_kmp_project.core.design.generated.resources.Res
import my_kmp_project.core.design.generated.resources.community_avatar

/** 底部评论面板的最大高度占比（覆盖层浮在内容之上，不重建底层页面）。 */
private const val SHEET_MAX_HEIGHT_FRACTION = 0.9f

private const val REPLY_ANIM_MS = 160
private const val ACTION_ANIM_MS = 140
private val INDENT_STEP = 16.dp

/**
 * 统一评论底部面板（Material3 ModalBottomSheet，白色圆角）。
 *
 * - 浮层由装配层（`NativeAndroidMain`）在底层页面之上渲染；关闭 / 返回只收起面板，
 *   底层 feed / 播放器 / 导航与底栏状态原样保留。
 * - 窗口 inset 由面板自己处理：`contentWindowInsets = 0` + `navigationBars ∪ ime`，
 *   避免与宿主重复加导航栏内边距，键盘弹出时输入条上移。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun UnifiedCommentSheet(
    target: UnifiedCommentTarget,
    onDismiss: () -> Unit,
    onSignIn: ((draft: String, reply: UnifiedCommentController.ReplyContext?) -> Unit)? = null,
    initialDraft: String = "",
    initialReply: UnifiedCommentController.ReplyContext? = null,
    newClientMsgId: () -> String = { com.example.my_kmp_project.core.uuid.AppUuid.random() },
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DemoColors.Background,
        contentColor = DemoColors.TextPrimary,
        dragHandle = { BottomSheetDefaults.DragHandle(color = DemoColors.Divider) },
        contentWindowInsets = { WindowInsets(0) },
    ) {
        val safeInsets = WindowInsets.navigationBars.union(WindowInsets.ime)
        when (target) {
            UnifiedCommentTarget.Invalid -> Box(
                Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(safeInsets),
            ) {
                InvalidTargetContent(onClose = onDismiss)
            }
            is UnifiedCommentTarget.Valid -> Box(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(SHEET_MAX_HEIGHT_FRACTION)
                    .windowInsetsPadding(safeInsets),
            ) {
                UnifiedCommentBody(
                    targetType = target.type,
                    targetId = target.id,
                    onClose = onDismiss,
                    onSignIn = onSignIn,
                    initialDraft = initialDraft,
                    initialReply = initialReply,
                    newClientMsgId = newClientMsgId,
                )
            }
        }
    }
}

@Composable
private fun UnifiedCommentBody(
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
private fun CommentBlock(
    comment: UnifiedComment,
    isPinned: Boolean,
    controller: UnifiedCommentController,
    scope: CoroutineScope,
    onRequestDelete: (UnifiedComment) -> Unit,
    onRequestHide: (UnifiedComment) -> Unit,
    isReply: Boolean = false,
) {
    val thread = controller.replyThreads[comment.id]
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 16.dp + (if (isReply) INDENT_STEP else 0.dp),
                end = 16.dp,
                top = if (isReply) 8.dp else 10.dp,
                bottom = if (isReply) 8.dp else 10.dp,
            ),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Avatar(url = comment.author?.avatar, nickname = comment.author?.nickname)
            Spacer(Modifier.size(10.dp))
            Column(Modifier.weight(1f)) {
                AuthorLine(comment = comment, isPinned = isPinned)
                ContentOrTombstone(comment = comment)
                Spacer(Modifier.height(2.dp))
                ActionRow(
                    comment = comment,
                    threadExpanded = thread?.expanded == true,
                    controller = controller,
                    scope = scope,
                    onRequestDelete = onRequestDelete,
                    onRequestHide = onRequestHide,
                )
                if (!isReply && comment.replyCount > 0L) {
                    Spacer(Modifier.height(2.dp))
                    ReplySection(
                        rootId = comment.id,
                        thread = thread,
                        controller = controller,
                        scope = scope,
                        onRequestDelete = onRequestDelete,
                        onRequestHide = onRequestHide,
                    )
                }
            }
        }
    }
}

@Composable
private fun Avatar(url: String?, nickname: String?) {
    PlatformNetworkImage(
        url = url,
        modifier = Modifier.size(34.dp).clip(CircleShape),
        contentScale = ContentScale.Crop,
        placeholder = Res.drawable.community_avatar,
        contentDescription = nickname?.takeIf { it.isNotBlank() }?.let { "$it 的头像" },
    )
}

@Composable
private fun AuthorLine(comment: UnifiedComment, isPinned: Boolean) {
    val nickname = comment.author?.nickname.orEmpty()
    val time = formatCommentRelativeTime(comment.createdAtMillis(), communityNowMs())
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (nickname.isNotBlank()) {
            Text(
                text = nickname,
                color = DemoColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
        if (comment.author?.isAuthor == true) {
            Spacer(Modifier.size(6.dp))
            Badge(text = "作者", background = DemoColors.Accent, foreground = Color.White)
        }
        if (isPinned || comment.isPinned) {
            Spacer(Modifier.size(6.dp))
            Badge(
                text = "置顶",
                background = DemoColors.LinkBgSoft,
                foreground = DemoColors.LinkDeep,
            )
        }
        Spacer(Modifier.weight(1f))
        if (time != null) {
            Spacer(Modifier.size(6.dp))
            Text(
                text = time,
                color = DemoColors.Muted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun Badge(text: String, background: Color, foreground: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(background)
            .padding(horizontal = 5.dp, vertical = 1.dp),
    ) {
        Text(text = text, color = foreground, fontSize = 10.sp)
    }
}

@Composable
private fun ContentOrTombstone(comment: UnifiedComment) {
    when (comment.contentState) {
        UnifiedCommentContentState.Visible -> {
            val replyTarget = comment.replyToAuthor?.nickname?.takeIf { it.isNotBlank() }
                ?: comment.legacyReplyToNickname
            if (replyTarget != null) {
                Text(
                    text = "回复 @$replyTarget",
                    color = DemoColors.Accent,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            if (comment.content.isNotBlank()) {
                Text(
                    text = comment.content,
                    color = DemoColors.TextPrimary,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        UnifiedCommentContentState.Hidden -> TombstoneText("该评论已被隐藏")
        UnifiedCommentContentState.Deleted -> TombstoneText("该评论已删除")
        UnifiedCommentContentState.Unknown -> TombstoneText("评论内容暂不可用")
    }
}

@Composable
private fun TombstoneText(text: String) {
    Text(
        text = text,
        color = DemoColors.Muted,
        fontSize = 14.sp,
        modifier = Modifier.padding(top = 2.dp),
    )
}

@Composable
private fun ActionRow(
    comment: UnifiedComment,
    threadExpanded: Boolean,
    controller: UnifiedCommentController,
    scope: CoroutineScope,
    onRequestDelete: (UnifiedComment) -> Unit,
    onRequestHide: (UnifiedComment) -> Unit,
) {
    // 窄屏（~320dp 面板）放不下「赞 + 回复 + 展开 N 条 + 更多」：展开单独一行。
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (comment.canLike) {
                ActionText(
                    label = likeLabel(comment),
                    enabled = comment.id !in controller.actionBusyIds,
                    onClick = { scope.launch { controller.toggleLike(comment) } },
                )
            } else if (comment.likeCount > 0L) {
                ActionText(label = "${comment.likeCount} 赞", enabled = false, onClick = {})
            }
            if (comment.canReply) {
                ActionText(label = "回复", onClick = { controller.startReply(comment) })
            }
            Spacer(Modifier.weight(1f))
            if (hasOverflowActions(comment)) {
                CommentOverflowMenu(
                    comment = comment,
                    controller = controller,
                    scope = scope,
                    onRequestDelete = onRequestDelete,
                    onRequestHide = onRequestHide,
                )
            }
        }
        if (comment.replyCount > 0L) {
            ActionText(
                label = if (threadExpanded) "收起回复" else "展开 ${comment.replyCount} 条回复",
                onClick = { scope.launch { controller.toggleReplies(comment.id) } },
            )
        }
    }
}

@Composable
private fun ActionText(
    label: String,
    enabled: Boolean = true,
    contentDescription: String? = null,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics {
                if (contentDescription != null) this.contentDescription = contentDescription
            }
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (enabled) DemoColors.TextSecondary else DemoColors.Muted,
            fontSize = 13.sp,
            maxLines = 1,
            modifier = Modifier.animateContentSize(animationSpec = tween(ACTION_ANIM_MS)),
        )
    }
}

private fun likeLabel(comment: UnifiedComment): String = when {
    comment.likeCount > 0L && comment.isLiked -> "已赞 ${comment.likeCount}"
    comment.likeCount > 0L -> "赞 ${comment.likeCount}"
    comment.isLiked -> "已赞"
    else -> "赞"
}

private fun hasOverflowActions(comment: UnifiedComment): Boolean =
    comment.canPin ||
        comment.canDelete ||
        (comment.canModerate && (
            comment.contentState == UnifiedCommentContentState.Hidden ||
                (comment.contentState == UnifiedCommentContentState.Visible && !comment.isPinned)
            ))

@Composable
private fun CommentOverflowMenu(
    comment: UnifiedComment,
    controller: UnifiedCommentController,
    scope: CoroutineScope,
    onRequestDelete: (UnifiedComment) -> Unit,
    onRequestHide: (UnifiedComment) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        ActionText(
            label = "⋯",
            contentDescription = "更多操作",
            onClick = { expanded = true },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (comment.canPin) {
                DropdownMenuItem(
                    text = { Text(if (comment.isPinned) "取消置顶" else "置顶") },
                    onClick = {
                        expanded = false
                        scope.launch { controller.setPinned(comment, !comment.isPinned) }
                    },
                )
            }
            if (comment.canModerate) {
                if (comment.contentState == UnifiedCommentContentState.Hidden) {
                    DropdownMenuItem(
                        text = { Text("恢复显示") },
                        onClick = {
                            expanded = false
                            scope.launch {
                                controller.moderate(
                                    comment,
                                    UnifiedCommentModerationAction.Unhide,
                                )
                            }
                        },
                    )
                } else if (comment.contentState == UnifiedCommentContentState.Visible &&
                    !comment.isPinned
                ) {
                    DropdownMenuItem(
                        text = { Text("隐藏评论") },
                        onClick = {
                            expanded = false
                            onRequestHide(comment)
                        },
                    )
                }
            }
            if (comment.canDelete) {
                DropdownMenuItem(
                    text = { Text("删除", color = DemoColors.Danger) },
                    onClick = {
                        expanded = false
                        onRequestDelete(comment)
                    },
                )
            }
        }
    }
}

@Composable
private fun ReplySection(
    rootId: String,
    thread: UnifiedCommentController.ReplyThread?,
    controller: UnifiedCommentController,
    scope: CoroutineScope,
    onRequestDelete: (UnifiedComment) -> Unit,
    onRequestHide: (UnifiedComment) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(top = 2.dp)) {
        AnimatedVisibility(
            visible = thread?.expanded == true,
            enter = expandVertically(tween(REPLY_ANIM_MS)) + fadeIn(tween(REPLY_ANIM_MS)),
            exit = shrinkVertically(tween(REPLY_ANIM_MS)) + fadeOut(tween(REPLY_ANIM_MS)),
        ) {
            Column(Modifier.fillMaxWidth()) {
                when {
                    thread?.loading == true -> SectionNote("加载回复…")
                    thread?.error != null -> Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = thread.error.message ?: "回复加载失败",
                            color = DemoColors.Danger,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { scope.launch { controller.toggleReplies(rootId) } }) {
                            Text("重试", color = DemoColors.Accent, fontSize = 13.sp)
                        }
                    }
                    else -> {
                        val replies = thread?.items.orEmpty()
                        if (replies.isEmpty()) {
                            SectionNote("暂无回复")
                        }
                        replies.forEach { reply ->
                            CommentBlock(
                                comment = reply,
                                isPinned = false,
                                controller = controller,
                                scope = scope,
                                onRequestDelete = onRequestDelete,
                                onRequestHide = onRequestHide,
                                isReply = true,
                            )
                        }
                        if (thread?.hasMore == true || thread?.loadingMore == true ||
                            thread?.loadMoreError != null
                        ) {
                            ReplyLoadMore(thread = thread, rootId = rootId, controller = controller, scope = scope)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReplyLoadMore(
    thread: UnifiedCommentController.ReplyThread,
    rootId: String,
    controller: UnifiedCommentController,
    scope: CoroutineScope,
) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        when {
            thread.loadingMore -> SectionNote("加载中…")
            thread.loadMoreError != null -> TextButton(
                onClick = { scope.launch { controller.loadMoreReplies(rootId) } },
            ) {
                Text("回复加载失败，点击重试", color = DemoColors.Danger, fontSize = 13.sp)
            }
            else -> TextButton(
                onClick = { scope.launch { controller.loadMoreReplies(rootId) } },
            ) {
                Text("查看更多回复", color = DemoColors.Accent, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun SectionNote(text: String) {
    Text(
        text = text,
        color = DemoColors.Muted,
        fontSize = 13.sp,
        modifier = Modifier.padding(vertical = 4.dp),
    )
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

@Composable
private fun Composer(
    controller: UnifiedCommentController,
    scope: CoroutineScope,
    focusRequester: FocusRequester,
    onSignIn: (() -> Unit)?,
) {
    Column(Modifier.fillMaxWidth().background(DemoColors.Background)) {
        HorizontalDivider(color = DemoColors.Divider)
        controller.composeError?.let { error ->
            Text(
                text = error.message ?: "评论操作失败，请稍后重试",
                color = DemoColors.Danger,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
        }
        controller.validationError?.let { message ->
            Text(
                text = message,
                color = DemoColors.Danger,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
        }
        controller.replyTo?.let { context ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (controller.replyToStale) {
                        "原评论已不可回复，请取消后重新选择"
                    } else {
                        "回复 @${context.nickname}"
                    },
                    color = if (controller.replyToStale) DemoColors.Danger else DemoColors.TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { controller.cancelReply() }) {
                    Text("取消", color = DemoColors.Muted, fontSize = 12.sp)
                }
            }
        }
        if (controller.authRequired && onSignIn == null) {
            Text(
                text = "登录后可参与评论",
                color = DemoColors.Muted,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = controller.draft,
                onValueChange = controller::updateDraft,
                textStyle = TextStyle(fontSize = 15.sp, color = DemoColors.TextPrimary),
                maxLines = 4,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp, max = 120.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(DemoColors.PageBg)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .focusRequester(focusRequester),
                decorationBox = { innerTextField ->
                    if (controller.draft.isEmpty()) {
                        Text("说点什么…", color = DemoColors.Muted, fontSize = 15.sp)
                    }
                    innerTextField()
                },
            )
            Spacer(Modifier.size(8.dp))
            val codePoints = countCommentCodePoints(controller.draft)
            val canSend = !controller.submitting &&
                controller.draft.trim().isNotEmpty() &&
                codePoints <= UnifiedCommentController.MAX_CONTENT_LENGTH
            TextButton(
                onClick = { scope.launch { controller.submit() } },
                enabled = canSend,
            ) {
                Text(
                    text = if (controller.submitting) "发送中…" else "发送",
                    color = if (canSend) DemoColors.Accent else DemoColors.Muted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
            val length = countCommentCodePoints(controller.draft)
            Text(
                text = "$length/${UnifiedCommentController.MAX_CONTENT_LENGTH}",
                color = if (length > UnifiedCommentController.MAX_CONTENT_LENGTH) {
                    DemoColors.Danger
                } else {
                    DemoColors.Muted
                },
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun InvalidTargetContent(onClose: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "无法打开评论",
            color = DemoColors.TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "评论目标无效或暂不支持",
            color = DemoColors.TextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onClose,
            colors = ButtonDefaults.buttonColors(
                containerColor = DemoColors.Primary,
                contentColor = DemoColors.OnPrimary,
            ),
        ) {
            Text("关闭")
        }
    }
}

@Composable
private fun UnavailableState(onClose: () -> Unit) {
    StateMessage(
        title = "评论功能暂未开放",
        description = "当前服务尚未启用统一评论，请稍后再试",
        actionLabel = "关闭",
        onAction = onClose,
    )
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    StateMessage(
        title = "加载失败",
        description = message,
        actionLabel = "重试",
        onAction = onRetry,
    )
}

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(color = DemoColors.Accent, strokeWidth = 2.dp)
        Spacer(Modifier.height(12.dp))
        Text("加载中…", color = DemoColors.Muted, fontSize = 13.sp)
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "暂无评论",
            color = DemoColors.TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(6.dp))
        Text("来抢沙发", color = DemoColors.TextSecondary, fontSize = 13.sp)
    }
}

@Composable
private fun StateMessage(
    title: String,
    description: String?,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            color = DemoColors.Danger,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        if (!description.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = description,
                color = DemoColors.TextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onAction,
            colors = ButtonDefaults.buttonColors(
                containerColor = DemoColors.Primary,
                contentColor = DemoColors.OnPrimary,
            ),
        ) {
            Text(actionLabel)
        }
    }
}

@Composable
private fun DeleteCommentDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("删除评论") },
        text = { Text("删除后不可恢复，确定删除这条评论？") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("删除", color = DemoColors.Danger)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@Composable
private fun HideCommentDialog(onConfirm: (String?) -> Unit, onDismiss: () -> Unit) {
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("隐藏评论") },
        text = {
            Column {
                Text("隐藏后其他用户将不可见。", color = DemoColors.TextSecondary, fontSize = 14.sp)
                Spacer(Modifier.height(10.dp))
                BasicTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    textStyle = TextStyle(fontSize = 14.sp, color = DemoColors.TextPrimary),
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DemoColors.PageBg)
                        .padding(12.dp),
                    decorationBox = { innerTextField ->
                        if (reason.isEmpty()) {
                            Text("原因（可选）", color = DemoColors.Muted, fontSize = 14.sp)
                        }
                        innerTextField()
                    },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(reason.trim().takeIf { it.isNotEmpty() })
                },
            ) {
                Text("隐藏", color = DemoColors.Danger)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.ui.PlatformNetworkImage
import com.example.my_kmp_project.feature.community.http.UnifiedComment
import com.example.my_kmp_project.feature.community.http.UnifiedCommentContentState
import com.example.my_kmp_project.feature.community.http.UnifiedCommentModerationAction
import com.example.my_kmp_project.feature.community.http.communityNowMs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import my_kmp_project.core.design.generated.resources.Res
import my_kmp_project.core.design.generated.resources.community_avatar

private const val REPLY_ANIM_MS = 160

private const val ACTION_ANIM_MS = 140

private val INDENT_STEP = 16.dp

@Composable
internal fun CommentBlock(
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

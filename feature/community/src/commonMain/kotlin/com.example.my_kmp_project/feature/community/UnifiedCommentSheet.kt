package com.example.my_kmp_project.feature.community

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.my_kmp_project.core.design.DemoColors

/** 底部评论面板的最大高度占比（覆盖层浮在内容之上，不重建底层页面）。 */
private const val SHEET_MAX_HEIGHT_FRACTION = 0.9f

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

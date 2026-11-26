package com.example.my_kmp_project.feature.community

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
internal fun Composer(
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

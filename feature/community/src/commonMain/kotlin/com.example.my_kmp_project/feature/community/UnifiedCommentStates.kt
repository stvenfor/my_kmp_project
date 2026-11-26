package com.example.my_kmp_project.feature.community

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors

@Composable
internal fun InvalidTargetContent(onClose: () -> Unit) {
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
internal fun UnavailableState(onClose: () -> Unit) {
    StateMessage(
        title = "评论功能暂未开放",
        description = "当前服务尚未启用统一评论，请稍后再试",
        actionLabel = "关闭",
        onAction = onClose,
    )
}

@Composable
internal fun ErrorState(message: String, onRetry: () -> Unit) {
    StateMessage(
        title = "加载失败",
        description = message,
        actionLabel = "重试",
        onAction = onRetry,
    )
}

@Composable
internal fun LoadingState() {
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
internal fun EmptyState() {
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
internal fun DeleteCommentDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
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
internal fun HideCommentDialog(onConfirm: (String?) -> Unit, onDismiss: () -> Unit) {
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

package com.example.my_kmp_project.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.DemoPrimaryButton
import com.example.my_kmp_project.core.design.DemoRadius

/**
 * 通用状态页语义类型：[Error] 表达加载/提交失败，[Empty] 表达无数据。
 */
enum class StateViewType {
    Error,
    Empty,
}

/**
 * 跨端复用的错误 / 无数据状态页。
 *
 * 文案（[title] / [description]）一律由调用方提供经过收口的通用文案，
 * 例如「加载失败，请稍后重试」；本组件不接收 Throwable，也不展示原始异常 message。
 * [actionLabel] 与 [onAction] 同时提供时才渲染操作按钮。
 */
@Composable
fun StateView(
    type: StateViewType,
    title: String,
    description: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(DemoRadius.Pill)
                .background(
                    when (type) {
                        StateViewType.Error -> DemoColors.ErrorSoft
                        StateViewType.Empty -> DemoColors.SurfaceSunken
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = when (type) {
                    StateViewType.Error -> "!"
                    StateViewType.Empty -> "—"
                },
                color = when (type) {
                    StateViewType.Error -> DemoColors.Danger
                    StateViewType.Empty -> DemoColors.Muted
                },
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            color = when (type) {
                StateViewType.Error -> DemoColors.Danger
                StateViewType.Empty -> DemoColors.TextPrimary
            },
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        if (!description.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = description,
                color = DemoColors.TextSecondary,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
            )
        }
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(18.dp))
            DemoPrimaryButton(
                text = actionLabel,
                onClick = onAction,
                modifier = Modifier.height(44.dp),
            )
        }
    }
}

package com.example.my_kmp_project.feature.community

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.platform.copyToClipboard
import com.example.my_kmp_project.core.platform.showPlatformToast

/**
 * 社区分享目标。仅用于面板展示与埋点占位：除「复制链接」外不调用任何分享 SDK。
 */
enum class CommunityShareTarget(
    val key: String,
    val label: String,
    val badge: String,
    val badgeColor: Color,
) {
    Wechat("wechat", "微信好友", "微", Color(0xFF07C160)),
    Moments("moments", "朋友圈", "圈", Color(0xFF07C160)),
    QQ("qq", "QQ", "Q", Color(0xFF12B7F5)),
    QZone("qzone", "QQ空间", "空", Color(0xFFFFB400)),
    Weibo("weibo", "微博", "博", Color(0xFFE6162D)),
    Douyin("douyin", "抖音", "抖", Color(0xFF161823)),
    CopyLink("copy", "复制链接", "链", Color(0xFF1677FF)),
    More("more", "更多", "…", Color(0xFF8A8F99)),
}

/**
 * 社区分享面板（主流渠道一次铺满，UI 效果与真实分享面板一致）。
 *
 * 行为约定：
 * - 微信 / 朋友圈 / QQ / QQ空间 / 微博 / 抖音 / 更多只展示，不调用分享 SDK，点击提示「暂未接入」；
 * - 复制链接真实写入剪贴板（无第三方依赖），链接按帖子 ID 生成；
 * - 点击任意目标后收起面板。
 *
 * @param postId 帖子 ID，用于生成分享链接
 * @param shareText 分享文案（可选）；为空时只复制链接
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun CommunityShareSheet(
    postId: String,
    onDismiss: () -> Unit,
    shareText: String = "",
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
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 8.dp),
        ) {
            Text(
                text = "分享到",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = DemoColors.TextPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 14.dp),
                textAlign = TextAlign.Center,
            )
            CommunityShareTarget.entries.chunked(4).forEach { row ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    row.forEach { target ->
                        ShareTargetItem(
                            target = target,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                when (target) {
                                    CommunityShareTarget.CopyLink -> {
                                        copyToClipboard(shareLink(postId, shareText))
                                        showPlatformToast("链接已复制")
                                    }
                                    CommunityShareTarget.More -> showPlatformToast("系统分享暂未接入")
                                    else -> showPlatformToast("${target.label}分享暂未接入")
                                }
                                onDismiss()
                            },
                        )
                    }
                    repeat(4 - row.size) { Box(Modifier.weight(1f)) }
                }
            }
            HorizontalDivider(
                modifier = Modifier.padding(top = 16.dp),
                thickness = 0.5.dp,
                color = DemoColors.Divider,
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                Text("取消", fontSize = 16.sp, color = DemoColors.TextSecondary)
            }
        }
    }
}

@Composable
private fun ShareTargetItem(
    target: CommunityShareTarget,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(target.badgeColor),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = target.badge,
                color = Color.White,
                fontSize = if (target.badge.length > 1) 14.sp else 22.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Text(
            text = target.label,
            fontSize = 12.sp,
            color = DemoColors.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/** 分享链接：BFF 暂无公开帖子页，用社区详情深链占位，保证复制出来可直接打开 App。 */
internal fun shareLink(postId: String, shareText: String = ""): String {
    val link = "https://xiaomaomain.com/community/post/$postId"
    return if (shareText.isBlank()) link else "$shareText $link"
}

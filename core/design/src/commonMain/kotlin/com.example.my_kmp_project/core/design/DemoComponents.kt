package com.example.my_kmp_project.core.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 共享 UI 基线：圆角 / 阴影 / 渐变 / 卡片 / 区块标题 / 按钮 / 图标底 / 空态。
 *
 * 设计方向：中性克制画布 + 柔和分层表面 + 品牌蓝紫渐变点缀。
 * - 卡片不再只靠 0.5px 描边，而是「微阴影 + 1px 低对比描边 + 16dp 圆角」。
 * - 所有页面统一走本文件，避免各 feature 自行写死圆角与阴影。
 * - 不涉及 insets：沉浸式仍由 [ImmersiveInsets] 与平台入口负责。
 */
object DemoRadius {
    val Xs: Dp = DesignTokens.Radius.Xs.dp
    val Sm: Dp = DesignTokens.Radius.Sm.dp
    val Md: Dp = DesignTokens.Radius.Md.dp
    val Lg: Dp = DesignTokens.Radius.Lg.dp
    val Xl: Dp = DesignTokens.Radius.Xl.dp
    val Xxl: Dp = DesignTokens.Radius.Xxl.dp

    /** 卡片默认圆角。 */
    val Card: Dp = Lg

    /** Hero / Banner 等大面积容器。 */
    val Hero: Dp = Xl

    /** 药丸标签 / 头像。 */
    val Pill: Shape = RoundedCornerShape(percent = 50)
}

object DemoElevation {
    val Card: Dp = DesignTokens.Elevation.Sm.dp
    val Floating: Dp = DesignTokens.Elevation.Md.dp
    val Overlay: Dp = DesignTokens.Elevation.Lg.dp

    /** 卡片统一的柔和环境阴影（低透明度、大模糊，避免发灰）。 */
    val CardShadow = Color(0x14171717)
}

object DemoGradients {
    val Brand: List<Color> = listOf(
        DemoColors.GradientStart,
        DemoColors.GradientMid,
        DemoColors.GradientEnd,
    )
    val BrandSoft: List<Color> = listOf(DemoColors.PrimarySoft, DemoColors.VioletSoft)

    fun brandHorizontal(): Brush = Brush.horizontalGradient(Brand)
    fun brandSoftHorizontal(): Brush = Brush.horizontalGradient(BrandSoft)
    fun brandVertical(): Brush = Brush.verticalGradient(Brand)
}

/**
 * 统一卡片表面：微阴影 + 圆角裁剪 + 表面色 + 低对比描边。
 * 用法：`Column(modifier = Modifier.demoCard().padding(16.dp)) { … }`
 */
fun Modifier.demoCard(
    shape: Shape = RoundedCornerShape(DemoRadius.Card),
    color: Color = DemoColors.SurfaceRaised,
    elevation: Dp = DemoElevation.Card,
    borderWidth: Dp = 0.5.dp,
    borderColor: Color = DemoColors.Divider.copy(alpha = 0.75f),
): Modifier = this
    .shadow(elevation, shape, clip = false, ambientColor = DemoElevation.CardShadow, spotColor = DemoElevation.CardShadow)
    .clip(shape)
    .background(color)
    .border(borderWidth, borderColor, shape)

/** 浮层表面：更强的阴影，用于弹窗 / 悬浮操作条。 */
fun Modifier.demoFloating(
    shape: Shape = RoundedCornerShape(DemoRadius.Xl),
    color: Color = DemoColors.SurfaceFloating,
): Modifier = this
    .shadow(DemoElevation.Overlay, shape, clip = false, ambientColor = DemoElevation.CardShadow, spotColor = DemoElevation.CardShadow)
    .clip(shape)
    .background(color)

@Composable
fun DemoCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(DemoRadius.Card),
    color: Color = DemoColors.SurfaceRaised,
    elevation: Dp = DemoElevation.Card,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val surface = modifier.demoCard(shape = shape, color = color, elevation = elevation)
    Column(
        modifier = if (onClick != null) surface.clickable(onClick = onClick) else surface,
    ) {
        Column(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}

/** 区块标题：20sp 半粗 + 可选右侧操作入口。 */
@Composable
fun DemoSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = DemoColors.TextPrimary,
            fontSize = DesignTokens.Typography.TitleMdSp.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.3).sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) {
            trailing()
        } else if (actionText != null && onAction != null) {
            Row(
                modifier = Modifier
                    .clip(DemoRadius.Pill)
                    .clickable(onClick = onAction)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = actionText,
                    color = DemoColors.Accent,
                    fontSize = DesignTokens.Typography.LabelMdSp.sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.size(2.dp))
                Text(
                    text = "›",
                    color = DemoColors.Accent,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

/** 图标底：品牌浅底 + 圆角，统一首页九宫格 / 服务网格 / 入口卡。 */
@Composable
fun DemoIconTile(
    icon: ImageVector,
    contentDescription: String?,
    tint: Color = DemoColors.Accent,
    container: Color = tint.copy(alpha = 0.10f),
    size: Dp = 46.dp,
    iconSize: Dp = 22.dp,
    radius: Dp = DemoRadius.Md,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(radius))
            .background(container),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize),
        )
    }
}

/** 药丸标签：用于「3 条新消息」「热门」「已认证」等状态点缀。 */
@Composable
fun DemoPill(
    text: String,
    modifier: Modifier = Modifier,
    contentColor: Color = DemoColors.Accent,
    containerColor: Color = DemoColors.PrimarySoft,
    leading: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .clip(DemoRadius.Pill)
            .background(containerColor)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        leading?.invoke()
        Text(
            text = text,
            color = contentColor,
            fontSize = DesignTokens.Typography.CaptionSp.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

/** 主按钮：品牌色 + 药丸 + 48dp 点击高度。 */
@Composable
fun DemoPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(48.dp),
        shape = DemoRadius.Pill,
        colors = ButtonDefaults.buttonColors(
            containerColor = DemoColors.Primary,
            contentColor = DemoColors.OnPrimary,
            disabledContainerColor = DemoColors.Divider,
            disabledContentColor = DemoColors.Muted,
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        contentPadding = PaddingValues(horizontal = 22.dp),
    ) {
        Text(text = text, fontSize = DesignTokens.Typography.BodyMdSp.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** 空态：图标 + 标题 + 说明，用于接口无数据时的诚实占位。 */
@Composable
fun DemoEmptyState(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    icon: ImageVector? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(DemoRadius.Pill)
                    .background(DemoColors.SurfaceSunken),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = DemoColors.Muted,
                    modifier = Modifier.size(26.dp),
                )
            }
            Spacer(Modifier.height(14.dp))
        }
        Text(
            text = title,
            color = DemoColors.TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
        if (!message.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = message,
                color = DemoColors.Muted,
                fontSize = DesignTokens.Typography.LabelMdSp.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
            )
        }
        if (action != null) {
            Spacer(Modifier.height(18.dp))
            action()
        }
    }
}

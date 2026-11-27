package com.example.my_kmp_project.feature.auth

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.DemoElevation
import com.example.my_kmp_project.core.design.DemoFonts
import com.example.my_kmp_project.core.design.DemoRadius
import com.example.my_kmp_project.core.design.DesignTokens
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * 登录/注册表单的语义别名——颜色与圆角全部委托给共享设计令牌，
 * 避免 auth 模块自带一套与 SoT 冲突的调色板（原 #007AFF / #F2F2F7 / #000000）。
 * 属性名保持不变，四个表单页无需改调用点。
 */
public object AuthUiTokens {
    val Accent = DemoColors.Accent
    val Background = DemoColors.PageBg
    val Surface = DemoColors.Background
    val FillSecondary = DemoColors.Divider
    val LabelPrimary = DemoColors.TextPrimary
    val LabelSecondary = DemoColors.TextSecondary
    val LabelTertiary = DemoColors.Muted
    val Separator = DemoColors.Divider
    /** 无独立 SoT 令牌：Mute 半透明，近似原禁用灰。 */
    val ButtonDisabled = DemoColors.Muted.copy(alpha = 0.5f)
    /** 表单圆角跟随新设计基线：输入框 12dp、按钮药丸。 */
    val RadiusMd = DesignTokens.Radius.Md.dp
    val RadiusLg = DesignTokens.Radius.Lg.dp
    val FieldHeight = 52.dp
    val ButtonHeight = 52.dp
    /** 四个表单页统一的主标题 / 副标题规格。 */
    val TitleSize = 30.sp
    val TitleLineHeight = 34.sp
    val TitleLetterSpacing = (-1.2).sp
    val TitleWeight = FontWeight.SemiBold
    val SubtitleSize = 15.sp
}

/** Local hour approximated as UTC+8 (product locale). */
@OptIn(ExperimentalTime::class)
public fun authGreeting(): String {
    val epochMs = Clock.System.now().toEpochMilliseconds()
    val hour = (((epochMs / 3_600_000L) + 8) % 24).toInt()
    return authGreetingForHour(hour)
}

/** Flutter `AuthController.greeting` brand string. */
public fun authGreetingForHour(hour: Int): String = when {
    hour < 12 -> "早上好，欢迎使用iHome"
    hour < 18 -> "下午好，欢迎使用iHome"
    else -> "晚上好，欢迎使用iHome"
}

@Composable
public fun AuthSegmentedControl(
    leftLabel: String,
    rightLabel: String,
    leftSelected: Boolean,
    onSelectLeft: () -> Unit,
    onSelectRight: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(DemoColors.SurfaceSunken, RoundedCornerShape(DemoRadius.Xl))
            .padding(4.dp),
    ) {
        AuthSegmentChip(
            label = leftLabel,
            selected = leftSelected,
            onClick = onSelectLeft,
            modifier = Modifier.weight(1f),
        )
        AuthSegmentChip(
            label = rightLabel,
            selected = !leftSelected,
            onClick = onSelectRight,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun AuthSegmentChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 只改背景色，不改尺寸/位置：切换 tab 不会移动输入框或影响键盘。
    val chipColor by animateColorAsState(
        targetValue = if (selected) AuthUiTokens.Surface else Color.Transparent,
        animationSpec = tween(durationMillis = 180),
        label = "authSegmentChip",
    )
    Box(
        modifier = modifier
            .height(40.dp)
            .shadow(
                elevation = if (selected) 2.dp else 0.dp,
                shape = RoundedCornerShape(DemoRadius.Card),
                ambientColor = DemoElevation.CardShadow,
                spotColor = DemoElevation.CardShadow,
            )
            .background(
                color = chipColor,
                shape = RoundedCornerShape(DemoRadius.Card),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = AuthUiTokens.LabelPrimary,
        )
    }
}

@Composable
public fun AuthFilledField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(AuthUiTokens.FieldHeight)
            .background(DemoColors.SurfaceSunken, RoundedCornerShape(AuthUiTokens.RadiusMd))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(modifier = Modifier.width(8.dp))
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = TextStyle(
                fontFamily = DemoFonts.Geist,
                fontSize = 16.sp,
                color = AuthUiTokens.LabelPrimary,
            ),
            cursorBrush = SolidColor(AuthUiTokens.Accent),
            visualTransformation = if (isPassword) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            text = hint,
                            fontFamily = DemoFonts.Geist,
                            color = AuthUiTokens.LabelTertiary,
                            fontSize = 16.sp,
                        )
                    }
                    inner()
                }
            },
        )
        if (trailing != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
        }
    }
}

@Composable
public fun AuthPrimaryButton(
    label: String,
    enabled: Boolean,
    loading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier
            .fillMaxWidth()
            .height(AuthUiTokens.ButtonHeight),
        shape = DemoRadius.Pill,
        colors = ButtonDefaults.buttonColors(
            containerColor = AuthUiTokens.Accent,
            contentColor = Color.White,
            disabledContainerColor = AuthUiTokens.ButtonDisabled,
            disabledContentColor = Color.White.copy(alpha = 0.8f),
        ),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.dp,
                color = Color.White,
            )
        } else {
            Text(
                text = label,
                fontFamily = DemoFonts.Geist,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

/** 注册表驱动的第三方登录按钮。 */
@Composable
public fun AuthMethodButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(AuthUiTokens.ButtonHeight)
            .border(1.dp, AuthUiTokens.Separator, DemoRadius.Pill)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "◎",
                color = AuthUiTokens.Accent,
                fontSize = 18.sp,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                color = AuthUiTokens.Accent,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
public fun AuthPrivacyRow(
    agreed: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Flutter privacy row: 44-tap target, 22 circle checkbox, 《某个隐私条款》.
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(
                        color = if (agreed) AuthUiTokens.Accent else Color.Transparent,
                        shape = CircleShape,
                    )
                    .border(
                        width = 1.5.dp,
                        color = if (agreed) AuthUiTokens.Accent else AuthUiTokens.Separator,
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (agreed) {
                    Text("✓", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Row(modifier = Modifier.padding(top = 12.dp)) {
            Text(
                text = "我已阅读并同意",
                color = AuthUiTokens.LabelSecondary,
                fontSize = 12.sp,
            )
            Text(
                text = "《某个隐私条款》",
                color = AuthUiTokens.Accent,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
public fun AuthErrorText(
    message: String?,
    modifier: Modifier = Modifier,
) {
    if (message.isNullOrBlank()) return
    // 纯文本、无内部间距与动画；间距由调用方在错误显示的条件分支内给出，文案变化只随状态更新。
    Text(
        text = message,
        color = DemoColors.Danger,
        fontSize = 13.sp,
        fontFamily = DemoFonts.Geist,
        modifier = modifier,
    )
}

@Composable
public fun AuthFooterLinks(
    onRegister: () -> Unit,
    onForgotPassword: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onRegister) {
            Text(
                text = "我要注册",
                color = AuthUiTokens.Accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .width(1.dp)
                .height(12.dp)
                .background(AuthUiTokens.Separator),
        )
        TextButton(onClick = onForgotPassword) {
            Text(
                text = "忘记密码",
                color = AuthUiTokens.Accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

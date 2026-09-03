package com.example.my_kmp_project.feature.auth

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** Flutter `AuthTheme` tokens used inside auth feature only. */
internal object AuthUiTokens {
    val Accent = Color(0xFF007AFF)
    val Background = Color(0xFFF2F2F7)
    val Surface = Color(0xFFFFFFFF)
    val FillSecondary = Color(0xFFE9E9EB)
    val LabelPrimary = Color(0xFF000000)
    val LabelSecondary = Color(0x993C3C43)
    val LabelTertiary = Color(0x4D3C3C43)
    val Separator = Color(0xFFC6C6C8)
    val ButtonDisabled = Color(0xFFC7C7CC)
    val RadiusMd = 12.dp
    val FieldHeight = 52.dp
    val ButtonHeight = 52.dp
}

/** Local hour approximated as UTC+8 (product locale). */
@OptIn(ExperimentalTime::class)
internal fun authGreeting(): String {
    val epochMs = Clock.System.now().toEpochMilliseconds()
    val hour = (((epochMs / 3_600_000L) + 8) % 24).toInt()
    return authGreetingForHour(hour)
}

internal fun authGreetingForHour(hour: Int): String = when {
    hour < 12 -> "早上好，欢迎使用i车商"
    hour < 18 -> "下午好，欢迎使用i车商"
    else -> "晚上好，欢迎使用i车商"
}

@Composable
internal fun AuthSegmentedControl(
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
            .background(AuthUiTokens.FillSecondary, RoundedCornerShape(AuthUiTokens.RadiusMd))
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
    Box(
        modifier = modifier
            .height(40.dp)
            .background(
                color = if (selected) AuthUiTokens.Surface else Color.Transparent,
                shape = RoundedCornerShape(10.dp),
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
internal fun AuthFilledField(
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
            .background(AuthUiTokens.Surface, RoundedCornerShape(AuthUiTokens.RadiusMd))
            .border(0.5.dp, AuthUiTokens.Separator, RoundedCornerShape(AuthUiTokens.RadiusMd))
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
                fontSize = 17.sp,
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
                            color = AuthUiTokens.LabelTertiary,
                            fontSize = 17.sp,
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
internal fun AuthPrimaryButton(
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
        shape = RoundedCornerShape(4.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AuthUiTokens.Accent,
            contentColor = Color.White,
            disabledContainerColor = AuthUiTokens.ButtonDisabled,
            disabledContentColor = Color.White,
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
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
internal fun AuthPrivacyRow(
    agreed: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(
                    color = if (agreed) AuthUiTokens.Accent else AuthUiTokens.Surface,
                    shape = RoundedCornerShape(6.dp),
                )
                .border(
                    width = 1.dp,
                    color = if (agreed) AuthUiTokens.Accent else AuthUiTokens.Separator,
                    shape = RoundedCornerShape(6.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (agreed) {
                Text("✓", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "我已阅读并同意《用户协议》和《隐私政策》",
            color = AuthUiTokens.LabelSecondary,
            fontSize = 13.sp,
        )
    }
}

@Composable
internal fun AuthErrorText(
    message: String?,
    modifier: Modifier = Modifier,
) {
    if (message.isNullOrBlank()) return
    Text(
        text = message,
        color = DemoColors.Danger,
        fontSize = 13.sp,
        modifier = modifier.padding(top = 12.dp),
    )
}

@Composable
internal fun AuthFooterLinks(
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
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        Box(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .width(1.dp)
                .height(12.dp)
                .background(AuthUiTokens.Separator),
        )
        TextButton(onClick = onForgotPassword) {
            Text(
                text = "忘记密码",
                color = AuthUiTokens.Accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

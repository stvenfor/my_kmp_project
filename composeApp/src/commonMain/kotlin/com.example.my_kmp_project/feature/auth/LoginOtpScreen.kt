package com.example.my_kmp_project.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.MineTopBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Flutter `LoginOtpPage` — 6-digit OTP after phone send.
 */
@Composable
internal fun LoginOtpScreen(
    onLoginSuccess: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var otp by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var otpCooldown by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val phone = AuthPendingCredentials.phone.filter { it.isDigit() }.take(11)
    val canSubmit = otp.length == 6 && AuthPhoneUtils.isValidChinaMobile(phone) && !loading

    LaunchedEffect(otpCooldown) {
        if (otpCooldown <= 0) return@LaunchedEffect
        delay(1_000)
        otpCooldown -= 1
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AuthUiTokens.Surface),
    ) {
        MineTopBar(
            title = "",
            onBack = onBack,
            containerColor = AuthUiTokens.Surface,
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 16.dp),
        ) {
            Text(
                text = "输入验证码",
                color = AuthUiTokens.LabelPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "验证码已发送至",
                color = AuthUiTokens.LabelSecondary,
                fontSize = 14.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "测试号 13400000000, 验证码 123456",
                color = AuthUiTokens.Accent,
                fontSize = 13.sp,
            )
            Spacer(modifier = Modifier.height(40.dp))
            BasicTextField(
                value = otp,
                onValueChange = { raw ->
                    otp = raw.filter { it.isDigit() }.take(6)
                    error = null
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = TextStyle(
                    color = AuthUiTokens.LabelPrimary,
                    fontSize = 24.sp,
                    letterSpacing = 8.sp,
                ),
                cursorBrush = SolidColor(AuthUiTokens.Accent),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    Column {
                        if (otp.isEmpty()) {
                            Text(
                                text = "6 位验证码",
                                color = AuthUiTokens.LabelTertiary,
                                fontSize = 18.sp,
                            )
                        }
                        inner()
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = AuthUiTokens.Accent)
                    }
                },
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = {
                    if (otpCooldown > 0 || loading) return@TextButton
                    if (!AuthPhoneUtils.isValidChinaMobile(phone)) {
                        error = "请输入有效的手机号"
                        return@TextButton
                    }
                    scope.launch {
                        loading = true
                        AuthRepository.sendPhoneOtp(phone).fold(
                            onSuccess = { otpCooldown = 60 },
                            onFailure = { error = it.message ?: "发送失败" },
                        )
                        loading = false
                    }
                },
                modifier = Modifier.align(Alignment.End),
                enabled = otpCooldown == 0 && !loading,
            ) {
                Text(
                    text = if (otpCooldown > 0) "${otpCooldown}s 后重发" else "重新发送",
                    color = AuthUiTokens.LabelPrimary,
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            if (error != null) {
                Text(text = error!!, color = AlertRed, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(12.dp))
            }
            AuthPrimaryButton(
                label = "登录",
                enabled = canSubmit,
                loading = loading,
                onClick = {
                    scope.launch {
                        loading = true
                        error = null
                        AuthRepository.loginWithOtp(phone, otp).fold(
                            onSuccess = { onLoginSuccess() },
                            onFailure = { error = it.message ?: "登录失败" },
                        )
                        loading = false
                    }
                },
            )
        }
    }
}

private val AlertRed = Color(0xFFFF3B30)

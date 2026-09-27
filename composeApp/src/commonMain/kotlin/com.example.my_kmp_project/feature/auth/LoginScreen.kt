package com.example.my_kmp_project.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.platform.PlatformBackHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class LoginMode { Email, Phone }

/**
 * Flutter-aligned login (邮箱 / 短信) via remote [AuthRepository].
 */
@Composable
internal fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onOpenRegister: () -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    if (onBack != null) {
        PlatformBackHandler(onBack = onBack)
    }
    var emailMode by remember { mutableStateOf(true) }
    val mode = if (emailMode) LoginMode.Email else LoginMode.Phone

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    // Flutter SoT test account (docs/USAGE_GUIDE.md · USE_MOCK_AUTH / Go OTP bypass)
    var phone by remember { mutableStateOf("13400000000") }
    var otp by remember { mutableStateOf("") }
    var agreedPrivacy by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var otpCooldown by remember { mutableIntStateOf(0) }
    var otpHint by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(email, phone) {
        AuthPendingCredentials.email = email
        AuthPendingCredentials.phone = phone
    }

    LaunchedEffect(otpCooldown) {
        if (otpCooldown <= 0) return@LaunchedEffect
        delay(1_000)
        otpCooldown -= 1
    }

    // Flutter LoginPage: no AppNavBar — status inset + 32 top padding.
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AuthUiTokens.Background)
            .statusBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                // Flutter: AppSafeInsets.top + 32; nudge −3.dp so content matches status-inset delta on gate viewport.
                .padding(start = 24.dp, end = 24.dp, top = 29.dp, bottom = 24.dp),
        ) {
            Text(
                text = authGreeting(),
                color = AuthUiTokens.LabelPrimary,
                fontSize = 32.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 36.sp,
                letterSpacing = (-1.6).sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "登录以继续使用",
                color = AuthUiTokens.LabelSecondary,
                fontSize = 14.sp,
            )
            Spacer(modifier = Modifier.height(40.dp))

            AuthSegmentedControl(
                leftLabel = "邮箱登录",
                rightLabel = "短信登录",
                leftSelected = emailMode,
                onSelectLeft = {
                    emailMode = true
                    error = null
                },
                onSelectRight = {
                    emailMode = false
                    error = null
                },
            )
            Spacer(modifier = Modifier.height(24.dp))

            when (mode) {
                LoginMode.Email -> {
                    AuthFilledField(
                        value = email,
                        onValueChange = { email = it; error = null },
                        hint = "邮箱",
                        keyboardType = KeyboardType.Email,
                        leading = {
                            Text("✉", color = AuthUiTokens.LabelSecondary, fontSize = 18.sp)
                        },
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    AuthFilledField(
                        value = password,
                        onValueChange = { password = it; error = null },
                        hint = "密码",
                        keyboardType = KeyboardType.Password,
                        isPassword = true,
                        leading = {
                            Text("⌂", color = AuthUiTokens.LabelSecondary, fontSize = 18.sp)
                        },
                    )
                }
                LoginMode.Phone -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .height(AuthUiTokens.FieldHeight)
                                .background(
                                    AuthUiTokens.Surface,
                                    RoundedCornerShape(AuthUiTokens.RadiusMd),
                                )
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "+86",
                                color = AuthUiTokens.LabelPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        AuthFilledField(
                            value = phone,
                            onValueChange = { raw ->
                                phone = raw.filter { it.isDigit() }.take(11)
                                error = null
                            },
                            hint = "手机号",
                            keyboardType = KeyboardType.Phone,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AuthFilledField(
                            value = otp,
                            onValueChange = { raw ->
                                otp = raw.filter { it.isDigit() }.take(6)
                                error = null
                            },
                            hint = "验证码",
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        TextButton(
                            onClick = {
                                if (!agreedPrivacy) {
                                    error = "请先阅读并同意隐私条款"
                                    return@TextButton
                                }
                                if (!AuthPhoneUtils.isValidChinaMobile(phone)) {
                                    error = "请输入有效的手机号"
                                    return@TextButton
                                }
                                if (otpCooldown > 0 || loading) return@TextButton
                                scope.launch {
                                    loading = true
                                    error = null
                                    AuthRepository.sendPhoneOtp(phone).fold(
                                        onSuccess = {
                                            otpCooldown = 60
                                            otpHint = "验证码已发送"
                                        },
                                        onFailure = { error = it.message ?: "发送失败" },
                                    )
                                    loading = false
                                }
                            },
                            enabled = otpCooldown <= 0 && !loading,
                        ) {
                            Text(
                                text = if (otpCooldown > 0) "${otpCooldown}s" else "获取验证码",
                                color = AuthUiTokens.Accent,
                                fontSize = 14.sp,
                            )
                        }
                    }
                    if (otpHint != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = otpHint.orEmpty(),
                            color = AuthUiTokens.LabelSecondary,
                            fontSize = 12.sp,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            AuthPrivacyRow(
                agreed = agreedPrivacy,
                onToggle = { agreedPrivacy = !agreedPrivacy; error = null },
            )
            AuthErrorText(error)

            Spacer(modifier = Modifier.height(32.dp))
            AuthPrimaryButton(
                label = "登录",
                enabled = when (mode) {
                    LoginMode.Email ->
                        email.isNotBlank() &&
                            password.length >= AuthValidators.MIN_PASSWORD_LENGTH &&
                            agreedPrivacy
                    LoginMode.Phone ->
                        AuthPhoneUtils.isValidChinaMobile(phone) &&
                            AuthValidators.isValidOtp(otp) &&
                            agreedPrivacy
                },
                loading = loading,
                onClick = {
                    if (!agreedPrivacy) {
                        error = "请先阅读并同意隐私条款"
                        return@AuthPrimaryButton
                    }
                    scope.launch {
                        loading = true
                        error = null
                        val result = when (mode) {
                            LoginMode.Email ->
                                AuthRepository.loginWithPassword(email, password)
                            LoginMode.Phone ->
                                AuthRepository.loginWithOtp(phone, otp)
                        }
                        result.fold(
                            onSuccess = { onLoginSuccess() },
                            onFailure = { error = it.message ?: "登录失败" },
                        )
                        loading = false
                    }
                },
            )

            Spacer(modifier = Modifier.height(16.dp))
            AuthWechatButton(
                enabled = !loading,
                onClick = { error = "微信登录暂未开放（见 platform-gap-registry）" },
            )

            Spacer(modifier = Modifier.height(24.dp))
            AuthFooterLinks(
                onRegister = onOpenRegister,
                onForgotPassword = { error = "忘记密码功能暂未开放" },
            )
        }
    }
}

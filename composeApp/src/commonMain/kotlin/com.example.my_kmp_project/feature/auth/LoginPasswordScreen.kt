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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.MineTopBar
import kotlinx.coroutines.launch

/**
 * Flutter `LoginPasswordPage` — underline password field after hub email entry.
 */
@Composable
internal fun LoginPasswordScreen(
    onLoginSuccess: () -> Unit,
    onOpenRegister: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var password by remember { mutableStateOf("") }
    var account by remember { mutableStateOf(AuthPendingCredentials.email) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val resolvedAccount = account.trim().ifBlank { AuthPendingCredentials.email.trim() }
    val canSubmit = password.length >= 6 && resolvedAccount.isNotBlank() && !loading

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
                text = "请输入你的密码",
                color = AuthUiTokens.LabelPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(40.dp))
            if (AuthPendingCredentials.email.isBlank()) {
                BasicTextField(
                    value = account,
                    onValueChange = {
                        account = it
                        AuthPendingCredentials.email = it
                        error = null
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    textStyle = TextStyle(
                        color = AuthUiTokens.LabelPrimary,
                        fontSize = 18.sp,
                    ),
                    cursorBrush = SolidColor(AuthUiTokens.Accent),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        Column {
                            if (account.isEmpty()) {
                                Text(
                                    text = "邮箱",
                                    color = AuthUiTokens.LabelTertiary,
                                    fontSize = 18.sp,
                                )
                            }
                            inner()
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = AuthUiTokens.Separator)
                        }
                    },
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
            BasicTextField(
                value = password,
                onValueChange = { password = it; error = null },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                textStyle = TextStyle(
                    color = AuthUiTokens.LabelPrimary,
                    fontSize = 18.sp,
                ),
                cursorBrush = SolidColor(AuthUiTokens.Accent),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    Column {
                        if (password.isEmpty()) {
                            Text(
                                text = "至少6位密码",
                                color = AuthUiTokens.LabelTertiary,
                                fontSize = 18.sp,
                            )
                        }
                        inner()
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = AuthUiTokens.Accent.copy(alpha = 0.5f))
                    }
                },
            )
            Spacer(modifier = Modifier.height(40.dp))
            if (error != null) {
                Text(text = error!!, color = DemoAlertRed, fontSize = 13.sp)
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
                        AuthRepository.loginWithPassword(resolvedAccount, password).fold(
                            onSuccess = { onLoginSuccess() },
                            onFailure = { error = it.message ?: "登录失败" },
                        )
                        loading = false
                    }
                },
            )
            Spacer(modifier = Modifier.height(20.dp))
            AuthFooterLinks(
                onRegister = onOpenRegister,
                onForgotPassword = { error = "请联系管理员重置密码" },
            )
        }
    }
}

private val DemoAlertRed = androidx.compose.ui.graphics.Color(0xFFFF3B30)

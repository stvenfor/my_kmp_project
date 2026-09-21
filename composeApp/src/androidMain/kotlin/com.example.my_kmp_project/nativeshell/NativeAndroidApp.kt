package com.example.my_kmp_project.nativeshell

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.app.AppContainer
import com.example.my_kmp_project.core.account.createPrivacyConsentStore
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.DesignTokens
import kotlinx.coroutines.delay
import my_kmp_project.composeapp.generated.resources.Res
import my_kmp_project.composeapp.generated.resources.bg_splash
import my_kmp_project.composeapp.generated.resources.ic_splash_logo
import org.jetbrains.compose.resources.painterResource

private enum class NativePhase { Splash, Privacy, Main }

/**
 * Android entry — ADR 0002 Jetpack shell (splash/privacy/tab roots/auth).
 * Shared CMP is only the Mine island overlay.
 */
@Composable
fun NativeAndroidApp() {
    AppContainer.get()
    val privacyStore = remember { createPrivacyConsentStore() }
    var phase by remember { mutableStateOf(NativePhase.Splash) }
    var privacyAccepted by remember { mutableStateOf(privacyStore.isAccepted()) }

    when (phase) {
        NativePhase.Splash -> NativeSplash {
            phase = if (privacyAccepted) NativePhase.Main else NativePhase.Privacy
        }
        NativePhase.Privacy -> NativePrivacy(
            onAccept = {
                privacyStore.setAccepted(true)
                privacyAccepted = true
                phase = NativePhase.Main
            },
        )
        NativePhase.Main -> NativeAndroidMain()
    }
}

@Composable
private fun NativeSplash(onFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(1200)
        onFinished()
    }
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(Res.drawable.bg_splash),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Image(
            painter = painterResource(Res.drawable.ic_splash_logo),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.Center)
                .size(96.dp),
        )
    }
}

@Composable
private fun NativePrivacy(onAccept: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DemoColors.PageBg)
            .statusBarsPadding()
            .padding(DesignTokens.Spacing.Lg.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "隐私政策",
            color = DemoColors.TextPrimary,
            fontSize = DesignTokens.Typography.DisplayMdSp.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(DesignTokens.Spacing.Md.dp))
        Text(
            text = "请阅读并同意隐私政策后继续使用本应用。",
            color = DemoColors.TextSecondary,
            fontSize = DesignTokens.Typography.BodyMdSp.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(DesignTokens.Spacing.Lg.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.Md.dp)) {
            TextButton(onClick = { /* Flutter blocks without grant */ }) {
                Text("不同意", color = DemoColors.TextSecondary)
            }
            Button(
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(containerColor = DemoColors.Primary),
            ) {
                Text("同意并继续")
            }
        }
    }
}

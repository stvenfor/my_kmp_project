package com.example.my_kmp_project

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.my_kmp_project.nativeshell.NativeAndroidApp
import com.example.my_kmp_project.core.log.AppLog
import com.example.my_kmp_project.core.storage.AndroidAppContext
import com.example.my_kmp_project.core.network.platformNetworkBootstrap
import com.example.my_kmp_project.core.platform.AndroidActivityContext
import com.example.my_kmp_project.core.router.acceptDeepLink
import com.example.my_kmp_project.app.ShellBootstrap

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        requestedOrientation = if (
            resources.configuration.smallestScreenWidthDp >= TABLET_SMALLEST_WIDTH_DP
        ) {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        AndroidAppContext.install(this)
        AndroidActivityContext.install(this)
        // 统一日志：debug 包放开 DEBUG；release 包保持 AppLog 默认 WARN（DEBUG/INFO 不可见）。
        if ((applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            AppLog.minLevel = AppLog.Level.Debug
        }
        platformNetworkBootstrap()
        // 会话水合 + 401 静默刷新装机（幂等）；须早于任何业务请求。
        ShellBootstrap.install()
        handleDeepLinkIntent(intent)

        setContent {
            NativeAndroidApp()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLinkIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        AndroidActivityContext.install(this)
    }

    override fun onPause() {
        AndroidActivityContext.clear(this)
        super.onPause()
    }

    private fun handleDeepLinkIntent(intent: Intent?) {
        val data: Uri = intent?.data ?: return
        acceptDeepLink(data.toString())
    }
}

private const val TABLET_SMALLEST_WIDTH_DP = 600

@Composable
internal fun AppAndroidPreview() {
    NativeAndroidApp()
}

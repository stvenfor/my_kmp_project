package com.example.my_kmp_project

import android.content.Intent
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
import com.example.my_kmp_project.core.storage.AndroidAppContext
import com.example.my_kmp_project.core.network.platformNetworkBootstrap
import com.example.my_kmp_project.core.platform.AndroidActivityContext
import com.example.my_kmp_project.core.router.acceptDeepLink
import com.example.my_kmp_project.app.ShellBootstrap
import com.example.my_kmp_project.feature.auth.MockAuthConfig

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        AndroidAppContext.install(this)
        AndroidActivityContext.install(this)
        platformNetworkBootstrap()
        // Mock 认证开关：core:network 的 android actual 恒 false，这里用 APK 真实的
        // debuggable 标志覆盖 → debug 包走本地 Mock（13400000000 / 123456），
        // release 包自动回落真实 BFF。
        MockAuthConfig.useMockAuth =
            (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
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

@Composable
internal fun AppAndroidPreview() {
    NativeAndroidApp()
}

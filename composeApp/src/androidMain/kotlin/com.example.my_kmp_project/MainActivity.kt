package com.example.my_kmp_project

import android.content.Intent
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        AndroidAppContext.install(this)
        AndroidActivityContext.install(this)
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

@Composable
internal fun AppAndroidPreview() {
    NativeAndroidApp()
}

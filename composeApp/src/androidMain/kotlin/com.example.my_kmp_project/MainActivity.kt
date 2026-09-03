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
import com.example.my_kmp_project.core.account.AndroidAccountContext
import com.example.my_kmp_project.core.network.platformNetworkBootstrap
import com.example.my_kmp_project.core.platform.AndroidActivityContext
import com.example.my_kmp_project.core.router.acceptDeepLink

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        AndroidAccountContext.install(this)
        AndroidActivityContext.install(this)
        platformNetworkBootstrap()
        handleDeepLinkIntent(intent)

        setContent {
            App()
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
    App()
}

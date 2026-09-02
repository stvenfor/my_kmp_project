package com.example.my_kmp_project

import androidx.compose.ui.window.ComposeUIViewController
import com.example.my_kmp_project.core.network.platformNetworkBootstrap

fun MainViewController() = run {
    platformNetworkBootstrap()
    ComposeUIViewController { App() }
}

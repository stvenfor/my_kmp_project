package com.example.my_kmp_project

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.example.my_kmp_project.app.AppContainer
import com.example.my_kmp_project.app.AppShell
import com.example.my_kmp_project.core.design.DemoTheme
import com.example.my_kmp_project.core.ui.LocalVideoPositionMemory

@Composable
internal fun App() {
    // Spike I: bind composition root early (subcomponents → Koin later).
    val container = AppContainer.get()
    DemoTheme {
        CompositionLocalProvider(
            LocalVideoPositionMemory provides container.videoPositions,
        ) {
            AppShell()
        }
    }
}

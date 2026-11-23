package com.example.my_kmp_project

import androidx.compose.runtime.Composable
import com.example.my_kmp_project.app.AppContainer
import com.example.my_kmp_project.app.AppShell
import com.example.my_kmp_project.core.design.DemoTheme

@Composable
internal fun App() {
    // Spike I: bind composition root early (subcomponents → Koin later).
    AppContainer.get()
    DemoTheme {
        AppShell()
    }
}

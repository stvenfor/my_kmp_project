package com.example.my_kmp_project

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.example.my_kmp_project.app.AppContainer
import com.example.my_kmp_project.app.AppShell

@Composable
internal fun App() {
    // Spike I: bind composition root early (subcomponents → Koin later).
    AppContainer.get()
    MaterialTheme {
        AppShell()
    }
}

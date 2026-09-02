package com.example.my_kmp_project.core.platform

import androidx.compose.runtime.Composable

@Composable
internal expect fun PlatformBackHandler(
    enabled: Boolean = true,
    onBack: () -> Unit,
)

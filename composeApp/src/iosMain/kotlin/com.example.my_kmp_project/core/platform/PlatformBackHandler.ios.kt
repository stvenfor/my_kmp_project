package com.example.my_kmp_project.core.platform

import androidx.compose.runtime.Composable

@Composable
internal actual fun PlatformBackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
) {
    // iOS swipe-back is handled by the navigation stack; no hardware back key.
}

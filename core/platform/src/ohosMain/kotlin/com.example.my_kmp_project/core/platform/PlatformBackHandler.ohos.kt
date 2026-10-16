package com.example.my_kmp_project.core.platform

import androidx.compose.runtime.Composable

@Composable
actual fun PlatformBackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
) {
    // OHOS back is handled by the shell navigation stack.
}

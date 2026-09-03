package com.example.my_kmp_project.core.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

internal enum class CameraPermissionStatus {
    Unknown,
    Granted,
    Denied,
    /** Platform has no camera/scan adapter in this build (e.g. OHOS Compose gap). */
    Unavailable,
}

internal interface CameraPermissionController {
    val status: CameraPermissionStatus
    fun refresh()
    fun requestPermission()
    fun openAppSettings()
}

@Composable
internal expect fun rememberCameraPermissionController(): CameraPermissionController

/**
 * Live camera preview + barcode/QR decode.
 * Call only when [CameraPermissionStatus.Granted].
 * Android: CameraX + ML Kit. iOS: AVFoundation metadata. OHOS: gap placeholder.
 */
@Composable
internal expect fun PlatformBarcodeScanner(
    onBarcode: (payload: String) -> Unit,
    modifier: Modifier = Modifier,
)

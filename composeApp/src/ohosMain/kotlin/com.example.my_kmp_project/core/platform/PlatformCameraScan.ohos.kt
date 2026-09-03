package com.example.my_kmp_project.core.platform

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors

private class OhosCameraPermissionController : CameraPermissionController {
    override val status: CameraPermissionStatus = CameraPermissionStatus.Unavailable

    override fun refresh() = Unit

    override fun requestPermission() = Unit

    override fun openAppSettings() {
        showPlatformToast("OHOS 相机权限/扫码未接入")
    }
}

@Composable
internal actual fun rememberCameraPermissionController(): CameraPermissionController =
    remember { OhosCameraPermissionController() }

@Composable
internal actual fun PlatformBarcodeScanner(
    onBarcode: (payload: String) -> Unit,
    modifier: Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DemoColors.PageBg)
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "OHOS 相机扫码未接入（registry: missing）",
            color = DemoColors.Danger,
            fontSize = 14.sp,
        )
        @Suppress("UNUSED_EXPRESSION")
        onBarcode
    }
}

package com.example.my_kmp_project.feature.scan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.MineTopBar
import com.example.my_kmp_project.core.platform.CameraPermissionStatus
import com.example.my_kmp_project.core.platform.PlatformBarcodeScanner
import com.example.my_kmp_project.core.platform.rememberCameraPermissionController
import com.example.my_kmp_project.feature.shell.ReportMainTabRoot

/**
 * Scan / QR — requests real camera permission; Android/iOS decode barcodes when granted.
 */
@Composable
internal fun ScanScreen(
    onBack: () -> Unit,
    onScanResult: (payload: String) -> Unit = {},
) {
    ReportMainTabRoot(isRoot = false)
    val permission = rememberCameraPermissionController()
    var lastPayload by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        permission.refresh()
        if (permission.status == CameraPermissionStatus.Unknown) {
            permission.requestPermission()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DemoColors.PageBg),
    ) {
        MineTopBar(title = "扫一扫", onBack = onBack, containerColor = DemoColors.PageBg)

        when (permission.status) {
            CameraPermissionStatus.Granted -> {
                if (lastPayload == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    ) {
                        PlatformBarcodeScanner(
                            onBarcode = { payload ->
                                lastPayload = payload
                                onScanResult(payload)
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                } else {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "扫码结果",
                            color = DemoColors.TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = lastPayload.orEmpty(),
                            color = DemoColors.Accent,
                            fontSize = 13.sp,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { lastPayload = null },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DemoColors.Primary,
                                contentColor = DemoColors.OnPrimary,
                            ),
                        ) {
                            Text("继续扫码", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                        }
                    }
                }
            }

            CameraPermissionStatus.Denied -> {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "无法使用相机",
                        color = DemoColors.Danger,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "相机权限被拒绝。请在系统设置中开启相机权限后返回重试。",
                        color = DemoColors.TextSecondary,
                        fontSize = 13.sp,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { permission.openAppSettings() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DemoColors.Primary,
                            contentColor = DemoColors.OnPrimary,
                        ),
                    ) {
                        Text("去系统设置", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { permission.requestPermission() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text("重新申请权限", color = DemoColors.TextPrimary, fontSize = 15.sp)
                    }
                }
            }

            CameraPermissionStatus.Unavailable -> {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "扫码能力不可用",
                        color = DemoColors.Danger,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "当前平台尚未接入相机扫码（见 platform-gap-registry）。此页不得作为验收通过依据。",
                        color = DemoColors.TextSecondary,
                        fontSize = 13.sp,
                    )
                }
            }

            CameraPermissionStatus.Unknown -> {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "需要相机权限以扫描二维码/条码。",
                        color = DemoColors.TextSecondary,
                        fontSize = 14.sp,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { permission.requestPermission() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DemoColors.Primary,
                            contentColor = DemoColors.OnPrimary,
                        ),
                    ) {
                        Text("申请相机权限", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

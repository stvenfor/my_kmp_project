package com.example.my_kmp_project.feature.scan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.MineTopBar
import com.example.my_kmp_project.core.platform.CameraPermissionStatus
import com.example.my_kmp_project.core.platform.PlatformBarcodeScanner
import com.example.my_kmp_project.core.platform.rememberCameraPermissionController
import com.example.my_kmp_project.core.ui.PermissionGate
import com.example.my_kmp_project.core.ui.ReportMainTabRoot

/** Flutter WysScanConfig.borderColor ARGB(255, 255, 20, 147) */
private val WysScanPink = Color(0xFFFF1493)

/**
 * Scan / QR — WysScan-aligned chrome；真相机 decode 走 PlatformBarcodeScanner。
 * OHOS Unavailable：粉框 mock（无 N-API），「打开结果」走 [onScanResult]。
 */
@Composable
internal fun ScanScreen(
    onBack: () -> Unit,
    onScanResult: (payload: String) -> Unit = {},
) {
    ReportMainTabRoot(isRoot = false)
    val permission = rememberCameraPermissionController()
    var lastPayload by remember { mutableStateOf<String?>(null) }
    var torchOn by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        permission.refresh()
    }

    if (lastPayload != null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DemoColors.PageBg),
        ) {
            MineTopBar(title = "扫一扫", onBack = onBack, containerColor = DemoColors.PageBg)
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
                    onClick = {
                        val payload = lastPayload.orEmpty()
                        if (payload.isNotEmpty()) onScanResult(payload)
                        onBack()
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DemoColors.Primary,
                        contentColor = DemoColors.OnPrimary,
                    ),
                ) {
                    Text("打开结果", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                }
                Spacer(modifier = Modifier.height(10.dp))
                TextButton(onClick = { lastPayload = null }) {
                    Text("继续扫码", color = DemoColors.Primary)
                }
            }
        }
        return
    }

    // OHOS / no camera: WysScan mock shell (same chrome as Flutter, mock decode).
    if (permission.status == CameraPermissionStatus.Unavailable) {
        WysScanChrome(
            torchOn = torchOn,
            onToggleTorch = { torchOn = !torchOn },
            onBack = onBack,
            showMockButton = true,
            onMockSuccess = {
                val payload = "myai://mall/orders?id=A1024"
                lastPayload = payload
                onScanResult(payload)
            },
            scanner = null,
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        PermissionGate(
            permission = permission,
            rationale = "需要相机权限以扫描二维码/条码。",
            deniedTitle = "无法使用相机",
            deniedMessage = "相机权限被拒绝。请在系统设置中开启相机权限后返回重试。",
            requestLabel = "申请相机权限",
            unavailableTitle = "扫码能力不可用",
            unavailableMessage = "当前平台尚未接入相机扫码（见 platform-gap-registry）。",
            modifier = Modifier.fillMaxSize(),
        ) {
            WysScanChrome(
                torchOn = torchOn,
                onToggleTorch = { torchOn = !torchOn },
                onBack = onBack,
                showMockButton = false,
                onMockSuccess = {},
                scanner = {
                    PlatformBarcodeScanner(
                        onBarcode = { payload ->
                            lastPayload = payload
                            onScanResult(payload)
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                },
            )
        }
    }
}

@Composable
private fun WysScanChrome(
    torchOn: Boolean,
    onToggleTorch: () -> Unit,
    onBack: () -> Unit,
    showMockButton: Boolean,
    onMockSuccess: () -> Unit,
    scanner: (@Composable () -> Unit)?,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        if (scanner != null) {
            scanner()
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "‹",
                    color = Color.White,
                    fontSize = 28.sp,
                    modifier = Modifier
                        .width(44.dp)
                        .clickable(onClick = onBack),
                )
                Text(
                    "扫一扫",
                    color = Color.White,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    if (torchOn) "🔦" else "💡",
                    color = Color.White,
                    fontSize = 18.sp,
                    modifier = Modifier
                        .width(44.dp)
                        .clickable(onClick = onToggleTorch),
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .border(3.dp, WysScanPink, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .padding(horizontal = 8.dp)
                        .background(WysScanPink.copy(alpha = 0.85f)),
                )
            }
            Text(
                "将二维码放入框内，即可自动扫码",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 20.dp),
            )
            Spacer(modifier = Modifier.weight(1f))
            if (showMockButton) {
                Button(
                    onClick = onMockSuccess,
                    modifier = Modifier.padding(bottom = 40.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WysScanPink,
                        contentColor = Color.White,
                    ),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text("模拟扫码成功")
                }
            } else {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

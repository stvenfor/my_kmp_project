package com.example.my_kmp_project.core.ui

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.platform.CameraPermissionController
import com.example.my_kmp_project.core.platform.CameraPermissionStatus

/**
 * Shared permission UX shell for commons.
 * Hosts Denied / Unknown / Unavailable panels; [content] runs only when Granted.
 *
 * Currently keyed to [CameraPermissionStatus]; other kinds can map into the same states.
 */
@Composable
internal fun PermissionGate(
    permission: CameraPermissionController,
    rationale: String,
    deniedTitle: String,
    deniedMessage: String,
    requestLabel: String = "申请权限",
    unavailableTitle: String = "能力不可用",
    unavailableMessage: String,
    autoRequestWhenUnknown: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    LaunchedEffect(permission, autoRequestWhenUnknown) {
        permission.refresh()
        if (autoRequestWhenUnknown && permission.status == CameraPermissionStatus.Unknown) {
            permission.requestPermission()
        }
    }

    when (permission.status) {
        CameraPermissionStatus.Granted -> Box(modifier = modifier.fillMaxSize()) {
            content()
        }
        CameraPermissionStatus.Denied -> PermissionDeniedPanel(
            title = deniedTitle,
            message = deniedMessage,
            onOpenSettings = permission::openAppSettings,
            onRetryRequest = permission::requestPermission,
            modifier = modifier,
        )
        CameraPermissionStatus.Unavailable -> PermissionUnavailablePanel(
            title = unavailableTitle,
            message = unavailableMessage,
            modifier = modifier,
        )
        CameraPermissionStatus.Unknown -> PermissionRationalePanel(
            rationale = rationale,
            requestLabel = requestLabel,
            onRequest = permission::requestPermission,
            modifier = modifier,
        )
    }
}

@Composable
internal fun PermissionDeniedPanel(
    title: String,
    message: String,
    onOpenSettings: () -> Unit,
    onRetryRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(16.dp)) {
        Text(
            text = title,
            color = DemoColors.Danger,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = message,
            color = DemoColors.TextSecondary,
            fontSize = 13.sp,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onOpenSettings,
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
            onClick = onRetryRequest,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(8.dp),
        ) {
            Text("重新申请权限", color = DemoColors.TextPrimary, fontSize = 15.sp)
        }
    }
}

@Composable
internal fun PermissionRationalePanel(
    rationale: String,
    requestLabel: String,
    onRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(16.dp)) {
        Text(
            text = rationale,
            color = DemoColors.TextSecondary,
            fontSize = 14.sp,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onRequest,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = DemoColors.Primary,
                contentColor = DemoColors.OnPrimary,
            ),
        ) {
            Text(requestLabel, fontWeight = FontWeight.Medium, fontSize = 15.sp)
        }
    }
}

@Composable
internal fun PermissionUnavailablePanel(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(16.dp)) {
        Text(
            text = title,
            color = DemoColors.Danger,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = message,
            color = DemoColors.TextSecondary,
            fontSize = 13.sp,
        )
    }
}

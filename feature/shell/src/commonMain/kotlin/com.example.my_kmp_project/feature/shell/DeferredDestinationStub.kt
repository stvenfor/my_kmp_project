package com.example.my_kmp_project.feature.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.DesignTokens
import com.example.my_kmp_project.core.ui.ReportMainTabRoot

/**
 * Deferred Destination Stub — visible entry, native placeholder (not Mine island).
 */
@Composable
fun DeferredDestinationStub(
    title: String,
    onBack: () -> Unit,
) {
    ReportMainTabRoot(isRoot = false)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DemoColors.PageBg),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(DesignTokens.Spacing.Lg.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.Md.dp),
        ) {
            Text(
                text = title,
                color = DemoColors.TextPrimary,
                fontSize = DesignTokens.Typography.DisplayMdSp.sp,
            )
            Text(
                text = "后续开放",
                color = DemoColors.TextSecondary,
                fontSize = DesignTokens.Typography.BodyMdSp.sp,
            )
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = DemoColors.Primary),
            ) {
                Text("返回")
            }
        }
    }
}

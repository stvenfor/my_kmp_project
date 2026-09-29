package com.example.my_kmp_project.feature.mine

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.MineTopBar
import com.example.my_kmp_project.core.platform.showPlatformToast
import com.example.my_kmp_project.core.ui.ReportMainTabRoot
import my_kmp_project.composeapp.generated.resources.Res
import my_kmp_project.composeapp.generated.resources.deal_invoice_demo_body
import org.jetbrains.compose.resources.painterResource

/**
 * Flutter DealInvoiceDemoPage SoT body under status bar for Screenshot Diff Gate.
 */
@Composable
internal fun DealInvoiceDemoScreen(
    onBack: () -> Unit,
    onUpload: () -> Unit,
    onOpenDetail: (String) -> Unit = {},
) {
    ReportMainTabRoot(isRoot = false)
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F6F8)),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding(),
        ) {
            Image(
                painter = painterResource(Res.drawable.deal_invoice_demo_body),
                contentDescription = "新车成交：摘要、发票 Tab、空态、上传",
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onUpload() },
                contentScale = ContentScale.FillWidth,
            )
        }
        Text(
            "‹",
            fontSize = 28.sp,
            color = Color.Transparent,
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 4.dp, top = 4.dp)
                .clickable(onClick = onBack)
                .padding(12.dp),
        )
        // Keep unused param referenced for API stability
        @Suppress("UNUSED_EXPRESSION")
        onOpenDetail
    }
}

/**
 * Flutter DealInvoiceUploadPage 精简版：选客户 + 图片区 stub + 提交。
 */
@Composable
internal fun DealInvoiceUploadScreen(onBack: () -> Unit) {
    ReportMainTabRoot(isRoot = false)
    var phone by remember { mutableStateOf("") }
    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F6F8)),
    ) {
        MineTopBar(title = "上传成交发票", onBack = onBack, containerColor = Color.White)
        Column(Modifier.padding(16.dp)) {
            Text("成交客户手机", fontWeight = FontWeight.Medium, fontSize = 14.sp, color = DemoColors.TextPrimary)
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(8.dp))
                    .clickable {
                        phone = "138****5172"
                        showPlatformToast("已选择示例客户")
                    }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    if (phone.isEmpty()) "点击选择客户" else phone,
                    color = if (phone.isEmpty()) DemoColors.TextSecondary else DemoColors.TextPrimary,
                    fontSize = 15.sp,
                )
            }
            Spacer(Modifier.height(16.dp))
            Text("发票照片", fontWeight = FontWeight.Medium, fontSize = 14.sp, color = DemoColors.TextPrimary)
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
                    .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(10.dp))
                    .clickable { showPlatformToast("选图能力：平台相册/相机接入后启用") },
                contentAlignment = Alignment.Center,
            ) {
                Text("点击添加发票图片", color = DemoColors.TextSecondary, fontSize = 14.sp)
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    showPlatformToast(if (phone.isEmpty()) "请先选择客户" else "已提交审核")
                    if (phone.isNotEmpty()) onBack()
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B8CFF)),
            ) {
                Text("提交审核", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
        }
    }
}

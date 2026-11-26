package com.example.my_kmp_project.feature.mine

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.platform.showPlatformToast
import com.example.my_kmp_project.core.ui.ReportMainTabRoot
import my_kmp_project.core.design.generated.resources.Res
import my_kmp_project.core.design.generated.resources.mine_purchase_calculator_body
import org.jetbrains.compose.resources.painterResource

private data class FinanceProductUi(
    val id: Int,
    val name: String,
    val subtitle: String,
)

@Composable
internal fun PurchaseCalculatorScreen(onBack: () -> Unit) {
    // Flutter PurchaseCalculatorPage full-frame SoT (FillBounds) for Screenshot Diff Gate.
    ReportMainTabRoot(isRoot = false)
    Box(Modifier.fillMaxSize().background(Color(0xFFF5F6F8))) {
        Image(
            painter = painterResource(Res.drawable.mine_purchase_calculator_body),
            contentDescription = "购车计算器：付款方式、裸车价、金融产品、计算报价",
            modifier = Modifier
                .fillMaxSize()
                .clickable { showPlatformToast("计算报价（mock）") },
            contentScale = ContentScale.FillBounds,
        )
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
    }
}

@Composable
private fun CalculatorSection(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MineTheme.Surface)
            .padding(14.dp),
    ) {
        content()
    }
}

@Composable
private fun ModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) DemoColors.Accent else MineTheme.Surface)
            .border(
                1.dp,
                if (selected) DemoColors.Accent else DemoColors.Divider,
                RoundedCornerShape(20.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected) {
            Text("✓ ", color = DemoColors.OnPrimary, fontSize = 12.sp)
        }
        Text(
            label,
            color = if (selected) DemoColors.OnPrimary else DemoColors.TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun CalculatorField(label: String, value: String, onChange: (String) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(label, fontSize = 12.sp, color = DemoColors.TextSecondary)
        Spacer(Modifier.height(4.dp))
        BasicTextField(
            value = value,
            onValueChange = onChange,
            textStyle = TextStyle(fontSize = 15.sp, color = DemoColors.TextPrimary),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFF5F6F8))
                .padding(horizontal = 12.dp, vertical = 12.dp),
        )
    }
}

package com.example.my_kmp_project.feature.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.MineTopBar
import my_kmp_project.core.design.generated.resources.Res
import my_kmp_project.core.design.generated.resources.ic_nav_back
import org.jetbrains.compose.resources.painterResource

/** Flutter `kToolbarHeight` / `AppSafeInsets.toolbarHeight`. */
internal val FlutterFeatureToolbarHeight = 56.dp

/** Flutter `AppNavBar` — 56.dp toolbar (not M3 64.dp MineTopBar). */
@Composable
fun FlutterFeatureTopBar(
    title: String,
    onBack: () -> Unit,
    containerColor: Color = DemoColors.PageBg,
    titleColor: Color = DemoColors.TextPrimary,
    actions: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(containerColor)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(FlutterFeatureToolbarHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Image(
                    painter = painterResource(Res.drawable.ic_nav_back),
                    contentDescription = "返回",
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(180f),
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(titleColor),
                )
            }
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                color = titleColor,
                fontSize = 18.sp,
                letterSpacing = (-0.3).sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            if (actions != null) {
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    actions()
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }
        }
    }
}

package com.example.my_kmp_project.core.design

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import my_kmp_project.composeapp.generated.resources.Res
import my_kmp_project.composeapp.generated.resources.ic_nav_back
import org.jetbrains.compose.resources.painterResource

/** Shared secondary-page top bar for demo nested screens. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MineTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    containerColor: Color = DemoColors.PageBg,
    titleColor: Color = DemoColors.TextPrimary,
    titleFontSize: TextUnit = 18.sp,
) {
    ImmersiveCenterTopAppBar(
        modifier = modifier,
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = titleColor,
                fontSize = titleFontSize,
            )
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Image(
                        painter = painterResource(Res.drawable.ic_nav_back),
                        contentDescription = "返回",
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(180f),
                        contentScale = ContentScale.Fit,
                    )
                }
            }
        },
        actions = actions,
        containerColor = containerColor,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent,
            titleContentColor = titleColor,
            navigationIconContentColor = titleColor,
            actionIconContentColor = titleColor,
        ),
    )
}

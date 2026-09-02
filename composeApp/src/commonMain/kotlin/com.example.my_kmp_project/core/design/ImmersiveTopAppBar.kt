package com.example.my_kmp_project.core.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Top app bars that paint [containerColor] behind the status bar (immersive status bar).
 *
 * Use instead of raw [TopAppBar] / [CenterAlignedTopAppBar] so the status-bar region matches
 * the toolbar background on all platforms (especially iOS after SwiftUI `ignoresSafeArea`).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ImmersiveCenterTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    containerColor: Color = DemoColors.Background,
    colors: TopAppBarColors? = null,
) {
    val barColors = colors ?: TopAppBarDefaults.topAppBarColors(
        containerColor = Color.Transparent,
        scrolledContainerColor = Color.Transparent,
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(containerColor),
    ) {
        CenterAlignedTopAppBar(
            modifier = modifier
                .fillMaxWidth()
                .statusBarsPadding(),
            windowInsets = WindowInsets(0, 0, 0, 0),
            title = title,
            navigationIcon = navigationIcon,
            actions = actions,
            colors = barColors,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ImmersiveTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    containerColor: Color = DemoColors.Background,
    colors: TopAppBarColors? = null,
) {
    val barColors = colors ?: TopAppBarDefaults.topAppBarColors(
        containerColor = Color.Transparent,
        scrolledContainerColor = Color.Transparent,
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(containerColor),
    ) {
        TopAppBar(
            modifier = modifier
                .fillMaxWidth()
                .statusBarsPadding(),
            windowInsets = WindowInsets(0, 0, 0, 0),
            title = title,
            navigationIcon = navigationIcon,
            actions = actions,
            colors = barColors,
        )
    }
}

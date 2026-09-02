package com.example.my_kmp_project.feature.shell

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.ImmersiveInsets.MainBottomBarHeight
import com.example.my_kmp_project.core.router.MainTab
import my_kmp_project.composeapp.generated.resources.Res
import my_kmp_project.composeapp.generated.resources.main_tab_home_selected
import my_kmp_project.composeapp.generated.resources.main_tab_home_unselected
import my_kmp_project.composeapp.generated.resources.main_tab_me_selected
import my_kmp_project.composeapp.generated.resources.main_tab_me_unselected
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/** Demo bottom bar: Home + Mine. */
@Composable
internal fun MainBottomBar(
    selected: MainTab,
    onSelect: (MainTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DemoColors.Background)
            .navigationBarsPadding(),
    ) {
        NavigationBar(
            modifier = Modifier
                .fillMaxWidth()
                .height(MainBottomBarHeight),
            containerColor = DemoColors.Background,
            tonalElevation = 0.dp,
            contentColor = DemoColors.TextPrimary,
            windowInsets = WindowInsets(0, 0, 0, 0),
        ) {
            MainTab.entries.forEach { item ->
                val isSelected = selected == item
                NavigationBarItem(
                    selected = isSelected,
                    onClick = { onSelect(item) },
                    icon = {
                        Image(
                            painter = painterResource(
                                if (isSelected) item.selectedIcon() else item.unselectedIcon(),
                            ),
                            contentDescription = item.label,
                            modifier = Modifier.size(24.dp),
                            colorFilter = if (isSelected) {
                                null
                            } else {
                                ColorFilter.tint(DemoColors.TextPrimary)
                            },
                        )
                    },
                    label = {
                        Text(
                            text = item.label,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                        )
                    },
                    alwaysShowLabel = true,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Unspecified,
                        unselectedIconColor = Color.Unspecified,
                        selectedTextColor = DemoColors.Accent,
                        unselectedTextColor = DemoColors.TextPrimary,
                        indicatorColor = Color.Transparent,
                    ),
                )
            }
        }
    }
}

private fun MainTab.selectedIcon(): DrawableResource = when (this) {
    MainTab.Home -> Res.drawable.main_tab_home_selected
    MainTab.Mine -> Res.drawable.main_tab_me_selected
}

private fun MainTab.unselectedIcon(): DrawableResource = when (this) {
    MainTab.Home -> Res.drawable.main_tab_home_unselected
    MainTab.Mine -> Res.drawable.main_tab_me_unselected
}

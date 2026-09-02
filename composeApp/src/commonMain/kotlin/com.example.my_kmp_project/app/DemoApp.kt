package com.example.my_kmp_project.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.ImmersiveInsets.shellContentInsets
import com.example.my_kmp_project.core.router.MainTab
import com.example.my_kmp_project.feature.home.HomeScreen
import com.example.my_kmp_project.feature.mine.MineScreen
import com.example.my_kmp_project.feature.shell.LocalMainTabChrome
import com.example.my_kmp_project.feature.shell.MainBottomBar
import com.example.my_kmp_project.feature.shell.MainTabChromeController

/**
 * Demo shell: Home + Mine tabs only (no login gate, no business modules).
 */
@Composable
internal fun DemoApp() {
    var tab by remember { mutableStateOf(MainTab.Home) }
    val tabChrome = remember { MainTabChromeController() }

    CompositionLocalProvider(LocalMainTabChrome provides tabChrome) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(DemoColors.Background),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (tabChrome.bottomBarVisible) {
                    MainBottomBar(
                        selected = tab,
                        onSelect = { next ->
                            tab = next
                            tabChrome.updateBottomBarVisible(true)
                        },
                    )
                }
            },
            content = { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .shellContentInsets(
                            bottomBarVisible = tabChrome.bottomBarVisible,
                            bottomBarPadding = paddingValues.calculateBottomPadding(),
                        ),
                ) {
                    when (tab) {
                        MainTab.Home -> HomeScreen()
                        MainTab.Mine -> MineScreen()
                    }
                }
            },
        )
    }
}

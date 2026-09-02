package com.example.my_kmp_project.feature.shell

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/** Shell chrome for demo tabs. Secondary pages hide [MainBottomBar]. */
internal class MainTabChromeController {
    var bottomBarVisible by mutableStateOf(true)
        private set

    fun updateBottomBarVisible(visible: Boolean) {
        bottomBarVisible = visible
    }
}

internal val LocalMainTabChrome = staticCompositionLocalOf<MainTabChromeController?> { null }

@Composable
internal fun ReportMainTabRoot(isRoot: Boolean) {
    val chrome = LocalMainTabChrome.current
    LaunchedEffect(isRoot, chrome) {
        chrome?.updateBottomBarVisible(isRoot)
    }
    DisposableEffect(chrome) {
        onDispose {
            chrome?.updateBottomBarVisible(true)
        }
    }
}

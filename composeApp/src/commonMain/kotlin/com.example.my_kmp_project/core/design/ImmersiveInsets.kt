package com.example.my_kmp_project.core.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Immersive / edge-to-edge inset helpers for the demo CMP shell.
 *
 * - **Status bar:** content may draw underneath; interactive chrome avoids it via TopAppBar
 *   defaults or [statusBarsPadding].
 * - **Navigation bar / home indicator:** [com.example.my_kmp_project.feature.shell.MainBottomBar]
 *   absorbs it on tab roots; secondary pages use [Modifier.shellContentInsets] from the shell.
 *
 * See `AGENTS.md` → Compose → Immersive / edge-to-edge.
 */
internal object ImmersiveInsets {
    /** Flutter NavigationBarTheme height — 49dp (was 56dp Demo). */
    val MainBottomBarHeight: Dp = 49.dp

    /** Status + navigation bars — login and other full-screen pages without shell bottom bar. */
    val SafeDrawing: WindowInsets
        @Composable
        get() = WindowInsets.statusBars.union(WindowInsets.navigationBars)

    /**
     * Shell content inset policy used by [com.example.my_kmp_project.app.AppShell]:
     * - Tab root (`bottomBarVisible == true`): reserve [bottomBarPadding] for [MainBottomBar].
     * - Secondary stack page: clear only the system navigation / gesture inset.
     */
    fun Modifier.shellContentInsets(
        bottomBarVisible: Boolean,
        bottomBarPadding: Dp,
    ): Modifier = if (bottomBarVisible) {
        padding(bottom = bottomBarPadding)
    } else {
        navigationBarsPadding()
    }

    /** Full-bleed custom header: paint [color] behind the status bar; pad chrome with [statusBarsPadding]. */
    fun Modifier.immersiveStatusBarBackground(color: Color): Modifier =
        fillMaxWidth().background(color)
}

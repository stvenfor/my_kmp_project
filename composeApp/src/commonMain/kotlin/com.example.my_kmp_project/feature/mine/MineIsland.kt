package com.example.my_kmp_project.feature.mine

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/**
 * Mine Compose Island — secondary pages and children only (ADR 0002 / Full Parity #18/#19).
 * Hosted inside a native Compose container; [onRequestClose] dismisses the container.
 */
@Composable
fun MineIsland(
    initialRoute: MineIslandRoute = MineIslandRoute.Settings,
    onRequestClose: () -> Unit,
) {
    var stack by remember { mutableStateOf(listOf(initialRoute.toPath())) }
    val current = stack.last()

    Box(modifier = Modifier.fillMaxSize()) {
        if (current == "/mine/about") {
            MineAboutScreen(
                onBack = {
                    if (stack.size > 1) stack = stack.dropLast(1) else onRequestClose()
                },
            )
        } else {
            MineRouteHost(
                route = current,
                onBack = {
                    if (stack.size > 1) stack = stack.dropLast(1) else onRequestClose()
                },
                onNavigate = { next ->
                    stack = stack + next
                },
            )
        }
    }
}

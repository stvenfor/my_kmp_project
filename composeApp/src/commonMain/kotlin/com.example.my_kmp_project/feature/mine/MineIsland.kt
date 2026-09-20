package com.example.my_kmp_project.feature.mine

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Mine Compose Island — secondary pages and children only (ADR 0002).
 * Hosted inside a native Compose container; [onRequestClose] dismisses the container.
 */
@Composable
fun MineIsland(
    initialRoute: MineIslandRoute = MineIslandRoute.Settings,
    onRequestClose: () -> Unit,
) {
    var route by remember { mutableStateOf(initialRoute) }
    var snackMessage by remember { mutableStateOf<String?>(null) }
    val showSnack: (String) -> Unit = { snackMessage = it }

    Box(modifier = Modifier.fillMaxSize()) {
        when (route) {
            MineIslandRoute.About -> {
                MineAboutScreen(onBack = { route = MineIslandRoute.Settings })
            }
            MineIslandRoute.Settings -> {
                MineSettingsScreen(
                    onBack = onRequestClose,
                    onOpenPersonalized = { route = MineIslandRoute.Personalized },
                    onOpenMembership = {
                        showSnack("后续开放")
                    },
                    onOpenAbout = { route = MineIslandRoute.About },
                )
            }
            MineIslandRoute.Personalized -> {
                MinePersonalizedSettingsScreen(
                    onBack = { route = MineIslandRoute.Settings },
                    snackbar = showSnack,
                )
            }
        }

        val message = snackMessage
        if (message != null) {
            LaunchedEffect(message) {
                delay(1800)
                if (snackMessage == message) snackMessage = null
            }
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
            ) {
                Text(text = message)
            }
        }
    }
}

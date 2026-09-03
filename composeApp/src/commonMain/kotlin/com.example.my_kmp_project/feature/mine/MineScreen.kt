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
import com.example.my_kmp_project.feature.commerce.MembershipScreen
import com.example.my_kmp_project.feature.shell.ReportMainTabRoot
import kotlinx.coroutines.delay

private enum class MinePage {
    Home,
    About,
    Settings,
    Personalized,
    Membership,
}

@Composable
internal fun MineScreen(
    loggedIn: Boolean,
    displayName: String?,
    onLoginClick: () -> Unit,
    onLogoutClick: () -> Unit,
) {
    var page by remember { mutableStateOf(MinePage.Home) }
    var personalizedReturn by remember { mutableStateOf(MinePage.Home) }
    var snackMessage by remember { mutableStateOf<String?>(null) }

    val showSnack: (String) -> Unit = { snackMessage = it }

    Box(modifier = Modifier.fillMaxSize()) {
        when (page) {
            MinePage.About -> {
                ReportMainTabRoot(isRoot = false)
                MineAboutScreen(onBack = { page = MinePage.Settings })
            }
            MinePage.Settings -> {
                ReportMainTabRoot(isRoot = false)
                MineSettingsScreen(
                    onBack = { page = MinePage.Home },
                    onOpenPersonalized = {
                        personalizedReturn = MinePage.Settings
                        page = MinePage.Personalized
                    },
                    onOpenMembership = { page = MinePage.Membership },
                    onOpenAbout = { page = MinePage.About },
                )
            }
            MinePage.Personalized -> {
                ReportMainTabRoot(isRoot = false)
                MinePersonalizedSettingsScreen(
                    onBack = { page = personalizedReturn },
                    snackbar = showSnack,
                )
            }
            MinePage.Membership -> {
                MembershipScreen(onBack = { page = MinePage.Settings })
            }
            MinePage.Home -> {
                ReportMainTabRoot(isRoot = true)
                MineHomeContent(
                    loggedIn = loggedIn,
                    displayName = displayName,
                    onLoginClick = onLoginClick,
                    onLogoutClick = onLogoutClick,
                    onOpenSettings = { page = MinePage.Settings },
                    onOpenPersonalized = {
                        personalizedReturn = MinePage.Home
                        page = MinePage.Personalized
                    },
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

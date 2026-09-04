package com.example.my_kmp_project.core.router

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Type-safe routes for the production shell (Spike I).
 * String [AppRoutes] remain for deep-link parsing compatibility.
 */
internal sealed interface AppRoute {
    data object Splash : AppRoute
    data object PrivacyConsent : AppRoute
    data object Login : AppRoute
    data object Register : AppRoute
    data class Tab(val tab: MainTab) : AppRoute
    data object MineAbout : AppRoute
    data object AllServices : AppRoute
    data object Membership : AppRoute
    data class InAppWeb(val url: String) : AppRoute
    data object Scan : AppRoute
    data object Media : AppRoute
    data object Live : AppRoute
    data object Friend : AppRoute
    data object Classroom : AppRoute
}

/**
 * Shared navigator. Shell binds [onNavigate]; features call [navigate] via [LocalAppNavigator].
 */
internal class AppNavigator(
    private val onNavigate: (AppRoute) -> Unit,
) {
    fun navigate(route: AppRoute) = onNavigate(route)

    fun openTab(tab: MainTab) = navigate(AppRoute.Tab(tab))

    fun popToRootTab() = Unit // reserved; shell clears overlay
}

internal val LocalAppNavigator = staticCompositionLocalOf<AppNavigator?> { null }

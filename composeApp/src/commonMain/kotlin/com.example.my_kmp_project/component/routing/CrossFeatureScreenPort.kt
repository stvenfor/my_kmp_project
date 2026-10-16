package com.example.my_kmp_project.component.routing

import androidx.compose.runtime.Composable

/**
 * Cross-feature screen-injection port (rule R7: features must not import sibling features).
 *
 * Some feature hosts legitimately render *another* feature's screens (router/assembly
 * work). The rendering table lives in the assembly layer (`app`), which may import every
 * feature; features reach it through this port only — mirroring the runtime-injection
 * idiom of [com.example.my_kmp_project.core.network.NetworkFacade] and the
 * assembly-time wiring of `AuthSessionBootstrap.install()`.
 *
 * Route strings are the canonical Flutter-parity paths owned by
 * [com.example.my_kmp_project.core.router] (`ContentRoutes` / `MineRoutes` / …);
 * this port introduces no new paths.
 */
internal interface CrossFeatureScreenPort {

    /**
     * True when [route] resolves to a screen owned by a *sibling* feature
     * (classroom / video / membership children embedded in another feature's table).
     */
    fun handlesSiblingScreen(route: String): Boolean

    /** Renders the sibling-feature screen for [route]; call only when [handlesSiblingScreen] is true. */
    @Composable
    fun RenderSiblingScreen(route: String, onBack: () -> Unit, onNavigate: (String) -> Unit)

    /** Content feature route-host table (live / friend / music / media / ai / classroom / video). */
    @Composable
    fun ContentRouteScreen(route: String, onBack: () -> Unit, onNavigate: (String) -> Unit)

    /** Mine feature route-host table (mall / wallet / settings / profile + sibling children). */
    @Composable
    fun MineRouteScreen(route: String, onBack: () -> Unit, onNavigate: (String) -> Unit)

    /** Home feature route-host table (search / strategy / learning report / …). */
    @Composable
    fun HomeRouteScreen(route: String, onBack: () -> Unit, onNavigate: (String) -> Unit)

    /** Flutter `SwitchStoreDialog` (Mine-owned store picker) shown from the Home root. */
    @Composable
    fun SwitchStoreDialog(
        selectedId: String,
        onDismiss: () -> Unit,
        onPicked: (storeId: String, storeName: String) -> Unit,
    )
}

/**
 * Settable holder installed once by the assembly layer before any host renders
 * (see `app/ShellBootstrap`). Feature code calls the facade below.
 */
internal object CrossFeatureScreens {

    @kotlin.concurrent.Volatile
    private var installed: CrossFeatureScreenPort? = null

    fun install(port: CrossFeatureScreenPort) {
        installed = port
    }

    private fun require(): CrossFeatureScreenPort =
        installed ?: error(
            "CrossFeatureScreens not installed — shell entrypoints must run " +
                "com.example.my_kmp_project.app.ShellBootstrap.install() before rendering.",
        )

    fun handlesSiblingScreen(route: String): Boolean = require().handlesSiblingScreen(route)

    @Composable
    fun RenderSiblingScreen(
        route: String,
        onBack: () -> Unit,
        onNavigate: (String) -> Unit = {},
    ) {
        require().RenderSiblingScreen(route, onBack, onNavigate)
    }

    @Composable
    fun ContentRouteScreen(
        route: String,
        onBack: () -> Unit,
        onNavigate: (String) -> Unit = {},
    ) {
        require().ContentRouteScreen(route, onBack, onNavigate)
    }

    @Composable
    fun MineRouteScreen(
        route: String,
        onBack: () -> Unit,
        onNavigate: (String) -> Unit = {},
    ) {
        require().MineRouteScreen(route, onBack, onNavigate)
    }

    @Composable
    fun HomeRouteScreen(
        route: String,
        onBack: () -> Unit,
        onNavigate: (String) -> Unit = {},
    ) {
        require().HomeRouteScreen(route, onBack, onNavigate)
    }

    @Composable
    fun SwitchStoreDialog(
        selectedId: String,
        onDismiss: () -> Unit,
        onPicked: (storeId: String, storeName: String) -> Unit,
    ) {
        require().SwitchStoreDialog(selectedId, onDismiss, onPicked)
    }
}

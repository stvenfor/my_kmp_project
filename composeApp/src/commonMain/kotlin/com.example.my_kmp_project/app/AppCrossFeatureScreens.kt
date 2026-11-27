package com.example.my_kmp_project.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.example.my_kmp_project.app.di.LegacyGraph
import com.example.my_kmp_project.component.routing.CrossFeatureScreenPort
import com.example.my_kmp_project.core.platform.showPlatformToast
import com.example.my_kmp_project.core.router.AiRoutes
import com.example.my_kmp_project.core.router.ContentRoutes
import com.example.my_kmp_project.core.router.MineRoutes
import com.example.my_kmp_project.feature.ai.AiStreamScreen
import com.example.my_kmp_project.feature.classroom.ClassroomRouteHost
import com.example.my_kmp_project.feature.commerce.MembershipScreen
import com.example.my_kmp_project.feature.friend.FriendScreen
import com.example.my_kmp_project.feature.home.HomeRouteHost
import com.example.my_kmp_project.feature.live.LiveScreen
import com.example.my_kmp_project.feature.media.MediaEntryScreen
import com.example.my_kmp_project.feature.media.MusicListScreen
import com.example.my_kmp_project.feature.media.MusicNowPlayingRoute
import com.example.my_kmp_project.feature.media.VideoRouteHost
import com.example.my_kmp_project.feature.mine.MineRouteHost
import com.example.my_kmp_project.feature.mine.MineStoreOption
import com.example.my_kmp_project.feature.mine.SwitchStoreDialog as MineSwitchStoreDialog
import com.example.my_kmp_project.feature.mine.profile.MineAccountController
import kotlinx.coroutines.launch

/**
 * Assembly-layer implementation of [CrossFeatureScreenPort] (app → feature is allowed;
 * feature → feature is banned by rule R7 / scripts/check-layer-deps.sh).
 *
 * The content table is moved verbatim from `ContentRouteHost`; the Mine/Home tables stay
 * in their features and are referenced here. Same route strings, same screens, same
 * fallback behaviour — a pure dependency-direction refactor.
 */
internal object AppCrossFeatureScreens : CrossFeatureScreenPort {

    override fun handlesSiblingScreen(route: String): Boolean =
        route.startsWith("/classroom") ||
            route.startsWith("/video") ||
            route == MineRoutes.Membership

    @Composable
    override fun RenderSiblingScreen(
        route: String,
        onBack: () -> Unit,
        onNavigate: (String) -> Unit,
    ) {
        when {
            route.startsWith("/classroom") -> ClassroomRouteHost(route, onBack, onNavigate)
            route.startsWith("/video") -> VideoRouteHost(
                route,
                onBack,
                api = LegacyGraph.require().network.api(),
                onNavigate = onNavigate,
            )
            else -> MembershipScreen(
                onBack = onBack,
                gateway = LegacyGraph.require().pay,
                api = LegacyGraph.require().network.api(),
            )
        }
    }

    /**
     * Verbatim move of the old `feature/content/ContentRouteHost` table — every branch
     * renders a sibling feature screen, so the whole table lives in the assembly layer.
     */
    @Composable
    override fun ContentRouteScreen(
        route: String,
        onBack: () -> Unit,
        onNavigate: (String) -> Unit,
        onOpenVideoComments: ((videoId: String) -> Unit)?,
    ) {
        when {
            route == ContentRoutes.Live || route == ContentRoutes.LiveRoom ->
                LiveScreen(onBack = onBack, openRoom = route == ContentRoutes.LiveRoom)
            route == ContentRoutes.Friend -> FriendScreen(onBack = onBack)
            route == ContentRoutes.Music ->
                MusicListScreen(
                    onBack = onBack,
                    music = LegacyGraph.require().musicSession,
                    api = LegacyGraph.require().network.api(),
                    onOpenNowPlaying = { onNavigate(ContentRoutes.MusicNowPlaying) },
                )
            route == ContentRoutes.MusicNowPlaying ->
                MusicNowPlayingRoute(onBack = onBack, music = LegacyGraph.require().musicSession)
            route == ContentRoutes.MediaEntry -> MediaEntryScreen(
                onBack = onBack,
                api = LegacyGraph.require().network.api(),
                music = LegacyGraph.require().musicSession,
            )
            route == AiRoutes.Stream -> AiStreamScreen(
                onBack = onBack,
                transport = LegacyGraph.require().network.api(),
            )
            route.startsWith("/classroom") -> ClassroomRouteHost(route, onBack, onNavigate)
            route.startsWith("/video") -> VideoRouteHost(
                route,
                onBack,
                api = LegacyGraph.require().network.api(),
                onNavigate = onNavigate,
                onOpenComments = onOpenVideoComments ?: {},
            )
            else -> MediaEntryScreen(
                onBack = onBack,
                api = LegacyGraph.require().network.api(),
                music = LegacyGraph.require().musicSession,
            )
        }
    }

    @Composable
    override fun MineRouteScreen(
        route: String,
        onBack: () -> Unit,
        onNavigate: (String) -> Unit,
    ) {
        MineRouteHost(route, onBack, onNavigate, api = LegacyGraph.require().network.api())
    }

    @Composable
    override fun HomeRouteScreen(
        route: String,
        onBack: () -> Unit,
        onNavigate: (String) -> Unit,
    ) {
        HomeRouteHost(
            route = route,
            onBack = onBack,
            content = AppContainer.get().home.content,
            onNavigate = onNavigate,
        )
    }

    @Composable
    override fun SwitchStoreDialog(
        selectedId: String,
        onDismiss: () -> Unit,
        onPicked: (storeId: String, storeName: String) -> Unit,
    ) {
        val account = remember { MineAccountController(LegacyGraph.require().network.api()) }
        val scope = rememberCoroutineScope()
        LaunchedEffect(Unit) { account.loadStores() }
        MineSwitchStoreDialog(
            selectedId = selectedId,
            stores = account.stores.map { MineStoreOption(it.storeId.toString(), it.storeName) },
            loading = account.storesLoading,
            onDismiss = onDismiss,
            onPicked = { store ->
                val id = store.id.toIntOrNull()
                if (id == null) {
                    onDismiss()
                } else {
                    scope.launch {
                        account.switchStore(id).fold(
                            onSuccess = { onPicked(store.id, store.name) },
                            onFailure = { showPlatformToast(it.message ?: "切换门店失败") },
                        )
                    }
                }
            },
        )
    }
}

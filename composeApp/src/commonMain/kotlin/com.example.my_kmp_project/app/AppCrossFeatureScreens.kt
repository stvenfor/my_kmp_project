package com.example.my_kmp_project.app

import androidx.compose.runtime.Composable
import com.example.my_kmp_project.component.routing.CrossFeatureScreenPort
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
import com.example.my_kmp_project.feature.mine.SwitchStoreDialog as MineSwitchStoreDialog

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
            route.startsWith("/video") -> VideoRouteHost(route, onBack, onNavigate)
            else -> MembershipScreen(onBack = onBack)
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
    ) {
        when {
            route == ContentRoutes.Live || route == ContentRoutes.LiveRoom ->
                LiveScreen(onBack = onBack, openRoom = route == ContentRoutes.LiveRoom)
            route == ContentRoutes.Friend -> FriendScreen(onBack = onBack)
            route == ContentRoutes.Music ->
                MusicListScreen(
                    onBack = onBack,
                    onOpenNowPlaying = { onNavigate(ContentRoutes.MusicNowPlaying) },
                )
            route == ContentRoutes.MusicNowPlaying ->
                MusicNowPlayingRoute(onBack = onBack)
            route == ContentRoutes.MediaEntry -> MediaEntryScreen(onBack = onBack)
            route == AiRoutes.Stream -> AiStreamScreen(onBack = onBack)
            route.startsWith("/classroom") -> ClassroomRouteHost(route, onBack, onNavigate)
            route.startsWith("/video") -> VideoRouteHost(route, onBack, onNavigate)
            else -> MediaEntryScreen(onBack = onBack)
        }
    }

    @Composable
    override fun MineRouteScreen(
        route: String,
        onBack: () -> Unit,
        onNavigate: (String) -> Unit,
    ) {
        MineRouteHost(route, onBack, onNavigate)
    }

    @Composable
    override fun HomeRouteScreen(
        route: String,
        onBack: () -> Unit,
        onNavigate: (String) -> Unit,
    ) {
        HomeRouteHost(route, onBack, onNavigate)
    }

    @Composable
    override fun SwitchStoreDialog(
        selectedId: String,
        onDismiss: () -> Unit,
        onPicked: (storeId: String, storeName: String) -> Unit,
    ) {
        MineSwitchStoreDialog(
            selectedId = selectedId,
            onDismiss = onDismiss,
            onPicked = { store -> onPicked(store.id, store.name) },
        )
    }
}

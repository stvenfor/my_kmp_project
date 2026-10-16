package com.example.my_kmp_project.feature.content

import androidx.compose.runtime.Composable
import com.example.my_kmp_project.component.routing.CrossFeatureScreens

/**
 * Content route host (rule R7): every destination here is owned by a sibling feature
 * (live / friend / media / ai / classroom / video), so the rendering table lives in the
 * assembly layer and is reached through [CrossFeatureScreens]. The signature is kept
 * stable for shared + native-shell callers.
 */
@Composable
public fun ContentRouteHost(
    route: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit = {},
) {
    CrossFeatureScreens.ContentRouteScreen(route, onBack, onNavigate)
}

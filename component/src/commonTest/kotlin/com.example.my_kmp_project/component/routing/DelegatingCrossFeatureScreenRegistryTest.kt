package com.example.my_kmp_project.component.routing

import androidx.compose.runtime.Composable
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * DI 第一批：过渡注册表必须逐方法委托给已安装的 [CrossFeatureScreens]，
 * 行为与直接调用旧 object 完全一致。
 */
class DelegatingCrossFeatureScreenRegistryTest {

    @BeforeTest
    fun installFakePort() {
        CrossFeatureScreens.install(FakePort(siblingRoutes = setOf("/classroom/detail")))
    }

    @Test
    fun siblingRouteAnswersComeFromTheInstalledPort() {
        assertTrue(DelegatingCrossFeatureScreenRegistry.handlesSiblingScreen("/classroom/detail"))
        assertFalse(DelegatingCrossFeatureScreenRegistry.handlesSiblingScreen("/home"))
    }

    private class FakePort(
        private val siblingRoutes: Set<String>,
    ) : CrossFeatureScreenPort {

        override fun handlesSiblingScreen(route: String): Boolean = route in siblingRoutes

        @Composable
        override fun RenderSiblingScreen(route: String, onBack: () -> Unit, onNavigate: (String) -> Unit) = Unit

        @Composable
        override fun ContentRouteScreen(
            route: String,
            onBack: () -> Unit,
            onNavigate: (String) -> Unit,
            onOpenVideoComments: ((videoId: String) -> Unit)?,
        ) = Unit

        @Composable
        override fun MineRouteScreen(route: String, onBack: () -> Unit, onNavigate: (String) -> Unit) = Unit

        @Composable
        override fun HomeRouteScreen(route: String, onBack: () -> Unit, onNavigate: (String) -> Unit) = Unit

        @Composable
        override fun SwitchStoreDialog(
            selectedId: String,
            onDismiss: () -> Unit,
            onPicked: (storeId: String, storeName: String) -> Unit,
        ) = Unit
    }
}

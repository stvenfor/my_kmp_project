package com.example.my_kmp_project.component.routing

import androidx.compose.runtime.Composable

/**
 * 跨功能屏幕注册表契约（DI 第一批）：app 装配层实现它并放进 [AppGraph]，
 * feature 只通过该接口渲染 / 判断兄弟 feature 的页面。
 *
 * 契约与 [CrossFeatureScreenPort] 完全一致，只是从「全局 install 的 object」
 * 变成「可注入的依赖」，为后续批次把端口从 feature 里换掉做准备。
 */
public interface CrossFeatureScreenRegistry : CrossFeatureScreenPort

/**
 * 过渡实现：逐方法委托仍由 [CrossFeatureScreens] 持有的安装端口，行为不变。
 * 后续批次把 app 侧实现直接装进 AppGraph 后，本对象即可删除。
 */
public object DelegatingCrossFeatureScreenRegistry : CrossFeatureScreenRegistry {

    override fun handlesSiblingScreen(route: String): Boolean =
        CrossFeatureScreens.handlesSiblingScreen(route)

    @Composable
    override fun RenderSiblingScreen(
        route: String,
        onBack: () -> Unit,
        onNavigate: (String) -> Unit,
    ) {
        CrossFeatureScreens.RenderSiblingScreen(route, onBack, onNavigate)
    }

    @Composable
    override fun ContentRouteScreen(
        route: String,
        onBack: () -> Unit,
        onNavigate: (String) -> Unit,
        onOpenVideoComments: ((videoId: String) -> Unit)?,
    ) {
        CrossFeatureScreens.ContentRouteScreen(route, onBack, onNavigate, onOpenVideoComments)
    }

    @Composable
    override fun MineRouteScreen(
        route: String,
        onBack: () -> Unit,
        onNavigate: (String) -> Unit,
    ) {
        CrossFeatureScreens.MineRouteScreen(route, onBack, onNavigate)
    }

    @Composable
    override fun HomeRouteScreen(
        route: String,
        onBack: () -> Unit,
        onNavigate: (String) -> Unit,
    ) {
        CrossFeatureScreens.HomeRouteScreen(route, onBack, onNavigate)
    }

    @Composable
    override fun SwitchStoreDialog(
        selectedId: String,
        onDismiss: () -> Unit,
        onPicked: (storeId: String, storeName: String) -> Unit,
    ) {
        CrossFeatureScreens.SwitchStoreDialog(selectedId, onDismiss, onPicked)
    }
}

package com.example.my_kmp_project.nativeshell

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.DemoShapes
import com.example.my_kmp_project.core.platform.showPlatformToast
import com.example.my_kmp_project.feature.mine.MineHomeContent

/** Flutter ChatTheme / ConversationListItem — WeChat-blue for @/#/reply prefixes. */
internal const val CommunityFeedPageSize = 10

internal val CommunityLinkBlue = Color(0xFF576B95)

internal val ChatOnline = DemoColors.Accent

/** Flutter ChatTheme 在线绿；DesignTokens 无对应语义色，保留 SoT 值。 */
internal val ChatOnlineGreen = Color(0xFF07C160)

/** 聊天列表卡 / 社区帖子卡共用圆角（DesignTokens.Radius.Md），消除各写一份的 8dp 魔数。 */
internal val ContentCardShape = DemoShapes.medium

@Composable
internal fun JetpackMineRoot(
    loggedIn: Boolean,
    displayName: String? = null,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPersonalized: () -> Unit,
    onDeferred: (String) -> Unit,
) {
    // CMP MineHomeContent already mirrors Flutter MinePage (header / quick / functions / menu).
    MineHomeContent(
        loggedIn = loggedIn,
        displayName = displayName,
        onLoginClick = onLogin,
        onLogoutClick = onLogout,
        onOpenSettings = onOpenSettings,
        onOpenPersonalized = onOpenPersonalized,
        onNavigate = { raw ->
            // Paths + Chinese labels → NativeAndroidMain.openDeferred / ProductRouteDispatch.
            onDeferred(raw)
        },
        // Soft-auth: openDeferred already gates mall/wallet/home routes via openWithSoftAuth.
        onRequireLogin = { redirect -> onDeferred(redirect) },
        snackbar = { label -> showPlatformToast(label) },
    )
}

package com.example.my_kmp_project.core.router

/** Shared route keys for the product shell. */
internal object AppRoutes {
    object Splash {
        const val SPLASH = "/splash"
    }

    object Privacy {
        const val CONSENT = "/privacy/consent"
    }

    object Auth {
        const val LOGIN = "/auth/login"
        const val REGISTER = "/auth/register"
    }

    object Home {
        const val HOME = "/home"
    }

    object Chat {
        const val CHAT = "/chat"
    }

    object Community {
        const val COMMUNITY = "/community"
    }

    object Mine {
        const val MINE = "/mine"
        const val ABOUT = "/mine/about"
    }
}

internal enum class MainTab(
    val route: String,
    val label: String,
    val requiresAuth: Boolean = false,
) {
    Home(AppRoutes.Home.HOME, "首页"),
    Chat(AppRoutes.Chat.CHAT, "聊天", requiresAuth = true),
    Community(AppRoutes.Community.COMMUNITY, "社区", requiresAuth = true),
    Mine(AppRoutes.Mine.MINE, "我的"),
}

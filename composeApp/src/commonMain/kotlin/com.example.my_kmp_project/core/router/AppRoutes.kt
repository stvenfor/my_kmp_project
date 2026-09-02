package com.example.my_kmp_project.core.router

/** Demo route keys — Home / Mine only. */
internal object AppRoutes {
    object Home {
        const val HOME = "/home"
    }

    object Mine {
        const val MINE = "/mine"
        const val ABOUT = "/mine/about"
    }
}

internal enum class MainTab(
    val route: String,
    val label: String,
) {
    Home(AppRoutes.Home.HOME, "首页"),
    Mine(AppRoutes.Mine.MINE, "我的"),
}

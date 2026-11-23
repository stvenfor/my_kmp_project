package com.example.my_kmp_project.core.design

/** Shared width classes for responsive layouts across Android, iOS, and HarmonyOS. */
enum class AdaptiveWindowClass {
    Compact,
    Medium,
    Expanded,
}

/** Material-style width breakpoints, evaluated from the available window width in dp. */
fun adaptiveWindowClass(widthDp: Float): AdaptiveWindowClass = when {
    widthDp < 600f -> AdaptiveWindowClass.Compact
    widthDp < 840f -> AdaptiveWindowClass.Medium
    else -> AdaptiveWindowClass.Expanded
}

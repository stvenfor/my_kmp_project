package com.example.my_kmp_project.component.di

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 把 [AppGraph] 传入 Compose 树的局部入口（DI 第一批）。
 *
 * 默认 null = 尚未接线，调用方必须显式处理未提供的情况；
 * 本批只在 app 层内部使用，feature 的读取改造留到后续批次。
 */
public val LocalAppGraph: ProvidableCompositionLocal<AppGraph?> =
    staticCompositionLocalOf { null }

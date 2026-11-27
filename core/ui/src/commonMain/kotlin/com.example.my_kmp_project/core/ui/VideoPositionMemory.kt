package com.example.my_kmp_project.core.ui

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 跨页面记住视频播放位置（AppScope 内存；进程退出即清空）。
 *
 * key 用视频地址（业务唯一且稳定）。平台没有可靠进度回传时不要写入，
 * 由调用方按真实快照决定，禁止臆造进度。
 */
interface VideoPositionMemory {
    fun positionOf(key: String): Long
    fun save(key: String, positionMs: Long)
    fun clear(key: String)
}

/** 未注入时为 null，视频页退化为「从头播放」，不影响现有行为。 */
val LocalVideoPositionMemory = staticCompositionLocalOf<VideoPositionMemory?> { null }

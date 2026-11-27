package com.example.my_kmp_project.feature.media

import com.example.my_kmp_project.core.ui.VideoPositionMemory

/** 进程内视频进度记忆；只在 AppScope 生命周期内有效，不落盘。 */
class InMemoryVideoPositionMemory : VideoPositionMemory {
    private val positions = mutableMapOf<String, Long>()

    override fun positionOf(key: String): Long = positions[key] ?: 0L

    override fun save(key: String, positionMs: Long) {
        if (key.isBlank() || positionMs <= 0L) return
        positions[key] = positionMs
    }

    override fun clear(key: String) {
        positions.remove(key)
    }
}

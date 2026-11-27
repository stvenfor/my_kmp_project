package com.example.my_kmp_project.app.di

import com.example.my_kmp_project.component.di.AppGraph
import com.example.my_kmp_project.component.di.SessionGraph
import kotlin.concurrent.Volatile

/**
 * 过渡期进程级图持有者（DI 第二批）。
 *
 * 只给 app 层新代码 / 测试用；存量调用方仍走各自的 object 单例。
 * 未安装时给出可读错误，而不是 NPE。
 */
internal object LegacyGraph {

    @Volatile
    private var installed: AppGraph? = null

    fun install(graph: AppGraph) {
        installed = graph
    }

    fun require(): AppGraph =
        installed ?: error("LegacyGraph not installed — call AppBootstrap.start() at a shell entry first.")

    fun sessionOrThrow(): SessionGraph = require().requireSession()

    /** 测试隔离用：清空已安装的图。生产入口不需要调用。 */
    fun reset() {
        installed = null
    }
}

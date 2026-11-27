package com.example.my_kmp_project.app

import com.example.my_kmp_project.app.di.AppBootstrap

/**
 * 入口兼容壳：三端（Android / iOS / OHOS）与 `AppShell` 仍调用 `install()`。
 *
 * 真正的启动逻辑已拆到 [AppBootstrap.start]（会话 → 屏幕端口 → realtime 顺序不变、
 * 幂等）。保留这个对象只是为了不触碰三个壳的入口代码。
 */
internal object ShellBootstrap {
    fun install() {
        AppBootstrap.start()
    }
}

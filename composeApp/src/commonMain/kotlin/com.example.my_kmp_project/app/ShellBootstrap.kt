package com.example.my_kmp_project.app

import com.example.my_kmp_project.component.routing.CrossFeatureScreens
import com.example.my_kmp_project.feature.auth.AuthSessionBootstrap
import kotlin.concurrent.Volatile

/**
 * Assembly bootstrap for every shell entrypoint (precedent: [AuthSessionBootstrap.install],
 * which this wraps so ordering stays idempotent and single-sourced):
 * session hydration + 401 refresh wiring + cross-feature screen port installation.
 *
 * Called from `AppShell`, `MainActivity`, iOS `MainViewController` factories and
 * `MainArkUIViewController` before any shared Compose UI renders.
 */
internal object ShellBootstrap {

    @Volatile
    private var installed = false

    fun install() {
        if (installed) return
        installed = true
        AuthSessionBootstrap.install()
        CrossFeatureScreens.install(AppCrossFeatureScreens)
    }
}

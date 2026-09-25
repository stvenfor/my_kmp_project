package com.example.my_kmp_project.feature.auth

/**
 * Draft credentials shared across Flutter-shaped auth step pages
 * (`/login` hub → `/login/password` | `/login/otp`).
 */
internal object AuthPendingCredentials {
    var email: String = ""
    var phone: String = "13400000000"
}

package com.example.my_kmp_project

internal interface Platform {
    val name: String
}

internal expect fun getPlatform(): Platform


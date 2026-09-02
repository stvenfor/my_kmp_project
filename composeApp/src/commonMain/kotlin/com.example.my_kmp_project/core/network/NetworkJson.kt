package com.example.my_kmp_project.core.network

import kotlinx.serialization.json.Json

internal val NetworkJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
}

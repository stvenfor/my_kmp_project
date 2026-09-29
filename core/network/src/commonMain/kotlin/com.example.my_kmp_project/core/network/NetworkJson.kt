package com.example.my_kmp_project.core.network

import kotlinx.serialization.json.Json

/** Single shared Json for BFF envelopes, request/response DTOs, and session snapshots. */
public val NetworkJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
    explicitNulls = false
}

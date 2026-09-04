package com.example.my_kmp_project.core.network

/**
 * Raw HTTP text payload (used for oauth/token which is not a business envelope).
 */
public data class HttpTextResponse(
    val statusCode: Int,
    val body: String,
)

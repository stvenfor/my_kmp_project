package com.example.my_kmp_project.core.network

import kotlinx.serialization.json.Json

/** Single shared Json for BFF envelopes, request/response DTOs, and session snapshots. */
public val NetworkJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
    explicitNulls = false
}

/**
 * 少数 BFF body **必须显式发 null**（Flutter：`/community/posts` 的
 * `video_url`/`video_cover_url`、`/used-car-orders` 与 `/deal-invoices` 的 `image_url`、
 * `/short-videos` 的上传占位字段）；[NetworkJson] 关掉了 `explicitNulls`，
 * 这些调用点改用本实例编码请求体，其余一律用 [NetworkJson]。
 */
public val NetworkJsonExplicitNulls: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
    explicitNulls = true
}

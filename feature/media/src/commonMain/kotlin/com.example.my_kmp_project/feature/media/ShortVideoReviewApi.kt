package com.example.my_kmp_project.feature.media

import com.example.my_kmp_project.core.network.ApiClient
import com.example.my_kmp_project.core.network.ApiResponse
import com.example.my_kmp_project.core.network.NetworkFacade
import com.example.my_kmp_project.core.network.NetworkJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * `POST /api/v1/short-videos/:id/review` 契约（后端 `short_video/router.go:49`）。
 *
 * 真相源：Go `my_go_study` `short_video/handler.go:164-188` Review —— 人工放行，
 * body 只接受 `{"action":"approve"}`（`strings.ToLower(TrimSpace(action)) != "approve"` 即 400），
 * 响应是 `response.Success(c, dto)`（放行后的视频 DTO，本契约层只透传 `data` 原始对象）。
 *
 * 仿 [PaymentPrepayApi] 样板：**只登记后端真实路由契约**（路径常量 + 请求模型 +
 * 可调用方法 + 契约测试），不自造 UI 调用点，也不改后端仓库。
 * 与 [HttpShortVideoRepository] 分开：后者是已接线的 #72–#78 读/写仓库，本端点是
 * `missing(待接线)`（契约表 B 区 #14），不塞进既有仓库方法。
 */
public class ShortVideoReviewApi(
    private val client: ApiClient = NetworkFacade.api(),
) {
    /** `POST /api/v1/short-videos/:id/review`，`data` 原样透传（与 `MallApi.requireRawMapPost` 同款）。 */
    public suspend fun review(id: String, request: ShortVideoReviewRequest): ApiResponse<JsonObject> =
        client.postApi(
            path = ShortVideoReviewPaths.review(id),
            body = NetworkJson.encodeToString(request),
        ) { el: JsonElement? -> el as? JsonObject }
}

/** 短视频人工放行路径常量（对齐 `short_video/router.go:49`）。 */
public object ShortVideoReviewPaths {
    private const val ROOT: String = "/api/v1/short-videos"

    /** `POST /api/v1/short-videos/:id/review`。 */
    public fun review(id: String): String = "$ROOT/$id/review"
}

/** `POST /short-videos/:id/review` body（`short_video/handler.go:171-173`，仅 `approve` 合法）。 */
@Serializable
public data class ShortVideoReviewRequest(
    val action: String = "approve",
)

package com.example.my_kmp_project.feature.commerce

import com.example.my_kmp_project.core.network.ApiClient
import com.example.my_kmp_project.core.network.ApiResponse
import com.example.my_kmp_project.core.network.NetworkError
import com.example.my_kmp_project.core.network.getApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 会员目录 / 权益的真实后端契约（`GET /api/v1/membership/me`）。 */
public object MembershipPaths {
    public const val ME: String = "/api/v1/membership/me"
}

@Serializable
public data class MembershipMePlan(
    @SerialName("plan_id") val planId: String = "",
    val tier: String = "",
    val title: String = "",
    val months: Int = 1,
    val price: String = "0.00",
    @SerialName("price_fen") val priceFen: Long = 0,
    @SerialName("auto_renew_eligible") val autoRenewEligible: Boolean = false,
) {
    public val priceLabel: String get() = "¥$price"
    public val durationLabel: String get() = when {
        months >= 12 -> "12 个月"
        months > 1 -> "$months 个月"
        else -> "1 个月"
    }
}

@Serializable
public data class MembershipMeEntitlement(
    val tier: String = "",
    @SerialName("expires_at") val expiresAt: String = "",
    val active: Boolean = false,
    val cta: String = "",
)

@Serializable
public data class MembershipMeData(
    val catalog: List<MembershipMePlan> = emptyList(),
    val entitlements: List<MembershipMeEntitlement> = emptyList(),
)

public class MembershipApi(private val api: ApiClient) {
    public suspend fun fetchMe(): MembershipMeData {
        val response: ApiResponse<MembershipMeData> = api.getApi(MembershipPaths.ME)
        val data = response.data
        if (!response.isSuccess || data == null) {
            throw NetworkError.Business(
                response.code,
                response.message?.takeIf { it.isNotEmpty() } ?: "加载会员信息失败",
            )
        }
        return data
    }
}

/** `svip` / `ai_svip` → 展示名。 */
public fun membershipTierLabel(tier: String): String = when (tier) {
    "ai_svip" -> "AI 会员"
    "svip" -> "会员"
    else -> tier
}

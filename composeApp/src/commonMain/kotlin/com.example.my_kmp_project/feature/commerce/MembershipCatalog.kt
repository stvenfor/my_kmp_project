package com.example.my_kmp_project.feature.commerce

import androidx.compose.ui.graphics.Color

internal enum class MembershipTier {
    Svip,
    AiSvip,
}

internal data class MembershipPlan(
    val id: String,
    val tier: MembershipTier,
    val title: String,
    val price: Double,
    val originalPrice: Double,
    val badge: String? = null,
    val dailyHint: String? = null,
)

internal data class MembershipPromo(
    val title: String,
    val subtitle: String,
)

internal data class MembershipFeature(
    val title: String,
    val subtitle: String,
)

/** Layout / palette tokens aligned with Flutter `module_pay` membership theme. */
internal object MembershipTokens {
    val PageBg = Color(0xFFF7F7F7)
    val CardWhite = Color(0xFFFFFFFF)
    val TitleBlack = Color(0xFF333333)
    val TextGray = Color(0xFF999999)
    val TextGrayLight = Color(0xFFBFBFBF)
    val PriceBlack = Color(0xFF1A1A1A)
    val OriginalPriceGray = Color(0xFFBFBFBF)
    val PlanBorderUnselected = Color(0xFFEEEEEE)
    val PlanSelectedFill = Color(0xFFFFF8ED)
    val PlanBadgePromoBg = Color(0xFFFFF8E6)
    val BeanOrange = Color(0xFFFF8A34)

    val SvipAccent = Color(0xFFFF8A34)
    val SvipAccentLight = Color(0xFFFFB800)
    val SvipPlanBorder = Color(0xFFFF8A34)
    val SvipCtaStart = Color(0xFFFFD36A)
    val SvipCtaEnd = Color(0xFFFF8A34)
    val SvipHeaderTop = Color(0xFFFFF3D4)
    val SvipPromoBg = Color(0xFFEFF8E8)
    val SvipPromoAccent = Color(0xFF52C41A)

    val AiAccent = Color(0xFF9D7CFF)
    val AiPlanBorder = Color(0xFF9D7CFF)
    val AiCtaStart = Color(0xFF9D7CFF)
    val AiCtaEnd = Color(0xFF5B7CFF)
    val AiHeaderTop = Color(0xFFE8E1FF)
    val AiPromoBg = Color(0xFFF3EEFF)

    const val PlanCardHeight = 148
    const val PlanCardRadius = 12
}

internal object MembershipCatalog {
    val statusExpired = "您的会员身份已过期"
    val displayName = "会员用户"

    val svipPlans = listOf(
        MembershipPlan(
            id = "svip_12m",
            tier = MembershipTier.Svip,
            title = "12个月",
            price = 380.0,
            originalPrice = 488.0,
            badge = "开学尝鲜价",
        ),
        MembershipPlan(
            id = "svip_24m",
            tier = MembershipTier.Svip,
            title = "24个月",
            price = 488.0,
            originalPrice = 888.0,
            badge = "活动利益点",
            dailyHint = "每日仅需0.66元",
        ),
        MembershipPlan(
            id = "svip_year_auto",
            tier = MembershipTier.Svip,
            title = "连续包年",
            price = 288.0,
            originalPrice = 488.0,
            dailyHint = "每日仅需0.78元",
        ),
    )

    val aiSvipPlans = listOf(
        MembershipPlan(
            id = "ai_12m",
            tier = MembershipTier.AiSvip,
            title = "12个月",
            price = 488.0,
            originalPrice = 688.0,
            badge = "开学尝鲜价",
        ),
        MembershipPlan(
            id = "ai_24m",
            tier = MembershipTier.AiSvip,
            title = "24个月",
            price = 688.0,
            originalPrice = 1288.0,
            badge = "活动利益点",
            dailyHint = "每日仅需0.94元",
        ),
        MembershipPlan(
            id = "ai_year_auto",
            tier = MembershipTier.AiSvip,
            title = "连续包年",
            price = 398.0,
            originalPrice = 688.0,
            dailyHint = "每日仅需1.09元",
        ),
    )

    fun plansFor(tier: MembershipTier): List<MembershipPlan> = when (tier) {
        MembershipTier.Svip -> svipPlans
        MembershipTier.AiSvip -> aiSvipPlans
    }

    fun promoFor(tier: MembershipTier): MembershipPromo = when (tier) {
        MembershipTier.Svip -> MembershipPromo("春日踏青礼", "加赠限定勋章、装扮套装")
        MembershipTier.AiSvip -> MembershipPromo("寒假学习礼", "赠新春礼包")
    }

    val aiFeatures = listOf(
        MembershipFeature("背单词", "听音辨义 拼写无忧"),
        MembershipFeature("读课文", "智能打分 纠正发音"),
        MembershipFeature("AI私教", "告别死记 活学活用"),
        MembershipFeature("刷真题", "考点精粹 高效提分"),
    )
}

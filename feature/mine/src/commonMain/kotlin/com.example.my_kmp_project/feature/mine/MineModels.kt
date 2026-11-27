package com.example.my_kmp_project.feature.mine
import com.example.my_kmp_project.core.design.MineIcons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class MineStat(val value: String, val label: String)

data class MineQuickService(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val iconColor: Color,
    val badge: String? = null,
)

data class MineMenuEntry(
    val id: String,
    val label: String,
    val showBadge: Boolean = false,
)

data class MineFunctionEntry(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconColor: Color,
    val accentColor: Color,
    val highlightValue: String? = null,
)

data class MineProfileUi(
    val displayName: String,
    val roleBadge: String,
    val storeName: String,
    val maskedPhone: String,
    val stats: List<MineStat>,
)

object MineCatalog {
    val guestStats = listOf(
        MineStat("0", "加入天数"),
        MineStat("0", "员工数"),
        MineStat("0", "店铺天数"),
        MineStat("0", "累计客户"),
    )

    /** 登录态骨架：真实昵称 / 门店 / 统计由 MineAccountController 从 /me/profile 拉取后覆盖。 */
    val goldDemoProfile = MineProfileUi(
        displayName = "qa_user",
        roleBadge = "",
        storeName = "",
        maskedPhone = "",
        stats = guestStats,
    )

    /** Flutter `MineQuickServiceData.items` — mall / wallet / order only. */
    val quickServices = listOf(
        MineQuickService("mall", "商城", MineIcons.Bag, Color(0xFF0070F3), badge = "HOT"),
        MineQuickService("wallet", "我的钱包", MineIcons.CreditCard, Color(0xFF5856D6)),
        MineQuickService("order", "我的订单", MineIcons.DocText, Color(0xFF34C759)),
    )

    /** Flutter `MineMenuData.items`. */
    val menuItems = listOf(
        MineMenuEntry("address", "收货地址"),
        MineMenuEntry("cooperation", "商务合作"),
        MineMenuEntry("reminder", "提醒事项"),
        MineMenuEntry("invite", "邀请好友"),
        MineMenuEntry("fan_group", "粉丝群", showBadge = true),
        MineMenuEntry("feedback", "意见反馈"),
        MineMenuEntry("settings", "设置"),
    )

    /** Flutter `MineFunctionData.catalog` order. */
    val functions = listOf(
        MineFunctionEntry(
            id = "sms",
            title = "短信模板",
            subtitle = "一键发送 轻松快捷",
            icon = MineIcons.Sms,
            iconColor = Color(0xFF0070F3),
            accentColor = Color(0x140070F3),
        ),
        MineFunctionEntry(
            id = "calculator",
            title = "购车计算器",
            subtitle = "全款/贷款/保险全能算",
            icon = MineIcons.Calculate,
            iconColor = Color(0xFF0070F3),
            accentColor = Color(0x140070F3),
            highlightValue = "5830.00",
        ),
        MineFunctionEntry(
            id = "used_car",
            title = "二手车",
            subtitle = "置换/专卖/收车",
            icon = MineIcons.Car,
            iconColor = Color(0xFF0070F3),
            accentColor = Color(0x140070F3),
        ),
        MineFunctionEntry(
            id = "ledger",
            title = "收支",
            subtitle = "个人收支记录",
            icon = MineIcons.CreditCard,
            iconColor = Color(0xFF1C7C54),
            accentColor = Color(0x141C7C54),
        ),
        MineFunctionEntry(
            id = "short_video",
            title = "小视频",
            subtitle = "用小视频秀车秀店",
            icon = MineIcons.PlayCircle,
            iconColor = Color(0xFF5856D6),
            accentColor = Color(0x145856D6),
        ),
        MineFunctionEntry(
            id = "after_sales",
            title = "售后专区",
            subtitle = "售后维修保养记录",
            icon = MineIcons.Build,
            iconColor = Color(0xFFFF9500),
            accentColor = Color(0x14FF9500),
        ),
        MineFunctionEntry(
            id = "qr_pay",
            title = "店铺收款码",
            subtitle = "常见问题 功能介绍",
            icon = MineIcons.QrCode,
            iconColor = Color(0xFF0070F3),
            accentColor = Color(0x140070F3),
        ),
        MineFunctionEntry(
            id = "qa",
            title = "选买问答",
            subtitle = "在线解答客户问题",
            icon = MineIcons.Support,
            iconColor = Color(0xFF0070F3),
            accentColor = Color(0x140070F3),
        ),
        MineFunctionEntry(
            id = "poster",
            title = "商家海报",
            subtitle = "置换/专卖/估价",
            icon = MineIcons.Chart,
            iconColor = Color(0xFFFF9500),
            accentColor = Color(0x14FF9500),
        ),
    )

    fun profile(loggedIn: Boolean, displayName: String?): MineProfileUi =
        if (loggedIn) {
            goldDemoProfile.copy(
                displayName = displayName?.takeIf { it.isNotBlank() } ?: goldDemoProfile.displayName,
            )
        } else {
            MineProfileUi(
                displayName = "访客",
                roleBadge = "未登录",
                storeName = "登录后查看门店信息",
                maskedPhone = "— — —",
                stats = guestStats,
            )
        }
}

package com.example.my_kmp_project.core.router

/**
 * Cross-feature route definitions, consolidated into `core.router` so route constants
 * live below the feature layer and no `feature:*` needs to import another feature.
 *
 * Extracted from the embedded `internal object XxxRoutes` blocks that previously lived
 * inside each feature's `*RouteHost.kt`. Visibility widened to `public` so features can
 * reference them. All cross-references resolve within this same package.
 *
 * Stays inside `:composeApp` — the `core/…` Gradle split was reverted (proposal rule R4:
 * one `:composeApp` module plus `libkn.so`); package layering, not module boundaries,
 * is what [scripts/check-layer-deps.sh] enforces.
 */

/** Flutter `RoutePath` home secondaries — string keys used by Native shell overlay. */
public object HomeRoutes {
    const val Search = "/home/search"
    const val Strategy = "/home/strategy"
    const val LearningReport = "/home/learning_report"
    const val CheckInMall = "/home/check_in_mall"
    const val DubbingFeed = "/home/dubbing_feed"
    const val HotRankDetail = "/home/hot_rank_detail"
    const val LifeService = "/home/life_service"
    const val LiveCommerce = "/home/live_commerce"
    const val Club = "/home/club"
    const val UsedCar = "/home/used_car"
    const val UsedCarDetail = "/home/used_car/detail"
    const val UsedCarCreate = "/home/used_car/create"
    const val Ledger = "/home/ledger"
    const val LedgerDetail = "/home/ledger/detail"
    const val DataAnalytics = "/home/data_analytics"
    const val DataAnalyticsDetail = "/home/data_analytics/detail"
    const val TodoPartner = "/home/todo/partner-pending"
    const val TodoFollowUp = "/home/todo/follow-up-customers"
    const val TodoAfterSales = "/home/todo/after-sales-appointments"
    const val TodoOrderReview = "/home/todo/order-pending-review"
    const val AfterSales = "/home/after_sales"
    const val AfterSalesCreate = "/home/after_sales/create"
    const val AfterSalesDetail = "/home/after_sales/detail"
    const val NewCarFollow = "/home/new_car_follow"
    const val NewCarFollowCreate = "/home/new_car_follow/create"
    const val NewCarFollowDetail = "/home/new_car_follow/detail"

    const val AllServices = "/home/all_services"

    /** Map Home feature / todo labels → route. */
    fun fromLabel(label: String): String? = when (label.trim()) {
        "搜索" -> Search
        "全部服务", "更多" -> AllServices
        "投资策略", "策略", "朋友圈营销", "朋友圈" -> Strategy
        "学习报告" -> LearningReport
        "签到商城", "积分商城", "签到日历", "签到", "营销活动" -> CheckInMall
        "配音", "配音首页" -> DubbingFeed
        "热榜", "热配榜" -> HotRankDetail
        "生活服务" -> LifeService
        "直播带货" -> LiveCommerce
        // 「直播」alone → Content live room in ContentRoutes; keep 直播带货 here.
        "Club" -> Club
        "二手车" -> UsedCar
        "台账", "公司数据", "收支" -> Ledger
        "数据分析", "竞品分析" -> DataAnalytics
        "新伙伴待确认" -> TodoPartner
        "待跟进客户", "销售顾问" -> TodoFollowUp
        "售后预约" -> TodoAfterSales
        "订单待审核" -> TodoOrderReview
        "售后", "售后专区" -> AfterSales
        // Flutter: 新车成交 → DealInvoiceDemo; 新车跟进 → NewCarFollow (distinct).
        "新车跟进", "新车关注" -> NewCarFollow
        "新车成交" -> MineRoutes.DealInvoiceDemo
        // Flutter HomeController.onStoreTap → SwitchStoreDialog (Mine-root), not DataAnalytics.
        "门店" -> null
        "话术库" -> Strategy
        else -> null
    }
}

public object MineRoutes {
    const val Mall = "/mall"
    const val MallDetail = "/mall/detail"
    const val MallOrders = "/mall/orders"
    const val MallOrderDetail = "/mall/orders/detail"
    const val Wallet = "/wallet"
    const val Pay = "/pay"
    const val Membership = "/pay/membership"
    const val Profile = "/mine/profile"
    const val Addresses = "/mine/addresses"
    const val AddressEdit = "/mine/addresses/edit"
    const val Calculator = "/mine/purchase_calculator"
    const val Classroom = ClassroomRoutes.MyClass
    const val ShortVideo = VideoRoutes.Short
    const val CheckIn = "/home/check_in_mall"
    const val SmsTemplates = "/mine/sms_templates"
    const val ShopQr = "/mine/shop_qr"
    /** Flutter `RoutePath.mineHttpTest` (选买问答). */
    const val HttpTest = "/mine/http_test"
    /** @deprecated alias — prefer [HttpTest]. */
    const val BuyQa = HttpTest
    const val Poster = "/mine/poster"
    const val Settings = "/settings"
    const val SettingsLegacy = "/mine/settings"
    const val DealInvoiceDemo = "/settings/deal_invoice_demo"
    const val DealInvoiceUpload = "/settings/deal_invoice/upload"
    const val PersonalizedSettings = "/mine/personalized_settings"
    const val Feedback = "/mine/feedback"
    const val Cooperation = "/mine/cooperation"
    const val Reminder = "/mine/reminder"
    const val Invite = "/mine/invite"

    fun fromLabel(label: String): String? = when (label.trim()) {
        "商城", "mall" -> Mall
        "我的订单", "订单", "订单中心" -> MallOrders
        "我的钱包", "钱包" -> Wallet
        "会员", "会员续费" -> Membership
        "我的课程", "课程" -> Classroom
        // Flutter onFunctionTap: sms / qr_pay / poster are toast-only — do NOT map.
        "短信模板", "店铺收款码", "收款码", "商家海报", "海报" -> null
        "购车计算器", "计算器" -> Calculator
        "二手车" -> HomeRoutes.UsedCar
        "收支", "台账" -> HomeRoutes.Ledger
        "售后", "售后专区" -> HomeRoutes.AfterSales
        "小视频" -> ShortVideo
        "选买问答" -> HttpTest
        "地址管理", "地址", "收货地址" -> Addresses
        "个人资料", "资料" -> Profile
        "签到日历" -> CheckIn
        "设置" -> Settings
        "设置页" -> Settings
        "/mine/settings" -> Settings
        "新车成交" -> DealInvoiceDemo
        // Flutter MineController: these are toast-only — do NOT open Invite/fake screens.
        "意见反馈", "帮助中心" -> null
        "商务合作" -> null
        "提醒事项" -> null
        "邀请好友" -> null
        // Flutter mine_controller: fan_group → toast('粉丝群')，无页面（showBadge=true）。
        "粉丝群" -> null
        "电子名片" -> null
        "好友" -> null // friend list is ContentRoutes.Friend
        "切换门店" -> null // SwitchStoreDialog on Mine root
        else -> null
    }

    /** Normalize shell catalog aliases → canonical MineRoutes paths. */
    fun canonicalize(route: String): String = when (route) {
        "/mine/sms_template" -> SmsTemplates
        "/mine/store_qr" -> ShopQr
        "/mine/qa", "/mine/buy_qa" -> HttpTest
        "/mine/business" -> Cooperation
        "/mine/reminders" -> Reminder
        // Do NOT map business_card / friend → Invite (crash + wrong page).
        else -> route
    }
}

/** Cross-module content routes (Video / Classroom / Live / Friend / Music / AI). */
public object ContentRoutes {
    const val Live = "/live"
    const val LiveRoom = "/live/room"
    const val Friend = "/friend"
    const val Music = "/music/list"
    const val MusicNowPlaying = "/music/now_playing"
    const val MediaEntry = "/media/entry"

    fun fromLabel(label: String): String? {
        VideoRoutes.fromLabel(label)?.let { return it }
        ClassroomRoutes.fromLabel(label)?.let { return it }
        AiRoutes.fromLabel(label)?.let { return it }
        return when (label.trim()) {
            "直播", "直播间" -> Live
            "好友", "通讯录", "朋友" -> Friend
            "音乐", "歌单", "音频列表" -> Music
            "音视频" -> MediaEntry
            else -> null
        }
    }
}

/** Flutter `RoutePath` video graph — Android-first clickable mock. */
public object VideoRoutes {
    const val Hub = "/video"
    const val Short = "/video/short"
    const val ShortPlay = "/video/short/play"
    const val ShortPublish = "/video/short/publish"
    const val ShortHelp = "/video/short/help"
    const val DubbingVideos = "/video/dubbing/videos"
    const val DubbingVideoDetail = "/video/dubbing/videos/detail"
    const val DubbingWorks = "/video/dubbing/works"
    const val DubbingWorkDetail = "/video/dubbing/works/detail"

    fun fromLabel(label: String): String? = when (label.trim()) {
        "视频", "视频中心", "Video", "视频号", "素材库", "视频列表" -> Hub
        "小视频", "短视频" -> Short
        "配音视频", "配音列表" -> DubbingVideos
        "配音作品", "作品列表" -> DubbingWorks
        else -> null
    }
}

public object CommunityRoutes {
    const val Publish = "/community/publish"
    const val Search = "/community/search"
    const val Convention = "/community/convention"
    const val ImagePreview = "/community/image_preview"
    const val VideoPlay = "/community/video_play"
    const val TopicSelect = "/community/topic_select"
    const val Comment = "/community/comment"

    fun fromLabel(label: String): String? = when (label.trim()) {
        "发布动态", "社区发布" -> Publish
        "社区搜索" -> Search
        "社区公约" -> Convention
        "话题选择" -> TopicSelect
        "评论", "社区评论" -> Comment
        else -> null
    }
}

public object ClassroomRoutes {
    const val MyClass = "/classroom/my_class"
    const val HomeworkStats = "/classroom/homework_stats"
    const val HomeworkTeacher = "/classroom/homework/detail_teacher"
    const val HomeworkStudent = "/classroom/homework/detail_student"
    const val HomeworkDubbing = "/classroom/homework/dubbing"
    const val HomeworkReview = "/classroom/homework/review"
    const val GiftClaim = "/classroom/gift/claim"
    const val VideoDetail = "/classroom/video/detail"

    fun fromLabel(label: String): String? = when (label.trim()) {
        "我的课程", "课程", "课堂", "我的班级", "班级教学", "培训" -> MyClass
        "作业统计" -> HomeworkStats
        "领取礼品卡", "礼品卡" -> GiftClaim
        else -> null
    }
}

public object AiRoutes {
    const val Stream = "/ai/stream"

    fun fromLabel(label: String): String? = when (label.trim()) {
        "AI小石头", "AI 小石头", "小石头", "AI" -> Stream
        else -> null
    }
}

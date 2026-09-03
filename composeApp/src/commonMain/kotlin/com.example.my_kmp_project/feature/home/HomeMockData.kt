package com.example.my_kmp_project.feature.home

/** Static mock models aligned with Flutter `module_home` sample data. */

internal data class HomeFeatureItem(val label: String)
internal data class HomeQuickAction(
    val title: String,
    val subtitle: String,
    val actionLabel: String,
)
internal data class HomeMetric(val value: String, val label: String)
internal data class HomeMetricDetail(val value: String, val label: String)
internal data class HomeServiceItem(val label: String, val badge: String? = null)
internal data class HomeContactItem(
    val title: String,
    val subtitle: String,
    val trailing: String? = null,
)
internal data class HomeNewsItem(
    val title: String,
    val source: String,
    val date: String,
)

internal data class AllServiceItem(
    val id: String,
    val label: String,
    /** Flutter all_services asset file name, e.g. calculator.png */
    val assetName: String = "all_functions.png",
)
internal data class AllServiceSection(
    val title: String,
    val subtitle: String? = null,
    val showEditButton: Boolean = false,
    val items: List<AllServiceItem>,
)

internal data class SearchRankItem(
    val id: String,
    val rank: Int,
    val title: String,
    val subtitle: String,
)

internal data class ReportHighlight(
    val emoji: String,
    val title: String,
    val subtitle: String,
    val trailing: String,
)

internal data class ReportRecord(
    val emoji: String,
    val title: String,
    val subtitle: String,
    val time: String,
    val status: String,
    val statusHighlight: Boolean = false,
)

internal data class StrategyAssetCell(
    val label: String,
    val value: String,
    val positive: Boolean,
)

internal object HomeMockData {
    const val greeting = "早上好，沃德龙鼎"
    const val storeName = "[4S]北京沃德龙鼎吉利"
    const val searchPlaceholder = "搜索客户、订单、资讯"

    val features = listOf(
        HomeFeatureItem("销售顾问"),
        HomeFeatureItem("生活服务"),
        HomeFeatureItem("二手车"),
        HomeFeatureItem("新车关注"),
        HomeFeatureItem("客户管理"),
        HomeFeatureItem("订单中心"),
        HomeFeatureItem("数据分析"),
        HomeFeatureItem("直播带货"),
        HomeFeatureItem("营销活动"),
        HomeFeatureItem("更多"),
    )

    val quickActions = listOf(
        HomeQuickAction("新伙伴待确认", "3 位新成员等待审核", "去处理"),
        HomeQuickAction("待跟进客户", "今日 5 位意向客户", "去查看"),
        HomeQuickAction("订单待审核", "2 笔新车订单", "去处理"),
        HomeQuickAction("售后预约", "4 位客户今日到店", "去查看"),
    )

    val metricsToday = listOf(
        HomeMetric("99", "意向客户"),
        HomeMetric("2", "新车订单"),
        HomeMetric("999.8", "成交额(万)"),
        HomeMetric("15", "试驾预约"),
    )

    val metricDetails = listOf(
        HomeMetricDetail("8", "待交车"),
        HomeMetricDetail("3", "待回访"),
        HomeMetricDetail("12", "待跟进"),
    )

    val services = listOf(
        HomeServiceItem("朋友圈", "热门"),
        HomeServiceItem("视频号"),
        HomeServiceItem("直播", "新品"),
        HomeServiceItem("素材库"),
        HomeServiceItem("话术库"),
        HomeServiceItem("培训"),
        HomeServiceItem("竞品分析"),
        HomeServiceItem("更多"),
    )

    val contacts = listOf(
        HomeContactItem("李大仁", "专属客户顾问 · 金牌销售"),
        HomeContactItem("AI在线咨询", "7×24 小时智能客服", "聊"),
        HomeContactItem("400 售后热线", "工作日 9:00-18:00", "拨"),
    )

    val news = listOf(
        HomeNewsItem("2024年新能源汽车市场趋势分析报告发布", "汽车之家行业频道", "2024.05.11"),
        HomeNewsItem("吉利星越L新款上市，配置全面升级", "汽车之家", "2024.05.10"),
        HomeNewsItem("经销商数字化转型白皮书：从流量到留量", "i车商资讯", "2024.05.09"),
    )

    /** Extra tool row — preserves existing KMP wiring for media/web/friend/classroom. */
    val toolEntries = listOf(
        HomeFeatureItem("音视频") to "media",
        HomeFeatureItem("内嵌网页") to "web",
        HomeFeatureItem("好友") to "friend",
        HomeFeatureItem("课堂") to "classroom",
    )

    val favoriteServices = listOf(
        AllServiceItem("intro", "引导动画", "smart_online_marketing.png"),
        AllServiceItem("glass", "玻璃卡片", "online_customer_acquisition.png"),
        AllServiceItem("diet", "地中海饮食", "small_video.png"),
        AllServiceItem("drawer", "侧滑导航", "service_management.png"),
        AllServiceItem("diary", "我的日记", "exhibition_hall_shooting.png"),
        AllServiceItem("training", "训练计划", "intelligence_task.png"),
        AllServiceItem("running", "跑步数据", "new_car_in_store.png"),
        AllServiceItem("wave", "波浪动画", "smart_number.png"),
    )

    val catalogSections = listOf(
        AllServiceSection(
            title = "线索服务",
            items = listOf(
                AllServiceItem("intro", "引导动画", "smart_online_marketing.png"),
                AllServiceItem("hotel", "酒店预订", "customer_profile.png"),
                AllServiceItem("filters", "酒店筛选", "smart_sale.png"),
                AllServiceItem("fitness", "健身应用", "new_car_deal.png"),
                AllServiceItem("glass", "玻璃卡片", "online_customer_acquisition.png"),
                AllServiceItem("running", "跑步数据", "new_car_in_store.png"),
                AllServiceItem("wave", "波浪动画", "smart_number.png"),
            ),
        ),
        AllServiceSection(
            title = "营销服务",
            items = listOf(
                AllServiceItem("diary", "我的日记", "exhibition_hall_shooting.png"),
                AllServiceItem("design", "设计课程", "marketing.png"),
                AllServiceItem("training", "训练计划", "intelligence_task.png"),
                AllServiceItem("workout", "训练视图", "v_store.png"),
                AllServiceItem("diet", "地中海饮食", "small_video.png"),
                AllServiceItem("course", "课程详情", "business_poster.png"),
            ),
        ),
        AllServiceSection(
            title = "教学服务",
            items = listOf(
                AllServiceItem("classroom", "班级教学", "intelligence_task.png"),
                AllServiceItem("dubbing", "配音首页", "dubbing_home.png"),
                AllServiceItem("videos", "视频列表", "small_video.png"),
                AllServiceItem("works", "作品列表", "exhibition_hall_shooting.png"),
            ),
        ),
        AllServiceSection(
            title = "其他服务",
            items = listOf(
                AllServiceItem("membership", "会员续费", "marketing.png"),
                AllServiceItem("help", "帮助中心", "after_sales_area.png"),
                AllServiceItem("feedback", "意见反馈", "calculator.png"),
                AllServiceItem("drawer", "侧滑导航", "service_management.png"),
                AllServiceItem("music", "音频列表", "used_car.png"),
            ),
        ),
    )

    val searchHistory = listOf(
        "极限文字一排两个显示",
        "极限文字超出九个字...",
        "宫崎骏宫漫作品",
        "龙猫",
        "闪光少女",
    )

    val searchDiscovery = listOf(
        "罗振宇2026跨年演讲",
        "极限文字超出九个字...",
        "小猪佩奇全系列",
        "百家讲坛全集",
        "百家讲坛明朝",
    )

    val filterTags = listOf(
        "3-5 个句子的配音",
        "较慢的语速",
        "初级难度",
        "女声",
        "1 分钟以内的视频",
    )

    val rankTabs = listOf("热配榜", "诵读榜", "剧集榜", "记录榜", "合作榜")

    fun rankItemsForTab(tabIndex: Int): List<SearchRankItem> = when (tabIndex) {
        1 -> listOf(
            SearchRankItem("r0", 1, "静夜思", "床前明月光，疑是地上霜..."),
            SearchRankItem("r1", 2, "春晓", "春眠不觉晓，处处闻啼鸟..."),
            SearchRankItem("r2", 3, "登鹳雀楼", "白日依山尽，黄河入海流..."),
            SearchRankItem("r3", 4, "望庐山瀑布", "日照香炉生紫烟，遥看瀑布挂前川..."),
            SearchRankItem("r4", 5, "悯农", "锄禾日当午，汗滴禾下土..."),
        )
        2 -> listOf(
            SearchRankItem("s0", 1, "小猪佩奇", "佩奇和乔治的日常生活..."),
            SearchRankItem("s1", 2, "汪汪队立大功", "莱德队长带领狗狗们救援..."),
            SearchRankItem("s2", 3, "超级飞侠", "乐迪环游世界送包裹..."),
            SearchRankItem("s3", 4, "熊出没", "熊大熊二与光头强的故事..."),
            SearchRankItem("s4", 5, "喜羊羊与灰太狼", "羊村与狼堡的欢乐对决..."),
        )
        3 -> listOf(
            SearchRankItem("d0", 1, "我的第一次配音", "完成度 98%，发音清晰自然..."),
            SearchRankItem("d1", 2, "英语朗读打卡", "连续打卡 30 天，进步明显..."),
            SearchRankItem("d2", 3, "亲子共读记录", "与孩子一起完成的温馨朗读..."),
            SearchRankItem("d3", 4, "班级作业精选", "老师推荐的优秀作业展示..."),
            SearchRankItem("d4", 5, "周末练习成果", "周末集中练习的成果汇总..."),
        )
        4 -> listOf(
            SearchRankItem("c0", 1, "BBC 合作专区", "BBC 精选纪录片配音素材..."),
            SearchRankItem("c1", 2, "迪士尼经典合作", "迪士尼动画经典片段..."),
            SearchRankItem("c2", 3, "国家地理探索", "探索自然与科学的配音..."),
            SearchRankItem("c3", 4, "牛津阅读树", "分级阅读配套配音练习..."),
            SearchRankItem("c4", 5, "剑桥少儿英语", "剑桥体系标准发音示范..."),
        )
        else -> listOf(
            SearchRankItem("h0", 1, "穿条纹睡衣的男孩", "某日布鲁诺决定，去铁丝网的另外..."),
            SearchRankItem("h1", 2, "蛮荒故事", "六个独立故事，荒诞与黑色幽默交织..."),
            SearchRankItem("h2", 3, "爱冒险的朵拉", "和朵拉一起开启奇妙冒险之旅..."),
            SearchRankItem("h3", 4, "小王子", "来自 B612 小行星的小王子..."),
            SearchRankItem("h4", 5, "寻梦环游记", "米格在亡灵节追寻音乐梦想..."),
            SearchRankItem("h5", 6, "飞屋环游记", "卡尔用气球带着房子去冒险..."),
            SearchRankItem("h6", 7, "头脑特工队", "情绪小人在大脑里协作成长..."),
            SearchRankItem("h7", 8, "疯狂动物城", "兔子警官与狐狸搭档破案..."),
        )
    }

    val reportHighlights = listOf(
        ReportHighlight("🎬", "《哈利波特》第3章", "视频配音 · 刚刚发布", "100"),
        ReportHighlight("🏆", "解锁「45天」打卡勋章", "里程碑达成 · 太棒了！", "🎉"),
        ReportHighlight("🎵", "\"Wingardium Leviosa!\"", "满分句子 · 可播放原声", "▶"),
    )

    val reportRecords = listOf(
        ReportRecord("🎬", "视频配音", "哈利波特 第3章", "18:32", "已发布", statusHighlight = true),
        ReportRecord("⚔️", "配音闯关", "Level 8 · 3关", "17:10", "15 min"),
        ReportRecord("📚", "同步练", "PEP 五年级上册 Unit 3", "16:45", "8 min"),
        ReportRecord("🤖", "AI 外教", "自由对话 · Tom老师", "15:20", "12 min"),
        ReportRecord("🎧", "听力练习", "英美绕口令 · 5题", "14:00", "5 min"),
    )

    val strategyTabs = listOf("推荐", "逆向", "趋势")
    val strategyPeriods = listOf("今年来", "近1周", "近1月", "近3月", "近1年")
    val strategyAssets = listOf(
        StrategyAssetCell("A股", "+19.22%", true),
        StrategyAssetCell("中债", "+3.15%", true),
        StrategyAssetCell("黄金", "+8.76%", true),
        StrategyAssetCell("港股", "+12.40%", true),
        StrategyAssetCell("美股", "+15.88%", true),
        StrategyAssetCell("原油", "-2.34%", false),
        StrategyAssetCell("美元债", "-1.80%", false),
        StrategyAssetCell("商品", "+4.56%", true),
        StrategyAssetCell("现金", "+1.20%", true),
    )
}

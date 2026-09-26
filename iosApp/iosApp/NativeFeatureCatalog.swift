import SwiftUI

/// Resolves Chinese labels / Flutter RoutePath → native feature id (ADR 0002).
enum NativeRouteResolver {
    static func resolve(_ raw: String) -> String {
        let key = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        if key.hasPrefix("/") { return normalizePath(key) }
        return labelToPath[key] ?? key
    }

    private static func normalizePath(_ path: String) -> String {
        if path == "/video/short" || path.hasPrefix("/video/short?") { return "/video/short" }
        if path.hasPrefix("/live") { return "/live" }
        if path.hasPrefix("/friend") { return "/friend" }
        if path.hasPrefix("/mall/orders") { return "/mall/orders" }
        if path.hasPrefix("/mall/detail") { return "/mall/detail" }
        if path.hasPrefix("/mall") { return "/mall" }
        if path.hasPrefix("/wallet") { return "/wallet" }
        if path.hasPrefix("/ai") { return "/ai/stream" }
        if path.hasPrefix("/music") { return "/music/list" }
        if path.hasPrefix("/classroom") { return "/classroom/my_class" }
        if path.hasPrefix("/community/publish") { return "/community/publish" }
        if path.hasPrefix("/community/search") { return "/community/search" }
        if path.hasPrefix("/community/comment") { return "/community/comment" }
        if path.hasPrefix("/community/image_preview") { return "/community/image_preview" }
        if path.hasPrefix("/web") || path.hasPrefix("http") { return "/web" }
        if path.hasPrefix("/scan") { return "/scan" }
        return path
    }

    static let labelToPath: [String: String] = [
        "搜索": "/home/search",
        "全部服务": "/home/all_services", "更多": "/home/all_services",
        "投资策略": "/home/strategy", "策略": "/home/strategy",
        "学习报告": "/home/learning_report",
        "签到商城": "/home/check_in_mall", "积分商城": "/home/check_in_mall",
        "配音": "/home/dubbing_feed", "配音首页": "/home/dubbing_feed",
        "热榜": "/home/hot_rank_detail", "热配榜": "/home/hot_rank_detail",
        "生活服务": "/home/life_service",
        "直播带货": "/home/live_commerce", "直播": "/live",
        "Club": "/home/club",
        "二手车": "/home/used_car",
        "台账": "/home/ledger", "公司数据": "/home/ledger", "收支": "/home/ledger",
        "数据分析": "/home/data_analytics",
        "新伙伴待确认": "/home/todo/partner-pending",
        "待跟进客户": "/home/todo/follow-up-customers",
        "售后预约": "/home/todo/after-sales-appointments",
        "订单待审核": "/home/todo/order-pending-review",
        "售后": "/home/after_sales", "售后专区": "/home/after_sales",
        "新车跟进": "/home/new_car_follow", "新车成交": "/home/new_car_follow", "新车关注": "/home/new_car_follow",
        "销售顾问": "/home/todo/follow-up-customers",
        "AI小石头": "/ai/stream", "AI 小石": "/ai/stream", "AI小石": "/ai/stream",
        "订单中心": "/mall/orders",
        "营销活动": "/home/check_in_mall",
        "朋友圈营销": "/home/strategy",
        "扫一扫": "/scan",
        "消息": "/chat",
        "发布动态": "/community/publish", "社区发布": "/community/publish",
        "社区搜索": "/community/search",
        "商城": "/mall",
        "我的钱包": "/wallet",
        "我的课程": "/classroom/my_class",
        "我的订单": "/mall/orders",
        "短信模板": "/mine/sms_template",
        "购车计算器": "/mine/purchase_calculator",
        "小视频": "/video/short", "短视频": "/video/short",
        "店铺收款码": "/mine/store_qr",
        "选买问答": "/mine/qa",
        "商家海报": "/mine/poster",
        "商务合作": "/mine/business",
        "提醒事项": "/mine/reminders",
        "邀请好友": "/friend",
        "粉丝群": "/video/short",
        "意见反馈": "/mine/feedback",
        "收货地址": "/mine/addresses", "地址管理": "/mine/addresses", "地址": "/mine/addresses",
        "个人资料": "/mine/profile", "资料": "/mine/profile",
        "签到日历": "/home/check_in_mall", "签到": "/home/check_in_mall",
        "H5 调试": "/web", "内嵌网页": "/web",
        "音乐": "/music/list",
        "好友": "/friend", "通讯录": "/friend", "朋友": "/friend",
        "首页": "/home", "聊天": "/chat", "社区": "/community", "我的": "/mine",
        "设置": "/settings", "个性化": "/mine/personalized_settings",
        "会员": "/pay/membership", "关于": "/mine/about",
    ]
}

struct NativeFeaturePage: Identifiable {
    let id: String
    let title: String
    let subtitle: String?
    let rows: [NativeFeatureRow]
    let primaryAction: String?
}

struct NativeFeatureRow: Identifiable {
    let id: String
    let title: String
    let detail: String
    let badge: String?
}

enum NativeFeatureCatalog {
    static func page(for pathOrLabel: String) -> NativeFeaturePage {
        let path = NativeRouteResolver.resolve(pathOrLabel)
        if let built = builders[path] { return built() }
        let title = path.split(separator: "/").last.map(String.init) ?? pathOrLabel
        return NativeFeaturePage(
            id: path,
            title: title,
            subtitle: path,
            rows: [
                NativeFeatureRow(id: "1", title: "功能已接入原生壳", detail: "SwiftUI 实现，业务 mock 对齐 Flutter", badge: nil),
            ],
            primaryAction: nil
        )
    }

    private static let builders: [String: () -> NativeFeaturePage] = [
        "/home/search": { .init(id: "/home/search", title: "搜索", subtitle: nil, rows: [], primaryAction: nil) },
        "/home/all_services": { .init(id: "/home/all_services", title: "全部服务", subtitle: nil, rows: [], primaryAction: nil) },
        "/home/used_car": {
            .init(id: "/home/used_car", title: "二手车", subtitle: "置换 / 专卖 / 估价", rows: [
                .init(id: "1", title: "2019 凯美瑞 2.0G", detail: "里程 6.2 万 · 北京大兴", badge: "在售"),
                .init(id: "2", title: "2021 卡罗拉 双擎", detail: "里程 3.1 万 · 天津", badge: "已定"),
                .init(id: "3", title: "2018 RAV4 荣放", detail: "里程 9.8 万 · 廊坊", badge: "在售"),
            ], primaryAction: "发布车源")
        },
        "/home/life_service": {
            .init(id: "/home/life_service", title: "生活服务", subtitle: "洗车 · 保养 · 代驾", rows: [
                .init(id: "1", title: "精洗套餐", detail: "外观+内饰 · ¥99", badge: "热门"),
                .init(id: "2", title: "小保养预约", detail: "机油机滤 · 明日可约", badge: nil),
                .init(id: "3", title: "代驾上门", detail: "起步 5km · ¥35", badge: nil),
            ], primaryAction: "立即预约")
        },
        "/home/new_car_follow": {
            .init(id: "/home/new_car_follow", title: "新车关注", subtitle: "跟进中的意向客户", rows: [
                .init(id: "1", title: "王先生 · 凯美瑞", detail: "跟进中 · 预计本周到店", badge: "A"),
                .init(id: "2", title: "李女士 · 汉兰达", detail: "报价已发 · 待回访", badge: "B"),
            ], primaryAction: "新建跟进")
        },
        "/home/data_analytics": {
            .init(id: "/home/data_analytics", title: "数据分析", subtitle: "本周门店经营概览", rows: [
                .init(id: "1", title: "进店客流", detail: "328 · 环比 +12%", badge: nil),
                .init(id: "2", title: "成交台数", detail: "17 · 环比 +2", badge: nil),
                .init(id: "3", title: "线索转化", detail: "6.8% · 持平", badge: nil),
            ], primaryAction: nil)
        },
        "/home/live_commerce": {
            .init(id: "/home/live_commerce", title: "直播带货", subtitle: "本场预告与回放", rows: [
                .init(id: "1", title: "周末新车专场", detail: "今晚 20:00 · 预约 1.2k", badge: "预告"),
                .init(id: "2", title: "二手车清库直播", detail: "回放 · 观看 8.6k", badge: "回放"),
            ], primaryAction: "开播")
        },
        "/home/club": {
            .init(id: "/home/club", title: "Club", subtitle: "车友活动与圈子", rows: [
                .init(id: "1", title: "周末自驾·密云水库", detail: "报名 36 · 周六出发", badge: "报名中"),
                .init(id: "2", title: "店庆抽奖夜", detail: "本周五 · 门店大厅", badge: nil),
            ], primaryAction: "发布活动")
        },
        "/home/strategy": {
            .init(id: "/home/strategy", title: "投资策略", subtitle: "朋友圈营销话术", rows: [
                .init(id: "1", title: "周末到店礼", detail: "适合朋友圈 · 已用 128 次", badge: "推荐"),
                .init(id: "2", title: "置换补贴海报", detail: "适合群发 · 已用 56 次", badge: nil),
            ], primaryAction: "一键发圈")
        },
        "/home/learning_report": {
            .init(id: "/home/learning_report", title: "学习报告", subtitle: "本周学习时长", rows: [
                .init(id: "1", title: "产品知识", detail: "完成 4/5 · 86 分", badge: nil),
                .init(id: "2", title: "话术演练", detail: "完成 2/3 · 78 分", badge: nil),
            ], primaryAction: nil)
        },
        "/home/check_in_mall": {
            .init(id: "/home/check_in_mall", title: "签到商城", subtitle: "积分兑换好物", rows: [
                .init(id: "1", title: "洗车券 ×1", detail: "200 积分", badge: "可兑"),
                .init(id: "2", title: "香氛挂件", detail: "500 积分", badge: nil),
            ], primaryAction: "去签到")
        },
        "/home/dubbing_feed": {
            .init(id: "/home/dubbing_feed", title: "配音", subtitle: "热门配音素材", rows: [
                .init(id: "1", title: "小王子 · 片段 3", detail: "难度初级 · 女声", badge: "热"),
                .init(id: "2", title: "飞屋环游记 · OP", detail: "难度中级 · 男声", badge: nil),
            ], primaryAction: "开始配音")
        },
        "/home/hot_rank_detail": {
            .init(id: "/home/hot_rank_detail", title: "热配榜", subtitle: "今日热门", rows: [
                .init(id: "1", title: "穿条纹睡衣的男孩", detail: "热度 9821", badge: "1"),
                .init(id: "2", title: "蛮荒故事", detail: "热度 8740", badge: "2"),
                .init(id: "3", title: "爱冒险的朵拉", detail: "热度 7655", badge: "3"),
            ], primaryAction: nil)
        },
        "/home/ledger": {
            .init(id: "/home/ledger", title: "台账", subtitle: "公司经营数据", rows: [
                .init(id: "1", title: "9 月销售额", detail: "¥ 2,860,000", badge: nil),
                .init(id: "2", title: "毛利率", detail: "18.6%", badge: nil),
            ], primaryAction: nil)
        },
        "/home/todo/partner-pending": {
            .init(id: "/home/todo/partner-pending", title: "新伙伴待确认", subtitle: "3 位新成员", rows: [
                .init(id: "1", title: "赵倩 · 销售顾问", detail: "待审核 · 昨天申请", badge: "待审"),
                .init(id: "2", title: "孙浩 · 售后技师", detail: "待审核 · 今天申请", badge: "待审"),
            ], primaryAction: "全部通过")
        },
        "/home/todo/follow-up-customers": {
            .init(id: "/home/todo/follow-up-customers", title: "待跟进客户", subtitle: "今日 5 位意向", rows: [
                .init(id: "1", title: "陈先生", detail: "意向凯美瑞 · 未回访 2 天", badge: "紧急"),
                .init(id: "2", title: "周女士", detail: "询价汉兰达 · 今早留言", badge: nil),
            ], primaryAction: "开始跟进")
        },
        "/home/todo/after-sales-appointments": {
            .init(id: "/home/todo/after-sales-appointments", title: "售后预约", subtitle: "今日工位", rows: [
                .init(id: "1", title: "10:30 · 保养", detail: "京 A·88888 · 技师小刘", badge: nil),
                .init(id: "2", title: "14:00 · 钣喷", detail: "京 B·66666 · 技师老王", badge: nil),
            ], primaryAction: "新建预约")
        },
        "/home/todo/order-pending-review": {
            .init(id: "/home/todo/order-pending-review", title: "订单待审核", subtitle: "门店订单", rows: [
                .init(id: "1", title: "订单 #90821", detail: "定金 ¥5000 · 待店长审", badge: "待审"),
            ], primaryAction: nil)
        },
        "/home/after_sales": {
            .init(id: "/home/after_sales", title: "售后专区", subtitle: "维修保养记录", rows: [
                .init(id: "1", title: "京 A·12345 · 小保养", detail: "完成 · 2026-09-20", badge: "完成"),
                .init(id: "2", title: "京 C·54321 · 四轮定位", detail: "进行中", badge: "进行中"),
            ], primaryAction: "新建工单")
        },
        "/mall": {
            .init(id: "/mall", title: "商城", subtitle: "车品精选", rows: [
                .init(id: "1", title: "行车记录仪 Pro", detail: "¥ 399", badge: "热销"),
                .init(id: "2", title: "脚垫全包围", detail: "¥ 268", badge: nil),
                .init(id: "3", title: "玻璃水 2L×4", detail: "¥ 39", badge: nil),
            ], primaryAction: "去结算")
        },
        "/mall/orders": {
            .init(id: "/mall/orders", title: "我的订单", subtitle: nil, rows: [
                .init(id: "1", title: "订单 #A1024", detail: "待发货 · ¥399", badge: "待发货"),
                .init(id: "2", title: "订单 #A0988", detail: "已完成 · ¥268", badge: "完成"),
            ], primaryAction: nil)
        },
        "/mall/detail": {
            .init(id: "/mall/detail", title: "商品详情", subtitle: "积分兑换", rows: [
                .init(id: "1", title: "店庆纪念马克杯", detail: "39.90元 · 已兑2391", badge: "实物"),
                .init(id: "2", title: "规格", detail: "默认款 · 1 件", badge: nil),
            ], primaryAction: "立即兑换")
        },
        "/wallet": {
            .init(id: "/wallet", title: "我的钱包", subtitle: "可用余额", rows: [
                .init(id: "1", title: "余额", detail: "¥ 1,280.50", badge: nil),
                .init(id: "2", title: "积分", detail: "3,560", badge: nil),
                .init(id: "3", title: "优惠券", detail: "4 张可用", badge: nil),
            ], primaryAction: "充值")
        },
        "/video/short": {
            .init(id: "/video/short", title: "小视频", subtitle: "用小视频秀车秀店", rows: [
                .init(id: "1", title: "新车到店 · 15s", detail: "播放 2.1k · 赞 186", badge: nil),
                .init(id: "2", title: "保养小贴士", detail: "播放 980 · 赞 64", badge: nil),
            ], primaryAction: "拍一个")
        },
        "/video/short/play": {
            .init(id: "/video/short/play", title: "播放", subtitle: "全屏预览", rows: [
                .init(id: "1", title: "新车到店 · 15s", detail: "点赞 · 评论 · 分享", badge: nil),
            ], primaryAction: nil)
        },
        "/video/short/publish": {
            .init(id: "/video/short/publish", title: "发布小视频", subtitle: "选择素材", rows: [
                .init(id: "1", title: "封面", detail: "从相册选择", badge: nil),
                .init(id: "2", title: "标题", detail: "说说这一刻…", badge: nil),
            ], primaryAction: "发布")
        },
        "/video/short/help": {
            .init(id: "/video/short/help", title: "如何拍摄小视频", subtitle: "拍摄技巧", rows: [
                .init(id: "1", title: "竖屏构图", detail: "主体居中，光线充足", badge: nil),
                .init(id: "2", title: "时长建议", detail: "15–30 秒最佳", badge: nil),
            ], primaryAction: nil)
        },
        "/live": {
            .init(id: "/live", title: "直播", subtitle: "直播间列表", rows: [
                .init(id: "1", title: "沃德龙鼎直播间", detail: "在线 326 · 讲解新车", badge: "直播中"),
                .init(id: "2", title: "售后讲堂", detail: "预约 88 · 明天 19:00", badge: "预约"),
            ], primaryAction: "进入直播间")
        },
        "/friend": {
            .init(id: "/friend", title: "通讯录", subtitle: "好友与申请", rows: [
                .init(id: "1", title: "王同学", detail: "请求添加你为好友", badge: "新"),
                .init(id: "2", title: "李老师", detail: "请求添加你为好友", badge: "新"),
                .init(id: "3", title: "林林", detail: "一周前", badge: nil),
                .init(id: "4", title: "客服小助手", detail: "昨天", badge: nil),
            ], primaryAction: "建群")
        },
        "/ai/stream": {
            .init(id: "/ai/stream", title: "AI 小石头", subtitle: "智能问答（mock chunk）", rows: [
                .init(id: "1", title: "你", detail: "帮我写一条凯美瑞朋友圈", badge: nil),
                .init(id: "2", title: "小石头", detail: "周末到店看凯美瑞双擎，油耗惊喜，置换补贴进行中～", badge: "AI"),
            ], primaryAction: "继续对话")
        },
        "/classroom/my_class": {
            .init(id: "/classroom/my_class", title: "我的课程", subtitle: "班级与作业", rows: [
                .init(id: "1", title: "产品知识班", detail: "未交作业 2", badge: nil),
                .init(id: "2", title: "配音作业", detail: "待批改 1", badge: nil),
            ], primaryAction: "进入课堂")
        },
        "/classroom/homework": {
            .init(id: "/classroom/homework", title: "作业详情", subtitle: "待提交", rows: [
                .init(id: "1", title: "产品知识测验", detail: "截止今晚 22:00", badge: "待交"),
            ], primaryAction: "去完成")
        },
        "/music/list": {
            .init(id: "/music/list", title: "音乐", subtitle: "播放列表", rows: [
                .init(id: "1", title: "Night Drive", detail: "3:28", badge: nil),
                .init(id: "2", title: "Showroom BGM", detail: "2:51", badge: nil),
            ], primaryAction: "播放")
        },
        "/community/publish": {
            .init(id: "/community/publish", title: "发布动态", subtitle: "分享新鲜事", rows: [
                .init(id: "1", title: "正文", detail: "说说今天的见闻…", badge: nil),
                .init(id: "2", title: "话题", detail: "#Flutter开发 #户外", badge: nil),
            ], primaryAction: "发布")
        },
        "/community/search": {
            .init(id: "/community/search", title: "社区搜索", subtitle: "动态 · 话题 · 用户", rows: [
                .init(id: "1", title: "热门话题", detail: "#换车季 #保养日记", badge: nil),
            ], primaryAction: nil)
        },
        "/community/comment": {
            .init(id: "/community/comment", title: "评论", subtitle: "说说你的看法", rows: [
                .init(id: "1", title: "李四", detail: "说得对！", badge: nil),
                .init(id: "2", title: "赵六", detail: "同感 +1", badge: nil),
            ], primaryAction: "发送评论")
        },
        "/community/image_preview": {
            .init(id: "/community/image_preview", title: "图片预览", subtitle: nil, rows: [
                .init(id: "1", title: "预览图", detail: "点击返回", badge: nil),
            ], primaryAction: nil)
        },
        "/scan": {
            .init(id: "/scan", title: "扫一扫", subtitle: "对准二维码完成核销 / 收款", rows: [
                .init(id: "1", title: "对准二维码", detail: "用于门店收款码 / 活动核销", badge: nil),
            ], primaryAction: "打开相机")
        },
        "/web": {
            .init(id: "/web", title: "网页", subtitle: "离线 fixture", rows: [
                .init(id: "1", title: "离线网页", detail: "无网络也可显示的固定页面", badge: nil),
            ], primaryAction: nil)
        },
        "/mine/purchase_calculator": {
            .init(id: "/mine/purchase_calculator", title: "购车计算器", subtitle: "全款 / 贷款 / 保险", rows: [
                .init(id: "1", title: "车价", detail: "¥ 180,000", badge: nil),
                .init(id: "2", title: "首付 30%", detail: "¥ 54,000", badge: nil),
                .init(id: "3", title: "月供估算", detail: "¥ 5,830.00 / 36 期", badge: "结果"),
            ], primaryAction: "重新计算")
        },
        "/mine/sms_template": {
            .init(id: "/mine/sms_template", title: "短信模板", subtitle: "一键发送", rows: [
                .init(id: "1", title: "到店提醒", detail: "您好，您预约的试驾已确认…", badge: nil),
                .init(id: "2", title: "保养到期", detail: "爱车即将到保养周期…", badge: nil),
            ], primaryAction: "发送")
        },
        "/mine/store_qr": {
            .init(id: "/mine/store_qr", title: "店铺收款码", subtitle: "常见问题 · 功能介绍", rows: [
                .init(id: "1", title: "收款码", detail: "展示给客户扫码支付", badge: nil),
            ], primaryAction: "保存到相册")
        },
        "/mine/qa": {
            .init(id: "/mine/qa", title: "选买问答", subtitle: "在线解答客户问题", rows: [
                .init(id: "1", title: "双擎和汽油怎么选？", detail: "待回复 · 3 人围观", badge: "待回"),
            ], primaryAction: "去回答")
        },
        "/mine/poster": {
            .init(id: "/mine/poster", title: "商家海报", subtitle: "置换 / 专卖 / 估价", rows: [
                .init(id: "1", title: "秋季置换季", detail: "模板 · 可编辑文案", badge: nil),
            ], primaryAction: "生成海报")
        },
        "/mine/business": {
            .init(id: "/mine/business", title: "商务合作", subtitle: nil, rows: [
                .init(id: "1", title: "渠道合作", detail: "提交合作意向", badge: nil),
            ], primaryAction: "提交")
        },
        "/mine/reminders": {
            .init(id: "/mine/reminders", title: "提醒事项", subtitle: nil, rows: [
                .init(id: "1", title: "回访陈先生", detail: "今天 16:00", badge: nil),
                .init(id: "2", title: "提交周报", detail: "周五 18:00", badge: nil),
            ], primaryAction: "新建提醒")
        },
        "/mine/feedback": {
            .init(id: "/mine/feedback", title: "意见反馈", subtitle: nil, rows: [
                .init(id: "1", title: "反馈类型", detail: "功能建议 / 体验问题 / 其它", badge: nil),
            ], primaryAction: "提交反馈")
        },
        "/mine/profile": {
            .init(id: "/mine/profile", title: "个人资料", subtitle: nil, rows: [
                .init(id: "1", title: "昵称", detail: "qa_user", badge: nil),
                .init(id: "2", title: "职位", detail: "销售顾问", badge: nil),
            ], primaryAction: "保存")
        },
        "/mine/addresses": {
            .init(id: "/mine/addresses", title: "收货地址", subtitle: nil, rows: [
                .init(id: "1", title: "默认地址", detail: "北京市大兴区 · 兴荣丰田", badge: "默认"),
            ], primaryAction: "添加地址")
        },
    ]
}

/// Universal native secondary host — list + primary action (SwiftUI).
struct NativeFeatureHost: View {
    let pathOrLabel: String
    var onClose: () -> Void
    var onOpen: ((String) -> Void)? = nil

    private var page: NativeFeaturePage { NativeFeatureCatalog.page(for: pathOrLabel) }

    var body: some View {
        let path = NativeRouteResolver.resolve(pathOrLabel)
        if path == "/home/search" {
            NativeSearchPage(onClose: onClose)
        } else if path == "/home/all_services" {
            NativeAllServicesPage(onClose: onClose, onOpen: { onOpen?($0) })
        } else if path == "/home/learning_report" || pathOrLabel == "学习报告" {
            NativeLearningReportPage(onClose: onClose)
        } else if path == "/mine/purchase_calculator" || pathOrLabel == "购车计算器" {
            NativePurchaseCalculatorPage(onClose: onClose)
        } else if path == "/friend" {
            NativeFriendPage(onClose: onClose, onOpen: onOpen)
        } else if path == "/community/search" {
            NativeCommunitySearchPage(onClose: onClose)
        } else if path == "/community/publish" {
            NativeCommunityPublishPage(onClose: onClose)
        } else if path == "/community/comment" {
            NativeCommunityCommentPage(onClose: onClose)
        } else if path == "/community/image_preview" {
            NativeCommunityImagePreviewPage(onClose: onClose)
        } else if path == "/mall" {
            NativeMallPage(onClose: onClose, onOpen: onOpen)
        } else if path == "/wallet" {
            NativeWalletPage(onClose: onClose)
        } else if path == "/video/short" {
            NativeShortVideoPage(onClose: onClose, onOpen: onOpen)
        } else if path == "/live" || path == "/home/live_commerce" {
            NativeLivePage(onClose: onClose)
        } else if path == "/ai/stream" {
            NativeAiStreamPage(onClose: onClose)
        } else if path == "/scan" {
            NativeScanPage(onClose: onClose)
        } else if path == "/home/strategy" {
            NativeStrategyPage(onClose: onClose)
        } else if path == "/home/check_in_mall" {
            NativeCheckInMallPage(onClose: onClose)
        } else if path == "/music/list" {
            NativeMusicPage(onClose: onClose)
        } else if path == "/classroom/my_class" {
            NativeClassroomPage(onClose: onClose, onOpen: onOpen)
        } else {
            genericList
        }
    }

    private var genericList: some View {
        VStack(spacing: 0) {
            HStack {
                Button {
                    onClose()
                } label: {
                    Image(systemName: "chevron.left").foregroundStyle(DesignTokens.link)
                }
                VStack(alignment: .leading, spacing: 2) {
                    Text(page.title, font: .system(size: 17, weight: .semibold), color: DesignTokens.ink)
                    if let subtitle = page.subtitle {
                        Text(subtitle, font: .system(size: 12), color: DesignTokens.body)
                    }
                }
                Spacer()
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .background(DesignTokens.canvas)

            ScrollView {
                VStack(spacing: 0) {
                    ForEach(page.rows) { row in
                        HStack(alignment: .top, spacing: 12) {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(row.title, font: .system(size: 16, weight: .semibold), color: DesignTokens.ink)
                                Text(row.detail, font: .system(size: 13), color: DesignTokens.body)
                            }
                            Spacer()
                            if let badge = row.badge {
                                Text(badge, font: .system(size: 11, weight: .medium), color: DesignTokens.link)
                                    .padding(.horizontal, 8)
                                    .padding(.vertical, 4)
                                    .background(DesignTokens.link.opacity(0.12), in: Capsule())
                            }
                        }
                        .padding(16)
                        Divider().overlay(DesignTokens.hairline)
                    }
                }
                .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 12))
                .padding(16)

                if let action = page.primaryAction {
                    Button(action) {}
                        .buttonStyle(.borderedProminent)
                        .tint(DesignTokens.link)
                        .padding(.bottom, 24)
                }
            }
        }
        .background(DesignTokens.canvasSoft2.ignoresSafeArea())
    }
}

import SwiftUI

/// Native SwiftUI secondary pages for high-traffic Home entries (ADR 0002).
/// Aligned to Flutter module_home + Compose HomeSearchScreen / AllServicesScreen.

struct NativeSearchPage: View {
    var onClose: () -> Void
    @State private var query = ""
    @State private var history = HomeNativeMock.searchHistory
    @State private var discovery = HomeNativeMock.searchDiscovery
    @State private var rankTab = 0

    var body: some View {
        VStack(spacing: 0) {
            HStack(spacing: 12) {
                Button("返回", action: onClose).foregroundStyle(DesignTokens.link)
                HStack(spacing: 8) {
                    Image(systemName: "magnifyingglass")
                        .font(.system(size: 14))
                        .foregroundStyle(DesignTokens.body)
                    TextField("搜索客户、订单、资讯", text: $query)
                        .font(.system(size: 15))
                }
                .padding(.horizontal, 12)
                .frame(height: 40)
                .background(DesignTokens.canvasSoft2, in: RoundedRectangle(cornerRadius: 10))
                .overlay(RoundedRectangle(cornerRadius: 10).stroke(DesignTokens.hairline, lineWidth: 0.5))
                Button("搜索") {
                    let text = query.trimmingCharacters(in: .whitespacesAndNewlines)
                    guard !text.isEmpty else { return }
                    history = [text] + history.filter { $0 != text }.prefix(9)
                }
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(DesignTokens.link)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 10)
            .background(DesignTokens.canvas)

            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    if !history.isEmpty {
                        chipSection(title: "搜索历史", action: "清除") {
                            history.removeAll()
                        } chips: {
                            ForEach(history, id: \.self) { chip in
                                chipView(chip).onTapGesture { query = chip }
                            }
                        }
                    }

                    chipSection(title: "搜索发现", action: "换一批") {
                        discovery.reverse()
                    } chips: {
                        ForEach(discovery, id: \.self) { chip in
                            chipView(chip).onTapGesture { query = chip }
                        }
                    }

                    VStack(alignment: .leading, spacing: 10) {
                        Text("筛选标签", font: .system(size: 15, weight: .semibold), color: DesignTokens.ink)
                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 8) {
                                ForEach(HomeNativeMock.searchFilters, id: \.self) { label in
                                    Text(label, font: .system(size: 13), color: DesignTokens.link)
                                        .padding(.horizontal, 12)
                                        .padding(.vertical, 6)
                                        .overlay(Capsule().stroke(DesignTokens.link.opacity(0.5), lineWidth: 1))
                                        .onTapGesture { query = label }
                                }
                            }
                        }
                    }

                    // Rank tabs — Flutter SearchRankTabBar
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 20) {
                            ForEach(Array(HomeNativeMock.rankTabs.enumerated()), id: \.offset) { index, label in
                                VStack(spacing: 6) {
                                    Text(
                                        label,
                                        font: .system(size: 15, weight: rankTab == index ? .semibold : .regular),
                                        color: rankTab == index ? DesignTokens.ink : DesignTokens.body
                                    )
                                    Capsule()
                                        .fill(rankTab == index ? DesignTokens.link : Color.clear)
                                        .frame(width: 20, height: 3)
                                }
                                .onTapGesture { rankTab = index }
                            }
                        }
                    }

                    VStack(spacing: 0) {
                        let ranks = HomeNativeMock.rankItems(for: rankTab)
                        ForEach(Array(ranks.enumerated()), id: \.offset) { index, item in
                            HStack(spacing: 12) {
                                Text("\(index + 1)", font: .system(size: 16, weight: .bold), color: index < 3 ? Color.red : DesignTokens.body)
                                    .frame(width: 24)
                                RoundedRectangle(cornerRadius: 6)
                                    .fill(DesignTokens.link.opacity(0.12))
                                    .frame(width: 40, height: 40)
                                    .overlay(Text(String(item.0.prefix(1))).foregroundStyle(DesignTokens.link))
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(item.0, font: .system(size: 15, weight: .semibold), color: DesignTokens.ink)
                                    Text(item.1, font: .system(size: 12), color: DesignTokens.body).lineLimit(1)
                                }
                                Spacer()
                            }
                            .padding(12)
                            .contentShape(Rectangle())
                            .onTapGesture { query = item.0 }
                            if index != ranks.count - 1 {
                                Divider().overlay(DesignTokens.hairline).padding(.leading, 48)
                            }
                        }
                    }
                    .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 12))
                    .overlay(RoundedRectangle(cornerRadius: 12).stroke(DesignTokens.hairline, lineWidth: 0.5))
                }
                .padding(16)
            }
        }
        .background(DesignTokens.canvasSoft2.ignoresSafeArea())
    }

    private func chipSection(
        title: String,
        action: String,
        actionBlock: @escaping () -> Void,
        @ViewBuilder chips: () -> some View
    ) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text(title, font: .system(size: 15, weight: .semibold), color: DesignTokens.ink)
                Spacer()
                Button(action, action: actionBlock).font(.system(size: 13)).foregroundStyle(DesignTokens.link)
            }
            FlowHStack { chips() }
        }
    }

    private func chipView(_ label: String) -> some View {
        Text(label, font: .system(size: 13), color: DesignTokens.ink)
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            .background(DesignTokens.hairline.opacity(0.6), in: Capsule())
    }
}

/// Simple wrapping HStack for chips.
private struct FlowHStack<Content: View>: View {
    @ViewBuilder var content: Content
    var body: some View {
        // Use horizontal scroll as Flutter TagFlow fallback when wrapping is complex
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) { content }
        }
    }
}

struct NativeAllServicesPage: View {
    var onClose: () -> Void
    var onOpen: (String) -> Void
    @State private var isEditing = false
    @State private var favoriteIds: Set<String> = Set(HomeNativeMock.favoriteServices.map(\.id))

    private var favoriteItems: [HomeNativeMock.ServiceItem] {
        let all = HomeNativeMock.favoriteServices + HomeNativeMock.catalogSections.flatMap(\.items)
        let byId = Dictionary(uniqueKeysWithValues: all.map { ($0.id, $0) })
        return favoriteIds.compactMap { byId[$0] }
    }

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Button {
                    onClose()
                } label: {
                    Image(systemName: "chevron.left").foregroundStyle(DesignTokens.link)
                }
                Text("全部服务", font: .system(size: 17, weight: .semibold), color: DesignTokens.ink)
                Spacer()
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .background(DesignTokens.canvas)

            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    sectionBlock(
                        title: "常用服务",
                        subtitle: "将按自定义顺序出现在首页",
                        showEdit: true,
                        items: favoriteItems.isEmpty ? Array(HomeNativeMock.favoriteServices.prefix(3)) : favoriteItems,
                        isFavoriteSection: true
                    )
                    ForEach(HomeNativeMock.catalogSections, id: \.title) { section in
                        sectionBlock(
                            title: section.title,
                            subtitle: nil,
                            showEdit: false,
                            items: section.items,
                            isFavoriteSection: false
                        )
                    }
                }
                .padding(16)
            }
        }
        .background(DesignTokens.canvasSoft2.ignoresSafeArea())
    }

    private func sectionBlock(
        title: String,
        subtitle: String?,
        showEdit: Bool,
        items: [HomeNativeMock.ServiceItem],
        isFavoriteSection: Bool
    ) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text(title, font: .system(size: 16, weight: .semibold), color: DesignTokens.ink)
                Spacer()
                if showEdit {
                    Button(isEditing ? "完成" : "编辑") {
                        isEditing.toggle()
                    }
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle(DesignTokens.link)
                }
            }
            if let subtitle {
                Text(subtitle, font: .system(size: 12), color: DesignTokens.body)
            }
            LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 4), spacing: 16) {
                ForEach(items) { item in
                    ZStack(alignment: .topTrailing) {
                        VStack(spacing: 8) {
                            Image(item.asset)
                                .resizable()
                                .scaledToFit()
                                .frame(width: 48, height: 48)
                                .clipShape(RoundedRectangle(cornerRadius: 12))
                            Text(item.label, font: .system(size: 12), color: DesignTokens.ink)
                                .lineLimit(1)
                                .minimumScaleFactor(0.8)
                        }
                        .frame(maxWidth: .infinity)
                        .onTapGesture {
                            if isEditing {
                                toggleFavorite(item)
                            } else {
                                onOpen(item.label)
                            }
                        }
                        if isEditing {
                            let inFav = favoriteIds.contains(item.id)
                            Text(isFavoriteSection || inFav ? "−" : "+")
                                .font(.system(size: 12, weight: .bold))
                                .foregroundStyle(.white)
                                .frame(width: 18, height: 18)
                                .background((isFavoriteSection || inFav) ? Color.red : DesignTokens.link, in: Circle())
                                .offset(x: 4, y: -4)
                                .onTapGesture { toggleFavorite(item) }
                        }
                    }
                }
            }
        }
        .padding(16)
        .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 12))
    }

    private func toggleFavorite(_ item: HomeNativeMock.ServiceItem) {
        if favoriteIds.contains(item.id) {
            if favoriteIds.count > 3 { favoriteIds.remove(item.id) }
        } else if favoriteIds.count < 8 {
            favoriteIds.insert(item.id)
        }
    }
}

// MARK: - Learning report / calculator / friend / community search

struct NativeLearningReportPage: View {
    var onClose: () -> Void
    private let highlights = HomeNativeMock.reportHighlights
    private let records = HomeNativeMock.reportRecords

    var body: some View {
        VStack(spacing: 0) {
            navBar(title: "学习报告", onClose: onClose, dark: true)
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    Text("今日高光", font: .system(size: 16, weight: .semibold), color: Color(white: 0.95))
                    ForEach(highlights, id: \.0) { h in
                        HStack(spacing: 12) {
                            Text(h.0).font(.system(size: 28))
                            VStack(alignment: .leading, spacing: 4) {
                                Text(h.1, font: .system(size: 15, weight: .semibold), color: .white)
                                Text(h.2, font: .system(size: 12), color: Color(white: 0.55))
                            }
                            Spacer()
                            Text(h.3, font: .system(size: 14), color: Color(red: 1, green: 0.54, blue: 0.2))
                        }
                        .padding(14)
                        .background(Color(red: 0.07, green: 0.1, blue: 0.12), in: RoundedRectangle(cornerRadius: 14))
                    }
                    Text("学习记录", font: .system(size: 16, weight: .semibold), color: Color(white: 0.95))
                        .padding(.top, 8)
                    VStack(spacing: 0) {
                        ForEach(Array(records.enumerated()), id: \.offset) { index, r in
                            HStack(spacing: 12) {
                                Text(r.0).font(.system(size: 22))
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(r.1, font: .system(size: 15, weight: .semibold), color: .white)
                                    Text(r.2, font: .system(size: 12), color: Color(white: 0.55))
                                }
                                Spacer()
                                VStack(alignment: .trailing, spacing: 2) {
                                    Text(r.3, font: .system(size: 11), color: Color(white: 0.45))
                                    Text(r.4, font: .system(size: 12), color: r.5 ? Color(red: 1, green: 0.54, blue: 0.2) : Color(white: 0.55))
                                }
                            }
                            .padding(14)
                            if index != records.count - 1 {
                                Divider().overlay(Color(white: 0.16))
                            }
                        }
                    }
                    .background(Color(red: 0.09, green: 0.09, blue: 0.12), in: RoundedRectangle(cornerRadius: 14))
                }
                .padding(16)
            }
        }
        .background(Color(red: 0.04, green: 0.05, blue: 0.07).ignoresSafeArea())
    }
}

struct NativePurchaseCalculatorPage: View {
    var onClose: () -> Void
    @State private var price = "180000"
    @State private var downPercent = 30.0
    @State private var months = 36.0

    private var priceValue: Double { Double(price) ?? 180_000 }
    private var downPayment: Double { priceValue * downPercent / 100 }
    private var loan: Double { priceValue - downPayment }
    private var monthly: Double {
        guard months > 0 else { return 0 }
        // Simple equal principal+interest approximation (Flutter mock style)
        let r = 0.045 / 12
        let n = months
        let factor = pow(1 + r, n)
        return loan * r * factor / (factor - 1)
    }

    var body: some View {
        VStack(spacing: 0) {
            navBar(title: "购车计算器", onClose: onClose, dark: false)
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    field("车价（元）", text: $price)
                    VStack(alignment: .leading, spacing: 8) {
                        Text("首付 \(Int(downPercent))%", font: .system(size: 14), color: DesignTokens.ink)
                        Slider(value: $downPercent, in: 10...60, step: 5).tint(DesignTokens.link)
                    }
                    VStack(alignment: .leading, spacing: 8) {
                        Text("期数 \(Int(months)) 期", font: .system(size: 14), color: DesignTokens.ink)
                        Slider(value: $months, in: 12...60, step: 12).tint(DesignTokens.link)
                    }
                    resultRow("首付", String(format: "¥ %.0f", downPayment))
                    resultRow("贷款", String(format: "¥ %.0f", loan))
                    resultRow("月供估算", String(format: "¥ %.2f / %d 期", monthly, Int(months)))
                }
                .padding(16)
            }
        }
        .background(DesignTokens.canvasSoft2.ignoresSafeArea())
    }

    private func field(_ title: String, text: Binding<String>) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title, font: .system(size: 14), color: DesignTokens.body)
            TextField(title, text: text)
                .keyboardType(.numberPad)
                .padding(12)
                .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 8))
                .overlay(RoundedRectangle(cornerRadius: 8).stroke(DesignTokens.hairline, lineWidth: 0.5))
        }
    }

    private func resultRow(_ title: String, _ value: String) -> some View {
        HStack {
            Text(title, font: .system(size: 15), color: DesignTokens.body)
            Spacer()
            Text(value, font: .system(size: 16, weight: .semibold), color: DesignTokens.ink)
        }
        .padding(14)
        .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 8))
    }
}

struct NativeFriendPage: View {
    var onClose: () -> Void
    var onOpen: ((String) -> Void)? = nil
    private let friends = [
        ("王同学", "请求添加你为好友", "新"),
        ("李老师", "请求添加你为好友", "新"),
        ("林林", "一周前", ""),
        ("客服小助手", "昨天", ""),
    ]

    var body: some View {
        VStack(spacing: 0) {
            navBar(title: "通讯录", onClose: onClose, dark: false)
            List {
                ForEach(Array(friends.enumerated()), id: \.offset) { _, f in
                    HStack {
                        Circle()
                            .fill(DesignTokens.link.opacity(0.15))
                            .frame(width: 44, height: 44)
                            .overlay(Text(String(f.0.suffix(1))).foregroundStyle(DesignTokens.link))
                        VStack(alignment: .leading, spacing: 2) {
                            Text(f.0, font: .system(size: 16, weight: .semibold), color: DesignTokens.ink)
                            Text(f.1, font: .system(size: 13), color: DesignTokens.body)
                        }
                        Spacer()
                        if !f.2.isEmpty {
                            Text(f.2, font: .system(size: 11), color: DesignTokens.link)
                                .padding(.horizontal, 8)
                                .padding(.vertical, 3)
                                .background(DesignTokens.link.opacity(0.12), in: Capsule())
                        }
                    }
                    .contentShape(Rectangle())
                    .onTapGesture { onOpen?("/chat") }
                }
            }
            .listStyle(.plain)
        }
        .background(DesignTokens.canvasSoft2.ignoresSafeArea())
    }
}

struct NativeCommunitySearchPage: View {
    var onClose: () -> Void
    @State private var query = ""
    @State private var tab = 0
    private let tabs = ["动态", "话题", "用户"]

    var body: some View {
        VStack(spacing: 0) {
            HStack(spacing: 12) {
                Button("取消", action: onClose).foregroundStyle(DesignTokens.link)
                HStack(spacing: 8) {
                    Image(systemName: "magnifyingglass").foregroundStyle(DesignTokens.body)
                    TextField("搜索动态、话题、用户", text: $query)
                }
                .padding(.horizontal, 12)
                .frame(height: 40)
                .background(DesignTokens.canvasSoft2, in: RoundedRectangle(cornerRadius: 10))
            }
            .padding(12)
            .background(DesignTokens.canvas)

            HStack {
                ForEach(Array(tabs.enumerated()), id: \.offset) { index, label in
                    Text(label, font: .system(size: 15, weight: tab == index ? .semibold : .regular),
                          color: tab == index ? DesignTokens.link : DesignTokens.body)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 10)
                        .overlay(alignment: .bottom) {
                            Rectangle()
                                .fill(tab == index ? DesignTokens.link : Color.clear)
                                .frame(height: 2)
                        }
                        .onTapGesture { tab = index }
                }
            }
            .background(DesignTokens.canvas)

            List {
                if tab == 0 {
                    Text("张三：周末 hiking #户外")
                    Text("李四：新车到店试驾")
                } else if tab == 1 {
                    Text("#换车季")
                    Text("#保养日记")
                    Text("#Flutter开发")
                } else {
                    Text("张三 · 粉丝 128")
                    Text("李四 · 粉丝 86")
                }
            }
            .listStyle(.plain)
        }
        .background(DesignTokens.canvasSoft2.ignoresSafeArea())
    }
}

private func navBar(title: String, onClose: @escaping () -> Void, dark: Bool) -> some View {
    HStack {
        Button {
            onClose()
        } label: {
            Image(systemName: "chevron.left")
                .foregroundStyle(dark ? Color.white : DesignTokens.link)
        }
        Text(title, font: .system(size: 17, weight: .semibold), color: dark ? .white : DesignTokens.ink)
        Spacer()
    }
    .padding(.horizontal, 16)
    .padding(.vertical, 12)
    .background(dark ? Color(red: 0.04, green: 0.05, blue: 0.07) : DesignTokens.canvas)
}

enum HomeNativeMock {
    struct ServiceItem: Identifiable, Hashable {
        let id: String
        let label: String
        let asset: String
    }

    struct ServiceSection: Identifiable {
        var id: String { title }
        let title: String
        let items: [ServiceItem]
    }

    static let searchHistory = ["极限文字一排两个显示", "极限文字超出九个字...", "宫崎骏宫漫作品", "龙猫", "闪光少女"]
    static let searchDiscovery = ["罗振宇2026跨年演讲", "极限文字超出九个字...", "小猪佩奇全系列", "百家讲坛全集", "百家讲坛明朝"]
    static let searchFilters = ["3-5 个句子的配音", "较慢的语速", "初级难度", "女声", "1 分钟以内的视频"]
    static let rankTabs = ["热配榜", "诵读榜", "剧集榜", "记录榜", "合作榜"]

    static func rankItems(for tab: Int) -> [(String, String)] {
        switch tab {
        case 1: return [("静夜思", "床前明月光，疑是地上霜..."), ("春晓", "春眠不觉晓，处处闻啼鸟..."), ("登鹳雀楼", "白日依山尽，黄河入海流..."), ("望庐山瀑布", "日照香炉生紫烟..."), ("悯农", "锄禾日当午，汗滴禾下土...")]
        case 2: return [("小猪佩奇", "佩奇和乔治的日常生活..."), ("汪汪队立大功", "莱德队长带领狗狗们救援..."), ("超级飞侠", "乐迪环游世界送包裹..."), ("熊出没", "熊大熊二与光头强..."), ("喜羊羊与灰太狼", "羊村与狼堡的欢乐对决...")]
        case 3: return [("我的第一次配音", "完成度 98%，发音清晰自然..."), ("英语朗读打卡", "连续打卡 30 天..."), ("亲子共读记录", "与孩子一起完成的温馨朗读..."), ("班级作业精选", "老师推荐的优秀作业..."), ("周末练习成果", "周末集中练习成果汇总...")]
        case 4: return [("BBC 合作专区", "BBC 精选纪录片配音素材..."), ("迪士尼经典合作", "迪士尼动画经典片段..."), ("国家地理探索", "探索自然与科学的配音..."), ("牛津阅读树", "分级阅读配套配音练习..."), ("剑桥少儿英语", "剑桥体系标准发音示范...")]
        default: return [
            ("穿条纹睡衣的男孩", "某日布鲁诺决定，去铁丝网的另外…"),
            ("蛮荒故事", "六个独立故事，荒诞与黑色幽默交织…"),
            ("爱冒险的朵拉", "和朵拉一起开启奇妙冒险之旅…"),
            ("小王子", "来自 B612 小行星的小王子…"),
            ("寻梦环游记", "米格在亡灵节追寻音乐梦想…"),
            ("飞屋环游记", "卡尔用气球带着房子去冒险…"),
            ("头脑特工队", "情绪小人在大脑里协作成长…"),
            ("疯狂动物城", "兔子警官与狐狸搭档破案…"),
        ]
        }
    }

    static let favoriteServices: [ServiceItem] = [
        .init(id: "intro", label: "引导动画", asset: "home_all_services_smart_online_marketing"),
        .init(id: "glass", label: "玻璃卡片", asset: "home_all_services_online_customer_acquisition"),
        .init(id: "diet", label: "地中海饮食", asset: "home_all_services_small_video"),
        .init(id: "drawer", label: "侧滑导航", asset: "home_all_services_service_management"),
        .init(id: "diary", label: "我的日记", asset: "home_all_services_exhibition_hall_shooting"),
        .init(id: "training", label: "训练计划", asset: "home_all_services_intelligence_task"),
        .init(id: "running", label: "跑步数据", asset: "home_all_services_new_car_in_store"),
        .init(id: "wave", label: "波浪动画", asset: "home_all_services_smart_number"),
    ]

    static let catalogSections: [ServiceSection] = [
        .init(title: "线索服务", items: [
            .init(id: "intro", label: "引导动画", asset: "home_all_services_smart_online_marketing"),
            .init(id: "hotel", label: "酒店预订", asset: "home_all_services_customer_profile"),
            .init(id: "filters", label: "酒店筛选", asset: "home_all_services_smart_sale"),
            .init(id: "fitness", label: "健身应用", asset: "home_all_services_new_car_deal"),
            .init(id: "glass", label: "玻璃卡片", asset: "home_all_services_online_customer_acquisition"),
            .init(id: "running", label: "跑步数据", asset: "home_all_services_new_car_in_store"),
            .init(id: "wave", label: "波浪动画", asset: "home_all_services_smart_number"),
        ]),
        .init(title: "营销服务", items: [
            .init(id: "diary", label: "我的日记", asset: "home_all_services_exhibition_hall_shooting"),
            .init(id: "design", label: "设计课程", asset: "home_all_services_marketing"),
            .init(id: "training", label: "训练计划", asset: "home_all_services_intelligence_task"),
            .init(id: "workout", label: "训练视图", asset: "home_all_services_v_store"),
            .init(id: "diet", label: "地中海饮食", asset: "home_all_services_small_video"),
            .init(id: "course", label: "课程详情", asset: "home_all_services_business_poster"),
        ]),
        .init(title: "车商工具", items: [
            .init(id: "calc", label: "购车计算器", asset: "home_all_services_calculator"),
            .init(id: "used", label: "二手车", asset: "home_all_services_used_car"),
            .init(id: "after", label: "售后专区", asset: "home_all_services_after_sales_area"),
            .init(id: "all", label: "全部功能", asset: "home_all_services_all_functions"),
        ]),
    ]

    static let reportHighlights: [(String, String, String, String)] = [
        ("🎬", "《哈利波特》第3章", "视频配音 · 刚刚发布", "100"),
        ("🏆", "解锁「45天」打卡勋章", "里程碑达成 · 太棒了！", "🎉"),
        ("🎵", "\"Wingardium Leviosa!\"", "满分句子 · 可播放原声", "▶"),
    ]

    static let reportRecords: [(String, String, String, String, String, Bool)] = [
        ("🎬", "视频配音", "哈利波特 第3章", "18:32", "已发布", true),
        ("⚔️", "配音闯关", "Level 8 · 3关", "17:10", "15 min", false),
        ("📚", "同步练", "PEP 五年级上册 Unit 3", "16:45", "8 min", false),
        ("🤖", "AI 外教", "自由对话 · Tom老师", "15:20", "12 min", false),
        ("🎧", "听力练习", "英美绕口令 · 5题", "14:00", "5 min", false),
    ]
}

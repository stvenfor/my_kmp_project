import SwiftUI

/// Native SwiftUI secondary pages for high-traffic Home entries (ADR 0002).
/// Not Compose — owned by iosApp shell.

struct NativeSearchPage: View {
    var onClose: () -> Void
    @State private var query = ""
    @State private var history = HomeNativeMock.searchHistory
    private let discovery = HomeNativeMock.searchDiscovery
    private let filters = HomeNativeMock.searchFilters
    private let ranks = HomeNativeMock.hotRanks

    var body: some View {
        VStack(spacing: 0) {
            HStack(spacing: 12) {
                Button("返回", action: onClose).foregroundStyle(DesignTokens.link)
                HStack(spacing: 8) {
                    Text("⌕", font: .system(size: 16), color: DesignTokens.body)
                    TextField("搜索客户、订单、资讯", text: $query)
                }
                .padding(.horizontal, 12)
                .frame(height: 40)
                .background(DesignTokens.canvasSoft2, in: RoundedRectangle(cornerRadius: 8))
                Button("搜索") {}.foregroundStyle(DesignTokens.link)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 10)
            .background(DesignTokens.canvas)

            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    chipSection(title: "搜索历史", action: "清除") {
                        history.removeAll()
                    } chips: {
                        ForEach(history, id: \.self) { chipView($0) }
                    }
                    chipSection(title: "搜索发现", action: "换一批") {} chips: {
                        ForEach(discovery, id: \.self) { chipView($0) }
                    }
                    VStack(alignment: .leading, spacing: 10) {
                        Text("筛选标签", font: .system(size: 15, weight: .semibold), color: DesignTokens.ink)
                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 8) {
                                ForEach(filters, id: \.self) { label in
                                    Text(label, font: .system(size: 13), color: DesignTokens.link)
                                        .padding(.horizontal, 12)
                                        .padding(.vertical, 6)
                                        .overlay(Capsule().stroke(DesignTokens.link.opacity(0.5), lineWidth: 1))
                                }
                            }
                        }
                    }
                    VStack(alignment: .leading, spacing: 12) {
                        Text("热配榜", font: .system(size: 15, weight: .semibold), color: DesignTokens.ink)
                        VStack(spacing: 0) {
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
                                if index != ranks.count - 1 {
                                    Divider().overlay(DesignTokens.hairline)
                                }
                            }
                        }
                        .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 12))
                    }
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
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) { chips() }
            }
        }
    }

    private func chipView(_ label: String) -> some View {
        Text(label, font: .system(size: 13), color: DesignTokens.ink)
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            .background(DesignTokens.hairline.opacity(0.6), in: Capsule())
    }
}

struct NativeAllServicesPage: View {
    var onClose: () -> Void
    var onOpen: (String) -> Void

    private let favorites = HomeNativeMock.favoriteServices
    private let sections = HomeNativeMock.catalogSections

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
                    sectionBlock(title: "常用服务", subtitle: "将按自定义顺序出现在首页", items: favorites)
                    ForEach(sections, id: \.0) { section in
                        sectionBlock(title: section.0, subtitle: nil, items: section.1)
                    }
                }
                .padding(16)
            }
        }
        .background(DesignTokens.canvasSoft2.ignoresSafeArea())
    }

    private func sectionBlock(title: String, subtitle: String?, items: [String]) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(title, font: .system(size: 16, weight: .semibold), color: DesignTokens.ink)
            if let subtitle {
                Text(subtitle, font: .system(size: 12), color: DesignTokens.body)
            }
            LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 4), spacing: 16) {
                ForEach(items, id: \.self) { label in
                    VStack(spacing: 8) {
                        Circle()
                            .fill(DesignTokens.link.opacity(0.12))
                            .frame(width: 48, height: 48)
                            .overlay(Text(String(label.prefix(1))).foregroundStyle(DesignTokens.link))
                        Text(label, font: .system(size: 12), color: DesignTokens.ink)
                            .lineLimit(1)
                    }
                    .onTapGesture { onOpen(label) }
                }
            }
        }
        .padding(16)
        .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 12))
    }
}

enum HomeNativeMock {
    static let searchHistory = ["极限文字一排两个显示", "极限文字超出九个字...", "宫崎骏宫..."]
    static let searchDiscovery = ["罗振宇2026跨年演讲", "极限文字超出九个字...", "小猪佩奇全..."]
    static let searchFilters = ["3-5 个句子的配音", "较慢的语速", "初级难度", "女声"]
    static let hotRanks: [(String, String)] = [
        ("穿条纹睡衣的男孩", "某日布鲁诺决定，去铁丝网的另外…"),
        ("蛮荒故事", "六个独立故事，荒诞与黑色幽默交织…"),
        ("爱冒险的朵拉", "和朵拉一起开启奇妙冒险之旅…"),
        ("小王子", "来自 B612 小行星的小王子…"),
        ("寻梦环游记", "米格在亡灵节追寻音乐梦想…"),
        ("飞屋环游记", "卡尔用气球带着房子去冒险…"),
    ]
    static let favoriteServices = ["销售顾问", "生活服务", "二手车", "新车关注", "AI小石头", "订单中心", "数据分析", "直播带货"]
    static let catalogSections: [(String, [String])] = [
        ("车商工具", ["购车计算器", "短信模板", "店铺收款码", "商家海报"]),
        ("内容运营", ["小视频", "选买问答", "营销活动", "Club"]),
        ("售后服务", ["售后专区", "我的课程", "我的订单", "我的钱包"]),
    ]
}

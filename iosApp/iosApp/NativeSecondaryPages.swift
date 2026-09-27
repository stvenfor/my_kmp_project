import SwiftUI
import UIKit

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
        ZStack(alignment: .bottom) {
            VStack(spacing: 0) {
                navBar(title: "学习报告", onClose: onClose, dark: true)
                ScrollView {
                    VStack(alignment: .leading, spacing: 16) {
                        Text("今日高光", font: .system(size: 16, weight: .semibold), color: Color(white: 0.95))
                        VStack(spacing: 0) {
                            ForEach(Array(highlights.enumerated()), id: \.offset) { index, h in
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
                                if index != highlights.count - 1 {
                                    Divider().overlay(Color(white: 0.16))
                                }
                            }
                        }
                        .background(Color(red: 0.07, green: 0.10, blue: 0.12), in: RoundedRectangle(cornerRadius: 14))

                        Text("今日学习记录", font: .system(size: 16, weight: .semibold), color: Color(white: 0.95))
                        VStack(spacing: 0) {
                            ForEach(Array(records.enumerated()), id: \.offset) { index, r in
                                HStack(spacing: 12) {
                                    Text(r.0).font(.system(size: 22))
                                    VStack(alignment: .leading, spacing: 4) {
                                        Text(r.1, font: .system(size: 15, weight: .semibold), color: .white)
                                        Text(r.2, font: .system(size: 12), color: Color(white: 0.55))
                                    }
                                    Spacer()
                                    VStack(alignment: .trailing, spacing: 4) {
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
                    .padding(.bottom, 100)
                }
            }

            VStack(alignment: .trailing, spacing: 10) {
                Text("👨‍👩‍👧 家长助手", font: .system(size: 12), color: Color(white: 0.55))
                    .padding(.horizontal, 12).padding(.vertical, 8)
                    .background(Color(red: 0.14, green: 0.15, blue: 0.19), in: Capsule())
                    .padding(.trailing, 16)

                HStack(spacing: 10) {
                    Text("👑").font(.system(size: 28))
                    VStack(alignment: .leading, spacing: 4) {
                        Text("开通会员，解锁全部内容", font: .system(size: 14, weight: .semibold), color: .white)
                        Text("全量剧集 · AI外教不限时 · 专属勋章", font: .system(size: 11), color: Color(white: 0.55))
                            .lineLimit(1)
                    }
                    Spacer()
                    Text("立即开通", font: .system(size: 13, weight: .semibold), color: .white)
                        .padding(.horizontal, 14).padding(.vertical, 8)
                        .background(Color(red: 0.90, green: 0.32, blue: 0), in: Capsule())
                }
                .padding(14)
                .background(
                    LinearGradient(colors: [Color(red: 0.16, green: 0.09, blue: 0.06), Color(red: 0.10, green: 0.07, blue: 0.06)], startPoint: .leading, endPoint: .trailing),
                    in: RoundedRectangle(cornerRadius: 16)
                )
                .padding(.horizontal, 16)
                .padding(.bottom, 12)
            }
        }
        .background(Color(red: 0.04, green: 0.05, blue: 0.07).ignoresSafeArea())
    }
}


struct NativePurchaseCalculatorPage: View {
    var onClose: () -> Void
    @State private var mode = "cash" // cash | loan
    @State private var barePrice = "100000"
    @State private var taxable = ""
    @State private var includeCommercial = false
    @State private var selectedProduct = 1
    @State private var quote: [(String, String)]? = nil
    private let products: [(Int, String, String)] = [
        (1, "示例银行车贷", "年利率 4.5% · 最低首付 20.0%"),
        (2, "厂商金融贴息", "年利率 4.5% · 最低首付 20.0% · 贴息减 0.5%"),
        (3, "低息精品贷", "年利率 3.98% · 最低首付 15.0% · 减本金 ¥2000"),
    ]

    var body: some View {
        VStack(spacing: 0) {
            navBar(title: "购车计算器", onClose: onClose, dark: false)
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    sectionCard {
                        Text("付款方式", font: .system(size: 15, weight: .semibold), color: DesignTokens.ink)
                        HStack(spacing: 8) {
                            modeChip("全款", selected: mode == "cash") { mode = "cash" }
                            modeChip("贷款", selected: mode == "loan") { mode = "loan" }
                        }
                        field("裸车价（元）", text: $barePrice)
                        field("计税价格（可选，默认裸车价/1.13）", text: $taxable)
                        Toggle(isOn: $includeCommercial) {
                            Text("计入商业险粗算", font: .system(size: 15), color: DesignTokens.ink)
                        }
                        .tint(DesignTokens.link)
                    }

                    sectionCard {
                        HStack {
                            Text("金融产品", font: .system(size: 15, weight: .semibold), color: DesignTokens.ink)
                            Spacer()
                            Text("刷新", font: .system(size: 14), color: DesignTokens.link)
                        }
                        ForEach(products, id: \.0) { p in
                            HStack(alignment: .top, spacing: 10) {
                                Text(selectedProduct == p.0 ? "◉" : "○",
                                     font: .system(size: 18),
                                     color: selectedProduct == p.0 ? DesignTokens.link : DesignTokens.mute)
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(p.1, font: .system(size: 15, weight: .medium),
                                         color: selectedProduct == p.0 ? DesignTokens.link : DesignTokens.ink)
                                    Text(p.2, font: .system(size: 12), color: DesignTokens.body)
                                }
                                Spacer()
                            }
                            .contentShape(Rectangle())
                            .onTapGesture { selectedProduct = p.0 }
                        }
                    }

                    Button {
                        let bare = Double(barePrice) ?? 0
                        let tax = Double(taxable) ?? (bare / 1.13)
                        let product = products.first(where: { $0.0 == selectedProduct })?.1 ?? ""
                        quote = [
                            ("付款方式", mode == "cash" ? "全款" : "贷款"),
                            ("金融产品", product),
                            ("裸车价", "¥\(Int(bare))"),
                            ("计税价格", "¥\(Int(tax))"),
                            ("商业险", includeCommercial ? "已计入粗算" : "未计入"),
                            ("合计参考", "¥\(Int(bare * 1.08))"),
                        ]
                    } label: {
                        Text("计算报价", font: .system(size: 16, weight: .semibold), color: .white)
                            .frame(maxWidth: .infinity).frame(height: 48)
                            .background(DesignTokens.ink, in: RoundedRectangle(cornerRadius: 12))
                    }

                    if let quote {
                        sectionCard {
                            Text("报价结果", font: .system(size: 15, weight: .semibold), color: DesignTokens.ink)
                            ForEach(quote, id: \.0) { row in
                                HStack {
                                    Text(row.0, font: .system(size: 14), color: DesignTokens.body)
                                    Spacer()
                                    Text(row.1, font: .system(size: 14, weight: .medium), color: DesignTokens.ink)
                                }
                            }
                        }
                    }
                }
                .padding(16)
            }
            .background(Color(red: 0.96, green: 0.96, blue: 0.97).ignoresSafeArea())
        }
    }

    private func sectionCard<Content: View>(@ViewBuilder _ content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 12) { content() }
            .padding(14)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 12))
    }

    private func modeChip(_ label: String, selected: Bool, action: @escaping () -> Void) -> some View {
        Text(selected ? "✓ \(label)" : label, font: .system(size: 14, weight: .medium),
             color: selected ? .white : DesignTokens.ink)
            .padding(.horizontal, 14).padding(.vertical, 8)
            .background(selected ? DesignTokens.link : DesignTokens.canvas, in: Capsule())
            .overlay(Capsule().stroke(selected ? DesignTokens.link : DesignTokens.hairline, lineWidth: 1))
            .onTapGesture(perform: action)
    }

    private func field(_ label: String, text: Binding<String>) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label, font: .system(size: 12), color: DesignTokens.body)
            TextField("", text: text)
                .padding(.horizontal, 12).padding(.vertical, 12)
                .background(Color(red: 0.96, green: 0.96, blue: 0.97), in: RoundedRectangle(cornerRadius: 8))
                .keyboardType(.decimalPad)
        }
    }
}


struct NativeMallDetailPage: View {
    var onClose: () -> Void
    @State private var qty = 1
    @State private var skuSel = 0
    @State private var toastText: String? = nil
    private let skus = ["经典白", "店庆红"]

    var body: some View {
        ZStack(alignment: .bottom) {
            VStack(spacing: 0) {
                navBar(title: "商品详情", onClose: onClose, dark: false)
                ScrollView {
                    VStack(spacing: 8) {
                        Text("店庆纪念马克杯", font: .system(size: 14), color: DesignTokens.mute)
                            .frame(maxWidth: .infinity)
                            .frame(height: 280)
                            .background(Color(red: 0xF0/255, green: 0xF0/255, blue: 0xF0/255))

                        VStack(alignment: .leading, spacing: 8) {
                            Text("店庆纪念马克杯", font: .system(size: 18, weight: .bold), color: DesignTokens.ink)
                            Text("39.90元", font: .system(size: 22, weight: .semibold), color: Color(red: 0xEE/255, green: 0, blue: 0))
                            Text("实体商品 · 需填写收货信息", font: .system(size: 13), color: DesignTokens.body)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(16)
                        .background(DesignTokens.canvas)

                        HStack {
                            Text("📍")
                            Text("选择收货地址", font: .system(size: 14), color: DesignTokens.mute)
                            Spacer()
                            Text("›", font: .system(size: 18), color: DesignTokens.mute)
                        }
                        .padding(.horizontal, 16)
                        .padding(.vertical, 14)
                        .background(DesignTokens.canvas)
                        .onTapGesture { flash("选择收货地址") }

                        VStack(alignment: .leading, spacing: 12) {
                            Text("规格", font: .system(size: 14, weight: .semibold), color: DesignTokens.ink)
                            HStack(spacing: 8) {
                                ForEach(Array(skus.enumerated()), id: \.offset) { i, label in
                                    let sel = skuSel == i
                                    Text(label, font: .system(size: 13, weight: sel ? .semibold : .regular),
                                          color: sel ? DesignTokens.link : DesignTokens.body)
                                        .padding(.horizontal, 12)
                                        .padding(.vertical, 8)
                                        .background(sel ? DesignTokens.link.opacity(0.12) : DesignTokens.canvasSoft2, in: RoundedRectangle(cornerRadius: 8))
                                        .overlay(RoundedRectangle(cornerRadius: 8).stroke(sel ? DesignTokens.link : DesignTokens.hairline, lineWidth: 1))
                                        .onTapGesture { skuSel = i }
                                }
                            }
                            HStack {
                                Text("数量", font: .system(size: 14, weight: .semibold), color: DesignTokens.ink)
                                Spacer()
                                Text("库存 128", font: .system(size: 12), color: DesignTokens.mute)
                                Button { if qty > 1 { qty -= 1 } } label: {
                                    Text("−").padding(.horizontal, 10).padding(.vertical, 4)
                                        .overlay(RoundedRectangle(cornerRadius: 4).stroke(DesignTokens.hairline))
                                }
                                Text("\(qty)", font: .system(size: 15, weight: .medium))
                                    .padding(.horizontal, 12)
                                Button { qty += 1 } label: {
                                    Text("+").padding(.horizontal, 10).padding(.vertical, 4)
                                        .overlay(RoundedRectangle(cornerRadius: 4).stroke(DesignTokens.hairline))
                                }
                            }
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(16)
                        .background(DesignTokens.canvas)

                        Color.clear.frame(height: 80)
                    }
                }

                HStack(spacing: 12) {
                    Button { flash("已加入购物车") } label: {
                        Text("加入购物车", font: .system(size: 15), color: DesignTokens.ink)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 12)
                            .overlay(RoundedRectangle(cornerRadius: 8).stroke(DesignTokens.hairline))
                    }
                    Button { flash("立即购买") } label: {
                        Text("立即购买", font: .system(size: 15, weight: .semibold), color: .white)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 12)
                            .background(DesignTokens.link, in: RoundedRectangle(cornerRadius: 8))
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 10)
                .background(DesignTokens.canvas)
            }
            .background(DesignTokens.canvasSoft2.ignoresSafeArea())

            if let toastText {
                Text(toastText, font: .system(size: 14), color: .white)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 10)
                    .background(Color.black.opacity(0.78), in: Capsule())
                    .padding(.bottom, 100)
            }
        }
    }

    private func flash(_ text: String) {
        toastText = text
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.6) {
            if toastText == text { toastText = nil }
        }
    }
}

struct NativeMallOrdersPage: View {
    var onClose: () -> Void
    var onOpen: ((String) -> Void)? = nil
    @State private var tab = "全部"
    private let tabs = ["全部", "待支付", "已支付", "已取消"]
    private let all: [(String, String, Int, String, String)] = [
        ("MO-1001", "店庆纪念马克杯", 2, "79.80", "待支付"),
        ("MO-0998", "电子礼品卡 50 元", 1, "50.00", "已支付"),
        ("MO-0992", "品牌帆布袋", 1, "29.00", "已取消"),
    ]

    private var filtered: [(String, String, Int, String, String)] {
        switch tab {
        case "待支付": return all.filter { $0.4 == "待支付" }
        case "已支付": return all.filter { $0.4 == "已支付" || $0.4 == "待发货" }
        case "已取消": return all.filter { $0.4 == "已取消" }
        default: return all
        }
    }

    var body: some View {
        VStack(spacing: 0) {
            navBar(title: "我的订单", onClose: onClose, dark: false)
            HStack(spacing: 0) {
                ForEach(tabs, id: \.self) { t in
                    let sel = tab == t
                    VStack(spacing: 6) {
                        Text(t, font: .system(size: 14, weight: sel ? .semibold : .regular),
                              color: sel ? DesignTokens.link : DesignTokens.body)
                        Capsule()
                            .fill(sel ? DesignTokens.link : Color.clear)
                            .frame(width: 28, height: 2)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .contentShape(Rectangle())
                    .onTapGesture { tab = t }
                }
            }
            .background(DesignTokens.canvas)

            if filtered.isEmpty {
                Spacer()
                Text("暂无订单", font: .system(size: 15), color: DesignTokens.body)
                Spacer()
            } else {
                ScrollView {
                    VStack(spacing: 10) {
                        ForEach(Array(filtered.enumerated()), id: \.offset) { _, o in
                            HStack(spacing: 12) {
                                RoundedRectangle(cornerRadius: 8)
                                    .fill(DesignTokens.canvasSoft2)
                                    .frame(width: 64, height: 64)
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(o.1, font: .system(size: 15, weight: .medium), color: DesignTokens.ink)
                                    Text("×\(o.2) · ¥\(o.3)", font: .system(size: 13), color: DesignTokens.body)
                                    Text(o.0, font: .system(size: 12), color: DesignTokens.mute)
                                }
                                Spacer()
                                Text(o.4, font: .system(size: 13, weight: .medium),
                                      color: o.4 == "待支付" ? DesignTokens.link : DesignTokens.body)
                            }
                            .padding(12)
                            .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 10))
                            .onTapGesture { onOpen?("/mall/detail") }
                        }
                    }
                    .padding(16)
                }
            }
        }
        .background(DesignTokens.canvasSoft2.ignoresSafeArea())
    }
}

struct NativeFriendPage: View {
    var onClose: () -> Void
    var onOpen: ((String) -> Void)? = nil
    @State private var query = ""
    @State private var searchHits: [(String, String)] = []
    @State private var incoming: [(String, String)] = [
        ("i1", "王同学"),
        ("i2", "李老师"),
    ]
    @State private var friends: [(String, String, String)] = [
        ("1", "小明", "刚刚在线"),
        ("2", "阿哲", "三天前"),
        ("3", "林林", "一周前"),
        ("4", "客服小助手", "昨天"),
    ]
    @State private var toastText: String? = nil
    private let directory: [(String, String)] = [
        ("新同学小周", "同校"),
        ("外教 Anna", "口语"),
    ]

    var body: some View {
        ZStack(alignment: .bottom) {
            VStack(spacing: 0) {
                HStack {
                    Button { onClose() } label: {
                        Text("‹", font: .system(size: 28), color: DesignTokens.link)
                            .frame(width: 44, alignment: .leading)
                    }
                    Text("通讯录", font: .system(size: 17, weight: .semibold), color: DesignTokens.ink)
                        .frame(maxWidth: .infinity)
                    Button {
                        flash("建群成功（mock）· 请到聊天 Tab")
                    } label: {
                        Text("建群", font: .system(size: 15, weight: .medium), color: DesignTokens.link)
                    }
                    .frame(width: 44, alignment: .trailing)
                }
                .padding(.horizontal, 8)
                .frame(height: 44)
                .background(DesignTokens.canvas)

                ScrollView {
                    VStack(alignment: .leading, spacing: 16) {
                        HStack {
                            TextField("搜索好友", text: $query)
                                .font(.system(size: 15))
                            Button {
                                let q = query.trimmingCharacters(in: .whitespaces)
                                if q.isEmpty {
                                    searchHits = []
                                } else {
                                    searchHits = directory.filter { $0.0.contains(q) }
                                    if searchHits.isEmpty { flash("未找到用户") }
                                }
                            } label: {
                                Text("搜索", font: .system(size: 14, weight: .medium), color: DesignTokens.link)
                            }
                        }
                        .padding(.horizontal, 14)
                        .padding(.vertical, 12)
                        .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 12))

                        if !searchHits.isEmpty {
                            sectionLabel("搜索结果")
                            groupedCard {
                                ForEach(Array(searchHits.enumerated()), id: \.offset) { i, hit in
                                    friendRow(name: hit.0, subtitle: hit.1) {
                                        Button { flash("已发送好友申请") } label: {
                                            Text("加好友", font: .system(size: 13, weight: .semibold), color: .white)
                                                .padding(.horizontal, 12)
                                                .padding(.vertical, 6)
                                                .background(DesignTokens.link, in: Capsule())
                                        }
                                    }
                                    if i < searchHits.count - 1 {
                                        Divider().padding(.leading, 70)
                                    }
                                }
                            }
                        }

                        sectionLabel("新的朋友（\(incoming.count)）")
                        groupedCard {
                            ForEach(Array(incoming.enumerated()), id: \.element.0) { i, req in
                                friendRow(name: req.1, subtitle: "请求添加你为好友") {
                                    HStack(spacing: 8) {
                                        Button {
                                            friends.append((req.0, req.1, "刚刚"))
                                            incoming.removeAll { $0.0 == req.0 }
                                            flash("已添加")
                                        } label: {
                                            Text("接受", font: .system(size: 13, weight: .semibold), color: .white)
                                                .padding(.horizontal, 12)
                                                .padding(.vertical, 6)
                                                .background(DesignTokens.link, in: Capsule())
                                        }
                                        Button {
                                            incoming.removeAll { $0.0 == req.0 }
                                        } label: {
                                            Text("拒绝", font: .system(size: 13), color: DesignTokens.mute)
                                        }
                                    }
                                }
                                if i < incoming.count - 1 {
                                    Divider().padding(.leading, 70)
                                }
                            }
                        }

                        sectionLabel("好友（\(friends.count)）")
                        groupedCard {
                            if friends.isEmpty {
                                Text("暂无好友", font: .system(size: 14), color: DesignTokens.mute)
                                    .padding(16)
                            } else {
                                ForEach(Array(friends.enumerated()), id: \.element.0) { i, row in
                                    friendRow(name: row.1, subtitle: row.2) {
                                        EmptyView()
                                    }
                                    .contentShape(Rectangle())
                                    .onTapGesture {
                                        onOpen?("/chat")
                                    }
                                    if i < friends.count - 1 {
                                        Divider().padding(.leading, 70)
                                    }
                                }
                            }
                        }
                    }
                    .padding(16)
                    .padding(.bottom, 28)
                }
            }
            .background(DesignTokens.canvasSoft2.ignoresSafeArea())

            if let toastText {
                Text(toastText, font: .system(size: 14), color: .white)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 10)
                    .background(Color.black.opacity(0.78), in: Capsule())
                    .padding(.bottom, 40)
            }
        }
    }

    private func sectionLabel(_ text: String) -> some View {
        Text(text, font: .system(size: 14, weight: .semibold), color: DesignTokens.ink)
    }

    private func groupedCard<Content: View>(@ViewBuilder content: () -> Content) -> some View {
        VStack(spacing: 0) {
            content()
        }
        .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 12))
    }

    private func friendRow<Trailing: View>(
        name: String,
        subtitle: String,
        @ViewBuilder trailing: () -> Trailing
    ) -> some View {
        HStack(spacing: 12) {
            Text(String(name.suffix(1)), font: .system(size: 16, weight: .semibold), color: DesignTokens.link)
                .frame(width: 44, height: 44)
                .background(DesignTokens.link.opacity(0.15), in: Circle())
            VStack(alignment: .leading, spacing: 2) {
                Text(name, font: .system(size: 16, weight: .semibold), color: DesignTokens.ink)
                Text(subtitle, font: .system(size: 13), color: DesignTokens.body)
            }
            Spacer(minLength: 0)
            trailing()
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
    }

    private func flash(_ text: String) {
        toastText = text
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.6) {
            if toastText == text { toastText = nil }
        }
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

struct NativeCommunityPublishPage: View {
    var onClose: () -> Void
    @State private var content = ""
    @State private var mediaType = "none"
    @State private var topic = ""
    @State private var showConvention = false
    @State private var publishing = false

    private static let conventionAckKey = "community_convention_ack_date"

    var body: some View {
        ZStack {
            VStack(spacing: 0) {
                HStack {
                    Button {
                        onClose()
                    } label: {
                        Image(systemName: "chevron.left").foregroundStyle(DesignTokens.link)
                    }
                    Spacer()
                    Button {
                        guard !content.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty, !publishing else { return }
                        publishing = true
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.4) {
                            publishing = false
                            onClose()
                        }
                    } label: {
                        Text("发布", font: .system(size: 14, weight: .semibold), color: .white)
                            .padding(.horizontal, 16)
                            .padding(.vertical, 8)
                            .background(
                                content.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                                    ? DesignTokens.link.opacity(0.4)
                                    : Color(red: 0x16/255, green: 0x77/255, blue: 1),
                                in: Capsule()
                            )
                    }
                }
                .padding(16)
                .background(DesignTokens.canvas)

                TextEditor(text: $content)
                    .frame(minHeight: 160)
                    .padding(.horizontal, 16)
                    .overlay(alignment: .topLeading) {
                        if content.isEmpty {
                            Text("记录一下吧", font: .system(size: 16), color: DesignTokens.mute)
                                .padding(.horizontal, 20)
                                .padding(.top, 8)
                        }
                    }

                HStack(spacing: 8) {
                    ForEach([("无媒体", "none"), ("图片", "image"), ("视频", "video")], id: \.0) { item in
                        Text(item.0, font: .system(size: 13), color: mediaType == item.1 ? DesignTokens.link : DesignTokens.body)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .background(
                                mediaType == item.1 ? DesignTokens.link.opacity(0.1) : DesignTokens.canvasSoft2,
                                in: Capsule()
                            )
                            .onTapGesture { mediaType = item.1 }
                    }
                    Spacer()
                }
                .padding(.horizontal, 16)
                .padding(.top, 8)

                if mediaType == "image" {
                    Text("+ 添加图片", font: .system(size: 14), color: DesignTokens.link)
                        .frame(width: 96, height: 96)
                        .background(DesignTokens.canvasSoft2, in: RoundedRectangle(cornerRadius: 8))
                        .padding(.leading, 16)
                        .padding(.top, 12)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
                if mediaType == "video" {
                    Text("+ 添加视频", font: .system(size: 14), color: DesignTokens.link)
                        .frame(width: 160, height: 96)
                        .background(DesignTokens.canvasSoft2, in: RoundedRectangle(cornerRadius: 8))
                        .padding(.leading, 16)
                        .padding(.top, 12)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }

                Button {
                    topic = topic.isEmpty ? "换车季" : ""
                } label: {
                    Text(topic.isEmpty ? "+ 添加话题" : "#\(topic)", font: .system(size: 14), color: DesignTokens.link)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 8)
                        .background(DesignTokens.link.opacity(0.1), in: Capsule())
                }
                .padding(.leading, 16)
                .padding(.top, 16)
                .frame(maxWidth: .infinity, alignment: .leading)

                Spacer()
            }
            .background(DesignTokens.canvas.ignoresSafeArea())

            if showConvention {
                Color.black.opacity(0.5).ignoresSafeArea()
                VStack(spacing: 12) {
                    Text("社区公约", font: .system(size: 18, weight: .semibold), color: DesignTokens.ink)
                    Text("请文明发言，禁止发布违法违规、广告引流等内容。", font: .system(size: 14), color: DesignTokens.body)
                        .multilineTextAlignment(.center)
                    Button("我知道了") {
                        let today = String(ISO8601DateFormatter().string(from: Date()).prefix(10))
                        UserDefaults.standard.set(today, forKey: Self.conventionAckKey)
                        showConvention = false
                    }
                        .buttonStyle(.borderedProminent)
                        .tint(DesignTokens.link)
                }
                .padding(24)
                .frame(maxWidth: 320)
                .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 12))
            }
        }
        .onAppear {
            // Flutter CommunityConventionDialog.maybeShow — once per local day.
            let today = String(ISO8601DateFormatter().string(from: Date()).prefix(10))
            let ack = UserDefaults.standard.string(forKey: Self.conventionAckKey)
            showConvention = ack != today
        }
    }
}

struct NativeCommunityCommentPage: View {
    var onClose: () -> Void
    @State private var draft = ""
    @State private var comments: [(String, String)] = [
        ("李四", "说得对！周末一起去门店看看"),
        ("赵六", "同感 +1，双擎确实省油"),
        ("客服小助手", "欢迎到店试驾，预约通道已开放"),
    ]

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Capsule().fill(DesignTokens.hairline).frame(width: 36, height: 4)
            }
            .frame(maxWidth: .infinity)
            .padding(.top, 8)
            HStack {
                Text("评论 \(comments.count)", font: .system(size: 16, weight: .semibold), color: DesignTokens.ink)
                Spacer()
                Button("关闭", action: onClose).foregroundStyle(DesignTokens.link)
            }
            .padding(16)

            ScrollView {
                LazyVStack(alignment: .leading, spacing: 0) {
                    ForEach(Array(comments.enumerated()), id: \.offset) { _, item in
                        HStack(alignment: .top, spacing: 12) {
                            Circle()
                                .fill(DesignTokens.link.opacity(0.15))
                                .frame(width: 36, height: 36)
                                .overlay(Text(String(item.0.prefix(1)), font: .system(size: 14, weight: .medium), color: DesignTokens.link))
                            VStack(alignment: .leading, spacing: 4) {
                                Text(item.0, font: .system(size: 14, weight: .semibold), color: DesignTokens.ink)
                                Text(item.1, font: .system(size: 14), color: DesignTokens.body)
                            }
                            Spacer()
                        }
                        .padding(.horizontal, 16)
                        .padding(.vertical, 12)
                        Divider().overlay(DesignTokens.hairline)
                    }
                }
            }

            HStack(spacing: 10) {
                TextField("说说你的看法…", text: $draft)
                    .padding(.horizontal, 12)
                    .frame(height: 40)
                    .background(DesignTokens.canvasSoft2, in: RoundedRectangle(cornerRadius: 20))
                Button("发送") {
                    let text = draft.trimmingCharacters(in: .whitespacesAndNewlines)
                    guard !text.isEmpty else { return }
                    comments.insert(("我", text), at: 0)
                    draft = ""
                }
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(.white)
                .padding(.horizontal, 14)
                .padding(.vertical, 10)
                .background(DesignTokens.link, in: Capsule())
            }
            .padding(12)
            .background(DesignTokens.canvas)
        }
        .background(DesignTokens.canvas.ignoresSafeArea())
    }
}

struct NativeCommunityImagePreviewPage: View {
    var onClose: () -> Void
    @State private var index = 0
    private let labels = ["社区配图 1", "社区配图 2", "社区配图 3"]

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            VStack {
                Spacer()
                RoundedRectangle(cornerRadius: 8)
                    .fill(Color.white.opacity(0.12))
                    .overlay(
                        Text(labels[index], font: .system(size: 18, weight: .medium), color: .white)
                    )
                    .frame(maxWidth: .infinity)
                    .frame(height: 360)
                    .padding(.horizontal, 24)
                Spacer()
            }
            VStack {
                HStack {
                    Button("关闭", action: onClose).foregroundStyle(.white)
                    Spacer()
                    Text("\(index + 1)/\(labels.count)", font: .system(size: 15), color: .white)
                }
                .padding(16)
                Spacer()
                HStack {
                    Button("上一张") { if index > 0 { index -= 1 } }
                        .foregroundStyle(.white)
                    Spacer()
                    Button("下一张") { if index < labels.count - 1 { index += 1 } }
                        .foregroundStyle(.white)
                }
                .padding(24)
            }
        }
    }
}

struct NativeMallPage: View {
    var onClose: () -> Void
    var onOpen: ((String) -> Void)? = nil
    @State private var category = 0
    @State private var filter = 1
    private let categories = ["推荐", "0元起兑", "国庆季", "钻铂专享", "数码家电", "生活好物"]
    private let filters = ["积分", "热兑", "上新", "筛选"]
    private let products: [(String, String, String, Bool)] = [
        ("店庆纪念马克杯", "39.90元", "已兑2391", false),
        ("电子礼品卡 50 元", "50.00元", "已兑2877", true),
        ("会员壁纸包", "6.00元", "已兑6566", true),
        ("线上精品课兑换", "99.00元", "已兑9492", true),
        ("品牌帆布袋", "29.00元", "已兑5613", false),
        ("冬季保暖围巾", "128.00元", "已兑8555", false),
    ]

    var body: some View {
        ZStack(alignment: .bottom) {
            VStack(spacing: 0) {
                HStack(spacing: 8) {
                    Button { onClose() } label: {
                        Image(systemName: "chevron.left").foregroundStyle(DesignTokens.ink)
                    }
                    HStack(spacing: 6) {
                        Image(systemName: "magnifyingglass").font(.system(size: 13)).foregroundStyle(DesignTokens.mute)
                        Text("视频会员卡", font: .system(size: 14), color: DesignTokens.mute)
                        Spacer()
                        Text("搜索", font: .system(size: 12, weight: .semibold), color: .white)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .background(DesignTokens.link, in: RoundedRectangle(cornerRadius: 6))
                    }
                    .padding(.leading, 10)
                    .padding(.trailing, 4)
                    .frame(height: 36)
                    .background(DesignTokens.canvasSoft2, in: RoundedRectangle(cornerRadius: 8))
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(DesignTokens.canvas)

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 18) {
                        ForEach(Array(categories.enumerated()), id: \.offset) { i, label in
                            VStack(spacing: 4) {
                                Text(label, font: .system(size: i == category ? 15 : 14, weight: i == category ? .semibold : .regular),
                                      color: i == category ? DesignTokens.ink : DesignTokens.mute)
                                Capsule()
                                    .fill(i == category ? DesignTokens.link : Color.clear)
                                    .frame(width: 16, height: 2)
                            }
                            .onTapGesture { category = i }
                        }
                    }
                    .padding(.horizontal, 12)
                    .padding(.vertical, 8)
                }
                .background(DesignTokens.canvas)

                HStack(spacing: 6) {
                    ForEach(Array(filters.enumerated()), id: \.offset) { i, label in
                        let active = i == filter
                        HStack(spacing: 2) {
                            if active { Text("✓", font: .system(size: 11), color: DesignTokens.link) }
                            Text(label, font: .system(size: 12, weight: active ? .semibold : .regular),
                                  color: active ? DesignTokens.link : DesignTokens.body)
                            if label == "积分" || label == "筛选" {
                                Text("▾", font: .system(size: 11), color: active ? DesignTokens.link : DesignTokens.mute)
                            }
                        }
                        .frame(maxWidth: .infinity)
                        .frame(height: 30)
                        .background(active ? DesignTokens.link.opacity(0.1) : DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 8))
                        .overlay(RoundedRectangle(cornerRadius: 8).stroke(active ? DesignTokens.link : DesignTokens.hairline, lineWidth: 1))
                        .onTapGesture { filter = i }
                    }
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 8)

                ScrollView {
                    LazyVGrid(columns: [GridItem(.flexible(), spacing: 8), GridItem(.flexible(), spacing: 8)], spacing: 8) {
                        ForEach(Array(products.enumerated()), id: \.offset) { _, p in
                            VStack(alignment: .leading, spacing: 8) {
                                RoundedRectangle(cornerRadius: 8)
                                    .fill(DesignTokens.canvasSoft2)
                                    .frame(height: 110)
                                    .overlay(
                                        Text(p.3 ? "虚拟" : "实物", font: .system(size: 12), color: DesignTokens.mute)
                                    )
                                Text(p.0, font: .system(size: 14, weight: .medium), color: DesignTokens.ink)
                                    .lineLimit(2)
                                Text(p.1, font: .system(size: 15, weight: .semibold), color: Color(red: 0.9, green: 0.3, blue: 0.2))
                                Text(p.2, font: .system(size: 11), color: DesignTokens.mute)
                            }
                            .padding(8)
                            .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 10))
                            .onTapGesture { onOpen?("/mall/detail") }
                        }
                    }
                    .padding(.horizontal, 10)
                    .padding(.bottom, 88)
                }
            }
            .background(DesignTokens.canvasSoft2.ignoresSafeArea())

            HStack {
                Button {
                    onOpen?("/mall/orders")
                } label: {
                    Text("我的订单", font: .system(size: 14, weight: .semibold), color: .white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .background(DesignTokens.link, in: Capsule())
                }
            }
            .padding(.horizontal, 24)
            .padding(.bottom, 16)
        }
    }
}

struct NativeWalletPage: View {
    var onClose: () -> Void
    @State private var amount = ""
    @State private var channel = 1
    @State private var bankName = ""
    @State private var cardLast4 = ""
    @State private var toastText: String? = nil
    private let flows = [
        ("membership_pay", "ref m2", "-30.00"),
        ("充值", "ref alipay", "+100.00"),
        ("充值", "ref alipay", "+1.00"),
    ]

    var body: some View {
        ZStack(alignment: .bottom) {
            VStack(spacing: 0) {
                navBar(title: "我的钱包", onClose: onClose, dark: false)
                ScrollView {
                    VStack(alignment: .leading, spacing: 12) {
                        VStack(alignment: .leading, spacing: 8) {
                            Text("余额（元）", font: .system(size: 14), color: DesignTokens.body)
                            Text("71.00", font: .system(size: 32, weight: .semibold), color: DesignTokens.ink)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(20)
                        .background(Color.white, in: RoundedRectangle(cornerRadius: 12))

                        Text("充值", font: .system(size: 16, weight: .bold), color: DesignTokens.ink)
                        TextField("金额 0.01-50000", text: $amount)
                            .keyboardType(.decimalPad)
                            .padding(12)
                            .background(Color.white, in: RoundedRectangle(cornerRadius: 8))
                            .overlay(RoundedRectangle(cornerRadius: 8).stroke(DesignTokens.hairline, lineWidth: 1))

                        HStack(spacing: 8) {
                            ForEach([(1, "支付宝"), (2, "微信"), (3, "银行卡")], id: \.0) { item in
                                let sel = channel == item.0
                                Text((sel ? "✓ " : "") + item.1, font: .system(size: 13), color: sel ? .white : DesignTokens.ink)
                                    .padding(.horizontal, 12).padding(.vertical, 8)
                                    .background(sel ? DesignTokens.link : Color.white, in: RoundedRectangle(cornerRadius: 8))
                                    .overlay(RoundedRectangle(cornerRadius: 8).stroke(sel ? DesignTokens.link : DesignTokens.hairline, lineWidth: 1))
                                    .onTapGesture { channel = item.0 }
                            }
                        }
                        HStack(spacing: 8) {
                            ForEach(["10", "50", "100"], id: \.self) { a in
                                Text(a, font: .system(size: 13), color: DesignTokens.ink)
                                    .padding(.horizontal, 16).padding(.vertical, 8)
                                    .background(Color.white, in: RoundedRectangle(cornerRadius: 8))
                                    .overlay(RoundedRectangle(cornerRadius: 8).stroke(DesignTokens.hairline, lineWidth: 1))
                                    .onTapGesture { amount = a }
                            }
                        }

                        Button("确认充值") {
                            if amount.trimmingCharacters(in: .whitespaces).isEmpty {
                                flash("请输入金额")
                            } else {
                                flash("确认充值 ¥\(amount)（mock）")
                            }
                        }
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundStyle(.white)
                        .frame(maxWidth: .infinity).frame(height: 48)
                        .background(Color.black, in: RoundedRectangle(cornerRadius: 10))

                        Text("银行卡", font: .system(size: 16, weight: .bold), color: DesignTokens.ink)
                        TextField("银行名称", text: $bankName)
                            .padding(12)
                            .background(Color.white, in: RoundedRectangle(cornerRadius: 8))
                            .overlay(RoundedRectangle(cornerRadius: 8).stroke(DesignTokens.hairline, lineWidth: 1))
                        TextField("卡号后四位", text: $cardLast4)
                            .keyboardType(.numberPad)
                            .onChange(of: cardLast4) { _, v in
                                cardLast4 = String(v.filter(\.isNumber).prefix(4))
                            }
                            .padding(12)
                            .background(Color.white, in: RoundedRectangle(cornerRadius: 8))
                            .overlay(RoundedRectangle(cornerRadius: 8).stroke(DesignTokens.hairline, lineWidth: 1))
                        Button("绑定银行卡") {
                            if bankName.isEmpty || cardLast4.count < 4 {
                                flash("请填写银行与卡号后四位")
                            } else {
                                flash("已绑定 \(bankName) ****\(cardLast4)（mock）")
                            }
                        }
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundStyle(.white)
                        .frame(maxWidth: .infinity).frame(height: 44)
                        .background(DesignTokens.link, in: RoundedRectangle(cornerRadius: 10))

                        Text("流水", font: .system(size: 16, weight: .bold), color: DesignTokens.ink)
                        ForEach(Array(flows.enumerated()), id: \.offset) { _, f in
                            HStack {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(f.0, font: .system(size: 15, weight: .medium), color: DesignTokens.ink)
                                    Text(f.1, font: .system(size: 12), color: DesignTokens.mute)
                                }
                                Spacer()
                                Text(f.2, font: .system(size: 15, weight: .semibold),
                                      color: f.2.hasPrefix("+") ? Color(red: 0.18, green: 0.49, blue: 0.20) : Color(red: 0.90, green: 0.22, blue: 0.21))
                            }
                            .padding(14)
                            .background(Color.white, in: RoundedRectangle(cornerRadius: 10))
                        }
                    }
                    .padding(16)
                }
                .background(Color(white: 0.96).ignoresSafeArea())
            }
            if let toastText {
                Text(toastText, font: .system(size: 14), color: .white)
                    .padding(.horizontal, 16).padding(.vertical, 10)
                    .background(Color.black.opacity(0.78), in: Capsule())
                    .padding(.bottom, 40)
            }
        }
    }

    private func flash(_ text: String) {
        toastText = text
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.6) {
            if toastText == text { toastText = nil }
        }
    }
}

struct NativeShortVideoPlayPage: View {
    var onClose: () -> Void
    @State private var playing = true
    @State private var liked = false
    @State private var toastText: String? = nil

    var body: some View {
        ZStack(alignment: .bottom) {
            VStack(spacing: 0) {
                HStack {
                    Button { onClose() } label: {
                        Text("‹", font: .system(size: 28), color: .white).frame(width: 44, alignment: .leading)
                    }
                    Text("播放", font: .system(size: 17, weight: .semibold), color: .white)
                        .frame(maxWidth: .infinity)
                    Color.clear.frame(width: 44, height: 1)
                }
                .padding(.horizontal, 8)
                .frame(height: 44)

                Spacer()
                Text(playing ? "播放中 · 单击暂停" : "已暂停 · 单击继续",
                      font: .system(size: 16), color: .white)
                    .onTapGesture { playing.toggle() }
                Spacer()

                HStack {
                    Button { liked.toggle() } label: {
                        Text(liked ? "已赞" : "赞", font: .system(size: 15), color: .white)
                    }
                    .frame(maxWidth: .infinity)
                    Button { flash("评论（mock）") } label: {
                        Text("评", font: .system(size: 15), color: .white)
                    }
                    .frame(maxWidth: .infinity)
                    Button { flash("分享（mock）") } label: {
                        Text("分享", font: .system(size: 15), color: .white)
                    }
                    .frame(maxWidth: .infinity)
                }
                .padding(16)

                Text("口语跟读 · 第一课  #口语", font: .system(size: 14), color: .white)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(16)
            }
            .background(Color.black.ignoresSafeArea())

            if let toastText {
                Text(toastText, font: .system(size: 14), color: .white)
                    .padding(.horizontal, 16).padding(.vertical, 10)
                    .background(Color.black.opacity(0.78), in: Capsule())
                    .padding(.bottom, 40)
            }
        }
    }

    private func flash(_ text: String) {
        toastText = text
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.6) {
            if toastText == text { toastText = nil }
        }
    }
}

struct NativeShortVideoPublishPage: View {
    var onClose: () -> Void
    @State private var title = ""
    @State private var topic = "#口语"
    @State private var toastText: String? = nil

    var body: some View {
        ZStack(alignment: .bottom) {
            VStack(spacing: 0) {
                navBar(title: "发布短视频", onClose: onClose, dark: false)
                ScrollView {
                    VStack(alignment: .leading, spacing: 12) {
                        Text("本地预览 · 选视频/拍摄见 platform-gap", font: .system(size: 13), color: DesignTokens.body)
                            .frame(maxWidth: .infinity)
                            .frame(height: 180)
                            .background(DesignTokens.canvasSoft2, in: RoundedRectangle(cornerRadius: 12))

                        Text("标题", font: .system(size: 15, weight: .medium), color: DesignTokens.ink)
                        TextField("输入标题（≤40）", text: $title)
                            .padding(12)
                            .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 8))
                            .onChange(of: title) { _, v in
                                if v.count > 40 { title = String(v.prefix(40)) }
                            }

                        Text("话题", font: .system(size: 15, weight: .medium), color: DesignTokens.ink)
                        TextField("话题", text: $topic)
                            .padding(12)
                            .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 8))

                        Button {
                            if title.trimmingCharacters(in: .whitespaces).isEmpty {
                                flash("请填写标题")
                            } else {
                                flash("已提交（mock）")
                                DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) { onClose() }
                            }
                        } label: {
                            Text("提交", font: .system(size: 16, weight: .semibold), color: .white)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 14)
                                .background(DesignTokens.link, in: RoundedRectangle(cornerRadius: 10))
                        }
                        .padding(.top, 8)
                    }
                    .padding(16)
                }
            }
            .background(DesignTokens.canvasSoft2.ignoresSafeArea())

            if let toastText {
                Text(toastText, font: .system(size: 14), color: .white)
                    .padding(.horizontal, 16).padding(.vertical, 10)
                    .background(Color.black.opacity(0.78), in: Capsule())
                    .padding(.bottom, 40)
            }
        }
    }

    private func flash(_ text: String) {
        toastText = text
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.6) {
            if toastText == text { toastText = nil }
        }
    }
}

struct NativeShortVideoHelpPage: View {
    var onClose: () -> Void
    private let steps = ["选择或拍摄视频", "填写标题与话题", "提交后等待审核", "在「我的作品」查看"]

    var body: some View {
        VStack(spacing: 0) {
            navBar(title: "短视频帮助", onClose: onClose, dark: false)
            ScrollView {
                VStack(spacing: 10) {
                    ForEach(Array(steps.enumerated()), id: \.offset) { i, step in
                        VStack(alignment: .leading, spacing: 4) {
                            Text("步骤 \(i + 1)", font: .system(size: 15, weight: .semibold), color: DesignTokens.link)
                            Text(step, font: .system(size: 14), color: DesignTokens.ink)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(14)
                        .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 10))
                    }
                }
                .padding(16)
            }
            .background(DesignTokens.canvasSoft2.ignoresSafeArea())
        }
    }
}

struct NativeShortVideoPage: View {
    var onClose: () -> Void
    var onOpen: ((String) -> Void)? = nil
    private let clips = [
        ("新车到店 · 15s", "播放 2.1k · 赞 186"),
        ("保养小贴士", "播放 980 · 赞 64"),
        ("交车仪式", "播放 3.4k · 赞 412"),
        ("展厅速览", "播放 1.1k · 赞 88"),
    ]

    var body: some View {
        VStack(spacing: 0) {
            navBar(title: "小视频", onClose: onClose, dark: false)
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    HStack(spacing: 12) {
                        Circle()
                            .fill(DesignTokens.link.opacity(0.2))
                            .frame(width: 56, height: 56)
                        VStack(alignment: .leading, spacing: 4) {
                            Text("沃德龙鼎", font: .system(size: 16, weight: .semibold), color: DesignTokens.ink)
                            Text("粉丝 12.6万 · 作品 48", font: .system(size: 13), color: DesignTokens.body)
                        }
                        Spacer()
                    }
                    .padding(16)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(
                        LinearGradient(colors: [Color(red: 0.86, green: 0.93, blue: 0.98), DesignTokens.canvasSoft2],
                                       startPoint: .top, endPoint: .bottom)
                    )

                    HStack {
                        Text("我发布的小视频", font: .system(size: 16, weight: .bold), color: DesignTokens.ink)
                        Spacer()
                        Button("如何拍摄小视频") { onOpen?("/video/short/help") }
                            .font(.system(size: 13))
                            .foregroundStyle(DesignTokens.link)
                    }
                    .padding(.horizontal, 16)

                    LazyVGrid(columns: [GridItem(.flexible(), spacing: 8), GridItem(.flexible(), spacing: 8)], spacing: 8) {
                        VStack {
                            Image(systemName: "plus")
                                .font(.system(size: 28))
                                .foregroundStyle(DesignTokens.link)
                            Text("拍一个", font: .system(size: 13), color: DesignTokens.link)
                        }
                        .frame(maxWidth: .infinity)
                        .frame(height: 160)
                        .overlay(
                            RoundedRectangle(cornerRadius: 10)
                                .stroke(style: StrokeStyle(lineWidth: 1, dash: [6]))
                                .foregroundStyle(DesignTokens.link)
                        )
                        .onTapGesture { onOpen?("/video/short/publish") }

                        ForEach(Array(clips.enumerated()), id: \.offset) { _, c in
                            VStack(alignment: .leading, spacing: 6) {
                                RoundedRectangle(cornerRadius: 10)
                                    .fill(Color(red: 0.1, green: 0.12, blue: 0.16))
                                    .frame(height: 120)
                                    .overlay(
                                        Image(systemName: "play.circle.fill")
                                            .font(.system(size: 32))
                                            .foregroundStyle(.white.opacity(0.9))
                                    )
                                Text(c.0, font: .system(size: 13, weight: .medium), color: DesignTokens.ink)
                                    .lineLimit(1)
                                Text(c.1, font: .system(size: 11), color: DesignTokens.mute)
                            }
                            .onTapGesture { onOpen?("/video/short/play") }
                        }
                    }
                    .padding(.horizontal, 16)

                    Text("没有更多了", font: .system(size: 12), color: DesignTokens.mute)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 16)
                }
            }
            .background(DesignTokens.canvasSoft2.ignoresSafeArea())
        }
    }
}

struct NativeLivePage: View {
    var onClose: () -> Void
    @State private var roomId: String? = nil
    @State private var joined = false
    @State private var signals: [String] = ["state: idle"]
    /// Align Compose LiveMockData
    private let rooms: [(String, String, String)] = [
        ("mock_room_001", "晚间答疑直播", "主播 · 小智"),
        ("mock_room_002", "口语陪练公开课", "主播 · 阿语"),
        ("mock_room_003", "周末分享会", "主播 · Demo"),
    ]

    var body: some View {
        if let id = roomId, let room = rooms.first(where: { $0.0 == id }) {
            VStack(spacing: 0) {
                navBar(title: "直播 \(room.0)", onClose: { roomId = nil; joined = false; signals = ["state: idle"] }, dark: false)
                RoundedRectangle(cornerRadius: 12)
                    .fill(DesignTokens.canvasSoft2)
                    .frame(height: 180)
                    .overlay(
                        Text(joined ? "WS: connected · paused 保持连接" : "WS: disconnected",
                             font: .system(size: 15), color: DesignTokens.ink)
                    )
                    .padding(.horizontal, 16)
                    .padding(.top, 8)
                VStack(alignment: .leading, spacing: 12) {
                    Text(room.2, font: .system(size: 14), color: DesignTokens.body)
                    Text("信令（上限 30）", font: .system(size: 15, weight: .semibold), color: DesignTokens.ink)
                    ForEach(signals.suffix(30), id: \.self) { line in
                        Text(line, font: .system(size: 12), color: DesignTokens.mute)
                    }
                    Button("发送 Mock 信令 live.join") {
                        joined = true
                        signals = Array((signals + ["signal: live.join payload={room=\(room.0)}"]).suffix(30))
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(DesignTokens.link)
                    .frame(maxWidth: .infinity)
                    Button("退订 liveSignal") {
                        joined = false
                        signals = signals + ["state: left"]
                    }
                    .buttonStyle(.bordered)
                    Text("Realtime SDK 未接入；本页 mock 信令列表。", font: .system(size: 12), color: DesignTokens.mute)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(16)
                Spacer()
            }
            .background(DesignTokens.canvasSoft2.ignoresSafeArea())
        } else {
            VStack(spacing: 0) {
                navBar(title: "直播", onClose: onClose, dark: false)
                Text("点选进入房间入口 · 推流/实时通道见 gap registry", font: .system(size: 13), color: DesignTokens.body)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.horizontal, 16)
                    .padding(.top, 12)
                ScrollView {
                    VStack(spacing: 0) {
                        ForEach(rooms, id: \.0) { room in
                            VStack(alignment: .leading, spacing: 4) {
                                Text(room.1, font: .system(size: 15, weight: .semibold), color: DesignTokens.ink)
                                Text(room.2, font: .system(size: 12), color: DesignTokens.mute)
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(.vertical, 12)
                            .padding(.horizontal, 16)
                            .contentShape(Rectangle())
                            .onTapGesture { roomId = room.0 }
                            Divider().overlay(DesignTokens.hairline)
                        }
                    }
                    .padding(.top, 8)
                }
                .background(DesignTokens.canvasSoft2.ignoresSafeArea())
            }
        }
    }
}

private struct AiStreamBubble: Identifiable {
    let id: String
    let role: String
    var text: String
}

struct NativeAiStreamPage: View {
    var onClose: () -> Void
    @State private var input = ""
    @State private var streaming = false
    @State private var stopRequested = false
    @State private var bubbles: [AiStreamBubble] = [
        AiStreamBubble(
            id: "0",
            role: "welcome",
            text: "你好，我是 AI 小石头——本 App / 4S 店的业务向导。你可以问「二手车入口在哪」「如何登录」或点下方快捷问。"
        ),
    ]
    private let chips = ["二手车入口在哪里？", "怎么登录账号？", "数据分析怎么看？"]
    private let welcomeBg = Color(red: 0xE8/255, green: 0xF0/255, blue: 0xFE/255)
    private let stopRed = Color(red: 0xE5/255, green: 0x39/255, blue: 0x35/255)

    private var showChips: Bool { bubbles.count <= 1 && !streaming }

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Button { onClose() } label: {
                    Image(systemName: "chevron.left").foregroundStyle(DesignTokens.link)
                }
                Text("AI 小石头", font: .system(size: 17, weight: .semibold), color: DesignTokens.ink)
                Spacer()
                if streaming {
                    Button("停止") { stopRequested = true }
                        .font(.system(size: 15, weight: .medium))
                        .foregroundStyle(stopRed)
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .background(DesignTokens.canvas)

            ScrollViewReader { proxy in
                ScrollView {
                    LazyVStack(alignment: .leading, spacing: 10) {
                        Color.clear.frame(height: 10)
                        ForEach(bubbles) { b in
                            bubbleView(b).id(b.id)
                        }
                        Color.clear.frame(height: 8)
                    }
                    .padding(.horizontal, 14)
                }
                .onChange(of: bubbles.last?.text) { _, _ in
                    if let last = bubbles.last {
                        withAnimation { proxy.scrollTo(last.id, anchor: .bottom) }
                    }
                }
            }
            .background(Color(white: 0.96))

            if showChips {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(chips, id: \.self) { chip in
                            Text(chip, font: .system(size: 13), color: DesignTokens.body)
                                .padding(.horizontal, 12)
                                .padding(.vertical, 8)
                                .background(Color.white, in: Capsule())
                                .overlay(Capsule().stroke(DesignTokens.hairline, lineWidth: 0.5))
                                .onTapGesture { send(chip) }
                        }
                    }
                    .padding(.horizontal, 14)
                }
                .padding(.bottom, 10)
            }

            HStack(spacing: 10) {
                TextField(streaming ? "生成中，请稍候…" : "输入问题…", text: $input)
                    .disabled(streaming)
                    .padding(.horizontal, 12)
                    .frame(height: 40)
                    .background(Color(white: 0.96), in: RoundedRectangle(cornerRadius: 20))
                Button("发送") { send(input) }
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(.white)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 10)
                    .background(
                        (streaming || input.trimmingCharacters(in: .whitespaces).isEmpty)
                            ? DesignTokens.mute : DesignTokens.link,
                        in: Capsule()
                    )
                    .disabled(streaming || input.trimmingCharacters(in: .whitespaces).isEmpty)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 10)
            .background(Color.white)
        }
    }

    @ViewBuilder
    private func bubbleView(_ b: AiStreamBubble) -> some View {
        switch b.role {
        case "user":
            HStack {
                Spacer(minLength: 48)
                Text(b.text, font: .system(size: 15), color: .white)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 10)
                    .background(DesignTokens.link, in: RoundedRectangle(cornerRadius: 16))
            }
        case "welcome":
            VStack(alignment: .leading, spacing: 6) {
                Text("AI 小石头", font: .system(size: 12, weight: .medium), color: DesignTokens.link)
                Text(b.text, font: .system(size: 15), color: DesignTokens.ink)
                    .lineSpacing(4)
            }
            .padding(14)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(welcomeBg, in: RoundedRectangle(cornerRadius: 16))
        default:
            VStack(alignment: .leading, spacing: 6) {
                Text("AI 小石头", font: .system(size: 12, weight: .medium), color: DesignTokens.link)
                Text(b.text.isEmpty ? "…" : b.text, font: .system(size: 15), color: DesignTokens.ink)
                    .lineSpacing(4)
            }
            .padding(14)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color.white, in: RoundedRectangle(cornerRadius: 16))
            .overlay(RoundedRectangle(cornerRadius: 16).stroke(DesignTokens.hairline, lineWidth: 0.5))
        }
    }

    private func mockReply(_ q: String) -> String {
        if q.contains("二手车") {
            return "二手车入口：首页「二手车」或全部服务 → 二手车（路由 /home/used_car）。需登录后查看车源列表。（mock）"
        }
        if q.contains("登录") {
            return "登录：我的 Tab 点头像/登录，或打开 /auth/login；支持密码与验证码（mock，真微信登录见 gap）。"
        }
        if q.contains("数据") {
            return "数据分析：首页/全部服务 →「数据分析」（/home/data_analytics），登录后可看门店指标。（mock）"
        }
        return "关于「\(q)」：我可以指路到二手车、登录、数据分析等业务入口。更多能力接 SSE 后开放。（mock 流）"
    }

    private func send(_ prompt: String) {
        let q = prompt.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !q.isEmpty, !streaming else { return }
        bubbles.append(AiStreamBubble(id: "u-\(bubbles.count)", role: "user", text: q))
        input = ""
        streaming = true
        stopRequested = false
        let full = mockReply(q)
        let id = "a-\(bubbles.count)"
        bubbles.append(AiStreamBubble(id: id, role: "assistant", text: ""))
        streamChars(id: id, full: full, index: 1)
    }

    private func streamChars(id: String, full: String, index: Int) {
        if stopRequested || index > full.count {
            streaming = false
            return
        }
        if let i = bubbles.firstIndex(where: { $0.id == id }) {
            bubbles[i].text = String(full.prefix(index))
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.028) {
            streamChars(id: id, full: full, index: index + 1)
        }
    }
}


struct NativeScanPage: View {
    var onClose: () -> Void
    var onOpen: ((String) -> Void)? = nil
    @State private var result: String? = nil
    @State private var torchOn = false
    /// Flutter WysScanConfig.borderColor ARGB(255, 255, 20, 147)
    private let pink = Color(red: 1, green: 20/255, blue: 147/255)

    var body: some View {
        VStack(spacing: 0) {
            if let result {
                navBar(title: "扫一扫", onClose: onClose, dark: false)
                VStack(alignment: .leading, spacing: 12) {
                    Text("扫码结果", font: .system(size: 15, weight: .semibold), color: DesignTokens.ink)
                    Text(result, font: .system(size: 15), color: DesignTokens.link)
                        .textSelection(.enabled)
                    Button("打开结果") {
                        onOpen?(result)
                        onClose()
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(DesignTokens.link)
                    Button("继续扫码") { self.result = nil }
                        .foregroundStyle(DesignTokens.link)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(16)
                Spacer()
                    .background(DesignTokens.canvasSoft2)
            } else {
                ZStack {
                    Color.black.ignoresSafeArea()
                    VStack(spacing: 0) {
                        HStack {
                            Button { onClose() } label: {
                                Text("‹", font: .system(size: 28), color: .white)
                                    .frame(width: 44, alignment: .leading)
                            }
                            Text("扫一扫", font: .system(size: 15), color: .white)
                                .frame(maxWidth: .infinity)
                            Button { torchOn.toggle() } label: {
                                Image(systemName: torchOn ? "flashlight.on.fill" : "flashlight.off.fill")
                                    .foregroundStyle(.white)
                                    .frame(width: 44, height: 44)
                            }
                        }
                        .padding(.horizontal, 8)
                        .frame(height: 44)

                        Spacer()
                        RoundedRectangle(cornerRadius: 10)
                            .stroke(pink, lineWidth: 3)
                            .frame(width: 280, height: 280)
                            .overlay(
                                Rectangle()
                                    .fill(pink.opacity(0.85))
                                    .frame(height: 2)
                                    .padding(.horizontal, 8)
                            )
                        Text("将二维码放入框内，即可自动扫码", font: .system(size: 14), color: .white.opacity(0.9))
                            .padding(.top, 20)
                        Spacer()
                        Button("模拟扫码成功") {
                            let payload = "myai://mall/orders?id=A1024"
                            result = payload
                        }
                        .buttonStyle(.borderedProminent)
                        .tint(pink)
                        .padding(.bottom, 40)
                    }
                }
            }
        }
        .background(DesignTokens.canvasSoft2.ignoresSafeArea())
    }
}

struct NativeStrategyPage: View {
    var onClose: () -> Void
    @State private var tab = 0
    @State private var period = 4
    private let tabs = ["推荐", "逆向", "趋势"]
    private let periods = ["今年来", "近1周", "近1月", "近3月", "近1年"]
    private let assets: [(String, String, Bool)] = [
        ("A股", "+19.22%", true), ("中债", "+3.15%", true), ("黄金", "+8.76%", true),
        ("港股", "+12.40%", true), ("美股", "+15.88%", true), ("原油", "-2.34%", false),
        ("美元债", "-1.80%", false), ("商品", "+4.56%", true), ("现金", "+1.20%", true),
    ]
    private let columns = [GridItem(.flexible()), GridItem(.flexible()), GridItem(.flexible())]
    private let gainRed = Color(red: 1, green: 59/255, blue: 48/255)
    private let gainGreen = Color(red: 52/255, green: 199/255, blue: 89/255)

    var body: some View {
        VStack(spacing: 0) {
            navBar(title: "策略", onClose: onClose, dark: false)
            ScrollView {
                VStack(spacing: 16) {
                    HStack(spacing: 0) {
                        ForEach(Array(tabs.enumerated()), id: \.offset) { i, label in
                            VStack(spacing: 8) {
                                Text(label, font: .system(size: 16, weight: tab == i ? .semibold : .regular),
                                     color: tab == i ? DesignTokens.ink : DesignTokens.body)
                                RoundedRectangle(cornerRadius: 2)
                                    .fill(tab == i ? DesignTokens.link : Color.clear)
                                    .frame(width: tab == i ? 24 : 0, height: 3)
                            }
                            .padding(.horizontal, 16)
                            .onTapGesture { tab = i }
                        }
                    }
                    .frame(maxWidth: .infinity)

                    VStack(alignment: .leading, spacing: 16) {
                        Text(
                            "「大类资产九宫格策略」通过分散配置降低波动，帮助你在不同市场环境下保持稳健收益。",
                            font: .system(size: 13),
                            color: DesignTokens.ink
                        )
                        .lineSpacing(4)

                        LazyVGrid(columns: columns, spacing: 8) {
                            ForEach(Array(assets.enumerated()), id: \.offset) { _, a in
                                VStack(spacing: 4) {
                                    Text(a.0, font: .system(size: 12), color: DesignTokens.ink)
                                    Text(a.1, font: .system(size: 14, weight: .bold),
                                         color: a.2 ? gainRed : gainGreen)
                                }
                                .frame(maxWidth: .infinity)
                                .padding(8)
                                .background(
                                    (a.2 ? gainRed : gainGreen).opacity(0.08),
                                    in: RoundedRectangle(cornerRadius: 8)
                                )
                            }
                        }

                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 16) {
                                ForEach(Array(periods.enumerated()), id: \.offset) { i, p in
                                    Text(p, font: .system(size: 13, weight: period == i ? .semibold : .regular),
                                         color: period == i ? DesignTokens.link : DesignTokens.body)
                                        .onTapGesture { period = i }
                                }
                            }
                        }
                    }
                    .padding(16)
                    .background(Color.white, in: RoundedRectangle(cornerRadius: 12))
                    .overlay(RoundedRectangle(cornerRadius: 12).stroke(DesignTokens.hairline, lineWidth: 0.5))

                    VStack(alignment: .leading, spacing: 0) {
                        HStack(alignment: .top) {
                            VStack(alignment: .leading, spacing: 6) {
                                Text("黄金恐贪定投 · 第一期", font: .system(size: 17, weight: .semibold), color: DesignTokens.ink)
                                Text(tabs[tab], font: .system(size: 11, weight: .semibold), color: DesignTokens.link)
                                    .padding(.horizontal, 8).padding(.vertical, 3)
                                    .background(DesignTokens.link.opacity(0.1), in: RoundedRectangle(cornerRadius: 4))
                            }
                            Spacer()
                            Text("如何跟投", font: .system(size: 14, weight: .medium), color: DesignTokens.link)
                        }
                        Spacer().frame(height: 20)
                        HStack(alignment: .bottom) {
                            VStack(alignment: .leading, spacing: 4) {
                                Text("-11.35%", font: .system(size: 32, weight: .bold), color: gainGreen)
                                Text("本期收益率", font: .system(size: 13), color: DesignTokens.body)
                            }
                            Spacer()
                            VStack(spacing: 4) {
                                Text("恐贪指数", font: .system(size: 11), color: DesignTokens.body)
                                Text("63 中立", font: .system(size: 14, weight: .semibold), color: DesignTokens.ink)
                            }
                        }
                        Spacer().frame(height: 20)
                        Text("定投进度", font: .system(size: 13), color: DesignTokens.body)
                        Spacer().frame(height: 8)
                        ZStack {
                            ProgressView(value: 36, total: 50)
                                .tint(DesignTokens.link)
                                .scaleEffect(x: 1, y: 2.2, anchor: .center)
                            Text("36 / 50", font: .system(size: 12, weight: .semibold), color: DesignTokens.ink)
                        }
                        .frame(height: 24)
                        Spacer().frame(height: 12)
                        HStack {
                            Text("本周已投 1 份", font: .system(size: 13), color: DesignTokens.body)
                            Spacer()
                            Text("订阅", font: .system(size: 14, weight: .semibold), color: .white)
                                .padding(.horizontal, 16).padding(.vertical, 8)
                                .background(DesignTokens.link, in: RoundedRectangle(cornerRadius: 8))
                        }
                        Spacer().frame(height: 16)
                        Text(
                            "在恐慌时买入、贪婪时卖出，通过定期定额降低择时压力，适合长期持有的投资者。",
                            font: .system(size: 13),
                            color: DesignTokens.body
                        )
                        .lineSpacing(4)
                    }
                    .padding(16)
                    .background(Color.white, in: RoundedRectangle(cornerRadius: 12))
                    .overlay(RoundedRectangle(cornerRadius: 12).stroke(DesignTokens.hairline, lineWidth: 0.5))
                }
                .padding(.horizontal, 16)
                .padding(.top, 8)
                .padding(.bottom, 24)
            }
            .background(Color(white: 0.96).ignoresSafeArea())
        }
    }
}


struct NativeCheckInMallPage: View {
    var onClose: () -> Void
    @State private var points = 1280
    @State private var streak = 3
    @State private var checkedToday = false
    @State private var remind = false
    @State private var checkingIn = false
    @State private var toastText: String? = nil

    private struct DayCell {
        let label: String
        let reward: Int
        let signed: Bool
        let isToday: Bool
    }
    private struct TaskRow {
        let title: String
        let points: Int
        let action: String
    }

    private let headerBlue = DesignTokens.link
    private let coinGold = Color(red: 0xF5/255, green: 0xA6/255, blue: 0x23/255)
    private let calendar: [DayCell] = [
        .init(label: "19", reward: 5, signed: true, isToday: false),
        .init(label: "20", reward: 5, signed: true, isToday: false),
        .init(label: "21", reward: 5, signed: true, isToday: false),
        .init(label: "22", reward: 5, signed: false, isToday: false),
        .init(label: "23", reward: 5, signed: false, isToday: false),
        .init(label: "24", reward: 5, signed: false, isToday: false),
        .init(label: "今天", reward: 10, signed: false, isToday: true),
    ]
    private let tasks: [TaskRow] = [
        .init(title: "每日登录", points: 5, action: "领取"),
        .init(title: "发一条动态", points: 10, action: "去完成"),
        .init(title: "商城下单", points: 20, action: "去完成"),
    ]

    var body: some View {
        ZStack(alignment: .bottom) {
            VStack(spacing: 0) {
                // Flutter header chrome: nav + notice + stats
                VStack(spacing: 0) {
                    HStack {
                        Button { onClose() } label: {
                            Text("‹", font: .system(size: 28), color: .white)
                                .frame(width: 44, alignment: .leading)
                        }
                        Text("签到商城", font: .system(size: 18, weight: .semibold), color: .white)
                            .frame(maxWidth: .infinity)
                        Color.clear.frame(width: 44, height: 1)
                    }
                    .padding(.horizontal, 4)
                    .frame(height: 44)

                    HStack(spacing: 8) {
                        Text("🔊").font(.system(size: 14))
                        Text(
                            "温馨提示：本页面只保留近3个月内的积分记录",
                            font: .system(size: 12),
                            color: .white
                        )
                        .lineLimit(1)
                        Spacer(minLength: 0)
                    }
                    .padding(.horizontal, 12)
                    .padding(.vertical, 8)
                    .background(Color(red: 0x3A/255, green: 0x8E/255, blue: 0xE6/255), in: RoundedRectangle(cornerRadius: 4))
                    .padding(.horizontal, 16)

                    HStack(alignment: .top) {
                        VStack(alignment: .leading, spacing: 4) {
                            Text("我的积分", font: .system(size: 13), color: .white.opacity(0.8))
                            Text("\(points)", font: .system(size: 32, weight: .bold), color: .white)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        VStack(alignment: .leading, spacing: 4) {
                            Text("连续签到", font: .system(size: 13), color: .white.opacity(0.8))
                            HStack(alignment: .bottom, spacing: 4) {
                                Text("\(streak)", font: .system(size: 32, weight: .bold), color: .white)
                                Text("天", font: .system(size: 13), color: .white.opacity(0.8))
                                    .padding(.bottom, 6)
                            }
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                    }
                    .padding(.horizontal, 24)
                    .padding(.top, 16)
                    .padding(.bottom, 16)
                }
                .background(headerBlue.ignoresSafeArea(edges: .top))

                ScrollView {
                    VStack(alignment: .leading, spacing: 16) {
                        // Check-in card
                        VStack(alignment: .leading, spacing: 16) {
                            HStack(alignment: .center) {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text("连签可得更多积分", font: .system(size: 15, weight: .semibold), color: DesignTokens.ink)
                                    HStack(spacing: 0) {
                                        Text("已连续签到 ", font: .system(size: 12), color: DesignTokens.body)
                                        Text("\(streak)", font: .system(size: 12, weight: .semibold), color: headerBlue)
                                        Text(" 天", font: .system(size: 12), color: DesignTokens.body)
                                    }
                                }
                                Spacer()
                                Button {
                                    guard !checkedToday && !checkingIn else { return }
                                    checkingIn = true
                                    checkedToday = true
                                    points += 10
                                    streak += 1
                                    checkingIn = false
                                    flashToast("签到成功，+10积分")
                                } label: {
                                    Text(
                                        checkedToday ? "已签到" : (checkingIn ? "签到中…" : "立即签到"),
                                        font: .system(size: 14, weight: .semibold),
                                        color: checkedToday ? DesignTokens.mute : .white
                                    )
                                    .padding(.horizontal, 16)
                                    .padding(.vertical, 8)
                                    .background(
                                        checkedToday ? Color(red: 0xF5/255, green: 0xF6/255, blue: 0xF8/255) : headerBlue,
                                        in: Capsule()
                                    )
                                }
                                .disabled(checkedToday || checkingIn)
                            }

                            HStack(spacing: 0) {
                                ForEach(Array(calendar.enumerated()), id: \.offset) { _, day in
                                    dayCell(day)
                                }
                            }

                            HStack {
                                Text("断签或者签完需重新开始", font: .system(size: 12), color: DesignTokens.mute)
                                Spacer()
                                Text("签到提醒", font: .system(size: 12), color: DesignTokens.body)
                                Toggle("", isOn: $remind)
                                    .labelsHidden()
                                    .tint(headerBlue)
                                    .scaleEffect(0.8)
                            }
                        }
                        .padding(16)
                        .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 12))
                        .padding(.horizontal, 16)
                        .padding(.top, 16)

                        Text("成长任务", font: .system(size: 16, weight: .semibold), color: DesignTokens.ink)
                            .padding(.horizontal, 16)

                        VStack(spacing: 0) {
                            ForEach(Array(tasks.enumerated()), id: \.offset) { idx, task in
                                if idx > 0 {
                                    Divider().background(DesignTokens.hairline)
                                }
                                HStack {
                                    VStack(alignment: .leading, spacing: 2) {
                                        Text(task.title, font: .system(size: 15, weight: .medium), color: DesignTokens.ink)
                                        Text("+\(task.points)积分", font: .system(size: 12), color: DesignTokens.body)
                                    }
                                    Spacer()
                                    Button {
                                        if task.action == "领取" {
                                            points += task.points
                                            flashToast("领取成功，+\(task.points)积分")
                                        } else {
                                            flashToast("去完成：\(task.title)")
                                        }
                                    } label: {
                                        Text(task.action, font: .system(size: 13, weight: .semibold), color: headerBlue)
                                            .padding(.horizontal, 12)
                                            .padding(.vertical, 6)
                                            .background(headerBlue.opacity(0.1), in: Capsule())
                                    }
                                }
                                .padding(.horizontal, 14)
                                .padding(.vertical, 12)
                            }
                        }
                        .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 12))
                        .padding(.horizontal, 16)

                        Text("积分换礼", font: .system(size: 16, weight: .semibold), color: DesignTokens.ink)
                            .padding(.horizontal, 16)

                        VStack(spacing: 16) {
                            Text("🎁").font(.system(size: 64)).opacity(0.3)
                            Text("暂无积分商品", font: .system(size: 14), color: DesignTokens.mute)
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 40)
                    }
                    .padding(.bottom, 24)
                }
                .background(DesignTokens.canvasSoft2)
            }

            if let toastText {
                Text(toastText, font: .system(size: 14), color: .white)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 10)
                    .background(Color.black.opacity(0.78), in: Capsule())
                    .padding(.bottom, 40)
                    .transition(.opacity)
            }
        }
    }


    @ViewBuilder
    private func dayCell(_ day: DayCell) -> some View {
        let signed = day.signed || (day.isToday && checkedToday)
        let bg: Color = {
            if signed { return headerBlue }
            if day.isToday { return coinGold }
            return Color(red: 0xF5/255, green: 0xF6/255, blue: 0xF8/255)
        }()
        let fg: Color = (signed || day.isToday) ? .white : DesignTokens.body
        VStack(spacing: 4) {
            VStack(spacing: 2) {
                Text("+\(day.reward)", font: .system(size: 11, weight: .semibold), color: fg)
                Text("›", font: .system(size: 10), color: fg.opacity(0.8))
            }
            .frame(maxWidth: .infinity)
            .aspectRatio(1, contentMode: .fit)
            .background(bg, in: RoundedRectangle(cornerRadius: 8))
            Text(
                signed ? "已签" : day.label,
                font: .system(size: 11),
                color: signed ? headerBlue : DesignTokens.body
            )
            .lineLimit(1)
        }
        .padding(.horizontal, 2)
        .frame(maxWidth: .infinity)
    }

    private func flashToast(_ text: String) {
        toastText = text
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.6) {
            if toastText == text { toastText = nil }
        }
    }
}

struct NativeMusicPage: View {
    var onClose: () -> Void
    @State private var playingTitle: String? = nil
    @State private var playingArtist: String = ""
    /// Align Compose FlutterTracks
    private let tracks: [(String, String)] = [
        ("Ya Ali - DJMaza.Com", "DJMaza"),
        ("Ek Do Teen - DJMaza.Info", "DJMaza"),
        ("16 yeh dil diwana hai", "Classic"),
        ("Shape of You", "Ed Sheeran"),
        ("Blinding Lights", "The Weeknd"),
        ("Levitating", "Dua Lipa"),
    ]
    var body: some View {
        ZStack(alignment: .bottom) {
            VStack(spacing: 0) {
                HStack {
                    Button(action: onClose) {
                        Text("‹", font: .system(size: 28), color: DesignTokens.link)
                            .frame(width: 44, alignment: .leading)
                    }
                    Text("音频列表", font: .system(size: 17, weight: .semibold), color: DesignTokens.ink)
                        .frame(maxWidth: .infinity)
                    if playingTitle != nil {
                        Text("Now Playing", font: .system(size: 14), color: DesignTokens.link)
                            .frame(width: 96, alignment: .trailing)
                    } else {
                        Color.clear.frame(width: 96)
                    }
                }
                .padding(.horizontal, 8)
                .frame(height: 44)
                .background(DesignTokens.canvas)

                ScrollView {
                    VStack(spacing: 8) {
                        ForEach(Array(tracks.enumerated()), id: \.offset) { _, t in
                            VStack(alignment: .leading, spacing: 4) {
                                Text(t.0, font: .system(size: 15, weight: .medium), color: DesignTokens.ink)
                                Text("By \(t.1)", font: .system(size: 12), color: DesignTokens.mute)
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(16)
                            .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 8))
                            .onTapGesture {
                                playingTitle = t.0
                                playingArtist = t.1
                            }
                        }
                    }
                    .padding(16)
                    .padding(.bottom, playingTitle == nil ? 16 : 72)
                }
                .background(DesignTokens.canvasSoft2.ignoresSafeArea())
            }

            if let title = playingTitle {
                VStack(spacing: 0) {
                    Divider().overlay(DesignTokens.hairline)
                    HStack {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(title, font: .system(size: 14, weight: .medium), color: DesignTokens.ink)
                                .lineLimit(1)
                            Text(playingArtist, font: .system(size: 12), color: DesignTokens.mute)
                        }
                        Spacer()
                        Button("关闭") {
                            playingTitle = nil
                            playingArtist = ""
                        }
                        .foregroundStyle(DesignTokens.body)
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 10)
                    .background(DesignTokens.canvas)
                }
            }
        }
    }
}


struct NativeHomeworkStatsPage: View {
    var onClose: () -> Void
    var onOpen: ((String) -> Void)? = nil
    private let rows: [(String, String, String)] = [
        ("语法练习 3", "书面", "今日 23:59"),
        ("配音作业 · 致橡树", "配音", "明日 18:00"),
        ("听力精听", "听力", "本周六"),
    ]

    var body: some View {
        VStack(spacing: 0) {
            navBar(title: "作业统计", onClose: onClose, dark: false)
            ScrollView {
                VStack(alignment: .leading, spacing: 10) {
                    Text("班级作业概览（mock）", font: .system(size: 13), color: DesignTokens.body)
                    ForEach(Array(rows.enumerated()), id: \.offset) { _, row in
                        VStack(alignment: .leading, spacing: 4) {
                            Text(row.0, font: .system(size: 15, weight: .semibold), color: DesignTokens.ink)
                            Text("\(row.1) · 截止 \(row.2)", font: .system(size: 12), color: DesignTokens.mute)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(14)
                        .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 10))
                        .onTapGesture {
                            if row.1 == "配音" {
                                onOpen?("/classroom/homework/dubbing")
                            } else {
                                onOpen?("/classroom/homework/detail_teacher")
                            }
                        }
                    }
                    Button("学生视角详情 →") { onOpen?("/classroom/homework/detail_student") }
                        .font(.system(size: 15, weight: .medium))
                        .foregroundStyle(DesignTokens.link)
                    Button("作业点评 →") { onOpen?("/classroom/homework/review") }
                        .font(.system(size: 15, weight: .medium))
                        .foregroundStyle(DesignTokens.link)
                    Button("课堂视频 →") { onOpen?("/classroom/video") }
                        .font(.system(size: 15, weight: .medium))
                        .foregroundStyle(DesignTokens.link)
                    Button("领取礼品卡 →") { onOpen?("/classroom/gift/claim") }
                        .font(.system(size: 15, weight: .medium))
                        .foregroundStyle(DesignTokens.link)
                }
                .padding(16)
            }
            .background(DesignTokens.canvasSoft2.ignoresSafeArea())
        }
    }
}

struct NativeGiftClaimPage: View {
    var onClose: () -> Void
    @State private var claimed = false
    @State private var toastText: String? = nil
    private let noteBg = Color(red: 0.97, green: 0.96, blue: 0.94)
    private let green = Color(red: 0.20, green: 0.78, blue: 0.35)

    var body: some View {
        ZStack(alignment: .bottom) {
            VStack(spacing: 0) {
                navBar(title: "领取礼品卡", onClose: onClose, dark: false)
                ScrollView {
                    VStack(spacing: 24) {
                        VStack {
                            ZStack(alignment: .topLeading) {
                                LinearGradient(
                                    colors: [Color(red: 0.086, green: 0.467, blue: 1), Color(red: 0.035, green: 0.345, blue: 0.851)],
                                    startPoint: .topLeading, endPoint: .bottomTrailing
                                )
                                VStack(alignment: .leading, spacing: 0) {
                                    HStack(spacing: 8) {
                                        Text("🦜").frame(width: 28, height: 28)
                                            .background(Circle().fill(Color.white))
                                        Text("iHome", font: .system(size: 13), color: .white)
                                    }
                                    Spacer()
                                    Text("Way to go ✨", font: .system(size: 28, weight: .bold).italic(), color: .white)
                                        .frame(maxWidth: .infinity)
                                    Spacer()
                                    HStack(alignment: .bottom) {
                                        VStack(alignment: .leading, spacing: 4) {
                                            Text("1天 AI SVIP", font: .system(size: 14), color: .white)
                                            Text("班级会员卡", font: .system(size: 10), color: .white)
                                                .padding(.horizontal, 8).padding(.vertical, 2)
                                                .background(Color.white.opacity(0.2), in: Capsule())
                                        }
                                        Spacer()
                                        Text("🧑‍🎓", font: .system(size: 48))
                                    }
                                }
                                .padding(16)
                            }
                            .frame(height: 180)
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                        }
                        .padding(16)
                        .background(Color.white, in: RoundedRectangle(cornerRadius: 16))

                        VStack(alignment: .leading, spacing: 12) {
                            Text("📎", font: .system(size: 18), color: Color(white: 0.6))
                            Text("乌克丽丽 同学：", font: .system(size: 15, weight: .medium), color: DesignTokens.ink)
                            Text("本次作业完成的很棒！老师送你一张体验卡，以资鼓励",
                                  font: .system(size: 14), color: DesignTokens.ink)
                            HStack {
                                Spacer()
                                VStack(alignment: .trailing, spacing: 4) {
                                    Text("老坛酸菜", font: .system(size: 14), color: DesignTokens.ink)
                                    Text("2026-05-20", font: .system(size: 13), color: DesignTokens.mute)
                                }
                            }
                        }
                        .padding(EdgeInsets(top: 24, leading: 20, bottom: 20, trailing: 20))
                        .background(Color.white, in: RoundedRectangle(cornerRadius: 4))

                        Button(claimed ? "已领取" : "立即领取") {
                            guard !claimed else { return }
                            claimed = true
                            flash("领取成功，可在背包中查看")
                        }
                        .buttonStyle(.borderedProminent)
                        .tint(green)
                        .disabled(claimed)
                        .frame(maxWidth: .infinity)
                        .frame(height: 48)
                        .clipShape(Capsule())
                    }
                    .padding(16)
                }
            }
            .background(noteBg.ignoresSafeArea())
            if let toastText {
                Text(toastText, font: .system(size: 14), color: .white)
                    .padding(.horizontal, 16).padding(.vertical, 10)
                    .background(Color.black.opacity(0.78), in: Capsule())
                    .padding(.bottom, 40)
            }
        }
    }

    private func flash(_ text: String) {
        toastText = text
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.6) {
            if toastText == text { toastText = nil }
        }
    }
}

struct NativeHomeworkDetailPage: View {
    var title: String
    var detail: String
    var onClose: () -> Void
    var action: String? = nil
    @State private var toastText: String? = nil

    var body: some View {
        ZStack(alignment: .bottom) {
            VStack(spacing: 0) {
                navBar(title: title, onClose: onClose, dark: false)
                VStack(alignment: .leading, spacing: 16) {
                    Text(detail, font: .system(size: 15), color: DesignTokens.body)
                    if let action {
                        Button(action) { flash(action + "（mock）") }
                            .buttonStyle(.borderedProminent)
                            .tint(DesignTokens.link)
                    }
                    Spacer()
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(16)
            }
            .background(DesignTokens.canvasSoft2.ignoresSafeArea())
            if let toastText {
                Text(toastText, font: .system(size: 14), color: .white)
                    .padding(.horizontal, 16).padding(.vertical, 10)
                    .background(Color.black.opacity(0.78), in: Capsule())
                    .padding(.bottom, 40)
            }
        }
    }

    private func flash(_ text: String) {
        toastText = text
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.6) {
            if toastText == text { toastText = nil }
        }
    }
}

struct NativeClassroomPage: View {
    var onClose: () -> Void
    var onOpen: ((String) -> Void)? = nil
    private let classes: [(String, String)] = [
        ("产品知识班", "未交作业 2"),
        ("配音作业", "待批改 1"),
        ("销售话术营", "进行中"),
    ]
    var body: some View {
        VStack(spacing: 0) {
            navBar(title: "我的课程", onClose: onClose, dark: false)
            ScrollView {
                VStack(spacing: 12) {
                    ForEach(Array(classes.enumerated()), id: \.offset) { _, c in
                        HStack {
                            RoundedRectangle(cornerRadius: 10)
                                .fill(DesignTokens.link.opacity(0.12))
                                .frame(width: 48, height: 48)
                                .overlay(Image(systemName: "book.fill").foregroundStyle(DesignTokens.link))
                            VStack(alignment: .leading, spacing: 4) {
                                Text(c.0, font: .system(size: 16, weight: .semibold), color: DesignTokens.ink)
                                Text(c.1, font: .system(size: 13), color: DesignTokens.body)
                            }
                            Spacer()
                            Image(systemName: "chevron.right").foregroundStyle(DesignTokens.mute)
                        }
                        .padding(16)
                        .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 12))
                        .onTapGesture { onOpen?("/classroom/homework_stats") }
                    }
                }
                .padding(16)
            }
            .background(DesignTokens.canvasSoft2.ignoresSafeArea())
        }
    }
}


struct NativeUsedCarPage: View {
    var onClose: () -> Void
    var onOpen: ((String) -> Void)? = nil
    @State private var status = "全部"
    @State private var kind = "全部类型"
    private let accent = Color(red: 0.043, green: 0.431, blue: 0.310)
    private let ink = Color(red: 0.110, green: 0.141, blue: 0.188)
    private let bg = Color(red: 0.953, green: 0.961, blue: 0.973)
    private let statusTabs = ["全部", "待审核", "已通过", "未通过"]
    private let kindTabs = ["全部类型", "置换", "专卖", "收车"]
    /// Align Compose HomeSecondaryMock.usedCarOrders
    private let orders: [(kind: String, status: String, date: String, model: String, plate: String, amountLabel: String, amount: String, customer: String)] = [
        ("置换", "待审核", "2026-09-22", "2021 帝豪", "京A·88X21", "评估价", "¥86,000", "张先生"),
        ("专卖", "已通过", "2026-09-18", "2020 星越L", "沪B·6K902", "成交价", "¥152,000", "李女士"),
        ("收车", "已提交", "2026-09-15", "2019 博越", "粤C·19H33", "收车价", "¥79,000", "王先生"),
    ]
    private var filtered: [(kind: String, status: String, date: String, model: String, plate: String, amountLabel: String, amount: String, customer: String)] {
        orders.filter { row in
            let statusOk = status == "全部" || row.status == status || (status == "待审核" && row.status == "已提交")
            let kindOk = kind == "全部类型" || row.kind == kind
            return statusOk && kindOk
        }
    }
    private var submitted: Int { orders.filter { $0.status == "已提交" }.count }
    private var pending: Int { orders.filter { $0.status == "待审核" || $0.status == "已提交" }.count }
    private var approved: Int { orders.filter { $0.status == "已通过" }.count }
    private var rejected: Int { orders.filter { $0.status == "未通过" }.count }

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Button { onClose() } label: { Image(systemName: "chevron.left").foregroundStyle(accent) }
                Text("二手车", font: .system(size: 17, weight: .semibold), color: ink)
                Spacer()
                Button("新建") { onOpen?("/home/used_car/create") }
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(accent)
            }
            .padding(.horizontal, 16).padding(.vertical, 12)
            .background(Color.white)

            ScrollView {
                VStack(spacing: 0) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("销售顾问", font: .system(size: 18, weight: .bold), color: .white)
                        Text("门店顾问 · 演示门店", font: .system(size: 13), color: .white.opacity(0.85))
                        HStack {
                            ForEach([("已提交", submitted), ("待审核", pending), ("已通过", approved), ("未通过", rejected)], id: \.0) { label, value in
                                VStack(spacing: 2) {
                                    Text("\(value)", font: .system(size: 18, weight: .bold), color: .white)
                                    Text(label, font: .system(size: 11), color: .white.opacity(0.8))
                                }
                                .frame(maxWidth: .infinity)
                            }
                        }
                        .padding(.top, 10)
                    }
                    .padding(16)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(
                        LinearGradient(colors: [accent, Color(red: 0.078, green: 0.620, blue: 0.435)], startPoint: .topLeading, endPoint: .bottomTrailing),
                        in: RoundedRectangle(cornerRadius: 16)
                    )
                    .padding(.horizontal, 16).padding(.top, 12).padding(.bottom, 8)

                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(statusTabs, id: \.self) { tab in
                                Text(tab, font: .system(size: 13), color: status == tab ? .white : ink)
                                    .padding(.horizontal, 14).padding(.vertical, 8)
                                    .background(status == tab ? accent : Color(red: 0.91, green: 0.925, blue: 0.941), in: Capsule())
                                    .onTapGesture { status = tab }
                            }
                        }
                        .padding(.horizontal, 16)
                    }
                    .padding(.bottom, 8)

                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(kindTabs, id: \.self) { tab in
                                Text(tab, font: .system(size: 13, weight: kind == tab ? .semibold : .regular),
                                      color: kind == tab ? accent : DesignTokens.mute)
                                    .padding(.horizontal, 14).padding(.vertical, 8)
                                    .background(kind == tab ? accent.opacity(0.08) : Color.white, in: Capsule())
                                    .onTapGesture { kind = tab }
                            }
                        }
                        .padding(.horizontal, 16)
                    }
                    .padding(.bottom, 12)

                    if filtered.isEmpty {
                        VStack(spacing: 8) {
                            Text("暂无业务单", font: .system(size: 16, weight: .semibold), color: ink)
                            Text("点击右上角新建置换 / 专卖 / 收车单", font: .system(size: 13), color: DesignTokens.mute)
                        }
                        .padding(48)
                    } else {
                        ForEach(Array(filtered.enumerated()), id: \.offset) { _, row in
                            VStack(alignment: .leading, spacing: 8) {
                                HStack {
                                    Text(row.kind, font: .system(size: 12, weight: .semibold), color: accent)
                                        .padding(.horizontal, 8).padding(.vertical, 3)
                                        .background(accent.opacity(0.1), in: RoundedRectangle(cornerRadius: 4))
                                    Spacer()
                                    Text(row.status, font: .system(size: 12), color: DesignTokens.mute)
                                }
                                Text(row.model, font: .system(size: 16, weight: .semibold), color: ink)
                                Text("\(row.plate) · \(row.customer)", font: .system(size: 13), color: DesignTokens.body)
                                HStack {
                                    Text(row.amountLabel, font: .system(size: 12), color: DesignTokens.mute)
                                    Text(row.amount, font: .system(size: 15, weight: .semibold), color: ink)
                                    Spacer()
                                    Text(row.date, font: .system(size: 12), color: DesignTokens.mute)
                                }
                            }
                            .padding(14)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .background(Color.white, in: RoundedRectangle(cornerRadius: 14))
                            .padding(.horizontal, 16).padding(.vertical, 5)
                            .onTapGesture { onOpen?("/home/used_car/detail") }
                        }
                    }
                }
                .padding(.bottom, 24)
            }
            .background(bg.ignoresSafeArea())
        }
    }
}

struct NativeNewCarFollowPage: View {
    var onClose: () -> Void
    var onOpen: ((String) -> Void)? = nil
    @State private var tab = "全部"
    private let accent = Color(red: 0.231, green: 0.549, blue: 1)
    private let ink = Color(red: 0.102, green: 0.102, blue: 0.102)
    private let bg = Color(red: 0.961, green: 0.965, blue: 0.973)
    private let tabs = ["全部", "高意向", "中意向", "低意向", "逾期"]
    /// Align Compose HomeSecondaryMock.newCarFollows
    private let rows: [(name: String, phone: String, vehicle: String, stage: String, intent: String, next: String, overdue: Bool)] = [
        ("孙某", "138****2101", "银河 L7", "跟进中", "高", "今日 15:00", false),
        ("吴某", "139****8820", "星愿", "报价", "中", "明日 10:30", false),
        ("赵某", "186****4412", "星越 L", "试驾", "低", "09-20 已逾期", true),
    ]
    private var filtered: [(name: String, phone: String, vehicle: String, stage: String, intent: String, next: String, overdue: Bool)] {
        switch tab {
        case "高意向": return rows.filter { $0.intent == "高" }
        case "中意向": return rows.filter { $0.intent == "中" }
        case "低意向": return rows.filter { $0.intent == "低" }
        case "逾期": return rows.filter { $0.overdue }
        default: return rows
        }
    }
    private var active: Int { rows.filter { !$0.overdue }.count }
    private var overdueCount: Int { rows.filter { $0.overdue }.count }
    private var high: Int { rows.filter { $0.intent == "高" }.count }

    var body: some View {
        ZStack(alignment: .bottomTrailing) {
            VStack(spacing: 0) {
                HStack {
                    Button { onClose() } label: { Image(systemName: "chevron.left").foregroundStyle(DesignTokens.ink) }
                    Text("新车跟进", font: .system(size: 17, weight: .semibold), color: ink)
                    Spacer()
                }
                .padding(.horizontal, 16).padding(.vertical, 12).background(Color.white)

                ScrollView {
                    VStack(spacing: 0) {
                        VStack(alignment: .leading, spacing: 0) {
                            HStack {
                                VStack(alignment: .leading, spacing: 6) {
                                    HStack(spacing: 8) {
                                        Text("销售顾问", font: .system(size: 20, weight: .bold), color: ink)
                                        Text("顾问", font: .system(size: 12), color: .white)
                                            .padding(.horizontal, 8).padding(.vertical, 3)
                                            .background(accent, in: RoundedRectangle(cornerRadius: 4))
                                    }
                                    Text("演示门店", font: .system(size: 13), color: DesignTokens.mute)
                                }
                                Spacer()
                                Text("销", font: .system(size: 22, weight: .bold), color: accent)
                                    .frame(width: 56, height: 56)
                                    .background(Color(red: 0.91, green: 0.933, blue: 0.973), in: Circle())
                            }
                            HStack {
                                ForEach([("\(active)", "跟进中"), ("\(overdueCount)", "逾期"), ("\(high)", "高意向"), ("0", "战败")], id: \.1) { v, label in
                                    VStack(spacing: 4) {
                                        Text(v, font: .system(size: 18, weight: .bold), color: ink)
                                        Text(label, font: .system(size: 12), color: DesignTokens.mute)
                                    }
                                    .frame(maxWidth: .infinity)
                                }
                            }
                            .padding(.top, 20)
                        }
                        .padding(16)
                        .background(Color.white, in: RoundedRectangle(cornerRadius: 12))
                        .padding(.horizontal, 16).padding(.top, 12).padding(.bottom, 8)

                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 0) {
                                ForEach(tabs, id: \.self) { t in
                                    VStack(spacing: 6) {
                                        Text(t, font: .system(size: 14, weight: tab == t ? .semibold : .regular),
                                              color: tab == t ? accent : DesignTokens.mute)
                                        Rectangle().fill(tab == t ? accent : Color.clear).frame(width: 20, height: 2)
                                    }
                                    .padding(.horizontal, 12).padding(.vertical, 10)
                                    .onTapGesture { tab = t }
                                }
                            }
                            .padding(.horizontal, 8)
                        }

                        ForEach(Array(filtered.enumerated()), id: \.offset) { _, r in
                            VStack(alignment: .leading, spacing: 8) {
                                HStack {
                                    Text(r.name, font: .system(size: 16, weight: .semibold), color: ink)
                                    Text(r.intent + "意向", font: .system(size: 11, weight: .medium),
                                          color: r.intent == "高" ? Color.red : accent)
                                        .padding(.horizontal, 6).padding(.vertical, 2)
                                        .background((r.intent == "高" ? Color.red : accent).opacity(0.12), in: Capsule())
                                    Spacer()
                                    if r.overdue {
                                        Text("逾期", font: .system(size: 11, weight: .medium), color: .white)
                                            .padding(.horizontal, 6).padding(.vertical, 2)
                                            .background(Color.red, in: Capsule())
                                    }
                                }
                                Text("\(r.vehicle) · \(r.stage)", font: .system(size: 13), color: DesignTokens.body)
                                Text("\(r.phone) · 下次 \(r.next)", font: .system(size: 12), color: DesignTokens.mute)
                            }
                            .padding(14)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .background(Color.white, in: RoundedRectangle(cornerRadius: 12))
                            .padding(.horizontal, 16).padding(.vertical, 5)
                            .onTapGesture { onOpen?("/home/new_car_follow/detail") }
                        }
                    }
                    .padding(.bottom, 88)
                }
                .background(bg.ignoresSafeArea())
            }
            Button {
                onOpen?("/home/new_car_follow/create")
            } label: {
                Image(systemName: "plus")
                    .font(.system(size: 22, weight: .semibold))
                    .foregroundStyle(.white)
                    .frame(width: 56, height: 56)
                    .background(accent, in: Circle())
                    .shadow(color: accent.opacity(0.35), radius: 8, y: 4)
            }
            .padding(24)
        }
    }
}

struct NativeClubPage: View {
    var title: String = "Club"
    var onClose: () -> Void
    @State private var filter = 0
    private let filters = ["最新", "嘉宾分享", "资料"]
    private let posts: [(String, String, String, String?)] = [
        ("莫听官方", "06-24", "【官方纪要】本期聚焦 AI 算力与产业趋势，内容仅供合格投资者参考。", "【莫听Club第78期】聊聊AI最靓的仔.pdf"),
        ("策略研究员", "06-20", "当星舰遇到算力：嘉宾分享回顾与延伸阅读。", nil),
    ]
    var body: some View {
        VStack(spacing: 0) {
            navBar(title: title, onClose: onClose, dark: false)
            ScrollView {
                VStack(spacing: 12) {
                    HStack(spacing: 12) {
                        RoundedRectangle(cornerRadius: 12).fill(Color(red: 0.11, green: 0.11, blue: 0.23))
                            .frame(width: 52, height: 52)
                            .overlay(Text("Club", font: .system(size: 13, weight: .bold), color: .white))
                        VStack(alignment: .leading, spacing: 4) {
                            Text("莫听Club", font: .system(size: 17, weight: .semibold), color: DesignTokens.ink)
                            Text("动态 127 | 成员 1040", font: .system(size: 12), color: DesignTokens.body)
                        }
                        Spacer()
                        Text("+ 加入", font: .system(size: 14), color: .white)
                            .padding(.horizontal, 16).padding(.vertical, 8)
                            .background(DesignTokens.link, in: Capsule())
                    }
                    .padding(16)
                    .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 12))

                    HStack(spacing: 0) {
                        ForEach(Array(filters.enumerated()), id: \.offset) { i, f in
                            Text(f, font: .system(size: 14, weight: i == filter ? .semibold : .regular),
                                  color: i == filter ? DesignTokens.link : DesignTokens.body)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 10)
                                .overlay(alignment: .bottom) {
                                    Rectangle().fill(i == filter ? DesignTokens.link : Color.clear).frame(height: 2)
                                }
                                .onTapGesture { filter = i }
                        }
                    }
                    .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 12))

                    ForEach(Array(posts.enumerated()), id: \.offset) { _, p in
                        VStack(alignment: .leading, spacing: 10) {
                            HStack(spacing: 10) {
                                Circle().fill(DesignTokens.canvasSoft2).frame(width: 40, height: 40)
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(p.0, font: .system(size: 15, weight: .semibold), color: DesignTokens.ink)
                                    Text(p.1, font: .system(size: 12), color: DesignTokens.mute)
                                }
                            }
                            Text(p.2, font: .system(size: 15), color: DesignTokens.ink)
                            if let pdf = p.3 {
                                HStack {
                                    Text("📄")
                                    Text(pdf, font: .system(size: 13), color: DesignTokens.body).lineLimit(1)
                                }
                                .padding(12)
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .background(DesignTokens.canvasSoft2, in: RoundedRectangle(cornerRadius: 10))
                            }
                            HStack(spacing: 20) {
                                Text("分享", font: .system(size: 13), color: DesignTokens.body)
                                Text("评论", font: .system(size: 13), color: DesignTokens.body)
                                Text("点赞", font: .system(size: 13), color: DesignTokens.body)
                            }
                        }
                        .padding(16)
                        .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 12))
                    }
                }
                .padding(16)
            }
            .background(DesignTokens.canvasSoft2.ignoresSafeArea())
        }
    }
}

struct NativeLifeServicePage: View {
    var onClose: () -> Void
    private let shortcuts: [(String, Color)] = [
        ("会员专享", DesignTokens.link),
        ("配音专栏", Color.orange),
        ("其他课程", Color(red: 0.345, green: 0.337, blue: 0.839)),
        ("功能教程", Color(red: 0.204, green: 0.780, blue: 0.349)),
    ]
    private let daily: [(String, String, Bool)] = [
        ("带你玩转 ETF", "直播中", true),
        ("新能源赛道解读", "回放", false),
        ("门店短视频运营", "直播中", true),
    ]
    private let courses: [(String, String, Bool)] = [
        ("【配置】当星舰撞上算力", "尤国梁", true),
        ("黄金恐贪定投实战", "策略组", false),
    ]
    var body: some View {
        VStack(spacing: 0) {
            navBar(title: "生活服务", onClose: onClose, dark: false)
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    HStack(spacing: 0) {
                        ForEach(shortcuts, id: \.0) { label, tint in
                            VStack(spacing: 8) {
                                Text(String(label.prefix(1)), font: .system(size: 18, weight: .bold), color: tint)
                                    .frame(width: 52, height: 52)
                                    .background(tint.opacity(0.12), in: RoundedRectangle(cornerRadius: 16))
                                Text(label, font: .system(size: 12), color: DesignTokens.ink).lineLimit(1)
                            }
                            .frame(maxWidth: .infinity)
                        }
                    }
                    .padding(.horizontal, 16).padding(.vertical, 8)

                    sectionHeader("每日推荐")
                    VStack(spacing: 10) {
                        ForEach(Array(daily.enumerated()), id: \.offset) { _, row in
                            HStack(spacing: 10) {
                                Text(row.1, font: .system(size: 11, weight: .semibold),
                                      color: row.2 ? Color.red : DesignTokens.mute)
                                    .padding(.horizontal, 8).padding(.vertical, 3)
                                    .background(row.2 ? Color.red.opacity(0.08) : Color(white: 0.95), in: RoundedRectangle(cornerRadius: 4))
                                Text(row.0, font: .system(size: 15), color: DesignTokens.ink)
                                Spacer()
                            }
                            .padding(12)
                            .background(Color.white, in: RoundedRectangle(cornerRadius: 12))
                            .overlay(RoundedRectangle(cornerRadius: 12).stroke(DesignTokens.hairline, lineWidth: 0.5))
                        }
                    }
                    .padding(.horizontal, 16)

                    sectionHeader("热门课程")
                    VStack(spacing: 10) {
                        ForEach(Array(courses.enumerated()), id: \.offset) { _, row in
                            HStack(spacing: 10) {
                                RoundedRectangle(cornerRadius: 8).fill(DesignTokens.link.opacity(0.1)).frame(width: 56, height: 56)
                                VStack(alignment: .leading, spacing: 4) {
                                    HStack {
                                        Text(row.0, font: .system(size: 14, weight: .semibold), color: DesignTokens.ink).lineLimit(1)
                                        if row.2 {
                                            Text("V 会员专属", font: .system(size: 10, weight: .semibold), color: Color.orange)
                                                .padding(.horizontal, 6).padding(.vertical, 2)
                                                .background(Color.orange.opacity(0.08), in: RoundedRectangle(cornerRadius: 4))
                                        }
                                    }
                                    Text(row.1, font: .system(size: 12), color: DesignTokens.mute)
                                }
                                Spacer()
                            }
                            .padding(12)
                            .background(Color.white, in: RoundedRectangle(cornerRadius: 12))
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.bottom, 24)
                }
            }
            .background(DesignTokens.canvasSoft2.ignoresSafeArea())
        }
    }

    private func sectionHeader(_ title: String) -> some View {
        HStack {
            Text(title, font: .system(size: 17, weight: .semibold), color: DesignTokens.ink)
            Spacer()
            Text("更多 >", font: .system(size: 13), color: DesignTokens.mute)
        }
        .padding(.horizontal, 16).padding(.top, 24).padding(.bottom, 12)
    }
}

struct NativeTodoListPage: View {
    var title: String
    var rows: [(String, String, String)]
    var action: String?
    var onClose: () -> Void
    var body: some View {
        VStack(spacing: 0) {
            navBar(title: title, onClose: onClose, dark: false)
            ScrollView {
                VStack(spacing: 0) {
                    ForEach(Array(rows.enumerated()), id: \.offset) { _, r in
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(r.0, font: .system(size: 16, weight: .semibold), color: DesignTokens.ink)
                                Text(r.1, font: .system(size: 13), color: DesignTokens.body)
                            }
                            Spacer()
                            if !r.2.isEmpty {
                                Text(r.2, font: .system(size: 11, weight: .medium), color: DesignTokens.link)
                                    .padding(.horizontal, 8).padding(.vertical, 4)
                                    .background(DesignTokens.link.opacity(0.12), in: Capsule())
                            }
                        }
                        .padding(16).background(DesignTokens.canvas)
                        Divider().overlay(DesignTokens.hairline)
                    }
                }
                .padding(16)
                if let action {
                    Button(action) {}
                        .buttonStyle(.borderedProminent)
                        .tint(DesignTokens.link)
                        .padding(.bottom, 24)
                }
            }
            .background(DesignTokens.canvasSoft2.ignoresSafeArea())
        }
    }
}


struct NativeWebPage: View {
    var onClose: () -> Void
    var body: some View {
        VStack(spacing: 0) {
            navBar(title: "网页", onClose: onClose, dark: false)
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    Text("离线 fixture", font: .system(size: 20, weight: .semibold), color: DesignTokens.ink)
                    Text("无网络也可显示的固定页面，对齐 Flutter InAppWeb 离线兜底。", font: .system(size: 15), color: DesignTokens.body)
                    Text("myai://web?fixture=offline", font: .system(size: 13), color: DesignTokens.link)
                        .padding(12)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(DesignTokens.canvasSoft2, in: RoundedRectangle(cornerRadius: 8))
                }
                .padding(16)
            }
            .background(DesignTokens.canvas.ignoresSafeArea())
        }
    }
}

struct NativeAfterSalesPage: View {
    var onClose: () -> Void
    var onOpen: ((String) -> Void)? = nil
    private let accent = Color(red: 1, green: 0.584, blue: 0)
    private let deep = Color(red: 0.902, green: 0.494, blue: 0.133)
    /// Align Compose HomeSecondaryMock
    private let appointments: [(String, String)] = [
        ("陈先生 · 保养", "今日 14:00 · 工位 A2"),
        ("周女士 · 钣喷", "今日 16:30 · 工位 B1"),
    ]
    private let records: [(String, String)] = [
        ("工单 AS-441", "保养套餐 · 进行中"),
        ("工单 AS-438", "索赔 · 待配件"),
    ]
    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Button { onClose() } label: { Image(systemName: "chevron.left").foregroundStyle(DesignTokens.ink) }
                Text("售后专区", font: .system(size: 17, weight: .semibold), color: DesignTokens.ink)
                Spacer()
                Button("新建") { onOpen?("/home/after_sales/create") }
                    .font(.system(size: 14, weight: .semibold)).foregroundStyle(accent)
            }
            .padding(.horizontal, 16).padding(.vertical, 12).background(Color.white)

            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    VStack(alignment: .leading, spacing: 0) {
                        HStack(spacing: 12) {
                            Text("修", font: .system(size: 16, weight: .bold), color: .white)
                                .frame(width: 40, height: 40)
                                .background(Color.white.opacity(0.18), in: RoundedRectangle(cornerRadius: 12))
                            VStack(alignment: .leading, spacing: 2) {
                                Text("维修保养档案", font: .system(size: 18, weight: .bold), color: .white)
                                Text("当前店服务记录与预约跟进", font: .system(size: 13), color: .white.opacity(0.85))
                            }
                        }
                        HStack {
                            VStack {
                                Text("\(records.count)", font: .system(size: 22, weight: .bold), color: .white)
                                Text("记录", font: .system(size: 12), color: .white.opacity(0.85))
                            }
                            .frame(maxWidth: .infinity)
                            Rectangle().fill(Color.white.opacity(0.25)).frame(width: 1, height: 28)
                            VStack {
                                Text("\(appointments.count)", font: .system(size: 22, weight: .bold), color: .white)
                                Text("待预约", font: .system(size: 12), color: .white.opacity(0.85))
                            }
                            .frame(maxWidth: .infinity)
                        }
                        .padding(.top, 16)
                    }
                    .padding(EdgeInsets(top: 18, leading: 18, bottom: 16, trailing: 18))
                    .background(
                        LinearGradient(colors: [deep, accent], startPoint: .topLeading, endPoint: .bottomTrailing),
                        in: RoundedRectangle(cornerRadius: 16)
                    )
                    .padding(.horizontal, 16).padding(.top, 12).padding(.bottom, 4)

                    Text("待处理预约", font: .system(size: 16, weight: .semibold), color: DesignTokens.ink)
                        .padding(.horizontal, 20).padding(.top, 8).padding(.bottom, 8)
                    ForEach(Array(appointments.enumerated()), id: \.offset) { _, a in
                        VStack(alignment: .leading, spacing: 4) {
                            Text(a.0, font: .system(size: 15, weight: .medium), color: DesignTokens.ink)
                            Text(a.1, font: .system(size: 13), color: DesignTokens.body)
                        }
                        .padding(14)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(Color.white, in: RoundedRectangle(cornerRadius: 10))
                        .padding(.horizontal, 16).padding(.vertical, 5)
                        .onTapGesture { onOpen?("/home/after_sales/create") }
                    }

                    Text("维修保养记录", font: .system(size: 16, weight: .semibold), color: DesignTokens.ink)
                        .padding(.horizontal, 20).padding(.top, 12).padding(.bottom, 8)
                    ForEach(Array(records.enumerated()), id: \.offset) { _, r in
                        VStack(alignment: .leading, spacing: 4) {
                            Text(r.0, font: .system(size: 15, weight: .medium), color: DesignTokens.ink)
                            Text(r.1, font: .system(size: 13), color: DesignTokens.body)
                        }
                        .padding(14)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(Color.white, in: RoundedRectangle(cornerRadius: 10))
                        .padding(.horizontal, 16).padding(.vertical, 5)
                        .onTapGesture { onOpen?("/home/after_sales/detail") }
                    }
                }
                .padding(.bottom, 24)
            }
            .background(Color(white: 0.96).ignoresSafeArea())
        }
    }
}

struct NativeLedgerPage: View {
    var onClose: () -> Void
    var onOpen: ((String) -> Void)? = nil
    /// Align Compose HomeSecondaryMock.ledger / Flutter LedgerListPage
    private let items: [(type: String, category: String, amount: String, date: String, note: String)] = [
        ("收入", "新车定金", "¥ 5000.00", "2026-09-24", "星越L 意向金"),
        ("支出", "售后配件", "¥ 1280.50", "2026-09-23", "工单 AS-441"),
        ("收入", "二手车过户费", "¥ 1.26 万", "2026-09-22", ""),
    ]
    var body: some View {
        VStack(spacing: 0) {
            navBar(title: "收支", onClose: onClose, dark: false)
            if items.isEmpty {
                Spacer()
                Text("暂无收支记录", font: .system(size: 15), color: DesignTokens.mute)
                Spacer()
            } else {
                ScrollView {
                    VStack(spacing: 12) {
                        ForEach(Array(items.enumerated()), id: \.offset) { _, item in
                            HStack(alignment: .top) {
                                VStack(alignment: .leading, spacing: 0) {
                                    HStack(spacing: 8) {
                                        Text(item.type, font: .system(size: 11, weight: .semibold),
                                              color: item.type == "收入" ? Color(red: 0.18, green: 0.49, blue: 0.20) : Color(red: 0.776, green: 0.157, blue: 0.157))
                                            .padding(.horizontal, 8).padding(.vertical, 3)
                                            .background(
                                                item.type == "收入" ? Color(red: 0.91, green: 0.961, blue: 0.914) : Color(red: 1, green: 0.922, blue: 0.933),
                                                in: RoundedRectangle(cornerRadius: 4)
                                            )
                                        Text(item.category, font: .system(size: 16, weight: .semibold), color: Color(red: 0.102, green: 0.102, blue: 0.102))
                                            .lineLimit(1)
                                    }
                                    Text(item.amount, font: .system(size: 22, weight: .bold), color: Color(red: 0.102, green: 0.102, blue: 0.102))
                                        .padding(.top, 12)
                                    HStack(spacing: 12) {
                                        Text(item.date, font: .system(size: 13), color: Color(white: 0.46))
                                        if !item.note.isEmpty {
                                            Text(item.note, font: .system(size: 13), color: Color(white: 0.62))
                                                .lineLimit(1)
                                        }
                                    }
                                    .padding(.top, 10)
                                }
                                Spacer(minLength: 8)
                                Text("›", font: .system(size: 22), color: Color(white: 0.74))
                            }
                            .padding(16)
                            .background(Color.white, in: RoundedRectangle(cornerRadius: 12))
                            .padding(.horizontal, 16)
                            .onTapGesture { onOpen?("/home/ledger/detail") }
                        }
                    }
                    .padding(.top, 12).padding(.bottom, 24)
                }
            }
        }
        .background(Color(red: 0.961, green: 0.965, blue: 0.973).ignoresSafeArea())
    }
}


struct NativeSmsTemplatePage: View {
    var onClose: () -> Void
    @State private var selected = 0
    /// Align Compose SmsTemplateScreen
    private let templates = [
        ("到店提醒", "尊敬的客户，预约保养已排至今日 14:00，请准时到店。"),
        ("试驾确认", "您好，试驾预约已确认，顾问将提前电话联系您。"),
        ("活动邀约", "本周末门店试驾会，到店即送礼品，欢迎莅临。"),
        ("回访关怀", "购车已满一周，如有用车问题请随时联系您的顾问。"),
    ]
    var body: some View {
        VStack(spacing: 0) {
            navBar(title: "短信模板", onClose: onClose, dark: false)
            ScrollView {
                VStack(spacing: 12) {
                    ForEach(Array(templates.enumerated()), id: \.offset) { i, item in
                        VStack(alignment: .leading, spacing: 8) {
                            HStack {
                                Text(item.0, font: .system(size: 16, weight: .semibold), color: DesignTokens.ink)
                                Spacer()
                                if selected == i {
                                    Text("已选", font: .system(size: 12), color: DesignTokens.link)
                                }
                            }
                            Text(item.1, font: .system(size: 14), color: DesignTokens.body)
                            Button("复制文案") {
                                UIPasteboard.general.string = item.1
                                selected = i
                            }
                            .buttonStyle(.borderedProminent)
                            .tint(DesignTokens.link)
                        }
                        .padding(16)
                        .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 12))
                        .overlay(
                            RoundedRectangle(cornerRadius: 12)
                                .stroke(selected == i ? DesignTokens.link : DesignTokens.hairline, lineWidth: selected == i ? 1.5 : 0.5)
                        )
                        .onTapGesture { selected = i }
                    }
                }
                .padding(16)
            }
            .background(DesignTokens.canvasSoft2.ignoresSafeArea())
        }
    }
}


struct NativeStoreQrPage: View {
    var onClose: () -> Void
    var body: some View {
        VStack(spacing: 0) {
            navBar(title: "店铺收款码", onClose: onClose, dark: false)
            ScrollView {
                VStack(spacing: 16) {
                    VStack(spacing: 8) {
                        Text("演示门店", font: .system(size: 18, weight: .bold), color: DesignTokens.ink)
                        Text("扫码向本店付款", font: .system(size: 13), color: DesignTokens.body)
                        ZStack {
                            RoundedRectangle(cornerRadius: 12).fill(Color(white: 0.07))
                                .frame(width: 200, height: 200)
                            RoundedRectangle(cornerRadius: 8).fill(Color.white)
                                .frame(width: 160, height: 160)
                            Text("QR", font: .system(size: 28, weight: .bold), color: Color(white: 0.07))
                        }
                        .padding(.vertical, 12)
                        Text("支持微信 / 支付宝", font: .system(size: 12), color: DesignTokens.mute)
                    }
                    .padding(24)
                    .frame(maxWidth: .infinity)
                    .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 16))
                    .padding(.horizontal, 24)

                    Button("保存到相册") {}
                        .buttonStyle(.borderedProminent)
                        .tint(DesignTokens.link)
                        .frame(maxWidth: .infinity)
                        .padding(.horizontal, 24)
                }
                .padding(.top, 24)
                .padding(.bottom, 32)
            }
            .background(DesignTokens.canvasSoft2.ignoresSafeArea())
        }
    }
}


struct NativeQaPage: View {
    var onClose: () -> Void
    /// Align Compose BuyQaScreen
    private let qs: [(String, String)] = [
        ("全款和贷款怎么选？", "已解答 · 顾问回复"),
        ("置换能抵多少？", "待回复 · 客户追问"),
        ("保养周期多久一次？", "已解答 · 知识库"),
    ]
    var body: some View {
        VStack(spacing: 0) {
            navBar(title: "选买问答", onClose: onClose, dark: false)
            ScrollView {
                VStack(spacing: 10) {
                    ForEach(Array(qs.enumerated()), id: \.offset) { _, q in
                        VStack(alignment: .leading, spacing: 6) {
                            Text(q.0, font: .system(size: 15, weight: .semibold), color: DesignTokens.ink)
                            Text(q.1, font: .system(size: 13), color: DesignTokens.body)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(14)
                        .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 10))
                    }
                }
                .padding(16)
            }
            .background(DesignTokens.canvasSoft2.ignoresSafeArea())
        }
    }
}


struct NativePosterPage: View {
    var onClose: () -> Void
    /// Align Compose PosterScreen
    private let templates = ["置换专场", "专卖精选", "估价引流", "到店礼"]
    private let columns = [GridItem(.flexible()), GridItem(.flexible())]
    private let previewBg = Color(red: 1, green: 0xF3/255, blue: 0xE0/255)
    private let previewFg = Color(red: 1, green: 0x95/255, blue: 0)

    var body: some View {
        VStack(spacing: 0) {
            navBar(title: "商家海报", onClose: onClose, dark: false)
            ScrollView {
                LazyVGrid(columns: columns, spacing: 12) {
                    ForEach(templates, id: \.self) { name in
                        VStack(spacing: 8) {
                            Text(String(name.prefix(2)), font: .system(size: 18, weight: .bold), color: previewFg)
                                .frame(maxWidth: .infinity)
                                .frame(height: 120)
                                .background(previewBg, in: RoundedRectangle(cornerRadius: 8))
                            Text(name, font: .system(size: 13, weight: .medium), color: DesignTokens.ink)
                        }
                        .padding(12)
                        .background(Color.white, in: RoundedRectangle(cornerRadius: 12))
                    }
                }
                .padding(16)
            }
            .background(Color(white: 0.96).ignoresSafeArea())
        }
    }
}


struct NativeBusinessPage: View {
    var onClose: () -> Void
    @State private var note = ""
    var body: some View {
        VStack(spacing: 0) {
            navBar(title: "商务合作", onClose: onClose, dark: false)
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    Text("渠道合作", font: .system(size: 16, weight: .semibold), color: DesignTokens.ink)
                    Text("提交合作意向后，商务同学会在 1–2 个工作日内联系您。", font: .system(size: 14), color: DesignTokens.body)
                    TextField("请简述合作意向…", text: $note, axis: .vertical)
                        .lineLimit(4...8)
                        .padding(12)
                        .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 10))
                    Button("提交") {}
                        .buttonStyle(.borderedProminent)
                        .tint(DesignTokens.link)
                        .frame(maxWidth: .infinity)
                }
                .padding(16)
            }
            .background(DesignTokens.canvasSoft2.ignoresSafeArea())
        }
    }
}

struct NativeRemindersPage: View {
    var onClose: () -> Void
    private let items: [(String, String)] = [
        ("回访陈先生", "今天 16:00"),
        ("提交周报", "周五 18:00"),
        ("试驾接待 · 周女士", "周六 10:30"),
    ]
    var body: some View {
        VStack(spacing: 0) {
            navBar(title: "提醒事项", onClose: onClose, dark: false)
            ScrollView {
                VStack(spacing: 0) {
                    ForEach(Array(items.enumerated()), id: \.offset) { _, it in
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(it.0, font: .system(size: 16, weight: .semibold), color: DesignTokens.ink)
                                Text(it.1, font: .system(size: 13), color: DesignTokens.body)
                            }
                            Spacer()
                        }
                        .padding(16).background(DesignTokens.canvas)
                        Divider().overlay(DesignTokens.hairline)
                    }
                }
                .padding(16)
                Button("新建提醒") {}
                    .buttonStyle(.borderedProminent)
                    .tint(DesignTokens.link)
                    .padding(.bottom, 24)
            }
            .background(DesignTokens.canvasSoft2.ignoresSafeArea())
        }
    }
}

struct NativeFeedbackPage: View {
    var onClose: () -> Void
    @State private var kind = 0
    @State private var content = ""
    private let kinds = ["功能建议", "体验问题", "其它"]
    var body: some View {
        VStack(spacing: 0) {
            navBar(title: "意见反馈", onClose: onClose, dark: false)
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    Text("反馈类型", font: .system(size: 14, weight: .medium), color: DesignTokens.body)
                    HStack(spacing: 8) {
                        ForEach(Array(kinds.enumerated()), id: \.offset) { i, k in
                            Text(k, font: .system(size: 13), color: kind == i ? .white : DesignTokens.ink)
                                .padding(.horizontal, 12).padding(.vertical, 8)
                                .background(kind == i ? DesignTokens.link : DesignTokens.canvas, in: Capsule())
                                .onTapGesture { kind = i }
                        }
                    }
                    TextField("请描述问题或建议…", text: $content, axis: .vertical)
                        .lineLimit(5...10)
                        .padding(12)
                        .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 10))
                    Button("提交反馈") {}
                        .buttonStyle(.borderedProminent)
                        .tint(DesignTokens.link)
                }
                .padding(16)
            }
            .background(DesignTokens.canvasSoft2.ignoresSafeArea())
        }
    }
}

struct NativeProfilePage: View {
    var onClose: () -> Void
    @State private var nickname = "qa_user"
    @State private var dirty = false
    @State private var toastText: String? = nil
    private let phone = "138****5172"

    var body: some View {
        ZStack(alignment: .bottom) {
            VStack(spacing: 0) {
                HStack {
                    Button { onClose() } label: {
                        Text("‹", font: .system(size: 28), color: DesignTokens.link)
                            .frame(width: 44, alignment: .leading)
                    }
                    Text("个人资料", font: .system(size: 17, weight: .semibold), color: DesignTokens.ink)
                        .frame(maxWidth: .infinity)
                    Button {
                        guard dirty else { return }
                        flash("已保存")
                        dirty = false
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.4) { onClose() }
                    } label: {
                        Text("保存", font: .system(size: 16, weight: .semibold),
                              color: dirty ? DesignTokens.link : DesignTokens.mute)
                    }
                    .disabled(!dirty)
                    .frame(width: 52, alignment: .trailing)
                }
                .padding(.horizontal, 8)
                .frame(height: 44)
                .background(DesignTokens.canvas)

                ScrollView {
                    VStack(spacing: 0) {
                        VStack(spacing: 10) {
                            ZStack(alignment: .bottomTrailing) {
                                Text("Q", font: .system(size: 40, weight: .bold), color: DesignTokens.mute)
                                    .frame(width: 104, height: 104)
                                    .background(Color(red: 0xE0/255, green: 0xE0/255, blue: 0xE0/255), in: Circle())
                                    .onTapGesture { flash("更换头像（开发中）") }
                                Text("📷", font: .system(size: 12))
                                    .frame(width: 32, height: 32)
                                    .background(DesignTokens.link, in: Circle())
                                    .overlay(Circle().stroke(Color.white, lineWidth: 2.5))
                            }
                            Text("轻触更换头像", font: .system(size: 13), color: DesignTokens.mute)
                        }
                        .padding(.top, 24)
                        .padding(.bottom, 32)

                        Text("基本信息", font: .system(size: 12, weight: .medium), color: DesignTokens.mute)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(.horizontal, 20)

                        VStack(spacing: 0) {
                            HStack {
                                Text("昵称", font: .system(size: 15), color: DesignTokens.ink)
                                    .frame(width: 72, alignment: .leading)
                                TextField("请输入昵称", text: $nickname)
                                    .multilineTextAlignment(.trailing)
                                    .font(.system(size: 15))
                                    .onChange(of: nickname) { _, _ in dirty = true }
                            }
                            .padding(.horizontal, 16)
                            .padding(.vertical, 14)
                            Divider().padding(.horizontal, 16)
                            HStack {
                                Text("手机号", font: .system(size: 15), color: DesignTokens.ink)
                                    .frame(width: 72, alignment: .leading)
                                Spacer()
                                Text(phone, font: .system(size: 15), color: DesignTokens.body)
                            }
                            .padding(.horizontal, 16)
                            .padding(.vertical, 14)
                        }
                        .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 12))
                        .padding(.horizontal, 16)
                        .padding(.top, 8)

                        Button {
                            flash("退出登录（开发中）")
                        } label: {
                            Text("退出登录", font: .system(size: 16, weight: .medium), color: Color(red: 0xE5/255, green: 0x39/255, blue: 0x35/255))
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 16)
                                .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 12))
                        }
                        .padding(.horizontal, 16)
                        .padding(.top, 32)
                        .padding(.bottom, 40)
                    }
                }
            }
            .background(DesignTokens.canvasSoft2.ignoresSafeArea())

            if let toastText {
                Text(toastText, font: .system(size: 14), color: .white)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 10)
                    .background(Color.black.opacity(0.78), in: Capsule())
                    .padding(.bottom, 40)
            }
        }
    }

    private func flash(_ text: String) {
        toastText = text
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.6) {
            if toastText == text { toastText = nil }
        }
    }
}

struct NativeAddressesPage: View {
    var onClose: () -> Void
    @State private var list: [(String, String, String, Bool)] = [
        ("qa_user", "138****5172", "北京市朝阳区演示路 1 号", true),
        ("测试乙", "139****0000", "上海市浦东新区世纪大道 100 号", false),
    ]
    @State private var toastText: String? = nil

    var body: some View {
        ZStack(alignment: .bottom) {
            VStack(spacing: 0) {
                HStack {
                    Button { onClose() } label: {
                        Text("‹", font: .system(size: 28), color: DesignTokens.link)
                            .frame(width: 44, alignment: .leading)
                    }
                    Text("收货地址", font: .system(size: 17, weight: .semibold), color: DesignTokens.ink)
                        .frame(maxWidth: .infinity)
                    Button { flash("编辑地址（开发中）") } label: {
                        Text("新增", font: .system(size: 15, weight: .semibold), color: DesignTokens.link)
                    }
                    .frame(width: 52, alignment: .trailing)
                }
                .padding(.horizontal, 8)
                .frame(height: 44)
                .background(DesignTokens.canvas)

                if list.isEmpty {
                    Spacer()
                    VStack(spacing: 12) {
                        Text("暂无地址", font: .system(size: 15), color: DesignTokens.mute)
                        Button { flash("编辑地址（开发中）") } label: {
                            Text("添加收货地址", font: .system(size: 15, weight: .semibold), color: DesignTokens.link)
                        }
                    }
                    Spacer()
                } else {
                    ScrollView {
                        VStack(spacing: 10) {
                            ForEach(Array(list.enumerated()), id: \.offset) { idx, a in
                                VStack(alignment: .leading, spacing: 6) {
                                    HStack(spacing: 8) {
                                        Text("\(a.0)  \(a.1)", font: .system(size: 15, weight: .medium), color: DesignTokens.ink)
                                        if a.3 {
                                            Text("默认", font: .system(size: 11), color: .white)
                                                .padding(.horizontal, 6)
                                                .padding(.vertical, 2)
                                                .background(DesignTokens.link, in: RoundedRectangle(cornerRadius: 4))
                                        }
                                        Spacer()
                                    }
                                    Text(a.2, font: .system(size: 13), color: DesignTokens.body)
                                    HStack(spacing: 16) {
                                        Button {
                                            list = list.enumerated().map { i, row in
                                                (row.0, row.1, row.2, i == idx)
                                            }
                                            flash("已设为默认")
                                        } label: {
                                            Text("设为默认", font: .system(size: 13), color: DesignTokens.link)
                                        }
                                        Button {
                                            list.remove(at: idx)
                                            flash("已删除")
                                        } label: {
                                            Text("删除", font: .system(size: 13), color: Color(red: 0xE5/255, green: 0x39/255, blue: 0x35/255))
                                        }
                                    }
                                    .padding(.top, 4)
                                }
                                .padding(14)
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 12))
                                .onTapGesture { flash("编辑地址（开发中）") }
                            }
                        }
                        .padding(16)
                    }
                }
            }
            .background(DesignTokens.canvasSoft2.ignoresSafeArea())

            if let toastText {
                Text(toastText, font: .system(size: 14), color: .white)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 10)
                    .background(Color.black.opacity(0.78), in: Capsule())
                    .padding(.bottom, 40)
            }
        }
    }

    private func flash(_ text: String) {
        toastText = text
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.6) {
            if toastText == text { toastText = nil }
        }
    }
}

struct NativeDubbingFeedPage: View {
    var onClose: () -> Void
    var onOpen: ((String) -> Void)? = nil
    private let categories = ["推荐", "动画", "电影", "启蒙", "跟读"]
    private let features = ["每日打卡", "影视单词", "经典剧场", "排行榜", "全部视频"]
    private let recent = [("穿梭在迷宫的勇士", "03:24"), ("萌宠部落", "02:18"), ("完美的世界", "04:05"), ("小猪佩奇", "01:56")]
    private let expert = [
        ("英语启蒙课堂", "蓝儿老师Joyue · 跟读练习 · 初级"),
        ("趣味配音挑战", "配音达人 · 动画配音 · 中级"),
        ("诵读之星", "朗读爱好者 · 经典诵读"),
    ]
    var body: some View {
        VStack(spacing: 0) {
            navBar(title: "学英语", onClose: onClose, dark: false)
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 16) {
                    ForEach(Array(categories.enumerated()), id: \.offset) { i, label in
                        Text(label, font: .system(size: 15, weight: i == 0 ? .semibold : .regular),
                              color: i == 0 ? DesignTokens.ink : DesignTokens.mute)
                    }
                }
                .padding(.horizontal, 16).padding(.vertical, 8)
            }
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    RoundedRectangle(cornerRadius: 12)
                        .fill(DesignTokens.link.opacity(0.15))
                        .frame(height: 120)
                        .overlay(alignment: .leading) {
                            VStack(alignment: .leading, spacing: 4) {
                                Text("身体的奥秘", font: .system(size: 18, weight: .bold), color: DesignTokens.ink)
                                Text("Banner · 萌宠部落 / 完美的世界", font: .system(size: 12), color: DesignTokens.mute)
                            }
                            .padding(16)
                        }
                        .padding(.horizontal, 16)

                    HStack(spacing: 0) {
                        ForEach(features, id: \.self) { label in
                            VStack(spacing: 6) {
                                Text(String(label.prefix(1)), font: .system(size: 16, weight: .bold), color: DesignTokens.link)
                                    .frame(width: 44, height: 44)
                                    .background(DesignTokens.link.opacity(0.1), in: RoundedRectangle(cornerRadius: 12))
                                Text(label, font: .system(size: 11), color: DesignTokens.ink).lineLimit(1)
                            }
                            .frame(maxWidth: .infinity)
                            .onTapGesture {
                                if label == "排行榜" { onOpen?("/home/hot_rank_detail") }
                            }
                        }
                    }
                    .padding(.horizontal, 8)

                    sectionTitle("最近在学")
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 10) {
                            ForEach(Array(recent.enumerated()), id: \.offset) { _, it in
                                VStack(alignment: .leading, spacing: 6) {
                                    RoundedRectangle(cornerRadius: 8).fill(Color(red: 0.91, green: 0.94, blue: 0.996)).frame(height: 72)
                                    Text(it.0, font: .system(size: 13, weight: .medium), color: DesignTokens.ink).lineLimit(2)
                                    Text(it.1, font: .system(size: 11), color: DesignTokens.mute)
                                }
                                .frame(width: 120)
                                .padding(10)
                                .background(Color.white, in: RoundedRectangle(cornerRadius: 10))
                            }
                        }
                        .padding(.horizontal, 16)
                    }

                    sectionTitle("新手赛场")
                    VStack(spacing: 8) {
                        ForEach(Array(expert.enumerated()), id: \.offset) { _, it in
                            HStack(spacing: 10) {
                                RoundedRectangle(cornerRadius: 8).fill(Color(red: 1, green: 0.953, blue: 0.878)).frame(width: 48, height: 48)
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(it.0, font: .system(size: 14, weight: .semibold), color: DesignTokens.ink)
                                    Text(it.1, font: .system(size: 12), color: DesignTokens.mute)
                                }
                                Spacer()
                            }
                            .padding(12)
                            .background(Color.white, in: RoundedRectangle(cornerRadius: 10))
                        }
                    }
                    .padding(.horizontal, 16)

                    Text("进入配音视频专区", font: .system(size: 15, weight: .semibold), color: DesignTokens.link)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .background(DesignTokens.link.opacity(0.08), in: RoundedRectangle(cornerRadius: 12))
                        .overlay(RoundedRectangle(cornerRadius: 12).stroke(DesignTokens.link.opacity(0.2)))
                        .padding(.horizontal, 16)
                        .padding(.bottom, 24)
                }
            }
            .background(DesignTokens.canvasSoft2.ignoresSafeArea())
        }
    }

    private func sectionTitle(_ title: String) -> some View {
        HStack {
            Text(title, font: .system(size: 17, weight: .semibold), color: DesignTokens.ink)
            Spacer()
            Text("更多 >", font: .system(size: 13), color: DesignTokens.mute)
        }
        .padding(.horizontal, 16).padding(.top, 12)
    }
}

struct NativeHotRankPage: View {
    var onClose: () -> Void
    @State private var category = 3 // 热搜榜 — Flutter HotRankCategory.hotSearch
    @State private var age = 2 // 1-3年级
    @State private var showAgeMenu = false
    private let categories = ["热读榜", "新书榜", "童话榜", "热搜榜", "科普榜", "高分榜"]
    private let ages = ["1-2岁", "3岁到大班", "1-3年级", "4年级以上"]
    private let titles = ["穿条纹睡衣的...", "蛮荒故事", "爱冒险的朵拉", "道奇小狗", "你好，小朋友", "完美的世界", "萌宠部落", "穿梭在迷宫的勇士"]
    private let subs = ["某日布鲁诺决定...", "一种近似父子的不寻常感情", "开启你的奇幻冒险之旅", "跟佩奇一起快乐学英语", "经典动画配音练习", "趣味英语启蒙课堂"]
    private let heats = [39274, 28390, 22007, 19874, 18560, 16230, 14890, 13540]
    private let ranks = [1, 2, 3, 88, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20]
    var body: some View {
        VStack(spacing: 0) {
            VStack(spacing: 6) {
                HStack {
                    Button(action: onClose) { Text("‹", font: .system(size: 28), color: DesignTokens.ink) }
                    Spacer()
                    Text("分享", font: .system(size: 14), color: DesignTokens.ink)
                }
                .padding(.horizontal, 16)
                HStack(spacing: 8) {
                    Text("🌾")
                    Text(categories[category], font: .system(size: 22, weight: .bold), color: DesignTokens.ink)
                    Text("🌾")
                }
                Text("iHome用户近期热搜内容", font: .system(size: 12), color: DesignTokens.mute)
            }
            .padding(.vertical, 12)
            .frame(maxWidth: .infinity)
            .background(
                LinearGradient(colors: [DesignTokens.canvasSoft2, DesignTokens.canvas], startPoint: .top, endPoint: .bottom)
            )
            HStack(alignment: .top, spacing: 0) {
                ScrollView {
                    VStack(spacing: 2) {
                        ForEach(Array(categories.enumerated()), id: \.offset) { i, label in
                            Text(label, font: .system(size: 13, weight: i == category ? .semibold : .regular),
                                  color: i == category ? DesignTokens.ink : DesignTokens.mute)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 14)
                                .background(i == category ? DesignTokens.canvas : Color.clear)
                                .onTapGesture { category = i }
                        }
                    }
                    .padding(.top, 8)
                }
                .frame(width: 88)
                .background(DesignTokens.canvasSoft2)
                VStack(alignment: .trailing, spacing: 0) {
                    HStack {
                        Spacer()
                        Text(ages[age] + " ▾", font: .system(size: 13), color: DesignTokens.ink)
                            .padding(.horizontal, 10).padding(.vertical, 6)
                            .background(DesignTokens.canvas, in: Capsule())
                            .overlay(Capsule().stroke(DesignTokens.hairline, lineWidth: 0.5))
                            .onTapGesture { showAgeMenu.toggle() }
                    }
                    .padding(.horizontal, 12).padding(.top, 10)
                    if showAgeMenu {
                        VStack(alignment: .leading, spacing: 0) {
                            ForEach(Array(ages.enumerated()), id: \.offset) { i, label in
                                Text(label, font: .system(size: 13, weight: i == age ? .semibold : .regular),
                                      color: i == age ? DesignTokens.link : DesignTokens.ink)
                                    .padding(.horizontal, 14).padding(.vertical, 10)
                                    .onTapGesture { age = i; showAgeMenu = false }
                            }
                        }
                        .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 10))
                        .overlay(RoundedRectangle(cornerRadius: 10).stroke(DesignTokens.hairline, lineWidth: 0.5))
                        .padding(.trailing, 12)
                    }
                    ScrollView {
                        VStack(spacing: 0) {
                            ForEach(0..<20, id: \.self) { i in
                                HStack(alignment: .top, spacing: 8) {
                                    RoundedRectangle(cornerRadius: 8).fill(Color(red: 0.91, green: 0.94, blue: 0.99))
                                        .frame(width: 56, height: 56)
                                    Text("\(ranks[i])", font: .system(size: 11, weight: .bold),
                                          color: ranks[i] <= 3 ? .white : DesignTokens.mute)
                                        .frame(width: 18, height: 18)
                                        .background(
                                            ranks[i] == 1 ? Color.orange :
                                            ranks[i] == 2 ? Color.gray :
                                            ranks[i] == 3 ? Color(red: 0.79, green: 0.47, blue: 0.23) :
                                            DesignTokens.mute.opacity(0.35),
                                            in: RoundedRectangle(cornerRadius: 4)
                                        )
                                    VStack(alignment: .leading, spacing: 4) {
                                        Text(titles[i % titles.count], font: .system(size: 14, weight: .semibold), color: DesignTokens.ink)
                                            .lineLimit(1)
                                        Text(subs[i % subs.count], font: .system(size: 12), color: DesignTokens.mute)
                                            .lineLimit(1)
                                        Text("热度\(heats[i % heats.count])", font: .system(size: 11), color: DesignTokens.mute)
                                    }
                                    Spacer(minLength: 0)
                                }
                                .padding(.horizontal, 12).padding(.vertical, 10)
                                Divider().overlay(DesignTokens.hairline).padding(.horizontal, 12)
                            }
                        }
                    }
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
        .background(DesignTokens.canvasSoft2.ignoresSafeArea())
    }
}

struct NativeDataAnalyticsPage: View {
    var onClose: () -> Void
    var onOpen: ((String) -> Void)? = nil
    private let primary = Color(red: 0, green: 0.439, blue: 0.953)
    /// Align Compose HomeSecondaryMock.analyticsRecords
    private let items: [(title: String, subtitle: String, pv: Int, clicks: Int, converts: Int, featured: Bool, anomaly: Bool)] = [
        ("本周线索转化", "门店线索漏斗 · 高意向优先", 12840, 962, 119, true, false),
        ("试驾到店", "预约试驾 → 到店完成", 4520, 610, 86, false, false),
        ("直播线索异常", "点击骤降 · 需排查投放", 2100, 42, 3, false, true),
    ]
    var body: some View {
        VStack(spacing: 0) {
            navBar(title: "数据分析", onClose: onClose, dark: false)
            HStack {
                Text("已加载 \(items.count) / 共 \(items.count) · 第 1 页", font: .system(size: 13), color: DesignTokens.mute)
                Spacer()
                Text("gRPC", font: .system(size: 11, weight: .bold), color: primary)
                    .padding(.horizontal, 8).padding(.vertical, 4)
                    .background(primary.opacity(0.08), in: Capsule())
            }
            .padding(.horizontal, 14).padding(.vertical, 10)
            .background(Color.white, in: RoundedRectangle(cornerRadius: 12))
            .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color(white: 0.918), lineWidth: 1))
            .padding(.horizontal, 12).padding(.top, 12)

            ScrollView {
                VStack(spacing: 8) {
                    ForEach(Array(items.enumerated()), id: \.offset) { _, row in
                        let rate = row.clicks > 0 ? Double(row.converts) * 100.0 / Double(row.clicks) : 0
                        let cue = row.anomaly ? Color.red : (row.featured ? Color.orange : Color.clear)
                        HStack(spacing: 0) {
                            Rectangle().fill(cue).frame(width: 4)
                            VStack(alignment: .leading, spacing: 4) {
                                Text(row.title, font: .system(size: 15, weight: .semibold), color: DesignTokens.ink)
                                Text(row.subtitle, font: .system(size: 12), color: DesignTokens.mute)
                                HStack {
                                    Text("PV \(row.pv)", font: .system(size: 12), color: DesignTokens.mute)
                                    Text("点击 \(row.clicks)", font: .system(size: 12), color: DesignTokens.mute)
                                    Text("转化 \(row.converts)", font: .system(size: 12), color: DesignTokens.mute)
                                    Spacer()
                                    Text(String(format: "%.1f%%", rate), font: .system(size: 13, weight: .bold), color: primary)
                                }
                                .padding(.top, 6)
                            }
                            .padding(14)
                        }
                        .background(Color.white, in: RoundedRectangle(cornerRadius: 8))
                        .onTapGesture { onOpen?("/home/data_analytics/detail") }
                    }
                }
                .padding(12)
            }
            .background(Color(white: 0.96).ignoresSafeArea())
        }
    }
}

struct NativeBusinessCardPage: View {
    var onClose: () -> Void
    var body: some View {
        VStack(spacing: 0) {
            navBar(title: "电子名片", onClose: onClose, dark: false)
            VStack(spacing: 16) {
                RoundedRectangle(cornerRadius: 16)
                    .fill(
                        LinearGradient(colors: [DesignTokens.link, Color(red: 0.2, green: 0.35, blue: 0.85)],
                                       startPoint: .topLeading, endPoint: .bottomTrailing)
                    )
                    .frame(height: 180)
                    .overlay(
                        VStack(alignment: .leading, spacing: 8) {
                            Text("qa_user", font: .system(size: 22, weight: .bold), color: .white)
                            Text("销售顾问 · 沃德龙鼎", font: .system(size: 14), color: .white.opacity(0.9))
                            Text("138****5172", font: .system(size: 14), color: .white.opacity(0.85))
                            Spacer()
                            Text("扫码加好友 / 预约到店", font: .system(size: 12), color: .white.opacity(0.75))
                        }
                        .padding(20)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    )
                Button("分享名片") {}
                    .buttonStyle(.borderedProminent)
                    .tint(DesignTokens.link)
                Spacer()
            }
            .padding(16)
            .background(DesignTokens.canvasSoft2.ignoresSafeArea())
        }
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

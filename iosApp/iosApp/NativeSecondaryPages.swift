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

struct NativeCommunityPublishPage: View {
    var onClose: () -> Void
    @State private var content = ""
    @State private var mediaType = "none"
    @State private var topic = ""
    @State private var showConvention = true
    @State private var publishing = false

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
                    Button("我知道了") { showConvention = false }
                        .buttonStyle(.borderedProminent)
                        .tint(DesignTokens.link)
                }
                .padding(24)
                .frame(maxWidth: 320)
                .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 12))
            }
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
    private let flows = [
        ("membership_pay", "ref m2", "-30.00"),
        ("充值", "ref alipay", "+100.00"),
        ("充值", "ref alipay", "+1.00"),
    ]

    var body: some View {
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
                    .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 12))

                    Text("充值", font: .system(size: 16, weight: .bold), color: DesignTokens.ink)
                    TextField("金额 0.01-50000", text: $amount)
                        .keyboardType(.decimalPad)
                        .padding(12)
                        .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 8))
                        .overlay(RoundedRectangle(cornerRadius: 8).stroke(DesignTokens.hairline, lineWidth: 1))

                    HStack(spacing: 8) {
                        ForEach([(1, "支付宝"), (2, "微信"), (3, "银行卡")], id: \.0) { item in
                            let sel = channel == item.0
                            Text((sel ? "✓ " : "") + item.1, font: .system(size: 13), color: sel ? .white : DesignTokens.ink)
                                .padding(.horizontal, 12)
                                .padding(.vertical, 8)
                                .background(sel ? DesignTokens.link : DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 8))
                                .overlay(RoundedRectangle(cornerRadius: 8).stroke(sel ? DesignTokens.link : DesignTokens.hairline, lineWidth: 1))
                                .onTapGesture { channel = item.0 }
                        }
                        ForEach(["10", "50", "100"], id: \.self) { a in
                            Text(a, font: .system(size: 13), color: DesignTokens.ink)
                                .padding(.horizontal, 16)
                                .padding(.vertical, 8)
                                .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 8))
                                .overlay(RoundedRectangle(cornerRadius: 8).stroke(DesignTokens.hairline, lineWidth: 1))
                                .onTapGesture { amount = a }
                        }
                    }

                    Button("立即充值") {}
                        .buttonStyle(.borderedProminent)
                        .tint(DesignTokens.link)
                        .frame(maxWidth: .infinity)

                    Text("流水", font: .system(size: 16, weight: .bold), color: DesignTokens.ink)
                    ForEach(Array(flows.enumerated()), id: \.offset) { _, f in
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(f.0, font: .system(size: 15, weight: .medium), color: DesignTokens.ink)
                                Text(f.1, font: .system(size: 12), color: DesignTokens.mute)
                            }
                            Spacer()
                            Text(f.2, font: .system(size: 15, weight: .semibold),
                                  color: f.2.hasPrefix("+") ? Color(red: 0.1, green: 0.6, blue: 0.3) : DesignTokens.ink)
                        }
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
    private let rooms: [(String, String, String, String)] = [
        ("1", "沃德龙鼎直播间", "在线 326 · 讲解新车", "直播中"),
        ("2", "售后讲堂", "预约 88 · 明天 19:00", "预约"),
        ("3", "二手车清库", "回放 · 观看 8.6k", "回放"),
    ]

    var body: some View {
        if let id = roomId, let room = rooms.first(where: { $0.0 == id }) {
            ZStack {
                Color.black.ignoresSafeArea()
                VStack {
                    Spacer()
                    Text(room.1, font: .system(size: 20, weight: .semibold), color: .white)
                    Text("直播画面（mock）", font: .system(size: 14), color: .white.opacity(0.7))
                        .padding(.top, 8)
                    Spacer()
                    Text("进入直播间 · 推流通道见 gap registry", font: .system(size: 12), color: .white.opacity(0.5))
                        .padding(.bottom, 32)
                }
                VStack {
                    HStack {
                        Button {
                            roomId = nil
                        } label: {
                            Image(systemName: "chevron.left").foregroundStyle(.white)
                        }
                        Text(room.1, font: .system(size: 17, weight: .semibold), color: .white)
                        Spacer()
                        Text(room.3, font: .system(size: 12, weight: .medium), color: .white)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(Color.red.opacity(0.8), in: Capsule())
                    }
                    .padding(16)
                    Spacer()
                }
            }
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
                            HStack {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(room.1, font: .system(size: 16, weight: .semibold), color: DesignTokens.ink)
                                    Text(room.2, font: .system(size: 13), color: DesignTokens.body)
                                }
                                Spacer()
                                Text(room.3, font: .system(size: 11, weight: .medium), color: DesignTokens.link)
                                    .padding(.horizontal, 8)
                                    .padding(.vertical, 4)
                                    .background(DesignTokens.link.opacity(0.12), in: Capsule())
                            }
                            .padding(16)
                            .background(DesignTokens.canvas)
                            .onTapGesture { roomId = room.0 }
                            Divider().overlay(DesignTokens.hairline)
                        }
                    }
                    .padding(16)
                }
                .background(DesignTokens.canvasSoft2.ignoresSafeArea())
            }
        }
    }
}

struct NativeAiStreamPage: View {
    var onClose: () -> Void
    @State private var input = ""
    @State private var streaming = false
    @State private var bubbles: [(String, String)] = [
        ("assistant", "你好，我是小石头。有什么想问的？"),
    ]
    private let chips = ["今日学习建议", "语法纠错", "口语话题"]

    var body: some View {
        VStack(spacing: 0) {
            navBar(title: "AI 小石头", onClose: onClose, dark: false)
            ScrollViewReader { proxy in
                ScrollView {
                    LazyVStack(alignment: .leading, spacing: 12) {
                        ForEach(Array(bubbles.enumerated()), id: \.offset) { i, b in
                            HStack {
                                if b.0 == "user" { Spacer(minLength: 48) }
                                Text(b.1, font: .system(size: 15), color: b.0 == "user" ? .white : DesignTokens.ink)
                                    .padding(12)
                                    .background(
                                        b.0 == "user" ? DesignTokens.link : DesignTokens.canvas,
                                        in: RoundedRectangle(cornerRadius: 12)
                                    )
                                if b.0 == "assistant" { Spacer(minLength: 48) }
                            }
                            .id(i)
                        }
                    }
                    .padding(16)
                }
                .onChange(of: bubbles.count) { _, _ in
                    if let last = bubbles.indices.last {
                        withAnimation { proxy.scrollTo(last, anchor: .bottom) }
                    }
                }
            }
            .background(DesignTokens.canvasSoft2)

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(chips, id: \.self) { chip in
                        Text(chip, font: .system(size: 13), color: DesignTokens.link)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 8)
                            .background(DesignTokens.link.opacity(0.1), in: Capsule())
                            .onTapGesture { send(chip) }
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
            }

            HStack(spacing: 10) {
                TextField("问问小石头…", text: $input)
                    .padding(.horizontal, 12)
                    .frame(height: 40)
                    .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 20))
                if streaming {
                    Button("停止") { streaming = false }
                        .foregroundStyle(DesignTokens.link)
                } else {
                    Button("发送") { send(input) }
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(.white)
                        .padding(.horizontal, 14)
                        .padding(.vertical, 10)
                        .background(DesignTokens.link, in: Capsule())
                }
            }
            .padding(12)
            .background(DesignTokens.canvas)
        }
    }

    private func send(_ prompt: String) {
        let q = prompt.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !q.isEmpty, !streaming else { return }
        bubbles.append(("user", q))
        input = ""
        streaming = true
        let reply = "关于「\(q)」：建议每天跟读 15 分钟，并记录生词。（mock 流）"
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.35) {
            bubbles.append(("assistant", reply))
            streaming = false
        }
    }
}

struct NativeScanPage: View {
    var onClose: () -> Void
    @State private var result: String? = nil

    var body: some View {
        VStack(spacing: 0) {
            navBar(title: "扫一扫", onClose: onClose, dark: false)
            if let result {
                VStack(alignment: .leading, spacing: 12) {
                    Text("扫码结果", font: .system(size: 15, weight: .semibold), color: DesignTokens.ink)
                    Text(result, font: .system(size: 15), color: DesignTokens.link)
                    Button("继续扫码") { self.result = nil }
                        .buttonStyle(.borderedProminent)
                        .tint(DesignTokens.link)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(16)
                Spacer()
            } else {
                ZStack {
                    Color.black.ignoresSafeArea(edges: .bottom)
                    VStack(spacing: 24) {
                        RoundedRectangle(cornerRadius: 12)
                            .stroke(DesignTokens.link, lineWidth: 2)
                            .frame(width: 220, height: 220)
                            .overlay(
                                Text("将二维码放入框内", font: .system(size: 13), color: .white.opacity(0.8))
                            )
                        Text("用于门店收款码 / 活动核销", font: .system(size: 13), color: .white.opacity(0.6))
                        Button("模拟扫码成功") {
                            result = "myai://mall/orders?id=A1024"
                        }
                        .buttonStyle(.borderedProminent)
                        .tint(DesignTokens.link)
                    }
                }
            }
        }
        .background(DesignTokens.canvasSoft2.ignoresSafeArea())
    }
}


struct NativeStrategyPage: View {
    var onClose: () -> Void
    private let scripts: [(String, String, String?)] = [
        ("周末到店礼", "适合朋友圈 · 已用 128 次", "推荐"),
        ("置换补贴海报", "适合群发 · 已用 56 次", nil),
        ("新车到店速递", "适合视频号 · 已用 34 次", nil),
    ]
    var body: some View {
        VStack(spacing: 0) {
            navBar(title: "投资策略", onClose: onClose, dark: false)
            ScrollView {
                VStack(spacing: 12) {
                    ForEach(Array(scripts.enumerated()), id: \.offset) { _, s in
                        VStack(alignment: .leading, spacing: 8) {
                            HStack {
                                Text(s.0, font: .system(size: 16, weight: .semibold), color: DesignTokens.ink)
                                Spacer()
                                if let badge = s.2 {
                                    Text(badge, font: .system(size: 11, weight: .medium), color: DesignTokens.link)
                                        .padding(.horizontal, 8).padding(.vertical, 4)
                                        .background(DesignTokens.link.opacity(0.12), in: Capsule())
                                }
                            }
                            Text(s.1, font: .system(size: 13), color: DesignTokens.body)
                            Text("周末到店看新车，置换补贴进行中，欢迎预约试驾～", font: .system(size: 14), color: DesignTokens.ink)
                                .padding(12)
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .background(DesignTokens.canvasSoft2, in: RoundedRectangle(cornerRadius: 8))
                            Button("一键发圈") {}
                                .buttonStyle(.borderedProminent)
                                .tint(DesignTokens.link)
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

struct NativeCheckInMallPage: View {
    var onClose: () -> Void
    @State private var points = 0
    @State private var checkedToday = false
    @State private var streak = 1
    private let days = ["19", "20", "21", "22", "23", "24", "今天"]
    private let gifts: [(String, String)] = [("洗车券 ×1", "200 积分"), ("香氛挂件", "500 积分"), ("定制马克杯", "800 积分")]

    var body: some View {
        VStack(spacing: 0) {
            VStack(spacing: 12) {
                HStack {
                    Button { onClose() } label: {
                        Image(systemName: "chevron.left").foregroundStyle(.white)
                    }
                    Text("签到商城", font: .system(size: 17, weight: .semibold), color: .white)
                    Spacer()
                }
                HStack {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("我的积分", font: .system(size: 13), color: .white.opacity(0.85))
                        Text("\(points)", font: .system(size: 32, weight: .bold), color: .white)
                    }
                    Spacer()
                    Button(checkedToday ? "已签到" : "立即签到") {
                        guard !checkedToday else { return }
                        checkedToday = true
                        points += 10
                        streak += 1
                    }
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(Color(red: 0.18, green: 0.42, blue: 1))
                    .padding(.horizontal, 16).padding(.vertical, 10)
                    .background(Color.white, in: Capsule())
                }
                HStack {
                    ForEach(Array(days.enumerated()), id: \.offset) { i, d in
                        VStack(spacing: 6) {
                            Circle()
                                .fill(i < streak || (i == days.count - 1 && checkedToday) ? Color.white : Color.white.opacity(0.25))
                                .frame(width: 28, height: 28)
                            Text(d, font: .system(size: 11), color: .white.opacity(0.9))
                        }
                        .frame(maxWidth: .infinity)
                    }
                }
            }
            .padding(16)
            .background(Color(red: 0.18, green: 0.42, blue: 1).ignoresSafeArea(edges: .top))

            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    Text("积分换礼", font: .system(size: 16, weight: .bold), color: DesignTokens.ink)
                    ForEach(Array(gifts.enumerated()), id: \.offset) { _, g in
                        HStack {
                            RoundedRectangle(cornerRadius: 8).fill(DesignTokens.canvasSoft2).frame(width: 56, height: 56)
                            VStack(alignment: .leading, spacing: 4) {
                                Text(g.0, font: .system(size: 15, weight: .medium), color: DesignTokens.ink)
                                Text(g.1, font: .system(size: 13), color: DesignTokens.body)
                            }
                            Spacer()
                            Text("兑换", font: .system(size: 13, weight: .semibold), color: .white)
                                .padding(.horizontal, 12).padding(.vertical, 8)
                                .background(DesignTokens.link, in: Capsule())
                        }
                        .padding(12)
                        .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 12))
                    }
                }
                .padding(16)
            }
            .background(DesignTokens.canvasSoft2.ignoresSafeArea())
        }
    }
}

struct NativeMusicPage: View {
    var onClose: () -> Void
    @State private var playing: String? = nil
    private let tracks = [("Night Drive", "3:28"), ("Showroom BGM", "2:51"), ("Weekend Cruise", "4:02")]
    var body: some View {
        VStack(spacing: 0) {
            navBar(title: "音乐", onClose: onClose, dark: false)
            ScrollView {
                VStack(spacing: 0) {
                    ForEach(Array(tracks.enumerated()), id: \.offset) { _, t in
                        HStack {
                            Image(systemName: playing == t.0 ? "pause.circle.fill" : "play.circle.fill")
                                .font(.system(size: 28))
                                .foregroundStyle(DesignTokens.link)
                            VStack(alignment: .leading, spacing: 4) {
                                Text(t.0, font: .system(size: 16, weight: .medium), color: DesignTokens.ink)
                                Text(t.1, font: .system(size: 12), color: DesignTokens.mute)
                            }
                            Spacer()
                            if playing == t.0 {
                                Text("播放中", font: .system(size: 12), color: DesignTokens.link)
                            }
                        }
                        .padding(16)
                        .background(DesignTokens.canvas)
                        .onTapGesture { playing = playing == t.0 ? nil : t.0 }
                        Divider().overlay(DesignTokens.hairline)
                    }
                }
                .padding(16)
            }
            .background(DesignTokens.canvasSoft2.ignoresSafeArea())
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
                        .onTapGesture { onOpen?("/classroom/homework") }
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
    private let cars: [(String, String, String)] = [
        ("2019 凯美瑞 双擎", "12.8 万 · 4.2 万公里", "急售"),
        ("2021 汉兰达", "22.5 万 · 3.1 万公里", ""),
        ("2018 雷凌", "7.9 万 · 6.8 万公里", "新上"),
    ]
    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Button { onClose() } label: { Image(systemName: "chevron.left").foregroundStyle(DesignTokens.link) }
                Text("二手车", font: .system(size: 17, weight: .semibold), color: DesignTokens.ink)
                Spacer()
                Button("发布") { onOpen?("/home/used_car/create") }
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(DesignTokens.link)
            }
            .padding(.horizontal, 16).padding(.vertical, 12)
            .background(DesignTokens.canvas)
            ScrollView {
                VStack(spacing: 12) {
                    ForEach(Array(cars.enumerated()), id: \.offset) { _, c in
                        HStack(spacing: 12) {
                            RoundedRectangle(cornerRadius: 8).fill(DesignTokens.canvasSoft2).frame(width: 96, height: 72)
                            VStack(alignment: .leading, spacing: 4) {
                                Text(c.0, font: .system(size: 15, weight: .semibold), color: DesignTokens.ink)
                                Text(c.1, font: .system(size: 13), color: DesignTokens.body)
                                if !c.2.isEmpty {
                                    Text(c.2, font: .system(size: 11, weight: .medium), color: DesignTokens.link)
                                }
                            }
                            Spacer()
                        }
                        .padding(12)
                        .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 12))
                        .onTapGesture { onOpen?("/home/used_car/detail") }
                    }
                }
                .padding(16)
            }
            .background(DesignTokens.canvasSoft2.ignoresSafeArea())
        }
    }
}

struct NativeNewCarFollowPage: View {
    var onClose: () -> Void
    var onOpen: ((String) -> Void)? = nil
    private let rows: [(String, String, String)] = [
        ("陈先生 · 凯美瑞", "意向强 · 未回访 2 天", "紧急"),
        ("周女士 · 汉兰达", "询价 · 今早留言", "跟进"),
        ("刘总 · 塞那", "试驾预约 · 周六", "预约"),
    ]
    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Button { onClose() } label: { Image(systemName: "chevron.left").foregroundStyle(DesignTokens.link) }
                Text("新车跟进", font: .system(size: 17, weight: .semibold), color: DesignTokens.ink)
                Spacer()
                Button("新建") { onOpen?("/home/new_car_follow/create") }
                    .font(.system(size: 14, weight: .semibold)).foregroundStyle(DesignTokens.link)
            }
            .padding(.horizontal, 16).padding(.vertical, 12).background(DesignTokens.canvas)
            ScrollView {
                VStack(spacing: 0) {
                    ForEach(Array(rows.enumerated()), id: \.offset) { _, r in
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(r.0, font: .system(size: 16, weight: .semibold), color: DesignTokens.ink)
                                Text(r.1, font: .system(size: 13), color: DesignTokens.body)
                            }
                            Spacer()
                            Text(r.2, font: .system(size: 11, weight: .medium), color: DesignTokens.link)
                                .padding(.horizontal, 8).padding(.vertical, 4)
                                .background(DesignTokens.link.opacity(0.12), in: Capsule())
                        }
                        .padding(16).background(DesignTokens.canvas)
                        .onTapGesture { onOpen?("/home/new_car_follow/detail") }
                        Divider().overlay(DesignTokens.hairline)
                    }
                }
                .padding(16)
            }
            .background(DesignTokens.canvasSoft2.ignoresSafeArea())
        }
    }
}

struct NativeClubPage: View {
    var onClose: () -> Void
    @State private var filter = 0
    private let filters = ["最新", "嘉宾分享", "资料"]
    private let posts: [(String, String, String, String?)] = [
        ("莫听官方", "06-24", "【官方纪要】本期聚焦 AI 算力与产业趋势，内容仅供合格投资者参考。", "【莫听Club第78期】聊聊AI最靓的仔.pdf"),
        ("策略研究员", "06-20", "当星舰遇到算力：嘉宾分享回顾与延伸阅读。", nil),
    ]
    var body: some View {
        VStack(spacing: 0) {
            navBar(title: "Club", onClose: onClose, dark: false)
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
    private let items = ["代驾", "洗车", "道路救援", "充电桩", "年检代办", "保险续保"]
    var body: some View {
        VStack(spacing: 0) {
            navBar(title: "生活服务", onClose: onClose, dark: false)
            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                ForEach(items, id: \.self) { item in
                    VStack(spacing: 8) {
                        Circle().fill(DesignTokens.link.opacity(0.12)).frame(width: 48, height: 48)
                            .overlay(Text(String(item.prefix(1)), font: .system(size: 16, weight: .semibold), color: DesignTokens.link))
                        Text(item, font: .system(size: 13), color: DesignTokens.ink)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 16)
                    .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 12))
                }
            }
            .padding(16)
            Spacer()
        }
        .background(DesignTokens.canvasSoft2.ignoresSafeArea())
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
    private let rows: [(String, String, String)] = [
        ("京 A·12345 · 小保养", "完成 · 2026-09-20", "完成"),
        ("京 C·54321 · 四轮定位", "进行中", "进行中"),
        ("京 B·99887 · 钣喷", "待进厂 · 明天 09:00", "预约"),
    ]
    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Button { onClose() } label: { Image(systemName: "chevron.left").foregroundStyle(DesignTokens.link) }
                Text("售后专区", font: .system(size: 17, weight: .semibold), color: DesignTokens.ink)
                Spacer()
                Button("新建工单") { onOpen?("/home/after_sales/create") }
                    .font(.system(size: 14, weight: .semibold)).foregroundStyle(DesignTokens.link)
            }
            .padding(.horizontal, 16).padding(.vertical, 12).background(DesignTokens.canvas)
            ScrollView {
                VStack(spacing: 0) {
                    ForEach(Array(rows.enumerated()), id: \.offset) { _, r in
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(r.0, font: .system(size: 16, weight: .semibold), color: DesignTokens.ink)
                                Text(r.1, font: .system(size: 13), color: DesignTokens.body)
                            }
                            Spacer()
                            Text(r.2, font: .system(size: 11, weight: .medium), color: DesignTokens.link)
                                .padding(.horizontal, 8).padding(.vertical, 4)
                                .background(DesignTokens.link.opacity(0.12), in: Capsule())
                        }
                        .padding(16).background(DesignTokens.canvas)
                        .onTapGesture { onOpen?("/home/after_sales/detail") }
                        Divider().overlay(DesignTokens.hairline)
                    }
                }
                .padding(16)
            }
            .background(DesignTokens.canvasSoft2.ignoresSafeArea())
        }
    }
}

struct NativeLedgerPage: View {
    var onClose: () -> Void
    private let metrics: [(String, String)] = [
        ("9 月销售额", "¥ 2,860,000"),
        ("毛利率", "18.6%"),
        ("库存周转", "42 天"),
        ("售后产值", "¥ 486,000"),
    ]
    var body: some View {
        VStack(spacing: 0) {
            navBar(title: "台账", onClose: onClose, dark: false)
            ScrollView {
                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                    ForEach(Array(metrics.enumerated()), id: \.offset) { _, m in
                        VStack(alignment: .leading, spacing: 8) {
                            Text(m.0, font: .system(size: 13), color: DesignTokens.body)
                            Text(m.1, font: .system(size: 20, weight: .semibold), color: DesignTokens.ink)
                        }
                        .padding(16)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 12))
                    }
                }
                .padding(16)
            }
            .background(DesignTokens.canvasSoft2.ignoresSafeArea())
        }
    }
}


struct NativeSmsTemplatePage: View {
    var onClose: () -> Void
    @State private var selected = 0
    private let templates = [
        ("到店提醒", "您好，您预约的试驾已确认，请准时到店。"),
        ("保养到期", "爱车即将到保养周期，回店可享工时折扣。"),
        ("交车祝福", "恭喜提车！如有用车问题随时联系专属顾问。"),
    ]
    var body: some View {
        VStack(spacing: 0) {
            navBar(title: "短信模板", onClose: onClose, dark: false)
            ScrollView {
                VStack(spacing: 12) {
                    ForEach(Array(templates.enumerated()), id: \.offset) { i, t in
                        VStack(alignment: .leading, spacing: 8) {
                            HStack {
                                Text(t.0, font: .system(size: 16, weight: .semibold), color: DesignTokens.ink)
                                Spacer()
                                if selected == i {
                                    Image(systemName: "checkmark.circle.fill").foregroundStyle(DesignTokens.link)
                                }
                            }
                            Text(t.1, font: .system(size: 14), color: DesignTokens.body)
                        }
                        .padding(16)
                        .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 12))
                        .overlay(RoundedRectangle(cornerRadius: 12).stroke(selected == i ? DesignTokens.link : DesignTokens.hairline, lineWidth: 1))
                        .onTapGesture { selected = i }
                    }
                    Button("发送") {}
                        .buttonStyle(.borderedProminent)
                        .tint(DesignTokens.link)
                        .padding(.top, 8)
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
            VStack(spacing: 16) {
                RoundedRectangle(cornerRadius: 12)
                    .stroke(DesignTokens.hairline, lineWidth: 1)
                    .frame(width: 220, height: 220)
                    .overlay(
                        VStack(spacing: 8) {
                            Image(systemName: "qrcode").font(.system(size: 72)).foregroundStyle(DesignTokens.ink)
                            Text("沃德龙鼎收款码", font: .system(size: 13), color: DesignTokens.body)
                        }
                    )
                Text("展示给客户扫码支付", font: .system(size: 14), color: DesignTokens.body)
                Button("保存到相册") {}
                    .buttonStyle(.borderedProminent)
                    .tint(DesignTokens.link)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(DesignTokens.canvasSoft2.ignoresSafeArea())
        }
    }
}

struct NativeQaPage: View {
    var onClose: () -> Void
    private let qs: [(String, String)] = [
        ("双擎和汽油怎么选？", "待回复 · 3 人围观"),
        ("置换补贴怎么算？", "已回复 · 12 人围观"),
        ("保养套餐有哪些？", "待回复 · 1 人围观"),
    ]
    var body: some View {
        VStack(spacing: 0) {
            navBar(title: "选买问答", onClose: onClose, dark: false)
            ScrollView {
                VStack(spacing: 0) {
                    ForEach(Array(qs.enumerated()), id: \.offset) { _, q in
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(q.0, font: .system(size: 15, weight: .semibold), color: DesignTokens.ink)
                                Text(q.1, font: .system(size: 13), color: DesignTokens.body)
                            }
                            Spacer()
                            Text("去回答", font: .system(size: 13), color: DesignTokens.link)
                        }
                        .padding(16).background(DesignTokens.canvas)
                        Divider().overlay(DesignTokens.hairline)
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
    @State private var picked = 0
    private let templates = ["秋季置换季", "周末到店礼", "新车上市"]
    var body: some View {
        VStack(spacing: 0) {
            navBar(title: "商家海报", onClose: onClose, dark: false)
            ScrollView {
                VStack(spacing: 12) {
                    ForEach(Array(templates.enumerated()), id: \.offset) { i, name in
                        HStack {
                            RoundedRectangle(cornerRadius: 8)
                                .fill(DesignTokens.link.opacity(0.15))
                                .frame(width: 72, height: 96)
                            VStack(alignment: .leading, spacing: 6) {
                                Text(name, font: .system(size: 16, weight: .semibold), color: DesignTokens.ink)
                                Text("模板 · 可编辑文案", font: .system(size: 13), color: DesignTokens.body)
                            }
                            Spacer()
                            if picked == i {
                                Image(systemName: "checkmark.circle.fill").foregroundStyle(DesignTokens.link)
                            }
                        }
                        .padding(12)
                        .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 12))
                        .onTapGesture { picked = i }
                    }
                    Button("生成海报") {}
                        .buttonStyle(.borderedProminent)
                        .tint(DesignTokens.link)
                }
                .padding(16)
            }
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

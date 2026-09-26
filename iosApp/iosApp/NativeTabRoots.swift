import SwiftUI

// MARK: - Tab bar (49pt, same contract as JetpackBottomBar)

struct NativeBottomBar: View {
    var selected: MainTab
    var onSelect: (MainTab) -> Void

    var body: some View {
        VStack(spacing: 0) {
            HStack(spacing: 0) {
                ForEach(MainTab.allCases) { tab in
                    let active = selected == tab
                    let tint = active ? DesignTokens.link : DesignTokens.body
                    Button {
                        onSelect(tab)
                    } label: {
                        VStack(spacing: 2) {
                            Image(tabIcon(tab, active: active))
                                .resizable()
                                .renderingMode(.original)
                                .frame(width: 22, height: 22)
                                .opacity(active ? 1 : 0.55)
                            Text(tab.title, font: .system(size: 10, weight: active ? .semibold : .regular), color: tint)
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 6)
                    }
                    .buttonStyle(.plain)
                }
            }
            .frame(height: 49)
        }
        .background(DesignTokens.tabBar)
        .overlay(alignment: .top) {
            Rectangle().fill(DesignTokens.hairline).frame(height: 0.5)
        }
    }

    private func tabIcon(_ tab: MainTab, active: Bool) -> String {
        switch tab {
        case .home:
            return active ? "main_tab_home_selected" : "main_tab_home_unselected"
        case .chat:
            return active ? "main_tab_chat_selected" : "main_tab_chat_unselected"
        case .community:
            return active ? "main_tab_community_selected" : "main_tab_community_unselected"
        case .mine:
            return active ? "main_tab_me_selected" : "main_tab_me_unselected"
        }
    }
}

// MARK: - Home
// SoT: my_ai_project HomePage — greeting → search → banner → feature(9) → todo →
// store metrics → strategy → services → contacts → news → learning report.
// NO 首页/视频/Club segment (removed from Flutter).

struct HomeTabView: View {
    var onDeferred: (String) -> Void
    @State private var metricTab = 0

    /// Flutter `HomeRepository.loadDashboard` features (max 9, last = 更多).
    private let features = [
        "H5 调试", "生活服务", "二手车", "新车成交", "新车跟进",
        "AI小石头", "Club", "直播带货", "更多",
    ]
    private let quickActions: [(String, String, String)] = [
        ("新伙伴待确认", "3 位新成员等待审核", "去处理"),
        ("待跟进客户", "今日 5 位意向客户", "去查看"),
        ("订单待审核", "2 笔新车订单", "去处理"),
        ("售后预约", "4 位客户今日到店", "去查看"),
    ]
    private let services: [(String, String?)] = [
        ("朋友圈", "热门"), ("视频号", nil), ("直播", "新品"), ("素材库", nil),
        ("话术库", nil), ("培训", nil), ("竞品分析", nil), ("更多", nil),
    ]
    private let metricSets: [[(String, String)]] = [
        [("99", "意向客户"), ("2", "新车订单"), ("999.8", "成交额(万)"), ("15", "试驾预约")],
        [("86", "意向客户"), ("1", "新车订单"), ("520.0", "成交额(万)"), ("12", "试驾预约")],
        [("1280", "意向客户"), ("45", "新车订单"), ("8600.5", "成交额(万)"), ("320", "试驾预约")],
    ]
    private let metricDetails = [("8", "待交车"), ("3", "待回访"), ("12", "待跟进")]

    private var greeting: String {
        let hour = Calendar.current.component(.hour, from: Date())
        let prefix = hour < 12 ? "早上好" : (hour < 18 ? "下午好" : "晚上好")
        return "\(prefix)，沃德龙鼎"
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                greetingRow
                searchRow
                banner
                featureGrid
                todoStrip
                storeMetrics
                hubEntry(title: "投资策略", subtitle: "资产九宫格 · 恐贪定投 · 趋势策略", route: "投资策略")
                serviceGrid
                contactsSection
                newsSection
                hubEntry(title: "学习报告", subtitle: "今日高光 · 学习记录", route: "学习报告")
            }
            .padding(.bottom, 24)
        }
        .safeAreaPadding(.top, 8)
        .background(DesignTokens.canvasSoft2)
    }

    private var greetingRow: some View {
        HStack(alignment: .center) {
            Text(greeting, font: .system(size: 28, weight: .bold), color: DesignTokens.ink)
                .tracking(-0.8)
                .lineLimit(1)
                .minimumScaleFactor(0.7)
                .frame(maxWidth: .infinity, alignment: .leading)
            HStack(spacing: 4) {
                Image(systemName: "bell")
                    .font(.system(size: 14, weight: .medium))
                    .foregroundStyle(DesignTokens.link)
                Text("3条新消息", font: .system(size: 12, weight: .medium), color: DesignTokens.link)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            .background(DesignTokens.link.opacity(0.1), in: Capsule())
            .onTapGesture { onDeferred("消息") }
        }
        .padding(.horizontal, 16)
        .padding(.top, 16)
    }

    private var searchRow: some View {
        HStack(spacing: 12) {
            HStack(spacing: 8) {
                Image(systemName: "magnifyingglass")
                    .font(.system(size: 16, weight: .medium))
                    .foregroundStyle(DesignTokens.body)
                Text("搜索客户、订单、资讯", font: .system(size: 15), color: DesignTokens.mute)
                Spacer(minLength: 0)
            }
            .padding(.horizontal, 14)
            .frame(height: 44)
            .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 8))
            .overlay(RoundedRectangle(cornerRadius: 8).stroke(DesignTokens.hairline, lineWidth: 0.5))
            .contentShape(Rectangle())
            .onTapGesture { onDeferred("搜索") }

            Image(systemName: "qrcode.viewfinder")
                .font(.system(size: 20, weight: .medium))
                .foregroundStyle(DesignTokens.link)
                .frame(width: 44, height: 44)
                .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 8))
                .overlay(RoundedRectangle(cornerRadius: 8).stroke(DesignTokens.hairline, lineWidth: 0.5))
                .onTapGesture { onDeferred("扫一扫") }
        }
        .padding(.horizontal, 16)
        .padding(.top, 16)
    }

    private var banner: some View {
        // Asset already contains title/CTA artwork — do not overlay duplicate text.
        Image("home_banner")
            .resizable()
            .scaledToFill()
            .frame(maxWidth: .infinity)
            .frame(height: 132)
            .clipShape(RoundedRectangle(cornerRadius: 8))
            .padding(.horizontal, 16)
            .padding(.top, 16)
            .onTapGesture { onDeferred("朋友圈营销") }
    }

    private var featureGrid: some View {
        let rows = stride(from: 0, to: features.count, by: 5).map { start in
            Array(features[start..<min(start + 5, features.count)])
        }
        return VStack(spacing: 0) {
            ForEach(Array(rows.enumerated()), id: \.offset) { _, row in
                HStack(spacing: 0) {
                    ForEach(row, id: \.self) { label in
                        featureCell(label)
                    }
                    ForEach(0..<(5 - row.count), id: \.self) { _ in
                        Color.clear.frame(maxWidth: .infinity)
                    }
                }
            }
        }
        .padding(.horizontal, 16)
        .padding(.top, 12)
    }

    private func featureCell(_ label: String) -> some View {
        VStack(spacing: 4) {
            Image(featureAsset(label))
                .resizable()
                .scaledToFill()
                .frame(width: 44, height: 44)
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            Text(label, font: .system(size: 11), color: DesignTokens.ink)
                .lineLimit(1)
                .minimumScaleFactor(0.75)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 8)
        .contentShape(Rectangle())
        .onTapGesture { onFeatureTap(label) }
    }

    private func onFeatureTap(_ label: String) {
        switch label {
        case "更多": onDeferred("全部服务")
        case "直播带货": onDeferred("直播带货")
        case "H5 调试": onDeferred("/web")
        default: onDeferred(label)
        }
    }

    private var todoStrip: some View {
        VStack(spacing: 10) {
            ForEach(Array(stride(from: 0, to: quickActions.count, by: 2)), id: \.self) { start in
                HStack(spacing: 12) {
                    ForEach(Array(quickActions[start..<min(start + 2, quickActions.count)]), id: \.0) { action in
                        VStack(alignment: .leading, spacing: 4) {
                            Text(action.0, font: .system(size: 15, weight: .semibold), color: DesignTokens.ink)
                                .lineLimit(1)
                            Text(action.1, font: .system(size: 12), color: DesignTokens.body)
                                .lineLimit(1)
                            Text(action.2, font: .system(size: 13, weight: .medium), color: DesignTokens.link)
                                .padding(.top, 4)
                        }
                        .padding(14)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 8))
                        .overlay(RoundedRectangle(cornerRadius: 8).stroke(DesignTokens.hairline, lineWidth: 0.5))
                        .onTapGesture { onDeferred(action.0) }
                    }
                    if start + 1 >= quickActions.count {
                        Color.clear.frame(maxWidth: .infinity)
                    }
                }
            }
        }
        .padding(.horizontal, 16)
        .padding(.top, 12)
    }

    private var storeMetrics: some View {
        let metrics = metricSets[min(metricTab, metricSets.count - 1)]
        return VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("公司数据", font: .system(size: 18, weight: .semibold), color: DesignTokens.ink)
                Spacer()
                Text("查看更多", font: .system(size: 13, weight: .medium), color: DesignTokens.link)
                Image(systemName: "chevron.right")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(DesignTokens.link)
            }

            VStack(alignment: .leading, spacing: 14) {
                HStack(spacing: 8) {
                    Image(systemName: "storefront")
                        .font(.system(size: 14))
                        .foregroundStyle(DesignTokens.link)
                        .frame(width: 28, height: 28)
                        .background(DesignTokens.link.opacity(0.12), in: RoundedRectangle(cornerRadius: 8))
                    Text("[4S]北京沃德龙鼎吉利", font: .system(size: 14, weight: .semibold), color: DesignTokens.ink)
                        .lineLimit(1)
                    Spacer()
                    Image(systemName: "chevron.down")
                        .font(.system(size: 12))
                        .foregroundStyle(DesignTokens.body)
                }
                .padding(.horizontal, 10)
                .padding(.vertical, 8)
                .background(DesignTokens.canvasSoft2, in: RoundedRectangle(cornerRadius: 10))

                HStack(spacing: 16) {
                    ForEach(Array(["今日", "昨日", "本月"].enumerated()), id: \.offset) { index, label in
                        Text(
                            label,
                            font: .system(size: 14, weight: metricTab == index ? .semibold : .regular),
                            color: metricTab == index ? DesignTokens.link : DesignTokens.body
                        )
                        .onTapGesture { metricTab = index }
                    }
                }

                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 10) {
                    ForEach(metrics, id: \.1) { m in
                        VStack(alignment: .leading, spacing: 4) {
                            Text(m.0, font: .system(size: 18, weight: .bold), color: DesignTokens.ink)
                            Text(m.1, font: .system(size: 11), color: DesignTokens.body)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(12)
                        .background(DesignTokens.canvasSoft2, in: RoundedRectangle(cornerRadius: 8))
                    }
                }

                Divider().overlay(DesignTokens.hairline)

                HStack(spacing: 8) {
                    ForEach(metricDetails, id: \.1) { d in
                        VStack(spacing: 4) {
                            Text(d.0, font: .system(size: 16, weight: .semibold), color: DesignTokens.ink)
                            Text(d.1, font: .system(size: 11), color: DesignTokens.body)
                            Text("详情 >", font: .system(size: 11), color: DesignTokens.link)
                        }
                        .frame(maxWidth: .infinity)
                    }
                }
            }
            .padding(14)
            .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 8))
            .overlay(RoundedRectangle(cornerRadius: 8).stroke(DesignTokens.hairline, lineWidth: 0.5))
        }
        .padding(.horizontal, 16)
        .padding(.top, 16)
    }

    private func hubEntry(title: String, subtitle: String, route: String) -> some View {
        HStack(spacing: 12) {
            Image(systemName: title.contains("学习") ? "chart.bar" : "square.grid.2x2")
                .font(.system(size: 20))
                .foregroundStyle(DesignTokens.link)
                .frame(width: 44, height: 44)
                .background(DesignTokens.link.opacity(0.1), in: RoundedRectangle(cornerRadius: 12))
            VStack(alignment: .leading, spacing: 2) {
                Text(title, font: .system(size: 17, weight: .semibold), color: DesignTokens.ink)
                Text(subtitle, font: .system(size: 13), color: DesignTokens.body)
            }
            Spacer()
            Image(systemName: "chevron.right")
                .font(.system(size: 13))
                .foregroundStyle(DesignTokens.mute)
        }
        .padding(16)
        .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 8))
        .overlay(RoundedRectangle(cornerRadius: 8).stroke(DesignTokens.hairline, lineWidth: 0.5))
        .padding(.horizontal, 16)
        .padding(.top, 16)
        .onTapGesture { onDeferred(route) }
    }

    private var serviceGrid: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("营销服务", font: .system(size: 18, weight: .semibold), color: DesignTokens.ink)
            LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 4), spacing: 12) {
                ForEach(services, id: \.0) { item in
                    VStack(spacing: 6) {
                        ZStack(alignment: .topTrailing) {
                            RoundedRectangle(cornerRadius: 12)
                                .fill(DesignTokens.link.opacity(0.1))
                                .frame(width: 48, height: 48)
                                .overlay(
                                    Text(String(item.0.prefix(1)), font: .system(size: 16, weight: .semibold), color: DesignTokens.link)
                                )
                            if let badge = item.1 {
                                Text(badge, font: .system(size: 9, weight: .medium), color: .white)
                                    .padding(.horizontal, 4)
                                    .padding(.vertical, 1)
                                    .background(Color.red, in: Capsule())
                                    .offset(x: 6, y: -4)
                            }
                        }
                        Text(item.0, font: .system(size: 11), color: DesignTokens.ink)
                            .lineLimit(1)
                    }
                    .onTapGesture {
                        if item.0 == "更多" { onDeferred("全部服务") }
                        else if item.0 == "直播" { onDeferred("直播") }
                        else { onDeferred(item.0) }
                    }
                }
            }
            .padding(12)
            .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 8))
            .overlay(RoundedRectangle(cornerRadius: 8).stroke(DesignTokens.hairline, lineWidth: 0.5))
        }
        .padding(.horizontal, 16)
        .padding(.top, 16)
    }

    private var contactsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("联系人", font: .system(size: 18, weight: .semibold), color: DesignTokens.ink)
            VStack(spacing: 0) {
                contactRow(title: "销售顾问小王", subtitle: "在线 · 专属顾问", trailing: "聊")
                Divider().overlay(DesignTokens.hairline)
                contactRow(title: "售后服务热线", subtitle: "400-800-8888", trailing: "拨")
            }
            .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 8))
            .overlay(RoundedRectangle(cornerRadius: 8).stroke(DesignTokens.hairline, lineWidth: 0.5))
        }
        .padding(.horizontal, 16)
        .padding(.top, 16)
    }

    private func contactRow(title: String, subtitle: String, trailing: String) -> some View {
        HStack(spacing: 12) {
            Circle()
                .fill(DesignTokens.link.opacity(0.15))
                .frame(width: 44, height: 44)
                .overlay(Text(String(title.suffix(1))).foregroundStyle(DesignTokens.link))
            VStack(alignment: .leading, spacing: 2) {
                Text(title, font: .system(size: 15, weight: .semibold), color: DesignTokens.ink)
                Text(subtitle, font: .system(size: 12), color: DesignTokens.body)
            }
            Spacer()
            Text(trailing, font: .system(size: 13, weight: .medium), color: DesignTokens.link)
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(DesignTokens.link.opacity(0.1), in: Capsule())
        }
        .padding(14)
    }

    private var newsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("资讯", font: .system(size: 18, weight: .semibold), color: DesignTokens.ink)
            HStack(spacing: 12) {
                RoundedRectangle(cornerRadius: 8)
                    .fill(DesignTokens.canvasSoft2)
                    .frame(width: 72, height: 72)
                VStack(alignment: .leading, spacing: 6) {
                    Text("吉利银河 E8 获年度车型奖", font: .system(size: 15, weight: .semibold), color: DesignTokens.ink)
                        .lineLimit(2)
                    Text("汽车之家 · 09-20", font: .system(size: 12), color: DesignTokens.body)
                }
                Spacer(minLength: 0)
            }
            .padding(14)
            .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 8))
            .overlay(RoundedRectangle(cornerRadius: 8).stroke(DesignTokens.hairline, lineWidth: 0.5))
        }
        .padding(.horizontal, 16)
        .padding(.top, 16)
    }

    private func featureAsset(_ label: String) -> String {
        switch label {
        case "H5 调试", "销售顾问": return "home_feature_sales"
        case "生活服务": return "home_feature_life"
        case "二手车": return "home_feature_usedcar"
        case "新车成交", "新车关注": return "home_feature_newcar"
        case "新车跟进", "数据分析": return "home_feature_data"
        case "AI小石头": return "home_feature_ai_stone"
        case "Club", "订单中心": return "home_feature_order"
        case "直播带货": return "home_feature_live"
        case "营销活动": return "home_feature_market"
        default: return "home_feature_more"
        }
    }
}

// MARK: - Chat

struct ChatTabView: View {
    var onDeferred: (String) -> Void = { _ in }
    var initialPeer: String? = nil
    private let peers: [(String, String, String, String?, Bool)] = [
        ("Mock好友1", "晚上一起吃饭吗？", "22:50", "2", true),
        ("Mock好友2", "你好", "22:45", nil, false),
        ("Mock好友3", "你好", "22:40", nil, true),
    ]
    @State private var selectedPeer: String? = nil
    @State private var draft = ""
    @State private var messages: [String] = []
    @State private var searchOpen = false
    @State private var searchQuery = ""
    @State private var inputMode: ChatInputMode = .keyboard
    @State private var showEmoji = false
    @State private var showMore = false

    private enum ChatInputMode { case keyboard, voice }

    private var visiblePeers: [(String, String, String, String?, Bool)] {
        let q = searchQuery.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !q.isEmpty else { return peers }
        return peers.filter { $0.0.contains(q) || $0.1.contains(q) }
    }

    var body: some View {
        VStack(spacing: 0) {
            if let peer = selectedPeer {
                chatDetail(peer: peer)
            } else {
                chatList
            }
        }
        .background(DesignTokens.canvasSoft2)
        .onAppear {
            if let peer = initialPeer, !peer.isEmpty {
                selectedPeer = peer
                messages = ["你好，我是\(peer)", "方便聊一下车源吗？"]
            }
        }
        .onChange(of: initialPeer) { _, peer in
            if let peer, !peer.isEmpty {
                selectedPeer = peer
                messages = ["你好，我是\(peer)", "方便聊一下车源吗？"]
            }
        }
    }

    private var chatList: some View {
        VStack(spacing: 0) {
            HStack {
                Text("消息", font: .system(size: 32, weight: .bold), color: DesignTokens.ink)
                Spacer()
                Button {
                    withAnimation(.easeOut(duration: 0.2)) {
                        searchOpen.toggle()
                        if !searchOpen { searchQuery = "" }
                    }
                } label: {
                    Image(systemName: searchOpen ? "xmark" : "magnifyingglass")
                        .font(.system(size: 18, weight: .medium))
                        .foregroundStyle(DesignTokens.link)
                        .frame(width: 40, height: 40)
                }
                Button {
                    onDeferred("/friend")
                } label: {
                    Image(systemName: "square.and.pencil")
                        .font(.system(size: 18, weight: .medium))
                        .foregroundStyle(DesignTokens.link)
                        .frame(width: 40, height: 40)
                }
            }
            .padding(.leading, 16)
            .padding(.trailing, 8)
            .padding(.vertical, 8)

            if searchOpen {
                HStack(spacing: 8) {
                    Image(systemName: "magnifyingglass")
                        .font(.system(size: 14))
                        .foregroundStyle(DesignTokens.mute)
                    TextField("搜索会话名称或消息", text: $searchQuery)
                        .font(.system(size: 15))
                }
                .padding(.horizontal, 12)
                .frame(height: 40)
                .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 10))
                .padding(.horizontal, 16)
                .padding(.bottom, 8)
            }

            if visiblePeers.isEmpty {
                VStack(spacing: 8) {
                    Spacer()
                    Text("无匹配会话", font: .system(size: 15), color: DesignTokens.body)
                    Spacer()
                }
            } else {
                ScrollView {
                    VStack(spacing: 0) {
                        ForEach(Array(visiblePeers.enumerated()), id: \.offset) { index, peer in
                            HStack(spacing: 12) {
                                ZStack(alignment: .bottomTrailing) {
                                    Text(String(peer.0.suffix(1)), color: DesignTokens.link)
                                        .frame(width: 48, height: 48)
                                        .background(DesignTokens.link.opacity(0.15), in: Circle())
                                    if peer.4 {
                                        Circle()
                                            .fill(DesignTokens.link)
                                            .frame(width: 10, height: 10)
                                            .overlay(Circle().stroke(.white, lineWidth: 1.5))
                                    }
                                }
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(peer.0, font: .system(size: 16, weight: .semibold), color: DesignTokens.ink)
                                    Text(peer.1, font: .system(size: 13), color: DesignTokens.body).lineLimit(1)
                                }
                                Spacer()
                                VStack(alignment: .trailing, spacing: 6) {
                                    Text(peer.2, font: .system(size: 11), color: DesignTokens.body)
                                    if let badge = peer.3 {
                                        Text(badge, font: .system(size: 11), color: .white)
                                            .padding(.horizontal, 6)
                                            .padding(.vertical, 2)
                                            .background(Color(red: 0xEE/255, green: 0, blue: 0), in: Capsule())
                                    }
                                }
                            }
                            .padding(16)
                            .contentShape(Rectangle())
                            .onTapGesture {
                                selectedPeer = peer.0
                                messages = ["你好，在吗？", peer.1]
                                draft = ""
                            }
                            if index != visiblePeers.count - 1 {
                                Divider().overlay(DesignTokens.hairline).padding(.leading, 76)
                            }
                        }
                    }
                    .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 12))
                    .padding(.horizontal, 16)
                }
            }
            Spacer(minLength: 0)
        }
    }

    private func chatDetail(peer: String) -> some View {
        VStack(spacing: 0) {
            HStack {
                Button {
                    selectedPeer = nil
                } label: {
                    Image(systemName: "chevron.left").foregroundStyle(DesignTokens.link)
                }
                Text(peer, font: .system(size: 17, weight: .semibold), color: DesignTokens.ink)
                Spacer()
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .background(DesignTokens.canvas)
            .overlay(alignment: .bottom) {
                Rectangle().fill(DesignTokens.hairline).frame(height: 0.5)
            }

            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    ForEach(Array(messages.enumerated()), id: \.offset) { index, body in
                        let isSelf = index % 2 == 1
                        HStack {
                            if isSelf { Spacer(minLength: 48) }
                            Text(body, font: .system(size: 15), color: isSelf ? .white : DesignTokens.ink)
                                .padding(.horizontal, 12)
                                .padding(.vertical, 8)
                                .background(
                                    isSelf ? DesignTokens.link : DesignTokens.canvas,
                                    in: RoundedRectangle(cornerRadius: 12)
                                )
                            if !isSelf { Spacer(minLength: 48) }
                        }
                    }
                }
                .padding(16)
            }

            // Flutter InputPanel: voice ↔ keyboard, emoji, more (album/camera)
            VStack(spacing: 0) {
                HStack(spacing: 8) {
                    Button {
                        inputMode = inputMode == .voice ? .keyboard : .voice
                        showEmoji = false
                        showMore = false
                    } label: {
                        Image(systemName: inputMode == .voice ? "keyboard" : "mic")
                            .font(.system(size: 20))
                            .foregroundStyle(DesignTokens.body)
                            .frame(width: 36, height: 36)
                    }
                    if inputMode == .voice {
                        Text("按住 说话")
                            .font(.system(size: 15, weight: .medium))
                            .foregroundStyle(DesignTokens.ink)
                            .frame(maxWidth: .infinity)
                            .frame(height: 40)
                            .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 8))
                            .overlay(RoundedRectangle(cornerRadius: 8).stroke(DesignTokens.hairline, lineWidth: 0.5))
                    } else {
                        TextField("输入消息…", text: $draft)
                            .padding(.horizontal, 12)
                            .frame(height: 40)
                            .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 8))
                    }
                    Button {
                        showEmoji.toggle()
                        showMore = false
                        inputMode = .keyboard
                    } label: {
                        Image(systemName: "face.smiling")
                            .font(.system(size: 20))
                            .foregroundStyle(showEmoji ? DesignTokens.link : DesignTokens.body)
                            .frame(width: 36, height: 36)
                    }
                    if draft.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                        Button {
                            showMore.toggle()
                            showEmoji = false
                        } label: {
                            Image(systemName: "plus.circle")
                                .font(.system(size: 28))
                                .foregroundStyle(showMore ? DesignTokens.link : DesignTokens.body)
                        }
                    } else {
                        Button {
                            let text = draft.trimmingCharacters(in: .whitespacesAndNewlines)
                            guard !text.isEmpty else { return }
                            messages.append(text)
                            draft = ""
                        } label: {
                            Text("发送", font: .system(size: 14, weight: .semibold), color: .white)
                                .padding(.horizontal, 12)
                                .frame(height: 36)
                                .background(DesignTokens.link, in: Capsule())
                        }
                    }
                }
                .padding(12)

                if showEmoji {
                    let emojis = ["😀", "😁", "😂", "🤣", "😊", "😍", "🥰", "😘", "👍", "🙏", "🔥", "🎉", "🚗", "🏠", "✅", "❤️"]
                    LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 8), spacing: 8) {
                        ForEach(emojis, id: \.self) { e in
                            Text(e).font(.system(size: 28))
                                .onTapGesture { draft += e }
                        }
                    }
                    .padding(12)
                    .frame(height: 160)
                    .background(DesignTokens.canvas)
                }

                if showMore {
                    HStack(spacing: 24) {
                        moreAction("照片", "photo.on.rectangle") { messages.append("[图片]") }
                        moreAction("拍摄", "camera") { messages.append("[拍摄]") }
                        moreAction("文件", "doc") { messages.append("[文件]") }
                        moreAction("位置", "location") { messages.append("[位置]") }
                    }
                    .padding(.vertical, 20)
                    .frame(maxWidth: .infinity)
                    .background(DesignTokens.canvas)
                }
            }
            .background(DesignTokens.canvasSoft2)
        }
    }

    private func moreAction(_ title: String, _ icon: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            VStack(spacing: 8) {
                Image(systemName: icon)
                    .font(.system(size: 22))
                    .foregroundStyle(DesignTokens.link)
                    .frame(width: 52, height: 52)
                    .background(DesignTokens.canvasSoft2, in: RoundedRectangle(cornerRadius: 12))
                Text(title, font: .system(size: 12), color: DesignTokens.body)
            }
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Community

struct CommunityTabView: View {
    var onDeferred: (String) -> Void = { _ in }
    @State private var filter = "最新"
    @State private var liked: Set<String> = []

    private var feed: [(String, String, String, [String], Bool, String, String)] {
        switch filter {
        case "热门":
            return [
                ("测试甲", "2小时前 · 热门", "本周试驾排行榜出炉！#新车\n欢迎访问：https://flutter.dev", (0..<9).map { "https://picsum.photos/seed/sot_hot_\($0)/400/400" }, false, "1.2k", "86"),
                ("测试甲", "昨天 · 热门", "周末自驾召集，评论报名 #户外", (0..<9).map { "https://picsum.photos/seed/sot_hot2_\($0)/400/400" }, false, "860", "42"),
            ]
        case "关注":
            return [
                ("测试甲", "刚刚 · 关注", "刚发了保养心得，求交流。", (0..<4).map { "https://picsum.photos/seed/sot_follow_\($0)/400/400" }, false, "12", "3"),
            ]
        default:
            return [
                ("测试甲", "7分钟前 · 来自 iPhone", "今天去了推荐的咖啡店，环境不错。\n#Flutter开发\n欢迎访问：https://flutter.dev", (0..<9).map { "https://picsum.photos/seed/sot_a_\($0)/400/400" }, false, "158", "6"),
                ("测试甲", "42分钟前 · 来自 Android", "周末 hiking，天气太好了！#户外", (0..<9).map { "https://picsum.photos/seed/sot_b_\($0)/400/400" }, false, "36", "4"),
            ]
        }
    }

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Text("社区", font: .system(size: 32, weight: .bold), color: DesignTokens.ink)
                Spacer()
                Text("+", font: .system(size: 22, weight: .bold), color: .white)
                    .frame(width: 36, height: 36)
                    .background(DesignTokens.link, in: Circle())
                    .onTapGesture { onDeferred("/community/publish") }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 8)

            HStack(spacing: 8) {
                Image(systemName: "magnifyingglass")
                    .font(.system(size: 14))
                    .foregroundStyle(DesignTokens.body)
                Text("搜索动态、话题、用户", font: .system(size: 14), color: DesignTokens.body)
                Spacer()
            }
            .padding(.horizontal, 12)
            .frame(height: 40)
            .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 10))
            .padding(.horizontal, 16)
            .onTapGesture { onDeferred("/community/search") }

            HStack {
                ForEach(["最新", "热门", "关注"], id: \.self) { item in
                    VStack(spacing: 6) {
                        Text(item, font: .system(size: 16, weight: filter == item ? .semibold : .regular), color: filter == item ? DesignTokens.ink : DesignTokens.body)
                        Capsule()
                            .fill(filter == item ? DesignTokens.link : .clear)
                            .frame(width: 18, height: 3)
                    }
                    .frame(maxWidth: .infinity)
                    .onTapGesture { filter = item }
                }
            }
            .padding(.vertical, 12)

            ScrollView {
                VStack(spacing: 12) {
                    ForEach(Array(feed.enumerated()), id: \.offset) { _, post in
                        communityCard(
                            id: "\(filter)-\(post.0)-\(post.1)",
                            name: post.0,
                            meta: post.1,
                            body: post.2,
                            images: post.3,
                            singleImage: post.4,
                            likes: post.5,
                            comments: post.6
                        )
                    }
                }
                .padding(.horizontal, 16)
                .padding(.top, 8)
                .padding(.bottom, 16)
            }
        }
        .background(DesignTokens.canvasSoft2)
    }

    private func communityCard(
        id: String,
        name: String,
        meta: String,
        body: String,
        images: [String],
        singleImage: Bool,
        likes: String,
        comments: String
    ) -> some View {
        let isLiked = liked.contains(id)
        return VStack(alignment: .leading, spacing: 10) {
            HStack(alignment: .top) {
                Circle().fill(DesignTokens.hairline).frame(width: 44, height: 44)
                VStack(alignment: .leading, spacing: 2) {
                    Text(name, font: .system(size: 16, weight: .semibold), color: DesignTokens.ink)
                    Text(meta, font: .system(size: 14), color: DesignTokens.body)
                }
                Spacer()
                Menu {
                    Button("复制") {}
                    Button("举报", role: .destructive) {}
                } label: {
                    Text("⋯", font: .system(size: 20), color: DesignTokens.body)
                        .frame(width: 44, height: 44)
                }
            }
            Text(body, font: .system(size: 16), color: DesignTokens.ink)
                .lineSpacing(4)
            communityImageGrid(images: images)
            HStack(spacing: 24) {
                Button {
                    if isLiked { liked.remove(id) } else { liked.insert(id) }
                } label: {
                    Label(isLiked ? "已赞 \(likes)" : "赞 \(likes)", systemImage: isLiked ? "heart.fill" : "heart")
                        .font(.system(size: 13))
                        .foregroundStyle(isLiked ? Color.red : DesignTokens.body)
                }
                Button {
                    onDeferred("/community/comment")
                } label: {
                    Label("评论 \(comments)", systemImage: "bubble.right")
                        .font(.system(size: 13))
                        .foregroundStyle(DesignTokens.body)
                }
                Spacer()
            }
        }
        .padding(14)
        .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 12))
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(DesignTokens.hairline, lineWidth: 0.5))
    }

    @ViewBuilder
    private func communityImageGrid(images: [String]) -> some View {
        let cells = Array(images.prefix(9))
        let columns = Array(repeating: GridItem(.flexible(), spacing: 4), count: min(3, max(1, cells.count)))
        LazyVGrid(columns: columns, spacing: 4) {
            ForEach(Array(cells.enumerated()), id: \.offset) { _, src in
                communityThumb(src)
                    .aspectRatio(1, contentMode: .fill)
                    .clipped()
                    .clipShape(RoundedRectangle(cornerRadius: 4))
                    .onTapGesture { onDeferred("/community/image_preview") }
            }
        }
    }

    @ViewBuilder
    private func communityThumb(_ src: String) -> some View {
        if src.hasPrefix("http"), let url = URL(string: src) {
            AsyncImage(url: url) { phase in
                switch phase {
                case .success(let image):
                    image.resizable().scaledToFill()
                default:
                    DesignTokens.hairline
                }
            }
        } else {
            Image(src).resizable().scaledToFill()
        }
    }
}

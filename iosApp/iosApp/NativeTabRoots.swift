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
    var displayName: String = "访客"
    var isLoggedIn: Bool = false
    @State private var metricTab = 0
    @State private var showTodos = true
    @State private var showSwitchStore = false
    @State private var storeName = "[4S]北京沃德龙鼎吉利"
    @State private var selectedStoreId = "1"
    @State private var showCheckIn = false
    private let stores: [(String, String)] = [
        ("1", "[4S]北京沃德龙鼎吉利"),
        ("2", "[4S]北京腾远吉利"),
    ]

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
        let name = displayName.trimmingCharacters(in: .whitespacesAndNewlines)
        return "\(prefix)，\(name.isEmpty ? "访客" : name)"
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                greetingRow
                searchRow
                banner
                featureGrid
                if showTodos { todoStrip }
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
        .sheet(isPresented: $showSwitchStore) {
            HomeSwitchStoreSheet(
                stores: stores,
                selectedId: selectedStoreId,
                onPick: { id in
                    selectedStoreId = id
                    storeName = stores.first(where: { $0.0 == id })?.1 ?? storeName
                    showSwitchStore = false
                },
                onClose: { showSwitchStore = false }
            )
            .presentationDetents([.height(320)])
        }
        .onAppear {
            guard isLoggedIn else { return }
            let today = ISO8601DateFormatter().string(from: Date()).prefix(10)
            let ack = UserDefaults.standard.string(forKey: "check_in_dialog_ack_date")
            if ack != String(today) {
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.4) {
                    showCheckIn = true
                }
            }
        }
        .alert("每日签到", isPresented: $showCheckIn) {
            Button("立即签到") {
                let today = ISO8601DateFormatter().string(from: Date()).prefix(10)
                UserDefaults.standard.set(String(today), forKey: "check_in_dialog_ack_date")
            }
            Button("稍后再说", role: .cancel) {
                let today = ISO8601DateFormatter().string(from: Date()).prefix(10)
                UserDefaults.standard.set(String(today), forKey: "check_in_dialog_ack_date")
            }
        } message: {
            Text("连续签到 3 天 · 今日可领 +10 积分")
        }
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
                    Text(storeName, font: .system(size: 14, weight: .semibold), color: DesignTokens.ink)
                        .lineLimit(1)
                    Spacer()
                    Image(systemName: "chevron.down")
                        .font(.system(size: 12))
                        .foregroundStyle(DesignTokens.body)
                }
                .padding(.horizontal, 10)
                .padding(.vertical, 8)
                .background(DesignTokens.canvasSoft2, in: RoundedRectangle(cornerRadius: 10))
                .contentShape(Rectangle())
                .onTapGesture {
                    if isLoggedIn { showSwitchStore = true }
                    else { onDeferred("请先登录") }
                }

                HStack(spacing: 16) {
                    ForEach(Array(["今日", "昨日", "近30天"].enumerated()), id: \.offset) { index, label in
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

/// Flutter SwitchStoreDialog — Home metrics store row.
private struct HomeSwitchStoreSheet: View {
    let stores: [(String, String)]
    let selectedId: String
    var onPick: (String) -> Void
    var onClose: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            Text("切换店铺", font: .system(size: 18, weight: .semibold), color: DesignTokens.ink)
                .padding(.top, 24)
            Text("可切换多个店铺查看数据", font: .system(size: 13), color: DesignTokens.body)
                .padding(.top, 8)
                .padding(.bottom, 20)
            VStack(spacing: 12) {
                ForEach(stores, id: \.0) { store in
                    let selected = store.0 == selectedId
                    Button {
                        if store.0 == selectedId { onClose() }
                        else { onPick(store.0) }
                    } label: {
                        Text(
                            store.1,
                            font: .system(size: 15, weight: .medium),
                            color: selected
                                ? Color(red: 0x1B/255, green: 0x82/255, blue: 0xD2/255)
                                : DesignTokens.ink
                        )
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .overlay(
                            RoundedRectangle(cornerRadius: 24)
                                .stroke(
                                    selected
                                        ? Color(red: 0x1B/255, green: 0x82/255, blue: 0xD2/255)
                                        : DesignTokens.hairline,
                                    lineWidth: 1
                                )
                        )
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal, 20)
            Spacer(minLength: 16)
            Button("取消", action: onClose)
                .padding(.bottom, 24)
        }
    }
}

// MARK: - Chat (Flutter MockImChatStore / ChatPage / ChatDetailPage SoT)

private struct ChatConv: Identifiable {
    let id: String
    let peerId: String
    var title: String
    var lastMessage: String
    var lastMessageAt: Date
    var unreadCount: Int
    var isOnline: Bool
}

private struct ChatMsg: Identifiable {
    let id: String
    var body: String
    var isSelf: Bool
    var createdAt: Date
    var sendStatus: String // sending | success | failed
    var readStatus: String // unread | read
    var type: String // text | image | voice | custom | time
}

/// Flutter `MockImChatStore` + mock `ImChatRepository._send` port for SwiftUI tab root.
@MainActor
private final class FlutterChatStore: ObservableObject {
    @Published var conversations: [ChatConv] = []
    @Published private var messagesById: [String: [ChatMsg]] = [:]

    init() { seed() }

    private func seed() {
        let now = Date()
        let peers = ["mock_peer_01", "mock_peer_02", "mock_peer_03"]
        var convs: [ChatConv] = []
        for (i, peer) in peers.enumerated() {
            let storageId = "private_\(peer)"
            convs.append(ChatConv(
                id: storageId,
                peerId: peer,
                title: "Mock好友\(i + 1)",
                lastMessage: i == 0 ? "晚上一起吃饭吗？" : "你好",
                lastMessageAt: now.addingTimeInterval(-Double(5 * (i + 1)) * 60),
                unreadCount: i == 0 ? 2 : 0,
                isOnline: i % 2 == 0
            ))
            if i == 0 {
                // Newest-first (Flutter insert(0) + reverse ListView).
                messagesById[storageId] = [
                    ChatMsg(id: "m_2", body: "在的，有什么事？", isSelf: true,
                            createdAt: now.addingTimeInterval(-28 * 60),
                            sendStatus: "success", readStatus: "read", type: "text"),
                    ChatMsg(id: "m_1", body: "你好，在吗？", isSelf: false,
                            createdAt: now.addingTimeInterval(-30 * 60),
                            sendStatus: "success", readStatus: "read", type: "text"),
                ]
            }
        }
        conversations = convs.sorted { $0.lastMessageAt > $1.lastMessageAt }
    }

    func messages(for id: String) -> [ChatMsg] {
        // Newest-first in store; UI reverses for chronological bottom-up like Flutter.
        Array((messagesById[id] ?? []).reversed())
    }

    func newestFirst(for id: String) -> [ChatMsg] {
        messagesById[id] ?? []
    }

    func filter(_ query: String) -> [ChatConv] {
        let q = query.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        guard !q.isEmpty else { return conversations }
        return conversations.filter {
            $0.title.lowercased().contains(q) || $0.lastMessage.lowercased().contains(q)
        }
    }

    func markRead(_ id: String) {
        if var list = messagesById[id] {
            for i in list.indices where !list[i].isSelf && list[i].readStatus == "unread" {
                list[i].readStatus = "read"
            }
            messagesById[id] = list
        }
        if let i = conversations.firstIndex(where: { $0.id == id }) {
            conversations[i].unreadCount = 0
        }
    }

    func ensure(id: String, title: String, lastMessage: String, unread: Int) -> String {
        let storageId = id.hasPrefix("private_") || id.hasPrefix("group_") ? id : "private_\(id)"
        if let i = conversations.firstIndex(where: { $0.id == storageId }) {
            conversations[i].title = title.isEmpty ? conversations[i].title : title
            if !lastMessage.isEmpty { conversations[i].lastMessage = lastMessage }
            if unread > 0 { conversations[i].unreadCount = unread }
            conversations[i].lastMessageAt = Date()
        } else {
            let peer = storageId.replacingOccurrences(of: "private_", with: "")
            conversations.insert(ChatConv(
                id: storageId, peerId: peer, title: title.isEmpty ? "推送会话" : title,
                lastMessage: lastMessage.isEmpty ? "来自 Push/Deeplink 的 mock 会话" : lastMessage,
                lastMessageAt: Date(), unreadCount: unread, isOnline: false
            ), at: 0)
            if !lastMessage.isEmpty {
                messagesById[storageId] = [
                    ChatMsg(id: "m_push", body: lastMessage, isSelf: false, createdAt: Date(),
                            sendStatus: "success", readStatus: "unread", type: "text"),
                ]
            }
        }
        conversations.sort { $0.lastMessageAt > $1.lastMessageAt }
        return storageId
    }

    func sendText(_ id: String, _ text: String) {
        let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        send(id: id, type: "text", body: trimmed, preview: trimmed)
    }

    func sendImage(_ id: String) {
        send(id: id, type: "image", body: "https://picsum.photos/seed/chat/600", preview: "[图片]")
    }

    func sendVoice(_ id: String) {
        send(id: id, type: "voice", body: "[语音]", preview: "[语音]")
    }

    func sendCustom(_ id: String, _ label: String) {
        send(id: id, type: "custom", body: label, preview: "[自定义消息]")
    }

    private func send(id: String, type: String, body: String, preview: String) {
        let now = Date()
        var list = messagesById[id] ?? []
        if let latest = list.first, latest.type != "time",
           abs(now.timeIntervalSince(latest.createdAt)) >= 5 * 60 {
            list.insert(ChatMsg(id: "time_\(Int(now.timeIntervalSince1970))", body: Self.hm(now),
                                isSelf: false, createdAt: now, sendStatus: "success",
                                readStatus: "read", type: "time"), at: 0)
        } else if list.isEmpty {
            list.insert(ChatMsg(id: "time_\(Int(now.timeIntervalSince1970))", body: Self.hm(now),
                                isSelf: false, createdAt: now, sendStatus: "success",
                                readStatus: "read", type: "time"), at: 0)
        }
        let localId = "local_\(Int(now.timeIntervalSince1970 * 1000))"
        list.insert(ChatMsg(id: localId, body: body, isSelf: true, createdAt: now,
                            sendStatus: "sending", readStatus: "unread", type: type), at: 0)
        messagesById[id] = list
        if let i = conversations.firstIndex(where: { $0.id == id }) {
            conversations[i].lastMessage = preview
            conversations[i].lastMessageAt = now
        }
        conversations.sort { $0.lastMessageAt > $1.lastMessageAt }
        objectWillChange.send()

        Task { @MainActor in
            try? await Task.sleep(nanoseconds: 280_000_000)
            if var cur = messagesById[id], let idx = cur.firstIndex(where: { $0.id == localId }) {
                cur[idx].sendStatus = "success"
                messagesById[id] = cur
                objectWillChange.send()
            }
            try? await Task.sleep(nanoseconds: 2_000_000_000)
            if var cur = messagesById[id], let idx = cur.firstIndex(where: { $0.id == localId && $0.isSelf }) {
                cur[idx].readStatus = "read"
                messagesById[id] = cur
                objectWillChange.send()
            }
        }
    }

    static func hm(_ date: Date) -> String {
        let f = DateFormatter()
        f.dateFormat = "HH:mm"
        return f.string(from: date)
    }

    static func listTime(_ date: Date) -> String {
        let cal = Calendar.current
        if cal.isDateInToday(date) { return hm(date) }
        if cal.isDateInYesterday(date) { return "昨天" }
        let f = DateFormatter()
        f.dateFormat = "M/d"
        return f.string(from: date)
    }

    static func statusLabel(_ m: ChatMsg) -> String {
        guard m.isSelf, m.type != "time" else { return "" }
        switch m.sendStatus {
        case "sending": return "发送中"
        case "failed": return "发送失败"
        default: return m.readStatus == "read" ? "已读" : "送达"
        }
    }
}

struct ChatTabView: View {
    var onDeferred: (String) -> Void = { _ in }
    var initialPeer: String? = nil
    @StateObject private var store = FlutterChatStore()
    @State private var selectedId: String? = nil
    @State private var draft = ""
    @State private var searchOpen = false
    @State private var searchQuery = ""
    @State private var inputMode: ChatInputMode = .keyboard
    @State private var showEmoji = false
    @State private var showMore = false

    private enum ChatInputMode { case keyboard, voice }

    private var visible: [ChatConv] { store.filter(searchQuery) }

    var body: some View {
        VStack(spacing: 0) {
            if let id = selectedId, let conv = store.conversations.first(where: { $0.id == id }) {
                chatDetail(conv)
            } else {
                chatList
            }
        }
        .background(DesignTokens.canvasSoft2)
        .onAppear {
            if let peer = initialPeer, !peer.isEmpty {
                let id = store.ensure(id: peer, title: peer, lastMessage: "来自 Push/Deeplink 的 mock 会话", unread: 1)
                selectedId = id
                store.markRead(id)
            }
        }
        .onChange(of: initialPeer) { _, peer in
            if let peer, !peer.isEmpty {
                let id = store.ensure(id: peer, title: peer, lastMessage: "来自 Push/Deeplink 的 mock 会话", unread: 1)
                selectedId = id
                store.markRead(id)
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
                Button { onDeferred("/friend") } label: {
                    Image(systemName: "square.and.pencil")
                        .font(.system(size: 18, weight: .medium))
                        .foregroundStyle(DesignTokens.link)
                        .frame(width: 40, height: 40)
                }
            }
            .padding(.leading, 16).padding(.trailing, 8).padding(.vertical, 8)

            if searchOpen {
                HStack(spacing: 8) {
                    Image(systemName: "magnifyingglass").font(.system(size: 14)).foregroundStyle(DesignTokens.mute)
                    TextField("搜索会话名称或消息", text: $searchQuery).font(.system(size: 15))
                }
                .padding(.horizontal, 12).frame(height: 40)
                .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 10))
                .padding(.horizontal, 16).padding(.bottom, 8)
            }

            if visible.isEmpty {
                VStack {
                    Spacer()
                    Text(searchQuery.isEmpty ? "还没有消息" : "没有匹配的会话",
                         font: .system(size: 15), color: DesignTokens.body)
                    Spacer()
                }
            } else {
                ScrollView {
                    VStack(spacing: 0) {
                        ForEach(Array(visible.enumerated()), id: \.element.id) { index, conv in
                            HStack(spacing: 12) {
                                ZStack(alignment: .bottomTrailing) {
                                    Text(String(conv.title.suffix(1)), color: DesignTokens.link)
                                        .frame(width: 52, height: 52)
                                        .background(DesignTokens.link.opacity(0.15), in: Circle())
                                    if conv.isOnline {
                                        Circle().fill(DesignTokens.link).frame(width: 12, height: 12)
                                            .overlay(Circle().stroke(.white, lineWidth: 2))
                                    }
                                }
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(conv.title, font: .system(size: 16, weight: .semibold), color: DesignTokens.ink)
                                    Text(conv.lastMessage, font: .system(size: 13), color: DesignTokens.body).lineLimit(1)
                                }
                                Spacer()
                                VStack(alignment: .trailing, spacing: 6) {
                                    Text(FlutterChatStore.listTime(conv.lastMessageAt), font: .system(size: 11), color: DesignTokens.body)
                                    if conv.unreadCount > 0 {
                                        Text(conv.unreadCount > 99 ? "99+" : "\(conv.unreadCount)",
                                             font: .system(size: 11), color: .white)
                                            .padding(.horizontal, 6).padding(.vertical, 2)
                                            .background(Color(red: 0xEE/255, green: 0, blue: 0), in: Capsule())
                                    }
                                }
                            }
                            .padding(16)
                            .contentShape(Rectangle())
                            .onTapGesture {
                                selectedId = conv.id
                                store.markRead(conv.id)
                                draft = ""
                                inputMode = .keyboard
                                showEmoji = false
                                showMore = false
                            }
                            if index != visible.count - 1 {
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

    private func chatDetail(_ conv: ChatConv) -> some View {
        let msgs = store.newestFirst(for: conv.id)
        return VStack(spacing: 0) {
            HStack {
                Button { selectedId = nil } label: {
                    Image(systemName: "chevron.left").foregroundStyle(DesignTokens.link)
                }
                Text(conv.title, font: .system(size: 17, weight: .semibold), color: DesignTokens.ink)
                Spacer()
            }
            .padding(.horizontal, 16).padding(.vertical, 12)
            .background(DesignTokens.canvas)
            .overlay(alignment: .bottom) { Rectangle().fill(DesignTokens.hairline).frame(height: 0.5) }

            ScrollViewReader { proxy in
                ScrollView {
                    LazyVStack(alignment: .leading, spacing: 12) {
                        ForEach(msgs.reversed()) { msg in
                            if msg.type == "time" {
                                Text(msg.body, font: .system(size: 12), color: DesignTokens.mute)
                                    .frame(maxWidth: .infinity)
                            } else {
                                HStack {
                                    if msg.isSelf { Spacer(minLength: 48) }
                                    VStack(alignment: msg.isSelf ? .trailing : .leading, spacing: 2) {
                                        Text(msg.type == "image" ? "[图片]" : (msg.type == "voice" ? "[语音]" : msg.body),
                                             font: .system(size: 15),
                                             color: msg.isSelf ? .white : DesignTokens.ink)
                                            .padding(.horizontal, 12).padding(.vertical, 8)
                                            .background(msg.isSelf ? DesignTokens.link : DesignTokens.canvas,
                                                        in: RoundedRectangle(cornerRadius: 12))
                                        let status = FlutterChatStore.statusLabel(msg)
                                        Text(status.isEmpty ? FlutterChatStore.hm(msg.createdAt) : status,
                                             font: .system(size: 11), color: DesignTokens.mute)
                                    }
                                    if !msg.isSelf { Spacer(minLength: 48) }
                                }
                                .id(msg.id)
                            }
                        }
                    }
                    .padding(16)
                }
                .onChange(of: msgs.first?.id) { _, _ in
                    if let id = msgs.first?.id {
                        withAnimation { proxy.scrollTo(id, anchor: .bottom) }
                    }
                }
            }

            VStack(spacing: 0) {
                HStack(spacing: 8) {
                    Button {
                        inputMode = inputMode == .voice ? .keyboard : .voice
                        showEmoji = false
                        showMore = false
                    } label: {
                        Image(systemName: inputMode == .voice ? "keyboard" : "mic")
                            .font(.system(size: 20)).foregroundStyle(DesignTokens.body)
                            .frame(width: 36, height: 36)
                    }
                    if inputMode == .voice {
                        Text("按住 说话")
                            .font(.system(size: 15, weight: .medium))
                            .frame(maxWidth: .infinity).frame(height: 40)
                            .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 8))
                            .onTapGesture { store.sendVoice(conv.id) }
                    } else {
                        TextField("输入消息…", text: $draft)
                            .padding(.horizontal, 12).frame(height: 40)
                            .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 8))
                    }
                    Button {
                        showEmoji.toggle(); showMore = false; inputMode = .keyboard
                    } label: {
                        Image(systemName: "face.smiling")
                            .font(.system(size: 20))
                            .foregroundStyle(showEmoji ? DesignTokens.link : DesignTokens.body)
                            .frame(width: 36, height: 36)
                    }
                    if draft.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                        Button { showMore.toggle(); showEmoji = false } label: {
                            Image(systemName: "plus.circle").font(.system(size: 28))
                                .foregroundStyle(showMore ? DesignTokens.link : DesignTokens.body)
                        }
                    } else {
                        Button {
                            store.sendText(conv.id, draft)
                            draft = ""
                            inputMode = .keyboard
                            showEmoji = false
                            showMore = false
                        } label: {
                            Text("发送", font: .system(size: 14, weight: .semibold), color: .white)
                                .padding(.horizontal, 12).frame(height: 36)
                                .background(DesignTokens.link, in: Capsule())
                        }
                    }
                }
                .padding(12)

                if showEmoji {
                    let emojis = ["😀", "😂", "🥰", "😎", "🤔", "👍", "🙏", "🎉",
                                  "❤️", "🔥", "👋", "😭", "🤣", "😊", "🥳", "💪"]
                    LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 8), spacing: 8) {
                        ForEach(emojis, id: \.self) { e in
                            Text(e).font(.system(size: 28)).onTapGesture { draft += e }
                        }
                    }
                    .padding(12).frame(height: 160).background(DesignTokens.canvas)
                }

                if showMore {
                    HStack(spacing: 24) {
                        moreAction("照片", "photo.on.rectangle") { store.sendImage(conv.id); showMore = false }
                        moreAction("拍摄", "camera") { store.sendImage(conv.id); showMore = false }
                        moreAction("文件", "doc") { store.sendCustom(conv.id, "文件"); showMore = false }
                        moreAction("位置", "location") { store.sendCustom(conv.id, "位置"); showMore = false }
                    }
                    .padding(.vertical, 20).frame(maxWidth: .infinity).background(DesignTokens.canvas)
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

// MARK: - Community (Flutter MockPostRepository SoT)

private struct CommunityPostModel: Identifiable {
    let id: String
    let userId: String
    var nickname: String
    var content: String
    var publishAt: Date
    var source: String
    var images: [String]
    var videoCoverUrl: String?
    var likeCount: Int
    var commentCount: Int
    var isLiked: Bool
    var isMine: Bool
    var previewComments: [(String, String, String?)] // nickname, body, replyTo?
}

/// Flutter `MockPostRepository` — seed Random(42)/35 posts, tabs, toggleLike, follow.
@MainActor
private final class FlutterCommunityStore: ObservableObject {
    @Published private(set) var posts: [CommunityPostModel] = []
    private var followed = Set<String>()
    private var seeded = false

    private static let samples = [
        "今天去了 @张三 推荐的咖啡店，环境不错。\n#Flutter开发\n欢迎访问：https://flutter.dev",
        "周末 hiking，天气太好了！#户外",
        "刚读完一本好书，推荐 @李四 也看看。",
        "分享一张随手拍～",
        "项目上线啦，感谢团队！#Flutter开发 https://dart.dev",
        "午餐打卡 @王五",
        "学习 GetX 状态管理中…",
    ]
    private static let nicknames = ["张三", "李四", "王五", "赵六", "小明", "小红", "开发者", "产品经理"]

    init() { ensureSeed() }

    private func ensureSeed() {
        guard !seeded else { return }
        seeded = true
        let now = Date()
        var rng = SeededGenerator(seed: 42)
        var list: [CommunityPostModel] = []
        for i in 0..<35 {
            let id = "post_\(i)"
            let isVideo = i % 10 == 0
            let imgCount = isVideo ? 0 : (i % 9) + 1
            let images = isVideo ? [] : (0..<imgCount).map { "https://picsum.photos/seed/\(id)_\($0)/400/400" }
            let nicks = Self.nicknames
            let preview: [(String, String, String?)] = [
                (nicks[(i + 1) % nicks.count], "说得对！", nil),
                (nicks[(i + 3) % nicks.count], "同感 +1", nicks[i % nicks.count]),
            ]
            list.append(CommunityPostModel(
                id: id,
                userId: "user_\(i % 8)",
                nickname: nicks[i % nicks.count],
                content: Self.samples[i % Self.samples.count],
                publishAt: now.addingTimeInterval(-Double(i * 17 + Int.random(in: 0..<30, using: &rng)) * 60),
                source: i % 2 == 0 ? "来自 iPhone" : "来自 Android",
                images: images,
                videoCoverUrl: isVideo ? "https://picsum.photos/seed/video_\(i)/640/360" : nil,
                likeCount: Int.random(in: 0..<200, using: &rng),
                commentCount: 2 + Int.random(in: 0..<8, using: &rng),
                isLiked: i % 4 == 0,
                isMine: i == 0,
                previewComments: preview
            ))
        }
        posts = list
    }

    func feed(tab: String) -> [CommunityPostModel] {
        ensureSeed()
        var source = posts
        switch tab {
        case "热门":
            source.sort {
                let ha = $0.likeCount * 2 + $0.commentCount
                let hb = $1.likeCount * 2 + $1.commentCount
                if ha != hb { return ha > hb }
                return $0.publishAt > $1.publishAt
            }
        case "关注":
            source = source.filter { followed.contains($0.userId) }
        default:
            source.sort { $0.publishAt > $1.publishAt }
        }
        return Array(source.prefix(10))
    }

    func toggleLike(_ id: String) {
        guard let i = posts.firstIndex(where: { $0.id == id }) else { return }
        var copy = posts
        if copy[i].isLiked {
            copy[i].isLiked = false
            copy[i].likeCount = max(0, copy[i].likeCount - 1)
        } else {
            copy[i].isLiked = true
            copy[i].likeCount += 1
        }
        posts = copy
    }

    func meta(_ p: CommunityPostModel) -> String {
        "\(Self.formatTime(p.publishAt)) · \(p.source)"
    }

    static func formatTime(_ date: Date) -> String {
        let mins = max(0, Int(Date().timeIntervalSince(date) / 60))
        if mins < 1 { return "刚刚" }
        if mins < 60 { return "\(mins)分钟前" }
        if mins < 60 * 24 { return "\(mins / 60)小时前" }
        if mins < 60 * 48 { return "昨天" }
        let f = DateFormatter()
        f.dateFormat = "M月d日"
        return f.string(from: date)
    }
}

/// Deterministic RNG matching Flutter `Random(42)` usage pattern (not bit-identical).
private struct SeededGenerator: RandomNumberGenerator {
    private var state: UInt64
    init(seed: UInt64) { state = seed == 0 ? 0xDEADBEEF : seed }
    mutating func next() -> UInt64 {
        state = state &* 6364136223846793005 &+ 1
        return state
    }
}

struct CommunityTabView: View {
    var onDeferred: (String) -> Void = { _ in }
    @StateObject private var store = FlutterCommunityStore()
    @State private var filter = "最新"

    private var feed: [CommunityPostModel] { store.feed(tab: filter) }

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

            if feed.isEmpty {
                Spacer()
                Text(filter == "关注" ? "还没有关注的人，去最新里看看吧" : "暂无动态",
                     font: .system(size: 15), color: DesignTokens.body)
                Spacer()
            } else {
                ScrollView {
                    VStack(spacing: 12) {
                        ForEach(feed) { post in
                            communityCard(post)
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.top, 8)
                    .padding(.bottom, 16)
                }
            }
        }
        .background(DesignTokens.canvasSoft2)
    }

    private func communityCard(_ post: CommunityPostModel) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(alignment: .top) {
                Circle().fill(DesignTokens.hairline).frame(width: 44, height: 44)
                VStack(alignment: .leading, spacing: 2) {
                    Text(post.nickname, font: .system(size: 16, weight: .semibold), color: DesignTokens.ink)
                    Text(store.meta(post), font: .system(size: 14), color: DesignTokens.body)
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
            Text(post.content, font: .system(size: 16), color: DesignTokens.ink)
                .lineSpacing(4)
            if let cover = post.videoCoverUrl {
                communityThumb(cover)
                    .frame(maxWidth: .infinity)
                    .aspectRatio(16 / 9, contentMode: .fill)
                    .clipped()
                    .clipShape(RoundedRectangle(cornerRadius: 4))
                    .onTapGesture { onDeferred("/community/image_preview") }
            } else if !post.images.isEmpty {
                communityImageGrid(images: post.images)
            }
            HStack(spacing: 24) {
                Button {
                    store.toggleLike(post.id)
                } label: {
                    Label(
                        post.isLiked ? "已赞 \(post.likeCount)" : "赞 \(post.likeCount)",
                        systemImage: post.isLiked ? "heart.fill" : "heart"
                    )
                    .font(.system(size: 13))
                    .foregroundStyle(post.isLiked ? Color.red : DesignTokens.body)
                }
                Button {
                    onDeferred("/community/comment")
                } label: {
                    Label("评论 \(post.commentCount)", systemImage: "bubble.right")
                        .font(.system(size: 13))
                        .foregroundStyle(DesignTokens.body)
                }
                Spacer()
            }
            if !post.previewComments.isEmpty {
                VStack(alignment: .leading, spacing: 4) {
                    ForEach(Array(post.previewComments.enumerated()), id: \.offset) { _, c in
                        if let to = c.2 {
                            Text("\(c.0) 回复 \(to)：\(c.1)", font: .system(size: 13), color: DesignTokens.body)
                        } else {
                            Text("\(c.0)：\(c.1)", font: .system(size: 13), color: DesignTokens.body)
                        }
                    }
                }
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

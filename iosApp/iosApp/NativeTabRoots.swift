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
                                .renderingMode(.template)
                                .foregroundStyle(tint)
                                .frame(width: 22, height: 22)
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
        case .home, .chat:
            return active ? "main_tab_home_selected" : "main_tab_home_unselected"
        case .community, .mine:
            return active ? "main_tab_me_selected" : "main_tab_me_unselected"
        }
    }
}

// MARK: - Home

struct HomeTabView: View {
    var onDeferred: (String) -> Void
    @State private var topTab = 0
    @State private var metricTab = 0

    private let features = [
        "销售顾问", "生活服务", "二手车", "新车关注", "AI小石头",
        "订单中心", "数据分析", "直播带货", "营销活动", "更多",
    ]
    private let quickActions = [
        ("新伙伴待确认", "3 位新成员等待审核"),
        ("待跟进客户", "今日 5 位意向客户"),
    ]

    private var greeting: String {
        let hour = Calendar.current.component(.hour, from: Date())
        let prefix = hour < 12 ? "早上好" : (hour < 18 ? "下午好" : "晚上好")
        return "\(prefix)，沃德龙鼎"
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                // Flutter HomeGreetingSection: top = safe + 16 + greeting padding 24 → use safeArea
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
                .padding(.top, 8)

                // Flutter HomeSearchBar
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

                // 首页 / 视频 / Club — 左对齐，三项全部可见
                HStack(spacing: 24) {
                    ForEach(Array(["首页", "视频", "Club"].enumerated()), id: \.offset) { index, label in
                        VStack(spacing: 6) {
                            Text(
                                label,
                                font: .system(size: topTab == index ? 16 : 15, weight: topTab == index ? .semibold : .regular),
                                color: topTab == index ? DesignTokens.ink : DesignTokens.body
                            )
                            Capsule()
                                .fill(topTab == index ? DesignTokens.link : Color.clear)
                                .frame(width: 20, height: 3)
                        }
                        .contentShape(Rectangle())
                        .onTapGesture { topTab = index }
                    }
                    Spacer(minLength: 0)
                }
                .padding(.horizontal, 16)
                .padding(.top, 16)

                if topTab == 0 {
                    // Flutter HomeBannerSection: h=132, margin 16
                    Image("home_banner")
                        .resizable()
                        .scaledToFill()
                        .frame(maxWidth: .infinity)
                        .frame(height: 132)
                        .clipShape(RoundedRectangle(cornerRadius: 8))
                        .padding(.horizontal, 16)
                        .padding(.top, 16)
                        .onTapGesture { onDeferred("朋友圈营销") }

                    // Flutter HomeFeatureGrid: icon 44, row gap 8, margin 16/12
                    featureGrid
                        .padding(.horizontal, 16)
                        .padding(.top, 12)

                    HStack(spacing: 12) {
                        ForEach(quickActions, id: \.0) { action in
                            HStack(spacing: 10) {
                                Text(String(action.0.prefix(1)), font: .system(size: 16, weight: .bold), color: DesignTokens.link)
                                    .frame(width: 40, height: 40)
                                    .background(DesignTokens.canvasSoft2, in: RoundedRectangle(cornerRadius: 10))
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(action.0, font: .system(size: 13, weight: .semibold), color: DesignTokens.ink)
                                        .lineLimit(1)
                                    Text(action.1, font: .system(size: 11), color: DesignTokens.body)
                                        .lineLimit(1)
                                }
                                Spacer(minLength: 0)
                            }
                            .padding(12)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 8))
                            .overlay(RoundedRectangle(cornerRadius: 8).stroke(DesignTokens.hairline, lineWidth: 0.5))
                            .onTapGesture { onDeferred(action.0) }
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.top, 12)

                    storeCard
                    strategyRow
                } else if topTab == 1 {
                    homeSubFeed(
                        title: "小视频",
                        items: [
                            ("新车到店 · 15s", "播放 2.1k · 赞 186"),
                            ("保养小贴士", "播放 980 · 赞 64"),
                            ("试驾花絮", "播放 1.4k · 赞 102"),
                        ],
                        actionTitle: "拍一个",
                        openRoute: "/video/short"
                    )
                } else {
                    homeSubFeed(
                        title: "Club",
                        items: [
                            ("周末自驾·密云水库", "报名 36 · 周六出发"),
                            ("店庆抽奖夜", "本周五 · 门店大厅"),
                            ("车友改装聚会", "下周日 · 报名中"),
                        ],
                        actionTitle: "发布活动",
                        openRoute: "Club"
                    )
                }
            }
            .padding(.bottom, 24)
        }
        .safeAreaPadding(.top, 8)
        .background(DesignTokens.canvasSoft2)
    }

    private func homeSubFeed(
        title: String,
        items: [(String, String)],
        actionTitle: String,
        openRoute: String
    ) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text(title, font: .system(size: 18, weight: .semibold), color: DesignTokens.ink)
                Spacer()
                Button(actionTitle) { onDeferred(openRoute) }
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle(DesignTokens.link)
            }
            .padding(.horizontal, 16)
            .padding(.top, 24)

            VStack(spacing: 0) {
                ForEach(Array(items.enumerated()), id: \.offset) { index, item in
                    HStack {
                        RoundedRectangle(cornerRadius: 8)
                            .fill(DesignTokens.link.opacity(0.12))
                            .frame(width: 56, height: 56)
                            .overlay(Text(String(item.0.prefix(1))).foregroundStyle(DesignTokens.link))
                        VStack(alignment: .leading, spacing: 4) {
                            Text(item.0, font: .system(size: 15, weight: .semibold), color: DesignTokens.ink)
                            Text(item.1, font: .system(size: 12), color: DesignTokens.body)
                        }
                        Spacer()
                    }
                    .padding(16)
                    .contentShape(Rectangle())
                    .onTapGesture { onDeferred(openRoute) }
                    if index != items.count - 1 {
                        Divider().overlay(DesignTokens.hairline)
                    }
                }
            }
            .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 12))
            .padding(.horizontal, 16)
        }
    }

    private var featureGrid: some View {
        // Flutter: iconSize=44, labelGap=4, fontSize=11, row gap=8, card padding 4/8
        VStack(spacing: 8) {
            ForEach(0..<2, id: \.self) { row in
                HStack(spacing: 0) {
                    ForEach(features[(row * 5)..<((row + 1) * 5)], id: \.self) { label in
                        VStack(spacing: 4) {
                            Image(featureAsset(label))
                                .resizable()
                                .scaledToFill()
                                .frame(width: 44, height: 44)
                                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                            Text(label, font: .system(size: 11), color: DesignTokens.ink)
                                .lineLimit(1)
                                .minimumScaleFactor(0.8)
                                .multilineTextAlignment(.center)
                        }
                        .frame(maxWidth: .infinity)
                        .contentShape(Rectangle())
                        .onTapGesture {
                            if label == "更多" { onDeferred("全部服务") }
                            else if label == "直播带货" { onDeferred("直播") }
                            else { onDeferred(label) }
                        }
                    }
                }
            }
        }
        .padding(.horizontal, 4)
        .padding(.vertical, 8)
        .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 8))
        .overlay(RoundedRectangle(cornerRadius: 8).stroke(DesignTokens.hairline, lineWidth: 0.5))
    }

    private var storeCard: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("[4S]北京沃德龙鼎吉利", font: .system(size: 16, weight: .semibold), color: DesignTokens.ink)
            HStack(spacing: 8) {
                ForEach(Array(["今日", "昨日", "本月"].enumerated()), id: \.offset) { index, label in
                    Text(label, font: .system(size: 12), color: metricTab == index ? DesignTokens.link : DesignTokens.body)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 6)
                        .background(metricTab == index ? DesignTokens.link.opacity(0.12) : .clear, in: Capsule())
                        .onTapGesture { metricTab = index }
                }
            }
            HStack {
                metric("99", "意向客户")
                metric("2", "新车订单")
                metric("999.8", "成交额(万)")
                metric("15", "试驾预约")
            }
            .padding(.top, 4)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 8))
        .overlay(RoundedRectangle(cornerRadius: 8).stroke(DesignTokens.hairline, lineWidth: 0.5))
        .padding(.horizontal, 16)
        .padding(.top, 16)
    }

    private var strategyRow: some View {
        HStack(spacing: 12) {
            Text("投", font: .system(size: 12, weight: .bold), color: .white)
                .frame(width: 28, height: 28)
                .background(DesignTokens.link, in: Circle())
            VStack(alignment: .leading, spacing: 2) {
                Text("投资策略", color: DesignTokens.ink)
                Text("资产九宫格 · 恐贪定投 · 趋势策略", font: .system(size: 12), color: DesignTokens.body)
            }
            Spacer()
            Text("›", color: DesignTokens.body)
        }
        .padding(16)
        .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 8))
        .overlay(RoundedRectangle(cornerRadius: 8).stroke(DesignTokens.hairline, lineWidth: 0.5))
        .padding(16)
        .onTapGesture { onDeferred("投资策略") }
    }

    private func metric(_ value: String, _ label: String) -> some View {
        VStack(spacing: 2) {
            Text(value, font: .system(size: 16, weight: .semibold), color: DesignTokens.ink)
            Text(label, font: .system(size: 11), color: DesignTokens.body)
        }
        .frame(maxWidth: .infinity)
    }

    private func featureAsset(_ label: String) -> String {
        switch label {
        case "销售顾问": return "home_feature_sales"
        case "生活服务": return "home_feature_life"
        case "二手车": return "home_feature_usedcar"
        case "新车关注": return "home_feature_newcar"
        case "AI小石头": return "home_feature_ai_stone"
        case "订单中心": return "home_feature_order"
        case "数据分析": return "home_feature_data"
        case "直播带货": return "home_feature_live"
        case "营销活动": return "home_feature_market"
        default: return "home_feature_more"
        }
    }
}

// MARK: - Chat

struct ChatTabView: View {
    private let peers: [(String, String, String, String?, Bool)] = [
        ("Mock好友1", "晚上一起吃饭吗？", "22:50", "2", true),
        ("Mock好友2", "你好", "22:45", nil, false),
        ("Mock好友3", "你好", "22:40", nil, true),
    ]
    @State private var selectedPeer: String? = nil
    @State private var draft = ""
    @State private var messages: [String] = []

    var body: some View {
        VStack(spacing: 0) {
            if let peer = selectedPeer {
                chatDetail(peer: peer)
            } else {
                chatList
            }
        }
        .background(DesignTokens.canvasSoft2)
    }

    private var chatList: some View {
        VStack(spacing: 0) {
            HStack {
                Text("消息", font: .system(size: 32, weight: .bold), color: DesignTokens.ink)
                Spacer()
                Text("⌕", font: .system(size: 22), color: DesignTokens.link)
                Text("✎", font: .system(size: 20), color: DesignTokens.link)
                    .padding(.leading, 16)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 8)

            VStack(spacing: 0) {
                ForEach(Array(peers.enumerated()), id: \.offset) { index, peer in
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
                    if index != peers.count - 1 {
                        Divider().overlay(DesignTokens.hairline)
                    }
                }
            }
            .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 12))
            .padding(.horizontal, 16)
            Spacer()
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

            HStack(spacing: 8) {
                TextField("输入消息…", text: $draft)
                    .padding(.horizontal, 12)
                    .frame(height: 40)
                    .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 8))
                Button("发送") {
                    let text = draft.trimmingCharacters(in: .whitespacesAndNewlines)
                    guard !text.isEmpty else { return }
                    messages.append(text)
                    draft = ""
                }
                .buttonStyle(.borderedProminent)
                .tint(DesignTokens.link)
            }
            .padding(12)
            .background(DesignTokens.canvasSoft2)
        }
    }
}

// MARK: - Community

struct CommunityTabView: View {
    var onDeferred: (String) -> Void = { _ in }
    @State private var filter = "最新"

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Text("社区", font: .system(size: 32, weight: .bold), color: DesignTokens.ink)
                Spacer()
                Text("+", font: .system(size: 22, weight: .bold), color: .white)
                    .frame(width: 36, height: 36)
                    .background(DesignTokens.link, in: Circle())
                    .onTapGesture { onDeferred("发布动态") }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 8)

            HStack(spacing: 8) {
                Text("⌕", font: .system(size: 16), color: DesignTokens.body)
                Text("搜索动态、话题、用户", font: .system(size: 14), color: DesignTokens.body)
                Spacer()
            }
            .padding(.horizontal, 12)
            .frame(height: 40)
            .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 10))
            .padding(.horizontal, 16)
            .onTapGesture { onDeferred("社区搜索") }

            HStack {
                ForEach(["最新", "热门", "关注"], id: \.self) { item in
                    VStack(spacing: 6) {
                        Text(item, font: .system(size: 16, weight: filter == item ? .semibold : .regular), color: filter == item ? DesignTokens.ink : DesignTokens.body)
                        RoundedRectangle(cornerRadius: 2)
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
                    communityCard(
                        name: "张三",
                        meta: "7分钟前 · 来自 iPhone",
                        body: "今天去了 @张三 推荐的咖啡店，环境不错。\n#Flutter开发\n欢迎访问：https://flutter.dev",
                        images: ["community_post_video"],
                        singleImage: true,
                        likes: "158",
                        comments: "6"
                    )
                    communityCard(
                        name: "李四",
                        meta: "42分钟前 · 来自 Android",
                        body: "周末 hiking，天气太好了！#户外",
                        images: ["community_post_a", "community_post_b"],
                        singleImage: false,
                        likes: nil,
                        comments: nil
                    )
                }
                .padding(.horizontal, 16)
                .padding(.top, 8)
            }
        }
        .background(DesignTokens.canvasSoft2)
    }

    private func communityCard(
        name: String,
        meta: String,
        body: String,
        images: [String],
        singleImage: Bool,
        likes: String?,
        comments: String?
    ) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(alignment: .top) {
                Circle().fill(DesignTokens.hairline).frame(width: 44, height: 44)
                VStack(alignment: .leading, spacing: 2) {
                    Text(name, font: .system(size: 16, weight: .semibold), color: DesignTokens.ink)
                    Text(meta, font: .system(size: 14), color: DesignTokens.body)
                }
                Spacer()
                Text("⋯", font: .system(size: 20), color: DesignTokens.body)
                    .frame(width: 44, height: 44)
            }
            Text(body, font: .system(size: 16), color: DesignTokens.ink)
                .lineSpacing(4)
            if singleImage, let first = images.first {
                // Flutter `_SingleImage`: maxWidth ≈ 62% screen
                Image(first).resizable().scaledToFill()
                    .frame(maxWidth: .infinity)
                    .aspectRatio(858 / 570, contentMode: .fit)
                    .frame(maxWidth: UIScreen.main.bounds.width * 0.62, alignment: .leading)
                    .clipped()
                    .clipShape(RoundedRectangle(cornerRadius: 6))
            } else {
                HStack(spacing: 4) {
                    ForEach(images, id: \.self) { name in
                        Image(name).resizable().scaledToFill()
                            .aspectRatio(678 / 518, contentMode: .fill)
                            .frame(maxWidth: .infinity)
                            .clipped()
                            .clipShape(RoundedRectangle(cornerRadius: 6))
                    }
                }
            }
            if let likes, let comments {
                HStack(spacing: 24) {
                    Text("♥ \(likes)", font: .system(size: 14), color: Color(red: 0xEE/255, green: 0, blue: 0))
                    Text("💬 \(comments)", font: .system(size: 14), color: DesignTokens.body)
                    Text("↗ 分享", font: .system(size: 14), color: DesignTokens.body)
                }
                VStack(alignment: .leading, spacing: 4) {
                    Text("李四：说得对！", font: .system(size: 14, weight: .semibold), color: Color(red: 0x57/255, green: 0x6B/255, blue: 0x95/255))
                    Text("赵六 回复 张三：同感 +1", font: .system(size: 14, weight: .semibold), color: Color(red: 0x57/255, green: 0x6B/255, blue: 0x95/255))
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(10)
                .background(DesignTokens.canvasSoft2.opacity(0.5), in: RoundedRectangle(cornerRadius: 4))
            }
        }
        .padding(EdgeInsets(top: 14, leading: 16, bottom: 14, trailing: 12))
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 8))
        .overlay(RoundedRectangle(cornerRadius: 8).stroke(DesignTokens.hairline, lineWidth: 0.5))
    }
}

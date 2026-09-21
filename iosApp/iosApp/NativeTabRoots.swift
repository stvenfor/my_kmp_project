import SwiftUI

// MARK: - Home (SwiftUI SoT-aligned structure)
struct HomeTabView: View {
    var onDeferred: (String) -> Void
    private let features = [
        "销售顾问", "生活服务", "二手车", "新车关注", "AI小石头",
        "订单中心", "数据分析", "直播带货", "营销活动", "更多",
    ]
    private let quickActions: [(String, String, String)] = [
        ("新伙伴待确认", "3 位新成员等待审核", "去处理"),
        ("待跟进客户", "今日 5 位意向客户", "去查看"),
        ("订单待审核", "2 笔新车订单", "去处理"),
        ("售后预约", "4 位客户今日到店", "去查看"),
    ]

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    HStack {
                        Text("早上好，沃德龙鼎", font: .title3.weight(.semibold), color: DesignTokens.ink)
                        Spacer()
                        Text("3条新消息")
                            .font(.caption)
                            .foregroundStyle(DesignTokens.link)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 6)
                            .background(DesignTokens.link.opacity(0.12), in: Capsule())
                    }
                    .padding(.horizontal)

                    HStack(spacing: 12) {
                        HStack {
                            Image(systemName: "magnifyingglass")
                                .foregroundStyle(DesignTokens.mute)
                            Text("搜索客户、订单、资讯", color: DesignTokens.mute)
                            Spacer()
                        }
                        .padding(12)
                        .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 12))
                        .overlay(RoundedRectangle(cornerRadius: 12).stroke(DesignTokens.hairline))
                        .onTapGesture { onDeferred("report") }

                        Image(systemName: "square.grid.2x2")
                            .foregroundStyle(DesignTokens.link)
                            .frame(width: 44, height: 44)
                            .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 12))
                            .overlay(RoundedRectangle(cornerRadius: 12).stroke(DesignTokens.hairline))
                            .onTapGesture { onDeferred("scan") }
                    }
                    .padding(.horizontal)

                    VStack(alignment: .leading, spacing: 8) {
                        Text("朋友圈营销", font: .headline, color: .white)
                        Text("一键分享，高效触达客户", color: .white.opacity(0.9))
                        Text("立即体验")
                            .font(.subheadline.weight(.semibold))
                            .foregroundStyle(DesignTokens.link)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 8)
                            .background(.white, in: Capsule())
                    }
                    .padding(20)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(
                        LinearGradient(colors: [DesignTokens.link, DesignTokens.link.opacity(0.55)],
                                       startPoint: .leading, endPoint: .trailing),
                        in: RoundedRectangle(cornerRadius: 12)
                    )
                    .padding(.horizontal)

                    LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 5), spacing: 12) {
                        ForEach(features, id: \.self) { label in
                            VStack(spacing: 6) {
                                RoundedRectangle(cornerRadius: 12)
                                    .fill(DesignTokens.link.opacity(0.12))
                                    .frame(width: 48, height: 48)
                                    .overlay(Text(String(label.prefix(1))).fontWeight(.semibold).foregroundStyle(DesignTokens.link))
                                Text(label, font: .caption2, color: DesignTokens.ink)
                                    .lineLimit(1)
                            }
                            .onTapGesture {
                                switch label {
                                case "更多": onDeferred("services")
                                case "直播带货": onDeferred("live")
                                default: break
                                }
                            }
                        }
                    }
                    .padding(.horizontal)

                    LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                        ForEach(quickActions, id: \.0) { item in
                            VStack(alignment: .leading, spacing: 6) {
                                Text(item.0, font: .subheadline.weight(.semibold), color: DesignTokens.ink)
                                Text(item.1, font: .caption, color: DesignTokens.body)
                                Text(item.2, font: .caption.weight(.semibold), color: DesignTokens.link)
                            }
                            .padding(12)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 12))
                            .overlay(RoundedRectangle(cornerRadius: 12).stroke(DesignTokens.hairline))
                        }
                    }
                    .padding(.horizontal)

                    VStack(alignment: .leading, spacing: 12) {
                        Text("[4S]北京沃德龙鼎吉利", font: .subheadline.weight(.semibold), color: DesignTokens.ink)
                        HStack {
                            ForEach(["今日", "昨日", "本月"], id: \.self) { t in
                                Text(t)
                                    .font(.caption.weight(t == "今日" ? .semibold : .regular))
                                    .foregroundStyle(t == "今日" ? DesignTokens.link : DesignTokens.mute)
                                    .padding(.horizontal, 10)
                                    .padding(.vertical, 6)
                                    .background(t == "今日" ? DesignTokens.link.opacity(0.12) : .clear, in: Capsule())
                            }
                        }
                        LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 4), spacing: 8) {
                            metric("99", "意向客户")
                            metric("2", "新车订单")
                            metric("999.8", "成交额(万)")
                            metric("15", "试驾预约")
                        }
                    }
                    .padding(16)
                    .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 12))
                    .overlay(RoundedRectangle(cornerRadius: 12).stroke(DesignTokens.hairline))
                    .padding(.horizontal)

                    Button {
                        onDeferred("strategy")
                    } label: {
                        HStack {
                            Text("投").font(.caption.weight(.bold)).foregroundStyle(.white)
                                .frame(width: 28, height: 28)
                                .background(DesignTokens.link, in: Circle())
                            VStack(alignment: .leading) {
                                Text("投资策略", color: DesignTokens.ink)
                                Text("资产九宫格 · 恐贪定投 · 趋势策略", font: .caption, color: DesignTokens.body)
                            }
                            Spacer()
                            Image(systemName: "chevron.right").foregroundStyle(DesignTokens.mute)
                        }
                        .padding(16)
                        .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 12))
                        .overlay(RoundedRectangle(cornerRadius: 12).stroke(DesignTokens.hairline))
                    }
                    .padding(.horizontal)
                    .buttonStyle(.plain)
                }
                .padding(.vertical, 12)
            }
            .background(DesignTokens.canvasSoft2.ignoresSafeArea())
            .navigationTitle("首页")
            .navigationBarTitleDisplayMode(.inline)
        }
    }

    private func metric(_ value: String, _ label: String) -> some View {
        VStack(spacing: 4) {
            Text(value, font: .headline.weight(.semibold), color: DesignTokens.ink)
            Text(label, font: .caption2, color: DesignTokens.mute)
        }
    }
}

// MARK: - Chat

struct ChatTabView: View {
    private let peers = [
        ("Mock好友1", "你好，最近怎么样？", "2"),
        ("Mock好友2", "明天一起开会吧", nil as String?),
        ("Mock好友3", "收到，谢谢", nil as String?),
    ]

    var body: some View {
        NavigationStack {
            List {
                ForEach(peers, id: \.0) { peer in
                    HStack(spacing: 12) {
                        Circle()
                            .fill(DesignTokens.link.opacity(0.15))
                            .frame(width: 48, height: 48)
                            .overlay(Text(String(peer.0.suffix(1))).foregroundStyle(DesignTokens.link))
                        VStack(alignment: .leading, spacing: 4) {
                            Text(peer.0, font: .body.weight(.semibold), color: DesignTokens.ink)
                            Text(peer.1, font: .caption, color: DesignTokens.body).lineLimit(1)
                        }
                        Spacer()
                        if let badge = peer.2 {
                            Text(badge)
                                .font(.caption2.weight(.bold))
                                .foregroundStyle(.white)
                                .padding(.horizontal, 6)
                                .padding(.vertical, 2)
                                .background(Color.red, in: Capsule())
                        }
                    }
                    .padding(.vertical, 4)
                }
            }
            .listStyle(.plain)
            .navigationTitle("消息")
            .toolbar {
                ToolbarItemGroup(placement: .topBarTrailing) {
                    Image(systemName: "magnifyingglass")
                    Image(systemName: "square.and.pencil")
                }
            }
        }
    }
}

// MARK: - Community

struct CommunityTabView: View {
    @State private var filter = "最新"
    private let posts: [(String, String, String)] = [
        ("张三", "7分钟前 · 来自 iPhone", "今天去了 @张三 推荐的咖啡店，环境不错。\n#Flutter开发"),
        ("李四", "42分钟前 · 来自 Android", "周末 hiking，天气太好了！#户外"),
        ("王五", "61分钟前 · 来自 iPhone", "刚读完一本好书，推荐 @李四 也看看。"),
    ]

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                HStack {
                    ForEach(["最新", "热门", "关注"], id: \.self) { item in
                        Button(item) { filter = item }
                            .font(.subheadline.weight(filter == item ? .semibold : .regular))
                            .foregroundStyle(filter == item ? DesignTokens.link : DesignTokens.mute)
                            .frame(maxWidth: .infinity)
                    }
                }
                .padding(.vertical, 10)
                .background(DesignTokens.canvas)

                List {
                    ForEach(posts, id: \.0) { post in
                        VStack(alignment: .leading, spacing: 8) {
                            HStack {
                                Circle().fill(DesignTokens.link.opacity(0.15)).frame(width: 36, height: 36)
                                VStack(alignment: .leading) {
                                    Text(post.0, font: .subheadline.weight(.semibold), color: DesignTokens.ink)
                                    Text(post.1, font: .caption2, color: DesignTokens.mute)
                                }
                            }
                            Text(post.2, color: DesignTokens.body)
                        }
                        .padding(.vertical, 6)
                    }
                }
                .listStyle(.plain)
            }
            .background(DesignTokens.canvasSoft2.ignoresSafeArea())
            .navigationTitle("社区")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Image(systemName: "square.and.pencil")
                }
            }
        }
    }
}

// MARK: - Mine Root (native)

struct MineRootView: View {
    var isLoggedIn: Bool
    var onLogin: () -> Void
    var onLogout: () -> Void
    var onOpenSettings: () -> Void
    var onOpenPersonalized: () -> Void
    var onDeferred: (String) -> Void

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    HStack {
                        Spacer()
                        Button(action: onOpenPersonalized) { Image(systemName: "info.circle") }
                        Button(action: onOpenSettings) { Image(systemName: "gearshape") }
                        Button(action: isLoggedIn ? onLogout : onLogin) {
                            Image(systemName: isLoggedIn ? "rectangle.portrait.and.arrow.right" : "person.badge.key")
                        }
                    }
                    .foregroundStyle(DesignTokens.ink)
                    .padding(.horizontal)

                    VStack(alignment: .leading, spacing: 8) {
                        Text(isLoggedIn ? "用户0000" : "未登录", font: .title3.weight(.semibold), color: DesignTokens.ink)
                        if isLoggedIn {
                            Text("销售经理", font: .caption, color: DesignTokens.link)
                            Text("[4S]北京大兴兴荣丰田汽车销售服务有限公司", font: .caption, color: DesignTokens.body)
                        } else {
                            Button("登录", action: onLogin)
                                .buttonStyle(.borderedProminent)
                                .tint(DesignTokens.link)
                        }
                    }
                    .padding(16)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 12))
                    .padding(.horizontal)

                    HStack {
                        ForEach([("1028", "加入天数"), ("28", "员工数"), ("2059", "店铺天数"), ("9366", "累计客户")], id: \.1) { item in
                            VStack(spacing: 4) {
                                Text(isLoggedIn ? item.0 : "0", font: .headline, color: DesignTokens.ink)
                                Text(item.1, font: .caption2, color: DesignTokens.mute)
                            }
                            .frame(maxWidth: .infinity)
                        }
                    }
                    .padding(.vertical, 12)
                    .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 12))
                    .padding(.horizontal)

                    Text("常用服务", font: .subheadline.weight(.semibold), color: DesignTokens.ink)
                        .padding(.horizontal)
                    LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 4), spacing: 12) {
                        ForEach(["商城", "我的钱包", "我的课程", "我的订单"], id: \.self) { label in
                            VStack(spacing: 6) {
                                RoundedRectangle(cornerRadius: 12)
                                    .fill(DesignTokens.link.opacity(0.12))
                                    .frame(width: 44, height: 44)
                                Text(label, font: .caption2, color: DesignTokens.ink)
                            }
                        }
                    }
                    .padding(.horizontal)

                    VStack(spacing: 0) {
                        ForEach(["商务合作", "提醒事项", "邀请好友", "粉丝群", "意见反馈", "设置"], id: \.self) { label in
                            Button {
                                if label == "设置" { onOpenSettings() }
                                else if label == "粉丝群" { onDeferred("media") }
                            } label: {
                                HStack {
                                    Text(label, color: DesignTokens.ink)
                                    Spacer()
                                    Image(systemName: "chevron.right").foregroundStyle(DesignTokens.mute)
                                }
                                .padding(16)
                            }
                            .buttonStyle(.plain)
                            Divider().overlay(DesignTokens.hairline)
                        }
                    }
                    .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 12))
                    .padding(.horizontal)
                }
                .padding(.vertical, 12)
            }
            .background(DesignTokens.canvasSoft2.ignoresSafeArea())
            .navigationTitle("我的")
            .navigationBarTitleDisplayMode(.inline)
        }
    }
}

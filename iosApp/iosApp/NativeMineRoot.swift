import SwiftUI

// MARK: - Mine root (native; island is Compose)
// Aligned to Flutter MinePage / Compose MineCatalog SoT.

struct MineRootView: View {
    var isLoggedIn: Bool
    var displayName: String = "qa_user"
    var onLogin: () -> Void
    var onLogout: () -> Void // retained for ContentView; logout lives in Settings island
    var onOpenSettings: () -> Void
    var onOpenPersonalized: () -> Void
    var onDeferred: (String) -> Void

    @State private var selectedStoreId = "1"
    @State private var showSwitchStore = false
    /// Flutter `MinePage._navFadeExtent` = 72.
    @State private var navOpacity: Double = 0

    private let stores: [(String, String)] = [
        ("1", "[4S]北京沃德龙鼎吉利"),
        ("2", "[4S]北京腾远吉利"),
    ]

    /// Flutter `MineQuickServiceData` — mall / wallet / order only.
    private let services: [(String, String)] = [
        ("商城", "HOT"),
        ("我的钱包", ""),
        ("我的订单", ""),
    ]

    /// Flutter `MineFunctionData.catalog` order (+ calculator highlight).
    private let functions: [(String, String, String?)] = [
        ("短信模板", "一键发送 轻松快捷", nil),
        ("购车计算器", "全款/贷款/保险全能算", "5830.00"),
        ("二手车", "置换/专卖/收车", nil),
        ("收支", "个人收支记录", nil),
        ("小视频", "用小视频秀车秀店", nil),
        ("售后专区", "售后维修保养记录", nil),
        ("店铺收款码", "常见问题 功能介绍", nil),
        ("选买问答", "在线解答客户问题", nil),
        ("商家海报", "置换/专卖/估价", nil),
    ]

    /// Flutter `MineMenuData.items`.
    private let menu: [(String, Bool)] = [
        ("收货地址", false),
        ("商务合作", false),
        ("提醒事项", false),
        ("邀请好友", false),
        ("粉丝群", true),
        ("意见反馈", false),
        ("设置", false),
    ]

    private var profileName: String { isLoggedIn ? displayName : "访客" }
    private var roleBadge: String { isLoggedIn ? "销售顾问" : "未登录" }
    private var storeName: String {
        if !isLoggedIn { return "登录后查看门店信息" }
        return stores.first(where: { $0.0 == selectedStoreId })?.1 ?? stores[0].1
    }
    private var maskedPhone: String { isLoggedIn ? "138****5172" : "— — —" }
    private var stats: [(String, String)] {
        // Flutter MineController: logged-in stats stay guest zeros until API fills.
        let values = ["0", "0", "0", "0"]
        let labels = ["加入天数", "员工数", "店铺天数", "累计客户"]
        return zip(values, labels).map { ($0, $1) }
    }

    var body: some View {
        ZStack(alignment: .top) {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    topChrome
                    profileCard
                    statsRow
                    quickServices
                    functionSection
                    menuSection
                }
                .padding(16)
                .background(
                    GeometryReader { geo in
                        Color.clear.preference(
                            key: MineScrollOffsetKey.self,
                            value: -geo.frame(in: .named("mineScroll")).minY
                        )
                    }
                )
            }
            .coordinateSpace(name: "mineScroll")
            .onPreferenceChange(MineScrollOffsetKey.self) { y in
                let next = min(1, max(0, y / 72))
                if abs(next - navOpacity) >= 0.01 {
                    navOpacity = next
                }
            }
            .background(DesignTokens.canvasSoft2)

            if navOpacity >= 0.05 {
                collapsedNavBar
                    .opacity(navOpacity)
                    .allowsHitTesting(navOpacity >= 0.05)
            }
        }
        .background(DesignTokens.canvasSoft2)
        .sheet(isPresented: $showSwitchStore) {
            SwitchStoreSheet(
                stores: stores,
                selectedId: selectedStoreId,
                onPick: { id in
                    selectedStoreId = id
                    showSwitchStore = false
                },
                onClose: { showSwitchStore = false }
            )
            .presentationDetents([.medium])
        }
    }

    /// Flutter `_MineCollapsedNavBar`.
    private var collapsedNavBar: some View {
        VStack(spacing: 0) {
            HStack(spacing: 0) {
                Text("我的", font: .system(size: 20, weight: .semibold), color: DesignTokens.ink)
                    .padding(.leading, 16)
                Spacer()
                chromeIcon("info.circle") { onOpenPersonalized() }
                chromeIcon("calendar") { onDeferred("签到日历") }
                chromeIcon("gearshape") { onOpenSettings() }
                chromeIcon(isLoggedIn ? "person.crop.circle" : "person.crop.circle.badge.plus") {
                    if isLoggedIn {
                        onDeferred("个人资料")
                    } else {
                        onLogin()
                    }
                }
            }
            .frame(height: 44)
            .padding(.top, 0)
            Divider().overlay(DesignTokens.hairline)
        }
        .background(DesignTokens.canvas)
    }

    private var topChrome: some View {
        HStack(spacing: 0) {
            Text("我的", font: .system(size: 32, weight: .bold), color: DesignTokens.ink)
            Spacer()
            chromeIcon("info.circle") { onOpenPersonalized() }
            chromeIcon("calendar") { onDeferred("签到日历") }
            chromeIcon("gearshape") { onOpenSettings() }
            chromeIcon(isLoggedIn ? "person.crop.circle" : "person.crop.circle.badge.plus") {
                if isLoggedIn {
                    onDeferred("个人资料")
                } else {
                    onLogin()
                }
            }
        }
    }

    private func chromeIcon(_ systemName: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Image(systemName: systemName)
                .font(.system(size: 20))
                .foregroundStyle(DesignTokens.link)
                .frame(width: 40, height: 44)
        }
        .buttonStyle(.plain)
    }

    private var profileCard: some View {
        HStack(alignment: .top, spacing: 16) {
            Circle()
                .fill(DesignTokens.link.opacity(0.12))
                .frame(width: 64, height: 64)
                .overlay(
                    Image(systemName: "person.fill")
                        .font(.system(size: 28))
                        .foregroundStyle(DesignTokens.link.opacity(0.7))
                )
                .onTapGesture {
                    if isLoggedIn { onDeferred("个人资料") }
                    else { onLogin() }
                }

            VStack(alignment: .leading, spacing: 8) {
                HStack(spacing: 8) {
                    Text(profileName, font: .system(size: 20, weight: .semibold), color: DesignTokens.ink)
                        .lineLimit(1)
                    Text(roleBadge, font: .system(size: 11, weight: .semibold), color: .white)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(DesignTokens.link, in: RoundedRectangle(cornerRadius: 6))
                }

                HStack(spacing: 4) {
                    Text(storeName, font: .system(size: 12), color: DesignTokens.body)
                        .lineLimit(1)
                    Image(systemName: "chevron.down")
                        .font(.system(size: 10))
                        .foregroundStyle(DesignTokens.body)
                }
                .onTapGesture {
                    if isLoggedIn { showSwitchStore = true }
                    else { onLogin() }
                }

                HStack(spacing: 16) {
                    HStack(spacing: 4) {
                        Image(systemName: "creditcard")
                            .font(.system(size: 12))
                            .foregroundStyle(DesignTokens.link)
                        Text("电子名片", font: .system(size: 12), color: DesignTokens.link)
                    }
                    .padding(.horizontal, 10)
                    .padding(.vertical, 6)
                    .background(DesignTokens.link.opacity(0.08), in: Capsule())
                    .onTapGesture { onDeferred("电子名片") }

                    Text(maskedPhone, font: .system(size: 12), color: DesignTokens.body)
                }

                if !isLoggedIn {
                    Button("登录", action: onLogin)
                        .buttonStyle(.borderedProminent)
                        .tint(DesignTokens.link)
                }
            }
            Spacer(minLength: 0)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 12))
    }

    private var statsRow: some View {
        HStack {
            ForEach(stats, id: \.1) { item in
                VStack(spacing: 6) {
                    Text(item.0, font: .system(size: 18, weight: .bold), color: DesignTokens.ink)
                    Text(item.1, font: .system(size: 12), color: DesignTokens.body)
                }
                .frame(maxWidth: .infinity)
            }
        }
        .padding(.vertical, 20)
        .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 12))
    }

    private var quickServices: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("常用服务", font: .system(size: 16, weight: .semibold), color: DesignTokens.ink)
            HStack {
                ForEach(services, id: \.0) { item in
                    VStack(spacing: 8) {
                        ZStack(alignment: .topTrailing) {
                            RoundedRectangle(cornerRadius: 12)
                                .fill(DesignTokens.link.opacity(0.12))
                                .frame(width: 44, height: 44)
                            if !item.1.isEmpty {
                                Text(item.1, font: .system(size: 9, weight: .bold), color: .white)
                                    .padding(.horizontal, 5)
                                    .padding(.vertical, 2)
                                    .background(Color.red, in: RoundedRectangle(cornerRadius: 6))
                                    .offset(x: 8, y: -6)
                            }
                        }
                        Text(item.0, font: .system(size: 12), color: DesignTokens.ink)
                            .lineLimit(1)
                    }
                    .frame(maxWidth: .infinity)
                    .onTapGesture { onDeferred(item.0) }
                }
            }
            .padding(.vertical, 12)
            .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 12))
        }
    }

    private var functionSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("个人功能", font: .system(size: 16, weight: .semibold), color: DesignTokens.ink)
                Spacer()
                Text("长按拖动顺序", font: .system(size: 12), color: DesignTokens.body)
            }
            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                ForEach(functions, id: \.0) { item in
                    VStack(alignment: .leading, spacing: 4) {
                        RoundedRectangle(cornerRadius: 12)
                            .fill(DesignTokens.link.opacity(0.08))
                            .frame(width: 44, height: 44)
                        Spacer(minLength: 8)
                        Text(item.0, font: .system(size: 16, weight: .semibold), color: DesignTokens.ink)
                        Text(item.1, font: .system(size: 12), color: DesignTokens.body)
                            .lineLimit(2)
                        if let value = item.2 {
                            Text(value, font: .system(size: 16, weight: .bold), color: DesignTokens.link)
                        }
                    }
                    .padding(16)
                    .frame(maxWidth: .infinity, minHeight: 140, alignment: .leading)
                    .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 12))
                    .overlay(RoundedRectangle(cornerRadius: 12).stroke(DesignTokens.hairline, lineWidth: 0.5))
                    .onTapGesture { onDeferred(item.0) }
                }
            }
        }
    }

    private var menuSection: some View {
        VStack(spacing: 0) {
            ForEach(Array(menu.enumerated()), id: \.element.0) { index, item in
                if index > 0 {
                    Divider().overlay(DesignTokens.hairline).padding(.leading, 52)
                }
                Button {
                    // Flutter MineController: most menu rows are toast-only.
                    switch item.0 {
                    case "设置": onOpenSettings()
                    case "收货地址": onDeferred("收货地址")
                    case "商务合作", "提醒事项", "邀请好友", "粉丝群", "意见反馈":
                        // toast via deferred no-op host — ContentView must toast these
                        onDeferred(item.0)
                    default:
                        onDeferred(item.0)
                    }
                } label: {
                    HStack {
                        Text(item.0, color: DesignTokens.ink)
                        Spacer()
                        if item.1 {
                            Circle()
                                .fill(Color.red)
                                .frame(width: 8, height: 8)
                        }
                        Image(systemName: "chevron.right").foregroundStyle(DesignTokens.body)
                    }
                    .padding(16)
                }
                .buttonStyle(.plain)
            }
        }
        .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 12))
    }
}

/// Flutter `SwitchStoreDialog` — Mine root only; never opens /friend.
private struct SwitchStoreSheet: View {
    let stores: [(String, String)]
    let selectedId: String
    var onPick: (String) -> Void
    var onClose: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            Text("切换店铺", font: .system(size: 18, weight: .semibold), color: DesignTokens.ink)
                .padding(.top, 24)
            Text(
                stores.isEmpty ? "暂无可切换的店铺" : "可切换多个店铺查看数据",
                font: .system(size: 13),
                color: DesignTokens.body
            )
            .padding(.top, 8)
            .padding(.bottom, 20)

            VStack(spacing: 12) {
                ForEach(stores, id: \.0) { store in
                    let selected = store.0 == selectedId
                    Button {
                        if store.0 == selectedId { onClose() }
                        else { onPick(store.0) }
                    } label: {
                        Text(store.1, font: .system(size: 15, weight: .medium),
                             color: selected ? Color(red: 0x1B/255, green: 0x82/255, blue: 0xD2/255) : DesignTokens.ink)
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
            Button(action: onClose) {
                Image(systemName: "xmark")
                    .foregroundStyle(.white)
                    .frame(width: 36, height: 36)
                    .background(Color.black.opacity(0.35), in: Circle())
            }
            .padding(.bottom, 20)
        }
        .background(DesignTokens.canvasSoft2)
    }
}

/// Tracks Mine tab scroll offset for Flutter-aligned collapsed nav fade.
private struct MineScrollOffsetKey: PreferenceKey {
    static var defaultValue: CGFloat = 0
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) {
        value = nextValue()
    }
}

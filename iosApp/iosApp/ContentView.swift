import SwiftUI
import UIKit
import ComposeApp

/// Synced from KMP `DesignTokens` / Flutter VercelTokens.light — do not fork.
enum DesignTokens {
    static let primary = Color(red: 0x17/255, green: 0x17/255, blue: 0x17/255)
    static let ink = Color(red: 0x17/255, green: 0x17/255, blue: 0x17/255)
    static let body = Color(red: 0x4D/255, green: 0x4D/255, blue: 0x4D/255)
    static let mute = Color(red: 0x88/255, green: 0x88/255, blue: 0x88/255)
    static let hairline = Color(red: 0xEB/255, green: 0xEB/255, blue: 0xEB/255)
    static let canvas = Color.white
    static let canvasSoft2 = Color(red: 0xF5/255, green: 0xF5/255, blue: 0xF5/255)
    static let link = Color(red: 0x00/255, green: 0x70/255, blue: 0xF3/255)
    static let spacingMd: CGFloat = 16
    static let spacingLg: CGFloat = 24
    /// 0xF2FFFFFF — same as `DesignTokens.Color.TabBarBackground`.
    static let tabBar = Color.white.opacity(242.0 / 255.0)
}

extension Text {
    init(_ content: String, color: Color) {
        self = Text(content).foregroundColor(color)
    }

    init(_ content: String, font: Font, color: Color) {
        self = Text(content).font(font).foregroundColor(color)
    }
}

private enum AppPhase { case splash, privacy, main }

enum MainTab: Int, CaseIterable, Identifiable {
    case home, chat, community, mine
    var id: Int { rawValue }
    var title: String {
        switch self {
        case .home: return "首页"
        case .chat: return "聊天"
        case .community: return "社区"
        case .mine: return "我的"
        }
    }

    static func fromLaunchArguments() -> MainTab {
        let args = ProcessInfo.processInfo.arguments
        guard let index = args.firstIndex(of: "-tab"), index + 1 < args.count else { return .home }
        switch args[index + 1] {
        case "chat": return .chat
        case "community": return .community
        case "mine": return .mine
        default: return .home
        }
    }
}

/// ADR 0002: SwiftUI owns splash / privacy / tab roots / Mine root / auth /
/// deferred secondary stubs. Compose hosts **Mine island only**.
struct ContentView: View {
    @State private var phase: AppPhase = .splash
    @State private var privacyAccepted = UserDefaults.standard.bool(forKey: "privacy_accepted")
    @State private var tab: MainTab = MainTab.fromLaunchArguments()
    @State private var isLoggedIn = ProcessInfo.processInfo.arguments.contains("-loggedIn")
        || MainViewControllerKt.AuthIsLoggedIn()
    @State private var loginDisplayName = MainViewControllerKt.AuthDisplayName()
    @State private var showLogin = false
    @State private var showMineIsland = false
    @State private var mineIslandRoute = "settings"
    @State private var showSecondary = false
    @State private var secondaryRoute = "/home/search"
    @State private var chatPendingPeer: String? = nil
    @State private var toastText: String? = nil
    @State private var pendingTabAfterLogin: MainTab? = nil
    @State private var pendingRouteAfterLogin: String? = nil

    var body: some View {
        Group {
            switch phase {
            case .splash:
                SplashView().onAppear {
                    DispatchQueue.main.asyncAfter(deadline: .now() + 1.2) {
                        let accepted = privacyAccepted || ProcessInfo.processInfo.arguments.contains("-acceptPrivacy")
                        phase = accepted ? .main : .privacy
                    }
                }
            case .privacy:
                PrivacyView {
                    UserDefaults.standard.set(true, forKey: "privacy_accepted")
                    privacyAccepted = true
                    phase = .main
                }
            case .main:
                mainShell
            }
        }
        .ignoresSafeArea(.keyboard)
        .onOpenURL { url in
            handleDeepLinkOrLabel(url.absoluteString)
        }
        .onAppear {
            if let pending = UserDefaults.standard.string(forKey: "pending_deeplink") {
                UserDefaults.standard.removeObject(forKey: "pending_deeplink")
                handleDeepLinkOrLabel(pending)
            }
            let args = ProcessInfo.processInfo.arguments
            if let idx = args.firstIndex(of: "-route"), idx + 1 < args.count {
                handleDeepLinkOrLabel(args[idx + 1])
            }
        }
    }

    /// Route ownership: tab roots native; Mine island Compose; all other product
    /// routes → Compose SecondaryRouteIsland (Flutter-aligned commonMain hosts).
    private func handleDeepLinkOrLabel(_ raw: String) {
        let route: String
        if let parsed = MainViewControllerKt.AcceptDeepLinkFromIos(uri: raw) {
            route = parsed
        } else if let url = URL(string: raw), !url.path.isEmpty {
            route = url.path.hasPrefix("/") ? url.path : "/\(url.path)"
        } else {
            route = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        }
        openOwnedRoute(route)
    }

    private func openOwnedRoute(_ routeOrLabel: String) {
        let key = routeOrLabel.trimmingCharacters(in: .whitespacesAndNewlines)
        // Flutter MineController toast-only labels — never open secondary Compose.
        let toastOnlyKeys: Set<String> = [
            "切换门店", "切换店铺", "电子名片", "商务合作", "提醒事项",
            "邀请好友", "粉丝群", "意见反馈", "帮助中心", "头像",
            "/mine/business_card", "/mine/invite", "/mine/business",
            "/mine/reminders", "/mine/reminder", "/mine/fan_group", "/mine/feedback",
        ]
        if toastOnlyKeys.contains(key) || key == "请先登录" {
            let toast: String
            switch key {
            case "/mine/business_card": toast = "电子名片"
            case "/mine/invite": toast = "邀请好友"
            case "/mine/business": toast = "商务合作"
            case "/mine/reminders", "/mine/reminder": toast = "提醒事项"
            case "/mine/fan_group": toast = "粉丝群"
            case "/mine/feedback": toast = "意见反馈"
            case "请先登录": toast = "请先登录"
            default: toast = key
            }
            toastText = toast
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.6) {
                if toastText == toast { toastText = nil }
            }
            return
        }
        // Flutter DealInvoiceNavigation: require login, then open demo.
        if key == "新车成交" || key == "/settings/deal_invoice_demo" {
            if !isLoggedIn {
                pendingRouteAfterLogin = "/settings/deal_invoice_demo"
                showLogin = true
                return
            }
            secondaryRoute = "/settings/deal_invoice_demo"
            showSecondary = true
            return
        }
        let path = NativeRouteResolver.resolve(key)
        if path.hasPrefix("/chat/detail") || key.hasPrefix("/chat/detail") {
            let peer = chatPeerFromDeepLink(key.hasPrefix("/") ? key : path)
            chatPendingPeer = peer
            tab = .chat
            if !isLoggedIn { showLogin = true }
            return
        }
        switch path {
        case "/", "/home", "/main":
            tab = .home
        case "/chat":
            tab = .chat
            chatPendingPeer = nil
            if !isLoggedIn { showLogin = true }
        case "/community":
            tab = .community
            if !isLoggedIn { showLogin = true }
        case "/mine":
            tab = .mine
        case "/settings":
            mineIslandRoute = "settings"
            showMineIsland = true
        case "/mine/personalized_settings":
            mineIslandRoute = "personalized"
            showMineIsland = true
        case "/pay/membership":
            mineIslandRoute = "membership"
            showMineIsland = true
        case "/mine/about":
            mineIslandRoute = "about"
            showMineIsland = true
        default:
            secondaryRoute = path.isEmpty ? key : path
            showSecondary = true
        }
    }

    private func chatPeerFromDeepLink(_ raw: String) -> String {
        guard let url = URL(string: raw.hasPrefix("http") || raw.hasPrefix("myai") ? raw : "myai://host\(raw.hasPrefix("/") ? raw : "/\(raw)")"),
              let items = URLComponents(url: url, resolvingAgainstBaseURL: false)?.queryItems else {
            return "mock_peer_01"
        }
        return items.first(where: { $0.name == "peerName" || $0.name == "name" || $0.name == "title" })?.value
            ?? items.first(where: { $0.name == "id" || $0.name == "peerId" })?.value
            ?? "mock_peer_01"
    }

    private var mainShell: some View {
        VStack(spacing: 0) {
            Group {
                switch tab {
                case .home:
                    HomeTabView(
                        onDeferred: { openOwnedRoute($0) },
                        displayName: loginDisplayName,
                        isLoggedIn: isLoggedIn
                    )
                case .chat:
                    if isLoggedIn {
                        ChatTabView(
                            onDeferred: { openOwnedRoute($0) },
                            initialPeer: chatPendingPeer
                        )
                    } else {
                        AuthGateView { showLogin = true }
                    }
                case .community:
                    if isLoggedIn {
                        CommunityTabView(onDeferred: { openOwnedRoute($0) })
                    } else {
                        AuthGateView { showLogin = true }
                    }
                case .mine:
                    MineRootView(
                        isLoggedIn: isLoggedIn,
                        displayName: loginDisplayName,
                        onLogin: { showLogin = true },
                        onLogout: {
                            MainViewControllerKt.AuthLogout()
                            isLoggedIn = false
                            loginDisplayName = "访客"
                        },
                        onOpenSettings: {
                            mineIslandRoute = "settings"
                            showMineIsland = true
                        },
                        onOpenPersonalized: {
                            mineIslandRoute = "personalized"
                            showMineIsland = true
                        },
                        onDeferred: { openOwnedRoute($0) }
                    )
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            NativeBottomBar(selected: tab) { next in
                // Flutter MainPage: soft-auth keeps current tab; remember pending destination.
                if (next == .chat || next == .community) && !isLoggedIn {
                    pendingTabAfterLogin = next
                    showLogin = true
                } else {
                    tab = next
                }
            }
        }
        .background(DesignTokens.canvasSoft2)
        .overlay(alignment: .bottom) {
            if let toastText {
                Text(toastText)
                    .font(.system(size: 14, weight: .medium))
                    .foregroundStyle(.white)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 10)
                    .background(Color.black.opacity(0.78), in: Capsule())
                    .padding(.bottom, 88)
                    .transition(.opacity)
            }
        }
        .fullScreenCover(isPresented: $showLogin) {
            NativeLoginView(
                onSuccess: {
                    isLoggedIn = true
                    loginDisplayName = MainViewControllerKt.AuthDisplayName()
                    showLogin = false
                    if let pending = pendingTabAfterLogin {
                        tab = pending
                        pendingTabAfterLogin = nil
                    }
                    if let route = pendingRouteAfterLogin {
                        pendingRouteAfterLogin = nil
                        secondaryRoute = route
                        showSecondary = true
                    }
                },
                onCancel: {
                    showLogin = false
                    pendingTabAfterLogin = nil
                    pendingRouteAfterLogin = nil
                    tab = .home
                }
            )
        }
        .fullScreenCover(isPresented: $showMineIsland) {
            MineIslandHost(route: mineIslandRoute)
                .ignoresSafeArea(.all)
        }
        .fullScreenCover(isPresented: $showSecondary) {
            SecondaryRouteHost(route: secondaryRoute)
                .ignoresSafeArea(.all)
        }
    }
}

private struct SplashView: View {
    var body: some View {
        ZStack {
            DesignTokens.canvasSoft2.ignoresSafeArea()
            Text("i车商", font: .largeTitle.weight(.semibold), color: DesignTokens.ink)
        }
    }
}

private struct PrivacyView: View {
    var onAccept: () -> Void
    var body: some View {
        VStack(spacing: DesignTokens.spacingMd) {
            Text("隐私政策", font: .title2.weight(.semibold), color: DesignTokens.ink)
            Text("请阅读并同意隐私政策后继续使用本应用。", color: DesignTokens.body)
                .multilineTextAlignment(.center)
                .padding(.horizontal)
            Button("同意并继续", action: onAccept)
                .buttonStyle(.borderedProminent)
                .tint(DesignTokens.link)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(DesignTokens.canvasSoft2.ignoresSafeArea())
    }
}

private struct AuthGateView: View {
    var onLogin: () -> Void
    var body: some View {
        VStack(spacing: DesignTokens.spacingMd) {
            Text("请先登录", font: .title3, color: DesignTokens.ink)
            Text("登录后可使用聊天与社区", font: .system(size: 14), color: DesignTokens.body)
            Button("去登录", action: onLogin)
                .buttonStyle(.borderedProminent)
                .tint(DesignTokens.link)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(DesignTokens.canvasSoft2.ignoresSafeArea())
    }
}

// MARK: - Compose host (Mine island only — ADR 0002)

private struct MineIslandHost: UIViewControllerRepresentable {
    var route: String
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MineIslandViewController(route: route)
    }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

/// Flutter-aligned secondary product UI (Home/Mine/Content/Community RouteHosts).
private struct SecondaryRouteHost: UIViewControllerRepresentable {
    var route: String
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.SecondaryRouteViewController(routeOrLabel: route)
    }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

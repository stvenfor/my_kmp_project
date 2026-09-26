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
        let path = NativeRouteResolver.resolve(key)
        switch path {
        case "/", "/home", "/main":
            tab = .home
        case "/chat":
            tab = .chat
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

    private var mainShell: some View {
        VStack(spacing: 0) {
            Group {
                switch tab {
                case .home:
                    HomeTabView(onDeferred: { openOwnedRoute($0) })
                case .chat:
                    if isLoggedIn {
                        ChatTabView(onDeferred: { openOwnedRoute($0) })
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
                if (next == .chat || next == .community) && !isLoggedIn {
                    tab = next
                    showLogin = true
                } else {
                    tab = next
                }
            }
        }
        .background(DesignTokens.canvasSoft2)
        .fullScreenCover(isPresented: $showLogin) {
            NativeLoginView(
                onSuccess: {
                    isLoggedIn = true
                    loginDisplayName = MainViewControllerKt.AuthDisplayName()
                    showLogin = false
                },
                onCancel: {
                    showLogin = false
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

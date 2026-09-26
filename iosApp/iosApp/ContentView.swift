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
    @State private var showLogin = false
    @State private var showMineIsland = false
    @State private var mineIslandRoute = "settings"
    @State private var nativeFeature: NativeFeatureItem? = nil

    private struct NativeFeatureItem: Identifiable {
        let route: String
        var id: String { route }
    }

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

    /// Route ownership (ADR 0002):
    /// - main tabs → switch SwiftUI tab
    /// - Mine island routes → Compose MineIsland
    /// - everything else → native SwiftUI feature pages
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
            nativeFeature = NativeFeatureItem(route: path)
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
                        onLogin: { showLogin = true },
                        onLogout: { isLoggedIn = false },
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
        .sheet(isPresented: $showLogin) {
            NativeLoginView(
                onSuccess: {
                    isLoggedIn = true
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
        .fullScreenCover(item: $nativeFeature) { item in
            NativeFeatureHost(
                pathOrLabel: item.route,
                onClose: { nativeFeature = nil },
                onOpen: { next in
                    nativeFeature = nil
                    openOwnedRoute(next)
                }
            )
            .ignoresSafeArea(.keyboard)
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
            Button("去登录", action: onLogin)
                .buttonStyle(.borderedProminent)
                .tint(DesignTokens.link)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(DesignTokens.canvasSoft2.ignoresSafeArea())
    }
}

private struct NativeLoginView: View {
    var onSuccess: () -> Void
    var onCancel: () -> Void
    var body: some View {
        NavigationStack {
            VStack(spacing: DesignTokens.spacingLg) {
                Text("登录", font: .title.weight(.semibold), color: DesignTokens.ink)
                Text("演示登录（共享会话后续接线）", color: DesignTokens.body)
                Button("登录", action: onSuccess)
                    .buttonStyle(.borderedProminent)
                    .tint(DesignTokens.link)
            }
            .padding()
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("关闭", action: onCancel)
                }
            }
        }
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

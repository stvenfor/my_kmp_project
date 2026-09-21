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
}

private enum AppPhase { case splash, privacy, main }

private enum MainTab: Int, CaseIterable, Identifiable {
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
}

/// ADR 0002: SwiftUI owns splash / privacy / tab roots / Mine root / auth.
/// Compose is only pushed for Mine secondary island and deferred native stubs.
struct ContentView: View {
    @State private var phase: AppPhase = .splash
    @State private var privacyAccepted = UserDefaults.standard.bool(forKey: "privacy_accepted")
    @State private var tab: MainTab = .home
    @State private var isLoggedIn = false
    @State private var showLogin = false
    @State private var showMineIsland = false
    @State private var mineIslandRoute = "settings"
    @State private var stubTitle: String? = nil

    var body: some View {
        Group {
            switch phase {
            case .splash:
                SplashView().onAppear {
                    DispatchQueue.main.asyncAfter(deadline: .now() + 1.2) {
                        phase = privacyAccepted ? .main : .privacy
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
    }

    private var mainShell: some View {
        TabView(selection: Binding(
            get: { tab },
            set: { next in
                if (next == .chat || next == .community) && !isLoggedIn {
                    tab = next
                    showLogin = true
                } else {
                    tab = next
                }
            }
        )) {
            HomeTabView(onDeferred: { stubTitle = $0 })
                .tabItem { Label(MainTab.home.title, systemImage: "house.fill") }
                .tag(MainTab.home)
            Group {
                if isLoggedIn {
                    ChatTabView()
                } else {
                    AuthGateView { showLogin = true }
                }
            }
            .tabItem { Label(MainTab.chat.title, systemImage: "bubble.left.and.bubble.right.fill") }
            .tag(MainTab.chat)
            Group {
                if isLoggedIn {
                    CommunityTabView()
                } else {
                    AuthGateView { showLogin = true }
                }
            }
            .tabItem { Label(MainTab.community.title, systemImage: "person.3.fill") }
            .tag(MainTab.community)
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
                onDeferred: { stubTitle = $0 }
            )
            .tabItem { Label(MainTab.mine.title, systemImage: "person.crop.circle") }
            .tag(MainTab.mine)
        }
        .tint(DesignTokens.link)
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
        .sheet(item: Binding(
            get: { stubTitle.map { StubItem(title: $0) } },
            set: { stubTitle = $0?.title }
        )) { item in
            DeferredStubView(title: item.title) { stubTitle = nil }
        }
    }
}

private struct StubItem: Identifiable {
    let title: String
    var id: String { title }
}

private struct DeferredStubView: View {
    var title: String
    var onClose: () -> Void
    var body: some View {
        NavigationStack {
            VStack(spacing: DesignTokens.spacingMd) {
                Text(title, font: .title2, color: DesignTokens.ink)
                Text("一期后置 · 原生占位", color: DesignTokens.body)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(DesignTokens.canvasSoft2.ignoresSafeArea())
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("关闭", action: onClose)
                }
            }
            .navigationTitle(title)
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

// MARK: - Compose hosts (island / deferred features only)

private struct MineIslandHost: UIViewControllerRepresentable {
    var route: String
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MineIslandViewController(route: route)
    }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

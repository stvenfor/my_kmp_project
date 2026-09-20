import SwiftUI
import UIKit
import ComposeApp

/// Synced from KMP `DesignTokens` / Flutter VercelTokens.light — do not fork.
enum DesignTokens {
    static let primary = Color(red: 0x17/255, green: 0x17/255, blue: 0x17/255)
    static let onPrimary = Color.white
    static let ink = Color(red: 0x17/255, green: 0x17/255, blue: 0x17/255)
    static let body = Color(red: 0x4D/255, green: 0x4D/255, blue: 0x4D/255)
    static let mute = Color(red: 0x88/255, green: 0x88/255, blue: 0x88/255)
    static let hairline = Color(red: 0xEB/255, green: 0xEB/255, blue: 0xEB/255)
    static let canvas = Color.white
    static let canvasSoft2 = Color(red: 0xF5/255, green: 0xF5/255, blue: 0xF5/255)
    static let link = Color(red: 0x00/255, green: 0x70/255, blue: 0xF3/255)
    static let error = Color(red: 0xEE/255, green: 0x00/255, blue: 0x00/255)
    static let tabBarBackground = Color.white.opacity(0.95)
    static let spacingMd: CGFloat = 16
    static let spacingLg: CGFloat = 24
}

private enum AppPhase {
    case splash, privacy, main
}

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

struct ContentView: View {
    @State private var phase: AppPhase = .splash
    @State private var privacyAccepted = UserDefaults.standard.bool(forKey: "privacy_accepted")
    @State private var tab: MainTab = .home
    @State private var showLogin = false
    @State private var showMineIsland = false
    @State private var mineIslandRoute = "settings"
    @State private var stubTitle: String? = nil
    @State private var isLoggedIn = false

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
                PrivacyView(
                    onAccept: {
                        UserDefaults.standard.set(true, forKey: "privacy_accepted")
                        privacyAccepted = true
                        phase = .main
                    }
                )
            case .main:
                mainShell
            }
        }
        .ignoresSafeArea(.keyboard)
    }

    private var mainShell: some View {
        TabView(selection: $tab) {
            HomeTabView(onDeferred: { stubTitle = $0 })
                .tabItem { Text(MainTab.home.title) }
                .tag(MainTab.home)
            gated(tab: .chat) {
                ChatTabView()
            }
            .tabItem { Text(MainTab.chat.title) }
            .tag(MainTab.chat)
            gated(tab: .community) {
                CommunityTabView()
            }
            .tabItem { Text(MainTab.community.title) }
            .tag(MainTab.community)
            MineRootView(
                isLoggedIn: isLoggedIn,
                onLogin: { showLogin = true },
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
            .tabItem { Text(MainTab.mine.title) }
            .tag(MainTab.mine)
        }
        .tint(DesignTokens.link)
        .onChange(of: tab) { _, newValue in
            if (newValue == .chat || newValue == .community) && !isLoggedIn {
                showLogin = true
            }
        }
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
            MineIslandHost(route: mineIslandRoute, isPresented: $showMineIsland)
                .ignoresSafeArea(.all)
        }
        .sheet(item: Binding(
            get: { stubTitle.map { StubItem(title: $0) } },
            set: { stubTitle = $0?.title }
        )) { item in
            DeferredStubView(title: item.title) { stubTitle = nil }
        }
    }

    @ViewBuilder
    private func gated<Content: View>(tab: MainTab, @ViewBuilder content: () -> Content) -> some View {
        if isLoggedIn {
            content()
        } else {
            AuthGateView(onLogin: { showLogin = true })
        }
    }
}

private struct StubItem: Identifiable {
    let title: String
    var id: String { title }
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

private struct HomeTabView: View {
    var onDeferred: (String) -> Void
    private let entries = ["学习报告", "全部服务", "短视频", "直播", "课堂", "二手车"]
    var body: some View {
        NavigationStack {
            List(entries, id: \.self) { item in
                Button(item) { onDeferred(item) }
                    .foregroundStyle(DesignTokens.ink)
            }
            .navigationTitle("首页")
            .background(DesignTokens.canvasSoft2)
        }
    }
}

private struct ChatTabView: View {
    var body: some View {
        NavigationStack {
            Text("聊天", color: DesignTokens.ink)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(DesignTokens.canvasSoft2)
                .navigationTitle("聊天")
        }
    }
}

private struct CommunityTabView: View {
    var body: some View {
        NavigationStack {
            Text("社区", color: DesignTokens.ink)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(DesignTokens.canvasSoft2)
                .navigationTitle("社区")
        }
    }
}

private struct MineRootView: View {
    var isLoggedIn: Bool
    var onLogin: () -> Void
    var onOpenSettings: () -> Void
    var onOpenPersonalized: () -> Void
    var onDeferred: (String) -> Void

    var body: some View {
        NavigationStack {
            List {
                Section {
                    Text(isLoggedIn ? "已登录" : "未登录", color: DesignTokens.ink)
                    if !isLoggedIn {
                        Button("登录", action: onLogin)
                    }
                }
                Section("功能") {
                    Button("设置", action: onOpenSettings)
                    Button("个性化设置", action: onOpenPersonalized)
                    Button("短视频") { onDeferred("短视频") }
                    Button("二手车") { onDeferred("二手车") }
                }
            }
            .navigationTitle("我的")
        }
    }
}

private struct DeferredStubView: View {
    var title: String
    var onClose: () -> Void
    var body: some View {
        NavigationStack {
            VStack(spacing: DesignTokens.spacingMd) {
                Text(title, font: .title2, color: DesignTokens.ink)
                Text("后续开放", color: DesignTokens.body)
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

private struct MineIslandHost: UIViewControllerRepresentable {
    var route: String
    @Binding var isPresented: Bool

    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MineIslandViewController(route: route)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

import SwiftUI
import UIKit
import ComposeApp

final class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        supportedInterfaceOrientationsFor window: UIWindow?
    ) -> UIInterfaceOrientationMask {
        // Matches Info.plist iPhone orientations; restore Kotlin bridge if Compose needs dynamic lock.
        .allButUpsideDown
    }

    func application(
        _ app: UIApplication,
        open url: URL,
        options: [UIApplication.OpenURLOptionsKey: Any] = [:]
    ) -> Bool {
        DeepLinkEntryKt.acceptDeepLink(uri: url.absoluteString)
    }
}

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    _ = DeepLinkEntryKt.acceptDeepLink(uri: url.absoluteString)
                }
        }
    }
}

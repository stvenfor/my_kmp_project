import SwiftUI
import UIKit
import ComposeApp

final class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        supportedInterfaceOrientationsFor window: UIWindow?
    ) -> UIInterfaceOrientationMask {
        // Phones stay portrait; iPad can follow all system orientations.
        UIDevice.current.userInterfaceIdiom == .pad ? .all : .portrait
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

package com.example.my_kmp_project.core.platform

import platform.Foundation.NSURL
import platform.UIKit.UIAlertController
import platform.UIKit.UIAlertControllerStyleAlert
import platform.UIKit.UIApplication
import platform.UIKit.UIPasteboard
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.darwin.DISPATCH_TIME_NOW
import platform.darwin.dispatch_after
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_time

internal actual fun openUrl(url: String) {
    val nsUrl = NSURL.URLWithString(url) ?: return
    UIApplication.sharedApplication.openURL(nsUrl)
}

internal actual fun copyToClipboard(text: String) {
    UIPasteboard.generalPasteboard.string = text
}

internal actual fun showPlatformToast(message: String) {
    val text = message.trim()
    if (text.isEmpty()) return
    println("[iOS toast] $text")
    dispatch_async(dispatch_get_main_queue()) {
        val presenter = topViewController() ?: return@dispatch_async
        val alert = UIAlertController.alertControllerWithTitle(
            title = null,
            message = text,
            preferredStyle = UIAlertControllerStyleAlert,
        )
        presenter.presentViewController(alert, animated = true, completion = null)
        dispatch_after(
            dispatch_time(DISPATCH_TIME_NOW, 1_500_000_000L),
            dispatch_get_main_queue(),
        ) {
            alert.dismissViewControllerAnimated(true, completion = null)
        }
    }
}

private fun topViewController(): UIViewController? {
    val app = UIApplication.sharedApplication
    val window = app.keyWindow
        ?: (app.windows as? List<*>)?.firstOrNull { it is UIWindow } as? UIWindow
    var vc = window?.rootViewController
    while (vc?.presentedViewController != null) {
        vc = vc?.presentedViewController
    }
    return vc
}

internal actual fun openSystemNotificationSettings(): String? =
    "Deferred · iOS 请在系统设置中开启本应用通知"

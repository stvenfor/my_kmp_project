package com.example.my_kmp_project.core.platform

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import com.example.my_kmp_project.core.account.AndroidAccountContext

internal actual fun openUrl(url: String) {
    val ctx = AndroidAccountContext.applicationContext ?: return
    runCatching {
        ctx.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

internal actual fun copyToClipboard(text: String) {
    val ctx = AndroidAccountContext.applicationContext ?: return
    val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText("text", text))
}

internal actual fun showPlatformToast(message: String) {
    val ctx = AndroidAccountContext.applicationContext ?: return
    val text = message.trim()
    if (text.isEmpty()) return
    val show = {
        Toast.makeText(ctx, text, Toast.LENGTH_SHORT).show()
    }
    if (Looper.myLooper() == Looper.getMainLooper()) {
        show()
    } else {
        Handler(Looper.getMainLooper()).post(show)
    }
}

internal actual fun openSystemNotificationSettings(): String? {
    val ctx = AndroidAccountContext.applicationContext
        ?: return "无法打开系统通知设置（无 Application Context）"
    return runCatching {
        val intent = Intent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
        } else {
            intent.action = "android.settings.APP_NOTIFICATION_SETTINGS"
            intent.putExtra("app_package", ctx.packageName)
            intent.putExtra("app_uid", ctx.applicationInfo.uid)
        }
        ctx.startActivity(intent)
        null
    }.getOrElse { "打开系统通知设置失败：${it.message}" }
}

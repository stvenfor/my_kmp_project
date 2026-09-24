# Module A — 壳/鉴权/桥接（Android 验收）

Date: 2026-09-25  
Commit baseline: post Module-A wiring

## 完成定义核对

| ID | 项 | 结果 |
|----|----|------|
| A.0.1.a | Splash 定时进隐私/Main | PASS — `NativeSplash` 1.2s + `bg_splash`/`ic_splash_logo` |
| A.0.2 | Flutter 资源一致 | PASS — composeResources + android res 已有同名 PNG |
| A.0.1.i/h | iOS/Harmony splash | missing — 登记 platform-gap |
| A.1.1.a | 四 Tab | PASS — NativeAndroidMain + MainBottomBar |
| A.1.2 | 底栏 49 + Flutter 图标 pill | PASS — `MainBottomBar` / ImmersiveInsets 49dp |
| A.1.3 | Tab 保活 | PASS — visited IndexedStack-style `keptTabs` |
| A.1.4 | soft-auth resume | PASS — SoftAuthPresenter + LoginScreen |
| A.2.1–3 | 隐私同意/持久化/拒绝重试 | PASS — PrivacyConsentStore + denied UX |
| A.3.1/4–12 | 登录/OTP/注册 | PASS — 合并 LoginScreen + RegisterScreen（非三独立路由，契约对齐） |
| A.3.13 | 401 清会话 | PASS — TokenExpiredHandler → logout |
| A.3.14 | Token refresh | missing — 登记；logout-only |
| A.3.15 | 微信登录 | missing — 登记 |
| A.4.1 | WebView overlay | PASS — ShellOverlay.InAppWeb |
| A.4.2–3 | JS Bridge + HomeWebHandlers | PASS — 8 methods + registry |
| A.4.4–5 | Scan + 结果 | PASS — ShellOverlay.Scan → DeepLinkRouter/toast |
| A.4.6 | Deeplink 冷启动消费 | PASS — consumePending in NativeAndroidMain |
| A.4.7 | Push | stub — privacy accept 调 StubPushBridge.registerForPush；厂商 SDK missing |

## 手工冒烟（Android）

1. 冷启动见 splash →（未同意）隐私 → 同意进 Home  
2. 二次启动跳过隐私  
3. 点 Chat Tab → LoginScreen → 登录成功 resume Chat  
4. Home 扫一扫 → ScanScreen；底栏图标为 Flutter 裁剪资源  

Compile: `./gradlew :composeApp:compileDebugKotlinAndroid -PandroidOnly=true` OK
